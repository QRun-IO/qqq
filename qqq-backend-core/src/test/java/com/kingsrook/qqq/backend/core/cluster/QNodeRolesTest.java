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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for QNodeRoles and QNodeLifecycle
 *******************************************************************************/
class QNodeRolesTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      QNodeRoles.set(null);
      QNodeLifecycle.resetForTests();
   }



   /*******************************************************************************
    ** An interpreter reading only the given environment.
    *******************************************************************************/
   private static QMetaDataVariableInterpreter env(Map<String, String> values)
   {
      return (new QMetaDataVariableInterpreter()
      {
         {
            setEnvironmentOverrides(new HashMap<>(values));
         }
      });
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDefaultIsEverything()
   {
      QNodeRoles.Settings settings = QNodeRoles.resolve(env(Map.of("HOSTNAME", "pod-a")));
      assertEquals(EnumSet.allOf(QNodeRoles.Role.class), settings.roles());
      assertTrue(settings.http());
      assertTrue(settings.scheduler());
      assertTrue(settings.esbTriggers());
      assertNull(settings.workerThreads(), "the instance's own worker thread setting applies");
      assertEquals("pod-a", settings.nodeId());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWebNode()
   {
      QNodeRoles.Settings settings = QNodeRoles.resolve(env(Map.of("QQQ_NODE_ROLES", "web", "QQQ_NODE_ID", "web-1", "HOSTNAME", "ignored")));
      assertEquals(EnumSet.of(QNodeRoles.Role.WEB), settings.roles());
      assertTrue(settings.http());
      assertFalse(settings.scheduler());
      assertFalse(settings.esbTriggers());
      assertEquals(0, settings.workerThreads(), "a node without the worker role runs no job workers");
      assertEquals("web-1", settings.nodeId());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWorkerNodeWithOverrides()
   {
      QNodeRoles.Settings settings = QNodeRoles.resolve(env(Map.of("QQQ_NODE_ROLES", " Worker , scheduler,esb,bogus", "QQQ_JOBS_WORKER_THREADS", "4",
         "QQQ_ESB_TRIGGERS_ENABLED", "false", "HOSTNAME", "worker-0")));
      assertEquals(EnumSet.of(QNodeRoles.Role.WORKER, QNodeRoles.Role.SCHEDULER, QNodeRoles.Role.ESB), settings.roles());
      assertFalse(settings.http());
      assertTrue(settings.scheduler());
      assertFalse(settings.esbTriggers(), "the explicit flag wins over the role");
      assertEquals(4, settings.workerThreads());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBadThreadCountAndAll()
   {
      QNodeRoles.Settings settings = QNodeRoles.resolve(env(Map.of("QQQ_NODE_ROLES", "web,all", "QQQ_JOBS_WORKER_THREADS", "lots")));
      assertEquals(EnumSet.allOf(QNodeRoles.Role.class), settings.roles());
      assertNull(settings.workerThreads());
      assertThat(settings.nodeId()).isNotBlank();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSetOverridesForTheProcess()
   {
      QNodeRoles.set(new QNodeRoles.Settings(EnumSet.of(QNodeRoles.Role.WORKER), false, false, false, 1, "test-node"));
      assertEquals("test-node", QNodeRoles.nodeId());
      assertTrue(QNodeRoles.has(QNodeRoles.Role.WORKER));
      assertFalse(QNodeRoles.has(QNodeRoles.Role.WEB));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDrainingRunsListenersOnce()
   {
      List<String> heard = new CopyOnWriteArrayList<>();
      QNodeLifecycle.onDrain(heard::add);
      assertFalse(QNodeLifecycle.isDraining());

      QNodeLifecycle.startDraining("shutdown");
      QNodeLifecycle.startDraining("again");
      assertTrue(QNodeLifecycle.isDraining());
      assertEquals("shutdown", QNodeLifecycle.getDrainReason());
      assertEquals(List.of("shutdown"), heard);

      /////////////////////////////////////////////////////////
      // a listener added after draining started hears at once //
      /////////////////////////////////////////////////////////
      QNodeLifecycle.onDrain(heard::add);
      assertEquals(List.of("shutdown", "shutdown"), heard);
   }
}
