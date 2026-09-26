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


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.AbstractQActionFunction;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.actions.audits.DMLAuditAction;
import com.kingsrook.qqq.backend.core.actions.automation.AutomationStatus;
import com.kingsrook.qqq.backend.core.actions.automation.RecordAutomationStatusUpdater;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPreInsertCustomizer;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.interfaces.InsertInterface;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.AssociationJoin;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.QueryStatManager;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.UniqueKeyHelper;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.ValidateRecordSecurityLockHelper;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeEvent;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeListenerHelper;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeType;
import com.kingsrook.qqq.backend.core.actions.values.ValueBehaviorApplier;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.audits.DMLAuditInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.querystats.QueryStat;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.QErrorMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.QWarningMessage;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleInterface;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Action to insert one or more records.
 **
 *******************************************************************************/
public class InsertAction extends AbstractQActionFunction<InsertInput, InsertOutput>
{
   private static final QLogger LOG = QLogger.getLogger(InsertAction.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   public QRecord executeForRecord(InsertInput insertInput) throws QException
   {
      InsertOutput insertOutput = new InsertAction().execute(insertInput);
      return (insertOutput.getRecords().get(0));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static List<QRecord> executeForRecords(InsertInput insertInput) throws QException
   {
      InsertOutput insertOutput = new InsertAction().execute(insertInput);
      return (insertOutput.getRecords());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public InsertOutput execute(InsertInput insertInput) throws QException
   {
      return execute(insertInput, false);
   }



   /*******************************************************************************
    ** Replacement trees stop failed writes before presentation callbacks can hide them.
    *******************************************************************************/
   InsertOutput execute(InsertInput input, boolean failOnRecordErrors) throws QException
   {
      return execute(input, null, null, failOnRecordErrors);
   }



   /*******************************************************************************
    ** Replace needs native identities before POST presentation changes. Original
    ** copy identity is also required when generated keys must reach caller records.
    *******************************************************************************/
   ReplaceResult executeForReplace(InsertInput input, boolean correlateOriginals) throws QException
   {
      Set<Object> originals = null;
      if(correlateOriginals)
      {
         originals = new HashSet<>();
         for(QRecord record : input.getRecords())
         {
            originals.add(record.trackCopies());
         }
      }
      List<QRecord> nativeRecords = new ArrayList<>();
      return new ReplaceResult(execute(input, nativeRecords, originals, true), nativeRecords);
   }



   /*******************************************************************************
    ** Ordinary Insert does not enable Replace's private identity bookkeeping.
    *******************************************************************************/
   private InsertOutput execute(InsertInput insertInput, List<QRecord> nativeRecords, Set<Object> originalIdentities, boolean failOnRecordErrors) throws QException
   {
      ActionHelper.validateSession(insertInput);

      if(!StringUtils.hasContent(insertInput.getTableName()))
      {
         throw (new QException("Table name was not specified in insert input"));
      }

      QTableMetaData table = insertInput.getTable();
      if(table == null)
      {
         throw (new QException("Error:  Undefined table: " + insertInput.getTableName()));
      }
      table = TableMetaDataPersonalizerAction.execute(insertInput);
      insertInput.setTableMetaData(table);

      setAutomationStatusField(insertInput);

      /////////////////////////////
      // run standard validators //
      /////////////////////////////
      performValidations(insertInput, false, false);

      Set<Object> expectedNativeOrigins = null;
      if(nativeRecords != null)
      {
         if(insertInput.getRecords() == null)
         {
            throw new QException("Replace INSERT PRE omitted its record list");
         }
         Set<Object> seen = new HashSet<>();
         for(QRecord record : CollectionUtils.nonNullList(insertInput.getRecords()))
         {
            if(record == null || !seen.add(record.trackCopies())
               || (originalIdentities != null && !originalIdentities.contains(record.trackCopies())))
            {
               throw new QException("Replace INSERT cannot correlate a PRE record with its original input");
            }
         }
         if(originalIdentities != null && !seen.equals(originalIdentities))
         {
            throw new QException("Replace INSERT omitted an original input record");
         }
         expectedNativeOrigins = seen;
      }

      //////////////////////////////////////////////////////
      // use the backend module to actually do the insert //
      //////////////////////////////////////////////////////
      QueryStat    queryStat    = QueryStatManager.newQueryStat(insertInput.getBackend(), table, null, InsertAction.class.getSimpleName());
      InsertOutput insertOutput = runInsertInBackend(insertInput);

      if(queryStat != null)
      {
         queryStat.setRecordCount(insertInput.getRecords() == null ? 0 : insertInput.getRecords().size());
         QueryStatManager.getInstance().add(queryStat);
      }

      if(insertOutput.getRecords() == null)
      {
         ////////////////////////////////////////////////////////////////////////////////////
         // in case the module failed to set record in the output, put an empty list there //
         // to avoid so many downstream NPE's                                              //
         ////////////////////////////////////////////////////////////////////////////////////
         insertOutput.setRecords(new ArrayList<>());
      }

      if(failOnRecordErrors)
      {
         ReplaceAction.requireSuccessfulRecords(insertOutput.getRecords(), "INSERT");
      }
      if(expectedNativeOrigins != null)
      {
         Set<Object> remaining = new HashSet<>(expectedNativeOrigins);
         for(QRecord record : insertOutput.getRecords())
         {
            if(record == null || !remaining.remove(record.trackCopies()))
            {
               throw new QException("Replace INSERT returned an unexpected native record origin");
            }
         }
         if(!remaining.isEmpty())
         {
            throw new QException("Replace INSERT omitted a native record origin");
         }
      }

      //////////////////////////////
      // log if there were errors //
      //////////////////////////////
      List<String> errors = insertOutput.getRecords().stream().flatMap(r -> r.getErrors().stream().map(Object::toString)).toList();
      if(CollectionUtils.nullSafeHasContents(errors))
      {
         LOG.info("Errors in insertAction", logPair("tableName", table.getName()), logPair("errorCount", errors.size()), errors.size() < 10 ? logPair("errors", errors) : logPair("first10Errors", errors.subList(0, 10)));
      }

      //////////////////////////////////////////////////
      // insert any associations in the input records //
      //////////////////////////////////////////////////
      manageAssociations(table, insertOutput.getRecords(), insertInput, failOnRecordErrors);

      //////////////////
      // do the audit //
      //////////////////
      if(insertInput.getOmitDmlAudit())
      {
         LOG.debug("Requested to omit DML audit");
      }
      else
      {
         new DMLAuditAction().execute(new DMLAuditInput()
            .withTableActionInput(insertInput)
            .withTransaction(insertInput.getTransaction())
            .withAuditContext(insertInput.getAuditContext())
            .withRecordList(insertOutput.getRecords()));
      }

      fireRecordChangeListeners(insertInput, insertOutput.getRecords());

      ////////////////////////////////////////////////////////////////
      // finally, run the post-insert customizers, if there are any //
      ////////////////////////////////////////////////////////////////
      if(failOnRecordErrors)
      {
         ReplaceAction.requireSuccessfulRecords(insertOutput.getRecords(), "INSERT");
      }
      if(nativeRecords != null)
      {
         for(QRecord record : insertOutput.getRecords())
         {
            record.capturePrimaryKey(table, record.getValue(table.getPrimaryKeyField()));
            nativeRecords.add(new QRecord(record));
         }
      }
      runPostInsertCustomizers(insertInput, table, insertOutput);
      if(failOnRecordErrors)
      {
         ReplaceAction.requireSuccessfulRecords(insertOutput.getRecords(), "INSERT");
      }

      return insertOutput;
   }



   /*******************************************************************************
    ** Package-private native snapshots never become ordinary Insert output fields.
    *******************************************************************************/
   record ReplaceResult(InsertOutput output, List<QRecord> nativeRecords)
   {
   }



   /*******************************************************************************
    ** Tell record change listeners about the records inserted without errors.
    ** Runs before post-insert customizers, which may change the records the
    ** action returns, so listeners see the values that were stored.
    *******************************************************************************/
   private static void fireRecordChangeListeners(InsertInput insertInput, List<QRecord> insertedRecords)
   {
      QInstance qInstance = QContext.getQInstance();
      if(!RecordChangeListenerHelper.anyApply(qInstance, insertInput.getTableName(), RecordChangeType.INSERT))
      {
         return;
      }

      List<QRecord> successfulRecords = insertedRecords.stream()
         .filter(r -> r != null && CollectionUtils.nullSafeIsEmpty(r.getErrors()))
         .toList();

      RecordChangeListenerHelper.fire(qInstance, new RecordChangeEvent()
         .withTableName(insertInput.getTableName())
         .withType(RecordChangeType.INSERT)
         .withRecords(successfulRecords)
         .withTransaction(insertInput.getTransaction()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static void runPostInsertCustomizers(InsertInput insertInput, QTableMetaData table, InsertOutput insertOutput)
   {
      Optional<TableCustomizerInterface> postInsertCustomizer = QCodeLoader.getTableCustomizer(table, TableCustomizers.POST_INSERT_RECORD.getRole());
      if(postInsertCustomizer.isPresent())
      {
         try
         {
            insertOutput.setRecords(postInsertCustomizer.get().postInsert(insertInput, insertOutput.getRecords()));
         }
         catch(Exception e)
         {
            for(QRecord record : insertOutput.getRecords())
            {
               record.addWarning(new QWarningMessage("An error occurred after the insert: " + e.getMessage()));
            }
         }
      }

      ///////////////////////////////////////////////
      // run all of the instance-level customizers //
      ///////////////////////////////////////////////
      List<QCodeReference> tableCustomizerCodes = QContext.getQInstance().getTableCustomizers(TableCustomizers.POST_INSERT_RECORD);
      for(QCodeReference tableCustomizerCode : tableCustomizerCodes)
      {
         try
         {
            TableCustomizerInterface tableCustomizer = QCodeLoader.getAdHoc(TableCustomizerInterface.class, tableCustomizerCode);
            insertOutput.setRecords(tableCustomizer.postInsert(insertInput, insertOutput.getRecords()));
         }
         catch(Exception e)
         {
            for(QRecord record : insertOutput.getRecords())
            {
               record.addWarning(new QWarningMessage("An error occurred after the insert: " + e.getMessage()));
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private InsertOutput runInsertInBackend(InsertInput insertInput) throws QException
   {
      ///////////////////////////////////
      // exit early if 0 input records //
      ///////////////////////////////////
      if(CollectionUtils.nullSafeIsEmpty(insertInput.getRecords()))
      {
         LOG.debug("Insert request called with 0 records.  Returning with no-op", logPair("tableName", insertInput.getTableName()));
         InsertOutput rs = new InsertOutput();
         rs.setRecords(new ArrayList<>());
         return (rs);
      }

      //////////////////////////////////////////////////////
      // load the backend module and its insert interface //
      //////////////////////////////////////////////////////
      QBackendModuleInterface qModule         = getBackendModuleInterface(insertInput.getBackend());
      InsertInterface         insertInterface = qModule.getInsertInterface();

      ////////////////////////////////////
      // have the backend do the insert //
      ////////////////////////////////////
      InsertOutput insertOutput = insertInterface.execute(insertInput);
      return insertOutput;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void performValidations(InsertInput insertInput, boolean isPreview, boolean didAlreadyRunCustomizer) throws QException
   {
      if(CollectionUtils.nullSafeIsEmpty(insertInput.getRecords()))
      {
         return;
      }

      QTableMetaData table = insertInput.getTable();

      ///////////////////////////////////////////////////////////////////
      // load the pre-insert customizer and set it up, if there is one //
      // then we'll run it based on its WhenToRun value                //
      // note - if we already ran it, then don't re-run it!            //
      ///////////////////////////////////////////////////////////////////
      Optional<TableCustomizerInterface> preInsertCustomizer = didAlreadyRunCustomizer ? Optional.empty() : QCodeLoader.getTableCustomizer(table, TableCustomizers.PRE_INSERT_RECORD.getRole());
      runPreInsertCustomizerIfItIsTime(insertInput, isPreview, preInsertCustomizer, AbstractPreInsertCustomizer.WhenToRun.BEFORE_ALL_VALIDATIONS);

      setDefaultValuesInRecords(table, insertInput.getRecords());

      ValueBehaviorApplier.applyFieldBehaviors(ValueBehaviorApplier.Action.INSERT, QContext.getQInstance(), table, insertInput.getRecords(), null);

      runPreInsertCustomizerIfItIsTime(insertInput, isPreview, preInsertCustomizer, AbstractPreInsertCustomizer.WhenToRun.BEFORE_UNIQUE_KEY_CHECKS);
      UniqueKeyHelper.validateInsertStoredKeys(insertInput);

      runPreInsertCustomizerIfItIsTime(insertInput, isPreview, preInsertCustomizer, AbstractPreInsertCustomizer.WhenToRun.BEFORE_REQUIRED_FIELD_CHECKS);
      if(insertInput.getInputSource().shouldValidateRequiredFields())
      {
         validateRequiredFields(insertInput);
      }

      runPreInsertCustomizerIfItIsTime(insertInput, isPreview, preInsertCustomizer, AbstractPreInsertCustomizer.WhenToRun.BEFORE_SECURITY_CHECKS);
      ValidateRecordSecurityLockHelper.validateSecurityFields(insertInput.getTable(), insertInput.getRecords(), ValidateRecordSecurityLockHelper.Action.INSERT, insertInput.getTransaction());

      UniqueKeyHelper.validateInsertBatchKeys(insertInput);
      runPreInsertCustomizerIfItIsTime(insertInput, isPreview, preInsertCustomizer, AbstractPreInsertCustomizer.WhenToRun.AFTER_ALL_VALIDATIONS);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void setDefaultValuesInRecords(QTableMetaData table, List<QRecord> records)
   {
      ////////////////////////////////////////////////////////////////////////////////////////////////
      // for all fields in the table - if any have a default value, then look at all input records, //
      // and if they have null value, then apply the default                                        //
      ////////////////////////////////////////////////////////////////////////////////////////////////
      for(QFieldMetaData field : table.getFields().values())
      {
         if(field.getDefaultValue() != null)
         {
            for(QRecord record : records)
            {
               if(record.getValue(field.getName()) == null)
               {
                  record.setValue(field.getName(), field.getDefaultValue());
               }
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void runPreInsertCustomizerIfItIsTime(InsertInput insertInput, boolean isPreview, Optional<TableCustomizerInterface> preInsertCustomizer, AbstractPreInsertCustomizer.WhenToRun whenToRun) throws QException
   {
      if(preInsertCustomizer.isPresent())
      {
         if(whenToRun.equals(preInsertCustomizer.get().whenToRunPreInsert(insertInput, isPreview)))
         {
            insertInput.setRecords(preInsertCustomizer.get().preInsert(insertInput, insertInput.getRecords(), isPreview));
         }
      }

      ///////////////////////////////////////////////
      // run all of the instance-level customizers //
      ///////////////////////////////////////////////
      List<QCodeReference> tableCustomizerCodes = QContext.getQInstance().getTableCustomizers(TableCustomizers.PRE_INSERT_RECORD);
      for(QCodeReference tableCustomizerCode : tableCustomizerCodes)
      {
         TableCustomizerInterface tableCustomizer = QCodeLoader.getAdHoc(TableCustomizerInterface.class, tableCustomizerCode);
         if(whenToRun.equals(tableCustomizer.whenToRunPreInsert(insertInput, isPreview)))
         {
            insertInput.setRecords(tableCustomizer.preInsert(insertInput, insertInput.getRecords(), isPreview));
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void validateRequiredFields(InsertInput insertInput)
   {
      QTableMetaData table = insertInput.getTable();
      Set<QFieldMetaData> requiredFields = table.getFields().values().stream()
         .filter(f -> f.getIsRequired())
         .collect(Collectors.toSet());

      if(!requiredFields.isEmpty())
      {
         for(QRecord record : insertInput.getRecords())
         {
            for(QFieldMetaData requiredField : requiredFields)
            {
               if(record.getValue(requiredField.getName()) == null || ((requiredField.getType().isStringLike() || record.getValue(requiredField.getName()) instanceof String) && record.getValueString(requiredField.getName()).trim().equals("")))
               {
                  record.addError(new BadInputStatusMessage("Missing value in required field: " + Objects.requireNonNullElse(requiredField.getLabel(), requiredField.getName())));
               }
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void manageAssociations(QTableMetaData table, List<QRecord> insertedRecords, InsertInput insertInput, boolean failOnRecordErrors) throws QException
   {
      for(Association association : CollectionUtils.nonNullList(table.getAssociations()))
      {
         AssociationJoin join = AssociationJoin.resolve(table, association);

         List<QRecord> nextLevelInserts = new ArrayList<>();
         for(QRecord record : insertedRecords)
         {
            if(CollectionUtils.nullSafeHasContents(record.getErrors()))
            {
               continue;
            }

            if(record.getAssociatedRecords() != null && record.getAssociatedRecords().containsKey(association.getName()))
            {
               for(QRecord associatedRecord : CollectionUtils.nonNullList(record.getAssociatedRecords().get(association.getName())))
               {
                  join.assignParentValues(record, associatedRecord);
                  nextLevelInserts.add(associatedRecord);
               }
            }
         }

         if(CollectionUtils.nullSafeHasContents(nextLevelInserts))
         {
            InsertInput nextLevelInsertInput = new InsertInput();
            nextLevelInsertInput.setInputSource(insertInput.getInputSource());
            nextLevelInsertInput.withFlags(insertInput.getFlags());
            nextLevelInsertInput.setTransaction(insertInput.getTransaction());
            nextLevelInsertInput.setTableName(association.getAssociatedTableName());
            nextLevelInsertInput.setRecords(nextLevelInserts);
            InsertOutput nextLevelInsertOutput = new InsertAction().execute(nextLevelInsertInput, failOnRecordErrors);

            ///////////////////////////////////////////////////////////////////////////////////////
            // copy inserted primary keys over from the output records to the input records also //
            // any errors or warnings that were generated and placed on the input records - make //
            // sure they are on the output records (in case they are new object instances)       //
            ///////////////////////////////////////////////////////////////////////////////////////
            List<QRecord> outputRecords = CollectionUtils.nonNullList(nextLevelInsertOutput.getRecords());
            copyGeneratedKeysWarningsAndErrorsToInsertedAssociatedRecords(outputRecords, nextLevelInserts, association.getAssociatedTableName());
         }
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   static void copyGeneratedKeysWarningsAndErrorsToInsertedAssociatedRecords(List<QRecord> outputRecords, List<QRecord> inputRecords, String tableName)
   {
      QTableMetaData associatedTable      = QContext.getQInstance().getTable(tableName);
      String         associatedPrimaryKey = associatedTable.getPrimaryKeyField();

      if(inputRecords.size() != outputRecords.size())
      {
         LOG.warn("Different size input and output record lists while managing association", logPair("tableName", tableName), logPair("inputRecords", inputRecords.size()), logPair("outputRecords", outputRecords.size()));
      }

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // we expect the lists to be the same size, but in case they're different, iterate only to the min length //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////
      for(int i = 0; i < Math.min(outputRecords.size(), inputRecords.size()); i++)
      {
         try
         {
            QRecord outputRecord = outputRecords.get(i);
            QRecord inputRecord  = inputRecords.get(i);

            /////////////////////////////////////////////////////////////////////////////////////
            // if the output & input records are the same object, then we can continue w/ noop //
            /////////////////////////////////////////////////////////////////////////////////////
            if(outputRecord == inputRecord)
            {
               continue;
            }

            ////////////////////////////////////////////////////////////////////////////////////////////////////////
            // if the input record didn't have a primary key value, but the output record does, then copy it over //
            ////////////////////////////////////////////////////////////////////////////////////////////////////////
            if(inputRecord.getValue(associatedPrimaryKey) == null && outputRecord.getValue(associatedPrimaryKey) != null)
            {
               inputRecord.setValue(associatedPrimaryKey, outputRecord.getValue(associatedPrimaryKey));
            }

            ////////////////////////////////////////////////////////////////////
            // if the output record has errors, copy them to the input record //
            ////////////////////////////////////////////////////////////////////
            if(CollectionUtils.nullSafeHasContents(outputRecord.getErrors()))
            {
               if(CollectionUtils.nullSafeIsEmpty(inputRecord.getErrors()))
               {
                  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  // if there weren't any errors in the input record, replace its list with a copy of the ones from the output record //
                  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  inputRecord.setErrors(new ArrayList<>(outputRecord.getErrors()));
               }
               else
               {
                  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  // else there are errors in the input record - so copy ones from the output record that weren't already in the input. //
                  // also, yes, this is potentially bad n^2 stuff, but, we're assuming very small lists, so we're okay with it.         //
                  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  for(QErrorMessage error : outputRecord.getErrors())
                  {
                     if(!inputRecord.getErrors().contains(error))
                     {
                        try
                        {
                           inputRecord.getErrors().add(error);
                        }
                        catch(Exception e)
                        {
                           //////////////////////////////////////////////////////
                           // looking for immutable list here (e.g., List.of)  //
                           // replace list with mutable one (array) and re-add //
                           //////////////////////////////////////////////////////
                           inputRecord.setErrors(new ArrayList<>(inputRecord.getErrors()));
                           inputRecord.getErrors().add(error);
                        }
                     }
                  }
               }
            }

            //////////////////////////////////////////////////////////////////////
            // if the output record has warnings, copy them to the input record //
            //////////////////////////////////////////////////////////////////////
            if(CollectionUtils.nullSafeHasContents(outputRecord.getWarnings()))
            {
               if(CollectionUtils.nullSafeIsEmpty(inputRecord.getWarnings()))
               {
                  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  // if there weren't any warnings in the input record, replace its list with a copy of the ones from the output record //
                  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  inputRecord.setWarnings(new ArrayList<>(outputRecord.getWarnings()));
               }
               else
               {
                  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  // else there are warnings in the input record - so copy ones from the output record that weren't already in the input. //
                  // also, yes, this is potentially bad n^2 stuff, but, we're assuming very small lists, so we're okay with it.           //
                  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  for(QWarningMessage warning : outputRecord.getWarnings())
                  {
                     if(!inputRecord.getWarnings().contains(warning))
                     {
                        try
                        {
                           inputRecord.getWarnings().add(warning);
                        }
                        catch(Exception e)
                        {
                           /////////////////////////////////////////////////////
                           // looking for immutable list here (e.g., List.of) //
                           // replace list with mutable one (array) and re-add //
                           /////////////////////////////////////////////////////
                           inputRecord.setWarnings(new ArrayList<>(inputRecord.getWarnings()));
                           inputRecord.getWarnings().add(warning);
                        }
                     }
                  }
               }
            }
         }
         catch(Exception e)
         {
            LOG.warn("Exception copying values to output associated record", e, logPair("tableName", tableName));
         }
      }
   }



   /*******************************************************************************
    ** If the table being inserted into uses an automation-status field, populate it now.
    *******************************************************************************/
   private void setAutomationStatusField(InsertInput insertInput)
   {
      RecordAutomationStatusUpdater.setAutomationStatusInRecords(insertInput.getTable(), insertInput.getRecords(), AutomationStatus.PENDING_INSERT_AUTOMATIONS, insertInput.getTransaction(), null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QBackendModuleInterface getBackendModuleInterface(QBackendMetaData backend) throws QException
   {
      QBackendModuleDispatcher qBackendModuleDispatcher = new QBackendModuleDispatcher();
      QBackendModuleInterface  qModule                  = qBackendModuleDispatcher.getQBackendModule(backend);
      return (qModule);
   }

}
