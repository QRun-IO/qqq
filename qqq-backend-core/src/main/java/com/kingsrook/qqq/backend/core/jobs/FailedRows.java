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


package com.kingsrook.qqq.backend.core.jobs;


import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.processes.ProcessFileDownload;
import com.kingsrook.qqq.backend.core.actions.tables.ConditionalUpdateAction;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.jobs.model.JobSourceType;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.model.LaunchRequest;
import com.kingsrook.qqq.backend.core.jobs.plans.ChunkedProcessPlan;
import com.kingsrook.qqq.backend.core.jobs.plans.StepsJobPlan;
import com.kingsrook.qqq.backend.core.jobs.store.DurableJobStore;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.ConditionalUpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.filehandling.FileToRowsInterface;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadFileRow;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.TABLE_CHUNK;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.TABLE_ROW_ERROR;


/*******************************************************************************
 ** The failed rows of a job (spec section 2.7):
 ** - retry them as a child job (ROW_SET) over just those rows, with the same
 **   inputs or a corrected bulk-load profile; the row errors get retryJobId;
 ** - download them as CSV (the original columns plus an error column) to fix
 **   in a spreadsheet and load again.
 *******************************************************************************/
public final class FailedRows
{
   private static final EnumSet<JobStatus> FINISHED_WITH_FAILURES = EnumSet.of(JobStatus.COMPLETED_WITH_ERRORS, JobStatus.FAILED, JobStatus.NEEDS_REVIEW, JobStatus.CANCELLED);



   /*******************************************************************************
    ** not instantiable
    *******************************************************************************/
   private FailedRows()
   {
   }



   /*******************************************************************************
    ** Launch a child job over a job's unresolved failed rows. Returns the new
    ** job's id. A corrected profile (JSON or saved id) replaces the parent's.
    *******************************************************************************/
   public static Integer retry(QRecord job, String correctedProfileJson, Integer correctedSavedProfileId) throws QException
   {
      Integer       jobId  = job.getValueInteger("id");
      List<QRecord> errors = unresolved(jobId);
      if(errors.isEmpty())
      {
         throw (new QUserFacingException(job.getValueString("label") + " has no failed rows to retry."));
      }
      if(!FINISHED_WITH_FAILURES.contains(JobStatus.valueOf(job.getValueString("status"))))
      {
         throw (new QUserFacingException(job.getValueString("label") + " is still running; retry its failed rows when it has finished."));
      }

      Map<String, Object> parentSource = sourceOf(job);
      boolean             fileRows     = errors.stream().anyMatch(e -> e.getValue("rowNumber") != null);
      Map<String, Object> rowSet       = new LinkedHashMap<>();
      rowSet.put(ChunkedProcessPlan.SOURCE_PARENT_JOB_ID, jobId);
      rowSet.put(ChunkedProcessPlan.SOURCE_PARENT_TYPE, fileRows ? JobSourceType.STORAGE_FILE.name() : JobSourceType.TABLE_FILTER.name());
      if(fileRows)
      {
         Set<Integer> rows = new LinkedHashSet<>();
         errors.forEach(e -> rows.add(e.getValueInteger("rowNumber")));
         rowSet.put(ChunkedProcessPlan.SOURCE_ROW_NUMBERS, new ArrayList<>(rows));
      }
      else
      {
         Set<String> pks = new LinkedHashSet<>();
         errors.forEach(e -> pks.add(e.getValueString("recordPk")));
         rowSet.put(ChunkedProcessPlan.SOURCE_RECORD_PKS, new ArrayList<>(pks));
      }

      String  profileJson    = StringUtils.hasContent(correctedProfileJson) ? correctedProfileJson : (String) parentSource.get(ChunkedProcessPlan.SOURCE_PROFILE_JSON);
      Integer savedProfileId = correctedSavedProfileId != null ? correctedSavedProfileId : (StringUtils.hasContent(correctedProfileJson) ? null : job.getValueInteger("savedBulkLoadProfileId"));
      if(profileJson != null && savedProfileId == null)
      {
         rowSet.put(ChunkedProcessPlan.SOURCE_PROFILE_JSON, profileJson);
      }

      LaunchRequest request = new LaunchRequest(job.getValueString("processName"))
         .withValues(StepsJobPlan.readValues(job.getValueString("paramsJson")))
         .withRowSetSource(rowSet)
         .withSavedBulkLoadProfileId(savedProfileId)
         .withPriority(job.getValueInteger("priority"))
         .withNotifyEmail(job.getValueBoolean("notifyEmail"))
         .withParentJobId(jobId)
         .withLabel("Retry of " + job.getValueString("label"));
      Integer childId = DurableJobs.launch(request);

      ///////////////////////////////////////////////////
      // point the row errors at the job retrying them //
      ///////////////////////////////////////////////////
      SystemScope.call(() -> new ConditionalUpdateAction().execute(new ConditionalUpdateInput(TABLE_ROW_ERROR)
         .withValue("retryJobId", childId)
         .withFilter(new QQueryFilter()
            .withCriteria(new QFilterCriteria("jobId", QCriteriaOperator.EQUALS, jobId))
            .withCriteria(new QFilterCriteria("resolved", QCriteriaOperator.NOT_EQUALS_OR_IS_NULL, true)))));
      return (childId);
   }



