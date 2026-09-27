/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.rdbms.actions;


import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.strategy.BaseRDBMSActionStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 **
 *******************************************************************************/
public class RDBMSInsertActionTest extends RDBMSActionTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   public void beforeEach() throws Exception
   {
      super.primeTestDatabase();

      getBaseRDBMSActionStrategyAndActivateCollectingStatistics();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testInsertNullList() throws QException
   {
      InsertInput insertInput = initInsertRequest();
      insertInput.setRecords(null);
      InsertOutput insertOutput = new InsertAction().execute(insertInput);
      assertEquals(0, insertOutput.getRecords().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testInsertEmptyList() throws QException
   {
      InsertInput insertInput = initInsertRequest();
      insertInput.setRecords(Collections.emptyList());
      InsertOutput insertOutput = new InsertAction().execute(insertInput);
      assertEquals(0, insertOutput.getRecords().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testInsertOne() throws Exception
   {
      InsertInput insertInput = initInsertRequest();
      QRecord record = new QRecord().withTableName("person")
         .withValue("firstName", "James")
         .withValue("lastName", "Kirk")
         .withValue("email", "jamestk@starfleet.net")
         .withValue("birthDate", "2210-05-20");
      insertInput.setRecords(List.of(record));
      InsertOutput insertOutput = new InsertAction().execute(insertInput);
      assertEquals(1, insertOutput.getRecords().size(), "Should return 1 row");
      assertNotNull(insertOutput.getRecords().get(0).getValue("id"), "Should have an id in the row");
      // todo - add errors to QRecord? assertTrue(insertResult.getRecords().stream().noneMatch(qrs -> CollectionUtils.nullSafeHasContents(qrs.getErrors())), "There should be no errors");
      assertAnInsertedPersonRecord("James", "Kirk", 6);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testInsertMany() throws Exception
   {
      getBaseRDBMSActionStrategy().setPageSize(2);

      InsertInput insertInput = initInsertRequest();
      QRecord record1 = new QRecord().withTableName("person")
         .withValue("firstName", "Jean-Luc")
         .withValue("lastName", "Picard")
         .withValue("email", "jl@starfleet.net")
         .withValue("birthDate", "2310-05-20");
      QRecord record2 = new QRecord().withTableName("person")
         .withValue("firstName", "William")
         .withValue("lastName", "Riker")
         .withValue("email", "notthomas@starfleet.net")
         .withValue("birthDate", "2320-05-20");
      QRecord record3 = new QRecord().withTableName("person")
         .withValue("firstName", "Beverly")
         .withValue("lastName", "Crusher")
         .withValue("email", "doctor@starfleet.net")
         .withValue("birthDate", "2320-06-26");
      insertInput.setRecords(List.of(record1, record2, record3));
      InsertOutput insertOutput = new InsertAction().execute(insertInput);
      assertEquals(3, insertOutput.getRecords().size(), "Should return right # of rows");
      assertEquals(6, insertOutput.getRecords().get(0).getValue("id"), "Should have next id in the row");
      assertEquals(7, insertOutput.getRecords().get(1).getValue("id"), "Should have next id in the row");
      assertEquals(8, insertOutput.getRecords().get(2).getValue("id"), "Should have next id in the row");

      Map<String, Integer> statistics = getBaseRDBMSActionStrategy().getStatistics();
      assertEquals(2, statistics.get(BaseRDBMSActionStrategy.STAT_QUERIES_RAN));

      assertAnInsertedPersonRecord("Jean-Luc", "Picard", 6);
      assertAnInsertedPersonRecord("William", "Riker", 7);
      assertAnInsertedPersonRecord("Beverly", "Crusher", 8);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInsertAssociations() throws QException
   {
      QContext.getQSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 1);

      int originalNoOfOrderLineExtrinsics = TestUtils.queryTable(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC).size();
      int originalNoOfOrderLines          = TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER_LINE).size();
      int originalNoOfOrders              = TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER).size();

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      insertInput.setRecords(List.of(
         new QRecord().withValue("storeId", 1).withValue("billToPersonId", 100).withValue("shipToPersonId", 200)

            .withAssociatedRecord("orderLine", new QRecord().withValue("storeId", 1).withValue("sku", "BASIC1").withValue("quantity", 1)
               .withAssociatedRecord("extrinsics", new QRecord().withValue("key", "LINE-EXT-1.1").withValue("value", "LINE-VAL-1")))

            .withAssociatedRecord("orderLine", new QRecord().withValue("storeId", 1).withValue("sku", "BASIC2").withValue("quantity", 2)
               .withAssociatedRecord("extrinsics", new QRecord().withValue("key", "LINE-EXT-2.1").withValue("value", "LINE-VAL-2"))
               .withAssociatedRecord("extrinsics", new QRecord().withValue("key", "LINE-EXT-2.2").withValue("value", "LINE-VAL-3")))
      ));
      new InsertAction().execute(insertInput);

      ////////////////////////////////////////////////////////////////////////
      // make sure ids got put on the inserted records in the output object //
      ////////////////////////////////////////////////////////////////////////
      Set<Serializable> outputOrderLineIds = insertInput.getRecords().stream()
         .flatMap(r -> r.getAssociatedRecords().get("orderLine").stream())
         .map(r -> r.getValue("id"))
         .collect(Collectors.toSet());
      assertThat(outputOrderLineIds).allMatch(id -> id != null);

      List<QRecord> orders = TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER);
      assertEquals(originalNoOfOrders + 1, orders.size());
      assertTrue(orders.stream().anyMatch(r -> Objects.equals(r.getValue("billToPersonId"), 100) && Objects.equals(r.getValue("shipToPersonId"), 200)));

      List<QRecord> orderLines = TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER_LINE);
      assertEquals(originalNoOfOrderLines + 2, orderLines.size());
      assertTrue(orderLines.stream().anyMatch(r -> Objects.equals(r.getValue("sku"), "BASIC1") && Objects.equals(r.getValue("quantity"), 1)));
      assertTrue(orderLines.stream().anyMatch(r -> Objects.equals(r.getValue("sku"), "BASIC2") && Objects.equals(r.getValue("quantity"), 2)));

      List<QRecord> lineItemExtrinsics = TestUtils.queryTable(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC);
      assertEquals(originalNoOfOrderLineExtrinsics + 3, lineItemExtrinsics.size());
      assertTrue(lineItemExtrinsics.stream().anyMatch(r -> Objects.equals(r.getValue("key"), "LINE-EXT-1.1") && Objects.equals(r.getValue("value"), "LINE-VAL-1")));
      assertTrue(lineItemExtrinsics.stream().anyMatch(r -> Objects.equals(r.getValue("key"), "LINE-EXT-2.1") && Objects.equals(r.getValue("value"), "LINE-VAL-2")));
      assertTrue(lineItemExtrinsics.stream().anyMatch(r -> Objects.equals(r.getValue("key"), "LINE-EXT-2.2") && Objects.equals(r.getValue("value"), "LINE-VAL-3")));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void assertAnInsertedPersonRecord(String firstName, String lastName, Integer id) throws Exception
   {
      runTestSql("SELECT * FROM person WHERE last_name = '" + lastName + "'", (rs ->
      {
         int rowsFound = 0;
         while(rs.next())
         {
            rowsFound++;
            assertEquals(id, rs.getInt("id"));
            assertEquals(firstName, rs.getString("first_name"));
            assertNotNull(rs.getString("create_date"));
            assertNotNull(rs.getString("modify_date"));
         }
         assertEquals(1, rowsFound);
      }));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private InsertInput initInsertRequest()
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.defineTablePerson().getName());
      return insertInput;
   }

}