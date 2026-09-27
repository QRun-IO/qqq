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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.middleware.javalin.executors.ProcessStatusExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.ProcessStatusInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.ProcessInitOrStepOrStatusResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.ProcessSpecUtilsV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.Context;


/*******************************************************************************
 **
 *******************************************************************************/
public class ProcessStatusSpecV1 extends AbstractEndpointSpec<ProcessStatusInput, ProcessInitOrStepOrStatusResponseV1, ProcessStatusExecutor>
{

   /***************************************************************************
    **
    ***************************************************************************/
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/processes/{processName}/{processUUID}/status/{jobUUID}")
         .withHttpMethod(HttpMethod.GET)
         .withTag(TagsV1.PROCESSES)
         .withShortSummary("Get job status")
         .withLongDescription("""
            Get the status of a running job for a process.
            
            Response is the same format as for an init or step call that completed synchronously.
            """
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
            .withName("processName")
            .withDescription("Name of the process that is being ran")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("samplePersonProcess")
            .withIn(In.PATH),

         new Parameter()
            .withName("processUUID")
            .withDescription("Unique identifier for this run of the process - as was returned by the `init` call.")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING).withFormat("uuid"))
            .withExample(ProcessSpecUtilsV1.EXAMPLE_PROCESS_UUID)
            .withIn(In.PATH),

         new Parameter()
            .withName("jobUUID")
            .withDescription("Unique identifier for the asynchronous job being executed, as returned by an `init` or `step` call that went asynch.")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING).withFormat("uuid"))
            .withExample(ProcessSpecUtilsV1.EXAMPLE_JOB_UUID)
            .withIn(In.PATH),

         ProcessSpecUtilsV1.defineTableVariantQueryParameter()
      );
   }



   /***************************************************************************
    ** These aren't in the components sub-package, so they don't get auto-found.
    ***************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return Map.of(
         ProcessSpecUtilsV1.getResponseSchemaRefName(), new ProcessInitOrStepOrStatusResponseV1().toSchema(),
         "ProcessStepComplete", new ProcessInitOrStepOrStatusResponseV1.ProcessStepComplete().toSchema(),
         "ProcessStepJobStarted", new ProcessInitOrStepOrStatusResponseV1.ProcessStepJobStarted().toSchema(),
         "ProcessStepRunning", new ProcessInitOrStepOrStatusResponseV1.ProcessStepRunning().toSchema(),
         "ProcessStepError", new ProcessInitOrStepOrStatusResponseV1.ProcessStepError().toSchema()
      );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public ProcessStatusInput buildInput(Context context) throws Exception
   {
      ProcessStatusInput input = new ProcessStatusInput();
      input.setProcessName(getRequestParam(context, "processName"));
      input.setProcessUUID(getRequestParam(context, "processUUID"));
      input.setJobUUID(getRequestParam(context, "jobUUID"));
      input.setTableVariant(ProcessSpecUtilsV1.getTableVariantParam(context));
      return (input);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      return new BasicResponse("""
         State of the backend's running of the specified job, with different fields set, 
         based on the status of the job.""",
         // new ProcessInitOrStepOrStatusResponseV1().toSchema(),

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
