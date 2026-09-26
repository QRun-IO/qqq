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

package com.kingsrook.qqq.backend.core.processes.implementations.etl.basic;


import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.adapters.JsonToQFieldMappingAdapter;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.shared.mapping.AbstractQFieldMapping;
import com.kingsrook.qqq.backend.core.model.actions.shared.mapping.QKeyBasedFieldMapping;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Function body for performing the Extract step of a basic ETL process.
 *******************************************************************************/
public class BasicETLTransformFunction implements BackendStep
{
   private static final QLogger LOG = QLogger.getLogger(BasicETLTransformFunction.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      String tableName = runBackendStepInput.getValueString(BasicETLProcess.FIELD_DESTINATION_TABLE);
      LOG.debug("Start transform for destination table: " + tableName);

      ////////////////////////////////////////////////////////////////////////////////////////////
      // exit early with no-op if no records made it here, or if we don't have a mapping to use //
      ////////////////////////////////////////////////////////////////////////////////////////////
      if(CollectionUtils.nullSafeIsEmpty(runBackendStepInput.getRecords()))
      {
         LOG.debug("Exiting early with no-op for empty input record list.");
         return;
      }

      String mappingJSON = runBackendStepInput.getValueString(BasicETLProcess.FIELD_MAPPING_JSON);
      if(!StringUtils.hasContent(mappingJSON))
      {
         LOG.debug("Exiting early with no-op for empty mappingJSON.");
         return;
      }

      /////////////////////////////////////////////////////////////////////////////////////////////
      // require that the mapping be a key-based mapping (can't use indexes into qRecord values) //
      /////////////////////////////////////////////////////////////////////////////////////////////
      AbstractQFieldMapping<?> mapping = new JsonToQFieldMappingAdapter().buildMappingFromJson(mappingJSON);
      if(!(mapping instanceof QKeyBasedFieldMapping keyBasedFieldMapping))
      {
         throw (new QException("Mapping was not a Key-based mapping type.  Was a : " + mapping.getClass().getName()));
      }

      QTableMetaData table         = QContext.getQInstance().getTable(tableName);
      List<QRecord>  mappedRecords = applyMapping(runBackendStepInput.getRecords(), table, keyBasedFieldMapping);

      //////////////////////////////////////////////////////////////////////////////////////////////////////
      // todo - should this be conditional, e.g., driven by a field, or an opt-in customization function? //
      //////////////////////////////////////////////////////////////////////////////////////////////////////
      removeNonNumericValuesFromMappedRecords(table, mappedRecords);

      runBackendStepOutput.setRecords(mappedRecords);
      LOG.info("Done transforming " + runBackendStepOutput.getRecords().size() + " records.");
   }



   /*******************************************************************************
    ** Note:  package-private for direct unit-testability
    *******************************************************************************/
   void removeNonNumericValuesFromMappedRecords(QTableMetaData table, List<QRecord> records)
   {
      for(QRecord record : records)
      {
         for(QFieldMetaData field : table.getFields().values())
         {
            Serializable value = record.getValue(field.getName());
            if(value != null && StringUtils.hasContent(String.valueOf(value)))
            {
               record.setValue(field.getName(), tryToParseNumber(field.getType(), value));
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Serializable tryToParseNumber(QFieldType fieldType, Serializable value)
   {
      if(value == null)
      {
         //////////////////////////////
         // null input = null output //
         //////////////////////////////
         return (null);
      }

      ////////////////////////////////////////////////////
      // get a string version of the value to work with //
      ////////////////////////////////////////////////////
      String stringValue = String.valueOf(value);
      try
      {
         ////////////////////////////////////////////////////////////////////////////////
         // based on field type, try to parse - noting bad formatted values will throw //
         ////////////////////////////////////////////////////////////////////////////////
         if(fieldType.equals(QFieldType.INTEGER))
         {
            Integer.parseInt(stringValue);
         }
         else if(fieldType.equals(QFieldType.DECIMAL))
         {
            new BigDecimal(stringValue);
         }

         //////////////////////////////////////////////////////////////////////////////////
         // if we got through the parsing without throwing, then we can return the value //
         //////////////////////////////////////////////////////////////////////////////////
         return (value);
      }
      catch(NumberFormatException nfe)
      {
         /////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // upon number format exception, look for commas - if found, strip them out and try again (recursive call) //
         /////////////////////////////////////////////////////////////////////////////////////////////////////////////
         if(stringValue.contains(","))
         {
            stringValue = stringValue.replaceAll(",", "");
            return (tryToParseNumber(fieldType, stringValue));
         }

         /////////////////////////////////////////////////////////////////////////////////
         // else, if no comma, then we want a null value here, rather than a non-number //
         /////////////////////////////////////////////////////////////////////////////////
         LOG.debug("Nulling out value " + value + " that will not work in a " + fieldType + " field");
         return (null);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<QRecord> applyMapping(List<QRecord> input, QTableMetaData table, QKeyBasedFieldMapping mapping)
   {
      List<QRecord> output = new ArrayList<>();
      for(QRecord inputRecord : input)
      {
         QRecord outputRecord = new QRecord();
         output.add(outputRecord);
         for(QFieldMetaData field : table.getFields().values())
         {
            String fieldSource = mapping.getFieldSource(field.getName());
            outputRecord.setValue(field.getName(), inputRecord.getValue(fieldSource));
         }
      }
      return (output);
   }

}
