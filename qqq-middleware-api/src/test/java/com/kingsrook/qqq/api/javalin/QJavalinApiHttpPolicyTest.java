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

package com.kingsrook.qqq.api.javalin;


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.api.BaseTest;
import com.kingsrook.qqq.api.TestUtils;
import com.kingsrook.qqq.api.actions.ApiImplementation;
import com.kingsrook.qqq.api.model.metadata.ApiHttpPolicy;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaData;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessInput;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessInputFieldsContainer;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessMetaData;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessObjectOutput;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessUtils;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleInterface;
import com.kingsrook.qqq.middleware.javalin.QJavalinImplementation;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.SecurityScheme;
import com.kingsrook.qqq.openapi.model.SecuritySchemeType;
import io.javalin.apibuilder.EndpointGroup;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** An API instance served to the public internet: ApiHttpPolicy (cache headers,
 ** ETag/304, CORS), a CUSTOM authentication module scoped to the API (anonymous
 ** unless a bearer token is present), publicPath and semantic versions.
 *******************************************************************************/
class QJavalinApiHttpPolicyTest extends BaseTest
{
   private static final int    PORT     = 6271;
   private static final String BASE_URL = "http://localhost:" + PORT;
   private static final String QUERY    = BASE_URL + TestUtils.ALTERNATIVE_API_PATH + TestUtils.V2023_Q1 + "/person/query";
   private static final String ORIGIN   = "https://www.example.com";

   private static QJavalinImplementation qJavalinImplementation;



   /*******************************************************************************
    ** Anonymous without a token, a "user" session for "Bearer good", 401 otherwise.
    *******************************************************************************/
   public static class TestBearerModule implements QAuthenticationModuleInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QSession createSession(QInstance qInstance, Map<String, String> context) throws QAuthenticationException
      {
         String token = context.get("accessToken");
         if(token == null)
         {
            return (new QSession().withUser(new QUser().withIdReference("anonymous")).withValue("kind", "anonymous"));
         }
         if(!"good".equals(token))
         {
            throw (new QAuthenticationException("bad token"));
         }
         return (new QSession().withUser(new QUser().withIdReference("user")).withValue("kind", "user"));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public boolean isSessionValid(QInstance instance, QSession session)
      {
         return (session != null && session.getValue("kind") != null);
      }
   }



