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

package com.kingsrook.qqq.esb.runtime;


import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.esb.envelope.EsbEvent;
import com.kingsrook.qqq.esb.envelope.EsbEventCodec;
import com.kingsrook.qqq.esb.envelope.EsbEventFactory;
import com.kingsrook.qqq.esb.model.EsbDedupeConfig;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.EsbProcessMetaData;
import com.kingsrook.qqq.esb.model.EsbTrigger;
import com.kingsrook.qqq.esb.outbox.EsbH2TestSupport;
import com.kingsrook.qqq.esb.outbox.EsbMessageDedupe;
import com.kingsrook.qqq.esb.outbox.EsbProcessedMessageTable;
import com.kingsrook.qqq.esb.stats.EsbStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit test for idempotent triggers (EsbDedupeConfig, EsbMessageDedupe):
 ** a redelivered message (same message id) runs its process once.
 *******************************************************************************/
class EsbDedupeTest extends EsbRuntimeTestBase
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws Exception
   {
      EsbH2TestSupport.resetDatabase();
   }



   /*******************************************************************************
    ** An instance with a queue trigger, and dedupe on (or off).
    *******************************************************************************/
   private QInstance setUpInstance(boolean dedupe)
   {
      QInstance qInstance = defineInstanceWithDestinations(PROVIDER_NAME);
      qInstance.addProcess(defineRecordingProcess(PROCESS_NAME, null, false)
         .withSupplementalMetaData(new EsbProcessMetaData().withTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME).withMaxAttempts(3))));
      EsbH2TestSupport.addTo(qInstance);
      EsbInstanceMetaData.of(qInstance).withDedupe(new EsbDedupeConfig().withEnabled(dedupe).withTableName(EsbH2TestSupport.PROCESSED_TABLE));
      QContext.init(qInstance, new QSession());
      return (qInstance);
   }



   /*******************************************************************************
    ** The same message sent twice (as after a failover, or an outbox re-send)
    ** runs once; a different message runs; both are recorded.
    *******************************************************************************/
   @Test
   void testDuplicateRunsOnce() throws Exception
   {
      setUpInstance(true);
      EsbEvent event = EsbEventFactory.custom("test", "test/dedupe", "qqq.test.sent", Map.of("n", 1));
      sendMessage(QUEUE_NAME, session -> EsbEventCodec.toMessage(session, event, "row-uuid-1"));
      sendMessage(QUEUE_NAME, session -> EsbEventCodec.toMessage(session, event, "row-uuid-1"));
      sendMessage(QUEUE_NAME, session -> EsbEventCodec.toMessage(session, event, "row-uuid-2"));

      startRuntime(QContext.getQInstance());
      waitFor("3 messages consumed", () -> EsbStats.getInstance().trigger(QUEUE_TRIGGER_NAME).consumed() == 3);
      pause(500);

      assertThat(RecordingStep.getCompletedRuns()).hasSize(2);
      assertThat(processedIds()).containsExactlyInAnyOrder("row-uuid-1", "row-uuid-2");
   }



   /*******************************************************************************
    ** A message whose run was already recorded (its commit was lost) is skipped
    ** and acknowledged.
    *******************************************************************************/
   @Test
   void testAlreadyRecordedIsSkipped() throws Exception
   {
      setUpInstance(true);
      new InsertAction().execute(new InsertInput(EsbH2TestSupport.PROCESSED_TABLE).withRecord(new QRecord()
         .withValue(EsbProcessedMessageTable.FIELD_TRIGGER_NAME, QUEUE_TRIGGER_NAME)
         .withValue(EsbProcessedMessageTable.FIELD_MESSAGE_ID, "row-uuid-1")
         .withValue(EsbProcessedMessageTable.FIELD_PROCESSED_AT, Instant.now())));
      sendMessage(QUEUE_NAME, session -> EsbEventCodec.toMessage(session, EsbEventFactory.custom("test", "test/dedupe", "qqq.test.sent", Map.of()), "row-uuid-1"));

      startRuntime(QContext.getQInstance());
      waitFor("the message consumed", () -> EsbStats.getInstance().trigger(QUEUE_TRIGGER_NAME).consumed() == 1);
      pause(500);
      assertThat(RecordingStep.getRuns()).isEmpty();
      assertThat(receiveAll(getBrokerQueueName(), Duration.ofMillis(300))).as("the skipped message was acknowledged").isEmpty();
   }



   /*******************************************************************************
    ** A failed run isn't recorded, so its retry still runs.
    *******************************************************************************/
   @Test
   void testFailedRunIsRetried() throws Exception
   {
      setUpInstance(true);
      RecordingStep.failNextRuns(1);
      sendEvent(QUEUE_NAME, Map.of("n", 1));

      startRuntime(QContext.getQInstance());
      waitFor("a completed run", () -> RecordingStep.getCompletedRuns().size() == 1);
      assertThat(RecordingStep.getRuns()).hasSize(2);
      waitFor("the run to be recorded", () -> processedIds().size() == 1);
   }



   /*******************************************************************************
    ** With dedupe off (the default), a duplicate runs again.
    *******************************************************************************/
   @Test
   void testDisabledRunsDuplicates() throws Exception
   {
      setUpInstance(false);
      EsbEvent event = EsbEventFactory.custom("test", "test/dedupe", "qqq.test.sent", Map.of("n", 1));
      sendMessage(QUEUE_NAME, session -> EsbEventCodec.toMessage(session, event, "row-uuid-1"));
      sendMessage(QUEUE_NAME, session -> EsbEventCodec.toMessage(session, event, "row-uuid-1"));

      startRuntime(QContext.getQInstance());
      waitFor("2 runs", () -> RecordingStep.getCompletedRuns().size() == 2);
      assertThat(processedIds()).isEmpty();
   }



   /*******************************************************************************
    ** findProcessed / recordProcessed directly: recording twice doesn't throw,
    ** and keys are per trigger.
    *******************************************************************************/
   @Test
   void testFindAndRecord() throws Exception
   {
      setUpInstance(true);
      EsbDedupeConfig config = EsbMessageDedupe.getEnabledConfig(QContext.getQInstance());
      EsbMessageDedupe.recordProcessed(config, "a.b", List.of("m1", "m2", "m1"));
      EsbMessageDedupe.recordProcessed(config, "a.b", List.of("m2"));
      assertThat(EsbMessageDedupe.findProcessed(config, "a.b", List.of("m1", "m2", "m3"))).isEqualTo(Set.of("m1", "m2"));
      assertThat(EsbMessageDedupe.findProcessed(config, "other.trigger", List.of("m1"))).isEmpty();
   }



   /*******************************************************************************
    ** The key: qqq_message_id, else ce_id.
    *******************************************************************************/
   @Test
   void testMessageKey() throws Exception
   {
      setUpInstance(true);
      EsbEvent event = EsbEventFactory.custom("test", "test/dedupe", "qqq.test.sent", Map.of());
      try(jakarta.jms.Session session = com.kingsrook.qqq.esb.connection.EsbConnectionManager.getInstance().openSession(PROVIDER_NAME, false))
      {
         jakarta.jms.Message withRowId = EsbEventCodec.toMessage(session, event, "row-uuid");
         assertThat(EsbEventCodec.getMessageKey(withRowId)).isEqualTo("row-uuid");
         jakarta.jms.Message direct = EsbEventCodec.toMessage(session, event);
         assertThat(EsbEventCodec.getMessageKey(direct)).isEqualTo(event.getId());
         direct.clearProperties();
         direct.setStringProperty(EsbEventCodec.PROPERTY_ID, "ce-id");
         assertThat(EsbEventCodec.getMessageKey(direct)).isEqualTo("ce-id");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<String> processedIds()
   {
      try
      {
         return (new QueryAction().execute(new QueryInput(EsbH2TestSupport.PROCESSED_TABLE)).getRecords().stream()
            .map(r -> r.getValueString(EsbProcessedMessageTable.FIELD_MESSAGE_ID)).toList());
      }
      catch(Exception e)
      {
         throw (new IllegalStateException(e));
      }
   }

}
