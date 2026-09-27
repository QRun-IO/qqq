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

package com.kingsrook.qqq.backend.core.processes.implementations.tablesync;


import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.values.QPossibleValueTranslator;
import com.kingsrook.qqq.backend.core.actions.values.QValueFormatter;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.audits.AuditSingleInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.processes.implementations.general.StandardProcessSummaryLineProducer;
import com.kingsrook.qqq.backend.core.processes.utils.RecordLookupHelper;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.Pair;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** This class is for transforming records from a Source table to a Destination table.
 **
 ** The Source table has a (unique/primary) key field:  sourceTableKeyField,
 ** Which is matched against the Destination table's foreign-key:  destinationTableForeignKeyField
 *******************************************************************************/
public abstract class AbstractTableSyncTransformStep extends AbstractTransformStep
{
   protected static final QLogger LOG = QLogger.getLogger(AbstractTableSyncTransformStep.class);

   protected ProcessSummaryLine okToInsert = StandardProcessSummaryLineProducer.getOkToInsertLine();
   protected ProcessSummaryLine okToUpdate = StandardProcessSummaryLineProducer.getOkToUpdateLine();

   protected ProcessSummaryLine willNotInsert = new ProcessSummaryLine(Status.INFO)
      .withMessageSuffix("because this process is not configured to insert records.")
      .withSingularFutureMessage("will not be inserted ")
      .withPluralFutureMessage("will not be inserted ")
      .withSingularPastMessage("was not inserted ")
      .withPluralPastMessage("were not inserted ");

   protected ProcessSummaryLine willNotUpdate = new ProcessSummaryLine(Status.INFO)
      .withMessageSuffix("because this process is not configured to update records.")
      .withSingularFutureMessage("will not be updated ")
      .withPluralFutureMessage("will not be updated ")
      .withSingularPastMessage("was not updated ")
      .withPluralPastMessage("were not updated ");

   protected ProcessSummaryLine errorMissingKeyField = new ProcessSummaryLine(Status.ERROR)
      .withMessageSuffix("missing a value for the key field.")
      .withSingularFutureMessage("will not be synced, because it is ")
      .withPluralFutureMessage("will not be synced, because they are ")
      .withSingularPastMessage("was not synced, because it is ")
      .withPluralPastMessage("were not synced, because they are ");

   protected ProcessSummaryLine unspecifiedError = new ProcessSummaryLine(Status.ERROR)
      .withMessageSuffix("because of an unexpected error: ")
      .withSingularFutureMessage("will not be synced, ")
      .withPluralFutureMessage("will not be synced, ")
      .withSingularPastMessage("was not synced, ")
      .withPluralPastMessage("were not synced, ");

   protected RunBackendStepInput  runBackendStepInput  = null;
   protected RunBackendStepOutput runBackendStepOutput = null;
   protected RecordLookupHelper   recordLookupHelper   = null;

   protected QPossibleValueTranslator possibleValueTranslator;

