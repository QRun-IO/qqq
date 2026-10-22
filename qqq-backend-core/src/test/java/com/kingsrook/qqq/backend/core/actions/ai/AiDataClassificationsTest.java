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

package com.kingsrook.qqq.backend.core.actions.ai;


import java.util.Arrays;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.ai.exposure.AiExposurePolicy;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Classification ordering, metadata fallback and exposure independence.
 *******************************************************************************/
class AiDataClassificationsTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOrderingAndMax()
   {
      List<DataClassification> ordered = List.of(DataClassification.PUBLIC, DataClassification.CONFIDENTIAL, DataClassification.PII, DataClassification.PHI);
      assertEquals(ordered, Arrays.asList(DataClassification.values()));
      assertNull(DataClassification.max(null, null));
      for(int i = 0; i < ordered.size(); i++)
      {
         DataClassification classification = ordered.get(i);
         assertTrue(classification.isAtLeast(null));
         assertEquals(classification, DataClassification.max(null, classification));
         assertEquals(classification, DataClassification.max(classification, null));
         for(int j = 0; j < ordered.size(); j++)
         {
            assertEquals(i >= j, classification.isAtLeast(ordered.get(j)));
            assertEquals(ordered.get(Math.max(i, j)), DataClassification.max(classification, ordered.get(j)));
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFieldFallbackAndHighest()
   {
      QTableMetaData table = new QTableMetaData().withName("classified")
         .withSupplementalMetaData(new AiTableMetaData().withClassification(DataClassification.CONFIDENTIAL))
         .withField(new QFieldMetaData("plain", QFieldType.STRING))
         .withField(new QFieldMetaData("unset", QFieldType.STRING).withSupplementalMetaData(new AiFieldMetaData()))
         .withField(new QFieldMetaData("public", QFieldType.STRING).withSupplementalMetaData(new AiFieldMetaData().withClassification(DataClassification.PUBLIC)))
         .withField(new QFieldMetaData("pii", QFieldType.STRING).withSupplementalMetaData(new AiFieldMetaData().withClassification(DataClassification.PII)))
         .withField(new QFieldMetaData("phi", QFieldType.STRING).withIsHidden(true).withSupplementalMetaData(new AiFieldMetaData().withClassification(DataClassification.PHI)));

      assertEquals(DataClassification.CONFIDENTIAL, AiDataClassifications.ofTable(table));
      for(String fieldName : Arrays.asList("plain", "unset", "missing", null))
      {
         assertEquals(DataClassification.CONFIDENTIAL, AiDataClassifications.ofField(table, fieldName));
      }
      assertEquals(DataClassification.PUBLIC, AiDataClassifications.ofField(table, "public"));
      assertEquals(DataClassification.PHI, AiDataClassifications.highest(table));
      assertEquals(DataClassification.PII, AiDataClassifications.highest(table, List.of("plain", "pii")));
      assertEquals(DataClassification.PUBLIC, AiDataClassifications.highest(table, List.of("public")));
      assertEquals(DataClassification.CONFIDENTIAL, AiDataClassifications.highest(table, Arrays.asList("missing", null)));
      assertEquals(DataClassification.CONFIDENTIAL, AiDataClassifications.highest(table, List.of()));
      assertEquals(DataClassification.CONFIDENTIAL, AiDataClassifications.highest(table, null));
      AiTableMetaData.of(table).setClassification(DataClassification.PHI);
      table.getFields().remove("phi");
      assertEquals(DataClassification.PHI, AiDataClassifications.highest(table));
      AiTableMetaData.of(table).setClassification(null);
      assertEquals(DataClassification.PII, AiDataClassifications.highest(table));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNullAndUnclassifiedInputs()
   {
      for(QTableMetaData table : Arrays.asList(null, new QTableMetaData(), new QTableMetaData().withSupplementalMetaData(new AiTableMetaData())))
      {
         assertNull(AiDataClassifications.ofTable(table));
         assertNull(AiDataClassifications.ofField(table, null));
         assertNull(AiDataClassifications.ofField(table, "missing"));
         assertNull(AiDataClassifications.highest(table));
         assertNull(AiDataClassifications.highest(table, null));
         assertNull(AiDataClassifications.highest(table, List.of()));
         assertNull(AiDataClassifications.highest(table, Arrays.asList("missing", null)));
      }
      QTableMetaData table = new QTableMetaData().withSupplementalMetaData(new AiTableMetaData().withClassification(DataClassification.PII));
      table.setFields(null);
      assertEquals(DataClassification.PII, AiDataClassifications.highest(table));
      assertEquals(DataClassification.PII, AiDataClassifications.ofField(table, "missing"));
      table.withField(new QFieldMetaData("unclassified", QFieldType.STRING));
      table.getFields().put("nullField", null);
      assertEquals(DataClassification.PII, AiDataClassifications.highest(table));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMetadataAndExposureUnchanged()
   {
      AiTableMetaData tableMetaData = new AiTableMetaData();
      AiFieldMetaData fieldMetaData = new AiFieldMetaData();
      assertNull(tableMetaData.getClassification());
      assertNull(fieldMetaData.getClassification());
      assertNull(tableMetaData.getExposure());
      assertNull(fieldMetaData.getExposure());
      assertSame(tableMetaData, tableMetaData.withClassification(DataClassification.PII));
      assertSame(fieldMetaData, fieldMetaData.withClassification(DataClassification.PHI));
      assertEquals(DataClassification.PII, tableMetaData.getClassification());
      assertEquals(DataClassification.PHI, fieldMetaData.getClassification());

      QFieldMetaData field = new QFieldMetaData("name", QFieldType.STRING).withSupplementalMetaData(fieldMetaData);
      QTableMetaData table = new QTableMetaData().withName("classified").withSupplementalMetaData(tableMetaData).withField(field);
      AiExposurePolicy policy = new AiExposurePolicy(new AiToolKernelSettings());
      assertEquals(AiExposure.VALUES, policy.getTableExposure(table));
      assertEquals(AiExposure.VALUES, policy.getFieldExposure(table, field));
      tableMetaData.withExposure(AiExposure.METADATA_ONLY).withAiDescription("table description");
      fieldMetaData.withExposure(AiExposure.MASKED).withAiDescription("field description");
      tableMetaData.setClassification(DataClassification.CONFIDENTIAL);
      fieldMetaData.setClassification(DataClassification.PUBLIC);
      assertEquals(DataClassification.PUBLIC, fieldMetaData.getClassification());
      assertEquals(AiExposure.METADATA_ONLY, policy.getTableExposure(table));
      assertEquals(AiExposure.METADATA_ONLY, policy.getFieldExposure(table, field));
      assertEquals(AiExposure.MASKED, fieldMetaData.getExposure());
      assertEquals("field description", fieldMetaData.getAiDescription());
      assertFalse(tableMetaData.includeInPartialFrontendMetaData());
      assertFalse(tableMetaData.includeInFullFrontendMetaData());
      assertFalse(fieldMetaData.includeInFrontendMetaData());

      AiTableMetaData cloned = AiTableMetaData.of(table.clone());
      assertEquals(DataClassification.CONFIDENTIAL, cloned.getClassification());
      assertEquals(AiExposure.METADATA_ONLY, cloned.getExposure());
      assertEquals("table description", cloned.getAiDescription());
      tableMetaData.setClassification(null);
      fieldMetaData.setClassification(null);
      assertNull(tableMetaData.getClassification());
      assertNull(fieldMetaData.getClassification());
      assertEquals(DataClassification.CONFIDENTIAL, cloned.getClassification());
   }
}
