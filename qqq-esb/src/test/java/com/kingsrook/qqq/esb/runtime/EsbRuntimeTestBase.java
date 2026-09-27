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


import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QRecordListMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.utils.SleepUtils;
import com.kingsrook.qqq.esb.EsbTestBase;
import com.kingsrook.qqq.esb.connection.EsbConnectionManager;
import com.kingsrook.qqq.esb.envelope.EsbCausation;
import com.kingsrook.qqq.esb.envelope.EsbEvent;
import com.kingsrook.qqq.esb.envelope.EsbEventCodec;
import com.kingsrook.qqq.esb.envelope.EsbEventFactory;
import com.kingsrook.qqq.esb.model.EsbDestinationType;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.EsbProcessMetaData;
import com.kingsrook.qqq.esb.model.EsbProviderType;
import com.kingsrook.qqq.esb.model.EsbTrigger;
import com.kingsrook.qqq.esb.model.QEsbDestinationMetaData;
import com.kingsrook.qqq.esb.model.QEsbProviderMetaData;
import com.kingsrook.qqq.esb.stats.EsbStats;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import static org.assertj.core.api.Assertions.fail;


/*******************************************************************************
 * Base class for the trigger runtime tests (against the embedded Artemis broker
 * from EsbTestBase).
 *
 * Each test gets its own broker-side queue and topic names, so leftovers from
 * one test can't reach another.  The process under test (handleOrder, or the
 * table-bound syncOrderRecords) runs RecordingStep, which records each run and
 * can be told to fail or to be slow.  Runtimes started through startRuntime are
 * stopped after each test, and the connection manager is reset.
 *******************************************************************************/
public abstract class EsbRuntimeTestBase extends EsbTestBase
{
   public static final String QUEUE_NAME                = "orderQueue";
   public static final String TOPIC_NAME                = "orderEvents";
   public static final String PROCESS_NAME              = "handleOrder";
   public static final String TABLE_PROCESS_NAME        = "syncOrderRecords";
   public static final String QUEUE_TRIGGER_NAME        = PROCESS_NAME + "." + QUEUE_NAME;
   public static final String TOPIC_TRIGGER_NAME        = PROCESS_NAME + "." + TOPIC_NAME;
   public static final String SECOND_PROVIDER_NAME      = "artemisNodeB";
   public static final String CUSTOM_SESSION_USER_ID    = "esbRunAsUser";
   public static final String RESTRICTED_PERMISSION     = "handleOrder.hasAccess";
   public static final String UNPARSEABLE_MESSAGE_ERROR = "unparseable message";

   protected static final Duration WAIT_TIMEOUT = Duration.ofSeconds(20);

   private final List<QEsbRuntime> runtimes = new ArrayList<>();

   private String brokerQueueName;
   private String brokerTopicName;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void esbRuntimeTestBaseBeforeEach()
   {
      String suffix = UUID.randomUUID().toString().substring(0, 8);
      brokerQueueName = "esb.test.orderQueue." + suffix;
      brokerTopicName = "esb.test.orderEvents." + suffix;
      RecordingStep.reset();
      EsbStats.getInstance().reset();
   }



   /*******************************************************************************
    ** Stop this test's runtimes, reset the connection manager, and make sure the
    ** broker is up for the next test (in case a test stopped it and failed).
    *******************************************************************************/
   @AfterEach
   void esbRuntimeTestBaseAfterEach() throws Exception
   {
      for(QEsbRuntime runtime : runtimes)
      {
         runtime.stop();
      }
      runtimes.clear();

      EsbConnectionManager.getInstance().closeAll();
      startEmbeddedBroker();
      RecordingStep.reset();
   }



   /*******************************************************************************
    ** An instance with this test's queue and topic destinations, and the
    ** handleOrder process with the given trigger - validated, and put in the
    ** QContext (so the test can send messages).
    *******************************************************************************/
   protected QInstance defineInstanceWithTrigger(EsbTrigger trigger)
   {
      QInstance qInstance = defineInstanceWithDestinations(PROVIDER_NAME);
      qInstance.addProcess(defineRecordingProcess(PROCESS_NAME, null, false)
         .withSupplementalMetaData(new EsbProcessMetaData().withTrigger(trigger)));
      QContext.init(qInstance, new QSession());
      return (qInstance);
   }



