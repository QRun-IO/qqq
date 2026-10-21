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


import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.ai.AiExposure;
import com.kingsrook.qqq.backend.core.actions.ai.AiToolKind;
import com.kingsrook.qqq.backend.core.actions.ai.filter.AiFilterBuilder;
import com.kingsrook.qqq.backend.core.actions.ai.output.AiLinkBuilder;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.MetaDataWithPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** navigate: build a same-origin in-app link (/app/...) to a table (optionally
 ** filtered), a record, a process, a report or an app, for the UI to route to.
 ** Reads no data.
 **
 ** The target is either the name of a table, process, report or app the user
 ** can see, or an /app/... path whose first segment is such a name.  Anything
 ** else - absolute URLs, protocol-relative //host paths, javascript: and other
 ** schemes, backslashes, ".." - is INVALID_INPUT.  Unknown and invisible names
 ** get the same answer, so a caller can't probe for objects.
 *******************************************************************************/
public class NavigateTool extends AbstractAiTool
{
   public static final String NAME = "navigate";



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
      return ("Take the user to a screen in this app: a table's list (optionally filtered), a record (give recordId), a process, a report or an app.  "
         + "Only use when the user asks to go somewhere.");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public AiToolKind getKind()
   {
      return (AiToolKind.NAVIGATION);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Map<String, Object> getInputSchema()
   {
      Map<String, Object> schema = AiJsonSchema.object(AiJsonSchema.properties(
         "target", AiJsonSchema.string("A table, process, report or app name - or an in-app path starting with /app/."),
         "recordId", AiJsonSchema.string("Optional: a record's primary key, to open that record of the target table."),
         "filter", AiJsonSchema.ref(AiFilterBuilder.DEF_QUERY_FILTER)
      ), List.of("target"));
      schema.put("$defs", AiFilterBuilder.buildSchemaDefinitions());
      return (schema);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Map<String, Object> execute(AiToolContext context, AiToolInput input) throws QException
   {
      String target   = input.getRequiredString("target").trim();
      String recordId = input.getOptionalString("recordId");

      String href;
      if(target.startsWith("/"))
      {
         if(recordId != null || input.has("filter"))
         {
            throw (AiToolException.invalidInput("recordId and filter can only be used with a table name target, not a path."));
         }
         href = pathHref(context, target);
      }
      else
      {
         MetaDataWithPermissionRules object = findVisibleObject(context, target);
         if(object instanceof QTableMetaData table)
         {
            href = tableHref(context, table, recordId, input.getOptionalMap("filter"));
         }
         else
         {
            if(recordId != null || input.has("filter"))
            {
               throw (AiToolException.invalidInput("recordId and filter can only be used with a table target."));
            }
            href = AiLinkBuilder.objectHref(target);
         }
      }

      if(!AiLinkBuilder.isSafeAppPath(href))
      {
         throw (AiToolException.invalidInput("That target can't be navigated to."));
      }

      context.getResult().setNavigateHref(href);
      context.getResult().setSummary("Navigate to " + href);
      context.getResult().setPermissionCheck("navigate:VISIBLE:OK");

      Map<String, Object> data = new LinkedHashMap<>();
      data.put("href", href);
      return (data);
   }



   /*******************************************************************************
    ** The href for a table target - its list, a record, or a filtered list.
    *******************************************************************************/
   private static String tableHref(AiToolContext context, QTableMetaData table, String recordId, Map<String, Object> filterInput) throws QException
   {
      if(recordId != null)
      {
         if(filterInput != null)
         {
            throw (AiToolException.invalidInput("Give either recordId or filter, not both."));
         }
         return (AiLinkBuilder.recordHref(table.getName(), GetRecordTool.convertPrimaryKey(table, recordId)));
      }

      if(filterInput != null)
      {
         QQueryFilter filter = AiFilterBuilder.build(filterInput, context.getNameableFields(context.personalize(table)));
         return (AiLinkBuilder.filteredTableHref(table.getName(), filter));
      }

      return (AiLinkBuilder.objectHref(table.getName()));
   }



   /*******************************************************************************
    ** The href for a path target - which must be a safe /app/ path whose first
    ** segment names an object the user can see.
    *******************************************************************************/
   private static String pathHref(AiToolContext context, String path) throws AiToolException
   {
      if(!AiLinkBuilder.isSafeAppPath(path))
      {
         throw (AiToolException.invalidInput("Only in-app paths starting with /app/ can be navigated to."));
      }

      String rest         = path.substring(AiLinkBuilder.APP_PREFIX.length());
      String firstSegment = rest.split("[/?#]", 2)[0];
      String decoded;
      try
      {
         decoded = URLDecoder.decode(firstSegment, StandardCharsets.UTF_8);
      }
      catch(IllegalArgumentException e)
      {
         throw (AiToolException.invalidInput("That path is not valid."));
      }

      findVisibleObject(context, decoded);
      return (path);
   }



   /*******************************************************************************
    ** Find the table (if exposed), process, report or app with a name, that the
    ** session can see - else INVALID_INPUT (the same for missing and invisible).
    *******************************************************************************/
   private static MetaDataWithPermissionRules findVisibleObject(AiToolContext context, String name) throws AiToolException
   {
      QTableMetaData table = context.getQInstance().getTable(name);
      if(table != null)
      {
         if(context.isVisible(table) && context.getTableExposure(table) != AiExposure.NONE)
         {
            return (table);
         }
         throw (unknownTarget(name));
      }

      MetaDataWithPermissionRules object = context.getQInstance().getProcess(name);
      if(object == null)
      {
         object = context.getQInstance().getReport(name);
      }
      if(object == null)
      {
         object = context.getQInstance().getApp(name);
      }

      if(object == null || !context.isVisible(object))
      {
         throw (unknownTarget(name));
      }
      return (object);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static AiToolException unknownTarget(String name)
   {
      return (AiToolException.invalidInput("There is no table, process, report or app named '" + name + "' that you can open."));
   }
}
