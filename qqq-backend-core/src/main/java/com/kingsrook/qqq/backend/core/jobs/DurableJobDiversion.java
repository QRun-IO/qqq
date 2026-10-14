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


import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.model.LaunchRequest;
import com.kingsrook.qqq.backend.core.jobs.plans.JobChunkScope;
import com.kingsrook.qqq.backend.core.jobs.uploads.UploadRefs;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.QUploadedFile;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamed.StreamedETLProcess;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractExtractStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** "Automatically" (spec section 2.4): when an eligible streamed ETL process
 ** reaches its execute step with more rows than durableThresholdRows (default
 ** 1,000), or is alwaysDurable, the execute step is not run inline: the process
 ** is launched as a durable job with the values collected so far, and the step
 ** returns at once with the value {@code durableJobId} and a summary line that
 ** says the work is running in the background. Clients link to the job.
 **
 ** Never inside a job (a chunk runs the same process steps), and never for the
 ** system user. A process value {@code durableRunInline=true} keeps the old
 ** inline behaviour for one run.
 *******************************************************************************/
public final class DurableJobDiversion
{
   private static final QLogger LOG = QLogger.getLogger(DurableJobDiversion.class);

   public static final String VALUE_DURABLE_JOB_ID = "durableJobId";
   public static final String VALUE_RUN_INLINE     = "durableRunInline";

   private static final Set<String> NOT_CARRIED = Set.of("theFile", "bulkInsertMapping", "bulkLoadProfile", "savedBulkLoadProfileRecord", "queryFilterJson", "recordIds", "filterJSON",
      StreamedETLWithFrontendProcess.FIELD_PROCESS_SUMMARY, StreamedETLWithFrontendProcess.FIELD_VALIDATION_SUMMARY);



   /*******************************************************************************
    ** not instantiable
    *******************************************************************************/
   private DurableJobDiversion()
   {
   }



