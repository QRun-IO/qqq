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

package com.kingsrook.qqq.backend.core.model.secrets;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** One-time secrets must not expose sensitive contents through diagnostics.
 *******************************************************************************/
class OneTimeSecretTest
{
   /*******************************************************************************
    ** Only label and masked representation appear, even when usage embeds the secret.
    *******************************************************************************/
   @Test
   void testToStringRedactsValueAndUsageText()
   {
      OneTimeSecret secret = new OneTimeSecret().withLabel("Access token").withValue("fixture-secret-value")
         .withMasked("qqq_…7f3a").withUsageText("curl -H 'Authorization: Bearer fixture-secret-value'\nhttps://example.test");
      assertEquals("OneTimeSecret{label='Access token', masked='qqq_…7f3a'}", secret.toString());
      assertEquals("OneTimeSecret{label='null', masked='null'}", new OneTimeSecret().toString());
   }
}
