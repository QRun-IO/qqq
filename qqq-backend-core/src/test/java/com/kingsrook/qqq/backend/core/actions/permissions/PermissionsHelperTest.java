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

package com.kingsrook.qqq.backend.core.actions.permissions;


import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.DenyBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportDataSource;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportField;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportView;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.ReportType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for PermissionsHelper
 *******************************************************************************/
class PermissionsHelperTest extends BaseTest
{
   static final String TABLE_NAME   = "testTable";
   static final String PROCESS_NAME = "testProcess";
   static final String APP_NAME     = "testApp";
   static final String REPORT_NAME  = "testReport";
   static final String WIDGET_NAME  = "testWidget";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableNoPermissionsMetaDataMeansFullAccess() throws QPermissionDeniedException
   {
      QInstance instance = newQInstance();
      QSession  session  = new QSession();
      enrich(instance);
      assertFullTableAccess(instance, session);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableWithHasAccessLevelAndWithoutPermission()
   {
      QInstance instance = newQInstance();
      instance.getTable(TABLE_NAME).setPermissionRules(new QPermissionRules()
         .withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
      enrich(instance);

      assertNoTableAccess(instance, new QSession());
      assertNoTableAccess(instance, new QSession().withPermission(TABLE_NAME + ".read"));
      assertNoTableAccess(instance, new QSession().withPermission(TABLE_NAME + ".write"));
      assertNoTableAccess(instance, new QSession().withPermission(TABLE_NAME + ".insert"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableWithHasAccessLevelAndWithPermission() throws QPermissionDeniedException
   {
      QInstance instance = newQInstance();
      instance.getTable(TABLE_NAME).setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
      enrich(instance);

      assertFullTableAccess(instance, new QSession().withPermission(TABLE_NAME + ".hasAccess"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableWithReadWriteLevelAndWithoutPermission()
   {
      QInstance instance = newQInstance();
      instance.setDefaultPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      enrich(instance);

      assertNoTableAccess(instance, new QSession());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableWithReadWriteLevelWithLimitedPermissions() throws QPermissionDeniedException
   {
      QInstance instance = newQInstance();
      instance.setDefaultPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      enrich(instance);

      {
         QSession session = new QSession().withPermissions(TABLE_NAME + ".read");
         QContext.setQSession(session);

         AbstractTableActionInput actionInput = new InsertInput().withTableName(TABLE_NAME);

         assertTrue(PermissionsHelper.hasTablePermission(actionInput, TABLE_NAME, TablePermissionSubType.READ));
         assertFalse(PermissionsHelper.hasTablePermission(actionInput, TABLE_NAME, TablePermissionSubType.INSERT));
         assertFalse(PermissionsHelper.hasTablePermission(actionInput, TABLE_NAME, TablePermissionSubType.EDIT));
         assertFalse(PermissionsHelper.hasTablePermission(actionInput, TABLE_NAME, TablePermissionSubType.DELETE));
         PermissionsHelper.checkTablePermissionThrowing(actionInput, TablePermissionSubType.READ);
         assertThatThrownBy(() -> PermissionsHelper.checkTablePermissionThrowing(actionInput, TablePermissionSubType.INSERT)).isInstanceOf(QPermissionDeniedException.class);
         assertThatThrownBy(() -> PermissionsHelper.checkTablePermissionThrowing(actionInput, TablePermissionSubType.EDIT)).isInstanceOf(QPermissionDeniedException.class);
         assertThatThrownBy(() -> PermissionsHelper.checkTablePermissionThrowing(actionInput, TablePermissionSubType.DELETE)).isInstanceOf(QPermissionDeniedException.class);
         assertEquals(PermissionCheckResult.ALLOW, PermissionsHelper.getPermissionCheckResult(actionInput, instance.getTable(TABLE_NAME)));
      }

      {
         QSession session = new QSession().withPermissions(TABLE_NAME + ".write");
         QContext.setQSession(session);

         AbstractTableActionInput actionInput = new InsertInput().withTableName(TABLE_NAME);

         assertFalse(PermissionsHelper.hasTablePermission(actionInput, TABLE_NAME, TablePermissionSubType.READ));
         assertTrue(PermissionsHelper.hasTablePermission(actionInput, TABLE_NAME, TablePermissionSubType.INSERT));
         assertTrue(PermissionsHelper.hasTablePermission(actionInput, TABLE_NAME, TablePermissionSubType.EDIT));
         assertTrue(PermissionsHelper.hasTablePermission(actionInput, TABLE_NAME, TablePermissionSubType.DELETE));
         assertThatThrownBy(() -> PermissionsHelper.checkTablePermissionThrowing(actionInput, TablePermissionSubType.READ)).isInstanceOf(QPermissionDeniedException.class);
         PermissionsHelper.checkTablePermissionThrowing(actionInput, TablePermissionSubType.INSERT);
         PermissionsHelper.checkTablePermissionThrowing(actionInput, TablePermissionSubType.EDIT);
         PermissionsHelper.checkTablePermissionThrowing(actionInput, TablePermissionSubType.DELETE);
         assertEquals(PermissionCheckResult.ALLOW, PermissionsHelper.getPermissionCheckResult(actionInput, instance.getTable(TABLE_NAME)));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableWithReadWriteLevelAndWithPermission() throws QPermissionDeniedException
   {
      QInstance instance = newQInstance();
      instance.setDefaultPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      enrich(instance);

      assertFullTableAccess(instance, new QSession().withPermissions(TABLE_NAME + ".read", TABLE_NAME + ".write"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableWithReadInsertEditDeleteLevelAndWithoutPermission()
   {
      QInstance instance = newQInstance();
      instance.getTable(TABLE_NAME).setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      enrich(instance);

      assertNoTableAccess(instance, new QSession());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableWithReadInsertEditDeleteLevelAndWithPermission() throws QPermissionDeniedException
   {
      QInstance instance = newQInstance();
      instance.getTable(TABLE_NAME).setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      enrich(instance);

      assertFullTableAccess(instance, new QSession().withPermissions(TABLE_NAME + ".read", TABLE_NAME + ".insert", TABLE_NAME + ".edit", TABLE_NAME + ".delete"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableWithReadInsertEditDeleteLevelWithLimitedPermissions() throws QPermissionDeniedException
   {
      QInstance instance = newQInstance();
      instance.setDefaultPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      enrich(instance);

      {
         QSession session = new QSession().withPermissions(TABLE_NAME + ".read");
         QContext.setQSession(session);

         AbstractTableActionInput actionInput = new InsertInput().withTableName(TABLE_NAME);

         assertTrue(PermissionsHelper.hasTablePermission(actionInput, TABLE_NAME, TablePermissionSubType.READ));
         assertFalse(PermissionsHelper.hasTablePermission(actionInput, TABLE_NAME, TablePermissionSubType.INSERT));
         assertFalse(PermissionsHelper.hasTablePermission(actionInput, TABLE_NAME, TablePermissionSubType.EDIT));
         assertFalse(PermissionsHelper.hasTablePermission(actionInput, TABLE_NAME, TablePermissionSubType.DELETE));
         PermissionsHelper.checkTablePermissionThrowing(actionInput, TablePermissionSubType.READ);
         assertThatThrownBy(() -> PermissionsHelper.checkTablePermissionThrowing(actionInput, TablePermissionSubType.INSERT)).isInstanceOf(QPermissionDeniedException.class);
         assertThatThrownBy(() -> PermissionsHelper.checkTablePermissionThrowing(actionInput, TablePermissionSubType.EDIT)).isInstanceOf(QPermissionDeniedException.class);
         assertThatThrownBy(() -> PermissionsHelper.checkTablePermissionThrowing(actionInput, TablePermissionSubType.DELETE)).isInstanceOf(QPermissionDeniedException.class);
         assertEquals(PermissionCheckResult.ALLOW, PermissionsHelper.getPermissionCheckResult(actionInput, instance.getTable(TABLE_NAME)));
      }

      {
         QSession session = new QSession().withPermissions(TABLE_NAME + ".insert");
         QContext.setQSession(session);

         AbstractTableActionInput actionInput = new InsertInput().withTableName(TABLE_NAME);

         assertFalse(PermissionsHelper.hasTablePermission(actionInput, TABLE_NAME, TablePermissionSubType.READ));
         assertTrue(PermissionsHelper.hasTablePermission(actionInput, TABLE_NAME, TablePermissionSubType.INSERT));
         assertFalse(PermissionsHelper.hasTablePermission(actionInput, TABLE_NAME, TablePermissionSubType.EDIT));
         assertFalse(PermissionsHelper.hasTablePermission(actionInput, TABLE_NAME, TablePermissionSubType.DELETE));
         assertThatThrownBy(() -> PermissionsHelper.checkTablePermissionThrowing(actionInput, TablePermissionSubType.READ)).isInstanceOf(QPermissionDeniedException.class);
         PermissionsHelper.checkTablePermissionThrowing(actionInput, TablePermissionSubType.INSERT);
         assertThatThrownBy(() -> PermissionsHelper.checkTablePermissionThrowing(actionInput, TablePermissionSubType.EDIT)).isInstanceOf(QPermissionDeniedException.class);
         assertThatThrownBy(() -> PermissionsHelper.checkTablePermissionThrowing(actionInput, TablePermissionSubType.DELETE)).isInstanceOf(QPermissionDeniedException.class);
         assertEquals(PermissionCheckResult.ALLOW, PermissionsHelper.getPermissionCheckResult(actionInput, instance.getTable(TABLE_NAME)));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testProcessNoPermissionsMetaDataMeansFullAccess() throws QPermissionDeniedException
   {
      QInstance instance = newQInstance();
      enrich(instance);

      QSession session = new QSession();
      QContext.setQSession(session);

      AbstractActionInput actionInput = new AbstractActionInput();
      assertTrue(PermissionsHelper.hasProcessPermission(actionInput, PROCESS_NAME));
      PermissionsHelper.checkProcessPermissionThrowing(actionInput, PROCESS_NAME);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testProcessWithHasAccessLevelAndWithoutPermission()
   {
      QInstance instance = newQInstance();
      instance.getProcess(PROCESS_NAME)
         .setPermissionRules(new QPermissionRules()
            .withLevel(PermissionLevel.HAS_ACCESS_PERMISSION)
            .withDenyBehavior(DenyBehavior.DISABLED)
         );
      enrich(instance);

      QSession session = new QSession();
      QContext.setQSession(session);

      AbstractActionInput actionInput = new AbstractActionInput();
      assertFalse(PermissionsHelper.hasProcessPermission(actionInput, PROCESS_NAME));
      assertThatThrownBy(() -> PermissionsHelper.checkProcessPermissionThrowing(actionInput, PROCESS_NAME)).isInstanceOf(QPermissionDeniedException.class);
      assertEquals(PermissionCheckResult.DENY_DISABLE, PermissionsHelper.getPermissionCheckResult(actionInput, instance.getProcess(PROCESS_NAME)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testProcessWithHasAccessLevelAndWithPermission() throws QPermissionDeniedException
   {
      QInstance instance = newQInstance();
      instance.setDefaultPermissionRules(new QPermissionRules().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
      instance.getProcess(PROCESS_NAME);
      enrich(instance);

      QSession session = new QSession().withPermission(PROCESS_NAME + ".hasAccess");
      QContext.setQSession(session);

      AbstractActionInput actionInput = new AbstractActionInput();
      assertTrue(PermissionsHelper.hasProcessPermission(actionInput, PROCESS_NAME));
      PermissionsHelper.checkProcessPermissionThrowing(actionInput, PROCESS_NAME);
      assertEquals(PermissionCheckResult.ALLOW, PermissionsHelper.getPermissionCheckResult(actionInput, instance.getProcess(PROCESS_NAME)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testThatProcessesUseHasAccessPermissionIfInstanceDefaultIsLowerLevelTableDetails() throws QPermissionDeniedException
   {
      {
         QInstance instance = newQInstance();
         instance.setDefaultPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
         instance.getProcess(PROCESS_NAME);
         enrich(instance);

         QSession session = new QSession().withPermission(PROCESS_NAME + ".hasAccess");
         QContext.setQSession(session);

         AbstractActionInput actionInput = new AbstractActionInput();
         assertTrue(PermissionsHelper.hasProcessPermission(actionInput, PROCESS_NAME));
         PermissionsHelper.checkProcessPermissionThrowing(actionInput, PROCESS_NAME);
         assertEquals(PermissionCheckResult.ALLOW, PermissionsHelper.getPermissionCheckResult(actionInput, instance.getProcess(PROCESS_NAME)));
      }

      {
         QInstance instance = newQInstance();
         instance.setDefaultPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
         instance.getProcess(PROCESS_NAME);
         enrich(instance);

         QSession session = new QSession();
         QContext.setQSession(session);

         AbstractActionInput actionInput = new AbstractActionInput();
         assertFalse(PermissionsHelper.hasProcessPermission(actionInput, PROCESS_NAME));
         assertThatThrownBy(() -> PermissionsHelper.checkProcessPermissionThrowing(actionInput, PROCESS_NAME)).isInstanceOf(QPermissionDeniedException.class);
         assertEquals(PermissionCheckResult.DENY_HIDE, PermissionsHelper.getPermissionCheckResult(actionInput, instance.getProcess(PROCESS_NAME)));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testProcessWithAlternatePermissionName() throws QPermissionDeniedException
   {
      QInstance instance = newQInstance();
      instance.getProcess(PROCESS_NAME)
         .setPermissionRules(new QPermissionRules()
            .withLevel(PermissionLevel.HAS_ACCESS_PERMISSION)
            .withDenyBehavior(instance.getDefaultPermissionRules().getDenyBehavior())
            .withPermissionBaseName("someProcess")
         );
      enrich(instance);

      {
         //////////////////////////////////////////////////////
         // make sure we FAIL with the processName.hasAccess //
         //////////////////////////////////////////////////////
         QSession session = new QSession();
         QContext.setQSession(session);

         AbstractActionInput actionInput = new AbstractActionInput();
         assertFalse(PermissionsHelper.hasProcessPermission(actionInput, PROCESS_NAME));
         assertThatThrownBy(() -> PermissionsHelper.checkProcessPermissionThrowing(actionInput, PROCESS_NAME)).isInstanceOf(QPermissionDeniedException.class);
         assertEquals(PermissionCheckResult.DENY_HIDE, PermissionsHelper.getPermissionCheckResult(actionInput, instance.getProcess(PROCESS_NAME)));
      }

      {
         ////////////////////////////////////////////////////////////////////////
         // make sure we PASS with the override (permissionBaseName).hasAccess //
         ////////////////////////////////////////////////////////////////////////
         QSession session = new QSession().withPermission("someProcess.hasAccess");
         QContext.setQSession(session);

         AbstractActionInput actionInput = new AbstractActionInput();
         assertTrue(PermissionsHelper.hasProcessPermission(actionInput, PROCESS_NAME));
         PermissionsHelper.checkProcessPermissionThrowing(actionInput, PROCESS_NAME);
         assertEquals(PermissionCheckResult.ALLOW, PermissionsHelper.getPermissionCheckResult(actionInput, instance.getProcess(PROCESS_NAME)));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAppNoPermissionsMetaDataMeansFullAccess() throws QPermissionDeniedException
   {
      QInstance instance = newQInstance();
      enrich(instance);

      QSession session = new QSession();
      QContext.setQSession(session);

      AbstractActionInput actionInput = new AbstractActionInput();
      assertTrue(PermissionsHelper.hasAppPermission(actionInput, APP_NAME));
      PermissionsHelper.checkAppPermissionThrowing(actionInput, APP_NAME);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAppWithHasAccessLevelAndWithoutPermission()
   {
      QInstance instance = newQInstance();
      instance.getApp(APP_NAME)
         .setPermissionRules(new QPermissionRules()
            .withLevel(PermissionLevel.HAS_ACCESS_PERMISSION)
            .withDenyBehavior(DenyBehavior.DISABLED)
         );
      enrich(instance);

      QSession session = new QSession();
      QContext.setQSession(session);

      AbstractActionInput actionInput = new AbstractActionInput();
      assertFalse(PermissionsHelper.hasAppPermission(actionInput, APP_NAME));
      assertThatThrownBy(() -> PermissionsHelper.checkAppPermissionThrowing(actionInput, APP_NAME)).isInstanceOf(QPermissionDeniedException.class);
      assertEquals(PermissionCheckResult.DENY_DISABLE, PermissionsHelper.getPermissionCheckResult(actionInput, instance.getApp(APP_NAME)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAppWithHasAccessLevelAndWithPermission() throws QPermissionDeniedException
   {
      QInstance instance = newQInstance();
      instance.setDefaultPermissionRules(new QPermissionRules().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
      instance.getApp(APP_NAME);
      enrich(instance);

      QSession session = new QSession().withPermission(APP_NAME + ".hasAccess");
      QContext.setQSession(session);

      AbstractActionInput actionInput = new AbstractActionInput();
      assertTrue(PermissionsHelper.hasAppPermission(actionInput, APP_NAME));
      PermissionsHelper.checkAppPermissionThrowing(actionInput, APP_NAME);
      assertEquals(PermissionCheckResult.ALLOW, PermissionsHelper.getPermissionCheckResult(actionInput, instance.getApp(APP_NAME)));
   }




   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testReportWithHasAccessLevel() throws QPermissionDeniedException
   {
      QInstance instance = newQInstance();
      instance.getReport(REPORT_NAME)
         .setPermissionRules(new QPermissionRules()
            .withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
      enrich(instance);

      {
         QSession session = new QSession();
         QContext.setQSession(session);

         AbstractActionInput actionInput = new AbstractActionInput();
         assertFalse(PermissionsHelper.hasReportPermission(actionInput, REPORT_NAME));
         assertThatThrownBy(() -> PermissionsHelper.checkReportPermissionThrowing(actionInput, REPORT_NAME)).isInstanceOf(QPermissionDeniedException.class);
         assertEquals(PermissionCheckResult.DENY_HIDE, PermissionsHelper.getPermissionCheckResult(actionInput, instance.getReport(REPORT_NAME)));
      }

      {
         QSession session = new QSession().withPermission(REPORT_NAME + ".hasAccess");
         QContext.setQSession(session);

         AbstractActionInput actionInput = new AbstractActionInput();
         assertTrue(PermissionsHelper.hasReportPermission(actionInput, REPORT_NAME));
         PermissionsHelper.checkReportPermissionThrowing(actionInput, REPORT_NAME);
         assertEquals(PermissionCheckResult.ALLOW, PermissionsHelper.getPermissionCheckResult(actionInput, instance.getReport(REPORT_NAME)));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWidgetWithNoProtection() throws QPermissionDeniedException
   {
      QInstance instance = newQInstance();
      enrich(instance);

      QSession session = new QSession();
      QContext.setQSession(session);

      AbstractActionInput actionInput = new AbstractActionInput();

      assertTrue(PermissionsHelper.hasWidgetPermission(actionInput, WIDGET_NAME));
      PermissionsHelper.checkWidgetPermissionThrowing(actionInput, WIDGET_NAME);
      assertEquals(PermissionCheckResult.ALLOW, PermissionsHelper.getPermissionCheckResult(actionInput, instance.getWidget(WIDGET_NAME)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetAllAvailablePermissionNames()
   {
      {
         QInstance instance = newQInstance();
         instance.setDefaultPermissionRules(new QPermissionRules()
            .withLevel(PermissionLevel.NOT_PROTECTED));
         enrich(instance);
         assertEquals(Set.of(), PermissionsHelper.getAllAvailablePermissionNames(instance));
      }

      {
         QInstance instance = newQInstance();
         instance.setDefaultPermissionRules(new QPermissionRules()
            .withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
         enrich(instance);
         assertEquals(Set.of(TABLE_NAME + ".hasAccess", PROCESS_NAME + ".hasAccess", REPORT_NAME + ".hasAccess", WIDGET_NAME + ".hasAccess", APP_NAME + ".hasAccess"), PermissionsHelper.getAllAvailablePermissionNames(instance));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static void enrich(QInstance instance)
   {
      reInitInstanceInContext(instance);
      new QInstanceEnricher(instance).enrich();
   }



   /*******************************************************************************
    * build a new QInstance with some tables and process convenient for this test
    * and other tests in this package
    *******************************************************************************/
   static QInstance newQInstance()
   {
      QInstance qInstance = new QInstance();

      qInstance.registerAuthenticationProvider(AuthScope.instanceDefault(), new QAuthenticationMetaData()
         .withType(QAuthenticationType.FULLY_ANONYMOUS)
         .withName("anonymous"));

      qInstance.addBackend(new QBackendMetaData()
         .withName("backend"));

      qInstance.addTable(new QTableMetaData()
         .withName(TABLE_NAME)
         .withBackendName("backend")
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER)));

      qInstance.addProcess(new QProcessMetaData()
         .withName(PROCESS_NAME)
         .withStepList(List.of(new QBackendStepMetaData()
            .withCode(new QCodeReference(RunProcessTest.NoopBackendStep.class))
            .withName("noop")
         )));

      qInstance.addApp(new QAppMetaData()
         .withName(APP_NAME));

      qInstance.addReport(new QReportMetaData()
         .withName(REPORT_NAME)
         .withDataSource(new QReportDataSource().withSourceTable(TABLE_NAME))
         .withView(new QReportView().withType(ReportType.TABLE).withColumn(new QReportField("id"))));

      qInstance.addWidget(new QWidgetMetaData()
         .withName(WIDGET_NAME)
         .withCodeReference(new QCodeReference(WidgetRenderer.class))
      );

      return (qInstance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class WidgetRenderer extends AbstractWidgetRenderer
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public RenderWidgetOutput render(RenderWidgetInput input) throws QException
      {
         return null;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static void assertFullTableAccess(QInstance instance, QSession session) throws QPermissionDeniedException
   {
      QContext.setQSession(session);
      AbstractTableActionInput actionInput = new InsertInput().withTableName(TABLE_NAME);

      for(TablePermissionSubType permissionSubType : TablePermissionSubType.values())
      {
         assertTrue(PermissionsHelper.hasTablePermission(actionInput, TABLE_NAME, permissionSubType), "Expected to have permission " + TABLE_NAME + ":" + permissionSubType);
         PermissionsHelper.checkTablePermissionThrowing(actionInput, permissionSubType);
      }

      assertEquals(PermissionCheckResult.ALLOW, PermissionsHelper.getPermissionCheckResult(actionInput, instance.getTable(TABLE_NAME)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static void assertNoTableAccess(QInstance instance, QSession session)
   {
      QContext.setQSession(session);
      AbstractTableActionInput actionInput = new InsertInput().withTableName(TABLE_NAME);

      for(TablePermissionSubType permissionSubType : TablePermissionSubType.values())
      {
         assertFalse(PermissionsHelper.hasTablePermission(actionInput, TABLE_NAME, permissionSubType));
         assertThatThrownBy(() -> PermissionsHelper.checkTablePermissionThrowing(actionInput, permissionSubType))
            .isExactlyInstanceOf(QPermissionDeniedException.class);
      }

      assertEquals(PermissionCheckResult.DENY_HIDE, PermissionsHelper.getPermissionCheckResult(actionInput, instance.getTable(TABLE_NAME)));
   }

}