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


import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.actions.messaging.MultiParty;
import com.kingsrook.qqq.backend.core.model.actions.messaging.SendMessageInput;
import com.kingsrook.qqq.backend.core.model.actions.messaging.SendMessageOutput;
import com.kingsrook.qqq.backend.core.model.actions.messaging.SendMessageStatus;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.messaging.QMessagingProviderMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.modules.messaging.MessagingProviderCapability;
import com.kingsrook.qqq.backend.core.modules.messaging.MessagingProviderInterface;
import com.kingsrook.qqq.backend.core.modules.messaging.QMessagingProviderDispatcher;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Transport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;


/*******************************************************************************
 ** Email recipients, provider selection and streaming attachment dispatch.
 *******************************************************************************/
class EmailDeliveryDestinationTest extends BaseTest
{
   private static SendMessageInput sent;
   private static String attachmentContents;



   /*******************************************************************************
    ** Register an in-memory streaming messaging provider; no email is sent externally.
    *******************************************************************************/
   @BeforeEach
   void setup() throws Exception
   {
      sent = null;
      attachmentContents = null;
      new DeliveryMetaDataProvider().defineAll(QContext.getQInstance(), TestUtils.MEMORY_BACKEND_NAME, null);
      QMessagingProviderDispatcher.registerMessagingProvider(new Provider());
      QContext.getQInstance().addMessagingProvider(new QMessagingProviderMetaData().withName("deliveryEmail").withType("DELIVERY_TEST_EMAIL"));
      DeliveryDestinationsMetaData.of(QContext.getQInstance()).withEmailProviderName("deliveryEmail");
      QContext.getQSession().getUser().setEmail("owner@example.com");
      QContext.getQInstance().addTable(new QTableMetaData().withName("deliveryStorage").withBackendName(TestUtils.MEMORY_BACKEND_NAME).withPrimaryKeyField("reference")
         .withField(new QFieldMetaData("reference", QFieldType.STRING)).withField(new QFieldMetaData("contents", QFieldType.BLOB)));
      new QInstanceEnricher(QContext.getQInstance()).enrich();
   }



   /*******************************************************************************
    ** Me uses the authenticated user's email, and the provider reads a storage stream.
    *******************************************************************************/
   @Test
   void meAndStreamingAttachment() throws Exception
   {
      try(OutputStream output = new StorageAction().createOutputStream(new StorageInput("deliveryStorage").withReference("object")))
      {
         output.write("payload".getBytes(StandardCharsets.UTF_8));
      }
      DeliveryResult result = new DeliverOutputAction().execute(new DeliveryRequest().withOwnerUserId(DEFAULT_USER_ID).withDestinationName("EMAIL")
         .withIdempotencyKey("email").withTitle("Output").withTargetValues(Map.of("recipient", "me", "addresses", "attacker@example.com"))
         .withAttachments(List.of(new DeliveryAttachment().withFileName("output.txt").withSize(7L).withStorageTableName("deliveryStorage").withStorageReference("object"))));
      assertEquals(DeliveryResult.Status.QUEUED, result.getStatus());
      assertEquals("owner@example.com", ((MultiParty) sent.getTo()).getPartyList().get(0).getAddress());
      assertEquals("email", sent.getIdempotencyKey());
      assertNull(sent.getAttachmentList().get(0).getContents());
      assertEquals("payload", attachmentContents);
   }



   /*******************************************************************************
    ** The built-in SMTP transport receives MIME attachment streams without network IO.
    *******************************************************************************/
   @Test
   void nativeSmtpStreamsAttachments() throws Exception
   {
      DeliveryDestinations.find("EMAIL").withEmailProviderName(TestUtils.EMAIL_MESSAGING_PROVIDER_NAME);
      try(OutputStream output = new StorageAction().createOutputStream(new StorageInput("deliveryStorage").withReference("native")))
      {
         output.write("native payload".getBytes(StandardCharsets.UTF_8));
      }
      try(MockedStatic<Transport> transport = mockStatic(Transport.class))
      {
         transport.when(() -> Transport.send(any(Message.class))).thenAnswer(invocation ->
         {
            Message message = invocation.getArgument(0);
            Multipart multipart = (Multipart) message.getContent();
            assertEquals(2, multipart.getCount());
            try(var stream = multipart.getBodyPart(1).getInputStream())
            {
               assertEquals("native payload", new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            }
            return (null);
         });
         DeliveryResult result = new DeliverOutputAction().execute(new DeliveryRequest().withOwnerUserId(DEFAULT_USER_ID).withDestinationName("EMAIL")
            .withIdempotencyKey("smtp").withTitle("Output").withTargetValues(Map.of("recipient", "me"))
            .withAttachments(List.of(new DeliveryAttachment().withFileName("output.txt").withMimeType("text/plain").withSize(14L)
               .withStorageTableName("deliveryStorage").withStorageReference("native"))));
         assertEquals(DeliveryResult.Status.DELIVERED, result.getStatus());
         transport.verify(() -> Transport.send(any(Message.class)));
      }
   }



   /*******************************************************************************
    ** Other recipients require the permission both in the picker and at send time.
    *******************************************************************************/
   @Test
   void othersRequirePermission() throws Exception
   {
      EmailDeliveryDestination destination = new EmailDeliveryDestination();
      assertEquals(1, destination.describeTargets(new DeliveryDestinationContext(), Map.of()).getOptions().size());
      assertThrows(QPermissionDeniedException.class, () -> destination.describeTargets(new DeliveryDestinationContext(), Map.of("recipient", "others")));
      QContext.getQSession().withPermission(EmailDeliveryDestination.OTHERS_PERMISSION);
      assertEquals(2, destination.describeTargets(new DeliveryDestinationContext(), Map.of()).getOptions().size());
      assertTrue(destination.describeTargets(new DeliveryDestinationContext(), Map.of("recipient", "others")).getFreeText());
      assertTrue(destination.describeTargets(new DeliveryDestinationContext(), Map.of("recipient", "others", "addresses", "other@example.com")).getComplete());
   }



   /*******************************************************************************
    ** Test provider consumes a stream and returns a durable queue reference.
    *******************************************************************************/
   public static class Provider implements MessagingProviderInterface
   {
      /*******************************************************************************
       ** A distinct provider type avoids modifying production registrations.
       *******************************************************************************/
      @Override
      public String getType()
      {
         return ("DELIVERY_TEST_EMAIL");
      }



      /*******************************************************************************
       ** This fake understands streamed attachments and idempotency keys.
       *******************************************************************************/
      @Override
      public Set<MessagingProviderCapability> getCapabilities()
      {
         return (Set.of(MessagingProviderCapability.STREAMING_ATTACHMENTS, MessagingProviderCapability.IDEMPOTENCY));
      }



      /*******************************************************************************
       ** Read and close a storage stream as a real messaging transport would.
       *******************************************************************************/
      @Override
      public SendMessageOutput sendMessage(SendMessageInput input) throws QException
      {
         sent = input;
         if(input.getAttachmentList() != null)
         {
            try(var stream = input.getAttachmentList().get(0).openStream())
            {
               attachmentContents = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
            catch(Exception e)
            {
               throw new QException("Test attachment failed", e);
            }
         }
         return (new SendMessageOutput().withStatus(SendMessageStatus.QUEUED).withMessageId("queued"));
      }
   }
}
