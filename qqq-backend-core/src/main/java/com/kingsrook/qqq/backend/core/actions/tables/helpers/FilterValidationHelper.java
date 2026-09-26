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

package com.kingsrook.qqq.backend.core.actions.tables.helpers;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryOrCountInputInterface;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByAggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByGroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAndJoinTable;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 * Utility to help query action validate the fieldNames in a filter.
 *******************************************************************************/
public class FilterValidationHelper
{
   private static final QLogger LOG = QLogger.getLogger(FilterValidationHelper.class);


   /***************************************************************************
    * throw an exception if a filter contains any field names (in its criteria
    * or orderBys) that aren't in the input table (which may be user-personalized).
    ***************************************************************************/
   public static void validateFieldNamesInFilter(QueryOrCountInputInterface input) throws QException
   {
      if(input.getFilter() == null)
      {
         ///////////////////////////////////////
         // if no filter, nothing to validate //
         ///////////////////////////////////////
         return;
      }

      List<String> unrecognizedFieldNames = new ArrayList<>();

      validateFieldNamesInFilterInner(input, input.getFilter(), input.getTable(), new HashMap<>(), unrecognizedFieldNames);

      if(!unrecognizedFieldNames.isEmpty())
      {
         throw (new QUserFacingException("Query Filter contained " + unrecognizedFieldNames.size() + " unrecognized field name" + StringUtils.plural(unrecognizedFieldNames) + ": " + StringUtils.join(",", unrecognizedFieldNames)));
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private static void validateFieldNamesInFilterInner(QueryOrCountInputInterface queryOrCountInputInterface, QQueryFilter filter, QTableMetaData mainTable, Map<String, QTableMetaData> joinTables, List<String> unrecognizedFieldNames) throws QException
   {
      if(filter == null)
      {
         throw (new QUserFacingException("Query Filter contained a null subfilter"));
      }
      for(QFilterCriteria criteria : CollectionUtils.nonNullList(filter.getCriteria()))
      {
         if(criteria == null || !StringUtils.hasContent(criteria.getFieldName()) || criteria.getOperator() == null)
         {
            throw (new QUserFacingException("Query Filter criteria must specify a field name and operator"));
         }
         validateFieldNameFromFilter(criteria.getFieldName(), queryOrCountInputInterface, mainTable, joinTables, unrecognizedFieldNames);
         validateFieldNameFromFilter(criteria.getOtherFieldName(), queryOrCountInputInterface, mainTable, joinTables, unrecognizedFieldNames);
      }

      for(QFilterOrderBy orderBy : CollectionUtils.nonNullList(filter.getOrderBys()))
      {
         String fieldName = orderBy == null ? null : orderBy.getFieldName();
         if(orderBy instanceof QFilterOrderByAggregate orderByAggregate)
         {
            fieldName = orderByAggregate.getAggregate() == null ? null : orderByAggregate.getAggregate().getFieldName();
         }
         else if(orderBy instanceof QFilterOrderByGroupBy orderByGroupBy)
         {
            fieldName = orderByGroupBy.getGroupBy() == null ? null : orderByGroupBy.getGroupBy().getFieldName();
         }
         if(!StringUtils.hasContent(fieldName))
         {
            throw (new QUserFacingException("Query Filter order by must specify a field name"));
         }
         if(orderBy instanceof QFilterOrderByAggregate || orderBy instanceof QFilterOrderByGroupBy)
         {
            unrecognizedFieldNames.addAll(SelectionValidationHelper.getUnrecognizedFieldNames(queryOrCountInputInterface, Set.of(fieldName)));
         }
         else
         {
            validateFieldNameFromFilter(fieldName, queryOrCountInputInterface, mainTable, joinTables, unrecognizedFieldNames);
         }
      }

      for(QQueryFilter subFilter : CollectionUtils.nonNullList(filter.getSubFilters()))
      {
         validateFieldNamesInFilterInner(queryOrCountInputInterface, subFilter, mainTable, joinTables, unrecognizedFieldNames);
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private static void validateFieldNameFromFilter(String fieldName, QueryOrCountInputInterface input, QTableMetaData mainTable, Map<String, QTableMetaData> joinTables, List<String> unrecognizedFieldNames)
   {
      if(StringUtils.hasContent(fieldName))
      {
         boolean found = false;
         try
         {
            FieldAndJoinTable fieldAndJoinTable = FieldAndJoinTable.get(mainTable, fieldName, input.getQueryJoins(), true);
            if(fieldAndJoinTable.joinTable().getName().equals(mainTable.getName()))
            {
               found = isFieldNameFoundAndAllowedForFilter(fieldAndJoinTable, mainTable);
            }
            else
            {
               String         joinTableName = fieldAndJoinTable.joinTable().getName();
               QTableMetaData joinTable     = joinTables.get(joinTableName);
               if(joinTable == null)
               {
                  QTableMetaData table = fieldAndJoinTable.joinTable();
                  joinTable = TableMetaDataPersonalizerAction.execute(new TableMetaDataPersonalizerInput().withTableMetaData(table).withInputSource(input.getInputSource()));
                  joinTables.put(joinTableName, joinTable);
               }

               found = isFieldNameFoundAndAllowedForFilter(fieldAndJoinTable, joinTable);
            }
         }
         catch(Exception e)
         {
            ///////////////////////
            // leave found false //
            ///////////////////////
         }

         if(!found)
         {
            unrecognizedFieldNames.add(fieldName);
         }
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private static boolean isFieldNameFoundAndAllowedForFilter(FieldAndJoinTable fieldAndJoinTable, QTableMetaData table)
   {
      QFieldMetaData field = fieldAndJoinTable.field();
      if(field instanceof QVirtualFieldMetaData virtualField)
      {
         if(!virtualField.getIsQueryCriteria())
         {
            LOG.info("Query Filter contained a Virtual Field that is not allowed in a query criteria", logPair("fieldName", virtualField.getName()));
            return (false);
         }

         return table.getVirtualFields().containsKey(virtualField.getName());
      }
      else
      {
         return table.getFields().containsKey(field.getName());
      }
   }



}
