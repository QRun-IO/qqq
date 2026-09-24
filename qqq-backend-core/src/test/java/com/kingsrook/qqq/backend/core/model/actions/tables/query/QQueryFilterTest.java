/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.actions.tables.query;


import java.util.List;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.FilterVariableExpression;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit tests for QQueryFilter — construction, boolean logic, cloning, and
 ** variable-name derivation in prepForBackend().
 *******************************************************************************/
class QQueryFilterTest
{

   /*******************************************************************************
    ** Empty filter has no criteria.
    *******************************************************************************/
   @Test
   void testHasAnyCriteria_emptyFilter_returnsFalse()
   {
      QQueryFilter filter = new QQueryFilter();

      assertFalse(filter.hasAnyCriteria());
   }



   /*******************************************************************************
    ** Filter with one top-level criterion reports true.
    *******************************************************************************/
   @Test
   void testHasAnyCriteria_withCriteria_returnsTrue()
   {
      QQueryFilter filter = new QQueryFilter()
         .withCriteria("status", QCriteriaOperator.EQUALS, "active");

      assertTrue(filter.hasAnyCriteria());
   }



   /*******************************************************************************
    ** Filter with no top-level criteria but a non-empty sub-filter reports true.
    *******************************************************************************/
   @Test
   void testHasAnyCriteria_criteriaInSubFilterOnly_returnsTrue()
   {
      QQueryFilter sub = new QQueryFilter()
         .withCriteria("tenantId", QCriteriaOperator.EQUALS, 1);
      QQueryFilter filter = new QQueryFilter()
         .withSubFilter(sub);

      assertTrue(filter.hasAnyCriteria());
   }



   /*******************************************************************************
    ** Sub-filter that is itself empty (no criteria) should not trigger true.
    *******************************************************************************/
   @Test
   void testHasAnyCriteria_emptySubFilter_returnsFalse()
   {
      QQueryFilter filter = new QQueryFilter()
         .withSubFilter(new QQueryFilter());

      assertFalse(filter.hasAnyCriteria());
   }



   /*******************************************************************************
    ** Null entries inside the subFilters list are tolerated.
    *******************************************************************************/
   @Test
   void testHasAnyCriteria_nullSubFilter_doesNotThrow()
   {
      QQueryFilter filter = new QQueryFilter();
      filter.setSubFilters(new java.util.ArrayList<>(java.util.Arrays.asList((QQueryFilter) null)));

      assertFalse(filter.hasAnyCriteria());
   }



   /*******************************************************************************
    ** Default BooleanOperator is AND.
    *******************************************************************************/
   @Test
   void testDefaultBooleanOperator_isAnd()
   {
      QQueryFilter filter = new QQueryFilter();

      assertEquals(QQueryFilter.BooleanOperator.AND, filter.getBooleanOperator());
   }



   /*******************************************************************************
    ** Clone produces a separate object with equal field values.
    *******************************************************************************/
   @Test
   void testClone_producesNewInstance()
   {
      QQueryFilter original = new QQueryFilter()
         .withCriteria("status", QCriteriaOperator.EQUALS, "active")
         .withOrderBy(new QFilterOrderBy("createdDate"));

      QQueryFilter cloned = original.clone();

      assertNotSame(original, cloned);
      assertThat(cloned.getCriteria()).hasSize(1);
      assertThat(cloned.getOrderBys()).hasSize(1);
   }



   /*******************************************************************************
    ** Mutating cloned criteria list must not affect the original.
    *******************************************************************************/
   @Test
   void testClone_deepCopiesCriteria()
   {
      QQueryFilter original = new QQueryFilter()
         .withCriteria("status", QCriteriaOperator.EQUALS, "active");

      QQueryFilter cloned = original.clone();
      cloned.getCriteria().clear();

      assertThat(original.getCriteria()).hasSize(1);
   }



   /*******************************************************************************
    ** Mutating cloned subFilters list must not affect the original.
    *******************************************************************************/
   @Test
   void testClone_deepCopiesSubFilters()
   {
      QQueryFilter sub      = new QQueryFilter().withCriteria("x", QCriteriaOperator.EQUALS, 1);
      QQueryFilter original = new QQueryFilter().withSubFilter(sub);

      QQueryFilter cloned = original.clone();
      cloned.getSubFilters().clear();

      assertThat(original.getSubFilters()).hasSize(1);
   }



