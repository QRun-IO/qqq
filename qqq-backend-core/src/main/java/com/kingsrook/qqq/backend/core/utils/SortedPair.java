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

package com.kingsrook.qqq.backend.core.utils;


/*******************************************************************************
 **
 *******************************************************************************/
public class SortedPair<A extends Comparable<A>> extends Pair<A, A>
{

   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public SortedPair(A a, A b)
   {
      super(compare(a, b) <= 0 ? a : b, compare(a, b) <= 0 ? b : a);
   }


   /***************************************************************************
    * do a null-safe compare that the constructor can use.
    ***************************************************************************/
   private static <A extends Comparable<A>> int compare(A a, A b)
   {
      if(a == null && b == null)
      {
         return (0);
      }
      else if(a == null)
      {
         return (-1);
      }
      else if(b == null)
      {
         return (1);
      }
      else
      {
         return (a.compareTo(b));
      }
   }

}
