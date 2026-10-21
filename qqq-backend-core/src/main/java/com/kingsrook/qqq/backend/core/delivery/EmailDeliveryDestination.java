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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.ai.DataClassification;
import com.kingsrook.qqq.backend.core.actions.messaging.SendMessageAction;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.messaging.Attachment;
import com.kingsrook.qqq.backend.core.model.actions.messaging.Content;
import com.kingsrook.qqq.backend.core.model.actions.messaging.MultiParty;
import com.kingsrook.qqq.backend.core.model.actions.messaging.Party;
import com.kingsrook.qqq.backend.core.model.actions.messaging.SendMessageInput;
import com.kingsrook.qqq.backend.core.model.actions.messaging.SendMessageOutput;
import com.kingsrook.qqq.backend.core.model.actions.messaging.email.EmailContentRole;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.modules.messaging.MessagingProviderCapability;
import com.kingsrook.qqq.backend.core.utils.ValidationUtils;


/*******************************************************************************
 ** Email through the configured core messaging provider, always as the output owner.
 *******************************************************************************/
public class EmailDeliveryDestination implements DeliveryDestinationInterface
{
   public static final String OTHERS_PERMISSION = "delivery.email.others";



   /*******************************************************************************
    ** Conservative email policy; hosts may explicitly replace this destination.
    *******************************************************************************/
   public static QDeliveryDestinationMetaData metadata()
   {
      return (new QDeliveryDestinationMetaData().withName("EMAIL").withLabel("Email").withIcon("email").withOrder(10)
         .withMaxClassification(DataClassification.CONFIDENTIAL).withAboveMaxClassification(QDeliveryDestinationMetaData.AboveMaxClassification.LINK_ONLY)
         .withMaxAttachmentBytes(10L * 1024 * 1024).withCodeReference(new QCodeReference(EmailDeliveryDestination.class)));
   }



   /*******************************************************************************
    ** Offer only owner-authorized target choices.
    *******************************************************************************/
   @Override
   public DeliveryTargetStep describeTargets(DeliveryDestinationContext context, Map<String, Serializable> chosen) throws QException
   {
      if("me".equals(chosen.get("recipient")))
      {
         return (new DeliveryTargetStep().withComplete(true).withSummary("Email: Me"));
      }
      if("others".equals(chosen.get("recipient")))
      {
         requireOthers();
         return (chosen.containsKey("addresses") ? new DeliveryTargetStep().withComplete(true).withSummary("Email: selected addresses")
            : new DeliveryTargetStep().withName("addresses").withLabel("Email addresses").withFreeText(true));
      }
      List<DeliveryTargetOption> options = new ArrayList<>();
      options.add(new DeliveryTargetOption().withValue("me").withLabel("Me"));
      if(QContext.getQSession().hasPermission(OTHERS_PERMISSION))
      {
         options.add(new DeliveryTargetOption().withValue("others").withLabel("Other email addresses"));
      }
      return (new DeliveryTargetStep().withName("recipient").withLabel("Send to").withOptions(options));
   }



   /*******************************************************************************
    ** Require explicit permission for free-text recipients on every send and retry.
    *******************************************************************************/
   private void requireOthers() throws QPermissionDeniedException
   {
      if(!QContext.getQSession().hasPermission(OTHERS_PERMISSION))
      {
         throw new QPermissionDeniedException("Sending email to other people is not permitted.");
      }
   }



   /*******************************************************************************
    ** Resolve Me from the authenticated owner, never from picker-supplied identity.
    *******************************************************************************/
   private String addresses(DeliveryRequest request) throws QException
   {
      if("me".equals(request.getTargetValues().get("recipient")))
      {
         return (QContext.getQSession().getUser().getEmail());
      }
      if("others".equals(request.getTargetValues().get("recipient")))
      {
         requireOthers();
         return ((String) request.getTargetValues().get("addresses"));
      }
      return (null);
   }



   /*******************************************************************************
    ** Resolve destination-specific provider before the instance default.
    *******************************************************************************/
   private String provider(DeliveryRequest request) throws QException
   {
      QDeliveryDestinationMetaData metadata = DeliveryDestinations.find(request.getDestinationName());
      return (metadata.getEmailProviderName() == null ? DeliveryDestinationsMetaData.of(QContext.getQInstance()).getEmailProviderName() : metadata.getEmailProviderName());
   }



   /*******************************************************************************
    ** Fail closed for missing email, invalid target, or unsupported streaming.
    *******************************************************************************/
   @Override
   public List<String> validate(DeliveryRequest request) throws QException
   {
      String addresses = addresses(request);
      if(addresses == null || addresses.isBlank())
      {
         return (List.of("No email address is available for this target."));
      }
      try
      {
         ValidationUtils.parseAndValidateEmailAddresses(addresses);
      }
      catch(QUserFacingException e)
      {
         return (List.of("The email target contains an invalid address."));
      }
      String provider = provider(request);
      if(!SendMessageAction.isAvailable(provider))
      {
         return (List.of("The email provider is not configured."));
      }
      if(!request.getAttachments().isEmpty() && !SendMessageAction.capabilities(provider).contains(MessagingProviderCapability.STREAMING_ATTACHMENTS))
      {
         return (List.of("The email provider does not support streaming attachments."));
      }
      return (List.of());
   }



   /*******************************************************************************
    ** Stream attachments; pass the delivery key to providers that support idempotency.
    *******************************************************************************/
   @Override
   public DeliveryResult deliver(DeliveryRequest request) throws QException
   {
      MultiParty recipients = new MultiParty();
      for(String address : ValidationUtils.parseAndValidateEmailAddresses(addresses(request)))
      {
         recipients.withParty(new Party().withAddress(address));
      }
      String provider = provider(request);
      SendMessageInput input = new SendMessageInput().withMessagingProviderName(provider).withTo(recipients).withSubject(request.getTitle())
         .withContent(new Content().withContentRole(EmailContentRole.TEXT).withBody((request.getSummary() == null ? "" : request.getSummary() + "\n\n")
            + (request.getLink() == null ? "" : request.getLink())));
      if(SendMessageAction.capabilities(provider).contains(MessagingProviderCapability.IDEMPOTENCY))
      {
         input.withIdempotencyKey(request.getIdempotencyKey());
      }
      String fromAddress = DeliveryDestinationsMetaData.of(QContext.getQInstance()).getEmailFromAddress();
      if(fromAddress == null)
      {
         fromAddress = QContext.getQSession().getUser().getEmail();
      }
      if(fromAddress != null)
      {
         input.withFrom(new Party().withAddress(fromAddress));
      }
      for(DeliveryAttachment attachment : request.getAttachments())
      {
         input.withAttachment(new Attachment().withName(attachment.getFileName()).withContentType(attachment.getMimeType()).withStreamSupplier(() ->
            new StorageAction().getInputStream(new StorageInput(attachment.getStorageTableName()).withReference(attachment.getStorageReference()))));
      }
      SendMessageOutput output = new SendMessageAction().execute(input);
      DeliveryResult.Status status = switch(output.getStatus())
      {
         case SENT, DUPLICATE -> DeliveryResult.Status.DELIVERED;
         case QUEUED -> DeliveryResult.Status.QUEUED;
         case SUPPRESSED, REFUSED -> DeliveryResult.Status.REFUSED;
      };
      return (new DeliveryResult().withStatus(status).withExternalReference(output.getMessageId() == null ? null : output.getMessageId().toString())
         .withReasons(output.getReasons() == null ? List.of() : output.getReasons()));
   }
}
