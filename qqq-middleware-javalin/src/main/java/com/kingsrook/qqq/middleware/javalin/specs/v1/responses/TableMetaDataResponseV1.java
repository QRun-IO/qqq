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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses;


import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendTableMetaData;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableMetaDataOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.SchemaBuilder;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.TableMetaData;
import com.kingsrook.qqq.openapi.model.Schema;


/*******************************************************************************
 **
 *******************************************************************************/
public class TableMetaDataResponseV1 implements TableMetaDataOutputInterface, ToSchema
{
   private TableMetaData tableMetaData;



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void setTableMetaData(QFrontendTableMetaData frontendTableMetaData)
   {
      this.tableMetaData = new TableMetaData(frontendTableMetaData);
   }



   /*******************************************************************************
    ** Fluent setter for frontendTableMetaData
    **
    *******************************************************************************/
   public TableMetaDataResponseV1 withTableMetaData(QFrontendTableMetaData frontendTableMetaData)
   {
      setTableMetaData(frontendTableMetaData);
      return (this);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Schema toSchema()
   {
      return new SchemaBuilder().classToSchema(TableMetaData.class);
   }



   /*******************************************************************************
    ** Getter for tableMetaData
    **
    *******************************************************************************/
   public TableMetaData getTableMetaData()
   {
      return tableMetaData;
   }
}
