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


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.ai.AiExposure;
import com.kingsrook.qqq.backend.core.actions.ai.AiFieldMetaData;
import com.kingsrook.qqq.backend.core.actions.ai.AiTableMetaData;
import com.kingsrook.qqq.backend.core.actions.ai.AiToolKind;
import com.kingsrook.qqq.backend.core.actions.ai.output.AiLinkBuilder;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** describe_table: one readable table's fields (only those exposed - NONE
 ** fields are omitted; METADATA_ONLY / MASKED ones are flagged), help text,
 ** descriptions and the user's permissions.  Never any record data.
 *******************************************************************************/
public class DescribeTableTool extends AbstractAiTool
{
   public static final String NAME = "describe_table";



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
      return ("Describe a table the current user can read: its fields (name, label, type, possible-value source, help), primary key and the user's permissions.  "
         + "Fields marked exposure METADATA_ONLY can be used in filters but their values are never returned; MASKED values are partially masked.");
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
         "table", AiJsonSchema.string("Table name from list_tables.")
      ), List.of("table")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Map<String, Object> execute(AiToolContext context, AiToolInput input) throws QException
   {
      QTableMetaData table        = context.requireReadableTable(input.getRequiredString("table"));
      QTableMetaData personalized = context.personalize(table);

      Map<String, Object> data = new LinkedHashMap<>();
      data.put("name", table.getName());
      data.put("label", table.getLabel());

      AiTableMetaData aiTableMetaData = AiTableMetaData.of(table);
      if(aiTableMetaData != null && aiTableMetaData.getAiDescription() != null)
      {
         data.put("description", context.sanitize(aiTableMetaData.getAiDescription()));
      }

      String help = AiHelpText.plain(table.getHelpContent());
      if(help != null)
      {
         data.put("help", context.sanitize(help));
      }

      AiExposure tableExposure = context.getTableExposure(table);
      if(tableExposure != AiExposure.VALUES)
      {
         data.put("exposure", tableExposure.name());
      }

      data.put("primaryKeyField", table.getPrimaryKeyField());
      data.put("canInsert", context.hasTablePermission(table, TablePermissionSubType.INSERT));
      data.put("canEdit", context.hasTablePermission(table, TablePermissionSubType.EDIT));
      data.put("canDelete", context.hasTablePermission(table, TablePermissionSubType.DELETE));
      data.put("openHref", AiLinkBuilder.objectHref(table.getName()));

      List<Map<String, Object>> fields = new ArrayList<>();
      for(QFieldMetaData field : context.getNameableFields(personalized).values())
      {
         fields.add(describeField(context, personalized, field));
      }
      data.put("fields", fields);

      context.getResult().setRowCount(fields.size());
      context.getResult().setSummary("Described " + table.getLabel() + " · " + fields.size() + " field" + (fields.size() == 1 ? "" : "s"));
      return (data);
   }



   /*******************************************************************************
    ** Describe one (exposed) field.
    *******************************************************************************/
   private Map<String, Object> describeField(AiToolContext context, QTableMetaData table, QFieldMetaData field)
   {
      Map<String, Object> fieldMap = new LinkedHashMap<>();
      fieldMap.put("name", field.getName());
      fieldMap.put("label", field.getLabel());
      fieldMap.put("type", field.getType() == null ? null : field.getType().name());

      if(field.getPossibleValueSourceName() != null)
      {
         fieldMap.put("possibleValueSource", field.getPossibleValueSourceName());
      }

      if(field.getIsRequired())
      {
         fieldMap.put("required", true);
      }

      AiExposure exposure = context.getFieldExposure(table, field);
      if(exposure != AiExposure.VALUES)
      {
         fieldMap.put("exposure", exposure.name());
      }

      AiFieldMetaData aiFieldMetaData = AiFieldMetaData.of(field);
      if(aiFieldMetaData != null && aiFieldMetaData.getAiDescription() != null)
      {
         fieldMap.put("description", context.sanitize(aiFieldMetaData.getAiDescription()));
      }

      String help = AiHelpText.plain(field.getHelpContents());
      if(help != null)
      {
         fieldMap.put("help", context.sanitize(help));
      }

      return (fieldMap);
   }
}
