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

package com.kingsrook.qqq.backend.core.scheduler.schedulable.identity;


import java.util.HashMap;
import com.kingsrook.qqq.backend.core.actions.automation.polling.PollingAutomationPerTableRunner;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.queues.QQueueMetaData;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJob;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.SchedulableType;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.runner.SchedulableRunner;


/*******************************************************************************
 ** Factory to produce SchedulableIdentity objects
 *******************************************************************************/
public class SchedulableIdentityFactory
{

   /*******************************************************************************
    ** Factory to create one of these for a scheduled job record
    *******************************************************************************/
   public static BasicSchedulableIdentity of(ScheduledJob scheduledJob)
   {
      String          description     = "";
      SchedulableType schedulableType = QContext.getQInstance().getSchedulableType(scheduledJob.getType());
      if(schedulableType != null)
      {
         try
         {
            SchedulableRunner runner = QCodeLoader.getAdHoc(SchedulableRunner.class, schedulableType.getRunner());
            description = runner.getDescription(new HashMap<>(scheduledJob.getJobParametersMap()));
         }
         catch(Exception e)
         {
            description = "type: " + schedulableType.getName();
         }
      }

      return new BasicSchedulableIdentity("scheduledJob:" + scheduledJob.getId(), description);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static BasicSchedulableIdentity of(QProcessMetaData process)
   {
      return new BasicSchedulableIdentity("process:" + process.getName(), "Process: " + process.getName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static SchedulableIdentity of(QQueueMetaData queue)
   {
      return new BasicSchedulableIdentity("queue:" + queue.getName(), "Queue: " + queue.getName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static SchedulableIdentity of(PollingAutomationPerTableRunner.TableActionsInterface tableActions)
   {
      return new BasicSchedulableIdentity("tableAutomations:" + tableActions.tableName() + "." + tableActions.status(), "TableAutomations: " + tableActions.tableName() + "." + tableActions.status());
   }
}
