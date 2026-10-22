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


package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.middleware.javalin.executors.UploadsExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.UploadsInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.UploadRefResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.Content;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.RequestBody;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;


/*******************************************************************************
 ** POST /uploads: stream a file into the uploads storage table and get an
 ** opaque UploadRef (spec section 3.2) to pass to a job launch (uploadRefs) or
 ** to the bulk-load operations. Send multipart form data with one file field,
 ** or the raw file as the body with a fileName query parameter (streamed, never
 ** buffered whole).
 *******************************************************************************/
public class UploadsSpecV1 extends AbstractEndpointSpec<UploadsInput, UploadRefResponseV1, UploadsExecutor>
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/uploads")
         .withHttpMethod(HttpMethod.POST)
         .withTag(TagsV1.PROCESSES)
         .withShortSummary("Upload a file for a job or a bulk load")
         .withLongDescription("""
            Streams a file into the application's uploads storage and returns an opaque, expiring ref bound to the caller. \
            Pass the ref in uploadRefs when launching a job (POST /processes/{processName}/jobs). Send multipart form data with \
            one file field, or the raw file as the request body with the fileName query parameter.""");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public List<Parameter> defineRequestParameters()
   {
      return List.of(new Parameter()
         .withName("fileName")
         .withDescription("The file's name, when the body is the raw file (not multipart).")
         .withRequired(false)
         .withSchema(new Schema().withType(Type.STRING))
         .withExample("people.csv")
         .withIn(In.QUERY));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public RequestBody defineRequestBody()
   {
      Map<String, Content> content = new LinkedHashMap<>();
      content.put(ContentType.MULTIPART_FORM_DATA.getMimeType(), new Content().withSchema(new Schema()
         .withType(Type.OBJECT)
         .withDescription("One file field.")
         .withProperties(new LinkedHashMap<>(Map.of("file", new Schema().withType(Type.STRING).withFormat("binary").withDescription("The file."))))));
      content.put(ContentType.APPLICATION_OCTET_STREAM.getMimeType(), new Content().withSchema(new Schema()
         .withType(Type.STRING)
         .withFormat("binary")
         .withDescription("The raw file (give its name in the fileName query parameter).")));
      return new RequestBody().withContent(content);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public UploadsInput buildInput(Context context) throws Exception
   {
      UploadsInput input = new UploadsInput();
      if(context.isMultipartFormData())
      {
         List<UploadedFile> files = context.uploadedFiles();
         if(files.size() != 1)
         {
            throw (new QBadRequestException("Send exactly one file."));
         }
         UploadedFile file = files.get(0);
         input.setFileName(file.filename());
         input.setContentType(file.contentType());
         input.setContentSupplier(file::content);
      }
      else
      {
         String fileName = context.queryParam("fileName");
         if(!StringUtils.hasContent(fileName))
         {
            throw (new QBadRequestException("Give the file's name in the fileName query parameter, or send multipart form data."));
         }
         input.setFileName(fileName);
         input.setContentType(context.contentType());
         input.setContentSupplier(context::bodyInputStream);
      }
      return (input);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return Map.of(UploadRefResponseV1.class.getSimpleName(), new UploadRefResponseV1().toSchema());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      return new BasicResponse("""
         The upload's ref, name, size, content type and expiry.""",
         UploadRefResponseV1.class.getSimpleName());
   }

}
