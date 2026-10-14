/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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


package com.kingsrook.qqq.middleware.javalin.specs.v1.responses;


import java.util.List;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIIncludeProperties;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;


/*******************************************************************************
 ** Jobs UI API v1 representation.
 *******************************************************************************/
@OpenAPIIncludeProperties(ancestorClasses = { JobSummaryV1.class })
public class JobDetailV1 extends JobSummaryV1 implements ToSchema
{
   private JobProgressV1 progress;
   private Object summary;
   @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
   @OpenAPIListItems(value = String.class, useRef = false)
   private List<String> actions;



   /*******************************************************************************
    ** Getter for progress
    *******************************************************************************/
   public JobProgressV1 getProgress()
   {
      return (this.progress);
   }



   /*******************************************************************************
    ** Setter for progress
    *******************************************************************************/
   public void setProgress(JobProgressV1 progress)
   {
      this.progress = progress;
   }



   /*******************************************************************************
    ** Fluent setter for progress
    *******************************************************************************/
   public JobDetailV1 withProgress(JobProgressV1 progress)
   {
      this.progress = progress;
      return (this);
   }



   /*******************************************************************************
    ** Getter for summary
    *******************************************************************************/
   public Object getSummary()
   {
      return (this.summary);
   }



   /*******************************************************************************
    ** Setter for summary
    *******************************************************************************/
   public void setSummary(Object summary)
   {
      this.summary = summary;
   }



   /*******************************************************************************
    ** Fluent setter for summary
    *******************************************************************************/
   public JobDetailV1 withSummary(Object summary)
   {
      this.summary = summary;
      return (this);
   }



   /*******************************************************************************
    ** Getter for actions
    *******************************************************************************/
   public List<String> getActions()
   {
      return (this.actions);
   }



   /*******************************************************************************
    ** Setter for actions
    *******************************************************************************/
   public void setActions(List<String> actions)
   {
      this.actions = actions;
   }



   /*******************************************************************************
    ** Fluent setter for actions
    *******************************************************************************/
   public JobDetailV1 withActions(List<String> actions)
   {
      this.actions = actions;
      return (this);
   }

}

