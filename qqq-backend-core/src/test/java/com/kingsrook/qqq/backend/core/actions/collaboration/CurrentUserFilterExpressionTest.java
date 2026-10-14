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

package com.kingsrook.qqq.backend.core.actions.collaboration;


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.CurrentUser;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Memory query/count, persistence, and session isolation for user-id expressions.
 *******************************************************************************/
class CurrentUserFilterExpressionTest extends BaseTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void seed() throws Exception
   {
      QContext.getQInstance().addTable(new QTableMetaData().withName("ownershipTest")
         .withBackendName(TestUtils.MEMORY_BACKEND_NAME)
         .withPrimaryKeyField("id").withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("owner", QFieldType.STRING)).withField(new QFieldMetaData("assignee", QFieldType.INTEGER)));
      new InsertAction().execute(new InsertInput("ownershipTest").withRecords(List.of(
         new QRecord().withValue("id", 1).withValue("owner", "2").withValue("assignee", 2),
         new QRecord().withValue("id", 2).withValue("owner", "3").withValue("assignee", 3))));
      QContext.setQSession(newSession("2"));
   }



   /*******************************************************************************
    ** Every supported operator accepts both id field types and both spellings.
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
               QContext.setQSession(newSession("3"));
               assertEquals(operator == QCriteriaOperator.NOT_EQUALS ? 1 : 2, QueryAction.execute("ownershipTest", filter).get(0).getValueInteger("id"));
               assertEquals(value, filter.getCriteria().get(0).getValues().get(0));
               QContext.setQSession(newSession("2"));
            }
         }
      }
      QQueryFilter mixed = new QQueryFilter(new QFilterCriteria("assignee", QCriteriaOperator.IN, List.of(new CurrentUser(), 3)));
      assertEquals(2, CountAction.execute("ownershipTest", mixed));
   }



   /*******************************************************************************
    ** Null identities must not select unowned rows or invert into all rows.
    *******************************************************************************/
   @Test
   void missingUserMatchesNothing() throws Exception
   {
      new InsertAction().execute(new InsertInput("ownershipTest").withRecords(List.of(new QRecord().withValue("id", 3))));
      for(QSession session : List.of(new QSession(), new QSession().withUser(new QUser())))
      {
         QContext.setQSession(session);
         assertNull(new CurrentUser().evaluate(null));
         for(QCriteriaOperator operator : List.of(QCriteriaOperator.EQUALS, QCriteriaOperator.NOT_EQUALS, QCriteriaOperator.IN))
         {
            QQueryFilter filter = new QQueryFilter(new QFilterCriteria("owner", operator, new CurrentUser()));
            assertEquals(0, CountAction.execute("ownershipTest", filter));
            assertTrue(QueryAction.execute("ownershipTest", filter).isEmpty());
         }
      }
      QContext.setQSession(null);
      assertNull(new CurrentUser().evaluate(null));
   }



   /*******************************************************************************
    ** Saved filters keep the discriminator and interpret the variable lazily.
    *******************************************************************************/
   @Test
   void jsonAndVariableRoundTrip() throws Exception
   {
      QQueryFilter filter = new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR)
         .withSubFilters(List.of(new QQueryFilter(new QFilterCriteria("owner", QCriteriaOperator.EQUALS, new CurrentUser())),
            new QQueryFilter(new QFilterCriteria("assignee", QCriteriaOperator.IN, "${currentUser}"))));
      String json = JsonUtils.toJson(filter);
      assertTrue(json.contains("\"type\":\"CurrentUser\""));
      QQueryFilter restored = JsonUtils.toObject(json, QQueryFilter.class);
      restored.interpretValues(Map.of());
      assertInstanceOf(CurrentUser.class, restored.getSubFilters().get(1).getCriteria().get(0).getValues().get(0));
      assertEquals(1, CountAction.execute("ownershipTest", restored));
      QContext.setQSession(new QSession());
      assertEquals(0, CountAction.execute("ownershipTest", restored));
      assertEquals(0, QueryAction.execute("ownershipTest", restored).size());
   }
}
