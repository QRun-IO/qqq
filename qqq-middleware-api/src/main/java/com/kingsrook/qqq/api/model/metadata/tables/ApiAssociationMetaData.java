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

package com.kingsrook.qqq.api.model.metadata.tables;


import com.kingsrook.qqq.api.model.APIVersionRange;


/*******************************************************************************
 **
 *******************************************************************************/
public class ApiAssociationMetaData implements Cloneable
{
   private String  initialVersion;
   private String  finalVersion;
   private Boolean isExcluded;



   /*******************************************************************************
    **
    *******************************************************************************/
   public APIVersionRange getApiVersionRange()
   {
      if(getInitialVersion() == null)
      {
         return APIVersionRange.none();
      }

      return (getFinalVersion() != null
         ? APIVersionRange.betweenAndIncluding(getInitialVersion(), getFinalVersion())
         : APIVersionRange.afterAndIncluding(getInitialVersion()));
   }



   /*******************************************************************************
    ** Getter for initialVersion
    *******************************************************************************/
   public String getInitialVersion()
   {
      return (this.initialVersion);
   }



   /*******************************************************************************
    ** Setter for initialVersion
    *******************************************************************************/
   public void setInitialVersion(String initialVersion)
   {
      this.initialVersion = initialVersion;
   }



   /*******************************************************************************
    ** Fluent setter for initialVersion
    *******************************************************************************/
   public ApiAssociationMetaData withInitialVersion(String initialVersion)
   {
      this.initialVersion = initialVersion;
      return (this);
   }



   /*******************************************************************************
    ** Getter for finalVersion
    *******************************************************************************/
   public String getFinalVersion()
   {
      return (this.finalVersion);
   }



   /*******************************************************************************
    ** Setter for finalVersion
    *******************************************************************************/
   public void setFinalVersion(String finalVersion)
   {
      this.finalVersion = finalVersion;
   }



   /*******************************************************************************
    ** Fluent setter for finalVersion
    *******************************************************************************/
   public ApiAssociationMetaData withFinalVersion(String finalVersion)
   {
      this.finalVersion = finalVersion;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isExcluded
    *******************************************************************************/
   public Boolean getIsExcluded()
   {
      return (this.isExcluded);
   }



   /*******************************************************************************
    ** Setter for isExcluded
    *******************************************************************************/
   public void setIsExcluded(Boolean isExcluded)
   {
      this.isExcluded = isExcluded;
   }



   /*******************************************************************************
    ** Fluent setter for isExcluded
    *******************************************************************************/
   public ApiAssociationMetaData withIsExcluded(Boolean isExcluded)
   {
      this.isExcluded = isExcluded;
      return (this);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public ApiAssociationMetaData clone()
   {
      try
      {
         ApiAssociationMetaData clone = (ApiAssociationMetaData) super.clone();
         return clone;
      }
      catch(CloneNotSupportedException e)
      {
         throw new AssertionError();
      }
   }
}
