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


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.health.HealthCheckKind;
import com.kingsrook.qqq.backend.core.health.HealthCheckMetaData;
import com.kingsrook.qqq.backend.core.health.HealthMetaDataProvider;
import com.kingsrook.qqq.backend.core.health.HealthRunner;
import com.kingsrook.qqq.backend.core.health.HealthStatus;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;


/*******************************************************************************
 ** Prometheus projection deliberately excludes all messages and details.
 *******************************************************************************/
public final class HealthMetrics
{
   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private HealthMetrics()
   {
   }



   /*******************************************************************************
    ** Prefer live outcomes, falling back to persisted last state across pods.
    *******************************************************************************/
   public static String render(QInstance instance) throws QException
   {
      Map<String, QRecord> rows = new LinkedHashMap<>();
      AtomicReference<QException> failure = new AtomicReference<>();
      QContext.withTemporaryContext(new CapturedContext(instance, new QSession().withPermissions("health.read")), () ->
      {
         try
         {
            new QueryAction().execute(new QueryInput().withTableName(HealthMetaDataProvider.CHECK_TABLE)).getRecords()
               .forEach(row -> rows.put(row.getValueString("name"), row));
         }
         catch(QException e)
         {
            failure.set(e);
         }
      });
      HealthRunner runner = HealthRunner.getForInstance(instance);
      Map<String, HealthCheckMetaData> definitions = new LinkedHashMap<>();
      HealthProbes.definitions(instance).forEach(check -> definitions.put(check.getName(), check));
      for(QRecord row : rows.values())
      {
         if(Boolean.TRUE.equals(row.getValueBoolean("registered")))
         {
            definitions.putIfAbsent(row.getValueString("name"), new HealthCheckMetaData().withName(row.getValueString("name"))
               .withComponent(row.getValueString("component")).withKind(HealthCheckKind.valueOf(row.getValueString("kind"))));
         }
      }
      StringBuilder text = new StringBuilder();
      for(var check : definitions.values())
      {
         var outcome = runner == null ? null : runner.getLastOutcome(check.getName());
         QRecord row = rows.get(check.getName());
         HealthStatus status = outcome != null ? outcome.getStatus() : row == null || row.getValueString("currentStatus") == null
            ? HealthStatus.UNKNOWN : HealthStatus.valueOf(row.getValueString("currentStatus"));
         Long latency = outcome != null ? outcome.getLatencyMs() : row == null ? null : row.getValueLong("lastLatencyMs");
         String labels = "{check=\"" + escape(check.getName()) + "\",component=\"" + escape(check.getComponent()) + "\",kind=\"" + escape(check.getKind().name()) + "\"}";
         int value = switch(status)
         {
            case UP -> 0;
            case DEGRADED -> 1;
            case DOWN -> 2;
            default -> 3;
         };
         text.append("qqq_health_check_status").append(labels).append(' ').append(value).append('\n');
         if(latency != null)
         {
            text.append("qqq_health_check_latency_ms").append(labels).append(' ').append(latency).append('\n');
         }
      }
      if(failure.get() != null && text.isEmpty())
      {
         throw failure.get();
      }
      return (text.toString());
   }



   /*******************************************************************************
    ** Prometheus label escaping order matters: backslashes must be first.
    *******************************************************************************/
   static String escape(String value)
   {
      return (value.replace("\\", "\\\\").replace("\n", "\\n").replace("\"", "\\\""));
   }
}