   /*******************************************************************************
    ** An instance (not yet validated) with this test's queue and topic
    ** destinations on the named provider - which, if it isn't the base class's
    ** provider, is added, pointing at the same embedded broker.
    *******************************************************************************/
   protected QInstance defineInstanceWithDestinations(String providerName)
   {
      QInstance           qInstance           = defineInstance();
      EsbInstanceMetaData esbInstanceMetaData = EsbInstanceMetaData.of(qInstance);
      if(!PROVIDER_NAME.equals(providerName))
      {
         esbInstanceMetaData.withProvider(new QEsbProviderMetaData()
            .withName(providerName)
            .withType(EsbProviderType.ACTIVEMQ_ARTEMIS)
            .withUrl(getBrokerUrl()));
      }

      esbInstanceMetaData
         .withDestination(new QEsbDestinationMetaData()
            .withName(QUEUE_NAME)
            .withType(EsbDestinationType.QUEUE)
            .withProviderName(providerName)
            .withDestinationName(brokerQueueName))
         .withDestination(new QEsbDestinationMetaData()
            .withName(TOPIC_NAME)
            .withType(EsbDestinationType.TOPIC)
            .withProviderName(providerName)
            .withDestinationName(brokerTopicName));

      return (qInstance);
   }



   /*******************************************************************************
    ** A process (on a table, or on none) that runs RecordingStep - after a
    ** frontend step, which a triggered run must skip.  With withRecordInput, the
    ** step takes the table's records as input (so they are loaded through the
    ** process callback).
    *******************************************************************************/
   protected static QProcessMetaData defineRecordingProcess(String processName, String tableName, Boolean withRecordInput)
   {
      QBackendStepMetaData recordStep = new QBackendStepMetaData()
         .withName("record")
         .withCode(new QCodeReference(RecordingStep.class));

      if(withRecordInput)
      {
         recordStep.withInputData(new QFunctionInputMetaData()
            .withRecordListMetaData(new QRecordListMetaData().withTableName(tableName)));
      }

      return (new QProcessMetaData()
         .withName(processName)
         .withTableName(tableName)
         .withStep(new QFrontendStepMetaData().withName("review"))
         .withStep(recordStep));
   }



   /*******************************************************************************
    ** Start a new runtime on an instance (it is stopped after the test).
    *******************************************************************************/
   protected QEsbRuntime startRuntime(QInstance qInstance)
   {
      QEsbRuntime runtime = new QEsbRuntime();
      runtimes.add(runtime);
      runtime.start(qInstance);
      return (runtime);
   }



   /*******************************************************************************
    ** Wait for a runtime's trigger to reach a state.
    *******************************************************************************/
   protected void waitForState(QEsbRuntime runtime, String triggerName, EsbTriggerState state)
   {
      waitFor("trigger " + triggerName + " to be " + state, () -> runtime.getRunner(triggerName) != null && runtime.getRunner(triggerName).getState() == state);
   }



   /*******************************************************************************
    ** Send a new event (with the given data) to a destination of the QContext's
    ** instance, and return it.
    *******************************************************************************/
   protected EsbEvent sendEvent(String destinationName, Map<String, Serializable> data) throws Exception
   {
      EsbEvent event = EsbEventFactory.custom("test", "test/" + destinationName, "qqq.test.sent", data);
      sendEvent(destinationName, event);
      return (event);
   }



   /*******************************************************************************
    ** Send an event to a destination of the QContext's instance.
    *******************************************************************************/
   protected void sendEvent(String destinationName, EsbEvent event) throws Exception
   {
      sendMessage(destinationName, session -> EsbEventCodec.toMessage(session, event));
   }



