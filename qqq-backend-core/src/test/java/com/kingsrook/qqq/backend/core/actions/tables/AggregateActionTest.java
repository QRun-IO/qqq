/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.tables;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.ExamplePersonalizer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Unit test for com.kingsrook.qqq.backend.core.actions.tables.AggregateAction
 *******************************************************************************/
class AggregateActionTest extends BaseTest
{

   /*******************************************************************************
    ** At the core level, there isn't much that can be asserted, as it uses the
    ** mock implementation - just confirming that all of the "wiring" works.
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      AggregateInput request = new AggregateInput();
      request.setTableName("person");
      assertThrows(IllegalStateException.class, () -> new AggregateAction().execute(request));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInvalidFieldNames() throws QException
   {
      assertThatThrownBy(() -> new AggregateAction().execute(new AggregateInput(TestUtils.TABLE_NAME_SHAPE)
         .withFilter(new QQueryFilter(new QFilterCriteria("notAField", QCriteriaOperator.IS_NOT_BLANK)))))
         .hasMessageContaining("1 unrecognized field name: notAField");

      assertThatThrownBy(() -> new AggregateAction().execute(new AggregateInput(TestUtils.TABLE_NAME_SHAPE)
         .withFilter(new QQueryFilter().withSubFilter(new QQueryFilter(new QFilterCriteria("notAField", QCriteriaOperator.IS_NOT_BLANK))))))
         .hasMessageContaining("1 unrecognized field name: notAField");

      assertThatThrownBy(() -> new AggregateAction().execute(new AggregateInput(TestUtils.TABLE_NAME_SHAPE)
         .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("notAField")))))
         .hasMessageContaining("Query Filter contained 1 unrecognized field name: notAField");

      assertThatThrownBy(() -> new AggregateAction().execute(new AggregateInput(TestUtils.TABLE_NAME_SHAPE)
         .withAggregate(new Aggregate("noWay", AggregateOperator.MAX))))
         .hasMessageContaining("AggregateInput contained 1 unrecognized field name: noWay");

      assertThatThrownBy(() -> new AggregateAction().execute(new AggregateInput(TestUtils.TABLE_NAME_SHAPE)
         .withGroupBy(new GroupBy(QFieldType.INTEGER, "nope"))))
         .hasMessageContaining("AggregateInput contained 1 unrecognized field name: nope");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testJoins() throws QException
   {
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // first 2 cases - supply a filter that allows us, through the JoinContext, to figure out that the Order table is joined in. //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      new AggregateAction().execute(new AggregateInput(TestUtils.TABLE_NAME_LINE_ITEM)
         .withFilter(new QQueryFilter(new QFilterCriteria(TestUtils.TABLE_NAME_ORDER + ".storeId", QCriteriaOperator.IS_NOT_BLANK)))
         .withAggregate(new Aggregate(TestUtils.TABLE_NAME_ORDER + ".orderNo", AggregateOperator.COUNT_DISTINCT)));

      new AggregateAction().execute(new AggregateInput(TestUtils.TABLE_NAME_LINE_ITEM)
         .withFilter(new QQueryFilter(new QFilterCriteria(TestUtils.TABLE_NAME_ORDER + ".storeId", QCriteriaOperator.IS_NOT_BLANK)))
         .withGroupBy(new GroupBy(QFieldType.INTEGER, TestUtils.TABLE_NAME_ORDER + ".storeId")));

      ///////////////////////////////////////////////////////////
      // next 2 cases - explicitly state order table as a join //
      ///////////////////////////////////////////////////////////
      new AggregateAction().execute(new AggregateInput(TestUtils.TABLE_NAME_LINE_ITEM)
         .withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_ORDER))
         .withAggregate(new Aggregate(TestUtils.TABLE_NAME_ORDER + ".orderNo", AggregateOperator.COUNT_DISTINCT)));

      new AggregateAction().execute(new AggregateInput(TestUtils.TABLE_NAME_LINE_ITEM)
         .withQueryJoin(new QueryJoin(TestUtils.TABLE_NAME_ORDER))
         .withGroupBy(new GroupBy(QFieldType.INTEGER, TestUtils.TABLE_NAME_ORDER + ".storeId")));

      ////////////////////////////////////////////////////////////////
      // now 2 fail cases, where join table can't be known, so fail //
      ////////////////////////////////////////////////////////////////
      assertThatThrownBy(() -> new AggregateAction().execute(new AggregateInput(TestUtils.TABLE_NAME_LINE_ITEM)
         .withAggregate(new Aggregate(TestUtils.TABLE_NAME_ORDER + ".orderNo", AggregateOperator.COUNT_DISTINCT))))
         .hasMessageContaining("AggregateInput contained 1 unrecognized field name: order.orderNo");

      assertThatThrownBy(() -> new AggregateAction().execute(new AggregateInput(TestUtils.TABLE_NAME_LINE_ITEM)
         .withGroupBy(new GroupBy(QFieldType.INTEGER, TestUtils.TABLE_NAME_ORDER + ".storeId"))))
         .hasMessageContaining("AggregateInput contained 1 unrecognized field name: order.storeId");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTablePersonalization() throws QException
   {
      QContext.getQSession().getUser().setIdReference("jdoe");
      ExamplePersonalizer.registerInQInstance();
      ExamplePersonalizer.addCustomizableTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      ExamplePersonalizer.addFieldToRemoveForUserId(TestUtils.TABLE_NAME_PERSON_MEMORY, "noOfShoes", QContext.getQSession().getUser().getIdReference());

      ///////////////////////////////////////////////////////////////////////////
      // make sure not allowed to filter by a field we don't have in the table //
      ///////////////////////////////////////////////////////////////////////////
      AggregateInput aggregateInput = new AggregateInput(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withFilter(new QQueryFilter(new QFilterCriteria("noOfShoes", QCriteriaOperator.EQUALS, 2)))
         .withInputSource(QInputSource.USER);
      assertThatThrownBy(() -> new AggregateAction().execute(aggregateInput))
         .hasMessageContaining("Query Filter contained 1 unrecognized field name: noOfShoes");
   }

}