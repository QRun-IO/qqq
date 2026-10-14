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


import com.kingsrook.qqq.backend.core.actions.ai.DataClassification;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;


/*******************************************************************************
 ** QDeliveryDestinationMetaData.
 *******************************************************************************/
public class QDeliveryDestinationMetaData implements java.io.Serializable
{
   /*******************************************************************************
    ** Policy for content above the destination ceiling.
    *******************************************************************************/
   public enum AboveMaxClassification
   {
      REFUSE, LINK_ONLY
   }

   private String name;
   private String label;
   private String icon;
   private String permissionName;
   private String emailProviderName;
   private Integer order = 0;
   private QCodeReference codeReference;
   private DataClassification maxClassification = DataClassification.PUBLIC;
   private Long maxAttachmentBytes;
   private boolean allowBearerLinks;
   private AboveMaxClassification aboveMaxClassification = AboveMaxClassification.REFUSE;



   /*******************************************************************************
    ** Get name.
    *******************************************************************************/
   public String getName()
   {
      return (name);
   }



   /*******************************************************************************
    ** Set name.
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** With name.
    *******************************************************************************/
   public QDeliveryDestinationMetaData withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Get label.
    *******************************************************************************/
   public String getLabel()
   {
      return (label);
   }



   /*******************************************************************************
    ** Set label.
    *******************************************************************************/
   public void setLabel(String label)
   {
      this.label = label;
   }



   /*******************************************************************************
    ** With label.
    *******************************************************************************/
   public QDeliveryDestinationMetaData withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Get icon.
    *******************************************************************************/
   public String getIcon()
   {
      return (icon);
   }



   /*******************************************************************************
    ** Set icon.
    *******************************************************************************/
   public void setIcon(String icon)
   {
      this.icon = icon;
   }



   /*******************************************************************************
    ** With icon.
    *******************************************************************************/
   public QDeliveryDestinationMetaData withIcon(String icon)
   {
      this.icon = icon;
      return (this);
   }



   /*******************************************************************************
    ** Get permissionName.
    *******************************************************************************/
   public String getPermissionName()
   {
      return (permissionName);
   }



   /*******************************************************************************
    ** Set permissionName.
    *******************************************************************************/
   public void setPermissionName(String permissionName)
   {
      this.permissionName = permissionName;
   }



   /*******************************************************************************
    ** With permissionName.
    *******************************************************************************/
   public QDeliveryDestinationMetaData withPermissionName(String permissionName)
   {
      this.permissionName = permissionName;
      return (this);
   }



   /*******************************************************************************
    ** Get emailProviderName.
    *******************************************************************************/
   public String getEmailProviderName()
   {
      return (emailProviderName);
   }



   /*******************************************************************************
    ** Set emailProviderName.
    *******************************************************************************/
   public void setEmailProviderName(String emailProviderName)
   {
      this.emailProviderName = emailProviderName;
   }



   /*******************************************************************************
    ** With emailProviderName.
    *******************************************************************************/
   public QDeliveryDestinationMetaData withEmailProviderName(String emailProviderName)
   {
      this.emailProviderName = emailProviderName;
      return (this);
   }



   /*******************************************************************************
    ** Get order.
    *******************************************************************************/
   public Integer getOrder()
   {
      return (order);
   }



   /*******************************************************************************
    ** Set order.
    *******************************************************************************/
   public void setOrder(Integer order)
   {
      this.order = order;
   }



   /*******************************************************************************
    ** With order.
    *******************************************************************************/
   public QDeliveryDestinationMetaData withOrder(Integer order)
   {
      this.order = order;
      return (this);
   }



   /*******************************************************************************
    ** Get codeReference.
    *******************************************************************************/
   public QCodeReference getCodeReference()
   {
      return (codeReference);
   }



   /*******************************************************************************
    ** Set codeReference.
    *******************************************************************************/
   public void setCodeReference(QCodeReference codeReference)
   {
      this.codeReference = codeReference;
   }



   /*******************************************************************************
    ** With codeReference.
    *******************************************************************************/
   public QDeliveryDestinationMetaData withCodeReference(QCodeReference codeReference)
   {
      this.codeReference = codeReference;
      return (this);
   }



   /*******************************************************************************
    ** Get maxClassification.
    *******************************************************************************/
   public DataClassification getMaxClassification()
   {
      return (maxClassification);
   }



   /*******************************************************************************
    ** Set maxClassification.
    *******************************************************************************/
   public void setMaxClassification(DataClassification maxClassification)
   {
      this.maxClassification = maxClassification;
   }



   /*******************************************************************************
    ** With maxClassification.
    *******************************************************************************/
   public QDeliveryDestinationMetaData withMaxClassification(DataClassification maxClassification)
   {
      this.maxClassification = maxClassification;
      return (this);
   }



   /*******************************************************************************
    ** Get maxAttachmentBytes.
    *******************************************************************************/
   public Long getMaxAttachmentBytes()
   {
      return (maxAttachmentBytes);
   }



   /*******************************************************************************
    ** Set maxAttachmentBytes.
    *******************************************************************************/
   public void setMaxAttachmentBytes(Long maxAttachmentBytes)
   {
      this.maxAttachmentBytes = maxAttachmentBytes;
   }



   /*******************************************************************************
    ** With maxAttachmentBytes.
    *******************************************************************************/
   public QDeliveryDestinationMetaData withMaxAttachmentBytes(Long maxAttachmentBytes)
   {
      this.maxAttachmentBytes = maxAttachmentBytes;
      return (this);
   }



   /*******************************************************************************
    ** Get allowBearerLinks.
    *******************************************************************************/
   public boolean getAllowBearerLinks()
   {
      return (allowBearerLinks);
   }



   /*******************************************************************************
    ** Set allowBearerLinks.
    *******************************************************************************/
   public void setAllowBearerLinks(boolean allowBearerLinks)
   {
      this.allowBearerLinks = allowBearerLinks;
   }



   /*******************************************************************************
    ** With allowBearerLinks.
    *******************************************************************************/
   public QDeliveryDestinationMetaData withAllowBearerLinks(boolean allowBearerLinks)
   {
      this.allowBearerLinks = allowBearerLinks;
      return (this);
   }



   /*******************************************************************************
    ** Get aboveMaxClassification.
    *******************************************************************************/
   public AboveMaxClassification getAboveMaxClassification()
   {
      return (aboveMaxClassification);
   }



   /*******************************************************************************
    ** Set aboveMaxClassification.
    *******************************************************************************/
   public void setAboveMaxClassification(AboveMaxClassification aboveMaxClassification)
   {
      this.aboveMaxClassification = aboveMaxClassification;
   }



   /*******************************************************************************
    ** With aboveMaxClassification.
    *******************************************************************************/
   public QDeliveryDestinationMetaData withAboveMaxClassification(AboveMaxClassification aboveMaxClassification)
   {
      this.aboveMaxClassification = aboveMaxClassification;
      return (this);
   }
}
