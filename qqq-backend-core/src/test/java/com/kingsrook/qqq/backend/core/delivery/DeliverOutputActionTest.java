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


import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.ai.AiFieldMetaData;
import com.kingsrook.qqq.backend.core.actions.ai.DataClassification;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.alerts.AlertResolution;
import com.kingsrook.qqq.backend.core.alerts.AlertSignal;
import com.kingsrook.qqq.backend.core.alerts.AlertSinkInterface;
import com.kingsrook.qqq.backend.core.alerts.AlertsInstanceMetaData;
import com.kingsrook.qqq.backend.core.alerts.NotificationCategory;
import com.kingsrook.qqq.backend.core.alerts.NotificationSignal;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Delivery security, classification, durable deduplication and owner retries.
 *******************************************************************************/
class DeliverOutputActionTest extends BaseTest
{
   private QDeliveryDestinationMetaData destination;
   private static DeliveryRequest received;
   private static int calls;
   private static DeliveryResult.Status outcome;
   private static NotificationSignal notification;
   private static AlertSignal alert;



   /*******************************************************************************
    ** Memory-backed tables and a destination with observable side effects.
    *******************************************************************************/
   @BeforeEach
   void setup() throws Exception
   {
      calls = 0;
      received = null;
      notification = null;
      alert = null;
      outcome = DeliveryResult.Status.DELIVERED;
      new DeliveryMetaDataProvider().defineAll(QContext.getQInstance(), TestUtils.MEMORY_BACKEND_NAME, null);
      destination = new QDeliveryDestinationMetaData().withName("TEST").withLabel("Test destination")
         .withCodeReference(new QCodeReference(RecordingDestination.class)).withMaxClassification(DataClassification.CONFIDENTIAL).withMaxAttachmentBytes(10L);
      DeliveryDestinationsMetaData.of(QContext.getQInstance()).withDestination(destination);
      AlertsInstanceMetaData.ofOrWithNew(QContext.getQInstance()).withSink(new QCodeReference(RecordingSink.class));
      new QInstanceEnricher(QContext.getQInstance()).enrich();
   }



   /*******************************************************************************
    ** A normal owner delivery is durable and repeat calls do not send again.
    *******************************************************************************/
   @Test
   void ownerAndIdempotency() throws Exception
   {
      DeliveryRequest request = request("once");
      assertEquals(DeliveryResult.Status.DELIVERED, new DeliverOutputAction().execute(request).getStatus());
      assertEquals(DEFAULT_USER_ID, received.getOwnerUserId());
      assertEquals(DeliveryResult.Status.DELIVERED, new DeliverOutputAction().execute(request).getStatus());
      assertEquals(1, calls);
      assertEquals(1, logs().size());
      assertEquals(1, logs().get(0).getValueInteger("attemptCount"));
   }



   /*******************************************************************************
    ** A final outcome remains final even if its owner can no longer authenticate.
    *******************************************************************************/
   @Test
   void finalResultSurvivesOwnerRemoval() throws Exception
   {
      DeliveryRequest request = request("removed-owner");
      new DeliverOutputAction().execute(request);
      QContext.setQSession(new QSystemUserSession());
      try(MockedStatic<DeliverySessions> sessions = Mockito.mockStatic(DeliverySessions.class))
      {
         sessions.when(() -> DeliverySessions.forOwner(DEFAULT_USER_ID)).thenThrow(new QException("Owner removed"));
         assertEquals(DeliveryResult.Status.DELIVERED, new DeliverOutputAction().execute(request).getStatus());
      }
      assertEquals(1, calls);
      assertNull(notification);
      assertEquals("DELIVERED", logs().get(0).getValueString("status"));
   }



   /*******************************************************************************
    ** System callers rebuild an ordinary owner session and restore their context.
    *******************************************************************************/
   @Test
   void systemRebuildsOwner() throws Exception
   {
      QSession system = new QSystemUserSession();
      QContext.setQSession(system);
      assertEquals(DeliveryResult.Status.DELIVERED, new DeliverOutputAction().execute(request("scheduled")).getStatus());
      assertEquals(1, calls);
      assertSame(system, QContext.getQSession());
   }



