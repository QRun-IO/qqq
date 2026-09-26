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

package com.kingsrook.qqq.backend.module.postgres.model.metadata;


import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableBackendDetails;
import com.kingsrook.qqq.backend.module.postgres.PostgreSQLBackendModule;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;


/*******************************************************************************
 ** Extension of RDBMSTableBackendDetails, with details specific to a
 ** PostgreSQL table.
 ** 
 ** This class provides PostgreSQL-specific table backend configuration details.
 ** It inherits common RDBMS table properties and adds PostgreSQL-specific
 ** behavior as needed.
 ******************************************************************************/
public class PostgreSQLTableBackendDetails extends RDBMSTableBackendDetails
{

   /***************************************************************************
    ** Default constructor. Initializes the table backend details with
    ** PostgreSQL backend type.
    ***************************************************************************/
   public PostgreSQLTableBackendDetails()
   {
      super();
      setBackendType(PostgreSQLBackendModule.class);
   }



   /***************************************************************************
    ** Finish the cloning operation started in the base class. Copy all state
    ** from the subclass into the input clone (which can be safely casted to
    ** the subclass's type, as it was obtained by super.clone()).
    **
    ** @param abstractClone the clone to finish
    ** @return the finished clone
    ***************************************************************************/
   @Override
   protected QTableBackendDetails finishClone(final QTableBackendDetails abstractClone)
   {
      PostgreSQLTableBackendDetails clone = (PostgreSQLTableBackendDetails) super.finishClone(abstractClone);
      return (clone);
   }

}
