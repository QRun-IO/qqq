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

package com.kingsrook.qqq.backend.core.health;


import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.alerts.AlertSeverity;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;


/*******************************************************************************
 ** Container restarts of this application's pods, read from the Kubernetes
 ** API, so a crash (OutOfMemoryError, OOMKilled) or a crash loop shows on the
 ** System Health page and raises the standard health.check.* alerts.
 **
 ** Opt-in: registered only when QQQ_HEALTH_POD_RESTARTS_SELECTOR (a label
 ** selector such as "app in (website-backend,website-backend-worker)") is set
 ** and a service account token is mounted. The pod's service account needs
 ** get/list on pods in its namespace.
 **
 ** DOWN: a container is in CrashLoopBackOff. DEGRADED: a container's last
 ** termination finished inside the window (QQQ_HEALTH_POD_RESTARTS_WINDOW_MINUTES,
 ** default 60), with its cause: JVM out of memory (exit 3), OOMKilled, or the
 ** exit code. A DEPENDENCY check: it never affects /ready or /live.
 *******************************************************************************/
public class KubernetesPodRestartsCheck implements HealthCheck
{
   public static final String NAME = "core.kubernetes.pod_restarts";

   private static final String SERVICE_ACCOUNT = "/var/run/secrets/kubernetes.io/serviceaccount";

   private final String  selector;
   private final Duration window;
   private final Fetcher fetcher;
   private final Clock   clock;



   /*******************************************************************************
    ** Reads the pod list JSON (a seam for tests).
    *******************************************************************************/
   @FunctionalInterface
   public interface Fetcher
   {
      /***************************************************************************
       ** The JSON body of GET /api/v1/namespaces/{ns}/pods?labelSelector=...
       ***************************************************************************/
      String fetch(String labelSelector) throws Exception;
   }



   /*******************************************************************************
    ** Constructor.
    *******************************************************************************/
   public KubernetesPodRestartsCheck(String selector, Duration window, Fetcher fetcher, Clock clock)
   {
      this.selector = selector;
      this.window = window;
      this.fetcher = fetcher;
      this.clock = clock;
   }



   /*******************************************************************************
    ** The registration when this process runs in Kubernetes and the host opted
    ** in; null otherwise.
    *******************************************************************************/
   public static HealthCheckMetaData definitionFromEnvironment(QMetaDataVariableInterpreter interpreter)
   {
      String selector = interpreter.getStringFromPropertyOrEnvironment("qqq.health.podRestarts.selector", "QQQ_HEALTH_POD_RESTARTS_SELECTOR", null);
      if(!StringUtils.hasContent(selector) || !Files.isReadable(Path.of(SERVICE_ACCOUNT, "token")) || !StringUtils.hasContent(System.getenv("KUBERNETES_SERVICE_HOST")))
      {
         return (null);
      }
      int minutes = Math.max(5, interpreter.getIntegerFromPropertyOrEnvironment("qqq.health.podRestarts.windowMinutes", "QQQ_HEALTH_POD_RESTARTS_WINDOW_MINUTES", 60));
      return (new HealthCheckMetaData().withName(NAME).withLabel("Pod restarts").withComponent("core")
         .withCategory(HealthCategory.CORE).withKind(HealthCheckKind.DEPENDENCY).withSeverity(AlertSeverity.ERROR)
         .withIntervalSeconds(60).withTimeoutMs(5000).withFailureThreshold(1).withRecoveryThreshold(1)
         .withCheckInstance(new KubernetesPodRestartsCheck(selector.trim(), Duration.ofMinutes(minutes), KubernetesPodRestartsCheck::fetchInCluster, Clock.systemUTC())));
   }



