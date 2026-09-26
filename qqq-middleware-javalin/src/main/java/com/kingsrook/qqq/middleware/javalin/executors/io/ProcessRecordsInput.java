/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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
 ** Middleware input for fetching records from a process state.
 *******************************************************************************/
public class ProcessRecordsInput extends AbstractMiddlewareInput
{
   private String  processName;
   private String  processUUID;
   private Integer skip  = 0;
   private Integer limit = 20;

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
   public ProcessRecordsInput withProcessName(String processName)
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
   public ProcessRecordsInput withProcessUUID(String processUUID)
   {
      this.processUUID = processUUID;
      return (this);
   }



   /*******************************************************************************
    ** Getter for skip
    *******************************************************************************/
   public Integer getSkip()
   {
      return (this.skip);
   }



   /*******************************************************************************
    ** Setter for skip
    *******************************************************************************/
   public void setSkip(Integer skip)
   {
      this.skip = skip;
   }



   /*******************************************************************************
    ** Fluent setter for skip
    *******************************************************************************/
   public ProcessRecordsInput withSkip(Integer skip)
   {
      this.skip = skip;
      return (this);
   }



   /*******************************************************************************
    ** Getter for limit
    *******************************************************************************/
   public Integer getLimit()
   {
      return (this.limit);
   }



   /*******************************************************************************
    ** Setter for limit
    *******************************************************************************/
   public void setLimit(Integer limit)
   {
      this.limit = limit;
   }



   /*******************************************************************************
    ** Fluent setter for limit
    *******************************************************************************/
   public ProcessRecordsInput withLimit(Integer limit)
   {
      this.limit = limit;
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
   public ProcessRecordsInput withTableVariant(TableVariant tableVariant)
   {
      this.tableVariant = tableVariant;
      return (this);
   }

}
