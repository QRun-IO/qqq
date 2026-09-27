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

package com.kingsrook.qqq.backend.module.rdbms.actions;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.CaseChangeBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Association reads and writes follow declared join pairs, with native
 ** SQL readback independent of the action's returned records.
 *******************************************************************************/
class AssociationShapeTest extends RDBMSActionTest
{
   private static final String PARENT = "associationShapeParent";
   private static final String CHILD = "associationShapeChild";
   private static final String GROUP = "care / primary";
   private static final String JOIN = "associationShapeRelation";

   private QTableMetaData parent;
   private QTableMetaData child;
   private QJoinMetaData join;
   private String registeredJoin;
   private String registeredAssociations;

   /*******************************************************************************
    ** Shapes accepted by the existing association metadata validator.
    *******************************************************************************/
   private enum Shape
   {
      PRIMARY,
      REVERSE,
      NON_PRIMARY,
      COMPOSITE
   }



   /*******************************************************************************
    ** Include irrelevant columns on both sides so a reversed assignment can write
    ** incorrect values instead of merely failing due to absent field metadata.
    *******************************************************************************/
   @BeforeEach
   void seed() throws Exception
   {
      primeTestDatabase();
      executeSql("CREATE TABLE association_shape_parent (id INT GENERATED BY DEFAULT AS IDENTITY(START WITH 100) PRIMARY KEY, parent_id INT, code VARCHAR(30), parent_code VARCHAR(30), region INT, payload VARCHAR(30))");
      executeSql("CREATE TABLE association_shape_child (id INT GENERATED BY DEFAULT AS IDENTITY(START WITH 100) PRIMARY KEY, parent_id INT, code VARCHAR(30), parent_code VARCHAR(30), region INT, payload VARCHAR(30))");
      executeSql("INSERT INTO association_shape_parent(id,parent_id,code,region,payload) VALUES (1,999,'ALPHA',7,'original'),(2,888,'BETA',7,'other')");
      executeSql("INSERT INTO association_shape_child(id,parent_id,parent_code,region,payload) VALUES (10,1,'ALPHA',7,'keep'),(20,1,'ALPHA',7,'omit'),(30,2,'BETA',7,'outside')");
      parent = table(PARENT, "association_shape_parent");
      child = table(CHILD, "association_shape_child");
      parent.withAssociation(new Association().withName(GROUP).withAssociatedTableName(CHILD).withJoinName(JOIN));
      QContext.getQInstance().addTable(parent);
      QContext.getQInstance().addTable(child);
   }



   /*******************************************************************************
    ** Drop only owned tables; leave the shared fixture lifecycle intact.
    *******************************************************************************/
   @AfterEach
   void removeFixture() throws Exception
   {
      executeSql("DROP TABLE IF EXISTS association_shape_child");
      executeSql("DROP TABLE IF EXISTS association_shape_parent");
   }



   /*******************************************************************************
    ** Exact named groups must attach matching children in either join direction.
    *******************************************************************************/
   @ParameterizedTest
   @EnumSource(Shape.class)
   void testExpandedQueryAndGet(Shape shape) throws Exception
   {
      configure(shape);
      List<Map<String, Object>> before = rows("SELECT * FROM association_shape_child ORDER BY id");
      assertMembers(query(1), 10, 20);
      assertMembers(get(1), 10, 20);
      assertMembers(query(2), 30);
      assertMembers(get(2), 30);
      assertEquals(before, rows("SELECT * FROM association_shape_child ORDER BY id"));
      assertMetadataUnchanged();
   }



   /*******************************************************************************
    ** Cascades follow the registered relationship in either direction and compare
    ** every component before deleting the parent or any related child.
    *******************************************************************************/
   @ParameterizedTest
   @EnumSource(Shape.class)
   void testDeleteUsesDeclaredRelationship(Shape shape) throws Exception
   {
      configure(shape);
      if(shape == Shape.COMPOSITE)
      {
         executeSql("INSERT INTO association_shape_child(id,parent_id,parent_code,region,payload) VALUES (40,1,'ALPHA',8,'different region')");
      }
      List<Map<String, Object>> unrelatedParent = rows("SELECT * FROM association_shape_parent WHERE id=2");
      List<Map<String, Object>> unrelatedChild = rows("SELECT * FROM association_shape_child WHERE id>=30 ORDER BY id");
      DeleteOutput output = new DeleteAction().execute(new DeleteInput(PARENT).withInputSource(QInputSource.USER).withPrimaryKeys(List.of("1")));
      assertEquals(1, output.getDeletedRecordCount());
      assertThat(output.getRecordsWithErrors()).isNullOrEmpty();
      assertThat(rows("SELECT * FROM association_shape_parent WHERE id=1")).isEmpty();
      assertThat(rows("SELECT * FROM association_shape_child WHERE id IN(10,20)")).isEmpty();
      assertEquals(unrelatedParent, rows("SELECT * FROM association_shape_parent ORDER BY id"));
      assertEquals(unrelatedChild, rows("SELECT * FROM association_shape_child ORDER BY id"));
      assertMetadataUnchanged();
   }



