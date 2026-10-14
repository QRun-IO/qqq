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

package com.kingsrook.qqq.backend.core.model.metadata.fields;




/*******************************************************************************
 ** Runtime custom field metadata and storage.
 *******************************************************************************/
public class JsonPathStorage implements FieldBackendStorage
{
   private String columnName;
   private String key;



   /*******************************************************************************
    ** Getter for columnName
    *******************************************************************************/
   public String getColumnName()
   {
      return (columnName);
   }



   /*******************************************************************************
    ** Setter for columnName
    *******************************************************************************/
   public void setColumnName(String columnName)
   {
      this.columnName = columnName;
   }



   /*******************************************************************************
    ** Fluent setter for columnName
    *******************************************************************************/
   public JsonPathStorage withColumnName(String columnName)
   {
      setColumnName(columnName);
      return (this);
   }



   /*******************************************************************************
    ** Getter for key
    *******************************************************************************/
   public String getKey()
   {
      return (key);
   }



   /*******************************************************************************
    ** Setter for key
    *******************************************************************************/
   public void setKey(String key)
   {
      this.key = key;
   }



   /*******************************************************************************
    ** Fluent setter for key
    *******************************************************************************/
   public JsonPathStorage withKey(String key)
   {
      setKey(key);
      return (this);
   }



   /*******************************************************************************
    ** Copy this storage declaration.
    *******************************************************************************/
   @Override
   public JsonPathStorage clone()
   {
      return (new JsonPathStorage().withColumnName(columnName).withKey(key));
   }
}
