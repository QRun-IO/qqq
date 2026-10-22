/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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


package com.kingsrook.qqq.backend.core.jobs.plans;


import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import com.fasterxml.jackson.core.type.TypeReference;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.audits.DMLAuditAction;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.KeysetPager;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaData;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsTables;
import com.kingsrook.qqq.backend.core.jobs.SystemScope;
import com.kingsrook.qqq.backend.core.jobs.model.ChunkOutcome;
import com.kingsrook.qqq.backend.core.jobs.model.JobIdempotency;
import com.kingsrook.qqq.backend.core.jobs.model.JobMode;
import com.kingsrook.qqq.backend.core.jobs.model.JobSourceType;
import com.kingsrook.qqq.backend.core.jobs.progress.JobProgressAsyncJobCallback;
import com.kingsrook.qqq.backend.core.jobs.uploads.UploadRefs;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.savedbulkloadprofiles.SavedBulkLoadProfile;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.assist.BulkLoadAssist;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.filehandling.FileToRowsInterface;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.filehandling.JsonlFileToRows;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadFileRow;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadProfile;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** The CHUNKED plan for streamed ETL processes (spec section 2.4, plan 1): bulk
 ** insert, edit and delete, record actions and data processes.
 **
 ** Planning (as the launching user):
 ** - TABLE_FILTER / RECORD_IDS: primary-key ranges over the frozen filter, one
 **   page of chunkSize keys each (KeysetPager). Rows added later are not picked
 **   up, as today.
 ** - STORAGE_FILE: the upload is streamed ONCE into chunk objects in the jobs
 **   storage table - JSON Lines of rows with their ORIGINAL row numbers (plus
 **   the header row) - so each chunk reads only its own object and a restart
 **   never rescans the file. No temp file.
 ** - ROW_SET (retry failed rows): the failed rows only, from the parent job's
 **   chunk objects (files) or by key (tables).
 **
 ** Running a chunk runs the process's OWN steps (headless, frontend steps
 ** skipped) restricted to the chunk: queryFilterJson AND the key range, or the
 ** chunk object as the bulk-load file. Failed rows are reported by the
 ** transform and load steps through JobChunkScope. With SAME_TRANSACTION the
 ** chunk's writes, its row errors and its DONE mark commit in one transaction.
 *******************************************************************************/
public class ChunkedProcessPlan implements JobPlan
{
   public static final String SOURCE_FILTER        = "filter";
   public static final String SOURCE_RECORD_IDS    = "recordIds";
   public static final String SOURCE_UPLOAD_REFS   = "uploadRefs";
   public static final String SOURCE_PROFILE_JSON  = "profileJson";
   public static final String SOURCE_PARENT_JOB_ID = "parentJobId";
   public static final String SOURCE_ROW_NUMBERS   = "rowNumbers";
   public static final String SOURCE_RECORD_PKS    = "recordPks";
   public static final String SOURCE_PARENT_TYPE   = "parentSourceType";

   private static final String RANGE_LO   = "lo";
   private static final String RANGE_HI   = "hi";
   private static final String RANGE_ROWS = "rows";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> planChunks(QRecord job) throws QException
   {
      JobSourceType       sourceType = JobSourceType.valueOf(job.getValueString("sourceType"));
      Map<String, Object> source     = StepsJobPlan.readSource(job);
      int                 chunkSize  = Math.max(1, Objects.requireNonNullElse(job.getValueInteger("chunkSize"), 500));

      List<QRecord> chunks = switch(sourceType)
      {
         case TABLE_FILTER, RECORD_IDS -> planTable(job, baseFilter(job, source), chunkSize);
         case STORAGE_FILE -> planFile(job, source, chunkSize);
         case ROW_SET -> planRowSet(job, source, chunkSize);
         default -> throw (new QException("The chunked plan does not handle source type " + sourceType));
      };

      long total = 0;
      for(QRecord chunk : chunks)
      {
         total += ValueUtils.getValueAsInteger(readRange(chunk).getOrDefault(RANGE_ROWS, 0));
      }
      job.setValue("rowsTotal", (int) total);
      return (chunks);
   }



