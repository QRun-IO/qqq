/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.audits;


import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QCollectingLogger;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.audits.AuditInput;
import com.kingsrook.qqq.backend.core.model.actions.audits.AuditSingleInput;
import com.kingsrook.qqq.backend.core.model.audits.AuditsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.security.MultiRecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.processes.utils.GeneralProcessUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for AuditAction
 *******************************************************************************/
class AuditActionTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSingle() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();
      new AuditsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);

      String userName = "John Doe";
      QContext.init(qInstance, new QSession().withUser(new QUser().withFullName(userName)));

      Integer recordId = 1701;
      AuditAction.execute(TestUtils.TABLE_NAME_PERSON_MEMORY, recordId, Map.of(), "Test Audit");

      /////////////////////////////////////
      // make sure things can be fetched //
      /////////////////////////////////////
      GeneralProcessUtils.getRecordByFieldOrElseThrow("auditTable", "name", TestUtils.TABLE_NAME_PERSON_MEMORY);
      GeneralProcessUtils.getRecordByFieldOrElseThrow("auditUser", "name", userName);
      QRecord auditRecord = GeneralProcessUtils.getRecordByFieldOrElseThrow("audit", "recordId", recordId);
      assertEquals("Test Audit", auditRecord.getValueString("message"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   @Disabled("this behavior has been changed to just log... should this be a setting?")
   void testFailWithoutSecurityKey() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();
      new AuditsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);

      String userName = "John Doe";
      QContext.init(qInstance, new QSession().withUser(new QUser().withFullName(userName)));

      int               recordId         = 1701;
      QCollectingLogger collectingLogger = QLogger.activateCollectingLoggerForClass(AuditAction.class);
      AuditAction.execute(TestUtils.TABLE_NAME_ORDER, recordId, Map.of(), "Test Audit");

      ///////////////////////////////////////////////////////////////////
      // it should not throw, but it should also not insert the audit. //
      ///////////////////////////////////////////////////////////////////
      Optional<QRecord> auditRecord = GeneralProcessUtils.getRecordByField("audit", "recordId", recordId);
      // assertFalse(auditRecord.isPresent());
      assertTrue(auditRecord.isPresent());
      assertThat(collectingLogger.getCollectedMessages()).anyMatch(clm -> clm.getMessage().contains("Missing securityKeyValue"));
      QLogger.deactivateCollectingLoggerForClass(AuditAction.class);

      ////////////////////////////////////////////////////////////////////////////////////////////////
      // try again with a null value in the key - that should be ok - as at least you were thinking //
      // about the key and put in SOME value (null has its own semantics in security keys)          //
      ////////////////////////////////////////////////////////////////////////////////////////////////
      Map<String, Serializable> securityKeys = new HashMap<>();
      securityKeys.put(TestUtils.SECURITY_KEY_TYPE_STORE, null);
      AuditAction.execute(TestUtils.TABLE_NAME_ORDER, recordId, securityKeys, "Test Audit");

      /////////////////////////////////////
      // now the audit should be stored. //
      /////////////////////////////////////
      auditRecord = GeneralProcessUtils.getRecordByField("audit", "recordId", recordId);
      assertTrue(auditRecord.isPresent());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testLogWithoutSecurityKey() throws QException
   {
      int recordId = 1701;

      QInstance qInstance = TestUtils.defineInstance();
      new AuditsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);

      String userName = "John Doe";
      QContext.init(qInstance, new QSession().withUser(new QUser().withFullName(userName)));

      QCollectingLogger collectingLogger = QLogger.activateCollectingLoggerForClass(AuditAction.class);
      AuditAction.execute(TestUtils.TABLE_NAME_ORDER, recordId, Map.of(), "Test Audit");

      ///////////////////////////////////////////////////////////////////
      // it should not throw, but it should also not insert the audit. //
      ///////////////////////////////////////////////////////////////////
      Optional<QRecord> auditRecord = GeneralProcessUtils.getRecordByField("audit", "recordId", recordId);
      assertTrue(auditRecord.isPresent());

      assertThat(collectingLogger.getCollectedMessages()).anyMatch(m -> m.getMessage().contains("Missing securityKeyValue in audit request"));
      collectingLogger.clear();

      ////////////////////////////////////////////////////////////////////////////////////////////////
      // try again with a null value in the key - that should be ok - as at least you were thinking //
      // about the key and put in SOME value (null has its own semantics in security keys)          //
      ////////////////////////////////////////////////////////////////////////////////////////////////
      Map<String, Serializable> securityKeys = new HashMap<>();
      securityKeys.put(TestUtils.SECURITY_KEY_TYPE_STORE, null);
      AuditAction.execute(TestUtils.TABLE_NAME_ORDER, recordId, securityKeys, "Test Audit");

      /////////////////////////////////////
      // now the audit should be stored. //
      /////////////////////////////////////
      auditRecord = GeneralProcessUtils.getRecordByField("audit", "recordId", recordId);
      assertTrue(auditRecord.isPresent());
      assertThat(collectingLogger.getCollectedMessages()).noneMatch(m -> m.getMessage().contains("Missing securityKeyValue in audit request"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMulti() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();
      new AuditsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);

      /////////////////////////////////////////////////////
      // add a 'store' security field to the audit table //
      /////////////////////////////////////////////////////
      qInstance.getTable("audit").addField(new QFieldMetaData(TestUtils.SECURITY_KEY_TYPE_STORE, QFieldType.INTEGER));

      String userName = "John Doe";
      QContext.init(qInstance, new QSession().withUser(new QUser().withFullName(userName)));

      Integer    recordId1  = 1701;
      Integer    recordId2  = 1702;
      AuditInput auditInput = new AuditInput();
      AuditAction.appendToInput(auditInput, TestUtils.TABLE_NAME_PERSON_MEMORY, recordId1, Map.of(), "Test Audit");
      AuditAction.appendToInput(auditInput, TestUtils.TABLE_NAME_ORDER, recordId2, Map.of(TestUtils.SECURITY_KEY_TYPE_STORE, 47), "Test Another Audit");
      new AuditAction().execute(auditInput);

      /////////////////////////////////////
      // make sure things can be fetched //
      /////////////////////////////////////
      GeneralProcessUtils.getRecordByFieldOrElseThrow("auditTable", "name", TestUtils.TABLE_NAME_PERSON_MEMORY);
      GeneralProcessUtils.getRecordByFieldOrElseThrow("auditUser", "name", userName);
      QRecord auditRecord = GeneralProcessUtils.getRecordByFieldOrElseThrow("audit", "recordId", recordId1);
      assertEquals("Test Audit", auditRecord.getValueString("message"));

      auditRecord = GeneralProcessUtils.getRecordByFieldOrElseThrow("audit", "recordId", recordId2);
      assertEquals("Test Another Audit", auditRecord.getValueString("message"));
      assertEquals(47, auditRecord.getValueInteger(TestUtils.SECURITY_KEY_TYPE_STORE));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMultiWithDetails() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();
      new AuditsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);

      /////////////////////////////////////////////////////
      // add a 'store' security field to the audit table //
      /////////////////////////////////////////////////////
      qInstance.getTable("audit").addField(new QFieldMetaData(TestUtils.SECURITY_KEY_TYPE_STORE, QFieldType.INTEGER));

      String userName = "John Doe";
      QContext.init(qInstance, new QSession().withUser(new QUser().withFullName(userName)));

      Integer    recordId1  = 1701;
      Integer    recordId2  = 1702;
      Integer    recordId3  = 1703;
      AuditInput auditInput = new AuditInput();
      AuditAction.appendToInput(auditInput, TestUtils.TABLE_NAME_PERSON_MEMORY, recordId1, Map.of(), "Test Audit", List.of(new QRecord().withValue("message", "Detail1"), new QRecord().withValue("message", "Detail2")));
      AuditAction.appendToInput(auditInput, TestUtils.TABLE_NAME_ORDER, recordId2, Map.of(TestUtils.SECURITY_KEY_TYPE_STORE, 47), "Test Another Audit", null);
      AuditAction.appendToInput(auditInput, TestUtils.TABLE_NAME_PERSON_MEMORY, recordId3, Map.of(TestUtils.SECURITY_KEY_TYPE_STORE, 42), "Audit 3", List.of(new QRecord().withValue("message", "Detail3")));
      new AuditAction().execute(auditInput);

      /////////////////////////////////////
      // make sure things can be fetched //
      /////////////////////////////////////
      GeneralProcessUtils.getRecordByFieldOrElseThrow("auditTable", "name", TestUtils.TABLE_NAME_PERSON_MEMORY);
      GeneralProcessUtils.getRecordByFieldOrElseThrow("auditUser", "name", userName);
      QRecord auditRecord = GeneralProcessUtils.getRecordByFieldOrElseThrow("audit", "recordId", recordId1);
      assertEquals("Test Audit", auditRecord.getValueString("message"));

      List<QRecord> auditDetails = GeneralProcessUtils.getRecordListByField("auditDetail", "auditId", auditRecord.getValueLong("id"));
      assertEquals(2, auditDetails.size());
      assertThat(auditDetails).anyMatch(r -> r.getValueString("message").equals("Detail1"));
      assertThat(auditDetails).anyMatch(r -> r.getValueString("message").equals("Detail2"));

      auditRecord = GeneralProcessUtils.getRecordByFieldOrElseThrow("audit", "recordId", recordId2);
      assertEquals("Test Another Audit", auditRecord.getValueString("message"));
      assertEquals(47, auditRecord.getValueInteger(TestUtils.SECURITY_KEY_TYPE_STORE));
      auditDetails = GeneralProcessUtils.getRecordListByField("auditDetail", "auditId", auditRecord.getValueLong("id"));
      assertEquals(0, auditDetails.size());

      auditRecord = GeneralProcessUtils.getRecordByFieldOrElseThrow("audit", "recordId", recordId3);
      assertEquals("Audit 3", auditRecord.getValueString("message"));
      assertEquals(42, auditRecord.getValueInteger(TestUtils.SECURITY_KEY_TYPE_STORE));
      auditDetails = GeneralProcessUtils.getRecordListByField("auditDetail", "auditId", auditRecord.getValueLong("id"));
      assertEquals(1, auditDetails.size());
      assertThat(auditDetails).anyMatch(r -> r.getValueString("message").equals("Detail3"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAppendToInputThatTakesRecordNotIdAndSecurityKeyValues()
   {
      AuditInput auditInput = new AuditInput();

      //////////////////////////////////////////////////////////////
      // make sure the recordId & securityKey got build correctly //
      //////////////////////////////////////////////////////////////
      AuditAction.appendToInput(auditInput, QContext.getQInstance().getTable(TestUtils.TABLE_NAME_ORDER), new QRecord().withValue("id", 47).withValue("storeId", 42), "Test");
      AuditSingleInput auditSingleInput = auditInput.getAuditSingleInputList().get(0);
      assertEquals(47, auditSingleInput.getRecordId());
      assertEquals(MapBuilder.of(TestUtils.SECURITY_KEY_TYPE_STORE, 42), auditSingleInput.getSecurityKeyValues());

      ///////////////////////////////////////////////////////////////////////////////////////////
      // acknowledge that we might get back a null key value if the record doesn't have it set //
      ///////////////////////////////////////////////////////////////////////////////////////////
      AuditAction.appendToInput(auditInput, QContext.getQInstance().getTable(TestUtils.TABLE_NAME_ORDER), new QRecord().withValue("id", 47), "Test");
      auditSingleInput = auditInput.getAuditSingleInputList().get(1);
      assertEquals(47, auditSingleInput.getRecordId());
      assertEquals(MapBuilder.of(TestUtils.SECURITY_KEY_TYPE_STORE, null), auditSingleInput.getSecurityKeyValues());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetRecordSecurityKeyValue()
   {
      QRecord record = new QRecord().withValue("red", 1).withValue("blue", 2).withValue("green", 3).withValue("white", 4);

      RecordSecurityLock redLock        = new RecordSecurityLock().withSecurityKeyType("red").withFieldName("red");
      RecordSecurityLock blueLock       = new RecordSecurityLock().withSecurityKeyType("blue").withFieldName("blue");
      RecordSecurityLock greenLock      = new RecordSecurityLock().withSecurityKeyType("green").withFieldName("green");
      RecordSecurityLock whiteWriteLock = new RecordSecurityLock().withSecurityKeyType("green").withFieldName("white").withLockScope(RecordSecurityLock.LockScope.WRITE);

      QTableMetaData simpleLockTable = new QTableMetaData()
         .withRecordSecurityLock(redLock);
      assertEquals(Map.of("red", 1), AuditAction.getRecordSecurityKeyValues(simpleLockTable, record, Optional.empty()));

      QTableMetaData writeOnlyLockTable = new QTableMetaData()
         .withRecordSecurityLock(whiteWriteLock);
      assertEquals(Collections.emptyMap(), AuditAction.getRecordSecurityKeyValues(writeOnlyLockTable, record, Optional.empty()));

      QTableMetaData multiAndLockTable = new QTableMetaData()
         .withRecordSecurityLock(new MultiRecordSecurityLock()
            .withOperator(MultiRecordSecurityLock.BooleanOperator.AND)
            .withLock(redLock)
            .withLock(blueLock));
      assertEquals(Map.of("red", 1, "blue", 2), AuditAction.getRecordSecurityKeyValues(multiAndLockTable, record, Optional.empty()));

      QTableMetaData multiOrLockTable = new QTableMetaData()
         .withRecordSecurityLock(new MultiRecordSecurityLock()
            .withOperator(MultiRecordSecurityLock.BooleanOperator.OR)
            .withLock(redLock)
            .withLock(blueLock));
      assertEquals(Map.of("red", 1, "blue", 2), AuditAction.getRecordSecurityKeyValues(multiOrLockTable, record, Optional.empty()));

      QTableMetaData multiLevelLockTable = new QTableMetaData()
         .withRecordSecurityLock(new MultiRecordSecurityLock()
            .withOperator(MultiRecordSecurityLock.BooleanOperator.AND)
            .withLock(redLock)
            .withLock(whiteWriteLock)
            .withLock(new MultiRecordSecurityLock()
               .withOperator(MultiRecordSecurityLock.BooleanOperator.OR)
               .withLock(blueLock)
               .withLock(greenLock)));
      assertEquals(Map.of("red", 1, "blue", 2, "green", 3), AuditAction.getRecordSecurityKeyValues(multiLevelLockTable, record, Optional.empty()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testValidateSecurityKeys()
   {
      RecordSecurityLock redLock        = new RecordSecurityLock().withSecurityKeyType("red").withFieldName("red");
      RecordSecurityLock blueLock       = new RecordSecurityLock().withSecurityKeyType("blue").withFieldName("blue");
      RecordSecurityLock greenLock      = new RecordSecurityLock().withSecurityKeyType("green").withFieldName("green");
      RecordSecurityLock whiteWriteLock = new RecordSecurityLock().withSecurityKeyType("green").withFieldName("white").withLockScope(RecordSecurityLock.LockScope.WRITE);

      AuditSingleInput inputWithNoKeys         = new AuditSingleInput().withSecurityKeyValues(Collections.emptyMap());
      AuditSingleInput inputWithRedKey         = new AuditSingleInput().withSecurityKeyValues(Map.of("red", 1));
      AuditSingleInput inputWithBlueKey        = new AuditSingleInput().withSecurityKeyValues(Map.of("blue", 2));
      AuditSingleInput inputWithRedAndBlueKeys = new AuditSingleInput().withSecurityKeyValues(Map.of("red", 1, "blue", 2));

      QTableMetaData noLockTable = new QTableMetaData();
      assertTrue(AuditAction.validateSecurityKeys(inputWithNoKeys, noLockTable));
      assertTrue(AuditAction.validateSecurityKeys(inputWithRedKey, noLockTable));

      QTableMetaData simpleLockTable = new QTableMetaData()
         .withRecordSecurityLock(redLock);
      assertFalse(AuditAction.validateSecurityKeys(inputWithNoKeys, simpleLockTable));
      assertTrue(AuditAction.validateSecurityKeys(inputWithRedKey, simpleLockTable));
      assertFalse(AuditAction.validateSecurityKeys(inputWithBlueKey, simpleLockTable));

      QTableMetaData writeOnlyLockTable = new QTableMetaData()
         .withRecordSecurityLock(whiteWriteLock);
      assertTrue(AuditAction.validateSecurityKeys(inputWithNoKeys, writeOnlyLockTable));
      assertTrue(AuditAction.validateSecurityKeys(inputWithRedKey, writeOnlyLockTable));

      QTableMetaData multiAndLockTable = new QTableMetaData()
         .withRecordSecurityLock(new MultiRecordSecurityLock()
            .withOperator(MultiRecordSecurityLock.BooleanOperator.AND)
            .withLock(redLock)
            .withLock(blueLock));
      assertFalse(AuditAction.validateSecurityKeys(inputWithNoKeys, multiAndLockTable));
      assertFalse(AuditAction.validateSecurityKeys(inputWithRedKey, multiAndLockTable));
      assertTrue(AuditAction.validateSecurityKeys(inputWithRedAndBlueKeys, multiAndLockTable));

      QTableMetaData multiOrLockTable = new QTableMetaData()
         .withRecordSecurityLock(new MultiRecordSecurityLock()
            .withOperator(MultiRecordSecurityLock.BooleanOperator.OR)
            .withLock(redLock)
            .withLock(blueLock));
      assertFalse(AuditAction.validateSecurityKeys(inputWithNoKeys, multiOrLockTable));
      assertTrue(AuditAction.validateSecurityKeys(inputWithRedKey, multiOrLockTable));
      assertTrue(AuditAction.validateSecurityKeys(inputWithRedAndBlueKeys, multiOrLockTable));

      QTableMetaData multiLevelLockTable = new QTableMetaData()
         .withRecordSecurityLock(new MultiRecordSecurityLock()
            .withOperator(MultiRecordSecurityLock.BooleanOperator.AND)
            .withLock(redLock)
            .withLock(whiteWriteLock)
            .withLock(new MultiRecordSecurityLock()
               .withOperator(MultiRecordSecurityLock.BooleanOperator.OR)
               .withLock(blueLock)
               .withLock(greenLock)));
      assertFalse(AuditAction.validateSecurityKeys(inputWithNoKeys, multiLevelLockTable));
      assertFalse(AuditAction.validateSecurityKeys(inputWithRedKey, multiLevelLockTable));
      assertTrue(AuditAction.validateSecurityKeys(inputWithRedAndBlueKeys, multiLevelLockTable));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCustomizer() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();
      new AuditsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);
      qInstance.addSupplementalCustomizer(AuditActionCustomizerInterface.CUSTOMIZER_TYPE, new QCodeReference(TestAuditActionCustomizer.class));
      qInstance.getTable(AuditsMetaDataProvider.TABLE_NAME_AUDIT)
         .addField(new QFieldMetaData("customField", QFieldType.INTEGER));

      String userName = "John Doe";
      QContext.init(qInstance, new QSession().withUser(new QUser().withFullName(userName)));

      Integer recordId = 1701;
      AuditAction.execute(TestUtils.TABLE_NAME_PERSON_MEMORY, recordId, Map.of(), "Test Audit");

      /////////////////////////////////////
      // make sure things can be fetched //
      /////////////////////////////////////
      GeneralProcessUtils.getRecordByFieldOrElseThrow("auditTable", "name", TestUtils.TABLE_NAME_PERSON_MEMORY);
      assertThatThrownBy(() -> GeneralProcessUtils.getRecordByFieldOrElseThrow("auditUser", "name", userName));
      QRecord auditRecord = GeneralProcessUtils.getRecordByFieldOrElseThrow("audit", "recordId", recordId);
      assertEquals("Test Audit customized!", auditRecord.getValueString("message"));
      Integer auditUserId = auditRecord.getValueInteger("auditUserId");
      assertEquals("Q(not)Anon", GeneralProcessUtils.getRecordByFieldOrElseThrow("auditUser", "id", auditUserId).getValueString("name"));
      assertEquals(42, auditRecord.getValueInteger("customField"));
   }



   /***************************************************************************
    *
    ***************************************************************************/
   public static class TestAuditActionCustomizer implements AuditActionCustomizerInterface
   {
      /***************************************************************************
       *
       ***************************************************************************/
      @Override
      public void customizeInput(AuditSingleInput auditSingleInput)
      {
         auditSingleInput.setAuditUserName("Q(not)Anon");
         auditSingleInput.setMessage(auditSingleInput.getMessage() + " customized!");
      }



      /***************************************************************************
       *
       ***************************************************************************/
      @Override
      public void customizeRecord(QRecord auditRecord, AuditSingleInput auditSingleInput)
      {
         auditRecord.setValue("customField", 42);
      }
   }
}
