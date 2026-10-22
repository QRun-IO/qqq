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
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.SystemScope;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.esb.model.EsbDedupeConfig;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 * Idempotent triggers: which messages a trigger already processed, kept in
 * the processed-message table (see EsbDedupeConfig).
 *
 * Why a table, not a per-node cache: a redelivery after a broker failover (or
 * after a lost commit) often lands on a different node than the one that
 * processed the message, and a restarted node has an empty cache - so only a
 * shared, durable record can answer "was this processed?".  A cache in front
 * would only help the rare duplicate, while every message still needs the
 * database check, so there is none.  The unique (trigger, message id) key
 * makes the record correct even when two nodes race.
 *
 * The trigger runner checks before running (skipping processed messages), and
 * records after a successful run, before acknowledging - so a failure between
 * the two can still run a message twice (at-least-once), but a redelivered
 * message whose run was recorded never runs again.
 *******************************************************************************/
public final class EsbMessageDedupe
{
   private static final QLogger LOG = QLogger.getLogger(EsbMessageDedupe.class);

   private static final int QUERY_CHUNK_SIZE = 500;



   /*******************************************************************************
    ** Static helpers only.
    *******************************************************************************/
   private EsbMessageDedupe()
   {
   }



   /*******************************************************************************
    ** The instance's dedupe settings, if dedupe is enabled - else null.
    *******************************************************************************/
   public static EsbDedupeConfig getEnabledConfig(QInstance qInstance)
   {
      EsbInstanceMetaData esbInstanceMetaData = (qInstance == null) ? null : QSupplementalInstanceMetaData.of(qInstance, EsbInstanceMetaData.NAME);
      return (esbInstanceMetaData != null && esbInstanceMetaData.isDedupeEnabled() ? esbInstanceMetaData.getDedupe() : null);
   }



   /*******************************************************************************
    ** Of the given message keys, those the trigger already processed.  Throws if
    ** the table can't be read (the caller must not run the messages then).
    *******************************************************************************/
   public static Set<String> findProcessed(EsbDedupeConfig config, String triggerName, Collection<String> messageKeys) throws QException
   {
      Set<String>  processed = new HashSet<>();
      List<String> keys      = new ArrayList<>(new LinkedHashSet<>(CollectionUtils.nonNullCollection(messageKeys)));
      for(int start = 0; start < keys.size(); start += QUERY_CHUNK_SIZE)
      {
         List<String> chunk = keys.subList(start, Math.min(keys.size(), start + QUERY_CHUNK_SIZE));
         List<QRecord> records = SystemScope.call(() -> new QueryAction().execute(new QueryInput(config.getTableName())
            .withFilter(new QQueryFilter(
               new QFilterCriteria(EsbProcessedMessageTable.FIELD_TRIGGER_NAME, QCriteriaOperator.EQUALS, triggerName),
               new QFilterCriteria(EsbProcessedMessageTable.FIELD_MESSAGE_ID, QCriteriaOperator.IN, chunk)))).getRecords());
         for(QRecord record : records)
         {
            processed.add(record.getValueString(EsbProcessedMessageTable.FIELD_MESSAGE_ID));
         }
      }
      return (processed);
   }



   /*******************************************************************************
    ** Record that the trigger processed these messages.  Never throws: a
    ** failure (e.g., another node recorded one first) is logged - the run has
    ** already happened.
    *******************************************************************************/
   public static void recordProcessed(EsbDedupeConfig config, String triggerName, Collection<String> messageKeys)
   {
      Instant       now     = Instant.now();
      List<QRecord> records = new ArrayList<>();
      for(String key : new LinkedHashSet<>(CollectionUtils.nonNullCollection(messageKeys)))
      {
         records.add(new QRecord()
            .withValue(EsbProcessedMessageTable.FIELD_TRIGGER_NAME, triggerName)
            .withValue(EsbProcessedMessageTable.FIELD_MESSAGE_ID, key)
            .withValue(EsbProcessedMessageTable.FIELD_PROCESSED_AT, now));
      }
      if(records.isEmpty())
      {
         return;
      }

      try
      {
         SystemScope.call(() -> new InsertAction().execute(new InsertInput(config.getTableName())
            .withRecords(records)
            .withOmitRecordChangeEvents(true)
            .withOmitDmlAudit(true)));
      }
      catch(Exception e)
      {
         LOG.warn("Could not record processed ESB messages; a redelivery of them would run again", e, logPair("triggerName", triggerName), logPair("messageCount", records.size()));
      }
   }

}
