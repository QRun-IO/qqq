/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.sampleapp;


import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicInteger;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.api.actions.BaseAPIActionUtil;
import com.kingsrook.qqq.backend.module.api.model.AuthorizationType;
import com.kingsrook.qqq.backend.module.api.model.metadata.APIBackendMetaData;
import com.kingsrook.qqq.backend.module.api.model.metadata.APITableBackendDetails;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Sample acceptance of real outbound API authentication on loopback HTTP.
 *******************************************************************************/
class SampleApiAuthenticationAcceptanceTest
{
   private static final String KEY = "fixture-key-59381";
   private static final String USER = "fixture-user";
   private static final String PASSWORD = "fixture-password-59381";
   private static final String TOKEN = "fixture-oauth-token-59381";



   /*******************************************************************************
    ** Every supported configured mode reaches the protected record endpoint.
    *******************************************************************************/
   @Test
   void testSevenConfiguredOutboundModes() throws Exception
   {
      for(AuthorizationType mode : EnumSet.of(AuthorizationType.API_KEY_HEADER, AuthorizationType.API_TOKEN,
         AuthorizationType.BASIC_AUTH_API_KEY, AuthorizationType.BASIC_AUTH_USERNAME_PASSWORD,
         AuthorizationType.OAUTH2, AuthorizationType.API_KEY_QUERY_PARAM, AuthorizationType.CUSTOM))
      {
         try(Fixture fixture = new Fixture(mode))
         {
            APIBackendMetaData backend = backend(fixture, mode, true);
            assertEquals("accepted", read(backend), mode.toString());
            assertEquals(1, fixture.recordRequests.get(), mode.toString());
            assertEquals(mode == AuthorizationType.OAUTH2 ? 1 : 0, fixture.tokenRequests.get(), mode.toString());
         }
      }
   }



   /*******************************************************************************
    ** Missing and rejected credentials never yield a protected record.
    *******************************************************************************/
   @Test
   void testMissingAndRejectedCredentials() throws Exception
   {
      for(AuthorizationType mode : EnumSet.of(AuthorizationType.API_KEY_HEADER, AuthorizationType.API_TOKEN,
         AuthorizationType.BASIC_AUTH_API_KEY, AuthorizationType.BASIC_AUTH_USERNAME_PASSWORD,
         AuthorizationType.OAUTH2, AuthorizationType.API_KEY_QUERY_PARAM, AuthorizationType.CUSTOM))
      {
         try(Fixture fixture = new Fixture(mode))
         {
            APIBackendMetaData backend = backend(fixture, mode, false);
            assertThrows(Exception.class, () -> read(backend), "missing " + mode);
            assertEquals(0, fixture.acceptedRequests.get(), "missing " + mode);
         }
         try(Fixture fixture = new Fixture(mode))
         {
            APIBackendMetaData backend = backend(fixture, mode, true);
            backend.withApiKey("rejected-key").withClientSecret("rejected-secret").withPassword("rejected-password");
            assertThrows(Exception.class, () -> read(backend), "rejected " + mode);
            assertEquals(0, fixture.acceptedRequests.get(), "rejected " + mode);
            assertEquals(mode == AuthorizationType.OAUTH2 ? 1 : 0, fixture.tokenRequests.get(), "rejected " + mode);
            assertEquals(mode == AuthorizationType.OAUTH2 ? 0 : 1, fixture.recordRequests.get(), "rejected " + mode);
         }
      }
   }



   /*******************************************************************************
    ** An expired bearer token refreshes once before retrying the protected GET.
    *******************************************************************************/
   @Test
   void testExpiredOAuthTokenRefreshes() throws Exception
   {
      try(Fixture fixture = new Fixture(AuthorizationType.OAUTH2))
      {
         fixture.rejectFirstBearer = true;
         APIBackendMetaData backend = backend(fixture, AuthorizationType.OAUTH2, true);
         assertEquals("accepted", read(backend));
         assertEquals(2, fixture.tokenRequests.get());
         assertEquals(2, fixture.recordRequests.get());
         assertEquals(1, fixture.acceptedRequests.get());
      }
   }



