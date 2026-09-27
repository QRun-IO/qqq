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

package com.kingsrook.qqq.backend.core.utils;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import org.junit.jupiter.api.Test;
import static com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.EQUALS;
import static com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.GREATER_THAN;
import static com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.IN;
import static com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.IS_NOT_BLANK;
import static com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.NOT_EQUALS;
import static com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.NOT_IN;
import static com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter.BooleanOperator.OR;
import static com.kingsrook.qqq.backend.core.utils.QQueryFilterDeduper.dedupeFilter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for QQueryFilterDeduper 
 *******************************************************************************/
class QQueryFilterDeduperTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDegenerateCases()
   {
      assertNull(dedupeFilter(null));

      QQueryFilter empty = new QQueryFilter();
      assertEquals(empty, dedupeFilter(empty));
      assertNotSame(empty, dedupeFilter(empty)); // method always clones, so, just assert that.
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSimpleFiltersWithNoChanges()
   {
      QQueryFilter oneCriteria = new QQueryFilter()
         .withCriteria(new QFilterCriteria("a", EQUALS, 1));
      assertEquals(oneCriteria, dedupeFilter(oneCriteria));
      assertNotSame(oneCriteria, dedupeFilter(oneCriteria));

      QQueryFilter twoCriteriaDifferentFields = new QQueryFilter()
         .withCriteria(new QFilterCriteria("a", EQUALS, 1))
         .withCriteria(new QFilterCriteria("b", GREATER_THAN, 2));
      assertEquals(twoCriteriaDifferentFields, dedupeFilter(twoCriteriaDifferentFields));
      assertNotSame(twoCriteriaDifferentFields, dedupeFilter(twoCriteriaDifferentFields));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOrs()
   {
      ///////////////////////////////////////////////////////
      // we've only written the simplest cases with ORs... //
      ///////////////////////////////////////////////////////
      assertEquals(new QQueryFilter().withBooleanOperator(OR).withCriteria(new QFilterCriteria("a", EQUALS, 1)), dedupeFilter(new QQueryFilter()
         .withBooleanOperator(OR)
         .withCriteria(new QFilterCriteria("a", EQUALS, 1))
         .withCriteria(new QFilterCriteria("a", EQUALS, 1))
         .withCriteria(new QFilterCriteria("a", EQUALS, 1))
      ));

      //////////////////////////////////////////////////////////////////////
      // just not built at this time - obviously, could become an IN list //
      //////////////////////////////////////////////////////////////////////
      QQueryFilter notSupportedOrTwoEquals = new QQueryFilter()
         .withBooleanOperator(OR)
         .withCriteria(new QFilterCriteria("f", EQUALS, 1))
         .withCriteria(new QFilterCriteria("f", EQUALS, 2));
      assertEquals(notSupportedOrTwoEquals, dedupeFilter(notSupportedOrTwoEquals));

      ///////////////////////////////////////////////////////////////////////////////////
      // I think the logic would be, that the EQUALS 1 would be removed (is redundant) //
      ///////////////////////////////////////////////////////////////////////////////////
      QQueryFilter notSupportedOrEqualsNotEquals = new QQueryFilter()
         .withBooleanOperator(OR)
         .withCriteria(new QFilterCriteria("f", EQUALS, 1))
         .withCriteria(new QFilterCriteria("f", NOT_EQUALS, 2));
      assertEquals(notSupportedOrEqualsNotEquals, dedupeFilter(notSupportedOrEqualsNotEquals));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMoreOperators()
   {
      //////////////////////////////////////////////////////////////////////
      // only simplest case (of criteria being .equals()) is supported... //
      //////////////////////////////////////////////////////////////////////
      assertEquals(new QQueryFilter().withCriteria(new QFilterCriteria("a", GREATER_THAN, 1)), dedupeFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("a", GREATER_THAN, 1))
         .withCriteria(new QFilterCriteria("a", GREATER_THAN, 1))
      ));

      ///////////////////////////////////////////////////////////////////////////////////
      // in theory, we could do more, but we just haven't yet (e.g, this could be > 5) //
      ///////////////////////////////////////////////////////////////////////////////////
      QQueryFilter tooComplex = new QQueryFilter()
         .withCriteria(new QFilterCriteria("f", GREATER_THAN, 1))
         .withCriteria(new QFilterCriteria("f", GREATER_THAN, 5));
      assertEquals(tooComplex, dedupeFilter(tooComplex));

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAllEquals()
   {
      assertEquals(new QQueryFilter().withCriteria(new QFilterCriteria("a", EQUALS, 1)), dedupeFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("a", EQUALS, 1))
         .withCriteria(new QFilterCriteria("a", EQUALS, 1))
      ));

      assertEquals(new QQueryFilter().withCriteria(new QFilterCriteria("a", EQUALS, 1)), dedupeFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("a", EQUALS, 1))
         .withCriteria(new QFilterCriteria("a", EQUALS, 1))
         .withCriteria(new QFilterCriteria("a", EQUALS, 1))
      ));

      assertEquals(new QQueryFilter()
            .withCriteria(new QFilterCriteria("a", EQUALS, 1))
            .withCriteria(new QFilterCriteria("b", EQUALS, 2))
            .withCriteria(new QFilterCriteria("c", EQUALS, 3)),
         dedupeFilter(new QQueryFilter()
            .withCriteria(new QFilterCriteria("a", EQUALS, 1))
            .withCriteria(new QFilterCriteria("b", EQUALS, 2))
            .withCriteria(new QFilterCriteria("a", EQUALS, 1))
            .withCriteria(new QFilterCriteria("b", EQUALS, 2))
            .withCriteria(new QFilterCriteria("b", EQUALS, 2))
            .withCriteria(new QFilterCriteria("a", EQUALS, 1))
            .withCriteria(new QFilterCriteria("c", EQUALS, 3))
            .withCriteria(new QFilterCriteria("c", EQUALS, 3))
            .withCriteria(new QFilterCriteria("c", EQUALS, 3))
         ));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testEqualsAndNotEqualsAndNotIn()
   {
      assertEquals(new QQueryFilter().withCriteria(new QFilterCriteria("f", EQUALS, 1)), dedupeFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("f", EQUALS, 1))
         .withCriteria(new QFilterCriteria("f", NOT_EQUALS, 2))
      ));

      assertEquals(new QQueryFilter().withCriteria(new QFilterCriteria("f", EQUALS, 1)), dedupeFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("f", EQUALS, 1))
         .withCriteria(new QFilterCriteria("f", NOT_EQUALS, 2))
         .withCriteria(new QFilterCriteria("f", NOT_EQUALS, 3))
      ));

      assertEquals(new QQueryFilter().withCriteria(new QFilterCriteria("f", EQUALS, 1)), dedupeFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("f", NOT_EQUALS, 2))
         .withCriteria(new QFilterCriteria("f", EQUALS, 1))
         .withCriteria(new QFilterCriteria("f", NOT_EQUALS, 3))
      ));

      assertEquals(new QQueryFilter().withCriteria(new QFilterCriteria("f", EQUALS, 1)), dedupeFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("f", NOT_EQUALS, 2))
         .withCriteria(new QFilterCriteria("f", NOT_EQUALS, 3))
         .withCriteria(new QFilterCriteria("f", EQUALS, 1))
      ));

      assertEquals(new QQueryFilter().withCriteria(new QFilterCriteria("f", EQUALS, 1)), dedupeFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("f", EQUALS, 1))
         .withCriteria(new QFilterCriteria("f", NOT_IN, 2, 3))
      ));

      assertEquals(new QQueryFilter().withCriteria(new QFilterCriteria("f", EQUALS, 1)), dedupeFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("f", EQUALS, 1))
         .withCriteria(new QFilterCriteria("f", NOT_IN, 2, 3))
         .withCriteria(new QFilterCriteria("f", NOT_EQUALS, 4))
      ));

      assertEquals(new QQueryFilter().withCriteria(new QFilterCriteria("f", EQUALS, 1)), dedupeFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("f", NOT_IN, 2, 3))
         .withCriteria(new QFilterCriteria("f", NOT_EQUALS, 4))
         .withCriteria(new QFilterCriteria("f", EQUALS, 1))
      ));

      assertEquals(new QQueryFilter().withCriteria(new QFilterCriteria("f", EQUALS, 1)), dedupeFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("f", NOT_IN, 2, 3))
         .withCriteria(new QFilterCriteria("f", EQUALS, 1))
         .withCriteria(new QFilterCriteria("f", NOT_EQUALS, 4))
      ));

      ////////////////////////////////////////////////////////////
      // this is a contradiction, so we choose not to dedupe it //
      ////////////////////////////////////////////////////////////
      QQueryFilter contradiction1 = new QQueryFilter()
         .withCriteria(new QFilterCriteria("f", EQUALS, 1))
         .withCriteria(new QFilterCriteria("f", NOT_EQUALS, 1));
      assertEquals(contradiction1, dedupeFilter(contradiction1));

      QQueryFilter contradiction2 = new QQueryFilter()
         .withCriteria(new QFilterCriteria("f", EQUALS, 1))
         .withCriteria(new QFilterCriteria("f", NOT_IN, 0, 1));
      assertEquals(contradiction2, dedupeFilter(contradiction2));

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // this case can collapse the two not-equals, but then fails to merge the equals with them, because they are a contradiction! //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      assertEquals(new QQueryFilter()
            .withCriteria(new QFilterCriteria("f", NOT_IN, 2, 3))
            .withCriteria(new QFilterCriteria("f", EQUALS, 2)),
         dedupeFilter(new QQueryFilter()
            .withCriteria(new QFilterCriteria("f", NOT_EQUALS, 2))
            .withCriteria(new QFilterCriteria("f", NOT_EQUALS, 3))
            .withCriteria(new QFilterCriteria("f", EQUALS, 2))
         ));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNotEqualsAndNotIn()
   {
      assertEquals(new QQueryFilter().withCriteria(new QFilterCriteria("f", NOT_IN, 1, 2, 3)), dedupeFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("f", NOT_EQUALS, 1))
         .withCriteria(new QFilterCriteria("f", NOT_EQUALS, 2))
         .withCriteria(new QFilterCriteria("f", NOT_EQUALS, 3))
      ));

      //////////////////////////////////////////////////////////////////////////////////////////
      // ideally, maybe, this would have the values ordered 1,2,3, but, is equivalent enough //
      //////////////////////////////////////////////////////////////////////////////////////////
      assertEquals(new QQueryFilter().withCriteria(new QFilterCriteria("f", NOT_IN, 2, 3, 1)), dedupeFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("f", NOT_EQUALS, 1))
         .withCriteria(new QFilterCriteria("f", NOT_IN, 2, 3))
      ));

      assertEquals(new QQueryFilter().withCriteria(new QFilterCriteria("f", NOT_IN, 2, 3, 1)), dedupeFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("f", NOT_IN, 2, 3))
         .withCriteria(new QFilterCriteria("f", NOT_EQUALS, 1))
      ));

      assertEquals(new QQueryFilter().withCriteria(new QFilterCriteria("f", NOT_IN, 1, 2, 3)), dedupeFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("f", NOT_IN, 1, 2))
         .withCriteria(new QFilterCriteria("f", NOT_IN, 2, 3))
      ));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInAndNotEquals()
   {
      assertEquals(new QQueryFilter().withCriteria(new QFilterCriteria("f", IN, 2, 3)), dedupeFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("f", NOT_EQUALS, 1))
         .withCriteria(new QFilterCriteria("f", IN, 2, 3))
      ));

      assertEquals(new QQueryFilter().withCriteria(new QFilterCriteria("f", IN, 2, 3)), dedupeFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("f", IN, 2, 3))
         .withCriteria(new QFilterCriteria("f", NOT_EQUALS, 1))
      ));

      QQueryFilter contradiction1 = new QQueryFilter()
         .withCriteria(new QFilterCriteria("f", NOT_EQUALS, 1))
         .withCriteria(new QFilterCriteria("f", IN, 1));
      assertEquals(contradiction1, dedupeFilter(contradiction1));

      QQueryFilter contradiction2 = new QQueryFilter()
         .withCriteria(new QFilterCriteria("f", IN, 1))
         .withCriteria(new QFilterCriteria("f", NOT_EQUALS, 1));
      assertEquals(contradiction2, dedupeFilter(contradiction2));

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMultipleInLists()
   {
      assertEquals(new QQueryFilter().withCriteria(new QFilterCriteria("f", IN, 2)), dedupeFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("f", IN, 1, 2))
         .withCriteria(new QFilterCriteria("f", IN, 2, 3))
      ));

      assertEquals(new QQueryFilter().withCriteria(new QFilterCriteria("f", IN, 3, 4)), dedupeFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("f", IN, 1, 2, 3, 4))
         .withCriteria(new QFilterCriteria("f", IN, 3, 4, 5, 6))
      ));

      assertEquals(new QQueryFilter().withCriteria(new QFilterCriteria("f", IN, 3)), dedupeFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("f", IN, 1, 2, 3, 4))
         .withCriteria(new QFilterCriteria("f", IN, 3, 4, 5, 6))
         .withCriteria(new QFilterCriteria("f", IN, 1, 3, 5, 7))
      ));

      ///////////////////////////////////////////////////////////////////
      // contradicting in-lists - we give up and refuse to simplify it //
      ///////////////////////////////////////////////////////////////////
      QQueryFilter contradiction = new QQueryFilter()
         .withCriteria(new QFilterCriteria("f", IN, 1, 2))
         .withCriteria(new QFilterCriteria("f", IN, 3, 4));
      assertEquals(contradiction, dedupeFilter(contradiction));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInAndIsNotBlank()
   {
      assertEquals(new QQueryFilter().withCriteria(new QFilterCriteria("f", IN, 1, 2)), dedupeFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("f", IN, 1, 2))
         .withCriteria(new QFilterCriteria("f", IS_NOT_BLANK))
      ));

      assertEquals(new QQueryFilter().withCriteria(new QFilterCriteria("f", IN, 1, 2)), dedupeFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("f", IS_NOT_BLANK))
         .withCriteria(new QFilterCriteria("f", IN, 1, 2))
      ));
   }

}