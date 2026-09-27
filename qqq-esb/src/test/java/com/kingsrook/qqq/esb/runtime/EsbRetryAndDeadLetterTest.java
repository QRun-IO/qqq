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


import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.esb.envelope.EsbEvent;
import com.kingsrook.qqq.esb.envelope.EsbEventCodec;
import com.kingsrook.qqq.esb.model.EsbDeadLetterAction;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.EsbProcessMetaData;
import com.kingsrook.qqq.esb.model.EsbTrigger;
import com.kingsrook.qqq.esb.model.QEsbDestinationMetaData;
import com.kingsrook.qqq.esb.stats.EsbCounterSnapshot;
import com.kingsrook.qqq.esb.stats.EsbStats;
import jakarta.jms.BytesMessage;
import jakarta.jms.MapMessage;
import jakarta.jms.Message;
import jakarta.jms.MessageEOFException;
import jakarta.jms.ObjectMessage;
import jakarta.jms.StreamMessage;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Unit test for trigger retries (by broker redelivery, JMSXDeliveryCount being
 ** the attempt number), backoff, timeouts, and dead letters.
 *******************************************************************************/
class EsbRetryAndDeadLetterTest extends EsbRuntimeTestBase
{

   /*******************************************************************************
    ** With maxAttempts 3, a message whose first two runs fail succeeds on the
    ** third attempt, and isn't dead-lettered.
    *******************************************************************************/
   @Test
   void failTwiceThenSucceedWithMaxAttemptsThree() throws Exception
   {
      EsbTrigger trigger = new EsbTrigger().withDestinationName(QUEUE_NAME).withMaxAttempts(3);
      defineInstanceWithTrigger(trigger);
      RecordingStep.failNextRuns(2);

      EsbEvent sent = sendEvent(QUEUE_NAME, Map.of());
      startRuntime(QContext.getQInstance());

      waitFor("the successful run", () -> RecordingStep.getCompletedRuns().size() == 1);
      assertThat(RecordingStep.getRuns()).hasSize(3);
      assertThat(RecordingStep.getRuns()).allSatisfy(run -> assertThat(run.getEvents().get(0).getId()).isEqualTo(sent.getId()));
      assertThat(receiveAll(getDeadLetterQueueName(trigger), Duration.ofMillis(500))).isEmpty();

      EsbCounterSnapshot counters = EsbStats.getInstance().trigger(QUEUE_TRIGGER_NAME);
      assertThat(counters.consumed()).isEqualTo(3);
      assertThat(counters.failed()).isEqualTo(2);
      assertThat(counters.retried()).isEqualTo(2);
      assertThat(counters.succeeded()).isEqualTo(1);
      assertThat(counters.deadLettered()).isZero();
   }



   /*******************************************************************************
    ** A message that always fails is attempted maxAttempts times, then sent -
    ** once - to the dead-letter queue, with the original event and the four
    ** qqq properties.
    *******************************************************************************/
   @Test
   void alwaysFailSendsOneDeadLetterWithTheFourProperties() throws Exception
   {
      EsbTrigger trigger = new EsbTrigger().withDestinationName(QUEUE_NAME);
      defineInstanceWithTrigger(trigger);
      RecordingStep.failAlways();

      Instant  before = Instant.now();
      EsbEvent sent   = sendEvent(QUEUE_NAME, Map.of("orderNo", "A-1"));
      startRuntime(QContext.getQInstance());

      List<Message> deadLetters = receiveAll(getDeadLetterQueueName(trigger), WAIT_TIMEOUT);
      assertThat(deadLetters).hasSize(1);
      assertThat(RecordingStep.getRuns()).hasSize(3);

      Message deadLetter = deadLetters.get(0);
      assertThat(deadLetter.getStringProperty("qqqError")).contains("boom on run 3");
      assertThat(deadLetter.getStringProperty("qqqFailedTrigger")).isEqualTo(QUEUE_TRIGGER_NAME);
      assertThat(deadLetter.getIntProperty("qqqAttempts")).isEqualTo(3);
      assertThat(Instant.parse(deadLetter.getStringProperty("qqqFailedAt"))).isAfterOrEqualTo(before).isBeforeOrEqualTo(Instant.now());

      EsbEvent deadEvent = EsbEventCodec.fromMessage(deadLetter);
      assertThat(deadEvent.getId()).isEqualTo(sent.getId());
      assertThat(deadEvent.getData()).containsEntry("orderNo", "A-1");
      assertThat(deadLetter.getStringProperty(EsbEventCodec.PROPERTY_ID)).isEqualTo(sent.getId());
      assertThat(deadLetter.getStringProperty(EsbEventCodec.PROPERTY_TYPE)).isEqualTo(sent.getType());

      waitFor("the dead-letter count", () -> EsbStats.getInstance().trigger(QUEUE_TRIGGER_NAME).deadLettered() == 1);
      EsbCounterSnapshot counters = EsbStats.getInstance().trigger(QUEUE_TRIGGER_NAME);
      assertThat(counters.failed()).isEqualTo(3);
      assertThat(counters.retried()).isEqualTo(2);
      assertThat(counters.lastError()).contains("boom");
   }



