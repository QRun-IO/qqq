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

package com.kingsrook.qqq.backend.module.rdbms.reporting;


import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.processes.QProcessCallbackFactory;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.reporting.GenerateReportAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportDestination;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormat;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormatPossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportInput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable.PivotTableDefinition;
import com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable.PivotTableFunction;
import com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable.PivotTableGroupBy;
import com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable.PivotTableValue;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportDataSource;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportField;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportView;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.ReportType;
import com.kingsrook.qqq.backend.core.model.savedreports.ReportColumns;
import com.kingsrook.qqq.backend.core.model.savedreports.SavedReport;
import com.kingsrook.qqq.backend.core.model.savedreports.SavedReportsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryStorageAction;
import com.kingsrook.qqq.backend.core.processes.implementations.savedreports.RenderSavedReportMetaDataProducer;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.LocalMacDevUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSActionTest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Do some tests on the qqq-backend-core GenerateReportAction, that are kinda
 ** hard to do in a backend that doesn't support joins, but that we can do in
 ** RDBMS.
 *******************************************************************************/
public class GenerateReportActionRDBMSTest extends RDBMSActionTest
{
   private static final String TEST_REPORT = "testReport";



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   public void beforeEach() throws Exception
   {
      super.primeTestDatabase();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTwoJoinsToSameTable() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();
      QReportMetaData report = new QReportMetaData()
         .withName(TEST_REPORT)
         .withDataSource(new QReportDataSource()
            .withSourceTable(TestUtils.TABLE_NAME_ORDER)
            .withQueryJoin(new QueryJoin(qInstance.getJoin("orderJoinBillToPerson")).withAlias("billToPerson").withType(QueryJoin.Type.LEFT).withSelect(true))
            .withQueryJoin(new QueryJoin(qInstance.getJoin("orderJoinShipToPerson")).withAlias("shipToPerson").withType(QueryJoin.Type.LEFT).withSelect(true))
         )
         .withView(new QReportView()
            .withType(ReportType.TABLE)
            .withColumn(new QReportField("id"))
            .withColumn(new QReportField("storeId").withLabel("Store Id"))
            .withColumn(new QReportField("storeName").withShowPossibleValueLabel(true).withSourceFieldName("storeId").withLabel("Store Name"))
            .withColumn(new QReportField("billToPerson.id"))
            .withColumn(new QReportField("billToPerson.firstName").withLabel("Bill To First Name"))
            .withColumn(new QReportField("billToPersonName").withShowPossibleValueLabel(true).withSourceFieldName("billToPersonId"))
            .withColumn(new QReportField("shipToPerson.id"))
            .withColumn(new QReportField("shipToPerson.firstName").withLabel("Ship To First Name"))
            .withColumn(new QReportField("shipToPersonName").withShowPossibleValueLabel(true).withSourceFieldName("billToPersonId"))
         );
      qInstance.addReport(report);
      reInitInstanceInContext(qInstance);

      String csv = runReport(qInstance);
      // System.out.println(csv);

      assertEquals("""
         "Id","Store Id","Store Name","Id","Bill To First Name","Bill To Person","Id","Ship To First Name","Bill To Person"
         "1","1","Q-Mart","1","Darin","Darin Kelkhoff","1","Darin","Darin Kelkhoff"
         "2","1","Q-Mart","1","Darin","Darin Kelkhoff","2","James","Darin Kelkhoff"
         "3","1","Q-Mart","2","James","James Maes","3","Tim","James Maes"
         "4","2","QQQ 'R' Us","4","Tyler","Tyler Samples","5","Garret","Tyler Samples"
         "5","2","QQQ 'R' Us","5","Garret","Garret Richardson","4","Tyler","Garret Richardson"
         "6","3","QDepot","5","Garret","Garret Richardson","","","Garret Richardson"
         "7","3","QDepot","","","","5","Garret",""
         "8","3","QDepot","","","","5","Garret",""
         """, csv);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTwoTablesWithSamePossibleValue() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();
      QReportMetaData report = new QReportMetaData()
         .withName(TEST_REPORT)
         .withDataSource(new QReportDataSource()
            .withSourceTable(TestUtils.TABLE_NAME_ORDER_LINE)
            .withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_ORDER).withSelect(true))
            .withQueryFilter(new QQueryFilter(new QFilterCriteria("storeId", QCriteriaOperator.NOT_EQUALS).withOtherFieldName("order.storeId")))
         )
         .withView(new QReportView()
            .withType(ReportType.TABLE)
            .withColumn(new QReportField("storeId").withLabel("Line Item Store Id"))
            .withColumn(new QReportField("storeName").withShowPossibleValueLabel(true).withSourceFieldName("storeId").withLabel("Line Item Store Name"))
            .withColumn(new QReportField("order.storeId").withLabel("Order Store Id"))
            .withColumn(new QReportField("order.storeName").withShowPossibleValueLabel(true).withSourceFieldName("order.storeId").withLabel("Order Store Name"))
         );
      qInstance.addReport(report);
      reInitInstanceInContext(qInstance);

      String csv = runReport(qInstance);
      // System.out.println(csv);

      assertEquals("""
         "Line Item Store Id","Line Item Store Name","Order Store Id","Order Store Name"
         "2","QQQ 'R' Us","1","Q-Mart"
         """, csv);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPossibleValueThroughAlias() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();
      QReportMetaData report = new QReportMetaData()
         .withName(TEST_REPORT)
         .withDataSource(new QReportDataSource()
            .withSourceTable(TestUtils.TABLE_NAME_ORDER_LINE)
            .withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_ITEM).withAlias("i").withSelect(true))
            .withQueryFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id")))
         )
         .withView(new QReportView()
            .withType(ReportType.TABLE)
            .withColumn(new QReportField("id").withLabel("Line Item Id"))
            .withColumn(new QReportField("sku").withLabel("Item SKU"))
            .withColumn(new QReportField("i.storeId").withLabel("Item Store Id"))
            .withColumn(new QReportField("i.storeName").withShowPossibleValueLabel(true).withSourceFieldName("i.storeId").withLabel("Item Store Name"))
         );
      qInstance.addReport(report);
      reInitInstanceInContext(qInstance);

      String csv = runReport(qInstance);
      System.out.println(csv);

      assertEquals("""
         "Line Item Id","Item SKU","Item Store Id","Item Store Name"
         "1","QM-1","1","Q-Mart"
         "2","QM-2","1","Q-Mart"
         "3","QM-3","1","Q-Mart"
         "4","QRU-1","2","QQQ 'R' Us"
         "5","QM-1","1","Q-Mart"
         "6","QRU-1","2","QQQ 'R' Us"
         "7","QRU-2","2","QQQ 'R' Us"
         "8","QRU-1","2","QQQ 'R' Us"
         "9","QD-1","3","QDepot"
         "10","QD-1","3","QDepot"
         "11","QD-1","3","QDepot"
         """, csv);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private RunProcessOutput runSavedReport(SavedReport savedReport, ReportFormatPossibleValueEnum reportFormat) throws Exception
   {
      savedReport.setLabel("Test Report");
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_STORE_ALL_ACCESS, true));

      if(QContext.getQInstance().getTable(SavedReport.TABLE_NAME) == null)
      {
         new SavedReportsMetaDataProvider().defineAll(QContext.getQInstance(), TestUtils.MEMORY_BACKEND_NAME, TestUtils.MEMORY_BACKEND_NAME, null);
      }

      QRecord savedReportRecord = new InsertAction().execute(new InsertInput(SavedReport.TABLE_NAME).withRecordEntity(savedReport)).getRecords().get(0);

      RunProcessInput input = new RunProcessInput();
      input.setProcessName(RenderSavedReportMetaDataProducer.NAME);
      input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
      input.setCallback(QProcessCallbackFactory.forRecord(savedReportRecord));
      input.addValue("reportFormat", reportFormat);
      RunProcessOutput runProcessOutput = new RunProcessAction().execute(input);
      return (runProcessOutput);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<String> runSavedReportForCSV(SavedReport savedReport) throws Exception
   {
      RunProcessOutput runProcessOutput = runSavedReport(savedReport, ReportFormatPossibleValueEnum.CSV);

      String      storageTableName = runProcessOutput.getValueString("storageTableName");
      String      storageReference = runProcessOutput.getValueString("storageReference");
      InputStream inputStream      = new MemoryStorageAction().getInputStream(new StorageInput(storageTableName).withReference(storageReference));

      return (IOUtils.readLines(inputStream, StandardCharsets.UTF_8));
   }



   /*******************************************************************************
    ** in here, by potentially ambiguous, we mean where there are possible joins
    ** between the order and orderInstructions tables.
    *******************************************************************************/
   @Test
   void testSavedReportWithPotentiallyAmbiguousExposedJoinSelections() throws Exception
   {
      List<String> lines = runSavedReportForCSV(new SavedReport()
         .withTableName(TestUtils.TABLE_NAME_ORDER)
         .withColumnsJson(JsonUtils.toJson(new ReportColumns()
            .withColumn("id")
            .withColumn("storeId")
            .withColumn("orderInstructions.instructions")))
         .withQueryFilterJson(JsonUtils.toJson(new QQueryFilter())));

      assertEquals("""
         "Id","Store","Order Instructions: Instructions"
         """.trim(), lines.get(0));
      assertEquals("""
         "1","Q-Mart","order 1 v2"
         """.trim(), lines.get(1));
   }



   /*******************************************************************************
    ** in here, by potentially ambiguous, we mean where there are possible joins
    ** between the order and orderInstructions tables.
    *******************************************************************************/
   @Test
   void testSavedReportWithPotentiallyAmbiguousExposedJoinSelectedAndOrdered() throws Exception
   {
      List<String> lines = runSavedReportForCSV(new SavedReport()
         .withTableName(TestUtils.TABLE_NAME_ORDER)
         .withColumnsJson(JsonUtils.toJson(new ReportColumns()
            .withColumn("id")
            .withColumn("storeId")
            .withColumn("orderInstructions.instructions")))
         .withQueryFilterJson(JsonUtils.toJson(new QQueryFilter()
            .withOrderBy(new QFilterOrderBy("orderInstructions.id", false))
         )));

      assertEquals("""
         "Id","Store","Order Instructions: Instructions"
         """.trim(), lines.get(0));
      assertEquals("""
         "8","QDepot","order 8 v1"
         """.trim(), lines.get(1));
   }



   /*******************************************************************************
    ** in here, by potentially ambiguous, we mean where there are possible joins
    ** between the order and orderInstructions tables.
    *******************************************************************************/
   @Test
   void testSavedReportWithPotentiallyAmbiguousExposedJoinCriteria() throws Exception
   {
      List<String> lines = runSavedReportForCSV(new SavedReport()
         .withTableName(TestUtils.TABLE_NAME_ORDER)
         .withColumnsJson(JsonUtils.toJson(new ReportColumns()
            .withColumn("id")
            .withColumn("storeId")))
         .withQueryFilterJson(JsonUtils.toJson(new QQueryFilter()
            .withCriteria(new QFilterCriteria("orderInstructions.instructions", QCriteriaOperator.CONTAINS, "v3"))
         )));

      assertEquals("""
         "Id","Store"
         """.trim(), lines.get(0));
      assertEquals("""
         "2","Q-Mart"
         """.trim(), lines.get(1));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSavedReportWithExposedJoinMultipleTablesAwaySelected() throws Exception
   {
      List<String> lines = runSavedReportForCSV(new SavedReport()
         .withTableName(TestUtils.TABLE_NAME_ORDER)
         .withColumnsJson(JsonUtils.toJson(new ReportColumns()
            .withColumn("id")
            .withColumn("storeId")
            .withColumn("item.description")))
         .withQueryFilterJson(JsonUtils.toJson(new QQueryFilter())));

      assertEquals("""
         "Id","Store","Item: Description"
         """.trim(), lines.get(0));
      assertEquals("""
         "1","Q-Mart","Q-Mart Item 1"
         """.trim(), lines.get(1));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSavedReportWithExposedJoinMultipleTablesAwayAsCriteria() throws Exception
   {
      List<String> lines = runSavedReportForCSV(new SavedReport()
         .withTableName(TestUtils.TABLE_NAME_ORDER)
         .withColumnsJson(JsonUtils.toJson(new ReportColumns()
            .withColumn("id")
            .withColumn("storeId")))
         .withQueryFilterJson(JsonUtils.toJson(new QQueryFilter()
            .withCriteria(new QFilterCriteria("item.description", QCriteriaOperator.CONTAINS, "Item 7"))
         )));

      assertEquals("""
         "Id","Store"
         """.trim(), lines.get(0));
      assertEquals("""
         "6","QDepot"
         """.trim(), lines.get(1));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSavedReportWithPivotsFromJoinTable() throws Exception
   {
      SavedReport savedReport = new SavedReport()
         .withTableName(TestUtils.TABLE_NAME_ORDER)
         .withColumnsJson(JsonUtils.toJson(new ReportColumns()
            .withColumn("id")
            .withColumn("item.storeId")
            .withColumn("item.description")))
         .withQueryFilterJson(JsonUtils.toJson(new QQueryFilter()))
         .withPivotTableJson(JsonUtils.toJson(new PivotTableDefinition()
            .withRow(new PivotTableGroupBy().withFieldName("item.storeId"))
            .withValue(new PivotTableValue().withFieldName("item.description").withFunction(PivotTableFunction.COUNT))));

      //////////////////////////////////////////////
      // make sure we can render xlsx w/o a crash //
      //////////////////////////////////////////////
      RunProcessOutput runProcessOutput = runSavedReport(savedReport, ReportFormatPossibleValueEnum.XLSX);
      String           storageTableName = runProcessOutput.getValueString("storageTableName");
      String           storageReference = runProcessOutput.getValueString("storageReference");
      InputStream      inputStream      = new MemoryStorageAction().getInputStream(new StorageInput(storageTableName).withReference(storageReference));

      String path = "/tmp/pivot.xlsx";
      inputStream.transferTo(new FileOutputStream(path));
      // LocalMacDevUtils.mayOpenFiles = true;
      LocalMacDevUtils.openFile(path);

      ///////////////////////////////////////////////////////
      // render as csv too - and assert about those values //
      ///////////////////////////////////////////////////////
      List<String> csv = runSavedReportForCSV(savedReport);
      System.out.println(StringUtils.join("\n", csv));
      assertEquals("""
         "Store","Count Of Item: Description\"""", csv.get(0));
      assertEquals("""
         "Q-Mart","4\"""", csv.get(1));
      assertEquals("""
         "Totals","11\"""", csv.get(4));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String runReport(QInstance qInstance) throws QException
   {
      ReportInput reportInput = new ReportInput();
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_STORE_ALL_ACCESS, true));
      reportInput.setReportName(TEST_REPORT);
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      reportInput.setReportDestination(new ReportDestination()
         .withReportFormat(ReportFormat.CSV)
         .withReportOutputStream(outputStream));

      new GenerateReportAction().execute(reportInput);
      return (outputStream.toString());
   }

}
