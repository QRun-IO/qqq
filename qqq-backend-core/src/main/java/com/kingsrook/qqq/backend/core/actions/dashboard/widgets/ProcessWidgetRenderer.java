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

package com.kingsrook.qqq.backend.core.actions.dashboard.widgets;


import java.util.HashMap;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.ProcessWidgetData;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;


/*******************************************************************************
 ** Generic widget for displaying a process as a widget
 *******************************************************************************/
public class ProcessWidgetRenderer extends AbstractWidgetRenderer
{
   public static final String WIDGET_PROCESS_NAME = "processName";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public RenderWidgetOutput render(RenderWidgetInput input) throws QException
   {
      ActionHelper.validateSession(input);

      try
      {
         ProcessWidgetData data = new ProcessWidgetData();
         if(input.getWidgetMetaData() instanceof QWidgetMetaData widgetMetaData)
         {
            setupDropdowns(input, widgetMetaData, data);

            String           processName     = (String) widgetMetaData.getDefaultValues().get(WIDGET_PROCESS_NAME);
            QProcessMetaData processMetaData = QContext.getQInstance().getProcess(processName);
            data.setProcessMetaData(processMetaData);

            data.setDefaultValues(new HashMap<>(input.getQueryParams()));
         }
         return (new RenderWidgetOutput(data));
      }
      catch(Exception e)
      {
         throw (new QException("Error rendering process widget", e));
      }
   }

}