   /*******************************************************************************
    ** With onDeadLetter DISCARD, the message is dropped after its last attempt:
    ** not dead-lettered, and not left on the queue.
    *******************************************************************************/
   @Test
   void discardDropsTheMessage() throws Exception
   {
      EsbTrigger trigger = new EsbTrigger().withDestinationName(QUEUE_NAME).withMaxAttempts(2).withOnDeadLetter(EsbDeadLetterAction.DISCARD);
      defineInstanceWithTrigger(trigger);
      RecordingStep.failAlways();

      sendEvent(QUEUE_NAME, Map.of());
      QEsbRuntime runtime = startRuntime(QContext.getQInstance());

      waitFor("the discard", () -> EsbStats.getInstance().trigger(QUEUE_TRIGGER_NAME).deadLettered() == 1);
      runtime.stop();

      assertThat(RecordingStep.getRuns()).hasSize(2);
      assertThat(receiveAll(getDeadLetterQueueName(trigger), Duration.ofMillis(500))).isEmpty();
      assertThat(receiveAll(getBrokerQueueName(), Duration.ofMillis(500))).isEmpty();
   }



   /*******************************************************************************
    ** With retryDelayMs 200, the retry starts at least 200 ms after the failed
    ** attempt ended.
    *******************************************************************************/
   @Test
   void backoffDelaysTheRetry() throws Exception
   {
      defineInstanceWithTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME).withRetryDelayMs(200));
      RecordingStep.failNextRuns(1);

      sendEvent(QUEUE_NAME, Map.of());
      startRuntime(QContext.getQInstance());

      waitFor("the successful retry", () -> RecordingStep.getCompletedRuns().size() == 1);
      List<RecordedRun> runs = RecordingStep.getRuns();
      assertThat(runs).hasSize(2);
      assertThat(Duration.between(runs.get(0).getEndedAt(), runs.get(1).getStartedAt())).isGreaterThanOrEqualTo(Duration.ofMillis(200));
   }



   /*******************************************************************************
    ** Messages that aren't CloudEvents (non-JSON text, JSON that isn't a
    ** CloudEvent, a non-text message) go straight to the dead-letter queue, with
    ** qqqError "unparseable message" and no retries; the process never runs.
    ** Text, bytes, map, and stream bodies are copied to the dead letter.  An
    ** object message's body is not deserialized, so its dead letter has no body,
    ** and qqqBodyDropped true.
    *******************************************************************************/
   @Test
   void unparseableMessageDeadLettersImmediately() throws Exception
   {
      EsbTrigger trigger = new EsbTrigger().withDestinationName(QUEUE_NAME).withMaxAttempts(3);
      defineInstanceWithTrigger(trigger);

      sendMessage(QUEUE_NAME, session -> session.createTextMessage("not json"));
      sendMessage(QUEUE_NAME, session -> session.createTextMessage("{\"hello\": \"world\"}"));
      sendMessage(QUEUE_NAME, session ->
      {
         BytesMessage bytesMessage = session.createBytesMessage();
         bytesMessage.writeBytes("raw bytes".getBytes(StandardCharsets.UTF_8));
         bytesMessage.setStringProperty("custom", "kept");
         return (bytesMessage);
      });
      sendMessage(QUEUE_NAME, session ->
      {
         MapMessage mapMessage = session.createMapMessage();
         mapMessage.setString("orderNo", "A-1");
         mapMessage.setInt("quantity", 7);
         mapMessage.setBytes("raw", new byte[] { 1, 2, 3 });
         mapMessage.setStringProperty("custom", "kept");
         return (mapMessage);
      });
      sendMessage(QUEUE_NAME, session ->
      {
         StreamMessage streamMessage = session.createStreamMessage();
         streamMessage.writeString("first");
         streamMessage.writeLong(42L);
         streamMessage.writeBoolean(true);
         streamMessage.writeBytes(new byte[] { 4, 5 });
         streamMessage.setStringProperty("custom", "kept");
         return (streamMessage);
      });
      sendMessage(QUEUE_NAME, session ->
      {
         ObjectMessage objectMessage = session.createObjectMessage("a serialized payload");
         objectMessage.setStringProperty("custom", "kept");
         return (objectMessage);
      });

      startRuntime(QContext.getQInstance());
      waitFor("6 dead letters", () -> EsbStats.getInstance().trigger(QUEUE_TRIGGER_NAME).deadLettered() == 6);

      List<Message> deadLetters = receiveAll(getDeadLetterQueueName(trigger), WAIT_TIMEOUT);
      assertThat(deadLetters).hasSize(6);
      assertThat(deadLetters).allSatisfy(deadLetter ->
      {
         assertThat(deadLetter.getStringProperty("qqqError")).isEqualTo(UNPARSEABLE_MESSAGE_ERROR);
         assertThat(deadLetter.getIntProperty("qqqAttempts")).isEqualTo(1);
         assertThat(deadLetter.getStringProperty("qqqFailedTrigger")).isEqualTo(QUEUE_TRIGGER_NAME);
      });
      assertThat(deadLetters.subList(0, 5)).allSatisfy(deadLetter -> assertThat(deadLetter.propertyExists("qqqBodyDropped")).isFalse());

      assertThat(((TextMessage) deadLetters.get(0)).getText()).isEqualTo("not json");
      assertThat(((TextMessage) deadLetters.get(1)).getText()).isEqualTo("{\"hello\": \"world\"}");
      BytesMessage bytesDeadLetter = (BytesMessage) deadLetters.get(2);
      byte[]       body            = new byte[(int) bytesDeadLetter.getBodyLength()];
      bytesDeadLetter.readBytes(body);
      assertThat(new String(body, StandardCharsets.UTF_8)).isEqualTo("raw bytes");
      assertThat(bytesDeadLetter.getStringProperty("custom")).isEqualTo("kept");

      MapMessage mapDeadLetter = (MapMessage) deadLetters.get(3);
      assertThat(Collections.list(mapDeadLetter.getMapNames())).containsExactlyInAnyOrder("orderNo", "quantity", "raw");
      assertThat(mapDeadLetter.getString("orderNo")).isEqualTo("A-1");
      assertThat(mapDeadLetter.getInt("quantity")).isEqualTo(7);
      assertThat(mapDeadLetter.getBytes("raw")).containsExactly(1, 2, 3);
      assertThat(mapDeadLetter.getStringProperty("custom")).isEqualTo("kept");

      StreamMessage streamDeadLetter = (StreamMessage) deadLetters.get(4);
      assertThat(streamDeadLetter.readString()).isEqualTo("first");
      assertThat(streamDeadLetter.readLong()).isEqualTo(42L);
      assertThat(streamDeadLetter.readBoolean()).isTrue();
      assertThat((byte[]) streamDeadLetter.readObject()).containsExactly(4, 5);
      assertThatThrownBy(streamDeadLetter::readObject).isInstanceOf(MessageEOFException.class);
      assertThat(streamDeadLetter.getStringProperty("custom")).isEqualTo("kept");

      Message objectDeadLetter = deadLetters.get(5);
      assertThat(objectDeadLetter).isNotInstanceOf(ObjectMessage.class);
      assertThat(objectDeadLetter.getBooleanProperty("qqqBodyDropped")).isTrue();
      assertThat(objectDeadLetter.getStringProperty("custom")).isEqualTo("kept");

      assertThat(RecordingStep.getRuns()).isEmpty();
      assertThat(EsbStats.getInstance().trigger(QUEUE_TRIGGER_NAME).retried()).isZero();
   }



   /*******************************************************************************
    ** Stopping the runtime during a slow run interrupts the run and rolls its
    ** message back - so it's processed after a restart, not lost (or
    ** dead-lettered).
    *******************************************************************************/
   @Test
   void stopDuringRunRedeliversAfterRestart() throws Exception
   {
      EsbTrigger trigger = new EsbTrigger().withDestinationName(QUEUE_NAME);
      defineInstanceWithTrigger(trigger);
      RecordingStep.slowNextRuns(1, 60_000L);

      EsbEvent    sent    = sendEvent(QUEUE_NAME, Map.of());
      QEsbRuntime runtime = startRuntime(QContext.getQInstance());
      waitFor("the slow run to start", () -> RecordingStep.getRuns().size() == 1);

      Instant stopStart = Instant.now();
      runtime.stop();
      assertThat(Duration.between(stopStart, Instant.now())).isLessThan(Duration.ofSeconds(5));
      assertThat(RecordingStep.getRuns().get(0).getInterrupted()).isTrue();
      assertThat(RecordingStep.getCompletedRuns()).isEmpty();

      runtime.start(QContext.getQInstance());
      waitFor("the redelivered run", () -> RecordingStep.getCompletedRuns().size() == 1);
      assertThat(RecordingStep.getCompletedRuns().get(0).getEvents().get(0).getId()).isEqualTo(sent.getId());
      assertThat(receiveAll(getDeadLetterQueueName(trigger), Duration.ofMillis(500))).isEmpty();
      assertThat(EsbStats.getInstance().trigger(QUEUE_TRIGGER_NAME).failed()).isZero();
   }



   /*******************************************************************************
    ** A run-as session without permission to the process fails the run with a
    ** permission error, and the message is dead-lettered with it (the process
    ** never runs).
    *******************************************************************************/
   @Test
   void runAsSessionWithoutProcessPermissionIsDeadLettered() throws Exception
   {
      EsbTrigger trigger = new EsbTrigger()
         .withDestinationName(QUEUE_NAME)
         .withMaxAttempts(1)
         .withRunAsSessionSupplier(new QCodeReference(NoPermissionSessionSupplier.class));

      QInstance qInstance = defineInstanceWithDestinations(PROVIDER_NAME);
      qInstance.addProcess(defineRecordingProcess(PROCESS_NAME, null, false)
         .withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION))
         .withSupplementalMetaData(new EsbProcessMetaData().withTrigger(trigger)));
      QContext.init(qInstance, new QSession());

      sendEvent(QUEUE_NAME, Map.of());
      startRuntime(qInstance);

      List<Message> deadLetters = receiveAll(getDeadLetterQueueName(trigger), WAIT_TIMEOUT);
      assertThat(deadLetters).hasSize(1);
      assertThat(deadLetters.get(0).getStringProperty("qqqError")).contains("Permission denied");
      assertThat(RecordingStep.getRuns()).isEmpty();
   }



   /*******************************************************************************
    ** A run longer than timeoutMs is interrupted and counts as a failed attempt;
    ** the retry then succeeds.
    *******************************************************************************/
   @Test
   void timeoutInterruptsTheRunAndCountsAsFailed() throws Exception
   {
      defineInstanceWithTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME).withTimeoutMs(300).withMaxAttempts(2));
      RecordingStep.slowNextRuns(1, 60_000L);

      sendEvent(QUEUE_NAME, Map.of());
      startRuntime(QContext.getQInstance());

      waitFor("the successful retry", () -> RecordingStep.getCompletedRuns().size() == 1);
      List<RecordedRun> runs = RecordingStep.getRuns();
      assertThat(runs).hasSize(2);
      assertThat(runs.get(0).getInterrupted()).isTrue();

      EsbCounterSnapshot counters = EsbStats.getInstance().trigger(QUEUE_TRIGGER_NAME);
      assertThat(counters.failed()).isEqualTo(1);
      assertThat(counters.lastError()).contains("timed out after 300 ms");
   }



   /*******************************************************************************
    ** A topic trigger's dead letters go to the default topic dead-letter queue:
    ** brokerTopicName.subscriptionName.dlq.
    *******************************************************************************/
   @Test
   void topicTriggerDeadLettersToTheSubscriptionDeadLetterQueue() throws Exception
   {
      defineInstanceWithTrigger(new EsbTrigger().withDestinationName(TOPIC_NAME).withMaxAttempts(1));
      RecordingStep.failAlways();

      QEsbRuntime runtime = startRuntime(QContext.getQInstance());
      waitForState(runtime, TOPIC_TRIGGER_NAME, EsbTriggerState.RUNNING);
      EsbEvent sent = sendEvent(TOPIC_NAME, Map.of());

      String        deadLetterQueueName = getBrokerTopicName() + "." + PROCESS_NAME + "." + getBrokerTopicName() + ".dlq";
      List<Message> deadLetters         = receiveAll(deadLetterQueueName, WAIT_TIMEOUT);
      assertThat(deadLetters).hasSize(1);
      assertThat(EsbEventCodec.fromMessage(deadLetters.get(0)).getId()).isEqualTo(sent.getId());
      assertThat(deadLetters.get(0).getStringProperty("qqqFailedTrigger")).isEqualTo(TOPIC_TRIGGER_NAME);
   }



   /*******************************************************************************
    ** The broker-side name of the trigger's dead-letter queue.
    *******************************************************************************/
   private String getDeadLetterQueueName(EsbTrigger trigger)
   {
      QEsbDestinationMetaData destination = EsbInstanceMetaData.of(QContext.getQInstance()).getDestination(trigger.getDestinationName());
      return (trigger.getEffectiveDeadLetterDestinationName(PROCESS_NAME, destination));
   }

}
