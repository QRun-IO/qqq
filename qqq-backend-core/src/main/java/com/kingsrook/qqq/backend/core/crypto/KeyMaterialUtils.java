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


import java.util.Base64;
import java.util.regex.Pattern;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Shared key handling: validating key ids and decoding base64 key text.  Used
 ** by the key providers and by RefreshTokenCipher.  Messages name the setting,
 ** never the key.
 *******************************************************************************/
public class KeyMaterialUtils
{
   public static final int AES_256_KEY_LENGTH_BYTES = 32;

   private static final Pattern KEY_ID_PATTERN = Pattern.compile("[A-Za-z0-9_.-]{1,64}");



   /*******************************************************************************
    ** Private constructor - utility class.
    *******************************************************************************/
   private KeyMaterialUtils()
   {
   }



   /*******************************************************************************
    ** Make sure a key id (or version) can be embedded in a stored format.
    *******************************************************************************/
   public static void validateKeyId(String keyId, String settingName) throws QException
   {
      if(keyId == null || !KEY_ID_PATTERN.matcher(keyId).matches())
      {
         throw (new QException(settingName + " must be 1-64 characters of letters, digits, '_', '.' or '-'"));
      }
   }



   /*******************************************************************************
    ** Decode base64 key text, requiring the given length in bytes.
    *******************************************************************************/
   public static byte[] decodeBase64Key(String base64Key, String settingName, int expectedLengthBytes) throws QException
   {
      if(!StringUtils.hasContent(base64Key))
      {
         throw (new QException(settingName + " is not set (or its variable did not resolve)"));
      }

      byte[] keyBytes;
      try
      {
         keyBytes = Base64.getDecoder().decode(base64Key.trim());
      }
      catch(IllegalArgumentException e)
      {
         throw (new QException(settingName + " is not valid base64"));
      }

      if(keyBytes.length != expectedLengthBytes)
      {
         throw (new QException(settingName + " must decode to " + expectedLengthBytes + " bytes (e.g., from: openssl rand -base64 32)"));
      }

      return (keyBytes);
   }

}
