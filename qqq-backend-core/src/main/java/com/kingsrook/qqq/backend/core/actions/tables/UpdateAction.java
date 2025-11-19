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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.actions.audits.DMLAuditAction;
import com.kingsrook.qqq.backend.core.actions.automation.AutomationStatus;
import com.kingsrook.qqq.backend.core.actions.automation.RecordAutomationStatusUpdater;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.interfaces.UpdateInterface;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.QueryStatManager;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.ValidateRecordSecurityLockHelper;
import com.kingsrook.qqq.backend.core.actions.values.ValueBehaviorApplier;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.audits.DMLAuditInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DynamicDefaultValueBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLockFilters;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.querystats.QueryStat;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.NotFoundStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.QErrorMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.QWarningMessage;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleInterface;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import org.apache.commons.lang.BooleanUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Action to update one or more records.
 **
 *******************************************************************************/
public class UpdateAction
{
   private static final QLogger LOG = QLogger.getLogger(UpdateAction.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   public QRecord executeForRecord(UpdateInput updateInput) throws QException
   {
      UpdateOutput updateOutput = new UpdateAction().execute(updateInput);
      return (updateOutput.getRecords().get(0));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static List<QRecord> executeForRecords(UpdateInput updateInput) throws QException
   {
      UpdateOutput updateOutput = new UpdateAction().execute(updateInput);
      return (updateOutput.getRecords());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public UpdateOutput execute(UpdateInput updateInput) throws QException
   {
      ActionHelper.validateSession(updateInput);

      if(!StringUtils.hasContent(updateInput.getTableName()))
      {
         throw (new QException("Table name was not specified in update input"));
      }

      QTableMetaData table = updateInput.getTable();
      if(table == null)
      {
         throw (new QException("Error:  Undefined table: " + updateInput.getTableName()));
      }
      table = TableMetaDataPersonalizerAction.execute(updateInput);
      updateInput.setTableMetaData(table);

      //////////////////////////////////////////////////////
      // load the backend module and its update interface //
      //////////////////////////////////////////////////////
      QBackendModuleDispatcher qBackendModuleDispatcher = new QBackendModuleDispatcher();
      QBackendModuleInterface  qModule                  = qBackendModuleDispatcher.getQBackendModule(updateInput.getBackend());
      UpdateInterface          updateInterface          = qModule.getUpdateInterface();

      ////////////////////////////////////////////////////////////////////////////////
      // fetch the old list of records (if the backend supports it), for audits,    //
      // for "not-found detection", and for the pre-action to use (if there is one) //
      ////////////////////////////////////////////////////////////////////////////////
      Optional<List<QRecord>> oldRecordList = fetchOldRecords(updateInput, updateInterface);

      ///////////////////////////////////////////////////////////////////////////////////////
      // allow caller to specify that we don't want to trigger automations. this isn't     //
      // isn't expected to be used much - by design, only for the process that is meant to //
      // heal automation status, so that it can force us into status=Pending-inserts       //
      ///////////////////////////////////////////////////////////////////////////////////////
      if(!updateInput.getOmitTriggeringAutomations())
      {
         setAutomationStatusField(updateInput, oldRecordList);
      }

      performValidations(updateInput, oldRecordList, false);

      ////////////////////////////////////
      // have the backend do the update //
      ////////////////////////////////////
      QueryStat    queryStat    = QueryStatManager.newQueryStat(updateInput.getBackend(), table, null, UpdateAction.class.getSimpleName());
      UpdateOutput updateOutput = runUpdateInBackend(updateInput, updateInterface);

      if(queryStat != null)
      {
         queryStat.setRecordCount(updateInput.getRecords().size());
         QueryStatManager.getInstance().add(queryStat);
      }

      if(updateOutput.getRecords() == null)
      {
         ////////////////////////////////////////////////////////////////////////////////////
         // in case the module failed to set record in the output, put an empty list there //
         // to avoid so many downstream NPE's                                              //
         ////////////////////////////////////////////////////////////////////////////////////
         updateOutput.setRecords(new ArrayList<>());
      }

      //////////////////////////////
      // log if there were errors //
      //////////////////////////////
      List<String> errors = updateOutput.getRecords().stream().flatMap(r -> r.getErrors().stream().map(Object::toString)).toList();
      if(CollectionUtils.nullSafeHasContents(errors))
      {
         LOG.info("Errors in updateAction", logPair("tableName", updateInput.getTableName()), logPair("errorCount", errors.size()), errors.size() < 10 ? logPair("errors", errors) : logPair("first10Errors", errors.subList(0, 10)));
      }

      /////////////////////////////////////////////////////////////////////////////////////
      // update (inserting and deleting as needed) any associations in the input records //
      /////////////////////////////////////////////////////////////////////////////////////
      manageAssociations(updateInput);

      //////////////////
      // do the audit //
      //////////////////
      if(updateInput.getOmitDmlAudit())
      {
         LOG.debug("Requested to omit DML audit");
      }
      else
      {
         DMLAuditInput dmlAuditInput = new DMLAuditInput()
            .withTransaction(updateInput.getTransaction())
            .withTableActionInput(updateInput)
            .withRecordList(updateOutput.getRecords())
            .withAuditContext(updateInput.getAuditContext());
         oldRecordList.ifPresent(l -> dmlAuditInput.setOldRecordList(l));
         new DMLAuditAction().execute(dmlAuditInput);
      }

      //////////////////////////////////////////////////////////////
      // finally, run the post-update customizer, if there is one //
      //////////////////////////////////////////////////////////////
      runPostUpdateCustomizers(updateInput, table, updateOutput, oldRecordList);

      return updateOutput;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static void runPostUpdateCustomizers(UpdateInput updateInput, QTableMetaData table, UpdateOutput updateOutput, Optional<List<QRecord>> oldRecordList)
   {
      Optional<TableCustomizerInterface> postUpdateCustomizer = QCodeLoader.getTableCustomizer(table, TableCustomizers.POST_UPDATE_RECORD.getRole());
      if(postUpdateCustomizer.isPresent())
      {
         try
         {
            updateOutput.setRecords(postUpdateCustomizer.get().postUpdate(updateInput, updateOutput.getRecords(), oldRecordList));
         }
         catch(Exception e)
         {
            for(QRecord record : updateOutput.getRecords())
            {
               record.addWarning(new QWarningMessage("An error occurred after the update: " + e.getMessage()));
            }
         }
      }

      ///////////////////////////////////////////////
      // run all of the instance-level customizers //
      ///////////////////////////////////////////////
      List<QCodeReference> tableCustomizerCodes = QContext.getQInstance().getTableCustomizers(TableCustomizers.POST_UPDATE_RECORD);
      for(QCodeReference tableCustomizerCode : tableCustomizerCodes)
      {
         try
         {
            TableCustomizerInterface tableCustomizer = QCodeLoader.getAdHoc(TableCustomizerInterface.class, tableCustomizerCode);
            updateOutput.setRecords(tableCustomizer.postUpdate(updateInput, updateOutput.getRecords(), oldRecordList));
         }
         catch(Exception e)
         {
            for(QRecord record : updateOutput.getRecords())
            {
               record.addWarning(new QWarningMessage("An error occurred after the update: " + e.getMessage()));
            }
         }
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static void runPreUpdateCustomizers(UpdateInput updateInput, QTableMetaData table, Optional<List<QRecord>> oldRecordList, boolean isPreview) throws QException
   {
      Optional<TableCustomizerInterface> preUpdateCustomizer = QCodeLoader.getTableCustomizer(table, TableCustomizers.PRE_UPDATE_RECORD.getRole());
      if(preUpdateCustomizer.isPresent())
      {
         updateInput.setRecords(preUpdateCustomizer.get().preUpdate(updateInput, updateInput.getRecords(), isPreview, oldRecordList));
      }

      ///////////////////////////////////////////////
      // run all of the instance-level customizers //
      ///////////////////////////////////////////////
      List<QCodeReference> tableCustomizerCodes = QContext.getQInstance().getTableCustomizers(TableCustomizers.PRE_UPDATE_RECORD);
      for(QCodeReference tableCustomizerCode : tableCustomizerCodes)
      {
         TableCustomizerInterface tableCustomizer = QCodeLoader.getAdHoc(TableCustomizerInterface.class, tableCustomizerCode);
         updateInput.setRecords(tableCustomizer.preUpdate(updateInput, updateInput.getRecords(), isPreview, oldRecordList));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private UpdateOutput runUpdateInBackend(UpdateInput updateInput, UpdateInterface updateInterface) throws QException
   {
      ///////////////////////////////////
      // exit early if 0 input records //
      ///////////////////////////////////
      if(CollectionUtils.nullSafeIsEmpty(updateInput.getRecords()))
      {
         LOG.debug("Update request called with 0 records.  Returning with no-op", logPair("tableName", updateInput.getTableName()));
         UpdateOutput rs = new UpdateOutput();
         rs.setRecords(new ArrayList<>());
         return (rs);
      }

      UpdateOutput updateOutput = updateInterface.execute(updateInput);
      return updateOutput;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void performValidations(UpdateInput updateInput, Optional<List<QRecord>> oldRecordList, boolean isPreview) throws QException
   {
      QTableMetaData table = updateInput.getTable();

      /////////////////////////////
      // run standard validators //
      /////////////////////////////
      Set<FieldBehavior<?>> behaviorsToOmit = null;
      if(BooleanUtils.isTrue(updateInput.getOmitModifyDateUpdate()))
      {
         behaviorsToOmit = Set.of(DynamicDefaultValueBehavior.MODIFY_DATE);
      }

      ValueBehaviorApplier.applyFieldBehaviors(ValueBehaviorApplier.Action.UPDATE, QContext.getQInstance(), table, updateInput.getRecords(), behaviorsToOmit);
      validatePrimaryKeysAreGiven(updateInput);

      if(oldRecordList.isPresent())
      {
         validateRecordsExistAndCanBeAccessed(updateInput, oldRecordList.get());
      }
      else
      {
         ValidateRecordSecurityLockHelper.validateSecurityFields(table, updateInput.getRecords(), ValidateRecordSecurityLockHelper.Action.UPDATE, updateInput.getTransaction());
      }

      if(updateInput.getInputSource().shouldValidateRequiredFields())
      {
         validateRequiredFields(updateInput);
      }

      ///////////////////////////////////////////////////////////////////////////
      // after all validations, run the pre-update customizer, if there is one //
      ///////////////////////////////////////////////////////////////////////////
      runPreUpdateCustomizers(updateInput, table, oldRecordList, isPreview);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Optional<List<QRecord>> fetchOldRecords(UpdateInput updateInput, UpdateInterface updateInterface) throws QException
   {
      if(updateInterface.supportsPreFetchQuery())
      {
         String             primaryKeyField   = updateInput.getTable().getPrimaryKeyField();
         List<Serializable> pkeysBeingUpdated = CollectionUtils.nonNullList(updateInput.getRecords()).stream().map(r -> r.getValue(primaryKeyField)).toList();

         QueryInput queryInput = new QueryInput();
         queryInput.setTransaction(updateInput.getTransaction());
         queryInput.setTableName(updateInput.getTableName());
         queryInput.setFilter(new QQueryFilter(new QFilterCriteria(primaryKeyField, QCriteriaOperator.IN, pkeysBeingUpdated)));
         // todo - need a limit?  what if too many??
         QueryOutput queryOutput = new QueryAction().execute(queryInput);

         return (Optional.of(queryOutput.getRecords()));
      }

      return (Optional.empty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void validatePrimaryKeysAreGiven(UpdateInput updateInput)
   {
      QTableMetaData table = updateInput.getTable();
      for(QRecord record : CollectionUtils.nonNullList(updateInput.getRecords()))
      {
         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // to update a record, we must have its primary key value - so - check - if it's missing, mark it as an error //
         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         if(record.getValue(table.getPrimaryKeyField()) == null)
         {
            record.addError(new BadInputStatusMessage("Missing value in primary key field"));
         }
      }
   }



   /*******************************************************************************
    ** Note - the "can be accessed" part of this method name - it implies that
    ** records that you can't see because of security - that they won't be found
    ** by the query here, so it's the same to you as if they don't exist at all!
    *******************************************************************************/
   private void validateRecordsExistAndCanBeAccessed(UpdateInput updateInput, List<QRecord> oldRecordList) throws QException
   {
      QTableMetaData table           = updateInput.getTable();
      QFieldMetaData primaryKeyField = table.getField(table.getPrimaryKeyField());

      /////////////////////////////////////////////////////////////
      // todo - evolve to use lock tree (e.g., from multi-locks) //
      /////////////////////////////////////////////////////////////
      List<RecordSecurityLock> onlyWriteLocks = RecordSecurityLockFilters.filterForOnlyWriteLocks(CollectionUtils.nonNullList(table.getRecordSecurityLocks()));

      for(List<QRecord> page : CollectionUtils.getPages(updateInput.getRecords(), 1000))
      {
         List<Serializable> primaryKeysToLookup = new ArrayList<>();
         for(QRecord record : page)
         {
            Serializable primaryKeyValue = record.getValue(table.getPrimaryKeyField());
            if(primaryKeyValue != null)
            {
               primaryKeysToLookup.add(primaryKeyValue);
            }
         }

         Map<Serializable, QRecord> lookedUpRecords = new HashMap<>();
         if(CollectionUtils.nullSafeHasContents(oldRecordList))
         {
            for(QRecord record : oldRecordList)
            {
               lookedUpRecords.put(record.getValue(table.getPrimaryKeyField()), record);
            }
         }
         else if(!primaryKeysToLookup.isEmpty())
         {
            QueryInput queryInput = new QueryInput();
            queryInput.setTransaction(updateInput.getTransaction());
            queryInput.setTableName(table.getName());
            queryInput.setFilter(new QQueryFilter(new QFilterCriteria(table.getPrimaryKeyField(), QCriteriaOperator.IN, primaryKeysToLookup)));
            QueryOutput queryOutput = new QueryAction().execute(queryInput);
            for(QRecord record : queryOutput.getRecords())
            {
               lookedUpRecords.put(record.getValue(table.getPrimaryKeyField()), record);
            }
         }

         ValidateRecordSecurityLockHelper.validateSecurityFields(table, updateInput.getRecords(), ValidateRecordSecurityLockHelper.Action.UPDATE, updateInput.getTransaction());

         for(QRecord record : page)
         {
            Serializable value = ValueUtils.getValueAsFieldType(primaryKeyField.getType(), record.getValue(table.getPrimaryKeyField()));
            if(value == null)
            {
               continue;
            }

            if(!lookedUpRecords.containsKey(value))
            {
               record.addError(new NotFoundStatusMessage("No record was found to update for " + primaryKeyField.getLabel() + " = " + value));
            }
            else
            {
               ///////////////////////////////////////////////////////////////////////////////////////////
               // if the table has any write-only locks, validate their values here, on the old-records //
               ///////////////////////////////////////////////////////////////////////////////////////////
               for(RecordSecurityLock lock : onlyWriteLocks)
               {
                  QRecord      oldRecord = lookedUpRecords.get(value);
                  QFieldType   fieldType = table.getField(lock.getFieldName()).getType();
                  Serializable lockValue = ValueUtils.getValueAsFieldType(fieldType, oldRecord.getValue(lock.getFieldName()));

                  List<QErrorMessage> errors = ValidateRecordSecurityLockHelper.validateRecordSecurityValue(table, lock, lockValue, fieldType, ValidateRecordSecurityLockHelper.Action.UPDATE, Collections.emptyMap(), QContext.getQSession());
                  if(CollectionUtils.nullSafeHasContents(errors))
                  {
                     errors.forEach(e -> record.addError(e));
                  }
               }
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void validateRequiredFields(UpdateInput updateInput)
   {
      QTableMetaData table = updateInput.getTable();
      Set<QFieldMetaData> requiredFields = table.getFields().values().stream()
         .filter(f -> f.getIsRequired())
         .collect(Collectors.toSet());

      if(!requiredFields.isEmpty())
      {
         for(QRecord record : CollectionUtils.nonNullList(updateInput.getRecords()))
         {
            for(QFieldMetaData requiredField : requiredFields)
            {
               /////////////////////////////////////////////////////////////////////////////////////////////
               // only consider fields that were set in the record to be updated (e.g., "patch" semantic) //
               /////////////////////////////////////////////////////////////////////////////////////////////
               if(record.getValues().containsKey(requiredField.getName()))
               {
                  if(record.getValue(requiredField.getName()) == null || record.getValueString(requiredField.getName()).trim().equals(""))
                  {
                     record.addError(new BadInputStatusMessage("Missing value in required field: " + requiredField.getLabel()));
                  }
               }
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void manageAssociations(UpdateInput updateInput) throws QException
   {
      QTableMetaData table = updateInput.getTable();
      for(Association association : CollectionUtils.nonNullList(table.getAssociations()))
      {
         // e.g., order -> orderLine
         QTableMetaData associatedTable = QContext.getQInstance().getTable(association.getAssociatedTableName());
         QJoinMetaData  join            = QContext.getQInstance().getJoin(association.getJoinName()); // todo ... ever need to flip?
         // just assume this, at least for now... if(BooleanUtils.isTrue(association.getDoInserts()))

         for(List<QRecord> page : CollectionUtils.getPages(updateInput.getRecords(), 500))
         {
            List<QRecord> nextLevelUpdates  = new ArrayList<>();
            List<QRecord> nextLevelInserts  = new ArrayList<>();
            QQueryFilter  findDeletesFilter = new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR);
            boolean       lookForDeletes    = false;

            //////////////////////////////////////////////////////
            // for each updated record, look at as associations //
            //////////////////////////////////////////////////////
            for(QRecord record : page)
            {
               if(CollectionUtils.nullSafeHasContents(record.getErrors()))
               {
                  continue;
               }

               if(record.getAssociatedRecords() != null && record.getAssociatedRecords().containsKey(association.getName()))
               {
                  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  // build a sub-query to find the children of this record - and we'll exclude (below) any whose ids are given //
                  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  QQueryFilter subFilter = new QQueryFilter();
                  findDeletesFilter.addSubFilter(subFilter);
                  lookForDeletes = true;
                  List<Serializable> idsBeingUpdated = new ArrayList<>();
                  for(JoinOn joinOn : join.getJoinOns())
                  {
                     subFilter.addCriteria(new QFilterCriteria(joinOn.getRightField(), QCriteriaOperator.EQUALS, record.getValue(joinOn.getLeftField())));
                  }

                  //////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  // for any associated records present here, figure out if they're being inserted (no primaryKey) or updated //
                  //////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  for(QRecord associatedRecord : CollectionUtils.nonNullList(record.getAssociatedRecords().get(association.getName())))
                  {
                     Serializable associatedId = associatedRecord.getValue(associatedTable.getPrimaryKeyField());
                     if(associatedId == null)
                     {
                        //////////////////////////////////////////////////////////////////////////////////////////////////////////
                        // if inserting, add to the inserts list, and propagate values from the header record down to the child //
                        //////////////////////////////////////////////////////////////////////////////////////////////////////////
                        for(JoinOn joinOn : join.getJoinOns())
                        {
                           QFieldType type = table.getField(joinOn.getLeftField()).getType();
                           associatedRecord.setValue(joinOn.getRightField(), ValueUtils.getValueAsFieldType(type, record.getValue(joinOn.getLeftField())));
                        }
                        nextLevelInserts.add(associatedRecord);
                     }
                     else
                     {
                        ///////////////////////////////////////////////////////////////////////////////
                        // if updating, add to the updates list, and add the id as one to not delete //
                        ///////////////////////////////////////////////////////////////////////////////
                        idsBeingUpdated.add(associatedId);
                        nextLevelUpdates.add(associatedRecord);

                        /////////////////////////////////////////////////////////////////////////////////////////////////
                        // make sure the child record being updated has its join fields populated (same as an insert). //
                        // this will make the next update action much happier                                          //
                        /////////////////////////////////////////////////////////////////////////////////////////////////
                        for(JoinOn joinOn : join.getJoinOns())
                        {
                           QFieldType type = table.getField(joinOn.getLeftField()).getType();
                           associatedRecord.setValue(joinOn.getRightField(), ValueUtils.getValueAsFieldType(type, record.getValue(joinOn.getLeftField())));
                        }
                     }
                  }

                  if(!idsBeingUpdated.isEmpty())
                  {
                     ///////////////////////////////////////////////////////////////////////////////
                     // if any records are being updated, add them to the query to NOT be deleted //
                     ///////////////////////////////////////////////////////////////////////////////
                     subFilter.addCriteria(new QFilterCriteria(associatedTable.getPrimaryKeyField(), QCriteriaOperator.NOT_IN, idsBeingUpdated));
                  }
               }
            }

            if(lookForDeletes)
            {
               QueryInput queryInput = new QueryInput();
               queryInput.setTransaction(updateInput.getTransaction());
               queryInput.setTableName(associatedTable.getName());
               queryInput.setFilter(findDeletesFilter);
               QueryOutput queryOutput = new QueryAction().execute(queryInput);
               if(!queryOutput.getRecords().isEmpty())
               {
                  LOG.debug("Deleting associatedRecords", logPair("associatedTable", associatedTable.getName()), logPair("noOfRecords", queryOutput.getRecords().size()));
                  DeleteInput deleteInput = new DeleteInput();
                  deleteInput.setFlags(updateInput.getFlags());
                  deleteInput.setTransaction(updateInput.getTransaction());
                  deleteInput.setTableName(association.getAssociatedTableName());
                  deleteInput.setPrimaryKeys(queryOutput.getRecords().stream().map(r -> r.getValue(associatedTable.getPrimaryKeyField())).collect(Collectors.toList()));
                  DeleteOutput deleteOutput = new DeleteAction().execute(deleteInput);
               }
            }

            if(CollectionUtils.nullSafeHasContents(nextLevelUpdates))
            {
               LOG.debug("Updating associatedRecords", logPair("associatedTable", associatedTable.getName()), logPair("noOfRecords", nextLevelUpdates.size()));
               UpdateInput nextLevelUpdateInput = new UpdateInput();
               nextLevelUpdateInput.setTransaction(updateInput.getTransaction());
               nextLevelUpdateInput.setFlags(updateInput.getFlags());
               nextLevelUpdateInput.setTableName(association.getAssociatedTableName());
               nextLevelUpdateInput.setRecords(nextLevelUpdates);
               UpdateOutput nextLevelUpdateOutput = new UpdateAction().execute(nextLevelUpdateInput);
            }

            if(CollectionUtils.nullSafeHasContents(nextLevelInserts))
            {
               LOG.debug("Inserting associatedRecords", logPair("associatedTable", associatedTable.getName()), logPair("noOfRecords", nextLevelUpdates.size()));
               InsertInput nextLevelInsertInput = new InsertInput();
               nextLevelInsertInput.setTransaction(updateInput.getTransaction());
               nextLevelInsertInput.setFlags(updateInput.getFlags());
               nextLevelInsertInput.setTableName(association.getAssociatedTableName());
               nextLevelInsertInput.setRecords(nextLevelInserts);
               InsertOutput nextLevelInsertOutput = new InsertAction().execute(nextLevelInsertInput);
            }
         }
      }
   }



   /*******************************************************************************
    ** If the table being updated uses an automation-status field, populate it now.
    *******************************************************************************/
   private void setAutomationStatusField(UpdateInput updateInput, Optional<List<QRecord>> oldRecordList)
   {
      RecordAutomationStatusUpdater.setAutomationStatusInRecords(updateInput.getTable(), updateInput.getRecords(), AutomationStatus.PENDING_UPDATE_AUTOMATIONS, updateInput.getTransaction(), oldRecordList.orElse(null));
   }

}
