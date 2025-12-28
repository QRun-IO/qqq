/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
 ** Field behavior that changes the whitespace of string values.
 *******************************************************************************/
public enum WhiteSpaceBehavior implements FieldBehavior<WhiteSpaceBehavior>, FieldBehaviorForFrontend, FieldFilterBehavior<WhiteSpaceBehavior>
{
   NONE(null),
   REMOVE_ALL_WHITESPACE((String s) -> s.chars().filter(c -> !Character.isWhitespace(c)).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString()),
   TRIM((String s) -> s.trim()),
   TRIM_LEFT((String s) -> s.stripLeading()),
   TRIM_RIGHT((String s) -> s.stripTrailing());


   private final Function<String, String> function;



   /*******************************************************************************
    **
    *******************************************************************************/
   WhiteSpaceBehavior(Function<String, String> function)
   {
      this.function = function;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public WhiteSpaceBehavior getDefault()
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
         case REMOVE_ALL_WHITESPACE, TRIM, TRIM_LEFT, TRIM_RIGHT -> applyFunction(recordList, table, field);
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
            errors.add("A WhiteSpaceBehavior was a applied to a non-String-like field:" + errorSuffix);
         }
      }

      return (errors);
   }

}
