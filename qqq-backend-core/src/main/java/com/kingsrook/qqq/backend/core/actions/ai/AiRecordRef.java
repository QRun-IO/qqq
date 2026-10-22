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

package com.kingsrook.qqq.backend.core.actions.ai;


import java.io.Serializable;


/*******************************************************************************
 ** A reference to one record, for the UI to render as a chip that deep-links to
 ** the record (href is an in-app path, like /app/{table}/{id}).
 *******************************************************************************/
public class AiRecordRef
{
   private String       tableName;
   private String       tableLabel;
   private Serializable id;
   private String       label;
   private String       href;


   /*******************************************************************************
    ** Getter for tableName
    *******************************************************************************/
   public String getTableName()
   {
      return (this.tableName);
   }



   /*******************************************************************************
    ** Setter for tableName
    *******************************************************************************/
   public void setTableName(String tableName)
   {
      this.tableName = tableName;
   }



   /*******************************************************************************
    ** Fluent setter for tableName
    *******************************************************************************/
   public AiRecordRef withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableLabel
    *******************************************************************************/
   public String getTableLabel()
   {
      return (this.tableLabel);
   }



   /*******************************************************************************
    ** Setter for tableLabel
    *******************************************************************************/
   public void setTableLabel(String tableLabel)
   {
      this.tableLabel = tableLabel;
   }



   /*******************************************************************************
    ** Fluent setter for tableLabel
    *******************************************************************************/
   public AiRecordRef withTableLabel(String tableLabel)
   {
      this.tableLabel = tableLabel;
      return (this);
   }



   /*******************************************************************************
    ** Getter for id
    *******************************************************************************/
   public Serializable getId()
   {
      return (this.id);
   }



   /*******************************************************************************
    ** Setter for id
    *******************************************************************************/
   public void setId(Serializable id)
   {
      this.id = id;
   }



   /*******************************************************************************
    ** Fluent setter for id
    *******************************************************************************/
   public AiRecordRef withId(Serializable id)
   {
      this.id = id;
      return (this);
   }



   /*******************************************************************************
    ** Getter for label
    *******************************************************************************/
   public String getLabel()
   {
      return (this.label);
   }



   /*******************************************************************************
    ** Setter for label
    *******************************************************************************/
   public void setLabel(String label)
   {
      this.label = label;
   }



   /*******************************************************************************
    ** Fluent setter for label
    *******************************************************************************/
   public AiRecordRef withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Getter for href
    *******************************************************************************/
   public String getHref()
   {
      return (this.href);
   }



   /*******************************************************************************
    ** Setter for href
    *******************************************************************************/
   public void setHref(String href)
   {
      this.href = href;
   }



   /*******************************************************************************
    ** Fluent setter for href
    *******************************************************************************/
   public AiRecordRef withHref(String href)
   {
      this.href = href;
      return (this);
   }
}
