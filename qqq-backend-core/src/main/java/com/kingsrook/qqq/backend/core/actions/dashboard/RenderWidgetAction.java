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

package com.kingsrook.qqq.backend.core.actions.dashboard;


import java.io.Serializable;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Class for loading widget implementation code and rendering of widgets
 **
 *******************************************************************************/
public class RenderWidgetAction
{

   /*******************************************************************************
    **
    *******************************************************************************/
   public RenderWidgetOutput execute(RenderWidgetInput input) throws QException
   {
      ActionHelper.validateSession(input);

      AbstractWidgetRenderer widgetRenderer = QCodeLoader.getAdHoc(AbstractWidgetRenderer.class, input.getWidgetMetaData().getCodeReference());

      ///////////////////////////////////////////////////////////////
      // move default values from meta data into this render input //
      ///////////////////////////////////////////////////////////////
      for(Map.Entry<String, Serializable> entry : input.getWidgetMetaData().getDefaultValues().entrySet())
      {
         input.addQueryParam(entry.getKey(), ValueUtils.getValueAsString(entry.getValue()));
      }

      return (widgetRenderer.render(input));
   }
}
