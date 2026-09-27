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

package com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
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
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionOutputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.VariantRunStrategy;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.QScheduleMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.basepull.BasepullConfiguration;


/*******************************************************************************
 ** Definition for Streamed ETL process that includes a frontend.
 **
 ** This process uses 3 backend steps, and 2 frontend steps, as follows:
 ** - preview (backend) - does just a little work (limited # of rows), to give the
 **      user a preview of what the final result will be - e.g., some data to seed the review screen
 ** - review (frontend) - a review screen
 ** - validate (backend) - optionally (per input on review screen), does like the preview step,
 **      but on all records from the extract step.
 ** - review (frontend) - a second view of the review screen, if the validate step was executed.
 ** - execute (backend) - processes all the rows, does all the work.
 ** - result (frontend) - a result screen
 **
 ** The preview & execute steps use additional BackendStep codes:
 ** - Extract - gets the rows to be processed.  Used in preview (but only for a
 **      limited number of rows), and execute (without limit)
 ** - Transform - do whatever transformation is needed to the rows.  Done on preview
 **      and execute.  Always works with a "page" of records at a time.
 ** - Load - store the records into the backend, as appropriate.  Always works
 **      with a "page" of records at a time.  Only called by execute step.
 *******************************************************************************/
public class StreamedETLWithFrontendProcess
{
   private static final QLogger LOG = QLogger.getLogger(StreamedETLWithFrontendProcess.class);

   public static final String STEP_NAME_PREVIEW  = "preview";
   public static final String STEP_NAME_REVIEW   = "review";
   public static final String STEP_NAME_VALIDATE = "validate";
   public static final String STEP_NAME_EXECUTE  = "execute";
   public static final String STEP_NAME_RESULT   = "result";

   public static final String FIELD_EXTRACT_CODE   = "extract"; // QCodeReference, of AbstractExtractStep
   public static final String FIELD_TRANSFORM_CODE = "transform"; // QCodeReference, of AbstractTransformStep
   public static final String FIELD_LOAD_CODE      = "load"; // QCodeReference, of AbstractLoadStep

   public static final String FIELD_SOURCE_TABLE         = "sourceTable"; // String
   public static final String FIELD_DESTINATION_TABLE    = "destinationTable"; // String
   public static final String FIELD_RECORD_COUNT         = "recordCount"; // Integer
   public static final String FIELD_DEFAULT_QUERY_FILTER = "defaultQueryFilter"; // QQueryFilter or String (json, of q QQueryFilter)
   public static final String FIELD_FETCH_HEAVY_FIELDS   = "fetchHeavyFields"; // Boolean
   public static final String FIELD_INCLUDE_ASSOCIATIONS = "includeAssociations"; // Boolean

   public static final String FIELD_SUPPORTS_FULL_VALIDATION = "supportsFullValidation"; // Boolean
   public static final String FIELD_DO_FULL_VALIDATION       = "doFullValidation"; // Boolean
   public static final String FIELD_VALIDATION_SUMMARY       = "validationSummary"; // List<ProcessSummaryLine>
   public static final String FIELD_PROCESS_SUMMARY          = "processResults"; // List<ProcessSummaryLine>

   public static final String DEFAULT_PREVIEW_MESSAGE_FOR_INSERT           = "This is a preview of the records that will be created.";
   public static final String DEFAULT_PREVIEW_MESSAGE_FOR_UPDATE           = "This is a preview of the records that will be updated.";
   public static final String DEFAULT_PREVIEW_MESSAGE_FOR_INSERT_OR_UPDATE = "This is a preview of the records that will be inserted or updated.";
   public static final String DEFAULT_PREVIEW_MESSAGE_FOR_DELETE           = "This is a preview of the records that will be deleted.";
   public static final String DEFAULT_PREVIEW_MESSAGE_PREFIX               = "This is a preview of the records that will be ";
   public static final String FIELD_PREVIEW_MESSAGE                        = "previewMessage";

   public static final String FIELD_TRANSACTION_LEVEL       = "transactionLevel";
   public static final String TRANSACTION_LEVEL_AUTO_COMMIT = "autoCommit";
   public static final String TRANSACTION_LEVEL_PAGE        = "page";
   public static final String TRANSACTION_LEVEL_PROCESS     = "process";



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QProcessMetaData defineProcessMetaData(
      String sourceTableName,
      String destinationTableName,
      Class<? extends AbstractExtractStep> extractStepClass,
      Class<? extends AbstractTransformStep> transformStepClass,
      Class<? extends AbstractLoadStep> loadStepClass
   )
   {
      Map<String, Serializable> defaultFieldValues = new HashMap<>();
      defaultFieldValues.put(FIELD_SOURCE_TABLE, sourceTableName);
      defaultFieldValues.put(FIELD_DESTINATION_TABLE, destinationTableName);
      defaultFieldValues.put(FIELD_TRANSACTION_LEVEL, TRANSACTION_LEVEL_PROCESS);
      return defineProcessMetaData(extractStepClass, transformStepClass, loadStepClass, defaultFieldValues);
   }



