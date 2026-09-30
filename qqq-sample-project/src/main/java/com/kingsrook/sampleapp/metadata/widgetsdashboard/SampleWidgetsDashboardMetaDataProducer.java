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
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;


/*******************************************************************************
 ** Meta Data Producer for SampleWidgetsDashboard
 *******************************************************************************/
public class SampleWidgetsDashboardMetaDataProducer extends MetaDataProducer<QAppMetaData>
{
   public static final String NAME = "SampleWidgetsDashboard";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QAppMetaData produce(QInstance qInstance) throws QException
   {
      // Divider
      // Parent
      // Process...

      // USMap ??
      // QuickSightChart ??
      // ChildRecordList
      // FieldValueList

      ////////////////////////////////////
      // in java enum, but not frontend //
      ////////////////////////////////////
      // HORIZONTAL_BAR_CHART("horizontalBarChart"),
      // LOCATION("location"),

      return (new QAppMetaData()
         .withName(NAME)
         .withIcon(new QIcon("widgets"))
         .withWidgets(List.of(
            SampleBigNumberBlocksWidgetMetaDataProducer.NAME,
            SampleMultiStatisticsWidgetMetaDataProducer.NAME,
            SamplePieChartWidgetMetaDataProducer.NAME,
            SampleStatisticsWidgetMetaDataProducer.NAME,
            SampleTableWidgetMetaDataProducer.NAME,
            SampleStackedBarChartWidgetMetaDataProducer.NAME,
            SampleStepperWidgetMetaDataProducer.NAME,
            SampleHTMLWidgetMetaDataProducer.NAME,
            SampleSmallLineChartWidgetMetaDataProducer.NAME,
            SampleLineChartWidgetMetaDataProducer.NAME,
            SampleBarChartWidgetMetaDataProducer.NAME
         )));

   }

}
