/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.executors.ProcessRecordsExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.ProcessRecordsInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.ProcessRecordsResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.ProcessSpecUtilsV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.Context;


/*******************************************************************************
 ** Endpoint spec for fetching records from a process state.
 *******************************************************************************/
public class ProcessRecordsSpecV1 extends AbstractEndpointSpec<ProcessRecordsInput, ProcessRecordsResponseV1, ProcessRecordsExecutor>
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/processes/{processName}/{processUUID}/records")
         .withHttpMethod(HttpMethod.GET)
         .withTag(TagsV1.PROCESSES)
         .withShortSummary("Get records from a process")
         .withLongDescription("""
            Retrieve the records associated with a running or completed process, with support
            for pagination via skip and limit query parameters.""");
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
            .withDescription("Name of the process")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("samplePersonProcess")
            .withIn(In.PATH),

         new Parameter()
            .withName("processUUID")
            .withDescription("Unique identifier for this run of the process")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING).withFormat("uuid"))
            .withExample(ProcessSpecUtilsV1.EXAMPLE_PROCESS_UUID)
            .withIn(In.PATH),

         new Parameter()
            .withName("skip")
            .withDescription("Number of records to skip (default 0)")
            .withRequired(false)
            .withSchema(new Schema().withType(Type.INTEGER))
            .withExample("0")
            .withIn(In.QUERY),

         new Parameter()
            .withName("limit")
            .withDescription("Maximum number of records to return (default 20)")
            .withRequired(false)
            .withSchema(new Schema().withType(Type.INTEGER))
            .withExample("20")
            .withIn(In.QUERY),

         ProcessSpecUtilsV1.defineTableVariantQueryParameter()
      );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public ProcessRecordsInput buildInput(Context context) throws Exception
   {
      ProcessRecordsInput input = new ProcessRecordsInput();
      input.setProcessName(getRequestParam(context, "processName"));
      input.setProcessUUID(getRequestParam(context, "processUUID"));
      input.setSkip(Objects.requireNonNullElse(getRequestParamInteger(context, "skip"), 0));
      input.setLimit(Objects.requireNonNullElse(getRequestParamInteger(context, "limit"), 20));
      input.setTableVariant(ProcessSpecUtilsV1.getTableVariantParam(context));
      return (input);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return Map.of(ProcessRecordsResponseV1.class.getSimpleName(), new ProcessRecordsResponseV1().toSchema());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      return new BasicResponse("""
         Paginated list of records from the process state.""",
         ProcessRecordsResponseV1.class.getSimpleName()
      );
   }



   /***************************************************************************
    ** Override handleOutput to ensure Include.ALWAYS on empty records list,
    ** following the same pattern as TableQuerySpecV1.
    ***************************************************************************/
   @Override
   public void handleOutput(Context context, ProcessRecordsResponseV1 output) throws Exception
   {
      if(CollectionUtils.nullSafeIsEmpty(output.getRecords()))
      {
         context.result(JsonUtils.toJsonCustomized(output, builder -> builder
            .serializationInclusion(JsonInclude.Include.ALWAYS)));
      }
      else
      {
         super.handleOutput(context, output);
      }
   }

}