   /*******************************************************************************
    ** @param defaultFieldValues - expected to possibly contain values for the following field names:
    ** - FIELD_SOURCE_TABLE
    ** - FIELD_DESTINATION_TABLE
    ** - FIELD_SUPPORTS_FULL_VALIDATION
    ** - FIELD_DEFAULT_QUERY_FILTER
    ** - FIELD_DO_FULL_VALIDATION
    ** - FIELD_PREVIEW_MESSAGE
    *******************************************************************************/
   public static QProcessMetaData defineProcessMetaData(
      Class<? extends AbstractExtractStep> extractStepClass,
      Class<? extends AbstractTransformStep> transformStepClass,
      Class<? extends AbstractLoadStep> loadStepClass,
      Map<String, Serializable> defaultFieldValues
   )
   {
      QStepMetaData previewStep = new QBackendStepMetaData()
         .withName(STEP_NAME_PREVIEW)
         .withCode(new QCodeReference(StreamedETLPreviewStep.class))
         .withInputData(new QFunctionInputMetaData()
            .withField(new QFieldMetaData(FIELD_SOURCE_TABLE, QFieldType.STRING).withDefaultValue(defaultFieldValues.get(FIELD_SOURCE_TABLE)))
            .withField(new QFieldMetaData(FIELD_INCLUDE_ASSOCIATIONS, QFieldType.BOOLEAN).withDefaultValue(defaultFieldValues.getOrDefault(FIELD_INCLUDE_ASSOCIATIONS, false)))
            .withField(new QFieldMetaData(FIELD_FETCH_HEAVY_FIELDS, QFieldType.BOOLEAN).withDefaultValue(defaultFieldValues.getOrDefault(FIELD_FETCH_HEAVY_FIELDS, false)))
            .withField(new QFieldMetaData(FIELD_DESTINATION_TABLE, QFieldType.STRING).withDefaultValue(defaultFieldValues.get(FIELD_DESTINATION_TABLE)))
            .withField(new QFieldMetaData(FIELD_SUPPORTS_FULL_VALIDATION, QFieldType.BOOLEAN).withDefaultValue(defaultFieldValues.getOrDefault(FIELD_SUPPORTS_FULL_VALIDATION, true)))
            .withField(new QFieldMetaData(FIELD_DO_FULL_VALIDATION, QFieldType.BOOLEAN).withDefaultValue(defaultFieldValues.get(FIELD_DO_FULL_VALIDATION)))
            .withField(new QFieldMetaData(FIELD_DEFAULT_QUERY_FILTER, QFieldType.STRING).withDefaultValue(defaultFieldValues.get(FIELD_DEFAULT_QUERY_FILTER)))
            .withField(new QFieldMetaData(FIELD_EXTRACT_CODE, QFieldType.STRING).withDefaultValue(extractStepClass == null ? null : new QCodeReference(extractStepClass)))
            .withField(new QFieldMetaData(FIELD_TRANSFORM_CODE, QFieldType.STRING).withDefaultValue(transformStepClass == null ? null : new QCodeReference(transformStepClass)))
            .withField(new QFieldMetaData(FIELD_TRANSFORM_CODE + "_expectedType", QFieldType.STRING).withDefaultValue(AbstractTransformStep.class.getName()))
            .withField(new QFieldMetaData(FIELD_PREVIEW_MESSAGE, QFieldType.STRING).withDefaultValue(defaultFieldValues.getOrDefault(FIELD_PREVIEW_MESSAGE, DEFAULT_PREVIEW_MESSAGE_FOR_INSERT)))
            .withField(new QFieldMetaData(FIELD_TRANSACTION_LEVEL, QFieldType.STRING).withDefaultValue(defaultFieldValues.getOrDefault(FIELD_TRANSACTION_LEVEL, TRANSACTION_LEVEL_PROCESS)))
         );

      QFrontendStepMetaData reviewStep = new QFrontendStepMetaData()
         .withName(STEP_NAME_REVIEW)
         .withComponent(new QFrontendComponentMetaData().withType(QComponentType.VALIDATION_REVIEW_SCREEN));

      QStepMetaData validateStep = new QBackendStepMetaData()
         .withName(STEP_NAME_VALIDATE)
         .withCode(new QCodeReference(StreamedETLValidateStep.class))
         .withOutputMetaData(new QFunctionOutputMetaData()
            .withField(new QFieldMetaData(FIELD_VALIDATION_SUMMARY, QFieldType.STRING))
         );

      QStepMetaData executeStep = new QBackendStepMetaData()
         .withName(STEP_NAME_EXECUTE)
         .withCode(new QCodeReference(StreamedETLExecuteStep.class))
         .withInputData(new QFunctionInputMetaData()
            .withField(new QFieldMetaData(FIELD_LOAD_CODE, QFieldType.STRING).withDefaultValue(loadStepClass == null ? null : new QCodeReference(loadStepClass)))
            .withField(new QFieldMetaData(FIELD_LOAD_CODE + "_expectedType", QFieldType.STRING).withDefaultValue(AbstractLoadStep.class.getName())))
         .withOutputMetaData(new QFunctionOutputMetaData()
            .withField(new QFieldMetaData(FIELD_PROCESS_SUMMARY, QFieldType.STRING))
         );

      QFrontendStepMetaData resultStep = new QFrontendStepMetaData()
         .withName(STEP_NAME_RESULT)
         .withComponent(new QFrontendComponentMetaData().withType(QComponentType.PROCESS_SUMMARY_RESULTS));

      return new QProcessMetaData()
         .withStep(previewStep)
         .withStep(reviewStep)
         .withStep(validateStep)
         .withStep(executeStep)
         .withStep(resultStep);
   }


