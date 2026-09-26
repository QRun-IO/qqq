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

package com.kingsrook.qqq.backend.core.state;


import java.time.Instant;


/*******************************************************************************
 **
 *******************************************************************************/
public class SimpleStateKey<T> extends AbstractStateKey
{
   private final T key;



   /*******************************************************************************
    ** Constructor.
    **
    *******************************************************************************/
   public SimpleStateKey(T key)
   {
      this.key = key;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String toString()
   {
      return (String.valueOf(this.key));
   }



   /*******************************************************************************
    ** Make the key give a unique string to identify itself.
    *
    *******************************************************************************/
   @Override
   public String getUniqueIdentifier()
   {
      return (String.valueOf(this.key));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean equals(Object o)
   {
      if(this == o)
      {
         return true;
      }
      if(o == null || getClass() != o.getClass())
      {
         return false;
      }
      SimpleStateKey<?> that = (SimpleStateKey<?>) o;
      return key.equals(that.key);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public int hashCode()
   {
      return key.hashCode();
   }



   /*******************************************************************************
    ** Getter for startTime
    *******************************************************************************/
   public Instant getStartTime()
   {
      //////////////////////////////////////////
      // For now these will never get cleaned //
      //////////////////////////////////////////
      return (Instant.now());
   }
}
