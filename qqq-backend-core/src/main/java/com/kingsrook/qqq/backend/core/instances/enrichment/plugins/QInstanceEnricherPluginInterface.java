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

package com.kingsrook.qqq.backend.core.instances.enrichment.plugins;


import com.kingsrook.qqq.backend.core.model.metadata.QInstance;


/*******************************************************************************
 ** Interface for additional / optional enrichment to be done on q instance members.
 ** Some may be provided by QQQ - others can be defined by applications.
 *******************************************************************************/
public interface QInstanceEnricherPluginInterface<T>
{

   /***************************************************************************
    * allow a plugin to be disabled at runtime.
    *
    * <p>Default implementation here in the interface is true.</p>
    *
    * @return true if enabled; false if not.
    ***************************************************************************/
   default boolean isEnabled()
   {
      return (true);
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   void enrich(T object, QInstance qInstance);


   /***************************************************************************
    **
    ***************************************************************************/
   default String getPluginIdentifier()
   {
      return getClass().getName();
   }

}
