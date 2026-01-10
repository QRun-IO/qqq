/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.javalin;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobManager;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobState;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobStatus;
import com.kingsrook.qqq.backend.core.actions.async.JobGoingAsyncException;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.processes.CancelProcessAction;
import com.kingsrook.qqq.backend.core.actions.processes.QProcessCallback;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.reporting.GenerateReportAction;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.actions.values.QValueFormatter;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessState;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportDestination;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormat;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ExceptionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeConsumer;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeFunction;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.jetty.http.HttpStatus;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;


/*******************************************************************************
 ** methods for handling qqq processes in javalin.
 *******************************************************************************/
public class QJavalinProcessHandler
{
   private static final QLogger LOG = QLogger.getLogger(QJavalinProcessHandler.class);

   private static int ASYNC_STEP_TIMEOUT_MILLIS = 3_000;



   /*******************************************************************************
    ** Define the routes
    *******************************************************************************/
   public static EndpointGroup getRoutes()
   {
      return (() ->
      {
         path("/processes", () ->
         {
            path("/{processName}", () ->
            {
               get("/init", QJavalinProcessHandler::processInit);
               post("/init", QJavalinProcessHandler::processInit);

               get("/run", QJavalinProcessHandler::processRun);
               post("/run", QJavalinProcessHandler::processRun);

               path("/{processUUID}", () ->
               {
                  post("/step/{step}", QJavalinProcessHandler::processStep);
                  get("/status/{jobUUID}", QJavalinProcessHandler::processStatus);
                  get("/records", QJavalinProcessHandler::processRecords);
                  get("/cancel", QJavalinProcessHandler::processCancel);
               });

               get("/possibleValues/{fieldName}", QJavalinProcessHandler::possibleValues);
               post("/possibleValues/{fieldName}", QJavalinProcessHandler::possibleValues);
            });
         });
         get("/download/{file}", QJavalinProcessHandler::downloadFile);
         post("/download/{file}", QJavalinProcessHandler::downloadFile);

         path("/reports", () ->
         {
            path("/{reportName}", () ->
            {
               get("", QJavalinProcessHandler::reportWithoutFilename);
               get("/{fileName}", QJavalinProcessHandler::reportWithFilename);
            });
         });
      });
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void reportWithFilename(Context context)
   {
      String filename = context.pathParam("fileName");
      report(context, Optional.of(filename));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void reportWithoutFilename(Context context)
   {
      report(context, Optional.empty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void report(Context context, Optional<String> optionalFilename)
   {
      try
      {
         //////////////////////////////////////////
         // read params from the request context //
         //////////////////////////////////////////
         String reportName = context.pathParam("reportName");
         String format     = context.queryParam("format");

         QReportMetaData report = QJavalinImplementation.qInstance.getReport(reportName);
         if(report == null)
         {
            throw (new QNotFoundException("Report [" + reportName + "] is not found."));
         }

         ReportFormat reportFormat = QJavalinImplementation.getReportFormat(context, optionalFilename, format);
         if(reportFormat == null)
         {
            return;
         }

         String filename = optionalFilename.orElse(reportName + "." + reportFormat.toString().toLowerCase(Locale.ROOT));

         /////////////////////////////////////////////
         // set up the report action's input object //
         /////////////////////////////////////////////
         ReportInput reportInput = new ReportInput();
         QJavalinImplementation.setupSession(context, reportInput);
         PermissionsHelper.checkReportPermissionThrowing(reportInput, reportName);

         reportInput.setReportName(reportName);
         reportInput.setInputValues(null); // todo!

         reportInput.setReportDestination(new ReportDestination()
            .withReportFormat(reportFormat)
            .withFilename(filename));

         //////////////////////////////////////////////////////////////
         // process the report's input fields, from the query string //
         //////////////////////////////////////////////////////////////
         for(QFieldMetaData inputField : CollectionUtils.nonNullList(report.getInputFields()))
         {
            try
            {
               boolean setValue = false;
               if(context.queryParamMap().containsKey(inputField.getName()))
               {
                  String       value      = context.queryParamMap().get(inputField.getName()).get(0);
                  Serializable typedValue = ValueUtils.getValueAsFieldType(inputField.getType(), value);
                  reportInput.addInputValue(inputField.getName(), typedValue);
                  setValue = true;
               }

               if(inputField.getIsRequired() && !setValue)
               {
                  QJavalinUtils.respondWithError(context, HttpStatus.Code.BAD_REQUEST, "Missing query param value for required input field: [" + inputField.getName() + "]");
                  return;
               }
            }
            catch(Exception e)
            {
               QJavalinUtils.respondWithError(context, HttpStatus.Code.BAD_REQUEST, "Error processing query param [" + inputField.getName() + "]: " + e.getClass().getSimpleName() + " (" + e.getMessage() + ")");
               return;
            }
         }

         UnsafeFunction<PipedOutputStream, GenerateReportAction, Exception> preAction = (PipedOutputStream pos) ->
         {
            reportInput.getReportDestination().setReportOutputStream(pos);

            GenerateReportAction reportAction = new GenerateReportAction();
            // any pre-action??  export uses this for "too many rows" checks...
            return (reportAction);
         };

         UnsafeConsumer<GenerateReportAction, Exception> execute = (GenerateReportAction generateReportAction) ->
         {
            QJavalinAccessLogger.logStart("report", logPair("reportName", reportName), logPair("format", reportFormat));
            try
            {
               generateReportAction.execute(reportInput);
               QJavalinAccessLogger.logEndSuccess();
            }
            catch(Exception e)
            {
               QJavalinAccessLogger.logEndFail(e);
               throw (e);
            }
         };

         QJavalinImplementation.runStreamedExportOrReport(context, reportFormat, filename, preAction, execute);
      }
      catch(Exception e)
      {
         QJavalinImplementation.handleException(context, e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void downloadFile(Context context)
   {
      try
      {
         QJavalinImplementation.setupSession(context, new AbstractActionInput());
         // todo context.contentType(reportFormat.getMimeType());
         context.header("Content-Disposition", "filename=" + context.pathParam("file"));

         String filePath         = context.queryParam("filePath");
         String storageTableName = context.queryParam("storageTableName");
         String reference        = context.queryParam("storageReference");

         if(filePath != null)
         {
            context.result(new FileInputStream(filePath));
         }
         else if(storageTableName != null && reference != null)
         {
            InputStream inputStream = new StorageAction().getInputStream(new StorageInput(storageTableName).withReference(reference));
            context.result(inputStream);
         }
         else
         {
            throw (new QBadRequestException("Missing query parameters to identify file to download"));
         }

      }
      catch(Exception e)
      {
         QJavalinImplementation.handleException(context, e);
      }
   }



   /*******************************************************************************
    ** Init a process (named in path param :process)
    **
    *******************************************************************************/
   public static void processInit(Context context)
   {
      doProcessInitOrStep(context, null, null, null, RunProcessInput.FrontendStepBehavior.BREAK);
   }



   /*******************************************************************************
    ** Run a process (named in path param :process) - that is - fully run, not
    ** breaking on frontend steps.  Note, we may still go Async - use query or
    ** form body param `_qStepTimeoutMillis` to set a higher timeout to get more
    ** synchronous-like behavior.
    **
    *******************************************************************************/
   public static void processRun(Context context)
   {
      doProcessInitOrStep(context, null, null, null, RunProcessInput.FrontendStepBehavior.SKIP);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void doProcessInitOrStep(Context context, String processUUID, String startAfterStep, String startAtStep, RunProcessInput.FrontendStepBehavior frontendStepBehavior)
   {
      Map<String, Object> resultForCaller    = new HashMap<>();
      Exception           returningException = null;

      String processName = context.pathParam("processName");

      try
      {
         if(processUUID == null)
         {
            processUUID = UUID.randomUUID().toString();
         }
         resultForCaller.put("processUUID", processUUID);

         if(startAfterStep == null && startAtStep == null)
         {
            LOG.info("Initiating process [" + processName + "] [" + processUUID + "]");
         }
         else if(startAfterStep != null)
         {
            LOG.info("Resuming process [" + processName + "] [" + processUUID + "] after step [" + startAfterStep + "]");
         }
         else if(startAtStep != null)
         {
            LOG.info("Resuming process [" + processName + "] [" + processUUID + "] at step [" + startAtStep + "]");
         }
         else
         {
            LOG.warn("A logical impossibility was reached, regarding the nullity of startAfterStep and startAtStep, at least given how this code was originally written.");
         }

         RunProcessInput runProcessInput = new RunProcessInput();
         QJavalinImplementation.setupSession(context, runProcessInput);

         runProcessInput.setProcessName(processName);
         runProcessInput.setFrontendStepBehavior(frontendStepBehavior);
         runProcessInput.setProcessUUID(processUUID);
         runProcessInput.setStartAfterStep(startAfterStep);
         runProcessInput.setStartAtStep(startAtStep);
         populateRunProcessRequestWithValuesFromContext(context, runProcessInput);

         String reportName = ValueUtils.getValueAsString(runProcessInput.getValue("reportName"));
         QJavalinAccessLogger.logStart(startAfterStep == null ? "processInit" : "processStep", logPair("processName", processName), logPair("processUUID", processUUID),
            StringUtils.hasContent(startAfterStep) ? logPair("startAfterStep", startAfterStep) : null,
            StringUtils.hasContent(startAtStep) ? logPair("startAtStep", startAfterStep) : null,
            StringUtils.hasContent(reportName) ? logPair("reportName", reportName) : null);

         //////////////////////////////////////////////////////////////////////////////////////////////////
         // important to do this check AFTER the runProcessInput is populated with values from context - //
         // e.g., in case things like a reportName are set in here                                       //
         //////////////////////////////////////////////////////////////////////////////////////////////////
         PermissionsHelper.checkProcessPermissionThrowing(runProcessInput, processName);

         ////////////////////////////////////////
         // run the process as an async action //
         ////////////////////////////////////////
         Integer timeout = getTimeoutMillis(context);
         RunProcessOutput runProcessOutput = new AsyncJobManager().startJob(processName, timeout, TimeUnit.MILLISECONDS, (callback) ->
         {
            runProcessInput.setAsyncJobCallback(callback);
            return (new RunProcessAction().execute(runProcessInput));
         });

         LOG.debug("Process result error? " + runProcessOutput.getException());
         for(QFieldMetaData outputField : QJavalinImplementation.qInstance.getProcess(runProcessInput.getProcessName()).getOutputFields())
         {
            LOG.debug("Process result output value: " + outputField.getName() + ": " + runProcessOutput.getValues().get(outputField.getName()));
         }

         serializeRunProcessResultForCaller(resultForCaller, runProcessOutput);
         QJavalinAccessLogger.logProcessSummary(processName, processUUID, runProcessOutput);
      }
      catch(JobGoingAsyncException jgae)
      {
         resultForCaller.put("jobUUID", jgae.getJobUUID());
      }
      catch(QPermissionDeniedException pde)
      {
         returningException = pde;
         QJavalinImplementation.handleException(context, pde);
      }
      catch(Exception e)
      {
         //////////////////////////////////////////////////////////////////////////////
         // our other actions in here would do: handleException(context, e);         //
         // which would return a 500 to the client.                                  //
         // but - other process-step actions, they always return a 200, just with an //
         // optional error message - so - keep all of the processes consistent.      //
         //////////////////////////////////////////////////////////////////////////////
         returningException = e;
         serializeRunProcessExceptionForCaller(resultForCaller, e);
      }

      if(returningException != null)
      {
         QJavalinAccessLogger.logEndFail(returningException);
      }
      else
      {
         QJavalinAccessLogger.logEndSuccess();
      }

      ///////////////////////////////////////////////////////////////////////////////////
      // Note:  originally we did not have this serializationInclusion:ALWAYS here -   //
      // which meant that null and empty values from backend would not go to frontend, //
      // which made things like clearing out a value in a field not happen.            //
      // So, this is added to get that beneficial effect.  Unclear if there are any    //
      // negative side-effects - but be aware.                                         //
      // One could imagine that we'd need this to be configurable in the future?       //
      ///////////////////////////////////////////////////////////////////////////////////
      try
      {
         String json = JsonUtils.toJson(resultForCaller, mapper ->
         {
            mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);

            /////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // use this custom serializer to convert null map-keys to empty-strings (rather than having an exception!) //
            /////////////////////////////////////////////////////////////////////////////////////////////////////////////
            mapper.getSerializerProvider().setNullKeySerializer(JsonUtils.nullKeyToEmptyStringSerializer);
         });
         context.result(json);
      }
      catch(Exception e)
      {
         /////////////////////////////////////////////////////////////////////////////////////////////////////
         // related to the change above - we've seen at least one new error that can come from the          //
         // Include.ALWAYS change (a null key in a map -> jackson exception).  So, try-catch around         //
         // the above serialization, and if it does throw, log, but continue trying a default serialization //
         // as that is probably preferable to an exception for the caller...                                //
         /////////////////////////////////////////////////////////////////////////////////////////////////////
         LOG.warn("Error deserializing process results with serializationInclusion:ALWAYS - will retry with default settings", e, logPair("processName", processName), logPair("processUUID", processUUID));
         context.result(JsonUtils.toJson(resultForCaller));
      }
   }



   /*******************************************************************************
    ** Whether a step finished synchronously or asynchronously, return its data
    ** to the caller the same way.
    *******************************************************************************/
   private static void serializeRunProcessResultForCaller(Map<String, Object> resultForCaller, RunProcessOutput runProcessOutput)
   {
      if(runProcessOutput.getException().isPresent())
      {
         ////////////////////////////////////////////////////////////////
         // per code coverage, this path may never actually get hit... //
         ////////////////////////////////////////////////////////////////
         serializeRunProcessExceptionForCaller(resultForCaller, runProcessOutput.getException().get());
      }
      resultForCaller.put("values", runProcessOutput.getValues());
      runProcessOutput.getProcessState().getNextStepName().ifPresent(nextStep -> resultForCaller.put("nextStep", nextStep));
      runProcessOutput.getProcessState().getBackStepName().ifPresent(backStep -> resultForCaller.put("backStep", backStep));

      /////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // todo - delete after all frontends look for processMetaDataAdjustment instead of updatedFrontendStepList //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////
      List<QFrontendStepMetaData> updatedFrontendStepList = runProcessOutput.getUpdatedFrontendStepList();
      if(updatedFrontendStepList != null)
      {
         resultForCaller.put("updatedFrontendStepList", updatedFrontendStepList);
      }

      if(runProcessOutput.getProcessMetaDataAdjustment() != null)
      {
         resultForCaller.put("processMetaDataAdjustment", runProcessOutput.getProcessMetaDataAdjustment());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void serializeRunProcessExceptionForCaller(Map<String, Object> resultForCaller, Exception exception)
   {
      QUserFacingException userFacingException = ExceptionUtils.findClassInRootChain(exception, QUserFacingException.class);

      if(userFacingException != null)
      {
         LOG.info("User-facing exception in process", userFacingException);
         resultForCaller.put("error", userFacingException.getMessage());
         resultForCaller.put("userFacingError", userFacingException.getMessage());
      }
      else
      {
         Throwable rootException = ExceptionUtils.getRootException(exception);
         LOG.warn("Uncaught Exception in process", exception);
         resultForCaller.put("error", "Error message: " + rootException.getMessage());
      }
   }



   /*******************************************************************************
    ** take values from query-string params, and put them into the run process request
    ** todo - make query params have a "field-" type of prefix??
    **
    *******************************************************************************/
   private static void populateRunProcessRequestWithValuesFromContext(Context context, RunProcessInput runProcessInput) throws IOException, QException
   {
      //////////////////////////
      // process query string //
      //////////////////////////
      for(Map.Entry<String, List<String>> queryParam : context.queryParamMap().entrySet())
      {
         String       fieldName = queryParam.getKey();
         List<String> values    = queryParam.getValue();
         if(CollectionUtils.nullSafeHasContents(values))
         {
            runProcessInput.addValue(fieldName, values.get(0));
         }
      }

      ////////////////////////////
      // process form/post body //
      ////////////////////////////
      for(Map.Entry<String, List<String>> formParam : context.formParamMap().entrySet())
      {
         String       fieldName = formParam.getKey();
         List<String> values    = formParam.getValue();
         if(CollectionUtils.nullSafeHasContents(values))
         {
            runProcessInput.addValue(fieldName, values.get(0));
         }
      }

      ////////////////////////////
      // process uploaded files //
      ////////////////////////////
      for(Map.Entry<String, List<UploadedFile>> entry : context.uploadedFileMap().entrySet())
      {
         String                  name          = entry.getKey();
         List<UploadedFile>      uploadedFiles = entry.getValue();
         ArrayList<StorageInput> storageInputs = new ArrayList<>();
         runProcessInput.addValue(name, storageInputs);

         String storageTableName = QJavalinImplementation.javalinMetaData.getUploadedFileArchiveTableName();
         if(!StringUtils.hasContent(storageTableName))
         {
            throw (new QException("UploadFileArchiveTableName was not specified in javalinMetaData.  Cannot accept file uploads."));
         }

         for(UploadedFile uploadedFile : uploadedFiles)
         {
            String reference = QValueFormatter.formatDate(LocalDate.now())
                               + File.separator + runProcessInput.getProcessName()
                               + File.separator + UUID.randomUUID()
                               + File.separator + uploadedFile.filename();

            StorageInput storageInput = new StorageInput(storageTableName).withReference(reference);
            storageInputs.add(storageInput);

            try
               (
                  InputStream content = uploadedFile.content();
                  OutputStream outputStream = new StorageAction().createOutputStream(storageInput);
               )
            {
               content.transferTo(outputStream);
               LOG.info("Streamed uploaded file", logPair("storageTable", storageTableName), logPair("reference", reference), logPair("processName", runProcessInput.getProcessName()), logPair("uploadFileName", uploadedFile.filename()));
            }
            catch(QException e)
            {
               throw (new QException("Error creating output stream in table [" + storageTableName + "] for storage action", e));
            }
         }
      }

      /////////////////////////////////////////////////////////////
      // deal with params that specify an initial-records filter //
      /////////////////////////////////////////////////////////////
      QQueryFilter initialRecordsFilter = buildProcessInitRecordsFilter(context, runProcessInput);
      if(initialRecordsFilter != null)
      {
         runProcessInput.setCallback(new QProcessCallback()
         {
            @Override
            public QQueryFilter getQueryFilter()
            {
               return (initialRecordsFilter);
            }



            @Override
            public Map<String, Serializable> getFieldValues(List<QFieldMetaData> fields)
            {
               return (Collections.emptyMap());
            }
         });
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QQueryFilter buildProcessInitRecordsFilter(Context context, RunProcessInput runProcessInput) throws IOException
   {
      QInstance        instance = QContext.getQInstance();
      QProcessMetaData process  = instance.getProcess(runProcessInput.getProcessName());
      QTableMetaData   table    = instance.getTable(process.getTableName());

      if(table == null)
      {
         LOG.info("No table found in process - so not building an init records filter.");
         return (null);
      }
      String primaryKeyField = table.getPrimaryKeyField();

      String recordsParam = context.queryParam("recordsParam");
      if(StringUtils.hasContent(recordsParam))
      {
         @SuppressWarnings("ConstantConditions")
         String paramValue = context.queryParam(recordsParam);
         if(!StringUtils.hasContent(paramValue))
         {
            throw (new IllegalArgumentException("Missing value in query parameter: " + recordsParam + " (which was specified as the recordsParam)"));
         }

         switch(recordsParam)
         {
            case "recordIds":
               @SuppressWarnings("ConstantConditions")
               Serializable[] idStrings = paramValue.split(",");
               return (new QQueryFilter().withCriteria(new QFilterCriteria()
                  .withFieldName(primaryKeyField)
                  .withOperator(QCriteriaOperator.IN)
                  .withValues(Arrays.stream(idStrings).toList())));
            case "filterJSON":
               return (JsonUtils.toObject(paramValue, QQueryFilter.class));
            case "filterId":
               // return (JsonUtils.toObject(context.queryParam(recordsParam), QQueryFilter.class));
               throw (new NotImplementedException("Saved filters are not yet implemented."));
            default:
               throw (new IllegalArgumentException("Unrecognized value [" + recordsParam + "] for query parameter: recordsParam"));
         }
      }

      return (null);
   }



   /*******************************************************************************
    ** Run a step in a process (named in path param :processName)
    **
    *******************************************************************************/
   public static void processStep(Context context)
   {
      String processUUID = context.pathParam("processUUID");

      String startAfterStep = null;
      String startAtStep = null;
      if(BooleanUtils.isTrue(ValueUtils.getValueAsBoolean(context.queryParam("isStepBack"))))
      {
         startAtStep = context.pathParam("step");
      }
      else
      {
         startAfterStep = context.pathParam("step");
      }

      doProcessInitOrStep(context, processUUID, startAfterStep, startAtStep, RunProcessInput.FrontendStepBehavior.BREAK);
   }



   /*******************************************************************************
    ** Get status for a currently running process (step)
    *******************************************************************************/
   public static void processStatus(Context context)
   {
      Map<String, Object> resultForCaller = new HashMap<>();

      try
      {
         AbstractActionInput input = new AbstractActionInput();
         QJavalinImplementation.setupSession(context, input);

         // todo... get process values? PermissionsHelper.checkProcessPermissionThrowing(input, context.pathParam("processName"));

         String processName = context.pathParam("processName");
         String processUUID = context.pathParam("processUUID");
         String jobUUID     = context.pathParam("jobUUID");

         resultForCaller.put("processUUID", processUUID);

         LOG.debug("Request for status of process " + processUUID + ", job " + jobUUID);
         Optional<AsyncJobStatus> optionalJobStatus = new AsyncJobManager().getJobStatus(jobUUID);
         if(optionalJobStatus.isEmpty())
         {
            serializeRunProcessExceptionForCaller(resultForCaller, new RuntimeException("Could not find status of process step job"));
         }
         else
         {
            AsyncJobStatus jobStatus = optionalJobStatus.get();

            resultForCaller.put("jobStatus", jobStatus);
            LOG.debug("Job status is " + jobStatus.getState() + " for " + jobUUID);

            if(jobStatus.getState().equals(AsyncJobState.COMPLETE))
            {
               ///////////////////////////////////////////////////////////////////////////////////////
               // if the job is complete, get the process result from state provider, and return it //
               // this output should look like it did if the job finished synchronously!!           //
               ///////////////////////////////////////////////////////////////////////////////////////
               Optional<ProcessState> processState = RunProcessAction.getState(processUUID);
               if(processState.isPresent())
               {
                  RunProcessOutput runProcessOutput = new RunProcessOutput(processState.get());
                  serializeRunProcessResultForCaller(resultForCaller, runProcessOutput);
                  QJavalinAccessLogger.logProcessSummary(processName, processUUID, runProcessOutput);
               }
               else
               {
                  serializeRunProcessExceptionForCaller(resultForCaller, new RuntimeException("Could not find results for process " + processUUID));
               }
            }
            else if(jobStatus.getState().equals(AsyncJobState.ERROR))
            {
               ///////////////////////////////////////////////////////////////////////////////////////////////////////////
               // if the job had an error (e.g., a process step threw), "nicely" serialize its exception for the caller //
               ///////////////////////////////////////////////////////////////////////////////////////////////////////////
               if(jobStatus.getCaughtException() != null)
               {
                  serializeRunProcessExceptionForCaller(resultForCaller, jobStatus.getCaughtException());
               }
            }
         }

         context.result(JsonUtils.toJson(resultForCaller));
      }
      catch(Exception e)
      {
         serializeRunProcessExceptionForCaller(resultForCaller, e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void processRecords(Context context)
   {
      try
      {
         AbstractActionInput input = new AbstractActionInput();
         QJavalinImplementation.setupSession(context, input);
         // todo - need process values? PermissionsHelper.checkProcessPermissionThrowing(input, context.pathParam("processName"));

         String  processUUID = context.pathParam("processUUID");
         Integer skip        = Objects.requireNonNullElse(QJavalinUtils.integerQueryParam(context, "skip"), 0);
         Integer limit       = Objects.requireNonNullElse(QJavalinUtils.integerQueryParam(context, "limit"), 20);

         // todo - potential optimization - if a future state provider could take advantage of it,
         //  we might pass the skip & limit in to a method that fetch just those 'n' rows from state, rather than the whole thing?
         Optional<ProcessState> optionalProcessState = RunProcessAction.getState(processUUID);
         if(optionalProcessState.isEmpty())
         {
            throw (new Exception("Could not find process results."));
         }
         ProcessState processState = optionalProcessState.get();

         List<QRecord>       records         = processState.getRecords();
         Map<String, Object> resultForCaller = new HashMap<>();

         if(records == null)
         {
            resultForCaller.put("records", new ArrayList<>());
            resultForCaller.put("totalRecords", 0);
         }
         else
         {
            List<QRecord> recordPage = CollectionUtils.safelyGetPage(records, skip, limit);
            resultForCaller.put("records", recordPage);
            resultForCaller.put("totalRecords", records.size());
         }

         context.result(JsonUtils.toJson(resultForCaller));
      }
      catch(Exception e)
      {
         QJavalinImplementation.handleException(context, e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void processCancel(Context context)
   {
      try
      {
         RunProcessInput runProcessInput = new RunProcessInput();
         QJavalinImplementation.setupSession(context, runProcessInput);

         runProcessInput.setProcessName(context.pathParam("processName"));
         runProcessInput.setProcessUUID(context.pathParam("processUUID"));

         new CancelProcessAction().execute(runProcessInput);

         Map<String, Object> resultForCaller = new HashMap<>();
         context.result(JsonUtils.toJson(resultForCaller));
      }
      catch(Exception e)
      {
         QJavalinImplementation.handleException(context, e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void possibleValues(Context context)
   {
      try
      {
         String processName = context.pathParam("processName");
         String fieldName   = context.pathParam("fieldName");

         QProcessMetaData process = QJavalinImplementation.qInstance.getProcess(processName);
         if(process == null)
         {
            throw (new QNotFoundException("Could not find process named " + processName + " in this instance."));
         }

         Optional<QFieldMetaData> optField = process.getInputFields().stream().filter(f -> f.getName().equals(fieldName)).findFirst();
         QFieldMetaData           field    = optField.orElseThrow(() -> new QNotFoundException("Could not find field named " + fieldName + " in process " + processName + "."));

         if(!StringUtils.hasContent(field.getPossibleValueSourceName()))
         {
            throw (new QNotFoundException("Field " + fieldName + " in process " + processName + " is not associated with a possible value source."));
         }

         QJavalinImplementation.finishPossibleValuesRequest(context, field);
      }
      catch(Exception e)
      {
         QJavalinImplementation.handleException(context, e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void setAsyncStepTimeoutMillis(int asyncStepTimeoutMillis)
   {
      ASYNC_STEP_TIMEOUT_MILLIS = asyncStepTimeoutMillis;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Integer getTimeoutMillis(Context context)
   {
      Integer timeout = QJavalinUtils.integerQueryParam(context, "_qStepTimeoutMillis");
      if(timeout == null)
      {
         timeout = QJavalinUtils.integerFormParam(context, "_qStepTimeoutMillis");
         if(timeout == null)
         {
            timeout = ASYNC_STEP_TIMEOUT_MILLIS;
         }
      }
      return timeout;
   }

}
