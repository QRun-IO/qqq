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


import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
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
import org.apache.commons.lang3.NotImplementedException;


/*******************************************************************************
 **
 *******************************************************************************/
public class ProcessInitSpecV1 extends AbstractEndpointSpec<ProcessInitOrStepInput, ProcessInitOrStepOrStatusResponseV1, ProcessInitOrStepExecutor>
{
   private static final QLogger LOG = QLogger.getLogger(ProcessInitSpecV1.class);

   public static int DEFAULT_ASYNC_STEP_TIMEOUT_MILLIS = 3_000;



   /***************************************************************************
    **
    ***************************************************************************/
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/processes/{processName}/init")
         .withHttpMethod(HttpMethod.POST)
         .withTag(TagsV1.PROCESSES)
         .withShortSummary("Initialize a process")
         .withLongDescription("""
            For a user to start running a process, this endpoint should be called, to start the process
            and run its first step(s) (any backend steps before the first frontend step).
            
            Additional process-specific values should posted in a form param named `values`, as JSON object
            with keys defined by the process in question.
            
            For a process which needs to operate on a set of records that a user selected, see
            `recordsParam`, and `recordIds` or `filterJSON`.
            
            The response will include a `processUUID`, to be included in all subsequent requests relevant
            to the process.
            
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
            .withDescription("Name of the process to initialize")
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
   public RequestBody defineRequestBody()
   {
      return new RequestBody()
         .withContent(
            ContentType.MULTIPART_FORM_DATA.getMimeType(), new Content()
               .withSchema(new Schema()
                  .withType(Type.OBJECT)
                  .withProperty("values", new Schema()
                     .withType(Type.OBJECT)
                     .withDescription("Process-specific field names and values."))

                  .withProperty("recordsParam", new Schema()
                     .withDescription("Specifies which other query-param will contain the indicator of initial records to pass in to the process.")
                     .withType(Type.STRING)
                     .withExample("recordIds", new Example().withValue("recordIds"))
                     .withExample("filterJSON", new Example().withValue("recordIds")))

                  .withProperty("recordIds", new Schema()
                     .withDescription("Comma-separated list of ids from the table this process is based on, to use as input records for the process.  Needs `recordsParam=recordIds` value to be given as well.")
                     .withType(Type.STRING)
                     .withExample("one id", new Example().withValue("1701"))
                     .withExample("multiple ids", new Example().withValue("42,47")))

                  .withProperty("filterJSON", new Schema()
                     .withDescription("JSON encoded QQueryFilter object, to execute against the table this process is based on, to find input records for the process.  Needs `recordsParam=filterJSON` value to be given as well.")
                     .withType(Type.STRING)
                     .withExample("empty filter (all records)", new Example().withValue("{}"))
                     .withExample("filter by a condition", new Example().withValue(
                        JsonUtils.toJson(new QQueryFilter().withCriteria(new QFilterCriteria("id", QCriteriaOperator.LESS_THAN, 10))))
                     ))

                  .withProperty("stepTimeoutMillis", new Schema()
                     .withDescription("Optionally change the time that the server will wait for the job before letting it go asynchronous.  Default value is 3000.")
                     .withType(Type.INTEGER)
                     .withExample("shorter timeout", new Example().withValue("500"))
                     .withExample("longer timeout", new Example().withValue("60000")))

                  .withProperty("file", new Schema()
                     .withType(Type.STRING)
                     .withFormat("binary")
                     .withDescription("A file upload, for processes which expect to be initialized with an uploaded file.")
                  )
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
      processInitOrStepInput.setStepTimeoutMillis(Objects.requireNonNullElse(getRequestParamInteger(context, "stepTimeoutMillis"), DEFAULT_ASYNC_STEP_TIMEOUT_MILLIS));
      processInitOrStepInput.setValues(getRequestParamMap(context, "values"));

      String       recordsParam         = getRequestParam(context, "recordsParam");
      String       recordIds            = getRequestParam(context, "recordIds");
      String       filterJSON           = getRequestParam(context, "filterJSON");
      QQueryFilter initialRecordsFilter = buildProcessInitRecordsFilter(recordsParam, recordIds, filterJSON, processInitOrStepInput);
      processInitOrStepInput.setRecordsFilter(initialRecordsFilter);

      // todo - uploaded files
      // todo - archive uploaded files?

      return (processInitOrStepInput);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QQueryFilter buildProcessInitRecordsFilter(String recordsParam, String recordIds, String filterJSON, ProcessInitOrStepInput processInitOrStepInput) throws IOException
   {
      QProcessMetaData process = QContext.getQInstance().getProcess(processInitOrStepInput.getProcessName());
      QTableMetaData   table   = QContext.getQInstance().getTable(process.getTableName());

      if(table == null)
      {
         LOG.info("No table found in process - so not building an init records filter.");
         return (null);
      }
      String primaryKeyField = table.getPrimaryKeyField();

      if(StringUtils.hasContent(recordsParam))
      {
         return switch(recordsParam)
         {
            case "recordIds" ->
            {
               Serializable[] idStrings = recordIds.split(",");
               yield (new QQueryFilter().withCriteria(new QFilterCriteria()
                  .withFieldName(primaryKeyField)
                  .withOperator(QCriteriaOperator.IN)
                  .withValues(Arrays.stream(idStrings).toList())));
            }
            case "filterJSON" -> (JsonUtils.toObject(filterJSON, QQueryFilter.class));
            case "filterId" -> throw (new NotImplementedException("Saved filters are not yet implemented."));
            default -> throw (new IllegalArgumentException("Unrecognized value [" + recordsParam + "] for query parameter: recordsParam"));
         };
      }

      return (null);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      return new BasicResponse("""
         State of the initialization of the job, with different fields set, based on the
         status of the task.""",

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
