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


import com.kingsrook.qqq.backend.core.jobs.uploads.UploadRef;
import com.kingsrook.qqq.middleware.javalin.executors.io.UploadsOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;


/*******************************************************************************
 ** An UploadRef (spec section 3.2).
 *******************************************************************************/
public class UploadRefResponseV1 implements UploadsOutputInterface, ToSchema
{
   @OpenAPIDescription("Opaque ref to the uploaded file, bound to the uploading user. Pass it in uploadRefs.")
   private String ref;

   @OpenAPIDescription("The file's name.")
   private String fileName;

   @OpenAPIDescription("The file's size in bytes.")
   private Long sizeBytes;

   @OpenAPIDescription("The file's content type, as uploaded.")
   private String contentType;

   @OpenAPIDescription("When the ref stops working (ISO-8601). A job that uses it extends it.")
   private String expiresAt;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void setUploadRef(UploadRef uploadRef)
   {
      this.ref = uploadRef.ref();
      this.fileName = uploadRef.fileName();
      this.sizeBytes = uploadRef.sizeBytes();
      this.contentType = uploadRef.contentType();
      this.expiresAt = uploadRef.expiresAt() == null ? null : uploadRef.expiresAt().toString();
   }



   /*******************************************************************************
    ** Getter for ref
    *******************************************************************************/
   public String getRef()
   {
      return (this.ref);
   }



   /*******************************************************************************
    ** Getter for fileName
    *******************************************************************************/
   public String getFileName()
   {
      return (this.fileName);
   }



   /*******************************************************************************
    ** Getter for sizeBytes
    *******************************************************************************/
   public Long getSizeBytes()
   {
      return (this.sizeBytes);
   }



   /*******************************************************************************
    ** Getter for contentType
    *******************************************************************************/
   public String getContentType()
   {
      return (this.contentType);
   }



   /*******************************************************************************
    ** Getter for expiresAt
    *******************************************************************************/
   public String getExpiresAt()
   {
      return (this.expiresAt);
   }

}
