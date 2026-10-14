/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess;




/*******************************************************************************
 ** Presentation rules for a restricted value.
 *******************************************************************************/
public enum FieldMaskStyle
{
   AUTO, EMAIL, LAST_4, FIRST_LAST, FULL;



   /*******************************************************************************
    ** FULL > partial styles > AUTO. Different partial styles are incomparable:
    ** use FULL rather than arbitrarily exposing one policy's protected characters.
    ** Null is the unspecified (AUTO) style.
    *******************************************************************************/
   public static FieldMaskStyle mostRestrictive(FieldMaskStyle first, FieldMaskStyle second)
   {
      first = first == null ? AUTO : first;
      second = second == null ? AUTO : second;
      if(first == second || second == AUTO)
      {
         return (first);
      }
      if(first == AUTO)
      {
         return (second);
      }
      return (FULL);
   }
}
