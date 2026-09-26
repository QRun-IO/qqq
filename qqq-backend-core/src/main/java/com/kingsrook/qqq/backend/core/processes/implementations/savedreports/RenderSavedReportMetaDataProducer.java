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
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormatPossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.nocode.WidgetHtmlLine;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.processes.NoCodeWidgetFrontendComponentMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QComponentType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendComponentMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QRecordListMetaData;
import com.kingsrook.qqq.backend.core.model.savedreports.SavedReport;
import com.kingsrook.qqq.backend.core.model.savedreports.SavedReportsMetaDataProvider;


/*******************************************************************************
 ** define process for rendering saved reports!
 *******************************************************************************/
public class RenderSavedReportMetaDataProducer implements MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "renderSavedReport";

   public static final String SES_PROVIDER_NAME             = "sesProviderName";
   public static final String FROM_EMAIL_ADDRESS            = "fromEmailAddress";
   public static final String REPLY_TO_EMAIL_ADDRESS        = "replyToEmailAddress";
   public static final String FIELD_NAME_STORAGE_TABLE_NAME = "storageTableName";
   public static final String FIELD_NAME_STORAGE_REFERENCE  = "storageReference";
   public static final String FIELD_NAME_REPORT_FORMAT      = "reportFormat";
   public static final String FIELD_NAME_EMAIL_ADDRESS      = "reportDestinationEmailAddress";
   public static final String FIELD_NAME_EMAIL_SUBJECT      = "emailSubject";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      QProcessMetaData process = new QProcessMetaData()
         .withName(NAME)
         .withLabel("Render Report")
         .withTableName(SavedReport.TABLE_NAME)
         .withIcon(new QIcon().withName("print"))

         .withStep(new QBackendStepMetaData()
            .withName("pre")
            .withInputData(new QFunctionInputMetaData()
               .withField(new QFieldMetaData(SES_PROVIDER_NAME, QFieldType.STRING))
               .withField(new QFieldMetaData(FROM_EMAIL_ADDRESS, QFieldType.STRING))
               .withField(new QFieldMetaData(REPLY_TO_EMAIL_ADDRESS, QFieldType.STRING))
               .withField(new QFieldMetaData(FIELD_NAME_STORAGE_TABLE_NAME, QFieldType.STRING))
               .withField(new QFieldMetaData(FIELD_NAME_STORAGE_REFERENCE, QFieldType.STRING))
               .withRecordListMetaData(new QRecordListMetaData().withTableName(SavedReport.TABLE_NAME)))
            .withCode(new QCodeReference(RenderSavedReportPreStep.class)))

         .withStep(new QFrontendStepMetaData()
            .withName("input")
            .withComponent(new QFrontendComponentMetaData().withType(QComponentType.EDIT_FORM))
            .withFormField(new QFieldMetaData(FIELD_NAME_REPORT_FORMAT, QFieldType.STRING)
               .withPossibleValueSourceName(ReportFormatPossibleValueEnum.NAME)
               .withIsRequired(true))
            .withFormField(new QFieldMetaData(FIELD_NAME_EMAIL_ADDRESS, QFieldType.STRING).withLabel("Email To"))
            .withFormField(new QFieldMetaData(FIELD_NAME_EMAIL_SUBJECT, QFieldType.STRING).withLabel("Email Subject"))
            .withComponent(new QFrontendComponentMetaData().withType(QComponentType.WIDGET)
               .withValue("widgetName", SavedReportsMetaDataProvider.RENDER_REPORT_PROCESS_VALUES_WIDGET)))

         .withStep(new QBackendStepMetaData()
            .withName("execute")
            .withInputData(new QFunctionInputMetaData().withRecordListMetaData(new QRecordListMetaData()
               .withTableName(SavedReport.TABLE_NAME)))
            .withCode(new QCodeReference(RenderSavedReportExecuteStep.class)))

         .withStep(new QFrontendStepMetaData()
            .withName("output")
            .withComponent(new QFrontendComponentMetaData().withType(QComponentType.DOWNLOAD_FORM))
            .withComponent(new NoCodeWidgetFrontendComponentMetaData()
               .withOutput(new WidgetHtmlLine()
                  .withCondition(new QFilterCriteria(FIELD_NAME_EMAIL_ADDRESS, QCriteriaOperator.IS_NOT_BLANK))
                  .withVelocityTemplate(String.format("Report has been emailed to: ${%s}", FIELD_NAME_EMAIL_ADDRESS))))
         );

      return (process);
   }

}
