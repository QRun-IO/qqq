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

package com.kingsrook.qqq.esb.envelope;


import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeType;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.esb.EsbTestBase;
import com.kingsrook.qqq.esb.model.EsbProcessEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Unit test for EsbEventFactory (and EsbCausation).
 *******************************************************************************/
class EsbEventFactoryTest extends EsbTestBase
{
   private static final String INSTANCE_NAME = "test";



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      EsbCausation.clear();
   }



   /*******************************************************************************
    ** An insert event: type, source, subject (the primary key), and data
    ** { record }, copied from the record.
    *******************************************************************************/
   @Test
   void testInsert()
   {
      Instant before = Instant.now();
      QRecord record = new QRecord().withValue("id", 1).withValue("orderNo", "A-1");

      EsbEvent event = EsbEventFactory.forRecordChange(INSTANCE_NAME, TABLE_NAME_ORDER, RecordChangeType.INSERT, record, null);

      assertThat(event.getType()).isEqualTo("qqq.table.order.inserted");
      assertThat(event.getSource()).isEqualTo("qqq://test/table/order");
      assertThat(event.getSubject()).isEqualTo("1");
      assertThat(event.getCausationId()).isNull();
      assertThat(UUID.fromString(event.getId())).isNotNull();
      assertThat(event.getTime()).isBetween(before, Instant.now());
      assertThat(event.getData()).containsOnlyKeys("record");
      assertThat(event.getData().get("record")).isEqualTo(Map.of("id", 1, "orderNo", "A-1"));

      ///////////////////////////////////////////////////////////////////
      // the event keeps a copy, so later changes to the record (which //
      // belongs to the action) do not change what gets published      //
      ///////////////////////////////////////////////////////////////////
      record.setValue("orderNo", "changed");
      assertThat(event.getData().get("record")).isEqualTo(Map.of("id", 1, "orderNo", "A-1"));
   }



   /*******************************************************************************
    ** An update event: data.record is the full post-update record (the old
    ** record's values overlaid with the updated values, including values
    ** updated to null), and data.oldRecord is the old record.
    *******************************************************************************/
   @Test
   void testUpdate()
   {
      QRecord oldRecord = new QRecord().withValue("id", 1).withValue("orderNo", "A-1").withValue("status", "NEW").withValue("note", "x");
      QRecord record    = new QRecord().withValue("id", 1).withValue("status", "SHIPPED").withValue("note", null);

      EsbEvent event = EsbEventFactory.forRecordChange(INSTANCE_NAME, TABLE_NAME_ORDER, RecordChangeType.UPDATE, record, oldRecord);

      assertThat(event.getType()).isEqualTo("qqq.table.order.updated");
      assertThat(event.getSubject()).isEqualTo("1");
      assertThat(event.getData()).containsOnlyKeys("record", "oldRecord");

      Map<String, Serializable> expectedRecord = new HashMap<>();
      expectedRecord.put("id", 1);
      expectedRecord.put("orderNo", "A-1");
      expectedRecord.put("status", "SHIPPED");
      expectedRecord.put("note", null);
      assertThat(event.getData().get("record")).isEqualTo(expectedRecord);
      assertThat(event.getData().get("oldRecord")).isEqualTo(Map.of("id", 1, "orderNo", "A-1", "status", "NEW", "note", "x"));

      oldRecord.setValue("status", "changed");
      assertThat(event.getData().get("oldRecord")).isEqualTo(Map.of("id", 1, "orderNo", "A-1", "status", "NEW", "note", "x"));
   }



   /*******************************************************************************
    ** An update whose old record couldn't be fetched has data { record } only.
    *******************************************************************************/
   @Test
   void testUpdateWithoutOldRecord()
   {
      QRecord  record = new QRecord().withValue("id", 2).withValue("status", "SHIPPED");
      EsbEvent event  = EsbEventFactory.forRecordChange(INSTANCE_NAME, TABLE_NAME_ORDER, RecordChangeType.UPDATE, record, null);

      assertThat(event.getSubject()).isEqualTo("2");
      assertThat(event.getData()).containsOnlyKeys("record");
      assertThat(event.getData().get("record")).isEqualTo(Map.of("id", 2, "status", "SHIPPED"));
   }



   /*******************************************************************************
    ** A delete event has data { oldRecord }: the old record, or the record if no
    ** old record is given.
    *******************************************************************************/
   @Test
   void testDelete()
   {
      QRecord deleted = new QRecord().withValue("id", 3).withValue("orderNo", "A-3");

      EsbEvent event = EsbEventFactory.forRecordChange(INSTANCE_NAME, TABLE_NAME_ORDER, RecordChangeType.DELETE, deleted, deleted);
      assertThat(event.getType()).isEqualTo("qqq.table.order.deleted");
      assertThat(event.getSubject()).isEqualTo("3");
      assertThat(event.getData()).containsOnlyKeys("oldRecord");
      assertThat(event.getData().get("oldRecord")).isEqualTo(Map.of("id", 3, "orderNo", "A-3"));

      EsbEvent withoutOldRecord = EsbEventFactory.forRecordChange(INSTANCE_NAME, TABLE_NAME_ORDER, RecordChangeType.DELETE, deleted, null);
      assertThat(withoutOldRecord.getData()).containsOnlyKeys("oldRecord");
      assertThat(withoutOldRecord.getData().get("oldRecord")).isEqualTo(Map.of("id", 3, "orderNo", "A-3"));

      EsbEvent withoutRecord = EsbEventFactory.forRecordChange(INSTANCE_NAME, TABLE_NAME_ORDER, RecordChangeType.DELETE, null, deleted);
      assertThat(withoutRecord.getData().get("oldRecord")).isEqualTo(Map.of("id", 3, "orderNo", "A-3"));
   }



   /*******************************************************************************
    ** A record change needs the record it describes.
    *******************************************************************************/
   @Test
   void testRecordChangeRequiresRecord()
   {
      assertThatThrownBy(() -> EsbEventFactory.forRecordChange(INSTANCE_NAME, TABLE_NAME_ORDER, RecordChangeType.INSERT, null, null))
         .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> EsbEventFactory.forRecordChange(INSTANCE_NAME, TABLE_NAME_ORDER, RecordChangeType.UPDATE, null, new QRecord()))
         .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> EsbEventFactory.forRecordChange(INSTANCE_NAME, TABLE_NAME_ORDER, RecordChangeType.DELETE, null, null))
         .isInstanceOf(IllegalArgumentException.class);
   }



   /*******************************************************************************
    ** The subject is null when the primary key can't be found: no value, a table
    ** that isn't in the instance, or no instance in the QContext.
    *******************************************************************************/
   @Test
   void testSubjectWithoutPrimaryKey()
   {
      QRecord noKey = new QRecord().withValue("orderNo", "A-1");
      assertThat(EsbEventFactory.forRecordChange(INSTANCE_NAME, TABLE_NAME_ORDER, RecordChangeType.INSERT, noKey, null).getSubject()).isNull();

      QRecord withKey = new QRecord().withValue("id", 1);
      assertThat(EsbEventFactory.forRecordChange(INSTANCE_NAME, "notATable", RecordChangeType.INSERT, withKey, null).getSubject()).isNull();

      QContext.clear();
      EsbEvent event = EsbEventFactory.forRecordChange(INSTANCE_NAME, TABLE_NAME_ORDER, RecordChangeType.INSERT, withKey, null);
      assertThat(event.getSubject()).isNull();
      assertThat(event.getType()).isEqualTo("qqq.table.order.inserted");
   }



   /*******************************************************************************
    ** Process events: type, source, no subject, and data
    ** { processName, processUUID, error? }.
    *******************************************************************************/
   @Test
   void testProcessEvents()
   {
      String processUUID = UUID.randomUUID().toString();

      EsbEvent started = EsbEventFactory.forProcess(INSTANCE_NAME, PROCESS_NAME_SYNC_ORDER, EsbProcessEvent.STARTED, processUUID, null);
      assertThat(started.getType()).isEqualTo("qqq.process.syncOrder.started");
      assertThat(started.getSource()).isEqualTo("qqq://test/process/syncOrder");
      assertThat(started.getSubject()).isNull();
      assertThat(started.getData()).isEqualTo(Map.of("processName", "syncOrder", "processUUID", processUUID));

      EsbEvent completed = EsbEventFactory.forProcess(INSTANCE_NAME, PROCESS_NAME_SYNC_ORDER, EsbProcessEvent.COMPLETED, processUUID, null);
      assertThat(completed.getType()).isEqualTo("qqq.process.syncOrder.completed");

      EsbEvent failed = EsbEventFactory.forProcess(INSTANCE_NAME, PROCESS_NAME_SYNC_ORDER, EsbProcessEvent.FAILED, processUUID, "Step sync failed");
      assertThat(failed.getType()).isEqualTo("qqq.process.syncOrder.failed");
      assertThat(failed.getData()).isEqualTo(Map.of("processName", "syncOrder", "processUUID", processUUID, "error", "Step sync failed"));
   }



   /*******************************************************************************
    ** Custom events: caller-supplied type and data (copied), and a source built
    ** from the source path.
    *******************************************************************************/
   @Test
   void testCustom()
   {
      LinkedHashMap<String, Serializable> data = new LinkedHashMap<>();
      data.put("orderId", 47);

      EsbEvent event = EsbEventFactory.custom(INSTANCE_NAME, "process/syncOrder", "com.example.order.synced", data);
      assertThat(event.getType()).isEqualTo("com.example.order.synced");
      assertThat(event.getSource()).isEqualTo("qqq://test/process/syncOrder");
      assertThat(event.getSubject()).isNull();
      assertThat(event.getData()).isEqualTo(Map.of("orderId", 47));

      data.put("orderId", 48);
      assertThat(event.getData()).isEqualTo(Map.of("orderId", 47));

      assertThat(EsbEventFactory.custom(INSTANCE_NAME, "/table/order", "t", null).getSource()).isEqualTo("qqq://test/table/order");
      assertThat(EsbEventFactory.custom(INSTANCE_NAME, null, "t", null).getSource()).isEqualTo("qqq://test/");
      assertThat(EsbEventFactory.custom(INSTANCE_NAME, null, "t", null).getData()).isEmpty();
      assertThat(EsbEventFactory.custom(null, "table/order", "t", null).getSource()).isEqualTo("qqq:///table/order");

      assertThatThrownBy(() -> EsbEventFactory.custom(INSTANCE_NAME, "x", " ", data))
         .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> EsbEventFactory.custom(INSTANCE_NAME, "x", null, data))
         .isInstanceOf(IllegalArgumentException.class);
   }



   /*******************************************************************************
    ** Each event gets its own id.
    *******************************************************************************/
   @Test
   void testIdsAreUnique()
   {
      EsbEvent a = EsbEventFactory.custom(INSTANCE_NAME, "x", "t", null);
      EsbEvent b = EsbEventFactory.custom(INSTANCE_NAME, "x", "t", null);
      assertThat(a.getId()).isNotEqualTo(b.getId());
   }



   /*******************************************************************************
    ** Every factory copies the thread's current causation id into the event;
    ** other threads don't see it, and clear (or set(null)) removes it.
    *******************************************************************************/
   @Test
   void testCausation() throws Exception
   {
      assertThat(EsbCausation.current()).isNull();

      EsbCausation.set("triggering-event-id");
      assertThat(EsbCausation.current()).isEqualTo("triggering-event-id");

      QRecord record = new QRecord().withValue("id", 1);
      assertThat(EsbEventFactory.forRecordChange(INSTANCE_NAME, TABLE_NAME_ORDER, RecordChangeType.INSERT, record, null).getCausationId()).isEqualTo("triggering-event-id");
      assertThat(EsbEventFactory.forProcess(INSTANCE_NAME, PROCESS_NAME_SYNC_ORDER, EsbProcessEvent.COMPLETED, "uuid", null).getCausationId()).isEqualTo("triggering-event-id");
      assertThat(EsbEventFactory.custom(INSTANCE_NAME, "x", "t", null).getCausationId()).isEqualTo("triggering-event-id");

      AtomicReference<String> seenOnOtherThread = new AtomicReference<>("not run");
      Thread                  otherThread       = Thread.ofVirtual().start(() -> seenOnOtherThread.set(EsbCausation.current()));
      otherThread.join();
      assertThat(seenOnOtherThread.get()).isNull();

      EsbCausation.clear();
      assertThat(EsbCausation.current()).isNull();
      assertThat(EsbEventFactory.custom(INSTANCE_NAME, "x", "t", null).getCausationId()).isNull();

      EsbCausation.set("another-id");
      EsbCausation.set(null);
      assertThat(EsbCausation.current()).isNull();
   }

}
