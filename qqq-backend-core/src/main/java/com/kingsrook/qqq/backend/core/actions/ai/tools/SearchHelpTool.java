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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import com.kingsrook.qqq.backend.core.actions.ai.AiExposure;
import com.kingsrook.qqq.backend.core.actions.ai.AiFieldMetaData;
import com.kingsrook.qqq.backend.core.actions.ai.AiTableMetaData;
import com.kingsrook.qqq.backend.core.actions.ai.AiToolKind;
import com.kingsrook.qqq.backend.core.actions.ai.output.AiLinkBuilder;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaDataInterface;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


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
      Set<String> terms = new LinkedHashSet<>();
      for(String word : input.getRequiredString("query").toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
      {
         if(word.length() >= 2 && terms.size() < MAX_TERMS)
         {
            terms.add(word);
         }
      }

      if(terms.isEmpty())
      {
         throw (AiToolException.invalidInput("The query must contain at least one word of 2 or more characters."));
      }

      List<Candidate> candidates = new ArrayList<>();
      collectCandidates(context, candidates);

      List<Candidate> matches = new ArrayList<>();
      for(Candidate candidate : candidates)
      {
         String text = candidate.searchText();
         candidate.score = 0;
         for(String term : terms)
         {
            if(text.contains(term))
            {
               candidate.score++;
            }
         }
         if(candidate.score > 0)
         {
            matches.add(candidate);
         }
      }

      matches.sort(Comparator.comparing((Candidate c) -> -c.score).thenComparing(c -> c.kind).thenComparing(c -> c.key));

      List<Map<String, Object>> results = new ArrayList<>();
      for(Candidate match : matches.subList(0, Math.min(MAX_RESULTS, matches.size())))
      {
         Map<String, Object> result = new LinkedHashMap<>();
         result.put("kind", match.kind);
         result.put("name", match.name);
         if(match.table != null)
         {
            result.put("table", match.table);
         }
         result.put("label", match.label);
         if(match.text != null)
         {
            result.put("help", context.sanitize(match.text));
         }
         if(match.href != null)
         {
            result.put("href", match.href);
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
    ** Collect the searchable objects this session can see.
    *******************************************************************************/
   private static void collectCandidates(AiToolContext context, List<Candidate> candidates) throws QException
   {
      for(QAppMetaData app : new TreeMap<>(CollectionUtils.nonNullMap(context.getQInstance().getApps())).values())
      {
         if(context.isVisible(app))
         {
            candidates.add(new Candidate("app", app.getName(), null, app.getLabel(), AiHelpText.plain(app.getHelpContent()), AiLinkBuilder.objectHref(app.getName())));
         }
      }

      for(QTableMetaData table : new TreeMap<>(CollectionUtils.nonNullMap(context.getQInstance().getTables())).values())
      {
         if(!context.isVisible(table) || context.getTableExposure(table) == AiExposure.NONE)
         {
            continue;
         }

         AiTableMetaData aiTableMetaData = AiTableMetaData.of(table);
         candidates.add(new Candidate("table", table.getName(), null, table.getLabel(), join(aiTableMetaData == null ? null : aiTableMetaData.getAiDescription(), AiHelpText.plain(table.getHelpContent())), AiLinkBuilder.objectHref(table.getName())));

         QTableMetaData personalized = context.personalize(table);
         for(QFieldMetaData field : context.getNameableFields(personalized).values())
         {
            AiFieldMetaData aiFieldMetaData = AiFieldMetaData.of(field);
            String          text            = join(aiFieldMetaData == null ? null : aiFieldMetaData.getAiDescription(), AiHelpText.plain(field.getHelpContents()));
            if(text != null)
            {
               candidates.add(new Candidate("field", field.getName(), table.getName(), field.getLabel(), text, null));
            }
         }
      }

      for(QProcessMetaData process : new TreeMap<>(CollectionUtils.nonNullMap(context.getQInstance().getProcesses())).values())
      {
         if(!process.getIsHidden() && context.isVisible(process))
         {
            candidates.add(new Candidate("process", process.getName(), process.getTableName(), process.getLabel(), null, AiLinkBuilder.objectHref(process.getName())));
         }
      }

      for(QWidgetMetaDataInterface widget : new TreeMap<>(CollectionUtils.nonNullMap(context.getQInstance().getWidgets())).values())
      {
         if(context.isVisible(widget) && !CollectionUtils.nonNullCollection(context.getSettings().getHiddenWidgetNames()).contains(widget.getName()))
         {
            candidates.add(new Candidate("widget", widget.getName(), null, widget.getLabel(), join(widget.getTooltip(), AiHelpText.plain(widget.getHelpContent())), null));
         }
      }
   }



   /*******************************************************************************
    ** Join non-null texts (null if none).
    *******************************************************************************/
   private static String join(String a, String b)
   {
      if(a == null)
      {
         return (b);
      }
      return (b == null ? a : a + " " + b);
   }



   /*******************************************************************************
    ** One searchable object.
    *******************************************************************************/
   private static class Candidate
   {
      private final String kind;
      private final String name;
      private final String table;
      private final String label;
      private final String text;
      private final String href;
      private final String key;

      private int score;



      /*******************************************************************************
       ** Constructor
       *******************************************************************************/
      Candidate(String kind, String name, String table, String label, String text, String href)
      {
         this.kind = kind;
         this.name = name;
         this.table = table;
         this.label = label;
         this.text = text;
         this.href = href;
         this.key = (table == null ? "" : table + ".") + name;
      }



      /*******************************************************************************
       ** The text to match terms against (lower case).
       *******************************************************************************/
      String searchText()
      {
         return ((name + " " + (label == null ? "" : label) + " " + (text == null ? "" : text)).toLowerCase(Locale.ROOT));
      }
   }
}
