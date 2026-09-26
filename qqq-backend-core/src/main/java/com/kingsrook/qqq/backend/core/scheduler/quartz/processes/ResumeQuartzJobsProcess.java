/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.scheduler.quartz.processes;


import java.util.List;
import java.util.function.BiFunction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerMultiOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractLoadStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ExtractViaQueryStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.NoopTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.scheduler.quartz.QuartzScheduler;


/*******************************************************************************
 **
 *******************************************************************************/
public class ResumeQuartzJobsProcess extends AbstractLoadStep implements MetaDataProducerInterface<MetaDataProducerMultiOutput>
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public MetaDataProducerMultiOutput produce(QInstance qInstance) throws QException
   {
      BiFunction<String, String, QProcessMetaData> processMaker = (String tableName, String label) ->
         StreamedETLWithFrontendProcess.processMetaDataBuilder()
            .withName(getClass().getSimpleName())
            .withLabel(label)
            .withPreviewMessage("This is a preview of the jobs that will be resumed.")
            .withTableName(tableName)
            .withSourceTable(tableName)
            .withDestinationTable(tableName)
            .withExtractStepClass(ExtractViaQueryStep.class)
            .withTransformStepClass(NoopTransformStep.class)
            .withLoadStepClass(getClass())
            .withIcon(new QIcon("play_circle_outline"))
            .withReviewStepRecordFields(List.of(
               new QFieldMetaData("id", QFieldType.LONG),
               new QFieldMetaData("jobName", QFieldType.STRING),
               new QFieldMetaData("jobGroup", QFieldType.STRING),
               new QFieldMetaData("description", QFieldType.STRING)))
            .getProcessMetaData()
            .withPermissionRules(new QPermissionRules().withPermissionBaseName(getClass().getSimpleName()));

      MetaDataProducerMultiOutput output = new MetaDataProducerMultiOutput();
      output.add(processMaker.apply("quartzJobDetails", "Resume Quartz Jobs"));
      output.add(processMaker.apply("quartzTriggers", "Resume Quartz Triggers").withName(getClass().getSimpleName() + "ForTriggers"));
      return (output);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void runOnePage(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      try
      {
         QuartzScheduler instance = QuartzScheduler.getInstance();
         for(QRecord record : runBackendStepInput.getRecords())
         {
            instance.resumeJob(record.getValueString("jobName"), record.getValueString("jobGroup"));
         }
      }
      catch(Exception e)
      {
         throw (new QException("Error resuming jobs", e));
      }
   }

}
