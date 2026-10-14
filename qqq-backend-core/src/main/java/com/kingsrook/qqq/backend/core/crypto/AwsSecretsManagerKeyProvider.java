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


import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import com.kingsrook.qqq.backend.core.instances.SecretsManagerUtils;


/*******************************************************************************
 ** Key provider that reads base64 keys from AWS Secrets Manager, through
 ** SecretsManagerUtils (so it uses the same SECRETS_MANAGER_* environment
 ** configuration).  The secret id is (path + name), per SecretsManagerUtils;
 ** the keyRef is the name.  See AbstractLookupKeyProvider for the version
 ** convention.
 **
 ** The secret lookup is injectable, for tests.
 *******************************************************************************/
public class AwsSecretsManagerKeyProvider extends AbstractLookupKeyProvider
{
   private final String                                       path;
   private final BiFunction<String, String, Optional<String>> secretLookup;



   /*******************************************************************************
    ** Constructor - uses SecretsManagerUtils.
    *******************************************************************************/
   public AwsSecretsManagerKeyProvider(String path)
   {
      this(path, SecretsManagerUtils::getSecret);
   }



   /*******************************************************************************
    ** Constructor with an injectable (path, name) secret lookup.
    *******************************************************************************/
   public AwsSecretsManagerKeyProvider(String path, BiFunction<String, String, Optional<String>> secretLookup)
   {
      this.path = Objects.requireNonNullElse(path, "");
      this.secretLookup = secretLookup;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected Optional<String> lookup(String name)
   {
      return (secretLookup.apply(path, name));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected String describeSource()
   {
      return ("secret");
   }

}
