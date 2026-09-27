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


import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.NoCodeWidgetRenderer;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.nocode.HtmlWrapper;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.nocode.QNoCodeWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.nocode.WidgetHtmlLine;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.frontend.materialdashboard.model.metadata.MaterialDashboardIconRoleNames;


/*******************************************************************************
 ** Meta Data Producer for SampleHTMLWidget
 *******************************************************************************/
public class SampleHTMLWidgetMetaDataProducer extends MetaDataProducer<QWidgetMetaData>
{
   public static final String NAME = "SampleHTMLWidget";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QWidgetMetaData produce(QInstance qInstance) throws QException
   {
      QNoCodeWidgetMetaData widgetMetaData = (QNoCodeWidgetMetaData) new QNoCodeWidgetMetaData()
         .withName(NAME)
         .withType(WidgetType.HTML.getType())
         .withGridColumns(4)
         .withIsCard(true)
         .withLabel("HTML")
         .withTooltip("This is a sample of an HTML widget")
         .withShowReloadButton(false)
         .withIcon(MaterialDashboardIconRoleNames.TOP_RIGHT_INSIDE_CARD, new QIcon("data_object").withColor("#D87E28"))
         .withCodeReference(new QCodeReference(NoCodeWidgetRenderer.class));

      widgetMetaData.withOutput(new WidgetHtmlLine()
         .withWrapper(HtmlWrapper.BIG_CENTERED)
         .withVelocityTemplate("Purely Custom"));

      widgetMetaData.withOutput(new WidgetHtmlLine()
         .withVelocityTemplate("<i>User</i> <b>Defined</b> <u>HTML</u>"));

      return widgetMetaData;
   }

}
