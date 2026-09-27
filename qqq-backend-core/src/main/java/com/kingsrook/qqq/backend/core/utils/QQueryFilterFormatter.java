/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.utils;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.values.SearchPossibleValueSourceAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceInput;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceOutput;
import com.kingsrook.qqq.backend.core.model.common.DayOfWeekPossibleValueSourceMetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAndJoinTable;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.WeekdayOfDateFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.WeekdayOfDateTimeFunction;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 **
 *******************************************************************************/
public class QQueryFilterFormatter
{
   private static final QLogger LOG = QLogger.getLogger(QQueryFilterFormatter.class);



   /***************************************************************************
    **
    ***************************************************************************/
   public static String formatQueryFilter(String tableName, QQueryFilter filter)
   {
      List<String>   parts = new ArrayList<>();
      QTableMetaData table = QContext.getQInstance().getTable(tableName);
      for(QFilterCriteria criteria : CollectionUtils.nonNullList(filter.getCriteria()))
      {
         parts.add(formatCriteria(table, criteria));
      }

      if(parts.isEmpty())
      {
         return ("Empty filter");
      }

      return String.join(" " + filter.getBooleanOperator() + " ", parts);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static String formatCriteria(QTableMetaData table, QFilterCriteria criteria)
   {
      String         fieldLabel = criteria.getFieldName();
      QFieldMetaData field      = null;
      QFieldType     fieldType  = null;
      try
      {
         FieldAndJoinTable fieldAndJoinTable = FieldAndJoinTable.get(table, criteria.getFieldName(), null, true);
         fieldLabel = fieldAndJoinTable.getLabel(table);
         field = fieldAndJoinTable.field();
         fieldType = field.getType();
      }
      catch(Exception e)
      {
         LOG.debug("Error getting field label for formatting criteria", e, logPair("fieldName", criteria.getFieldName()));
      }

      boolean isTemporal = fieldType != null && fieldType.isTemporal();

      StringBuilder rs = new StringBuilder(fieldLabel);

      switch(criteria.getOperator())
      {
         case EQUALS -> rs.append(" equals ");
         case NOT_EQUALS, NOT_EQUALS_OR_IS_NULL -> rs.append(" does not equal ");
         case IN ->
         {
            if(hasWeekdayFunction(criteria))
            {
               rs.append(" day");
            }
            rs.append(" is any of ");
         }
         case NOT_IN ->
         {
            if(hasWeekdayFunction(criteria))
            {
               rs.append(" day");
            }
            rs.append(" is none of ");
         }
         case IS_NULL_OR_IN -> rs.append(" is blank or any of ");
         case LIKE -> rs.append(" is like ");
         case NOT_LIKE -> rs.append(" is not like ");
         case STARTS_WITH -> rs.append(" starts with ");
         case ENDS_WITH -> rs.append(" ends with ");
         case CONTAINS -> rs.append(" contains ");
         case NOT_STARTS_WITH -> rs.append(" does not start with ");
         case NOT_ENDS_WITH -> rs.append(" does not end with ");
         case NOT_CONTAINS -> rs.append(" does not contain ");
         case LESS_THAN -> rs.append(isTemporal ? " is before " : " is less than ");
         case LESS_THAN_OR_EQUALS -> rs.append(isTemporal ? " is before or at " : " is less than or equal to ");
         case GREATER_THAN -> rs.append(isTemporal ? " is after " : " is greater than ");
         case GREATER_THAN_OR_EQUALS -> rs.append(isTemporal ? " is after or at " : " is greater than or equal to ");
         case IS_BLANK -> rs.append(" is empty ");
         case IS_NOT_BLANK -> rs.append(" is not empty ");
         case BETWEEN -> rs.append(" is between ");
         case NOT_BETWEEN -> rs.append(" is not ");
         case TRUE -> rs.append(" is True ");
         case FALSE -> rs.append(" is False ");
         default -> rs.append(" ").append(StringUtils.allCapsToMixedCase(String.valueOf(criteria.getOperator())).replaceAll("_", " ")).append(" ");
      }

      List<Serializable> values = criteria.getValues();
      if(values.size() == 1)
      {
         rs.append(formatValue(field, criteria, values.get(0)));
      }
      else if(values.size() == 2)
      {
         rs.append(formatValue(field, criteria, values.get(0))).append(" and ").append(formatValue(field, criteria, values.get(1)));
      }
      else if(values.size() > 1)
      {
         rs.append(formatValue(field, criteria, values.get(0))).append(" and ").append(values.size() - 1).append(" other values");
      }

      return (rs.toString());
   }


   /***************************************************************************
    *
    ***************************************************************************/
   private static boolean hasWeekdayFunction(QFilterCriteria criteria)
   {
      String functionTypeIdentifier = ObjectUtils.tryElse(() -> criteria.getFieldFunction().getFunctionTypeIdentifier().getName(), null);
      return WeekdayOfDateTimeFunction.IDENTIFIER.getName().equals(functionTypeIdentifier) || WeekdayOfDateFunction.IDENTIFIER.getName().equals(functionTypeIdentifier);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static String formatValue(QFieldMetaData field, QFilterCriteria criteria, Serializable value)
   {
      try
      {
         String possibleValueSourceName = (field == null ? null : field.getPossibleValueSourceName());
         if(hasWeekdayFunction(criteria))
         {
            possibleValueSourceName = DayOfWeekPossibleValueSourceMetaDataProducer.NAME;
         }

         if(StringUtils.hasContent(possibleValueSourceName))
         {
            SearchPossibleValueSourceOutput searchPossibleValueSourceOutput = new SearchPossibleValueSourceAction().execute(new SearchPossibleValueSourceInput()
               .withIdList(List.of(value))
               .withPossibleValueSourceName(possibleValueSourceName));
            if(CollectionUtils.nullSafeHasContents(searchPossibleValueSourceOutput.getResults()))
            {
               return searchPossibleValueSourceOutput.getResults().get(0).getLabel();
            }
         }
      }
      catch(Exception e)
      {
         LOG.debug("Error getting formatting value for criteria", e, logPair("field", () -> field.getName()), logPair("value", value));
      }
      return ValueUtils.getValueAsString(value);
   }

}
