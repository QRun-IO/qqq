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

package com.kingsrook.qqq.backend.core.model.metadata.permissions;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit tests for QPermissionRules — factory defaults, fluent setters, and
 ** the clone() contract.
 *******************************************************************************/
class QPermissionRulesTest
{

   /*******************************************************************************
    ** defaultInstance() returns NOT_PROTECTED + HIDDEN — the "open" out-of-box
    ** permission rules applied when none are explicitly configured.
    *******************************************************************************/
   @Test
   void testDefaultInstance_hasExpectedDefaults()
   {
      QPermissionRules rules = QPermissionRules.defaultInstance();

      assertNotNull(rules);
      assertEquals(PermissionLevel.NOT_PROTECTED, rules.getLevel());
      assertEquals(DenyBehavior.HIDDEN, rules.getDenyBehavior());
   }



   /*******************************************************************************
    ** Fluent setters (withX) return the same instance for chaining.
    *******************************************************************************/
   @Test
   void testFluentSetters_returnSameInstance()
   {
      QPermissionRules rules = new QPermissionRules();

      QPermissionRules returned = rules
         .withLevel(PermissionLevel.HAS_ACCESS_PERMISSION)
         .withDenyBehavior(DenyBehavior.DISABLED)
         .withPermissionBaseName("myBase");

      assertEquals(rules, returned, "Fluent setters must return the same instance");
   }



   /*******************************************************************************
    ** Fluent setters actually persist the values they receive.
    *******************************************************************************/
   @Test
   void testFluentSetters_valuesAreRetained()
   {
      QPermissionRules rules = new QPermissionRules()
         .withLevel(PermissionLevel.READ_WRITE_PERMISSIONS)
         .withDenyBehavior(DenyBehavior.DISABLED)
         .withPermissionBaseName("orderPermBase");

      assertEquals(PermissionLevel.READ_WRITE_PERMISSIONS, rules.getLevel());
      assertEquals(DenyBehavior.DISABLED, rules.getDenyBehavior());
      assertEquals("orderPermBase", rules.getPermissionBaseName());
   }



   /*******************************************************************************
    ** clone() produces a distinct object whose fields equal the original's.
    *******************************************************************************/
   @Test
   void testClone_producesIndependentCopy()
   {
      QPermissionRules original = new QPermissionRules()
         .withLevel(PermissionLevel.HAS_ACCESS_PERMISSION)
         .withDenyBehavior(DenyBehavior.HIDDEN)
         .withPermissionBaseName("base");

      QPermissionRules clone = original.clone();

      assertNotSame(original, clone);
      assertEquals(original.getLevel(), clone.getLevel());
      assertEquals(original.getDenyBehavior(), clone.getDenyBehavior());
      assertEquals(original.getPermissionBaseName(), clone.getPermissionBaseName());
   }



   /*******************************************************************************
    ** Mutating the clone after copy does not affect the original.
    *******************************************************************************/
   @Test
   void testClone_mutatingCloneDoesNotAffectOriginal()
   {
      QPermissionRules original = new QPermissionRules()
         .withLevel(PermissionLevel.NOT_PROTECTED);

      QPermissionRules clone = original.clone();
      clone.setLevel(PermissionLevel.READ_WRITE_PERMISSIONS);

      assertEquals(PermissionLevel.NOT_PROTECTED, original.getLevel(), "Original should not be affected by clone mutation");
   }



   /*******************************************************************************
    ** A fresh instance has all null fields — no surprise defaults.
    *******************************************************************************/
   @Test
   void testDefaultConstructor_allFieldsNull()
   {
      QPermissionRules rules = new QPermissionRules();

      assertNull(rules.getLevel());
      assertNull(rules.getDenyBehavior());
      assertNull(rules.getPermissionBaseName());
      assertNull(rules.getCustomPermissionChecker());
   }

}
