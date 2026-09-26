/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.tables;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.AbstractQActionFunction;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.AssociatedRecordUpdate;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.UniqueKeyHelper;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.replace.ReplaceInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.replace.ReplaceOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;


/*******************************************************************************
 ** Action to do a "replace" - e.g: Update rows with unique-key values that are
 ** already in the table; insert rows whose unique keys weren't already in the
 ** table, and delete rows that weren't in the input (all based on a
 ** UniqueKey that's part of the input)
 **
 ** Note - the filter in the ReplaceInput - its role is to limit what rows are
 ** potentially deleted.  e.g., if you have a table that's segmented, and you're
 ** only replacing a particular segment of it (say, for 1 client), then you pass
 ** in a filter that finds  rows matching that segment.  See Test for example.
 *******************************************************************************/
public class ReplaceAction extends AbstractQActionFunction<ReplaceInput, ReplaceOutput>
{
   private static final QLogger LOG = QLogger.getLogger(ReplaceAction.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public ReplaceOutput execute(ReplaceInput input) throws QException
   {
      ReplaceOutput output = new ReplaceOutput();

      QBackendTransaction transaction         = input.getTransaction();
      boolean             weOwnTheTransaction = false;

      try
      {
         ActionHelper.validateSession(input);
         QTableMetaData table                     = TableMetaDataPersonalizerAction.execute(input);
         if(table == null)
         {
            throw new QException("Replace requires a known table");
         }
         UniqueKey      uniqueKey                 = input.getKey();
         String         primaryKeyField           = table.getPrimaryKeyField();
         boolean        allowNullKeyValuesToEqual = BooleanUtils.isTrue(input.getAllowNullKeyValuesToEqual());

         if(transaction == null)
         {
            InsertInput transactionInput = new InsertInput(table.getName());
            transactionInput.setTableMetaData(table);
            transactionInput.setInputSource(input.getInputSource());
            transaction = QBackendTransaction.openFor(transactionInput);
            weOwnTheTransaction = true;
         }

         List<QRecord>      insertList        = new ArrayList<>();
         List<QRecord>      updateList        = new ArrayList<>();
         List<Serializable> primaryKeysToKeep = new ArrayList<>();

         Map<QRecord, Serializable> existingKeys = UniqueKeyHelper.findMatches(table, input.getRecords(), uniqueKey,
            allowNullKeyValuesToEqual, input.getInputSource(), transaction);
         Map<Object, QRecord> callerRecords = new HashMap<>();
         Set<Object> callerKeys = new HashSet<>();
         for(QRecord record : input.getRecords())
         {
            callerRecords.put(record.startCopyTracking(), record);
            if(record.getValue(primaryKeyField) != null)
            {
               callerKeys.add(AssociatedRecordUpdate.primaryKey(table, new QRecord().withValue(primaryKeyField, record.getValue(primaryKeyField))));
            }
         }
         boolean privateKey = isPrivatePrimaryKey(table);
         for(QRecord record : input.getRecords())
         {
            QRecord workingRecord = privateKey ? new QRecord(record) : record;
            if(existingKeys.containsKey(record))
            {
               Serializable primaryKey = existingKeys.get(record);
               workingRecord.setValue(primaryKeyField, primaryKey);
               if(privateKey)
               {
                  workingRecord.capturePrimaryKey(table, primaryKey);
               }
               publishPrimaryKey(table, record, primaryKey, callerKeys);
               updateList.add(workingRecord);
               primaryKeysToKeep.add(primaryKey);
            }
            else
            {
               insertList.add(workingRecord);
            }
         }

         InsertInput insertInput = new InsertInput();
         insertInput.setTableName(table.getName());
         insertInput.setTableMetaData(table);
         insertInput.setInputSource(input.getInputSource());
         insertInput.setRecords(insertList);
         insertInput.withFlags(input.getFlags());
         insertInput.setTransaction(transaction);
         insertInput.setOmitDmlAudit(input.getOmitDmlAudit());
         InsertAction.ReplaceResult insertResult = new InsertAction().executeForReplace(insertInput, input.getSetPrimaryKeyInInsertedRecords());
         InsertOutput insertOutput = insertResult.output();
         requireSuccessfulRecords(insertResult.nativeRecords(), "INSERT");
         requireSuccessfulRecords(insertOutput.getRecords(), "INSERT");
         validateInsertedIdentities(table, insertResult.nativeRecords(), insertOutput.getRecords());
         Map<Object, Serializable> insertedKeys = new HashMap<>();
         for(QRecord record : insertResult.nativeRecords())
         {
            Serializable primaryKey = record.resolvePrimaryKey(table);
            primaryKeysToKeep.add(primaryKey);
            if(insertedKeys.put(record.trackCopies(), primaryKey) != null)
            {
               throw new QException("Replace INSERT returned duplicate record origins");
            }
         }
         if(input.getSetPrimaryKeyInInsertedRecords()
            && (insertedKeys.size() != insertList.size() || insertList.stream().anyMatch(record -> !insertedKeys.containsKey(record.trackCopies()))))
         {
            throw new QException("Replace INSERT cannot correlate native results with its original input");
         }
         sanitizeDiscoveredKeys(table, insertOutput.getRecords(), callerKeys);
         output.setInsertOutput(insertOutput);

         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(table.getName());
         updateInput.setTableMetaData(table);
         updateInput.setInputSource(input.getInputSource());
         updateInput.setRecords(updateList);
         updateInput.withFlags(input.getFlags());
         updateInput.setTransaction(transaction);
         updateInput.setOmitDmlAudit(input.getOmitDmlAudit());
         UpdateOutput updateOutput = new UpdateAction().execute(updateInput, true);
         requireSuccessfulRecords(updateOutput.getRecords(), "UPDATE");
         sanitizeDiscoveredKeys(table, updateOutput.getRecords(), callerKeys);
         output.setUpdateOutput(updateOutput);

         if(input.getPerformDeletes())
         {
            QQueryFilter deleteFilter = omissionFilter(input.getFilter(), primaryKeyField, primaryKeysToKeep);

            DeleteInput deleteInput = new DeleteInput();
            deleteInput.setTableName(table.getName());
            deleteInput.setTableMetaData(table);
            deleteInput.setInputSource(input.getInputSource());
            deleteInput.setQueryFilter(deleteFilter);
            deleteInput.withFlags(input.getFlags());
            deleteInput.setTransaction(transaction);
            deleteInput.setOmitDmlAudit(input.getOmitDmlAudit());
            DeleteOutput deleteOutput = new DeleteAction().execute(deleteInput);
            requireSuccessfulRecords(deleteOutput.getRecordsWithErrors(), "DELETE");
            output.setDeleteOutput(deleteOutput);
         }

         if(input.getSetPrimaryKeyInInsertedRecords())
         {
            for(QRecord record : insertList)
            {
               publishPrimaryKey(table, callerRecords.get(record.trackCopies()), insertedKeys.get(record.trackCopies()), callerKeys);
            }
         }

         if(weOwnTheTransaction)
         {
            transaction.commit();
         }

         return (output);
      }
      catch(Exception e)
      {
         if(weOwnTheTransaction)
         {
            LOG.warn("Replace failed; rolling back its transaction", e);
            transaction.rollback();
         }
         throw (new QException("Error executing replace action", e));
      }
      finally
      {
         if(weOwnTheTransaction)
         {
            transaction.close();
         }
      }
   }



   /*******************************************************************************
    ** Exclude retained identities before ordering/windowing omission candidates.
    ** A set operation stays at the root, with exclusion applied to each operand.
    *******************************************************************************/
   private static QQueryFilter omissionFilter(QQueryFilter requested, String primaryKeyField, List<Serializable> primaryKeysToKeep)
   {
      QQueryFilter filter = requested == null ? new QQueryFilter() : requested.clone();
      if(filter.getSubFilterSetOperator() != null && CollectionUtils.nullSafeHasContents(filter.getSubFilters()))
      {
         List<QQueryFilter> operands = new ArrayList<>();
         for(QQueryFilter operand : filter.getSubFilters())
         {
            operands.add(new QQueryFilter(new QFilterCriteria(primaryKeyField, QCriteriaOperator.NOT_IN, primaryKeysToKeep)).withSubFilter(operand));
         }
         filter.setSubFilters(operands);
         return filter;
      }
      QQueryFilter omissions = new QQueryFilter(new QFilterCriteria(primaryKeyField, QCriteriaOperator.NOT_IN, primaryKeysToKeep)).withSubFilter(filter);
      omissions.setOrderBys(filter.getOrderBys());
      omissions.setSkip(filter.getSkip());
      omissions.setLimit(filter.getLimit());
      return omissions;
   }



   /*******************************************************************************
    ** A discovered operational identity does not grant permission to disclose it.
    *******************************************************************************/
   private static boolean isPrivatePrimaryKey(QTableMetaData table)
   {
      QFieldMetaData field = table.getField(table.getPrimaryKeyField());
      return field.getIsHidden() || field.getIsHeavy() || (field.getType().needsMasked() && !field.hasAdornmentType(AdornmentType.REVEAL));
   }



   /*******************************************************************************
    ** Explicit caller keys retain their existing write semantics. Other private
    ** identities stay available only through the nonserialized record carrier.
    *******************************************************************************/
   private static void publishPrimaryKey(QTableMetaData table, QRecord record, Serializable key, Set<Object> callerKeys) throws QException
   {
      QFieldMetaData field = table.getField(table.getPrimaryKeyField());
      Object typedKey = AssociatedRecordUpdate.primaryKey(table, new QRecord().withValue(field.getName(), key));
      if(isPrivatePrimaryKey(table) && !callerKeys.contains(typedKey))
      {
         record.removeValue(field.getName());
         if(!field.getIsHidden() && !field.getIsHeavy() && field.getType().needsMasked())
         {
            record.setValue(field.getName(), "************");
         }
         record.setRecordLabel(null);
         record.capturePrimaryKey(table, key);
      }
      else
      {
         record.setValue(field.getName(), key);
      }
   }



   /*******************************************************************************
    ** Keep nested action output privacy consistent with discovered caller keys.
    *******************************************************************************/
   private static void sanitizeDiscoveredKeys(QTableMetaData table, List<QRecord> records, Set<Object> callerKeys) throws QException
   {
      if(isPrivatePrimaryKey(table))
      {
         for(QRecord record : CollectionUtils.nonNullList(records))
         {
            publishPrimaryKey(table, record, record.resolvePrimaryKey(table), callerKeys);
         }
      }
   }



   /*******************************************************************************
    ** POST presentation may reorder or clone rows, but cannot choose which native
    ** inserted identities survive omission deletion or disappear from the result.
    *******************************************************************************/
   private static void validateInsertedIdentities(QTableMetaData table, List<QRecord> nativeRecords, List<QRecord> records) throws QException
   {
      Set<Object> remaining = new HashSet<>();
      for(QRecord record : nativeRecords)
      {
         Object key = AssociatedRecordUpdate.primaryKey(table, record);
         if(key == null || !remaining.add(key))
         {
            throw new QException("Replace INSERT returned invalid native identities");
         }
      }
      if(records == null)
      {
         throw new QException("Replace INSERT omitted its result records");
      }
      for(QRecord record : records)
      {
         if(record == null || !remaining.remove(AssociatedRecordUpdate.primaryKey(table, record)))
         {
            throw new QException("Replace INSERT returned an invalid POST identity");
         }
      }
      if(!remaining.isEmpty())
      {
         throw new QException("Replace INSERT omitted a native identity from its POST result");
      }
   }



   /*******************************************************************************
    ** A failed phase must stop later writes and the owned transaction's commit.
    ** Associated record failures can be reported beneath a successful parent.
    *******************************************************************************/
   static void requireSuccessfulRecords(List<QRecord> records, String phase) throws QException
   {
      for(QRecord record : CollectionUtils.nonNullList(records))
      {
         if(CollectionUtils.nullSafeHasContents(record.getErrors()))
         {
            throw new QException("Replace " + phase + " returned a failed record");
         }
         if(record.getAssociatedRecords() != null)
         {
            for(List<QRecord> children : record.getAssociatedRecords().values())
            {
               requireSuccessfulRecords(children, phase);
            }
         }
      }
   }
}
