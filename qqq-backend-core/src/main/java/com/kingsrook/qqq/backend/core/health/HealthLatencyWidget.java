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


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.LineChartData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** A standard small line chart on each check record, using recorded latencies.
 *******************************************************************************/
public class HealthLatencyWidget extends AbstractWidgetRenderer
{
   public static final String NAME = "healthCheckLatency";



   /*******************************************************************************
    ** A missing record id returns an honest empty chart, never sample data.
    *******************************************************************************/
   @Override
   public RenderWidgetOutput render(RenderWidgetInput input) throws QException
   {
      List<QRecord> rows = new ArrayList<>();
      String id = input.getQueryParams().get("id");
      if(id != null)
      {
         rows.addAll(HealthStore.query(HealthMetaDataProvider.RESULT_TABLE, new QQueryFilter()
            .withCriteria(new QFilterCriteria("healthCheckId", QCriteriaOperator.EQUALS, id))
            .withCriteria(new QFilterCriteria("status", QCriteriaOperator.NOT_EQUALS, "RUNNING"))
            .withOrderBy(new QFilterOrderBy("startedAt", false)).withLimit(500)));
         Collections.reverse(rows);
      }
      LineChartData data = new SmallChart().withTitle(rows.isEmpty() ? "No checks have run yet" : "Latency (ms)")
         .withChartData(new LineChartData.Data().withLabels(rows.stream().map(row -> row.getValueString("startedAt")).toList())
            .withLineLabels(List.of("Latency (ms)")).withDatasets(List.of(new LineChartData.Data.Dataset().withLabel("Latency (ms)")
               .withData(rows.stream().map(row -> (Number) row.getValueLong("latencyMs")).toList()))));
      return (new RenderWidgetOutput(data));
   }



   /*******************************************************************************
    ** The small chart uses the same data structure as the generic line chart.
    *******************************************************************************/
   public static class SmallChart extends LineChartData
   {
      @Override
      public String getType()
      {
         return (WidgetType.SMALL_LINE_CHART.getType());
      }
   }
}
