/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.api.middleware;


import java.util.ArrayList;
import java.util.List;
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
import com.kingsrook.qqq.middleware.javalin.specs.AbstractMiddlewareVersion;
import com.kingsrook.qqq.middleware.javalin.specs.v1.MiddlewareVersionV1;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


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
    *******************************************************************************/
   @Test
   void testWildcardPathsDoNotClobberMetaDataAuthenticationEndpoint() throws QException
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

      javalinServer.start();

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
