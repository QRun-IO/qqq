/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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
import com.kingsrook.qqq.middleware.javalin.executors.ProcessCancelExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.ProcessCancelInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.ProcessCancelResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.ProcessSpecUtilsV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.Context;


/*******************************************************************************
 ** Endpoint spec for cancelling a running process.
 *******************************************************************************/
public class ProcessCancelSpecV1 extends AbstractEndpointSpec<ProcessCancelInput, ProcessCancelResponseV1, ProcessCancelExecutor>
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/processes/{processName}/{processUUID}/cancel")
         .withHttpMethod(HttpMethod.POST)
         .withTag(TagsV1.PROCESSES)
         .withShortSummary("Cancel a running process")
         .withLongDescription("""
            Cancel a running process. If the process defines a cancel step, it will be executed.""");
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
            .withDescription("Name of the process to cancel")
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
            .withIn(In.PATH)
      );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public ProcessCancelInput buildInput(Context context) throws Exception
   {
      ProcessCancelInput input = new ProcessCancelInput();
      input.setProcessName(getRequestParam(context, "processName"));
      input.setProcessUUID(getRequestParam(context, "processUUID"));
      return (input);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      return new BasicResponse("""
         Empty JSON object indicating successful cancellation.""",
         ProcessCancelResponseV1.class.getSimpleName()
      );
   }



   /***************************************************************************
    ** Override handleOutput to return a simple empty JSON object.
    ***************************************************************************/
   @Override
   public void handleOutput(Context context, ProcessCancelResponseV1 output) throws Exception
   {
      context.result("{}");
   }

}
