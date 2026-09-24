/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit tests for QueryJoin — constructor variants, Type helper, clone depth,
 ** and single-criteria lazy-init.
 *******************************************************************************/
class QueryJoinTest
{

   /*******************************************************************************
    ** No-arg constructor defaults: type INNER, select false, no join table.
    *******************************************************************************/
   @Test
   void testNoArgConstructor_defaults()
   {
      QueryJoin join = new QueryJoin();

      assertNull(join.getJoinTable());
      assertNull(join.getBaseTableOrAlias());
      assertNull(join.getAlias());
      assertFalse(join.getSelect());
      assertEquals(QueryJoin.Type.INNER, join.getType());
   }



   /*******************************************************************************
    ** Single-string constructor sets joinTable.
    *******************************************************************************/
   @Test
   void testSingleStringConstructor_setsJoinTable()
   {
      QueryJoin join = new QueryJoin("orderLine");

      assertEquals("orderLine", join.getJoinTable());
      assertNull(join.getBaseTableOrAlias());
   }



   /*******************************************************************************
    ** Two-string constructor sets baseTableOrAlias and joinTable.
    *******************************************************************************/
   @Test
   void testTwoStringConstructor_setsBothTables()
   {
      QueryJoin join = new QueryJoin("order", "orderLine");

      assertEquals("order", join.getBaseTableOrAlias());
      assertEquals("orderLine", join.getJoinTable());
   }



   /*******************************************************************************
    ** Type.isOuter — LEFT and RIGHT are outer; INNER and FULL are not.
    *******************************************************************************/
   @Test
   void testTypeIsOuter_leftAndRight_areOuter()
   {
      assertTrue(QueryJoin.Type.isOuter(QueryJoin.Type.LEFT));
      assertTrue(QueryJoin.Type.isOuter(QueryJoin.Type.RIGHT));
      assertFalse(QueryJoin.Type.isOuter(QueryJoin.Type.INNER));
      assertFalse(QueryJoin.Type.isOuter(QueryJoin.Type.FULL));
   }



   /*******************************************************************************
    ** Fluent chain sets all fields and returns the same instance.
    *******************************************************************************/
   @Test
   void testFluentChain_allFieldsAndSameInstance()
   {
      QueryJoin join = new QueryJoin()
         .withJoinTable("customer")
         .withBaseTableOrAlias("order")
         .withAlias("c")
         .withSelect(true)
         .withType(QueryJoin.Type.LEFT);

      assertEquals("customer", join.getJoinTable());
      assertEquals("order", join.getBaseTableOrAlias());
      assertEquals("c", join.getAlias());
      assertTrue(join.getSelect());
      assertEquals(QueryJoin.Type.LEFT, join.getType());
   }



   /*******************************************************************************
    ** clone() should return a new instance that is not the same reference.
    *******************************************************************************/
   @Test
   void testClone_returnsNewInstance()
   {
      QueryJoin original = new QueryJoin("orderLine")
         .withType(QueryJoin.Type.LEFT)
         .withAlias("ol");

      QueryJoin cloned = original.clone();

      assertNotSame(original, cloned);
      assertEquals(original.getJoinTable(), cloned.getJoinTable());
      assertEquals(original.getType(), cloned.getType());
      assertEquals(original.getAlias(), cloned.getAlias());
   }



   /*******************************************************************************
    ** clone() must deep-copy securityCriteria — mutations to clone must not
    ** affect original.
    *******************************************************************************/
   @Test
   void testClone_deepCopiesSecurityCriteria()
   {
      QFilterCriteria criterion = new QFilterCriteria("tenantId", QCriteriaOperator.EQUALS, List.of(1));
      QueryJoin       original  = new QueryJoin("orderLine").withSecurityCriteria(criterion);
      QueryJoin       cloned    = original.clone();

      assertNotSame(original.getSecurityCriteria(), cloned.getSecurityCriteria());
      assertThat(cloned.getSecurityCriteria()).hasSize(1);

      cloned.getSecurityCriteria().clear();
      assertThat(original.getSecurityCriteria()).hasSize(1);
   }



   /*******************************************************************************
    ** withSecurityCriteria(single) lazily initialises the list and appends.
    *******************************************************************************/
   @Test
   void testWithSecurityCriteria_single_lazyInit()
   {
      QueryJoin       join = new QueryJoin("orderLine");
      QFilterCriteria c1   = new QFilterCriteria("tenantId", QCriteriaOperator.EQUALS, List.of(1));
      QFilterCriteria c2   = new QFilterCriteria("status", QCriteriaOperator.NOT_EQUALS, List.of("deleted"));

      join.withSecurityCriteria(c1);
      join.withSecurityCriteria(c2);

      assertThat(join.getSecurityCriteria()).hasSize(2).containsExactly(c1, c2);
   }



   /*******************************************************************************
    ** toString should contain joinTable, type, and alias.
    *******************************************************************************/
   @Test
   void testToString_containsKeyFields()
   {
      QueryJoin join = new QueryJoin("customer")
         .withAlias("c")
         .withType(QueryJoin.Type.LEFT);

      String str = join.toString();

      assertThat(str).contains("customer");
      assertThat(str).contains("LEFT");
      assertThat(str).contains("c");
   }



   /*******************************************************************************
    ** securityCriteria list starts as non-null empty list.
    *******************************************************************************/
   @Test
   void testGetSecurityCriteria_default_nonNullEmpty()
   {
      QueryJoin join = new QueryJoin();

      assertNotNull(join.getSecurityCriteria());
      assertThat(join.getSecurityCriteria()).isEmpty();
   }



   /*******************************************************************************
    ** getJoinTableOrItsAlias returns alias when set, otherwise joinTable.
    *******************************************************************************/
   @Test
   void testGetJoinTableOrItsAlias_aliasPresent_returnsAlias()
   {
      QueryJoin join = new QueryJoin("customer").withAlias("c");
      assertEquals("c", join.getJoinTableOrItsAlias());
   }



   /*******************************************************************************
    ** getJoinTableOrItsAlias falls back to joinTable when alias is absent.
    *******************************************************************************/
   @Test
   void testGetJoinTableOrItsAlias_noAlias_returnsJoinTable()
   {
      QueryJoin join = new QueryJoin("customer");
      assertEquals("customer", join.getJoinTableOrItsAlias());
   }

}
