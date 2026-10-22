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


import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** This node's lifecycle in a cluster: a node that is draining reports not-ready
 ** (so the load balancer stops sending it requests), stops claiming new work,
 ** and lets in-flight work finish before it stops (spec section D).
 **
 ** Anything that should react (the registry in qbit-cluster, job claimers) adds
 ** a listener; readiness probes call isDraining().
 *******************************************************************************/
public final class QNodeLifecycle
{
   private static final QLogger LOG = QLogger.getLogger(QNodeLifecycle.class);

   private static volatile boolean draining = false;
   private static volatile String  drainReason;

   private static final List<Consumer<String>> LISTENERS = new CopyOnWriteArrayList<>();



   /*******************************************************************************
    ** not instantiable
    *******************************************************************************/
   private QNodeLifecycle()
   {
   }



   /*******************************************************************************
    ** Whether this node is draining.
    *******************************************************************************/
   public static boolean isDraining()
   {
      return (draining);
   }



   /*******************************************************************************
    ** Why this node is draining (null when it is not).
    *******************************************************************************/
   public static String getDrainReason()
   {
      return (drainReason);
   }



   /*******************************************************************************
    ** Start draining (idempotent). Listeners run once, on the first call.
    *******************************************************************************/
   public static void startDraining(String reason)
   {
      synchronized(QNodeLifecycle.class)
      {
         if(draining)
         {
            return;
         }
         drainReason = reason;
         draining = true;
      }

      LOG.info("Node is draining", logPair("reason", reason), logPair("nodeId", QNodeRoles.nodeId()));
      for(Consumer<String> listener : LISTENERS)
      {
         try
         {
            listener.accept(reason);
         }
         catch(Exception e)
         {
            LOG.warn("Drain listener failed", e);
         }
      }
   }



   /*******************************************************************************
    ** Add a listener called when draining starts (immediately, if it already has).
    *******************************************************************************/
   public static void onDrain(Consumer<String> listener)
   {
      LISTENERS.add(listener);
      if(draining)
      {
         listener.accept(drainReason);
      }
   }



   /*******************************************************************************
    ** Reset (tests only).
    *******************************************************************************/
   public static void resetForTests()
   {
      draining = false;
      drainReason = null;
      LISTENERS.clear();
   }
}
