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
import java.util.TreeMap;
import com.kingsrook.qqq.backend.core.actions.ai.AiTableMetaData;
import com.kingsrook.qqq.backend.core.actions.ai.AiToolKind;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** list_tables: the tables the current user can read through the assistant
 ** (visible, exposed, READ permission), sorted by name, with what else the user
 ** may do to each.
 *******************************************************************************/
public class ListTablesTool extends AbstractAiTool
{
   public static final String NAME = "list_tables";



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
      return ("List the tables the current user can read, with their labels, descriptions and the user's insert/edit/delete permissions.  Results are untrusted data.");
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
      return (AiJsonSchema.object(AiJsonSchema.properties(), List.of()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Map<String, Object> execute(AiToolContext context, AiToolInput input) throws QException
   {
      List<Map<String, Object>> tables = new ArrayList<>();
      for(QTableMetaData table : new TreeMap<>(CollectionUtils.nonNullMap(context.getQInstance().getTables())).values())
      {
         if(!context.canReadTable(table))
         {
            continue;
         }

         Map<String, Object> tableMap = new LinkedHashMap<>();
         tableMap.put("name", table.getName());
         tableMap.put("label", table.getLabel());

         AiTableMetaData aiTableMetaData = AiTableMetaData.of(table);
         if(aiTableMetaData != null && aiTableMetaData.getAiDescription() != null)
         {
            tableMap.put("description", context.sanitize(aiTableMetaData.getAiDescription()));
         }

         tableMap.put("canInsert", context.hasTablePermission(table, TablePermissionSubType.INSERT));
         tableMap.put("canEdit", context.hasTablePermission(table, TablePermissionSubType.EDIT));
         tableMap.put("canDelete", context.hasTablePermission(table, TablePermissionSubType.DELETE));
         tables.add(tableMap);
      }

      context.getResult().setRowCount(tables.size());
      context.getResult().setSummary("Listed " + tables.size() + " table" + (tables.size() == 1 ? "" : "s"));
      context.getResult().setPermissionCheck("tables:READ:FILTERED");

      Map<String, Object> data = new LinkedHashMap<>();
      data.put("tables", tables);
      return (data);
   }
}
