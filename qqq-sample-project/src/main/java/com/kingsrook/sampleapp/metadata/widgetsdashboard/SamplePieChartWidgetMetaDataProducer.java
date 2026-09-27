/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.sampleapp.metadata.widgetsdashboard;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.ChartData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.ChartSubheaderData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.frontend.materialdashboard.model.metadata.MaterialDashboardIconRoleNames;


/*******************************************************************************
 ** Meta Data Producer for SamplePieChart
 *******************************************************************************/
public class SamplePieChartWidgetMetaDataProducer extends MetaDataProducer<QWidgetMetaData>
{
   public static final String NAME = "SamplePieChartWidget";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QWidgetMetaData produce(QInstance qInstance) throws QException
   {
      return new QWidgetMetaData()
         .withName(NAME)
         .withType(WidgetType.PIE_CHART.getType())
         .withGridColumns(4)
         .withIsCard(true)
         .withLabel("Pie Chart")
         .withTooltip("This is a sample of a pie chart")
         .withShowReloadButton(true)
         .withIcon(MaterialDashboardIconRoleNames.TOP_RIGHT_INSIDE_CARD, new QIcon("add_alert").withColor("#10B8A6"))
         .withCodeReference(new QCodeReference(SamplePieChartRenderer.class));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class SamplePieChartRenderer extends AbstractWidgetRenderer
   {
      private List<String> labels = new ArrayList<>();
      private List<String> colors = new ArrayList<>();
      private List<Number> data   = new ArrayList<>();
      private List<String> urls   = new ArrayList<>();



      /*******************************************************************************
       **
       *******************************************************************************/
      private void addSlice(String label, String color, Number datum, String url)
      {
         labels.add(label);
         colors.add(color);
         data.add(datum);
         urls.add(url);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public RenderWidgetOutput render(RenderWidgetInput input) throws QException
      {
         addSlice("Apple", "#FF0000", 100, null);
         addSlice("Orange", "#FF8000", 150, null);
         addSlice("Banana", "#FFFF00", 75, null);
         addSlice("Lime", "#00FF00", 100, null);
         addSlice("Blueberry", "#0000FF", 200, null);

         ChartData chartData = new ChartData()
            .withChartData(new ChartData.Data()
               .withLabels(labels)
               .withDatasets(List.of(
                  new ChartData.Data.Dataset()
                     .withLabel("Pie")
                     .withData(data)
                     .withBackgroundColors(colors)
                     .withUrls(urls)
               ))
            );

         ChartSubheaderData chartSubheaderData = new ChartSubheaderData()
            .withMainNumber(1000)
            .withVsPreviousNumber(100);
         // .withMainNumberUrl(mainUrl)
         // .withPreviousNumberUrl(previousUrl);

         chartSubheaderData.calculatePercentsEtc(true);

         chartData.setChartSubheaderData(chartSubheaderData);

         return (new RenderWidgetOutput(chartData));
      }
   }

}
