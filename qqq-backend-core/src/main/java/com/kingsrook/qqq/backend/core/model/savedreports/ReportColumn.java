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

package com.kingsrook.qqq.backend.core.model.savedreports;


import java.io.Serializable;


/*******************************************************************************
 ** single entry in ReportColumns object - as part of SavedReport
 *******************************************************************************/
public class ReportColumn implements Serializable
{
   private String  name;
   private Boolean isVisible;



   /*******************************************************************************
    ** Getter for name
    *******************************************************************************/
   public String getName()
   {
      return (this.name);
   }



   /*******************************************************************************
    ** Setter for name
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Fluent setter for name
    *******************************************************************************/
   public ReportColumn withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isVisible
    *******************************************************************************/
   public Boolean getIsVisible()
   {
      return (this.isVisible);
   }



   /*******************************************************************************
    ** Setter for isVisible
    *******************************************************************************/
   public void setIsVisible(Boolean isVisible)
   {
      this.isVisible = isVisible;
   }



   /*******************************************************************************
    ** Fluent setter for isVisible
    *******************************************************************************/
   public ReportColumn withIsVisible(Boolean isVisible)
   {
      this.isVisible = isVisible;
      return (this);
   }

}
