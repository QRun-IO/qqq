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
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.filehandling.CsvFileToRows;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkInsertMapping;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadFileRow;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for TallRowsToRecord 
 *******************************************************************************/
class TallRowsToRecordTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOrderAndLines() throws QException
   {
      CsvFileToRows fileToRows = CsvFileToRows.forString("""
         orderNo, Ship To, lastName, SKU,       Quantity
         1,       Homer,   Simpson,  DONUT,     12
         ,        Homer,   Simpson,  BEER,      500
         ,        Homer,   Simpson,  COUCH,     1
         2,       Ned,     Flanders, BIBLE,     7
         ,        Ned,     Flanders, LAWNMOWER, 1
         """);

      BulkLoadFileRow header = fileToRows.next();

      TallRowsToRecord rowsToRecord = new TallRowsToRecord();

      BulkInsertMapping mapping = new BulkInsertMapping()
         .withFieldNameToHeaderNameMap(Map.of(
            "orderNo", "orderNo",
            "shipToName", "Ship To",
            "orderLine.sku", "SKU",
            "orderLine.quantity", "Quantity"
         ))
         .withTallLayoutGroupByIndexMap(Map.of(
            TestUtils.TABLE_NAME_ORDER, List.of(1, 2),
            "orderLine", List.of(3)
         ))
         .withMappedAssociations(List.of("orderLine"))
         .withTableName(TestUtils.TABLE_NAME_ORDER)
         .withLayout(BulkInsertMapping.Layout.TALL)
         .withHasHeaderRow(true);

      List<QRecord> records = rowsToRecord.nextPage(fileToRows, header, mapping, Integer.MAX_VALUE);
      assertEquals(2, records.size());

      QRecord order = records.get(0);
      assertEquals(1, order.getValueInteger("orderNo"));
      assertEquals("Homer", order.getValueString("shipToName"));
      assertEquals(3, order.getAssociatedRecords().get("orderLine").size());
      assertEquals(List.of("DONUT", "BEER", "COUCH"), getValues(order.getAssociatedRecords().get("orderLine"), "sku"));
      assertEquals(List.of(12, 500, 1), getValues(order.getAssociatedRecords().get("orderLine"), "quantity"));
      assertEquals("Rows 2-4", order.getBackendDetail("rowNos"));
      assertEquals(3, ((List<?>) order.getBackendDetail("fileRows")).size());
      assertEquals("Row 2", order.getAssociatedRecords().get("orderLine").get(0).getBackendDetail("rowNos"));
      assertEquals("Row 3", order.getAssociatedRecords().get("orderLine").get(1).getBackendDetail("rowNos"));
      assertEquals("Row 4", order.getAssociatedRecords().get("orderLine").get(2).getBackendDetail("rowNos"));

      order = records.get(1);
      assertEquals(2, order.getValueInteger("orderNo"));
      assertEquals("Ned", order.getValueString("shipToName"));
      assertEquals(2, order.getAssociatedRecords().get("orderLine").size());
      assertEquals(List.of("BIBLE", "LAWNMOWER"), getValues(order.getAssociatedRecords().get("orderLine"), "sku"));
      assertEquals(List.of(7, 1), getValues(order.getAssociatedRecords().get("orderLine"), "quantity"));
      assertEquals("Rows 5-6", order.getBackendDetail("rowNos"));
      assertEquals(2, ((List<?>) order.getBackendDetail("fileRows")).size());
   }



   /*******************************************************************************
    ** test to show that we can do 1 default line item (child record) for each
    ** header record.
    *******************************************************************************/
   @Test
   void testOrderAndLinesWithLineValuesFromDefaults() throws QException
   {
      CsvFileToRows fileToRows = CsvFileToRows.forString("""
         orderNo, Ship To, lastName
         1,       Homer,   Simpson
         2,       Ned,     Flanders
         """);

      BulkLoadFileRow header = fileToRows.next();

      TallRowsToRecord rowsToRecord = new TallRowsToRecord();

      BulkInsertMapping mapping = new BulkInsertMapping()
         .withFieldNameToHeaderNameMap(Map.of(
            "orderNo", "orderNo",
            "shipToName", "Ship To"
         ))
         .withFieldNameToDefaultValueMap(Map.of(
            "orderLine.sku", "NUCLEAR-ROD",
            "orderLine.quantity", 1
         ))
         .withMappedAssociations(List.of("orderLine"))
         .withTableName(TestUtils.TABLE_NAME_ORDER)
         .withLayout(BulkInsertMapping.Layout.TALL)
         .withHasHeaderRow(true);

      List<QRecord> records = rowsToRecord.nextPage(fileToRows, header, mapping, Integer.MAX_VALUE);
      assertEquals(2, records.size());

      QRecord order = records.get(0);
      assertEquals(1, order.getValueInteger("orderNo"));
      assertEquals("Homer", order.getValueString("shipToName"));
      assertEquals(1, order.getAssociatedRecords().get("orderLine").size());
      assertEquals(List.of("NUCLEAR-ROD"), getValues(order.getAssociatedRecords().get("orderLine"), "sku"));
      assertEquals(List.of(1), getValues(order.getAssociatedRecords().get("orderLine"), "quantity"));
      assertEquals("Row 2", order.getBackendDetail("rowNos"));
      assertEquals(1, ((List<?>) order.getBackendDetail("fileRows")).size());

      order = records.get(1);
      assertEquals(2, order.getValueInteger("orderNo"));
      assertEquals("Ned", order.getValueString("shipToName"));
      assertEquals(1, order.getAssociatedRecords().get("orderLine").size());
      assertEquals(List.of("NUCLEAR-ROD"), getValues(order.getAssociatedRecords().get("orderLine"), "sku"));
      assertEquals(List.of(1), getValues(order.getAssociatedRecords().get("orderLine"), "quantity"));
      assertEquals("Row 3", order.getBackendDetail("rowNos"));
      assertEquals(1, ((List<?>) order.getBackendDetail("fileRows")).size());
   }

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOrderAndLinesWithoutHeader() throws QException
   {
      // 0, 1,     2,        3,         4
      CsvFileToRows fileToRows = CsvFileToRows.forString("""
         1, Homer, Simpson,  DONUT,     12
         ,  Homer, Simpson,  BEER,      500
         ,  Homer, Simpson,  COUCH,     1
         2, Ned,   Flanders, BIBLE,     7
         ,  Ned,   Flanders, LAWNMOWER, 1
         """);

      BulkLoadFileRow header = null;

      TallRowsToRecord rowsToRecord = new TallRowsToRecord();

      BulkInsertMapping mapping = new BulkInsertMapping()
         .withFieldNameToIndexMap(Map.of(
            "orderNo", 0,
            "shipToName", 1,
            "orderLine.sku", 3,
            "orderLine.quantity", 4
         ))
         .withTallLayoutGroupByIndexMap(Map.of(
            TestUtils.TABLE_NAME_ORDER, List.of(1, 2),
            "orderLine", List.of(3)
         ))
         .withMappedAssociations(List.of("orderLine"))
         .withTableName(TestUtils.TABLE_NAME_ORDER)
         .withLayout(BulkInsertMapping.Layout.TALL)
         .withHasHeaderRow(false);

      List<QRecord> records = rowsToRecord.nextPage(fileToRows, header, mapping, Integer.MAX_VALUE);
      assertEquals(2, records.size());

      QRecord order = records.get(0);
      assertEquals(1, order.getValueInteger("orderNo"));
      assertEquals("Homer", order.getValueString("shipToName"));
      assertEquals(3, order.getAssociatedRecords().get("orderLine").size());
      assertEquals(List.of("DONUT", "BEER", "COUCH"), getValues(order.getAssociatedRecords().get("orderLine"), "sku"));
      assertEquals(List.of(12, 500, 1), getValues(order.getAssociatedRecords().get("orderLine"), "quantity"));
      assertEquals("Rows 1-3", order.getBackendDetail("rowNos"));
      assertEquals(3, ((List<?>) order.getBackendDetail("fileRows")).size());
      assertEquals("Row 1", order.getAssociatedRecords().get("orderLine").get(0).getBackendDetail("rowNos"));
      assertEquals("Row 2", order.getAssociatedRecords().get("orderLine").get(1).getBackendDetail("rowNos"));
      assertEquals("Row 3", order.getAssociatedRecords().get("orderLine").get(2).getBackendDetail("rowNos"));

      order = records.get(1);
      assertEquals(2, order.getValueInteger("orderNo"));
      assertEquals("Ned", order.getValueString("shipToName"));
      assertEquals(2, order.getAssociatedRecords().get("orderLine").size());
      assertEquals(List.of("BIBLE", "LAWNMOWER"), getValues(order.getAssociatedRecords().get("orderLine"), "sku"));
      assertEquals(List.of(7, 1), getValues(order.getAssociatedRecords().get("orderLine"), "quantity"));
      assertEquals("Rows 4-5", order.getBackendDetail("rowNos"));
      assertEquals(2, ((List<?>) order.getBackendDetail("fileRows")).size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOrderLinesAndOrderExtrinsic() throws QException
   {
      CsvFileToRows fileToRows = CsvFileToRows.forString("""
         orderNo, Ship To, lastName, SKU,       Quantity, Extrinsic Key, Extrinsic Value
         1,       Homer,   Simpson,  DONUT,     12,       Store Name,    QQQ Mart
         1,       ,        ,         BEER,      500,      Coupon Code,   10QOff
         1,       ,        ,         COUCH,     1
         2,       Ned,     Flanders, BIBLE,     7
         2,       ,        ,         LAWNMOWER, 1
         """);

      BulkLoadFileRow header = fileToRows.next();

      TallRowsToRecord rowsToRecord = new TallRowsToRecord();

      BulkInsertMapping mapping = new BulkInsertMapping()
         .withFieldNameToHeaderNameMap(Map.of(
            "orderNo", "orderNo",
            "shipToName", "Ship To",
            "orderLine.sku", "SKU",
            "orderLine.quantity", "Quantity",
            "extrinsics.key", "Extrinsic Key",
            "extrinsics.value", "Extrinsic Value"
         ))
         .withTallLayoutGroupByIndexMap(Map.of(
            TestUtils.TABLE_NAME_ORDER, List.of(0),
            "orderLine", List.of(3),
            "extrinsics", List.of(5)
         ))
         .withMappedAssociations(List.of("orderLine", "extrinsics"))
         .withTableName(TestUtils.TABLE_NAME_ORDER)
         .withLayout(BulkInsertMapping.Layout.TALL)
         .withHasHeaderRow(true);

      List<QRecord> records = rowsToRecord.nextPage(fileToRows, header, mapping, Integer.MAX_VALUE);
      assertEquals(2, records.size());

      QRecord order = records.get(0);
      assertEquals(1, order.getValueInteger("orderNo"));
      assertEquals("Homer", order.getValueString("shipToName"));
      assertEquals(3, order.getAssociatedRecords().get("orderLine").size());
      assertEquals(List.of("DONUT", "BEER", "COUCH"), getValues(order.getAssociatedRecords().get("orderLine"), "sku"));
      assertEquals(List.of(12, 500, 1), getValues(order.getAssociatedRecords().get("orderLine"), "quantity"));
      assertEquals(2, order.getAssociatedRecords().get("extrinsics").size());
      assertEquals(List.of("Store Name", "Coupon Code"), getValues(order.getAssociatedRecords().get("extrinsics"), "key"));
      assertEquals(List.of("QQQ Mart", "10QOff"), getValues(order.getAssociatedRecords().get("extrinsics"), "value"));

      order = records.get(1);
      assertEquals(2, order.getValueInteger("orderNo"));
      assertEquals("Ned", order.getValueString("shipToName"));
      assertEquals(2, order.getAssociatedRecords().get("orderLine").size());
      assertEquals(List.of("BIBLE", "LAWNMOWER"), getValues(order.getAssociatedRecords().get("orderLine"), "sku"));
      assertEquals(List.of(7, 1), getValues(order.getAssociatedRecords().get("orderLine"), "quantity"));
      assertThat(order.getAssociatedRecords().get("extrinsics")).isNullOrEmpty();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOrderLinesWithLineExtrinsicsAndOrderExtrinsic() throws QException
   {
      Integer defaultStoreId              = 101;
      String  defaultLineNo               = "102";
      String  defaultOrderLineExtraSource = "file";

      CsvFileToRows fileToRows = CsvFileToRows.forString("""
         orderNo, Ship To, lastName, SKU,       Quantity, Extrinsic Key, Extrinsic Value, Line Extrinsic Key, Line Extrinsic Value
         1,       Homer,   Simpson,  DONUT,     12,       Store Name,    QQQ Mart,        Flavor,             Chocolate
         1,       ,        ,         DONUT,     ,         Coupon Code,   10QOff,          Size,               Large
         1,       ,        ,         BEER,      500,      ,              ,                Flavor,             Hops
         1,       ,        ,         COUCH,     1
         2,       Ned,     Flanders, BIBLE,     7,        ,              ,                Flavor,             King James
         2,       ,        ,         LAWNMOWER, 1
         """);

      BulkLoadFileRow header = fileToRows.next();

      TallRowsToRecord rowsToRecord = new TallRowsToRecord();

      BulkInsertMapping mapping = new BulkInsertMapping()
         .withFieldNameToHeaderNameMap(Map.of(
            "orderNo", "orderNo",
            "shipToName", "Ship To",
            "orderLine.sku", "SKU",
            "orderLine.quantity", "Quantity",
            "extrinsics.key", "Extrinsic Key",
            "extrinsics.value", "Extrinsic Value",
            "orderLine.extrinsics.key", "Line Extrinsic Key",
            "orderLine.extrinsics.value", "Line Extrinsic Value"
         ))
         .withFieldNameToDefaultValueMap(Map.of(
            "storeId", defaultStoreId,
            "orderLine.lineNumber", defaultLineNo,
            "orderLine.extrinsics.source", defaultOrderLineExtraSource
         ))
         .withFieldNameToValueMapping(Map.of("orderLine.sku", Map.of("DONUT", "D'OH-NUT")))
         .withTallLayoutGroupByIndexMap(Map.of(
            TestUtils.TABLE_NAME_ORDER, List.of(0),
            "orderLine", List.of(3),
            "extrinsics", List.of(5),
            "orderLine.extrinsics", List.of(7)
         ))
         .withMappedAssociations(List.of("orderLine", "extrinsics", "orderLine.extrinsics"))
         .withTableName(TestUtils.TABLE_NAME_ORDER)
         .withLayout(BulkInsertMapping.Layout.TALL)
         .withHasHeaderRow(true);

      List<QRecord> records = rowsToRecord.nextPage(fileToRows, header, mapping, Integer.MAX_VALUE);
      assertEquals(2, records.size());

      QRecord order = records.get(0);
      assertEquals(1, order.getValueInteger("orderNo"));
      assertEquals("Homer", order.getValueString("shipToName"));
      assertEquals(defaultStoreId, order.getValue("storeId"));
      assertEquals(List.of("D'OH-NUT", "BEER", "COUCH"), getValues(order.getAssociatedRecords().get("orderLine"), "sku"));
      assertEquals(List.of(12, 500, 1), getValues(order.getAssociatedRecords().get("orderLine"), "quantity"));
      assertEquals(List.of(defaultLineNo, defaultLineNo, defaultLineNo), getValues(order.getAssociatedRecords().get("orderLine"), "lineNumber"));
      assertEquals(List.of("Store Name", "Coupon Code"), getValues(order.getAssociatedRecords().get("extrinsics"), "key"));
      assertEquals(List.of("QQQ Mart", "10QOff"), getValues(order.getAssociatedRecords().get("extrinsics"), "value"));

      QRecord lineItem = order.getAssociatedRecords().get("orderLine").get(0);
      assertEquals(List.of("Flavor", "Size"), getValues(lineItem.getAssociatedRecords().get("extrinsics"), "key"));
      assertEquals(List.of("Chocolate", "Large"), getValues(lineItem.getAssociatedRecords().get("extrinsics"), "value"));
      assertEquals(List.of(defaultOrderLineExtraSource, defaultOrderLineExtraSource), getValues(lineItem.getAssociatedRecords().get("extrinsics"), "source"));

      lineItem = order.getAssociatedRecords().get("orderLine").get(1);
      assertEquals(List.of("Flavor"), getValues(lineItem.getAssociatedRecords().get("extrinsics"), "key"));
      assertEquals(List.of("Hops"), getValues(lineItem.getAssociatedRecords().get("extrinsics"), "value"));

      order = records.get(1);
      assertEquals(2, order.getValueInteger("orderNo"));
      assertEquals("Ned", order.getValueString("shipToName"));
      assertEquals(defaultStoreId, order.getValue("storeId"));
      assertEquals(List.of("BIBLE", "LAWNMOWER"), getValues(order.getAssociatedRecords().get("orderLine"), "sku"));
      assertEquals(List.of(7, 1), getValues(order.getAssociatedRecords().get("orderLine"), "quantity"));
      assertEquals(List.of(defaultLineNo, defaultLineNo), getValues(order.getAssociatedRecords().get("orderLine"), "lineNumber"));
      assertThat(order.getAssociatedRecords().get("extrinsics")).isNullOrEmpty();

      lineItem = order.getAssociatedRecords().get("orderLine").get(0);
      assertEquals(List.of("Flavor"), getValues(lineItem.getAssociatedRecords().get("extrinsics"), "key"));
      assertEquals(List.of("King James"), getValues(lineItem.getAssociatedRecords().get("extrinsics"), "value"));
      assertEquals(List.of(defaultOrderLineExtraSource), getValues(lineItem.getAssociatedRecords().get("extrinsics"), "source"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAutomaticGroupByAllIndexes() throws QException
   {
      Integer defaultStoreId              = 101;
      String  defaultLineNo               = "102";
      String  defaultOrderLineExtraSource = "file";

      CsvFileToRows fileToRows = CsvFileToRows.forString("""
         orderNo, Ship To, lastName, SKU,       Quantity, Extrinsic Key, Extrinsic Value, Line Extrinsic Key, Line Extrinsic Value
         1,       Homer,   Simpson,  DONUT,     12,       Store Name,    QQQ Mart,        Flavor,             Chocolate
         1,       Homer,   Simpson,  DONUT,     12,       Coupon Code,   10QOff,          Size,               Large
         1,       Homer,   Simpson,  BEER,      500,      ,              ,                Flavor,             Hops
         1,       Homer,   Simpson,  COUCH,     1
         2,       Ned,     Flanders, BIBLE,     7,        ,              ,                Flavor,             King James
         2,       Ned,     Flanders, LAWNMOWER, 1
         """);

      BulkLoadFileRow header = fileToRows.next();

      TallRowsToRecord rowsToRecord = new TallRowsToRecord();

      BulkInsertMapping mapping = new BulkInsertMapping()
         .withFieldNameToHeaderNameMap(Map.of(
            "orderNo", "orderNo",
            "shipToName", "Ship To",
            "orderLine.sku", "SKU",
            "orderLine.quantity", "Quantity",
            "extrinsics.key", "Extrinsic Key",
            "extrinsics.value", "Extrinsic Value",
            "orderLine.extrinsics.key", "Line Extrinsic Key",
            "orderLine.extrinsics.value", "Line Extrinsic Value"
         ))
         .withFieldNameToDefaultValueMap(Map.of(
            "storeId", defaultStoreId,
            "orderLine.lineNumber", defaultLineNo,
            "orderLine.extrinsics.source", defaultOrderLineExtraSource
         ))
         .withFieldNameToValueMapping(Map.of("orderLine.sku", Map.of("DONUT", "D'OH-NUT")))
         .withMappedAssociations(List.of("orderLine", "extrinsics", "orderLine.extrinsics"))
         .withTableName(TestUtils.TABLE_NAME_ORDER)
         .withLayout(BulkInsertMapping.Layout.TALL)
         .withHasHeaderRow(true);

      List<QRecord> records = rowsToRecord.nextPage(fileToRows, header, mapping, Integer.MAX_VALUE);
      assertEquals(2, records.size());

      QRecord order = records.get(0);
      assertEquals(1, order.getValueInteger("orderNo"));
      assertEquals("Homer", order.getValueString("shipToName"));
      assertEquals(defaultStoreId, order.getValue("storeId"));
      assertEquals(List.of("D'OH-NUT", "BEER", "COUCH"), getValues(order.getAssociatedRecords().get("orderLine"), "sku"));
      assertEquals(List.of(12, 500, 1), getValues(order.getAssociatedRecords().get("orderLine"), "quantity"));
      assertEquals(List.of(defaultLineNo, defaultLineNo, defaultLineNo), getValues(order.getAssociatedRecords().get("orderLine"), "lineNumber"));
      assertEquals(List.of("Store Name", "Coupon Code"), getValues(order.getAssociatedRecords().get("extrinsics"), "key"));
      assertEquals(List.of("QQQ Mart", "10QOff"), getValues(order.getAssociatedRecords().get("extrinsics"), "value"));

      QRecord lineItem = order.getAssociatedRecords().get("orderLine").get(0);
      assertEquals(List.of("Flavor", "Size"), getValues(lineItem.getAssociatedRecords().get("extrinsics"), "key"));
      assertEquals(List.of("Chocolate", "Large"), getValues(lineItem.getAssociatedRecords().get("extrinsics"), "value"));
      assertEquals(List.of(defaultOrderLineExtraSource, defaultOrderLineExtraSource), getValues(lineItem.getAssociatedRecords().get("extrinsics"), "source"));

      lineItem = order.getAssociatedRecords().get("orderLine").get(1);
      assertEquals(List.of("Flavor"), getValues(lineItem.getAssociatedRecords().get("extrinsics"), "key"));
      assertEquals(List.of("Hops"), getValues(lineItem.getAssociatedRecords().get("extrinsics"), "value"));

      order = records.get(1);
      assertEquals(2, order.getValueInteger("orderNo"));
      assertEquals("Ned", order.getValueString("shipToName"));
      assertEquals(defaultStoreId, order.getValue("storeId"));
      assertEquals(List.of("BIBLE", "LAWNMOWER"), getValues(order.getAssociatedRecords().get("orderLine"), "sku"));
      assertEquals(List.of(7, 1), getValues(order.getAssociatedRecords().get("orderLine"), "quantity"));
      assertEquals(List.of(defaultLineNo, defaultLineNo), getValues(order.getAssociatedRecords().get("orderLine"), "lineNumber"));
      assertThat(order.getAssociatedRecords().get("extrinsics")).isNullOrEmpty();

      lineItem = order.getAssociatedRecords().get("orderLine").get(0);
      assertEquals(List.of("Flavor"), getValues(lineItem.getAssociatedRecords().get("extrinsics"), "key"));
      assertEquals(List.of("King James"), getValues(lineItem.getAssociatedRecords().get("extrinsics"), "value"));
      assertEquals(List.of(defaultOrderLineExtraSource), getValues(lineItem.getAssociatedRecords().get("extrinsics"), "source"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSingleLine() throws QException
   {
      Integer defaultStoreId = 101;

      CsvFileToRows fileToRows = CsvFileToRows.forString("""
         orderNo, Ship To, lastName
         1,       Homer,   Simpson
         """);

      BulkLoadFileRow header = fileToRows.next();

      TallRowsToRecord rowsToRecord = new TallRowsToRecord();

      BulkInsertMapping mapping = new BulkInsertMapping()
         .withFieldNameToHeaderNameMap(Map.of(
            "orderNo", "orderNo",
            "shipToName", "Ship To"
         ))
         .withFieldNameToDefaultValueMap(Map.of(
            "storeId", defaultStoreId
         ))
         .withTableName(TestUtils.TABLE_NAME_ORDER)
         .withLayout(BulkInsertMapping.Layout.TALL)
         .withHasHeaderRow(true);

      List<QRecord> records = rowsToRecord.nextPage(fileToRows, header, mapping, Integer.MAX_VALUE);
      assertEquals(1, records.size());

      QRecord order = records.get(0);
      assertEquals(1, order.getValueInteger("orderNo"));
      assertEquals("Homer", order.getValueString("shipToName"));
      assertEquals(defaultStoreId, order.getValue("storeId"));
      assertEquals("Row 2", order.getBackendDetail("rowNos"));
      assertEquals(1, ((List<?>) order.getBackendDetail("fileRows")).size());
   }




   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPagination() throws QException
   {
      CsvFileToRows fileToRows = CsvFileToRows.forString("""
         orderNo, Ship To, lastName, SKU,       Quantity
         1,       Homer,   Simpson,  DONUT,     12
         2,       Ned,     Flanders, BIBLE,     7
         2,       Ned,     Flanders, LAWNMOWER, 1
         3,       Bart,    Simpson,  SKATEBOARD,1
         3,       Bart,    Simpson,  SLINGSHOT, 1
         """);

      BulkLoadFileRow header = fileToRows.next();

      TallRowsToRecord rowsToRecord = new TallRowsToRecord();

      BulkInsertMapping mapping = new BulkInsertMapping()
         .withFieldNameToHeaderNameMap(Map.of(
            "orderNo", "orderNo",
            "shipToName", "Ship To",
            "orderLine.sku", "SKU",
            "orderLine.quantity", "Quantity"
         ))
         .withTallLayoutGroupByIndexMap(Map.of(
            TestUtils.TABLE_NAME_ORDER, List.of(1, 2),
            "orderLine", List.of(3)
         ))
         .withMappedAssociations(List.of("orderLine"))
         .withTableName(TestUtils.TABLE_NAME_ORDER)
         .withLayout(BulkInsertMapping.Layout.TALL)
         .withHasHeaderRow(true);

      List<QRecord> records = rowsToRecord.nextPage(fileToRows, header, mapping, 2);
      assertEquals(2, records.size());

      QRecord order = records.get(0);
      assertEquals(1, order.getValueInteger("orderNo"));
      assertEquals("Homer", order.getValueString("shipToName"));
      assertEquals(List.of("DONUT"), getValues(order.getAssociatedRecords().get("orderLine"), "sku"));
      assertEquals("Row 2", order.getBackendDetail("rowNos"));
      assertEquals(1, ((List<?>) order.getBackendDetail("fileRows")).size());

      order = records.get(1);
      assertEquals(2, order.getValueInteger("orderNo"));
      assertEquals("Ned", order.getValueString("shipToName"));
      assertEquals(List.of("BIBLE", "LAWNMOWER"), getValues(order.getAssociatedRecords().get("orderLine"), "sku"));
      assertEquals("Rows 3-4", order.getBackendDetail("rowNos"));
      assertEquals(2, ((List<?>) order.getBackendDetail("fileRows")).size());

      records = rowsToRecord.nextPage(fileToRows, header, mapping, 2);
      assertEquals(1, records.size());
      order = records.get(0);
      assertEquals(3, order.getValueInteger("orderNo"));
      assertEquals("Bart", order.getValueString("shipToName"));
      assertEquals(List.of("SKATEBOARD", "SLINGSHOT"), getValues(order.getAssociatedRecords().get("orderLine"), "sku"));
      assertEquals("Rows 5-6", order.getBackendDetail("rowNos"));
      assertEquals(2, ((List<?>) order.getBackendDetail("fileRows")).size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testShouldProcessAssociation()
   {
      TallRowsToRecord tallRowsToRecord = new TallRowsToRecord();
      assertTrue(tallRowsToRecord.shouldProcessAssociation(null, "foo"));
      assertTrue(tallRowsToRecord.shouldProcessAssociation("", "foo"));
      assertTrue(tallRowsToRecord.shouldProcessAssociation("foo", "foo.bar"));
      assertTrue(tallRowsToRecord.shouldProcessAssociation("foo.bar", "foo.bar.baz"));

      assertFalse(tallRowsToRecord.shouldProcessAssociation(null, "foo.bar"));
      assertFalse(tallRowsToRecord.shouldProcessAssociation("", "foo.bar"));
      assertFalse(tallRowsToRecord.shouldProcessAssociation("fiz", "foo.bar"));
      assertFalse(tallRowsToRecord.shouldProcessAssociation("fiz.biz", "foo.bar"));
      assertFalse(tallRowsToRecord.shouldProcessAssociation("foo", "foo.bar.baz"));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private List<Serializable> getValues(List<QRecord> records, String fieldName)
   {
      return (records.stream().map(r -> r.getValue(fieldName)).toList());
   }

}