   /*******************************************************************************
    ** An invalid token document fails without sending a record request.
    *******************************************************************************/
   @Test
   void testMalformedOAuthTokenResponse() throws Exception
   {
      try(Fixture fixture = new Fixture(AuthorizationType.OAUTH2))
      {
         fixture.malformedToken = true;
         assertThrows(Exception.class, () -> read(backend(fixture, AuthorizationType.OAUTH2, true)));
         assertEquals(1, fixture.tokenRequests.get());
         assertEquals(0, fixture.recordRequests.get());
      }
   }



   /*******************************************************************************
    ** Provider echoes and query credentials cannot enter errors or diagnostics.
    *******************************************************************************/
   @Test
   void testSecretFreeDiagnostics() throws Exception
   {
      StringBuilder events = new StringBuilder();
      Logger logger = (Logger) LogManager.getLogger(BaseAPIActionUtil.class);
      Level oldLevel = logger.getLevel();
      AbstractAppender appender = new AbstractAppender("api-auth-acceptance", null,
         PatternLayout.createDefaultLayout(), false, Property.EMPTY_ARRAY)
      {
         @Override
         public void append(LogEvent event)
         {
            events.append(event.getMessage().getFormattedMessage());
            if(event.getThrown() != null)
            {
               events.append(event.getThrown());
            }
         }
      };
      appender.start();
      logger.addAppender(appender);
      logger.setLevel(Level.DEBUG);
      try
      {
         try(Fixture fixture = new Fixture(AuthorizationType.API_KEY_QUERY_PARAM))
         {
            fixture.echoCredentialOnReject = true;
            APIBackendMetaData backend = backend(fixture, AuthorizationType.API_KEY_QUERY_PARAM, true);
            backend.withApiKey("rejected-" + KEY);
            Exception error = assertThrows(Exception.class, () -> read(backend));
            assertTrue(error.getMessage() != null);
            assertTrue(!error.getMessage().contains(KEY));
            assertTrue(!events.toString().contains(KEY));
         }
         events.setLength(0);
         try(Fixture fixture = new Fixture(AuthorizationType.OAUTH2))
         {
            fixture.echoCredentialOnReject = true;
            APIBackendMetaData backend = backend(fixture, AuthorizationType.OAUTH2, true);
            backend.withClientSecret("rejected-" + PASSWORD);
            Exception error = assertThrows(Exception.class, () -> read(backend));
            assertTrue(error.getMessage() != null);
            assertTrue(!error.getMessage().contains(PASSWORD));
            assertTrue(!events.toString().contains(PASSWORD));
         }
         events.setLength(0);
         try(Fixture fixture = new Fixture(AuthorizationType.OAUTH2))
         {
            assertEquals("accepted", read(backend(fixture, AuthorizationType.OAUTH2, true)));
            assertTrue(!events.toString().contains(TOKEN));
         }
      }
      finally
      {
         logger.setLevel(oldLevel);
         logger.removeAppender(appender);
         appender.stop();
      }
   }



