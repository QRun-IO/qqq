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

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model;


import java.io.Serializable;
import java.util.Map;


/***************************************************************************
 **
 ***************************************************************************/
public class BulkLoadProfileField
{
   private String                    fieldName;
   private Integer                   columnIndex;
   private String                    headerName;
   private Serializable              defaultValue;
   private Boolean                   clearIfEmpty;
   private Boolean                   doValueMapping;
   private Map<String, Serializable> valueMappings;



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
   public BulkLoadProfileField withFieldName(String fieldName)
   {
      this.fieldName = fieldName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for columnIndex
    *******************************************************************************/
   public Integer getColumnIndex()
   {
      return (this.columnIndex);
   }



   /*******************************************************************************
    ** Setter for columnIndex
    *******************************************************************************/
   public void setColumnIndex(Integer columnIndex)
   {
      this.columnIndex = columnIndex;
   }



   /*******************************************************************************
    ** Fluent setter for columnIndex
    *******************************************************************************/
   public BulkLoadProfileField withColumnIndex(Integer columnIndex)
   {
      this.columnIndex = columnIndex;
      return (this);
   }



   /*******************************************************************************
    ** Getter for defaultValue
    *******************************************************************************/
   public Serializable getDefaultValue()
   {
      return (this.defaultValue);
   }



   /*******************************************************************************
    ** Setter for defaultValue
    *******************************************************************************/
   public void setDefaultValue(Serializable defaultValue)
   {
      this.defaultValue = defaultValue;
   }



   /*******************************************************************************
    ** Fluent setter for defaultValue
    *******************************************************************************/
   public BulkLoadProfileField withDefaultValue(Serializable defaultValue)
   {
      this.defaultValue = defaultValue;
      return (this);
   }



   /*******************************************************************************
    ** Getter for doValueMapping
    *******************************************************************************/
   public Boolean getDoValueMapping()
   {
      return (this.doValueMapping);
   }



   /*******************************************************************************
    ** Setter for doValueMapping
    *******************************************************************************/
   public void setDoValueMapping(Boolean doValueMapping)
   {
      this.doValueMapping = doValueMapping;
   }



   /*******************************************************************************
    ** Fluent setter for doValueMapping
    *******************************************************************************/
   public BulkLoadProfileField withDoValueMapping(Boolean doValueMapping)
   {
      this.doValueMapping = doValueMapping;
      return (this);
   }



   /*******************************************************************************
    ** Getter for valueMappings
    *******************************************************************************/
   public Map<String, Serializable> getValueMappings()
   {
      return (this.valueMappings);
   }



   /*******************************************************************************
    ** Setter for valueMappings
    *******************************************************************************/
   public void setValueMappings(Map<String, Serializable> valueMappings)
   {
      this.valueMappings = valueMappings;
   }



   /*******************************************************************************
    ** Fluent setter for valueMappings
    *******************************************************************************/
   public BulkLoadProfileField withValueMappings(Map<String, Serializable> valueMappings)
   {
      this.valueMappings = valueMappings;
      return (this);
   }



   /*******************************************************************************
    ** Getter for headerName
    *******************************************************************************/
   public String getHeaderName()
   {
      return (this.headerName);
   }



   /*******************************************************************************
    ** Setter for headerName
    *******************************************************************************/
   public void setHeaderName(String headerName)
   {
      this.headerName = headerName;
   }



   /*******************************************************************************
    ** Fluent setter for headerName
    *******************************************************************************/
   public BulkLoadProfileField withHeaderName(String headerName)
   {
      this.headerName = headerName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for clearIfEmpty
    *******************************************************************************/
   public Boolean getClearIfEmpty()
   {
      return (this.clearIfEmpty);
   }



   /*******************************************************************************
    ** Setter for clearIfEmpty
    *******************************************************************************/
   public void setClearIfEmpty(Boolean clearIfEmpty)
   {
      this.clearIfEmpty = clearIfEmpty;
   }



   /*******************************************************************************
    ** Fluent setter for clearIfEmpty
    *******************************************************************************/
   public BulkLoadProfileField withClearIfEmpty(Boolean clearIfEmpty)
   {
      this.clearIfEmpty = clearIfEmpty;
      return (this);
   }

}
