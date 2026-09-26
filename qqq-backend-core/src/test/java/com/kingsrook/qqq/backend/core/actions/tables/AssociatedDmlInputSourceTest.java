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

package com.kingsrook.qqq.backend.core.actions.tables;


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPreDeleteCustomizer;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.ExamplePersonalizer;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerInterface;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.CaseChangeBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Associated writes retain the caller's field rules through every DML boundary.
 *******************************************************************************/
class AssociatedDmlInputSourceTest extends BaseTest
{
   /*******************************************************************************
    ** Required child fields must not become optional when reached through a parent.
    *******************************************************************************/
   @Test
   void testInsertUsesChildAndGrandchildPersonalization() throws QException
   {
      requireChildFieldsForUser();
      QRecord userOrder = new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_ORDER)
         .withInputSource(QInputSource.USER).withRecord(orderWithMissingChildFields())).getRecords().get(0);

      assertThat(userOrder.getAssociatedRecords().get("orderLine").get(0).getErrorsAsString()).contains("required");
      assertThat(userOrder.getAssociatedRecords().get("orderLine").get(1).getAssociatedRecords().get("extrinsics").get(0).getErrorsAsString()).contains("required");
      assertEquals(List.of("valid-sku"), TestUtils.queryTable(TestUtils.TABLE_NAME_LINE_ITEM).stream().map(record -> record.getValueString("sku")).toList());
      assertEquals(List.of("valid-key"), TestUtils.queryTable(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC).stream().map(record -> record.getValueString("key")).toList());

      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_ORDER)
         .withInputSource(QInputSource.SYSTEM).withRecord(orderWithMissingChildFields()));
      assertEquals(3, TestUtils.queryTable(TestUtils.TABLE_NAME_LINE_ITEM).size());
      assertEquals(3, TestUtils.queryTable(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC).size());
   }



   /*******************************************************************************
    ** New descendants submitted on update follow the same rules as a direct insert.
    *******************************************************************************/
   @Test
   void testUpdateInsertsUseChildAndGrandchildPersonalization() throws QException
   {
      requireChildFieldsForUser();
      QRecord parent = new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_ORDER)
         .withRecord(new QRecord().withValue("storeId", 1).withValue("orderNo", "parent"))).getRecords().get(0);

      new UpdateAction().execute(new UpdateInput(TestUtils.TABLE_NAME_ORDER).withInputSource(QInputSource.USER)
         .withRecord(orderWithMissingChildFields().withValue("id", parent.getValue("id"))));
      assertEquals(List.of("valid-sku"), TestUtils.queryTable(TestUtils.TABLE_NAME_LINE_ITEM).stream().map(record -> record.getValueString("sku")).toList());
      assertEquals(List.of("valid-key"), TestUtils.queryTable(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC).stream().map(record -> record.getValueString("key")).toList());

      new UpdateAction().execute(new UpdateInput(TestUtils.TABLE_NAME_ORDER).withInputSource(QInputSource.SYSTEM)
         .withRecord(orderWithMissingChildFields().withValue("id", parent.getValue("id"))));
      assertEquals(2, TestUtils.queryTable(TestUtils.TABLE_NAME_LINE_ITEM).size());
      assertEquals(2, TestUtils.queryTable(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC).size());
   }



   /*******************************************************************************
    ** USER field behavior must apply to existing descendants as well as new ones.
    *******************************************************************************/
   @Test
   void testUpdateUsesChildAndGrandchildPersonalization() throws QException
   {
      QRecord parent = insertValidOrder();
      ExamplePersonalizer.registerInQInstance();
      for(String tableName : List.of(TestUtils.TABLE_NAME_LINE_ITEM, TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC))
      {
         String fieldName = tableName.equals(TestUtils.TABLE_NAME_LINE_ITEM) ? "sku" : "value";
         ExamplePersonalizer.addCustomizableTable(tableName);
         ExamplePersonalizer.addFieldToAddForUserId(tableName,
            QContext.getQInstance().getTable(tableName).getField(fieldName).clone().withBehavior(CaseChangeBehavior.TO_UPPER_CASE), DEFAULT_USER_ID);
      }

      new UpdateAction().execute(new UpdateInput(TestUtils.TABLE_NAME_ORDER).withInputSource(QInputSource.USER).withRecord(orderUpdate(parent)));
      assertEquals("UPDATED-SKU", TestUtils.queryTable(TestUtils.TABLE_NAME_LINE_ITEM).get(0).getValueString("sku"));
      assertEquals("UPDATED-VALUE", TestUtils.queryTable(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC).get(0).getValueString("value"));

      new UpdateAction().execute(new UpdateInput(TestUtils.TABLE_NAME_ORDER).withInputSource(QInputSource.SYSTEM).withRecord(orderUpdate(parent)));
      assertEquals("updated-sku", TestUtils.queryTable(TestUtils.TABLE_NAME_LINE_ITEM).get(0).getValueString("sku"));
      assertEquals("updated-value", TestUtils.queryTable(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC).get(0).getValueString("value"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCascadeDeletePreservesSourceAndSchemaAssociations() throws QException
   {
      QRecord userOrder = insertValidOrder();
      QContext.getQInstance().addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(HideAssociationsPersonalizer.class));
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC)
         .withCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(ProtectUserDeletes.class));

      List<Map<String, Serializable>> originalOrders = values(TestUtils.TABLE_NAME_ORDER);
      List<Map<String, Serializable>> originalLines = values(TestUtils.TABLE_NAME_LINE_ITEM);
      List<Map<String, Serializable>> originalExtrinsics = values(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC);
      DeleteOutput denied = new DeleteAction().execute(new DeleteInput(TestUtils.TABLE_NAME_ORDER).withInputSource(QInputSource.USER)
         .withPrimaryKeys(List.of(userOrder.getValue("id"))));
      assertEquals(0, denied.getDeletedRecordCount());
      assertThat(denied.getRecordsWithErrors()).singleElement().satisfies(record -> assertThat(record.getErrors()).isNotEmpty());
      assertEquals(originalOrders, values(TestUtils.TABLE_NAME_ORDER));
      assertEquals(originalLines, values(TestUtils.TABLE_NAME_LINE_ITEM));
      assertEquals(originalExtrinsics, values(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC));

      QRecord systemOrder = insertValidOrder();
      DeleteOutput allowed = new DeleteAction().execute(new DeleteInput(TestUtils.TABLE_NAME_ORDER).withInputSource(QInputSource.SYSTEM)
         .withPrimaryKeys(List.of(systemOrder.getValue("id"))));
      assertEquals(1, allowed.getDeletedRecordCount());
      assertThat(allowed.getRecordsWithErrors()).isEmpty();
      assertEquals(originalOrders, values(TestUtils.TABLE_NAME_ORDER));
      assertEquals(originalLines, values(TestUtils.TABLE_NAME_LINE_ITEM));
      assertEquals(originalExtrinsics, values(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUpdateOmissionDeletesRetainSourceThroughCascade() throws QException
   {
      QRecord userOrder = insertValidOrder();
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC)
         .withCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(ProtectUserDeletes.class));

      List<Map<String, Serializable>> originalOrders = values(TestUtils.TABLE_NAME_ORDER);
      List<Map<String, Serializable>> originalLines = values(TestUtils.TABLE_NAME_LINE_ITEM);
      List<Map<String, Serializable>> originalExtrinsics = values(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC);
      QRecord denied = new UpdateAction().execute(new UpdateInput(TestUtils.TABLE_NAME_ORDER).withInputSource(QInputSource.USER)
         .withRecord(new QRecord().withValue("id", userOrder.getValue("id")).withValue("orderNo", "denied-change")
            .withAssociatedRecords("orderLine", List.of()))).getRecords().get(0);
      assertThat(denied.getErrors()).isNotEmpty();
      assertEquals(originalOrders, values(TestUtils.TABLE_NAME_ORDER));
      assertEquals(originalLines, values(TestUtils.TABLE_NAME_LINE_ITEM));
      assertEquals(originalExtrinsics, values(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC));

      QRecord systemOrder = insertValidOrder();
      QRecord allowed = new UpdateAction().execute(new UpdateInput(TestUtils.TABLE_NAME_ORDER).withInputSource(QInputSource.SYSTEM)
         .withRecord(new QRecord().withValue("id", systemOrder.getValue("id")).withAssociatedRecords("orderLine", List.of()))).getRecords().get(0);
      assertThat(allowed.getErrors()).isEmpty();
      assertEquals(2, TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER).size());
      assertEquals(originalOrders.get(0), values(TestUtils.TABLE_NAME_ORDER).get(0));
      assertEquals(originalLines, values(TestUtils.TABLE_NAME_LINE_ITEM));
      assertEquals(originalExtrinsics, values(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC));
   }



   /*******************************************************************************
    ** Successful and failed actions restore the caller context without nested entries.
    *******************************************************************************/
   @Test
   void testDmlRestoresCallerActionStack() throws QException
   {
      UpdateInput caller = new UpdateInput(TestUtils.TABLE_NAME_ORDER);
      QContext.pushAction(caller);
      try
      {
         QRecord parent = insertValidOrder();
         QRecord updated = new UpdateAction().execute(new UpdateInput(TestUtils.TABLE_NAME_ORDER)
            .withRecord(new QRecord().withValue("id", parent.getValue("id")).withValue("orderNo", "stack-control"))).getRecords().get(0);
         assertThat(updated.getErrors()).isEmpty();
         assertEquals(1, QContext.getActionStack().size());
         assertSame(caller, QContext.getFirstActionInStack().orElseThrow());
         assertEquals(1, new DeleteAction().execute(new DeleteInput(TestUtils.TABLE_NAME_ORDER).withPrimaryKey(parent.getValue("id"))).getDeletedRecordCount());
         assertEquals(1, QContext.getActionStack().size());
         assertSame(caller, QContext.getFirstActionInStack().orElseThrow());
         assertThrows(QException.class, () -> new UpdateAction().execute(new UpdateInput()));
         assertThrows(QException.class, () -> new DeleteAction().execute(new DeleteInput()));
         assertEquals(1, QContext.getActionStack().size());
         assertSame(caller, QContext.getFirstActionInStack().orElseThrow());
      }
      finally
      {
         QContext.popAction();
      }
      assertThat(QContext.getActionStack()).isEmpty();
   }



   /*******************************************************************************
    ** Compare every fetched value without relying on QRecord object identity.
    *******************************************************************************/
   private List<Map<String, Serializable>> values(String tableName) throws QException
   {
      return TestUtils.queryTable(tableName).stream().map(QRecord::getValues).toList();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord orderUpdate(QRecord parent)
   {
      QRecord child = parent.getAssociatedRecords().get("orderLine").get(0);
      QRecord grandchild = child.getAssociatedRecords().get("extrinsics").get(0);
      return new QRecord().withValue("id", parent.getValue("id"))
         .withAssociatedRecord("orderLine", new QRecord().withValue("id", child.getValue("id")).withValue("sku", "updated-sku")
            .withAssociatedRecord("extrinsics", new QRecord().withValue("id", grandchild.getValue("id")).withValue("value", "updated-value")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord insertValidOrder() throws QException
   {
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);
      return new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_ORDER).withInputSource(QInputSource.SYSTEM)
         .withRecord(new QRecord().withValue("storeId", 1).withValue("orderNo", "source-rules")
            .withAssociatedRecord("orderLine", new QRecord().withValue("sku", "initial-sku").withValue("quantity", 1)
               .withAssociatedRecord("extrinsics", new QRecord().withValue("key", "protected").withValue("value", "initial-value"))))).getRecords().get(0);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void requireChildFieldsForUser()
   {
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);
      ExamplePersonalizer.registerInQInstance();
      for(String tableName : List.of(TestUtils.TABLE_NAME_LINE_ITEM, TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC))
      {
         String fieldName = tableName.equals(TestUtils.TABLE_NAME_LINE_ITEM) ? "sku" : "key";
         ExamplePersonalizer.addCustomizableTable(tableName);
         ExamplePersonalizer.addFieldToAddForUserId(tableName,
            QContext.getQInstance().getTable(tableName).getField(fieldName).clone().withIsRequired(true), DEFAULT_USER_ID);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord orderWithMissingChildFields()
   {
      return new QRecord().withValue("storeId", 1).withValue("orderNo", "source-rules")
         .withAssociatedRecord("orderLine", new QRecord().withValue("quantity", 1))
         .withAssociatedRecord("orderLine", new QRecord().withValue("sku", "valid-sku").withValue("quantity", 2)
            .withAssociatedRecord("extrinsics", new QRecord().withValue("value", "missing-key"))
            .withAssociatedRecord("extrinsics", new QRecord().withValue("key", "valid-key").withValue("value", "kept")));
   }



   /*******************************************************************************
    ** Display personalization must not redefine schema-owned cascade deletion.
    *******************************************************************************/
   public static class HideAssociationsPersonalizer implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         return QInputSource.USER.equals(input.getInputSource()) ? input.getTable().clone().withAssociations(List.of()) : input.getTable();
      }
   }



   /*******************************************************************************
    ** A table's write rule remains observable when reached through a cascade.
    *******************************************************************************/
   public static class ProtectUserDeletes extends AbstractPreDeleteCustomizer
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> apply(List<QRecord> records)
      {
         AbstractTableActionInput owner = (AbstractTableActionInput) QContext.getFirstActionInStack().orElseThrow();
         assertEquals(TestUtils.TABLE_NAME_ORDER, owner.getTableName());
         if(QInputSource.USER.equals(deleteInput.getInputSource()))
         {
            records.forEach(record -> record.addError(new BadInputStatusMessage("Protected from user deletion")));
         }
         return records;
      }
   }
}
