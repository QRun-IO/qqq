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

package com.kingsrook.qqq.backend.core.actions.collaboration;


import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.FeaturePermissionMode;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeature;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeaturesMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppSection;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.TableFeaturePermission;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.TableFeaturePermissionsMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Collaboration contributes permissions through the generic registry at every scope.
 *******************************************************************************/
class RecordFeaturePermissionsRegistrarTest extends BaseTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void enrichmentRegistersAllScopesIdempotently() throws Exception
   {
      for(String scope : new String[] {"instance", "app", "table"})
      {
         QInstance instance = TestUtils.defineInstance();
         QTableMetaData table = instance.getTable(TestUtils.TABLE_NAME_ORDER);
         assertNull(TableFeaturePermissionsMetaData.of(instance));
         RecordFeaturesMetaData features;
         if(scope.equals("instance"))
         {
            features = RecordFeaturesMetaData.ofOrWithNew(instance);
         }
         else if(scope.equals("app"))
         {
            QAppMetaData app = new QAppMetaData().withName("featureApp").withSection(new QAppSection().withName("records").withTable(table.getName()));
            instance.addApp(app);
            features = RecordFeaturesMetaData.ofOrWithNew(app);
         }
         else
         {
            features = RecordFeaturesMetaData.ofOrWithNew(table);
         }
         features.withEnabled(RecordFeature.COMMENTS, true);
         new QInstanceEnricher(instance).enrich();
         TableFeaturePermissionsMetaData.of(instance).withPermission(new TableFeaturePermission().withSuffix("analyze").withLabel("Analyze"));
         new QInstanceEnricher(instance).enrich();
         assertThat(TableFeaturePermissionsMetaData.of(instance).getPermissions()).hasSize(7);
         assertThat(PermissionsHelper.getAllAvailablePermissionNames(instance)).contains("order.comment", "order.analyze");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void catalogAndSessionsCoverEveryFeatureAndBothModes() throws Exception
   {
      QInstance instance = QContext.getQInstance();
      QTableMetaData table = instance.getTable(TestUtils.TABLE_NAME_ORDER);
      table.setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS).withPermissionBaseName("purchase"));
      RecordFeaturesMetaData features = RecordFeaturesMetaData.ofOrWithNew(table);
      Map<RecordFeature, String> labels = Map.of(RecordFeature.COMMENTS, "Comment", RecordFeature.ATTACHMENTS, "Attach files",
         RecordFeature.TAGS, "Tag", RecordFeature.SHARING, "Share", RecordFeature.TASKS, "Tasks", RecordFeature.ASSIGNMENT, "Assign");
      labels.keySet().forEach(feature -> features.withEnabled(feature, true));
      new QInstanceEnricher(instance).enrich();
      for(FeaturePermissionMode mode : FeaturePermissionMode.values())
      {
         labels.keySet().forEach(feature -> features.withPermissionMode(feature, mode));
         RuntimeMetaDataOverlay.refresh(instance);
         for(var entry : labels.entrySet())
         {
            RecordFeature feature = entry.getKey();
            String suffix = feature.getPermissionSuffix();
            String grant = "purchase." + suffix;
            assertThat(PermissionsHelper.getAllAvailablePermissions(instance)).anySatisfy(permission ->
            {
               assertThat(permission.getName()).isEqualTo(grant);
               assertThat(permission.getPermissionType()).isEqualTo(entry.getValue());
               assertThat(permission.getObjectName()).isEqualTo(table.getLabel());
               assertThat(permission.getObjectType()).isEqualTo("Table");
            });
            for(Set<String> permissions : Set.of(Set.<String>of(), Set.of("purchase.read"), Set.of(grant), Set.of("purchase.edit")))
            {
               QContext.getQSession().setPermissions(permissions);
               assertFalse(PermissionsHelper.hasTableFeaturePermission(table, suffix));
               assertFalse(RecordFeaturePermissions.hasTableCapability(table, feature));
            }
            QContext.getQSession().setPermissions(Set.of("purchase.read", "purchase.edit"));
            assertThat(PermissionsHelper.hasTableFeaturePermission(table, suffix)).isEqualTo(mode == FeaturePermissionMode.EDIT_OR_GRANTED);
            assertThat(RecordFeaturePermissions.hasTableCapability(table, feature)).isEqualTo(mode == FeaturePermissionMode.EDIT_OR_GRANTED);
            QContext.getQSession().setPermissions(Set.of("purchase.read", grant));
            assertTrue(PermissionsHelper.hasTableFeaturePermission(table, suffix));
            assertTrue(RecordFeaturePermissions.hasTableCapability(table, feature));
         }
      }
      features.withEnabled(RecordFeature.COMMENTS, false);
      RuntimeMetaDataOverlay.refresh(instance);
      QContext.getQSession().setPermissions(Set.of("purchase.read", "purchase.comment"));
      assertFalse(PermissionsHelper.hasTableFeaturePermission(table, "comment"));
      assertThat(PermissionsHelper.getAllAvailablePermissionNames(instance)).doesNotContain("purchase.comment");
   }



   /*******************************************************************************
    ** Serialized registrations resolve against their destination instance, not the source.
    *******************************************************************************/
   @Test
   void serializedCallbacksResolveDestinationMetadata() throws Exception
   {
      QInstance original = QContext.getQInstance();
      RecordFeaturesMetaData.ofOrWithNew(original).withEnabled(RecordFeature.COMMENTS, true);
      new QInstanceEnricher(original).enrich();
      TableFeaturePermissionsMetaData copy = SerializationUtils.clone(TableFeaturePermissionsMetaData.of(original));
      QInstance destination = TestUtils.defineInstance();
      destination.withSupplementalMetaData(copy);
      QTableMetaData table = destination.getTable(TestUtils.TABLE_NAME_ORDER);
      table.setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      QContext.init(destination, newSession().withPermissions("order.read", "order.edit"));
      assertFalse(PermissionsHelper.hasTableFeaturePermission(table, "comment"));
      RecordFeaturesMetaData.ofOrWithNew(table).withEnabled(RecordFeature.COMMENTS, true).withPermissionMode(RecordFeature.COMMENTS, FeaturePermissionMode.GRANTED_ONLY);
      RuntimeMetaDataOverlay.refresh(destination);
      assertFalse(PermissionsHelper.hasTableFeaturePermission(table, "comment"));
      QContext.getQSession().withPermission("order.comment");
      assertTrue(PermissionsHelper.hasTableFeaturePermission(table, "comment"));
      RecordFeaturesMetaData.of(table).withPermissionMode(RecordFeature.COMMENTS, FeaturePermissionMode.EDIT_OR_GRANTED);
      RuntimeMetaDataOverlay.refresh(destination);
      QContext.getQSession().setPermissions(Set.of("order.read", "order.edit"));
      assertTrue(PermissionsHelper.hasTableFeaturePermission(table, "comment"));
   }

}
