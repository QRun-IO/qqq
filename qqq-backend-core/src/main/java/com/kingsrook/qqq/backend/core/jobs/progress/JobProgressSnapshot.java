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
import java.time.Instant;
import java.util.List;


/*******************************************************************************
 ** Counts and display text for job progress. Never contains row values.
 *******************************************************************************/
public class JobProgressSnapshot implements Serializable
{
   private List<JobPhase> phases;
   private String currentPhase;
   private String unitLabel;
   private Long done;
   private Long total;
   private Long bytesDone;
   private Long bytesTotal;
   private Double percent;
   private Long etaSeconds;
   private String message;
   private Instant updatedAt;



   /*******************************************************************************
    ** Getter for phases
    *******************************************************************************/
   public List<JobPhase> getPhases()
   {
      return (this.phases);
   }



   /*******************************************************************************
    ** Setter for phases
    *******************************************************************************/
   public void setPhases(List<JobPhase> phases)
   {
      this.phases = phases;
   }



   /*******************************************************************************
    ** Fluent setter for phases
    *******************************************************************************/
   public JobProgressSnapshot withPhases(List<JobPhase> phases)
   {
      this.phases = phases;
      return (this);
   }



   /*******************************************************************************
    ** Getter for currentPhase
    *******************************************************************************/
   public String getCurrentPhase()
   {
      return (this.currentPhase);
   }



   /*******************************************************************************
    ** Setter for currentPhase
    *******************************************************************************/
   public void setCurrentPhase(String currentPhase)
   {
      this.currentPhase = currentPhase;
   }



   /*******************************************************************************
    ** Fluent setter for currentPhase
    *******************************************************************************/
   public JobProgressSnapshot withCurrentPhase(String currentPhase)
   {
      this.currentPhase = currentPhase;
      return (this);
   }



   /*******************************************************************************
    ** Getter for unitLabel
    *******************************************************************************/
   public String getUnitLabel()
   {
      return (this.unitLabel);
   }



   /*******************************************************************************
    ** Setter for unitLabel
    *******************************************************************************/
   public void setUnitLabel(String unitLabel)
   {
      this.unitLabel = unitLabel;
   }



   /*******************************************************************************
    ** Fluent setter for unitLabel
    *******************************************************************************/
   public JobProgressSnapshot withUnitLabel(String unitLabel)
   {
      this.unitLabel = unitLabel;
      return (this);
   }



   /*******************************************************************************
    ** Getter for done
    *******************************************************************************/
   public Long getDone()
   {
      return (this.done);
   }



   /*******************************************************************************
    ** Setter for done
    *******************************************************************************/
   public void setDone(Long done)
   {
      this.done = done;
   }



   /*******************************************************************************
    ** Fluent setter for done
    *******************************************************************************/
   public JobProgressSnapshot withDone(Long done)
   {
      this.done = done;
      return (this);
   }



   /*******************************************************************************
    ** Getter for total
    *******************************************************************************/
   public Long getTotal()
   {
      return (this.total);
   }



   /*******************************************************************************
    ** Setter for total
    *******************************************************************************/
   public void setTotal(Long total)
   {
      this.total = total;
   }



   /*******************************************************************************
    ** Fluent setter for total
    *******************************************************************************/
   public JobProgressSnapshot withTotal(Long total)
   {
      this.total = total;
      return (this);
   }



   /*******************************************************************************
    ** Getter for bytesDone
    *******************************************************************************/
   public Long getBytesDone()
   {
      return (this.bytesDone);
   }



   /*******************************************************************************
    ** Setter for bytesDone
    *******************************************************************************/
   public void setBytesDone(Long bytesDone)
   {
      this.bytesDone = bytesDone;
   }



   /*******************************************************************************
    ** Fluent setter for bytesDone
    *******************************************************************************/
   public JobProgressSnapshot withBytesDone(Long bytesDone)
   {
      this.bytesDone = bytesDone;
      return (this);
   }



   /*******************************************************************************
    ** Getter for bytesTotal
    *******************************************************************************/
   public Long getBytesTotal()
   {
      return (this.bytesTotal);
   }



   /*******************************************************************************
    ** Setter for bytesTotal
    *******************************************************************************/
   public void setBytesTotal(Long bytesTotal)
   {
      this.bytesTotal = bytesTotal;
   }



   /*******************************************************************************
    ** Fluent setter for bytesTotal
    *******************************************************************************/
   public JobProgressSnapshot withBytesTotal(Long bytesTotal)
   {
      this.bytesTotal = bytesTotal;
      return (this);
   }



   /*******************************************************************************
    ** Getter for percent
    *******************************************************************************/
   public Double getPercent()
   {
      return (this.percent);
   }



   /*******************************************************************************
    ** Setter for percent
    *******************************************************************************/
   public void setPercent(Double percent)
   {
      this.percent = percent;
   }



   /*******************************************************************************
    ** Fluent setter for percent
    *******************************************************************************/
   public JobProgressSnapshot withPercent(Double percent)
   {
      this.percent = percent;
      return (this);
   }



   /*******************************************************************************
    ** Getter for etaSeconds
    *******************************************************************************/
   public Long getEtaSeconds()
   {
      return (this.etaSeconds);
   }



   /*******************************************************************************
    ** Setter for etaSeconds
    *******************************************************************************/
   public void setEtaSeconds(Long etaSeconds)
   {
      this.etaSeconds = etaSeconds;
   }



   /*******************************************************************************
    ** Fluent setter for etaSeconds
    *******************************************************************************/
   public JobProgressSnapshot withEtaSeconds(Long etaSeconds)
   {
      this.etaSeconds = etaSeconds;
      return (this);
   }



   /*******************************************************************************
    ** Getter for message
    *******************************************************************************/
   public String getMessage()
   {
      return (this.message);
   }



   /*******************************************************************************
    ** Setter for message
    *******************************************************************************/
   public void setMessage(String message)
   {
      this.message = message;
   }



   /*******************************************************************************
    ** Fluent setter for message
    *******************************************************************************/
   public JobProgressSnapshot withMessage(String message)
   {
      this.message = message;
      return (this);
   }



   /*******************************************************************************
    ** Getter for updatedAt
    *******************************************************************************/
   public Instant getUpdatedAt()
   {
      return (this.updatedAt);
   }



   /*******************************************************************************
    ** Setter for updatedAt
    *******************************************************************************/
   public void setUpdatedAt(Instant updatedAt)
   {
      this.updatedAt = updatedAt;
   }



   /*******************************************************************************
    ** Fluent setter for updatedAt
    *******************************************************************************/
   public JobProgressSnapshot withUpdatedAt(Instant updatedAt)
   {
      this.updatedAt = updatedAt;
      return (this);
   }

}
