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


import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.kingsrook.qqq.backend.core.exceptions.QReportingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ExportInput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportDestination;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportView;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.memoization.Memoization;


/*******************************************************************************
 ** JSON export format implementation
 *******************************************************************************/
public class JsonExportStreamer implements ExportStreamerInterface
{
   private static final QLogger LOG = QLogger.getLogger(JsonExportStreamer.class);

   private boolean prettyPrint = true;

   private ExportInput          exportInput;
   private QTableMetaData       table;
   private List<QFieldMetaData> fields;
   private OutputStream         outputStream;

   private boolean multipleViews       = false;
   private boolean haveStartedAnyViews = false;

   private boolean needCommaBeforeRecord = false;

   private byte[] indent       = new byte[0];
   private String indentString = "";

   private Pattern                     colonLetterPattern    = Pattern.compile(":([A-Z]+)($|[A-Z][a-z])");
   private Memoization<String, String> fieldLabelMemoization = new Memoization<>();



   /*******************************************************************************
    **
    *******************************************************************************/
   public JsonExportStreamer()
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void preRun(ReportDestination reportDestination, List<QReportView> views) throws QReportingException
   {
      outputStream = reportDestination.getReportOutputStream();

      if(views.size() > 1)
      {
         multipleViews = true;
      }

      if(multipleViews)
      {
         try
         {
            indentIfPretty(outputStream);
            outputStream.write('[');
            newlineIfPretty(outputStream);
            increaseIndent();
         }
         catch(IOException e)
         {
            throw (new QReportingException("Error starting report output", e));
         }
      }
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

      needCommaBeforeRecord = false;

      try
      {
         if(multipleViews)
         {
            if(haveStartedAnyViews)
            {
               /////////////////////////
               // close the last view //
               /////////////////////////
               newlineIfPretty(outputStream);

               decreaseIndent();
               indentIfPretty(outputStream);
               outputStream.write(']');
               newlineIfPretty(outputStream);

               decreaseIndent();
               indentIfPretty(outputStream);
               outputStream.write('}');
               outputStream.write(',');
               newlineIfPretty(outputStream);
            }

            /////////////////////////////////////////////////////////////
            // open a new view, as an object, with a name & data entry //
            /////////////////////////////////////////////////////////////
            indentIfPretty(outputStream);
            outputStream.write('{');
            newlineIfPretty(outputStream);
            increaseIndent();

            indentIfPretty(outputStream);
            outputStream.write(String.format("""
               "name":"%s",""", label).getBytes(StandardCharsets.UTF_8));
            newlineIfPretty(outputStream);

            indentIfPretty(outputStream);
            outputStream.write("""
               "data":""".getBytes(StandardCharsets.UTF_8));
            newlineIfPretty(outputStream);
         }

         //////////////////////////////////////////////
         // start the array of entries for this view //
         //////////////////////////////////////////////
         indentIfPretty(outputStream);
         outputStream.write('[');
         increaseIndent();
      }
      catch(IOException e)
      {
         throw (new QReportingException("Error starting report output", e));
      }

      haveStartedAnyViews = true;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void increaseIndent()
   {
      indent = new byte[indent.length + 3];
      Arrays.fill(indent, (byte) ' ');
      indentString = new String(indent);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void decreaseIndent()
   {
      indent = new byte[Math.max(0, indent.length - 3)];
      Arrays.fill(indent, (byte) ' ');
      indentString = new String(indent);
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
         if(needCommaBeforeRecord)
         {
            outputStream.write(',');
         }

         Map<String, Serializable> mapForJson = new LinkedHashMap<>();
         for(QFieldMetaData field : fields)
         {
            mapForJson.put(getLabelForJson(field), qRecord.getValue(field.getName()));
         }

         String json = prettyPrint ? JsonUtils.toPrettyJson(mapForJson) : JsonUtils.toJson(mapForJson);
         if(prettyPrint)
         {
            json = json.replaceAll("(?s)\n", "\n" + indentString);
         }

         if(prettyPrint)
         {
            outputStream.write('\n');
         }

         indentIfPretty(outputStream);
         outputStream.write(json.getBytes(StandardCharsets.UTF_8));

         outputStream.flush(); // todo - less often?
         needCommaBeforeRecord = true;
      }
      catch(Exception e)
      {
         throw (new QReportingException("Error writing JSON report", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   String getLabelForJson(QFieldMetaData field)
   {
      //////////////////////////////////////////////////////////////////////////
      // memoize, to avoid running these regex/replacements millions of times //
      //////////////////////////////////////////////////////////////////////////
      Optional<String> result = fieldLabelMemoization.getResult(field.getName(), fieldName ->
      {
         String labelForJson = field.getLabel().replace(" ", "");

         /////////////////////////////////////////////////////////////////////////////
         // now fix up any field-name-parts after the table: portion of a join name //
         // lineItem:SKU to become lineItem:sku                                     //
         // parcel:SLAStatus to become parcel:slaStatus                             //
         // order:Client to become order:client                                     //
         /////////////////////////////////////////////////////////////////////////////
         Matcher allCaps                          = Pattern.compile("^[A-Z]+$").matcher(labelForJson);
         Matcher startsAllCapsThenNextWordMatcher = Pattern.compile("([A-Z]+)([A-Z][a-z].*)").matcher(labelForJson);
         Matcher startsOneCapMatcher              = Pattern.compile("([A-Z])(.*)").matcher(labelForJson);

         if(allCaps.matches())
         {
            labelForJson = allCaps.replaceAll(m -> m.group().toLowerCase());
         }
         else if(startsAllCapsThenNextWordMatcher.matches())
         {
            labelForJson = startsAllCapsThenNextWordMatcher.replaceAll(m -> m.group(1).toLowerCase() + m.group(2));
         }
         else if(startsOneCapMatcher.matches())
         {
            labelForJson = startsOneCapMatcher.replaceAll(m -> m.group(1).toLowerCase() + m.group(2));
         }

         /////////////////////////////////////////////////////////////////////////////
         // now fix up any field-name-parts after the table: portion of a join name //
         // lineItem:SKU to become lineItem:sku                                     //
         // parcel:SLAStatus to become parcel:slaStatus                             //
         // order:Client to become order:client                                     //
         /////////////////////////////////////////////////////////////////////////////
         Matcher colonThenAllCapsThenEndMatcher      = Pattern.compile("(.*:)([A-Z]+)$").matcher(labelForJson);
         Matcher colonThenAllCapsThenNextWordMatcher = Pattern.compile("(.*:)([A-Z]+)([A-Z][a-z].*)").matcher(labelForJson);
         Matcher colonThenOneCapMatcher              = Pattern.compile("(.*:)([A-Z])(.*)").matcher(labelForJson);

         if(colonThenAllCapsThenEndMatcher.matches())
         {
            labelForJson = colonThenAllCapsThenEndMatcher.replaceAll(m -> m.group(1) + m.group(2).toLowerCase());
         }
         else if(colonThenAllCapsThenNextWordMatcher.matches())
         {
            labelForJson = colonThenAllCapsThenNextWordMatcher.replaceAll(m -> m.group(1) + m.group(2).toLowerCase() + m.group(3));
         }
         else if(colonThenOneCapMatcher.matches())
         {
            labelForJson = colonThenOneCapMatcher.replaceAll(m -> m.group(1) + m.group(2).toLowerCase() + m.group(3));
         }

         System.out.println("Label: " + labelForJson);
         return (labelForJson);
      });

      return result.orElse(field.getName());
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
   public void finish() throws QReportingException
   {
      try
      {
         //////////////////////////////////////////////
         // close the array of entries for this view //
         //////////////////////////////////////////////
         newlineIfPretty(outputStream);

         decreaseIndent();
         indentIfPretty(outputStream);
         outputStream.write(']');
         newlineIfPretty(outputStream);

         if(multipleViews)
         {
            ////////////////////////////////////////////
            // close this view, if there are multiple //
            ////////////////////////////////////////////
            decreaseIndent();
            indentIfPretty(outputStream);
            outputStream.write('}');
            newlineIfPretty(outputStream);

            /////////////////////////////
            // close the list of views //
            /////////////////////////////
            decreaseIndent();
            indentIfPretty(outputStream);
            outputStream.write(']');
            newlineIfPretty(outputStream);
         }
      }
      catch(IOException e)
      {
         throw (new QReportingException("Error ending report output", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void newlineIfPretty(OutputStream outputStream) throws IOException
   {
      if(prettyPrint)
      {
         outputStream.write('\n');
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void indentIfPretty(OutputStream outputStream) throws IOException
   {
      if(prettyPrint)
      {
         outputStream.write(indent);
      }
   }

}
