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
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.esb.envelope.EsbEvent;
import com.kingsrook.qqq.esb.envelope.EsbEventCodec;
import com.kingsrook.qqq.esb.envelope.EsbEventFactory;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.EsbProcessMetaData;
import com.kingsrook.qqq.esb.model.EsbTrigger;
import com.kingsrook.qqq.esb.model.EsbTriggerMode;
import com.kingsrook.qqq.esb.stats.EsbStats;
import jakarta.jms.Message;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit test for BATCH-mode triggers: up to batchSize messages (or what arrives
 ** within batchWaitMs) in one run, committed or rolled back together.
 *******************************************************************************/
class EsbBatchTriggerTest extends EsbRuntimeTestBase
{

   /*******************************************************************************
    ** Ten messages, with batchSize 10, make one run with all ten events - and,
    ** being a BATCH run, no causation id.
    *******************************************************************************/
   @Test
   void batchOfTenInOneRun() throws Exception
   {
      defineInstanceWithTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME).withMode(EsbTriggerMode.BATCH).withBatchSize(10).withBatchWaitMs(10_000));

      QEsbRuntime runtime = startRuntime(QContext.getQInstance());
      waitForState(runtime, QUEUE_TRIGGER_NAME, EsbTriggerState.RUNNING);

      List<String> sentIds = new ArrayList<>();
      for(int i = 0; i < 10; i++)
      {
         sentIds.add(sendEvent(QUEUE_NAME, Map.of("n", i)).getId());
      }

      waitFor("the batch run", () -> RecordingStep.getCompletedRuns().size() == 1);
      pause(1000);

      List<RecordedRun> runs = RecordingStep.getRuns();
      assertThat(runs).hasSize(1);
      assertThat(runs.get(0).getEvents().stream().map(EsbEvent::getId).toList()).containsExactlyElementsOf(sentIds);
      assertThat(runs.get(0).getCausationId()).isNull();

      waitFor("the counters", () -> EsbStats.getInstance().trigger(QUEUE_TRIGGER_NAME).succeeded() == 1);
      assertThat(EsbStats.getInstance().trigger(QUEUE_TRIGGER_NAME).consumed()).isEqualTo(10);
   }



   /*******************************************************************************
    ** A batch that doesn't fill up runs once batchWaitMs has passed since its
    ** first message.
    *******************************************************************************/
   @Test
   void partialBatchRunsWhenTheWaitIsOver() throws Exception
   {
      defineInstanceWithTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME).withMode(EsbTriggerMode.BATCH).withBatchWaitMs(1500));

      QEsbRuntime runtime = startRuntime(QContext.getQInstance());
      waitForState(runtime, QUEUE_TRIGGER_NAME, EsbTriggerState.RUNNING);

      Instant sentAt = Instant.now();
      for(int i = 0; i < 3; i++)
      {
         sendEvent(QUEUE_NAME, Map.of("n", i));
      }

      waitFor("the batch run", () -> RecordingStep.getCompletedRuns().size() == 1);
      RecordedRun run = RecordingStep.getRuns().get(0);
      assertThat(run.getEvents()).hasSize(3);
      assertThat(Duration.between(sentAt, run.getStartedAt())).isGreaterThanOrEqualTo(Duration.ofMillis(1000));
   }



   /*******************************************************************************
    ** A failing batch is retried as a whole, then every message in it is
    ** dead-lettered (each with its own attempt count).
    *******************************************************************************/
   @Test
   void failedBatchIsRetriedAndDeadLetteredTogether() throws Exception
   {
      EsbTrigger trigger = new EsbTrigger().withDestinationName(QUEUE_NAME).withMode(EsbTriggerMode.BATCH).withBatchSize(3).withBatchWaitMs(5000).withMaxAttempts(2);
      defineInstanceWithTrigger(trigger);
      RecordingStep.failAlways();

      List<String> sentIds = new ArrayList<>();
      for(int i = 0; i < 3; i++)
      {
         sentIds.add(sendEvent(QUEUE_NAME, Map.of("n", i)).getId());
      }
      startRuntime(QContext.getQInstance());

      waitFor("3 dead letters", () -> EsbStats.getInstance().trigger(QUEUE_TRIGGER_NAME).deadLettered() == 3);
      List<Message> deadLetters = receiveAll(getDeadLetterQueueName(trigger), WAIT_TIMEOUT);
      assertThat(deadLetters).hasSize(3);
      assertThat(deadLetters).allSatisfy(deadLetter -> assertThat(deadLetter.getIntProperty("qqqAttempts")).isEqualTo(2));

      List<String> deadLetterIds = new ArrayList<>();
      for(Message deadLetter : deadLetters)
      {
         deadLetterIds.add(EsbEventCodec.fromMessage(deadLetter).getId());
      }
      assertThat(deadLetterIds).containsExactlyInAnyOrderElementsOf(sentIds);

      assertThat(RecordingStep.getRuns()).hasSize(2);
      assertThat(RecordingStep.getRuns()).allSatisfy(run -> assertThat(run.getEvents()).hasSize(3));
      assertThat(EsbStats.getInstance().trigger(QUEUE_TRIGGER_NAME).retried()).isEqualTo(3);
   }



   /*******************************************************************************
    ** An unparseable message in a batch is dead-lettered at once; the rest of the
    ** batch runs.
    *******************************************************************************/
   @Test
   void unparseableMessageInABatchIsDeadLetteredAndTheRestRun() throws Exception
   {
      EsbTrigger trigger = new EsbTrigger().withDestinationName(QUEUE_NAME).withMode(EsbTriggerMode.BATCH).withBatchSize(3).withBatchWaitMs(5000);
      defineInstanceWithTrigger(trigger);

      EsbEvent first = sendEvent(QUEUE_NAME, Map.of("n", 1));
      sendMessage(QUEUE_NAME, session -> session.createTextMessage("not json"));
      EsbEvent third = sendEvent(QUEUE_NAME, Map.of("n", 3));
      startRuntime(QContext.getQInstance());

      waitFor("the batch run", () -> RecordingStep.getCompletedRuns().size() == 1);
      assertThat(RecordingStep.getRuns().get(0).getEvents().stream().map(EsbEvent::getId).toList()).containsExactly(first.getId(), third.getId());

      List<Message> deadLetters = receiveAll(getDeadLetterQueueName(trigger), WAIT_TIMEOUT);
      assertThat(deadLetters).hasSize(1);
      assertThat(deadLetters.get(0).getStringProperty("qqqError")).isEqualTo(UNPARSEABLE_MESSAGE_ERROR);
   }



   /*******************************************************************************
    ** A table-bound process's batch run gets a callback filter on all of the
    ** batch's primary keys (each once), and so gets those records.
    *******************************************************************************/
   @Test
   void batchForTableBoundProcessFiltersOnAllSubjects() throws Exception
   {
      QInstance qInstance = defineInstanceWithDestinations(PROVIDER_NAME);
      qInstance.addProcess(defineRecordingProcess(TABLE_PROCESS_NAME, TABLE_NAME_ORDER, true)
         .withSupplementalMetaData(new EsbProcessMetaData().withTrigger(new EsbTrigger()
            .withDestinationName(QUEUE_NAME)
            .withMode(EsbTriggerMode.BATCH)
            .withBatchSize(4)
            .withBatchWaitMs(5000))));
      QContext.init(qInstance, new QSystemUserSession());

      List<QRecord> inserted = new InsertAction().execute(new InsertInput(TABLE_NAME_ORDER).withRecords(List.of(
         new QRecord().withValue("orderNo", "B-1"),
         new QRecord().withValue("orderNo", "B-2"),
         new QRecord().withValue("orderNo", "B-3")))).getRecords();

      sendEvent(QUEUE_NAME, EsbEventFactory.forRecordChange("test", TABLE_NAME_ORDER, RecordChangeType.INSERT, inserted.get(0), null));
      sendEvent(QUEUE_NAME, EsbEventFactory.forRecordChange("test", TABLE_NAME_ORDER, RecordChangeType.UPDATE, inserted.get(2), inserted.get(2)));
      sendEvent(QUEUE_NAME, EsbEventFactory.forRecordChange("test", TABLE_NAME_ORDER, RecordChangeType.UPDATE, inserted.get(0), inserted.get(0)));
      sendEvent(QUEUE_NAME, EsbEventFactory.custom("test", "process/other", "qqq.custom.thing", Map.of()));

      startRuntime(qInstance);
      waitFor("the batch run", () -> RecordingStep.getCompletedRuns().size() == 1);

      RecordedRun run = RecordingStep.getRuns().get(0);
      assertThat(run.getEvents()).hasSize(4);
      QFilterCriteria criteria = run.getCallbackFilter().getCriteria().get(0);
      assertThat(criteria.getValues()).containsExactly(inserted.get(0).getValueInteger("id"), inserted.get(2).getValueInteger("id"));
      assertThat(run.getRecords().stream().map(record -> record.getValueString("orderNo")).toList()).containsExactlyInAnyOrder("B-1", "B-3");
   }



   /*******************************************************************************
    ** Pausing while a batch is being collected ends the collecting: the batch so
    ** far runs right away, not after batchWaitMs, and the trigger then pauses.
    *******************************************************************************/
   @Test
   void pauseEndsTheBatchBeingCollected() throws Exception
   {
      defineInstanceWithTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME).withMode(EsbTriggerMode.BATCH).withBatchSize(10).withBatchWaitMs(30_000));
      QEsbRuntime runtime = startRuntime(QContext.getQInstance());
      waitForState(runtime, QUEUE_TRIGGER_NAME, EsbTriggerState.RUNNING);

      sendEvent(QUEUE_NAME, Map.of("n", 1));
      sendEvent(QUEUE_NAME, Map.of("n", 2));
      pause(1500);
      assertThat(RecordingStep.getRuns()).isEmpty();

      Instant pausedAt = Instant.now();
      runtime.getRunner(QUEUE_TRIGGER_NAME).pauseLocal();
      waitFor("the batch run", () -> RecordingStep.getCompletedRuns().size() == 1);

      assertThat(Duration.between(pausedAt, RecordingStep.getRuns().get(0).getStartedAt())).isLessThan(Duration.ofSeconds(5));
      assertThat(RecordingStep.getRuns().get(0).getEvents()).hasSize(2);
      assertThat(runtime.getRunner(QUEUE_TRIGGER_NAME).getState()).isEqualTo(EsbTriggerState.PAUSED);
   }



   /*******************************************************************************
    ** The broker-side name of the trigger's dead-letter queue.
    *******************************************************************************/
   private String getDeadLetterQueueName(EsbTrigger trigger)
   {
      return (trigger.getEffectiveDeadLetterDestinationName(PROCESS_NAME, EsbInstanceMetaData.of(QContext.getQInstance()).getDestination(trigger.getDestinationName())));
   }

}
