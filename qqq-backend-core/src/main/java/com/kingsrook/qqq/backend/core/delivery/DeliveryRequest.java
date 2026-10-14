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
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.ai.DataClassification;


/*******************************************************************************
 ** DeliveryRequest.
 *******************************************************************************/
public class DeliveryRequest implements java.io.Serializable
{
   private String destinationName;
   private String ownerUserId;
   private String sourceType;
   private String sourceKey;
   private String title;
   private String summary;
   private String link;
   private String idempotencyKey;
   private Map<String, Serializable> targetValues = Map.of();
   private List<DeliveryAttachment> attachments = List.of();
   private DataClassification contentClassification;
   private List<String> restrictedFieldLabels = List.of();
   private boolean leaveOutRestricted;



   /*******************************************************************************
    ** Get destinationName.
    *******************************************************************************/
   public String getDestinationName()
   {
      return (destinationName);
   }



   /*******************************************************************************
    ** Set destinationName.
    *******************************************************************************/
   public void setDestinationName(String destinationName)
   {
      this.destinationName = destinationName;
   }



   /*******************************************************************************
    ** With destinationName.
    *******************************************************************************/
   public DeliveryRequest withDestinationName(String destinationName)
   {
      this.destinationName = destinationName;
      return (this);
   }



   /*******************************************************************************
    ** Get ownerUserId.
    *******************************************************************************/
   public String getOwnerUserId()
   {
      return (ownerUserId);
   }



   /*******************************************************************************
    ** Set ownerUserId.
    *******************************************************************************/
   public void setOwnerUserId(String ownerUserId)
   {
      this.ownerUserId = ownerUserId;
   }



   /*******************************************************************************
    ** With ownerUserId.
    *******************************************************************************/
   public DeliveryRequest withOwnerUserId(String ownerUserId)
   {
      this.ownerUserId = ownerUserId;
      return (this);
   }



   /*******************************************************************************
    ** Get sourceType.
    *******************************************************************************/
   public String getSourceType()
   {
      return (sourceType);
   }



   /*******************************************************************************
    ** Set sourceType.
    *******************************************************************************/
   public void setSourceType(String sourceType)
   {
      this.sourceType = sourceType;
   }



   /*******************************************************************************
    ** With sourceType.
    *******************************************************************************/
   public DeliveryRequest withSourceType(String sourceType)
   {
      this.sourceType = sourceType;
      return (this);
   }



   /*******************************************************************************
    ** Get sourceKey.
    *******************************************************************************/
   public String getSourceKey()
   {
      return (sourceKey);
   }



   /*******************************************************************************
    ** Set sourceKey.
    *******************************************************************************/
   public void setSourceKey(String sourceKey)
   {
      this.sourceKey = sourceKey;
   }



   /*******************************************************************************
    ** With sourceKey.
    *******************************************************************************/
   public DeliveryRequest withSourceKey(String sourceKey)
   {
      this.sourceKey = sourceKey;
      return (this);
   }



   /*******************************************************************************
    ** Get title.
    *******************************************************************************/
   public String getTitle()
   {
      return (title);
   }



   /*******************************************************************************
    ** Set title.
    *******************************************************************************/
   public void setTitle(String title)
   {
      this.title = title;
   }



   /*******************************************************************************
    ** With title.
    *******************************************************************************/
   public DeliveryRequest withTitle(String title)
   {
      this.title = title;
      return (this);
   }



   /*******************************************************************************
    ** Get summary.
    *******************************************************************************/
   public String getSummary()
   {
      return (summary);
   }



   /*******************************************************************************
    ** Set summary.
    *******************************************************************************/
   public void setSummary(String summary)
   {
      this.summary = summary;
   }



   /*******************************************************************************
    ** With summary.
    *******************************************************************************/
   public DeliveryRequest withSummary(String summary)
   {
      this.summary = summary;
      return (this);
   }



   /*******************************************************************************
    ** Get link.
    *******************************************************************************/
   public String getLink()
   {
      return (link);
   }



   /*******************************************************************************
    ** Set link.
    *******************************************************************************/
   public void setLink(String link)
   {
      this.link = link;
   }



