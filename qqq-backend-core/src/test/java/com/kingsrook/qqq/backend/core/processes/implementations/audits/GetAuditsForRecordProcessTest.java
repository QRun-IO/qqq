/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.audits;


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.audits.AuditAction;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.ExamplePersonalizer;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.permissions.SystemTablePermissions;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.audits.AuditsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.serviceidentity.LocalServiceIdentityProvider;
import com.kingsrook.qqq.backend.core.serviceidentity.QServicePrincipalMetaData;
import com.kingsrook.qqq.backend.core.utils.ExceptionUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Unit test for GetAuditsForRecordProcess 
 *******************************************************************************/
class GetAuditsForRecordProcessTest extends BaseTest
{
   private Integer recordId = 1701;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();
      new AuditsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);

      String userName = "John Doe";
      QContext.init(qInstance, new QSession().withUser(new QUser().withFullName(userName)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSimple() throws QException
   {
      AuditAction.execute(TestUtils.TABLE_NAME_PERSON_MEMORY, recordId, Map.of(), "Test Audit", List.of(
         new QRecord().withValue("message", "My Detail")
      ));

      RunProcessInput input = new RunProcessInput();
      input.setProcessName(GetAuditsForRecordProcess.NAME);
      input.addValue("tableName", TestUtils.TABLE_NAME_PERSON_MEMORY);
      input.addValue("recordId", recordId);
      RunProcessOutput runProcessOutput = new RunProcessAction().execute(input);
      List<QRecord>    audits           = (List<QRecord>) runProcessOutput.getValue("audits");

      assertEquals(1, audits.size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMany() throws QException
   {
      AuditAction.execute(TestUtils.TABLE_NAME_PERSON_MEMORY, recordId, Map.of(), "Test Audit A", List.of(
         new QRecord().withValue("message", "Detail 1"),
         new QRecord().withValue("message", "Detail 2")
      ));

      AuditAction.execute(TestUtils.TABLE_NAME_PERSON_MEMORY, recordId, Map.of(), "Test Audit B", List.of(
         new QRecord().withValue("message", "Detail 3"),
         new QRecord().withValue("message", "Detail 4")
      ));

      AuditAction.execute(TestUtils.TABLE_NAME_PERSON_MEMORY, recordId, Map.of(), "Test Audit C", List.of(
         new QRecord().withValue("message", "Detail 5")
      ));

      //////////////////////////////////////////////////////////////////////
      // run with limit smaller than the number of details we've inserted //
      //////////////////////////////////////////////////////////////////////
      RunProcessInput input = new RunProcessInput();
      input.setProcessName(GetAuditsForRecordProcess.NAME);
      input.addValue("tableName", TestUtils.TABLE_NAME_PERSON_MEMORY);
      input.addValue("recordId", recordId);
      input.addValue("limit", 4);
      input.addValue("isSortAscending", true);
      RunProcessOutput runProcessOutput = new RunProcessAction().execute(input);

      List<QRecord> audits = (List<QRecord>) runProcessOutput.getValue("audits");
      assertEquals(4, audits.size());
      assertThat(audits).anyMatch(qRecord -> qRecord.getValue("auditDetail.message").equals("Detail 1"));
      assertThat(audits).allMatch(qRecord -> qRecord.getDisplayValue("auditUserId").equals("John Doe"));
      assertThat(audits).noneMatch(qRecord -> qRecord.getValue("auditDetail.message").equals("Detail 5"));
      assertEquals(5, runProcessOutput.getValueInteger("distinctCount"));

      //////////////////////////////////
      // run again with reversed sort //
      //////////////////////////////////
      input.addValue("isSortAscending", false);
      runProcessOutput = new RunProcessAction().execute(input);
      audits = (List<QRecord>) runProcessOutput.getValue("audits");
      assertEquals(4, audits.size());
      assertThat(audits).anyMatch(qRecord -> qRecord.getValue("auditDetail.message").equals("Detail 5"));

      //////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // a somewhat sloppy mid-audit pagination break issue puts Detail 1 in this result... but we'll lve with it //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////
      assertThat(audits).noneMatch(qRecord -> qRecord.getValue("auditDetail.message").equals("Detail 2"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testStrippedFieldDetails() throws QException
   {
      AuditAction.execute(TestUtils.TABLE_NAME_PERSON_MEMORY, recordId, Map.of(), "Test Audit A", List.of(
         new QRecord().withValue("message", "Detail 1"),
         new QRecord().withValue("message", "Detail 2").withValue("fieldName", "firstName").withValue("newValue", "John"),
         new QRecord().withValue("message", "Detail 3").withValue("fieldName", "noSuchField").withValue("newValue", "Xyz"),
         new QRecord().withValue("message", "Detail 4").withValue("fieldName", "lastName").withValue("newValue", "Doe")
      ));

      RunProcessInput input = new RunProcessInput();
      input.setProcessName(GetAuditsForRecordProcess.NAME);
      input.addValue("tableName", TestUtils.TABLE_NAME_PERSON_MEMORY);
      input.addValue("recordId", recordId);
      RunProcessOutput runProcessOutput = new RunProcessAction().execute(input);

      /////////////////////////////////////////////////////////////////////////////////////
      // assert we got the audit details for the valid fields, and the one with no field //
      /////////////////////////////////////////////////////////////////////////////////////
      List<QRecord> audits = (List<QRecord>) runProcessOutput.getValue("audits");
      assertEquals(3, audits.size());
      assertThat(audits).anyMatch(qRecord -> "firstName".equals(qRecord.getValue("auditDetail.fieldName")));
      assertThat(audits).noneMatch(qRecord -> "noSuchField".equals(qRecord.getValue("auditDetail.fieldName")));
      assertThat(audits).anyMatch(qRecord -> "lastName".equals(qRecord.getValue("auditDetail.fieldName")));
      assertThat(audits).anyMatch(qRecord -> qRecord.getValue("auditDetail.fieldName") == null);

      ////////////////////////////////////////////////////
      // personalize table to take away firstName field //
      ////////////////////////////////////////////////////
      ExamplePersonalizer.registerInQInstance();
      ExamplePersonalizer.addCustomizableTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      ExamplePersonalizer.addFieldToRemoveForUserId(TestUtils.TABLE_NAME_PERSON_MEMORY, "firstName", "jdoe");
      QContext.setQSession(new QSession().withUser(new QUser().withIdReference("jdoe")));

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // assert we got the audit details for the valid fields, but not the one we can't see (firstName), and the one with no field //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      runProcessOutput = new RunProcessAction().execute(input);
      audits = (List<QRecord>) runProcessOutput.getValue("audits");
      assertEquals(2, audits.size());
      assertThat(audits).noneMatch(qRecord -> "firstName".equals(qRecord.getValue("auditDetail.fieldName")));
      assertThat(audits).noneMatch(qRecord -> "noSuchField".equals(qRecord.getValue("auditDetail.fieldName")));
      assertThat(audits).anyMatch(qRecord -> "lastName".equals(qRecord.getValue("auditDetail.fieldName")));
      assertThat(audits).anyMatch(qRecord -> qRecord.getValue("auditDetail.fieldName") == null);
   }



   /*******************************************************************************
    ** a record's audits need read permission on its table, and a known table.
    *******************************************************************************/
   @Test
   void testRequiresReadPermissionOnTheAuditedTable() throws QException
   {
      AuditAction.execute(TestUtils.TABLE_NAME_PERSON_MEMORY, recordId, Map.of(), "Test Audit", List.of(new QRecord().withValue("message", "My Detail")));
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));

      QContext.init(QContext.getQInstance(), new QSession().withUser(new QUser().withFullName("No Person Read")));
      assertDenied(auditsInput(TestUtils.TABLE_NAME_PERSON_MEMORY));

      QContext.init(QContext.getQInstance(), new QSession().withPermission(TestUtils.TABLE_NAME_PERSON_MEMORY + ".read"));
      assertEquals(1, ((List<QRecord>) new RunProcessAction().execute(auditsInput(TestUtils.TABLE_NAME_PERSON_MEMORY)).getValue("audits")).size());

      assertDenied(auditsInput("noSuchTable"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private RunProcessInput auditsInput(String tableName)
   {
      RunProcessInput input = new RunProcessInput();
      input.setProcessName(GetAuditsForRecordProcess.NAME);
      input.addValue("tableName", tableName);
      input.addValue("recordId", recordId);
      return (input);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertDenied(RunProcessInput input)
   {
      assertThatThrownBy(() -> new RunProcessAction().execute(input))
         .satisfies(e -> assertNotNull(ExceptionUtils.findClassInRootChain(e, QPermissionDeniedException.class)));
   }


   /*******************************************************************************
    ** An app that never declared audit permissions (an unprotected instance)
    ** keeps record history for everyone who can read the record - even with core
    ** permission enforcement on, and although the audit table itself now needs
    ** audit.read to query directly.
    *******************************************************************************/
   @Test
   void testHistoryNeedsNoAuditPermission() throws QException
   {
      AuditAction.execute(TestUtils.TABLE_NAME_PERSON_MEMORY, recordId, Map.of(), "Test Audit", List.of(new QRecord().withValue("message", "My Detail")));
      QContext.getQInstance().setEnforcePermissionsInCoreActions(true);
      QContext.init(QContext.getQInstance(), new QSession().withUser(new QUser().withFullName("No Permissions")));

      RunProcessInput input = auditsInput(TestUtils.TABLE_NAME_PERSON_MEMORY);
      input.withPermissionEnforcement(PermissionEnforcement.ENFORCE);
      assertEquals(1, ((List<QRecord>) new RunProcessAction().execute(input).getValue("audits")).size());

      assertThatThrownBy(() -> new QueryAction().execute(new QueryInput(AuditsMetaDataProvider.TABLE_NAME_AUDIT)))
         .satisfies(e -> assertNotNull(ExceptionUtils.findClassInRootChain(e, QPermissionDeniedException.class)));
   }



   /*******************************************************************************
    ** On a table with record security locks, history needs the record itself to
    ** be readable by the caller.
    *******************************************************************************/
   @Test
   void testRecordLockedTableNeedsTheRecordReadable() throws QException
   {
      QContext.withTemporaryContext(new CapturedContext(QContext.getQInstance(), new QSystemUserSession()), () ->
         new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_ORDER).withRecords(List.of(
            new QRecord().withValue("id", 1).withValue("storeId", 1),
            new QRecord().withValue("id", 2).withValue("storeId", 2)))));
      AuditAction.execute(TestUtils.TABLE_NAME_ORDER, 1, Map.of(TestUtils.SECURITY_KEY_TYPE_STORE, 1), "Order 1 audit", List.of());

      RunProcessInput input = auditsInput(TestUtils.TABLE_NAME_ORDER);
      input.addValue("recordId", 1);

      QContext.init(QContext.getQInstance(), new QSession().withUser(new QUser().withFullName("Store 1")).withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE, 1));
      assertEquals(1, ((List<QRecord>) new RunProcessAction().execute(input).getValue("audits")).size());

      QContext.init(QContext.getQInstance(), new QSession().withUser(new QUser().withFullName("Store 2")).withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE, 2));
      assertDenied(input);
   }



   /*******************************************************************************
    ** Service principal sessions follow the same rule: read on the record's
    ** table gives its history; no read, no history.
    *******************************************************************************/
   @Test
   void testServicePrincipalSessions() throws QException
   {
      AuditAction.execute(TestUtils.TABLE_NAME_PERSON_MEMORY, recordId, Map.of(), "Test Audit", List.of());
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));

      QContext.init(QContext.getQInstance(), new LocalServiceIdentityProvider().sessionFor(QContext.getQInstance(),
         new QServicePrincipalMetaData().withName("reader").withLocalPermissions(TestUtils.TABLE_NAME_PERSON_MEMORY + ".read")));
      assertEquals(1, ((List<QRecord>) new RunProcessAction().execute(auditsInput(TestUtils.TABLE_NAME_PERSON_MEMORY)).getValue("audits")).size());

      QContext.init(QContext.getQInstance(), new LocalServiceIdentityProvider().sessionFor(QContext.getQInstance(),
         new QServicePrincipalMetaData().withName("other").withLocalPermissions("someOtherTable.read")));
      assertDenied(auditsInput(TestUtils.TABLE_NAME_PERSON_MEMORY));
   }



   /*******************************************************************************
    ** The opt-in stricter rule: history only for audit.read holders.
    *******************************************************************************/
   @Test
   void testOptInToAuditTablePermission() throws QException
   {
      AuditAction.execute(TestUtils.TABLE_NAME_PERSON_MEMORY, recordId, Map.of(), "Test Audit", List.of());
      assertEquals(PermissionLevel.NOT_PROTECTED, QContext.getQInstance().getProcess(GetAuditsForRecordProcess.NAME).getPermissionRules().getLevel());
      GetAuditsForRecordProcess.setProcessPermissionToBeBasedOnAuditTableReadPermission(QContext.getQInstance().getProcess(GetAuditsForRecordProcess.NAME));

      QContext.init(QContext.getQInstance(), new QSession().withUser(new QUser().withFullName("No Audit Read")));
      RunProcessInput input = auditsInput(TestUtils.TABLE_NAME_PERSON_MEMORY);
      input.withPermissionEnforcement(PermissionEnforcement.ENFORCE);
      assertDenied(input);

      QContext.init(QContext.getQInstance(), new QSession().withUser(new QUser().withFullName("Auditor")).withPermission(SystemTablePermissions.AUDIT_READ_PERMISSION));
      assertEquals(1, ((List<QRecord>) new RunProcessAction().execute(input).getValue("audits")).size());
   }

}
