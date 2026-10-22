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

package com.kingsrook.qqq.backend.core.model.actions.tables.search;


import java.io.Serializable;


/*******************************************************************************
 ** A secondary field value of a record search result (a search field that is
 ** not part of the record label), for display under the record label.
 *******************************************************************************/
public class RecordSearchDetail implements Serializable
{
   private static final long serialVersionUID = 1L;

   private String fieldName;
   private String fieldLabel;
   private String displayValue;



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
   public RecordSearchDetail withFieldName(String fieldName)
   {
      this.fieldName = fieldName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fieldLabel
    *******************************************************************************/
   public String getFieldLabel()
   {
      return (this.fieldLabel);
   }



   /*******************************************************************************
    ** Setter for fieldLabel
    *******************************************************************************/
   public void setFieldLabel(String fieldLabel)
   {
      this.fieldLabel = fieldLabel;
   }



   /*******************************************************************************
    ** Fluent setter for fieldLabel
    *******************************************************************************/
   public RecordSearchDetail withFieldLabel(String fieldLabel)
   {
      this.fieldLabel = fieldLabel;
      return (this);
   }



   /*******************************************************************************
    ** Getter for displayValue
    *******************************************************************************/
   public String getDisplayValue()
   {
      return (this.displayValue);
   }



   /*******************************************************************************
    ** Setter for displayValue
    *******************************************************************************/
   public void setDisplayValue(String displayValue)
   {
      this.displayValue = displayValue;
   }



   /*******************************************************************************
    ** Fluent setter for displayValue
    *******************************************************************************/
   public RecordSearchDetail withDisplayValue(String displayValue)
   {
      this.displayValue = displayValue;
      return (this);
   }

}