   /*******************************************************************************
    ** Send a message (made on the sending session) to a destination of the
    ** QContext's instance.
    *******************************************************************************/
   protected void sendMessage(String destinationName, MessageMaker messageMaker) throws Exception
   {
      QEsbDestinationMetaData destination = EsbInstanceMetaData.of(QContext.getQInstance()).getDestination(destinationName);
      EsbConnectionManager    manager     = EsbConnectionManager.getInstance();
      try(Session session = manager.openSession(destination.getProviderName(), false))
      {
         session.createProducer(manager.resolve(session, destination)).send(messageMaker.make(session));
      }
   }



   /*******************************************************************************
    ** Receive every message now on a broker queue (waiting up to firstWait for the
    ** first one, then briefly for more).
    *******************************************************************************/
   protected List<Message> receiveAll(String brokerQueueName, Duration firstWait) throws Exception
   {
      EsbConnectionManager manager  = EsbConnectionManager.getInstance();
      List<Message>        messages = new ArrayList<>();
      try(Session session = manager.openSession(PROVIDER_NAME, false))
      {
         Queue           queue    = manager.resolveQueue(session, PROVIDER_NAME, brokerQueueName);
         MessageConsumer consumer = session.createConsumer(queue);
         Message         message  = consumer.receive(firstWait.toMillis());
         while(message != null)
         {
            messages.add(message);
            message = consumer.receive(300);
         }
      }
      return (messages);
   }



   /*******************************************************************************
    ** Wait (polling) for a condition to become true, failing after WAIT_TIMEOUT.
    *******************************************************************************/
   protected static void waitFor(String description, BooleanSupplier condition)
   {
      Instant deadline = Instant.now().plus(WAIT_TIMEOUT);
      while(!condition.getAsBoolean())
      {
         if(Instant.now().isAfter(deadline))
         {
            fail("Timed out waiting for " + description);
         }
         SleepUtils.sleep(25, TimeUnit.MILLISECONDS);
      }
   }



   /*******************************************************************************
    ** Wait a little, then return - for asserting that something did NOT happen.
    *******************************************************************************/
   protected static void pause(Integer millis)
   {
      SleepUtils.sleep(millis, TimeUnit.MILLISECONDS);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected String getBrokerQueueName()
   {
      return (brokerQueueName);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected String getBrokerTopicName()
   {
      return (brokerTopicName);
   }



   /*******************************************************************************
    * Makes a message to send, on the sending session.
    *******************************************************************************/
   @FunctionalInterface
   protected interface MessageMaker
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      Message make(Session session) throws JMSException;
   }



   /*******************************************************************************
    * A run-as session supplier with a known user (and every permission).
    *******************************************************************************/
   public static class CustomSessionSupplier implements Supplier<QSession>
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QSession get()
      {
         return (new QSession()
            .withUser(new QUser().withIdReference(CUSTOM_SESSION_USER_ID))
            .withPermissions(RESTRICTED_PERMISSION));
      }
   }



