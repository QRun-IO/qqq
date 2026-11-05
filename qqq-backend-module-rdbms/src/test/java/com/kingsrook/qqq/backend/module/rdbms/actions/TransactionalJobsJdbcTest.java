/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.rdbms.actions;


import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeEvent;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeListenerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeType;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.TransactionalRecordChangeMetaData;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.DurableJobs;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaData;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaDataProvider;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsTables;
import com.kingsrook.qqq.backend.core.jobs.DurableProcessMetaData;
import com.kingsrook.qqq.backend.core.jobs.FixedProcessRequest;
import com.kingsrook.qqq.backend.core.jobs.JobSessions;
import com.kingsrook.qqq.backend.core.jobs.model.JobAction;
import com.kingsrook.qqq.backend.core.jobs.model.JobEventType;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.store.DurableJobStore;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Actual synchronous file-H2 transactions, independent observer connections and
 ** cold reopen evidence. No mock transaction claims durability or worker identity.
 *******************************************************************************/
class TransactionalJobsJdbcTest
{
   @TempDir
   Path directory;
   protected QInstance instance;
   protected DurableJobsMetaData settings;
   protected DurableJobStore store;
   protected String url;
   private static Integer enlistedId;
   private static boolean failAfter;
   private static boolean twice;
   private static boolean changeScope;
   private static boolean restoredMalformed;
   private static String operationKey;
   private static final List<String> observedEventIds = new java.util.concurrent.CopyOnWriteArrayList<>();
   private static volatile boolean verifyExecution;
   private static volatile String verifierMutation;
   private static volatile String observedVerifierRefusal;
   private static volatile com.kingsrook.qqq.backend.core.jobs.runtime.EnlistedExecution.Verified verifiedExecution;



