/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.joins;


/*******************************************************************************
 ** Type for a QJoin.
 **
 ** - One to One - or zero, i guess...
 ** - One to Many - e.g., where the parent record really "owns" all of the child
 **      records.  Like Order -> OrderLine.
 ** - Many to One - e.g., where a child references a parent, but we'd never really
 **      view or manage all of the children under the parent.
 ** - Many to Many - e.g., through an intersection table... ? Needs more thought.
 *******************************************************************************/
public enum JoinType
{
   ONE_TO_ONE,
   ONE_TO_MANY,
   MANY_TO_ONE,
   MANY_TO_MANY;



   /*******************************************************************************
    **
    *******************************************************************************/
   public JoinType flip()
   {
      return switch(this)
      {
         case ONE_TO_MANY -> MANY_TO_ONE;
         case MANY_TO_ONE -> ONE_TO_MANY;
         case MANY_TO_MANY, ONE_TO_ONE -> this;
      };
   }
}
