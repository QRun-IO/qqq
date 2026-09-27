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

package com.kingsrook.sampleapp;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import com.kingsrook.qqq.backend.core.actions.reporting.GenerateReportAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QFormulaException;
import com.kingsrook.qqq.backend.core.exceptions.QInstanceValidationException;
import com.kingsrook.qqq.backend.core.exceptions.QReportingException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportDestination;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormat;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportInput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable.PivotTableDefinition;
import com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable.PivotTableFunction;
import com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable.PivotTableGroupBy;
import com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable.PivotTableValue;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportDataSource;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportField;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportView;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.ReportType;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractTransformStep;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.apache.poi.xssf.usermodel.XSSFPivotTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Native report views use canonical H2 rows and readable output documents.
 *******************************************************************************/
class SampleReportViewContractTest
{
   /*******************************************************************************
    ** 
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      QSession session = new QSession();
      session.setUser(new QUser());
      session.setPermissions(Set.of());
      QContext.init(SampleMetaDataProvider.defineTestInstance(), session);
   }



   /*******************************************************************************
    ** 
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      QContext.clear();
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    ** POI verifies pivot structure; Excel calculates totals on refresh.
    *******************************************************************************/
   @Test
   void testNativePivotUsesCanonicalRows() throws Exception
   {
      try(XSSFWorkbook workbook = workbook(pivotReport()))
      {
         assertEquals(2, workbook.getNumberOfSheets());
         assertEquals(6, workbook.getSheet("People").getPhysicalNumberOfRows());
         assertEquals("Avery", workbook.getSheet("People").getRow(1).getCell(0).getStringCellValue());
         XSSFPivotTable pivot = workbook.getSheet("Pivot").getPivotTables().get(0);
         assertEquals(List.of(1), pivot.getRowLabelColumns());
         assertEquals(List.of(0), pivot.getColLabelColumns());
         assertEquals("People", pivot.getPivotCacheDefinition().getCTPivotCacheDefinition().getCacheSource().getWorksheetSource().getSheet());
         assertEquals("A1:C6", pivot.getPivotCacheDefinition().getCTPivotCacheDefinition().getCacheSource().getWorksheetSource().getRef());
         assertTrue(pivot.getPivotCacheDefinition().getCTPivotCacheDefinition().getRefreshOnLoad());
      }
   }



   /*******************************************************************************
    ** 
    *******************************************************************************/
   @Test
   void testEmptyPivotRetainsHeaderSource() throws Exception
   {
      QReportMetaData report = pivotReport();
      report.getDataSources().get(0).setQueryFilter(new QQueryFilter().withCriteria(new QFilterCriteria("id", QCriteriaOperator.EQUALS, -1)));
      try(XSSFWorkbook workbook = workbook(report))
      {
         assertEquals(1, workbook.getSheet("People").getPhysicalNumberOfRows());
         XSSFPivotTable pivot = workbook.getSheet("Pivot").getPivotTables().get(0);
         assertEquals("A1:C1", pivot.getPivotCacheDefinition().getCTPivotCacheDefinition().getCacheSource().getWorksheetSource().getRef());
      }
   }



   /*******************************************************************************
    ** 
    *******************************************************************************/
   @Test
   void testJsonOmitsNativePivotView() throws Exception
   {
      JSONArray views = json(pivotReport());
      assertEquals(1, views.length());
      assertEquals(5, views.getJSONObject(0).getJSONArray("data").length());
   }



   /*******************************************************************************
    ** 
    *******************************************************************************/
   @Test
   void testStaticSupplierTable() throws Exception
   {
      QReportMetaData report = new QReportMetaData().withName("static")
         .withDataSources(List.of(new QReportDataSource().withName("static").withStaticDataSupplier(new QCodeReference(StaticRows.class))))
         .withViews(List.of(new QReportView().withName("static").withDataSourceName("static").withType(ReportType.TABLE)
            .withColumns(List.of(new QReportField("column0").withLabel("Name").withType(QFieldType.STRING).withIsVirtual(true),
               new QReportField("column1").withLabel("Quantity").withType(QFieldType.INTEGER).withIsVirtual(true)))));
      JSONArray rows = json(report);
      assertEquals(2, rows.length());
      assertEquals("Owned A", rows.getJSONObject(0).getString("name"));
      assertEquals(3, rows.getJSONObject(1).getInt("quantity"));
   }



   /*******************************************************************************
    ** 
    *******************************************************************************/
   @Test
   void testInputFilterUsesCanonicalRows() throws Exception
   {
      QReportMetaData report = tableReport();
      report.getDataSources().get(0).getQueryFilter().withCriteria(new QFilterCriteria("id", QCriteriaOperator.GREATER_THAN, "${input.minimumId}"));
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      ReportInput input = input(report, ReportFormat.JSON, bytes);
      input.setInputValues(Map.of("minimumId", 3));
      assertEquals(2, new GenerateReportAction().execute(input).getTotalRecordCount());
      JSONArray rows = new JSONArray(bytes.toString(StandardCharsets.UTF_8));
      assertEquals("Drew", rows.getJSONObject(0).getString("firstName"));
      assertEquals("Morgan", rows.getJSONObject(1).getString("firstName"));
   }



