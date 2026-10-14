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


import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.alerts.AlertLink;
import com.kingsrook.qqq.backend.core.alerts.AlertSignal;
import com.kingsrook.qqq.backend.core.alerts.NotificationAudience;
import com.kingsrook.qqq.backend.core.alerts.NotificationCategory;
import com.kingsrook.qqq.backend.core.alerts.NotificationSignal;
import com.kingsrook.qqq.backend.core.alerts.QAlerts;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;


/*******************************************************************************
 ** Enforce owner identity and egress policy before invoking any destination.
 *******************************************************************************/
public class DeliverOutputAction
{
   /*******************************************************************************
    ** Run in a temporary owner context; never leave a scheduler impersonating a user.
    *******************************************************************************/
   public DeliveryResult execute(DeliveryRequest request) throws QException
   {
      if(request == null || request.getIdempotencyKey() == null || request.getIdempotencyKey().isBlank())
      {
         throw new QException("Delivery requires an idempotency key.");
      }
      QSession owner;
      try
      {
         owner = DeliverySessions.forOwner(request.getOwnerUserId());
      }
      catch(QException e)
      {
         if(QContext.getQSession() instanceof QSystemUserSession && QContext.getQInstance().getTable(DeliveryMetaDataProvider.DELIVERY_LOG) != null)
         {
            QRecord existing = findLog(request.getIdempotencyKey());
            if(existing != null)
            {
               if(!Objects.equals(existing.getValueString("ownerUserId"), request.getOwnerUserId()))
               {
                  return (unauthorized(request));
               }
               if(existing.getValueString("status") != null && !"FAILED_RETRYABLE".equals(existing.getValueString("status")))
               {
                  return (storedResult(existing));
               }
            }
         }
         DeliveryResult denied = unauthorized(request);
         if(QContext.getQSession() instanceof QSystemUserSession && QContext.getQInstance().getTable(DeliveryMetaDataProvider.DELIVERY_LOG) != null)
         {
            boolean previous = DeliveryTableCustomizer.LOG_WRITE.get();
            DeliveryTableCustomizer.LOG_WRITE.set(true);
            try
            {
               QRecord log = findLog(request.getIdempotencyKey());
               if(log == null)
               {
                  log = createLog(request);
               }
               saveResult(log, denied, false);
            }
            finally
            {
               DeliveryTableCustomizer.LOG_WRITE.set(previous);
            }
         }
         return (denied);
      }
      DeliveryResult[] result = new DeliveryResult[1];
      QContext.withTemporaryContext(QContext.capture(), () ->
      {
         QContext.setQSession(owner);
         synchronized(QContext.getQInstance())
         {
            boolean previous = DeliveryTableCustomizer.LOG_WRITE.get();
            DeliveryTableCustomizer.LOG_WRITE.set(true);
            try
            {
               result[0] = executeAsOwner(request);
            }
            finally
            {
               DeliveryTableCustomizer.LOG_WRITE.set(previous);
            }
         }
      });
      return (result[0]);
   }



