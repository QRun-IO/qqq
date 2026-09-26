/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
