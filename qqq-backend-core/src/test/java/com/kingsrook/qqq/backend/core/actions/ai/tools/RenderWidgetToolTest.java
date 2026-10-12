/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.ai.tools;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.ai.AiToolKernelSettings;
import com.kingsrook.qqq.backend.core.actions.ai.AiToolResult;
import com.kingsrook.qqq.backend.core.actions.ai.exposure.AiExposurePolicy;
import com.kingsrook.qqq.backend.core.actions.ai.output.AiRedactor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit tests for RenderWidgetTool's widget-data sanitizing and the widget
 ** allow-list settings.
 *******************************************************************************/
class RenderWidgetToolTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   private static AiToolContext context()
   {
      AiToolKernelSettings settings = new AiToolKernelSettings();
      return (new AiToolContext(null, settings, new AiExposurePolicy(settings), new AiRedactor(null), new AiToolResult()));
   }



   /*******************************************************************************
    ** The whole tree is capped at MAX_TREE_NODES values - map entries count.
    *******************************************************************************/
   @Test
   @SuppressWarnings("unchecked")
   void testTreeIsCapped()
   {
      Map<String, Object> wide = new LinkedHashMap<>();
      for(int i = 0; i < 5000; i++)
      {
         wide.put("k" + i, "v" + i);
      }

      Map<String, Object> result = (Map<String, Object>) RenderWidgetTool.sanitizeTree(context(), wide, 0, new int[] {RenderWidgetTool.MAX_TREE_NODES});
      assertEquals(RenderWidgetTool.MAX_TREE_NODES, result.size());

      ////////////////////////////////////////////////////////
      // many small maps inside lists share the same budget //
      ////////////////////////////////////////////////////////
      List<Object> outer = new ArrayList<>();
      for(int i = 0; i < 40; i++)
      {
         Map<String, Object> inner = new LinkedHashMap<>();
         for(int j = 0; j < 100; j++)
         {
            inner.put("k" + j, j);
         }
         outer.add(inner);
      }
      int[]        budget  = new int[] {1000};
      List<Object> capped  = (List<Object>) RenderWidgetTool.sanitizeTree(context(), outer, 0, budget);
      int          entries = 0;
      for(Object element : capped)
      {
         entries += ((Map<String, Object>) element).size() + 1;
      }
      assertEquals(1000, entries);
      assertEquals(0, budget[0]);

      ///////////////////////////////////
      // depth beyond MAX_DEPTH is cut //
      ///////////////////////////////////
      Object deep = "leaf";
      for(int i = 0; i < 20; i++)
      {
         deep = Map.of("x", deep);
      }
      Object node = RenderWidgetTool.sanitizeTree(context(), deep, 0, new int[] {RenderWidgetTool.MAX_TREE_NODES});
      for(int i = 0; i <= RenderWidgetTool.MAX_DEPTH; i++)
      {
         node = ((Map<String, Object>) node).get("x");
      }
      assertNull(node);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWidgetAllowList()
   {
      AiToolKernelSettings settings = new AiToolKernelSettings();
      assertFalse(settings.isWidgetAllowed("w"));
      assertFalse(settings.isWidgetAllowed(null));

      settings.withAllowedWidgetName("w");
      assertTrue(settings.isWidgetAllowed("w"));

      settings.withHiddenWidgetName("w");
      assertFalse(settings.isWidgetAllowed("w"));

      settings.setAllowedWidgetNames(null);
      settings.setHiddenWidgetNames(null);
      assertFalse(settings.isWidgetAllowed("w"));
      settings.withAllowedWidgetName("w").withHiddenWidgetName("other");
      assertTrue(settings.isWidgetAllowed("w"));
   }
}
