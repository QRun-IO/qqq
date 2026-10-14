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


import java.util.Set;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.session.QCredentialScope;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Credential scopes narrow - never extend - the user's live permissions.
 *******************************************************************************/
class CredentialScopePermissionsTest extends BaseTest
{
   private static final String PERSON = TestUtils.TABLE_NAME_PERSON_MEMORY;
   private static final String SHAPE  = TestUtils.TABLE_NAME_SHAPE;



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testScopeIntersectsWithPermissions() throws Exception
   {
      QInstance  qInstance = TestUtils.defineInstance();
      QueryInput input     = new QueryInput(PERSON);

      ///////////////////////////////////////////////////////////////////
      // scoped to person, read-only: person readable, not writable;   //
      // shape not readable; processes denied (none listed)            //
      ///////////////////////////////////////////////////////////////////
      QCredentialScope scope = new QCredentialScope().withCredentialId("t1").withReadOnly(true).withTables(Set.of(PERSON));
      QContext.init(qInstance, new QSession().withCredentialScope(scope));
      assertTrue(PermissionsHelper.hasTablePermission(input, PERSON, TablePermissionSubType.READ));
      assertFalse(PermissionsHelper.hasTablePermission(input, PERSON, TablePermissionSubType.INSERT));
      assertFalse(PermissionsHelper.hasTablePermission(input, PERSON, TablePermissionSubType.EDIT));
      assertFalse(PermissionsHelper.hasTablePermission(input, PERSON, TablePermissionSubType.DELETE));
      assertFalse(PermissionsHelper.hasTablePermission(input, SHAPE, TablePermissionSubType.READ));
      assertEquals(PermissionCheckResult.DENY_HIDE, PermissionsHelper.getPermissionCheckResult(input, qInstance.getTable(SHAPE)));
      String someProcess = qInstance.getProcesses().keySet().iterator().next();
      assertFalse(PermissionsHelper.hasProcessPermission(input, someProcess));

      //////////////////////////////////////////////////////////
      // a listed process is allowed even when read-only      //
      //////////////////////////////////////////////////////////
      QContext.init(qInstance, new QSession().withCredentialScope(scope.clone().withProcesses(Set.of(someProcess))));
      assertTrue(PermissionsHelper.hasProcessPermission(input, someProcess));

      ////////////////////////////////////////////////////////////////////////////
      // the scope never grants: a protected table the user lacks stays denied //
      ////////////////////////////////////////////////////////////////////////////
      qInstance.getTable(PERSON).setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      QContext.init(qInstance, new QSession().withCredentialScope(new QCredentialScope().withTables(Set.of(PERSON))));
      assertFalse(PermissionsHelper.hasTablePermission(input, PERSON, TablePermissionSubType.READ));
      QContext.init(qInstance, new QSession().withPermissions(PERSON + ".read").withCredentialScope(new QCredentialScope().withTables(Set.of(PERSON))));
      assertTrue(PermissionsHelper.hasTablePermission(input, PERSON, TablePermissionSubType.READ));

      //////////////////////////////////////////
      // no scope: unrestricted (as before)   //
      //////////////////////////////////////////
      QContext.init(qInstance, new QSession());
      assertTrue(PermissionsHelper.hasTablePermission(input, SHAPE, TablePermissionSubType.READ));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testCloneAndChannel() throws Exception
   {
      QCredentialScope scope   = new QCredentialScope().withCredentialId("t2").withTables(Set.of("a"));
      QSession         session = new QSession().withCredentialScope(scope);
      QSession         clone   = session.clone();
      assertNotSame(scope, clone.getCredentialScope());
      clone.getCredentialScope().getTables().add("b");
      assertFalse(scope.getTables().contains("b"));
      assertEquals("token:t2", scope.getClientChannel());
      assertEquals("token", new QSession().withClientChannel("token:t2").getClientChannelName());
      assertTrue(QCredentialScope.allows(null, "anything"));
      assertFalse(QCredentialScope.allows(Set.of("a"), null));
   }
}
