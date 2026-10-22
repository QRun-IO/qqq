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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkInsertMapping;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadProfile;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class BulkInsertReceiveValueMappingStep implements BackendStep
{
   private static final QLogger LOG = QLogger.getLogger(BulkInsertReceiveValueMappingStep.class);



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      try
      {
         BulkInsertStepUtils.handleSavedBulkLoadProfileIdValue(runBackendStepInput, runBackendStepOutput);

         List<String> fieldNamesToDoValueMapping = (List<String>) runBackendStepInput.getValue("fieldNamesToDoValueMapping");
         Integer      valueMappingFieldIndex     = runBackendStepInput.getValueInteger("valueMappingFieldIndex");

         String fieldName = fieldNamesToDoValueMapping.get(valueMappingFieldIndex);

         ///////////////////////////////////////////////////////////////////
         // read process values - construct a bulkLoadProfile out of them //
         ///////////////////////////////////////////////////////////////////
         BulkLoadProfile bulkLoadProfile = BulkInsertStepUtils.getBulkLoadProfile(runBackendStepInput);

         /////////////////////////////////////////////////////////////////////////
         // put the list of bulk load profile into the process state - it's the //
         // thing that the frontend will be looking at as the saved profile     //
         /////////////////////////////////////////////////////////////////////////
         runBackendStepOutput.addValue("bulkLoadProfile", bulkLoadProfile);

         ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // get the bulkInsertMapping object from the process, creating a fieldNameToValueMapping map within it if needed //
         ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         BulkInsertMapping                      bulkInsertMapping       = (BulkInsertMapping) runBackendStepOutput.getValue("bulkInsertMapping");
         Map<String, Map<String, Serializable>> fieldNameToValueMapping = bulkInsertMapping.getFieldNameToValueMapping();
         if(fieldNameToValueMapping == null)
         {
            fieldNameToValueMapping = new HashMap<>();
            bulkInsertMapping.setFieldNameToValueMapping(fieldNameToValueMapping);
         }
         runBackendStepOutput.addValue("bulkInsertMapping", bulkInsertMapping);

         ////////////////////////////////////////////////
         // put the mapped values into the mapping map //
         ////////////////////////////////////////////////
         Map<String, Serializable> mappedValues = JsonUtils.toObject(runBackendStepInput.getValueString("mappedValuesJSON"), new TypeReference<>() {});
         fieldNameToValueMapping.put(fieldName, mappedValues);

         /////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // always return to the prepare-mapping step - as it will determine if it's time to break the loop or not. //
         /////////////////////////////////////////////////////////////////////////////////////////////////////////////
         BulkInsertStepUtils.setNextStepPrepareValueMapping(runBackendStepOutput);
      }
      catch(Exception e)
      {
         LOG.warn("Error in bulk insert receive mapping", e);
         throw new QException("Unhandled error in bulk insert receive mapping step", e);
      }
   }

}
