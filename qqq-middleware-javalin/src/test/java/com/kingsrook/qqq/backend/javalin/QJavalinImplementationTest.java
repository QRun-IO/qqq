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

package com.kingsrook.qqq.backend.javalin;


import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QInstanceValidationException;
import com.kingsrook.qqq.backend.core.logging.QCollectingLogger;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormat;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReferenceLambda;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAdornment;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.mock.MockBackendModule;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.SleepUtils;
import com.kingsrook.qqq.middleware.javalin.misc.DownloadFileSupplementalAction;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.apache.logging.log4j.Level;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for the QJavalinImplementation
 **
 ** based on https://javalin.io/tutorials/testing - starts a javalin instance
 ** and actually makes http requests into it.
 **
 *******************************************************************************/
class QJavalinImplementationTest extends QJavalinTestBase
{


   /*******************************************************************************
    ** test the top-level meta-data endpoint
    **
    *******************************************************************************/
   @Test
   public void test_metaData()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/metaData").asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertTrue(jsonObject.has("tables"));
      JSONObject tables = jsonObject.getJSONObject("tables");
      assertThat(tables.length()).isGreaterThan(1);
      JSONObject personTable = tables.getJSONObject("person");
      assertTrue(personTable.has("name"));
      assertEquals("person", personTable.getString("name"));
      assertTrue(personTable.has("label"));
      assertEquals("Person", personTable.getString("label"));
      assertFalse(personTable.getBoolean("isHidden"));

