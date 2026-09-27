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

package com.kingsrook.sampleapp;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPreInsertCustomizer;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.tables.ReplaceAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.replace.ReplaceInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.replace.ReplaceOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DynamicDefaultValueBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.QWarningMessage;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Canonical native Replace callbacks must retain real origin/key identity.
 ** FieldLab covers generated-key mapping; Person/Pet/PetNote prove a nested
 ** returned validation failure cannot be cleared to commit a partial tree.
 *******************************************************************************/
class SampleReplaceCallbackContractTest
{
   private static final String TABLE = "fieldLab";
   private QInstance instance;
   private QTableMetaData canonical;
   private String canonicalJson;
   private QTableMetaData active;
   private String activeJson;



   /*******************************************************************************
    ** Change only owned cloned defaults; callback behavior is selected per test.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      QContext.init(instance, new QSession());
      canonical = instance.getTable(TABLE);
      canonicalJson = JsonUtils.toJson(canonical);
      active = canonical.clone();
      for(String field : List.of("createDate", "modifyDate", "createdDay", "modifiedDay", "userIdValue"))
      {
         active.getField(field).withBehaviors(new HashSet<>(active.getField(field).getBehaviors()))
            .withBehavior(DynamicDefaultValueBehavior.NONE);
      }
      active.withCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(Callbacks.class))
         .withCustomizer(TableCustomizers.POST_INSERT_RECORD, new QCodeReference(Callbacks.class))
         .withCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(CountDeletes.class));
      Map<String, QTableMetaData> tables = new LinkedHashMap<>(instance.getTables());
      tables.put(TABLE, active);
      instance.setTables(tables);
      new QInstanceValidator().revalidate(instance);
      activeJson = JsonUtils.toJson(active);
      assertMetaDataUnchanged();
      seed();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      resetCallbacks();
      QContext.clear();
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    ** Initially identical nullable-key drafts become distinct valid native rows
    ** in PRE. Reordering cloned drafts must not swap their published identities.
    *******************************************************************************/
   @Test
   void testClonedReorderedNullableDraftsPublishTheirOwnNativeKeys() throws Exception
   {
      Callbacks.preMode = "cloned-reorder";
      QRecord first = draft("Draft", "Identical payload").withValue("normalizedKey", null);
      QRecord second = draft("Draft", "Identical payload").withValue("normalizedKey", null);
      assertEquals(first.getValues(), second.getValues());
      ReplaceInput input = input(first, second).withKey(new UniqueKey("normalizedKey"));
      assertFalse(input.getAllowNullKeyValuesToEqual());
      ReplaceOutput output = new ReplaceAction().execute(input);
      assertSuccess(output, 2);
      Map<String, Integer> ids = assertCreatedRows(Map.of("From first", "First payload", "From second", "Second payload"));
      assertAll(
         () -> assertEquals(2, Callbacks.preCount),
         () -> assertEquals(List.of("From second", "From first"), output.getInsertOutput().getRecords().stream().map(r -> r.getValueString("name")).toList()),
         () -> assertEquals(ids.get("From first"), first.getValueInteger("id")),
         () -> assertEquals(ids.get("From second"), second.getValueInteger("id")),
         () -> assertFalse(first.getValue("id").equals(second.getValue("id"))),
         this::assertMetaDataUnchanged);
   }



   /*******************************************************************************
    ** Fresh PRE records have no origin in the original input. Publication true
    ** rejects before native INSERT; false retains ordinary callback flexibility.
    *******************************************************************************/
   @Test
   void testFreshPreReplacementRequiresPublicationToBeDisabled()
   {
      assertAll(
         () ->
         {
            seed();
            Callbacks.preMode = "fresh";
            assertFieldRejected(input(draft("Original", "Original payload")), 1, 0);
         },
         () ->
         {
            seed();
            Callbacks.preMode = "fresh";
            QRecord original = draft("Original", "Original payload");
            ReplaceInput input = input(original).withSetPrimaryKeyInInsertedRecords(false);
            ReplaceOutput output = new ReplaceAction().execute(input);
            assertSuccess(output, 1);
            Map<String, Integer> ids = assertCreatedRows(Map.of("Fresh replacement", "Fresh payload"));
            assertAll(
               () -> assertEquals(ids.get("Fresh replacement"), output.getInsertOutput().getRecords().get(0).getValueInteger("id")),
               () -> assertNull(original.getValue("id")),
               () -> assertEquals("Original", original.getValueString("name")),
               this::assertMetaDataUnchanged);
         });
   }



