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
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.dashboard.RenderWidgetAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.ChildRecordListData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.ParentWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.ParentWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaDataInterface;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.WidgetDropdownData;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit test for ChildRecordListRenderer
 *******************************************************************************/
class ParentWidgetRendererTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   @AfterEach
   void beforeAndAfterEach()
   {
      MemoryRecordStore.getInstance().reset();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testParentWidget() throws QException
   {
      QInstance qInstance = QContext.getQInstance();

      QWidgetMetaDataInterface parentWidget = new ParentWidgetMetaData()
         .withTitle("Parent")
         .withChildWidgetNameList(
            List.of(
               ProcessWidgetRenderer.class.getSimpleName()
            )
         )
         .withDropdowns(
            List.of(
               new WidgetDropdownData()
                  .withPossibleValueSourceName(TestUtils.defineStatesPossibleValueSource().getName()),
               new WidgetDropdownData()
                  .withPossibleValueSourceName(TestUtils.defineShapePossibleValueSource().getName())
            )
         )
         .withType(WidgetType.PARENT_WIDGET.getType())
         .withName(ProcessWidgetRenderer.class.getSimpleName())
         .withGridColumns(12)
         .withLabel("Test Parent Widget")
         .withCodeReference(new QCodeReference(ParentWidgetRenderer.class))
         .withIcon("local_shipping");
      qInstance.addWidget(parentWidget);

      RenderWidgetInput input = new RenderWidgetInput();
      input.setWidgetMetaData(parentWidget);

      RenderWidgetAction renderWidgetAction = new RenderWidgetAction();
      RenderWidgetOutput output             = renderWidgetAction.execute(input);

      assertThat(output.getWidgetData()).isNotNull();
      ParentWidgetData parentWidgetData = (ParentWidgetData) output.getWidgetData();
      assertThat(parentWidgetData.getDropdownDataList().size()).isEqualTo(2);
      assertThat(parentWidgetData.getChildWidgetNameList().size()).isEqualTo(1);
      assertThat(parentWidgetData.getType()).isEqualTo(WidgetType.PARENT_WIDGET.getType());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNoChildRecordsFound() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      QWidgetMetaData widget = ChildRecordListRenderer.widgetMetaDataBuilder(qInstance.getJoin("orderLineItem"))
         .withLabel("Line Items")
         .getWidgetMetaData();
      qInstance.addWidget(widget);
      reInitInstanceInContext(qInstance);

      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);

      TestUtils.insertRecords(qInstance.getTable(TestUtils.TABLE_NAME_ORDER), List.of(
         new QRecord().withValue("id", 1)
      ));

      RenderWidgetInput input = new RenderWidgetInput();
      input.setWidgetMetaData(widget);
      input.setQueryParams(new HashMap<>(Map.of("id", "1")));

      RenderWidgetAction renderWidgetAction = new RenderWidgetAction();
      RenderWidgetOutput output             = renderWidgetAction.execute(input);

      ChildRecordListData childRecordListData = (ChildRecordListData) output.getWidgetData();
      assertThat(childRecordListData.getChildTableMetaData()).hasFieldOrPropertyWithValue("name", TestUtils.TABLE_NAME_LINE_ITEM);
      assertThat(childRecordListData.getQueryOutput().getRecords()).isEmpty();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testChildRecordsFound() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      QWidgetMetaData widget = ChildRecordListRenderer.widgetMetaDataBuilder(qInstance.getJoin("orderLineItem"))
         .withLabel("Line Items")
         .getWidgetMetaData();
      qInstance.addWidget(widget);
      reInitInstanceInContext(qInstance);

      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);

      TestUtils.insertRecords(qInstance.getTable(TestUtils.TABLE_NAME_ORDER), List.of(
         new QRecord().withValue("id", 1),
         new QRecord().withValue("id", 2)
      ));

      TestUtils.insertRecords(qInstance.getTable(TestUtils.TABLE_NAME_LINE_ITEM), List.of(
         new QRecord().withValue("orderId", 1).withValue("sku", "ABC").withValue("lineNumber", 2),
         new QRecord().withValue("orderId", 1).withValue("sku", "BCD").withValue("lineNumber", 1),
         new QRecord().withValue("orderId", 2).withValue("sku", "XYZ") // should not be found.
      ));

      RenderWidgetInput input = new RenderWidgetInput();
      input.setWidgetMetaData(widget);
      input.setQueryParams(new HashMap<>(Map.of("id", "1")));

      RenderWidgetAction renderWidgetAction = new RenderWidgetAction();
      RenderWidgetOutput output             = renderWidgetAction.execute(input);

      ChildRecordListData childRecordListData = (ChildRecordListData) output.getWidgetData();
      assertThat(childRecordListData.getChildTableMetaData()).hasFieldOrPropertyWithValue("name", TestUtils.TABLE_NAME_LINE_ITEM);
      assertThat(childRecordListData.getQueryOutput().getRecords()).hasSize(2);
      assertThat(childRecordListData.getQueryOutput().getRecords().get(0).getValueString("sku")).isEqualTo("BCD");
      assertThat(childRecordListData.getQueryOutput().getRecords().get(1).getValueString("sku")).isEqualTo("ABC");
   }

}
