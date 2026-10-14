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
      String serialized = JsonUtils.toJson(output);
      for(String privatePart : privateParts)
      {
         for(int offset = 0; offset + 5 <= privatePart.length(); offset++)
         {
            assertFalse(serialized.contains(privatePart.substring(offset, offset + 5)), "Unmasked sentinel in output");
         }
      }
   }
}
