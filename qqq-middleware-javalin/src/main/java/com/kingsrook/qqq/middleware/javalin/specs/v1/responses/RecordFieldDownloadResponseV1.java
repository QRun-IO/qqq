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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses;


import com.kingsrook.qqq.middleware.javalin.executors.io.RecordFieldDownloadOutputInterface;


/*******************************************************************************
 ** Response wrapper for the record field download endpoint. Carries the binary
 ** content and metadata needed to produce the HTTP response.
 *******************************************************************************/
public class RecordFieldDownloadResponseV1 implements RecordFieldDownloadOutputInterface
{
   private byte[] bytes;
   private String contentType;
   private String filename;
   private String redirectUrl;



   /*******************************************************************************
    ** Setter for bytes
    *******************************************************************************/
   @Override
   public void setBytes(byte[] bytes)
   {
      this.bytes = bytes;
   }



   /*******************************************************************************
    ** Setter for contentType
    *******************************************************************************/
   @Override
   public void setContentType(String contentType)
   {
      this.contentType = contentType;
   }



   /*******************************************************************************
    ** Setter for filename
    *******************************************************************************/
   @Override
   public void setFilename(String filename)
   {
      this.filename = filename;
   }



   /*******************************************************************************
    ** Getter for bytes
    *******************************************************************************/
   public byte[] getBytes()
   {
      return (this.bytes);
   }



   /*******************************************************************************
    ** Getter for contentType
    *******************************************************************************/
   public String getContentType()
   {
      return (this.contentType);
   }



   /*******************************************************************************
    ** Getter for filename
    *******************************************************************************/
   public String getFilename()
   {
      return (this.filename);
   }



   /*******************************************************************************
    ** Setter for redirectUrl
    *******************************************************************************/
   @Override
   public void setRedirectUrl(String url)
   {
      this.redirectUrl = url;
   }



   /*******************************************************************************
    ** Getter for redirectUrl
    *******************************************************************************/
   public String getRedirectUrl()
   {
      return (this.redirectUrl);
   }

}
