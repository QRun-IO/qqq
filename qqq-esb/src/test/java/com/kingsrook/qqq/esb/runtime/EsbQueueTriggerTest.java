/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.esb.runtime;


import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeType;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.esb.envelope.EsbEvent;
import com.kingsrook.qqq.esb.envelope.EsbEventFactory;
import com.kingsrook.qqq.esb.model.EsbProcessMetaData;
import com.kingsrook.qqq.esb.model.EsbTrigger;
import com.kingsrook.qqq.esb.stats.EsbCounterSnapshot;
import com.kingsrook.qqq.esb.stats.EsbStats;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Unit test for queue triggers in QEsbRuntime (and the runner's lifecycle):
 ** one run per message, concurrency, the run context, pause / resume /
 ** restart, and connecting when the broker is (or goes) down.
 *******************************************************************************/
class EsbQueueTriggerTest extends EsbRuntimeTestBase
{

   /*******************************************************************************
    ** Each message on the queue runs the process once, with that message's event
    ** as its only esbMessages entry; the counters follow.
    *******************************************************************************/
   @Test
   void queueTriggerRunsOncePerMessage() throws Exception
   {
      defineInstanceWithTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME));

      List<String> sentIds = new ArrayList<>();
      for(int i = 0; i < 5; i++)
      {
         sentIds.add(sendEvent(QUEUE_NAME, Map.of("n", i)).getId());
      }

      QEsbRuntime runtime = startRuntime(QContext.getQInstance());
      waitFor("5 runs", () -> RecordingStep.getCompletedRuns().size() == 5);
      pause(500);

      List<RecordedRun> runs = RecordingStep.getRuns();
      assertThat(runs).hasSize(5);
      assertThat(runs).allSatisfy(run -> assertThat(run.getEvents()).hasSize(1));
      assertThat(runs.stream().map(run -> run.getEvents().get(0).getId()).toList()).containsExactlyInAnyOrderElementsOf(sentIds);
      assertThat(runs.get(0).getEvents().get(0).getData()).containsEntry("n", 0);

      waitFor("the counters to settle", () -> EsbStats.getInstance().trigger(QUEUE_TRIGGER_NAME).succeeded() == 5);
      EsbCounterSnapshot counters = EsbStats.getInstance().trigger(QUEUE_TRIGGER_NAME);
      assertThat(counters.consumed()).isEqualTo(5);
      assertThat(counters.failed()).isZero();
      assertThat(counters.inFlight()).isZero();
      assertThat(runtime.getRunner(QUEUE_TRIGGER_NAME).getState()).isEqualTo(EsbTriggerState.RUNNING);
   }



   /*******************************************************************************
    ** With concurrency 3, three slow messages run at the same time.
    *******************************************************************************/
   @Test
   void concurrencyThreeRunsThreeInParallel() throws Exception
   {
      defineInstanceWithTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME).withConcurrency(3));
      RecordingStep.slowNextRuns(3, 1500L);

      QEsbRuntime runtime = startRuntime(QContext.getQInstance());
      waitForState(runtime, QUEUE_TRIGGER_NAME, EsbTriggerState.RUNNING);

      Instant start = Instant.now();
      for(int i = 0; i < 3; i++)
      {
         sendEvent(QUEUE_NAME, Map.of("n", i));
      }

      waitFor("3 runs", () -> RecordingStep.getCompletedRuns().size() == 3);
      assertThat(RecordingStep.getMaxConcurrentRuns()).isEqualTo(3);
      assertThat(Duration.between(start, Instant.now())).isLessThan(Duration.ofMillis(3 * 1500));
   }



   /*******************************************************************************
    ** In a SINGLE-mode run, EsbCausation is the triggering event's id - and events
    ** made during the run carry it as their causation id.
    *******************************************************************************/
   @Test
   void singleRunSetsCausationToTheEventId() throws Exception
   {
      defineInstanceWithTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME));
      EsbEvent sent = sendEvent(QUEUE_NAME, Map.of());

      startRuntime(QContext.getQInstance());
      waitFor("1 run", () -> RecordingStep.getCompletedRuns().size() == 1);

      assertThat(RecordingStep.getRuns().get(0).getCausationId()).isEqualTo(sent.getId());
   }



   /*******************************************************************************
    ** A process whose table is the event's table gets a callback filter on the
    ** events' primary keys (subjects), so a step that takes that table's records
    ** gets the changed records.
    *******************************************************************************/
   @Test
   void tableBoundProcessReceivesCallbackFilter() throws Exception
   {
      QInstance qInstance = defineInstanceWithDestinations(PROVIDER_NAME);
      qInstance.addProcess(defineRecordingProcess(TABLE_PROCESS_NAME, TABLE_NAME_ORDER, true)
         .withSupplementalMetaData(new EsbProcessMetaData().withTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME))));
      QContext.init(qInstance, new QSystemUserSession());

      List<QRecord> inserted = new InsertAction().execute(new InsertInput(TABLE_NAME_ORDER).withRecords(List.of(
         new QRecord().withValue("orderNo", "A-1"),
         new QRecord().withValue("orderNo", "A-2"),
         new QRecord().withValue("orderNo", "A-3")))).getRecords();
      QRecord second = inserted.get(1);

      EsbEvent event = EsbEventFactory.forRecordChange("test", TABLE_NAME_ORDER, RecordChangeType.INSERT, second, null);
      assertThat(event.getSubject()).isEqualTo(String.valueOf(second.getValueInteger("id")));
      sendEvent(QUEUE_NAME, event);

      startRuntime(qInstance);
      waitFor("1 run", () -> RecordingStep.getCompletedRuns().size() == 1);

      RecordedRun run = RecordingStep.getRuns().get(0);
      assertThat(run.getCallbackFilter()).isNotNull();
      QFilterCriteria criteria = run.getCallbackFilter().getCriteria().get(0);
      assertThat(criteria.getFieldName()).isEqualTo("id");
      assertThat(criteria.getOperator()).isEqualTo(QCriteriaOperator.IN);
      assertThat(criteria.getValues()).containsExactly(second.getValueInteger("id"));

      assertThat(run.getRecords()).hasSize(1);
      assertThat(run.getRecords().get(0).getValueString("orderNo")).isEqualTo("A-2");
   }



   /*******************************************************************************
    ** Runs use the system user session by default, or the trigger's run-as
    ** session; either way the frontend step before the backend step is skipped.
    *******************************************************************************/
   @Test
   void runsAsSystemUserByDefaultOrAsTheRunAsSession() throws Exception
   {
      QInstance qInstance = defineInstanceWithDestinations(PROVIDER_NAME);
      qInstance.addProcess(defineRecordingProcess(PROCESS_NAME, null, false)
         .withSupplementalMetaData(new EsbProcessMetaData()
            .withTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME))
            .withTrigger(new EsbTrigger().withDestinationName(TOPIC_NAME).withRunAsSessionSupplier(new QCodeReference(CustomSessionSupplier.class)))));
      QContext.init(qInstance, new QSystemUserSession());

      QEsbRuntime runtime = startRuntime(qInstance);
      waitForState(runtime, TOPIC_TRIGGER_NAME, EsbTriggerState.RUNNING);

      sendEvent(QUEUE_NAME, Map.of("via", "queue"));
      waitFor("the queue run", () -> RecordingStep.getCompletedRuns().size() == 1);
      sendEvent(TOPIC_NAME, Map.of("via", "topic"));
      waitFor("the topic run", () -> RecordingStep.getCompletedRuns().size() == 2);

      List<RecordedRun> runs = RecordingStep.getRuns();
      assertThat(runs.get(0).getSession()).isInstanceOf(QSystemUserSession.class);
      assertThat(runs.get(1).getSession()).isNotInstanceOf(QSystemUserSession.class);
      assertThat(runs.get(1).getSession().getUser().getIdReference()).isEqualTo(CUSTOM_SESSION_USER_ID);
      assertThat(runs.get(1).getQInstance()).isSameAs(qInstance);
   }



   /*******************************************************************************
    ** A message sent while the runtime was stopped is processed when it starts
    ** again (the runtime can be restarted).
    *******************************************************************************/
   @Test
   void messageSentWhileStoppedIsProcessedAfterStart() throws Exception
   {
      defineInstanceWithTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME));

      QEsbRuntime runtime = startRuntime(QContext.getQInstance());
      waitForState(runtime, QUEUE_TRIGGER_NAME, EsbTriggerState.RUNNING);
      runtime.stop();
      assertThat(runtime.isRunning()).isFalse();
      assertThat(runtime.getRunner(QUEUE_TRIGGER_NAME).getState()).isEqualTo(EsbTriggerState.STOPPED);

      EsbEvent sent = sendEvent(QUEUE_NAME, Map.of());
      pause(500);
      assertThat(RecordingStep.getRuns()).isEmpty();

      runtime.start(QContext.getQInstance());
      waitFor("1 run", () -> RecordingStep.getCompletedRuns().size() == 1);
      assertThat(RecordingStep.getRuns().get(0).getEvents().get(0).getId()).isEqualTo(sent.getId());
   }



   /*******************************************************************************
    ** startPaused starts without consuming; resume consumes; pause stops
    ** consuming; restart keeps a paused trigger paused, and rebuilds a running
    ** trigger's consumers.
    *******************************************************************************/
   @Test
   void startPausedAndPauseResumeRestart() throws Exception
   {
      defineInstanceWithTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME).withStartPaused(true));

      QEsbRuntime      runtime = startRuntime(QContext.getQInstance());
      EsbTriggerRunner runner  = runtime.getRunner(QUEUE_TRIGGER_NAME);
      assertThat(runner.getState()).isEqualTo(EsbTriggerState.PAUSED);

      sendEvent(QUEUE_NAME, Map.of("n", 1));
      pause(1500);
      assertThat(RecordingStep.getRuns()).isEmpty();

      runner.resumeLocal();
      waitFor("the first run", () -> RecordingStep.getCompletedRuns().size() == 1);
      waitForState(runtime, QUEUE_TRIGGER_NAME, EsbTriggerState.RUNNING);

      runner.pauseLocal();
      assertThat(runner.getState()).isEqualTo(EsbTriggerState.PAUSED);
      pause(1500);
      sendEvent(QUEUE_NAME, Map.of("n", 2));
      pause(1500);
      assertThat(RecordingStep.getRuns()).hasSize(1);

      runner.restartLocal();
      pause(500);
      assertThat(runner.getState()).isEqualTo(EsbTriggerState.PAUSED);
      assertThat(RecordingStep.getRuns()).hasSize(1);

      runner.resumeLocal();
      waitFor("the second run", () -> RecordingStep.getCompletedRuns().size() == 2);

      runner.restartLocal();
      waitForState(runtime, QUEUE_TRIGGER_NAME, EsbTriggerState.RUNNING);
      sendEvent(QUEUE_NAME, Map.of("n", 3));
      waitFor("the third run", () -> RecordingStep.getCompletedRuns().size() == 3);

      /////////////////////////////////////////////////////////////
      // resume when not paused, and pause twice, change nothing //
      /////////////////////////////////////////////////////////////
      runner.resumeLocal();
      assertThat(runner.getState()).isEqualTo(EsbTriggerState.RUNNING);
      runner.pauseLocal();
      runner.pauseLocal();
      assertThat(runner.getState()).isEqualTo(EsbTriggerState.PAUSED);
   }



   /*******************************************************************************
    ** With the broker down, start returns at once and the trigger waits in
    ** CONNECTING; when the broker comes up, it connects and consumes.
    *******************************************************************************/
   @Test
   void startDoesNotBlockWhenBrokerIsDown() throws Exception
   {
      defineInstanceWithTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME).withConcurrency(2));
      stopEmbeddedBroker();

      Instant     start   = Instant.now();
      QEsbRuntime runtime = startRuntime(QContext.getQInstance());
      assertThat(Duration.between(start, Instant.now())).isLessThan(Duration.ofSeconds(1));
      assertThat(runtime.isRunning()).isFalse();
      assertThat(runtime.getRunner(QUEUE_TRIGGER_NAME).getState()).isEqualTo(EsbTriggerState.CONNECTING);

      pause(500);
      assertThat(runtime.getRunner(QUEUE_TRIGGER_NAME).getState()).isEqualTo(EsbTriggerState.CONNECTING);

      startEmbeddedBroker();
      waitForState(runtime, QUEUE_TRIGGER_NAME, EsbTriggerState.RUNNING);
      waitFor("runtime control listener after broker starts", runtime::isRunning);

      sendEvent(QUEUE_NAME, Map.of());
      waitFor("1 run", () -> RecordingStep.getCompletedRuns().size() == 1);
   }



   /*******************************************************************************
    ** When the broker restarts, the trigger goes to CONNECTING, then rebuilds its
    ** consumers after the reconnect and consumes again.
    *******************************************************************************/
   @Test
   void reconnectsAfterBrokerRestart() throws Exception
   {
      defineInstanceWithTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME));

      QEsbRuntime runtime = startRuntime(QContext.getQInstance());
      waitForState(runtime, QUEUE_TRIGGER_NAME, EsbTriggerState.RUNNING);

      stopEmbeddedBroker();
      waitForState(runtime, QUEUE_TRIGGER_NAME, EsbTriggerState.CONNECTING);

      startEmbeddedBroker();
      waitForState(runtime, QUEUE_TRIGGER_NAME, EsbTriggerState.RUNNING);

      sendEvent(QUEUE_NAME, Map.of());
      waitFor("1 run", () -> RecordingStep.getCompletedRuns().size() == 1);
   }



   /*******************************************************************************
    ** Runner lookup and details; start and stop are idempotent; an instance
    ** without ESB triggers starts with no runners; getInstance is a singleton.
    *******************************************************************************/
   @Test
   void runtimeLifecycleAndRunnerLookup() throws Exception
   {
      QInstance   qInstance = defineInstanceWithTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME).withConcurrency(2));
      QEsbRuntime runtime   = startRuntime(qInstance);
      waitFor("runtime control listener", runtime::isRunning);

      EsbTriggerRunner runner = runtime.getRunner(QUEUE_TRIGGER_NAME);
      assertThat(runner.getTriggerName()).isEqualTo(QUEUE_TRIGGER_NAME);
      assertThat(runner.getProcessName()).isEqualTo(PROCESS_NAME);
      assertThat(runner.getTrigger().getConcurrency()).isEqualTo(2);
      assertThat(runner.getDestination().getEffectiveDestinationName()).isEqualTo(getBrokerQueueName());
      assertThat(runtime.getRunners()).containsExactly(runner);
      assertThat(runtime.getRunner("noSuchTrigger")).isNull();

      /////////////////////////////////////////////
      // a second start is ignored (same runner) //
      /////////////////////////////////////////////
      runtime.start(qInstance);
      assertThat(runtime.getRunner(QUEUE_TRIGGER_NAME)).isSameAs(runner);

      runtime.stop();
      runtime.stop();
      assertThat(runtime.isRunning()).isFalse();
      assertThat(runner.getState()).isEqualTo(EsbTriggerState.STOPPED);

      ///////////////////////////////////////////////////////////////////
      // pause/resume/restart on a stopped runner don't start anything //
      ///////////////////////////////////////////////////////////////////
      runner.pauseLocal();
      runner.resumeLocal();
      runner.restartLocal();
      assertThat(runner.getState()).isEqualTo(EsbTriggerState.STOPPED);

      QInstance emptyInstance = defineInstance();
      new QInstanceValidator().validate(emptyInstance);
      QEsbRuntime emptyRuntime = startRuntime(emptyInstance);
      assertThat(emptyRuntime.isRunning()).isTrue();
      assertThat(emptyRuntime.getRunners()).isEmpty();

      ///////////////////////////////////////////////////////////////////////
      // an unvalidated instance is refused before runners are built       //
      ///////////////////////////////////////////////////////////////////////
      QInstance unvalidated = defineInstance();
      unvalidated.addProcess(defineRecordingProcess(PROCESS_NAME, null, false)
         .withSupplementalMetaData(new EsbProcessMetaData().withTrigger(new EsbTrigger().withDestinationName("noSuchDestination"))));
      assertThatThrownBy(() -> startRuntime(unvalidated)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("validated");
      assertThat(runtime.getRunner(null)).isNull();

      assertThat(QEsbRuntime.getInstance()).isSameAs(QEsbRuntime.getInstance());
   }

}
