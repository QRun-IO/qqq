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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.instances.QInstanceHelpContentManager;
import com.kingsrook.qqq.backend.core.model.helpcontent.HelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.help.HelpContentVisibility;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Authoring coverage, including hidden targets, gated by help author permissions.
 *******************************************************************************/
public class HelpCoverageAction
{
   /*******************************************************************************
    ** Totals reflect kind/parent scope before missingOnly filters the entries.
    *******************************************************************************/
   public HelpCoverageOutput execute(HelpCoverageInput input) throws QException
   {
      ActionHelper.validateSession(input);
      if(QContext.getQInstance().getTable(HelpContent.TABLE_NAME) == null
         || (!PermissionsHelper.hasTablePermission(input, HelpContent.TABLE_NAME, TablePermissionSubType.INSERT)
         && !PermissionsHelper.hasTablePermission(input, HelpContent.TABLE_NAME, TablePermissionSubType.EDIT)))
      {
         throw (new QPermissionDeniedException("Permission to insert or edit helpContent is required."));
      }
      QInstanceHelpContentManager.reloadIfChanged(QContext.getQInstance());
      List<HelpCoverageEntry> entries = new ArrayList<>();
      Map<String, HelpCoverageTotal> totals = new TreeMap<>();
      for(HelpTarget target : HelpTargetCatalog.collect(QContext.getQInstance()))
      {
         if(target.key().contains(";slot:") && !"instance".equals(target.kind()))
         {
            String slot = "widget".equals(target.kind()) ? "label" : QHelpContent.SLOT_HELP;
            if(!target.key().equals(target.kind() + ":" + target.name() + ";slot:" + slot))
            {
               continue;
            }
         }
         if(StringUtils.hasContent(input.getKind()) && !input.getKind().equals(target.kind()))
         {
            continue;
         }
         if(StringUtils.hasContent(input.getParentKey()) && (target.parentKey() == null || !target.parentKey().startsWith(input.getParentKey())))
         {
            continue;
         }
         List<QHelpContent> contents = CollectionUtils.nonNullList(HelpContentVisibility.filter(target.contents()));
         boolean hasSummary = contents.stream().anyMatch(content -> StringUtils.hasContent(content.getSummary()));
         boolean hasContent = contents.stream().anyMatch(content -> StringUtils.hasContent(content.getContent()));
         HelpCoverageTotal total = totals.computeIfAbsent(target.kind(), kind -> new HelpCoverageTotal().withKind(kind));
         total.setTotal(total.getTotal() + 1);
         total.setWithHelp(total.getWithHelp() + (hasSummary || hasContent ? 1 : 0));
         if(!input.getMissingOnly() || (!hasSummary && !hasContent))
         {
            entries.add(new HelpCoverageEntry().withKind(target.kind()).withKey(target.key()).withLabel(target.label())
               .withParentKey(target.parentKey()).withHidden(target.hidden()).withHasSummary(hasSummary).withHasContent(hasContent)
               .withRoles(contents.stream().flatMap(content -> CollectionUtils.nonNullCollection(content.getRoles()).stream()).map(Object::toString).distinct().sorted().toList()));
         }
      }
      entries.sort(Comparator.comparing(HelpCoverageEntry::getKind).thenComparing(HelpCoverageEntry::getKey));
      return (new HelpCoverageOutput().withEntries(entries).withTotals(new ArrayList<>(totals.values())));
   }
}
