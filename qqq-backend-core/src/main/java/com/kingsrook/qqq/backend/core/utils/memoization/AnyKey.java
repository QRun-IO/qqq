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

package com.kingsrook.qqq.backend.core.utils.memoization;


/*******************************************************************************
 ** Meant to serve as the key in a Memoization where we actually don't need a parameter.
 ** e.g., to memoize a function like:  public Object f();
 *******************************************************************************/
public class AnyKey
{
   private static AnyKey anyKey = null;



   /*******************************************************************************
    ** Singleton constructor
    *******************************************************************************/
   private AnyKey()
   {

   }



   /*******************************************************************************
    ** Singleton accessor
    *******************************************************************************/
   public static AnyKey getInstance()
   {
      if(anyKey == null)
      {
         anyKey = new AnyKey();
      }
      return (anyKey);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean equals(Object obj)
   {
      return (obj == this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public int hashCode()
   {
      return 1;
   }
}
