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

package com.kingsrook.qqq.backend.core.scheduler.quartz;


import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.SchedulableType;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.runner.SchedulableRunner;
import org.apache.logging.log4j.Level;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 **
 *******************************************************************************/
@DisallowConcurrentExecution
public class QuartzJobRunner implements Job
{
   private static final QLogger LOG = QLogger.getLogger(QuartzJobRunner.class);

   private static Level logLevel = null;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void execute(JobExecutionContext context) throws JobExecutionException
   {
      CapturedContext capturedContext = QContext.capture();

      String              name            = null;
      SchedulableType     schedulableType = null;
      Map<String, Object> params          = null;
      try
      {
         name = context.getJobDetail().getKey().getName();

         QuartzScheduler quartzScheduler = QuartzScheduler.getInstance();
         QInstance       qInstance       = quartzScheduler.getQInstance();
         QContext.init(qInstance, quartzScheduler.getSessionSupplier().get());

         schedulableType = qInstance.getSchedulableType(context.getJobDetail().getJobDataMap().getString("type"));
         @SuppressWarnings("unchecked")
         Map<String, Object> paramsFromJobDataMap = (Map<String, Object>) context.getJobDetail().getJobDataMap().get("params");
         params = paramsFromJobDataMap;

         SchedulableRunner schedulableRunner = QCodeLoader.getAdHoc(SchedulableRunner.class, schedulableType.getRunner());

         if(logLevel != null)
         {
            LOG.log(logLevel, "Running QuartzJob", null, logPair("name", name), logPair("type", schedulableType.getName()), logPair("params", params));
         }

         schedulableRunner.run(params);

         if(logLevel != null)
         {
            LOG.log(logLevel, "Finished QuartzJob", null, logPair("name", name), logPair("type", schedulableType.getName()), logPair("params", params));
         }
      }
      catch(Exception e)
      {
         LOG.warn("Error running QuartzJob", e, logPair("name", name), logPair("type", schedulableType == null ? null : schedulableType.getName()), logPair("params", params));
      }
      finally
      {
         QContext.init(capturedContext);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void setLogLevel(Level level)
   {
      logLevel = level;
   }

}
