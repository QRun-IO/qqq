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

package com.kingsrook.qqq.esb.outbox;


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.SystemScope;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.esb.envelope.EsbEvent;
import com.kingsrook.qqq.esb.envelope.EsbEventCodec;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.EsbOutboxConfig;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 * Writes events to the ESB outbox table (see EsbOutboxConfig and
 * EsbOutboxRelay).
 *
 * Where the rows are written depends on the record write's transaction:
 * - a transaction on the outbox table's backend: in that transaction, so the
 *   rows commit (or roll back) with the records;
 * - a transaction on another backend: right after it commits (never if it
 *   rolls back) - not atomic with the records, so put the outbox on the
 *   publishing tables' backend;
 * - no transaction (the records are already stored): right away.
 *
 * Rows are inserted as the system user, without record change events.  Each
 * commit (or immediate write) wakes this node's relay.
 *******************************************************************************/
public final class EsbOutbox
{
   private static final QLogger LOG = QLogger.getLogger(EsbOutbox.class);



   /*******************************************************************************
    ** Static helpers only.
    *******************************************************************************/
   private EsbOutbox()
   {
   }



   /*******************************************************************************
    ** The instance's outbox settings, if the outbox is enabled - else null.
    *******************************************************************************/
   public static EsbOutboxConfig getEnabledConfig(QInstance qInstance)
   {
      EsbInstanceMetaData esbInstanceMetaData = (qInstance == null) ? null : QSupplementalInstanceMetaData.of(qInstance, EsbInstanceMetaData.NAME);
      return (esbInstanceMetaData != null && esbInstanceMetaData.isOutboxEnabled() ? esbInstanceMetaData.getOutbox() : null);
   }



   /*******************************************************************************
    ** Write the events (by destination, in order) for a write to recordTableName
    ** (see the class comment for when).  Throws if a write made now fails - in a
    ** transaction, the caller must then roll back.
    *******************************************************************************/
   public static void write(String recordTableName, QBackendTransaction transaction, Map<String, List<EsbEvent>> eventsByDestination) throws QException
   {
      QInstance       qInstance = QContext.getQInstance();
      EsbOutboxConfig config    = getEnabledConfig(qInstance);
      if(config == null)
      {
         throw (new QException("The ESB outbox is not enabled"));
      }

      List<QRecord> rows = makeRows(eventsByDestination);
      if(rows.isEmpty())
      {
         return;
      }

      String outboxTableName = config.getTableName();
      if(transaction == null)
      {
         insert(outboxTableName, rows, null);
         EsbOutboxRelay.getInstance().nudge();
         return;
      }

      if(isSameBackend(qInstance, recordTableName, outboxTableName))
      {
         insert(outboxTableName, rows, transaction);
         transaction.addAfterCommitCallback(() -> EsbOutboxRelay.getInstance().nudge());
         return;
      }

      transaction.addAfterCommitCallback(() ->
      {
         try
         {
            insert(outboxTableName, rows, null);
            EsbOutboxRelay.getInstance().nudge();
         }
         catch(Exception e)
         {
            LOG.error("Could not write ESB events to the outbox after their records' commit; they are lost", e, logPair("tableName", recordTableName), logPair("eventCount", rows.size()));
         }
      });
   }



   /*******************************************************************************
    ** One PENDING row per event per destination, in order.
    *******************************************************************************/
   static List<QRecord> makeRows(Map<String, List<EsbEvent>> eventsByDestination)
   {
      Instant       now  = Instant.now();
      List<QRecord> rows = new ArrayList<>();
      for(Map.Entry<String, List<EsbEvent>> entry : CollectionUtils.nonNullMap(eventsByDestination).entrySet())
      {
         for(EsbEvent event : CollectionUtils.nonNullList(entry.getValue()))
         {
            rows.add(new QRecord()
               .withValue(EsbOutboxTable.FIELD_UUID, UUID.randomUUID().toString())
               .withValue(EsbOutboxTable.FIELD_DESTINATION_NAME, entry.getKey())
               .withValue(EsbOutboxTable.FIELD_EVENT_TYPE, event.getType())
               .withValue(EsbOutboxTable.FIELD_EVENT_JSON, EsbEventCodec.toJson(event))
               .withValue(EsbOutboxTable.FIELD_STATUS, EsbOutboxTable.STATUS_PENDING)
               .withValue(EsbOutboxTable.FIELD_ATTEMPTS, 0)
               .withValue(EsbOutboxTable.FIELD_CREATE_DATE, now));
         }
      }
      return (rows);
   }



   /*******************************************************************************
    ** Whether the record table and the outbox table are on one backend (so one
    ** transaction covers both).
    *******************************************************************************/
   private static boolean isSameBackend(QInstance qInstance, String recordTableName, String outboxTableName)
   {
      QTableMetaData recordTable = qInstance.getTable(recordTableName);
      QTableMetaData outboxTable = qInstance.getTable(outboxTableName);
      return (recordTable != null && outboxTable != null && Objects.equals(recordTable.getBackendName(), outboxTable.getBackendName()));
   }



   /*******************************************************************************
    ** Insert the rows (as the system user), failing if any row has an error.
    *******************************************************************************/
   private static void insert(String outboxTableName, List<QRecord> rows, QBackendTransaction transaction) throws QException
   {
      InsertOutput output = SystemScope.call(() -> new InsertAction().execute(new InsertInput(outboxTableName)
         .withRecords(rows)
         .withTransaction(transaction)
         .withOmitRecordChangeEvents(true)
         .withOmitDmlAudit(true)));
      for(QRecord record : CollectionUtils.nonNullList(output.getRecords()))
      {
         if(CollectionUtils.nullSafeHasContents(record.getErrors()))
         {
            throw (new QException("Could not write an ESB event to the outbox: " + record.getErrors()));
         }
      }
   }

}