   /*******************************************************************************
    ** 
    *******************************************************************************/
   @Test
   void testSummaryFormulasAndZeroDenominator() throws Exception
   {
      JSONArray rows = json(summaryReport());
      assertEquals(1, rows.length());
      JSONObject row = rows.getJSONObject(0);
      assertEquals(3.0, row.getDouble("mean"), 0.000001);
      assertEquals(6.0, row.getDouble("doubleMean"), 0.000001);
      assertTrue(row.isNull("undefined"));
   }



   /*******************************************************************************
    ** 
    *******************************************************************************/
   @Test
   void testBadFormulaFailsWithoutClosingDestination()
   {
      QReportMetaData report = summaryReport();
      report.getViews().get(0).getColumns().get(0).setFormula("=UNKNOWN(1)");
      DestinationProbe bytes = new DestinationProbe();
      assertThrows(QFormulaException.class, () -> new GenerateReportAction().execute(input(report, ReportFormat.JSON, bytes)));
      assertEquals(0, bytes.closes);
   }



   /*******************************************************************************
    ** 
    *******************************************************************************/
   @Test
   void testEmptySummaryReturnsNoRows() throws Exception
   {
      QReportMetaData report = summaryReport();
      report.getDataSources().get(0).setQueryFilter(new QQueryFilter().withCriteria(new QFilterCriteria("id", QCriteriaOperator.EQUALS, -1)));
      assertEquals(0, json(report).length());
   }



   /*******************************************************************************
    ** 
    *******************************************************************************/
   @Test
   void testMissingPivotViewFailsBeforeOutput()
   {
      QReportMetaData report = pivotReport();
      report.getViews().get(1).setPivotTableSourceViewName("missing");
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      assertThrows(QReportingException.class, () -> new GenerateReportAction().execute(input(report, ReportFormat.XLSX, bytes)));
      assertEquals(0, bytes.size());
   }



   /*******************************************************************************
    ** Delimited file formats render the source table and omit native pivots.
    *******************************************************************************/
   @Test
   void testDelimitedFormatsOmitNativePivot() throws Exception
   {
      for(ReportFormat format : List.of(ReportFormat.CSV, ReportFormat.TSV))
      {
         ByteArrayOutputStream table = new ByteArrayOutputStream();
         ByteArrayOutputStream pivot = new ByteArrayOutputStream();
         new GenerateReportAction().execute(input(tableReport(), format, table));
         new GenerateReportAction().execute(input(pivotReport(), format, pivot));
         assertArrayEquals(table.toByteArray(), pivot.toByteArray());
         assertTrue(pivot.toString(StandardCharsets.UTF_8).contains("Morgan"));
      }
   }



