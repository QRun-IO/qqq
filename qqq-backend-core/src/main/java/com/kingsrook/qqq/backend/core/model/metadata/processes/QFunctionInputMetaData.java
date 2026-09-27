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
import java.util.Optional;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;


/*******************************************************************************
 ** Meta-Data to define the Input Data for a QQQ Function
 **
 *******************************************************************************/
public class QFunctionInputMetaData
{
   private QRecordListMetaData  recordListMetaData;
   private List<QFieldMetaData> fieldList = new ArrayList<>();



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
   public QFunctionInputMetaData withRecordListMetaData(QRecordListMetaData recordListMetaData)
   {
      this.recordListMetaData = recordListMetaData;
      return (this);
   }



   /*******************************************************************************
    ** Getter a field with the given name
    **
    *******************************************************************************/
   public Optional<QFieldMetaData> getField(String name)
   {
      return (fieldList.stream().filter(field -> name.equals(field.getName())).findFirst());
   }



   /*******************************************************************************
    ** Getter a field with the given name - throwing if it wasn't found
    **
    *******************************************************************************/
   public QFieldMetaData getFieldThrowing(String name) throws QException
   {
      Optional<QFieldMetaData> field = fieldList.stream().filter(f -> name.equals(f.getName())).findFirst();
      if(field.isEmpty())
      {
         throw (new QException("Could not find field [" + name + "] in function input meta data"));
      }
      return (field.get());
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
    ** Fluently ADD a list of fields to this object's existing list
    **
    *******************************************************************************/
   public QFunctionInputMetaData withFields(List<QFieldMetaData> fieldList)
   {
      if(this.fieldList == null)
      {
         this.fieldList = new ArrayList<>();
      }
      this.fieldList.addAll(fieldList);
      return (this);
   }



   /*******************************************************************************
    ** Fluent Setter for fieldList - e.g., will overwrite any previously set fields!!
    **
    *******************************************************************************/
   public QFunctionInputMetaData withFieldList(List<QFieldMetaData> fieldList)
   {
      this.fieldList = fieldList;
      return (this);
   }



   /*******************************************************************************
    ** Fluently add a field to the list
    **
    *******************************************************************************/
   public QFunctionInputMetaData withField(QFieldMetaData field)
   {
      if(this.fieldList == null)
      {
         this.fieldList = new ArrayList<>();
      }
      this.fieldList.add(field);
      return (this);
   }


}
