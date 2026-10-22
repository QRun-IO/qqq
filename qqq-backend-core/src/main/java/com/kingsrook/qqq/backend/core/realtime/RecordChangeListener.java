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

package com.kingsrook.qqq.backend.core.realtime;


import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import com.kingsrook.qqq.backend.core.actions.collaboration.RecordFeaturesResolver;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeEvent;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeListenerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeType;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeature;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.ResolvedRecordFeatures;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Publish committed invalidations only for tables using presence or following.
 *******************************************************************************/
public class RecordChangeListener implements RecordChangeListenerInterface
{
   private static final Map<QInstance, Map<String, Instant>> LAST_TABLE_EVENTS = new WeakHashMap<>();



   /*******************************************************************************
    ** Resolve runtime feature overrides before each write.
    *******************************************************************************/
   @Override
   public boolean appliesTo(String tableName, RecordChangeType type)
   {
      QInstance instance = QContext.getQInstance();
      QTableMetaData table = instance.getTable(tableName);
      if(table == null)
      {
         return (false);
      }
      ResolvedRecordFeatures features = RecordFeaturesResolver.resolve(instance, table);
      return (features.isEnabled(RecordFeature.PRESENCE) || features.isEnabled(RecordFeature.FOLLOW));
   }



   /*******************************************************************************
    ** Snapshot hints before returning; transactions publish only after commit.
    *******************************************************************************/
   @Override
   public void onRecordsChanged(RecordChangeEvent event)
   {
      QInstance instance = QContext.getQInstance();
      QTableMetaData table = instance.getTable(event.getTableName());
      RealtimeBusInterface bus = RealtimeBus.get(instance);
      List<RealtimeMessage> messages = new ArrayList<>();
      for(int i = 0; i < event.getRecords().size(); i++)
      {
         QRecord record = event.getRecords().get(i);
         Serializable primaryKey = record.getValue(table.getPrimaryKeyField());
         Map<String, Serializable> data = new LinkedHashMap<>();
         data.put("table", table.getName());
         data.put("pk", primaryKey);
         if(event.getType() != RecordChangeType.DELETE)
         {
            QRecord old = event.getOldRecords() == null ? null : event.getOldRecords().get(i);
            ArrayList<String> fields = new ArrayList<>();
            record.getValues().forEach((name, value) ->
            {
               if(old == null || !Objects.deepEquals(old.getValue(name), value))
               {
                  fields.add(name);
               }
            });
            String versionField = table.getRecordVersionField();
            data.put("version", versionField == null || versionField.isEmpty() ? null
               : record.getValues().containsKey(versionField) ? record.getValue(versionField) : old == null ? null : old.getValue(versionField));
            data.put("changedBy", QContext.getQSession() == null || QContext.getQSession().getUser() == null ? null : QContext.getQSession().getUser().getIdReference());
            data.put("fields", fields);
         }
         messages.add(new RealtimeMessage().withChannel("record:" + table.getName() + ":" + primaryKey)
            .withEvent(event.getType() == RecordChangeType.DELETE ? "record.deleted" : "record.changed").withData(data));
      }
      Runnable publish = () ->
      {
         messages.forEach(bus::publish);
         if(claimTableEvent(instance, table.getName()))
         {
            bus.publish(new RealtimeMessage().withChannel("table:" + table.getName()).withEvent("table.changed")
               .withData(Map.of("table", table.getName())));
         }
      };
      if(event.getTransaction() == null)
      {
         publish.run();
      }
      else
      {
         event.getTransaction().addAfterCommitCallback(publish);
      }
   }



   /*******************************************************************************
    ** Coalesce table hints per instance without holding the lock during delivery.
    *******************************************************************************/
   private static synchronized boolean claimTableEvent(QInstance instance, String table)
   {
      Map<String, Instant> times = LAST_TABLE_EVENTS.computeIfAbsent(instance, key -> new LinkedHashMap<>());
      Instant now = Instant.now();
      Instant previous = times.get(table);
      if(previous != null && previous.plusSeconds(1).isAfter(now))
      {
         return (false);
      }
      times.put(table, now);
      return (true);
   }
}

