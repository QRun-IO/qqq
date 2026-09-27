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


import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.collections.ListBuilder;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


/*******************************************************************************
 ** Tests on RDBMS - specifically dealing with Joins.
 *******************************************************************************/
public class RDBMSQueryActionJoinsTest extends RDBMSActionTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   public void beforeEach() throws Exception
   {
      super.primeTestDatabase();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      AbstractRDBMSAction.setLogSQL(false);
      AbstractRDBMSAction.setLogSQLReformat(false);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QueryInput initQueryRequest()
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      return queryInput;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFilterFromJoinTableImplicitly() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter(new QFilterCriteria("personalIdCard.idNumber", QCriteriaOperator.EQUALS, "19800531")));
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertEquals(1, queryOutput.getRecords().size(), "Query should find 1 rows");
      assertThat(queryOutput.getRecords()).anyMatch(r -> r.getValueString("firstName").equals("Darin"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOneToOneInnerJoinWithoutWhere() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_PERSONAL_ID_CARD).withSelect(true));
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertEquals(3, queryOutput.getRecords().size(), "Join query should find 3 rows");
      assertThat(queryOutput.getRecords()).anyMatch(r -> r.getValueString("firstName").equals("Darin") && r.getValueString("personalIdCard.idNumber").equals("19800531"));
      assertThat(queryOutput.getRecords()).anyMatch(r -> r.getValueString("firstName").equals("James") && r.getValueString("personalIdCard.idNumber").equals("19800515"));
      assertThat(queryOutput.getRecords()).anyMatch(r -> r.getValueString("firstName").equals("Tim") && r.getValueString("personalIdCard.idNumber").equals("19760528"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOneToOneLeftJoinWithoutWhere() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_PERSONAL_ID_CARD).withType(QueryJoin.Type.LEFT).withSelect(true));
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertEquals(5, queryOutput.getRecords().size(), "Left Join query should find 5 rows");
      assertThat(queryOutput.getRecords()).anyMatch(r -> r.getValueString("firstName").equals("Darin") && r.getValueString("personalIdCard.idNumber").equals("19800531"));
      assertThat(queryOutput.getRecords()).anyMatch(r -> r.getValueString("firstName").equals("James") && r.getValueString("personalIdCard.idNumber").equals("19800515"));
      assertThat(queryOutput.getRecords()).anyMatch(r -> r.getValueString("firstName").equals("Tim") && r.getValueString("personalIdCard.idNumber").equals("19760528"));
      assertThat(queryOutput.getRecords()).anyMatch(r -> r.getValueString("firstName").equals("Garret") && r.getValue("personalIdCard.idNumber") == null);
      assertThat(queryOutput.getRecords()).anyMatch(r -> r.getValueString("firstName").equals("Tyler") && r.getValue("personalIdCard.idNumber") == null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOneToOneRightJoinWithoutWhere() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_PERSONAL_ID_CARD).withType(QueryJoin.Type.RIGHT).withSelect(true));
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertEquals(6, queryOutput.getRecords().size(), "Right Join query should find 6 rows");
      assertThat(queryOutput.getRecords()).anyMatch(r -> r.getValueString("firstName").equals("Darin") && r.getValueString("personalIdCard.idNumber").equals("19800531"));
      assertThat(queryOutput.getRecords()).anyMatch(r -> r.getValueString("firstName").equals("James") && r.getValueString("personalIdCard.idNumber").equals("19800515"));
      assertThat(queryOutput.getRecords()).anyMatch(r -> r.getValueString("firstName").equals("Tim") && r.getValueString("personalIdCard.idNumber").equals("19760528"));
      assertThat(queryOutput.getRecords()).anyMatch(r -> r.getValue("firstName") == null && r.getValueString("personalIdCard.idNumber").equals("123123123"));
      assertThat(queryOutput.getRecords()).anyMatch(r -> r.getValue("firstName") == null && r.getValueString("personalIdCard.idNumber").equals("987987987"));
      assertThat(queryOutput.getRecords()).anyMatch(r -> r.getValue("firstName") == null && r.getValueString("personalIdCard.idNumber").equals("456456456"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOneToOneInnerJoinWithWhere() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_PERSONAL_ID_CARD).withSelect(true));
      queryInput.setFilter(new QQueryFilter(new QFilterCriteria(TestUtils.TABLE_NAME_PERSONAL_ID_CARD + ".idNumber", QCriteriaOperator.STARTS_WITH, "1980")));
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertEquals(2, queryOutput.getRecords().size(), "Join query should find 2 rows");
      assertThat(queryOutput.getRecords()).anyMatch(r -> r.getValueString("firstName").equals("Darin") && r.getValueString("personalIdCard.idNumber").equals("19800531"));
      assertThat(queryOutput.getRecords()).anyMatch(r -> r.getValueString("firstName").equals("James") && r.getValueString("personalIdCard.idNumber").equals("19800515"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOneToOneInnerJoinWithOrderBy() throws QException
   {
      QInstance  qInstance  = TestUtils.defineInstance();
      QueryInput queryInput = initQueryRequest();
      queryInput.withQueryJoin(new QueryJoin(qInstance.getJoin(TestUtils.TABLE_NAME_PERSON + "Join" + StringUtils.ucFirst(TestUtils.TABLE_NAME_PERSONAL_ID_CARD))).withSelect(true));
      queryInput.setFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy(TestUtils.TABLE_NAME_PERSONAL_ID_CARD + ".idNumber")));
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertEquals(3, queryOutput.getRecords().size(), "Join query should find 3 rows");
      List<String> idNumberListFromQuery = queryOutput.getRecords().stream().map(r -> r.getValueString(TestUtils.TABLE_NAME_PERSONAL_ID_CARD + ".idNumber")).toList();
      assertEquals(List.of("19760528", "19800515", "19800531"), idNumberListFromQuery);

      /////////////////////////
      // repeat, sorted desc //
      /////////////////////////
      queryInput.setFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy(TestUtils.TABLE_NAME_PERSONAL_ID_CARD + ".idNumber", false)));
      queryOutput = new QueryAction().execute(queryInput);
      assertEquals(3, queryOutput.getRecords().size(), "Join query should find 3 rows");
      idNumberListFromQuery = queryOutput.getRecords().stream().map(r -> r.getValueString(TestUtils.TABLE_NAME_PERSONAL_ID_CARD + ".idNumber")).toList();
      assertEquals(List.of("19800531", "19800515", "19760528"), idNumberListFromQuery);
   }



   /*******************************************************************************
    ** In the prime data, we've got 1 order line set up with an item from a different
    ** store than its order.  Write a query to find such a case.
    *******************************************************************************/
   @Test
   void testFiveTableOmsJoinFindMismatchedStoreId() throws Exception
   {
      QueryInput queryInput = new QueryInput();
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_STORE_ALL_ACCESS, true));
      queryInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      queryInput.withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_ORDER, TestUtils.TABLE_NAME_STORE).withAlias("orderStore").withSelect(true));
      queryInput.withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_ORDER, TestUtils.TABLE_NAME_ORDER_LINE).withSelect(true));
      queryInput.withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_ORDER_LINE, TestUtils.TABLE_NAME_ITEM).withSelect(true));
      queryInput.withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_ITEM, TestUtils.TABLE_NAME_STORE).withAlias("itemStore").withSelect(true));

      queryInput.setFilter(new QQueryFilter(new QFilterCriteria().withFieldName("orderStore.id").withOperator(QCriteriaOperator.NOT_EQUALS).withOtherFieldName("item.storeId")));
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertEquals(1, queryOutput.getRecords().size(), "# of rows found by query");
      QRecord qRecord = queryOutput.getRecords().get(0);
      assertEquals(2, qRecord.getValueInteger("id"));
      assertEquals(1, qRecord.getValueInteger("orderStore.id"));
      assertEquals(2, qRecord.getValueInteger("itemStore.id"));

      //////////////////////////////////////////////////////////////////////////////////////////////////////////
      // run the same setup, but this time, use the other-field-name as itemStore.id, instead of item.storeId //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////
      queryInput.setFilter(new QQueryFilter(new QFilterCriteria().withFieldName("orderStore.id").withOperator(QCriteriaOperator.NOT_EQUALS).withOtherFieldName("itemStore.id")));
      queryOutput = new QueryAction().execute(queryInput);
      assertEquals(1, queryOutput.getRecords().size(), "# of rows found by query");
      qRecord = queryOutput.getRecords().get(0);
      assertEquals(2, qRecord.getValueInteger("id"));
      assertEquals(1, qRecord.getValueInteger("orderStore.id"));
      assertEquals(2, qRecord.getValueInteger("itemStore.id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOmsQueryByOrderLines() throws Exception
   {
      AtomicInteger orderLineCount = new AtomicInteger();
      runTestSql("SELECT COUNT(*) from order_line", (rs) ->
      {
         rs.next();
         orderLineCount.set(rs.getInt(1));
      });

      QueryInput queryInput = new QueryInput();
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_STORE_ALL_ACCESS, true));
      queryInput.setTableName(TestUtils.TABLE_NAME_ORDER_LINE);
      queryInput.withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_ORDER).withSelect(true));

      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertEquals(orderLineCount.get(), queryOutput.getRecords().size(), "# of rows found by query");
      assertEquals(3, queryOutput.getRecords().stream().filter(r -> r.getValueInteger("order.id").equals(1)).count());
      assertEquals(1, queryOutput.getRecords().stream().filter(r -> r.getValueInteger("order.id").equals(2)).count());
      assertEquals(1, queryOutput.getRecords().stream().filter(r -> r.getValueInteger("orderId").equals(3)).count());
      assertEquals(2, queryOutput.getRecords().stream().filter(r -> r.getValueInteger("orderId").equals(4)).count());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOmsQueryByPersons() throws Exception
   {
      QInstance  instance   = TestUtils.defineInstance();
      QueryInput queryInput = new QueryInput();
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_STORE_ALL_ACCESS, true));
      queryInput.setTableName(TestUtils.TABLE_NAME_ORDER);

      /////////////////////////////////////////////////////
      // inner join on bill-to person should find 6 rows //
      /////////////////////////////////////////////////////
      queryInput.withQueryJoins(List.of(new QueryJoin(TestUtils.TABLE_NAME_PERSON).withJoinMetaData(instance.getJoin("orderJoinBillToPerson")).withSelect(true)));
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertEquals(6, queryOutput.getRecords().size(), "# of rows found by query");

      /////////////////////////////////////////////////////
      // inner join on ship-to person should find 7 rows //
      /////////////////////////////////////////////////////
      queryInput.withQueryJoins(List.of(new QueryJoin(instance.getJoin("orderJoinShipToPerson")).withSelect(true)));
      queryOutput = new QueryAction().execute(queryInput);
      assertEquals(7, queryOutput.getRecords().size(), "# of rows found by query");

      /////////////////////////////////////////////////////////////////////////////
      // inner join on both bill-to person and ship-to person should find 5 rows //
      /////////////////////////////////////////////////////////////////////////////
      queryInput.withQueryJoins(List.of(
         new QueryJoin(instance.getJoin("orderJoinShipToPerson")).withAlias("shipToPerson").withSelect(true),
         new QueryJoin(instance.getJoin("orderJoinBillToPerson")).withAlias("billToPerson").withSelect(true)
      ));
      queryOutput = new QueryAction().execute(queryInput);
      assertEquals(5, queryOutput.getRecords().size(), "# of rows found by query");

      /////////////////////////////////////////////////////////////////////////////
      // left join on both bill-to person and ship-to person should find 8 rows //
      /////////////////////////////////////////////////////////////////////////////
      queryInput.withQueryJoins(List.of(
         new QueryJoin(instance.getJoin("orderJoinShipToPerson")).withType(QueryJoin.Type.LEFT).withAlias("shipToPerson").withSelect(true),
         new QueryJoin(instance.getJoin("orderJoinBillToPerson")).withType(QueryJoin.Type.LEFT).withAlias("billToPerson").withSelect(true)
      ));
      queryOutput = new QueryAction().execute(queryInput);
      assertEquals(8, queryOutput.getRecords().size(), "# of rows found by query");

      //////////////////////////////////////////////////
      // now join through to personalIdCard table too //
      //////////////////////////////////////////////////
      queryInput.withQueryJoins(List.of(
         new QueryJoin(instance.getJoin("orderJoinShipToPerson")).withAlias("shipToPerson").withSelect(true),
         new QueryJoin(instance.getJoin("orderJoinBillToPerson")).withAlias("billToPerson").withSelect(true),
         new QueryJoin("billToPerson", TestUtils.TABLE_NAME_PERSONAL_ID_CARD).withAlias("billToIdCard").withSelect(true),
         new QueryJoin("shipToPerson", TestUtils.TABLE_NAME_PERSONAL_ID_CARD).withAlias("shipToIdCard").withSelect(true)
      ));
      queryInput.setFilter(new QQueryFilter()
         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // look for billToPersons w/ idNumber starting with 1980 - should only be James and Darin (assert on that below). //
         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         .withCriteria(new QFilterCriteria("billToIdCard.idNumber", QCriteriaOperator.STARTS_WITH, "1980"))
      );
      queryOutput = new QueryAction().execute(queryInput);
      assertEquals(3, queryOutput.getRecords().size(), "# of rows found by query");
      assertThat(queryOutput.getRecords().stream().map(r -> r.getValueString("billToPerson.firstName")).toList()).allMatch(p -> p.equals("Darin") || p.equals("James"));

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // ensure we throw if either of the ambiguous joins from person to id-card doesn't specify its left-table //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////
      queryInput.withQueryJoins(List.of(
         new QueryJoin(instance.getJoin("orderJoinShipToPerson")).withAlias("shipToPerson").withSelect(true),
         new QueryJoin(instance.getJoin("orderJoinBillToPerson")).withAlias("billToPerson").withSelect(true),
         new QueryJoin(TestUtils.TABLE_NAME_PERSONAL_ID_CARD).withAlias("billToIdCard").withSelect(true),
         new QueryJoin("shipToPerson", TestUtils.TABLE_NAME_PERSONAL_ID_CARD).withAlias("shipToIdCard").withSelect(true)
      ));
      assertThatThrownBy(() -> new QueryAction().execute(queryInput))
         .rootCause()
         .hasMessageContaining("Could not find a join between tables [order][personalIdCard]");

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // ensure we throw if either of the ambiguous joins from person to id-card doesn't specify its left-table //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////
      queryInput.withQueryJoins(List.of(
         new QueryJoin(instance.getJoin("orderJoinShipToPerson")).withAlias("shipToPerson").withSelect(true),
         new QueryJoin(instance.getJoin("orderJoinBillToPerson")).withAlias("billToPerson").withSelect(true),
         new QueryJoin("billToPerson", TestUtils.TABLE_NAME_PERSONAL_ID_CARD).withAlias("billToIdCard").withSelect(true),
         new QueryJoin(TestUtils.TABLE_NAME_PERSONAL_ID_CARD).withAlias("shipToIdCard").withSelect(true)
      ));
      assertThatThrownBy(() -> new QueryAction().execute(queryInput))
         .rootCause()
         .hasMessageContaining("Could not find a join between tables [order][personalIdCard]");

      ////////////////////////////////////////////////////////////////////////
      // ensure we throw if we have a bogus alias name given as a left-side //
      ////////////////////////////////////////////////////////////////////////
      queryInput.withQueryJoins(List.of(
         new QueryJoin(instance.getJoin("orderJoinShipToPerson")).withAlias("shipToPerson").withSelect(true),
         new QueryJoin(instance.getJoin("orderJoinBillToPerson")).withAlias("billToPerson").withSelect(true),
         new QueryJoin("notATable", TestUtils.TABLE_NAME_PERSONAL_ID_CARD).withAlias("billToIdCard").withSelect(true),
         new QueryJoin("shipToPerson", TestUtils.TABLE_NAME_PERSONAL_ID_CARD).withAlias("shipToIdCard").withSelect(true)
      ));
      assertThatThrownBy(() -> new QueryAction().execute(queryInput))
         .hasRootCauseMessage("Could not find a join between tables [notATable][personalIdCard]");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOmsQueryByPersonsExtraKelkhoffOrder() throws Exception
   {
      QInstance  instance   = TestUtils.defineInstance();
      QueryInput queryInput = new QueryInput();
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_STORE_ALL_ACCESS, true));
      queryInput.setTableName(TestUtils.TABLE_NAME_ORDER);

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // insert a second person w/ last name Kelkhoff, then an order for Darin Kelkhoff and this new Kelkhoff - //
      // then query for orders w/ bill to person & ship to person both lastname = Kelkhoff, but different ids.  //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////
      Integer specialOrderId = 1701;
      runTestSql("INSERT INTO person (id, first_name, last_name, email) VALUES (6, 'Jimmy', 'Kelkhoff', 'dk@gmail.com')", null);
      runTestSql("INSERT INTO `order` (id, store_id, bill_to_person_id, ship_to_person_id) VALUES (" + specialOrderId + ", 1, 1, 6)", null);
      queryInput.withQueryJoins(List.of(
         new QueryJoin(instance.getJoin("orderJoinShipToPerson")).withType(QueryJoin.Type.LEFT).withAlias("shipToPerson").withSelect(true),
         new QueryJoin(instance.getJoin("orderJoinBillToPerson")).withType(QueryJoin.Type.LEFT).withAlias("billToPerson").withSelect(true)
      ));
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria().withFieldName("shipToPerson.lastName").withOperator(QCriteriaOperator.EQUALS).withOtherFieldName("billToPerson.lastName"))
         .withCriteria(new QFilterCriteria().withFieldName("shipToPerson.id").withOperator(QCriteriaOperator.NOT_EQUALS).withOtherFieldName("billToPerson.id"))
      );
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertEquals(1, queryOutput.getRecords().size(), "# of rows found by query");
      assertEquals(specialOrderId, queryOutput.getRecords().get(0).getValueInteger("id"));

      ////////////////////////////////////////////////////////////
      // re-run that query using personIds from the order table //
      ////////////////////////////////////////////////////////////
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria().withFieldName("shipToPerson.lastName").withOperator(QCriteriaOperator.EQUALS).withOtherFieldName("billToPerson.lastName"))
         .withCriteria(new QFilterCriteria().withFieldName("order.shipToPersonId").withOperator(QCriteriaOperator.NOT_EQUALS).withOtherFieldName("order.billToPersonId"))
      );
      queryOutput = new QueryAction().execute(queryInput);
      assertEquals(1, queryOutput.getRecords().size(), "# of rows found by query");
      assertEquals(specialOrderId, queryOutput.getRecords().get(0).getValueInteger("id"));

      ///////////////////////////////////////////////////////////////////////////////////////////////
      // re-run that query using personIds from the order table, but not specifying the table name //
      ///////////////////////////////////////////////////////////////////////////////////////////////
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria().withFieldName("shipToPerson.lastName").withOperator(QCriteriaOperator.EQUALS).withOtherFieldName("billToPerson.lastName"))
         .withCriteria(new QFilterCriteria().withFieldName("shipToPersonId").withOperator(QCriteriaOperator.NOT_EQUALS).withOtherFieldName("billToPersonId"))
      );
      queryOutput = new QueryAction().execute(queryInput);
      assertEquals(1, queryOutput.getRecords().size(), "# of rows found by query");
      assertEquals(specialOrderId, queryOutput.getRecords().get(0).getValueInteger("id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDuplicateAliases()
   {
      QInstance  instance   = TestUtils.defineInstance();
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_ORDER);

      queryInput.withQueryJoins(List.of(
         new QueryJoin(instance.getJoin("orderJoinShipToPerson")).withAlias("shipToPerson"),
         new QueryJoin(instance.getJoin("orderJoinBillToPerson")).withAlias("billToPerson"),
         new QueryJoin("billToPerson", TestUtils.TABLE_NAME_PERSONAL_ID_CARD).withSelect(true),
         new QueryJoin("shipToPerson", TestUtils.TABLE_NAME_PERSONAL_ID_CARD).withSelect(true) // w/o alias, should get exception here - dupe table.
      ));
      assertThatThrownBy(() -> new QueryAction().execute(queryInput))
         .hasRootCauseMessage("Duplicate table name or alias: personalIdCard");

      queryInput.withQueryJoins(List.of(
         new QueryJoin(instance.getJoin("orderJoinShipToPerson")).withAlias("shipToPerson"),
         new QueryJoin(instance.getJoin("orderJoinBillToPerson")).withAlias("billToPerson"),
         new QueryJoin("shipToPerson", TestUtils.TABLE_NAME_PERSONAL_ID_CARD).withAlias("shipToPerson").withSelect(true), // dupe alias, should get exception here
         new QueryJoin("billToPerson", TestUtils.TABLE_NAME_PERSONAL_ID_CARD).withAlias("billToPerson").withSelect(true)
      ));
      assertThatThrownBy(() -> new QueryAction().execute(queryInput))
         .hasRootCauseMessage("Duplicate table name or alias: shipToPerson");
   }



   /*******************************************************************************
    ** Given tables:
    **   order - orderLine - item
    ** with exposedJoin on order to item
    ** do a query on order, also selecting item.
    *******************************************************************************/
   @Test
   void testTwoTableAwayExposedJoin() throws QException
   {
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_STORE_ALL_ACCESS, true));

      QInstance  instance   = TestUtils.defineInstance();
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_ORDER);

      queryInput.withQueryJoins(List.of(
         new QueryJoin(TestUtils.TABLE_NAME_ITEM).withType(QueryJoin.Type.INNER).withSelect(true)
      ));

      QueryOutput queryOutput = new QueryAction().execute(queryInput);

      List<QRecord> records = queryOutput.getRecords();
      assertThat(records).hasSize(11); // one per line item
      assertThat(records).allMatch(r -> r.getValue("id") != null);
      assertThat(records).allMatch(r -> r.getValue(TestUtils.TABLE_NAME_ITEM + ".description") != null);
   }



   /*******************************************************************************
    ** Given tables:
    **   order - orderLine - item
    ** with exposedJoin on item to order
    ** do a query on item, also selecting order.
    ** This is a reverse of the above, to make sure join flipping, etc, is good.
    *******************************************************************************/
   @Test
   void testTwoTableAwayExposedJoinReversed() throws QException
   {
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_STORE_ALL_ACCESS, true));

      QInstance  instance   = TestUtils.defineInstance();
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_ITEM);

      queryInput.withQueryJoins(List.of(
         new QueryJoin(TestUtils.TABLE_NAME_ORDER).withType(QueryJoin.Type.INNER).withSelect(true)
      ));

      QueryOutput queryOutput = new QueryAction().execute(queryInput);

      List<QRecord> records = queryOutput.getRecords();
      assertThat(records).hasSize(11); // one per line item
      assertThat(records).allMatch(r -> r.getValue("description") != null);
      assertThat(records).allMatch(r -> r.getValue(TestUtils.TABLE_NAME_ORDER + ".id") != null);
   }



   /*******************************************************************************
    ** Given tables:
    **   order - orderLine - item
    ** with exposedJoin on order to item
    ** do a query on order, also selecting item, and also selecting orderLine...
    *******************************************************************************/
   @Test
   void testTwoTableAwayExposedJoinAlsoSelectingInBetweenTable() throws QException
   {
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_STORE_ALL_ACCESS, true));

      QInstance  instance   = TestUtils.defineInstance();
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_ORDER);

      queryInput.withQueryJoins(List.of(
         new QueryJoin(TestUtils.TABLE_NAME_ORDER_LINE).withType(QueryJoin.Type.INNER).withSelect(true),
         new QueryJoin(TestUtils.TABLE_NAME_ITEM).withType(QueryJoin.Type.INNER).withSelect(true)
      ));

      QueryOutput queryOutput = new QueryAction().execute(queryInput);

      List<QRecord> records = queryOutput.getRecords();
      assertThat(records).hasSize(11); // one per line item
      assertThat(records).allMatch(r -> r.getValue("id") != null);
      assertThat(records).allMatch(r -> r.getValue(TestUtils.TABLE_NAME_ORDER_LINE + ".quantity") != null);
      assertThat(records).allMatch(r -> r.getValue(TestUtils.TABLE_NAME_ITEM + ".description") != null);
   }



   /*******************************************************************************
    ** Given tables:
    **   order - orderLine - item
    ** with exposedJoin on order to item
    ** do a query on order, filtered by item
    *******************************************************************************/
   @Test
   void testTwoTableAwayExposedJoinWhereClauseOnly() throws QException
   {
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_STORE_ALL_ACCESS, true));

      QInstance  instance   = TestUtils.defineInstance();
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      queryInput.setFilter(new QQueryFilter(new QFilterCriteria(TestUtils.TABLE_NAME_ITEM + ".description", QCriteriaOperator.STARTS_WITH, "Q-Mart")));

      QueryOutput queryOutput = new QueryAction().execute(queryInput);

      List<QRecord> records = queryOutput.getRecords();
      assertThat(records).hasSize(4);
      assertThat(records).allMatch(r -> r.getValue("id") != null);
   }



   /*******************************************************************************
    ** Given tables:
    **   order - orderLine - item
    ** with exposedJoin on order to item
    ** do a query on order, filtered by item
    *******************************************************************************/
   @Test
   void testTwoTableAwayExposedJoinWhereClauseBothJoinTables() throws QException
   {
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_STORE_ALL_ACCESS, true));

      QInstance  instance   = TestUtils.defineInstance();
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria(TestUtils.TABLE_NAME_ITEM + ".description", QCriteriaOperator.STARTS_WITH, "Q-Mart"))
         .withCriteria(new QFilterCriteria(TestUtils.TABLE_NAME_ORDER_LINE + ".quantity", QCriteriaOperator.IS_NOT_BLANK))
      );

      QueryOutput queryOutput = new QueryAction().execute(queryInput);

      List<QRecord> records = queryOutput.getRecords();
      assertThat(records).hasSize(4);
      assertThat(records).allMatch(r -> r.getValue("id") != null);
   }



   /*******************************************************************************
    ** queries on the store table, where the primary key (id) is the security field
    *******************************************************************************/
   @Test
   void testRecordSecurityPrimaryKeyFieldNoFilters() throws QException
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_STORE);

      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_STORE_ALL_ACCESS, true));
      assertThat(new QueryAction().execute(queryInput).getRecords()).hasSize(3);

      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 1));
      assertThat(new QueryAction().execute(queryInput).getRecords())
         .hasSize(1)
         .anyMatch(r -> r.getValueInteger("id").equals(1));

      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 2));
      assertThat(new QueryAction().execute(queryInput).getRecords())
         .hasSize(1)
         .anyMatch(r -> r.getValueInteger("id").equals(2));

      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 5));
      assertThat(new QueryAction().execute(queryInput).getRecords()).isEmpty();

      QContext.setQSession(new QSession());
      assertThat(new QueryAction().execute(queryInput).getRecords()).isEmpty();

      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, null));
      assertThat(new QueryAction().execute(queryInput).getRecords()).isEmpty();

      QContext.setQSession(new QSession().withSecurityKeyValues(Collections.emptyMap()));
      assertThat(new QueryAction().execute(queryInput).getRecords()).isEmpty();

      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 1).withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 3));
      assertThat(new QueryAction().execute(queryInput).getRecords())
         .hasSize(2)
         .anyMatch(r -> r.getValueInteger("id").equals(1))
         .anyMatch(r -> r.getValueInteger("id").equals(3));
   }



   /*******************************************************************************
    ** not really expected to be any different from where we filter on the primary key,
    ** but just good to make sure
    *******************************************************************************/
   @Test
   void testRecordSecurityForeignKeyFieldNoFilters() throws QException
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_ORDER);

      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_STORE_ALL_ACCESS, true));
      assertThat(new QueryAction().execute(queryInput).getRecords()).hasSize(8);

      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 1));
      assertThat(new QueryAction().execute(queryInput).getRecords())
         .hasSize(3)
         .allMatch(r -> r.getValueInteger("storeId").equals(1));

      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 2));
      assertThat(new QueryAction().execute(queryInput).getRecords())
         .hasSize(2)
         .allMatch(r -> r.getValueInteger("storeId").equals(2));

      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 5));
      assertThat(new QueryAction().execute(queryInput).getRecords()).isEmpty();

      QContext.setQSession(new QSession());
      assertThat(new QueryAction().execute(queryInput).getRecords()).isEmpty();

      QContext.setQSession(new QSession().withSecurityKeyValues(Collections.emptyMap()));
      assertThat(new QueryAction().execute(queryInput).getRecords()).isEmpty();

      QContext.setQSession(new QSession().withSecurityKeyValues(Map.of(TestUtils.TABLE_NAME_STORE, Collections.emptyList())));
      assertThat(new QueryAction().execute(queryInput).getRecords()).isEmpty();

      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 1).withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 3));
      assertThat(new QueryAction().execute(queryInput).getRecords())
         .hasSize(6)
         .allMatch(r -> r.getValueInteger("storeId").equals(1) || r.getValueInteger("storeId").equals(3));
   }



   /*******************************************************************************
    ** Error seen in CTLive - query for order join lineItem, where lineItem's security
    ** key is in order.
    **
    ** Note - in this test-db setup, there happens to be a storeId in both order &
    ** orderLine tables, so we can't quite reproduce the error we saw in CTL - so
    ** query on different tables with the structure that'll produce the error.
    *******************************************************************************/
   @Test
   void testRequestedJoinWithTableWhoseSecurityFieldIsInMainTable() throws QException
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_WAREHOUSE_STORE_INT);
      queryInput.withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_WAREHOUSE).withSelect(true));

      //////////////////////////////////////////////
      // with the all-access key, find all 3 rows //
      //////////////////////////////////////////////
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_STORE_ALL_ACCESS, true));
      assertThat(new QueryAction().execute(queryInput).getRecords()).hasSize(3);

      ///////////////////////////////////////////
      // with 1 security key value, find 1 row //
      ///////////////////////////////////////////
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 1));
      assertThat(new QueryAction().execute(queryInput).getRecords())
         .hasSize(1)
         .allMatch(r -> r.getValueInteger("storeId").equals(1));

      ///////////////////////////////////////////
      // with 1 security key value, find 1 row //
      ///////////////////////////////////////////
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 2));
      assertThat(new QueryAction().execute(queryInput).getRecords())
         .hasSize(1)
         .allMatch(r -> r.getValueInteger("storeId").equals(2));

      //////////////////////////////////////////////////////////
      // with a mis-matching security key value, 0 rows found //
      //////////////////////////////////////////////////////////
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 5));
      assertThat(new QueryAction().execute(queryInput).getRecords()).isEmpty();

      ///////////////////////////////////////////////
      // with no security key values, 0 rows found //
      ///////////////////////////////////////////////
      QContext.setQSession(new QSession());
      assertThat(new QueryAction().execute(queryInput).getRecords()).isEmpty();

      ////////////////////////////////////////////////
      // with null security key value, 0 rows found //
      ////////////////////////////////////////////////
      QContext.setQSession(new QSession().withSecurityKeyValues(Collections.emptyMap()));
      assertThat(new QueryAction().execute(queryInput).getRecords()).isEmpty();

      //////////////////////////////////////////////////////
      // with empty-list security key value, 0 rows found //
      //////////////////////////////////////////////////////
      QContext.setQSession(new QSession().withSecurityKeyValues(Map.of(TestUtils.TABLE_NAME_STORE, Collections.emptyList())));
      assertThat(new QueryAction().execute(queryInput).getRecords()).isEmpty();

      ////////////////////////////////
      // with 2 values, find 2 rows //
      ////////////////////////////////
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 1).withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 3));
      assertThat(new QueryAction().execute(queryInput).getRecords())
         .hasSize(2)
         .allMatch(r -> r.getValueInteger("storeId").equals(1) || r.getValueInteger("storeId").equals(3));
   }



   /*******************************************************************************
    ** Error seen in CTLive - query for a record in a sub-table, but whose security
    ** key comes from a main table, but the main-table record doesn't exist.
    **
    ** In this QInstance, our warehouse table's security key comes from
    ** storeWarehouseInt.storeId - so if we insert a warehouse, but no stores, we
    ** might not be able to find it (if this bug exists!)
    *******************************************************************************/
   @Test
   void testRequestedJoinWithTableWhoseSecurityFieldIsInMainTableAndNoRowIsInMainTable() throws Exception
   {
      runTestSql("INSERT INTO warehouse (name) VALUES ('Springfield')", null);

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_WAREHOUSE);
      queryInput.setFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Springfield")));

      /////////////////////////////////////////
      // with all access key, should find it //
      /////////////////////////////////////////
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_STORE_ALL_ACCESS, true));
      assertThat(new QueryAction().execute(queryInput).getRecords()).hasSize(1);

      ////////////////////////////////////////////
      // with a regular key, should not find it //
      ////////////////////////////////////////////
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 1));
      assertThat(new QueryAction().execute(queryInput).getRecords()).hasSize(0);

      /////////////////////////////////////////
      // now assign the warehouse to a store //
      /////////////////////////////////////////
      runTestSql("INSERT INTO warehouse_store_int (store_id, warehouse_id) SELECT 1, id FROM warehouse WHERE name='Springfield'", null);

      /////////////////////////////////////////
      // with all access key, should find it //
      /////////////////////////////////////////
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_STORE_ALL_ACCESS, true));
      assertThat(new QueryAction().execute(queryInput).getRecords()).hasSize(1);

      ///////////////////////////////////////////////////////
      // with a regular key, should find it if key matches //
      ///////////////////////////////////////////////////////
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 1));
      assertThat(new QueryAction().execute(queryInput).getRecords()).hasSize(1);

      //////////////////////////////////////////////////////////////////
      // with a regular key, should not find it if key does not match //
      //////////////////////////////////////////////////////////////////
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 2));
      assertThat(new QueryAction().execute(queryInput).getRecords()).hasSize(0);

   }


   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRecordSecurityWithFilters() throws QException
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_ORDER);

      queryInput.setFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.BETWEEN, List.of(2, 7))));
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_STORE_ALL_ACCESS, true));
      assertThat(new QueryAction().execute(queryInput).getRecords()).hasSize(6);

      queryInput.setFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.BETWEEN, List.of(2, 7))));
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 1));
      assertThat(new QueryAction().execute(queryInput).getRecords())
         .hasSize(2)
         .allMatch(r -> r.getValueInteger("storeId").equals(1));

      queryInput.setFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.BETWEEN, List.of(2, 7))));
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 5));
      assertThat(new QueryAction().execute(queryInput).getRecords()).isEmpty();

      queryInput.setFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.BETWEEN, List.of(2, 7))));
      QContext.setQSession(new QSession());
      assertThat(new QueryAction().execute(queryInput).getRecords()).isEmpty();

      queryInput.setFilter(new QQueryFilter(new QFilterCriteria("storeId", QCriteriaOperator.IN, List.of(1, 2))));
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 1).withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 3));
      assertThat(new QueryAction().execute(queryInput).getRecords())
         .hasSize(3)
         .allMatch(r -> r.getValueInteger("storeId").equals(1));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRecordSecurityFromJoinTableAlsoImplicitlyInQuery() throws QException
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_ORDER_LINE);

      ///////////////////////////////////////////////////////////////////////////////////////
      // orders 1, 2, and 3 are from store 1, so their lines (5 in total) should be found. //
      // note, order 2 has the line with mis-matched store id - but, that shouldn't apply  //
      // here, because the line table's security comes from the order table.               //
      ///////////////////////////////////////////////////////////////////////////////////////
      queryInput.setFilter(new QQueryFilter(new QFilterCriteria("order.id", QCriteriaOperator.IN, List.of(1, 2, 3, 4))));
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 1));
      assertThat(new QueryAction().execute(queryInput).getRecords()).hasSize(5);

      ///////////////////////////////////////////////////////////////////
      // order 4 should be the only one found this time (with 2 lines) //
      ///////////////////////////////////////////////////////////////////
      queryInput.setFilter(new QQueryFilter(new QFilterCriteria("order.id", QCriteriaOperator.IN, List.of(1, 2, 3, 4))));
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 2));
      assertThat(new QueryAction().execute(queryInput).getRecords()).hasSize(2);

      ////////////////////////////////////////////////////////////////
      // make sure we're also good if we explicitly join this table //
      ////////////////////////////////////////////////////////////////
      queryInput.withQueryJoin(new QueryJoin().withJoinTable(TestUtils.TABLE_NAME_ORDER).withSelect(true));
      assertThat(new QueryAction().execute(queryInput).getRecords()).hasSize(2);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRecordSecurityWithLockFromJoinTable() throws QException
   {
      QInstance  qInstance  = TestUtils.defineInstance();
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_ORDER);

      /////////////////////////////////////////////////////////////////////////////////////////////////
      // remove the normal lock on the order table - replace it with one from the joined store table //
      /////////////////////////////////////////////////////////////////////////////////////////////////
      qInstance.getTable(TestUtils.TABLE_NAME_ORDER).getRecordSecurityLocks().clear();
      qInstance.getTable(TestUtils.TABLE_NAME_ORDER).withRecordSecurityLock(new RecordSecurityLock()
         .withSecurityKeyType(TestUtils.TABLE_NAME_STORE)
         .withJoinNameChain(List.of("orderJoinStore"))
         .withFieldName("store.id"));

      queryInput.setFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.BETWEEN, List.of(2, 7))));
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_STORE_ALL_ACCESS, true));
      assertThat(new QueryAction().execute(queryInput).getRecords()).hasSize(6);

      queryInput.setFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.BETWEEN, List.of(2, 7))));
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 1));
      assertThat(new QueryAction().execute(queryInput).getRecords())
         .hasSize(2)
         .allMatch(r -> r.getValueInteger("storeId").equals(1));

      queryInput.setFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.BETWEEN, List.of(2, 7))));
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 5));
      assertThat(new QueryAction().execute(queryInput).getRecords()).isEmpty();

      queryInput.setFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.BETWEEN, List.of(2, 7))));
      QContext.setQSession(new QSession());
      assertThat(new QueryAction().execute(queryInput).getRecords()).isEmpty();

      queryInput.setFilter(new QQueryFilter(new QFilterCriteria("storeId", QCriteriaOperator.IN, List.of(1, 2))));
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 1).withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 3));
      assertThat(new QueryAction().execute(queryInput).getRecords())
         .hasSize(3)
         .allMatch(r -> r.getValueInteger("storeId").equals(1));
   }



   /*******************************************************************************
    ** Note, this test was originally written asserting size=1... but reading
    ** the data, for an all-access key, that seems wrong - as the user should see
    ** all the records in this table, not just ones associated with a store...
    ** so, switching to 4 (same issue in CountActionTest too).
    *******************************************************************************/
   @Test
   void testRecordSecurityWithLockFromJoinTableWhereTheKeyIsOnTheManySide() throws QException
   {
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_STORE_ALL_ACCESS, true));
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_WAREHOUSE);

      List<QRecord> records = new QueryAction().execute(queryInput).getRecords();
      assertThat(records)
         .hasSize(4);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMultipleReversedDirectionJoinsBetweenSameTablesAllAccessKey() throws QException
   {
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_STORE_ALL_ACCESS, true));

      Integer noOfOrders            = new CountAction().execute(new CountInput(TestUtils.TABLE_NAME_ORDER)).getCount();
      Integer noOfOrderInstructions = new CountAction().execute(new CountInput(TestUtils.TABLE_NAME_ORDER_INSTRUCTIONS)).getCount();

      {
         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // make sure we can join on order.current_order_instruction_id = order_instruction.id -- and that we get back 1 row per order //
         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(TestUtils.TABLE_NAME_ORDER);
         queryInput.withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_ORDER_INSTRUCTIONS).withJoinMetaData(QContext.getQInstance().getJoin("orderJoinCurrentOrderInstructions")));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertEquals(noOfOrders, queryOutput.getRecords().size());
      }

      {
         ////////////////////////////////////////////////////////////////////////////////////////////////
         // assert that the query succeeds (based on exposed join) if the joinMetaData isn't specified //
         ////////////////////////////////////////////////////////////////////////////////////////////////
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(TestUtils.TABLE_NAME_ORDER);
         queryInput.withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_ORDER_INSTRUCTIONS));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertEquals(noOfOrders, queryOutput.getRecords().size());
      }

      {
         ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // make sure we can join on order.id = order_instruction.order_id (e.g., not the exposed one used above) -- and that we get back 1 row per order instruction //
         ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(TestUtils.TABLE_NAME_ORDER);
         queryInput.withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_ORDER_INSTRUCTIONS).withJoinMetaData(QContext.getQInstance().getJoin("orderInstructionsJoinOrder")));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         assertEquals(noOfOrderInstructions, queryOutput.getRecords().size());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSecurityJoinForJoinedTableFromImplicitlyJoinedTable() throws QException
   {
      /////////////////////////////////////////////////////////////////////////////////////////
      // in this test:                                                                       //
      // query on Order, joined with OrderLine.                                              //
      // Order has its own security field (storeId), that's always worked fine.              //
      // We want to change OrderLine's security field to be item.storeId - not order.storeId //
      // so that item has to be brought into the query to secure the items.                  //
      // this was originally broken, as it would generate a WHERE clause for item.storeId,   //
      // but it wouldn't put item in the FROM cluase.
      /////////////////////////////////////////////////////////////////////////////////////////
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 2));
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_ORDER_LINE)
         .setRecordSecurityLocks(ListBuilder.of(
            new RecordSecurityLock()
               .withSecurityKeyType(TestUtils.TABLE_NAME_STORE)
               .withFieldName("item.storeId")
               .withJoinNameChain(List.of("orderLineJoinItem"))));

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      queryInput.withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_ORDER_LINE).withSelect(true));
      queryInput.withFilter(new QQueryFilter(new QFilterCriteria(TestUtils.TABLE_NAME_ORDER_LINE + ".sku", QCriteriaOperator.IS_NOT_BLANK)));
      QueryOutput   queryOutput = new QueryAction().execute(queryInput);
      List<QRecord> records     = queryOutput.getRecords();
      assertEquals(3, records.size(), "expected no of records");

      ///////////////////////////////////////////////////////////////////////
      // we should get the orderLines for orders 4 and 5 - but not the one //
      // for order 2, as it has an item from a different store             //
      ///////////////////////////////////////////////////////////////////////
      assertThat(records).allMatch(r -> r.getValueInteger("id").equals(4) || r.getValueInteger("id").equals(5));
   }



   /*******************************************************************************
    ** We had, at one time, a bug where, for tables with 2 joins between each other,
    ** an ON clause could get written using the wrong table name in one part.
    **
    ** With that bug, this QueryAction.execute would throw an SQL Exception.
    **
    ** So this test, just makes sure that no such exception gets thrown.
    *******************************************************************************/
   @Test
   void testFlippedJoinForOnClause() throws QException
   {
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 1));

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_ORDER_INSTRUCTIONS);
      queryInput.withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_ORDER));
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertFalse(queryOutput.getRecords().isEmpty());

      ////////////////////////////////////
      // if no exception, then we pass. //
      ////////////////////////////////////
   }



   /*******************************************************************************
    ** Addressing a regression where a table was brought into a query for its
    ** security field, but it was a write-scope lock, so, it shouldn't have been.
    *******************************************************************************/
   @Test
   void testWriteLockOnJoinTableDoesntLimitQuery() throws Exception
   {
      ///////////////////////////////////////////////////////////////////////
      // add a security key type for "idNumber"                            //
      // then set up the person table with a read-write lock on that field //
      ///////////////////////////////////////////////////////////////////////
      QContext.getQInstance().addSecurityKeyType(new QSecurityKeyType().withName("idNumber"));
      QTableMetaData personTable = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON);
      personTable.withRecordSecurityLock(new RecordSecurityLock()
         .withLockScope(RecordSecurityLock.LockScope.READ_AND_WRITE)
         .withSecurityKeyType("idNumber")
         .withFieldName(TestUtils.TABLE_NAME_PERSONAL_ID_CARD + ".idNumber")
         .withJoinNameChain(List.of(QJoinMetaData.makeInferredJoinName(TestUtils.TABLE_NAME_PERSON, TestUtils.TABLE_NAME_PERSONAL_ID_CARD))));

      /////////////////////////////////////////////////////////////////////////////////////////
      // first, with no idNumber security key in session, query on person should find 0 rows //
      /////////////////////////////////////////////////////////////////////////////////////////
      assertEquals(0, new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON)).getRecords().size());

      ///////////////////////////////////////////////////////////////////
      // put an idNumber in the session - query and find just that one //
      ///////////////////////////////////////////////////////////////////
      QContext.setQSession(new QSession().withSecurityKeyValue("idNumber", "19800531"));
      assertEquals(1, new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON)).getRecords().size());

      //////////////////////////////////////////////////////////////////////////////////////////////
      // change the lock to be scope=WRITE - and now, we should be able to see all of the records //
      //////////////////////////////////////////////////////////////////////////////////////////////
      personTable.getRecordSecurityLocks().get(0).setLockScope(RecordSecurityLock.LockScope.WRITE);
      assertEquals(5, new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON)).getRecords().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFieldNamesToInclude() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_PERSONAL_ID_CARD).withSelect(true));
      queryInput.withFieldNamesToInclude(Set.of("firstName", TestUtils.TABLE_NAME_PERSONAL_ID_CARD + ".idNumber"));
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertThat(queryOutput.getRecords()).allMatch(r -> r.getValues().containsKey("firstName"));
      assertThat(queryOutput.getRecords()).allMatch(r -> r.getValues().containsKey(TestUtils.TABLE_NAME_PERSONAL_ID_CARD + ".idNumber"));
      assertThat(queryOutput.getRecords()).allMatch(r -> r.getValues().size() == 2);

      ////////////////////////////////////////////////////////////////////////////////////////////////////
      // re-run w/ null fieldNamesToInclude -- and should still see those 2, and more (values size > 2) //
      ////////////////////////////////////////////////////////////////////////////////////////////////////
      queryInput.withFieldNamesToInclude(null);
      queryOutput = new QueryAction().execute(queryInput);
      assertThat(queryOutput.getRecords()).allMatch(r -> r.getValues().containsKey("firstName"));
      assertThat(queryOutput.getRecords()).allMatch(r -> r.getValues().containsKey(TestUtils.TABLE_NAME_PERSONAL_ID_CARD + ".idNumber"));
      assertThat(queryOutput.getRecords()).allMatch(r -> r.getValues().size() > 2);

      ////////////////////////////////////////////////////////////////////////////
      // regression from original build - where only join fields made sql error //
      ////////////////////////////////////////////////////////////////////////////
      queryInput.withFieldNamesToInclude(Set.of(TestUtils.TABLE_NAME_PERSONAL_ID_CARD + ".idNumber"));
      queryOutput = new QueryAction().execute(queryInput);
      assertThat(queryOutput.getRecords()).allMatch(r -> r.getValues().containsKey(TestUtils.TABLE_NAME_PERSONAL_ID_CARD + ".idNumber"));
      assertThat(queryOutput.getRecords()).allMatch(r -> r.getValues().size() == 1);

      //////////////////////////////////////////////////////////////////////////////////////////
      // similar regression to above, if one of the join tables didn't have anything selected //
      //////////////////////////////////////////////////////////////////////////////////////////
      queryInput.withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_PERSONAL_ID_CARD).withAlias("id2").withSelect(true));
      queryInput.withFieldNamesToInclude(Set.of("firstName", "id2.idNumber"));
      queryOutput = new QueryAction().execute(queryInput);
      assertThat(queryOutput.getRecords()).allMatch(r -> r.getValues().containsKey("firstName"));
      assertThat(queryOutput.getRecords()).allMatch(r -> r.getValues().containsKey("id2.idNumber"));
      assertThat(queryOutput.getRecords()).allMatch(r -> r.getValues().size() == 2);
   }



   /*******************************************************************************
    ** Exercises a 2-hop security lock chain through the RDBMS backend.
    **
    ** The lineItemExtrinsic table has a security lock on order.storeId via
    ** joinNameChain [orderJoinOrderLine, orderLineJoinLineItemExtrinsic].
    ** This means querying lineItemExtrinsic requires joining through orderLine
    ** to order to evaluate the security lock.
    **
    ** This test verifies that the generated SQL correctly handles the 2-hop
    ** chain — specifically that fillInMissingJoinMetaData does not double-flip
    ** the second hop's join metadata, which would produce an invalid ON clause.
    *******************************************************************************/
   @Test
   void testTwoHopSecurityLockChainOnLineItemExtrinsic() throws QException
   {
      //////////////////////////////////////////////////////////////////////////////////////
      // insert lineItemExtrinsic rows linked to order_line rows from different stores.   //
      // order_line id=1 is on order 1 (store 1), order_line id=6 is on order 4 (store 2) //
      //////////////////////////////////////////////////////////////////////////////////////
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_STORE_ALL_ACCESS, true));
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC).withRecords(List.of(
         new QRecord().withValue("orderLineId", 1).withValue("key", "color").withValue("value", "red"),
         new QRecord().withValue("orderLineId", 1).withValue("key", "size").withValue("value", "large"),
         new QRecord().withValue("orderLineId", 6).withValue("key", "color").withValue("value", "blue")
      )));

      ////////////////////////////////////////////
      // with all-access, should see all 3 rows //
      ////////////////////////////////////////////
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC);
      List<QRecord> allRecords = new QueryAction().execute(queryInput).getRecords();
      assertEquals(3, allRecords.size(), "All-access should see all 3 lineItemExtrinsic rows");

      ///////////////////////////////////////////////////////////////////////////////
      // with store=1, should see the 2 rows linked through order_line 1 → order 1 //
      ///////////////////////////////////////////////////////////////////////////////
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 1));
      queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC);
      List<QRecord> store1Records = new QueryAction().execute(queryInput).getRecords();
      assertEquals(2, store1Records.size(), "Store 1 should see 2 lineItemExtrinsic rows");
      assertThat(store1Records).allMatch(r -> r.getValueInteger("orderLineId").equals(1));

      ///////////////////////////////////////////////////////////////////////////////
      // with store=2, should see the 1 row linked through order_line 6 → order 4 //
      ///////////////////////////////////////////////////////////////////////////////
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 2));
      queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC);
      List<QRecord> store2Records = new QueryAction().execute(queryInput).getRecords();
      assertEquals(1, store2Records.size(), "Store 2 should see 1 lineItemExtrinsic row");
      assertThat(store2Records).allMatch(r -> r.getValueInteger("orderLineId").equals(6));

      //////////////////////////////////////////////////
      // with store=5 (no orders), should see nothing //
      //////////////////////////////////////////////////
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 5));
      queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC);
      List<QRecord> noRecords = new QueryAction().execute(queryInput).getRecords();
      assertEquals(0, noRecords.size(), "Store 5 should see no lineItemExtrinsic rows");

      /////////////////////////////////////////////////////
      // with no security key at all, should see nothing //
      /////////////////////////////////////////////////////
      QContext.setQSession(new QSession());
      queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC);
      List<QRecord> emptySessionRecords = new QueryAction().execute(queryInput).getRecords();
      assertEquals(0, emptySessionRecords.size(), "Empty session should see no lineItemExtrinsic rows");
   }

}
