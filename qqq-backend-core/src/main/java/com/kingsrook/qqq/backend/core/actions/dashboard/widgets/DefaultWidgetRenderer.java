/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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


import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.QWidgetData;


/*******************************************************************************
 **
 *******************************************************************************/
public class DefaultWidgetRenderer extends AbstractWidgetRenderer
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public RenderWidgetOutput render(RenderWidgetInput input) throws QException
   {
      return new RenderWidgetOutput(new DefaultWidgetData(input));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class DefaultWidgetData extends QWidgetData
   {
      private final String              type;
      private final Map<String, String> queryParams;



      /*******************************************************************************
       ** Constructor
       **
       *******************************************************************************/
      public DefaultWidgetData(RenderWidgetInput renderWidgetInput)
      {
         this.type = renderWidgetInput.getWidgetMetaData().getType();
         this.queryParams = renderWidgetInput.getQueryParams();
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public String getType()
      {
         return (type);
      }



      /*******************************************************************************
       ** Getter for queryParams
       **
       *******************************************************************************/
      public Map<String, String> getQueryParams()
      {
         return queryParams;
      }
   }

}
