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


/*******************************************************************************
 ** Unique identifier for a thing that can be scheduled
 *******************************************************************************/
public interface SchedulableIdentity
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   boolean equals(Object that);


   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   int hashCode();


   /*******************************************************************************
    **
    *******************************************************************************/
   String getIdentity();


   /*******************************************************************************
    ** should NOT be part of equals & has code
    *******************************************************************************/
   String getDescription();

}
