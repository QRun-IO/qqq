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

package com.kingsrook.qqq.backend.core.model.data;


import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for QRecordWithJoinedRecords 
 *******************************************************************************/
class QRecordWithJoinedRecordsTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test()
   {
      QRecord order     = new QRecord().withValue("id", 1).withValue("orderNo", "101").withValue("orderDate", LocalDate.of(2025, Month.JANUARY, 1));
      QRecord lineItem  = new QRecord().withValue("id", 2).withValue("sku", "ABC").withValue("quantity", 47);
      QRecord extrinsic = new QRecord().withValue("id", 3).withValue("key", "MyKey").withValue("value", "MyValue");

      QRecordWithJoinedRecords joinedRecords = new QRecordWithJoinedRecords(order);
      joinedRecords.addJoinedRecordValues("lineItem", lineItem);
      joinedRecords.addJoinedRecordValues("extrinsic", extrinsic);

      assertEquals(1, joinedRecords.getValue("id"));
      assertEquals("101", joinedRecords.getValue("orderNo"));
      assertEquals(LocalDate.of(2025, Month.JANUARY, 1), joinedRecords.getValue("orderDate"));
      assertEquals(2, joinedRecords.getValue("lineItem.id"));
      assertEquals("ABC", joinedRecords.getValue("lineItem.sku"));
      assertEquals(47, joinedRecords.getValue("lineItem.quantity"));
      assertEquals(3, joinedRecords.getValue("extrinsic.id"));
      assertEquals("MyKey", joinedRecords.getValue("extrinsic.key"));
      assertEquals("MyValue", joinedRecords.getValue("extrinsic.value"));

      assertEquals(9, joinedRecords.getValues().size());
      assertEquals(1, joinedRecords.getValues().get("id"));
      assertEquals(2, joinedRecords.getValues().get("lineItem.id"));
      assertEquals(3, joinedRecords.getValues().get("extrinsic.id"));

      joinedRecords.setValue("lineItem.color", "RED");
      assertEquals("RED", joinedRecords.getValue("lineItem.color"));
      assertEquals("RED", lineItem.getValue("color"));

      joinedRecords.setValue("shipToCity", "St. Louis");
      assertEquals("St. Louis", joinedRecords.getValue("shipToCity"));
      assertEquals("St. Louis", order.getValue("shipToCity"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBuildCrossProduct()
   {
      QRecord order = new QRecord().withValue("id", 1).withValue("orderNo", "101").withValue("orderDate", LocalDate.of(2025, Month.JANUARY, 1));
      List<QRecord> lineItems = List.of(
         new QRecord().withValue("id", 2).withValue("sku", "ABC").withValue("quantity", 47),
         new QRecord().withValue("id", 3).withValue("sku", "DEF").withValue("quantity", 42),
         new QRecord().withValue("id", 4).withValue("sku", "XYZ").withValue("quantity", 1024)
      );

      QRecordWithJoinedRecords joinedRecords = new QRecordWithJoinedRecords(order);
      List<QRecordWithJoinedRecords> crossProduct = joinedRecords.buildCrossProduct("lineItem", lineItems);
      assertEquals(3, crossProduct.size());
      assertEquals("101", crossProduct.get(0).getValue("orderNo"));
      assertEquals("ABC", crossProduct.get(0).getValue("lineItem.sku"));
      assertEquals("101", crossProduct.get(1).getValue("orderNo"));
      assertEquals("DEF", crossProduct.get(1).getValue("lineItem.sku"));
      assertEquals("101", crossProduct.get(2).getValue("orderNo"));
      assertEquals("XYZ", crossProduct.get(2).getValue("lineItem.sku"));

   }

}