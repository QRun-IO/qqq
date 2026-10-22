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


import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReferenceLambda;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.esb.envelope.EsbCausation;
import com.kingsrook.qqq.esb.envelope.EsbEvent;
import com.kingsrook.qqq.esb.envelope.EsbEventFactory;
import com.kingsrook.qqq.esb.model.EsbProcessMetaData;
import com.kingsrook.qqq.esb.model.EsbTrigger;
import com.kingsrook.qqq.esb.model.EsbTriggerMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Unit test for EsbTriggerHandler used directly (as a dead-letter replay
 ** would, from a thread that has its own QContext), and its helpers.
 *******************************************************************************/
class EsbTriggerHandlerTest extends EsbRuntimeTestBase
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      EsbCausation.clear();
   }



   /*******************************************************************************
    ** runProcess runs as the trigger's run-as session with the SINGLE event's id
    ** as causation - then puts back the caller's QContext and causation.
    *******************************************************************************/
   @Test
   void runProcessRestoresTheCallersContextAndCausation() throws Exception
   {
      EsbTrigger trigger   = new EsbTrigger().withDestinationName(QUEUE_NAME);
      QInstance  qInstance = defineInstanceWithTrigger(trigger);
      QSession   caller    = new QSession().withUser(new QUser().withIdReference("caller"));
      QContext.init(qInstance, caller);
      EsbCausation.set("outer");

      EsbEvent event = EsbEventFactory.custom("test", "test", "qqq.test.replayed", Map.of());
      new EsbTriggerHandler(qInstance).runProcess(trigger, PROCESS_NAME, List.of(event));

      RecordedRun run = RecordingStep.getRuns().get(0);
      assertThat(run.getCompleted()).isTrue();
      assertThat(run.getCausationId()).isEqualTo(event.getId());
      assertThat(run.getSession()).isInstanceOf(QSystemUserSession.class);
      assertThat(run.getEvents()).extracting(EsbEvent::getId).containsExactly(event.getId());

      assertThat(QContext.getQSession()).isSameAs(caller);
      assertThat(QContext.getQInstance()).isSameAs(qInstance);
      assertThat(EsbCausation.current()).isEqualTo("outer");
   }



   /*******************************************************************************
    ** A failing process's exception comes out of runProcess, and the caller's
    ** context is still put back.  BATCH mode leaves causation as it was.
    *******************************************************************************/
   @Test
   void runProcessThrowsTheRunsFailure() throws Exception
   {
      EsbTrigger trigger   = new EsbTrigger().withDestinationName(QUEUE_NAME).withMode(EsbTriggerMode.BATCH);
      QInstance  qInstance = defineInstanceWithTrigger(trigger);
      QSession   caller    = QContext.getQSession();
      RecordingStep.failAlways();

      EsbEvent event = EsbEventFactory.custom("test", "test", "qqq.test.replayed", Map.of());
      assertThatThrownBy(() -> new EsbTriggerHandler(qInstance).runProcess(trigger, PROCESS_NAME, List.of(event)))
         .isInstanceOf(QException.class)
         .hasMessageContaining("boom");

      assertThat(RecordingStep.getRuns().get(0).getCausationId()).isNull();
      assertThat(QContext.getQSession()).isSameAs(caller);
      assertThat(EsbCausation.current()).isNull();
   }



   /*******************************************************************************
    ** The run-as session can come from a lambda code reference; a supplier that
    ** gives no session, or can't be loaded, fails the run.
    *******************************************************************************/
   @Test
   void runAsSessionSuppliers() throws Exception
   {
      QInstance         qInstance = defineInstanceWithTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME));
      EsbTriggerHandler handler   = new EsbTriggerHandler(qInstance);
      List<EsbEvent>    events    = List.of(EsbEventFactory.custom("test", "test", "qqq.test.sent", Map.of()));

      Supplier<QSession> lambdaSupplier = () -> new QSession().withUser(new QUser().withIdReference("fromLambda"));
      handler.runProcess(new EsbTrigger().withDestinationName(QUEUE_NAME).withRunAsSessionSupplier(new QCodeReferenceLambda<>(lambdaSupplier)), PROCESS_NAME, events);
      assertThat(RecordingStep.getRuns().get(0).getSession().getUser().getIdReference()).isEqualTo("fromLambda");

      Supplier<QSession> nullSupplier = () -> null;
      assertThatThrownBy(() -> handler.runProcess(new EsbTrigger().withDestinationName(QUEUE_NAME).withRunAsSessionSupplier(new QCodeReferenceLambda<>(nullSupplier)), PROCESS_NAME, events))
         .isInstanceOf(QException.class)
         .hasMessageContaining("did not supply a QSession");

      assertThatThrownBy(() -> handler.runProcess(new EsbTrigger().withDestinationName(QUEUE_NAME).withRunAsSessionSupplier(new QCodeReference(String.class)), PROCESS_NAME, events))
         .isInstanceOf(QException.class)
         .hasMessageContaining("runAsSessionSupplier");

      assertThat(RecordingStep.getRuns()).hasSize(1);
   }



   /*******************************************************************************
    ** Only events from the process's own table go into the callback filter: a
    ** table-bound process with other events gets no filter; a process with no
    ** table gets none either.
    *******************************************************************************/
   @Test
   void callbackFilterOnlyForTheProcessesTable() throws Exception
   {
      QInstance qInstance = defineInstanceWithDestinations(PROVIDER_NAME);
      qInstance.addProcess(defineRecordingProcess(TABLE_PROCESS_NAME, TABLE_NAME_ORDER, false)
         .withSupplementalMetaData(new EsbProcessMetaData().withTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME))));
      qInstance.addProcess(defineRecordingProcess(PROCESS_NAME, null, false));
      QContext.init(qInstance, new QSession());

      EsbTriggerHandler handler     = new EsbTriggerHandler(qInstance);
      EsbTrigger        trigger     = new EsbTrigger().withDestinationName(QUEUE_NAME).withMode(EsbTriggerMode.BATCH);
      EsbEvent          otherTable  = new EsbEvent().withId("1").withSource("qqq://app/table/customer").withType("qqq.table.customer.inserted").withSubject("7");
      EsbEvent          noSubject   = new EsbEvent().withId("2").withSource("qqq://app/table/order").withType("qqq.table.order.deleted");
      EsbEvent          orderEvent  = new EsbEvent().withId("3").withSource("qqq://app/table/order").withType("qqq.table.order.updated").withSubject("42");
      List<EsbEvent>    unrelated   = List.of(otherTable, noSubject);
      List<EsbEvent>    withAnOrder = List.of(otherTable, orderEvent);

      handler.runProcess(trigger, TABLE_PROCESS_NAME, unrelated);
      assertThat(RecordingStep.getRuns().get(0).getCallbackFilter()).isNull();

      handler.runProcess(trigger, TABLE_PROCESS_NAME, withAnOrder);
      assertThat(RecordingStep.getRuns().get(1).getCallbackFilter().getCriteria().get(0).getValues()).containsExactly(42);

      handler.runProcess(trigger, PROCESS_NAME, withAnOrder);
      assertThat(RecordingStep.getRuns().get(2).getCallbackFilter()).isNull();
   }



   /*******************************************************************************
    ** The table name comes from a qqq:// table source; anything else has none.
    *******************************************************************************/
   @Test
   void eventTableName()
   {
      assertThat(EsbTriggerHandler.getEventTableName(new EsbEvent().withSource("qqq://app/table/order"))).isEqualTo("order");
      assertThat(EsbTriggerHandler.getEventTableName(new EsbEvent().withSource("qqq:///table/order"))).isEqualTo("order");
      assertThat(EsbTriggerHandler.getEventTableName(new EsbEvent().withSource("qqq://app/process/syncOrder"))).isNull();
      assertThat(EsbTriggerHandler.getEventTableName(new EsbEvent().withSource("qqq://app"))).isNull();
      assertThat(EsbTriggerHandler.getEventTableName(new EsbEvent().withSource("https://example.com/table/order"))).isNull();
      assertThat(EsbTriggerHandler.getEventTableName(new EsbEvent())).isNull();
   }



   /*******************************************************************************
    ** Backoff: retryDelayMs, times retryMultiplier per earlier attempt, capped at
    ** retryMaxDelayMs; 0 means immediate.
    *******************************************************************************/
   @Test
   void retryDelay()
   {
      EsbTrigger trigger = new EsbTrigger().withRetryDelayMs(100).withRetryMultiplier(2.0).withRetryMaxDelayMs(250);
      assertThat(EsbTriggerHandler.getRetryDelayMs(trigger, 1)).isEqualTo(100L);
      assertThat(EsbTriggerHandler.getRetryDelayMs(trigger, 2)).isEqualTo(200L);
      assertThat(EsbTriggerHandler.getRetryDelayMs(trigger, 3)).isEqualTo(250L);
      assertThat(EsbTriggerHandler.getRetryDelayMs(trigger, 1000)).isEqualTo(250L);

      assertThat(EsbTriggerHandler.getRetryDelayMs(new EsbTrigger(), 1)).isZero();
      assertThat(EsbTriggerHandler.getRetryDelayMs(new EsbTrigger().withRetryDelayMs(null), 2)).isZero();
   }



   /*******************************************************************************
    ** Error text: the message (or class name), plus the root cause's message.
    *******************************************************************************/
   @Test
   void errorText()
   {
      assertThat(EsbTriggerHandler.getErrorText(new QException("boom"))).isEqualTo("boom");
      assertThat(EsbTriggerHandler.getErrorText(new QException("Error running process", new IllegalStateException("bad state")))).isEqualTo("Error running process: bad state");
      assertThat(EsbTriggerHandler.getErrorText(new IllegalStateException())).isEqualTo("IllegalStateException");
      assertThat(EsbTriggerHandler.getErrorText(new QException("same", new QException("same")))).isEqualTo("same");
      assertThat(EsbTriggerHandler.getErrorText(null)).isNull();
      assertThat(EsbTriggerHandler.getErrorText(new QException("x".repeat(5000)))).hasSize(EsbTriggerHandler.MAX_ERROR_LENGTH);
   }

}
