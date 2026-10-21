/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.actions.tables.storage;


import java.io.Serializable;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;


/*******************************************************************************
 ** Input for Storage actions.
 *******************************************************************************/
public class StorageInput extends AbstractTableActionInput implements Serializable
{
   private String reference;
   private String contentType;



   /*******************************************************************************
    **
    *******************************************************************************/
   public StorageInput(String storageTableName)
   {
      super();
      setTableName(storageTableName);
   }



   /*******************************************************************************
    ** Getter for reference
    *******************************************************************************/
   public String getReference()
   {
      return (this.reference);
   }



   /*******************************************************************************
    ** Setter for reference
    *******************************************************************************/
   public void setReference(String reference)
   {
      this.reference = reference;
   }



   /*******************************************************************************
    ** Fluent setter for reference
    *******************************************************************************/
   public StorageInput withReference(String reference)
   {
      this.reference = reference;
      return (this);
   }



   /*******************************************************************************
    ** Getter for contentType
    *******************************************************************************/
   public String getContentType()
   {
      return (this.contentType);
   }



   /*******************************************************************************
    ** Setter for contentType
    *******************************************************************************/
   public void setContentType(String contentType)
   {
      this.contentType = contentType;
   }



   /*******************************************************************************
    ** Fluent setter for contentType
    *******************************************************************************/
   public StorageInput withContentType(String contentType)
   {
      this.contentType = contentType;
      return (this);
   }

}
