/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.middleware.javalin.executors;


import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.dashboard.RenderWidgetAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
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
      renderWidgetInput.setWidgetMetaData(widgetMetaData);

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
