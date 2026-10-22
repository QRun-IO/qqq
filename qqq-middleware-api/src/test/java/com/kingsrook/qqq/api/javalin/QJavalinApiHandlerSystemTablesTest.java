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


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.api.BaseTest;
import com.kingsrook.qqq.api.TestUtils;
import com.kingsrook.qqq.api.actions.ApiImplementation;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaData;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaDataContainer;
import com.kingsrook.qqq.backend.core.actions.permissions.SystemTablePermissions;
import com.kingsrook.qqq.backend.core.model.audits.AuditsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.metadata.RedirectStateMetaDataProducer;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.metadata.UserSessionMetaDataProducer;
import com.kingsrook.qqq.middleware.javalin.QJavalinImplementation;
import io.javalin.apibuilder.EndpointGroup;
import kong.unirest.Unirest;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static com.kingsrook.qqq.api.javalin.QJavalinApiHandlerTest.assertErrorResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** QQQ's system tables through the published (model) API, even when an
 ** application exposes them in its API: a session without audit.read gets 403
 ** on the audit tables, and the (hidden) session tables are absent - in an
 ** instance whose default rules leave its other tables unprotected.
 *******************************************************************************/
class QJavalinApiHandlerSystemTablesTest extends BaseTest
{
   private static final int    PORT     = 6266;
   private static final String BASE_URL = "http://localhost:" + PORT;
   private static final String VERSION  = TestUtils.V2023_Q1;

   private static QJavalinImplementation qJavalinImplementation;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeAll
   static void beforeAll() throws Exception
   {
      QInstance qInstance = TestUtils.defineInstance();
      new AuditsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);
      qInstance.addTable(new UserSessionMetaDataProducer(TestUtils.MEMORY_BACKEND_NAME).produce(qInstance));
      qInstance.addTable(new RedirectStateMetaDataProducer(TestUtils.MEMORY_BACKEND_NAME).produce(qInstance));
      for(String tableName : systemTables())
      {
         qInstance.getTable(tableName).withSupplementalMetaData(new ApiTableMetaDataContainer()
            .withApiTableMetaData(TestUtils.API_NAME, new ApiTableMetaData().withInitialVersion(TestUtils.V2022_Q4)));
      }

      qJavalinImplementation = new QJavalinImplementation(qInstance);
      qJavalinImplementation.clearJavalinRoutes();
      EndpointGroup routes = new QJavalinApiHandler(qInstance).getRoutes();
      qJavalinImplementation.addJavalinRoutes(routes);
      qJavalinImplementation.startJavalinServer(PORT);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   @AfterEach
   void beforeAndAfterEach()
   {
      ApiImplementation.clearCaches();
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
    ** query and get of every system table are 403; an ordinary table is not.
    *******************************************************************************/
   @Test
   void testSystemTablesAreForbidden()
   {
      for(String tableName : SystemTablePermissions.AUDIT_TABLE_NAMES)
      {
         assertErrorResponse(HttpStatus.FORBIDDEN_403, "You do not have permission", Unirest.get(BASE_URL + "/api/" + VERSION + "/" + tableName + "/query").asString());
         assertErrorResponse(HttpStatus.FORBIDDEN_403, "You do not have permission", Unirest.get(BASE_URL + "/api/" + VERSION + "/" + tableName + "/1").asString());
      }

      /////////////////////////////////////////////////////////////////////
      // the internal (hidden) tables are absent from the API altogether //
      /////////////////////////////////////////////////////////////////////
      for(String tableName : List.of(SystemTablePermissions.INTERNAL_TABLE_NAMES.get(0), SystemTablePermissions.INTERNAL_TABLE_NAMES.get(1)))
      {
         assertEquals(HttpStatus.NOT_FOUND_404, Unirest.get(BASE_URL + "/api/" + VERSION + "/" + tableName + "/query").asString().getStatus(), tableName);
         assertEquals(HttpStatus.NOT_FOUND_404, Unirest.get(BASE_URL + "/api/" + VERSION + "/" + tableName + "/1").asString().getStatus(), tableName);
      }

      assertEquals(HttpStatus.OK_200, Unirest.get(BASE_URL + "/api/" + VERSION + "/" + TestUtils.TABLE_NAME_PERSON + "/query").asString().getStatus());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<String> systemTables()
   {
      List<String> rs = new ArrayList<>(SystemTablePermissions.AUDIT_TABLE_NAMES);
      rs.add(SystemTablePermissions.INTERNAL_TABLE_NAMES.get(0));
      rs.add(SystemTablePermissions.INTERNAL_TABLE_NAMES.get(1));
      return (rs);
   }
}
