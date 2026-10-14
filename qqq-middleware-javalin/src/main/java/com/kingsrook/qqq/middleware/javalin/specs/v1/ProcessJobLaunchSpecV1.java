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


import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.jobs.model.JobMode;
import com.kingsrook.qqq.backend.core.jobs.model.LaunchRequest;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.middleware.javalin.executors.ProcessJobLaunchExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.ProcessJobLaunchInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.BasicErrorResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.ProcessJobLaunchResponseV1;
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
import io.javalin.http.HttpStatus;


/*******************************************************************************
 ** POST /processes/{processName}/jobs: run any eligible process as a durable
 ** job ("Run in background"). Docs: docs/specs/DURABLE-JOBS-AND-ASSISTED-BULK-LOAD.md.
 *******************************************************************************/
public class ProcessJobLaunchSpecV1 extends AbstractEndpointSpec<ProcessJobLaunchInput, ProcessJobLaunchResponseV1, ProcessJobLaunchExecutor>
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/processes/{processName}/jobs")
         .withHttpMethod(HttpMethod.POST)
         .withTag(TagsV1.PROCESSES)
         .withShortSummary("Run a process as a durable background job")
         .withLongDescription("""
            Launch an eligible process as a durable job: it survives pod restarts, runs in checkpointed chunks under leases, \
            and can be paused, resumed, cancelled, restarted and retried with the job control processes on the jobs_job table. \
            Values for the process's frontend steps are given up front. The job runs with the caller's permissions (never more). \
            A launch from the assistant client channel waits for confirmation (status AWAITING_CONFIRMATION). \
            Progress arrives as job.progress and job.status events on the realtime channel record:jobs_job:{jobId}.""");
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
            .withDescription("Name of the process to run as a job")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("person.bulkEdit")
            .withIn(In.PATH));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public RequestBody defineRequestBody()
   {
      Map<String, Schema> properties = new LinkedHashMap<>();
      properties.put("values", new Schema().withType(Type.OBJECT).withDescription("Process values (what the frontend steps would have collected)."));
      properties.put("filter", new Schema().withType(Type.OBJECT).withDescription("A records filter (QQueryFilter JSON) the process runs over."));
      properties.put("recordIds", new Schema().withType(Type.ARRAY).withItems(new Schema().withType(Type.STRING)).withDescription("Primary keys the process runs over."));
      properties.put("uploadRefs", new Schema().withType(Type.ARRAY).withItems(new Schema().withType(Type.STRING)).withDescription("Upload refs from POST /uploads."));
      properties.put("savedBulkLoadProfileId", new Schema().withType(Type.INTEGER).withDescription("A saved bulk load profile to load with."));
      properties.put("profile", new Schema().withType(Type.OBJECT).withDescription("A bulk load profile (BulkLoadProfile v1 JSON) to load with."));
      properties.put("mode", new Schema().withType(Type.STRING).withEnumValues(List.of("EXECUTE", "VALIDATE")).withDescription("EXECUTE (default) or VALIDATE (no store)."));
      properties.put("priority", new Schema().withType(Type.INTEGER).withDescription("0-9, default 5 (higher runs first)."));
      properties.put("notifyEmail", new Schema().withType(Type.BOOLEAN).withDescription("Email the owner when the job finishes."));
      properties.put("label", new Schema().withType(Type.STRING).withDescription("A label for the job (defaults to the process label)."));
      properties.put("startAfterStep", new Schema().withType(Type.STRING).withDescription("The launch point: backend steps after this step run in the job."));
      properties.put("expectedRows", new Schema().withType(Type.INTEGER).withDescription("A known row count (e.g., from a count)."));

      Map<String, Content> content = new LinkedHashMap<>();
      content.put(ContentType.APPLICATION_JSON.getMimeType(), new Content().withSchema(new Schema()
         .withType(Type.OBJECT)
         .withDescription("The launch request.")
         .withProperties(properties)));
      return new RequestBody().withContent(content);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public ProcessJobLaunchInput buildInput(Context context) throws Exception
   {
      Map<String, Object> body;
      try
      {
         body = StringUtils.hasContent(context.body()) ? JsonUtils.toObject(context.body(), new TypeReference<>() {}) : Map.of();
      }
      catch(Exception e)
      {
         throw (new QBadRequestException("The request body is not a JSON object."));
      }

      LaunchRequest request = new LaunchRequest(getRequestParam(context, "processName"));
      if(body.get("values") instanceof Map<?, ?> values)
      {
         Map<String, Serializable> map = new LinkedHashMap<>();
         values.forEach((k, v) -> map.put(String.valueOf(k), (Serializable) v));
         request.withValues(map);
      }
      if(body.get("filter") != null)
      {
         request.withFilter(JsonUtils.toObject(JsonUtils.toJson(body.get("filter")), QQueryFilter.class));
      }
      if(body.get("recordIds") instanceof List<?> ids)
      {
         request.withRecordIds(ids.stream().map(id -> (Serializable) id).toList());
      }
      if(body.get("uploadRefs") instanceof List<?> refs)
      {
         request.withUploadRefs(refs.stream().map(String::valueOf).toList());
      }
      request.withSavedBulkLoadProfileId(ValueUtils.getValueAsInteger(body.get("savedBulkLoadProfileId")));
      if(body.get("profile") != null)
      {
         request.withProfileJson(JsonUtils.toJson(body.get("profile")));
      }
      if(body.get("mode") != null)
      {
         try
         {
            request.withMode(JobMode.valueOf(String.valueOf(body.get("mode"))));
         }
         catch(IllegalArgumentException e)
         {
            throw (new QBadRequestException("mode must be EXECUTE or VALIDATE."));
         }
      }
      request.withPriority(ValueUtils.getValueAsInteger(body.get("priority")));
      request.withNotifyEmail(Boolean.TRUE.equals(ValueUtils.getValueAsBoolean(body.get("notifyEmail"))));
      request.withLabel(ValueUtils.getValueAsString(body.get("label")));
      request.withStartAfterStep(ValueUtils.getValueAsString(body.get("startAfterStep")));
      Integer expectedRows = ValueUtils.getValueAsInteger(body.get("expectedRows"));
      request.withExpectedRows(expectedRows == null ? null : expectedRows.longValue());

      ProcessJobLaunchInput input = new ProcessJobLaunchInput();
      input.setLaunchRequest(request);
      return (input);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return Map.of(ProcessJobLaunchResponseV1.class.getSimpleName(), new ProcessJobLaunchResponseV1().toSchema(),
         "BasicErrorResponseV1", new BasicErrorResponseV1().toSchema());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      return new BasicResponse("""
         The new job's id and status.""",
         ProcessJobLaunchResponseV1.class.getSimpleName());
   }



   /*******************************************************************************
    ** Too many queued jobs for the user is a documented 429.
    *******************************************************************************/
   @Override
   public List<BasicResponse> defineAdditionalBasicResponses()
   {
      return (List.of(new BasicResponse(HttpStatus.TOO_MANY_REQUESTS, "Too many jobs are waiting for this user (or for the assistant).", "BasicErrorResponseV1")));
   }

}