   /*******************************************************************************
    ** Create the native owner schema and seed its private admission row explicitly.
    *******************************************************************************/
   @BeforeEach
   void prepare() throws Exception
   {
      instance = new QInstance();
      RDBMSBackendMetaData backend = newBackend();
      configureBackend(backend);
      instance.addBackend(backend);
      instance.registerAuthenticationProvider(com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope.instanceDefault(), TestUtils.defineAuthentication());
      QTableMetaData source = new QTableMetaData().withName("source_record").withBackendName(backend.getName()).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER)).withField(new QFieldMetaData("value", QFieldType.STRING).withMaxLength(100))
         .withSupplementalMetaData(new TransactionalRecordChangeMetaData(new QCodeReference(Listener.class), Set.of(RecordChangeType.INSERT)));
      instance.addTable(source);
      new DurableJobsMetaDataProvider().defineAll(instance, backend.getName(), DurableJobsMetaDataProvider.snakeCaseColumnNames());
      settings = new DurableJobsMetaData().withTransactionalAdmissionEnabled(true).withTransactionalNamespace("test-instance")
         .withJobGrantKey(Base64.getEncoder().encodeToString(new byte[32])).withMaxQueuedJobsPerUser(20);
      instance.withSupplementalMetaData(settings);
      instance.addProcess(new QProcessMetaData().withName("dispatch").withLabel("Dispatch")
         .withSupplementalMetaData(new DurableProcessMetaData().withEligible(true).withTransactionalRevision("v1"))
         .withStepList(List.of(new QBackendStepMetaData().withName("execute").withCode(new QCodeReference(Step.class)))));
      try(Connection connection = openConnection(); Statement statement = connection.createStatement())
      {
         for(QTableMetaData table : instance.getTables().values())
         {
            List<String> columns = new ArrayList<>();
            for(QFieldMetaData field : table.getFields().values())
            {
               String name = field.getBackendName() == null ? field.getName() : field.getBackendName();
               columns.add(quote(name) + " " + (field.getName().equals(table.getPrimaryKeyField()) ? identityType() : type(field)));
            }
            if(table.getUniqueKeys() != null)
            {
               table.getUniqueKeys().forEach(key -> columns.add("UNIQUE(" + String.join(",", key.getFieldNames().stream()
                  .map(name -> quote(table.getField(name).getBackendName())).toList()) + ")"));
            }
            statement.execute("CREATE TABLE " + table.getName() + " (" + String.join(",", columns) + ")" + tableSuffix());
         }
         statement.execute("CREATE TABLE jobs_admission(id INTEGER PRIMARY KEY)" + tableSuffix());
         statement.execute("INSERT INTO jobs_admission(id) VALUES(1)");
      }
      QContext.init(instance, session());
      store = new DurableJobStore(settings);
      enlistedId = null;
      failAfter = false;
      twice = false;
      changeScope = false;
      restoredMalformed = false;
      operationKey = null;
      observedEventIds.clear();
      verifyExecution = false;
      verifierMutation = null;
      observedVerifierRefusal = null;
      verifiedExecution = null;
   }



   /*******************************************************************************
    ** The backend metadata object for this provider; the native postgres fixture
    ** returns its own native metadata type.
    *******************************************************************************/
   protected RDBMSBackendMetaData newBackend()
   {
      return (TestUtils.defineBackend());
   }



   /*******************************************************************************
    ** Identifier quoting for the fixture DDL (H2 and MySQL accept backticks).
    *******************************************************************************/
   protected String quote(String identifier)
   {
      return ("`" + identifier + "`");
   }



   /*******************************************************************************
    ** Configure the actual provider before metadata/native pools are initialized.
    *******************************************************************************/
   protected void configureBackend(RDBMSBackendMetaData backend) throws Exception
   {
      url = "jdbc:h2:file:" + directory.resolve("jobs").toString().replace('\\', '/')
         + ";DATABASE_TO_UPPER=FALSE;WRITE_DELAY=0;LOCK_TIMEOUT=5000";
      backend.setJdbcUrl(url);
   }



   /*******************************************************************************
    ** Independent observer connection, overridden by the real MySQL fixture.
    *******************************************************************************/
   protected Connection openConnection() throws Exception
   {
      return (DriverManager.getConnection(url, "sa", ""));
   }



   /*******************************************************************************
    ** Native identity type for the provider's standard owner schema.
    *******************************************************************************/
   protected String identityType()
   {
      return ("INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY");
   }



   /*******************************************************************************
    ** H2 needs no vendor-specific table suffix.
    *******************************************************************************/
   protected String tableSuffix()
   {
      return ("");
   }



   /*******************************************************************************
    ** Close native pools before deleting a test-owned database directory.
    *******************************************************************************/
   @AfterEach
   void close()
   {
      QContext.clear();
      ConnectionManager.closeConnectionProviders();
   }



   /*******************************************************************************
    ** Explicit synthetic initiating identity, never a worker system identity.
    *******************************************************************************/
   private static QSession session()
   {
      QSession session = new QSession().withUser(new QUser().withIdReference("actor").withFullName("Actor"));
      session.setUuid("interactive-fixture-session");
      return (session);
   }



   /*******************************************************************************
    ** Emit only the primitive storage types used by the public owner metadata.
    *******************************************************************************/
   protected String type(QFieldMetaData field)
   {
      return (switch(field.getType())
      {
         case INTEGER -> "INTEGER";
         case LONG -> "BIGINT";
         case BOOLEAN -> "BOOLEAN";
         case DATE_TIME -> "TIMESTAMP";
         case STRING -> "VARCHAR(" + (field.getMaxLength() == null ? 255 : field.getMaxLength()) + ")";
         case TEXT -> "CLOB";
         default -> throw new IllegalArgumentException(field.getType().name());
      });
   }



   /*******************************************************************************
    ** Observe committed state through a separate physical connection.
    *******************************************************************************/
   protected int count(String table) throws Exception
   {
      try(Connection connection = openConnection(); Statement statement = connection.createStatement();
          ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM " + table))
      {
         rows.next();
         return (rows.getInt(1));
      }
   }



   /*******************************************************************************
    ** All intent rows stay invisible until the caller commits and survive reopen.
    *******************************************************************************/
   @Test
   void sameCommitContainsSourceJobChunkGrantAndEvent() throws Exception
   {
      try(QBackendTransaction transaction = QBackendTransaction.openFor(new InsertInput("source_record")))
      {
         new InsertAction().execute(new InsertInput("source_record").withTransaction(transaction).withRecord(new QRecord().withValue("value", "one")));
         assertNotNull(enlistedId);
         assertEquals(0, count("source_record"));
         assertEquals(0, count("jobs_job"));
         assertEquals(0, count("jobs_chunk"));
         assertEquals(0, count("jobs_event"));
         transaction.commit();
      }
      ConnectionManager.closeConnectionProviders();
      assertEquals(1, count("source_record"));
      assertEquals(1, count("jobs_job"));
      assertEquals(1, count("jobs_chunk"));
      assertEquals(1, count("jobs_event"));
      assertTrue(store.getJob(enlistedId).getValueString("credentialScopeJson").contains("__grant"));
   }



   /*******************************************************************************
    ** Caught listener failure makes a later explicit commit impossible.
    *******************************************************************************/
   @Test
   void caughtFailureRollsBackAllIntent() throws Exception
   {
      failAfter = true;
      try(QBackendTransaction transaction = QBackendTransaction.openFor(new InsertInput("source_record")))
      {
         assertThrows(com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeListenerHelper.RequiredListenerException.class, () -> new InsertAction().execute(new InsertInput("source_record").withTransaction(transaction)
            .withRecord(new QRecord().withValue("value", "one"))));
         assertTrue(transaction.isRollbackOnly());
         assertThrows(QException.class, transaction::commit);
         transaction.rollback();
      }
      assertEquals(0, count("source_record"));
      assertEquals(0, count("jobs_job"));
      assertEquals(0, count("jobs_chunk"));
      assertEquals(0, count("jobs_event"));
   }



   /*******************************************************************************
    ** Same active event/request deduplicates; changed captured authority conflicts.
    *******************************************************************************/
   @Test
   void duplicateIntentIsExactAndChangedScopeDoesNotCreateAnotherJob() throws Exception
   {
      twice = true;
      new InsertAction().execute(new InsertInput("source_record").withRecord(new QRecord().withValue("value", "one")));
      assertEquals(1, count("jobs_job"));
      changeScope = true;
      assertThrows(com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeListenerHelper.RequiredListenerException.class, () -> new InsertAction().execute(new InsertInput("source_record").withRecord(new QRecord().withValue("value", "two"))));
      assertEquals(1, count("jobs_job"));
      assertEquals(1, count("source_record"));
   }



   /*******************************************************************************
    ** Shared guard serializes competing native inserts at a one-job waiting cap.
    *******************************************************************************/
   @Test
   void concurrentInsertionsCannotExceedQueueCapacity() throws Exception
   {
      settings.withMaxQueuedJobsPerUser(1);
      CountDownLatch ready = new CountDownLatch(2);
      CountDownLatch start = new CountDownLatch(1);
      AtomicInteger accepted = new AtomicInteger();
      try(var executor = Executors.newFixedThreadPool(2))
      {
         List<java.util.concurrent.Future<?>> futures = new ArrayList<>();
         for(int index = 0; index < 2; index++)
         {
            futures.add(executor.submit(() ->
            {
               QContext.init(instance, session());
               ready.countDown();
               try
               {
                  assertTrue(start.await(5, TimeUnit.SECONDS));
                  store.insertJob(job("QUEUED"), null);
                  accepted.incrementAndGet();
               }
               catch(com.kingsrook.qqq.backend.core.exceptions.QTooManyRequestsException expected)
               {
                  // The contender observes the committed reservation under the guard.
               }
               catch(Exception error)
               {
                  throw new IllegalStateException(error);
               }
               finally
               {
                  QContext.clear();
               }
            }));
         }
         assertTrue(ready.await(5, TimeUnit.SECONDS));
         start.countDown();
         for(var future : futures)
         {
            future.get(15, TimeUnit.SECONDS);
         }
      }
      assertEquals(1, accepted.get());
      assertEquals(1, count("jobs_job"));
   }



   /*******************************************************************************
    ** Resume/retry refusal preserves state, chunks and the existing signed grant.
    *******************************************************************************/
   @Test
   void refusedResumeAndRetryDoNotChangeState() throws Exception
   {
      settings.withMaxQueuedJobsPerUser(1);
      store.insertJob(job("QUEUED"), null);
      Integer paused = store.insertJob(job("PAUSED").withValue("credentialScopeJson", "preserved"), null);
      assertThrows(QException.class, () -> DurableJobs.control(paused, JobAction.RESUME));
      assertEquals("PAUSED", store.getJob(paused).getValueString("status"));
      assertEquals("preserved", store.getJob(paused).getValueString("credentialScopeJson"));
      Integer failed = store.insertJob(job("FAILED"), null);
      store.insertChunks(List.of(new QRecord().withValue("jobId", failed).withValue("seq", 0).withValue("status", "FAILED")), null);
      assertThrows(QException.class, () -> DurableJobs.control(failed, JobAction.RETRY_FAILED));
      assertEquals("FAILED", store.getChunks(failed).get(0).getValueString("status"));
      assertEquals("FAILED", store.getJob(failed).getValueString("status"));
   }



   /*******************************************************************************
    ** On real JDBC with admission on, a refused control (the job already moved on)
    ** rolls back inside the control transaction: failed chunks stay failed.
    *******************************************************************************/
   @Test
   void refusedRetryOfCancelledJobLeavesChunksUntouched() throws Exception
   {
      Integer cancelled = store.insertJob(job("CANCELLED"), null);
      store.insertChunks(List.of(new QRecord().withValue("jobId", cancelled).withValue("seq", 0).withValue("status", "FAILED").withValue("attempt", 3)), null);
      assertThrows(QException.class, () -> DurableJobs.control(cancelled, JobAction.RETRY_FAILED));
      assertEquals("FAILED", store.getChunks(cancelled).get(0).getValueString("status"));
      assertEquals(3, store.getChunks(cancelled).get(0).getValueInteger("attempt"));
      assertEquals("CANCELLED", store.getJob(cancelled).getValueString("status"));
   }



   /*******************************************************************************
    ** Required audit refusal rolls back the status transition on real JDBC.
    *******************************************************************************/
   @Test
   void requiredEventFailureDoesNotCommitStatus() throws Exception
   {
      Integer id = store.insertJob(job("PAUSED"), null);
      try(Connection connection = openConnection(); Statement statement = connection.createStatement())
      {
         statement.execute("ALTER TABLE jobs_event ADD CONSTRAINT refuse_resume CHECK(type <> 'RESUMED')");
      }
      assertThrows(QException.class, () -> store.setJobStatusWithEvent(id, Set.of(JobStatus.PAUSED), JobStatus.QUEUED, Map.of(), JobEventType.RESUMED, "resume"));
      assertEquals("PAUSED", store.getJob(id).getValueString("status"));
      assertEquals(0, count("jobs_event"));
   }



   /*******************************************************************************
    ** Mapping weakening and case-folded actor storage fail before insertion.
    *******************************************************************************/
   @Test
   void refusesWeakenedMetadataAndCaseInsensitiveActorColumn() throws Exception
   {
      QFieldMetaData owner = instance.getTable(DurableJobsTables.TABLE_JOB).getField("userId");
      String original = owner.getBackendName();
      owner.setBackendName("client_channel");
      assertThrows(QException.class, () -> store.insertJob(job("QUEUED"), null));
      owner.setBackendName(original);
      try(Connection connection = openConnection(); Statement statement = connection.createStatement())
      {
         statement.execute("ALTER TABLE jobs_job ALTER COLUMN user_id VARCHAR_IGNORECASE(250)");
      }
      assertThrows(QException.class, () -> store.insertJob(job("QUEUED"), null));
      assertEquals(0, count("jobs_job"));
   }



   /*******************************************************************************
    ** An enlisted row can never fall back to a legacy automated user identity.
    *******************************************************************************/
   @Test
   void missingEnlistedGrantRefusesWorkerRestoration() throws Exception
   {
      QRecord job = job("QUEUED").withValue("id", 1).withValue("enlistmentKey", "opaque");
      assertThrows(QException.class, () -> JobSessions.forJob(instance, job));
   }



   /*******************************************************************************
    ** Ordinary launch and native enlisted launch share the same one-job budget.
    *******************************************************************************/
   @Test
   void ordinaryAndEnlistedLaunchCompeteForOneReservation() throws Exception
   {
      settings.withMaxQueuedJobsPerUser(1);
      assertEquals(1, race(List.of(
         () ->
         {
            DurableJobs.launch(new com.kingsrook.qqq.backend.core.jobs.model.LaunchRequest("dispatch"));
            return true;
         },
         () ->
         {
            new InsertAction().execute(new InsertInput("source_record").withRecord(new QRecord().withValue("value", "race")));
            return true;
         })));
      assertEquals(1, count("jobs_job"));
      assertEquals(1, count("jobs_chunk"));
   }



   /*******************************************************************************
    ** A concurrent resume cannot reserve the same waiting slot as an ordinary launch.
    *******************************************************************************/
   @Test
   void ordinaryLaunchAndResumeShareCapacity() throws Exception
   {
      settings.withMaxQueuedJobsPerUser(1);
      Integer id = store.insertJob(job("PAUSED"), null);
      store.insertChunks(List.of(new QRecord().withValue("jobId", id).withValue("seq", 0).withValue("status", "PENDING").withValue("leaseVersion", 0)), null);
      assertEquals(1, race(List.of(
         () ->
         {
            DurableJobs.launch(new com.kingsrook.qqq.backend.core.jobs.model.LaunchRequest("dispatch"));
            return true;
         },
         () ->
         {
            DurableJobs.control(id, JobAction.RESUME);
            return true;
         })));
      assertEquals(1, store.query(DurableJobsTables.TABLE_JOB, new com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter(
         new com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria("status", com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.EQUALS, "QUEUED"))).size());
   }



   /*******************************************************************************
    ** A caught invalid event cannot be restored to hide the failed enlistment.
    *******************************************************************************/
   @Test
   void caughtMalformedRestoredEventStillPoisonsTransaction() throws Exception
   {
      restoredMalformed = true;
      try(QBackendTransaction transaction = QBackendTransaction.openFor(new InsertInput("source_record")))
      {
         assertThrows(com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeListenerHelper.RequiredListenerException.class, () -> new InsertAction().execute(new InsertInput("source_record").withTransaction(transaction)
            .withRecord(new QRecord().withValue("value", "one"))));
         assertTrue(transaction.isRollbackOnly());
         assertThrows(QException.class, transaction::commit);
         transaction.rollback();
      }
      assertEquals(0, count("source_record"));
      assertEquals(0, count("jobs_job"));
   }



   /*******************************************************************************
    ** Positive execution membership is guarded centrally, self-transitions are not
    ** double charged, and a different process still observes the owner's budget.
    *******************************************************************************/
   @Test
   void executionMembershipCannotBypassCapacityThroughGenericUpdate() throws Exception
   {
      settings.withMaxActiveJobsPerUser(1).withDefaultMaxConcurrentJobsPerProcess(1);
      Integer first = store.insertJob(job("RUNNING"), null);
      store.updateJob(first, Map.of("status", "RUNNING"));
      Integer second = store.insertJob(job("NEEDS_REVIEW"), null);
      assertThrows(QException.class, () -> store.updateJob(second, Map.of("status", "RUNNING")));
      assertThrows(QException.class, () -> store.insertJob(job("RUNNING").withValue("processName", "anotherProcess"), null));
      assertEquals("NEEDS_REVIEW", store.getJob(second).getValueString("status"));
   }



   /*******************************************************************************
    ** Two pods cannot claim two chunks when the job admits one parallel chunk.
    *******************************************************************************/
   @Test
   void concurrentClaimersRespectChunkParallelism() throws Exception
   {
      Integer id = store.insertJob(job("QUEUED"), null);
      for(int seq = 0; seq < 2; seq++)
      {
         store.insertChunks(List.of(new QRecord().withValue("jobId", id).withValue("seq", seq).withValue("status", "PENDING")
            .withValue("leaseVersion", 0).withValue("attempt", 0).withValue("maxAttempts", 1)), null);
      }
      assertEquals(1, race(List.of(
         () -> new com.kingsrook.qqq.backend.core.jobs.runtime.JobClaimer(store, "pod-a").claimNext().isPresent(),
         () -> new com.kingsrook.qqq.backend.core.jobs.runtime.JobClaimer(store, "pod-b").claimNext().isPresent())));
      assertEquals(1, store.getChunks(id).stream().filter(chunk -> "RUNNING".equals(chunk.getValueString("status"))).count());
   }



   /*******************************************************************************
    ** Process job capacity is counted under the guard, independently of chunk caps.
    *******************************************************************************/
   @Test
   void concurrentClaimersRespectProcessCapacity() throws Exception
   {
      settings.withDefaultMaxConcurrentJobsPerProcess(1).withMaxActiveJobsPerUser(10);
      for(int index = 0; index < 2; index++)
      {
         Integer id = store.insertJob(job("QUEUED"), null);
         store.insertChunks(List.of(new QRecord().withValue("jobId", id).withValue("seq", 0).withValue("status", "PENDING")
            .withValue("leaseVersion", 0).withValue("attempt", 0).withValue("maxAttempts", 1)), null);
      }
      assertEquals(1, race(List.of(
         () -> new com.kingsrook.qqq.backend.core.jobs.runtime.JobClaimer(store, "pod-a").claimNext().isPresent(),
         () -> new com.kingsrook.qqq.backend.core.jobs.runtime.JobClaimer(store, "pod-b").claimNext().isPresent())));
   }



   /*******************************************************************************
    ** Run actual concurrent native calls with separate initiating thread contexts.
    ** Only explicit capacity refusals count as rejected admission.
    *******************************************************************************/
   private int race(List<java.util.concurrent.Callable<Boolean>> operations) throws Exception
   {
      CountDownLatch ready = new CountDownLatch(operations.size());
      CountDownLatch start = new CountDownLatch(1);
      int successes = 0;
      try(var executor = Executors.newFixedThreadPool(operations.size()))
      {
         List<java.util.concurrent.Future<Boolean>> futures = new ArrayList<>();
         for(var operation : operations)
         {
            futures.add(executor.submit(() ->
            {
               QContext.init(instance, session());
               ready.countDown();
               try
               {
                  assertTrue(start.await(5, TimeUnit.SECONDS));
                  return (operation.call());
               }
               catch(Exception error)
               {
                  Throwable cause = error;
                  while(cause != null)
                  {
                     if(cause instanceof com.kingsrook.qqq.backend.core.exceptions.QTooManyRequestsException)
                     {
                        return (false);
                     }
                     cause = cause.getCause();
                  }
                  throw (error);
               }
               finally
               {
                  QContext.clear();
               }
            }));
         }
         assertTrue(ready.await(5, TimeUnit.SECONDS));
         start.countDown();
         for(var future : futures)
         {
            if(future.get(20, TimeUnit.SECONDS))
            {
               successes++;
            }
         }
      }
      return (successes);
   }



   /*******************************************************************************
    ** Missing signing configuration refuses the whole intent after the source write.
    *******************************************************************************/
   @Test
   void missingGrantKeyRollsBackSourceAndIntent() throws Exception
   {
      settings.withJobGrantKey(null);
      assertThrows(com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeListenerHelper.RequiredListenerException.class,
         () -> new InsertAction().execute(new InsertInput("source_record").withRecord(new QRecord().withValue("value", "one"))));
      assertEquals(0, count("source_record"));
      assertEquals(0, count("jobs_job"));
      assertEquals(0, count("jobs_chunk"));
      assertEquals(0, count("jobs_event"));
   }



   /*******************************************************************************
    ** A pinned host revision cannot silently execute its later replacement.
    *******************************************************************************/
   @Test
   void changedProcessRevisionRefusesBeforeWorkerExecution() throws Exception
   {
      new InsertAction().execute(new InsertInput("source_record").withRecord(new QRecord().withValue("value", "one")));
      QRecord saved = store.getJob(enlistedId);
      DurableJobs.requireEnlistedProcessRevision(saved, instance.getProcess("dispatch"));
      DurableProcessMetaData.of(instance.getProcess("dispatch")).withTransactionalRevision("v2");
      assertThrows(QException.class, () -> DurableJobs.requireEnlistedProcessRevision(saved, instance.getProcess("dispatch")));
   }



   /*******************************************************************************
    ** Effect-review capacity refusal rolls back the already proposed resolution.
    *******************************************************************************/
   @Test
   void effectRecoveryRefusalPreservesEffectAndJob() throws Exception
   {
      settings.withMaxActiveJobsPerUser(1).withDefaultMaxConcurrentJobsPerProcess(1);
      store.insertJob(job("RUNNING"), null);
      Integer id = store.insertJob(job("NEEDS_REVIEW"), null);
      Integer effectId = com.kingsrook.qqq.backend.core.jobs.SystemScope.call(() -> new InsertAction().execute(new InsertInput(DurableJobsTables.TABLE_EFFECT)
         .withRecord(new QRecord().withValue("jobId", id).withValue("status", "UNKNOWN").withValue("needsReview", true)
            .withValue("leaseVersion", 0).withValue("idempotencyKey", "effect-fixture"))).getRecords().get(0).getValueInteger("id"));
      RunBackendStepInput input = new RunBackendStepInput();
      input.setProcessName("jobsEffectResend");
      input.setRecords(List.of(new QRecord().withValue("id", effectId)));
      assertThrows(QException.class, () -> new com.kingsrook.qqq.backend.core.jobs.processes.JobEffectReviewProcesses.Load().runOnePage(input, new RunBackendStepOutput()));
      assertEquals("UNKNOWN", new com.kingsrook.qqq.backend.core.jobs.effects.EffectStore(settings).get(effectId).getValueString("status"));
      assertEquals("NEEDS_REVIEW", store.getJob(id).getValueString("status"));
   }



   /*******************************************************************************
    ** Cancellation of waiting work releases capacity and cannot need a new slot.
    *******************************************************************************/
   @Test
   void fullExecutionCapacityDoesNotPreventQueuedOrPausedCancellation() throws Exception
   {
      settings.withMaxActiveJobsPerUser(1).withDefaultMaxConcurrentJobsPerProcess(1);
      store.insertJob(job("RUNNING"), null);
      for(String status : List.of("QUEUED", "PAUSED"))
      {
         Integer id = store.insertJob(job(status), null);
         DurableJobs.control(id, JobAction.CANCEL);
         assertEquals("CANCELLED", store.getJob(id).getValueString("status"));
      }
   }



   /*******************************************************************************
    ** Retry keeps the reservation while another chunk runs; cancellation remains
    ** possible at full capacity and does not discard that live chunk's fence.
    *******************************************************************************/
   @Test
   void retryRetainsExecutionReservationAndAllowsRunningCancellation() throws Exception
   {
      settings.withMaxActiveJobsPerUser(1).withDefaultMaxConcurrentJobsPerProcess(1);
      Integer id = store.insertJob(job("RUNNING"), null);
      store.insertChunks(List.of(new QRecord().withValue("jobId", id).withValue("seq", 0).withValue("status", "RUNNING")
         .withValue("leaseToken", "live-fence").withValue("leaseVersion", 1),
         new QRecord().withValue("jobId", id).withValue("seq", 1).withValue("status", "FAILED")), null);
      DurableJobs.control(id, JobAction.RETRY_FAILED);
      assertEquals("RUNNING", store.getJob(id).getValueString("status"));
      assertThrows(QException.class, () -> store.updateJob(id, Map.of("status", "QUEUED")));
      DurableJobs.control(id, JobAction.CANCEL);
      assertEquals("CANCELLING", store.getJob(id).getValueString("status"));
      assertEquals("live-fence", store.getChunks(id).get(0).getValueString("leaseToken"));
   }



   /*******************************************************************************
    ** Assistant matching follows canonical channel names and refuses unmigrated
    ** counted rows rather than overlooking mixed-case historical origin strings.
    *******************************************************************************/
   @Test
   void canonicalAssistantBudgetDoesNotMatchAssistant2OrMissLegacyCase() throws Exception
   {
      settings.withAssistantMaxActiveJobs(1);
      store.insertJob(job("QUEUED").withValue("clientChannel", "assistant2/turn"), null);
      store.insertJob(job("QUEUED").withValue("clientChannel", "assistant/turn"), null);
      assertThrows(QException.class, () -> store.insertJob(job("QUEUED").withValue("clientChannel", "assistant:second"), null));
      assertThrows(QException.class, () -> store.insertJob(job("QUEUED").withValue("clientChannel", "Assistant/turn"), null));
      assertThrows(QException.class, () -> store.insertJob(job("QUEUED").withValue("clientChannel", " assistant/turn"), null));
      settings.withTransactionalAdmissionEnabled(false);
      store.insertJob(job("QUEUED").withValue("clientChannel", "Assistant/old"), null);
      settings.withTransactionalAdmissionEnabled(true);
      assertThrows(QException.class, () -> store.insertJob(job("QUEUED"), null));
      assertEquals(3, count("jobs_job"));
   }



   /*******************************************************************************
    ** New metadata requires the additive columns even with strict mode disabled;
    ** applying the shipped migration preserves nullable fields on existing rows.
    *******************************************************************************/
   @Test
   void additiveMigrationPreservesOldRowsWithAdmissionDisabled() throws Exception
   {
      settings.withTransactionalAdmissionEnabled(false);
      Set<String> added = Set.of("enlistmentKey", "intentFingerprint", "processRevision");
      QTableMetaData table = instance.getTable(DurableJobsTables.TABLE_JOB);
      try(Connection connection = openConnection(); Statement statement = connection.createStatement())
      {
         statement.execute("DROP TABLE jobs_job");
         statement.execute("DROP TABLE jobs_admission");
         List<String> columns = new ArrayList<>();
         for(QFieldMetaData field : table.getFields().values())
         {
            if(!added.contains(field.getName()))
            {
               columns.add(quote(field.getBackendName()) + " " + (field.getName().equals("id") ? identityType() : type(field)));
            }
         }
         statement.execute("CREATE TABLE jobs_job(" + String.join(",", columns) + ")" + tableSuffix());
         statement.execute("INSERT INTO jobs_job(label,process_name,status,user_id) VALUES('Old job','dispatch','QUEUED','actor')");
      }
      assertThrows(QException.class, () -> store.getJob(1));
      String script = java.nio.file.Files.readString(Path.of(System.getProperty("basedir")).getParent()
         .resolve("docs/transactional-jobs").resolve(migrationScript())).replaceAll("(?m)^--.*$", "");
      try(Connection connection = openConnection(); Statement statement = connection.createStatement())
      {
         for(String sql : script.split(";"))
         {
            if(!sql.isBlank())
            {
               statement.execute(sql);
            }
         }
      }
      QRecord old = store.getJob(1);
      assertEquals("Old job", old.getValueString("label"));
      org.junit.jupiter.api.Assertions.assertNull(old.getValue("enlistmentKey"));
      assertNotNull(store.insertJob(job("QUEUED"), null));
      assertEquals(2, count("jobs_job"));
   }



   /*******************************************************************************
    ** Shipped additive migration for this native provider.
    *******************************************************************************/
   protected String migrationScript()
   {
      return ("h2-additive.sql");
   }



   /*******************************************************************************
    ** Key rotation conflicts with the prior stable operation identity; it neither
    ** creates a replacement intent nor silently refreshes its initiating grant.
    *******************************************************************************/
   @Test
   void rotatedKeyFindsPriorOperationAndRefusesRebinding() throws Exception
   {
      instance.getTable("source_record").withSupplementalMetaData(new TransactionalRecordChangeMetaData(
         new QCodeReference(Listener.class), Set.of(RecordChangeType.UPDATE)));
      Integer sourceId = new InsertAction().execute(new InsertInput("source_record").withRecord(new QRecord().withValue("value", "same"))).getRecords().get(0).getValueInteger("id");
      operationKey = "stable-update-operation";
      new com.kingsrook.qqq.backend.core.actions.tables.UpdateAction().execute(new com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput("source_record")
         .withRecord(new QRecord().withValue("id", sourceId).withValue("value", "same")));
      Integer firstId = enlistedId;
      String firstGrant = store.getJob(firstId).getValueString("credentialScopeJson");
      byte[] rotated = new byte[32];
      rotated[0] = 1;
      settings.withJobGrantKey(Base64.getEncoder().encodeToString(rotated));
      assertThrows(com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeListenerHelper.RequiredListenerException.class,
         () -> new com.kingsrook.qqq.backend.core.actions.tables.UpdateAction().execute(new com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput("source_record")
            .withRecord(new QRecord().withValue("id", sourceId).withValue("value", "same"))));
      assertEquals(1, count("jobs_job"));
      assertEquals(firstGrant, store.getJob(firstId).getValueString("credentialScopeJson"));
   }



   /*******************************************************************************
    ** Strict storage must match native UTC zone-less timestamp binding; accepting
    ** zoned columns would shift leases on non-UTC hosts.
    *******************************************************************************/
   @Test
   void incompatibleTemporalStorageRefusesBeforeSourceMutation() throws Exception
   {
      try(Connection connection = openConnection(); Statement statement = connection.createStatement())
      {
         statement.execute(incompatibleTemporalStorageSql());
      }
      assertThrows(com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeListenerHelper.RequiredListenerException.class,
         () -> new InsertAction().execute(new InsertInput("source_record").withRecord(new QRecord().withValue("value", "refuse"))));
      assertEquals(0, count("source_record"));
      assertEquals(0, count("jobs_job"));
   }



   /*******************************************************************************
    ** Provider-specific actual DDL incompatible with native DATE_TIME semantics.
    *******************************************************************************/
   protected String incompatibleTemporalStorageSql()
   {
      return ("ALTER TABLE jobs_chunk ALTER COLUMN lease_expires_at TIMESTAMP WITH TIME ZONE");
   }



   /*******************************************************************************
    ** Native insertion, conditional claim, native update and native read agree on
    ** the same instant. The database clock and JVM need not share a timezone.
    *******************************************************************************/
   @Test
   void nativeAndConditionalTimestampPathsPreserveLeaseInstants() throws Exception
   {
      new InsertAction().execute(new InsertInput("source_record").withRecord(new QRecord().withValue("value", "clock")));
      QRecord chunk = store.getChunks(enlistedId).get(0);
      var lease = store.claimChunk(chunk, "clock-fixture", 60, null).orElseThrow();
      assertEquals(lease.expiresAt().truncatedTo(java.time.temporal.ChronoUnit.MICROS),
         store.getChunk(lease.chunkId()).getValueInstant("leaseExpiresAt").truncatedTo(java.time.temporal.ChronoUnit.MICROS));
      java.time.Instant expired = store.now().minusSeconds(1).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
      Step.writeChunk(lease.chunkId(), "leaseExpiresAt", expired);
      assertEquals(expired, store.getChunk(lease.chunkId()).getValueInstant("leaseExpiresAt"));
      assertTrue(store.getChunk(lease.chunkId()).getValueInstant("leaseExpiresAt").isBefore(store.now()));
   }



   /*******************************************************************************
    ** One exact event has a stable scope-owned identity; later identical updates
    ** remain distinct logical events without a consumer-side registry.
    *******************************************************************************/
   @Test
   void scopeEventIdentityIsStableWithinEventAndDistinctAcrossIdenticalMutations() throws Exception
   {
      instance.getTable("source_record").withSupplementalMetaData(new TransactionalRecordChangeMetaData(
         new QCodeReference(Listener.class), Set.of(RecordChangeType.UPDATE)));
      Integer id = new InsertAction().execute(new InsertInput("source_record").withRecord(new QRecord().withValue("value", "same"))).getRecords().get(0).getValueInteger("id");
      twice = true;
      for(int index = 0; index < 2; index++)
      {
         new com.kingsrook.qqq.backend.core.actions.tables.UpdateAction().execute(new com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput("source_record")
            .withRecord(new QRecord().withValue("id", id).withValue("value", "same")));
      }
      assertEquals(2, observedEventIds.size());
      assertFalse(observedEventIds.get(0).equals(observedEventIds.get(1)));
      assertEquals(2, count("jobs_job"));
   }



   /*******************************************************************************
    ** Actual worker restores the initiating sign-in and exposes only committed,
    ** immutable dispatch data. Direct and ordinary launch cannot claim enlistment.
    *******************************************************************************/
   @Test
   void runtimeVerifierDistinguishesEnlistedFromOrdinaryAndDirectExecution() throws Exception
   {
      instance.getAuthentication().withModuleCodeReference(new QCodeReference(WorkerAuthentication.class));
      settings.withWorkerThreads(1).withPollIntervalMillis(25);
      verifyExecution = true;
      assertThrows(QException.class, () -> DurableJobs.requireEnlistedExecution("dispatch", "v1"));
      new InsertAction().execute(new InsertInput("source_record").withRecord(new QRecord().withValue("value", "safe")));
      Integer committed = enlistedId;
      com.kingsrook.qqq.backend.core.jobs.runtime.DurableJobsRuntime.start(instance);
      try
      {
         awaitTerminal(committed);
         assertEquals("COMPLETED", store.getJob(committed).getValueString("status"));
         assertNotNull(verifiedExecution);
         assertEquals(committed, verifiedExecution.jobId());
         assertEquals("actor", verifiedExecution.actorId());
         assertEquals("INSERT", verifiedExecution.eventType());
         assertEquals("source_record", verifiedExecution.sourceTable());
         assertEquals(Map.of("source", "fixture"), verifiedExecution.parameters());
         assertThrows(UnsupportedOperationException.class, () -> verifiedExecution.parameters().put("spoof", "value"));
         verifiedExecution = null;
         Integer ordinary = DurableJobs.launch(new com.kingsrook.qqq.backend.core.jobs.model.LaunchRequest("dispatch"));
         awaitTerminal(ordinary);
         assertEquals("FAILED", store.getJob(ordinary).getValueString("status"));
         org.junit.jupiter.api.Assertions.assertNull(verifiedExecution);
      }
      finally
      {
         com.kingsrook.qqq.backend.core.jobs.runtime.DurableJobsRuntime.stop(instance, java.time.Duration.ofSeconds(5));
      }
      assertThrows(QException.class, () -> DurableJobs.requireEnlistedExecution("dispatch", "v1"));
   }



   /*******************************************************************************
    ** Every claimed verifier boundary is exercised inside the real native worker.
    ** Restore injected fixture corruption so normal failure bookkeeping can finish.
    *******************************************************************************/
   @Test
   void runtimeVerifierRefusesAuthorityRevisionAndLeaseChangesAndClearsAfterFailure() throws Exception
   {
      instance.getAuthentication().withModuleCodeReference(new QCodeReference(WorkerAuthentication.class));
      settings.withWorkerThreads(1).withPollIntervalMillis(25);
      verifyExecution = true;
      com.kingsrook.qqq.backend.core.jobs.runtime.DurableJobsRuntime.start(instance);
      try
      {
         for(String mutation : List.of("actor", "scope", "revision", "token", "expiry", "throw"))
         {
            verifierMutation = mutation;
            observedVerifierRefusal = null;
            verifiedExecution = null;
            new InsertAction().execute(new InsertInput("source_record").withRecord(new QRecord().withValue("value", mutation)));
            Integer id = enlistedId;
            awaitTerminal(id);
            assertEquals("FAILED", store.getJob(id).getValueString("status"), mutation);
            org.junit.jupiter.api.Assertions.assertNull(verifiedExecution, mutation);
            if(!"throw".equals(mutation))
            {
               assertEquals(mutation, observedVerifierRefusal, "Verifier branch must be reached: " + mutation);
            }
         }
         verifierMutation = null;
         Integer ordinary = DurableJobs.launch(new com.kingsrook.qqq.backend.core.jobs.model.LaunchRequest("dispatch"));
         awaitTerminal(ordinary);
         assertEquals("FAILED", store.getJob(ordinary).getValueString("status"));
         org.junit.jupiter.api.Assertions.assertNull(verifiedExecution);
      }
      finally
      {
         com.kingsrook.qqq.backend.core.jobs.runtime.DurableJobsRuntime.stop(instance, java.time.Duration.ofSeconds(5));
      }
   }



   /*******************************************************************************
    ** Bounded polling observes actual native worker completion, not guessed sleep.
    *******************************************************************************/
   private void awaitTerminal(Integer id) throws Exception
   {
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
      while(System.nanoTime() < deadline)
      {
         if(Set.of("COMPLETED", "FAILED", "NEEDS_REVIEW", "CANCELLED").contains(store.getJob(id).getValueString("status")))
         {
            return;
         }
         Thread.sleep(25);
      }
      throw (new AssertionError("Native worker did not finish within the fixture deadline"));
   }



   /*******************************************************************************
    ** Synthetic fixture provider reconstructs only its exact initiating sign-in.
    ** This tests the real runtime path, not a production identity integration.
    *******************************************************************************/
   public static class WorkerAuthentication extends com.kingsrook.qqq.backend.core.modules.authentication.implementations.MockAuthenticationModule
   {
      /*******************************************************************************
       ** Accept only the fixture's established session identity.
       *******************************************************************************/
      @Override
      public QSession createSession(QInstance instance, Map<String, String> context) throws com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException
      {
         QSession restored = session();
         if(!restored.getUuid().equals(context.get("sessionUUID")))
         {
            throw (new com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException("Fixture sign-in unavailable"));
         }
         return (restored);
      }
   }



   /*******************************************************************************
    ** Minimal trusted owner row for admission-only fixtures.
    *******************************************************************************/
   protected static QRecord job(String status)
   {
      return (new QRecord().withValue("label", "Fixture").withValue("processName", "dispatch").withValue("status", status)
         .withValue("userId", "actor").withValue("sourceType", "STEPS").withValue("mode", "EXECUTE").withValue("maxParallelChunks", 1));
   }



   /*******************************************************************************
    ** Trusted required listener creates the real enlisted intent in native actions.
    *******************************************************************************/
   public static class Listener implements RecordChangeListenerInterface
   {
      /*******************************************************************************
       ** Apply only to the fixture source insertion.
       *******************************************************************************/
      @Override
      public boolean appliesTo(String table, RecordChangeType type)
      {
         return ("source_record".equals(table));
      }



      /*******************************************************************************
       ** Exercise exact active event identity, immutable request and atomic refusal.
       *******************************************************************************/
      @Override
      public void onRecordsChanged(RecordChangeEvent event) throws QException
      {
         String eventId = com.kingsrook.qqq.backend.core.actions.tables.listeners.TransactionalRecordChangeScope.admittedEvent(event).eventId();
         assertEquals(eventId, com.kingsrook.qqq.backend.core.actions.tables.listeners.TransactionalRecordChangeScope.admittedEvent(event).eventId());
         observedEventIds.add(eventId);
         FixedProcessRequest request = new FixedProcessRequest("dispatch", operationKey == null ? eventId : operationKey, Map.of("source", "fixture"));
         enlistedId = DurableJobs.enlistFixedProcess(event, request);
         if(restoredMalformed)
         {
            QRecord record = event.getRecords().get(0);
            java.io.Serializable original = record.getValue("value");
            record.setValue("value", "x".repeat(65537));
            try
            {
               assertThrows(QException.class, () -> DurableJobs.enlistFixedProcess(event, request));
            }
            finally
            {
               record.setValue("value", original);
            }
         }
         if(twice)
         {
            if(changeScope)
            {
               QContext.getQSession().getUser().setFullName("Changed Name");
            }
            assertEquals(enlistedId, DurableJobs.enlistFixedProcess(event, request));
         }
         if(failAfter)
         {
            throw (new QException("Injected listener failure"));
         }
      }
   }



   /*******************************************************************************
    ** Trusted fixed process fixture; worker execution qualification is separate.
    *******************************************************************************/
   public static class Step implements BackendStep
   {
      /*******************************************************************************
       ** This intent fixture has no external effects.
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         if(verifyExecution)
         {
            if(verifierMutation != null)
            {
               refuseMutation(verifierMutation);
               return;
            }
            input.addValue("source", "forged-process-input");
            verifiedExecution = DurableJobs.requireEnlistedExecution("dispatch", "v1");
         }
      }



      /*******************************************************************************
       ** Fault injection occurs only in this trusted test process, then restores
       ** state before native worker outcome handling. The verifier must refuse.
       *******************************************************************************/
      private static void refuseMutation(String mutation) throws QException
      {
         QSession session = QContext.getQSession();
         String actor = session.getUser().getIdReference();
         Map<String, List<java.io.Serializable>> keys = session.getSecurityKeyValues();
         DurableProcessMetaData process = DurableProcessMetaData.of(QContext.getQInstance().getProcess("dispatch"));
         com.kingsrook.qqq.backend.core.jobs.plans.JobChunkScope.ChunkIdentity identity = com.kingsrook.qqq.backend.core.jobs.plans.JobChunkScope.current().orElseThrow().getIdentity();
         DurableJobStore store = new DurableJobStore(DurableJobsMetaData.of(QContext.getQInstance()));
         QRecord chunk = store.getChunk(identity.chunkId());
         try
         {
            switch(mutation)
            {
               case "actor" -> session.getUser().setIdReference("other-actor");
               case "scope" -> session.setSecurityKeyValues(Map.of("changed", List.of("broader")));
               case "revision" -> process.withTransactionalRevision("v2");
               case "token" -> writeChunk(identity.chunkId(), "leaseToken", "replacement-fence");
               case "expiry" -> writeChunk(identity.chunkId(), "leaseExpiresAt", store.now().minusSeconds(1));
               case "throw" -> throw (new QException("Injected process exception"));
               default -> throw (new QException("Unknown fixture mutation"));
            }
            try
            {
               verifiedExecution = DurableJobs.requireEnlistedExecution("dispatch", "v1");
            }
            catch(QException refused)
            {
               String expected = "revision".equals(mutation) ? "The enlisted process revision is no longer available"
                  : "An active authenticated enlisted execution is required";
               if(expected.equals(refused.getMessage()))
               {
                  observedVerifierRefusal = mutation;
               }
               throw (refused);
            }
         }
         finally
         {
            session.getUser().setIdReference(actor);
            session.setSecurityKeyValues(keys);
            process.withTransactionalRevision("v1");
            if("token".equals(mutation))
            {
               writeChunk(identity.chunkId(), "leaseToken", chunk.getValue("leaseToken"));
            }
            if("expiry".equals(mutation))
            {
               writeChunk(identity.chunkId(), "leaseExpiresAt", chunk.getValue("leaseExpiresAt"));
            }
         }
      }



      /*******************************************************************************
       ** Exact owned fixture row update; production callers do not receive tokens.
       *******************************************************************************/
      private static void writeChunk(Integer id, String field, java.io.Serializable value) throws QException
      {
         var updated = com.kingsrook.qqq.backend.core.jobs.SystemScope.call(() -> new com.kingsrook.qqq.backend.core.actions.tables.UpdateAction().execute(
            new com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput(DurableJobsTables.TABLE_CHUNK)
               .withRecord(new QRecord().withValue("id", id).withValue(field, value))));
         if(updated.getRecords().size() != 1 || !updated.getRecords().get(0).getErrors().isEmpty())
         {
            throw (new QException("Fixture mutation was not acknowledged"));
         }
         QRecord visible = new DurableJobStore(DurableJobsMetaData.of(QContext.getQInstance())).getChunk(id);
         if(value instanceof java.time.Instant expected)
         {
            if(!expected.truncatedTo(java.time.temporal.ChronoUnit.MILLIS).equals(visible.getValueInstant(field).truncatedTo(java.time.temporal.ChronoUnit.MILLIS)))
            {
               throw (new QException("Fixture timestamp mismatch: " + expected + " versus " + visible.getValueInstant(field)));
            }
         }
         else
         {
            assertEquals(value, visible.getValue(field));
         }
      }

   }
}


