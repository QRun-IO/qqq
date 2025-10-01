/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.tables;


import java.util.HashSet;
import java.util.Set;


/*******************************************************************************
 ** Things that can be done to tables, fields.
 **
 *******************************************************************************/
public enum Capability
{
   TABLE_QUERY,
   TABLE_GET,
   TABLE_COUNT,
   TABLE_INSERT,
   TABLE_UPDATE,
   TABLE_DELETE,
   TABLE_EXPORT,
   ///////////////////////////////////////////////////////////////////////
   // keep these values in sync with Capability.ts in qqq-frontend-core //
   ///////////////////////////////////////////////////////////////////////

   QUERY_STATS;



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Set<Capability> allReadCapabilities()
   {
      return (new HashSet<>(Set.of(TABLE_QUERY, TABLE_GET, TABLE_COUNT, TABLE_EXPORT, QUERY_STATS)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Set<Capability> allWriteCapabilities()
   {
      return (new HashSet<>(Set.of(TABLE_INSERT, TABLE_UPDATE, TABLE_DELETE)));
   }

}
