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

package com.kingsrook.qqq.backend.core.actions.audits;


import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.ai.AiFieldMetaData;
import com.kingsrook.qqq.backend.core.actions.ai.DataClassification;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.ClassifiedValueTokens;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.audits.DMLAuditInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.WipeAuditFlag;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.audits.AuditsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Audit details are protected before AuditAction persists them.
 *******************************************************************************/
class AuditValueProtectionTest extends BaseTest
{
   private QTableMetaData table;

   /*******************************************************************************
    ** Setup.
    *******************************************************************************/
   @BeforeEach
   void setup()
   {
      table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      table.getField("firstName").withSupplementalMetaData(new AiFieldMetaData().withClassification(DataClassification.PII));
      table.getField("lastName").withSupplementalMetaData(new AiFieldMetaData().withClassification(DataClassification.PHI));
   }



   /*******************************************************************************
    ** Insert update delete tokens and messages.
    *******************************************************************************/
   @Test
   void insertUpdateDeleteTokensAndMessages()
   {
      QRecord old = new QRecord().withValue("id", 1).withValue("firstName", "secret-old").withValue("lastName", "secret-old");
      QRecord next = new QRecord().withValue("id", 1).withValue("firstName", "secret-new").withValue("lastName", "secret-new");
      for(String field : List.of("firstName", "lastName"))
      {
         QRecord inserted = DMLAuditAction.makeAuditDetailRecordForField(field, table, DMLAuditAction.DMLType.INSERT, next, null).orElseThrow();
         assertNull(inserted.getValue("oldValue"));
         assertEquals(ClassifiedValueTokens.forRecordField(table, table.getField(field), 1, "secret-new"), inserted.getValue("newValue"));
         assertEquals("Set " + table.getField(field).getLabel(), inserted.getValue("message"));
         QRecord updated = DMLAuditAction.makeAuditDetailRecordForField(field, table, DMLAuditAction.DMLType.UPDATE, next, old).orElseThrow();
         assertEquals("Changed " + table.getField(field).getLabel(), updated.getValue("message"));
         assertFalse(updated.getValueString("oldValue").contains("secret"));
         assertFalse(updated.getValueString("newValue").contains("secret"));
         QRecord deleted = DMLAuditAction.makeAuditDetailRecordForField(field, table, DMLAuditAction.DMLType.DELETE, old, null).orElseThrow();
         assertEquals("Removed " + table.getField(field).getLabel(), deleted.getValue("message"));
         assertNull(deleted.getValue("newValue"));
         assertFalse(deleted.getValueString("oldValue").contains("secret"));
      }
   }



   /*******************************************************************************
    ** Wipe and shred are persisted.
    *******************************************************************************/
   @Test
   void wipeAndShredArePersisted() throws Exception
   {
      new AuditsMetaDataProvider().defineAll(QContext.getQInstance(), TestUtils.MEMORY_BACKEND_NAME, null);
      table.setAuditRules(new QAuditRules().withAuditLevel(AuditLevel.FIELD));
      QRecord old = new QRecord().withValue("id", 1).withValue("firstName", "secret");
      QRecord next = new QRecord().withValue("id", 1).withValue("firstName", null);
      new DMLAuditAction().execute(new DMLAuditInput().withTableActionInput(new UpdateInput().withTableName(table.getName()).withFlags(Set.of(new WipeAuditFlag("retention-30"))))
         .withRecordList(List.of(next)).withOldRecordList(List.of(old)));
      QRecord wiped = TestUtils.queryTable("auditDetail").stream().filter(r -> "firstName".equals(r.getValueString("fieldName"))).findFirst().orElseThrow();
      assertEquals("wiped:v1:retention-30", wiped.getValue("newValue"));
      assertNull(wiped.getValue("oldValue"));
      assertEquals("Wiped First Name per policy", wiped.getValue("message"));
      AuditShreddedEvent.shreddedDetail(table, 1, table.getField("firstName"), "retention-30");
      assertTrue(TestUtils.queryTable("auditDetail").stream().anyMatch(r -> "shredded:v1:retention-30".equals(r.getValue("newValue")) && "Audit values for First Name shredded per policy".equals(r.getValue("message"))));
   }



   /*******************************************************************************
    ** Application classification and tokens.
    *******************************************************************************/
   @Test
   void applicationClassificationAndTokens()
   {
      AuditValueProtectionMetaData.ofOrWithNew(QContext.getQInstance()).withProtectorInstance(new AuditValueProtectorInterface()
      {
         @Override
         public DataClassification classificationOf(QTableMetaData t, com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData f)
         {
            return DataClassification.PHI;
         }

         @Override
         public ProtectedAuditValues protect(QTableMetaData t, com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData f, java.io.Serializable pk, java.io.Serializable old, java.io.Serializable next, DMLAuditAction.DMLType type)
         {
            return new ProtectedAuditValues(null, "changed:v1", "Set " + f.getLabel());
         }
      });
      QRecord detail = DMLAuditAction.makeAuditDetailRecordForField("email", table, DMLAuditAction.DMLType.INSERT, new QRecord().withValue("id", 1).withValue("email", "secret"), null).orElseThrow();
      assertEquals("changed:v1", detail.getValue("newValue"));
      assertEquals("Set Email", detail.getValue("message"));
   }


   /*******************************************************************************
    ** Protection sees the caller transaction, and the outer context is restored.
    *******************************************************************************/
   @Test
   void protectionUsesCallerTransaction() throws Exception
   {
      new AuditsMetaDataProvider().defineAll(QContext.getQInstance(), TestUtils.MEMORY_BACKEND_NAME, null);
      table.setAuditRules(new QAuditRules().withAuditLevel(AuditLevel.FIELD));
      QBackendTransaction previous = new QBackendTransaction();
      QBackendTransaction transaction = new QBackendTransaction();
      QContext.setTransaction(previous);
      java.util.concurrent.atomic.AtomicBoolean called = new java.util.concurrent.atomic.AtomicBoolean();
      AuditValueProtectionMetaData.ofOrWithNew(QContext.getQInstance()).withProtectorInstance((t, f, pk, old, next, type) ->
      {
         assertSame(transaction, QContext.getQBackendTransaction());
         called.set(true);
         return new ProtectedAuditValues(null, "changed:v1", "Set " + f.getLabel());
      });
      new DMLAuditAction().execute(new DMLAuditInput().withTransaction(transaction).withTableActionInput(new InsertInput(table.getName()))
         .withRecordList(List.of(new QRecord().withValue("id", 1).withValue("firstName", "private"))));
      assertTrue(called.get());
      assertSame(previous, QContext.getQBackendTransaction());
      assertEquals(1, TestUtils.queryTable("audit").size());
   }



   /*******************************************************************************
    ** Formatting ordinary fields never populates classified display values.
    *******************************************************************************/
   @Test
   void classifiedDisplayValuesAreSkipped()
   {
      QRecord record = new QRecord().withValue("id", 1).withValue("firstName", "private").withValue("noOfShoes", 3);
      QRecord detail = DMLAuditAction.makeAuditDetailRecordForField("noOfShoes", table, DMLAuditAction.DMLType.INSERT, record, null).orElseThrow();
      assertTrue(detail.getValueString("message").contains("3"));
      assertNull(record.getDisplayValue("firstName"));
   }

}
