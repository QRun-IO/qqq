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

package com.kingsrook.qqq.middleware.javalin.executors;


import com.kingsrook.qqq.backend.core.actions.metadata.ProcessMetaDataAction;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.model.actions.metadata.ProcessMetaDataOutput;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.middleware.javalin.executors.io.ProcessMetaDataInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.ProcessMetaDataOutputInterface;


/*******************************************************************************
 **
 *******************************************************************************/
public class ProcessMetaDataExecutor extends AbstractMiddlewareExecutor<ProcessMetaDataInput, ProcessMetaDataOutputInterface>
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void execute(ProcessMetaDataInput input, ProcessMetaDataOutputInterface output) throws QException
   {
      com.kingsrook.qqq.backend.core.model.actions.metadata.ProcessMetaDataInput processMetaDataInput = new com.kingsrook.qqq.backend.core.model.actions.metadata.ProcessMetaDataInput();

      String           processName = input.getProcessName();
      QProcessMetaData process     = QContext.getQInstance().getProcess(processName);
      if(process == null)
      {
         throw (new QNotFoundException("Process [" + processName + "] was not found."));
      }
      PermissionsHelper.checkProcessPermissionThrowing(processMetaDataInput, processName);

      processMetaDataInput.setProcessName(processName);
      ProcessMetaDataAction processMetaDataAction = new ProcessMetaDataAction();
      ProcessMetaDataOutput processMetaDataOutput = processMetaDataAction.execute(processMetaDataInput);

      output.setProcessMetaData(processMetaDataOutput.getProcess());
   }

}
