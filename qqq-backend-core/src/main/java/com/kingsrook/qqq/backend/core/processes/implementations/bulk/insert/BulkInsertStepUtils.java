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


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.savedbulkloadprofiles.SavedBulkLoadProfile;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadProfile;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadProfileField;
import com.kingsrook.qqq.backend.core.processes.tracing.ProcessTracerKeyRecordMessage;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.json.JSONArray;
import org.json.JSONObject;


/*******************************************************************************
 **
 *******************************************************************************/
public class BulkInsertStepUtils
{

   /***************************************************************************
    **
    ***************************************************************************/
   public static StorageInput getStorageInputForTheFile(RunBackendStepInput input) throws QException
   {
      @SuppressWarnings("unchecked")
      ArrayList<StorageInput> storageInputs = (ArrayList<StorageInput>) input.getValue("theFile");
      if(storageInputs == null)
      {
         throw (new QException("StorageInputs for theFile were not found in process state"));
      }

      if(storageInputs.isEmpty())
      {
         throw (new QException("StorageInputs for theFile was an empty list"));
      }

      StorageInput storageInput = storageInputs.get(0);
      return (storageInput);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static void setStorageInputForTheFile(RunProcessInput runProcessInput, StorageInput storageInput)
   {
      ArrayList<StorageInput> storageInputs = new ArrayList<>();
      storageInputs.add(storageInput);
      runProcessInput.addValue("theFile", storageInputs);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static void setNextStepStreamedETLPreview(RunBackendStepOutput runBackendStepOutput)
   {
      runBackendStepOutput.setOverrideLastStepName("receiveValueMapping");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static void setNextStepPrepareValueMapping(RunBackendStepOutput runBackendStepOutput)
   {
      runBackendStepOutput.setOverrideLastStepName("receiveFileMapping");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static BulkLoadProfile getBulkLoadProfile(RunBackendStepInput runBackendStepInput)
   {
      String version = runBackendStepInput.getValueString("version");
      if("v1".equals(version))
      {
         String  layout       = runBackendStepInput.getValueString("layout");
         Boolean hasHeaderRow = runBackendStepInput.getValueBoolean("hasHeaderRow");
         String  keyFields    = runBackendStepInput.getValueString("keyFields");
         Boolean isBulkEdit   = runBackendStepInput.getValueBoolean("isBulkEdit");

         ArrayList<BulkLoadProfileField> fieldList = new ArrayList<>();

         JSONArray array = new JSONArray(runBackendStepInput.getValueString("fieldListJSON"));
         for(int i = 0; i < array.length(); i++)
         {
            JSONObject           jsonObject           = array.getJSONObject(i);
            BulkLoadProfileField bulkLoadProfileField = new BulkLoadProfileField();
            fieldList.add(bulkLoadProfileField);
            bulkLoadProfileField.setFieldName(jsonObject.optString("fieldName"));
            bulkLoadProfileField.setHeaderName(jsonObject.has("headerName") ? jsonObject.getString("headerName") : null);
            bulkLoadProfileField.setColumnIndex(jsonObject.has("columnIndex") ? jsonObject.getInt("columnIndex") : null);
            bulkLoadProfileField.setDefaultValue((Serializable) jsonObject.opt("defaultValue"));
            bulkLoadProfileField.setDoValueMapping(jsonObject.optBoolean("doValueMapping"));
            bulkLoadProfileField.setClearIfEmpty(jsonObject.optBoolean("clearIfEmpty"));

            if(BooleanUtils.isTrue(bulkLoadProfileField.getDoValueMapping()) && jsonObject.has("valueMappings"))
            {
               bulkLoadProfileField.setValueMappings(new HashMap<>());
               JSONObject valueMappingsJsonObject = jsonObject.getJSONObject("valueMappings");
               for(String fileValue : valueMappingsJsonObject.keySet())
               {
                  bulkLoadProfileField.getValueMappings().put(fileValue, ValueUtils.getValueAsString(valueMappingsJsonObject.get(fileValue)));
               }
            }
         }

         BulkLoadProfile bulkLoadProfile = new BulkLoadProfile()
            .withIsBulkEdit(isBulkEdit)
            .withKeyFields(keyFields)
            .withVersion(version)
            .withFieldList(fieldList)
            .withHasHeaderRow(hasHeaderRow)
            .withLayout(layout);

         return (bulkLoadProfile);
      }
      else
      {
         throw (new IllegalArgumentException("Unexpected version for bulk load profile: " + version));
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static QRecord handleSavedBulkLoadProfileIdValue(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      Integer savedBulkLoadProfileId = runBackendStepInput.getValueInteger("savedBulkLoadProfileId");
      if(savedBulkLoadProfileId != null)
      {
         QRecord savedBulkLoadProfileRecord = GetAction.execute(SavedBulkLoadProfile.TABLE_NAME, savedBulkLoadProfileId);
         runBackendStepOutput.addValue("savedBulkLoadProfileRecord", savedBulkLoadProfileRecord);
         return (savedBulkLoadProfileRecord);
      }

      return (null);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static boolean isHeadless(RunBackendStepInput runBackendStepInput)
   {
      return (runBackendStepInput.getValuePrimitiveBoolean("isHeadless"));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static void setHeadless(RunProcessInput runProcessInput)
   {
      runProcessInput.addValue("isHeadless", true);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static void setProcessTracerKeyRecordMessage(RunProcessInput runProcessInput, ProcessTracerKeyRecordMessage processTracerKeyRecordMessage)
   {
      runProcessInput.addValue("processTracerKeyRecordMessage", processTracerKeyRecordMessage);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static ProcessTracerKeyRecordMessage getProcessTracerKeyRecordMessage(RunBackendStepInput runBackendStepInput)
   {
      Serializable value = runBackendStepInput.getValue("processTracerKeyRecordMessage");
      if(value instanceof ProcessTracerKeyRecordMessage processTracerKeyRecordMessage)
      {
         return (processTracerKeyRecordMessage);
      }

      return (null);
   }
}