   /*******************************************************************************
    ** If this backend step should become a durable job, launch it and return
    ** the step's output; else empty (run the step as usual).
    *******************************************************************************/
   public static Optional<RunBackendStepOutput> maybeDivert(RunBackendStepInput input, QProcessMetaData process, QBackendStepMetaData step) throws QException
   {
      if(!StreamedETLWithFrontendProcess.STEP_NAME_EXECUTE.equals(step.getName()) || DurableJobsMetaData.of(QContext.getQInstance()) == null)
      {
         return (Optional.empty());
      }
      if(JobChunkScope.current().isPresent() || QContext.getQSession() == null || QContext.getQSession() instanceof QSystemUserSession || DurableJobsSecurity.userId(QContext.getQSession()) == null)
      {
         return (Optional.empty());
      }
      if(Boolean.TRUE.equals(input.getValueBoolean(VALUE_RUN_INLINE)) || !DurableJobs.isEligible(process))
      {
         return (Optional.empty());
      }

      DurableJobsMetaData    settings  = DurableJobsMetaData.of(QContext.getQInstance());
      DurableProcessMetaData durable   = DurableProcessMetaData.of(process);
      boolean                always    = durable != null && Boolean.TRUE.equals(durable.getAlwaysDurable());
      int                    threshold = durable != null && durable.getDurableThresholdRows() != null ? durable.getDurableThresholdRows() : settings.getDurableThresholdRows();
      Integer                count     = input.getValueInteger(StreamedETLProcess.FIELD_RECORD_COUNT);
      if(count == null && !always)
      {
         count = countWithExtractStep(input);
      }
      if(!always && (count == null || count <= threshold))
      {
         return (Optional.empty());
      }

      LaunchRequest request = new LaunchRequest(process.getName())
         .withValues(carriedValues(input.getValues()))
         .withExpectedRows(count == null ? null : count.longValue())
         .withLabel(process.getLabel());

      ////////////////////////////////////////////////////////////////////
      // where the rows come from: an uploaded file, or a records filter //
      ////////////////////////////////////////////////////////////////////
      if(input.getValue("theFile") instanceof List<?> files && !files.isEmpty() && files.get(0) instanceof StorageInput file)
      {
         String fileName = StringUtils.hasContent(file.getReference()) ? file.getReference().substring(file.getReference().lastIndexOf('/') + 1) : "upload.csv";
         request.withUploadRefs(List.of(UploadRefs.register(fileName, file).ref()));
         if(input.getValueInteger("savedBulkLoadProfileId") != null)
         {
            request.withSavedBulkLoadProfileId(input.getValueInteger("savedBulkLoadProfileId"));
         }
         if(input.getValue("bulkLoadProfile") != null)
         {
            request.withProfileJson(JsonUtils.toJson(input.getValue("bulkLoadProfile")));
         }
      }
      else
      {
         ////////////////////////////////////////////////////////////////////
         // the same precedence ExtractViaQueryStep uses for its records  //
         ////////////////////////////////////////////////////////////////////
         String filterJson = StringUtils.hasContent(input.getValueString("queryFilterJson")) ? input.getValueString("queryFilterJson") : input.getValueString("filterJSON");
         if(StringUtils.hasContent(filterJson))
         {
            try
            {
               request.withFilter(JsonUtils.toObject(filterJson, QQueryFilter.class));
            }
            catch(Exception e)
            {
               throw (new QException("Could not read the process's records filter to run it as a job", e));
            }
         }
         else if(StringUtils.hasContent(input.getValueString("recordIds")))
         {
            request.withRecordIds(new ArrayList<>(List.of((Serializable[]) input.getValueString("recordIds").split(","))));
         }
      }

      Integer jobId = DurableJobs.launch(request);
      LOG.info("Running a process as a durable job (over the threshold)", logPair("processName", process.getName()), logPair("jobId", jobId), logPair("rows", count));

      RunBackendStepOutput output = new RunBackendStepOutput();
      output.seedFromRequest(input);
      output.addValue(VALUE_DURABLE_JOB_ID, jobId);
      ArrayList<ProcessSummaryLineInterface> lines = new ArrayList<>();
      lines.add(new ProcessSummaryLine(Status.INFO, null, "This is running as a background job (job " + jobId + ")"
         + (count == null ? "" : " over " + String.format("%,d", count) + " rows")
         + ". You can leave this page; you'll be notified when it finishes."));
      output.addValue(StreamedETLWithFrontendProcess.FIELD_PROCESS_SUMMARY, lines);
      return (Optional.of(output));
   }



   /*******************************************************************************
    ** Values the job carries: everything the steps collected, minus uploaded
    ** bytes, the source (carried separately) and earlier summaries.
    *******************************************************************************/
   private static Map<String, Serializable> carriedValues(Map<String, Serializable> values)
   {
      Map<String, Serializable> rs = new LinkedHashMap<>();
      values.forEach((k, v) ->
      {
         if(!NOT_CARRIED.contains(k) && !(v instanceof QUploadedFile))
         {
            rs.put(k, v);
         }
      });
      return (rs);
   }




   /*******************************************************************************
    ** The extract step's own count, when the preview did not count (it also
    ** leaves the extract's filter in queryFilterJson).
    *******************************************************************************/
   private static Integer countWithExtractStep(RunBackendStepInput input)
   {
      try
      {
         if(input.getValue(StreamedETLWithFrontendProcess.FIELD_EXTRACT_CODE) instanceof QCodeReference codeReference)
         {
            AbstractExtractStep  extractStep = QCodeLoader.getAdHoc(AbstractExtractStep.class, codeReference);
            RunBackendStepOutput scratch     = new RunBackendStepOutput();
            scratch.seedFromRequest(input);
            extractStep.preRun(input, scratch);
            return (extractStep.doCount(input));
         }
      }
      catch(Exception e)
      {
         LOG.debug("Could not count rows for the durable-jobs threshold", e);
      }
      return (null);
   }

}
