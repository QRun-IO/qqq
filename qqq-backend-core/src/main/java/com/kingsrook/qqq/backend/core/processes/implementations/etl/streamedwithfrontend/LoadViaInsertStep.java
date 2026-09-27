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

package com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend;


import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.InputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;


/*******************************************************************************
 ** Generic implementation of a LoadStep - that runs an Insert action for a
 ** specified table.
 *******************************************************************************/
public class LoadViaInsertStep extends AbstractLoadStep
{
   public static final String FIELD_DESTINATION_TABLE     = "destinationTable";
   public static final String FIELD_SKIP_UNIQUE_KEY_CHECK = "skipUniqueKeyCheck";



   /*******************************************************************************
    **
    *******************************************************************************/
   protected InputSource getInputSource()
   {
      return (QInputSource.SYSTEM);
   }



   /*******************************************************************************
    ** Execute the backend step - using the request as input, and the result as output.
    **
    *******************************************************************************/
   @Override
   public void runOnePage(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setInputSource(getInputSource());
      insertInput.setTableName(runBackendStepInput.getValueString(FIELD_DESTINATION_TABLE));
      insertInput.setRecords(runBackendStepInput.getRecords());
      getTransaction().ifPresent(insertInput::setTransaction);
      insertInput.setAsyncJobCallback(runBackendStepInput.getAsyncJobCallback());

      if(runBackendStepInput.getValuePrimitiveBoolean(FIELD_SKIP_UNIQUE_KEY_CHECK))
      {
         insertInput.setSkipUniqueKeyCheck(true);
      }

      InsertOutput insertOutput = new InsertAction().execute(insertInput);
      runBackendStepOutput.getRecords().addAll(insertOutput.getRecords());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Optional<QBackendTransaction> openTransaction(RunBackendStepInput runBackendStepInput) throws QException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(runBackendStepInput.getValueString(FIELD_DESTINATION_TABLE));
      return (Optional.of(QBackendTransaction.openFor(insertInput)));
   }
}
