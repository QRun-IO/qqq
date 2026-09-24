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
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit tests for TsvExportStreamer — header writing, tab-delimited records,
 ** totals row, and title row ordering.
 *******************************************************************************/
class TsvExportStreamerTest extends BaseTest
{

   /*******************************************************************************
    ** Header row contains each column label separated by tabs.
    *******************************************************************************/
   @Test
   void testStart_withHeaderRow_writesTabDelimitedLabels() throws Exception
   {
      ByteArrayOutputStream out      = new ByteArrayOutputStream();
      TsvExportStreamer     streamer = new TsvExportStreamer();
      List<QFieldMetaData> fields   = List.of(
         new QFieldMetaData("id", QFieldType.INTEGER).withLabel("Id"),
         new QFieldMetaData("name", QFieldType.STRING).withLabel("Name")
      );

      streamer.start(buildExportInput(out, true, null), fields, "sheet1", null);

      String output = out.toString(StandardCharsets.UTF_8);
      assertThat(output).contains("Id");
      assertThat(output).contains("Name");
      assertThat(output).contains("\t");
   }



   /*******************************************************************************
    ** When includeHeaderRow is false, no header is written.
    *******************************************************************************/
   @Test
   void testStart_withoutHeaderRow_noHeaderWritten() throws Exception
   {
      ByteArrayOutputStream out      = new ByteArrayOutputStream();
      TsvExportStreamer     streamer = new TsvExportStreamer();
      List<QFieldMetaData> fields   = List.of(new QFieldMetaData("id", QFieldType.INTEGER).withLabel("Id"));

      streamer.start(buildExportInput(out, false, null), fields, "sheet1", null);

      assertThat(out.toString(StandardCharsets.UTF_8)).doesNotContain("Id");
   }



   /*******************************************************************************
    ** Title row appears before the header when set.
    *******************************************************************************/
   @Test
   void testStart_withTitleRow_titlePrecedesHeader() throws Exception
   {
      ByteArrayOutputStream out      = new ByteArrayOutputStream();
      TsvExportStreamer     streamer = new TsvExportStreamer();
      List<QFieldMetaData> fields   = List.of(new QFieldMetaData("id", QFieldType.INTEGER).withLabel("Id"));

      streamer.start(buildExportInput(out, true, "My TSV Title"), fields, "sheet1", null);

      String content   = out.toString(StandardCharsets.UTF_8);
      int    titlePos  = content.indexOf("My TSV Title");
      int    headerPos = content.indexOf("Id");
      assertThat(titlePos).isGreaterThanOrEqualTo(0);
      assertThat(titlePos).isLessThan(headerPos);
   }



   /*******************************************************************************
    ** addRecords writes each record as a line; values appear in output.
    *******************************************************************************/
   @Test
   void testAddRecords_writesRecordValues() throws Exception
   {
      ByteArrayOutputStream out      = new ByteArrayOutputStream();
      TsvExportStreamer     streamer = new TsvExportStreamer();
      List<QFieldMetaData> fields   = List.of(
         new QFieldMetaData("id", QFieldType.INTEGER).withLabel("Id"),
         new QFieldMetaData("name", QFieldType.STRING).withLabel("Name")
      );
      ExportInput exportInput = buildExportInputWithTable(out, false, null);

      streamer.start(exportInput, fields, "sheet1", null);
      streamer.addRecords(List.of(
         new QRecord().withValue("id", 1).withValue("name", "Alice"),
         new QRecord().withValue("id", 2).withValue("name", "Bob")
      ));

      String content = out.toString(StandardCharsets.UTF_8);
      assertThat(content).contains("Alice");
      assertThat(content).contains("Bob");
   }



   /*******************************************************************************
    ** addRecords with an empty list produces no output and does not throw.
    *******************************************************************************/
   @Test
   void testAddRecords_emptyList_noExceptionNoOutput() throws Exception
   {
      ByteArrayOutputStream out      = new ByteArrayOutputStream();
      TsvExportStreamer     streamer = new TsvExportStreamer();
      List<QFieldMetaData> fields   = List.of(new QFieldMetaData("id", QFieldType.INTEGER).withLabel("Id"));
      ExportInput          input    = buildExportInputWithTable(out, false, null);

      streamer.start(input, fields, "sheet1", null);
      streamer.addRecords(List.of());

      assertThat(out.toString(StandardCharsets.UTF_8)).isEmpty();
   }



   /*******************************************************************************
    ** addTotalsRow uses the same write path and produces output.
    *******************************************************************************/
   @Test
   void testAddTotalsRow_writesRow() throws Exception
   {
      ByteArrayOutputStream out      = new ByteArrayOutputStream();
      TsvExportStreamer     streamer = new TsvExportStreamer();
      List<QFieldMetaData> fields   = List.of(
         new QFieldMetaData("id", QFieldType.INTEGER).withLabel("Id"),
         new QFieldMetaData("total", QFieldType.INTEGER).withLabel("Total")
      );

      streamer.start(buildExportInputWithTable(out, false, null), fields, "sheet1", null);
      streamer.addTotalsRow(new QRecord().withValue("id", null).withValue("total", 42));

      assertThat(out.toString(StandardCharsets.UTF_8)).contains("42");
   }



   /*******************************************************************************
    ** finish() is a no-op and must not throw.
    *******************************************************************************/
   @Test
   void testFinish_noException() throws Exception
   {
      ByteArrayOutputStream out      = new ByteArrayOutputStream();
      TsvExportStreamer     streamer = new TsvExportStreamer();
      List<QFieldMetaData> fields   = List.of(new QFieldMetaData("id", QFieldType.INTEGER).withLabel("Id"));

      streamer.start(buildExportInputWithTable(out, false, null), fields, "sheet1", null);
      streamer.finish();
   }



   /*******************************************************************************
    ** Multiple columns have exactly (fieldCount - 1) tab characters on the header line.
    *******************************************************************************/
   @Test
   void testStart_threeFields_twoTabsInHeaderLine() throws Exception
   {
      ByteArrayOutputStream out      = new ByteArrayOutputStream();
      TsvExportStreamer     streamer = new TsvExportStreamer();
      List<QFieldMetaData> fields   = List.of(
         new QFieldMetaData("a", QFieldType.STRING).withLabel("Alpha"),
         new QFieldMetaData("b", QFieldType.STRING).withLabel("Beta"),
         new QFieldMetaData("c", QFieldType.STRING).withLabel("Gamma")
      );

      streamer.start(buildExportInput(out, true, null), fields, "sheet1", null);

      String firstLine = out.toString(StandardCharsets.UTF_8).lines().findFirst().orElse("");
      long   tabCount  = firstLine.chars().filter(ch -> ch == '\t').count();
      assertThat(tabCount).isEqualTo(2);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private ExportInput buildExportInput(ByteArrayOutputStream out, boolean includeHeader, String titleRow)
   {
      ExportInput exportInput = new ExportInput();
      exportInput.setIncludeHeaderRow(includeHeader);
      exportInput.setTitleRow(titleRow);
      exportInput.setReportDestination(new ReportDestination()
         .withReportFormat(ReportFormat.TSV)
         .withReportOutputStream(out));
      return exportInput;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private ExportInput buildExportInputWithTable(ByteArrayOutputStream out, boolean includeHeader, String titleRow)
   {
      ExportInput exportInput = buildExportInput(out, includeHeader, titleRow);
      exportInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      return exportInput;
   }

}
