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

package com.kingsrook.qqq.backend.core.delivery;


import java.util.List;
import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DynamicDefaultValueBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;


/*******************************************************************************
 ** Optional persistent delivery tables. Hosts supply the backend and migrations.
 *******************************************************************************/
public class DeliveryMetaDataProvider
{
   public static final String OUTPUT_DELIVERY = "output_delivery";
   public static final String DELIVERY_LOG = "delivery_log";
   public static final String ADMIN_PERMISSION = "output_delivery.admin";



   /*******************************************************************************
    ** Define owner-locked records and the optional retry process.
    *******************************************************************************/
   public void defineAll(QInstance instance, String backendName, Consumer<QTableMetaData> enricher) throws QException
   {
      instance.addSecurityKeyType(new QSecurityKeyType().withName("qqq.session.userId").withAllAccessKeyName("qqq.permission." + ADMIN_PERMISSION));
      QTableMetaData choices = table(OUTPUT_DELIVERY, "Output Deliveries", backendName);
      fields(choices, QFieldType.STRING, "sourceTableName", "sourceRecordId", "destinationName", "targetSummary", "lastStatus");
      fields(choices, QFieldType.TEXT, "targetJson", "lastError");
      fields(choices, QFieldType.BOOLEAN, "leaveOutRestricted", "enabled");
      fields(choices, QFieldType.DATE_TIME, "lastDeliveredAt");
      QTableMetaData log = table(DELIVERY_LOG, "Delivery Log", backendName);
      fields(log, QFieldType.STRING, "sourceType", "sourceKey", "destinationName", "targetSummary", "idempotencyKey", "status", "externalReference", "contentClassification", "attachmentName");
      fields(log, QFieldType.INTEGER, "outputDeliveryId", "attemptCount");
      fields(log, QFieldType.LONG, "attachmentBytes");
      fields(log, QFieldType.DATE_TIME, "nextAttemptAt", "createdAt", "deliveredAt");
      fields(log, QFieldType.BOOLEAN, "deliveredAsLink");
      fields(log, QFieldType.TEXT, "storageReference", "lastError");
      log.withUniqueKey(new UniqueKey("idempotencyKey"));
      log.getField("idempotencyKey").setIsRequired(true);
      for(QTableMetaData table : List.of(choices, log))
      {
         if(enricher != null)
         {
            enricher.accept(table);
         }
         instance.addTable(table);
      }
      DeliveryDestinationsMetaData registry = DeliveryDestinationsMetaData.ofOrWithNew(instance);
      if(registry.getDestinations().stream().noneMatch(destination -> "DOWNLOAD".equals(destination.getName())))
      {
         registry.withDestination(DownloadDeliveryDestination.metadata());
      }
      if(registry.getDestinations().stream().noneMatch(destination -> "EMAIL".equals(destination.getName())))
      {
         registry.withDestination(EmailDeliveryDestination.metadata());
      }
      instance.addProcess(new DeliveryRetryProcess().produce(instance));
   }



   /*******************************************************************************
    ** Common immutable owner identity and row security.
    *******************************************************************************/
   private QTableMetaData table(String name, String label, String backendName)
   {
      return (new QTableMetaData().withName(name).withLabel(label).withBackendName(backendName).withPrimaryKeyField("id")
         .withRecordLabelFormatAndFields("%s", "destinationName")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("ownerUserId", QFieldType.STRING).withIsRequired(true).withIsEditable(false)
            .withBehavior(DynamicDefaultValueBehavior.USER_ID))
         .withRecordSecurityLock(new RecordSecurityLock().withFieldName("ownerUserId").withSecurityKeyType("qqq.session.userId"))
         .withCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(DeliveryTableCustomizer.class))
         .withCustomizer(TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(DeliveryTableCustomizer.class))
         .withCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(DeliveryTableCustomizer.class)));
   }



   /*******************************************************************************
    ** Add fields with the standard backend enrichment applied afterwards.
    *******************************************************************************/
   private void fields(QTableMetaData table, QFieldType type, String... names)
   {
      for(String name : names)
      {
         table.withField(new QFieldMetaData(name, type));
      }
   }
}
