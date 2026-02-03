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
import com.kingsrook.qqq.backend.core.instances.QInstanceValidatorTest;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
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
   void testFlippingJoin() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);

      ///////////////////////////////////////////////////////////////////
      // build a many-to-one join - e.g., flipped from what's expected //
      ///////////////////////////////////////////////////////////////////
      qInstance.addJoin(new QJoinMetaData()
         .withName("lineItemJoinOrder")
         .withType(JoinType.MANY_TO_ONE)
         .withLeftTable(TestUtils.TABLE_NAME_LINE_ITEM)
         .withRightTable(TestUtils.TABLE_NAME_ORDER)
         .withJoinOn(new JoinOn("orderId", "id"))
         .withOrderBy(new QFilterOrderBy("lineNumber")));

      ////////////////////////////////////////////
      // make a widget with this backwards join //
      ////////////////////////////////////////////
      QWidgetMetaData widget = ChildRecordListRenderer.widgetMetaDataBuilder(qInstance.getJoin("lineItemJoinOrder"))
         .withLabel("Line Items")
         .getWidgetMetaData();
      qInstance.addWidget(widget);

      //////////////////////////////////
      // make sure it fails to render //
      //////////////////////////////////
      assertThatThrownBy(() -> insertOrdersAndRenderWidget(qInstance, widget));

      //////////////////////////////////////////////////////////////////////////////////
      // remove the bad widget - replace it with one where the flipJoin param is true //
      //////////////////////////////////////////////////////////////////////////////////
      qInstance.getWidgets().remove(widget.getName());
      QWidgetMetaData flippedWidget = ChildRecordListRenderer.widgetMetaDataBuilder(qInstance.getJoin("lineItemJoinOrder"))
         .withFlipJoin(true)
         .withLabel("Line Items")
         .getWidgetMetaData();
      qInstance.addWidget(flippedWidget);

      ////////////////////////////////
      // now the widget should work //
      ////////////////////////////////
      MemoryRecordStore.getInstance().reset();
      ChildRecordListData childRecordListData = insertOrdersAndRenderWidget(qInstance, flippedWidget);

      assertThat(childRecordListData.getChildTableMetaData()).hasFieldOrPropertyWithValue("name", TestUtils.TABLE_NAME_LINE_ITEM);
      assertThat(childRecordListData.getQueryOutput().getRecords()).hasSize(2);
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
      assertTrue(childRecordListData.getChildFrontendTableMetaData().getFields().containsKey("lineNumber"));
      assertTrue(childRecordListData.getChildTableMetaData().getFields().containsKey("lineNumber"));

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
      assertFalse(childRecordListData.getChildFrontendTableMetaData().getFields().containsKey("lineNumber"));
      assertFalse(childRecordListData.getChildTableMetaData().getFields().containsKey("lineNumber"));
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



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBaseFilterAndOrderBy() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);
      QWidgetMetaData widget = ChildRecordListRenderer.widgetMetaDataBuilder(qInstance.getJoin("orderLineItem"))
         .withLabel("Line Items")
         .withBaseFilter(new QQueryFilter(new QFilterCriteria("lineNumber", QCriteriaOperator.NOT_EQUALS, 3)))
         .withOrderBys(List.of(new QFilterOrderBy("lineNumber", false)))
         .withOnlyIncludeFieldNames(List.of("sku", "quantity"))
         .getWidgetMetaData();
      qInstance.addWidget(widget);

      //////////////////////////////////////////////////////////////////////////////////////////////////////
      // insert a few more lines that'll match the order inserted by the insertOrdersAndRenderWidget call //
      //////////////////////////////////////////////////////////////////////////////////////////////////////
      TestUtils.insertRecords(qInstance.getTable(TestUtils.TABLE_NAME_LINE_ITEM), List.of(
         new QRecord().withValue("orderId", 1).withValue("sku", "CDE").withValue("lineNumber", 3),
         new QRecord().withValue("orderId", 1).withValue("sku", "DEF").withValue("lineNumber", 4)));
      ChildRecordListData childRecordListData = insertOrdersAndRenderWidget(qInstance, widget);

      assertThat(childRecordListData.getChildTableMetaData()).hasFieldOrPropertyWithValue("name", TestUtils.TABLE_NAME_LINE_ITEM);
      assertThat(childRecordListData.getQueryOutput().getRecords()).hasSize(3);
      assertThat(childRecordListData.getQueryOutput().getRecords().get(0).getValueInteger("lineNumber")).isEqualTo(4);
      assertThat(childRecordListData.getQueryOutput().getRecords().get(1).getValueInteger("lineNumber")).isEqualTo(2);
      assertThat(childRecordListData.getQueryOutput().getRecords().get(2).getValueInteger("lineNumber")).isEqualTo(1);

      assertEquals(List.of("sku", "quantity"), childRecordListData.getOnlyIncludeFieldNames());
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



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testValidation()
   {
      /////////////////////////////////
      // failure for not a real join //
      /////////////////////////////////
      QInstanceValidatorTest.assertValidationFailureReasons(qInstance ->
            qInstance.addWidget(ChildRecordListRenderer.widgetMetaDataBuilder(new QJoinMetaData().withName("notReal"))
               .getWidgetMetaData()),
         "Widget notReal: No join named notReal exists");

      //////////////////////////////////////
      // failure for bad association name //
      //////////////////////////////////////
      QInstanceValidatorTest.assertValidationFailureReasons(qInstance ->
            qInstance.addWidget(ChildRecordListRenderer.widgetMetaDataBuilder(qInstance.getJoin("orderLineItem"))
               .withManageAssociationName("noAssociation")
               .getWidgetMetaData()),
         "an association named [noAssociation] does not exist on table [order]");

      /////////////////////////////
      // failure for base filter //
      /////////////////////////////
      QInstanceValidatorTest.assertValidationFailureReasons(qInstance ->
            qInstance.addWidget(ChildRecordListRenderer.widgetMetaDataBuilder(qInstance.getJoin("orderLineItem"))
               .withBaseFilter(new QQueryFilter(new QFilterCriteria("noField", QCriteriaOperator.EQUALS, 1)))
               .getWidgetMetaData()),
         "Widget orderLineItem: baseFilter: Criteria fieldName noField is not a field in this table");

      //////////////////////////////
      // bad types in the configs //
      //////////////////////////////
      QInstanceValidatorTest.assertValidationFailureReasons(qInstance ->
            qInstance.addWidget(ChildRecordListRenderer.widgetMetaDataBuilder(qInstance.getJoin("orderLineItem"))
               .getWidgetMetaData()
               .withDefaultValue(ChildRecordListRenderer.KEY_BASE_FILTER, "order = 1")),
         "Widget orderLineItem: baseFilter is not an instance of QQueryFilter");

      QInstanceValidatorTest.assertValidationFailureReasons(qInstance ->
            qInstance.addWidget(ChildRecordListRenderer.widgetMetaDataBuilder(qInstance.getJoin("orderLineItem"))
               .getWidgetMetaData()
               .withDefaultValue(ChildRecordListRenderer.KEY_QUERY_JOINS, "order")),
         "Widget orderLineItem: queryJoins is not an instance of List");

      QInstanceValidatorTest.assertValidationFailureReasons(qInstance ->
            qInstance.addWidget(ChildRecordListRenderer.widgetMetaDataBuilder(qInstance.getJoin("orderLineItem"))
               .getWidgetMetaData()
               .withDefaultValue(ChildRecordListRenderer.KEY_QUERY_JOINS, new ArrayList<>(List.of("order")))),
         "Widget orderLineItem: queryJoins has an element which is not a QueryJoin");

      QInstanceValidatorTest.assertValidationFailureReasons(qInstance ->
            qInstance.addWidget(ChildRecordListRenderer.widgetMetaDataBuilder(qInstance.getJoin("orderLineItem"))
               .getWidgetMetaData()
               .withDefaultValue(ChildRecordListRenderer.KEY_ORDER_BY, "sku")),
         "Widget orderLineItem: orderBy is not an instance of List");

      QInstanceValidatorTest.assertValidationFailureReasons(qInstance ->
            qInstance.addWidget(ChildRecordListRenderer.widgetMetaDataBuilder(qInstance.getJoin("orderLineItem"))
               .getWidgetMetaData()
               .withDefaultValue(ChildRecordListRenderer.KEY_ORDER_BY, new ArrayList<>(List.of("sku")))),
         "Widget orderLineItem: orderBy has an element which is not a QFilterOrderBy");

      //////////////////////
      // successful cases //
      //////////////////////
      QInstanceValidatorTest.assertValidationSuccess(qInstance ->
         qInstance.addWidget(ChildRecordListRenderer.widgetMetaDataBuilder(qInstance.getJoin("orderLineItem"))
            .getWidgetMetaData()));

      QInstanceValidatorTest.assertValidationSuccess(qInstance ->
         qInstance.addWidget(ChildRecordListRenderer.widgetMetaDataBuilder(qInstance.getJoin("orderLineItem"))
            .withManageAssociationName("orderLine")
            .getWidgetMetaData()));

      QInstanceValidatorTest.assertValidationSuccess(qInstance ->
         qInstance.addWidget(ChildRecordListRenderer.widgetMetaDataBuilder(qInstance.getJoin("orderLineItem"))
            .withBaseFilter(new QQueryFilter(new QFilterCriteria("quantity", QCriteriaOperator.EQUALS, 1)))
            .getWidgetMetaData()));

      QInstanceValidatorTest.assertValidationSuccess(qInstance ->
         qInstance.addWidget(ChildRecordListRenderer.widgetMetaDataBuilder(qInstance.getJoin("orderLineItem"))
            .withQueryJoins(List.of(new QueryJoin("order")))
            .withBaseFilter(new QQueryFilter(new QFilterCriteria("order.storeId", QCriteriaOperator.EQUALS, 1)))
            .withOrderBys(List.of(new QFilterOrderBy("order.id"), new QFilterOrderBy("sku")))
            .getWidgetMetaData()));
   }

}