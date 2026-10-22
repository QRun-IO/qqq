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

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert;


import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.mapping.BulkLoadTableStructureBuilder;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadTableStructure;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** step before the upload screen, to prepare dynamic help-text for user.
 *******************************************************************************/
public class BulkInsertPrepareFileUploadStep implements BackendStep
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      ////////////////////////////////////////////////////////////////////////////////////////
      // for headless-bulk load (e.g., sftp import), set up the process tracer's key record //
      ////////////////////////////////////////////////////////////////////////////////////////
      runBackendStepInput.traceMessage(BulkInsertStepUtils.getProcessTracerKeyRecordMessage(runBackendStepInput));

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // if user has come back here, clear out file (else the storageInput object that it is comes to the frontend, which isn't what we want!) //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      if(runBackendStepOutput.getProcessState().getIsStepBack())
      {
         runBackendStepOutput.addValue("theFile", null);
      }

      boolean                isBulkEdit     = runBackendStepInput.getProcessName().endsWith("EditWithFile");
      String                 tableName      = runBackendStepInput.getValueString("tableName");
      QTableMetaData         table          = QContext.getQInstance().getTable(tableName);
      BulkLoadTableStructure tableStructure = BulkLoadTableStructureBuilder.buildTableStructure(tableName);
      runBackendStepOutput.addValue("tableStructure", tableStructure);
      runBackendStepOutput.addValue("isBulkEdit", isBulkEdit);

      List<QFieldMetaData> requiredFields   = new ArrayList<>();
      List<QFieldMetaData> additionalFields = new ArrayList<>();
      for(QFieldMetaData field : tableStructure.getFields())
      {
         if(field.getIsRequired())
         {
            requiredFields.add(field);
         }
         else
         {
            additionalFields.add(field);
         }
      }

      /////////////////////////////////////////////
      // bulk edit allows primary key as a field //
      /////////////////////////////////////////////
      if(isBulkEdit)
      {
         requiredFields.add(0, table.getField(table.getPrimaryKeyField()));
      }

      StringBuilder html;
      String        childTableLabels = "";

      StringBuilder tallCSV = new StringBuilder();
      StringBuilder wideCSV = new StringBuilder();
      StringBuilder flatCSV = new StringBuilder();

      //////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // potentially this could be a parameter - for now, hard-code false, but keep the code around that did this //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////
      boolean listFieldsInHelpText = false;

      if(isBulkEdit || !CollectionUtils.nullSafeHasContents(tableStructure.getAssociations()))
      {
         html = new StringBuilder("""
            <p>Upload either a CSV or Excel (.xlsx) file, with one row for each record you want to
            ${action} in the ${tableLabel} table.</p><br />
            
            <p>Your file can contain any number of columns.  You will be prompted to map fields from
            the ${tableLabel} table to columns from your file or default values for all records that
            you are loading on the next screen.  It is optional whether you include a header row in your
            file (though it is encouraged, and is the only way to received suggested field mappings).
            For Excel files, only the first sheet in the workbook will be used.</p><br />
            """);

         if(listFieldsInHelpText)
         {
            appendTableRequiredAndAdditionalFields(html, requiredFields, additionalFields);
            html.append("""
               Template: <a href="data:text/csv;base64,${flatCSV}" download="${tableLabel}.csv">${tableLabel}.csv</a>""");
         }
         else
         {
            html.append("""
               <p>You can download a template file to see the full list of available fields:
               <a href="data:text/csv;base64,${flatCSV}" download="${tableLabel}.csv">${tableLabel}.csv</a>
               </p>
               """);
         }
      }
      else
      {
         childTableLabels = StringUtils.joinWithCommasAndAnd(tableStructure.getAssociations().stream().map(a -> a.getLabel()).toList()) + " table" + StringUtils.plural(table.getAssociations());

         html = new StringBuilder("""
            <p>Upload either a CSV or Excel (.xlsx) file.  Your file can be in one of three layouts:<p>
            ${openUL}
            <li><b>Flat</b>: Each row in the file will create one record in the ${tableLabel} table.</li>
            <li><b>Wide</b>: Each row in the file will create one record in the ${tableLabel} table,
            and optionally one or more records in the ${childTableLabels}, by supplying additional columns
            for each sub-record that you want to create.</li>
            <li><b>Tall</b>: Rows with matching values in the fields being used for the ${tableLabel}
            table will be used to create one ${tableLabel} record.  One or more records will also be built
            in the ${childTableLabels} by providing unique values in each row for the sub-records.</li>
            </ul><br />
            
            <p>Your file can contain any number of columns.  You will be prompted to map fields from
            the ${tableLabel} table to columns from your file or default values for all records that
            you are loading on the next screen.  It is optional whether you include a header row in your
            file (though it is encouraged, and is the only way to received suggested field mappings).
            For Excel files, only the first sheet in the workbook will be used.</p><br />
            """);

         if(listFieldsInHelpText)
         {
            appendTableRequiredAndAdditionalFields(html, requiredFields, additionalFields);
         }

         addCsvFields(tallCSV, requiredFields, additionalFields);
         addCsvFields(wideCSV, requiredFields, additionalFields);

         for(BulkLoadTableStructure association : tableStructure.getAssociations())
         {
            if(listFieldsInHelpText)
            {
               html.append("""
                  <p>You can also add values for these ${childLabel} fields:</p>
                  """.replace("${childLabel}", association.getLabel()));
               appendFieldsAsUlToHtml(html, association.getFields());
            }

            addCsvFields(tallCSV, association.getFields(), Collections.emptyList(), association.getLabel() + ": ", "");
            addCsvFields(wideCSV, association.getFields(), Collections.emptyList(), association.getLabel() + ": ", " - 1");
            addCsvFields(wideCSV, association.getFields(), Collections.emptyList(), association.getLabel() + ": ", " - 2");
         }

         finishCSV(tallCSV);
         finishCSV(wideCSV);

         if(listFieldsInHelpText)
         {
            html.append("""
               Templates: <a href="data:text/csv;base64,${flatCSV}" download="${tableLabel} - Flat.csv">${tableLabel} - Flat.csv</a>
               | <a href="data:text/csv;base64,${tallCSV}" download="${tableLabel} - Tall.csv">${tableLabel} - Tall.csv</a>
               | <a href="data:text/csv;base64,${wideCSV}" download="${tableLabel} - Wide.csv">${tableLabel} - Wide.csv</a>
               """);
         }
         else
         {
            html.append("""
               <p>You can download a template file to see the full list of available fields:
               <a href="data:text/csv;base64,${flatCSV}" download="${tableLabel} - Flat.csv">${tableLabel} - Flat.csv</a>
               | <a href="data:text/csv;base64,${tallCSV}" download="${tableLabel} - Tall.csv">${tableLabel} - Tall.csv</a>
               | <a href="data:text/csv;base64,${wideCSV}" download="${tableLabel} - Wide.csv">${tableLabel} - Wide.csv</a>
               </p>
               """);
         }
      }

      html.insert(0, """
         <details style="margin-top: 1rem; border: 1px solid gray; padding: 0.5rem; border-radius: 0.5rem; font-size: 0.875rem;">
            <summary style="cursor: pointer;">File Upload Instructions</summary>
            <div style="padding-top: 0.5rem;"></div>
         """);
      html.append("</details>");

      addCsvFields(flatCSV, requiredFields, additionalFields);
      finishCSV(flatCSV);

      String htmlString = html.toString()
         .replace("${action}", (isBulkEdit ? "edit" : "insert"))
         .replace("${tableLabel}", table.getLabel())
         .replace("${childTableLabels}", childTableLabels)
         .replace("${flatCSV}", Base64.getEncoder().encodeToString(flatCSV.toString().getBytes(StandardCharsets.UTF_8)))
         .replace("${tallCSV}", Base64.getEncoder().encodeToString(tallCSV.toString().getBytes(StandardCharsets.UTF_8)))
         .replace("${wideCSV}", Base64.getEncoder().encodeToString(wideCSV.toString().getBytes(StandardCharsets.UTF_8)))
         .replace("${openUL}", "<ul style='margin-left: 2rem;'>");

      runBackendStepOutput.addValue("upload.html", htmlString);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static void finishCSV(StringBuilder flatCSV)
   {
      flatCSV.deleteCharAt(flatCSV.length() - 1);
      flatCSV.append("\n");
      flatCSV.append(flatCSV.toString().replaceAll("[^,]", ""));
      flatCSV.append("\n");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static void addCsvFields(StringBuilder csv, List<QFieldMetaData> requiredFields, List<QFieldMetaData> additionalFields)
   {
      addCsvFields(csv, requiredFields, additionalFields, "", "");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static void addCsvFields(StringBuilder csv, List<QFieldMetaData> requiredFields, List<QFieldMetaData> additionalFields, String fieldLabelPrefix, String fieldLabelSuffix)
   {
      for(QFieldMetaData field : requiredFields)
      {
         csv.append(fieldLabelPrefix).append(field.getLabel()).append(fieldLabelSuffix).append(",");
      }

      for(QFieldMetaData field : additionalFields)
      {
         csv.append(fieldLabelPrefix).append(field.getLabel()).append(fieldLabelSuffix).append(",");
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static void appendTableRequiredAndAdditionalFields(StringBuilder html, List<QFieldMetaData> requiredFields, List<QFieldMetaData> additionalFields)
   {
      if(!requiredFields.isEmpty())
      {
         html.append("""
            <p> You will be required to supply values (either in a column in the file, or by
            choosing a default value on the next screen) for the following ${tableLabel} fields:</p>
            """);
         appendFieldsAsUlToHtml(html, requiredFields);
      }

      if(!additionalFields.isEmpty())
      {
         if(requiredFields.isEmpty())
         {
            html.append("""
               <p>You can supply values (either in a column in the file, or by choosing a
               default value on the next screen) for the following ${tableLabel} fields:</p>
               """);
         }
         else
         {
            html.append("<p>You can also add values for these fields:</p>");
         }

         appendFieldsAsUlToHtml(html, additionalFields);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static void appendFieldsAsUlToHtml(StringBuilder html, List<QFieldMetaData> additionalFields)
   {
      html.append("${openUL}");
      for(QFieldMetaData field : additionalFields)
      {
         html.append("<li>").append(field.getLabel()).append("</li>");
      }
      html.append("</ul><br />");
   }

}
