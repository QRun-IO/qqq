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
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;


/*******************************************************************************
 **
 *******************************************************************************/
public abstract class AbstractFilterExpression<T extends Serializable> implements Serializable
{
   /*******************************************************************************
    **
    *******************************************************************************/
   public abstract T evaluate(QFieldMetaData field) throws QException;



   /*******************************************************************************
    ** Evaluate the expression, given a map of input values.
    **
    ** By default, this will defer to the evaluate(void) method - but, a subclass
    ** (e.g., FilterVariableExpression) may react differently.
    *******************************************************************************/
   public T evaluateInputValues(Map<String, Serializable> inputValues) throws QException
   {
      return evaluate(null);
   }



   /*******************************************************************************
    ** To help with serialization, define a "type" in all subclasses
    *******************************************************************************/
   public String getType()
   {
      return (getClass().getSimpleName());
   }



   /*******************************************************************************
    ** noop - but here so serialization won't be upset about there being a type
    ** in a json object.
    *******************************************************************************/
   public void setType(String type)
   {

   }
}
