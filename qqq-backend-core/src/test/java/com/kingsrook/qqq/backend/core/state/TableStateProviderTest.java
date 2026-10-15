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

package com.kingsrook.qqq.backend.core.state;


import java.io.ObjectInputFilter;
import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobManager;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobRegistry;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobState;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobStatus;
import com.kingsrook.qqq.backend.core.actions.processes.QProcessCallback;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessState;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for TableStateProvider (and the shared-state behavior of process
 ** and async job state built on it).
 *******************************************************************************/
class TableStateProviderTest extends BaseTest
{
   private static final String TABLE = StateProviderMetaData.DEFAULT_TABLE_NAME;



   /*******************************************************************************
    ** Share state through the memory backend.
    *******************************************************************************/
   @BeforeEach
   void beforeEach()
   {
      share(QContext.getQInstance(), new StateProviderMetaData().withTableName(TABLE).withEncryptionKey(null));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      QStateProviders.clearForTests();
   }



   /*******************************************************************************
    ** Add the state table and meta data to an instance.
    *******************************************************************************/
   private static void share(QInstance instance, StateProviderMetaData metaData)
   {
      if(instance.getTable(TABLE) == null)
      {
         instance.addTable(TableStateProvider.defineTable(TABLE, TestUtils.MEMORY_BACKEND_NAME));
      }
      instance.withSupplementalMetaData(metaData);
      QStateProviders.clearForTests();
   }