      JSONObject processes = jsonObject.getJSONObject("processes");
      assertTrue(processes.getJSONObject("simpleSleep").getBoolean("isHidden"));
   }



   /*******************************************************************************
    ** test getting serverInfo
    **
    *******************************************************************************/
   @Test
   public void test_serverInfo()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/serverInfo").asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertTrue(jsonObject.has("startTimeMillis"));
      assertTrue(jsonObject.has("startTimeHuman"));
      assertTrue(jsonObject.has("uptimeHuman"));
      assertTrue(jsonObject.has("uptimeMillis"));
   }



   /*******************************************************************************
    ** test the table-level meta-data endpoint
    **
    *******************************************************************************/
   @Test
   public void test_tableMetaData()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/metaData/table/person").asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals(1, jsonObject.keySet().size(), "Number of top-level keys");
      JSONObject table = jsonObject.getJSONObject("table");
      assertEquals("person", table.getString("name"));
      assertEquals("Person", table.getString("label"));
      assertEquals("id", table.getString("primaryKeyField"));
      JSONObject fields = table.getJSONObject("fields");
      JSONObject field0 = fields.getJSONObject("id");
      assertEquals("id", field0.getString("name"));
      assertEquals("INTEGER", field0.getString("type"));
   }



   /*******************************************************************************
    ** test the table-level meta-data endpoint for a non-real name
    **
    *******************************************************************************/
   @Test
   public void test_tableMetaData_notFound()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/metaData/table/notAnActualTable").asString();

      assertEquals(HttpStatus.NOT_FOUND_404, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals(1, jsonObject.keySet().size(), "Number of top-level keys");
      String error = jsonObject.getString("error");
      assertTrue(error.contains("not found"));
   }



   /*******************************************************************************
    ** test the process-level meta-data endpoint
    **
    *******************************************************************************/
   @Test
   public void test_processMetaData()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/metaData/process/greetInteractive").asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals(1, jsonObject.keySet().size(), "Number of top-level keys");
      JSONObject process = jsonObject.getJSONObject("process");
      assertEquals("greetInteractive", process.getString("name"));
      assertEquals("Greet Interactive", process.getString("label"));
      assertEquals("person", process.getString("tableName"));

      JSONArray  frontendSteps = process.getJSONArray("frontendSteps");
      JSONObject setupStep     = frontendSteps.getJSONObject(0);
      assertEquals("Setup", setupStep.getString("label"));
      JSONArray setupFields = setupStep.getJSONArray("formFields");
      assertEquals(2, setupFields.length());
      assertTrue(setupFields.toList().stream().anyMatch(field -> "greetingPrefix".equals(((Map<?, ?>) field).get("name"))));
   }



   /*******************************************************************************
    ** test the process-level meta-data endpoint for a non-real name
    **
    *******************************************************************************/
   @Test
   public void test_processMetaData_notFound()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/metaData/process/notAnActualProcess").asString();

      assertEquals(HttpStatus.NOT_FOUND_404, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals(1, jsonObject.keySet().size(), "Number of top-level keys");
      String error = jsonObject.getString("error");
      assertTrue(error.contains("not found"));
   }



   /*******************************************************************************
    ** test a table get (single record)
    **
    *******************************************************************************/
   @Test
   public void test_dataGet()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/data/person/1").asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertTrue(jsonObject.has("values"));
      assertEquals("person", jsonObject.getString("tableName"));
      JSONObject values = jsonObject.getJSONObject("values");
      assertTrue(values.has("firstName"));
      assertTrue(values.has("id"));
      assertTrue(values.has("photo"));

      JSONObject displayValues = jsonObject.getJSONObject("displayValues");
      assertEquals("darin-photo.png", displayValues.getString("photo"));

      ////////////////////////////////////////////////////
      // make sure person 2 doesn't have the blob value //
      ////////////////////////////////////////////////////
      response = Unirest.get(BASE_URL + "/data/person/2").asString();
      assertEquals(200, response.getStatus());
      jsonObject = JsonUtils.toJSONObject(response.getBody());
      values = jsonObject.getJSONObject("values");
      assertFalse(values.has("photo"));
   }



   /*******************************************************************************
    ** test downloading a blob file
    **
    *******************************************************************************/
   @Test
   public void test_dataDownloadRecordField()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/data/person/1/photo/darin-photo.png").asString();
      assertEquals(200, response.getStatus());
      assertThat(response.getHeaders().get("content-type").get(0)).contains("image");

      response = Unirest.get(BASE_URL + "/data/person/1/photo/darin-photo.png?download=1").asString();
      assertEquals(200, response.getStatus());
      assertThat(response.getHeaders().get("content-disposition").get(0))
         .contains("attachment")
         .contains("darin-photo.png");

      /////////////////////////
      // bad record id = 404 //
      /////////////////////////
      response = Unirest.get(BASE_URL + "/data/person/-1/photo/darin-photo.png").asString();
      assertEquals(404, response.getStatus());

      //////////////////////////
      // bad field name = 404 //
      //////////////////////////
      response = Unirest.get(BASE_URL + "/data/person/1/notPhoto/darin-photo.png").asString();
      assertEquals(404, response.getStatus());

      /////////////////////////////
      // missing file name = 404 //
      /////////////////////////////
      response = Unirest.get(BASE_URL + "/data/person/1/photo").asString();
      assertEquals(404, response.getStatus());

      //////////////////////////
      // bad table name = 404 //
      //////////////////////////
      response = Unirest.get(BASE_URL + "/data/notPerson/1/photo/darin-photo.png").asString();
      assertEquals(404, response.getStatus());
   }



   /*******************************************************************************
    ** test downloading from a URL field
    **
    *******************************************************************************/
   @Test
   public void test_dataDownloadRecordFieldUrl()
   {
      try
      {
         TestDownloadFileSupplementalAction.callCount = 0;

         Unirest.config().reset();
         Unirest.config().followRedirects(false);

         ////////////////////////////////////////////////////////////////////////////////////////////////////////
         // first request - has no custom code - should just give us back a redirect to the value in the field //
         ////////////////////////////////////////////////////////////////////////////////////////////////////////
         HttpResponse<String> response = Unirest.get(BASE_URL + "/data/person/1/licenseScanPdfUrl/License-1.pdf").asString();
         assertEquals(302, response.getStatus());
         assertThat(response.getHeaders().get("location").get(0)).contains("https://");

         ////////////////////////////////////////////////////
         // set a code-reference on the download adornment //
         ////////////////////////////////////////////////////
         Optional<FieldAdornment> fileDownloadAdornment = QJavalinImplementation.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON)
            .getField("licenseScanPdfUrl")
            .getAdornment(AdornmentType.FILE_DOWNLOAD);
         fileDownloadAdornment.get().withValue(AdornmentType.FileDownloadValues.SUPPLEMENTAL_CODE_REFERENCE, new QCodeReference(TestDownloadFileSupplementalAction.class));

         /////////////////////////////////////////
         // request again - assert the code ran //
         /////////////////////////////////////////
         assertEquals(0, TestDownloadFileSupplementalAction.callCount);
         response = Unirest.get(BASE_URL + "/data/person/1/licenseScanPdfUrl/License-1.pdf").asString();
         assertEquals(302, response.getStatus());
         assertThat(response.getHeaders().get("location").get(0)).contains("https://");
         assertEquals(1, TestDownloadFileSupplementalAction.callCount);

         ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // set adornment to run process (note, leaving the code-ref - this demonstrates that process "trumps" if both exist) //
         ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         AtomicInteger processRunCount = new AtomicInteger(0);
         QJavalinImplementation.getQInstance().addProcess(new QProcessMetaData().withName("testDownloadProcess").withStep(
            new QBackendStepMetaData().withName("execute").withCode(new QCodeReferenceLambda<BackendStep>((input, output) -> processRunCount.incrementAndGet()))
         ));
         fileDownloadAdornment.get().withValue(AdornmentType.FileDownloadValues.SUPPLEMENTAL_PROCESS_NAME, "testDownloadProcess");

         /////////////////////////////////////////
         // request again - assert the code ran //
         /////////////////////////////////////////
         response = Unirest.get(BASE_URL + "/data/person/1/licenseScanPdfUrl/License-1.pdf").asString();
         assertEquals(302, response.getStatus());
         assertThat(response.getHeaders().get("location").get(0)).contains("https://");
         assertEquals(1, TestDownloadFileSupplementalAction.callCount);
         assertEquals(1, processRunCount.get());
      }
      finally
      {
         Unirest.config().reset();
      }
   }



   /*******************************************************************************
    ** test a table get (single record) for an id that isn't found
    **
    *******************************************************************************/
   @Test
   public void test_dataGetNotFound()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/data/person/98765").asString();
      assertEquals(HttpStatus.NOT_FOUND_404, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals(1, jsonObject.keySet().size(), "Number of top-level keys");
      String error = jsonObject.getString("error");
      assertEquals("Could not find Person with Id of 98765", error);
   }



   /*******************************************************************************
    ** test a table get (single record) for an id that isn't the expected type
    **
    *******************************************************************************/
   @Test
   public void test_dataGetWrongIdType()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/data/person/not-an-integer").asString();
      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR_500, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertEquals(1, jsonObject.keySet().size(), "Number of top-level keys");
   }



   /*******************************************************************************
    ** test a table count
    **
    *******************************************************************************/
   @Test
   public void test_dataCount()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/data/person/count").asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertTrue(jsonObject.has("count"));
      int count = jsonObject.getInt("count");
      assertEquals(6, count);
   }



   /*******************************************************************************
    ** test a table count with a filter.
    **
    *******************************************************************************/
   @Test
   public void test_dataCountWithFilter()
   {
      String               filterJson = getFirstNameEqualsFilterJSON("Tim");
      HttpResponse<String> response   = Unirest.get(BASE_URL + "/data/person/count?filter=" + URLEncoder.encode(filterJson, StandardCharsets.UTF_8)).asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());

      assertTrue(jsonObject.has("count"));
      int count = jsonObject.getInt("count");
      assertEquals(1, count);
   }



   /*******************************************************************************
    ** test a table count POST.
    **
    *******************************************************************************/
   @Test
   public void test_dataCountPOST()
   {
      String filterJson = getFirstNameEqualsFilterJSON("Tim");
      HttpResponse<String> response = Unirest.post(BASE_URL + "/data/person/count")
         .field("filter", filterJson)
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());

      assertTrue(jsonObject.has("count"));
      int count = jsonObject.getInt("count");
      assertEquals(1, count);
   }



   /*******************************************************************************
    ** test a table query
    **
    *******************************************************************************/
   @Test
   public void test_dataQuery()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/data/person").asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertTrue(jsonObject.has("records"));
      JSONArray records = jsonObject.getJSONArray("records");
      assertEquals(6, records.length());
      JSONObject record0 = records.getJSONObject(0);
      assertTrue(record0.has("values"));
      assertEquals("person", record0.getString("tableName"));
      JSONObject values0 = record0.getJSONObject("values");
      assertTrue(values0.has("firstName"));
      assertTrue(values0.has("id"));
   }



   /*******************************************************************************
    ** test a table query using an actual filter.
    **
    *******************************************************************************/
   @Test
   public void test_dataQueryWithFilter()
   {
      String               filterJson = getFirstNameEqualsFilterJSON("Tim");
      HttpResponse<String> response   = Unirest.get(BASE_URL + "/data/person?filter=" + URLEncoder.encode(filterJson, StandardCharsets.UTF_8)).asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertTrue(jsonObject.has("records"));
      JSONArray records = jsonObject.getJSONArray("records");
      assertEquals(1, records.length());
      JSONObject record0 = records.getJSONObject(0);
      JSONObject values0 = record0.getJSONObject("values");
      assertEquals("Tim", values0.getString("firstName"));
   }



   /*******************************************************************************
    ** test a table query using a filter and a queryJoin.
    **
    *******************************************************************************/
   @Test
   public void test_dataQueryWithFilterAndQueryJoin()
   {
      String filterJson = getFirstNameEqualsFilterJSON("Darin");
      String queryJoinsJson = """
         [{"joinTable":"person","select":true,"type":"INNER","joinName":"PersonJoinPartnerPerson","alias":"partnerPerson"}]
         """;

      HttpResponse<String> response = Unirest.get(BASE_URL + "/data/person?filter=" + URLEncoder.encode(filterJson, StandardCharsets.UTF_8)
                                                  + "&queryJoins=" + URLEncoder.encode(queryJoinsJson, StandardCharsets.UTF_8)).asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertTrue(jsonObject.has("records"));
      JSONArray records = jsonObject.getJSONArray("records");
      assertEquals(1, records.length());
      JSONObject record0 = records.getJSONObject(0);
      JSONObject values0 = record0.getJSONObject("values");
      assertEquals("Darin", values0.getString("firstName"));
      assertEquals("Linda", values0.getString("partnerPerson.firstName"));
   }



   /*******************************************************************************
    ** test a table query using an actual filter via POST.
    **
    *******************************************************************************/
   @Test
   public void test_dataQueryWithFilterPOST()
   {
      String filterJson = getFirstNameEqualsFilterJSON("Tim");
      HttpResponse<String> response = Unirest.post(BASE_URL + "/data/person/query")
         .field("filter", filterJson)
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertTrue(jsonObject.has("records"));
      JSONArray records = jsonObject.getJSONArray("records");
      assertEquals(1, records.length());
      JSONObject record0 = records.getJSONObject(0);
      JSONObject values0 = record0.getJSONObject("values");
      assertEquals("Tim", values0.getString("firstName"));
   }



   /*******************************************************************************
    ** test a table query using an actual filter via POST, with no limit specified,
    ** and with that not being allowed.
    **
    *******************************************************************************/
   @Test
   public void test_dataQueryWithFilterPOSTWithoutLimitNotAllowed() throws QInstanceValidationException
   {
      try
      {
         qJavalinImplementation.getJavalinMetaData()
            .withQueryWithoutLimitAllowed(false)
            .withQueryWithoutLimitDefault(3)
            .withQueryWithoutLimitLogLevel(Level.WARN);

         QCollectingLogger collectingLogger = QLogger.activateCollectingLoggerForClass(QJavalinUtils.class);

         String filterJson = """
            {"criteria":[]}""";

         HttpResponse<String> response = Unirest.post(BASE_URL + "/data/person/query")
            .field("filter", filterJson)
            .asString();

         assertEquals(200, response.getStatus());
         JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
         assertTrue(jsonObject.has("records"));
         JSONArray records = jsonObject.getJSONArray("records");
         assertEquals(3, records.length());

         assertThat(collectingLogger.getCollectedMessages())
            .anyMatch(m -> m.getLevel().equals(Level.WARN) && m.getMessage().contains("Query request did not specify a limit"));
      }
      finally
      {
         QLogger.activateCollectingLoggerForClass(QJavalinUtils.class);
         resetMetaDataQueryWithoutLimitSettings();
      }
   }



   /*******************************************************************************
    ** test a table query using an actual filter via POST, with no limit specified,
    ** but with that being allowed.
    **
    *******************************************************************************/
   @Test
   public void test_dataQueryWithFilterPOSTWithoutLimitAllowed() throws QInstanceValidationException
   {
      try
      {
         qJavalinImplementation.getJavalinMetaData()
            .withQueryWithoutLimitAllowed(true);

         QCollectingLogger collectingLogger = QLogger.activateCollectingLoggerForClass(QJavalinImplementation.class);

         String filterJson = """
            {"criteria":[]}""";

         HttpResponse<String> response = Unirest.post(BASE_URL + "/data/person/query")
            .field("filter", filterJson)
            .asString();

         assertEquals(200, response.getStatus());
         JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
         assertTrue(jsonObject.has("records"));
         JSONArray records = jsonObject.getJSONArray("records");
         assertEquals(6, records.length());

         assertThat(collectingLogger.getCollectedMessages())
            .noneMatch(m -> m.getMessage().contains("Query request did not specify a limit"));
      }
      finally
      {
         QLogger.activateCollectingLoggerForClass(QJavalinImplementation.class);
         resetMetaDataQueryWithoutLimitSettings();
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void resetMetaDataQueryWithoutLimitSettings()
   {
      qJavalinImplementation.getJavalinMetaData()
         .withQueryWithoutLimitAllowed(false)
         .withQueryWithoutLimitDefault(1000)
         .withQueryWithoutLimitLogLevel(Level.INFO);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String getFirstNameEqualsFilterJSON(String name)
   {
      return """
         {"criteria":[{"fieldName":"firstName","operator":"EQUALS","values":["${name}"]}]}""".replaceAll("\\$\\{name}", name);
   }



   /*******************************************************************************
    ** test an insert - posting the data as a JSON object.
    **
    ** This was the original supported version, but multipart form was added in May 2023
    **
    *******************************************************************************/
   @Test
   public void test_dataInsertJson()
   {
      Map<String, Serializable> body = new HashMap<>();
      body.put("firstName", "Bobby");
      body.put("lastName", "Hull");
      body.put("email", "bobby@hull.com");

      HttpResponse<String> response = Unirest.post(BASE_URL + "/data/person")
         .header("Content-Type", "application/json")
         .body(body)
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertTrue(jsonObject.has("records"));
      JSONArray records = jsonObject.getJSONArray("records");
      assertEquals(1, records.length());
      JSONObject record0 = records.getJSONObject(0);
      assertTrue(record0.has("values"));
      assertEquals("person", record0.getString("tableName"));
      JSONObject values0 = record0.getJSONObject("values");
      assertTrue(values0.has("firstName"));
      assertEquals("Bobby", values0.getString("firstName"));
      assertTrue(values0.has("id"));
      assertEquals(7, values0.getInt("id"));
   }



   /*******************************************************************************
    ** test an insert that returns a warning
    **
    *******************************************************************************/
   @Test
   public void test_dataInsertWithWarning()
   {
      Map<String, Serializable> body = new HashMap<>();
      body.put("firstName", "Warning");
      body.put("lastName", "Kelkhoff");
      body.put("email", "warning@kelkhoff.com");

      HttpResponse<String> response = Unirest.post(BASE_URL + "/data/person")
         .header("Content-Type", "application/json")
         .body(body)
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertTrue(jsonObject.has("records"));
      JSONArray records = jsonObject.getJSONArray("records");
      assertEquals(1, records.length());
      JSONObject record0 = records.getJSONObject(0);
      assertTrue(record0.has("values"));
      JSONObject values0 = record0.getJSONObject("values");
      assertTrue(values0.has("id"));
      assertEquals(7, values0.getInt("id"));

      assertTrue(record0.has("warnings"));
      JSONArray warnings = record0.getJSONArray("warnings");
      assertEquals(1, warnings.length());
      assertTrue(warnings.getJSONObject(0).has("message"));
   }



   /*******************************************************************************
    ** test an insert - posting a multipart form.
    **
    *******************************************************************************/
   @Test
   public void test_dataInsertMultipartForm() throws IOException, QException
   {
      HttpResponse<String> response = Unirest.post(BASE_URL + "/data/person")
         .header("Content-Type", "application/json")
         .multiPartContent()
         .field("firstName", "Bobby")
         .field("lastName", "Hull")
         .field("email", "bobby@hull.com")
         .field("associations", """
            {
               "pets":
               [
                  {"name": "Fido", "species": "dog"}
               ]
            }
            """)
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertTrue(jsonObject.has("records"));
      JSONArray records = jsonObject.getJSONArray("records");
      assertEquals(1, records.length());
      JSONObject record0     = records.getJSONObject(0);
      Integer    newPersonId = record0.getJSONObject("values").getInt("id");

      //////////////////////////////////////////////////////////////////////////
      // get all the pets - assert a new one was inserted for this new person //
      //////////////////////////////////////////////////////////////////////////
      HttpResponse<String> petGetResponse = Unirest.get(BASE_URL + "/data/pet").asString();
      assertEquals(200, petGetResponse.getStatus());
      JSONObject petsJsonObject       = JsonUtils.toJSONObject(petGetResponse.getBody());
      JSONArray  petRecords           = petsJsonObject.getJSONArray("records");
      boolean    foundPetForNewPerson = false;
      for(int i = 0; i < petRecords.length(); i++)
      {
         if(newPersonId.equals(petRecords.getJSONObject(i).getJSONObject("values").getInt("ownerPersonId")))
         {
            assertEquals("Fido", petRecords.getJSONObject(i).getJSONObject("values").getString("name"));
            foundPetForNewPerson = true;
         }
      }
      assertTrue(foundPetForNewPerson);
   }



   /*******************************************************************************
    ** test an insert - posting a multipart form - including associations!
    **
    *******************************************************************************/
   @Test
   public void test_dataInsertMultipartFormWithAssocitions() throws IOException
   {
      try(InputStream photoInputStream = getClass().getResourceAsStream("/photo.png"))
      {
         HttpResponse<String> response = Unirest.post(BASE_URL + "/data/person")
            .header("Content-Type", "application/json")
            .multiPartContent()
            .field("firstName", "Bobby")
            .field("lastName", "Hull")
            .field("email", "bobby@hull.com")
            .field("photo", photoInputStream.readAllBytes(), "image")
            .asString();

         assertEquals(200, response.getStatus());
         JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
         assertTrue(jsonObject.has("records"));
         JSONArray records = jsonObject.getJSONArray("records");
         assertEquals(1, records.length());
         JSONObject record0 = records.getJSONObject(0);
         assertTrue(record0.has("values"));
         assertEquals("person", record0.getString("tableName"));
         JSONObject values0 = record0.getJSONObject("values");
         assertTrue(values0.has("firstName"));
         assertEquals("Bobby", values0.getString("firstName"));
         assertTrue(values0.has("id"));
         assertEquals(7, values0.getInt("id"));
         assertTrue(values0.has("photo"));
      }
   }



   /*******************************************************************************
    ** test an update - posting the data as a JSON object.
    **
    ** This was the original supported version, but multipart form was added in May 2023
    **
    *******************************************************************************/
   @Test
   public void test_dataUpdateJson()
   {
      Map<String, Serializable> body = new HashMap<>();
      body.put("firstName", "Free");
      body.put("birthDate", "");

      HttpResponse<String> response = Unirest.patch(BASE_URL + "/data/person/4")
         .header("Content-Type", "application/json")
         .body(body)
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertTrue(jsonObject.has("records"));
      JSONArray records = jsonObject.getJSONArray("records");
      assertEquals(1, records.length());
      JSONObject record0 = records.getJSONObject(0);
      assertTrue(record0.has("values"));
      assertEquals("person", record0.getString("tableName"));
      JSONObject values0 = record0.getJSONObject("values");
      assertEquals(4, values0.getInt("id"));
      assertEquals("Free", values0.getString("firstName"));

      ///////////////////////////////////////////////////////////////////
      // re-GET the record, and validate that birthDate was nulled out //
      ///////////////////////////////////////////////////////////////////
      response = Unirest.get(BASE_URL + "/data/person/4").asString();
      assertEquals(200, response.getStatus());
      jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertTrue(jsonObject.has("values"));
      JSONObject values = jsonObject.getJSONObject("values");
      assertFalse(values.has("birthDate"));
   }



   /*******************************************************************************
    ** test an update - with a warning returned
    **
    *******************************************************************************/
   @Test
   public void test_dataUpdateWithWarning()
   {
      Map<String, Serializable> body = new HashMap<>();
      body.put("firstName", "Warning");
      body.put("birthDate", "");

      HttpResponse<String> response = Unirest.patch(BASE_URL + "/data/person/4")
         .header("Content-Type", "application/json")
         .body(body)
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertTrue(jsonObject.has("records"));
      JSONArray records = jsonObject.getJSONArray("records");
      assertEquals(1, records.length());
      JSONObject record0 = records.getJSONObject(0);
      assertTrue(record0.has("values"));
      assertEquals("person", record0.getString("tableName"));
      JSONObject values0 = record0.getJSONObject("values");
      assertEquals(4, values0.getInt("id"));
      assertEquals("Warning", values0.getString("firstName"));

      assertTrue(record0.has("warnings"));
      JSONArray warnings = record0.getJSONArray("warnings");
      assertEquals(1, warnings.length());
      assertTrue(warnings.getJSONObject(0).has("message"));

      ///////////////////////////////////////////////////////////////////
      // re-GET the record, and validate that birthDate was nulled out //
      ///////////////////////////////////////////////////////////////////
      response = Unirest.get(BASE_URL + "/data/person/4").asString();
      assertEquals(200, response.getStatus());
      jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertTrue(jsonObject.has("values"));
      JSONObject values = jsonObject.getJSONObject("values");
      assertFalse(values.has("birthDate"));
   }



   /*******************************************************************************
    ** test an update - posting the data as a multipart form
    **
    *******************************************************************************/
   @Test
   public void test_dataUpdateMultipartForm()
   {
      HttpResponse<String> response = Unirest.patch(BASE_URL + "/data/person/4")
         .multiPartContent()
         .field("firstName", "Free")
         .field("birthDate", "")
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertTrue(jsonObject.has("records"));
      JSONArray records = jsonObject.getJSONArray("records");
      assertEquals(1, records.length());
      JSONObject record0 = records.getJSONObject(0);
      assertTrue(record0.has("values"));
      assertEquals("person", record0.getString("tableName"));
      JSONObject values0 = record0.getJSONObject("values");
      assertEquals(4, values0.getInt("id"));
      assertEquals("Free", values0.getString("firstName"));

      ///////////////////////////////////////////////////////////////////
      // re-GET the record, and validate that birthDate was nulled out //
      ///////////////////////////////////////////////////////////////////
      response = Unirest.get(BASE_URL + "/data/person/4").asString();
      assertEquals(200, response.getStatus());
      jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertTrue(jsonObject.has("values"));
      JSONObject values = jsonObject.getJSONObject("values");
      assertFalse(values.has("birthDate"));
   }



   /*******************************************************************************
    ** test a delete
    **
    *******************************************************************************/
   @Test
   public void test_dataDelete() throws Exception
   {
      HttpResponse<String> response = Unirest.delete(BASE_URL + "/data/person/3").asString();
      assertEquals(200, response.getStatus());

      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);
      assertEquals(1, jsonObject.getInt("deletedRecordCount"));
      TestUtils.runTestSql("SELECT id FROM person", (rs ->
      {
         int rowsFound = 0;
         while(rs.next())
         {
            rowsFound++;
            assertNotEquals(3, rs.getInt(1));
         }
         assertEquals(5, rowsFound);
      }));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testExportCsvPerFileName()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/data/person/export/MyPersonExport.csv").asString();
      assertEquals(200, response.getStatus());
      assertEquals("text/csv;charset=utf-8", response.getHeaders().get("Content-Type").get(0));
      assertEquals("filename=MyPersonExport.csv", response.getHeaders().get("Content-Disposition").get(0));
      String[] csvLines = response.getBody().split("\n");
      assertEquals(7, csvLines.length);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testExportNoFormat()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/data/person/export/").asString();
      assertEquals(HttpStatus.BAD_REQUEST_400, response.getStatus());
      assertThat(response.getBody()).contains("Report format was not specified");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testExportExcelPerFormatQueryParam()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/data/person/export/?format=xlsx").asString();
      assertEquals(200, response.getStatus());
      assertEquals(ReportFormat.XLSX.getMimeType() + ";charset=utf-8", response.getHeaders().get("Content-Type").get(0));
      assertEquals("filename=person.xlsx", response.getHeaders().get("Content-Disposition").get(0));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testExportFilterQueryParam()
   {
      String               filterJson = getFirstNameEqualsFilterJSON("Tim");
      HttpResponse<String> response   = Unirest.get(BASE_URL + "/data/person/export/Favorite People.csv?filter=" + URLEncoder.encode(filterJson, StandardCharsets.UTF_8)).asString();
      assertEquals("filename=Favorite People.csv", response.getHeaders().get("Content-Disposition").get(0));
      String[] csvLines = response.getBody().split("\n");
      assertEquals(2, csvLines.length);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testExportFieldsQueryParam()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/data/person/export/People.csv?fields=id,birthDate").asString();
      String[]             csvLines = response.getBody().split("\n");
      assertEquals("""
         "Id","Birth Date\"""", csvLines[0]);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testExportSupportedFormat()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/data/person/export/?format=docx").asString();
      assertEquals(HttpStatus.BAD_REQUEST_400, response.getStatus());
      assertThat(response.getBody()).contains("Unsupported report format");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testExportOmitHeader()
   {
      String               filterJson = getFirstNameEqualsFilterJSON("Tim");
      HttpResponse<String> response   = Unirest.get(BASE_URL + "/data/person/export/Favorite People.csv?omitHeaderRow=true&filter=" + URLEncoder.encode(filterJson, StandardCharsets.UTF_8)).asString();
      assertEquals("filename=Favorite People.csv", response.getHeaders().get("Content-Disposition").get(0));
      String[] csvLines = response.getBody().split("\n");
      assertEquals(1, csvLines.length);
      assertThat(csvLines[0]).contains("Tim");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWidget()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/widget/" + PersonsByCreateDateBarChart.class.getSimpleName()).asString();
      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);
      assertEquals(WidgetType.CHART.getType(), jsonObject.getString("type"));
      assertNotNull(jsonObject.getString("title"));
      assertNotNull(jsonObject.getJSONObject("chartData"));
   }



   /*******************************************************************************
    * verify that query string params form a GET are processed
    *******************************************************************************/
   @Test
   void testWidgetQueryStringParam()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/widget/" + TestUtils.EchoWidgetRenderer.class.getSimpleName() + "?input=helloThere").asString();
      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);
      assertEquals(WidgetType.HTML.getType(), jsonObject.getString("type"));
      assertNotNull(jsonObject.getString("title"));
      assertEquals("helloThere", jsonObject.getString("html"));
   }



   /*******************************************************************************
    * verify the widget POST endpoint and that it accepts posted values (multipart form)
    *******************************************************************************/
   @Test
   void testWidgetPost()
   {
      HttpResponse<String> response = Unirest.post(BASE_URL + "/widget/" + TestUtils.EchoWidgetRenderer.class.getSimpleName())
         .field("input", "foobar")
         .asString();
      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);
      assertEquals(WidgetType.HTML.getType(), jsonObject.getString("type"));
      assertNotNull(jsonObject.getString("title"));
      assertEquals("foobar", jsonObject.getString("html"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testStandalonePossibleValueSource()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/possibleValues/person").asString();
      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);
      assertNotNull(jsonObject.getJSONArray("options"));
      assertEquals(6, jsonObject.getJSONArray("options").length());
      assertEquals(1, jsonObject.getJSONArray("options").getJSONObject(0).getInt("id"));
      assertEquals("Darin Kelkhoff (1)", jsonObject.getJSONArray("options").getJSONObject(0).getString("label"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testStandalonePossibleValueSourceWithFilter()
   {
      HttpResponse<String> response = Unirest.post(BASE_URL + "/possibleValues/person")
         .field("values", JsonUtils.toJson(Map.of("firstInitial", "D")))
         .field("filter", JsonUtils.toJson(new QQueryFilter(new QFilterCriteria("firstName", QCriteriaOperator.STARTS_WITH, "${input.firstInitial}"))))
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);
      assertNotNull(jsonObject.getJSONArray("options"));
      assertEquals(1, jsonObject.getJSONArray("options").length());
      assertEquals(1, jsonObject.getJSONArray("options").getJSONObject(0).getInt("id"));
      assertEquals("Darin Kelkhoff (1)", jsonObject.getJSONArray("options").getJSONObject(0).getString("label"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPossibleValueUnfiltered()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/data/person/possibleValues/partnerPersonId").asString();
      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);
      assertNotNull(jsonObject.getJSONArray("options"));
      assertEquals(6, jsonObject.getJSONArray("options").length());
      assertEquals(1, jsonObject.getJSONArray("options").getJSONObject(0).getInt("id"));
      assertEquals("Darin Kelkhoff (1)", jsonObject.getJSONArray("options").getJSONObject(0).getString("label"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPossibleValueWithSearchTerm()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/data/person/possibleValues/partnerPersonId?searchTerm=Chamber").asString();
      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);
      assertNotNull(jsonObject.getJSONArray("options"));
      assertEquals(1, jsonObject.getJSONArray("options").length());
      assertEquals("Tim Chamberlain (3)", jsonObject.getJSONArray("options").getJSONObject(0).getString("label"));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private JSONArray assertPossibleValueSuccessfulResponseAndGetOptionsArray(HttpResponse<String> response)
   {
      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);
      return (jsonObject.getJSONArray("options"));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void assertPossibleValueSuccessfulResponseWithNoOptions(HttpResponse<String> response)
   {
      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);
      assertFalse(jsonObject.has("options")); // no results comes back as result w/o options array.
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPossibleValueWithFilter()
   {
      /////////////////////////////////////////////////////////////
      // post with no search term, and values that find a result //
      /////////////////////////////////////////////////////////////
      HttpResponse<String> response = Unirest.post(BASE_URL + "/data/pet/possibleValues/ownerPersonId?searchTerm=")
         .field("values", """
            {"email":"tsamples@mmltholdings.com"}
            """)
         .asString();

      JSONArray options = assertPossibleValueSuccessfulResponseAndGetOptionsArray(response);
      assertNotNull(options);
      assertEquals(1, options.length());
      assertEquals("Tyler Samples (4)", options.getJSONObject(0).getString("label"));

      ///////////////////////////////////////////////////////////
      // post with search term and values that find no results //
      ///////////////////////////////////////////////////////////
      response = Unirest.post(BASE_URL + "/data/pet/possibleValues/ownerPersonId?searchTerm=notFound")
         .field("values", """
            {"email":"tsamples@mmltholdings.com"}
            """)
         .asString();
      assertPossibleValueSuccessfulResponseWithNoOptions(response);

      ////////////////////////////////////////////////////////////////
      // post with no search term, but values that cause no matches //
      ////////////////////////////////////////////////////////////////
      response = Unirest.post(BASE_URL + "/data/pet/possibleValues/ownerPersonId?searchTerm=")
         .field("values", """
            {"email":"noUser@mmltholdings.com"}
            """)
         .asString();
      assertPossibleValueSuccessfulResponseWithNoOptions(response);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPossibleValueWithFilterMissingValue()
   {
      /////////////////////////////////////////////////////////////
      // filter use-case, with no values, should return options. //
      /////////////////////////////////////////////////////////////
      HttpResponse<String> response = Unirest.post(BASE_URL + "/data/pet/possibleValues/ownerPersonId?searchTerm=&useCase=filter").asString();
      JSONArray            options  = assertPossibleValueSuccessfulResponseAndGetOptionsArray(response);
      assertNotNull(options);
      assertThat(options.length()).isGreaterThanOrEqualTo(5);

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // similarly, values map, but not the 'email' value, that this PVS field is based on, should return options. //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
      response = Unirest.post(BASE_URL + "/data/pet/possibleValues/ownerPersonId?searchTerm=&useCase=filter")
         .field("values", """
            {"userId":"123"}
            """)
         .asString();
      options = assertPossibleValueSuccessfulResponseAndGetOptionsArray(response);
      assertNotNull(options);
      assertThat(options.length()).isGreaterThanOrEqualTo(5);

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // similarly, values map, with the email value, but an empty string in there - should act the same as if it's missing, and not filter the values. //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      response = Unirest.post(BASE_URL + "/data/pet/possibleValues/ownerPersonId?searchTerm=&useCase=filter")
         .field("values", """
            {"email":""}
            """)
         .asString();
      options = assertPossibleValueSuccessfulResponseAndGetOptionsArray(response);
      assertNotNull(options);
      assertThat(options.length()).isGreaterThanOrEqualTo(5);

      /////////////////////////////////////////////////////////////////////////
      // versus form use-case with no values, which should return 0 options. //
      /////////////////////////////////////////////////////////////////////////
      response = Unirest.post(BASE_URL + "/data/pet/possibleValues/ownerPersonId?searchTerm=&useCase=form").asString();
      assertPossibleValueSuccessfulResponseWithNoOptions(response);

      /////////////////////////////////////////////////////////////////////////////////
      // versus form use-case with expected value, which should return some options. //
      /////////////////////////////////////////////////////////////////////////////////
      response = Unirest.post(BASE_URL + "/data/pet/possibleValues/ownerPersonId?searchTerm=&useCase=form")
         .field("values", """
            {"email":"tsamples@mmltholdings.com"}
            """)
         .asString();
      options = assertPossibleValueSuccessfulResponseAndGetOptionsArray(response);
      assertNotNull(options);
      assertEquals(1, options.length());
      assertEquals("Tyler Samples (4)", options.getJSONObject(0).getString("label"));

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // finally an unrecognized useCase (or missing or empty), should behave the same as a form, and return 0 options if the filter-value is missing. //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      assertPossibleValueSuccessfulResponseWithNoOptions(Unirest.post(BASE_URL + "/data/pet/possibleValues/ownerPersonId?searchTerm=&useCase=notAUseCase").asString());
      assertPossibleValueSuccessfulResponseWithNoOptions(Unirest.post(BASE_URL + "/data/pet/possibleValues/ownerPersonId?searchTerm=").asString());
      assertPossibleValueSuccessfulResponseWithNoOptions(Unirest.post(BASE_URL + "/data/pet/possibleValues/ownerPersonId?searchTerm=&useCase=").asString());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPossibleValueWithIds()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/data/person/possibleValues/partnerPersonId?ids=4,5").asString();
      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);
      assertNotNull(jsonObject.getJSONArray("options"));
      assertEquals(2, jsonObject.getJSONArray("options").length());
      assertEquals(4, jsonObject.getJSONArray("options").getJSONObject(0).getInt("id"));
      assertEquals(5, jsonObject.getJSONArray("options").getJSONObject(1).getInt("id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testServerInfo()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/serverInfo").asString();
      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);
      assertTrue(jsonObject.has("startTimeMillis"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAuthenticationMetaData()
   {
      HttpResponse<String> response = Unirest.get(BASE_URL + "/metaData/authentication").asString();
      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);
      assertTrue(jsonObject.has("name"));
      assertTrue(jsonObject.has("type"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testManageSession()
   {
      String body = """
         {
            "accessToken": "abcd",
            "doStoreUserSession": true
         }
         """;
      HttpResponse<String> response = Unirest.post(BASE_URL + "/manageSession")
         .header("Content-Type", "application/json")
         .body(body)
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);
      assertTrue(jsonObject.has("uuid"));
      response.getHeaders().get("Set-Cookie").stream().anyMatch(s -> s.contains("sessionUUID"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTimeZoneHeaders()
   {
      {
         HttpResponse<String> response = Unirest.get(BASE_URL + "/widget/timezoneWidget")
            .header("X-QQQ-UserTimezoneOffsetMinutes", "300")
            .header("X-QQQ-UserTimezone", "US/Central")
            .asString();

         JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
         assertEquals("300|US/Central", jsonObject.getString("html"));
      }

      {
         HttpResponse<String> response = Unirest.get(BASE_URL + "/widget/timezoneWidget")
            .header("X-QQQ-UserTimezoneOffsetMinutes", "-600")
            .header("X-QQQ-UserTimezone", "SomeZone")
            .asString();

         JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
         assertEquals("-600|SomeZone", jsonObject.getString("html"));
      }

      {
         HttpResponse<String> response = Unirest.get(BASE_URL + "/widget/timezoneWidget")
            .header("X-QQQ-UserTimezoneOffsetMinutes", "foo")
            .asString();

         JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
         assertEquals("null|null", jsonObject.getString("html"));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testHotSwap() throws QInstanceValidationException
   {
      try
      {
         Function<String, QInstance> makeNewInstanceWithBackendName = (backendName) ->
         {
            QInstance newInstance = new QInstance();
            newInstance.setAuthentication(new QAuthenticationMetaData().withType(QAuthenticationType.FULLY_ANONYMOUS).withName("anonymous"));
            newInstance.addBackend(new QBackendMetaData().withName(backendName).withBackendType(MockBackendModule.class));

            if(!"invalid".equals(backendName))
            {
               newInstance.addTable(new QTableMetaData()
                  .withName("newTable")
                  .withBackendName(backendName)
                  .withField(new QFieldMetaData("newField", QFieldType.INTEGER))
                  .withPrimaryKeyField("newField")
               );
            }

            return (newInstance);
         };

         QJavalinImplementation.setQInstanceHotSwapSupplier(() -> makeNewInstanceWithBackendName.apply("newBackend"));

         /////////////////////////////////////////////////////////////////////////////////
         // make sure before a hot-swap, that the instance doesn't have our new backend //
         /////////////////////////////////////////////////////////////////////////////////
         assertNull(QJavalinImplementation.qInstance.getBackend("newBackend"));

         ///////////////////////////////////////////////////////
         // do a hot-swap, make sure the new backend is there //
         ///////////////////////////////////////////////////////
         QJavalinImplementation.hotSwapQInstance(null);
         assertNotNull(QJavalinImplementation.qInstance.getBackend("newBackend"));

         ////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // now change to make a different backend - try to swap again - but the newer backend shouldn't be there, //
         // because the millis-between-hot-swaps won't have passed                                                 //
         ////////////////////////////////////////////////////////////////////////////////////////////////////////////
         QJavalinImplementation.setQInstanceHotSwapSupplier(() -> makeNewInstanceWithBackendName.apply("newerBackend"));
         QJavalinImplementation.hotSwapQInstance(null);
         assertNull(QJavalinImplementation.qInstance.getBackend("newerBackend"));

         ////////////////////////////////////////////////////////////////////////////////////////////
         // set the sleep threshold to 1 milli, sleep for 2, and then assert that we do swap again //
         ////////////////////////////////////////////////////////////////////////////////////////////
         QJavalinImplementation.setMillisBetweenHotSwaps(1);
         SleepUtils.sleep(2, TimeUnit.MILLISECONDS);

         QJavalinImplementation.setQInstanceHotSwapSupplier(() -> makeNewInstanceWithBackendName.apply("newerBackend"));
         QJavalinImplementation.hotSwapQInstance(null);
         assertNotNull(QJavalinImplementation.qInstance.getBackend("newerBackend"));

         ////////////////////////////////////////////////////////////
         // assert that an invalid instance doesn't get swapped in //
         // e.g., "newerBackend" still exists                      //
         ////////////////////////////////////////////////////////////
         SleepUtils.sleep(2, TimeUnit.MILLISECONDS);
         QJavalinImplementation.setQInstanceHotSwapSupplier(() -> makeNewInstanceWithBackendName.apply("invalid"));
         QJavalinImplementation.hotSwapQInstance(null);
         assertNotNull(QJavalinImplementation.qInstance.getBackend("newerBackend"));

         ///////////////////////////////////////////////////////
         // assert that if the supplier throws, we don't swap //
         // e.g., "newerBackend" still exists                 //
         ///////////////////////////////////////////////////////
         SleepUtils.sleep(2, TimeUnit.MILLISECONDS);
         QJavalinImplementation.setQInstanceHotSwapSupplier(() ->
         {
            throw new RuntimeException("oops");
         });
         QJavalinImplementation.hotSwapQInstance(null);
         assertNotNull(QJavalinImplementation.qInstance.getBackend("newerBackend"));

         /////////////////////////////////////////////////////////////
         // assert that if the supplier returns null, we don't swap //
         // e.g., "newerBackend" still exists                       //
         /////////////////////////////////////////////////////////////
         SleepUtils.sleep(2, TimeUnit.MILLISECONDS);
         QJavalinImplementation.setQInstanceHotSwapSupplier(() -> null);
         QJavalinImplementation.hotSwapQInstance(null);
         assertNotNull(QJavalinImplementation.qInstance.getBackend("newerBackend"));
      }
      finally
      {
         ////////////////////////////////////////////////////////////
         // restore things to how they used to be, for other tests //
         ////////////////////////////////////////////////////////////
         QInstance qInstance = TestUtils.defineInstance();
         QJavalinImplementation.setQInstanceHotSwapSupplier(null);
         restartServerWithInstance(qInstance);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static class TestDownloadFileSupplementalAction implements DownloadFileSupplementalAction
   {
      static int callCount = 0;



      @Override
      public void run(DownloadFileSupplementalActionInput input, DownloadFileSupplementalActionOutput output) throws QException
      {
         callCount++;
      }
   }
}
