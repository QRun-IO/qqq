/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.backend.core.model.metadata.processes;


import java.util.LinkedHashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;


/*******************************************************************************
 **
 *******************************************************************************/
public class QRecordListMetaData
{
   private String                      tableName;
   private Map<String, QFieldMetaData> fields;



   /*******************************************************************************
    **
    *******************************************************************************/
   public QFieldMetaData getField(String fieldName)
   {
      if(fields == null)
      {
         return (null);
      }

      QFieldMetaData field = getFields().get(fieldName);
      if(field == null)
      {
         throw (new IllegalArgumentException("Field [" + fieldName + "] was not found."));
      }

      return (field);
   }



   /*******************************************************************************
    ** Getter for tableName
    **
    *******************************************************************************/
   public String getTableName()
   {
      return tableName;
   }



   /*******************************************************************************
    ** Setter for tableName
    **
    *******************************************************************************/
   public void setTableName(String tableName)
   {
      this.tableName = tableName;
   }



   /*******************************************************************************
    ** Setter for tableName
    **
    *******************************************************************************/
   public QRecordListMetaData withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public Map<String, QFieldMetaData> getFields()
   {
      return fields;
   }



   /*******************************************************************************
    ** Setter for fields
    **
    *******************************************************************************/
   public void setFields(Map<String, QFieldMetaData> fields)
   {
      this.fields = fields;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QRecordListMetaData withField(QFieldMetaData field)
   {
      if(this.fields == null)
      {
         this.fields = new LinkedHashMap<>();
      }
      this.fields.put(field.getName(), field);
      return (this);
   }



}
