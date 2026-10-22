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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.filehandling.CsvFileToRows;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkInsertMapping;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadFileRow;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for WideRowsToRecordWithExplicitFieldNameSuffixIndexBasedMapping 
 *******************************************************************************/
class WideRowsToRecordWithExplicitFieldNameSuffixIndexBasedMappingTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOrderAndLinesWithoutDupes() throws QException
   {
      String csv = """
         orderNo, Ship To, lastName, SKU 1,     Quantity 1, SKU 2,     Quantity 2, SKU 3, Quantity 3
         1,       Homer,   Simpson,  DONUT,     12,         BEER,      500,        COUCH,  two
         2,       Ned,     Flanders, BIBLE,     7,          LAWNMOWER, 1
         """;

      CsvFileToRows   fileToRows = CsvFileToRows.forString(csv);
      BulkLoadFileRow header     = fileToRows.next();

      WideRowsToRecordWithExplicitFieldNameSuffixIndexBasedMapping rowsToRecord = new WideRowsToRecordWithExplicitFieldNameSuffixIndexBasedMapping();

      BulkInsertMapping mapping = new BulkInsertMapping()
         .withFieldNameToHeaderNameMap(Map.of(
            "orderNo", "orderNo",
            "shipToName", "Ship To",
            "orderLine.sku,0", "SKU 1",
            "orderLine.quantity,0", "Quantity 1",
            "orderLine.sku,1", "SKU 2",
            "orderLine.quantity,1", "Quantity 2",
            "orderLine.sku,2", "SKU 3",
            "orderLine.quantity,2", "Quantity 3"
         ))
         .withMappedAssociations(List.of("orderLine"))
         .withFieldNameToValueMapping(Map.of(
            "orderLine.quantity,2", Map.of("two", 2)
         ))
         .withTableName(TestUtils.TABLE_NAME_ORDER)
         .withLayout(BulkInsertMapping.Layout.WIDE)
         .withHasHeaderRow(true);

      List<QRecord> records = rowsToRecord.nextPage(fileToRows, header, mapping, Integer.MAX_VALUE);
      assertEquals(2, records.size());

      QRecord order = records.get(0);
      assertEquals(1, order.getValueInteger("orderNo"));
      assertEquals("Homer", order.getValueString("shipToName"));
      assertEquals(List.of("DONUT", "BEER", "COUCH"), getValues(order.getAssociatedRecords().get("orderLine"), "sku"));
      assertEquals(List.of(12, 500, 2), getValues(order.getAssociatedRecords().get("orderLine"), "quantity"));
      assertEquals(1, ((List<?>) order.getBackendDetail("fileRows")).size());
      assertEquals("Row 2", order.getAssociatedRecords().get("orderLine").get(0).getBackendDetail("rowNos"));

      order = records.get(1);
      assertEquals(2, order.getValueInteger("orderNo"));
      assertEquals("Ned", order.getValueString("shipToName"));
      assertEquals(List.of("BIBLE", "LAWNMOWER"), getValues(order.getAssociatedRecords().get("orderLine"), "sku"));
      assertEquals(List.of(7, 1), getValues(order.getAssociatedRecords().get("orderLine"), "quantity"));
      assertEquals(1, ((List<?>) order.getBackendDetail("fileRows")).size());
      assertEquals("Row 3", order.getAssociatedRecords().get("orderLine").get(0).getBackendDetail("rowNos"));
   }

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOrderAndLinesWithLineValuesFromDefaults() throws QException
   {
      String csv = """
         orderNo, Ship To, lastName, SKU 1,     Quantity 1
         1,       Homer,   Simpson,  DONUT,     12,
         2,       Ned,     Flanders, 
         """;

      CsvFileToRows   fileToRows = CsvFileToRows.forString(csv);
      BulkLoadFileRow header     = fileToRows.next();

      WideRowsToRecordWithExplicitFieldNameSuffixIndexBasedMapping rowsToRecord = new WideRowsToRecordWithExplicitFieldNameSuffixIndexBasedMapping();

      BulkInsertMapping mapping = new BulkInsertMapping()
         .withFieldNameToHeaderNameMap(Map.of(
            "orderNo", "orderNo",
            "shipToName", "Ship To",
            "orderLine.sku,0", "SKU 1",
            "orderLine.quantity,0", "Quantity 1"
         ))
         .withFieldNameToDefaultValueMap(Map.of(
            "orderLine.sku,1", "NUCLEAR-ROD",
            "orderLine.quantity,1", 1
         ))
         .withMappedAssociations(List.of("orderLine"))
         .withTableName(TestUtils.TABLE_NAME_ORDER)
         .withLayout(BulkInsertMapping.Layout.WIDE)
         .withHasHeaderRow(true);

      List<QRecord> records = rowsToRecord.nextPage(fileToRows, header, mapping, Integer.MAX_VALUE);
      assertEquals(2, records.size());

      QRecord order = records.get(0);
      assertEquals(1, order.getValueInteger("orderNo"));
      assertEquals("Homer", order.getValueString("shipToName"));
      assertEquals(List.of("DONUT", "NUCLEAR-ROD"), getValues(order.getAssociatedRecords().get("orderLine"), "sku"));
      assertEquals(List.of(12, 1), getValues(order.getAssociatedRecords().get("orderLine"), "quantity"));
      assertEquals(1, ((List<?>) order.getBackendDetail("fileRows")).size());
      assertEquals("Row 2", order.getAssociatedRecords().get("orderLine").get(0).getBackendDetail("rowNos"));

      order = records.get(1);
      assertEquals(2, order.getValueInteger("orderNo"));
      assertEquals("Ned", order.getValueString("shipToName"));
      assertEquals(List.of("NUCLEAR-ROD"), getValues(order.getAssociatedRecords().get("orderLine"), "sku"));
      assertEquals(List.of(1), getValues(order.getAssociatedRecords().get("orderLine"), "quantity"));
      assertEquals(1, ((List<?>) order.getBackendDetail("fileRows")).size());
      assertEquals("Row 3", order.getAssociatedRecords().get("orderLine").get(0).getBackendDetail("rowNos"));
   }

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOrderAndLinesWithoutHeader() throws QException
   {
      // 0, 1,     2,        3,     4,  5,         6,   7,     8
      String csv = """
         1, Homer, Simpson,  DONUT, 12, BEER,      500, COUCH, 1
         2, Ned,   Flanders, BIBLE, 7,  LAWNMOWER, 1
         """;

      CsvFileToRows   fileToRows = CsvFileToRows.forString(csv);
      BulkLoadFileRow header     = null;

      WideRowsToRecordWithExplicitFieldNameSuffixIndexBasedMapping rowsToRecord = new WideRowsToRecordWithExplicitFieldNameSuffixIndexBasedMapping();

      BulkInsertMapping mapping = new BulkInsertMapping()
         .withFieldNameToIndexMap(Map.of(
            "orderNo", 0,
            "shipToName", 1,
            "orderLine.sku,0", 3,
            "orderLine.quantity,0", 4,
            "orderLine.sku,1", 5,
            "orderLine.quantity,1", 6,
            "orderLine.sku,2", 7,
            "orderLine.quantity,2", 8
         ))
         .withMappedAssociations(List.of("orderLine"))
         .withTableName(TestUtils.TABLE_NAME_ORDER)
         .withLayout(BulkInsertMapping.Layout.WIDE)
         .withHasHeaderRow(false);

      List<QRecord> records = rowsToRecord.nextPage(fileToRows, header, mapping, Integer.MAX_VALUE);
      assertEquals(2, records.size());

      QRecord order = records.get(0);
      assertEquals(1, order.getValueInteger("orderNo"));
      assertEquals("Homer", order.getValueString("shipToName"));
      assertEquals(List.of("DONUT", "BEER", "COUCH"), getValues(order.getAssociatedRecords().get("orderLine"), "sku"));
      assertEquals(List.of(12, 500, 1), getValues(order.getAssociatedRecords().get("orderLine"), "quantity"));
      assertEquals(1, ((List<?>) order.getBackendDetail("fileRows")).size());
      assertEquals("Row 1", order.getAssociatedRecords().get("orderLine").get(0).getBackendDetail("rowNos"));
      assertEquals("Row 1", order.getAssociatedRecords().get("orderLine").get(0).getBackendDetail("rowNos"));
      assertEquals("Row 1", order.getAssociatedRecords().get("orderLine").get(1).getBackendDetail("rowNos"));
      assertEquals("Row 1", order.getAssociatedRecords().get("orderLine").get(2).getBackendDetail("rowNos"));

      order = records.get(1);
      assertEquals(2, order.getValueInteger("orderNo"));
      assertEquals("Ned", order.getValueString("shipToName"));
      assertEquals(List.of("BIBLE", "LAWNMOWER"), getValues(order.getAssociatedRecords().get("orderLine"), "sku"));
      assertEquals(List.of(7, 1), getValues(order.getAssociatedRecords().get("orderLine"), "quantity"));
      assertEquals(1, ((List<?>) order.getBackendDetail("fileRows")).size());
      assertEquals("Row 2", order.getAssociatedRecords().get("orderLine").get(0).getBackendDetail("rowNos"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOrderLinesAndOrderExtrinsicWithoutDupes() throws QException
   {
      String csv = """
         orderNo, Ship To, lastName, SKU 1, Quantity 1, SKU 2,     Quantity 2, SKU 3, Quantity 3, Extrinsic Key 1, Extrinsic Value 1, Extrinsic Key 2, Extrinsic Value 2
         1,       Homer,   Simpson,  DONUT, 12,         BEER,      500,        COUCH, 1,          Store Name,      QQQ Mart,          Coupon Code,     10QOff
         2,       Ned,     Flanders, BIBLE, 7,          LAWNMOWER, 1
         """;

      CsvFileToRows   fileToRows = CsvFileToRows.forString(csv);
      BulkLoadFileRow header     = fileToRows.next();

      WideRowsToRecordWithExplicitFieldNameSuffixIndexBasedMapping rowsToRecord = new WideRowsToRecordWithExplicitFieldNameSuffixIndexBasedMapping();

      BulkInsertMapping mapping = new BulkInsertMapping()
         .withFieldNameToHeaderNameMap(MapBuilder.of(() -> new HashMap<String, String>())
            .with("orderNo", "orderNo")
            .with("shipToName", "Ship To")

            .with("orderLine.sku,0", "SKU 1")
            .with("orderLine.quantity,0", "Quantity 1")
            .with("orderLine.sku,1", "SKU 2")
            .with("orderLine.quantity,1", "Quantity 2")
            .with("orderLine.sku,2", "SKU 3")
            .with("orderLine.quantity,2", "Quantity 3")

            .with("extrinsics.key,0", "Extrinsic Key 1")
            .with("extrinsics.value,0", "Extrinsic Value 1")
            .with("extrinsics.key,1", "Extrinsic Key 2")
            .with("extrinsics.value,1", "Extrinsic Value 2")
            .build()
         )
         .withFieldNameToDefaultValueMap(Map.of(
            "orderLine.lineNumber,0", "1",
            "orderLine.lineNumber,1", "2",
            "orderLine.lineNumber,2", "3"
         ))
         .withMappedAssociations(List.of("orderLine", "extrinsics"))
         .withTableName(TestUtils.TABLE_NAME_ORDER)
         .withLayout(BulkInsertMapping.Layout.WIDE)
         .withHasHeaderRow(true);

      List<QRecord> records = rowsToRecord.nextPage(fileToRows, header, mapping, Integer.MAX_VALUE);
      assertEquals(2, records.size());

      QRecord order = records.get(0);
      assertEquals(1, order.getValueInteger("orderNo"));
      assertEquals("Homer", order.getValueString("shipToName"));
      assertEquals(List.of("DONUT", "BEER", "COUCH"), getValues(order.getAssociatedRecords().get("orderLine"), "sku"));
      assertEquals(List.of(12, 500, 1), getValues(order.getAssociatedRecords().get("orderLine"), "quantity"));
      assertEquals(List.of("1", "2", "3"), getValues(order.getAssociatedRecords().get("orderLine"), "lineNumber"));
      assertEquals(List.of("Store Name", "Coupon Code"), getValues(order.getAssociatedRecords().get("extrinsics"), "key"));
      assertEquals(List.of("QQQ Mart", "10QOff"), getValues(order.getAssociatedRecords().get("extrinsics"), "value"));

      order = records.get(1);
      assertEquals(2, order.getValueInteger("orderNo"));
      assertEquals("Ned", order.getValueString("shipToName"));
      assertEquals(List.of("BIBLE", "LAWNMOWER"), getValues(order.getAssociatedRecords().get("orderLine"), "sku"));
      assertEquals(List.of(7, 1), getValues(order.getAssociatedRecords().get("orderLine"), "quantity"));
      assertEquals(List.of("1", "2"), getValues(order.getAssociatedRecords().get("orderLine"), "lineNumber"));
      assertThat(order.getAssociatedRecords().get("extrinsics")).isNullOrEmpty();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private List<Serializable> getValues(List<QRecord> records, String fieldName)
   {
      return (records.stream().map(r -> r.getValue(fieldName)).toList());
   }

}