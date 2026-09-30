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


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.MultiStatisticsData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;


/*******************************************************************************
 ** Meta Data Producer for SampleMultiStatisticsWidget
 *******************************************************************************/
public class SampleMultiStatisticsWidgetMetaDataProducer extends MetaDataProducer<QWidgetMetaData>
{
   public static final String NAME = "SampleMultiStatisticsWidget";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QWidgetMetaData produce(QInstance qInstance) throws QException
   {
      return new QWidgetMetaData()
         .withName(NAME)
         .withType(WidgetType.MULTI_STATISTICS.getType())
         .withGridColumns(4)
         .withIsCard(true)
         .withLabel("Multi Statistics")
         .withTooltip("This is a sample of a multi-statistics widget")
         .withShowReloadButton(true)
         // .withIcon(MaterialDashboardIconRoleNames.TOP_RIGHT_INSIDE_CARD, new QIcon("local_shipping").withColor(WidgetConstants.COLOR_NEW_GREEN))
         .withCodeReference(new QCodeReference(SampleStatisticsWidgetRenderer.class));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class SampleStatisticsWidgetRenderer extends AbstractWidgetRenderer
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public RenderWidgetOutput render(RenderWidgetInput input) throws QException
      {
         MultiStatisticsData.StatisticsGroupData thisWeek = new MultiStatisticsData.StatisticsGroupData()
            .withIcon("check")
            .withIconColor("green")
            .withHeader("This Week")
            .withSubheader("(1/1/24 - 1/7/24")
            .withStatisticList(List.of(
               new MultiStatisticsData.StatisticsGroupData.Statistic("Red", 15, null),
               new MultiStatisticsData.StatisticsGroupData.Statistic("Green", 20, null),
               new MultiStatisticsData.StatisticsGroupData.Statistic("Blue", 25, null)
            ));

         MultiStatisticsData.StatisticsGroupData lastWeek = new MultiStatisticsData.StatisticsGroupData()
            .withIcon("pending")
            .withIconColor("red")
            .withHeader("Last Week")
            .withSubheader("(12/25/23 - 12/31/23")
            .withStatisticList(List.of(
               new MultiStatisticsData.StatisticsGroupData.Statistic("Red", 10, null),
               new MultiStatisticsData.StatisticsGroupData.Statistic("Green", 25, null),
               new MultiStatisticsData.StatisticsGroupData.Statistic("Blue", 17, null)
            ));

         MultiStatisticsData multiStatisticsData = new MultiStatisticsData()
            .withTitle("Sample Multi Statsitics")
            .withStatisticsGroupData(
               List.of(
                  thisWeek,
                  lastWeek
               )
            );
         return (new RenderWidgetOutput(multiStatisticsData));
      }
   }

}
