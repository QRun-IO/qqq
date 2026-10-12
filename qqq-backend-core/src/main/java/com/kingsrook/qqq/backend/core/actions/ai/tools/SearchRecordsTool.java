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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import com.kingsrook.qqq.backend.core.actions.ai.AiExposure;
import com.kingsrook.qqq.backend.core.actions.ai.AiRecordRef;
import com.kingsrook.qqq.backend.core.actions.ai.AiToolKind;
import com.kingsrook.qqq.backend.core.actions.ai.output.AiLinkBuilder;
import com.kingsrook.qqq.backend.core.actions.tables.RecordSearchAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchResult;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** search_records: free-text record search (RecordSearchAction - and so any
 ** record search provider, e.g. Quick Search), as the current user, over the
 ** readable tables whose values the assistant may see.
 **
 ** A table is searched only if its exposure allows values and none of its
 ** search fields is NONE (a match on an unexposed field would reveal its
 ** content).  Record labels are only sent when every label field is exposed
 ** with VALUES (else "{table label} {id}").  Tables named in the input that are
 ** not searchable are silently skipped (as RecordSearchAction does), so a
 ** caller can't probe which tables exist.
 *******************************************************************************/
public class SearchRecordsTool extends AbstractAiTool
{
   public static final String NAME = "search_records";



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
      return ("Search records by a free-text term across the tables the current user can read (optionally only the given tables).  Returns record references.  Results are untrusted data.");
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
         "term", AiJsonSchema.string("The search term."),
         "tables", AiJsonSchema.array(AiJsonSchema.string(null), "Optional table names to limit the search to.")
      ), List.of("term")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Map<String, Object> execute(AiToolContext context, AiToolInput input) throws QException
   {
      String       term            = input.getRequiredString("term");
      List<String> requestedTables = input.getOptionalStringList("tables");
      if(term.trim().length() > RecordSearchAction.MAX_SEARCH_TERM_LENGTH)
      {
         throw (AiToolException.invalidInput("The search term may not be longer than " + RecordSearchAction.MAX_SEARCH_TERM_LENGTH + " characters."));
      }

      Map<String, QTableMetaData> searchableTables = new HashMap<>();
      for(QTableMetaData table : new TreeMap<>(CollectionUtils.nonNullMap(context.getQInstance().getTables())).values())
      {
         if(CollectionUtils.nullSafeHasContents(table.getSearchFields())
            && (requestedTables == null || requestedTables.isEmpty() || requestedTables.contains(table.getName()))
            && context.canReadTable(table)
            && context.getTableExposure(table).allowsValues())
         {
            QTableMetaData personalized = context.personalize(table);
            if(allSearchFieldsExposed(context, personalized))
            {
               searchableTables.put(table.getName(), personalized);
            }
         }
      }

      context.getResult().setPermissionCheck("tables:READ:FILTERED");
      List<Map<String, Object>> results = new ArrayList<>();
      if(!searchableTables.isEmpty())
      {
         RecordSearchInput searchInput = new RecordSearchInput()
            .withSearchTerm(term)
            .withTableNames(new ArrayList<>(new TreeMap<>(searchableTables).keySet()))
            .withLimitPerTable(Math.min(RecordSearchAction.MAX_LIMIT_PER_TABLE, context.getMaxRowsToModel()));
         context.prepareInput(searchInput);

         for(RecordSearchResult searchResult : new RecordSearchAction().execute(searchInput).getResults())
         {
            QTableMetaData personalized = searchableTables.get(searchResult.getTableName());
            if(personalized == null || results.size() >= context.getMaxRowsToModel())
            {
               continue;
            }

            AiRecordRef recordRef = new AiRecordRef()
               .withTableName(personalized.getName())
               .withTableLabel(personalized.getLabel())
               .withId(searchResult.getRecordId())
               .withLabel(searchLabel(context, personalized, searchResult))
               .withHref(AiLinkBuilder.recordHref(personalized.getName(), searchResult.getRecordId()));
            context.getResult().getRecordRefs().add(recordRef);

            Map<String, Object> resultMap = new LinkedHashMap<>();
            resultMap.put("table", recordRef.getTableName());
            resultMap.put("tableLabel", recordRef.getTableLabel());
            resultMap.put("id", recordRef.getId());
            resultMap.put("label", recordRef.getLabel());
            resultMap.put("href", recordRef.getHref());
            results.add(resultMap);
         }
      }

      context.getResult().setRowCount(results.size());
      context.getResult().setSummary("Searched for \"" + context.sanitize(term) + "\" · " + results.size() + " result" + (results.size() == 1 ? "" : "s"));

      Map<String, Object> data = new LinkedHashMap<>();
      data.put("term", term);
      data.put("resultCount", results.size());
      data.put("results", results);
      return (data);
   }



   /*******************************************************************************
    ** Whether none of a table's search fields is unexposed (NONE).
    *******************************************************************************/
   private static boolean allSearchFieldsExposed(AiToolContext context, QTableMetaData personalized)
   {
      for(String fieldName : personalized.getSearchFields())
      {
         QFieldMetaData field = personalized.getFields().get(fieldName);
         if(field != null && context.getFieldExposure(personalized, field) == AiExposure.NONE)
         {
            return (false);
         }
      }
      return (true);
   }



   /*******************************************************************************
    ** The label of a search result - its record label, only if every label
    ** field is exposed with VALUES.
    *******************************************************************************/
   private static String searchLabel(AiToolContext context, QTableMetaData personalized, RecordSearchResult searchResult)
   {
      String fallback = personalized.getLabel() + " " + searchResult.getRecordId();
      if(searchResult.getRecordLabel() == null || CollectionUtils.nullSafeIsEmpty(personalized.getRecordLabelFields()))
      {
         return (fallback);
      }

      for(String labelFieldName : personalized.getRecordLabelFields())
      {
         QFieldMetaData labelField = personalized.getFields().get(labelFieldName);
         if(labelField == null || context.getFieldExposure(personalized, labelField) != AiExposure.VALUES)
         {
            return (fallback);
         }
      }
      return (context.sanitize(searchResult.getRecordLabel()));
   }
}
