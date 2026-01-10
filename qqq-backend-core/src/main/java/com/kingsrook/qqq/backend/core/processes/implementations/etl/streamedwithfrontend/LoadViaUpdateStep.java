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

package com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend;


import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.InputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateOutput;
import org.apache.commons.lang3.BooleanUtils;


/*******************************************************************************
 ** Generic implementation of a LoadStep - that runs an Update action for a
 ** specified table.
 *******************************************************************************/
public class LoadViaUpdateStep extends AbstractLoadStep
{
   public static final String FIELD_DESTINATION_TABLE               = "destinationTable";
   public static final String DO_NOT_UPDATE_MODIFY_DATE_FIELD_NAME  = "doNotUpdateModifyDateFieldName";
   public static final String DO_NOT_TRIGGER_AUTOMATIONS_FIELD_NAME = "doNotTriggerAutomationsFieldName";



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
      UpdateInput updateInput = new UpdateInput();
      updateInput.setInputSource(getInputSource());
      updateInput.setTableName(runBackendStepInput.getValueString(FIELD_DESTINATION_TABLE));
      updateInput.setRecords(runBackendStepInput.getRecords());
      getTransaction().ifPresent(updateInput::setTransaction);
      updateInput.setAsyncJobCallback(runBackendStepInput.getAsyncJobCallback());

      //////////////////////////////////////////////////////////////////////////////////////////
      // look for flags in the input to either not update modify dates or not run automations //
      //////////////////////////////////////////////////////////////////////////////////////////
      boolean doNotUpdateModifyDate   = BooleanUtils.isTrue(runBackendStepInput.getValueBoolean(LoadViaUpdateStep.DO_NOT_UPDATE_MODIFY_DATE_FIELD_NAME));
      boolean doNotTriggerAutomations = BooleanUtils.isTrue(runBackendStepInput.getValueBoolean(LoadViaUpdateStep.DO_NOT_TRIGGER_AUTOMATIONS_FIELD_NAME));
      updateInput.setOmitModifyDateUpdate(doNotUpdateModifyDate);
      updateInput.setOmitTriggeringAutomations(doNotTriggerAutomations);

      UpdateOutput updateOutput = new UpdateAction().execute(updateInput);
      runBackendStepOutput.getRecords().addAll(updateOutput.getRecords());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Optional<QBackendTransaction> openTransaction(RunBackendStepInput runBackendStepInput) throws QException
   {
      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName(runBackendStepInput.getValueString(FIELD_DESTINATION_TABLE));
      return (Optional.of(QBackendTransaction.openFor(updateInput)));
   }
}
