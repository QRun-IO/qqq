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


/*******************************************************************************
 ** Unit test for WideRowsToRecord
 *******************************************************************************/
class WideRowsToRecordWithSpreadMappingTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOrderAndLinesWithoutDupes() throws QException
   {
      testOrderAndLines("""
         orderNo, Ship To, lastName, SKU 1,     Quantity 1, SKU 2,     Quantity 2, SKU 3, Quantity 3
         1,       Homer,   Simpson,  DONUT,     12,         BEER,      500,        COUCH,  1
         2,       Ned,     Flanders, BIBLE,     7,          LAWNMOWER, 1
         """);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOrderAndLinesWithDupes() throws QException
   {
      testOrderAndLines("""
         orderNo, Ship To, lastName, SKU,   Quantity, SKU,       Quantity, SKU,   Quantity
         1,       Homer,   Simpson,  DONUT, 12,       BEER,      500,      COUCH, 1
         2,       Ned,     Flanders, BIBLE, 7,        LAWNMOWER, 1
         """);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void testOrderAndLines(String csv) throws QException
   {
      CsvFileToRows   fileToRows = CsvFileToRows.forString(csv);
      BulkLoadFileRow header     = fileToRows.next();

      WideRowsToRecordWithSpreadMapping rowsToRecord = new WideRowsToRecordWithSpreadMapping();

      BulkInsertMapping mapping = new BulkInsertMapping()
         .withFieldNameToHeaderNameMap(Map.of(
            "orderNo", "orderNo",
            "shipToName", "Ship To",
            "orderLine.sku", "SKU",
            "orderLine.quantity", "Quantity"
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
      assertEquals(List.of("DONUT", "BEER", "COUCH"), getValues(order.getAssociatedRecords().get("orderLine"), "sku"));
      assertEquals(List.of(12, 500, 1), getValues(order.getAssociatedRecords().get("orderLine"), "quantity"));

      order = records.get(1);
      assertEquals(2, order.getValueInteger("orderNo"));
      assertEquals("Ned", order.getValueString("shipToName"));
      assertEquals(List.of("BIBLE", "LAWNMOWER"), getValues(order.getAssociatedRecords().get("orderLine"), "sku"));
      assertEquals(List.of(7, 1), getValues(order.getAssociatedRecords().get("orderLine"), "quantity"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOrderLinesAndOrderExtrinsicWithoutDupes() throws QException
   {
      testOrderLinesAndOrderExtrinsic("""
         orderNo, Ship To, lastName, SKU 1, Quantity 1, SKU 2,     Quantity 2, SKU 3, Quantity 3, Extrinsic Key 1, Extrinsic Value 1, Extrinsic Key 2, Extrinsic Value 2
         1,       Homer,   Simpson,  DONUT, 12,         BEER,      500,        COUCH, 1,          Store Name,      QQQ Mart,          Coupon Code,     10QOff
         2,       Ned,     Flanders, BIBLE, 7,          LAWNMOWER, 1
         """);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOrderLinesAndOrderExtrinsicWithDupes() throws QException
   {
      testOrderLinesAndOrderExtrinsic("""
         orderNo, Ship To, lastName, SKU,   Quantity, SKU,       Quantity, SKU,   Quantity, Extrinsic Key, Extrinsic Value, Extrinsic Key, Extrinsic Value
         1,       Homer,   Simpson,  DONUT, 12,       BEER,      500,      COUCH, 1,        Store Name,    QQQ Mart,        Coupon Code,   10QOff
         2,       Ned,     Flanders, BIBLE, 7,        LAWNMOWER, 1
         """);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void testOrderLinesAndOrderExtrinsic(String csv) throws QException
   {
      CsvFileToRows   fileToRows = CsvFileToRows.forString(csv);
      BulkLoadFileRow header     = fileToRows.next();

      WideRowsToRecordWithSpreadMapping rowsToRecord = new WideRowsToRecordWithSpreadMapping();

      BulkInsertMapping mapping = new BulkInsertMapping()
         .withFieldNameToHeaderNameMap(Map.of(
            "orderNo", "orderNo",
            "shipToName", "Ship To",
            "orderLine.sku", "SKU",
            "orderLine.quantity", "Quantity",
            "extrinsics.key", "Extrinsic Key",
            "extrinsics.value", "Extrinsic Value"
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
      assertEquals(List.of("Store Name", "Coupon Code"), getValues(order.getAssociatedRecords().get("extrinsics"), "key"));
      assertEquals(List.of("QQQ Mart", "10QOff"), getValues(order.getAssociatedRecords().get("extrinsics"), "value"));

      order = records.get(1);
      assertEquals(2, order.getValueInteger("orderNo"));
      assertEquals("Ned", order.getValueString("shipToName"));
      assertEquals(List.of("BIBLE", "LAWNMOWER"), getValues(order.getAssociatedRecords().get("orderLine"), "sku"));
      assertEquals(List.of(7, 1), getValues(order.getAssociatedRecords().get("orderLine"), "quantity"));
      assertThat(order.getAssociatedRecords().get("extrinsics")).isNullOrEmpty();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOrderLinesWithLineExtrinsicsAndOrderExtrinsicWithoutDupes() throws QException
   {
      testOrderLinesWithLineExtrinsicsAndOrderExtrinsic("""
         orderNo, Ship To, lastName,  Extrinsic Key 1, Extrinsic Value 1, Extrinsic Key 2, Extrinsic Value 2, SKU 1, Quantity 1, Line Extrinsic Key 1, Line Extrinsic Value 1, Line Extrinsic Key 2, Line Extrinsic Value 2, SKU 2,     Quantity 2, Line Extrinsic Key 1, Line Extrinsic Value 1, SKU 3, Quantity 3, Line Extrinsic Key 1, Line Extrinsic Value 1, Line Extrinsic Key 2
         1,       Homer,   Simpson,   Store Name,      QQQ Mart,          Coupon Code,     10QOff,            DONUT, 12,         Flavor,               Chocolate,              Size,                 Large,                  BEER,      500,        Flavor,               Hops,                   COUCH, 1,          Color,                Brown,                  foo,
         2,       Ned,     Flanders,  ,                ,                  ,                ,                  BIBLE, 7,          Flavor,               King James,             Size,                 X-Large,                LAWNMOWER, 1
         """);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOrderLinesWithLineExtrinsicsAndOrderExtrinsicWithDupes() throws QException
   {
      testOrderLinesWithLineExtrinsicsAndOrderExtrinsic("""
         orderNo, Ship To, lastName,  Extrinsic Key, Extrinsic Value, Extrinsic Key, Extrinsic Value, SKU,   Quantity, Line Extrinsic Key, Line Extrinsic Value, Line Extrinsic Key, Line Extrinsic Value, SKU,       Quantity, Line Extrinsic Key, Line Extrinsic Value, SKU,   Quantity, Line Extrinsic Key, Line Extrinsic Value, Line Extrinsic Key
         1,       Homer,   Simpson,   Store Name,    QQQ Mart,        Coupon Code,   10QOff,          DONUT, 12,       Flavor,             Chocolate,            Size,               Large,                BEER,      500,      Flavor,             Hops,                 COUCH, 1,        Color,              Brown,                foo
         2,       Ned,     Flanders,  ,              ,                ,              ,                BIBLE, 7,        Flavor,             King James,           Size,               X-Large,              LAWNMOWER, 1
         """);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void testOrderLinesWithLineExtrinsicsAndOrderExtrinsic(String csv) throws QException
   {
      Integer defaultStoreId        = 42;
      String  defaultLineNo         = "47";
      String  defaultLineExtraValue = "bar";

      CsvFileToRows   fileToRows = CsvFileToRows.forString(csv);
      BulkLoadFileRow header     = fileToRows.next();

      WideRowsToRecordWithSpreadMapping rowsToRecord = new WideRowsToRecordWithSpreadMapping();

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
         .withMappedAssociations(List.of("orderLine", "extrinsics", "orderLine.extrinsics"))
         .withFieldNameToValueMapping(Map.of("orderLine.extrinsics.value", Map.of("Large", "L", "X-Large", "XL")))
         .withFieldNameToDefaultValueMap(Map.of(
            "storeId", defaultStoreId,
            "orderLine.lineNumber", defaultLineNo,
            "orderLine.extrinsics.value", defaultLineExtraValue
         ))
         .withTableName(TestUtils.TABLE_NAME_ORDER)
         .withLayout(BulkInsertMapping.Layout.WIDE)
         .withHasHeaderRow(true);

      List<QRecord> records = rowsToRecord.nextPage(fileToRows, header, mapping, Integer.MAX_VALUE);
      assertEquals(2, records.size());

      QRecord order = records.get(0);
      assertEquals(1, order.getValueInteger("orderNo"));
      assertEquals("Homer", order.getValueString("shipToName"));
      assertEquals(defaultStoreId, order.getValue("storeId"));
      assertEquals(List.of("DONUT", "BEER", "COUCH"), getValues(order.getAssociatedRecords().get("orderLine"), "sku"));
      assertEquals(List.of(12, 500, 1), getValues(order.getAssociatedRecords().get("orderLine"), "quantity"));
      assertEquals(List.of(defaultLineNo, defaultLineNo, defaultLineNo), getValues(order.getAssociatedRecords().get("orderLine"), "lineNumber"));
      assertEquals(List.of("Store Name", "Coupon Code"), getValues(order.getAssociatedRecords().get("extrinsics"), "key"));
      assertEquals(List.of("QQQ Mart", "10QOff"), getValues(order.getAssociatedRecords().get("extrinsics"), "value"));

      QRecord lineItem = order.getAssociatedRecords().get("orderLine").get(0);
      assertEquals(List.of("Flavor", "Size"), getValues(lineItem.getAssociatedRecords().get("extrinsics"), "key"));
      assertEquals(List.of("Chocolate", "L"), getValues(lineItem.getAssociatedRecords().get("extrinsics"), "value"));

      lineItem = order.getAssociatedRecords().get("orderLine").get(1);
      assertEquals(List.of("Flavor"), getValues(lineItem.getAssociatedRecords().get("extrinsics"), "key"));
      assertEquals(List.of("Hops"), getValues(lineItem.getAssociatedRecords().get("extrinsics"), "value"));

      lineItem = order.getAssociatedRecords().get("orderLine").get(2);
      assertEquals(List.of("Color", "foo"), getValues(lineItem.getAssociatedRecords().get("extrinsics"), "key"));
      assertEquals(List.of("Brown", defaultLineExtraValue), getValues(lineItem.getAssociatedRecords().get("extrinsics"), "value"));

      order = records.get(1);
      assertEquals(2, order.getValueInteger("orderNo"));
      assertEquals("Ned", order.getValueString("shipToName"));
      assertEquals(defaultStoreId, order.getValue("storeId"));
      assertEquals(List.of("BIBLE", "LAWNMOWER"), getValues(order.getAssociatedRecords().get("orderLine"), "sku"));
      assertEquals(List.of(7, 1), getValues(order.getAssociatedRecords().get("orderLine"), "quantity"));
      assertEquals(List.of(defaultLineNo, defaultLineNo), getValues(order.getAssociatedRecords().get("orderLine"), "lineNumber"));
      assertThat(order.getAssociatedRecords().get("extrinsics")).isNullOrEmpty();

      lineItem = order.getAssociatedRecords().get("orderLine").get(0);
      assertEquals(List.of("Flavor", "Size"), getValues(lineItem.getAssociatedRecords().get("extrinsics"), "key"));
      assertEquals(List.of("King James", "XL"), getValues(lineItem.getAssociatedRecords().get("extrinsics"), "value"));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private List<Serializable> getValues(List<QRecord> records, String fieldName)
   {
      return (records.stream().map(r -> r.getValue(fieldName)).toList());
   }

}