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
import com.kingsrook.qqq.middleware.javalin.executors.GeneralDownloadExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.GeneralDownloadInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.GeneralDownloadResponseV1;
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
 ** Spec for the general file download endpoint (GET /download/{file}).
 ** This endpoint returns binary file content from server-side storage.
 *******************************************************************************/
public class GeneralDownloadSpecV1 extends AbstractEndpointSpec<GeneralDownloadInput, GeneralDownloadResponseV1, GeneralDownloadExecutor>
{

   /***************************************************************************
    **
    ***************************************************************************/
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/download/{file}")
         .withHttpMethod(HttpMethod.GET)
         .withTag(TagsV1.GENERAL)
         .withShortSummary("Download a file")
         .withLongDescription("""
            Download a file from server-side storage by file path or storage table reference."""
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
            .withName("file")
            .withDescription("Filename for the download.")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("report.csv")
            .withIn(In.PATH),
         new Parameter()
            .withName("filePath")
            .withDescription("Server-side file path to download from.")
            .withRequired(false)
            .withSchema(new Schema().withType(Type.STRING))
            .withIn(In.QUERY),
         new Parameter()
            .withName("storageTableName")
            .withDescription("Name of the storage table to download from.")
            .withRequired(false)
            .withSchema(new Schema().withType(Type.STRING))
            .withIn(In.QUERY),
         new Parameter()
            .withName("storageReference")
            .withDescription("Storage reference key for the file.")
            .withRequired(false)
            .withSchema(new Schema().withType(Type.STRING))
            .withIn(In.QUERY)
      );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public GeneralDownloadInput buildInput(Context context) throws Exception
   {
      GeneralDownloadInput input = new GeneralDownloadInput();
      input.setFile(getRequestParam(context, "file"));
      input.setFilePath(getRequestParam(context, "filePath"));
      input.setStorageTableName(getRequestParam(context, "storageTableName"));
      input.setStorageReference(getRequestParam(context, "storageReference"));
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
    ** Override handleOutput to stream binary content rather than JSON.
    ***************************************************************************/
   @Override
   public void handleOutput(Context context, GeneralDownloadResponseV1 output) throws Exception
   {
      if(StringUtils.hasContent(output.getContentType()))
      {
         context.contentType(output.getContentType());
      }

      if(StringUtils.hasContent(output.getFilename()))
      {
         context.header("Content-Disposition", "attachment; filename=\"" + output.getFilename() + "\"");
      }

      if(output.getInputStream() != null)
      {
         context.result(output.getInputStream());
      }
   }

}
