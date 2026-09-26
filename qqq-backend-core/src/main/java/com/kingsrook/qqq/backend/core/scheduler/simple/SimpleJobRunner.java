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

package com.kingsrook.qqq.backend.core.scheduler.simple;


import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.SchedulableType;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.runner.SchedulableRunner;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 **
 *******************************************************************************/
public class SimpleJobRunner implements Runnable
{
   private static final QLogger LOG = QLogger.getLogger(SimpleJobRunner.class);

   private QInstance           qInstance;
   private SchedulableType     schedulableType;
   private Map<String, Object> params;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public SimpleJobRunner(QInstance qInstance, SchedulableType type, Map<String, Object> params)
   {
      this.qInstance = qInstance;
      this.schedulableType = type;
      this.params = params;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void run()
   {
      CapturedContext capturedContext = QContext.capture();
      try
      {
         SimpleScheduler simpleScheduler = SimpleScheduler.getInstance(qInstance);
         QContext.init(qInstance, simpleScheduler.getSessionSupplier().get());

         SchedulableRunner schedulableRunner = QCodeLoader.getAdHoc(SchedulableRunner.class, schedulableType.getRunner());
         schedulableRunner.run(params);
      }
      catch(Exception e)
      {
         LOG.warn("Error running SimpleScheduler job", e, logPair("params", params));
      }
      finally
      {
         QContext.init(capturedContext);
      }
   }

}
