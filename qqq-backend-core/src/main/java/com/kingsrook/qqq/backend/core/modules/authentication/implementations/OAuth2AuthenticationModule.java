/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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


import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
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
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.model.UserSession;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.memoization.Memoization;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.GeneralException;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import org.json.JSONObject;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Implementation of OAuth2 authentication.
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
         throw (new QAuthenticationException("Failed to create session (token)", e));
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private QSession createSessionFromTokenRequest(TokenRequest tokenRequest) throws ParseException, IOException, QException
   {
      TokenResponse tokenResponse = TokenResponse.parse(tokenRequest.toHTTPRequest().send());

      if(tokenResponse.indicatesSuccess())
      {
         AccessToken accessToken = tokenResponse.toSuccessResponse().getTokens().getAccessToken();

         ////////////////////////////////////////////////////////////////////
         // todo - do we want to try to do anything with a refresh token?? //
         ////////////////////////////////////////////////////////////////////
         // RefreshToken refreshToken = tokenResponse.toSuccessResponse().getTokens().getRefreshToken();

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
         LOG.info("Token request failed", logPair("code", errorObject.getCode()), logPair("description", errorObject.getDescription()));
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
      return QAuthenticationModuleInterface.super.createAutomatedSessionForUser(qInstance, userId);
   }



   /*******************************************************************************
    ** Look up access_token from session UUID
    **
    *******************************************************************************/
   private String getAccessTokenFromSessionUUID(String sessionUUID) throws QAuthenticationException
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

         /////////////////////////////////////////////
         // delete the session record from database //
         /////////////////////////////////////////////
         new DeleteAction().execute(new DeleteInput(UserSession.TABLE_NAME)
            .withQueryFilter(new QQueryFilter(new QFilterCriteria("uuid", QCriteriaOperator.EQUALS, sessionUUID))));

         ///////////////////////////////////////////
         // clear the session from memoization cache //
         ///////////////////////////////////////////
         getAccessTokenFromSessionUUIDMemoization.clearKey(sessionUUID);

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



   /***************************************************************************
    ** Clear cached OIDC provider metadata. Primarily for testing purposes
    ** when WireMock servers restart on different ports between tests.
    ***************************************************************************/
   public static void clearOIDCProviderMetadataCache()
   {
      oidcProviderMetadataMemoization.clear();
   }

}
