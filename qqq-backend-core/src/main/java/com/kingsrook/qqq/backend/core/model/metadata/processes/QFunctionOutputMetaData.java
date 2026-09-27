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
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;


/*******************************************************************************
 ** Meta-Data to define the Output Data for a QQQ Function
 **
 *******************************************************************************/
public class QFunctionOutputMetaData
{
   private QRecordListMetaData  recordListMetaData;
   private List<QFieldMetaData> fieldList;



   /*******************************************************************************
    ** Getter for recordListMetaData
    **
    *******************************************************************************/
   public QRecordListMetaData getRecordListMetaData()
   {
      return recordListMetaData;
   }



   /*******************************************************************************
    ** Setter for recordListMetaData
    **
    *******************************************************************************/
   public void setRecordListMetaData(QRecordListMetaData recordListMetaData)
   {
      this.recordListMetaData = recordListMetaData;
   }



   /*******************************************************************************
    ** Setter for recordListMetaData
    **
    *******************************************************************************/
   public QFunctionOutputMetaData withRecordListMetaData(QRecordListMetaData recordListMetaData)
   {
      this.recordListMetaData = recordListMetaData;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fieldList
    **
    *******************************************************************************/
   public List<QFieldMetaData> getFieldList()
   {
      return fieldList;
   }



   /*******************************************************************************
    ** Setter for fieldList
    **
    *******************************************************************************/
   public void setFieldList(List<QFieldMetaData> fieldList)
   {
      this.fieldList = fieldList;
   }



   /*******************************************************************************
    ** Setter for fieldList
    **
    *******************************************************************************/
   public QFunctionOutputMetaData withFieldList(List<QFieldMetaData> fieldList)
   {
      this.fieldList = fieldList;
      return (this);
   }




   /*******************************************************************************
    ** Fluently add a field to the list
    **
    *******************************************************************************/
   public QFunctionOutputMetaData withField(QFieldMetaData field)
   {
      if(this.fieldList == null)
      {
         this.fieldList = new ArrayList<>();
      }
      this.fieldList.add(field);
      return (this);
   }

}

