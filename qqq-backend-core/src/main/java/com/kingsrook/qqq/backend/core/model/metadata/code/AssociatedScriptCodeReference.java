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

package com.kingsrook.qqq.backend.core.model.metadata.code;


import java.io.Serializable;
import java.util.Objects;


/*******************************************************************************
 **
 *******************************************************************************/
public class AssociatedScriptCodeReference extends QCodeReference
{
   private String       recordTable;
   private Serializable recordPrimaryKey;
   private String       fieldName;



   /*******************************************************************************
    ** Getter for recordTable
    **
    *******************************************************************************/
   public String getRecordTable()
   {
      return recordTable;
   }



   /*******************************************************************************
    ** Setter for recordTable
    **
    *******************************************************************************/
   public void setRecordTable(String recordTable)
   {
      this.recordTable = recordTable;
   }



   /*******************************************************************************
    ** Fluent setter for recordTable
    **
    *******************************************************************************/
   public AssociatedScriptCodeReference withRecordTable(String recordTable)
   {
      this.recordTable = recordTable;
      return (this);
   }



   /*******************************************************************************
    ** Getter for recordPrimaryKey
    **
    *******************************************************************************/
   public Serializable getRecordPrimaryKey()
   {
      return recordPrimaryKey;
   }



   /*******************************************************************************
    ** Setter for recordPrimaryKey
    **
    *******************************************************************************/
   public void setRecordPrimaryKey(Serializable recordPrimaryKey)
   {
      this.recordPrimaryKey = recordPrimaryKey;
   }



   /*******************************************************************************
    ** Fluent setter for recordPrimaryKey
    **
    *******************************************************************************/
   public AssociatedScriptCodeReference withRecordPrimaryKey(Serializable recordPrimaryKey)
   {
      this.recordPrimaryKey = recordPrimaryKey;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fieldName
    **
    *******************************************************************************/
   public String getFieldName()
   {
      return fieldName;
   }



   /*******************************************************************************
    ** Setter for fieldName
    **
    *******************************************************************************/
   public void setFieldName(String fieldName)
   {
      this.fieldName = fieldName;
   }



   /*******************************************************************************
    ** Fluent setter for fieldName
    **
    *******************************************************************************/
   public AssociatedScriptCodeReference withFieldName(String fieldName)
   {
      this.fieldName = fieldName;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean equals(Object o)
   {
      if(this == o)
      {
         return true;
      }
      if(o == null || getClass() != o.getClass())
      {
         return false;
      }
      AssociatedScriptCodeReference that = (AssociatedScriptCodeReference) o;
      return Objects.equals(recordTable, that.recordTable) && Objects.equals(recordPrimaryKey, that.recordPrimaryKey) && Objects.equals(fieldName, that.fieldName);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public int hashCode()
   {
      return Objects.hash(recordTable, recordPrimaryKey, fieldName);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String toString()
   {
      return "AssociatedScriptCodeReference{recordTable='" + recordTable + '\'' + ", recordPrimaryKey=" + recordPrimaryKey + ", fieldName='" + fieldName + '\'' + '}';
   }

}
