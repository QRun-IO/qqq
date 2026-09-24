/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.MetaDataWithPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Unit tests for BulkTableActionProcessPermissionChecker.
 **
 ** The class parses process names of the form "tableName.bulkAction" and maps
 ** bulkInsert → INSERT, bulkEdit/bulkEditWithFile → EDIT, bulkDelete → DELETE.
 *******************************************************************************/
class BulkTableActionProcessPermissionCheckerTest extends BaseTest
{
   private static final String TABLE_NAME = "testTable";



   /*******************************************************************************
    ** bulkInsert with WRITE permission granted does not throw.
    ** Note: READ_WRITE_PERMISSIONS level maps INSERT/EDIT/DELETE → "write".
    *******************************************************************************/
   @Test
   void testCheckPermissions_bulkInsert_withInsertPermission_passes() throws QPermissionDeniedException
   {
      QInstance instance = buildInstanceWithTablePermissionLevel(PermissionLevel.READ_WRITE_PERMISSIONS);
      QContext.setQSession(new QSession().withPermission(TABLE_NAME + ".write"));

      BulkTableActionProcessPermissionChecker checker = new BulkTableActionProcessPermissionChecker();
      checker.checkPermissionsThrowing(new AbstractActionInput(), namedProcess(TABLE_NAME + ".bulkInsert"));
   }



   /*******************************************************************************
    ** bulkInsert without INSERT permission throws QPermissionDeniedException.
    *******************************************************************************/
   @Test
   void testCheckPermissions_bulkInsert_withoutInsertPermission_throws()
   {
      buildInstanceWithTablePermissionLevel(PermissionLevel.READ_WRITE_PERMISSIONS);
      QContext.setQSession(new QSession()); // no permissions

      BulkTableActionProcessPermissionChecker checker = new BulkTableActionProcessPermissionChecker();
      assertThatThrownBy(() -> checker.checkPermissionsThrowing(new AbstractActionInput(), namedProcess(TABLE_NAME + ".bulkInsert")))
         .isInstanceOf(QPermissionDeniedException.class);
   }



   /*******************************************************************************
    ** bulkEdit with WRITE permission granted does not throw.
    ** Note: READ_WRITE_PERMISSIONS level maps EDIT → "write".
    *******************************************************************************/
   @Test
   void testCheckPermissions_bulkEdit_withEditPermission_passes() throws QPermissionDeniedException
   {
      buildInstanceWithTablePermissionLevel(PermissionLevel.READ_WRITE_PERMISSIONS);
      QContext.setQSession(new QSession().withPermission(TABLE_NAME + ".write"));

      BulkTableActionProcessPermissionChecker checker = new BulkTableActionProcessPermissionChecker();
      checker.checkPermissionsThrowing(new AbstractActionInput(), namedProcess(TABLE_NAME + ".bulkEdit"));
   }



   /*******************************************************************************
    ** bulkEditWithFile routes to the same EDIT check as bulkEdit.
    ** Note: READ_WRITE_PERMISSIONS level maps EDIT → "write".
    *******************************************************************************/
   @Test
   void testCheckPermissions_bulkEditWithFile_withEditPermission_passes() throws QPermissionDeniedException
   {
      buildInstanceWithTablePermissionLevel(PermissionLevel.READ_WRITE_PERMISSIONS);
      QContext.setQSession(new QSession().withPermission(TABLE_NAME + ".write"));

      BulkTableActionProcessPermissionChecker checker = new BulkTableActionProcessPermissionChecker();
      checker.checkPermissionsThrowing(new AbstractActionInput(), namedProcess(TABLE_NAME + ".bulkEditWithFile"));
   }



   /*******************************************************************************
    ** bulkDelete with WRITE permission granted does not throw.
    ** Note: READ_WRITE_PERMISSIONS level maps DELETE → "write".
    *******************************************************************************/
   @Test
   void testCheckPermissions_bulkDelete_withDeletePermission_passes() throws QPermissionDeniedException
   {
      buildInstanceWithTablePermissionLevel(PermissionLevel.READ_WRITE_PERMISSIONS);
      QContext.setQSession(new QSession().withPermission(TABLE_NAME + ".write"));

      BulkTableActionProcessPermissionChecker checker = new BulkTableActionProcessPermissionChecker();
      checker.checkPermissionsThrowing(new AbstractActionInput(), namedProcess(TABLE_NAME + ".bulkDelete"));
   }