   /*******************************************************************************
    ** Key ranges of chunkSize rows over a filter.
    *******************************************************************************/
   private List<QRecord> planTable(QRecord job, QQueryFilter filter, int chunkSize) throws QException
   {
      String         tableName = sourceTableName(job);
      QTableMetaData table     = QContext.getQInstance().getTable(tableName);
      String         pkField   = table.getPrimaryKeyField();
      List<QRecord>  chunks    = new ArrayList<>();

      KeysetPager.forTable(tableName).withFilter(filter.clone()).withPageSize(chunkSize).forEachPage(page ->
      {
         if(!page.isEmpty())
         {
            Map<String, Object> range = new LinkedHashMap<>();
            range.put(RANGE_LO, page.get(0).getValue(pkField));
            range.put(RANGE_HI, page.get(page.size() - 1).getValue(pkField));
            range.put(RANGE_ROWS, page.size());
            chunks.add(new QRecord().withValue("seq", chunks.size()).withValue("rangeJson", JsonUtils.toJson(range)));
         }
      });
      return (chunks);
   }



   /*******************************************************************************
    ** Stream an upload once into JSONL chunk objects.
    *******************************************************************************/
   private List<QRecord> planFile(QRecord job, Map<String, Object> source, int chunkSize) throws QException
   {
      List<?> refs = source.get(SOURCE_UPLOAD_REFS) instanceof List<?> list ? list : List.of();
      if(refs.isEmpty())
      {
         throw (new QUserFacingException("A file job needs an uploaded file (uploadRefs)."));
      }
      String       ref       = String.valueOf(refs.get(0));
      QRecord      upload    = UploadRefs.get(ref);
      StorageInput input     = UploadRefs.open(ref);
      boolean      hasHeader = hasHeaderRow(job, source);

      try(InputStream inputStream = SystemScope.call(() -> new StorageAction().getInputStream(input));
          FileToRowsInterface rows = FileToRowsInterface.forFile(upload.getValueString("fileName"), inputStream))
      {
         BulkLoadFileRow header = hasHeader && rows.hasNext() ? rows.next() : null;
         return (writeChunkObjects(header, rows, chunkSize, null));
      }
      catch(QException e)
      {
         throw (e);
      }
      catch(Exception e)
      {
         throw (new QException("Could not split the uploaded file into chunks", e));
      }
   }



   /*******************************************************************************
    ** The failed rows of a parent job, re-planned as a new job's chunks.
    *******************************************************************************/
   private List<QRecord> planRowSet(QRecord job, Map<String, Object> source, int chunkSize) throws QException
   {
      Integer parentJobId = ValueUtils.getValueAsInteger(source.get(SOURCE_PARENT_JOB_ID));
      String  parentType  = ValueUtils.getValueAsString(source.get(SOURCE_PARENT_TYPE));
      if(JobSourceType.STORAGE_FILE.name().equals(parentType) || JobSourceType.ROW_SET.name().equals(parentType) && source.get(SOURCE_ROW_NUMBERS) != null)
      {
         Set<Integer> wanted = new HashSet<>();
         if(source.get(SOURCE_ROW_NUMBERS) instanceof List<?> list)
         {
            list.forEach(n -> wanted.add(ValueUtils.getValueAsInteger(n)));
         }

         List<QRecord>       parentChunks = SystemScope.call(() -> QueryAction.execute(DurableJobsTables.TABLE_CHUNK,
            new QQueryFilter(new QFilterCriteria("jobId", QCriteriaOperator.EQUALS, parentJobId)).withOrderBy(new QFilterOrderBy("seq"))));
         String              storageTable = storageTableName();
         List<BulkLoadFileRow> selected   = new ArrayList<>();
         BulkLoadFileRow       header     = null;
         boolean               hasHeader  = hasHeaderRow(job, source);
         for(QRecord parentChunk : parentChunks)
         {
            String objectRef = parentChunk.getValueString("inputStorageRef");
            if(objectRef == null)
            {
               continue;
            }
            StorageInput chunkInput = new StorageInput(storageTable).withReference(objectRef);
            try(InputStream inputStream = SystemScope.call(() -> new StorageAction().getInputStream(chunkInput));
                FileToRowsInterface rows = FileToRowsInterface.forFile(objectRef, inputStream))
            {
               BulkLoadFileRow chunkHeader = hasHeader && rows.hasNext() ? rows.next() : null;
               header = header == null ? chunkHeader : header;
               while(rows.hasNext())
               {
                  BulkLoadFileRow row = rows.next();
                  if(wanted.contains(row.getRowNo()))
                  {
                     selected.add(row);
                  }
               }
            }
            catch(QException e)
            {
               throw (e);
            }
            catch(Exception e)
            {
               throw (new QException("Could not read the parent job's chunk " + objectRef, e));
            }
         }
         return (writeChunkObjects(header, selected.iterator(), chunkSize, null));
      }

      ///////////////////////////////////////
      // table rows: by key, in key ranges //
      ///////////////////////////////////////
      List<Serializable> pks = new ArrayList<>();
      if(source.get(SOURCE_RECORD_PKS) instanceof List<?> list)
      {
         list.forEach(pk -> pks.add((Serializable) pk));
      }
      QTableMetaData table = QContext.getQInstance().getTable(sourceTableName(job));
      return (planTable(job, new QQueryFilter(new QFilterCriteria(table.getPrimaryKeyField(), QCriteriaOperator.IN, pks)), chunkSize));
   }



