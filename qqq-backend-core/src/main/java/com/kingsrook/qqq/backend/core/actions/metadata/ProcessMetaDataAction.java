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

package com.kingsrook.qqq.backend.core.actions.metadata;


import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.model.actions.metadata.ProcessMetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.ProcessMetaDataOutput;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;


/*******************************************************************************
 ** Action to fetch meta-data for a process.
 **
 *******************************************************************************/
public class ProcessMetaDataAction
{
   /*******************************************************************************
    **
    *******************************************************************************/
   public ProcessMetaDataOutput execute(ProcessMetaDataInput processMetaDataInput) throws QException
   {
      ActionHelper.validateSession(processMetaDataInput);

      // todo pre-customization - just get to modify the request?
      ProcessMetaDataOutput processMetaDataOutput = new ProcessMetaDataOutput();

      QProcessMetaData process = QContext.getQInstance().getProcess(processMetaDataInput.getProcessName());
      if(process == null)
      {
         throw (new QNotFoundException("Process [" + processMetaDataInput.getProcessName() + "] was not found."));
      }
      processMetaDataOutput.setProcess(new QFrontendProcessMetaData(processMetaDataInput, process, true));

      // todo post-customization - can do whatever w/ the result if you want

      return processMetaDataOutput;
   }
}
