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

package com.kingsrook.qqq.backend.core.processes.implementations.mergeduplicates;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.audits.AuditInput;
import com.kingsrook.qqq.backend.core.model.actions.audits.AuditSingleInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.general.StandardProcessSummaryLineProducer;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ListingHash;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** This class is for merging duplicate records in a table.
 **
 ** We must define for the table one or more fields that we'll use to mark records as unique
 *******************************************************************************/
public abstract class AbstractMergeDuplicatesTransformStep extends AbstractTransformStep
{
   private static final QLogger LOG = QLogger.getLogger(AbstractMergeDuplicatesTransformStep.class);

   private ProcessSummaryLine okToInsert           = StandardProcessSummaryLineProducer.getOkToInsertLine();
   private ProcessSummaryLine okToUpdate           = StandardProcessSummaryLineProducer.getOkToUpdateLine();
   private ProcessSummaryLine okToDelete           = StandardProcessSummaryLineProducer.getOkToDeleteLine();
   private ProcessSummaryLine errorMissingKeyField = new ProcessSummaryLine(Status.ERROR)
      .withMessageSuffix("missing a value for the key field.")
      .withSingularFutureMessage("will not be synced, because it is ")
      .withPluralFutureMessage("will not be synced, because they are ")
      .withSingularPastMessage("was not synced, because it is ")
      .withPluralPastMessage("were not synced, because they are ");

   private ProcessSummaryLine notADuplicate   = new ProcessSummaryLine(Status.INFO, "did not have any duplicates.");
   private ProcessSummaryLine requestedToSkip = new ProcessSummaryLine(Status.INFO)
      .withSingularFutureMessage("will be skipped, because it was not clear how it should be processed.")
      .withPluralFutureMessage("will be skipped, because it was not clear how they should be processed.")
      .withSingularPastMessage("was skipped, because it was not clear how it should have been processed.")
      .withPluralPastMessage("were skipped, because it was not clear how they should have been processed.");

   protected RunBackendStepInput runBackendStepInput = null;

   private ListingHash<String, Serializable> otherTableIdsToDelete     = new ListingHash<>();
   private ListingHash<String, QQueryFilter> otherTableFiltersToDelete = new ListingHash<>();
   private ListingHash<String, QRecord>      otherTableRecordsToStore  = new ListingHash<>();

   private AuditInput auditInput = new AuditInput();

   private Set<List<Serializable>> keysSeenInPreviousPages = new HashSet<>();



   /*******************************************************************************
    ** Do the main work for this process - merge a list of records.
    ** May also call addOtherTableIdsToDelete, addOtherTableFilterToDelete,
    ** and addOtherTableRecordsToStore
    *******************************************************************************/
   public abstract QRecord buildRecordToKeep(RunBackendStepInput runBackendStepInput, List<QRecord> duplicateRecords) throws QException, SkipTheseRecordsException;


   /*******************************************************************************
    ** Define the config for this process - e.g., what fields & tables are used.
    *******************************************************************************/
   protected abstract MergeProcessConfig getMergeProcessConfig();



