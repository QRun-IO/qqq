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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.audits.AuditAction;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.SystemTablePermissions;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.audits.AuditsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.querystats.QueryStatMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.model.tables.QQQTablesMetaDataProvider;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.metadata.RedirectStateMetaDataProducer;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.metadata.UserSessionMetaDataProducer;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.metadata.UserSessionRefreshTokenMetaDataProducer;
import com.kingsrook.qqq.backend.core.processes.implementations.audits.GetAuditsForRecordProcess;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import io.javalin.http.ContentType;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** QQQ's system tables through the UI API (/qqq/v1), in an instance whose
 ** default permission rules leave everything else unprotected:
 ** - no permissions: query, get, count, export and possible values of every
 **   system table are 403, and the tables are absent from the meta-data -
 **   but the history of a record the user can read still comes back;
 ** - audit.read: the audit tables are ordinary tables;
 ** - every permission the instance offers (an admin): still no session,
 **   redirect-state or refresh-token table.
 *******************************************************************************/
class SystemTablePermissionsSpecV1Test extends SpecTestBase
{
   private static final String AUDITOR = "auditor";
   private static final String ADMIN   = "admin";
   private static final String NOBODY  = "nobody";



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new TableQuerySpecV1();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected List<AbstractEndpointSpec<?, ?, ?>> getAdditionalSpecs()
   {
      return (List.of(new TableGetSpecV1(), new TableCountSpecV1(), new TableExportSpecV1(), new TableMetaDataSpecV1(),
         new StandalonePossibleValuesSpecV1(), new ProcessInitSpecV1()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected String getVersion()
   {
      return "v1";
   }



   /***************************************************************************
    ** The standard test instance plus every system table, with a session
    ** customizer granting permissions by session id.
    ***************************************************************************/
   @Override
   protected QInstance defineQInstance() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();
      new AuditsMetaDataProvider().defineAll(qInstance, TestUtils.BACKEND_NAME_MEMORY, null);
      new QQQTablesMetaDataProvider().defineAll(qInstance, TestUtils.BACKEND_NAME_MEMORY, TestUtils.BACKEND_NAME_MEMORY, null);
      new QueryStatMetaDataProvider().defineAll(qInstance, TestUtils.BACKEND_NAME_MEMORY, null);
      qInstance.addTable(new UserSessionMetaDataProducer(TestUtils.BACKEND_NAME_MEMORY).produce(qInstance));
      qInstance.addTable(new RedirectStateMetaDataProducer(TestUtils.BACKEND_NAME_MEMORY).produce(qInstance));
      qInstance.addTable(new UserSessionRefreshTokenMetaDataProducer(TestUtils.BACKEND_NAME_MEMORY).produce(qInstance));
      qInstance.getAuthentication().withCustomizer(new QCodeReference(Permissions.class));
      return (qInstance);
   }



   /***************************************************************************
    ** Also store an audit of person 1.
    ***************************************************************************/
   @Override
   protected void primeTestData(QInstance qInstance) throws Exception
   {
      super.primeTestData(qInstance);
      QContext.withTemporaryContext(new CapturedContext(qInstance, new QSession().withUser(new QUser().withIdReference("editor").withFullName("Editor"))), () ->
         AuditAction.execute(TestUtils.TABLE_NAME_PERSON, 1, Map.of(), "Person 1 was edited"));
   }



   /*******************************************************************************
    ** Grant permissions by the session id cookie.
    *******************************************************************************/
   public static class Permissions implements QAuthenticationModuleCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void customizeSession(QInstance instance, QSession session, Map<String, Object> context)
      {
         if(AUDITOR.equals(session.getUuid()))
         {
            session.withPermission(SystemTablePermissions.AUDIT_READ_PERMISSION);
         }
         else if(ADMIN.equals(session.getUuid()))
         {
            QContext.withTemporaryContext(new CapturedContext(instance, new QSystemUserSession()), () ->
               session.withPermissions(PermissionsHelper.getAllAvailablePermissionNames(instance)));
         }
      }
   }



   /*******************************************************************************
    ** No permissions: every endpoint on every system table is denied, and none
    ** of the tables is in the meta-data; an ordinary table is fine.
    *******************************************************************************/
   @Test
   void testNoPermissionSessionIsDeniedEverySystemTable()
   {
      for(String tableName : allSystemTables())
      {
         assertEveryTableEndpoint(NOBODY, tableName, 403);
      }

      assertEveryTableEndpoint(NOBODY, TestUtils.TABLE_NAME_PERSON, 200);

      HttpResponse<String> metaData = get(NOBODY, "/metaData/table/" + AuditsMetaDataProvider.TABLE_NAME_AUDIT);
      assertThat(metaData.getStatus()).isIn(403, 404);

      for(String possibleValueSource : List.of(AuditsMetaDataProvider.TABLE_NAME_AUDIT, AuditsMetaDataProvider.TABLE_NAME_AUDIT_USER, AuditsMetaDataProvider.TABLE_NAME_AUDIT_TABLE))
      {
         HttpResponse<String> response = post(NOBODY, "/possibleValues/" + possibleValueSource, "{}");
         assertEquals(403, response.getStatus(), possibleValueSource);
         assertThat(response.getBody()).doesNotContain("Editor");
      }
   }



   /*******************************************************************************
    ** No permissions, but read on the person table: person 1's history.
    *******************************************************************************/
   @Test
   void testRecordHistoryWithoutAuditPermission()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/processes/" + GetAuditsForRecordProcess.NAME + "/init")
         .cookie("sessionId", NOBODY)
         .multiPartContent()
         .field("values", JsonUtils.toJson(Map.of("tableName", TestUtils.TABLE_NAME_PERSON, "recordId", "1")))
         .asString();
      assertEquals(200, response.getStatus(), response.getBody());
      JSONObject values = JsonUtils.toJSONObject(response.getBody()).getJSONObject("values");
      assertEquals(1, values.getJSONArray("audits").length());
      assertThat(response.getBody()).contains("Person 1 was edited");
   }



