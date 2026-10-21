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

package com.kingsrook.qqq.backend.core.actions.interfaces;


import java.time.Instant;
import com.kingsrook.qqq.backend.core.model.querystats.QueryStat;


/*******************************************************************************
 ** Base class for "query" (e.g., read-operations) action interfaces (query, count, aggregate).
 ** Initially just here for the QueryStat methods - if we expand those to apply
 ** to insert/update/delete, well, then rename this maybe to BaseActionInterface?
 *******************************************************************************/
public interface BaseQueryInterface
{

   /*******************************************************************************
    **
    *******************************************************************************/
   default void setQueryStat(QueryStat queryStat)
   {
      //////////
      // noop //
      //////////
   }

   /*******************************************************************************
    **
    *******************************************************************************/
   default QueryStat getQueryStat()
   {
      return (null);
   }

   /*******************************************************************************
    **
    *******************************************************************************/
   default void setQueryStatFirstResultTime()
   {
      QueryStat queryStat = getQueryStat();
      if(queryStat != null)
      {
         if(queryStat.getFirstResultTimestamp() == null)
         {
            queryStat.setFirstResultTimestamp(Instant.now());
         }
      }
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   default void cancelAction()
   {
      //////////////////////////////////////////////
      // initially at least, a noop in base class //
      //////////////////////////////////////////////
   }

}
