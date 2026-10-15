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

package com.kingsrook.qqq.middleware.health;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.health.HealthCheckOutcome;
import com.kingsrook.qqq.backend.core.health.HealthMetric;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit test for HealthMetrics' per-check gauge lines.
 *******************************************************************************/
class HealthMetricsTest
{

   /*******************************************************************************
    ** Each finite metric becomes one labelled gauge line; bad ones are skipped.
    *******************************************************************************/
   @Test
   void appendCheckMetrics()
   {
      List<HealthMetric> metrics = new ArrayList<>();
      metrics.add(new HealthMetric().withName("pool_connectionsBusy").withValue(3).withUnit("count"));
      metrics.add(new HealthMetric().withName("pool_threadsAwaitingCheckout").withValue(0L).withUnit("count"));
      metrics.add(new HealthMetric().withName("bad").withValue(Double.NaN));
      metrics.add(null);

      StringBuilder text = new StringBuilder();
      HealthMetrics.appendCheckMetrics(text, "{check=\"db\",component=\"rdbms\",kind=\"READINESS\"}", HealthCheckOutcome.up("ok").withMetrics(metrics));

      assertThat(text.toString()).isEqualTo("""
         qqq_health_check_metric{check="db",component="rdbms",kind="READINESS",metric="pool_connectionsBusy"} 3
         qqq_health_check_metric{check="db",component="rdbms",kind="READINESS",metric="pool_threadsAwaitingCheckout"} 0
         """);

      StringBuilder none = new StringBuilder();
      HealthMetrics.appendCheckMetrics(none, "{check=\"db\"}", null);
      HealthMetrics.appendCheckMetrics(none, "{check=\"db\"}", HealthCheckOutcome.up("ok"));
      assertThat(none.toString()).isEmpty();
   }

}
