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


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.ai.AiToolKind;
import com.kingsrook.qqq.backend.core.actions.ai.filter.AiFilterBuilder;
import com.kingsrook.qqq.backend.core.actions.ai.output.AiLinkBuilder;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryHint;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** count_records: the number of records of a readable table matching a filter
 ** (METADATA_ONLY fields may be filtered on) - plus a "count" render payload
 ** with an openHref to the filtered list.
 *******************************************************************************/
public class CountRecordsTool extends AbstractAiTool
{
   public static final String NAME = "count_records";



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
      return ("Count the records in a QQQ table the current user can read that match a filter.");
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
      Map<String, Object> schema = AiJsonSchema.object(AiJsonSchema.properties(
         "table", AiJsonSchema.string("Table name from list_tables"),
         "filter", AiJsonSchema.ref(AiFilterBuilder.DEF_QUERY_FILTER)
      ), List.of("table", "filter"));
      schema.put("$defs", AiFilterBuilder.buildSchemaDefinitions());
      return (schema);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Map<String, Object> execute(AiToolContext context, AiToolInput input) throws QException
   {
      QTableMetaData table        = context.requireReadableTable(input.getRequiredString("table"));
      QTableMetaData personalized = context.personalize(table);
      QQueryFilter   filter       = QueryRecordsTool.buildFilter(context, personalized, input.getOptionalMap("filter"));
      String         openHref     = AiLinkBuilder.filteredTableHref(table.getName(), filter);

      CountInput countInput = new CountInput(table.getName());
      countInput.setFilter(filter);
      countInput.setTimeoutSeconds(QueryRecordsTool.QUERY_TIMEOUT_SECONDS);
      countInput.withQueryHint(QueryHint.MAY_USE_READ_ONLY_BACKEND);
      context.prepareInput(countInput);
      Integer count = context.getDataGateway().count(countInput);

      Map<String, Object> render = new LinkedHashMap<>();
      render.put("type", "count");
      render.put("tableName", table.getName());
      render.put("tableLabel", table.getLabel());
      render.put("count", count);
      render.put("openHref", openHref);
      context.getResult().setRender(render);
      context.getResult().setRowCount(count);
      context.getResult().setSummary("Counted " + table.getLabel() + " · " + count);

      Map<String, Object> data = new LinkedHashMap<>();
      data.put("table", table.getName());
      data.put("tableLabel", table.getLabel());
      data.put("count", count);
      data.put("openHref", openHref);
      return (data);
   }
}
