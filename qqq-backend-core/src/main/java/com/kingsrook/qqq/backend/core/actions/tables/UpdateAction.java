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
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import com.kingsrook.qqq.backend.core.actions.tables.helpers.AssociatedRecordDiscovery;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.AssociatedRecordUpdate;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.AssociationJoin;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.QueryStatManager;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.UniqueKeyHelper;
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
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.querystats.QueryStat;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.NotFoundStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.PermissionDeniedMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.QErrorMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.QWarningMessage;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleInterface;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import org.apache.commons.lang3.BooleanUtils;
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
      return execute(updateInput, false);
   }



   /*******************************************************************************
    ** Replacement trees retain failed write outcomes before POST presentation.
    *******************************************************************************/
   UpdateOutput execute(UpdateInput updateInput, boolean failOnRecordErrors) throws QException
   {
      QContext.pushAction(updateInput);
      try
      {
         return executeInternal(updateInput, failOnRecordErrors);
      }
      finally
      {
         QContext.popAction();
      }
   }



   /*******************************************************************************
    ** Keep the owning action visible to nested DML callbacks until it completes.
    *******************************************************************************/
   private UpdateOutput executeInternal(UpdateInput updateInput, boolean failOnRecordErrors) throws QException
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
      Map<QRecord, AssociatedRecordUpdate.Values> associationValues = AssociatedRecordUpdate.prepare(table, updateInput.getRecords(), updateInput.getTransaction());
      deleteOmittedAssociations(updateInput, associationValues);

      ////////////////////////////////////
      // have the backend do the update //
      ////////////////////////////////////
      QueryStat    queryStat    = QueryStatManager.newQueryStat(updateInput.getBackend(), table, null, UpdateAction.class.getSimpleName());
      UpdateOutput updateOutput = runUpdateInBackend(updateInput, updateInterface);

      if(queryStat != null)
      {
         queryStat.setRecordCount(updateInput.getRecords() == null ? 0 : updateInput.getRecords().size());
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

      if(failOnRecordErrors)
      {
         ReplaceAction.requireSuccessfulRecords(updateOutput.getRecords(), "UPDATE");
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
      manageAssociations(updateInput, updateOutput, associationValues, failOnRecordErrors);

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
      if(failOnRecordErrors)
      {
         ReplaceAction.requireSuccessfulRecords(updateOutput.getRecords(), "UPDATE");
      }
      runPostUpdateCustomizers(updateInput, table, updateOutput, oldRecordList);
      if(failOnRecordErrors)
      {
         ReplaceAction.requireSuccessfulRecords(updateOutput.getRecords(), "UPDATE");
      }

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
      UniqueKeyHelper.validateUpdateUniqueKeys(updateInput);
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
         queryInput.setTableMetaData(updateInput.getTable());
         queryInput.setInputSource(updateInput.getInputSource());
         queryInput.setFilter(new QQueryFilter(new QFilterCriteria(primaryKeyField, QCriteriaOperator.IN, pkeysBeingUpdated)));
         // todo - need a limit?  what if too many??
         QueryOutput queryOutput = new QueryAction().executeForDml(queryInput);

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

      List<QRecord> records = CollectionUtils.nonNullList(updateInput.getRecords());
      if(records.isEmpty())
      {
         return;
      }

      Map<Object, QRecord> lookedUpRecords = new HashMap<>();
      List<QRecord> recordsToCheck = oldRecordList;
      if(CollectionUtils.nullSafeIsEmpty(recordsToCheck))
      {
         List<Serializable> primaryKeys = records.stream()
            .map(record -> record.getValue(table.getPrimaryKeyField())).filter(Objects::nonNull).toList();
         if(!primaryKeys.isEmpty())
         {
            QueryInput queryInput = new QueryInput(table.getName()).withTransaction(updateInput.getTransaction())
               .withFilter(new QQueryFilter(new QFilterCriteria(table.getPrimaryKeyField(), QCriteriaOperator.IN, primaryKeys)));
            queryInput.setTableMetaData(table);
            queryInput.setInputSource(updateInput.getInputSource());
            recordsToCheck = new QueryAction().executeForDml(queryInput).getRecords();
         }
      }
      for(QRecord record : CollectionUtils.nonNullList(recordsToCheck))
      {
         lookedUpRecords.put(AssociatedRecordUpdate.primaryKey(table, record), record);
      }

      ValidateRecordSecurityLockHelper.validateSecurityFields(table, records, ValidateRecordSecurityLockHelper.Action.UPDATE, updateInput.getTransaction());
      Map<Object, QRecord> authorizedOldRecords = ValidateRecordSecurityLockHelper.validateStoredWriteLocks(table, lookedUpRecords, ValidateRecordSecurityLockHelper.Action.UPDATE, updateInput.getTransaction());

      for(QRecord record : records)
      {
         Serializable value = ValueUtils.getValueAsFieldType(primaryKeyField.getType(), record.getValue(table.getPrimaryKeyField()));
         if(value == null)
         {
            continue;
         }

         Object key = AssociatedRecordUpdate.primaryKey(table, record);
         if(!lookedUpRecords.containsKey(key))
         {
            record.addError(new NotFoundStatusMessage("No record was found to update for " + primaryKeyField.getLabel() + " = " + value));
         }
         else if(authorizedOldRecords != null)
         {
            QRecord oldRecord = authorizedOldRecords.get(key);
            if(oldRecord == null || CollectionUtils.nullSafeHasContents(oldRecord.getErrors()))
            {
               record.addError(new PermissionDeniedMessage("You do not have permission to update this record."));
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
                  if(record.getValue(requiredField.getName()) == null || ((requiredField.getType().isStringLike() || record.getValue(requiredField.getName()) instanceof String) && record.getValueString(requiredField.getName()).trim().equals("")))
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
   private void deleteOmittedAssociations(UpdateInput input, Map<QRecord, AssociatedRecordUpdate.Values> associationValues) throws QException
   {
      if(associationValues.isEmpty())
      {
         return;
      }
      QTableMetaData table = AssociatedRecordDiscovery.physicalParentTable(input.getTable());
      for(QRecord record : input.getRecords())
      {
         AssociatedRecordUpdate.Values values = associationValues.get(record);
         if(values == null || CollectionUtils.nullSafeHasContents(record.getErrors()))
         {
            continue;
         }
         for(Association association : table.getAssociations())
         {
            if(!record.getAssociatedRecords().containsKey(association.getName())
               || AssociationJoin.resolve(table, association).parentValues(values.before()).contains(null))
            {
               continue;
            }
            QTableMetaData childTable = QContext.getQInstance().getTable(association.getAssociatedTableName());
            List<Serializable> retainedKeys = new ArrayList<>();
            for(QRecord child : CollectionUtils.nonNullList(record.getAssociatedRecords().get(association.getName())))
            {
               Serializable key = child.getValue(childTable.getPrimaryKeyField());
               if(key != null)
               {
                  retainedKeys.add(key);
               }
            }
            List<Serializable> omittedKeys = AssociatedRecordDiscovery.findPrimaryKeys(table, association,
               List.of(new AssociatedRecordDiscovery.Parent(values.before().getValues(), retainedKeys)), input.getTransaction());
            if(omittedKeys.isEmpty())
            {
               continue;
            }
            DeleteInput childInput = new DeleteInput(childTable.getName()).withPrimaryKeys(omittedKeys)
               .withInputSource(input.getInputSource()).withFlags(input.getFlags()).withTransaction(input.getTransaction())
               .withOmitDmlAudit(input.getOmitDmlAudit()).withAuditContext(input.getAuditContext());
            DeleteOutput childOutput = new DeleteAction().executeForAssociation(childInput, table, record);
            if(CollectionUtils.nullSafeHasContents(childOutput.getRecordsWithErrors()) || childOutput.getDeletedRecordCount() != omittedKeys.size())
            {
               record.addError(DeleteAction.associationFailure(childOutput, association.getName()));
               break;
            }
            if(CollectionUtils.nullSafeHasContents(childOutput.getRecordsWithWarnings()))
            {
               record.addWarning(new QWarningMessage("Warnings occurred deleting omitted records for association [" + association.getName() + "]"));
            }
         }
      }
   }



   /*******************************************************************************
    ** Apply retained/new child writes only after a successful parent update.
    *******************************************************************************/
   private void manageAssociations(UpdateInput updateInput, UpdateOutput updateOutput, Map<QRecord, AssociatedRecordUpdate.Values> associationValues, boolean failOnRecordErrors) throws QException
   {
      if(associationValues.isEmpty())
      {
         return;
      }
      QTableMetaData table = AssociatedRecordDiscovery.physicalParentTable(updateInput.getTable());
      Map<Object, QRecord> outputsByKey = new HashMap<>();
      for(QRecord output : updateOutput.getRecords())
      {
         outputsByKey.put(AssociatedRecordUpdate.primaryKey(table, output), output);
      }
      for(Association association : CollectionUtils.nonNullList(table.getAssociations()))
      {
         QTableMetaData associatedTable = QContext.getQInstance().getTable(association.getAssociatedTableName());
         AssociationJoin join = AssociationJoin.resolve(table, association);
         for(List<QRecord> page : CollectionUtils.getPages(updateInput.getRecords(), 500))
         {
            List<QRecord> nextLevelUpdates = new ArrayList<>();
            List<QRecord> nextLevelInserts = new ArrayList<>();
            List<QRecord> originalUpdates = new ArrayList<>();
            List<QRecord> originalInserts = new ArrayList<>();
            List<Set<String>> privateUpdateFields = new ArrayList<>();
            List<Set<String>> privateInsertFields = new ArrayList<>();
            for(QRecord record : page)
            {
               AssociatedRecordUpdate.Values values = associationValues.get(record);
               QRecord output = outputsByKey.get(AssociatedRecordUpdate.primaryKey(table, record));
               if(values == null || output == null || CollectionUtils.nullSafeHasContents(record.getErrors())
                  || CollectionUtils.nullSafeHasContents(output.getErrors()) || record.getAssociatedRecords() == null
                  || !record.getAssociatedRecords().containsKey(association.getName()))
               {
                  continue;
               }

               Set<String> privateFields = new HashSet<>();
               for(JoinOn joinOn : join.getJoinOns())
               {
                  if(values.storedFields().contains(joinOn.getLeftField()))
                  {
                     privateFields.add(joinOn.getRightField());
                  }
               }
               for(QRecord original : CollectionUtils.nonNullList(record.getAssociatedRecords().get(association.getName())))
               {
                  Serializable associatedId = original.getValue(associatedTable.getPrimaryKeyField());
                  QRecord working = new QRecord(original);
                  join.assignParentValues(values.after(), working);
                  if(associatedId == null)
                  {
                     nextLevelInserts.add(working);
                     originalInserts.add(original);
                     privateInsertFields.add(privateFields);
                  }
                  else
                  {
                     nextLevelUpdates.add(working);
                     originalUpdates.add(original);
                     privateUpdateFields.add(privateFields);
                  }
               }

               output.withAssociatedRecords(association.getName(), record.getAssociatedRecords().get(association.getName()));
            }

            if(!nextLevelUpdates.isEmpty())
            {
               UpdateInput childInput = new UpdateInput();
               childInput.setInputSource(updateInput.getInputSource());
               childInput.setTransaction(updateInput.getTransaction());
               childInput.setFlags(updateInput.getFlags());
               childInput.setTableName(association.getAssociatedTableName());
               childInput.setRecords(nextLevelUpdates);
               UpdateOutput childOutput = new UpdateAction().execute(childInput, failOnRecordErrors);
               copyAssociatedResults(associatedTable, childOutput.getRecords(), nextLevelUpdates, originalUpdates, privateUpdateFields);
            }
            if(!nextLevelInserts.isEmpty())
            {
               InsertInput childInput = new InsertInput();
               childInput.setInputSource(updateInput.getInputSource());
               childInput.setTransaction(updateInput.getTransaction());
               childInput.setFlags(updateInput.getFlags());
               childInput.setTableName(association.getAssociatedTableName());
               childInput.setRecords(nextLevelInserts);
               InsertOutput childOutput = new InsertAction().execute(childInput, failOnRecordErrors);
               copyAssociatedResults(associatedTable, childOutput.getRecords(), nextLevelInserts, originalInserts, privateInsertFields);
            }
         }
      }
   }



   /*******************************************************************************
    ** Publish normalized input values, generated keys and statuses without private
    ** values. Track those fields through descendants, including shared keys.
    *******************************************************************************/
   private void copyAssociatedResults(QTableMetaData table, List<QRecord> outputs, List<QRecord> workingRecords, List<QRecord> originals, List<Set<String>> privateFields) throws QException
   {
      if(outputs == null || outputs.size() != originals.size())
      {
         throw new QException("Incomplete associated write results for table [" + table.getName() + "]");
      }
      for(int i = 0; i < originals.size(); i++)
      {
         QRecord original = originals.get(i);
         QRecord output = outputs.get(i);
         QRecord working = workingRecords.get(i);
         Map<String, Serializable> publicValues = new HashMap<>(working.getValues());
         for(String field : privateFields.get(i))
         {
            if(original.getValues().containsKey(field))
            {
               publicValues.put(field, original.getValue(field));
            }
            else
            {
               publicValues.remove(field);
            }
         }
         original.setValues(publicValues);
         String primaryKey = table.getPrimaryKeyField();
         if(original.getValue(primaryKey) == null && !privateFields.get(i).contains(primaryKey))
         {
            original.setValue(primaryKey, output.getValue(primaryKey));
         }
         for(QErrorMessage error : CollectionUtils.nonNullList(output.getErrors()))
         {
            if(!original.getErrors().contains(error))
            {
               original.addError(error);
            }
         }
         for(QWarningMessage warning : CollectionUtils.nonNullList(output.getWarnings()))
         {
            if(!original.getWarnings().contains(warning))
            {
               original.addWarning(warning);
            }
         }
         for(Association association : CollectionUtils.nonNullList(table.getAssociations()))
         {
            if(original.getAssociatedRecords() == null || output.getAssociatedRecords() == null
               || !original.getAssociatedRecords().containsKey(association.getName())
               || !output.getAssociatedRecords().containsKey(association.getName()))
            {
               continue;
            }
            Set<String> childPrivateFields = new HashSet<>();
            for(JoinOn joinOn : AssociationJoin.resolve(table, association).getJoinOns())
            {
               if(privateFields.get(i).contains(joinOn.getLeftField()))
               {
                  childPrivateFields.add(joinOn.getRightField());
               }
            }
            List<QRecord> originalChildren = CollectionUtils.nonNullList(original.getAssociatedRecords().get(association.getName()));
            copyAssociatedResults(QContext.getQInstance().getTable(association.getAssociatedTableName()),
               CollectionUtils.nonNullList(output.getAssociatedRecords().get(association.getName())),
               CollectionUtils.nonNullList(working.getAssociatedRecords().get(association.getName())), originalChildren,
               Collections.nCopies(originalChildren.size(), childPrivateFields));
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
