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

package com.kingsrook.qqq.middleware.javalin.specs.v1.utils;


import java.io.IOException;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kingsrook.qqq.backend.core.exceptions.QRuntimeException;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;


/*******************************************************************************
 ** Plain-text siblings at the widget response boundary; record values are opaque.
 *******************************************************************************/
public class WidgetPlainText
{
   /*******************************************************************************
    ** Preserve the serialized tree, including explicit null and empty values.
    *******************************************************************************/
   public static Object enrich(Object widget)
   {
      if(widget == null)
      {
         return (null);
      }
      try
      {
         JsonNode result = JsonUtils.toObject(JsonUtils.toJson(widget), JsonNode.class);
         enrichNode(result);
         return (result);
      }
      catch(IOException e)
      {
         throw (new QRuntimeException("Unable to add widget plain text", e));
      }
   }



   /*******************************************************************************
    ** Add display text only to widgets and nested blocks, never record values.
    *******************************************************************************/
   private static void enrichNode(JsonNode node)
   {
      if(!(node instanceof ObjectNode widget))
      {
         return;
      }
      for(String key : List.of("footerHTML", "html", "overlayHtml"))
      {
         JsonNode html = widget.get(key);
         if(html != null && html.isTextual())
         {
            widget.put(key.equals("html") ? "text" : key.equals("footerHTML") ? "footerText" : "overlayText", HtmlText.fromHtml(html.asText()));
         }
      }
      for(String key : List.of("blocks", "subBlocks"))
      {
         JsonNode blocks = widget.get(key);
         if(blocks != null && blocks.isArray())
         {
            blocks.forEach(WidgetPlainText::enrichNode);
         }
      }
   }
}
