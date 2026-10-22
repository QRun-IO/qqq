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


import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import static org.junit.jupiter.api.Assertions.assertFalse;


/*******************************************************************************
 ** Secured field access contract tests.
 *******************************************************************************/
class FieldAccessSentinelScanner
{



   /*******************************************************************************
    ** Scan serialized outputs for every private substring of at least five characters.
    *******************************************************************************/
   static void assertSafe(Object output, String... privateParts)
   {
      String serialized = withoutInstants(JsonUtils.toJson(output));
      for(String privatePart : privateParts)
      {
         for(int offset = 0; offset + 5 <= privatePart.length(); offset++)
         {
            String window = privatePart.substring(offset, offset + 5);
            int    found  = serialized.indexOf(window);
            assertFalse(found >= 0, () -> "Unmasked sentinel in output: [" + window + "] in ..." + serialized.substring(Math.max(0, found - 40), Math.min(serialized.length(), found + 45)) + "...");
         }
      }
   }



   /*******************************************************************************
    ** System timestamps are not secured data, but their fraction digits can
    ** contain any five-digit run - e.g. createDate 2026-10-16T14:15:57.918273645Z
    ** matched the SSN sentinel and failed CI (qqq#1260). Replace ISO-8601
    ** instants before scanning; every other digit run is still checked.
    *******************************************************************************/
   static String withoutInstants(String serialized)
   {
      return (serialized.replaceAll("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,9})?Z", "<instant>"));
   }
}
