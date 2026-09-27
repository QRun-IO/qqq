/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.executors;


import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.dashboard.RenderWidgetAction;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaDataInterface;
import com.kingsrook.qqq.middleware.javalin.executors.io.WidgetInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.WidgetOutputInterface;


/*******************************************************************************
 ** Executor for rendering a widget.
 *******************************************************************************/
public class WidgetExecutor extends AbstractMiddlewareExecutor<WidgetInput, WidgetOutputInterface>
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void execute(WidgetInput input, WidgetOutputInterface output) throws QException
   {
      String                    widgetName     = input.getWidgetName();
      QWidgetMetaDataInterface  widgetMetaData = QContext.getQInstance().getWidget(widgetName);

      if(widgetMetaData == null)
      {
         throw new QNotFoundException("Widget not found: " + widgetName);
      }

      RenderWidgetInput renderWidgetInput = new RenderWidgetInput();
      renderWidgetInput.setInputSource(QInputSource.USER);
      renderWidgetInput.setWidgetMetaData(widgetMetaData);

      ////////////////////////////////////////////////////////////////
      // the same permission check as the legacy widget data route //
      ////////////////////////////////////////////////////////////////
      PermissionsHelper.checkWidgetPermissionThrowing(renderWidgetInput, widgetName);

      Map<String, String> queryParams = input.getQueryParams();
      if(queryParams != null)
      {
         for(Map.Entry<String, String> entry : queryParams.entrySet())
         {
            renderWidgetInput.addQueryParam(entry.getKey(), entry.getValue());
         }
      }

      RenderWidgetOutput renderWidgetOutput = new RenderWidgetAction().execute(renderWidgetInput);
      output.setWidgetData(renderWidgetOutput.getWidgetData());
   }

}
