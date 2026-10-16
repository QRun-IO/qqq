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

package com.kingsrook.qqq.backend.core.actions.permissions;



import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** qqq#1260: the sentinel scanner must not report system timestamps (whose
 ** fraction digits can contain any five-digit run) but must still report any
 ** other occurrence of a private part.
 *******************************************************************************/
class FieldAccessSentinelScannerTest
{
   /*******************************************************************************
    ** The exact CI failure shape: masked values plus a createDate whose
    ** nanoseconds contain a window of the SSN sentinel.
    *******************************************************************************/
   @Test
   void timestampFractionIsNotALeak()
   {
      String csv = "\"1\",\"2026-10-16T14:15:57.918273645Z\",\"S***@example.com\",\"S***5\"";
      assertTrue(csv.contains("91827"), "the raw output does contain a sentinel window (the old false positive)");
      assertDoesNotThrow(() -> FieldAccessSentinelScanner.assertSafe(csv, "918273645", "ABCDEFG"));
   }



   /*******************************************************************************
    ** Real leaks are still reported, including partial masks and digit runs
    ** that are not part of an ISO-8601 instant.
    *******************************************************************************/
   @Test
   void realLeaksAreStillReported()
   {
      assertThrows(AssertionError.class, () -> FieldAccessSentinelScanner.assertSafe("\"S***73645\"", "918273645"));
      assertThrows(AssertionError.class, () -> FieldAccessSentinelScanner.assertSafe("\"SENTINEL-SSN-918273645\"", "918273645"));
      assertThrows(AssertionError.class, () -> FieldAccessSentinelScanner.assertSafe("\"57.918273645\"", "918273645"));
      assertThrows(AssertionError.class, () -> FieldAccessSentinelScanner.assertSafe("\"2026-10-16 918273645Z\"", "918273645"));
      assertThrows(AssertionError.class, () -> FieldAccessSentinelScanner.assertSafe("\"NOTES-ABCDEFG\"", "ABCDEFG"));
   }
}
