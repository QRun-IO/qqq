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


import java.util.Arrays;
import java.util.Objects;


/*******************************************************************************
 ** A resolved key: its bytes, the id (the keyRef) that names it, and its version.
 **
 ** The bytes are a defensive copy on the way in and on the way out, and
 ** toString never includes them.
 *******************************************************************************/
public final class QKeyMaterial
{
   private final byte[] keyBytes;
   private final String keyId;
   private final String version;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public QKeyMaterial(byte[] keyBytes, String keyId, String version)
   {
      this.keyBytes = Objects.requireNonNull(keyBytes, "keyBytes").clone();
      this.keyId = Objects.requireNonNull(keyId, "keyId");
      this.version = Objects.requireNonNull(version, "version");
   }



   /*******************************************************************************
    ** Getter for keyBytes (a copy)
    *******************************************************************************/
   public byte[] getKeyBytes()
   {
      return (keyBytes.clone());
   }



   /*******************************************************************************
    ** Getter for keyId
    *******************************************************************************/
   public String getKeyId()
   {
      return (keyId);
   }



   /*******************************************************************************
    ** Getter for version
    *******************************************************************************/
   public String getVersion()
   {
      return (version);
   }



   /*******************************************************************************
    ** Never reveals key bytes.
    *******************************************************************************/
   @Override
   public String toString()
   {
      return ("QKeyMaterial[keyId=" + keyId + ", version=" + version + ", keyBytes=<redacted>]");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean equals(Object o)
   {
      return (o instanceof QKeyMaterial that && Arrays.equals(keyBytes, that.keyBytes) && keyId.equals(that.keyId) && version.equals(that.version));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public int hashCode()
   {
      return (31 * Objects.hash(keyId, version) + Arrays.hashCode(keyBytes));
   }

}
