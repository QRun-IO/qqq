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

package com.kingsrook.qqq.middleware.javalin.executors.utils;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.permissions.CorePermissionEnforcer;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessHelper;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessState;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QComponentType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendComponentMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.audits.GetAuditsForRecordProcess;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ExceptionUtils;
import com.kingsrook.qqq.middleware.javalin.executors.io.ProcessInitOrStepOrStatusOutputInterface;


/*******************************************************************************
 **
 *******************************************************************************/
public class ProcessExecutorUtils
{
   private static final QLogger LOG = QLogger.getLogger(ProcessExecutorUtils.class);



   /*******************************************************************************
    ** Whether a step finished synchronously or asynchronously, return its data
    ** to the caller the same way.
    *******************************************************************************/
   public static void serializeRunProcessResultForCaller(ProcessInitOrStepOrStatusOutputInterface processInitOrStepOutput, String processName, RunProcessOutput runProcessOutput)
   {
      processInitOrStepOutput.setType(ProcessInitOrStepOrStatusOutputInterface.Type.COMPLETE);

      if(runProcessOutput.getException().isPresent())
      {
         ////////////////////////////////////////////////////////////////
         // per code coverage, this path may never actually get hit... //
         ////////////////////////////////////////////////////////////////
         serializeRunProcessExceptionForCaller(processInitOrStepOutput, runProcessOutput.getException().get());
      }

      processInitOrStepOutput.setValues(valuesForCaller(processName, runProcessOutput.getValues()));
      // processInitOrStepOutput.setValues(getValuesForCaller(processName, runProcessOutput));

      runProcessOutput.getProcessState().getNextStepName().ifPresent(nextStep -> processInitOrStepOutput.setNextStep(nextStep));
      runProcessOutput.getProcessState().getBackStepName().ifPresent(backStep -> processInitOrStepOutput.setBackStep(backStep));

      if(runProcessOutput.getProcessMetaDataAdjustment() != null)
      {
         processInitOrStepOutput.setProcessMetaDataAdjustment(runProcessOutput.getProcessMetaDataAdjustment());
      }
   }

   // /***************************************************************************
   //  ** maybe good idea here, but... to only return fields that the frontend steps
   //  ** say they care about.  yeah.
   //  ***************************************************************************/
   // private static Map<String, Serializable> getValuesForCaller(String processName, RunProcessOutput runProcessOutput)
   // {
   //    QProcessMetaData process = QContext.getQInstance().getProcess(processName);
   //    Map<String, QFieldMetaData> frontendFields = new LinkedHashMap<>();
   //    for(QStepMetaData step : process.getAllSteps().values())
   //    {
   //       if(step instanceof QFrontendStepMetaData frontendStepMetaData)
   //       {
   //          frontendFields.addAll(frontendStepMetaData.getAllFields());
   //       }
   //       else if(step instanceof QStateMachineStep stateMachineStep)
   //       {
   //          for(QStepMetaData subStep : stateMachineStep.getSubSteps())
   //          {
   //             // recur, etc
   //          }
   //       }
   //    }
   //
   //    // then, only return ones in the map, eh
   // }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void serializeRunProcessExceptionForCaller(ProcessInitOrStepOrStatusOutputInterface processInitOrStepOutput, Exception exception)
   {
      processInitOrStepOutput.setType(ProcessInitOrStepOrStatusOutputInterface.Type.ERROR);

      QUserFacingException userFacingException = ExceptionUtils.findClassInRootChain(exception, QUserFacingException.class);

      if(userFacingException != null)
      {
         LOG.info("User-facing exception in process", userFacingException);
         processInitOrStepOutput.setError(userFacingException.getMessage());
         processInitOrStepOutput.setUserFacingError(userFacingException.getMessage());
      }
      else
      {
         Throwable rootException = ExceptionUtils.getRootException(exception);
         LOG.warn("Uncaught Exception in process", exception);
         processInitOrStepOutput.setError("Error message: " + rootException.getMessage());
      }
   }



