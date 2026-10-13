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

package com.kingsrook.qqq.middleware.javalin.specs.extensions;


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.AbstractQQQApplication;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.middleware.javalin.QApplicationJavalinServer;
import com.kingsrook.qqq.middleware.javalin.QJavalinImplementation;
import com.kingsrook.qqq.middleware.javalin.QJavalinMetaData;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.UiApiRouteAuditor;
import com.kingsrook.qqq.middleware.javalin.specs.invoke.UiApiInvoker;
import com.kingsrook.qqq.middleware.javalin.specs.invoke.UiApiOperation;
import com.kingsrook.qqq.middleware.javalin.specs.invoke.UiApiRequest;
import com.kingsrook.qqq.middleware.javalin.specs.invoke.UiApiResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.MiddlewareVersionV1;
import com.kingsrook.qqq.middleware.javalin.tools.OpenApiCompatibilityChecker;
import com.kingsrook.qqq.middleware.javalin.tools.UiApiContract;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.OpenAPI;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Tests for the UI API extension point: registration, validation, OpenAPI,
 ** routing, the version endpoint, the raw-route audit and the in-process
 ** invoker.
 *******************************************************************************/
class UiApiExtensionPointTest
{
   private static final int PORT = 6271;

   private QApplicationJavalinServer javalinServer;



   /***************************************************************************
    **
    ***************************************************************************/
   @AfterEach
   void afterEach()
   {
      if(javalinServer != null)
      {
         javalinServer.stop();
         javalinServer = null;
      }

      ////////////////////////////////////////////////////////////////////////////
      // the server sets this static from its dashboard choice; restore default //
      ////////////////////////////////////////////////////////////////////////////
      QJavalinImplementation.setSessionCookieHttpOnly(false);
      Unirest.config().reset();
   }



