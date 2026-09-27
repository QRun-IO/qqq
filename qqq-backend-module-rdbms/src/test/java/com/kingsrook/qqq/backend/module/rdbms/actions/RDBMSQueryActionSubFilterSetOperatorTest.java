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


import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.SubStringFunction;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** test for subfilter set
 *******************************************************************************/
public class RDBMSQueryActionSubFilterSetOperatorTest extends RDBMSActionTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   public void beforeEach() throws Exception
   {
      super.primeTestDatabase();

      // AbstractRDBMSAction.setLogSQL(true, true, "system.out");
   }



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
   public void testUnion() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withSubFilterSetOperator(QQueryFilter.SubFilterSetOperator.UNION)
         .withSubFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.IN, 1, 2)))
         .withSubFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.IN, 2, 3)))
         .withOrderBy(new QFilterOrderBy("id", false))
      );

      QueryOutput queryOutput = new RDBMSQueryAction().execute(queryInput);
      assertEquals(3, queryOutput.getRecords().size(), "Expected # of rows");
      assertEquals(3, queryOutput.getRecords().get(0).getValueInteger("id"));
      assertEquals(2, queryOutput.getRecords().get(1).getValueInteger("id"));
      assertEquals(1, queryOutput.getRecords().get(2).getValueInteger("id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testUnionAll() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withSubFilterSetOperator(QQueryFilter.SubFilterSetOperator.UNION_ALL)
         .withSubFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.IN, 1, 2)))
         .withSubFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.IN, 2, 3)))
         .withOrderBy(new QFilterOrderBy("id", false))
      );

      QueryOutput queryOutput = new RDBMSQueryAction().execute(queryInput);
      assertEquals(4, queryOutput.getRecords().size(), "Expected # of rows");
      assertEquals(3, queryOutput.getRecords().get(0).getValueInteger("id"));
      assertEquals(2, queryOutput.getRecords().get(1).getValueInteger("id"));
      assertEquals(2, queryOutput.getRecords().get(2).getValueInteger("id"));
      assertEquals(1, queryOutput.getRecords().get(3).getValueInteger("id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testIntersect() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withSubFilterSetOperator(QQueryFilter.SubFilterSetOperator.INTERSECT)
         .withSubFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.IN, 1, 2)))
         .withSubFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.IN, 2, 3)))
         .withOrderBy(new QFilterOrderBy("id", false))
      );

      QueryOutput queryOutput = new RDBMSQueryAction().execute(queryInput);
      assertEquals(1, queryOutput.getRecords().size(), "Expected # of rows");
      assertEquals(2, queryOutput.getRecords().get(0).getValueInteger("id"));
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testExcept() throws QException
   {
      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withSubFilterSetOperator(QQueryFilter.SubFilterSetOperator.EXCEPT)
         .withSubFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.IN, 1, 2, 3)))
         .withSubFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.IN, 2)))
         .withOrderBy(new QFilterOrderBy("id", true))
      );

      QueryOutput queryOutput = new RDBMSQueryAction().execute(queryInput);
      assertEquals(2, queryOutput.getRecords().size(), "Expected # of rows");
      assertEquals(1, queryOutput.getRecords().get(0).getValueInteger("id"));
      assertEquals(3, queryOutput.getRecords().get(1).getValueInteger("id"));
   }



   /***************************************************************************
    ** Test UNION with virtual fields that have function parameters (e.g.,
    ** SubStringFunction with fromIndex and length params).  Each sub-query in
    ** the set operation needs to bind the function parameters independently.
    ***************************************************************************/
   @Test
   public void testUnionWithVirtualFieldFunctionParameters() throws QException
   {
      //////////////////////////////////////////////////////////////////////////////////////
      // add a virtual field using SubStringFunction with parameters to the person table  //
      //////////////////////////////////////////////////////////////////////////////////////
      QVirtualFieldMetaData firstNameSub3For2 = new QVirtualFieldMetaData("firstNameSub3For2", QFieldType.STRING)
         .withIsQueryCriteria(true)
         .withIsQuerySelectable(true)
         .withFieldFunction(new FieldFunction()
            .withFieldName("firstName")
            .withFunctionTypeIdentifier(SubStringFunction.IDENTIFIER)
            .withArguments(Map.of(SubStringFunction.FROM_INDEX_PARAM, 3, SubStringFunction.LENGTH_PARAM, 2)));

      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON)
         .withVirtualField(firstNameSub3For2);

      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // test data: Darin(id=1), James(id=2), Tim(id=3), Tyler(id=4), Garret(id=5)                                   //
      // SUBSTRING(firstName, 3, 2): Darin->ri, James->me, Tim->m, Tyler->le, Garret->rr                             //
      // sub-filter 1: firstNameSub3For2 = 'ri' => Darin (id=1)                                                      //
      // sub-filter 2: firstNameSub3For2 = 'le' => Tyler (id=4)                                                      //
      // UNION of those two should give us ids 1 and 4                                                                //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      QueryInput queryInput = initQueryRequest();
      queryInput.setFilter(new QQueryFilter()
         .withSubFilterSetOperator(QQueryFilter.SubFilterSetOperator.UNION)
         .withSubFilter(new QQueryFilter(new QFilterCriteria("firstNameSub3For2", QCriteriaOperator.EQUALS, "ri")))
         .withSubFilter(new QQueryFilter(new QFilterCriteria("firstNameSub3For2", QCriteriaOperator.EQUALS, "le")))
         .withOrderBy(new QFilterOrderBy("id", true))
      );

      QueryOutput queryOutput = new RDBMSQueryAction().execute(queryInput);
      assertEquals(2, queryOutput.getRecords().size(), "Expected # of rows");
      assertEquals(1, queryOutput.getRecords().get(0).getValueInteger("id"));
      assertEquals(4, queryOutput.getRecords().get(1).getValueInteger("id"));
   }

}
