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

package com.kingsrook.qqq.backend.core.actions.tables.helpers;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.values.ContingentPossibleValueHelper;
import com.kingsrook.qqq.backend.core.actions.values.SearchPossibleValueSourceAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueSearchFilterUseCase;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Validate user writes against dependent choices, batching by dependency values.
 *******************************************************************************/
public final class ValidateContingentPossibleValuesHelper
{
   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private ValidateContingentPossibleValuesHelper()
   {
   }



   /*******************************************************************************
    ** Null oldRecords means insert; updates may supply an empty list to fetch only
    ** missing values needed for touched dependent fields. Never mutate the input
    ** record to fill missing update fields, as doing so would expand the write.
    *******************************************************************************/
   public static void validate(QTableMetaData table, List<QRecord> records, List<QRecord> oldRecords, QBackendTransaction transaction) throws QException
   {
      List<QFieldMetaData> fields = table.getFields().values().stream().filter(ValidateContingentPossibleValuesHelper::shouldValidate).toList();
      if(fields.isEmpty() || CollectionUtils.nullSafeIsEmpty(records))
      {
         return;
      }

      Map<String, QRecord> oldById = new HashMap<>();
      for(QRecord oldRecord : CollectionUtils.nonNullList(oldRecords))
      {
         oldById.put(String.valueOf(oldRecord.getValue(table.getPrimaryKeyField())), oldRecord);
      }
      if(oldRecords != null)
      {
         fetchMissingOldValues(table, records, fields, oldById, transaction);
      }

      for(QFieldMetaData field : fields)
      {
         Set<String> dependencies = ContingentPossibleValueHelper.getDependencyFieldNames(field);
         Map<Map<String, Serializable>, List<QRecord>> groups = new LinkedHashMap<>();
         Map<QRecord, Map<String, Serializable>> effectiveValues = new IdentityHashMap<>();
         for(QRecord record : records)
         {
            if(oldRecords != null && !isTouched(record, field, dependencies))
            {
               continue;
            }
            Map<String, Serializable> values = new LinkedHashMap<>();
            QRecord oldRecord = oldById.get(String.valueOf(record.getValue(table.getPrimaryKeyField())));
            if(oldRecord != null)
            {
               values.putAll(oldRecord.getValues());
            }
            values.putAll(record.getValues());
            if(valueList(values.get(field.getName())).isEmpty())
            {
               continue;
            }
            effectiveValues.put(record, values);
            Map<String, Serializable> key = new LinkedHashMap<>();
            for(String dependency : dependencies)
            {
               key.put(dependency, values.get(dependency));
            }
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(record);
         }

         for(List<QRecord> group : groups.values())
         {
            Map<String, Serializable> values = effectiveValues.get(group.get(0));
            Set<Serializable> ids = new LinkedHashSet<>();
            for(QRecord record : group)
            {
               ids.addAll(valueList(effectiveValues.get(record).get(field.getName())));
            }
            QQueryFilter filter = field.getPossibleValueSourceFilter() == null ? null : field.getPossibleValueSourceFilter().clone();
            if(filter != null)
            {
               filter.interpretValues(Map.of("input", values), PossibleValueSearchFilterUseCase.FILTER);
            }
            SearchPossibleValueSourceInput searchInput = new SearchPossibleValueSourceInput()
               .withPossibleValueSourceName(field.getPossibleValueSourceName())
               .withIdList(new ArrayList<>(ids)).withLimit(ids.size())
               .withDefaultQueryFilter(filter).withOtherValues(values);
            Set<String> validIds = new SearchPossibleValueSourceAction().execute(searchInput).getResults().stream()
               .map(value -> String.valueOf(value.getId())).collect(Collectors.toSet());
            String dependencyLabels = dependencies.stream().map(name -> table.getFields().get(name) == null ? name : label(table.getFields().get(name))).collect(Collectors.joining(" and "));
            for(QRecord record : group)
            {
               for(Serializable value : valueList(effectiveValues.get(record).get(field.getName())))
               {
                  if(!validIds.contains(String.valueOf(value)))
                  {
                     record.addError(new BadInputStatusMessage(label(field) + " [" + value + "] is not a valid choice for the selected " + dependencyLabels));
                  }
               }
            }
         }
      }
   }