   /*******************************************************************************
    ** Missing grants never invoke the provider or choose an alternative destination.
    *******************************************************************************/
   @Test
   void lostPermissionNotifiesOwner() throws Exception
   {
      destination.withPermissionName("delivery.test");
      QContext.setQSession(new QSystemUserSession());
      assertEquals(DeliveryResult.Status.UNAUTHORIZED, new DeliverOutputAction().execute(request("denied").withLink("/reports/7")).getStatus());
      assertEquals(0, calls);
      assertEquals(NotificationCategory.SYSTEM, notification.getCategory());
      assertEquals("/reports/7", notification.getLink().getPath());
      assertEquals(List.of(DEFAULT_USER_ID), notification.getAudience().getValues());
      assertEquals("delivery.unauthorized", alert.getAlertTypeName());
      assertThat(alert.getDedupKey()).startsWith(DEFAULT_USER_ID + ":TEST:");
   }



   /*******************************************************************************
    ** An interactive caller cannot impersonate another owner.
    *******************************************************************************/
   @Test
   void cannotBorrowAnotherOwner() throws Exception
   {
      assertEquals(DeliveryResult.Status.UNAUTHORIZED, new DeliverOutputAction().execute(request("wrong-owner").withOwnerUserId("other")).getStatus());
      assertEquals(0, calls);
   }



   /*******************************************************************************
    ** PHI is refused with field labels; an already rerendered safe payload can pass.
    *******************************************************************************/
   @Test
   void classificationAndRerendering() throws Exception
   {
      DeliveryRequest request = request("phi").withContentClassification(DataClassification.PHI).withRestrictedFieldLabels(List.of("Patient name", "Date of birth"));
      DeliveryResult refused = new DeliverOutputAction().execute(request);
      assertEquals(DeliveryResult.Status.REFUSED, refused.getStatus());
      assertThat(refused.getReasons().get(0)).contains("Patient name", "Date of birth");
      assertEquals(0, calls);
      request.withIdempotencyKey("rerendered").withLeaveOutRestricted(true).withContentClassification(DataClassification.PUBLIC).withRestrictedFieldLabels(List.of());
      assertEquals(DeliveryResult.Status.DELIVERED, new DeliverOutputAction().execute(request).getStatus());
      request.withIdempotencyKey("still-phi").withContentClassification(DataClassification.PHI);
      assertEquals(DeliveryResult.Status.REFUSED, new DeliverOutputAction().execute(request).getStatus());
   }



   /*******************************************************************************
    ** Link-only delivery removes data without mutating the producer's original input.
    *******************************************************************************/
   @Test
   void linkOnlyDropsData() throws Exception
   {
      destination.withAboveMaxClassification(QDeliveryDestinationMetaData.AboveMaxClassification.LINK_ONLY);
      DeliveryRequest request = request("secure-link").withContentClassification(DataClassification.PHI).withSummary("secret")
         .withAttachments(List.of(attachment(5L)));
      assertEquals(DeliveryResult.Status.DELIVERED, new DeliverOutputAction().execute(request).getStatus());
      assertNull(received.getSummary());
      assertTrue(received.getAttachments().isEmpty());
      assertThat(received.getLink()).startsWith("/qqq/v1/delivery/output/");
      assertEquals("secret", request.getSummary());
      assertEquals(1, request.getAttachments().size());
   }



   /*******************************************************************************
    ** Without a presign SPI, even public content uses an authenticated app link.
    *******************************************************************************/
   @Test
   void oversizeUsesAppLink() throws Exception
   {
      destination.withAllowBearerLinks(true);
      new DeliverOutputAction().execute(request("large").withContentClassification(DataClassification.PUBLIC).withAttachments(List.of(attachment(11L))));
      assertTrue(received.getAttachments().isEmpty());
      assertThat(received.getLink()).startsWith("/qqq/v1/delivery/output/");
      assertTrue(logs().get(0).getValueBoolean("deliveredAsLink"));
   }



