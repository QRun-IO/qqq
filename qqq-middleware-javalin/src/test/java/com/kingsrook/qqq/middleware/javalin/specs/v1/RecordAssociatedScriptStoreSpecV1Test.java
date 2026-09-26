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
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
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
import io.javalin.http.ContentType;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for RecordAssociatedScriptStoreSpecV1
 *******************************************************************************/
class RecordAssociatedScriptStoreSpecV1Test extends SpecTestBase
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new RecordAssociatedScriptStoreSpecV1();
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
    ** Storing creates the script (referenced from the record) and its first
    ** revision; storing again adds the next revision of the same script.
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      insertScriptType();

      HttpResponse<String> response = store("var j = 0;", "V1 Commit");
      assertEquals(200, response.getStatus(), response.getBody());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals(Set.of("scriptId", "scriptName", "scriptRevisionId", "scriptRevisionSequenceNo"), jsonObject.keySet());
      assertEquals("Darin Kelkhoff - Test", jsonObject.getString("scriptName"));
      assertEquals(1, jsonObject.getInt("scriptRevisionSequenceNo"));
      int scriptId = jsonObject.getInt("scriptId");

      QContext.init(serverQInstance, new QSystemUserSession());
      assertEquals(scriptId, GetAction.execute(TestUtils.TABLE_NAME_PERSON, 1).getValueInteger("testScriptId"));
      List<QRecord> revisions = queryAll("scriptRevision");
      assertEquals(1, revisions.size());
      assertEquals("V1 Commit", revisions.get(0).getValueString("commitMessage"));
      assertEquals(jsonObject.getInt("scriptRevisionId"), revisions.get(0).getValueInteger("id"));
      QContext.clear();

      response = store("var j = 1;", null);
      assertEquals(200, response.getStatus(), response.getBody());
      jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals(scriptId, jsonObject.getInt("scriptId"));
      assertEquals(2, jsonObject.getInt("scriptRevisionSequenceNo"));
   }



   /*******************************************************************************
    ** Without EDIT permission on the table (even with READ), nothing is stored:
    ** no script or revision is created, and the record's field is unchanged.
    *******************************************************************************/
   @Test
   void testPermissionDenied() throws QException
   {
      insertScriptType();
      serverQInstance.getTable(TestUtils.TABLE_NAME_PERSON).setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));

      HttpResponse<String> response = store("var j = 0;", "No permissions");
      assertEquals(HttpStatus.FORBIDDEN_403, response.getStatus(), response.getBody());

      serverQInstance.getAuthentication().setCustomizer(new QCodeReference(PersonReadPermission.class));
      response = store("var j = 0;", "Read only");
      assertEquals(HttpStatus.FORBIDDEN_403, response.getStatus(), response.getBody());
      assertThat(JsonUtils.toJSONObject(response.getBody()).getString("error")).contains("Permission denied");

      QContext.init(serverQInstance, new QSystemUserSession());
      assertEquals(0, queryAll("script").size());
      assertEquals(0, queryAll("scriptRevision").size());
      assertNull(GetAction.execute(TestUtils.TABLE_NAME_PERSON, 1).getValue("testScriptId"));
      QContext.clear();

      ////////////////////////////////////////////////////
      // with edit permission, the same request stores //
      ////////////////////////////////////////////////////
      serverQInstance.getAuthentication().setCustomizer(new QCodeReference(PersonReadEditPermission.class));
      response = store("var j = 0;", "Read and edit");
      assertEquals(200, response.getStatus(), response.getBody());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<String> store(String contents, String commitMessage)
   {
      JSONObject body = new JSONObject();
      body.put("contents", contents);
      if(commitMessage != null)
      {
         body.put("commitMessage", commitMessage);
      }

      return (Unirest.post(getBaseUrlAndPath() + "/table/person/1/developer/associatedScript/testScriptId")
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(body.toString())
         .asString());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<QRecord> queryAll(String tableName) throws QException
   {
      return (new QueryAction().execute(new QueryInput(tableName)).getRecords());
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



   /*******************************************************************************
    ** Grant read and edit on the person table.
    *******************************************************************************/
   public static class PersonReadEditPermission implements QAuthenticationModuleCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void customizeSession(QInstance instance, QSession session, Map<String, Object> context)
      {
         session.withPermissions("person.read", "person.edit");
      }
   }

}
