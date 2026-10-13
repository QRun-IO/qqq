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

package com.kingsrook.sampleapp.metadata;


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.ChildRecordListRenderer;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerMultiOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DisplayFormat;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import com.kingsrook.qqq.backend.core.model.metadata.tables.picker.QRelatedRecordPicker;
import com.kingsrook.qqq.backend.core.model.metadata.tables.picker.RelatedRecordPickerMode;


/*******************************************************************************
 ** Sample sales tables for the related-record picker
 ** (docs/RELATED-RECORD-PICKER.md): an order picks its customer (SINGLE, with
 ** quick-create), adds SKUs as order lines with a review step (MULTI_CHILD),
 ** and links tags (LINK).
 *******************************************************************************/
public class SalesMetaDataProducer extends MetaDataProducer<MetaDataProducerMultiOutput>
{
   public static final String APP_NAME         = "sales";
   public static final String CUSTOMER         = "customer";
   public static final String SKU              = "sku";
   public static final String TAG              = "tag";
   public static final String SALES_ORDER      = "sales_order";
   public static final String SALES_ORDER_LINE = "sales_order_line";
   public static final String SALES_ORDER_TAG  = "sales_order_tag";

   private static final String JOIN_ORDER_LINE = "salesOrderJoinLine";
   private static final String JOIN_ORDER_TAG  = "salesOrderJoinTag";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public MetaDataProducerMultiOutput produce(QInstance qInstance)
   {
      MetaDataProducerMultiOutput output = new MetaDataProducerMultiOutput();

      output.add(table(CUSTOMER, "Customer", "name")
         .withSearchFields("name", "email")
         .withField(new QFieldMetaData("name", QFieldType.STRING).withIsRequired(true).withMaxLength(100))
         .withField(new QFieldMetaData("email", QFieldType.STRING).withMaxLength(250))
         .withField(new QFieldMetaData("phone", QFieldType.STRING).withMaxLength(40))
         .withField(new QFieldMetaData("region", QFieldType.STRING).withMaxLength(40))
         .withField(new QFieldMetaData("isActive", QFieldType.BOOLEAN).withDefaultValue(true))
         .withSection(new QFieldSection("identity", "Identity", new QIcon("badge"), Tier.T1, List.of("id", "name", "email")))
         .withSection(new QFieldSection("details", "Details", new QIcon("dataset"), Tier.T2, List.of("phone", "region", "isActive")))
         .withSection(dates())
         .withQuickCreateSectionName("identity"));

      output.add(table(SKU, "SKU", "code")
         .withSearchFields("code", "name")
         .withField(new QFieldMetaData("code", QFieldType.STRING).withIsRequired(true).withMaxLength(40))
         .withField(new QFieldMetaData("name", QFieldType.STRING).withIsRequired(true).withMaxLength(100))
         .withField(new QFieldMetaData("description", QFieldType.STRING).withMaxLength(250))
         .withField(new QFieldMetaData("price", QFieldType.DECIMAL).withDisplayFormat(DisplayFormat.CURRENCY))
         .withField(new QFieldMetaData("region", QFieldType.STRING).withMaxLength(40))
         .withField(new QFieldMetaData("isActive", QFieldType.BOOLEAN).withDefaultValue(true))
         .withSection(new QFieldSection("identity", "Identity", new QIcon("inventory_2"), Tier.T1, List.of("id", "code", "name")))
         .withSection(new QFieldSection("details", "Details", new QIcon("dataset"), Tier.T2, List.of("description", "price", "region", "isActive")))
         .withSection(dates()));

      output.add(table(TAG, "Tag", "name")
         .withSearchFields("name")
         .withField(new QFieldMetaData("name", QFieldType.STRING).withIsRequired(true).withMaxLength(40))
         .withSection(new QFieldSection("identity", "Identity", new QIcon("sell"), Tier.T1, List.of("id", "name")))
         .withSection(dates()));

      output.add(QPossibleValueSource.newForTable(CUSTOMER));
      output.add(QPossibleValueSource.newForTable(SKU));
      output.add(QPossibleValueSource.newForTable(TAG));
      output.add(QPossibleValueSource.newForTable(SALES_ORDER));

      QJoinMetaData lineJoin = new QJoinMetaData().withName(JOIN_ORDER_LINE).withLeftTable(SALES_ORDER).withRightTable(SALES_ORDER_LINE)
         .withType(JoinType.ONE_TO_MANY).withJoinOn(new JoinOn("id", "salesOrderId")).withOrderBy(new QFilterOrderBy("id"));
      QJoinMetaData tagJoin = new QJoinMetaData().withName(JOIN_ORDER_TAG).withLeftTable(SALES_ORDER).withRightTable(SALES_ORDER_TAG)
         .withType(JoinType.ONE_TO_MANY).withJoinOn(new JoinOn("id", "salesOrderId")).withOrderBy(new QFilterOrderBy("id"));
      output.add(lineJoin);
      output.add(tagJoin);

      output.add(table(SALES_ORDER_LINE, "Sales Order Line", "id")
         .withField(new QFieldMetaData("salesOrderId", QFieldType.INTEGER).withIsRequired(true).withPossibleValueSourceName(SALES_ORDER))
         .withField(new QFieldMetaData("skuId", QFieldType.INTEGER).withIsRequired(true).withPossibleValueSourceName(SKU).withLabel("SKU"))
         .withField(new QFieldMetaData("quantity", QFieldType.INTEGER).withIsRequired(true).withDefaultValue(1))
         .withField(new QFieldMetaData("unitPrice", QFieldType.DECIMAL).withDisplayFormat(DisplayFormat.CURRENCY))
         .withSection(new QFieldSection("identity", "Line", new QIcon("list"), Tier.T1, List.of("id", "salesOrderId", "skuId", "quantity", "unitPrice")))
         .withSection(dates()));

      output.add(table(SALES_ORDER_TAG, "Sales Order Tag", "id")
         .withField(new QFieldMetaData("salesOrderId", QFieldType.INTEGER).withIsRequired(true).withPossibleValueSourceName(SALES_ORDER))
         .withField(new QFieldMetaData("tagId", QFieldType.INTEGER).withIsRequired(true).withPossibleValueSourceName(TAG))
         .withSection(new QFieldSection("identity", "Tag", new QIcon("sell"), Tier.T1, List.of("id", "salesOrderId", "tagId")))
         .withSection(dates()));

      output.add(ChildRecordListRenderer.widgetMetaDataBuilder(lineJoin).withName("salesOrderLines").withLabel("Lines").withMaxRows(50).getWidgetMetaData());
      output.add(ChildRecordListRenderer.widgetMetaDataBuilder(tagJoin).withName("salesOrderTags").withLabel("Tags").withMaxRows(50).getWidgetMetaData());

      output.add(table(SALES_ORDER, "Sales Order", "orderNo")
         .withSearchFields("orderNo")
         .withField(new QFieldMetaData("orderNo", QFieldType.STRING).withIsRequired(true).withMaxLength(40))
         .withField(new QFieldMetaData("customerId", QFieldType.INTEGER).withPossibleValueSourceName(CUSTOMER))
         .withField(new QFieldMetaData("region", QFieldType.STRING).withMaxLength(40))
         .withField(new QFieldMetaData("orderDate", QFieldType.DATE))
         .withAssociation(new Association().withName("lines").withAssociatedTableName(SALES_ORDER_LINE).withJoinName(JOIN_ORDER_LINE))
         .withAssociation(new Association().withName("tags").withAssociatedTableName(SALES_ORDER_TAG).withJoinName(JOIN_ORDER_TAG))
         .withSection(new QFieldSection("identity", "Identity", new QIcon("receipt_long"), Tier.T1, List.of("id", "orderNo", "customerId")))
         .withSection(new QFieldSection("details", "Details", new QIcon("dataset"), Tier.T2, List.of("region", "orderDate")))
         .withSection(new QFieldSection().withName("lines").withLabel("Lines").withTier(Tier.T2).withWidgetName("salesOrderLines"))
         .withSection(new QFieldSection().withName("tags").withLabel("Tags").withTier(Tier.T2).withWidgetName("salesOrderTags"))
         .withSection(dates())
         .withRelatedRecordPicker(new QRelatedRecordPicker()
            .withName("customer")
            .withMode(RelatedRecordPickerMode.SINGLE)
            .withFieldName("customerId")
            .withFilter(new QQueryFilter(new QFilterCriteria("isActive", QCriteriaOperator.EQUALS, true)))
            .withDefaultFilter(new QQueryFilter(new QFilterCriteria("region", QCriteriaOperator.EQUALS, "${input.region}")))
            .withColumns(List.of("name", "email", "region"))
            .withPreviewFields(List.of("name", "email", "phone", "region")))
         .withRelatedRecordPicker(new QRelatedRecordPicker()
            .withName("addSkus")
            .withLabel("Add SKUs")
            .withMode(RelatedRecordPickerMode.MULTI_CHILD)
            .withAssociationName("lines")
            .withChildTargetFieldName("skuId")
            .withFilter(new QQueryFilter(new QFilterCriteria("isActive", QCriteriaOperator.EQUALS, true)))
            .withColumns(List.of("code", "name", "price", "region"))
            .withPreviewFields(List.of("code", "name", "description", "price", "region"))
            .withChildFieldDefaults(Map.of("quantity", 1))
            .withChildValuesFromTarget(Map.of("unitPrice", "price"))
            .withReviewFieldNames(List.of("quantity", "unitPrice")))
         .withRelatedRecordPicker(new QRelatedRecordPicker()
            .withName("tags")
            .withLabel("Tags")
            .withMode(RelatedRecordPickerMode.LINK)
            .withAssociationName("tags")
            .withChildTargetFieldName("tagId")));

      inferBackendNames(output);
      output.add(new QAppMetaData()
         .withName(APP_NAME)
         .withLabel("Sales")
         .withIcon(new QIcon("storefront"))
         .withChild(tableIn(output, SALES_ORDER).withIcon(new QIcon("receipt_long")))
         .withChild(tableIn(output, CUSTOMER).withIcon(new QIcon("groups")))
         .withChild(tableIn(output, SKU).withIcon(new QIcon("inventory_2")))
         .withChild(tableIn(output, TAG).withIcon(new QIcon("sell"))));

      return (output);
   }



