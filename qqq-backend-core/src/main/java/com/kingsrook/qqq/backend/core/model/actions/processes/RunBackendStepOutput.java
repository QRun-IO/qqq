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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QRuntimeException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionOutput;
import com.kingsrook.qqq.backend.core.model.actions.audits.AuditInput;
import com.kingsrook.qqq.backend.core.model.actions.audits.AuditSingleInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Output data container for the RunBackendStep action
 **
 *******************************************************************************/
public class RunBackendStepOutput extends AbstractActionOutput implements Serializable
{
   private String processName;

   private ProcessState processState;
   private Exception    exception; // todo - make optional

   private String overrideLastStepName; // todo - does this need to go into state too??

   private List<AuditInput> auditInputList = new ArrayList<>();



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String toString()
   {
      return "RunBackendStepOutput{exception?='" + (exception == null ? "null" : exception.getMessage())
         + ",records.size()=" + (processState == null ? null : processState.getRecords().size())
         + ",values=" + (processState == null ? null : processState.getValues())
         + "}";
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public RunBackendStepOutput()
   {
      this.processState = new ProcessState();
   }



   /*******************************************************************************
    ** e.g., populate the process state (records, values) in this result object.
    **
    *******************************************************************************/
   public void seedFromRequest(RunBackendStepInput runBackendStepInput)
   {
      this.processState = runBackendStepInput.getProcessState();
      this.processName = runBackendStepInput.getProcessName();
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
   public RunBackendStepOutput withRecords(List<QRecord> records)
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
    ** Setter for values
    **
    *******************************************************************************/
   public void setValues(Map<String, Serializable> values)
   {
      this.processState.setValues(values);
   }



   /*******************************************************************************
    ** Setter for values
    **
    *******************************************************************************/
   public RunBackendStepOutput withValues(Map<String, Serializable> values)
   {
      this.processState.setValues(values);
      return (this);
   }



   /*******************************************************************************
    ** Setter for values
    **
    *******************************************************************************/
   public RunBackendStepOutput addValue(String fieldName, Serializable value)
   {
      this.processState.getValues().put(fieldName, value);
      return (this);
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
    **
    *******************************************************************************/
   public void setException(Exception exception)
   {
      this.exception = exception;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public Exception getException()
   {
      return exception;
   }



   /*******************************************************************************
    ** Getter for a single field's value
    **
    *******************************************************************************/
   public Serializable getValue(String fieldName)
   {
      return (processState.getValues().get(fieldName));
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
    ** Getter for a single field's value
    **
    *******************************************************************************/
   public Boolean getValueBoolean(String fieldName)
   {
      return (ValueUtils.getValueAsBoolean(getValue(fieldName)));
   }



   /*******************************************************************************
    ** Getter for a single field's value
    **
    *******************************************************************************/
   public BigDecimal getValueBigDecimal(String fieldName)
   {
      return (ValueUtils.getValueAsBigDecimal(getValue(fieldName)));
   }



   /*******************************************************************************
    ** add a record to the step output, e.g., for going through to the next step.
    *******************************************************************************/
   public void addRecord(QRecord record)
   {
      if(this.processState.getRecords() == null)
      {
         this.processState.setRecords(new ArrayList<>());
      }
      this.processState.getRecords().add(record);
   }



   /***************************************************************************
    ** add a RecordEntity to the step output, e.g., for going through to the next step.
    ***************************************************************************/
   public void addRecordEntity(QRecordEntity recordEntity)
   {
      addRecord(recordEntity.toQRecord());
   }



   /*******************************************************************************
    ** Getter for auditInputList
    *******************************************************************************/
   public List<AuditInput> getAuditInputList()
   {
      return (this.auditInputList);
   }



   /*******************************************************************************
    ** Setter for auditInputList
    *******************************************************************************/
   public void setAuditInputList(List<AuditInput> auditInputList)
   {
      this.auditInputList = auditInputList;
   }



   /*******************************************************************************
    ** Fluent setter for auditInputList
    *******************************************************************************/
   public RunBackendStepOutput withAuditInputList(List<AuditInput> auditInputList)
   {
      this.auditInputList = auditInputList;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void addAuditSingleInput(AuditSingleInput auditSingleInput)
   {
      if(getAuditInputList() == null)
      {
         setAuditInputList(new ArrayList<>());
      }

      if(getAuditInputList().isEmpty())
      {
         getAuditInputList().add(new AuditInput());
      }

      AuditInput auditInput = getAuditInputList().get(0);
      auditInput.addAuditSingleInput(auditSingleInput);
   }



   /*******************************************************************************
    ** Getter for overrideLastStepName
    *******************************************************************************/
   public String getOverrideLastStepName()
   {
      return (this.overrideLastStepName);
   }



   /*******************************************************************************
    ** Setter for overrideLastStepName
    *******************************************************************************/
   public void setOverrideLastStepName(String overrideLastStepName)
   {
      this.overrideLastStepName = overrideLastStepName;
   }



   /*******************************************************************************
    ** Fluent setter for overrideLastStepName
    *******************************************************************************/
   public RunBackendStepOutput withOverrideLastStepName(String overrideLastStepName)
   {
      this.overrideLastStepName = overrideLastStepName;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void updateStepList(List<String> stepList)
   {
      getProcessState().setStepList(stepList);

      if(processName == null)
      {
         throw (new QRuntimeException("ProcessName was not set in this object, therefore updateStepList cannot complete successfully.  Try to manually call setProcessName as a work around."));
      }

      QProcessMetaData processMetaData = QContext.getQInstance().getProcess(processName);

      ArrayList<QFrontendStepMetaData> updatedFrontendStepList = new ArrayList<>(stepList.stream()
         .map(name -> processMetaData.getStep(name))
         .filter(step -> step instanceof QFrontendStepMetaData)
         .map(step -> (QFrontendStepMetaData) step)
         .toList());

      ProcessMetaDataAdjustment processMetaDataAdjustment = getProcessMetaDataAdjustment();
      if(processMetaDataAdjustment == null)
      {
         processMetaDataAdjustment = new ProcessMetaDataAdjustment();
      }
      processMetaDataAdjustment.setUpdatedFrontendStepList(updatedFrontendStepList);
      setProcessMetaDataAdjustment(processMetaDataAdjustment);
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
   public RunBackendStepOutput withProcessName(String processName)
   {
      this.processName = processName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for ProcessMetaDataAdjustment (pass-through to processState)
    *******************************************************************************/
   public ProcessMetaDataAdjustment getProcessMetaDataAdjustment()
   {
      return (this.processState.getProcessMetaDataAdjustment());
   }



   /*******************************************************************************
    ** Setter for updatedFrontendStepList (pass-through to processState)
    *******************************************************************************/
   public void setProcessMetaDataAdjustment(ProcessMetaDataAdjustment processMetaDataAdjustment)
   {
      this.processState.setProcessMetaDataAdjustment(processMetaDataAdjustment);
   }



   /***************************************************************************
    ** Update the process state with values from the input processPayload
    ** subclass instance.
    ***************************************************************************/
   public void setProcessPayload(QProcessPayload processPayload)
   {
      processPayload.toProcessState(getProcessState());
   }
}
