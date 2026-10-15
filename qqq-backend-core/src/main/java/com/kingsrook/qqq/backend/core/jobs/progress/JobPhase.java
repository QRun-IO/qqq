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


package com.kingsrook.qqq.backend.core.jobs.progress;


import java.io.Serializable;


/*******************************************************************************
 ** Counts and display text for job progress. Never contains row values.
 *******************************************************************************/
public class JobPhase implements Serializable
{
   private String name;
   private String label;
   private Double weight;
   private Status status;


   /*******************************************************************************
    ** Phase lifecycle within an attempt.
    *******************************************************************************/
   public enum Status
   {
      PENDING, RUNNING, DONE, SKIPPED, FAILED
   }


   /*******************************************************************************
    ** Create a pending weighted phase.
    *******************************************************************************/
   public static JobPhase of(String name, String label, double weight)
   {
      return (new JobPhase().withName(name).withLabel(label).withWeight(weight).withStatus(Status.PENDING));
   }



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
   public JobPhase withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for label
    *******************************************************************************/
   public String getLabel()
   {
      return (this.label);
   }



   /*******************************************************************************
    ** Setter for label
    *******************************************************************************/
   public void setLabel(String label)
   {
      this.label = label;
   }



   /*******************************************************************************
    ** Fluent setter for label
    *******************************************************************************/
   public JobPhase withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Getter for weight
    *******************************************************************************/
   public Double getWeight()
   {
      return (this.weight);
   }



   /*******************************************************************************
    ** Setter for weight
    *******************************************************************************/
   public void setWeight(Double weight)
   {
      this.weight = weight;
   }



   /*******************************************************************************
    ** Fluent setter for weight
    *******************************************************************************/
   public JobPhase withWeight(Double weight)
   {
      this.weight = weight;
      return (this);
   }



   /*******************************************************************************
    ** Getter for status
    *******************************************************************************/
   public Status getStatus()
   {
      return (this.status);
   }



   /*******************************************************************************
    ** Setter for status
    *******************************************************************************/
   public void setStatus(Status status)
   {
      this.status = status;
   }



   /*******************************************************************************
    ** Fluent setter for status
    *******************************************************************************/
   public JobPhase withStatus(Status status)
   {
      this.status = status;
      return (this);
   }

}
