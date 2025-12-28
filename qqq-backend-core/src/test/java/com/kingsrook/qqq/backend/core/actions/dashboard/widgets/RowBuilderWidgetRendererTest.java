/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidatorTest;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.RowBuilderData;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for RowBuilderWidgetRenderer 
 *******************************************************************************/
class RowBuilderWidgetRendererTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      QWidgetMetaData widgetMetaData = RowBuilderWidgetRenderer.widgetMetaDataBuilder("testWidget")
         .withParentTableName(TestUtils.TABLE_NAME_ORDER)
         .withAssociationName("orderLine")
         .withFields(List.of(
            new QFieldMetaData("sku", QFieldType.STRING),
            new QFieldMetaData("quantity", QFieldType.INTEGER)))
         .getWidgetMetaData();
      QContext.getQInstance().addWidget(widgetMetaData);

      Map<String, String> params = new HashMap<>();
      widgetMetaData.getDefaultValues().forEach((key, value) -> params.put(key, ValueUtils.getValueAsString(value)));
      params.put("id", "1");

      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true));
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_ORDER).withRecords(List.of(
         new QRecord().withValue("id", 1)
            .withAssociatedRecord("orderLine", new QRecord().withValue("sku", "A").withValue("quantity", 1))
            .withAssociatedRecord("orderLine", new QRecord().withValue("sku", "B").withValue("quantity", 2))
      )));

      RenderWidgetInput input = new RenderWidgetInput()
         .withWidgetMetaData(widgetMetaData)
         .withUrlParams(params);
      RenderWidgetOutput widgetOutput   = new RowBuilderWidgetRenderer().render(input);
      RowBuilderData     rowBuilderData = ((RowBuilderData) widgetOutput.getWidgetData());
      assertEquals(2, rowBuilderData.getRecords().size());
      assertThat(rowBuilderData.getRecords()).anyMatch(r -> Objects.equals("A", r.getValueString("sku")) && Objects.equals(1, r.getValueInteger("quantity")));
      assertThat(rowBuilderData.getRecords()).anyMatch(r -> Objects.equals("B", r.getValueString("sku")) && Objects.equals(2, r.getValueInteger("quantity")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testValidation()
   {
      /////////////////////
      // field use cases //
      /////////////////////
      QInstanceValidatorTest.assertValidationFailureReasons(qInstance -> qInstance.addWidget(RowBuilderWidgetRenderer.widgetMetaDataBuilder("testWidget")
            .getWidgetMetaData()),
         "Must have a fields list with 1 or more QFieldMetaData");

      QInstanceValidatorTest.assertValidationFailureReasons(qInstance -> qInstance.addWidget(RowBuilderWidgetRenderer.widgetMetaDataBuilder("testWidget")
            .withFields(List.of())
            .getWidgetMetaData()),
         "Must have a fields list with 1 or more QFieldMetaData");

      List<?> notFields = List.of(new Object());
      QInstanceValidatorTest.assertValidationFailureReasons(qInstance -> qInstance.addWidget(RowBuilderWidgetRenderer.widgetMetaDataBuilder("testWidget")
            .withFields((List<QFieldMetaData>) notFields)
            .getWidgetMetaData()),
         "Error validating fields: class java.lang.Object cannot be cast");

      QInstanceValidatorTest.assertValidationFailureReasons(qInstance -> qInstance.addWidget(RowBuilderWidgetRenderer.widgetMetaDataBuilder("testWidget")
            .withFields(List.of(
               new QFieldMetaData("a", QFieldType.STRING),
               new QFieldMetaData("a", QFieldType.STRING)
            ))
            .getWidgetMetaData()),
         "more than 1 field named [a]");

      QInstanceValidatorTest.assertValidationFailureReasons(qInstance -> qInstance.addWidget(RowBuilderWidgetRenderer.widgetMetaDataBuilder("testWidget")
            .withFields(List.of(new QFieldMetaData("a", null)))
            .getWidgetMetaData()),
         "Widget testWidget:  Field a: is missing a type");

      QInstanceValidatorTest.assertValidationFailureReasons(qInstance -> qInstance.addWidget(RowBuilderWidgetRenderer.widgetMetaDataBuilder("testWidget")
            .withFields(List.of(
               new QFieldMetaData("", QFieldType.INTEGER),
               new QFieldMetaData(null, QFieldType.INTEGER)
            ))
            .getWidgetMetaData()),
         "Widget testWidget:  Field : is missing a name",
         "Widget testWidget:  Field null: is missing a name");

      /////////////////////
      // table use cases //
      /////////////////////
      QInstanceValidatorTest.assertValidationFailureReasons(qInstance -> qInstance.addWidget(RowBuilderWidgetRenderer.widgetMetaDataBuilder("testWidget")
            .withFields(List.of(new QFieldMetaData("a", QFieldType.STRING)))
            .withParentTableName(TestUtils.TABLE_NAME_ORDER)
            .getWidgetMetaData()),
         "Has some attributes set for use on a table, but not all");

      QInstanceValidatorTest.assertValidationFailureReasons(qInstance -> qInstance.addWidget(RowBuilderWidgetRenderer.widgetMetaDataBuilder("testWidget")
            .withFields(List.of(new QFieldMetaData("a", QFieldType.STRING)))
            .withAssociationName("orderLine")
            .getWidgetMetaData()),
         "Has some attributes set for use on a table, but not all");

      QInstanceValidatorTest.assertValidationFailureReasons(qInstance -> qInstance.addWidget(RowBuilderWidgetRenderer.widgetMetaDataBuilder("testWidget")
            .withFields(List.of(new QFieldMetaData("a", QFieldType.STRING)))
            .withIsForRecordViewAndEditScreen(true)
            .getWidgetMetaData()),
         "Has some attributes set for use on a table, but not all");

      QInstanceValidatorTest.assertValidationFailureReasons(qInstance -> qInstance.addWidget(RowBuilderWidgetRenderer.widgetMetaDataBuilder("testWidget")
            .withFields(List.of(new QFieldMetaData("a", QFieldType.STRING)))
            .withIsForRecordViewAndEditScreen(false)
            .withParentTableName(TestUtils.TABLE_NAME_ORDER)
            .withAssociationName("orderLine")
            .getWidgetMetaData()),
         "Has some attributes set for use on a table, but not all");

      QInstanceValidatorTest.assertValidationFailureReasons(qInstance -> qInstance.addWidget(RowBuilderWidgetRenderer.widgetMetaDataBuilder("testWidget")
            .withFields(List.of(new QFieldMetaData("a", QFieldType.STRING)))
            .withIsForRecordViewAndEditScreen(true)
            .withParentTableName(TestUtils.TABLE_NAME_ORDER + "not")
            .withAssociationName("orderLine")
            .getWidgetMetaData()),
         "Specified an unrecognized value for parentTableName [ordernot]");

      QInstanceValidatorTest.assertValidationFailureReasons(qInstance -> qInstance.addWidget(RowBuilderWidgetRenderer.widgetMetaDataBuilder("testWidget")
            .withFields(List.of(new QFieldMetaData("a", QFieldType.STRING)))
            .withIsForRecordViewAndEditScreen(true)
            .withParentTableName(TestUtils.TABLE_NAME_ORDER)
            .withAssociationName("orderLineNot")
            .getWidgetMetaData()),
         "Specified an unrecognized association name [orderLineNot] within table [order]");

      QInstanceValidatorTest.assertValidationFailureReasons(qInstance -> qInstance.addWidget(RowBuilderWidgetRenderer.widgetMetaDataBuilder("testWidget")
            .withFields(List.of(new QFieldMetaData("a", QFieldType.STRING)))
            .withIsForRecordViewAndEditScreen(true)
            .withParentTableName(TestUtils.TABLE_NAME_ORDER)
            .withAssociationName("orderLineNot")
            .getWidgetMetaData()),
         "Specified an unrecognized association name [orderLineNot] within table [order]");

      QInstanceValidatorTest.assertValidationSuccess(qInstance -> qInstance.addWidget(RowBuilderWidgetRenderer.widgetMetaDataBuilder("testWidget")
         .withFields(List.of(new QFieldMetaData("a", QFieldType.STRING)))
         .withIsForRecordViewAndEditScreen(true)
         .withParentTableName(TestUtils.TABLE_NAME_ORDER)
         .withAssociationName("orderLine")
         .getWidgetMetaData()));

      ///////////////////////
      // reorder use cases //
      ///////////////////////
      QInstanceValidatorTest.assertValidationFailureReasons(qInstance -> qInstance.addWidget(RowBuilderWidgetRenderer.widgetMetaDataBuilder("testWidget")
            .withFields(List.of(new QFieldMetaData("a", QFieldType.STRING)))
            .withMayReorderRows(true)
            .getWidgetMetaData()),
         "missing orderByFieldName");

      QInstanceValidatorTest.assertValidationFailureReasons(qInstance -> qInstance.addWidget(RowBuilderWidgetRenderer.widgetMetaDataBuilder("testWidget")
            .withFields(List.of(new QFieldMetaData("a", QFieldType.STRING)))
            .withOrderByFieldName("a")
            .getWidgetMetaData()),
         "should not specify orderByFieldName unless mayReorderRows=true");

      QInstanceValidatorTest.assertValidationFailureReasons(qInstance -> qInstance.addWidget(RowBuilderWidgetRenderer.widgetMetaDataBuilder("testWidget")
            .withFields(List.of(new QFieldMetaData("a", QFieldType.STRING)))
            .withMayReorderRows(true)
            .withOrderByFieldName("x")
            .getWidgetMetaData()),
         "specified an unrecognized value for orderByFieldName [x]");

      QInstanceValidatorTest.assertValidationSuccess(qInstance -> qInstance.addWidget(RowBuilderWidgetRenderer.widgetMetaDataBuilder("testWidget")
         .withFields(List.of(new QFieldMetaData("a", QFieldType.STRING)))
         .withMayReorderRows(true)
         .withOrderByFieldName("a")
         .getWidgetMetaData()));

   }

}