   /*******************************************************************************
    ** A scheduled retry reopens the owner's session and retains the delivery key.
    *******************************************************************************/
   @Test
   void retryDueRows() throws Exception
   {
      DeliveryDestinationsMetaData.of(QContext.getQInstance()).withRetryMinutes(List.of(1));
      outcome = DeliveryResult.Status.FAILED_RETRYABLE;
      new DeliverOutputAction().execute(request("retry"));
      QRecord log = logs().get(0);
      assertTrue(log.getValueInstant("nextAttemptAt").isAfter(Instant.now()));
      DeliveryTableCustomizer.LOG_WRITE.set(true);
      try
      {
         new UpdateAction().execute(new UpdateInput(DeliveryMetaDataProvider.DELIVERY_LOG).withRecords(List.of(new QRecord()
            .withValue("id", log.getValue("id")).withValue("nextAttemptAt", Instant.now().minusSeconds(1)))));
      }
      finally
      {
         DeliveryTableCustomizer.LOG_WRITE.remove();
      }
      outcome = DeliveryResult.Status.DELIVERED;
      QContext.setQSession(new QSystemUserSession());
      new DeliveryRetryProcess().run(null, null);
      assertEquals(2, calls);
      assertEquals("retry", received.getIdempotencyKey());
      assertEquals("DELIVERED", logs().get(0).getValueString("status"));
   }



   /*******************************************************************************
    ** Generic record queries enforce owner identity; administrators can read all.
    *******************************************************************************/
   @Test
   void logIsolationAndTamperProtection() throws Exception
   {
      new DeliverOutputAction().execute(request("private"));
      QRecord log = logs().get(0);
      assertThrows(QException.class, () -> new UpdateAction().execute(new UpdateInput(DeliveryMetaDataProvider.DELIVERY_LOG)
         .withRecords(List.of(new QRecord().withValue("id", log.getValue("id")).withValue("storageReference", "tampered")))));
      QContext.setQSession(newSession("B"));
      assertTrue(logs().isEmpty());
      QContext.getQSession().withPermission(DeliveryMetaDataProvider.ADMIN_PERMISSION);
      assertEquals(1, logs().size());
   }



   /*******************************************************************************
    ** Picker options use the requesting session and never a global tenant cache.
    *******************************************************************************/
   @Test
   void pickerIsolation() throws Exception
   {
      RecordingDestination picker = new RecordingDestination();
      assertEquals(DEFAULT_USER_ID, picker.describeTargets(new DeliveryDestinationContext(), Map.of()).getOptions().get(0).getValue());
      QContext.setQSession(newSession("B"));
      assertEquals("B", picker.describeTargets(new DeliveryDestinationContext(), Map.of()).getOptions().get(0).getValue());
   }



   /*******************************************************************************
    ** Effective field metadata produces a shared highest-classification result.
    *******************************************************************************/
   @Test
   void classificationHelper() throws Exception
   {
      QFieldMetaData phi = new QFieldMetaData("patient", QFieldType.STRING).withLabel("Patient name");
      phi.withSupplementalMetaData(new AiFieldMetaData().withClassification(DataClassification.PHI));
      QTableMetaData table = new QTableMetaData().withName("payload").withField(phi).withField(new QFieldMetaData("count", QFieldType.INTEGER));
      DeliveryClassification result = DeliveryClassification.of(table, List.of("patient", "count"));
      assertEquals(DataClassification.PHI, result.classification());
      assertEquals(List.of("Patient name"), result.restrictedFieldLabels());
   }



   /*******************************************************************************
    ** Small files remain attached when a different attachment exceeds the limit.
    *******************************************************************************/
   @Test
   void mixedAttachmentSizes() throws Exception
   {
      new DeliverOutputAction().execute(request("mixed").withAttachments(List.of(attachment(5L), attachment(11L))));
      assertEquals(1, received.getAttachments().size());
      assertEquals(5L, received.getAttachments().get(0).getSize());
      assertThat(received.getLink()).startsWith("/qqq/v1/delivery/output/");
   }



   /*******************************************************************************
    ** A host without tables can send directly but cannot construct a protected output link.
    *******************************************************************************/
   @Test
   void optionalPersistence() throws Exception
   {
      QContext.getQInstance().getTables().remove(DeliveryMetaDataProvider.DELIVERY_LOG);
      assertEquals(DeliveryResult.Status.DELIVERED, new DeliverOutputAction().execute(request("direct")).getStatus());
      assertEquals(DeliveryResult.Status.REFUSED, new DeliverOutputAction().execute(request("large-direct").withAttachments(List.of(attachment(11L)))).getStatus());
      new DeliveryRetryProcess().run(null, null);
      assertEquals(1, calls);
   }



