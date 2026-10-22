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

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.health.HealthMetaDataProvider;
import com.kingsrook.qqq.backend.core.health.HealthRunner;
import com.kingsrook.qqq.backend.core.health.HealthStatus;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.middleware.javalin.specs.extensions.AbstractJsonEndpointSpec;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;


/*******************************************************************************
 ** Shared secured table projections. No elevated session is used for reads.
 *******************************************************************************/
public abstract class HealthSpecSupport extends AbstractJsonEndpointSpec
{
   /*******************************************************************************
    ** Require the same permissions as the health record screens.
    *******************************************************************************/
   protected void requirePermission(boolean write) throws QException
   {
      PermissionsHelper.checkTablePermissionThrowing(new QueryInput().withTableName(HealthMetaDataProvider.CHECK_TABLE),
         write ? TablePermissionSubType.EDIT : TablePermissionSubType.READ);
   }



   /*******************************************************************************
    ** All schemas are owned by this extension.
    *******************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return (HealthSchemasV1.all());
   }



   /*******************************************************************************
    ** Shared path parameter.
    *******************************************************************************/
   protected Parameter nameParameter()
   {
      return (new Parameter().withName("name").withIn(In.PATH).withRequired(true).withDescription("Stable health check or component name.")
         .withSchema(new Schema().withType(Type.STRING)));
   }



   /*******************************************************************************
    ** Read through standard actions under the caller's session.
    *******************************************************************************/
   protected List<QRecord> rows(String table, QQueryFilter filter) throws QException
   {
      return (new QueryAction().execute(new QueryInput().withTableName(table).withFilter(filter)).getRecords());
   }



