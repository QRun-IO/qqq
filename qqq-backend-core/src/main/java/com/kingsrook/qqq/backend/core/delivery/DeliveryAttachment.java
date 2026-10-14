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




/*******************************************************************************
 ** DeliveryAttachment.
 *******************************************************************************/
public class DeliveryAttachment implements java.io.Serializable
{
   private String fileName;
   private String mimeType;
   private Long size;
   private String storageTableName;
   private String storageReference;



   /*******************************************************************************
    ** Get fileName.
    *******************************************************************************/
   public String getFileName()
   {
      return (fileName);
   }



   /*******************************************************************************
    ** Set fileName.
    *******************************************************************************/
   public void setFileName(String fileName)
   {
      this.fileName = fileName;
   }



   /*******************************************************************************
    ** With fileName.
    *******************************************************************************/
   public DeliveryAttachment withFileName(String fileName)
   {
      this.fileName = fileName;
      return (this);
   }



   /*******************************************************************************
    ** Get mimeType.
    *******************************************************************************/
   public String getMimeType()
   {
      return (mimeType);
   }



   /*******************************************************************************
    ** Set mimeType.
    *******************************************************************************/
   public void setMimeType(String mimeType)
   {
      this.mimeType = mimeType;
   }



   /*******************************************************************************
    ** With mimeType.
    *******************************************************************************/
   public DeliveryAttachment withMimeType(String mimeType)
   {
      this.mimeType = mimeType;
      return (this);
   }



   /*******************************************************************************
    ** Get size.
    *******************************************************************************/
   public Long getSize()
   {
      return (size);
   }



   /*******************************************************************************
    ** Set size.
    *******************************************************************************/
   public void setSize(Long size)
   {
      this.size = size;
   }



   /*******************************************************************************
    ** With size.
    *******************************************************************************/
   public DeliveryAttachment withSize(Long size)
   {
      this.size = size;
      return (this);
   }



   /*******************************************************************************
    ** Get storageTableName.
    *******************************************************************************/
   public String getStorageTableName()
   {
      return (storageTableName);
   }



   /*******************************************************************************
    ** Set storageTableName.
    *******************************************************************************/
   public void setStorageTableName(String storageTableName)
   {
      this.storageTableName = storageTableName;
   }



   /*******************************************************************************
    ** With storageTableName.
    *******************************************************************************/
   public DeliveryAttachment withStorageTableName(String storageTableName)
   {
      this.storageTableName = storageTableName;
      return (this);
   }



   /*******************************************************************************
    ** Get storageReference.
    *******************************************************************************/
   public String getStorageReference()
   {
      return (storageReference);
   }



   /*******************************************************************************
    ** Set storageReference.
    *******************************************************************************/
   public void setStorageReference(String storageReference)
   {
      this.storageReference = storageReference;
   }



   /*******************************************************************************
    ** With storageReference.
    *******************************************************************************/
   public DeliveryAttachment withStorageReference(String storageReference)
   {
      this.storageReference = storageReference;
      return (this);
   }
}
