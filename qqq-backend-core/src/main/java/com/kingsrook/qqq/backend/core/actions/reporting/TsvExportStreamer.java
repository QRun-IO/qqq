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


import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import com.kingsrook.qqq.backend.core.adapters.QRecordToTsvAdapter;
import com.kingsrook.qqq.backend.core.exceptions.QReportingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ExportInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportView;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** TSV (Tab Separated Values) export format straemer implementation
 *******************************************************************************/
public class TsvExportStreamer implements ExportStreamerInterface
{
   private static final QLogger LOG = QLogger.getLogger(TsvExportStreamer.class);

   private final QRecordToTsvAdapter qRecordToTsvAdapter;

   private ExportInput          exportInput;
   private QTableMetaData       table;
   private List<QFieldMetaData> fields;
   private OutputStream         outputStream;



   /*******************************************************************************
    **
    *******************************************************************************/
   public TsvExportStreamer()
   {
      qRecordToTsvAdapter = new QRecordToTsvAdapter();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void start(ExportInput exportInput, List<QFieldMetaData> fields, String label, QReportView view) throws QReportingException
   {
      this.exportInput = exportInput;
      this.fields = fields;
      table = exportInput.getTable();
      outputStream = this.exportInput.getReportDestination().getReportOutputStream();

      writeTitleAndHeader();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void writeTitleAndHeader() throws QReportingException
   {
      try
      {
         if(StringUtils.hasContent(exportInput.getTitleRow()))
         {
            outputStream.write((exportInput.getTitleRow() + "\n").getBytes(StandardCharsets.UTF_8));
         }

         if(exportInput.getIncludeHeaderRow())
         {
            int col = 0;
            for(QFieldMetaData column : fields)
            {
               if(col++ > 0)
               {
                  outputStream.write('\t');
               }
               outputStream.write((column.getLabel()).getBytes(StandardCharsets.UTF_8));
            }
            outputStream.write('\n');
         }

         outputStream.flush();
      }
      catch(Exception e)
      {
         throw (new QReportingException("Error starting TSV report", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void addRecords(List<QRecord> qRecords) throws QReportingException
   {
      LOG.info("Consuming [" + qRecords.size() + "] records from the pipe");

      for(QRecord qRecord : qRecords)
      {
         writeRecord(qRecord);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void writeRecord(QRecord qRecord) throws QReportingException
   {
      try
      {
         String tsv = qRecordToTsvAdapter.recordToTsv(table, qRecord, fields);
         outputStream.write(tsv.getBytes(StandardCharsets.UTF_8));
         outputStream.flush(); // todo - less often?
      }
      catch(Exception e)
      {
         throw (new QReportingException("Error writing TSV report", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void addTotalsRow(QRecord record) throws QReportingException
   {
      writeRecord(record);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void finish()
   {

   }

}
