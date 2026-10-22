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


import java.time.Duration;
import java.time.Instant;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.alerts.AlertSeverity;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.health.HealthCategory;
import com.kingsrook.qqq.backend.core.health.HealthCheck;
import com.kingsrook.qqq.backend.core.health.HealthCheckInput;
import com.kingsrook.qqq.backend.core.health.HealthCheckKind;
import com.kingsrook.qqq.backend.core.health.HealthCheckMetaData;
import com.kingsrook.qqq.backend.core.health.HealthCheckOutcome;
import com.kingsrook.qqq.backend.core.health.HealthChecksMetaData;
import com.kingsrook.qqq.backend.core.health.HealthMetric;
import com.kingsrook.qqq.backend.core.health.HealthRedactor;
import com.kingsrook.qqq.backend.core.jobs.SystemScope;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.esb.model.EsbOutboxConfig;
import com.kingsrook.qqq.esb.outbox.EsbOutbox;
import com.kingsrook.qqq.esb.outbox.EsbOutboxTable;


/*******************************************************************************
 * The ESB outbox's health check, core.esb.outbox (registered when the outbox
 * is enabled): DOWN when any row is DEAD (an event that will not be sent
 * without someone's help), DEGRADED when the oldest PENDING row is older than
 * pendingAlertMs (the relay is stuck, or the broker has been unreachable that
 * long), else UP - with dead, pending, and oldest-pending-age metrics.
 *******************************************************************************/
public final class EsbOutboxHealthChecks
{
   public static final String CHECK_NAME = "core.esb.outbox";



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private EsbOutboxHealthChecks()
   {
   }



   /*******************************************************************************
    ** Register the check (once, however many times the instance is enriched).
    *******************************************************************************/
   public static void register(QInstance instance)
   {
      HealthChecksMetaData registry = HealthChecksMetaData.ofOrWithNew(instance);
      if(registry.getChecks().stream().anyMatch(check -> CHECK_NAME.equals(check.getName())))
      {
         return;
      }

      registry.withCheck(new HealthCheckMetaData().withName(CHECK_NAME).withLabel(CHECK_NAME).withComponent("esb")
         .withCategory(HealthCategory.MESSAGING).withKind(HealthCheckKind.DEPENDENCY)
         .withIntervalSeconds(60).withTimeoutMs(5000).withSeverity(AlertSeverity.ERROR).withCheckInstance(new HealthCheck()
         {
            /*******************************************************************
             ** Database failures are sanitized before leaving this boundary.
             *******************************************************************/
            @Override
            public HealthCheckOutcome run(HealthCheckInput input)
            {
               try
               {
                  return (SystemScope.call(() -> check(input.getQInstance())));
               }
               catch(Exception e)
               {
                  return (HealthCheckOutcome.down(HealthRedactor.redactThrowable(e)));
               }
            }
         }));
   }



   /*******************************************************************************
    ** The outcome for the instance's outbox (see the class comment).
    *******************************************************************************/
   public static HealthCheckOutcome check(QInstance instance) throws QException
   {
      EsbOutboxConfig config = EsbOutbox.getEnabledConfig(instance);
      if(config == null)
      {
         return (HealthCheckOutcome.unknown("ESB outbox not enabled"));
      }

      String  tableName = config.getTableName();
      Integer dead      = new CountAction().execute(new CountInput(tableName).withFilter(statusFilter(EsbOutboxTable.STATUS_DEAD))).getCount();
      Integer pending   = new CountAction().execute(new CountInput(tableName).withFilter(statusFilter(EsbOutboxTable.STATUS_PENDING))).getCount();

      long oldestPendingMs = 0;
      List<QRecord> oldest = new QueryAction().execute(new QueryInput(tableName)
         .withFilter(statusFilter(EsbOutboxTable.STATUS_PENDING).withOrderBy(new QFilterOrderBy(EsbOutboxTable.FIELD_ID)).withLimit(1))).getRecords();
      if(!oldest.isEmpty() && oldest.get(0).getValueInstant(EsbOutboxTable.FIELD_CREATE_DATE) != null)
      {
         oldestPendingMs = Math.max(0, Duration.between(oldest.get(0).getValueInstant(EsbOutboxTable.FIELD_CREATE_DATE), Instant.now()).toMillis());
      }

      HealthCheckOutcome outcome;
      if(dead != null && dead > 0)
      {
         outcome = HealthCheckOutcome.down(dead + " ESB outbox event(s) are DEAD");
      }
      else if(oldestPendingMs > config.getPendingAlertMs())
      {
         outcome = HealthCheckOutcome.degraded("The oldest pending ESB outbox event is " + (oldestPendingMs / 1000) + " s old");
      }
      else
      {
         outcome = HealthCheckOutcome.up("ESB outbox is draining");
      }

      return (outcome.withMetrics(List.of(
         new HealthMetric().withName("dead").withValue(dead == null ? 0 : dead).withUnit("events"),
         new HealthMetric().withName("pending").withValue(pending == null ? 0 : pending).withUnit("events"),
         new HealthMetric().withName("oldestPendingAge").withValue(oldestPendingMs).withUnit("ms"))));
   }



   /*******************************************************************************
    ** Rows with the given status.
    *******************************************************************************/
   private static QQueryFilter statusFilter(String status)
   {
      return (new QQueryFilter(new QFilterCriteria(EsbOutboxTable.FIELD_STATUS, QCriteriaOperator.EQUALS, status)));
   }

}