   /*******************************************************************************
    ** Write rows into chunk objects of chunkSize rows each (header first in every
    ** object). Bounded memory: one row at a time.
    *******************************************************************************/
   private List<QRecord> writeChunkObjects(BulkLoadFileRow header, Iterator<BulkLoadFileRow> rows, int chunkSize, String prefixOrNull) throws QException
   {
      String        storageTable = storageTableName();
      String        prefix       = prefixOrNull != null ? prefixOrNull : "jobs/" + LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy/MM/dd")) + "/" + UUID.randomUUID() + "/";
      List<QRecord> chunks       = new ArrayList<>();

      while(rows.hasNext())
      {
         int          seq       = chunks.size();
         String       reference = prefix + String.format("chunk-%06d.jsonl", seq);
         StorageInput output    = new StorageInput(storageTable).withReference(reference).withContentType("application/x-ndjson");
         int          count     = 0;
         Integer      firstRow  = null;
         Integer      lastRow   = null;

         try(OutputStream outputStream = SystemScope.call(() -> new StorageAction().createOutputStream(output)))
         {
            if(header != null)
            {
               JsonlFileToRows.writeRow(outputStream, header);
            }
            while(rows.hasNext() && count < chunkSize)
            {
               BulkLoadFileRow row = rows.next();
               JsonlFileToRows.writeRow(outputStream, row);
               firstRow = firstRow == null ? row.getRowNo() : firstRow;
               lastRow = row.getRowNo();
               count++;
            }
         }
         catch(QException e)
         {
            throw (e);
         }
         catch(Exception e)
         {
            throw (new QException("Could not write a chunk object", e));
         }

         Map<String, Object> range = new LinkedHashMap<>();
         range.put(RANGE_LO, firstRow);
         range.put(RANGE_HI, lastRow);
         range.put(RANGE_ROWS, count);
         chunks.add(new QRecord().withValue("seq", seq).withValue("rangeJson", JsonUtils.toJson(range)).withValue("inputStorageRef", reference));
      }
      return (chunks);
   }