   /*******************************************************************************
    ** Read the durable key first, then record exactly the payload used for retries.
    *******************************************************************************/
   private DeliveryResult executeAsOwner(DeliveryRequest original) throws QException
   {
      boolean persistent = QContext.getQInstance().getTable(DeliveryMetaDataProvider.DELIVERY_LOG) != null;
      QRecord log = persistent ? findLog(original.getIdempotencyKey()) : null;
      if(log != null && !Objects.equals(log.getValueString("ownerUserId"), original.getOwnerUserId()))
      {
         return (unauthorized(original));
      }
      if(log != null && log.getValueString("status") != null && !"FAILED_RETRYABLE".equals(log.getValueString("status")))
      {
         return (storedResult(log));
      }
      if(log != null)
      {
         try
         {
            original = JsonUtils.toObject(log.getValueString("storageReference"), DeliveryRequest.class);
         }
         catch(java.io.IOException e)
         {
            throw new QException("Could not read stored delivery request.", e);
         }
      }
      QDeliveryDestinationMetaData destination = DeliveryDestinations.find(original.getDestinationName());
      if(destination == null)
      {
         DeliveryResult result = unauthorized(original);
         if(persistent)
         {
            log = log == null ? createLog(original) : log;
            saveResult(log, result, false);
         }
         return (result);
      }
      DeliveryRequest request = copy(original);
      if(log == null && persistent)
      {
         log = createLog(original);
      }
      if(log != null)
      {
         request.setLink("/qqq/v1/delivery/output/" + log.getValue("id"));
      }
      boolean above = request.getContentClassification() != null && !destination.getMaxClassification().isAtLeast(request.getContentClassification());
      boolean asLink = above || request.getAttachments().stream().anyMatch(attachment -> destination.getMaxAttachmentBytes() == null
         || attachment.getSize() == null || attachment.getSize() > destination.getMaxAttachmentBytes());
      DeliveryResult result;
      if(above && destination.getAboveMaxClassification() == QDeliveryDestinationMetaData.AboveMaxClassification.REFUSE)
      {
         result = new DeliveryResult().withStatus(DeliveryResult.Status.REFUSED).withReasons(List.of("This output includes restricted fields ("
            + String.join(", ", request.getRestrictedFieldLabels()) + "). " + destination.getLabel()
            + " can't receive them. Leave them out, or choose another destination."));
      }
      else if(asLink && log == null)
      {
         result = new DeliveryResult().withStatus(DeliveryResult.Status.REFUSED).withReasons(List.of("Stored delivery output is required for an authenticated link."));
      }
      else
      {
         if(asLink)
         {
            request.setAttachments(above ? List.of() : request.getAttachments().stream()
               .filter(attachment -> destination.getMaxAttachmentBytes() != null && attachment.getSize() != null && attachment.getSize() <= destination.getMaxAttachmentBytes()).toList());
            if(above)
            {
               request.setSummary(null);
               request.setRestrictedFieldLabels(List.of());
               request.setContentClassification(null);
            }
         }
         DeliveryDestinationInterface implementation = DeliveryDestinations.implementation(destination);
         try
         {
            List<String> reasons = implementation.validate(request);
            result = reasons.isEmpty() ? implementation.deliver(request) : new DeliveryResult().withStatus(DeliveryResult.Status.REFUSED).withReasons(reasons);
            if(result == null || result.getStatus() == null)
            {
               result = new DeliveryResult().withStatus(DeliveryResult.Status.FAILED).withReasons(List.of("The destination returned no delivery outcome."));
            }
         }
         catch(QPermissionDeniedException e)
         {
            result = new DeliveryResult().withStatus(DeliveryResult.Status.UNAUTHORIZED);
         }
         catch(QException e)
         {
            result = new DeliveryResult().withStatus(DeliveryResult.Status.FAILED_RETRYABLE).withReasons(List.of("The destination could not accept this output."));
         }
         if(result.getStatus() == DeliveryResult.Status.UNAUTHORIZED)
         {
            result = unauthorized(original);
         }
      }
      if(log != null)
      {
         saveResult(log, result, asLink);
      }
      return (result);
   }



   /*******************************************************************************
    ** Copy mutable producer input before removing restricted content.
    *******************************************************************************/
   private DeliveryRequest copy(DeliveryRequest request) throws QException
   {
      try
      {
         return (JsonUtils.toObject(JsonUtils.toJson(request), DeliveryRequest.class));
      }
      catch(Exception e)
      {
         throw new QException("Could not prepare delivery request.", e);
      }
   }



   /*******************************************************************************
    ** Look up a key through the owner's row security.
    *******************************************************************************/
   private QRecord findLog(String key) throws QException
   {
      List<QRecord> records = new QueryAction().execute(new QueryInput(DeliveryMetaDataProvider.DELIVERY_LOG)
         .withFilter(new QQueryFilter().withCriteria(new QFilterCriteria("idempotencyKey", QCriteriaOperator.EQUALS, key)))).getRecords();
      return (records.isEmpty() ? null : records.get(0));
   }



