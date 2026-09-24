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

package com.kingsrook.qqq.backend.core.utils;


import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for ObjectUtils
 *******************************************************************************/
class ObjectUtilsTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRequireNonNullElse()
   {
      Object nullObject = null;
      assertThatThrownBy(() -> ObjectUtils.requireNonNullElse(nullObject)).isInstanceOf(NullPointerException.class);
      assertThatThrownBy(() -> ObjectUtils.requireNonNullElse(null, null)).isInstanceOf(NullPointerException.class);
      assertThatThrownBy(() -> ObjectUtils.requireNonNullElse(null, null, null)).isInstanceOf(NullPointerException.class);
      assertEquals("a", ObjectUtils.requireNonNullElse("a", "b"));
      assertEquals("b", ObjectUtils.requireNonNullElse(null, "b", "c"));
      assertEquals("c", ObjectUtils.requireNonNullElse(null, null, "c"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @SuppressWarnings({ "StringOperationCanBeSimplified", "DataFlowIssue" })
   @Test
   void testTryElse()
   {
      String nullString = null;
      assertEquals("tried", ObjectUtils.tryElse(() -> "tried".toString(), "else"));
      assertEquals("else", ObjectUtils.tryElse(() -> nullString.toString(), "else"));
      assertNull(ObjectUtils.tryElse(() -> null, "else"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @SuppressWarnings({ "StringOperationCanBeSimplified", "DataFlowIssue" })
   @Test
   void testTryAndRequireNonNullElse()
   {
      String nullString = null;
      assertEquals("tried", ObjectUtils.tryAndRequireNonNullElse(() -> "tried".toString(), "else"));
      assertEquals("else", ObjectUtils.tryAndRequireNonNullElse(() -> nullString.toString(), "else"));
      assertEquals("else", ObjectUtils.tryAndRequireNonNullElse(() -> null, "else"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testIfCan()
   {
      Object nullObject = null;
      assertTrue(ObjectUtils.ifCan(() -> true));
      assertTrue(ObjectUtils.ifCan(() -> "a".equals("a")));
      assertFalse(ObjectUtils.ifCan(() -> 1 == 2));
      assertFalse(ObjectUtils.ifCan(() -> nullObject.equals("a")));
      assertFalse(ObjectUtils.ifCan(() -> null));
   }



   /*******************************************************************************
    ** ifNotNull must invoke the consumer only when the object is non-null.
    *******************************************************************************/
   @Test
   void testIfNotNull_nullObject_consumerNotInvoked()
   {
      List<String> invocations = new ArrayList<>();
      ObjectUtils.ifNotNull(null, o -> invocations.add("called"));
      assertThat(invocations).isEmpty();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testIfNotNull_nonNullObject_consumerInvoked()
   {
      List<String> invocations = new ArrayList<>();
      ObjectUtils.ifNotNull("hello", o -> invocations.add(o));
      assertThat(invocations).containsExactly("hello");
   }



   /*******************************************************************************
    ** ifNotNullUnsafe must invoke the unsafe consumer only when the object is
    ** non-null, and must propagate thrown exceptions.
    *******************************************************************************/
   @Test
   void testIfNotNullUnsafe_nullObject_consumerNotInvoked() throws Exception
   {
      List<String> invocations = new ArrayList<>();
      ObjectUtils.ifNotNullUnsafe(null, o -> invocations.add("called"));
      assertThat(invocations).isEmpty();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testIfNotNullUnsafe_nonNullObject_consumerInvoked() throws Exception
   {
      List<String> invocations = new ArrayList<>();
      ObjectUtils.ifNotNullUnsafe("world", o -> invocations.add(o));
      assertThat(invocations).containsExactly("world");
   }



   /*******************************************************************************
    ** ifNotNullUnsafe must propagate exceptions thrown by the consumer.
    *******************************************************************************/
   @Test
   void testIfNotNullUnsafe_consumerThrows_exceptionPropagated()
   {
      assertThatThrownBy(() -> ObjectUtils.ifNotNullUnsafe("value", o ->
      {
         throw new Exception("boom");
      })).hasMessage("boom");
   }



   /*******************************************************************************
    ** requireConditionElse must return a when the predicate is satisfied and
    ** b otherwise.
    *******************************************************************************/
   @Test
   void testRequireConditionElse_conditionTrue_returnsA()
   {
      String result = ObjectUtils.requireConditionElse("hello", s -> s.length() > 3, "default");
      assertThat(result).isEqualTo("hello");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRequireConditionElse_conditionFalse_returnsB()
   {
      String result = ObjectUtils.requireConditionElse("hi", s -> s.length() > 3, "default");
      assertThat(result).isEqualTo("default");
   }



   /*******************************************************************************
    ** requireConditionElse with a null a value and a failing predicate returns b.
    *******************************************************************************/
   @Test
   @SuppressWarnings("DataFlowIssue")
   void testRequireConditionElse_nullA_conditionFalse_returnsB()
   {
      String result = ObjectUtils.requireConditionElse(null, s -> s != null, "fallback");
      assertThat(result).isEqualTo("fallback");
   }

}