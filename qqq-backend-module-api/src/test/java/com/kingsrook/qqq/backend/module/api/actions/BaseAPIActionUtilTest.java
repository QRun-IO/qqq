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

package com.kingsrook.qqq.backend.module.api.actions;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.model.metadata.variants.BackendVariantsConfig;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.SleepUtils;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeConsumer;
import com.kingsrook.qqq.backend.module.api.BaseTest;
import com.kingsrook.qqq.backend.module.api.TestUtils;
import com.kingsrook.qqq.backend.module.api.exceptions.RateLimitException;
import com.kingsrook.qqq.backend.module.api.mocks.MockApiActionUtils;
import com.kingsrook.qqq.backend.module.api.mocks.MockApiUtilsHelper;
import com.kingsrook.qqq.backend.module.api.model.AuthorizationType;
import com.kingsrook.qqq.backend.module.api.model.OutboundAPILog;
import com.kingsrook.qqq.backend.module.api.model.OutboundAPILogMetaDataProvider;
import com.kingsrook.qqq.backend.module.api.model.metadata.APIBackendMetaData;
import com.kingsrook.qqq.backend.module.api.model.metadata.APIBackendVariantSetting;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/*******************************************************************************
 ** Unit test for com.kingsrook.qqq.backend.module.api.actions.BaseAPIActionUtil
 *******************************************************************************/
class BaseAPIActionUtilTest extends BaseTest
{
   private static final QLogger LOG = QLogger.getLogger(BaseAPIActionUtilTest.class);

