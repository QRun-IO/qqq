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


import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.auth0.client.auth.AuthAPI;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.auth.TokenHolder;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.net.Response;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.AccessTokenException;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.Auth0AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleInterface;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.model.UserSession;
import com.kingsrook.qqq.backend.core.state.InMemoryStateProvider;
import com.kingsrook.qqq.backend.core.state.SimpleStateKey;
import com.kingsrook.qqq.backend.core.state.StateProviderInterface;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.core.utils.memoization.Memoization;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** QQQ AuthenticationModule for working with Auth0.
 **
 ** createSession can be called with the following fields in its context:
 **
 ** System-User session use-case:
 ** 1: Takes in an "accessToken" (but doesn't store a userSession record).
 ** 1b: legacy frontend use-case does the same as system-user!
 **
 ** Web User session use-cases:
 ** 2: creates a new session (userSession record) by taking an "accessToken"
 ** 3: looks up an existing session (userSession record) by taking a "sessionUUID"
 ** 4: takes an "apiKey" (looked up in metaData.AccessTokenTableName - refreshing accessToken with auth0 if needed).
 ** 5: takes a "basicAuthString" (encoded username:password), which make a new accessToken in auth0
 **
 *******************************************************************************/
public class Auth0AuthenticationModule implements QAuthenticationModuleInterface
{
   private static final QLogger LOG = QLogger.getLogger(Auth0AuthenticationModule.class);

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////
   // 30 minutes - ideally this would be lower, but right now we've been dealing with re-validation issues... //
   /////////////////////////////////////////////////////////////////////////////////////////////////////////////
   public static final int ID_TOKEN_VALIDATION_INTERVAL_SECONDS = 1800;

   public static final String ACCESS_TOKEN_KEY = "accessToken";
   public static final String API_KEY          = "apiKey"; // todo - look for users of this, see if we can change to use this constant; maybe move constants up?
   public static final String SESSION_UUID_KEY = "sessionUUID";
   public static final String BASIC_AUTH_KEY   = "basicAuthString"; // todo - look for users of this, see if we can change to use this constant; maybe move constants up?

   public static final String DO_STORE_USER_SESSION_KEY = "doStoreUserSession";

   static final String TOKEN_NOT_PROVIDED_ERROR = "Access Token was not provided";
   static final String COULD_NOT_DECODE_ERROR   = "Unable to decode access token";
   static final String EXPIRED_TOKEN_ERROR      = "Token has expired";
   static final String INVALID_TOKEN_ERROR      = "An invalid token was provided";

   private Auth0AuthenticationMetaData metaData;

   //////////////////////////////////////////////////////////////////////////////////
   // do not use this var directly - rather - always call the getCustomizer method //
   //////////////////////////////////////////////////////////////////////////////////
   private QAuthenticationModuleCustomizerInterface _customizer                = null;
   private boolean                                  customizerHasBeenRequested = false;

   private static boolean mayMemoize = true;

   private static final Memoization<String, String> getAccessTokenFromSessionUUIDMemoization = new Memoization<String, String>()
      .withTimeout(Duration.of(1, ChronoUnit.MINUTES))
      .withMaxSize(1000);

   ////////////////////////////////////////////////////////////////////////////////////////////////////////////
   // this is how we allow the actions within this class to work without themselves having a logged-in user. //
   ////////////////////////////////////////////////////////////////////////////////////////////////////////////
   private static QSession chickenAndEggSession = null;



   /*******************************************************************************
    ** Getter for special session
    **
    *******************************************************************************/
   private QSession getChickenAndEggSession()
   {
      if(chickenAndEggSession == null)
      {
         ////////////////////////////////////////////////////////////////////////////////
         // if the static field is null, then let's make a new session;                //
         // prime it with all all-access keys; and then set it in the static field.    //
         // and, if 2 threads get in here at the same time, no real harm will be done, //
         // other than creating the session twice, and whoever loses the race, that'll //
         // be the one that stays in the field                                         //
         ////////////////////////////////////////////////////////////////////////////////
         QSession newChickenAndEggSession = new QSession();

         for(String typeName : QContext.getQInstance().getSecurityKeyTypes().keySet())
         {
            QSecurityKeyType keyType = QContext.getQInstance().getSecurityKeyType(typeName);
            if(StringUtils.hasContent(keyType.getAllAccessKeyName()))
            {
               newChickenAndEggSession.withSecurityKeyValue(keyType.getAllAccessKeyName(), true);
            }
         }

         chickenAndEggSession = newChickenAndEggSession;
      }

      return (chickenAndEggSession);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QSession createSession(QInstance qInstance, Map<String, String> context) throws QAuthenticationException
   {
      QInstance contextInstanceBefore = QContext.getQInstance();

      try
      {
         QContext.setQInstance(qInstance);
         this.metaData = (Auth0AuthenticationMetaData) qInstance.getAuthentication();

         String accessToken = null;
         if(CollectionUtils.containsKeyWithNonNullValue(context, SESSION_UUID_KEY))
         {
            /////////////////////////////////////////////////////////////////////////////////////////
            // process a sessionUUID - looks up userSession record - cannot create token this way. //
            /////////////////////////////////////////////////////////////////////////////////////////
            String sessionUUID = context.get(SESSION_UUID_KEY);
            LOG.trace("Creating session from sessionUUID (userSession)", logPair("sessionUUID", maskForLog(sessionUUID)));
            if(sessionUUID != null)
            {
               accessToken = getAccessTokenFromSessionUUID(metaData, sessionUUID);
            }
         }
         else if(CollectionUtils.containsKeyWithNonNullValue(context, ACCESS_TOKEN_KEY))
         {
            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // if the context contains an access token, then create a new session based on that token.                          //
            // todo#authHeader - this else/if should maybe be first, but while we have frontend passing both, we want it second //
            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            accessToken = context.get(ACCESS_TOKEN_KEY);
            QSession qSession = buildAndValidateSession(qInstance, accessToken);

            ////////////////////////////////////////////////////////////////
            // build & store userSession db record, if requested to do so //
            ////////////////////////////////////////////////////////////////
            if(CollectionUtils.containsKeyWithNonNullValue(context, DO_STORE_USER_SESSION_KEY))
            {
               insertUserSession(qInstance, accessToken, qSession);
               LOG.info("Creating session based on input accessToken and creating a userSession", logPair("userId", qSession.getUser().getIdReference()));
            }
            else
            {
               String userName = qSession.getUser() != null ? qSession.getUser().getFullName() : null;
               if(userName != null && !userName.contains("System User"))
               {
                  LOG.info("Creating session based on input accessToken but not creating a userSession", logPair("userName", qSession.getUser().getFullName()));
               }
            }

            //////////////////////////////////////////////////////////////
            // allow customizer to do custom things here, if so desired //
            //////////////////////////////////////////////////////////////
            finalCustomizeSession(qInstance, qSession);

            return (qSession);
         }
         else if(CollectionUtils.containsKeyWithNonNullValue(context, BASIC_AUTH_KEY))
         {
            //////////////////////////////////////////////////////////////////////////////////////
            // Process a basic auth (username:password)                                         //
            // by getting an access token from auth0 (re-using from state provider if possible) //
            //////////////////////////////////////////////////////////////////////////////////////
            AuthAPI auth = AuthAPI.newBuilder(metaData.getBaseUrl(), metaData.getClientId(), metaData.getClientSecret()).build();
            try
            {
               /////////////////////////////////////////////////
               // decode the credentials from the header auth //
               /////////////////////////////////////////////////
               String base64Credentials = context.get(BASIC_AUTH_KEY).trim();
               LOG.trace("Creating session from basicAuthentication", logPair("base64Credentials", maskForLog(base64Credentials)));
               accessToken = getAccessTokenFromBase64BasicAuthCredentials(metaData, auth, base64Credentials);
            }
            catch(Auth0Exception e)
            {
               ////////////////
               // ¯\_(ツ)_/¯ //
               ////////////////
               String message = "Error handling basic authentication: " + e.getMessage();
               LOG.error(message, e);
               throw (new QAuthenticationException(message));
            }
         }
         else if(CollectionUtils.containsKeyWithNonNullValue(context, API_KEY))
         {
            ///////////////////////////////////////////////////////////////////////////////////////
            // process an api key - looks up client application token (creating token if needed) //
            ///////////////////////////////////////////////////////////////////////////////////////
            String apiKey = context.get(API_KEY);
            LOG.trace("Creating session from apiKey (accessTokenTable)", logPair("apiKey", maskForLog(apiKey)));
            if(apiKey != null)
            {
               accessToken = getAccessTokenFromApiKey(metaData, apiKey);
            }
         }

         ///////////////////////////////////////////
         // if token wasn't found by now, give up //
         ///////////////////////////////////////////
         if(accessToken == null)
         {
            LOG.warn(TOKEN_NOT_PROVIDED_ERROR);
            throw (new QAuthenticationException(TOKEN_NOT_PROVIDED_ERROR));
         }

         /////////////////////////////////////////////////////
         // try to build session to see if still valid      //
         // then call method to check more session validity //
         /////////////////////////////////////////////////////
         QSession qSession = buildAndValidateSession(qInstance, accessToken);

         /////////////////////////////////////////////////////////////////////////////////////
         // if we took in a session UUID, make sure that is the UUID on the session object. //
         /////////////////////////////////////////////////////////////////////////////////////
         if(CollectionUtils.containsKeyWithNonNullValue(context, SESSION_UUID_KEY))
         {
            qSession.setUuid(context.get(SESSION_UUID_KEY));
         }

         //////////////////////////////////////////////////////////////
         // allow customizer to do custom things here, if so desired //
         //////////////////////////////////////////////////////////////
         finalCustomizeSession(qInstance, qSession);

         return (qSession);
      }
      catch(QAuthenticationException qae)
      {
         throw (qae);
      }
      catch(JWTDecodeException jde)
      {
         ////////////////////////////////
         // could not decode the token //
         ////////////////////////////////
         LOG.warn(COULD_NOT_DECODE_ERROR, jde);
         throw (new QAuthenticationException(COULD_NOT_DECODE_ERROR));
      }
      catch(TokenExpiredException tee)
      {
         LOG.info(EXPIRED_TOKEN_ERROR, tee);
         throw (new QAuthenticationException(EXPIRED_TOKEN_ERROR));
      }
      catch(JWTVerificationException | JwkException jve)
      {
         ///////////////////////////////////////////
         // token had invalid signature or claims //
         ///////////////////////////////////////////
         LOG.warn(INVALID_TOKEN_ERROR, jve);
         throw (new QAuthenticationException(INVALID_TOKEN_ERROR));
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
      finally
      {
         QContext.setQInstance(contextInstanceBefore);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void finalCustomizeSession(QInstance qInstance, QSession qSession)
   {
      if(getCustomizer() != null)
      {
         QContext.withTemporaryContext(QContext.capture(), () ->
         {
            QContext.setQSession(getChickenAndEggSession());
            getCustomizer().finalCustomizeSession(qInstance, qSession);
         });
      }
   }



   /*******************************************************************************
    ** Insert a session as a new record into userSession table
    *******************************************************************************/
   private void insertUserSession(QInstance qInstance, String accessToken, QSession qSession) throws QException
   {
      CapturedContext capturedContext = QContext.capture();
      try
      {
         QContext.init(qInstance, null);
         QContext.setQSession(getChickenAndEggSession());

         UserSession userSession = new UserSession()
            .withUuid(qSession.getUuid())
            .withUserId(qSession.getUser().getIdReference())
            .withAccessToken(accessToken);

         new InsertAction().execute(new InsertInput(UserSession.TABLE_NAME).withRecordEntity(userSession));
      }
      finally
      {
         QContext.init(capturedContext);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QSession buildAndValidateSession(QInstance qInstance, String accessToken) throws JwkException
   {
      QSession beforeSession = QContext.getQSession();

      try
      {
         QContext.setQSession(getChickenAndEggSession());
         QSession qSession = buildQSessionFromToken(accessToken, qInstance);
         if(isSessionValid(qInstance, qSession))
         {
            return (qSession);
         }

         //////////////////////////////////////////////////////////////////////////////////////////
         // if we make it here it means we have never validated this token or it has been a long //
         // enough duration so we need to re-verify the token                                    //
         //////////////////////////////////////////////////////////////////////////////////////////
         qSession = revalidateTokenAndBuildSession(qInstance, accessToken);

         /////////////////////////////////////////////////////////////////////
         // put now into state so we don't check until next interval passes //
         /////////////////////////////////////////////////////////////////////
         StateProviderInterface spi = getStateProvider();
         SimpleStateKey<String> key = new SimpleStateKey<>(qSession.getIdReference());
         spi.put(key, Instant.now());

         return (qSession);
      }
      finally
      {
         QContext.setQSession(beforeSession);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String getAccessTokenFromBase64BasicAuthCredentials(Auth0AuthenticationMetaData metaData, AuthAPI auth, String base64Credentials) throws Auth0Exception
   {
      ////////////////////////////////////////////////////////////////////////////////////
      // look for a fresh accessToken in the state provider for this set of credentials //
      ////////////////////////////////////////////////////////////////////////////////////
      SimpleStateKey<String> accessTokenStateKey = new SimpleStateKey<>(base64Credentials + ":accessToken");
      SimpleStateKey<String> timestampStateKey   = new SimpleStateKey<>(base64Credentials + ":timestamp");
      StateProviderInterface stateProvider       = getStateProvider();
      Optional<String>       cachedAccessToken   = stateProvider.get(String.class, accessTokenStateKey);
      Optional<Instant>      cachedTimestamp     = stateProvider.get(Instant.class, timestampStateKey);
      if(cachedAccessToken.isPresent() && cachedTimestamp.isPresent())
      {
         if(cachedTimestamp.get().isAfter(Instant.now().minus(1, ChronoUnit.MINUTES)))
         {
            return cachedAccessToken.get();
         }
      }

      //////////////////////////////////////////////////////////////////////////////////
      // not found in cache, make request to auth0 and cache the returned accessToken //
      //////////////////////////////////////////////////////////////////////////////////
      byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
      String credentials = new String(credDecoded, StandardCharsets.UTF_8);

      String accessToken = getAccessTokenForUsernameAndPasswordFromAuth0(metaData, auth, credentials);
      stateProvider.put(accessTokenStateKey, accessToken);
      stateProvider.put(timestampStateKey, Instant.now());
      return (accessToken);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected String getAccessTokenForUsernameAndPasswordFromAuth0(Auth0AuthenticationMetaData metaData, AuthAPI auth, String credentials) throws Auth0Exception
   {
      /////////////////////////////////////
      // call auth0 with a login request //
      /////////////////////////////////////
      Response<TokenHolder> result = auth.login(credentials.split(":")[0], credentials.split(":")[1].toCharArray())
         .setScope("openid email nickname")
         .setAudience(metaData.getAudience())
         .execute();

      return (result.getBody().getAccessToken());
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

      StateProviderInterface spi                     = getStateProvider();
      SimpleStateKey<String> key                     = new SimpleStateKey<>(session.getIdReference());
      Optional<Instant>      lastTimeCheckedOptional = spi.get(Instant.class, key);
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

         try
         {
            LOG.debug("Re-validating token due to validation interval being passed: " + session.getIdReference());
            revalidateTokenAndBuildSession(instance, session.getIdReference());

            //////////////////////////////////////////////////////////////////
            // update the timestamp in state provider, to avoid re-checking //
            //////////////////////////////////////////////////////////////////
            spi.put(key, Instant.now());

            return (true);
         }
         catch(Exception e)
         {
            LOG.warn(INVALID_TOKEN_ERROR, e);
            return (false);
         }
      }

      return (false);
   }



   /*******************************************************************************
    ** makes request to check if a token is still valid and build new qSession if it is
    **
    *******************************************************************************/
   private QSession revalidateTokenAndBuildSession(QInstance qInstance, String accessToken) throws JwkException
   {
      ///////////////////////////////////
      // make call to verify the token //
      ///////////////////////////////////
      validateToken(qInstance, accessToken);
      return (buildQSessionFromToken(accessToken, qInstance));
   }



   /*******************************************************************************
    ** tests validity of a token
    **
    *******************************************************************************/
   private void validateToken(QInstance qInstance, String tokenString) throws JwkException
   {
      this.metaData = (Auth0AuthenticationMetaData) qInstance.getAuthentication();

      DecodedJWT  idToken   = JWT.decode(tokenString);
      JwkProvider provider  = new UrlJwkProvider(metaData.getBaseUrl());
      Jwk         jwk       = provider.get(idToken.getKeyId());
      Algorithm   algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
      JWT.require(algorithm)
         .withIssuer(idToken.getIssuer())
         .build()
         .verify(idToken);
   }



   /*******************************************************************************
    ** extracts info from token creating a QSession
    **
    *******************************************************************************/
   private QSession buildQSessionFromToken(String accessToken, QInstance qInstance) throws JwkException
   {
      ////////////////////////////////////
      // decode and extract the payload //
      ////////////////////////////////////
      DecodedJWT     jwt           = JWT.decode(accessToken);
      Base64.Decoder decoder       = Base64.getUrlDecoder();
      String         payloadString = new String(decoder.decode(jwt.getPayload()));
      JSONObject     payload       = new JSONObject(payloadString);

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////
      // create user object.  look for multiple possible keys in the jwt payload where the name & email may be //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////
      QUser qUser = new QUser();
      qUser.setFullName("Unknown");
      for(String key : List.of("name", "com.kingsrook.qqq.name", "com.kingsrook.qqq.client_name"))
      {
         if(payload.has(key))
         {
            qUser.setFullName(payload.getString(key));
            break;
         }
      }

      for(String key : List.of("email", "com.kingsrook.qqq.email", "sub"))
      {
         if(payload.has(key))
         {
            qUser.setIdReference(payload.getString(key));
            break;
         }
      }

      /////////////////////////////////////////////////////////
      // create session object - link to access token & user //
      /////////////////////////////////////////////////////////
      QSession qSession = new QSession();
      qSession.setIdReference(accessToken);
      qSession.setUser(qUser);

      ////////////////////////////////////////////////////////////////////
      // put the user id reference in security key value for userId key //
      ////////////////////////////////////////////////////////////////////
      addSecurityKeyValueToSession(qSession, "userId", qUser.getIdReference());

      /////////////////////////////////////////////////
      // set permissions in the session from the JWT //
      /////////////////////////////////////////////////
      setPermissionsInSessionFromJwtPayload(payload, qSession);

      ///////////////////////////////////////////////////
      // set security keys in the session from the JWT //
      ///////////////////////////////////////////////////
      setSecurityKeysInSessionFromJwtPayload(qInstance, payload, qSession);

      //////////////////////////////////////////////////////////////
      // allow customizer to do custom things here, if so desired //
      //////////////////////////////////////////////////////////////
      if(getCustomizer() != null)
      {
         getCustomizer().customizeSession(qInstance, qSession, Map.of("jwtPayloadJsonObject", payload));
      }

      return (qSession);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void addSecurityKeyValueToSession(QSession qSession, String key, String value)
   {
      if(getCustomizer() == null)
      {
         ///////////////////////////////////////////////////
         // if there's no customizer, do the direct thing //
         ///////////////////////////////////////////////////
         qSession.withSecurityKeyValue(key, value);
      }
      else
      {
         ///////////////////////////////////////////////////////////
         // else have the customizer add the value to the session //
         ///////////////////////////////////////////////////////////
         getCustomizer().addSecurityKeyValueToSession(qSession, key, value);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static void setPermissionsInSessionFromJwtPayload(JSONObject payload, QSession qSession)
   {
      HashSet<String> permissions = new HashSet<>();
      if(payload.has("permissions"))
      {
         try
         {
            JSONArray jwtPermissions = payload.getJSONArray("permissions");
            for(int i = 0; i < jwtPermissions.length(); i++)
            {
               permissions.add(jwtPermissions.optString(i));
            }
         }
         catch(Exception e)
         {
            LOG.error("Error getting permissions from JWT", e);
         }
      }
      qSession.setPermissions(permissions);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   void setSecurityKeysInSessionFromJwtPayload(QInstance qInstance, JSONObject payload, QSession qSession)
   {
      for(String payloadKey : List.of("com.kingsrook.qqq.app_metadata", "com.kingsrook.qqq.client_metadata"))
      {
         if(!payload.has(payloadKey))
         {
            continue;
         }

         try
         {
            JSONObject  appMetadata             = payload.getJSONObject(payloadKey);
            Set<String> allowedSecurityKeyNames = qInstance.getAllowedSecurityKeyNames();

            //////////////////////////////////////////////////////////////////////////////////
            // for users, they will have a map of securityKeyValues (in their app_metadata) //
            //////////////////////////////////////////////////////////////////////////////////
            JSONObject securityKeyValues = appMetadata.optJSONObject("securityKeyValues");
            if(securityKeyValues != null)
            {
               for(String keyName : securityKeyValues.keySet())
               {
                  setSecurityKeyValuesFromToken(allowedSecurityKeyNames, qSession, keyName, securityKeyValues, keyName);
               }
            }
            else
            {
               //////////////////////////////////////////////////////////////////////////////////////////////////
               // for system-logins, there will be keys prefixed by securityKeyValues: (under client_metadata) //
               //////////////////////////////////////////////////////////////////////////////////////////////////
               for(String appMetaDataKey : appMetadata.keySet())
               {
                  if(appMetaDataKey.startsWith("securityKeyValues:"))
                  {
                     String securityKeyName = appMetaDataKey.replace("securityKeyValues:", "");
                     setSecurityKeyValuesFromToken(allowedSecurityKeyNames, qSession, securityKeyName, appMetadata, appMetaDataKey);
                  }
               }
            }
         }
         catch(Exception e)
         {
            LOG.error("Error getting securityKey values from app_metadata from JWT", e);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void setSecurityKeyValuesFromToken(Set<String> allowedSecurityKeyNames, QSession qSession, String securityKeyName, JSONObject securityKeyValues, String jsonKey)
   {
      if(!allowedSecurityKeyNames.contains(securityKeyName))
      {
         QUser user = qSession.getUser();
         LOG.warn("Unrecognized security key name [" + securityKeyName + "] when creating session for user [" + user + "].  Allowed key names are: " + allowedSecurityKeyNames);
         return;
      }

      JSONArray valueArray = securityKeyValues.optJSONArray(jsonKey);
      if(valueArray != null)
      {
         // todo - types?
         for(int i = 0; i < valueArray.length(); i++)
         {
            Object optValue = valueArray.opt(i);
            if(optValue != null)
            {
               addSecurityKeyValueToSession(qSession, securityKeyName, ValueUtils.getValueAsString(optValue));
            }
         }
      }
      else
      {
         String values = securityKeyValues.optString(jsonKey);
         if(StringUtils.hasContent(values))
         {
            for(String v : values.split(","))
            {
               addSecurityKeyValueToSession(qSession, securityKeyName, v);
            }
         }
      }
   }



   /*******************************************************************************
    ** Load an instance of the appropriate state provider
    **
    *******************************************************************************/
   private static StateProviderInterface getStateProvider()
   {
      // TODO - read this from somewhere in meta data eh?
      return (InMemoryStateProvider.getInstance());
   }



   /*******************************************************************************
    ** make http request to Auth0 for a new access token for an application - e.g.,
    ** with a clientId and clientSecret as params
    **
    *******************************************************************************/
   public JSONObject requestAccessTokenForClientIdAndSecretFromAuth0(Auth0AuthenticationMetaData auth0MetaData, String clientId, String clientSecret) throws AccessTokenException
   {
      ///////////////////////////////////////////////////////////////////
      // make a request to Auth0 using the client_id and client_secret //
      ///////////////////////////////////////////////////////////////////
      try(CloseableHttpClient httpClient = HttpClientBuilder.create().build())
      {
         UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(List.of(
            new BasicNameValuePair("content-type", "application/x-www-form-urlencoded"),
            new BasicNameValuePair("grant_type", "client_credentials"),
            new BasicNameValuePair("audience", auth0MetaData.getAudience()),
            new BasicNameValuePair("client_id", clientId),
            new BasicNameValuePair("client_secret", clientSecret)));

         HttpPost request = new HttpPost(auth0MetaData.getBaseUrl() + "oauth/token");
         request.setEntity(urlEncodedFormEntity);

         try(CloseableHttpResponse response = httpClient.execute(request))
         {
            int    statusCode = response.getStatusLine().getStatusCode();
            String content    = EntityUtils.toString(response.getEntity());

            //////////////////////////////////////
            // if 200OK, return the json object //
            //////////////////////////////////////
            if(statusCode == 200)
            {
               return (JsonUtils.toJSONObject(content));
            }

            //////////////////////////////////////////////////////////////////////////////////////////////////////////
            // if not 200, throw an access token exception with the message and status code of the non-200 response //
            //////////////////////////////////////////////////////////////////////////////////////////////////////////
            throw (new AccessTokenException(content, statusCode));
         }
      }
      catch(AccessTokenException ate)
      {
         throw (ate);
      }
      catch(Exception e)
      {
         throw (new AccessTokenException(e.getMessage(), e));
      }
   }



   /*******************************************************************************
    ** Look up access_token record, return if found.
    **
    *******************************************************************************/
   String lookupActualAccessToken(Auth0AuthenticationMetaData metaData, String qqqAccessToken)
   {
      String   accessToken   = null;
      QSession beforeSession = QContext.getQSession();

      try
      {
         QContext.setQSession(getChickenAndEggSession());

         //////////////////////////////////////////////////////////////////////////////////////
         // try to look up existing auth0 application from database, insert one if not found //
         //////////////////////////////////////////////////////////////////////////////////////
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(metaData.getAccessTokenTableName());
         queryInput.setFilter(new QQueryFilter(new QFilterCriteria(metaData.getQqqAccessTokenField(), QCriteriaOperator.EQUALS, qqqAccessToken)));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);

         if(CollectionUtils.nullSafeHasContents(queryOutput.getRecords()))
         {
            accessToken = queryOutput.getRecords().get(0).getValueString(metaData.getAuth0AccessTokenField());
         }
      }
      catch(Exception e)
      {
         LOG.warn("Could not find Auth0 access token for provided qqq access token", e);
      }
      finally
      {
         QContext.setQSession(beforeSession);
      }

      return (accessToken);
   }



   /*******************************************************************************
    ** Look up access_token from session UUID
    **
    *******************************************************************************/
   private String getAccessTokenFromSessionUUID(Auth0AuthenticationMetaData metaData, String sessionUUID) throws QAuthenticationException
   {
      if(mayMemoize)
      {
         return getAccessTokenFromSessionUUIDMemoization.getResultThrowing(sessionUUID, (String x) ->
            doGetAccessTokenFromSessionUUID(sessionUUID)
         ).orElse(null);
      }
      else
      {
         return (doGetAccessTokenFromSessionUUID(sessionUUID));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String doGetAccessTokenFromSessionUUID(String sessionUUID) throws QAuthenticationException
   {
      String   accessToken   = null;
      QSession beforeSession = QContext.getQSession();

      try
      {
         QContext.setQSession(getChickenAndEggSession());

         ///////////////////////////////////////
         // query for the user session record //
         ///////////////////////////////////////
         QRecord userSessionRecord = new GetAction().executeForRecord(new GetInput(UserSession.TABLE_NAME)
            .withUniqueKey(Map.of("uuid", sessionUUID))
            .withShouldMaskPasswords(false)
            .withShouldOmitHiddenFields(false));

         if(userSessionRecord != null)
         {
            accessToken = userSessionRecord.getValueString("accessToken");

            ////////////////////////////////////////////////////////////
            // decode the accessToken and make sure it is not expired //
            ////////////////////////////////////////////////////////////
            if(accessToken != null)
            {
               DecodedJWT jwt = JWT.decode(accessToken);
               if(jwt.getExpiresAtAsInstant().isBefore(Instant.now()))
               {
                  throw (new QAuthenticationException("accessToken is expired"));
               }
            }
         }
      }
      catch(QAuthenticationException qae)
      {
         throw (qae);
      }
      catch(Exception e)
      {
         LOG.warn("Error looking up userSession by sessionUUID", e);
         throw (new QAuthenticationException("Error looking up userSession by sessionUUID", e));
      }
      finally
      {
         QContext.setQSession(beforeSession);
      }

      return (accessToken);
   }



   /*******************************************************************************
    ** Look up access_token from api key
    **
    *******************************************************************************/
   String getAccessTokenFromApiKey(Auth0AuthenticationMetaData metaData, String apiKey)
   {
      String   accessToken   = null;
      QSession beforeSession = QContext.getQSession();

      try
      {
         QContext.setQSession(getChickenAndEggSession());

         //////////////////////////////////////////////////////////////////////////////////////
         // try to look up existing auth0 application from database, insert one if not found //
         //////////////////////////////////////////////////////////////////////////////////////
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(metaData.getAccessTokenTableName());
         queryInput.setShouldOmitHiddenFields(false);
         queryInput.setShouldMaskPasswords(false);
         queryInput.setFilter(new QQueryFilter(new QFilterCriteria(metaData.getQqqApiKeyField(), QCriteriaOperator.EQUALS, apiKey)));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);

         if(CollectionUtils.nullSafeHasContents(queryOutput.getRecords()))
         {
            QRecord clientAuth0ApiKey = queryOutput.getRecords().get(0);
            accessToken = clientAuth0ApiKey.getValueString(metaData.getAuth0AccessTokenField());

            ////////////////////////////////////////////////////////////
            // decode the accessToken and make sure it is not expired //
            ////////////////////////////////////////////////////////////
            boolean needNewToken = true;
            if(StringUtils.hasContent(accessToken))
            {
               DecodedJWT jwt     = JWT.decode(accessToken);
               String     payload = jwt.getPayload();
               if(jwt.getExpiresAtAsInstant().isAfter(Instant.now()))
               {
                  needNewToken = false;
               }
            }

            if(needNewToken)
            {
               ///////////////////////////////////////////////
               // get id/secret from the application record //
               ///////////////////////////////////////////////
               GetInput getInput = new GetInput();
               getInput.setTableName(metaData.getClientAuth0ApplicationTableName());
               getInput.setShouldOmitHiddenFields(false);
               getInput.setShouldMaskPasswords(false);
               getInput.setPrimaryKey(clientAuth0ApiKey.getValueString(metaData.getClientAuth0ApplicationIdField()));

               ///////////////////////////////
               // create a new access token //
               ///////////////////////////////
               QRecord clientAuth0Application = new GetAction().execute(getInput).getRecord();
               String  clientId               = clientAuth0Application.getValueString(metaData.getAuth0ClientIdField());
               String  clientSecret           = clientAuth0Application.getValueString(metaData.getAuth0ClientSecretField());

               /////////////////////////////////////////////////////////////////////////////////////////////////
               // request access token from auth0 if exception is not thrown, that means 200OK, we want to    //
               // store the actual access token in the database, and return a unique value                    //
               // back to the user which will be what they use on subsequent requests (because token too big) //
               /////////////////////////////////////////////////////////////////////////////////////////////////
               JSONObject accessTokenData = requestAccessTokenForClientIdAndSecretFromAuth0(metaData, clientId, clientSecret);

               Integer expiresInSeconds = accessTokenData.getInt("expires_in");
               accessToken = accessTokenData.getString("access_token");

               //////////////////////////////////////////////////////////
               // update the api key record and store new access token //
               //////////////////////////////////////////////////////////
               clientAuth0ApiKey.setValue(metaData.getAuth0AccessTokenField(), accessToken);
               UpdateInput updateInput = new UpdateInput();
               updateInput.setTableName(metaData.getAccessTokenTableName());
               updateInput.setRecords(List.of(clientAuth0ApiKey));
               new UpdateAction().execute(updateInput);
            }
         }
      }
      catch(Exception e)
      {
         LOG.warn("Could not find Auth0 access token for provided qqq API Key", e);
      }
      finally
      {
         QContext.setQSession(beforeSession);
      }

      return (accessToken);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static String maskForLog(String input)
   {
      if(input == null)
      {
         return (null);
      }

      if(input.length() < 8)
      {
         return ("******");
      }
      else
      {
         return (input.substring(0, 6) + "******");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QAuthenticationModuleCustomizerInterface getCustomizer()
   {
      try
      {
         if(!customizerHasBeenRequested)
         {
            customizerHasBeenRequested = true;

            if(this.metaData == null)
            {
               this.metaData = (Auth0AuthenticationMetaData) QContext.getQInstance().getAuthentication();
            }

            if(this.metaData.getCustomizer() != null)
            {
               this._customizer = QCodeLoader.getAdHoc(QAuthenticationModuleCustomizerInterface.class, this.metaData.getCustomizer());
            }
         }

         return (this._customizer);
      }
      catch(Exception e)
      {
         ////////////////////////
         // should this throw? //
         ////////////////////////
         LOG.warn("Error getting customizer.", e);
         return (null);
      }
   }



   /*******************************************************************************
    ** e.g., if a scheduled job needs to run as a user (say, a report)...
    *******************************************************************************/
   @Override
   public QSession createAutomatedSessionForUser(QInstance qInstance, Serializable userId) throws QAuthenticationException
   {
      QSession automatedSessionForUser = QAuthenticationModuleInterface.super.createAutomatedSessionForUser(qInstance, userId);
      if(getCustomizer() != null)
      {
         getCustomizer().customizeAutomatedSessionForUser(qInstance, automatedSessionForUser, userId);
      }
      return (automatedSessionForUser);
   }
}
