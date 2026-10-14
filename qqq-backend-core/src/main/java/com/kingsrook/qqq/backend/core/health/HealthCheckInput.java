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

package com.kingsrook.qqq.backend.core.health;


import java.time.Instant;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;


/*******************************************************************************
 ** HealthCheckInput
 *******************************************************************************/
public class HealthCheckInput
{
   private QInstance qInstance;
   private Instant deadline;
   private boolean onDemand;
   private Integer attempt = 1;



   /*******************************************************************************
    ** Getter for qInstance
    *******************************************************************************/
   public QInstance getQInstance()
   {
      return (qInstance);
   }



   /*******************************************************************************
    ** Setter for qInstance
    *******************************************************************************/
   public void setQInstance(QInstance qInstance)
   {
      this.qInstance = qInstance;
   }



   /*******************************************************************************
    ** Fluent setter for qInstance
    *******************************************************************************/
   public HealthCheckInput withQInstance(QInstance qInstance)
   {
      setQInstance(qInstance);
      return (this);
   }



   /*******************************************************************************
    ** Getter for deadline
    *******************************************************************************/
   public Instant getDeadline()
   {
      return (deadline);
   }



   /*******************************************************************************
    ** Setter for deadline
    *******************************************************************************/
   public void setDeadline(Instant deadline)
   {
      this.deadline = deadline;
   }



   /*******************************************************************************
    ** Fluent setter for deadline
    *******************************************************************************/
   public HealthCheckInput withDeadline(Instant deadline)
   {
      setDeadline(deadline);
      return (this);
   }



   /*******************************************************************************
    ** Getter for onDemand
    *******************************************************************************/
   public boolean getOnDemand()
   {
      return (onDemand);
   }



   /*******************************************************************************
    ** Setter for onDemand
    *******************************************************************************/
   public void setOnDemand(boolean onDemand)
   {
      this.onDemand = onDemand;
   }



   /*******************************************************************************
    ** Fluent setter for onDemand
    *******************************************************************************/
   public HealthCheckInput withOnDemand(boolean onDemand)
   {
      setOnDemand(onDemand);
      return (this);
   }



   /*******************************************************************************
    ** Getter for attempt
    *******************************************************************************/
   public Integer getAttempt()
   {
      return (attempt);
   }



   /*******************************************************************************
    ** Setter for attempt
    *******************************************************************************/
   public void setAttempt(Integer attempt)
   {
      this.attempt = attempt;
   }



   /*******************************************************************************
    ** Fluent setter for attempt
    *******************************************************************************/
   public HealthCheckInput withAttempt(Integer attempt)
   {
      setAttempt(attempt);
      return (this);
   }
}
