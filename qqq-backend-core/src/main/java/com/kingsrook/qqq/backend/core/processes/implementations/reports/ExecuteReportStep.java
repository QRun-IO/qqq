/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.reports;


import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.processes.ProcessFileDownload;
import com.kingsrook.qqq.backend.core.actions.reporting.GenerateReportAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportDestination;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormat;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportInput;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Process step to execute a report.
 **
 ** Writes it to a temp file...  Returns that file name in process output.
 *******************************************************************************/
public class ExecuteReportStep implements BackendStep
{


   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      File tmpFile = null;
      try
      {

         ReportFormat    reportFormat = getReportFormat(runBackendStepInput);
         String          reportName   = runBackendStepInput.getValueString("reportName");
         QReportMetaData report       = QContext.getQInstance().getReport(reportName);
         tmpFile = File.createTempFile(reportName, "." + reportFormat.getExtension());

         runBackendStepInput.getAsyncJobCallback().updateStatus("Generating Report");

         try(FileOutputStream reportOutputStream = new FileOutputStream(tmpFile))
         {
            ReportInput reportInput = new ReportInput();
            reportInput.setReportName(reportName);
            reportInput.setReportDestination(new ReportDestination()
               .withReportFormat(reportFormat)
               .withReportOutputStream(reportOutputStream));

            Map<String, Serializable> values = runBackendStepInput.getValues();
            reportInput.setInputValues(values);

            new GenerateReportAction().execute(reportInput);
         }

         String downloadFileBaseName = getDownloadFileBaseName(runBackendStepInput, report);

         runBackendStepOutput.addValue("downloadFileName", downloadFileBaseName + "." + reportFormat.getExtension());
         runBackendStepOutput.addValue("serverFilePath", ProcessFileDownload.register(tmpFile));
      }
      catch(Exception e)
      {
         if(tmpFile != null)
         {
            try
            {
               Files.deleteIfExists(tmpFile.toPath());
            }
            catch(Exception cleanupException)
            {
               e.addSuppressed(cleanupException);
            }
         }
         throw (new QException("Error running report", e));
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private ReportFormat getReportFormat(RunBackendStepInput runBackendStepInput) throws QUserFacingException
   {
      String reportFormatInput = runBackendStepInput.getValueString(BasicRunReportProcess.FIELD_REPORT_FORMAT);
      if(StringUtils.hasContent(reportFormatInput))
      {
         return (ReportFormat.fromString(reportFormatInput));
      }

      return (ReportFormat.XLSX);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String getDownloadFileBaseName(RunBackendStepInput runBackendStepInput, QReportMetaData report)
   {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm").withZone(ZoneId.systemDefault());
      String            datePart  = formatter.format(Instant.now());

      String downloadFileBaseName = runBackendStepInput.getValueString("downloadFileBaseName");
      if(!StringUtils.hasContent(downloadFileBaseName))
      {
         downloadFileBaseName = report.getLabel();
      }

      downloadFileBaseName = downloadFileBaseName.replaceAll("/", "-");

      return (downloadFileBaseName + " - " + datePart);
   }

}
