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
import java.util.Optional;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.utils.ObjectUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Output data container for the RunProcess action
 **
 *******************************************************************************/
public class RunProcessOutput extends AbstractActionOutput implements Serializable
{
   private ProcessState        processState;
   private String              processUUID;
   private Optional<Exception> exception = Optional.empty();



   /*******************************************************************************
    **
    *******************************************************************************/
   public RunProcessOutput()
   {
      processState = new ProcessState();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public RunProcessOutput(ProcessState processState)
   {
      this.processState = processState;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String toString()
   {
      return "RunProcessOutput{uuid='" + processUUID
         + ",exception?='" + (exception.isPresent() ? exception.get().getMessage() : "null")
         + ",records.size()=" + (processState == null ? null : processState.getRecords().size())
         + ",values=" + (processState == null ? null : processState.getValues())
         + "}";
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
   public RunProcessOutput withRecords(List<QRecord> records)
   {
      this.processState.setRecords(records);
      return (this);
   }



   /*******************************************************************************
    ** Getter for values
    **
    *******************************************************************************/
   public Map<String, Serializable> getValues()
   {
      return processState.getValues();
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
   public RunProcessOutput withValue(String fieldName, Serializable value)
   {
      this.processState.getValues().put(fieldName, value);
      return (this);
   }



   /*******************************************************************************
    ** Setter for values
    **
    *******************************************************************************/
   public RunProcessOutput withValues(Map<String, Serializable> values)
   {
      this.processState.setValues(values);
      return (this);
   }



   /*******************************************************************************
    ** Setter for values
    **
    *******************************************************************************/
   public RunProcessOutput addValue(String fieldName, Serializable value)
   {
      this.processState.getValues().put(fieldName, value);
      return (this);
   }



   /*******************************************************************************
    ** Getter for processUUID
    **
    *******************************************************************************/
   public String getProcessUUID()
   {
      return processUUID;
   }



   /*******************************************************************************
    ** Setter for processUUID
    **
    *******************************************************************************/
   public void setProcessUUID(String processUUID)
   {
      this.processUUID = processUUID;
   }



   /*******************************************************************************
    ** Getter for processState
    **
    *******************************************************************************/
   public ProcessState getProcessState()
   {
      return processState;
   }



   /*******************************************************************************
    ** Setter for processState
    **
    *******************************************************************************/
   public void setProcessState(ProcessState processState)
   {
      this.processState = processState;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setException(Exception exception)
   {
      this.exception = Optional.of(exception);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public Optional<Exception> getException()
   {
      return exception;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setUpdatedFrontendStepList(List<QFrontendStepMetaData> updatedFrontendStepList)
   {
      if(this.processState.getProcessMetaDataAdjustment() == null)
      {
         this.processState.setProcessMetaDataAdjustment(new ProcessMetaDataAdjustment());
      }

      this.processState.getProcessMetaDataAdjustment().setUpdatedFrontendStepList(updatedFrontendStepList);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public List<QFrontendStepMetaData> getUpdatedFrontendStepList()
   {
      return ObjectUtils.tryElse(() -> this.processState.getProcessMetaDataAdjustment().getUpdatedFrontendStepList(), null);
   }



   /*******************************************************************************
    ** Getter for processMetaDataAdjustment
    *******************************************************************************/
   public ProcessMetaDataAdjustment getProcessMetaDataAdjustment()
   {
      return (this.processState.getProcessMetaDataAdjustment());
   }



   /*******************************************************************************
    ** Setter for processMetaDataAdjustment
    *******************************************************************************/
   public void setProcessMetaDataAdjustment(ProcessMetaDataAdjustment processMetaDataAdjustment)
   {
      this.processState.setProcessMetaDataAdjustment(processMetaDataAdjustment);
   }

}