   /*******************************************************************************
    ** Fetch only records and columns needed to complete partial updates.
    *******************************************************************************/
   private static void fetchMissingOldValues(QTableMetaData table, List<QRecord> records, List<QFieldMetaData> fields, Map<String, QRecord> oldById, QBackendTransaction transaction) throws QException
   {
      Set<Serializable> ids = new LinkedHashSet<>();
      Set<String> columns = new LinkedHashSet<>();
      columns.add(table.getPrimaryKeyField());
      for(QRecord record : records)
      {
         for(QFieldMetaData field : fields)
         {
            Set<String> dependencies = ContingentPossibleValueHelper.getDependencyFieldNames(field);
            if(isTouched(record, field, dependencies))
            {
               Set<String> needed = new LinkedHashSet<>(dependencies);
               needed.add(field.getName());
               needed.retainAll(table.getFields().keySet());
               columns.addAll(needed);
               QRecord oldRecord = oldById.get(String.valueOf(record.getValue(table.getPrimaryKeyField())));
               needed.removeAll(record.getValues().keySet());
               if(oldRecord != null)
               {
                  needed.removeAll(oldRecord.getValues().keySet());
               }
               if(!needed.isEmpty() && record.getValue(table.getPrimaryKeyField()) != null)
               {
                  ids.add(record.getValue(table.getPrimaryKeyField()));
               }
            }
         }
      }
      if(!ids.isEmpty())
      {
         QueryInput query = new QueryInput().withTableName(table.getName()).withTransaction(transaction)
            .withFieldNamesToInclude(columns)
            .withFilter(new QQueryFilter(new QFilterCriteria(table.getPrimaryKeyField(), QCriteriaOperator.IN, new ArrayList<>(ids))));
         for(QRecord oldRecord : new QueryAction().execute(query).getRecords())
         {
            oldById.put(String.valueOf(oldRecord.getValue(table.getPrimaryKeyField())), oldRecord);
         }
      }
   }



   /*******************************************************************************
    ** Only opted-in contingent sources need validation.
    *******************************************************************************/
   private static boolean shouldValidate(QFieldMetaData field)
   {
      if(Boolean.FALSE.equals(field.getValidatePossibleValueSourceOnWrite()) || field.getPossibleValueSourceName() == null || !ContingentPossibleValueHelper.isContingent(field))
      {
         return (false);
      }
      QPossibleValueSource source = QContext.getQInstance().getPossibleValueSource(field.getPossibleValueSourceName());
      return (source != null && (source.getType() != QPossibleValueSourceType.ENUM || field.getPossibleValueSourceFilter() != null));
   }



   /*******************************************************************************
    ** A change to either side requires checking the resulting pair.
    *******************************************************************************/
   private static boolean isTouched(QRecord record, QFieldMetaData field, Set<String> dependencies)
   {
      return (record.getValues().containsKey(field.getName()) || dependencies.stream().anyMatch(record.getValues()::containsKey));
   }



   /*******************************************************************************
    ** Validate each nonempty member of a multi-value field.
    *******************************************************************************/
   private static List<Serializable> valueList(Serializable value)
   {
      Collection<?> values = value instanceof Collection<?> collection ? collection : Collections.singletonList(value);
      List<Serializable> result = new ArrayList<>();
      for(Object element : values)
      {
         if(element != null && !(element instanceof String string && string.isBlank()))
         {
            result.add((Serializable) element);
         }
      }
      return (result);
   }



   /*******************************************************************************
    ** Labels are optional before instance enrichment.
    *******************************************************************************/
   private static String label(QFieldMetaData field)
   {
      return (Objects.requireNonNullElse(field.getLabel(), field.getName()));
   }
}
