/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.interfaces.QueryInterface;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerInterface;
import com.kingsrook.qqq.backend.core.actions.permissions.CustomPermissionChecker;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.AssociatedRecordDiscovery;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.CaseChangeBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.MetaDataWithPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryQueryAction;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Recursive authorization follows operation-specific metadata and actual keys.
 *******************************************************************************/
class AssociatedWritePermissionsTest
{
   private QInstance instance;
   private static List<QueryInput> storedValueReads = new ArrayList<>();
   private static List<QueryInput> membershipReads = new ArrayList<>();
   private static boolean refuseStoredValues;



   /*******************************************************************************
    ** A second header and its descendants catch overbroad cascade discovery.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      MemoryRecordStore.fullReset();
      storedValueReads = new ArrayList<>();
      membershipReads = new ArrayList<>();
      refuseStoredValues = false;
      instance = TestUtils.defineInstance();
      for(String name : List.of("header", "line", "detail"))
      {
         instance.addTable(new QTableMetaData().withName(name).withBackendName(TestUtils.defineMemoryBackend().getName())
            .withPrimaryKeyField("id").withField(new QFieldMetaData("id", QFieldType.INTEGER))
            .withField(new QFieldMetaData("parentId", QFieldType.INTEGER)).withField(new QFieldMetaData("name", QFieldType.STRING))
            .withPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS)));
      }
      addAssociation("header", "line");
      addAssociation("line", "detail");
      QContext.init(instance, new QSession());
      MemoryRecordStore.getInstance().insert(new InsertInput("header").withRecords(List.of(record(1, null), record(2, null))), false);
      MemoryRecordStore.getInstance().insert(new InsertInput("line").withRecords(List.of(record(1, 1), record(2, 1), record(3, 2))), false);
      MemoryRecordStore.getInstance().insert(new InsertInput("detail").withRecords(List.of(record(1, 1), record(2, 3))), false);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      QContext.clear();
      MemoryRecordStore.fullReset();
      storedValueReads.clear();
      membershipReads.clear();
      refuseStoredValues = false;
   }



   /*******************************************************************************
    ** Supplied ids under INSERT are still inserts; nested metadata is followed.
    *******************************************************************************/
   @Test
   void testNestedInsertAndNoMutation() throws Exception
   {
      InsertInput input = new InsertInput("header").withInputSource(QInputSource.USER).withRecord(record(9, null)
         .withAssociatedRecords("line", List.of(record(8, 99).withAssociatedRecords("detail", List.of(record(7, 98))))));
      String before = JsonUtils.toJson(input.getRecords());
      QContext.getQSession().withPermissions("line.insert", "detail.edit");
      assertThrows(QPermissionDeniedException.class, () -> AssociatedWritePermissions.check(input));
      assertEquals(before, JsonUtils.toJson(input.getRecords()));
      QContext.getQSession().withPermissions("detail.insert");
      assertDoesNotThrow(() -> AssociatedWritePermissions.check(input));
      assertEquals(before, JsonUtils.toJson(input.getRecords()));
   }



   /*******************************************************************************
    ** A reverse join must not read the child's foreign-key field from the parent.
    *******************************************************************************/
   @Test
   void testReverseInsertPermissionsAndNoMutation() throws Exception
   {
      QJoinMetaData join = instance.getJoin("headerJoinline").flip();
      instance.getJoins().put(join.getName(), join);
      instance.getTable("header").getFields().remove("parentId");
      InsertInput input = new InsertInput("header").withInputSource(QInputSource.USER)
         .withRecord(new QRecord().withValue("id", 9).withValue("name", "New header")
            .withAssociatedRecords("line", List.of(record(8, 99))));
      String before = JsonUtils.toJson(input.getRecords());
      String beforeJoin = JsonUtils.toJson(join);

      QContext.setQSession(new QSession().withPermissions("line.edit"));
      assertThrows(QPermissionDeniedException.class, () -> AssociatedWritePermissions.check(input));
      assertEquals(before, JsonUtils.toJson(input.getRecords()));
      assertEquals(beforeJoin, JsonUtils.toJson(instance.getJoin(join.getName())));

      QContext.setQSession(new QSession().withPermissions("line.insert"));
      assertDoesNotThrow(() -> AssociatedWritePermissions.check(input));
      assertEquals(before, JsonUtils.toJson(input.getRecords()));
      assertEquals(beforeJoin, JsonUtils.toJson(instance.getJoin(join.getName())));
   }



