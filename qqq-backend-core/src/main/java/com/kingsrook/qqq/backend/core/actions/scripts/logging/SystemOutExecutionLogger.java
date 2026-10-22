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

package com.kingsrook.qqq.backend.core.actions.scripts.logging;


import java.io.Serializable;
import com.kingsrook.qqq.backend.core.model.actions.scripts.ExecuteCodeInput;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Implementation of a code execution logger that logs to System.out and
 ** System.err (for exceptions)
 *******************************************************************************/
public class SystemOutExecutionLogger implements QCodeExecutionLoggerInterface
{
   private QCodeReference qCodeReference;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void acceptExecutionStart(ExecuteCodeInput executeCodeInput)
   {
      this.qCodeReference = executeCodeInput.getCodeReference();

      String inputString = ValueUtils.getValueAsString(executeCodeInput.getInput());
      System.out.println("Starting script execution: " + qCodeReference.getName() + ", with input: " + inputString);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void acceptLogLine(String logLine)
   {
      System.out.println("Script log: " + logLine);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void acceptException(Exception exception)
   {
      System.out.println("Script Exception: " + exception.getMessage());
      exception.printStackTrace();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void acceptExecutionEnd(Serializable output)
   {
      String outputString = ValueUtils.getValueAsString(output);
      System.out.println("Finished script execution: " + qCodeReference.getName() + ", with output: " + outputString);
   }

}
