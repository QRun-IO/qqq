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
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;


/*******************************************************************************
 ** Jobs UI API v1 representation.
 *******************************************************************************/
public class JobListResponseV1 implements ToSchema
{
   @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
   @OpenAPIListItems(value = JobSummaryV1.class, useRef = true)
   private List<JobSummaryV1> jobs;
   private Integer activeCount;
   private String generatedAt;



   /*******************************************************************************
    ** Getter for jobs
    *******************************************************************************/
   public List<JobSummaryV1> getJobs()
   {
      return (this.jobs);
   }



   /*******************************************************************************
    ** Setter for jobs
    *******************************************************************************/
   public void setJobs(List<JobSummaryV1> jobs)
   {
      this.jobs = jobs;
   }



   /*******************************************************************************
    ** Fluent setter for jobs
    *******************************************************************************/
   public JobListResponseV1 withJobs(List<JobSummaryV1> jobs)
   {
      this.jobs = jobs;
      return (this);
   }



   /*******************************************************************************
    ** Getter for activeCount
    *******************************************************************************/
   public Integer getActiveCount()
   {
      return (this.activeCount);
   }



   /*******************************************************************************
    ** Setter for activeCount
    *******************************************************************************/
   public void setActiveCount(Integer activeCount)
   {
      this.activeCount = activeCount;
   }



   /*******************************************************************************
    ** Fluent setter for activeCount
    *******************************************************************************/
   public JobListResponseV1 withActiveCount(Integer activeCount)
   {
      this.activeCount = activeCount;
      return (this);
   }



   /*******************************************************************************
    ** Getter for generatedAt
    *******************************************************************************/
   public String getGeneratedAt()
   {
      return (this.generatedAt);
   }



   /*******************************************************************************
    ** Setter for generatedAt
    *******************************************************************************/
   public void setGeneratedAt(String generatedAt)
   {
      this.generatedAt = generatedAt;
   }



   /*******************************************************************************
    ** Fluent setter for generatedAt
    *******************************************************************************/
   public JobListResponseV1 withGeneratedAt(String generatedAt)
   {
      this.generatedAt = generatedAt;
      return (this);
   }

}

