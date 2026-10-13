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


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeEvent;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeListenerHelper;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeListenerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeType;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.FeaturePermissionMode;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeature;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeatureOverride;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeaturesAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeaturesMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeaturesTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordOwnership;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordReference;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordReferenceSecurityConfig;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordReferenceTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RuntimeMetaDataContribution;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppSection;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Core contracts and enforcement through the memory backend.
 *******************************************************************************/
public class RecordCollaborationTest extends BaseTest
{
   private QInstance instance;
   private QTableMetaData orders;
   private static final String EXTENSION = "collaboration_comment";



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUpCollaboration() throws Exception
   {
      instance = QContext.getQInstance();
      orders = instance.getTable(TestUtils.TABLE_NAME_ORDER);
      OverrideProvider.entries = List.of();
      Cleanup.keys = new ArrayList<>();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void resolutionMergesCodeThenRuntimeAndKeepsHardLimits() throws Exception
   {
      RecordFeaturesMetaData.ofOrWithNew(instance).withEnabled(RecordFeature.COMMENTS, true).withSetting(RecordFeature.COMMENTS, "instance", "yes");
      instance.addApp(new QAppMetaData().withName("first").withSection(new QAppSection().withName("records").withTables(List.of(orders.getName())))
         .withSupplementalMetaData(new RecordFeaturesAppMetaData().withEnabled(RecordFeature.COMMENTS, false).withSetting(RecordFeature.COMMENTS, "mode", "app")));
      RecordFeaturesMetaData.ofOrWithNew(orders).withEnabled(RecordFeature.COMMENTS, true).withSetting(RecordFeature.COMMENTS, "mode", "table")
         .withFeatureLocked(RecordFeature.ATTACHMENTS, false);
      RecordFeaturesMetaData.of(instance).withOverrideProvider(new QCodeReference(OverrideProvider.class));
      OverrideProvider.entries = List.of(
         override(RecordFeatureOverride.ScopeType.TABLE, orders.getName(), RecordFeature.COMMENTS, true).withSettings(Map.of("mode", "runtime table")),
         override(RecordFeatureOverride.ScopeType.INSTANCE, null, RecordFeature.COMMENTS, false),
         override(RecordFeatureOverride.ScopeType.APP, "first", RecordFeature.COMMENTS, false).withSettings(Map.of("extra", "app runtime")),
         override(RecordFeatureOverride.ScopeType.TABLE, orders.getName(), RecordFeature.ATTACHMENTS, true));
      RuntimeMetaDataOverlay.refresh(instance);
      var resolved = RecordFeaturesResolver.resolve(instance, orders);
      assertTrue(resolved.isEnabled(RecordFeature.COMMENTS));
      assertFalse(resolved.isEnabled(RecordFeature.ATTACHMENTS));
      assertEquals(Map.of("instance", "yes", "mode", "runtime table", "extra", "app runtime"), resolved.getFeatures().get(RecordFeature.COMMENTS).getSettings());
      resolved.getFeatures().get(RecordFeature.COMMENTS).setEnabled(false);
      assertTrue(RecordFeaturesResolver.resolve(instance, orders).isEnabled(RecordFeature.COMMENTS));
      assertEquals(1, RuntimeMetaDataOverlay.getGeneration(instance));
      QTableMetaData noKey = orders.clone().withName("noKey").withPrimaryKeyField(null);
      assertTrue(RecordFeaturesResolver.resolve(instance, noKey).enabledFeatures().isEmpty());
      QTableMetaData internal = orders.clone().withName(EXTENSION);
      assertTrue(RecordFeaturesResolver.resolve(instance, internal).enabledFeatures().isEmpty());
      QTableMetaData noRead = orders.clone().withName("noRead").withoutCapability(Capability.TABLE_GET).withoutCapability(Capability.TABLE_QUERY);
      assertTrue(RecordFeaturesResolver.resolve(instance, noRead).enabledFeatures().isEmpty());
      QInstance other = TestUtils.defineInstance();
      assertTrue(RecordFeaturesResolver.resolve(other, other.getTable(orders.getName())).enabledFeatures().isEmpty());
      OverrideProvider.entries = List.of(override(RecordFeatureOverride.ScopeType.TABLE, orders.getName(), RecordFeature.COMMENTS, false));
      assertTrue(RecordFeaturesResolver.resolve(instance, orders).isEnabled(RecordFeature.COMMENTS));
      RuntimeMetaDataOverlay.refresh(instance);
      assertFalse(RecordFeaturesResolver.resolve(instance, orders).isEnabled(RecordFeature.COMMENTS));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void catalogAndBothPermissionModes() throws Exception
   {
      enableComments();
      orders.setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS).withPermissionBaseName("purchase"));
      QContext.getQSession().withPermissions("purchase.read", "purchase.edit");
      assertTrue(RecordFeaturePermissions.hasTableCapability(orders, RecordFeature.COMMENTS));
      assertThat(PermissionsHelper.getAllAvailablePermissions(instance)).anySatisfy(permission ->
      {
         assertEquals("purchase.comment", permission.getName());
         assertEquals("Table", permission.getObjectType());
         assertEquals("Comment", permission.getPermissionType());
      });
      RecordFeaturesMetaData.of(orders).withPermissionMode(RecordFeature.COMMENTS, FeaturePermissionMode.GRANTED_ONLY);
      RuntimeMetaDataOverlay.refresh(instance);
      assertFalse(RecordFeaturePermissions.hasTableCapability(orders, RecordFeature.COMMENTS));
      assertThrows(QPermissionDeniedException.class, () -> RecordFeaturePermissions.requireTableCapability(orders, RecordFeature.COMMENTS));
      QContext.getQSession().withPermission("purchase.comment");
      assertTrue(RecordFeaturePermissions.hasTableCapability(orders, RecordFeature.COMMENTS));
      QContext.getQSession().setPermissions(Set.of("purchase.comment"));
      assertFalse(RecordFeaturePermissions.hasTableCapability(orders, RecordFeature.COMMENTS));
      orders.setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.NOT_PROTECTED));
      assertTrue(RecordFeaturePermissions.hasTableCapability(orders, RecordFeature.COMMENTS));
      orders.setIsHidden(true);
      assertThat(PermissionsHelper.getAllAvailablePermissionNames(instance)).doesNotContain("purchase.comment");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void editFallbackRequiresWriteLocksButExplicitGrantDoesNot() throws Exception
   {
      enableComments();
      QContext.setQSession(new QSystemUserSession());
      QRecord parent = insert(orders.getName(), new QRecord().withValue("id", 1).withValue("storeId", 1));
      orders.getRecordSecurityLocks().get(0).setLockScope(RecordSecurityLock.LockScope.WRITE);
      orders.setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      QContext.setQSession(newSession().withPermissions("order.read", "order.edit"));
      assertTrue(RecordFeaturePermissions.hasTableCapability(orders, RecordFeature.COMMENTS));
      assertFalse(RecordFeaturePermissions.checkRecordCapability(orders, RecordFeature.COMMENTS, parent));
      QContext.getQSession().withPermission("order.comment");
      assertTrue(RecordFeaturePermissions.checkRecordCapability(orders, RecordFeature.COMMENTS, parent));
      orders.getRecordSecurityLocks().get(0).setLockScope(RecordSecurityLock.LockScope.READ_AND_WRITE);
      assertFalse(RecordFeaturePermissions.checkRecordCapability(orders, RecordFeature.COMMENTS, parent));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void referenceQueriesAndWritesInheritParentVisibility() throws Exception
   {
      prepareReferences(false);
      QContext.setQSession(new QSystemUserSession());
      insert(orders.getName(), new QRecord().withValue("id", 1).withValue("storeId", 1));
      insert(orders.getName(), new QRecord().withValue("id", 2).withValue("storeId", 2));
      insert(EXTENSION, row(1, "001"));
      insert(EXTENSION, row(2, "001"));
      QContext.setQSession(newSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE, 1));
      QQueryFilter pinned = new QQueryFilter(new QFilterCriteria("tableName", QCriteriaOperator.EQUALS, "order"),
         new QFilterCriteria("recordId", QCriteriaOperator.IN, List.of("1", "2")));
      assertEquals(1, new QueryAction().execute(new QueryInput(EXTENSION).withFilter(pinned)).getRecords().size());
      assertEquals(2, pinned.getCriteria().get(1).getValues().size(), "Caller filter is not mutated");
      assertEquals(1, new CountAction().execute(new CountInput(EXTENSION).withFilter(pinned)).getCount());
      Aggregate count = new Aggregate("id", AggregateOperator.COUNT);
      assertEquals(1, ((Number) new AggregateAction().execute(new AggregateInput(EXTENSION).withFilter(pinned).withAggregate(count))
         .getResults().get(0).getAggregateValue(count)).intValue());
      QRecord invisible = insert(EXTENSION, row(2, "001"));
      QRecord missing = insert(EXTENSION, row(999, "001"));
      assertFalse(invisible.getErrors().isEmpty());
      assertEquals(missing.getErrors().get(0).toString(), invisible.getErrors().get(0).toString());
      assertThat(invisible.getErrors().get(0).toString()).contains("No record was found");
      assertNull(GetAction.execute(EXTENSION, 2));
      orders.setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      assertEquals(0, new QueryAction().execute(new QueryInput(EXTENSION)).getRecords().size());
      assertEquals(0, new CountAction().execute(new CountInput(EXTENSION)).getCount());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void ownRowRuleUsesStoredAuthorAndChecksDeletes() throws Exception
   {
      prepareReferences(false);
      QContext.setQSession(new QSystemUserSession());
      insert(orders.getName(), new QRecord().withValue("id", 1).withValue("storeId", 1));
      QRecord extension = insert(EXTENSION, row(1, "other"));
      Serializable key = extension.getValue("id");
      QContext.setQSession(newSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE, 1));
      var denied = new UpdateAction().execute(new UpdateInput().withTableName(EXTENSION).withRecords(List.of(new QRecord().withValue("id", key).withValue("author", "001"))));
      assertFalse(denied.getRecords().get(0).getErrors().isEmpty());
      var deleted = new DeleteAction().execute(new DeleteInput().withTableName(EXTENSION).withPrimaryKeys(List.of(key)));
      assertEquals(0, deleted.getDeletedRecordCount());
      QContext.getQSession().withPermission(RecordReferenceSecurityHelper.ADMIN_PERMISSION);
      var allowed = new UpdateAction().execute(new UpdateInput().withTableName(EXTENSION).withRecords(List.of(new QRecord().withValue("id", key).withValue("body", "moderated"))));
      assertTrue(allowed.getRecords().get(0).getErrors().isEmpty());
      assertEquals(1, new DeleteAction().execute(new DeleteInput().withTableName(EXTENSION).withPrimaryKeys(List.of(key))).getDeletedRecordCount());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void mirrorsCopyAndFollowParentChangesAndCleanupWaitsForCommit() throws Exception
   {
      prepareReferences(true);
      RecordReferenceSecurityConfig config = RecordReferenceSecurityConfig.ofOrWithNew(instance)
         .withMirroredSecurityKeyTypes(List.of(TestUtils.SECURITY_KEY_TYPE_STORE)).withCleanupProviders(List.of(new QCodeReference(Cleanup.class)));
      config.enrich(instance);
      config.enrich(instance);
      assertEquals(1, instance.getRecordChangeListeners().size());
      QContext.setQSession(new QSystemUserSession());
      insert(orders.getName(), new QRecord().withValue("id", 1).withValue("storeId", 1));
      String mirror = RecordReferenceSecurityConfig.lockFieldName(TestUtils.SECURITY_KEY_TYPE_STORE);
      QRecord extension = insert(EXTENSION, row(1, "someone").withValue(mirror, 999));
      assertEquals(1, extension.getValueInteger(mirror));
      new UpdateAction().execute(new UpdateInput().withTableName(orders.getName()).withRecords(List.of(new QRecord().withValue("id", 1).withValue("storeId", 2))));
      assertEquals(2, GetAction.execute(EXTENSION, extension.getValue("id")).getValueInteger(mirror));
      RecordFeaturesMetaData.of(orders).withEnabled(RecordFeature.COMMENTS, false);
      RuntimeMetaDataOverlay.refresh(instance);
      new UpdateAction().execute(new UpdateInput().withTableName(orders.getName()).withRecords(List.of(new QRecord().withValue("id", 1).withValue("storeId", 3))));
      RecordFeaturesMetaData.of(orders).withEnabled(RecordFeature.COMMENTS, true);
      RuntimeMetaDataOverlay.refresh(instance);
      assertEquals(3, GetAction.execute(EXTENSION, extension.getValue("id")).getValueInteger(mirror));
      QBackendTransaction transaction = new QBackendTransaction();
      new DeleteAction().execute(new DeleteInput().withTableName(orders.getName()).withPrimaryKeys(List.of(1)).withTransaction(transaction));
      assertTrue(Cleanup.keys.isEmpty());
      transaction.commit();
      assertEquals(List.of(1), Cleanup.keys);
      assertEquals("lockKey_tenant_id", RecordReferenceSecurityConfig.lockFieldName("tenant-id"));
   }



   /*******************************************************************************
    ** Mirrored values must replace input values before the extension's lock check.
    *******************************************************************************/
   @Test
   void insertChecksMirroredLocksAfterCopyingParentValues() throws Exception
   {
      prepareReferences(true);
      RecordReferenceSecurityConfig.ofOrWithNew(instance).withMirroredSecurityKeyTypes(List.of(TestUtils.SECURITY_KEY_TYPE_STORE));
      String mirror = RecordReferenceSecurityConfig.lockFieldName(TestUtils.SECURITY_KEY_TYPE_STORE);
      instance.getTable(EXTENSION).withRecordSecurityLock(new RecordSecurityLock()
         .withFieldName(mirror).withSecurityKeyType(TestUtils.SECURITY_KEY_TYPE_STORE));
      QContext.setQSession(new QSystemUserSession());
      insert(orders.getName(), new QRecord().withValue("id", 1).withValue("storeId", 1));
      QContext.setQSession(newSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE, 1));
      QRecord extension = insert(EXTENSION, row(1, "001").withValue(mirror, 999));
      assertTrue(extension.getErrors().isEmpty());
      assertEquals(1, extension.getValueInteger(mirror));
      assertNotNull(GetAction.execute(EXTENSION, extension.getValue("id")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void frontendSummaryAndValueContracts() throws Exception
   {
      assertNull(RecordFeatureSummaryHelper.frontend(instance, orders));
      enableComments();
      RecordFeaturesMetaData.of(orders).withOwnership(new RecordOwnership().withOwnerFieldName("shipToName"));
      RuntimeMetaDataOverlay.refresh(instance);
      QContext.setQSession(new QSystemUserSession());
      QRecord record = insert(orders.getName(), new QRecord().withValue("id", 1).withValue("storeId", 1).withValue("shipToName", "owner"));
      assertEquals("1", new RecordReference("order", 1).primaryKeyAsString());
      assertEquals(List.of(RecordFeature.COMMENTS), RecordFeatureSummaryHelper.frontend(instance, orders).getFeatures());
      assertEquals("owner", RecordFeatureSummaryHelper.summarize(orders, record).getOwner());
      assertTrue(RecordFeatureSummaryHelper.summarize(orders, record).getCapabilities().get("comment"));
      assertNull(RecordFeatureSummaryHelper.summarize(orders, record).getIsFavorite());
      RecordAccessCheckerInterface checker = (user, reference) -> java.util.Optional.empty();
      UserSessionProviderInterface sessions = user -> java.util.Optional.of(QContext.getQSession());
      assertTrue(checker.canRead("other", new RecordReference("order", 1)).isEmpty());
      assertTrue(sessions.sessionForUser("001").isPresent());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void enableComments()
   {
      RecordFeaturesMetaData.ofOrWithNew(orders).withEnabled(RecordFeature.COMMENTS, true);
      new QInstanceEnricher(instance).enrich();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void prepareReferences(boolean mirrored) throws Exception
   {
      enableComments();
      QTableMetaData extension = new QTableMetaData().withName(EXTENSION).withBackendName(TestUtils.MEMORY_BACKEND_NAME).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER)).withField(new QFieldMetaData("tableName", QFieldType.STRING))
         .withField(new QFieldMetaData("recordId", QFieldType.STRING)).withField(new QFieldMetaData("body", QFieldType.STRING))
         .withField(new QFieldMetaData("author", QFieldType.STRING))
         .withField(new QFieldMetaData(RecordReferenceSecurityConfig.lockFieldName(TestUtils.SECURITY_KEY_TYPE_STORE), QFieldType.INTEGER))
         .withSupplementalMetaData(new RecordReferenceTableMetaData().withTableNameField("tableName").withRecordIdField("recordId")
            .withFeature(RecordFeature.COMMENTS).withMirroredLocks(mirrored).withAuthorField("author"));
      instance.addTable(extension);
      new QInstanceEnricher(instance).enrich();
      RuntimeMetaDataOverlay.refresh(instance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord row(int primaryKey, String author)
   {
      return (new QRecord().withValue("tableName", "order").withValue("recordId", Integer.toString(primaryKey)).withValue("author", author).withValue("body", "Hello"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private RecordFeatureOverride override(RecordFeatureOverride.ScopeType scope, String name, RecordFeature feature, boolean enabled)
   {
      return (new RecordFeatureOverride().withScopeType(scope).withScopeName(name).withFeature(feature).withEnabled(enabled));
   }



   /*******************************************************************************
    ** Test runtime override provider.
    *******************************************************************************/
   public static class OverrideProvider implements RecordFeatureOverrideProviderInterface
   {
      private static List<RecordFeatureOverride> entries;



      /*******************************************************************************
       **
       *******************************************************************************/
      public List<RecordFeatureOverride> getOverrides(QInstance instance)
      {
         return (entries);
      }
   }



   /*******************************************************************************
    ** Test cleanup provider.
    *******************************************************************************/
   public static class Cleanup implements RecordReferenceCleanupInterface
   {
      private static List<Serializable> keys;



      /*******************************************************************************
       **
       *******************************************************************************/
      public void onParentsDeleted(String tableName, List<Serializable> primaryKeys)
      {
         keys.addAll(primaryKeys);
      }
   }

   /*******************************************************************************
    ** Insert one row through normal core enforcement.
    *******************************************************************************/
   private QRecord insert(String tableName, QRecord record) throws QException
   {
      return (new InsertAction().execute(new InsertInput().withTableName(tableName).withRecords(List.of(record))).getRecords().get(0));
   }




   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void runtimeContributionGenerationAndInvalidRefresh() throws Exception
   {
      RecordFeaturesMetaData.ofOrWithNew(instance).withRuntimeProviders(List.of(new QCodeReference(RuntimeProvider.class)));
      RuntimeProvider.contribution = new RuntimeMetaDataContribution().withGeneration(37).withFeatureOverrides(List.of(
         override(RecordFeatureOverride.ScopeType.TABLE, orders.getName(), RecordFeature.FAVORITES, true)));
      RuntimeMetaDataOverlay.refresh(instance);
      assertEquals(37, RuntimeMetaDataOverlay.getGeneration(instance));
      assertTrue(RecordFeaturesResolver.resolve(instance, orders).isEnabled(RecordFeature.FAVORITES));
      RuntimeProvider.contribution = new RuntimeMetaDataContribution().withGeneration(38).withFeatureOverrides(List.of(new RecordFeatureOverride()));
      RuntimeMetaDataOverlay.refresh(instance);
      assertTrue(RecordFeaturesResolver.resolve(instance, orders).isEnabled(RecordFeature.FAVORITES));
      assertEquals(38, RuntimeMetaDataOverlay.getGeneration(instance));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void validationAndCloningProtectMetadataContracts() throws Exception
   {
      QInstanceValidator validator = new QInstanceValidator();
      RecordFeaturesMetaData.ofOrWithNew(instance).withEnabled(RecordFeature.COMMENTS, true);
      QTableMetaData noKey = orders.clone().withName("noKey").withPrimaryKeyField(null);
      RecordFeaturesTableMetaData explicit = RecordFeaturesTableMetaData.ofOrWithNew(noKey).withEnabled(RecordFeature.COMMENTS, true);
      explicit.validate(instance, noKey, validator);
      assertThat(validator.getErrors()).anyMatch(error -> error.contains("single primary key"));
      validator = new QInstanceValidator();
      explicit.withEnabled(RecordFeature.COMMENTS, false).validate(instance, noKey, validator);
      assertTrue(validator.getErrors().isEmpty(), "Inherited enablement on a keyless table is suppressed, not a validation failure");
      RecordFeaturesMetaData.ofOrWithNew(orders).withEnabled(RecordFeature.COMMENTS, true).withSetting(RecordFeature.COMMENTS, "mode", "original");
      QTableMetaData clone = orders.clone();
      RecordFeaturesMetaData.of(clone).withSetting(RecordFeature.COMMENTS, "mode", "copy");
      assertEquals("original", RecordFeaturesMetaData.of(orders).getSettings().get(RecordFeature.COMMENTS).get("mode"));
      RecordReferenceTableMetaData reference = new RecordReferenceTableMetaData().withTableNameField("missing").withRecordIdField("alsoMissing")
         .withAuthorField("missingAuthor").withFeature(RecordFeature.COMMENTS);
      reference.validate(instance, orders, validator);
      assertEquals(3, validator.getErrors().size());
      RecordFeaturesMetaData.of(instance).withOverrideProvider(new QCodeReference(String.class));
      RecordFeaturesMetaData.of(instance).validate(instance, validator);
      assertTrue(validator.getErrors().size() > 3);
      RecordReferenceSecurityConfig.ofOrWithNew(instance).withMirroredSecurityKeyTypes(List.of("a-b", "a_b")).validate(instance, validator);
      assertThat(validator.getErrors()).anyMatch(error -> error.contains("distinct valid field names"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void permissionlessReferenceNeedsOnlyVisibilityAndMirrorMovesWithReference() throws Exception
   {
      prepareReferences(true);
      RecordReferenceSecurityConfig.ofOrWithNew(instance).withMirroredSecurityKeyTypes(List.of(TestUtils.SECURITY_KEY_TYPE_STORE));
      RecordFeaturesMetaData.of(orders).withEnabled(RecordFeature.FAVORITES, true);
      RecordReferenceTableMetaData.of(instance.getTable(EXTENSION)).withFeature(RecordFeature.FAVORITES);
      RuntimeMetaDataOverlay.refresh(instance);
      QContext.setQSession(new QSystemUserSession());
      insert(orders.getName(), new QRecord().withValue("id", 1).withValue("storeId", 1));
      insert(orders.getName(), new QRecord().withValue("id", 2).withValue("storeId", 2));
      orders.setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      QContext.setQSession(newSession().withPermission("order.read").withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE, 1).withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE, 2));
      QRecord extension = insert(EXTENSION, row(1, "001"));
      assertTrue(extension.getErrors().isEmpty());
      String mirror = RecordReferenceSecurityConfig.lockFieldName(TestUtils.SECURITY_KEY_TYPE_STORE);
      new UpdateAction().execute(new UpdateInput().withTableName(EXTENSION).withRecords(List.of(
         new QRecord().withValue("id", extension.getValue("id")).withValue("recordId", "2"))));
      assertEquals(2, GetAction.execute(EXTENSION, extension.getValue("id")).getValueInteger(mirror));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void mirroredReadLocksAndRollbackCleanup() throws Exception
   {
      prepareReferences(true);
      String mirror = RecordReferenceSecurityConfig.lockFieldName(TestUtils.SECURITY_KEY_TYPE_STORE);
      instance.getTable(EXTENSION).withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType(TestUtils.SECURITY_KEY_TYPE_STORE)
         .withFieldName(mirror).withNullValueBehavior(RecordSecurityLock.NullValueBehavior.ALLOW));
      RecordReferenceSecurityConfig.ofOrWithNew(instance).withMirroredSecurityKeyTypes(List.of(TestUtils.SECURITY_KEY_TYPE_STORE))
         .withCleanupProviders(List.of(new QCodeReference(Cleanup.class))).enrich(instance);
      QContext.setQSession(new QSystemUserSession());
      insert(orders.getName(), new QRecord().withValue("id", 1).withValue("storeId", 1));
      insert(orders.getName(), new QRecord().withValue("id", 2).withValue("storeId", 2));
      insert(EXTENSION, row(1, "001"));
      insert(EXTENSION, row(2, "001"));
      QContext.setQSession(newSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE, 1));
      assertEquals(1, new QueryAction().execute(new QueryInput(EXTENSION)).getRecords().size());
      orders.getRecordSecurityLocks().get(0).setLockScope(RecordSecurityLock.LockScope.READ);
      new UpdateAction().execute(new UpdateInput().withTableName(orders.getName()).withRecords(List.of(new QRecord().withValue("id", 1).withValue("storeId", 2))));
      assertNull(GetAction.execute(EXTENSION, 1));
      QContext.setQSession(new QSystemUserSession());
      assertEquals(2, GetAction.execute(EXTENSION, 1).getValueInteger(mirror));
      QBackendTransaction transaction = new QBackendTransaction();
      new DeleteAction().execute(new DeleteInput().withTableName(orders.getName()).withPrimaryKeys(List.of(1)).withTransaction(transaction));
      transaction.rollback();
      assertTrue(Cleanup.keys.isEmpty());
   }



   /*******************************************************************************
    ** Runtime provider with a source generation.
    *******************************************************************************/
   public static class RuntimeProvider implements RuntimeMetaDataProviderInterface
   {
      private static RuntimeMetaDataContribution contribution;



      /*******************************************************************************
       **
       *******************************************************************************/
      public RuntimeMetaDataContribution getContribution(QInstance instance)
      {
         return (contribution);
      }
   }



   /*******************************************************************************
    ** Required integrity failures must escape the action's event preparation guard.
    *******************************************************************************/
   @Test
   void requiredListenerFailureIsNotSwallowed() throws Exception
   {
      QContext.setQSession(new QSystemUserSession());
      insert(orders.getName(), new QRecord().withValue("id", 1).withValue("storeId", 1));
      instance.withRecordChangeListener(new QCodeReference(RequiredFailure.class));
      assertThrows(RecordChangeListenerHelper.RequiredListenerException.class, () -> new UpdateAction().execute(
         new UpdateInput().withTableName(orders.getName()).withRecords(List.of(new QRecord().withValue("id", 1).withValue("shipToName", "new")))));
   }



   /*******************************************************************************
    ** Simulate a failed required integrity operation.
    *******************************************************************************/
   public static class RequiredFailure implements RecordChangeListenerInterface
   {
      /*******************************************************************************
       ** Require the operation to succeed.
       *******************************************************************************/
      public boolean isRequired()
      {
         return (true);
      }



      /*******************************************************************************
       ** Listen to parent updates.
       *******************************************************************************/
      public boolean appliesTo(String tableName, RecordChangeType type)
      {
         return (type == RecordChangeType.UPDATE);
      }



      /*******************************************************************************
       ** Fail before the caller commits.
       *******************************************************************************/
      public void onRecordsChanged(RecordChangeEvent event) throws QException
      {
         throw (new QException("Integrity operation failed"));
      }
   }
}
