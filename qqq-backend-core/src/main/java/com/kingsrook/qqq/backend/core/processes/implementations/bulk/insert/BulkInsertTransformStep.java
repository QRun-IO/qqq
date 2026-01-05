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

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPreInsertCustomizer.WhenToRun;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.UniqueKeyHelper;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaDataInterface;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.model.statusmessages.QErrorMessage;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.mapping.AbstractBulkLoadRollableValueError;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.mapping.BulkLoadRecordUtils;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.mapping.BulkLoadTableStructureBuilder;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadTableStructure;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.LoadViaInsertStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.processes.implementations.general.ProcessSummaryWarningsAndErrorsRollup;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ListingHash;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import org.apache.commons.lang.BooleanUtils;
import org.json.JSONArray;
import org.json.JSONObject;


/*******************************************************************************
 * Transform step for generic table bulk-insert AND bulk-edit-via-file ETL process.
 *
 *******************************************************************************/
public class BulkInsertTransformStep extends AbstractTransformStep
{
   private static final QLogger LOG = QLogger.getLogger(BulkInsertTransformStep.class);

   public ProcessSummaryLine okSummary = new ProcessSummaryLine(Status.OK);

   public ProcessSummaryWarningsAndErrorsRollup processSummaryWarningsAndErrorsRollup = ProcessSummaryWarningsAndErrorsRollup.build("inserted")
      .withDoReplaceSingletonCountLinesWithSuffixOnly(false);

   private ListingHash<String, RowValue> errorToExampleRowValueMap = new ListingHash<>();
   private ListingHash<String, String>   errorToExampleRowsMap     = new ListingHash<>();

   private Map<UniqueKey, ProcessSummaryLineWithUKSampleValues> ukErrorSummaries              = new HashMap<>();
   private Map<String, ProcessSummaryLine>                      associationsToInsertSummaries = new HashMap<>();

   private QTableMetaData table;

   private Map<UniqueKey, Set<List<Serializable>>> keysInThisFile = new HashMap<>();

   private int rowsProcessed = 0;



   /*******************************************************************************
    ** extension of ProcessSummaryLine for lines where a UniqueKey was violated,
    ** where we'll collect a sample (or maybe all) of the values that broke the UK.
    *******************************************************************************/
   private static class ProcessSummaryLineWithUKSampleValues extends ProcessSummaryLine
   {
      private Set<String> sampleValues             = new LinkedHashSet<>();
      private boolean     areThereMoreSampleValues = false;



