/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.instances.validation.plugins;


import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;


/*******************************************************************************
 ** Interface for additional / optional q instance validators.  Some will be
 ** provided by QQQ - others can be defined by applications.
 *******************************************************************************/
public interface QInstanceValidatorPluginInterface<T>
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
   void validate(T object, QInstance qInstance, QInstanceValidator qInstanceValidator);


   /***************************************************************************
    **
    ***************************************************************************/
   default String getPluginIdentifier()
   {
      return getClass().getName();
   }

}
