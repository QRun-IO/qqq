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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.MultiStatisticsData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.QWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;


/*******************************************************************************
 ** Generic dashboard widgets backed exclusively by persisted health records.
 *******************************************************************************/
public class HealthWidgets extends AbstractWidgetRenderer
{
   private Clock clock = Clock.systemUTC();



   /*******************************************************************************
    ** Allow deterministic time windows in tests and embedded applications.
    *******************************************************************************/
   public HealthWidgets withClock(Clock clock)
   {
      this.clock = clock;
      return (this);
   }



   /*******************************************************************************
    ** Register standard MULTI_STATISTICS and TABLE widget types, never HTML.
    *******************************************************************************/
   public static void define(QInstance qInstance)
   {
      for(String name : List.of("healthSummary", "healthComponents", "healthProblems", "healthTransitions"))
      {
         qInstance.addWidget(new QWidgetMetaData().withName(name).withLabel(switch(name)
         {
            case "healthSummary" -> "System Health";
            case "healthComponents" -> "Components";
            case "healthProblems" -> "Problems";
            default -> "Recent transitions";
         }).withType((name.equals("healthSummary") ? WidgetType.MULTI_STATISTICS : WidgetType.TABLE).getType())
            .withGridColumns(12).withCodeReference(new QCodeReference(HealthWidgets.class)).withPermissionRules(HealthMetaDataProvider.readPermissions()));
      }
   }



   /*******************************************************************************
    ** Use the ordinary widget action's session and table read permissions.
    *******************************************************************************/
   @Override
   public RenderWidgetOutput render(RenderWidgetInput input) throws QException
   {
      String name = input.getWidgetMetaData().getName();
      QWidgetData data;
      if("healthSummary".equals(name))
      {
         data = summary();
      }
      else
      {
         String table;
         List<String> columns;
         QQueryFilter filter = new QQueryFilter();
         switch(name)
         {
            case "healthComponents" ->
            {
               table = HealthMetaDataProvider.COMPONENT_TABLE;
               columns = List.of("label", "status", "since", "checksDown");
               filter.withOrderBy(new QFilterOrderBy("name", true));
            }
            case "healthProblems" ->
            {
               table = HealthMetaDataProvider.CHECK_TABLE;
               columns = List.of("label", "currentStatus", "currentMessage", "currentSince");
               filter.withCriteria(new QFilterCriteria("currentStatus", QCriteriaOperator.NOT_EQUALS, "UP"))
                  .withCriteria(new QFilterCriteria("registered", QCriteriaOperator.EQUALS, true));
            }
            default ->
            {
               table = HealthMetaDataProvider.RESULT_TABLE;
               columns = List.of("healthCheckId", "status", "message", "finishedAt");
               filter.withCriteria(new QFilterCriteria("isTransition", QCriteriaOperator.EQUALS, true))
                  .withOrderBy(new QFilterOrderBy("startedAt", false)).withLimit(20);
            }
         }
         List<QRecord> records = new ArrayList<>(HealthStore.query(table, filter));
         if("healthProblems".equals(name))
         {
            records.sort(Comparator.comparingInt(record -> rank(record.getValueString("currentStatus"))));
         }
         List<Map<String, Object>> rows = new ArrayList<>();
         for(QRecord record : records)
         {
            Map<String, Object> row = new LinkedHashMap<>();
            for(String column : columns)
            {
               row.put(column, record.getValue(column));
            }
            rows.add(row);
         }
         data = new TableData(input.getWidgetMetaData().getLabel(), columns.stream()
            .map(column -> new TableData.Column("text", QContext.getQInstance().getTable(table).getField(column).getLabel(), column, null, "left")).toList(), rows)
            .withNoRowsFoundHTML("No checks have run yet").withRowsPerPage(20);
      }
      return (new RenderWidgetOutput(data));
   }



   /*******************************************************************************
    ** Overall severity excludes INFO checks, per the application severity cap.
    ** Pod count represents distinct pods observed running checks in two minutes.
    *******************************************************************************/
   private MultiStatisticsData summary() throws QException
   {
      List<QRecord> components = HealthStore.query(HealthMetaDataProvider.COMPONENT_TABLE, new QQueryFilter());
      List<QRecord> checks = HealthStore.query(HealthMetaDataProvider.CHECK_TABLE, new QQueryFilter()
         .withCriteria(new QFilterCriteria("registered", QCriteriaOperator.EQUALS, true)));
      List<QRecord> results = HealthStore.query(HealthMetaDataProvider.RESULT_TABLE, new QQueryFilter()
         .withCriteria(new QFilterCriteria("startedAt", QCriteriaOperator.GREATER_THAN_OR_EQUALS, clock.instant().minusSeconds(3600))));
      HealthStatus overall = HealthStatus.aggregate(checks.stream().filter(check -> !"INFO".equals(check.getValueString("severity")))
         .map(check -> HealthTransitionEvaluator.status(check.getValueString("currentStatus"))).toArray(HealthStatus[]::new));
      int down = (int) components.stream().filter(row -> "DOWN".equals(row.getValueString("status"))).count();
      int degraded = (int) components.stream().filter(row -> "DEGRADED".equals(row.getValueString("status"))).count();
      int pods = (int) results.stream().filter(row -> row.getValueInstant("startedAt").isAfter(clock.instant().minusSeconds(120)))
         .map(row -> row.getValueString("runnerPod")).distinct().count();
      boolean empty = checks.stream().noneMatch(check -> check.getValueInstant("lastRunAt") != null);
      return (new MultiStatisticsData().withTitle(empty ? "No checks have run yet" : "System Health: " + overall)
         .withStatisticsGroupData(List.of(new MultiStatisticsData.StatisticsGroupData().withHeader(overall.name())
            .withStatisticList(List.of(new MultiStatisticsData.StatisticsGroupData.Statistic("Components down", down, null),
               new MultiStatisticsData.StatisticsGroupData.Statistic("Components degraded", degraded, null),
               new MultiStatisticsData.StatisticsGroupData.Statistic("Checks run last hour", results.size(), null),
               new MultiStatisticsData.StatisticsGroupData.Statistic("Runner pods alive", pods, null))))));
   }



   /*******************************************************************************
    ** Most severe problems appear first, independent of alphabetical enum order.
    *******************************************************************************/
   private int rank(String status)
   {
      return switch(HealthTransitionEvaluator.status(status))
      {
         case DOWN -> 0;
         case DEGRADED -> 1;
         default -> 2;
      };
   }
}