   /*******************************************************************************
    ** Optional point where subclasses can pre-load data in-bulk on all the duplicates.
    *******************************************************************************/
   protected void preProcess(ListingHash<List<Serializable>, QRecord> duplicatesMap) throws QException
   {
      ////////////////////////
      // noop in base class //
      ////////////////////////
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput runBackendStepOutput, boolean isForResultScreen)
   {
      ArrayList<ProcessSummaryLineInterface> processSummaryLineInterfaces = StandardProcessSummaryLineProducer.toArrayList(okToInsert, okToUpdate, okToDelete, notADuplicate, requestedToSkip, errorMissingKeyField);

      try
      {
         Serializable recordCount = runBackendStepOutput.getValue("recordCount");
         if(recordCount instanceof Integer recordCountInt)
         {
            int sum = processSummaryLineInterfaces.stream().filter(ProcessSummaryLine.class::isInstance).mapToInt(psli -> ((ProcessSummaryLine) psli).getCount()).sum();
            if(sum != recordCountInt)
            {
               processSummaryLineInterfaces.add(new ProcessSummaryLine(Status.INFO, null, "These counts may not add up to the number of selected records, because this process looks for duplicates of the selected records outside of what was selected."));
            }
         }
      }
      catch(Exception e)
      {
         // just continue
      }

      return processSummaryLineInterfaces;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected void addOtherTableIdsToDelete(String tableName, Collection<Serializable> ids)
   {
      otherTableIdsToDelete.addAll(tableName, ids);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected void addOtherTableFilterToDelete(String tableName, QQueryFilter filter)
   {
      otherTableFiltersToDelete.add(tableName, filter);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected void addOtherTableRecordsToStore(String tableName, Collection<QRecord> records)
   {
      otherTableRecordsToStore.addAll(tableName, records);
   }



   /*******************************************************************************
    ** Record to store the config for this process - e.g., what fields & tables are used.
    *******************************************************************************/
   public record MergeProcessConfig(String tableName, List<String> uniqueKeyFieldNames, boolean doAutomaticAudits)
   {
      /*******************************************************************************
       ** artificial method, here to make jacoco see that this class is indeed
       ** included in test coverage...
       ** todo call me
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
         return;
      }

      this.runBackendStepInput = runBackendStepInput;

      ////////////////////////////////////
      // clear these lists on each page //
      ////////////////////////////////////
      otherTableIdsToDelete.clear();
      otherTableFiltersToDelete.clear();
      otherTableRecordsToStore.clear();

      MergeProcessConfig config = getMergeProcessConfig();

      String       tableName           = config.tableName;
      List<String> uniqueKeyFieldNames = config.uniqueKeyFieldNames;

      if(!StringUtils.hasContent(tableName))
      {
         throw (new IllegalStateException("Missing tableName in config for " + getClass().getSimpleName()));
      }

      if(CollectionUtils.nullSafeIsEmpty(uniqueKeyFieldNames))
      {
         throw (new IllegalStateException("Missing uniqueKeyFieldNames in config for " + getClass().getSimpleName()));
      }

      QTableMetaData table = QContext.getQInstance().getTable(tableName);
      if(table == null)
      {
         throw (new IllegalStateException("Unrecognized table name: " + tableName));
      }

      String               primaryKeyField = table.getPrimaryKeyField();
      List<QFieldMetaData> uniqueKeyFields = new ArrayList<>();

      for(String fieldName : uniqueKeyFieldNames)
      {
         QFieldMetaData field = table.getField(fieldName);
         uniqueKeyFields.add(field);
      }

      String ukLabels = StringUtils.joinWithCommasAndAnd(uniqueKeyFields.stream().map(QFieldMetaData::getLabel).toList());

      ////////////////////////////////
      // build query for duplicates //
      ////////////////////////////////
      QQueryFilter filter = new QQueryFilter();
      filter.setBooleanOperator(QQueryFilter.BooleanOperator.OR);
      for(QRecord record : runBackendStepInput.getRecords())
      {
         List<Serializable> ukValues  = new ArrayList<>();
         QQueryFilter       subFilter = new QQueryFilter();
         for(QFieldMetaData field : uniqueKeyFields)
         {
            ukValues.add(record.getValue(field.getName()));
            subFilter.addCriteria(new QFilterCriteria(field.getName(), QCriteriaOperator.EQUALS, record.getValue(field.getName())));
         }

         if(keysSeenInPreviousPages.contains(ukValues))
         {
            /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // skip this key if it was in a previous page (on a different record, but the same key, so *this* record would have been found & dealt with there) //
            /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            LOG.trace("Not re-processing a key from a previous page: " + ukValues);
            continue;
         }

         filter.addSubFilter(subFilter);
         keysSeenInPreviousPages.add(ukValues);
      }

      if(CollectionUtils.nullSafeIsEmpty(filter.getSubFilters()))
      {
         LOG.trace("No sub-filters were added - all of these records were duplicates that were processed in previous pages.");
         return;
      }

      LOG.trace("For an input list of [" + runBackendStepInput.getRecords().size() + "] records, we have a query with [" + filter.getSubFilters().size() + "] sub-filters.");

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(tableName);
      queryInput.setFilter(filter);
      QueryOutput queryOutput = new QueryAction().execute(queryInput);

      ListingHash<List<Serializable>, QRecord> duplicatesMap = new ListingHash<>();
      for(QRecord record : queryOutput.getRecords())
      {
         List<Serializable> ukValues = new ArrayList<>();
         for(QFieldMetaData field : uniqueKeyFields)
         {
            ukValues.add(record.getValue(field.getName()));
         }
         duplicatesMap.add(ukValues, record);
      }
      LOG.trace("Query for duplicates found [" + queryOutput.getRecords().size() + "] records with [" + duplicatesMap.keySet().size() + "] unique keys.");

      ////////////////////////////////////////////////////////////////////////////////////////////////
      // let the subclass optionally do any needed pre-processing on the batch (e.g., bulk lookups) //
      ////////////////////////////////////////////////////////////////////////////////////////////////
      preProcess(duplicatesMap);

      /////////////////////////////////////////////////////////////////////////////////////////
      // process the entries - keys are the duplicated key, values are the duplicate records //
      /////////////////////////////////////////////////////////////////////////////////////////
      for(Map.Entry<List<Serializable>, List<QRecord>> entry : duplicatesMap.entrySet())
      {
         List<QRecord> duplicateRecords = entry.getValue();
         if(duplicateRecords.size() == 1)
         {
            ////////////////////////////////////////////////////
            // if there aren't any duplicates here, note that //
            ////////////////////////////////////////////////////
            notADuplicate.incrementCountAndAddPrimaryKey(duplicateRecords.get(0).getValue(primaryKeyField));
            continue;
         }

         try
         {
            QRecord recordToKeep = buildRecordToKeep(runBackendStepInput, duplicateRecords);
            if(recordToKeep == null)
            {
               duplicateRecords.forEach(requestedToSkip::incrementCountAndAddPrimaryKey);
               continue;
            }

            runBackendStepOutput.addRecord(recordToKeep);
            Serializable primaryKeyToKeep = recordToKeep.getValue(primaryKeyField);
            if(primaryKeyToKeep == null)
            {
               okToInsert.incrementCount();

               if(config.doAutomaticAudits)
               {
                  // todo - how to get the id of the inserted record in here...
                  // todo - audit details w/ the ids of the others
                  AuditSingleInput auditSingleInput = new AuditSingleInput().forRecord(table, recordToKeep)
                     .withMessage("Merged " + duplicateRecords.size() + " records with the same " + ukLabels + " into a new record.");
                  auditInput.addAuditSingleInput(auditSingleInput);
               }
            }
            else
            {
               LOG.trace("Decided to keep pkey [" + primaryKeyToKeep + "] for key [" + entry.getKey() + "]");
               okToUpdate.incrementCountAndAddPrimaryKey(primaryKeyToKeep);

               if(config.doAutomaticAudits)
               {
                  // todo - audit details w/ the ids of the others
                  AuditSingleInput auditSingleInput = new AuditSingleInput().forRecord(table, recordToKeep)
                     .withMessage("Merged " + (duplicateRecords.size() - 1) + " other record" + StringUtils.plural(duplicateRecords.size() - 1) + " with the same " + ukLabels + " into this record.");
                  auditInput.addAuditSingleInput(auditSingleInput);
               }
            }

            for(QRecord duplicate : duplicateRecords)
            {
               Serializable duplicatePrimaryKey = duplicate.getValue(primaryKeyField);
               if(!Objects.equals(primaryKeyToKeep, duplicatePrimaryKey))
               {
                  otherTableIdsToDelete.add(tableName, duplicatePrimaryKey);
                  okToDelete.incrementCountAndAddPrimaryKey(duplicatePrimaryKey);
                  LOG.trace("Decided to delete pkey [" + duplicate + "] for key [" + entry.getKey() + "]");

                  if(config.doAutomaticAudits)
                  {
                     auditInput.addAuditSingleInput(new AuditSingleInput().forRecord(table, duplicate)
                        .withMessage("Deleted this record while merging it with " + (primaryKeyToKeep == null ? "a new record" : primaryKeyToKeep)));
                  }
               }
            }
         }
         catch(SkipTheseRecordsException e)
         {
            duplicateRecords.forEach(requestedToSkip::incrementCountAndAddPrimaryKey);
         }
      }

      runBackendStepOutput.addValue("otherTableIdsToDelete", otherTableIdsToDelete);
      runBackendStepOutput.addValue("otherTableFiltersToDelete", otherTableFiltersToDelete);
      runBackendStepOutput.addValue("otherTableRecordsToStore", otherTableRecordsToStore);

      if(config.doAutomaticAudits && "execute".equals(runBackendStepInput.getStepName()))
      {
         runBackendStepOutput.addValue("auditInput", auditInput);
      }

      ////////////////////////////////////////////////
      // populate possible-values for review screen //
      ////////////////////////////////////////////////
      /* todo
      if(RunProcessInput.FrontendStepBehavior.BREAK.equals(runBackendStepInput.getFrontendStepBehavior()))
      {
         if(CollectionUtils.nullSafeHasContents(runBackendStepOutput.getRecords()))
         {
            if(possibleValueTranslator == null)
            {
               possibleValueTranslator = new QPossibleValueTranslator(runBackendStepInput.getInstance(), runBackendStepInput.getSession());
            }

            possibleValueTranslator.translatePossibleValuesInRecords(runBackendStepInput.getInstance().getTable(uniqueKeyFieldNames), runBackendStepOutput.getRecords());
         }
      }
      */
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class SkipTheseRecordsException extends QException
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      public SkipTheseRecordsException(Throwable t)
      {
         super(t);
      }



      /*******************************************************************************
       ** Constructor
       **
       *******************************************************************************/
      public SkipTheseRecordsException(String message)
      {
         super(message);
      }



      /*******************************************************************************
       ** Constructor
       **
       *******************************************************************************/
      public SkipTheseRecordsException(String message, Throwable t)
      {
         super(message, t);
      }

   }
}