   /*******************************************************************************
    ** Reverse replacement sets distinguish edits, inserts and actual omitted ids.
    *******************************************************************************/
   @Test
   void testReverseUpdatePermissionsAndNoMutation() throws Exception
   {
      QJoinMetaData join = instance.getJoin("headerJoinline").flip();
      instance.getJoins().put(join.getName(), join);
      instance.getTable("header").getFields().remove("parentId");
      String beforeJoin = JsonUtils.toJson(join);
      UpdateInput retained = new UpdateInput("header").withInputSource(QInputSource.USER)
         .withRecord(new QRecord().withValue("id", 1).withValue("name", "Changed")
            .withAssociatedRecords("line", List.of(record(1, 99), record(2, 99))));
      UpdateInput inserted = new UpdateInput("header").withInputSource(QInputSource.USER)
         .withRecord(new QRecord().withValue("id", 1).withValue("name", "Changed")
            .withAssociatedRecords("line", List.of(record(1, 99), record(2, 99), record(null, 99))));
      UpdateInput omitted = new UpdateInput("header").withInputSource(QInputSource.USER)
         .withRecord(new QRecord().withValue("id", 1).withValue("name", "Changed")
            .withAssociatedRecords("line", List.of(record(1, 99))));
      List<UpdateInput> inputs = List.of(retained, inserted, omitted);
      List<String> before = inputs.stream().map(input -> JsonUtils.toJson(input.getRecords())).toList();

      QContext.setQSession(new QSession().withPermissions("line.insert"));
      assertThrows(QPermissionDeniedException.class, () -> AssociatedWritePermissions.check(retained));
      QContext.setQSession(new QSession().withPermissions("line.edit"));
      assertDoesNotThrow(() -> AssociatedWritePermissions.check(retained));
      assertThrows(QPermissionDeniedException.class, () -> AssociatedWritePermissions.check(inserted));
      assertThrows(QPermissionDeniedException.class, () -> AssociatedWritePermissions.check(omitted));
      assertEquals(before, inputs.stream().map(input -> JsonUtils.toJson(input.getRecords())).toList());
      assertEquals(beforeJoin, JsonUtils.toJson(instance.getJoin(join.getName())));

      QContext.setQSession(new QSession().withPermissions("line.edit", "line.insert"));
      assertDoesNotThrow(() -> AssociatedWritePermissions.check(inserted));
      QContext.setQSession(new QSession().withPermissions("line.edit", "line.delete"));
      assertDoesNotThrow(() -> AssociatedWritePermissions.check(omitted));
      assertEquals(before, inputs.stream().map(input -> JsonUtils.toJson(input.getRecords())).toList());
      assertEquals(beforeJoin, JsonUtils.toJson(instance.getJoin(join.getName())));
   }



