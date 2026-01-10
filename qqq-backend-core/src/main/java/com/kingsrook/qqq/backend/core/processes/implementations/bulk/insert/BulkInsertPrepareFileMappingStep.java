/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert;


import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.filehandling.FileToRowsInterface;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.mapping.BulkLoadMappingSuggester;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.mapping.BulkLoadTableStructureBuilder;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadFileRow;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadProfile;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadProfileField;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadTableStructure;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.json.JSONObject;


/*******************************************************************************
 **
 *******************************************************************************/
public class BulkInsertPrepareFileMappingStep implements BackendStep
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      buildFileDetailsForMappingStep(runBackendStepInput, runBackendStepOutput);

      boolean                isBulkEdit     = BooleanUtils.isTrue(runBackendStepInput.getValueBoolean("isBulkEdit"));
      String                 tableName      = runBackendStepInput.getValueString("tableName");
      BulkLoadTableStructure tableStructure = BulkLoadTableStructureBuilder.buildTableStructure(tableName, isBulkEdit);
      runBackendStepOutput.addValue("tableStructure", tableStructure);
      runBackendStepOutput.addValue("isBulkEdit", isBulkEdit);

      boolean needSuggestedMapping = true;
      if(runBackendStepOutput.getProcessState().getIsStepBack())
      {
         needSuggestedMapping = false;

         StreamedETLWithFrontendProcess.resetValidationFields(runBackendStepInput, runBackendStepOutput);
      }

      if(needSuggestedMapping)
      {
         @SuppressWarnings("unchecked")
         List<String> headerValues = (List<String>) runBackendStepOutput.getValue("headerValues");
         buildSuggestedMapping(isBulkEdit, headerValues, getPrepopulatedValues(runBackendStepInput), tableStructure, runBackendStepOutput);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private Map<String, Serializable> getPrepopulatedValues(RunBackendStepInput runBackendStepInput)
   {
      String prepopulatedValuesJson = runBackendStepInput.getValueString("prepopulatedValues");
      if(StringUtils.hasContent(prepopulatedValuesJson))
      {
         Map<String, Serializable> rs         = new LinkedHashMap<>();
         JSONObject                jsonObject = JsonUtils.toJSONObject(prepopulatedValuesJson);
         for(String key : jsonObject.keySet())
         {
            rs.put(key, jsonObject.optString(key, null));
         }
         return (rs);
      }

      return (Collections.emptyMap());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void buildSuggestedMapping(boolean isBulkEdit, List<String> headerValues, Map<String, Serializable> prepopulatedValues, BulkLoadTableStructure tableStructure, RunBackendStepOutput runBackendStepOutput)
   {
      BulkLoadMappingSuggester bulkLoadMappingSuggester = new BulkLoadMappingSuggester();
      BulkLoadProfile          bulkLoadProfile          = bulkLoadMappingSuggester.suggestBulkLoadMappingProfile(tableStructure, headerValues, isBulkEdit);

      if(CollectionUtils.nullSafeHasContents(prepopulatedValues))
      {
         for(Map.Entry<String, Serializable> entry : prepopulatedValues.entrySet())
         {
            String  fieldName           = entry.getKey();
            boolean foundFieldInProfile = false;

            for(BulkLoadProfileField bulkLoadProfileField : bulkLoadProfile.getFieldList())
            {
               if(bulkLoadProfileField.getFieldName().equals(fieldName))
               {
                  foundFieldInProfile = true;
                  bulkLoadProfileField.setColumnIndex(null);
                  bulkLoadProfileField.setHeaderName(null);
                  bulkLoadProfileField.setDefaultValue(entry.getValue());
                  break;
               }
            }

            if(!foundFieldInProfile)
            {
               BulkLoadProfileField bulkLoadProfileField = new BulkLoadProfileField();
               bulkLoadProfileField.setFieldName(fieldName);
               bulkLoadProfileField.setDefaultValue(entry.getValue());
               bulkLoadProfile.getFieldList().add(bulkLoadProfileField);
            }
         }
      }

      runBackendStepOutput.addValue("bulkLoadProfile", bulkLoadProfile);
      runBackendStepOutput.addValue("suggestedBulkLoadProfile", bulkLoadProfile);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static void buildFileDetailsForMappingStep(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      StorageInput storageInput = BulkInsertStepUtils.getStorageInputForTheFile(runBackendStepInput);
      File         file         = new File(storageInput.getReference());
      runBackendStepOutput.addValue("fileBaseName", file.getName());

      try
         (
            ///////////////////////////////////////////////////////////////////////////////////////////////////////////
            // open a stream to read from our file, and a FileToRows object, that knows how to read from that stream //
            ///////////////////////////////////////////////////////////////////////////////////////////////////////////
            InputStream inputStream = new StorageAction().getInputStream(storageInput);
            FileToRowsInterface fileToRowsInterface = FileToRowsInterface.forFile(storageInput.getReference(), inputStream);
         )
      {
         /////////////////////////////////////////////////
         // read the 1st row, and assume it is a header //
         /////////////////////////////////////////////////
         BulkLoadFileRow   headerRow     = fileToRowsInterface.next();
         ArrayList<String> headerValues  = new ArrayList<>();
         ArrayList<String> headerLetters = new ArrayList<>();
         for(int i = 0; i < headerRow.size(); i++)
         {
            headerValues.add(ValueUtils.getValueAsString(headerRow.getValue(i)));
            headerLetters.add(toHeaderLetter(i));
         }
         runBackendStepOutput.addValue("headerValues", headerValues);
         runBackendStepOutput.addValue("headerLetters", headerLetters);

         ///////////////////////////////////////////////////////////////////////////////////////////
         // while there are more rows in the file - and we're under preview-rows limit, read rows //
         ///////////////////////////////////////////////////////////////////////////////////////////
         int                          previewRows      = 0;
         int                          previewRowsLimit = 5;
         ArrayList<ArrayList<String>> bodyValues       = new ArrayList<>();
         for(int i = 0; i < headerRow.size(); i++)
         {
            bodyValues.add(new ArrayList<>());
         }

         while(fileToRowsInterface.hasNext() && previewRows < previewRowsLimit)
         {
            BulkLoadFileRow bodyRow = fileToRowsInterface.next();
            previewRows++;

            for(int i = 0; i < headerRow.size(); i++)
            {
               bodyValues.get(i).add(ValueUtils.getValueAsString(bodyRow.getValueElseNull(i)));
            }
         }
         runBackendStepOutput.addValue("bodyValuesPreview", bodyValues);

      }
      catch(Exception e)
      {
         throw (new QException("Error reading bulk load file", e));
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   static String toHeaderLetter(int i)
   {
      StringBuilder rs = new StringBuilder();

      do
      {
         rs.insert(0, (char) ('A' + (i % 26)));
         i = (i / 26) - 1;
      }
      while(i >= 0);

      return (rs.toString());
   }

}
