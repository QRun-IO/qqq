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

package com.kingsrook.qqq.backend.core.actions.permissions;


import java.util.Collection;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.metadata.MetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for UseOtherPermissionNameCustomPermissionChecker 
 *******************************************************************************/
class UseOtherPermissionNameCustomPermissionCheckerTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableHasAccessLevel() throws QPermissionDeniedException
   {
      String   tableName = PermissionsHelperTest.TABLE_NAME;
      QSession session;

      {
         ////////////////////////
         // establish baseline //
         ////////////////////////
         QInstance instance = PermissionsHelperTest.newQInstance();
         instance.getTable(tableName).setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
         PermissionsHelperTest.enrich(instance);

         session = new QSession().withPermissions(tableName + ".hasAccess");
         PermissionsHelperTest.assertFullTableAccess(instance, session);
      }

      {
         ////////////////////////////////////////////////////////////////
         // in this instance make table have custom permission checker //
         ////////////////////////////////////////////////////////////////
         QInstance instance = PermissionsHelperTest.newQInstance();
         instance.getTable(tableName).setPermissionRules(new QPermissionRules()
            .withLevel(PermissionLevel.HAS_ACCESS_PERMISSION)
            .withCustomPermissionChecker(UseOtherPermissionNameCustomPermissionChecker.build("somePermission")));
         PermissionsHelperTest.enrich(instance);

         session = new QSession();
         PermissionsHelperTest.assertNoTableAccess(instance, session);

         session = new QSession().withPermissions("someOtherPermission");
         PermissionsHelperTest.assertNoTableAccess(instance, session);

         session = new QSession().withPermissions(tableName + ".hasAccess");
         PermissionsHelperTest.assertNoTableAccess(instance, session);

         session = new QSession().withPermissions("somePermission");
         PermissionsHelperTest.assertFullTableAccess(instance, session);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableReadWrite() throws QPermissionDeniedException
   {
      String   tableName = PermissionsHelperTest.TABLE_NAME;
      QSession session;

      {
         ////////////////////////
         // establish baseline //
         ////////////////////////
         QInstance instance = PermissionsHelperTest.newQInstance();
         instance.getTable(tableName).setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
         PermissionsHelperTest.enrich(instance);

         session = new QSession().withPermissions(tableName + ".read", tableName + ".write");
         PermissionsHelperTest.assertFullTableAccess(instance, session);
      }

      {
         ////////////////////////////////////////////////////////////////
         // in this instance make table have custom permission checker //
         ////////////////////////////////////////////////////////////////
         QInstance instance = PermissionsHelperTest.newQInstance();
         instance.getTable(tableName).setPermissionRules(new QPermissionRules()
            .withLevel(PermissionLevel.READ_WRITE_PERMISSIONS)
            .withCustomPermissionChecker(UseOtherPermissionNameCustomPermissionChecker.build("somePermission")));
         PermissionsHelperTest.enrich(instance);

         session = new QSession();
         PermissionsHelperTest.assertNoTableAccess(instance, session);

         session = new QSession().withPermissions("someOtherPermission");
         PermissionsHelperTest.assertNoTableAccess(instance, session);

         session = new QSession().withPermissions(tableName + ".read", tableName + ".write");
         PermissionsHelperTest.assertNoTableAccess(instance, session);

         session = new QSession().withPermissions("somePermission");
         PermissionsHelperTest.assertFullTableAccess(instance, session);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableReadInsertEditDeletePermissions() throws QPermissionDeniedException
   {
      String   tableName = PermissionsHelperTest.TABLE_NAME;
      QSession session;

      {
         ////////////////////////
         // establish baseline //
         ////////////////////////
         QInstance instance = PermissionsHelperTest.newQInstance();
         instance.getTable(tableName).setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
         PermissionsHelperTest.enrich(instance);

         session = new QSession().withPermissions(tableName + ".read", tableName + ".insert", tableName + ".edit", tableName + ".delete");
         PermissionsHelperTest.assertFullTableAccess(instance, session);
      }

      {
         ////////////////////////////////////////////////////////////////
         // in this instance make table have custom permission checker //
         ////////////////////////////////////////////////////////////////
         QInstance instance = PermissionsHelperTest.newQInstance();
         instance.getTable(tableName).setPermissionRules(new QPermissionRules()
            .withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS)
            .withCustomPermissionChecker(UseOtherPermissionNameCustomPermissionChecker.build("somePermission")));
         PermissionsHelperTest.enrich(instance);

         session = new QSession();
         PermissionsHelperTest.assertNoTableAccess(instance, session);

         session = new QSession().withPermissions("someOtherPermission");
         PermissionsHelperTest.assertNoTableAccess(instance, session);

         session = new QSession().withPermissions(tableName + ".read", tableName + ".insert", tableName + ".edit", tableName + ".delete");
         PermissionsHelperTest.assertNoTableAccess(instance, session);

         session = new QSession().withPermissions("somePermission");
         PermissionsHelperTest.assertFullTableAccess(instance, session);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testProcessHasAccessLevel()
   {
      String          processName = PermissionsHelperTest.PROCESS_NAME;
      RunProcessInput actionInput = new RunProcessInput().withProcessName(processName);

      {
         ////////////////////////
         // establish baseline //
         ////////////////////////
         QInstance instance = PermissionsHelperTest.newQInstance();
         instance.getProcess(processName).setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
         PermissionsHelperTest.enrich(instance);

         QContext.setQSession(new QSession().withPermissions(processName + ".hasAccess"));
         assertTrue(PermissionsHelper.hasProcessPermission(actionInput, processName));
      }

      {
         //////////////////////////////////////////////////////////////////
         // in this instance make process have custom permission checker //
         //////////////////////////////////////////////////////////////////
         QInstance instance = PermissionsHelperTest.newQInstance();
         instance.getProcess(processName).setPermissionRules(new QPermissionRules()
            .withLevel(PermissionLevel.HAS_ACCESS_PERMISSION)
            .withCustomPermissionChecker(UseOtherPermissionNameCustomPermissionChecker.build("somePermission")));
         PermissionsHelperTest.enrich(instance);

         QContext.setQSession(new QSession());
         assertFalse(PermissionsHelper.hasProcessPermission(actionInput, processName));

         QContext.setQSession(new QSession().withPermissions("someOtherPermission"));
         assertFalse(PermissionsHelper.hasProcessPermission(actionInput, processName));

         QContext.setQSession(new QSession().withPermissions(processName + ".hasAccess"));
         assertFalse(PermissionsHelper.hasProcessPermission(actionInput, processName));

         QContext.setQSession(new QSession().withPermissions("somePermission"));
         assertTrue(PermissionsHelper.hasProcessPermission(actionInput, processName));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAppHasAccessLevel()
   {
      String        appName     = PermissionsHelperTest.APP_NAME;
      MetaDataInput actionInput = new MetaDataInput();

      {
         ////////////////////////
         // establish baseline //
         ////////////////////////
         QInstance instance = PermissionsHelperTest.newQInstance();
         instance.getApp(appName).setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
         PermissionsHelperTest.enrich(instance);

         QContext.setQSession(new QSession().withPermissions(appName + ".hasAccess"));
         assertTrue(PermissionsHelper.hasAppPermission(actionInput, appName));
      }

      {
         //////////////////////////////////////////////////////////////
         // in this instance make app have custom permission checker //
         //////////////////////////////////////////////////////////////
         QInstance instance = PermissionsHelperTest.newQInstance();
         instance.getApp(appName).setPermissionRules(new QPermissionRules()
            .withLevel(PermissionLevel.HAS_ACCESS_PERMISSION)
            .withCustomPermissionChecker(UseOtherPermissionNameCustomPermissionChecker.build("somePermission")));
         PermissionsHelperTest.enrich(instance);

         QContext.setQSession(new QSession());
         assertFalse(PermissionsHelper.hasAppPermission(actionInput, appName));

         QContext.setQSession(new QSession().withPermissions("someOtherPermission"));
         assertFalse(PermissionsHelper.hasAppPermission(actionInput, appName));

         QContext.setQSession(new QSession().withPermissions(appName + ".hasAccess"));
         assertFalse(PermissionsHelper.hasAppPermission(actionInput, appName));

         QContext.setQSession(new QSession().withPermissions("somePermission"));
         assertTrue(PermissionsHelper.hasAppPermission(actionInput, appName));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWidgetHasAccessLevel()
   {
      String        widgetName  = PermissionsHelperTest.WIDGET_NAME;
      MetaDataInput actionInput = new MetaDataInput();

      {
         ////////////////////////
         // establish baseline //
         ////////////////////////
         QInstance instance = PermissionsHelperTest.newQInstance();
         instance.getWidget(widgetName).setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
         PermissionsHelperTest.enrich(instance);

         QContext.setQSession(new QSession().withPermissions(widgetName + ".hasAccess"));
         assertTrue(PermissionsHelper.hasWidgetPermission(actionInput, widgetName));
      }

      {
         //////////////////////////////////////////////////////////////
         // in this instance make widget have custom permission checker //
         //////////////////////////////////////////////////////////////
         QInstance instance = PermissionsHelperTest.newQInstance();
         instance.getWidget(widgetName).setPermissionRules(new QPermissionRules()
            .withLevel(PermissionLevel.HAS_ACCESS_PERMISSION)
            .withCustomPermissionChecker(UseOtherPermissionNameCustomPermissionChecker.build("somePermission")));
         PermissionsHelperTest.enrich(instance);

         QContext.setQSession(new QSession());
         assertFalse(PermissionsHelper.hasWidgetPermission(actionInput, widgetName));

         QContext.setQSession(new QSession().withPermissions("someOtherPermission"));
         assertFalse(PermissionsHelper.hasWidgetPermission(actionInput, widgetName));

         QContext.setQSession(new QSession().withPermissions(widgetName + ".hasAccess"));
         assertFalse(PermissionsHelper.hasWidgetPermission(actionInput, widgetName));

         QContext.setQSession(new QSession().withPermissions("somePermission"));
         assertTrue(PermissionsHelper.hasWidgetPermission(actionInput, widgetName));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testReportHasAccessLevel()
   {
      String        reportName  = PermissionsHelperTest.REPORT_NAME;
      MetaDataInput actionInput = new MetaDataInput();

      {
         ////////////////////////
         // establish baseline //
         ////////////////////////
         QInstance instance = PermissionsHelperTest.newQInstance();
         instance.getReport(reportName).setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
         PermissionsHelperTest.enrich(instance);

         QContext.setQSession(new QSession().withPermissions(reportName + ".hasAccess"));
         assertTrue(PermissionsHelper.hasReportPermission(actionInput, reportName));
      }

      {
         /////////////////////////////////////////////////////////////////
         // in this instance make report have custom permission checker //
         /////////////////////////////////////////////////////////////////
         QInstance instance = PermissionsHelperTest.newQInstance();
         instance.getReport(reportName).setPermissionRules(new QPermissionRules()
            .withLevel(PermissionLevel.HAS_ACCESS_PERMISSION)
            .withCustomPermissionChecker(UseOtherPermissionNameCustomPermissionChecker.build("somePermission")));
         PermissionsHelperTest.enrich(instance);

         QContext.setQSession(new QSession());
         assertFalse(PermissionsHelper.hasReportPermission(actionInput, reportName));

         QContext.setQSession(new QSession().withPermissions("someOtherPermission"));
         assertFalse(PermissionsHelper.hasReportPermission(actionInput, reportName));

         QContext.setQSession(new QSession().withPermissions(reportName + ".hasAccess"));
         assertFalse(PermissionsHelper.hasReportPermission(actionInput, reportName));

         QContext.setQSession(new QSession().withPermissions("somePermission"));
         assertTrue(PermissionsHelper.hasReportPermission(actionInput, reportName));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetAvailablePermissionNames()
   {
      //////////////////////////////////////////////////////////////////////////////////
      // test each permission level for the table - with the custom checker each time //
      //////////////////////////////////////////////////////////////////////////////////
      for(PermissionLevel permissionLevel : PermissionLevel.values())
      {
         QInstance instance = PermissionsHelperTest.newQInstance();
         instance.getTable(PermissionsHelperTest.TABLE_NAME).setPermissionRules(new QPermissionRules()
            .withLevel(permissionLevel)
            .withCustomPermissionChecker(UseOtherPermissionNameCustomPermissionChecker.build("somePermission")));
         PermissionsHelperTest.enrich(instance);

         Collection<String> allAvailablePermissionNames = PermissionsHelper.getAllAvailablePermissionNames(instance);
         assertThat(allAvailablePermissionNames)
            .doesNotContain(PermissionsHelperTest.TABLE_NAME + ".hasAccess")
            .doesNotContain(PermissionsHelperTest.TABLE_NAME + ".read")
            .doesNotContain(PermissionsHelperTest.TABLE_NAME + ".write")
            .doesNotContain(PermissionsHelperTest.TABLE_NAME + ".insert");

         if(PermissionLevel.NOT_PROTECTED.equals(permissionLevel))
         {
            assertThat(allAvailablePermissionNames).doesNotContain("somePermission");
         }
         else
         {
            assertThat(allAvailablePermissionNames).contains("somePermission");
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetAvailablePermissions()
   {
      //////////////////////////////////////////////////////////////////////////////////
      // test each permission level for the table - with the custom checker each time //
      //////////////////////////////////////////////////////////////////////////////////
      for(PermissionLevel permissionLevel : PermissionLevel.values())
      {
         QInstance instance = PermissionsHelperTest.newQInstance();
         instance.getTable(PermissionsHelperTest.TABLE_NAME).setPermissionRules(new QPermissionRules()
            .withLevel(permissionLevel)
            .withCustomPermissionChecker(UseOtherPermissionNameCustomPermissionChecker.build("somePermission")));
         PermissionsHelperTest.enrich(instance);

         Collection<AvailablePermission> allAvailablePermissions = PermissionsHelper.getAllAvailablePermissions(instance);

         int expectedNo = switch(permissionLevel)
         {
            case NOT_PROTECTED -> 0;
            case HAS_ACCESS_PERMISSION -> 1;
            case READ_WRITE_PERMISSIONS -> 2;
            case READ_INSERT_EDIT_DELETE_PERMISSIONS -> 4;
         };
         assertEquals(expectedNo, allAvailablePermissions.size());
         assertThat(allAvailablePermissions).allMatch(ap -> "somePermission".equals(ap.getName()));
      }
   }

}