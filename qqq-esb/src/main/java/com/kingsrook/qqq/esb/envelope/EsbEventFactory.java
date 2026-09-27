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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeType;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.esb.model.EsbProcessEvent;


/*******************************************************************************
 * Makes the EsbEvents that QQQ publishes (spec section 4).
 *
 * Every event gets a new UUID id, the current time, and the current thread's
 * EsbCausation id as its causationId.  Sources are qqq://instanceName/path;
 * a null instance name leaves the authority empty (qqq:///path).
 *
 * Table events (type qqq.table.tableName.inserted|updated|deleted) have the
 * record's primary key as their subject, and data holding copies of the
 * records' field values (QRecord.getValues()):
 * - INSERT: { record }
 * - UPDATE: { record, oldRecord }, where record is the full post-update record:
 *   the old record's values overlaid with the updated values (the records a
 *   record change listener gets hold only the fields that were sent).  Without
 *   an old record, data is { record } with just the updated values.
 * - DELETE: { oldRecord }
 *
 * Process events (type qqq.process.processName.started|completed|failed) have
 * data { processName, processUUID, error? }.
 *******************************************************************************/
public class EsbEventFactory
{
   public static final String SOURCE_PREFIX = "qqq://";

   public static final String DATA_RECORD       = "record";
   public static final String DATA_OLD_RECORD   = "oldRecord";
   public static final String DATA_PROCESS_NAME = "processName";
   public static final String DATA_PROCESS_UUID = "processUUID";
   public static final String DATA_ERROR        = "error";



   /*******************************************************************************
    ** An event for one record written by an insert, update, or delete.
    **
    ** record is the record as the action wrote it; oldRecord is the stored record
    ** from before the write (null for an insert, and allowed to be null for an
    ** update or a delete).  The subject is the primary key, found through the
    ** table's meta-data in the QContext's instance (null if either is missing).
    **
    ** Throws IllegalArgumentException if there's no record to describe.
    *******************************************************************************/
   public static EsbEvent forRecordChange(String instanceName, String tableName, RecordChangeType changeType, QRecord record, QRecord oldRecord)
   {
      LinkedHashMap<String, Serializable> data = new LinkedHashMap<>();
      LinkedHashMap<String, Serializable> recordValues;
      String                              typeSuffix;

      switch(Objects.requireNonNull(changeType, "changeType"))
      {
         case INSERT ->
         {
            recordValues = copyValues(requireRecord(record, changeType));
            data.put(DATA_RECORD, recordValues);
            typeSuffix = "inserted";
         }
         case UPDATE ->
         {
            recordValues = (oldRecord == null) ? new LinkedHashMap<>() : copyValues(oldRecord);
            recordValues.putAll(copyValues(requireRecord(record, changeType)));

            data.put(DATA_RECORD, recordValues);
            if(oldRecord != null)
            {
               data.put(DATA_OLD_RECORD, copyValues(oldRecord));
            }
            typeSuffix = "updated";
         }
         case DELETE ->
         {
            recordValues = copyValues(requireRecord(oldRecord != null ? oldRecord : record, changeType));
            data.put(DATA_OLD_RECORD, recordValues);
            typeSuffix = "deleted";
         }
         default -> throw (new IllegalArgumentException("Unexpected record change type: " + changeType));
      }

      return (newEvent(instanceName, "table/" + tableName, "qqq.table." + tableName + "." + typeSuffix, data)
         .withSubject(getPrimaryKeyAsString(tableName, recordValues)));
   }



   /*******************************************************************************
    ** An event for a process run starting, completing, or failing.  error is the
    ** failure's message (null when there is none), and is left out of data when
    ** null.
    *******************************************************************************/
   public static EsbEvent forProcess(String instanceName, String processName, EsbProcessEvent processEvent, String processUUID, String error)
   {
      String typeSuffix = switch(Objects.requireNonNull(processEvent, "processEvent"))
      {
         case STARTED -> "started";
         case COMPLETED -> "completed";
         case FAILED -> "failed";
      };

      LinkedHashMap<String, Serializable> data = new LinkedHashMap<>();
      data.put(DATA_PROCESS_NAME, processName);
      data.put(DATA_PROCESS_UUID, processUUID);
      if(error != null)
      {
         data.put(DATA_ERROR, error);
      }

      return (newEvent(instanceName, "process/" + processName, "qqq.process." + processName + "." + typeSuffix, data));
   }



   /*******************************************************************************
    ** An explicitly published event, with a caller-supplied type and data (copied;
    ** null means empty).  sourcePath is what follows the instance name in the
    ** source (e.g., process/syncOrder); a leading slash is ignored.
    **
    ** Throws IllegalArgumentException if type is empty.
    *******************************************************************************/
   public static EsbEvent custom(String instanceName, String sourcePath, String type, Map<String, Serializable> data)
   {
      if(!StringUtils.hasContent(type))
      {
         throw (new IllegalArgumentException("An ESB event type is required"));
      }

      String path = (sourcePath == null) ? "" : sourcePath.replaceFirst("^/+", "");
      return (newEvent(instanceName, path, type, (data == null) ? new LinkedHashMap<>() : new LinkedHashMap<>(data)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static EsbEvent newEvent(String instanceName, String sourcePath, String type, Map<String, Serializable> data)
   {
      return (new EsbEvent()
         .withId(UUID.randomUUID().toString())
         .withSource(SOURCE_PREFIX + Objects.requireNonNullElse(instanceName, "") + "/" + sourcePath)
         .withType(type)
         .withTime(Instant.now())
         .withCausationId(EsbCausation.current())
         .withData(data));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QRecord requireRecord(QRecord record, RecordChangeType changeType)
   {
      if(record == null)
      {
         throw (new IllegalArgumentException("A record is required for an ESB " + changeType + " event"));
      }
      return (record);
   }



   /*******************************************************************************
    ** A copy of the record's values, because the record belongs to the action
    ** that wrote it and events are published later (after commit).
    *******************************************************************************/
   private static LinkedHashMap<String, Serializable> copyValues(QRecord record)
   {
      return (record.getValues() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(record.getValues()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String getPrimaryKeyAsString(String tableName, Map<String, Serializable> values)
   {
      QInstance      qInstance = QContext.getQInstance();
      QTableMetaData table     = (qInstance == null) ? null : qInstance.getTable(tableName);
      if(table == null || table.getPrimaryKeyField() == null)
      {
         return (null);
      }

      return (ValueUtils.getValueAsString(values.get(table.getPrimaryKeyField())));
   }

}