   /***************************************************************************
    ** useful for a process step to call upon 'back'
    ***************************************************************************/
   public static void resetValidationFields(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput)
   {
      runBackendStepInput.addValue(FIELD_DO_FULL_VALIDATION, null);
      runBackendStepInput.addValue(FIELD_VALIDATION_SUMMARY, null);
      runBackendStepInput.addValue(FIELD_PROCESS_SUMMARY, null);

      //////////////////////////////////////////////////////////////////////////////////////////////////
      // in case, on the first time forward, the review step got moved after the validation step      //
      // (see BaseStreamedETLStep.moveReviewStepAfterValidateStep) - then un-do that upon going back. //
      //////////////////////////////////////////////////////////////////////////////////////////////////
      ArrayList<String> stepList = new ArrayList<>(runBackendStepOutput.getProcessState().getStepList());
      LOG.debug("Resetting step list.  It was:" + stepList);
      stepList.removeIf(s -> s.equals(StreamedETLWithFrontendProcess.STEP_NAME_REVIEW));
      stepList.add(stepList.indexOf(StreamedETLWithFrontendProcess.STEP_NAME_PREVIEW) + 1, StreamedETLWithFrontendProcess.STEP_NAME_REVIEW);
      runBackendStepOutput.getProcessState().setStepList(stepList);
      LOG.debug("... and now step list is:  " + stepList);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Builder processMetaDataBuilder()
   {
      return (new Builder(defineProcessMetaData(null, null, null, Collections.emptyMap())));
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
       ** Fluent setter for extractStepClass
       **
       *******************************************************************************/
      public Builder withExtractStepClass(Class<? extends AbstractExtractStep> extractStepClass)
      {
         setInputFieldDefaultValue(FIELD_EXTRACT_CODE, new QCodeReference(extractStepClass));
         return (this);
      }



      /*******************************************************************************
       ** Fluent setter for transformStepClass
       **
       *******************************************************************************/
      public Builder withTransformStepClass(Class<? extends AbstractTransformStep> transformStepClass)
      {
         setInputFieldDefaultValue(FIELD_TRANSFORM_CODE, new QCodeReference(transformStepClass));
         return (this);
      }



      /*******************************************************************************
       ** Fluent setter for loadStepClass
       **
       *******************************************************************************/
      public Builder withLoadStepClass(Class<? extends AbstractLoadStep> loadStepClass)
      {
         setInputFieldDefaultValue(FIELD_LOAD_CODE, new QCodeReference(loadStepClass));
         return (this);
      }



      /*******************************************************************************
       ** Fluent setter for sourceTable
       **
       *******************************************************************************/
      public Builder withSourceTable(String sourceTable)
      {
         setInputFieldDefaultValue(FIELD_SOURCE_TABLE, sourceTable);
         return (this);
      }



      /*******************************************************************************
       ** Fluent setter for destinationTable
       **
       *******************************************************************************/
      public Builder withDestinationTable(String destinationTable)
      {
         setInputFieldDefaultValue(FIELD_DESTINATION_TABLE, destinationTable);
         return (this);
      }



      /*******************************************************************************
       ** Fluent setter for supportsFullValidation
       **
       *******************************************************************************/
      public Builder withSupportsFullValidation(Boolean supportsFullValidation)
      {
         setInputFieldDefaultValue(FIELD_SUPPORTS_FULL_VALIDATION, supportsFullValidation);
         return (this);
      }



      /*******************************************************************************
       ** Fluent setter to set transaction level to auto-commit
       **
       *******************************************************************************/
      public Builder withTransactionLevelAutoCommit()
      {
         setInputFieldDefaultValue(FIELD_TRANSACTION_LEVEL, TRANSACTION_LEVEL_AUTO_COMMIT);
         return (this);
      }



      /*******************************************************************************
       ** Fluent setter to set transaction level to page
       **
       *******************************************************************************/
      public Builder withTransactionLevelPage()
      {
         setInputFieldDefaultValue(FIELD_TRANSACTION_LEVEL, TRANSACTION_LEVEL_PAGE);
         return (this);
      }



      /*******************************************************************************
       ** Fluent setter to set transaction level to process
       **
       *******************************************************************************/
      public Builder withTransactionLevelProcess()
      {
         setInputFieldDefaultValue(FIELD_TRANSACTION_LEVEL, TRANSACTION_LEVEL_PROCESS);
         return (this);
      }



      /*******************************************************************************
       ** Fluent setter for doFullValidation
       **
       *******************************************************************************/
      public Builder withDoFullValidation(Boolean doFullValidation)
      {
         setInputFieldDefaultValue(FIELD_DO_FULL_VALIDATION, doFullValidation);
         return (this);
      }



      /*******************************************************************************
       ** Fluent setter for defaultQueryFilter
       **
       *******************************************************************************/
      public Builder withDefaultQueryFilter(QQueryFilter defaultQueryFilter)
      {
         setInputFieldDefaultValue(FIELD_DEFAULT_QUERY_FILTER, defaultQueryFilter);
         return (this);
      }



      /*******************************************************************************
       ** Fluent setter for previewMessage
       **
       *******************************************************************************/
      public Builder withPreviewMessage(String previewMessage)
      {
         setInputFieldDefaultValue(FIELD_PREVIEW_MESSAGE, previewMessage);
         return (this);
      }



      /*******************************************************************************
       ** Fluent setter for name
       **
       *******************************************************************************/
      public Builder withName(String name)
      {
         processMetaData.setName(name);
         return (this);
      }



      /*******************************************************************************
       ** Fluent setter for label
       **
       *******************************************************************************/
      public Builder withLabel(String name)
      {
         processMetaData.setLabel(name);
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
       ** Fluent setter for minInputRecords
       **
       *******************************************************************************/
      public Builder withMinInputRecords(Integer minInputRecords)
      {
         processMetaData.setMinInputRecords(minInputRecords);
         return (this);
      }



      /*******************************************************************************
       ** Fluent setter for maxInputRecords
       **
       *******************************************************************************/
      public Builder withMaxInputRecords(Integer maxInputRecords)
      {
         processMetaData.setMaxInputRecords(maxInputRecords);
         return (this);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public Builder withReviewStepRecordFields(List<QFieldMetaData> fieldList)
      {
         QFrontendStepMetaData reviewStep = processMetaData.getFrontendStep(StreamedETLWithFrontendProcess.STEP_NAME_REVIEW);
         for(QFieldMetaData fieldMetaData : fieldList)
         {
            reviewStep.withRecordListField(fieldMetaData);
         }

         return (this);
      }



      /*******************************************************************************
       ** Attach more input fields to the process (to its first step)
       *******************************************************************************/
      public Builder withFields(List<QFieldMetaData> fieldList)
      {
         QBackendStepMetaData previewStep = processMetaData.getBackendStep(STEP_NAME_PREVIEW);
         for(QFieldMetaData field : fieldList)
         {
            previewStep.getInputMetaData().withField(field);
         }

         return (this);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public Builder withBasepullConfiguration(BasepullConfiguration basepullConfiguration)
      {
         processMetaData.setBasepullConfiguration(basepullConfiguration);
         return (this);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public Builder withSchedule(QScheduleMetaData schedule)
      {
         processMetaData.setSchedule(schedule);
         return (this);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public Builder withVariantRunStrategy(VariantRunStrategy variantRunStrategy)
      {
         processMetaData.setVariantRunStrategy(variantRunStrategy);
         return (this);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public Builder withVariantBackend(String variantBackend)
      {
         processMetaData.setVariantBackend(variantBackend);
         return (this);
      }
   }
}