   /*******************************************************************************
    ** Cloning a filter with null criteria/orderBys/subFilters should not throw.
    *******************************************************************************/
   @Test
   void testClone_nullCollections_doesNotThrow()
   {
      QQueryFilter filter = new QQueryFilter();
      filter.setCriteria(null);
      filter.setOrderBys(null);
      filter.setSubFilters(null);

      QQueryFilter cloned = filter.clone();

      assertNotNull(cloned);
   }



   /*******************************************************************************
    ** varargs withCriteria overload appends a criterion with explicit values.
    *******************************************************************************/
   @Test
   void testWithCriteria_varargs_appendsCriteria()
   {
      QQueryFilter filter = new QQueryFilter()
         .withCriteria("amount", QCriteriaOperator.GREATER_THAN, 100)
         .withCriteria("amount", QCriteriaOperator.LESS_THAN, 500);

      assertThat(filter.getCriteria()).hasSize(2);
      assertEquals("amount", filter.getCriteria().get(0).getFieldName());
   }



   /*******************************************************************************
    ** Collection overload of withCriteria builds correctly.
    *******************************************************************************/
   @Test
   void testWithCriteria_collection_appendsCriteria()
   {
      QQueryFilter filter = new QQueryFilter()
         .withCriteria("id", QCriteriaOperator.IN, List.of(1, 2, 3));

      assertThat(filter.getCriteria()).hasSize(1);
      assertEquals(QCriteriaOperator.IN, filter.getCriteria().get(0).getOperator());
   }



   /*******************************************************************************
    ** toString of an empty filter returns "()" with no content.
    *******************************************************************************/
   @Test
   void testToString_emptyFilter()
   {
      String s = new QQueryFilter().toString();

      assertThat(s).isEqualTo("()");
   }



   /*******************************************************************************
    ** toString includes the criterion field name.
    *******************************************************************************/
   @Test
   void testToString_withCriteria_containsFieldName()
   {
      QQueryFilter filter = new QQueryFilter()
         .withCriteria("status", QCriteriaOperator.EQUALS, "active");

      assertThat(filter.toString()).contains("status");
   }



   /*******************************************************************************
    ** toString lists multiple criteria joined by the BooleanOperator keyword.
    *******************************************************************************/
   @Test
   void testToString_multipleCriteria_containsOperator()
   {
      QQueryFilter filter = new QQueryFilter()
         .withBooleanOperator(QQueryFilter.BooleanOperator.OR)
         .withCriteria("a", QCriteriaOperator.EQUALS, 1)
         .withCriteria("b", QCriteriaOperator.EQUALS, 2);

      assertThat(filter.toString()).contains("OR");
   }



   /*******************************************************************************
    ** toString includes sub-filter block.
    *******************************************************************************/
   @Test
   void testToString_withSubFilter_containsSubBlock()
   {
      QQueryFilter sub    = new QQueryFilter().withCriteria("x", QCriteriaOperator.EQUALS, 1);
      QQueryFilter filter = new QQueryFilter().withSubFilter(sub);

      assertThat(filter.toString()).contains("Sub:{");
   }



   /*******************************************************************************
    ** toString includes OrderBy section when order-bys present.
    *******************************************************************************/
   @Test
   void testToString_withOrderBy_containsOrderBySection()
   {
      QQueryFilter filter = new QQueryFilter()
         .withOrderBy(new QFilterOrderBy("createdDate"));

      assertThat(filter.toString()).contains("OrderBy[");
   }



   /*******************************************************************************
    ** prepForBackend assigns a camelCase variableName derived from fieldName + operator.
    ** Simple case: single criterion, single value → "fieldEquals" pattern.
    *******************************************************************************/
   @Test
   void testPrepForBackend_singleCriteria_assignsVariableName()
   {
      FilterVariableExpression fve = new FilterVariableExpression();
      QFilterCriteria criterion = new QFilterCriteria("status", QCriteriaOperator.EQUALS, List.of(fve));
      QQueryFilter filter = new QQueryFilter(criterion);

      filter.prepForBackend();

      assertNotNull(fve.getVariableName());
      assertThat(fve.getVariableName()).startsWith("status");
   }



