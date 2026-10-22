/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.CurrentUser;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** H2 exercises the actual JDBC predicates produced from session expressions.
 *******************************************************************************/
class CurrentUserRDBMSFilterExpressionTest extends RDBMSActionTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void seed() throws Exception
   {
      primeTestDatabase();
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineBackend());
          Statement statement = connection.createStatement())
      {
         statement.execute("CREATE TABLE ownership_test (id INTEGER PRIMARY KEY, owner VARCHAR(80), assignee INTEGER)");
         statement.execute("INSERT INTO ownership_test VALUES (1, '2', 2), (2, '3', 3)");
      }
      QContext.getQInstance().addTable(new QTableMetaData().withName("ownershipTest").withBackendName(TestUtils.DEFAULT_BACKEND_NAME)
         .withBackendDetails(new RDBMSTableBackendDetails().withTableName("ownership_test"))
         .withPrimaryKeyField("id").withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("owner", QFieldType.STRING)).withField(new QFieldMetaData("assignee", QFieldType.INTEGER)));
      QContext.setQSession(new QSession().withUser(new QUser().withIdReference("2")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void cleanup() throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineBackend());
          Statement statement = connection.createStatement())
      {
         statement.execute("DROP TABLE IF EXISTS ownership_test");
      }
   }



   /*******************************************************************************
    ** Values use JDBC parameters, including integer ids and mixed IN lists.
    *******************************************************************************/
   @Test
   void queryAndCountResolvePerSession() throws Exception
   {
      for(String field : List.of("owner", "assignee"))
      {
         for(QCriteriaOperator operator : List.of(QCriteriaOperator.EQUALS, QCriteriaOperator.NOT_EQUALS, QCriteriaOperator.IN))
         {
            for(Serializable value : List.of(new CurrentUser(), "${currentUser}"))
            {
               QQueryFilter filter = new QQueryFilter(new QFilterCriteria(field, operator, value));
               assertEquals(1, CountAction.execute("ownershipTest", filter));
               assertEquals(operator == QCriteriaOperator.NOT_EQUALS ? 2 : 1, QueryAction.execute("ownershipTest", filter).get(0).getValueInteger("id"));
               QContext.getQSession().getUser().setIdReference("3");
               assertEquals(operator == QCriteriaOperator.NOT_EQUALS ? 1 : 2, QueryAction.execute("ownershipTest", filter).get(0).getValueInteger("id"));
               assertEquals(value, filter.getCriteria().get(0).getValues().get(0));
               QContext.getQSession().getUser().setIdReference("2");
            }
         }
      }
      assertEquals(2, CountAction.execute("ownershipTest", new QQueryFilter(new QFilterCriteria("assignee", QCriteriaOperator.IN, List.of(new CurrentUser(), 3)))));
   }



   /*******************************************************************************
    ** FALSE rather than IS NULL or IS NOT NULL when the identity is absent.
    *******************************************************************************/
   @Test
   void missingUserMatchesNothing() throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineBackend());
          Statement statement = connection.createStatement())
      {
         statement.execute("INSERT INTO ownership_test VALUES (3, NULL, NULL)");
      }
      for(QSession session : List.of(new QSession(), new QSession().withUser(new QUser())))
      {
         QContext.setQSession(session);
         for(String field : List.of("owner", "assignee"))
         {
            for(QCriteriaOperator operator : List.of(QCriteriaOperator.EQUALS, QCriteriaOperator.NOT_EQUALS, QCriteriaOperator.IN))
            {
               for(Serializable value : List.of(new CurrentUser(), "${currentUser}"))
               {
                  QQueryFilter filter = new QQueryFilter(new QFilterCriteria(field, operator, value));
                  assertEquals(0, CountAction.execute("ownershipTest", filter));
                  assertTrue(QueryAction.execute("ownershipTest", filter).isEmpty());
               }
            }
         }
      }
      QQueryFilter nested = new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR)
         .withSubFilters(List.of(new QQueryFilter(new QFilterCriteria("owner", QCriteriaOperator.EQUALS, new CurrentUser())),
            new QQueryFilter(new QFilterCriteria("assignee", QCriteriaOperator.IN, new CurrentUser()))));
      assertEquals(0, CountAction.execute("ownershipTest", nested));
      assertTrue(QueryAction.execute("ownershipTest", nested).isEmpty());
   }
}
