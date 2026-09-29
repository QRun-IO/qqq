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

package com.kingsrook.qqq.middleware.javalin.executors;


import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QSessionStoreRegistry;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.OAuth2AuthenticationModule;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.memoization.Memoization;
import com.kingsrook.qqq.middleware.javalin.executors.io.BackChannelLogoutInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.BackChannelLogoutOutputInterface;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.claims.LogoutTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.validators.LogoutTokenValidator;


/*******************************************************************************
 ** Validate a provider's signed logout event before modifying stored OAuth2 sessions.
 *******************************************************************************/
public class BackChannelLogoutExecutor extends AbstractMiddlewareExecutor<BackChannelLogoutInput, BackChannelLogoutOutputInterface>
{
   private static final Duration MAX_TOKEN_AGE = Duration.ofMinutes(5);
   private static final Memoization<List<String>, LogoutTokenValidator> VALIDATORS = new Memoization<List<String>, LogoutTokenValidator>()
      .withTimeout(Duration.ofMinutes(10)).withMaxSize(100);
   private static final Map<List<Object>, Instant> PROCESSED_TOKENS = new HashMap<>();



   /*******************************************************************************
    ** Invalid requests must fail before acquiring a SYSTEM session or deleting data.
    *******************************************************************************/
   @Override
   public void execute(BackChannelLogoutInput input, BackChannelLogoutOutputInterface output) throws QException
   {
      QInstance instance = QContext.getQInstance();
      if(!(instance.getAuthentication() instanceof OAuth2AuthenticationMetaData authentication)
         || !StringUtils.hasContent(authentication.getUserSessionTableName())
         || instance.getTable(authentication.getUserSessionTableName()) == null)
      {
         throw (new QBadRequestException("Back-channel logout requires configured OAuth2 session storage"));
      }

      LogoutTokenClaimsSet claims = validateToken(input.getLogoutToken(), authentication);
      List<Object> replayKey = List.of(instance, claims.getIssuer().getValue(), authentication.getClientId(), authentication.getUserSessionTableName(), claims.getJWTID().getValue());
      synchronized(PROCESSED_TOKENS)
      {
         Instant now = Instant.now();
         PROCESSED_TOKENS.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
         if(PROCESSED_TOKENS.containsKey(replayKey))
         {
            throw (new QBadRequestException("Logout token has already been processed"));
         }
         if(PROCESSED_TOKENS.size() >= 10000)
         {
            throw (new QBadRequestException("Logout replay protection is at capacity"));
         }
         deleteMatchingSessions(authentication.getUserSessionTableName(), claims, authentication.getSessionStoreEnabled());
         PROCESSED_TOKENS.put(replayKey, now.plus(Duration.ofMinutes(6)));
      }
   }



   /*******************************************************************************
    ** Trust provider/client configuration, never URLs or issuer claims from the token.
    *******************************************************************************/
   private LogoutTokenClaimsSet validateToken(String token, OAuth2AuthenticationMetaData authentication) throws QBadRequestException
   {
      try
      {
         if(!StringUtils.hasContent(token) || !StringUtils.hasContent(authentication.getBaseUrl()) || !StringUtils.hasContent(authentication.getClientId()))
         {
            throw (new IllegalArgumentException("Missing token or provider configuration"));
         }
         String issuer = StringUtils.hasContent(authentication.getExternalBaseUrl()) ? authentication.getExternalBaseUrl() : authentication.getBaseUrl();
         List<String> providerKey = List.of(authentication.getBaseUrl(), issuer, authentication.getClientId());
         LogoutTokenValidator validator = VALIDATORS.getResultThrowing(providerKey, key ->
         {
            Issuer expectedIssuer = new Issuer(issuer);
            OIDCProviderMetadata provider = OIDCProviderMetadata.resolve(expectedIssuer,
               java.net.URI.create(authentication.getBaseUrl()).toURL(), 5000, 5000);
            return (new LogoutTokenValidator(expectedIssuer, new ClientID(authentication.getClientId()), JWSAlgorithm.RS256,
               provider.getJWKSetURI().toURL(), new DefaultResourceRetriever(5000, 5000, 1000000)));
         }).orElseThrow();

         LogoutTokenClaimsSet claims = validator.validate(SignedJWT.parse(token));
         Instant now = Instant.now();
         if(claims.getIssueTime().toInstant().isBefore(now.minus(MAX_TOKEN_AGE))
            || claims.getIssueTime().toInstant().isAfter(now.plusSeconds(60))
            || !StringUtils.hasContent(claims.getJWTID().getValue()))
         {
            throw (new IllegalArgumentException("Invalid logout token age or identifier"));
         }
         return (claims);
      }
      catch(Exception e)
      {
         throw (new QBadRequestException("Invalid logout token"));
      }
   }



   /*******************************************************************************
    ** Match every supplied subject/session claim and retain sessions from other issuers.
    *******************************************************************************/
   private void deleteMatchingSessions(String tableName, LogoutTokenClaimsSet claims, Boolean useSessionStore) throws QException
   {
      var previousSession = QContext.getQSession();
      try
      {
         QContext.setQSession(new QSystemUserSession());
         String subject = claims.getSubject() == null ? null : claims.getSubject().getValue();
         String sessionId = claims.getSessionID() == null ? null : claims.getSessionID().getValue();
         QueryInput query = new QueryInput(tableName);
         query.setShouldOmitHiddenFields(false);
         query.setShouldMaskPasswords(false);
         List<String> sessionUuids = new ArrayList<>();
         for(QRecord session : new QueryAction().execute(query).getRecords())
         {
            String accessToken = session.getValueString("accessToken");
            if(!StringUtils.hasContent(accessToken))
            {
               continue;
            }
            try
            {
               JWTClaimsSet storedClaims = JWTParser.parse(accessToken).getJWTClaimsSet();
               if(claims.getIssuer().getValue().equals(storedClaims.getIssuer())
                  && (subject == null || subject.equals(storedClaims.getSubject()))
                  && (sessionId == null || sessionId.equals(storedClaims.getStringClaim("sid"))))
               {
                  sessionUuids.add(session.getValueString("uuid"));
               }
            }
            catch(java.text.ParseException | IllegalArgumentException e)
            {
               /////////////////////////////
               // Unreadable sessions cannot establish a matching issuer/session. //
               /////////////////////////////
            }
         }
         if(!sessionUuids.isEmpty())
         {
            if(Boolean.TRUE.equals(useSessionStore))
            {
               QSessionStoreRegistry.getInstance().getProvider().ifPresent(provider -> sessionUuids.forEach(provider::remove));
            }
            try
            {
               var deleted = new DeleteAction().execute(new DeleteInput().withTableName(tableName)
                  .withQueryFilter(new QQueryFilter(new QFilterCriteria("uuid", QCriteriaOperator.IN, sessionUuids))));
               if(deleted.getRecordsWithErrors() != null && !deleted.getRecordsWithErrors().isEmpty())
               {
                  throw (new QException("Could not delete all matching logout sessions"));
               }
            }
            finally
            {
               sessionUuids.forEach(OAuth2AuthenticationModule::clearAccessTokenCache);
            }
         }
      }
      finally
      {
         QContext.setQSession(previousSession);
      }
   }

}
