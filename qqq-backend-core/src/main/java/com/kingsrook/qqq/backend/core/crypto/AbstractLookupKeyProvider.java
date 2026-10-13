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


import java.util.Optional;
import com.kingsrook.qqq.backend.core.exceptions.QException;


/*******************************************************************************
 ** Base for key providers that look up base64 text by name.  Naming convention
 ** for a keyRef NAME:
 **
 ** - NAME            holds the CURRENT key (base64 of 32 bytes)
 ** - NAME_VERSION    optionally names the current version (default "1")
 ** - NAME_V{version} holds the key for an older version, kept while data written
 **                   under it still exists
 **
 ** So rotating is: copy NAME to NAME_V{old}, put the new key in NAME, and set
 ** NAME_VERSION to the new version.
 *******************************************************************************/
public abstract class AbstractLookupKeyProvider implements QKeyProviderInterface
{
   public static final String DEFAULT_VERSION = "1";



   /*******************************************************************************
    ** Look up the (base64) value stored under a name.  Empty if not found.
    *******************************************************************************/
   protected abstract Optional<String> lookup(String name);



   /*******************************************************************************
    ** Describe the source, for messages (e.g., "environment variable").
    *******************************************************************************/
   protected abstract String describeSource();



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QKeyMaterial getKey(String keyRef) throws QException
   {
      KeyMaterialUtils.validateKeyId(keyRef, "keyRef");
      return (getKey(keyRef, currentVersion(keyRef)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QKeyMaterial getKey(String keyRef, String version) throws QException
   {
      KeyMaterialUtils.validateKeyId(keyRef, "keyRef");
      KeyMaterialUtils.validateKeyId(version, "key version");

      String           name  = version.equals(currentVersion(keyRef)) ? keyRef : keyRef + "_V" + version;
      Optional<String> value = lookup(name);
      if(value.isEmpty())
      {
         throw (new QException("No key found for keyRef [" + keyRef + "] version [" + version + "] (" + describeSource() + " [" + name + "] is not set)"));
      }

      byte[] bytes = KeyMaterialUtils.decodeBase64Key(value.get(), "Key [" + keyRef + "] version [" + version + "]", KeyMaterialUtils.AES_256_KEY_LENGTH_BYTES);
      return (new QKeyMaterial(bytes, keyRef, version));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String currentVersion(String keyRef) throws QException
   {
      String version = lookup(keyRef + "_VERSION").map(String::trim).filter(s -> !s.isEmpty()).orElse(DEFAULT_VERSION);
      KeyMaterialUtils.validateKeyId(version, "key version");
      return (version);
   }

}
