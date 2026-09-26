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

package com.kingsrook.qqq.api.model;


import java.util.Objects;


/*******************************************************************************
 ** Should work as well for https://semver.org/spec/v2.0.0.html or https://calver.org/
 ** or simple increasing integers, or ?
 *******************************************************************************/
public class APIVersion implements Comparable<APIVersion>
{
   private String   version;
   private String[] parts;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public APIVersion(String version)
   {
      this.version = version;
      this.parts = version.split("[.-]");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public int compareTo(APIVersion that)
   {
      int i = 0;
      while(true)
      {
         if(i >= this.parts.length && i >= that.parts.length)
         {
            return (0);
         }
         else if(i >= this.parts.length)
         {
            return (1); // todo - review
         }
         else if(i >= that.parts.length)
         {
            return (0); // todo - review
         }
         else
         {
            String thisPart = this.parts[i];
            String thatPart = that.parts[i];
            if(!Objects.equals(thisPart, thatPart))
            {
               try
               {
                  Integer thisInt = Integer.parseInt(thisPart);
                  Integer thatInt = Integer.parseInt(thatPart);
                  return (thisInt.compareTo(thatInt));
               }
               catch(Exception e)
               {
                  //////////////////////////////////////////////////////////////////////////
                  // if either doesn't parse as an integer, then compare them as strings. //
                  //////////////////////////////////////////////////////////////////////////
                  return (thisPart.compareTo(thatPart));
               }
            }
         }
         i++;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String toString()
   {
      return (version);
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

      APIVersion that = (APIVersion) o;
      return Objects.equals(version, that.version);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public int hashCode()
   {
      return Objects.hash(version);
   }
}
