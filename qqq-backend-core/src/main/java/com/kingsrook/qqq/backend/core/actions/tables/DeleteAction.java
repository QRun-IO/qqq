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

package com.kingsrook.qqq.backend.core.actions.tables;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.actions.audits.DMLAuditAction;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.interfaces.DeleteInterface;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.AssociatedRecordDiscovery;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.AssociatedRecordUpdate;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.AssociationJoin;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.FilterValidationHelper;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.QueryStatManager;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.ValidateRecordSecurityLockHelper;
import com.kingsrook.qqq.backend.core.actions.values.ValueBehaviorApplier;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.LogPair;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.audits.DMLAuditInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.querystats.QueryStat;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.NotFoundStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.PermissionDeniedMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.QErrorMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.QWarningMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.SystemErrorStatusMessage;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleInterface;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Action to delete 1 or more records.
 **
 *******************************************************************************/
public class DeleteAction
{
   private static final QLogger LOG = QLogger.getLogger(DeleteAction.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   public DeleteOutput execute(DeleteInput deleteInput) throws QException
   {
      return execute(deleteInput, new HashSet<>());
   }



   /*******************************************************************************
    ** An omission cascade may not delete the parent that is being updated.
    *******************************************************************************/
   DeleteOutput executeForAssociation(DeleteInput input, QTableMetaData parentTable, QRecord parent) throws QException
   {
      return execute(input, new HashSet<>(Set.of(new Target(parentTable.getName(), AssociatedRecordUpdate.primaryKey(parentTable, parent)))));
   }



   /*******************************************************************************
    ** Input selection belongs to the caller, even after a failed recursive delete.
    *******************************************************************************/
   private DeleteOutput execute(DeleteInput deleteInput, Set<Target> ancestors) throws QException
   {
      List<Serializable> primaryKeys = deleteInput.getPrimaryKeys() == null ? null : new ArrayList<>(deleteInput.getPrimaryKeys());
      QQueryFilter filter = deleteInput.getQueryFilter();
      QContext.pushAction(deleteInput);
      try
      {
         return executeInternal(deleteInput, ancestors);
      }
      finally
      {
         deleteInput.setPrimaryKeys(primaryKeys);
         deleteInput.setQueryFilter(filter);
         QContext.popAction();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private DeleteOutput executeInternal(DeleteInput deleteInput, Set<Target> ancestors) throws QException
   {
      ActionHelper.validateSession(deleteInput);

      if(deleteInput.getTableName() == null)
      {
         throw (new QException("Table name was not specified in delete input"));
      }

      QTableMetaData table = deleteInput.getTable();
      if(table == null)
      {
         throw (new QException("A table named [" + deleteInput.getTableName() + "] was not found in the active QInstance"));
      }
      table = TableMetaDataPersonalizerAction.execute(deleteInput);
      deleteInput.setTableMetaData(table);
      String         primaryKeyFieldName = table.getPrimaryKeyField();
      QFieldMetaData primaryKeyField     = table.getField(primaryKeyFieldName);

      List<Serializable> primaryKeys = deleteInput.getPrimaryKeys();
      List<Serializable> originalPrimaryKeys = primaryKeys == null ? null : new ArrayList<>(primaryKeys);
      QQueryFilter originalFilter = deleteInput.getQueryFilter();
      if(CollectionUtils.nullSafeHasContents(primaryKeys) && deleteInput.getQueryFilter() != null)
      {
         throw (new QException("A delete request may not contain both a list of primary keys and a query filter."));
      }

      ////////////////////////////////////////////////////////
      // make sure the primary keys are of the correct type //
      ////////////////////////////////////////////////////////
      if(CollectionUtils.nullSafeHasContents(primaryKeys))
      {
         primaryKeys = new ArrayList<>(primaryKeys);
         deleteInput.setPrimaryKeys(primaryKeys);
         for(int i = 0; i < primaryKeys.size(); i++)
         {
            Serializable primaryKey       = primaryKeys.get(i);
            Serializable valueAsFieldType = ValueUtils.getValueAsFieldType(primaryKeyField.getType(), primaryKey);
            if(!Objects.equals(primaryKey, valueAsFieldType))
            {
               primaryKeys.set(i, valueAsFieldType);
            }
         }
      }

      //////////////////////////////////////////////////////
      // load the backend module and its delete interface //
      //////////////////////////////////////////////////////
      QBackendModuleDispatcher qBackendModuleDispatcher = new QBackendModuleDispatcher();
      QBackendModuleInterface  qModule                  = qBackendModuleDispatcher.getQBackendModule(deleteInput.getBackend());
      DeleteInterface          deleteInterface          = qModule.getDeleteInterface();

      //////////////////////////////////////////////////////////////////////
      // Validation and cascades must act on the exact selected key list. //
      //////////////////////////////////////////////////////////////////////
      if(deleteInput.getQueryFilter() != null && (!deleteInterface.supportsQueryFilterInput() || deleteInterface.supportsPreFetchQuery() || CollectionUtils.nullSafeHasContents(QContext.getQInstance().getTable(table.getName()).getAssociations())))
      {
         LOG.debug("Selecting primary keys before validated or cascading delete", new LogPair("tableName", table.getName()));
         List<Serializable> primaryKeyList = getPrimaryKeysFromQueryFilter(deleteInput);
         deleteInput.setPrimaryKeys(primaryKeyList);
         deleteInput.setQueryFilter(null);
         primaryKeys = deleteInput.getPrimaryKeys();

         if(primaryKeyList.isEmpty())
         {
            LOG.info("0 primaryKeys found.  Returning with no-op");
            DeleteOutput deleteOutput = new DeleteOutput();
            deleteOutput.setRecordsWithErrors(new ArrayList<>());
            deleteOutput.setDeletedRecordCount(0);
            return (deleteOutput);
         }
      }
      else if(deleteInput.getQueryFilter() != null)
      {
         deleteInput.setQueryFilter(prepareFilterQuery(deleteInput).getFilter());
      }

      if(primaryKeys != null)
      {
         Map<Object, Serializable> distinctKeys = new LinkedHashMap<>();
         for(Serializable key : primaryKeys)
         {
            distinctKeys.putIfAbsent(AssociatedRecordUpdate.primaryKey(table, new QRecord().withValue(primaryKeyFieldName, key)), key);
         }
         deleteInput.setPrimaryKeys(new ArrayList<>(distinctKeys.values()));
         primaryKeys = deleteInput.getPrimaryKeys();
      }

      ////////////////////////////////////////////////////////////////////////////////
      // fetch the old list of records (if the backend supports it), for audits,    //
      // for "not-found detection", and for the pre-action to use (if there is one) //
      ////////////////////////////////////////////////////////////////////////////////
      Optional<List<QRecord>> oldRecordList = fetchOldRecords(deleteInput, deleteInterface);

      List<QRecord>              customizerResult              = performValidations(deleteInput, oldRecordList, false);
      List<QRecord>              recordsWithValidationErrors   = new ArrayList<>();
      Map<Object, QRecord>       recordsWithValidationWarnings = new LinkedHashMap<>();

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // check if any records got errors in the customizer - if so, remove them from the input list of pkeys to delete //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      if(customizerResult != null)
      {
         Set<Object> primaryKeysToRemoveFromInput = new HashSet<>();
         for(QRecord record : customizerResult)
         {
            if(CollectionUtils.nullSafeHasContents(record.getErrors()))
            {
               recordsWithValidationErrors.add(record);
               primaryKeysToRemoveFromInput.add(AssociatedRecordUpdate.primaryKey(table, record));
            }
            else if(CollectionUtils.nullSafeHasContents(record.getWarnings()))
            {
               recordsWithValidationWarnings.put(AssociatedRecordUpdate.primaryKey(table, record), record);
            }
         }

         if(!primaryKeysToRemoveFromInput.isEmpty())
         {
            if(primaryKeys == null)
            {
               LOG.warn("There were primary keys to remove from the input, but no primary key list (filter supplied as input?)", new LogPair("primaryKeysToRemoveFromInput", primaryKeysToRemoveFromInput));
            }
            else
            {
               for(int i = primaryKeys.size() - 1; i >= 0; i--)
               {
                  if(primaryKeysToRemoveFromInput.contains(AssociatedRecordUpdate.primaryKey(table, new QRecord().withValue(primaryKeyFieldName, primaryKeys.get(i)))))
                  {
                     primaryKeys.remove(i);
                  }
               }
            }
         }
      }

      List<QRecord> associationResults = deleteAssociations(deleteInput, ancestors);
      Set<Object> failedKeys = new HashSet<>();
      for(QRecord record : associationResults)
      {
         if(CollectionUtils.nullSafeHasContents(record.getErrors()))
         {
            recordsWithValidationErrors.add(record);
            failedKeys.add(AssociatedRecordUpdate.primaryKey(table, record));
         }
         else if(CollectionUtils.nullSafeHasContents(record.getWarnings()))
         {
            recordsWithValidationWarnings.put(AssociatedRecordUpdate.primaryKey(table, record), record);
         }
      }
      if(primaryKeys != null)
      {
         List<Serializable> eligibleKeys = new ArrayList<>();
         for(Serializable key : primaryKeys)
         {
            if(!failedKeys.contains(AssociatedRecordUpdate.primaryKey(table, new QRecord().withValue(primaryKeyFieldName, key))))
            {
               eligibleKeys.add(key);
            }
         }
         deleteInput.setPrimaryKeys(eligibleKeys);
      }

      ////////////////////////////////////
      // have the backend do the delete //
      ////////////////////////////////////
      QueryStat    queryStat    = QueryStatManager.newQueryStat(deleteInput.getBackend(), table, null, DeleteAction.class.getSimpleName());
      DeleteOutput deleteOutput = deleteInput.getQueryFilter() == null && CollectionUtils.nullSafeIsEmpty(deleteInput.getPrimaryKeys())
         ? new DeleteOutput() : deleteInterface.execute(deleteInput);

      sanitizeResultPrimaryKeys(table, deleteOutput.getRecordsWithErrors(), true);
      sanitizeResultPrimaryKeys(table, deleteOutput.getRecordsWithWarnings(), true);

      if(queryStat != null)
      {
         queryStat.setRecordCount(deleteOutput.getDeletedRecordCount() + CollectionUtils.nonNullList(deleteOutput.getRecordsWithErrors()).size());
         QueryStatManager.getInstance().add(queryStat);
      }

      deleteInput.setPrimaryKeys(originalPrimaryKeys);
      deleteInput.setQueryFilter(originalFilter);

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // merge the backend's output with any validation errors we found (whose pkeys wouldn't have gotten into the backend delete) //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      List<QRecord> outputRecordsWithErrors = Objects.requireNonNullElseGet(deleteOutput.getRecordsWithErrors(), () -> new ArrayList<>());
      outputRecordsWithErrors.addAll(recordsWithValidationErrors);

      ///////////////////////////////////////////////////////////////////////////////////////////
      // combine the warning list from validation to that from execution - avoiding duplicates //
      // use a map to manage this list for the rest of this method                             //
      ///////////////////////////////////////////////////////////////////////////////////////////
      Map<Object, QRecord> outputRecordsWithWarningMap = new LinkedHashMap<>();
      for(QRecord record : CollectionUtils.nonNullList(deleteOutput.getRecordsWithWarnings()))
      {
         outputRecordsWithWarningMap.putIfAbsent(AssociatedRecordUpdate.primaryKey(table, record), record);
      }
      for(Map.Entry<Object, QRecord> entry : recordsWithValidationWarnings.entrySet())
      {
         outputRecordsWithWarningMap.putIfAbsent(entry.getKey(), entry.getValue());
      }
      for(QRecord record : outputRecordsWithErrors)
      {
         outputRecordsWithWarningMap.remove(AssociatedRecordUpdate.primaryKey(table, record));
      }

      //////////////////
      // do the audit //
      //////////////////
      if(deleteInput.getOmitDmlAudit())
      {
         LOG.debug("Requested to omit DML audit");
      }
      else
      {
         DMLAuditInput dmlAuditInput = new DMLAuditInput()
            .withTableActionInput(deleteInput)
            .withTransaction(deleteInput.getTransaction())
            .withAuditContext(deleteInput.getAuditContext());
         if(oldRecordList.isPresent())
         {
            dmlAuditInput.setRecordList(makeListOfRecordsNotInErrorList(table, oldRecordList.get(), outputRecordsWithErrors));
         }
         new DMLAuditAction().execute(dmlAuditInput);
      }

      //////////////////////////////////////////////////////////////
      // finally, run the post-delete customizer, if there is one //
      //////////////////////////////////////////////////////////////
      Optional<TableCustomizerInterface> postDeleteCustomizer = QCodeLoader.getTableCustomizer(table, TableCustomizers.POST_DELETE_RECORD.getRole());
      if(postDeleteCustomizer.isPresent() && oldRecordList.isPresent())
      {
         ////////////////////////////////////////////////////////////////////////////
         // make list of records that are still good - to pass into the customizer //
         ////////////////////////////////////////////////////////////////////////////
         List<QRecord> recordsForCustomizer = makeListOfRecordsNotInErrorList(table, oldRecordList.get(), outputRecordsWithErrors);
         Map<Object, QRecord> successfulRecords = new LinkedHashMap<>();
         for(QRecord record : recordsForCustomizer)
         {
            QRecord fallbackRecord = new QRecord(record);
            if(record.getValue(primaryKeyFieldName) instanceof byte[] keyBytes)
            {
               fallbackRecord.setValue(primaryKeyFieldName, keyBytes.clone());
            }
            successfulRecords.put(AssociatedRecordUpdate.primaryKey(table, record), fallbackRecord);
         }

         try
         {
            List<QRecord> postCustomizerResult = postDeleteCustomizer.get().postDelete(deleteInput, recordsForCustomizer);

            for(QRecord record : postCustomizerResult)
            {
               Object key = AssociatedRecordUpdate.primaryKey(table, record);
               if(key == null || !successfulRecords.containsKey(key))
               {
                  throw new QException("Post-delete customizer returned a record outside the successful result");
               }
            }

            ///////////////////////////////////////////////////////
            // check if any records got errors in the customizer //
            ///////////////////////////////////////////////////////
            for(QRecord record : postCustomizerResult)
            {
               Object pkey = AssociatedRecordUpdate.primaryKey(table, record);
               if(CollectionUtils.nullSafeHasContents(record.getErrors()))
               {
                  outputRecordsWithErrors.add(record);
                  outputRecordsWithWarningMap.remove(pkey);
               }
               else if(CollectionUtils.nullSafeHasContents(record.getWarnings()))
               {
                  outputRecordsWithWarningMap.put(pkey, record);
               }
            }
         }
         catch(Exception e)
         {
            for(Map.Entry<Object, QRecord> entry : successfulRecords.entrySet())
            {
               QRecord record = entry.getValue();
               record.addWarning(new QWarningMessage("An error occurred after the delete: " + e.getMessage()));
               outputRecordsWithWarningMap.put(entry.getKey(), record);
            }
         }
      }

      sanitizeResultPrimaryKeys(table, outputRecordsWithErrors, false);
      sanitizeResultPrimaryKeys(table, new ArrayList<>(outputRecordsWithWarningMap.values()), false);
      deleteOutput.setRecordsWithErrors(outputRecordsWithErrors);
      deleteOutput.setRecordsWithWarnings(new ArrayList<>(outputRecordsWithWarningMap.values()));

      return deleteOutput;
   }



   /*******************************************************************************
    ** Native failures contain selection keys; callbacks and outputs retain the
    ** same key privacy as the prefetch. Validate every callback record before DML.
    *******************************************************************************/
   private static void sanitizeResultPrimaryKeys(QTableMetaData table, List<QRecord> records, boolean nativeResult) throws QException
   {
      QFieldMetaData field = table.getField(table.getPrimaryKeyField());
      boolean hidden = field.getIsHidden() || field.getIsHeavy();
      boolean masked = field.getType().needsMasked() && !field.hasAdornmentType(AdornmentType.REVEAL);
      for(QRecord record : CollectionUtils.nonNullList(records))
      {
         Serializable key = record.resolvePrimaryKey(table);
         if(hidden || masked)
         {
            boolean present = record.getValues().containsKey(field.getName());
            record.removeValue(field.getName());
            if(masked && !hidden && present)
            {
               record.setValue(field.getName(), "************");
            }
            record.setRecordLabel(null);
            if(key != null)
            {
               record.capturePrimaryKey(table, key);
            }
            if(nativeResult && (field.getIsHidden() || masked))
            {
               record.setErrors(new ArrayList<>(CollectionUtils.nonNullList(record.getErrors())));
               for(int i = 0; i < record.getErrors().size(); i++)
               {
                  if(record.getErrors().get(i) instanceof SystemErrorStatusMessage)
                  {
                     record.getErrors().set(i, new SystemErrorStatusMessage("An error occurred while deleting this record"));
                  }
                  else if(record.getErrors().get(i) instanceof NotFoundStatusMessage)
                  {
                     record.getErrors().set(i, new NotFoundStatusMessage("No record was found to delete"));
                  }
               }
               if(CollectionUtils.nullSafeHasContents(record.getWarnings()))
               {
                  record.setWarnings(new ArrayList<>(List.of(new QWarningMessage("A warning occurred while deleting this record"))));
               }
            }
         }
      }
   }



   /*******************************************************************************
    ** this method takes in the deleteInput, and the list of old records that matched
    ** the pkeys in that input.
    **
    ** it'll check if any of those pkeys aren't found (in a sub-method) - a record
    ** with an error message will be added to oldRecordList for any such records.
    **
    ** it'll also then call the pre-customizer, if there is one - taking in the
    ** oldRecordList.  it can add other errors or warnings to records.
    **
    ** The return value here is basically oldRecordList - possibly with some new
    ** entries for the pkey-not-founds, and possibly w/ errors and warnings from the
    ** customizer.
    *******************************************************************************/
   public List<QRecord> performValidations(DeleteInput deleteInput, Optional<List<QRecord>> oldRecordList, boolean isPreview) throws QException
   {
      if(oldRecordList.isEmpty())
      {
         return (null);
      }

      QTableMetaData table               = deleteInput.getTable();
      List<QRecord>  primaryKeysNotFound = validateRecordsExistAndCanBeAccessed(deleteInput, oldRecordList.get());

      Map<Object, QRecord> visibleRecords = new LinkedHashMap<>();
      for(QRecord record : oldRecordList.get())
      {
         visibleRecords.put(AssociatedRecordUpdate.primaryKey(table, record), record);
      }
      Map<Object, QRecord> authorizedRecords = ValidateRecordSecurityLockHelper.validateStoredWriteLocks(table, visibleRecords,
         ValidateRecordSecurityLockHelper.Action.DELETE, deleteInput.getTransaction());
      if(authorizedRecords != null)
      {
         for(QRecord record : oldRecordList.get())
         {
            QRecord storedRecord = authorizedRecords.get(AssociatedRecordUpdate.primaryKey(table, record));
            if(storedRecord == null || CollectionUtils.nullSafeHasContents(storedRecord.getErrors()))
            {
               record.addError(new PermissionDeniedMessage("You do not have permission to delete this record."));
            }
         }
      }

      ///////////////////////////////////////////////////////////////////////////
      // after all validations, run the pre-delete customizer, if there is one //
      ///////////////////////////////////////////////////////////////////////////
      Optional<TableCustomizerInterface> preDeleteCustomizer = QCodeLoader.getTableCustomizer(table, TableCustomizers.PRE_DELETE_RECORD.getRole());
      List<QRecord>                         customizerResult    = oldRecordList.get();
      if(preDeleteCustomizer.isPresent())
      {
         customizerResult = preDeleteCustomizer.get().preDelete(deleteInput, oldRecordList.get(), isPreview);
         for(QRecord record : customizerResult)
         {
            Object key = AssociatedRecordUpdate.primaryKey(table, record);
            if(key == null || !visibleRecords.containsKey(key))
            {
               throw new QException("Pre-delete customizer returned a record outside the prefetched result");
            }
         }
      }

      /////////////////////////////////////////////////////////////////////////
      // add any pkey-not-found records to the front of the customizerResult //
      /////////////////////////////////////////////////////////////////////////
      customizerResult = new ArrayList<>(customizerResult);
      customizerResult.addAll(primaryKeysNotFound);
      sanitizeResultPrimaryKeys(table, customizerResult, false);

      return customizerResult;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<QRecord> makeListOfRecordsNotInErrorList(QTableMetaData table, List<QRecord> oldRecordList, List<QRecord> outputRecordsWithErrors) throws QException
   {
      Set<Object> errorKeys = new HashSet<>();
      for(QRecord record : outputRecordsWithErrors)
      {
         errorKeys.add(AssociatedRecordUpdate.primaryKey(table, record));
      }
      List<QRecord> recordsForCustomizer = new ArrayList<>();
      for(QRecord record : oldRecordList)
      {
         if(!errorKeys.contains(AssociatedRecordUpdate.primaryKey(table, record)))
         {
            recordsForCustomizer.add(record);
         }
      }
      return recordsForCustomizer;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<QRecord> deleteAssociations(DeleteInput input, Set<Target> ancestors) throws QException
   {
      List<QRecord> results = new ArrayList<>();
      QTableMetaData canonical = QContext.getQInstance().getTable(input.getTableName());
      if(CollectionUtils.nullSafeIsEmpty(input.getPrimaryKeys()) || CollectionUtils.nullSafeIsEmpty(canonical.getAssociations()))
      {
         return results;
      }
      QTableMetaData table = AssociatedRecordDiscovery.physicalParentTable(input.getTable());
      table.setAssociations(canonical.getAssociations());
      String primaryKey = table.getPrimaryKeyField();
      boolean needsStoredValues = false;
      for(Association association : table.getAssociations())
      {
         if(AssociationJoin.resolve(table, association).getJoinOns().stream().anyMatch(pair -> !primaryKey.equals(pair.getLeftField())))
         {
            needsStoredValues = true;
         }
      }
      List<QRecord> parents = needsStoredValues
         ? AssociatedRecordDiscovery.readParentValues(table, table.getAssociations(), input.getPrimaryKeys(), input.getTransaction())
         : input.getPrimaryKeys().stream().map(key -> new QRecord().withValue(primaryKey, key)).toList();
      Map<Object, QRecord> parentsByKey = new HashMap<>();
      for(QRecord parent : parents)
      {
         parentsByKey.put(AssociatedRecordUpdate.primaryKey(table, parent), parent);
      }
      for(Serializable key : input.getPrimaryKeys())
      {
         QRecord result = new QRecord().withValue(primaryKey, key);
         Target target = new Target(table.getName(), AssociatedRecordUpdate.primaryKey(table, result));
         QRecord parent = parentsByKey.get(target.key());
         if(parent == null)
         {
            result.addError(new NotFoundStatusMessage("No parent record was found for the associated delete"));
            results.add(result);
            continue;
         }
         if(!ancestors.add(target))
         {
            result.addError(new BadInputStatusMessage("A cyclic association cannot be deleted recursively"));
            results.add(result);
            continue;
         }
         try
         {
            for(Association association : table.getAssociations())
            {
               AssociationJoin join = AssociationJoin.resolve(table, association);
               if(join.parentValues(parent).contains(null))
               {
                  continue;
               }
               List<Serializable> childKeys = AssociatedRecordDiscovery.findPrimaryKeys(table, association,
                  List.of(new AssociatedRecordDiscovery.Parent(parent.getValues(), List.of())), input.getTransaction());
               if(childKeys.isEmpty())
               {
                  continue;
               }
               QTableMetaData childTable = QContext.getQInstance().getTable(association.getAssociatedTableName());
               boolean cyclic = false;
               for(Serializable childKey : childKeys)
               {
                  if(ancestors.contains(new Target(childTable.getName(), AssociatedRecordUpdate.primaryKey(childTable, new QRecord().withValue(childTable.getPrimaryKeyField(), childKey)))))
                  {
                     cyclic = true;
                     break;
                  }
               }
               if(cyclic)
               {
                  result.addError(new BadInputStatusMessage("A cyclic association cannot be deleted recursively"));
                  break;
               }
               DeleteInput childInput = new DeleteInput(association.getAssociatedTableName()).withPrimaryKeys(childKeys)
                  .withInputSource(input.getInputSource()).withFlags(input.getFlags()).withTransaction(input.getTransaction())
                  .withOmitDmlAudit(input.getOmitDmlAudit()).withAuditContext(input.getAuditContext());
               DeleteOutput childOutput = new DeleteAction().execute(childInput, ancestors);
               if(CollectionUtils.nullSafeHasContents(childOutput.getRecordsWithErrors()) || childOutput.getDeletedRecordCount() != childKeys.size())
               {
                  result.addError(associationFailure(childOutput, association.getName()));
                  break;
               }
               if(CollectionUtils.nullSafeHasContents(childOutput.getRecordsWithWarnings()))
               {
                  result.addWarning(new QWarningMessage("Warnings occurred deleting association [" + association.getName() + "]"));
               }
            }
         }
         finally
         {
            ancestors.remove(target);
         }
         if(CollectionUtils.nullSafeHasContents(result.getErrors()) || CollectionUtils.nullSafeHasContents(result.getWarnings()))
         {
            results.add(result);
         }
      }
      return results;
   }



   /*******************************************************************************
    ** Preserve the failure category without publishing private descendant details.
    *******************************************************************************/
   public static QErrorMessage associationFailure(DeleteOutput output, String associationName)
   {
      String message = "Unable to delete all records for association [" + associationName + "]";
      List<QErrorMessage> errors = CollectionUtils.nonNullList(output.getRecordsWithErrors()).stream()
         .flatMap(record -> CollectionUtils.nonNullList(record.getErrors()).stream()).toList();
      if(errors.stream().anyMatch(error -> error instanceof SystemErrorStatusMessage))
      {
         return new SystemErrorStatusMessage(message);
      }
      if(errors.stream().anyMatch(error -> error instanceof PermissionDeniedMessage))
      {
         return new PermissionDeniedMessage(message);
      }
      if(errors.stream().anyMatch(error -> error instanceof BadInputStatusMessage))
      {
         return new BadInputStatusMessage(message);
      }
      if(errors.stream().anyMatch(error -> error instanceof NotFoundStatusMessage))
      {
         return new NotFoundStatusMessage(message);
      }
      return new SystemErrorStatusMessage(message);
   }



   /*******************************************************************************
    ** A branch-local identity prevents recursive cycles without hiding row locks.
    *******************************************************************************/
   private record Target(String tableName, Object key)
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Optional<List<QRecord>> fetchOldRecords(DeleteInput deleteInput, DeleteInterface deleteInterface) throws QException
   {
      if(deleteInterface.supportsPreFetchQuery())
      {
         List<Serializable> primaryKeyList = deleteInput.getPrimaryKeys();
         if(CollectionUtils.nullSafeIsEmpty(deleteInput.getPrimaryKeys()) && deleteInput.getQueryFilter() != null)
         {
            primaryKeyList = getPrimaryKeysFromQueryFilter(deleteInput);
         }

         if(CollectionUtils.nullSafeHasContents(primaryKeyList))
         {
            QueryInput queryInput = new QueryInput();
            queryInput.setTransaction(deleteInput.getTransaction());
            queryInput.setTableName(deleteInput.getTableName());
            queryInput.setInputSource(deleteInput.getInputSource());
            queryInput.setTableMetaData(deleteInput.getTable());
            queryInput.setFilter(new QQueryFilter(new QFilterCriteria(deleteInput.getTable().getPrimaryKeyField(), QCriteriaOperator.IN, primaryKeyList)));
            QueryOutput queryOutput = new QueryAction().executeForDml(queryInput);
            return (Optional.of(queryOutput.getRecords()));
         }
      }

      return (Optional.empty());
   }



   /*******************************************************************************
    ** Note - the "can be accessed" part of this method name - it implies that
    ** records that you can't see because of security - that they won't be found
    ** by the query here, so it's the same to you as if they don't exist at all!
    **
    ** If this method identifies any missing records (e.g., from PKeys that are
    ** requested to be deleted, but don't exist (or can't be seen)), then it will
    ** return those as new QRecords, with error messages.
    *******************************************************************************/
   private List<QRecord> validateRecordsExistAndCanBeAccessed(DeleteInput deleteInput, List<QRecord> oldRecordList) throws QException
   {
      List<QRecord> recordsWithErrors = new ArrayList<>();

      QTableMetaData table           = deleteInput.getTable();
      QFieldMetaData primaryKeyField = table.getField(table.getPrimaryKeyField());

      Set<Object> oldRecordKeys = new HashSet<>();
      for(QRecord record : oldRecordList)
      {
         oldRecordKeys.add(AssociatedRecordUpdate.primaryKey(table, record));
      }
      for(Serializable primaryKeyValue : deleteInput.getPrimaryKeys())
      {
         primaryKeyValue = ValueUtils.getValueAsFieldType(primaryKeyField.getType(), primaryKeyValue);
         QRecord record = new QRecord().withValue(primaryKeyField.getName(), primaryKeyValue);
         if(!oldRecordKeys.contains(AssociatedRecordUpdate.primaryKey(table, record)))
         {
            recordsWithErrors.add(record);
            record.addError(new NotFoundStatusMessage(primaryKeyField.getIsHidden() || primaryKeyField.getType().needsMasked()
               ? "No record was found to delete"
               : "No record was found to delete for " + Objects.requireNonNullElse(primaryKeyField.getLabel(), primaryKeyField.getName()) + " = " + primaryKeyValue));
         }
      }

      return (recordsWithErrors);
   }



   /*******************************************************************************
    ** For an implementation that doesn't support a queryFilter as its input,
    ** but a scenario where a query filter was passed in - run the query, to
    ** get a list of primary keys.
    *******************************************************************************/
   public static List<Serializable> getPrimaryKeysFromQueryFilter(DeleteInput deleteInput) throws QException
   {
      try
      {
         QBackendModuleDispatcher qBackendModuleDispatcher = new QBackendModuleDispatcher();
         QBackendModuleInterface  qModule                  = qBackendModuleDispatcher.getQBackendModule(deleteInput.getBackend());

         QueryInput queryInput = prepareFilterQuery(deleteInput);
         queryInput.setFieldNamesToInclude(Set.of(deleteInput.getTable().getPrimaryKeyField()));
         queryInput.setShouldFetchHeavyFields(true);
         queryInput.setShouldOmitHiddenFields(false);
         QueryOutput queryOutput = qModule.getQueryInterface().executeForDml(queryInput);

         return (queryOutput.getRecords().stream()
            .map(r -> r.getValue(deleteInput.getTable().getPrimaryKeyField()))
            .toList());
      }
      catch(Exception e)
      {
         LOG.warn("Error getting primary keys from query filter before bulk-delete", e);
         throw (new QException("Error getting keys from filter prior to delete.", e));
      }
   }



   /*******************************************************************************
    ** Validate before native selection so discarded malformed criteria cannot
    ** broaden a delete. Presentation customizers do not choose deletion targets.
    *******************************************************************************/
   private static QueryInput prepareFilterQuery(DeleteInput deleteInput) throws QException
   {
      QueryInput queryInput = new QueryInput(deleteInput.getTableName()).withTransaction(deleteInput.getTransaction())
         .withInputSource(deleteInput.getInputSource()).withFilter(deleteInput.getQueryFilter());
      queryInput.setTableMetaData(TableMetaDataPersonalizerAction.execute(deleteInput));
      FilterValidationHelper.validateFieldNamesInFilter(queryInput);
      QQueryFilter filter = queryInput.getFilter();
      if(filter != null)
      {
         if(filter.getLimit() != null && filter.getLimit() < 0)
         {
            throw (new QException("Query limit must be greater than or equal to zero"));
         }
         if(filter.getSkip() != null && filter.getSkip() < 0)
         {
            throw (new QException("Query skip must be greater than or equal to zero"));
         }
         queryInput.setFilter(ValueBehaviorApplier.applyFieldBehaviorsToFilter(QContext.getQInstance(), queryInput.getTable(), filter.clone(), Collections.emptySet()));
      }
      return queryInput;
   }

}
