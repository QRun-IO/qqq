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
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Internal native key material for write validation. These results must never
 ** replace ordinary read/access checks or enter write patches and public outputs.
 *******************************************************************************/
public class UniqueKeyLookup
{
   /*******************************************************************************
    ** Fetch only declared key components for exact typed target primary keys.
    *******************************************************************************/
   public static List<QRecord> readStoredComponents(QTableMetaData activeTable, List<Serializable> typedPrimaryKeys, QBackendTransaction transaction) throws QException
   {
      if(typedPrimaryKeys.isEmpty())
      {
         return List.of();
      }
      Set<String> fields = declaredFields(activeTable);
      String primaryKey = activeTable.getPrimaryKeyField();
      requirePhysicalField(activeTable, primaryKey);
      fields.add(primaryKey);
      List<Serializable> keys = new ArrayList<>();
      for(Serializable key : typedPrimaryKeys)
      {
         Serializable typedKey = typedValue(activeTable, primaryKey, key);
         if(typedKey == null)
         {
            throw new QException("Unique-key lookup requires non-null primary keys");
         }
         keys.add(typedKey);
      }
      return execute(new Input(activeTable, fields, Map.of(primaryKey, keys), true, transaction));
   }



   /*******************************************************************************
    ** Keep all native matches associated with this one declared key predicate.
    ** A null component is distinct, matching the existing INSERT contract.
    *******************************************************************************/
   public static List<QRecord> findConflicts(QTableMetaData activeTable, UniqueKey declaredKey, QRecord candidate, QBackendTransaction transaction) throws QException
   {
      if(CollectionUtils.nonNullList(activeTable.getUniqueKeys()).stream().noneMatch(key -> Objects.equals(key.getFieldNames(), declaredKey.getFieldNames())))
      {
         throw new QException("Unique key is not declared by the active write table");
      }
      Map<String, List<Serializable>> values = new LinkedHashMap<>();
      boolean hasNull = false;
      for(String field : declaredKey.getFieldNames())
      {
         Serializable value = typedValue(activeTable, field, candidate.getValue(field));
         hasNull |= value == null;
         values.put(field, java.util.Collections.singletonList(value));
      }
      if(values.isEmpty())
      {
         throw new QException("Unique key has no fields");
      }
      if(hasNull)
      {
         return List.of();
      }
      Set<String> fields = new LinkedHashSet<>(values.keySet());
      if(activeTable.getPrimaryKeyField() != null)
      {
         requirePhysicalField(activeTable, activeTable.getPrimaryKeyField());
         fields.add(activeTable.getPrimaryKeyField());
      }
      return execute(new Input(activeTable, fields, values, false, transaction));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<QRecord> execute(Input input) throws QException
   {
      List<QRecord> records = new QBackendModuleDispatcher().getQBackendModule(QContext.getQInstance().getBackend(input.table.getBackendName()))
         .getQueryInterface().lookupUniqueKey(input);
      List<QRecord> result = new ArrayList<>();
      for(QRecord record : records)
      {
         if(CollectionUtils.nullSafeHasContents(record.getErrors()))
         {
            throw new QException("Native unique-key lookup returned a failed record");
         }
         QRecord projected = new QRecord();
         for(String field : input.fields)
         {
            if(!record.getValues().containsKey(field))
            {
               throw new QException("Native unique-key lookup omitted a required field");
            }
            projected.setValue(field, snapshot(typedValue(input.table, field, record.getValue(field))));
         }
         result.add(projected);
      }
      return result;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Set<String> declaredFields(QTableMetaData table) throws QException
   {
      Set<String> fields = new LinkedHashSet<>();
      for(UniqueKey key : CollectionUtils.nonNullList(table.getUniqueKeys()))
      {
         for(String field : key.getFieldNames())
         {
            requirePhysicalField(table, field);
            fields.add(field);
         }
      }
      if(fields.isEmpty())
      {
         throw new QException("Table has no declared unique-key fields");
      }
      return fields;
   }



   /*******************************************************************************
    ** Convert values using the declared physical field type, failing closed.
    *******************************************************************************/
   static Serializable typedValue(QTableMetaData table, String fieldName, Serializable value) throws QException
   {
      QFieldMetaData field = requirePhysicalField(table, fieldName);
      try
      {
         return ValueUtils.getValueAsFieldType(field.getType(), value);
      }
      catch(RuntimeException e)
      {
         throw new QException("Invalid value for unique-key validation", e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QFieldMetaData requirePhysicalField(QTableMetaData table, String name) throws QException
   {
      QTableMetaData canonical = QContext.getQInstance().getTable(table.getName());
      if(name == null || canonical == null || !Objects.equals(canonical.getBackendName(), table.getBackendName())
         || canonical.getFields().get(name) == null || table.getFields().get(name) == null)
      {
         throw new QException("Unique-key lookup requires a declared physical field");
      }
      return table.getFields().get(name);
   }



   /*******************************************************************************
    ** Java-side bookkeeping models typed numeric and binary equality, not arbitrary
    ** database collations. Native conflict results are never re-keyed by this rule.
    *******************************************************************************/
   static Object equalityValue(Serializable value)
   {
      if(value instanceof BigDecimal decimal)
      {
         return decimal.stripTrailingZeros();
      }
      if(value != null && value.getClass().isArray())
      {
         List<Object> elements = new ArrayList<>();
         for(int i = 0; i < Array.getLength(value); i++)
         {
            elements.add(equalityValue((Serializable) Array.get(value, i)));
         }
         return elements;
      }
      return value;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static Serializable snapshot(Serializable value)
   {
      if(value != null && value.getClass().isArray())
      {
         Object copy = Array.newInstance(value.getClass().getComponentType(), Array.getLength(value));
         for(int i = 0; i < Array.getLength(value); i++)
         {
            Array.set(copy, i, snapshot((Serializable) Array.get(value, i)));
         }
         return (Serializable) copy;
      }
      return value;
   }



   /*******************************************************************************
    ** Privately constructed, schema-owned lookup shapes only. Adapters receive a
    ** fresh snapshot; no caller-bindable security flag or arbitrary query is accepted.
    *******************************************************************************/
   public static final class Input
   {
      private final QTableMetaData table;
      private final Set<String> fields;
      private final Map<String, List<Serializable>> values;
      private final boolean primaryKeys;
      private final QBackendTransaction transaction;



      /*******************************************************************************
       **
       *******************************************************************************/
      private Input(QTableMetaData table, Set<String> fields, Map<String, List<Serializable>> values, boolean primaryKeys, QBackendTransaction transaction)
      {
         this.table = table.clone();
         this.fields = Set.copyOf(fields);
         this.values = new LinkedHashMap<>();
         values.forEach((field, fieldValues) -> this.values.put(field, fieldValues.stream().map(UniqueKeyLookup::snapshot).toList()));
         this.primaryKeys = primaryKeys;
         this.transaction = transaction;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public QueryInput newQueryInput()
      {
         QQueryFilter filter = new QQueryFilter();
         values.forEach((field, fieldValues) -> filter.addCriteria(new QFilterCriteria(field, primaryKeys ? QCriteriaOperator.IN : QCriteriaOperator.EQUALS,
            fieldValues.stream().map(UniqueKeyLookup::snapshot).toList())));
         QueryInput input = new QueryInput(table.getName()).withFilter(filter).withTransaction(transaction)
            .withFieldNamesToInclude(new LinkedHashSet<>(fields)).withShouldFetchHeavyFields(true)
            .withShouldOmitHiddenFields(false).withShouldMaskPasswords(false);
         input.setTableMetaData(table.clone());
         return input;
      }



      /*******************************************************************************
       ** Native collection adapters use metadata-typed equality on raw values.
       *******************************************************************************/
      public boolean matches(QRecord record) throws QException
      {
         for(Map.Entry<String, List<Serializable>> entry : values.entrySet())
         {
            Object actual = equalityValue(typedValue(table, entry.getKey(), record.getValue(entry.getKey())));
            if(entry.getValue().stream().noneMatch(value -> Objects.equals(actual, equalityValue(value))))
            {
               return false;
            }
         }
         return true;
      }
   }
}
