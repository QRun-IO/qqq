/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.backend.core.actions.processes;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReferenceLambda;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Action handler for running backend steps as part of processes.
 *
 *******************************************************************************/
public class RunBackendStepAction
{
   private static final QLogger LOG = QLogger.getLogger(RunBackendStepAction.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   public RunBackendStepOutput execute(RunBackendStepInput runBackendStepInput) throws QException
   {
      ActionHelper.validateSession(runBackendStepInput);

      QProcessMetaData process = QContext.getQInstance().getProcess(runBackendStepInput.getProcessName());
      if(process == null)
      {
         throw new QException("Process [" + runBackendStepInput.getProcessName() + "] is not defined in this instance.");
      }

      QStepMetaData stepMetaData = process.getStep(runBackendStepInput.getStepName());
      if(stepMetaData == null)
      {
         if(process.getCancelStep() != null && Objects.equals(process.getCancelStep().getName(), runBackendStepInput.getStepName()))
         {
            /////////////////////////////////////
            // special case for cancel step... //
            /////////////////////////////////////
            stepMetaData = process.getCancelStep();
         }
         else
         {
            throw new QException("Step [" + runBackendStepInput.getStepName() + "] is not defined in the process [" + process.getName() + "]");
         }
      }

      if(!(stepMetaData instanceof QBackendStepMetaData backendStepMetaData))
      {
         throw new QException("Step [" + runBackendStepInput.getStepName() + "] is not a backend step.");
      }

      //////////////////////////////////////////////////////////////////////////////////////
      // ensure input data is set as needed - use callback object to get anything missing //
      //////////////////////////////////////////////////////////////////////////////////////
      ensureRecordsAreInRequest(runBackendStepInput, backendStepMetaData, process);
      ensureInputFieldsAreInRequest(runBackendStepInput, backendStepMetaData);

      ////////////////////////////////////////////////////////////////////
      // load and run the user-defined code that actually does the work //
      ////////////////////////////////////////////////////////////////////
      return (runStepCode(backendStepMetaData.getCode(), runBackendStepInput));
   }



   /*******************************************************************************
    ** check if this step needs any input fields - and if so, if we need to get one
    ** via the callback
    **
    *******************************************************************************/
   private void ensureInputFieldsAreInRequest(RunBackendStepInput runBackendStepInput, QBackendStepMetaData step) throws QException
   {
      QFunctionInputMetaData inputMetaData = step.getInputMetaData();
      if(inputMetaData == null)
      {
         return;
      }

      List<QFieldMetaData> fieldsToGet           = new ArrayList<>();
      List<QFieldMetaData> requiredFieldsMissing = new ArrayList<>();
      for(QFieldMetaData field : inputMetaData.getFieldList())
      {
         Serializable value = runBackendStepInput.getValue(field.getName());
         if(value == null)
         {
            if(field.getDefaultValue() != null)
            {
               runBackendStepInput.addValue(field.getName(), field.getDefaultValue());
            }
            else
            {
               fieldsToGet.add(field);
               if(field.getIsRequired())
               {
                  requiredFieldsMissing.add(field);
               }
            }
         }
      }

      if(!fieldsToGet.isEmpty())
      {
         QProcessCallback callback = runBackendStepInput.getCallback();
         if(callback == null)
         {
            if(requiredFieldsMissing.isEmpty())
            {
               ///////////////////////////////////////////////////////////////
               // if no required fields are missing, just return gracefully //
               ///////////////////////////////////////////////////////////////
               return;
            }

            ///////////////////////////////////////////////////////////////////
            // but if any required fields ARE missing, then that's an error. //
            ///////////////////////////////////////////////////////////////////
            LOG.info("Missing value for required fields: " + requiredFieldsMissing.stream().map(QFieldMetaData::getName).collect(Collectors.joining(", ")));
            throw (new QUserFacingException("Missing values for one or more fields",
               new QException("Function is missing values for fields, but no callback was present to request fields from a user")));
         }

         Map<String, Serializable> fieldValues = callback.getFieldValues(fieldsToGet);
         if(fieldValues != null)
         {
            for(Map.Entry<String, Serializable> entry : fieldValues.entrySet())
            {
               runBackendStepInput.addValue(entry.getKey(), entry.getValue());
               // todo - check to make sure got values back?
            }
         }
      }
   }



   /*******************************************************************************
    ** check if this step uses a record list - and if so, if we need to get one
    ** via the callback
    *******************************************************************************/
   private void ensureRecordsAreInRequest(RunBackendStepInput runBackendStepInput, QBackendStepMetaData step, QProcessMetaData process) throws QException
   {
      QFunctionInputMetaData inputMetaData = step.getInputMetaData();
      if(inputMetaData != null && inputMetaData.getRecordListMetaData() != null)
      {
         if(CollectionUtils.nullSafeIsEmpty(runBackendStepInput.getRecords()))
         {
            QTableMetaData table      = QContext.getQInstance().getTable(inputMetaData.getRecordListMetaData().getTableName());
            QueryInput     queryInput = new QueryInput();
            queryInput.setTableName(table.getName());
            queryInput.setInputSource(runBackendStepInput.getInputSource());

            //////////////////////////////////////////////////
            // look for record ids in the input data values //
            //////////////////////////////////////////////////
            String recordIds = runBackendStepInput.getValueString("recordIds");
            if(recordIds == null)
            {
               recordIds = runBackendStepInput.getValueString("recordId");
            }

            ///////////////////////////////////////////////////////////
            // if records were found, add as criteria to query input //
            ///////////////////////////////////////////////////////////
            if(recordIds != null)
            {
               queryInput.setFilter(new QQueryFilter(new QFilterCriteria(table.getPrimaryKeyField(), QCriteriaOperator.IN, recordIds.split(","))));
            }
            else
            {
               // todo - handle this being async (e.g., http)
               // seems like it just needs to throw, breaking this flow, and to send a response to the frontend, directing it to prompt the user for the needed data
               // then this step can re-run, hopefully with the needed data.
               QProcessCallback callback = runBackendStepInput.getCallback();
               if(callback == null)
               {
                  throw (new QUserFacingException("Missing input records.",
                     new QException("Function is missing input records, but no callback was present to request fields from a user")));
               }

               queryInput.setFilter(callback.getQueryFilter());
            }

            //////////////////////////////////////////////////////////////////////////////////////////
            // if process has a max-no of records, set a limit on the process of that number plus 1 //
            // (the plus 1 being so we can see "oh, you selected more than that many; error!"       //
            //////////////////////////////////////////////////////////////////////////////////////////
            if(process.getMaxInputRecords() != null)
            {
               if(queryInput.getFilter() == null)
               {
                  queryInput.setFilter(new QQueryFilter());
               }

               queryInput.getFilter().setLimit(process.getMaxInputRecords() + 1);
            }

            /////////////////////////////////////////////////////////////////////////////////
            // allow callback to customize the query input just here before the query runs //
            /////////////////////////////////////////////////////////////////////////////////
            if(runBackendStepInput.getCallback() != null)
            {
               runBackendStepInput.getCallback().customizeInputPreQuery(runBackendStepInput, queryInput);
            }

            if(runBackendStepInput.getInputSource() == QInputSource.USER)
            {
               PermissionsHelper.checkTablePermissionThrowing(queryInput, TablePermissionSubType.READ);
               PermissionsHelper.checkJoinedTableReadPermissions(queryInput, queryInput.getQueryJoins(), queryInput.getFilter());
            }

            QueryOutput queryOutput = new QueryAction().execute(queryInput);
            runBackendStepInput.setRecords(queryOutput.getRecords());

            ////////////////////////////////////////////////////////////////////////////////
            // if process defines a max, and more than the max were found, throw an error //
            ////////////////////////////////////////////////////////////////////////////////
            if(process.getMaxInputRecords() != null)
            {
               if(queryOutput.getRecords().size() > process.getMaxInputRecords())
               {
                  throw (new QUserFacingException("Too many records were selected for this process.  At most, only " + process.getMaxInputRecords() + " can be selected."));
               }
            }

            /////////////////////////////////////////////////////////////////////////////////
            // if process defines a min, and fewer than the min were found, throw an error //
            /////////////////////////////////////////////////////////////////////////////////
            if(process.getMinInputRecords() != null)
            {
               if(queryOutput.getRecords().size() < process.getMinInputRecords())
               {
                  throw (new QUserFacingException("Too few records were selected for this process.  At least " + process.getMinInputRecords() + " must be selected."));
               }
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private RunBackendStepOutput runStepCode(QCodeReference code, RunBackendStepInput runBackendStepInput)
   {
      RunBackendStepOutput runBackendStepOutput = new RunBackendStepOutput();
      try
      {
         runBackendStepOutput.seedFromRequest(runBackendStepInput);

         Object codeObject;
         if(code instanceof QCodeReferenceLambda<?> qCodeReferenceLambda)
         {
            codeObject = qCodeReferenceLambda.getLambda();
         }
         else
         {
            Class<?> codeClass = Class.forName(code.getName());
            codeObject = codeClass.getConstructor().newInstance();
         }

         if(!(codeObject instanceof BackendStep backendStepCodeObject))
         {
            throw (new QException("The supplied codeReference [" + code + "] is not a reference to a BackendStep"));
         }

         backendStepCodeObject.run(runBackendStepInput, runBackendStepOutput);
      }
      catch(Exception e)
      {
         runBackendStepOutput = new RunBackendStepOutput();
         runBackendStepOutput.setException(e);
         LOG.info("Error running backend step code", e);
      }

      return (runBackendStepOutput);
   }
}
