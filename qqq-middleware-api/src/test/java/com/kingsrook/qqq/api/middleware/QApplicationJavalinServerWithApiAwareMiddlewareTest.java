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

package com.kingsrook.qqq.api.middleware;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.api.middleware.specs.v1.ApiAwareMiddlewareVersionV1;
import com.kingsrook.qqq.api.model.APIVersion;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaData;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaDataContainer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.AbstractQQQApplication;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.middleware.javalin.QApplicationJavalinServer;
import com.kingsrook.qqq.middleware.javalin.QJavalinRouteProviderInterface;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractMiddlewareVersion;
import com.kingsrook.qqq.middleware.javalin.specs.v1.MiddlewareVersionV1;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.apibuilder.EndpointGroup;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 * Test serving api-aware middleware endpoints through a {@link QApplicationJavalinServer}
 *******************************************************************************/
public class QApplicationJavalinServerWithApiAwareMiddlewareTest
{
   private static final int PORT = 6265;

   private static List<String>     apiNames    = List.of("full-api", "simple-api");
   private static List<APIVersion> apiVersions = List.of(new APIVersion("v1"), new APIVersion("v2"));



   /*******************************************************************************
    * This is a regression test built when it was discovered that serving
    * api-versioned middleware would register wildcard endpoints that conflicted
    * with some non-wildcard paths from the non-api versioned middleware.
    *
    * Specifically:
    * /qqq/v1/{applicationApiPath}/{applicationApiVersion}/ (the open api doc index page)
    *
    * Was registered before (and would therefore clobber):
    * /qqq/v1/metaData/authentication (meta-data about how to authenticate).
    * Additional route providers registered after documentation also need to
    * retain their JSON and permission-refusal responses (QRun-IO/qqq#977).
    *******************************************************************************/
   @Test
   void testDocumentationPathsDoNotClobberMiddlewareOrAdditionalProviders() throws QException
   {
      AbstractQQQApplication minimalApplication = createMinimalApplication();

      //////////////////////////////////////////////
      // set up an application server, with       //
      // v1 of the versioned-middleware and also  //
      // api-aware versions of the v1 middleware. //
      //////////////////////////////////////////////
      QApplicationJavalinServer javalinServer = new QApplicationJavalinServer(minimalApplication);
      javalinServer.setPort(PORT);
      javalinServer.setServeFrontendMaterialDashboard(false);
      javalinServer.setServeLegacyUnversionedMiddlewareAPI(false);

      List<AbstractMiddlewareVersion> middlewareVersionList = new ArrayList<>();
      middlewareVersionList.add(new MiddlewareVersionV1());
      javalinServer.withMiddlewareVersionList(middlewareVersionList);

      QContext.setQInstance(minimalApplication.defineQInstance());

      ApiAwareMiddlewareVersionV1 apiAwareMiddlewareV1 = new ApiAwareMiddlewareVersionV1();
      middlewareVersionList.add(apiAwareMiddlewareV1);
      for(String apiName : apiNames)
      {
         for(APIVersion apiVersion : apiVersions)
         {
            apiAwareMiddlewareV1.addVersion(apiName, apiVersion);
         }
      }

      javalinServer.setServeFrontendNext(false);
      javalinServer.withAdditionalRouteProviders(List.of(new QJavalinRouteProviderInterface()
      {
         /***************************************************************************
          ** This provider isolates route precedence without depending on qqq-esb.
          ***************************************************************************/
         @Override
         public void setQInstance(QInstance qInstance)
         {
         }



         /***************************************************************************
          ** Exercise the real ESB path with both successful and refused responses.
          ***************************************************************************/
         @Override
         public EndpointGroup getJavalinEndpointGroup()
         {
            return (() -> ApiBuilder.get("/qqq/v1/esb/overview", context ->
            {
               if("true".equals(context.header("X-Test-Denied")))
               {
                  context.status(403).json(Map.of("error", "Permission denied."));
               }
               else
               {
                  context.json(Map.of("providers", List.of(), "destinations", List.of()));
               }
            }));
         }
      }));

      javalinServer.start();
      try
      {
         //////////////////////////////////////////////////////////////////////////////////////////////
         // do a basic control test, fetching /metaData (which didn't have an issue before this bug) //
         //////////////////////////////////////////////////////////////////////////////////////////////
         HttpResponse<String> metaDataResponse = Unirest.get("http://localhost:" + PORT + "/qqq/v1/metaData").asString();
         assertEquals(200, metaDataResponse.getStatus());
         JSONObject metaData = new JSONObject(metaDataResponse.getBody());
         JSONObject tables   = metaData.getJSONObject("tables");

         /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // now do the condition we're regression testing for - the /middleware/authentication path, which did have the bug //
         /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         HttpResponse<String> authMetaDataResponse = Unirest.get("http://localhost:" + PORT + "/qqq/v1/metaData/authentication").asString();
         assertEquals(200, authMetaDataResponse.getStatus());
         JSONObject authenticationMetaData = new JSONObject(authMetaDataResponse.getBody());
         String     authenticationType     = authenticationMetaData.getString("type");
         assertEquals("FULLY_ANONYMOUS", authenticationType);

         HttpResponse<String> overview = Unirest.get("http://localhost:" + PORT + "/qqq/v1/esb/overview").asString();
         assertEquals(200, overview.getStatus());
         assertTrue(overview.getHeaders().getFirst("Content-Type").startsWith("application/json"));
         assertTrue(new JSONObject(overview.getBody()).has("providers"));
         assertTrue(new JSONObject(overview.getBody()).has("destinations"));

         HttpResponse<String> denied = Unirest.get("http://localhost:" + PORT + "/qqq/v1/esb/overview").header("X-Test-Denied", "true").asString();
         assertEquals(403, denied.getStatus());
         assertTrue(denied.getHeaders().getFirst("Content-Type").startsWith("application/json"));
         assertEquals("Permission denied.", new JSONObject(denied.getBody()).getString("error"));

         for(String apiName : apiNames)
         {
            for(APIVersion apiVersion : apiVersions)
            {
               String docsPath = "http://localhost:" + PORT + "/qqq/v1/" + apiName + "/" + apiVersion;
               for(String suffix : List.of("", "/openapi.html"))
               {
                  HttpResponse<String> docs = Unirest.get(docsPath + suffix).asString();
                  assertEquals(200, docs.getStatus());
                  assertTrue(docs.getHeaders().getFirst("Content-Type").startsWith("text/html"));
                  assertTrue(docs.getBody().contains("QQQ Middleware API - v1"));
               }
               HttpResponse<String> spec = Unirest.get(docsPath + "/openapi.json").asString();
               assertEquals(200, spec.getStatus());
               assertTrue(new JSONObject(spec.getBody()).has("openapi"));
               HttpResponse<String> yaml = Unirest.get(docsPath + "/openapi.yaml").asString();
               assertEquals(200, yaml.getStatus());
               assertTrue(yaml.getBody().contains("openapi:"));
            }
         }
         assertEquals(404, Unirest.get("http://localhost:" + PORT + "/qqq/v1/unknown/v1").asString().getStatus());
         assertEquals(404, Unirest.get("http://localhost:" + PORT + "/qqq/v1/full-api/unknown").asString().getStatus());
      }
      finally
      {
         javalinServer.stop();
         QContext.clear();
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private static AbstractQQQApplication createMinimalApplication()
   {
      return new AbstractQQQApplication()
      {
         /***************************************************************************
          *
          ***************************************************************************/
         @Override
         public QInstance defineQInstance()
         {
            QInstance qInstance = new QInstance();
            qInstance.addBackend(new QBackendMetaData().withBackendType(MemoryBackendModule.class).withName("memory"));
            qInstance.registerAuthenticationProvider(AuthScope.instanceDefault(), new QAuthenticationMetaData().withName("anon").withType(QAuthenticationType.FULLY_ANONYMOUS));
            qInstance.addTable(new QTableMetaData()
               .withName("table")
               .withBackendName("memory")
               .withField(new QFieldMetaData("id", QFieldType.INTEGER))
               .withPrimaryKeyField("id"));

            ApiInstanceMetaDataContainer apiInstanceMetaDataContainer = new ApiInstanceMetaDataContainer();
            qInstance.add(apiInstanceMetaDataContainer);

            //////////////////////////////////
            // define apis in this instance //
            //////////////////////////////////
            for(String apiName : apiNames)
            {
               apiInstanceMetaDataContainer.withApiInstanceMetaData(new ApiInstanceMetaData()
                  .withName(apiName)
                  .withLabel(apiName)
                  .withDescription(apiName + " description")
                  .withContactEmail("contact@kingsrook.com")
                  .withPath("/" + apiName + "/")
                  .withCurrentVersion(apiVersions.getLast())
                  .withPastVersions(List.of(apiVersions.getFirst()))
                  .withSupportedVersions(apiVersions)
               );
            }

            return qInstance;
         }
      };
   }

}
