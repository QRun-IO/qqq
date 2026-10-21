/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.inputfield;


import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.BlockValuesInterface;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;


/*******************************************************************************
 **
 *******************************************************************************/
public class InputFieldValues implements BlockValuesInterface
{
   private QFieldMetaData fieldMetaData;

   private Boolean autoFocus;
   private Boolean submitOnEnter;
   private Boolean hideSoftKeyboard;
   private String  placeholder;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public InputFieldValues()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public InputFieldValues(QFieldMetaData fieldMetaData)
   {
      setFieldMetaData(fieldMetaData);
   }



   /*******************************************************************************
    ** Getter for fieldMetaData
    *******************************************************************************/
   public QFieldMetaData getFieldMetaData()
   {
      return (this.fieldMetaData);
   }



   /*******************************************************************************
    ** Setter for fieldMetaData
    *******************************************************************************/
   public void setFieldMetaData(QFieldMetaData fieldMetaData)
   {
      this.fieldMetaData = fieldMetaData;
   }



   /*******************************************************************************
    ** Fluent setter for fieldMetaData
    *******************************************************************************/
   public InputFieldValues withFieldMetaData(QFieldMetaData fieldMetaData)
   {
      this.fieldMetaData = fieldMetaData;
      return (this);
   }



   /*******************************************************************************
    ** Getter for autoFocus
    *******************************************************************************/
   public Boolean getAutoFocus()
   {
      return (this.autoFocus);
   }



   /*******************************************************************************
    ** Setter for autoFocus
    *******************************************************************************/
   public void setAutoFocus(Boolean autoFocus)
   {
      this.autoFocus = autoFocus;
   }



   /*******************************************************************************
    ** Fluent setter for autoFocus
    *******************************************************************************/
   public InputFieldValues withAutoFocus(Boolean autoFocus)
   {
      this.autoFocus = autoFocus;
      return (this);
   }



   /*******************************************************************************
    ** Getter for submitOnEnter
    *******************************************************************************/
   public Boolean getSubmitOnEnter()
   {
      return (this.submitOnEnter);
   }



   /*******************************************************************************
    ** Setter for submitOnEnter
    *******************************************************************************/
   public void setSubmitOnEnter(Boolean submitOnEnter)
   {
      this.submitOnEnter = submitOnEnter;
   }



   /*******************************************************************************
    ** Fluent setter for submitOnEnter
    *******************************************************************************/
   public InputFieldValues withSubmitOnEnter(Boolean submitOnEnter)
   {
      this.submitOnEnter = submitOnEnter;
      return (this);
   }



   /*******************************************************************************
    ** Getter for placeholder
    *******************************************************************************/
   public String getPlaceholder()
   {
      return (this.placeholder);
   }



   /*******************************************************************************
    ** Setter for placeholder
    *******************************************************************************/
   public void setPlaceholder(String placeholder)
   {
      this.placeholder = placeholder;
   }



   /*******************************************************************************
    ** Fluent setter for placeholder
    *******************************************************************************/
   public InputFieldValues withPlaceholder(String placeholder)
   {
      this.placeholder = placeholder;
      return (this);
   }


   /*******************************************************************************
    ** Getter for hideSoftKeyboard
    *******************************************************************************/
   public Boolean getHideSoftKeyboard()
   {
      return (this.hideSoftKeyboard);
   }



   /*******************************************************************************
    ** Setter for hideSoftKeyboard
    *******************************************************************************/
   public void setHideSoftKeyboard(Boolean hideSoftKeyboard)
   {
      this.hideSoftKeyboard = hideSoftKeyboard;
   }



   /*******************************************************************************
    ** Fluent setter for hideSoftKeyboard
    *******************************************************************************/
   public InputFieldValues withHideSoftKeyboard(Boolean hideSoftKeyboard)
   {
      this.hideSoftKeyboard = hideSoftKeyboard;
      return (this);
   }


}
