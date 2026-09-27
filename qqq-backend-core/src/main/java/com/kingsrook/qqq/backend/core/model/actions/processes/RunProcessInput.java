/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.actions.processes;


import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobCallback;
import com.kingsrook.qqq.backend.core.actions.processes.QProcessCallback;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.InputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Input data container for the RunProcess action
 **
 *******************************************************************************/
public class RunProcessInput extends AbstractActionInput
{
   private InputSource inputSource = QInputSource.SYSTEM;

   private String               processName;
   private QProcessCallback     callback;
   private ProcessState         processState;
   private FrontendStepBehavior frontendStepBehavior = FrontendStepBehavior.BREAK;
   private String               startAfterStep;
   private String               startAtStep;
   private String               processUUID;
   private AsyncJobCallback     asyncJobCallback;



   /*******************************************************************************
    **
    *******************************************************************************/
   public enum FrontendStepBehavior
   {
      BREAK,
      SKIP,
      FAIL
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public RunProcessInput()
   {
      processState = new ProcessState();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getActionIdentity()
   {
      return (getClass().getSimpleName() + ":" + getProcessName());
   }



   /*******************************************************************************
    ** e.g., for steps after the first step in a process, seed the data in a run
    ** function request from a process state.
    **
    *******************************************************************************/
   public void seedFromProcessState(ProcessState processState)
   {
      this.processState = processState;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QProcessMetaData getProcessMetaData()
   {
      return (QContext.getQInstance().getProcess(getProcessName()));
   }



   /*******************************************************************************
    ** Getter for processName
    **
    *******************************************************************************/
   public String getProcessName()
   {
      return processName;
   }



   /*******************************************************************************
    ** Setter for processName
    **
    *******************************************************************************/
   public void setProcessName(String processName)
   {
      this.processName = processName;
   }



   /*******************************************************************************
    ** Setter for processName
    **
    *******************************************************************************/
   public RunProcessInput withProcessName(String processName)
   {
      this.processName = processName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for records
    **
    *******************************************************************************/
   public List<QRecord> getRecords()
   {
      return processState.getRecords();
   }



   /*******************************************************************************
    ** Setter for records
    **
    *******************************************************************************/
   public void setRecords(List<QRecord> records)
   {
      this.processState.setRecords(records);
   }



   /*******************************************************************************
    ** Setter for records
    **
    *******************************************************************************/
   public RunProcessInput withRecords(List<QRecord> records)
   {
      setRecords(records);
      return (this);
   }



   /*******************************************************************************
    ** Getter for values
    **
    *******************************************************************************/
   public Map<String, Serializable> getValues()
   {
      return this.processState.getValues();
   }



   /*******************************************************************************
    ** Setter for values
    **
    *******************************************************************************/
   public void setValues(Map<String, Serializable> values)
   {
      this.processState.setValues(values);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public RunProcessInput withValue(String fieldName, Serializable value)
   {
      this.processState.getValues().put(fieldName, value);
      return (this);
   }



   /*******************************************************************************
    ** Setter for values
    **
    *******************************************************************************/
   public RunProcessInput withValues(Map<String, Serializable> values)
   {
      this.processState.setValues(values);
      return (this);
   }



   /*******************************************************************************
    ** Setter for values
    **
    *******************************************************************************/
   public RunProcessInput addValue(String fieldName, Serializable value)
   {
      this.processState.getValues().put(fieldName, value);
      return (this);
   }



   /*******************************************************************************
    ** Getter for callback
    **
    *******************************************************************************/
   public QProcessCallback getCallback()
   {
      return callback;
   }



   /*******************************************************************************
    ** Setter for callback
    **
    *******************************************************************************/
   public void setCallback(QProcessCallback callback)
   {
      this.callback = callback;
   }



   /*******************************************************************************
    ** Setter for callback
    **
    *******************************************************************************/
   public RunProcessInput withCallback(QProcessCallback callback)
   {
      this.callback = callback;
      return (this);
   }



   /*******************************************************************************
    ** Getter for a single field's value
    **
    *******************************************************************************/
   public Serializable getValue(String fieldName)
   {
      return (this.processState.getValues().get(fieldName));
   }



   /*******************************************************************************
    ** Getter for a single field's value
    **
    *******************************************************************************/
   public String getValueString(String fieldName)
   {
      return (ValueUtils.getValueAsString(getValue(fieldName)));
   }



   /*******************************************************************************
    ** Getter for a single field's value
    **
    *******************************************************************************/
   public Integer getValueInteger(String fieldName)
   {
      return (ValueUtils.getValueAsInteger(getValue(fieldName)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public BigDecimal getValueBigDecimal(String fieldName)
   {
      return (ValueUtils.getValueAsBigDecimal(getValue(fieldName)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public Boolean getValueBoolean(String fieldName)
   {
      return (ValueUtils.getValueAsBoolean(getValue(fieldName)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public LocalTime getValueLocalTime(String fieldName)
   {
      return (ValueUtils.getValueAsLocalTime(getValue(fieldName)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public LocalDate getValueLocalDate(String fieldName)
   {
      return (ValueUtils.getValueAsLocalDate(getValue(fieldName)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public byte[] getValueByteArray(String fieldName)
   {
      return (ValueUtils.getValueAsByteArray(getValue(fieldName)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public Instant getValueInstant(String fieldName)
   {
      return (ValueUtils.getValueAsInstant(getValue(fieldName)));
   }



   /*******************************************************************************
    ** Accessor for processState
    **
    *******************************************************************************/
   public ProcessState getProcessState()
   {
      return processState;
   }



   /*******************************************************************************
    ** Getter for frontendStepBehavior
    **
    *******************************************************************************/
   public FrontendStepBehavior getFrontendStepBehavior()
   {
      return frontendStepBehavior;
   }



   /*******************************************************************************
    ** Setter for frontendStepBehavior
    **
    *******************************************************************************/
   public void setFrontendStepBehavior(FrontendStepBehavior frontendStepBehavior)
   {
      this.frontendStepBehavior = frontendStepBehavior;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setStartAfterStep(String startAfterStep)
   {
      this.startAfterStep = startAfterStep;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String getStartAfterStep()
   {
      return startAfterStep;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setProcessUUID(String processUUID)
   {
      this.processUUID = processUUID;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String getProcessUUID()
   {
      return processUUID;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setAsyncJobCallback(AsyncJobCallback asyncJobCallback)
   {
      this.asyncJobCallback = asyncJobCallback;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public AsyncJobCallback getAsyncJobCallback()
   {
      return asyncJobCallback;
   }



   /*******************************************************************************
    ** Getter for startAtStep
    *******************************************************************************/
   public String getStartAtStep()
   {
      return (this.startAtStep);
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
   public RunProcessInput withStartAtStep(String startAtStep)
   {
      this.startAtStep = startAtStep;
      return (this);
   }



   /*******************************************************************************
    * Fluent setter for frontendStepBehavior
    *
    * @param frontendStepBehavior
    * @return this
    *******************************************************************************/
   public RunProcessInput withFrontendStepBehavior(FrontendStepBehavior frontendStepBehavior)
   {
      this.frontendStepBehavior = frontendStepBehavior;
      return (this);
   }



   /*******************************************************************************
    * Fluent setter for startAfterStep
    *
    * @param startAfterStep
    * @return this
    *******************************************************************************/
   public RunProcessInput withStartAfterStep(String startAfterStep)
   {
      this.startAfterStep = startAfterStep;
      return (this);
   }



   /*******************************************************************************
    * Fluent setter for processUUID
    *
    * @param processUUID
    * @return this
    *******************************************************************************/
   public RunProcessInput withProcessUUID(String processUUID)
   {
      this.processUUID = processUUID;
      return (this);
   }



   /*******************************************************************************
    * Fluent setter for asyncJobCallback
    *
    * @param asyncJobCallback
    * @return this
    *******************************************************************************/
   public RunProcessInput withAsyncJobCallback(AsyncJobCallback asyncJobCallback)
   {
      this.asyncJobCallback = asyncJobCallback;
      return (this);
   }



   /*******************************************************************************
    ** Source of caller values and automatic record loading. Backend code continues
    ** to choose the source for its own actions independently.
    *******************************************************************************/
   public InputSource getInputSource()
   {
      return (inputSource);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setInputSource(InputSource inputSource)
   {
      this.inputSource = inputSource;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public RunProcessInput withInputSource(InputSource inputSource)
   {
      setInputSource(inputSource);
      return (this);
   }
}