   /*******************************************************************************
    ** Duplicate/missing origins cannot publish keys to two original drafts. PRE
    ** assigns unique native names so normal name validation cannot hide this test.
    *******************************************************************************/
   @Test
   void testDuplicateAndMissingPreOriginsRejectBeforeNativeInsert()
   {
      assertAll(
         () -> assertMalformedPre("duplicate-origin"),
         () -> assertMalformedPre("missing-origin"));
   }



   /*******************************************************************************
    ** POST receives successful native rows, then forges or drops their identities.
    ** Each case independently proves owned rollback and no omission DELETE.
    *******************************************************************************/
   @Test
   void testInvalidPostKeysCannotControlOmissionDeletionOrCommit()
   {
      assertAll(
         () -> assertMalformedPost("forged-key"),
         () -> assertMalformedPost("missing-key"),
         () -> assertMalformedPost("duplicate-key"),
         () -> assertMalformedPost("missing-result"),
         () -> assertMalformedPost("in-place-key"));
   }



   /*******************************************************************************
    ** Warnings and immutable cloned/reordered results are valid POST behavior.
    ** Exact native mapping still drives both publication and omission selection.
    *******************************************************************************/
   @Test
   void testPostClonesReorderAndWarningsPreserveNativeIdentities() throws Exception
   {
      Callbacks.postMode = "reorder-warnings";
      QRecord first = draft("New first", "First payload");
      QRecord second = draft("New second", "Second payload");
      ReplaceOutput output = new ReplaceAction().execute(input(first, second));
      assertSuccess(output, 2);
      Map<String, Integer> ids = assertCreatedRows(Map.of("New first", "First payload", "New second", "Second payload"));
      assertAll(
         () -> assertEquals(List.of("New second", "New first"), output.getInsertOutput().getRecords().stream().map(r -> r.getValueString("name")).toList()),
         () -> assertEquals(ids.get("New first"), first.getValueInteger("id")),
         () -> assertEquals(ids.get("New second"), second.getValueInteger("id")),
         () -> assertEquals(1, CountDeletes.count),
         () ->
         {
            for(QRecord record : output.getInsertOutput().getRecords())
            {
               assertEquals(List.of("Reviewed " + record.getValueString("name")), record.getWarnings().stream().map(QWarningMessage::getMessage).toList());
            }
         },
         this::assertMetaDataUnchanged);
   }



