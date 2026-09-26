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

package com.kingsrook.qqq.middleware.javalin.executors.io;


import java.util.List;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.TableVariant;


/*******************************************************************************
 **
 *******************************************************************************/
public class TableGetInput extends AbstractMiddlewareInput
{
   private String          tableName;
   private String          primaryKey;
   private Boolean         includeAssociations;
   private List<QueryJoin> queryJoins;
   private TableVariant tableVariant;



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
    ** Fluent setter for tableName
    **
    *******************************************************************************/
   public TableGetInput withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for primaryKey
    **
    *******************************************************************************/
   public String getPrimaryKey()
   {
      return primaryKey;
   }



   /*******************************************************************************
    ** Setter for primaryKey
    **
    *******************************************************************************/
   public void setPrimaryKey(String primaryKey)
   {
      this.primaryKey = primaryKey;
   }



   /*******************************************************************************
    ** Fluent setter for primaryKey
    **
    *******************************************************************************/
   public TableGetInput withPrimaryKey(String primaryKey)
   {
      this.primaryKey = primaryKey;
      return (this);
   }



   /*******************************************************************************
    ** Getter for includeAssociations
    **
    *******************************************************************************/
   public Boolean getIncludeAssociations()
   {
      return includeAssociations;
   }



   /*******************************************************************************
    ** Setter for includeAssociations
    **
    *******************************************************************************/
   public void setIncludeAssociations(Boolean includeAssociations)
   {
      this.includeAssociations = includeAssociations;
   }



   /*******************************************************************************
    ** Fluent setter for includeAssociations
    **
    *******************************************************************************/
   public TableGetInput withIncludeAssociations(Boolean includeAssociations)
   {
      this.includeAssociations = includeAssociations;
      return (this);
   }



   /*******************************************************************************
    ** Getter for queryJoins
    **
    *******************************************************************************/
   public List<QueryJoin> getQueryJoins()
   {
      return queryJoins;
   }



   /*******************************************************************************
    ** Setter for queryJoins
    **
    *******************************************************************************/
   public void setQueryJoins(List<QueryJoin> queryJoins)
   {
      this.queryJoins = queryJoins;
   }



   /*******************************************************************************
    ** Fluent setter for queryJoins
    **
    *******************************************************************************/
   public TableGetInput withQueryJoins(List<QueryJoin> queryJoins)
   {
      this.queryJoins = queryJoins;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableVariant
    *******************************************************************************/
   public TableVariant getTableVariant()
   {
      return (tableVariant);
   }



   /*******************************************************************************
    ** Setter for tableVariant
    *******************************************************************************/
   public void setTableVariant(TableVariant tableVariant)
   {
      this.tableVariant = tableVariant;
   }



   /*******************************************************************************
    ** Fluent setter for tableVariant
    *******************************************************************************/
   public TableGetInput withTableVariant(TableVariant tableVariant)
   {
      this.tableVariant = tableVariant;
      return (this);
   }

}
