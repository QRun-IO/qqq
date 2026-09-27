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

package com.kingsrook.qqq.backend.core.model.metadata.processes;


import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;


/*******************************************************************************
 ** Meta-Data to define a backend-step in a process in a QQQ instance.  e.g.,
 ** code that runs on a server/backend, to do something to some data.
 **
 *******************************************************************************/
public class QBackendStepMetaData extends QStepMetaData
{
   private QFunctionInputMetaData  inputMetaData;
   private QFunctionOutputMetaData outputMetaData;
   private QCodeReference          code;



   /*******************************************************************************
    **
    *******************************************************************************/
   public QBackendStepMetaData()
   {
      setStepType("backend");
   }



   /*******************************************************************************
    ** Setter for label
    **
    *******************************************************************************/
   @Override
   public QBackendStepMetaData withName(String name)
   {
      setName(name);
      return (this);
   }



   /*******************************************************************************
    ** Getter for inputData
    **
    *******************************************************************************/
   public QFunctionInputMetaData getInputMetaData()
   {
      return inputMetaData;
   }



   /*******************************************************************************
    ** Setter for inputData
    **
    *******************************************************************************/
   public void setInputMetaData(QFunctionInputMetaData inputMetaData)
   {
      this.inputMetaData = inputMetaData;
   }



   /*******************************************************************************
    ** Setter for inputData
    **
    *******************************************************************************/
   public QBackendStepMetaData withInputData(QFunctionInputMetaData inputData)
   {
      this.inputMetaData = inputData;
      return (this);
   }



   /*******************************************************************************
    ** Getter for outputData
    **
    *******************************************************************************/
   public QFunctionOutputMetaData getOutputMetaData()
   {
      return outputMetaData;
   }



   /*******************************************************************************
    ** Setter for outputData
    **
    *******************************************************************************/
   public void setOutputMetaData(QFunctionOutputMetaData outputMetaData)
   {
      this.outputMetaData = outputMetaData;
   }



   /*******************************************************************************
    ** Setter for outputData
    **
    *******************************************************************************/
   public QBackendStepMetaData withOutputMetaData(QFunctionOutputMetaData outputMetaData)
   {
      this.outputMetaData = outputMetaData;
      return (this);
   }



   /*******************************************************************************
    ** Getter for code
    **
    *******************************************************************************/
   public QCodeReference getCode()
   {
      return code;
   }



   /*******************************************************************************
    ** Setter for code
    **
    *******************************************************************************/
   public void setCode(QCodeReference code)
   {
      this.code = code;
   }



   /*******************************************************************************
    ** Setter for code
    **
    *******************************************************************************/
   public QBackendStepMetaData withCode(QCodeReference code)
   {
      this.code = code;
      return (this);
   }



   /*******************************************************************************
    ** Get a list of all of the input fields used by this function
    *******************************************************************************/
   @JsonIgnore // because this is a computed property - we don't want it in our json.
   @Override
   public List<QFieldMetaData> getInputFields()
   {
      List<QFieldMetaData> rs = new ArrayList<>();
      if(inputMetaData != null && inputMetaData.getFieldList() != null)
      {
         rs.addAll(inputMetaData.getFieldList());
      }
      return (rs);
   }



   /*******************************************************************************
    ** Get a list of all of the output fields used by this function
    *******************************************************************************/
   @JsonIgnore // because this is a computed property - we don't want it in our json.
   @Override
   public List<QFieldMetaData> getOutputFields()
   {
      List<QFieldMetaData> rs = new ArrayList<>();
      if(outputMetaData != null && outputMetaData.getFieldList() != null)
      {
         rs.addAll(outputMetaData.getFieldList());
      }
      return (rs);
   }

}
