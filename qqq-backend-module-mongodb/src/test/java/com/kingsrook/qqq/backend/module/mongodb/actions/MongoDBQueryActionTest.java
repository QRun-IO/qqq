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

package com.kingsrook.qqq.backend.module.mongodb.actions;


import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.Now;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.NowWithOffset;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.mongodb.BaseTest;
import com.kingsrook.qqq.backend.module.mongodb.TestUtils;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for MongoDBQueryAction 
 *******************************************************************************/
class MongoDBQueryActionTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws QException
   {
      primeTestDatabase();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected void primeTestDatabase() throws QException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      insertInput.setRecords(List.of(
         new QRecord().withValue("seqNo", 1).withValue("firstName", "Darin").withValue("lastName", "Kelkhoff").withValue("birthDate", LocalDate.parse("1980-05-31")).withValue("email", "darin.kelkhoff@gmail.com").withValue("isEmployed", true).withValue("annualSalary", 25000).withValue("daysWorked", 27).withValue("homeTown", "Chester"),
         new QRecord().withValue("seqNo", 2).withValue("firstName", "James").withValue("lastName", "Maes").withValue("birthDate", LocalDate.parse("1980-05-15")).withValue("email", "jmaes@mmltholdings.com").withValue("isEmployed", true).withValue("annualSalary", 26000).withValue("daysWorked", 124).withValue("homeTown", "Chester"),
         new QRecord().withValue("seqNo", 3).withValue("firstName", "Tim").withValue("lastName", "Chamberlain").withValue("birthDate", LocalDate.parse("1976-05-28")).withValue("email", "tchamberlain@mmltholdings.com").withValue("isEmployed", false).withValue("annualSalary", null).withValue("daysWorked", 0).withValue("homeTown", "Decatur"),
         new QRecord().withValue("seqNo", 4).withValue("firstName", "Tyler").withValue("lastName", "Samples").withValue("birthDate", null).withValue("email", "tsamples@mmltholdings.com").withValue("isEmployed", true).withValue("annualSalary", 30000).withValue("daysWorked", 99).withValue("homeTown", "Texas"),
         new QRecord().withValue("seqNo", 5).withValue("firstName", "Garret").withValue("lastName", "Richardson").withValue("birthDate", LocalDate.parse("1981-01-01")).withValue("email", "grichardson@mmltholdings.com").withValue("isEmployed", true).withValue("annualSalary", 1000000).withValue("daysWorked", 232).withValue("homeTown", null)
      ));
      InsertOutput insertOutput = new InsertAction().execute(insertInput);

      MongoDatabase database = getMongoClient().getDatabase(TestUtils.MONGO_DATABASE);

      MongoCollection<Document> storeCollection = database.getCollection(TestUtils.TABLE_NAME_STORE);
      storeCollection.insertMany(List.of(
         Document.parse("""
            {"key":1, "name": "Q-Mart"}"""),
         Document.parse("""
            {"key":2, "name": "QQQ 'R' Us"}"""),
         Document.parse("""
            {"key":3, "name": "QDepot"}""")
      ));

      MongoCollection<Document> orderCollection = database.getCollection(TestUtils.TABLE_NAME_ORDER);
      orderCollection.insertMany(List.of(
         Document.parse("""
            {"key": 1, "storeKey":1, "billToPersonId": 1, "shipToPersonId": 1}}"""),
         Document.parse("""
            {"key": 2, "storeKey":1, "billToPersonId": 1, "shipToPersonId": 2}}"""),
         Document.parse("""
            {"key": 3, "storeKey":1, "billToPersonId": 2, "shipToPersonId": 3}}"""),
         Document.parse("""
            {"key": 4, "storeKey":2, "billToPersonId": 4, "shipToPersonId": 5}}"""),
         Document.parse("""
            {"key": 5, "storeKey":2, "billToPersonId": 5, "shipToPersonId": 4}}"""),
         Document.parse("""
            {"key": 6, "storeKey":3, "billToPersonId": 5, "shipToPersonId": null}}"""),
         Document.parse("""
            {"key": 7, "storeKey":3, "billToPersonId": null, "shipToPersonId": 5}"""),
         Document.parse("""
            {"key": 8, "storeKey":3, "billToPersonId": null, "shipToPersonId": 5}""")
      ));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      //////////////////////////////////////////////////////////
      // let's not use the primed-database rows for this test //
      //////////////////////////////////////////////////////////
      clearDatabase();

      ////////////////////////////////////////
      // directly insert some mongo records //
      ////////////////////////////////////////
      MongoDatabase             database   = getMongoClient().getDatabase(TestUtils.MONGO_DATABASE);
      MongoCollection<Document> collection = database.getCollection(TestUtils.TABLE_NAME_PERSON);
      collection.insertMany(List.of(
         Document.parse("""
            {  "metaData": {"createDate": "2023-01-09T01:01:01.123Z", "modifyDate": "2023-01-09T02:02:02.123Z", "oops": "All Crunchberries"},
               "firstName": "Darin",
               "lastName": "Kelkhoff",
               "unmappedField": 1701,
               "unmappedList": [1,2,3],
               "unmappedObject": {
                  "A": "B",
                  "One": 2,
                  "subSub": {
                     "so": true
                  }
               }
            }"""),
         Document.parse("""
            {"metaData": {"createDate": "2023-01-09T03:03:03.123Z", "modifyDate": "2023-01-09T04:04:04.123Z"}, "firstName": "Tylers", "lastName": "Sample"}""")
      ));

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      QueryOutput queryOutput = new QueryAction().execute(queryInput);

      assertEquals(2, queryOutput.getRecords().size());

      QRecord record = queryOutput.getRecords().get(0);
      assertEquals(Instant.parse("2023-01-09T01:01:01.123Z"), record.getValueInstant("createDate"));
      assertEquals(Instant.parse("2023-01-09T02:02:02.123Z"), record.getValueInstant("modifyDate"));
      assertThat(record.getValue("id")).isInstanceOf(String.class);
      assertEquals("Darin", record.getValueString("firstName"));
      assertEquals("Kelkhoff", record.getValueString("lastName"));

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // test that un-mapped (or un-structured) fields come through, with their shape as they exist in the mongo record //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      assertEquals(1701, record.getValueInteger("unmappedField"));
      assertEquals(List.of(1, 2, 3), record.getValue("unmappedList"));
      assertEquals(Map.of("A", "B", "One", 2, "subSub", Map.of("so", true)), record.getValue("unmappedObject"));
      assertEquals(Map.of("oops", "All Crunchberries"), record.getValue("metaData"));

      record = queryOutput.getRecords().get(1);
      assertEquals(Instant.parse("2023-01-09T03:03:03.123Z"), record.getValueInstant("createDate"));
      assertEquals(Instant.parse("2023-01-09T04:04:04.123Z"), record.getValueInstant("modifyDate"));
      assertEquals("Tylers", record.getValueString("firstName"));
      assertEquals("Sample", record.getValueString("lastName"));
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
   public void testUnfilteredQuery() throws QException
   {
      QueryInput  queryInput  = initQueryRequest();
      QueryOutput queryOutput = new MongoDBQueryAction().execute(queryInput);
      assertEquals(5, queryOutput.getRecords().size(), "Unfiltered query should find all rows");
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testTrueQuery() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter(new QFilterCriteria("email", QCriteriaOperator.TRUE)));
      QueryOutput queryOutput = new MongoDBQueryAction().execute(queryInput);
      assertEquals(5, queryOutput.getRecords().size(), "'TRUE' query should find all rows");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testFalseQuery() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter(new QFilterCriteria("email", QCriteriaOperator.FALSE)));
      QueryOutput queryOutput = new MongoDBQueryAction().execute(queryInput);
      assertEquals(0, queryOutput.getRecords().size(), "'FALSE' query should find no rows");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testEqualsQuery() throws QException
   {
      String email = "darin.kelkhoff@gmail.com";

      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria()
            .withFieldName("email")
            .withOperator(QCriteriaOperator.EQUALS)
            .withValues(List.of(email)))
      );
      QueryOutput queryOutput = new MongoDBQueryAction().execute(queryInput);
      assertEquals(1, queryOutput.getRecords().size(), "Expected # of rows");
      assertEquals(email, queryOutput.getRecords().get(0).getValueString("email"), "Should find expected email address");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testNotEqualsQuery() throws QException
   {
      String email = "darin.kelkhoff@gmail.com";

      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria()
            .withFieldName("email")
            .withOperator(QCriteriaOperator.NOT_EQUALS)
            .withValues(List.of(email)))
      );
      QueryOutput queryOutput = new MongoDBQueryAction().execute(queryInput);
      assertEquals(4, queryOutput.getRecords().size(), "Expected # of rows");
      Assertions.assertTrue(queryOutput.getRecords().stream().noneMatch(r -> r.getValueString("email").equals(email)), "Should NOT find expected email address");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testNotEqualsOrIsNullQuery() throws QException
   {
      /////////////////////////////////////////////////////////////////////////////
      // 5 rows, 1 has a null salary, 1 has 1,000,000.                           //
      // first confirm that query for != returns 3 (the null does NOT come back) //
      // then, confirm that != or is null gives the (more humanly expected) 4.   //
      /////////////////////////////////////////////////////////////////////////////
      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria()
            .withFieldName("annualSalary")
            .withOperator(QCriteriaOperator.NOT_EQUALS)
            .withValues(List.of(1_000_000))));
      QueryOutput queryOutput = new MongoDBQueryAction().execute(queryInput);
      assertEquals(3, queryOutput.getRecords().size(), "Expected # of rows");

      queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria()
            .withFieldName("annualSalary")
            .withOperator(QCriteriaOperator.NOT_EQUALS_OR_IS_NULL)
            .withValues(List.of(1_000_000))));
      queryOutput = new MongoDBQueryAction().execute(queryInput);
      assertEquals(4, queryOutput.getRecords().size(), "Expected # of rows");
      Assertions.assertTrue(queryOutput.getRecords().stream().noneMatch(r -> Objects.equals(1_000_000, r.getValueInteger("annualSalary"))), "Should NOT find expected salary");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testInQuery() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria()
            .withFieldName("seqNo")
            .withOperator(QCriteriaOperator.IN)
            .withValues(List.of(2, 4)))
      );
      QueryOutput queryOutput = new MongoDBQueryAction().execute(queryInput);
      assertEquals(2, queryOutput.getRecords().size(), "Expected # of rows");
      Assertions.assertTrue(queryOutput.getRecords().stream().allMatch(r -> r.getValueInteger("seqNo").equals(2) || r.getValueInteger("seqNo").equals(4)), "Should find expected ids");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testNotInQuery() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria()
            .withFieldName("seqNo")
            .withOperator(QCriteriaOperator.NOT_IN)
            .withValues(List.of(2, 3, 4)))
      );
      QueryOutput queryOutput = new MongoDBQueryAction().execute(queryInput);
      assertEquals(2, queryOutput.getRecords().size(), "Expected # of rows");
      Assertions.assertTrue(queryOutput.getRecords().stream().allMatch(r -> r.getValueInteger("seqNo").equals(1) || r.getValueInteger("seqNo").equals(5)), "Should find expected ids");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testStartsWith() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria()
            .withFieldName("email")
            .withOperator(QCriteriaOperator.STARTS_WITH)
            .withValues(List.of("darin")))
      );
      QueryOutput queryOutput = new MongoDBQueryAction().execute(queryInput);
      assertEquals(1, queryOutput.getRecords().size(), "Expected # of rows");
      Assertions.assertTrue(queryOutput.getRecords().stream().allMatch(r -> r.getValueString("email").matches("darin.*")), "Should find matching email address");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testContains() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria()
            .withFieldName("email")
            .withOperator(QCriteriaOperator.CONTAINS)
            .withValues(List.of("kelkhoff")))
      );
      QueryOutput queryOutput = new MongoDBQueryAction().execute(queryInput);
      assertEquals(1, queryOutput.getRecords().size(), "Expected # of rows");
      Assertions.assertTrue(queryOutput.getRecords().stream().allMatch(r -> r.getValueString("email").matches(".*kelkhoff.*")), "Should find matching email address");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testLike() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria()
            .withFieldName("email")
            .withOperator(QCriteriaOperator.LIKE)
            .withValues(List.of("%kelk%")))
      );
      QueryOutput queryOutput = new MongoDBQueryAction().execute(queryInput);
      assertEquals(1, queryOutput.getRecords().size(), "Expected # of rows");
      Assertions.assertTrue(queryOutput.getRecords().stream().allMatch(r -> r.getValueString("email").matches(".*kelkhoff.*")), "Should find matching email address");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testNotLike() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria()
            .withFieldName("email")
            .withOperator(QCriteriaOperator.NOT_LIKE)
            .withValues(List.of("%kelk%")))
      );
      QueryOutput queryOutput = new MongoDBQueryAction().execute(queryInput);
      assertEquals(4, queryOutput.getRecords().size(), "Expected # of rows");
      Assertions.assertTrue(queryOutput.getRecords().stream().noneMatch(r -> r.getValueString("email").matches(".*kelkhoff.*")), "Should find matching email address");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testEndsWith() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria()
            .withFieldName("email")
            .withOperator(QCriteriaOperator.ENDS_WITH)
            .withValues(List.of("gmail.com")))
      );
      QueryOutput queryOutput = new MongoDBQueryAction().execute(queryInput);
      assertEquals(1, queryOutput.getRecords().size(), "Expected # of rows");
      Assertions.assertTrue(queryOutput.getRecords().stream().allMatch(r -> r.getValueString("email").matches(".*gmail.com")), "Should find matching email address");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testNotStartsWith() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria()
            .withFieldName("email")
            .withOperator(QCriteriaOperator.NOT_STARTS_WITH)
            .withValues(List.of("darin")))
      );
      QueryOutput queryOutput = new MongoDBQueryAction().execute(queryInput);
      assertEquals(4, queryOutput.getRecords().size(), "Expected # of rows");
      Assertions.assertTrue(queryOutput.getRecords().stream().noneMatch(r -> r.getValueString("email").matches("darin.*")), "Should find matching email address");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testNotContains() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria()
            .withFieldName("email")
            .withOperator(QCriteriaOperator.NOT_CONTAINS)
            .withValues(List.of("kelkhoff")))
      );
      QueryOutput queryOutput = new MongoDBQueryAction().execute(queryInput);
      assertEquals(4, queryOutput.getRecords().size(), "Expected # of rows");
      Assertions.assertTrue(queryOutput.getRecords().stream().noneMatch(r -> r.getValueString("email").matches(".*kelkhoff.*")), "Should find matching email address");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testNotEndsWith() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria()
            .withFieldName("email")
            .withOperator(QCriteriaOperator.NOT_ENDS_WITH)
            .withValues(List.of("gmail.com")))
      );
      QueryOutput queryOutput = new MongoDBQueryAction().execute(queryInput);
      assertEquals(4, queryOutput.getRecords().size(), "Expected # of rows");
      Assertions.assertTrue(queryOutput.getRecords().stream().noneMatch(r -> r.getValueString("email").matches(".*gmail.com")), "Should find matching email address");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testLessThanQuery() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria()
            .withFieldName("seqNo")
            .withOperator(QCriteriaOperator.LESS_THAN)
            .withValues(List.of(3)))
      );
      QueryOutput queryOutput = new MongoDBQueryAction().execute(queryInput);
      assertEquals(2, queryOutput.getRecords().size(), "Expected # of rows");
      Assertions.assertTrue(queryOutput.getRecords().stream().allMatch(r -> r.getValueInteger("seqNo").equals(1) || r.getValueInteger("seqNo").equals(2)), "Should find expected ids");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testLessThanOrEqualsQuery() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria()
            .withFieldName("seqNo")
            .withOperator(QCriteriaOperator.LESS_THAN_OR_EQUALS)
            .withValues(List.of(2)))
      );
      QueryOutput queryOutput = new MongoDBQueryAction().execute(queryInput);
      assertEquals(2, queryOutput.getRecords().size(), "Expected # of rows");
      Assertions.assertTrue(queryOutput.getRecords().stream().allMatch(r -> r.getValueInteger("seqNo").equals(1) || r.getValueInteger("seqNo").equals(2)), "Should find expected ids");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testGreaterThanQuery() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria()
            .withFieldName("seqNo")
            .withOperator(QCriteriaOperator.GREATER_THAN)
            .withValues(List.of(3)))
      );
      QueryOutput queryOutput = new MongoDBQueryAction().execute(queryInput);
      assertEquals(2, queryOutput.getRecords().size(), "Expected # of rows");
      Assertions.assertTrue(queryOutput.getRecords().stream().allMatch(r -> r.getValueInteger("seqNo").equals(4) || r.getValueInteger("seqNo").equals(5)), "Should find expected ids");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testGreaterThanOrEqualsQuery() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria()
            .withFieldName("seqNo")
            .withOperator(QCriteriaOperator.GREATER_THAN_OR_EQUALS)
            .withValues(List.of(4)))
      );
      QueryOutput queryOutput = new MongoDBQueryAction().execute(queryInput);
      assertEquals(2, queryOutput.getRecords().size(), "Expected # of rows");
      Assertions.assertTrue(queryOutput.getRecords().stream().allMatch(r -> r.getValueInteger("seqNo").equals(4) || r.getValueInteger("seqNo").equals(5)), "Should find expected ids");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testIsBlankQuery() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria()
            .withFieldName("birthDate")
            .withOperator(QCriteriaOperator.IS_BLANK)
         ));
      QueryOutput queryOutput = new MongoDBQueryAction().execute(queryInput);
      assertEquals(1, queryOutput.getRecords().size(), "Expected # of rows");
      Assertions.assertTrue(queryOutput.getRecords().stream().allMatch(r -> r.getValue("birthDate") == null), "Should find expected row");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testIsNotBlankQuery() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria()
            .withFieldName("firstName")
            .withOperator(QCriteriaOperator.IS_NOT_BLANK)
         ));
      QueryOutput queryOutput = new MongoDBQueryAction().execute(queryInput);
      assertEquals(5, queryOutput.getRecords().size(), "Expected # of rows");
      Assertions.assertTrue(queryOutput.getRecords().stream().allMatch(r -> r.getValue("firstName") != null), "Should find expected rows");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testBetweenQuery() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria()
            .withFieldName("seqNo")
            .withOperator(QCriteriaOperator.BETWEEN)
            .withValues(List.of(2, 4))
         ));
      QueryOutput queryOutput = new MongoDBQueryAction().execute(queryInput);
      assertEquals(3, queryOutput.getRecords().size(), "Expected # of rows");
      Assertions.assertTrue(queryOutput.getRecords().stream().allMatch(r -> r.getValueInteger("seqNo").equals(2) || r.getValueInteger("seqNo").equals(3) || r.getValueInteger("seqNo").equals(4)), "Should find expected ids");
   }



   /*******************************************************************************
    **
    * [
    *    And Filter
    *    {
    *       filters=
    *       [
    *          Not Filter
    *          {
    *             filter=And Filter
    *             {
    *                filters=
    *                [
    *                   Operator Filter
    *                   {
    *                      fieldName='seqNo', operator='$gte', value=2
    *                   },
    *                   Operator Filter
    *                   {
    *                      fieldName='seqNo', operator='$lte', value=4
    *                   }
    *                ]
    *             }
    *          }
    *       ]
    *    }
    * ]
    *******************************************************************************/
   @Test
   public void testNotBetweenQuery() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria()
            .withFieldName("seqNo")
            .withOperator(QCriteriaOperator.NOT_BETWEEN)
            .withValues(List.of(2, 4))
         ));
      QueryOutput queryOutput = new MongoDBQueryAction().execute(queryInput);
      assertEquals(2, queryOutput.getRecords().size(), "Expected # of rows");
      Assertions.assertTrue(queryOutput.getRecords().stream().allMatch(r -> r.getValueInteger("seqNo").equals(1) || r.getValueInteger("seqNo").equals(5)), "Should find expected ids");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testFilterExpressions() throws QException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      insertInput.setRecords(List.of(
         new QRecord().withValue("email", "-").withValue("firstName", "past").withValue("lastName", "ExpressionTest").withValue("birthDate", Instant.now().minus(3, ChronoUnit.DAYS)),
         new QRecord().withValue("email", "-").withValue("firstName", "future").withValue("lastName", "ExpressionTest").withValue("birthDate", Instant.now().plus(3, ChronoUnit.DAYS))
      ));
      new InsertAction().execute(insertInput);

      {
         QueryInput queryInput = initQueryRequest();
         queryInput.setFilter(new QQueryFilter()
            .withCriteria(new QFilterCriteria().withFieldName("lastName").withOperator(QCriteriaOperator.EQUALS).withValues(List.of("ExpressionTest")))
            .withCriteria(new QFilterCriteria().withFieldName("birthDate").withOperator(QCriteriaOperator.LESS_THAN).withValues(List.of(new Now()))));
         QueryOutput queryOutput = new MongoDBQueryAction().execute(queryInput);
         assertEquals(1, queryOutput.getRecords().size(), "Expected # of rows");
         Assertions.assertTrue(queryOutput.getRecords().stream().anyMatch(r -> r.getValue("firstName").equals("past")), "Should find expected row");
      }

      {
         QueryInput queryInput = initQueryRequest();
         queryInput.setFilter(new QQueryFilter()
            .withCriteria(new QFilterCriteria().withFieldName("lastName").withOperator(QCriteriaOperator.EQUALS).withValues(List.of("ExpressionTest")))
            .withCriteria(new QFilterCriteria().withFieldName("birthDate").withOperator(QCriteriaOperator.LESS_THAN).withValues(List.of(NowWithOffset.plus(2, ChronoUnit.DAYS)))));
         QueryOutput queryOutput = new MongoDBQueryAction().execute(queryInput);
         assertEquals(1, queryOutput.getRecords().size(), "Expected # of rows");
         Assertions.assertTrue(queryOutput.getRecords().stream().anyMatch(r -> r.getValue("firstName").equals("past")), "Should find expected row");
      }

      {
         QueryInput queryInput = initQueryRequest();
         queryInput.setFilter(new QQueryFilter()
            .withCriteria(new QFilterCriteria().withFieldName("lastName").withOperator(QCriteriaOperator.EQUALS).withValues(List.of("ExpressionTest")))
            .withCriteria(new QFilterCriteria().withFieldName("birthDate").withOperator(QCriteriaOperator.GREATER_THAN).withValues(List.of(NowWithOffset.minus(5, ChronoUnit.DAYS)))));
         QueryOutput queryOutput = new MongoDBQueryAction().execute(queryInput);
         assertEquals(2, queryOutput.getRecords().size(), "Expected # of rows");
         Assertions.assertTrue(queryOutput.getRecords().stream().anyMatch(r -> r.getValue("firstName").equals("past")), "Should find expected row");
         Assertions.assertTrue(queryOutput.getRecords().stream().anyMatch(r -> r.getValue("firstName").equals("future")), "Should find expected row");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testEmptyInList() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter().withCriteria(new QFilterCriteria("firstName", QCriteriaOperator.IN, List.of())));
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertEquals(0, queryOutput.getRecords().size(), "IN empty list should find nothing.");

      queryInput.setFilter(new QQueryFilter().withCriteria(new QFilterCriteria("firstName", QCriteriaOperator.NOT_IN, List.of())));
      queryOutput = new QueryAction().execute(queryInput);
      assertEquals(5, queryOutput.getRecords().size(), "NOT_IN empty list should find everything.");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOr() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withBooleanOperator(QQueryFilter.BooleanOperator.OR)
         .withCriteria(new QFilterCriteria("firstName", QCriteriaOperator.EQUALS, List.of("Darin")))
         .withCriteria(new QFilterCriteria("firstName", QCriteriaOperator.EQUALS, List.of("Tim")))
      );
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertEquals(2, queryOutput.getRecords().size(), "OR should find 2 rows");
      assertThat(queryOutput.getRecords()).anyMatch(r -> r.getValueString("firstName").equals("Darin"));
      assertThat(queryOutput.getRecords()).anyMatch(r -> r.getValueString("firstName").equals("Tim"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNestedFilterAndOrOr() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withBooleanOperator(QQueryFilter.BooleanOperator.OR)
         .withSubFilters(List.of(
            new QQueryFilter()
               .withBooleanOperator(QQueryFilter.BooleanOperator.AND)
               .withCriteria(new QFilterCriteria("firstName", QCriteriaOperator.EQUALS, List.of("James")))
               .withCriteria(new QFilterCriteria("lastName", QCriteriaOperator.EQUALS, List.of("Maes"))),
            new QQueryFilter()
               .withBooleanOperator(QQueryFilter.BooleanOperator.AND)
               .withCriteria(new QFilterCriteria("firstName", QCriteriaOperator.EQUALS, List.of("Darin")))
               .withCriteria(new QFilterCriteria("lastName", QCriteriaOperator.EQUALS, List.of("Kelkhoff")))
         ))
      );
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertEquals(2, queryOutput.getRecords().size(), "Complex query should find 2 rows");
      assertThat(queryOutput.getRecords()).anyMatch(r -> r.getValueString("firstName").equals("James") && r.getValueString("lastName").equals("Maes"));
      assertThat(queryOutput.getRecords()).anyMatch(r -> r.getValueString("firstName").equals("Darin") && r.getValueString("lastName").equals("Kelkhoff"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNestedFilterOrAndAnd() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withBooleanOperator(QQueryFilter.BooleanOperator.AND)
         .withSubFilters(List.of(
            new QQueryFilter()
               .withBooleanOperator(QQueryFilter.BooleanOperator.OR)
               .withCriteria(new QFilterCriteria("firstName", QCriteriaOperator.EQUALS, List.of("James")))
               .withCriteria(new QFilterCriteria("firstName", QCriteriaOperator.EQUALS, List.of("Tim"))),
            new QQueryFilter()
               .withBooleanOperator(QQueryFilter.BooleanOperator.OR)
               .withCriteria(new QFilterCriteria("lastName", QCriteriaOperator.EQUALS, List.of("Kelkhoff")))
               .withCriteria(new QFilterCriteria("lastName", QCriteriaOperator.EQUALS, List.of("Chamberlain")))
         ))
      );
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertEquals(1, queryOutput.getRecords().size(), "Complex query should find 1 row");
      assertThat(queryOutput.getRecords()).anyMatch(r -> r.getValueString("firstName").equals("Tim") && r.getValueString("lastName").equals("Chamberlain"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNestedFilterAndTopLevelFilter() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("seqNo", QCriteriaOperator.EQUALS, 3))
         .withBooleanOperator(QQueryFilter.BooleanOperator.AND)
         .withSubFilters(List.of(
            new QQueryFilter()
               .withBooleanOperator(QQueryFilter.BooleanOperator.OR)
               .withCriteria(new QFilterCriteria("firstName", QCriteriaOperator.EQUALS, List.of("James")))
               .withCriteria(new QFilterCriteria("firstName", QCriteriaOperator.EQUALS, List.of("Tim"))),
            new QQueryFilter()
               .withBooleanOperator(QQueryFilter.BooleanOperator.OR)
               .withCriteria(new QFilterCriteria("lastName", QCriteriaOperator.EQUALS, List.of("Kelkhoff")))
               .withCriteria(new QFilterCriteria("lastName", QCriteriaOperator.EQUALS, List.of("Chamberlain")))
         ))
      );
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertEquals(1, queryOutput.getRecords().size(), "Complex query should find 1 row");
      assertThat(queryOutput.getRecords()).anyMatch(r -> r.getValueInteger("seqNo").equals(3) && r.getValueString("firstName").equals("Tim") && r.getValueString("lastName").equals("Chamberlain"));

      queryInput.getFilter().setCriteria(List.of(new QFilterCriteria("seqNo", QCriteriaOperator.NOT_EQUALS, 3)));
      queryOutput = new QueryAction().execute(queryInput);
      assertEquals(0, queryOutput.getRecords().size(), "Next complex query should find 0 rows");
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
         .anyMatch(r -> r.getValueInteger("key").equals(1));

      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 2));
      assertThat(new QueryAction().execute(queryInput).getRecords())
         .hasSize(1)
         .anyMatch(r -> r.getValueInteger("key").equals(2));

      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 5));
      assertThat(new QueryAction().execute(queryInput).getRecords()).isEmpty();

      QContext.setQSession(new QSession());
      assertThat(new QueryAction().execute(queryInput).getRecords()).isEmpty();

      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, null));
      assertThat(new QueryAction().execute(queryInput).getRecords()).isEmpty();

      QContext.setQSession(new QSession().withSecurityKeyValues(Map.of(TestUtils.TABLE_NAME_STORE, Collections.emptyList())));
      assertThat(new QueryAction().execute(queryInput).getRecords()).isEmpty();

      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 1).withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 3));
      assertThat(new QueryAction().execute(queryInput).getRecords())
         .hasSize(2)
         .anyMatch(r -> r.getValueInteger("key").equals(1))
         .anyMatch(r -> r.getValueInteger("key").equals(3));
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
         .allMatch(r -> r.getValueInteger("storeKey").equals(1));

      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 2));
      assertThat(new QueryAction().execute(queryInput).getRecords())
         .hasSize(2)
         .allMatch(r -> r.getValueInteger("storeKey").equals(2));

      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 5));
      assertThat(new QueryAction().execute(queryInput).getRecords()).isEmpty();

      QContext.setQSession(new QSession());
      assertThat(new QueryAction().execute(queryInput).getRecords()).isEmpty();

      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, null));
      assertThat(new QueryAction().execute(queryInput).getRecords()).isEmpty();

      QContext.setQSession(new QSession().withSecurityKeyValues(Map.of(TestUtils.TABLE_NAME_STORE, Collections.emptyList())));
      assertThat(new QueryAction().execute(queryInput).getRecords()).isEmpty();

      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 1).withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 3));
      assertThat(new QueryAction().execute(queryInput).getRecords())
         .hasSize(6)
         .allMatch(r -> r.getValueInteger("storeKey").equals(1) || r.getValueInteger("storeKey").equals(3));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRecordSecurityWithFilters() throws QException
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_ORDER);

      queryInput.setFilter(new QQueryFilter(new QFilterCriteria("key", QCriteriaOperator.BETWEEN, List.of(2, 7))));
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_STORE_ALL_ACCESS, true));
      assertThat(new QueryAction().execute(queryInput).getRecords()).hasSize(6);

      queryInput.setFilter(new QQueryFilter(new QFilterCriteria("key", QCriteriaOperator.BETWEEN, List.of(2, 7))));
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 1));
      assertThat(new QueryAction().execute(queryInput).getRecords())
         .hasSize(2)
         .allMatch(r -> r.getValueInteger("storeKey").equals(1));

      queryInput.setFilter(new QQueryFilter(new QFilterCriteria("key", QCriteriaOperator.BETWEEN, List.of(2, 7))));
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 5));
      assertThat(new QueryAction().execute(queryInput).getRecords()).isEmpty();

      queryInput.setFilter(new QQueryFilter(new QFilterCriteria("key", QCriteriaOperator.BETWEEN, List.of(2, 7))));
      QContext.setQSession(new QSession());
      assertThat(new QueryAction().execute(queryInput).getRecords()).isEmpty();

      queryInput.setFilter(new QQueryFilter(new QFilterCriteria("storeKey", QCriteriaOperator.IN, List.of(1, 2))));
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 1).withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 3));
      assertThat(new QueryAction().execute(queryInput).getRecords())
         .hasSize(3)
         .allMatch(r -> r.getValueInteger("storeKey").equals(1));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFieldNamesToInclude() throws QException
   {
      QQueryFilter  filter     = new QQueryFilter().withCriteria("firstName", QCriteriaOperator.EQUALS, "Darin");
      QueryInput    queryInput = new QueryInput(TestUtils.TABLE_NAME_PERSON).withFilter(filter);

      QRecord record = new QueryAction().execute(queryInput.withFieldNamesToInclude(null)).getRecords().get(0);
      assertTrue(record.getValues().containsKey("id"));
      assertTrue(record.getValues().containsKey("firstName"));
      assertTrue(record.getValues().containsKey("createDate"));
      //////////////////////////////////////////////////////////////////////////////
      // note, we get an extra "metaData" field (??), which, i guess is expected? //
      //////////////////////////////////////////////////////////////////////////////
      assertEquals(QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON).getFields().size() + 1, record.getValues().size());

      record = new QueryAction().execute(queryInput.withFieldNamesToInclude(Set.of("id", "firstName"))).getRecords().get(0);
      assertTrue(record.getValues().containsKey("id"));
      assertTrue(record.getValues().containsKey("firstName"));
      assertFalse(record.getValues().containsKey("createDate"));
      assertEquals(2, record.getValues().size());
      //////////////////////////////////////////////////////////////////////////////////////////////
      // here, we'd have put _id (which mongo always returns) as "id", since caller requested it. //
      //////////////////////////////////////////////////////////////////////////////////////////////
      assertFalse(record.getValues().containsKey("_id"));

      record = new QueryAction().execute(queryInput.withFieldNamesToInclude(Set.of("homeTown"))).getRecords().get(0);
      assertFalse(record.getValues().containsKey("id"));
      assertFalse(record.getValues().containsKey("firstName"));
      assertFalse(record.getValues().containsKey("createDate"));
      assertTrue(record.getValues().containsKey("homeTown"));
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // here, mongo always gives back _id (but, we won't have re-mapped it to "id", since caller didn't request it), so, do expect 2 fields here //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      assertEquals(2, record.getValues().size());
      assertTrue(record.getValues().containsKey("_id"));
   }

}