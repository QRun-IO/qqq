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

package com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions;


import java.io.Serializable;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class FilterVariableExpression extends AbstractFilterExpression<Serializable>
{
   private String variableName;
   private String fieldName;
   private String operator;
   private int    valueIndex = 0;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Serializable evaluate(QFieldMetaData field) throws QException
   {
      throw (new QUserFacingException("Missing variable value."));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Serializable evaluateInputValues(Map<String, Serializable> inputValues) throws QException
   {
      if(!inputValues.containsKey(variableName) || "".equals(ValueUtils.getValueAsString(inputValues.get(variableName))))
      {
         throw (new QUserFacingException("Missing value for variable: " + variableName));
      }
      return (inputValues.get(variableName));
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public FilterVariableExpression()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   private FilterVariableExpression(String fieldName, int valueIndex)
   {
      this.fieldName = fieldName;
      this.valueIndex = valueIndex;
   }



   /*******************************************************************************
    ** Getter for valueIndex
    *******************************************************************************/
   public int getValueIndex()
   {
      return (this.valueIndex);
   }



   /*******************************************************************************
    ** Setter for valueIndex
    *******************************************************************************/
   public void setValueIndex(int valueIndex)
   {
      this.valueIndex = valueIndex;
   }



   /*******************************************************************************
    ** Fluent setter for valueIndex
    *******************************************************************************/
   public FilterVariableExpression withValueIndex(int valueIndex)
   {
      this.valueIndex = valueIndex;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fieldName
    *******************************************************************************/
   public String getFieldName()
   {
      return (this.fieldName);
   }



   /*******************************************************************************
    ** Setter for fieldName
    *******************************************************************************/
   public void setFieldName(String fieldName)
   {
      this.fieldName = fieldName;
   }



   /*******************************************************************************
    ** Fluent setter for fieldName
    *******************************************************************************/
   public FilterVariableExpression withFieldName(String fieldName)
   {
      this.fieldName = fieldName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for variableName
    *******************************************************************************/
   public String getVariableName()
   {
      return (this.variableName);
   }



   /*******************************************************************************
    ** Setter for variableName
    *******************************************************************************/
   public void setVariableName(String variableName)
   {
      this.variableName = variableName;
   }



   /*******************************************************************************
    ** Fluent setter for variableName
    *******************************************************************************/
   public FilterVariableExpression withVariableName(String variableName)
   {
      this.variableName = variableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for operator
    *******************************************************************************/
   public String getOperator()
   {
      return (this.operator);
   }



   /*******************************************************************************
    ** Setter for operator
    *******************************************************************************/
   public void setOperator(String operator)
   {
      this.operator = operator;
   }



   /*******************************************************************************
    ** Fluent setter for operator
    *******************************************************************************/
   public FilterVariableExpression withOperator(String operator)
   {
      this.operator = operator;
      return (this);
   }

}