   /*******************************************************************************
    ** A table on the sample's RDBMS backend with id and dates.
    *******************************************************************************/
   private static QTableMetaData table(String name, String label, String labelField)
   {
      return (new QTableMetaData()
         .withName(name)
         .withLabel(label)
         .withBackendName(SampleMetaDataProvider.RDBMS_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withRecordLabelFormat("%s")
         .withRecordLabelFields(labelField)
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("createDate", QFieldType.DATE_TIME).withIsEditable(false))
         .withField(new QFieldMetaData("modifyDate", QFieldType.DATE_TIME).withIsEditable(false)));
   }



   /*******************************************************************************
    ** The standard dates section.
    *******************************************************************************/
   private static QFieldSection dates()
   {
      return (new QFieldSection("dates", "Dates", new QIcon("calendar_month"), Tier.T3, List.of("createDate", "modifyDate")));
   }



   /*******************************************************************************
    ** A table this producer made.
    *******************************************************************************/
   private static QTableMetaData tableIn(MetaDataProducerMultiOutput output, String name)
   {
      return (output.getEach(QTableMetaData.class).stream().filter(t -> t.getName().equals(name)).findFirst().orElseThrow());
   }



   /*******************************************************************************
    ** Infer the backend (column) names of every sales table: camelCase to
    ** snake_case.
    *******************************************************************************/
   static void inferBackendNames(MetaDataProducerMultiOutput output)
   {
      output.getEach(QTableMetaData.class).forEach(QInstanceEnricher::setInferredFieldBackendNames);
   }
}
