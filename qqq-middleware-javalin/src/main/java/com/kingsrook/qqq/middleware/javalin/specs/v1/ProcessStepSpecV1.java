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
import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.middleware.javalin.executors.ProcessInitOrStepExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.ProcessInitOrStepInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.ProcessInitOrStepOrStatusResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.ProcessSpecUtilsV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.Content;
import com.kingsrook.qqq.openapi.model.Example;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.RequestBody;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.ContentType;
import io.javalin.http.Context;


/*******************************************************************************
 **
 *******************************************************************************/
public class ProcessStepSpecV1 extends AbstractEndpointSpec<ProcessInitOrStepInput, ProcessInitOrStepOrStatusResponseV1, ProcessInitOrStepExecutor>
{
   public static int DEFAULT_ASYNC_STEP_TIMEOUT_MILLIS = 3_000;



   /***************************************************************************
    **
    ***************************************************************************/
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/processes/{processName}/{processUUID}/step/{stepName}")
         .withHttpMethod(HttpMethod.POST)
         .withTag(TagsV1.PROCESSES)
         .withShortSummary("Run a step in a process")
         .withLongDescription("""
            To run the next step in a process, this endpoint should be called, with the `processName`
            and existing `processUUID`, as well as the step that was just completed in the frontend,
            given as `stepName`.
            
            Additional process-specific values should posted in a form param named `values`, as JSON object
            with keys defined by the process in question.
            
            Note that this request, if it takes longer than a given threshold* to complete, will return a
            a `jobUUID`, which should be sent to the `/processes/{processName}/{processUUID}/status/{jobUUID}`
            endpoint, to poll for a status update.
            
            *This threshold has a default value of 3,000 ms., but can be set per-request via the form
            parameter `stepTimeoutMillis`.
            """);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public List<Parameter> defineRequestParameters()
   {
      return List.of(

         new Parameter()
            .withName("processName")
            .withDescription("Name of the process to perform the step in.")
            .withRequired(true)
            .withExample("samplePersonProcess")
            .withSchema(new Schema().withType(Type.STRING))
            .withIn(In.PATH),

         new Parameter()
            .withName("processUUID")
            .withDescription("Unique identifier for this run of the process - as was returned by the `init` call.")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample(ProcessSpecUtilsV1.EXAMPLE_PROCESS_UUID)
            .withIn(In.PATH),

         new Parameter()
            .withName("stepName")
            .withDescription("Name of the frontend step that the user has just completed.")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("inputForm")
            .withIn(In.PATH),

         ProcessSpecUtilsV1.defineTableVariantQueryParameter()
      );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public RequestBody defineRequestBody()
   {
      return new RequestBody()
         .withContent(ContentType.MULTIPART_FORM_DATA.getMimeType(), new Content()
            .withSchema(new Schema()
               .withType(Type.OBJECT)
               .withProperty("values", new Schema()
                  .withType(Type.OBJECT)
                  .withDescription("Process-specific field names and values."))

               .withProperty("stepTimeoutMillis", new Schema()
                  .withDescription("Optionally change the time that the server will wait for the job before letting it go asynchronous.  Default value is 3000.")
                  .withType(Type.INTEGER)
                  .withExample("shorter timeout", new Example().withValue("500"))
                  .withExample("longer timeout", new Example().withValue("60000")))

               .withProperty("file", new Schema()
                  .withType(Type.STRING)
                  .withFormat("binary")
                  .withDescription("A file upload, for process steps which expect an uploaded file."))

               .withProperty(ProcessSpecUtilsV1.TABLE_VARIANT_PARAM, ProcessSpecUtilsV1.defineTableVariantFormProperty())
            )
         );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public ProcessInitOrStepInput buildInput(Context context) throws Exception
   {
      ProcessInitOrStepInput processInitOrStepInput = new ProcessInitOrStepInput();
      processInitOrStepInput.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.BREAK);

      processInitOrStepInput.setProcessName(getRequestParam(context, "processName"));
      processInitOrStepInput.setProcessUUID(getRequestParam(context, "processUUID"));
      ///////////////////////////////////////////////////////////////////////////////////
      // with isStepBack=true, the named step is the process's back step: restart at it //
      ///////////////////////////////////////////////////////////////////////////////////
      String stepName = getRequestParam(context, "stepName");
      if("true".equalsIgnoreCase(context.queryParam("isStepBack")))
      {
         processInitOrStepInput.setStartAtStep(stepName);
      }
      else
      {
         processInitOrStepInput.setStartAfterStep(stepName);
      }
      processInitOrStepInput.setStepTimeoutMillis(Objects.requireNonNullElse(getRequestParamInteger(context, "stepTimeoutMillis"), DEFAULT_ASYNC_STEP_TIMEOUT_MILLIS));
      processInitOrStepInput.setValues(getRequestParamMap(context, "values"));
      processInitOrStepInput.setTableVariant(ProcessSpecUtilsV1.getTableVariantParam(context));

      /////////////////////////////////////////////////////////////////////////
      // uploaded files are stored and referenced from their process values //
      /////////////////////////////////////////////////////////////////////////
      if(processInitOrStepInput.getValues() == null)
      {
         processInitOrStepInput.setValues(new LinkedHashMap<>());
      }
      ProcessSpecUtilsV1.addUploadedFiles(context, processInitOrStepInput);

      return (processInitOrStepInput);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      return new BasicResponse("""
         State of the backend's running of the next step(s) of the job, with different fields set,
         based on the status of the job.""",

         ProcessSpecUtilsV1.getResponseSchemaRefName(),
         ProcessSpecUtilsV1.buildResponseExample()
      );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void handleOutput(Context context, ProcessInitOrStepOrStatusResponseV1 output) throws Exception
   {
      ProcessSpecUtilsV1.handleOutput(context, output);
   }

}
