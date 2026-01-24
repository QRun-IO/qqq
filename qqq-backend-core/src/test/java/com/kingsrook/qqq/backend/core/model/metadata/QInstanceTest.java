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

package com.kingsrook.qqq.backend.core.model.metadata;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.DenyBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeVoidVoidMethod;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for QInstance
 *******************************************************************************/
class QInstanceTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetTablePath() throws QException
   {
      QInstance qInstance = QContext.getQInstance();

      GetInput getInput = new GetInput();

      String tablePath = qInstance.getTablePath(TestUtils.TABLE_NAME_PERSON);
      assertEquals("/peopleApp/person", tablePath);

      ////////////////////////////////////////////////////////////////////////////////////////
      // call again (to make sure getting from memoization works - verify w/ breakpoint...) //
      ////////////////////////////////////////////////////////////////////////////////////////
      tablePath = qInstance.getTablePath(TestUtils.TABLE_NAME_PERSON);
      assertEquals("/peopleApp/person", tablePath);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetTablePathPermissionsAndMemoization() throws QException
   {
      String expectedPath = "/peopleApp/person";

      UnsafeVoidVoidMethod<QException> setupInstance = () ->
      {
         QInstance qInstance = TestUtils.defineInstance();
         for(QTableMetaData table : qInstance.getTables().values())
         {
            table.withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION).withDenyBehavior(DenyBehavior.HIDDEN));
         }
         QContext.setQInstance(qInstance);
      };

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // make a new instance - call the method as a user with permissions, then as one without - and should get the result either way. //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      setupInstance.run();

      ///////////////////////////////////////
      // with permissions, should get path //
      ///////////////////////////////////////
      QContext.setQSession(new QSession().withPermission(TestUtils.TABLE_NAME_PERSON + ".hasAccess"));
      assertEquals(expectedPath, QContext.getQInstance().getTablePath(TestUtils.TABLE_NAME_PERSON));

      ///////////////////////////////////////////////
      // with no permissions, should still get it! //
      ///////////////////////////////////////////////
      QContext.setQSession(new QSession());
      assertEquals(expectedPath, QContext.getQInstance().getTablePath(TestUtils.TABLE_NAME_PERSON));

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // now re-set the instance, and this time run in the opposite order - this is where we used to memoize a null because of a non-permission //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      setupInstance.run();

      ///////////////////////////////////////////////
      // with no permissions, should still get it! //
      ///////////////////////////////////////////////
      QContext.setQSession(new QSession());
      assertEquals(expectedPath, QContext.getQInstance().getTablePath(TestUtils.TABLE_NAME_PERSON));

      ///////////////////////////////////////
      // with permissions, should get path //
      ///////////////////////////////////////
      QContext.setQSession(new QSession().withPermission(TestUtils.TABLE_NAME_PERSON + ".hasAccess"));
      assertEquals(expectedPath, QContext.getQInstance().getTablePath(TestUtils.TABLE_NAME_PERSON));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetTablePathNotInAnyApp() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      qInstance.addTable(new QTableMetaData().withName("aTableInNoApps"));

      /////////////////////////////////////////////
      // first run for a non-existing table name //
      /////////////////////////////////////////////
      String tablePath = qInstance.getTablePath("notATable");
      assertNull(tablePath);

      ////////////////////////////////////////////////////////////////////////////////////////
      // call again (to make sure getting from memoization works - verify w/ breakpoint...) //
      ////////////////////////////////////////////////////////////////////////////////////////
      tablePath = qInstance.getTablePath("notATable");
      assertNull(tablePath);

      //////////////////////////////////////
      // now run for our table-in-no-apps //
      //////////////////////////////////////
      tablePath = qInstance.getTablePath("aTableInNoApps");
      assertNull(tablePath);

      ////////////////////////////////////////////////////////////////////////////////////////
      // call again (to make sure getting from memoization works - verify w/ breakpoint...) //
      ////////////////////////////////////////////////////////////////////////////////////////
      tablePath = qInstance.getTablePath("aTableInNoApps");
      assertNull(tablePath);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableInMultipleApps() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();

      QAppMetaData otherApp = new QAppMetaData()
         .withName("otherApp")
         .withChild(qInstance.getTable(TestUtils.TABLE_NAME_PERSON));
      qInstance.addApp(otherApp);
      otherApp.setChildAppAffinity(TestUtils.TABLE_NAME_PERSON, 2);

      QAppMetaData extraApp = new QAppMetaData()
         .withName("extraApp")
         .withChild(qInstance.getTable(TestUtils.TABLE_NAME_PERSON));
      qInstance.addApp(extraApp);
      extraApp.setChildAppAffinity(TestUtils.TABLE_NAME_PERSON, 1);

      reInitInstanceInContext(qInstance);

      String personTablePath = qInstance.getTablePath(TestUtils.TABLE_NAME_PERSON);
      assertEquals("/otherApp/person", personTablePath);
   }

}
