/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.columnstats;


import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.ExamplePersonalizer;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessHelper;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.security.FieldSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Unit test for ColumnStatsStep 
 *******************************************************************************/
class ColumnStatsStepTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testEmptyStringAndNullRollUpTogether() throws QException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      insertInput.setRecords(List.of(
         new QRecord().withValue("noOfShoes", 1).withValue("lastName", "Simpson"),
         new QRecord().withValue("noOfShoes", 2).withValue("lastName", "Simpson"),
         new QRecord().withValue("noOfShoes", 2).withValue("lastName", "Simpson"),
         new QRecord().withValue("noOfShoes", 2).withValue("lastName", ""), // this record and the next one -
         new QRecord().withValue("noOfShoes", 3).withValue("lastName", null), // this record and the previous - should both come out as null below
         new QRecord().withValue("noOfShoes", null).withValue("lastName", "Flanders")
      ));
      new InsertAction().execute(insertInput);

      RunBackendStepInput input = new RunBackendStepInput();
      input.addValue("tableName", TestUtils.TABLE_NAME_PERSON_MEMORY);
      input.addValue("fieldName", "lastName");
      input.addValue("orderBy", "count.desc");

      RunBackendStepOutput output = new RunBackendStepOutput();
      new ColumnStatsStep().run(input, output);

      Map<String, Serializable> values = output.getValues();

      @SuppressWarnings("unchecked")
      List<QRecord> valueCounts = (List<QRecord>) values.get("valueCounts");

      assertThat(valueCounts.get(0).getValues())
         .hasFieldOrPropertyWithValue("lastName", "Simpson")
         .hasFieldOrPropertyWithValue("count", 3)
         .hasFieldOrPropertyWithValue("percent", new BigDecimal("50.00"));

      assertThat(valueCounts.get(1).getValues())
         .hasFieldOrPropertyWithValue("lastName", null)
         .hasFieldOrPropertyWithValue("count", 2) // here's the assert for the "" and null record above.
         .hasFieldOrPropertyWithValue("percent", new BigDecimal("33.33"));

      assertThat(valueCounts.get(2).getValues())
         .hasFieldOrPropertyWithValue("lastName", "Flanders")
         .hasFieldOrPropertyWithValue("count", 1)
         .hasFieldOrPropertyWithValue("percent", new BigDecimal("16.67"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDateTimesRollupByHour() throws QException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      insertInput.setRecords(List.of(
         new QRecord().withValue("timestamp", Instant.parse("2024-01-31T09:59:01Z")),
         new QRecord().withValue("timestamp", Instant.parse("2024-01-31T09:59:59Z")),
         new QRecord().withValue("timestamp", Instant.parse("2024-01-31T10:00:00Z")),
         new QRecord().withValue("timestamp", Instant.parse("2024-01-31T10:01:01Z")),
         new QRecord().withValue("timestamp", Instant.parse("2024-01-31T10:59:59Z")),
         new QRecord().withValue("timestamp", null)
      ));
      new InsertAction().execute(insertInput);

      RunBackendStepInput input = new RunBackendStepInput();
      input.addValue("tableName", TestUtils.TABLE_NAME_PERSON_MEMORY);
      input.addValue("fieldName", "timestamp");
      input.addValue("orderBy", "count.desc");

      RunBackendStepOutput output = new RunBackendStepOutput();
      new ColumnStatsStep().run(input, output);

      Map<String, Serializable> values = output.getValues();

      @SuppressWarnings("unchecked")
      List<QRecord> valueCounts = (List<QRecord>) values.get("valueCounts");

      assertThat(valueCounts.get(0).getValues())
         .hasFieldOrPropertyWithValue("timestamp", Instant.parse("2024-01-31T10:00:00Z"))
         .hasFieldOrPropertyWithValue("count", 3);

      assertThat(valueCounts.get(1).getValues())
         .hasFieldOrPropertyWithValue("timestamp", Instant.parse("2024-01-31T09:00:00Z"))
         .hasFieldOrPropertyWithValue("count", 2);

      assertThat(valueCounts.get(2).getValues())
         .hasFieldOrPropertyWithValue("timestamp", null)
         .hasFieldOrPropertyWithValue("count", 1);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPersonalizationLeadsToNoFieldErrorWhenAppropriate() throws QException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      insertInput.setRecords(List.of(new QRecord().withValue("noOfShoes", 1).withValue("lastName", "Simpson")));
      new InsertAction().execute(insertInput);

      RunBackendStepInput input = new RunBackendStepInput();
      input.addValue("tableName", TestUtils.TABLE_NAME_PERSON_MEMORY);
      input.addValue("fieldName", "lastName");
      input.addValue("orderBy", "count.desc");

      RunBackendStepOutput output = new RunBackendStepOutput();
      new ColumnStatsStep().run(input, output);
      Map<String, Serializable> values = output.getValues();

      /////////////////////////////////////////////////
      // by default assert we get values as expected //
      /////////////////////////////////////////////////
      @SuppressWarnings("unchecked")
      List<QRecord> valueCounts = (List<QRecord>) values.get("valueCounts");
      assertThat(valueCounts.get(0).getValues())
         .hasFieldOrPropertyWithValue("lastName", "Simpson")
         .hasFieldOrPropertyWithValue("count", 1);

      ///////////////////////////////////////////////////////////////////////////////
      // now personalize the table to remove the field we're trying to do stats on //
      ///////////////////////////////////////////////////////////////////////////////
      String userId = "jdoe";
      ExamplePersonalizer.registerInQInstance();
      ExamplePersonalizer.addCustomizableTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      ExamplePersonalizer.addFieldToRemoveForUserId(TestUtils.TABLE_NAME_PERSON_MEMORY, "lastName", userId);
      QContext.getQSession().getUser().setIdReference(userId);

      ///////////////////////////////////////////////////////////////////////////////////////////////
      // we shoudl get an exception if we re-run now -- this user isn't allowed to see that field. //
      ///////////////////////////////////////////////////////////////////////////////////////////////
      assertThatThrownBy(() -> new ColumnStatsStep().run(input, new RunBackendStepOutput()))
         .rootCause()
         .hasMessageContaining("Field [lastName] was not found in table");

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFailIfCountOverLimit() throws QException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      insertInput.setRecords(List.of(
         new QRecord().withValue("lastName", "Simpson"),
         new QRecord().withValue("lastName", "Burns"),
         new QRecord().withValue("lastName", "Smithers"),
         new QRecord().withValue("lastName", "Flanders")
      ));
      new InsertAction().execute(insertInput);

      RunBackendStepInput input = new RunBackendStepInput();
      input.addValue("tableName", TestUtils.TABLE_NAME_PERSON_MEMORY);
      input.addValue("fieldName", "lastName");
      input.addValue("orderBy", "count.desc");

      /////////////////////////////////////////////////////////////////
      // with a limit smaller than the # of rows, should throw error //
      /////////////////////////////////////////////////////////////////
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      ColumnStatsTableConfig.ofOrWithNew(table).withFailIfCountOverLimit(3);
      assertThatThrownBy(() -> new ColumnStatsStep().run(input, new RunBackendStepOutput()))
         .rootCause().hasMessageContaining("Unable to calculate Column Statistics.  Your filter matches too many rows (4). The limit for this table is 3.");

      ///////////////////////////////////////////////
      // change limit higher, and confirm no error //
      ///////////////////////////////////////////////
      ColumnStatsTableConfig.ofOrWithNew(table).withFailIfCountOverLimit(5);
      new ColumnStatsStep().run(input, new RunBackendStepOutput());

      /////////////////////////
      // null limit no error //
      /////////////////////////
      ColumnStatsTableConfig.ofOrWithNew(table).withFailIfCountOverLimit(null);
      new ColumnStatsStep().run(input, new RunBackendStepOutput());

      //////////////////////////////////////////////////////
      // no ColumnStatsTableConfig on the table works too //
      //////////////////////////////////////////////////////
      table.getSupplementalMetaData().remove(ColumnStatsTableConfig.NAME);
      new ColumnStatsStep().run(input, new RunBackendStepOutput());

      ///////////////////////////////////////////////
      // verify config can be set as process value //
      ///////////////////////////////////////////////
      input.addValue("ColumnStatsTableConfig", new ColumnStatsTableConfig().withFailIfCountOverLimit(2));
      assertThatThrownBy(() -> new ColumnStatsStep().run(input, new RunBackendStepOutput()))
         .rootCause().hasMessageContaining("Unable to calculate Column Statistics.  Your filter matches too many rows (4). The limit for this table is 2.");
   }



   /*******************************************************************************
    ** Statistics are refused for a field the caller cannot read in full, even
    ** when the process run does not enforce field access; and the results are
    ** tagged with the analysed table, so process-record field access keeps them
    ** instead of withholding table-less records (#1367).
    *******************************************************************************/
   @Test
   void testFieldAccess() throws QException
   {
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecords(List.of(
         new QRecord().withValue("firstName", "Homer").withValue("lastName", "Simpson"),
         new QRecord().withValue("firstName", "Marge").withValue("lastName", "Simpson"))));

      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).getField("lastName")
         .setFieldSecurityLock(new FieldSecurityLock().withDefaultBehavior(FieldSecurityLock.Behavior.MASK));

      RunBackendStepInput restrictedInput = new RunBackendStepInput();
      restrictedInput.addValue("tableName", TestUtils.TABLE_NAME_PERSON_MEMORY);
      restrictedInput.addValue("fieldName", "lastName");
      assertThat(restrictedInput.isFieldAccessEnforced()).isFalse();
      assertThatThrownBy(() -> new ColumnStatsStep().run(restrictedInput, new RunBackendStepOutput()))
         .isInstanceOf(QPermissionDeniedException.class);

      RunBackendStepInput input = new RunBackendStepInput();
      input.addValue("tableName", TestUtils.TABLE_NAME_PERSON_MEMORY);
      input.addValue("fieldName", "firstName");
      RunBackendStepOutput output = new RunBackendStepOutput();
      new ColumnStatsStep().run(input, output);

      @SuppressWarnings("unchecked")
      List<QRecord> valueCounts = (List<QRecord>) output.getValues().get("valueCounts");
      QRecord       statsRecord = (QRecord) output.getValues().get("statsRecord");
      assertThat(valueCounts).hasSize(2).allMatch(record -> TestUtils.TABLE_NAME_PERSON_MEMORY.equals(record.getTableName()));
      assertThat(statsRecord.getTableName()).isEqualTo(TestUtils.TABLE_NAME_PERSON_MEMORY);

      FieldAccessHelper.applyToProcessRecords("columnStats", null, valueCounts);
      FieldAccessHelper.applyToProcessRecords("columnStats", null, List.of(statsRecord));
      assertThat(valueCounts.get(0).getValueString("firstName")).isIn("Homer", "Marge");
      assertThat(valueCounts.get(0).getValueInteger("count")).isEqualTo(1);
      assertThat(statsRecord.getValueInteger("count")).isEqualTo(2);
   }

}
