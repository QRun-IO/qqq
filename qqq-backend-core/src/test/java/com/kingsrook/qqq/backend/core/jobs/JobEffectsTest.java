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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.alerts.AlertsInstanceMetaData;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectDispatcher;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectKeys;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectLease;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectRequest;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectResult;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectStatus;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectStore;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectTarget;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectTargets;
import com.kingsrook.qqq.backend.core.jobs.effects.JobEffectDeclaration;
import com.kingsrook.qqq.backend.core.jobs.effects.JobEffectType;
import com.kingsrook.qqq.backend.core.jobs.effects.JobEffects;
import com.kingsrook.qqq.backend.core.jobs.effects.ReconcileResult;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.model.LaunchRequest;
import com.kingsrook.qqq.backend.core.jobs.runtime.DurableJobsRuntime;
import com.kingsrook.qqq.backend.core.jobs.runtime.JobSignals;
import com.kingsrook.qqq.backend.core.jobs.store.DurableJobStore;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ExtractViaQueryStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.LoadViaUpdateStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static com.kingsrook.qqq.backend.core.jobs.ChunkedJobsTest.TABLE_ITEM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** The transactional outbox for side effects outside the job's database (spec
 ** section 2.13), on the memory backend: emit, outbox rows, delivery,
 ** reconcile, review, retry/fail, lease expiry, and the job's finish.
 *******************************************************************************/
class JobEffectsTest extends BaseTest
{
   static final String PROCESS_NOTIFY = "jobsTestNotifyItems";
   static final String EFFECT         = "notify";

   private QInstance       instance;
   private DurableJobStore store;
   private EffectStore     effects;



   /*******************************************************************************
    ** An instance with the items table, a process that emits one effect per
    ** item, and the stub target. Dispatchers are off: tests drive them.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws QException
   {
      StubTarget.reset();
      EffectTargets.clear();
      instance = ChunkedJobsTest.defineInstance();
      DurableJobsMetaData.of(instance)
         .withEffectTarget("stub", new QCodeReference(StubTarget.class))
         .withEffectDispatcherThreads(0)
         .withEffectReconcileAttempts(2)
         .withEffectMaxAttempts(2)
         .withEffectRetryBaseSeconds(1)
         .withEffectLeaseTtlSeconds(1);
      instance.addProcess(notifyProcess(JobEffectType.NON_IDEMPOTENT_EXTERNAL));
      AlertsInstanceMetaData.ofOrWithNew(instance).withSink(new QCodeReference(DurableJobsTestSupport.CapturingSink.class));
      QContext.init(instance, newSession());
      store = new DurableJobStore(DurableJobsMetaData.of(instance));
      effects = new EffectStore(DurableJobsMetaData.of(instance));

      List<QRecord> items = new ArrayList<>();
      for(int i = 1; i <= 5; i++)
      {
         items.add(new QRecord().withValue("code", "c" + i).withValue("name", "Item " + i).withValue("amount", i));
      }
      new InsertAction().execute(new InsertInput(TABLE_ITEM).withRecords(items));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      DurableJobsRuntime.stop(instance, Duration.ofSeconds(5));
   }



   /*******************************************************************************
    ** Chunks write one outbox row per item (deterministic keys); the job waits
    ** for delivery; the dispatcher delivers each exactly once; then it completes.
    *******************************************************************************/
   @Test
   void outboxRowsAreDeliveredOnceThenTheJobCompletes() throws Exception
   {
      Integer jobId = launch();
      DurableJobsRuntime.start(instance);
      awaitCondition("chunks done", () -> store.getChunks(jobId).stream().allMatch(c -> "DONE".equals(c.getValueString("status"))));

      List<QRecord> outbox = effectsOf(jobId);
      assertThat(outbox).hasSize(5).allSatisfy(effect ->
      {
         assertThat(effect.getValueString("status")).isEqualTo(EffectStatus.PENDING.name());
         assertThat(effect.getValueString("idempotencyKey")).isEqualTo(EffectKeys.key(jobId, effect.getValueInteger("chunkSeq"), effect.getValueString("rowKey"), EFFECT));
      });
      DurableJobs.evaluate(jobId);
      assertThat(store.getJob(jobId).getValueString("status")).as("waits for its effects").isEqualTo(JobStatus.RUNNING.name());

      dispatcher().dispatchOnce();
      assertThat(StubTarget.DELIVERIES).hasSize(5).doesNotHaveDuplicates();
      assertThat(effectsOf(jobId)).allSatisfy(effect -> assertThat(effect.getValueString("status")).isEqualTo(EffectStatus.SENT.name()));
      assertThat(store.getJob(jobId).getValueString("status")).isEqualTo(JobStatus.COMPLETED.name());

      dispatcher().dispatchOnce();
      assertThat(StubTarget.DELIVERIES).as("nothing is delivered twice").hasSize(5);
   }