   /*******************************************************************************
    ** prepForBackend appends a numeric suffix when the same field+operator appears
    ** more than once (deduplication guard).
    *******************************************************************************/
   @Test
   void testPrepForBackend_duplicateFieldOperator_appendsIndex()
   {
      FilterVariableExpression fve1 = new FilterVariableExpression();
      FilterVariableExpression fve2 = new FilterVariableExpression();

      QFilterCriteria c1 = new QFilterCriteria("status", QCriteriaOperator.EQUALS, List.of(fve1));
      QFilterCriteria c2 = new QFilterCriteria("status", QCriteriaOperator.EQUALS, List.of(fve2));

      QQueryFilter filter = new QQueryFilter(c1, c2);
      filter.prepForBackend();

      assertNotNull(fve1.getVariableName());
      assertNotNull(fve2.getVariableName());
      assertThat(fve2.getVariableName()).endsWith("2");
   }



   /*******************************************************************************
    ** BETWEEN criteria appends "From" to the first value variable name.
    *******************************************************************************/
   @Test
   void testPrepForBackend_betweenOperator_appendsFromAndTo()
   {
      FilterVariableExpression fveFrom = new FilterVariableExpression();
      FilterVariableExpression fveTo   = new FilterVariableExpression();

      QFilterCriteria criterion = new QFilterCriteria("amount", QCriteriaOperator.BETWEEN, List.of(fveFrom, fveTo));
      QQueryFilter    filter    = new QQueryFilter(criterion);
      filter.prepForBackend();

      assertThat(fveFrom.getVariableName()).endsWith("From");
      assertThat(fveTo.getVariableName()).endsWith("To");
   }



   /*******************************************************************************
    ** prepForBackend skips non-FilterVariableExpression values without throwing.
    *******************************************************************************/
   @Test
   void testPrepForBackend_plainValues_noException()
   {
      QFilterCriteria criterion = new QFilterCriteria("status", QCriteriaOperator.EQUALS, "active");
      QQueryFilter    filter    = new QQueryFilter(criterion);

      filter.prepForBackend();
   }



   /*******************************************************************************
    ** SubFilterSetOperator enum has the expected members.
    *******************************************************************************/
   @Test
   void testSubFilterSetOperator_enumValues()
   {
      assertNotNull(QQueryFilter.SubFilterSetOperator.UNION);
      assertNotNull(QQueryFilter.SubFilterSetOperator.UNION_ALL);
      assertNotNull(QQueryFilter.SubFilterSetOperator.INTERSECT);
      assertNotNull(QQueryFilter.SubFilterSetOperator.EXCEPT);
   }



   /*******************************************************************************
    ** skip and limit are null by default, and can be set via fluent setters.
    *******************************************************************************/
   @Test
   void testSkipAndLimit_defaultNullAndFluentSetter()
   {
      QQueryFilter filter = new QQueryFilter();

      assertNull(filter.getSkip());
      assertNull(filter.getLimit());

      filter.withSkip(10).withLimit(25);

      assertEquals(10, filter.getSkip());
      assertEquals(25, filter.getLimit());
   }



   /*******************************************************************************
    ** prepForBackend currently processes only top-level criteria — FVEs inside
    ** sub-filters are NOT assigned variable names.  This test documents the current
    ** (non-recursive) behaviour so any future change that adds recursion will be caught.
    *******************************************************************************/
   @Test
   void testPrepForBackend_fveInSubFilter_isNotAssigned()
   {
      FilterVariableExpression fveInSub = new FilterVariableExpression();
      QFilterCriteria          subCrit  = new QFilterCriteria("tenantId", QCriteriaOperator.EQUALS, List.of(fveInSub));
      QQueryFilter             sub      = new QQueryFilter(subCrit);

      QQueryFilter outer = new QQueryFilter().withSubFilter(sub);
      outer.prepForBackend();

      ///////////////////////////////////////////////////////////////////////////////////////
      // The FVE in the sub-filter must remain un-named — prepForBackend does not recurse. //
      // If this assertion fails after a code change, verify the recursion is intentional. //
      ///////////////////////////////////////////////////////////////////////////////////////
      assertNull(fveInSub.getVariableName());
   }

}
