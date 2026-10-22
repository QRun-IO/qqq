/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Scanner catches partial body and header leaks, while accepting masks.
 *******************************************************************************/
class SentinelResponseScannerTest
{
   /*******************************************************************************
    ** Check both channels and a five-character fragment.
    *******************************************************************************/
   @Test
   void catchesFragments()
   {
      SentinelResponseScanner body = new SentinelResponseScanner();
      body.collect("7f3a9", "");
      assertThrows(AssertionError.class, () -> body.assertSafe("SENTINEL-PHI-7f3a91c2"));
      SentinelResponseScanner header = new SentinelResponseScanner();
      header.collect("***", "X-Value: SENTINEL-PHI-7f3a91c2");
      assertThrows(AssertionError.class, () -> header.assertSafe("SENTINEL-PHI-7f3a91c2"));
      SentinelResponseScanner safe = new SentinelResponseScanner();
      safe.collect("S***2", "Content-Type: application/json");
      safe.assertSafe("SENTINEL-PHI-7f3a91c2");
   }
}