   /*******************************************************************************
    ** A rerun chunk writes the same keys; the outbox skips them.
    *******************************************************************************/
   @Test
   void rewritingTheSameKeysDoesNotDouble() throws Exception
   {
      Integer       jobId = launch();
      List<QRecord> rows  = new ArrayList<>();
      for(int i = 0; i < 3; i++)
      {
         rows.add(new QRecord().withValue("jobId", jobId).withValue("chunkSeq", 0).withValue("effectName", EFFECT).withValue("effectType", JobEffectType.NON_IDEMPOTENT_EXTERNAL.name())
            .withValue("targetName", "stub").withValue("rowKey", "r1").withValue("idempotencyKey", EffectKeys.key(jobId, 0, "r1", EFFECT))
            .withValue("status", EffectStatus.PENDING.name()).withValue("needsReview", false).withValue("attempt", 0).withValue("leaseVersion", 0));
      }
      assertThat(effects.insertEffects(rows, null)).isEqualTo(1);
      assertThat(effects.insertEffects(rows, null)).isZero();
      assertThat(effectsOf(jobId).stream().filter(e -> "r1".equals(e.getValueString("rowKey")))).hasSize(1);
   }



   /*******************************************************************************
    ** UNKNOWN (non-idempotent): never resent; reconcile finds it -> SENT.
    *******************************************************************************/
   @Test
   void unknownIsReconciledNotResent() throws Exception
   {
      StubTarget.deliverOutcome = EffectResult.Outcome.UNKNOWN;
      StubTarget.reconcileOutcome = ReconcileResult.Outcome.FOUND;
      Integer jobId = runChunks();

      dispatcher().dispatchOnce();
      assertThat(effectsOf(jobId)).allSatisfy(e -> assertThat(e.getValueString("status")).isEqualTo(EffectStatus.UNKNOWN.name()));
      awaitCondition("reconciled", () ->
      {
         dispatcher().dispatchOnce();
         return (effectsOf(jobId).stream().allMatch(e -> EffectStatus.SENT.name().equals(e.getValueString("status"))));
      });
      assertThat(StubTarget.DELIVERIES).as("delivered once each, never resent").hasSize(5).doesNotHaveDuplicates();
      assertThat(StubTarget.RECONCILES).hasSize(5);
      assertThat(store.getJob(jobId).getValueString("status")).isEqualTo(JobStatus.COMPLETED.name());
   }



   /*******************************************************************************
    ** UNKNOWN that reconcile cannot resolve: queued for review, alert raised,
    ** job NEEDS_REVIEW; still never resent.
    *******************************************************************************/
   @Test
   void unresolvedGoesToReview() throws Exception
   {
      StubTarget.deliverOutcome = EffectResult.Outcome.UNKNOWN;
      StubTarget.reconcileOutcome = ReconcileResult.Outcome.UNKNOWN;
      Integer jobId = runChunks();

      awaitCondition("queued for review", () ->
      {
         dispatcher().dispatchOnce();
         return (effectsOf(jobId).stream().allMatch(e -> Boolean.TRUE.equals(e.getValueBoolean("needsReview"))));
      });
      assertThat(StubTarget.DELIVERIES).hasSize(5).doesNotHaveDuplicates();
      assertThat(effectsOf(jobId)).allSatisfy(e -> assertThat(e.getValueString("status")).isEqualTo(EffectStatus.UNKNOWN.name()));
      QRecord job = store.getJob(jobId);
      assertThat(job.getValueString("status")).isEqualTo(JobStatus.NEEDS_REVIEW.name());
      assertThat(job.getValueString("statusReason")).contains("5 effects", "may or may not have been delivered");
      assertThat(DurableJobsTestSupport.ALERTS).filteredOn(a -> JobSignals.ALERT_EFFECT_UNRESOLVED.equals(a.getAlertTypeName())).hasSize(5);

      ///////////////////////////////////////////////////////////////
      // review resolves them: the job then finishes (not resent) //
      ///////////////////////////////////////////////////////////////
      for(QRecord effect : effectsOf(jobId))
      {
         assertThat(effects.resolve(effect.getValueInteger("id"), EffectStatus.SENT, "Confirmed in the provider's console")).isTrue();
      }
      store.setJobStatus(jobId, java.util.EnumSet.of(JobStatus.NEEDS_REVIEW), JobStatus.RUNNING, Map.of());
      DurableJobs.evaluate(jobId);
      assertThat(store.getJob(jobId).getValueString("status")).isEqualTo(JobStatus.COMPLETED.name());
      assertThat(StubTarget.DELIVERIES).hasSize(5);
   }



