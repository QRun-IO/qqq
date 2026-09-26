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

package com.kingsrook.qqq.backend.module.mongodb.model.metadata;


import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableBackendDetails;
import com.kingsrook.qqq.backend.module.mongodb.MongoDBBackendModule;


/*******************************************************************************
 ** Extension of QTableBackendDetails, with details specific to a MongoDB table.
 *******************************************************************************/
public class MongoDBTableBackendDetails extends QTableBackendDetails
{
   private String tableName;



   /*******************************************************************************
    ** Default Constructor.
    *******************************************************************************/
   public MongoDBTableBackendDetails()
   {
      super();
      setBackendType(MongoDBBackendModule.class);
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
    ** Fluent Setter for tableName
    **
    *******************************************************************************/
   public MongoDBTableBackendDetails withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /***************************************************************************
    * finish the cloning operation started in the base class. copy all state
    * from the subclass into the input clone (which can be safely casted to
    * the subclass's type, as it was obtained by super.clone())
    ***************************************************************************/
   @Override
   protected QTableBackendDetails finishClone(QTableBackendDetails abstractClone)
   {
      MongoDBTableBackendDetails clone = (MongoDBTableBackendDetails) abstractClone;
      clone.tableName = tableName;
      return (clone);
   }

}
