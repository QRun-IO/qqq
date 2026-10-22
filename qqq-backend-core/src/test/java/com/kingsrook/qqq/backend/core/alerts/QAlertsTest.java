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

package com.kingsrook.qqq.backend.core.alerts;


import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidatorTest;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for the alerts producer SPI: QAlerts, its sinks, the type and
 ** process meta-data, and the process lifecycle listener.
 *******************************************************************************/
class QAlertsTest extends BaseTest
{
   ///////////////////////////////////////////////////////////////////////
   // sinks are instantiated by QCodeLoader, so they record into these  //
   ///////////////////////////////////////////////////////////////////////
   private static final List<String> events = new ArrayList<>();

   private static AlertSignal lastSignal = null;

   private static NotificationSignal lastNotification = null;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach()
   {
      events.clear();
      lastSignal = null;
      lastNotification = null;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void useRecordingSink()
   {
      AlertsInstanceMetaData.ofOrWithNew(QContext.getQInstance()).withSink(new QCodeReference(RecordingSink.class));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNoImplementation_usesLoggingSinkAndNeverThrows()
   {
      assertNull(AlertsInstanceMetaData.of(QContext.getQInstance()));
      assertThat(QAlerts.getSink(QContext.getQInstance())).isInstanceOf(LoggingAlertSink.class);

      assertDoesNotThrow(() -> QAlerts.raise(new AlertSignal("test.thing.broke").withTitle("It broke").withSeverity(AlertSeverity.ERROR)));
      assertDoesNotThrow(() -> QAlerts.resolve("test.thing.broke", null, "fixed"));
      assertDoesNotThrow(() -> QAlerts.heartbeat("test.beat"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBadInput_isCountedNotThrown()
   {
      long before = QAlerts.getProducerFailureCount();
      assertDoesNotThrow(() -> QAlerts.raise(null));
      assertDoesNotThrow(() -> QAlerts.raise(new AlertSignal(" ")));
      assertDoesNotThrow(() -> QAlerts.resolve(null, null));
      assertDoesNotThrow(() -> QAlerts.heartbeat(null));
      assertEquals(before + 4, QAlerts.getProducerFailureCount());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRegisteredSink_receivesSignalsWithDefaults()
   {
      useRecordingSink();
      QContext.getQInstance().add(new QAlertTypeMetaData().withName("test.thing.broke").withDefaultSeverity(AlertSeverity.CRITICAL));

      QAlerts.raise(new AlertSignal("test.thing.broke").withDedupKey("k1").withTitle("It broke"));
      QAlerts.resolve("test.thing.broke", "k1", "fixed");
      QAlerts.heartbeat("test.beat");

      assertEquals(List.of("raise:test.thing.broke:k1", "resolve:test.thing.broke:k1:fixed", "heartbeat:test.beat"), events);
      assertEquals(AlertSeverity.CRITICAL, lastSignal.getSeverity());
      assertNotNull(lastSignal.getOccurredAt());

      ////////////////////////////////////////////
      // the sink is loaded once and then cached //
      ////////////////////////////////////////////
      assertSame(QAlerts.getSink(QContext.getQInstance()), QAlerts.getSink(QContext.getQInstance()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUnknownType_isStillDelivered()
   {
      useRecordingSink();
      QAlerts.raise(new AlertSignal("test.not.declared"));
      assertEquals(List.of("raise:test.not.declared:test.not.declared"), events);
      assertNull(lastSignal.getSeverity());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTransaction_deferredToCommit_droppedOnRollback() throws QException
   {
      useRecordingSink();

      QBackendTransaction committed = new QBackendTransaction();
      QAlerts.raise(new AlertSignal("test.thing.broke"), committed);
      QAlerts.resolve(new AlertResolution("test.other.broke", "x", null), committed);
      assertThat(events).isEmpty();
      committed.commit();
      assertEquals(List.of("raise:test.thing.broke:test.thing.broke", "resolve:test.other.broke:x:null"), events);

      events.clear();
      QBackendTransaction rolledBack = new QBackendTransaction();
      QAlerts.raise(new AlertSignal("test.thing.broke"), rolledBack);
      rolledBack.rollback();
      rolledBack.commit();
      assertThat(events).isEmpty();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAmbientTransaction_isUsed() throws QException
   {
      useRecordingSink();
      QBackendTransaction transaction = new QBackendTransaction();
      QContext.setTransaction(transaction);
      try
      {
         QAlerts.raise(new AlertSignal("test.thing.broke"));
         assertThat(events).isEmpty();
         transaction.commit();
         assertEquals(1, events.size());
      }
      finally
      {
         QContext.setTransaction(null);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testThrowingSink_neverReachesCaller() throws QException
   {
      AlertsInstanceMetaData.ofOrWithNew(QContext.getQInstance()).withSink(new QCodeReference(ThrowingSink.class));
      long before = QAlerts.getProducerFailureCount();

      assertDoesNotThrow(() -> QAlerts.raise(new AlertSignal("test.thing.broke")));
      assertDoesNotThrow(() -> QAlerts.resolve("test.thing.broke", null, null));
      assertDoesNotThrow(() -> QAlerts.heartbeat("test.beat"));

      QBackendTransaction transaction = new QBackendTransaction();
      QAlerts.raise(new AlertSignal("test.thing.broke"), transaction);
      assertDoesNotThrow(transaction::commit);

      assertEquals(before + 4, QAlerts.getProducerFailureCount());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUnloadableSink_fallsBackToLogging()
   {
      AlertsInstanceMetaData.ofOrWithNew(QContext.getQInstance()).withSink(new QCodeReference().withName("com.example.NoSuchSink"));
      assertThat(QAlerts.getSink(QContext.getQInstance())).isInstanceOf(LoggingAlertSink.class);
      assertDoesNotThrow(() -> QAlerts.raise(new AlertSignal("test.thing.broke")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSignalBounds()
   {
      List<Serializable> ids    = new ArrayList<>(IntStream.rangeClosed(1, 150).boxed().toList());
      AlertSignal        signal = new AlertSignal("test.thing.broke").withRecordRefs(AlertRecordRef.of("person", ids));
      assertEquals(AlertSignal.MAX_RECORD_REFS, signal.getRecordRefs().size());
      assertEquals(150, signal.getEffectiveCount());

      AlertSignal small = new AlertSignal("test.thing.broke").withRecordRef(AlertRecordRef.of("person", 1)).withRecordRef(null);
      assertEquals(1, small.getEffectiveCount());
      assertNull(new AlertSignal("x.y").getEffectiveCount());

      for(int i = 0; i < 25; i++)
      {
         signal.withAttribute("k" + i, "v");
      }
      signal.withAttribute("k0", "replaced");
      assertEquals(AlertSignal.MAX_ATTRIBUTES, signal.getAttributes().size());
      assertEquals("replaced", signal.getAttributes().get("k0"));

      assertEquals("test.thing.broke", signal.getEffectiveDedupKey());
      assertEquals("k", signal.withDedupKey("k").getEffectiveDedupKey());
      assertEquals(AlertRecordRef.of("person", 1), AlertRecordRef.of("person", "1"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSeverityAndDetailLevel()
   {
      assertEquals(AlertSeverity.CRITICAL, AlertSeverity.fromLabel("critical", null));
      assertEquals(AlertSeverity.WARNING, AlertSeverity.fromLabel("Warn", null));
      assertEquals(AlertSeverity.ERROR, AlertSeverity.fromLabel("error", null));
      assertEquals(AlertSeverity.INFO, AlertSeverity.fromLabel("info", null));
      assertEquals(AlertSeverity.WARNING, AlertSeverity.fromLabel("bogus", AlertSeverity.WARNING));
      assertEquals(AlertSeverity.WARNING, AlertSeverity.fromLabel(null, AlertSeverity.WARNING));
      assertTrue(AlertSeverity.ERROR.isAtLeast(AlertSeverity.WARNING));
      assertFalse(AlertSeverity.INFO.isAtLeast(AlertSeverity.WARNING));
      assertTrue(AlertSeverity.INFO.isAtLeast(null));

      assertEquals(DetailLevel.MINIMAL, DetailLevel.lowerOf(DetailLevel.FULL, DetailLevel.MINIMAL));
      assertEquals(DetailLevel.STANDARD, DetailLevel.lowerOf(null, DetailLevel.STANDARD));
      assertEquals(DetailLevel.FULL, DetailLevel.lowerOf(DetailLevel.FULL, null));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testLoggingSink_rateLimitsPerKey()
   {
      LoggingAlertSink sink = new LoggingAlertSink();
      Instant          now  = Instant.now();
      assertTrue(sink.shouldLog("a", now));
      assertFalse(sink.shouldLog("a", now.plusSeconds(10)));
      assertTrue(sink.shouldLog("b", now.plusSeconds(10)));
      assertTrue(sink.shouldLog("a", now.plus(LoggingAlertSink.RATE_LIMIT_WINDOW)));

      assertDoesNotThrow(() -> sink.raise(new AlertSignal("x.y").withOccurredAt(now)));
      assertDoesNotThrow(() -> sink.raise(new AlertSignal("x.y").withSeverity(AlertSeverity.ERROR).withOccurredAt(now.plusSeconds(1000))));
      assertDoesNotThrow(() -> sink.resolve(new AlertResolution("x.y", null, null).withResolvedAt(now)));
      assertDoesNotThrow(() -> sink.heartbeat("x", now));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTypeMetaData_addedAndFound()
   {
      QInstance          qInstance = QContext.getQInstance();
      QAlertTypeMetaData type      = new QAlertTypeMetaData().withName("test.thing.broke").withLabel("Thing broke").withSourceQBitName("qbit-test");
      qInstance.add(type);
      assertSame(type, QAlertTypeMetaData.of(qInstance, "test.thing.broke"));
      assertEquals("qbit-test", type.getEffectiveSourceName());
      assertEquals("src", type.withSourceName("src").getEffectiveSourceName());
      assertNull(QAlertTypeMetaData.of(qInstance, "nope.nope"));

      type.withQuietPeriod(Duration.ofHours(24));
      assertEquals(ResolveMode.QUIET_PERIOD, type.getResolveMode());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testValidation()
   {
      QInstanceValidatorTest.assertValidationFailureReasons(qInstance -> qInstance.add(new QAlertTypeMetaData().withName("NotValid")),
         false, "name must be lowercase and dotted");

      QInstanceValidatorTest.assertValidationFailureReasons(qInstance ->
      {
         qInstance.add(new QAlertTypeMetaData().withName("test.dupe"));
         qInstance.add(new QAlertTypeMetaData().withName("test.dupe"));
      }, false, "declared more than once");

      QInstanceValidatorTest.assertValidationFailureReasons(qInstance -> qInstance.add(new QAlertTypeMetaData()
            .withName("test.thing.broke")
            .withSubjectTableName("notATable")
            .withVisibilityPermission(" ")
            .withDefaultSeverity(null)
            .withResolveMode(ResolveMode.QUIET_PERIOD)
            .withDefaultMonitor(new AlertDefaultMonitor().withKind(AlertMonitorKind.HEARTBEAT))
            .withDefaultMonitor(new AlertDefaultMonitor())),
         false, "subjectTableName notATable", "visibilityPermission must not be blank", "defaultSeverity is required", "positive quietPeriod", "positive expectedEverySeconds", "needs a kind");

      QInstanceValidatorTest.assertValidationFailureReasons(qInstance -> AlertsInstanceMetaData.ofOrWithNew(qInstance).withSink(new QCodeReference(String.class)),
         true, "Alerts sink");

      QInstanceValidatorTest.assertValidationFailureReasons(qInstance -> qInstance.getProcess(TestUtils.PROCESS_NAME_GREET_PEOPLE).withSupplementalMetaData(new AlertProcessMetaData().withExpectedEvery(Duration.ZERO)),
         true, "expectedEvery must be positive");

      QInstanceValidatorTest.assertValidationFailureReasons(qInstance -> qInstance.getProcess(TestUtils.PROCESS_NAME_GREET_PEOPLE).withSupplementalMetaData(new AlertProcessMetaData()),
         true, "set alertOnFailure and/or expectedEvery");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testValidInstance_passes() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();
      qInstance.add(new QAlertTypeMetaData()
         .withName("test.person.stale")
         .withSubjectTableName(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withVisibilityPermission("test.view")
         .withQuietPeriod(Duration.ofHours(1))
         .withDefaultMonitor(AlertDefaultMonitor.heartbeat("test.beat", "test.beat", 3600, 60)));
      AlertsInstanceMetaData.of(qInstance).withSink(new QCodeReference(RecordingSink.class));
      assertDoesNotThrow(() -> new QInstanceValidator().validate(qInstance));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTypeSegment()
   {
      assertEquals("alerts-evaluate-monitors", AlertProcessMetaData.toTypeSegment("alertsEvaluateMonitors"));
      assertEquals("person-memory-bulk-insert", AlertProcessMetaData.toTypeSegment("person_memory.bulkInsert"));
      assertEquals("unnamed", AlertProcessMetaData.toTypeSegment(null));
      assertEquals("process.greet.failed", AlertProcessMetaData.failedTypeName("greet"));
      assertTrue(AlertsInstanceMetaData.ALERT_TYPE_NAME_PATTERN.matcher(AlertProcessMetaData.missedTypeName("myProcess_v2")).matches());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testProcessMetaData_declaresTypesAndAlertsOnFailure() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      useRecordingSink();

      QProcessMetaData failing = new QProcessMetaData()
         .withName("alertsTestFailing")
         .withLabel("Nightly thing")
         .withStep(new QBackendStepMetaData().withName("explode").withCode(new QCodeReference(FailingStep.class)))
         .withSupplementalMetaData(new AlertProcessMetaData().withAlertOnFailure(AlertSeverity.ERROR).withExpectedEvery(Duration.ofHours(24)));
      qInstance.addProcess(failing);

      QProcessMetaData passing = new QProcessMetaData()
         .withName("alertsTestPassing")
         .withStep(new QBackendStepMetaData().withName("ok").withCode(new QCodeReference(PassingStep.class)))
         .withSupplementalMetaData(new AlertProcessMetaData().withAlertOnFailure(AlertSeverity.WARNING).withExpectedEvery(Duration.ofMinutes(5)));
      qInstance.addProcess(passing);

      QInstanceEnricher enricher = new QInstanceEnricher(qInstance);
      AlertProcessMetaData.of(failing).enrich(enricher, failing);
      AlertProcessMetaData.of(failing).enrich(enricher, failing);
      AlertProcessMetaData.of(passing).enrich(enricher, passing);

      QAlertTypeMetaData failedType = QAlertTypeMetaData.of(qInstance, "process.alerts-test-failing.failed");
      assertNotNull(failedType);
      assertEquals("Nightly thing failed", failedType.getLabel());
      assertEquals(AlertSeverity.ERROR, failedType.getDefaultSeverity());

      QAlertTypeMetaData missedType = QAlertTypeMetaData.of(qInstance, "process.alerts-test-failing.missed");
      assertNotNull(missedType);
      assertEquals(AlertMonitorKind.HEARTBEAT, missedType.getDefaultMonitors().get(0).getKind());
      assertEquals(86400, missedType.getDefaultMonitors().get(0).getDefinition().get("expectedEverySeconds"));
      assertEquals(8640, missedType.getDefaultMonitors().get(0).getDefinition().get("graceSeconds"));

      assertEquals(1, qInstance.getProcessLifecycleListeners().stream().filter(ref -> AlertProcessLifecycleListener.class.getName().equals(ref.getName())).count());

      assertThrows(QException.class, () -> new RunProcessAction().execute(new RunProcessInput().withProcessName("alertsTestFailing")));
      assertThat(events).contains("raise:process.alerts-test-failing.failed:alertsTestFailing");
      assertEquals("Nightly thing failed", lastSignal.getTitle());
      assertThat(lastSignal.getDetails()).contains("boom");
      assertThat(lastSignal.getTitle()).doesNotContain("boom");

      events.clear();
      new RunProcessAction().execute(new RunProcessInput().withProcessName("alertsTestPassing"));
      assertThat(events).contains("resolve:process.alerts-test-passing.failed:alertsTestPassing:A later run completed", "heartbeat:process.alerts-test-passing");

      //////////////////////////////////////////////////////////////
      // a process without the meta-data is ignored by the listener //
      //////////////////////////////////////////////////////////////
      AlertProcessLifecycleListener listener = new AlertProcessLifecycleListener();
      assertFalse(listener.appliesTo(TestUtils.PROCESS_NAME_GREET_PEOPLE));
      assertFalse(listener.appliesTo(null));
      assertDoesNotThrow(() -> listener.onProcessStarted(new RunProcessInput().withProcessName("alertsTestPassing")));
      events.clear();
      listener.onProcessCompleted(new RunProcessInput().withProcessName(TestUtils.PROCESS_NAME_GREET_PEOPLE), null);
      listener.onProcessFailed(new RunProcessInput().withProcessName(TestUtils.PROCESS_NAME_GREET_PEOPLE), new Exception("x"));
      assertThat(events).isEmpty();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNotify_noImplementation_logsAndNeverThrows()
   {
      long before = QAlerts.getProducerFailureCount();
      assertDoesNotThrow(() -> QAlerts.notify(new NotificationSignal("Your export is ready").withAudience(NotificationAudience.user("u1"))));
      assertDoesNotThrow(() -> QAlerts.notify(new NotificationSignal("Export failed").withSeverity(NotificationSeverity.ERROR).withSourceKey("process:1").withAudience(NotificationAudience.allUsers())));
      assertEquals(before, QAlerts.getProducerFailureCount());

      LoggingAlertSink sink = new LoggingAlertSink();
      Instant          now  = Instant.now();
      assertDoesNotThrow(() -> sink.notify(new NotificationSignal("t").withCategory(NotificationCategory.JOB).withOccurredAt(now)));
      assertDoesNotThrow(() -> sink.notify(new NotificationSignal("t").withCategory(NotificationCategory.JOB).withSeverity(NotificationSeverity.CRITICAL).withOccurredAt(now.plusSeconds(1))));
      assertFalse(sink.shouldLog("notify|JOB|t", now.plusSeconds(2)));
      assertTrue(sink.shouldLog("notify|JOB|k", now.plusSeconds(2)));

      ///////////////////////////////////////////////////////////////////
      // a sink written before notify existed still accepts the call   //
      ///////////////////////////////////////////////////////////////////
      AlertsInstanceMetaData.ofOrWithNew(QContext.getQInstance()).withSink(new QCodeReference(LegacySink.class));
      before = QAlerts.getProducerFailureCount();
      assertDoesNotThrow(() -> QAlerts.notify(new NotificationSignal("t").withAudience(NotificationAudience.allUsers())));
      assertDoesNotThrow(() -> new LegacySink().notify(null));
      assertEquals(before, QAlerts.getProducerFailureCount());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNotify_registeredSink_receivesWithDefaults()
   {
      useRecordingSink();
      AlertLink link = AlertLink.toUrl("/app/orders", "Orders");
      QAlerts.notify(new NotificationSignal("Your export failed")
         .withBody("**Orders export** stopped")
         .withSourceName("test")
         .withSourceKey("process:1")
         .withSourceTableName("person")
         .withSourceRecordId("7")
         .withLink(link)
         .withSticky(true)
         .withPublishAt(Instant.EPOCH)
         .withExpireAt(Instant.EPOCH.plusSeconds(60))
         .withAudience(NotificationAudience.user("u1")));

      assertEquals(List.of("notify:Your export failed:USERS[u1]"), events);
      assertEquals(NotificationSeverity.INFO, lastNotification.getSeverity());
      assertEquals(NotificationCategory.SYSTEM, lastNotification.getCategory());
      assertNotNull(lastNotification.getOccurredAt());
      assertEquals("**Orders export** stopped", lastNotification.getBody());
      assertEquals("test", lastNotification.getSourceName());
      assertEquals("process:1", lastNotification.getSourceKey());
      assertEquals("person", lastNotification.getSourceTableName());
      assertEquals("7", lastNotification.getSourceRecordId());
      assertSame(link, lastNotification.getLink());
      assertTrue(lastNotification.getSticky());
      assertEquals(Instant.EPOCH, lastNotification.getPublishAt());
      assertEquals(Instant.EPOCH.plusSeconds(60), lastNotification.getExpireAt());

      events.clear();
      QAlerts.notify(new NotificationSignal("Done").withSeverity(NotificationSeverity.SUCCESS).withCategory(NotificationCategory.JOB).withAudience(NotificationAudience.allUsers()));
      assertEquals(List.of("notify:Done:ALL_USERS"), events);
      assertEquals(NotificationSeverity.SUCCESS, lastNotification.getSeverity());
      assertEquals(NotificationCategory.JOB, lastNotification.getCategory());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNotify_deferredToCommit_droppedOnRollback() throws QException
   {
      useRecordingSink();

      QBackendTransaction committed = new QBackendTransaction();
      QAlerts.notify(new NotificationSignal("Shared with you").withAudience(NotificationAudience.user("u1")), committed);
      assertThat(events).isEmpty();
      committed.commit();
      assertEquals(1, events.size());

      events.clear();
      QBackendTransaction rolledBack = new QBackendTransaction();
      QAlerts.notify(new NotificationSignal("Shared with you").withAudience(NotificationAudience.user("u1")), rolledBack);
      rolledBack.rollback();
      rolledBack.commit();
      assertThat(events).isEmpty();

      QBackendTransaction ambient = new QBackendTransaction();
      QContext.setTransaction(ambient);
      try
      {
         QAlerts.notify(new NotificationSignal("Shared with you").withAudience(NotificationAudience.user("u1")));
         assertThat(events).isEmpty();
         ambient.commit();
         assertEquals(1, events.size());
      }
      finally
      {
         QContext.setTransaction(null);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNotify_badInputAndThrowingSink_areCounted() throws QException
   {
      useRecordingSink();
      long before = QAlerts.getProducerFailureCount();
      assertDoesNotThrow(() -> QAlerts.notify(null));
      assertDoesNotThrow(() -> QAlerts.notify(new NotificationSignal(" ").withAudience(NotificationAudience.allUsers())));
      assertDoesNotThrow(() -> QAlerts.notify(new NotificationSignal().withAudience(NotificationAudience.allUsers())));
      assertDoesNotThrow(() -> QAlerts.notify(new NotificationSignal("No audience")));
      assertEquals(before + 4, QAlerts.getProducerFailureCount());
      assertThat(events).isEmpty();

      AlertsInstanceMetaData.of(QContext.getQInstance()).withSink(new QCodeReference(ThrowingSink.class));
      before = QAlerts.getProducerFailureCount();
      assertDoesNotThrow(() -> QAlerts.notify(new NotificationSignal("t").withAudience(NotificationAudience.allUsers())));
      QBackendTransaction transaction = new QBackendTransaction();
      QAlerts.notify(new NotificationSignal("t").withAudience(NotificationAudience.allUsers()), transaction);
      assertDoesNotThrow(transaction::commit);
      assertEquals(before + 2, QAlerts.getProducerFailureCount());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNotificationAudienceAndSignalBounds()
   {
      assertEquals(NotificationAudience.Type.ALL_USERS, NotificationAudience.allUsers().getType());
      assertThat(NotificationAudience.allUsers().getValues()).isEmpty();
      assertEquals(List.of("admin", "ops"), NotificationAudience.roles("admin", null, " ", "ops").getValues());
      assertEquals(NotificationAudience.Type.ROLES, NotificationAudience.roles("admin").getType());
      assertEquals(NotificationAudience.Type.PERMISSION, NotificationAudience.permission("cdn.view").getType());
      assertEquals(List.of("cdn.view"), NotificationAudience.permission("cdn.view").getValues());
      assertEquals(NotificationAudience.users("u1"), NotificationAudience.user("u1"));
      assertEquals(NotificationAudience.users("u1").hashCode(), NotificationAudience.user("u1").hashCode());
      assertNotEquals(NotificationAudience.users("u1"), NotificationAudience.roles("u1"));
      assertNotEquals(NotificationAudience.users("u1"), NotificationAudience.users("u2"));
      assertNotEquals(NotificationAudience.users("u1"), (Object) "u1");
      NotificationAudience same = NotificationAudience.allUsers();
      assertEquals(same, same);
      assertEquals(List.of("u1", "u2"), NotificationAudience.users("u1", "u2").getValues());
      assertThat(NotificationAudience.users((String[]) null).getValues()).isEmpty();
      assertEquals("ALL_USERS", NotificationAudience.allUsers().toString());
      assertEquals("PERMISSION[cdn.view]", NotificationAudience.permission("cdn.view").toString());
      assertThrows(UnsupportedOperationException.class, () -> NotificationAudience.user("u1").getValues().add("u2"));

      NotificationSignal signal = new NotificationSignal("t");
      for(int i = 0; i < 25; i++)
      {
         signal.withAttribute("k" + i, "v");
      }
      signal.withAttribute("k0", "replaced").withAttribute(null, "v");
      assertEquals(NotificationSignal.MAX_ATTRIBUTES, signal.getAttributes().size());
      assertEquals("replaced", signal.getAttributes().get("k0"));

      signal.setAttributes(Map.of("a", "b"));
      assertEquals(Map.of("a", "b"), signal.getAttributes());
      signal.setAttributes(null);
      assertThat(signal.getAttributes()).isEmpty();

      signal.setTitle("x");
      signal.setBody("b");
      signal.setSeverity(NotificationSeverity.WARNING);
      signal.setCategory(NotificationCategory.SECURITY);
      signal.setSourceName("n");
      signal.setSourceKey("k");
      signal.setSourceTableName("t");
      signal.setSourceRecordId("1");
      signal.setLink(null);
      signal.setAudience(NotificationAudience.permission("p"));
      signal.setPublishAt(Instant.EPOCH);
      signal.setExpireAt(Instant.EPOCH);
      signal.setSticky(false);
      signal.setOccurredAt(Instant.EPOCH);
      assertEquals("x", signal.getTitle());
      assertEquals("b", signal.getBody());
      assertEquals("n", signal.getSourceName());
      assertEquals(NotificationSeverity.WARNING, signal.getSeverity());
      assertEquals(NotificationCategory.SECURITY, signal.getCategory());
      assertEquals(NotificationAudience.permission("p"), signal.getAudience());
      assertNull(signal.getLink());
      assertFalse(signal.getSticky());
      assertEquals(Instant.EPOCH, signal.getOccurredAt());
   }



   /*******************************************************************************
    ** A sink that records what it was sent.
    *******************************************************************************/
   public static class RecordingSink implements AlertSinkInterface
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void raise(AlertSignal signal)
      {
         lastSignal = signal;
         events.add("raise:" + signal.getAlertTypeName() + ":" + signal.getEffectiveDedupKey());
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void resolve(AlertResolution resolution)
      {
         events.add("resolve:" + resolution.getAlertTypeName() + ":" + resolution.getEffectiveDedupKey() + ":" + resolution.getNote());
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void heartbeat(String heartbeatKey, Instant at)
      {
         events.add("heartbeat:" + heartbeatKey);
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void notify(NotificationSignal signal)
      {
         lastNotification = signal;
         events.add("notify:" + signal.getTitle() + ":" + signal.getAudience());
      }
   }



   /*******************************************************************************
    ** A sink written before notify existed: it inherits the default.
    *******************************************************************************/
   public static class LegacySink implements AlertSinkInterface
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void raise(AlertSignal signal)
      {
         events.add("legacy-raise");
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void resolve(AlertResolution resolution)
      {
         events.add("legacy-resolve");
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void heartbeat(String heartbeatKey, Instant at)
      {
         events.add("legacy-heartbeat");
      }
   }



   /*******************************************************************************
    ** A sink that always throws.
    *******************************************************************************/
   public static class ThrowingSink implements AlertSinkInterface
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void raise(AlertSignal signal)
      {
         throw (new IllegalStateException("sink down"));
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void resolve(AlertResolution resolution)
      {
         throw (new IllegalStateException("sink down"));
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void heartbeat(String heartbeatKey, Instant at)
      {
         throw (new IllegalStateException("sink down"));
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void notify(NotificationSignal signal)
      {
         throw (new IllegalStateException("sink down"));
      }
   }



   /*******************************************************************************
    ** A step that always fails.
    *******************************************************************************/
   public static class FailingStep implements BackendStep
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
      {
         throw (new QException("boom"));
      }
   }



   /*******************************************************************************
    ** A step that succeeds.
    *******************************************************************************/
   public static class PassingStep implements BackendStep
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput)
      {
         ////////////
         // noop   //
         ////////////
      }
   }
}
