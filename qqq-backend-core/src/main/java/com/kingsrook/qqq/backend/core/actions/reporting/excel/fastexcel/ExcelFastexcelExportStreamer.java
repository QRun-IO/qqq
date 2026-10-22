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

package com.kingsrook.qqq.backend.core.actions.reporting.excel.fastexcel;


import java.io.OutputStream;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.actions.reporting.ExportStreamerInterface;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QReportingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ExportInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DisplayFormat;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportView;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.ObjectUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import org.dhatim.fastexcel.StyleSetter;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;


/*******************************************************************************
 ** Excel export format implementation - built using fastexcel library
 *******************************************************************************/
public class ExcelFastexcelExportStreamer implements ExportStreamerInterface
{
   private static final QLogger LOG = QLogger.getLogger(ExcelFastexcelExportStreamer.class);

   private ExportInput          exportInput;
   private QTableMetaData       table;
   private List<QFieldMetaData> fields;
   private OutputStream         outputStream;

   private FastExcelStylerInterface fastExcelStylerInterface = new PlainFastExcelStyler();
   private Map<String, String>      excelCellFormats;

   private Workbook  workbook;
   private Worksheet worksheet;
   private int       row        = 0;
   private int       sheetCount = 0;



   /*******************************************************************************
    **
    *******************************************************************************/
   public ExcelFastexcelExportStreamer()
   {
   }



   /*******************************************************************************
    ** display formats is a map of field name to Excel format strings (e.g., $#,##0.00)
    *******************************************************************************/
   @Override
   public void setDisplayFormats(Map<String, String> displayFormats)
   {
      this.excelCellFormats = new HashMap<>();
      for(Map.Entry<String, String> entry : displayFormats.entrySet())
      {
         String excelFormat = DisplayFormat.getExcelFormat(entry.getValue());
         if(excelFormat != null)
         {
            excelCellFormats.put(entry.getKey(), excelFormat);
         }
      }
   }



   /*******************************************************************************
    ** Starts a new worksheet in the current workbook.  Can be called multiple times.
    *******************************************************************************/
   @Override
   public void start(ExportInput exportInput, List<QFieldMetaData> fields, String label, QReportView view) throws QReportingException
   {
      try
      {
         this.exportInput = exportInput;
         this.fields = fields;
         table = exportInput.getTable();
         outputStream = this.exportInput.getReportDestination().getReportOutputStream();
         this.row = 0;
         this.sheetCount++;

         /////////////////////////////////////////////////////////////////////////////////////////////////////
         // if this is the first call in here (e.g., the workbook hasn't been opened yet), then open it now //
         /////////////////////////////////////////////////////////////////////////////////////////////////////
         if(workbook == null)
         {
            String    appName = ObjectUtils.tryAndRequireNonNullElse(() -> QContext.getQInstance().getBranding().getAppName(), "QQQ");
            QInstance instance = QContext.getQInstance();
            if(instance != null && instance.getBranding() != null && instance.getBranding().getCompanyName() != null)
            {
               appName = instance.getBranding().getCompanyName();
            }
            workbook = new Workbook(outputStream, appName, null);
         }

         /////////////////////////////////////////////////////////////////////////////////////
         // if start is called a second time (e.g., and there's already an open worksheet), //
         // finish that sheet, before a new one is created.                                 //
         /////////////////////////////////////////////////////////////////////////////////////
         if(worksheet != null)
         {
            worksheet.finish();
         }

         worksheet = workbook.newWorksheet(Objects.requireNonNullElse(label, "Sheet" + sheetCount));

         writeTitleAndHeader();
      }
      catch(Exception e)
      {
         throw (new QReportingException("Error starting worksheet", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void writeTitleAndHeader() throws QReportingException
   {
      try
      {
         ///////////////
         // title row //
         ///////////////
         if(StringUtils.hasContent(exportInput.getTitleRow()))
         {
            worksheet.value(row, 0, exportInput.getTitleRow());
            worksheet.range(row, 0, row, fields.size() - 1).merge();

            StyleSetter titleStyle = worksheet.range(row, 0, row, fields.size() - 1).style();
            fastExcelStylerInterface.styleTitleRow(titleStyle);
            titleStyle.set();

            row++;
            worksheet.flush();
         }

         ////////////////
         // header row //
         ////////////////
         if(exportInput.getIncludeHeaderRow())
         {
            int col = 0;
            for(QFieldMetaData column : fields)
            {
               worksheet.value(row, col, column.getLabel());
               col++;
            }

            StyleSetter headerStyle = worksheet.range(row, 0, row, fields.size() - 1).style();
            fastExcelStylerInterface.styleHeaderRow(headerStyle);
            headerStyle.set();

            row++;
            worksheet.flush();
         }
      }
      catch(Exception e)
      {
         throw (new QReportingException("Error starting Excel report"));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void addRecords(List<QRecord> qRecords) throws QReportingException
   {
      LOG.info("Consuming [" + qRecords.size() + "] records from the pipe");

      try
      {
         for(QRecord qRecord : qRecords)
         {
            writeRecord(qRecord);

            row++;
            worksheet.flush(); // todo?  not at all?  or just sometimes?
         }
      }
      catch(Exception e)
      {
         LOG.error("Exception generating excel file", e);
         try
         {
            workbook.finish();
            outputStream.close();
         }
         finally
         {
            throw (new QReportingException("Error generating Excel report", e));
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void writeRecord(QRecord qRecord)
   {
      int col = 0;
      for(QFieldMetaData field : fields)
      {
         Serializable value = qRecord.getValue(field.getName());

         if(value != null)
         {
            if(value instanceof String s)
            {
               worksheet.value(row, col, s);
            }
            else if(value instanceof Number n)
            {
               worksheet.value(row, col, n);

               if(excelCellFormats != null)
               {
                  String format = excelCellFormats.get(field.getName());
                  if(format != null)
                  {
                     worksheet.style(row, col).format(format).set();
                  }
               }
            }
            else if(value instanceof Boolean b)
            {
               worksheet.value(row, col, b);
            }
            else if(value instanceof Date d)
            {
               worksheet.value(row, col, d);
               worksheet.style(row, col).format("yyyy-MM-dd").set();
            }
            else if(value instanceof LocalDate d)
            {
               worksheet.value(row, col, d);
               worksheet.style(row, col).format("yyyy-MM-dd").set();
            }
            else if(value instanceof LocalDateTime d)
            {
               worksheet.value(row, col, d);
               worksheet.style(row, col).format("yyyy-MM-dd H:mm:ss").set();
            }
            else if(value instanceof ZonedDateTime d)
            {
               worksheet.value(row, col, d);
               worksheet.style(row, col).format("yyyy-MM-dd H:mm:ss").set();
            }
            else if(value instanceof Instant i)
            {
               // todo - what would be a better zone to use here?
               worksheet.value(row, col, i.atZone(ZoneId.systemDefault()));
               worksheet.style(row, col).format("yyyy-MM-dd H:mm:ss").set();
            }
            else
            {
               worksheet.value(row, col, ValueUtils.getValueAsString(value));
            }
         }
         col++;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void addTotalsRow(QRecord record)
   {
      writeRecord(record);

      StyleSetter totalsRowStyle = worksheet.range(row, 0, row, fields.size() - 1).style();
      fastExcelStylerInterface.styleTotalsRow(totalsRowStyle);
      totalsRowStyle.set();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void finish() throws QReportingException
   {
      try
      {
         if(workbook != null)
         {
            workbook.finish();
         }
      }
      catch(Exception e)
      {
         throw (new QReportingException("Error finishing Excel report", e));
      }
   }

}
