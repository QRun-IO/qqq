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

package com.kingsrook.qqq.backend.core.model.actions.reporting;


import java.io.Serializable;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionOutput;


/*******************************************************************************
 ** Output for a Report action
 *******************************************************************************/
public class ReportOutput extends AbstractActionOutput implements Serializable
{
   private Integer totalRecordCount;



   /*******************************************************************************
    ** Getter for totalRecordCount
    *******************************************************************************/
   public Integer getTotalRecordCount()
   {
      return (this.totalRecordCount);
   }



   /*******************************************************************************
    ** Setter for totalRecordCount
    *******************************************************************************/
   public void setTotalRecordCount(Integer totalRecordCount)
   {
      this.totalRecordCount = totalRecordCount;
   }



   /*******************************************************************************
    ** Fluent setter for totalRecordCount
    *******************************************************************************/
   public ReportOutput withTotalRecordCount(Integer totalRecordCount)
   {
      this.totalRecordCount = totalRecordCount;
      return (this);
   }

}