   /*******************************************************************************
    ** DELETE keeps the schema-owned graph even when ordinary metadata omits a
    ** physical relationship field; raw hidden/heavy components stay private.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(booleans = { false, true })
   void testDeleteUsesHiddenStoredCompositeTuple(boolean reverse) throws Exception
   {
      parent.getField("code").withIsHidden(true).withIsHeavy(true);
      parent.getField("region").withIsHidden(true).withIsHeavy(true);
      configureComposite(reverse);
      executeSql("INSERT INTO association_shape_child(id,parent_id,parent_code,region,payload) VALUES (40,1,'ALPHA',8,'different region')");
      QContext.getQInstance().addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(RemoveParentCode.class));
      assertThat(new QueryAction().execute(new QueryInput(PARENT)).getRecords())
         .allSatisfy(record -> assertThat(record.getValues()).doesNotContainKeys("code", "region"));
      List<Map<String, Object>> unrelatedParent = rows("SELECT * FROM association_shape_parent WHERE id=2");
      List<Map<String, Object>> unrelatedChild = rows("SELECT * FROM association_shape_child WHERE id>=30 ORDER BY id");
      DeleteOutput output = new DeleteAction().execute(new DeleteInput(PARENT).withInputSource(QInputSource.USER).withPrimaryKeys(List.of(1)));
      assertEquals(1, output.getDeletedRecordCount());
      assertThat(output.getRecordsWithErrors()).isNullOrEmpty();
      assertEquals(unrelatedParent, rows("SELECT * FROM association_shape_parent ORDER BY id"));
      assertEquals(unrelatedChild, rows("SELECT * FROM association_shape_child ORDER BY id"));
      assertThat(JsonUtils.toJson(output)).doesNotContain("ALPHA");
      assertMetadataUnchanged();
   }



   /*******************************************************************************
    ** A null stored tuple has no members; deleting its parent must not remove
    ** unrelated unassigned children whose relationship fields are also null.
    *******************************************************************************/
   @ParameterizedTest
   @EnumSource(value = Shape.class, names = { "NON_PRIMARY", "COMPOSITE" })
   void testDeleteNullTuplePreservesUnassignedChildren(Shape shape) throws Exception
   {
      configure(shape);
      executeSql("UPDATE association_shape_parent SET code=NULL WHERE id=1");
      executeSql("UPDATE association_shape_child SET parent_code=NULL WHERE id IN(10,20)");
      List<Map<String, Object>> beforeChildren = rows("SELECT * FROM association_shape_child ORDER BY id");
      List<Map<String, Object>> unrelatedParent = rows("SELECT * FROM association_shape_parent WHERE id=2");
      DeleteOutput output = new DeleteAction().execute(new DeleteInput(PARENT).withInputSource(QInputSource.USER).withPrimaryKeys(List.of(1)));
      assertEquals(1, output.getDeletedRecordCount());
      assertThat(output.getRecordsWithErrors()).isNullOrEmpty();
      assertEquals(beforeChildren, rows("SELECT * FROM association_shape_child ORDER BY id"));
      assertEquals(unrelatedParent, rows("SELECT * FROM association_shape_parent ORDER BY id"));
      assertMetadataUnchanged();
   }



