/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.cluster;


import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.utils.ClassPathUtils;
import com.kingsrook.qqq.backend.core.utils.memoization.Memoization;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for ClusterBus and ClusterCaches: a shared transport between two
 ** "nodes" (the node id is switched to stand for the other node).
 *******************************************************************************/
class ClusterBusTest extends BaseTest
{
   /** the "wire": events every node's transport sees */
   static final List<Consumer<ClusterEvent>> WIRE = new CopyOnWriteArrayList<>();



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      ClusterBus.close(QContext.getQInstance());
      WIRE.clear();
      QNodeRoles.set(null);
   }



   /*******************************************************************************
    ** A transport over the static WIRE (so a "remote" publish reaches us).
    *******************************************************************************/
   public static class WireBus implements ClusterBusInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void publish(ClusterEvent event)
      {
         WIRE.forEach(listener -> listener.accept(event));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public AutoCloseable subscribe(Consumer<ClusterEvent> listener)
      {
         WIRE.add(listener);
         return (() -> WIRE.remove(listener));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void asNode(String nodeId)
   {
      QNodeRoles.set(new QNodeRoles.Settings(EnumSet.allOf(QNodeRoles.Role.class), true, true, true, null, nodeId));
   }



   /*******************************************************************************
    ** A remote invalidation clears this node's cache; our own echo is ignored.
    *******************************************************************************/
   @Test
   void testRemoteInvalidationClearsLocalCacheAndOwnEchoIsIgnored()
   {
      QInstance instance = QContext.getQInstance();
      instance.withSupplementalMetaData(new ClusterBusMetaData().withBus(new QCodeReference(WireBus.class)));
      asNode("node-a");
      ClusterBus.connect(instance);

      Memoization<Integer, String> memo = ClusterCaches.shared("test.byId", new Memoization<>(), Integer::valueOf);
      memo.storeResult(1, "one");
      memo.storeResult(2, "two");

      List<String> handled = new CopyOnWriteArrayList<>();
      ClusterBus.on("test-type", (i, event) -> handled.add(event.getOriginNodeId()));

      ///////////////////////////////////////////////////////////////
      // our own publish: handled once (locally), echo is dropped  //
      ///////////////////////////////////////////////////////////////
      ClusterBus.publish(instance, new ClusterEvent("test-type", null, null));
      assertEquals(List.of("node-a"), handled);

      ///////////////////////////////////////////////////////////////
      // another node invalidates key 1                            //
      ///////////////////////////////////////////////////////////////
      ClusterEvent remote = new ClusterEvent(ClusterEvent.TYPE_MEMO_INVALIDATE, "test.byId", "1");
      remote.setOriginNodeId("node-b");
      WIRE.forEach(listener -> listener.accept(remote));
      assertTrue(memo.getMemoizedResult(1).isEmpty());
      assertTrue(memo.getMemoizedResult(2).isPresent());

      ///////////////////////////////////////
      // and then everything               //
      ///////////////////////////////////////
      ClusterCaches.invalidate("test.byId");
      assertTrue(memo.getMemoizedResult(2).isEmpty());
   }



   /*******************************************************************************
    ** A session ending on another node clears session-keyed caches here.
    *******************************************************************************/
   @Test
   void testSessionEndedClearsSessionKeyedCaches()
   {
      QInstance instance = QContext.getQInstance();
      instance.withSupplementalMetaData(new ClusterBusMetaData().withBus(new QCodeReference(WireBus.class)));
      asNode("node-a");
      ClusterBus.connect(instance);

      Memoization<String, String> tokens = ClusterCaches.sessionKeyed("test.tokens", new Memoization<>());
      tokens.storeResult("session-1", "token-1");
      tokens.storeResult("session-2", "token-2");

      ClusterEvent ended = new ClusterEvent(ClusterEvent.TYPE_SESSION_ENDED, null, "session-1");
      ended.setOriginNodeId("node-b");
      WIRE.forEach(listener -> listener.accept(ended));

      assertTrue(tokens.getMemoizedResult("session-1").isEmpty());
      assertTrue(tokens.getMemoizedResult("session-2").isPresent());
   }



   /*******************************************************************************
    ** Every static Memoization in QQQ core is registered (shared or node-local):
    ** a new static cache must say how it behaves on several nodes.
    *******************************************************************************/
   @Test
   void testEveryStaticCacheIsRegistered() throws Exception
   {
      assertTrue(ClassPathUtils.getClassesInPackage("com.kingsrook.qqq.backend.core").size() > 500, "the audit scans the whole core");
      assertEquals(List.of(), ClusterCacheAudit.unregisteredStaticCaches("com.kingsrook.qqq.backend.core"));
   }
}
