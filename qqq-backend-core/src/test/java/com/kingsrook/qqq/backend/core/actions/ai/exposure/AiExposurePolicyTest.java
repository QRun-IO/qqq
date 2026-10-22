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

package com.kingsrook.qqq.backend.core.actions.ai.exposure;


import com.kingsrook.qqq.backend.core.actions.ai.AiExposure;
import com.kingsrook.qqq.backend.core.actions.ai.AiFieldMetaData;
import com.kingsrook.qqq.backend.core.actions.ai.AiTableMetaData;
import com.kingsrook.qqq.backend.core.actions.ai.AiToolKernelSettings;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.security.FieldSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit tests for AiExposurePolicy, AiValueMasker and the AI supplemental
 ** meta-data.
 *******************************************************************************/
class AiExposurePolicyTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableExposure()
   {
      AiExposurePolicy policy = new AiExposurePolicy(new AiToolKernelSettings());
      assertEquals(AiExposure.NONE, policy.getTableExposure(null));
      assertEquals(AiExposure.VALUES, policy.getTableExposure(new QTableMetaData().withName("blogPost")));
      assertEquals(AiExposure.NONE, policy.getTableExposure(new QTableMetaData().withName("assistantMessage")));
      assertEquals(AiExposure.NONE, policy.getTableExposure(new QTableMetaData().withName("userSession")));
      assertEquals(AiExposure.NONE, policy.getTableExposure(new QTableMetaData().withName("qqqOAuthToken")));
      assertEquals(AiExposure.METADATA_ONLY, policy.getTableExposure(new QTableMetaData().withName("assistantMessage").withSupplementalMetaData(new AiTableMetaData().withExposure(AiExposure.METADATA_ONLY))));
      assertTrue(AiExposurePolicy.isDefaultNoneTableName(null));

      AiExposurePolicy strictPolicy = new AiExposurePolicy(new AiToolKernelSettings().withDefaultExposure(AiExposure.METADATA_ONLY));
      assertEquals(AiExposure.METADATA_ONLY, strictPolicy.getTableExposure(new QTableMetaData().withName("blogPost")));

      AiExposurePolicy nullDefaultPolicy = new AiExposurePolicy(new AiToolKernelSettings().withDefaultExposure(null));
      assertEquals(AiExposure.VALUES, nullDefaultPolicy.getTableExposure(new QTableMetaData().withName("blogPost")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFieldExposureAndReasons()
   {
      AiExposurePolicy policy = new AiExposurePolicy(new AiToolKernelSettings());
      QTableMetaData   table  = new QTableMetaData().withName("blogPost");

      QFieldMetaData plain    = new QFieldMetaData("title", QFieldType.STRING);
      QFieldMetaData hidden   = new QFieldMetaData("x", QFieldType.STRING).withIsHidden(true);
      QFieldMetaData password = new QFieldMetaData("pw", QFieldType.PASSWORD);
      QFieldMetaData locked   = new QFieldMetaData("cost", QFieldType.DECIMAL).withFieldSecurityLock(new FieldSecurityLock());
      QFieldMetaData explicit = new QFieldMetaData("x2", QFieldType.STRING).withIsHidden(true).withSupplementalMetaData(new AiFieldMetaData().withExposure(AiExposure.MASKED));

      assertEquals(AiExposure.NONE, policy.getFieldExposure(table, null));
      assertEquals(AiExposure.VALUES, policy.getFieldExposure(table, plain));
      assertEquals(AiExposure.NONE, policy.getFieldExposure(table, hidden));
      assertEquals(AiExposure.NONE, policy.getFieldExposure(table, password));
      assertEquals(AiExposure.NONE, policy.getFieldExposure(table, locked));
      assertEquals(AiExposure.MASKED, policy.getFieldExposure(table, explicit));

      assertEquals("default", policy.getFieldExposureReason(table, plain));
      assertEquals("hidden", policy.getFieldExposureReason(table, hidden));
      assertEquals("password", policy.getFieldExposureReason(table, password));
      assertEquals("field security lock", policy.getFieldExposureReason(table, locked));
      assertEquals("metadata", policy.getFieldExposureReason(table, explicit));

      /////////////////////////////////////////
      // a table's exposure caps its fields' //
      /////////////////////////////////////////
      QTableMetaData metadataOnlyTable = new QTableMetaData().withName("t").withSupplementalMetaData(new AiTableMetaData().withExposure(AiExposure.METADATA_ONLY));
      assertEquals(AiExposure.METADATA_ONLY, policy.getFieldExposure(metadataOnlyTable, plain));
      assertEquals(AiExposure.METADATA_ONLY, policy.getFieldExposure(metadataOnlyTable, explicit));
      assertEquals("table", policy.getFieldExposureReason(metadataOnlyTable, plain));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testExposureOrdering()
   {
      assertEquals(AiExposure.NONE, AiExposure.VALUES.mostRestrictive(AiExposure.NONE));
      assertEquals(AiExposure.MASKED, AiExposure.MASKED.mostRestrictive(AiExposure.VALUES));
      assertEquals(AiExposure.MASKED, AiExposure.MASKED.mostRestrictive(null));
      assertTrue(AiExposure.VALUES.allowsValues());
      assertTrue(AiExposure.MASKED.allowsValues());
      assertFalse(AiExposure.METADATA_ONLY.allowsValues());
      assertFalse(AiExposure.NONE.allowsValues());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMasker()
   {
      assertNull(AiValueMasker.mask(null, null));
      assertEquals("j***@example.com", AiValueMasker.mask("jane.doe@example.com", QFieldType.STRING));
      assertEquals("***6789", AiValueMasker.mask("555-123-6789", QFieldType.STRING));
      assertEquals("***4567", AiValueMasker.mask("1234567", QFieldType.INTEGER));
      assertEquals("***", AiValueMasker.mask("12", QFieldType.INTEGER));
      assertEquals("***", AiValueMasker.mask("abcd", QFieldType.STRING));
      assertEquals("S***t", AiValueMasker.mask("Secret", null));
      assertEquals("a***c", AiValueMasker.mask("a@b@c", QFieldType.STRING));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSupplementalMetaData()
   {
      QTableMetaData table = new QTableMetaData().withName("t");
      assertNull(AiTableMetaData.of(table));
      assertNull(AiTableMetaData.of(null));

      AiTableMetaData aiTableMetaData = AiTableMetaData.ofOrWithNew(table).withExposure(AiExposure.MASKED).withAiDescription("desc");
      assertEquals("ai", aiTableMetaData.getType());
      assertEquals(AiExposure.MASKED, AiTableMetaData.of(table).getExposure());

      QTableMetaData clone = table.clone();
      AiTableMetaData cloned = AiTableMetaData.of(clone);
      assertNotSame(aiTableMetaData, cloned);
      assertEquals("desc", cloned.getAiDescription());
      assertEquals(AiExposure.MASKED, cloned.getExposure());

      QFieldMetaData field = new QFieldMetaData("f", QFieldType.STRING);
      assertNull(AiFieldMetaData.of(field));
      assertNull(AiFieldMetaData.of(null));
      field.withSupplementalMetaData(new AiFieldMetaData().withAiDescription("about f"));
      assertEquals("about f", AiFieldMetaData.of(field).getAiDescription());
      assertEquals("ai", AiFieldMetaData.of(field).getType());
   }
}
