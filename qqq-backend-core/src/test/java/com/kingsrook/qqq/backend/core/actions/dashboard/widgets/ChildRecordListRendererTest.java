/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.dashboard.widgets;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.dashboard.RenderWidgetAction;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.ExamplePersonalizer;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.ChildRecordListData;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for ChildRecordListRenderer
 *******************************************************************************/
class ChildRecordListRendererTest extends BaseTest
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
   void testParentRecordNotFound() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      QWidgetMetaData widget = ChildRecordListRenderer.widgetMetaDataBuilder(qInstance.getJoin("orderLineItem"))
         .withLabel("Line Items")
         .getWidgetMetaData();
      qInstance.addWidget(widget);

      RenderWidgetInput input = new RenderWidgetInput();
      input.setWidgetMetaData(widget);
      input.setQueryParams(new HashMap<>(Map.of("id", "1")));

      RenderWidgetAction renderWidgetAction = new RenderWidgetAction();
      assertThatThrownBy(() -> renderWidgetAction.execute(input))
         .isInstanceOf(QNotFoundException.class);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNoChildRecordsFound() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);
      QWidgetMetaData widget = ChildRecordListRenderer.widgetMetaDataBuilder(qInstance.getJoin("orderLineItem"))
         .withLabel("Line Items")
         .getWidgetMetaData();
      qInstance.addWidget(widget);

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
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);
      QWidgetMetaData widget = ChildRecordListRenderer.widgetMetaDataBuilder(qInstance.getJoin("orderLineItem"))
         .withLabel("Line Items")
         .getWidgetMetaData();
      qInstance.addWidget(widget);

      ChildRecordListData childRecordListData = insertOrdersAndRenderWidget(qInstance, widget);

      assertThat(childRecordListData.getChildTableMetaData()).hasFieldOrPropertyWithValue("name", TestUtils.TABLE_NAME_LINE_ITEM);
      assertThat(childRecordListData.getQueryOutput().getRecords()).hasSize(2);
      assertThat(childRecordListData.getQueryOutput().getRecords().get(0).getValueString("sku")).isEqualTo("BCD");
      assertThat(childRecordListData.getQueryOutput().getRecords().get(1).getValueString("sku")).isEqualTo("ABC");

      ////////////////////////////////////////////////////////////////////////////////////////////////////
      // order id, being the join field, should implicitly be omitted - and we asked to omit lineNumber //
      ////////////////////////////////////////////////////////////////////////////////////////////////////
      assertTrue(childRecordListData.getOmitFieldNames().contains("orderId"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOmitFields() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);
      QWidgetMetaData widget = ChildRecordListRenderer.widgetMetaDataBuilder(qInstance.getJoin("orderLineItem"))
         .withLabel("Line Items")
         .withOmitFieldNames(List.of("lineNumber"))
         .getWidgetMetaData();
      qInstance.addWidget(widget);

      ChildRecordListData childRecordListData = insertOrdersAndRenderWidget(qInstance, widget);

      assertThat(childRecordListData.getChildTableMetaData()).hasFieldOrPropertyWithValue("name", TestUtils.TABLE_NAME_LINE_ITEM);
      assertThat(childRecordListData.getQueryOutput().getRecords()).hasSize(2);

      ///////////////////////////////////////////////////////////////////////////
      // we still get the data - it just includes a list of omitFieldNames now //
      ///////////////////////////////////////////////////////////////////////////
      assertThat(childRecordListData.getQueryOutput().getRecords().get(0).getValue("orderId")).isNotNull();

      ////////////////////////////////////////////////////////////////////////////////////////////////////
      // order id, being the join field, should implicitly be omitted - and we asked to omit lineNumber //
      ////////////////////////////////////////////////////////////////////////////////////////////////////
      assertTrue(childRecordListData.getOmitFieldNames().contains("orderId"));
      assertTrue(childRecordListData.getOmitFieldNames().contains("lineNumber"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testKeepJoinField() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);
      QWidgetMetaData widget = ChildRecordListRenderer.widgetMetaDataBuilder(qInstance.getJoin("orderLineItem"))
         .withLabel("Line Items")
         .withKeepJoinField(true)
         .getWidgetMetaData();
      qInstance.addWidget(widget);

      ChildRecordListData childRecordListData = insertOrdersAndRenderWidget(qInstance, widget);

      ////////////////////////////////////////////////////////////////////////////////////////////
      // there should be no omitted fields, due to the config that said to keep the join field. //
      ////////////////////////////////////////////////////////////////////////////////////////////
      assertThat(childRecordListData.getOmitFieldNames()).isNullOrEmpty();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testKeepJoinFieldOmitOtherFields() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);
      QWidgetMetaData widget = ChildRecordListRenderer.widgetMetaDataBuilder(qInstance.getJoin("orderLineItem"))
         .withLabel("Line Items")
         .withKeepJoinField(true)
         .withOmitFieldNames(List.of("lineNumber"))
         .getWidgetMetaData();
      qInstance.addWidget(widget);

      ChildRecordListData childRecordListData = insertOrdersAndRenderWidget(qInstance, widget);

      //////////////////////////////////////////////////////
      // there should only be the specified omitted field //
      //////////////////////////////////////////////////////
      assertEquals(1, childRecordListData.getOmitFieldNames().size());
      assertTrue(childRecordListData.getOmitFieldNames().contains("lineNumber"));
      assertFalse(childRecordListData.getOmitFieldNames().contains("orderId"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWithExposedJoin() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);
      QWidgetMetaData widget = ChildRecordListRenderer.widgetMetaDataBuilder(qInstance.getJoin("orderLineItem"))
         .withLabel("Line Items")
         .withQueryJoins(List.of(new QueryJoin(TestUtils.TABLE_NAME_ORDER).withType(QueryJoin.Type.LEFT).withSelect(true)))
         .getWidgetMetaData();
      qInstance.addWidget(widget);

      ChildRecordListData childRecordListData = insertOrdersAndRenderWidget(qInstance, widget);

      assertThat(childRecordListData.getChildTableMetaData()).hasFieldOrPropertyWithValue("name", TestUtils.TABLE_NAME_LINE_ITEM);
      assertThat(childRecordListData.getQueryOutput().getRecords()).hasSize(2);
      assertThat(childRecordListData.getQueryOutput().getRecords().get(0).getValueString("sku")).isEqualTo("BCD");
      assertThat(childRecordListData.getQueryOutput().getRecords().get(1).getValueString("sku")).isEqualTo("ABC");
      assertThat(childRecordListData.getQueryOutput().getRecords().get(0).getValueString("order.orderNo")).isEqualTo("ORD001");
      assertThat(childRecordListData.getQueryOutput().getRecords().get(1).getValueString("order.orderNo")).isEqualTo("ORD001");

      assertEquals(List.of(TestUtils.TABLE_NAME_ORDER), childRecordListData.getIncludeExposedJoinTables());
      assertTrue(childRecordListData.getOmitFieldNames().contains("orderId"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOnlyIncludeFieldNames() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);
      QWidgetMetaData widget = ChildRecordListRenderer.widgetMetaDataBuilder(qInstance.getJoin("orderLineItem"))
         .withLabel("Line Items")
         .withOnlyIncludeFieldNames(List.of("sku", "quantity"))
         .getWidgetMetaData();
      qInstance.addWidget(widget);

      ChildRecordListData childRecordListData = insertOrdersAndRenderWidget(qInstance, widget);

      assertThat(childRecordListData.getChildTableMetaData()).hasFieldOrPropertyWithValue("name", TestUtils.TABLE_NAME_LINE_ITEM);
      assertThat(childRecordListData.getQueryOutput().getRecords()).hasSize(2);
      assertThat(childRecordListData.getQueryOutput().getRecords().get(0).getValueString("sku")).isEqualTo("BCD");
      assertThat(childRecordListData.getQueryOutput().getRecords().get(1).getValueString("sku")).isEqualTo("ABC");

      assertEquals(List.of("sku", "quantity"), childRecordListData.getOnlyIncludeFieldNames());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPersonalization() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);
      QWidgetMetaData widget = ChildRecordListRenderer.widgetMetaDataBuilder(qInstance.getJoin("orderLineItem"))
         .withLabel("Line Items")
         .getWidgetMetaData()
         .withDefaultValue("orderBy", new ArrayList<>(List.of(new QFilterOrderBy("id"))));
      qInstance.addWidget(widget);

      insertTwoOrdersAndThreeLines(qInstance);

      RenderWidgetInput input = new RenderWidgetInput();
      input.setWidgetMetaData(widget);
      input.setQueryParams(new HashMap<>(Map.of("id", "1")));

      RenderWidgetAction  renderWidgetAction  = new RenderWidgetAction();
      RenderWidgetOutput  output              = renderWidgetAction.execute(input);
      ChildRecordListData childRecordListData = (ChildRecordListData) output.getWidgetData();

      //////////////////////////////////////////////////////
      // by default make sure we get the lineNumber field //
      //////////////////////////////////////////////////////
      assertThat(childRecordListData.getQueryOutput().getRecords()).hasSize(2);
      assertThat(childRecordListData.getQueryOutput().getRecords()).allMatch(record -> record.getValue("lineNumber") != null);

      ////////////////////////////////////////////////////
      // now personalize the table to remove that field //
      ////////////////////////////////////////////////////
      String userId = "jdoe";
      ExamplePersonalizer.registerInQInstance();
      ExamplePersonalizer.addCustomizableTable(TestUtils.TABLE_NAME_LINE_ITEM);
      ExamplePersonalizer.addFieldToRemoveForUserId(TestUtils.TABLE_NAME_LINE_ITEM, "lineNumber", userId);
      QContext.getQSession().getUser().setIdReference(userId);

      //////////////////////////////////////
      // re-run and assert no lineNumbers //
      //////////////////////////////////////
      renderWidgetAction = new RenderWidgetAction();
      output = renderWidgetAction.execute(input);
      childRecordListData = (ChildRecordListData) output.getWidgetData();

      assertThat(childRecordListData.getQueryOutput().getRecords()).hasSize(2);
      assertThat(childRecordListData.getQueryOutput().getRecords()).allMatch(record -> !record.getValues().containsKey("lineNumber"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testJoinFieldNull() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      qInstance.addTable(new QTableMetaData()
         .withName("foo")
         .withBackendName(TestUtils.MEMORY_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("barId", QFieldType.INTEGER)));

      qInstance.addTable(new QTableMetaData()
         .withName("bar")
         .withBackendName(TestUtils.MEMORY_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("baz", QFieldType.STRING)));

      QJoinMetaData fooJoinBar = new QJoinMetaData()
         .withName("fooJoinBar")
         .withLeftTable("foo")
         .withRightTable("bar")
         .withType(JoinType.ONE_TO_MANY)
         .withJoinOn(new JoinOn("barId", "id"));
      qInstance.addJoin(fooJoinBar);

      QWidgetMetaData widget = ChildRecordListRenderer.widgetMetaDataBuilder(fooJoinBar).getWidgetMetaData();
      qInstance.addWidget(widget);

      new InsertAction().execute(new InsertInput("foo").withRecord(new QRecord().withValue("id", 1)));

      RenderWidgetInput input = new RenderWidgetInput();
      input.setWidgetMetaData(widget);
      input.setQueryParams(new HashMap<>(Map.of("id", "1")));

      RenderWidgetAction  renderWidgetAction  = new RenderWidgetAction();
      RenderWidgetOutput  output              = renderWidgetAction.execute(input);
      ChildRecordListData childRecordListData = (ChildRecordListData) output.getWidgetData();

      assertThat(childRecordListData.getQueryOutput().getRecords()).isEmpty();
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private static void insertTwoOrdersAndThreeLines(QInstance qInstance) throws QException
   {
      TestUtils.insertRecords(qInstance.getTable(TestUtils.TABLE_NAME_ORDER), List.of(
         new QRecord().withValue("id", 1).withValue("orderNo", "ORD001"),
         new QRecord().withValue("id", 2).withValue("orderNo", "ORD002")
      ));

      TestUtils.insertRecords(qInstance.getTable(TestUtils.TABLE_NAME_LINE_ITEM), List.of(
         new QRecord().withValue("orderId", 1).withValue("sku", "ABC").withValue("lineNumber", 2),
         new QRecord().withValue("orderId", 1).withValue("sku", "BCD").withValue("lineNumber", 1),
         new QRecord().withValue("orderId", 2).withValue("sku", "XYZ")
      ));
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private static ChildRecordListData insertOrdersAndRenderWidget(QInstance qInstance, QWidgetMetaData widget) throws QException
   {
      insertTwoOrdersAndThreeLines(qInstance);

      RenderWidgetInput input = new RenderWidgetInput();
      input.setWidgetMetaData(widget);
      input.setQueryParams(new HashMap<>(Map.of("id", "1")));

      RenderWidgetAction renderWidgetAction = new RenderWidgetAction();
      RenderWidgetOutput output             = renderWidgetAction.execute(input);

      ChildRecordListData childRecordListData = (ChildRecordListData) output.getWidgetData();
      return childRecordListData;
   }

}