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

package com.kingsrook.qqq.middleware.javalin.executors.io;


/*******************************************************************************
 ** Input for the general file download endpoint.
 *******************************************************************************/
public class GeneralDownloadInput extends AbstractMiddlewareInput
{
   private String file;
   private String filePath;
   private String storageTableName;
   private String storageReference;



   /*******************************************************************************
    ** Getter for file
    *******************************************************************************/
   public String getFile()
   {
      return (this.file);
   }



   /*******************************************************************************
    ** Setter for file
    *******************************************************************************/
   public void setFile(String file)
   {
      this.file = file;
   }



   /*******************************************************************************
    ** Fluent setter for file
    *******************************************************************************/
   public GeneralDownloadInput withFile(String file)
   {
      this.file = file;
      return (this);
   }



   /*******************************************************************************
    ** Getter for filePath
    *******************************************************************************/
   public String getFilePath()
   {
      return (this.filePath);
   }



   /*******************************************************************************
    ** Setter for filePath
    *******************************************************************************/
   public void setFilePath(String filePath)
   {
      this.filePath = filePath;
   }



   /*******************************************************************************
    ** Fluent setter for filePath
    *******************************************************************************/
   public GeneralDownloadInput withFilePath(String filePath)
   {
      this.filePath = filePath;
      return (this);
   }



   /*******************************************************************************
    ** Getter for storageTableName
    *******************************************************************************/
   public String getStorageTableName()
   {
      return (this.storageTableName);
   }



   /*******************************************************************************
    ** Setter for storageTableName
    *******************************************************************************/
   public void setStorageTableName(String storageTableName)
   {
      this.storageTableName = storageTableName;
   }



   /*******************************************************************************
    ** Fluent setter for storageTableName
    *******************************************************************************/
   public GeneralDownloadInput withStorageTableName(String storageTableName)
   {
      this.storageTableName = storageTableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for storageReference
    *******************************************************************************/
   public String getStorageReference()
   {
      return (this.storageReference);
   }



   /*******************************************************************************
    ** Setter for storageReference
    *******************************************************************************/
   public void setStorageReference(String storageReference)
   {
      this.storageReference = storageReference;
   }



   /*******************************************************************************
    ** Fluent setter for storageReference
    *******************************************************************************/
   public GeneralDownloadInput withStorageReference(String storageReference)
   {
      this.storageReference = storageReference;
      return (this);
   }

}
