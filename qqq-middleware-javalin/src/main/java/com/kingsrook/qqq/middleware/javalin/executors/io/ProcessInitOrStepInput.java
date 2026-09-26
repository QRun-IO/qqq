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


import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.TableVariant;


/*******************************************************************************
 **
 *******************************************************************************/
public class ProcessInitOrStepInput extends AbstractMiddlewareInput
{
   private String       processName;
   private Integer      stepTimeoutMillis = 3000;
   private QQueryFilter recordsFilter;

   private Map<String, Serializable> values = new LinkedHashMap<>();

   /////////////////////////////////////
   // used only for 'step' (not init) //
   /////////////////////////////////////
   private String processUUID;
   private String startAfterStep;
   // todo - add (in next version?) startAtStep (for back)

   private RunProcessInput.FrontendStepBehavior frontendStepBehavior = RunProcessInput.FrontendStepBehavior.BREAK;
   private String startAtStep;

   private TableVariant tableVariant;

   // todo - file??



   /***************************************************************************
    **
    ***************************************************************************/
   public enum RecordsParam
   {
      FILTER_JSON("filterJSON"),
      RECORD_IDS("recordIds");


      private final String value;



      /***************************************************************************
       **
       ***************************************************************************/
      RecordsParam(String value)
      {
         this.value = value;
      }
   }



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
   public ProcessInitOrStepInput withProcessName(String processName)
   {
      this.processName = processName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for stepTimeoutMillis
    *******************************************************************************/
   public Integer getStepTimeoutMillis()
   {
      return (this.stepTimeoutMillis);
   }



   /*******************************************************************************
    ** Setter for stepTimeoutMillis
    *******************************************************************************/
   public void setStepTimeoutMillis(Integer stepTimeoutMillis)
   {
      this.stepTimeoutMillis = stepTimeoutMillis;
   }



   /*******************************************************************************
    ** Fluent setter for stepTimeoutMillis
    *******************************************************************************/
   public ProcessInitOrStepInput withStepTimeoutMillis(Integer stepTimeoutMillis)
   {
      this.stepTimeoutMillis = stepTimeoutMillis;
      return (this);
   }




   /*******************************************************************************
    ** Getter for recordsFilter
    *******************************************************************************/
   public QQueryFilter getRecordsFilter()
   {
      return (this.recordsFilter);
   }



   /*******************************************************************************
    ** Setter for recordsFilter
    *******************************************************************************/
   public void setRecordsFilter(QQueryFilter recordsFilter)
   {
      this.recordsFilter = recordsFilter;
   }



   /*******************************************************************************
    ** Fluent setter for recordsFilter
    *******************************************************************************/
   public ProcessInitOrStepInput withRecordsFilter(QQueryFilter recordsFilter)
   {
      this.recordsFilter = recordsFilter;
      return (this);
   }



   /*******************************************************************************
    ** Getter for values
    *******************************************************************************/
   public Map<String, Serializable> getValues()
   {
      return (this.values);
   }



   /*******************************************************************************
    ** Setter for values
    *******************************************************************************/
   public void setValues(Map<String, Serializable> values)
   {
      this.values = values;
   }



   /*******************************************************************************
    ** Fluent setter for values
    *******************************************************************************/
   public ProcessInitOrStepInput withValues(Map<String, Serializable> values)
   {
      this.values = values;
      return (this);
   }



   /*******************************************************************************
    ** Getter for frontendStepBehavior
    *******************************************************************************/
   public RunProcessInput.FrontendStepBehavior getFrontendStepBehavior()
   {
      return (this.frontendStepBehavior);
   }



   /*******************************************************************************
    ** Setter for frontendStepBehavior
    *******************************************************************************/
   public void setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior frontendStepBehavior)
   {
      this.frontendStepBehavior = frontendStepBehavior;
   }



   /*******************************************************************************
    ** Fluent setter for frontendStepBehavior
    *******************************************************************************/
   public ProcessInitOrStepInput withFrontendStepBehavior(RunProcessInput.FrontendStepBehavior frontendStepBehavior)
   {
      this.frontendStepBehavior = frontendStepBehavior;
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
   public ProcessInitOrStepInput withProcessUUID(String processUUID)
   {
      this.processUUID = processUUID;
      return (this);
   }



   /*******************************************************************************
    ** Getter for startAfterStep
    *******************************************************************************/
   public String getStartAfterStep()
   {
      return (this.startAfterStep);
   }



   /*******************************************************************************
    ** Setter for startAfterStep
    *******************************************************************************/
   public void setStartAfterStep(String startAfterStep)
   {
      this.startAfterStep = startAfterStep;
   }



   /*******************************************************************************
    ** Fluent setter for startAfterStep
    *******************************************************************************/
   public ProcessInitOrStepInput withStartAfterStep(String startAfterStep)
   {
      this.startAfterStep = startAfterStep;
      return (this);
   }



   /*******************************************************************************
    ** Getter for startAtStep
    *******************************************************************************/
   public String getStartAtStep()
   {
      return (startAtStep);
   }



   /*******************************************************************************
    ** Setter for startAtStep
    *******************************************************************************/
   public void setStartAtStep(String startAtStep)
   {
      this.startAtStep = startAtStep;
   }



   /*******************************************************************************
    ** Fluent setter for startAtStep
    *******************************************************************************/
   public ProcessInitOrStepInput withStartAtStep(String startAtStep)
   {
      this.startAtStep = startAtStep;
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
   public ProcessInitOrStepInput withTableVariant(TableVariant tableVariant)
   {
      this.tableVariant = tableVariant;
      return (this);
   }

}
