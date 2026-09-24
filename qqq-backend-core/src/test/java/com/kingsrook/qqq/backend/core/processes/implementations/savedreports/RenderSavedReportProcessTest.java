/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.savedreports;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.processes.QProcessCallbackFactory;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.reporting.GenerateReportActionTest;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormatPossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable.PivotTableDefinition;
import com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable.PivotTableFunction;
import com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable.PivotTableGroupBy;
import com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable.PivotTableValue;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.savedreports.ReportColumns;
import com.kingsrook.qqq.backend.core.model.savedreports.SavedReport;
import com.kingsrook.qqq.backend.core.model.savedreports.SavedReportsMetaDataProvider;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryStorageAction;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.LocalMacDevUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFPivotTable;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for RenderSavedReportExecuteStep 
 *******************************************************************************/
class RenderSavedReportProcessTest extends BaseTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws Exception
   {
      new SavedReportsMetaDataProvider().defineAll(QContext.getQInstance(), TestUtils.MEMORY_BACKEND_NAME, TestUtils.MEMORY_BACKEND_NAME, null);
      GenerateReportActionTest.insertPersonRecords(QContext.getQInstance());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   @Disabled
   void testForDevPrintAPivotDefinitionAsJson()
   {
      System.out.println(JsonUtils.toPrettyJson(new PivotTableDefinition()
         .withRow(new PivotTableGroupBy()
            .withFieldName("homeStateId"))
         .withRow(new PivotTableGroupBy()
            .withFieldName("firstName"))
         .withValue(new PivotTableValue()
            .withFieldName("id")
            .withFunction(PivotTableFunction.COUNT))
         .withValue(new PivotTableValue()
            .withFieldName("cost")
            .withFunction(PivotTableFunction.SUM))
      ));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableOnlyReport() throws Exception
   {
      String label = "Test Report";

      //////////////////////////////////////////////////////////////////////////////////////////
      // do columns json as a string, rather than a toJson'ed ReportColumns object,           //
      // to help verify that we don't choke on un-recognized properties (e.g., as QFMD sends) //
      //////////////////////////////////////////////////////////////////////////////////////////
      String columnsJson = """
         {"columns":[
               {"name": "k"},
               {"name": "id"},
               {"name": "firstName", "isVisible": true},
               {"name": "lastName", "pinned": "left"},
               {"name": "createDate", "isVisible": false}
         ]}""";

      QRecord savedReport = new InsertAction().execute(new InsertInput(SavedReport.TABLE_NAME).withRecordEntity(new SavedReport()
         .withLabel(label)
         .withTableName(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withColumnsJson(columnsJson)
         .withQueryFilterJson(JsonUtils.toJson(new QQueryFilter()))
      )).getRecords().get(0);

      RunProcessOutput runProcessOutput = runRenderReportProcess(savedReport, ReportFormatPossibleValueEnum.CSV);

      String downloadFileName = runProcessOutput.getValueString("downloadFileName");
      assertThat(downloadFileName)
         .startsWith(label + " - ")
         .matches(".*\\d\\d\\d\\d-\\d\\d-\\d\\d-\\d\\d\\d\\d.*")
         .endsWith(".csv");

      InputStream  inputStream = getInputStream(runProcessOutput);
      List<String> lines       = IOUtils.readLines(inputStream, StandardCharsets.UTF_8);

      assertEquals("""
         "Id","First Name","Last Name"
         """.trim(), lines.get(0));
      assertEquals("""
         "1","Darin","Jonson"
         """.trim(), lines.get(1));

      writeTmpFileAndOpen(inputStream, ".csv");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static InputStream getInputStream(RunProcessOutput runProcessOutput) throws QException
   {
      String              storageTableName    = runProcessOutput.getValueString("storageTableName");
      String              storageReference    = runProcessOutput.getValueString("storageReference");

      MemoryStorageAction memoryStorageAction = new MemoryStorageAction();
      StorageInput        storageInput        = new StorageInput(storageTableName).withReference(storageReference);
      InputStream         inputStream         = memoryStorageAction.getInputStream(storageInput);

      /////////////////////////////////////////////////////////////////////////////////////////////////////
      // to help add a little bit of class-level code coverage, for the QStorageInterface, call a method //
      // that has a defualt implementation in there                                                      //
      /////////////////////////////////////////////////////////////////////////////////////////////////////
      memoryStorageAction.getDownloadURL(storageInput);

      return inputStream;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void writeTmpFileAndOpen(InputStream inputStream, String suffix) throws IOException
   {
      // LocalMacDevUtils.mayOpenFiles = true;
      if(LocalMacDevUtils.mayOpenFiles)
      {
         inputStream.reset();

         File             tmpFile          = File.createTempFile(getClass().getName(), suffix, new File("/tmp/"));
         FileOutputStream fileOutputStream = new FileOutputStream(tmpFile);
         inputStream.transferTo(fileOutputStream);
         fileOutputStream.close();

         LocalMacDevUtils.openFile(tmpFile.getAbsolutePath());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord insertBasicSavedPivotReport(String label) throws QException
   {
      return new InsertAction().execute(new InsertInput(SavedReport.TABLE_NAME).withRecordEntity(new SavedReport()
         .withLabel(label)
         .withTableName(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withColumnsJson(JsonUtils.toJson(new ReportColumns()
            .withColumn("id")
            .withColumn("firstName")
            .withColumn("lastName")
            .withColumn("cost")
            .withColumn("birthDate")
            .withColumn("homeStateId")))
         .withQueryFilterJson(JsonUtils.toJson(new QQueryFilter()))
         .withPivotTableJson(JsonUtils.toJson(new PivotTableDefinition()
            .withRow(new PivotTableGroupBy()
               .withFieldName("homeStateId"))
            .withRow(new PivotTableGroupBy()
               .withFieldName("firstName"))
            .withValue(new PivotTableValue()
               .withFieldName("id")
               .withFunction(PivotTableFunction.COUNT))
            .withValue(new PivotTableValue()
               .withFieldName("cost")
               .withFunction(PivotTableFunction.SUM))
            .withValue(new PivotTableValue()
               .withFieldName("birthDate")
               .withFunction(PivotTableFunction.MIN))
            .withValue(new PivotTableValue()
               .withFieldName("birthDate")
               .withFunction(PivotTableFunction.MAX))
         ))
      )).getRecords().get(0);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPivotXlsx() throws Exception
   {
      String           label            = "Test Pivot Report";
      QRecord          savedReport      = insertBasicSavedPivotReport(label);
      RunProcessOutput runProcessOutput = runRenderReportProcess(savedReport, ReportFormatPossibleValueEnum.XLSX);

      try(InputStream inputStream = getInputStream(runProcessOutput); XSSFWorkbook workbook = new XSSFWorkbook(inputStream))
      {
         assertEquals(2, workbook.getNumberOfSheets());
         XSSFSheet data = workbook.getSheet(label);
         assertEquals(7, data.getPhysicalNumberOfRows());
         assertEquals(6, data.getRow(0).getLastCellNum());
         assertEquals("Id", data.getRow(0).getCell(0).getStringCellValue());
         assertEquals("First Name", data.getRow(0).getCell(1).getStringCellValue());

         List<String> firstNames = List.of("Darin", "Darin", "Darin", "Trevor", "Trevor", "Kelly");
         List<String> lastNames = List.of("Jonson", "Jones", "Kelly", "Keller", "Kelkhoff", "Kelkhoff");
         List<String> birthDates = List.of("1980-01-31", "1980-01-31", "1979-12-30", "1980-01-07", "1980-02-15", "1980-03-20");
         for(int index = 0; index < 6; index++)
         {
            Row row = data.getRow(index + 1);
            assertEquals(index + 1, row.getCell(0).getNumericCellValue());
            assertEquals(firstNames.get(index), row.getCell(1).getStringCellValue());
            assertEquals(lastNames.get(index), row.getCell(2).getStringCellValue());
            assertEquals(index < 3 ? 0.5 : 3.5, row.getCell(3).getNumericCellValue());
            assertEquals(LocalDate.parse(birthDates.get(index)), row.getCell(4).getLocalDateTimeCellValue().toLocalDate());
            assertEquals(index < 5 ? "IL" : "MO", row.getCell(5).getStringCellValue());
         }

         XSSFSheet pivotSheet = workbook.getSheet("Pivot Table");
         assertEquals(1, pivotSheet.getPivotTables().size());
         XSSFPivotTable pivot = pivotSheet.getPivotTables().get(0);
         assertEquals(List.of(5, 1), pivot.getRowLabelColumns());
         assertEquals(label, pivot.getPivotCacheDefinition().getCTPivotCacheDefinition().getCacheSource().getWorksheetSource().getSheet());
         assertEquals("A1:F7", pivot.getPivotCacheDefinition().getCTPivotCacheDefinition().getCacheSource().getWorksheetSource().getRef());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPivotJson() throws Exception
   {
      String           label            = "Test Pivot Report JSON";
      QRecord          savedReport      = insertBasicSavedPivotReport(label);
      RunProcessOutput runProcessOutput = runRenderReportProcess(savedReport, ReportFormatPossibleValueEnum.JSON);

      InputStream inputStream = getInputStream(runProcessOutput);
      String      json        = StringUtils.join("\n", IOUtils.readLines(inputStream, StandardCharsets.UTF_8));
      printReport(json);

      JSONArray jsonArray = new JSONArray(json);
      assertEquals(2, jsonArray.length());

      JSONObject firstView = jsonArray.getJSONObject(0);
      assertEquals(label, firstView.getString("name"));
      JSONArray firstViewData = firstView.getJSONArray("data");
      assertEquals(6, firstViewData.length());
      assertThat(firstViewData.getJSONObject(0).toMap())
         .hasFieldOrPropertyWithValue("id", 1)
         .hasFieldOrPropertyWithValue("firstName", "Darin");

      JSONObject pivotView = jsonArray.getJSONObject(1);
      assertEquals("Pivot Table", pivotView.getString("name"));
      JSONArray pivotViewData = pivotView.getJSONArray("data");
      assertEquals(4, pivotViewData.length());
      assertThat(pivotViewData.getJSONObject(0).toMap())
         .hasFieldOrPropertyWithValue("homeState", "IL")
         .hasFieldOrPropertyWithValue("firstName", "Darin")
         .hasFieldOrPropertyWithValue("countOfId", 3)
         .hasFieldOrPropertyWithValue("sumOfCost", new BigDecimal("1.50"));
      assertThat(pivotViewData.getJSONObject(3).toMap())
         .hasFieldOrPropertyWithValue("homeState", "Totals")
         .hasFieldOrPropertyWithValue("countOfId", 6)
         .hasFieldOrPropertyWithValue("sumOfCost", new BigDecimal("12.00"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void printReport(String report)
   {
      // System.out.println(report);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPivotCSV() throws Exception
   {
      String           label            = "Test Pivot Report CSV";
      QRecord          savedReport      = insertBasicSavedPivotReport(label);
      RunProcessOutput runProcessOutput = runRenderReportProcess(savedReport, ReportFormatPossibleValueEnum.CSV);

      InputStream  inputStream = getInputStream(runProcessOutput);
      List<String> csv         = IOUtils.readLines(inputStream, StandardCharsets.UTF_8);
      System.out.println(csv);

      assertEquals("""
         "Home State","First Name","Count Of Id","Sum Of Cost","Min Of Birth Date","Max Of Birth Date\"""", csv.get(0));

      assertEquals("""
         "Totals","","6","12.00","1979-12-30","1980-03-20\"""", csv.get(4));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord insertSavedPivotReportWithAllFunctions(String label) throws QException
   {
      PivotTableDefinition pivotTableDefinition = new PivotTableDefinition()
         .withRow(new PivotTableGroupBy().withFieldName("firstName"));

      for(PivotTableFunction function : PivotTableFunction.values())
      {
         pivotTableDefinition.withValue(new PivotTableValue().withFieldName("cost").withFunction(function));
      }

      return new InsertAction().execute(new InsertInput(SavedReport.TABLE_NAME).withRecordEntity(new SavedReport()
         .withLabel(label)
         .withTableName(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withColumnsJson(JsonUtils.toJson(new ReportColumns()
            .withColumn("id")
            .withColumn("firstName")
            .withColumn("cost")))
         .withQueryFilterJson(JsonUtils.toJson(new QQueryFilter()))
         .withPivotTableJson(JsonUtils.toJson(pivotTableDefinition))
      )).getRecords().get(0);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPivotXlsxAllFunctions() throws Exception
   {
      String           label            = "Test Pivot Report";
      QRecord          savedReport      = insertSavedPivotReportWithAllFunctions(label);
      RunProcessOutput runProcessOutput = runRenderReportProcess(savedReport, ReportFormatPossibleValueEnum.XLSX);

      String serverFilePath = runProcessOutput.getValueString("serverFilePath");
      System.out.println(serverFilePath);

      InputStream inputStream = getInputStream(runProcessOutput);
      writeTmpFileAndOpen(inputStream, ".xlsx");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPivotCSVAllFunctions() throws Exception
   {
      String           label            = "Test Pivot Report CSV";
      QRecord          savedReport      = insertSavedPivotReportWithAllFunctions(label);
      RunProcessOutput runProcessOutput = runRenderReportProcess(savedReport, ReportFormatPossibleValueEnum.CSV);

      InputStream  inputStream = getInputStream(runProcessOutput);
      List<String> csv         = IOUtils.readLines(inputStream, StandardCharsets.UTF_8);
      System.out.println(csv);

      assertEquals("""
         "First Name","Average Of Cost","Count Of Cost","Count_nums Of Cost","Max Of Cost","Min Of Cost","Product Of Cost","Std_dev Of Cost","Std_devp Of Cost","Sum Of Cost","Var Of Cost","Varp Of Cost\"""", csv.get(0));

      assertEquals("""
         "Totals","2.0","6","6","3.50","0.50","5.359375000000","1.6432","1.5000","12.00","2.7000","2.2500\"""", csv.get(4));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static RunProcessOutput runRenderReportProcess(QRecord savedReport, ReportFormatPossibleValueEnum reportFormat) throws QException
   {
      RunProcessInput input = new RunProcessInput();
      input.setProcessName(RenderSavedReportMetaDataProducer.NAME);
      input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
      input.setCallback(QProcessCallbackFactory.forRecord(savedReport));
      input.addValue("reportFormat", reportFormat.getPossibleValueId());
      RunProcessOutput runProcessOutput = new RunProcessAction().execute(input);
      return runProcessOutput;
   }




}