   /***************************************************************************
    ** validate an instance carrying the given extensions; return the errors
    ***************************************************************************/
   private List<String> validationErrors(MiddlewareSpecExtension... extensions)
   {
      QInstance                        qInstance = TestUtils.defineInstance();
      MiddlewareSpecExtensionsMetaData registry  = MiddlewareSpecExtensionsMetaData.ofOrWithNew(qInstance);
      for(MiddlewareSpecExtension extension : extensions)
      {
         registry.withExtension(extension);
      }
      QInstanceValidator validator = new QInstanceValidator();
      registry.validate(qInstance, validator);
      return (validator.getErrors());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testValidExtensionValidates()
   {
      assertThat(validationErrors(TestExtensionSpecs.echoExtension())).isEmpty();
      assertEquals("/qbit/test-ext", TestExtensionSpecs.echoExtension().getEffectivePathPrefix());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testInvalidExtensions()
   {
      assertThat(validationErrors(TestExtensionSpecs.echoExtension().withName("Bad Name"))).anyMatch(e -> e.contains("name must match"));
      assertThat(validationErrors(TestExtensionSpecs.echoExtension().withSemanticVersion("1.0"))).anyMatch(e -> e.contains("MAJOR.MINOR.PATCH"));
      assertThat(validationErrors(TestExtensionSpecs.echoExtension().withPathPrefix("/custom"))).anyMatch(e -> e.contains("is not allowed"));
      assertThat(validationErrors(TestExtensionSpecs.echoExtension().withName("esb").withPathPrefix("/esb"))).anyMatch(e -> e.contains("must start with /esb"));
      assertThat(validationErrors(TestExtensionSpecs.echoExtension().withSpec(TestExtensionSpecs.WrongPrefixSpec.class))).anyMatch(e -> e.contains("must start with /qbit/test-ext"));
      assertThat(validationErrors(new MiddlewareSpecExtension().withName("empty"))).anyMatch(e -> e.contains("has no specs"));
      assertThat(validationErrors(new MiddlewareSpecExtension().withName("nope").withSpec(new QCodeReference(String.class)))).anyMatch(e -> e.contains("could not be instantiated"));

      ////////////////////////////////////////////////////////////////////////
      // two extensions over the same prefix; and the same operation twice //
      ////////////////////////////////////////////////////////////////////////
      MiddlewareSpecExtension other = TestExtensionSpecs.echoExtension().withName("other").withPathPrefix("/qbit/other");
      assertThat(validationErrors(TestExtensionSpecs.echoExtension(), other)).anyMatch(e -> e.contains("must start with /qbit/other"));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testNormalizePathTemplate()
   {
      assertEquals("/qqq/v1/table/{}/query", MiddlewareSpecExtensionsMetaData.normalizePathTemplate("/qqq//v1/table/{tableName}/query/"));
      assertEquals("/a/{}", MiddlewareSpecExtensionsMetaData.normalizePathTemplate("a/<id>"));
      assertEquals("/", MiddlewareSpecExtensionsMetaData.normalizePathTemplate(""));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testExtensionSpecsInOpenApi() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();
      MiddlewareSpecExtensionsMetaData.ofOrWithNew(qInstance).withExtension(TestExtensionSpecs.echoExtension());

      MiddlewareVersionV1 version = new MiddlewareVersionV1();
      version.setQInstance(qInstance);
      assertEquals(version.getEndpointSpecs().size() + 1, version.getAllEndpointSpecs().size());
      assertThat(version.getExtensions()).containsKey("test-ext");

      OpenAPI openAPI = version.generateOpenAPIModel("qqq");
      assertThat(openAPI.getPaths()).containsKey("/qqq/v1/qbit/test-ext/echo/{word}");
      assertThat(openAPI.getComponents().getSchemas()).containsKey("TestExtEchoResponseV1");
      assertEquals(MiddlewareVersionV1.SEMANTIC_VERSION, openAPI.getInfo().getVersion());

      ///////////////////////////////////////////////////////////////
      // the extension-alone document has only the extension path //
      ///////////////////////////////////////////////////////////////
      OpenAPI extensionOpenAPI = version.generateExtensionOpenAPIModel("qqq", TestExtensionSpecs.echoExtension());
      assertEquals(1, extensionOpenAPI.getPaths().size());
      assertEquals("1.2.3", extensionOpenAPI.getInfo().getVersion());

      ///////////////////////////////////////////////////////////////////////////////////
      // the contract check for a qbit: generated == committed, and versioned properly //
      ///////////////////////////////////////////////////////////////////////////////////
      String yaml = UiApiContract.generateExtensionYaml(version, TestExtensionSpecs.echoExtension());
      assertThat(UiApiContract.check(yaml, yaml, yaml, 1, "regen")).isEmpty();
      assertThat(UiApiContract.check(yaml, yaml.replace("Echo a word", "Echo"), null, 1, "regen")).anyMatch(p -> p.contains("Regenerate"));
   }



   /***************************************************************************
    ** application whose instance registers the echo extension and a raw route
    ***************************************************************************/
   public static class ExtensionApplication extends AbstractQQQApplication
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public QInstance defineQInstance() throws QException
      {
         QInstance qInstance = TestUtils.defineInstance();
         MiddlewareSpecExtensionsMetaData.ofOrWithNew(qInstance).withExtension(TestExtensionSpecs.echoExtension());
         QJavalinMetaData.ofOrWithNew(qInstance).withAdditionalRouteProviderReference(new QCodeReference(TestExtensionSpecs.RawRouteProvider.class));
         return (qInstance);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void startServer(boolean strict) throws QException
   {
      javalinServer = new QApplicationJavalinServer(new ExtensionApplication())
         .withPort(PORT)
         .withServeFrontendNext(false)
         .withServeFrontendMaterialDashboard(false)
         .withServeLegacyUnversionedMiddlewareAPI(false)
         .withStrictUiApiRoutes(strict);
      javalinServer.start();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testServerServesExtensionVersionAndAuditsRawRoutes() throws Exception
   {
      startServer(false);

      ///////////////////////////////
      // the extension route works //
      ///////////////////////////////
      HttpResponse<String> response = Unirest.get("http://localhost:" + PORT + "/qqq/v1/qbit/test-ext/echo/hello").asString();
      assertEquals(200, response.getStatus());
      assertEquals("hello", new JSONObject(response.getBody()).getString("word"));

      ////////////////////////////////////////////////////////////
      // the served spec includes it; the version endpoint too //
      ////////////////////////////////////////////////////////////
      String servedYaml = Unirest.get("http://localhost:" + PORT + "/qqq/v1/openapi.yaml").asString().getBody();
      assertThat(servedYaml).contains("/qqq/v1/qbit/test-ext/echo/{word}");

      JSONObject version = new JSONObject(Unirest.get("http://localhost:" + PORT + "/qqq/v1/version").asString().getBody());
      assertEquals(MiddlewareVersionV1.SEMANTIC_VERSION, version.getString("apiVersion"));
      assertEquals("/qqq/v1", version.getString("basePath"));
      assertEquals(1, version.getJSONArray("supportedMajorVersions").getInt(0));
      assertThat(version.getJSONArray("features").toList()).contains("version", "extensions");
      JSONObject extension = version.getJSONArray("extensions").getJSONObject(0);
      assertEquals("test-ext", extension.getString("name"));
      assertEquals("1.2.3", extension.getString("version"));
      assertEquals("/qbit/test-ext", extension.getString("pathPrefix"));

      //////////////////////////////////////////////////////////////
      // the raw route still works (warn mode) but is reported    //
      //////////////////////////////////////////////////////////////
      assertEquals("raw", Unirest.get("http://localhost:" + PORT + "/qqq/v1/raw-thing").asString().getBody());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testStrictModeFailsOnRawRoutes()
   {
      assertThrows(Exception.class, () -> startServer(true));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testAuditorFindsRawRouteOnly() throws Exception
   {
      startServer(false);
      MiddlewareVersionV1 version = (MiddlewareVersionV1) javalinServer.getMiddlewareVersionList().get(0);
      assertEquals(List.of("GET /qqq/v1/raw-thing"), UiApiRouteAuditor.findUnspecifiedRoutes(getService(), List.of(version)));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private io.javalin.Javalin getService() throws Exception
   {
      java.lang.reflect.Field field = QApplicationJavalinServer.class.getDeclaredField("service");
      field.setAccessible(true);
      return ((io.javalin.Javalin) field.get(javalinServer));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testInvoker() throws Exception
   {
      startServer(false);
      UiApiInvoker invoker = UiApiInvoker.forServer(javalinServer);

      //////////////////////////////////////////////////////////
      // operations listed from the specs, incl. extensions  //
      //////////////////////////////////////////////////////////
      List<UiApiOperation> operations = invoker.listOperations();
      assertThat(operations).anyMatch(o -> o.pathTemplate().equals("/table/{tableName}/query") && o.method() == HttpMethod.POST && o.secured());
      assertThat(operations).anyMatch(o -> o.pathTemplate().equals("/qbit/test-ext/echo/{word}"));
      assertThat(operations).noneMatch(o -> o.pathTemplate().contains("raw-thing"));

      ///////////////////////////////////////////////////////
      // an extension op and a core op, with a session    //
      ///////////////////////////////////////////////////////
      UiApiResponse echo = invoker.invoke(new UiApiRequest()
         .withMethod(HttpMethod.GET)
         .withPath("/qbit/test-ext/echo/{word}")
         .withPathParam("word", "two words")
         .withSessionUUID("a-session")
         .withInvokedBy("test"));
      assertEquals(200, echo.getStatus());
      assertEquals("two words", echo.getJson().get("word").asText());

      UiApiResponse metaData = invoker.invoke(new UiApiRequest()
         .withMethod(HttpMethod.GET)
         .withPath("/metaData")
         .withSessionUUID("a-session")
         .withUserTimezone("America/Chicago"));
      assertTrue(metaData.isSuccess(), metaData.getBody());
      assertTrue(metaData.getJson().has("tables"));

      //////////////////////////////////////////////////////////////////
      // a JSON-body operation reaches the executor (same as over HTTP) //
      //////////////////////////////////////////////////////////////////
      UiApiResponse count = invoker.invoke(new UiApiRequest()
         .withMethod(HttpMethod.POST)
         .withPath("/table/{tableName}/count")
         .withPathParam("tableName", "noSuchTable")
         .withJsonBody(Map.of("filter", Map.of()))
         .withSessionUUID("a-session"));
      assertFalse(count.isSuccess());

      ///////////////////////////////////////////////
      // refused: raw routes, missing session/params //
      ///////////////////////////////////////////////
      assertThrows(QException.class, () -> invoker.invoke(new UiApiRequest().withMethod(HttpMethod.GET).withPath("/raw-thing").withSessionUUID("x")));
      assertThrows(QException.class, () -> invoker.invoke(new UiApiRequest().withMethod(HttpMethod.POST).withPath("/table/{tableName}/query").withPathParam("tableName", "person")));
      assertThrows(QException.class, () -> invoker.invoke(new UiApiRequest().withMethod(HttpMethod.GET).withPath("/qbit/test-ext/echo/{word}").withSessionUUID("x")));
      assertThrows(QException.class, () -> invoker.invoke(new UiApiRequest()));

      /////////////////////////////////////////////////////////
      // version needs no session; and is found by template //
      /////////////////////////////////////////////////////////
      assertFalse(invoker.findOperation(HttpMethod.GET, "/version").orElseThrow().secured());
      assertEquals(200, invoker.invoke(new UiApiRequest().withMethod(HttpMethod.GET).withPath("/version")).getStatus());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testSemverCompare()
   {
      assertTrue(OpenApiCompatibilityChecker.compareSemver("1.10.0", "1.9.9") > 0);
      assertEquals(0, OpenApiCompatibilityChecker.compareSemver("1.0.0", "1.0"));
      assertTrue(OpenApiCompatibilityChecker.compareSemver("v1", "1.0.1") < 0);
   }
}
