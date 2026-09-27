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

package com.kingsrook.qqq.backend.core.model.metadata.scheduleing.quartz;


import java.util.Properties;
import java.util.function.Supplier;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.QSchedulerMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.scheduler.QSchedulerInterface;
import com.kingsrook.qqq.backend.core.scheduler.quartz.QuartzScheduler;


/*******************************************************************************
 **
 *******************************************************************************/
public class QuartzSchedulerMetaData extends QSchedulerMetaData
{
   private static final QLogger LOG = QLogger.getLogger(QuartzSchedulerMetaData.class);

   public static final String TYPE = "quartz";

   private Properties properties;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public QuartzSchedulerMetaData()
   {
      setType(TYPE);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean supportsCronSchedules()
   {
      return (true);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public boolean mayUseInScheduledJobsTable()
   {
      return (true);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QSchedulerInterface initSchedulerInstance(QInstance qInstance, Supplier<QSession> systemSessionSupplier) throws QException
   {
      try
      {
         QuartzScheduler quartzScheduler = QuartzScheduler.initInstance(qInstance, getName(), getProperties(), systemSessionSupplier);
         return (quartzScheduler);
      }
      catch(Exception e)
      {
         LOG.error("Error initializing quartz scheduler", e);
         throw (new QException("Error initializing quartz scheduler", e));
      }
   }



   /*******************************************************************************
    ** Getter for properties
    *******************************************************************************/
   public Properties getProperties()
   {
      return (this.properties);
   }



   /*******************************************************************************
    ** Setter for properties
    *******************************************************************************/
   public void setProperties(Properties properties)
   {
      this.properties = properties;
   }



   /*******************************************************************************
    ** Fluent setter for properties
    *******************************************************************************/
   public QuartzSchedulerMetaData withProperties(Properties properties)
   {
      this.properties = properties;
      return (this);
   }

}
