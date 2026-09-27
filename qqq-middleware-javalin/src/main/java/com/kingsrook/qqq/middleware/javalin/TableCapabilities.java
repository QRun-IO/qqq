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
