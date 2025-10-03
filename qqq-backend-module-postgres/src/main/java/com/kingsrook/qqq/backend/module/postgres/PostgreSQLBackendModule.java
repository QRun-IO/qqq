/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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
