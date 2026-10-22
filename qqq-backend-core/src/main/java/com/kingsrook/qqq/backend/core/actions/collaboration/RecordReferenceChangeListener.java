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

package com.kingsrook.qqq.backend.core.actions.collaboration;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeEvent;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeListenerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeType;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordReferenceSecurityConfig;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordReferenceTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.security.MultiRecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Parent lock propagation and after-commit cleanup for registered record references.
 *******************************************************************************/
public class RecordReferenceChangeListener implements RecordChangeListenerInterface
{
   private static final QLogger LOG = QLogger.getLogger(RecordReferenceChangeListener.class);



   /*******************************************************************************
    ** A stale security mirror must not silently survive a successful transaction.
    *******************************************************************************/
   @Override
   public boolean isRequired()
   {
      return (true);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public boolean appliesTo(String tableName, RecordChangeType type)
   {
      QTableMetaData table = QContext.getQInstance().getTable(tableName);
      RecordReferenceSecurityConfig config = RecordReferenceSecurityConfig.of(QContext.getQInstance());
      if(table == null || config == null || RecordReferenceTableMetaData.of(table) != null)
      {
         return (false);
      }
      if(type == RecordChangeType.UPDATE)
      {
         return (!config.getMirroredSecurityKeyTypes().isEmpty() && CollectionUtils.nullSafeHasContents(table.getRecordSecurityLocks()));
      }
      return (type == RecordChangeType.DELETE && !config.getCleanupProviders().isEmpty()
         && !RecordFeaturesResolver.resolve(QContext.getQInstance(), table).enabledFeatures().isEmpty());
   }



   /*******************************************************************************
    ** Propagate only derived lock columns through the backend, without re-running user edits or own-row checks.
    *******************************************************************************/
   public void onRecordsChanged(RecordChangeEvent event) throws QException
   {
      QInstance instance = QContext.getQInstance();
      RecordReferenceSecurityConfig config = RecordReferenceSecurityConfig.of(instance);
      if(config == null)
      {
         return;
      }
      QTableMetaData parentTable = instance.getTable(event.getTableName());
      if(event.getType() == RecordChangeType.DELETE)
      {
         List<Serializable> keys = event.getRecords().stream().map(record -> record.getValue(parentTable.getPrimaryKeyField())).toList();
         List<QCodeReference> providers = List.copyOf(config.getCleanupProviders());
         CapturedContext context = new CapturedContext(instance, QContext.getQSession());
         String tableName = event.getTableName();
         Runnable cleanup = () -> QContext.withTemporaryContext(context, () ->
         {
            for(QCodeReference provider : providers)
            {
               try
               {
                  QCodeLoader.getAdHoc(RecordReferenceCleanupInterface.class, provider).onParentsDeleted(tableName, keys);
               }
               catch(Exception e)
               {
                  LOG.warn("Record reference cleanup failed", e);
               }
            }
         });
         if(event.getTransaction() == null)
         {
            cleanup.run();
         }
         else
         {
            event.getTransaction().addAfterCommitCallback(cleanup);
         }
         return;
      }
      for(QRecord updated : event.getRecords())
      {
         Serializable key = updated.getValue(parentTable.getPrimaryKeyField());
         QRecord old = CollectionUtils.nonNullList(event.getOldRecords()).stream()
            .filter(Objects::nonNull)
            .filter(record -> Objects.equals(key, record.getValue(parentTable.getPrimaryKeyField()))).findFirst().orElse(null);
         Map<String, Serializable> values = new HashMap<>();
         collectChanges(parentTable.getRecordSecurityLocks(), updated, old, config, values);
         if(values.isEmpty())
         {
            continue;
         }
         for(QTableMetaData extension : CollectionUtils.nonNullMap(instance.getTables()).values())
         {
            RecordReferenceTableMetaData reference = RecordReferenceTableMetaData.of(extension);
            if(reference == null || !reference.getMirroredLocks())
            {
               continue;
            }
            Map<String, Serializable> applicableValues = new HashMap<>(values);
            applicableValues.keySet().retainAll(extension.getFields().keySet());
            if(applicableValues.isEmpty())
            {
               continue;
            }
            QueryInput query = new QueryInput(extension.getName()).withTransaction(event.getTransaction())
               .withFilter(new QQueryFilter(new QFilterCriteria(reference.getTableNameField(), QCriteriaOperator.EQUALS, parentTable.getName()),
                  new QFilterCriteria(reference.getRecordIdField(), QCriteriaOperator.EQUALS, key.toString())));
            query.setTableMetaData(extension);
            List<QRecord> rows = new QBackendModuleDispatcher().getQBackendModule(query.getBackend()).getQueryInterface().executeForDml(query).getRecords();
            List<QRecord> updates = new ArrayList<>();
            for(QRecord row : rows)
            {
               QRecord update = new QRecord().withValue(extension.getPrimaryKeyField(), row.getValue(extension.getPrimaryKeyField()));
               update.getValues().putAll(applicableValues);
               updates.add(update);
            }
            if(!updates.isEmpty())
            {
               UpdateInput input = new UpdateInput().withTableName(extension.getName()).withRecords(updates).withTransaction(event.getTransaction());
               input.setTableMetaData(extension);
               List<QRecord> results = new QBackendModuleDispatcher().getQBackendModule(input.getBackend()).getUpdateInterface().execute(input).getRecords();
               if(results.stream().anyMatch(record -> CollectionUtils.nullSafeHasContents(record.getErrors())))
               {
                  throw (new QException("Unable to update mirrored record security locks"));
               }
            }
         }
      }
   }



   /*******************************************************************************
    ** Use the successfully stored changes, including a move outside the actor's
    ** READ scope. Unchanged or unreturned fields retain their existing mirrors.
    *******************************************************************************/
   private static void collectChanges(List<RecordSecurityLock> locks, QRecord updated, QRecord old, RecordReferenceSecurityConfig config, Map<String, Serializable> values)
   {
      for(RecordSecurityLock lock : CollectionUtils.nonNullList(locks))
      {
         if(lock instanceof MultiRecordSecurityLock multi)
         {
            collectChanges(multi.getLocks(), updated, old, config, values);
         }
         else if(config.getMirroredSecurityKeyTypes().contains(lock.getSecurityKeyType()) && CollectionUtils.nullSafeIsEmpty(lock.getJoinNameChain())
            && updated.getValues().containsKey(lock.getFieldName())
            && (old == null || !old.getValues().containsKey(lock.getFieldName()) || !Objects.equals(old.getValue(lock.getFieldName()), updated.getValue(lock.getFieldName()))))
         {
            values.put(RecordReferenceSecurityConfig.lockFieldName(lock.getSecurityKeyType()), updated.getValue(lock.getFieldName()));
         }
      }
   }

}