   /*******************************************************************************
    ** Echoes request metadata and a body value.
    *******************************************************************************/
   public static class EchoStep implements BackendStep
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput)
      {
         runBackendStepOutput.addValue("userAgent", runBackendStepInput.getValueString(ApiProcessUtils.REQUEST_USER_AGENT));
         runBackendStepOutput.addValue("remoteAddress", runBackendStepInput.getValueString(ApiProcessUtils.REQUEST_REMOTE_ADDRESS));
         runBackendStepOutput.addValue("message", runBackendStepInput.getValueString("message"));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeAll
   static void beforeAll() throws Exception
   {
      QInstance           instance = TestUtils.defineInstance();
      ApiInstanceMetaData api      = ApiInstanceMetaDataContainer.of(instance).getApiInstanceMetaData(TestUtils.ALTERNATIVE_API_NAME);
      api.withHttpPolicy(new ApiHttpPolicy().withCorsAllowedOrigins(List.of(ORIGIN)))
         .withAllowsAnonymousAccess(true)
         .withPublicPath("/")
         .withSemanticVersions(Map.of(TestUtils.V2023_Q1, "2.3.1"))
         .withSecurityScheme("bearerAuth", new SecurityScheme().withType(SecuritySchemeType.HTTP).withScheme("bearer").withBearerFormat("JWT"));
      instance.registerAuthenticationProvider(AuthScope.api(api), new QAuthenticationMetaData()
         .withName("test-bearer").withType(QAuthenticationType.CUSTOM)
         .withModuleCodeReference(new QCodeReference(TestBearerModule.class)));

      instance.addProcess(new QProcessMetaData()
         .withName("echo")
         .withStep(new QBackendStepMetaData().withName("run").withCode(new QCodeReference(EchoStep.class)))
         .withSupplementalMetaData(new ApiProcessMetaDataContainer().withApiProcessMetaData(TestUtils.ALTERNATIVE_API_NAME, new ApiProcessMetaData()
            .withInitialVersion(TestUtils.V2022_Q4)
            .withMethod(HttpMethod.POST)
            .withAsyncMode(ApiProcessMetaData.AsyncMode.NEVER)
            .withInput(new ApiProcessInput().withRecordBodyParams(new ApiProcessInputFieldsContainer().withField(new QFieldMetaData("message", QFieldType.STRING))))
            .withOutput(new ApiProcessObjectOutput().withOutputFields(List.of(new QFieldMetaData("userAgent", QFieldType.STRING),
               new QFieldMetaData("remoteAddress", QFieldType.STRING), new QFieldMetaData("message", QFieldType.STRING)))))));

      qJavalinImplementation = new QJavalinImplementation(instance);
      qJavalinImplementation.clearJavalinRoutes();
      EndpointGroup routes = new QJavalinApiHandler(instance).getRoutes();
      qJavalinImplementation.addJavalinRoutes(routes);
      qJavalinImplementation.startJavalinServer(PORT);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterAll
   static void afterAll()
   {
      qJavalinImplementation.stopJavalinServer();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   @AfterEach
   void reset()
   {
      ////////////////////////////////////////////////////////////////////////
      // no cookie jar: cookies other tests' servers set on localhost would //
      // make these requests credentialed (cookies ignore the port), nor    //
      // another test's default Cookie header.                             //
      ////////////////////////////////////////////////////////////////////////
      Unirest.config().reset().clearDefaultHeaders().enableCookieManagement(false);
      ApiImplementation.clearCaches();
   }



   /*******************************************************************************
    ** Anonymous GET: shared-cache headers, weak ETag, JSON, CORS for any origin.
    *******************************************************************************/
   @Test
   void publicReadsAreCacheableAndOpenToAnyOrigin()
   {
      HttpResponse<String> response = Unirest.get(QUERY).header("Origin", "https://anywhere.example").asString();
      assertThat(response.getStatus()).isEqualTo(200);
      assertThat(response.getHeaders().getFirst("Cache-Control")).isEqualTo("public, max-age=60, s-maxage=60, stale-while-revalidate=60, stale-if-error=86400");
      assertThat(response.getHeaders().getFirst("CDN-Cache-Control")).isEqualTo("max-age=60, stale-while-revalidate=60, stale-if-error=86400");
      assertThat(response.getHeaders().getFirst("ETag")).startsWith("W/\"");
      assertThat(response.getHeaders().getFirst("Content-Type")).startsWith("application/json");
      assertThat(response.getHeaders().getFirst("Access-Control-Allow-Origin")).isEqualTo("*");
      assertThat(response.getHeaders().getFirst("Access-Control-Allow-Credentials")).isEmpty();
      assertThat(response.getHeaders().get("Vary")).doesNotContain("Origin");

      HttpResponse<String> again = Unirest.get(QUERY).header("If-None-Match", response.getHeaders().getFirst("ETag")).asString();
      assertThat(again.getStatus()).isEqualTo(304);
      assertThat(again.getBody()).isNullOrEmpty();
   }



   /*******************************************************************************
    ** Credentialed requests are never shared-cacheable; CORS only for the
    ** allow-listed origin.
    *******************************************************************************/
   @Test
   void credentialedRequestsArePrivate()
   {
      HttpResponse<String> response = Unirest.get(QUERY).header("Authorization", "Bearer good").header("Origin", ORIGIN).asString();
      assertThat(response.getStatus()).isEqualTo(200);
      assertThat(response.getHeaders().getFirst("Cache-Control")).isEqualTo("private, no-store");
      assertThat(response.getHeaders().getFirst("CDN-Cache-Control")).isEmpty();
      assertThat(response.getHeaders().getFirst("ETag")).isEmpty();
      assertThat(response.getHeaders().getFirst("Access-Control-Allow-Origin")).isEqualTo(ORIGIN);
      assertThat(response.getHeaders().getFirst("Vary")).contains("Origin");

      HttpResponse<String> other = Unirest.get(QUERY).header("Authorization", "Bearer good").header("Origin", "https://evil.example").asString();
      assertThat(other.getHeaders().getFirst("Access-Control-Allow-Origin")).isEmpty();
      assertThat(other.getHeaders().getFirst("Cache-Control")).isEqualTo("private, no-store");
   }



   /*******************************************************************************
    ** The scoped CUSTOM module decides: bad tokens are rejected.
    *******************************************************************************/
   @Test
   void customModuleAuthenticatesTheApi()
   {
      assertThat(Unirest.get(QUERY).header("Authorization", "Bearer bad").asString().getStatus()).isEqualTo(401);
      assertThat(Unirest.get(QUERY).header("Authorization", "Bearer good").asString().getStatus()).isEqualTo(200);
   }



   /*******************************************************************************
    ** Preflights answer 204 with the allow-list headers.
    *******************************************************************************/
   @Test
   void preflight()
   {
      HttpResponse<String> response = Unirest.request("OPTIONS", QUERY).header("Origin", ORIGIN).header("Access-Control-Request-Method", "PATCH").asString();
      assertThat(response.getStatus()).isEqualTo(204);
      assertThat(response.getHeaders().getFirst("Access-Control-Allow-Origin")).isEqualTo(ORIGIN);
      assertThat(response.getHeaders().getFirst("Access-Control-Allow-Methods")).contains("PATCH");
      assertThat(response.getHeaders().getFirst("Access-Control-Allow-Headers")).contains("Authorization");
   }



   /*******************************************************************************
    ** Errors and writes are private.
    *******************************************************************************/
   @Test
   void errorsAreNotCached()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + TestUtils.ALTERNATIVE_API_PATH + TestUtils.V2023_Q1 + "/person/987654").asString();
      assertThat(response.getStatus()).isEqualTo(404);
      assertThat(response.getHeaders().getFirst("Cache-Control")).isEqualTo("private, no-store");
      assertThat(response.getHeaders().getFirst("Content-Type")).startsWith("application/json");
   }



   /*******************************************************************************
    ** Spec: published paths, semantic version, optional security.
    *******************************************************************************/
   @Test
   void specUsesPublicPathSemverAndOptionalSecurity()
   {
      JSONObject spec = new JSONObject(Unirest.get(BASE_URL + TestUtils.ALTERNATIVE_API_PATH + TestUtils.V2023_Q1 + "/openapi.json").asString().getBody());
      assertThat(spec.getJSONObject("info").getString("version")).isEqualTo("2.3.1");
      assertThat(spec.getJSONObject("paths").keySet()).contains("/" + TestUtils.V2023_Q1 + "/person/query");
      assertThat(spec.getJSONObject("paths").keySet()).noneMatch(path -> path.startsWith(TestUtils.ALTERNATIVE_API_PATH));
      assertThat(spec.getJSONObject("paths").getJSONObject("/" + TestUtils.V2023_Q1 + "/person/query").getJSONObject("get").getJSONArray("security").getJSONObject(0).isEmpty()).isTrue();

      JSONObject versions = new JSONObject(Unirest.get(BASE_URL + TestUtils.ALTERNATIVE_API_PATH + "versions.json").asString().getBody());
      assertThat(versions.getJSONObject("semanticVersions").getString(TestUtils.V2023_Q1)).isEqualTo("2.3.1");
   }



   /*******************************************************************************
    ** Unit pieces.
    *******************************************************************************/
   @Test
   void etagHelpers()
   {
      String etag = ApiHttpPolicyHandler.weakEtag("abc".getBytes());
      assertThat(etag).isEqualTo(ApiHttpPolicyHandler.weakEtag("abc".getBytes())).startsWith("W/\"");
      assertThat(ApiHttpPolicyHandler.ifNoneMatchMatches(etag, etag)).isTrue();
      assertThat(ApiHttpPolicyHandler.ifNoneMatchMatches("\"x\", " + etag.substring(2), etag)).isTrue();
      assertThat(ApiHttpPolicyHandler.ifNoneMatchMatches("*", etag)).isTrue();
      assertThat(ApiHttpPolicyHandler.ifNoneMatchMatches("\"other\"", etag)).isFalse();
      assertThat(ApiHttpPolicyHandler.ifNoneMatchMatches(null, etag)).isFalse();
      assertThat(ApiHttpPolicyHandler.plainPublicMaxAge("public, max-age=300")).isEqualTo(300);
      assertThat(ApiHttpPolicyHandler.plainPublicMaxAge("public, max-age=300, immutable")).isNull();
      assertThat(new ApiHttpPolicy().withStaleIfErrorSeconds(null).withStaleWhileRevalidateSeconds(null).publicCacheControl(5)).isEqualTo("public, max-age=5, s-maxage=5");
   }


   /*******************************************************************************
    ** Processes see the request's metadata (never a caller-supplied value), an
    ** empty JSON body is no values, and a malformed one is a 400.
    *******************************************************************************/
   @Test
   void processesSeeRequestMetadata()
   {
      String url = BASE_URL + TestUtils.ALTERNATIVE_API_PATH + TestUtils.V2023_Q1 + "/echo";
      JSONObject output = new JSONObject(Unirest.post(url).header("User-Agent", "probe/1.0").header("Content-Type", "application/json")
         .body("{\"message\":\"hi\",\"apiRequest.userAgent\":\"spoofed\"}").asString().getBody());
      assertThat(output.getString("userAgent")).isEqualTo("probe/1.0");
      assertThat(output.getString("remoteAddress")).isNotBlank();
      assertThat(output.getString("message")).isEqualTo("hi");

      assertThat(Unirest.post(url).asString().getStatus()).isEqualTo(200);
      assertThat(Unirest.post(url).body("not json").asString().getStatus()).isEqualTo(400);
   }


   /*******************************************************************************
    ** Behind a path-mapping proxy the docs page links its spec and assets by the
    ** public path, and the assets are served under the API's own path.
    *******************************************************************************/
   @Test
   void docsPageUsesPublicPath()
   {
      String html = Unirest.get(BASE_URL + TestUtils.ALTERNATIVE_API_PATH).asString().getBody();
      assertThat(html).contains("spec-url=\"/" + TestUtils.V2023_Q1 + "/openapi.json\"");
      assertThat(html).contains("src=\"/" + QJavalinApiHandler.DOCS_ASSETS_PATH + "js/rapidoc.min.js\"");
      assertThat(html).doesNotContain(TestUtils.ALTERNATIVE_API_PATH + TestUtils.V2023_Q1);

      HttpResponse<String> script = Unirest.get(BASE_URL + TestUtils.ALTERNATIVE_API_PATH + QJavalinApiHandler.DOCS_ASSETS_PATH + "js/rapidoc.min.js").asString();
      assertThat(script.getStatus()).isEqualTo(200);
      assertThat(Unirest.get(BASE_URL + TestUtils.ALTERNATIVE_API_PATH + QJavalinApiHandler.DOCS_ASSETS_PATH + "css/qqq-api-styles.css").asString().getStatus()).isEqualTo(200);
   }
}
