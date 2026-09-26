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
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.BlockValuesInterface;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.audio.AudioValues;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.button.ButtonValues;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.image.ImageValues;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.inputfield.InputFieldValues;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.text.TextValues;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;


/*******************************************************************************
 **
 *******************************************************************************/
public sealed interface WidgetBlockValues extends ToSchema permits
   WidgetBlockAudioValues,
   WidgetBlockButtonValues,
   WidgetBlockImageValues,
   WidgetBlockInputFieldValues,
   WidgetBlockTextValues
{
   @OpenAPIExclude
   QLogger LOG = QLogger.getLogger(WidgetBlockValues.class);


   /***************************************************************************
    **
    ***************************************************************************/
   static WidgetBlockValues of(BlockValuesInterface blockValues)
   {
      if(blockValues == null)
      {
         return (null);
      }

      if(blockValues instanceof AudioValues v)
      {
         return (new WidgetBlockAudioValues(v));
      }
      else if(blockValues instanceof ButtonValues v)
      {
         return (new WidgetBlockButtonValues(v));
      }
      else if(blockValues instanceof ImageValues v)
      {
         return (new WidgetBlockImageValues(v));
      }
      else if(blockValues instanceof InputFieldValues v)
      {
         return (new WidgetBlockInputFieldValues(v));
      }
      else if(blockValues instanceof TextValues v)
      {
         return (new WidgetBlockTextValues(v));
      }

      LOG.warn("Unrecognized block value type: " + blockValues.getClass().getName());
      return (null);
   }

}
