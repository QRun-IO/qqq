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

package com.kingsrook.qqq.backend.core.actions.tables.picker;


import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.metadata.TableMetaDataAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.picker.RelatedRecordPickerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.picker.RelatedRecordPickerOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.audits.AuditsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditRules;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendRelatedRecordPicker;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import com.kingsrook.qqq.backend.core.model.metadata.tables.picker.QRelatedRecordPicker;
import com.kingsrook.qqq.backend.core.model.metadata.tables.picker.RelatedRecordPickerMode;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for RelatedRecordPickerAction (and the picker's frontend
 ** metadata): an order with a customer (SINGLE), lines of SKUs (MULTI_CHILD)
 ** and tags (LINK); SKUs and orders are locked by store.
 *******************************************************************************/
class RelatedRecordPickerActionTest extends BaseTest
{
   static final String CUSTOMER   = "rrp_customer";
   static final String SKU        = "rrp_sku";
   static final String ORDER      = "rrp_order";
   static final String ORDER_LINE = "rrp_order_line";
   static final String TAG        = "rrp_tag";
   static final String ORDER_TAG  = "rrp_order_tag";

   static final String STORE = TestUtils.SECURITY_KEY_TYPE_STORE;



   /*******************************************************************************
    ** Add the picker tables to the standard test instance.
    *******************************************************************************/
   public static void addPickerTables(QInstance qInstance)
   {
      String backend = TestUtils.MEMORY_BACKEND_NAME;

      qInstance.addTable(new QTableMetaData().withName(CUSTOMER).withBackendName(backend).withPrimaryKeyField("id")
         .withRecordLabelFormatAndFields("%s", "name").withSearchFields("name", "email")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("name", QFieldType.STRING).withIsRequired(true))
         .withField(new QFieldMetaData("email", QFieldType.STRING))
         .withField(new QFieldMetaData("region", QFieldType.STRING))
         .withField(new QFieldMetaData("isActive", QFieldType.BOOLEAN))
         .withSection(new QFieldSection("identity", "Identity", null, Tier.T1, List.of("id", "name", "email")))
         .withSection(new QFieldSection("details", "Details", null, Tier.T2, List.of("region", "isActive")))
         .withQuickCreateSectionName("identity"));

