/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.authentication;


import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Configured browser URLs and native callback ownership are never inferred.
 *******************************************************************************/
class OAuth2NativeSessionLoginMetaDataTest
{
   /*******************************************************************************
    ** Public metadata accepts only complete, credential-free HTTPS endpoints.
    *******************************************************************************/
   @Test
   void publicEndpointsMustBeExplicitHttps()
   {
      assertFalse(new OAuth2NativeSessionLoginMetaData().isValid());
      assertTrue(valid().isValid());
      for(String endpoint : List.of("", "http://login.example.test/", "https://user:secret@login.example.test/",
         "https://login.example.test/?secret=value", "https://login.example.test/#fragment", "https:///missing-host",
         "https://login.example.test:0/", "https://login.example.test:99999/", "relative"))
      {
         assertFalse(valid().withIssuer(endpoint).isValid());
         assertFalse(valid().withDiscoveryUrl(endpoint).isValid());
      }
   }



   /*******************************************************************************
    ** Exact callbacks support HTTPS app links and reverse-domain private schemes.
    *******************************************************************************/
   @Test
   void redirectAllowlistRejectsAmbiguousOrUnsafeEntries()
   {
      assertTrue(valid().withRedirectUris(List.of("https://app.example.test/callback")).isValid());
      for(String callback : List.of("", "http://app.example.test/callback", "javascript:callback", "file:///callback",
         "com.example.app:/callback?value=1", "com.example.app:/callback#fragment", "com.example.app://user:secret@host/callback",
         "com.example.app://user:secret@/callback", "https://app.example.test:0/callback",
         "com.example.app:/*", "com.example.app:"))
      {
         assertFalse(valid().withRedirectUris(List.of(callback)).isValid());
      }
      assertFalse(valid().withRedirectUris(List.of()).isValid());
      assertFalse(valid().withRedirectUris(null).isValid());
   }



   /*******************************************************************************
    ** A caller cannot later mutate the allowlist through its source or result list.
    *******************************************************************************/
   @Test
   void redirectAllowlistIsDefensivelyCopied()
   {
      List<String> callbacks = new ArrayList<>(List.of("com.example.app:/callback"));
      OAuth2NativeSessionLoginMetaData metadata = valid().withRedirectUris(callbacks);
      callbacks.clear();
      assertTrue(metadata.isValid());
      assertThrows(UnsupportedOperationException.class, () -> metadata.getRedirectUris().clear());
   }



   /*******************************************************************************
    ** Explicit sample policy, with no runtime provider or application assumptions.
    *******************************************************************************/
   private OAuth2NativeSessionLoginMetaData valid()
   {
      return (new OAuth2NativeSessionLoginMetaData().withIssuer("https://login.example.test/issuer/")
         .withDiscoveryUrl("https://login.example.test/issuer/.well-known/openid-configuration")
         .withRedirectUris(List.of("com.example.app:/callback")));
   }
}
