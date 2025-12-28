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

package com.kingsrook.qqq.api.actions.io;


import java.io.Serializable;
import java.util.List;


/***************************************************************************
 ** interface to define wrappers for either a Map of values (e.g., the
 ** original/native return type for the API), or a QRecord.  Built for use
 ** by QRecordApiAdapter - not clear ever useful outside of there.
 **
 ** Type params are:
 ** C: the wrapped Contents
 ** A: the child-type... e.g:
 **    class Child implements ApiOutputRecordInterface(Something, Child)
 ***************************************************************************/
public interface ApiOutputRecordWrapperInterface<C, A extends ApiOutputRecordWrapperInterface<C, A>>
{
   /***************************************************************************
    ** put a value in the wrapped object
    ***************************************************************************/
   void putValue(String key, Serializable value);

   /***************************************************************************
    ** put associated objects in the wrapped object
    ***************************************************************************/
   void putAssociation(String key, List<C> values);

   /***************************************************************************
    ** create a new "sibling" object to this - e.g., a wrapper around a new
    ** instance of the contents object
    ***************************************************************************/
   ApiOutputRecordWrapperInterface<C, A> newSibling(String tableName);

   /***************************************************************************
    ** get the wrapped contents object
    ***************************************************************************/
   C getContents();

   /***************************************************************************
    ** return this, but as the `A` type...
    ***************************************************************************/
   @SuppressWarnings("unchecked")
   default A unwrap()
   {
      return (A) this;
   }
}



