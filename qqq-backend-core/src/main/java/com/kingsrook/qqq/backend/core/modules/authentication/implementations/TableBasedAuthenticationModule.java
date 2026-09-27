/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kingsrook.qqq.backend.core.modules.authentication.implementations;


import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.TableBasedAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleInterface;
import com.kingsrook.qqq.backend.core.state.InMemoryStateProvider;
import com.kingsrook.qqq.backend.core.state.SimpleStateKey;
import com.kingsrook.qqq.backend.core.state.StateProviderInterface;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 **
 *******************************************************************************/
public class TableBasedAuthenticationModule implements QAuthenticationModuleInterface
{
   private static final QLogger LOG = QLogger.getLogger(TableBasedAuthenticationModule.class);

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////
   // 30 minutes - ideally this would be lower, but right now we've been dealing with re-validation issues... //
   /////////////////////////////////////////////////////////////////////////////////////////////////////////////
   public static final int ID_TOKEN_VALIDATION_INTERVAL_SECONDS = 1800;

   public static final String SESSION_ID_KEY   = "sessionId";
   public static final String SESSION_UUID_KEY = "sessionUUID";
   public static final String BASIC_AUTH_KEY   = "basicAuthString";

   public static final String SESSION_ID_NOT_PROVIDED_ERROR = "Session ID was not provided";


   ////////////////////////////////////////////////////////////////////////////////////////////////////////////
   // this is how we allow the actions within this class to work without themselves having a logged-in user. //
   ////////////////////////////////////////////////////////////////////////////////////////////////////////////
   // (it holds every permission: the user and session tables are permission-protected by default, QRun-IO/qqq#696)
   private static QSession chickenAndEggSession = new QSession()
   {
      @Override
      public boolean hasPermission(String permissionName)
      {
         return (true);
      }
   };

   /////////////////////////////////////////////////////////////////////////////////////
   // state keys for the last request seen per session (next to the last validation) //
   /////////////////////////////////////////////////////////////////////////////////////
   private static final String ACTIVITY_KEY_PREFIX = "tableBasedAuthActivity:";