   /*******************************************************************************
    ** Evaluate the pod list.
    *******************************************************************************/
   @Override
   public HealthCheckOutcome run(HealthCheckInput input)
   {
      JSONObject list;
      try
      {
         list = new JSONObject(fetcher.fetch(selector));
      }
      catch(Exception e)
      {
         return (HealthCheckOutcome.unknown("Pod status unavailable from the Kubernetes API (" + e.getClass().getSimpleName() + ")"));
      }

      Instant      since       = clock.instant().minus(window);
      List<String> recent      = new ArrayList<>();
      List<String> crashLoops  = new ArrayList<>();
      int          pods        = 0;
      int          oomRestarts = 0;
      JSONArray    items       = list.optJSONArray("items");
      for(int i = 0; items != null && i < items.length(); i++)
      {
         JSONObject pod     = items.getJSONObject(i);
         String     podName = pod.optJSONObject("metadata") == null ? "?" : pod.getJSONObject("metadata").optString("name", "?");
         pods++;
         JSONArray statuses = pod.optJSONObject("status") == null ? null : pod.getJSONObject("status").optJSONArray("containerStatuses");
         for(int j = 0; statuses != null && j < statuses.length(); j++)
         {
            JSONObject status  = statuses.getJSONObject(j);
            JSONObject waiting = status.optJSONObject("state") == null ? null : status.getJSONObject("state").optJSONObject("waiting");
            if(waiting != null && "CrashLoopBackOff".equals(waiting.optString("reason")))
            {
               crashLoops.add(podName);
            }
            JSONObject terminated = status.optJSONObject("lastState") == null ? null : status.getJSONObject("lastState").optJSONObject("terminated");
            if(terminated == null)
            {
               continue;
            }
            Instant finished = parse(terminated.optString("finishedAt", null));
            if(finished != null && finished.isAfter(since))
            {
               String cause = cause(terminated.optString("reason", ""), terminated.optInt("exitCode", -1));
               if(cause.contains("out of memory"))
               {
                  oomRestarts++;
               }
               recent.add(podName + " at " + finished + " (" + cause + ", " + status.optInt("restartCount", 0) + " restarts since the pod started)");
            }
         }
      }

      HealthDetails details = new HealthDetails().withValue("pods", pods).withValue("recentRestarts", recent.size())
         .withValue("outOfMemoryRestarts", oomRestarts).withValue("crashLooping", crashLoops.size()).withValue("windowMinutes", window.toMinutes());
      HealthCheckOutcome outcome;
      if(!crashLoops.isEmpty())
      {
         outcome = HealthCheckOutcome.down("Crash loop: " + String.join(", ", crashLoops) + (recent.isEmpty() ? "" : "; last restarts: " + String.join("; ", recent)));
      }
      else if(!recent.isEmpty())
      {
         outcome = HealthCheckOutcome.degraded("Restarted in the last " + window.toMinutes() + " minutes: " + String.join("; ", recent));
      }
      else
      {
         outcome = HealthCheckOutcome.up("No restarts in the last " + window.toMinutes() + " minutes across " + pods + " pods");
      }
      return (outcome.withDetails(details).withMetrics(List.of(new HealthMetric().withName("recentRestarts").withValue(recent.size()).withUnit("restarts"))));
   }



   /*******************************************************************************
    ** Plain-language termination cause. Exit 3 is the JVM's
    ** -XX:+ExitOnOutOfMemoryError; OOMKilled / 137 is the container limit.
    *******************************************************************************/
   static String cause(String reason, int exitCode)
   {
      if("OOMKilled".equals(reason))
      {
         return ("container out of memory (OOMKilled)");
      }
      if(exitCode == 3)
      {
         return ("JVM out of memory (exit 3)");
      }
      if(exitCode == 137)
      {
         return ("killed (exit 137)");
      }
      return ((StringUtils.hasContent(reason) ? reason : "terminated") + " (exit " + exitCode + ")");
   }



   /*******************************************************************************
    ** RFC 3339 timestamp or null.
    *******************************************************************************/
   private static Instant parse(String value)
   {
      try
      {
         return (value == null ? null : Instant.parse(value));
      }
      catch(Exception e)
      {
         return (null);
      }
   }



   /*******************************************************************************
    ** In-cluster GET with the mounted service account token and CA.
    *******************************************************************************/
   static String fetchInCluster(String labelSelector) throws Exception
   {
      String namespace = Files.readString(Path.of(SERVICE_ACCOUNT, "namespace"), StandardCharsets.UTF_8).trim();
      String token     = Files.readString(Path.of(SERVICE_ACCOUNT, "token"), StandardCharsets.UTF_8).trim();
      KeyStore trust = KeyStore.getInstance(KeyStore.getDefaultType());
      trust.load(null, null);
      try(InputStream ca = Files.newInputStream(Path.of(SERVICE_ACCOUNT, "ca.crt")))
      {
         int n = 0;
         for(Certificate certificate : CertificateFactory.getInstance("X.509").generateCertificates(ca))
         {
            trust.setCertificateEntry("k8s-ca-" + n++, certificate);
         }
      }
      TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      factory.init(trust);
      SSLContext ssl = SSLContext.getInstance("TLS");
      ssl.init(null, factory.getTrustManagers(), null);

      String host = System.getenv("KUBERNETES_SERVICE_HOST");
      String port = StringUtils.hasContent(System.getenv("KUBERNETES_SERVICE_PORT")) ? System.getenv("KUBERNETES_SERVICE_PORT") : "443";
      URI uri = URI.create("https://" + (host.contains(":") ? "[" + host + "]" : host) + ":" + port + "/api/v1/namespaces/"
         + URLEncoder.encode(namespace, StandardCharsets.UTF_8) + "/pods?labelSelector=" + URLEncoder.encode(labelSelector, StandardCharsets.UTF_8));
      try(HttpClient client = HttpClient.newBuilder().sslContext(ssl).connectTimeout(Duration.ofSeconds(3)).build())
      {
         HttpResponse<String> response = client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(4)).header("Authorization", "Bearer " + token)
            .header("Accept", "application/json").GET().build(), HttpResponse.BodyHandlers.ofString());
         if(response.statusCode() != 200)
         {
            throw (new IllegalStateException("Kubernetes API returned " + response.statusCode()));
         }
         return (response.body());
      }
   }
}
