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


import com.kingsrook.qqq.middleware.javalin.executors.io.ProcessJobLaunchOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;


/*******************************************************************************
 ** Response of POST /processes/{processName}/jobs.
 *******************************************************************************/
public class ProcessJobLaunchResponseV1 implements ProcessJobLaunchOutputInterface, ToSchema
{
   @OpenAPIDescription("Id of the new durable job (a jobs_job record; read it with GET /table/jobs_job/{jobId}).")
   private Integer jobId;

   @OpenAPIDescription("Status of the new job: QUEUED, or AWAITING_CONFIRMATION for a launch from the assistant channel.")
   private String status;



   /*******************************************************************************
    ** Getter for jobId
    *******************************************************************************/
   public Integer getJobId()
   {
      return (this.jobId);
   }



   /*******************************************************************************
    ** Setter for jobId
    *******************************************************************************/
   @Override
   public void setJobId(Integer jobId)
   {
      this.jobId = jobId;
   }



   /*******************************************************************************
    ** Fluent setter for jobId
    *******************************************************************************/
   public ProcessJobLaunchResponseV1 withJobId(Integer jobId)
   {
      this.jobId = jobId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for status
    *******************************************************************************/
   public String getStatus()
   {
      return (this.status);
   }



   /*******************************************************************************
    ** Setter for status
    *******************************************************************************/
   @Override
   public void setStatus(String status)
   {
      this.status = status;
   }



   /*******************************************************************************
    ** Fluent setter for status
    *******************************************************************************/
   public ProcessJobLaunchResponseV1 withStatus(String status)
   {
      this.status = status;
      return (this);
   }

}