   /*******************************************************************************
    ** Store a reference envelope so retries retain target and storage ids.
    ** It contains no file bytes or credentials.
    *******************************************************************************/
   private QRecord createLog(DeliveryRequest request) throws QException
   {
      QRecord record = new QRecord().withValue("ownerUserId", request.getOwnerUserId()).withValue("sourceType", request.getSourceType())
         .withValue("sourceKey", request.getSourceKey()).withValue("destinationName", request.getDestinationName())
         .withValue("idempotencyKey", request.getIdempotencyKey()).withValue("attemptCount", 0).withValue("createdAt", Instant.now())
         .withValue("contentClassification", request.getContentClassification() == null ? null : request.getContentClassification().name())
         .withValue("storageReference", JsonUtils.toJson(request));
      if(!request.getAttachments().isEmpty())
      {
         DeliveryAttachment attachment = request.getAttachments().get(0);
         record.withValue("attachmentName", attachment.getFileName()).withValue("attachmentBytes", attachment.getSize());
      }
      QRecord saved = new InsertAction().execute(new InsertInput(DeliveryMetaDataProvider.DELIVERY_LOG).withRecords(List.of(record))).getRecords().get(0);
      assertSaved(saved);
      return (saved);
   }



   /*******************************************************************************
    ** Persist outcome and bounded retry backoff.
    *******************************************************************************/
   private void saveResult(QRecord log, DeliveryResult result, boolean asLink) throws QException
   {
      int attempts = log.getValueInteger("attemptCount") + 1;
      List<Integer> delays = DeliveryDestinationsMetaData.of(QContext.getQInstance()).getRetryMinutes();
      if(result.getStatus() == DeliveryResult.Status.FAILED_RETRYABLE && attempts > delays.size())
      {
         result.setStatus(DeliveryResult.Status.FAILED);
      }
      QRecord update = new QRecord().withValue("id", log.getValue("id")).withValue("status", result.getStatus().name())
         .withValue("externalReference", result.getExternalReference()).withValue("lastError", JsonUtils.toJson(result.getReasons()))
         .withValue("attemptCount", attempts).withValue("deliveredAsLink", asLink)
         .withValue("nextAttemptAt", result.getStatus() == DeliveryResult.Status.FAILED_RETRYABLE ? Instant.now().plusSeconds(60L * delays.get(attempts - 1)) : null)
         .withValue("deliveredAt", result.getStatus() == DeliveryResult.Status.DELIVERED ? Instant.now() : null);
      assertSaved(new UpdateAction().execute(new UpdateInput(DeliveryMetaDataProvider.DELIVERY_LOG).withRecords(List.of(update))).getRecords().get(0));
   }



   /*******************************************************************************
    ** Do not report success if the backend rejected the log write.
    *******************************************************************************/
   private void assertSaved(QRecord record) throws QException
   {
      if(CollectionUtils.nullSafeHasContents(record.getErrors()))
      {
         throw new QException("Could not persist delivery log.");
      }
   }



   /*******************************************************************************
    ** Return the stored final outcome without invoking the destination.
    *******************************************************************************/
   private DeliveryResult storedResult(QRecord log) throws QException
   {
      try
      {
         List<String> reasons = log.getValueString("lastError") == null ? List.of() : List.of(JsonUtils.toObject(log.getValueString("lastError"), String[].class));
         return (new DeliveryResult().withStatus(DeliveryResult.Status.valueOf(log.getValueString("status")))
            .withExternalReference(log.getValueString("externalReference")).withReasons(reasons));
      }
      catch(Exception e)
      {
         throw new QException("Could not read stored delivery result.", e);
      }
   }



   /*******************************************************************************
    ** A stable daily key lets the alert provider deduplicate owner notifications.
    *******************************************************************************/
   private DeliveryResult unauthorized(DeliveryRequest request)
   {
      String key = request.getOwnerUserId() + ":" + request.getDestinationName() + ":" + LocalDate.now(ZoneOffset.UTC);
      QAlerts.notify(new NotificationSignal("Output delivery needs your attention").withCategory(NotificationCategory.SYSTEM)
         .withAudience(NotificationAudience.user(request.getOwnerUserId())).withSourceName("delivery.unauthorized").withSourceKey(key)
         .withLink(request.getLink() == null ? null : AlertLink.toPath(request.getLink(), "Open source"))
         .withBody("You no longer have access to this delivery destination. No output was sent."));
      QAlerts.raise(new AlertSignal("delivery.unauthorized").withDedupKey(key).withTitle("Output delivery unauthorized"));
      return (new DeliveryResult().withStatus(DeliveryResult.Status.UNAUTHORIZED).withReasons(List.of("The owner is not authorized to use this destination.")));
   }
}
