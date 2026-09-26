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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldDisplayBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldFilterBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Utility class to apply value behaviors to records.
 *******************************************************************************/
public class ValueBehaviorApplier
{

   /*******************************************************************************
    **
    *******************************************************************************/
   public enum Action
   {
      INSERT,
      UPDATE,
      READ,
      FORMATTING
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void applyFieldBehaviors(Action action, QInstance instance, QTableMetaData table, List<QRecord> recordList, Set<FieldBehavior<?>> behaviorsToOmit)
   {
      if(CollectionUtils.nullSafeIsEmpty(recordList))
      {
         return;
      }

      for(QFieldMetaData field : table.getFields().values())
      {
         for(FieldBehavior<?> fieldBehavior : CollectionUtils.nonNullCollection(field.getBehaviors()))
         {
            boolean applyBehavior = true;
            if(behaviorsToOmit != null && behaviorsToOmit.contains(fieldBehavior))
            {
               /////////////////////////////////////////////////////////////////////////////////////////
               // if we're given a set of behaviors to omit, and this behavior is in there, then skip //
               /////////////////////////////////////////////////////////////////////////////////////////
               applyBehavior = false;
            }

            if(Action.FORMATTING == action && !(fieldBehavior instanceof FieldDisplayBehavior<?>))
            {
               ////////////////////////////////////////////////////////////////////////////////////////////////
               // for the formatting action, do not apply the behavior unless it is a field-display-behavior //
               ////////////////////////////////////////////////////////////////////////////////////////////////
               applyBehavior = false;
            }
            else if(Action.FORMATTING != action && fieldBehavior instanceof FieldDisplayBehavior<?>)
            {
               /////////////////////////////////////////////////////////////////////////////////////////////
               // for non-formatting actions, do not apply the behavior IF it is a field-display-behavior //
               /////////////////////////////////////////////////////////////////////////////////////////////
               applyBehavior = false;
            }

            if(applyBehavior)
            {
               fieldBehavior.apply(action, recordList, instance, table, field);
            }
         }
      }
   }



