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


import java.util.Objects;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Basic implementation of interface for identifying schedulable things
 *******************************************************************************/
public class BasicSchedulableIdentity implements SchedulableIdentity
{
   private String identity;
   private String description;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public BasicSchedulableIdentity(String identity, String description)
   {
      if(!StringUtils.hasContent(identity))
      {
         throw (new IllegalArgumentException("Identity may not be null or empty."));
      }

      this.identity = identity;
      this.description = description;
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

      BasicSchedulableIdentity that = (BasicSchedulableIdentity) o;
      return Objects.equals(identity, that.identity);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public int hashCode()
   {
      return Objects.hash(identity);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getIdentity()
   {
      return identity;
   }



   /*******************************************************************************
    ** Getter for description
    **
    *******************************************************************************/
   @Override
   public String getDescription()
   {
      return description;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String toString()
   {
      return getIdentity();
   }

}
