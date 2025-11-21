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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.actions.audits.DMLAuditAction;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.interfaces.DeleteInterface;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.QueryStatManager;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.ValidateRecordSecurityLockHelper;
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
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.querystats.QueryStat;
import com.kingsrook.qqq.backend.core.model.statusmessages.NotFoundStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.QWarningMessage;
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
      ActionHelper.validateSession(deleteInput);

      if(deleteInput.getTableName() == null)
      {
         throw (new QException("Table name was not specified in delete input"));
      }

      QTableMetaData table               = deleteInput.getTable();
      String         primaryKeyFieldName = table.getPrimaryKeyField();
      QFieldMetaData primaryKeyField     = table.getField(primaryKeyFieldName);

      List<Serializable> primaryKeys         = deleteInput.getPrimaryKeys();
      List<Serializable> originalPrimaryKeys = primaryKeys == null ? null : new ArrayList<>(primaryKeys);
      if(CollectionUtils.nullSafeHasContents(primaryKeys) && deleteInput.getQueryFilter() != null)
      {
         throw (new QException("A delete request may not contain both a list of primary keys and a query filter."));
      }

      ////////////////////////////////////////////////////////
      // make sure the primary keys are of the correct type //
      ////////////////////////////////////////////////////////
      if(CollectionUtils.nullSafeHasContents(primaryKeys))
      {
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

      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // if there's a query filter, but the interface doesn't support using a query filter, then do a query for the filter, to get a list of primary keys instead //
      // or - anytime there are associations on the table we want primary keys, as that's what the manage associations method uses                                //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      if(deleteInput.getQueryFilter() != null && (!deleteInterface.supportsQueryFilterInput() || CollectionUtils.nullSafeHasContents(table.getAssociations())))
      {
         LOG.info("Querying for primary keys, for table " + table.getName() + " in backend module " + qModule.getBackendType() + " which does not support queryFilter input for deletes (or the table has associations)");
         List<Serializable> primaryKeyList = getPrimaryKeysFromQueryFilter(deleteInput);
         deleteInput.setPrimaryKeys(primaryKeyList);
         primaryKeys = primaryKeyList;

         if(primaryKeyList.isEmpty())
         {
            LOG.info("0 primaryKeys found.  Returning with no-op");
            DeleteOutput deleteOutput = new DeleteOutput();
            deleteOutput.setRecordsWithErrors(new ArrayList<>());
            deleteOutput.setDeletedRecordCount(0);
            return (deleteOutput);
         }
      }

      ////////////////////////////////////////////////////////////////////////////////
      // fetch the old list of records (if the backend supports it), for audits,    //
      // for "not-found detection", and for the pre-action to use (if there is one) //
      ////////////////////////////////////////////////////////////////////////////////
      Optional<List<QRecord>> oldRecordList = fetchOldRecords(deleteInput, deleteInterface);

      List<QRecord>              customizerResult              = performValidations(deleteInput, oldRecordList, false);
      List<QRecord>              recordsWithValidationErrors   = new ArrayList<>();
      Map<Serializable, QRecord> recordsWithValidationWarnings = new LinkedHashMap<>();

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // check if any records got errors in the customizer - if so, remove them from the input list of pkeys to delete //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      if(customizerResult != null)
      {
         Set<Serializable> primaryKeysToRemoveFromInput = new HashSet<>();
         for(QRecord record : customizerResult)
         {
            if(CollectionUtils.nullSafeHasContents(record.getErrors()))
            {
               recordsWithValidationErrors.add(record);
               primaryKeysToRemoveFromInput.add(record.getValue(primaryKeyFieldName));
            }
            else if(CollectionUtils.nullSafeHasContents(record.getWarnings()))
            {
               recordsWithValidationWarnings.put(record.getValue(primaryKeyFieldName), record);
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
               primaryKeys.removeAll(primaryKeysToRemoveFromInput);
            }
         }
      }

      ////////////////////////////////////////////////////////////////////////////////////////////////
      // stash a copy of primary keys that didn't have errors (for use in manageAssociations below) //
      ////////////////////////////////////////////////////////////////////////////////////////////////
      Set<Serializable> primaryKeysWithoutErrors = new HashSet<>(CollectionUtils.nonNullList(primaryKeys));

      ////////////////////////////////////
      // have the backend do the delete //
      ////////////////////////////////////
      QueryStat    queryStat    = QueryStatManager.newQueryStat(deleteInput.getBackend(), table, null, DeleteAction.class.getSimpleName());
      DeleteOutput deleteOutput = deleteInterface.execute(deleteInput);

      if(queryStat != null)
      {
         queryStat.setRecordCount(deleteOutput.getDeletedRecordCount() + CollectionUtils.nonNullList(deleteOutput.getRecordsWithErrors()).size());
         QueryStatManager.getInstance().add(queryStat);
      }

      /////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // reset the input's list of primary keys -- callers may use & expect that to be what they had passed in!! //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////
      deleteInput.setPrimaryKeys(originalPrimaryKeys);

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // merge the backend's output with any validation errors we found (whose pkeys wouldn't have gotten into the backend delete) //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      List<QRecord> outputRecordsWithErrors = Objects.requireNonNullElseGet(deleteOutput.getRecordsWithErrors(), () -> new ArrayList<>());
      outputRecordsWithErrors.addAll(recordsWithValidationErrors);

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // if a record had a validation warning, but then an execution error, remove it from the warning list - so it's only in one of them. //
      // also, always remove from
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      for(QRecord outputRecordWithError : outputRecordsWithErrors)
      {
         Serializable pkey = outputRecordWithError.getValue(primaryKeyFieldName);
         recordsWithValidationWarnings.remove(pkey);
         primaryKeysWithoutErrors.remove(pkey);
      }

      ///////////////////////////////////////////////////////////////////////////////////////////
      // combine the warning list from validation to that from execution - avoiding duplicates //
      // use a map to manage this list for the rest of this method                             //
      ///////////////////////////////////////////////////////////////////////////////////////////
      Map<Serializable, QRecord> outputRecordsWithWarningMap = CollectionUtils.nullSafeIsEmpty(deleteOutput.getRecordsWithWarnings()) ? new LinkedHashMap<>()
         : deleteOutput.getRecordsWithWarnings().stream().collect(Collectors.toMap(r -> r.getValue(primaryKeyFieldName), r -> r, (a, b) -> a, () -> new LinkedHashMap<>()));
      for(Map.Entry<Serializable, QRecord> entry : recordsWithValidationWarnings.entrySet())
      {
         if(!outputRecordsWithWarningMap.containsKey(entry.getKey()))
         {
            outputRecordsWithWarningMap.put(entry.getKey(), entry.getValue());
         }
      }

      ////////////////////////////////////////
      // delete associations, if applicable //
      ////////////////////////////////////////
      manageAssociations(primaryKeysWithoutErrors, deleteInput);

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
         oldRecordList.ifPresent(l -> dmlAuditInput.setRecordList(l));
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
         List<QRecord> recordsForCustomizer = makeListOfRecordsNotInErrorList(primaryKeyFieldName, oldRecordList.get(), outputRecordsWithErrors);

         try
         {
            List<QRecord> postCustomizerResult = postDeleteCustomizer.get().postDelete(deleteInput, recordsForCustomizer);

            ///////////////////////////////////////////////////////
            // check if any records got errors in the customizer //
            ///////////////////////////////////////////////////////
            for(QRecord record : postCustomizerResult)
            {
               Serializable pkey = record.getValue(primaryKeyFieldName);
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
            for(QRecord record : recordsForCustomizer)
            {
               record.addWarning(new QWarningMessage("An error occurred after the delete: " + e.getMessage()));
               outputRecordsWithWarningMap.put(record.getValue(primaryKeyFieldName), record);
            }
         }
      }

      deleteOutput.setRecordsWithErrors(outputRecordsWithErrors);
      deleteOutput.setRecordsWithWarnings(new ArrayList<>(outputRecordsWithWarningMap.values()));

      return deleteOutput;
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

      ValidateRecordSecurityLockHelper.validateSecurityFields(table, oldRecordList.get(), ValidateRecordSecurityLockHelper.Action.DELETE, deleteInput.getTransaction());

      ///////////////////////////////////////////////////////////////////////////
      // after all validations, run the pre-delete customizer, if there is one //
      ///////////////////////////////////////////////////////////////////////////
      Optional<TableCustomizerInterface> preDeleteCustomizer = QCodeLoader.getTableCustomizer(table, TableCustomizers.PRE_DELETE_RECORD.getRole());
      List<QRecord>                         customizerResult    = oldRecordList.get();
      if(preDeleteCustomizer.isPresent())
      {
         customizerResult = preDeleteCustomizer.get().preDelete(deleteInput, oldRecordList.get(), isPreview);
      }

      /////////////////////////////////////////////////////////////////////////
      // add any pkey-not-found records to the front of the customizerResult //
      /////////////////////////////////////////////////////////////////////////
      customizerResult.addAll(primaryKeysNotFound);

      return customizerResult;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<QRecord> makeListOfRecordsNotInErrorList(String primaryKeyField, List<QRecord> oldRecordList, List<QRecord> outputRecordsWithErrors)
   {
      Map<Serializable, QRecord> recordsWithErrorsMap = outputRecordsWithErrors.stream().collect(Collectors.toMap(r -> r.getValue(primaryKeyField), r -> r));
      List<QRecord>              recordsForCustomizer = new ArrayList<>();
      for(QRecord record : oldRecordList)
      {
         if(!recordsWithErrorsMap.containsKey(record.getValue(primaryKeyField)))
         {
            recordsForCustomizer.add(record);
         }
      }
      return recordsForCustomizer;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void manageAssociations(Set<Serializable> primaryKeysWithoutErrors, DeleteInput deleteInput) throws QException
   {
      QTableMetaData table = deleteInput.getTable();
      for(Association association : CollectionUtils.nonNullList(table.getAssociations()))
      {
         // e.g., order -> orderLine
         QJoinMetaData join = QContext.getQInstance().getJoin(association.getJoinName()); // todo ... ever need to flip?
         // just assume this, at least for now... if(BooleanUtils.isTrue(association.getDoInserts()))

         QQueryFilter filter = new QQueryFilter();

         if(join.getJoinOns().size() == 1 && join.getJoinOns().get(0).getLeftField().equals(table.getPrimaryKeyField()))
         {
            filter.addCriteria(new QFilterCriteria(join.getJoinOns().get(0).getRightField(), QCriteriaOperator.IN, new ArrayList<>(primaryKeysWithoutErrors)));
         }
         else
         {
            throw (new QException("Join of this type is not supported for an associated delete at this time..."));
         }

         QTableMetaData associatedTable = QContext.getQInstance().getTable(association.getAssociatedTableName());

         QueryInput queryInput = new QueryInput();
         queryInput.setTransaction(deleteInput.getTransaction());
         queryInput.setTableName(association.getAssociatedTableName());
         queryInput.setFilter(filter);
         QueryOutput        queryOutput    = new QueryAction().execute(queryInput);
         List<Serializable> associatedKeys = queryOutput.getRecords().stream().map(r -> r.getValue(associatedTable.getPrimaryKeyField())).toList();

         if(CollectionUtils.nullSafeHasContents(associatedKeys))
         {
            DeleteInput nextLevelDeleteInput = new DeleteInput();
            nextLevelDeleteInput.setFlags(deleteInput.getFlags());
            nextLevelDeleteInput.setTransaction(deleteInput.getTransaction());
            nextLevelDeleteInput.setTableName(association.getAssociatedTableName());
            nextLevelDeleteInput.setPrimaryKeys(associatedKeys);
            DeleteOutput nextLevelDeleteOutput = new DeleteAction().execute(nextLevelDeleteInput);
         }
      }
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
            queryInput.setFilter(new QQueryFilter(new QFilterCriteria(deleteInput.getTable().getPrimaryKeyField(), QCriteriaOperator.IN, primaryKeyList)));
            QueryOutput queryOutput = new QueryAction().execute(queryInput);
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

      List<List<Serializable>> pages = CollectionUtils.getPages(deleteInput.getPrimaryKeys(), 1000);
      for(List<Serializable> page : pages)
      {
         Map<Serializable, QRecord> oldRecordMapByPrimaryKey = new HashMap<>();
         for(QRecord record : oldRecordList)
         {
            Serializable primaryKeyValue = record.getValue(table.getPrimaryKeyField());
            primaryKeyValue = ValueUtils.getValueAsFieldType(primaryKeyField.getType(), primaryKeyValue);
            oldRecordMapByPrimaryKey.put(primaryKeyValue, record);
         }

         for(Serializable primaryKeyValue : page)
         {
            primaryKeyValue = ValueUtils.getValueAsFieldType(primaryKeyField.getType(), primaryKeyValue);
            if(!oldRecordMapByPrimaryKey.containsKey(primaryKeyValue))
            {
               QRecord recordWithError = new QRecord();
               recordsWithErrors.add(recordWithError);
               recordWithError.setValue(primaryKeyField.getName(), primaryKeyValue);
               recordWithError.addError(new NotFoundStatusMessage("No record was found to delete for " + Objects.requireNonNullElse(primaryKeyField.getLabel(), primaryKeyField.getName()) + " = " + primaryKeyValue));
            }
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

         QueryInput queryInput = new QueryInput();
         queryInput.setTransaction(deleteInput.getTransaction());
         queryInput.setTableName(deleteInput.getTableName());
         queryInput.setFilter(deleteInput.getQueryFilter());
         QueryOutput queryOutput = qModule.getQueryInterface().execute(queryInput);

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

}
