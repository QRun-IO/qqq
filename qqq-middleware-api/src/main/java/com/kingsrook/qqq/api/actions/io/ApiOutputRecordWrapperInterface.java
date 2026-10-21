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



