/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.ai.tools;


import com.kingsrook.qqq.backend.core.actions.ai.AiToolOutcome;
import com.kingsrook.qqq.backend.core.exceptions.QException;


/*******************************************************************************
 ** Thrown by an AI tool to end its call with a non-OK outcome (DENIED,
 ** NOT_FOUND, INVALID_INPUT) and a polite message for the model.  The kernel
 ** turns it into an {@link com.kingsrook.qqq.backend.core.actions.ai.AiToolResult}
 ** - it never escapes AiToolKernel.execute.
 **
 ** The message is sent to the model, so it must never contain data the
 ** session may not read.
 *******************************************************************************/
public class AiToolException extends QException
{
   private final AiToolOutcome outcome;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public AiToolException(AiToolOutcome outcome, String message)
   {
      super(message);
      this.outcome = outcome;
   }



   /*******************************************************************************
    ** An INVALID_INPUT exception.
    *******************************************************************************/
   public static AiToolException invalidInput(String message)
   {
      return (new AiToolException(AiToolOutcome.INVALID_INPUT, message));
   }



   /*******************************************************************************
    ** Getter for outcome
    *******************************************************************************/
   public AiToolOutcome getOutcome()
   {
      return (this.outcome);
   }
}
