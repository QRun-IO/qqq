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

package com.kingsrook.qqq.backend.core.model.dashboard.widgets;


import java.util.List;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;


/*******************************************************************************
 **
 *******************************************************************************/
public class DynamicFormWidgetData extends QWidgetData
{
   private List<QFieldMetaData> fieldList;

   /////////////////////////////////////////////////////////////////////
   // values for the fields -                                         //
   // use a QRecord, so we can do "richer" things, like DisplayValues //
   /////////////////////////////////////////////////////////////////////
   private QRecord recordOfFieldValues;

   /////////////////////////////////////////////////////
   // if there are no fields, what message to display //
   /////////////////////////////////////////////////////
   private String noFieldsMessage;

   ///////////////////////////////////////////////////////////////////////////////////
   // what 1 field do we want to combine the dynamic fields into (as a JSON string) //
   ///////////////////////////////////////////////////////////////////////////////////
   private String mergedDynamicFormValuesIntoFieldName;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getType()
   {
      return WidgetType.DYNAMIC_FORM.getType();
   }



   /*******************************************************************************
    ** Getter for fieldList
    *******************************************************************************/
   public List<QFieldMetaData> getFieldList()
   {
      return (this.fieldList);
   }



   /*******************************************************************************
    ** Setter for fieldList
    *******************************************************************************/
   public void setFieldList(List<QFieldMetaData> fieldList)
   {
      this.fieldList = fieldList;
   }



   /*******************************************************************************
    ** Fluent setter for fieldList
    *******************************************************************************/
   public DynamicFormWidgetData withFieldList(List<QFieldMetaData> fieldList)
   {
      this.fieldList = fieldList;
      return (this);
   }



   /*******************************************************************************
    ** Getter for noFieldsMessage
    *******************************************************************************/
   public String getNoFieldsMessage()
   {
      return (this.noFieldsMessage);
   }



   /*******************************************************************************
    ** Setter for noFieldsMessage
    *******************************************************************************/
   public void setNoFieldsMessage(String noFieldsMessage)
   {
      this.noFieldsMessage = noFieldsMessage;
   }



   /*******************************************************************************
    ** Fluent setter for noFieldsMessage
    *******************************************************************************/
   public DynamicFormWidgetData withNoFieldsMessage(String noFieldsMessage)
   {
      this.noFieldsMessage = noFieldsMessage;
      return (this);
   }



   /*******************************************************************************
    ** Getter for mergedDynamicFormValuesIntoFieldName
    *******************************************************************************/
   public String getMergedDynamicFormValuesIntoFieldName()
   {
      return (this.mergedDynamicFormValuesIntoFieldName);
   }



   /*******************************************************************************
    ** Setter for mergedDynamicFormValuesIntoFieldName
    *******************************************************************************/
   public void setMergedDynamicFormValuesIntoFieldName(String mergedDynamicFormValuesIntoFieldName)
   {
      this.mergedDynamicFormValuesIntoFieldName = mergedDynamicFormValuesIntoFieldName;
   }



   /*******************************************************************************
    ** Fluent setter for mergedDynamicFormValuesIntoFieldName
    *******************************************************************************/
   public DynamicFormWidgetData withMergedDynamicFormValuesIntoFieldName(String mergedDynamicFormValuesIntoFieldName)
   {
      this.mergedDynamicFormValuesIntoFieldName = mergedDynamicFormValuesIntoFieldName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for recordOfFieldValues
    *******************************************************************************/
   public QRecord getRecordOfFieldValues()
   {
      return (this.recordOfFieldValues);
   }



   /*******************************************************************************
    ** Setter for recordOfFieldValues
    *******************************************************************************/
   public void setRecordOfFieldValues(QRecord recordOfFieldValues)
   {
      this.recordOfFieldValues = recordOfFieldValues;
   }



   /*******************************************************************************
    ** Fluent setter for recordOfFieldValues
    *******************************************************************************/
   public DynamicFormWidgetData withRecordOfFieldValues(QRecord recordOfFieldValues)
   {
      this.recordOfFieldValues = recordOfFieldValues;
      return (this);
   }

}