   /*******************************************************************************
    ** The public GetAction is the seam under test, using a disposable API table.
    *******************************************************************************/
   private String read(APIBackendMetaData backend) throws Exception
   {
      QInstance instance = new QInstance();
      instance.registerAuthenticationProvider(AuthScope.instanceDefault(), new QAuthenticationMetaData()
         .withName("mock").withType(QAuthenticationType.MOCK));
      instance.addBackend(backend);
      instance.addTable(new QTableMetaData().withName("fixtureRecord").withBackendName("fixtureApi")
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("name", QFieldType.STRING))
         .withBackendDetails(new APITableBackendDetails().withTablePath("records")));
      QContext.init(instance, new QSession());
      try
      {
         GetInput input = new GetInput();
         input.setTableName("fixtureRecord");
         input.setPrimaryKey(7);
         return new GetAction().execute(input).getRecord().getValueString("name");
      }
      finally
      {
         QContext.clear();
      }
   }



   /*******************************************************************************
    ** Fixture credentials are synthetic, and never loaded from user secrets.
    *******************************************************************************/
   private APIBackendMetaData backend(Fixture fixture, AuthorizationType mode, boolean credentials)
   {
      APIBackendMetaData backend = new APIBackendMetaData().withName("fixtureApi")
         .withBaseUrl(fixture.baseUrl()).withContentType("application/json")
         .withAuthorizationType(mode).withActionUtil(new QCodeReference(FixtureActionUtil.class));
      if(mode == AuthorizationType.API_KEY_QUERY_PARAM)
      {
         backend.withApiKeyQueryParamName("fixture_key");
      }
      if(credentials)
      {
         backend.withApiKey(KEY).withUsername(USER).withPassword(PASSWORD)
            .withClientId(USER).withClientSecret(PASSWORD);
      }
      return backend;
   }



   /*******************************************************************************
    ** A local HTTP peer checks the wire representation of every auth mode.
    *******************************************************************************/
   private static class Fixture implements AutoCloseable
   {
      private final AuthorizationType mode;
      private final HttpServer server;
      private final AtomicInteger recordRequests = new AtomicInteger();
      private final AtomicInteger acceptedRequests = new AtomicInteger();
      private final AtomicInteger tokenRequests = new AtomicInteger();
      private volatile boolean rejectFirstBearer;
      private volatile boolean malformedToken;
      private volatile boolean echoCredentialOnReject;

      /*******************************************************************************
       **
       *******************************************************************************/
      private Fixture(AuthorizationType mode) throws IOException
      {
         this.mode = mode;
         server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
         server.createContext("/oauth/token", this::token);
         server.createContext("/records/7", this::record);
         server.start();
      }

      /*******************************************************************************
       **
       *******************************************************************************/
      private String baseUrl()
      {
         return "http://127.0.0.1:" + server.getAddress().getPort() + "/";
      }

      /*******************************************************************************
       **
       *******************************************************************************/
      private void token(HttpExchange exchange) throws IOException
      {
         tokenRequests.incrementAndGet();
         String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
         boolean authorized = body.contains("grant_type=client_credentials")
            && body.contains("client_id=" + USER) && body.contains("client_secret=" + PASSWORD);
         respond(exchange, authorized ? 200 : 401,
            malformedToken ? "{invalid token" : authorized ? "{\"access_token\":\"" + TOKEN + "\"}"
               : echoCredentialOnReject ? "denied " + body : "denied");
      }

      /*******************************************************************************
       **
       *******************************************************************************/
      private void record(HttpExchange exchange) throws IOException
      {
         int requestNumber = recordRequests.incrementAndGet();
         String authorization = exchange.getRequestHeaders().getFirst("Authorization");
         boolean authorized = switch(mode)
         {
            case API_KEY_HEADER -> KEY.equals(exchange.getRequestHeaders().getFirst("API-Key"));
            case API_TOKEN -> ("Token " + KEY).equals(authorization);
            case BASIC_AUTH_API_KEY -> basic(KEY).equals(authorization);
            case BASIC_AUTH_USERNAME_PASSWORD -> basic(USER + ":" + PASSWORD).equals(authorization);
            case OAUTH2 -> ("Bearer " + TOKEN).equals(authorization) && (!rejectFirstBearer || requestNumber > 1);
            case API_KEY_QUERY_PARAM -> ("fixture_key=" + KEY).equals(exchange.getRequestURI().getRawQuery());
            case CUSTOM -> KEY.equals(exchange.getRequestHeaders().getFirst("X-Fixture-Auth"));
            default -> false;
         };
         if(authorized)
         {
            acceptedRequests.incrementAndGet();
         }
         respond(exchange, authorized ? 200 : 401,
            authorized ? "{\"id\":7,\"name\":\"accepted\"}"
               : echoCredentialOnReject ? "denied " + exchange.getRequestURI() : "denied");
      }

      /*******************************************************************************
       **
       *******************************************************************************/
      private static String basic(String value)
      {
         return "Basic " + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
      }

      /*******************************************************************************
       **
       *******************************************************************************/
      private void respond(HttpExchange exchange, int status, String body) throws IOException
      {
         byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
         exchange.getResponseHeaders().add("Content-Type", "application/json");
         exchange.sendResponseHeaders(status, bytes.length);
         try(exchange)
         {
            exchange.getResponseBody().write(bytes);
         }
      }

      @Override
      public void close()
      {
         server.stop(0);
      }
   }



   /*******************************************************************************
    ** The sample's custom mode supplies its own header on the outgoing request.
    *******************************************************************************/
   public static class FixtureActionUtil extends BaseAPIActionUtil
   {
      @Override
      public String buildUrlSuffixForSingleRecordGet(Serializable primaryKey)
      {
         return "/" + primaryKey;
      }

      @Override
      protected void handleCustomAuthorization(HttpRequestBase request)
      {
         request.setHeader("X-Fixture-Auth", backendMetaData.getApiKey());
      }
   }
}
