/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.scripts.logging.QCodeExecutionLoggerInterface;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;


/*******************************************************************************
 ** Base class for input wrappers that end up running scripts (ExecuteCodeAction)
 *******************************************************************************/
public class AbstractRunScriptInput<C extends QCodeReference> extends AbstractTableActionInput
{
   private C                             codeReference;
   private Map<String, Serializable>     inputValues;
   private QCodeExecutionLoggerInterface logger;
   private Serializable                  outputObject;
   private Serializable                  scriptUtils;



   /*******************************************************************************
    ** Getter for codeReference
    *******************************************************************************/
   public C getCodeReference()
   {
      return (this.codeReference);
   }



   /*******************************************************************************
    ** Setter for codeReference
    *******************************************************************************/
   public void setCodeReference(C codeReference)
   {
      this.codeReference = codeReference;
   }



   /*******************************************************************************
    ** Fluent setter for codeReference
    *******************************************************************************/
   public AbstractRunScriptInput<C> withCodeReference(C codeReference)
   {
      this.codeReference = codeReference;
      return (this);
   }



   /*******************************************************************************
    ** Getter for inputValues
    *******************************************************************************/
   public Map<String, Serializable> getInputValues()
   {
      return (this.inputValues);
   }



   /*******************************************************************************
    ** Setter for inputValues
    *******************************************************************************/
   public void setInputValues(Map<String, Serializable> inputValues)
   {
      this.inputValues = inputValues;
   }



   /*******************************************************************************
    ** Fluent setter for inputValues
    *******************************************************************************/
   public AbstractRunScriptInput<C> withInputValues(Map<String, Serializable> inputValues)
   {
      this.inputValues = inputValues;
      return (this);
   }



   /*******************************************************************************
    ** Getter for logger
    *******************************************************************************/
   public QCodeExecutionLoggerInterface getLogger()
   {
      return (this.logger);
   }



   /*******************************************************************************
    ** Setter for logger
    *******************************************************************************/
   public void setLogger(QCodeExecutionLoggerInterface logger)
   {
      this.logger = logger;
   }



   /*******************************************************************************
    ** Fluent setter for logger
    *******************************************************************************/
   public AbstractRunScriptInput<C> withLogger(QCodeExecutionLoggerInterface logger)
   {
      this.logger = logger;
      return (this);
   }



   /*******************************************************************************
    ** Getter for outputObject
    *******************************************************************************/
   public Serializable getOutputObject()
   {
      return (this.outputObject);
   }



   /*******************************************************************************
    ** Setter for outputObject
    *******************************************************************************/
   public void setOutputObject(Serializable outputObject)
   {
      this.outputObject = outputObject;
   }



   /*******************************************************************************
    ** Fluent setter for outputObject
    *******************************************************************************/
   public AbstractRunScriptInput<C> withOutputObject(Serializable outputObject)
   {
      this.outputObject = outputObject;
      return (this);
   }



   /*******************************************************************************
    ** Getter for scriptUtils
    *******************************************************************************/
   public Serializable getScriptUtils()
   {
      return (this.scriptUtils);
   }



   /*******************************************************************************
    ** Setter for scriptUtils
    *******************************************************************************/
   public void setScriptUtils(Serializable scriptUtils)
   {
      this.scriptUtils = scriptUtils;
   }



   /*******************************************************************************
    ** Fluent setter for scriptUtils
    *******************************************************************************/
   public AbstractRunScriptInput<C> withScriptUtils(Serializable scriptUtils)
   {
      this.scriptUtils = scriptUtils;
      return (this);
   }

}
