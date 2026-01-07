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


import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.audits.DMLAuditInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.audits.AuditsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditRules;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import org.junit.jupiter.api.Test;
import static com.kingsrook.qqq.backend.core.actions.audits.DMLAuditAction.DMLType.INSERT;
import static com.kingsrook.qqq.backend.core.actions.audits.DMLAuditAction.DMLType.UPDATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for DMLAuditAction
 *******************************************************************************/
class DMLAuditActionTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      new AuditsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);

      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);

      DeleteInput deleteInput = new DeleteInput();
      deleteInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);

      List<QRecord> recordList    = List.of(new QRecord().withValue("id", 1).withValue("firstName", "Darin").withValue("noOfShoes", 5).withValue("favoriteShapeId", null).withValue("createDate", Instant.now()).withValue("modifyDate", Instant.now()));
      List<QRecord> oldRecordList = List.of(new QRecord().withValue("id", 1).withValue("firstName", "Tim").withValue("noOfShoes", null).withValue("favoriteShapeId", 1).withValue("lastName", "Simpson"));

      ////////////////////////////////////////////////////////
      // set audit rules null - confirm no audits are built //
      ////////////////////////////////////////////////////////
      {
         qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).setAuditRules(null);
         new DMLAuditAction().execute(new DMLAuditInput().withTableActionInput(deleteInput).withRecordList(recordList));
         List<QRecord> auditList = TestUtils.queryTable("audit");
         assertTrue(auditList.isEmpty());
      }

      ////////////////////////////////////////////////////////
      // set audit level NONE - confirm no audits are built //
      ////////////////////////////////////////////////////////
      {
         qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).setAuditRules(new QAuditRules().withAuditLevel(AuditLevel.NONE));
         new DMLAuditAction().execute(new DMLAuditInput().withTableActionInput(deleteInput).withRecordList(recordList));
         List<QRecord> auditList = TestUtils.queryTable("audit");
         assertTrue(auditList.isEmpty());
      }

      /////////////////////////////////////////////////////////////
      // set audit level RECORD - confirm only header, no detail //
      /////////////////////////////////////////////////////////////
      {
         qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).setAuditRules(new QAuditRules().withAuditLevel(AuditLevel.RECORD));
         new DMLAuditAction().execute(new DMLAuditInput().withTableActionInput(insertInput).withRecordList(recordList));
         List<QRecord> auditList = TestUtils.queryTable("audit");
         assertEquals(1, auditList.size());
         assertEquals("Record was Inserted", auditList.get(0).getValueString("message"));
         List<QRecord> auditDetailList = TestUtils.queryTable("auditDetail");
         assertTrue(auditDetailList.isEmpty());
         MemoryRecordStore.getInstance().reset();
      }

      ////////////////////////////////////////////////////////
      // set audit level FIELD - confirm header, and detail //
      ////////////////////////////////////////////////////////
      {
         qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).setAuditRules(new QAuditRules().withAuditLevel(AuditLevel.FIELD));
         new DMLAuditAction().execute(new DMLAuditInput().withTableActionInput(updateInput).withRecordList(recordList));
         List<QRecord> auditList = TestUtils.queryTable("audit");
         assertEquals(1, auditList.size());
         assertEquals("Record was Edited", auditList.get(0).getValueString("message"));
         List<QRecord> auditDetailList = TestUtils.queryTable("auditDetail");

         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // since we didn't provide old-records, there should be a detail for every field in the updated record - OTHER THAN createDate and modifyDate //
         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         assertEquals(recordList.get(0).getValues().size() - 2, auditDetailList.size());
         assertTrue(auditDetailList.stream().allMatch(r -> r.getValueString("message").matches("Set.*to.*")));
         assertTrue(auditDetailList.stream().allMatch(r -> r.getValueString("fieldName") != null));
         assertTrue(auditDetailList.stream().allMatch(r -> r.getValueString("oldValue") == null));
         assertTrue(auditDetailList.stream().anyMatch(r -> r.getValueString("newValue") != null));
         MemoryRecordStore.getInstance().reset();
      }

      //////////////////////////////////////////////
      // this time supply old-records to the edit //
      //////////////////////////////////////////////
      {
         qInstance.getTable(TestUtils.TABLE_NAME_SHAPE).setAuditRules(new QAuditRules().withAuditLevel(AuditLevel.NONE));
         TestUtils.insertDefaultShapes(qInstance);

         qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).setAuditRules(new QAuditRules().withAuditLevel(AuditLevel.FIELD));
         new DMLAuditAction().execute(new DMLAuditInput().withTableActionInput(updateInput).withOldRecordList(oldRecordList).withRecordList(recordList));
         List<QRecord> auditList = TestUtils.queryTable("audit");
         assertEquals(1, auditList.size());
         assertEquals("Record was Edited", auditList.get(0).getValueString("message"));
         List<QRecord> auditDetailList = TestUtils.queryTable("auditDetail");

         assertEquals(3, auditDetailList.size());

         assertEquals("Removed \"Triangle\" from Favorite Shape", auditDetailList.get(0).getValueString("message"));
         assertEquals("favoriteShapeId", auditDetailList.get(0).getValueString("fieldName"));
         assertEquals("Triangle", auditDetailList.get(0).getValueString("oldValue"));
         assertNull(auditDetailList.get(0).getValueString("newValue"));

         assertEquals("Changed First Name from \"Tim\" to \"Darin\"", auditDetailList.get(1).getValueString("message"));
         assertEquals("firstName", auditDetailList.get(1).getValueString("fieldName"));
         assertEquals("Tim", auditDetailList.get(1).getValueString("oldValue"));
         assertEquals("Darin", auditDetailList.get(1).getValueString("newValue"));

         assertEquals("Set No Of Shoes to 5", auditDetailList.get(2).getValueString("message"));
         assertEquals("noOfShoes", auditDetailList.get(2).getValueString("fieldName"));
         assertNull(auditDetailList.get(2).getValueString("oldValue"));
         assertEquals("5", auditDetailList.get(2).getValueString("newValue"));

         MemoryRecordStore.getInstance().reset();
      }

      /////////////////////////////////////////////////
      // confirm we don't log null fields on inserts //
      /////////////////////////////////////////////////
      {
         qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).setAuditRules(new QAuditRules().withAuditLevel(AuditLevel.FIELD));
         new DMLAuditAction().execute(new DMLAuditInput().withTableActionInput(insertInput).withRecordList(recordList));
         List<QRecord> auditDetailList = TestUtils.queryTable("auditDetail");
         assertFalse(auditDetailList.isEmpty());
         assertTrue(auditDetailList.stream().noneMatch(r -> r.getValueString("message").contains("Favorite Shape")));
         MemoryRecordStore.getInstance().reset();
      }

      ///////////////////////////////////////////////////////////
      // confirm if nothing changed on an edit, that no audit. //
      ///////////////////////////////////////////////////////////
      {
         qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).setAuditRules(new QAuditRules().withAuditLevel(AuditLevel.FIELD));
         new DMLAuditAction().execute(new DMLAuditInput().withTableActionInput(updateInput).withRecordList(recordList).withOldRecordList(recordList));
         List<QRecord> auditList = TestUtils.queryTable("audit");
         assertEquals(0, auditList.size());
         MemoryRecordStore.getInstance().reset();
      }

      ////////////////////////////////////////////////////
      // confirm we don't audit for records with errors //
      ////////////////////////////////////////////////////
      {
         qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).setAuditRules(new QAuditRules().withAuditLevel(AuditLevel.FIELD));
         new DMLAuditAction().execute(new DMLAuditInput().withTableActionInput(updateInput)
            .withRecordList(List.of(new QRecord().withValue("id", 1).withValue("firstName", "B").withError(new BadInputStatusMessage("Error"))))
            .withOldRecordList(List.of(new QRecord().withValue("id", 1).withValue("firstName", "A"))));
         List<QRecord> auditList = TestUtils.queryTable("audit");
         assertEquals(0, auditList.size());
         MemoryRecordStore.getInstance().reset();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMakeAuditDetailRecordForField()
   {
      QTableMetaData table = new QTableMetaData()
         .withField(new QFieldMetaData("createDate", QFieldType.DATE_TIME).withLabel("Create Date"))
         .withField(new QFieldMetaData("modifyDate", QFieldType.DATE_TIME).withLabel("Modify Date"))
         .withField(new QFieldMetaData("someTimestamp", QFieldType.DATE_TIME).withLabel("Some Timestamp"))
         .withField(new QFieldMetaData("name", QFieldType.STRING).withLabel("Name"))
         .withField(new QFieldMetaData("seqNo", QFieldType.INTEGER).withLabel("Sequence No."))
         .withField(new QFieldMetaData("price", QFieldType.DECIMAL).withLabel("Price"));

      ///////////////////////////////
      // create date - never audit //
      ///////////////////////////////
      assertThat(DMLAuditAction.makeAuditDetailRecordForField("createDate", table, INSERT,
         new QRecord().withValue("createDate", Instant.now()),
         new QRecord().withValue("createDate", Instant.now().minusSeconds(100))))
         .isEmpty();

      ///////////////////////////////
      // modify date - never audit //
      ///////////////////////////////
      assertThat(DMLAuditAction.makeAuditDetailRecordForField("modifyDate", table, UPDATE,
         new QRecord().withValue("modifyDate", Instant.now()),
         new QRecord().withValue("modifyDate", Instant.now().minusSeconds(100))))
         .isEmpty();

      ////////////////////////////////////////////////////////
      // datetime different only in precision - don't audit //
      ////////////////////////////////////////////////////////
      assertThat(DMLAuditAction.makeAuditDetailRecordForField("someTimestamp", table, UPDATE,
         new QRecord().withValue("someTimestamp", ValueUtils.getValueAsInstant("2023-04-17T14:33:08.777")),
         new QRecord().withValue("someTimestamp", Instant.parse("2023-04-17T14:33:08Z"))))
         .isEmpty();

      /////////////////////////////////////////////
      // datetime actually different - audit it. //
      /////////////////////////////////////////////
      assertThat(DMLAuditAction.makeAuditDetailRecordForField("someTimestamp", table, UPDATE,
         new QRecord().withValue("someTimestamp", Instant.parse("2023-04-17T14:33:09Z")),
         new QRecord().withValue("someTimestamp", Instant.parse("2023-04-17T14:33:08Z"))))
         .isPresent()
         .get().extracting(r -> r.getValueString("message"))
         .matches(s -> s.matches("Changed Some Timestamp from 2023.* to 2023.*"));

      ////////////////////////////////////////////////
      // datetime changing null to not null - audit //
      ////////////////////////////////////////////////
      assertThat(DMLAuditAction.makeAuditDetailRecordForField("someTimestamp", table, UPDATE,
         new QRecord().withValue("someTimestamp", ValueUtils.getValueAsInstant("2023-04-17T14:33:08.777")),
         new QRecord().withValue("someTimestamp", null)))
         .isPresent()
         .get().extracting(r -> r.getValueString("message"))
         .matches(s -> s.matches("Set Some Timestamp to 2023.*"));

      ////////////////////////////////////////////////
      // datetime changing not null to null - audit //
      ////////////////////////////////////////////////
      assertThat(DMLAuditAction.makeAuditDetailRecordForField("someTimestamp", table, UPDATE,
         new QRecord().withValue("someTimestamp", null),
         new QRecord().withValue("someTimestamp", Instant.parse("2023-04-17T14:33:08Z"))))
         .isPresent()
         .get().extracting(r -> r.getValueString("message"))
         .matches(s -> s.matches("Removed 2023.*from Some Timestamp"));

      ////////////////////////////////////////
      // string that is the same - no audit //
      ////////////////////////////////////////
      assertThat(DMLAuditAction.makeAuditDetailRecordForField("name", table, UPDATE,
         new QRecord().withValue("name", "Homer"),
         new QRecord().withValue("name", "Homer")))
         .isEmpty();

      //////////////////////////////////////////
      // string from null to empty - no audit //
      //////////////////////////////////////////
      assertThat(DMLAuditAction.makeAuditDetailRecordForField("name", table, UPDATE,
         new QRecord().withValue("name", null),
         new QRecord().withValue("name", "")))
         .isEmpty();

      //////////////////////////////////////////
      // string from empty to null - no audit //
      //////////////////////////////////////////
      assertThat(DMLAuditAction.makeAuditDetailRecordForField("name", table, UPDATE,
         new QRecord().withValue("name", ""),
         new QRecord().withValue("name", null)))
         .isEmpty();

      //////////////////////////////////////////////////////////
      // decimal that only changes in precision - don't audit //
      //////////////////////////////////////////////////////////
      assertThat(DMLAuditAction.makeAuditDetailRecordForField("price", table, UPDATE,
         new QRecord().withValue("price", "10"),
         new QRecord().withValue("price", new BigDecimal("10.00"))))
         .isEmpty();

      //////////////////////////////////////////////////
      // decimal that's actually different - do audit //
      //////////////////////////////////////////////////
      assertThat(DMLAuditAction.makeAuditDetailRecordForField("price", table, UPDATE,
         new QRecord().withValue("price", "10.01"),
         new QRecord().withValue("price", new BigDecimal("10.00"))))
         .isPresent()
         .get().extracting(r -> r.getValueString("message"))
         .matches(s -> s.matches("Changed Price from 10.00 to 10.01"));

      ///////////////////////////////////////
      // decimal null, input "" - no audit //
      ///////////////////////////////////////
      assertThat(DMLAuditAction.makeAuditDetailRecordForField("price", table, UPDATE,
         new QRecord().withValue("price", ""),
         new QRecord().withValue("price", null)))
         .isEmpty();

      /////////////////////////////////////////
      // decimal not-null to null - do audit //
      /////////////////////////////////////////
      assertThat(DMLAuditAction.makeAuditDetailRecordForField("price", table, UPDATE,
         new QRecord().withValue("price", BigDecimal.ONE),
         new QRecord().withValue("price", null)))
         .isPresent()
         .get().extracting(r -> r.getValueString("message"))
         .matches(s -> s.matches("Set Price to 1"));

      /////////////////////////////////////////
      // decimal null to not-null - do audit //
      /////////////////////////////////////////
      assertThat(DMLAuditAction.makeAuditDetailRecordForField("price", table, UPDATE,
         new QRecord().withValue("price", null),
         new QRecord().withValue("price", BigDecimal.ONE)))
         .isPresent()
         .get().extracting(r -> r.getValueString("message"))
         .matches(s -> s.matches("Removed 1 from Price"));

      ///////////////////////////////////////
      // integer null, input "" - no audit //
      ///////////////////////////////////////
      assertThat(DMLAuditAction.makeAuditDetailRecordForField("seqNo", table, UPDATE,
         new QRecord().withValue("seqNo", ""),
         new QRecord().withValue("seqNo", null)))
         .isEmpty();

      ////////////////////////////////
      // integer changed - do audit //
      ////////////////////////////////
      assertThat(DMLAuditAction.makeAuditDetailRecordForField("seqNo", table, UPDATE,
         new QRecord().withValue("seqNo", 2),
         new QRecord().withValue("seqNo", 1)))
         .isPresent()
         .get().extracting(r -> r.getValueString("message"))
         .matches(s -> s.matches("Changed Sequence No. from 1 to 2"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetContextSuffix()
   {
      assertEquals("", DMLAuditAction.getContentSuffix(new DMLAuditInput()));
      assertEquals(" while shipping an order", DMLAuditAction.getContentSuffix(new DMLAuditInput().withAuditContext("while shipping an order")));

      QContext.pushAction(new RunProcessInput().withValue(DMLAuditAction.AUDIT_CONTEXT_FIELD_NAME, "via Script \"My Script\""));
      assertEquals(" via Script \"My Script\"", DMLAuditAction.getContentSuffix(new DMLAuditInput()));
      QContext.popAction();

      QContext.pushAction(new RunProcessInput().withProcessName(TestUtils.PROCESS_NAME_GREET_PEOPLE));
      assertEquals(" during process: Greet", DMLAuditAction.getContentSuffix(new DMLAuditInput()));
      QContext.popAction();

      QContext.setQSession(new QSession().withValue("apiVersion", "1.0"));
      assertEquals(" via API Version: 1.0", DMLAuditAction.getContentSuffix(new DMLAuditInput()));

      QContext.setQSession(new QSession().withValue("apiVersion", "20230921").withValue("apiLabel", "Our Public API"));
      assertEquals(" via Our Public API Version: 20230921", DMLAuditAction.getContentSuffix(new DMLAuditInput()));

      QContext.pushAction(new RunProcessInput().withProcessName(TestUtils.PROCESS_NAME_GREET_PEOPLE).withValue(DMLAuditAction.AUDIT_CONTEXT_FIELD_NAME, "via Script \"My Script\""));
      QContext.setQSession(new QSession().withValue("apiVersion", "20230921").withValue("apiLabel", "Our Public API"));
      assertEquals(" while shipping an order via Script \"My Script\" during process: Greet via Our Public API Version: 20230921", DMLAuditAction.getContentSuffix(new DMLAuditInput().withAuditContext("while shipping an order")));
      QContext.popAction();
   }



   /*******************************************************************************
    ** Test that tables without INTEGER primary keys are NOT audited when using
    ** the default INTEGER recordId configuration (backwards compatibility).
    *******************************************************************************/
   @Test
   void testTableWithoutIntegerPrimaryKey() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      new AuditsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);

      ////////////////////////////////////////////////////////////////////////////////////////////////////
      // we used to throw if table had no primary key.  first, assert that we do not throw in that case //
      ////////////////////////////////////////////////////////////////////////////////////////////////////
      QContext.getQInstance().addTable(
         new QTableMetaData()
            .withName("nullPkey")
            .withField(new QFieldMetaData("foo", QFieldType.STRING))
            .withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.FIELD)));

      new DMLAuditAction().execute(new DMLAuditInput()
         .withTableActionInput(new InsertInput("nullPkey"))
         .withRecordList(List.of(new QRecord())));

      //////////////////////////////////////////////////////////////////////////////////////////////
      // next, make sure we don't throw (and don't record anything) if table's pkey isn't integer //
      //////////////////////////////////////////////////////////////////////////////////////////////
      QContext.getQInstance().addTable(
         new QTableMetaData()
            .withName("stringPkey")
            .withField(new QFieldMetaData("idString", QFieldType.STRING))
            .withPrimaryKeyField("idString")
            .withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.FIELD)));

      new DMLAuditAction().execute(new DMLAuditInput()
         .withTableActionInput(new InsertInput("stringPkey"))
         .withRecordList(List.of(new QRecord())));

      //////////////////////////////////
      // make sure no audits happened //
      //////////////////////////////////
      List<QRecord> auditList = TestUtils.queryTable("audit");
      assertTrue(auditList.isEmpty());
   }



   /*******************************************************************************
    ** Test that tables with STRING primary keys CAN be audited when using
    ** the STRING recordId configuration.
    *******************************************************************************/
   @Test
   void testStringRecordIdConfigurationWithStringPrimaryKey() throws QException
   {
      QInstance qInstance = QContext.getQInstance();

      /////////////////////////////////////////////////////////////////////////
      // configure audit tables with STRING recordId type instead of INTEGER //
      /////////////////////////////////////////////////////////////////////////
      new AuditsMetaDataProvider()
         .withRecordIdType(QFieldType.STRING)
         .defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);

      ////////////////////////////////////////
      // add a table with STRING primary key //
      ////////////////////////////////////////
      qInstance.addTable(
         new QTableMetaData()
            .withName("stringPkeyTable")
            .withBackendName(TestUtils.MEMORY_BACKEND_NAME)
            .withField(new QFieldMetaData("uuid", QFieldType.STRING))
            .withField(new QFieldMetaData("name", QFieldType.STRING).withLabel("Name"))
            .withPrimaryKeyField("uuid")
            .withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.FIELD)));

      ////////////////////////////////////////
      // execute an insert and verify audit //
      ////////////////////////////////////////
      String testUuid = "abc-123-def-456";
      new DMLAuditAction().execute(new DMLAuditInput()
         .withTableActionInput(new InsertInput("stringPkeyTable"))
         .withRecordList(List.of(new QRecord()
            .withValue("uuid", testUuid)
            .withValue("name", "Test Record"))));

      ////////////////////////////////////////////////
      // verify an audit was created with STRING id //
      ////////////////////////////////////////////////
      List<QRecord> auditList = TestUtils.queryTable("audit");
      assertEquals(1, auditList.size());
      assertEquals("Record was Inserted", auditList.get(0).getValueString("message"));
      assertEquals(testUuid, auditList.get(0).getValueString("recordId"));

      /////////////////////////////////////
      // verify audit details were saved //
      /////////////////////////////////////
      List<QRecord> auditDetailList = TestUtils.queryTable("auditDetail");
      assertFalse(auditDetailList.isEmpty());
      assertTrue(auditDetailList.stream().anyMatch(r -> "name".equals(r.getValueString("fieldName"))));
   }



   /*******************************************************************************
    ** Test that tables with INTEGER primary keys can STILL be audited when using
    ** the STRING recordId configuration (integers are converted to strings).
    *******************************************************************************/
   @Test
   void testStringRecordIdConfigurationWithIntegerPrimaryKey() throws QException
   {
      QInstance qInstance = QContext.getQInstance();

      /////////////////////////////////////////////////////////////////////////
      // configure audit tables with STRING recordId type instead of INTEGER //
      /////////////////////////////////////////////////////////////////////////
      new AuditsMetaDataProvider()
         .withRecordIdType(QFieldType.STRING)
         .defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);

      ///////////////////////////////////////////////
      // use the standard person table (INTEGER PK) //
      ///////////////////////////////////////////////
      qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .setAuditRules(new QAuditRules().withAuditLevel(AuditLevel.FIELD));

      ////////////////////////////////////////
      // execute an insert and verify audit //
      ////////////////////////////////////////
      Integer testId = 42;
      new DMLAuditAction().execute(new DMLAuditInput()
         .withTableActionInput(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY))
         .withRecordList(List.of(new QRecord()
            .withValue("id", testId)
            .withValue("firstName", "John"))));

      ////////////////////////////////////////////////////////
      // verify an audit was created with ID stored as text //
      ////////////////////////////////////////////////////////
      List<QRecord> auditList = TestUtils.queryTable("audit");
      assertEquals(1, auditList.size());
      assertEquals("Record was Inserted", auditList.get(0).getValueString("message"));
      assertEquals("42", auditList.get(0).getValueString("recordId"));
   }



   /*******************************************************************************
    ** Test that edits (updates) work correctly with STRING recordId configuration.
    *******************************************************************************/
   @Test
   void testStringRecordIdConfigurationWithUpdate() throws QException
   {
      QInstance qInstance = QContext.getQInstance();

      /////////////////////////////////////////////////////////////////////////
      // configure audit tables with STRING recordId type instead of INTEGER //
      /////////////////////////////////////////////////////////////////////////
      new AuditsMetaDataProvider()
         .withRecordIdType(QFieldType.STRING)
         .defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);

      ////////////////////////////////////////
      // add a table with STRING primary key //
      ////////////////////////////////////////
      qInstance.addTable(
         new QTableMetaData()
            .withName("stringPkeyTable")
            .withBackendName(TestUtils.MEMORY_BACKEND_NAME)
            .withField(new QFieldMetaData("uuid", QFieldType.STRING))
            .withField(new QFieldMetaData("name", QFieldType.STRING).withLabel("Name"))
            .withField(new QFieldMetaData("status", QFieldType.STRING).withLabel("Status"))
            .withPrimaryKeyField("uuid")
            .withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.FIELD)));

      ////////////////////////////////////////
      // execute an update and verify audit //
      ////////////////////////////////////////
      String testUuid = "uuid-for-update-test";
      QRecord oldRecord = new QRecord()
         .withValue("uuid", testUuid)
         .withValue("name", "Original Name")
         .withValue("status", "PENDING");
      QRecord newRecord = new QRecord()
         .withValue("uuid", testUuid)
         .withValue("name", "Updated Name")
         .withValue("status", "PENDING");

      new DMLAuditAction().execute(new DMLAuditInput()
         .withTableActionInput(new UpdateInput("stringPkeyTable"))
         .withOldRecordList(List.of(oldRecord))
         .withRecordList(List.of(newRecord)));

      ///////////////////////////////////////////////////
      // verify an audit was created for the edit      //
      ///////////////////////////////////////////////////
      List<QRecord> auditList = TestUtils.queryTable("audit");
      assertEquals(1, auditList.size());
      assertEquals("Record was Edited", auditList.get(0).getValueString("message"));
      assertEquals(testUuid, auditList.get(0).getValueString("recordId"));

      //////////////////////////////////////////////
      // verify audit detail captured the change  //
      //////////////////////////////////////////////
      List<QRecord> auditDetailList = TestUtils.queryTable("auditDetail");
      assertEquals(1, auditDetailList.size());
      assertEquals("name", auditDetailList.get(0).getValueString("fieldName"));
      assertEquals("Original Name", auditDetailList.get(0).getValueString("oldValue"));
      assertEquals("Updated Name", auditDetailList.get(0).getValueString("newValue"));
   }



   /*******************************************************************************
    ** Test that deletes work correctly with STRING recordId configuration.
    *******************************************************************************/
   @Test
   void testStringRecordIdConfigurationWithDelete() throws QException
   {
      QInstance qInstance = QContext.getQInstance();

      /////////////////////////////////////////////////////////////////////////
      // configure audit tables with STRING recordId type instead of INTEGER //
      /////////////////////////////////////////////////////////////////////////
      new AuditsMetaDataProvider()
         .withRecordIdType(QFieldType.STRING)
         .defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);

      ////////////////////////////////////////
      // add a table with STRING primary key //
      ////////////////////////////////////////
      qInstance.addTable(
         new QTableMetaData()
            .withName("stringPkeyTable")
            .withBackendName(TestUtils.MEMORY_BACKEND_NAME)
            .withField(new QFieldMetaData("uuid", QFieldType.STRING))
            .withField(new QFieldMetaData("name", QFieldType.STRING).withLabel("Name"))
            .withPrimaryKeyField("uuid")
            .withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.RECORD)));

      ////////////////////////////////////////
      // execute a delete and verify audit  //
      ////////////////////////////////////////
      String testUuid = "uuid-for-delete-test";
      new DMLAuditAction().execute(new DMLAuditInput()
         .withTableActionInput(new DeleteInput("stringPkeyTable"))
         .withRecordList(List.of(new QRecord()
            .withValue("uuid", testUuid)
            .withValue("name", "Record To Delete"))));

      ///////////////////////////////////////////////////
      // verify an audit was created for the delete    //
      ///////////////////////////////////////////////////
      List<QRecord> auditList = TestUtils.queryTable("audit");
      assertEquals(1, auditList.size());
      assertEquals("Record was Deleted", auditList.get(0).getValueString("message"));
      assertEquals(testUuid, auditList.get(0).getValueString("recordId"));
   }

}
