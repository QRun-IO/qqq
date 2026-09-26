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

package com.kingsrook.qqq.middleware.javalin.executors.io;


import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.TableVariant;


/*******************************************************************************
 **
 *******************************************************************************/
public class ProcessStatusInput extends AbstractMiddlewareInput
{
   private String processName;
   private String processUUID;
   private String jobUUID;

   private TableVariant tableVariant;



   /*******************************************************************************
    ** Getter for processName
    *******************************************************************************/
   public String getProcessName()
   {
      return (this.processName);
   }



   /*******************************************************************************
    ** Setter for processName
    *******************************************************************************/
   public void setProcessName(String processName)
   {
      this.processName = processName;
   }



   /*******************************************************************************
    ** Fluent setter for processName
    *******************************************************************************/
   public ProcessStatusInput withProcessName(String processName)
   {
      this.processName = processName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for processUUID
    *******************************************************************************/
   public String getProcessUUID()
   {
      return (this.processUUID);
   }



   /*******************************************************************************
    ** Setter for processUUID
    *******************************************************************************/
   public void setProcessUUID(String processUUID)
   {
      this.processUUID = processUUID;
   }



   /*******************************************************************************
    ** Fluent setter for processUUID
    *******************************************************************************/
   public ProcessStatusInput withProcessUUID(String processUUID)
   {
      this.processUUID = processUUID;
      return (this);
   }



   /*******************************************************************************
    ** Getter for jobUUID
    *******************************************************************************/
   public String getJobUUID()
   {
      return (this.jobUUID);
   }



   /*******************************************************************************
    ** Setter for jobUUID
    *******************************************************************************/
   public void setJobUUID(String jobUUID)
   {
      this.jobUUID = jobUUID;
   }



   /*******************************************************************************
    ** Fluent setter for jobUUID
    *******************************************************************************/
   public ProcessStatusInput withJobUUID(String jobUUID)
   {
      this.jobUUID = jobUUID;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableVariant
    *******************************************************************************/
   public TableVariant getTableVariant()
   {
      return (this.tableVariant);
   }



   /*******************************************************************************
    ** Setter for tableVariant
    *******************************************************************************/
   public void setTableVariant(TableVariant tableVariant)
   {
      this.tableVariant = tableVariant;
   }



   /*******************************************************************************
    ** Fluent setter for tableVariant
    *******************************************************************************/
   public ProcessStatusInput withTableVariant(TableVariant tableVariant)
   {
      this.tableVariant = tableVariant;
      return (this);
   }

}
