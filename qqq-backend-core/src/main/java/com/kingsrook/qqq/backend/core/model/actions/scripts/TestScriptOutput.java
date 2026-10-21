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
import java.util.List;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 **
 *******************************************************************************/
public class TestScriptOutput extends AbstractActionOutput
{
   private Serializable  outputObject;
   private Exception     exception;
   private QRecord       scriptLog;
   private List<QRecord> scriptLogLines;



   /*******************************************************************************
    ** Getter for outputObject
    **
    *******************************************************************************/
   public Serializable getOutputObject()
   {
      return outputObject;
   }



   /*******************************************************************************
    ** Setter for outputObject
    **
    *******************************************************************************/
   public void setOutputObject(Serializable outputObject)
   {
      this.outputObject = outputObject;
   }



   /*******************************************************************************
    ** Fluent setter for outputObject
    **
    *******************************************************************************/
   public TestScriptOutput withOutputObject(Serializable outputObject)
   {
      this.outputObject = outputObject;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setException(Exception exception)
   {
      this.exception = exception;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public Exception getException()
   {
      return exception;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setScriptLog(QRecord scriptLog)
   {
      this.scriptLog = scriptLog;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QRecord getScriptLog()
   {
      return scriptLog;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setScriptLogLines(List<QRecord> scriptLogLines)
   {
      this.scriptLogLines = scriptLogLines;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public List<QRecord> getScriptLogLines()
   {
      return scriptLogLines;
   }

}
