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


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QReportingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ExportInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportView;


/*******************************************************************************
 ** Report streamer implementation that just builds up a STATIC list of lists of strings.
 ** Meant only for use in unit tests at this time...  would need refactored for
 ** multi-thread/multi-use if wanted for real usage.
 *******************************************************************************/
public class ListOfMapsExportStreamer implements ExportStreamerInterface
{
   private static final QLogger LOG = QLogger.getLogger(ListOfMapsExportStreamer.class);

   private ExportInput          exportInput;
   private List<QFieldMetaData> fields;

   private static Map<String, List<Map<String, String>>> rows    = new LinkedHashMap<>();
   private static Map<String, List<String>>              headers = new LinkedHashMap<>();
   private static String                                 currentSheetLabel;



   /*******************************************************************************
    **
    *******************************************************************************/
   public ListOfMapsExportStreamer()
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void reset()
   {
      rows.clear();
      headers.clear();
      currentSheetLabel = null;
   }



   /*******************************************************************************
    ** Getter for list
    **
    *******************************************************************************/
   public static List<Map<String, String>> getList(String name)
   {
      return (rows.get(name));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void start(ExportInput exportInput, List<QFieldMetaData> fields, String label, QReportView view) throws QReportingException
   {
      this.exportInput = exportInput;
      this.fields = fields;

      currentSheetLabel = label;

      rows.put(label, new ArrayList<>());

      if(exportInput.getIncludeHeaderRow())
      {
         headers.put(label, new ArrayList<>());
         for(QFieldMetaData field : fields)
         {
            headers.get(label).add(field.getLabel());
         }
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
         addRecord(qRecord);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void addRecord(QRecord qRecord)
   {
      Map<String, String> row = new LinkedHashMap<>();
      rows.get(currentSheetLabel).add(row);
      for(int i = 0; i < fields.size(); i++)
      {
         row.put(headers.get(currentSheetLabel).get(i), qRecord.getValueString(fields.get(i).getName()));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void addTotalsRow(QRecord record)
   {
      addRecord(record);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void finish()
   {

   }

}
