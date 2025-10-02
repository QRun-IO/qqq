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