   /*******************************************************************************
    ** audit.read: the audit tables are ordinary tables; the others are not.
    *******************************************************************************/
   @Test
   void testAuditorSeesTheAuditTables()
   {
      for(String tableName : SystemTablePermissions.AUDIT_TABLE_NAMES)
      {
         assertEveryTableEndpoint(AUDITOR, tableName, 200);
      }
      assertThat(post(AUDITOR, "/table/" + AuditsMetaDataProvider.TABLE_NAME_AUDIT + "/query", "{}").getBody()).contains("Person 1 was edited");
      assertEquals(200, get(AUDITOR, "/metaData/table/" + AuditsMetaDataProvider.TABLE_NAME_AUDIT).getStatus());
      assertEquals(200, post(AUDITOR, "/possibleValues/" + AuditsMetaDataProvider.TABLE_NAME_AUDIT_USER, "{}").getStatus());

      for(String tableName : SystemTablePermissions.QUERY_STAT_TABLE_NAMES)
      {
         assertEveryTableEndpoint(AUDITOR, tableName, 403);
      }
      for(String tableName : SystemTablePermissions.INTERNAL_TABLE_NAMES)
      {
         assertEveryTableEndpoint(AUDITOR, tableName, 403);
      }
   }



   /*******************************************************************************
    ** Every permission the instance offers: audit and query statistics, but
    ** never the internal tables.
    *******************************************************************************/
   @Test
   void testAdminNeverSeesTheInternalTables()
   {
      for(String tableName : SystemTablePermissions.AUDIT_TABLE_NAMES)
      {
         assertEveryTableEndpoint(ADMIN, tableName, 200);
      }
      for(String tableName : SystemTablePermissions.QUERY_STAT_TABLE_NAMES)
      {
         assertEveryTableEndpoint(ADMIN, tableName, 200);
      }
      for(String tableName : SystemTablePermissions.INTERNAL_TABLE_NAMES)
      {
         assertEveryTableEndpoint(ADMIN, tableName, 403);
      }
   }



   /*******************************************************************************
    ** query, count, get and export of a table, as a session, all return the
    ** expected status (for get, 404 counts as allowed: there is no record 1 in
    ** most of these tables).
    *******************************************************************************/
   private void assertEveryTableEndpoint(String sessionId, String tableName, int expectedStatus)
   {
      String path = "/table/" + tableName;
      assertEquals(expectedStatus, post(sessionId, path + "/query", "{}").getStatus(), sessionId + " query " + tableName);
      assertEquals(expectedStatus, post(sessionId, path + "/count", "{}").getStatus(), sessionId + " count " + tableName);
      assertEquals(expectedStatus, post(sessionId, path + "/export", JsonUtils.toJson(Map.of("format", "csv"))).getStatus(), sessionId + " export " + tableName);

      int getStatus = get(sessionId, path + "/1").getStatus();
      if(expectedStatus == 200)
      {
         assertThat(getStatus).as(sessionId + " get " + tableName).isIn(200, 404);
      }
      else
      {
         assertEquals(expectedStatus, getStatus, sessionId + " get " + tableName);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<String> get(String sessionId, String path)
   {
      return (Unirest.get(getBaseUrlAndPath() + path).cookie("sessionId", sessionId).asString());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<String> post(String sessionId, String path, String body)
   {
      return (Unirest.post(getBaseUrlAndPath() + path).cookie("sessionId", sessionId).contentType(ContentType.APPLICATION_JSON.getMimeType()).body(body).asString());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<String> allSystemTables()
   {
      List<String> rs = new ArrayList<>(SystemTablePermissions.AUDIT_TABLE_NAMES);
      rs.addAll(SystemTablePermissions.QUERY_STAT_TABLE_NAMES);
      rs.addAll(SystemTablePermissions.INTERNAL_TABLE_NAMES);
      return (rs);
   }
}
