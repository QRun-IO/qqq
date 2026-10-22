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


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByAggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByGroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.module.mongodb.BaseTest;
import com.kingsrook.qqq.backend.module.mongodb.TestUtils;
import org.junit.jupiter.api.Test;


/*******************************************************************************
 ** Unit test for MongoDBQueryAction 
 *******************************************************************************/
class MongoDBAggregateActionTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      insertInput.setRecords(List.of(
         new QRecord().withValue("firstName", "Darin").withValue("lastName", "Kelkhoff").withValue("isEmployed", true).withValue("annualSalary", 1),
         new QRecord().withValue("firstName", "Linda").withValue("lastName", "Kelkhoff").withValue("isEmployed", true).withValue("annualSalary", 5),
         new QRecord().withValue("firstName", "Tim").withValue("lastName", "Chamberlain").withValue("isEmployed", true).withValue("annualSalary", 3),
         new QRecord().withValue("firstName", "James").withValue("lastName", "Maes").withValue("isEmployed", true).withValue("annualSalary", 5),
         new QRecord().withValue("firstName", "J.D.").withValue("lastName", "Maes").withValue("isEmployed", false).withValue("annualSalary", 0)
      ));
      new InsertAction().execute(insertInput);

      {
         AggregateInput aggregateInput = new AggregateInput();
         aggregateInput.setTableName(TestUtils.TABLE_NAME_PERSON);
         aggregateInput.setFilter(new QQueryFilter()
            .withOrderBy(new QFilterOrderByAggregate(new Aggregate("annualSalary", AggregateOperator.MAX)).withIsAscending(false))
            .withOrderBy(new QFilterOrderByGroupBy(new GroupBy(QFieldType.STRING, "lastName")))
         );
         aggregateInput.withAggregate(new Aggregate("id", AggregateOperator.COUNT));
         aggregateInput.withAggregate(new Aggregate("annualSalary", AggregateOperator.SUM));
         aggregateInput.withAggregate(new Aggregate("annualSalary", AggregateOperator.MAX));
         aggregateInput.withGroupBy(new GroupBy(QFieldType.STRING, "lastName"));
         aggregateInput.withGroupBy(new GroupBy(QFieldType.BOOLEAN, "isEmployed"));
         AggregateOutput aggregateOutput = new AggregateAction().execute(aggregateInput);
         // todo - actual assertions
      }
      {
         AggregateInput aggregateInput = new AggregateInput();
         aggregateInput.setTableName(TestUtils.TABLE_NAME_PERSON);
         aggregateInput.withAggregate(new Aggregate("id", AggregateOperator.COUNT));
         aggregateInput.withAggregate(new Aggregate("annualSalary", AggregateOperator.AVG));
         AggregateOutput aggregateOutput = new AggregateAction().execute(aggregateInput);
         // todo - actual assertions
      }
   }

}