   /*******************************************************************************
    ** UNKNOWN for an idempotent effect: simply resent with the same key.
    *******************************************************************************/
   @Test
   void idempotentUnknownIsResentWithTheSameKey() throws Exception
   {
      instance.addProcess(notifyProcess(JobEffectType.IDEMPOTENT_EXTERNAL).withName(PROCESS_NOTIFY + "Idem"));
      StubTarget.deliverOutcome = EffectResult.Outcome.UNKNOWN;
      Integer jobId = DurableJobs.launch(new LaunchRequest(PROCESS_NOTIFY + "Idem").withFilter(new QQueryFilter()));
      DurableJobsRuntime.start(instance);
      awaitCondition("chunks done", () -> store.getChunks(jobId).stream().allMatch(c -> "DONE".equals(c.getValueString("status"))));

      dispatcher().dispatchOnce();
      StubTarget.deliverOutcome = EffectResult.Outcome.SENT;
      awaitCondition("resent", () ->
      {
         dispatcher().dispatchOnce();
         return (effectsOf(jobId).stream().allMatch(e -> EffectStatus.SENT.name().equals(e.getValueString("status"))));
      });
      assertThat(StubTarget.DELIVERIES).hasSize(10);
      assertThat(StubTarget.DELIVERIES.stream().distinct()).hasSize(5);
      assertThat(StubTarget.RECONCILES).isEmpty();
   }



   /*******************************************************************************
    ** RETRY until the attempts run out, then FAILED: the job completes with
    ** errors and says so.
    *******************************************************************************/
   @Test
   void retriesThenFails() throws Exception
   {
      StubTarget.deliverOutcome = EffectResult.Outcome.RETRY;
      Integer jobId = runChunks();
      awaitCondition("failed", () ->
      {
         dispatcher().dispatchOnce();
         return (effectsOf(jobId).stream().allMatch(e -> EffectStatus.FAILED.name().equals(e.getValueString("status"))));
      });
      assertThat(StubTarget.DELIVERIES).hasSize(10);
      QRecord job = store.getJob(jobId);
      assertThat(job.getValueString("status")).isEqualTo(JobStatus.COMPLETED_WITH_ERRORS.name());
      assertThat(job.getValueString("statusReason")).contains("5 effects were not delivered");
   }



   /*******************************************************************************
    ** A dispatcher that dies holding a lease: non-idempotent -> UNKNOWN
    ** (reconcile), idempotent -> PENDING (resend). Never left SENDING.
    *******************************************************************************/
   @Test
   void expiredLeasesAreReclaimedByType() throws Exception
   {
      Integer jobId = runChunks();
      List<EffectLease> held = effects.claimDue("dead-pod", 5);
      assertThat(held).hasSize(5);
      QRecord first = effects.get(held.get(0).effectId());
      SystemScope.call(() -> new com.kingsrook.qqq.backend.core.actions.tables.UpdateAction().execute(new com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput(DurableJobsTables.TABLE_EFFECT)
         .withRecord(new QRecord().withValue("id", first.getValueInteger("id")).withValue("effectType", JobEffectType.IDEMPOTENT_EXTERNAL.name()))));

      Thread.sleep(1100);
      assertThat(effects.reapExpired()).containsExactly(jobId);
      assertThat(effects.get(first.getValueInteger("id")).getValueString("status")).isEqualTo(EffectStatus.PENDING.name());
      assertThat(effectsOf(jobId).stream().filter(e -> EffectStatus.UNKNOWN.name().equals(e.getValueString("status")))).hasSize(4);
      assertThat(StubTarget.DELIVERIES).isEmpty();

      ///////////////////////////////////////////////////////////////
      // the dead pod's late writes are refused (fenced on lease) //
      ///////////////////////////////////////////////////////////////
      assertThat(effects.markSent(held.get(1), "late")).isFalse();
   }



