/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.modules.backend.implementations.utils;


import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.CriteriaOption;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.JoinsContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.SubStringFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.WeekdayOfDateFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.WeekdayOfDateTimeFunction;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import com.kingsrook.qqq.backend.core.utils.collections.ListBuilder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for BackendQueryFilterUtils
 *******************************************************************************/
class BackendQueryFilterUtilsTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDoesRecordMatch_emptyFilters()
   {
      assertTrue(BackendQueryFilterUtils.doesRecordMatch(null, new QRecord().withValue("a", 1)));
      assertTrue(BackendQueryFilterUtils.doesRecordMatch(new QQueryFilter(), new QRecord().withValue("a", 1)));
      assertTrue(BackendQueryFilterUtils.doesRecordMatch(new QQueryFilter().withSubFilters(ListBuilder.of(null)), new QRecord().withValue("a", 1)));
      assertTrue(BackendQueryFilterUtils.doesRecordMatch(new QQueryFilter().withSubFilters(List.of(new QQueryFilter())), new QRecord().withValue("a", 1)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDoesRecordMatch_singleAnd()
   {
      QQueryFilter filter = new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.AND)
         .withCriteria(new QFilterCriteria("a", QCriteriaOperator.EQUALS, 1));

      assertTrue(BackendQueryFilterUtils.doesRecordMatch(filter, new QRecord().withValue("a", 1)));
      assertFalse(BackendQueryFilterUtils.doesRecordMatch(filter, new QRecord().withValue("a", 2)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDoesRecordMatch_singleOr()
   {
      QQueryFilter filter = new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR)
         .withCriteria(new QFilterCriteria("a", QCriteriaOperator.EQUALS, 1));

      assertTrue(BackendQueryFilterUtils.doesRecordMatch(filter, new QRecord().withValue("a", 1)));
      assertFalse(BackendQueryFilterUtils.doesRecordMatch(filter, new QRecord().withValue("a", 2)));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testDoesRecordMatch_multipleAnd()
   {
      QQueryFilter filter = new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.AND)
         .withCriteria(new QFilterCriteria("a", QCriteriaOperator.EQUALS, 1))
         .withCriteria(new QFilterCriteria("b", QCriteriaOperator.EQUALS, 2));

      assertTrue(BackendQueryFilterUtils.doesRecordMatch(filter, new QRecord().withValue("a", 1).withValue("b", 2)));
      assertFalse(BackendQueryFilterUtils.doesRecordMatch(filter, new QRecord().withValue("a", 2).withValue("b", 2)));
      assertFalse(BackendQueryFilterUtils.doesRecordMatch(filter, new QRecord().withValue("a", 1).withValue("b", 1)));
      assertFalse(BackendQueryFilterUtils.doesRecordMatch(filter, new QRecord()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testDoesRecordMatch_multipleOr()
   {
      QQueryFilter filter = new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR)
         .withCriteria(new QFilterCriteria("a", QCriteriaOperator.EQUALS, 1))
         .withCriteria(new QFilterCriteria("b", QCriteriaOperator.EQUALS, 2));

      assertTrue(BackendQueryFilterUtils.doesRecordMatch(filter, new QRecord().withValue("a", 1).withValue("b", 2)));
      assertTrue(BackendQueryFilterUtils.doesRecordMatch(filter, new QRecord().withValue("a", 2).withValue("b", 2)));
      assertTrue(BackendQueryFilterUtils.doesRecordMatch(filter, new QRecord().withValue("a", 1).withValue("b", 1)));
      assertFalse(BackendQueryFilterUtils.doesRecordMatch(filter, new QRecord().withValue("a", 3).withValue("b", 4)));
      assertFalse(BackendQueryFilterUtils.doesRecordMatch(filter, new QRecord()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testDoesRecordMatch_subFilterAnd()
   {
      QQueryFilter filter = new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.AND)
         .withSubFilters(List.of(
            new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.AND)
               .withCriteria(new QFilterCriteria("a", QCriteriaOperator.EQUALS, 1)),
            new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.AND)
               .withCriteria(new QFilterCriteria("b", QCriteriaOperator.EQUALS, 2))
         ));

      assertTrue(BackendQueryFilterUtils.doesRecordMatch(filter, new QRecord().withValue("a", 1).withValue("b", 2)));
      assertFalse(BackendQueryFilterUtils.doesRecordMatch(filter, new QRecord().withValue("a", 2).withValue("b", 2)));
      assertFalse(BackendQueryFilterUtils.doesRecordMatch(filter, new QRecord().withValue("a", 1).withValue("b", 1)));
      assertFalse(BackendQueryFilterUtils.doesRecordMatch(filter, new QRecord()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testDoesRecordMatch_subFilterOr()
   {
      QQueryFilter filter = new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR)
         .withSubFilters(List.of(
            new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR)
               .withCriteria(new QFilterCriteria("a", QCriteriaOperator.EQUALS, 1)),
            new QQueryFilter()
               .withCriteria(new QFilterCriteria("b", QCriteriaOperator.EQUALS, 2))
         ));

      assertTrue(BackendQueryFilterUtils.doesRecordMatch(filter, new QRecord().withValue("a", 1).withValue("b", 2)));
      assertTrue(BackendQueryFilterUtils.doesRecordMatch(filter, new QRecord().withValue("a", 2).withValue("b", 2)));
      assertTrue(BackendQueryFilterUtils.doesRecordMatch(filter, new QRecord().withValue("a", 1).withValue("b", 1)));
      assertFalse(BackendQueryFilterUtils.doesRecordMatch(filter, new QRecord().withValue("a", 3).withValue("b", 4)));
      assertFalse(BackendQueryFilterUtils.doesRecordMatch(filter, new QRecord()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDoesRecordMatch_criteriaHasTableNameNoFieldsDo()
   {
      QQueryFilter filter = new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.AND)
         .withCriteria(new QFilterCriteria("t.a", QCriteriaOperator.EQUALS, 1));
      assertTrue(BackendQueryFilterUtils.doesRecordMatch(filter, new QRecord().withValue("a", 1)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDoesRecordMatch_criteriaHasTableNameSomeFieldsDo()
   {
      QQueryFilter filter = new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.AND)
         .withCriteria(new QFilterCriteria("t.a", QCriteriaOperator.EQUALS, 1));

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // shouldn't find the "a", because "some" fields in here have a prefix (e.g., 's' was a join table, selected with 't' as the main table, which didn't prefix) //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      assertFalse(BackendQueryFilterUtils.doesRecordMatch(filter, new QRecord().withValue("a", 1).withValue("s.b", 2)));

      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // but this case (contrasted with above) set the record's tableName to "t", so criteria on "t.a" should find field "a" //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      assertTrue(BackendQueryFilterUtils.doesRecordMatch(filter, new QRecord().withTableName("t").withValue("a", 1).withValue("s.b", 2)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDoesRecordMatch_criteriaHasTableNameMatchingField()
   {
      QQueryFilter filter = new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.AND)
         .withCriteria(new QFilterCriteria("t.a", QCriteriaOperator.EQUALS, 1));
      assertTrue(BackendQueryFilterUtils.doesRecordMatch(filter, new QRecord().withValue("t.a", 1)));
      assertFalse(BackendQueryFilterUtils.doesRecordMatch(filter, new QRecord().withValue("t.b", 1)));
      assertFalse(BackendQueryFilterUtils.doesRecordMatch(filter, new QRecord().withValue("s.a", 1)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDoesCriterionMatch()
   {
      //////////////////////////////////////////////
      // < and > w/ mix of numbers and strings... //
      //////////////////////////////////////////////
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.GREATER_THAN, 1), "f", "2"));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.GREATER_THAN, "1"), "f", 2));
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.GREATER_THAN, 1), "f", "1"));
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.GREATER_THAN, "1"), "f", 1));
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.GREATER_THAN, 1), "f", "0"));
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.GREATER_THAN, "1"), "f", 0));

      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.GREATER_THAN_OR_EQUALS, 1), "f", "2"));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.GREATER_THAN_OR_EQUALS, "1"), "f", 2));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.GREATER_THAN_OR_EQUALS, 1), "f", "1"));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.GREATER_THAN_OR_EQUALS, "1"), "f", 1));
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.GREATER_THAN_OR_EQUALS, 1), "f", "0"));
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.GREATER_THAN_OR_EQUALS, "1"), "f", 0));

      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.LESS_THAN, 2), "f", "1"));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.LESS_THAN, "2"), "f", 1));
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.LESS_THAN, 1), "f", "1"));
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.LESS_THAN, "1"), "f", 1));
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.LESS_THAN, 0), "f", "1"));
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.LESS_THAN, "0"), "f", 1));

      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.LESS_THAN_OR_EQUALS, 2), "f", "1"));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.LESS_THAN_OR_EQUALS, "2"), "f", 1));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.LESS_THAN_OR_EQUALS, 1), "f", "1"));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.LESS_THAN_OR_EQUALS, "1"), "f", 1));
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.LESS_THAN_OR_EQUALS, 0), "f", "1"));
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.LESS_THAN_OR_EQUALS, "0"), "f", 1));

      /////////////////////////////
      // between and not-between //
      /////////////////////////////
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.BETWEEN, List.of(1, 3)), "f", 0));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.BETWEEN, List.of(1, 3)), "f", 1));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.BETWEEN, List.of(1, 3)), "f", 2));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.BETWEEN, List.of(1, 3)), "f", 3));
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.BETWEEN, List.of(1, 3)), "f", 4));

      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.NOT_BETWEEN, List.of(1, 3)), "f", 0));
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.NOT_BETWEEN, List.of(1, 3)), "f", 1));
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.NOT_BETWEEN, List.of(1, 3)), "f", 2));
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.NOT_BETWEEN, List.of(1, 3)), "f", 3));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.NOT_BETWEEN, List.of(1, 3)), "f", 4));

      ////////////////
      // like & not //
      ////////////////
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.LIKE, "Test"), "f", "Test"));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.LIKE, "T%"), "f", "Test"));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.LIKE, "T_st"), "f", "Test"));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.NOT_LIKE, "Test"), "f", "Tst"));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.NOT_LIKE, "T%"), "f", "Rest"));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.NOT_LIKE, "T_st"), "f", "Toast"));

      //////////////
      // IN & NOT //
      //////////////
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.IN, "A"), "f", "A"));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.IN, "A", "B"), "f", "A"));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.IN, "A", "B"), "f", "B"));
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.IN, List.of()), "f", "A"));
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.IN, ListBuilder.of(null)), "f", "A"));

      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.NOT_IN, "A"), "f", "A"));
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.NOT_IN, "A", "B"), "f", "A"));
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.NOT_IN, "A", "B"), "f", "B"));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.NOT_IN, List.of()), "f", "A"));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.NOT_IN, ListBuilder.of(null)), "f", "A"));

      ///////////////////////////
      // NOT_EQUALS_OR_IS_NULL //
      ///////////////////////////
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.NOT_EQUALS_OR_IS_NULL, "A"), "f", "A"));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.NOT_EQUALS_OR_IS_NULL, "A"), "f", "B"));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria("f", QCriteriaOperator.NOT_EQUALS_OR_IS_NULL, "A"), "f", null));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private QFilterCriteria newCaseInsensitiveCriteria(String fieldName, QCriteriaOperator operator, Serializable... values)
   {
      return new QFilterCriteria(fieldName, operator, values).withOption(CriteriaOption.CASE_INSENSITIVE);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private QFilterCriteria newCaseInsensitiveCriteria(String fieldName, QCriteriaOperator operator, List<Serializable> values)
   {
      return new QFilterCriteria(fieldName, operator, values).withOption(CriteriaOption.CASE_INSENSITIVE);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDoesCriterionMatchCaseInsensitive()
   {
      ////////////////
      // like & not //
      ////////////////
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(newCaseInsensitiveCriteria("f", QCriteriaOperator.LIKE, "Test"), "f", "test"));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(newCaseInsensitiveCriteria("f", QCriteriaOperator.LIKE, "test"), "f", "Test"));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(newCaseInsensitiveCriteria("f", QCriteriaOperator.LIKE, "T%"), "f", "test"));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(newCaseInsensitiveCriteria("f", QCriteriaOperator.LIKE, "t%"), "f", "Test"));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(newCaseInsensitiveCriteria("f", QCriteriaOperator.LIKE, "T_st"), "f", "test"));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(newCaseInsensitiveCriteria("f", QCriteriaOperator.LIKE, "t_st"), "f", "Test"));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(newCaseInsensitiveCriteria("f", QCriteriaOperator.NOT_LIKE, "Test"), "f", "Tst"));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(newCaseInsensitiveCriteria("f", QCriteriaOperator.NOT_LIKE, "Test"), "f", "tst"));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(newCaseInsensitiveCriteria("f", QCriteriaOperator.NOT_LIKE, "T%"), "f", "Rest"));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(newCaseInsensitiveCriteria("f", QCriteriaOperator.NOT_LIKE, "T_st"), "f", "Toast"));

      //////////////
      // IN & NOT //
      //////////////
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(newCaseInsensitiveCriteria("f", QCriteriaOperator.IN, "A"), "f", "a"));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(newCaseInsensitiveCriteria("f", QCriteriaOperator.IN, "a"), "f", "A"));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(newCaseInsensitiveCriteria("f", QCriteriaOperator.IN, "A", "B"), "f", "a"));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(newCaseInsensitiveCriteria("f", QCriteriaOperator.IN, "A", "b"), "f", "B"));
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(newCaseInsensitiveCriteria("f", QCriteriaOperator.IN, List.of()), "f", "A"));
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(newCaseInsensitiveCriteria("f", QCriteriaOperator.IN, ListBuilder.of(null)), "f", "A"));

      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(newCaseInsensitiveCriteria("f", QCriteriaOperator.NOT_IN, "A"), "f", "A"));
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(newCaseInsensitiveCriteria("f", QCriteriaOperator.NOT_IN, "A", "B"), "f", "a"));
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(newCaseInsensitiveCriteria("f", QCriteriaOperator.NOT_IN, "A", "b"), "f", "B"));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(newCaseInsensitiveCriteria("f", QCriteriaOperator.NOT_IN, List.of()), "f", "A"));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(newCaseInsensitiveCriteria("f", QCriteriaOperator.NOT_IN, ListBuilder.of(null)), "f", "A"));

      ///////////////////////////
      // NOT_EQUALS_OR_IS_NULL //
      ///////////////////////////
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(newCaseInsensitiveCriteria("f", QCriteriaOperator.NOT_EQUALS_OR_IS_NULL, "A"), "f", "A"));
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(newCaseInsensitiveCriteria("f", QCriteriaOperator.NOT_EQUALS_OR_IS_NULL, "A"), "f", "a"));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(newCaseInsensitiveCriteria("f", QCriteriaOperator.NOT_EQUALS_OR_IS_NULL, "A"), "f", "B"));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(newCaseInsensitiveCriteria("f", QCriteriaOperator.NOT_EQUALS_OR_IS_NULL, "A"), "f", null));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testLikeDarPercent()
   {
      String pattern = BackendQueryFilterUtils.sqlLikeToRegex("Dar%");
      assertTrue("Darin".matches(pattern));
      assertTrue("Dar".matches(pattern));
      assertFalse("Not Darin".matches(pattern));
      assertFalse("David".matches(pattern));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testLikeDPercentIn()
   {
      String pattern = BackendQueryFilterUtils.sqlLikeToRegex("D%in");
      assertTrue("Darin".matches(pattern));
      assertFalse("Dar".matches(pattern));
      assertFalse("Not Darin".matches(pattern));
      assertTrue("Davin".matches(pattern));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testLikeDPercentUnderscoreN()
   {
      String pattern = BackendQueryFilterUtils.sqlLikeToRegex("D%_n");
      assertTrue("Darin".matches(pattern));
      assertTrue("Daron".matches(pattern));
      assertTrue("Dan".matches(pattern));
      assertFalse("Dar".matches(pattern));
      assertFalse("Not Darin".matches(pattern));
      assertTrue("Davin".matches(pattern));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testLikeDarUnderscore()
   {
      String pattern = BackendQueryFilterUtils.sqlLikeToRegex("Dar_");
      assertFalse("Darin".matches(pattern));
      assertFalse("Dar".matches(pattern));
      assertTrue("Dart".matches(pattern));
      assertFalse("Not Darin".matches(pattern));
      assertFalse("David".matches(pattern));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDoesCriteriaMatchWithFieldFunctions() throws Exception
   {
      QFieldMetaData firstNameField = new QFieldMetaData("firstName", QFieldType.STRING);
      QFieldMetaData birthDateField = new QFieldMetaData("birthDate", QFieldType.DATE);
      QVirtualFieldMetaData firstInitialField = new QVirtualFieldMetaData("firstInitial", QFieldType.STRING)
         .withIsQueryCriteria(true)
         .withFieldFunction(new FieldFunction().withFunctionTypeIdentifier(SubStringFunction.IDENTIFIER).withFieldName(firstNameField.getName())
            .withArguments(Map.of(SubStringFunction.FROM_INDEX_PARAM, 1, SubStringFunction.LENGTH_PARAM, 1)));

      QRecord darinRecord     = new QRecord().withValue("firstName", "Darin").withValue("birthDate", LocalDate.of(1980, Month.MAY, 31));
      QRecord timRecord       = new QRecord().withValue("firstName", "Tim").withValue("birthDate", LocalDate.of(1976, Month.MAY, 28));
      QRecord anonymousRecord = new QRecord();

      FieldFunction substring2 = new FieldFunction().withFunctionTypeIdentifier(SubStringFunction.IDENTIFIER).withFieldName(firstNameField.getName())
         .withArguments(Map.of(SubStringFunction.FROM_INDEX_PARAM, 2));

      FieldFunction substring2For1 = new FieldFunction().withFunctionTypeIdentifier(SubStringFunction.IDENTIFIER).withFieldName(firstNameField.getName())
         .withArguments(Map.of(SubStringFunction.FROM_INDEX_PARAM, 2, SubStringFunction.LENGTH_PARAM, 1));

      FieldFunction weekdayOfDate  = new FieldFunction().withFunctionTypeIdentifier(WeekdayOfDateFunction.IDENTIFIER).withFieldName(birthDateField.getName());

      //////////////////////////////////////////
      // field function specified in criteria //
      //////////////////////////////////////////
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria(firstNameField.getName(), QCriteriaOperator.EQUALS, "arin").withFieldFunction(substring2), firstNameField, darinRecord));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria(firstNameField.getName(), QCriteriaOperator.EQUALS, "a").withFieldFunction(substring2For1), firstNameField, darinRecord));
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria(firstNameField.getName(), QCriteriaOperator.EQUALS, "arin").withFieldFunction(substring2), firstNameField, timRecord));
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria(firstNameField.getName(), QCriteriaOperator.EQUALS, "a").withFieldFunction(substring2For1), firstNameField, timRecord));
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria(firstNameField.getName(), QCriteriaOperator.EQUALS, "a").withFieldFunction(substring2For1), firstNameField, anonymousRecord));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria(firstNameField.getName(), QCriteriaOperator.IS_BLANK).withFieldFunction(substring2For1), firstNameField, anonymousRecord));

      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria(birthDateField.getName(), QCriteriaOperator.EQUALS, DayOfWeek.SATURDAY.getValue()).withFieldFunction(weekdayOfDate), birthDateField, darinRecord));
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria(birthDateField.getName(), QCriteriaOperator.EQUALS, DayOfWeek.SATURDAY.getValue()).withFieldFunction(weekdayOfDate), birthDateField, timRecord));
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria(birthDateField.getName(), QCriteriaOperator.EQUALS, DayOfWeek.FRIDAY.getValue()).withFieldFunction(weekdayOfDate), birthDateField, darinRecord));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria(birthDateField.getName(), QCriteriaOperator.EQUALS, DayOfWeek.FRIDAY.getValue()).withFieldFunction(weekdayOfDate), birthDateField, timRecord));
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria(birthDateField.getName(), QCriteriaOperator.EQUALS, DayOfWeek.FRIDAY.getValue()).withFieldFunction(weekdayOfDate), birthDateField, anonymousRecord));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria(birthDateField.getName(), QCriteriaOperator.IS_BLANK).withFieldFunction(weekdayOfDate), birthDateField, anonymousRecord));

      ////////////////////////////////////////////////
      // implicit field function from virtual field //
      ////////////////////////////////////////////////
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria(firstInitialField.getName(), QCriteriaOperator.EQUALS, "D"), firstInitialField, darinRecord));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria(firstInitialField.getName(), QCriteriaOperator.EQUALS, "T"), firstInitialField, timRecord));
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria(firstInitialField.getName(), QCriteriaOperator.EQUALS, "T"), firstInitialField, darinRecord));
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria(firstInitialField.getName(), QCriteriaOperator.EQUALS, "D"), firstInitialField, timRecord));
      assertFalse(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria(firstInitialField.getName(), QCriteriaOperator.EQUALS, "A"), firstInitialField, anonymousRecord));
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria(firstInitialField.getName(), QCriteriaOperator.IS_BLANK), firstInitialField, anonymousRecord));

      ////////////////////////////////////////////////////////////////////
      // field function on criteria takes precedence over virtual field //
      ////////////////////////////////////////////////////////////////////
      assertTrue(BackendQueryFilterUtils.doesCriteriaMatch(new QFilterCriteria(firstInitialField.getName(), QCriteriaOperator.EQUALS, "arin").withFieldFunction(substring2), firstInitialField, darinRecord));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testApplyBooleanOperator()
   {
      /////////////////////////////
      // tests for operator: AND //
      /////////////////////////////
      {
         /////////////////////////////////////////////////////////////////////////////////////
         // old value was true; new value is true.                                          //
         // result should be true, and we should not be short-circuited (return value null) //
         /////////////////////////////////////////////////////////////////////////////////////
         AtomicBoolean accumulator = new AtomicBoolean(true);
         assertNull(BackendQueryFilterUtils.applyBooleanOperator(accumulator, true, QQueryFilter.BooleanOperator.AND));
         assertTrue(accumulator.getPlain());
      }
      {
         //////////////////////////////////////////////////////////////////////////////////////
         // old value was true; new value is false.                                          //
         // result should be false, and we should be short-circuited (return value not-null) //
         //////////////////////////////////////////////////////////////////////////////////////
         AtomicBoolean accumulator = new AtomicBoolean(true);
         assertEquals(Boolean.FALSE, BackendQueryFilterUtils.applyBooleanOperator(accumulator, false, QQueryFilter.BooleanOperator.AND));
         assertFalse(accumulator.getPlain());
      }
      {
         //////////////////////////////////////////////////////////////////////////////////////
         // old value was false; new value is true.                                          //
         // result should be false, and we should be short-circuited (return value not-null) //
         //////////////////////////////////////////////////////////////////////////////////////
         AtomicBoolean accumulator = new AtomicBoolean(false);
         assertEquals(Boolean.FALSE, BackendQueryFilterUtils.applyBooleanOperator(accumulator, true, QQueryFilter.BooleanOperator.AND));
         assertFalse(accumulator.getPlain());
      }
      {
         //////////////////////////////////////////////////////////////////////////////////////
         // old value was false; new value is false.                                         //
         // result should be false, and we should be short-circuited (return value not-null) //
         //////////////////////////////////////////////////////////////////////////////////////
         AtomicBoolean accumulator = new AtomicBoolean(false);
         assertEquals(Boolean.FALSE, BackendQueryFilterUtils.applyBooleanOperator(accumulator, false, QQueryFilter.BooleanOperator.AND));
         assertFalse(accumulator.getPlain());
      }

      ////////////////////////////
      // tests for operator: OR //
      ////////////////////////////
      {
         /////////////////////////////////////////////////////////////////////////////////////
         // old value was true; new value is true.                                          //
         // result should be true, and we should be short-circuited (return value not-null) //
         /////////////////////////////////////////////////////////////////////////////////////
         AtomicBoolean accumulator = new AtomicBoolean(true);
         assertEquals(Boolean.TRUE, BackendQueryFilterUtils.applyBooleanOperator(accumulator, true, QQueryFilter.BooleanOperator.OR));
         assertTrue(accumulator.getPlain());
      }
      {
         //////////////////////////////////////////////////////////////////////////////////////
         // old value was true; new value is false.                                          //
         // result should be true, and we should be short-circuited (return value not-null) //
         //////////////////////////////////////////////////////////////////////////////////////
         AtomicBoolean accumulator = new AtomicBoolean(true);
         assertEquals(Boolean.TRUE, BackendQueryFilterUtils.applyBooleanOperator(accumulator, false, QQueryFilter.BooleanOperator.OR));
         assertTrue(accumulator.getPlain());
      }
      {
         //////////////////////////////////////////////////////////////////////////////////////
         // old value was false; new value is true.                                          //
         // result should be false, and we should be short-circuited (return value not-null) //
         //////////////////////////////////////////////////////////////////////////////////////
         AtomicBoolean accumulator = new AtomicBoolean(false);
         assertEquals(Boolean.TRUE, BackendQueryFilterUtils.applyBooleanOperator(accumulator, true, QQueryFilter.BooleanOperator.OR));
         assertTrue(accumulator.getPlain());
      }
      {
         //////////////////////////////////////////////////////////////////////////////////////
         // old value was false; new value is false.                                         //
         // result should be false, and we should not be short-circuited (return value null) //
         //////////////////////////////////////////////////////////////////////////////////////
         AtomicBoolean accumulator = new AtomicBoolean(false);
         assertNull(BackendQueryFilterUtils.applyBooleanOperator(accumulator, false, QQueryFilter.BooleanOperator.OR));
         assertFalse(accumulator.getPlain());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testIdentifyJoinTablesInFilter() throws QException
   {
      QFilterCriteria lineItemCriteria               = new QFilterCriteria(TestUtils.TABLE_NAME_LINE_ITEM + ".sku", QCriteriaOperator.EQUALS, "BASIC1");
      QFilterCriteria lineItemCriteriaOtherFieldName = new QFilterCriteria("orderNo", QCriteriaOperator.EQUALS).withOtherFieldName(TestUtils.TABLE_NAME_LINE_ITEM + ".sku");

      assertEquals(Set.of(), BackendQueryFilterUtils.identifyJoinTablesInFilter(TestUtils.TABLE_NAME_ORDER,
         new QQueryFilter()));

      assertEquals(Set.of(), BackendQueryFilterUtils.identifyJoinTablesInFilter(TestUtils.TABLE_NAME_ORDER,
         new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.IS_NOT_BLANK))));

      assertEquals(Set.of(TestUtils.TABLE_NAME_LINE_ITEM), BackendQueryFilterUtils.identifyJoinTablesInFilter(TestUtils.TABLE_NAME_ORDER,
         new QQueryFilter().withCriteria(lineItemCriteria)));

      assertEquals(Set.of(TestUtils.TABLE_NAME_LINE_ITEM), BackendQueryFilterUtils.identifyJoinTablesInFilter(TestUtils.TABLE_NAME_ORDER,
         new QQueryFilter().withCriteria(lineItemCriteriaOtherFieldName)));

      assertEquals(Set.of(TestUtils.TABLE_NAME_LINE_ITEM), BackendQueryFilterUtils.identifyJoinTablesInFilter(TestUtils.TABLE_NAME_ORDER,
         new QQueryFilter().withSubFilter(new QQueryFilter().withCriteria(lineItemCriteria))));

      assertEquals(Set.of(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC), BackendQueryFilterUtils.identifyJoinTablesInFilter(TestUtils.TABLE_NAME_ORDER,
         new QQueryFilter().withOrderBy(new QFilterOrderBy(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC + ".id"))));

      assertEquals(Set.of(TestUtils.TABLE_NAME_LINE_ITEM, TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC), BackendQueryFilterUtils.identifyJoinTablesInFilter(TestUtils.TABLE_NAME_ORDER,
         new QQueryFilter()
            .withCriteria(lineItemCriteria)
            .withOrderBy(new QFilterOrderBy(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC + ".id"))));

      //////////////////////////////////////////////////////////////////////////
      // virtual fields in criteria should not cause an exception (bug fix)   //
      // previously, identifyJoinTablesInFilter used FieldAndJoinTable.get()  //
      // without allowVirtualFields=true, so virtual field names would throw. //
      //////////////////////////////////////////////////////////////////////////
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_ORDER)
         .withVirtualField(new QVirtualFieldMetaData("firstInitial", QFieldType.STRING)
            .withIsQueryCriteria(true)
            .withFieldFunction(new FieldFunction().withFunctionTypeIdentifier(SubStringFunction.IDENTIFIER).withFieldName("orderNo")
               .withArguments(Map.of(SubStringFunction.FROM_INDEX_PARAM, 1, SubStringFunction.LENGTH_PARAM, 1))));

      assertEquals(Set.of(), BackendQueryFilterUtils.identifyJoinTablesInFilter(TestUtils.TABLE_NAME_ORDER,
         new QQueryFilter(new QFilterCriteria("firstInitial", QCriteriaOperator.EQUALS, "A"))));

      assertEquals(Set.of(), BackendQueryFilterUtils.identifyJoinTablesInFilter(TestUtils.TABLE_NAME_ORDER,
         new QQueryFilter(new QFilterCriteria("orderNo", QCriteriaOperator.EQUALS).withOtherFieldName("firstInitial"))));

   }



   /*******************************************************************************
    ** A joined function sees its local datetime and secondary timezone field.
    ** Ordinary filters use ISO weekdays, while sorting retains Sunday-first.
    ** Qualified function sources and root-qualified references remain compatible.
    *******************************************************************************/
   @Test
   void testJoinedWeekdaySecondaryFieldAndSortingCompatibility() throws Exception
   {
      QInstance instance = QContext.getQInstance();
      Map<String, QTableMetaData> originalTables = instance.getTables();
      String originalMetadata = JsonUtils.toJson(originalTables);
      Map<String, QTableMetaData> tables = new LinkedHashMap<>(originalTables);
      Map<String, Serializable> arguments = Map.of(WeekdayOfDateTimeFunction.PARAM_TIME_ZONE_ID, "UTC",
         WeekdayOfDateTimeFunction.PARAM_ZONE_ID_FROM_FIELD_NAME, "zone",
         WeekdayOfDateTimeFunction.PARAM_USE_SESSION_ZONE_ID, false,
         WeekdayOfDateTimeFunction.PARAM_SORT_SUNDAY_FIRST, true);
      FieldFunction localFunction = new FieldFunction().withFunctionTypeIdentifier(WeekdayOfDateTimeFunction.IDENTIFIER)
         .withFieldName("timestamp").withArguments(arguments);
      FieldFunction qualifiedFunction = new FieldFunction().withFunctionTypeIdentifier(WeekdayOfDateTimeFunction.IDENTIFIER)
         .withFieldName("owner.timestamp").withArguments(Map.of(WeekdayOfDateTimeFunction.PARAM_TIME_ZONE_ID, "UTC",
            WeekdayOfDateTimeFunction.PARAM_ZONE_ID_FROM_FIELD_NAME, "owner.zone",
            WeekdayOfDateTimeFunction.PARAM_USE_SESSION_ZONE_ID, false,
            WeekdayOfDateTimeFunction.PARAM_SORT_SUNDAY_FIRST, true));
      for(String tableName : List.of(TestUtils.TABLE_NAME_ORDER, TestUtils.TABLE_NAME_LINE_ITEM))
      {
         tables.put(tableName, tables.get(tableName).clone().withRecordSecurityLocks(List.of())
            .withField(new QFieldMetaData("zone", QFieldType.STRING))
            .withVirtualField(new QVirtualFieldMetaData("weekday", QFieldType.INTEGER).withIsQueryCriteria(true).withIsQuerySelectable(true)
               .withFieldFunction(localFunction)));
      }
      instance.setTables(tables);
      try
      {
         JoinsContext joins = new JoinsContext(instance, TestUtils.TABLE_NAME_ORDER,
            new ArrayList<>(List.of(new QueryJoin(TestUtils.TABLE_NAME_LINE_ITEM).withAlias("owner").withType(QueryJoin.Type.INNER))), new QQueryFilter());
         Instant sundayInChildZone = Instant.parse("2026-03-02T00:30:00Z");
         Instant mondayInChildZone = Instant.parse("2026-03-02T12:30:00Z");
         QRecord childSunday = new QRecord().withValue("timestamp", sundayInChildZone).withValue("zone", "America/Los_Angeles");
         QRecord childMonday = new QRecord().withValue("timestamp", mondayInChildZone).withValue("zone", "America/Los_Angeles");
         WeekdayOfDateTimeFunction type = new WeekdayOfDateTimeFunction();
         assertAll(
            () -> assertEquals(7, type.apply(localFunction, childSunday)),
            () -> assertEquals(1, type.apply(localFunction, childMonday)),
            () -> assertEquals(0, type.applyForSorting(localFunction, childSunday)),
            () -> assertEquals(1, type.applyForSorting(localFunction, childMonday)));
         QRecord sunday = new QRecord().withTableName(TestUtils.TABLE_NAME_ORDER).withValue("id", 2)
            .withValue("timestamp", Instant.parse("2026-03-03T12:00:00Z")).withValue("zone", "UTC")
            .withValue("owner.timestamp", sundayInChildZone).withValue("owner.zone", "America/Los_Angeles");
         QRecord monday = new QRecord().withTableName(TestUtils.TABLE_NAME_ORDER).withValue("id", 1)
            .withValue("timestamp", Instant.parse("2026-03-01T12:00:00Z")).withValue("zone", "UTC")
            .withValue("owner.timestamp", mondayInChildZone).withValue("owner.zone", "America/Los_Angeles");
         QRecord absentChild = new QRecord(sunday);
         absentChild.removeValue("owner.timestamp");
         QQueryFilter local = new QQueryFilter(new QFilterCriteria("owner.weekday", QCriteriaOperator.EQUALS, 7));
         QQueryFilter nested = new QQueryFilter().withSubFilter(local.clone());
         QQueryFilter qualified = new QQueryFilter(new QFilterCriteria("owner.timestamp", QCriteriaOperator.EQUALS, 7).withFieldFunction(qualifiedFunction));
         QQueryFilter root = new QQueryFilter(new QFilterCriteria("weekday", QCriteriaOperator.EQUALS, 2));
         QQueryFilter qualifiedRoot = new QQueryFilter(new QFilterCriteria(TestUtils.TABLE_NAME_ORDER + ".weekday", QCriteriaOperator.EQUALS, 2));
         QQueryFilter missing = new QQueryFilter(new QFilterCriteria("owner.weekday", QCriteriaOperator.IS_BLANK));
         QQueryFilter unknown = new QQueryFilter(new QFilterCriteria("ghost.timestamp", QCriteriaOperator.EQUALS, 2).withFieldFunction(localFunction));
         QQueryFilter childSort = new QQueryFilter().withOrderBy(new QFilterOrderBy("owner.weekday")).withOrderBy(new QFilterOrderBy("id"));
         QQueryFilter rootSort = new QQueryFilter().withOrderBy(new QFilterOrderBy(TestUtils.TABLE_NAME_ORDER + ".weekday")).withOrderBy(new QFilterOrderBy("id"));
         List<QQueryFilter> filters = List.of(local, nested, qualified, root, qualifiedRoot, missing, unknown, childSort, rootSort);
         List<QRecord> records = List.of(sunday, monday, absentChild, childSunday, childMonday);
         String filtersBefore = JsonUtils.toJson(filters);
         String recordsBefore = JsonUtils.toJson(records);
         String functionsBefore = JsonUtils.toJson(List.of(localFunction, qualifiedFunction));
         String metadataBefore = JsonUtils.toJson(tables);
         String joinsBefore = JsonUtils.toJson(joins.getQueryJoins());
         assertAll(
            () -> assertTrue(BackendQueryFilterUtils.doesRecordMatch(local, joins, sunday), "Joined local datetime and secondary zone"),
            () -> assertFalse(BackendQueryFilterUtils.doesRecordMatch(local, joins, monday), "Joined Monday control"),
            () -> assertTrue(BackendQueryFilterUtils.doesRecordMatch(nested, joins, sunday), "Nested joined function"),
            () -> assertTrue(BackendQueryFilterUtils.doesRecordMatch(qualified, joins, sunday), "Already-qualified function source and zone"),
            () -> assertTrue(BackendQueryFilterUtils.doesRecordMatch(root, joins, sunday), "Unqualified root function"),
            () -> assertTrue(BackendQueryFilterUtils.doesRecordMatch(qualifiedRoot, joins, sunday), "Qualified root function"),
            () -> assertTrue(BackendQueryFilterUtils.doesRecordMatch(missing, joins, absentChild), "Absent child cannot fall back to root"),
            () -> assertFalse(BackendQueryFilterUtils.doesRecordMatch(unknown, joins, sunday), "Unknown prefix cannot evaluate root local function"),
            () ->
            {
               List<QRecord> sorted = new ArrayList<>(List.of(monday, sunday));
               BackendQueryFilterUtils.sortRecordList(joins, childSort, sorted);
               assertEquals(List.of(2, 1), sorted.stream().map(record -> record.getValueInteger("id")).toList(), "Child Sunday sorts before Monday using applyForSorting");
            },
            () ->
            {
               List<QRecord> sorted = new ArrayList<>(List.of(sunday, monday));
               BackendQueryFilterUtils.sortRecordList(joins, rootSort, sorted);
               assertEquals(List.of(1, 2), sorted.stream().map(record -> record.getValueInteger("id")).toList(), "Root-qualified Sunday-first compatibility");
            },
            () -> assertEquals(filtersBefore, JsonUtils.toJson(filters)),
            () -> assertEquals(recordsBefore, JsonUtils.toJson(records)),
            () -> assertEquals(functionsBefore, JsonUtils.toJson(List.of(localFunction, qualifiedFunction))),
            () -> assertSame(arguments, localFunction.getArguments()),
            () -> assertEquals(metadataBefore, JsonUtils.toJson(tables)),
            () -> assertEquals(joinsBefore, JsonUtils.toJson(joins.getQueryJoins())));
      }
      finally
      {
         instance.setTables(originalTables);
         assertEquals(originalMetadata, JsonUtils.toJson(originalTables));
      }
   }

}