   /*******************************************************************************
    ** bulkDelete without DELETE permission throws QPermissionDeniedException.
    *******************************************************************************/
   @Test
   void testCheckPermissions_bulkDelete_withoutDeletePermission_throws()
   {
      buildInstanceWithTablePermissionLevel(PermissionLevel.READ_WRITE_PERMISSIONS);
      QContext.setQSession(new QSession()); // no permissions

      BulkTableActionProcessPermissionChecker checker = new BulkTableActionProcessPermissionChecker();
      assertThatThrownBy(() -> checker.checkPermissionsThrowing(new AbstractActionInput(), namedProcess(TABLE_NAME + ".bulkDelete")))
         .isInstanceOf(QPermissionDeniedException.class);
   }



   /*******************************************************************************
    ** An unrecognised bulk action name (e.g. "bulkFoo") logs a warning but does
    ** NOT throw — the checker has no permission to enforce in that case.
    *******************************************************************************/
   @Test
   void testCheckPermissions_unknownBulkAction_doesNotThrow()
   {
      buildInstanceWithTablePermissionLevel(PermissionLevel.READ_WRITE_PERMISSIONS);
      QContext.setQSession(new QSession()); // no permissions

      BulkTableActionProcessPermissionChecker checker = new BulkTableActionProcessPermissionChecker();
      assertThatCode(() -> checker.checkPermissionsThrowing(new AbstractActionInput(), namedProcess(TABLE_NAME + ".bulkFoo")))
         .doesNotThrowAnyException();
   }



   /*******************************************************************************
    ** A process name without a dot is ignored entirely — no permission check runs.
    *******************************************************************************/
   @Test
   void testCheckPermissions_noDotInProcessName_doesNotThrow()
   {
      buildInstanceWithTablePermissionLevel(PermissionLevel.READ_WRITE_PERMISSIONS);
      QContext.setQSession(new QSession()); // no permissions

      BulkTableActionProcessPermissionChecker checker = new BulkTableActionProcessPermissionChecker();
      assertThatCode(() -> checker.checkPermissionsThrowing(new AbstractActionInput(), namedProcess("noDotHere")))
         .doesNotThrowAnyException();
   }



   /*******************************************************************************
    ** NOT_PROTECTED table means all bulk operations pass regardless of session.
    *******************************************************************************/
   @Test
   void testCheckPermissions_notProtectedTable_allBulkActionsPass() throws QPermissionDeniedException
   {
      buildInstanceWithTablePermissionLevel(PermissionLevel.NOT_PROTECTED);
      QContext.setQSession(new QSession()); // no permissions needed

      BulkTableActionProcessPermissionChecker checker = new BulkTableActionProcessPermissionChecker();
      checker.checkPermissionsThrowing(new AbstractActionInput(), namedProcess(TABLE_NAME + ".bulkInsert"));
      checker.checkPermissionsThrowing(new AbstractActionInput(), namedProcess(TABLE_NAME + ".bulkEdit"));
      checker.checkPermissionsThrowing(new AbstractActionInput(), namedProcess(TABLE_NAME + ".bulkDelete"));
   }



   /*******************************************************************************
    ** Builds a QInstance with testTable at the given permission level and loads
    ** it into QContext.
    *******************************************************************************/
   private QInstance buildInstanceWithTablePermissionLevel(PermissionLevel level)
   {
      QInstance instance = new QInstance();

      instance.registerAuthenticationProvider(AuthScope.instanceDefault(), new QAuthenticationMetaData()
         .withType(QAuthenticationType.FULLY_ANONYMOUS)
         .withName("anonymous"));

      instance.addBackend(new QBackendMetaData().withName("backend"));

      instance.addTable(new QTableMetaData()
         .withName(TABLE_NAME)
         .withBackendName("backend")
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withPermissionRules(new QPermissionRules().withLevel(level)));

      reInitInstanceInContext(instance);
      new QInstanceEnricher(instance).enrich();
      return instance;
   }



   /*******************************************************************************
    ** Minimal MetaDataWithPermissionRules implementation that returns a fixed name.
    *******************************************************************************/
   private MetaDataWithPermissionRules namedProcess(String name)
   {
      return new MetaDataWithPermissionRules()
      {
         @Override
         public String getName()
         {
            return name;
         }


         @Override
         public void setName(String name)
         {
         }


         @Override
         public String getLabel()
         {
            return name;
         }


         @Override
         public void setLabel(String label)
         {
         }


         @Override
         public QPermissionRules getPermissionRules()
         {
            return null;
         }


         @Override
         public void setPermissionRules(QPermissionRules permissionRules)
         {
         }
      };
   }

}
