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

package com.kingsrook.qqq.backend.core.model.metadata.joins;


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit test for QJoinMetaData
 *******************************************************************************/
@TestMethodOrder(MethodOrderer.MethodName.class)
class QJoinMetaDataTest extends BaseTest
{
   private static final QLogger LOG = QLogger.getLogger(QJoinMetaDataTest.class);



   /*******************************************************************************
    ** flip() on ONE_TO_MANY swaps tables and inverts the type to MANY_TO_ONE.
    *******************************************************************************/
   @Test
   void testFlip_oneToMany_producesCorrectInversion()
   {
      QJoinMetaData join = new QJoinMetaData()
         .withName("orderToOrderLine")
         .withLeftTable("order")
         .withRightTable("orderLine")
         .withType(JoinType.ONE_TO_MANY)
         .withJoinOns(List.of(new JoinOn("id", "orderId")));

      QJoinMetaData flipped = join.flip();

      assertThat(flipped.getLeftTable()).isEqualTo("orderLine");
      assertThat(flipped.getRightTable()).isEqualTo("order");
      assertThat(flipped.getType()).isEqualTo(JoinType.MANY_TO_ONE);
      assertThat(flipped.getJoinOns()).hasSize(1);
      assertThat(flipped.getJoinOns().get(0).getLeftField()).isEqualTo("orderId");
      assertThat(flipped.getJoinOns().get(0).getRightField()).isEqualTo("id");
   }



   /*******************************************************************************
    ** flip() on MANY_TO_ONE inverts to ONE_TO_MANY.
    *******************************************************************************/
   @Test
   void testFlip_manyToOne_producesCorrectInversion()
   {
      QJoinMetaData join = new QJoinMetaData()
         .withLeftTable("orderLine")
         .withRightTable("order")
         .withType(JoinType.MANY_TO_ONE)
         .withJoinOns(List.of(new JoinOn("orderId", "id")));

      QJoinMetaData flipped = join.flip();

      assertThat(flipped.getType()).isEqualTo(JoinType.ONE_TO_MANY);
      assertThat(flipped.getLeftTable()).isEqualTo("order");
      assertThat(flipped.getRightTable()).isEqualTo("orderLine");
   }



   /*******************************************************************************
    ** flip() on ONE_TO_ONE leaves the type unchanged.
    *******************************************************************************/
   @Test
   void testFlip_oneToOne_typeUnchanged()
   {
      QJoinMetaData join = new QJoinMetaData()
         .withLeftTable("person")
         .withRightTable("personDetail")
         .withType(JoinType.ONE_TO_ONE)
         .withJoinOns(List.of(new JoinOn("id", "personId")));

      QJoinMetaData flipped = join.flip();

      assertThat(flipped.getType()).isEqualTo(JoinType.ONE_TO_ONE);
   }



   /*******************************************************************************
    ** flip() on MANY_TO_MANY leaves the type unchanged.
    *******************************************************************************/
   @Test
   void testFlip_manyToMany_typeUnchanged()
   {
      QJoinMetaData join = new QJoinMetaData()
         .withLeftTable("tag")
         .withRightTable("article")
         .withType(JoinType.MANY_TO_MANY)
         .withJoinOns(List.of(new JoinOn("id", "tagId")));

      QJoinMetaData flipped = join.flip();

      assertThat(flipped.getType()).isEqualTo(JoinType.MANY_TO_MANY);
      assertThat(flipped.getLeftTable()).isEqualTo("article");
      assertThat(flipped.getRightTable()).isEqualTo("tag");
   }



   /*******************************************************************************
    ** clone() produces an independent copy; mutating the clone does not affect the
    ** original's joinOns list.
    *******************************************************************************/
   @Test
   void testClone_producesDeepCopyOfJoinOns()
   {
      QJoinMetaData original = new QJoinMetaData()
         .withName("myJoin")
         .withLeftTable("left")
         .withRightTable("right")
         .withType(JoinType.ONE_TO_MANY)
         .withJoinOns(List.of(new JoinOn("leftId", "rightLeftId")));

      QJoinMetaData clone = original.clone();

      assertThat(clone).isNotSameAs(original);
      assertThat(clone.getName()).isEqualTo(original.getName());
      assertThat(clone.getJoinOns()).hasSize(1);
      assertThat(clone.getJoinOns()).isNotSameAs(original.getJoinOns());
   }



   /*******************************************************************************
    ** JoinType.flip() round-trips correctly.
    *******************************************************************************/
   @Test
   void testJoinTypeFlip_roundTrip()
   {
      assertThat(JoinType.ONE_TO_MANY.flip()).isEqualTo(JoinType.MANY_TO_ONE);
      assertThat(JoinType.MANY_TO_ONE.flip()).isEqualTo(JoinType.ONE_TO_MANY);
      assertThat(JoinType.ONE_TO_ONE.flip()).isEqualTo(JoinType.ONE_TO_ONE);
      assertThat(JoinType.MANY_TO_MANY.flip()).isEqualTo(JoinType.MANY_TO_MANY);
   }



   /*******************************************************************************
    ** Fluent builder setters should populate all fields correctly.
    *******************************************************************************/
   @Test
   void testFluentBuilderSetters_populateAllFields()
   {
      JoinOn joinOn = new JoinOn("orderId", "id");

      QJoinMetaData join = new QJoinMetaData()
         .withName("orderToCustomer")
         .withLeftTable("order")
         .withRightTable("customer")
         .withType(JoinType.MANY_TO_ONE)
         .withJoinOns(List.of(joinOn));

      assertThat(join.getName()).isEqualTo("orderToCustomer");
      assertThat(join.getLeftTable()).isEqualTo("order");
      assertThat(join.getRightTable()).isEqualTo("customer");
      assertThat(join.getType()).isEqualTo(JoinType.MANY_TO_ONE);
      assertThat(join.getJoinOns()).containsExactly(joinOn);
   }

}
