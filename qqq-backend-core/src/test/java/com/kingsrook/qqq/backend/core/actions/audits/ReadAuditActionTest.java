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

package com.kingsrook.qqq.backend.core.actions.audits;


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.audits.ReadAuditHandlerInput;
import com.kingsrook.qqq.backend.core.model.actions.audits.ReadAuditInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.audits.AuditsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditHandlerType;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditHandlerMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditRules;
import com.kingsrook.qqq.backend.core.model.metadata.audits.ReadAuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static com.kingsrook.qqq.backend.core.actions.audits.AuditAction.getRecordSecurityKeyValues;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for ReadAuditAction.
 *******************************************************************************/
class ReadAuditActionTest extends BaseTest
{
   private static final Integer ASYNC_WAIT_MS = 500;



   /*******************************************************************************
    ** Helper to set up audit infrastructure and insert test data.
    *******************************************************************************/
   private void setupAuditInfrastructure(ReadAuditLevel readAuditLevel) throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      new AuditsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);

      QTableMetaData table = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      if(table.getAuditRules() == null)
      {
         table.setAuditRules(new QAuditRules());
      }
      table.getAuditRules().setReadAuditLevel(readAuditLevel);

      ///////////////////////////
      // insert some test data //
      ///////////////////////////
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      insertInput.setRecords(List.of(
         new QRecord().withValue("id", 1).withValue("firstName", "Darin").withValue("lastName", "Kelkhoff"),
         new QRecord().withValue("id", 2).withValue("firstName", "Tim").withValue("lastName", "Chamberlain"),
         new QRecord().withValue("id", 3).withValue("firstName", "James").withValue("lastName", "Maes")
      ));
      new InsertAction().execute(insertInput);
   }



   /*******************************************************************************
    ** Wait for async audit to complete.
    *******************************************************************************/
   private void waitForAsyncAudit() throws InterruptedException
   {
      Thread.sleep(ASYNC_WAIT_MS);
   }



   /*******************************************************************************
    ** Test that readAuditLevel=NONE produces no audits on GET.
    *******************************************************************************/
   @Test
   void testReadAuditLevelNone_noAuditsOnGet() throws Exception
   {
      setupAuditInfrastructure(ReadAuditLevel.NONE);

      GetInput getInput = new GetInput();
      getInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      getInput.setPrimaryKey(1);
      getInput.setInputSource(QInputSource.USER);
      new GetAction().execute(getInput);

      waitForAsyncAudit();

      List<QRecord> audits = TestUtils.queryTable("audit");
      assertTrue(audits.isEmpty(), "No audits should be created when readAuditLevel is NONE");
   }



   /*******************************************************************************
    ** Test that readAuditLevel=NONE produces no audits on Query.
    *******************************************************************************/
   @Test
   void testReadAuditLevelNone_noAuditsOnQuery() throws Exception
   {
      setupAuditInfrastructure(ReadAuditLevel.NONE);

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      queryInput.setInputSource(QInputSource.USER);
      new QueryAction().execute(queryInput);

      waitForAsyncAudit();

      List<QRecord> audits = TestUtils.queryTable("audit");
      assertTrue(audits.isEmpty(), "No audits should be created when readAuditLevel is NONE");
   }



   /*******************************************************************************
    ** Test that readAuditLevel=GET produces audit on single-record GET.
    *******************************************************************************/
   @Test
   void testReadAuditLevelGet_auditsOnGet() throws Exception
   {
      setupAuditInfrastructure(ReadAuditLevel.GET);

      GetInput getInput = new GetInput();
      getInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      getInput.setPrimaryKey(1);
      getInput.setInputSource(QInputSource.USER);
      new GetAction().execute(getInput);

      waitForAsyncAudit();

      List<QRecord> audits = TestUtils.queryTable("audit");
      assertEquals(1, audits.size(), "One audit record should be created for GET");
      assertThat(audits.get(0).getValueString("message")).isEqualTo("Record was Viewed");
      assertEquals(1, audits.get(0).getValueInteger("recordId"));
   }



   /*******************************************************************************
    ** Test that readAuditLevel=GET does NOT produce audits on Query.
    *******************************************************************************/
   @Test
   void testReadAuditLevelGet_noAuditsOnQuery() throws Exception
   {
      setupAuditInfrastructure(ReadAuditLevel.GET);

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      queryInput.setInputSource(QInputSource.USER);
      new QueryAction().execute(queryInput);

      waitForAsyncAudit();

      List<QRecord> audits = TestUtils.queryTable("audit");
      assertTrue(audits.isEmpty(), "No audits should be created for Query when readAuditLevel is GET");
   }



   /*******************************************************************************
    ** Test that readAuditLevel=GET_AND_QUERY produces audits on GET.
    *******************************************************************************/
   @Test
   void testReadAuditLevelGetAndQuery_auditsOnGet() throws Exception
   {
      setupAuditInfrastructure(ReadAuditLevel.GET_AND_QUERY);

      GetInput getInput = new GetInput();
      getInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      getInput.setPrimaryKey(1);
      getInput.setInputSource(QInputSource.USER);
      new GetAction().execute(getInput);

      waitForAsyncAudit();

      List<QRecord> audits = TestUtils.queryTable("audit");
      assertEquals(1, audits.size(), "One audit record should be created for GET");
      assertThat(audits.get(0).getValueString("message")).isEqualTo("Record was Viewed");
   }



   /*******************************************************************************
    ** Test that readAuditLevel=GET_AND_QUERY produces per-record audits on Query.
    *******************************************************************************/
   @Test
   void testReadAuditLevelGetAndQuery_auditsOnQuery() throws Exception
   {
      setupAuditInfrastructure(ReadAuditLevel.GET_AND_QUERY);

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      queryInput.setInputSource(QInputSource.USER);
      new QueryAction().execute(queryInput);

      waitForAsyncAudit();

      List<QRecord> audits = TestUtils.queryTable("audit");
      assertEquals(3, audits.size(), "One audit record per queried record (3 records)");
      for(QRecord audit : audits)
      {
         assertThat(audit.getValueString("message")).isEqualTo("Record was included in Query result");
      }
   }



   /*******************************************************************************
    ** Test that SYSTEM input source does not produce read audits.
    *******************************************************************************/
   @Test
   void testSystemInputSource_noAudits() throws Exception
   {
      setupAuditInfrastructure(ReadAuditLevel.GET_AND_QUERY);

      /////////////////////////////////////////////////////////////////////
      // default InputSource is SYSTEM - should NOT produce read audits //
      /////////////////////////////////////////////////////////////////////
      GetInput getInput = new GetInput();
      getInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      getInput.setPrimaryKey(1);
      new GetAction().execute(getInput);

      waitForAsyncAudit();

      List<QRecord> audits = TestUtils.queryTable("audit");
      assertTrue(audits.isEmpty(), "System-sourced reads should not produce audits");
   }



   /*******************************************************************************
    ** Test that GET of non-existent record produces no audit.
    *******************************************************************************/
   @Test
   void testGetNonExistentRecord_noAudit() throws Exception
   {
      setupAuditInfrastructure(ReadAuditLevel.GET);

      GetInput getInput = new GetInput();
      getInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      getInput.setPrimaryKey(999);
      getInput.setInputSource(QInputSource.USER);
      new GetAction().execute(getInput);

      waitForAsyncAudit();

      List<QRecord> audits = TestUtils.queryTable("audit");
      assertTrue(audits.isEmpty(), "No audit should be created when record not found");
   }



   /*******************************************************************************
    ** Test that null readAuditLevel (default) behaves like NONE.
    *******************************************************************************/
   @Test
   void testNullReadAuditLevel_behavesAsNone() throws Exception
   {
      QInstance qInstance = QContext.getQInstance();
      new AuditsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);

      QTableMetaData table = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      table.setAuditRules(new QAuditRules());

      ///////////////////////////
      // insert some test data //
      ///////////////////////////
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      insertInput.setRecords(List.of(
         new QRecord().withValue("id", 1).withValue("firstName", "Darin").withValue("lastName", "Kelkhoff")
      ));
      new InsertAction().execute(insertInput);

      GetInput getInput = new GetInput();
      getInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      getInput.setPrimaryKey(1);
      getInput.setInputSource(QInputSource.USER);
      new GetAction().execute(getInput);

      waitForAsyncAudit();

      List<QRecord> audits = TestUtils.queryTable("audit");
      assertTrue(audits.isEmpty(), "Null readAuditLevel should behave as NONE");
   }



   /*******************************************************************************
    ** Test that no audits for tables with null audit rules.
    *******************************************************************************/
   @Test
   void testNullAuditRules_noAudits() throws Exception
   {
      QInstance qInstance = QContext.getQInstance();
      new AuditsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);

      QTableMetaData table = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      table.setAuditRules(null);

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      insertInput.setRecords(List.of(
         new QRecord().withValue("id", 1).withValue("firstName", "Darin").withValue("lastName", "Kelkhoff")
      ));
      new InsertAction().execute(insertInput);

      GetInput getInput = new GetInput();
      getInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      getInput.setPrimaryKey(1);
      getInput.setInputSource(QInputSource.USER);
      new GetAction().execute(getInput);

      waitForAsyncAudit();

      List<QRecord> audits = TestUtils.queryTable("audit");
      assertTrue(audits.isEmpty(), "Null audit rules should produce no audits");
   }



   /*******************************************************************************
    ** Test the executeInBackground method directly (synchronous path for coverage).
    *******************************************************************************/
   @Test
   void testExecuteInBackground_directCall() throws Exception
   {
      setupAuditInfrastructure(ReadAuditLevel.GET);

      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      QRecord record = new QRecord().withValue("id", 1).withValue("firstName", "Darin").withValue("lastName", "Kelkhoff");

      List<Serializable> primaryKeys = List.of(record.getValue(table.getPrimaryKeyField()));
      List<Map<String, Serializable>> securityKeyValuesList = List.of(getRecordSecurityKeyValues(table, record, Optional.empty()));

      ReadAuditAction.executeInBackground(table, primaryKeys, securityKeyValuesList, ReadAuditInput.ReadType.GET, null);

      List<QRecord> audits = TestUtils.queryTable("audit");
      assertEquals(1, audits.size());
      assertThat(audits.get(0).getValueString("message")).isEqualTo("Record was Viewed");
   }



   /*******************************************************************************
    ** Test the getReadAuditLevel helper method.
    *******************************************************************************/
   @Test
   void testGetReadAuditLevel()
   {
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);

      //////////////////////////////////////////////
      // null audit rules should return NONE      //
      //////////////////////////////////////////////
      table.setAuditRules(null);
      assertEquals(ReadAuditLevel.NONE, ReadAuditAction.getReadAuditLevel(table));

      ///////////////////////////////////////////////////
      // null readAuditLevel should default to NONE    //
      ///////////////////////////////////////////////////
      table.setAuditRules(new QAuditRules());
      assertEquals(ReadAuditLevel.NONE, ReadAuditAction.getReadAuditLevel(table));

      ///////////////////////////////////////////////////
      // explicit level should be returned as-is       //
      ///////////////////////////////////////////////////
      table.setAuditRules(new QAuditRules().withReadAuditLevel(ReadAuditLevel.GET));
      assertEquals(ReadAuditLevel.GET, ReadAuditAction.getReadAuditLevel(table));

      table.setAuditRules(new QAuditRules().withReadAuditLevel(ReadAuditLevel.GET_AND_QUERY));
      assertEquals(ReadAuditLevel.GET_AND_QUERY, ReadAuditAction.getReadAuditLevel(table));
   }



   /*******************************************************************************
    ** Test that GET read audits produce no detail records.
    *******************************************************************************/
   @Test
   void testGetReadAudit_noDetailRecords() throws Exception
   {
      setupAuditInfrastructure(ReadAuditLevel.GET_AND_QUERY);

      GetInput getInput = new GetInput();
      getInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      getInput.setPrimaryKey(1);
      getInput.setInputSource(QInputSource.USER);
      new GetAction().execute(getInput);

      waitForAsyncAudit();

      List<QRecord> audits = TestUtils.queryTable("audit");
      assertEquals(1, audits.size());

      List<QRecord> details = TestUtils.queryTable("auditDetail");
      assertTrue(details.isEmpty(), "GET read audits should not produce detail records");
   }



   /*******************************************************************************
    ** Test that QUERY read audits include the filter summary as auditDetail records.
    *******************************************************************************/
   @Test
   void testQueryReadAudit_includesFilterSummaryInAuditDetail() throws Exception
   {
      setupAuditInfrastructure(ReadAuditLevel.GET_AND_QUERY);

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      queryInput.setInputSource(QInputSource.USER);
      queryInput.setFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("firstName", QCriteriaOperator.EQUALS, "Darin")));
      new QueryAction().execute(queryInput);

      waitForAsyncAudit();

      List<QRecord> audits = TestUtils.queryTable("audit");
      assertEquals(1, audits.size(), "One audit record for the one matching record");

      List<QRecord> details = TestUtils.queryTable("auditDetail");
      assertEquals(1, details.size(), "One auditDetail record for the query filter summary");
      assertThat(details.get(0).getValueString("message")).startsWith("Query Filter:");
      assertThat(details.get(0).getValueString("message")).contains("equals");
      assertThat(details.get(0).getValueString("message")).contains("Darin");
   }



   /*******************************************************************************
    ** Test that audit failure does not propagate to the read operation.
    ** The GET should return normally even if the async audit fails.
    *******************************************************************************/
   @Test
   void testAuditFailure_doesNotPropagateToReadOperation() throws Exception
   {
      setupAuditInfrastructure(ReadAuditLevel.GET);

      //////////////////////////////////////////////////////////////////////////
      // remove the audit table after setup to force the async audit to fail //
      //////////////////////////////////////////////////////////////////////////
      QContext.getQInstance().getTables().remove(AuditsMetaDataProvider.TABLE_NAME_AUDIT);

      GetInput getInput = new GetInput();
      getInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      getInput.setPrimaryKey(1);
      getInput.setInputSource(QInputSource.USER);
      GetOutput getOutput = new GetAction().execute(getInput);

      ///////////////////////////////////////////////////////////////////////////
      // the GET should succeed - the record should be returned regardless of //
      // whether the background audit succeeds or fails                       //
      ///////////////////////////////////////////////////////////////////////////
      assertNotNull(getOutput.getRecord(), "GET should return a record even if audit will fail");
      assertEquals(1, getOutput.getRecord().getValueInteger("id"));
   }



   /*******************************************************************************
    ** Test that a READ audit handler receives events correctly.
    *******************************************************************************/
   @Test
   void testReadHandler_receivesEvents() throws Exception
   {
      setupAuditInfrastructure(ReadAuditLevel.GET);
      CapturingReadAuditHandler.capturedInputs.clear();

      /////////////////////////////////////////
      // register a READ handler for testing //
      /////////////////////////////////////////
      QContext.getQInstance().addAuditHandler(new QAuditHandlerMetaData()
         .withName("testReadHandler")
         .withHandlerCode(new QCodeReference(CapturingReadAuditHandler.class))
         .withHandlerType(AuditHandlerType.READ)
         .withIsAsync(false)
         .withEnabled(true));

      GetInput getInput = new GetInput();
      getInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      getInput.setPrimaryKey(1);
      getInput.setInputSource(QInputSource.USER);
      new GetAction().execute(getInput);

      waitForAsyncAudit();

      ////////////////////////////////////////////////
      // verify the handler was called with correct  //
      // table, readType, and record count           //
      ////////////////////////////////////////////////
      assertEquals(1, CapturingReadAuditHandler.capturedInputs.size());
      ReadAuditHandlerInput handlerInput = CapturingReadAuditHandler.capturedInputs.get(0);
      assertEquals(TestUtils.TABLE_NAME_PERSON_MEMORY, handlerInput.getTableName());
      assertEquals(ReadAuditInput.ReadType.GET, handlerInput.getReadType());
      assertEquals(1, handlerInput.getResultCount());
      assertNotNull(handlerInput.getTimestamp());
      assertNotNull(handlerInput.getSession());
   }



   /*******************************************************************************
    ** Test that reading audit system tables does not produce recursive read audits.
    ** Audit tables have no readAuditLevel configured, so reading them should not
    ** trigger further audit writes.
    *******************************************************************************/
   @Test
   void testNoRecursion_readingAuditTables() throws Exception
   {
      setupAuditInfrastructure(ReadAuditLevel.GET);

      //////////////////////////////////////////////////////////////////
      // first, do a GET to produce an audit record in the audit table //
      //////////////////////////////////////////////////////////////////
      GetInput getInput = new GetInput();
      getInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      getInput.setPrimaryKey(1);
      getInput.setInputSource(QInputSource.USER);
      new GetAction().execute(getInput);

      waitForAsyncAudit();

      List<QRecord> audits = TestUtils.queryTable("audit");
      assertEquals(1, audits.size(), "One audit from the person GET");

      //////////////////////////////////////////////////////////////////////
      // now query the audit table itself - this should NOT produce       //
      // additional audit records, since the audit table has no           //
      // readAuditLevel configured (defaults to NONE).                    //
      //////////////////////////////////////////////////////////////////////
      QueryInput auditQuery = new QueryInput();
      auditQuery.setTableName(AuditsMetaDataProvider.TABLE_NAME_AUDIT);
      auditQuery.setInputSource(QInputSource.USER);
      new QueryAction().execute(auditQuery);

      waitForAsyncAudit();

      ////////////////////////////////////////////////////////////////////
      // verify no additional audits were created from reading the      //
      // audit table. Only the original 1 audit from the person GET.   //
      ////////////////////////////////////////////////////////////////////
      List<QRecord> auditsAfter = TestUtils.queryTable("audit");
      assertEquals(1, auditsAfter.size(), "No additional audit records should be created from reading the audit table");
   }



   /*******************************************************************************
    ** Test handler for capturing read audit handler invocations.
    *******************************************************************************/
   public static class CapturingReadAuditHandler implements ReadAuditHandlerInterface
   {
      static List<ReadAuditHandlerInput> capturedInputs = new CopyOnWriteArrayList<>();



      /*******************************************************************************
       ** Getter for name
       *******************************************************************************/
      @Override
      public String getName()
      {
         return ("capturingReadAuditHandler");
      }



      /*******************************************************************************
       ** Handle read audit by capturing the input.
       *******************************************************************************/
      @Override
      public void handleReadAudit(ReadAuditHandlerInput input) throws QException
      {
         capturedInputs.add(input);
      }
   }

}
