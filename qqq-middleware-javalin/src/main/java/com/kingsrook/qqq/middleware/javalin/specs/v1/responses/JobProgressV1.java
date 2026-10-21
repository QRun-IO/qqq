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
public class JobProgressV1 implements ToSchema
{
   @OpenAPIListItems(value = JobPhaseV1.class, useRef = true)
   private List<JobPhaseV1> phases;
   private String currentPhase;
   private String unitLabel;
   private Long done;
   private Long total;
   private Long bytesDone;
   private Long bytesTotal;
   private Double percent;
   private Long etaSeconds;
   private String message;
   private String updatedAt;



   /*******************************************************************************
    ** Getter for phases
    *******************************************************************************/
   public List<JobPhaseV1> getPhases()
   {
      return (this.phases);
   }



   /*******************************************************************************
    ** Setter for phases
    *******************************************************************************/
   public void setPhases(List<JobPhaseV1> phases)
   {
      this.phases = phases;
   }



   /*******************************************************************************
    ** Fluent setter for phases
    *******************************************************************************/
   public JobProgressV1 withPhases(List<JobPhaseV1> phases)
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
   public JobProgressV1 withCurrentPhase(String currentPhase)
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
   public JobProgressV1 withUnitLabel(String unitLabel)
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
   public JobProgressV1 withDone(Long done)
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
   public JobProgressV1 withTotal(Long total)
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
   public JobProgressV1 withBytesDone(Long bytesDone)
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
   public JobProgressV1 withBytesTotal(Long bytesTotal)
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
   public JobProgressV1 withPercent(Double percent)
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
   public JobProgressV1 withEtaSeconds(Long etaSeconds)
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
   public JobProgressV1 withMessage(String message)
   {
      this.message = message;
      return (this);
   }



   /*******************************************************************************
    ** Getter for updatedAt
    *******************************************************************************/
   public String getUpdatedAt()
   {
      return (this.updatedAt);
   }



   /*******************************************************************************
    ** Setter for updatedAt
    *******************************************************************************/
   public void setUpdatedAt(String updatedAt)
   {
      this.updatedAt = updatedAt;
   }



   /*******************************************************************************
    ** Fluent setter for updatedAt
    *******************************************************************************/
   public JobProgressV1 withUpdatedAt(String updatedAt)
   {
      this.updatedAt = updatedAt;
      return (this);
   }

}