      qInstance.addTable(new QTableMetaData().withName(SKU).withBackendName(backend).withPrimaryKeyField("id")
         .withRecordLabelFormatAndFields("%s", "code")
         .withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType(STORE).withFieldName("storeId"))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("code", QFieldType.STRING))
         .withField(new QFieldMetaData("name", QFieldType.STRING))
         .withField(new QFieldMetaData("price", QFieldType.DECIMAL))
         .withField(new QFieldMetaData("region", QFieldType.STRING))
         .withField(new QFieldMetaData("isActive", QFieldType.BOOLEAN))
         .withField(new QFieldMetaData("storeId", QFieldType.INTEGER)));

      qInstance.addTable(new QTableMetaData().withName(TAG).withBackendName(backend).withPrimaryKeyField("id")
         .withRecordLabelFormatAndFields("%s", "name")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("name", QFieldType.STRING)));

      qInstance.addPossibleValueSource(QPossibleValueSource.newForTable(CUSTOMER));
      qInstance.addPossibleValueSource(QPossibleValueSource.newForTable(SKU));
      qInstance.addPossibleValueSource(QPossibleValueSource.newForTable(TAG));

      qInstance.addJoin(new QJoinMetaData().withName("rrpOrderLine").withLeftTable(ORDER).withRightTable(ORDER_LINE).withType(JoinType.ONE_TO_MANY).withJoinOn(new JoinOn("id", "orderId")));
      qInstance.addJoin(new QJoinMetaData().withName("rrpOrderTag").withLeftTable(ORDER).withRightTable(ORDER_TAG).withType(JoinType.ONE_TO_MANY).withJoinOn(new JoinOn("id", "orderId")));

      qInstance.addTable(new QTableMetaData().withName(ORDER_LINE).withBackendName(backend).withPrimaryKeyField("id")
         .withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType(STORE).withFieldName(ORDER + ".storeId").withJoinNameChain(List.of("rrpOrderLine")))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("orderId", QFieldType.INTEGER))
         .withField(new QFieldMetaData("skuId", QFieldType.INTEGER).withPossibleValueSourceName(SKU))
         .withField(new QFieldMetaData("quantity", QFieldType.INTEGER).withIsRequired(true))
         .withField(new QFieldMetaData("unitPrice", QFieldType.DECIMAL))
         .withField(new QFieldMetaData("note", QFieldType.STRING)));

      qInstance.addTable(new QTableMetaData().withName(ORDER_TAG).withBackendName(backend).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("orderId", QFieldType.INTEGER))
         .withField(new QFieldMetaData("tagId", QFieldType.INTEGER).withPossibleValueSourceName(TAG)));

      qInstance.addTable(new QTableMetaData().withName(ORDER).withBackendName(backend).withPrimaryKeyField("id")
         .withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType(STORE).withFieldName("storeId"))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("customerId", QFieldType.INTEGER).withPossibleValueSourceName(CUSTOMER))
         .withField(new QFieldMetaData("region", QFieldType.STRING))
         .withField(new QFieldMetaData("storeId", QFieldType.INTEGER))
         .withAssociation(new Association().withName("orderLines").withAssociatedTableName(ORDER_LINE).withJoinName("rrpOrderLine"))
         .withAssociation(new Association().withName("orderTags").withAssociatedTableName(ORDER_TAG).withJoinName("rrpOrderTag"))
         .withRelatedRecordPicker(new QRelatedRecordPicker().withName("customer").withMode(RelatedRecordPickerMode.SINGLE).withFieldName("customerId")
            .withFilter(new QQueryFilter(new QFilterCriteria("isActive", QCriteriaOperator.EQUALS, true)))
            .withDefaultFilter(new QQueryFilter(new QFilterCriteria("region", QCriteriaOperator.EQUALS, "${input.region}"))))
         .withRelatedRecordPicker(new QRelatedRecordPicker().withName("addSkus").withLabel("Add SKUs").withMode(RelatedRecordPickerMode.MULTI_CHILD)
            .withAssociationName("orderLines").withChildTargetFieldName("skuId")
            .withFilter(new QQueryFilter(new QFilterCriteria("isActive", QCriteriaOperator.EQUALS, true)))
            .withColumns(List.of("code", "name", "price"))
            .withChildFieldDefaults(Map.of("quantity", 1, "note", "picked"))
            .withChildValuesFromTarget(Map.of("unitPrice", "price"))
            .withReviewFieldNames(List.of("quantity", "unitPrice")))
         .withRelatedRecordPicker(new QRelatedRecordPicker().withName("tags").withMode(RelatedRecordPickerMode.LINK)
            .withAssociationName("orderTags").withChildTargetFieldName("tagId")));
   }



   /*******************************************************************************
    ** Instance with the picker tables, a store-1 session and some data.
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();
      addPickerTables(qInstance);
      new AuditsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);
      qInstance.getTable(ORDER_LINE).setAuditRules(new QAuditRules().withAuditLevel(AuditLevel.RECORD));
      reInitInstanceInContext(qInstance);
      QContext.getQSession().withSecurityKeyValue(STORE, 1);

      TestUtils.insertRecords(qInstance.getTable(CUSTOMER), List.of(
         new QRecord().withValue("id", 1).withValue("name", "Acme").withValue("region", "West").withValue("isActive", true),
         new QRecord().withValue("id", 2).withValue("name", "Bolt").withValue("region", "East").withValue("isActive", true),
         new QRecord().withValue("id", 3).withValue("name", "Cogs").withValue("region", "West").withValue("isActive", false)));
      TestUtils.insertRecords(qInstance.getTable(SKU), List.of(
         new QRecord().withValue("id", 11).withValue("code", "WID-1").withValue("name", "Widget").withValue("price", new BigDecimal("9.50")).withValue("isActive", true).withValue("storeId", 1),
         new QRecord().withValue("id", 12).withValue("code", "GAD-2").withValue("name", "Gadget").withValue("price", new BigDecimal("4.00")).withValue("isActive", true).withValue("storeId", 1),
         new QRecord().withValue("id", 13).withValue("code", "OLD-3").withValue("name", "Retired").withValue("price", new BigDecimal("1.00")).withValue("isActive", false).withValue("storeId", 1),
         new QRecord().withValue("id", 14).withValue("code", "OTH-4").withValue("name", "Other store").withValue("price", new BigDecimal("2.00")).withValue("isActive", true).withValue("storeId", 2)));
      TestUtils.insertRecords(qInstance.getTable(TAG), List.of(
         new QRecord().withValue("id", 21).withValue("name", "Rush"),
         new QRecord().withValue("id", 22).withValue("name", "Gift")));
      TestUtils.insertRecords(qInstance.getTable(ORDER), List.of(
         new QRecord().withValue("id", 101).withValue("region", "West").withValue("storeId", 1),
         new QRecord().withValue("id", 102).withValue("region", "East").withValue("storeId", 2)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private RelatedRecordPickerInput input(String pickerName)
   {
      return (new RelatedRecordPickerInput(ORDER, pickerName));
   }



   /*******************************************************************************
    ** SINGLE: picker filter, resolved default filter from form values, search.
    *******************************************************************************/
   @Test
   void testQuerySingle() throws QException
   {
      RelatedRecordPickerOutput output = new RelatedRecordPickerAction().query(input("customer").withValues(Map.of("region", "West")));
      assertThat(output.getRecords()).extracting(r -> r.getValueString("name")).containsExactly("Acme", "Bolt");
      assertFalse(output.getHasMore());
      assertNull(output.getLinkedIds());
      assertEquals("West", output.getResolvedDefaultFilter().getCriteria().get(0).getValues().get(0));

      output = new RelatedRecordPickerAction().query(input("customer").withSearchTerm("bol"));
      assertThat(output.getRecords()).extracting(r -> r.getValueString("name")).containsExactly("Bolt");

      output = new RelatedRecordPickerAction().query(input("customer").withFilter(new QQueryFilter(new QFilterCriteria("region", QCriteriaOperator.EQUALS, "East"))));
      assertThat(output.getRecords()).extracting(r -> r.getValueString("name")).containsExactly("Bolt");

      ///////////////////////////////////////////////////////////////////
      // without a value for ${input.region}, the criterion is dropped //
      ///////////////////////////////////////////////////////////////////
      output = new RelatedRecordPickerAction().query(input("customer"));
      assertEquals(QCriteriaOperator.TRUE, output.getResolvedDefaultFilter().getCriteria().get(0).getOperator());

      output = new RelatedRecordPickerAction().query(input("customer").withLimit(1));
      assertEquals(1, output.getRecords().size());
      assertTrue(output.getHasMore());
      output = new RelatedRecordPickerAction().query(input("customer").withLimit(1).withSkip(1));
      assertThat(output.getRecords()).extracting(r -> r.getValueString("name")).containsExactly("Bolt");
      assertNull(output.getResolvedDefaultFilter());

      assertThatThrownBy(() -> new RelatedRecordPickerAction().query(input("customer").withFilter(new QQueryFilter(new QFilterCriteria("nope", QCriteriaOperator.EQUALS, 1)))))
         .isInstanceOf(QBadRequestException.class);
   }



   /*******************************************************************************
    ** MULTI_CHILD query: record locks hide the other store's SKU, the picker
    ** filter hides inactive ones, linkedIds marks SKUs already on the order.
    *******************************************************************************/
   @Test
   void testQueryChildLocksAndLinked() throws QException
   {
      TestUtils.insertRecords(QContext.getQInstance().getTable(ORDER_LINE), List.of(new QRecord().withValue("orderId", 101).withValue("skuId", 12).withValue("quantity", 1)));
      RelatedRecordPickerOutput output = new RelatedRecordPickerAction().query(input("addSkus").withPrimaryKey(101));
      assertThat(output.getRecords()).extracting(r -> r.getValueString("code")).containsExactlyInAnyOrder("WID-1", "GAD-2");
      assertThat(output.getLinkedIds()).containsExactly(12);

      ///////////////////////////////////////////////
      // the other store's order is not found (404) //
      ///////////////////////////////////////////////
      assertThatThrownBy(() -> new RelatedRecordPickerAction().query(input("addSkus").withPrimaryKey(102))).isInstanceOf(QNotFoundException.class);
      assertThatThrownBy(() -> new RelatedRecordPickerAction().query(input("nope"))).isInstanceOf(QNotFoundException.class);
   }



   /*******************************************************************************
    ** prepare: defaults, copied target values, forced join values.
    *******************************************************************************/
   @Test
   void testPrepare() throws QException
   {
      RelatedRecordPickerOutput output = new RelatedRecordPickerAction().prepare(input("addSkus").withPrimaryKey(101).withTargetIds(List.of(11, "12")));
      assertEquals(2, output.getRecords().size());
      QRecord first = output.getRecords().get(0);
      assertEquals(101, first.getValueInteger("orderId"));
      assertEquals(11, first.getValueInteger("skuId"));
      assertEquals(1, first.getValueInteger("quantity"));
      assertEquals(new BigDecimal("9.50"), first.getValueBigDecimal("unitPrice"));
      assertEquals("WID-1", first.getDisplayValue("skuId"));
      assertEquals(List.of("quantity", "unitPrice"), output.getReviewFieldNames());
      assertTrue(TestUtils.queryTable(ORDER_LINE).isEmpty());

      ////////////////////////////////////////////////////////////
      // inactive (picker filter) and other-store (lock) SKUs   //
      ////////////////////////////////////////////////////////////
      assertThatThrownBy(() -> new RelatedRecordPickerAction().prepare(input("addSkus").withPrimaryKey(101).withTargetIds(List.of(13, 14))))
         .isInstanceOf(QBadRequestException.class).hasMessageContaining("13").hasMessageContaining("14");
      assertThatThrownBy(() -> new RelatedRecordPickerAction().prepare(input("customer").withPrimaryKey(101).withTargetIds(List.of(1))))
         .isInstanceOf(QBadRequestException.class);
      assertThatThrownBy(() -> new RelatedRecordPickerAction().prepare(input("addSkus").withTargetIds(List.of(11))))
         .isInstanceOf(QBadRequestException.class);
   }



   /*******************************************************************************
    ** add: review values applied, forced values win, audit written.
    *******************************************************************************/
   @Test
   void testAddWithReviewValues() throws QException
   {
      List<Map<String, Serializable>> rows = new ArrayList<>();
      rows.add(new java.util.HashMap<>(Map.of("skuId", 11, "quantity", "3", "unitPrice", "8.00", "orderId", 999, "note", "client note ignored")));
      rows.add(new java.util.HashMap<>(Map.of("skuId", 12, "quantity", 2)));
      RelatedRecordPickerOutput output = new RelatedRecordPickerAction().add(input("addSkus").withPrimaryKey(101).withRecords(rows));
      assertEquals(2, output.getRecords().size());

      List<QRecord> lines = TestUtils.queryTable(ORDER_LINE);
      assertEquals(2, lines.size());
      QRecord line = lines.stream().filter(r -> r.getValueInteger("skuId") == 11).findFirst().orElseThrow();
      assertEquals(101, line.getValueInteger("orderId"));
      assertEquals(3, line.getValueInteger("quantity"));
      assertEquals(0, new BigDecimal("8.00").compareTo(line.getValueBigDecimal("unitPrice")));
      assertEquals("picked", line.getValueString("note"));

      assertEquals(2, TestUtils.queryTable("audit").size());
   }



   /*******************************************************************************
    ** add is all or nothing: one invalid row (missing required quantity) stops
    ** every row.
    *******************************************************************************/
   @Test
   void testAddAllOrNothing() throws QException
   {
      List<Map<String, Serializable>> rows = new ArrayList<>();
      rows.add(new java.util.HashMap<>(Map.of("skuId", 11, "quantity", 3)));
      java.util.HashMap<String, Serializable> bad = new java.util.HashMap<>();
      bad.put("skuId", 12);
      bad.put("quantity", null);
      rows.add(bad);
      assertThatThrownBy(() -> new RelatedRecordPickerAction().add(input("addSkus").withPrimaryKey(101).withRecords(rows)))
         .isInstanceOf(QBadRequestException.class).hasMessageContaining("Row 2");
      assertTrue(TestUtils.queryTable(ORDER_LINE).isEmpty());

      assertThatThrownBy(() -> new RelatedRecordPickerAction().add(input("addSkus").withPrimaryKey(101).withRecords(List.of(Map.of("quantity", 1)))))
         .isInstanceOf(QBadRequestException.class);
   }



   /*******************************************************************************
    ** LINK: add by target ids, linked ids, remove only this order's links.
    *******************************************************************************/
   @Test
   void testLinkAndUnlink() throws QException
   {
      new RelatedRecordPickerAction().add(input("tags").withPrimaryKey(101).withTargetIds(List.of(21, 22)));
      assertEquals(2, TestUtils.queryTable(ORDER_TAG).size());
      assertThat(new RelatedRecordPickerAction().query(input("tags").withPrimaryKey(101)).getLinkedIds()).containsExactlyInAnyOrder(21, 22);

      RelatedRecordPickerOutput output = new RelatedRecordPickerAction().remove(input("tags").withPrimaryKey(101).withTargetIds(List.of(22)));
      assertEquals(1, output.getDeletedCount());
      assertThat(TestUtils.queryTable(ORDER_TAG)).extracting(r -> r.getValueInteger("tagId")).containsExactly(21);

      assertThatThrownBy(() -> new RelatedRecordPickerAction().remove(input("addSkus").withPrimaryKey(101).withTargetIds(List.of(11))))
         .isInstanceOf(QBadRequestException.class);
   }



   /*******************************************************************************
    ** Permissions: READ on the target to query, INSERT on the child to add,
    ** DELETE on the join table to remove; frontend flags follow them.
    *******************************************************************************/
   @Test
   void testPermissions() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      for(String tableName : List.of(ORDER, SKU, ORDER_LINE, ORDER_TAG, TAG, CUSTOMER))
      {
         qInstance.getTable(tableName).setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      }
      QSession session = newSession().withSecurityKeyValue(STORE, 1)
         .withPermissions(ORDER + ".read", TAG + ".read", ORDER_TAG + ".insert", CUSTOMER + ".read");
      QContext.init(qInstance, session);

      assertThatThrownBy(() -> new RelatedRecordPickerAction().query(input("addSkus").withPrimaryKey(101))).isInstanceOf(QPermissionDeniedException.class);
      new RelatedRecordPickerAction().add(input("tags").withPrimaryKey(101).withTargetIds(List.of(21)));
      assertThatThrownBy(() -> new RelatedRecordPickerAction().remove(input("tags").withPrimaryKey(101).withTargetIds(List.of(21)))).isInstanceOf(QPermissionDeniedException.class);

      TableMetaDataInput metaDataInput = new TableMetaDataInput();
      metaDataInput.setTableName(ORDER);
      List<QFrontendRelatedRecordPicker> pickers = new TableMetaDataAction().execute(metaDataInput).getTable().getRelatedRecordPickers();
      assertThat(pickers).extracting(QFrontendRelatedRecordPicker::getName).containsExactly("customer", "tags");
      QFrontendRelatedRecordPicker tags = pickers.get(1);
      assertTrue(tags.getCanAddChildren());
      assertFalse(tags.getCanRemoveChildren());
      assertFalse(tags.getCanCreateTarget());
      assertEquals("Link " + qInstance.getTable(TAG).getLabel(), tags.getLabel());
      assertEquals("name", tags.getColumns().get(0));
   }



   /*******************************************************************************
    ** Frontend metadata defaults with full access.
    *******************************************************************************/
   @Test
   void testFrontendMetaData() throws QException
   {
      TableMetaDataInput metaDataInput = new TableMetaDataInput();
      metaDataInput.setTableName(ORDER);
      List<QFrontendRelatedRecordPicker> pickers = new TableMetaDataAction().execute(metaDataInput).getTable().getRelatedRecordPickers();
      assertEquals(3, pickers.size());

      QFrontendRelatedRecordPicker customer = pickers.get(0);
      assertEquals("SINGLE", customer.getMode());
      assertEquals(CUSTOMER, customer.getTargetTableName());
      assertEquals(List.of("name", "id", "email"), customer.getColumns());
      assertEquals(List.of("id", "name", "email"), customer.getPreviewFields());
      assertTrue(customer.getCanCreateTarget());
      assertNull(customer.getMaxSelections());

      QFrontendRelatedRecordPicker skus = pickers.get(1);
      assertEquals("Add SKUs", skus.getLabel());
      assertEquals(ORDER_LINE, skus.getChildTableName());
      assertEquals(List.of("quantity", "unitPrice"), skus.getReviewFieldNames());
      assertEquals(200, skus.getMaxSelections());

      metaDataInput.setTableName(CUSTOMER);
      assertEquals("identity", new TableMetaDataAction().execute(metaDataInput).getTable().getQuickCreateSectionName());
   }
}
