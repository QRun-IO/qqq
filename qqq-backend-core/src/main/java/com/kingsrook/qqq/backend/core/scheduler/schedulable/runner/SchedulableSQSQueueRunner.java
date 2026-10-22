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

package com.kingsrook.qqq.backend.core.scheduler.schedulable.runner;


import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.queues.SQSQueuePoller;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.queues.QQueueMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.queues.QQueueProviderMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.queues.SQSQueueProviderMetaData;
import com.kingsrook.qqq.backend.core.scheduler.quartz.QuartzScheduler;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.identity.SchedulableIdentity;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Schedulable SQSQueue runner - e.g., how an SQSQueuePoller is run by a scheduler.
 *******************************************************************************/
public class SchedulableSQSQueueRunner implements SchedulableRunner
{
   private static final QLogger LOG = QLogger.getLogger(SchedulableSQSQueueRunner.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void run(Map<String, Object> params)
   {
      QInstance qInstance = QuartzScheduler.getInstance().getQInstance();

      String queueName = ValueUtils.getValueAsString(params.get("queueName"));
      if(!StringUtils.hasContent(queueName))
      {
         LOG.warn("Missing queueName in params.");
         return;
      }

      QQueueMetaData queue = qInstance.getQueue(queueName);
      if(queue == null)
      {
         LOG.warn("Unrecognized queueName [" + queueName + "]");
         return;
      }

      QQueueProviderMetaData queueProvider = qInstance.getQueueProvider(queue.getProviderName());
      if(!(queueProvider instanceof SQSQueueProviderMetaData))
      {
         LOG.warn("Queue [" + queueName + "] is of an unsupported queue provider type (not SQS)");
         return;
      }

      SQSQueuePoller sqsQueuePoller = new SQSQueuePoller();
      sqsQueuePoller.setQueueMetaData(queue);
      sqsQueuePoller.setQueueProviderMetaData((SQSQueueProviderMetaData) queueProvider);
      sqsQueuePoller.setQInstance(qInstance);
      sqsQueuePoller.setSessionSupplier(QuartzScheduler.getInstance().getSessionSupplier());

      /////////////
      // run it. //
      /////////////
      LOG.debug("Running SQS Queue poller", logPair("queueName", queueName));
      sqsQueuePoller.run();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void validateParams(SchedulableIdentity schedulableIdentity, Map<String, Object> paramMap) throws QException
   {
      String queueName = ValueUtils.getValueAsString(paramMap.get("queueName"));
      if(!StringUtils.hasContent(queueName))
      {
         throw (new QException("Missing scheduledJobParameter with key [queueName] in " + schedulableIdentity));
      }

      QQueueMetaData queue = QContext.getQInstance().getQueue(queueName);
      if(queue == null)
      {
         throw (new QException("Unrecognized queueName [" + queueName + "] in " + schedulableIdentity));
      }

      QQueueProviderMetaData queueProvider = QContext.getQInstance().getQueueProvider(queue.getProviderName());
      if(!(queueProvider instanceof SQSQueueProviderMetaData))
      {
         throw (new QException("Queue [" + queueName + "] is of an unsupported queue provider type (not SQS) in " + schedulableIdentity));
      }

      if(queue.getSchedule() != null)
      {
         throw (new QException("Queue [" + queueName + "] has a schedule in its metaData - so it should not be dynamically scheduled via a scheduled job! " + schedulableIdentity));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getDescription(Map<String, Object> params)
   {
      return "Queue: " + params.get("queueName");
   }

}
