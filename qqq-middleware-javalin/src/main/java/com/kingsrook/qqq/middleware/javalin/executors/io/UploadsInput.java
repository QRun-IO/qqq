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


package com.kingsrook.qqq.middleware.javalin.executors.io;


import java.io.InputStream;


/*******************************************************************************
 ** Input for POST /uploads: the file's name, content type, and a supplier of
 ** its bytes (read once, streamed).
 *******************************************************************************/
public class UploadsInput extends AbstractMiddlewareInput
{
   private String          fileName;
   private String          contentType;
   private ContentSupplier contentSupplier;



   /*******************************************************************************
    ** Opens the request's file content.
    *******************************************************************************/
   @FunctionalInterface
   public interface ContentSupplier
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      InputStream open() throws Exception;
   }



   /*******************************************************************************
    ** Getter for fileName
    *******************************************************************************/
   public String getFileName()
   {
      return (this.fileName);
   }



   /*******************************************************************************
    ** Setter for fileName
    *******************************************************************************/
   public void setFileName(String fileName)
   {
      this.fileName = fileName;
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
    ** Getter for contentSupplier
    *******************************************************************************/
   public ContentSupplier getContentSupplier()
   {
      return (this.contentSupplier);
   }



   /*******************************************************************************
    ** Setter for contentSupplier
    *******************************************************************************/
   public void setContentSupplier(ContentSupplier contentSupplier)
   {
      this.contentSupplier = contentSupplier;
   }

}
