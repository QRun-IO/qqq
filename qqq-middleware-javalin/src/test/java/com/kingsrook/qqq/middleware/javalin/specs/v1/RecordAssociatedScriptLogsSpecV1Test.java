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


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.executors.utils.RecordDeveloperModeUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for RecordAssociatedScriptLogsSpecV1
 *******************************************************************************/
class RecordAssociatedScriptLogsSpecV1Test extends SpecTestBase
{
   private static final String LOGS_PATH = "/table/person/1/developer/associatedScript/testScriptId/100/logs";



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new RecordAssociatedScriptLogsSpecV1();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected String getVersion()
   {
      return "v1";
   }



   /*******************************************************************************
    ** The revision's logs, newest first, each with its lines (as records with
    ** values) - and not other revisions' logs.
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      Instant timestamp = Instant.parse("2026-09-25T12:00:00Z");
      QContext.init(serverQInstance, new QSystemUserSession());
      new InsertAction().execute(new InsertInput("scriptLog").withRecords(List.of(
         new QRecord().withValue("id", 1).withValue("output", "testOutput").withValue("scriptRevisionId", 100),
         new QRecord().withValue("id", 2).withValue("output", "secondOutput").withValue("scriptRevisionId", 100),
         new QRecord().withValue("id", 3).withValue("output", "otherRevision").withValue("scriptRevisionId", 200))));
      new InsertAction().execute(new InsertInput("scriptLogLine").withRecords(List.of(
         new QRecord().withValue("scriptLogId", 1).withValue("text", "line one").withValue("timestamp", timestamp),
         new QRecord().withValue("scriptLogId", 1).withValue("text", "line two").withValue("timestamp", timestamp),
         new QRecord().withValue("scriptLogId", 3).withValue("text", "other line").withValue("timestamp", timestamp))));
      QContext.clear();

      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + LOGS_PATH).asString();
      assertEquals(200, response.getStatus(), response.getBody());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals(Set.of("scriptLogRecords"), jsonObject.keySet());

      JSONArray scriptLogRecords = jsonObject.getJSONArray("scriptLogRecords");
      assertEquals(2, scriptLogRecords.length());
      assertEquals(2, scriptLogRecords.getJSONObject(0).getJSONObject("values").getInt("id"));
      assertEquals("secondOutput", scriptLogRecords.getJSONObject(0).getJSONObject("values").getString("output"));

      JSONObject firstLog = scriptLogRecords.getJSONObject(1);
      assertEquals("scriptLog", firstLog.getString("tableName"));
      assertEquals("testOutput", firstLog.getJSONObject("values").getString("output"));
      JSONArray lines = firstLog.getJSONObject("values").getJSONArray("scriptLogLine");
      assertEquals(2, lines.length());
      assertEquals("line one", lines.getJSONObject(0).getJSONObject("values").getString("text"));
      assertEquals("2026-09-25T12:00:00Z", lines.getJSONObject(0).getJSONObject("values").getString("timestamp"));
      assertEquals("line two", lines.getJSONObject(1).getJSONObject("values").getString("text"));
   }



   /*******************************************************************************
    ** At most 100 logs are returned (the newest).
    *******************************************************************************/
   @Test
   void testLimit() throws QException
   {
      QContext.init(serverQInstance, new QSystemUserSession());
      List<QRecord> logs = new ArrayList<>();
      for(int i = 1; i <= RecordDeveloperModeUtils.SCRIPT_LOG_LIMIT + 5; i++)
      {
         logs.add(new QRecord().withValue("id", i).withValue("scriptRevisionId", 100));
      }
      new InsertAction().execute(new InsertInput("scriptLog").withRecords(logs));
      QContext.clear();

      JSONArray scriptLogRecords = JsonUtils.toJSONObject(Unirest.get(getBaseUrlAndPath() + LOGS_PATH).asString().getBody()).getJSONArray("scriptLogRecords");
      assertEquals(RecordDeveloperModeUtils.SCRIPT_LOG_LIMIT, scriptLogRecords.length());
      assertEquals(RecordDeveloperModeUtils.SCRIPT_LOG_LIMIT + 5, scriptLogRecords.getJSONObject(0).getJSONObject("values").getInt("id"));
   }



   /*******************************************************************************
    ** No logs:  an empty list.
    *******************************************************************************/
   @Test
   void testNoLogs()
   {
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + LOGS_PATH).asString();
      assertEquals(200, response.getStatus(), response.getBody());
      assertEquals(0, JsonUtils.toJSONObject(response.getBody()).getJSONArray("scriptLogRecords").length());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRecordNotFound()
   {
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/table/person/999999/developer/associatedScript/testScriptId/100/logs").asString();
      assertEquals(HttpStatus.NOT_FOUND_404, response.getStatus());
      assertEquals("Could not find Person with Id of 999999", JsonUtils.toJSONObject(response.getBody()).getString("error"));
   }



   /*******************************************************************************
    ** Without READ permission on the record's table, the logs are refused.
    *******************************************************************************/
   @Test
   void testPermissionDenied() throws QException
   {
      QContext.init(serverQInstance, new QSystemUserSession());
      new InsertAction().execute(new InsertInput("scriptLog").withRecords(List.of(new QRecord().withValue("id", 1).withValue("output", "testOutput").withValue("scriptRevisionId", 100))));
      QContext.clear();

      serverQInstance.getTable(TestUtils.TABLE_NAME_PERSON).setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + LOGS_PATH).asString();
      assertEquals(HttpStatus.FORBIDDEN_403, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals(Set.of("error"), jsonObject.keySet());
      assertThat(jsonObject.getString("error")).contains("Permission denied");

      serverQInstance.getAuthentication().setCustomizer(new QCodeReference(PersonReadPermission.class));
      response = Unirest.get(getBaseUrlAndPath() + LOGS_PATH).asString();
      assertEquals(200, response.getStatus(), response.getBody());
      assertEquals(1, JsonUtils.toJSONObject(response.getBody()).getJSONArray("scriptLogRecords").length());
   }



   /*******************************************************************************
    ** Grant read (only) on the person table.
    *******************************************************************************/
   public static class PersonReadPermission implements QAuthenticationModuleCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void customizeSession(QInstance instance, QSession session, Map<String, Object> context)
      {
         session.withPermissions("person.read");
      }
   }

}
