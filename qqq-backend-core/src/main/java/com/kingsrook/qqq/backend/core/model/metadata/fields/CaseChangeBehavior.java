/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.fields;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import com.kingsrook.qqq.backend.core.actions.values.ValueBehaviorApplier;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Field behavior that changes the case of string values.
 *******************************************************************************/
public enum CaseChangeBehavior implements FieldBehavior<CaseChangeBehavior>, FieldBehaviorForFrontend, FieldFilterBehavior<CaseChangeBehavior>
{
   NONE(null),
   TO_UPPER_CASE((String s) -> s.toUpperCase()),
   TO_LOWER_CASE((String s) -> s.toLowerCase());


   private final Function<String, String> function;



   /*******************************************************************************
    **
    *******************************************************************************/
   CaseChangeBehavior(Function<String, String> function)
   {
      this.function = function;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public CaseChangeBehavior getDefault()
   {
      return (NONE);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void apply(ValueBehaviorApplier.Action action, List<QRecord> recordList, QInstance instance, QTableMetaData table, QFieldMetaData field)
   {
      if(this.equals(NONE))
      {
         return;
      }

      switch(this)
      {
         case TO_UPPER_CASE, TO_LOWER_CASE -> applyFunction(recordList, table, field);
         default -> throw new IllegalStateException("Unexpected enum value: " + this);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void applyFunction(List<QRecord> recordList, QTableMetaData table, QFieldMetaData field)
   {
      String fieldName = field.getName();
      for(QRecord record : CollectionUtils.nonNullList(recordList))
      {
         String value = record.getValueString(fieldName);
         if(value != null && function != null)
         {
            record.setValue(fieldName, function.apply(value));
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Serializable applyToFilterCriteriaValue(Serializable value, QInstance instance, QTableMetaData table, QFieldMetaData field)
   {
      if(this.equals(NONE) || function == null)
      {
         return (value);
      }

      if(value instanceof String s)
      {
         String newValue = function.apply(s);
         if(!Objects.equals(value, newValue))
         {
            return (newValue);
         }
      }

      return (value);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean allowMultipleBehaviorsOfThisType()
   {
      return (false);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<String> validateBehaviorConfiguration(QTableMetaData tableMetaData, QFieldMetaData fieldMetaData)
   {
      if(this == NONE)
      {
         return Collections.emptyList();
      }

      List<String> errors      = new ArrayList<>();
      String       errorSuffix = " field [" + fieldMetaData.getName() + "]";
      if(tableMetaData != null)
      {
         errorSuffix += " in table [" + tableMetaData.getName() + "]";
      }

      if(fieldMetaData.getType() != null)
      {
         if(!fieldMetaData.getType().isStringLike())
         {
            errors.add("A CaseChange was a applied to a non-String-like field:" + errorSuffix);
         }
      }

      return (errors);
   }

}
