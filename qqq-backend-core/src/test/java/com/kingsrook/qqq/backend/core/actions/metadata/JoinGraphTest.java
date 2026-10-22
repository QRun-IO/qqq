/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.metadata;


import java.util.List;
import java.util.Optional;
import java.util.Set;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for JoinGraph
 *******************************************************************************/
class JoinGraphTest extends BaseTest
{

   /*******************************************************************************
    ** Build a simple instance with 3 tables in a chain: order -> orderLine -> item
    *******************************************************************************/
   private QInstance buildSimpleInstance()
   {
      QInstance qInstance = new QInstance();

      qInstance.addJoin(new QJoinMetaData()
         .withName("orderToOrderLine")
         .withLeftTable("order")
         .withRightTable("orderLine")
         .withType(JoinType.ONE_TO_MANY)
         .withJoinOn(new JoinOn("id", "orderId")));

      qInstance.addJoin(new QJoinMetaData()
         .withName("orderLineToItem")
         .withLeftTable("orderLine")
         .withRightTable("item")
         .withType(JoinType.MANY_TO_ONE)
         .withJoinOn(new JoinOn("itemId", "id")));

      return qInstance;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetJoinConnectionsSimpleChain()
   {
      QInstance  qInstance = buildSimpleInstance();
      JoinGraph  joinGraph = new JoinGraph(qInstance);

      ///////////////////////////////////////////////////////
      // from "order" we should reach orderLine (directly) //
      // and item (via orderLine)                          //
      ///////////////////////////////////////////////////////
      Set<JoinGraph.JoinConnectionList> connections = joinGraph.getJoinConnections("order");
      assertEquals(2, connections.size());

      /////////////////////////////////////
      // verify direct join to orderLine //
      /////////////////////////////////////
      Optional<JoinGraph.JoinConnectionList> directToOrderLine = connections.stream()
         .filter(jcl -> jcl.list().size() == 1 && jcl.list().get(0).joinTable().equals("orderLine"))
         .findFirst();
      assertTrue(directToOrderLine.isPresent());
      assertEquals("orderToOrderLine", directToOrderLine.get().list().get(0).viaJoinName());

      ///////////////////////////////
      // verify 2-hop join to item //
      ///////////////////////////////
      Optional<JoinGraph.JoinConnectionList> toItem = connections.stream()
         .filter(jcl -> jcl.list().size() == 2)
         .findFirst();
      assertTrue(toItem.isPresent());
      assertEquals("orderLine", toItem.get().list().get(0).joinTable());
      assertEquals("item", toItem.get().list().get(1).joinTable());
      assertEquals("orderLineToItem", toItem.get().list().get(1).viaJoinName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetJoinConnectionsFromMiddleTable()
   {
      QInstance  qInstance = buildSimpleInstance();
      JoinGraph  joinGraph = new JoinGraph(qInstance);

      //////////////////////////////////////////////////////////////////
      // from "orderLine" we should reach order and item (1 hop each) //
      //////////////////////////////////////////////////////////////////
      Set<JoinGraph.JoinConnectionList> connections = joinGraph.getJoinConnections("orderLine");
      assertEquals(2, connections.size());

      assertTrue(connections.stream().anyMatch(jcl ->
         jcl.list().size() == 1
            && jcl.list().get(0).joinTable().equals("order")
            && jcl.list().get(0).viaJoinName().equals("orderToOrderLine")));

      assertTrue(connections.stream().anyMatch(jcl ->
         jcl.list().size() == 1
            && jcl.list().get(0).joinTable().equals("item")
            && jcl.list().get(0).viaJoinName().equals("orderLineToItem")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetJoinConnectionsNoConnections()
   {
      QInstance qInstance = new QInstance();
      qInstance.addJoin(new QJoinMetaData()
         .withName("aToB")
         .withLeftTable("a")
         .withRightTable("b")
         .withType(JoinType.ONE_TO_ONE)
         .withJoinOn(new JoinOn("bId", "id")));

      JoinGraph joinGraph = new JoinGraph(qInstance);

      ///////////////////////////////////////////////////////////
      // a table not involved in any join should have no paths //
      ///////////////////////////////////////////////////////////
      Set<JoinGraph.JoinConnectionList> connections = joinGraph.getJoinConnections("unrelated");
      assertTrue(connections.isEmpty());
   }



   /*******************************************************************************
    ** Test that duplicate (flipped) joins between the same tables are deduplicated
    ** in the graph edges - i.e., if we have A->B and B->A, the graph still only
    ** has one edge between them.
    *******************************************************************************/
   @Test
   void testDuplicateJoinsAreDeduplicated()
   {
      QInstance qInstance = new QInstance();

      qInstance.addJoin(new QJoinMetaData()
         .withName("orderToOrderLine")
         .withLeftTable("order")
         .withRightTable("orderLine")
         .withType(JoinType.ONE_TO_MANY)
         .withJoinOn(new JoinOn("id", "orderId")));

      qInstance.addJoin(new QJoinMetaData()
         .withName("orderLineToOrder")
         .withLeftTable("orderLine")
         .withRightTable("order")
         .withType(JoinType.MANY_TO_ONE)
         .withJoinOn(new JoinOn("orderId", "id")));

      JoinGraph joinGraph = new JoinGraph(qInstance);

      ////////////////////////////////////////////////////////////////////////////
      // even with two joins defined between the same pair, there's only 1 path //
      ////////////////////////////////////////////////////////////////////////////
      Set<JoinGraph.JoinConnectionList> fromOrder = joinGraph.getJoinConnections("order");
      assertEquals(1, fromOrder.size());
      assertEquals("orderLine", fromOrder.iterator().next().list().get(0).joinTable());
   }



   /*******************************************************************************
    ** Test that joins between the same table pair are NOT deduplicated when their
    ** join-on fields are different.
    *******************************************************************************/
   @Test
   void testJoinsWithDifferentJoinOnsAreNotDeduplicated()
   {
      QInstance qInstance = new QInstance();

      qInstance.addJoin(new QJoinMetaData()
         .withName("orderToOrderLineByOrderId")
         .withLeftTable("order")
         .withRightTable("orderLine")
         .withType(JoinType.ONE_TO_MANY)
         .withJoinOn(new JoinOn("id", "orderId")));

      qInstance.addJoin(new QJoinMetaData()
         .withName("orderToOrderLineByStoreId")
         .withLeftTable("order")
         .withRightTable("orderLine")
         .withType(JoinType.ONE_TO_MANY)
         .withJoinOn(new JoinOn("storeId", "storeId")));

      JoinGraph joinGraph = new JoinGraph(qInstance);

      Set<JoinGraph.JoinConnectionList> fromOrder = joinGraph.getJoinConnections("order");

      List<JoinGraph.JoinConnectionList> oneHopToOrderLine = fromOrder.stream()
         .filter(jcl -> jcl.list().size() == 1 && jcl.list().get(0).joinTable().equals("orderLine"))
         .toList();

      assertEquals(2, oneHopToOrderLine.size());
      assertTrue(oneHopToOrderLine.stream().anyMatch(jcl -> jcl.list().get(0).viaJoinName().equals("orderToOrderLineByOrderId")));
      assertTrue(oneHopToOrderLine.stream().anyMatch(jcl -> jcl.list().get(0).viaJoinName().equals("orderToOrderLineByStoreId")));
   }




   /*******************************************************************************
    * test an odd-ball case (probably not supported, nor intended/correct) where
    * a join breaks the sorting rules (between left & right) in NormalizedJoin.build,
    * because it has the same table name on bot sides, and the same list of join
    * fields on both sides (e.g., employee.id -> employee.id).
    *******************************************************************************/
   @Test
   void testIdenticalJoins()
   {
      QInstance qInstance = new QInstance();

      qInstance.addJoin(new QJoinMetaData()
         .withName("employeeToSelf")
         .withLeftTable("employee")
         .withRightTable("employee")
         .withType(JoinType.MANY_TO_ONE)
         .withJoinOn(new JoinOn("id", "id")));

      new JoinGraph(qInstance);

      ///////////////////////////////////////////////////////////////////////////////////////////////
      // just make sure we didn't crash (or infinitely loop or some-such) on this unsupported case //
      ///////////////////////////////////////////////////////////////////////////////////////////////
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMatchesJoinPathSimple()
   {
      QInstance  qInstance = buildSimpleInstance();
      JoinGraph  joinGraph = new JoinGraph(qInstance);

      Set<JoinGraph.JoinConnectionList> connections = joinGraph.getJoinConnections("order");

      /////////////////////////////////////////////////////////////////
      // find the 1-hop path to orderLine and verify matchesJoinPath //
      /////////////////////////////////////////////////////////////////
      JoinGraph.JoinConnectionList directPath = connections.stream()
         .filter(jcl -> jcl.list().size() == 1)
         .findFirst()
         .orElseThrow();

      assertTrue(directPath.matchesJoinPath(List.of("orderToOrderLine")));
      assertFalse(directPath.matchesJoinPath(List.of("orderLineToItem")));
      assertFalse(directPath.matchesJoinPath(List.of("orderToOrderLine", "orderLineToItem")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMatchesJoinPathMultiHop()
   {
      QInstance  qInstance = buildSimpleInstance();
      JoinGraph  joinGraph = new JoinGraph(qInstance);

      Set<JoinGraph.JoinConnectionList> connections = joinGraph.getJoinConnections("order");

      ////////////////////////////////////////////////////////////
      // find the 2-hop path to item and verify matchesJoinPath //
      ////////////////////////////////////////////////////////////
      JoinGraph.JoinConnectionList twoHopPath = connections.stream()
         .filter(jcl -> jcl.list().size() == 2)
         .findFirst()
         .orElseThrow();

      assertTrue(twoHopPath.matchesJoinPath(List.of("orderToOrderLine", "orderLineToItem")));
      assertFalse(twoHopPath.matchesJoinPath(List.of("orderLineToItem", "orderToOrderLine")));
      assertFalse(twoHopPath.matchesJoinPath(List.of("orderToOrderLine")));
   }



   /*******************************************************************************
    ** This test exercises the flippedJoins-aware matchesJoinPath overload.
    ** Scenario: the instance has two joins between order and orderLine:
    ** - orderToOrderLine (order -> orderLine)
    ** - orderLineToOrder (orderLine -> order)
    ** The graph deduplicates them into one edge (using orderToOrderLine).
    ** When we ask matchesJoinPath for "orderLineToOrder", it should still match
    ** because the flippedJoins map knows they're equivalent.
    ** This exercises line 300 (continue OUTER via flippedJoins match).
    *******************************************************************************/
   @Test
   void testMatchesJoinPathWithFlippedJoins()
   {
      QInstance qInstance = new QInstance();

      qInstance.addJoin(new QJoinMetaData()
         .withName("orderToOrderLine")
         .withLeftTable("order")
         .withRightTable("orderLine")
         .withType(JoinType.ONE_TO_MANY)
         .withJoinOn(new JoinOn("id", "orderId")));

      qInstance.addJoin(new QJoinMetaData()
         .withName("orderLineToOrder")
         .withLeftTable("orderLine")
         .withRightTable("order")
         .withType(JoinType.MANY_TO_ONE)
         .withJoinOn(new JoinOn("orderId", "id")));

      JoinGraph joinGraph = new JoinGraph(qInstance);

      Set<JoinGraph.JoinConnectionList> connections = joinGraph.getJoinConnections("order");
      assertEquals(1, connections.size());

      JoinGraph.JoinConnectionList connectionList = connections.iterator().next();

      /////////////////////////////////////////////////////////////////
      // the edge stored uses "orderToOrderLine" as the join name    //
      // so a simple matchesJoinPath with the exact name should work //
      /////////////////////////////////////////////////////////////////
      assertTrue(connectionList.matchesJoinPath(List.of("orderToOrderLine")));

      //////////////////////////////////////////////////////////////////////
      // a simple matchesJoinPath (no flipped joins) should NOT match the //
      // alternate name, because it only does exact name comparison       //
      //////////////////////////////////////////////////////////////////////
      assertFalse(connectionList.matchesJoinPath(List.of("orderLineToOrder")));

      /////////////////////////////////////////////////////////////////////////
      // but the flippedJoins-aware overload SHOULD match "orderLineToOrder" //
      // because it knows both join names refer to the same table pair.      //
      // This is the code path that hits line 300 (continue OUTER).          //
      /////////////////////////////////////////////////////////////////////////
      assertTrue(connectionList.matchesJoinPath(List.of("orderLineToOrder"), joinGraph, qInstance));

      ////////////////////////////////////////////////////////////////////
      // and the exact name should also still work via the 3-arg method //
      ////////////////////////////////////////////////////////////////////
      assertTrue(connectionList.matchesJoinPath(List.of("orderToOrderLine"), joinGraph, qInstance));

      //////////////////////////////////////////////////////////////////
      // a completely unrelated join name should not match either way //
      //////////////////////////////////////////////////////////////////
      assertFalse(connectionList.matchesJoinPath(List.of("bogusJoin"), joinGraph, qInstance));
   }



   /*******************************************************************************
    ** For a self-join, verify flipped definitions normalize to the same key so
    ** flippedJoins-aware matching treats both names as equivalent.
    *******************************************************************************/
   @Test
   void testMatchesJoinPathWithFlippedSelfJoin()
   {
      QInstance qInstance = new QInstance();

      qInstance.addJoin(new QJoinMetaData()
         .withName("employeeToManager")
         .withLeftTable("employee")
         .withRightTable("employee")
         .withType(JoinType.MANY_TO_ONE)
         .withJoinOn(new JoinOn("managerId", "id")));

      qInstance.addJoin(new QJoinMetaData()
         .withName("managerToEmployee")
         .withLeftTable("employee")
         .withRightTable("employee")
         .withType(JoinType.ONE_TO_MANY)
         .withJoinOn(new JoinOn("id", "managerId")));

      JoinGraph joinGraph = new JoinGraph(qInstance);

      JoinGraph.JoinConnectionList connectionList = new JoinGraph.JoinConnectionList(List.of(
         new JoinGraph.JoinConnection("employee", "employeeToManager")));

      ///////////////////////////////////////////////////////////////////////////////////////////////
      // the overload of matchesJoinPath that only takes string won't see the 2nd join as matching //
      ///////////////////////////////////////////////////////////////////////////////////////////////
      assertTrue(connectionList.matchesJoinPath(List.of("employeeToManager")));
      assertFalse(connectionList.matchesJoinPath(List.of("managerToEmployee")));
      ///////////////////////////////////////////////////////////////////////////////////////////////////////
      // but the overload that takes the graph - it can see flipped joins, so it will see both as matching //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////
      assertTrue(connectionList.matchesJoinPath(List.of("employeeToManager"), joinGraph, qInstance));
      assertTrue(connectionList.matchesJoinPath(List.of("managerToEmployee"), joinGraph, qInstance));
   }



   /*******************************************************************************
    ** Test the flipped-joins-aware matchesJoinPath for a multi-hop path where
    ** one of the hops uses the alternate (flipped) join name.
    *******************************************************************************/
   @Test
   void testMatchesJoinPathWithFlippedJoinsMultiHop()
   {
      QInstance qInstance = new QInstance();

      qInstance.addJoin(new QJoinMetaData()
         .withName("orderToOrderLine")
         .withLeftTable("order")
         .withRightTable("orderLine")
         .withType(JoinType.ONE_TO_MANY)
         .withJoinOn(new JoinOn("id", "orderId")));

      qInstance.addJoin(new QJoinMetaData()
         .withName("orderLineToOrder")
         .withLeftTable("orderLine")
         .withRightTable("order")
         .withType(JoinType.MANY_TO_ONE)
         .withJoinOn(new JoinOn("orderId", "id")));

      qInstance.addJoin(new QJoinMetaData()
         .withName("orderLineToItem")
         .withLeftTable("orderLine")
         .withRightTable("item")
         .withType(JoinType.MANY_TO_ONE)
         .withJoinOn(new JoinOn("itemId", "id")));

      JoinGraph joinGraph = new JoinGraph(qInstance);

      //////////////////////////////////////////////////////////////////
      // from "order", get the 2-hop path: order -> orderLine -> item //
      //////////////////////////////////////////////////////////////////
      Set<JoinGraph.JoinConnectionList> connections = joinGraph.getJoinConnections("order");
      JoinGraph.JoinConnectionList twoHopPath = connections.stream()
         .filter(jcl -> jcl.list().size() == 2 && jcl.list().get(1).joinTable().equals("item"))
         .findFirst()
         .orElseThrow();

      ////////////////////////////////////////////////////////////////////////////////////
      // the stored path uses "orderToOrderLine" for the first hop.                     //
      // asking with "orderLineToOrder" as the first hop should match via flippedJoins. //
      ////////////////////////////////////////////////////////////////////////////////////
      assertTrue(twoHopPath.matchesJoinPath(List.of("orderLineToOrder", "orderLineToItem"), joinGraph, qInstance));
      assertTrue(twoHopPath.matchesJoinPath(List.of("orderToOrderLine", "orderLineToItem"), joinGraph, qInstance));

      ////////////////////////////
      // wrong size should fail //
      ////////////////////////////
      assertFalse(twoHopPath.matchesJoinPath(List.of("orderLineToOrder"), joinGraph, qInstance));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testJoinConnectionListCompareTo()
   {
      JoinGraph.JoinConnectionList list1 = new JoinGraph.JoinConnectionList(List.of(
         new JoinGraph.JoinConnection("alpha", "joinA")));

      JoinGraph.JoinConnectionList list2 = new JoinGraph.JoinConnectionList(List.of(
         new JoinGraph.JoinConnection("beta", "joinB")));

      JoinGraph.JoinConnectionList list1Copy = new JoinGraph.JoinConnectionList(List.of(
         new JoinGraph.JoinConnection("alpha", "joinA")));

      assertEquals(0, list1.compareTo(list1Copy));
      assertTrue(list1.compareTo(list2) < 0);
      assertTrue(list2.compareTo(list1) > 0);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testJoinConnectionListCompareToBySize()
   {
      JoinGraph.JoinConnectionList shorter = new JoinGraph.JoinConnectionList(List.of(
         new JoinGraph.JoinConnection("alpha", "joinA")));

      JoinGraph.JoinConnectionList longer = new JoinGraph.JoinConnectionList(List.of(
         new JoinGraph.JoinConnection("alpha", "joinA"),
         new JoinGraph.JoinConnection("beta", "joinB")));

      assertTrue(shorter.compareTo(longer) < 0);
      assertTrue(longer.compareTo(shorter) > 0);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testJoinConnectionListGetJoinNames()
   {
      JoinGraph.JoinConnectionList connectionList = new JoinGraph.JoinConnectionList(List.of(
         new JoinGraph.JoinConnection("orderLine", "orderToOrderLine"),
         new JoinGraph.JoinConnection("item", "orderLineToItem")));

      assertEquals("orderToOrderLine, orderLineToItem", connectionList.getJoinNamesAsString());
      assertEquals(List.of("orderToOrderLine", "orderLineToItem"), connectionList.getJoinNamesAsList());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testJoinConnectionListCopy()
   {
      JoinGraph.JoinConnectionList original = new JoinGraph.JoinConnectionList(new java.util.ArrayList<>(List.of(
         new JoinGraph.JoinConnection("orderLine", "orderToOrderLine"))));

      JoinGraph.JoinConnectionList copy = original.copy();
      assertEquals(original, copy);

      ///////////////////////////////////////////////////
      // modifying the copy should not affect original //
      ///////////////////////////////////////////////////
      copy.list().add(new JoinGraph.JoinConnection("item", "orderLineToItem"));
      assertEquals(1, original.list().size());
      assertEquals(2, copy.list().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testEmptyInstance()
   {
      QInstance qInstance = new QInstance();
      JoinGraph joinGraph = new JoinGraph(qInstance);

      Set<JoinGraph.JoinConnectionList> connections = joinGraph.getJoinConnections("anyTable");
      assertTrue(connections.isEmpty());
   }

}