   /*******************************************************************************
    ** A retry job finished: its parent's rows that did not fail again are
    ** resolved.
    *******************************************************************************/
   public static void resolveAfterRetry(QRecord retryJob) throws QException
   {
      Integer parentJobId = retryJob.getValueInteger("parentJobId");
      if(parentJobId == null || !JobSourceType.ROW_SET.name().equals(retryJob.getValueString("sourceType")))
      {
         return;
      }

      Set<String> stillFailing = new LinkedHashSet<>();
      for(QRecord error : rowErrors(retryJob.getValueInteger("id")))
      {
         stillFailing.add(key(error));
      }
      for(QRecord error : rowErrors(parentJobId))
      {
         if(retryJob.getValueInteger("id").equals(error.getValueInteger("retryJobId")) && !stillFailing.contains(key(error)))
         {
            SystemScope.call(() -> new ConditionalUpdateAction().execute(new ConditionalUpdateInput(TABLE_ROW_ERROR)
               .withValue("resolved", true)
               .withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, error.getValueInteger("id"))))));
         }
      }
   }



   /*******************************************************************************
    ** Write a job's failed rows as CSV to the jobs storage table and grant the
    ** current session its download. Returns {downloadFileName, storageTableName,
    ** storageReference} - the standard process download values.
    *******************************************************************************/
   public static Map<String, Serializable> download(QRecord job) throws QException
   {
      Integer       jobId  = job.getValueInteger("id");
      List<QRecord> errors = rowErrors(jobId);
      if(errors.isEmpty())
      {
         throw (new QUserFacingException(job.getValueString("label") + " has no failed rows."));
      }
      String storageTable = DurableJobs.settings().getStorageTableName();
      if(storageTable == null)
      {
         throw (new QUserFacingException("Durable jobs need a storage table to write downloads."));
      }

      Map<Integer, String> errorByRow = new LinkedHashMap<>();
      Map<String, String>  errorByPk  = new LinkedHashMap<>();
      for(QRecord error : errors)
      {
         String message = (error.getValueString("fieldName") == null ? "" : error.getValueString("fieldName") + ": ") + error.getValueString("message");
         if(error.getValue("rowNumber") != null)
         {
            errorByRow.merge(error.getValueInteger("rowNumber"), message, (a, b) -> a + "; " + b);
         }
         else
         {
            errorByPk.merge(String.valueOf(error.getValue("recordPk")), message, (a, b) -> a + "; " + b);
         }
      }

      String       stamp     = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC).format(Instant.now());
      String       reference = "jobs/" + jobId + "/failed-rows-" + stamp + ".csv";
      StorageInput output    = new StorageInput(storageTable).withReference(reference).withContentType("text/csv");
      try(OutputStream outputStream = SystemScope.call(() -> new StorageAction().createOutputStream(output)))
      {
         if(!errorByRow.isEmpty())
         {
            writeFileRows(jobId, storageTable, errorByRow, outputStream);
         }
         else
         {
            writeLine(outputStream, List.of("Record Key", "Error"));
            for(Map.Entry<String, String> entry : errorByPk.entrySet())
            {
               writeLine(outputStream, List.of(entry.getKey(), entry.getValue()));
            }
         }
      }
      catch(QException e)
      {
         throw (e);
      }
      catch(Exception e)
      {
         throw (new QException("Could not write the failed rows", e));
      }

      ProcessFileDownload.registerStorage(output);
      Map<String, Serializable> values = new LinkedHashMap<>();
      values.put("downloadFileName", StringUtils.safeTruncate(job.getValueString("label"), 100).replaceAll("[^A-Za-z0-9._ -]", "_") + " - failed rows.csv");
      values.put("storageTableName", storageTable);
      values.put("storageReference", reference);
      return (values);
   }



   /*******************************************************************************
    ** The original header and failed rows from the job's chunk objects, plus an
    ** Error column.
    *******************************************************************************/
   private static void writeFileRows(Integer jobId, String storageTable, Map<Integer, String> errorByRow, OutputStream outputStream) throws Exception
   {
      boolean wroteHeader = false;
      for(QRecord chunk : new DurableJobStore(DurableJobs.settings()).query(TABLE_CHUNK, new QQueryFilter(new QFilterCriteria("jobId", QCriteriaOperator.EQUALS, jobId)).withOrderBy(new QFilterOrderBy("seq"))))
      {
         String objectRef = chunk.getValueString("inputStorageRef");
         if(objectRef == null)
         {
            continue;
         }
         StorageInput input = new StorageInput(storageTable).withReference(objectRef);
         try(InputStream inputStream = SystemScope.call(() -> new StorageAction().getInputStream(input)); FileToRowsInterface rows = FileToRowsInterface.forFile(objectRef, inputStream))
         {
            boolean first = true;
            while(rows.hasNext())
            {
               BulkLoadFileRow row = rows.next();
               if(first && !errorByRow.containsKey(row.getRowNo()))
               {
                  ///////////////////////////////////////////////////////////
                  // the first line of a chunk object is its header row   //
                  ///////////////////////////////////////////////////////////
                  if(!wroteHeader)
                  {
                     List<String> header = values(row);
                     header.add("Error");
                     writeLine(outputStream, header);
                     wroteHeader = true;
                  }
               }
               else if(errorByRow.containsKey(row.getRowNo()))
               {
                  List<String> line = values(row);
                  line.add(errorByRow.get(row.getRowNo()));
                  writeLine(outputStream, line);
               }
               first = false;
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<String> values(BulkLoadFileRow row)
   {
      List<String> values = new ArrayList<>();
      for(int i = 0; i < row.size(); i++)
      {
         values.add(ValueUtils.getValueAsString(row.getValueElseNull(i)));
      }
      return (values);
   }



   /*******************************************************************************
    ** One CSV line (RFC 4180 quoting).
    *******************************************************************************/
   private static void writeLine(OutputStream outputStream, List<String> values) throws Exception
   {
      StringBuilder line = new StringBuilder();
      for(int i = 0; i < values.size(); i++)
      {
         if(i > 0)
         {
            line.append(',');
         }
         String value = values.get(i) == null ? "" : values.get(i);
         if(value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r"))
         {
            value = "\"" + value.replace("\"", "\"\"") + "\"";
         }
         line.append(value);
      }
      line.append("\r\n");
      outputStream.write(line.toString().getBytes(StandardCharsets.UTF_8));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<QRecord> unresolved(Integer jobId) throws QException
   {
      return (new DurableJobStore(DurableJobs.settings()).query(TABLE_ROW_ERROR, new QQueryFilter()
         .withCriteria(new QFilterCriteria("jobId", QCriteriaOperator.EQUALS, jobId))
         .withCriteria(new QFilterCriteria("resolved", QCriteriaOperator.NOT_EQUALS_OR_IS_NULL, true))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<QRecord> rowErrors(Integer jobId) throws QException
   {
      return (new DurableJobStore(DurableJobs.settings()).query(TABLE_ROW_ERROR, new QQueryFilter(new QFilterCriteria("jobId", QCriteriaOperator.EQUALS, jobId)).withOrderBy(new QFilterOrderBy("id"))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String key(QRecord error)
   {
      return (error.getValue("rowNumber") != null ? "r" + error.getValueInteger("rowNumber") : "k" + error.getValueString("recordPk"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Map<String, Object> sourceOf(QRecord job) throws QException
   {
      try
      {
         return (job.getValueString("sourceJson") == null ? new LinkedHashMap<>() : com.kingsrook.qqq.backend.core.utils.JsonUtils.toObject(job.getValueString("sourceJson"), new com.fasterxml.jackson.core.type.TypeReference<>() {}));
      }
      catch(Exception e)
      {
         throw (new QException("Could not read the job's source", e));
      }
   }

}
