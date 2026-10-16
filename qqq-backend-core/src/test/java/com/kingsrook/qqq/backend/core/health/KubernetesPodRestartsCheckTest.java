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


import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Restarts read from the Kubernetes pod list reach System Health with their
 ** cause. Fixture: the kof22 web pod that exited with ExitOnOutOfMemoryError
 ** (exit 3) at 19:20:18Z on 2026-10-16.
 *******************************************************************************/
class KubernetesPodRestartsCheckTest
{
   private static final Clock NOW = Clock.fixed(Instant.parse("2026-10-16T19:40:00Z"), ZoneOffset.UTC);



   /*******************************************************************************
    ** A JVM out-of-memory restart inside the window is DEGRADED with its cause.
    *******************************************************************************/
   @Test
   void outOfMemoryRestartIsDegraded()
   {
      HealthCheckOutcome outcome = check(pods(pod("website-backend-74bdc7754c-dc7wc", "{\"running\":{}}", terminated("Error", 3, "2026-10-16T19:20:18Z"), 1),
         pod("website-backend-74bdc7754c-tk7ws", "{\"running\":{}}", null, 0)));
      assertThat(outcome.getStatus()).isEqualTo(HealthStatus.DEGRADED);
      assertThat(outcome.getMessage()).contains("website-backend-74bdc7754c-dc7wc", "JVM out of memory (exit 3)");
      assertThat(outcome.getDetails().getValues()).containsEntry("recentRestarts", 1).containsEntry("outOfMemoryRestarts", 1).containsEntry("pods", 2);
   }



   /*******************************************************************************
    ** Container-limit kills and old restarts.
    *******************************************************************************/
   @Test
   void oomKilledAndOldRestarts()
   {
      assertThat(check(pods(pod("a", "{\"running\":{}}", terminated("OOMKilled", 137, "2026-10-16T19:30:00Z"), 4))).getMessage()).contains("OOMKilled");
      HealthCheckOutcome old = check(pods(pod("a", "{\"running\":{}}", terminated("Error", 3, "2026-10-16T17:00:00Z"), 1)));
      assertThat(old.getStatus()).isEqualTo(HealthStatus.UP);
      assertThat(old.getMessage()).contains("No restarts in the last 60 minutes across 1 pods");
   }



   /*******************************************************************************
    ** CrashLoopBackOff is DOWN.
    *******************************************************************************/
   @Test
   void crashLoopIsDown()
   {
      HealthCheckOutcome outcome = check(pods(pod("b", "{\"waiting\":{\"reason\":\"CrashLoopBackOff\"}}", terminated("Error", 3, "2026-10-16T19:39:00Z"), 6)));
      assertThat(outcome.getStatus()).isEqualTo(HealthStatus.DOWN);
      assertThat(outcome.getMessage()).startsWith("Crash loop: b");
   }



   /*******************************************************************************
    ** API failures are UNKNOWN, never DOWN; without opt-in nothing registers.
    *******************************************************************************/
   @Test
   void unavailableAndOptIn()
   {
      HealthCheckOutcome outcome = new KubernetesPodRestartsCheck("app=x", Duration.ofMinutes(60), selector ->
      {
         throw (new IllegalStateException("Kubernetes API returned 403"));
      }, NOW).run(new HealthCheckInput());
      assertThat(outcome.getStatus()).isEqualTo(HealthStatus.UNKNOWN);
      assertThat(KubernetesPodRestartsCheck.definitionFromEnvironment(new QMetaDataVariableInterpreter())).isNull();
      assertThat(KubernetesPodRestartsCheck.cause("", 1)).isEqualTo("terminated (exit 1)");
   }



   /*******************************************************************************
    ** Run against a JSON body.
    *******************************************************************************/
   private static HealthCheckOutcome check(String json)
   {
      return (new KubernetesPodRestartsCheck("app=website-backend", Duration.ofMinutes(60), selector -> json, NOW).run(new HealthCheckInput()));
   }



   /*******************************************************************************
    ** Pod list.
    *******************************************************************************/
   private static String pods(String... pods)
   {
      return ("{\"kind\":\"PodList\",\"items\":[" + String.join(",", pods) + "]}");
   }



   /*******************************************************************************
    ** One pod with one container.
    *******************************************************************************/
   private static String pod(String name, String state, String lastTerminated, int restarts)
   {
      return ("{\"metadata\":{\"name\":\"" + name + "\"},\"status\":{\"containerStatuses\":[{\"name\":\"website-backend\",\"restartCount\":" + restarts
         + ",\"state\":" + state + ",\"lastState\":" + (lastTerminated == null ? "{}" : "{\"terminated\":" + lastTerminated + "}") + "}]}}");
   }



   /*******************************************************************************
    ** Last termination.
    *******************************************************************************/
   private static String terminated(String reason, int exitCode, String finishedAt)
   {
      return ("{\"reason\":\"" + reason + "\",\"exitCode\":" + exitCode + ",\"finishedAt\":\"" + finishedAt + "\"}");
   }
}
