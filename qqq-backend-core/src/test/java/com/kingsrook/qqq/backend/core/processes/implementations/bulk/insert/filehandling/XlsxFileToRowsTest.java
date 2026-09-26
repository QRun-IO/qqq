/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.filehandling;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.Month;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.reporting.GenerateReportAction;
import com.kingsrook.qqq.backend.core.actions.reporting.GenerateReportActionTest;
import com.kingsrook.qqq.backend.core.actions.reporting.excel.fastexcel.ExcelFastexcelExportStreamer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportDestination;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormat;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadFileRow;
import org.junit.jupiter.api.Test;
import static com.kingsrook.qqq.backend.core.actions.reporting.GenerateReportActionTest.REPORT_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for XlsxFileToRows 
 *******************************************************************************/
class XlsxFileToRowsTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException, IOException
   {
      byte[] byteArray = writeExcelBytes();

      FileToRowsInterface fileToRowsInterface = new XlsxFileToRows();
      fileToRowsInterface.init(new ByteArrayInputStream(byteArray));

      BulkLoadFileRow headerRow = fileToRowsInterface.next();
      BulkLoadFileRow bodyRow   = fileToRowsInterface.next();

      assertEquals(new BulkLoadFileRow(new String[] { "Id", "First Name", "Last Name", "Birth Date" }, 1), headerRow);
      assertEquals(new BulkLoadFileRow(new Serializable[] { 1, "Darin", "Jonson", LocalDate.of(1980, Month.JANUARY, 31) }, 2), bodyRow);

      ///////////////////////////////////////////////////////////////////////////////////////
      // make sure there's at least a limit (less than 20) to how many more rows there are //
      ///////////////////////////////////////////////////////////////////////////////////////
      int otherRowCount = 0;
      while(fileToRowsInterface.hasNext() && otherRowCount < 20)
      {
         fileToRowsInterface.next();
         otherRowCount++;
      }
      assertFalse(fileToRowsInterface.hasNext());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static byte[] writeExcelBytes() throws QException, IOException
   {
      ReportFormat          format = ReportFormat.XLSX;
      ByteArrayOutputStream baos   = new ByteArrayOutputStream();

      QInstance qInstance = QContext.getQInstance();
      qInstance.addReport(GenerateReportActionTest.defineTableOnlyReport());
      GenerateReportActionTest.insertPersonRecords(qInstance);

      ReportInput reportInput = new ReportInput();
      reportInput.setReportName(REPORT_NAME);
      reportInput.setReportDestination(new ReportDestination().withReportFormat(format).withReportOutputStream(baos));
      reportInput.setInputValues(Map.of("startDate", LocalDate.of(1970, Month.MAY, 15), "endDate", LocalDate.now()));
      reportInput.setOverrideExportStreamerSupplier(ExcelFastexcelExportStreamer::new);
      new GenerateReportAction().execute(reportInput);

      byte[] byteArray = baos.toByteArray();
      // FileUtils.writeByteArrayToFile(new File("/tmp/xlsx.xlsx"), byteArray);
      return byteArray;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDateTimeFormats()
   {
      assertFormatDateAndOrDateTime(true, false, "dddd, m/d/yy at h:mm");
      assertFormatDateAndOrDateTime(true, false, "h PM, ddd mmm dd");
      assertFormatDateAndOrDateTime(true, false, "dd/mm/yyyy hh:mm");
      assertFormatDateAndOrDateTime(true, false, "yyyy-mm-dd hh:mm:ss.000");
      assertFormatDateAndOrDateTime(true, false, "hh:mm dd/mm/yyyy");

      assertFormatDateAndOrDateTime(false, true, "yyyy-mm-dd");
      assertFormatDateAndOrDateTime(false, true, "mmmm d \\[dddd\\]");
      assertFormatDateAndOrDateTime(false, true, "mmm dd, yyyy");
      assertFormatDateAndOrDateTime(false, true, "d-mmm");
      assertFormatDateAndOrDateTime(false, true, "dd.mm.yyyy");

      assertFormatDateAndOrDateTime(false, false, "yyyy");
      assertFormatDateAndOrDateTime(false, false, "mmm-yyyy");
      assertFormatDateAndOrDateTime(false, false, "hh");
      assertFormatDateAndOrDateTime(false, false, "hh:mm");
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private void assertFormatDateAndOrDateTime(boolean expectDateTime, boolean expectDate, String format)
   {
      if(XlsxFileToRows.isDateTimeFormat(format))
      {
         assertTrue(expectDateTime, format + " was considered a dateTime, but wasn't expected to.");
         assertFalse(expectDate, format + " was considered a dateTime, but was expected to be a date.");
      }
      else if(XlsxFileToRows.isDateFormat(format))
      {
         assertFalse(expectDateTime, format + " was considered a date, but was expected to be a dateTime.");
         assertTrue(expectDate, format + " was considered a date, but was expected to.");
      }
      else
      {
         assertFalse(expectDateTime, format + " was not considered a dateTime, but was expected to.");
         assertFalse(expectDate, format + " was considered a date, but was expected to.");
      }
   }

}