   /*******************************************************************************
    ** 
    *******************************************************************************/
   @Test
   void testVarianceSourceUsesItsOwnRows() throws Exception
   {
      QReportMetaData report = tableReport();
      report.getDataSources().get(0).setQueryFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.LESS_THAN_OR_EQUALS, 3)));
      report.setDataSources(List.of(report.getDataSources().get(0), new QReportDataSource().withName("prior").withSourceTable("person")
         .withQueryFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.LESS_THAN_OR_EQUALS, 2)))));
      report.setViews(List.of(new QReportView().withName("comparison").withType(ReportType.SUMMARY).withDataSourceName("people")
         .withVarianceDataSourceName("prior").withSummaryFields(List.of("lastName")).withIncludeTotalRow(false)
         .withColumns(List.of(new QReportField("current").withLabel("Current").withFormula("=MULTIPLY(${pivot.sum.id},1)"),
            new QReportField("prior").withLabel("Prior").withFormula("=MULTIPLY(${variance.sum.id},1)"),
            new QReportField("ratio").withLabel("Ratio").withFormula("=DIVIDE(${pivot.sum.id},${variance.sum.id})")))));
      JSONArray rows = json(report);
      assertEquals(1, rows.length());
      assertEquals(6.0, rows.getJSONObject(0).getDouble("current"), 0.000001);
      assertEquals(3.0, rows.getJSONObject(0).getDouble("prior"), 0.000001);
      assertEquals(2.0, rows.getJSONObject(0).getDouble("ratio"), 0.000001);
   }



   /*******************************************************************************
    ** A table transform changes exported values while the sample stays intact.
    *******************************************************************************/
   @Test
   void testCustomRecordTransformChangesOnlyOutput() throws Exception
   {
      QReportMetaData report = tableReport();
      report.getViews().get(0).setRecordTransformStep(new QCodeReference(PrefixNames.class));
      JSONArray transformed = json(report);
      assertEquals(List.of("Sample: Avery", "Sample: Blair", "Sample: Casey", "Sample: Drew", "Sample: Morgan"),
         transformed.toList().stream().map(value -> ((Map<?, ?>) value).get("firstName")).toList());
      assertEquals("Avery", json(tableReport()).getJSONObject(0).getString("firstName"));
   }



   /*******************************************************************************
    ** Broken report source references fail during instance validation.
    *******************************************************************************/
   @Test
   void testMissingSourceMetadataIsRejected() throws Exception
   {
      QInstance missingTable = SampleMetaDataProvider.defineTestInstance();
      QReportMetaData report = tableReport();
      report.getDataSources().get(0).setSourceTable("missingSampleTable");
      missingTable.addReport(report);
      assertThrows(QInstanceValidationException.class, () -> new QInstanceValidator().validate(missingTable));
      QInstance missingSource = SampleMetaDataProvider.defineTestInstance();
      report = tableReport();
      report.getViews().get(0).setDataSourceName("missingSampleSource");
      missingSource.addReport(report);
      assertThrows(QInstanceValidationException.class, () -> new QInstanceValidator().validate(missingSource));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QReportMetaData tableReport()
   {
      return new QReportMetaData().withName("ownedViews")
         .withDataSources(List.of(new QReportDataSource().withName("people").withSourceTable("person")
            .withQueryFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id")))))
         .withViews(List.of(new QReportView().withName("people").withLabel("People").withType(ReportType.TABLE).withDataSourceName("people")
            .withColumns(List.of(new QReportField("firstName"), new QReportField("lastName"), new QReportField("id")))));
   }



   /*******************************************************************************
    ** 
    *******************************************************************************/
   private QReportMetaData pivotReport()
   {
      QReportMetaData report = tableReport();
      report.setViews(List.of(report.getViews().get(0), new QReportView().withName("pivot").withLabel("Pivot").withType(ReportType.PIVOT)
         .withPivotTableSourceViewName("people").withPivotTableDefinition(new PivotTableDefinition()
            .withRow(new PivotTableGroupBy().withFieldName("lastName"))
            .withColumn(new PivotTableGroupBy().withFieldName("firstName"))
            .withValue(new PivotTableValue().withFieldName("id").withFunction(PivotTableFunction.SUM)))));
      return report;
   }



   /*******************************************************************************
    ** 
    *******************************************************************************/
   private QReportMetaData summaryReport()
   {
      QReportMetaData report = tableReport();
      report.setViews(List.of(new QReportView().withName("summary").withType(ReportType.SUMMARY).withDataSourceName("people")
         .withSummaryFields(List.of("lastName")).withIncludeTotalRow(false)
         .withColumns(List.of(new QReportField("mean").withLabel("Mean").withFormula("=DIVIDE(${pivot.sum.id},${pivot.count.id})"),
            new QReportField("doubleMean").withLabel("Double Mean").withFormula("=MULTIPLY(${thisRow.mean},2)"),
            new QReportField("undefined").withLabel("Undefined").withFormula("=DIVIDE(${pivot.count.id},0)")))));
      return report;
   }



   /*******************************************************************************
    ** 
    *******************************************************************************/
   private ReportInput input(QReportMetaData report, ReportFormat format, ByteArrayOutputStream bytes)
   {
      ReportInput input = new ReportInput();
      input.setInputSource(QInputSource.USER);
      input.setReportMetaData(report);
      input.setReportDestination(new ReportDestination().withReportFormat(format).withReportOutputStream(bytes));
      return input;
   }



   /*******************************************************************************
    ** 
    *******************************************************************************/
   private XSSFWorkbook workbook(QReportMetaData report) throws Exception
   {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      new GenerateReportAction().execute(input(report, ReportFormat.XLSX, bytes));
      return new XSSFWorkbook(new ByteArrayInputStream(bytes.toByteArray()));
   }



   /*******************************************************************************
    ** 
    *******************************************************************************/
   private JSONArray json(QReportMetaData report) throws Exception
   {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      new GenerateReportAction().execute(input(report, ReportFormat.JSON, bytes));
      return new JSONArray(bytes.toString(StandardCharsets.UTF_8));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class StaticRows implements Supplier<List<List<Serializable>>>
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<List<Serializable>> get()
      {
         return List.of(List.of("Owned A", 2), List.of("Owned B", 3));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class PrefixNames extends AbstractTransformStep
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output)
      {
         input.getRecords().forEach(record -> record.setValue("firstName", "Sample: " + record.getValueString("firstName")));
         output.setRecords(input.getRecords());
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput output, boolean isForResultScreen)
      {
         return new ArrayList<>();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static class DestinationProbe extends ByteArrayOutputStream
   {
      private int closes;

      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void close()
      {
         closes++;
      }
   }
}
