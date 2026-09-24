/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.tables.helpers;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.values.ValueBehaviorApplier;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Internal structural values for associated writes and their permission preflight.
 ** Record visibility and display customization cannot hide relationship values.
 ** These results must never replace access checks or enter public read responses.
 *******************************************************************************/
public class AssociatedRecordDiscovery
{
   /*******************************************************************************
    ** Resolve only the supplied parent's declared association and relationship values.
    *******************************************************************************/
   public static List<Serializable> findPrimaryKeys(QTableMetaData parentTable, Association association, List<Parent> parents, QBackendTransaction transaction) throws QException
   {
      AssociationJoin join = AssociationJoin.resolve(parentTable, association);
      QTableMetaData childTable = QContext.getQInstance().getTable(association.getAssociatedTableName());
      if(childTable.getField(childTable.getPrimaryKeyField()) == null)
      {
         throw new QException("Association child table has no primary key field");
      }
      if(parents.isEmpty())
      {
         return List.of();
      }

      QQueryFilter filter = new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR);
      for(Parent parent : parents)
      {
         QQueryFilter childFilter = new QQueryFilter();
         QRecord parentRecord = new QRecord();
         parentRecord.setValues(parent.values());
         List<Serializable> values = join.parentValues(parentRecord);
         for(int i = 0; i < join.getJoinOns().size(); i++)
         {
            childFilter.addCriteria(new QFilterCriteria(join.getJoinOns().get(i).getRightField(), QCriteriaOperator.EQUALS, Collections.singletonList(values.get(i))));
         }
         if(!parent.retainedPrimaryKeys().isEmpty())
         {
            childFilter.addCriteria(new QFilterCriteria(childTable.getPrimaryKeyField(), QCriteriaOperator.NOT_IN, parent.retainedPrimaryKeys()));
         }
         filter.addSubFilter(childFilter);
      }
      filter = ValueBehaviorApplier.applyFieldBehaviorsToFilter(QContext.getQInstance(), childTable, filter, Collections.emptySet());
      Input input = new Input(childTable, filter, transaction);
      List<Serializable> keys = new QBackendModuleDispatcher().getQBackendModule(QContext.getQInstance().getBackend(childTable.getBackendName()))
         .getQueryInterface().findAssociatedPrimaryKeys(input);
      Map<Object, Serializable> typedKeys = new LinkedHashMap<>();
      for(Serializable key : keys)
      {
         if(key == null)
         {
            throw new QException("Association discovery returned a null primary key");
         }
         Serializable typedKey = ValueUtils.getValueAsFieldType(childTable.getField(childTable.getPrimaryKeyField()).getType(), key);
         typedKeys.putIfAbsent(UniqueKeyLookup.equalityValue(typedKey), typedKey);
      }
      return new ArrayList<>(typedKeys.values());
   }



   /*******************************************************************************
    ** Retain the active write graph and routing while privately restoring physical
    ** fields removed from display metadata. Never modify either metadata source.
    *******************************************************************************/
   public static QTableMetaData physicalParentTable(QTableMetaData active) throws QException
   {
      QTableMetaData canonical = active == null ? null : QContext.getQInstance().getTable(active.getName());
      if(canonical == null || !Objects.equals(canonical.getBackendName(), active.getBackendName())
         || !Objects.equals(canonical.getPrimaryKeyField(), active.getPrimaryKeyField())
         || canonical.getPrimaryKeyField() == null || canonical.getFields().get(canonical.getPrimaryKeyField()) == null)
      {
         throw new QException("Association-value lookup requires the canonical backend and primary key");
      }
      QTableMetaData physical = active.clone();
      for(Map.Entry<String, QFieldMetaData> entry : canonical.getFields().entrySet())
      {
         if(!physical.getFields().containsKey(entry.getKey()))
         {
            physical.addField(entry.getValue().clone());
         }
      }
      return physical;
   }



   /*******************************************************************************
    ** Read exact stored parent keys and only their declared relationship fields.
    ** Missing rows remain absent; incomplete or unexpected provider rows fail closed.
    *******************************************************************************/
   public static List<QRecord> readParentValues(QTableMetaData activeParentTable, List<Association> associations,
      List<Serializable> primaryKeys, QBackendTransaction transaction) throws QException
   {
      QTableMetaData table = physicalParentTable(activeParentTable);
      Set<String> fields = new LinkedHashSet<>();
      fields.add(table.getPrimaryKeyField());
      for(Association association : associations)
      {
         for(JoinOn joinOn : AssociationJoin.resolve(table, association).getJoinOns())
         {
            String field = joinOn.getLeftField();
            if(QContext.getQInstance().getTable(table.getName()).getFields().get(field) == null)
            {
               throw new QException("Association-value lookup requires declared physical relationship fields");
            }
            fields.add(field);
         }
      }
      Map<Object, Serializable> keys = new LinkedHashMap<>();
      for(Serializable key : primaryKeys)
      {
         Serializable typedKey = typedStoredValue(table, table.getPrimaryKeyField(), key);
         if(typedKey == null)
         {
            throw new QException("Association-value lookup requires non-null primary keys");
         }
         keys.putIfAbsent(UniqueKeyLookup.equalityValue(typedKey), typedKey);
      }
      if(keys.isEmpty())
      {
         return List.of();
      }
      StoredValuesInput input = new StoredValuesInput(table, fields, new ArrayList<>(keys.values()), transaction);
      List<QRecord> records = new QBackendModuleDispatcher().getQBackendModule(QContext.getQInstance().getBackend(table.getBackendName()))
         .getQueryInterface().readAssociationValues(input);
      if(records == null)
      {
         throw new QException("Native association-value lookup returned no result list");
      }
      List<QRecord> result = new ArrayList<>();
      Set<Object> returnedKeys = new LinkedHashSet<>();
      for(QRecord record : records)
      {
         if(record == null || CollectionUtils.nullSafeHasContents(record.getErrors()))
         {
            throw new QException("Native association-value lookup returned a failed record");
         }
         QRecord projected = new QRecord();
         for(String field : fields)
         {
            if(!record.getValues().containsKey(field))
            {
               throw new QException("Native association-value lookup omitted a required field");
            }
            projected.setValue(field, typedStoredValue(table, field, record.getValue(field)));
         }
         Object key = UniqueKeyLookup.equalityValue(projected.getValue(table.getPrimaryKeyField()));
         if(!keys.containsKey(key) || !returnedKeys.add(key))
         {
            throw new QException("Native association-value lookup returned an unexpected or duplicate primary key");
         }
         result.add(projected);
      }
      return result;
   }



   /*******************************************************************************
    ** Stored values use physical parent types; assignment later uses child types.
    *******************************************************************************/
   private static Serializable typedStoredValue(QTableMetaData table, String field, Serializable value) throws QException
   {
      try
      {
         return UniqueKeyLookup.snapshot(ValueUtils.getValueAsFieldType(table.getFields().get(field).getType(), value));
      }
      catch(RuntimeException e)
      {
         throw new QException("Invalid stored association field value", e);
      }
   }



   /*******************************************************************************
    ** Only declared parent relationships can construct this exact-key projection.
    ** Adapters receive fresh snapshots, never a caller-bindable bypass flag.
    *******************************************************************************/
   public static final class StoredValuesInput
   {
      private final QTableMetaData table;
      private final Set<String> fields;
      private final List<Serializable> primaryKeys;
      private final Set<Object> equalityKeys;
      private final QBackendTransaction transaction;



      /*******************************************************************************
       **
       *******************************************************************************/
      private StoredValuesInput(QTableMetaData table, Set<String> fields, List<Serializable> primaryKeys, QBackendTransaction transaction)
      {
         this.table = table.clone();
         this.fields = Set.copyOf(fields);
         this.primaryKeys = primaryKeys.stream().map(UniqueKeyLookup::snapshot).toList();
         this.equalityKeys = new LinkedHashSet<>();
         this.primaryKeys.forEach(key -> equalityKeys.add(UniqueKeyLookup.equalityValue(key)));
         this.transaction = transaction;
      }



      /*******************************************************************************
       ** Native adapters alone omit record locks for this constrained lookup.
       *******************************************************************************/
      public QueryInput newQueryInput()
      {
         QQueryFilter filter = new QQueryFilter().withCriteria(new QFilterCriteria(table.getPrimaryKeyField(), QCriteriaOperator.IN,
            primaryKeys.stream().map(UniqueKeyLookup::snapshot).toList()));
         QueryInput input = new QueryInput(table.getName()).withFilter(filter).withTransaction(transaction)
            .withFieldNamesToInclude(new LinkedHashSet<>(fields)).withShouldFetchHeavyFields(true)
            .withShouldOmitHiddenFields(false).withShouldMaskPasswords(false);
         input.setTableMetaData(table.clone());
         return input;
      }



      /*******************************************************************************
       ** Collection adapters match only the exact typed target primary keys.
       *******************************************************************************/
      public boolean matches(QRecord record) throws QException
      {
         return equalityKeys.contains(UniqueKeyLookup.equalityValue(typedStoredValue(table, table.getPrimaryKeyField(), record.getValue(table.getPrimaryKeyField()))));
      }
   }



   /*******************************************************************************
    ** Snapshot values without rejecting explicit nulls or mutating caller records.
    *******************************************************************************/
   public record Parent(Map<String, Serializable> values, List<Serializable> retainedPrimaryKeys)
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      public Parent
      {
         values = Collections.unmodifiableMap(new HashMap<>(values));
         retainedPrimaryKeys = Collections.unmodifiableList(new ArrayList<>(retainedPrimaryKeys));
      }
   }



   /*******************************************************************************
    ** Only this helper constructs backend requests; no generic security-bypass flag
    ** or caller-supplied filter is accepted by this operation.
    *******************************************************************************/
   public static final class Input
   {
      private final QTableMetaData table;
      private final QQueryFilter filter;
      private final QBackendTransaction transaction;



      /*******************************************************************************
       **
       *******************************************************************************/
      private Input(QTableMetaData table, QQueryFilter filter, QBackendTransaction transaction)
      {
         this.table = table;
         this.filter = filter;
         this.transaction = transaction;
      }



      /*******************************************************************************
       ** Build a fresh ordinary input; native discovery adapters alone omit locks.
       *******************************************************************************/
      public QueryInput newQueryInput()
      {
         QueryInput queryInput = new QueryInput(table.getName()).withFilter(filter.clone())
            .withTransaction(transaction).withFieldNamesToInclude(Set.of(table.getPrimaryKeyField()))
            .withShouldOmitHiddenFields(false).withShouldFetchHeavyFields(true);
         queryInput.setTableMetaData(table);
         return queryInput;
      }
   }
}
