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

package com.kingsrook.qqq.backend.core.actions.tables;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.audits.DMLAuditAction;
import com.kingsrook.qqq.backend.core.actions.automation.AutomationStatus;
import com.kingsrook.qqq.backend.core.actions.automation.RecordAutomationStatusUpdater;
import com.kingsrook.qqq.backend.core.actions.collaboration.RecordReferenceSecurityHelper;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.interfaces.UpdateInterface;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.actions.permissions.CorePermissionEnforcer;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessHelper;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.AssociatedRecordDiscovery;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.AssociatedRecordUpdate;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.AssociationJoin;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.QueryStatManager;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.RecordVersionCheck;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.RestoreWriteHelper;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.UniqueKeyHelper;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.ValidateContingentPossibleValuesHelper;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.ValidateRecordSecurityLockHelper;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeEvent;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeListenerHelper;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeType;
import com.kingsrook.qqq.backend.core.actions.values.ValueBehaviorApplier;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.audits.DMLAuditInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.RestoreWriteFlag;
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
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
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
      CorePermissionEnforcer.checkRestoreWriteFlag(updateInput, updateInput.getFlags());

      boolean enforce = false;
      if(StringUtils.hasContent(updateInput.getTableName()) && updateInput.getTable() != null)
      {
         CorePermissionEnforcer.checkTablePermissions(updateInput, TablePermissionSubType.EDIT);
         enforce = CorePermissionEnforcer.shouldEnforceFieldAccess(updateInput);
         if(enforce)
         {
            FieldAccessHelper.assertWritesAllowed(updateInput.getTable(), updateInput.getRecords());
            if(CorePermissionEnforcer.shouldEnforce(updateInput))
            {
               CorePermissionEnforcer.checkAssociatedWrites(updateInput);
            }
         }
      }

      QContext.pushAction(updateInput);
      try(CorePermissionEnforcer.AuthorizedScope ignored = CorePermissionEnforcer.openAuthorizedScope())
      {
         UpdateOutput updateOutput = executeInternal(updateInput, failOnRecordErrors);
         if(enforce)
         {
            FieldAccessHelper.applyToRecords(updateInput.getTableName(), null, updateOutput.getRecords(), "update");
         }
         return (updateOutput);
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
      Optional<List<QRecord>> oldRecordList      = fetchOldRecords(updateInput, updateInterface);
      if(updateInput.getExpectedVersions() != null && !updateInput.getExpectedVersions().isEmpty() && oldRecordList.isEmpty())
      {
         oldRecordList = Optional.of(queryOldRecords(updateInput));
      }
      RecordVersionCheck.check(updateInput, oldRecordList.orElse(List.of()));
      List<QRecord> listenerOldRecords = fetchOldRecordsForListeners(updateInput, oldRecordList);

      ///////////////////////////////////////////////////////////////////////////////////////
      // allow caller to specify that we don't want to trigger automations. this isn't     //
      // isn't expected to be used much - by design, only for the process that is meant to //
      // heal automation status, so that it can force us into status=Pending-inserts       //
      ///////////////////////////////////////////////////////////////////////////////////////
      RestoreWriteFlag restoreWriteFlag    = RestoreWriteFlag.from(updateInput.getFlags());
      boolean          suppressAutomations = restoreWriteFlag != null && restoreWriteFlag.getSuppressAutomations();
      if(!updateInput.getOmitTriggeringAutomations() && !suppressAutomations)
      {
         setAutomationStatusField(updateInput, oldRecordList);
      }

      performValidations(updateInput, oldRecordList, updateInput.getDryRun());
      if(updateInput.getDryRun())
      {
         return (buildDryRunOutput(updateInput, table, oldRecordList));
      }

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
      else if(restoreWriteFlag != null && !restoreWriteFlag.getPerRecordAudit())
      {
         RestoreWriteHelper.writeSummaryAudit(updateInput, "update", updateOutput.getRecords());
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

      fireRecordChangeListeners(updateInput, table, updateOutput.getRecords(), listenerOldRecords);

      //////////////////////////////////////////////////////////////
      // finally, run the post-update customizer, if there is one //
      //////////////////////////////////////////////////////////////
      if(failOnRecordErrors)
      {
         ReplaceAction.requireSuccessfulRecords(updateOutput.getRecords(), "UPDATE");
      }
      if(!skipsCustomizers(updateInput))
      {
         runPostUpdateCustomizers(updateInput, table, updateOutput, oldRecordList);
      }
      if(failOnRecordErrors)
      {
         ReplaceAction.requireSuccessfulRecords(updateOutput.getRecords(), "UPDATE");
      }

      return updateOutput;
   }



   /***************************************************************************
    ** for a dry run: the would-be records after the update - each input record's
    ** values overlaid on its old record (when old records could be fetched),
    ** keeping the input record's errors and warnings.
    ***************************************************************************/
   private static UpdateOutput buildDryRunOutput(UpdateInput updateInput, QTableMetaData table, Optional<List<QRecord>> oldRecordList)
   {
      Map<Serializable, QRecord> oldRecordsByPrimaryKey = new HashMap<>();
      for(QRecord oldRecord : oldRecordList.orElse(List.of()))
      {
         oldRecordsByPrimaryKey.put(ValueUtils.getValueAsString(oldRecord.getValue(table.getPrimaryKeyField())), oldRecord);
      }

      List<QRecord> outputRecords = new ArrayList<>();
      for(QRecord inputRecord : CollectionUtils.nonNullList(updateInput.getRecords()))
      {
         QRecord oldRecord = oldRecordsByPrimaryKey.get(ValueUtils.getValueAsString(inputRecord.getValue(table.getPrimaryKeyField())));
         QRecord wouldBe   = oldRecord == null ? new QRecord() : new QRecord(oldRecord);
         wouldBe.setTableName(table.getName());
         wouldBe.getValues().putAll(inputRecord.getValues());
         wouldBe.setErrors(new ArrayList<>(CollectionUtils.nonNullList(inputRecord.getErrors())));
         wouldBe.setWarnings(new ArrayList<>(CollectionUtils.nonNullList(inputRecord.getWarnings())));
         outputRecords.add(wouldBe);
      }

      UpdateOutput updateOutput = new UpdateOutput();
      updateOutput.setRecords(outputRecords);
      return (updateOutput);
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
    ** In restore mode, pre- and post-update customizers (table-level and
    ** instance-level) only run if the flag says so.
    *******************************************************************************/
   private static boolean skipsCustomizers(UpdateInput updateInput)
   {
      RestoreWriteFlag restoreWriteFlag = RestoreWriteFlag.from(updateInput.getFlags());
      return (restoreWriteFlag != null && !restoreWriteFlag.getRunCustomizers());
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

      if(RestoreWriteFlag.from(updateInput.getFlags()) != null)
      {
         RestoreWriteHelper.applyFieldBehaviorsKeepingDates(ValueBehaviorApplier.Action.UPDATE, table, updateInput.getRecords(), behaviorsToOmit);
      }
      else
      {
         ValueBehaviorApplier.applyFieldBehaviors(ValueBehaviorApplier.Action.UPDATE, QContext.getQInstance(), table, updateInput.getRecords(), behaviorsToOmit);
      }
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
         if(updateInput.getInputSource() != QInputSource.SYSTEM)
         {
            ValidateContingentPossibleValuesHelper.validate(table, updateInput.getRecords(), oldRecordList.orElse(List.of()), updateInput.getTransaction());
         }
      }

      ///////////////////////////////////////////////////////////////////////////
      // after all validations, run the pre-update customizer, if there is one //
      ///////////////////////////////////////////////////////////////////////////
      if(!skipsCustomizers(updateInput))
      {
         runPreUpdateCustomizers(updateInput, table, oldRecordList, isPreview);
      }
      UniqueKeyHelper.validateUpdateUniqueKeys(updateInput);
      RecordReferenceSecurityHelper.validateWrites(table, updateInput.getRecords(), RecordChangeType.UPDATE, updateInput.getTransaction());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Optional<List<QRecord>> fetchOldRecords(UpdateInput updateInput, UpdateInterface updateInterface) throws QException
   {
      if(updateInterface.supportsPreFetchQuery())
      {
         return (Optional.of(queryOldRecords(updateInput)));
      }

      return (Optional.empty());
   }



   /*******************************************************************************
    ** Query the stored versions of the records being updated.
    *******************************************************************************/
   private static List<QRecord> queryOldRecords(UpdateInput updateInput) throws QException
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

      return (queryOutput.getRecords());
   }



   /*******************************************************************************
    ** Old records for record change listeners.  Reuses the pre-fetch when the
    ** backend ran one; otherwise queries only if a listener applies to this
    ** update.  That query is only for listeners, so a failure is logged (the
    ** listeners get null old records) rather than failing the update.
    *******************************************************************************/
   private static List<QRecord> fetchOldRecordsForListeners(UpdateInput updateInput, Optional<List<QRecord>> oldRecordList)
   {
      if(oldRecordList.isPresent())
      {
         return (oldRecordList.get());
      }

      if(CollectionUtils.nullSafeIsEmpty(updateInput.getRecords()) || !RecordChangeListenerHelper.anyApply(QContext.getQInstance(), updateInput.getTableName(), RecordChangeType.UPDATE))
      {
         return (null);
      }

      try
      {
         return (queryOldRecords(updateInput));
      }
      catch(Exception e)
      {
         LOG.warn("Error fetching old records for record change listeners", e, logPair("tableName", updateInput.getTableName()));
         return (null);
      }
   }



   /*******************************************************************************
    ** Tell record change listeners about the records updated without errors, each
    ** paired (by primary key) with its old record.  Runs before post-update
    ** customizers, which may change the records the action returns.
    **
    ** A record whose primary key can't be read is left out of the event (and an
    ** old record whose key can't be read is not paired), with a warning - rather
    ** than dropping the event for every other record.
    *******************************************************************************/
   private static void fireRecordChangeListeners(UpdateInput updateInput, QTableMetaData table, List<QRecord> updatedRecords, List<QRecord> oldRecords)
   {
      QInstance qInstance = QContext.getQInstance();
      if(!RecordChangeListenerHelper.anyApply(qInstance, updateInput.getTableName(), RecordChangeType.UPDATE))
      {
         return;
      }

      try
      {
         Map<Object, QRecord> oldRecordsByPrimaryKey = new HashMap<>();
         for(QRecord oldRecord : CollectionUtils.nonNullList(oldRecords))
         {
            try
            {
               oldRecordsByPrimaryKey.put(AssociatedRecordUpdate.primaryKey(table, oldRecord), oldRecord);
            }
            catch(QException e)
            {
               LOG.warn("Could not read primary key of an old record for record change listeners; it will not be paired", e, logPair("tableName", updateInput.getTableName()));
            }
         }

         List<QRecord> successfulRecords = new ArrayList<>();
         List<QRecord> pairedOldRecords  = new ArrayList<>();
         for(QRecord record : updatedRecords)
         {
            if(record != null && CollectionUtils.nullSafeIsEmpty(record.getErrors()))
            {
               Object primaryKey;
               try
               {
                  primaryKey = AssociatedRecordUpdate.primaryKey(table, record);
               }
               catch(QException e)
               {
                  LOG.warn("Could not read primary key of an updated record; leaving it out of the record change event", e, logPair("tableName", updateInput.getTableName()));
                  continue;
               }

               successfulRecords.add(record);
               pairedOldRecords.add(oldRecordsByPrimaryKey.get(primaryKey));
            }
         }

         RestoreWriteFlag restoreWriteFlag = RestoreWriteFlag.from(updateInput.getFlags());
         RecordChangeListenerHelper.fire(qInstance, new RecordChangeEvent()
            .withTableName(updateInput.getTableName())
            .withType(RecordChangeType.UPDATE)
            .withRecords(Collections.unmodifiableList(successfulRecords))
            .withOldRecords(Collections.unmodifiableList(pairedOldRecords))
            .withTransaction(updateInput.getTransaction()), restoreWriteFlag != null && !restoreWriteFlag.getPublishEvents());
      }
      catch(Exception e)
      {
         if(e instanceof RecordChangeListenerHelper.RequiredListenerException required)
         {
            throw (required);
         }
         LOG.warn("Error preparing record change event", e, logPair("tableName", updateInput.getTableName()));
      }
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

      Map<Object, QRecord> authorizedOldRecords = ValidateRecordSecurityLockHelper.validateStoredWriteLocks(table, lookedUpRecords, ValidateRecordSecurityLockHelper.Action.UPDATE, updateInput.getTransaction());
      validateSecurityFieldsAsUpdated(table, records, authorizedOldRecords, updateInput.getTransaction());

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
    ** Validate the write locks against each record as it will be after the
    ** update: its stored values overlaid with the values being written. A
    ** partial update that omits a lock field - or the foreign key a joined
    ** lock walks through, e.g. editing only a line item's sku - keeps the
    ** stored value, so it is validated with that value instead of as if the
    ** field were blank (which denied every such update with "the referenced
    ** ... was not found"). Fields the update does set, including nulls, are
    ** validated as written. Only stored records that passed the stored write
    ** locks are overlaid, so an error can only name a value the caller sent.
    ** Errors land on the input records.
    *******************************************************************************/
   private static void validateSecurityFieldsAsUpdated(QTableMetaData table, List<QRecord> records, Map<Object, QRecord> storedRecords, QBackendTransaction transaction) throws QException
   {
      if(storedRecords == null)
      {
         ValidateRecordSecurityLockHelper.validateSecurityFields(table, records, ValidateRecordSecurityLockHelper.Action.UPDATE, transaction);
         return;
      }

      List<QRecord>         toValidate       = new ArrayList<>();
      Map<QRecord, QRecord> inputByEffective = new IdentityHashMap<>();
      for(QRecord record : records)
      {
         ///////////////////////////////////////////////////////////////////////
         // a record whose stored values already failed the write locks is   //
         // denied generically later; never validate (and so never echo) its //
         // stored lock values here                                          //
         ///////////////////////////////////////////////////////////////////////
         QRecord stored = storedRecords.get(AssociatedRecordUpdate.primaryKey(table, record));
         if(stored == null || CollectionUtils.nullSafeHasContents(stored.getErrors()))
         {
            toValidate.add(record);
            continue;
         }

         QRecord effective = new QRecord(stored);
         effective.setErrors(new ArrayList<>());
         effective.setWarnings(new ArrayList<>());
         for(Map.Entry<String, Serializable> entry : record.getValues().entrySet())
         {
            effective.setValue(entry.getKey(), entry.getValue());
         }
         toValidate.add(effective);
         inputByEffective.put(effective, record);
      }

      ValidateRecordSecurityLockHelper.validateSecurityFields(table, toValidate, ValidateRecordSecurityLockHelper.Action.UPDATE, transaction);
      for(Map.Entry<QRecord, QRecord> entry : inputByEffective.entrySet())
      {
         for(QErrorMessage error : CollectionUtils.nonNullList(entry.getKey().getErrors()))
         {
            entry.getValue().addError(error);
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
