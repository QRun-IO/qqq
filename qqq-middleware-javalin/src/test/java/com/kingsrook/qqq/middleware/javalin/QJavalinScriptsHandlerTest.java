/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin;


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Unit test for the javalin scripts handler methods.
 *******************************************************************************/
class QJavalinScriptsHandlerTest extends QJavalinTestBase
{
   /***************************************************************************
    **
    ***************************************************************************/
   @BeforeEach
   public void beforeEach() throws Exception
   {
      QContext.init(TestUtils.defineInstance(), new QSession());
      super.beforeEach();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      QContext.clear();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetRecordDeveloperMode() throws QException
   {
      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName("person");
      updateInput.setRecords(List.of(new QRecord().withValue("id", 1).withValue("testScriptId", 47)));
      new UpdateAction().execute(updateInput);

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName("script");
      insertInput.setRecords(List.of(new QRecord().withValue("id", 47).withValue("currentScriptRevisionId", 100)));
      new InsertAction().execute(insertInput);

      insertInput.setTableName("scriptRevision");
      insertInput.setRecords(List.of(new QRecord().withValue("id", 1000).withValue("scriptId", 47).withValue("content", "var i;")));
      new InsertAction().execute(insertInput);

      HttpResponse<String> response = Unirest.get(BASE_URL + "/data/person/1/developer").asString();
      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      System.out.println(jsonObject.toString(3));
      assertNotNull(jsonObject);
      assertNotNull(jsonObject.getJSONObject("record"));
      assertEquals("Darin", jsonObject.getJSONObject("record").getJSONObject("values").getString("firstName"));
      assertEquals("Darin", jsonObject.getJSONObject("record").getJSONObject("displayValues").getString("firstName"));
      assertNotNull(jsonObject.getJSONArray("associatedScripts"));
      assertNotNull(jsonObject.getJSONArray("associatedScripts").getJSONObject(0));
      assertNotNull(jsonObject.getJSONArray("associatedScripts").getJSONObject(0).getJSONArray("scriptRevisions"));
      assertEquals("var i;", jsonObject.getJSONArray("associatedScripts").getJSONObject(0).getJSONArray("scriptRevisions").getJSONObject(0).getJSONObject("values").getString("content"));
      assertNotNull(jsonObject.getJSONArray("associatedScripts").getJSONObject(0).getJSONObject("script"));
      assertEquals(100, jsonObject.getJSONArray("associatedScripts").getJSONObject(0).getJSONObject("script").getJSONObject("values").getInt("currentScriptRevisionId"));
      assertNotNull(jsonObject.getJSONArray("associatedScripts").getJSONObject(0).getJSONObject("associatedScript"));
      assertEquals("testScriptId", jsonObject.getJSONArray("associatedScripts").getJSONObject(0).getJSONObject("associatedScript").getString("fieldName"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testStoreRecordAssociatedScript() throws QException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName("scriptType");
      insertInput.setRecords(List.of(new QRecord().withValue("id", 1).withValue("name", "Test")));
      new InsertAction().execute(insertInput);

      HttpResponse<String> response = Unirest.post(BASE_URL + "/data/person/1/developer/associatedScript/testScriptId")
         .field("contents", "var j = 0;")
         .field("commitMessage", "Javalin Commit")
         .asString();

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName("scriptRevision");
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("commitMessage", QCriteriaOperator.EQUALS, List.of("Javalin Commit"))));
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertEquals(1, queryOutput.getRecords().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTestAssociatedScript() throws QException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName("scriptType");
      insertInput.setRecords(List.of(new QRecord().withValue("id", 1).withValue("name", "Test")));
      new InsertAction().execute(insertInput);

      HttpResponse<String> response = Unirest.post(BASE_URL + "/data/person/1/developer/associatedScript/testScriptId/test")
         .field("code", """
            // output.setMessage(`${input.getName()} is ${input.getAge()} years old.`);
            output.setMessage("I am " + input.getName());
            return (output);
            """)
         .field("scriptTypeId", "1")
         .field("name", "Tim")
         .asString();

      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals("I am Tim", jsonObject.getJSONObject("outputObject").getString("message"));
      assertNotNull(jsonObject.getJSONObject("scriptLog"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetAssociatedScriptLogs() throws QException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName("scriptLog");
      insertInput.setRecords(List.of(new QRecord().withValue("id", 1).withValue("output", "testOutput").withValue("scriptRevisionId", 100)));
      new InsertAction().execute(insertInput);

      insertInput.setTableName("scriptLogLine");
      insertInput.setRecords(List.of(
         new QRecord().withValue("scriptLogId", 1).withValue("text", "line one"),
         new QRecord().withValue("scriptLogId", 1).withValue("text", "line two")
      ));
      new InsertAction().execute(insertInput);

      HttpResponse<String> response = Unirest.get(BASE_URL + "/data/person/1/developer/associatedScript/testScriptId/100/logs").asString();
      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject.getJSONArray("scriptLogRecords"));
      assertEquals(1, jsonObject.getJSONArray("scriptLogRecords").length());
      assertNotNull(jsonObject.getJSONArray("scriptLogRecords").getJSONObject(0).getJSONObject("values"));
      assertEquals("testOutput", jsonObject.getJSONArray("scriptLogRecords").getJSONObject(0).getJSONObject("values").getString("output"));
      assertNotNull(jsonObject.getJSONArray("scriptLogRecords").getJSONObject(0).getJSONObject("values").getJSONArray("scriptLogLine"));
      assertEquals(2, jsonObject.getJSONArray("scriptLogRecords").getJSONObject(0).getJSONObject("values").getJSONArray("scriptLogLine").length());
      assertEquals("line one", jsonObject.getJSONArray("scriptLogRecords").getJSONObject(0).getJSONObject("values").getJSONArray("scriptLogLine").getJSONObject(0).getJSONObject("values").getString("text"));
      assertEquals("line two", jsonObject.getJSONArray("scriptLogRecords").getJSONObject(0).getJSONObject("values").getJSONArray("scriptLogLine").getJSONObject(1).getJSONObject("values").getString("text"));
   }

}