      /*******************************************************************************
       **
       *******************************************************************************/
      public ProcessSummaryLineWithUKSampleValues(Status status)
      {
         super(status);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void preRun(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      this.table = QContext.getQInstance().getTable(runBackendStepInput.getTableName());

      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // since we're doing a unique key check in this class, we can tell the loadViaInsert step that it (rather, the InsertAction) doesn't need to re-do one. //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      runBackendStepOutput.addValue(LoadViaInsertStep.FIELD_SKIP_UNIQUE_KEY_CHECK, true);

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // make sure that if a saved profile was selected on a review screen, that the result screen knows about it. //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
      BulkInsertStepUtils.handleSavedBulkLoadProfileIdValue(runBackendStepInput, runBackendStepOutput);

      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // set up the validationReview widget to render preview records using the table layout, and including the associations //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      runBackendStepOutput.addValue("formatPreviewRecordUsingTableLayout", table.getName());

      BulkLoadTableStructure tableStructure = BulkLoadTableStructureBuilder.buildTableStructure(table.getName());
      if(CollectionUtils.nullSafeHasContents(tableStructure.getAssociations()))
      {
         ArrayList<String> previewRecordAssociatedTableNames  = new ArrayList<>();
         ArrayList<String> previewRecordAssociatedWidgetNames = new ArrayList<>();
         ArrayList<String> previewRecordAssociationNames      = new ArrayList<>();

         ////////////////////////////////////////////////////////////
         // note - not recursively processing associations here... //
         ////////////////////////////////////////////////////////////
         for(BulkLoadTableStructure associatedStructure : tableStructure.getAssociations())
         {
            String                associationName = associatedStructure.getAssociationPath();
            Optional<Association> association     = table.getAssociations().stream().filter(a -> a.getName().equals(associationName)).findFirst();
            if(association.isPresent())
            {
               for(QFieldSection section : table.getSections())
               {
                  QWidgetMetaDataInterface widget = QContext.getQInstance().getWidget(section.getWidgetName());
                  if(widget != null && WidgetType.CHILD_RECORD_LIST.getType().equals(widget.getType()))
                  {
                     Serializable widgetJoinName = widget.getDefaultValues().get("joinName");
                     if(Objects.equals(widgetJoinName, association.get().getJoinName()))
                     {
                        previewRecordAssociatedTableNames.add(association.get().getAssociatedTableName());
                        previewRecordAssociatedWidgetNames.add(widget.getName());
                        previewRecordAssociationNames.add(association.get().getName());
                     }
                  }
               }
            }
         }
         runBackendStepOutput.addValue("previewRecordAssociatedTableNames", previewRecordAssociatedTableNames);
         runBackendStepOutput.addValue("previewRecordAssociatedWidgetNames", previewRecordAssociatedWidgetNames);
         runBackendStepOutput.addValue("previewRecordAssociationNames", previewRecordAssociationNames);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void runOnePage(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      QTableMetaData table   = QContext.getQInstance().getTable(runBackendStepInput.getTableName());
      List<QRecord>  records = runBackendStepInput.getRecords();

      if(BooleanUtils.isTrue(runBackendStepInput.getValueBoolean("isBulkEdit")))
      {
         handleBulkEdit(runBackendStepInput, runBackendStepOutput, records, table);
         runBackendStepOutput.addValue("isBulkEdit", true);
      }
      else
      {
         handleBulkLoad(runBackendStepInput, runBackendStepOutput, records, table);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void handleBulkEdit(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput, List<QRecord> records, QTableMetaData table) throws QException
   {
      updateStatusCount(runBackendStepInput, "Updating", table);

      ///////////////////////////////////////////
      // get the key fields for this bulk edit //
      ///////////////////////////////////////////
      String       keyFieldsString = runBackendStepInput.getValueString("keyFields");
      List<String> keyFields       = Arrays.asList(keyFieldsString.split("\\|"));

      //////////////////////////////////////////////////////////////////////////
      // if the key field is the primary key, then just look up those records //
      //////////////////////////////////////////////////////////////////////////
      List<QRecord> nonMatchingRecords = new ArrayList<>();
      List<QRecord> oldRecords         = new ArrayList<>();
      List<QRecord> recordsToUpdate    = new ArrayList<>();
      if(keyFields.size() == 1 && table.getPrimaryKeyField().equals(keyFields.get(0)))
      {
         recordsToUpdate = records;
         String             primaryKeyName = table.getPrimaryKeyField();
         List<Serializable> primaryKeys    = records.stream().map(record -> record.getValue(primaryKeyName)).toList();
         oldRecords = new QueryAction().execute(new QueryInput(table.getName()).withFilter(new QQueryFilter(new QFilterCriteria(table.getPrimaryKeyField(), QCriteriaOperator.IN, primaryKeys)))).getRecords();

         ///////////////////////////////////////////
         // get a set of old records primary keys //
         ///////////////////////////////////////////
         Set<Serializable> matchedPrimaryKeys = oldRecords.stream()
            .map(r -> r.getValue(table.getPrimaryKeyField()))
            .collect(java.util.stream.Collectors.toSet());

         ////////////////////////////////////////////////////////////////////////////////////////////////////
         // iterate over file records and if primary keys dont match, add to the non matching records list //
         ////////////////////////////////////////////////////////////////////////////////////////////////////
         for(QRecord record : records)
         {
            Serializable recordKey = record.getValue(table.getPrimaryKeyField());
            if(!matchedPrimaryKeys.contains(recordKey))
            {
               nonMatchingRecords.add(record);
            }
         }
      }
      else
      {
         List<QRecord> potentialRecords               = loadPotentialRecordsByUniqueKey(runBackendStepInput, records, table, keyFields);
         Integer       previousPotentialRecordsLoaded = Objects.requireNonNullElse(runBackendStepOutput.getValueInteger("potentialRecordsLoaded"), 0);
         runBackendStepOutput.addValue("potentialRecordsLoaded", previousPotentialRecordsLoaded + potentialRecords.size());

         ///////////////////////////////////////////////////////////////////////////////
         // now iterate over all of the potential records checking each unique fields //
         ///////////////////////////////////////////////////////////////////////////////
         fileRecordLoop:
         for(QRecord fileRecord : records)
         {
            for(QRecord databaseRecord : potentialRecords)
            {
               boolean allMatch = true;

               for(String uniqueKeyPart : keyFields)
               {
                  if(!Objects.equals(fileRecord.getValue(uniqueKeyPart), databaseRecord.getValue(uniqueKeyPart)))
                  {
                     allMatch = false;
                  }
               }

               //////////////////////////////////////////////////////////////////////////////////////
               // if we get here with all matching, update the record from the file's primary key, //
               // add it to the list to update, and continue looping over file records             //
               //////////////////////////////////////////////////////////////////////////////////////
               if(allMatch)
               {
                  oldRecords.add(databaseRecord);
                  fileRecord.setValue(table.getPrimaryKeyField(), databaseRecord.getValue(table.getPrimaryKeyField()));

                  //////////////////////////////////////////////////////////////////////////////////////////////////////////
                  // iterate over the fields in the bulk load profile, if the value for that field is empty and the value //
                  // of 'clear if empty' is set to true, then update the record to update with the old record's value     //
                  //////////////////////////////////////////////////////////////////////////////////////////////////////////
                  JSONArray array = new JSONArray(runBackendStepInput.getValueString("fieldListJSON"));
                  for(int i = 0; i < array.length(); i++)
                  {
                     JSONObject jsonObject   = array.getJSONObject(i);
                     String     fieldName    = jsonObject.optString("fieldName");
                     boolean    clearIfEmpty = jsonObject.optBoolean("clearIfEmpty");

                     if(fileRecord.getValue(fieldName) == null)
                     {
                        if(clearIfEmpty)
                        {
                           fileRecord.setValue(fieldName, null);
                        }
                        else
                        {
                           fileRecord.setValue(fieldName, databaseRecord.getValue(fieldName));
                        }
                     }
                  }

                  recordsToUpdate.add(fileRecord);
                  continue fileRecordLoop;
               }
            }

            ///////////////////////////////////////////////////////////////////////////////////////
            // if we make it here, that means the record was not found, keep for logging warning //
            ///////////////////////////////////////////////////////////////////////////////////////
            nonMatchingRecords.add(fileRecord);
         }
      }

      for(QRecord missingRecord : CollectionUtils.nonNullList(nonMatchingRecords))
      {
         String message = "Did not have a matching existing record.";
         processSummaryWarningsAndErrorsRollup.addError(message, null);
         addToErrorToExampleRowMap(message, missingRecord);
      }

      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // set up an insert-input, which will be used as input to the pre-customizer as well as for additional validations //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      UpdateInput updateInput = new UpdateInput(table.getName());
      updateInput.setInputSource(QInputSource.USER);
      updateInput.setRecords(recordsToUpdate);

      //////////////////////////////////////////////////////////////////////
      // load the pre-insert customizer and set it up, if there is one    //
      // then we'll run it based on its WhenToRun value                   //
      // we do this, in case it needs to, for example, adjust values that //
      // are part of a unique key                                         //
      //////////////////////////////////////////////////////////////////////
      boolean                            didAlreadyRunCustomizer = false;
      Optional<TableCustomizerInterface> preUpdateCustomizer     = QCodeLoader.getTableCustomizer(table, TableCustomizers.PRE_UPDATE_RECORD.getRole());
      if(preUpdateCustomizer.isPresent())
      {
         List<QRecord> recordsAfterCustomizer = preUpdateCustomizer.get().preUpdate(updateInput, records, true, Optional.of(oldRecords));
         runBackendStepInput.setRecords(recordsAfterCustomizer);

         /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // so we used to have a comment here asking "do we care if the customizer runs both now, and in the validation below?" //
         // when implementing Bulk Load V2, we were seeing that some customizers were adding errors to records, both now, and   //
         // when they ran below.  so, at that time, we added this boolean, to track and avoid the double-run...                 //
         // we could also imagine this being a setting on the pre-insert customizer, similar to its whenToRun attribute...      //
         /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         didAlreadyRunCustomizer = true;
      }

      /////////////////////////////////////////////////////////////////////////////////
      // run all validation from the insert action - in Preview mode (boolean param) //
      /////////////////////////////////////////////////////////////////////////////////
      updateInput.setRecords(recordsToUpdate);
      UpdateAction updateAction = new UpdateAction();
      updateAction.performValidations(updateInput, Optional.of(recordsToUpdate), didAlreadyRunCustomizer);
      List<QRecord> validationResultRecords = updateInput.getRecords();

      /////////////////////////////////////////////////////////////////
      // look at validation results to build process summary results //
      /////////////////////////////////////////////////////////////////
      List<QRecord> outputRecords = new ArrayList<>();
      for(QRecord record : validationResultRecords)
      {
         List<QErrorMessage> errorsFromAssociations = getErrorsFromAssociations(record);
         if(CollectionUtils.nullSafeHasContents(errorsFromAssociations))
         {
            List<QErrorMessage> recordErrors = Objects.requireNonNullElseGet(record.getErrors(), () -> new ArrayList<>());
            recordErrors.addAll(errorsFromAssociations);
            record.setErrors(recordErrors);
         }

         if(CollectionUtils.nullSafeHasContents(record.getErrors()))
         {
            for(QErrorMessage error : record.getErrors())
            {
               if(error instanceof AbstractBulkLoadRollableValueError rollableValueError)
               {
                  processSummaryWarningsAndErrorsRollup.addError(rollableValueError.getMessageToUseAsProcessSummaryRollupKey(), null);
                  addToErrorToExampleRowValueMap(rollableValueError, record);
               }
               else
               {
                  processSummaryWarningsAndErrorsRollup.addError(error.getMessage(), null);
                  addToErrorToExampleRowMap(error.getMessage(), record);
               }
            }
         }
         else if(CollectionUtils.nullSafeHasContents(record.getWarnings()))
         {
            String message = record.getWarnings().get(0).getMessage();
            processSummaryWarningsAndErrorsRollup.addWarning(message, null);
            outputRecords.add(record);
         }
         else
         {
            okSummary.incrementCountAndAddPrimaryKey(null);
            outputRecords.add(record);

            for(Map.Entry<String, List<QRecord>> entry : CollectionUtils.nonNullMap(record.getAssociatedRecords()).entrySet())
            {
               String             associationName         = entry.getKey();
               ProcessSummaryLine associationToInsertLine = associationsToInsertSummaries.computeIfAbsent(associationName, x -> new ProcessSummaryLine(Status.OK));
               associationToInsertLine.incrementCount(CollectionUtils.nonNullList(entry.getValue()).size());
            }
         }
      }

      runBackendStepOutput.setRecords(outputRecords);
      this.rowsProcessed += records.size();
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private void updateStatusCount(RunBackendStepInput runBackendStepInput, String insertingOrUpdating, QTableMetaData table)
   {
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // on the validate step, we haven't read the full file, so we don't know how many rows there are - thus        //
      // record count is null, and the ValidateStep won't be setting status counters - so - do it here in that case. //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      if(runBackendStepInput.getStepName().equals(StreamedETLWithFrontendProcess.STEP_NAME_VALIDATE))
      {
         runBackendStepInput.getAsyncJobCallback().updateStatus("Processing row " + "%,d".formatted(rowsProcessed + 1));
      }
      else if(runBackendStepInput.getStepName().equals(StreamedETLWithFrontendProcess.STEP_NAME_EXECUTE))
      {
         if(runBackendStepInput.getValue(StreamedETLWithFrontendProcess.FIELD_RECORD_COUNT) == null)
         {
            runBackendStepInput.getAsyncJobCallback().updateStatus(insertingOrUpdating + " " + table.getLabel() + " record " + "%,d".formatted(okSummary.getCount()));
         }
         else
         {
            runBackendStepInput.getAsyncJobCallback().updateStatus(insertingOrUpdating + " " + table.getLabel() + " records");
         }
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   protected List<QRecord> loadPotentialRecordsByUniqueKey(RunBackendStepInput runBackendStepInput, List<QRecord> fileRecords, QTableMetaData table, List<String> keyFields) throws QException
   {
      /////////////////////////////////////////////////////////////////////////
      // try a few different ways to look up the records to edit by unique   //
      // keys - trying to balance minimizing the number of queries with      //
      // avoiding fetching way too many records, exhausting server resources //
      /////////////////////////////////////////////////////////////////////////
      if(!runBackendStepInput.getValuePrimitiveBoolean("ByPerFieldInListsQueryFailed"))
      {
         try
         {
            //////////////////////////////////////////////////////////////////////////////////////////////////
            // first we try the original technique. but, for larger tables with fields that have lots of    //
            // records that might match partial keys, this approach has been seen to load too many records. //
            // so, we put a limit on the query, and if too many are found, we throw and switch to plan B.   //
            //////////////////////////////////////////////////////////////////////////////////////////////////
            return loadPotentialRecordsByUniqueKeyByPerFieldInLists(fileRecords, table, keyFields);
         }
         catch(TryAnotherWayException tawe)
         {
            ///////////////////////////////////////////////////////////////////////////////////
            // add this process value, to signal that we shouldn't try this technique again. //
            ///////////////////////////////////////////////////////////////////////////////////
            runBackendStepInput.addValue("ByPerFieldInListsQueryFailed", true);
            LOG.info("Caught a TryAnotherWayException", tawe);
         }
      }

      if(!runBackendStepInput.getValuePrimitiveBoolean("ByOrQueriesFailed"))
      {
         try
         {
            ///////////////////////////////////////////////////////////////////
            // the second technique hasn't been seen to load too many        //
            // records, but does also use the limit technique, same as above //
            ///////////////////////////////////////////////////////////////////
            return loadPotentialRecordsByUniqueKeyByOrQueries(fileRecords, table, keyFields);
         }
         catch(TryAnotherWayException tawe)
         {
            LOG.info("Caught a TryAnotherWayException", tawe);
            runBackendStepInput.addValue("ByOrQueriesFailed", true);
         }
      }

      ///////////////////////////////////////////////////////////////////////
      // as a last resort, query row-by-row.  hopefully we never hit here? //
      ///////////////////////////////////////////////////////////////////////
      return loadPotentialRecordsByUniqueKeyRecordByRecord(fileRecords, table, keyFields);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   protected static class TryAnotherWayException extends Exception
   {
      /***************************************************************************
       *
       ***************************************************************************/
      TryAnotherWayException(String message)
      {
         super(message);
      }
   }



   /***************************************************************************
    * The approach in this method is designed to minimize the number of queries
    * needed to look up the records to update.  To do so, we will run one query
    * per field in the unique key, selecting all rows with values IN the set of
    * values in the file records, and adding all matching records to our list of
    * potential-match records from the backend.
    *
    * This is known to potentially over-shoot, e.g., selecting more than we need.
    * Thus the caller needs to check values in each record for true matches.
    ***************************************************************************/
   protected List<QRecord> loadPotentialRecordsByUniqueKeyByPerFieldInLists(List<QRecord> fileRecords, QTableMetaData table, List<String> keyFields) throws QException, TryAnotherWayException
   {
      List<QRecord>     potentialRecords = new ArrayList<>();
      Set<Serializable> uniqueIds        = new HashSet<>();

      ////////////////////////////////////////////////////////////////////////////////////////////////////
      // if not using the primary key, then we will look up all records for each part of the unique key //
      // and for each found, if all unique parts match we will add to our list of database records      //
      ////////////////////////////////////////////////////////////////////////////////////////////////////
      for(String uniqueKeyPart : keyFields)
      {
         Set<Serializable> values = fileRecords.stream().map(record -> record.getValue(uniqueKeyPart)).collect(Collectors.toSet());

         int limit = fileRecords.size() * getMaxPotentialRecordsByUniqueKeyLimitFactor();
         QueryInput queryInput = new QueryInput(table.getName())
            .withFilter(new QQueryFilter(new QFilterCriteria(uniqueKeyPart, QCriteriaOperator.IN, values))
               .withLimit(limit));

         List<QRecord> selectedRecords = new QueryAction().execute(queryInput).getRecords();

         /////////////////////////////////////////////////////////////////////////////////////////////////////////
         // if we've hit the limit on our query size - be defensive and assume this query might actually return //
         // way too many records to keep in memory, and switch to the by-or-queries load technique              //
         /////////////////////////////////////////////////////////////////////////////////////////////////////////
         if(selectedRecords.size() == limit)
         {
            throw (new TryAnotherWayException("Query on " + uniqueKeyPart + " returns more than " + limit + " records."));
         }

         for(QRecord databaseRecord : selectedRecords)
         {
            if(!uniqueIds.contains(databaseRecord.getValue(table.getPrimaryKeyField())))
            {
               potentialRecords.add(databaseRecord);
               uniqueIds.add(databaseRecord.getValue(table.getPrimaryKeyField()));
            }
         }
      }

      return (potentialRecords);
   }



   /***************************************************************************
    * This approach builds a big query, in the format:
    * <pre>(c1=? AND c2=? ... cn=?) OR (c1=? AND c2=? ... cn=?) OR ... </pre>
    * which, can be a bit of a challenge for the backend - so - we do that on
    * pages of file records, rather than the full file, to try to strike a
    * balance between minimizing number of queries, vs queries that can cause
    * backend storage system issues.
    ***************************************************************************/
   protected List<QRecord> loadPotentialRecordsByUniqueKeyByOrQueries(List<QRecord> fileRecords, QTableMetaData table, List<String> keyFields) throws QException, TryAnotherWayException
   {
      List<QRecord> potentialRecords = new ArrayList<>();

      for(List<QRecord> page : CollectionUtils.getPages(fileRecords, getOrListQueryPageSize()))
      {
         int          limit  = fileRecords.size() * getMaxPotentialRecordsByUniqueKeyLimitFactor();
         QQueryFilter filter = new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR).withLimit(limit);
         for(QRecord record : page)
         {
            QQueryFilter subFilter = new QQueryFilter();
            filter.addSubFilter(subFilter);
            for(String fieldName : keyFields)
            {
               Serializable value = record.getValue(fieldName);
               if(value == null)
               {
                  subFilter.addCriteria(new QFilterCriteria(fieldName, QCriteriaOperator.IS_BLANK));
               }
               else
               {
                  subFilter.addCriteria(new QFilterCriteria(fieldName, QCriteriaOperator.EQUALS, value));
               }
            }
         }

         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(table.getName());
         queryInput.setFilter(filter);

         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         if(queryOutput.getRecords().size() == limit)
         {
            throw (new TryAnotherWayException("Query for a page using OR queries returns more than " + limit + " records."));
         }

         potentialRecords.addAll(queryOutput.getRecords());
      }

      return (potentialRecords);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   protected List<QRecord> loadPotentialRecordsByUniqueKeyRecordByRecord(List<QRecord> fileRecords, QTableMetaData table, List<String> keyFields) throws QException
   {
      List<QRecord> potentialRecords = new ArrayList<>();

      for(QRecord record : fileRecords)
      {
         QQueryFilter filter = new QQueryFilter().withLimit(2);
         for(String fieldName : keyFields)
         {
            filter.addCriteria(new QFilterCriteria(fieldName, QCriteriaOperator.EQUALS, record.getValue(fieldName)));
         }

         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(table.getName());
         queryInput.setFilter(filter);

         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         for(QRecord outputRecord : queryOutput.getRecords())
         {
            potentialRecords.add(outputRecord);
            /////////////////////////////////////////////////////////////////////////////////////////////////////////
            // todo - if we allowed mapping by non-unique keys, we'd need to handle having more than 1 match here. //
            /////////////////////////////////////////////////////////////////////////////////////////////////////////
            break;
         }
      }
      return (potentialRecords);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void handleBulkLoad(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput, List<QRecord> records, QTableMetaData table) throws QException
   {
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // set up an insert-input, which will be used as input to the pre-customizer as well as for additional validations //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      InsertInput insertInput = new InsertInput();
      insertInput.setInputSource(QInputSource.USER);
      insertInput.setTableName(runBackendStepInput.getTableName());
      insertInput.setRecords(records);
      insertInput.setSkipUniqueKeyCheck(true);

      //////////////////////////////////////////////////////////////////////
      // load the pre-insert customizer and set it up, if there is one    //
      // then we'll run it based on its WhenToRun value                   //
      // we do this, in case it needs to, for example, adjust values that //
      // are part of a unique key                                         //
      //////////////////////////////////////////////////////////////////////
      boolean                            didAlreadyRunCustomizer = false;
      Optional<TableCustomizerInterface> preInsertCustomizer     = QCodeLoader.getTableCustomizer(table, TableCustomizers.PRE_INSERT_RECORD.getRole());
      if(preInsertCustomizer.isPresent())
      {
         WhenToRun whenToRun = preInsertCustomizer.get().whenToRunPreInsert(insertInput, true);
         if(WhenToRun.BEFORE_ALL_VALIDATIONS.equals(whenToRun) || WhenToRun.BEFORE_UNIQUE_KEY_CHECKS.equals(whenToRun))
         {
            List<QRecord> recordsAfterCustomizer = preInsertCustomizer.get().preInsert(insertInput, records, true);
            runBackendStepInput.setRecords(recordsAfterCustomizer);

            /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // so we used to have a comment here asking "do we care if the customizer runs both now, and in the validation below?" //
            // when implementing Bulk Load V2, we were seeing that some customizers were adding errors to records, both now, and   //
            // when they ran below.  so, at that time, we added this boolean, to track and avoid the double-run...                 //
            // we could also imagine this being a setting on the pre-insert customizer, similar to its whenToRun attribute...      //
            /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            didAlreadyRunCustomizer = true;
         }
      }

      ///////////////////////////////////////////////////////////////////////////////
      // If the table has unique keys - then capture all values on these records   //
      // for each key and set up a processSummaryLine for each of the table's UK's //
      ///////////////////////////////////////////////////////////////////////////////
      Map<UniqueKey, Set<List<Serializable>>> existingKeys = new HashMap<>();
      List<UniqueKey>                         uniqueKeys   = CollectionUtils.nonNullList(table.getUniqueKeys());
      for(UniqueKey uniqueKey : uniqueKeys)
      {
         existingKeys.put(uniqueKey, UniqueKeyHelper.getExistingKeys(null, table, records, uniqueKey).keySet());
         ukErrorSummaries.computeIfAbsent(uniqueKey, x -> new ProcessSummaryLineWithUKSampleValues(Status.ERROR));
      }

      updateStatusCount(runBackendStepInput, "Inserting", table);

      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // Note, we want to do our own UK checking here, even though InsertAction also tries to do it, because InsertAction //
      // will only be getting the records in pages, but in here, we'll track UK's across pages!!                          //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      List<QRecord> recordsWithoutUkErrors = getRecordsWithoutUniqueKeyErrors(records, existingKeys, uniqueKeys, table);

      /////////////////////////////////////////////////////////////////////////////////
      // run all validation from the insert action - in Preview mode (boolean param) //
      /////////////////////////////////////////////////////////////////////////////////
      insertInput.setRecords(recordsWithoutUkErrors);
      InsertAction insertAction = new InsertAction();
      insertAction.performValidations(insertInput, true, didAlreadyRunCustomizer);
      List<QRecord> validationResultRecords = insertInput.getRecords();

      /////////////////////////////////////////////////////////////////
      // look at validation results to build process summary results //
      /////////////////////////////////////////////////////////////////
      List<QRecord> outputRecords = new ArrayList<>();
      for(QRecord record : validationResultRecords)
      {
         List<QErrorMessage> errorsFromAssociations = getErrorsFromAssociations(record);
         if(CollectionUtils.nullSafeHasContents(errorsFromAssociations))
         {
            List<QErrorMessage> recordErrors = Objects.requireNonNullElseGet(record.getErrors(), () -> new ArrayList<>());
            recordErrors.addAll(errorsFromAssociations);
            record.setErrors(recordErrors);
         }

         if(CollectionUtils.nullSafeHasContents(record.getErrors()))
         {
            for(QErrorMessage error : record.getErrors())
            {
               if(error instanceof AbstractBulkLoadRollableValueError rollableValueError)
               {
                  processSummaryWarningsAndErrorsRollup.addError(rollableValueError.getMessageToUseAsProcessSummaryRollupKey(), null);
                  addToErrorToExampleRowValueMap(rollableValueError, record);
               }
               else
               {
                  processSummaryWarningsAndErrorsRollup.addError(error.getMessage(), null);
                  addToErrorToExampleRowMap(error.getMessage(), record);
               }
            }
         }
         else if(CollectionUtils.nullSafeHasContents(record.getWarnings()))
         {
            String message = record.getWarnings().get(0).getMessage();
            processSummaryWarningsAndErrorsRollup.addWarning(message, null);
            outputRecords.add(record);
         }
         else
         {
            okSummary.incrementCountAndAddPrimaryKey(null);
            outputRecords.add(record);

            for(Map.Entry<String, List<QRecord>> entry : CollectionUtils.nonNullMap(record.getAssociatedRecords()).entrySet())
            {
               String             associationName         = entry.getKey();
               ProcessSummaryLine associationToInsertLine = associationsToInsertSummaries.computeIfAbsent(associationName, x -> new ProcessSummaryLine(Status.OK));
               associationToInsertLine.incrementCount(CollectionUtils.nonNullList(entry.getValue()).size());
            }
         }
      }

      runBackendStepOutput.setRecords(outputRecords);
      this.rowsProcessed += records.size();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private List<QErrorMessage> getErrorsFromAssociations(QRecord record)
   {
      List<QErrorMessage> rs = null;
      for(Map.Entry<String, List<QRecord>> entry : CollectionUtils.nonNullMap(record.getAssociatedRecords()).entrySet())
      {
         for(QRecord associatedRecord : CollectionUtils.nonNullList(entry.getValue()))
         {
            if(CollectionUtils.nullSafeHasContents(associatedRecord.getErrors()))
            {
               rs = Objects.requireNonNullElseGet(rs, () -> new ArrayList<>());
               rs.addAll(associatedRecord.getErrors());

               List<QErrorMessage> childErrors = getErrorsFromAssociations(associatedRecord);
               if(CollectionUtils.nullSafeHasContents(childErrors))
               {
                  rs.addAll(childErrors);
               }
            }
         }
      }
      return (rs);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void addToErrorToExampleRowValueMap(AbstractBulkLoadRollableValueError bulkLoadRollableValueError, QRecord record)
   {
      String         message   = bulkLoadRollableValueError.getMessageToUseAsProcessSummaryRollupKey();
      List<RowValue> rowValues = errorToExampleRowValueMap.computeIfAbsent(message, k -> new ArrayList<>());

      if(rowValues.size() < getExampleRowLimit())
      {
         rowValues.add(new RowValue(bulkLoadRollableValueError, record));
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void addToErrorToExampleRowMap(String message, QRecord record)
   {
      List<String> rowNos = errorToExampleRowsMap.computeIfAbsent(message, k -> new ArrayList<>());

      if(rowNos.size() < getExampleRowLimit())
      {
         rowNos.add(BulkLoadRecordUtils.getRowNosString(record));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<QRecord> getRecordsWithoutUniqueKeyErrors(List<QRecord> records, Map<UniqueKey, Set<List<Serializable>>> existingKeys, List<UniqueKey> uniqueKeys, QTableMetaData table)
   {
      ////////////////////////////////////////////////////
      // if there are no UK's, proceed with all records //
      ////////////////////////////////////////////////////
      List<QRecord> recordsWithoutUkErrors = new ArrayList<>();
      if(existingKeys.isEmpty())
      {
         recordsWithoutUkErrors.addAll(records);
      }
      else
      {
         /////////////////////////////////////////////////////////////
         // else, only proceed with records that don't violate a UK //
         /////////////////////////////////////////////////////////////
         for(UniqueKey uniqueKey : uniqueKeys)
         {
            keysInThisFile.computeIfAbsent(uniqueKey, x -> new HashSet<>());
         }

         ///////////////////////////////////////////////////////////////////////////
         // else, get each records keys and see if it already exists or not       //
         // also, build a set of keys we've seen (within this page (or overall?)) //
         ///////////////////////////////////////////////////////////////////////////
         for(QRecord record : records)
         {
            if(CollectionUtils.nullSafeHasContents(record.getErrors()))
            {
               ///////////////////////////////////////////////////
               // skip any records that may already be in error //
               ///////////////////////////////////////////////////
               recordsWithoutUkErrors.add(record);
               continue;
            }

            //////////////////////////////////////////////////////////
            // check if this record violates any of the unique keys //
            //////////////////////////////////////////////////////////
            boolean foundDupe = false;
            for(UniqueKey uniqueKey : uniqueKeys)
            {
               Optional<List<Serializable>> keyValues = UniqueKeyHelper.getKeyValues(table, uniqueKey, record);
               if(keyValues.isPresent() && (existingKeys.get(uniqueKey).contains(keyValues.get()) || keysInThisFile.get(uniqueKey).contains(keyValues.get())))
               {
                  ProcessSummaryLineWithUKSampleValues processSummaryLineWithUKSampleValues = ukErrorSummaries.get(uniqueKey);
                  processSummaryLineWithUKSampleValues.incrementCount();
                  if(processSummaryLineWithUKSampleValues.sampleValues.size() < 3)
                  {
                     processSummaryLineWithUKSampleValues.sampleValues.add(keyValues.get().toString());
                  }
                  else
                  {
                     processSummaryLineWithUKSampleValues.areThereMoreSampleValues = true;
                  }
                  foundDupe = true;
                  break;
               }
            }

            ///////////////////////////////////////////////////////////////////////////////
            // if this record doesn't violate any uk's, then we can add it to the output //
            ///////////////////////////////////////////////////////////////////////////////
            if(!foundDupe)
            {
               for(UniqueKey uniqueKey : uniqueKeys)
               {
                  Optional<List<Serializable>> keyValues = UniqueKeyHelper.getKeyValues(table, uniqueKey, record);
                  keyValues.ifPresent(kv -> keysInThisFile.get(uniqueKey).add(kv));
               }
               recordsWithoutUkErrors.add(record);
            }
         }
      }
      return recordsWithoutUkErrors;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput runBackendStepOutput, boolean isForResultScreen)
   {
      ArrayList<ProcessSummaryLineInterface> rs         = new ArrayList<>();
      String                                 tableLabel = table == null ? "" : table.getLabel();

      ProcessSummaryLine recordsProcessedLine = new ProcessSummaryLine(Status.INFO);
      recordsProcessedLine.setCount(rowsProcessed);
      rs.add(recordsProcessedLine);
      recordsProcessedLine.withMessageSuffix(" processed from the file.");
      recordsProcessedLine.withSingularFutureMessage("record was");
      recordsProcessedLine.withSingularPastMessage("record was");
      recordsProcessedLine.withPluralFutureMessage("records were");
      recordsProcessedLine.withPluralPastMessage("records were");

      boolean isBulkEdit       = BooleanUtils.isTrue(runBackendStepOutput.getValueBoolean("isBulkEdit"));
      String  action           = isBulkEdit ? "updated" : "inserted";
      String  noWarningsSuffix = processSummaryWarningsAndErrorsRollup.countWarnings() == 0 ? "" : " with no warnings";
      okSummary.setSingularFutureMessage(tableLabel + " record will be " + action + noWarningsSuffix + ".");
      okSummary.setPluralFutureMessage(tableLabel + " records will be " + action + noWarningsSuffix + ".");
      okSummary.setSingularPastMessage(tableLabel + " record was " + action + noWarningsSuffix + ".");
      okSummary.setPluralPastMessage(tableLabel + " records were " + action + noWarningsSuffix + ".");
      okSummary.pickMessage(isForResultScreen);
      okSummary.addSelfToListIfAnyCount(rs);

      for(Map.Entry<String, ProcessSummaryLine> entry : associationsToInsertSummaries.entrySet())
      {
         Optional<Association> association = table.getAssociations().stream().filter(a -> a.getName().equals(entry.getKey())).findFirst();
         if(association.isPresent())
         {
            QTableMetaData associationTable = QContext.getQInstance().getTable(association.get().getAssociatedTableName());
            String         associationLabel = associationTable.getLabel();

            ProcessSummaryLine line = entry.getValue();
            line.setSingularFutureMessage(associationLabel + " record will be " + action + ".");
            line.setPluralFutureMessage(associationLabel + " records will be " + action + ".");
            line.setSingularPastMessage(associationLabel + " record was " + action + ".");
            line.setPluralPastMessage(associationLabel + " records were " + action + ".");
            line.pickMessage(isForResultScreen);
            line.addSelfToListIfAnyCount(rs);
         }
      }

      for(Map.Entry<UniqueKey, ProcessSummaryLineWithUKSampleValues> entry : ukErrorSummaries.entrySet())
      {
         UniqueKey                            uniqueKey      = entry.getKey();
         ProcessSummaryLineWithUKSampleValues ukErrorSummary = entry.getValue();

         ukErrorSummary
            .withMessageSuffix(" inserted, because of duplicate values in a unique key on the fields (" + uniqueKey.getDescription(table) + "), with values"
               + (ukErrorSummary.areThereMoreSampleValues ? " such as: " : ": ")
               + StringUtils.joinWithCommasAndAnd(new ArrayList<>(ukErrorSummary.sampleValues)))

            .withSingularFutureMessage(" record will not be")
            .withPluralFutureMessage(" records will not be")
            .withSingularPastMessage(" record was not")
            .withPluralPastMessage(" records were not");

         ukErrorSummary.addSelfToListIfAnyCount(rs);
      }

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // for process summary lines that exist in the error-to-example-row-value map, add those example values to the lines. //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      for(Map.Entry<String, ProcessSummaryLine> entry : processSummaryWarningsAndErrorsRollup.getErrorSummaries().entrySet())
      {
         String message = entry.getKey();
         if(errorToExampleRowValueMap.containsKey(message))
         {
            ProcessSummaryLine line          = entry.getValue();
            List<RowValue>     rowValues     = errorToExampleRowValueMap.get(message);
            String             exampleOrFull = rowValues.size() < line.getCount() ? "Example " : "";
            line.setMessageSuffix(line.getMessageSuffix() + periodIfNeeded(line.getMessageSuffix()) + "  " + exampleOrFull + "Values:");
            line.setBulletsOfText(new ArrayList<>(rowValues.stream().map(String::valueOf).toList()));
         }
         else if(errorToExampleRowsMap.containsKey(message))
         {
            ProcessSummaryLine line            = entry.getValue();
            List<String>       rowDescriptions = errorToExampleRowsMap.get(message);
            String             exampleOrFull   = rowDescriptions.size() < line.getCount() ? "Example " : "";
            line.setMessageSuffix(line.getMessageSuffix() + periodIfNeeded(line.getMessageSuffix()) + "  " + exampleOrFull + "Records:");
            line.setBulletsOfText(new ArrayList<>(rowDescriptions.stream().map(String::valueOf).toList()));
         }
      }

      processSummaryWarningsAndErrorsRollup.addToList(rs);

      return (rs);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private String periodIfNeeded(String input)
   {
      if(input != null && input.matches(".*\\. *$"))
      {
         return ("");
      }

      return (".");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private record RowValue(String row, String value)
   {

      /***************************************************************************
       **
       ***************************************************************************/
      public RowValue(AbstractBulkLoadRollableValueError bulkLoadRollableValueError, QRecord record)
      {
         this(BulkLoadRecordUtils.getRowNosString(record), ValueUtils.getValueAsString(bulkLoadRollableValueError.getValue()));
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public String toString()
      {
         return row + " [" + value + "]";
      }
   }



   /***************************************************************************
    * method a subclass can override, to configure the max number of example-rows
    * to include in error messages presented to users.
    * Default value is 10.
    ***************************************************************************/
   protected int getExampleRowLimit()
   {
      return 10;
   }



   /***************************************************************************
    * method a subclass can override, to configure the page-size used in the
    * loadPotentialRecordsByUniqueKeyByOrQueries method - e.g., where a potentially
    * big query of i n (AND AND) OR (AND AND) OR ... style is built.
    * Default value is 50.
    ***************************************************************************/
   protected int getOrListQueryPageSize()
   {
      return 50;
   }



   /***************************************************************************
    * method a subclass can override, to configure the multiplier for how many
    * rows is considered too many to be fetched in methods used under
    * loadPotentialRecordsByUniqueKeyByOrQueries.  the limit is the number of file
    * records multiplied by this factor.   Default value is 5.
    ***************************************************************************/
   protected int getMaxPotentialRecordsByUniqueKeyLimitFactor()
   {
      return 5;
   }

}
