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


import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.core.type.TypeReference;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.model.ChunkOutcome;
import com.kingsrook.qqq.backend.core.jobs.model.JobSemantics;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessState;
import com.kingsrook.qqq.backend.core.model.actions.processes.QUploadedFile;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QStepMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamed.StreamedETLProcess;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.state.StateType;
import com.kingsrook.qqq.backend.core.state.UUIDAndTypeStateKey;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** The STEPS plan (and ATOMIC): run any process's backend steps from its launch
 ** point onward, as one chunk, with frontend steps skipped (their values were
 ** collected before launch).
 **
 ** STEPS: after every backend step the process values are checkpointed (fenced)
 ** to jobs_chunk.checkpointJson, and the cooperative check runs. A reclaimed
 ** chunk resumes after the last finished step - so a three-step process killed
 ** after step 2 resumes at step 3 with step 2's values. Whether a step that was
 ** running when its pod died may rerun is the job's idempotency (the reaper sends
 ** NONE to NEEDS_REVIEW).
 **
 ** ATOMIC: no checkpoints; a reclaim reruns the process from the start (all or
 ** nothing - the process's own transaction rolls back on a crash).
 *******************************************************************************/
public class StepsJobPlan implements JobPlan
{
   static final String CHECKPOINT_LAST_STEP = "lastStep";
   static final String CHECKPOINT_VALUES    = "values";
   static final String CHECKPOINT_STEP_LIST = "stepList";

   public static final String SOURCE_START_AFTER_STEP = "startAfterStep";



   /*******************************************************************************
    ** One chunk.
    *******************************************************************************/
   @Override
   public List<QRecord> planChunks(QRecord job)
   {
      return (List.of(new QRecord().withValue("seq", 0).withValue("rangeJson", "{\"plan\":\"" + (isAtomic(job) ? "ATOMIC" : "STEPS") + "\"}")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public ChunkOutcome runChunk(ChunkContext context) throws Exception
   {
      QRecord          job     = context.getJob();
      QProcessMetaData process = QContext.getQInstance().getProcess(job.getValueString("processName"));
      if(process == null)
      {
         throw (new QException("Process " + job.getValueString("processName") + " is not defined in this instance."));
      }

      boolean                   atomic         = isAtomic(job);
      Map<String, Serializable> values         = readValues(job.getValueString("paramsJson"));
      String                    startAfterStep = readSource(job).get(SOURCE_START_AFTER_STEP) instanceof String s ? s : null;
      List<String>              stepList       = null;

      /////////////////////////////////////////////////////////////////////
      // resume from the last checkpoint (never for ATOMIC: start again) //
      /////////////////////////////////////////////////////////////////////
      String checkpointJson = context.getChunk().getValueString("checkpointJson");
      if(!atomic && StringUtils.hasContent(checkpointJson))
      {
         Map<String, Object> checkpoint = JsonUtils.toObject(checkpointJson, new TypeReference<>() {});
         startAfterStep = (String) checkpoint.get(CHECKPOINT_LAST_STEP);
         values = toSerializableMap(checkpoint.get(CHECKPOINT_VALUES));
         if(checkpoint.get(CHECKPOINT_STEP_LIST) instanceof List<?> list)
         {
            stepList = list.stream().map(String::valueOf).toList();
         }
      }

      String          processUUID = UUID.randomUUID().toString();
      RunProcessInput input       = new RunProcessInput();
      input.setProcessName(process.getName());
      input.setProcessUUID(processUUID);
      input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
      input.setValues(new LinkedHashMap<>(values));

      if(startAfterStep != null)
      {
         ///////////////////////////////////////////////////////////////////////
         // RunProcessAction continues an existing run only from stored state //
         // - seed it in this pod's state provider from the checkpoint.       //
         ///////////////////////////////////////////////////////////////////////
         ProcessState state = new ProcessState();
         state.setValues(new LinkedHashMap<>(values));
         state.setStepList(stepList != null ? new ArrayList<>(stepList) : process.getStepList().stream().map(QStepMetaData::getName).toList());
         RunProcessAction.getStateProvider().put(new UUIDAndTypeStateKey(UUID.fromString(processUUID), StateType.PROCESS_STATUS), state);
         input.setStartAfterStep(startAfterStep);
      }

      input.setBackendStepListener((stepName, processState) ->
      {
         //////////////////////////////////////////////////////////////////
         // the step's outbox effects are written before its checkpoint: //
         // a resume after this step never re-emits them (and a rerun of //
         // the step emits the same keys, which are skipped)             //
         //////////////////////////////////////////////////////////////////
         if(JobChunkScope.current().isPresent())
         {
            context.recordEffects(JobChunkScope.current().get().drainEffects());
         }
         if(!atomic)
         {
            Map<String, Object> checkpoint = new LinkedHashMap<>();
            checkpoint.put(CHECKPOINT_LAST_STEP, stepName);
            checkpoint.put(CHECKPOINT_VALUES, checkpointableValues(processState.getValues()));
            checkpoint.put(CHECKPOINT_STEP_LIST, processState.getStepList());
            context.checkpoint(JsonUtils.toJson(checkpoint));
         }
         context.check();
      });

      try(JobChunkScope scope = JobChunkScope.open(null, null, context.identity()))
      {
         context.check();
         RunProcessOutput output = new RunProcessAction().execute(input);
         if(output.getException().isPresent())
         {
            throw (output.getException().get());
         }
         context.recordEffects(scope.drainEffects());

         Integer recordCount = ValueUtils.getValueAsInteger(output.getValues().get(StreamedETLProcess.FIELD_RECORD_COUNT));
         int     rows        = recordCount == null ? 0 : recordCount;
         String  summary     = output.getValues().get(StreamedETLWithFrontendProcess.FIELD_PROCESS_SUMMARY) == null ? null : JsonUtils.toJson(output.getValues().get(StreamedETLWithFrontendProcess.FIELD_PROCESS_SUMMARY));
         return (new ChunkOutcome(rows, rows, 0, summary, outputs(output.getValues())));
      }
      finally
      {
         RunProcessAction.getStateProvider().remove(new UUIDAndTypeStateKey(UUID.fromString(processUUID), StateType.PROCESS_STATUS));
      }
   }



   /*******************************************************************************
    ** Outputs a process left for download (e.g., a report in a storage table).
    *******************************************************************************/
   private static String outputs(Map<String, Serializable> values)
   {
      if(values.get("storageTableName") != null && values.get("storageReference") != null)
      {
         Map<String, Serializable> output = new LinkedHashMap<>();
         output.put("storageTableName", values.get("storageTableName"));
         output.put("storageReference", values.get("storageReference"));
         output.put("downloadFileName", values.get("downloadFileName"));
         return (JsonUtils.toJson(List.of(output)));
      }
      return (null);
   }



   /*******************************************************************************
    ** Values worth checkpointing: everything JSON can carry, minus uploaded files
    ** (their bytes are never copied into the jobs tables).
    *******************************************************************************/
   private static Map<String, Serializable> checkpointableValues(Map<String, Serializable> values)
   {
      Map<String, Serializable> rs = new LinkedHashMap<>();
      if(values != null)
      {
         for(Map.Entry<String, Serializable> entry : values.entrySet())
         {
            if(!(entry.getValue() instanceof QUploadedFile))
            {
               rs.put(entry.getKey(), entry.getValue());
            }
         }
      }
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static boolean isAtomic(QRecord job)
   {
      return (JobSemantics.ATOMIC.name().equals(job.getValueString("semantics")));
   }



   /*******************************************************************************
    ** The job's source description (JSON object), or an empty map.
    *******************************************************************************/
   static Map<String, Object> readSource(QRecord job) throws QException
   {
      String json = job.getValueString("sourceJson");
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
         throw (new QException("Could not read the job's source", e));
      }
   }



   /*******************************************************************************
    ** The job's parameter values (JSON object), or an empty map.
    *******************************************************************************/
   public static Map<String, Serializable> readValues(String paramsJson) throws QException
   {
      if(!StringUtils.hasContent(paramsJson))
      {
         return (new LinkedHashMap<>());
      }
      try
      {
         return (toSerializableMap(JsonUtils.toObject(paramsJson, new TypeReference<Map<String, Object>>() {})));
      }
      catch(Exception e)
      {
         throw (new QException("Could not read the job's parameters", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Map<String, Serializable> toSerializableMap(Object object)
   {
      Map<String, Serializable> rs = new LinkedHashMap<>();
      if(object instanceof Map<?, ?> map)
      {
         for(Map.Entry<?, ?> entry : map.entrySet())
         {
            rs.put(String.valueOf(entry.getKey()), (Serializable) entry.getValue());
         }
      }
      return (rs);
   }
}
