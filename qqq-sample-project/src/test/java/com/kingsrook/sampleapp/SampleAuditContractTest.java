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

package com.kingsrook.sampleapp;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.actions.audits.AuditAction;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.audits.AuditsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditRules;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.processes.implementations.audits.GetAuditsForRecordProcess;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Record audit acceptance through the sample's real H2-backed Person table.
 *******************************************************************************/
class SampleAuditContractTest
{
   private static final String PERSON = SampleMetaDataProvider.TABLE_NAME_PERSON;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      MemoryRecordStore.getInstance().reset();
      QInstance instance = SampleMetaDataProvider.defineTestInstance();
      new AuditsMetaDataProvider().defineAll(instance, SampleMetaDataProvider.MEMORY_BACKEND_NAME, null);
      instance.getTable(PERSON).withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.FIELD));
      QContext.init(instance, new QSession().withUser(new QUser().withIdReference("audit-sample-user").withFullName("Audit Sample User")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      QContext.clear();
      MemoryRecordStore.getInstance().reset();
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    ** A real Person edit exposes the previous and new field values in audits.
    *******************************************************************************/
   @Test
   void testPersonFieldHistoryThroughAuditProcess() throws Exception
   {
      assertEquals(AuditLevel.FIELD, QContext.getQInstance().getTable(PERSON).getAuditRules().getAuditLevel());
      QRecord inserted = new InsertAction().execute(new InsertInput(PERSON).withRecord(new QRecord()
         .withValue("firstName", "Audit Before").withValue("lastName", "Sample")
         .withValue("email", "audit@example.invalid"))).getRecords().get(0);
      assertTrue(inserted.getErrors().isEmpty(), inserted.getErrorsAsString());
      Integer personId = inserted.getValueInteger("id");

      QRecord updated = new UpdateAction().execute(new UpdateInput(PERSON).withRecord(new QRecord()
         .withValue("id", personId).withValue("firstName", "Audit After"))).getRecords().get(0);
      assertTrue(updated.getErrors().isEmpty(), updated.getErrorsAsString());

      List<QRecord> audits = auditsFor(personId);
      assertTrue(audits.stream().anyMatch(audit -> "firstName".equals(audit.getValueString("auditDetail.fieldName"))
         && "Audit Before".equals(audit.getValueString("auditDetail.oldValue"))
         && "Audit After".equals(audit.getValueString("auditDetail.newValue"))), () -> audits.toString());
   }



   /*******************************************************************************
    ** NONE suppresses automatic events; RECORD and explicit actions have headers.
    *******************************************************************************/
   @Test
   void testNoneRecordAndExplicitAuditEvents() throws Exception
   {
      QContext.getQInstance().getTable(PERSON).withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.NONE));
      QRecord inserted = new InsertAction().execute(new InsertInput(PERSON).withRecord(new QRecord()
         .withValue("firstName", "Audit Levels").withValue("lastName", "Sample")
         .withValue("email", "levels@example.invalid"))).getRecords().get(0);
      assertTrue(inserted.getErrors().isEmpty(), inserted.getErrorsAsString());
      Integer personId = inserted.getValueInteger("id");
      assertTrue(auditsFor(personId).isEmpty());

      QContext.getQInstance().getTable(PERSON).withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.RECORD));
      QRecord updated = new UpdateAction().execute(new UpdateInput(PERSON).withRecord(new QRecord()
         .withValue("id", personId).withValue("firstName", "Audit Levels Changed"))).getRecords().get(0);
      assertTrue(updated.getErrors().isEmpty(), updated.getErrorsAsString());
      List<QRecord> recordAudits = auditsFor(personId);
      assertEquals(1, recordAudits.size());
      assertEquals("Record was Edited", recordAudits.get(0).getValueString("message"));
      assertNull(recordAudits.get(0).getValue("auditDetail.fieldName"));

      AuditAction.execute(PERSON, personId, Map.of(), "Manual review");
      List<QRecord> withExplicitEvent = auditsFor(personId);
      assertEquals(2, withExplicitEvent.size());
      assertTrue(withExplicitEvent.stream().anyMatch(audit -> "Manual review".equals(audit.getValueString("message"))));
   }



   /*******************************************************************************
    ** Audit retrieval follows the audit table's read permission.
    *******************************************************************************/
   @Test
   void testAuditRetrievalRequiresPermission() throws Exception
   {
      QInstance instance = QContext.getQInstance();
      instance.getAuthentication().setCustomizer(new QCodeReference(LimitedAuditReads.class));
      instance.getTable("audit").withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      GetAuditsForRecordProcess.setProcessPermissionToBeBasedOnAuditTableReadPermission(instance.getProcess(GetAuditsForRecordProcess.NAME));
      SampleJavalinServer server = new SampleJavalinServer(new SampleMetaDataProvider()
      {
         @Override
         public QInstance defineQInstance()
         {
            return instance;
         }
      });
      AtomicReference<Javalin> service = new AtomicReference<>();
      server.setPort(0);
      server.withJavalinConfigurationCustomizer(service::set);
      try
      {
         server.start();
         AuditAction.execute(PERSON, 1, Map.of(), "Protected audit event");
         try(HttpClient client = HttpClient.newHttpClient())
         {
            String baseUrl = "http://127.0.0.1:" + service.get().port();
            HttpRequest personRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/data/person/1"))
               .header("Cookie", "sessionId=person-only").GET().build();
            HttpResponse<String> person = client.send(personRequest, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, person.statusCode(), person.body());
            assertTrue(person.body().contains("Avery"), person.body());

            HttpResponse<String> denied = requestAudits(client, baseUrl, "person-only");
            assertEquals(403, denied.statusCode(), denied.body());
            assertFalse(denied.body().contains("Protected audit event"));

            HttpResponse<String> allowed = requestAudits(client, baseUrl, "audit-reader");
            assertEquals(200, allowed.statusCode(), allowed.body());
            assertTrue(allowed.body().contains("Protected audit event"), allowed.body());
         }
      }
      finally
      {
         server.stop();
      }
   }



   /*******************************************************************************
    ** The two HTTP callers differ only in their audit-table read permission.
    *******************************************************************************/
   public static class LimitedAuditReads implements QAuthenticationModuleCustomizerInterface
   {
      @Override
      public void customizeSession(QInstance instance, QSession session, Map<String, Object> context)
      {
         session.withPermissions("person.read");
         if("audit-reader".equals(session.getUuid()))
         {
            session.withPermission("audit.read");
         }
      }
   }



   /*******************************************************************************
    ** Run the same process with a valid record request and a caller cookie.
    *******************************************************************************/
   private HttpResponse<String> requestAudits(HttpClient client, String baseUrl, String caller) throws Exception
   {
      HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/processes/" + GetAuditsForRecordProcess.NAME + "/run"))
         .header("Cookie", "sessionId=" + caller).header("Content-Type", "application/x-www-form-urlencoded")
         .POST(HttpRequest.BodyPublishers.ofString("tableName=person&recordId=1")).build();
      return client.send(request, HttpResponse.BodyHandlers.ofString());
   }



   /*******************************************************************************
    ** An invalid Person update produces neither a write nor another event.
    *******************************************************************************/
   @Test
   void testFailedWriteAndMissingRecordHaveNoAuditEvent() throws Exception
   {
      Integer personId = insertPerson("Unchanged");
      int before = auditsFor(personId).size();
      QRecord rejected = new UpdateAction().execute(new UpdateInput(PERSON).withRecord(new QRecord()
         .withValue("id", personId).withValue("firstName", null))).getRecords().get(0);
      assertFalse(rejected.getErrors().isEmpty());
      assertEquals(before, auditsFor(personId).size());
      assertEquals("Unchanged", new GetAction().executeForRecord(new GetInput(PERSON).withPrimaryKey(personId)).getValueString("firstName"));
      assertTrue(auditsFor(999999).isEmpty());
   }



   /*******************************************************************************
    ** Password history reports a change without storing either secret value.
    *******************************************************************************/
   @Test
   void testPasswordAuditMasksOldAndNewValues() throws Exception
   {
      QContext.getQInstance().getTable("fieldLab").withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.FIELD));
      QRecord inserted = new InsertAction().execute(new InsertInput("fieldLab").withRecord(new QRecord()
         .withValue("name", "Audit secret row").withValue("passwordValue", "first-private-value"))).getRecords().get(0);
      assertTrue(inserted.getErrors().isEmpty(), inserted.getErrorsAsString());
      Integer id = inserted.getValueInteger("id");
      QRecord updated = new UpdateAction().execute(new UpdateInput("fieldLab").withRecord(new QRecord()
         .withValue("id", id).withValue("passwordValue", "second-private-value"))).getRecords().get(0);
      assertTrue(updated.getErrors().isEmpty(), updated.getErrorsAsString());

      List<QRecord> audits = auditsFor("fieldLab", id);
      List<QRecord> passwordChanges = audits.stream()
         .filter(audit -> "passwordValue".equals(audit.getValueString("auditDetail.fieldName"))).toList();
      assertEquals(2, passwordChanges.size());
      assertTrue(passwordChanges.stream().allMatch(audit -> audit.getValue("auditDetail.oldValue") == null
         && audit.getValue("auditDetail.newValue") == null));
      assertFalse(audits.toString().contains("first-private-value"));
      assertFalse(audits.toString().contains("second-private-value"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Integer insertPerson(String firstName) throws Exception
   {
      QRecord inserted = new InsertAction().execute(new InsertInput(PERSON).withRecord(new QRecord()
         .withValue("firstName", firstName).withValue("lastName", "Audit")
         .withValue("email", "audit-fixture@example.invalid"))).getRecords().get(0);
      assertTrue(inserted.getErrors().isEmpty(), inserted.getErrorsAsString());
      return inserted.getValueInteger("id");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<QRecord> auditsFor(Integer personId) throws Exception
   {
      return auditsFor(PERSON, personId);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   private List<QRecord> auditsFor(String tableName, Integer recordId) throws Exception
   {
      RunProcessInput input = new RunProcessInput().withProcessName(GetAuditsForRecordProcess.NAME).withInputSource(QInputSource.USER);
      input.addValue("tableName", tableName);
      input.addValue("recordId", recordId);
      RunProcessOutput output = new RunProcessAction().execute(input);
      return (List<QRecord>) output.getValue("audits");
   }
}