   /*******************************************************************************
    ** With link.
    *******************************************************************************/
   public DeliveryRequest withLink(String link)
   {
      this.link = link;
      return (this);
   }



   /*******************************************************************************
    ** Get idempotencyKey.
    *******************************************************************************/
   public String getIdempotencyKey()
   {
      return (idempotencyKey);
   }



   /*******************************************************************************
    ** Set idempotencyKey.
    *******************************************************************************/
   public void setIdempotencyKey(String idempotencyKey)
   {
      this.idempotencyKey = idempotencyKey;
   }



   /*******************************************************************************
    ** With idempotencyKey.
    *******************************************************************************/
   public DeliveryRequest withIdempotencyKey(String idempotencyKey)
   {
      this.idempotencyKey = idempotencyKey;
      return (this);
   }



   /*******************************************************************************
    ** Get targetValues.
    *******************************************************************************/
   public Map<String, Serializable> getTargetValues()
   {
      return (targetValues);
   }



   /*******************************************************************************
    ** Set targetValues.
    *******************************************************************************/
   public void setTargetValues(Map<String, Serializable> targetValues)
   {
      this.targetValues = targetValues;
   }



   /*******************************************************************************
    ** With targetValues.
    *******************************************************************************/
   public DeliveryRequest withTargetValues(Map<String, Serializable> targetValues)
   {
      this.targetValues = targetValues;
      return (this);
   }



   /*******************************************************************************
    ** Get attachments.
    *******************************************************************************/
   public List<DeliveryAttachment> getAttachments()
   {
      return (attachments);
   }



   /*******************************************************************************
    ** Set attachments.
    *******************************************************************************/
   public void setAttachments(List<DeliveryAttachment> attachments)
   {
      this.attachments = attachments;
   }



   /*******************************************************************************
    ** With attachments.
    *******************************************************************************/
   public DeliveryRequest withAttachments(List<DeliveryAttachment> attachments)
   {
      this.attachments = attachments;
      return (this);
   }



   /*******************************************************************************
    ** Get contentClassification.
    *******************************************************************************/
   public DataClassification getContentClassification()
   {
      return (contentClassification);
   }



   /*******************************************************************************
    ** Set contentClassification.
    *******************************************************************************/
   public void setContentClassification(DataClassification contentClassification)
   {
      this.contentClassification = contentClassification;
   }



   /*******************************************************************************
    ** With contentClassification.
    *******************************************************************************/
   public DeliveryRequest withContentClassification(DataClassification contentClassification)
   {
      this.contentClassification = contentClassification;
      return (this);
   }



   /*******************************************************************************
    ** Get restrictedFieldLabels.
    *******************************************************************************/
   public List<String> getRestrictedFieldLabels()
   {
      return (restrictedFieldLabels);
   }



   /*******************************************************************************
    ** Set restrictedFieldLabels.
    *******************************************************************************/
   public void setRestrictedFieldLabels(List<String> restrictedFieldLabels)
   {
      this.restrictedFieldLabels = restrictedFieldLabels;
   }



   /*******************************************************************************
    ** With restrictedFieldLabels.
    *******************************************************************************/
   public DeliveryRequest withRestrictedFieldLabels(List<String> restrictedFieldLabels)
   {
      this.restrictedFieldLabels = restrictedFieldLabels;
      return (this);
   }



   /*******************************************************************************
    ** Get leaveOutRestricted.
    *******************************************************************************/
   public boolean getLeaveOutRestricted()
   {
      return (leaveOutRestricted);
   }



   /*******************************************************************************
    ** Set leaveOutRestricted.
    *******************************************************************************/
   public void setLeaveOutRestricted(boolean leaveOutRestricted)
   {
      this.leaveOutRestricted = leaveOutRestricted;
   }



   /*******************************************************************************
    ** With leaveOutRestricted.
    *******************************************************************************/
   public DeliveryRequest withLeaveOutRestricted(boolean leaveOutRestricted)
   {
      this.leaveOutRestricted = leaveOutRestricted;
      return (this);
   }
}
