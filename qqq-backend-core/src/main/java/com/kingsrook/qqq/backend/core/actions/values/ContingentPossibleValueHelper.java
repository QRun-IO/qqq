/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.values;


import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueDependencyBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueSearchFilterUseCase;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Shared dependency discovery for forms, bulk edit and write validation.
 *******************************************************************************/
public final class ContingentPossibleValueHelper
{
   private static final Pattern INPUT_VARIABLE = Pattern.compile("\\$\\{input\\.([^}]+)}");



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private ContingentPossibleValueHelper()
   {
   }



   /*******************************************************************************
    ** Explicit dependencies first, then filter dependencies in traversal order.
    *******************************************************************************/
   public static Set<String> getDependencyFieldNames(QFieldMetaData field)
   {
      Set<String> names = new LinkedHashSet<>(CollectionUtils.nonNullList(field.getPossibleValueSourceDependsOn()));
      collectDependencies(field.getPossibleValueSourceFilter(), names);
      return (names);
   }



   /*******************************************************************************
    ** Walk every criterion, including fallback expressions and nested filters.
    *******************************************************************************/
   private static void collectDependencies(QQueryFilter filter, Set<String> names)
   {
      if(filter == null)
      {
         return;
      }
      for(QFilterCriteria criterion : CollectionUtils.nonNullList(filter.getCriteria()))
      {
         for(Serializable value : CollectionUtils.nonNullList(criterion.getValues()))
         {
            if(value instanceof String expression)
            {
               Matcher matcher = INPUT_VARIABLE.matcher(expression);
               while(matcher.find())
               {
                  names.add(matcher.group(1));
               }
            }
         }
      }
      for(QQueryFilter subFilter : CollectionUtils.nonNullList(filter.getSubFilters()))
      {
         collectDependencies(subFilter, names);
      }
   }



   /*******************************************************************************
    ** Whether another input field controls this field's choices.
    *******************************************************************************/
   public static boolean isContingent(QFieldMetaData field)
   {
      return (!getDependencyFieldNames(field).isEmpty());
   }



   /*******************************************************************************
    ** Consistent case-insensitive parsing and form behavior for all UI endpoints.
    *******************************************************************************/
   public static PossibleValueSearchFilterUseCase getSearchUseCase(QFieldMetaData field, String useCase)
   {
      if("FILTER".equals(useCase == null ? null : useCase.toUpperCase(Locale.ROOT))
         || (field != null && field.getPossibleValueSourceDependencyBehavior() == PossibleValueDependencyBehavior.SHOW_ALL_UNTIL_SET))
      {
         return (PossibleValueSearchFilterUseCase.FILTER);
      }
      return (PossibleValueSearchFilterUseCase.FORM);
   }



   /*******************************************************************************
    ** Explicit dependencies cannot be enforced by filter interpretation. Keep
    ** this separate from inferred dependencies so process fallbacks still work.
    *******************************************************************************/
   public static boolean hasMissingExplicitDependency(QFieldMetaData field, Map<String, Serializable> values, String useCase)
   {
      if(getSearchUseCase(field, useCase) != PossibleValueSearchFilterUseCase.FORM)
      {
         return (false);
      }
      for(String name : CollectionUtils.nonNullList(field.getPossibleValueSourceDependsOn()))
      {
         Object value = values == null ? null : values.get(name);
         if(value == null || (value instanceof String string && string.isBlank()) || (value instanceof Collection<?> collection && collection.isEmpty()))
         {
            return (true);
         }
      }
      return (false);
   }
}
