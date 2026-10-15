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

package com.kingsrook.sampleapp.processes.cluster;


import java.time.Duration;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobCallback;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobManager;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.cluster.ClusterCaches;
import com.kingsrook.qqq.backend.core.cluster.QNodeLifecycle;
import com.kingsrook.qqq.backend.core.cluster.QNodeRoles;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QComponentType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendComponentMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.secrets.OneTimeSecret;
import com.kingsrook.qqq.backend.core.utils.memoization.Memoization;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;


/*******************************************************************************
 ** Small processes that show which node did the work, used to prove the sample
 ** behaves the same on one node and on several (docs/DEPLOYMENT-MODES.md).
 ** Every step reports this node's id (QNodeRoles.nodeId), so a caller can see
 ** that a wizard started on one node continued on another.
 **
 ** They are added in cluster mode (SampleClusterMode) and by the single-node
 ** acceptance test; the default sample does not register them.
 *******************************************************************************/
public final class SampleClusterProcesses
{
   public static final String PROCESS_WIZARD          = "clusterWizard";
   public static final String PROCESS_WORK            = "clusterWork";
   public static final String PROCESS_CANCEL_JOB      = "clusterCancelJob";
   public static final String PROCESS_SECRET          = "clusterSecret";
   public static final String PROCESS_CACHE_READ      = "clusterCacheRead";
   public static final String PROCESS_CACHE_INVALIDATE = "clusterCacheInvalidate";
   public static final String PROCESS_DRAIN           = "clusterDrain";

   /** the ClusterCaches name of the person first-name memo */
   public static final String CACHE_NAME = "sample.personFirstName";

   private static final Memoization<Integer, String> FIRST_NAMES = ClusterCaches.shared(CACHE_NAME,
      new Memoization<Integer, String>(Duration.ofHours(1)), Integer::valueOf);



   /*******************************************************************************
    ** not instantiable
    *******************************************************************************/
   private SampleClusterProcesses()
   {
   }



