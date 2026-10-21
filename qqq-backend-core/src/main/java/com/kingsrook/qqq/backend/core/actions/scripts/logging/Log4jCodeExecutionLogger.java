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
import java.util.UUID;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.scripts.ExecuteCodeInput;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Implementation of a code execution logger that logs to LOG 4j
 *******************************************************************************/
public class Log4jCodeExecutionLogger implements QCodeExecutionLoggerInterface
{
   private static final QLogger LOG = QLogger.getLogger(Log4jCodeExecutionLogger.class);

   private QCodeReference qCodeReference;
   private String         uuid = UUID.randomUUID().toString();

   private boolean includeUUID = true;


   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void acceptExecutionStart(ExecuteCodeInput executeCodeInput)
   {
      this.qCodeReference = executeCodeInput.getCodeReference();

      String inputString = StringUtils.safeTruncate(ValueUtils.getValueAsString(executeCodeInput.getInput()), 250, "...");
      LOG.info("Starting script execution: " + qCodeReference.getName() + (includeUUID ? ", uuid: " + uuid : "") + ", with input: " + inputString);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void acceptLogLine(String logLine)
   {
      LOG.info("Script log: " + (includeUUID ? uuid + ": " : "") + logLine);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void acceptException(Exception exception)
   {
      LOG.info("Script Exception: " + (includeUUID ? uuid : ""), exception);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void acceptExecutionEnd(Serializable output)
   {
      String outputString = StringUtils.safeTruncate(ValueUtils.getValueAsString(output), 250, "...");
      LOG.info("Finished script execution: " + qCodeReference.getName() + (includeUUID ? ", uuid: " + uuid : "") + ", with output: " + outputString);
   }



   /*******************************************************************************
    ** Getter for includeUUID
    *******************************************************************************/
   public boolean getIncludeUUID()
   {
      return (this.includeUUID);
   }



   /*******************************************************************************
    ** Setter for includeUUID
    *******************************************************************************/
   public void setIncludeUUID(boolean includeUUID)
   {
      this.includeUUID = includeUUID;
   }



   /*******************************************************************************
    ** Fluent setter for includeUUID
    *******************************************************************************/
   public Log4jCodeExecutionLogger withIncludeUUID(boolean includeUUID)
   {
      this.includeUUID = includeUUID;
      return (this);
   }

}
