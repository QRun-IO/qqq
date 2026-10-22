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

package com.kingsrook.qqq.backend.core.processes.implementations.tablesync;


import java.util.Collections;
import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QRuntimeException;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.VariantRunStrategy;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.QScheduleMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.basepull.BasepullConfiguration;
import com.kingsrook.qqq.backend.core.processes.implementations.basepull.ExtractViaBasepullQueryStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractExtractStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractLoadStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ExtractViaQueryStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.LoadViaInsertOrUpdateStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;


/*******************************************************************************
 ** Definition for Standard process to sync data from one table into another.
 **
 *******************************************************************************/
public class TableSyncProcess
{
   public static final String FIELD_SOURCE_TABLE_KEY_FIELD        = "sourceTableKeyField"; // String
   public static final String FIELD_DESTINATION_TABLE_FOREIGN_KEY = "destinationTableForeignKey"; // String



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Builder processMetaDataBuilder(boolean isBasePull)
   {
      return (Builder) new Builder(StreamedETLWithFrontendProcess.defineProcessMetaData(
         isBasePull ? ExtractViaBasepullQueryStep.class : ExtractViaQueryStep.class,
         null,
         LoadViaInsertOrUpdateStep.class,
         Collections.emptyMap()))
         .withFields(List.of(
            new QFieldMetaData(FIELD_SOURCE_TABLE_KEY_FIELD, QFieldType.STRING),
            new QFieldMetaData(FIELD_DESTINATION_TABLE_FOREIGN_KEY, QFieldType.STRING)
         ))
         .withPreviewMessage(StreamedETLWithFrontendProcess.DEFAULT_PREVIEW_MESSAGE_FOR_INSERT_OR_UPDATE);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class Builder extends StreamedETLWithFrontendProcess.Builder
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
       ** Fluent setter for transformStepClass
       **
       *******************************************************************************/
      public Builder withTransformStepClass(Class<? extends AbstractTransformStep> transformStepClass)
      {
         throw (new IllegalArgumentException("withTransformStepClass should not be called in a TableSyncProcess.  You probably meant withSyncTransformStepClass"));
      }



      /*******************************************************************************
       ** Fluent setter for loadStepClass
       **
       *******************************************************************************/
      public Builder withLoadStepClass(Class<? extends AbstractLoadStep> loadStepClass)
      {
         super.withLoadStepClass(loadStepClass);
         return (this);
      }



      /*******************************************************************************
       ** Fluent setter for transformStepClass.  Note - call this method also makes
       ** sourceTable and destinationTable be set - by getting them from the
       ** SyncProcessConfig record defined in the step class.
       **
       *******************************************************************************/
      public Builder withSyncTransformStepClass(Class<? extends AbstractTableSyncTransformStep> transformStepClass)
      {
         setInputFieldDefaultValue(StreamedETLWithFrontendProcess.FIELD_TRANSFORM_CODE, new QCodeReference(transformStepClass));
         AbstractTableSyncTransformStep.SyncProcessConfig config;

         try
         {
            AbstractTableSyncTransformStep transformStep = transformStepClass.getConstructor().newInstance();
            config = transformStep.getSyncProcessConfig();
         }
         catch(Exception e)
         {
            throw (new QRuntimeException("Error setting up process with transform step class: " + transformStepClass.getName(), e));
         }

         setInputFieldDefaultValue(StreamedETLWithFrontendProcess.FIELD_SOURCE_TABLE, config.sourceTable());
         setInputFieldDefaultValue(StreamedETLWithFrontendProcess.FIELD_DESTINATION_TABLE, config.destinationTable());
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
         super.withFields(fieldList);
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
      public Builder withExtractStepClass(Class<? extends AbstractExtractStep> extractStepClass)
      {
         super.withExtractStepClass(extractStepClass);
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
