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

package com.kingsrook.qqq.backend.core.jobs;


import java.util.Base64;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.utils.BoundedIntentData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Source-derived intent metadata cannot be a public low-entropy guessing oracle.
 *******************************************************************************/
class JobIntentFingerprintTest extends DurableJobsTestBase
{
   /*******************************************************************************
    ** A keyed opaque value differs from the public digest and changes on rotation.
    *******************************************************************************/
   @Test
   void lowEntropyIntentUsesSecretDomainSeparatedFingerprint() throws Exception
   {
      String publicHash = BoundedIntentData.fingerprint(Map.of("hiddenTier", "restricted"));
      settings.withJobGrantKey(Base64.getEncoder().encodeToString(new byte[32]));
      String protectedHash = JobGrants.intentFingerprint(instance, publicHash);
      assertEquals(64, protectedHash.length());
      assertNotEquals(publicHash, protectedHash);
      assertEquals(protectedHash, JobGrants.intentFingerprint(instance, publicHash));
      byte[] rotated = new byte[32];
      rotated[0] = 1;
      settings.withJobGrantKey(Base64.getEncoder().encodeToString(rotated));
      assertNotEquals(protectedHash, JobGrants.intentFingerprint(instance, publicHash));
   }



   /*******************************************************************************
    ** No missing-key fallback or arbitrary-message signing is accepted.
    *******************************************************************************/
   @Test
   void missingKeyAndNoncanonicalDigestAreRefused() throws Exception
   {
      settings.withJobGrantKey(null);
      assertThrows(QException.class, () -> JobGrants.intentFingerprint(instance, "0".repeat(64)));
      settings.withJobGrantKey(Base64.getEncoder().encodeToString(new byte[32]));
      assertThrows(QException.class, () -> JobGrants.intentFingerprint(instance, "arbitrary message"));
   }
}
