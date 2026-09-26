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


import java.io.Serializable;
import java.time.Instant;


/*******************************************************************************
 **
 *******************************************************************************/
public abstract class AbstractStateKey implements Serializable
{
   /*******************************************************************************
    ** Make the key give a unique string to identify itself.
    *
    *******************************************************************************/
   public abstract String getUniqueIdentifier();

   /*******************************************************************************
    ** Require all state keys to implement the equals method
    *
    *******************************************************************************/
   @Override
   public abstract boolean equals(Object that);

   /*******************************************************************************
    ** Require all state keys to implement the hashCode method
    *
    *******************************************************************************/
   @Override
   public abstract int hashCode();

   /*******************************************************************************
    ** Require all state keys to implement the toString method
    *
    *******************************************************************************/
   @Override
   public abstract String toString();

   /*******************************************************************************
    ** Require all state keys to implement the getStartTime method
    *
    *******************************************************************************/
   public abstract Instant getStartTime();

}
