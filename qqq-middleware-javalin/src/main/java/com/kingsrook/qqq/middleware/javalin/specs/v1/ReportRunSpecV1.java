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


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.middleware.javalin.executors.ReportRunExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.ReportRunInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.TableExportResponseV1;
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
 ** Spec for running a report and downloading the generated file
 ** (GET /reports/{reportName}) - the v1 form of the legacy streaming report
 ** route, for reports that are not run through a process.  Returns the file
 ** (CSV, XLSX, JSON, ...) rather than a JSON response body.
 *******************************************************************************/
public class ReportRunSpecV1 extends AbstractEndpointSpec<ReportRunInput, TableExportResponseV1, ReportRunExecutor>
{

   /***************************************************************************
    **
    ***************************************************************************/
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/reports/{reportName}")
         .withHttpMethod(HttpMethod.GET)
         .withTag(TagsV1.REPORTS)
         .withShortSummary("Run a report and download the file")
         .withLongDescription("""
            Run a report and stream the generated file in the requested format.  The report's input fields are given as
            query parameters named for the fields; a missing required input is a 400.  Requires permission to run the report."""
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
            .withName("reportName")
            .withDescription("Name of the report to run.")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("personReport")
            .withIn(In.PATH),
         new Parameter()
            .withName("format")
            .withDescription("Output format: csv, xlsx, json (as for table export).")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("csv")
            .withIn(In.QUERY)
      );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public ReportRunInput buildInput(Context context) throws Exception
   {
      Map<String, String> inputValues = new LinkedHashMap<>();
      context.queryParamMap().forEach((name, values) ->
      {
         if(!"format".equals(name) && values != null && !values.isEmpty())
         {
            inputValues.put(name, values.get(0));
         }
      });

      return (new ReportRunInput()
         .withReportName(getRequestParam(context, "reportName"))
         .withFormat(getRequestParam(context, "format"))
         .withInputValues(inputValues));
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
         .withDescription("The generated report file, in the requested format")
         .withContent(Map.of("application/octet-stream", new Content()
            .withSchema(new Schema().withType(Type.STRING).withFormat("binary")))));
   }



   /***************************************************************************
    ** Stream the file rather than a JSON body.
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
         String filename = output.getFilename().replaceAll("[\"\\\\\\p{Cntrl}]", "_");
         context.header("Content-Disposition", "attachment; filename=\"" + filename + "\"");
      }

      if(output.getInputStream() != null)
      {
         context.result(output.getInputStream());
      }
   }

}
