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


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.executors.ProcessMetaDataExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.ProcessMetaDataInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.ProcessMetaDataResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.Example;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.Context;


/*******************************************************************************
 **
 *******************************************************************************/
public class ProcessMetaDataSpecV1 extends AbstractEndpointSpec<ProcessMetaDataInput, ProcessMetaDataResponseV1, ProcessMetaDataExecutor>
{

   /***************************************************************************
    **
    ***************************************************************************/
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/metaData/process/{processName}")
         .withHttpMethod(HttpMethod.GET)
         .withTag(TagsV1.PROCESSES)
         .withShortSummary("Get process metaData")
         .withLongDescription("""
            Load the full metadata for a single process, including all screens (aka, frontend steps), which a frontend
            needs to display to users."""
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
            .withDescription("Name of the process to load.")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("samplePersonProcess")
            .withIn(In.PATH)
      );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public ProcessMetaDataInput buildInput(Context context) throws Exception
   {
      ProcessMetaDataInput input = new ProcessMetaDataInput();
      input.setProcessName(getRequestParam(context, "processName"));
      return (input);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return Map.of(ProcessMetaDataResponseV1.class.getSimpleName(), new ProcessMetaDataResponseV1().toSchema());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      Map<String, Example> examples = new LinkedHashMap<>();
      examples.put("TODO", new Example()
         .withValue(new ProcessMetaDataResponseV1())); // todo do

      return new BasicResponse("""
         The full process metadata""",
         ProcessMetaDataResponseV1.class.getSimpleName(),
         examples
      );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void handleOutput(Context context, ProcessMetaDataResponseV1 output) throws Exception
   {
      context.result(JsonUtils.toJson(output.getProcessMetaData()));
   }

}
