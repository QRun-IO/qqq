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

package com.kingsrook.qqq.backend.core.model.scheduledjobs;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.values.QCustomPossibleValueProvider;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceInput;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.QSchedulerMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class SchedulersPossibleValueSource implements QCustomPossibleValueProvider<String>
{
   public static final String NAME = "schedulers";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QPossibleValue<String> getPossibleValue(Serializable idValue)
   {
      QSchedulerMetaData scheduler = QContext.getQInstance().getScheduler(String.valueOf(idValue));
      if(scheduler != null)
      {
         return schedulerToPossibleValue(scheduler);
      }

      return null;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QPossibleValue<String>> search(SearchPossibleValueSourceInput input) throws QException
   {
      List<QPossibleValue<String>> rs = new ArrayList<>();
      for(QSchedulerMetaData scheduler : CollectionUtils.nonNullMap(QContext.getQInstance().getSchedulers()).values())
      {
         if(scheduler.mayUseInScheduledJobsTable())
         {
            rs.add(schedulerToPossibleValue(scheduler));
         }
      }
      return rs;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QPossibleValue<String> schedulerToPossibleValue(QSchedulerMetaData scheduler)
   {
      return new QPossibleValue<>(scheduler.getName(), scheduler.getName());
   }

}
