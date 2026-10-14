/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.help;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.actions.ai.output.AiLinkBuilder;
import com.kingsrook.qqq.backend.core.actions.metadata.MetaDataAction;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceHelpContentManager;
import com.kingsrook.qqq.backend.core.model.actions.metadata.MetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataInput;
import com.kingsrook.qqq.backend.core.model.metadata.help.HelpContentVisibility;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.MetaDataWithPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Ranked help search shared by UI clients and the assistant.
 *******************************************************************************/
public class HelpSearchAction
{
   public static final int DEFAULT_LIMIT = 20;
   public static final int MAX_LIMIT = 50;
   public static final int MAX_TERMS = 10;



   /*******************************************************************************
    ** Search help with the current user's metadata and table-read permissions.
    *******************************************************************************/
   public List<HelpSearchResult> execute(HelpSearchInput input) throws QException
   {
      ActionHelper.validateSession(input);
      return (rank(collectCandidates(), input.getQuery(), input.getLimit()));
   }



   /*******************************************************************************
    ** Collect visible documentation, including label-only targets for navigation.
    *******************************************************************************/
   public List<HelpSearchResult> collectCandidates() throws QException
   {
      QInstanceHelpContentManager.reloadIfChanged(QContext.getQInstance());
      MetaDataInput input = new MetaDataInput();
      var customizer = new MetaDataAction().getMetaDataActionCustomizer();
      Map<String, QTableMetaData> tables = new LinkedHashMap<>();
      for(QTableMetaData table : QContext.getQInstance().getTables().values())
      {
         if(!MetaDataAction.isObjectDenied(input, customizer, table) && PermissionsHelper.hasTablePermission(input, table.getName(), TablePermissionSubType.READ))
         {
            QTableMetaData personalized = TableMetaDataPersonalizerAction.execute(new TableMetaDataInput().withTableName(table.getName()));
            if(personalized != null)
            {
               tables.put(table.getName(), personalized);
            }
         }
      }
      List<HelpSearchResult> results = new ArrayList<>();
      for(HelpTarget target : HelpTargetCatalog.collect(QContext.getQInstance(), tables))
      {
         if(target.hidden())
         {
            continue;
         }
         if(target.owner() instanceof MetaDataWithPermissionRules owner && MetaDataAction.isObjectDenied(input, customizer, owner))
         {
            continue;
         }
         if(target.table() != null && !PermissionsHelper.hasTablePermission(input, target.table(), TablePermissionSubType.READ))
         {
            continue;
         }
         List<QHelpContent> contents = CollectionUtils.nonNullList(HelpContentVisibility.filter(target.contents()));
         String summary = String.join(" ", contents.stream().map(QHelpContent::getSummaryOrDerived).filter(StringUtils::hasContent).toList());
         String text = String.join(" ", contents.stream().map(QHelpContent::getPlainText).filter(StringUtils::hasContent).toList());
         String html = String.join("\n", contents.stream().map(QHelpContent::getContentAsHtml).filter(Objects::nonNull).toList());
         String linkName = target.process() != null ? target.process() : target.table() != null ? target.table() : target.name();
         String href = List.of("instance", "widget", "possibleValueSource").contains(target.kind()) ? null : AiLinkBuilder.objectHref(linkName);
         results.add(new HelpSearchResult().withKind(target.kind()).withKey(target.key()).withName(target.name())
            .withTable(target.table()).withProcess(target.process()).withLabel(target.label()).withSummary(summary)
            .withSearchSummary(String.join(" ", contents.stream().map(QHelpContent::getSummary).filter(StringUtils::hasContent).toList()))
            .withText(text).withContentAsHtml(html).withHref(href));
      }
      return (results);
   }



   /*******************************************************************************
    ** Tokenization preserves the assistant's existing ten-term contract.
    *******************************************************************************/
   public static Set<String> terms(String query) throws QBadRequestException
   {
      Set<String> terms = new LinkedHashSet<>();
      for(String word : Objects.requireNonNullElse(query, "").toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
      {
         if(word.length() >= 2 && terms.size() < MAX_TERMS)
         {
            terms.add(word);
         }
      }
      if(terms.isEmpty())
      {
         throw (new QBadRequestException("The query must contain at least one word of 2 or more characters."));
      }
      return (terms);
   }



   /*******************************************************************************
    ** Rank before limiting or truncating content, with deterministic tie breaks.
    *******************************************************************************/
   public static List<HelpSearchResult> rank(List<HelpSearchResult> candidates, String query, Integer limit) throws QBadRequestException
   {
      Set<String> terms = terms(query);
      int maximum = limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(MAX_LIMIT, limit));
      List<HelpSearchResult> results = candidates.stream().filter(candidate -> score(candidate, terms) > 0)
         .sorted(Comparator.comparingInt((HelpSearchResult candidate) -> -score(candidate, terms)).thenComparing(HelpSearchResult::getKind).thenComparing(HelpSearchResult::getKey))
         .limit(maximum).toList();
      for(HelpSearchResult result : results)
      {
         if(result.getText() != null && result.getText().length() > 600)
         {
            result.setText(result.getText().substring(0, 599) + "…");
         }
      }
      return (results);
   }



   /*******************************************************************************
    ** Label/name hits weigh three, summaries two, and long content one.
    *******************************************************************************/
   private static int score(HelpSearchResult candidate, Set<String> terms)
   {
      int score = 0;
      for(String term : terms)
      {
         score += contains(candidate.getLabel() + " " + candidate.getName(), term) ? 3 : 0;
         score += contains(candidate.getSearchSummary(), term) ? 2 : 0;
         score += contains(candidate.getText(), term) ? 1 : 0;
      }
      return (score);
   }



   /*******************************************************************************
    ** Case-insensitive term matching.
    *******************************************************************************/
   private static boolean contains(String text, String term)
   {
      return (text != null && text.toLowerCase(Locale.ROOT).contains(term));
   }
}
