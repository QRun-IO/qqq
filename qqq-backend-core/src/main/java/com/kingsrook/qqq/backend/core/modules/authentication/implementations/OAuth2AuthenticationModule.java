/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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


import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleInterface;
import com.kingsrook.qqq.backend.core.modules.authentication.QSessionStoreHelper;
import com.kingsrook.qqq.backend.core.modules.authentication.QSessionStoreRegistry;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.model.UserSession;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.model.UserSessionRefreshToken;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.memoization.Memoization;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.GeneralException;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.RefreshTokenGrant;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.TokenRevocationRequest;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import org.json.JSONObject;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Implementation of OAuth2 authentication.
 **
 ** Sessions are stored in the userSession table (keyed by a uuid, which is what
 ** the client holds, in a cookie).  If the provider issues a refresh token (e.g.,
 ** the offline_access scope was granted) and the instance defines the refresh-
 ** token table (see UserSessionRefreshTokenMetaDataProducer), the refresh token
 ** is stored server-side and used to renew the access token when it expires (or
 ** is within refreshTokenSkewSeconds of expiring), so the session survives.
 ** Refresh tokens are never sent to clients, put on the QSession, or logged.
 *******************************************************************************/
public class OAuth2AuthenticationModule implements QAuthenticationModuleInterface
{
   private static final QLogger LOG = QLogger.getLogger(OAuth2AuthenticationModule.class);

   private static boolean mayMemoize = true;

   private static final Memoization<String, String> getAccessTokenFromSessionUUIDMemoization = new Memoization<String, String>()
      .withTimeout(Duration.of(1, ChronoUnit.MINUTES))
      .withMaxSize(1000);

   private static final Memoization<String, OIDCProviderMetadata> oidcProviderMetadataMemoization = new Memoization<String, OIDCProviderMetadata>()
      .withMayStoreNullValues(false);

   ///////////////////////////////////////////////////////////////////////////////
   // striped locks, so that concurrent requests for the same session perform  //
   // at most one refresh (the others wait, then find the rotated token).      //
   // striping keeps memory bounded, versus a lock-per-session map.            //
   ///////////////////////////////////////////////////////////////////////////////
   private static final int             REFRESH_LOCK_STRIPES = 64;
   private static final ReentrantLock[] REFRESH_LOCKS        = new ReentrantLock[REFRESH_LOCK_STRIPES];

   private static final int TOKEN_ENDPOINT_TIMEOUT_MILLIS = 10_000;

   static
   {
      for(int i = 0; i < REFRESH_LOCK_STRIPES; i++)
      {
         REFRESH_LOCKS[i] = new ReentrantLock();
      }
   }

