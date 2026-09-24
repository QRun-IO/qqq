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

package com.kingsrook.qqq.backend.core.utils;


import java.lang.reflect.Method;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit tests for ReflectiveBeanLikeClassUtils — field-name derivation,
 ** getter recognition, setter lookup, and default allowed-types list.
 *******************************************************************************/
class ReflectiveBeanLikeClassUtilsTest
{

   /*******************************************************************************
    ** getFieldNameFromGetter strips the "get" prefix and lowercases the first letter.
    *******************************************************************************/
   @Test
   void testGetFieldNameFromGetter_standardGetter_stripsGetPrefix() throws Exception
   {
      Method getter = QRecord.class.getMethod("getTableName");
      String fieldName = ReflectiveBeanLikeClassUtils.getFieldNameFromGetter(getter);

      assertEquals("tableName", fieldName);
   }



   /*******************************************************************************
    ** Single-character getter (e.g., getX) should produce "x" (lowercase).
    *******************************************************************************/
   @Test
   void testGetFieldNameFromGetter_singleCharAfterGet_returnsLowercase() throws Exception
   {
      /////////////////////////////////////////////////////////////////////////
      // QQueryFilter has no single-char getter, so we use a local stub via  //
      // anonymous class to exercise the length-1 branch                     //
      /////////////////////////////////////////////////////////////////////////
      /*******************************************************************************
       ** Minimal bean stub with a single-character getter for testing.
       *******************************************************************************/
      class StubBean
      {

         /*******************************************************************************
          ** Single-char getter — exercises the length-1 branch in getFieldNameFromGetter.
          *******************************************************************************/
         @SuppressWarnings("unused")
         public String getX()
         {
            return "x";
         }
      }

      Method getter = StubBean.class.getMethod("getX");
      String fieldName = ReflectiveBeanLikeClassUtils.getFieldNameFromGetter(getter);

      assertEquals("x", fieldName);
   }



   /*******************************************************************************
    ** isGetter returns true for a standard String getter on an allowed type.
    *******************************************************************************/
   @Test
   void testIsGetter_stringReturnType_returnsTrue() throws Exception
   {
      Method getter = QRecord.class.getMethod("getTableName");

      assertTrue(ReflectiveBeanLikeClassUtils.isGetter(getter, false));
   }



   /*******************************************************************************
    ** isGetter returns false for a method that has parameters (not a getter).
    *******************************************************************************/
   @Test
   void testIsGetter_methodWithParameters_returnsFalse() throws Exception
   {
      Method withParam = QRecord.class.getMethod("getValue", String.class);

      assertFalse(ReflectiveBeanLikeClassUtils.isGetter(withParam, false));
   }



   /*******************************************************************************
    ** isGetter returns false for getClass() — special-cased to avoid noise.
    *******************************************************************************/
   @Test
   void testIsGetter_getClass_returnsFalse() throws Exception
   {
      Method getClass = Object.class.getMethod("getClass");

      assertFalse(ReflectiveBeanLikeClassUtils.isGetter(getClass, false));
   }



   /*******************************************************************************
    ** isGetter returns false for a method that does not start with "get".
    *******************************************************************************/
   @Test
   void testIsGetter_nonGetPrefix_returnsFalse() throws Exception
   {
      Method hashCode = Object.class.getMethod("hashCode");

      assertFalse(ReflectiveBeanLikeClassUtils.isGetter(hashCode, false));
   }



   /*******************************************************************************
    ** isGetter returns false for a getter whose return type is not in the allowed list.
    *******************************************************************************/
   @Test
   void testIsGetter_unsupportedReturnType_returnsFalse() throws Exception
   {
      ///////////////////////////////////////////////////////////////////////
      // QRecord.getValues() returns a Map — not in the default allow list //
      ///////////////////////////////////////////////////////////////////////
      Method getValues = QRecord.class.getMethod("getValues");

      assertFalse(ReflectiveBeanLikeClassUtils.isGetter(getValues, false));
   }



   /*******************************************************************************
    ** getSetterForGetter finds the matching setter by name and parameter type.
    *******************************************************************************/
   @Test
   void testGetSetterForGetter_matchingSetterExists_returnsIt() throws Exception
   {
      Method getter = QRecord.class.getMethod("getTableName");
      Optional<Method> setter = ReflectiveBeanLikeClassUtils.getSetterForGetter(QRecord.class, getter);

      assertTrue(setter.isPresent());
      assertEquals("setTableName", setter.get().getName());
   }



   /*******************************************************************************
    ** getSetterForGetter returns empty when no setter exists for the getter.
    *******************************************************************************/
   @Test
   void testGetSetterForGetter_noSetter_returnsEmpty() throws Exception
   {
      //////////////////////////////////////////////////////////////////////////
      // Object.getClass() has no corresponding setClass — returns empty      //
      //////////////////////////////////////////////////////////////////////////
      Method getClass = Object.class.getMethod("getClass");
      Optional<Method> setter = ReflectiveBeanLikeClassUtils.getSetterForGetter(Object.class, getClass);

      assertFalse(setter.isPresent());
   }



   /*******************************************************************************
    ** defaultAllowedTypes includes the core primitive-wrapper types used by QQQ fields.
    *******************************************************************************/
   @Test
   void testDefaultAllowedTypes_containsExpectedTypes()
   {
      var allowedTypes = ReflectiveBeanLikeClassUtils.defaultAllowedTypes();

      assertThat(allowedTypes).contains(
         String.class,
         Integer.class,
         Long.class,
         Boolean.class,
         java.math.BigDecimal.class,
         java.time.Instant.class,
         java.time.LocalDate.class
      );
   }

}