   /*******************************************************************************
    ** Emit rules: undeclared and LOCAL_TX effects are refused; outside a job an
    ** effect is delivered at once and anything but SENT throws.
    *******************************************************************************/
   @Test
   void emitRulesAndInlineDelivery() throws Exception
   {
      RunBackendStepInput input = new RunBackendStepInput().withProcessName(PROCESS_NOTIFY);
      assertThatThrownBy(() -> JobEffects.emit(input, "nope", "1", Map.of())).hasMessageContaining("not declared");

      DurableProcessMetaData.of(instance.getProcess(PROCESS_NOTIFY)).withEffect(new JobEffectDeclaration("local", null, null, JobEffectType.LOCAL_TX));
      assertThatThrownBy(() -> JobEffects.emit(input, "local", "1", Map.of())).hasMessageContaining("LOCAL_TX");

      String key = JobEffects.emit(input, EFFECT, "inline-1", Map.of("to", "a@example.com"));
      assertThat(key).startsWith("inline:").endsWith(":inline-1:" + EFFECT);
      assertThat(StubTarget.DELIVERIES).containsExactly(key);

      StubTarget.deliverOutcome = EffectResult.Outcome.UNKNOWN;
      assertThatThrownBy(() -> JobEffects.emit(input, EFFECT, "inline-2", Map.of())).hasMessageContaining("was not delivered");
   }



   /*******************************************************************************
    ** Declarations are validated: unknown targets, duplicates, LOCAL_TX targets.
    *******************************************************************************/
   @Test
   void declarationsAreValidated()
   {
      DurableProcessMetaData durable = DurableProcessMetaData.of(instance.getProcess(PROCESS_NOTIFY));
      durable.withEffect(new JobEffectDeclaration("other", null, "missing", JobEffectType.NON_IDEMPOTENT_EXTERNAL));
      durable.withEffect(new JobEffectDeclaration(EFFECT, null, "stub", JobEffectType.NON_IDEMPOTENT_EXTERNAL));
      durable.withEffect(new JobEffectDeclaration("local", null, "stub", JobEffectType.LOCAL_TX));
      durable.withEffect(new JobEffectDeclaration("stepless", "noSuchStep", "stub", JobEffectType.IDEMPOTENT_EXTERNAL));
      instance.setHasBeenValidated(null);
      assertThatThrownBy(() -> new QInstanceValidator().validate(instance))
         .hasMessageContaining("needs a target registered")
         .hasMessageContaining("is declared twice")
         .hasMessageContaining("has no target")
         .hasMessageContaining("which the process does not have");
   }



   /*******************************************************************************
    ** Launch the notify process over every item, chunk size 2.
    *******************************************************************************/
   private Integer launch() throws QException
   {
      return (DurableJobs.launch(new LaunchRequest(PROCESS_NOTIFY).withFilter(new QQueryFilter())));
   }



   /*******************************************************************************
    ** Launch, start the runtime, and wait until every chunk is DONE.
    *******************************************************************************/
   private Integer runChunks() throws Exception
   {
      Integer jobId = launch();
      DurableJobsRuntime.start(instance);
      awaitCondition("chunks done", () -> store.getChunks(jobId).stream().allMatch(c -> "DONE".equals(c.getValueString("status"))));
      return (jobId);
   }



   /*******************************************************************************
    ** A dispatcher for this test (no fault hook).
    *******************************************************************************/
   private EffectDispatcher dispatcher()
   {
      return (new EffectDispatcher(DurableJobsMetaData.of(instance), store, "test-dispatcher", null));
   }



