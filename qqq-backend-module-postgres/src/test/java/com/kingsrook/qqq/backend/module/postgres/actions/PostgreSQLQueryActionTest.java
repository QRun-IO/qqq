/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.postgres.actions;


import java.io.Serializable;
import java.sql.Connection;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.Now;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.NowWithOffset;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.ThisOrLastPeriod;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeFunction;
import com.kingsrook.qqq.backend.module.postgres.BaseTest;
import com.kingsrook.qqq.backend.module.postgres.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.actions.AbstractRDBMSAction;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.QueryManager;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSQueryAction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Test class for PostgreSQL query operations.
 ** 
 ** This class tests various query scenarios including:
 ** - Filtering operations (equals, not equals, in, like, etc.)
 ** - Comparison operators (less than, greater than, between, etc.)
 ** - Complex filter expressions with AND/OR logic
 ** - Temporal filter expressions (now, date offsets, periods)
 ** - Record security with queries
 ** - Heavy field handling
 ** - Transactions
 *******************************************************************************/
public class PostgreSQLQueryActionTest extends BaseTest
{


   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      AbstractRDBMSAction.setLogSQL(false);
      QContext.getQSession().removeValue(QSession.VALUE_KEY_USER_TIMEZONE);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testUnfilteredQuery() throws QException
   {
      QueryInput  queryInput  = initQueryRequest();
      QueryOutput queryOutput = new RDBMSQueryAction().execute(queryInput);
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
      QueryOutput queryOutput = new RDBMSQueryAction().execute(queryInput);
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
      QueryOutput queryOutput = new RDBMSQueryAction().execute(queryInput);
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
      QueryOutput queryOutput = new RDBMSQueryAction().execute(queryInput);
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
      QueryOutput queryOutput = new RDBMSQueryAction().execute(queryInput);
      assertEquals(4, queryOutput.getRecords().size(), "Expected # of rows");
      assertTrue(queryOutput.getRecords().stream().noneMatch(r -> r.getValueString("email").equals(email)), "Should NOT find expected email address");
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
      QueryOutput queryOutput = new RDBMSQueryAction().execute(queryInput);
      assertEquals(3, queryOutput.getRecords().size(), "Expected # of rows");

      queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria()
            .withFieldName("annualSalary")
            .withOperator(QCriteriaOperator.NOT_EQUALS_OR_IS_NULL)
            .withValues(List.of(1_000_000))));
      queryOutput = new RDBMSQueryAction().execute(queryInput);
      assertEquals(4, queryOutput.getRecords().size(), "Expected # of rows");
      assertTrue(queryOutput.getRecords().stream().noneMatch(r -> Objects.equals(1_000_000, r.getValueInteger("annualSalary"))), "Should NOT find expected salary");
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
            .withFieldName("id")
            .withOperator(QCriteriaOperator.IN)
            .withValues(List.of(2, 4)))
      );
      QueryOutput queryOutput = new RDBMSQueryAction().execute(queryInput);
      assertEquals(2, queryOutput.getRecords().size(), "Expected # of rows");
      assertTrue(queryOutput.getRecords().stream().allMatch(r -> r.getValueInteger("id").equals(2) || r.getValueInteger("id").equals(4)), "Should find expected ids");
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
            .withFieldName("id")
            .withOperator(QCriteriaOperator.NOT_IN)
            .withValues(List.of(2, 3, 4)))
      );
      QueryOutput queryOutput = new RDBMSQueryAction().execute(queryInput);
      assertEquals(2, queryOutput.getRecords().size(), "Expected # of rows");
      assertTrue(queryOutput.getRecords().stream().allMatch(r -> r.getValueInteger("id").equals(1) || r.getValueInteger("id").equals(5)), "Should find expected ids");
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
      QueryOutput queryOutput = new RDBMSQueryAction().execute(queryInput);
      assertEquals(1, queryOutput.getRecords().size(), "Expected # of rows");
      assertTrue(queryOutput.getRecords().stream().allMatch(r -> r.getValueString("email").matches("darin.*")), "Should find matching email address");
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
      QueryOutput queryOutput = new RDBMSQueryAction().execute(queryInput);
      assertEquals(1, queryOutput.getRecords().size(), "Expected # of rows");
      assertTrue(queryOutput.getRecords().stream().allMatch(r -> r.getValueString("email").matches(".*kelkhoff.*")), "Should find matching email address");
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
      QueryOutput queryOutput = new RDBMSQueryAction().execute(queryInput);
      assertEquals(1, queryOutput.getRecords().size(), "Expected # of rows");
      assertTrue(queryOutput.getRecords().stream().allMatch(r -> r.getValueString("email").matches(".*kelkhoff.*")), "Should find matching email address");
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
      QueryOutput queryOutput = new RDBMSQueryAction().execute(queryInput);
      assertEquals(4, queryOutput.getRecords().size(), "Expected # of rows");
      assertTrue(queryOutput.getRecords().stream().noneMatch(r -> r.getValueString("email").matches(".*kelkhoff.*")), "Should find matching email address");
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
      QueryOutput queryOutput = new RDBMSQueryAction().execute(queryInput);
      assertEquals(1, queryOutput.getRecords().size(), "Expected # of rows");
      assertTrue(queryOutput.getRecords().stream().allMatch(r -> r.getValueString("email").matches(".*gmail.com")), "Should find matching email address");
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
      QueryOutput queryOutput = new RDBMSQueryAction().execute(queryInput);
      assertEquals(4, queryOutput.getRecords().size(), "Expected # of rows");
      assertTrue(queryOutput.getRecords().stream().noneMatch(r -> r.getValueString("email").matches("darin.*")), "Should find matching email address");
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
      QueryOutput queryOutput = new RDBMSQueryAction().execute(queryInput);
      assertEquals(4, queryOutput.getRecords().size(), "Expected # of rows");
      assertTrue(queryOutput.getRecords().stream().noneMatch(r -> r.getValueString("email").matches(".*kelkhoff.*")), "Should find matching email address");
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
      QueryOutput queryOutput = new RDBMSQueryAction().execute(queryInput);
      assertEquals(4, queryOutput.getRecords().size(), "Expected # of rows");
      assertTrue(queryOutput.getRecords().stream().noneMatch(r -> r.getValueString("email").matches(".*gmail.com")), "Should find matching email address");
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
            .withFieldName("id")
            .withOperator(QCriteriaOperator.LESS_THAN)
            .withValues(List.of(3)))
      );
      QueryOutput queryOutput = new RDBMSQueryAction().execute(queryInput);
      assertEquals(2, queryOutput.getRecords().size(), "Expected # of rows");
      assertTrue(queryOutput.getRecords().stream().allMatch(r -> r.getValueInteger("id").equals(1) || r.getValueInteger("id").equals(2)), "Should find expected ids");
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
            .withFieldName("id")
            .withOperator(QCriteriaOperator.LESS_THAN_OR_EQUALS)
            .withValues(List.of(2)))
      );
      QueryOutput queryOutput = new RDBMSQueryAction().execute(queryInput);
      assertEquals(2, queryOutput.getRecords().size(), "Expected # of rows");
      assertTrue(queryOutput.getRecords().stream().allMatch(r -> r.getValueInteger("id").equals(1) || r.getValueInteger("id").equals(2)), "Should find expected ids");
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
            .withFieldName("id")
            .withOperator(QCriteriaOperator.GREATER_THAN)
            .withValues(List.of(3)))
      );
      QueryOutput queryOutput = new RDBMSQueryAction().execute(queryInput);
      assertEquals(2, queryOutput.getRecords().size(), "Expected # of rows");
      assertTrue(queryOutput.getRecords().stream().allMatch(r -> r.getValueInteger("id").equals(4) || r.getValueInteger("id").equals(5)), "Should find expected ids");
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
            .withFieldName("id")
            .withOperator(QCriteriaOperator.GREATER_THAN_OR_EQUALS)
            .withValues(List.of(4)))
      );
      QueryOutput queryOutput = new RDBMSQueryAction().execute(queryInput);
      assertEquals(2, queryOutput.getRecords().size(), "Expected # of rows");
      assertTrue(queryOutput.getRecords().stream().allMatch(r -> r.getValueInteger("id").equals(4) || r.getValueInteger("id").equals(5)), "Should find expected ids");
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
      QueryOutput queryOutput = new RDBMSQueryAction().execute(queryInput);
      assertEquals(1, queryOutput.getRecords().size(), "Expected # of rows");
      assertTrue(queryOutput.getRecords().stream().allMatch(r -> r.getValue("birthDate") == null), "Should find expected row");
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
      QueryOutput queryOutput = new RDBMSQueryAction().execute(queryInput);
      assertEquals(5, queryOutput.getRecords().size(), "Expected # of rows");
      assertTrue(queryOutput.getRecords().stream().allMatch(r -> r.getValue("firstName") != null), "Should find expected rows");
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
            .withFieldName("id")
            .withOperator(QCriteriaOperator.BETWEEN)
            .withValues(List.of(2, 4))
         ));
      QueryOutput queryOutput = new RDBMSQueryAction().execute(queryInput);
      assertEquals(3, queryOutput.getRecords().size(), "Expected # of rows");
      assertTrue(queryOutput.getRecords().stream().allMatch(r -> r.getValueInteger("id").equals(2) || r.getValueInteger("id").equals(3) || r.getValueInteger("id").equals(4)), "Should find expected ids");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testNotBetweenQuery() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria()
            .withFieldName("id")
            .withOperator(QCriteriaOperator.NOT_BETWEEN)
            .withValues(List.of(2, 4))
         ));
      QueryOutput queryOutput = new RDBMSQueryAction().execute(queryInput);
      assertEquals(2, queryOutput.getRecords().size(), "Expected # of rows");
      assertTrue(queryOutput.getRecords().stream().allMatch(r -> r.getValueInteger("id").equals(1) || r.getValueInteger("id").equals(5)), "Should find expected ids");
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
         QueryOutput queryOutput = new RDBMSQueryAction().execute(queryInput);
         assertEquals(1, queryOutput.getRecords().size(), "Expected # of rows");
         assertTrue(queryOutput.getRecords().stream().anyMatch(r -> r.getValue("firstName").equals("past")), "Should find expected row");
      }

      {
         QueryInput queryInput = initQueryRequest();
         queryInput.setFilter(new QQueryFilter()
            .withCriteria(new QFilterCriteria().withFieldName("lastName").withOperator(QCriteriaOperator.EQUALS).withValues(List.of("ExpressionTest")))
            .withCriteria(new QFilterCriteria().withFieldName("birthDate").withOperator(QCriteriaOperator.LESS_THAN).withValues(List.of(NowWithOffset.plus(2, ChronoUnit.DAYS)))));
         QueryOutput queryOutput = new RDBMSQueryAction().execute(queryInput);
         assertEquals(1, queryOutput.getRecords().size(), "Expected # of rows");
         assertTrue(queryOutput.getRecords().stream().anyMatch(r -> r.getValue("firstName").equals("past")), "Should find expected row");
      }

      {
         QueryInput queryInput = initQueryRequest();
         queryInput.setFilter(new QQueryFilter()
            .withCriteria(new QFilterCriteria().withFieldName("lastName").withOperator(QCriteriaOperator.EQUALS).withValues(List.of("ExpressionTest")))
            .withCriteria(new QFilterCriteria().withFieldName("birthDate").withOperator(QCriteriaOperator.GREATER_THAN).withValues(List.of(NowWithOffset.minus(5, ChronoUnit.DAYS)))));
         QueryOutput queryOutput = new RDBMSQueryAction().execute(queryInput);
         assertEquals(2, queryOutput.getRecords().size(), "Expected # of rows");
         assertTrue(queryOutput.getRecords().stream().anyMatch(r -> r.getValue("firstName").equals("past")), "Should find expected row");
         assertTrue(queryOutput.getRecords().stream().anyMatch(r -> r.getValue("firstName").equals("future")), "Should find expected row");
      }
   }



   /*******************************************************************************
    ** Adding additional test conditions, specifically for DATE-precision
    *******************************************************************************/
   @ParameterizedTest()
   @ValueSource(strings = { "UTC", "US/Eastern", "UTC+12" })
   void testMoreFilterExpressions(String userTimezone) throws QException
   {
      QContext.getQSession().setValue(QSession.VALUE_KEY_USER_TIMEZONE, userTimezone);

      LocalDate today     = Instant.now().atZone(ZoneId.of(userTimezone)).toLocalDate();
      LocalDate yesterday = today.minusDays(1);
      LocalDate tomorrow  = today.plusDays(1);

      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON).withRecords(List.of(
         new QRecord().withValue("email", "-").withValue("firstName", "yesterday").withValue("lastName", "ExpressionTest").withValue("birthDate", yesterday),
         new QRecord().withValue("email", "-").withValue("firstName", "today").withValue("lastName", "ExpressionTest").withValue("birthDate", today),
         new QRecord().withValue("email", "-").withValue("firstName", "tomorrow").withValue("lastName", "ExpressionTest").withValue("birthDate", tomorrow))
      ));

      UnsafeFunction<Consumer<QQueryFilter>, List<QRecord>, QException> testFunction = (filterConsumer) ->
      {
         QQueryFilter filter = new QQueryFilter().withCriteria("lastName", QCriteriaOperator.EQUALS, "ExpressionTest");
         filter.withOrderBy(new QFilterOrderBy("birthDate"));
         filterConsumer.accept(filter);

         return QueryAction.execute(TestUtils.TABLE_NAME_PERSON, filter);
      };

      assertOneRecordWithFirstName("today", testFunction.apply(filter -> filter.withCriteria(new QFilterCriteria("birthDate", QCriteriaOperator.EQUALS, new Now()))));
      assertOneRecordWithFirstName("tomorrow", testFunction.apply(filter -> filter.withCriteria(new QFilterCriteria("birthDate", QCriteriaOperator.GREATER_THAN, new Now()))));
      assertOneRecordWithFirstName("yesterday", testFunction.apply(filter -> filter.withCriteria(new QFilterCriteria("birthDate", QCriteriaOperator.LESS_THAN, new Now()))));
      assertTwoRecordsWithFirstNames("yesterday", "today", testFunction.apply(filter -> filter.withCriteria(new QFilterCriteria("birthDate", QCriteriaOperator.LESS_THAN_OR_EQUALS, new Now()))));
      assertTwoRecordsWithFirstNames("today", "tomorrow", testFunction.apply(filter -> filter.withCriteria(new QFilterCriteria("birthDate", QCriteriaOperator.GREATER_THAN_OR_EQUALS, new Now()))));

      assertNoOfRecords(0, testFunction.apply(filter -> filter.withCriteria(new QFilterCriteria("birthDate", QCriteriaOperator.LESS_THAN, NowWithOffset.minus(1, ChronoUnit.DAYS)))));
      assertNoOfRecords(3, testFunction.apply(filter -> filter.withCriteria(new QFilterCriteria("birthDate", QCriteriaOperator.LESS_THAN_OR_EQUALS, NowWithOffset.plus(1, ChronoUnit.DAYS)))));
      assertOneRecordWithFirstName("yesterday", testFunction.apply(filter -> filter.withCriteria(new QFilterCriteria("birthDate", QCriteriaOperator.EQUALS, NowWithOffset.minus(1, ChronoUnit.DAYS)))));
      assertOneRecordWithFirstName("tomorrow", testFunction.apply(filter -> filter.withCriteria(new QFilterCriteria("birthDate", QCriteriaOperator.EQUALS, NowWithOffset.plus(1, ChronoUnit.DAYS)))));
      assertNoOfRecords(3, testFunction.apply(filter -> filter.withCriteria(new QFilterCriteria("birthDate", QCriteriaOperator.LESS_THAN, NowWithOffset.plus(1, ChronoUnit.WEEKS)))));
      assertNoOfRecords(3, testFunction.apply(filter -> filter.withCriteria(new QFilterCriteria("birthDate", QCriteriaOperator.LESS_THAN, NowWithOffset.plus(1, ChronoUnit.MONTHS)))));
      assertNoOfRecords(3, testFunction.apply(filter -> filter.withCriteria(new QFilterCriteria("birthDate", QCriteriaOperator.LESS_THAN, NowWithOffset.plus(1, ChronoUnit.YEARS)))));

      assertThatThrownBy(() -> testFunction.apply(filter -> filter.withCriteria(new QFilterCriteria("birthDate", QCriteriaOperator.LESS_THAN, NowWithOffset.plus(1, ChronoUnit.HOURS)))))
         .hasRootCauseMessage("Unsupported unit: Hours");

      assertOneRecordWithFirstName("today", testFunction.apply(filter -> filter.withCriteria(new QFilterCriteria("birthDate", QCriteriaOperator.EQUALS, ThisOrLastPeriod.this_(ChronoUnit.DAYS)))));
      assertOneRecordWithFirstName("yesterday", testFunction.apply(filter -> filter.withCriteria(new QFilterCriteria("birthDate", QCriteriaOperator.EQUALS, ThisOrLastPeriod.last(ChronoUnit.DAYS)))));
      assertNoOfRecords(3, testFunction.apply(filter -> filter.withCriteria(new QFilterCriteria("birthDate", QCriteriaOperator.GREATER_THAN, ThisOrLastPeriod.last(ChronoUnit.WEEKS)))));
      assertNoOfRecords(3, testFunction.apply(filter -> filter.withCriteria(new QFilterCriteria("birthDate", QCriteriaOperator.GREATER_THAN, ThisOrLastPeriod.last(ChronoUnit.MONTHS)))));
      assertNoOfRecords(3, testFunction.apply(filter -> filter.withCriteria(new QFilterCriteria("birthDate", QCriteriaOperator.GREATER_THAN, ThisOrLastPeriod.last(ChronoUnit.YEARS)))));
      assertNoOfRecords(0, testFunction.apply(filter -> filter.withCriteria(new QFilterCriteria("birthDate", QCriteriaOperator.LESS_THAN, ThisOrLastPeriod.last(ChronoUnit.WEEKS)))));
      assertNoOfRecords(0, testFunction.apply(filter -> filter.withCriteria(new QFilterCriteria("birthDate", QCriteriaOperator.LESS_THAN, ThisOrLastPeriod.last(ChronoUnit.MONTHS)))));
      assertNoOfRecords(0, testFunction.apply(filter -> filter.withCriteria(new QFilterCriteria("birthDate", QCriteriaOperator.LESS_THAN, ThisOrLastPeriod.last(ChronoUnit.YEARS)))));

      assertThatThrownBy(() -> testFunction.apply(filter -> filter.withCriteria(new QFilterCriteria("birthDate", QCriteriaOperator.LESS_THAN, ThisOrLastPeriod.this_(ChronoUnit.HOURS)))))
         .hasRootCauseMessage("Unsupported unit: Hours");
      assertThatThrownBy(() -> testFunction.apply(filter -> filter.withCriteria(new QFilterCriteria("birthDate", QCriteriaOperator.LESS_THAN, ThisOrLastPeriod.last(ChronoUnit.MINUTES)))))
         .hasRootCauseMessage("Unsupported unit: Minutes");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void assertNoOfRecords(Integer expectedSize, List<QRecord> actualRecords)
   {
      assertEquals(expectedSize, actualRecords.size());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void assertOneRecordWithFirstName(String expectedFirstName, List<QRecord> actualRecords)
   {
      assertEquals(1, actualRecords.size());
      assertEquals(expectedFirstName, actualRecords.get(0).getValueString("firstName"));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void assertTwoRecordsWithFirstNames(String expectedFirstName0, String expectedFirstName1, List<QRecord> actualRecords)
   {
      assertEquals(2, actualRecords.size());
      assertEquals(expectedFirstName0, actualRecords.get(0).getValueString("firstName"));
      assertEquals(expectedFirstName1, actualRecords.get(1).getValueString("firstName"));
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
    ** This doesn't really test any RDBMS code, but is a checkpoint that the core
    ** module is populating displayValues when it performs the system-level query action
    ** (if so requested by input field).
    *******************************************************************************/
   @Test
   public void testThatDisplayValuesGetSetGoingThroughQueryAction() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.setShouldGenerateDisplayValues(true);
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertEquals(5, queryOutput.getRecords().size(), "Unfiltered query should find all rows");

      for(QRecord record : queryOutput.getRecords())
      {
         assertThat(record.getValues()).isNotEmpty();
         assertThat(record.getDisplayValues()).isNotEmpty();
         assertThat(record.getErrors()).isEmpty();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testLookInsideTransaction() throws QException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON);

      InsertAction        insertAction = new InsertAction();
      QBackendTransaction transaction  = QBackendTransaction.openFor(insertInput);

      insertInput.setTransaction(transaction);
      insertInput.setRecords(List.of(
         new QRecord().withValue("firstName", "George").withValue("lastName", "Washington").withValue("email", "gw@kingsrook.com")
      ));

      insertAction.execute(insertInput);

      QueryInput  queryInput  = initQueryRequest();
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      assertEquals(5, queryOutput.getRecords().size(), "Query without the transaction should not see the new row.");

      queryInput = initQueryRequest();
      queryInput.setTransaction(transaction);
      queryOutput = new QueryAction().execute(queryInput);
      assertEquals(6, queryOutput.getRecords().size(), "Query with the transaction should see the new row.");

      transaction.rollback();
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
         .withCriteria(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 3))
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
      assertThat(queryOutput.getRecords()).anyMatch(r -> r.getValueInteger("id").equals(3) && r.getValueString("firstName").equals("Tim") && r.getValueString("lastName").equals("Chamberlain"));

      queryInput.getFilter().setCriteria(List.of(new QFilterCriteria("id", QCriteriaOperator.NOT_EQUALS, 3)));
      queryOutput = new QueryAction().execute(queryInput);
      assertEquals(0, queryOutput.getRecords().size(), "Next complex query should find 0 rows");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRecordSecurityWithOrQueries() throws QException
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_ORDER);

      queryInput.setFilter(new QQueryFilter(
         new QFilterCriteria("billToPersonId", QCriteriaOperator.EQUALS, List.of(1)),
         new QFilterCriteria("shipToPersonId", QCriteriaOperator.EQUALS, List.of(5))
      ).withBooleanOperator(QQueryFilter.BooleanOperator.OR));
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_STORE_ALL_ACCESS, true));
      assertThat(new QueryAction().execute(queryInput).getRecords())
         .hasSize(5)
         .allMatch(r -> Objects.equals(r.getValueInteger("billToPersonId"), 1) || Objects.equals(r.getValueInteger("shipToPersonId"), 5));

      queryInput.setFilter(new QQueryFilter(
         new QFilterCriteria("billToPersonId", QCriteriaOperator.EQUALS, List.of(1)),
         new QFilterCriteria("shipToPersonId", QCriteriaOperator.EQUALS, List.of(5))
      ).withBooleanOperator(QQueryFilter.BooleanOperator.OR));
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 2));
      assertThat(new QueryAction().execute(queryInput).getRecords())
         .hasSize(1)
         .allMatch(r -> r.getValueInteger("storeId").equals(2))
         .allMatch(r -> Objects.equals(r.getValueInteger("billToPersonId"), 1) || Objects.equals(r.getValueInteger("shipToPersonId"), 5));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRecordSecurityWithSubFilters() throws QException
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_ORDER);

      queryInput.setFilter(new QQueryFilter()
         .withBooleanOperator(QQueryFilter.BooleanOperator.OR)
         .withSubFilters(List.of(
            new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.GREATER_THAN_OR_EQUALS, 2), new QFilterCriteria("billToPersonId", QCriteriaOperator.EQUALS, 1)),
            new QQueryFilter(new QFilterCriteria("billToPersonId", QCriteriaOperator.IS_BLANK), new QFilterCriteria("shipToPersonId", QCriteriaOperator.IS_BLANK)).withBooleanOperator(QQueryFilter.BooleanOperator.OR)
         )));
      Predicate<QRecord> p = r -> r.getValueInteger("billToPersonId") == null || r.getValueInteger("shipToPersonId") == null || (r.getValueInteger("id") >= 2 && r.getValueInteger("billToPersonId") == 1);

      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_STORE_ALL_ACCESS, true));
      assertThat(new QueryAction().execute(queryInput).getRecords())
         .hasSize(4)
         .allMatch(p);

      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 1));
      assertThat(new QueryAction().execute(queryInput).getRecords())
         .hasSize(1)
         .allMatch(r -> r.getValueInteger("storeId").equals(1))
         .allMatch(p);

      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 3));
      assertThat(new QueryAction().execute(queryInput).getRecords())
         .hasSize(3)
         .allMatch(r -> r.getValueInteger("storeId").equals(3))
         .allMatch(p);

      QContext.setQSession(new QSession());
      assertThat(new QueryAction().execute(queryInput).getRecords()).isEmpty();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRecordSecurityNullValues() throws Exception
   {
      runTestSql("INSERT INTO \"order\" (id, store_id, bill_to_person_id, ship_to_person_id) VALUES (9, NULL, 1, 6)", null);
      runTestSql("INSERT INTO \"order\" (id, store_id, bill_to_person_id, ship_to_person_id) VALUES (10, NULL, 6, 5)", null);

      QInstance  qInstance  = TestUtils.defineInstance();
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      reInitInstanceInContext(qInstance);

      Predicate<QRecord> hasNullStoreId = r -> r.getValueInteger("storeId") == null;

      ////////////////////////////////////////////
      // all-access user should get all 10 rows //
      ////////////////////////////////////////////
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_STORE_ALL_ACCESS, true));
      assertThat(new QueryAction().execute(queryInput).getRecords())
         .hasSize(10)
         .anyMatch(hasNullStoreId);

      //////////////////////////////////////////////////////////////////////////////////////////////////
      // no-values user should get 0 rows (given that default null-behavior on this key type is DENY) //
      //////////////////////////////////////////////////////////////////////////////////////////////////
      QContext.setQSession(new QSession());
      assertThat(new QueryAction().execute(queryInput).getRecords()).isEmpty();

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // user with list of all ids shouldn't see the nulls (given that default null-behavior on this key type is DENY) //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      {
         QSession qSession = new QSession();
         for(Integer i : List.of(1, 2, 3, 4, 5))
         {
            qSession.withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, i);
         }
         QContext.setQSession(qSession);
         assertThat(new QueryAction().execute(queryInput).getRecords())
            .hasSize(8)
            .noneMatch(hasNullStoreId);
      }

      //////////////////////////////////////////////////////////////////////////
      // specifically set the null behavior to deny - repeat the last 2 tests //
      //////////////////////////////////////////////////////////////////////////
      qInstance.getTable(TestUtils.TABLE_NAME_ORDER).getRecordSecurityLocks().get(0).setNullValueBehavior(RecordSecurityLock.NullValueBehavior.DENY);

      QContext.setQSession(new QSession());
      assertThat(new QueryAction().execute(queryInput).getRecords()).isEmpty();

      {
         QSession qSession = new QSession();
         for(Integer i : List.of(1, 2, 3, 4, 5))
         {
            qSession.withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, i);
         }
         QContext.setQSession(qSession);
         assertThat(new QueryAction().execute(queryInput).getRecords())
            .hasSize(8)
            .noneMatch(hasNullStoreId);
      }

      ///////////////////////////////////
      // change null behavior to ALLOW //
      ///////////////////////////////////
      qInstance.getTable(TestUtils.TABLE_NAME_ORDER).getRecordSecurityLocks().get(0).setNullValueBehavior(RecordSecurityLock.NullValueBehavior.ALLOW);

      /////////////////////////////////////////////
      // all-access user should still get all 10 //
      /////////////////////////////////////////////
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_STORE_ALL_ACCESS, true));
      assertThat(new QueryAction().execute(queryInput).getRecords())
         .hasSize(10)
         .anyMatch(hasNullStoreId);

      /////////////////////////////////////////////////////
      // no-values user should only get the rows w/ null //
      /////////////////////////////////////////////////////
      QContext.setQSession(new QSession());
      assertThat(new QueryAction().execute(queryInput).getRecords())
         .hasSize(2)
         .allMatch(hasNullStoreId);

      ////////////////////////////////////////////////////
      // user with list of all ids should see the nulls //
      ////////////////////////////////////////////////////
      {
         QSession qSession = new QSession();
         for(Integer i : List.of(1, 2, 3, 4, 5))
         {
            qSession.withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, i);
         }
         QContext.setQSession(qSession);
         assertThat(new QueryAction().execute(queryInput).getRecords())
            .hasSize(10)
            .anyMatch(hasNullStoreId);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   @SuppressWarnings("unchecked")
   void testHeavyFields() throws QException
   {
      //////////////////////////////////////////////////////////
      // set homeTown field as heavy - so it won't be fetched //
      //////////////////////////////////////////////////////////
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON)
         .getField("homeTown")
         .withIsHeavy(true);

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      List<QRecord> records = new QueryAction().execute(queryInput).getRecords();
      assertThat(records).describedAs("No records should have the heavy homeTown field set").noneMatch(r -> r.getValue("homeTown") != null);
      assertThat(records).describedAs("Some records should have a homeTown length backend detail set").anyMatch(r -> ((Map<String, Serializable>) r.getBackendDetail(QRecord.BACKEND_DETAILS_TYPE_HEAVY_FIELD_LENGTHS)).get("homeTown") != null);
      assertThat(records).describedAs("Some records should have a null homeTown length backend").anyMatch(r -> ((Map<String, Serializable>) r.getBackendDetail(QRecord.BACKEND_DETAILS_TYPE_HEAVY_FIELD_LENGTHS)).get("homeTown") == null);

      //////////////////////////////////////////////
      // re-do the query, requesting heavy fields //
      //////////////////////////////////////////////
      queryInput.setShouldFetchHeavyFields(true);
      records = new QueryAction().execute(queryInput).getRecords();
      assertThat(records).describedAs("Some records should have the heavy homeTown field set when heavies are requested").anyMatch(r -> r.getValue("homeTown") != null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFieldNamesToInclude() throws QException
   {
      QQueryFilter filter     = new QQueryFilter().withCriteria("id", QCriteriaOperator.EQUALS, 1);
      QueryInput   queryInput = new QueryInput(TestUtils.TABLE_NAME_PERSON).withFilter(filter);

      QRecord record = new QueryAction().execute(queryInput.withFieldNamesToInclude(null)).getRecords().get(0);
      assertTrue(record.getValues().containsKey("id"));
      assertTrue(record.getValues().containsKey("firstName"));
      assertTrue(record.getValues().containsKey("createDate"));
      assertEquals(QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON).getFields().size(), record.getValues().size());

      record = new QueryAction().execute(queryInput.withFieldNamesToInclude(Set.of("id", "firstName"))).getRecords().get(0);
      assertTrue(record.getValues().containsKey("id"));
      assertTrue(record.getValues().containsKey("firstName"));
      assertFalse(record.getValues().containsKey("createDate"));
      assertEquals(2, record.getValues().size());

      record = new QueryAction().execute(queryInput.withFieldNamesToInclude(Set.of("homeTown"))).getRecords().get(0);
      assertFalse(record.getValues().containsKey("id"));
      assertFalse(record.getValues().containsKey("firstName"));
      assertFalse(record.getValues().containsKey("createDate"));
      assertEquals(1, record.getValues().size());
   }



   /*******************************************************************************
    ** Test that PostgreSQL properly handles type conversion for INTEGER and LONG
    ** fields when query parameters come in as strings (e.g., from HTTP requests).
    ** 
    ** This is critical for PostgreSQL which is stricter than MySQL about type
    ** matching and won't implicitly convert "1" (string) to 1 (integer).
    *******************************************************************************/
   @Test
   void testIntegerAndLongTypeConversionFromStrings() throws QException
   {
      /////////////////////////////////////////////////////
      // Test EQUALS with string value for INTEGER field //
      /////////////////////////////////////////////////////
      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria()
            .withFieldName("id")
            .withOperator(QCriteriaOperator.EQUALS)
            .withValues(List.of("1")))  // String value for INTEGER field
      );
      QueryOutput queryOutput = new RDBMSQueryAction().execute(queryInput);
      assertEquals(1, queryOutput.getRecords().size(), "Should find 1 row with id=1");
      assertEquals(1, queryOutput.getRecords().get(0).getValueInteger("id"), "Should find expected id");

      ////////////////////////////////////////////////
      // Test IN with string values for LONG field //
      ////////////////////////////////////////////////
      queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria()
            .withFieldName("annualSalary")
            .withOperator(QCriteriaOperator.IN)
            .withValues(List.of("25000", "1000000")))  // String values for INTEGER field
      );
      queryOutput = new RDBMSQueryAction().execute(queryInput);
      assertEquals(2, queryOutput.getRecords().size(), "Should find 2 rows");
      assertTrue(queryOutput.getRecords().stream()
         .allMatch(r -> r.getValueInteger("annualSalary").equals(25000) || r.getValueInteger("annualSalary").equals(1_000_000)),
         "Should find expected salaries");

      ////////////////////////////////////////////////////////
      // Test GREATER_THAN with string value for LONG field //
      ////////////////////////////////////////////////////////
      queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria()
            .withFieldName("annualSalary")
            .withOperator(QCriteriaOperator.GREATER_THAN)
            .withValues(List.of("100000")))  // String value for INTEGER field
      );
      queryOutput = new RDBMSQueryAction().execute(queryInput);
      assertEquals(1, queryOutput.getRecords().size(), "Should find 1 row with salary > 100000");
      assertEquals(1_000_000, queryOutput.getRecords().get(0).getValueInteger("annualSalary"), "Should find expected salary");

      ////////////////////////////////////////////////////////
      // Test BETWEEN with string values for INTEGER field //
      ////////////////////////////////////////////////////////
      queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria()
            .withFieldName("id")
            .withOperator(QCriteriaOperator.BETWEEN)
            .withValues(List.of("2", "4")))  // String values for INTEGER field
      );
      queryOutput = new RDBMSQueryAction().execute(queryInput);
      assertEquals(3, queryOutput.getRecords().size(), "Should find 3 rows with id between 2 and 4");
      assertTrue(queryOutput.getRecords().stream()
         .allMatch(r -> r.getValueInteger("id") >= 2 && r.getValueInteger("id") <= 4),
         "Should find ids between 2 and 4");
   }



   /*******************************************************************************
    ** Test that PostgreSQL TIMESTAMP WITH TIME ZONE (timestamptz) values are
    ** parsed correctly by QueryManager.getInstant().
    **
    ** PostgreSQL returns timestamptz values with timezone offset suffix (e.g., +00)
    ** which was previously causing DateTimeParseException in getInstant().
    **
    ** @see <a href="https://github.com/QRun-IO/qqq/issues/333">Issue #333</a>
    *******************************************************************************/
   @Test
   void testTimestampWithTimezoneOffset() throws Exception
   {
      try(Connection connection = new ConnectionManager().getConnection(TestUtils.defineBackend()))
      {
         ///////////////////////////////////////////////////////////////////////////////////////////
         // create a test table with TIMESTAMPTZ - PostgreSQL's timestamp with timezone type      //
         // this type returns values with timezone offset suffix like "2026-01-08T20:35:45.27+00" //
         ///////////////////////////////////////////////////////////////////////////////////////////
         QueryManager.executeUpdate(connection, """
            CREATE TABLE IF NOT EXISTS timestamptz_test
            (
               id SERIAL PRIMARY KEY,
               event_time TIMESTAMPTZ NOT NULL
            )
            """);

         try
         {
            ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // insert timestamps with explicit timezone offsets to simulate real PostgreSQL behavior                     //
            // PostgreSQL stores these in UTC internally but returns them with the server's timezone offset in the query //
            ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
            QueryManager.executeUpdate(connection, """
               INSERT INTO timestamptz_test (event_time) VALUES
               ('2026-01-08 20:35:45.270008+00'),
               ('2026-06-15 14:30:00+05:30'),
               ('2025-12-31 23:59:59.999-08')
               """);

            ////////////////////////////////////////////////////////////////////////////////////////////////
            // query the timestamps using getInstant() - this should NOT throw DateTimeParseException now //
            ////////////////////////////////////////////////////////////////////////////////////////////////
            QueryManager.executeStatement(connection, "SELECT id, event_time FROM timestamptz_test ORDER BY id", rs ->
            {
               int count = 0;
               while(rs.next())
               {
                  count++;
                  Instant instant = QueryManager.getInstant(rs, 2);
                  assertThat(instant).isNotNull();

                  //////////////////////////////////////////////////
                  // verify the instants are reasonable UTC times //
                  //////////////////////////////////////////////////
                  assertThat(instant.toString()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*Z");
               }
               assertThat(count).isEqualTo(3);
            });
         }
         finally
         {
            QueryManager.executeUpdate(connection, "DROP TABLE IF EXISTS timestamptz_test");
         }
      }
   }

}
