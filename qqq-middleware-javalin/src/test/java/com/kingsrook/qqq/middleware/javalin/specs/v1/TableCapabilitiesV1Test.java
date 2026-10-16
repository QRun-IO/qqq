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


package com.kingsrook.qqq.middleware.javalin.specs.v1;


import com.kingsrook.qqq.backend.core.actions.permissions.SystemTablePermissions;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.audits.AuditsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditRules;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.processes.implementations.audits.GetAuditsForRecordProcess;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.AuditsCapabilityV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.TableCapabilitiesV1;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for TableCapabilitiesV1
 *******************************************************************************/
class TableCapabilitiesV1Test
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      QContext.clear();
   }



   /*******************************************************************************
    ** audits are offered whenever the user can read them, whatever the table's
    ** level: code can write audits for records of a table at level NONE.
    *******************************************************************************/
   @Test
   void testAuditsAvailableRegardlessOfLevel() throws Exception
   {
      QInstance instance = TestUtils.defineInstance();
      new AuditsMetaDataProvider().defineAll(instance, TestUtils.BACKEND_NAME_MEMORY, null);
      QContext.init(instance, new QSession());

      QTableMetaData   person = instance.getTable(TestUtils.TABLE_NAME_PERSON).withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.NONE));
      AuditsCapabilityV1 audits = TableCapabilitiesV1.of(person).getAudits();
      assertTrue(audits.getAvailable());
      assertEquals("NONE", audits.getLevel());
      assertEquals(GetAuditsForRecordProcess.NAME, audits.getProcessName());

      //////////////////////////////////////////////////////////////////////
      // the audit tables themselves are offered only to audit.read holders //
      //////////////////////////////////////////////////////////////////////
      assertNull(audits.getTableName());
      assertNull(audits.getDetailTableName());

      QContext.setQSession(new QSession().withPermission(SystemTablePermissions.AUDIT_READ_PERMISSION));
      audits = TableCapabilitiesV1.of(person).getAudits();
      assertEquals(GetAuditsForRecordProcess.NAME, audits.getProcessName());
      assertEquals(AuditsMetaDataProvider.TABLE_NAME_AUDIT, audits.getTableName());
      assertEquals(AuditsMetaDataProvider.TABLE_NAME_AUDIT_DETAIL, audits.getDetailTableName());

      person.withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.FIELD));
      assertEquals("FIELD", TableCapabilitiesV1.of(person).getAudits().getLevel());
   }



   /*******************************************************************************
    ** without the audit process or table, audits are not offered.
    *******************************************************************************/
   @Test
   void testAuditsUnavailableWithoutProcessOrTable() throws Exception
   {
      QInstance instance = TestUtils.defineInstance();
      QContext.init(instance, new QSession());

      QTableMetaData     person = instance.getTable(TestUtils.TABLE_NAME_PERSON).withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.FIELD));
      AuditsCapabilityV1 audits = TableCapabilitiesV1.of(person).getAudits();
      assertFalse(audits.getAvailable());
      assertEquals("FIELD", audits.getLevel());
   }



   /*******************************************************************************
    ** available only shows the button: a user who may not read the table (or
    ** the audits) gets available=false; the audit process enforces the same.
    *******************************************************************************/
   @Test
   void testAuditsRequireReadPermission() throws Exception
   {
      QInstance instance = TestUtils.defineInstance();
      new AuditsMetaDataProvider().defineAll(instance, TestUtils.BACKEND_NAME_MEMORY, null);
      QTableMetaData person = instance.getTable(TestUtils.TABLE_NAME_PERSON);
      person.setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));

      QContext.init(instance, new QSession());
      assertFalse(TableCapabilitiesV1.of(person).getAudits().getAvailable());

      QContext.init(instance, new QSession().withPermission(TestUtils.TABLE_NAME_PERSON + ".read"));
      assertTrue(TableCapabilitiesV1.of(person).getAudits().getAvailable());

      ///////////////////////////////////////////////////////////////////////////
      // the audit process and audit table protected too: reading the table   //
      // alone is not enough                                                   //
      ///////////////////////////////////////////////////////////////////////////
      instance.getProcess(GetAuditsForRecordProcess.NAME).setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
      instance.getTable(AuditsMetaDataProvider.TABLE_NAME_AUDIT).setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      AuditsCapabilityV1 audits = TableCapabilitiesV1.of(person).getAudits();
      assertFalse(audits.getAvailable());
      assertNull(audits.getProcessName());
      assertNull(audits.getTableName());

      QContext.init(instance, new QSession().withPermissions(TestUtils.TABLE_NAME_PERSON + ".read", GetAuditsForRecordProcess.NAME + ".hasAccess"));
      audits = TableCapabilitiesV1.of(person).getAudits();
      assertTrue(audits.getAvailable());
      assertEquals(GetAuditsForRecordProcess.NAME, audits.getProcessName());
      assertNull(audits.getTableName());
   }

}
