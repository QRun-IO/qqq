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


import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.processes.AbstractProcessMetaDataBuilder;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QComponentType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendComponentMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportMetaData;


/*******************************************************************************
 ** Definition for Basic process to run a report.
 *******************************************************************************/
public class RunReportForRecordProcess
{
   public static final String PROCESS_NAME = "reports.forRecord";

   public static final String STEP_NAME_PREPARE = "prepare";
   public static final String STEP_NAME_INPUT   = "input";
   public static final String STEP_NAME_EXECUTE = "execute";
   public static final String STEP_NAME_ACCESS  = "accessReport";

   public static final String FIELD_REPORT_NAME = "reportName";
   public static final String FIELD_RECORD_ID   = "recordId";



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Builder processMetaDataBuilder()
   {
      return (new Builder(defineProcessMetaData()));
   }



   /*******************************************************************************
    ** Create a process meta data builder for this type of process, pre-populated
    ** with attributes based on a given report.
    *******************************************************************************/
   public static Builder processMetaDataBuilder(QReportMetaData reportMetaData)
   {
      return (new Builder(defineProcessMetaData())
         .withProcessName(reportMetaData.getProcessName())
         .withReportName(reportMetaData.getName())
         .withTableName(reportMetaData.getDataSources().get(0).getSourceTable())
         .withIcon(reportMetaData.getIcon()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QProcessMetaData defineProcessMetaData()
   {
      QStepMetaData prepareStep = new QBackendStepMetaData()
         .withName(STEP_NAME_PREPARE)
         .withCode(new QCodeReference(PrepareReportForRecordStep.class))
         .withInputData(new QFunctionInputMetaData()
            .withField(new QFieldMetaData(FIELD_REPORT_NAME, QFieldType.STRING))
            .withField(new QFieldMetaData(FIELD_RECORD_ID, QFieldType.STRING)));

      QStepMetaData inputStep = new QFrontendStepMetaData()
         .withName(STEP_NAME_INPUT)
         .withComponent(new QFrontendComponentMetaData().withType(QComponentType.EDIT_FORM));

      QStepMetaData executeStep = new QBackendStepMetaData()
         .withName(STEP_NAME_EXECUTE)
         .withCode(new QCodeReference(ExecuteReportStep.class))
         .withInputData(new QFunctionInputMetaData()
            .withField(new QFieldMetaData(FIELD_REPORT_NAME, QFieldType.STRING)));

      QStepMetaData accessStep = new QFrontendStepMetaData()
         .withName(STEP_NAME_ACCESS)
         .withComponent(new QFrontendComponentMetaData().withType(QComponentType.DOWNLOAD_FORM));
      // .withViewField(new QFieldMetaData("outputFile", QFieldType.STRING))
      // .withViewField(new QFieldMetaData("message", QFieldType.STRING));

      return new QProcessMetaData()
         .withName(PROCESS_NAME)
         .withStep(prepareStep)
         .withStep(inputStep)
         .withStep(executeStep)
         .withStep(accessStep);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class Builder extends AbstractProcessMetaDataBuilder
   {

      /*******************************************************************************
       ** Constructor
       **
       *******************************************************************************/
      public Builder(QProcessMetaData processMetaData)
      {
         super(processMetaData);
      }



      /*******************************************************************************
       ** Fluent setter for name
       **
       *******************************************************************************/
      public Builder withProcessName(String name)
      {
         processMetaData.setName(name);
         return (this);
      }



      /*******************************************************************************
       ** Fluent setter for tableName
       **
       *******************************************************************************/
      public Builder withTableName(String tableName)
      {
         processMetaData.setTableName(tableName);
         return (this);
      }



      /*******************************************************************************
       ** Fluent setter for icon
       **
       *******************************************************************************/
      public Builder withIcon(QIcon icon)
      {
         processMetaData.setIcon(icon);
         return (this);
      }



      /*******************************************************************************
       ** Fluent setter for reportName
       **
       *******************************************************************************/
      public Builder withReportName(String reportName)
      {
         setInputFieldDefaultValue(RunReportForRecordProcess.FIELD_REPORT_NAME, reportName);
         return (this);
      }
   }
}
