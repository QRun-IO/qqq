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
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;


/*******************************************************************************
 * The processed-message table for idempotent triggers: one row per message a
 * trigger processed, keyed (uniquely, in the database) by trigger name and
 * message id.  Rows older than the dedupe retentionDays are purged.
 *
 * Columns are snake_case; there are no foreign keys.  The application adds the
 * table to its instance (defineTable) and creates it in its migrations (the
 * PostgreSQL DDL is in docs/esb/index.adoc).
 *******************************************************************************/
public final class EsbProcessedMessageTable
{
   public static final String FIELD_ID           = "id";
   public static final String FIELD_TRIGGER_NAME = "triggerName";
   public static final String FIELD_MESSAGE_ID   = "messageId";
   public static final String FIELD_PROCESSED_AT = "processedAt";



   /*******************************************************************************
    ** Static helpers only.
    *******************************************************************************/
   private EsbProcessedMessageTable()
   {
   }



   /*******************************************************************************
    ** The processed-message table, on the given backend.
    *******************************************************************************/
   public static QTableMetaData defineTable(String tableName, String backendName)
   {
      return (new QTableMetaData()
         .withName(tableName)
         .withLabel("ESB Processed Message")
         .withBackendName(backendName)
         .withPrimaryKeyField(FIELD_ID)
         .withRecordLabelFormat("%s %s")
         .withRecordLabelFields(FIELD_TRIGGER_NAME, FIELD_MESSAGE_ID)
         .withField(new QFieldMetaData(FIELD_ID, QFieldType.LONG).withBackendName("id").withIsEditable(false))
         .withField(new QFieldMetaData(FIELD_TRIGGER_NAME, QFieldType.STRING).withBackendName("trigger_name").withMaxLength(250))
         .withField(new QFieldMetaData(FIELD_MESSAGE_ID, QFieldType.STRING).withBackendName("message_id").withMaxLength(250))
         .withField(new QFieldMetaData(FIELD_PROCESSED_AT, QFieldType.DATE_TIME).withBackendName("processed_at"))
         .withUniqueKey(new UniqueKey(FIELD_TRIGGER_NAME, FIELD_MESSAGE_ID)));
   }

}
