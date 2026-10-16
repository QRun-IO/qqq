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


import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.audits.AuditAction;
import com.kingsrook.qqq.backend.core.actions.reporting.ExportAction;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.values.SearchPossibleValueSourceAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ExportInput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportDestination;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormat;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceInput;
import com.kingsrook.qqq.backend.core.model.audits.AuditsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.querystats.QueryStat;
import com.kingsrook.qqq.backend.core.model.querystats.QueryStatMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.model.tables.QQQTablesMetaDataProvider;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.metadata.RedirectStateMetaDataProducer;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.metadata.UserSessionMetaDataProducer;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.metadata.UserSessionRefreshTokenMetaDataProducer;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.model.UserSession;
import com.kingsrook.qqq.backend.core.serviceidentity.LocalServiceIdentityProvider;
import com.kingsrook.qqq.backend.core.serviceidentity.QServicePrincipalMetaData;
import com.kingsrook.qqq.backend.core.utils.ExceptionUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for SystemTablePermissions: QQQ's own system tables need an
 ** explicit permission (audit.read, queryStat.read) to read directly, and the
 ** internal session tables can't be read by any user at all - in an instance
 ** whose default permission rules leave everything else unprotected.
 *******************************************************************************/
class SystemTablePermissionsTest extends BaseTest
{
   private static final List<String> READABLE_SYSTEM_TABLES = new ArrayList<>();

   static
   {
      READABLE_SYSTEM_TABLES.addAll(SystemTablePermissions.AUDIT_TABLE_NAMES);
      READABLE_SYSTEM_TABLES.addAll(SystemTablePermissions.QUERY_STAT_TABLE_NAMES);
   }



   /*******************************************************************************
    ** The standard test instance (unprotected by default), with every system
    ** table defined, and core permission enforcement on.
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws QException
   {
      QInstance qInstance = defineInstance();
      QContext.init(qInstance, new QSession().withUser(new QUser().withIdReference("nobody").withFullName("Nobody")));
      AuditAction.execute(TestUtils.TABLE_NAME_PERSON_MEMORY, 1, Map.of(), "Audit of person 1");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static QInstance defineInstance() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();
      assertEquals(PermissionLevel.NOT_PROTECTED, qInstance.getDefaultPermissionRules().getLevel());
      new AuditsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);
      new QQQTablesMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, TestUtils.MEMORY_BACKEND_NAME, null);
      new QueryStatMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);
      qInstance.addTable(new UserSessionMetaDataProducer(TestUtils.MEMORY_BACKEND_NAME).produce(qInstance));
      qInstance.addTable(new RedirectStateMetaDataProducer(TestUtils.MEMORY_BACKEND_NAME).produce(qInstance));
      qInstance.addTable(new UserSessionRefreshTokenMetaDataProducer(TestUtils.MEMORY_BACKEND_NAME).produce(qInstance));
      qInstance.setEnforcePermissionsInCoreActions(true);
      return (qInstance);
   }



   /*******************************************************************************
    ** Every system table this class covers is defined by the instance.
    *******************************************************************************/
   @Test
   void testEverySystemTableIsCovered()
   {
      for(String tableName : allSystemTables())
      {
         assertNotNull(QContext.getQInstance().getTable(tableName), tableName);
         assertTrue(SystemTablePermissions.isSystemTable(tableName), tableName);
      }
      assertFalse(SystemTablePermissions.isSystemTable(TestUtils.TABLE_NAME_PERSON_MEMORY));
      assertFalse(SystemTablePermissions.isSystemTable(null));
   }



