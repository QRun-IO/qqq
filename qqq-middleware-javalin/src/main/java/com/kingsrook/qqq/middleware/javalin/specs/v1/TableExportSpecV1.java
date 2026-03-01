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


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.middleware.javalin.executors.TableExportExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableExportInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.TableExportResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.Content;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.RequestBody;
import com.kingsrook.qqq.openapi.model.Response;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import org.json.JSONArray;
import org.json.JSONObject;


/*******************************************************************************
 ** Spec for the table export endpoint (POST /table/{tableName}/export).
 ** This endpoint returns a binary stream (CSV, XLSX, JSON, etc.) rather than
 ** a JSON response body.
 *******************************************************************************/
public class TableExportSpecV1 extends AbstractEndpointSpec<TableExportInput, TableExportResponseV1, TableExportExecutor>
{

   /***************************************************************************
    **
    ***************************************************************************/
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/table/{tableName}/export")
         .withHttpMethod(HttpMethod.POST)
         .withTag(TagsV1.TABLES)
         .withShortSummary("Export records from a table")
         .withLongDescription("""
            Export records from a table as a downloadable file in the specified format (CSV, XLSX, JSON, TSV)."""
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
            .withDescription("Name of the table to export.")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("person")
            .withIn(In.PATH)
      );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public RequestBody defineRequestBody()
   {
      Map<String, Schema> properties = new LinkedHashMap<>();
      properties.put("format", new Schema()
         .withDescription("Export format: csv, xlsx, json, tsv")
         .withType(Type.STRING));
      properties.put("filename", new Schema()
         .withDescription("Optional filename for the download. Generated if absent.")
         .withType(Type.STRING));
      properties.put("filter", new Schema()
         .withDescription("QueryFilter to specify matching records to export")
         .withRef("#/components/schemas/QueryFilter"));
      properties.put("fieldNames", new Schema()
         .withDescription("Optional subset of field names to include in the export")
         .withType(Type.ARRAY)
         .withItems(new Schema().withType(Type.STRING)));
      properties.put("limit", new Schema()
         .withDescription("Optional maximum number of records to export")
         .withType(Type.INTEGER));
      properties.put("includeHeaderRow", new Schema()
         .withDescription("Whether to include a header row (default true)")
         .withType(Type.BOOLEAN));

      return new RequestBody()
         .withContent(Map.of(
            ContentType.APPLICATION_JSON.getMimeType(), new Content()
               .withSchema(new Schema()
                  .withType(Type.OBJECT)
                  .withProperties(properties))
         ));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public TableExportInput buildInput(Context context) throws Exception
   {
      TableExportInput input = new TableExportInput();
      input.setTableName(getRequestParam(context, "tableName"));

      JSONObject requestBody = getRequestBodyAsJsonObject(context);
      if(requestBody != null)
      {
         if(requestBody.has("format"))
         {
            input.setFormat(requestBody.getString("format"));
         }

         if(requestBody.has("filename"))
         {
            input.setFilename(requestBody.getString("filename"));
         }

         if(requestBody.has("filter"))
         {
            Object filterObj = requestBody.get("filter");
            if(filterObj instanceof JSONObject filterJson)
            {
               input.setFilter(JsonUtils.toObject(filterJson.toString(), QQueryFilter.class));
            }
         }

         if(requestBody.has("fieldNames"))
         {
            JSONArray fieldNamesArray = requestBody.getJSONArray("fieldNames");
            List<String> fieldNames = new ArrayList<>();
            for(int i = 0; i < fieldNamesArray.length(); i++)
            {
               fieldNames.add(fieldNamesArray.getString(i));
            }
            input.setFieldNames(fieldNames);
         }

         if(requestBody.has("limit"))
         {
            input.setLimit(requestBody.getInt("limit"));
         }

         if(requestBody.has("includeHeaderRow"))
         {
            input.setIncludeHeaderRow(requestBody.getBoolean("includeHeaderRow"));
         }
      }

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
         .withDescription("Binary file content in the requested export format")
         .withContent(Map.of("application/octet-stream", new Content()
            .withSchema(new Schema().withType(Type.STRING).withFormat("binary")))));
   }



   /***************************************************************************
    ** Override handleOutput to stream binary content rather than JSON.
    ***************************************************************************/
   @Override
   public void handleOutput(Context context, TableExportResponseV1 output) throws Exception
   {
      if(output.getReportFormat() != null && StringUtils.hasContent(output.getReportFormat().getMimeType()))
      {
         context.contentType(output.getReportFormat().getMimeType());
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
