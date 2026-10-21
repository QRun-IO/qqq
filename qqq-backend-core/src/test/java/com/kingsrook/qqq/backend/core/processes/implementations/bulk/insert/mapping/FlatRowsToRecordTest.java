/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.mapping;


import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.filehandling.TestFileToRows;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkInsertMapping;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadFileRow;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import com.kingsrook.qqq.backend.core.utils.collections.ListBuilder;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for FlatRowsToRecord 
 *******************************************************************************/
class FlatRowsToRecordTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFieldNameToHeaderNameMapping() throws QException
   {
      TestFileToRows fileToRows = new TestFileToRows(List.of(
         new Serializable[] { "id", "firstName", "Last Name", "Ignore", "cost" },
         new Serializable[] { 1, "Homer", "Simpson", true, "three fifty" },
         new Serializable[] { 2, "Marge", "Simpson", false, "" },
         new Serializable[] { 3, "Bart", "Simpson", "A", "99.95" },
         new Serializable[] { 4, "Ned", "Flanders", 3.1, "one$" },
         new Serializable[] { "", "", "", "", "" } // all blank row (we can get these at the bottoms of files) - make sure it doesn't become a record.
      ));

      BulkLoadFileRow header = fileToRows.next();

      FlatRowsToRecord rowsToRecord = new FlatRowsToRecord();

      BulkInsertMapping mapping = new BulkInsertMapping()
         .withFieldNameToHeaderNameMap(Map.of(
            "firstName", "firstName",
            "lastName", "Last Name",
            "cost", "cost"
         ))
         .withFieldNameToDefaultValueMap(Map.of(
            "noOfShoes", 2
         ))
         .withFieldNameToValueMapping(Map.of("cost", Map.of("three fifty", new BigDecimal("3.50"), "one$", new BigDecimal("1.00"))))
         .withTableName(TestUtils.TABLE_NAME_PERSON)
         .withHasHeaderRow(true);

      List<QRecord> records = rowsToRecord.nextPage(fileToRows, header, mapping, 1);
      assertEquals(1, records.size());
      assertEquals(List.of("Homer"), getValues(records, "firstName"));
      assertEquals(List.of("Simpson"), getValues(records, "lastName"));
      assertEquals(List.of(2), getValues(records, "noOfShoes"));
      assertEquals(List.of(new BigDecimal("3.50")), getValues(records, "cost"));
      assertEquals(4, records.get(0).getValues().size()); // make sure no additional values were set
      assertEquals(1, ((List<?>) records.get(0).getBackendDetail("fileRows")).size());
      assertEquals("Row 2", records.get(0).getBackendDetail("rowNos"));

      records = rowsToRecord.nextPage(fileToRows, header, mapping, 2);
      assertEquals(2, records.size());
      assertEquals(List.of("Marge", "Bart"), getValues(records, "firstName"));
      assertEquals(List.of(2, 2), getValues(records, "noOfShoes"));
      assertEquals(ListBuilder.of(null, new BigDecimal("99.95")), getValues(records, "cost"));
      assertEquals(1, ((List<?>) records.get(0).getBackendDetail("fileRows")).size());
      assertEquals("Row 3", records.get(0).getBackendDetail("rowNos"));
      assertEquals("Row 4", records.get(1).getBackendDetail("rowNos"));

      records = rowsToRecord.nextPage(fileToRows, header, mapping, Integer.MAX_VALUE);
      assertEquals(1, records.size());
      assertEquals(List.of("Ned"), getValues(records, "firstName"));
      assertEquals(List.of(2), getValues(records, "noOfShoes"));
      assertEquals(ListBuilder.of(new BigDecimal("1.00")), getValues(records, "cost"));
      assertEquals("Row 5", records.get(0).getBackendDetail("rowNos"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFieldNameToColumnIndexMapping() throws QException
   {
      TestFileToRows fileToRows = new TestFileToRows(List.of(
         //                   0, 1,       2,         3,    4
         new Serializable[] { 1, "Homer", "Simpson", true, "three fifty" },
         new Serializable[] { 2, "Marge", "Simpson", false, "" },
         new Serializable[] { 3, "Bart", "Simpson", "A", "99.95" },
         new Serializable[] { 4, "Ned", "Flanders", 3.1, "one$" }
      ));

      FlatRowsToRecord rowsToRecord = new FlatRowsToRecord();

      BulkInsertMapping mapping = new BulkInsertMapping()
         .withFieldNameToIndexMap(Map.of(
            "firstName", 1,
            "lastName", 2,
            "cost", 4
         ))
         .withFieldNameToDefaultValueMap(Map.of(
            "noOfShoes", 2
         ))
         .withFieldNameToValueMapping(Map.of("cost", Map.of("three fifty", new BigDecimal("3.50"), "one$", new BigDecimal("1.00"))))
         .withTableName(TestUtils.TABLE_NAME_PERSON)
         .withHasHeaderRow(false);

      List<QRecord> records = rowsToRecord.nextPage(fileToRows, null, mapping, 1);
      assertEquals(1, records.size());
      assertEquals(List.of("Homer"), getValues(records, "firstName"));
      assertEquals(List.of("Simpson"), getValues(records, "lastName"));
      assertEquals(List.of(2), getValues(records, "noOfShoes"));
      assertEquals(List.of(new BigDecimal("3.50")), getValues(records, "cost"));
      assertEquals(4, records.get(0).getValues().size()); // make sure no additional values were set
      assertEquals(1, ((List<?>) records.get(0).getBackendDetail("fileRows")).size());
      assertEquals("Row 1", records.get(0).getBackendDetail("rowNos"));

      records = rowsToRecord.nextPage(fileToRows, null, mapping, 2);
      assertEquals(2, records.size());
      assertEquals(List.of("Marge", "Bart"), getValues(records, "firstName"));
      assertEquals(List.of(2, 2), getValues(records, "noOfShoes"));
      assertEquals(ListBuilder.of(null, new BigDecimal("99.95")), getValues(records, "cost"));
      assertEquals(1, ((List<?>) records.get(0).getBackendDetail("fileRows")).size());
      assertEquals("Row 2", records.get(0).getBackendDetail("rowNos"));
      assertEquals("Row 3", records.get(1).getBackendDetail("rowNos"));

      records = rowsToRecord.nextPage(fileToRows, null, mapping, Integer.MAX_VALUE);
      assertEquals(1, records.size());
      assertEquals(List.of("Ned"), getValues(records, "firstName"));
      assertEquals(List.of(2), getValues(records, "noOfShoes"));
      assertEquals(ListBuilder.of(new BigDecimal("1.00")), getValues(records, "cost"));
      assertEquals("Row 4", records.get(0).getBackendDetail("rowNos"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFieldNameToIndexMapping() throws QException
   {
      TestFileToRows fileToRows = new TestFileToRows(List.of(
         new Serializable[] { 1, "Homer", "Simpson", true },
         new Serializable[] { 2, "Marge", "Simpson", false },
         new Serializable[] { 3, "Bart", "Simpson", "A" },
         new Serializable[] { 4, "Ned", "Flanders", 3.1 }
      ));

      BulkLoadFileRow header = null;

      FlatRowsToRecord rowsToRecord = new FlatRowsToRecord();

      BulkInsertMapping mapping = new BulkInsertMapping()
         .withFieldNameToIndexMap(Map.of(
            "firstName", 1,
            "lastName", 2
         ))
         .withFieldNameToDefaultValueMap(Map.of(
            "noOfShoes", 2
         ))
         .withTableName(TestUtils.TABLE_NAME_PERSON)
         .withHasHeaderRow(false);

      List<QRecord> records = rowsToRecord.nextPage(fileToRows, header, mapping, 1);
      assertEquals(List.of("Homer"), getValues(records, "firstName"));
      assertEquals(List.of("Simpson"), getValues(records, "lastName"));
      assertEquals(List.of(2), getValues(records, "noOfShoes"));
      assertEquals(3, records.get(0).getValues().size()); // make sure no additional values were set

      records = rowsToRecord.nextPage(fileToRows, header, mapping, 2);
      assertEquals(List.of("Marge", "Bart"), getValues(records, "firstName"));
      assertEquals(List.of(2, 2), getValues(records, "noOfShoes"));

      records = rowsToRecord.nextPage(fileToRows, header, mapping, Integer.MAX_VALUE);
      assertEquals(List.of("Ned"), getValues(records, "firstName"));
      assertEquals(List.of(2), getValues(records, "noOfShoes"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPossibleValueMappings() throws QException
   {
      TestFileToRows fileToRows = new TestFileToRows(List.of(
         new Serializable[] { "id", "firstName", "Last Name", "Home State" },
         new Serializable[] { 1, "Homer", "Simpson", 1 },
         new Serializable[] { 2, "Marge", "Simpson", "MO" },
         new Serializable[] { 3, "Bart", "Simpson", null },
         new Serializable[] { 4, "Ned", "Flanders", "Not a state" },
         new Serializable[] { 5, "Mr.", "Burns", 5 }
      ));

      BulkLoadFileRow header = fileToRows.next();

      FlatRowsToRecord rowsToRecord = new FlatRowsToRecord();

      BulkInsertMapping mapping = new BulkInsertMapping()
         .withFieldNameToHeaderNameMap(Map.of(
            "firstName", "firstName",
            "lastName", "Last Name",
            "homeStateId", "Home State"
         ))
         .withTableName(TestUtils.TABLE_NAME_PERSON)
         .withHasHeaderRow(true);

      List<QRecord> records = rowsToRecord.nextPage(fileToRows, header, mapping, Integer.MAX_VALUE);
      assertEquals(5, records.size());
      assertEquals(List.of("Homer", "Marge", "Bart", "Ned", "Mr."), getValues(records, "firstName"));
      assertEquals(ListBuilder.of(1, 2, null, null, null), getValues(records, "homeStateId"));

      assertThat(records.get(0).getErrors()).isNullOrEmpty();
      assertThat(records.get(1).getErrors()).isNullOrEmpty();
      assertThat(records.get(2).getErrors()).isNullOrEmpty();
      assertThat(records.get(3).getErrors()).hasSize(1).element(0).matches(e -> e.getMessage().contains("not a valid option"));
      assertThat(records.get(4).getErrors()).hasSize(1).element(0).matches(e -> e.getMessage().contains("not a valid option"));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private List<Serializable> getValues(List<QRecord> records, String fieldName)
   {
      return (records.stream().map(r -> r.getValue(fieldName)).toList());
   }

}