   /*******************************************************************************
    ** apply field behaviors (of FieldFilterBehavior type) to a QQueryFilter.
    ** note that, we don't like to ever edit a QQueryFilter itself (e.g., as it might
    ** have come from meta-data, or it might have some immutable structures in it).
    ** So, if any changes are needed, they'll be returned in a clone.
    ** So, either way, you should use this method like:
    *
    ** QQueryFilter myFilter = // wherever I got my filter from
    ** myFilter = ValueBehaviorApplier.applyFieldBehaviorsToFilter(QContext.getInstance, table, myFilter, null);
    ** // e.g., always re-assign over top of your filter.
    *******************************************************************************/
   public static QQueryFilter applyFieldBehaviorsToFilter(QInstance instance, QTableMetaData table, QQueryFilter filter, Set<FieldBehavior<?>> behaviorsToOmit)
   {
      ////////////////////////////////////////////////
      // for null or empty filter, return the input //
      ////////////////////////////////////////////////
      if(filter == null || !filter.hasAnyCriteria())
      {
         return (filter);
      }

      ///////////////////////////////////////////////////////////////////
      // track if we need to make & return a clone.                    //
      // which will be the case if we get back any different criteria, //
      // or any different sub-filters, than what we originally had.    //
      ///////////////////////////////////////////////////////////////////
      boolean needToUseClone = false;

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // make a new criteria list, and a new subFilter list - either null, if the source was null, or a new array list //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      List<QFilterCriteria> newCriteriaList = filter.getCriteria() == null ? null : new ArrayList<>();
      List<QQueryFilter>    newSubFilters   = filter.getSubFilters() == null ? null : new ArrayList<>();

      //////////////////////////////////////////////////////////////////////////////
      // for each criteria, if its field has any applicable behaviors, apply them //
      //////////////////////////////////////////////////////////////////////////////
      for(QFilterCriteria criteria : CollectionUtils.nonNullList(filter.getCriteria()))
      {
         QFieldMetaData field = table.getFields().get(criteria.getFieldName());
         if(field == null && criteria.getFieldName() != null && criteria.getFieldName().contains("."))
         {
            String[] parts = criteria.getFieldName().split("\\.");
            if(parts.length == 2)
            {
               QTableMetaData joinTable = instance.getTable(parts[0]);
               if(joinTable != null)
               {
                  field = joinTable.getFields().get(parts[1]);
               }
            }
         }

         QFilterCriteria criteriaToUse = criteria;
         if(field != null)
         {
            for(FieldBehavior<?> fieldBehavior : CollectionUtils.nonNullCollection(field.getBehaviors()))
            {
               boolean applyBehavior = true;
               if(behaviorsToOmit != null && behaviorsToOmit.contains(fieldBehavior))
               {
                  applyBehavior = false;
               }

               if(applyBehavior && fieldBehavior instanceof FieldFilterBehavior<?> filterBehavior)
               {
                  //////////////////////////////////////////////////////////////////////
                  // call to apply the behavior on the criteria - which will return a //
                  // new criteria if any values are changed, else the input criteria  //
                  //////////////////////////////////////////////////////////////////////
                  criteriaToUse = apply(criteriaToUse, instance, table, field, filterBehavior);

                  ////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  // if the new criteria is not the same as the old criteria, mark that we need to make and return a clone. //
                  ////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  if(criteriaToUse != criteria)
                  {
                     needToUseClone = true;
                  }
               }
            }
         }

         newCriteriaList.add(criteriaToUse);
      }

      /////////////////////////////////////////////////////////////////////////////////////////////////
      // similar to above - iterate over the subfilters, making a recursive call, and tracking if we //
      // got back the same object (in which case, there are no changes, and we don't need to clone), //
      // or a different object (in which case, we do need a clone, because there were changes).      //
      /////////////////////////////////////////////////////////////////////////////////////////////////
      for(QQueryFilter subFilter : CollectionUtils.nonNullList(filter.getSubFilters()))
      {
         QQueryFilter newSubFilter = applyFieldBehaviorsToFilter(instance, table, subFilter, behaviorsToOmit);
         if(newSubFilter != subFilter)
         {
            newSubFilters.add(newSubFilter);
            needToUseClone = true;
         }
         else
         {
            newSubFilters.add(subFilter);
         }
      }

      //////////////////////////////////////////////////////////////////////////////////////////////////
      // if we need to return a clone, then do so, replacing the lists with the ones we built in here //
      //////////////////////////////////////////////////////////////////////////////////////////////////
      if(needToUseClone)
      {
         QQueryFilter cloneFilter = filter.clone();
         cloneFilter.setCriteria(newCriteriaList);
         cloneFilter.setSubFilters(newSubFilters);
         return (cloneFilter);
      }

      /////////////////////////////////////////////////////////////////////////////
      // else, if no clone needed (e.g., no changes), return the original filter //
      /////////////////////////////////////////////////////////////////////////////
      return (filter);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QFilterCriteria apply(QFilterCriteria criteria, QInstance instance, QTableMetaData table, QFieldMetaData field, FieldFilterBehavior<?> filterBehavior)
   {
      if(criteria == null || CollectionUtils.nullSafeIsEmpty(criteria.getValues()))
      {
         return (criteria);
      }

      List<Serializable> newValues  = new ArrayList<>();
      boolean            changedAny = false;

      for(Serializable value : criteria.getValues())
      {
         Serializable newValue = filterBehavior.applyToFilterCriteriaValue(value, instance, table, field);
         if(!Objects.equals(value, newValue))
         {
            newValues.add(newValue);
            changedAny = true;
         }
         else
         {
            newValues.add(value);
         }
      }

      if(changedAny)
      {
         QFilterCriteria clone = criteria.clone();
         clone.setValues(newValues);
         return (clone);
      }
      else
      {
         return (criteria);
      }
   }

}
