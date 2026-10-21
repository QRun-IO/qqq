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
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.interfaces.QueryInterface;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeType;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeaturesMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordReferenceSecurityConfig;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordReferenceTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.security.MultiRecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.NotFoundStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.PermissionDeniedMessage;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleInterface;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Core enforcement shared by all record-reference table actions; no session or all-access bypass.
 *******************************************************************************/
public class RecordReferenceSecurityHelper
{
   public static final String ADMIN_PERMISSION = "collaboration.admin";



   /*******************************************************************************
    ** Register the listener once, during enrichment or qbit installation.
    *******************************************************************************/
   public static void register(QInstance instance)
   {
      if(CollectionUtils.nonNullList(instance.getRecordChangeListeners()).stream().noneMatch(reference -> RecordReferenceChangeListener.class.getName().equals(reference.getName())))
      {
         instance.withRecordChangeListener(new QCodeReference(RecordReferenceChangeListener.class));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QQueryFilter secureFilter(QTableMetaData table, QQueryFilter filter, QBackendTransaction transaction) throws QException
   {
      RecordReferenceTableMetaData reference = RecordReferenceTableMetaData.of(table);
      if(reference == null)
      {
         return (filter);
      }
      QQueryFilter secured = filter == null ? new QQueryFilter() : filter.clone();
      List<Serializable> readableTables = new ArrayList<>();
      for(QTableMetaData parent : CollectionUtils.nonNullMap(QContext.getQInstance().getTables()).values())
      {
         if(RecordReferenceTableMetaData.of(parent) == null
            && RecordFeaturesResolver.resolve(QContext.getQInstance(), parent).isEnabled(reference.getFeature())
            && PermissionsHelper.hasTablePermission(new GetInput(), parent.getName(), TablePermissionSubType.READ))
         {
            readableTables.add(parent.getName());
         }
      }
      if(secured.getBooleanOperator() == QQueryFilter.BooleanOperator.AND)
      {
         String parentTable = null;
         for(QFilterCriteria criterion : secured.getCriteria())
         {
            if(reference.getTableNameField().equals(criterion.getFieldName()) && criterion.getOperator() == QCriteriaOperator.EQUALS && criterion.getValues().size() == 1)
            {
               parentTable = String.valueOf(criterion.getValues().get(0));
            }
         }
         if(parentTable != null)
         {
            for(QFilterCriteria criterion : secured.getCriteria())
            {
               if(reference.getRecordIdField().equals(criterion.getFieldName())
                  && (criterion.getOperator() == QCriteriaOperator.EQUALS || criterion.getOperator() == QCriteriaOperator.IN))
               {
                  List<Serializable> visible = new ArrayList<>();
                  for(Serializable primaryKey : criterion.getValues())
                  {
                     if(readableTables.contains(parentTable) && visibleParent(parentTable, primaryKey, transaction) != null)
                     {
                        visible.add(primaryKey);
                     }
                  }
                  criterion.setOperator(QCriteriaOperator.IN);
                  criterion.setValues(visible);
               }
            }
         }
      }
      // todo: spec 3.2 item 4 - post-filter unpinned queries for unmirrored parent locks, refill paging and mark approximate counts.
      QQueryFilter result = new QQueryFilter().withSubFilter(secured)
         .withCriteria(new QFilterCriteria(reference.getTableNameField(), QCriteriaOperator.IN, readableTables));
      result.setLimit(secured.getLimit());
      result.setSkip(secured.getSkip());
      result.setOrderBys(secured.getOrderBys());
      secured.setLimit(null);
      secured.setSkip(null);
      secured.setOrderBys(null);
      return (result);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QRecord visibleParent(String tableName, Serializable primaryKey, QBackendTransaction transaction) throws QException
   {
      QTableMetaData parent = QContext.getQInstance().getTable(tableName);
      if(parent == null || RecordReferenceTableMetaData.of(parent) != null || primaryKey == null
         || !PermissionsHelper.hasTablePermission(new GetInput(), tableName, TablePermissionSubType.READ))
      {
         return (null);
      }
      try
      {
         return (new GetAction().execute(new GetInput(tableName).withPrimaryKey(primaryKey).withTransaction(transaction)).getRecord());
      }
      catch(QPermissionDeniedException | QNotFoundException e)
      {
         return (null);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static boolean canReadReference(QTableMetaData table, QRecord record, QBackendTransaction transaction) throws QException
   {
      RecordReferenceTableMetaData reference = RecordReferenceTableMetaData.of(table);
      if(reference == null || record == null)
      {
         return (true);
      }
      record = storedRecord(table, record.getValue(table.getPrimaryKeyField()), transaction);
      String parentName = record.getValueString(reference.getTableNameField());
      QTableMetaData parent = QContext.getQInstance().getTable(parentName);
      return (parent != null && RecordFeaturesResolver.resolve(QContext.getQInstance(), parent).isEnabled(reference.getFeature())
         && visibleParent(parentName, record.getValue(reference.getRecordIdField()), transaction) != null);
   }



   /*******************************************************************************
    ** Validate stored and proposed parents, so moving a row cannot evade either policy. Always replace client-supplied mirror values.
    *******************************************************************************/
   public static void validateWrites(QTableMetaData table, List<QRecord> records, RecordChangeType type, QBackendTransaction transaction) throws QException
   {
      RecordReferenceTableMetaData reference = RecordReferenceTableMetaData.of(table);
      if(reference == null)
      {
         return;
      }
      for(QRecord record : CollectionUtils.nonNullList(records))
      {
         if(CollectionUtils.nullSafeHasContents(record.getErrors()))
         {
            continue;
         }
         QRecord stored = null;
         if(type != RecordChangeType.INSERT)
         {
            QRecord visible = new GetAction().execute(new GetInput(table.getName())
               .withPrimaryKey(record.getValue(table.getPrimaryKeyField())).withTransaction(transaction)).getRecord();
            if(visible == null)
            {
               record.addError(new NotFoundStatusMessage("No record was found"));
               continue;
            }
            stored = storedRecord(table, visible.getValue(table.getPrimaryKeyField()), transaction);
            if(reference.getAuthorField() != null && !Objects.equals(stored.getValueString(reference.getAuthorField()), currentUserId())
               && !QContext.getQSession().hasPermission(ADMIN_PERMISSION))
            {
               record.addError(new PermissionDeniedMessage("Permission denied."));
               continue;
            }
            if(!authorizeParent(reference, stored, record, transaction))
            {
               continue;
            }
         }
         QRecord effective = stored == null ? new QRecord(record) : new QRecord(stored);
         if(type != RecordChangeType.DELETE)
         {
            effective.getValues().putAll(record.getValues());
         }
         if(!authorizeParent(reference, effective, record, transaction))
         {
            continue;
         }
         if(type != RecordChangeType.DELETE && reference.getMirroredLocks())
         {
            QTableMetaData parentTable = QContext.getQInstance().getTable(effective.getValueString(reference.getTableNameField()));
            QRecord parent = storedRecord(parentTable, effective.getValue(reference.getRecordIdField()), transaction);
            copyMirroredLocks(parentTable, parent, table, record);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String currentUserId()
   {
      return (QContext.getQSession().getUser() == null ? null : QContext.getQSession().getUser().getIdReference());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static boolean authorizeParent(RecordReferenceTableMetaData reference, QRecord row, QRecord errors, QBackendTransaction transaction) throws QException
   {
      String parentName = row.getValueString(reference.getTableNameField());
      Serializable primaryKey = row.getValue(reference.getRecordIdField());
      QRecord parent = visibleParent(parentName, primaryKey, transaction);
      if(parent == null)
      {
         errors.addError(new NotFoundStatusMessage("No record was found"));
         return (false);
      }
      QTableMetaData table = QContext.getQInstance().getTable(parentName);
      if(!RecordFeaturesResolver.resolve(QContext.getQInstance(), table).isEnabled(reference.getFeature())
         || (reference.getFeature().getPermissionSuffix() != null
         && !RecordFeaturePermissions.checkRecordCapability(table, reference.getFeature(), parent, transaction)))
      {
         errors.addError(new PermissionDeniedMessage("Permission denied."));
         return (false);
      }
      return (true);
   }



   /*******************************************************************************
    ** Private stored values, fetched only after authorizing visibility. Presentation customizers cannot alter authorization or mirrors.
    *******************************************************************************/
   static QRecord storedRecord(QTableMetaData table, Serializable primaryKey, QBackendTransaction transaction) throws QException
   {
      QueryInput input = new QueryInput(table.getName()).withTransaction(transaction)
         .withFilter(new QQueryFilter(new QFilterCriteria(table.getPrimaryKeyField(), QCriteriaOperator.EQUALS, primaryKey)))
         .withShouldFetchHeavyFields(true).withShouldOmitHiddenFields(false).withShouldMaskPasswords(false)
         .withFieldNamesToInclude(new HashSet<>(table.getFields().keySet()));
      input.setTableMetaData(table);
      QBackendModuleInterface module = new QBackendModuleDispatcher().getQBackendModule(input.getBackend());
      QueryInterface queryInterface = null;
      try
      {
         queryInterface = module.getQueryInterface();
      }
      catch(IllegalStateException e)
      {
         // A parent may support GET without QUERY.
      }
      List<QRecord> records;
      if(queryInterface != null)
      {
         records = queryInterface.executeForDml(input).getRecords();
      }
      else
      {
         GetInput getInput = new GetInput(table.getName()).withPrimaryKey(primaryKey).withTransaction(transaction)
            .withShouldFetchHeavyFields(true).withShouldOmitHiddenFields(false).withShouldMaskPasswords(false);
         getInput.setTableMetaData(table);
         QRecord record = module.getGetInterface().execute(getInput).getRecord();
         records = record == null ? List.of() : List.of(record);
      }
      if(records == null || records.size() != 1 || CollectionUtils.nullSafeHasContents(records.get(0).getErrors()))
      {
         throw (new QNotFoundException("No record was found"));
      }
      return (records.get(0));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static Map<String, Serializable> mirroredValues(QTableMetaData parentTable, QRecord parent) throws QException
   {
      Map<String, Serializable> values = new HashMap<>();
      RecordReferenceSecurityConfig config = RecordReferenceSecurityConfig.of(QContext.getQInstance());
      if(config != null)
      {
         for(String keyType : config.getMirroredSecurityKeyTypes())
         {
            values.put(RecordReferenceSecurityConfig.lockFieldName(keyType), null);
         }
         collectMirroredValues(parentTable.getRecordSecurityLocks(), parent, config, values);
      }
      return (values);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void collectMirroredValues(List<RecordSecurityLock> locks, QRecord parent, RecordReferenceSecurityConfig config, Map<String, Serializable> values) throws QException
   {
      for(RecordSecurityLock lock : CollectionUtils.nonNullList(locks))
      {
         if(lock instanceof MultiRecordSecurityLock multi)
         {
            collectMirroredValues(multi.getLocks(), parent, config, values);
         }
         else if(config.getMirroredSecurityKeyTypes().contains(lock.getSecurityKeyType()) && CollectionUtils.nullSafeIsEmpty(lock.getJoinNameChain()))
         {
            if(!parent.getValues().containsKey(lock.getFieldName()))
            {
               throw (new QException("Missing stored parent security field"));
            }
            values.put(RecordReferenceSecurityConfig.lockFieldName(lock.getSecurityKeyType()), parent.getValue(lock.getFieldName()));
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void copyMirroredLocks(QTableMetaData parentTable, QRecord parent, QTableMetaData extension, QRecord row) throws QException
   {
      for(Map.Entry<String, Serializable> value : mirroredValues(parentTable, parent).entrySet())
      {
         if(extension.getFields().containsKey(value.getKey()))
         {
            row.setValue(value.getKey(), value.getValue());
         }
      }
   }

}