   /*******************************************************************************
    ** The job's outbox rows, by id.
    *******************************************************************************/
   private List<QRecord> effectsOf(Integer jobId) throws QException
   {
      return (effects.query(new QQueryFilter(new QFilterCriteria("jobId", QCriteriaOperator.EQUALS, jobId)).withOrderBy(new QFilterOrderBy("id"))));
   }



   /*******************************************************************************
    ** Poll a condition (15s).
    *******************************************************************************/
   private static void awaitCondition(String description, java.util.concurrent.Callable<Boolean> condition) throws Exception
   {
      long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
      while(System.nanoTime() < deadline)
      {
         if(condition.call())
         {
            return;
         }
         Thread.sleep(50);
      }
      throw (new AssertionError("Timed out waiting for " + description));
   }



   /*******************************************************************************
    ** The items doubler with one declared effect per item.
    *******************************************************************************/
   static QProcessMetaData notifyProcess(JobEffectType type)
   {
      QProcessMetaData process = StreamedETLWithFrontendProcess.processMetaDataBuilder()
         .withName(PROCESS_NOTIFY)
         .withTableName(TABLE_ITEM)
         .withSourceTable(TABLE_ITEM)
         .withDestinationTable(TABLE_ITEM)
         .withExtractStepClass(ExtractViaQueryStep.class)
         .withTransformStepClass(NotifyTransform.class)
         .withLoadStepClass(LoadViaUpdateStep.class)
         .getProcessMetaData();
      process.withSupplementalMetaData(new DurableProcessMetaData().withEligible(true).withChunkSize(2)
         .withEffect(new JobEffectDeclaration(EFFECT, null, "stub", type)));
      return (process);
   }



   /*******************************************************************************
    ** Doubles each amount and emits one notify effect per item.
    *******************************************************************************/
   public static class NotifyTransform extends AbstractTransformStep
   {
      private final ProcessSummaryLine ok = new ProcessSummaryLine(Status.OK, 0, "notified");



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         for(QRecord record : input.getRecords())
         {
            output.addRecord(new QRecord().withValue("id", record.getValue("id")).withValue("amount", record.getValueInteger("amount") * 2));
            JobEffects.emit(input, EFFECT, String.valueOf(record.getValue("id")), Map.of("item", record.getValueString("code")));
            ok.incrementCountAndAddPrimaryKey(record.getValue("id"));
         }
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput output, boolean isForResultScreen)
      {
         return (new ArrayList<>(List.of(ok)));
      }
   }



   /*******************************************************************************
    ** A target that records every call and answers as configured.
    *******************************************************************************/
   public static class StubTarget implements EffectTarget
   {
      static final List<String>                       DELIVERIES = new CopyOnWriteArrayList<>();
      static final List<String>                       RECONCILES = new CopyOnWriteArrayList<>();
      static final Map<String, Serializable>           LAST       = new ConcurrentHashMap<>();
      static volatile EffectResult.Outcome    deliverOutcome   = EffectResult.Outcome.SENT;
      static volatile ReconcileResult.Outcome reconcileOutcome = ReconcileResult.Outcome.UNKNOWN;



      /*******************************************************************************
       **
       *******************************************************************************/
      static void reset()
      {
         DELIVERIES.clear();
         RECONCILES.clear();
         LAST.clear();
         deliverOutcome = EffectResult.Outcome.SENT;
         reconcileOutcome = ReconcileResult.Outcome.UNKNOWN;
         DurableJobsTestSupport.ALERTS.clear();
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public EffectResult deliver(EffectRequest request)
      {
         DELIVERIES.add(request.idempotencyKey());
         return (switch(deliverOutcome)
         {
            case SENT -> EffectResult.sent("stub-" + EffectKeys.digest(request.idempotencyKey()));
            case RETRY -> EffectResult.retry("busy");
            case FAILED -> EffectResult.failed("rejected");
            case UNKNOWN -> EffectResult.unknown("timed out after sending");
         });
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public ReconcileResult reconcile(EffectRequest request)
      {
         RECONCILES.add(request.idempotencyKey());
         return (switch(reconcileOutcome)
         {
            case FOUND -> DELIVERIES.contains(request.idempotencyKey()) ? ReconcileResult.found("stub") : ReconcileResult.notFound();
            case NOT_FOUND -> ReconcileResult.notFound();
            case UNKNOWN -> ReconcileResult.unknown("lookup unavailable");
         });
      }
   }
}