   /*******************************************************************************
    ** Run one chunk: the process's own steps, restricted to the chunk.
    *******************************************************************************/
   @Override
   public ChunkOutcome runChunk(ChunkContext context) throws Exception
   {
      QRecord             job     = context.getJob();
      QRecord             chunk   = context.getChunk();
      QProcessMetaData    process = QContext.getQInstance().getProcess(job.getValueString("processName"));
      Map<String, Object> source  = StepsJobPlan.readSource(job);
      Map<String, Object> range   = readRange(chunk);
      boolean             isFile  = chunk.getValueString("inputStorageRef") != null;
      boolean             isValidate = JobMode.VALIDATE.name().equals(job.getValueString("mode"));

      Map<String, Serializable> values = StepsJobPlan.readValues(job.getValueString("paramsJson"));
      values.put(DMLAuditAction.AUDIT_CONTEXT_FIELD_NAME, "job:" + job.getValueInteger("id") + "/chunk:" + chunk.getValueInteger("seq"));
      values.put(StreamedETLWithFrontendProcess.FIELD_DO_FULL_VALIDATION, isValidate);

      if(isFile)
      {
         ArrayList<StorageInput> theFile = new ArrayList<>(List.of(new StorageInput(storageTableName()).withReference(chunk.getValueString("inputStorageRef"))));
         values.put("theFile", theFile);
         values.put("isHeadless", true);
         values.put("tableName", job.getValueString("tableName"));
         if(job.getValueInteger("savedBulkLoadProfileId") != null)
         {
            values.put("savedBulkLoadProfileId", job.getValueInteger("savedBulkLoadProfileId"));
         }
         else if(source.get(SOURCE_PROFILE_JSON) != null)
         {
            values.put(BulkLoadAssist.VALUE_BULK_LOAD_PROFILE_JSON, String.valueOf(source.get(SOURCE_PROFILE_JSON)));
         }
      }
      else
      {
         QTableMetaData table  = QContext.getQInstance().getTable(sourceTableName(job));
         QQueryFilter   filter = rowSetOrBaseFilter(job, source, table);
         filter.addCriteria(new QFilterCriteria(table.getPrimaryKeyField(), QCriteriaOperator.GREATER_THAN_OR_EQUALS, (Serializable) range.get(RANGE_LO)));
         filter.addCriteria(new QFilterCriteria(table.getPrimaryKeyField(), QCriteriaOperator.LESS_THAN_OR_EQUALS, (Serializable) range.get(RANGE_HI)));
         values.put("queryFilterJson", JsonUtils.toJson(filter));
         values.remove("recordIds");
         values.remove("filterJSON");
      }

      boolean             sameTransaction = JobIdempotency.SAME_TRANSACTION.name().equals(job.getValueString("idempotency")) && !isValidate;
      QBackendTransaction transaction     = sameTransaction ? QBackendTransaction.openFor(new InsertInput(destinationTableName(job, process))) : null;
      try(JobChunkScope scope = JobChunkScope.open(transaction, context::check, context.identity()).withProgress(context.getProgress()))
      {
         context.check();

         RunProcessInput input = new RunProcessInput();
         input.setProcessName(process.getName());
         input.setAsyncJobCallback(new JobProgressAsyncJobCallback(context.getProgress()));
         input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
         input.setValues(new LinkedHashMap<>(values));
         Map<String, Serializable> validation = new LinkedHashMap<>();
         if(isValidate)
         {
            //////////////////////////////////////////////////////////////////
            // VALIDATE: stop after validation; nothing is stored (no load) //
            //////////////////////////////////////////////////////////////////
            input.setBackendStepListener((stepName, processState) ->
            {
               if(StreamedETLWithFrontendProcess.STEP_NAME_VALIDATE.equals(stepName))
               {
                  validation.putAll(processState.getValues());
                  throw (new StopAfterValidation());
               }
            });
         }

         Map<String, Serializable> outputValues;
         try
         {
            RunProcessOutput output = new RunProcessAction().execute(input);
            if(output.getException().isPresent())
            {
               throw (output.getException().get());
            }
            outputValues = output.getValues();
         }
         catch(Exception e)
         {
            if(isValidate && findStop(e))
            {
               outputValues = validation;
            }
            else
            {
               throw (e);
            }
         }

         List<QRecord> rowErrors = toRowErrorRecords(job, chunk, scope.getRowErrors());
         int           rowsIn    = ValueUtils.getValueAsInteger(range.getOrDefault(RANGE_ROWS, 0));
         int           rowsFailed = countFailedRows(scope.getRowErrors());
         Serializable  summary   = outputValues.get(isValidate ? StreamedETLWithFrontendProcess.FIELD_VALIDATION_SUMMARY : StreamedETLWithFrontendProcess.FIELD_PROCESS_SUMMARY);
         ChunkOutcome  outcome   = new ChunkOutcome(rowsIn, Math.max(0, rowsIn - rowsFailed), rowsFailed, summary == null ? null : JsonUtils.toJson(summary), null);

         if(transaction != null)
         {
            context.completeInTransaction(outcome, rowErrors, scope.drainEffects(), transaction);
            transaction.commit();
         }
         else
         {
            context.getStore().recordRowErrors(rowErrors, null);
            context.getProgress().flush();
            context.recordEffects(scope.drainEffects());
         }
         return (outcome);
      }
      catch(Exception e)
      {
         if(transaction != null)
         {
            try
            {
               transaction.rollback();
            }
            catch(Exception rollbackException)
            {
               e.addSuppressed(rollbackException);
            }
         }
         throw (e);
      }
      finally
      {
         if(transaction != null)
         {
            transaction.close();
         }
      }
   }