   //////////////////////////////////////////////////////////////////////////////////
   // do not use this var directly - rather - always call the getCustomizer method //
   //////////////////////////////////////////////////////////////////////////////////
   private QAuthenticationModuleCustomizerInterface _customizer                = null;
   private boolean                                  customizerHasBeenRequested = false;



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public QSession createSession(QInstance qInstance, Map<String, String> context) throws QAuthenticationException
   {
      try
      {
         OAuth2AuthenticationMetaData oauth2MetaData = (OAuth2AuthenticationMetaData) qInstance.getAuthentication();

         if(context.containsKey("code") && context.containsKey("state"))
         {
            ///////////////////////////////////////////////////////////////////////
            // handle a callback to initially auth a user for a traditional      //
            // (non-js) site - where the code & state params come to the backend //
            ///////////////////////////////////////////////////////////////////////
            AuthorizationCode code = new AuthorizationCode(context.get("code"));

            /////////////////////////////////////////
            // verify the state in our state table //
            /////////////////////////////////////////
            AtomicReference<String> redirectUri = new AtomicReference<>(null);
            QContext.withTemporaryContext(new CapturedContext(qInstance, new QSystemUserSession()), () ->
            {
               QRecord redirectStateRecord = GetAction.execute(oauth2MetaData.getRedirectStateTableName(), Map.of("state", context.get("state")));
               if(redirectStateRecord == null)
               {
                  LOG.warn("OAuth callback state not found", logPair("state", context.get("state")));
                  throw (new QAuthenticationException("State not found"));
               }
               redirectUri.set(redirectStateRecord.getValueString("redirectUri"));
            });

            URI                    redirectURI       = new URI(redirectUri.get());
            ClientSecretBasic      clientSecretBasic = new ClientSecretBasic(new ClientID(oauth2MetaData.getClientId()), new Secret(oauth2MetaData.getClientSecret()));
            AuthorizationCodeGrant codeGrant         = new AuthorizationCodeGrant(code, redirectURI);

            URI          tokenEndpoint = getOIDCProviderMetadata(oauth2MetaData).getTokenEndpointURI();
            Scope        scope         = new Scope(oauth2MetaData.getScopes());
            TokenRequest tokenRequest  = new TokenRequest(tokenEndpoint, clientSecretBasic, codeGrant, scope);

            return createSessionFromTokenRequest(tokenRequest);
         }
         else if(context.containsKey("code") && context.containsKey("redirectUri") && context.containsKey("codeVerifier"))
         {
            ////////////////////////////////////////////////////////////////////////////////
            // handle a call down to this backend code to initially auth a user for an    //
            // SPA that received a code (where the javascript generated the codeVerifier) //
            ////////////////////////////////////////////////////////////////////////////////
            AuthorizationCode  code         = new AuthorizationCode(context.get("code"));
            URI                callback     = new URI(context.get("redirectUri"));
            CodeVerifier       codeVerifier = new CodeVerifier(context.get("codeVerifier"));
            AuthorizationGrant codeGrant    = new AuthorizationCodeGrant(code, callback, codeVerifier);

            ClientID             clientID     = new ClientID(oauth2MetaData.getClientId());
            Secret               clientSecret = new Secret(oauth2MetaData.getClientSecret());
            ClientAuthentication clientAuth   = new ClientSecretBasic(clientID, clientSecret);

            URI          tokenEndpoint = getOIDCProviderMetadata(oauth2MetaData).getTokenEndpointURI();
            Scope        scope         = new Scope(oauth2MetaData.getScopes());
            TokenRequest tokenRequest  = new TokenRequest(tokenEndpoint, clientAuth, codeGrant, scope);

            return createSessionFromTokenRequest(tokenRequest);
         }
         else if(context.containsKey("sessionUUID") || context.containsKey("sessionId") || context.containsKey("uuid"))
         {
            //////////////////////////////////////////////////////////////////////
            // handle a "normal" request, where we aren't opening a new session //
            // per-se, but instead are looking for one in our userSession table //
            //////////////////////////////////////////////////////////////////////
            String uuid = Objects.requireNonNullElseGet(context.get("sessionUUID"), () ->
               Objects.requireNonNullElseGet(context.get("sessionId"), () ->
                  context.get("uuid")));

            ///////////////////////////////////////////////////////////////////////////
            // if session store is enabled, try to load cached session first         //
            // this avoids re-deriving security keys and other expensive operations  //
            ///////////////////////////////////////////////////////////////////////////
            if(Boolean.TRUE.equals(oauth2MetaData.getSessionStoreEnabled()))
            {
               Optional<QSession> cachedSession = QSessionStoreHelper.loadAndTouchSession(uuid);
               if(cachedSession.isPresent())
               {
                  return cachedSession.get();
               }
            }

            String   accessToken = getAccessTokenFromSessionUUID(uuid);
            QSession session     = createSessionFromToken(accessToken);
            session.setUuid(uuid);
            //////////////////////////////////////////////////////////////////////////////
            // Update idReference to match the UUID from the context so getIdReference() //
            // returns the correct session identifier for cookie management               //
            //////////////////////////////////////////////////////////////////////////////
            session.setIdReference(uuid);

            //////////////////////////////////////////////////////////////////
            // todo - do we need to validate its age or ping the provider?? //
            //////////////////////////////////////////////////////////////////

            //////////////////////////////////////////////////////////////
            // allow customizer to do custom things here, if so desired //
            //////////////////////////////////////////////////////////////
            finalCustomizeSession(qInstance, session);

            ///////////////////////////////////////////////////////////////////////
            // if session store is enabled, store the session for future use    //
            // this happens after finalCustomizeSession so security keys cached //
            ///////////////////////////////////////////////////////////////////////
            if(Boolean.TRUE.equals(oauth2MetaData.getSessionStoreEnabled()))
            {
               QSessionStoreHelper.storeSession(uuid, session, QSessionStoreHelper.getDefaultTtl());
            }

            return (session);
         }
         else
         {
            String message = "Did not receive recognized values in context for creating session";
            LOG.warn(message, logPair("contextKeys", context.keySet()));
            throw (new QAuthenticationException(message));
         }
      }
      catch(QAuthenticationException qae)
      {
         throw (qae);
      }
      catch(Exception e)
      {
         LOG.warn("Failed to create session", e, logPair("contextKeys", context.keySet()));
         throw (new QAuthenticationException("Failed to create session (token)", e));
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private QSession createSessionFromTokenRequest(TokenRequest tokenRequest) throws ParseException, IOException, QException
   {
      ///////////////////////////////////////////////////////////////////////////
      // log token request details before sending to aid debugging auth issues //
      ///////////////////////////////////////////////////////////////////////////
      LOG.debug("Sending token request",
         logPair("tokenEndpoint", tokenRequest.getEndpointURI()),
         logPair("clientId", tokenRequest.getClientAuthentication() != null ? tokenRequest.getClientAuthentication().getClientID() : null),
         logPair("grantType", tokenRequest.getAuthorizationGrant() != null ? tokenRequest.getAuthorizationGrant().getType() : null));

      TokenResponse tokenResponse = TokenResponse.parse(tokenRequest.toHTTPRequest().send());

      if(tokenResponse.indicatesSuccess())
      {
         LOG.debug("Token request succeeded", logPair("tokenEndpoint", tokenRequest.getEndpointURI()));

         AccessToken  accessToken  = tokenResponse.toSuccessResponse().getTokens().getAccessToken();
         RefreshToken refreshToken = tokenResponse.toSuccessResponse().getTokens().getRefreshToken();

         ///////////////////////////////////////////////////////////////////////
         // extract id token claims if available (for OIDC flows)             //
         // this gives customizers access to claims that OIDC providers       //
         // typically place in the ID token (groups, permissions, custom claims) //
         ///////////////////////////////////////////////////////////////////////
         JSONObject idTokenPayload = null;
         try
         {
            OIDCTokens oidcTokens = tokenResponse.toSuccessResponse().getTokens().toOIDCTokens();
            if(oidcTokens != null && oidcTokens.getIDToken() != null)
            {
               idTokenPayload = new JSONObject(oidcTokens.getIDToken().getJWTClaimsSet().toJSONObject());
            }
         }
         catch(Exception e)
         {
            ////////////////////////////////////////////////////////////////////
            // not an OIDC flow or ID token parsing failed - continue without //
            ////////////////////////////////////////////////////////////////////
            LOG.debug("Could not extract ID token from token response", e);
         }

         QSession session = createSessionFromToken(accessToken.getValue(), accessToken.getValue(), idTokenPayload);
         insertUserSession(accessToken.getValue(), session);

         /////////////////////////////////////////////////////////////////////////
         // if the provider issued a refresh token (e.g., offline_access), keep //
         // it server-side, so the session can outlive the access token.  it    //
         // is never put on the session, so it never reaches a client.          //
         /////////////////////////////////////////////////////////////////////////
         if(refreshToken != null)
         {
            storeRefreshToken(session.getUuid(), refreshToken, tokenResponse.toSuccessResponse());
         }

         //////////////////////////////////////////////////////////////
         // allow customizer to do custom things here, if so desired //
         //////////////////////////////////////////////////////////////
         finalCustomizeSession(QContext.getQInstance(), session);

         ///////////////////////////////////////////////////////////////////////
         // if session store is enabled, store the session for future use    //
         // this happens after finalCustomizeSession so security keys cached //
         ///////////////////////////////////////////////////////////////////////
         OAuth2AuthenticationMetaData oauth2MetaData = (OAuth2AuthenticationMetaData) QContext.getQInstance().getAuthentication();
         if(Boolean.TRUE.equals(oauth2MetaData.getSessionStoreEnabled()))
         {
            QSessionStoreHelper.storeSession(session.getUuid(), session, QSessionStoreHelper.getDefaultTtl());
         }

         return (session);
      }
      else
      {
         ErrorObject errorObject = tokenResponse.toErrorResponse().getErrorObject();
         LOG.warn("Token request failed",
            logPair("code", errorObject.getCode()),
            logPair("description", errorObject.getDescription()),
            logPair("httpStatus", errorObject.getHTTPStatusCode()),
            logPair("tokenEndpoint", tokenRequest.getEndpointURI()),
            logPair("clientId", tokenRequest.getClientAuthentication() != null ? tokenRequest.getClientAuthentication().getClientID() : null));
         throw (new QAuthenticationException(errorObject.getDescription()));
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public boolean isSessionValid(QInstance instance, QSession session)
   {
      if(session instanceof QSystemUserSession)
      {
         return (true);
      }

      try
      {
         ///////////////////////////////////////////////////////////////////////////
         // note - looking up the access token refreshes it (if it is expired or  //
         // near expiry, and a refresh token is stored), so a refreshable session //
         // stays valid here.                                                     //
         ///////////////////////////////////////////////////////////////////////////
         String     accessToken = getAccessTokenFromSessionUUID(session.getUuid());
         DecodedJWT jwt         = JWT.decode(accessToken);
         if(jwt.getExpiresAtAsInstant().isBefore(Instant.now()))
         {
            LOG.debug("accessToken is expired", logPair("sessionUUID", session.getUuid()));
            return (false);
         }

         return true;
      }
      catch(QAuthenticationException e)
      {
         return (false);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public String getLoginRedirectUrl(String originalUrl) throws QAuthenticationException
   {
      try
      {
         QInstance                    qInstance      = QContext.getQInstance();
         OAuth2AuthenticationMetaData oauth2MetaData = (OAuth2AuthenticationMetaData) qInstance.getAuthentication();
         String                       authUrl        = getOIDCProviderMetadata(oauth2MetaData).getAuthorizationEndpointURI().toString();

         QTableMetaData stateTable = QContext.getQInstance().getTable(oauth2MetaData.getRedirectStateTableName());
         if(stateTable == null)
         {
            LOG.error("OAuth redirect state table not defined in QInstance", logPair("tableName", oauth2MetaData.getRedirectStateTableName()));
            throw (new QAuthenticationException("The table specified as the oauthRedirectStateTableName [" + oauth2MetaData.getRedirectStateTableName() + "] is not defined in the QInstance"));
         }

         ///////////////////////////////////////////////////////////////////
         // generate a secure state, of either default length (32 bytes), //
         // or at a size (base64 encoded) that fits in the state table    //
         ///////////////////////////////////////////////////////////////////
         Integer stateStringLength = stateTable.getField("state").getMaxLength();
         State   state             = stateStringLength == null ? new State(32) : new State((stateStringLength / 4) * 3);
         String  stateValue        = state.getValue();

         /////////////////////////////
         // insert the state record //
         /////////////////////////////
         QContext.withTemporaryContext(new CapturedContext(qInstance, new QSystemUserSession()), () ->
         {
            QRecord insertedState = new InsertAction().execute(new InsertInput(oauth2MetaData.getRedirectStateTableName()).withRecord(new QRecord()
               .withValue("state", stateValue)
               .withValue("redirectUri", originalUrl))).getRecords().get(0);
            if(CollectionUtils.nullSafeHasContents(insertedState.getErrors()))
            {
               LOG.warn("Error storing OAuth redirect state", logPair("errors", insertedState.getErrorsAsString()));
               throw (new QAuthenticationException("Error storing redirect state: " + insertedState.getErrorsAsString()));
            }
         });

         return authUrl
            + "?client_id=" + URLEncoder.encode(oauth2MetaData.getClientId(), StandardCharsets.UTF_8)
            + "&redirect_uri=" + URLEncoder.encode(originalUrl, StandardCharsets.UTF_8)
            + "&response_type=code"
            + "&scope=" + URLEncoder.encode(oauth2MetaData.getScopes(), StandardCharsets.UTF_8)
            + "&state=" + URLEncoder.encode(state.getValue(), StandardCharsets.UTF_8);
      }
      catch(Exception e)
      {
         LOG.warn("Error getting login redirect url", e);
         throw (new QAuthenticationException("Error getting login redirect url", e));
      }
   }



   /***************************************************************************
    ** Create session from access token only (for session resume path)
    ***************************************************************************/
   private QSession createSessionFromToken(String accessToken) throws QException
   {
      return createSessionFromToken(accessToken, null, null);
   }



   /***************************************************************************
    ** Create session from access token with optional tokens for customizer.
    **
    ** @param accessToken the JWT access token to decode for session info
    ** @param accessTokenValue raw access token string to pass to customizer
    **        (for calling userinfo endpoint). May be null on session resume.
    ** @param idTokenPayload decoded ID token claims to pass to customizer.
    **        May be null if not an OIDC flow or on session resume.
    ***************************************************************************/
   private QSession createSessionFromToken(String accessToken, String accessTokenValue, JSONObject idTokenPayload) throws QException
   {
      DecodedJWT     jwt           = JWT.decode(accessToken);
      Base64.Decoder decoder       = Base64.getUrlDecoder();
      String         payloadString = new String(decoder.decode(jwt.getPayload()));
      JSONObject     payload       = new JSONObject(payloadString);

      QSession session = new QSession();
      QUser    user    = new QUser();
      session.setUser(user);

      user.setFullName("Unknown");
      String sub   = Objects.requireNonNullElseGet(payload.optString("sub", null), () -> payload.optString("email", null));
      String email = payload.optString("email", sub);
      String name  = payload.optString("name", sub);

      user.setIdReference(sub);
      user.setFullName(name);

      ////////////////////////////////////////////////////////////
      // todo wip - this needs to be much better standardized w/ fe //
      ////////////////////////////////////////////////////////////
      session.withValueForFrontend("user", new HashMap<>(Map.of("name", name, "email", email)));

      //////////////////////////////////////////////////////////////////////////////
      // Set the session's idReference to its UUID so getIdReference() returns    //
      // a non-null value for use in cookies and session management               //
      //////////////////////////////////////////////////////////////////////////////
      session.setIdReference(session.getUuid());

      //////////////////////////////////////////////////////////////
      // allow customizer to do custom things here, if so desired //
      //////////////////////////////////////////////////////////////
      if(getCustomizer() != null)
      {
         Map<String, Object> context = new HashMap<>();
         context.put("jwtPayloadJsonObject", payload);

         ///////////////////////////////////////////////////////////////////////
         // pass access token string so customizer can call userinfo endpoint //
         ///////////////////////////////////////////////////////////////////////
         if(accessTokenValue != null)
         {
            context.put("accessToken", accessTokenValue);
         }

         ////////////////////////////////////////////////////////////////////
         // pass ID token claims if available (contains custom OIDC claims) //
         ////////////////////////////////////////////////////////////////////
         if(idTokenPayload != null)
         {
            context.put("idToken", idTokenPayload);
         }

         getCustomizer().customizeSession(QContext.getQInstance(), session, context);
      }

      return session;
   }



   /*******************************************************************************
    ** Insert a session as a new record into userSession table
    *******************************************************************************/
   private void insertUserSession(String accessToken, QSession qSession) throws QException
   {
      CapturedContext capturedContext = QContext.capture();
      try
      {
         QContext.init(capturedContext.qInstance(), new QSystemUserSession());

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



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public QSession createAutomatedSessionForUser(QInstance qInstance, Serializable userId) throws QAuthenticationException
   {
      QSession session = QAuthenticationModuleInterface.super.createAutomatedSessionForUser(qInstance, userId);
      if(getCustomizer() != null)
      {
         getCustomizer().customizeAutomatedSessionForUser(qInstance, session, userId);
      }
      return session;
   }



   /*******************************************************************************
    ** Look up access_token from session UUID
    **
    *******************************************************************************/
   private String getAccessTokenFromSessionUUID(String sessionUUID) throws QAuthenticationException
   {
      if(mayMemoize)
      {
         String accessToken = getAccessTokenFromSessionUUIDMemoization.getResultThrowing(sessionUUID, (String x) ->
            doGetAccessTokenFromSessionUUID(sessionUUID)
         ).orElse(null);

         //////////////////////////////////////////////////////////////////////////
         // a memoized token may have reached its refresh window since it was    //
         // cached - if so, forget it and go back to the store (which refreshes  //
         // when possible), rather than letting the session lapse.               //
         //////////////////////////////////////////////////////////////////////////
         if(accessToken == null || !isTokenExpiredOrNearExpiry(accessToken, getRefreshSkewSeconds()))
         {
            return (accessToken);
         }

         getAccessTokenFromSessionUUIDMemoization.clearKey(sessionUUID);
         accessToken = doGetAccessTokenFromSessionUUID(sessionUUID);
         if(accessToken != null)
         {
            getAccessTokenFromSessionUUIDMemoization.storeResult(sessionUUID, accessToken);
         }
         return (accessToken);
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
         QContext.setQSession(new QSystemUserSession());

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
            String storedUserId = userSessionRecord.getValueString("userId");

            /////////////////////////////////////////////////////////////////////
            // if the access token is expired (or about to be), try to refresh //
            // it using a stored refresh token; if that isn't possible and the //
            // token is expired, the session is over (below).                  //
            /////////////////////////////////////////////////////////////////////
            if(accessToken != null && isTokenExpiredOrNearExpiry(accessToken, getRefreshSkewSeconds()))
            {
               String refreshedAccessToken = refreshAccessToken(sessionUUID);
               if(refreshedAccessToken != null)
               {
                  accessToken = refreshedAccessToken;
               }
            }

            ////////////////////////////////////////////////////////////
            // decode the accessToken and make sure it is not expired //
            ////////////////////////////////////////////////////////////
            if(accessToken != null)
            {
               DecodedJWT jwt = JWT.decode(accessToken);
               if(jwt.getExpiresAtAsInstant().isBefore(Instant.now()))
               {
                  LOG.warn("Session accessToken is expired", logPair("sessionUUID", sessionUUID));
                  throw (new QAuthenticationException("accessToken is expired"));
               }

               ///////////////////////////////////////////////////////////////////
               // validate that the token's identity matches the stored userId  //
               // this prevents session hijacking if data becomes inconsistent  //
               // prefer sub (OIDC standard, guaranteed unique) over email      //
               ///////////////////////////////////////////////////////////////////
               Base64.Decoder decoder       = Base64.getUrlDecoder();
               String         payloadString = new String(decoder.decode(jwt.getPayload()));
               JSONObject     payload       = new JSONObject(payloadString);
               String         tokenIdentity = Objects.requireNonNullElseGet(payload.optString("sub", null), () -> payload.optString("email", null));

               if(storedUserId != null && tokenIdentity != null && !storedUserId.equals(tokenIdentity))
               {
                  LOG.warn("Session userId mismatch", logPair("sessionUUID", sessionUUID), logPair("storedUserId", storedUserId), logPair("tokenIdentity", tokenIdentity));
                  throw (new QAuthenticationException("Session identity mismatch"));
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



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public boolean usesSessionIdCookie()
   {
      return (true);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private OIDCProviderMetadata getOIDCProviderMetadata(OAuth2AuthenticationMetaData oAuth2AuthenticationMetaData) throws GeneralException, IOException
   {
      return oidcProviderMetadataMemoization.getResult(oAuth2AuthenticationMetaData.getName(), (name ->
      {
         Issuer               issuer   = new Issuer(oAuth2AuthenticationMetaData.getBaseUrl());
         OIDCProviderMetadata metadata = OIDCProviderMetadata.resolve(issuer);
         return (metadata);
      })).orElseThrow(() -> new GeneralException("Could not resolve OIDCProviderMetadata for " + oAuth2AuthenticationMetaData.getName()));
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

            OAuth2AuthenticationMetaData oauth2MetaData = (OAuth2AuthenticationMetaData) QContext.getQInstance().getAuthentication();

            if(oauth2MetaData.getCustomizer() != null)
            {
               _customizer = QCodeLoader.getAdHoc(QAuthenticationModuleCustomizerInterface.class, oauth2MetaData.getCustomizer());
            }
         }

         return (_customizer);
      }
      catch(Exception e)
      {
         LOG.warn("Error getting customizer", e);
         return (null);
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
            QContext.setQSession(new QSystemUserSession());
            getCustomizer().finalCustomizeSession(qInstance, qSession);
         });
      }
   }



   /***************************************************************************
    ** Logout a session by deleting it from the database and clearing the
    ** memoization cache.
    ***************************************************************************/
   @Override
   public void logout(QInstance qInstance, String sessionUUID)
   {
      if(sessionUUID == null)
      {
         return;
      }

      QSession beforeSession = QContext.getQSession();
      try
      {
         QContext.setQSession(new QSystemUserSession());

         /////////////////////////////////////////////////////////////////////
         // revoke (when the provider advertises a revocation endpoint) and //
         // delete any stored refresh token, before the session itself      //
         /////////////////////////////////////////////////////////////////////
         removeStoredRefreshToken(qInstance, sessionUUID, true);

         /////////////////////////////////////////////
         // delete the session record from database //
         /////////////////////////////////////////////
         new DeleteAction().execute(new DeleteInput(UserSession.TABLE_NAME)
            .withQueryFilter(new QQueryFilter(new QFilterCriteria("uuid", QCriteriaOperator.EQUALS, sessionUUID))));

         ///////////////////////////////////////////
         // clear the session from memoization cache //
         ///////////////////////////////////////////
         clearAccessTokenCache(sessionUUID);

         LOG.debug("Logged out session", logPair("sessionUUID", sessionUUID));
      }
      catch(Exception e)
      {
         LOG.warn("Error during logout", e, logPair("sessionUUID", sessionUUID));
      }
      finally
      {
         QContext.setQSession(beforeSession);
      }
   }



   /*******************************************************************************
    ** Get the OAuth2 meta-data from the current QInstance, or null if the instance's
    ** authentication isn't OAuth2.
    *******************************************************************************/
   private static OAuth2AuthenticationMetaData getOAuth2MetaData(QInstance qInstance)
   {
      if(qInstance != null && qInstance.getAuthentication() instanceof OAuth2AuthenticationMetaData oauth2MetaData)
      {
         return (oauth2MetaData);
      }
      return (null);
   }



   /*******************************************************************************
    ** Get the name of the table used to store refresh tokens - but only if that
    ** table is defined in the instance (defining it is how an app opts in to
    ** refresh-token storage) - else null.
    *******************************************************************************/
   private static String getRefreshTokenTableName(QInstance qInstance)
   {
      OAuth2AuthenticationMetaData oauth2MetaData = getOAuth2MetaData(qInstance);
      if(oauth2MetaData == null)
      {
         return (null);
      }

      String tableName = Objects.requireNonNullElse(oauth2MetaData.getRefreshTokenTableName(), UserSessionRefreshToken.TABLE_NAME);
      return (qInstance.getTable(tableName) == null ? null : tableName);
   }



   /*******************************************************************************
    ** Get the configured refresh skew (seconds before expiry at which an access
    ** token is refreshed), or 0 if refreshing is disabled.
    *******************************************************************************/
   private static int getRefreshSkewSeconds()
   {
      OAuth2AuthenticationMetaData oauth2MetaData = getOAuth2MetaData(QContext.getQInstance());
      if(oauth2MetaData == null || Boolean.FALSE.equals(oauth2MetaData.getRefreshTokensEnabled()))
      {
         return (0);
      }
      return (Objects.requireNonNullElse(oauth2MetaData.getRefreshTokenSkewSeconds(), OAuth2AuthenticationMetaData.DEFAULT_REFRESH_TOKEN_SKEW_SECONDS));
   }



   /*******************************************************************************
    ** Check if a JWT access token is expired, or will expire within skewSeconds.
    ** Tokens without an exp claim are treated as not expiring.
    *******************************************************************************/
   private static boolean isTokenExpiredOrNearExpiry(String accessToken, int skewSeconds)
   {
      Instant expiresAt = JWT.decode(accessToken).getExpiresAtAsInstant();
      return (expiresAt != null && expiresAt.isBefore(Instant.now().plusSeconds(skewSeconds)));
   }



   /*******************************************************************************
    ** Store a newly issued refresh token for a session, if refresh tokens are
    ** enabled and the refresh-token table is defined.  The token itself is never
    ** logged nor placed on the QSession.
    *******************************************************************************/
   private void storeRefreshToken(String sessionUuid, RefreshToken refreshToken, AccessTokenResponse tokenResponse) throws QException
   {
      QInstance                    qInstance      = QContext.getQInstance();
      OAuth2AuthenticationMetaData oauth2MetaData = getOAuth2MetaData(qInstance);
      if(oauth2MetaData == null || Boolean.FALSE.equals(oauth2MetaData.getRefreshTokensEnabled()))
      {
         return;
      }

      String tableName = getRefreshTokenTableName(qInstance);
      if(tableName == null)
      {
         LOG.debug("Provider issued a refresh token, but no refresh-token table is defined, so it is not being stored",
            logPair("refreshTokenTableName", Objects.requireNonNullElse(oauth2MetaData.getRefreshTokenTableName(), UserSessionRefreshToken.TABLE_NAME)));
         return;
      }

      UserSessionRefreshToken userSessionRefreshToken = new UserSessionRefreshToken()
         .withSessionUuid(sessionUuid)
         .withRefreshToken(refreshToken.getValue())
         .withRefreshTokenExpiresAt(getRefreshTokenExpiresAt(tokenResponse))
         .withSessionStartedAt(Instant.now());

      QContext.withTemporaryContext(new CapturedContext(qInstance, new QSystemUserSession()), () ->
      {
         QRecord inserted = new InsertAction().execute(new InsertInput(tableName).withRecordEntity(userSessionRefreshToken)).getRecords().get(0);
         if(CollectionUtils.nullSafeHasContents(inserted.getErrors()))
         {
            ////////////////////////////////////////////////////////////////////
            // not fatal for the login - the session just won't be refreshable //
            ////////////////////////////////////////////////////////////////////
            LOG.warn("Error storing refresh token for session", logPair("sessionUUID", sessionUuid), logPair("errors", inserted.getErrorsAsString()));
         }
      });
   }



   /*******************************************************************************
    ** If the provider told us when the refresh token expires (non-standard, but
    ** e.g., Keycloak's refresh_expires_in), compute that instant; else null.
    *******************************************************************************/
   private static Instant getRefreshTokenExpiresAt(AccessTokenResponse tokenResponse)
   {
      Object refreshExpiresIn = tokenResponse.getCustomParameters().get("refresh_expires_in");
      try
      {
         if(refreshExpiresIn != null)
         {
            long seconds = Long.parseLong(String.valueOf(refreshExpiresIn));
            if(seconds > 0)
            {
               return (Instant.now().plusSeconds(seconds));
            }
         }
      }
      catch(NumberFormatException e)
      {
         LOG.debug("Unparseable refresh_expires_in in token response - ignoring it");
      }
      return (null);
   }



   /*******************************************************************************
    ** Try to get a fresh access token for a session, using its stored refresh
    ** token.  Returns the new (or already-refreshed) access token, or null if the
    ** session cannot be refreshed (no refresh token, refresh disabled, lifetime
    ** exceeded, or a transient provider failure).  If the provider rejects the
    ** refresh token (invalid_grant), the session is ended and this throws.
    **
    ** Must be called with a system session in context.  Concurrent calls for the
    ** same session (in this JVM) are serialized, and only the first one calls the
    ** provider - later ones find the already-rotated token.
    *******************************************************************************/
   private String refreshAccessToken(String sessionUUID) throws QAuthenticationException
   {
      QInstance                    qInstance      = QContext.getQInstance();
      OAuth2AuthenticationMetaData oauth2MetaData = getOAuth2MetaData(qInstance);
      String                       tableName      = getRefreshTokenTableName(qInstance);
      if(oauth2MetaData == null || tableName == null || Boolean.FALSE.equals(oauth2MetaData.getRefreshTokensEnabled()))
      {
         return (null);
      }

      ReentrantLock lock = REFRESH_LOCKS[Math.floorMod(sessionUUID.hashCode(), REFRESH_LOCK_STRIPES)];
      lock.lock();
      try
      {
         //////////////////////////////////////////////////////////////////////
         // re-read the session, as another thread may have refreshed it     //
         // while we waited for the lock - in which case, use its new token. //
         //////////////////////////////////////////////////////////////////////
         QRecord userSessionRecord = getRecordByUniqueKey(UserSession.TABLE_NAME, "uuid", sessionUUID);
         if(userSessionRecord == null)
         {
            return (null);
         }

         String currentAccessToken = userSessionRecord.getValueString("accessToken");
         if(currentAccessToken != null && !isTokenExpiredOrNearExpiry(currentAccessToken, getRefreshSkewSeconds()))
         {
            return (currentAccessToken);
         }

         QRecord refreshTokenRecord = getRecordByUniqueKey(tableName, "sessionUuid", sessionUUID);
         if(refreshTokenRecord == null || !StringUtils.hasContent(refreshTokenRecord.getValueString("refreshToken")))
         {
            return (null);
         }

         UserSessionRefreshToken storedRefreshToken = new UserSessionRefreshToken(refreshTokenRecord);

         ////////////////////////////////////////////////////////////////////////////
         // don't extend the session past its max lifetime, nor try a refresh      //
         // token that we know is expired - drop it (revoking, if it may still be  //
         // live at the provider), and let the session end with its access token. //
         ////////////////////////////////////////////////////////////////////////////
         Instant now               = Instant.now();
         Integer maxLifetime       = oauth2MetaData.getMaxSessionLifetimeSeconds();
         boolean lifetimeExceeded  = maxLifetime != null && storedRefreshToken.getSessionStartedAt() != null && storedRefreshToken.getSessionStartedAt().plusSeconds(maxLifetime).isBefore(now);
         boolean refreshExpired    = storedRefreshToken.getRefreshTokenExpiresAt() != null && storedRefreshToken.getRefreshTokenExpiresAt().isBefore(now);
         if(lifetimeExceeded || refreshExpired)
         {
            LOG.info("Not refreshing OAuth2 session", logPair("sessionUUID", sessionUUID), logPair("reason", lifetimeExceeded ? "maxSessionLifetimeExceeded" : "refreshTokenExpired"));
            removeStoredRefreshToken(qInstance, sessionUUID, lifetimeExceeded);
            return (null);
         }

         return (doRefreshAccessToken(oauth2MetaData, tableName, sessionUUID, userSessionRecord, storedRefreshToken));
      }
      catch(QAuthenticationException qae)
      {
         throw (qae);
      }
      catch(Exception e)
      {
         LOG.warn("Error refreshing OAuth2 session", logPair("sessionUUID", sessionUUID), logPair("exceptionType", e.getClass().getSimpleName()));
         return (null);
      }
      finally
      {
         lock.unlock();
      }
   }



   /*******************************************************************************
    ** Call the provider's token endpoint with grant_type=refresh_token, and
    ** persist the resulting (possibly rotated) tokens.
    *******************************************************************************/
   private String doRefreshAccessToken(OAuth2AuthenticationMetaData oauth2MetaData, String tableName, String sessionUUID, QRecord userSessionRecord, UserSessionRefreshToken storedRefreshToken) throws Exception
   {
      ClientAuthentication clientAuth    = new ClientSecretBasic(new ClientID(oauth2MetaData.getClientId()), new Secret(oauth2MetaData.getClientSecret()));
      URI                  tokenEndpoint = getOIDCProviderMetadata(oauth2MetaData).getTokenEndpointURI();
      TokenRequest         tokenRequest  = new TokenRequest(tokenEndpoint, clientAuth, new RefreshTokenGrant(new RefreshToken(storedRefreshToken.getRefreshToken())));

      HTTPRequest httpRequest = tokenRequest.toHTTPRequest();
      httpRequest.setConnectTimeout(TOKEN_ENDPOINT_TIMEOUT_MILLIS);
      httpRequest.setReadTimeout(TOKEN_ENDPOINT_TIMEOUT_MILLIS);

      LOG.debug("Sending refresh token request", logPair("tokenEndpoint", tokenEndpoint), logPair("sessionUUID", sessionUUID));
      TokenResponse tokenResponse = TokenResponse.parse(httpRequest.send());

      if(tokenResponse.indicatesSuccess())
      {
         AccessTokenResponse successResponse = tokenResponse.toSuccessResponse();
         String              newAccessToken  = successResponse.getTokens().getAccessToken().getValue();
         RefreshToken        rotatedToken    = successResponse.getTokens().getRefreshToken();

         //////////////////////////////////////////////////////////////////////////
         // persist the refresh token first: if the provider rotated it, the old //
         // one is now dead, so losing the new one would end the session.        //
         //////////////////////////////////////////////////////////////////////////
         QRecord refreshTokenUpdate = new QRecord().withValue("id", storedRefreshToken.getId());
         if(rotatedToken != null)
         {
            refreshTokenUpdate.setValue("refreshToken", rotatedToken.getValue());
            refreshTokenUpdate.setValue("refreshTokenExpiresAt", getRefreshTokenExpiresAt(successResponse));
         }
         refreshTokenUpdate.setValue("modifyDate", Instant.now());
         new UpdateAction().execute(new UpdateInput(tableName).withRecord(refreshTokenUpdate));

         new UpdateAction().execute(new UpdateInput(UserSession.TABLE_NAME).withRecord(new QRecord()
            .withValue("id", userSessionRecord.getValue("id"))
            .withValue("accessToken", newAccessToken)));

         clearAccessTokenCache(sessionUUID);
         LOG.info("Refreshed OAuth2 session access token", logPair("sessionUUID", sessionUUID), logPair("rotatedRefreshToken", rotatedToken != null));
         return (newAccessToken);
      }

      ErrorObject errorObject = tokenResponse.toErrorResponse().getErrorObject();
      if(OAuth2Error.INVALID_GRANT.getCode().equals(errorObject.getCode()))
      {
         ////////////////////////////////////////////////////////////////////////
         // if a different server (sharing this session) rotated the token     //
         // since we read it, our copy is stale - use what that server stored. //
         ////////////////////////////////////////////////////////////////////////
         QRecord currentRefreshTokenRecord = getRecordByUniqueKey(tableName, "sessionUuid", sessionUUID);
         if(currentRefreshTokenRecord != null && !Objects.equals(currentRefreshTokenRecord.getValueString("refreshToken"), storedRefreshToken.getRefreshToken()))
         {
            QRecord currentUserSessionRecord = getRecordByUniqueKey(UserSession.TABLE_NAME, "uuid", sessionUUID);
            String  currentAccessToken       = currentUserSessionRecord == null ? null : currentUserSessionRecord.getValueString("accessToken");
            if(currentAccessToken != null && !isTokenExpiredOrNearExpiry(currentAccessToken, 0))
            {
               return (currentAccessToken);
            }
         }

         LOG.info("Refresh token was rejected by the provider - ending session", logPair("sessionUUID", sessionUUID), logPair("code", errorObject.getCode()));
         endSession(tableName, sessionUUID);
         throw (new QAuthenticationException("Session has ended (refresh token was rejected)"));
      }

      ////////////////////////////////////////////////////////////////////////////
      // other errors (e.g., provider down, invalid_client) may be transient -  //
      // keep the stored token, so a later request can try again.               //
      ////////////////////////////////////////////////////////////////////////////
      LOG.warn("Refresh token request failed",
         logPair("code", errorObject.getCode()),
         logPair("httpStatus", errorObject.getHTTPStatusCode()),
         logPair("tokenEndpoint", tokenEndpoint),
         logPair("sessionUUID", sessionUUID));
      return (null);
   }



   /*******************************************************************************
    ** Fully end a session whose refresh token was rejected: delete its refresh
    ** token and userSession rows, and forget any cached copies.
    *******************************************************************************/
   private void endSession(String tableName, String sessionUUID) throws QException
   {
      new DeleteAction().execute(new DeleteInput(tableName)
         .withQueryFilter(new QQueryFilter(new QFilterCriteria("sessionUuid", QCriteriaOperator.EQUALS, sessionUUID))));
      new DeleteAction().execute(new DeleteInput(UserSession.TABLE_NAME)
         .withQueryFilter(new QQueryFilter(new QFilterCriteria("uuid", QCriteriaOperator.EQUALS, sessionUUID))));
      clearAccessTokenCache(sessionUUID);

      OAuth2AuthenticationMetaData oauth2MetaData = getOAuth2MetaData(QContext.getQInstance());
      if(oauth2MetaData != null && Boolean.TRUE.equals(oauth2MetaData.getSessionStoreEnabled()))
      {
         QSessionStoreRegistry.getInstance().getProvider().ifPresent(provider -> provider.remove(sessionUUID));
      }
   }



   /*******************************************************************************
    ** Look up a single record by a unique-key field, as needed for the system's
    ** own session bookkeeping (i.e., including hidden fields, unmasked).
    *******************************************************************************/
   private static QRecord getRecordByUniqueKey(String tableName, String fieldName, String value) throws QException
   {
      return (new GetAction().executeForRecord(new GetInput(tableName)
         .withUniqueKey(Map.of(fieldName, value))
         .withShouldMaskPasswords(false)
         .withShouldOmitHiddenFields(false)));
   }



   /*******************************************************************************
    ** Remove the stored refresh token for a session (if any), optionally first
    ** revoking it at the provider (when it advertises a revocation endpoint).
    ** Revocation is best-effort; deletion is not.  Must be called with a system
    ** session in context.
    *******************************************************************************/
   private void removeStoredRefreshToken(QInstance qInstance, String sessionUUID, boolean revoke) throws QException
   {
      String tableName = getRefreshTokenTableName(qInstance);
      if(tableName == null)
      {
         return;
      }

      if(revoke)
      {
         QRecord refreshTokenRecord = getRecordByUniqueKey(tableName, "sessionUuid", sessionUUID);
         if(refreshTokenRecord != null && StringUtils.hasContent(refreshTokenRecord.getValueString("refreshToken")))
         {
            revokeRefreshToken(getOAuth2MetaData(qInstance), sessionUUID, refreshTokenRecord.getValueString("refreshToken"));
         }
      }

      deleteStoredRefreshTokens(qInstance, List.of(sessionUUID));
   }



   /*******************************************************************************
    ** Best-effort revocation of a refresh token (RFC 7009), if the provider's
    ** discovery document includes a revocation_endpoint.
    *******************************************************************************/
   private void revokeRefreshToken(OAuth2AuthenticationMetaData oauth2MetaData, String sessionUUID, String refreshToken)
   {
      try
      {
         URI revocationEndpoint = getOIDCProviderMetadata(oauth2MetaData).getRevocationEndpointURI();
         if(revocationEndpoint == null)
         {
            return;
         }

         ClientAuthentication clientAuth  = new ClientSecretBasic(new ClientID(oauth2MetaData.getClientId()), new Secret(oauth2MetaData.getClientSecret()));
         HTTPRequest          httpRequest = new TokenRevocationRequest(revocationEndpoint, clientAuth, new RefreshToken(refreshToken)).toHTTPRequest();
         httpRequest.setConnectTimeout(TOKEN_ENDPOINT_TIMEOUT_MILLIS);
         httpRequest.setReadTimeout(TOKEN_ENDPOINT_TIMEOUT_MILLIS);

         HTTPResponse httpResponse = httpRequest.send();
         if(!httpResponse.indicatesSuccess())
         {
            LOG.info("Refresh token revocation was not successful", logPair("sessionUUID", sessionUUID), logPair("httpStatus", httpResponse.getStatusCode()));
         }
      }
      catch(Exception e)
      {
         LOG.info("Error revoking refresh token", logPair("sessionUUID", sessionUUID), logPair("exceptionType", e.getClass().getSimpleName()));
      }
   }



   /*******************************************************************************
    ** Delete stored refresh tokens for the given sessions (no-op if the instance
    ** has no refresh-token table).  Used at logout and by OIDC back-channel
    ** logout - in the latter case, the provider has already ended the session,
    ** so there is no revocation call.  Runs with a system session.
    *******************************************************************************/
   public static void deleteStoredRefreshTokens(QInstance qInstance, Collection<String> sessionUuids) throws QException
   {
      String tableName = getRefreshTokenTableName(qInstance);
      if(tableName == null || CollectionUtils.nullSafeIsEmpty(sessionUuids))
      {
         return;
      }

      QContext.withTemporaryContext(new CapturedContext(qInstance, new QSystemUserSession()), () ->
      {
         var deleteOutput = new DeleteAction().execute(new DeleteInput(tableName)
            .withQueryFilter(new QQueryFilter(new QFilterCriteria("sessionUuid", QCriteriaOperator.IN, sessionUuids))));
         if(CollectionUtils.nullSafeHasContents(deleteOutput.getRecordsWithErrors()))
         {
            throw (new QException("Could not delete all stored refresh tokens"));
         }
      });
   }



   /*******************************************************************************
    ** Forget a cached token after its authoritative session has been removed.
    *******************************************************************************/
   public static void clearAccessTokenCache(String sessionUUID)
   {
      getAccessTokenFromSessionUUIDMemoization.clearKey(sessionUUID);
   }



   /***************************************************************************
    ** Clear cached OIDC provider metadata. Primarily for testing purposes
    ** when WireMock servers restart on different ports between tests.
    ***************************************************************************/
   public static void clearOIDCProviderMetadataCache()
   {
      oidcProviderMetadataMemoization.clear();
   }

}
