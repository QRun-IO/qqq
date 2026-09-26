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
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.nocode.WidgetHtmlLine;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.processes.NoCodeWidgetFrontendComponentMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.scheduler.quartz.QuartzScheduler;


/*******************************************************************************
 ** Manage process to pause all quartz jobs
 *******************************************************************************/
public class PauseAllQuartzJobsProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance qInstance) throws QException
   {
      return new QProcessMetaData()
         .withName(getClass().getSimpleName())
         .withLabel("Pause All Quartz Jobs")
         .withStepList(List.of(
            new QFrontendStepMetaData()
               .withName("confirm")
               .withComponent(new NoCodeWidgetFrontendComponentMetaData()
                  .withOutput(new WidgetHtmlLine().withVelocityTemplate("Please confirm you wish to pause all quartz jobs."))),
            new QBackendStepMetaData()
               .withName("execute")
               .withCode(new QCodeReference(getClass())),
            new QFrontendStepMetaData()
               .withName("results")
               .withComponent(new NoCodeWidgetFrontendComponentMetaData()
                  .withOutput(new WidgetHtmlLine().withVelocityTemplate("All quartz jobs have been paused")))))
         .withIcon(new QIcon("pause_circle_outline"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      try
      {
         QuartzScheduler.getInstance().pauseAll();
      }
      catch(Exception e)
      {
         throw (new QException("Error pausing all jobs", e));
      }
   }

}
