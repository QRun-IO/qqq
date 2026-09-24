/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.backend.core.model.metadata.fields;


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.values.ValueBehaviorApplier;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit test for ValueTooLongBehavior
 *******************************************************************************/
@TestMethodOrder(MethodOrderer.MethodName.class)
class ValueTooLongBehaviorTest extends BaseTest
{
   private static final QLogger LOG = QLogger.getLogger(ValueTooLongBehaviorTest.class);



   /*******************************************************************************
    ** TRUNCATE clips the value to maxLength without an ellipsis.
    *******************************************************************************/
   @Test
   void testApply_truncate_clipsToMaxLength()
   {
      QTableMetaData table = buildTableWithMaxLength(5);
      QFieldMetaData field = table.getField("name");

      QRecord record = new QRecord().withValue("name", "TooLongString");
      ValueTooLongBehavior.TRUNCATE.apply(ValueBehaviorApplier.Action.INSERT, List.of(record),
         QContext.getQInstance(), table, field);

      assertThat(record.getValueString("name")).isEqualTo("TooLo");
      assertThat(record.getErrors()).isEmpty();
   }



   /*******************************************************************************
    ** TRUNCATE_ELLIPSIS clips and appends "..." using the remaining space.
    *******************************************************************************/
   @Test
   void testApply_truncateEllipsis_appendsDots()
   {
      QTableMetaData table = buildTableWithMaxLength(8);
      QFieldMetaData field = table.getField("name");

      QRecord record = new QRecord().withValue("name", "TooLongString");
      ValueTooLongBehavior.TRUNCATE_ELLIPSIS.apply(ValueBehaviorApplier.Action.INSERT, List.of(record),
         QContext.getQInstance(), table, field);

      String result = record.getValueString("name");
      assertThat(result).endsWith("...");
      assertThat(result.length()).isLessThanOrEqualTo(8);
      assertThat(record.getErrors()).isEmpty();
   }



   /*******************************************************************************
    ** ERROR adds a BadInputStatusMessage and does not mutate the value.
    *******************************************************************************/
   @Test
   void testApply_error_addsErrorAndLeavesValueUnchanged()
   {
      QTableMetaData table = buildTableWithMaxLength(5);
      QFieldMetaData field = table.getField("name");

      String  tooLong = "TooLongString";
      QRecord record  = new QRecord().withValue("name", tooLong);
      ValueTooLongBehavior.ERROR.apply(ValueBehaviorApplier.Action.INSERT, List.of(record),
         QContext.getQInstance(), table, field);

      assertThat(record.getValueString("name")).isEqualTo(tooLong);
      assertThat(record.getErrors()).hasSize(1);
      assertThat(record.getErrors().get(0).toString()).contains("too long");
   }



   /*******************************************************************************
    ** PASS_THROUGH does nothing to the record regardless of value length.
    *******************************************************************************/
   @Test
   void testApply_passThrough_doesNothing()
   {
      QTableMetaData table = buildTableWithMaxLength(5);
      QFieldMetaData field = table.getField("name");

      String  tooLong = "TooLongString";
      QRecord record  = new QRecord().withValue("name", tooLong);
      ValueTooLongBehavior.PASS_THROUGH.apply(ValueBehaviorApplier.Action.INSERT, List.of(record),
         QContext.getQInstance(), table, field);

      assertThat(record.getValueString("name")).isEqualTo(tooLong);
      assertThat(record.getErrors()).isEmpty();
   }



   /*******************************************************************************
    ** Applying any behavior to a non-STRING field must be a no-op (no error added,
    ** value unchanged) — the class logs a debug message and returns early.
    *******************************************************************************/
   @Test
   void testApply_nonStringField_isNoOp()
   {
      QTableMetaData table  = buildTableWithMaxLength(5);
      QFieldMetaData field  = new QFieldMetaData("count", QFieldType.INTEGER).withMaxLength(3);
      QRecord        record = new QRecord().withValue("count", 99999);

      ValueTooLongBehavior.TRUNCATE.apply(ValueBehaviorApplier.Action.INSERT, List.of(record),
         QContext.getQInstance(), table, field);

      assertThat(record.getValueInteger("count")).isEqualTo(99999);
      assertThat(record.getErrors()).isEmpty();
   }



   /*******************************************************************************
    ** Applying any behavior when the field has no maxLength must be a no-op.
    *******************************************************************************/
   @Test
   void testApply_noMaxLength_isNoOp()
   {
      QTableMetaData table  = buildTableWithMaxLength(5);
      QFieldMetaData field  = new QFieldMetaData("name", QFieldType.STRING); // no maxLength
      QRecord        record = new QRecord().withValue("name", "TooLongString");

      ValueTooLongBehavior.TRUNCATE.apply(ValueBehaviorApplier.Action.INSERT, List.of(record),
         QContext.getQInstance(), table, field);

      assertThat(record.getValueString("name")).isEqualTo("TooLongString");
      assertThat(record.getErrors()).isEmpty();
   }



   /*******************************************************************************
    ** A null field value must not throw any exception under any behavior.
    *******************************************************************************/
   @Test
   void testApply_nullValue_doesNotThrow()
   {
      QTableMetaData table  = buildTableWithMaxLength(5);
      QFieldMetaData field  = table.getField("name");
      QRecord        record = new QRecord(); // no value for "name"

      for(ValueTooLongBehavior behavior : ValueTooLongBehavior.values())
      {
         behavior.apply(ValueBehaviorApplier.Action.INSERT, List.of(record),
            QContext.getQInstance(), table, field);
      }

      assertThat(record.getErrors()).isEmpty();
   }



   /*******************************************************************************
    ** getDefault must return PASS_THROUGH.
    *******************************************************************************/
   @Test
   void testGetDefault_returnsPassThrough()
   {
      assertThat(ValueTooLongBehavior.TRUNCATE.getDefault()).isEqualTo(ValueTooLongBehavior.PASS_THROUGH);
   }



   /*******************************************************************************
    ** Helper — build a simple QTableMetaData with one STRING field "name" and
    ** the given maxLength.  Uses the live QInstance from the test context so that
    ** field label resolution works.
    *******************************************************************************/
   private QTableMetaData buildTableWithMaxLength(int maxLength)
   {
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON);
      //////////////////////////////////////////////////////////////////////////
      // For a self-contained field definition, construct an ad-hoc table.    //
      // We only need it for the table.getName() call inside the behavior impl //
      //////////////////////////////////////////////////////////////////////////
      QTableMetaData adHocTable = new QTableMetaData()
         .withName("testTable")
         .withField(new QFieldMetaData("name", QFieldType.STRING)
            .withLabel("Name")
            .withMaxLength(maxLength));
      return adHocTable;
   }

}
