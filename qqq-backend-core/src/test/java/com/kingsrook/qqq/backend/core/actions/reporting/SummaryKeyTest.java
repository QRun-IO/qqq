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

package com.kingsrook.qqq.backend.core.actions.reporting;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit test for SummaryKey
 *******************************************************************************/
@TestMethodOrder(MethodOrderer.MethodName.class)
class SummaryKeyTest extends BaseTest
{
   private static final QLogger LOG = QLogger.getLogger(SummaryKeyTest.class);



   /*******************************************************************************
    ** Two empty SummaryKeys should be equal and share the same hashCode.
    *******************************************************************************/
   @Test
   void testEquals_emptyKeys_areEqual()
   {
      SummaryKey a = new SummaryKey();
      SummaryKey b = new SummaryKey();

      assertThat(a).isEqualTo(b);
      assertThat(a.hashCode()).isEqualTo(b.hashCode());
   }



   /*******************************************************************************
    ** Keys with identical field/value pairs should be equal.
    *******************************************************************************/
   @Test
   void testEquals_sameFieldValuePairs_areEqual()
   {
      SummaryKey a = new SummaryKey();
      a.add("state", "MO");
      a.add("city", "St. Louis");

      SummaryKey b = new SummaryKey();
      b.add("state", "MO");
      b.add("city", "St. Louis");

      assertThat(a).isEqualTo(b);
      assertThat(a.hashCode()).isEqualTo(b.hashCode());
   }



   /*******************************************************************************
    ** Keys with different values must not be equal.
    *******************************************************************************/
   @Test
   void testEquals_differentValues_areNotEqual()
   {
      SummaryKey a = new SummaryKey();
      a.add("state", "MO");

      SummaryKey b = new SummaryKey();
      b.add("state", "KS");

      assertThat(a).isNotEqualTo(b);
   }



   /*******************************************************************************
    ** Order of entries matters — reversed pair order must not be equal.
    *******************************************************************************/
   @Test
   void testEquals_differentFieldOrder_areNotEqual()
   {
      SummaryKey a = new SummaryKey();
      a.add("state", "MO");
      a.add("city", "Springfield");

      SummaryKey b = new SummaryKey();
      b.add("city", "Springfield");
      b.add("state", "MO");

      assertThat(a).isNotEqualTo(b);
   }



   /*******************************************************************************
    ** Null value should be tolerated in a key entry.
    *******************************************************************************/
   @Test
   void testAdd_nullValue_doesNotThrow()
   {
      SummaryKey key = new SummaryKey();
      key.add("zipCode", null);

      assertThat(key.getKeys()).hasSize(1);
      assertThat(key.getKeys().get(0).getB()).isNull();
   }



   /*******************************************************************************
    ** clone must produce a structurally equal but distinct object.
    *******************************************************************************/
   @Test
   void testClone_producesDeepCopy()
   {
      SummaryKey original = new SummaryKey();
      original.add("state", "MO");
      original.add("city", "Kansas City");

      SummaryKey clone = original.clone();

      assertThat(clone).isEqualTo(original);
      assertThat(clone).isNotSameAs(original);

      ///////////////////////////////////////////////////////////
      // Mutating the clone must not affect the original.      //
      ///////////////////////////////////////////////////////////
      clone.add("zipCode", "64101");
      assertThat(original.getKeys()).hasSize(2);
      assertThat(clone.getKeys()).hasSize(3);
   }



   /*******************************************************************************
    ** toString must not throw and must mention the keys list.
    *******************************************************************************/
   @Test
   void testToString_containsKeys()
   {
      SummaryKey key = new SummaryKey();
      key.add("state", "MO");

      String result = key.toString();
      assertThat(result).isNotNull();
      assertThat(result).contains("MO");
   }



   /*******************************************************************************
    ** A SummaryKey is not equal to null or a non-SummaryKey object.
    *******************************************************************************/
   @Test
   void testEquals_differentType_notEqual()
   {
      SummaryKey key = new SummaryKey();
      key.add("state", "MO");

      assertThat(key).isNotEqualTo(null);
      assertThat(key).isNotEqualTo("MO");
   }



   /*******************************************************************************
    ** getKeys returns the list in insertion order.
    *******************************************************************************/
   @Test
   void testGetKeys_returnsPairsInOrder()
   {
      SummaryKey key = new SummaryKey();
      key.add("a", 1);
      key.add("b", 2);
      key.add("c", 3);

      assertThat(key.getKeys()).hasSize(3);
      assertThat(key.getKeys().get(0).getA()).isEqualTo("a");
      assertThat(key.getKeys().get(1).getA()).isEqualTo("b");
      assertThat(key.getKeys().get(2).getA()).isEqualTo("c");
   }

}
