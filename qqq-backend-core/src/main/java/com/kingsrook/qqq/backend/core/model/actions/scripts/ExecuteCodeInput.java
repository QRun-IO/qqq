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

package com.kingsrook.qqq.backend.core.model.actions.scripts;


import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.scripts.logging.QCodeExecutionLoggerInterface;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;


/*******************************************************************************
 **
 *******************************************************************************/
public class ExecuteCodeInput extends AbstractActionInput
{
   private QCodeReference                codeReference;
   private Map<String, Serializable>     input;
   private Map<String, Serializable>     context;
   private QCodeExecutionLoggerInterface executionLogger;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public ExecuteCodeInput()
   {
   }



   /*******************************************************************************
    ** Getter for codeReference
    **
    *******************************************************************************/
   public QCodeReference getCodeReference()
   {
      return codeReference;
   }



   /*******************************************************************************
    ** Setter for codeReference
    **
    *******************************************************************************/
   public void setCodeReference(QCodeReference codeReference)
   {
      this.codeReference = codeReference;
   }



   /*******************************************************************************
    ** Fluent setter for codeReference
    **
    *******************************************************************************/
   public ExecuteCodeInput withCodeReference(QCodeReference codeReference)
   {
      this.codeReference = codeReference;
      return (this);
   }



   /*******************************************************************************
    ** Getter for input
    **
    *******************************************************************************/
   public Map<String, Serializable> getInput()
   {
      return input;
   }



   /*******************************************************************************
    ** Setter for input
    **
    *******************************************************************************/
   public void setInput(Map<String, Serializable> input)
   {
      this.input = input;
   }



   /*******************************************************************************
    ** Fluent setter for input
    **
    *******************************************************************************/
   public ExecuteCodeInput withInput(Map<String, Serializable> input)
   {
      this.input = input;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for input
    **
    *******************************************************************************/
   public ExecuteCodeInput withInput(String key, Serializable value)
   {
      if(this.input == null)
      {
         input = new HashMap<>();
      }
      this.input.put(key, value);
      return (this);
   }



   /*******************************************************************************
    ** Getter for context
    **
    *******************************************************************************/
   public Map<String, Serializable> getContext()
   {
      return context;
   }



   /*******************************************************************************
    ** Setter for context
    **
    *******************************************************************************/
   public void setContext(Map<String, Serializable> context)
   {
      this.context = context;
   }



   /*******************************************************************************
    ** Fluent setter for context
    **
    *******************************************************************************/
   public ExecuteCodeInput withContext(Map<String, Serializable> context)
   {
      this.context = context;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for context
    **
    *******************************************************************************/
   public ExecuteCodeInput withContext(String key, Serializable value)
   {
      if(this.context == null)
      {
         context = new HashMap<>();
      }
      this.context.put(key, value);
      return (this);
   }



   /*******************************************************************************
    ** Getter for executionLogger
    **
    *******************************************************************************/
   public QCodeExecutionLoggerInterface getExecutionLogger()
   {
      return executionLogger;
   }



   /*******************************************************************************
    ** Setter for executionLogger
    **
    *******************************************************************************/
   public void setExecutionLogger(QCodeExecutionLoggerInterface executionLogger)
   {
      this.executionLogger = executionLogger;
   }



   /*******************************************************************************
    ** Fluent setter for executionLogger
    **
    *******************************************************************************/
   public ExecuteCodeInput withExecutionLogger(QCodeExecutionLoggerInterface executionLogger)
   {
      this.executionLogger = executionLogger;
      return (this);
   }

}