   private static final SignInThrottle SIGN_IN_THROTTLE = new SignInThrottle();



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean usesSessionIdCookie()
   {
      return (true);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QSession createSession(QInstance qInstance, Map<String, String> context) throws QAuthenticationException
   {
      TableBasedAuthenticationMetaData metaData    = (TableBasedAuthenticationMetaData) qInstance.getAuthentication();
      String                           sessionUuid = context.get(SESSION_ID_KEY);

      //////////////////////////////////////////////////////////////////////////////////
      // a session created by manageSession is identified by the sessionUUID cookie, //
      // which carries the same id as the sessionId cookie secured routes set.       //
      //////////////////////////////////////////////////////////////////////////////////
      if(!StringUtils.hasContent(sessionUuid))
      {
         sessionUuid = context.get(SESSION_UUID_KEY);
      }

      ///////////////////////////////////////////////////////////
      // check if we are processing a Basic Auth Session first //
      ///////////////////////////////////////////////////////////
      if(context.containsKey(BASIC_AUTH_KEY))
      {
         QSession contextSessionBefore = QContext.getQSession();
         try
         {
            QContext.setQSession(chickenAndEggSession);

            /////////////////////////////////////////////////
            // decode the credentials from the header auth //
            /////////////////////////////////////////////////
            String base64Credentials = context.get(BASIC_AUTH_KEY).trim();
            byte[] credDecoded       = Base64.getDecoder().decode(base64Credentials);
            String credentials       = new String(credDecoded, StandardCharsets.UTF_8);

            ////////////////////////////////////////////////////////////////////////////
            // user-id and password are split at the first colon: a password may     //
            // contain colons, a user-id may not (RFC 7617).                          //
            ////////////////////////////////////////////////////////////////////////////
            int colon = credentials.indexOf(':');
            if(colon < 1)
            {
               throw (new QAuthenticationException("Incorrect username or password."));
            }
            String username = credentials.substring(0, colon);

            //////////////////////////////////////////////////////////////////////////
            // soft lockout: after repeated failures for a username, refuse further //
            // attempts for a while without checking the password (ASVS 2.2.1).     //
            // Unknown usernames are counted the same way, so nothing is revealed.  //
            //////////////////////////////////////////////////////////////////////////
            if(SIGN_IN_THROTTLE.isLockedOut(metaData, username))
            {
               throw (new QAuthenticationException(SignInThrottle.LOCKED_OUT_MESSAGE));
            }

            ///////////////////////////
            // fetch the user record //
            ///////////////////////////
            GetInput getInput = new GetInput();
            getInput.setTableName(metaData.getUserTableName());
            getInput.setUniqueKey(Map.of(metaData.getUserTableUsernameField(), username));
            ////////////////////////////////////////////////////////////////////
            // the password hash field is hidden (never returned by the API); //
            // this module is the one reader that needs it                    //
            ////////////////////////////////////////////////////////////////////
            getInput.setShouldOmitHiddenFields(false);
            getInput.setShouldMaskPasswords(false);
            GetOutput getOutput     = new GetAction().execute(getInput);
            String    inputPassword = credentials.substring(colon + 1);
            if(getOutput.getRecord() == null)
            {
               ///////////////////////////////////////////////////////////////////////
               // hash anyway, so an unknown username takes as long to refuse as a //
               // wrong password (the response must not reveal which users exist)  //
               ///////////////////////////////////////////////////////////////////////
               PasswordHasher.validatePassword(inputPassword, PasswordHasher.getUnknownUserHash());
               throw (SIGN_IN_THROTTLE.recordFailure(metaData, username));
            }

            //////////////////////////////////////////////////////////
            // compare the hashed input password to the stored hash //
            //////////////////////////////////////////////////////////
            QRecord user       = getOutput.getRecord();
            String  storedHash = user.getValueString(metaData.getUserTablePasswordHashField());
            if(!StringUtils.hasContent(storedHash))
            {
               throw (SIGN_IN_THROTTLE.recordFailure(metaData, username));
            }

            if(!PasswordHasher.validatePassword(inputPassword, storedHash))
            {
               throw (SIGN_IN_THROTTLE.recordFailure(metaData, username));
            }
            SIGN_IN_THROTTLE.recordSuccess(username);

            //////////////////////
            // insert a session //
            //////////////////////
            sessionUuid = UUID.randomUUID().toString();
            QRecord sessionRecord = new QRecord()
               .withValue(metaData.getSessionTableUuidField(), sessionUuid)
               .withValue(metaData.getSessionTableAccessTimestampField(), Instant.now())
               .withValue(metaData.getSessionTableUserIdField(), user.getValue(metaData.getUserTablePrimaryKeyField()));
            InsertInput insertInput = new InsertInput();
            insertInput.setTableName(metaData.getSessionTableName());
            insertInput.setRecords(List.of(sessionRecord));
            InsertOutput insertOutput = new InsertAction().execute(insertInput);
            if(CollectionUtils.nullSafeHasContents(insertOutput.getRecords().get(0).getErrors()))
            {
               LOG.warn("Inserting session failed: " + insertOutput.getRecords().get(0).getErrors());
               throw (new QAuthenticationException("Incorrect username or password."));
            }
         }
         catch(QAuthenticationException ae)
         {
            // todo - sleep to obscure what was the issue.
            throw (ae);
         }
         catch(Exception e)
         {
            ////////////////
            // ¯\_(ツ)_/¯ //
            ////////////////
            // todo - sleep to obscure what was the issue.
            String message = "Error handling basic authentication: " + e.getMessage();
            LOG.error(message, e);
            throw (new QAuthenticationException(message));
         }
         finally
         {
            QContext.setQSession(contextSessionBefore);
         }
      }

      //////////////////////////////////////////////////
      // get the session uuid from the context object //
      //////////////////////////////////////////////////
      if(sessionUuid == null)
      {
         LOG.warn(SESSION_ID_NOT_PROVIDED_ERROR);
         throw (new QAuthenticationException(SESSION_ID_NOT_PROVIDED_ERROR));
      }

      try
      {
         /////////////////////////////////////////////////////
         // try to build session to see if still valid      //
         // then call method to check more session validity //
         /////////////////////////////////////////////////////
         QSession qSession = buildQSessionFromUuid(qInstance, metaData, sessionUuid);
         if(isSessionValid(qInstance, qSession))
         {
            return (qSession);
         }

         ///////////////////////////////////////////////////////////////////////////////////////
         // if we make it here it means we have never validated this token or its been a long //
         // enough duration so we need to re-verify the token                                 //
         ///////////////////////////////////////////////////////////////////////////////////////
         qSession = revalidateSession(qInstance, sessionUuid, null);

         ////////////////////////////////////////////////////////////////////
         // put now into state so we dont check until next interval passes //
         ///////////////////////////////////////////////////////////////////
         rememberValidation(qSession.getIdReference(), Instant.now());

         return (qSession);
      }
      catch(QAuthenticationException ae)
      {
         LOG.info("Authentication exception", ae);
         throw (ae);
      }
      catch(Exception e)
      {
         ////////////////
         // ¯\_(ツ)_/¯ //
         ////////////////
         String message = "An unknown error occurred";
         LOG.error(message, e);
         throw (new QAuthenticationException(message));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean isSessionValid(QInstance instance, QSession session)
   {
      if(session == chickenAndEggSession)
      {
         ////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // this is how we allow the actions within this class to work without themselves having a logged-in user. //
         ////////////////////////////////////////////////////////////////////////////////////////////////////////////
         return (true);
      }

      if(session instanceof QSystemUserSession)
      {
         return (true);
      }

      if(session == null)
      {
         return (false);
      }

      if(session.getIdReference() == null)
      {
         return (false);
      }

      ////////////////////////////////////////////////////////////////////////////////
      // A validation is remembered for a while, so not every request reads and     //
      // writes the session table. The inactivity timeout is still enforced on      //
      // every request (QRun-IO/qqq#696): the last request seen is kept in memory,   //
      // a session idle longer than the timeout is checked against the table again, //
      // and the table is revalidated (and its access time refreshed) at least      //
      // twice per timeout, so an active session is never expired early.           //
      ////////////////////////////////////////////////////////////////////////////////
      TableBasedAuthenticationMetaData metaData      = (TableBasedAuthenticationMetaData) instance.getAuthentication();
      Instant                          now           = Instant.now();
      StateProviderInterface           stateProvider = getStateProvider();
      Optional<Instant>                lastValidated = stateProvider.get(Instant.class, new SimpleStateKey<>(session.getIdReference()));
      Optional<Instant>                lastActivity  = stateProvider.get(Instant.class, activityKey(session.getIdReference()));
      if(lastValidated.isPresent() && Duration.between(lastValidated.get(), now).compareTo(validationInterval(metaData)) < 0)
      {
         Instant lastSeen = lastActivity.orElse(lastValidated.get());
         if(Duration.between(lastSeen, now).compareTo(inactivityTimeout(metaData)) <= 0)
         {
            stateProvider.put(activityKey(session.getIdReference()), now);
            return (true);
         }
      }

      try
      {
         LOG.debug("Re-validating session (validation interval passed, never set, or idle): " + session.getIdReference());
         revalidateSession(instance, session.getIdReference(), lastActivity.orElse(null));
         rememberValidation(session.getIdReference(), now);
         return (true);
      }
      catch(QAuthenticationException ae)
      {
         forgetValidation(session.getIdReference());
         return (false);
      }
      catch(Exception e)
      {
         LOG.warn("Error validating session", e);
         return (false);
      }
   }



   /*******************************************************************************
    ** How long a validation is trusted without reading the session table: the
    ** standard interval, but at most half the inactivity timeout.
    *******************************************************************************/
   static Duration validationInterval(TableBasedAuthenticationMetaData metaData)
   {
      long halfTimeout = Math.max(1, inactivityTimeout(metaData).toSeconds() / 2);
      return (Duration.ofSeconds(Math.min(ID_TOKEN_VALIDATION_INTERVAL_SECONDS, halfTimeout)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Duration inactivityTimeout(TableBasedAuthenticationMetaData metaData)
   {
      return (Duration.ofSeconds(metaData.getInactivityTimeoutSeconds() == null ? 14_400 : metaData.getInactivityTimeoutSeconds()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static SimpleStateKey<String> activityKey(String sessionUuid)
   {
      return (new SimpleStateKey<>(ACTIVITY_KEY_PREFIX + sessionUuid));
   }



   /*******************************************************************************
    ** Record that a session was validated against the session table (and used) now.
    *******************************************************************************/
   private static void rememberValidation(String sessionUuid, Instant now)
   {
      getStateProvider().put(new SimpleStateKey<>(sessionUuid), now);
      getStateProvider().put(activityKey(sessionUuid), now);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void forgetValidation(String sessionUuid)
   {
      getStateProvider().remove(new SimpleStateKey<>(sessionUuid));
      getStateProvider().remove(activityKey(sessionUuid));
   }



   /*******************************************************************************
    ** makes request to check if a session is still valid and build new qSession if it is
    **
    ** @param lastActivitySeen the last request this server saw for the session
    **                         (newer than the table's access time between
    **                         revalidations), or null
    *******************************************************************************/
   private QSession revalidateSession(QInstance qInstance, String sessionUuid, Instant lastActivitySeen) throws QException
   {
      QSession contextSessionBefore = QContext.getQSession();
      try
      {
         QContext.setQSession(chickenAndEggSession);

         TableBasedAuthenticationMetaData metaData = (TableBasedAuthenticationMetaData) qInstance.getAuthentication();

         GetInput getSessionInput = new GetInput();
         getSessionInput.setTableName(metaData.getSessionTableName());
         getSessionInput.setUniqueKey(Map.of(metaData.getSessionTableUuidField(), sessionUuid));
         GetOutput getSessionOutput = new GetAction().execute(getSessionInput);
         if(getSessionOutput.getRecord() == null)
         {
            throw (new QAuthenticationException("Session not found."));
         }
         QRecord sessionRecord = getSessionOutput.getRecord();
         Instant lastAccess    = sessionRecord.getValueInstant(metaData.getSessionTableAccessTimestampField());
         if(lastAccess == null || (lastActivitySeen != null && lastActivitySeen.isAfter(lastAccess)))
         {
            lastAccess = lastActivitySeen;
         }
         if(lastAccess == null)
         {
            throw (new QAuthenticationException("Session is expired."));
         }

         ///////////////////////////////////////////////////////////////////////////////////////////////////
         // returns negative int if less than compared duration, 0 if equal, positive int if greater than //
         // - so this is basically saying, if the time between the last time the session was marked as    //
         // active, and right now is more than the timeout seconds, then the session is expired          //
         ///////////////////////////////////////////////////////////////////////////////////////////////////
         if(lastAccess.plus(inactivityTimeout(metaData)).isBefore(Instant.now()))
         {
            throw (new QAuthenticationException("Session is expired."));
         }

         ///////////////////////////////////////////////
         // update the timestamp in the session table //
         ///////////////////////////////////////////////
         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(metaData.getSessionTableName());
         updateInput.setRecords(List.of(new QRecord()
            .withValue(metaData.getSessionTablePrimaryKeyField(), sessionRecord.getValue(metaData.getSessionTablePrimaryKeyField()))
            .withValue(metaData.getSessionTableAccessTimestampField(), Instant.now())));
         new UpdateAction().execute(updateInput);

         return (buildQSessionFromUuid(qInstance, metaData, sessionUuid));
      }
      finally
      {
         QContext.setQSession(contextSessionBefore);
      }
   }



   /*******************************************************************************
    ** extracts info from token creating a QSession
    **
    *******************************************************************************/
   private QSession buildQSessionFromUuid(QInstance qInstance, TableBasedAuthenticationMetaData metaData, String sessionUuid) throws QException
   {
      QSession contextSessionBefore = QContext.getQSession();

      try
      {
         QContext.setQSession(chickenAndEggSession);

         GetInput getSessionInput = new GetInput();
         getSessionInput.setTableName(metaData.getSessionTableName());
         getSessionInput.setUniqueKey(Map.of(metaData.getSessionTableUuidField(), sessionUuid));
         GetOutput getSessionOutput = new GetAction().execute(getSessionInput);
         if(getSessionOutput.getRecord() == null)
         {
            throw (new QAuthenticationException("Session not found."));
         }
         QRecord sessionRecord = getSessionOutput.getRecord();

         GetInput getUserInput = new GetInput();
         getUserInput.setTableName(metaData.getUserTableName());
         getUserInput.setPrimaryKey(sessionRecord.getValue(metaData.getSessionTableUserIdField()));
         GetOutput getUserOutput = new GetAction().execute(getUserInput);
         if(getUserOutput.getRecord() == null)
         {
            throw (new QAuthenticationException("User for session not found."));
         }
         QRecord userRecord = getUserOutput.getRecord();

         QUser qUser = new QUser();
         qUser.setFullName(userRecord.getValueString(metaData.getUserTableFullNameField()));
         qUser.setIdReference(userRecord.getValueString(metaData.getUserTableUsernameField()));

         QSession qSession = new QSession();
         qSession.setUuid(sessionUuid);
         qSession.setIdReference(sessionUuid);
         qSession.setUser(qUser);

         //////////////////////////////////////////////////////////////////
         // identify the user to frontends (manageSession values.user), //
         // as the OAuth2 module does - never the password hash         //
         //////////////////////////////////////////////////////////////////
         String                  username = userRecord.getValueString(metaData.getUserTableUsernameField());
         String                  fullName = userRecord.getValueString(metaData.getUserTableFullNameField());
         HashMap<String, String> user     = new HashMap<>();
         user.put("name", StringUtils.hasContent(fullName) ? fullName : username);
         user.put("username", username);
         qSession.withValueForFrontend("user", user);

         return (qSession);
      }
      finally
      {
         QContext.setQSession(contextSessionBefore);
      }
   }



   /*******************************************************************************
    ** End a session: delete its row from the session table, so neither cookie
    ** can resume it, and forget its last validation time.
    *******************************************************************************/
   @Override
   public void logout(QInstance qInstance, String sessionUUID)
   {
      if(!StringUtils.hasContent(sessionUUID) || !(qInstance.getAuthentication() instanceof TableBasedAuthenticationMetaData metaData))
      {
         return;
      }

      QSession contextSessionBefore = QContext.getQSession();
      try
      {
         QContext.setQSession(chickenAndEggSession);
         new DeleteAction().execute(new DeleteInput(metaData.getSessionTableName())
            .withQueryFilter(new QQueryFilter(new QFilterCriteria(metaData.getSessionTableUuidField(), QCriteriaOperator.EQUALS, sessionUUID))));
         forgetValidation(sessionUUID);
      }
      catch(Exception e)
      {
         LOG.warn("Error deleting session at logout", e);
      }
      finally
      {
         QContext.setQSession(contextSessionBefore);
      }
   }



   /*******************************************************************************
    ** Load an instance of the appropriate state provider
    **
    *******************************************************************************/
   public static StateProviderInterface getStateProvider()
   {
      // TODO - read this from somewhere in meta data eh?
      return (InMemoryStateProvider.getInstance());
   }



   /*******************************************************************************
    ** Soft lockout for password sign-in (ASVS 2.2.1, QRun-IO/qqq#696): after
    ** maxFailedSignInAttempts failures for a username within signInLockoutSeconds,
    ** that username is refused for signInLockoutSeconds, whatever password is
    ** given. Unknown usernames are counted like known ones. Kept per server in
    ** memory; put per-address rate limiting in front of the application as well.
    *******************************************************************************/
   static final class SignInThrottle
   {
      static final String LOCKED_OUT_MESSAGE = "Too many failed sign-in attempts. Try again later.";

      private static final int MAX_TRACKED_USERNAMES = 10_000;

      private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();



      /***************************************************************************
       ** Failures within the window, and the end of an active lockout.
       ***************************************************************************/
      private static final class Attempts
      {
         private final Deque<Instant> failures = new ArrayDeque<>();
         private       Instant        lockedUntil;
      }



      /***************************************************************************
       **
       ***************************************************************************/
      boolean isLockedOut(TableBasedAuthenticationMetaData metaData, String username)
      {
         if(!isEnabled(metaData))
         {
            return (false);
         }
         Attempts entry = attempts.get(key(username));
         if(entry == null)
         {
            return (false);
         }
         synchronized(entry)
         {
            return (entry.lockedUntil != null && entry.lockedUntil.isAfter(Instant.now()));
         }
      }



      /***************************************************************************
       ** Count a failed attempt; returns the exception to throw.
       ***************************************************************************/
      QAuthenticationException recordFailure(TableBasedAuthenticationMetaData metaData, String username)
      {
         QAuthenticationException incorrect = new QAuthenticationException("Incorrect username or password.");
         if(!isEnabled(metaData))
         {
            return (incorrect);
         }

         Instant  now    = Instant.now();
         Duration window = Duration.ofSeconds(metaData.getSignInLockoutSeconds());
         if(attempts.size() >= MAX_TRACKED_USERNAMES)
         {
            purge(now, window);
         }

         Attempts entry = attempts.computeIfAbsent(key(username), name -> new Attempts());
         synchronized(entry)
         {
            while(!entry.failures.isEmpty() && entry.failures.peekFirst().plus(window).isBefore(now))
            {
               entry.failures.removeFirst();
            }
            entry.failures.addLast(now);
            if(entry.failures.size() >= metaData.getMaxFailedSignInAttempts())
            {
               entry.failures.clear();
               entry.lockedUntil = now.plus(window);
               LOG.warn("Locking out password sign-in after repeated failures", logPair("lockoutSeconds", window.toSeconds()));
            }
         }
         return (incorrect);
      }



      /***************************************************************************
       **
       ***************************************************************************/
      void recordSuccess(String username)
      {
         attempts.remove(key(username));
      }



      /***************************************************************************
       ** Forget all attempts (tests).
       ***************************************************************************/
      void clear()
      {
         attempts.clear();
      }



      /***************************************************************************
       **
       ***************************************************************************/
      private void purge(Instant now, Duration window)
      {
         attempts.entrySet().removeIf(entry ->
         {
            synchronized(entry.getValue())
            {
               Attempts value         = entry.getValue();
               boolean  lockExpired   = value.lockedUntil == null || value.lockedUntil.isBefore(now);
               boolean  failuresStale = value.failures.isEmpty() || value.failures.peekLast().plus(window).isBefore(now);
               return (lockExpired && failuresStale);
            }
         });
      }



      /***************************************************************************
       **
       ***************************************************************************/
      private static boolean isEnabled(TableBasedAuthenticationMetaData metaData)
      {
         return (metaData.getMaxFailedSignInAttempts() != null && metaData.getMaxFailedSignInAttempts() > 0
            && metaData.getSignInLockoutSeconds() != null && metaData.getSignInLockoutSeconds() > 0);
      }



      /***************************************************************************
       **
       ***************************************************************************/
      private static String key(String username)
      {
         return (username.trim().toLowerCase(Locale.ROOT));
      }
   }



   /*******************************************************************************
    ** Forget failed sign-in attempts and lockouts (for tests).
    *******************************************************************************/
   static void clearSignInThrottle()
   {
      SIGN_IN_THROTTLE.clear();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class PasswordHasher
   {
      private static final String PBKDF2_ALGORITHM_SHA256 = "PBKDF2WithHmacSHA256";
      private static final String ALGORITHM_PREFIX_SHA256 = "sha256";

      private static final int SALT_BYTE_SIZE        = 32;
      private static final int HASH_BYTE_SIZE        = 32;
      private static final int PBKDF2_ITERATIONS     = 100000; // OWASP recommended minimum for SHA256

      private static String unknownUserHash;



      /*******************************************************************************
       ** A hash of a random password, checked when a username is not found so that
       ** refusing it costs the same as refusing a wrong password.
       *******************************************************************************/
      static synchronized String getUnknownUserHash() throws NoSuchAlgorithmException, InvalidKeySpecException
      {
         if(unknownUserHash == null)
         {
            unknownUserHash = createHashedPassword(UUID.randomUUID().toString());
         }
         return (unknownUserHash);
      }



      /*******************************************************************************
       ** Returns a salted, hashed version of a raw password.
       ** Format: sha256:iterations:salt:hash
       **
       *******************************************************************************/
      public static String createHashedPassword(String password) throws NoSuchAlgorithmException, InvalidKeySpecException
      {
         ////////////////////////////
         // Generate a random salt //
         ////////////////////////////
         SecureRandom random = new SecureRandom();
         byte[]       salt   = new byte[SALT_BYTE_SIZE];
         random.nextBytes(salt);

         ///////////////////////
         // Hash the password //
         ///////////////////////
         byte[] passwordHash = computePbkdf2Hash(password.toCharArray(), salt, PBKDF2_ITERATIONS, HASH_BYTE_SIZE, PBKDF2_ALGORITHM_SHA256);

         //////////////////////////////////////////////////////////////
         // return string in the format sha256:iterations:salt:hash  //
         //////////////////////////////////////////////////////////////
         return (ALGORITHM_PREFIX_SHA256 + ":" + PBKDF2_ITERATIONS + ":" + toHex(salt) + ":" + toHex(passwordHash));
      }



      /*******************************************************************************
       ** Computes the PBKDF2 hash using the specified algorithm.
       **
       *******************************************************************************/
      private static byte[] computePbkdf2Hash(char[] password, byte[] salt, int iterations, int bytes, String algorithm) throws NoSuchAlgorithmException, InvalidKeySpecException
      {
         PBEKeySpec       spec = new PBEKeySpec(password, salt, iterations, bytes * 8);
         SecretKeyFactory skf  = SecretKeyFactory.getInstance(algorithm);

         return skf.generateSecret(spec).getEncoded();
      }



      /*******************************************************************************
       ** Thanks to Baeldung for this and related methods
       ** https://www.baeldung.com/java-byte-arrays-hex-strings
       *******************************************************************************/
      private static String toHex(byte[] array)
      {
         StringBuilder hexStringBuffer = new StringBuilder();
         for(byte b : array)
         {
            hexStringBuffer.append(Character.forDigit((b >> 4) & 0xF, 16));
            hexStringBuffer.append(Character.forDigit((b & 0xF), 16));
         }
         return hexStringBuffer.toString();
      }



      /*******************************************************************************
       ** Validates a password against a hash.
       ** Expected format: sha256:iterations:salt:hash
       **
       *******************************************************************************/
      private static boolean validatePassword(String password, String passwordHash) throws NoSuchAlgorithmException, InvalidKeySpecException
      {
         String[] params = passwordHash.split(":");

         if(!params[0].equals(ALGORITHM_PREFIX_SHA256))
         {
            throw new IllegalArgumentException("Unsupported password hash format. Only SHA256 format (sha256:iterations:salt:hash) is supported. Legacy passwords must be reset.");
         }

         int    iterations = Integer.parseInt(params[1]);
         byte[] salt       = fromHex(params[2]);
         byte[] hash       = fromHex(params[3]);

         byte[] testHash = computePbkdf2Hash(password.toCharArray(), salt, iterations, hash.length, PBKDF2_ALGORITHM_SHA256);
         return slowEquals(hash, testHash);
      }



      /*******************************************************************************
       ** Compares two byte arrays in length-constant time. This comparison method
       ** is used so that password hashes cannot be extracted from an on-line
       ** system using a timing attack and then attacked off-line.
       **
       *******************************************************************************/
      private static boolean slowEquals(byte[] a, byte[] b)
      {
         int diff = a.length ^ b.length;

         for(int i = 0; i < a.length && i < b.length; i++)
         {
            diff |= a[i] ^ b[i];
         }

         return diff == 0;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      private static int toHexDigit(char hexChar)
      {
         int digit = Character.digit(hexChar, 16);
         if(digit == -1)
         {
            throw new IllegalArgumentException("Invalid Hexadecimal Character: " + hexChar);
         }
         return digit;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      private static byte[] fromHex(String hexString)
      {
         if(hexString.length() % 2 == 1)
         {
            throw new IllegalArgumentException("Invalid hexadecimal String supplied.");
         }

         byte[] bytes = new byte[hexString.length() / 2];
         for(int i = 0; i < hexString.length(); i += 2)
         {
            int firstDigit  = toHexDigit(hexString.charAt(i));
            int secondDigit = toHexDigit(hexString.charAt(i + 1));
            bytes[i / 2] = (byte) ((firstDigit << 4) + secondDigit);
         }
         return bytes;
      }
   }

}
