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

package com.kingsrook.qqq.backend.core.processes.implementations.savedreports;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.nocode.WidgetHtmlLine;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.processes.NoCodeWidgetFrontendComponentMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QRecordListMetaData;
import com.kingsrook.qqq.backend.core.model.savedreports.ScheduledReport;


/*******************************************************************************
 ** define process for rendering scheduled reports - that is - a thin layer on
 ** top of rendering a saved report.
 *******************************************************************************/
public class RunScheduledReportMetaDataProducer implements MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "runScheduledReport";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      QProcessMetaData process = new QProcessMetaData()
         .withName(NAME)
         .withLabel("Run Scheduled Report")
         .withTableName(ScheduledReport.TABLE_NAME)
         .withIcon(new QIcon().withName("print"))

         .withStep(new QBackendStepMetaData()
            .withName("execute")
            .withInputData(new QFunctionInputMetaData().withRecordListMetaData(new QRecordListMetaData()
               .withTableName(ScheduledReport.TABLE_NAME)))
            .withCode(new QCodeReference(RunScheduledReportExecuteStep.class)))

         .withStep(new QFrontendStepMetaData()
            .withName("results")
            .withComponent(new NoCodeWidgetFrontendComponentMetaData()
               .withOutput(new WidgetHtmlLine().withVelocityTemplate("Success")))); // todo!!!

      return (process);
   }

}
