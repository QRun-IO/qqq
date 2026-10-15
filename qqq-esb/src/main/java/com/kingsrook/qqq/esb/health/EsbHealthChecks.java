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


import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import com.kingsrook.qqq.backend.core.alerts.AlertSeverity;
import com.kingsrook.qqq.backend.core.health.HealthCategory;
import com.kingsrook.qqq.backend.core.health.HealthCheck;
import com.kingsrook.qqq.backend.core.health.HealthCheckInput;
import com.kingsrook.qqq.backend.core.health.HealthCheckKind;
import com.kingsrook.qqq.backend.core.health.HealthCheckMetaData;
import com.kingsrook.qqq.backend.core.health.HealthCheckOutcome;
import com.kingsrook.qqq.backend.core.health.HealthChecksMetaData;
import com.kingsrook.qqq.backend.core.health.HealthMetric;
import com.kingsrook.qqq.backend.core.health.HealthRedactor;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.esb.connection.EsbConnectionManager;
import com.kingsrook.qqq.esb.management.EsbBrokerAdapters;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.EsbProcessMetaData;
import com.kingsrook.qqq.esb.model.EsbTrigger;
import com.kingsrook.qqq.esb.model.QEsbDestinationMetaData;
import com.kingsrook.qqq.esb.runtime.QEsbRuntime;


/*******************************************************************************
 ** Register passive connection checks and read-only broker management checks.
 *******************************************************************************/
public final class EsbHealthChecks
{
   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private EsbHealthChecks()
   {
   }



   /*******************************************************************************
    ** Enrichment may run repeatedly; do not register duplicate check names.
    *******************************************************************************/
   public static void register(QInstance instance)
   {
      HealthChecksMetaData registry = HealthChecksMetaData.ofOrWithNew(instance);
      for(String provider : EsbInstanceMetaData.of(instance).getProviders().keySet())
      {
         if(provider == null || provider.isBlank())
         {
            continue;
         }
         add(registry, "core.esb.connection." + HealthChecksMetaData.toNamePart(provider), 30, AlertSeverity.ERROR, input ->
         {
            if(!Boolean.TRUE.equals(QEsbRuntime.getInstance().isRunning()))
            {
               return (HealthCheckOutcome.unknown("ESB runtime not started"));
            }
            return (EsbConnectionManager.getInstance().isConnected(provider)
               ? HealthCheckOutcome.up("Connected") : HealthCheckOutcome.down("Disconnected"));
         });
      }
      add(registry, "core.esb.dead_letters", 300, AlertSeverity.WARNING, EsbHealthChecks::deadLetters);
   }



   /*******************************************************************************
    ** Adapt the body while retaining a stable SPI name.
    *******************************************************************************/
   private static void add(HealthChecksMetaData registry, String name, int interval, AlertSeverity severity, Probe probe)
   {
      if(registry.getChecks().stream().anyMatch(check -> name.equals(check.getName())))
      {
         return;
      }
      registry.withCheck(new HealthCheckMetaData().withName(name).withLabel(name).withComponent("esb")
         .withCategory(HealthCategory.MESSAGING).withKind(HealthCheckKind.DEPENDENCY)
         .withIntervalSeconds(interval).withTimeoutMs(5000).withSeverity(severity).withCheckInstance(new HealthCheck()
         {


            /*******************************************************************
             ** Broker failures are sanitized before leaving this boundary.
             *******************************************************************/
            @Override
            public HealthCheckOutcome run(HealthCheckInput input)
            {
               try
               {
                  return (probe.run(input));
               }
               catch(Exception e)
               {
                  return (HealthCheckOutcome.down(HealthRedactor.redactThrowable(e)));
               }
            }
         }));
   }



   /*******************************************************************************
    ** Count each broker queue once, including shared trigger dead-letter queues
    ** and explicitly configured DLQ/expiry destinations. No messages are read.
    *******************************************************************************/
   private static HealthCheckOutcome deadLetters(HealthCheckInput input) throws Exception
   {
      EsbInstanceMetaData esb = EsbInstanceMetaData.of(input.getQInstance());
      boolean managed = false;
      long depth = 0;
      for(String provider : esb.getProviders().keySet())
      {
         var adapter = EsbBrokerAdapters.forProvider(provider);
         if(adapter.isEmpty())
         {
            continue;
         }
         managed = true;
         Set<String> queues = new HashSet<>();
         if(esb.getProvider(provider).getType() == com.kingsrook.qqq.esb.model.EsbProviderType.ACTIVEMQ_ARTEMIS)
         {
            queues.add("DLQ");
            queues.add("ExpiryQueue");
         }
         for(QEsbDestinationMetaData destination : esb.getDestinations().values())
         {
            String name = destination.getEffectiveDestinationName().toLowerCase(Locale.ROOT);
            if(provider.equals(destination.getProviderName()) && (name.contains("deadletter") || name.contains("dead_letter") || name.contains("dlq") || name.contains("expiry")))
            {
               queues.add(destination.getEffectiveDestinationName());
            }
         }
         for(QProcessMetaData process : input.getQInstance().getProcesses().values())
         {
            EsbProcessMetaData metadata = EsbProcessMetaData.of(process);
            if(metadata == null || metadata.getTriggers() == null)
            {
               continue;
            }
            for(EsbTrigger trigger : metadata.getTriggers())
            {
               QEsbDestinationMetaData destination = esb.getDestination(trigger.getDestinationName());
               if(destination != null && provider.equals(destination.getProviderName()))
               {
                  queues.add(trigger.getEffectiveDeadLetterDestinationName(process.getName(), destination));
               }
            }
         }
         for(String queue : queues)
         {
            if(!java.time.Instant.now().isBefore(input.getDeadline()))
            {
               return (HealthCheckOutcome.down("Broker management probe timed out"));
            }
            var info = adapter.get().getQueueInfo(queue);
            if(info.isPresent() && info.get().messageCount() != null)
            {
               depth += info.get().messageCount();
            }
         }
      }
      if(!managed)
      {
         return (HealthCheckOutcome.unknown("Broker management not enabled"));
      }
      return ((depth > 0 ? HealthCheckOutcome.degraded("Dead-letter or expiry messages present") : HealthCheckOutcome.up("No dead letters"))
         .withMetrics(List.of(new HealthMetric().withName("depth").withValue(depth).withUnit("messages"))));
   }



   /*******************************************************************************
    ** Internal probe body.
    *******************************************************************************/
   @FunctionalInterface
   private interface Probe
   {
      /***************************************************************************
       ** Execute within the runner's deadline.
       ***************************************************************************/
      HealthCheckOutcome run(HealthCheckInput input) throws Exception;
   }
}
