/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
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

   public static final String SESSION_ID_KEY = "sessionId";
   public static final String BASIC_AUTH_KEY = "basicAuthString";

   public static final String SESSION_ID_NOT_PROVIDED_ERROR = "Session ID was not provided";


   ////////////////////////////////////////////////////////////////////////////////////////////////////////////
   // this is how we allow the actions within this class to work without themselves having a logged-in user. //
   ////////////////////////////////////////////////////////////////////////////////////////////////////////////
   private static QSession chickenAndEggSession = new QSession()
   {

   };



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

            ///////////////////////////
            // fetch the user record //
            ///////////////////////////
            GetInput getInput = new GetInput();
            getInput.setTableName(metaData.getUserTableName());
            getInput.setUniqueKey(Map.of(metaData.getUserTableUsernameField(), credentials.split(":")[0]));
            GetOutput getOutput = new GetAction().execute(getInput);
            if(getOutput.getRecord() == null)
            {
               throw (new QAuthenticationException("Incorrect username or password."));
            }

            //////////////////////////////////////////////////////////
            // compare the hashed input password to the stored hash //
            //////////////////////////////////////////////////////////
            QRecord user          = getOutput.getRecord();
            String  inputPassword = credentials.split(":")[1];
            String  storedHash    = user.getValueString(metaData.getUserTablePasswordHashField());

            // if(!inputHash.equals(storedHash))
            if(!PasswordHasher.validatePassword(inputPassword, storedHash))
            {
               throw (new QAuthenticationException("Incorrect username or password."));
            }

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
         qSession = revalidateSession(qInstance, sessionUuid);

         ////////////////////////////////////////////////////////////////////
         // put now into state so we dont check until next interval passes //
         ///////////////////////////////////////////////////////////////////
         StateProviderInterface spi = getStateProvider();
         SimpleStateKey<String> key = new SimpleStateKey<>(qSession.getIdReference());
         spi.put(key, Instant.now());

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

      StateProviderInterface stateProvider           = getStateProvider();
      SimpleStateKey<String> key                     = new SimpleStateKey<>(session.getIdReference());
      Optional<Instant>      lastTimeCheckedOptional = stateProvider.get(Instant.class, key);
      if(lastTimeCheckedOptional.isPresent())
      {
         Instant lastTimeChecked = lastTimeCheckedOptional.get();

         ///////////////////////////////////////////////////////////////////////////////////////////////////
         // returns negative int if less than compared duration, 0 if equal, positive int if greater than //
         // - so this is basically saying, if the time between the last time we checked the token and     //
         // right now is more than ID_TOKEN_VALIDATION_INTERVAL_SECTIONS, then session needs revalidated  //
         ///////////////////////////////////////////////////////////////////////////////////////////////////
         if(Duration.between(lastTimeChecked, Instant.now()).compareTo(Duration.ofSeconds(ID_TOKEN_VALIDATION_INTERVAL_SECONDS)) < 0)
         {
            return (true);
         }
      }

      try
      {
         LOG.debug("Re-validating token due to validation interval [" + lastTimeCheckedOptional + "] being passed (or never being set): " + session.getIdReference());
         revalidateSession(instance, session.getIdReference());

         //////////////////////////////////////////////////////////////////
         // update the timestamp in state provider, to avoid re-checking //
         //////////////////////////////////////////////////////////////////
         stateProvider.put(key, Instant.now());

         return (true);
      }
      catch(QAuthenticationException ae)
      {
         return (false);
      }
      catch(Exception e)
      {
         LOG.warn("Error validating session", e);
         return (false);
      }
   }



   /*******************************************************************************
    ** makes request to check if a session is still valid and build new qSession if it is
    **
    *******************************************************************************/
   private QSession revalidateSession(QInstance qInstance, String sessionUuid) throws QException
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

         ///////////////////////////////////////////////////////////////////////////////////////////////////
         // returns negative int if less than compared duration, 0 if equal, positive int if greater than //
         // - so this is basically saying, if the time between the last time the session was marked as    //
         // active, and right now is more than the timeout seconds, then the session is expired          //
         ///////////////////////////////////////////////////////////////////////////////////////////////////
         if(lastAccess.plus(Duration.ofSeconds(metaData.getInactivityTimeoutSeconds())).isBefore(Instant.now()))
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

         return (qSession);
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
    **
    *******************************************************************************/
   public static class PasswordHasher
   {
      private static final String PBKDF2_ALGORITHM_SHA256 = "PBKDF2WithHmacSHA256";
      private static final String ALGORITHM_PREFIX_SHA256 = "sha256";

      private static final int SALT_BYTE_SIZE        = 32;
      private static final int HASH_BYTE_SIZE        = 32;
      private static final int PBKDF2_ITERATIONS     = 100000; // OWASP recommended minimum for SHA256



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

         // If the algorithm prefix is not the expected one, still perform a dummy
         // constant-time validation to avoid timing differences that could aid
         // enumeration of legacy or unsupported hashes.
         if(params.length < 4 || !ALGORITHM_PREFIX_SHA256.equals(params[0]))
         {
            return constantTimeFalse(password);
         }

         try
         {
            int    iterations = Integer.parseInt(params[1]);
            byte[] salt       = fromHex(params[2]);
            byte[] hash       = fromHex(params[3]);

            byte[] testHash = computePbkdf2Hash(password.toCharArray(), salt, iterations, hash.length, PBKDF2_ALGORITHM_SHA256);
            return slowEquals(hash, testHash);
         }
         catch(RuntimeException e)
         {
            // Any parsing or decoding error (e.g., NumberFormatException, IllegalArgumentException,
            // ArrayIndexOutOfBoundsException) is treated as an invalid password, but we still
            // execute a dummy constant-time validation to mitigate timing attacks.
            return constantTimeFalse(password);
         }
      }



      /*******************************************************************************
       ** Perform a dummy password validation in (approximately) constant time so that
       ** callers cannot distinguish formatting or parsing errors from real failures
       ** based on timing.
       *******************************************************************************/
      private static boolean constantTimeFalse(String password) throws NoSuchAlgorithmException, InvalidKeySpecException
      {
         // Fixed parameters for dummy computation; these values are not secret and
         // must not be changed in a way that significantly alters runtime
         // characteristics relative to the real validation.
         final int    dummyIterations = 10000;
         final byte[] dummySalt       = new byte[] { 0x00, 0x01, 0x02, 0x03 };
         final byte[] dummyHash       = new byte[] { 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11 };

         byte[] dummyTestHash = computePbkdf2Hash(password.toCharArray(), dummySalt, dummyIterations, dummyHash.length, PBKDF2_ALGORITHM_SHA256);
         // Always return false; slowEquals is used only to equalize timing.
         slowEquals(dummyHash, dummyTestHash);
         return false;
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
