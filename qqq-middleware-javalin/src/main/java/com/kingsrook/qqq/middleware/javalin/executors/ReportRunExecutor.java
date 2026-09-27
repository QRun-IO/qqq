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

package com.kingsrook.qqq.middleware.javalin.executors;


import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobManager;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.reporting.GenerateReportAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportDestination;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormat;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportInput;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.middleware.javalin.executors.io.ReportRunInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableExportOutputInterface;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Run a report and stream the generated file, as the legacy
 ** GET /reports/{reportName} route does: 404 for an unknown report, the report
 ** permission check, the report's input fields read from the request (400 for a
 ** missing required one or a value of the wrong type), and the file streamed in
 ** the requested format while it is generated.
 *******************************************************************************/
public class ReportRunExecutor extends AbstractMiddlewareExecutor<ReportRunInput, TableExportOutputInterface>
{
   private static final QLogger LOG = QLogger.getLogger(ReportRunExecutor.class);



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void execute(ReportRunInput input, TableExportOutputInterface output) throws QException
   {
      try
      {
         String          reportName = input.getReportName();
         QReportMetaData report     = QContext.getQInstance().getReport(reportName);
         if(report == null)
         {
            throw (new QNotFoundException("Report [" + reportName + "] is not found."));
         }

         ReportInput reportInput = new ReportInput();
         PermissionsHelper.checkReportPermissionThrowing(reportInput, reportName);

         if(!StringUtils.hasContent(input.getFormat()))
         {
            throw (new QBadRequestException("Report format was not specified."));
         }
         ReportFormat reportFormat;
         try
         {
            reportFormat = ReportFormat.fromString(input.getFormat());
         }
         catch(Exception e)
         {
            throw (new QBadRequestException("Unsupported report format: " + input.getFormat()));
         }
         String filename = reportName + "." + reportFormat.toString().toLowerCase(Locale.ROOT);

         reportInput.setReportName(reportName);
         reportInput.setReportDestination(new ReportDestination()
            .withReportFormat(reportFormat)
            .withFilename(filename));

         Map<String, String> inputValues = CollectionUtils.nonNullMap(input.getInputValues());
         for(QFieldMetaData inputField : CollectionUtils.nonNullList(report.getInputFields()))
         {
            if(inputValues.containsKey(inputField.getName()))
            {
               Serializable typedValue;
               try
               {
                  typedValue = ValueUtils.getValueAsFieldType(inputField.getType(), inputValues.get(inputField.getName()));
               }
               catch(Exception e)
               {
                  throw (new QBadRequestException("Error processing query param [" + inputField.getName() + "]: " + e.getClass().getSimpleName() + " (" + e.getMessage() + ")"));
               }
               reportInput.addInputValue(inputField.getName(), typedValue);
            }
            else if(inputField.getIsRequired())
            {
               throw (new QBadRequestException("Missing query param value for required input field: [" + inputField.getName() + "]"));
            }
         }

         PipedOutputStream pipedOutputStream = new PipedOutputStream();
         PipedInputStream  pipedInputStream  = new PipedInputStream();
         pipedOutputStream.connect(pipedInputStream);
         reportInput.getReportDestination().setReportOutputStream(pipedOutputStream);

         CapturedContext capturedContext = QContext.capture();
         new AsyncJobManager().startJob("Javalin>ReportAction", (o) ->
         {
            try
            {
               QContext.init(capturedContext);
               new GenerateReportAction().execute(reportInput);
               return (true);
            }
            catch(Exception e)
            {
               LOG.warn("Exception in report async job", e, logPair("reportName", reportName));
               try
               {
                  pipedOutputStream.write(("Error generating report: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
               }
               catch(Exception writeException)
               {
                  LOG.warn("Failed to write error message to report output stream", writeException);
               }
               return (false);
            }
            finally
            {
               try
               {
                  pipedOutputStream.close();
               }
               catch(Exception closeException)
               {
                  LOG.warn("Failed to close pipedOutputStream", closeException);
               }
               QContext.clear();
            }
         });

         output.setReportFormat(reportFormat);
         output.setFilename(filename);
         output.setInputStream(pipedInputStream);
      }
      catch(QException qe)
      {
         throw (qe);
      }
      catch(Exception e)
      {
         throw (new QException("Error running report", e));
      }
   }

}
