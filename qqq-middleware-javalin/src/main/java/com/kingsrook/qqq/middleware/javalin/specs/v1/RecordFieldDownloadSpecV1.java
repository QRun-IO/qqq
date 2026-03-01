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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.middleware.javalin.executors.RecordFieldDownloadExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.RecordFieldDownloadInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.RecordFieldDownloadResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.Content;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Response;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.Context;


/*******************************************************************************
 ** Spec for the record field download endpoint
 ** (GET /table/{tableName}/{primaryKey}/{fieldName}/{filename}).
 ** This endpoint returns binary content for a blob field on a record.
 *******************************************************************************/
public class RecordFieldDownloadSpecV1 extends AbstractEndpointSpec<RecordFieldDownloadInput, RecordFieldDownloadResponseV1, RecordFieldDownloadExecutor>
{

   /***************************************************************************
    **
    ***************************************************************************/
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/table/{tableName}/{primaryKey}/{fieldName}/{filename}")
         .withHttpMethod(HttpMethod.GET)
         .withTag(TagsV1.TABLES)
         .withShortSummary("Download a field value from a record")
         .withLongDescription("""
            Download the binary content of a blob field from a specific record."""
         );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public List<Parameter> defineRequestParameters()
   {
      return List.of(
         new Parameter()
            .withName("tableName")
            .withDescription("Name of the table containing the record.")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("person")
            .withIn(In.PATH),
         new Parameter()
            .withName("primaryKey")
            .withDescription("Primary key value of the record.")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("42")
            .withIn(In.PATH),
         new Parameter()
            .withName("fieldName")
            .withDescription("Name of the field to download.")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("photo")
            .withIn(In.PATH),
         new Parameter()
            .withName("filename")
            .withDescription("Filename for the downloaded content.")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("photo.jpg")
            .withIn(In.PATH)
      );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public RecordFieldDownloadInput buildInput(Context context) throws Exception
   {
      RecordFieldDownloadInput input = new RecordFieldDownloadInput();
      input.setTableName(getRequestParam(context, "tableName"));
      input.setPrimaryKey(getRequestParam(context, "primaryKey"));
      input.setFieldName(getRequestParam(context, "fieldName"));
      input.setFilename(getRequestParam(context, "filename"));
      return (input);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return Map.of();
   }



   /***************************************************************************
    ** Binary download endpoints do not return a JSON schema. Override to
    ** describe the response as application/octet-stream binary content.
    ***************************************************************************/
   @Override
   public Map<Integer, Response> defineResponses()
   {
      return Map.of(200, new Response()
         .withDescription("Binary file content")
         .withContent(Map.of("application/octet-stream", new Content()
            .withSchema(new Schema().withType(Type.STRING).withFormat("binary")))));
   }



   /***************************************************************************
    ** Override handleOutput to write binary content or issue a redirect
    ** rather than JSON.
    ***************************************************************************/
   @Override
   public void handleOutput(Context context, RecordFieldDownloadResponseV1 output) throws Exception
   {
      /////////////////////////////////////////////////////////////////////
      // for non-blob fields, the value is a URL -- issue an HTTP redirect //
      /////////////////////////////////////////////////////////////////////
      if(StringUtils.hasContent(output.getRedirectUrl()))
      {
         context.redirect(output.getRedirectUrl());
         return;
      }

      if(StringUtils.hasContent(output.getContentType()))
      {
         context.contentType(output.getContentType());
      }

      if(StringUtils.hasContent(output.getFilename()))
      {
         context.header("Content-Disposition", "attachment; filename=\"" + output.getFilename() + "\"");
      }

      if(output.getBytes() != null)
      {
         context.result(output.getBytes());
      }
   }

}
