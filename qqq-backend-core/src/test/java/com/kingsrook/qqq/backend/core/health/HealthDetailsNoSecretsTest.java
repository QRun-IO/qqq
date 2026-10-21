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
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.alerts.AlertsInstanceMetaData;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Poisoned provider configuration must never reach stored health or alert data.
 *******************************************************************************/
class HealthDetailsNoSecretsTest extends BaseTest
{


   /*******************************************************************************
    ** Verify health behavior against the public contract.
    *******************************************************************************/
   @Test
   void everyCoreCheckAndPoisonedProbeRemainSafe() throws Exception
   {
      QInstance instance = QContext.getQInstance();
      new HealthMetaDataProvider().defineAll(instance, TestUtils.MEMORY_BACKEND_NAME, null);
      QContext.getQSession().withPermissions("health.read", "health.write");
      HealthRunnerTest.Sink.raises.clear();
      HealthRunnerTest.Sink.resolutions.clear();
      AlertsInstanceMetaData.ofOrWithNew(instance).withSink(new QCodeReference(HealthRunnerTest.Sink.class));
      String sentinel = "S3NT1NEL-secret-value";
      String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjMifQ.signature";
      Map<String, String> configuration = Map.of("password", sentinel);
      HealthChecksMetaData.ofOrWithNew(instance)
         .withCheck(new HealthCheckMetaData().withName("test.poison").withLabel("Poisoned probe").withComponent("fixture").withFailureThreshold(1)
            .withCheckInstance(input ->
            {
               throw new QException("Bearer credentialABC https://user:pass@host/x?token=abc alice@example.com " + jwt + " " + configuration.get("password"));
            }))
         .withCheck(new HealthCheckMetaData().withName("test.details").withLabel("Poisoned details").withComponent("fixture").withFailureThreshold(1)
            .withCheckInstance(input -> HealthCheckOutcome.down("password=" + configuration.get("password"))
               .withDetails(new HealthDetails().withValue("password", configuration.get("password")).withValue("reason", "alice@example.com " + jwt))
               .withMetrics(List.of(new HealthMetric().withName("alice@example.com").withUnit("Bearer credentialABC").withValue(1)))));
      try(HealthRunner runner = new HealthRunner(instance, "pod-a", Clock.systemUTC()))
      {
         for(HealthCheckMetaData check : HealthChecksMetaData.ofOrWithNew(instance).resolveAll(instance))
         {
            HealthCheckOutcome outcome = runner.runNow(check.getName());
            assertSafe(JsonUtils.toJson(outcome), sentinel, jwt);
         }
      }
      for(String table : List.of(HealthMetaDataProvider.CHECK_TABLE, HealthMetaDataProvider.RESULT_TABLE, HealthMetaDataProvider.COMPONENT_TABLE))
      {
         for(QRecord row : HealthStore.query(table, new QQueryFilter()))
         {
            assertSafe(JsonUtils.toJson(row.getValues()), sentinel, jwt);
         }
      }
      assertSafe(JsonUtils.toJson(HealthRunnerTest.Sink.raises), sentinel, jwt);
      assertSafe(JsonUtils.toJson(HealthRunnerTest.Sink.resolutions), sentinel, jwt);
      assertThat(HealthRunnerTest.Sink.raises).hasSize(2);
   }



   /*******************************************************************************
    ** Shared assertion also applies to API response models in downstream modules.
    *******************************************************************************/
   static void assertSafe(String text, String sentinel, String jwt)
   {
      assertThat(text).doesNotContain(sentinel, jwt, "credentialABC", "user:pass", "?token=abc", "alice@example.com");
   }
}
