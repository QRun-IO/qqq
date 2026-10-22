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

package com.kingsrook.qqq.backend.core.model.metadata.fields;


import java.math.BigDecimal;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.values.ValueBehaviorApplier;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendFieldValidation;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for ValuePatternBehavior, and the validation rules published to
 ** frontends (QFrontendFieldValidation).
 *******************************************************************************/
class ValuePatternBehaviorTest extends BaseTest
{

   /*******************************************************************************
    ** non-matching values are refused; empty and absent values are left alone.
    *******************************************************************************/
   @Test
   void testApply()
   {
      QInstance      qInstance = QContext.getQInstance();
      QTableMetaData table     = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      table.getField("email").withBehavior(new ValuePatternBehavior("[^@\\s]+@[^@\\s]+\\.[a-z]{2,}", null));

      List<QRecord> records = List.of(
         new QRecord().withValue("id", 1).withValue("email", "darin@example.com"),
         new QRecord().withValue("id", 2).withValue("email", "not an email"),
         new QRecord().withValue("id", 3).withValue("email", ""),
         new QRecord().withValue("id", 4));
      ValueBehaviorApplier.applyFieldBehaviors(ValueBehaviorApplier.Action.INSERT, qInstance, table, records, null);

      assertThat(records.get(0).getErrors()).isNullOrEmpty();
      assertThat(records.get(1).getErrors()).hasSize(1).anyMatch(e -> e.getMessage().equals("The value for Email is not in the expected format"));
      assertThat(records.get(2).getErrors()).isNullOrEmpty();
      assertThat(records.get(3).getErrors()).isNullOrEmpty();

      //////////////////////////////////////////////////
      // the whole value must match (not just a part) //
      //////////////////////////////////////////////////
      table.getField("email").withBehavior(new ValuePatternBehavior("[A-Z]{3}", "Use three capital letters"));
      List<QRecord> more = List.of(new QRecord().withValue("id", 5).withValue("email", "xABCx"));
      ValueBehaviorApplier.applyFieldBehaviors(ValueBehaviorApplier.Action.UPDATE, qInstance, table, more, null);
      assertThat(more.get(0).getErrors()).hasSize(1).anyMatch(e -> e.getMessage().equals("Use three capital letters"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testValidateConfiguration()
   {
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      assertThat(new ValuePatternBehavior("[unclosed", null).validateBehaviorConfiguration(table, table.getField("email"))).anyMatch(e -> e.contains("not a valid regular expression"));
      assertThat(new ValuePatternBehavior(null, null).validateBehaviorConfiguration(table, table.getField("email"))).anyMatch(e -> e.contains("pattern is required"));
      assertThat(new ValuePatternBehavior("\\d+", null).validateBehaviorConfiguration(table, table.getField("noOfShoes"))).anyMatch(e -> e.contains("only to string fields"));
      assertThat(new ValuePatternBehavior("\\d+", null).validateBehaviorConfiguration(table, table.getField("email"))).isEmpty();
   }



   /*******************************************************************************
    ** the validation rules a frontend receives match what the server enforces.
    *******************************************************************************/
   @Test
   void testFrontendValidation()
   {
      QFieldMetaData plain = new QFieldMetaData("plain", QFieldType.INTEGER);
      assertNull(QFrontendFieldValidation.of(plain));

      QFieldMetaData name = new QFieldMetaData("name", QFieldType.STRING).withLabel("Name").withIsRequired(true).withMaxLength(40)
         .withBehavior(ValueTooLongBehavior.ERROR)
         .withBehavior(new ValuePatternBehavior("[A-Za-z ]+", null));
      QFrontendFieldValidation rules = QFrontendFieldValidation.of(name);
      assertTrue(rules.getRequired());
      assertEquals(40, rules.getMaxLength());
      assertEquals("ERROR", rules.getTooLongBehavior());
      assertEquals("[A-Za-z ]+", rules.getPattern());
      assertEquals("The value for Name is not in the expected format", rules.getPatternMessage());
      assertNull(rules.getMinValue());

      QFieldMetaData noBehavior = new QFieldMetaData("code", QFieldType.STRING).withMaxLength(10);
      assertEquals("PASS_THROUGH", QFrontendFieldValidation.of(noBehavior).getTooLongBehavior());

      QFieldMetaData price = new QFieldMetaData("price", QFieldType.DECIMAL).withBehavior(new ValueRangeBehavior()
         .withMinValue(BigDecimal.ZERO).withMinAllowEqualTo(false)
         .withMaxValue(new BigDecimal("1000.50")));
      QFrontendFieldValidation range = QFrontendFieldValidation.of(price);
      assertEquals("0", range.getMinValue());
      assertFalse(range.getMinInclusive());
      assertEquals("1000.50", range.getMaxValue());
      assertTrue(range.getMaxInclusive());

      ////////////////////////////////////////////////////////
      // a CLIP range is not a rule a client should enforce //
      ////////////////////////////////////////////////////////
      QFieldMetaData clipped = new QFieldMetaData("clipped", QFieldType.INTEGER).withBehavior(new ValueRangeBehavior()
         .withMin(0, true, ValueRangeBehavior.Behavior.CLIP, BigDecimal.ONE));
      assertNull(QFrontendFieldValidation.of(clipped));

      assertEquals("ERROR", new QFrontendFieldMetaData(name).getValidation().getTooLongBehavior());
   }
}