   /*******************************************************************************
    ** Missing names are 404, including on-demand requests.
    *******************************************************************************/
   protected QRecord named(String table, String name) throws QException
   {
      List<QRecord> records = rows(table, new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, name)).withLimit(1));
      if(records.isEmpty())
      {
         throw new QNotFoundException("Unknown health name");
      }
      return (records.getFirst());
   }



   /*******************************************************************************
    ** Project only contract fields; table values have already been redacted.
    *******************************************************************************/
   protected Map<String, Object> project(QRecord record, String... fields)
   {
      Map<String, Object> result = new LinkedHashMap<>();
      for(String field : fields)
      {
         result.put(field, record.getValue(field));
      }
      return (result);
   }



   /*******************************************************************************
    ** Component state shared by summary and detail.
    *******************************************************************************/
   protected Map<String, Object> component(QRecord record)
   {
      return (project(record, "name", "label", "status", "since", "checksUp", "checksDegraded", "checksDown", "checksUnknown", "worstCheckId", "updatedAt"));
   }



   /*******************************************************************************
    ** Aggregate follows core severity semantics; pod presence is inferred from
    ** recent results, as in the System Health home.
    *******************************************************************************/
   protected Object summary() throws QException
   {
      List<QRecord> checks = rows(HealthMetaDataProvider.CHECK_TABLE, new QQueryFilter(new QFilterCriteria("registered", QCriteriaOperator.EQUALS, true)));
      List<QRecord> components = rows(HealthMetaDataProvider.COMPONENT_TABLE, new QQueryFilter());
      Map<String, Long> counts = new LinkedHashMap<>();
      for(HealthStatus status : HealthStatus.values())
      {
         counts.put(status.name().toLowerCase(Locale.ROOT), checks.stream().filter(row -> status.name().equals(row.getValueString("currentStatus"))).count());
      }
      List<QRecord> recent = rows(HealthMetaDataProvider.RESULT_TABLE, new QQueryFilter(new QFilterCriteria("startedAt", QCriteriaOperator.GREATER_THAN_OR_EQUALS, Instant.now().minusSeconds(120))));
      Map<String, Object> runner = new LinkedHashMap<>();
      runner.put("lastBeatAt", recent.stream().map(row -> row.getValueInstant("startedAt")).max(Instant::compareTo).orElse(null));
      runner.put("pods", recent.stream().map(row -> row.getValueString("runnerPod")).distinct().sorted().toList());
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("status", HealthStatus.aggregate(checks.stream().filter(row -> !"INFO".equals(row.getValueString("severity")))
         .map(row -> row.getValueString("currentStatus") == null ? HealthStatus.UNKNOWN : HealthStatus.valueOf(row.getValueString("currentStatus"))).toArray(HealthStatus[]::new)));
      result.put("since", checks.stream().map(row -> row.getValueInstant("currentSince")).filter(Objects::nonNull).max(Instant::compareTo).orElse(null));
      result.put("counts", counts);
      result.put("components", components.stream().map(this::component).toList());
      result.put("runner", runner);
      return (result);
   }



   /*******************************************************************************
    ** History is always bounded, newest first, with inclusive ISO-8601 bounds.
    *******************************************************************************/
   protected Object history(JsonInput input) throws QException
   {
      QRecord check = named(HealthMetaDataProvider.CHECK_TABLE, input.getPathParam("name"));
      Integer requested = input.getQueryParamInteger("limit");
      if(requested != null && requested < 1)
      {
         throw new QBadRequestException("limit must be positive");
      }
      QQueryFilter filter = new QQueryFilter(new QFilterCriteria("healthCheckId", QCriteriaOperator.EQUALS, check.getValue("id")))
         .withOrderBy(new QFilterOrderBy("startedAt", false)).withOrderBy(new QFilterOrderBy("id", false)).withLimit(requested == null ? 100 : Math.min(500, requested));
      Instant from = parseTime(input.getQueryParam("from"));
      Instant to = parseTime(input.getQueryParam("to"));
      if(from != null && to != null && from.isAfter(to))
      {
         throw new QBadRequestException("from must not be after to");
      }
      if(from != null)
      {
         filter.withCriteria(new QFilterCriteria("startedAt", QCriteriaOperator.GREATER_THAN_OR_EQUALS, from));
      }
      if(to != null)
      {
         filter.withCriteria(new QFilterCriteria("startedAt", QCriteriaOperator.LESS_THAN_OR_EQUALS, to));
      }
      return (Map.of("results", rows(HealthMetaDataProvider.RESULT_TABLE, filter).stream().map(row -> project(row,
         "id", "status", "message", "detailsJson", "metricsJson", "latencyMs", "startedAt", "finishedAt", "runnerPod", "isTransition", "triggeredBy")).toList()));
   }



   /*******************************************************************************
    ** Invalid date bounds are a user input error.
    *******************************************************************************/
   private Instant parseTime(String value) throws QBadRequestException
   {
      try
      {
         return (value == null ? null : Instant.parse(value));
      }
      catch(DateTimeParseException e)
      {
         throw new QBadRequestException("from and to must be ISO-8601 instants");
      }
   }



   /*******************************************************************************
    ** Reuse the live runner, closing a transient one on every exit path.
    *******************************************************************************/
   protected Object runNow(String name) throws QException
   {
      requirePermission(true);
      QueryInput lookup = new QueryInput().withTableName(HealthMetaDataProvider.CHECK_TABLE)
         .withFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, name)).withLimit(1));
      lookup.withPermissionBypass("Health run already authorized with table write permission; lookup only establishes registration");
      List<QRecord> records = new QueryAction().execute(lookup).getRecords();
      if(records.isEmpty())
      {
         throw new QNotFoundException("Unknown health name");
      }
      QRecord check = records.getFirst();
      if(!Boolean.TRUE.equals(check.getValueBoolean("registered")))
      {
         throw new QNotFoundException("Health check is no longer registered");
      }
      HealthRunner runner = HealthRunner.getForInstance(QContext.getQInstance());
      if(runner != null)
      {
         return (runner.runNow(name));
      }
      try(HealthRunner temporary = new HealthRunner(QContext.getQInstance(), "on-demand-" + UUID.randomUUID(), Clock.systemUTC()))
      {
         return (temporary.runNow(name));
      }
   }
}