   /*******************************************************************************
    ** Copy records before applying current session policy to persisted process results.
    *******************************************************************************/
   public static List<QRecord> recordsForCaller(String processName, List<QRecord> records)
   {
      return (recordsForCaller(processName, null, records));
   }



   /*******************************************************************************
    ** Prefer the current record-list component table when it identifies one.
    *******************************************************************************/
   public static List<QRecord> recordsForCaller(String processName, ProcessState state, List<QRecord> records)
   {
      List<QRecord> copies = new ArrayList<>();
      records.forEach(record -> copies.add(copyRecord(record)));
      String tableName = QContext.getQInstance().getProcess(processName).getTableName();
      if(state != null && state.getNextStepName().isPresent() && QContext.getQInstance().getProcess(processName).getStep(state.getNextStepName().get()) instanceof QFrontendStepMetaData step)
      {
         for(QFrontendComponentMetaData component : CollectionUtils.nonNullList(step.getComponents()))
         {
            if(component.getType() == QComponentType.RECORD_LIST && CollectionUtils.nonNullMap(component.getValues()).get("tableName") instanceof String componentTable)
            {
               tableName = componentTable;
               break;
            }
         }
      }
      if(CorePermissionEnforcer.shouldEnforceFieldAccess(new RunProcessInput().withInputSource(QInputSource.USER)))
      {
         FieldAccessHelper.applyToProcessRecords(processName, tableName, copies);
      }
      return (copies);
   }



   /*******************************************************************************
    ** Process values may contain individual records or nested record collections.
    *******************************************************************************/
   private static Serializable valueForCaller(String processName, Serializable value)
   {
      if(value instanceof QRecord record)
      {
         return (recordsForCaller(processName, List.of(record)).get(0));
      }
      if(value instanceof List<?> list)
      {
         ArrayList<Serializable> copies = new ArrayList<>();
         for(Object item : list)
         {
            copies.add(valueForCaller(processName, (Serializable) item));
         }
         return (copies);
      }
      if(value instanceof Map<?, ?> map)
      {
         LinkedHashMap<Serializable, Serializable> copy = new LinkedHashMap<>();
         map.forEach((key, item) -> copy.put((Serializable) key, valueForCaller(processName, (Serializable) item)));
         return (copy);
      }
      return (value);
   }



   /*******************************************************************************
    ** Associated records in persisted process state need their own copies too.
    *******************************************************************************/
   private static QRecord copyRecord(QRecord record)
   {
      QRecord copy = new QRecord(record);
      if(record.getAssociatedRecords() != null)
      {
         Map<String, List<QRecord>> associations = new LinkedHashMap<>();
         record.getAssociatedRecords().forEach((name, children) -> associations.put(name, children.stream().map(ProcessExecutorUtils::copyRecord).toList()));
         copy.setAssociatedRecords(associations);
      }
      return (copy);
   }



   /*******************************************************************************
    ** Share process value masking between legacy and versioned HTTP routes.
    *******************************************************************************/
   public static Map<String, Serializable> valuesForCaller(String processName, Map<String, Serializable> processValues)
   {
      Map<String, Serializable> values = new LinkedHashMap<>();
      processValues.forEach((name, value) -> values.put(name, valueForCaller(processName, value)));
      if(GetAuditsForRecordProcess.NAME.equals(processName) && values.get("tableName") instanceof String tableName
         && values.get("audits") instanceof List<?> audits
         && CorePermissionEnforcer.shouldEnforceFieldAccess(new RunProcessInput().withInputSource(QInputSource.USER)))
      {
         List<QRecord> records = new ArrayList<>();
         for(Object audit : audits)
         {
            if(audit instanceof QRecord record)
            {
               records.add(record);
            }
         }
         FieldAccessHelper.applyToAuditDetails(tableName, records);
         values.put("audits", (Serializable) records);
      }
      return (values);
   }
}
