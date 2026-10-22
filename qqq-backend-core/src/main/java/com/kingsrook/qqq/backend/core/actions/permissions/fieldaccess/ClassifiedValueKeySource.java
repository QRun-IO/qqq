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

package com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess;


import java.io.Serializable;
import java.util.Optional;


/*******************************************************************************
 ** Supplies instance keys and optional per-record, per-field shreddable keys.
 *******************************************************************************/
public interface ClassifiedValueKeySource
{
   /*******************************************************************************
    ** The active instance key, if configured.
    *******************************************************************************/
   Optional<KeyAndId> instanceKey();

   /*******************************************************************************
    ** Verification uses create=false and must never recreate a destroyed key.
    *******************************************************************************/
   Optional<KeyAndId> keyForRecordField(String table, String field, Serializable pk, boolean create);

   /*******************************************************************************
    ** Key bytes and the public identifier stored in a token.
    *******************************************************************************/
   record KeyAndId(byte[] key, String id)
   {
      /*******************************************************************************
       ** Own the key bytes; identifiers must keep tokens ASCII and under 200 characters.
       *******************************************************************************/
      public KeyAndId
      {
         if(key == null || key.length == 0 || id == null || !id.matches("[A-Za-z0-9._-]{1,140}"))
         {
            throw new IllegalArgumentException("Invalid classified-value key or identifier");
         }
         key = key.clone();
      }

      /*******************************************************************************
       ** Do not expose mutable key material.
       *******************************************************************************/
      @Override
      public byte[] key()
      {
         return key.clone();
      }
   }
}
