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

package com.kingsrook.qqq.backend.core.scheduler;


import java.io.Serializable;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.QScheduleMetaData;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.SchedulableType;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.identity.BasicSchedulableIdentity;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.identity.SchedulableIdentity;


/*******************************************************************************
 **
 *******************************************************************************/
public interface QSchedulerInterface
{

   /*******************************************************************************
    **
    *******************************************************************************/
   String getSchedulerName();

   /*******************************************************************************
    **
    *******************************************************************************/
   void start();

   /*******************************************************************************
    ** called to indicate that the schedule manager is past its startup routine,
    ** but that the schedule should not actually be running in this process.
    *******************************************************************************/
   default void doNotStart()
   {

   }

   /*******************************************************************************
    **
    *******************************************************************************/
   void setupSchedulable(SchedulableIdentity schedulableIdentity, SchedulableType schedulableType, Map<String, Serializable> parameters, QScheduleMetaData schedule, boolean allowedToStart);

   /*******************************************************************************
    **
    *******************************************************************************/
   void unscheduleSchedulable(SchedulableIdentity schedulableIdentity, SchedulableType schedulableType);

   /*******************************************************************************
    **
    *******************************************************************************/
   boolean isScheduled(BasicSchedulableIdentity schedulableIdentity, SchedulableType schedulableType);

   /*******************************************************************************
    **
    *******************************************************************************/
   void unscheduleAll() throws QException;

   /*******************************************************************************
    **
    *******************************************************************************/
   void stopAsync();

   /*******************************************************************************
    **
    *******************************************************************************/
   void stop();

   /*******************************************************************************
    ** Handle a whole shutdown of the scheduler system (e.g., between unit tests).
    *******************************************************************************/
   default void unInit()
   {
      /////////////////////
      // noop by default //
      /////////////////////
   }

   /*******************************************************************************
    ** let the scheduler know when the schedule manager is at the start of setting up schedules.
    *******************************************************************************/
   default void startOfSetupSchedules()
   {
      /////////////////////
      // noop by default //
      /////////////////////
   }

   /*******************************************************************************
    ** let the scheduler know when the schedule manager is at the end of setting up schedules.
    *******************************************************************************/
   default void endOfSetupSchedules()
   {
      /////////////////////
      // noop by default //
      /////////////////////
   }

}
