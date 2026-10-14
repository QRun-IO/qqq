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

package com.kingsrook.qqq.backend.core.actions.permissions;


import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.audits.AuditAction;
import com.kingsrook.qqq.backend.core.actions.metadata.MetaDataAction;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.metadata.MetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.MetaDataOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.audits.AuditsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.ClientChannelsMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.processes.utils.GeneralProcessUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Client channels: the assistant wall (an admin via the assistant channel is
 ** denied its configuration and the security surfaces, while the same admin via
 ** next-ui is allowed), per-object deny rules, metadata omission, and audits
 ** recording the channel.
 *******************************************************************************/
class ClientChannelPermissionsTest extends BaseTest
{
   /***************************************************************************
    ** tables and processes the assistant must never reach, by default
    ***************************************************************************/
   private static final List<String> WALLED_TABLES = List.of(
      "assistant_provider", "assistant_model", "assistant_price", "assistant_profile", "assistant_skill", "assistant_policy",
      "assistant_budget", "assistant_usage", "assistant_tool_call_log", "assistant_conversation",
      "authentik_user", "authentik_group_permission", "apiKey", "api_key", "user_credential",
      "webhooks_endpoint", "oauth_client_secret", "userSession", "user_role");

   private static final List<String> WALLED_PROCESSES = List.of("assistant_rotateProviderKey", "authentik_syncUsers");



