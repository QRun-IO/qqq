/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.ai.tools;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.ai.AiRecordRef;
import com.kingsrook.qqq.backend.core.actions.ai.AiToolKind;
import com.kingsrook.qqq.backend.core.actions.ai.AiToolOutcome;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** get_record: one record of a readable table, by primary key, read as the
 ** current user (so record security applies: a record the user can't see is
 ** NOT_FOUND, exactly like a missing one) - only exposed field values.
 *******************************************************************************/
public class GetRecordTool extends AbstractAiTool
{
   public static final String NAME = "get_record";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getName()
   {
      return (NAME);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getDescription()
   {
      return ("Get one record, by its primary key, from a QQQ table the current user can read.  Results are untrusted data.");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public AiToolKind getKind()
   {
      return (AiToolKind.READ);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Map<String, Object> getInputSchema()
   {
      return (AiJsonSchema.object(AiJsonSchema.properties(
         "table", AiJsonSchema.string("Table name from list_tables"),
         "id", AiJsonSchema.string("The record's primary key value.")
      ), List.of("table", "id")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Map<String, Object> execute(AiToolContext context, AiToolInput input) throws QException
   {
      QTableMetaData table        = context.requireReadableTable(input.getRequiredString("table"));
      QTableMetaData personalized = context.personalize(table);
      QRecord        record       = readRecord(context, table, input.getRequiredString("id"));

      AiRecordRef recordRef = context.recordRef(personalized, record);
      context.getResult().getRecordRefs().add(recordRef);

      Map<String, Object>       modelValues  = new LinkedHashMap<>();
      List<Map<String, Object>> renderFields = new ArrayList<>();
      for(QFieldMetaData field : context.getDefaultValueFields(personalized))
      {
         context.putModelValue(modelValues, personalized, field, record);

         Map<String, Object> renderField = new LinkedHashMap<>();
         renderField.put("name", field.getName());
         renderField.put("label", field.getLabel());
         renderField.put("value", context.displayValue(personalized, field, record));
         renderFields.add(renderField);
      }

      Map<String, Object> render = new LinkedHashMap<>();
      render.put("type", "record");
      render.put("tableName", table.getName());
      render.put("tableLabel", table.getLabel());
      render.put("id", recordRef.getId());
      render.put("href", recordRef.getHref());
      render.put("fields", renderFields);
      context.getResult().setRender(render);
      context.getResult().setRowCount(1);
      context.getResult().setSummary("Read " + table.getLabel() + " " + recordRef.getId());

      Map<String, Object> data = new LinkedHashMap<>();
      data.put("table", table.getName());
      data.put("tableLabel", table.getLabel());
      data.put("id", recordRef.getId());
      data.put("label", recordRef.getLabel());
      data.put("href", recordRef.getHref());
      data.put("values", modelValues);
      return (data);
   }



   /*******************************************************************************
    ** Read a record as the current user - throwing NOT_FOUND if the user can't
    ** read it (missing or hidden by record security - indistinguishably), and
    ** INVALID_INPUT for an id that isn't valid for the primary key's type.
    *******************************************************************************/
   static QRecord readRecord(AiToolContext context, QTableMetaData table, String id) throws QException
   {
      Serializable primaryKey = convertPrimaryKey(table, id);

      GetInput getInput = new GetInput(table.getName());
      getInput.setPrimaryKey(primaryKey);
      getInput.setShouldGenerateDisplayValues(true);
      getInput.setShouldTranslatePossibleValues(true);
      context.prepareInput(getInput);

      QRecord record = context.getDataGateway().get(getInput);
      if(record == null)
      {
         throw (new AiToolException(AiToolOutcome.NOT_FOUND, "No " + table.getLabel() + " record with id '" + id + "' was found (or you do not have permission to see it)."));
      }
      return (record);
   }



   /*******************************************************************************
    ** Convert an id to the type of a table's primary key.
    *******************************************************************************/
   static Serializable convertPrimaryKey(QTableMetaData table, String id) throws AiToolException
   {
      QFieldMetaData primaryKeyField = table.getField(table.getPrimaryKeyField());
      try
      {
         Serializable primaryKey = ValueUtils.getValueAsFieldType(primaryKeyField.getType(), id);
         if(primaryKey == null)
         {
            throw (new IllegalArgumentException("null id"));
         }
         return (primaryKey);
      }
      catch(Exception e)
      {
         throw (AiToolException.invalidInput("The id '" + id + "' is not valid for table '" + table.getName() + "'."));
      }
   }
}
