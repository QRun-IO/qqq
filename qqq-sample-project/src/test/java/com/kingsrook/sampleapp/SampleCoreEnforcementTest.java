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

package com.kingsrook.sampleapp;


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.permissions.CorePermissionEnforcer;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.security.FieldSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Sample-app check of core permission enforcement (#1061 / #1062) against the
 ** RDBMS backend, calling actions directly - the way an MCP server, assistant or
 ** custom route does - rather than through a middleware.
 *******************************************************************************/
class SampleCoreEnforcementTest
{
   private QInstance instance;



   /*******************************************************************************
    ** person: READ/INSERT/EDIT/DELETE permissions; annualSalary locked to
    ** sessions with clearance=hr.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      instance.addSecurityKeyType(new QSecurityKeyType().withName("clearance"));
      instance.getTable(SampleMetaDataProvider.TABLE_NAME_PERSON).setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      instance.getTable(SampleMetaDataProvider.TABLE_NAME_PERSON).getField("annualSalary").setFieldSecurityLock(new FieldSecurityLock()
         .withSecurityKeyType("clearance")
         .withDefaultBehavior(FieldSecurityLock.Behavior.DENY)
         .withOverrideValues(List.of("hr")));
      QContext.init(instance, new QSession());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      QContext.clear();
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    ** The setting is off by default: legacy behavior.  ENFORCE on an input checks
    ** anyway (the opt-in for MCP / assistant callers).
    *******************************************************************************/
   @Test
   void testOffByDefaultAndPerCallEnforce() throws Exception
   {
      assertEquals(5, new QueryAction().execute(new QueryInput(SampleMetaDataProvider.TABLE_NAME_PERSON)).getRecords().size());

      QueryInput enforced = new QueryInput(SampleMetaDataProvider.TABLE_NAME_PERSON);
      enforced.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
      assertThatThrownBy(() -> new QueryAction().execute(enforced)).isInstanceOf(QPermissionDeniedException.class);
   }



   /*******************************************************************************
    ** With the instance setting on, direct calls follow the session's roles.
    *******************************************************************************/
   @Test
   void testInstanceSettingOn() throws Exception
   {
      instance.setEnforcePermissionsInCoreActions(true);
      String person = SampleMetaDataProvider.TABLE_NAME_PERSON;

      assertThatThrownBy(() -> new CountAction().execute(new CountInput(person))).isInstanceOf(QPermissionDeniedException.class);

      QContext.setQSession(new QSession().withPermissions(person + ".read"));
      List<QRecord> records = new QueryAction().execute(new QueryInput(person)).getRecords();
      assertEquals(5, records.size());
      assertTrue(records.get(0).getValues().containsKey("email"));
      assertFalse(records.get(0).getValues().containsKey("annualSalary"));
      assertThatThrownBy(() -> new QueryAction().execute(new QueryInput(person).withFilter(new QQueryFilter(new QFilterCriteria("annualSalary", QCriteriaOperator.GREATER_THAN, 100000)))))
         .isInstanceOf(QPermissionDeniedException.class);
      assertThatThrownBy(() -> new UpdateAction().execute(new UpdateInput(person).withRecord(new QRecord().withValue("id", 1).withValue("lastName", "X"))))
         .isInstanceOf(QPermissionDeniedException.class);

      QContext.setQSession(new QSession().withPermissions(person + ".read", person + ".edit").withSecurityKeyValue("clearance", "hr"));
      assertTrue(new QueryAction().execute(new QueryInput(person)).getRecords().get(0).getValues().containsKey("annualSalary"));
      new UpdateAction().execute(new UpdateInput(person).withRecord(new QRecord().withValue("id", 1).withValue("annualSalary", 80000)));

      ///////////////////////////////////////////////////////////////
      // an explicit, auditable bypass for trusted internal work  //
      ///////////////////////////////////////////////////////////////
      QContext.setQSession(new QSession());
      assertEquals(5, new CountAction().execute((CountInput) new CountInput(person).withPermissionBypass("sample: trusted internal count")).getCount());
      assertEquals(5, CorePermissionEnforcer.runTrusted("sample: trusted block", () -> new CountAction().execute(new CountInput(person)).getCount()));
   }



   /*******************************************************************************
    ** Dry runs against the RDBMS backend store nothing.
    *******************************************************************************/
   @Test
   void testDryRuns() throws Exception
   {
      String person = SampleMetaDataProvider.TABLE_NAME_PERSON;
      new InsertAction().execute(new InsertInput(person).withDryRun(true).withRecord(new QRecord().withValue("firstName", "Dry").withValue("lastName", "Run").withValue("email", "dry@example.invalid")));
      assertEquals(5, new CountAction().execute(new CountInput(person)).getCount());

      DeleteOutput deleteOutput = new DeleteAction().execute(new DeleteInput(person).withDryRun(true).withQueryFilter(new QQueryFilter(new QFilterCriteria("firstName", QCriteriaOperator.EQUALS, "Avery"))));
      assertEquals(1, deleteOutput.getDryRunPrimaryKeys().size());
      assertEquals(5, new CountAction().execute(new CountInput(person)).getCount());
   }

}
