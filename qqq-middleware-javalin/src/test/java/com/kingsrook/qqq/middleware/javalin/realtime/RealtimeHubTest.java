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

package com.kingsrook.qqq.middleware.javalin.realtime;


import java.io.Serializable;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.security.FieldSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.realtime.RealtimeBus;
import com.kingsrook.qqq.backend.core.realtime.RealtimeMessage;
import com.kingsrook.qqq.backend.core.realtime.RealtimeMetaData;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Hub security, replay, limits and session lifecycle without network timing.
 *******************************************************************************/
class RealtimeHubTest
{
   private QInstance instance;
   private QSession session;
   private RealtimeHub hub;
   private MutableClock clock;



   /*******************************************************************************
    ** A memory table with one ordinary field, one hidden field and one locked field.
    *******************************************************************************/
   @BeforeEach
   void setup() throws Exception
   {
      MemoryRecordStore.fullReset();
      instance = TestUtils.defineInstance();
      instance.addBackend(new QBackendMetaData().withName("rtMemory").withBackendType(MemoryBackendModule.class));
      instance.addSecurityKeyType(new QSecurityKeyType().withName("secretAccess"));
      instance.addTable(new QTableMetaData().withName("rt").withPrimaryKeyField("id").withBackendName("rtMemory")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("name", QFieldType.STRING))
         .withField(new QFieldMetaData("hidden", QFieldType.STRING).withIsHidden(true))
         .withField(new QFieldMetaData("locked", QFieldType.STRING).withFieldSecurityLock(new FieldSecurityLock().withSecurityKeyType("secretAccess").withOverrideValues(List.of("allowed")))));
      instance.withSupplementalMetaData(new RealtimeMetaData().withMaxChannelsPerConnection(2).withMaxConnectionsPerSession(2)
         .withReplaySeconds(120).withHeartbeatSeconds(25).withRecheckMinutes(1));
      new QInstanceEnricher(instance).enrich();
      session = new QSession().withUser(new QUser().withIdReference("alice"));
      QContext.init(instance, session);
      new InsertAction().execute(new InsertInput("rt").withRecord(new QRecord().withValue("id", 1).withValue("name", "visible")));
      clock = new MutableClock();
      hub = new RealtimeHub(instance, clock);
   }



   /*******************************************************************************
    ** Dispose the server and application transport.
    *******************************************************************************/
   @AfterEach
   void close()
   {
      if(hub != null)
      {
         hub.close();
      }
      RealtimeBus.close(instance);
      QContext.clear();
   }



   /*******************************************************************************
    ** Only matching channels receive data, with frontend field visibility applied.
    *******************************************************************************/
   @Test
   void deliverySecurityAndOwnership() throws Exception
   {
      var connection = hub.connect(session, null);
      assertEquals("hello", connection.poll(100).event());
      Map<String, List<String>> result = hub.subscriptions(session, connection.getConnectionId(),
         List.of("record:rt:1", "record:rt:999", "user:bob", "user:me", "instance"), List.of());
      assertEquals(List.of("record:rt:1", "user:me"), result.get("active"));
      assertEquals(List.of("record:rt:999", "user:bob", "instance"), result.get("refused"));
      assertThrows(QNotFoundException.class, () -> hub.subscriptions(new QSession(), connection.getConnectionId(), List.of(), List.of()));
      RealtimeBus.get(instance).publish(new RealtimeMessage().withChannel("table:rt").withEvent("table.changed").withData(Map.of("table", "rt")));
      assertNull(connection.poll(10));
      publish();
      var changed = connection.poll(100);
      assertEquals("record.changed", changed.event());
      assertEquals(List.of("name"), changed.data().get("fields"));
      assertTrue(changed.id().startsWith(connection.getConnectionId() + ":"));
   }



   /*******************************************************************************
    ** Retained cursors replay in order; old or foreign cursors resync without data leakage.
    *******************************************************************************/
   @Test
   void replayResyncAndRefiltering() throws Exception
   {
      var first = hub.connect(session, null);
      var hello = first.poll(100);
      hub.subscriptions(session, first.getConnectionId(), List.of("record:rt:1"), List.of());
      hub.disconnect(first);
      publish();
      var resumed = hub.connect(session, hello.id());
      assertEquals(first.getConnectionId(), resumed.getConnectionId());
      assertEquals("hello", resumed.poll(100).event());
      assertEquals("record.changed", resumed.poll(100).event());
      assertNull(resumed.poll(10));
      var foreign = hub.connect(new QSession(), hello.id());
      assertEquals("hello", foreign.poll(100).event());
      assertEquals("resync", foreign.poll(100).event());
      assertNull(foreign.poll(10));
      clock.advance(121);
      var expired = hub.connect(session, hello.id());
      assertEquals("hello", expired.poll(100).event());
      assertEquals("resync", expired.poll(100).event());
   }



   /*******************************************************************************
    ** Replayed field names are filtered again after metadata visibility changes.
    *******************************************************************************/
   @Test
   void replayDoesNotLeakNewlyHiddenFields() throws Exception
   {
      var first = hub.connect(session, null);
      var hello = first.poll(100);
      hub.subscriptions(session, first.getConnectionId(), List.of("record:rt:1"), List.of());
      hub.disconnect(first);
      publish();
      instance.getTable("rt").getField("name").setIsHidden(true);
      var resumed = hub.connect(session, hello.id());
      assertEquals("hello", resumed.poll(100).event());
      assertEquals(List.of(), resumed.poll(100).data().get("fields"));
   }



   /*******************************************************************************
    ** Revocation and expiry are checked without sleeping for production intervals.
    *******************************************************************************/
   @Test
   void limitsRevocationAndSessionEnded() throws Exception
   {
      var first = hub.connect(session, null);
      first.poll(100);
      hub.connect(session, null);
      hub.connect(session, null);
      assertEquals("session-ended", first.poll(100).event());
      assertEquals("", first.poll(100).event());
      var active = hub.connect(session, null);
      active.poll(100);
      hub.subscriptions(session, active.getConnectionId(), List.of("record:rt:1"), List.of());
      instance.getTable("rt").withRecordSecurityLock(new RecordSecurityLock().withFieldName("name").withSecurityKeyType("secretAccess"));
      clock.advance(61);
      hub.maintenance();
      assertNull(active.poll(100).event());
      var revoked = active.poll(100);
      assertEquals("channel-revoked", revoked.event());
      assertEquals("record:rt:1", revoked.data().get("channel"));
      session.setValue("isInvalid", "true");
      clock.advance(25);
      hub.maintenance();
      assertEquals("session-ended", active.poll(100).event());
      assertEquals("", active.poll(100).event());
   }



   /*******************************************************************************
    ** Publish representative field-name hints without values.
    *******************************************************************************/
   private void publish()
   {
      Map<String, Serializable> data = Map.of("table", "rt", "pk", 1, "fields", new ArrayList<>(List.of("name", "hidden", "locked")));
      RealtimeBus.get(instance).publish(new RealtimeMessage().withChannel("record:rt:1").withEvent("record.changed").withData(data));
   }



   /*******************************************************************************
    ** Thread-safe test clock shared with the maintenance task.
    *******************************************************************************/
   private static class MutableClock extends Clock
   {
      private volatile Instant now = Instant.parse("2026-10-13T00:00:00Z");



      /*******************************************************************************
       ** Advance retention and recheck deadlines.
       *******************************************************************************/
      void advance(long seconds)
      {
         now = now.plusSeconds(seconds);
      }



      /*******************************************************************************
       ** UTC clock.
       *******************************************************************************/
      @Override
      public ZoneId getZone()
      {
         return (ZoneOffset.UTC);
      }



      /*******************************************************************************
       ** These tests only use UTC.
       *******************************************************************************/
      @Override
      public Clock withZone(ZoneId zone)
      {
         return (this);
      }



      /*******************************************************************************
       ** Current test instant.
       *******************************************************************************/
      @Override
      public Instant instant()
      {
         return (now);
      }
   }
}

