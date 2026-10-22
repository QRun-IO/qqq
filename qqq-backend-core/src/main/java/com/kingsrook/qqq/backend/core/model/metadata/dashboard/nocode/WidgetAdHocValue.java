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

package com.kingsrook.qqq.backend.core.model.metadata.dashboard.nocode;


import java.io.Serializable;
import java.util.Map;
import java.util.function.Function;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;


/*******************************************************************************
 **
 *******************************************************************************/
public class WidgetAdHocValue extends AbstractWidgetValueSource
{
   private QCodeReference codeReference;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public WidgetAdHocValue()
   {
      setType(getClass().getSimpleName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Object evaluate(Map<String, Object> context, RenderWidgetInput input) throws QException
   {
      if(inputValues != null)
      {
         context.putAll(inputValues);
      }

      @SuppressWarnings("unchecked")
      Function<Object, Object> function = QCodeLoader.getAdHoc(Function.class, codeReference);
      Object                   result   = function.apply(context);
      return (result);
   }



   /*******************************************************************************
    ** Fluent setter for name
    *******************************************************************************/
   @Override
   public WidgetAdHocValue withName(String name)
   {
      setName(name);
      return (this);
   }



   /*******************************************************************************
    ** Getter for codeReference
    *******************************************************************************/
   public QCodeReference getCodeReference()
   {
      return (this.codeReference);
   }



   /*******************************************************************************
    ** Setter for codeReference
    *******************************************************************************/
   public void setCodeReference(QCodeReference codeReference)
   {
      this.codeReference = codeReference;
   }



   /*******************************************************************************
    ** Fluent setter for codeReference
    *******************************************************************************/
   public WidgetAdHocValue withCodeReference(QCodeReference codeReference)
   {
      this.codeReference = codeReference;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for inputValues
    *******************************************************************************/
   @Override
   public WidgetAdHocValue withInputValues(Map<String, Serializable> inputValues)
   {
      this.inputValues = inputValues;
      return (this);
   }

}
