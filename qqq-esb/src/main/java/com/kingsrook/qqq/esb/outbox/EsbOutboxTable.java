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


import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 * The ESB outbox table: one row per event per destination, written in the same
 * backend transaction as the record write that made it, and published by
 * EsbOutboxRelay.
 *
 * Columns are snake_case; there are no foreign keys.  The application adds the
 * table to its instance (defineTable) and creates it in its migrations (the
 * PostgreSQL DDL is in docs/esb/index.adoc).
 *
 * A row is PENDING until it is sent (SENT, kept retentionDays, then purged),
 * or until it fails maxAttempts times while the broker is connected (DEAD,
 * kept until someone deletes it, or sets it back to PENDING to retry it).
 *******************************************************************************/
public final class EsbOutboxTable
{
   public static final String FIELD_ID               = "id";
   public static final String FIELD_UUID             = "uuid";
   public static final String FIELD_DESTINATION_NAME = "destinationName";
   public static final String FIELD_EVENT_TYPE       = "eventType";
   public static final String FIELD_EVENT_JSON       = "eventJson";
   public static final String FIELD_STATUS           = "status";
   public static final String FIELD_ATTEMPTS         = "attempts";
   public static final String FIELD_NEXT_ATTEMPT_AT  = "nextAttemptAt";
   public static final String FIELD_LEASE_OWNER      = "leaseOwner";
   public static final String FIELD_LEASE_EXPIRES_AT = "leaseExpiresAt";
   public static final String FIELD_LAST_ERROR       = "lastError";
   public static final String FIELD_CREATE_DATE      = "createDate";
   public static final String FIELD_SENT_AT          = "sentAt";

   public static final String STATUS_PENDING = "PENDING";
   public static final String STATUS_SENT    = "SENT";
   public static final String STATUS_DEAD    = "DEAD";



   /*******************************************************************************
    ** Static helpers only.
    *******************************************************************************/
   private EsbOutboxTable()
   {
   }



   /*******************************************************************************
    ** The outbox table, on the given backend.  Put it on the same backend as the
    ** tables that publish, so their events are written in the same transaction
    ** as their records.
    *******************************************************************************/
   public static QTableMetaData defineTable(String tableName, String backendName)
   {
      return (new QTableMetaData()
         .withName(tableName)
         .withLabel("ESB Outbox")
         .withBackendName(backendName)
         .withPrimaryKeyField(FIELD_ID)
         .withRecordLabelFormat("%s %s")
         .withRecordLabelFields(FIELD_DESTINATION_NAME, FIELD_UUID)
         .withField(new QFieldMetaData(FIELD_ID, QFieldType.LONG).withBackendName("id").withIsEditable(false))
         .withField(new QFieldMetaData(FIELD_UUID, QFieldType.STRING).withBackendName("uuid").withMaxLength(36))
         .withField(new QFieldMetaData(FIELD_DESTINATION_NAME, QFieldType.STRING).withBackendName("destination_name").withMaxLength(250))
         .withField(new QFieldMetaData(FIELD_EVENT_TYPE, QFieldType.STRING).withBackendName("event_type").withMaxLength(250))
         .withField(new QFieldMetaData(FIELD_EVENT_JSON, QFieldType.TEXT).withBackendName("event_json"))
         .withField(new QFieldMetaData(FIELD_STATUS, QFieldType.STRING).withBackendName("status").withMaxLength(20))
         .withField(new QFieldMetaData(FIELD_ATTEMPTS, QFieldType.INTEGER).withBackendName("attempts"))
         .withField(new QFieldMetaData(FIELD_NEXT_ATTEMPT_AT, QFieldType.DATE_TIME).withBackendName("next_attempt_at"))
         .withField(new QFieldMetaData(FIELD_LEASE_OWNER, QFieldType.STRING).withBackendName("lease_owner").withMaxLength(250))
         .withField(new QFieldMetaData(FIELD_LEASE_EXPIRES_AT, QFieldType.DATE_TIME).withBackendName("lease_expires_at"))
         .withField(new QFieldMetaData(FIELD_LAST_ERROR, QFieldType.TEXT).withBackendName("last_error"))
         .withField(new QFieldMetaData(FIELD_CREATE_DATE, QFieldType.DATE_TIME).withBackendName("created_at"))
         .withField(new QFieldMetaData(FIELD_SENT_AT, QFieldType.DATE_TIME).withBackendName("sent_at")));
   }

}
