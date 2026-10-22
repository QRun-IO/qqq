/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.api.utils;


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.api.BaseTest;
import com.kingsrook.qqq.api.TestUtils;
import com.kingsrook.qqq.api.javalin.QBadRequestException;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.SleepUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import org.junit.jupiter.api.Test;
import static com.kingsrook.qqq.api.TestUtils.insertSimpsons;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;


/*******************************************************************************
 ** Unit test for com.kingsrook.qqq.api.utils.ApiScriptUtils
 *******************************************************************************/
class ApiScriptUtilsTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSetApiNameAndApiVersion()
   {
      ApiScriptUtils apiScriptUtils = newDefaultApiScriptUtils();

      assertThatThrownBy(() -> apiScriptUtils.setApiName("not an api"))
         .isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("not a valid API name");

      assertThatThrownBy(() -> apiScriptUtils.setApiVersion("not a version"))
         .isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("not a supported version");

      assertThatThrownBy(() -> new ApiScriptUtils("not an api", TestUtils.CURRENT_API_VERSION))
         .isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("not a valid API name");

      assertThatThrownBy(() -> new ApiScriptUtils(TestUtils.ALTERNATIVE_API_NAME, "not a version"))
         .isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("not a supported version");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGet() throws QException
   {
      ApiScriptUtils apiScriptUtils = newDefaultApiScriptUtils();

      assertThatThrownBy(() -> apiScriptUtils.get(TestUtils.TABLE_NAME_PERSON, 1))
         .isInstanceOf(QNotFoundException.class);

      insertSimpsons();

      Map<String, Serializable> result = apiScriptUtils.get(TestUtils.TABLE_NAME_PERSON, 1);
      assertEquals("Homer", result.get("firstName"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQuery() throws QException
   {
      ApiScriptUtils apiScriptUtils = newDefaultApiScriptUtils();

      assertThatThrownBy(() -> apiScriptUtils.query(TestUtils.TABLE_NAME_PERSON, "foo=bar"))
         .isInstanceOf(QBadRequestException.class)
         .hasMessageContaining("Unrecognized filter criteria field: foo");

      insertSimpsons();

      Map<String, Serializable> result = apiScriptUtils.query(TestUtils.TABLE_NAME_PERSON, "id=2");
      assertEquals(1, result.get("count"));
      assertEquals(1, ((List<?>) result.get("records")).size());
      assertEquals("Marge", ((Map<?, ?>) ((List<?>) result.get("records")).get(0)).get("firstName"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInsert() throws QException
   {
      ApiScriptUtils apiScriptUtils = newDefaultApiScriptUtils();
      Map<String, Serializable> result = apiScriptUtils.insert(TestUtils.TABLE_NAME_PERSON, """
         { "firstName": "Mr.", "lastName": "Burns" }
         """);
      assertEquals(1, result.get("id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBulkInsert() throws QException
   {
      ApiScriptUtils apiScriptUtils = newDefaultApiScriptUtils();
      List<Map<String, Serializable>> result = apiScriptUtils.bulkInsert(TestUtils.TABLE_NAME_PERSON, """
         [
            { "firstName": "Mr.", "lastName": "Burns" },
            { "firstName": "Waylon", "lastName": "Smithers" }
         ]
         """);
      assertEquals(2, result.size());
      assertEquals(1, result.get(0).get("id"));
      assertEquals(2, result.get(1).get("id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUpdate() throws QException
   {
      ApiScriptUtils apiScriptUtils = newDefaultApiScriptUtils();
      String updateJSON = """
         { "firstName": "Homer J." }
         """;

      assertThatThrownBy(() -> apiScriptUtils.update(TestUtils.TABLE_NAME_PERSON, 1, updateJSON))
         .isInstanceOf(QNotFoundException.class);

      insertSimpsons();

      apiScriptUtils.update(TestUtils.TABLE_NAME_PERSON, 1, updateJSON);

      GetInput getInput = new GetInput();
      getInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      getInput.setPrimaryKey(1);
      GetOutput getOutput = new GetAction().execute(getInput);
      assertEquals("Homer J.", getOutput.getRecord().getValueString("firstName"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBulkUpdate() throws QException
   {
      ApiScriptUtils apiScriptUtils = newDefaultApiScriptUtils();

      insertSimpsons();

      String updateJSON = """
         [
            { "id": 1, "firstName": "Homer J." },
            { "id": 6, "firstName": "C.M." }
         ]
         """;

      List<Map<String, Serializable>> result = apiScriptUtils.bulkUpdate(TestUtils.TABLE_NAME_PERSON, updateJSON);

      assertEquals(2, result.size());
      assertEquals(1, result.get(0).get("id"));
      assertEquals(6, result.get(1).get("id"));
      assertEquals(404, result.get(1).get("statusCode"));
      assertNotNull(result.get(1).get("error"));

      GetInput getInput = new GetInput();
      getInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      getInput.setPrimaryKey(1);
      GetOutput getOutput = new GetAction().execute(getInput);
      assertEquals("Homer J.", getOutput.getRecord().getValueString("firstName"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDelete() throws QException
   {
      ApiScriptUtils apiScriptUtils = newDefaultApiScriptUtils();
      assertThatThrownBy(() -> apiScriptUtils.delete(TestUtils.TABLE_NAME_PERSON, 1))
         .isInstanceOf(QNotFoundException.class);

      insertSimpsons();

      apiScriptUtils.delete(TestUtils.TABLE_NAME_PERSON, 1);

      GetInput getInput = new GetInput();
      getInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      getInput.setPrimaryKey(1);
      GetOutput getOutput = new GetAction().execute(getInput);
      assertNull(getOutput.getRecord());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBulkDelete() throws QException
   {
      ApiScriptUtils apiScriptUtils = newDefaultApiScriptUtils();

      insertSimpsons();

      List<Map<String, Serializable>> result = apiScriptUtils.bulkDelete(TestUtils.TABLE_NAME_PERSON, "[1,6]");

      assertEquals(2, result.size());
      assertEquals(1, ValueUtils.getValueAsInteger(result.get(0).get("id")));
      assertEquals(6, ValueUtils.getValueAsInteger(result.get(1).get("id")));
      assertEquals(404, result.get(1).get("statusCode"));
      assertNotNull(result.get(1).get("error"));

      GetInput getInput = new GetInput();
      getInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      getInput.setPrimaryKey(1);
      GetOutput getOutput = new GetAction().execute(getInput);
      assertNull(getOutput.getRecord());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetProcessForObject() throws QException
   {
      ApiScriptUtils apiScriptUtils = newDefaultApiScriptUtils();

      assertThatThrownBy(() -> apiScriptUtils.runProcess(TestUtils.PROCESS_NAME_GET_PERSON_INFO))
         .isInstanceOf(QBadRequestException.class)
         .hasMessageContaining("Request failed with 4 reasons: Missing value for required input field");

      Object result = apiScriptUtils.runProcess(TestUtils.PROCESS_NAME_GET_PERSON_INFO, """
         {"age": 43, "partnerPersonId": 1, "heightInches": 72, "weightPounds": 220, "homeTown": "Chester"}
         """);

      assertThat(result).isInstanceOf(Map.class);
      Map<?, ?> resultMap = (Map<?, ?>) result;
      assertEquals(15695, resultMap.get("daysOld"));
      assertEquals("Guy from Chester", resultMap.get("nickname"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPostProcessForProcessSummaryList() throws QException
   {
      insertSimpsons();

      ApiScriptUtils apiScriptUtils = newDefaultApiScriptUtils();

      assertThatThrownBy(() -> apiScriptUtils.runProcess(TestUtils.PROCESS_NAME_TRANSFORM_PEOPLE, null))
         .isInstanceOf(QBadRequestException.class)
         .hasMessageContaining("Records to run through this process were not specified");

      Serializable emptyResult = apiScriptUtils.runProcess(TestUtils.PROCESS_NAME_TRANSFORM_PEOPLE, JsonUtils.toJson(Map.of("id", 999)));
      assertThat(emptyResult).isInstanceOf(List.class);
      assertEquals(0, ((List<?>) emptyResult).size());

      Serializable result = apiScriptUtils.runProcess(TestUtils.PROCESS_NAME_TRANSFORM_PEOPLE, JsonUtils.toJson(Map.of("id", "1,2,3")));
      assertThat(result).isInstanceOf(List.class);
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> resultList = (List<Map<String, Object>>) result;
      assertEquals(3, resultList.size());

      assertThat(resultList.stream().filter(m -> m.get("id").equals(2)).findFirst()).isPresent().get().hasFieldOrPropertyWithValue("statusCode", 200);
      assertThat(resultList.stream().filter(m -> m.get("id").equals(3)).findFirst()).isPresent().get().hasFieldOrPropertyWithValue("statusCode", 500);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAsyncProcessAndGetStatus() throws QException
   {
      insertSimpsons();

      ApiScriptUtils apiScriptUtils = newDefaultApiScriptUtils();

      Serializable asyncResult = apiScriptUtils.runProcess(TestUtils.PROCESS_NAME_TRANSFORM_PEOPLE, JsonUtils.toJson(Map.of("id", "1,2,3", "async", true)));
      assertThat(asyncResult).isInstanceOf(Map.class);
      @SuppressWarnings("unchecked")
      String jobId = ValueUtils.getValueAsString(((Map<String, ?>) asyncResult).get("jobId"));
      assertNotNull(jobId);

      //////////////////////////////////////////////////////////////////////////////////
      // check every 100 ms or so to see if the process is done - but after 10 loops, //
      //////////////////////////////////////////////////////////////////////////////////
      for(int i = 0; i < 10; i++)
      {
         Serializable result = apiScriptUtils.getProcessStatus(TestUtils.PROCESS_NAME_TRANSFORM_PEOPLE, jobId);

         if(result instanceof Map map && map.containsKey("jobId"))
         {
            System.out.println("Process is still running - sleep and look again...");
            SleepUtils.sleep(100, TimeUnit.MILLISECONDS);
            continue;
         }

         assertThat(result).isInstanceOf(List.class);
         @SuppressWarnings("unchecked")
         List<Map<String, Object>> resultList = (List<Map<String, Object>>) result;
         assertEquals(3, resultList.size());

         assertThat(resultList.stream().filter(m -> m.get("id").equals(2)).findFirst()).isPresent().get().hasFieldOrPropertyWithValue("statusCode", 200);
         assertThat(resultList.stream().filter(m -> m.get("id").equals(3)).findFirst()).isPresent().get().hasFieldOrPropertyWithValue("statusCode", 500);

         return;
      }

      fail("Process didn't complete after 10 loops, ~1 second.");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static ApiScriptUtils newDefaultApiScriptUtils()
   {
      return (new ApiScriptUtils(TestUtils.API_NAME, TestUtils.CURRENT_API_VERSION));
   }

}