   /*******************************************************************************
    ** Sparse composite replacements require only actual child write grants.
    ** Hidden/heavy stored components must neither require READ nor enter input.
    *******************************************************************************/
   @Test
   void testSparseCompositePermissionsAndNoMutationForward() throws Exception
   {
      assertSparseCompositePermissionsAndNoMutation(false);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSparseCompositePermissionsAndNoMutationReverse() throws Exception
   {
      assertSparseCompositePermissionsAndNoMutation(true);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertSparseCompositePermissionsAndNoMutation(boolean reverse) throws Exception
   {
      configureStoredComposite(reverse);
      UpdateInput retained = sparseReplacement(List.of(new QRecord().withValue("id", 1), new QRecord().withValue("id", 2)));
      UpdateInput inserted = sparseReplacement(List.of(new QRecord().withValue("id", 1), new QRecord().withValue("id", 2), new QRecord().withValue("name", "new line")));
      UpdateInput omitted = sparseReplacement(List.of(new QRecord().withValue("id", 1)));
      List<UpdateInput> inputs = List.of(retained, inserted, omitted);
      List<String> before = inputs.stream().map(input -> JsonUtils.toJson(input.getRecords())).toList();
      List<String> metadataBefore = associationMetadataSnapshot();
      String storedBefore = storedSnapshot();

      QContext.setQSession(new QSession().withPermissions("header.edit", "line.insert"));
      assertThrows(QPermissionDeniedException.class, () -> AssociatedWritePermissions.check(retained));
      QContext.setQSession(new QSession().withPermissions("header.edit", "line.edit"));
      assertDoesNotThrow(() -> AssociatedWritePermissions.check(retained));
      assertThrows(QPermissionDeniedException.class, () -> AssociatedWritePermissions.check(inserted));
      assertThrows(QPermissionDeniedException.class, () -> AssociatedWritePermissions.check(omitted));
      assertEquals(before, inputs.stream().map(input -> JsonUtils.toJson(input.getRecords())).toList());
      assertEquals(metadataBefore, associationMetadataSnapshot());
      assertEquals(storedBefore, storedSnapshot());

      QContext.setQSession(new QSession().withPermissions("header.edit", "line.edit", "line.insert"));
      assertDoesNotThrow(() -> AssociatedWritePermissions.check(inserted));
      QContext.setQSession(new QSession().withPermissions("header.edit", "line.edit", "line.delete"));
      assertDoesNotThrow(() -> AssociatedWritePermissions.check(omitted));
      assertEquals(before, inputs.stream().map(input -> JsonUtils.toJson(input.getRecords())).toList());
      assertEquals(metadataBefore, associationMetadataSnapshot());
      assertEquals(storedBefore, storedSnapshot());
   }



   /*******************************************************************************
    ** Moving to a new tuple does not conceal omitted members of the old tuple.
    ** Supplying every old child remains legal with EDIT alone and no child READ.
    *******************************************************************************/
   @Test
   void testChangedCompositeTupleChecksOldOmissionsForward() throws Exception
   {
      assertChangedCompositeTupleChecksOldOmissions(false);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testChangedCompositeTupleChecksOldOmissionsReverse() throws Exception
   {
      assertChangedCompositeTupleChecksOldOmissions(true);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertChangedCompositeTupleChecksOldOmissions(boolean reverse) throws Exception
   {
      configureStoredComposite(reverse);
      UpdateInput retained = sparseReplacement(List.of(new QRecord().withValue("id", 1), new QRecord().withValue("id", 2)));
      UpdateInput omitted = sparseReplacement(List.of(new QRecord().withValue("id", 1)));
      for(UpdateInput input : List.of(retained, omitted))
      {
         input.getRecords().get(0).withValue("code", "NEW").withValue("region", 0);
      }
      List<String> before = List.of(JsonUtils.toJson(retained.getRecords()), JsonUtils.toJson(omitted.getRecords()));
      List<String> metadataBefore = associationMetadataSnapshot();
      String storedBefore = storedSnapshot();
      QContext.setQSession(new QSession().withPermissions("header.edit", "line.edit"));
      assertDoesNotThrow(() -> AssociatedWritePermissions.check(retained));
      assertThrows(QPermissionDeniedException.class, () -> AssociatedWritePermissions.check(omitted));
      QContext.setQSession(new QSession().withPermissions("header.edit", "line.edit", "line.delete"));
      assertDoesNotThrow(() -> AssociatedWritePermissions.check(omitted));
      assertEquals(before, List.of(JsonUtils.toJson(retained.getRecords()), JsonUtils.toJson(omitted.getRecords())));
      assertEquals(metadataBefore, associationMetadataSnapshot());
      assertEquals(storedBefore, storedSnapshot());
   }



   /*******************************************************************************
    ** Clearing a relationship still deletes old members, but an already-null
    ** stored tuple has none: unrelated unassigned children need no DELETE grant.
    *******************************************************************************/
   @Test
   void testNullReplacementPermissionsUseOldMembership() throws Exception
   {
      configureStoredComposite(false);
      UpdateInput input = sparseReplacement(List.of());
      input.getRecords().get(0).withValue("code", null);
      String before = JsonUtils.toJson(input.getRecords());
      List<String> metadataBefore = associationMetadataSnapshot();
      String storedBefore = storedSnapshot();
      QContext.setQSession(new QSession().withPermissions("header.edit"));
      assertThrows(QPermissionDeniedException.class, () -> AssociatedWritePermissions.check(input));
      assertEquals(storedBefore, storedSnapshot());
      QContext.setQSession(new QSession().withPermissions("header.edit", "line.delete"));
      assertDoesNotThrow(() -> AssociatedWritePermissions.check(input));
      assertEquals(before, JsonUtils.toJson(input.getRecords()));
      assertEquals(storedBefore, storedSnapshot());

      MemoryRecordStore.getInstance().update(new UpdateInput("header").withRecord(new QRecord().withValue("id", 1).withValue("code", null)), false);
      MemoryRecordStore.getInstance().update(new UpdateInput("line").withRecords(List.of(
         new QRecord().withValue("id", 1).withValue("headerCode", null),
         new QRecord().withValue("id", 2).withValue("headerCode", null))), false);
      String nullStoredBefore = storedSnapshot();
      QContext.setQSession(new QSession().withPermissions("header.edit"));
      assertDoesNotThrow(() -> AssociatedWritePermissions.check(input));
      assertEquals(before, JsonUtils.toJson(input.getRecords()));
      assertEquals(metadataBefore, associationMetadataSnapshot());
      assertEquals(nullStoredBefore, storedSnapshot());
   }



   /*******************************************************************************
    ** Empty or null nested association lists contain no requested inserts.
    *******************************************************************************/
   @Test
   void testNullNestedAssociationDoesNotMutateOrRequireInsert() throws Exception
   {
      QRecord child = record(1, 99).withAssociatedRecords("detail", null);
      InsertInput input = new InsertInput("header").withInputSource(QInputSource.USER)
         .withRecord(record(1, null).withAssociatedRecords("line", List.of(child)));
      QContext.getQSession().withPermissions("line.insert");
      String before = JsonUtils.toJson(input.getRecords());
      assertDoesNotThrow(() -> AssociatedWritePermissions.check(input));
      assertEquals(before, JsonUtils.toJson(input.getRecords()));
   }



   /*******************************************************************************
    ** Each level distinguishes existing ids, new records and omitted records.
    *******************************************************************************/
   @Test
   void testNestedUpdateOperationsAndNoMutation() throws Exception
   {
      UpdateInput input = new UpdateInput("header").withInputSource(QInputSource.USER).withRecord(record(1, null)
         .withAssociatedRecords("line", List.of(record(1, 99).withAssociatedRecords("detail", List.of(record(1, 98), record(null, 98))), record(2, 99))));
      String before = JsonUtils.toJson(input.getRecords());
      QContext.getQSession().withPermissions("line.edit", "detail.edit");
      assertThrows(QPermissionDeniedException.class, () -> AssociatedWritePermissions.check(input));
      assertEquals(before, JsonUtils.toJson(input.getRecords()));
      QContext.getQSession().withPermissions("detail.insert");
      assertDoesNotThrow(() -> AssociatedWritePermissions.check(input));
      assertEquals(before, JsonUtils.toJson(input.getRecords()));
   }



   /*******************************************************************************
    ** Deleting omitted children also checks their own raw cascade graph.
    *******************************************************************************/
   @Test
   void testOmissionChecksGrandchildDelete() throws Exception
   {
      UpdateInput input = new UpdateInput("header").withInputSource(QInputSource.USER).withRecord(record(1, null)
         .withAssociatedRecords("line", List.of(record(2, 1))));
      QContext.getQSession().withPermissions("line.edit", "line.delete");
      assertThrows(QPermissionDeniedException.class, () -> AssociatedWritePermissions.check(input));
      QContext.getQSession().withPermissions("detail.delete");
      assertDoesNotThrow(() -> AssociatedWritePermissions.check(input));
   }



   /*******************************************************************************
    ** Prospective normalization must not replace the stored old omission key.
    *******************************************************************************/
   @Test
   void testUpdateOmissionsUseStoredKeyBeforeProspectiveNormalization() throws Exception
   {
      instance.getTable("header").getField("name").withBehavior(CaseChangeBehavior.TO_UPPER_CASE);
      instance.getTable("line").withField(new QFieldMetaData("headerCode", QFieldType.STRING));
      instance.getJoin("headerJoinline").setJoinOns(List.of(new JoinOn("name", "headerCode")));
      MemoryRecordStore.getInstance().update(new UpdateInput("header").withRecord(new QRecord().withValue("id", 1).withValue("name", "CODE")), false);
      MemoryRecordStore.getInstance().insert(new InsertInput("line").withRecord(record(4, 1).withValue("headerCode", "CODE")), false);
      QRecord header = record(1, null).withValue("name", "new").withAssociatedRecords("line", List.of());
      UpdateInput input = new UpdateInput("header").withInputSource(QInputSource.USER).withRecord(header);
      assertThrows(QPermissionDeniedException.class, () -> AssociatedWritePermissions.check(input));
      assertEquals("new", header.getValueString("name"));
   }



   /*******************************************************************************
    ** A cascade with no affected grandchildren requires no grandchild grant.
    *******************************************************************************/
   @Test
   void testDeleteUsesAffectedKeysAndTerminatesCycles() throws Exception
   {
      QContext.getQSession().withPermissions("line.delete");
      assertDoesNotThrow(() -> AssociatedWritePermissions.check(new DeleteInput("line").withPrimaryKeys(List.of(2))));
      DeleteInput input = new DeleteInput("header").withInputSource(QInputSource.USER).withPrimaryKeys(List.of("1"));
      assertThrows(QPermissionDeniedException.class, () -> AssociatedWritePermissions.check(input));
      QContext.getQSession().withPermissions("detail.delete", "header.delete");
      instance.addJoin(new QJoinMetaData().withName("detailJoinHeader").withLeftTable("detail").withRightTable("header")
         .withType(JoinType.ONE_TO_ONE).withJoinOn(new JoinOn("id", "id")));
      instance.getTable("detail").withAssociation(new Association().withName("header").withAssociatedTableName("header").withJoinName("detailJoinHeader"));
      assertDoesNotThrow(() -> AssociatedWritePermissions.check(input));
      assertEquals(List.of("1"), input.getPrimaryKeys());
   }



   /*******************************************************************************
    ** A non-PK relationship uses stored hidden/heavy values before authorization.
    *******************************************************************************/
   @Test
   void testForwardNonPrimaryDeletePermissions() throws Exception
   {
      assertStoredDeletePermissions(false, false);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testReverseNonPrimaryDeletePermissions() throws Exception
   {
      assertStoredDeletePermissions(true, false);
   }



   /*******************************************************************************
    ** Every composite component constrains the raw cascade graph.
    *******************************************************************************/
   @Test
   void testForwardCompositeDeletePermissions() throws Exception
   {
      assertStoredDeletePermissions(false, true);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testReverseCompositeDeletePermissions() throws Exception
   {
      assertStoredDeletePermissions(true, true);
   }



   /*******************************************************************************
    ** Denied descendants are found without READ or display association metadata.
    ** Preflight itself never changes caller keys, metadata, or native records.
    *******************************************************************************/
   private void assertStoredDeletePermissions(boolean reverse, boolean composite) throws Exception
   {
      configureStoredComposite(reverse);
      if(!composite)
      {
         QJoinMetaData join = instance.getJoin("headerJoinline");
         join.setJoinOns(List.of(join.getJoinOns().get(0)));
      }
      addAssociation("line", "detail");
      if(composite)
      {
         MemoryRecordStore.getInstance().update(new UpdateInput("line").withRecord(
            new QRecord().withValue("id", 3).withValue("headerCode", "ALPHA").withValue("headerRegion", 8)), false);
      }
      instance.addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(HideLineAssociations.class));
      useTrackingDiscoveryBackend();
      DeleteInput input = new DeleteInput("header").withInputSource(QInputSource.USER).withPrimaryKeys(List.of("1", 1));
      List<String> metadataBefore = associationMetadataSnapshot();
      String storedBefore = completeStoredSnapshot();
      QContext.setQSession(new QSession().withPermissions("header.delete", "line.delete"));
      assertThrows(QPermissionDeniedException.class, () -> AssociatedWritePermissions.check(input));
      assertThat(storedValueReads).hasSize(1);
      assertEquals(composite ? Set.of("id", "code", "region") : Set.of("id", "code"), storedValueReads.get(0).getFieldNamesToInclude());
      assertThat(storedValueReads.get(0).getFilter().getCriteria().get(0).getValues()).containsExactly(1);
      assertEquals(storedBefore, completeStoredSnapshot());
      assertEquals(metadataBefore, associationMetadataSnapshot());
      assertEquals(List.of("1", 1), input.getPrimaryKeys());

      QContext.setQSession(new QSession().withPermissions("header.delete", "line.delete", "detail.delete"));
      assertDoesNotThrow(() -> AssociatedWritePermissions.check(input));
      assertEquals(storedBefore, completeStoredSnapshot());
      assertEquals(metadataBefore, associationMetadataSnapshot());
      assertEquals(List.of("1", 1), input.getPrimaryKeys());
      if(composite)
      {
         MemoryRecordStore.getInstance().update(new UpdateInput("detail").withRecord(new QRecord().withValue("id", 1).withValue("parentId", 3)), false);
         String outsideDescendantsBefore = completeStoredSnapshot();
         QContext.setQSession(new QSession().withPermissions("header.delete", "line.delete"));
         assertDoesNotThrow(() -> AssociatedWritePermissions.check(input));
         assertEquals(outsideDescendantsBefore, completeStoredSnapshot());
         assertEquals(metadataBefore, associationMetadataSnapshot());
         assertEquals(List.of("1", 1), input.getPrimaryKeys());
      }
   }



   /*******************************************************************************
    ** Null stored tuples have no members, including children with matching nulls.
    ** A non-null control still requires the otherwise absent DELETE grant.
    *******************************************************************************/
   @Test
   void testNullStoredDeleteTupleHasNoMembership() throws Exception
   {
      configureStoredComposite(true);
      MemoryRecordStore.getInstance().update(new UpdateInput("header").withRecord(new QRecord().withValue("id", 1).withValue("code", null)), false);
      MemoryRecordStore.getInstance().update(new UpdateInput("line").withRecords(List.of(
         new QRecord().withValue("id", 1).withValue("headerCode", null),
         new QRecord().withValue("id", 2).withValue("headerCode", null))), false);
      useTrackingDiscoveryBackend();
      String storedBefore = completeStoredSnapshot();
      DeleteInput input = new DeleteInput("header").withInputSource(QInputSource.USER).withPrimaryKeys(List.of("1"));
      QContext.setQSession(new QSession().withPermissions("header.delete"));
      assertDoesNotThrow(() -> AssociatedWritePermissions.check(input));
      assertThat(storedValueReads).hasSize(1);
      assertThat(membershipReads).isEmpty();
      assertThrows(QPermissionDeniedException.class, () -> AssociatedWritePermissions.check(new DeleteInput("header").withPrimaryKeys(List.of(2))));
      assertThat(membershipReads).hasSize(1);
      assertEquals(storedBefore, completeStoredSnapshot());
      assertEquals(List.of("1"), input.getPrimaryKeys());
   }



   /*******************************************************************************
    ** PK-only joins require no stored-value capability. A fully authorized raw
    ** leaf also needs neither lookup, even for a non-PK relationship.
    *******************************************************************************/
   @Test
   void testDeleteLookupFastPathsAndUnsupportedControl() throws Exception
   {
      useTrackingDiscoveryBackend();
      refuseStoredValues = true;
      QContext.setQSession(new QSession().withPermissions("header.delete", "line.delete", "detail.delete"));
      assertDoesNotThrow(() -> AssociatedWritePermissions.check(new DeleteInput("header").withPrimaryKeys(List.of(1))));
      assertThat(storedValueReads).isEmpty();
      assertThat(membershipReads).hasSize(1);
      QJoinMetaData forward = instance.getJoin("headerJoinline");
      instance.getJoins().put(forward.getName(), forward.flip());
      assertDoesNotThrow(() -> AssociatedWritePermissions.check(new DeleteInput("header").withPrimaryKeys(List.of(1))));
      assertThat(storedValueReads).isEmpty();
      assertThat(membershipReads).hasSize(2);
      instance.getJoins().put(forward.getName(), forward);

      configureStoredComposite(false);
      QBackendModuleDispatcher.registerBackendModule(new WithoutDiscoveryBackend());
      instance.getBackend(TestUtils.defineMemoryBackend().getName()).setBackendType(WithoutDiscoveryBackend.class);
      DeleteInput input = new DeleteInput("header").withInputSource(QInputSource.USER).withPrimaryKeys(List.of(1));
      String storedBefore = completeStoredSnapshot();
      assertDoesNotThrow(() -> AssociatedWritePermissions.check(input));
      QContext.setQSession(new QSession().withPermissions("header.delete"));
      assertEquals("Backend does not implement stored association-value lookup", assertThrows(QException.class, () -> AssociatedWritePermissions.check(input)).getMessage());
      assertEquals(storedBefore, completeStoredSnapshot());
   }



   /*******************************************************************************
    ** Both lookup operations carry the caller transaction; custom permission
    ** checks keep the original USER DeleteInput throughout recursive discovery.
    *******************************************************************************/
   @Test
   void testStoredDeleteTransactionAndOriginalPermissionInput() throws Exception
   {
      configureStoredComposite(false);
      addAssociation("line", "detail");
      useTrackingDiscoveryBackend();
      QBackendTransaction transaction = new QBackendTransaction();
      DeleteInput input = new DeleteInput("header").withInputSource(QInputSource.USER).withTransaction(transaction).withPrimaryKeys(List.of(1));
      QContext.setObject("expectedWriteInput", new AtomicReference<>(input));
      instance.getTable("line").getPermissionRules().setCustomPermissionChecker(new QCodeReference(OriginalInputChecker.class));
      assertThrows(QPermissionDeniedException.class, () -> AssociatedWritePermissions.check(input));
      assertThat(storedValueReads).hasSize(1).allSatisfy(query -> assertSame(transaction, query.getTransaction()));
      assertThat(membershipReads).hasSize(2).allSatisfy(query -> assertSame(transaction, query.getTransaction()));
      QContext.setQSession(new QSession().withPermissions("detail.delete"));
      assertDoesNotThrow(() -> AssociatedWritePermissions.check(input));
      assertEquals(List.of(1), input.getPrimaryKeys());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void useTrackingDiscoveryBackend()
   {
      QBackendModuleDispatcher.registerBackendModule(new TrackingDiscoveryBackend());
      instance.getBackend(TestUtils.defineMemoryBackend().getName()).setBackendType(TrackingDiscoveryBackend.class);
   }



   /*******************************************************************************
    ** Read all physical values independently, without changing table metadata.
    *******************************************************************************/
   private String completeStoredSnapshot() throws Exception
   {
      List<List<QRecord>> tables = new ArrayList<>();
      for(String table : List.of("header", "line", "detail"))
      {
         tables.add(MemoryRecordStore.getInstance().query(new QueryInput(table).withShouldFetchHeavyFields(true)
            .withShouldOmitHiddenFields(false).withShouldMaskPasswords(false)));
      }
      return JsonUtils.toJson(tables);
   }



   /*******************************************************************************
    ** INSERT/UPDATE honor USER metadata; DELETE remains schema-owned.
    *******************************************************************************/
   @Test
   void testPersonalizedWriteGraphAndRawDeleteGraph() throws Exception
   {
      instance.addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(HideLineAssociations.class));
      QContext.getQSession().withPermissions("line.insert", "line.edit", "line.delete");
      QRecord header = record(1, null).withAssociatedRecords("line", List.of(record(1, 1)
         .withAssociatedRecords("detail", List.of(record(1, 1))), record(2, 1)));
      assertDoesNotThrow(() -> AssociatedWritePermissions.check(new InsertInput("header").withInputSource(QInputSource.USER).withRecord(header)));
      assertDoesNotThrow(() -> AssociatedWritePermissions.check(new UpdateInput("header").withInputSource(QInputSource.USER).withRecord(header)));
      assertThrows(QPermissionDeniedException.class, () -> AssociatedWritePermissions.check(new InsertInput("header").withInputSource(QInputSource.SYSTEM).withRecord(header)));
      assertThrows(QPermissionDeniedException.class, () -> AssociatedWritePermissions.check(new DeleteInput("header").withInputSource(QInputSource.USER).withPrimaryKeys(List.of(1))));
      assertEquals(1, instance.getTable("line").getAssociations().size());
   }



   /*******************************************************************************
    ** A granted leaf needs no preflight discovery. Execution still requires the
    ** complete structural operation and must refuse before parent or child DML.
    *******************************************************************************/
   @Test
   void testGrantedLeafPreflightDoesNotRequireDiscoverySupport() throws Exception
   {
      QBackendModuleDispatcher.registerBackendModule(new WithoutDiscoveryBackend());
      instance.getBackend(TestUtils.defineMemoryBackend().getName()).setBackendType(WithoutDiscoveryBackend.class);
      instance.getTable("line").setAssociations(List.of());
      QContext.getQSession().withPermissions("header.edit", "line.delete");
      UpdateInput input = new UpdateInput("header").withInputSource(QInputSource.USER)
         .withRecord(record(1, null).withAssociatedRecords("line", List.of()));
      String storedBefore = completeStoredSnapshot();
      assertDoesNotThrow(() -> AssociatedWritePermissions.check(input));
      QException executionError = assertThrows(QException.class, () -> new UpdateAction().execute(input));
      assertEquals("Backend does not implement structural association discovery", executionError.getMessage());
      assertEquals(storedBefore, completeStoredSnapshot());

      QContext.setQSession(new QSession().withPermissions("header.edit"));
      UpdateInput denied = new UpdateInput("header").withInputSource(QInputSource.USER)
         .withRecord(record(2, null).withAssociatedRecords("line", List.of()));
      QException exception = assertThrows(QException.class, () -> AssociatedWritePermissions.check(denied));
      assertEquals("Backend does not implement structural association discovery", exception.getMessage());
      assertEquals(storedBefore, completeStoredSnapshot());
   }



   /*******************************************************************************
    ** Known INSERT/EDIT denial needs no discovery and must not become an adapter error.
    *******************************************************************************/
   @Test
   void testKnownWritesDeniedBeforeUnsupportedDiscovery() throws Exception
   {
      QBackendModuleDispatcher.registerBackendModule(new WithoutDiscoveryBackend());
      instance.getBackend(TestUtils.defineMemoryBackend().getName()).setBackendType(WithoutDiscoveryBackend.class);
      instance.getTable("line").setAssociations(List.of());
      QContext.getQSession().withPermissions("header.edit");
      UpdateInput edit = new UpdateInput("header").withInputSource(QInputSource.USER)
         .withRecord(record(2, null).withAssociatedRecords("line", List.of(record(3, 2))));
      UpdateInput insert = new UpdateInput("header").withInputSource(QInputSource.USER)
         .withRecord(record(2, null).withAssociatedRecords("line", List.of(record(null, 2))));
      assertAll(() -> assertThrows(QPermissionDeniedException.class, () -> AssociatedWritePermissions.check(edit)),
         () -> assertThrows(QPermissionDeniedException.class, () -> AssociatedWritePermissions.check(insert)));
      assertEquals(3, new QueryAction().execute(new QueryInput("line")).getRecords().size());
   }



   /*******************************************************************************
    ** Custom checkers receive the original HTTP input and actual child table.
    *******************************************************************************/
   @Test
   void testCustomCheckerKeepsOriginalInput() throws Exception
   {
      InsertInput input = new InsertInput("header").withInputSource(QInputSource.USER).withRecord(record(9, null)
         .withAssociatedRecords("line", List.of(record(null, 9))));
      QContext.setObject("expectedWriteInput", new AtomicReference<>(input));
      instance.getTable("line").getPermissionRules().setCustomPermissionChecker(new QCodeReference(OriginalInputChecker.class));
      assertDoesNotThrow(() -> AssociatedWritePermissions.check(input));
   }



   /*******************************************************************************
    ** Malformed cascade metadata still fails before the parent's backend write.
    *******************************************************************************/
   @Test
   void testMalformedDeleteJoinFailsClosed()
   {
      instance.getJoin("headerJoinline").withJoinOn(new JoinOn("missingPhysicalField", "name"));
      assertThrows(QException.class,
         () -> AssociatedWritePermissions.check(new DeleteInput("header").withPrimaryKeys(List.of(1))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void configureStoredComposite(boolean reverse) throws Exception
   {
      instance.getTable("header").withField(new QFieldMetaData("code", QFieldType.STRING).withIsHidden(true).withIsHeavy(true))
         .withField(new QFieldMetaData("region", QFieldType.INTEGER).withIsHidden(true).withIsHeavy(true));
      instance.getTable("line").withField(new QFieldMetaData("headerCode", QFieldType.STRING))
         .withField(new QFieldMetaData("headerRegion", QFieldType.INTEGER)).withAssociations(List.of());
      QJoinMetaData join = instance.getJoin("headerJoinline").withJoinOns(List.of(new JoinOn("code", "headerCode"), new JoinOn("region", "headerRegion")));
      if(reverse)
      {
         instance.getJoins().put(join.getName(), join.flip());
      }
      MemoryRecordStore.getInstance().update(new UpdateInput("header").withRecords(List.of(
         new QRecord().withValue("id", 1).withValue("code", "ALPHA").withValue("region", 7),
         new QRecord().withValue("id", 2).withValue("code", "BETA").withValue("region", 7))), false);
      MemoryRecordStore.getInstance().update(new UpdateInput("line").withRecords(List.of(
         new QRecord().withValue("id", 1).withValue("headerCode", "ALPHA").withValue("headerRegion", 7),
         new QRecord().withValue("id", 2).withValue("headerCode", "ALPHA").withValue("headerRegion", 7),
         new QRecord().withValue("id", 3).withValue("headerCode", "BETA").withValue("headerRegion", 7))), false);
   }



   /*******************************************************************************
    ** Do not include relationship fields on the parent or children in this patch.
    *******************************************************************************/
   private UpdateInput sparseReplacement(List<QRecord> children)
   {
      return new UpdateInput("header").withInputSource(QInputSource.USER)
         .withRecord(new QRecord().withValue("id", "1").withValue("name", "Changed").withAssociatedRecords("line", children));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<String> associationMetadataSnapshot()
   {
      return List.of(JsonUtils.toJson(instance.getTable("header")), JsonUtils.toJson(instance.getTable("line")), JsonUtils.toJson(instance.getJoin("headerJoinline")));
   }



   /*******************************************************************************
    ** Snapshot native memory storage independently from the permission helper.
    *******************************************************************************/
   private String storedSnapshot() throws Exception
   {
      return JsonUtils.toJson(List.of(MemoryRecordStore.getInstance().query(new QueryInput("header")),
         MemoryRecordStore.getInstance().query(new QueryInput("line"))));
   }



   /*******************************************************************************
    ** Rewiring a fixture may restore a previously removed association whose join
    ** is still registered; replace that owned declaration deliberately.
    *******************************************************************************/
   private void addAssociation(String parent, String child)
   {
      String joinName = parent + "Join" + child;
      instance.getJoins().put(joinName, new QJoinMetaData().withName(joinName).withLeftTable(parent).withRightTable(child)
         .withType(JoinType.ONE_TO_MANY).withJoinOn(new JoinOn("id", "parentId")));
      QTableMetaData table = instance.getTable(parent);
      List<Association> associations = new ArrayList<>(table.getAssociations() == null ? List.of() : table.getAssociations());
      associations.add(new Association().withName(child).withAssociatedTableName(child).withJoinName(joinName));
      table.setAssociations(associations);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord record(Integer id, Integer parentId)
   {
      return new QRecord().withValue("id", id).withValue("parentId", parentId).withValue("name", "Original");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class HideLineAssociations implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         return "line".equals(input.getTableName()) && QInputSource.USER.equals(input.getInputSource())
            ? input.getTable().clone().withAssociations(List.of()) : input.getTable();
      }
   }



   /*******************************************************************************
    ** Capture only native lookup requests while retaining real Memory storage.
    *******************************************************************************/
   public static class TrackingDiscoveryBackend extends MemoryBackendModule
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public String getBackendType()
      {
         return "trackingAssociationDeleteDiscovery";
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QueryInterface getQueryInterface()
      {
         return new MemoryQueryAction()
         {
            @Override
            public List<QRecord> readAssociationValues(AssociatedRecordDiscovery.StoredValuesInput input) throws QException
            {
               storedValueReads.add(input.newQueryInput());
               if(refuseStoredValues)
               {
                  throw new QException("Stored-value lookup is unavailable in this control");
               }
               return super.readAssociationValues(input);
            }



            @Override
            public List<Serializable> findAssociatedPrimaryKeys(AssociatedRecordDiscovery.Input input) throws QException
            {
               membershipReads.add(input.newQueryInput());
               return super.findAssociatedPrimaryKeys(input);
            }
         };
      }
   }



   /*******************************************************************************
    ** Ordinary CRUD remains available, while QueryInterface's default discovery
    ** operation deliberately remains unsupported.
    *******************************************************************************/
   public static class WithoutDiscoveryBackend extends MemoryBackendModule
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public String getBackendType()
      {
         return "withoutDiscoveryForPermissionTest";
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QueryInterface getQueryInterface()
      {
         return new MemoryQueryAction()::execute;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class OriginalInputChecker implements CustomPermissionChecker
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void checkPermissionsThrowing(AbstractActionInput input, MetaDataWithPermissionRules metadata)
      {
         assertSame(((AtomicReference<?>) QContext.getObject("expectedWriteInput")).get(), input);
         assertEquals("line", metadata.getName());

      }
   }
}
