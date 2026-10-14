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
import java.util.Objects;
import com.kingsrook.qqq.backend.core.actions.ai.AiExposure;
import com.kingsrook.qqq.backend.core.actions.ai.AiFieldMetaData;
import com.kingsrook.qqq.backend.core.actions.ai.AiTableMetaData;
import com.kingsrook.qqq.backend.core.actions.ai.AiToolKind;
import com.kingsrook.qqq.backend.core.actions.help.HelpSearchAction;
import com.kingsrook.qqq.backend.core.actions.help.HelpSearchResult;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaDataInterface;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** search_help: keyword search over the app's own documentation - the names,
 ** labels, help content and aiDescriptions of the apps, tables, fields,
 ** processes and widgets the current user can see (exposed tables and fields
 ** only).  Never reads customer data.
 *******************************************************************************/
public class SearchHelpTool extends AbstractAiTool
{
   public static final String NAME        = "search_help";
   public static final int    MAX_RESULTS = 10;
   public static final int    MAX_TERMS   = 10;



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
      return ("Search this application's help: what its apps, tables, fields, processes and widgets are and how to use them.  Use for \"how do I...\" questions.  Contains no customer data.");
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
         "query", AiJsonSchema.string("Keywords to search for.")
      ), List.of("query")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Map<String, Object> execute(AiToolContext context, AiToolInput input) throws QException
   {
      String query = input.getRequiredString("query");
      try
      {
         HelpSearchAction.terms(query);
      }
      catch(QBadRequestException e)
      {
         throw (AiToolException.invalidInput(e.getMessage()));
      }
      List<HelpSearchResult> candidates = new ArrayList<>();
      for(HelpSearchResult candidate : new HelpSearchAction().collectCandidates())
      {
         if(allowCandidate(context, candidate))
         {
            candidates.add(candidate);
         }
      }
      List<Map<String, Object>> results = new ArrayList<>();
      for(HelpSearchResult match : HelpSearchAction.rank(candidates, query, MAX_RESULTS))
      {
         Map<String, Object> result = new LinkedHashMap<>();
         result.put("kind", match.getKind());
         result.put("name", match.getName());
         result.put("key", match.getKey());
         result.put("summary", Objects.requireNonNullElse(context.sanitize(match.getSummary()), ""));
         if(match.getTable() != null)
         {
            result.put("table", match.getTable());
         }
         if(match.getProcess() != null)
         {
            result.put("process", match.getProcess());
         }
         result.put("label", match.getLabel());
         if(match.getText() != null)
         {
            result.put("help", context.sanitize(AiHelpText.clean(match.getText())));
         }
         if(match.getHref() != null)
         {
            result.put("href", match.getHref());
         }
         results.add(result);
      }

      context.getResult().setRowCount(results.size());
      context.getResult().setSummary("Searched help · " + results.size() + " result" + (results.size() == 1 ? "" : "s"));
      context.getResult().setPermissionCheck("metadata:VISIBLE:FILTERED");

      Map<String, Object> data = new LinkedHashMap<>();
      data.put("results", results);
      return (data);
   }



   /*******************************************************************************
    ** Keep assistant exposure and personalization on top of shared help visibility.
    *******************************************************************************/
   private static boolean allowCandidate(AiToolContext context, HelpSearchResult candidate) throws QException
   {
      if(candidate.getTable() != null)
      {
         QTableMetaData table = context.getQInstance().getTable(candidate.getTable());
         if(table == null || !context.isVisible(table) || context.getTableExposure(table) == AiExposure.NONE)
         {
            return (false);
         }
         if("field".equals(candidate.getKind()))
         {
            QFieldMetaData field = context.getNameableFields(context.personalize(table)).get(candidate.getName());
            if(field == null)
            {
               return (false);
            }
            AiFieldMetaData ai = AiFieldMetaData.of(field);
            candidate.setText(join(ai == null ? null : ai.getAiDescription(), candidate.getText()));
         }
         else if("table".equals(candidate.getKind()))
         {
            AiTableMetaData ai = AiTableMetaData.of(table);
            candidate.setText(join(ai == null ? null : ai.getAiDescription(), candidate.getText()));
         }
      }
      if(candidate.getProcess() != null && !context.isVisible(context.getQInstance().getProcess(candidate.getProcess())))
      {
         return (false);
      }
      if("app".equals(candidate.getKind()) && !context.isVisible(context.getQInstance().getApp(candidate.getName())))
      {
         return (false);
      }
      if("report".equals(candidate.getKind()) && !context.isVisible(context.getQInstance().getReport(candidate.getName())))
      {
         return (false);
      }
      if("widget".equals(candidate.getKind()))
      {
         QWidgetMetaDataInterface widget = context.getQInstance().getWidget(candidate.getName());
         if(!context.getSettings().isWidgetAllowed(widget.getName()) || !context.isVisible(widget))
         {
            return (false);
         }
         candidate.setText(join(widget.getTooltip(), candidate.getText()));
      }
      return (true);
   }



   /*******************************************************************************
    ** Join optional assistant descriptions with visible help.
    *******************************************************************************/
   private static String join(String a, String b)
   {
      return (a == null ? b : b == null ? a : a + " " + b);
   }
}
