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

package com.kingsrook.qqq.backend.core.modules.backend.implementations.enumeration;


import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.interfaces.CountInterface;
import com.kingsrook.qqq.backend.core.actions.interfaces.QueryInterface;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableBackendDetails;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleInterface;


/*******************************************************************************
 ** Backend module for a table based on a java enum.  So we can expose an enum
 ** as a table (similar to exposing an enum as a possible value source), with multiple
 ** fields in the enum (exposed via getter methods in the enum) as fields in the table.
 **
 ** Only supports read-operations, as you can't modify an enum.
 *******************************************************************************/
public class EnumerationBackendModule implements QBackendModuleInterface
{
   static
   {
      QBackendModuleDispatcher.registerBackendModule(new EnumerationBackendModule());
   }

   /*******************************************************************************
    ** Method where a backend module must be able to provide its type (name).
    *******************************************************************************/
   @Override
   public String getBackendType()
   {
      return ("enum");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Class<? extends QTableBackendDetails> getTableBackendDetailsClass()
   {
      return (EnumerationTableBackendDetails.class);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QueryInterface getQueryInterface()
   {
      return new EnumerationQueryAction();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public CountInterface getCountInterface()
   {
      return new EnumerationCountAction();
   }



   /*******************************************************************************
    ** An enum cannot be modified, so tables on this backend are read-only.
    *******************************************************************************/
   @Override
   public Set<Capability> getUnsupportedCapabilities()
   {
      return (Set.of(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE));
   }

}