   /*******************************************************************************
    ** Thrown by the VALIDATE listener to stop before the execute step.
    *******************************************************************************/
   static class StopAfterValidation extends QException
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      StopAfterValidation()
      {
         super("Validation finished");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static boolean findStop(Throwable t)
   {
      for(int i = 0; t != null && i < 50; t = t.getCause(), i++)
      {
         if(t instanceof StopAfterValidation)
         {
            return (true);
         }
      }
      return (false);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<QRecord> toRowErrorRecords(QRecord job, QRecord chunk, List<JobChunkScope.RowError> errors)
   {
      List<QRecord> records = new ArrayList<>();
      Instant now = Instant.now();
      for(JobChunkScope.RowError error : errors)
      {
         records.add(new QRecord()
            .withValue("jobId", job.getValueInteger("id"))
            .withValue("chunkId", chunk.getValueInteger("id"))
            .withValue("rowNumber", error.rowNumber())
            .withValue("recordPk", error.recordPk() == null ? null : String.valueOf(error.recordPk()))
            .withValue("fieldName", error.fieldName())
            .withValue("errorCode", StringUtils.safeTruncate(error.errorCode(), 100))
            .withValue("message", StringUtils.safeTruncate(error.message(), 4000))
            .withValue("resolved", false)
            .withValue("createDate", now));
      }
      return (records);
   }



   /*******************************************************************************
    ** Distinct failed rows (a row with two errors counts once).
    *******************************************************************************/
   private static int countFailedRows(List<JobChunkScope.RowError> errors)
   {
      Set<String> rows = new HashSet<>();
      for(JobChunkScope.RowError error : errors)
      {
         rows.add(error.rowNumber() != null ? "r" + error.rowNumber() : "k" + error.recordPk());
      }
      return (rows.size());
   }



   /*******************************************************************************
    ** The job's base filter: its filter, or its record ids as an IN.
    *******************************************************************************/
   static QQueryFilter baseFilter(QRecord job, Map<String, Object> source) throws QException
   {
      QTableMetaData table = QContext.getQInstance().getTable(sourceTableName(job));
      if(source.get(SOURCE_FILTER) != null)
      {
         try
         {
            QQueryFilter filter = JsonUtils.toObject(JsonUtils.toJson(source.get(SOURCE_FILTER)), QQueryFilter.class);
            filter.setOrderBys(new ArrayList<>());
            filter.setLimit(null);
            filter.setSkip(null);
            return (filter);
         }
         catch(Exception e)
         {
            throw (new QException("Could not read the job's filter", e));
         }
      }
      if(source.get(SOURCE_RECORD_IDS) instanceof List<?> ids)
      {
         return (new QQueryFilter(new QFilterCriteria(table.getPrimaryKeyField(), QCriteriaOperator.IN, ids.stream().map(id -> (Serializable) id).toList())));
      }
      throw (new QUserFacingException("A table job needs a filter or record ids."));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QQueryFilter rowSetOrBaseFilter(QRecord job, Map<String, Object> source, QTableMetaData table) throws QException
   {
      if(JobSourceType.ROW_SET.name().equals(job.getValueString("sourceType")))
      {
         List<Serializable> pks = new ArrayList<>();
         if(source.get(SOURCE_RECORD_PKS) instanceof List<?> list)
         {
            list.forEach(pk -> pks.add((Serializable) pk));
         }
         return (new QQueryFilter(new QFilterCriteria(table.getPrimaryKeyField(), QCriteriaOperator.IN, pks)));
      }
      return (baseFilter(job, source));
   }



   /*******************************************************************************
    ** The table a streamed ETL process reads from.
    *******************************************************************************/
   static String sourceTableName(QRecord job)
   {
      QProcessMetaData process = QContext.getQInstance().getProcess(job.getValueString("processName"));
      Serializable     value   = process == null ? null : inputDefault(process, StreamedETLWithFrontendProcess.FIELD_SOURCE_TABLE);
      return (value != null ? String.valueOf(value) : job.getValueString("tableName"));
   }



   /*******************************************************************************
    ** The table a streamed ETL process writes to.
    *******************************************************************************/
   static String destinationTableName(QRecord job, QProcessMetaData process)
   {
      Serializable value = inputDefault(process, StreamedETLWithFrontendProcess.FIELD_DESTINATION_TABLE);
      return (value != null ? String.valueOf(value) : job.getValueString("tableName"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static boolean hasHeaderRow(QRecord job, Map<String, Object> source) throws QException
   {
      try
      {
         String profileJson = source.get(SOURCE_PROFILE_JSON) == null ? null : String.valueOf(source.get(SOURCE_PROFILE_JSON));
         if(profileJson == null && job.getValueInteger("savedBulkLoadProfileId") != null)
         {
            QRecord saved = GetAction.execute(SavedBulkLoadProfile.TABLE_NAME, job.getValueInteger("savedBulkLoadProfileId"));
            profileJson = saved == null ? null : saved.getValueString("mappingJson");
         }
         if(profileJson != null)
         {
            BulkLoadProfile profile = JsonUtils.toObject(profileJson, BulkLoadProfile.class);
            return (!Boolean.FALSE.equals(profile.getHasHeaderRow()));
         }
      }
      catch(QException e)
      {
         throw (e);
      }
      catch(Exception e)
      {
         throw (new QUserFacingException("Could not read the bulk load profile: " + e.getMessage()));
      }
      return (true);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String storageTableName() throws QException
   {
      String name = DurableJobsMetaData.of(QContext.getQInstance()).getStorageTableName();
      if(name == null)
      {
         throw (new QUserFacingException("Durable jobs need a storage table (DurableJobsMetaData.storageTableName) for file jobs."));
      }
      return (name);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static Map<String, Object> readRange(QRecord chunk) throws QException
   {
      String json = chunk.getValueString("rangeJson");
      if(!StringUtils.hasContent(json))
      {
         return (new LinkedHashMap<>());
      }
      try
      {
         return (JsonUtils.toObject(json, new TypeReference<>() {}));
      }
      catch(Exception e)
      {
         throw (new QException("Could not read the chunk range", e));
      }
   }




   /*******************************************************************************
    ** The default value of a process input field (from any backend step).
    *******************************************************************************/
   public static Serializable inputDefault(QProcessMetaData process, String fieldName)
   {
      if(process == null || process.getStepList() == null)
      {
         return (null);
      }
      for(var step : process.getStepList())
      {
         if(step instanceof QBackendStepMetaData backendStep
            && backendStep.getInputMetaData() != null && backendStep.getInputMetaData().getFieldList() != null)
         {
            for(var field : backendStep.getInputMetaData().getFieldList())
            {
               if(fieldName.equals(field.getName()) && field.getDefaultValue() != null)
               {
                  return (field.getDefaultValue());
               }
            }
         }
      }
      return (null);
   }

}
