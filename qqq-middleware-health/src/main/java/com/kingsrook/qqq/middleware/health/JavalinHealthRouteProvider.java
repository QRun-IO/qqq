/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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


import com.kingsrook.qqq.backend.core.health.HealthMonitoringMetaData;
import com.kingsrook.qqq.backend.core.health.HealthStatus;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthCheckMetaData;
import com.kingsrook.qqq.middleware.javalin.QJavalinRouteProviderInterface;
import io.javalin.config.JavalinConfig;
import static io.javalin.apibuilder.ApiBuilder.get;


/*******************************************************************************
 ** Unauthenticated liveness, readiness and optional Prometheus routes.
 *******************************************************************************/
public class JavalinHealthRouteProvider implements QJavalinRouteProviderInterface
{
   private QInstance qInstance;



   /*******************************************************************************
    ** Called during server setup and instance replacement.
    *******************************************************************************/
   @Override
   public void setQInstance(QInstance qInstance)
   {
      this.qInstance = qInstance;
   }



   /*******************************************************************************
    ** These routes are outside the UI API and never run dependency checks.
    *******************************************************************************/
   @Override
   public void acceptJavalinConfig(JavalinConfig config)
   {
      HealthCheckMetaData legacy = QSupplementalInstanceMetaData.of(qInstance, HealthCheckMetaData.METADATA_KEY);
      HealthMonitoringMetaData monitoring = QSupplementalInstanceMetaData.of(qInstance, HealthMonitoringMetaData.NAME);
      if((legacy == null && monitoring == null) || (legacy != null && !Boolean.TRUE.equals(legacy.getEnabled())))
      {
         return;
      }
      String healthPath = legacy == null ? "/health" : legacy.getEndpointPath();
      String readyPath = legacy == null ? "/ready" : legacy.getReadinessEndpointPath();
      config.routes.apiBuilder(() ->
      {
         get(healthPath, ctx ->
         {
            var body = HealthProbes.run(qInstance, false);
            ctx.status(body.get("status") == HealthStatus.DOWN ? 503 : 200).json(body);
         });
         get(readyPath, ctx ->
         {
            var body = HealthProbes.run(qInstance, true);
            ctx.status(body.get("status") == HealthStatus.DOWN ? 503 : 200).json(body);
         });
         if(monitoring != null && monitoring.getPrometheusEnabled())
         {
            get("/metrics", ctx -> ctx.contentType("text/plain; version=0.0.4; charset=utf-8").result(HealthMetrics.render(qInstance)));
         }
      });
   }
}
