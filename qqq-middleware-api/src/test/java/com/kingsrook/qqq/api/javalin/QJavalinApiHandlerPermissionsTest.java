/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.api.javalin;


import com.kingsrook.qqq.api.BaseTest;
import com.kingsrook.qqq.api.TestUtils;
import com.kingsrook.qqq.api.actions.ApiImplementation;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
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


/*******************************************************************************
 ** Unit test permissions within QJavalinApiHandler
 *******************************************************************************/
class QJavalinApiHandlerPermissionsTest extends BaseTest
{
   private static final   int    PORT     = 6263;
   protected static final String BASE_URL = "http://localhost:" + PORT;

   private static final String VERSION = "2023.Q1";

   protected static QJavalinImplementation qJavalinImplementation;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeAll
   static void beforeAll() throws Exception
   {
      QInstance qInstance = TestUtils.defineInstance();

      ///////////////////////////////////////////////////
      // turn on permissions on all tables & processes //
      ///////////////////////////////////////////////////
      for(QTableMetaData table : qInstance.getTables().values())
      {
         table.withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      }

      for(QProcessMetaData process : qInstance.getProcesses().values())
      {
         process.withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
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
    ** Before the class (all) runs, start a javalin server.
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
   @Test
   void test403s()
   {
      ////////////////////////////
      // tables - single & bulk //
      ////////////////////////////
      assertErrorResponse(HttpStatus.FORBIDDEN_403, "You do not have permission", Unirest.get(BASE_URL + "/api/" + VERSION + "/order/query").asString());
      assertErrorResponse(HttpStatus.FORBIDDEN_403, "You do not have permission", Unirest.get(BASE_URL + "/api/" + VERSION + "/order/1").asString());
      assertErrorResponse(HttpStatus.FORBIDDEN_403, "You do not have permission", Unirest.post(BASE_URL + "/api/" + VERSION + "/person").asString());
      assertErrorResponse(HttpStatus.FORBIDDEN_403, "You do not have permission", Unirest.post(BASE_URL + "/api/" + VERSION + "/person/bulk").asString());
      assertErrorResponse(HttpStatus.FORBIDDEN_403, "You do not have permission", Unirest.delete(BASE_URL + "/api/" + VERSION + "/person/1").asString());
      assertErrorResponse(HttpStatus.FORBIDDEN_403, "You do not have permission", Unirest.delete(BASE_URL + "/api/" + VERSION + "/person/bulk").asString());
      assertErrorResponse(HttpStatus.FORBIDDEN_403, "You do not have permission", Unirest.patch(BASE_URL + "/api/" + VERSION + "/person/1").asString());
      assertErrorResponse(HttpStatus.FORBIDDEN_403, "You do not have permission", Unirest.patch(BASE_URL + "/api/" + VERSION + "/person/bulk").asString());

      ///////////////
      // processes //
      ///////////////
      assertErrorResponse(HttpStatus.FORBIDDEN_403, "You do not have permission", Unirest.get(BASE_URL + "/api/" + VERSION + "/person/getPersonInfo").asString());
      assertErrorResponse(HttpStatus.FORBIDDEN_403, "You do not have permission", Unirest.post(BASE_URL + "/api/" + VERSION + "/person/transformPeople").asString());
   }

}
