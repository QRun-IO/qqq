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

package com.kingsrook.sampleapp;


import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.actions.async.AsyncJobRegistry;
import com.kingsrook.sampleapp.processes.cluster.SampleClusterProcesses;
import org.h2.tools.Server;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.parallel.ResourceLock;
import static com.kingsrook.sampleapp.SampleSingleNodeDefaultsTest.awaitCondition;
import static com.kingsrook.sampleapp.SampleSingleNodeDefaultsTest.secretValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;


/*******************************************************************************
 ** Multi-node (HA) mode: separate packaged sample JVMs share one H2 database
 ** (an H2 TCP server in this JVM) and the Artemis broker embedded in node B,
 ** with SAMPLE_CLUSTER_MODE (qqq_state + StateProviderMetaData with a test key,
 ** EsbClusterBus) and distinct QQQ_NODE_ID values.
 **
 ** - node A: QQQ_NODE_ROLES=web.
 ** - node B: QQQ_NODE_ROLES=all (it serves requests too, so the user's next
 **   request can land on it, as on a second web pod); it hosts the broker.
 ** - node C: QQQ_NODE_ROLES=worker,scheduler,esb - the worker pod shape - started
 **   at the end, against the same database, to show a node without the web role.
 **
 ** Every request carries the same session cookie, so it is one user whose
 ** requests a load balancer spreads across nodes without stickiness.
 ** The single-node counterpart is SampleSingleNodeDefaultsTest; see
 ** docs/DEPLOYMENT-MODES.md.
 *******************************************************************************/
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ResourceLock("qqq.sample.esb.port")
class SampleMultiNodeIT
{
   private static final int HEARTBEAT_SECONDS  = 1;
   private static final int LOST_AFTER_SECONDS = 4;

   private Path   directory;
   private Server database;
   private String jdbcUrl;
   private String encryptionKey;
   private int    brokerPort;

   private PackagedSampleServer nodeA;
   private PackagedSampleServer nodeB;
   private PackagedSampleServer nodeC;

   private SampleProcessHttp onA;
   private SampleProcessHttp onB;



