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


/*******************************************************************************
 **
 *******************************************************************************/
public class APIVersionRange
{
   private APIVersion start;
   private Boolean    startInclusive = true;
   private APIVersion end;
   private Boolean    endInclusive   = true;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public APIVersionRange()
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private APIVersionRange(APIVersion start, Boolean startInclusive, APIVersion end, Boolean endInclusive)
   {
      this.start = start;
      this.startInclusive = startInclusive;
      this.end = end;
      this.endInclusive = endInclusive;
   }



   /*******************************************************************************
    ** return a version range that includes no versions.
    *******************************************************************************/
   public static APIVersionRange none()
   {
      return (new APIVersionRange()
      {
         @Override
         public boolean includes(APIVersion apiVersion)
         {
            return (false);
         }
      });
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static APIVersionRange afterAndIncluding(APIVersion start)
   {
      return (new APIVersionRange(start, true, null, null));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static APIVersionRange afterAndIncluding(String start)
   {
      return (afterAndIncluding(new APIVersion(start)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static APIVersionRange afterButExcluding(String start)
   {
      return (afterButExcluding(new APIVersion(start)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static APIVersionRange afterButExcluding(APIVersion start)
   {
      return (new APIVersionRange(start, false, null, null));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static APIVersionRange beforeAndIncluding(APIVersion end)
   {
      return (new APIVersionRange(null, null, end, true));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static APIVersionRange beforeAndIncluding(String end)
   {
      return (beforeAndIncluding(new APIVersion(end)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static APIVersionRange beforeButExcluding(String end)
   {
      return (beforeButExcluding(new APIVersion(end)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static APIVersionRange beforeButExcluding(APIVersion end)
   {
      return (new APIVersionRange(null, null, end, false));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static APIVersionRange betweenAndIncluding(APIVersion start, APIVersion end)
   {
      return (new APIVersionRange(start, true, end, true));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static APIVersionRange betweenAndIncluding(String start, String end)
   {
      return (betweenAndIncluding(new APIVersion(start), new APIVersion(end)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static APIVersionRange betweenButExcluding(String start, String end)
   {
      return (betweenButExcluding(new APIVersion(start), new APIVersion(end)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static APIVersionRange betweenButExcluding(APIVersion start, APIVersion end)
   {
      return (new APIVersionRange(start, false, end, false));
   }



   /*******************************************************************************
    ** Getter for start
    *******************************************************************************/
   public APIVersion getStart()
   {
      return (this.start);
   }



   /*******************************************************************************
    ** Setter for start
    *******************************************************************************/
   public void setStart(APIVersion start)
   {
      this.start = start;
   }



   /*******************************************************************************
    ** Fluent setter for start
    *******************************************************************************/
   public APIVersionRange withStart(APIVersion start)
   {
      this.start = start;
      return (this);
   }



   /*******************************************************************************
    ** Getter for startInclusive
    *******************************************************************************/
   public Boolean getStartInclusive()
   {
      return (this.startInclusive);
   }



   /*******************************************************************************
    ** Setter for startInclusive
    *******************************************************************************/
   public void setStartInclusive(Boolean startInclusive)
   {
      this.startInclusive = startInclusive;
   }



   /*******************************************************************************
    ** Fluent setter for startInclusive
    *******************************************************************************/
   public APIVersionRange withStartInclusive(Boolean startInclusive)
   {
      this.startInclusive = startInclusive;
      return (this);
   }



   /*******************************************************************************
    ** Getter for end
    *******************************************************************************/
   public APIVersion getEnd()
   {
      return (this.end);
   }



   /*******************************************************************************
    ** Setter for end
    *******************************************************************************/
   public void setEnd(APIVersion end)
   {
      this.end = end;
   }



   /*******************************************************************************
    ** Fluent setter for end
    *******************************************************************************/
   public APIVersionRange withEnd(APIVersion end)
   {
      this.end = end;
      return (this);
   }



   /*******************************************************************************
    ** Getter for endInclusive
    *******************************************************************************/
   public Boolean getEndInclusive()
   {
      return (this.endInclusive);
   }



   /*******************************************************************************
    ** Setter for endInclusive
    *******************************************************************************/
   public void setEndInclusive(Boolean endInclusive)
   {
      this.endInclusive = endInclusive;
   }



   /*******************************************************************************
    ** Fluent setter for endInclusive
    *******************************************************************************/
   public APIVersionRange withEndInclusive(Boolean endInclusive)
   {
      this.endInclusive = endInclusive;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public boolean includes(APIVersion apiVersion)
   {
      if(start != null)
      {
         if(startInclusive)
         {
            if(apiVersion.compareTo(start) < 0)
            {
               return (false);
            }
         }
         else
         {
            if(apiVersion.compareTo(start) <= 0)
            {
               return (false);
            }
         }
      }

      if(end != null)
      {
         if(endInclusive)
         {
            if(apiVersion.compareTo(end) > 0)
            {
               return (false);
            }
         }
         else
         {
            if(apiVersion.compareTo(end) >= 0)
            {
               return (false);
            }
         }
      }

      return (true);
   }
}
