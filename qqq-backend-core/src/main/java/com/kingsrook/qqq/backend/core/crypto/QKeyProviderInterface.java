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

package com.kingsrook.qqq.backend.core.crypto;


import com.kingsrook.qqq.backend.core.exceptions.QException;


/*******************************************************************************
 ** Source of symmetric key-encryption keys, addressed by a key reference.
 **
 ** A keyRef names a logical key (e.g., an environment variable name, or a
 ** secret name).  Each key can have several versions, so keys can be rotated:
 ** getKey(keyRef) returns the CURRENT version (use it for writes), while
 ** getKey(keyRef, version) returns a specific, possibly older, version (use it
 ** to read data that was written under that version).
 **
 ** Implementations must never log key bytes, and must not put them in messages
 ** of exceptions they throw.
 *******************************************************************************/
public interface QKeyProviderInterface
{

   /*******************************************************************************
    ** Get the current version of the key.
    **
    ** @throws QException if the key cannot be found or is invalid
    *******************************************************************************/
   QKeyMaterial getKey(String keyRef) throws QException;


   /*******************************************************************************
    ** Get a specific version of the key.
    **
    ** @throws QException if that version of the key cannot be found or is invalid
    *******************************************************************************/
   QKeyMaterial getKey(String keyRef, String version) throws QException;

}
