/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.sampleapp.dashboard.widgets;


import com.kingsrook.qqq.backend.core.actions.dashboard.RenderWidgetAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaDataInterface;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QuickSightChartMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.Test;


/*******************************************************************************
 ** Unit test for PersonsByCreateDateBarChart
 *******************************************************************************/
class RenderAllWidgetsTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      QInstance qInstance = SampleMetaDataProvider.defineTestInstance();
      QContext.init(qInstance, new QSession());

      ////////////////////////////////////////////////////////////////
      // make sure no widgets throw - and we get some code coverage //
      ////////////////////////////////////////////////////////////////
      for(QWidgetMetaDataInterface widget : qInstance.getWidgets().values())
      {
         if(widget instanceof QuickSightChartMetaData)
         {
            ///////////////////////////////////////////
            // credentials not in circleci, so, skip //
            ///////////////////////////////////////////
            continue;
         }

         RenderWidgetInput input = new RenderWidgetInput();
         input.setWidgetMetaData(widget);
         RenderWidgetOutput output = new RenderWidgetAction().execute(input);
      }
   }

}
