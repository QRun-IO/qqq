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


import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.google.gson.reflect.TypeToken;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.actions.values.SearchPossibleValueSourceAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceInput;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceOutput;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.filehandling.FileToRowsInterface;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkInsertMapping;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadFileRow;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class BulkInsertPrepareValueMappingStep implements BackendStep
{
   private static final QLogger LOG = QLogger.getLogger(BulkInsertPrepareValueMappingStep.class);



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      try
      {
         if(runBackendStepOutput.getProcessState().getIsStepBack())
         {
            StreamedETLWithFrontendProcess.resetValidationFields(runBackendStepInput, runBackendStepOutput);
         }

         /////////////////////////////////////////////////////////////
         // prep the frontend for what field we're going to map now //
         /////////////////////////////////////////////////////////////
         List<String> fieldNamesToDoValueMapping = (List<String>) runBackendStepInput.getValue("fieldNamesToDoValueMapping");
         Integer      valueMappingFieldIndex     = runBackendStepInput.getValueInteger("valueMappingFieldIndex");
         if(valueMappingFieldIndex == null)
         {
            valueMappingFieldIndex = 0;
         }
         else
         {
            valueMappingFieldIndex++;
         }

         ////////////////////////////////////////////////////////////////////////////////////////////////////
         // if there are no more fields (values) to map, then proceed to the standard streamed-ETL preview //
         ////////////////////////////////////////////////////////////////////////////////////////////////////
         if(valueMappingFieldIndex >= fieldNamesToDoValueMapping.size())
         {
            BulkInsertStepUtils.setNextStepStreamedETLPreview(runBackendStepOutput);
            return;
         }

         runBackendStepInput.addValue("valueMappingFieldIndex", valueMappingFieldIndex);

         String fullFieldName              = fieldNamesToDoValueMapping.get(valueMappingFieldIndex);
         String fieldNameWithoutWideSuffix = fullFieldName;
         if(fieldNameWithoutWideSuffix.contains(","))
         {
            fieldNameWithoutWideSuffix = fieldNameWithoutWideSuffix.replaceFirst(",.*", "");
         }
         TableAndField tableAndField = getTableAndField(runBackendStepInput.getValueString("tableName"), fieldNameWithoutWideSuffix);

         runBackendStepInput.addValue("valueMappingField", new QFrontendFieldMetaData(tableAndField.field()));
         runBackendStepInput.addValue("valueMappingFullFieldName", fullFieldName);
         runBackendStepInput.addValue("valueMappingFieldTableName", tableAndField.table().getName());

         ////////////////////////////////////////////////////
         // get all the values from the file in this field //
         // todo - should do all mapping fields at once?   //
         ////////////////////////////////////////////////////
         ArrayList<Serializable> fileValues = getValuesForField(tableAndField.table(), tableAndField.field(), fullFieldName, runBackendStepInput);
         runBackendStepOutput.addValue("fileValues", fileValues);

         ///////////////////////////////////////////////
         // clear these in case not getting set below //
         ///////////////////////////////////////////////
         runBackendStepOutput.addValue("valueMapping", new HashMap<>());
         runBackendStepOutput.addValue("mappedValueLabels", new HashMap<>());

         BulkInsertMapping             bulkInsertMapping = (BulkInsertMapping) runBackendStepInput.getValue("bulkInsertMapping");
         HashMap<String, Serializable> valueMapping      = null;
         if(bulkInsertMapping.getFieldNameToValueMapping() != null && bulkInsertMapping.getFieldNameToValueMapping().containsKey(fullFieldName))
         {
            valueMapping = CollectionUtils.useOrWrap(bulkInsertMapping.getFieldNameToValueMapping().get(fullFieldName), new TypeToken<>() {});
            runBackendStepOutput.addValue("valueMapping", valueMapping);

            if(StringUtils.hasContent(tableAndField.field().getPossibleValueSourceName()))
            {
               HashMap<Serializable, String> possibleValueLabels = loadPossibleValues(tableAndField.field(), valueMapping);
               runBackendStepOutput.addValue("mappedValueLabels", possibleValueLabels);
            }
         }
      }
      catch(Exception e)
      {
         LOG.warn("Error in bulk insert prepare value mapping", e);
         throw new QException("Unhandled error in bulk insert prepare value mapping step", e);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static TableAndField getTableAndField(String tableName, String fullFieldName) throws QException
   {
      List<String> parts         = new ArrayList<>(List.of(fullFieldName.split("\\.")));
      String       fieldBaseName = parts.remove(parts.size() - 1);

      QTableMetaData table = QContext.getQInstance().getTable(tableName);
      for(String associationName : parts)
      {
         Optional<Association> association = table.getAssociations().stream().filter(a -> a.getName().equals(associationName)).findFirst();
         if(association.isPresent())
         {
            table = QContext.getQInstance().getTable(association.get().getAssociatedTableName());
         }
         else
         {
            throw new QException("Missing association [" + associationName + "] on table [" + table.getName() + "]");
         }
      }

      TableAndField result = new TableAndField(table, table.getField(fieldBaseName));
      return result;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public record TableAndField(QTableMetaData table, QFieldMetaData field) {}



   /***************************************************************************
    **
    ***************************************************************************/
   private HashMap<Serializable, String> loadPossibleValues(QFieldMetaData field, Map<String, Serializable> valueMapping) throws QException
   {
      SearchPossibleValueSourceInput input = new SearchPossibleValueSourceInput();
      input.setPossibleValueSourceName(field.getPossibleValueSourceName());
      input.setIdList(new ArrayList<>(new HashSet<>(valueMapping.values()))); // go through a set to strip dupes
      SearchPossibleValueSourceOutput output = new SearchPossibleValueSourceAction().execute(input);

      HashMap<Serializable, String> rs = new HashMap<>();
      for(QPossibleValue<?> result : output.getResults())
      {
         Serializable id = (Serializable) result.getId();
         rs.put(id, result.getLabel());
      }
      return rs;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private ArrayList<Serializable> getValuesForField(QTableMetaData table, QFieldMetaData field, String fullFieldName, RunBackendStepInput runBackendStepInput) throws QException
   {
      StorageInput      storageInput      = BulkInsertStepUtils.getStorageInputForTheFile(runBackendStepInput);
      BulkInsertMapping bulkInsertMapping = (BulkInsertMapping) runBackendStepInput.getValue("bulkInsertMapping");

      List<Integer> wideAssociationIndexes = null;
      if(fullFieldName.contains(","))
      {
         wideAssociationIndexes = new ArrayList<>();
         String indexes = fullFieldName.substring(fullFieldName.lastIndexOf(",") + 1);
         for(String index : indexes.split("\\."))
         {
            wideAssociationIndexes.add(Integer.parseInt(index));
         }
      }

      String associationNameChain = null;
      if(fullFieldName.contains("."))
      {
         associationNameChain = fullFieldName.substring(0, fullFieldName.lastIndexOf('.'));
      }

      try
         (
            InputStream inputStream = new StorageAction().getInputStream(storageInput);
            FileToRowsInterface fileToRowsInterface = FileToRowsInterface.forFile(storageInput.getReference(), inputStream);
         )
      {
         Set<String>          values       = new LinkedHashSet<>();
         BulkLoadFileRow      headerRow    = bulkInsertMapping.getHasHeaderRow() ? fileToRowsInterface.next() : null;
         Map<String, Integer> fieldIndexes = bulkInsertMapping.getFieldIndexes(table, associationNameChain, headerRow, wideAssociationIndexes);
         int                  index        = fieldIndexes.get(field.getName());

         while(fileToRowsInterface.hasNext())
         {
            BulkLoadFileRow row   = fileToRowsInterface.next();
            Serializable    value = row.getValueElseNull(index);
            if(value != null)
            {
               values.add(ValueUtils.getValueAsString(value));
            }

            if(values.size() > 100)
            {
               throw (new QUserFacingException("Too many unique values were found for mapping for field: " + field.getName()));
            }
         }

         return (new ArrayList<>(values));
      }
      catch(Exception e)
      {
         throw (new QException("Error getting values from file", e));
      }
   }

}
