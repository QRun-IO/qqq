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

package com.kingsrook.qqq.backend.core.actions.tables.helpers;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryOrCountInputInterface;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.CurrentUser;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAndJoinTable;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Resolve session values before backend-specific null and comparison semantics.
 *******************************************************************************/
public class CurrentUserFilterHelper
{
   /*******************************************************************************
    ** Copy only when necessary, so reusable filters retain their expressions.
    *******************************************************************************/
   public static QQueryFilter resolve(QueryOrCountInputInterface input, QQueryFilter filter) throws QException
   {
      if(filter == null)
      {
         return (null);
      }
      QQueryFilter result = filter;
      List<QFilterCriteria> criteria = new ArrayList<>();
      for(QFilterCriteria criterion : CollectionUtils.nonNullList(filter.getCriteria()))
      {
         QFilterCriteria replacement = criterion;
         if(CollectionUtils.nonNullList(criterion.getValues()).stream().anyMatch(value -> value instanceof CurrentUser || "${currentUser}".equals(value)))
         {
            replacement = criterion.clone();
            Serializable userId = new CurrentUser().evaluate(null);
            if(userId == null)
            {
               replacement.setOperator(QCriteriaOperator.FALSE);
               replacement.setValues(new ArrayList<>());
            }
            else
            {
               FieldAndJoinTable field = FieldAndJoinTable.get(input.getTable(), criterion.getFieldName(), input.getQueryJoins(), true);
               Serializable typedUserId = ValueUtils.getValueAsFieldType(field.joinTable(), field.field(), userId);
               replacement.getValues().replaceAll(value -> value instanceof CurrentUser || "${currentUser}".equals(value) ? typedUserId : value);
            }
            if(result == filter)
            {
               result = filter.clone();
            }
         }
         criteria.add(replacement);
      }
      List<QQueryFilter> subFilters = new ArrayList<>();
      for(QQueryFilter subFilter : CollectionUtils.nonNullList(filter.getSubFilters()))
      {
         QQueryFilter replacement = resolve(input, subFilter);
         subFilters.add(replacement);
         if(replacement != subFilter && result == filter)
         {
            result = filter.clone();
         }
      }
      if(result != filter)
      {
         result.setCriteria(criteria);
         result.setSubFilters(subFilters);
      }
      return (result);
   }
}
