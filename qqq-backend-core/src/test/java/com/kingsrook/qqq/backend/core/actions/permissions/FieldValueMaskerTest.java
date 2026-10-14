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


import com.kingsrook.qqq.backend.core.actions.ai.exposure.AiValueMasker;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldMaskStyle;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldValueMasker;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Secured field access contract tests.
 *******************************************************************************/
class FieldValueMaskerTest
{



   /*******************************************************************************
    ** Verify the secured-data contract.
    *******************************************************************************/
   @Test
   void styles()
   {
      assertEquals("s***@example.com", FieldValueMasker.mask(" secret@example.com ", QFieldType.STRING, FieldMaskStyle.EMAIL));
      assertEquals("***", FieldValueMasker.mask("not email", null, FieldMaskStyle.EMAIL));
      assertEquals("***6789", FieldValueMasker.mask("123-45-6789", null, FieldMaskStyle.LAST_4));
      assertEquals("***", FieldValueMasker.mask("letters12345", null, FieldMaskStyle.LAST_4));
      assertEquals("a***z", FieldValueMasker.mask("abcdefz", null, FieldMaskStyle.FIRST_LAST));
      assertEquals("***", FieldValueMasker.mask("abcd", null, FieldMaskStyle.FIRST_LAST));
      assertEquals("***", FieldValueMasker.mask("secret@example.com", null, FieldMaskStyle.FULL));
      for(FieldMaskStyle style : FieldMaskStyle.values())
      {
         assertNull(FieldValueMasker.mask(null, null, style));
      }
      assertEquals("s***@example.com", AiValueMasker.mask("secret@example.com", QFieldType.STRING));
      assertEquals("***6789", AiValueMasker.mask("123-45-6789", QFieldType.STRING));
      assertEquals("a***z", AiValueMasker.mask("abcdefz", QFieldType.STRING));
      assertEquals("***", AiValueMasker.mask("abcd", QFieldType.STRING));
   }
}
