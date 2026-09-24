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

package com.kingsrook.qqq.backend.core.model.data;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.testentities.Item;
import com.kingsrook.qqq.backend.core.model.data.testentities.ItemWithPrimitives;
import com.kingsrook.qqq.backend.core.model.data.testentities.LineItem;
import com.kingsrook.qqq.backend.core.model.data.testentities.Order;
import com.kingsrook.qqq.backend.core.model.data.testentities.OrderWithoutTableName;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DisplayFormat;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 **
 *******************************************************************************/
class QRecordEntityTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws QException
   {
      QContext.getQInstance().addTable(new QTableMetaData()
         .withName(Item.TABLE_NAME)
         .withFieldsFromEntity(Item.class)
      );
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      QContext.getQInstance().getTables().remove(Item.TABLE_NAME);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testItemToQRecord() throws QException
   {
      Item item = new Item();
      item.setSku("ABC-123");
      item.setDescription("My Item");
      item.setQuantity(47);
      item.setPrice(new BigDecimal("3.50"));
      item.setFeatured(true);

      QRecord qRecord = item.toQRecord();
      assertEquals("ABC-123", qRecord.getValueString("sku"));
      assertEquals("My Item", qRecord.getValueString("description"));
      assertEquals(47, qRecord.getValueInteger("quantity"));
      assertEquals(new BigDecimal("3.50"), qRecord.getValueBigDecimal("price"));
      assertTrue(qRecord.getValueBoolean("featured"));

      /////////////////////////////////////////////////////////////////////////////////////////////////////////
      // assert that, if we had no lists of associations in the entity, that we also have none in the record //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////
      assertThat(qRecord.getAssociatedRecords()).isNullOrEmpty();

      ///////////////////////////////////////////////////////////////////////
      // now assert that an empty list translates through to an empty list //
      ///////////////////////////////////////////////////////////////////////
      item.setItemAlternates(Collections.emptyList());
      qRecord = item.toQRecord();
      assertTrue(qRecord.getAssociatedRecords().containsKey(Item.ASSOCIATION_ITEM_ALTERNATES_NAME));
      assertTrue(qRecord.getAssociatedRecords().get(Item.ASSOCIATION_ITEM_ALTERNATES_NAME).isEmpty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testItemToQRecordWithAssociations() throws QException
   {
      Item item = new Item();
      item.setSku("ABC-123");
      item.setQuantity(47);
      item.setItemAlternates(List.of(
         new Item().withSku("DEF"),
         new Item().withSku("GHI").withQuantity(3)
      ));

      QRecord qRecord = item.toQRecord();
      assertEquals("ABC-123", qRecord.getValueString("sku"));
      assertEquals(47, qRecord.getValueInteger("quantity"));

      List<QRecord> associatedRecords = qRecord.getAssociatedRecords().get(Item.ASSOCIATION_ITEM_ALTERNATES_NAME);
      assertEquals(2, associatedRecords.size());
      assertEquals("DEF", associatedRecords.get(0).getValue("sku"));
      assertTrue(associatedRecords.get(0).getValues().containsKey("quantity"));
      assertNull(associatedRecords.get(0).getValue("quantity"));
      assertEquals("GHI", associatedRecords.get(1).getValue("sku"));
      assertEquals(3, associatedRecords.get(1).getValue("quantity"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @SuppressWarnings("deprecation")
   @Test
   void testItemToQRecordOnlyChangedFieldsEntityThatCameFromQRecord() throws QException
   {
      Item item = new Item(new QRecord()
         .withValue("id", 1701)
         .withValue("sku", "ABC-123")
         .withValue("description", null)
         .withValue("quantity", 47)
         .withValue("price", new BigDecimal("3.50"))
         .withValue("isFeatured", true));

      QRecord qRecordOnlyChangedFields = item.toQRecordOnlyChangedFields(false);
      assertTrue(qRecordOnlyChangedFields.getValues().isEmpty());

      QRecord qRecordOnlyChangedFieldsIncludePKey = item.toQRecordOnlyChangedFields(true);
      assertEquals(1, qRecordOnlyChangedFieldsIncludePKey.getValues().size());
      assertEquals(1701, qRecordOnlyChangedFieldsIncludePKey.getValue("id"));

      item.setDescription("My Changed Item");
      qRecordOnlyChangedFields = item.toQRecordOnlyChangedFields(false);
      assertEquals(1, qRecordOnlyChangedFields.getValues().size());
      assertEquals("My Changed Item", qRecordOnlyChangedFields.getValueString("description"));

      qRecordOnlyChangedFieldsIncludePKey = item.toQRecordOnlyChangedFields(true);
      assertEquals(2, qRecordOnlyChangedFieldsIncludePKey.getValues().size());
      assertEquals("My Changed Item", qRecordOnlyChangedFieldsIncludePKey.getValueString("description"));
      assertEquals(1701, qRecordOnlyChangedFieldsIncludePKey.getValue("id"));

      item.setPrice(null);
      qRecordOnlyChangedFields = item.toQRecordOnlyChangedFields(false);
      assertEquals(2, qRecordOnlyChangedFields.getValues().size());
      assertNull(qRecordOnlyChangedFields.getValueString("price"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @SuppressWarnings("deprecation")
   @Test
   void testItemToQRecordOnlyChangedFieldsFromNewEntity() throws QException
   {
      Item item = new Item()
         .withId(1701)
         .withSku("ABC-123");

      QRecord qRecordOnlyChangedFields = item.toQRecordOnlyChangedFields(false);
      assertEquals(2, qRecordOnlyChangedFields.getValues().size());
      assertEquals(1701, qRecordOnlyChangedFields.getValue("id"));
      assertEquals("ABC-123", qRecordOnlyChangedFields.getValue("sku"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @SuppressWarnings("deprecation")
   @Test
   void testItemToQRecordOnlyChangedFieldsWithAssociations() throws QException
   {
      Item item = new Item(new QRecord()
         .withValue("id", 1701)
         .withValue("sku", "ABC-123")
         .withAssociatedRecord(Item.ASSOCIATION_ITEM_ALTERNATES_NAME, new Item(new QRecord()
            .withValue("id", 1702)
            .withValue("sku", "DEF")
            .withValue("quantity", 3)
            .withValue("price", new BigDecimal("3.50"))
         ).toQRecord())
      );

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // if no values were changed in the entities, from when they were constructed (from records), then value maps should be empty //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      QRecord qRecordOnlyChangedFields = item.toQRecordOnlyChangedFields(false);
      assertTrue(qRecordOnlyChangedFields.getValues().isEmpty());
      List<QRecord> associatedRecords = qRecordOnlyChangedFields.getAssociatedRecords().get(Item.ASSOCIATION_ITEM_ALTERNATES_NAME);
      assertTrue(associatedRecords.get(0).getValues().isEmpty());

      ///////////////////////////////////////////////////////
      // but - if pkeys are requested, confirm we get them //
      ///////////////////////////////////////////////////////
      qRecordOnlyChangedFields = item.toQRecordOnlyChangedFields(true);
      assertEquals(1, qRecordOnlyChangedFields.getValues().size());
      assertEquals(1701, qRecordOnlyChangedFields.getValue("id"));
      associatedRecords = qRecordOnlyChangedFields.getAssociatedRecords().get(Item.ASSOCIATION_ITEM_ALTERNATES_NAME);
      assertEquals(1, associatedRecords.get(0).getValues().size());
      assertEquals(1702, associatedRecords.get(0).getValue("id"));

      ////////////////////////////////////////////
      // change some properties in the entities //
      ////////////////////////////////////////////
      item.setDescription("My Changed Item");
      item.getItemAlternates().get(0).setQuantity(4);
      item.getItemAlternates().get(0).setPrice(null);

      qRecordOnlyChangedFields = item.toQRecordOnlyChangedFields(true);
      assertEquals(2, qRecordOnlyChangedFields.getValues().size());
      assertEquals(1701, qRecordOnlyChangedFields.getValue("id"));
      assertEquals("My Changed Item", qRecordOnlyChangedFields.getValue("description"));
      associatedRecords = qRecordOnlyChangedFields.getAssociatedRecords().get(Item.ASSOCIATION_ITEM_ALTERNATES_NAME);
      assertEquals(3, associatedRecords.get(0).getValues().size());
      assertEquals(1702, associatedRecords.get(0).getValue("id"));
      assertEquals(4, associatedRecords.get(0).getValue("quantity"));
      assertNull(associatedRecords.get(0).getValue("price"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQRecordToItem() throws QException
   {
      QRecord qRecord = new QRecord()
         .withValue("sku", "WXYZ-9876")
         .withValue("description", "Items are cool")
         .withValue("quantity", 42)
         .withValue("price", new BigDecimal("3.50"))
         .withValue("featured", false);

      Item item = qRecord.toEntity(Item.class);
      assertEquals("WXYZ-9876", item.getSku());
      assertEquals("Items are cool", item.getDescription());
      assertEquals(42, item.getQuantity());
      assertEquals(new BigDecimal("3.50"), item.getPrice());
      assertFalse(item.getFeatured());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQRecordFromJoinToItem() throws QException
   {
      QRecord qRecord = new QRecord()
         .withValue("item.sku", "WXYZ-9876")
         .withValue("item.description", "Items are cool")
         .withValue("item.quantity", 42)
         .withValue("item.price", new BigDecimal("3.50"))
         .withValue("item.featured", false);

      Item item = QRecordEntity.fromQRecord(Item.class, qRecord, "item.");
      assertEquals("WXYZ-9876", item.getSku());
      assertEquals("Items are cool", item.getDescription());
      assertEquals(42, item.getQuantity());
      assertEquals(new BigDecimal("3.50"), item.getPrice());
      assertFalse(item.getFeatured());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testItemWithPrimitivesToQRecord() throws QException
   {
      ItemWithPrimitives item = new ItemWithPrimitives();
      item.setSku("ABC-123");
      item.setDescription(null);
      item.setQuantity(47);
      item.setPrice(new BigDecimal("3.50"));
      item.setFeatured(true);

      QRecord qRecord = item.toQRecord();
      assertEquals("ABC-123", qRecord.getValueString("sku"));
      assertNull(qRecord.getValueString("description"));
      assertEquals(47, qRecord.getValueInteger("quantity"));
      assertEquals(new BigDecimal("3.50"), qRecord.getValueBigDecimal("price"));
      assertTrue(qRecord.getValueBoolean("featured"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQRecordToItemWithPrimitives() throws QException
   {
      QRecord qRecord = new QRecord()
         .withValue("sku", "WXYZ-9876")
         .withValue("description", null)
         .withValue("quantity", 42)
         .withValue("price", new BigDecimal("3.50"))
         .withValue("featured", false);

      ItemWithPrimitives item = qRecord.toEntity(ItemWithPrimitives.class);
      assertEquals("WXYZ-9876", item.getSku());
      assertNull(item.getDescription());
      assertEquals(42, item.getQuantity());
      assertEquals(new BigDecimal("3.50"), item.getPrice());
      assertFalse(item.getFeatured());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQRecordWithAllStringValuesToItem() throws QException
   {
      QRecord qRecord = new QRecord()
         .withValue("sku", "WXYZ-9876")
         .withValue("description", "Items are cool")
         .withValue("quantity", "42")
         .withValue("price", "3.50")
         .withValue("featured", "false");

      Item item = qRecord.toEntity(Item.class);
      assertEquals("WXYZ-9876", item.getSku());
      assertEquals("Items are cool", item.getDescription());
      assertEquals(42, item.getQuantity());
      assertEquals(new BigDecimal("3.50"), item.getPrice());
      assertFalse(item.getFeatured());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @SuppressWarnings("ResultOfMethodCallIgnored")
   @Test
   void testQTableConstructionFromEntityGetterReferences() throws QException
   {
      QTableMetaData qTableMetaData = new QTableMetaData()
         .withField(new QFieldMetaData(Item::getSku))
         .withField(new QFieldMetaData(Item::getDescription))
         .withField(new QFieldMetaData(Item::getQuantity))
         .withField(new QFieldMetaData(Item::getFeatured))
         .withField(new QFieldMetaData(Item::getPrice));

      assertEquals(QFieldType.STRING, qTableMetaData.getField("sku").getType());
      assertEquals(QFieldType.INTEGER, qTableMetaData.getField("quantity").getType());

      ///////////////////////////////////////////////////////////////
      // assert about attributes that came from @QField annotation //
      ///////////////////////////////////////////////////////////////
      assertEquals("SKU", qTableMetaData.getField("sku").getLabel());
      assertEquals(DisplayFormat.COMMAS, qTableMetaData.getField("quantity").getDisplayFormat());
      assertTrue(qTableMetaData.getField("sku").getIsRequired());
      assertFalse(qTableMetaData.getField("quantity").getIsEditable());
      assertEquals("is_featured", qTableMetaData.getField("featured").getBackendName());

      //////////////////////////////////////////////////////////////////////////
      // assert about attributes that weren't specified in @QField annotation //
      //////////////////////////////////////////////////////////////////////////
      assertTrue(qTableMetaData.getField("sku").getIsEditable());
      assertFalse(qTableMetaData.getField("quantity").getIsRequired());
      assertNull(qTableMetaData.getField("sku").getBackendName());

      /////////////////////////////////////////////////////////////////////
      // assert about attributes for fields without a @QField annotation //
      /////////////////////////////////////////////////////////////////////
      assertTrue(qTableMetaData.getField("price").getIsEditable());
      assertFalse(qTableMetaData.getField("price").getIsRequired());
      assertNull(qTableMetaData.getField("price").getBackendName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQTableConstructionFromEntity() throws QException
   {
      QTableMetaData qTableMetaData = new QTableMetaData()
         .withFieldsFromEntity(Item.class);

      assertEquals(QFieldType.STRING, qTableMetaData.getField("sku").getType());
      assertEquals(QFieldType.INTEGER, qTableMetaData.getField("quantity").getType());

      ///////////////////////////////////////////////////////////////
      // assert about attributes that came from @QField annotation //
      ///////////////////////////////////////////////////////////////
      assertTrue(qTableMetaData.getField("sku").getIsRequired());
      assertFalse(qTableMetaData.getField("quantity").getIsEditable());
      assertEquals("is_featured", qTableMetaData.getField("featured").getBackendName());

      //////////////////////////////////////////////////////////////////////////
      // assert about attributes that weren't specified in @QField annotation //
      //////////////////////////////////////////////////////////////////////////
      assertTrue(qTableMetaData.getField("sku").getIsEditable());
      assertFalse(qTableMetaData.getField("quantity").getIsRequired());
      assertNull(qTableMetaData.getField("sku").getBackendName());

      /////////////////////////////////////////////////////////////////////
      // assert about attributes for fields without a @QField annotation //
      /////////////////////////////////////////////////////////////////////
      assertTrue(qTableMetaData.getField("price").getIsEditable());
      assertFalse(qTableMetaData.getField("price").getIsRequired());
      assertNull(qTableMetaData.getField("price").getBackendName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @SuppressWarnings("ResultOfMethodCallIgnored")
   @Test
   void testQTableConstructionWithPrimitives() throws QException
   {
      QTableMetaData qTableMetaData = new QTableMetaData()
         .withField(new QFieldMetaData(ItemWithPrimitives::getSku))
         .withField(new QFieldMetaData(ItemWithPrimitives::getDescription))
         .withField(new QFieldMetaData(ItemWithPrimitives::getQuantity));

      assertEquals(QFieldType.STRING, qTableMetaData.getField("sku").getType());
      assertEquals(QFieldType.INTEGER, qTableMetaData.getField("quantity").getType());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOrderWithAssociationsToQRecord() throws QException
   {
      Order order = new Order();
      order.setOrderNo("ORD001");
      order.setLineItems(List.of(
         new LineItem().withSku("ABC").withQuantity(1),
         new LineItem().withSku("DEF").withQuantity(2)
      ));

      QRecord qRecord = order.toQRecord();
      assertEquals("ORD001", qRecord.getValueString("orderNo"));
      List<QRecord> lineItems = qRecord.getAssociatedRecords().get("lineItems");
      assertNotNull(lineItems);
      assertEquals(2, lineItems.size());
      assertEquals("ABC", lineItems.get(0).getValueString("sku"));
      assertEquals(1, lineItems.get(0).getValueInteger("quantity"));
      assertEquals("DEF", lineItems.get(1).getValueString("sku"));
      assertEquals(2, lineItems.get(1).getValueInteger("quantity"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOrderWithoutAssociationsToQRecord() throws QException
   {
      Order order = new Order();
      order.setOrderNo("ORD001");
      order.setLineItems(null);

      QRecord qRecord = order.toQRecord();
      assertEquals("ORD001", qRecord.getValueString("orderNo"));
      List<QRecord> lineItems = qRecord.getAssociatedRecords().get("lineItems");
      assertNull(lineItems);

      order.setLineItems(new ArrayList<>());
      qRecord = order.toQRecord();
      lineItems = qRecord.getAssociatedRecords().get("lineItems");
      assertNotNull(lineItems);
      assertEquals(0, lineItems.size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQRecordWithAssociationsToOrder() throws QException
   {
      QRecord qRecord = new QRecord()
         .withValue("orderNo", "ORD002")
         .withAssociatedRecords("lineItems", List.of(
            new QRecord().withValue("sku", "AB12").withValue("quantity", 42),
            new QRecord().withValue("sku", "XY89").withValue("quantity", 47)
         ));

      Order order = qRecord.toEntity(Order.class);
      assertEquals("ORD002", order.getOrderNo());
      assertEquals(2, order.getLineItems().size());
      assertEquals("AB12", order.getLineItems().get(0).getSku());
      assertEquals(42, order.getLineItems().get(0).getQuantity());
      assertEquals("XY89", order.getLineItems().get(1).getSku());
      assertEquals(47, order.getLineItems().get(1).getQuantity());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQRecordWithoutAssociationsToOrder() throws QException
   {
      QRecord qRecord = new QRecord().withValue("orderNo", "ORD002");
      Order   order   = qRecord.toEntity(Order.class);
      assertEquals("ORD002", order.getOrderNo());
      assertNull(order.getLineItems());

      qRecord.withAssociatedRecords("lineItems", null);
      order = qRecord.toEntity(Order.class);
      assertNull(order.getLineItems());

      qRecord.withAssociatedRecords("lineItems", new ArrayList<>());
      order = qRecord.toEntity(Order.class);
      assertNotNull(order.getLineItems());
      assertEquals(0, order.getLineItems().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableName() throws QException
   {
      assertEquals(Item.TABLE_NAME, QRecordEntity.getTableName(Item.class));
      assertEquals(Item.TABLE_NAME, Item.getTableName(Item.class));
      assertEquals(Item.TABLE_NAME, new Item().tableName());

      //////////////////////////////////
      // no TABLE_NAME in Order class //
      //////////////////////////////////
      assertThatThrownBy(() -> OrderWithoutTableName.getTableName(OrderWithoutTableName.class));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQRecordToEntityWithVirtualField() throws QException
   {
      QRecord qRecord = new QRecord()
         .withValue("sku", "WXYZ-9876")
         .withValue("quantity", 42)
         .withValue("displayName", "My Display Name");

      Item item = qRecord.toEntity(Item.class);
      assertEquals("WXYZ-9876", item.getSku());
      assertEquals(42, item.getQuantity());
      assertEquals("My Display Name", item.getDisplayName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testEntityWithVirtualFieldToQRecord() throws QException
   {
      Item item = new Item();
      item.setSku("ABC-123");
      item.setQuantity(47);
      item.setDisplayName("My Display Name");

      QRecord qRecord = item.toQRecord();
      assertEquals("ABC-123", qRecord.getValueString("sku"));
      assertEquals(47, qRecord.getValueInteger("quantity"));
      assertEquals("My Display Name", qRecord.getValueString("displayName"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testVirtualFieldNotInRegularFieldList() throws QException
   {
      List<QRecordEntityField> fieldList = QRecordEntity.getFieldList(Item.class);
      assertThat(fieldList).noneMatch(f -> f.getFieldName().equals("displayName"));
      assertThat(fieldList).anyMatch(f -> f.getFieldName().equals("sku"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetVirtualFieldList() throws QException
   {
      List<QRecordEntityField> virtualFieldList = QRecordEntity.getVirtualFieldList(Item.class);
      assertThat(virtualFieldList).anyMatch(f -> f.getFieldName().equals("displayName"));
      assertThat(virtualFieldList).noneMatch(f -> f.getFieldName().equals("sku"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQRecordFromJoinToEntityWithVirtualField() throws QException
   {
      QRecord qRecord = new QRecord()
         .withValue("item.sku", "WXYZ-9876")
         .withValue("item.quantity", 42)
         .withValue("item.displayName", "Joined Display Name");

      Item item = QRecordEntity.fromQRecord(Item.class, qRecord, "item.");
      assertEquals("WXYZ-9876", item.getSku());
      assertEquals(42, item.getQuantity());
      assertEquals("Joined Display Name", item.getDisplayName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testVirtualFieldNotIncludedInTQRecordOnlyChangedFields() throws QException
   {
      Item item = new Item(new QRecord()
         .withValue("id", 1701)
         .withValue("sku", "ABC-123")
         .withValue("displayName", "My Display Name"));

      assertEquals("My Display Name", item.getDisplayName());

      ///////////////////////////////////////////////////////
      // with nothing changed, this should be an empty set //
      ///////////////////////////////////////////////////////
      QRecord qRecordOnlyChangedFields = item.toQRecordOnlyChangedFields(false);
      assertTrue(qRecordOnlyChangedFields.getValues().isEmpty());

      ///////////////////////////////////////////////////////////////////
      // changing a virtual field shouldn't show up as a changed field //
      ///////////////////////////////////////////////////////////////////
      item.setDisplayName("updated");
      qRecordOnlyChangedFields = item.toQRecordOnlyChangedFields(false);
      assertTrue(qRecordOnlyChangedFields.getValues().isEmpty());

      ///////////////////////////////////////////////////////
      // changing a real field should give that field back //
      ///////////////////////////////////////////////////////
      item.setDescription("updated");
      qRecordOnlyChangedFields = item.toQRecordOnlyChangedFields(false);
      assertEquals(Set.of("description"), qRecordOnlyChangedFields.getValues().keySet());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetQVirtualFieldAnnotation() throws QException
   {
      assertTrue(QRecordEntity.getQVirtualFieldAnnotation(Item.class, "displayName").isPresent());
      assertFalse(QRecordEntity.getQVirtualFieldAnnotation(Item.class, "sku").isPresent());
   }

}