   /*******************************************************************************
    * A run-as session supplier whose session has no permissions.
    *******************************************************************************/
   public static class NoPermissionSessionSupplier implements Supplier<QSession>
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QSession get()
      {
         return (new QSession().withUser(new QUser().withIdReference("noPermissions")));
      }
   }



   /*******************************************************************************
    * What one run of RecordingStep saw, and how it ended.
    *******************************************************************************/
   public static class RecordedRun
   {
      private final List<EsbEvent> events;
      private final String         causationId;
      private final QQueryFilter   callbackFilter;
      private final List<QRecord>  records;
      private final QSession       session;
      private final QInstance      qInstance;
      private final Instant        startedAt = Instant.now();

      private volatile Instant endedAt;
      private volatile Boolean completed   = false;
      private volatile Boolean interrupted = false;



      /*******************************************************************************
       ** Constructor - captures the run's input and context.
       *******************************************************************************/
      @SuppressWarnings("unchecked")
      RecordedRun(RunBackendStepInput input)
      {
         this.events = new ArrayList<>((List<EsbEvent>) input.getValue("esbMessages"));
         this.causationId = EsbCausation.current();
         this.callbackFilter = input.getCallback() == null ? null : input.getCallback().getQueryFilter();
         this.records = input.getRecords() == null ? List.of() : new ArrayList<>(input.getRecords());
         this.session = QContext.getQSession();
         this.qInstance = QContext.getQInstance();
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public List<EsbEvent> getEvents()
      {
         return (events);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public String getCausationId()
      {
         return (causationId);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public QQueryFilter getCallbackFilter()
      {
         return (callbackFilter);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public List<QRecord> getRecords()
      {
         return (records);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public QSession getSession()
      {
         return (session);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public QInstance getQInstance()
      {
         return (qInstance);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public Instant getStartedAt()
      {
         return (startedAt);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public Instant getEndedAt()
      {
         return (endedAt);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public Boolean getCompleted()
      {
         return (completed);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public Boolean getInterrupted()
      {
         return (interrupted);
      }
   }



   /*******************************************************************************
    * Backend step that records each run (see RecordedRun), and that tests can
    * make fail (a number of times, or always) or slow (for a number of runs).
    * A slow run that is interrupted fails.
    *******************************************************************************/
   public static class RecordingStep implements BackendStep
   {
      private static final List<RecordedRun> runs              = new CopyOnWriteArrayList<>();
      private static final AtomicInteger     failuresRemaining = new AtomicInteger(0);
      private static final AtomicInteger     slowRunsRemaining = new AtomicInteger(0);
      private static final AtomicInteger     currentRuns       = new AtomicInteger(0);
      private static final AtomicInteger     maxConcurrentRuns = new AtomicInteger(0);

      private static volatile Boolean alwaysFail      = false;
      private static volatile Boolean alwaysThrowError = false;
      private static volatile Long    slowRunMs       = 0L;



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
      {
         RecordedRun run = new RecordedRun(runBackendStepInput);
         runs.add(run);
         maxConcurrentRuns.accumulateAndGet(currentRuns.incrementAndGet(), Math::max);
         try
         {
            if(slowRunsRemaining.getAndDecrement() > 0)
            {
               sleepInterruptibly(run);
            }

            if(alwaysThrowError)
            {
               throw (new AssertionError("error on run " + runs.size()));
            }

            if(alwaysFail || failuresRemaining.getAndDecrement() > 0)
            {
               throw (new QException("boom on run " + runs.size()));
            }

            run.completed = true;
         }
         finally
         {
            currentRuns.decrementAndGet();
            run.endedAt = Instant.now();
         }
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      private static void sleepInterruptibly(RecordedRun run) throws QException
      {
         try
         {
            Thread.sleep(slowRunMs);
         }
         catch(InterruptedException e)
         {
            run.interrupted = true;
            Thread.currentThread().interrupt();
            throw (new QException("interrupted"));
         }
      }



      /*******************************************************************************
       ** Forget all runs and behaviors.
       *******************************************************************************/
      public static void reset()
      {
         runs.clear();
         failuresRemaining.set(0);
         slowRunsRemaining.set(0);
         currentRuns.set(0);
         maxConcurrentRuns.set(0);
         alwaysFail = false;
         alwaysThrowError = false;
         slowRunMs = 0L;
      }



      /*******************************************************************************
       ** Fail the next n runs.
       *******************************************************************************/
      public static void failNextRuns(Integer n)
      {
         failuresRemaining.set(n);
      }



      /*******************************************************************************
       ** Fail every run.
       *******************************************************************************/
      public static void failAlways()
      {
         alwaysFail = true;
      }



      /*******************************************************************************
       ** Make every run throw an Error (not an Exception).
       *******************************************************************************/
      public static void throwErrorAlways()
      {
         alwaysThrowError = true;
      }



      /*******************************************************************************
       ** Make the next n runs sleep for ms milliseconds (before they fail or
       ** succeed).
       *******************************************************************************/
      public static void slowNextRuns(Integer n, Long ms)
      {
         slowRunMs = ms;
         slowRunsRemaining.set(n);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public static List<RecordedRun> getRuns()
      {
         return (runs);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public static List<RecordedRun> getCompletedRuns()
      {
         return (runs.stream().filter(RecordedRun::getCompleted).toList());
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public static Integer getMaxConcurrentRuns()
      {
         return (maxConcurrentRuns.get());
      }
   }

}
