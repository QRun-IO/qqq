/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.postgres;


import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableBackendDetails;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.module.postgres.model.metadata.PostgreSQLBackendMetaData;
import com.kingsrook.qqq.backend.module.postgres.model.metadata.PostgreSQLTableBackendDetails;
import com.kingsrook.qqq.backend.module.rdbms.RDBMSBackendModule;


/*******************************************************************************
 ** QQQ Backend module for working with PostgreSQL databases.
 ** 
 ** This module extends the generic RDBMS backend module to provide PostgreSQL-specific
 ** functionality, including connection management, query execution, and data type handling.
 ******************************************************************************/
public class PostgreSQLBackendModule extends RDBMSBackendModule
{
   private static final QLogger LOG = QLogger.getLogger(
      PostgreSQLBackendModule.class);

   private static final String NAME = "postgres";

   static
   {
      QBackendModuleDispatcher.registerBackendModule(
         new PostgreSQLBackendModule());
   }

   /***************************************************************************
    ** Method where a backend module must be able to provide its type (name).
    **
    ** @return the backend type name
    ***************************************************************************/
   public String getBackendType()
   {
      return NAME;
   }



   /***************************************************************************
    ** Method to identify the class used for backend meta data for this
    ** module.
    **
    ** @return the backend metadata class
    ***************************************************************************/
   @Override
   public Class<? extends QBackendMetaData> getBackendMetaDataClass()
   {
      return (PostgreSQLBackendMetaData.class);
   }



   /***************************************************************************
    ** Method to identify the class used for table-backend details for this
    ** module.
    **
    ** @return the table backend details class
    ***************************************************************************/
   @Override
   public Class<? extends QTableBackendDetails> getTableBackendDetailsClass()
   {
      return (PostgreSQLTableBackendDetails.class);
   }

}
