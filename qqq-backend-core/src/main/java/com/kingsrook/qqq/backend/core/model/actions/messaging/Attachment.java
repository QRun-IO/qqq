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

package com.kingsrook.qqq.backend.core.model.actions.messaging;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.Callable;


/*******************************************************************************
 **
 *******************************************************************************/
public class Attachment
{
   private byte[] contents;
   private String name;
   private Callable<InputStream> streamSupplier;
   private String contentType = "application/octet-stream";



   /*******************************************************************************
    ** Getter for contents
    *******************************************************************************/
   public byte[] getContents()
   {
      return (this.contents);
   }



   /*******************************************************************************
    ** Setter for contents
    *******************************************************************************/
   public void setContents(byte[] contents)
   {
      this.contents = contents;
   }



   /*******************************************************************************
    ** Fluent setter for contents
    *******************************************************************************/
   public Attachment withContents(byte[] contents)
   {
      this.contents = contents;
      return (this);
   }



   /*******************************************************************************
    ** Getter for name
    *******************************************************************************/
   public String getName()
   {
      return (this.name);
   }



   /*******************************************************************************
    ** Setter for name
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Fluent setter for name
    *******************************************************************************/
   public Attachment withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Stream-backed attachment; each invocation opens a fresh stream owned by the caller.
    *******************************************************************************/
   public Attachment withStreamSupplier(Callable<InputStream> streamSupplier)
   {
      this.streamSupplier = streamSupplier;
      return (this);
   }



   /*******************************************************************************
    ** Whether this attachment requires streaming support from its provider.
    *******************************************************************************/
   public boolean isStreamBacked()
   {
      return (streamSupplier != null);
   }



   /*******************************************************************************
    ** Open the attachment without buffering storage output into memory.
    *******************************************************************************/
   public InputStream openStream() throws Exception
   {
      return (streamSupplier == null ? new ByteArrayInputStream(contents) : streamSupplier.call());
   }


   /*******************************************************************************
    ** MIME type sent to the messaging transport.
    *******************************************************************************/
   public String getContentType()
   {
      return (contentType);
   }



   /*******************************************************************************
    ** Configure the MIME type, defaulting unknown types to binary.
    *******************************************************************************/
   public Attachment withContentType(String contentType)
   {
      this.contentType = contentType == null ? "application/octet-stream" : contentType;
      return (this);
   }
}
