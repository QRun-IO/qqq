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

package com.kingsrook.qqq.esb.health;


import java.time.Instant;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.health.HealthCheckInput;
import com.kingsrook.qqq.backend.core.health.HealthCheckKind;
import com.kingsrook.qqq.backend.core.health.HealthChecksMetaData;
import com.kingsrook.qqq.backend.core.health.HealthStatus;
import com.kingsrook.qqq.esb.EsbTestBase;
import com.kingsrook.qqq.esb.connection.EsbConnectionManager;
import com.kingsrook.qqq.esb.management.MockManagementServer;
import com.kingsrook.qqq.esb.model.EsbDestinationType;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.EsbProviderType;
import com.kingsrook.qqq.esb.model.QEsbDestinationMetaData;
import com.kingsrook.qqq.esb.runtime.QEsbRuntime;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Central checks reuse ESB runtime signals and management adapters.
 *******************************************************************************/
class EsbHealthChecksTest extends EsbTestBase
{
   /*******************************************************************************
    ** Enrichment is idempotent and connection checks distinguish not-started.
    *******************************************************************************/
   @Test
   void connectionStates() throws Exception
   {
      var instance = QContext.getQInstance();
      EsbInstanceMetaData.of(instance).enrich(instance);
      EsbInstanceMetaData.of(instance).enrich(instance);
      var checks = HealthChecksMetaData.ofOrWithNew(instance).getChecks();
      assertThat(checks).hasSize(2).allSatisfy(check -> assertThat(check.getKind()).isEqualTo(HealthCheckKind.DEPENDENCY));
      var connection = checks.stream().filter(check -> check.getName().endsWith("." + PROVIDER_NAME)).findFirst().orElseThrow();
      var input = new HealthCheckInput().withQInstance(instance).withDeadline(Instant.now().plusSeconds(5));
      QEsbRuntime runtime = QEsbRuntime.getInstance();
      runtime.stop();
      assertThat(connection.getCheckInstance().run(input).getStatus()).isEqualTo(HealthStatus.UNKNOWN);
      runtime.start(instance);
      try
      {
         EsbConnectionManager.getInstance().closeAll();
         assertThat(connection.getCheckInstance().run(input).getStatus()).isEqualTo(HealthStatus.DOWN);
         try(var session = EsbConnectionManager.getInstance().openSession(PROVIDER_NAME, false))
         {
            assertThat(connection.getCheckInstance().run(input).getStatus()).isEqualTo(HealthStatus.UP);
         }
      }
      finally
      {
         runtime.stop();
      }
   }



   /*******************************************************************************
    ** No management means UNKNOWN; queue depths yield a numeric gauge.
    *******************************************************************************/
   @Test
   void deadLettersAndExpiryDepth() throws Exception
   {
      var instance = QContext.getQInstance();
      EsbInstanceMetaData.of(instance).withDestination(new QEsbDestinationMetaData().withName("DLQ").withProviderName(PROVIDER_NAME).withType(EsbDestinationType.QUEUE))
         .withDestination(new QEsbDestinationMetaData().withName("ExpiryQueue").withProviderName(PROVIDER_NAME).withType(EsbDestinationType.QUEUE));
      EsbHealthChecks.register(instance);
      var check = HealthChecksMetaData.ofOrWithNew(instance).getChecks().stream().filter(definition -> definition.getName().equals("core.esb.dead_letters")).findFirst().orElseThrow();
      var input = new HealthCheckInput().withQInstance(instance).withDeadline(Instant.now().plusSeconds(5));
      assertThat(check.getCheckInstance().run(input).getStatus()).isEqualTo(HealthStatus.UNKNOWN);
      try(MockManagementServer management = new MockManagementServer())
      {
         EsbInstanceMetaData.of(instance).getProvider(PROVIDER_NAME).withType(EsbProviderType.RABBITMQ)
            .withUrl("amqp://localhost/").withManagementUrl(management.getBaseUrl());
         management.withResponse(200, "{\"messages\":0,\"consumers\":0}").withResponse(404, "{}");
         assertThat(check.getCheckInstance().run(input).getStatus()).isEqualTo(HealthStatus.UP);
         management.withResponse(200, "{\"messages\":7,\"consumers\":0}").withResponse(200, "{\"messages\":0,\"consumers\":0}");
         var outcome = check.getCheckInstance().run(input);
         assertThat(outcome.getStatus()).isEqualTo(HealthStatus.DEGRADED);
         assertThat(outcome.getMetrics().getFirst().getName()).isEqualTo("depth");
         assertThat(outcome.getMetrics().getFirst().getValue().longValue()).isEqualTo(7);
         management.withResponse(500, "password=sentinel");
         outcome = check.getCheckInstance().run(input);
         assertThat(outcome.getStatus()).isEqualTo(HealthStatus.DOWN);
         assertThat(outcome.getMessage()).doesNotContain("sentinel");
      }
   }
}
