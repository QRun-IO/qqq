/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin;


import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** HTTP requests may only use the capabilities a table declares, in the same
 ** way frontends only offer them: a table without TABLE_COUNT cannot be counted
 ** and a table without TABLE_EXPORT cannot be exported, over any route.
 *******************************************************************************/
public final class TableCapabilities
{

   /*******************************************************************************
    **
    *******************************************************************************/
   private TableCapabilities()
   {
   }



   /*******************************************************************************
    ** Throw a permission-denied error when the table lacks the capability.
    ** Unknown tables are left to the action's own (permission) checks.
    *******************************************************************************/
   public static void checkCapabilityThrowing(String tableName, Capability capability) throws QPermissionDeniedException
   {
      QTableMetaData table = QContext.getQInstance().getTable(tableName);
      if(table == null)
      {
         return;
      }

      QBackendMetaData backend = QContext.getQInstance().getBackendForTable(tableName);
      if(!table.isCapabilityEnabled(backend, capability))
      {
         throw (new QPermissionDeniedException("Permission denied."));
      }
   }
}
