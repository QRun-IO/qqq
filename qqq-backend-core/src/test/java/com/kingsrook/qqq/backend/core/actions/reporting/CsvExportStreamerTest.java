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

package com.kingsrook.qqq.backend.core.actions.reporting;


import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ExportInput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportDestination;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormat;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit tests for CsvExportStreamer — header writing, record streaming,
 ** title row, and special-character escaping.
 *******************************************************************************/
class CsvExportStreamerTest extends BaseTest
{

   /*******************************************************************************
    ** Header row is written when includeHeaderRow is true.
    *******************************************************************************/
   @Test
   void testStart_withHeaderRow_writesColumnLabels() throws Exception
   {
      ByteArrayOutputStream out           = new ByteArrayOutputStream();
      CsvExportStreamer     streamer       = new CsvExportStreamer();
      ExportInput           exportInput   = buildExportInput(out, true, null);
      List<QFieldMetaData>  fields        = List.of(
         new QFieldMetaData("id", QFieldType.INTEGER).withLabel("Id"),
         new QFieldMetaData("name", QFieldType.STRING).withLabel("Name")
      );

      streamer.start(exportInput, fields, "label", null);

      String csv = out.toString(StandardCharsets.UTF_8);
      assertThat(csv).contains("\"Id\"");
      assertThat(csv).contains("\"Name\"");
   }



   /*******************************************************************************
    ** No header row is written when includeHeaderRow is false.
    *******************************************************************************/
   @Test
   void testStart_withoutHeaderRow_noHeaderLine() throws Exception
   {
      ByteArrayOutputStream out         = new ByteArrayOutputStream();
      CsvExportStreamer     streamer     = new CsvExportStreamer();
      ExportInput           exportInput = buildExportInput(out, false, null);
      List<QFieldMetaData>  fields      = List.of(new QFieldMetaData("id", QFieldType.INTEGER).withLabel("Id"));

      streamer.start(exportInput, fields, "label", null);

      String csv = out.toString(StandardCharsets.UTF_8);
      assertThat(csv).doesNotContain("\"Id\"");
   }



   /*******************************************************************************
    ** Title row is written before the header when titleRow is set.
    *******************************************************************************/
   @Test
   void testStart_withTitleRow_titleAppearsFirst() throws Exception
   {
      ByteArrayOutputStream out         = new ByteArrayOutputStream();
      CsvExportStreamer     streamer     = new CsvExportStreamer();
      ExportInput           exportInput = buildExportInput(out, true, "My Report Title");
      List<QFieldMetaData>  fields      = List.of(new QFieldMetaData("id", QFieldType.INTEGER).withLabel("Id"));

      streamer.start(exportInput, fields, "label", null);

      String csv = out.toString(StandardCharsets.UTF_8);
      int titlePos  = csv.indexOf("My Report Title");
      int headerPos = csv.indexOf("\"Id\"");
      assertThat(titlePos).isGreaterThanOrEqualTo(0);
      assertThat(titlePos).isLessThan(headerPos);
   }



   /*******************************************************************************
    ** addRecords writes each record as a CSV row after start.
    *******************************************************************************/
   @Test
   void testAddRecords_writesRows() throws Exception
   {
      ByteArrayOutputStream out         = new ByteArrayOutputStream();
      CsvExportStreamer     streamer     = new CsvExportStreamer();
      List<QFieldMetaData>  fields      = List.of(
         new QFieldMetaData("id", QFieldType.INTEGER).withLabel("Id"),
         new QFieldMetaData("name", QFieldType.STRING).withLabel("Name")
      );
      ExportInput exportInput = buildExportInputWithTable(out, true, null);

      streamer.start(exportInput, fields, "label", null);
      streamer.addRecords(List.of(
         new QRecord().withValue("id", 1).withValue("name", "Alice"),
         new QRecord().withValue("id", 2).withValue("name", "Bob")
      ));

      String csv = out.toString(StandardCharsets.UTF_8);
      assertThat(csv).contains("Alice");
      assertThat(csv).contains("Bob");
   }



