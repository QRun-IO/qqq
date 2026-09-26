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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
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
import static org.junit.jupiter.api.Assertions.assertFalse;


/*******************************************************************************
 ** Unit test for RecordDeveloperModeSpecV1
 *******************************************************************************/
class RecordDeveloperModeSpecV1Test extends SpecTestBase
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new RecordDeveloperModeSpecV1();
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
    ** A record that references a script:  the record (with values and display
    ** values), and its associated script with type, script, revisions (newest
    ** first) and test fields.
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      insertScriptType();
      QContext.init(serverQInstance, new QSystemUserSession());
      new UpdateAction().execute(new UpdateInput(TestUtils.TABLE_NAME_PERSON).withRecords(List.of(new QRecord().withValue("id", 1).withValue("testScriptId", 47))));
      new InsertAction().execute(new InsertInput("script").withRecords(List.of(new QRecord().withValue("id", 47).withValue("name", "Darin's Script").withValue("scriptTypeId", 1).withValue("currentScriptRevisionId", 1001))));
      new InsertAction().execute(new InsertInput("scriptRevision").withRecords(List.of(
         new QRecord().withValue("id", 1000).withValue("scriptId", 47).withValue("sequenceNo", 1).withValue("content", "var i;"),
         new QRecord().withValue("id", 1001).withValue("scriptId", 47).withValue("sequenceNo", 2).withValue("content", "var j;"),
         new QRecord().withValue("id", 2000).withValue("scriptId", 48).withValue("sequenceNo", 1).withValue("content", "var other;"))));
      QContext.clear();

      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/table/person/1/developer").asString();
      assertEquals(200, response.getStatus(), response.getBody());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals(Set.of("record", "associatedScripts"), jsonObject.keySet());

      JSONObject record = jsonObject.getJSONObject("record");
      assertEquals("person", record.getString("tableName"));
      assertEquals("Darin", record.getJSONObject("values").getString("firstName"));
      assertEquals(47, record.getJSONObject("values").getInt("testScriptId"));
      assertEquals("Darin", record.getJSONObject("displayValues").getString("firstName"));

      JSONArray associatedScripts = jsonObject.getJSONArray("associatedScripts");
      assertEquals(1, associatedScripts.length());
      JSONObject associatedScript = associatedScripts.getJSONObject(0);

      /////////////////////////////////////////////////////////////////////////////
      // the definition is only its field and script type (not its tester class) //
      /////////////////////////////////////////////////////////////////////////////
      JSONObject definition = associatedScript.getJSONObject("associatedScript");
      assertEquals(Set.of("fieldName", "scriptTypeId"), definition.keySet());
      assertEquals("testScriptId", definition.getString("fieldName"));
      assertEquals(1, definition.getInt("scriptTypeId"));

      assertEquals("Test", associatedScript.getJSONObject("scriptType").getJSONObject("values").getString("name"));
      JSONObject script = associatedScript.getJSONObject("script");
      assertEquals(1001, script.getJSONObject("values").getInt("currentScriptRevisionId"));
      assertEquals("Darin's Script", script.getJSONObject("values").getString("name"));

      JSONArray scriptRevisions = associatedScript.getJSONArray("scriptRevisions");
      assertEquals(2, scriptRevisions.length());
      assertEquals(1001, scriptRevisions.getJSONObject(0).getJSONObject("values").getInt("id"));
      assertEquals("var j;", scriptRevisions.getJSONObject(0).getJSONObject("values").getString("content"));
      assertEquals(1000, scriptRevisions.getJSONObject(1).getJSONObject("values").getInt("id"));

      JSONArray testInputFields = associatedScript.getJSONArray("testInputFields");
      assertEquals(2, testInputFields.length());
      assertEquals("name", testInputFields.getJSONObject(0).getString("name"));
      assertEquals("Name", testInputFields.getJSONObject(0).getString("label"));
      assertEquals("STRING", testInputFields.getJSONObject(0).getString("type"));
      assertEquals("age", testInputFields.getJSONObject(1).getString("name"));
      assertEquals("message", associatedScript.getJSONArray("testOutputFields").getJSONObject(0).getString("name"));
   }



   /*******************************************************************************
    ** A record that does not reference a script still lists the associated
    ** script (with its type and test fields) - but no script or revisions.
    *******************************************************************************/
   @Test
   void testRecordWithoutScript() throws QException
   {
      insertScriptType();

      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/table/person/2/developer").asString();
      assertEquals(200, response.getStatus(), response.getBody());
      JSONObject associatedScript = JsonUtils.toJSONObject(response.getBody()).getJSONArray("associatedScripts").getJSONObject(0);
      assertEquals("testScriptId", associatedScript.getJSONObject("associatedScript").getString("fieldName"));
      assertEquals("Test", associatedScript.getJSONObject("scriptType").getJSONObject("values").getString("name"));
      assertFalse(associatedScript.has("script"));
      assertFalse(associatedScript.has("scriptRevisions"));
      assertEquals(2, associatedScript.getJSONArray("testInputFields").length());
   }



   /*******************************************************************************
    ** A table without associated scripts gets an empty list.
    *******************************************************************************/
   @Test
   void testTableWithoutAssociatedScripts()
   {
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/table/pet/1/developer").asString();
      assertEquals(200, response.getStatus(), response.getBody());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals(0, jsonObject.getJSONArray("associatedScripts").length());
      assertEquals(1, jsonObject.getJSONObject("record").getJSONObject("values").getInt("id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRecordNotFound()
   {
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/table/person/999999/developer").asString();
      assertEquals(HttpStatus.NOT_FOUND_404, response.getStatus());
      assertEquals("Could not find Person with Id of 999999", JsonUtils.toJSONObject(response.getBody()).getString("error"));
   }



   /*******************************************************************************
    ** Without READ permission on the table (or for a table that does not exist),
    ** the record and its scripts are refused.
    *******************************************************************************/
   @Test
   void testPermissionDenied()
   {
      serverQInstance.getTable(TestUtils.TABLE_NAME_PERSON).setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));

      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/table/person/1/developer").asString();
      assertEquals(HttpStatus.FORBIDDEN_403, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals(Set.of("error"), jsonObject.keySet());
      assertThat(jsonObject.getString("error")).contains("Permission denied");

      ///////////////////////////////////////////////
      // with read permission, the record is shown //
      ///////////////////////////////////////////////
      serverQInstance.getAuthentication().setCustomizer(new QCodeReference(PersonReadPermission.class));
      assertEquals(200, Unirest.get(getBaseUrlAndPath() + "/table/person/1/developer").asString().getStatus());

      assertEquals(HttpStatus.FORBIDDEN_403, Unirest.get(getBaseUrlAndPath() + "/table/notATable/1/developer").asString().getStatus());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void insertScriptType() throws QException
   {
      QContext.init(serverQInstance, new QSystemUserSession());
      new InsertAction().execute(new InsertInput("scriptType").withRecords(List.of(new QRecord().withValue("id", 1).withValue("name", "Test"))));
      QContext.clear();
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