   /*******************************************************************************
    ** Add every cluster demo process to the instance.
    *******************************************************************************/
   public static void defineAll(QInstance instance)
   {
      instance.addProcess(new QProcessMetaData().withName(PROCESS_WIZARD).withLabel("Cluster Wizard")
         .withStep(backend("start", WizardStartStep.class))
         .withStep(new QFrontendStepMetaData().withName("input")
            .withComponent(new QFrontendComponentMetaData().withType(QComponentType.EDIT_FORM))
            .withFormField(new QFieldMetaData("note", QFieldType.STRING)))
         .withStep(backend("remember", WizardRememberStep.class)
            .withInputData(new QFunctionInputMetaData().withField(new QFieldMetaData("note", QFieldType.STRING))))
         .withStep(new QFrontendStepMetaData().withName("done")
            .withComponent(new QFrontendComponentMetaData().withType(QComponentType.VIEW_FORM))
            .withViewField(new QFieldMetaData("summary", QFieldType.STRING))));

      instance.addProcess(new QProcessMetaData().withName(PROCESS_WORK).withLabel("Cluster Work")
         .withStep(backend("work", WorkStep.class)
            .withInputData(new QFunctionInputMetaData().withField(new QFieldMetaData("seconds", QFieldType.INTEGER)))));

      instance.addProcess(new QProcessMetaData().withName(PROCESS_CANCEL_JOB).withLabel("Cluster Cancel Job")
         .withStep(backend("cancel", CancelJobStep.class)
            .withInputData(new QFunctionInputMetaData().withField(new QFieldMetaData("jobUUID", QFieldType.STRING)))));

      instance.addProcess(new QProcessMetaData().withName(PROCESS_SECRET).withLabel("Cluster Secret")
         .withStep(backend("issue", SecretStep.class)
            .withInputData(new QFunctionInputMetaData().withField(new QFieldMetaData("sleepMillis", QFieldType.INTEGER)))));

      instance.addProcess(new QProcessMetaData().withName(PROCESS_CACHE_READ).withLabel("Cluster Cache Read")
         .withStep(backend("read", CacheReadStep.class)
            .withInputData(new QFunctionInputMetaData().withField(new QFieldMetaData("personId", QFieldType.INTEGER)))));

      instance.addProcess(new QProcessMetaData().withName(PROCESS_CACHE_INVALIDATE).withLabel("Cluster Cache Invalidate")
         .withStep(backend("invalidate", CacheInvalidateStep.class)));

      instance.addProcess(new QProcessMetaData().withName(PROCESS_DRAIN).withLabel("Cluster Drain This Node")
         .withStep(backend("drain", DrainStep.class)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QBackendStepMetaData backend(String name, Class<? extends BackendStep> step)
   {
      return (new QBackendStepMetaData().withName(name).withCode(new QCodeReference(step)));
   }



   /*******************************************************************************
    ** First wizard step: records where it ran and a token, kept in process state.
    *******************************************************************************/
   public static class WizardStartStep implements BackendStep
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         output.addValue("startedOn", QNodeRoles.nodeId());
         output.addValue("token", UUID.randomUUID().toString());
      }
   }



   /*******************************************************************************
    ** Second wizard step: reads the first step's values back from process state.
    *******************************************************************************/
   public static class WizardRememberStep implements BackendStep
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         String token = input.getValueString("token");
         if(token == null)
         {
            throw (new QUserFacingException("The wizard's first step left no token in process state."));
         }
         output.addValue("rememberedOn", QNodeRoles.nodeId());
         output.addValue("summary", input.getValueString("startedOn") + "|" + token + "|" + input.getValueString("note"));
      }
   }



   /*******************************************************************************
    ** A long step: reports progress every 100 ms until its time is up or a cancel
    ** is requested (from any node).
    *******************************************************************************/
   public static class WorkStep implements BackendStep
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         Integer seconds = input.getValueInteger("seconds");
         int     ticks   = (seconds == null ? 30 : Math.max(0, seconds)) * 10;
         output.addValue("ranOn", QNodeRoles.nodeId());
         output.addValue("cancelled", false);

         AsyncJobCallback callback = input.getAsyncJobCallback();
         for(int tick = 0; tick < ticks; tick++)
         {
            if(callback != null)
            {
               callback.updateStatus("Working on " + QNodeRoles.nodeId(), tick, ticks);
               if(callback.wasCancelRequested())
               {
                  output.addValue("cancelled", true);
                  return;
               }
            }
            sleep(100);
         }
      }
   }



   /*******************************************************************************
    ** Request a cancel of an async job by its uuid, the way the jobs tray does,
    ** from whichever node serves this request.
    *******************************************************************************/
   public static class CancelJobStep implements BackendStep
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         String jobUUID = input.getValueString("jobUUID");
         if(jobUUID == null)
         {
            throw (new QUserFacingException("Give the jobUUID to cancel."));
         }
         new AsyncJobManager().cancelJob(jobUUID);
         output.addValue("cancelRequestedOn", QNodeRoles.nodeId());
      }
   }



   /*******************************************************************************
    ** Issue a one-time secret (after an optional pause, so the step can go async).
    *******************************************************************************/
   public static class SecretStep implements BackendStep
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         Integer sleepMillis = input.getValueInteger("sleepMillis");
         if(sleepMillis != null && sleepMillis > 0)
         {
            sleep(sleepMillis);
         }
         String value = "sample-secret-" + UUID.randomUUID();
         output.addOneTimeSecret(new OneTimeSecret().withLabel("Sample token").withValue(value).withMasked("sample-secret-****"));
         output.addValue("issuedOn", QNodeRoles.nodeId());
      }
   }



   /*******************************************************************************
    ** Read a person's first name through a cluster-registered memo.
    *******************************************************************************/
   public static class CacheReadStep implements BackendStep
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         Integer personId = input.getValueInteger("personId");
         if(personId == null)
         {
            throw (new QUserFacingException("Give the personId to read."));
         }
         String firstName = FIRST_NAMES.getResultThrowing(personId, id ->
         {
            QRecord person = GetAction.execute(SampleMetaDataProvider.TABLE_NAME_PERSON, id);
            return (person == null ? null : person.getValueString("firstName"));
         }).orElse(null);
         output.addValue("firstName", firstName);
         output.addValue("readOn", QNodeRoles.nodeId());
      }
   }



   /*******************************************************************************
    ** Invalidate the memo on every node (ClusterBus memo-invalidate event).
    *******************************************************************************/
   public static class CacheInvalidateStep implements BackendStep
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         ClusterCaches.invalidate(CACHE_NAME);
         output.addValue("invalidatedOn", QNodeRoles.nodeId());
      }
   }



   /*******************************************************************************
    ** Start draining this node (as a drain request from qbit-cluster would).
    *******************************************************************************/
   public static class DrainStep implements BackendStep
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         QNodeLifecycle.startDraining("sample drain request");
         output.addValue("drainingNode", QNodeRoles.nodeId());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void sleep(long millis) throws QException
   {
      try
      {
         Thread.sleep(millis);
      }
      catch(InterruptedException e)
      {
         Thread.currentThread().interrupt();
         throw (new QException("Interrupted", e));
      }
   }
}
