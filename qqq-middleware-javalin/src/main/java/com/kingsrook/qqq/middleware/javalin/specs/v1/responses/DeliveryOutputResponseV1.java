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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses;


import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.delivery.DeliveryAttachment;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.DeliveryOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.Context;


/*******************************************************************************
 ** DeliveryOutputResponseV1.
 *******************************************************************************/
public class DeliveryOutputResponseV1 implements DeliveryOutputInterface, ToSchema
{
   private List<DeliveryAttachment> attachments;
   private String textOutput;



   /*******************************************************************************
    ** Binary output; multiple files are streamed as a ZIP archive.
    *******************************************************************************/
   @Override
   public Schema toSchema()
   {
      return (new Schema().withType(Type.STRING).withFormat("binary"));
   }



   /*******************************************************************************
    ** Accept only references authorized by the executor.
    *******************************************************************************/
   @Override
   public void setAttachments(List<DeliveryAttachment> attachments)
   {
      this.attachments = attachments;
   }



   /*******************************************************************************
    ** Stream storage directly to the response; never stage files or buffer payloads.
    *******************************************************************************/
   public void write(Context context) throws Exception
   {
      context.header("Cache-Control", "private, no-store");
      context.header("X-Content-Type-Options", "nosniff");
      context.contentType("application/octet-stream");
      context.header("Content-Disposition", "attachment; filename=output" + (attachments.size() > 1 ? ".zip" : ""));
      if(attachments.isEmpty())
      {
         context.result(textOutput == null ? "" : textOutput);
      }
      else if(attachments.size() == 1)
      {
         DeliveryAttachment attachment = attachments.get(0);
         try(InputStream stream = new StorageAction().getInputStream(new StorageInput(attachment.getStorageTableName()).withReference(attachment.getStorageReference())))
         {
            stream.transferTo(context.res().getOutputStream());
         }
      }
      else
      {
         ZipOutputStream zip = new ZipOutputStream(context.res().getOutputStream());
         int index = 0;
         for(DeliveryAttachment attachment : attachments)
         {
            String name = attachment.getFileName() == null ? "output" : attachment.getFileName().replaceAll("[^a-zA-Z0-9._-]", "_");
            zip.putNextEntry(new ZipEntry(++index + "-" + name));
            try(InputStream stream = new StorageAction().getInputStream(new StorageInput(attachment.getStorageTableName()).withReference(attachment.getStorageReference())))
            {
               stream.transferTo(zip);
            }
            zip.closeEntry();
         }
         zip.finish();
      }
   }


   /*******************************************************************************
    ** Retain text already authorized by the executor.
    *******************************************************************************/
   @Override
   public void setTextOutput(String text)
   {
      textOutput = text;
   }
}
