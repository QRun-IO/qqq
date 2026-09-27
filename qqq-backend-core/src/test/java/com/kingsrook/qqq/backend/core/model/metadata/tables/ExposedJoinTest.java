/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.tables;


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for ExposedJoin
 *******************************************************************************/
class ExposedJoinTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testIsManyOneToOne()
   {
      QInstance qInstance = QContext.getQInstance();
      qInstance.addTable(new QTableMetaData().withName("A").withExposedJoin(new ExposedJoin().withJoinTable("B").withJoinPath(List.of("AB"))));
      qInstance.addTable(new QTableMetaData().withName("B").withExposedJoin(new ExposedJoin().withJoinTable("A").withJoinPath(List.of("AB"))));
      qInstance.addJoin(new QJoinMetaData().withName("AB").withLeftTable("A").withRightTable("B").withType(JoinType.ONE_TO_ONE));

      assertFalse(qInstance.getTable("A").getExposedJoins().get(0).getIsMany());
      assertFalse(qInstance.getTable("B").getExposedJoins().get(0).getIsMany());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testIsManyOneToMany()
   {
      QInstance qInstance = QContext.getQInstance();
      qInstance.addTable(new QTableMetaData().withName("A").withExposedJoin(new ExposedJoin().withJoinTable("B").withJoinPath(List.of("AB"))));
      qInstance.addTable(new QTableMetaData().withName("B").withExposedJoin(new ExposedJoin().withJoinTable("A").withJoinPath(List.of("AB"))));
      qInstance.addJoin(new QJoinMetaData().withName("AB").withLeftTable("A").withRightTable("B").withType(JoinType.ONE_TO_MANY));

      assertTrue(qInstance.getTable("A").getExposedJoins().get(0).getIsMany());
      assertFalse(qInstance.getTable("B").getExposedJoins().get(0).getIsMany());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testIsManyManyToOne()
   {
      QInstance qInstance = QContext.getQInstance();
      qInstance.addTable(new QTableMetaData().withName("A").withExposedJoin(new ExposedJoin().withJoinTable("B").withJoinPath(List.of("AB"))));
      qInstance.addTable(new QTableMetaData().withName("B").withExposedJoin(new ExposedJoin().withJoinTable("A").withJoinPath(List.of("AB"))));
      qInstance.addJoin(new QJoinMetaData().withName("AB").withLeftTable("A").withRightTable("B").withType(JoinType.MANY_TO_ONE));

      assertFalse(qInstance.getTable("A").getExposedJoins().get(0).getIsMany());
      assertTrue(qInstance.getTable("B").getExposedJoins().get(0).getIsMany());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testIsManyOneToOneThroughChain()
   {
      QInstance qInstance = QContext.getQInstance();
      qInstance.addTable(new QTableMetaData().withName("A").withExposedJoin(new ExposedJoin().withJoinTable("G").withJoinPath(List.of("AB", "BC", "CD", "DE", "EF", "FG"))));
      qInstance.addTable(new QTableMetaData().withName("B").withExposedJoin(new ExposedJoin().withJoinTable("G").withJoinPath(List.of("BC", "CD", "DE", "EF", "FG"))));
      qInstance.addJoin(new QJoinMetaData().withName("AB").withLeftTable("A").withRightTable("B").withType(JoinType.ONE_TO_ONE));
      qInstance.addJoin(new QJoinMetaData().withName("BC").withLeftTable("B").withRightTable("C").withType(JoinType.ONE_TO_ONE));
      qInstance.addJoin(new QJoinMetaData().withName("CD").withLeftTable("C").withRightTable("D").withType(JoinType.ONE_TO_ONE));
      qInstance.addJoin(new QJoinMetaData().withName("DE").withLeftTable("D").withRightTable("E").withType(JoinType.ONE_TO_ONE));
      qInstance.addJoin(new QJoinMetaData().withName("EF").withLeftTable("E").withRightTable("F").withType(JoinType.ONE_TO_ONE));
      qInstance.addJoin(new QJoinMetaData().withName("FG").withLeftTable("F").withRightTable("G").withType(JoinType.ONE_TO_ONE));

      assertFalse(qInstance.getTable("A").getExposedJoins().get(0).getIsMany());
      assertFalse(qInstance.getTable("B").getExposedJoins().get(0).getIsMany());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testIsManyOneToManyThroughChain()
   {
      QInstance qInstance = QContext.getQInstance();
      qInstance.addTable(new QTableMetaData().withName("A").withExposedJoin(new ExposedJoin().withJoinTable("G").withJoinPath(List.of("AB", "BC", "CD", "DE", "EF", "FG"))));
      qInstance.addTable(new QTableMetaData().withName("B").withExposedJoin(new ExposedJoin().withJoinTable("E").withJoinPath(List.of("BC", "CD", "DE"))));
      qInstance.addTable(new QTableMetaData().withName("F").withExposedJoin(new ExposedJoin().withJoinTable("C").withJoinPath(List.of("FG", "EF", "DE", "CD"))));
      qInstance.addJoin(new QJoinMetaData().withName("AB").withLeftTable("A").withRightTable("B").withType(JoinType.ONE_TO_ONE));
      qInstance.addJoin(new QJoinMetaData().withName("BC").withLeftTable("B").withRightTable("C").withType(JoinType.ONE_TO_ONE));
      qInstance.addJoin(new QJoinMetaData().withName("CD").withLeftTable("C").withRightTable("D").withType(JoinType.ONE_TO_ONE));
      qInstance.addJoin(new QJoinMetaData().withName("DE").withLeftTable("D").withRightTable("E").withType(JoinType.ONE_TO_ONE));
      qInstance.addJoin(new QJoinMetaData().withName("EF").withLeftTable("E").withRightTable("F").withType(JoinType.ONE_TO_MANY));
      qInstance.addJoin(new QJoinMetaData().withName("FG").withLeftTable("F").withRightTable("G").withType(JoinType.ONE_TO_ONE));

      assertTrue(qInstance.getTable("A").getExposedJoins().get(0).getIsMany());
      assertFalse(qInstance.getTable("B").getExposedJoins().get(0).getIsMany());
      assertFalse(qInstance.getTable("F").getExposedJoins().get(0).getIsMany());
   }

}