   /*******************************************************************************
    ** addRecords with empty list writes nothing extra.
    *******************************************************************************/
   @Test
   void testAddRecords_emptyList_noException() throws Exception
   {
      ByteArrayOutputStream out         = new ByteArrayOutputStream();
      CsvExportStreamer     streamer     = new CsvExportStreamer();
      List<QFieldMetaData>  fields      = List.of(new QFieldMetaData("id", QFieldType.INTEGER).withLabel("Id"));
      ExportInput           exportInput = buildExportInputWithTable(out, false, null);

      streamer.start(exportInput, fields, "label", null);
      streamer.addRecords(List.of());

      String csv = out.toString(StandardCharsets.UTF_8);
      assertThat(csv).isEmpty();
   }



   /*******************************************************************************
    ** addTotalsRow delegates to the same write path and produces CSV output.
    *******************************************************************************/
   @Test
   void testAddTotalsRow_writesRow() throws Exception
   {
      ByteArrayOutputStream out         = new ByteArrayOutputStream();
      CsvExportStreamer     streamer     = new CsvExportStreamer();
      List<QFieldMetaData>  fields      = List.of(
         new QFieldMetaData("id", QFieldType.INTEGER).withLabel("Id"),
         new QFieldMetaData("total", QFieldType.INTEGER).withLabel("Total")
      );
      ExportInput exportInput = buildExportInputWithTable(out, false, null);

      streamer.start(exportInput, fields, "label", null);
      streamer.addTotalsRow(new QRecord().withValue("id", null).withValue("total", 999));

      String csv = out.toString(StandardCharsets.UTF_8);
      assertThat(csv).contains("999");
   }



   /*******************************************************************************
    ** finish() is a no-op and must not throw.
    *******************************************************************************/
   @Test
   void testFinish_noException() throws Exception
   {
      ByteArrayOutputStream out         = new ByteArrayOutputStream();
      CsvExportStreamer     streamer     = new CsvExportStreamer();
      List<QFieldMetaData>  fields      = List.of(new QFieldMetaData("id", QFieldType.INTEGER).withLabel("Id"));
      ExportInput           exportInput = buildExportInputWithTable(out, false, null);

      streamer.start(exportInput, fields, "label", null);
      streamer.finish();
   }



   /*******************************************************************************
    ** Multiple columns have their labels comma-separated in the header.
    *******************************************************************************/
   @Test
   void testStart_multipleFields_commaDelimitedHeader() throws Exception
   {
      ByteArrayOutputStream out         = new ByteArrayOutputStream();
      CsvExportStreamer     streamer     = new CsvExportStreamer();
      List<QFieldMetaData>  fields      = List.of(
         new QFieldMetaData("a", QFieldType.STRING).withLabel("Alpha"),
         new QFieldMetaData("b", QFieldType.STRING).withLabel("Beta"),
         new QFieldMetaData("c", QFieldType.STRING).withLabel("Gamma")
      );
      ExportInput exportInput = buildExportInput(out, true, null);

      streamer.start(exportInput, fields, "label", null);

      String firstLine = out.toString(StandardCharsets.UTF_8).lines().findFirst().orElse("");
      assertThat(firstLine).contains(",");
      assertThat(firstLine).contains("\"Alpha\"");
      assertThat(firstLine).contains("\"Beta\"");
      assertThat(firstLine).contains("\"Gamma\"");
   }



   /*******************************************************************************
    ** Builds a minimal ExportInput without a QTableMetaData (for header-only tests).
    *******************************************************************************/
   private ExportInput buildExportInput(ByteArrayOutputStream out, boolean includeHeader, String titleRow)
   {
      ExportInput exportInput = new ExportInput();
      exportInput.setIncludeHeaderRow(includeHeader);
      exportInput.setTitleRow(titleRow);
      exportInput.setReportDestination(new ReportDestination()
         .withReportFormat(ReportFormat.CSV)
         .withReportOutputStream(out));
      return exportInput;
   }



   /*******************************************************************************
    ** Builds an ExportInput wired to the TestUtils "person" table so record writes work.
    *******************************************************************************/
   private ExportInput buildExportInputWithTable(ByteArrayOutputStream out, boolean includeHeader, String titleRow)
   {
      ExportInput exportInput = buildExportInput(out, includeHeader, titleRow);
      exportInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      return exportInput;
   }

}
