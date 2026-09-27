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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.reporting.excel.TestExcelStyler;
import com.kingsrook.qqq.backend.core.actions.reporting.excel.fastexcel.ExcelFastexcelExportStreamer;
import com.kingsrook.qqq.backend.core.actions.reporting.excel.poi.BoldHeaderAndFooterPoiExcelStyler;
import com.kingsrook.qqq.backend.core.actions.reporting.excel.poi.ExcelPoiBasedStreamingExportStreamer;
import com.kingsrook.qqq.backend.core.actions.reporting.excel.poi.PoiExcelStylerInterface;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportDestination;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormat;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportInput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable.PivotTableDefinition;
import com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable.PivotTableFunction;
import com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable.PivotTableGroupBy;
import com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable.PivotTableValue;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DisplayFormat;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportDataSource;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportField;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportView;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.ReportType;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.testutils.PersonQRecord;
import com.kingsrook.qqq.backend.core.utils.LocalMacDevUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFPivotTable;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for GenerateReportAction
 *******************************************************************************/
public class GenerateReportActionTest extends BaseTest
{
   public static final String REPORT_NAME = "personReport1";



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   @AfterEach
   void beforeAndAfterEach()
   {
      ListOfMapsExportStreamer.reset();
      MemoryRecordStore.getInstance().reset();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSummary1() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      qInstance.addReport(definePersonShoesSummaryReport(true));
      insertPersonRecords(qInstance);
      runReport(qInstance, Map.of("startDate", LocalDate.of(1980, Month.JANUARY, 1), "endDate", LocalDate.of(1980, Month.DECEMBER, 31)));

      List<Map<String, String>>     list     = ListOfMapsExportStreamer.getList("summary");
      Iterator<Map<String, String>> iterator = list.iterator();
      Map<String, String>           row      = iterator.next();
      assertEquals(3, list.size());
      assertThat(list.get(0)).containsOnlyKeys("Last Name", "Report Start Date", "Report End Date", "Person Count", "Quantity", "Revenue", "Cost", "Profit", "Cost Per", "% Total", "Margins", "Revenue Per", "Margin Per");

      assertThat(row.get("Last Name")).isEqualTo("Keller");
      assertThat(row.get("Person Count")).isEqualTo("1");
      assertThat(row.get("Quantity")).isEqualTo("5");
      assertThat(row.get("Report Start Date")).isEqualTo("1980-01-01");
      assertThat(row.get("Report End Date")).isEqualTo("1980-12-31");
      assertThat(row.get("Cost")).isEqualTo("3.50");
      assertThat(row.get("Revenue")).isEqualTo("2.40");
      assertThat(row.get("Cost Per")).isEqualTo("0.70");
      assertThat(row.get("Revenue Per")).isEqualTo("0.48");
      assertThat(row.get("Margin Per")).isEqualTo("-0.22");

      row = iterator.next();
      assertThat(row.get("Last Name")).isEqualTo("Kelkhoff");
      assertThat(row.get("Person Count")).isEqualTo("2");
      assertThat(row.get("Quantity")).isEqualTo("13");
      assertThat(row.get("Cost")).isEqualTo("7.00"); // sum of the 2 Kelkhoff rows' costs
      assertThat(row.get("Revenue")).isEqualTo("8.40"); // sum of the 2 Kelkhoff rows' price
      assertThat(row.get("Cost Per")).isEqualTo("0.54"); // sum cost / quantity
      assertThat(row.get("Revenue Per")).isEqualTo("0.65"); // sum price (Revenue) / quantity
      assertThat(row.get("Margin Per")).isEqualTo("0.11"); // Revenue Per - Cost Per

      row = iterator.next();
      assertThat(row.get("Last Name")).isEqualTo("Totals");
      assertThat(row.get("Person Count")).isEqualTo("3");
      assertThat(row.get("Quantity")).isEqualTo("18");
      assertThat(row.get("Cost")).isEqualTo("10.50");
      assertThat(row.get("Cost Per")).startsWith("0.58");
      assertThat(row.get("Cost")).isEqualTo("10.50"); // sum of all 3 matching rows' costs
      assertThat(row.get("Revenue")).isEqualTo("10.80"); // sum of all 3 matching rows' price
      assertThat(row.get("Profit")).isEqualTo("0.30"); // Revenue - Cost
      assertThat(row.get("Margins")).isEqualTo("0.03"); // 100*Profit / Revenue
      assertThat(row.get("Cost Per")).isEqualTo("0.58"); // sum cost / quantity
      assertThat(row.get("Revenue Per")).isEqualTo("0.60"); // sum price (Revenue) / quantity
      assertThat(row.get("Margin Per")).isEqualTo("0.02"); // Revenue Per - Cost Per
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSummary2() throws QException
   {
      QInstance       qInstance = QContext.getQInstance();
      QReportMetaData report    = definePersonShoesSummaryReport(false);

      //////////////////////////////////////////////
      // change from the default to sort reversed //
      //////////////////////////////////////////////
      report.getViews().get(0).getOrderByFields().get(0).setIsAscending(false);
      qInstance.addReport(report);
      insertPersonRecords(qInstance);
      runReport(qInstance, Map.of("startDate", LocalDate.of(1980, Month.JANUARY, 1), "endDate", LocalDate.of(1980, Month.DECEMBER, 31)));

      List<Map<String, String>>     list     = ListOfMapsExportStreamer.getList("summary");
      Iterator<Map<String, String>> iterator = list.iterator();
      Map<String, String>           row      = iterator.next();
      assertEquals(2, list.size());

      assertThat(row.get("Last Name")).isEqualTo("Kelkhoff");
      assertThat(row.get("Quantity")).isEqualTo("13");

      row = iterator.next();
      assertThat(row.get("Last Name")).isEqualTo("Keller");
      assertThat(row.get("Quantity")).isEqualTo("5");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSummary3() throws QException
   {
      QInstance       qInstance = QContext.getQInstance();
      QReportMetaData report    = definePersonShoesSummaryReport(false);

      //////////////////////////////////////////////////////////////////////////////////////////////
      // remove the filters, change to sort by personCount (to get some ties), then sumPrice desc //
      // this also shows the behavior of a null value in an order by                              //
      //////////////////////////////////////////////////////////////////////////////////////////////
      report.getDataSources().get(0).getQueryFilter().setCriteria(null);
      report.getViews().get(0).setOrderByFields(List.of(new QFilterOrderBy("personCount"), new QFilterOrderBy("sumPrice", false)));
      qInstance.addReport(report);
      insertPersonRecords(qInstance);
      runReport(qInstance, Map.of("startDate", LocalDate.now(), "endDate", LocalDate.now()));

      List<Map<String, String>>     list     = ListOfMapsExportStreamer.getList("summary");
      Iterator<Map<String, String>> iterator = list.iterator();
      Map<String, String>           row      = iterator.next();

      assertEquals(5, list.size());
      assertThat(row.get("Last Name")).isEqualTo("Keller");
      assertThat(row.get("Person Count")).isEqualTo("1");
      assertThat(row.get("Revenue")).isEqualTo("2.40");

      row = iterator.next();
      assertThat(row.get("Last Name")).isEqualTo("Kelly");
      assertThat(row.get("Person Count")).isEqualTo("1");
      assertThat(row.get("Revenue")).isEqualTo("1.20");

      row = iterator.next();
      assertThat(row.get("Last Name")).isEqualTo("Jones");
      assertThat(row.get("Person Count")).isEqualTo("1");
      assertThat(row.get("Revenue")).isEqualTo("1.00");

      row = iterator.next();
      assertThat(row.get("Last Name")).isEqualTo("Jonson");
      assertThat(row.get("Person Count")).isEqualTo("1");
      assertThat(row.get("Revenue")).isNull();

      row = iterator.next();
      assertThat(row.get("Last Name")).isEqualTo("Kelkhoff");
      assertThat(row.get("Person Count")).isEqualTo("2");
      assertThat(row.get("Revenue")).isEqualTo("8.40");

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSummary4() throws QException
   {
      QInstance       qInstance = QContext.getQInstance();
      QReportMetaData report    = definePersonShoesSummaryReport(false);

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // remove the filter, change to have 2 pivot columns - homeStateId and lastName - we should get no roll-up like this. //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      report.getDataSources().get(0).getQueryFilter().setCriteria(null);
      report.getViews().get(0).setSummaryFields(List.of(
         "homeStateId",
         "lastName"
      ));
      qInstance.addReport(report);
      insertPersonRecords(qInstance);
      runReport(qInstance, Map.of("startDate", LocalDate.now(), "endDate", LocalDate.now()));

      List<Map<String, String>>     list     = ListOfMapsExportStreamer.getList("summary");
      Iterator<Map<String, String>> iterator = list.iterator();
      Map<String, String>           row      = iterator.next();
      assertEquals(6, list.size());

      assertThat(row.get("Home State")).isEqualTo("IL");
      assertThat(row.get("Last Name")).isEqualTo("Jonson");
      assertThat(row.get("Quantity")).isNull();

      row = iterator.next();
      assertThat(row.get("Home State")).isEqualTo("IL");
      assertThat(row.get("Last Name")).isEqualTo("Jones");
      assertThat(row.get("Quantity")).isEqualTo("3");

      row = iterator.next();
      assertThat(row.get("Home State")).isEqualTo("IL");
      assertThat(row.get("Last Name")).isEqualTo("Kelly");
      assertThat(row.get("Quantity")).isEqualTo("4");

      row = iterator.next();
      assertThat(row.get("Home State")).isEqualTo("IL");
      assertThat(row.get("Last Name")).isEqualTo("Keller");
      assertThat(row.get("Quantity")).isEqualTo("5");

      row = iterator.next();
      assertThat(row.get("Home State")).isEqualTo("IL");
      assertThat(row.get("Last Name")).isEqualTo("Kelkhoff");
      assertThat(row.get("Quantity")).isEqualTo("6");

      row = iterator.next();
      assertThat(row.get("Home State")).isEqualTo("MO");
      assertThat(row.get("Last Name")).isEqualTo("Kelkhoff");
      assertThat(row.get("Quantity")).isEqualTo("7");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSummary5() throws QException
   {
      QInstance       qInstance = QContext.getQInstance();
      QReportMetaData report    = definePersonShoesSummaryReport(false);

      /////////////////////////////////////////////////////////////////////////////////////
      // remove the filter, and just pivot on homeStateId - should aggregate differently //
      /////////////////////////////////////////////////////////////////////////////////////
      report.getDataSources().get(0).getQueryFilter().setCriteria(null);
      report.getViews().get(0).setSummaryFields(List.of("homeStateId"));
      qInstance.addReport(report);
      insertPersonRecords(qInstance);
      runReport(qInstance, Map.of("startDate", LocalDate.now(), "endDate", LocalDate.now()));

      List<Map<String, String>>     list     = ListOfMapsExportStreamer.getList("summary");
      Iterator<Map<String, String>> iterator = list.iterator();
      Map<String, String>           row      = iterator.next();
      assertEquals(2, list.size());
      assertThat(row.get("Home State")).isEqualTo("MO");
      assertThat(row.get("Last Name")).isNull();
      assertThat(row.get("Quantity")).isEqualTo("7");

      row = iterator.next();
      assertThat(row.get("Home State")).isEqualTo("IL");
      assertThat(row.get("Last Name")).isNull();
      assertThat(row.get("Quantity")).isEqualTo("18");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String runToString(ReportFormat reportFormat, String reportName) throws Exception
   {
      return (runToString(reportFormat, reportName, Map.of("startDate", LocalDate.of(1970, Month.MAY, 15), "endDate", LocalDate.now())));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String runToString(ReportFormat reportFormat, String reportName, Map<String, Serializable> inputValues) throws Exception
   {
      String name = "/tmp/report." + reportFormat.getExtension();
      try(FileOutputStream fileOutputStream = new FileOutputStream(name))
      {
         ReportInput reportInput = new ReportInput();
         reportInput.setReportName(reportName);
         reportInput.setReportDestination(new ReportDestination().withReportFormat(reportFormat).withReportOutputStream(fileOutputStream));
         reportInput.setInputValues(inputValues);
         new GenerateReportAction().execute(reportInput);
         System.out.println("Wrote File: " + name);
         return (FileUtils.readFileToString(new File(name), StandardCharsets.UTF_8));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void runSummaryToXlsx() throws Exception
   {
      ReportFormat format = ReportFormat.XLSX;
      String       name   = "/tmp/report-" + format + ".xlsx";
      try(FileOutputStream fileOutputStream = new FileOutputStream(name))
      {
         QInstance qInstance = QContext.getQInstance();
         qInstance.addReport(definePersonShoesSummaryReport(true));
         insertPersonRecords(qInstance);

         ReportInput reportInput = new ReportInput();
         reportInput.setReportName(REPORT_NAME);
         reportInput.setReportDestination(new ReportDestination().withReportFormat(format).withReportOutputStream(fileOutputStream));
         reportInput.setInputValues(Map.of("startDate", LocalDate.of(1970, Month.MAY, 15), "endDate", LocalDate.now()));
         new GenerateReportAction().execute(reportInput);
         System.out.println("Wrote File: " + name);

         LocalMacDevUtils.openFile(name);
      }
   }



   /*******************************************************************************
    ** Keep some test coverage on the fastexcel library (as long as we keep it around)
    *******************************************************************************/
   @Test
   void runTableToXlsxFastexcel() throws Exception
   {
      ReportFormat format = ReportFormat.XLSX;
      String       name   = "/tmp/report-fastexcel.xlsx";
      try(FileOutputStream fileOutputStream = new FileOutputStream(name))
      {
         QInstance qInstance = QContext.getQInstance();
         qInstance.addReport(defineTableOnlyReport());
         insertPersonRecords(qInstance);

         ReportInput reportInput = new ReportInput();
         reportInput.setReportName(REPORT_NAME);
         reportInput.setReportDestination(new ReportDestination().withReportFormat(format).withReportOutputStream(fileOutputStream));
         reportInput.setInputValues(Map.of("startDate", LocalDate.of(1970, Month.MAY, 15), "endDate", LocalDate.now()));
         reportInput.setOverrideExportStreamerSupplier(ExcelFastexcelExportStreamer::new);
         new GenerateReportAction().execute(reportInput);
         System.out.println("Wrote File: " + name);

         LocalMacDevUtils.openFile(name);
      }
   }



   /*******************************************************************************
    ** Imagine if we used the boldHeaderAndFooter styler
    *******************************************************************************/
   @Test
   void runTableToXlsxWithOverrideStyles() throws Exception
   {
      ReportFormat format = ReportFormat.XLSX;
      String       name   = "/tmp/report-fastexcel.xlsx";
      try(FileOutputStream fileOutputStream = new FileOutputStream(name))
      {
         QInstance       qInstance      = QContext.getQInstance();
         QReportMetaData reportMetaData = defineTableOnlyReport();
         reportMetaData.getViews().get(0).withTitleFormat("My Title");

         qInstance.addReport(reportMetaData);
         insertPersonRecords(qInstance);

         ReportInput reportInput = new ReportInput();
         reportInput.setReportName(REPORT_NAME);
         reportInput.setReportDestination(new ReportDestination().withReportFormat(format).withReportOutputStream(fileOutputStream));
         reportInput.setInputValues(Map.of("startDate", LocalDate.of(1970, Month.MAY, 15), "endDate", LocalDate.now()));

         reportInput.setOverrideExportStreamerSupplier(() -> new ExcelPoiBasedStreamingExportStreamer()
         {
            @Override
            protected PoiExcelStylerInterface getStylerInterface()
            {
               return new BoldHeaderAndFooterPoiExcelStyler();
            }
         });

         new GenerateReportAction().execute(reportInput);
         System.out.println("Wrote File: " + name);

         LocalMacDevUtils.openFile(name);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void runTableToXlsx() throws Exception
   {
      ReportFormat format = ReportFormat.XLSX;
      String       name   = "/tmp/report-" + format + ".xlsx";
      try(FileOutputStream fileOutputStream = new FileOutputStream(name))
      {
         QInstance qInstance = QContext.getQInstance();
         qInstance.addReport(defineTableOnlyReport());
         insertPersonRecords(qInstance);

         ReportInput reportInput = new ReportInput();
         reportInput.setReportName(REPORT_NAME);
         reportInput.setReportDestination(new ReportDestination().withReportFormat(format).withReportOutputStream(fileOutputStream));
         reportInput.setInputValues(Map.of("startDate", LocalDate.of(1970, Month.MAY, 15), "endDate", LocalDate.now()));
         new GenerateReportAction().execute(reportInput);
         System.out.println("Wrote File: " + name);

         LocalMacDevUtils.openFile(name);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void runXlsxWithStyleCustomizer() throws Exception
   {
      ReportFormat format = ReportFormat.XLSX;
      String       name   = "/tmp/report-customized.xlsx";
      try(FileOutputStream fileOutputStream = new FileOutputStream(name))
      {
         QInstance qInstance = QContext.getQInstance();
         qInstance.addReport(defineTableOnlyReport());
         insertPersonRecords(qInstance);

         ReportInput reportInput = new ReportInput();
         reportInput.setReportName(REPORT_NAME);
         reportInput.setReportDestination(new ReportDestination().withReportFormat(format).withReportOutputStream(fileOutputStream));
         reportInput.setInputValues(Map.of("startDate", LocalDate.of(1970, Month.MAY, 15), "endDate", LocalDate.now()));
         reportInput.setExportStyleCustomizer(new QCodeReference(TestExcelStyler.class));
         new GenerateReportAction().execute(reportInput);
         System.out.println("Wrote File: " + name);

         LocalMacDevUtils.openFile(name);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void runPivotToXlsx() throws Exception
   {
      String name = "/tmp/pivot-test.xlsx";
      try(FileOutputStream fileOutputStream = new FileOutputStream(name))
      {
         QInstance qInstance = QContext.getQInstance();
         qInstance.addReport(definePivotReport());
         insertPersonRecords(qInstance);

         ReportInput reportInput = new ReportInput();
         reportInput.setReportName(REPORT_NAME);
         reportInput.setReportDestination(new ReportDestination().withReportFormat(ReportFormat.XLSX).withReportOutputStream(fileOutputStream));
         reportInput.setInputValues(Map.of("startDate", LocalDate.of(1970, Month.MAY, 15), "endDate", LocalDate.now()));
         new GenerateReportAction().execute(reportInput);
         System.out.println("Wrote File: " + name);

         LocalMacDevUtils.openFile(name, "/Applications/Numbers.app");
      }

      ///////////////////////////////////////////////////////////
      // read the file we wrote, and assert about its contents //
      ///////////////////////////////////////////////////////////
      FileInputStream file     = new FileInputStream(name);
      XSSFWorkbook    workbook = new XSSFWorkbook(file);

      XSSFSheet            sheet           = workbook.getSheetAt(1);
      List<XSSFPivotTable> pivotTables     = sheet.getPivotTables();
      XSSFPivotTable       xssfPivotTable  = pivotTables.get(0);
      List<Integer>        rowLabelColumns = xssfPivotTable.getRowLabelColumns();
      List<Integer>        colLabelColumns = xssfPivotTable.getColLabelColumns();
      Sheet                dataSheet       = xssfPivotTable.getDataSheet();
      Sheet                parentSheet     = xssfPivotTable.getParentSheet();
      System.out.println();

      Map<Integer, List<Object>> data = new HashMap<>();
      int                        i    = 0;
      for(Row row : sheet)
      {
         data.put(i, new ArrayList<>());

         for(Cell cell : row)
         {
            data.get(i).add(switch(cell.getCellType())
            {
               case _NONE -> "<_NONE>";
               case NUMERIC -> cell.getNumericCellValue();
               case STRING -> cell.getStringCellValue();
               case FORMULA -> cell.getCellFormula();
               case BLANK -> "<BLANK>";
               case BOOLEAN -> cell.getBooleanCellValue();
               case ERROR -> cell.getErrorCellValue();
            });
         }
         i++;
      }

      System.out.println(data);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void runReport(QInstance qInstance, Map<String, Serializable> inputValues) throws QException
   {
      ReportInput reportInput = new ReportInput();
      reportInput.setReportName(REPORT_NAME);
      reportInput.setReportDestination(new ReportDestination().withReportFormat(ReportFormat.LIST_OF_MAPS).withReportOutputStream(new ByteArrayOutputStream()));
      reportInput.setInputValues(inputValues);
      new GenerateReportAction().execute(reportInput);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void insertPersonRecords(QInstance qInstance) throws QException
   {
      TestUtils.insertRecords(qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY), List.of(
         new PersonQRecord().withFirstName("Darin").withLastName("Jonson").withBirthDate(LocalDate.of(1980, Month.JANUARY, 31)).withNoOfShoes(null).withHomeStateId(1).withPrice(null).withCost(new BigDecimal("0.50")), // wrong last initial
         new PersonQRecord().withFirstName("Darin").withLastName("Jones").withBirthDate(LocalDate.of(1980, Month.JANUARY, 31)).withNoOfShoes(3).withHomeStateId(1).withPrice(new BigDecimal("1.00")).withCost(new BigDecimal("0.50")), // wrong last initial
         new PersonQRecord().withFirstName("Darin").withLastName("Kelly").withBirthDate(LocalDate.of(1979, Month.DECEMBER, 30)).withNoOfShoes(4).withHomeStateId(1).withPrice(new BigDecimal("1.20")).withCost(new BigDecimal("0.50")), // bad birthdate
         new PersonQRecord().withFirstName("Trevor").withLastName("Keller").withBirthDate(LocalDate.of(1980, Month.JANUARY, 7)).withNoOfShoes(5).withHomeStateId(1).withPrice(new BigDecimal("2.40")).withCost(new BigDecimal("3.50")),
         new PersonQRecord().withFirstName("Trevor").withLastName("Kelkhoff").withBirthDate(LocalDate.of(1980, Month.FEBRUARY, 15)).withNoOfShoes(6).withHomeStateId(1).withPrice(new BigDecimal("3.60")).withCost(new BigDecimal("3.50")),
         new PersonQRecord().withFirstName("Kelly").withLastName("Kelkhoff").withBirthDate(LocalDate.of(1980, Month.MARCH, 20)).withNoOfShoes(7).withHomeStateId(2).withPrice(new BigDecimal("4.80")).withCost(new BigDecimal("3.50"))
      ));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QReportMetaData definePersonShoesSummaryReport(boolean includeTotalRow)
   {
      return new QReportMetaData()
         .withName(REPORT_NAME)
         .withDataSources(List.of(
            new QReportDataSource()
               .withName("persons")
               .withSourceTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
               .withQueryFilter(new QQueryFilter()
                  .withCriteria(new QFilterCriteria("lastName", QCriteriaOperator.STARTS_WITH, List.of("K")))
                  .withCriteria(new QFilterCriteria("birthDate", QCriteriaOperator.BETWEEN, List.of("${input.startDate}", "${input.endDate}")))
               )
         ))
         .withInputFields(List.of(
            new QFieldMetaData("startDate", QFieldType.DATE_TIME),
            new QFieldMetaData("endDate", QFieldType.DATE_TIME)
         ))
         .withViews(List.of(
            new QReportView()
               .withName("summary")
               .withLabel("summary")
               .withDataSourceName("persons")
               .withType(ReportType.SUMMARY)
               .withSummaryFields(List.of("lastName"))
               .withIncludeTotalRow(includeTotalRow)
               .withTitleFormat("Number of shoes - people born between %s and %s - pivot on LastName, sort by Quantity, Revenue DESC")
               .withTitleFields(List.of("${input.startDate}", "${input.endDate}"))
               .withOrderByFields(List.of(new QFilterOrderBy("shoeCount"), new QFilterOrderBy("sumPrice", false)))
               .withColumns(List.of(
                  new QReportField().withName("reportStartDate").withLabel("Report Start Date").withFormula("${input.startDate}"),
                  new QReportField().withName("reportEndDate").withLabel("Report End Date").withFormula("${input.endDate}"),
                  new QReportField().withName("personCount").withLabel("Person Count").withFormula("${pivot.count.id}").withDisplayFormat(DisplayFormat.COMMAS),
                  new QReportField().withName("shoeCount").withLabel("Quantity").withFormula("${pivot.sum.noOfShoes}").withDisplayFormat(DisplayFormat.COMMAS),
                  // new QReportField().withName("percentOfTotal").withLabel("% Total").withFormula("=MULTIPLY(100,DIVIDE(${pivot.sum.noOfShoes},${total.sum.noOfShoes}))").withDisplayFormat(DisplayFormat.PERCENT_POINT2),
                  new QReportField().withName("percentOfTotal").withLabel("% Total").withFormula("=DIVIDE(${pivot.sum.noOfShoes},${total.sum.noOfShoes})").withDisplayFormat(DisplayFormat.PERCENT_POINT2),
                  new QReportField().withName("sumCost").withLabel("Cost").withFormula("${pivot.sum.cost}").withDisplayFormat(DisplayFormat.CURRENCY),
                  new QReportField().withName("sumPrice").withLabel("Revenue").withFormula("${pivot.sum.price}").withDisplayFormat(DisplayFormat.CURRENCY),
                  new QReportField().withName("profit").withLabel("Profit").withFormula("=MINUS(${pivot.sum.price},${pivot.sum.cost})").withDisplayFormat(DisplayFormat.CURRENCY),
                  // new QReportField().withName("margin").withLabel("Margins").withFormula("=SCALE(MULTIPLY(100,DIVIDE(MINUS(${pivot.sum.price},${pivot.sum.cost}),${pivot.sum.price})),0)").withDisplayFormat(DisplayFormat.PERCENT),
                  new QReportField().withName("margin").withLabel("Margins").withFormula("=SCALE(DIVIDE(MINUS(${pivot.sum.price},${pivot.sum.cost}),${pivot.sum.price}),2)").withDisplayFormat(DisplayFormat.PERCENT),
                  new QReportField().withName("costPerShoe").withLabel("Cost Per").withFormula("=DIVIDE_SCALE(${pivot.sum.cost},${pivot.sum.noOfShoes},2)").withDisplayFormat(DisplayFormat.CURRENCY),
                  new QReportField().withName("revenuePerShoe").withLabel("Revenue Per").withFormula("=DIVIDE_SCALE(${pivot.sum.price},${pivot.sum.noOfShoes},2)").withDisplayFormat(DisplayFormat.CURRENCY),
                  new QReportField().withName("marginPer").withLabel("Margin Per").withFormula("=MINUS(DIVIDE_SCALE(${pivot.sum.price},${pivot.sum.noOfShoes},2),DIVIDE_SCALE(${pivot.sum.cost},${pivot.sum.noOfShoes},2))").withDisplayFormat(DisplayFormat.CURRENCY)
               ))
         ));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableOnlyReport() throws QException
   {
      QInstance       qInstance = QContext.getQInstance();
      QReportMetaData report    = defineTableOnlyReport();
      qInstance.addReport(report);

      insertPersonRecords(qInstance);
      runReport(qInstance, Map.of("startDate", LocalDate.of(1980, Month.JANUARY, 1)));

      List<Map<String, String>>     list     = ListOfMapsExportStreamer.getList("Table 1");
      Iterator<Map<String, String>> iterator = list.iterator();
      Map<String, String>           row      = iterator.next();
      assertEquals(5, list.size());
      assertThat(row).containsOnlyKeys("Id", "First Name", "Last Name", "Birth Date");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QReportMetaData defineTableOnlyReport()
   {
      QReportMetaData report = new QReportMetaData()
         .withName(REPORT_NAME)
         .withDataSources(List.of(
            new QReportDataSource()
               .withName("persons")
               .withSourceTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
               .withQueryFilter(new QQueryFilter()
                  .withCriteria(new QFilterCriteria("birthDate", QCriteriaOperator.GREATER_THAN, List.of("${input.startDate}"))))))

         .withInputFields(List.of(
            new QFieldMetaData("startDate", QFieldType.DATE_TIME)))

         .withViews(List.of(
            new QReportView()
               .withName("table1")
               .withLabel("Table 1")
               .withDataSourceName("persons")
               .withType(ReportType.TABLE)
               .withColumns(List.of(
                  new QReportField().withName("id"),
                  new QReportField().withName("firstName"),
                  new QReportField().withName("lastName"),
                  new QReportField().withName("birthDate")
               ))));

      return report;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QReportMetaData definePivotReport()
   {
      QReportMetaData report = new QReportMetaData()
         .withName(REPORT_NAME)
         .withDataSources(List.of(
            new QReportDataSource()
               .withName("persons")
               .withSourceTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
               .withQueryFilter(new QQueryFilter()
                  .withCriteria(new QFilterCriteria("birthDate", QCriteriaOperator.GREATER_THAN, List.of("${input.startDate}"))))))

         .withInputFields(List.of(
            new QFieldMetaData("startDate", QFieldType.DATE_TIME)))

         .withViews(List.of(

            new QReportView()
               .withName("table1")
               .withLabel("Table 1")
               .withDataSourceName("persons")
               .withType(ReportType.TABLE)
               .withColumns(List.of(
                  new QReportField().withName("id"),
                  new QReportField().withName("firstName"),
                  new QReportField().withName("lastName"),
                  new QReportField().withName("homeStateId")
               )),

            new QReportView()
               .withName("pivotTable1")
               .withLabel("My Pivot Table")

               .withType(ReportType.PIVOT)
               .withPivotTableSourceViewName("table1")
               .withPivotTableDefinition(new PivotTableDefinition()
                  .withRow(new PivotTableGroupBy().withFieldName("homeStateId"))
                  // .withRow(new PivotTableGroupBy().withFieldName("lastName"))
                  // .withColumn(new PivotTableGroupBy().withFieldName("firstName"))
                  .withValue(new PivotTableValue().withFunction(PivotTableFunction.COUNT).withFieldName("id")))
         ));

      return report;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTwoTableViewsOneDataSourceReport() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      defineTwoViewsOneDataSourceReport(qInstance);

      insertPersonRecords(qInstance);
      runReport(qInstance, Map.of("startDate", LocalDate.of(1980, Month.JANUARY, 1)));

      List<Map<String, String>>     list     = ListOfMapsExportStreamer.getList("Table 1");
      Iterator<Map<String, String>> iterator = list.iterator();
      Map<String, String>           row      = iterator.next();
      assertEquals(5, list.size());
      assertThat(row).containsOnlyKeys("Id", "First Name", "Last Name");

      list = ListOfMapsExportStreamer.getList("Table 2");
      iterator = list.iterator();
      row = iterator.next();
      assertEquals(5, list.size());
      assertThat(row).containsOnlyKeys("Birth Date");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOneTableViewsOneDataSourceJsonReport() throws Exception
   {
      QInstance       qInstance = QContext.getQInstance();
      QReportMetaData report    = defineTableOnlyReport();
      qInstance.addReport(report);

      insertPersonRecords(qInstance);
      String json = runToString(ReportFormat.JSON, report.getName());
      // System.out.println(json);

      /////////////////////////////////////////////////////////////////////////////////
      // for a one-view report, we should just have an array of the report's records //
      /////////////////////////////////////////////////////////////////////////////////
      JSONArray jsonArray = new JSONArray(json);
      assertEquals(6, jsonArray.length());
      assertThat(jsonArray.getJSONObject(0).toMap())
         .hasFieldOrPropertyWithValue("id", 1)
         .hasFieldOrPropertyWithValue("firstName", "Darin")
         .hasFieldOrPropertyWithValue("lastName", "Jonson");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTwoTableViewsOneDataSourceJsonReport() throws Exception
   {
      QInstance       qInstance = QContext.getQInstance();
      QReportMetaData report    = defineTwoViewsOneDataSourceReport(qInstance);

      insertPersonRecords(qInstance);
      String json = runToString(ReportFormat.JSON, report.getName());
      // System.out.println(json);

      /////////////////////////////////////////////////////////////////////////////////
      // for a multi-view report, we should have an array with the views as elements //
      /////////////////////////////////////////////////////////////////////////////////
      JSONArray jsonArray = new JSONArray(json);
      assertEquals(2, jsonArray.length());

      JSONObject firstView = jsonArray.getJSONObject(0);
      assertEquals("Table 1", firstView.getString("name"));
      JSONArray firstViewData = firstView.getJSONArray("data");
      assertEquals(6, firstViewData.length());
      assertThat(firstViewData.getJSONObject(0).toMap())
         .hasFieldOrPropertyWithValue("id", 1)
         .hasFieldOrPropertyWithValue("firstName", "Darin")
         .hasFieldOrPropertyWithValue("lastName", "Jonson");

      JSONObject secondView = jsonArray.getJSONObject(1);
      assertEquals("Table 2", secondView.getString("name"));
      JSONArray secondViewData = secondView.getJSONArray("data");
      assertEquals(6, secondViewData.length());
      assertThat(secondViewData.getJSONObject(0).toMap())
         .hasFieldOrPropertyWithValue("birthDate", "1980-01-31");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableViewsAndSummaryViewJsonReport() throws Exception
   {
      QInstance       qInstance = QContext.getQInstance();
      QReportMetaData report    = defineSimplePersonTableAndSummaryByFirstNameReport();
      qInstance.addReport(report);

      insertPersonRecords(qInstance);
      String json = runToString(ReportFormat.JSON, report.getName());
      System.out.println(json);

      /////////////////////////////////////////////////////////////////////////////////
      // for a multi-view report, we should have an array with the views as elements //
      /////////////////////////////////////////////////////////////////////////////////
      JSONArray jsonArray = new JSONArray(json);
      assertEquals(2, jsonArray.length());

      JSONObject firstView = jsonArray.getJSONObject(0);
      assertEquals("Table 1", firstView.getString("name"));
      JSONArray firstViewData = firstView.getJSONArray("data");
      assertEquals(6, firstViewData.length());
      assertThat(firstViewData.getJSONObject(0).toMap())
         .hasFieldOrPropertyWithValue("id", 1)
         .hasFieldOrPropertyWithValue("firstName", "Darin")
         .hasFieldOrPropertyWithValue("lastName", "Jonson");

      JSONObject secondView = jsonArray.getJSONObject(1);
      assertEquals("Summary", secondView.getString("name"));
      JSONArray secondViewData = secondView.getJSONArray("data");
      assertEquals(4, secondViewData.length());
      assertThat(secondViewData.getJSONObject(0).toMap())
         .hasFieldOrPropertyWithValue("firstName", "Darin")
         .hasFieldOrPropertyWithValue("personCount", 3);
      assertThat(secondViewData.getJSONObject(3).toMap())
         .hasFieldOrPropertyWithValue("firstName", "Totals")
         .hasFieldOrPropertyWithValue("personCount", 6);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QReportMetaData defineSimplePersonTableAndSummaryByFirstNameReport()
   {
      return new QReportMetaData()
         .withName(REPORT_NAME)
         .withDataSources(List.of(
            new QReportDataSource()
               .withName("persons")
               .withSourceTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
               .withQueryFilter(new QQueryFilter())
         ))
         .withViews(List.of(
            new QReportView()
               .withName("table1")
               .withLabel("Table 1")
               .withDataSourceName("persons")
               .withType(ReportType.TABLE)
               .withColumns(List.of(
                  new QReportField().withName("id"),
                  new QReportField().withName("firstName"),
                  new QReportField().withName("lastName"),
                  new QReportField().withName("homeStateId")
               )),
            new QReportView()
               .withName("summary")
               .withLabel("Summary")
               .withDataSourceName("persons")
               .withType(ReportType.SUMMARY)
               .withSummaryFields(List.of("firstName"))
               .withIncludeTotalRow(true)
               .withOrderByFields(List.of(new QFilterOrderBy("personCount", false)))
               .withColumns(List.of(
                  new QReportField().withName("personCount").withLabel("Person Count").withFormula("${pivot.count.id}").withDisplayFormat(DisplayFormat.COMMAS)
               ))
         ));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QReportMetaData defineTwoViewsOneDataSourceReport(QInstance qInstance)
   {
      QReportMetaData report = new QReportMetaData()
         .withName(REPORT_NAME)
         .withDataSources(List.of(
            new QReportDataSource()
               .withName("persons")
               .withSourceTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
               .withQueryFilter(new QQueryFilter()
                  .withCriteria(new QFilterCriteria("birthDate", QCriteriaOperator.GREATER_THAN, List.of("${input.startDate}")))
               )
         ))
         .withInputFields(List.of(
            new QFieldMetaData("startDate", QFieldType.DATE_TIME)
         ))
         .withViews(List.of(
            new QReportView()
               .withName("table1")
               .withLabel("Table 1")
               .withDataSourceName("persons")
               .withType(ReportType.TABLE)
               .withColumns(List.of(
                  new QReportField().withName("id"),
                  new QReportField().withName("firstName"),
                  new QReportField().withName("lastName")
               )),
            new QReportView()
               .withName("table2")
               .withLabel("Table 2")
               .withDataSourceName("persons")
               .withType(ReportType.TABLE)
               .withColumns(List.of(
                  new QReportField().withName("birthDate")
               ))
         ));

      qInstance.addReport(report);
      return (report);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testReportWithPossibleValueColumns() throws QException
   {
      QInstance qInstance = QContext.getQInstance();

      insertPersonRecords(qInstance);

      ReportInput reportInput = new ReportInput();
      reportInput.setReportName(TestUtils.REPORT_NAME_PERSON_SIMPLE);
      reportInput.setReportDestination(new ReportDestination().withReportFormat(ReportFormat.LIST_OF_MAPS).withReportOutputStream(new ByteArrayOutputStream()));
      new GenerateReportAction().execute(reportInput);

      List<Map<String, String>>     list     = ListOfMapsExportStreamer.getList("Simple Report");
      Iterator<Map<String, String>> iterator = list.iterator();
      Map<String, String>           row      = iterator.next();
      assertThat(row).containsKeys("Id", "First Name", "Last Name", "Home State Id", "Home State Name");

      row = iterator.next();
      assertThat(row.get("Home State Id")).isEqualTo("1");
      assertThat(row.get("Home State Name")).isEqualTo("IL");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFilterMissingValue() throws Exception
   {
      //////////////////////////////////////////////////////////////////////////////////////////////////////
      // define a report in the instance, with an input field - then we'll run it w/o supplying the value //
      //////////////////////////////////////////////////////////////////////////////////////////////////////
      QReportMetaData report = new QReportMetaData()
         .withName(REPORT_NAME)
         .withDataSources(List.of(
            new QReportDataSource()
               .withName("persons")
               .withSourceTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
               .withQueryFilter(new QQueryFilter()
                  .withCriteria(new QFilterCriteria("birthDate", QCriteriaOperator.GREATER_THAN, List.of("${input.startDate}"))))))
         .withViews(List.of(
            new QReportView()
               .withName("table1")
               .withLabel("Table 1")
               .withDataSourceName("persons")
               .withType(ReportType.TABLE)
               .withColumns(List.of(new QReportField().withName("id")))
         ));

      QInstance qInstance = QContext.getQInstance();
      qInstance.addReport(report);

      //////////////////////////////////////////////////////////////////////////////////////////////////
      // the report should run, but with the filter removed, so it should find all (6) person records //
      //////////////////////////////////////////////////////////////////////////////////////////////////
      insertPersonRecords(qInstance);
      String json = runToString(ReportFormat.JSON, report.getName(), Collections.emptyMap());
      JSONArray reportJsonArray = new JSONArray(json);
      assertEquals(6, reportJsonArray.length());

      /////////////////////////////////////////////////////////////////////////////////////////////
      // re-run now, pretending to be a report that wasn't defined in meta-data, but instead was //
      // ah-hoc-style, in which case, a missing input is defined as, should throw exception.     //
      /////////////////////////////////////////////////////////////////////////////////////////////
      ReportInput reportInput = new ReportInput();
      report.setName(null);
      reportInput.setReportMetaData(report);
      reportInput.setReportDestination(new ReportDestination().withReportFormat(ReportFormat.JSON).withReportOutputStream(new ByteArrayOutputStream()));
      assertThatThrownBy(() -> new GenerateReportAction().execute(reportInput))
         .hasMessageContaining("Missing value for criteria on field: birthDate");
   }

}