   /*******************************************************************************
    ** The stored row for a key.
    *******************************************************************************/
   private static QRecord row(AbstractStateKey key) throws QException
   {
      QRecord[] result = new QRecord[1];
      QContext.withTemporaryContext(new CapturedContext(QContext.getQInstance(), new QSystemUserSession()),
         () -> result[0] = new GetAction().executeForRecord(new GetInput(TABLE).withPrimaryKey(TableStateProvider.storedKey(key))));
      return (result[0]);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPutGetRemove() throws QException
   {
      StateProviderInterface provider = QStateProviders.get();
      assertInstanceOf(TableStateProvider.class, provider);

      UUIDAndTypeStateKey key   = new UUIDAndTypeStateKey(StateType.PROCESS_STATUS);
      ProcessState        state = new ProcessState();
      state.setNextStepName("review");
      state.getValues().put("count", 42);
      state.setRecords(List.of(new QRecord().withValue("id", 1)));
      provider.put(key, state);

      QRecord stored = row(key);
      assertThat(stored.getValueString(TableStateProvider.FIELD_PAYLOAD)).startsWith("g1:");
      assertEquals("PROCESS_STATUS", stored.getValueString(TableStateProvider.FIELD_STATE_TYPE));

      ProcessState readBack = provider.get(ProcessState.class, key).orElseThrow();
      assertEquals(Optional.of("review"), readBack.getNextStepName());
      assertEquals(42, readBack.getValues().get("count"));
      assertEquals(1, readBack.getRecords().get(0).getValueInteger("id"));

      ////////////////////////////////////////////
      // a second put updates the same row     //
      ////////////////////////////////////////////
      readBack.getValues().put("count", 43);
      provider.put(key, readBack);
      assertEquals(43, provider.get(ProcessState.class, key).orElseThrow().getValues().get("count"));

      provider.remove(key);
      assertTrue(provider.get(ProcessState.class, key).isEmpty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testExpiredEntriesAreAbsentAndSwept() throws QException
   {
      StateProviderInterface provider = QStateProviders.get();
      UUIDAndTypeStateKey    key      = new UUIDAndTypeStateKey(StateType.ASYNC_JOB_STATUS);
      provider.put(key, new AsyncJobStatus());

      QRecord expired = row(key).withValue(TableStateProvider.FIELD_EXPIRES_AT, Instant.now().minus(1, ChronoUnit.MINUTES));
      QContext.withTemporaryContext(new CapturedContext(QContext.getQInstance(), new QSystemUserSession()),
         () -> new UpdateAction().execute(new UpdateInput(TABLE).withRecord(expired)));

      assertTrue(provider.get(AsyncJobStatus.class, key).isEmpty());
      provider.clean(Instant.now());
      assertEquals(null, row(key));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testEncryptionBindsTheKey() throws QException
   {
      String keyBase64 = Base64.getEncoder().encodeToString(new byte[32]);
      share(QContext.getQInstance(), new StateProviderMetaData().withTableName(TABLE).withEncryptionKey(keyBase64));

      StateProviderInterface provider = QStateProviders.get();
      UUIDAndTypeStateKey    key      = new UUIDAndTypeStateKey(StateType.PROCESS_STATUS);
      ProcessState           state    = new ProcessState();
      state.getValues().put("secretish", "value");
      provider.put(key, state);

      assertThat(row(key).getValueString(TableStateProvider.FIELD_PAYLOAD)).startsWith("e1:").doesNotContain("secretish");
      assertEquals("value", provider.get(ProcessState.class, key).orElseThrow().getValues().get("secretish"));

      //////////////////////////////////////////////////////////////////////
      // the ciphertext is bound to its key: copied to another key, it    //
      // does not decrypt (reads as absent)                               //
      //////////////////////////////////////////////////////////////////////
      StateSerialization serialization = new StateSerialization(new StateProviderMetaData().withEncryptionKey(keyBase64));
      String             payload       = row(key).getValueString(TableStateProvider.FIELD_PAYLOAD);
      assertThatThrownBy(() -> serialization.read("PROCESS_STATUS:" + UUID.randomUUID(), payload)).isInstanceOf(QException.class);
      assertThat(serialization.read(TableStateProvider.storedKey(key), payload)).isInstanceOf(ProcessState.class);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAllowListRejectsOtherClasses() throws QException
   {
      StateSerialization serialization = new StateSerialization(new StateProviderMetaData().withEncryptionKey(null));
      HashMap<String, Serializable> value = new HashMap<>();
      value.put("x", new Outsider());

      ///////////////////////////////////////////////////////////////////
      // the test class is under com.kingsrook, so build a "foreign"   //
      // allow list by reading with prefixes that exclude this package //
      ///////////////////////////////////////////////////////////////////
      byte[] bytes = StateSerialization.toBytes(value);
      assertThat(serialization.fromBytes(bytes)).isInstanceOf(HashMap.class);

      assertThat(serialization.filter(new FilterInfoStub(Thread.class))).isEqualTo(ObjectInputFilter.Status.ALLOWED);
      assertThat(serialization.filter(new FilterInfoStub(Test.class))).isEqualTo(ObjectInputFilter.Status.REJECTED);

      StateSerialization widened = new StateSerialization(new StateProviderMetaData().withEncryptionKey(null).withAllowedClassPrefixes(List.of("org.junit.")));
      assertThat(widened.filter(new FilterInfoStub(Test.class))).isEqualTo(ObjectInputFilter.Status.ALLOWED);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNonSerializableValueStaysOnThisNode()
   {
      StateProviderInterface provider = QStateProviders.get();
      UUIDAndTypeStateKey    key      = new UUIDAndTypeStateKey(StateType.PROCESS_STATUS);
      ProcessState           state    = new ProcessState();
      state.getValues().put("bad", new NotSerializableHolder());
      provider.put(key, state);

      assertTrue(provider.get(ProcessState.class, key).isPresent(), "kept in this node's memory");
   }



   /*******************************************************************************
    ** A process paused at a frontend step resumes from the shared table, and
    ** only for the session that started it.
    *******************************************************************************/
   @Test
   void testProcessResumesFromSharedStateForItsOwnerOnly() throws QException
   {
      RunProcessInput input = new RunProcessInput();
      input.setProcessName(TestUtils.PROCESS_NAME_GREET_PEOPLE_INTERACTIVE);
      input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.BREAK);
      input.setCallback(new EmptyCallback());
      input.setInputSource(QInputSource.USER);
      RunProcessOutput first = new RunProcessAction().execute(input);
      String           step  = first.getProcessState().getNextStepName().orElseThrow();

      ////////////////////////////////////////////////////////////////////
      // forget anything this "node" holds in memory: state is the table //
      ////////////////////////////////////////////////////////////////////
      InMemoryStateProvider.getInstance().clean(Instant.now().plusSeconds(60));
      assertTrue(row(new UUIDAndTypeStateKey(UUID.fromString(input.getProcessUUID()), StateType.PROCESS_STATUS)) != null);
      assertTrue(RunProcessAction.getStateForUser(input.getProcessUUID(), TestUtils.PROCESS_NAME_GREET_PEOPLE_INTERACTIVE).isPresent());

      input.setStartAfterStep(step);
      RunProcessOutput second = new RunProcessAction().execute(input);
      assertTrue(second.getException().isEmpty());
      assertThat(second.getProcessState().getNextStepName()).isPresent();

      ///////////////////////////////////////
      // another user may not read it      //
      ///////////////////////////////////////
      QContext.getQSession().setIdReference("someone-else");
      QContext.getQSession().setUuid(UUID.randomUUID().toString());
      assertThatThrownBy(() -> RunProcessAction.getStateForUser(input.getProcessUUID(), TestUtils.PROCESS_NAME_GREET_PEOPLE_INTERACTIVE))
         .isInstanceOf(QPermissionDeniedException.class);
   }



   /*******************************************************************************
    ** A RUNNING job whose node stopped heart-beating reads as failed.
    *******************************************************************************/
   @Test
   void testLostNodeJobReadsAsFailed()
   {
      UUID           jobUUID = UUID.randomUUID();
      AsyncJobStatus status  = new AsyncJobStatus();
      status.setState(AsyncJobState.RUNNING);
      status.setNodeId("gone-node");
      status.setHeartbeatAt(Instant.now().minusSeconds(600));
      QStateProviders.get().put(new UUIDAndTypeStateKey(jobUUID, StateType.ASYNC_JOB_STATUS), status);

      AsyncJobStatus read = new AsyncJobManager().getJobStatus(jobUUID.toString()).orElseThrow();
      assertEquals(AsyncJobState.ERROR, read.getState());
      assertInstanceOf(QUserFacingException.class, read.getCaughtException());
      assertEquals(AsyncJobRegistry.LOST_NODE_MESSAGE, read.getCaughtException().getMessage());

      /////////////////////////////////////////////////
      // a fresh heartbeat is still running          //
      /////////////////////////////////////////////////
      status.setHeartbeatAt(Instant.now());
      QStateProviders.get().put(new UUIDAndTypeStateKey(jobUUID, StateType.ASYNC_JOB_STATUS), status);
      assertEquals(AsyncJobState.RUNNING, new AsyncJobManager().getJobStatus(jobUUID.toString()).orElseThrow().getState());
   }



   /*******************************************************************************
    ** A cancel stored by any node reaches the running job's callback.
    *******************************************************************************/
   @Test
   void testCancelReachesTheRunningJob() throws Exception
   {
      String jobUUID = new AsyncJobManager().startJob("test", callback ->
      {
         long deadline = System.currentTimeMillis() + 10_000;
         while(!callback.wasCancelRequested() && System.currentTimeMillis() < deadline)
         {
            Thread.sleep(50);
         }
         callback.updateStatus("stopping");
         return (callback.wasCancelRequested());
      });

      ////////////////////////////////////////////////////////////////////
      // "another node": request the cancel through the store only      //
      ////////////////////////////////////////////////////////////////////
      UUIDAndTypeStateKey key    = new UUIDAndTypeStateKey(UUID.fromString(jobUUID), StateType.ASYNC_JOB_STATUS);
      AsyncJobStatus      stored = QStateProviders.get().get(AsyncJobStatus.class, key).orElseThrow();
      stored.setCancelRequested(true);
      QStateProviders.get().put(key, stored);

      long deadline = System.currentTimeMillis() + 10_000;
      while(System.currentTimeMillis() < deadline && new AsyncJobManager().getJobStatus(jobUUID).orElseThrow().getState() == AsyncJobState.RUNNING)
      {
         Thread.sleep(100);
      }
      AsyncJobStatus finished = new AsyncJobManager().getJobStatus(jobUUID).orElseThrow();
      assertEquals(AsyncJobState.COMPLETE, finished.getState());
      assertTrue(finished.getCancelRequested(), "the cancel survives the job's own later writes");
      assertFalse(AsyncJobRegistry.runningCount() > 0 && finished.getState() == AsyncJobState.RUNNING);
   }



   /*******************************************************************************
    ** A serializable test value (under com.kingsrook, so allowed by default).
    *******************************************************************************/
   private static class Outsider implements Serializable
   {
   }



   /*******************************************************************************
    ** Not serializable.
    *******************************************************************************/
   private static class NotSerializableHolder implements Serializable
   {
      @SuppressWarnings("unused")
      private final transient Object ok      = new Object();
      @SuppressWarnings({ "unused", "serial" })
      private final Object           notOkay = new Object();
   }



   /*******************************************************************************
    ** A callback with no filter and no values.
    *******************************************************************************/
   private static class EmptyCallback implements QProcessCallback
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QQueryFilter getQueryFilter()
      {
         return (new QQueryFilter());
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public Map<String, Serializable> getFieldValues(List<QFieldMetaData> fields)
      {
         return (new HashMap<>());
      }
   }



   /*******************************************************************************
    ** Filter info for one class.
    *******************************************************************************/
   private record FilterInfoStub(Class<?> serialClass) implements ObjectInputFilter.FilterInfo
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public long arrayLength()
      {
         return (-1);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public long depth()
      {
         return (1);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public long references()
      {
         return (1);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public long streamBytes()
      {
         return (100);
      }
   }
}
