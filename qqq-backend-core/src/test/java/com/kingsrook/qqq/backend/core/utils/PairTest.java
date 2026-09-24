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


import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit tests for Pair — construction, factory method, equals/hashCode contract,
 ** clone, and toString format.
 *******************************************************************************/
class PairTest
{

   /*******************************************************************************
    ** Constructor populates both slots; getA and getB return the expected values.
    *******************************************************************************/
   @Test
   void testConstructor_getAandGetB_returnValues()
   {
      Pair<String, Integer> pair = new Pair<>("hello", 42);

      assertEquals("hello", pair.getA());
      assertEquals(42, pair.getB());
   }



   /*******************************************************************************
    ** of() factory creates a pair equal to the direct constructor.
    *******************************************************************************/
   @Test
   void testOf_factoryProducesSamePairAsConstructor()
   {
      Pair<String, Integer> direct  = new Pair<>("a", 1);
      Pair<String, Integer> factory = Pair.of("a", 1);

      assertEquals(direct, factory);
   }



   /*******************************************************************************
    ** Two pairs with equal elements must be equal (equals contract).
    *******************************************************************************/
   @Test
   void testEquals_samePairValues_areEqual()
   {
      Pair<String, Integer> p1 = Pair.of("x", 10);
      Pair<String, Integer> p2 = Pair.of("x", 10);

      assertEquals(p1, p2);
   }



   /*******************************************************************************
    ** Pairs with different elements must not be equal.
    *******************************************************************************/
   @Test
   void testEquals_differentValues_notEqual()
   {
      assertNotEquals(Pair.of("a", 1), Pair.of("b", 1));
      assertNotEquals(Pair.of("a", 1), Pair.of("a", 2));
   }



   /*******************************************************************************
    ** A pair is not equal to null.
    *******************************************************************************/
   @Test
   void testEquals_null_notEqual()
   {
      Pair<String, Integer> p = Pair.of("a", 1);

      assertNotEquals(null, p);
   }



   /*******************************************************************************
    ** A pair is not equal to an object of a different type.
    *******************************************************************************/
   @Test
   void testEquals_differentType_notEqual()
   {
      Pair<String, Integer> p = Pair.of("a", 1);

      assertNotEquals("a:1", p);
   }



   /*******************************************************************************
    ** Equal pairs must have the same hashCode (hashCode contract).
    *******************************************************************************/
   @Test
   void testHashCode_equalPairs_sameHashCode()
   {
      Pair<String, Integer> p1 = Pair.of("x", 10);
      Pair<String, Integer> p2 = Pair.of("x", 10);

      assertEquals(p1.hashCode(), p2.hashCode());
   }



   /*******************************************************************************
    ** Pairs with null elements are supported — equals and hashCode must not throw.
    *******************************************************************************/
   @Test
   void testNullElements_equalsAndHashCode_doNotThrow()
   {
      Pair<String, Integer> withNullA  = Pair.of(null, 1);
      Pair<String, Integer> withNullB  = Pair.of("a", null);
      Pair<String, Integer> bothNull   = Pair.of(null, null);
      Pair<String, Integer> bothNull2  = Pair.of(null, null);

      assertEquals(bothNull, bothNull2);
      assertEquals(bothNull.hashCode(), bothNull2.hashCode());
      assertNull(withNullA.getA());
      assertNull(withNullB.getB());
   }



   /*******************************************************************************
    ** toString should produce "{a}:{b}" format.
    *******************************************************************************/
   @Test
   void testToString_format()
   {
      Pair<String, Integer> p = Pair.of("hello", 42);

      assertEquals("hello:42", p.toString());
   }



   /*******************************************************************************
    ** clone() should return a new object with equal field values.
    *******************************************************************************/
   @Test
   void testClone_returnsNewInstance_withEqualValues()
   {
      Pair<String, Integer> original = Pair.of("orig", 99);
      Pair<String, Integer> cloned   = original.clone();

      assertNotSame(original, cloned);
      assertEquals(original, cloned);
      assertEquals(original.getA(), cloned.getA());
      assertEquals(original.getB(), cloned.getB());
   }



   /*******************************************************************************
    ** Pair is reflexively equal to itself.
    *******************************************************************************/
   @Test
   void testEquals_reflexive_sameInstanceIsEqual()
   {
      Pair<String, Integer> p = Pair.of("a", 1);

      assertThat(p).isEqualTo(p);
   }

}
