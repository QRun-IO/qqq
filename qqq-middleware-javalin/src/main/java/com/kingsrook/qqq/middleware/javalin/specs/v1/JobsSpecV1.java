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


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsTables;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.executors.JobsExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.JobsInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.JobsOutput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractMiddlewareVersion;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.BasicErrorResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.JobActionResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.JobDetailV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.JobListResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.JobLogLineV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.JobPhaseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.JobProgressPollV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.JobProgressV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.JobSummaryV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.JobTrackRequestV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.Content;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.RequestBody;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;


/*******************************************************************************
 ** Jobs tray, detail, polling and actions, registered only with the jobs tables.
 *******************************************************************************/
public class JobsSpecV1 extends AbstractEndpointSpec<JobsInput, JobsOutput, JobsExecutor>
{
   private final String operation;
   private final String path;
   private final HttpMethod method;
   private final String responseSchema;



   /*******************************************************************************
    ** One specification per operation, sharing parsing and schema definitions.
    *******************************************************************************/
   public JobsSpecV1(String operation, String path, HttpMethod method, String responseSchema)
   {
      this.operation = operation;
      this.path = path;
      this.method = method;
      this.responseSchema = responseSchema;
   }



   /*******************************************************************************
    ** The six jobs UI operations.
    *******************************************************************************/
   public static List<JobsSpecV1> all()
   {
      return (List.of(
         new JobsSpecV1("list", "/jobs", HttpMethod.GET, "JobListResponseV1"),
         new JobsSpecV1("detail", "/jobs/{jobId}", HttpMethod.GET, "JobDetailV1"),
         new JobsSpecV1("progress", "/jobs/{jobId}/progress", HttpMethod.GET, "JobProgressPollV1"),
         new JobsSpecV1("cancel", "/jobs/{jobId}/cancel", HttpMethod.POST, "JobActionResponseV1"),
         new JobsSpecV1("retry", "/jobs/{jobId}/retry", HttpMethod.POST, "JobActionResponseV1"),
         new JobsSpecV1("track", "/jobs/track", HttpMethod.POST, "JobActionResponseV1")));
   }



   /*******************************************************************************
    ** Declare the operation and cursor overlap contract.
    *******************************************************************************/
   @Override
   public BasicOperation defineBasicOperation()
   {
      return (new BasicOperation().withPath(path).withHttpMethod(method).withTag(TagsV1.PROCESSES)
         .withShortSummary("Jobs: " + operation)
         .withLongDescription("Requires jobs_job.read and the job owner lock; jobs_admin can see all jobs. Hidden jobs return 404; invalid transitions return 409. "
            + "Progress polls include a twenty-id overlap for concurrent commits; deduplicate log lines by id. Recent jobs finished in the last seven days."));
   }



   /*******************************************************************************
    ** Do not bind routes when the instance has no durable jobs tables.
    *******************************************************************************/
   @Override
   public void defineRoute(AbstractMiddlewareVersion version, String base)
   {
      if(qInstance != null && qInstance.getTable(DurableJobsTables.TABLE_JOB) != null && qInstance.getTable(DurableJobsTables.TABLE_LOG) != null)
      {
         super.defineRoute(version, base);
      }
   }



   /*******************************************************************************
    ** Typed query and path parameters.
    *******************************************************************************/
   @Override
   public List<Parameter> defineRequestParameters()
   {
      List<Parameter> parameters = new ArrayList<>();
      if(path.contains("{jobId}"))
      {
         parameters.add(new Parameter().withName("jobId").withIn(In.PATH).withRequired(true).withSchema(new Schema().withType(Type.INTEGER)));
      }
      if("list".equals(operation))
      {
         parameters.add(query("scope", Type.STRING, "mine (default), or all (requires jobs_admin)"));
         parameters.add(query("state", Type.STRING, "active (default), recent, or all"));
         parameters.add(query("limit", Type.INTEGER, "1-200; default 50"));
         parameters.add(query("processName", Type.STRING, "Optional process filter"));
      }
      if("progress".equals(operation))
      {
         parameters.add(query("afterLogId", Type.INTEGER, "Last observed log id, default 0; response includes a twenty-id overlap"));
         parameters.add(query("logLimit", Type.INTEGER, "1-200 new lines, plus overlap; default 100"));
      }
      return (parameters);
   }



   /*******************************************************************************
    ** Query parameter helper.
    *******************************************************************************/
   private Parameter query(String name, Type type, String description)
   {
      return (new Parameter().withName(name).withIn(In.QUERY).withRequired(false).withSchema(new Schema().withType(type)).withDescription(description));
   }