   private static MockApiUtilsHelper mockApiUtilsHelper = new MockApiUtilsHelper();



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws QException
   {
      mockApiUtilsHelper = new MockApiUtilsHelper();
      mockApiUtilsHelper.setUseMock(true);
      MockApiActionUtils.mockApiUtilsHelper = mockApiUtilsHelper;

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName("variant");
      QueryOutput        output = new QueryAction().execute(queryInput);
      List<Serializable> ids    = output.getRecords().stream().map(r -> r.getValue("id")).toList();

      DeleteInput deleteInput = new DeleteInput();
      deleteInput.setTableName("variant");
      deleteInput.setPrimaryKeys(ids);
      new DeleteAction().execute(deleteInput);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCount() throws QException
   {
      mockApiUtilsHelper.enqueueMockResponse("""
         [
            {"id": 1, "name": "Homer"},
            {"id": 2, "name": "Marge"},
            {"id": 3, "name": "Bart"},
            {"id": 4, "name": "Lisa"},
            {"id": 5, "name": "Maggie"}
         ]
         """);

      CountInput countInput = new CountInput();
      countInput.setTableName(TestUtils.MOCK_TABLE_NAME);
      CountOutput countOutput = new CountAction().execute(countInput);
      assertEquals(5, countOutput.getCount());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCountError() throws QException
   {
      ////////////////////////////////////////
      // avoid the fully mocked makeRequest //
      ////////////////////////////////////////
      mockApiUtilsHelper.setUseMock(false);

      //////////////////////////
      // set to retry 3 times //
      //////////////////////////
      for(int i = 0; i < 4; i++)
      {
         mockApiUtilsHelper.enqueueMockResponse(new QHttpResponse().withStatusCode(500).withContent("""
            {"error": "Server error"}
            """));
      }

      CountInput countInput = new CountInput();
      countInput.setTableName(TestUtils.MOCK_TABLE_NAME);
      assertThatThrownBy(() -> new CountAction().execute(countInput)).hasRootCauseInstanceOf(Exception.class);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGet() throws QException
   {
      mockApiUtilsHelper.enqueueMockResponse("""
         {"id": 3, "name": "Bart"},
         """);

      GetInput getInput = new GetInput();
      getInput.setTableName(TestUtils.MOCK_TABLE_NAME);
      getInput.setPrimaryKey(3);
      GetOutput getOutput = new GetAction().execute(getInput);
      assertEquals(3, getOutput.getRecord().getValueInteger("id"));
      assertEquals("Bart", getOutput.getRecord().getValueString("name"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetByKey() throws QException
   {
      QContext.getQInstance().getTable(TestUtils.MOCK_TABLE_NAME).withUniqueKey(new UniqueKey("id"));

      mockApiUtilsHelper.enqueueMockResponse("""
         {"id": 3, "name": "Bart"},
         """);

      GetInput getInput = new GetInput();
      getInput.setTableName(TestUtils.MOCK_TABLE_NAME);
      getInput.setUniqueKey(Map.of("id", 3));
      GetOutput getOutput = new GetAction().execute(getInput);
      assertEquals(3, getOutput.getRecord().getValueInteger("id"));
      assertEquals("Bart", getOutput.getRecord().getValueString("name"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQuery() throws QException
   {
      mockApiUtilsHelper.enqueueMockResponse("""
         [
            {"id": 1, "name": "Homer"},
            {"id": 2, "name": "Marge"},
            {"id": 3, "name": "Bart"},
            {"id": 4, "name": "Lisa"},
            {"id": 5, "name": "Maggie"}
         ]
         """);

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.MOCK_TABLE_NAME);
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertEquals(5, queryOutput.getRecords().size());
      assertEquals(1, queryOutput.getRecords().get(0).getValueInteger("id"));
      assertEquals("Homer", queryOutput.getRecords().get(0).getValueString("name"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQueryObjectWrappingList() throws QException
   {
      mockApiUtilsHelper.enqueueMockResponse("""
         {"mocks": [
            {"id": 1, "name": "Homer"},
            {"id": 2, "name": "Marge"},
            {"id": 3, "name": "Bart"},
            {"id": 4, "name": "Lisa"},
            {"id": 5, "name": "Maggie"}
         ]}
         """);

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.MOCK_TABLE_NAME);
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertEquals(5, queryOutput.getRecords().size());
      assertEquals(1, queryOutput.getRecords().get(0).getValueInteger("id"));
      assertEquals("Homer", queryOutput.getRecords().get(0).getValueString("name"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQueryObjectWrappingSingleObject() throws QException
   {
      mockApiUtilsHelper.enqueueMockResponse("""
         {"mocks": 
            {"id": 1, "name": "Homer"}
         }
         """);

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.MOCK_TABLE_NAME);
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertEquals(1, queryOutput.getRecords().size());
      assertEquals(1, queryOutput.getRecords().get(0).getValueInteger("id"));
      assertEquals("Homer", queryOutput.getRecords().get(0).getValueString("name"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQueryPaginate() throws QException
   {
      String oneObject = """
         {"id": 1, "name": "Homer"}
         """;
      StringBuilder response = new StringBuilder("[");
      for(int i = 0; i < 19; i++)
      {
         response.append(oneObject).append(",");
      }
      response.append(oneObject);
      response.append("]");
      mockApiUtilsHelper.enqueueMockResponse(response.toString());
      mockApiUtilsHelper.enqueueMockResponse(response.toString());
      mockApiUtilsHelper.enqueueMockResponse(response.toString());
      mockApiUtilsHelper.enqueueMockResponse("[]");

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.MOCK_TABLE_NAME);
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertEquals(60, queryOutput.getRecords().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQueryError() throws QException
   {
      ////////////////////////////////////////
      // avoid the fully mocked makeRequest //
      ////////////////////////////////////////
      mockApiUtilsHelper.setUseMock(false);

      //////////////////////////
      // set to retry 3 times //
      //////////////////////////
      for(int i = 0; i < 4; i++)
      {
         mockApiUtilsHelper.enqueueMockResponse(new QHttpResponse().withStatusCode(500).withContent("""
            {"error": "Server error"}
            """));
      }

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.MOCK_TABLE_NAME);
      assertThatThrownBy(() -> new QueryAction().execute(queryInput)).hasRootCauseInstanceOf(Exception.class);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInsert() throws QException
   {
      mockApiUtilsHelper.enqueueMockResponse("""
         {"id": 6}
         """);
      mockApiUtilsHelper.withMockRequestAsserter(httpRequestBase ->
      {
         HttpEntity entity      = ((HttpEntityEnclosingRequestBase) httpRequestBase).getEntity();
         byte[]     bytes       = entity.getContent().readAllBytes();
         String     requestBody = new String(bytes, StandardCharsets.UTF_8);

         ///////////////////////////////////////
         // default ISO-8559-1: ... a0 ...    //
         // updated UTF-8:      ... c2 a0 ... //
         ///////////////////////////////////////
         byte previousByte = 0;
         for(byte b : bytes)
         {
            if(b == (byte) 0xa0 && previousByte != (byte) 0xc2)
            {
               fail("Found byte 0xa0 (without being prefixed by 0xc2) - so this is invalid UTF-8!");
            }
            previousByte = b;
         }

         assertThat(requestBody).contains("van Houten");
      });

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.MOCK_TABLE_NAME);
      insertInput.setRecords(List.of(new QRecord().withValue("name", "Milhouse van Houten")));
      InsertOutput insertOutput = new InsertAction().execute(insertInput);
      assertEquals(6, insertOutput.getRecords().get(0).getValueInteger("id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInsertEmptyInputList() throws QException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.MOCK_TABLE_NAME);
      insertInput.setRecords(List.of());
      InsertOutput insertOutput = new InsertAction().execute(insertInput);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInsertError() throws QException
   {
      ////////////////////////////////////////
      // avoid the fully mocked makeRequest //
      ////////////////////////////////////////
      mockApiUtilsHelper.setUseMock(false);

      //////////////////////////
      // set to retry 3 times //
      //////////////////////////
      for(int i = 0; i < 4; i++)
      {
         mockApiUtilsHelper.enqueueMockResponse(new QHttpResponse().withStatusCode(500).withContent("""
            {"error": "Server error"}
            """));
      }

      InsertInput insertInput = new InsertInput();
      insertInput.setRecords(List.of(new QRecord().withValue("name", "Milhouse")));
      insertInput.setTableName(TestUtils.MOCK_TABLE_NAME);
      InsertOutput insertOutput = new InsertAction().execute(insertInput);
      assertTrue(CollectionUtils.nullSafeHasContents(insertOutput.getRecords().get(0).getErrors()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUpdate() throws QException
   {
      mockApiUtilsHelper.enqueueMockResponse("");

      mockApiUtilsHelper.setMockRequestAsserter(httpRequestBase ->
      {
         String     requestBody   = MockApiUtilsHelper.readRequestBody(httpRequestBase);
         JSONObject requestObject = new JSONObject(requestBody);

         JSONArray  mocks  = requestObject.getJSONArray("mocks");
         JSONObject record = mocks.getJSONObject(0);

         assertEquals("Bartholomew", record.getString("name"));
         assertEquals(3, record.getInt("id"));
      });

      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName(TestUtils.MOCK_TABLE_NAME);
      updateInput.setRecords(List.of(new QRecord().withValue("id", "3").withValue("name", "Bartholomew")));
      UpdateOutput updateOutput = new UpdateAction().execute(updateInput);

      // not sure what to assert in here...
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDelete() throws QException
   {
      mockApiUtilsHelper.enqueueMockResponse("");
      mockApiUtilsHelper.enqueueMockResponse(new QHttpResponse().withStatusCode(204).withContent(null));

      DeleteInput deleteInput = new DeleteInput();
      deleteInput.setTableName(TestUtils.MOCK_TABLE_NAME);
      deleteInput.setPrimaryKeys(List.of(1));
      DeleteOutput deleteOutput = new DeleteAction().execute(deleteInput);

      // not sure what to assert in here...
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUpdateEmptyInputList() throws QException
   {
      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName(TestUtils.MOCK_TABLE_NAME);
      updateInput.setRecords(List.of());
      UpdateOutput updateOutput = new UpdateAction().execute(updateInput);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUpdateError() throws QException
   {
      ////////////////////////////////////////
      // avoid the fully mocked makeRequest //
      ////////////////////////////////////////
      mockApiUtilsHelper.setUseMock(false);

      for(int i = 0; i < 4; i++)
      {
         mockApiUtilsHelper.enqueueMockResponse(new QHttpResponse().withStatusCode(500).withContent("""
            {"error": "Server error"}
            """));
      }

      UpdateInput updateInput = new UpdateInput();
      updateInput.setRecords(List.of(new QRecord().withValue("name", "Milhouse")));
      updateInput.setTableName(TestUtils.MOCK_TABLE_NAME);

      /////////////////////////////////////////////////////////////////////////////////
      // note - right now this is inconsistent with insertAction (and rdbms update), //
      // where errors are placed in the records, rather than thrown...               //
      /////////////////////////////////////////////////////////////////////////////////
      assertThatThrownBy(() -> new UpdateAction().execute(updateInput)).hasRootCauseInstanceOf(Exception.class);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMakeRequest() throws QException
   {
      ////////////////////////////////////////////////////////////////////////////////////////////
      // this will make it not use the mock makeRequest method,                                 //
      // but instead the mock executeHttpRequest, so we can test code from the base makeRequest //
      ////////////////////////////////////////////////////////////////////////////////////////////
      mockApiUtilsHelper.setUseMock(false);
      mockApiUtilsHelper.enqueueMockResponse("""
         {"id": 3, "name": "Bart"},
         """);

      GetInput getInput = new GetInput();
      getInput.setTableName(TestUtils.MOCK_TABLE_NAME);
      getInput.setPrimaryKey(3);
      GetOutput getOutput = new GetAction().execute(getInput);
      assertEquals(3, getOutput.getRecord().getValueInteger("id"));
      assertEquals("Bart", getOutput.getRecord().getValueString("name"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test429Then200() throws QException
   {
      ////////////////////////////////////////////////////////////////////////////////////////////
      // this will make it not use the mock makeRequest method,                                 //
      // but instead the mock executeHttpRequest, so we can test code from the base makeRequest //
      // specifically, that we can get one 429, and then eventually a 200                       //
      ////////////////////////////////////////////////////////////////////////////////////////////
      mockApiUtilsHelper.setUseMock(false);
      mockApiUtilsHelper.enqueueMockResponse(new QHttpResponse().withStatusCode(429).withContent("Try again"));
      mockApiUtilsHelper.enqueueMockResponse("""
         {"id": 3, "name": "Bart"},
         """);

      GetInput getInput = new GetInput();
      getInput.setTableName(TestUtils.MOCK_TABLE_NAME);
      getInput.setPrimaryKey(3);
      GetOutput getOutput = new GetAction().execute(getInput);
      assertEquals(3, getOutput.getRecord().getValueInteger("id"));
      assertEquals("Bart", getOutput.getRecord().getValueString("name"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTooMany429() throws QException
   {
      ////////////////////////////////////////////////////////////////////////////////////////////
      // this will make it not use the mock makeRequest method,                                 //
      // but instead the mock executeHttpRequest, so we can test code from the base makeRequest //
      // specifically, that after too many 429's we get an error                                //
      ////////////////////////////////////////////////////////////////////////////////////////////
      mockApiUtilsHelper.setUseMock(false);
      mockApiUtilsHelper.enqueueMockResponse(new QHttpResponse().withStatusCode(429).withContent("Try again"));
      mockApiUtilsHelper.enqueueMockResponse(new QHttpResponse().withStatusCode(429).withContent("Try again"));
      mockApiUtilsHelper.enqueueMockResponse(new QHttpResponse().withStatusCode(429).withContent("Try again"));
      mockApiUtilsHelper.enqueueMockResponse(new QHttpResponse().withStatusCode(429).withContent("Try again"));

      GetInput getInput = new GetInput();
      getInput.setTableName(TestUtils.MOCK_TABLE_NAME);
      getInput.setPrimaryKey(3);

      assertThatThrownBy(() -> new GetAction().execute(getInput)).hasRootCauseInstanceOf(RateLimitException.class);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testApiLogs() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      OutboundAPILogMetaDataProvider.defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);

      mockApiUtilsHelper.setUseMock(false);
      mockApiUtilsHelper.enqueueMockResponse("""
         {"id": 6}
         """);

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.MOCK_TABLE_NAME);
      insertInput.setRecords(List.of(new QRecord().withValue("name", "Milhouse")));
      InsertOutput insertOutput = new InsertAction().execute(insertInput);
      assertEquals(6, insertOutput.getRecords().get(0).getValueInteger("id"));

      //////////////////////////////////////////////////////////////////////////////////////////
      // the outbound api log is inserted async, so... do or do not, and sleep some if needed //
      //////////////////////////////////////////////////////////////////////////////////////////
      QueryOutput apiLogRecords = null;
      int         tries         = 0;
      do
      {
         SleepUtils.sleep(10, TimeUnit.MILLISECONDS);
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(OutboundAPILog.TABLE_NAME);
         apiLogRecords = new QueryAction().execute(queryInput);
      }
      while(apiLogRecords.getRecords().isEmpty() && tries++ < 10);

      assertEquals(1, apiLogRecords.getRecords().size());
      assertEquals("POST", apiLogRecords.getRecords().get(0).getValueString("method"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBasicAuthApiKey() throws QException
   {
      APIBackendMetaData backend = (APIBackendMetaData) QContext.getQInstance().getBackend(TestUtils.MOCK_BACKEND_NAME);
      backend.setAuthorizationType(AuthorizationType.BASIC_AUTH_API_KEY);
      backend.setApiKey("9876-WXYZ");

      ////////////////////////////////////////////////////////////////////////////////////////////
      // this will make it not use the mock makeRequest method,                                 //
      // but instead the mock executeHttpRequest, so we can test code from the base makeRequest //
      ////////////////////////////////////////////////////////////////////////////////////////////
      mockApiUtilsHelper.setUseMock(false);
      mockApiUtilsHelper.enqueueMockResponse("""
         {"id": 3, "name": "Bart"},
         """);

      mockApiUtilsHelper.setMockRequestAsserter(request ->
      {
         Header authHeader = request.getFirstHeader("Authorization");
         assertTrue(authHeader.getValue().startsWith("Basic "));
         String apiKey = new String(Base64.getDecoder().decode(authHeader.getValue().replace("Basic ", "")), StandardCharsets.UTF_8);
         assertEquals("9876-WXYZ", apiKey);
      });

      runSimpleGetAction();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBackendWithVariantsApiKey() throws QException
   {
      APIBackendMetaData backend = (APIBackendMetaData) QContext.getQInstance().getBackend(TestUtils.MOCK_BACKEND_NAME);
      backend.setAuthorizationType(AuthorizationType.API_KEY_HEADER);
      backend.setUsesVariants(true);
      backend.setBackendVariantsConfig(new BackendVariantsConfig()
         .withOptionsTableName("variant")
         .withVariantTypeKey("API_KEY_TYPE")
         .withBackendSettingSourceFieldName(APIBackendVariantSetting.API_KEY, "apiKey")
      );

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName("variant");
      insertInput.setRecords(List.of(new QRecord()
         .withValue("type", "API_KEY_TYPE")
         .withValue("apiKey", "abcdefg1234567")));
      InsertOutput insertOutput = new InsertAction().execute(insertInput);

      QContext.getQSession().setBackendVariants(Map.of("API_KEY_TYPE", insertOutput.getRecords().get(0).getValue("id")));
      HttpGet           httpGet = new HttpGet();
      BaseAPIActionUtil util    = new BaseAPIActionUtil();
      util.setBackendMetaData(backend);
      util.setupAuthorizationInRequest(httpGet);
      Header authHeader = httpGet.getFirstHeader("API-Key");
      assertTrue(authHeader.getValue().startsWith("abcde"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBackendWithVariantsUsernamePassword() throws QException
   {
      APIBackendMetaData backend = (APIBackendMetaData) QContext.getQInstance().getBackend(TestUtils.MOCK_BACKEND_NAME);
      backend.setAuthorizationType(AuthorizationType.BASIC_AUTH_USERNAME_PASSWORD);
      backend.setUsesVariants(true);
      backend.setBackendVariantsConfig(new BackendVariantsConfig()
         .withOptionsTableName("variant")
         .withVariantTypeKey("USER_PASS")
         .withBackendSettingSourceFieldName(APIBackendVariantSetting.USERNAME, "username")
         .withBackendSettingSourceFieldName(APIBackendVariantSetting.PASSWORD, "password")
      );

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName("variant");
      insertInput.setRecords(List.of(new QRecord()
         .withValue("type", "USER_PASS")
         .withValue("username", "user")
         .withValue("password", "pass")));
      InsertOutput insertOutput = new InsertAction().execute(insertInput);

      QContext.getQSession().setBackendVariants(Map.of("USER_PASS", insertOutput.getRecords().get(0).getValue("id")));
      HttpGet           httpGet = new HttpGet();
      BaseAPIActionUtil util    = new BaseAPIActionUtil();
      util.setBackendMetaData(backend);
      util.setupAuthorizationInRequest(httpGet);
      Header authHeader = httpGet.getFirstHeader("Authorization");
      assertEquals(authHeader.getValue(), util.getBasicAuthenticationHeader("user", "pass"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBasicAuthUsernamePassword() throws QException
   {
      APIBackendMetaData backend = (APIBackendMetaData) QContext.getQInstance().getBackend(TestUtils.MOCK_BACKEND_NAME);
      backend.setAuthorizationType(AuthorizationType.BASIC_AUTH_USERNAME_PASSWORD);
      backend.setUsername("god");
      backend.setPassword("5fingers");

      ////////////////////////////////////////////////////////////////////////////////////////////
      // this will make it not use the mock makeRequest method,                                 //
      // but instead the mock executeHttpRequest, so we can test code from the base makeRequest //
      ////////////////////////////////////////////////////////////////////////////////////////////
      mockApiUtilsHelper.setUseMock(false);
      mockApiUtilsHelper.enqueueMockResponse("""
         {"id": 3, "name": "Bart"},
         """);

      mockApiUtilsHelper.setMockRequestAsserter(request ->
      {
         Header authHeader = request.getFirstHeader("Authorization");
         assertTrue(authHeader.getValue().startsWith("Basic "));
         String usernamePassword = new String(Base64.getDecoder().decode(authHeader.getValue().replace("Basic ", "")), StandardCharsets.UTF_8);
         assertEquals("god:5fingers", usernamePassword);
      });

      runSimpleGetAction();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOAuth2ValidToken() throws QException
   {
      APIBackendMetaData backend = (APIBackendMetaData) QContext.getQInstance().getBackend(TestUtils.MOCK_BACKEND_NAME);
      backend.setAuthorizationType(AuthorizationType.OAUTH2);
      backend.withCustomValue("accessToken", "validToken");

      ////////////////////////////////////////////////////////////////////////////////////////////
      // this will make it not use the mock makeRequest method,                                 //
      // but instead the mock executeHttpRequest, so we can test code from the base makeRequest //
      ////////////////////////////////////////////////////////////////////////////////////////////
      mockApiUtilsHelper.setUseMock(false);
      mockApiUtilsHelper.enqueueMockResponse("""
         {"id": 3, "name": "Bart"},
         """);

      mockApiUtilsHelper.setMockRequestAsserter(request ->
      {
         Header authHeader = request.getFirstHeader("Authorization");
         assertTrue(authHeader.getValue().startsWith("Bearer "));
         String token = authHeader.getValue().replace("Bearer ", "");
         assertEquals("validToken", token);
      });

      runSimpleGetAction();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOAuth2NullToken() throws QException
   {
      APIBackendMetaData backend = (APIBackendMetaData) QContext.getQInstance().getBackend(TestUtils.MOCK_BACKEND_NAME);
      backend.setAuthorizationType(AuthorizationType.OAUTH2);

      ////////////////////////////////////////////////////////////////////////////////////////////
      // this will make it not use the mock makeRequest method,                                 //
      // but instead the mock executeHttpRequest, so we can test code from the base makeRequest //
      ////////////////////////////////////////////////////////////////////////////////////////////
      mockApiUtilsHelper.setUseMock(false);
      mockApiUtilsHelper.enqueueMockResponse("""
         {"access_token": "myNewToken"}
         """);
      mockApiUtilsHelper.enqueueMockResponse("""
         {"id": 3, "name": "Bart"},
         """);

      GetOutput getOutput = runSimpleGetAction();
      assertEquals(3, getOutput.getRecord().getValueInteger("id"));
      assertEquals("Bart", getOutput.getRecord().getValueString("name"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTimeouts() throws QException
   {
      ShortTimeoutActionUtil shortTimeoutActionUtil = new ShortTimeoutActionUtil();
      shortTimeoutActionUtil.setBackendMetaData((APIBackendMetaData) QContext.getQInstance().getBackend(TestUtils.MOCK_BACKEND_NAME));

      /////////////////////////////////////////////////////////////
      // make sure we work correctly with a large enough timeout //
      /////////////////////////////////////////////////////////////
      {
         startSimpleHttpServer(8888);
         HttpGet request = new HttpGet("http://localhost:8888");
         shortTimeoutActionUtil.setTimeoutMillis(3000);

         shortTimeoutActionUtil.makeRequest(QContext.getQInstance().getTable(TestUtils.MOCK_TABLE_NAME), request);
      }

      ////////////////////////////////////////////////
      // make sure we fail with a too-small timeout //
      ////////////////////////////////////////////////
      {
         startSimpleHttpServer(8889);
         HttpGet request = new HttpGet("http://localhost:8889");
         shortTimeoutActionUtil.setTimeoutMillis(1);

         assertThatThrownBy(() -> shortTimeoutActionUtil.makeRequest(QContext.getQInstance().getTable(TestUtils.MOCK_TABLE_NAME), request))
            .hasRootCauseInstanceOf(SocketTimeoutException.class)
            .rootCause().hasMessageContaining("timed out");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAuthorizationApiKeyQueryParam() throws QException
   {
      APIBackendMetaData backend = (APIBackendMetaData) QContext.getQInstance().getBackend(TestUtils.MOCK_BACKEND_NAME);
      backend.setAuthorizationType(AuthorizationType.API_KEY_QUERY_PARAM);
      backend.setApiKeyQueryParamName("apikey");
      backend.setApiKey("9876-WXYZ");

      ////////////////////////////////////////////////////////////////////////////////////////////
      // this will make it not use the mock makeRequest method,                                 //
      // but instead the mock executeHttpRequest, so we can test code from the base makeRequest //
      ////////////////////////////////////////////////////////////////////////////////////////////
      mockApiUtilsHelper.setUseMock(false);

      //////////////////////////////////////////////////////////////////////////////
      // we'll want to assert that the URL has the api query string - and just    //
      // one copy of it (as we once had a bug where it got duplicated upon retry) //
      //////////////////////////////////////////////////////////////////////////////
      UnsafeConsumer<HttpRequestBase, Exception> asserter = request -> assertThat(request.getURI().toString())
         .contains("?apikey=9876-WXYZ")
         .doesNotContain("?apikey=9876-WXYZ&apikey=9876-WXYZ");

      ////////////////////////////////////////
      // queue up a 429, so we'll try-again //
      ////////////////////////////////////////
      mockApiUtilsHelper.setMockRequestAsserter(asserter);
      mockApiUtilsHelper.enqueueMockResponse(new QHttpResponse().withStatusCode(429).withContent(""));

      //////////////////////
      // queue a response //
      //////////////////////
      mockApiUtilsHelper.setMockRequestAsserter(asserter);
      mockApiUtilsHelper.enqueueMockResponse("""
         {"id": 3, "name": "Bart"},
         """);

      GetOutput getOutput = runSimpleGetAction();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGenerateOutboundApiLogRecord() throws QException
   {
      APIBackendMetaData backend = (APIBackendMetaData) QContext.getQInstance().getBackend(TestUtils.MOCK_BACKEND_NAME);
      backend.setAuthorizationType(AuthorizationType.API_KEY_QUERY_PARAM);
      backend.setApiKeyQueryParamName("apikey");
      backend.setApiKey("9876-WXYZ");

      MockApiActionUtils mockApiActionUtils = new MockApiActionUtils();
      mockApiActionUtils.setBackendMetaData(backend);
      OutboundAPILog outboundAPILog = mockApiActionUtils.generateOutboundApiLogRecord(new HttpGet("...?apikey=9876-WXYZ"), new QHttpResponse());

      assertThat(outboundAPILog.getUrl())
         .doesNotContain("9876-WXYZ")
         .contains("?apikey=*****");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void startSimpleHttpServer(int port)
   {
      Executors.newSingleThreadExecutor().submit(() ->
      {
         LOG.info("Listening on " + port);
         try(ServerSocket serverSocket = new ServerSocket(port))
         {
            Socket         clientSocket = serverSocket.accept();
            PrintWriter    out          = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in           = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String         greeting     = in.readLine();
            LOG.info("Read: " + greeting);
            SleepUtils.sleep(1, TimeUnit.SECONDS);
            out.println("HTTP/1.1 200 OK");
            out.close();
            clientSocket.close();
         }
         catch(Exception e)
         {
            LOG.info("Exception in simple http server", e);
         }
      });

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // give time for the thread w/ the listening socket to start before returning control to the thread that's going to try to connect to it //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      SleepUtils.sleep(100, TimeUnit.MILLISECONDS);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static class ShortTimeoutActionUtil extends BaseAPIActionUtil
   {
      private int timeoutMillis = 1;



      /*******************************************************************************
       ** Setter for timeoutMillis
       **
       *******************************************************************************/
      public void setTimeoutMillis(int timeoutMillis)
      {
         this.timeoutMillis = timeoutMillis;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      protected int getSocketTimeoutMillis()
      {
         return (timeoutMillis);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static GetOutput runSimpleGetAction() throws QException
   {
      GetInput getInput = new GetInput();
      getInput.setTableName(TestUtils.MOCK_TABLE_NAME);
      getInput.setPrimaryKey(3);
      return (new GetAction().execute(getInput));
   }

}
