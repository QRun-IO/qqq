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

package com.kingsrook.qqq.middleware.javalin.cluster;


import java.time.Duration;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobRegistry;
import com.kingsrook.qqq.backend.core.cluster.QNodeLifecycle;
import com.kingsrook.qqq.backend.core.cluster.QNodeRoles;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import io.javalin.config.JavalinConfig;
import io.javalin.http.ServiceUnavailableResponse;
import org.eclipse.jetty.server.handler.GracefulHandler;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Javalin side of running as one node of a cluster (spec
 ** 2026-10-15-cluster-ha-design, sections A and D):
 ** - a node without the web role answers 503 on every route except probes and
 **   metrics, so a Service that routes users to it is obvious at once;
 ** - Jetty stops gracefully: in-flight requests finish (up to
 **   QQQ_HTTP_DRAIN_SECONDS, default 20) before connections close;
 ** - on SIGTERM the node drains: it reports not-ready (QNodeLifecycle), waits
 **   QQQ_DRAIN_DELAY_SECONDS (default 0 - a k8s preStop sleep usually covers
 **   endpoint propagation), lets this node's async process steps finish (up to
 **   QQQ_ASYNC_DRAIN_SECONDS, default 20), then stops the server.
 *******************************************************************************/
public final class JavalinClusterSupport
{
   private static final QLogger LOG = QLogger.getLogger(JavalinClusterSupport.class);

   private static final List<String> PROBE_PATH_PREFIXES = List.of("/health", "/ready", "/metrics", "/livez", "/readyz");



   /*******************************************************************************
    ** not instantiable
    *******************************************************************************/
   private JavalinClusterSupport()
   {
   }



   /*******************************************************************************
    ** Apply to a Javalin config: graceful Jetty stop and the web-role filter.
    *******************************************************************************/
   public static void configure(JavalinConfig config)
   {
      long httpDrainMillis = seconds("qqq.http.drainSeconds", "QQQ_HTTP_DRAIN_SECONDS", 20) * 1000L;
      config.jetty.modifyServer(server ->
      {
         server.insertHandler(new GracefulHandler());
         server.setStopTimeout(httpDrainMillis);
      });

      if(!QNodeRoles.get().http())
      {
         LOG.info("This node does not serve application requests (no web role); only probes and metrics are served", logPair("nodeId", QNodeRoles.nodeId()));
         config.routes.before(context ->
         {
            if(!isProbePath(context.path()))
            {
               throw (new ServiceUnavailableResponse("This node does not serve requests (no web role)."));
            }
         });
      }
   }



   /*******************************************************************************
    ** Whether a path is a probe or metrics path (always served).
    *******************************************************************************/
   static boolean isProbePath(String path)
   {
      if(path == null)
      {
         return (false);
      }
      for(String prefix : PROBE_PATH_PREFIXES)
      {
         if(path.equals(prefix) || path.startsWith(prefix + "/"))
         {
            return (true);
         }
      }
      return (false);
   }



   /*******************************************************************************
    ** The SIGTERM drain, run before the given stop action. Returns the hook
    ** thread (already registered).
    *******************************************************************************/
   public static Thread installShutdownHook(Runnable stopServer)
   {
      Thread hook = new Thread(() -> drainThenStop(stopServer), "qqq-node-drain");
      Runtime.getRuntime().addShutdownHook(hook);
      return (hook);
   }



   /*******************************************************************************
    ** Drain this node, then stop the server.
    *******************************************************************************/
   static void drainThenStop(Runnable stopServer)
   {
      long started = System.currentTimeMillis();
      QNodeLifecycle.startDraining("shutdown");

      int delaySeconds = seconds("qqq.drain.delaySeconds", "QQQ_DRAIN_DELAY_SECONDS", 0);
      if(delaySeconds > 0)
      {
         try
         {
            Thread.sleep(delaySeconds * 1000L);
         }
         catch(InterruptedException e)
         {
            Thread.currentThread().interrupt();
         }
      }

      int     asyncSeconds = seconds("qqq.async.drainSeconds", "QQQ_ASYNC_DRAIN_SECONDS", 20);
      boolean idle         = AsyncJobRegistry.awaitIdle(Duration.ofSeconds(asyncSeconds));

      try
      {
         stopServer.run();
      }
      catch(Exception e)
      {
         LOG.warn("Error stopping the server during the drain", e);
      }
      LOG.info("Node drained", logPair("nodeId", QNodeRoles.nodeId()), logPair("asyncJobsFinished", idle), logPair("millis", System.currentTimeMillis() - started));
   }



   /*******************************************************************************
    ** A non-negative number of seconds from a property or environment variable.
    *******************************************************************************/
   static int seconds(String property, String env, int defaultValue)
   {
      String value = new QMetaDataVariableInterpreter().getStringFromPropertyOrEnvironment(property, env, null);
      if(value == null || value.isBlank())
      {
         return (defaultValue);
      }
      try
      {
         return (Math.max(0, Integer.parseInt(value.trim())));
      }
      catch(NumberFormatException e)
      {
         LOG.warn("Ignoring a non-numeric drain setting", logPair("name", env), logPair("value", value));
         return (defaultValue);
      }
   }
}
