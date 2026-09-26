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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components;


import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.BlockStylesInterface;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.base.BaseStyles;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.button.ButtonStyles;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.image.ImageStyles;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.text.TextStyles;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;


/*******************************************************************************
 **
 *******************************************************************************/
public sealed interface WidgetBlockStyles extends ToSchema permits
   WidgetBlockBaseStyles,
   WidgetBlockButtonStyles,
   WidgetBlockImageStyles,
   WidgetBlockTextStyles
{
   @OpenAPIExclude
   QLogger LOG = QLogger.getLogger(WidgetBlockStyles.class);


   /***************************************************************************
    **
    ***************************************************************************/
   static WidgetBlockStyles of(BlockStylesInterface blockStyles)
   {
      if(blockStyles == null)
      {
         return (null);
      }

      if(blockStyles instanceof ButtonStyles s)
      {
         return (new WidgetBlockButtonStyles(s));
      }
      else if(blockStyles instanceof ImageStyles s)
      {
         return (new WidgetBlockImageStyles(s));
      }
      else if(blockStyles instanceof TextStyles s)
      {
         return (new WidgetBlockTextStyles(s));
      }
      //////////////////////////////////////////////////////////////////////////////////////////////
      // note - important for this one to be last, since it's a base class to some of the above!! //
      //////////////////////////////////////////////////////////////////////////////////////////////
      else if(blockStyles instanceof BaseStyles s)
      {
         return (new WidgetBlockBaseStyles(s));
      }

      LOG.warn("Unrecognized block value type: " + blockStyles.getClass().getName());
      return (null);
   }

}
