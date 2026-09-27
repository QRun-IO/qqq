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
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobCallback;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobStatus;
import com.kingsrook.qqq.backend.core.actions.async.NonPersistedAsyncJobCallback;
import com.kingsrook.qqq.backend.core.actions.processes.QProcessCallback;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.InputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.processes.tracing.ProcessTracerInterface;
import com.kingsrook.qqq.backend.core.processes.tracing.ProcessTracerMessage;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Input data container for the RunBackendStep action
 **
 *******************************************************************************/
public class RunBackendStepInput extends AbstractActionInput
{
   private InputSource inputSource = QInputSource.SYSTEM;

   private static final QLogger LOG = QLogger.getLogger(RunBackendStepInput.class);

   private ProcessState                         processState;
   private String                               processName;
   private String                               tableName;
   private String                               stepName;
   private QProcessCallback                     callback;
   private AsyncJobCallback                     asyncJobCallback;
   private RunProcessInput.FrontendStepBehavior frontendStepBehavior;
   private Instant                              basepullLastRunTime;

   private ProcessTracerInterface processTracer;

   ////////////////////////////////////////////////////////////////////////////
   // note - new fields should generally be added in method: cloneFieldsInto //
   ////////////////////////////////////////////////////////////////////////////


   /*******************************************************************************
    **
    *******************************************************************************/
   public RunBackendStepInput()
   {
      processState = new ProcessState();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public RunBackendStepInput(ProcessState processState)
   {
      this.processState = processState;
   }



   /*******************************************************************************
    ** Kinda like a reverse copy-constructor -- for a subclass that wants all the
    ** field values from this object.  Keep this in sync with the fields in this class!
    **
    ** Of note - the processState does NOT get cloned - because...  well, in our first
    ** use-case (a subclass that doesn't WANT the same/full state), that's what we needed.
    *******************************************************************************/
   public void cloneFieldsInto(RunBackendStepInput target)
   {
      target.setInputSource(getInputSource());
      target.setStepName(getStepName());
      target.setTableName(getTableName());
      target.setProcessName(getProcessName());
      target.setAsyncJobCallback(getAsyncJobCallback());
      target.setFrontendStepBehavior(getFrontendStepBehavior());
      target.setValues(getValues());
      target.setProcessTracer(getProcessTracer().orElse(null));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QStepMetaData getStepMetaData()
   {
      return (QContext.getQInstance().getProcessStep(getProcessName(), getStepName()));
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
   public RunBackendStepInput withProcessName(String processName)
   {
      this.processName = processName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableName
    **
    *******************************************************************************/
   public String getTableName()
   {
      return tableName;
   }



   /*******************************************************************************
    ** Setter for tableName
    **
    *******************************************************************************/
   public void setTableName(String tableName)
   {
      this.tableName = tableName;
   }



   /*******************************************************************************
    ** Fluent setter for tableName
    **
    *******************************************************************************/
   public RunBackendStepInput withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QTableMetaData getTable()
   {
      if(tableName == null)
      {
         return (null);
      }

      return (QContext.getQInstance().getTable(tableName));
   }



   /*******************************************************************************
    ** Getter for functionName
    **
    *******************************************************************************/
   public String getStepName()
   {
      return stepName;
   }



   /*******************************************************************************
    ** Setter for functionName
    **
    *******************************************************************************/
   public void setStepName(String stepName)
   {
      this.stepName = stepName;
   }



   /*******************************************************************************
    ** Setter for functionName
    **
    *******************************************************************************/
   public RunBackendStepInput withFunctionName(String functionName)
   {
      this.stepName = functionName;
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
    ** Getter for records converted to entities of a given type.
    **
    *******************************************************************************/
   public <E extends QRecordEntity> List<E> getRecordsAsEntities(Class<E> entityClass) throws QException
   {
      List<E> rs = new ArrayList<>();

      ///////////////////////////////////////////////////////////////////////////////////
      // note - important to call getRecords here, which is overwritten in subclasses! //
      ///////////////////////////////////////////////////////////////////////////////////
      for(QRecord record : getRecords())
      {
         rs.add(QRecordEntity.fromQRecord(entityClass, record));
      }
      return (rs);
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
   public RunBackendStepInput withRecords(List<QRecord> records)
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
   public RunBackendStepInput withValues(Map<String, Serializable> values)
   {
      this.processState.setValues(values);
      return (this);
   }



   /*******************************************************************************
    ** Setter for values
    **
    *******************************************************************************/
   public RunBackendStepInput addValue(String fieldName, Serializable value)
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
   public RunBackendStepInput withCallback(QProcessCallback callback)
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
      return (processState.getValues().get(fieldName));
   }



   /*******************************************************************************
    ** Getter for a single field's date value
    **
    *******************************************************************************/
   public LocalDate getValueLocalDate(String fieldName)
   {
      return (ValueUtils.getValueAsLocalDate(getValue(fieldName)));
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
   public Boolean getValueBoolean(String fieldName)
   {
      return (ValueUtils.getValueAsBoolean(getValue(fieldName)));
   }



   /*******************************************************************************
    ** Getter for a single field's value as a primitive boolean - with null => false.
    **
    *******************************************************************************/
   public boolean getValuePrimitiveBoolean(String fieldName)
   {
      Boolean valueAsBoolean = ValueUtils.getValueAsBoolean(getValue(fieldName));
      return (valueAsBoolean != null && valueAsBoolean);
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
   public Instant getValueInstant(String fieldName)
   {
      return (ValueUtils.getValueAsInstant(getValue(fieldName)));
   }



   /*******************************************************************************
    ** Accessor for processState's isStepBack attribute
    **
    *******************************************************************************/
   public boolean getIsStepBack()
   {
      return processState.getIsStepBack();
   }



   /*******************************************************************************
    ** Accessor for processState - protected, because we generally want to access
    ** its members through wrapper methods, we think
    **
    *******************************************************************************/
   protected ProcessState getProcessState()
   {
      return processState;
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
      if(asyncJobCallback == null)
      {
         /////////////////////////////////////////////////////////////////////////
         // avoid NPE in case we didn't have one of these!  create a new one... //
         /////////////////////////////////////////////////////////////////////////
         asyncJobCallback = new NonPersistedAsyncJobCallback(UUID.randomUUID(), new AsyncJobStatus().withJobName(processName + "." + stepName));
      }
      return (asyncJobCallback);
   }



   /*******************************************************************************
    ** Getter for frontendStepBehavior
    **
    *******************************************************************************/
   public RunProcessInput.FrontendStepBehavior getFrontendStepBehavior()
   {
      return frontendStepBehavior;
   }



   /*******************************************************************************
    ** Setter for frontendStepBehavior
    **
    *******************************************************************************/
   public void setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior frontendStepBehavior)
   {
      this.frontendStepBehavior = frontendStepBehavior;
   }



   /*******************************************************************************
    ** Fluent setter for frontendStepBehavior
    **
    *******************************************************************************/
   public RunBackendStepInput withFrontendStepBehavior(RunProcessInput.FrontendStepBehavior frontendStepBehavior)
   {
      this.frontendStepBehavior = frontendStepBehavior;
      return (this);
   }



   /*******************************************************************************
    ** Getter for basepullLastRunTime
    **
    *******************************************************************************/
   public Instant getBasepullLastRunTime()
   {
      return basepullLastRunTime;
   }



   /*******************************************************************************
    ** Setter for basepullLastRunTime
    **
    *******************************************************************************/
   public void setBasepullLastRunTime(Instant basepullLastRunTime)
   {
      this.basepullLastRunTime = basepullLastRunTime;
   }



   /*******************************************************************************
    ** Fluent setter for basepullLastRunTime
    **
    *******************************************************************************/
   public RunBackendStepInput withBasepullLastRunTime(Instant basepullLastRunTime)
   {
      this.basepullLastRunTime = basepullLastRunTime;
      return (this);
   }



   /*******************************************************************************
    ** Setter for processTracer
    *******************************************************************************/
   public void setProcessTracer(ProcessTracerInterface processTracer)
   {
      this.processTracer = processTracer;
   }



   /*******************************************************************************
    ** Fluent setter for processTracer
    *******************************************************************************/
   public RunBackendStepInput withProcessTracer(ProcessTracerInterface processTracer)
   {
      this.processTracer = processTracer;
      return (this);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public Optional<ProcessTracerInterface> getProcessTracer()
   {
      return Optional.ofNullable(processTracer);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public void traceMessage(ProcessTracerMessage message)
   {
      if(processTracer != null && message != null)
      {
         try
         {
            processTracer.handleMessage(this, message);
         }
         catch(Exception e)
         {
            LOG.warn("Error tracing message", e, logPair("message", message));
         }
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public QProcessMetaData getProcess()
   {
      return (QContext.getQInstance().getProcess(getProcessName()));
   }



   /***************************************************************************
    ** return a QProcessPayload subclass instance, with values populated from
    ** the current process state.
    ***************************************************************************/
   public <T extends QProcessPayload> T getProcessPayload(Class<T> payloadClass) throws QException
   {
      return QProcessPayload.fromProcessState(payloadClass, getProcessState());
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
   public RunBackendStepInput withInputSource(InputSource inputSource)
   {
      setInputSource(inputSource);
      return (this);
   }
}
