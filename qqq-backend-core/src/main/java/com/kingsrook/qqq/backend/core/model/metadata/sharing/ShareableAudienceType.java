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

package com.kingsrook.qqq.backend.core.model.metadata.sharing;


import java.io.Serializable;


/*******************************************************************************
 ** As a component of a ShareableTableMetaData instance, define details about
 ** one particular audience type.
 **
 ** e.g., if a table can be shared to users and groups, there'd be 2 instances of
 ** this object - one like:
 ** - name: user
 ** - fieldName: userId
 ** - sourceTableName: User.TABLE_NAME
 ** - sourceTableKeyFieldName: email (e.g., can be a UK, not just the PKey)
 **
 ** and another similar, w/ the group-type details.
 *******************************************************************************/
public class ShareableAudienceType implements Serializable
{
   private String name;
   private String fieldName;
   private String sourceTableName;

   /////////////////////////////////////////////////////////////////////////////////////////////////////
   // maybe normally the primary key in the source table, but could be a unique-key instead sometimes //
   /////////////////////////////////////////////////////////////////////////////////////////////////////
   private String sourceTableKeyFieldName;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public ShareableAudienceType()
   {
   }



   /*******************************************************************************
    ** Getter for name
    *******************************************************************************/
   public String getName()
   {
      return (this.name);
   }



   /*******************************************************************************
    ** Setter for name
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Fluent setter for name
    *******************************************************************************/
   public ShareableAudienceType withName(String name)
   {
      this.name = name;
      return (this);
   }



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
   public ShareableAudienceType withFieldName(String fieldName)
   {
      this.fieldName = fieldName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sourceTableName
    *******************************************************************************/
   public String getSourceTableName()
   {
      return (this.sourceTableName);
   }



   /*******************************************************************************
    ** Setter for sourceTableName
    *******************************************************************************/
   public void setSourceTableName(String sourceTableName)
   {
      this.sourceTableName = sourceTableName;
   }



   /*******************************************************************************
    ** Fluent setter for sourceTableName
    *******************************************************************************/
   public ShareableAudienceType withSourceTableName(String sourceTableName)
   {
      this.sourceTableName = sourceTableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sourceTableKeyFieldName
    *******************************************************************************/
   public String getSourceTableKeyFieldName()
   {
      return (this.sourceTableKeyFieldName);
   }



   /*******************************************************************************
    ** Setter for sourceTableKeyFieldName
    *******************************************************************************/
   public void setSourceTableKeyFieldName(String sourceTableKeyFieldName)
   {
      this.sourceTableKeyFieldName = sourceTableKeyFieldName;
   }



   /*******************************************************************************
    ** Fluent setter for sourceTableKeyFieldName
    *******************************************************************************/
   public ShareableAudienceType withSourceTableKeyFieldName(String sourceTableKeyFieldName)
   {
      this.sourceTableKeyFieldName = sourceTableKeyFieldName;
      return (this);
   }

}
