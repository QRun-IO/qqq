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


/*******************************************************************************
 ** Interface to provide logging functionality to QCodeExecution (e.g., scripts)
 *******************************************************************************/
public interface QCodeExecutionLoggerInterface extends Serializable
{

   /*******************************************************************************
    ** Called when the execution starts - takes the execution's input object.
    *******************************************************************************/
   void acceptExecutionStart(ExecuteCodeInput executeCodeInput);

   /*******************************************************************************
    ** Called to log a line, a message.
    *******************************************************************************/
   void acceptLogLine(String logLine);

   /*******************************************************************************
    ** In case the loggerInterface object is provided to the script as context,
    ** this method gives a clean interface for the script to log a line.
    *******************************************************************************/
   default void log(String message)
   {
      acceptLogLine(message);
   }

   /*******************************************************************************
    ** Called if the script fails with an exception.
    *******************************************************************************/
   void acceptException(Exception exception);

   /*******************************************************************************
    ** Called if the script completes without exception.
    *******************************************************************************/
   void acceptExecutionEnd(Serializable output);

}
