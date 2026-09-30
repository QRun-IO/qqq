/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.reporting;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.ExamplePersonalizer;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ExportInput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ExportOutput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportDestination;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormat;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.StringLengthFunction;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.LocalMacDevUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for ExportAction
 *******************************************************************************/
class ExportActionTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testCSV() throws Exception
   {
      int    recordCount = 1000;
      String filename    = "/tmp/ReportActionTest.csv";

      runExport(recordCount, filename, ReportFormat.CSV, false);

      File file = new File(filename);
      @SuppressWarnings("unchecked")
      List<String> fileLines = FileUtils.readLines(file, StandardCharsets.UTF_8.name());
      assertEquals(recordCount + 1, fileLines.size());
      assertTrue(file.delete());
   }



   /*******************************************************************************
    ** This test runs for more records, to stress more of the pipe-filling and
    ** other bits of the ReportAction.
    *******************************************************************************/
   @Test
   public void testBigger() throws Exception
   {
      // int    recordCount = 2_000_000; // to really stress locally, use this.
      int    recordCount = 10_000;
      String filename    = "/tmp/ReportActionTest.csv";

      runExport(recordCount, filename, ReportFormat.CSV, false);

      File file = new File(filename);
      @SuppressWarnings("unchecked")
      List<String> fileLines = FileUtils.readLines(file, StandardCharsets.UTF_8.name());
      assertEquals(recordCount + 1, fileLines.size());
      assertTrue(file.delete());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testExcel() throws Exception
   {
      int    recordCount = 1000;
      String filename    = "/tmp/ReportActionTest.xlsx";

      runExport(recordCount, filename, ReportFormat.XLSX, true);

      File file = new File(filename);
      LocalMacDevUtils.openFile(file.getAbsolutePath());

      assertTrue(file.delete());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testExcelPOI() throws Exception
   {
      int    recordCount = 1000;
      String filename    = "/tmp/ReportActionTest-POI.xlsx";

      runExport(recordCount, filename, ReportFormat.XLSX, true);

      File file = new File(filename);
      LocalMacDevUtils.openFile(file.getAbsolutePath());

      assertTrue(file.delete());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testJoins() throws QException, IOException
   {
      QInstance qInstance = QContext.getQInstance();
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);

      TestUtils.insertRecords(qInstance.getTable(TestUtils.TABLE_NAME_ORDER), List.of(
         new QRecord().withValue("id", 1).withValue("orderNo", "ORD1").withValue("storeId", 1),
         new QRecord().withValue("id", 2).withValue("orderNo", "ORD2").withValue("storeId", 1)
      ));

      TestUtils.insertRecords(qInstance.getTable(TestUtils.TABLE_NAME_LINE_ITEM), List.of(
         new QRecord().withValue("id", 1).withValue("orderId", 1).withValue("sku", "A").withValue("quantity", 10),
         new QRecord().withValue("id", 2).withValue("orderId", 1).withValue("sku", "B").withValue("quantity", 15),
         new QRecord().withValue("id", 3).withValue("orderId", 2).withValue("sku", "A").withValue("quantity", 20)
      ));

      ExportInput exportInput = new ExportInput();
      exportInput.setTableName(TestUtils.TABLE_NAME_ORDER);

      ByteArrayOutputStream reportOutputStream = new ByteArrayOutputStream();
      exportInput.setReportDestination(new ReportDestination()
         .withReportFormat(ReportFormat.CSV)
         .withReportOutputStream(reportOutputStream));
      exportInput.setQueryFilter(new QQueryFilter());
      exportInput.setFieldNames(List.of("id", "orderNo", "storeId", "orderLine.id", "orderLine.sku", "orderLine.quantity"));
      // exportInput.setFieldNames(List.of("id", "orderNo", "storeId"));
      new ExportAction().execute(exportInput);

      String              csv               = reportOutputStream.toString(StandardCharsets.UTF_8);
      CSVParser           parse             = CSVParser.parse(csv, CSVFormat.DEFAULT.withFirstRecordAsHeader());
      Iterator<CSVRecord> csvRecordIterator = parse.iterator();
      assertFalse(parse.getHeaderMap().isEmpty());
      assertTrue(parse.getHeaderMap().containsKey("Id"));
      assertTrue(parse.getHeaderMap().containsKey("Order Line: Id"));
      assertTrue(parse.getHeaderMap().containsKey("Order Line: SKU"));

      CSVRecord csvRecord = csvRecordIterator.next();
      assertEquals("1", csvRecord.get("Id"));
      assertEquals("1", csvRecord.get("Order Line: Id"));
      assertEquals("A", csvRecord.get("Order Line: SKU"));
      assertEquals("10", csvRecord.get("Order Line: Quantity"));

      csvRecord = csvRecordIterator.next();
      assertEquals("1", csvRecord.get("Id"));
      assertEquals("2", csvRecord.get("Order Line: Id"));
      assertEquals("B", csvRecord.get("Order Line: SKU"));
      assertEquals("15", csvRecord.get("Order Line: Quantity"));

      csvRecord = csvRecordIterator.next();
      assertEquals("2", csvRecord.get("Id"));
      assertEquals("3", csvRecord.get("Order Line: Id"));
      assertEquals("A", csvRecord.get("Order Line: SKU"));
      assertEquals("20", csvRecord.get("Order Line: Quantity"));

      assertFalse(csvRecordIterator.hasNext());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void runExport(int recordCount, String filename, ReportFormat reportFormat, boolean specifyFields) throws IOException, QException
   {
      runExport("person", recordCount, filename, reportFormat, specifyFields);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void runExport(String tableName, int recordCount, String filename, ReportFormat reportFormat, boolean specifyFields) throws IOException, QException
   {
      try(FileOutputStream outputStream = new FileOutputStream(filename))
      {
         ExportInput exportInput = new ExportInput();
         exportInput.setTableName(tableName);
         QTableMetaData table = exportInput.getTable();

         exportInput.setReportDestination(new ReportDestination().withReportFormat(reportFormat).withReportOutputStream(outputStream));
         exportInput.setQueryFilter(new QQueryFilter());
         exportInput.setLimit(recordCount);
         exportInput.setInputSource(QInputSource.USER);

         if(specifyFields)
         {
            exportInput.setFieldNames(table.getFields().values().stream().map(QFieldMetaData::getName).collect(Collectors.toList()));
         }
         ExportOutput exportOutput = new ExportAction().execute(exportInput);
         assertNotNull(exportOutput);
         assertEquals(recordCount, exportOutput.getRecordCount());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBadFieldNames()
   {
      ExportInput exportInput = new ExportInput();
      exportInput.setTableName("person");
      exportInput.setFieldNames(List.of("Foo", "Bar", "Baz"));
      assertThrows(QUserFacingException.class, () ->
      {
         new ExportAction().execute(exportInput);
      });
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPreExecuteCount() throws QException
   {
      ExportInput exportInput = new ExportInput();
      exportInput.setTableName("person");

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // use xlsx, which has a max-rows limit, to verify that code runs, but doesn't throw when there aren't too many rows //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      exportInput.setReportDestination(new ReportDestination().withReportFormat(ReportFormat.XLSX));

      new ExportAction().preExecute(exportInput);

      ////////////////////////////////////////////////////////////////////////////
      // nothing to assert - but if preExecute throws, then the test will fail. //
      ////////////////////////////////////////////////////////////////////////////
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTooManyColumns() throws QException
   {
      QTableMetaData wideTable = new QTableMetaData()
         .withName("wide")
         .withPrimaryKeyField("field0")
         .withBackendName(TestUtils.DEFAULT_BACKEND_NAME);
      for(int i = 0; i < ReportFormat.XLSX.getMaxCols() + 1; i++)
      {
         wideTable.addField(new QFieldMetaData("field" + i, QFieldType.STRING));
      }

      QInstance qInstance = QContext.getQInstance();
      qInstance.addTable(wideTable);

      ExportInput exportInput = new ExportInput();
      exportInput.setTableName("wide");

      ////////////////////////////////////////////////////////////////
      // use xlsx, which has a max-cols limit, to verify that code. //
      ////////////////////////////////////////////////////////////////
      exportInput.setReportDestination(new ReportDestination().withReportFormat(ReportFormat.XLSX));

      assertThrows(QUserFacingException.class, () ->
      {
         new ExportAction().preExecute(exportInput);
      });
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testJSON() throws Exception
   {
      int    recordCount = 1000;
      String filename    = "/tmp/ReportActionTest.json";

      runExport(recordCount, filename, ReportFormat.JSON, false);

      File file = new File(filename);
      @SuppressWarnings("unchecked")
      String fileContent = FileUtils.readFileToString(file, StandardCharsets.UTF_8.name());
      JSONArray jsonArray = new JSONArray(fileContent);
      assertEquals(recordCount, jsonArray.length());
      JSONObject row0 = jsonArray.getJSONObject(0);
      assertNotNull(row0.optString("lastName"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPersonalizedTable() throws QException, IOException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      insertInput.setRecords(List.of(new QRecord().withValue("firstName", "Homer").withValue("lastName", "Simpson")));
      new InsertAction().execute(insertInput);

      //////////////////////////////////////////////////////////////
      // run a default export - assert it contains lastName field //
      //////////////////////////////////////////////////////////////
      String fullFileName = "/tmp/full.csv";
      runExport(TestUtils.TABLE_NAME_PERSON_MEMORY, 1, fullFileName, ReportFormat.CSV, false);
      String fullFileContent = FileUtils.readFileToString(new File(fullFileName), StandardCharsets.UTF_8.name());
      assertThat(fullFileContent)
         .contains("First Name").contains("Homer")
         .contains("Last Name").contains("Simpson");

      /////////////////////////////////////////////////
      // now personalize the table to remove a field //
      /////////////////////////////////////////////////
      String userId = "jdoe";
      ExamplePersonalizer.registerInQInstance();
      ExamplePersonalizer.addCustomizableTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      ExamplePersonalizer.addFieldToRemoveForUserId(TestUtils.TABLE_NAME_PERSON_MEMORY, "lastName", userId);
      QContext.getQSession().getUser().setIdReference(userId);

      String limitedFileName = "/tmp/limited.csv";
      runExport(TestUtils.TABLE_NAME_PERSON_MEMORY, 1, limitedFileName, ReportFormat.CSV, false);
      String limitedFileContent = FileUtils.readFileToString(new File(limitedFileName), StandardCharsets.UTF_8.name());
      assertThat(limitedFileContent)
         .contains("First Name").contains("Homer")
         .doesNotContain("Last Name").doesNotContain("Simpson");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testVirtualFieldNotQueryableRejected()
   {
      QInstance     qInstance = QContext.getQInstance();
      QTableMetaData table    = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      table.withVirtualField(new QVirtualFieldMetaData("notSelectable", QFieldType.INTEGER)
         .withIsQuerySelectable(false)
         .withFieldFunction(new FieldFunction()
            .withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER)
            .withFieldName("firstName")));

      ExportInput exportInput = new ExportInput();
      exportInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      exportInput.setFieldNames(List.of("firstName", "notSelectable"));
      exportInput.setReportDestination(new ReportDestination().withReportFormat(ReportFormat.CSV));

      QUserFacingException exception = assertThrows(QUserFacingException.class, () ->
         new ExportAction().preExecute(exportInput));
      assertThat(exception.getMessage()).contains("not queryable");
      assertThat(exception.getMessage()).contains("notSelectable");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNotFoundAndNotQueryableCombinedErrorMessage()
   {
      QInstance     qInstance = QContext.getQInstance();
      QTableMetaData table    = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      table.withVirtualField(new QVirtualFieldMetaData("notSelectable", QFieldType.INTEGER)
         .withIsQuerySelectable(false)
         .withFieldFunction(new FieldFunction()
            .withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER)
            .withFieldName("firstName")));

      ExportInput exportInput = new ExportInput();
      exportInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      exportInput.setFieldNames(List.of("noSuchField", "notSelectable"));
      exportInput.setReportDestination(new ReportDestination().withReportFormat(ReportFormat.CSV));

      QUserFacingException exception = assertThrows(QUserFacingException.class, () ->
         new ExportAction().preExecute(exportInput));
      assertThat(exception.getMessage()).contains("not found");
      assertThat(exception.getMessage()).contains("not queryable");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testExportWithSelectableVirtualField() throws QException, IOException
   {
      QInstance     qInstance = QContext.getQInstance();
      QTableMetaData table    = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      table.withVirtualField(new QVirtualFieldMetaData("firstNameLength", QFieldType.INTEGER)
         .withLabel("First Name Length")
         .withIsQuerySelectable(true)
         .withFieldFunction(new FieldFunction()
            .withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER)
            .withFieldName("firstName")));

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      insertInput.setRecords(List.of(new QRecord().withValue("firstName", "Homer").withValue("lastName", "Simpson")));
      new InsertAction().execute(insertInput);

      ByteArrayOutputStream reportOutputStream = new ByteArrayOutputStream();
      ExportInput exportInput = new ExportInput();
      exportInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      exportInput.setReportDestination(new ReportDestination()
         .withReportFormat(ReportFormat.CSV)
         .withReportOutputStream(reportOutputStream));
      exportInput.setQueryFilter(new QQueryFilter());
      exportInput.setFieldNames(List.of("firstName", "firstNameLength"));
      new ExportAction().execute(exportInput);

      String csv = reportOutputStream.toString(StandardCharsets.UTF_8);
      assertThat(csv).contains("First Name Length");
      assertThat(csv).contains("Homer");
      assertThat(csv).contains("5");
   }

}