   /*******************************************************************************
    ** This is a native-backed returned BadInput failure, not a SQL exception:
    ** Person and Pet insert successfully, but the Note lacks its required text.
    ** A Person POST callback attempts to erase that error-bearing nested tree.
    *******************************************************************************/
   @Test
   void testNestedNoteFailureCannotBeClearedByPostToCommitParentAndPet() throws Exception
   {
      instance.getTable("person").withCustomizer(TableCustomizers.POST_INSERT_RECORD, new QCodeReference(ClearNestedFailure.class))
         .withCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(CountDeletes.class));
      instance.getTable("petNote").withCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(ObserveNoteFailure.class));
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         assertEquals(1, statement.executeUpdate("INSERT INTO person(id,first_name,last_name,email) VALUES(99,'Omitted','Replace callback','replace-omitted@example.invalid')"));
      }
      Map<String, List<Map<String, String>>> before = nativeTree();
      QRecord note = new QRecord();
      QRecord pet = new QRecord().withValue("name", "Replace child").withValue("speciesId", 1)
         .withAssociatedRecords("notes", List.of(note));
      QRecord person = new QRecord().withValue("firstName", "Created").withValue("lastName", "Replace callback")
         .withValue("email", "replace-created@example.invalid").withAssociatedRecords("pets", List.of(pet));
      ReplaceInput input = new ReplaceInput().withRecords(List.of(person)).withKey(new UniqueKey("email"))
         .withFilter(new QQueryFilter(new QFilterCriteria("lastName", QCriteriaOperator.EQUALS, "Replace callback")));
      input.setTableName("person");
      input.setInputSource(QInputSource.USER);
      Attempt attempt = attempt(input);
      assertAll(
         () -> assertRejected(attempt),
         () -> assertEquals(1, ObserveNoteFailure.count),
         () -> assertTrue(ObserveNoteFailure.missingRequiredNote),
         () -> assertEquals(0, ClearNestedFailure.count),
         () -> assertEquals(0, CountDeletes.count),
         () -> assertEquals(before, nativeTree()));
   }



   /*******************************************************************************
    ** The failing leaf's own POST must not erase its required error before any
    ** ancestor observes it. Every ancestor and the omission target must survive.
    *******************************************************************************/
   @Test
   void testLeafInsertPostCannotHideItsOwnFailureFromReplace() throws Exception
   {
      instance.getTable("petNote")
         .withCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(ClearLeafFailure.class))
         .withCustomizer(TableCustomizers.POST_INSERT_RECORD, new QCodeReference(ClearLeafFailure.class));
      instance.getTable("person").withCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(CountDeletes.class));
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         assertEquals(1, statement.executeUpdate("INSERT INTO person(id,first_name,last_name,email) VALUES(99,'Omitted','Replace leaf insert','replace-leaf-omitted@example.invalid')"));
      }
      Map<String, List<Map<String, String>>> before = nativeTree();
      QRecord pet = new QRecord().withValue("name", "Leaf insert parent").withValue("speciesId", 1)
         .withAssociatedRecords("notes", List.of(new QRecord()));
      QRecord person = new QRecord().withValue("firstName", "Created").withValue("lastName", "Replace leaf insert")
         .withValue("email", "replace-leaf-created@example.invalid").withAssociatedRecords("pets", List.of(pet));
      ReplaceInput input = new ReplaceInput().withRecords(List.of(person)).withKey(new UniqueKey("email"))
         .withFilter(new QQueryFilter(new QFilterCriteria("lastName", QCriteriaOperator.EQUALS, "Replace leaf insert")));
      input.setTableName("person");
      input.setInputSource(QInputSource.USER);
      Attempt attempt = attempt(input);
      assertAll(
         () -> assertRejected(attempt),
         () -> assertEquals(1, ClearLeafFailure.preCount),
         () -> assertTrue(ClearLeafFailure.sawRequiredNote),
         () -> assertEquals(0, ClearLeafFailure.postCount),
         () -> assertEquals(0, CountDeletes.count),
         () -> assertEquals(before, nativeTree()));
   }



   /*******************************************************************************
    ** A retained Note's explicit null fails actual UPDATE validation. Its local
    ** POST must not clear the error and commit successful Person/Pet updates.
    *******************************************************************************/
   @Test
   void testLeafUpdatePostCannotHideItsOwnFailureFromReplace() throws Exception
   {
      instance.getTable("petNote")
         .withCustomizer(TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(ClearLeafFailure.class))
         .withCustomizer(TableCustomizers.POST_UPDATE_RECORD, new QCodeReference(ClearLeafFailure.class));
      instance.getTable("person").withCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(CountDeletes.class));
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         assertEquals(2, statement.executeUpdate("INSERT INTO person(id,first_name,last_name,email) VALUES "
            + "(98,'Omitted','Replace leaf update','replace-leaf-update-omitted@example.invalid'),"
            + "(99,'Original person','Replace leaf update','replace-leaf-update-target@example.invalid')"));
         assertEquals(1, statement.executeUpdate("INSERT INTO pet(id,name,person_id,species_id) VALUES(99,'Original pet',99,1)"));
         assertEquals(1, statement.executeUpdate("INSERT INTO pet_note(id,pet_id,note) VALUES(99,99,'Original note')"));
      }
      Map<String, List<Map<String, String>>> before = nativeTree();
      QRecord note = new QRecord().withValue("id", 99).withValue("note", null);
      QRecord pet = new QRecord().withValue("id", 99).withValue("name", "Changed pet")
         .withAssociatedRecords("notes", List.of(note));
      QRecord person = new QRecord().withValue("firstName", "Changed person").withValue("email", "replace-leaf-update-target@example.invalid")
         .withAssociatedRecords("pets", List.of(pet));
      ReplaceInput input = new ReplaceInput().withRecords(List.of(person)).withKey(new UniqueKey("email"))
         .withFilter(new QQueryFilter(new QFilterCriteria("lastName", QCriteriaOperator.EQUALS, "Replace leaf update")));
      input.setTableName("person");
      input.setInputSource(QInputSource.USER);
      Attempt attempt = attempt(input);
      assertAll(
         () -> assertRejected(attempt),
         () -> assertEquals(1, ClearLeafFailure.preCount),
         () -> assertTrue(ClearLeafFailure.sawRequiredNote),
         () -> assertEquals(0, ClearLeafFailure.postCount),
         () -> assertEquals(0, CountDeletes.count),
         () -> assertEquals(before, nativeTree()));
   }



   /*******************************************************************************
    ** The set operator applies before omission ordering/windowing. UNION_ALL's
    ** duplicate selected keys are deduplicated by ordinary Delete after the window.
    *******************************************************************************/
   @Test
   void testSetOperationOmissionsPreserveOperatorAndWindow()
   {
      assertAll(
         () -> assertSetOperation(QQueryFilter.SubFilterSetOperator.EXCEPT, null, null, List.of(1)),
         () -> assertSetOperation(QQueryFilter.SubFilterSetOperator.UNION, null, 1, List.of(4)),
         () -> assertSetOperation(QQueryFilter.SubFilterSetOperator.INTERSECT, null, 1, List.of(3)),
         () -> assertSetOperation(QQueryFilter.SubFilterSetOperator.UNION_ALL, 1, 2, List.of(3)));
   }



   /*******************************************************************************
    ** A={1,2,3}, B={2,3,4}; row 2 is retained. The root orders descending by
    ** the selected primary key, which is supported by native set-query sorting.
    *******************************************************************************/
   private void assertSetOperation(QQueryFilter.SubFilterSetOperator operator, Integer skip, Integer limit, List<Integer> deleted) throws Exception
   {
      resetCallbacks();
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         statement.executeUpdate("DELETE FROM field_lab");
         assertEquals(4, statement.executeUpdate("INSERT INTO field_lab(id,name,long_value,html_value) VALUES "
            + "(1,'Only A',10,'Alpha'),(2,'Overlap keep',20,'Keep'),"
            + "(3,'Overlap omitted',30,'Both'),(4,'Only B',40,'Beta')"));
      }
      List<Map<String, String>> before = rows("field_lab");
      QQueryFilter filter = new QQueryFilter().withSubFilterSetOperator(operator)
         .withSubFilters(List.of(
            new QQueryFilter(new QFilterCriteria("longValue", QCriteriaOperator.LESS_THAN_OR_EQUALS, 30L)),
            new QQueryFilter(new QFilterCriteria("longValue", QCriteriaOperator.GREATER_THAN_OR_EQUALS, 20L))))
         .withOrderBy(new QFilterOrderBy("id", false)).withSkip(skip).withLimit(limit);
      String filterJson = JsonUtils.toJson(filter);
      QRecord retained = new QRecord().withValue("name", "Overlap keep");
      ReplaceOutput output = new ReplaceAction().execute(input(retained).withFilter(filter));
      List<Map<String, String>> expected = before.stream().filter(row -> !deleted.contains(Integer.parseInt(row.get("ID")))).toList();
      assertAll(
         () -> assertEquals(expected, rows("field_lab")),
         () -> assertEquals(0, output.getInsertOutput().getRecords().size()),
         () -> assertEquals(1, output.getUpdateOutput().getRecords().size()),
         () -> assertEquals(deleted.size(), output.getDeleteOutput().getDeletedRecordCount()),
         () -> assertEquals(deleted.size(), CountDeletes.count),
         () -> assertEquals(2, retained.getValueInteger("id")),
         () -> assertFalse(hasErrors(output), JsonUtils.toJson(output)),
         () -> assertEquals(filterJson, JsonUtils.toJson(filter)),
         this::assertMetaDataUnchanged);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertMalformedPre(String mode) throws Exception
   {
      seed();
      Callbacks.preMode = mode;
      assertFieldRejected(input(draft("New first", "First payload"), draft("New second", "Second payload")), 2, 0);
   }



   /*******************************************************************************
    ** Native generated keys are observed before the POST corruption. This proves
    ** the test reached actual INSERT rather than an unrelated earlier failure.
    *******************************************************************************/
   private void assertMalformedPost(String mode) throws Exception
   {
      seed();
      Callbacks.postMode = mode;
      assertFieldRejected(input(draft("New first", "First payload"), draft("New second", "Second payload")), 2, 1);
      assertEquals(2, new HashSet<>(Callbacks.nativeIds).size());
      assertTrue(Callbacks.nativeIds.stream().allMatch(id -> id != null && id > 2));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertFieldRejected(ReplaceInput input, int preCount, int postCount) throws Exception
   {
      List<Map<String, String>> before = rows("field_lab");
      Attempt attempt = attempt(input);
      assertAll(
         () -> assertRejected(attempt),
         () -> assertEquals(preCount, Callbacks.preCount),
         () -> assertEquals(postCount, Callbacks.postCount),
         () -> assertEquals(0, CountDeletes.count),
         () -> assertEquals(before, rows("field_lab")),
         this::assertMetaDataUnchanged);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Attempt attempt(ReplaceInput input)
   {
      try
      {
         return new Attempt(new ReplaceAction().execute(input), null);
      }
      catch(QException e)
      {
         return new Attempt(null, e);
      }
   }



   /*******************************************************************************
    ** Exact error transport is independent of the required native safety outcome.
    *******************************************************************************/
   private void assertRejected(Attempt attempt)
   {
      assertTrue(attempt.exception() != null || hasErrors(attempt.output()), "Replace accepted an invalid callback result");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private boolean hasErrors(ReplaceOutput output)
   {
      return output != null && (output.getInsertOutput() != null && output.getInsertOutput().getRecords().stream().anyMatch(r -> !r.getErrors().isEmpty())
         || output.getUpdateOutput() != null && output.getUpdateOutput().getRecords().stream().anyMatch(r -> !r.getErrors().isEmpty())
         || output.getDeleteOutput() != null && CollectionUtils.nullSafeHasContents(output.getDeleteOutput().getRecordsWithErrors()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertSuccess(ReplaceOutput output, int inserted)
   {
      assertNotNull(output);
      assertAll(
         () -> assertEquals(inserted, output.getInsertOutput().getRecords().size()),
         () -> assertEquals(0, output.getUpdateOutput().getRecords().size()),
         () -> assertEquals(1, output.getDeleteOutput().getDeletedRecordCount()),
         () -> assertFalse(hasErrors(output), JsonUtils.toJson(output)));
   }



   /*******************************************************************************
    ** Expected fields come from declared callback proposals. Only generated ids
    ** come from independent native name lookup, and every native column is checked.
    *******************************************************************************/
   private Map<String, Integer> assertCreatedRows(Map<String, String> payloads) throws Exception
   {
      List<Map<String, String>> actual = rows("field_lab");
      assertEquals(payloads.size() + 1, actual.size());
      Map<String, Integer> ids = new LinkedHashMap<>();
      List<Map<String, String>> expected = new ArrayList<>();
      Map<String, String> outside = new LinkedHashMap<>();
      actual.get(0).keySet().forEach(key -> outside.put(key, null));
      outside.put("ID", "2");
      outside.put("NAME", "Outside");
      outside.put("LONG_VALUE", "20");
      outside.put("HTML_VALUE", "Keep outside");
      expected.add(outside);
      for(Map.Entry<String, String> entry : payloads.entrySet())
      {
         Map<String, String> row = actual.stream().filter(value -> entry.getKey().equals(value.get("NAME"))).findFirst().orElseThrow();
         int id = Integer.parseInt(row.get("ID"));
         assertTrue(id > 2);
         ids.put(entry.getKey(), id);
         Map<String, String> wanted = new LinkedHashMap<>();
         row.keySet().forEach(key -> wanted.put(key, null));
         wanted.put("ID", Integer.toString(id));
         wanted.put("NAME", entry.getKey());
         wanted.put("HTML_VALUE", entry.getValue());
         wanted.put("LONG_VALUE", "10");
         wanted.put("BOOLEAN_VALUE", "TRUE");
         expected.add(wanted);
      }
      expected.sort((left, right) -> Integer.compare(Integer.parseInt(left.get("ID")), Integer.parseInt(right.get("ID"))));
      assertEquals(expected, actual);
      return ids;
   }



   /*******************************************************************************
    ** The filter continues to limit omission deletion only.
    *******************************************************************************/
   private ReplaceInput input(QRecord... records)
   {
      ReplaceInput input = new ReplaceInput().withKey(new UniqueKey("name")).withRecords(List.of(records))
         .withSetPrimaryKeyInInsertedRecords(true)
         .withFilter(new QQueryFilter(new QFilterCriteria("longValue", QCriteriaOperator.EQUALS, 10L)));
      input.setTableName(TABLE);
      return input;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QRecord draft(String name, String html)
   {
      return new QRecord().withValue("name", name).withValue("htmlValue", html).withValue("longValue", 10L);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void seed() throws Exception
   {
      resetCallbacks();
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         statement.executeUpdate("DELETE FROM field_lab");
         assertEquals(2, statement.executeUpdate("INSERT INTO field_lab(id,name,long_value,html_value) VALUES "
            + "(1,'Omitted',10,'Remove only on success'),(2,'Outside',20,'Keep outside')"));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void resetCallbacks()
   {
      Callbacks.preMode = "none";
      Callbacks.postMode = "none";
      Callbacks.preCount = 0;
      Callbacks.postCount = 0;
      Callbacks.nativeIds = List.of();
      CountDeletes.count = 0;
      ObserveNoteFailure.count = 0;
      ObserveNoteFailure.missingRequiredNote = false;
      ClearNestedFailure.count = 0;
      ClearLeafFailure.preCount = 0;
      ClearLeafFailure.postCount = 0;
      ClearLeafFailure.sawRequiredNote = false;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Map<String, List<Map<String, String>>> nativeTree() throws Exception
   {
      Map<String, List<Map<String, String>>> result = new LinkedHashMap<>();
      for(String table : List.of("person", "pet", "pet_note"))
      {
         result.put(table, rows(table));
      }
      return result;
   }



   /*******************************************************************************
    ** Only owned literal table names are used; all physical values are compared.
    *******************************************************************************/
   private List<Map<String, String>> rows(String table) throws Exception
   {
      List<Map<String, String>> result = new ArrayList<>();
      try(Connection connection = connection(); Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT * FROM " + table + " ORDER BY id"))
      {
         while(rows.next())
         {
            Map<String, String> row = new LinkedHashMap<>();
            for(int index = 1; index <= rows.getMetaData().getColumnCount(); index++)
            {
               String name = rows.getMetaData().getColumnLabel(index);
               row.put(name, "BLOB_VALUE".equals(name) && rows.getBytes(index) != null
                  ? Base64.getEncoder().encodeToString(rows.getBytes(index)) : rows.getString(index));
            }
            result.add(row);
         }
      }
      return result;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Connection connection() throws Exception
   {
      return ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertMetaDataUnchanged()
   {
      assertEquals(activeJson, JsonUtils.toJson(active));
      assertEquals(canonicalJson, JsonUtils.toJson(canonical));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private record Attempt(ReplaceOutput output, QException exception)
   {
   }



   /*******************************************************************************
    ** Finite modes exercise the public callback API without private token access.
    *******************************************************************************/
   public static class Callbacks implements TableCustomizerInterface
   {
      private static String preMode;
      private static String postMode;
      private static int preCount;
      private static int postCount;
      private static List<Integer> nativeIds = List.of();



      /*******************************************************************************
       ** Identical drafts must get distinct valid names before uniqueness checks.
       *******************************************************************************/
      @Override
      public AbstractPreInsertCustomizer.WhenToRun whenToRunPreInsert(InsertInput input, boolean isPreview)
      {
         return AbstractPreInsertCustomizer.WhenToRun.BEFORE_ALL_VALIDATIONS;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> preInsert(InsertInput input, List<QRecord> records, boolean isPreview)
      {
         preCount += records.size();
         if("cloned-reorder".equals(preMode))
         {
            QRecord first = new QRecord(records.get(0)).withValue("name", "From first").withValue("htmlValue", "First payload");
            QRecord second = new QRecord(records.get(1)).withValue("name", "From second").withValue("htmlValue", "Second payload");
            return List.of(second, first);
         }
         if("fresh".equals(preMode))
         {
            return List.of(draft("Fresh replacement", "Fresh payload"));
         }
         if("duplicate-origin".equals(preMode))
         {
            return List.of(new QRecord(records.get(0)).withValue("name", "Duplicate first"), new QRecord(records.get(0)).withValue("name", "Duplicate second"));
         }
         if("missing-origin".equals(preMode))
         {
            return List.of(new QRecord(records.get(0)));
         }
         return records;
      }



      /*******************************************************************************
       ** All invalid identities are introduced after successful native INSERT.
       *******************************************************************************/
      @Override
      public List<QRecord> postInsert(InsertInput input, List<QRecord> records)
      {
         postCount++;
         nativeIds = records.stream().map(record -> record.getValueInteger("id")).toList();
         if("forged-key".equals(postMode))
         {
            return List.of(new QRecord().withValue("id", 2).withValue("name", "Forged outside"), new QRecord(records.get(1)));
         }
         if("missing-key".equals(postMode))
         {
            return List.of(new QRecord().withValue("name", "Missing identity"), new QRecord(records.get(1)));
         }
         if("duplicate-key".equals(postMode))
         {
            return List.of(new QRecord(records.get(0)), new QRecord(records.get(0)));
         }
         if("missing-result".equals(postMode))
         {
            return List.of(new QRecord(records.get(0)));
         }
         if("in-place-key".equals(postMode))
         {
            records.get(0).setValue("id", 2);
            return records;
         }
         if("reorder-warnings".equals(postMode))
         {
            QRecord first = new QRecord(records.get(0));
            QRecord second = new QRecord(records.get(1));
            first.addWarning(new QWarningMessage("Reviewed " + first.getValueString("name")));
            second.addWarning(new QWarningMessage("Reviewed " + second.getValueString("name")));
            return List.of(second, first);
         }
         return records;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class CountDeletes implements TableCustomizerInterface
   {
      private static int count;



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> preDelete(DeleteInput input, List<QRecord> records, boolean isPreview)
      {
         count += records.size();
         return records;
      }
   }



   /*******************************************************************************
    ** Default AFTER_ALL_VALIDATIONS placement observes the real required error.
    *******************************************************************************/
   public static class ObserveNoteFailure implements TableCustomizerInterface
   {
      private static int count;
      private static boolean missingRequiredNote;



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> preInsert(InsertInput input, List<QRecord> records, boolean isPreview)
      {
         count += records.size();
         missingRequiredNote = input.getInputSource() == QInputSource.USER && records.stream().anyMatch(record -> record.getErrors().stream()
            .anyMatch(error -> error instanceof BadInputStatusMessage && error.getMessage().contains("Note")));
         return records;
      }
   }



   /*******************************************************************************
    ** This would erase the failed Note evidence if POST were allowed to run.
    *******************************************************************************/
   public static class ClearNestedFailure implements TableCustomizerInterface
   {
      private static int count;



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postInsert(InsertInput input, List<QRecord> records)
      {
         count++;
         for(QRecord record : records)
         {
            record.getAssociatedRecords().clear();
         }
         return records;
      }
   }



   /*******************************************************************************
    ** Observe the real required error, then try to erase it at that same leaf.
    *******************************************************************************/
   public static class ClearLeafFailure implements TableCustomizerInterface
   {
      private static int preCount;
      private static int postCount;
      private static boolean sawRequiredNote;



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> preInsert(InsertInput input, List<QRecord> records, boolean isPreview)
      {
         observe(input.getInputSource() == QInputSource.USER, records);
         return records;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> preUpdate(UpdateInput input, List<QRecord> records, boolean isPreview, Optional<List<QRecord>> oldRecordList)
      {
         observe(input.getInputSource() == QInputSource.USER, records);
         return records;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      private static void observe(boolean user, List<QRecord> records)
      {
         preCount += records.size();
         sawRequiredNote = user && records.stream().anyMatch(record -> record.getErrors().stream()
            .anyMatch(error -> error instanceof BadInputStatusMessage && error.getMessage().contains("Note")));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postInsert(InsertInput input, List<QRecord> records)
      {
         return clear(records);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postUpdate(UpdateInput input, List<QRecord> records, Optional<List<QRecord>> oldRecordList)
      {
         return clear(records);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      private static List<QRecord> clear(List<QRecord> records)
      {
         postCount++;
         records.forEach(record -> record.getErrors().clear());
         return records;
      }
   }
}