   /*******************************************************************************
    ** Tracking request is a named schema.
    *******************************************************************************/
   @Override
   public RequestBody defineRequestBody()
   {
      return ("track".equals(operation) ? new RequestBody().withContent(Map.of("application/json", new Content().withSchema(new Schema().withRef("#/components/schemas/JobTrackRequestV1")))) : null);
   }



   /*******************************************************************************
    ** Validate all numeric bounds and enumerated query values.
    *******************************************************************************/
   @Override
   public JobsInput buildInput(Context context) throws Exception
   {
      JobsInput input = new JobsInput().withOperation(operation);
      try
      {
         if(path.contains("{jobId}"))
         {
            input.setJobId(Integer.valueOf(context.pathParam("jobId")));
         }
         input.withScope(context.queryParam("scope") == null ? "mine" : context.queryParam("scope"))
            .withState(context.queryParam("state") == null ? "active" : context.queryParam("state"))
            .withLimit(integer(context, "limit", 50)).withLogLimit(integer(context, "logLimit", 100))
            .withAfterLogId(context.queryParam("afterLogId") == null ? 0L : Long.valueOf(context.queryParam("afterLogId")))
            .withProcessName(context.queryParam("processName"));
         if(!List.of("mine", "all").contains(input.getScope()) || !List.of("active", "recent", "all").contains(input.getState()) || input.getAfterLogId() < 0)
         {
            throw (new IllegalArgumentException());
         }
         if("track".equals(operation))
         {
            JobTrackRequestV1 request = JsonUtils.toObject(context.body(), JobTrackRequestV1.class);
            if(request == null || request.getProcessName() == null || request.getProcessName().isBlank())
            {
               throw (new IllegalArgumentException());
            }
            UUID.fromString(request.getProcessUUID());
            UUID.fromString(request.getJobUUID());
            input.setTrack(request);
         }
      }
      catch(Exception e)
      {
         throw (new QBadRequestException("Invalid jobs request parameters."));
      }
      return (input);
   }



   /*******************************************************************************
    ** Bound list sizes before querying storage.
    *******************************************************************************/
   private int integer(Context context, String name, int fallback)
   {
      int value = context.queryParam(name) == null ? fallback : Integer.parseInt(context.queryParam(name));
      if(value < 1 || value > 200)
      {
         throw (new IllegalArgumentException());
      }
      return (value);
   }



   /*******************************************************************************
    ** Serialize the operation-specific response directly.
    *******************************************************************************/
   @Override
   public void handleOutput(Context context, JobsOutput output)
   {
      context.result(JsonUtils.toJson(output.getBody()));
   }



   /*******************************************************************************
    ** Map only transition conflicts to 409.
    *******************************************************************************/
   @Override
   protected void handleException(Context context, Exception exception)
   {
      if(exception instanceof JobsExecutor.Conflict)
      {
         context.status(409).result(JsonUtils.toJson(Map.of("error", exception.getMessage())));
      }
      else
      {
         super.handleException(context, exception);
      }
   }



   /*******************************************************************************
    ** Document success and state conflicts.
    *******************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      return (new BasicResponse("Job " + operation + " result", responseSchema));
   }



   /*******************************************************************************
    ** Additional documented errors.
    *******************************************************************************/
   @Override
   public List<BasicResponse> defineAdditionalBasicResponses()
   {
      return (List.of(new BasicResponse(HttpStatus.CONFLICT, "Invalid job transition", "BasicErrorResponseV1"),
         new BasicResponse(HttpStatus.NOT_FOUND, "Job not found or not visible", "BasicErrorResponseV1"),
         new BasicResponse(HttpStatus.FORBIDDEN, "Permission denied", "BasicErrorResponseV1")));
   }



   /*******************************************************************************
    ** All versioned jobs representations.
    *******************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      Map<String, Schema> schemas = new LinkedHashMap<>();
      schemas.put("JobSummaryV1", new JobSummaryV1().toSchema());
      schemas.put("JobDetailV1", new JobDetailV1().toSchema());
      schemas.put("JobProgressV1", new JobProgressV1().toSchema());
      schemas.put("JobPhaseV1", new JobPhaseV1().toSchema());
      schemas.put("JobLogLineV1", new JobLogLineV1().toSchema());
      schemas.put("JobListResponseV1", new JobListResponseV1().toSchema());
      schemas.put("JobProgressPollV1", new JobProgressPollV1().toSchema());
      schemas.put("JobTrackRequestV1", new JobTrackRequestV1().toSchema());
      schemas.put("JobActionResponseV1", new JobActionResponseV1().toSchema());
      schemas.put("BasicErrorResponseV1", new BasicErrorResponseV1().toSchema());
      return (schemas);
   }
}
