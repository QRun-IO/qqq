/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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
import java.util.Objects;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.values.ValueBehaviorApplier;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueRangeBehavior.Behavior;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionAssert;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;


/*******************************************************************************
 ** Unit test for ValueOutsideOfRangeBehavior 
 *******************************************************************************/
class ValueRangeBehaviorTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test()
   {
      QInstance      qInstance = QContext.getQInstance();
      QTableMetaData table     = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);

      table.getField("noOfShoes").withBehavior(new ValueRangeBehavior().withMinValue(0));
      table.getField("cost").withBehavior(new ValueRangeBehavior().withMaxValue(new BigDecimal("3.50")));
      table.getField("price").withBehavior(new ValueRangeBehavior()
         .withMin(BigDecimal.ZERO, false, Behavior.CLIP, new BigDecimal(".01"))
         .withMaxValue(new BigDecimal(100)).withMaxAllowEqualTo(false));

      List<QRecord> recordList = List.of(
         new QRecord().withValue("id", 1).withValue("noOfShoes", -1).withValue("cost", new BigDecimal("3.50")).withValue("price", new BigDecimal(-1)),
         new QRecord().withValue("id", 2).withValue("noOfShoes", 0).withValue("cost", new BigDecimal("3.51")).withValue("price", new BigDecimal(200)),
         new QRecord().withValue("id", 3).withValue("noOfShoes", 1).withValue("cost", new BigDecimal("3.50")).withValue("price", new BigDecimal("99.99"))
      );
      ValueBehaviorApplier.applyFieldBehaviors(ValueBehaviorApplier.Action.INSERT, qInstance, table, recordList, null);

      {
         QRecord record = getRecordById(recordList, 1);
         assertEquals(-1, record.getValueInteger("noOfShoes")); // error (but didn't change value)
         assertEquals(new BigDecimal("3.50"), record.getValueBigDecimal("cost")); // all okay
         assertEquals(new BigDecimal("0.01"), record.getValueBigDecimal("price")); // got clipped
         assertThat(record.getErrors())
            .hasSize(1)
            .anyMatch(e -> e.getMessage().equals("The value for No Of Shoes is too small (minimum allowed value is 0)"));
      }

      {
         QRecord record = getRecordById(recordList, 2);
         assertEquals(0, record.getValueInteger("noOfShoes")); // all ok
         assertEquals(new BigDecimal("3.51"), record.getValueBigDecimal("cost")); // error (but didn't change value)
         assertEquals(new BigDecimal(200), record.getValueBigDecimal("price")); // error (but didn't change value)
         assertThat(record.getErrors())
            .hasSize(2)
            .anyMatch(e -> e.getMessage().equals("The value for Cost is too large (maximum allowed value is 3.50)"))
            .anyMatch(e -> e.getMessage().equals("The value for Price is too large (maximum allowed value is less than 100)"));
      }

      {
         QRecord record = getRecordById(recordList, 3);
         assertEquals(1, record.getValueInteger("noOfShoes")); // all ok
         assertEquals(new BigDecimal("3.50"), record.getValueBigDecimal("cost")); // all ok
         assertEquals(new BigDecimal("99.99"), record.getValueBigDecimal("price")); // all ok
         assertThat(record.getErrors()).isNullOrEmpty();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testValidation()
   {
      QInstance      qInstance      = QContext.getQInstance();
      QTableMetaData table          = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      QFieldMetaData noOfShoesField = table.getField("noOfShoes");
      QFieldMetaData firstNameField = table.getField("firstName");

      CollectionAssert.assertThat(new ValueRangeBehavior().validateBehaviorConfiguration(table, noOfShoesField))
         .matchesAll(List.of("Either minValue or maxValue (or both) must be set."), Objects::equals);

      CollectionAssert.assertThat(new ValueRangeBehavior().withMinValue(0).validateBehaviorConfiguration(table, noOfShoesField)).isNullOrEmpty();
      CollectionAssert.assertThat(new ValueRangeBehavior().withMaxValue(100).validateBehaviorConfiguration(table, noOfShoesField)).isNullOrEmpty();
      CollectionAssert.assertThat(new ValueRangeBehavior().withMinValue(0).withMaxValue(100).validateBehaviorConfiguration(table, noOfShoesField)).isNullOrEmpty();

      CollectionAssert.assertThat(new ValueRangeBehavior().withMinValue(1).withMaxValue(0).validateBehaviorConfiguration(table, noOfShoesField))
         .matchesAll(List.of("minValue must be >= maxValue."), Objects::equals);

      CollectionAssert.assertThat(new ValueRangeBehavior().withMinValue(1).validateBehaviorConfiguration(table, firstNameField))
         .matchesAll(List.of("can only be applied to a numeric type field."), Objects::equals);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QRecord getRecordById(List<QRecord> recordList, Integer id)
   {
      Optional<QRecord> recordOpt = recordList.stream().filter(r -> r.getValueInteger("id").equals(id)).findFirst();
      if(recordOpt.isEmpty())
      {
         fail("Didn't find record with id=" + id);
      }
      return (recordOpt.get());
   }

}