   /*******************************************************************************
    ** Exhausted retries become final failures and subsequent callers see the stored result.
    *******************************************************************************/
   @Test
   void retryExhaustionIsFinal() throws Exception
   {
      DeliveryDestinationsMetaData.of(QContext.getQInstance()).withRetryMinutes(List.of(1));
      outcome = DeliveryResult.Status.FAILED_RETRYABLE;
      assertEquals(DeliveryResult.Status.FAILED_RETRYABLE, new DeliverOutputAction().execute(request("exhausted")).getStatus());
      assertEquals(DeliveryResult.Status.FAILED, new DeliverOutputAction().execute(request("exhausted")).getStatus());
      assertEquals(DeliveryResult.Status.FAILED, new DeliverOutputAction().execute(request("exhausted")).getStatus());
      assertEquals(2, calls);
      assertNull(logs().get(0).getValue("nextAttemptAt"));
   }



   /*******************************************************************************
    ** A producer supplies a unique key and owner; storage references carry no bytes.
    *******************************************************************************/
   private DeliveryRequest request(String key)
   {
      return (new DeliveryRequest().withDestinationName("TEST").withOwnerUserId(DEFAULT_USER_ID).withIdempotencyKey(key).withTitle("Output"));
   }



   /*******************************************************************************
    ** Test attachment reference; a recording destination never reads its bytes.
    *******************************************************************************/
   private DeliveryAttachment attachment(Long bytes)
   {
      return (new DeliveryAttachment().withFileName("output.csv").withMimeType("text/csv").withSize(bytes).withStorageTableName("storage").withStorageReference("object"));
   }



   /*******************************************************************************
    ** Query through the ordinary row security pipeline.
    *******************************************************************************/
   private List<QRecord> logs() throws QException
   {
      return (new QueryAction().execute(new QueryInput(DeliveryMetaDataProvider.DELIVERY_LOG)).getRecords());
   }



   /*******************************************************************************
    ** Fake tenant-scoped destination with observable sends.
    *******************************************************************************/
   public static class RecordingDestination implements DeliveryDestinationInterface
   {
      /*******************************************************************************
       ** Expose only the current tenant option.
       *******************************************************************************/
      @Override
      public DeliveryTargetStep describeTargets(DeliveryDestinationContext context, Map<String, Serializable> chosen)
      {
         String owner = QContext.getQSession().getUser().getIdReference();
         return (new DeliveryTargetStep().withName("tenant").withOptions(List.of(new DeliveryTargetOption().withValue(owner).withLabel(owner))));
      }



      /*******************************************************************************
       ** Assert the actual execution identity rather than only the request fields.
       *******************************************************************************/
      @Override
      public DeliveryResult deliver(DeliveryRequest request)
      {
         assertFalse(QContext.getQSession() instanceof QSystemUserSession);
         assertEquals(request.getOwnerUserId(), QContext.getQSession().getUser().getIdReference());
         calls++;
         received = request;
         return (new DeliveryResult().withStatus(outcome));
      }
   }



   /*******************************************************************************
    ** Capture value-free owner notifications and operational alerts.
    *******************************************************************************/
   public static class RecordingSink implements AlertSinkInterface
   {
      /*******************************************************************************
       ** Capture the alert.
       *******************************************************************************/
      @Override
      public void raise(AlertSignal signal)
      {
         alert = signal;
      }



      /*******************************************************************************
       ** Capture the owner notification.
       *******************************************************************************/
      @Override
      public void notify(NotificationSignal signal)
      {
         notification = signal;
      }



      /*******************************************************************************
       ** No resolution is expected from these tests.
       *******************************************************************************/
      @Override
      public void resolve(AlertResolution resolution)
      {
      }



      /*******************************************************************************
       ** No heartbeat is expected from these tests.
       *******************************************************************************/
      @Override
      public void heartbeat(String key, Instant at)
      {
      }
   }
}