   /*******************************************************************************
    ** Failed private projection cannot delete the parent before discovering that
    ** its relationship cannot be resolved from native storage.
    *******************************************************************************/
   @Test
   void testDeleteInvalidStoredProjectionPreservesParent() throws Exception
   {
      parent.getField("code").withBackendName("missing_native_column");
      configure(Shape.NON_PRIMARY);
      QContext.getQInstance().addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(RemoveParentCode.class));
      assertEquals(2, new QueryAction().execute(new QueryInput(PARENT)).getRecords().size());
      List<Map<String, Object>> beforeParents = rows("SELECT * FROM association_shape_parent ORDER BY id");
      List<Map<String, Object>> beforeChildren = rows("SELECT * FROM association_shape_child ORDER BY id");
      assertThrows(QException.class, () -> new DeleteAction().execute(new DeleteInput(PARENT).withInputSource(QInputSource.USER).withPrimaryKeys(List.of(1))));
      assertEquals(beforeParents, rows("SELECT * FROM association_shape_parent ORDER BY id"));
      assertEquals(beforeChildren, rows("SELECT * FROM association_shape_child ORDER BY id"));
      assertMetadataUnchanged();
   }



   /*******************************************************************************
    ** Generated parent IDs and all non-PK components must reach the native child
    ** row, without rewriting existing or unrelated children.
    *******************************************************************************/
   @ParameterizedTest
   @EnumSource(Shape.class)
   void testNestedInsertAssignsDeclaredRelationship(Shape shape) throws Exception
   {
      configure(shape);
      List<Map<String, Object>> before = rows("SELECT * FROM association_shape_child ORDER BY id");
      QRecord input = new QRecord().withValue("parentId", 777).withValue("code", "NEW").withValue("region", 0).withValue("payload", "inserted")
         .withAssociatedRecord(GROUP, new QRecord().withValue("payload", "new child"));
      QRecord output = new InsertAction().execute(new InsertInput(PARENT).withInputSource(QInputSource.USER).withRecord(input)).getRecords().get(0);
      assertNoErrors(output);
      Integer parentId = output.getValueInteger("id");
      assertNotNull(parentId);
      List<Map<String, Object>> inserted = rows("SELECT * FROM association_shape_child WHERE payload='new child'");
      assertEquals(1, inserted.size());
      assertStoredRelationship(shape, inserted.get(0), parentId, "NEW", 0);
      assertEquals(before, rows("SELECT * FROM association_shape_child WHERE payload<>'new child' ORDER BY id"));
      assertMembers(get(parentId), ((Number) inserted.get(0).get("ID")).intValue());
      assertMetadataUnchanged();
   }



   /*******************************************************************************
    ** A complete replacement changes its retained child, removes its omitted
    ** child and inserts a new child, preserving the unrelated parent and child.
    *******************************************************************************/
   @ParameterizedTest
   @EnumSource(Shape.class)
   void testCompleteUpdatePreservesUnrelatedRecords(Shape shape) throws Exception
   {
      configure(shape);
      List<Map<String, Object>> unrelatedParent = rows("SELECT * FROM association_shape_parent WHERE id=2");
      List<Map<String, Object>> unrelatedChild = rows("SELECT * FROM association_shape_child WHERE id=30");
      QRecord patch = new QRecord().withValue("id", 1).withValue("parentId", 999).withValue("code", "ALPHA").withValue("region", 7).withValue("payload", "updated")
         .withAssociatedRecord(GROUP, new QRecord().withValue("id", 10).withValue("payload", "changed"))
         .withAssociatedRecord(GROUP, new QRecord().withValue("payload", "new child"));
      QRecord output = new UpdateAction().execute(new UpdateInput(PARENT).withInputSource(QInputSource.USER).withRecord(patch)).getRecords().get(0);
      assertNoErrors(output);
      assertEquals("updated", rows("SELECT payload FROM association_shape_parent WHERE id=1").get(0).get("PAYLOAD"));
      assertThat(rows("SELECT * FROM association_shape_child WHERE id=20")).isEmpty();
      Map<String, Object> retained = rows("SELECT * FROM association_shape_child WHERE id=10").get(0);
      assertEquals("changed", retained.get("PAYLOAD"));
      assertStoredRelationship(shape, retained, 1, "ALPHA", 7);
      List<Map<String, Object>> inserted = rows("SELECT * FROM association_shape_child WHERE payload='new child'");
      assertEquals(1, inserted.size());
      assertStoredRelationship(shape, inserted.get(0), 1, "ALPHA", 7);
      assertEquals(3, rows("SELECT * FROM association_shape_child").size());
      assertEquals(unrelatedParent, rows("SELECT * FROM association_shape_parent WHERE id=2"));
      assertEquals(unrelatedChild, rows("SELECT * FROM association_shape_child WHERE id=30"));
      assertMembers(get(1), 10, ((Number) inserted.get(0).get("ID")).intValue());
      assertMetadataUnchanged();
   }



   /*******************************************************************************
    ** Hidden/heavy stored tuple components guide sparse replacement privately,
    ** including when the registered join points from the child to the parent.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(booleans = { false, true })
   void testSparseUpdateUsesPrivateStoredCompositeTuple(boolean reverse) throws Exception
   {
      parent.getField("code").withIsHidden(true).withIsHeavy(true);
      parent.getField("region").withIsHidden(true).withIsHeavy(true);
      configureComposite(reverse);
      assertThat(new QueryAction().execute(new QueryInput(PARENT)).getRecords())
         .allSatisfy(record -> assertThat(record.getValues()).doesNotContainKeys("code", "region"));
      List<Map<String, Object>> unrelatedParent = rows("SELECT * FROM association_shape_parent WHERE id=2");
      List<Map<String, Object>> unrelatedChild = rows("SELECT * FROM association_shape_child WHERE id=30");
      QRecord retainedInput = new QRecord().withValue("id", 10).withValue("payload", "changed");
      QRecord insertedInput = new QRecord().withValue("payload", "new child");
      QRecord patch = new QRecord().withValue("id", "1").withValue("payload", "sparse updated")
         .withAssociatedRecords(GROUP, List.of(retainedInput, insertedInput));

      QRecord output = new UpdateAction().execute(new UpdateInput(PARENT).withInputSource(QInputSource.USER).withRecord(patch)).getRecords().get(0);
      assertNoErrors(output);
      assertEquals("sparse updated", rows("SELECT payload FROM association_shape_parent WHERE id=1").get(0).get("PAYLOAD"));
      assertThat(rows("SELECT * FROM association_shape_child WHERE id=20")).isEmpty();
      Map<String, Object> retained = rows("SELECT * FROM association_shape_child WHERE id=10").get(0);
      assertEquals("changed", retained.get("PAYLOAD"));
      assertStoredRelationship(Shape.COMPOSITE, retained, 1, "ALPHA", 7);
      List<Map<String, Object>> inserted = rows("SELECT * FROM association_shape_child WHERE payload='new child'");
      assertEquals(1, inserted.size());
      assertStoredRelationship(Shape.COMPOSITE, inserted.get(0), 1, "ALPHA", 7);
      assertEquals(3, rows("SELECT * FROM association_shape_child").size());
      assertEquals(unrelatedParent, rows("SELECT * FROM association_shape_parent WHERE id=2"));
      assertEquals(unrelatedChild, rows("SELECT * FROM association_shape_child WHERE id=30"));
      for(QRecord record : List.of(patch, output))
      {
         assertThat(record.getValues()).doesNotContainKeys("code", "region");
         assertThat(record.getAssociatedRecords()).containsOnlyKeys(GROUP);
         assertThat(record.getAssociatedRecords().get(GROUP)).hasSize(2)
            .allSatisfy(childRecord -> assertThat(childRecord.getValues()).doesNotContainKeys("parentCode", "region"));
      }
      assertThat(retainedInput.getValues()).doesNotContainKeys("parentCode", "region");
      assertThat(insertedInput.getValues()).doesNotContainKeys("parentCode", "region");
      assertMetadataUnchanged();
   }



   /*******************************************************************************
    ** Omissions belong to the stored old tuple; retained/new children use the
    ** prospective tuple, even when the caller supplies every join component.
    *******************************************************************************/
   @ParameterizedTest
   @EnumSource(value = Shape.class, names = { "NON_PRIMARY", "COMPOSITE" })
   void testChangedTupleDeletesOldOmissionsAndRelinksReplacement(Shape shape) throws Exception
   {
      configure(shape);
      List<Map<String, Object>> unrelatedParent = rows("SELECT * FROM association_shape_parent WHERE id=2");
      List<Map<String, Object>> unrelatedChild = rows("SELECT * FROM association_shape_child WHERE id=30");
      QRecord patch = new QRecord().withValue("id", 1).withValue("code", "NEW").withValue("region", 0).withValue("payload", "moved")
         .withAssociatedRecord(GROUP, new QRecord().withValue("id", 10).withValue("payload", "changed"))
         .withAssociatedRecord(GROUP, new QRecord().withValue("payload", "new child"));
      QRecord output = new UpdateAction().execute(new UpdateInput(PARENT).withInputSource(QInputSource.USER).withRecord(patch)).getRecords().get(0);
      assertNoErrors(output);
      assertEquals("NEW", rows("SELECT code FROM association_shape_parent WHERE id=1").get(0).get("CODE"));
      assertThat(rows("SELECT * FROM association_shape_child WHERE id=20")).isEmpty();
      Map<String, Object> retained = rows("SELECT * FROM association_shape_child WHERE id=10").get(0);
      assertEquals("changed", retained.get("PAYLOAD"));
      assertStoredRelationship(shape, retained, 1, "NEW", 0);
      List<Map<String, Object>> inserted = rows("SELECT * FROM association_shape_child WHERE payload='new child'");
      assertEquals(1, inserted.size());
      assertStoredRelationship(shape, inserted.get(0), 1, "NEW", 0);
      assertEquals(3, rows("SELECT * FROM association_shape_child").size());
      assertEquals(unrelatedParent, rows("SELECT * FROM association_shape_parent WHERE id=2"));
      assertEquals(unrelatedChild, rows("SELECT * FROM association_shape_child WHERE id=30"));
      assertMembers(get(1), 10, ((Number) inserted.get(0).get("ID")).intValue());
      assertMetadataUnchanged();
   }



   /*******************************************************************************
    ** A partial composite overlay preserves the missing component privately and
    ** still removes omissions from the entire stored old relationship.
    *******************************************************************************/
   @Test
   void testSparseCompositeOverlayPreservesMissingComponent() throws Exception
   {
      parent.getField("region").withIsHidden(true).withIsHeavy(true);
      configure(Shape.COMPOSITE);
      List<Map<String, Object>> unrelated = rows("SELECT * FROM association_shape_child WHERE id=30");
      QRecord childPatch = new QRecord().withValue("id", 10).withValue("payload", "changed");
      QRecord patch = new QRecord().withValue("id", 1).withValue("code", "NEW").withAssociatedRecord(GROUP, childPatch);
      QRecord output = new UpdateAction().execute(new UpdateInput(PARENT).withInputSource(QInputSource.USER).withRecord(patch)).getRecords().get(0);
      assertNoErrors(output);
      assertEquals(Map.of("CODE", "NEW", "REGION", 7), rows("SELECT code,region FROM association_shape_parent WHERE id=1").get(0));
      assertStoredRelationship(Shape.COMPOSITE, rows("SELECT * FROM association_shape_child WHERE id=10").get(0), 1, "NEW", 7);
      assertThat(rows("SELECT * FROM association_shape_child WHERE id=20")).isEmpty();
      assertEquals(unrelated, rows("SELECT * FROM association_shape_child WHERE id=30"));
      assertThat(patch.getValues()).doesNotContainKey("region");
      assertThat(output.getValues()).doesNotContainKey("region");
      assertThat(childPatch.getValues()).doesNotContainKey("region");
      assertThat(output.getAssociatedRecords().get(GROUP)).hasSize(1)
         .allSatisfy(record -> assertThat(record.getValues()).doesNotContainKey("region"));
      assertMetadataUnchanged();
   }



   /*******************************************************************************
    ** Copying private child inputs must preserve ordinary normalization/defaults
    ** in both native storage and the returned nested records.
    *******************************************************************************/
   @Test
   void testSparseChildWorkingCopiesPublishNormalizationAndDefaults() throws Exception
   {
      parent.getField("code").withIsHidden(true).withIsHeavy(true);
      parent.getField("region").withIsHidden(true).withIsHeavy(true);
      child.getField("payload").withBehavior(CaseChangeBehavior.TO_UPPER_CASE);
      child.getField("code").withDefaultValue("child default");
      configure(Shape.COMPOSITE);
      QRecord retained = new QRecord().withValue("id", 10).withValue("payload", "changed");
      QRecord inserted = new QRecord().withValue("payload", "new child");
      QRecord patch = new QRecord().withValue("id", 1).withAssociatedRecords(GROUP, List.of(retained, inserted));
      QRecord output = new UpdateAction().execute(new UpdateInput(PARENT).withInputSource(QInputSource.USER).withRecord(patch)).getRecords().get(0);
      assertNoErrors(output);
      assertEquals("CHANGED", rows("SELECT payload FROM association_shape_child WHERE id=10").get(0).get("PAYLOAD"));
      Map<String, Object> storedInsert = rows("SELECT * FROM association_shape_child WHERE payload='NEW CHILD'").get(0);
      assertEquals("child default", storedInsert.get("CODE"));
      assertStoredRelationship(Shape.COMPOSITE, storedInsert, 1, "ALPHA", 7);
      assertEquals("CHANGED", retained.getValueString("payload"));
      assertEquals("NEW CHILD", inserted.getValueString("payload"));
      assertEquals("child default", inserted.getValueString("code"));
      assertEquals(((Number) storedInsert.get("ID")).intValue(), inserted.getValueInteger("id"));
      assertThat(output.getAssociatedRecords().get(GROUP)).hasSize(2)
         .allSatisfy(record -> assertThat(record.getValues()).doesNotContainKeys("parentCode", "region"));
      assertThat(output.getAssociatedRecords().get(GROUP)).extracting(record -> record.getValueString("payload"))
         .containsExactly("CHANGED", "NEW CHILD");
      assertEquals("child default", output.getAssociatedRecords().get(GROUP).get(1).getValueString("code"));
      assertMetadataUnchanged();
   }



   /*******************************************************************************
    ** A field removed from active metadata is not a writable overlay merely
    ** because the private stored projection restores its physical declaration.
    *******************************************************************************/
   @Test
   void testForgedRemovedParentFieldCannotChangeChildRelationship() throws Exception
   {
      configure(Shape.NON_PRIMARY);
      QContext.getQInstance().addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(RemoveParentCode.class));
      List<Map<String, Object>> unrelated = rows("SELECT * FROM association_shape_child WHERE id=30");
      QRecord retained = new QRecord().withValue("id", 10).withValue("payload", "changed");
      QRecord inserted = new QRecord().withValue("payload", "new child");
      QRecord patch = new QRecord().withValue("id", 1).withValue("code", "FORGED").withValue("payload", "changed")
         .withAssociatedRecords(GROUP, List.of(retained, inserted));
      QRecord output = new UpdateAction().execute(new UpdateInput(PARENT).withInputSource(QInputSource.USER).withRecord(patch)).getRecords().get(0);
      assertNoErrors(output);
      assertEquals("ALPHA", rows("SELECT code FROM association_shape_parent WHERE id=1").get(0).get("CODE"));
      assertEquals("changed", rows("SELECT payload FROM association_shape_parent WHERE id=1").get(0).get("PAYLOAD"));
      assertStoredRelationship(Shape.NON_PRIMARY, rows("SELECT * FROM association_shape_child WHERE id=10").get(0), 1, "ALPHA", 7);
      assertStoredRelationship(Shape.NON_PRIMARY, rows("SELECT * FROM association_shape_child WHERE payload='new child'").get(0), 1, "ALPHA", 7);
      assertThat(rows("SELECT * FROM association_shape_child WHERE id=20")).isEmpty();
      assertEquals(unrelated, rows("SELECT * FROM association_shape_child WHERE id=30"));
      assertThat(output.getAssociatedRecords().get(GROUP)).hasSize(2)
         .allSatisfy(record -> assertThat(record.getValues()).doesNotContainKey("parentCode"));
      assertThat(retained.getValues()).doesNotContainKey("parentCode");
      assertThat(inserted.getValues()).doesNotContainKey("parentCode");
      assertThat(patch.getValues()).doesNotContainValue("ALPHA");
      assertThat(output.getValues()).doesNotContainValue("ALPHA");
      assertMetadataUnchanged();
   }



   /*******************************************************************************
    ** Explicit null is a supplied value. An empty replacement removes old members
    ** before leaving the parent with no relationship; unrelated rows survive.
    *******************************************************************************/
   @ParameterizedTest
   @EnumSource(value = Shape.class, names = { "NON_PRIMARY", "COMPOSITE" })
   void testNullProspectiveTupleWithEmptyReplacementDeletesOldMembers(Shape shape) throws Exception
   {
      configure(shape);
      List<Map<String, Object>> unrelatedParent = rows("SELECT * FROM association_shape_parent WHERE id=2");
      List<Map<String, Object>> unrelatedChild = rows("SELECT * FROM association_shape_child WHERE id=30");
      QRecord patch = new QRecord().withValue("id", 1).withValue("code", null).withValue("payload", "cleared")
         .withAssociatedRecords(GROUP, List.of());
      QRecord output = new UpdateAction().execute(new UpdateInput(PARENT).withInputSource(QInputSource.USER).withRecord(patch)).getRecords().get(0);
      assertNoErrors(output);
      assertThat(rows("SELECT code,payload FROM association_shape_parent WHERE id=1").get(0)).containsEntry("CODE", null).containsEntry("PAYLOAD", "cleared");
      assertThat(rows("SELECT * FROM association_shape_child WHERE id IN(10,20)")).isEmpty();
      assertEquals(unrelatedParent, rows("SELECT * FROM association_shape_parent WHERE id=2"));
      assertEquals(unrelatedChild, rows("SELECT * FROM association_shape_child ORDER BY id"));
      assertThat(patch.getValues()).containsEntry("code", null);
      assertMetadataUnchanged();
   }



   /*******************************************************************************
    ** An already-null stored tuple has no members, even if unassigned children
    ** contain matching nulls. Missing input must not become a broad deletion.
    *******************************************************************************/
   @ParameterizedTest
   @EnumSource(value = Shape.class, names = { "NON_PRIMARY", "COMPOSITE" })
   void testNullStoredTupleDoesNotDeleteUnassignedChildren(Shape shape) throws Exception
   {
      configure(shape);
      executeSql("UPDATE association_shape_parent SET code=NULL WHERE id=1");
      executeSql("UPDATE association_shape_child SET parent_code=NULL WHERE id IN(10,20)");
      List<Map<String, Object>> before = rows("SELECT * FROM association_shape_child ORDER BY id");
      QRecord patch = new QRecord().withValue("id", 1).withValue("payload", "changed").withAssociatedRecords(GROUP, List.of());
      QRecord output = new UpdateAction().execute(new UpdateInput(PARENT).withInputSource(QInputSource.USER).withRecord(patch)).getRecords().get(0);
      assertNoErrors(output);
      assertEquals("changed", rows("SELECT payload FROM association_shape_parent WHERE id=1").get(0).get("PAYLOAD"));
      assertEquals(before, rows("SELECT * FROM association_shape_child ORDER BY id"));
      assertThat(patch.getValues()).doesNotContainKey("code");
      assertThat(output.getValues()).doesNotContainKey("code");
      assertMetadataUnchanged();
   }



   /*******************************************************************************
    ** Neither an edit nor a new child can be attached to a null prospective
    ** tuple. Reject both before changing the parent or any existing child.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(booleans = { false, true })
   void testNullProspectiveTupleRejectsChildWritesBeforeParentDml(boolean existingChild) throws Exception
   {
      configure(Shape.COMPOSITE);
      List<Map<String, Object>> beforeParents = rows("SELECT * FROM association_shape_parent ORDER BY id");
      List<Map<String, Object>> beforeChildren = rows("SELECT * FROM association_shape_child ORDER BY id");
      QRecord childPatch = new QRecord().withValue("payload", "must not write");
      if(existingChild)
      {
         childPatch.setValue("id", 10);
      }
      QRecord patch = new QRecord().withValue("id", 1).withValue("code", null).withValue("payload", "must not write")
         .withAssociatedRecord(GROUP, childPatch);
      QRecord output = new UpdateAction().execute(new UpdateInput(PARENT).withInputSource(QInputSource.USER).withRecord(patch)).getRecords().get(0);
      assertThat(output.getErrors()).anyMatch(error -> error instanceof BadInputStatusMessage);
      assertEquals(beforeParents, rows("SELECT * FROM association_shape_parent ORDER BY id"));
      assertEquals(beforeChildren, rows("SELECT * FROM association_shape_child ORDER BY id"));
      assertThat(childPatch.getValues()).doesNotContainKeys("parentCode", "region");
      assertMetadataUnchanged();
   }



   /*******************************************************************************
    ** Stored lookup and child replacement use the caller's uncommitted tuple;
    ** all mutations stay on its connection and can be rolled back together.
    *******************************************************************************/
   @Test
   void testSparseReplacementUsesCallerTransactionAndRollsBack() throws Exception
   {
      configure(Shape.COMPOSITE);
      List<Map<String, Object>> beforeParents = rows("SELECT * FROM association_shape_parent ORDER BY id");
      List<Map<String, Object>> beforeChildren = rows("SELECT * FROM association_shape_child ORDER BY id");
      try(RDBMSTransaction transaction = new RDBMSTransaction(ConnectionManager.getConnection(TestUtils.defineBackend())))
      {
         try(Statement statement = transaction.getConnection().createStatement())
         {
            statement.executeUpdate("UPDATE association_shape_parent SET code='TX',region=9 WHERE id=1");
            statement.executeUpdate("UPDATE association_shape_child SET parent_code='TX',region=9 WHERE id IN(10,20)");
         }
         QRecord patch = new QRecord().withValue("id", 1).withValue("payload", "transaction updated")
            .withAssociatedRecord(GROUP, new QRecord().withValue("id", 10).withValue("payload", "changed"))
            .withAssociatedRecord(GROUP, new QRecord().withValue("payload", "transaction child"));
         QRecord output = new UpdateAction().execute(new UpdateInput(PARENT).withInputSource(QInputSource.USER)
            .withTransaction(transaction).withRecord(patch)).getRecords().get(0);
         assertNoErrors(output);
         assertFalse(transaction.getConnection().isClosed());
         assertEquals("transaction updated", rows(transaction.getConnection(), "SELECT payload FROM association_shape_parent WHERE id=1").get(0).get("PAYLOAD"));
         assertThat(rows(transaction.getConnection(), "SELECT * FROM association_shape_child WHERE id=20")).isEmpty();
         assertStoredRelationship(Shape.COMPOSITE, rows(transaction.getConnection(), "SELECT * FROM association_shape_child WHERE id=10").get(0), 1, "TX", 9);
         List<Map<String, Object>> inserted = rows(transaction.getConnection(), "SELECT * FROM association_shape_child WHERE payload='transaction child'");
         assertEquals(1, inserted.size());
         assertStoredRelationship(Shape.COMPOSITE, inserted.get(0), 1, "TX", 9);
         assertEquals(beforeChildren.get(2), rows(transaction.getConnection(), "SELECT * FROM association_shape_child WHERE id=30").get(0));
         assertEquals(beforeParents, rows("SELECT * FROM association_shape_parent ORDER BY id"));
         assertEquals(beforeChildren, rows("SELECT * FROM association_shape_child ORDER BY id"));
         transaction.rollback();
      }
      assertEquals(beforeParents, rows("SELECT * FROM association_shape_parent ORDER BY id"));
      assertEquals(beforeChildren, rows("SELECT * FROM association_shape_child ORDER BY id"));
      assertMetadataUnchanged();
   }



   /*******************************************************************************
    ** A native projection failure must precede parent DML. Personalized prefetch
    ** omits this physical field, so only private stored-value lookup detects it.
    *******************************************************************************/
   @Test
   void testInvalidStoredProjectionFailsBeforeParentDml() throws Exception
   {
      parent.getField("code").withIsHidden(true).withIsHeavy(true).withBackendName("missing_native_column");
      configure(Shape.NON_PRIMARY);
      QContext.getQInstance().addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(RemoveParentCode.class));
      assertEquals(2, new QueryAction().execute(new QueryInput(PARENT)).getRecords().size());
      List<Map<String, Object>> beforeParents = rows("SELECT * FROM association_shape_parent ORDER BY id");
      List<Map<String, Object>> beforeChildren = rows("SELECT * FROM association_shape_child ORDER BY id");
      QRecord patch = new QRecord().withValue("id", 1).withValue("payload", "must not write").withAssociatedRecords(GROUP, List.of());
      assertThrows(QException.class, () -> new UpdateAction().execute(new UpdateInput(PARENT).withInputSource(QInputSource.USER).withRecord(patch)));
      assertEquals(beforeParents, rows("SELECT * FROM association_shape_parent ORDER BY id"));
      assertEquals(beforeChildren, rows("SELECT * FROM association_shape_child ORDER BY id"));
      assertThat(patch.getValues()).doesNotContainKey("code");
      assertMetadataUnchanged();
   }



   /*******************************************************************************
    ** Native INTEGER/BIGINT equality must survive attachment into Java tuples.
    *******************************************************************************/
   @ParameterizedTest
   @EnumSource(value = Shape.class, names = { "PRIMARY", "COMPOSITE" })
   void testDifferentNumericTypesAttachNativeMatches(Shape shape) throws Exception
   {
      String fieldName = shape == Shape.PRIMARY ? "parentId" : "region";
      String columnName = shape == Shape.PRIMARY ? "parent_id" : "region";
      child.getField(fieldName).setType(QFieldType.LONG);
      executeSql("ALTER TABLE association_shape_child ALTER COLUMN " + columnName + " BIGINT");
      configure(shape);
      List<Map<String, Object>> before = rows("SELECT * FROM association_shape_child ORDER BY id");
      String nativeRelation = shape == Shape.PRIMARY ? "c.parent_id=p.id" : "c.parent_code=p.code AND c.region=p.region";
      assertEquals(2, rows("SELECT c.id FROM association_shape_parent p JOIN association_shape_child c ON " + nativeRelation + " WHERE p.id=1").size());
      assertMembers(query(1), 10, 20);
      assertMembers(get(1), 10, 20);
      assertEquals(before, rows("SELECT * FROM association_shape_child ORDER BY id"));
      assertMetadataUnchanged();
   }



   /*******************************************************************************
    ** A null component does not join to other nulls under native SQL equality;
    ** the successful empty group must not hide the valid second parent's group.
    *******************************************************************************/
   @ParameterizedTest
   @EnumSource(value = Shape.class, names = { "NON_PRIMARY", "COMPOSITE" })
   void testNullTupleHasNoNativeMembership(Shape shape) throws Exception
   {
      configure(shape);
      executeSql("UPDATE association_shape_parent SET code=NULL WHERE id=1");
      executeSql("UPDATE association_shape_child SET parent_code=NULL WHERE id IN(10,20)");
      List<Map<String, Object>> before = rows("SELECT * FROM association_shape_child ORDER BY id");
      String nativeRelation = "c.parent_code=p.code" + (shape == Shape.COMPOSITE ? " AND c.region=p.region" : "");
      assertThat(rows("SELECT c.id FROM association_shape_parent p JOIN association_shape_child c ON " + nativeRelation + " WHERE p.id=1")).isEmpty();
      assertMembers(query(1));
      assertMembers(get(1));
      List<QRecord> parents = new QueryAction().execute(new QueryInput(PARENT).withInputSource(QInputSource.USER).withIncludeAssociations(true)).getRecords();
      assertEquals(2, parents.size());
      assertMembers(parents.stream().filter(record -> record.getValueInteger("id").equals(1)).findFirst().orElseThrow());
      assertMembers(parents.stream().filter(record -> record.getValueInteger("id").equals(2)).findFirst().orElseThrow(), 30);
      assertEquals(before, rows("SELECT * FROM association_shape_child ORDER BY id"));
      assertMetadataUnchanged();
   }



   /*******************************************************************************
    ** Zero is a supplied relationship component, including a parent primary key.
    *******************************************************************************/
   @ParameterizedTest
   @EnumSource(value = Shape.class, names = { "PRIMARY", "COMPOSITE" })
   void testZeroTupleRemainsRelated(Shape shape) throws Exception
   {
      configure(shape);
      executeSql("INSERT INTO association_shape_parent(id,parent_id,code,region,payload) VALUES (0,777,'ZERO',0,'zero parent')");
      executeSql("INSERT INTO association_shape_child(id,parent_id,parent_code,region,payload) VALUES (40,0,'ZERO',0,'zero child'),(41,2,'ZERO',1,'different tuple')");
      List<Map<String, Object>> before = rows("SELECT * FROM association_shape_child ORDER BY id");
      assertMembers(query(0), 40);
      assertMembers(get(0), 40);
      assertEquals(before, rows("SELECT * FROM association_shape_child ORDER BY id"));
      assertMetadataUnchanged();
   }



   /*******************************************************************************
    ** Each accepted shape uses registered metadata, never a mutated/reversed copy
    ** supplied directly to the action under test.
    *******************************************************************************/
   private void configure(Shape shape) throws Exception
   {
      join = new QJoinMetaData().withName(JOIN).withLeftTable(PARENT).withRightTable(CHILD).withType(JoinType.ONE_TO_MANY);
      switch(shape)
      {
         case PRIMARY -> join.withJoinOn(new JoinOn("id", "parentId"));
         case REVERSE -> join.withLeftTable(CHILD).withRightTable(PARENT).withType(JoinType.MANY_TO_ONE).withJoinOn(new JoinOn("parentId", "id"));
         case NON_PRIMARY -> join.withJoinOn(new JoinOn("code", "parentCode"));
         case COMPOSITE -> join.withJoinOn(new JoinOn("code", "parentCode")).withJoinOn(new JoinOn("region", "region"));
         default -> throw new IllegalStateException("Unexpected association shape: " + shape);
      }
      QContext.getQInstance().addJoin(join);
      new QInstanceValidator().revalidate(QContext.getQInstance());
      registeredJoin = JsonUtils.toJson(join);
      registeredAssociations = JsonUtils.toJson(parent.getAssociations());
   }



   /*******************************************************************************
    ** Keep both directions of the same composite relationship as real metadata.
    *******************************************************************************/
   private void configureComposite(boolean reverse) throws Exception
   {
      configure(Shape.COMPOSITE);
      if(reverse)
      {
         join = join.flip();
         QContext.getQInstance().getJoins().put(JOIN, join);
         new QInstanceValidator().revalidate(QContext.getQInstance());
         registeredJoin = JsonUtils.toJson(join);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QTableMetaData table(String name, String physicalName)
   {
      return new QTableMetaData().withName(name).withBackendName(TestUtils.DEFAULT_BACKEND_NAME)
         .withBackendDetails(new RDBMSTableBackendDetails().withTableName(physicalName)).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("parentId", QFieldType.INTEGER).withBackendName("parent_id"))
         .withField(new QFieldMetaData("code", QFieldType.STRING))
         .withField(new QFieldMetaData("parentCode", QFieldType.STRING).withBackendName("parent_code"))
         .withField(new QFieldMetaData("region", QFieldType.INTEGER))
         .withField(new QFieldMetaData("payload", QFieldType.STRING));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord query(Integer id) throws Exception
   {
      List<QRecord> records = new QueryAction().execute(new QueryInput(PARENT).withInputSource(QInputSource.USER).withIncludeAssociations(true)
         .withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, id)))).getRecords();
      assertEquals(1, records.size());
      return records.get(0);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord get(Integer id) throws Exception
   {
      return new GetAction().execute(new GetInput(PARENT).withPrimaryKey(id).withInputSource(QInputSource.USER).withIncludeAssociations(true)).getRecord();
   }



   /*******************************************************************************
    ** The group must exist under its exact name even when its selected result is empty.
    *******************************************************************************/
   private void assertMembers(QRecord record, Integer... expectedIds)
   {
      assertNotNull(record);
      assertNoErrors(record);
      assertThat(record.getAssociatedRecords()).containsOnlyKeys(GROUP);
      assertThat(record.getAssociatedRecords().get(GROUP)).extracting(childRecord -> childRecord.getValueInteger("id")).containsExactlyInAnyOrder(expectedIds);
   }



   /*******************************************************************************
    ** Nested failures must not pass merely because the parent has no errors.
    *******************************************************************************/
   private void assertNoErrors(QRecord record)
   {
      assertThat(record.getErrors()).isEmpty();
      if(record.getAssociatedRecords() != null)
      {
         record.getAssociatedRecords().values().forEach(records -> records.forEach(childRecord -> assertThat(childRecord.getErrors()).isEmpty()));
      }
   }



   /*******************************************************************************
    ** Compare independently selected physical columns to the intended relationship.
    *******************************************************************************/
   private void assertStoredRelationship(Shape shape, Map<String, Object> row, Integer parentId, String code, Integer region)
   {
      if(shape == Shape.PRIMARY || shape == Shape.REVERSE)
      {
         assertEquals(parentId, row.get("PARENT_ID"));
      }
      else
      {
         assertEquals(code, row.get("PARENT_CODE"));
         if(shape == Shape.COMPOSITE)
         {
            assertEquals(region, row.get("REGION"));
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertMetadataUnchanged() throws Exception
   {
      assertSame(join, QContext.getQInstance().getJoin(JOIN));
      assertEquals(registeredJoin, JsonUtils.toJson(join));
      assertEquals(registeredAssociations, JsonUtils.toJson(parent.getAssociations()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void executeSql(String sql) throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineBackend());
          Statement statement = connection.createStatement())
      {
         statement.execute(sql);
      }
   }



   /*******************************************************************************
    ** Keep nulls in snapshots so accidental unlinking cannot be hidden by JSON omission.
    *******************************************************************************/
   private List<Map<String, Object>> rows(String sql) throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineBackend()))
      {
         return rows(connection, sql);
      }
   }



   /*******************************************************************************
    ** The caller retains ownership of its transaction connection.
    *******************************************************************************/
   private List<Map<String, Object>> rows(Connection connection, String sql) throws Exception
   {
      List<Map<String, Object>> rows = new ArrayList<>();
      try(Statement statement = connection.createStatement();
          ResultSet resultSet = statement.executeQuery(sql))
      {
         while(resultSet.next())
         {
            Map<String, Object> row = new LinkedHashMap<>();
            for(int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++)
            {
               row.put(resultSet.getMetaData().getColumnLabel(i).toUpperCase(Locale.ROOT), resultSet.getObject(i));
            }
            rows.add(row);
         }
      }
      return rows;
   }



   /*******************************************************************************
    ** Display metadata omits the physical field for ordinary USER and SYSTEM reads;
    ** the constrained stored lookup alone restores its canonical declaration.
    *******************************************************************************/
   public static class RemoveParentCode implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         if(PARENT.equals(input.getTableName()))
         {
            QTableMetaData table = input.getTable().clone();
            table.getFields().remove("code");
            return table;
         }
         return input.getTable();
      }
   }
}