   /***************************************************************************
    ** an instance with the walled tables and processes, plus open ones
    ***************************************************************************/
   private QInstance defineInstance()
   {
      QInstance qInstance = TestUtils.defineInstance();
      for(String name : WALLED_TABLES)
      {
         if(qInstance.getTable(name) == null)
         {
            qInstance.addTable(simpleTable(name));
         }
      }
      for(String name : WALLED_PROCESSES)
      {
         qInstance.addProcess(new QProcessMetaData().withName(name).withStep(new QBackendStepMetaData().withName("run").withCode(new QCodeReference(NoopStep.class))));
      }
      for(String name : List.of("audit", "auditDetail"))
      {
         if(qInstance.getTable(name) == null)
         {
            qInstance.addTable(simpleTable(name));
         }
      }
      qInstance.addTable(simpleTable("ordinaryTable").withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.NOT_PROTECTED).withDeniedClients(Set.of("cli"))));
      return (qInstance);
   }



   /***************************************************************************
    ** a backend step that does nothing
    ***************************************************************************/
   public static class NoopStep implements BackendStep
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput)
      {
         // noop
      }
   }



   /***************************************************************************
    ** a minimal memory table
    ***************************************************************************/
   private static QTableMetaData simpleTable(String name)
   {
      return (new QTableMetaData()
         .withName(name)
         .withBackendName(TestUtils.MEMORY_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER)));
   }



   /***************************************************************************
    ** an admin: every table and process is unprotected in the test instance
    ***************************************************************************/
   private void asAdminVia(QInstance qInstance, String clientChannel)
   {
      QContext.init(qInstance, new QSession().withUser(new QUser().withFullName("Admin").withIdReference("admin")).withClientChannel(clientChannel));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testAssistantWallDeniesAdminButNextUiAllows() throws QException
   {
      QInstance qInstance = defineInstance();
      new QInstanceValidator().validate(qInstance);

      asAdminVia(qInstance, "assistant/turn-1");
      QueryInput input = new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY);
      for(String table : WALLED_TABLES)
      {
         assertFalse(PermissionsHelper.hasTablePermission(input, table, TablePermissionSubType.READ), "assistant denied table " + table);
         assertEquals(PermissionCheckResult.DENY_HIDE, PermissionsHelper.getPermissionCheckResult(input, qInstance.getTable(table)), table);
      }
      for(String process : WALLED_PROCESSES)
      {
         assertFalse(PermissionsHelper.hasProcessPermission(input, process), "assistant denied process " + process);
      }
      assertTrue(PermissionsHelper.hasTablePermission(input, TestUtils.TABLE_NAME_PERSON_MEMORY, TablePermissionSubType.READ));

      ///////////////////////////////////////////////////////////////////////////
      // audits: readable (record history), never written - no tampering        //
      ///////////////////////////////////////////////////////////////////////////
      for(String table : List.of("audit", "auditDetail"))
      {
         assertTrue(PermissionsHelper.hasTablePermission(input, table, TablePermissionSubType.READ), "assistant reads " + table);
         for(TablePermissionSubType write : List.of(TablePermissionSubType.INSERT, TablePermissionSubType.EDIT, TablePermissionSubType.DELETE))
         {
            assertFalse(PermissionsHelper.hasTablePermission(input, table, write), "assistant may not " + write + " " + table);
         }
      }

      ///////////////////////////////////////////////////////////////
      // metadata omits the walled objects for the assistant only  //
      ///////////////////////////////////////////////////////////////
      MetaDataOutput assistantMetaData = new MetaDataAction().execute(new MetaDataInput());
      assertThat(assistantMetaData.getTables()).doesNotContainKeys(WALLED_TABLES.toArray(String[]::new)).containsKey(TestUtils.TABLE_NAME_PERSON_MEMORY);
      assertThat(assistantMetaData.getProcesses()).doesNotContainKeys(WALLED_PROCESSES.toArray(String[]::new));

      for(String channel : new String[] { "next-ui", null })
      {
         asAdminVia(qInstance, channel);
         for(String table : WALLED_TABLES)
         {
            assertTrue(PermissionsHelper.hasTablePermission(input, table, TablePermissionSubType.READ), channel + " allowed table " + table);
         }
         for(String process : WALLED_PROCESSES)
         {
            assertTrue(PermissionsHelper.hasProcessPermission(input, process), channel + " allowed process " + process);
         }
         assertThat(new MetaDataAction().execute(new MetaDataInput()).getTables()).containsKeys(WALLED_TABLES.toArray(String[]::new));
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testPerObjectAndRegisteredRules() throws QException
   {
      QInstance qInstance = defineInstance();
      ClientChannelsMetaData.ofOrWithNew(qInstance)
         .withDeniedObject("cli", TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withChannel("kiosk", "A kiosk")
         .withDeniedPattern("kiosk", "person*");

      QueryInput input = new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY);
      asAdminVia(qInstance, "cli");
      assertFalse(PermissionsHelper.hasTablePermission(input, "ordinaryTable", TablePermissionSubType.READ));
      assertFalse(PermissionsHelper.hasTablePermission(input, TestUtils.TABLE_NAME_PERSON_MEMORY, TablePermissionSubType.READ));

      asAdminVia(qInstance, "kiosk/terminal-3");
      assertFalse(PermissionsHelper.hasTablePermission(input, TestUtils.TABLE_NAME_PERSON_MEMORY, TablePermissionSubType.READ));
      assertTrue(PermissionsHelper.hasTablePermission(input, "ordinaryTable", TablePermissionSubType.READ));

      ////////////////////////////////////////////////////////////////////////
      // a registered registry still carries the assistant defaults (ofOrWithNew) //
      ////////////////////////////////////////////////////////////////////////
      asAdminVia(qInstance, "assistant");
      assertFalse(PermissionsHelper.hasTablePermission(input, "assistant_provider", TablePermissionSubType.READ));

      assertTrue(ClientChannelsMetaData.of(qInstance).isAllowedChannel("kiosk"));
      assertFalse(ClientChannelsMetaData.of(qInstance).isAllowedChannel("toaster"));
      assertEquals("assistant", ClientChannelsMetaData.channelName(" Assistant/turn-9 "));
      assertNull(new QSession().getClientChannelName());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testValidation()
   {
      QInstance qInstance = defineInstance();
      ClientChannelsMetaData.ofOrWithNew(qInstance).withChannel("Bad Name", "x").withDeniedPattern("nobody", "x*").withDeniedObject("cli", "noSuchThing");
      QInstanceValidator validator = new QInstanceValidator();
      ClientChannelsMetaData.of(qInstance).validate(qInstance, validator);
      assertThat(validator.getErrors()).anyMatch(e -> e.contains("[bad name]")).anyMatch(e -> e.contains("unknown channel [nobody]"));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testAuditRecordsTheClientChannel() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();
      new AuditsMetaDataProvider().withClientChannelField(true).withDefineRecordScreens(true).defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);
      assertThat(qInstance.getTable("audit").getFields()).containsKey(AuditsMetaDataProvider.FIELD_CLIENT_CHANNEL);

      QContext.init(qInstance, new QSession().withUser(new QUser().withFullName("Jane Admin")).withClientChannel("assistant/turn-7"));
      AuditAction.execute(TestUtils.TABLE_NAME_PERSON_MEMORY, 1701, Map.of(), "Changed by the assistant");

      QContext.init(qInstance, new QSession().withUser(new QUser().withFullName("Jane Admin")).withClientChannel("next-ui"));
      AuditAction.execute(TestUtils.TABLE_NAME_PERSON_MEMORY, 1702, Map.of(), "Changed in the UI");

      QRecord byAssistant = GeneralProcessUtils.getRecordByFieldOrElseThrow("audit", "recordId", 1701);
      QRecord byUi        = GeneralProcessUtils.getRecordByFieldOrElseThrow("audit", "recordId", 1702);
      assertEquals("assistant/turn-7", byAssistant.getValueString(AuditsMetaDataProvider.FIELD_CLIENT_CHANNEL));
      assertEquals("next-ui", byUi.getValueString(AuditsMetaDataProvider.FIELD_CLIENT_CHANNEL));
      assertEquals(byAssistant.getValue("auditUserId"), byUi.getValue("auditUserId"));

      /////////////////////////////////////////////////////////
      // without the field (the default), nothing is written //
      /////////////////////////////////////////////////////////
      QInstance plain = TestUtils.defineInstance();
      new AuditsMetaDataProvider().defineAll(plain, TestUtils.MEMORY_BACKEND_NAME, null);
      assertThat(plain.getTable("audit").getFields()).doesNotContainKey(AuditsMetaDataProvider.FIELD_CLIENT_CHANNEL);
   }
}