   /*******************************************************************************
    ** A session with no permissions: every system table is denied (and hidden)
    ** for query, get, count and export - while an ordinary table is not.
    *******************************************************************************/
   @Test
   void testNoPermissionSessionIsDeniedEverySystemTable() throws QException
   {
      for(String tableName : allSystemTables())
      {
         assertNoDirectAccess(tableName);
      }

      new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY));
      assertTrue(PermissionsHelper.hasTablePermission(new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY), TestUtils.TABLE_NAME_PERSON_MEMORY, TablePermissionSubType.READ));
   }



   /*******************************************************************************
    ** audit.read opens exactly the four audit tables, as ordinary tables.
    *******************************************************************************/
   @Test
   void testAuditReadPermissionOpensTheAuditTables() throws QException
   {
      QContext.setQSession(new QSession().withUser(new QUser().withIdReference("auditor")).withPermission(SystemTablePermissions.AUDIT_READ_PERMISSION));
      assertEquals("audit.read", SystemTablePermissions.AUDIT_READ_PERMISSION);

      for(String tableName : SystemTablePermissions.AUDIT_TABLE_NAMES)
      {
         assertEquals(PermissionCheckResult.ALLOW, PermissionsHelper.getPermissionCheckResult(new QueryInput(tableName), QContext.getQInstance().getTable(tableName)), tableName);
         new QueryAction().execute(new QueryInput(tableName));
         new CountAction().execute(new CountInput(tableName));
      }
      List<QRecord> audits = new QueryAction().execute(new QueryInput(AuditsMetaDataProvider.TABLE_NAME_AUDIT)).getRecords();
      assertEquals(1, audits.size());

      for(String tableName : SystemTablePermissions.QUERY_STAT_TABLE_NAMES)
      {
         assertNoDirectAccess(tableName);
      }
      for(String tableName : SystemTablePermissions.INTERNAL_TABLE_NAMES)
      {
         assertNoDirectAccess(tableName);
      }
   }



   /*******************************************************************************
    ** queryStat.read opens the query statistics tables.
    *******************************************************************************/
   @Test
   void testQueryStatReadPermissionOpensTheQueryStatTables() throws QException
   {
      QContext.setQSession(new QSession().withPermission(SystemTablePermissions.QUERY_STAT_READ_PERMISSION));
      for(String tableName : SystemTablePermissions.QUERY_STAT_TABLE_NAMES)
      {
         new QueryAction().execute(new QueryInput(tableName));
      }
      assertNoDirectAccess(AuditsMetaDataProvider.TABLE_NAME_AUDIT);
   }



   /*******************************************************************************
    ** The internal tables are hidden, are not offered as permissions, and stay
    ** denied to a session holding every permission the instance offers.
    *******************************************************************************/
   @Test
   void testInternalTablesAreDeniedEvenToAnAdmin() throws QException
   {
      Collection<String> available = PermissionsHelper.getAllAvailablePermissionNames(QContext.getQInstance());
      assertThat(available).contains(SystemTablePermissions.AUDIT_READ_PERMISSION, SystemTablePermissions.QUERY_STAT_READ_PERMISSION);
      assertThat(available).doesNotContain(AuditsMetaDataProvider.TABLE_NAME_AUDIT_DETAIL + ".read", AuditsMetaDataProvider.TABLE_NAME_AUDIT + ".insert");
      assertThat(available).noneMatch(name -> name.startsWith("qqq.internal."));

      QContext.setQSession(new QSession().withUser(new QUser().withIdReference("admin")).withPermissions(available));
      for(String tableName : SystemTablePermissions.INTERNAL_TABLE_NAMES)
      {
         assertTrue(QContext.getQInstance().getTable(tableName).getIsHidden(), tableName);
         assertNoDirectAccess(tableName);
      }

      for(String tableName : READABLE_SYSTEM_TABLES)
      {
         new QueryAction().execute(new QueryInput(tableName));
      }
   }



   /*******************************************************************************
    ** The authentication modules work on the internal tables as the system user.
    *******************************************************************************/
   @Test
   void testSystemUserStillReadsTheInternalTables() throws QException
   {
      QContext.setQSession(new QSystemUserSession());
      for(String tableName : SystemTablePermissions.INTERNAL_TABLE_NAMES)
      {
         new QueryAction().execute(new QueryInput(tableName));
      }
   }



   /*******************************************************************************
    ** A service principal follows the same rules as a user.
    *******************************************************************************/
   @Test
   void testServicePrincipalSessionsFollowTheSameRules() throws QException
   {
      QSession plain = new LocalServiceIdentityProvider().sessionFor(QContext.getQInstance(), new QServicePrincipalMetaData().withName("indexer").withLocalPermissions(TestUtils.TABLE_NAME_PERSON_MEMORY + ".read"));
      QContext.setQSession(plain);
      for(String tableName : allSystemTables())
      {
         assertNoDirectAccess(tableName);
      }

      QSession auditor = new LocalServiceIdentityProvider().sessionFor(QContext.getQInstance(), new QServicePrincipalMetaData().withName("auditor").withLocalPermissions(SystemTablePermissions.AUDIT_READ_PERMISSION));
      QContext.setQSession(auditor);
      assertEquals(1, new QueryAction().execute(new QueryInput(AuditsMetaDataProvider.TABLE_NAME_AUDIT)).getRecords().size());
      assertNoDirectAccess(UserSession.TABLE_NAME);
   }



   /*******************************************************************************
    ** Writing audits never needs audit.read, even with enforcement on.
    *******************************************************************************/
   @Test
   void testWritingAuditsNeedsNoPermission() throws QException
   {
      AuditAction.execute(TestUtils.TABLE_NAME_PERSON_MEMORY, 2, Map.of(), "Audit of person 2", List.of(new QRecord().withValue("message", "detail")));

      QContext.setQSession(new QSystemUserSession());
      assertEquals(2, new QueryAction().execute(new QueryInput(AuditsMetaDataProvider.TABLE_NAME_AUDIT)).getRecords().size());
      assertEquals(1, new QueryAction().execute(new QueryInput(AuditsMetaDataProvider.TABLE_NAME_AUDIT_DETAIL)).getRecords().size());
   }



   /*******************************************************************************
    ** A user's own search of a possible-value source over a system table needs
    ** READ on that table; the framework's own (nested) lookups do not.
    *******************************************************************************/
   @Test
   void testPossibleValueSearchOfASystemTable() throws QException
   {
      assertThatThrownBy(() -> new SearchPossibleValueSourceAction().execute(pvsInput(AuditsMetaDataProvider.TABLE_NAME_AUDIT_USER)))
         .isInstanceOf(QPermissionDeniedException.class);
      assertThatThrownBy(() -> new SearchPossibleValueSourceAction().execute(pvsInput(QueryStat.TABLE_NAME)))
         .isInstanceOf(QPermissionDeniedException.class);

      ////////////////////////////////////////////////////////////////////////
      // not a user search (e.g., the framework's own): no table check here //
      ////////////////////////////////////////////////////////////////////////
      SearchPossibleValueSourceInput systemInput = pvsInput(AuditsMetaDataProvider.TABLE_NAME_AUDIT_USER);
      systemInput.setInputSource(QInputSource.SYSTEM);
      assertEquals(1, new SearchPossibleValueSourceAction().execute(systemInput).getResults().size());

      QContext.setQSession(new QSession().withPermission(SystemTablePermissions.AUDIT_READ_PERMISSION));
      assertEquals(1, new SearchPossibleValueSourceAction().execute(pvsInput(AuditsMetaDataProvider.TABLE_NAME_AUDIT_USER)).getResults().size());
   }



   /*******************************************************************************
    ** An instance whose default rules are protected keeps its level for the
    ** system tables, under the shared base names.
    *******************************************************************************/
   @Test
   void testProtectedInstanceDefaultKeepsItsLevel() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();
      qInstance.setDefaultPermissionRules(new QPermissionRules().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
      new AuditsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);

      QPermissionRules rules = qInstance.getTable(AuditsMetaDataProvider.TABLE_NAME_AUDIT_DETAIL).getPermissionRules();
      assertEquals(PermissionLevel.HAS_ACCESS_PERMISSION, rules.getLevel());
      assertEquals(SystemTablePermissions.AUDIT_PERMISSION_BASE_NAME, rules.getPermissionBaseName());

      QPermissionRules unprotected = SystemTablePermissions.readableSystemTableRules(TestUtils.defineInstance(), "x");
      assertEquals(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS, unprotected.getLevel());
   }



   /*******************************************************************************
    ** The enricher locks an OAuth2 instance's session and redirect-state tables
    ** when the application defined them itself, without permission rules.
    *******************************************************************************/
   @Test
   void testEnricherLocksConfiguredAuthenticationTables()
   {
      QInstance qInstance = TestUtils.defineInstance();
      qInstance.withInstanceDefaultAuthentication(new OAuth2AuthenticationMetaData()
         .withUserSessionTableName("mySessions")
         .withRedirectStateTableName("myStates")
         .withName("oauth2")
         .withType(QAuthenticationType.OAUTH2));
      for(String tableName : List.of("mySessions", "myStates"))
      {
         qInstance.addTable(new QTableMetaData().withName(tableName).withBackendName(TestUtils.MEMORY_BACKEND_NAME).withPrimaryKeyField("id")
            .withField(new QFieldMetaData("id", QFieldType.INTEGER)));
      }

      new QInstanceEnricher(qInstance).enrich();
      for(String tableName : List.of("mySessions", "myStates"))
      {
         QTableMetaData table = qInstance.getTable(tableName);
         assertTrue(table.getIsHidden(), tableName);
         assertEquals(PermissionLevel.HAS_ACCESS_PERMISSION, table.getPermissionRules().getLevel(), tableName);
         assertEquals(SystemTablePermissions.noAccessPermissionBaseName(tableName), table.getPermissionRules().getPermissionBaseName(), tableName);
      }
   }



   /*******************************************************************************
    ** Query, get, count and export of the table are all denied, and the table
    ** is hidden from the session.
    *******************************************************************************/
   private static void assertNoDirectAccess(String tableName)
   {
      assertEquals(PermissionCheckResult.DENY_HIDE, PermissionsHelper.getPermissionCheckResult(new QueryInput(tableName), QContext.getQInstance().getTable(tableName)), tableName);
      assertDenied(tableName, () -> new QueryAction().execute(new QueryInput(tableName)));
      assertDenied(tableName, () -> new GetAction().execute(new GetInput(tableName).withPrimaryKey(1)));
      assertDenied(tableName, () -> new CountAction().execute(new CountInput(tableName)));
      assertDenied(tableName, () ->
      {
         ExportInput exportInput = new ExportInput();
         exportInput.setTableName(tableName);
         new ExportAction().execute(exportInput.withReportDestination(new ReportDestination().withReportFormat(ReportFormat.CSV).withReportOutputStream(new ByteArrayOutputStream())));
      });
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void assertDenied(String tableName, org.assertj.core.api.ThrowableAssert.ThrowingCallable callable)
   {
      assertThatThrownBy(callable)
         .as(tableName)
         .satisfies(e -> assertNotNull(ExceptionUtils.findClassInRootChain(e, QPermissionDeniedException.class), tableName));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static SearchPossibleValueSourceInput pvsInput(String possibleValueSourceName)
   {
      SearchPossibleValueSourceInput input = new SearchPossibleValueSourceInput();
      input.setPossibleValueSourceName(possibleValueSourceName);
      input.setInputSource(QInputSource.USER);
      return (input);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<String> allSystemTables()
   {
      List<String> rs = new ArrayList<>(READABLE_SYSTEM_TABLES);
      rs.addAll(SystemTablePermissions.INTERNAL_TABLE_NAMES);
      return (rs);
   }
}
