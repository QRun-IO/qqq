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


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.RawHTML;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.nocode.HtmlWrapper;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.nocode.QNoCodeWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.nocode.WidgetCalculation;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.nocode.WidgetCount;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.nocode.WidgetHtmlLine;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for NoCodeWidgetRenderer
 *******************************************************************************/
class NoCodeWidgetRendererTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      TestUtils.insertDefaultShapes(QContext.getQInstance());

      QNoCodeWidgetMetaData metaData = new QNoCodeWidgetMetaData();
      metaData.withValue(new WidgetCount()
         .withName("shapeCount")
         .withTableName(TestUtils.TABLE_NAME_SHAPE));

      metaData.withValue(new WidgetCalculation()
         .withName("shapeCountPlusShapeCount")
         .withOperator(WidgetCalculation.Operator.SUM_INTEGERS)
         .withValues(List.of("shapeCount", "shapeCount")));

      metaData.withOutput(new WidgetHtmlLine()
         .withWrapper(HtmlWrapper.SUBHEADER)
         .withVelocityTemplate("Header"));

      metaData.withOutput(new WidgetHtmlLine()
         .withCondition(new QFilterCriteria("shapeCount", QCriteriaOperator.GREATER_THAN_OR_EQUALS, 0))
         .withWrapper(HtmlWrapper.INDENT_1)
         .withVelocityTemplate("""
               ${utils.checkIcon()} Yes: ${shapeCount} ${utils.plural($shapeCount, "shape", "shapes")}
            """));

      metaData.withOutput(new WidgetHtmlLine()
         .withCondition(new QFilterCriteria("shapeCount", QCriteriaOperator.EQUALS, 0))
         .withVelocityTemplate("No: ${shapeCount}"));

      metaData.withOutput(new WidgetHtmlLine()
         .withVelocityTemplate("Double: ${shapeCountPlusShapeCount}"));

      RenderWidgetInput input = new RenderWidgetInput();
      input.setWidgetMetaData(metaData);
      RenderWidgetOutput output = new NoCodeWidgetRenderer().render(input);

      String html = ((RawHTML) output.getWidgetData()).getHtml();
      System.out.println(html);

      assertTrue(html.matches("(?s).*<h4>.*Header.*</h4>.*"));
      assertTrue(html.matches("(?s).*1rem.*Yes: 3 shapes.*"));
      assertTrue(html.matches("(?s).*>check<.*"));
      assertFalse(html.matches("(?s).*No: 3.*"));
      assertTrue(html.matches("(?s).*Double: 6.*"));
   }

}