   /*******************************************************************************
    ** One shared database, then node B (primes it, hosts the broker), then A.
    *******************************************************************************/
   @BeforeAll
   void startCluster() throws Exception
   {
      directory = Files.createTempDirectory("qqq-multinode-");
      database = Server.createTcpServer("-tcpPort", Integer.toString(freePort()), "-ifNotExists").start();
      jdbcUrl = "jdbc:h2:tcp://127.0.0.1:" + database.getPort() + "/mem:qqq_cluster_" + UUID.randomUUID().toString().replace("-", "") + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
      byte[] key = new byte[32];
      new SecureRandom().nextBytes(key);
      encryptionKey = Base64.getEncoder().encodeToString(key);
      brokerPort = freePort();

      nodeB = startNode("node-b", "all", true, true, brokerPort, Map.of());
      URI baseB = nodeB.awaitReady();
      nodeA = startNode("node-a", "web", false, false, brokerPort, Map.of());
      URI baseA = nodeA.awaitReady();

      onA = new SampleProcessHttp(baseA, UUID.randomUUID().toString());
      onB = onA.on(baseB);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterAll
   void stopCluster() throws Exception
   {
      for(PackagedSampleServer node : new PackagedSampleServer[] { nodeC, nodeA, nodeB })
      {
         if(node != null)
         {
            node.close();
         }
      }
      if(database != null)
      {
         database.stop();
      }
   }



   /*******************************************************************************
    ** Both nodes are up, ready, and share state in qqq_state.
    *******************************************************************************/
   @Test
   @Order(1)
   void nodesAreReady() throws Exception
   {
      assertEquals(200, onA.get("/ready").statusCode());
      assertEquals(200, onB.get("/ready").statusCode());
      assertEquals(200, onA.get("/qqq/v1/metaData").statusCode());
   }



   /*******************************************************************************
    ** A wizard started on A continues on B, with its state from qqq_state.
    *******************************************************************************/
   @Test
   @Order(2)
   void wizardStartedOnAContinuesOnB() throws Exception
   {
      JSONObject started = onA.init(SampleClusterProcesses.PROCESS_WIZARD, null, 10_000);
      assertEquals("input", started.getString("nextStep"));
      String processUUID = started.getString("processUUID");
      String token       = started.getJSONObject("values").getString("token");
      assertEquals("node-a", started.getJSONObject("values").getString("startedOn"));
      assertEquals("node-a", storedState("PROCESS_STATUS:" + processUUID).get("node_id"), "node A stored the wizard's state");

      JSONObject remembered = onB.step(SampleClusterProcesses.PROCESS_WIZARD, processUUID, "input", "{\"note\":\"two nodes\"}");
      assertEquals("node-b", remembered.getJSONObject("values").getString("rememberedOn"));
      assertEquals("node-a|" + token + "|two nodes", remembered.getJSONObject("values").getString("summary"));

      Map<String, String> row = storedState("PROCESS_STATUS:" + processUUID);
      assertEquals("node-b", row.get("node_id"), "node B read and rewrote the same row");
      assertFalse(row.get("payload").contains(token), "the stored state is encrypted");
   }



   /*******************************************************************************
    ** An async step running on A is polled to completion on B.
    *******************************************************************************/
   @Test
   @Order(3)
   void asyncStatusPollsOnTheOtherNode() throws Exception
   {
      JSONObject started = onA.init(SampleClusterProcesses.PROCESS_WORK, "{\"seconds\":3}", 1);
      assertEquals("JOB_STARTED", started.getString("type"), started.toString());
      String processUUID = started.getString("processUUID");
      String jobUUID     = started.getString("jobUUID");

      JSONObject running = awaitRunning(onB, processUUID, jobUUID);
      assertEquals("Working on node-a", running.getString("message"));

      JSONObject done = onB.awaitDone(SampleClusterProcesses.PROCESS_WORK, processUUID, jobUUID, Duration.ofSeconds(20));
      assertEquals("COMPLETE", done.getString("type"), done.toString());
      assertEquals("node-a", done.getJSONObject("values").getString("ranOn"));
      assertFalse(done.getJSONObject("values").getBoolean("cancelled"));
   }



   /*******************************************************************************
    ** A one-time secret produced by an async step on A is handed out once, by
    ** B's status poll, through the encrypted shared store - and not again by A.
    *******************************************************************************/
   @Test
   @Order(4)
   void oneTimeSecretHandsOffAcrossNodesOnce() throws Exception
   {
      JSONObject started = onA.init(SampleClusterProcesses.PROCESS_SECRET, "{\"sleepMillis\":750}", 1);
      assertEquals("JOB_STARTED", started.getString("type"), started.toString());
      String processUUID = started.getString("processUUID");
      String jobUUID     = started.getString("jobUUID");

      JSONObject done = onB.awaitDone(SampleClusterProcesses.PROCESS_SECRET, processUUID, jobUUID, Duration.ofSeconds(20));
      assertEquals("node-a", done.getJSONObject("values").getString("issuedOn"));
      assertTrue(secretValue(done).startsWith("sample-secret-"), done.toString());

      for(SampleProcessHttp node : List.of(onA, onB))
      {
         JSONObject again = node.status(SampleClusterProcesses.PROCESS_SECRET, processUUID, jobUUID);
         assertFalse(again.has("oneTimeSecrets") && !again.getJSONArray("oneTimeSecrets").isEmpty(), "handed out a second time: " + again);
      }
   }



   /*******************************************************************************
    ** A cancel requested on B reaches the step running on A.
    *******************************************************************************/
   @Test
   @Order(5)
   void cancelOnBReachesStepOnA() throws Exception
   {
      JSONObject started = onA.init(SampleClusterProcesses.PROCESS_WORK, "{\"seconds\":60}", 1);
      String     processUUID = started.getString("processUUID");
      String     jobUUID     = started.getString("jobUUID");
      awaitRunning(onB, processUUID, jobUUID);

      JSONObject cancel = onB.run(SampleClusterProcesses.PROCESS_CANCEL_JOB, "{\"jobUUID\":\"" + jobUUID + "\"}");
      assertEquals("node-b", cancel.getJSONObject("values").getString("cancelRequestedOn"));

      JSONObject done = onB.awaitDone(SampleClusterProcesses.PROCESS_WORK, processUUID, jobUUID, Duration.ofSeconds(20));
      assertEquals("COMPLETE", done.getString("type"), done.toString());
      assertEquals("node-a", done.getJSONObject("values").getString("ranOn"));
      assertTrue(done.getJSONObject("values").getBoolean("cancelled"), "the step on A saw B's cancel");
   }



   /*******************************************************************************
    ** ClusterCaches invalidation on A clears B's memo (EsbClusterBus over the
    ** broker): B keeps serving its cached value until A invalidates.
    *******************************************************************************/
   @Test
   @Order(6)
   void cacheInvalidationCrossesNodes() throws Exception
   {
      String original = readFirstName(onB);
      assertEquals(original, readFirstName(onA));

      String changed = "Clustered" + UUID.randomUUID().toString().substring(0, 8);
      try(Connection connection = DriverManager.getConnection(jdbcUrl, "sa", ""); PreparedStatement update = connection.prepareStatement("UPDATE person SET first_name = ? WHERE id = 1"))
      {
         update.setString(1, changed);
         assertEquals(1, update.executeUpdate());
      }
      assertEquals(original, readFirstName(onB), "B serves its cached value until it is invalidated");

      JSONObject invalidated = onA.run(SampleClusterProcesses.PROCESS_CACHE_INVALIDATE, null);
      assertEquals("node-a", invalidated.getJSONObject("values").getString("invalidatedOn"));
      awaitCondition("node B's memo to be invalidated by node A", () -> changed.equals(readFirstName(onB)));
      assertEquals(changed, readFirstName(onA));
   }



   /*******************************************************************************
    ** Killing A in the middle of an async step: B reports the "server stopped"
    ** error within lostAfterSeconds instead of RUNNING forever.
    *******************************************************************************/
   @Test
   @Order(7)
   void killedNodeIsReportedAsStopped() throws Exception
   {
      JSONObject started = onA.init(SampleClusterProcesses.PROCESS_WORK, "{\"seconds\":120}", 1);
      String     processUUID = started.getString("processUUID");
      String     jobUUID     = started.getString("jobUUID");
      awaitRunning(onB, processUUID, jobUUID);

      nodeA.kill();
      long killedAt = System.nanoTime();
      JSONObject done = onB.awaitDone(SampleClusterProcesses.PROCESS_WORK, processUUID, jobUUID, Duration.ofSeconds(30));
      Duration   took = Duration.ofNanos(System.nanoTime() - killedAt);

      assertEquals("ERROR", done.optString("type"), done.toString());
      assertTrue(done.toString().contains(AsyncJobRegistry.LOST_NODE_MESSAGE), done.toString());
      assertTrue(took.compareTo(Duration.ofSeconds(LOST_AFTER_SECONDS + HEARTBEAT_SECONDS + 5)) < 0, "reported after " + took);
   }



   /*******************************************************************************
    ** A draining node answers /ready 503 (so the load balancer drops it), and
    ** still finishes the requests it gets.
    *******************************************************************************/
   @Test
   @Order(8)
   void drainingNodeIsNotReady() throws Exception
   {
      JSONObject drained = onB.run(SampleClusterProcesses.PROCESS_DRAIN, null);
      assertEquals("node-b", drained.getJSONObject("values").getString("drainingNode"));

      HttpResponse<String> ready = onB.get("/ready");
      assertEquals(503, ready.statusCode(), ready.body());
      assertEquals("draining", new JSONObject(ready.body()).getString("reason"));
      assertEquals(200, onB.get("/health").statusCode(), "draining is not dead");
   }



   /*******************************************************************************
    ** A worker-shaped node (no web role) joins the same database: it answers 503
    ** on application routes and 200 on its probes.
    *******************************************************************************/
   @Test
   @Order(9)
   void workerNodeServesProbesOnly() throws Exception
   {
      startWorkerNode();
      SampleProcessHttp onC = onA.on(nodeC.awaitReady());

      HttpResponse<String> app = onC.get("/qqq/v1/metaData");
      assertEquals(503, app.statusCode(), app.body());
      assertTrue(app.body().contains("no web role"), app.body());
      assertEquals(200, onC.get("/health").statusCode());
      assertEquals(200, onC.get("/ready").statusCode());
   }



   /*******************************************************************************
    ** SIGTERM on a node: /ready turns 503 during QQQ_DRAIN_DELAY_SECONDS, before
    ** the server stops. (Unix only: on Windows, Process.destroy is a hard kill.)
    *******************************************************************************/
   @Test
   @Order(10)
   void sigtermDrainsBeforeStopping() throws Exception
   {
      assumeFalse(System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win"), "SIGTERM is not available on Windows");
      if(nodeC == null)
      {
         startWorkerNode();
      }
      SampleProcessHttp onC = onA.on(nodeC.awaitReady());
      assertEquals(200, onC.get("/ready").statusCode());

      nodeC.terminate();
      awaitCondition("/ready to answer 503 while draining", () ->
      {
         HttpResponse<String> ready = onC.get("/ready");
         return (ready.statusCode() == 503 && ready.body().contains("draining"));
      });
      assertTrue(nodeC.isAlive(), "the node is still up during the drain delay");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void startWorkerNode() throws Exception
   {
      if(nodeB != null)
      {
         nodeB.close();
         nodeB = null;
      }
      nodeC = startNode("node-c", "worker,scheduler,esb", false, true, freePort(), Map.of("QQQ_DRAIN_DELAY_SECONDS", "5"));
   }



   /*******************************************************************************
    ** Start a packaged sample node in cluster mode.
    *******************************************************************************/
   private PackagedSampleServer startNode(String nodeId, String roles, boolean prime, boolean embeddedBroker, int port, Map<String, String> extraEnvironment) throws Exception
   {
      Map<String, String> environment = new HashMap<>();
      environment.put("SAMPLE_CLUSTER_MODE", "true");
      environment.put("SAMPLE_JDBC_URL", jdbcUrl);
      environment.put("SAMPLE_CLUSTER_PRIME_DATABASE", Boolean.toString(prime));
      environment.put("SAMPLE_CLUSTER_HEARTBEAT_SECONDS", Integer.toString(HEARTBEAT_SECONDS));
      environment.put("SAMPLE_CLUSTER_LOST_AFTER_SECONDS", Integer.toString(LOST_AFTER_SECONDS));
      environment.put("QQQ_STATE_ENCRYPTION_KEY", encryptionKey);
      environment.put("QQQ_NODE_ID", nodeId);
      environment.put("QQQ_NODE_ROLES", roles);
      environment.putAll(extraEnvironment);
      List<String> jvmOptions = List.of("-Xmx512m", "-Dqqq.sample.esb.port=" + port, "-Dqqq.sample.esb.embedded=" + embeddedBroker);
      return (PackagedSampleServer.start(SampleJavalinServer.class, directory, List.of(), jvmOptions, environment));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static JSONObject awaitRunning(SampleProcessHttp node, String processUUID, String jobUUID) throws Exception
   {
      long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
      while(System.nanoTime() < deadline)
      {
         JSONObject status = node.status(SampleClusterProcesses.PROCESS_WORK, processUUID, jobUUID);
         if("RUNNING".equals(status.optString("type")) && status.has("message"))
         {
            return (status);
         }
         Thread.sleep(100);
      }
      throw (new AssertionError("The step never showed as running on the other node"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String readFirstName(SampleProcessHttp node) throws Exception
   {
      return (node.run(SampleClusterProcesses.PROCESS_CACHE_READ, "{\"personId\":1}").getJSONObject("values").getString("firstName"));
   }



   /*******************************************************************************
    ** A qqq_state row's node_id and payload, read straight from the database.
    *******************************************************************************/
   private Map<String, String> storedState(String stateKey) throws Exception
   {
      try(Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
         PreparedStatement query = connection.prepareStatement("SELECT node_id, payload FROM qqq_state WHERE state_key = ?"))
      {
         query.setString(1, stateKey);
         try(ResultSet rows = query.executeQuery())
         {
            assertTrue(rows.next(), "no qqq_state row " + stateKey);
            Map<String, String> row = new HashMap<>();
            row.put("node_id", rows.getString(1));
            row.put("payload", rows.getString(2));
            assertNotEquals(null, row.get("payload"));
            return (row);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static int freePort() throws Exception
   {
      try(ServerSocket socket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")))
      {
         return (socket.getLocalPort());
      }
   }
}
