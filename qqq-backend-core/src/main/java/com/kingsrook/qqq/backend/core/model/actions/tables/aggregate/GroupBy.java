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

package com.kingsrook.qqq.backend.core.model.actions.tables.aggregate;


import java.io.Serializable;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;


/*******************************************************************************
 **
 *******************************************************************************/
public class GroupBy implements Serializable
{
   private QFieldType type;
   private String     fieldName;
   private String     formatString;



   /*******************************************************************************
    **
    *******************************************************************************/
   public GroupBy(QFieldMetaData field)
   {
      this.type = field.getType();
      this.fieldName = field.getName();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public GroupBy(QFieldType type, String fieldName)
   {
      this.type = type;
      this.fieldName = fieldName;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public GroupBy(QFieldType type, String fieldName, String formatString)
   {
      this.type = type;
      this.fieldName = fieldName;
      this.formatString = formatString;
   }



   /*******************************************************************************
    ** Getter for type
    **
    *******************************************************************************/
   public QFieldType getType()
   {
      return type;
   }



   /*******************************************************************************
    ** Setter for type
    **
    *******************************************************************************/
   public void setType(QFieldType type)
   {
      this.type = type;
   }



   /*******************************************************************************
    ** Fluent setter for type
    **
    *******************************************************************************/
   public GroupBy withType(QFieldType type)
   {
      this.type = type;
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
   public GroupBy withFieldName(String fieldName)
   {
      this.fieldName = fieldName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for formatString
    **
    *******************************************************************************/
   public String getFormatString()
   {
      return formatString;
   }



   /*******************************************************************************
    ** Setter for formatString
    **
    *******************************************************************************/
   public void setFormatString(String formatString)
   {
      this.formatString = formatString;
   }



   /*******************************************************************************
    ** Fluent setter for formatString
    **
    *******************************************************************************/
   public GroupBy withFormatString(String formatString)
   {
      this.formatString = formatString;
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
      GroupBy groupBy = (GroupBy) o;
      return type == groupBy.type && Objects.equals(fieldName, groupBy.fieldName) && Objects.equals(formatString, groupBy.formatString);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public int hashCode()
   {
      return Objects.hash(type, fieldName, formatString);
   }
}
