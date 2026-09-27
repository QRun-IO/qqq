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

package com.kingsrook.qqq.backend.core.utils.memoization;


import java.time.Instant;


/*******************************************************************************
 ** Object stored in the Memoization class.  Shouldn't need to be visible outside
 ** its package.
 *******************************************************************************/
public class MemoizedResult<T>
{
   private T       result;
   private Instant time;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public MemoizedResult(T result)
   {
      this.result = result;
      this.time = Instant.now();
   }



   /*******************************************************************************
    ** Getter for result
    **
    *******************************************************************************/
   public T getResult()
   {
      return result;
   }



   /*******************************************************************************
    ** Getter for time
    **
    *******************************************************************************/
   public Instant getTime()
   {
      return time;
   }
}
