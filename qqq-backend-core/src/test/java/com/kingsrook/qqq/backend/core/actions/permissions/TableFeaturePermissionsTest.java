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


import java.util.HashSet;
import java.util.Set;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.TableFeaturePermission;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.TableFeaturePermissionsMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Generic permissions work without collaboration metadata or registrations.
 *******************************************************************************/
class TableFeaturePermissionsTest extends BaseTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void explicitAnalyzeGrantRequiresReadAndUsesEffectiveBaseName()
   {
      QInstance instance = QContext.getQInstance();
      QTableMetaData table = protectedTable();
      assertNull(TableFeaturePermissionsMetaData.of(instance));
      assertFalse(PermissionsHelper.hasTableFeaturePermission(table, "analyze"));
      TableFeaturePermissionsMetaData metadata = TableFeaturePermissionsMetaData.ofOrWithNew(instance)
         .withPermission(new TableFeaturePermission().withSuffix("analyze").withLabel("Analyze"));
      assertSame(metadata, TableFeaturePermissionsMetaData.ofOrWithNew(instance));
      assertThat(PermissionsHelper.getAllAvailablePermissions(instance)).anySatisfy(permission ->
      {
         assertThat(permission.getName()).isEqualTo("purchase.analyze");
         assertThat(permission.getObjectName()).isEqualTo(table.getLabel());
         assertThat(permission.getObjectType()).isEqualTo("Table");
         assertThat(permission.getPermissionType()).isEqualTo("Analyze");
      });
      for(Set<String> permissions : Set.of(Set.<String>of(), Set.of("purchase.read"), Set.of("purchase.analyze"), Set.of("purchase.read", "purchase.edit")))
      {
         QContext.getQSession().setPermissions(permissions);
         assertFalse(PermissionsHelper.hasTableFeaturePermission(table, "analyze"));
      }
      QContext.getQSession().setPermissions(Set.of("purchase.read", "purchase.analyze"));
      assertTrue(PermissionsHelper.hasTableFeaturePermission(table, "analyze"));
      assertFalse(PermissionsHelper.hasTableFeaturePermission(table, "unregistered"));
      table.setIsHidden(true);
      assertThat(PermissionsHelper.getAllAvailablePermissionNames(instance)).doesNotContain("purchase.analyze");
      assertFalse(PermissionsHelper.hasTableFeaturePermission(table, "analyze"));
      table.setIsHidden(false);
      table.getPermissionRules().setLevel(PermissionLevel.NOT_PROTECTED);
      QContext.getQSession().setPermissions(Set.of());
      assertTrue(PermissionsHelper.hasTableFeaturePermission(table, "analyze"));
      assertFalse(PermissionsHelper.hasTableFeaturePermission(table, "unregistered"));
   }



   /*******************************************************************************
    ** The explicit instance/session overload checks the given session, not
    ** QContext's, and leaves QContext unchanged.
    *******************************************************************************/
   @Test
   void explicitSessionOverload()
   {
      QInstance      instance = QContext.getQInstance();
      QTableMetaData table    = protectedTable();
      TableFeaturePermissionsMetaData.ofOrWithNew(instance).withPermission(new TableFeaturePermission().withSuffix("analyze").withLabel("Analyze"));

      QSession contextSession = QContext.getQSession();
      contextSession.setPermissions(Set.of());

      QSession other = new QSession();
      other.setPermissions(Set.of("purchase.read", "purchase.analyze"));
      assertTrue(PermissionsHelper.hasTableFeaturePermission(instance, other, table, "analyze"));
      assertSame(contextSession, QContext.getQSession());
      assertFalse(PermissionsHelper.hasTableFeaturePermission(table, "analyze"));
      assertFalse(PermissionsHelper.hasTableFeaturePermission(instance, contextSession, table, "analyze"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void predicatesReplacementAndNullImplication()
   {
      QInstance instance = QContext.getQInstance();
      QTableMetaData table = protectedTable();
      TableFeaturePermissionsMetaData metadata = TableFeaturePermissionsMetaData.ofOrWithNew(instance)
         .withPermission(new TableFeaturePermission().withSuffix("analyze").withLabel("Old")
            .withAppliesTo((currentInstance, currentTable) -> false));
      QContext.getQSession().setPermissions(Set.of("purchase.read", "purchase.analyze"));
      assertFalse(PermissionsHelper.hasTableFeaturePermission(table, "analyze"));
      assertThat(PermissionsHelper.getAllAvailablePermissionNames(instance)).doesNotContain("purchase.analyze");
      metadata.withPermission(new TableFeaturePermission().withSuffix("analyze").withLabel("Analyze")
         .withAppliesTo((currentInstance, currentTable) -> currentTable == currentInstance.getTable("order"))
         .withImpliedBy((currentInstance, currentTable) -> null));
      assertThat(metadata.getPermissions()).hasSize(1);
      assertTrue(PermissionsHelper.hasTableFeaturePermission(table, "analyze"));
      assertThat(PermissionsHelper.getAllAvailablePermissions(instance).stream().filter(permission -> permission.getName().endsWith(".analyze")))
         .singleElement().satisfies(permission -> assertThat(permission.getPermissionType()).isEqualTo("Analyze"));
      QContext.getQSession().setPermissions(new HashSet<>(Set.of("purchase.read", "purchase.edit")));
      assertFalse(PermissionsHelper.hasTableFeaturePermission(table, "analyze"));
   }



   /*******************************************************************************
    ** Every standard subtype goes through the existing permission-level mapping.
    *******************************************************************************/
   @Test
   void implicationsUseStandardTableChecks()
   {
      QTableMetaData table = protectedTable();
      TableFeaturePermissionsMetaData metadata = TableFeaturePermissionsMetaData.ofOrWithNew(QContext.getQInstance());
      for(TablePermissionSubType subtype : TablePermissionSubType.values())
      {
         metadata.withPermission(new TableFeaturePermission().withSuffix("analyze").withLabel("Analyze")
            .withImpliedBy((instance, currentTable) -> subtype));
         QContext.getQSession().setPermissions(Set.of());
         assertFalse(PermissionsHelper.hasTableFeaturePermission(table, "analyze"));
         QContext.getQSession().setPermissions(new HashSet<>(Set.of("purchase.read")));
         assertThat(PermissionsHelper.hasTableFeaturePermission(table, "analyze")).isEqualTo(subtype == TablePermissionSubType.READ);
         QContext.getQSession().withPermission("purchase." + subtype.getPermissionSuffix());
         assertTrue(PermissionsHelper.hasTableFeaturePermission(table, "analyze"));
         QContext.getQSession().setPermissions(Set.of("purchase." + subtype.getPermissionSuffix()));
         assertThat(PermissionsHelper.hasTableFeaturePermission(table, "analyze")).isEqualTo(subtype == TablePermissionSubType.READ);
      }
      metadata.withPermission(new TableFeaturePermission().withSuffix("analyze").withLabel("Analyze")
         .withImpliedBy((instance, currentTable) -> TablePermissionSubType.EDIT));
      table.getPermissionRules().setLevel(PermissionLevel.READ_WRITE_PERMISSIONS);
      QContext.getQSession().setPermissions(new HashSet<>(Set.of("purchase.read", "purchase.edit")));
      assertFalse(PermissionsHelper.hasTableFeaturePermission(table, "analyze"));
      QContext.getQSession().withPermission("purchase.write");
      assertTrue(PermissionsHelper.hasTableFeaturePermission(table, "analyze"));
      table.getPermissionRules().setLevel(PermissionLevel.HAS_ACCESS_PERMISSION);
      QContext.getQSession().setPermissions(Set.of("purchase.hasAccess"));
      assertTrue(PermissionsHelper.hasTableFeaturePermission(table, "analyze"));
   }



   /*******************************************************************************
    ** Supplemental metadata is serializable even though QInstance has no clone API.
    *******************************************************************************/
   @Test
   void metadataAndCallbacksSurviveSerializationAndClone()
   {
      QInstance instance = QContext.getQInstance();
      QTableMetaData table = protectedTable();
      TableFeaturePermissionsMetaData original = TableFeaturePermissionsMetaData.ofOrWithNew(instance)
         .withPermission(new TableFeaturePermission().withSuffix("analyze").withLabel("Analyze")
            .withAppliesTo((currentInstance, currentTable) -> currentInstance.getTable(currentTable.getName()) == currentTable)
            .withImpliedBy((currentInstance, currentTable) -> TablePermissionSubType.EDIT));
      TableFeaturePermissionsMetaData restored = SerializationUtils.clone(original);
      TableFeaturePermissionsMetaData clone = original.clone();
      for(TableFeaturePermissionsMetaData copy : new TableFeaturePermissionsMetaData[] {restored, clone})
      {
         assertNotSame(original.getPermissions(), copy.getPermissions());
         assertNotSame(original.getPermissions().get(0), copy.getPermissions().get(0));
         instance.withSupplementalMetaData(copy);
         QContext.getQSession().setPermissions(new HashSet<>(Set.of("purchase.read", "purchase.edit")));
         assertTrue(PermissionsHelper.hasTableFeaturePermission(table, "analyze"));
         copy.getPermissions().get(0).setLabel("Changed");
         copy.withPermission(new TableFeaturePermission().withSuffix("other"));
      }
      assertThat(original.getPermissions()).hasSize(1);
      assertThat(original.getPermissions().get(0).getLabel()).isEqualTo("Analyze");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QTableMetaData protectedTable()
   {
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_ORDER);
      table.setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS).withPermissionBaseName("purchase"));
      return (table);
   }

}
