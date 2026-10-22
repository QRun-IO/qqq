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

package com.kingsrook.qqq.middleware.javalin.health;


import java.util.LinkedHashMap;
import java.util.Map;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;


/*******************************************************************************
 ** Versioned, explicit schemas for redacted health projections.
 *******************************************************************************/
final class HealthSchemasV1
{
   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private HealthSchemasV1()
   {
   }



   /*******************************************************************************
    ** Shared shapes and the four endpoint response contracts.
    *******************************************************************************/
   static Map<String, Schema> all()
   {
      Map<String, Schema> schemas = new LinkedHashMap<>();
      schemas.put("HealthCountsV1", object("up", integer(), "degraded", integer(), "down", integer(), "unknown", integer()));
      schemas.put("HealthRunnerV1", object("lastBeatAt", text(), "pods", array(text())));
      schemas.put("HealthCheckV1", object("name", text(), "label", text(), "kind", text(), "severity", text(),
         "currentStatus", text(), "currentMessage", text(), "currentSince", text(), "lastRunAt", text(), "lastLatencyMs", integer()));
      schemas.put("HealthComponentV1", object("name", text(), "label", text(), "status", text(), "since", text(),
         "checksUp", integer(), "checksDegraded", integer(), "checksDown", integer(), "checksUnknown", integer(),
         "worstCheckId", integer(), "updatedAt", text(), "checks", array(ref("HealthCheckV1"))));
      schemas.put("HealthSummaryV1", object("status", text(), "since", text(), "counts", ref("HealthCountsV1"),
         "components", array(ref("HealthComponentV1")), "runner", ref("HealthRunnerV1")));
      schemas.put("HealthResultV1", object("id", integer(), "status", text(), "message", text(), "detailsJson", text(), "metricsJson", text(),
         "latencyMs", integer(), "startedAt", text(), "finishedAt", text(), "runnerPod", text(), "isTransition", new Schema().withType(Type.BOOLEAN), "triggeredBy", text()));
      schemas.put("HealthHistoryV1", object("results", array(ref("HealthResultV1"))));
      schemas.put("HealthDetailsV1", object("values", new Schema().withType(Type.OBJECT).withAdditionalProperties(true)));
      schemas.put("HealthMetricV1", object("name", text(), "value", new Schema().withType(Type.NUMBER), "unit", text()));
      schemas.put("HealthRunV1", object("status", text(), "message", text(), "details", ref("HealthDetailsV1"),
         "latencyMs", integer(), "metrics", array(ref("HealthMetricV1"))));
      return (schemas);
   }



   /*******************************************************************************
    ** Object properties from name/schema pairs.
    *******************************************************************************/
   private static Schema object(Object... fields)
   {
      Map<String, Schema> properties = new LinkedHashMap<>();
      for(int i = 0; i < fields.length; i += 2)
      {
         properties.put((String) fields[i], (Schema) fields[i + 1]);
      }
      return (new Schema().withType(Type.OBJECT).withProperties(properties));
   }



   /*******************************************************************************
    ** Nullable persisted text and timestamps.
    *******************************************************************************/
   private static Schema text()
   {
      return (new Schema().withType(Type.STRING).withNullable(true));
   }



   /*******************************************************************************
    ** Numeric counters and latency.
    *******************************************************************************/
   private static Schema integer()
   {
      return (new Schema().withType(Type.INTEGER).withNullable(true));
   }



   /*******************************************************************************
    ** Array shape.
    *******************************************************************************/
   private static Schema array(Schema items)
   {
      return (new Schema().withType(Type.ARRAY).withItems(items));
   }



   /*******************************************************************************
    ** Component reference.
    *******************************************************************************/
   private static Schema ref(String name)
   {
      return (new Schema().withRefToSchema(name));
   }
}