   protected static final String SYNC_TABLE_PERFORM_INSERTS_KEY = "syncTablePerformInsertsKey";
   protected static final String SYNC_TABLE_PERFORM_UPDATES_KEY = "syncTablePerformUpdatesKey";
   protected static final String LOG_TRANSFORM_RESULTS          = "logTransformResults";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput runBackendStepOutput, boolean isForResultScreen)
   {
      return StandardProcessSummaryLineProducer.toArrayList(okToInsert, okToUpdate, errorMissingKeyField, unspecifiedError, willNotInsert, willNotUpdate);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected ArrayList<ProcessSummaryLineInterface> getErrorProcessSummaryLines(RunBackendStepOutput runBackendStepOutput, boolean isForResultScreen)
   {
      return StandardProcessSummaryLineProducer.toArrayList(errorMissingKeyField, unspecifiedError);
   }



   /*******************************************************************************
    ** Map a record from the source table to the destination table.   e.g., put
    ** values into the destinationRecord, from the sourceRecord.
    **
    ** The destinationRecord will already be constructed, and will actually already
    ** be the record being updated, in the case of an update.  It'll be empty (newly
    ** constructed) for an insert.
    *******************************************************************************/
   public abstract QRecord populateRecordToStore(RunBackendStepInput runBackendStepInput, QRecord destinationRecord, QRecord sourceRecord) throws QException;



   /*******************************************************************************
    ** Specify a list of tableName/keyColumnName pairs to run through
    ** the preloadRecords method of the recordLookupHelper.
    *******************************************************************************/
   protected List<Pair<String, String>> getLookupsToPreLoad()
   {
      return (null);
   }



   /*******************************************************************************
    ** Define the query filter to find existing records.  e.g., for determining
    ** insert vs. update.  Subclasses may override this to customize the behavior,
    ** e.g., in case an additional field is needed in the query.
    *******************************************************************************/
   protected QQueryFilter getExistingRecordQueryFilter(RunBackendStepInput runBackendStepInput, List<Serializable> sourceKeyList)
   {
      String         destinationTableForeignKeyFieldName = getSyncProcessConfig().destinationTableForeignKey;
      String         destinationTableName                = getSyncProcessConfig().destinationTable;
      QFieldMetaData destinationForeignKeyField          = QContext.getQInstance().getTable(destinationTableName).getField(destinationTableForeignKeyFieldName);

      List<Serializable> sourceKeysInDestinationKeyTypeList = null;
      if(sourceKeyList != null)
      {
         sourceKeysInDestinationKeyTypeList = new ArrayList<>();
         for(Serializable sourceKey : sourceKeyList)
         {
            sourceKeysInDestinationKeyTypeList.add(ValueUtils.getValueAsFieldType(destinationForeignKeyField.getType(), sourceKey));
         }
      }

      return new QQueryFilter().withCriteria(new QFilterCriteria(destinationTableForeignKeyFieldName, QCriteriaOperator.IN, sourceKeysInDestinationKeyTypeList));
   }



   /*******************************************************************************
    ** Define the config for this process - e.g., what fields & tables are used.
    *******************************************************************************/
   protected abstract SyncProcessConfig getSyncProcessConfig();



   /*******************************************************************************
    ** Record to store the config for this process - e.g., what fields & tables are used.
    *******************************************************************************/
   public record SyncProcessConfig(String sourceTable, String sourceTableKeyField, String destinationTable, String destinationTableForeignKey, boolean performInserts, boolean performUpdates)
   {

      /*******************************************************************************
       ** Overloaded constructor - defaults both performInserts & performUpdates to true.
       *******************************************************************************/
      public SyncProcessConfig(String sourceTable, String sourceTableKeyField, String destinationTable, String destinationTableForeignKey)
      {
         this(sourceTable, sourceTableKeyField, destinationTable, destinationTableForeignKey, true, true);
      }



      /*******************************************************************************
       ** artificial method, here to make jacoco see that this class is indeed
       ** included in test coverage...
       *******************************************************************************/
      void noop()
      {
         System.out.println("noop");
      }

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void runOnePage(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      if(CollectionUtils.nullSafeIsEmpty(runBackendStepInput.getRecords()))
      {
         LOG.debug("No input records were found.");
         return;
      }

      this.runBackendStepInput = runBackendStepInput;
      this.runBackendStepOutput = runBackendStepOutput;

      SyncProcessConfig config = getSyncProcessConfig();

      ////////////////////////////////////////////////////////////
      // see if these fields have been updated via input fields //
      ////////////////////////////////////////////////////////////
      if(runBackendStepInput.getValueString(SYNC_TABLE_PERFORM_INSERTS_KEY) != null)
      {
         boolean performInserts = Boolean.parseBoolean(runBackendStepInput.getValueString(SYNC_TABLE_PERFORM_INSERTS_KEY));
         config = new SyncProcessConfig(config.sourceTable, config.sourceTableKeyField, config.destinationTable, config.destinationTableForeignKey, performInserts, config.performUpdates);
      }
      if(runBackendStepInput.getValueString(SYNC_TABLE_PERFORM_UPDATES_KEY) != null)
      {
         boolean performUpdates = Boolean.parseBoolean(runBackendStepInput.getValueString(SYNC_TABLE_PERFORM_UPDATES_KEY));
         config = new SyncProcessConfig(config.sourceTable, config.sourceTableKeyField, config.destinationTable, config.destinationTableForeignKey, config.performUpdates, performUpdates);
      }
      String sourceTableKeyField             = config.sourceTableKeyField;
      String destinationTableForeignKeyField = config.destinationTableForeignKey;
      String destinationTableName            = config.destinationTable;
      runBackendStepOutput.addValue(StreamedETLWithFrontendProcess.FIELD_DESTINATION_TABLE, destinationTableName);

      if(!StringUtils.hasContent(sourceTableKeyField))
      {
         throw (new IllegalStateException("Missing sourceTableKeyField in config for " + getClass().getSimpleName()));
      }

      if(!StringUtils.hasContent(destinationTableForeignKeyField))
      {
         throw (new IllegalStateException("Missing destinationTableForeignKey in config for " + getClass().getSimpleName()));
      }

      if(!StringUtils.hasContent(destinationTableName))
      {
         throw (new IllegalStateException("Missing destinationTable in config for " + getClass().getSimpleName()));
      }

      //////////////////////////////////////
      // extract keys from source records //
      //////////////////////////////////////
      List<Serializable> sourceKeyList = runBackendStepInput.getRecords().stream()
         .map(r -> extractSourceKeyValueFromRecord(r, sourceTableKeyField))
         .filter(Objects::nonNull)
         .filter(v -> !"".equals(String.valueOf(v)))
         .collect(Collectors.toList());

      if(this.recordLookupHelper == null)
      {
         initializeRecordLookupHelper(runBackendStepInput, runBackendStepInput.getRecords());
      }
      else
      {
         reinitializeRecordLookupHelper(runBackendStepInput, runBackendStepInput.getRecords());
      }

      ///////////////////////////////////////////////////////////////////////////////////////////////////
      // query to see if we already have those records in the destination (to determine insert/update) //
      ///////////////////////////////////////////////////////////////////////////////////////////////////
      Map<Pair<String, Serializable>, QRecord> existingRecordsByForeignKey = getExistingRecordsByForeignKey(runBackendStepInput, destinationTableForeignKeyField, destinationTableName, sourceKeyList);

      /////////////////////////////////////////////////////////////////
      // foreach source record, build the record we'll insert/update //
      /////////////////////////////////////////////////////////////////
      QFieldMetaData    destinationForeignKeyField = QContext.getQInstance().getTable(destinationTableName).getField(destinationTableForeignKeyField);
      Set<Serializable> processedSourceKeys        = new HashSet<>();
      for(QRecord sourceRecord : runBackendStepInput.getRecords())
      {
         Serializable sourcePrimaryKey = sourceRecord.getValue(QContext.getQInstance().getTable(config.sourceTable).getPrimaryKeyField());
         Serializable sourceKeyValue   = extractSourceKeyValueFromRecord(sourceRecord, sourceTableKeyField);
         if(processedSourceKeys.contains(sourceKeyValue))
         {
            LOG.info("Skipping duplicated source-key within page", logPair("key", sourceKeyValue));
            continue;
         }
         processedSourceKeys.add(sourceKeyValue);

         if(sourceKeyValue == null || "".equals(sourceKeyValue))
         {
            LOG.debug("Skipping record without a value in the sourceKeyField", logPair("keyField", sourceTableKeyField));
            errorMissingKeyField.incrementCountAndAddPrimaryKey(sourcePrimaryKey);

            try
            {
               errorMissingKeyField.setMessageSuffix("missing a value for the field " + runBackendStepInput.getTable().getField(sourceTableKeyField).getLabel());
            }
            catch(Exception e)
            {
               /////////////////////////////////////////
               // just leave the default error suffix //
               /////////////////////////////////////////
            }

            continue;
         }

         //////////////////////////////////////////////////////////////
         // look for the existing record, to determine insert/update //
         //////////////////////////////////////////////////////////////
         try
         {
            QRecord existingRecord = getExistingRecord(existingRecordsByForeignKey, destinationForeignKeyField, sourceKeyValue);

            QRecord recordToStore;
            if(existingRecord != null && config.performUpdates)
            {
               recordToStore = existingRecord;
            }
            else if(existingRecord == null && config.performInserts)
            {
               recordToStore = new QRecord();
            }
            else
            {
               if(existingRecord != null)
               {
                  LOG.debug("Skipping storing existing record because this sync process is set to not perform updates");
                  willNotInsert.incrementCountAndAddPrimaryKey(sourcePrimaryKey);
               }
               else
               {
                  LOG.debug("Skipping storing new record because this sync process is set to not perform inserts");
                  willNotUpdate.incrementCountAndAddPrimaryKey(sourcePrimaryKey);
               }
               continue;
            }

            //////////////////////////////////////////////////////////////////////////////////
            // if we received a record to store add to the output records and summary lines //
            //////////////////////////////////////////////////////////////////////////////////
            recordToStore = populateRecordToStore(runBackendStepInput, recordToStore, sourceRecord);
            if(recordToStore != null)
            {
               if(existingRecord != null)
               {
                  okToUpdate.incrementCountAndAddPrimaryKey(sourcePrimaryKey);
               }
               else
               {
                  okToInsert.incrementCountAndAddPrimaryKey(sourcePrimaryKey);
               }

               runBackendStepOutput.addRecord(recordToStore);
            }
         }
         catch(Exception e)
         {
            unspecifiedError.incrementCountAndAddPrimaryKey(sourcePrimaryKey);
            unspecifiedError.setMessageSuffix(unspecifiedError.getMessageSuffix() + e.getMessage());
         }
      }

      ////////////////////////////////////////////////
      // populate possible-values for review screen //
      ////////////////////////////////////////////////
      if(RunProcessInput.FrontendStepBehavior.BREAK.equals(runBackendStepInput.getFrontendStepBehavior()))
      {
         if(CollectionUtils.nullSafeHasContents(runBackendStepOutput.getRecords()))
         {
            if(possibleValueTranslator == null)
            {
               possibleValueTranslator = new QPossibleValueTranslator(QContext.getQInstance(), QContext.getQSession());
            }

            possibleValueTranslator.translatePossibleValuesInRecords(QContext.getQInstance().getTable(destinationTableName), runBackendStepOutput.getRecords());
         }
      }

      if(Boolean.parseBoolean(runBackendStepInput.getValueString(LOG_TRANSFORM_RESULTS)))
      {
         logResults(runBackendStepInput, config);
      }
   }



   /*******************************************************************************
    ** Log results of transformation
    **
    *******************************************************************************/
   protected void logResults(RunBackendStepInput runBackendStepInput, SyncProcessConfig syncProcessConfig)
   {
      String timezone = QContext.getQSession().getValue(QSession.VALUE_KEY_USER_TIMEZONE);
      if(timezone == null)
      {
         timezone = QContext.getQInstance().getDefaultTimeZoneId();
      }
      Instant lastRunTime = Instant.now();
      if(runBackendStepInput.getBasepullLastRunTime() != null)
      {
         lastRunTime = runBackendStepInput.getBasepullLastRunTime();
      }

      ZonedDateTime dateTime = lastRunTime.atZone(ZoneId.of(timezone));

      if(syncProcessConfig.performInserts)
      {
         if(okToInsert.getCount() == 0)
         {
            LOG.info("No Records were found to insert since " + QValueFormatter.formatDateTimeWithZone(dateTime) + ".");
         }
         else
         {
            String pluralized = okToInsert.getCount() > 1 ? " Records were " : " Record was ";
            LOG.info(okToInsert.getCount() + pluralized + " found to insert since " + QValueFormatter.formatDateTimeWithZone(dateTime) + ".", logPair("primaryKeys", okToInsert.getPrimaryKeys()));
         }
      }

      if(syncProcessConfig.performUpdates)
      {
         if(okToUpdate.getCount() == 0)
         {
            LOG.info("No Records were found to update since " + QValueFormatter.formatDateTimeWithZone(dateTime) + ".");
         }
         else
         {
            String pluralized = okToUpdate.getCount() > 1 ? " Records were " : " Record was ";
            LOG.info(okToUpdate.getCount() + pluralized + " found to update since " + QValueFormatter.formatDateTimeWithZone(dateTime) + ".", logPair("primaryKeys", okToInsert.getPrimaryKeys()));
         }
      }
   }



   /*******************************************************************************
    ** Given a source record, extract what we'll use as its key from it.
    **
    ** Normally this is just its sourceTableKeyField value - but - a subclass may
    ** do something more interesting, including, returning a java-record.
    *******************************************************************************/
   protected Serializable extractSourceKeyValueFromRecord(QRecord sourceRecord, String sourceTableKeyField)
   {
      return sourceRecord.getValue(sourceTableKeyField);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected QRecord getExistingRecord(Map<Pair<String, Serializable>, QRecord> existingRecordsByForeignKey, QFieldMetaData destinationForeignKeyField, Serializable sourceKeyValue)
   {
      //////////////////////////////////////////////////////////////////////////////////////////////////
      // note - we may need to type-convert here, the sourceKey value from the source table to        //
      // the destinationKey.  e.g., if source table had an integer, and the destination has a string. //
      //////////////////////////////////////////////////////////////////////////////////////////////////
      Serializable sourceKeyValueInTargetFieldType = ValueUtils.getValueAsFieldType(destinationForeignKeyField.getType(), sourceKeyValue);
      return (existingRecordsByForeignKey.get(Pair.of(destinationForeignKeyField.getName(), sourceKeyValueInTargetFieldType)));
   }



   /*******************************************************************************
    ** Run the existingRecordQueryFilter - to look in the destinationTable for
    ** any records that may need an update (rather than an insert).
    **
    ** Generally returns a Map, keyed by a Pair of the destinationTableForeignKeyField
    ** and the value in that field.  But, for more complex use-cases, one can override
    ** the buildExistingRecordsMap method, to make different keys (e.g., if there are
    ** two possible destinationTableForeignKeyFields).
    *******************************************************************************/
   protected Map<Pair<String, Serializable>, QRecord> getExistingRecordsByForeignKey(RunBackendStepInput runBackendStepInput, String destinationTableForeignKeyField, String destinationTableName, List<Serializable> sourceKeyList) throws QException
   {
      if(sourceKeyList.isEmpty())
      {
         return (Collections.emptyMap());
      }

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(destinationTableName);
      getTransaction().ifPresent(queryInput::setTransaction);
      QQueryFilter filter = getExistingRecordQueryFilter(runBackendStepInput, sourceKeyList);
      queryInput.setFilter(filter);

      Collection<String> associationNamesToInclude = getAssociationNamesToInclude();
      if(CollectionUtils.nullSafeHasContents(associationNamesToInclude))
      {
         queryInput.setIncludeAssociations(true);
         queryInput.setAssociationNamesToInclude(associationNamesToInclude);
      }

      Collection<QueryJoin> joins = getQueryJoins();
      if(CollectionUtils.nullSafeHasContents(joins))
      {
         queryInput.setQueryJoins(getQueryJoins());
      }

      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      return (buildExistingRecordsMap(destinationTableForeignKeyField, queryOutput.getRecords()));
   }



   /*******************************************************************************
    ** Overridable point where you can, for example, keys in the existingRecordsMap
    ** with different fieldNames from the destinationTable.
    **
    ** Note, if you're overriding this method, you'll likely also want & need to
    ** override getExistingRecord.
    *******************************************************************************/
   protected Map<Pair<String, Serializable>, QRecord> buildExistingRecordsMap(String destinationTableForeignKeyField, List<QRecord> existingRecordList) throws QException
   {
      Map<Pair<String, Serializable>, QRecord> existingRecordsByForeignKey = new HashMap<>();
      for(QRecord record : existingRecordList)
      {
         existingRecordsByForeignKey.put(Pair.of(destinationTableForeignKeyField, record.getValue(destinationTableForeignKeyField)), record);
      }
      return (existingRecordsByForeignKey);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected List<QueryJoin> getQueryJoins()
   {
      return null;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected Collection<String> getAssociationNamesToInclude()
   {
      return null;
   }



   /*******************************************************************************
    ** If needed, init a record lookup helper for this process.
    *******************************************************************************/
   protected void initializeRecordLookupHelper(RunBackendStepInput runBackendStepInput, List<QRecord> sourceRecordList) throws QException
   {
      this.recordLookupHelper = new RecordLookupHelper();

      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // if there's only 1 record, don't bother preloading all records - just do the single lookup by the single needed key. //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      if(runBackendStepInput.getRecords().size() > 1)
      {
         for(Pair<String, String> pair : CollectionUtils.nonNullList(getLookupsToPreLoad()))
         {
            recordLookupHelper.preloadRecords(pair.getA(), pair.getB());
         }
      }
   }



   /*******************************************************************************
    ** for pages after the first, possibly load more records in the lookup helper.
    *******************************************************************************/
   protected void reinitializeRecordLookupHelper(RunBackendStepInput runBackendStepInput, List<QRecord> sourceRecordList) throws QException
   {
      ////////////////////////
      // noop in base class //
      ////////////////////////
   }



   /*******************************************************************************
    ** Let the subclass "easily" add an audit to be inserted on the Execute step.
    *******************************************************************************/
   protected void addAuditForExecuteStep(AuditSingleInput auditSingleInput)
   {
      if(StreamedETLWithFrontendProcess.STEP_NAME_EXECUTE.equals(this.runBackendStepInput.getStepName()))
      {
         this.runBackendStepOutput.addAuditSingleInput(auditSingleInput);
      }
   }

}
