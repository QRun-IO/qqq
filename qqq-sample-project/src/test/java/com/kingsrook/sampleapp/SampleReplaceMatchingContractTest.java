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


import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.tables.ReplaceAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.replace.ReplaceInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.replace.ReplaceOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DynamicDefaultValueBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Native H2 matching probes using existing canonical FieldLab columns. These
 ** exercise common Replace, not a SQL UPSERT or a native UNIQUE-key guarantee.
 ** Each matching tuple must identify exactly one desired/native owner. Failures
 ** must not be silently interpreted as an intentional empty replacement.
 *******************************************************************************/
class SampleReplaceMatchingContractTest
{
   private static final String TABLE = "fieldLab";
   private QInstance instance;
   private QTableMetaData canonical;
   private String canonicalJson;
   private QTableMetaData active;
   private String activeJson;



   /*******************************************************************************
    ** Copy behavior sets before disabling unrelated clock/user defaults. Field
    ** clones otherwise share their behavior sets with the canonical descriptor.
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
      Map<String, QTableMetaData> tables = new LinkedHashMap<>(instance.getTables());
      tables.put(TABLE, active);
      instance.setTables(tables);
      freezeMetaData();
      seed();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      QContext.clear();
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    ** SQL can find the normalized owner while Java matching still uses the input
    ** tuple. The existing owner must be retained, with native normalized storage.
    *******************************************************************************/
   @Test
   void testNormalizedMatchingKeyRetainsNativeOwner() throws Exception
   {
      assertRetainedOwner("normalizedKey", "normalized_key", " match ", true);
   }



   /*******************************************************************************
    ** Hidden matching values are operational input, not ordinary read projections.
    *******************************************************************************/
   @Test
   void testHiddenMatchingFieldRetainsNativeOwner() throws Exception
   {
      active.getField("normalizedKey").setIsHidden(true);
      for(QFieldSection section : active.getSections())
      {
         section.setFieldNames(section.getFieldNames().stream().filter(name -> !"normalizedKey".equals(name)).toList());
      }
      freezeMetaData();
      assertRetainedOwner("normalizedKey", "normalized_key", "MATCH", false);
   }



   /*******************************************************************************
    ** Use TEXT here to separate omitted-heavy matching from byte-array equality.
    *******************************************************************************/
   @Test
   void testHeavyMatchingFieldRetainsNativeOwner() throws Exception
   {
      active.getField("textValue").setIsHeavy(true);
      freezeMetaData();
      assertRetainedOwner("textValue", "text_value", "Owner text first", false);
   }



   /*******************************************************************************
    ** Matching a raw caller password must not compare it with a display mask.
    *******************************************************************************/
   @Test
   void testPasswordMatchingFieldRetainsNativeOwner() throws Exception
   {
      assertRetainedOwner("passwordValue", "password_value", "matching-secret-first", false);
   }



   /*******************************************************************************
    ** The physical column is DECIMAL(20,4); 1.0 and 1.0000 have the same value.
    ** This does not assert arbitrary vendor rounding or text collation semantics.
    *******************************************************************************/
   @Test
   void testDecimalMatchingUsesValueEqualityAcrossScale() throws Exception
   {
      assertRetainedOwner("decimalValue", "decimal_value", new BigDecimal("1.0"), false);
   }



   /*******************************************************************************
    ** Explicitly make BLOB non-heavy, isolating equal-content array matching from
    ** heavy-field omission. The equal-length decoy has different native bytes.
    *******************************************************************************/
   @Test
   void testBinaryMatchingUsesContentEquality() throws Exception
   {
      active.getField("blobValue").setIsHeavy(false);
      freezeMetaData();
      assertRetainedOwner("blobValue", "blob_value", new byte[] { 1, 2, 3 }, false);
   }



   /*******************************************************************************
    ** Replace accepts a caller matching key that need not be a declared UNIQUE
    ** constraint. Two owners cannot be selected by arbitrary query/map order.
    *******************************************************************************/
   @Test
   void testAmbiguousNativeOwnersRejectWithoutChoosingOrDeletingOne() throws Exception
   {
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         assertEquals(1, statement.executeUpdate("UPDATE field_lab SET long_value=10 WHERE id=2"));
      }
      assertEquals(List.of(1, 2), nativeOwners("long_value", 10L, false));
      assertRejectedUnchanged(input(new UniqueKey("longValue"), candidate("Candidate", 10L)));
   }



   /*******************************************************************************
    ** Non-key patches share one matching tuple. Omitting the unique name field
    ** prevents ordinary Update unique-key checks from concealing this ambiguity.
    *******************************************************************************/
   @Test
   void testDuplicateDesiredMatchedTuplesReject() throws Exception
   {
      assertEquals(List.of(1), nativeOwners("long_value", 10L, false));
      QRecord first = new QRecord().withValue("longValue", 10L).withValue("htmlValue", "First proposal");
      QRecord last = new QRecord().withValue("longValue", 10L).withValue("htmlValue", "Last proposal");
      assertRejectedUnchanged(input(new UniqueKey("longValue"), first, last));
   }



   /*******************************************************************************
    ** There is no physical UNIQUE constraint on long_value. Different names
    ** prove duplicate desired matching tuples are rejected by Replace itself.
    *******************************************************************************/
   @Test
   void testDuplicateDesiredNewTuplesReject() throws Exception
   {
      assertEquals(List.of(), nativeOwners("long_value", 77L, false));
      assertRejectedUnchanged(input(new UniqueKey("longValue"), candidate("First proposal", 77L), candidate("Last proposal", 77L)));
   }



   /*******************************************************************************
    ** Duplicates straddle the 1,000-record planning boundary; 999 valid new rows
    ** in between must not be committed before global matching ambiguity is known.
    *******************************************************************************/
   @Test
   void testDuplicateMatchedTupleAcrossPlanningPagesRejectsWholeRequest() throws Exception
   {
      assertPageBoundaryDuplicate(10L);
   }



   /*******************************************************************************
    ** New tuples are not visible in native storage during planning. The desired
    ** request itself must detect the duplicate on the next page.
    *******************************************************************************/
   @Test
   void testDuplicateNewTupleAcrossPlanningPagesRejectsWholeRequest() throws Exception
   {
      assertPageBoundaryDuplicate(77L);
   }



   /*******************************************************************************
    ** Missing/empty/unknown key definitions and invalid typed values must not be
    ** silently discarded into an empty keep set. Each probe gets fresh native rows.
    *******************************************************************************/
   @Test
   void testInvalidMatchingInputsCannotBecomeDestructiveEmptyReplacement()
   {
      assertAll(
         () -> assertInvalidKey(null),
         () -> assertInvalidKey(new UniqueKey()),
         () -> assertInvalidKey(new UniqueKey(List.of())),
         () -> assertInvalidKey(new UniqueKey("")),
         () -> assertInvalidKey(new UniqueKey("missingField")),
         () ->
         {
            seed();
            assertRejectedUnchanged(input(new UniqueKey("longValue"), new QRecord().withValue("name", "Invalid typed")
               .withValue("longValue", "not-a-number")));
         },
         () ->
         {
            seed();
            QRecord invalid = new QRecord().withValue("name", "Previously rejected").withValue("longValue", "not-a-number");
            invalid.addError(new BadInputStatusMessage("Caller already rejected this record"));
            assertRejectedUnchanged(input(new UniqueKey("longValue"), invalid));
         });
   }



   /*******************************************************************************
    ** Explicit empty records mean delete the selected segment, not all rows.
    ** Disabling deletes makes the same valid empty input a no-op. The separate
    ** missing-records case must not be interpreted as this deliberate clear.
    *******************************************************************************/
   @Test
   void testExplicitEmptyReplacementHonorsDeletionScopeAndDisableFlag() throws Exception
   {
      List<Map<String, String>> before = rows();
      ReplaceInput input = input(new UniqueKey("name"));
      QQueryFilter originalFilter = input.getFilter();
      ReplaceOutput output = new ReplaceAction().execute(input);
      assertSuccess(output, 0, 0, 1);
      assertAll(
         () -> assertEquals(List.of(before.get(1), before.get(2)), rows()),
         () -> assertSame(originalFilter, input.getFilter()),
         this::assertMetaDataUnchanged);
      seed();
      ReplaceInput disabled = input(new UniqueKey("name")).withPerformDeletes(false);
      ReplaceOutput noOp = new ReplaceAction().execute(disabled);
      assertSuccess(noOp, 0, 0, null);
      assertAll(
         () -> assertNull(noOp.getDeleteOutput()),
         () -> assertEquals(before, rows()),
         this::assertMetaDataUnchanged);
   }



   /*******************************************************************************
    ** Existing matches overwrite a caller-supplied wrong id with the actual owner.
    ** New ids are published only when requested, mapped correctly across two
    ** INSERT results interleaved with one UPDATE in the original input order.
    *******************************************************************************/
   @Test
   void testCallerIdentityAndOptionalInsertedKeyPublication() throws Exception
   {
      assertAll(
         () -> assertIdentityPublication(false, false),
         () -> assertIdentityPublication(true, false));
   }



   /*******************************************************************************
    ** A missing record list is not an explicit request to clear the segment. This
    ** stages the destructive-call boundary separately from the valid empty list.
    *******************************************************************************/
   @Test
   void testMissingRecordListCannotClearSelectedSegment() throws Exception
   {
      ReplaceInput input = input(new UniqueKey("name"));
      input.setRecords(null);
      assertNull(input.getRecords());
      assertRejectedUnchanged(input);
   }



   /*******************************************************************************
    ** The normal POST_INSERT extension may reorder successful native records.
    ** Caller key publication must still bind each identity to its actual row.
    *******************************************************************************/
   @Test
   void testInsertedKeyPublicationSurvivesReorderedNativeOutputs() throws Exception
   {
      active.withCustomizer(TableCustomizers.POST_INSERT_RECORD, new QCodeReference(ReverseInsertedRecords.class));
      freezeMetaData();
      assertIdentityPublication(true, true);
   }



   /*******************************************************************************
    ** Distinct desired owners survive both lookup pages; the omitted native row
    ** is deleted without changing any sparse fields or existing identities.
    *******************************************************************************/
   @Test
   void testDistinctOwnersAcrossMatchingPageBoundaryRetainAllNativeIdentities() throws Exception
   {
      List<QRecord> desired = new ArrayList<>();
      try(Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(
         "INSERT INTO field_lab(id,name,long_value,html_value) VALUES (?,?,?,?)"))
      {
         for(int index = 0; index < 1002; index++)
         {
            statement.setInt(1, 10000 + index);
            statement.setString(2, "Paged owner " + index);
            statement.setLong(3, 10000L + index);
            statement.setString(4, "Original paged payload");
            statement.addBatch();
            if(index < 1001)
            {
               desired.add(new QRecord().withValue("longValue", 10000L + index).withValue("htmlValue", "Updated paged payload"));
            }
         }
         assertEquals(1002, statement.executeBatch().length);
      }
      List<Map<String, String>> expected = rows();
      expected.removeIf(row -> "11001".equals(row.get("ID")));
      for(Map<String, String> row : expected)
      {
         if(Integer.parseInt(row.get("ID")) >= 10000)
         {
            row.put("HTML_VALUE", "Updated paged payload");
         }
      }
      ReplaceInput input = input(new UniqueKey("longValue"));
      input.setRecords(desired);
      input.setFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.GREATER_THAN_OR_EQUALS, 10000)));
      ReplaceOutput output = new ReplaceAction().execute(input);
      assertSuccess(output, 0, 1001, 1);
      for(int index = 0; index < desired.size(); index++)
      {
         assertEquals(10000 + index, desired.get(index).getValueInteger("id"));
      }
      assertAll(() -> assertEquals(expected, rows()), this::assertMetaDataUnchanged);
   }



   /*******************************************************************************
    ** Native equality is proved before Replace. Full row snapshots require that
    ** neither decoys nor fields omitted by the sparse desired record change.
    *******************************************************************************/
   private void assertRetainedOwner(String field, String column, Serializable value, boolean normalize) throws Exception
   {
      assertEquals(List.of(1), nativeOwners(column, value, normalize));
      List<Map<String, String>> expected = rows();
      expected.get(0).put("NAME", "Candidate");
      expected.get(0).put("HTML_VALUE", "Changed payload");
      QRecord candidate = new QRecord().withValue("name", "Candidate").withValue("htmlValue", "Changed payload").withValue(field, value);
      ReplaceInput input = input(new UniqueKey(field), candidate);
      String keyJson = JsonUtils.toJson(input.getKey());
      ReplaceOutput output = null;
      try
      {
         output = new ReplaceAction().execute(input);
      }
      finally
      {
         assertAll(
            () -> assertEquals(expected, rows()),
            () -> assertEquals(keyJson, JsonUtils.toJson(input.getKey())),
            this::assertMetaDataUnchanged);
      }
      assertSuccess(output, 0, 1, 0);
      assertEquals(1, candidate.getValueInteger("id"));
      assertEquals(1, output.getUpdateOutput().getRecords().get(0).getValueInteger("id"));
   }



   /*******************************************************************************
    ** The native long column has no extra constraint; distinct names make every
    ** intervening insert valid when considered on its own.
    *******************************************************************************/
   private void assertPageBoundaryDuplicate(long duplicate) throws Exception
   {
      QRecord first = candidate("Page first", duplicate);
      QRecord last = candidate("Page last", duplicate);
      if(duplicate == 10L)
      {
         first.removeValue("name");
         last.removeValue("name");
         first.setValue("htmlValue", "First matched proposal");
         last.setValue("htmlValue", "Last matched proposal");
      }
      List<QRecord> desired = new ArrayList<>();
      desired.add(first);
      for(int index = 1; index <= 999; index++)
      {
         desired.add(candidate("Page filler " + index, 1000L + index));
      }
      desired.add(last);
      assertEquals(1001, desired.size());
      assertEquals(duplicate, desired.get(0).getValueLong("longValue"));
      assertEquals(duplicate, desired.get(1000).getValueLong("longValue"));
      ReplaceInput input = input(new UniqueKey("longValue"));
      input.setRecords(desired);
      assertRejectedUnchanged(input);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertInvalidKey(UniqueKey key) throws Exception
   {
      seed();
      assertRejectedUnchanged(input(key, new QRecord().withValue("name", "Invalid definition")));
   }



   /*******************************************************************************
    ** An explicit negative outcome is mandatory; failure transport remains open
    ** until the action policy is implemented. SQL preservation is independent.
    *******************************************************************************/
   private void assertRejectedUnchanged(ReplaceInput input) throws Exception
   {
      List<Map<String, String>> before = rows();
      QQueryFilter filter = input.getFilter();
      String filterJson = JsonUtils.toJson(filter);
      String keyJson = JsonUtils.toJson(input.getKey());
      ReplaceOutput output = null;
      QException exception = null;
      try
      {
         output = new ReplaceAction().execute(input);
      }
      catch(QException e)
      {
         exception = e;
      }
      boolean rejected = exception != null || hasErrors(output);
      assertAll(
         () -> assertTrue(rejected, "Replace silently accepted invalid/ambiguous desired membership"),
         () -> assertEquals(before, rows()),
         () -> assertSame(filter, input.getFilter()),
         () -> assertEquals(filterJson, JsonUtils.toJson(filter)),
         () -> assertEquals(keyJson, JsonUtils.toJson(input.getKey())),
         this::assertMetaDataUnchanged);
   }



   /*******************************************************************************
    ** Two native generated keys are checked by their input names, not by assuming
    ** identity sequence values. Explicit wrong match id points at the decoy row.
    *******************************************************************************/
   private void assertIdentityPublication(boolean publishInsertedKeys, boolean reversedOutput) throws Exception
   {
      seed();
      List<Map<String, String>> before = rows();
      QRecord first = new QRecord().withValue("name", "New first").withValue("htmlValue", "First payload");
      QRecord matched = new QRecord().withValue("id", 3).withValue("name", "Target").withValue("htmlValue", "Matched payload");
      QRecord last = new QRecord().withValue("name", "New last").withValue("htmlValue", "Last payload");
      ReplaceInput input = input(new UniqueKey("name"), first, matched, last).withPerformDeletes(false);
      if(publishInsertedKeys)
      {
         input.setSetPrimaryKeyInInsertedRecords(true);
      }
      else
      {
         assertFalse(input.getSetPrimaryKeyInInsertedRecords());
      }
      ReplaceOutput output = new ReplaceAction().execute(input);
      assertSuccess(output, 2, 1, null);
      Map<String, Integer> insertedIds = new LinkedHashMap<>();
      for(QRecord inserted : output.getInsertOutput().getRecords())
      {
         assertNull(insertedIds.put(inserted.getValueString("name"), inserted.getValueInteger("id")));
      }
      assertEquals(reversedOutput ? List.of("New last", "New first") : List.of("New first", "New last"), new ArrayList<>(insertedIds.keySet()));
      assertTrue(insertedIds.values().stream().allMatch(id -> id != null && id > 3));
      assertEquals(2, new HashSet<>(insertedIds.values()).size());
      List<Map<String, String>> expected = new ArrayList<>(before);
      expected.get(0).put("HTML_VALUE", "Matched payload");
      expected.add(insertedRow(before.get(0), insertedIds.get("New first"), "New first", "First payload"));
      expected.add(insertedRow(before.get(0), insertedIds.get("New last"), "New last", "Last payload"));
      expected.sort((left, right) -> Integer.compare(Integer.parseInt(left.get("ID")), Integer.parseInt(right.get("ID"))));
      assertAll(
         () -> assertEquals(1, matched.getValueInteger("id")),
         () -> assertSame(first, input.getRecords().get(0)),
         () -> assertSame(matched, input.getRecords().get(1)),
         () -> assertSame(last, input.getRecords().get(2)),
         () -> assertEquals(publishInsertedKeys ? insertedIds.get("New first") : null, first.getValue("id")),
         () -> assertEquals(publishInsertedKeys ? insertedIds.get("New last") : null, last.getValue("id")),
         () -> assertNull(output.getDeleteOutput()),
         () -> assertEquals(expected, rows()),
         this::assertMetaDataUnchanged);
   }



   /*******************************************************************************
    ** Input filter deliberately limits only omission deletion. Matching is global.
    *******************************************************************************/
   private ReplaceInput input(UniqueKey key, QRecord... records)
   {
      ReplaceInput input = new ReplaceInput().withKey(key).withRecords(List.of(records))
         .withFilter(new QQueryFilter(new QFilterCriteria("longValue", QCriteriaOperator.EQUALS, 10L)));
      input.setTableName(TABLE);
      return input;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord candidate(String name, long key)
   {
      return new QRecord().withValue("name", name).withValue("longValue", key).withValue("htmlValue", "Desired payload");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private boolean hasErrors(ReplaceOutput output)
   {
      if(output == null)
      {
         return false;
      }
      return output.getInsertOutput() != null && output.getInsertOutput().getRecords().stream().anyMatch(r -> !r.getErrors().isEmpty())
         || output.getUpdateOutput() != null && output.getUpdateOutput().getRecords().stream().anyMatch(r -> !r.getErrors().isEmpty())
         || output.getDeleteOutput() != null && CollectionUtils.nullSafeHasContents(output.getDeleteOutput().getRecordsWithErrors());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertSuccess(ReplaceOutput output, int inserts, int updates, Integer deletes)
   {
      assertNotNull(output);
      assertAll(
         () -> assertEquals(inserts, output.getInsertOutput().getRecords().size()),
         () -> assertEquals(updates, output.getUpdateOutput().getRecords().size()),
         () -> assertFalse(hasErrors(output), JsonUtils.toJson(output)));
      if(deletes != null)
      {
         assertEquals(deletes, output.getDeleteOutput().getDeletedRecordCount());
      }
   }



   /*******************************************************************************
    ** Arbitrary runtime matching keys use existing columns, without changing
    ** FieldLab's physical name/normalized_key UNIQUE constraints or INTEGER PK.
    *******************************************************************************/
   private void seed() throws Exception
   {
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         statement.executeUpdate("DELETE FROM field_lab");
         assertEquals(3, statement.executeUpdate("INSERT INTO field_lab(id,name,normalized_key,long_value,decimal_value,password_value,blob_value,text_value,html_value) VALUES "
            + "(1,'Target','MATCH',10,1.0000,'matching-secret-first',X'010203','Owner text first','Original HTML first'),"
            + "(2,'Decoy','DECOY',20,2.0000,'matching-secret-second',X'040506','Owner text second','Original HTML second'),"
            + "(3,'Outside','OUT',30,3.0000,'matching-secret-third',X'070809','Owner text third','Original HTML third')"));
      }
   }



   /*******************************************************************************
    ** Native equality independently proves which row should match. Only the
    ** preselected test column names are used; candidate values remain parameters.
    *******************************************************************************/
   private List<Integer> nativeOwners(String column, Serializable value, boolean normalize) throws Exception
   {
      List<Integer> ids = new ArrayList<>();
      try(Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(
         "SELECT id FROM field_lab WHERE " + column + " = " + (normalize ? "UPPER(TRIM(?))" : "?") + " ORDER BY id"))
      {
         if(value instanceof byte[] bytes)
         {
            statement.setBytes(1, bytes);
         }
         else
         {
            statement.setObject(1, value);
         }
         try(ResultSet rows = statement.executeQuery())
         {
            while(rows.next())
            {
               ids.add(rows.getInt(1));
            }
         }
      }
      return ids;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Map<String, String> insertedRow(Map<String, String> template, int id, String name, String html)
   {
      Map<String, String> row = new LinkedHashMap<>();
      template.keySet().forEach(key -> row.put(key, null));
      row.put("ID", String.valueOf(id));
      row.put("NAME", name);
      row.put("HTML_VALUE", html);
      row.put("BOOLEAN_VALUE", "TRUE");
      return row;
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
   private List<Map<String, String>> rows() throws Exception
   {
      try(Connection connection = connection())
      {
         return rows(connection);
      }
   }



   /*******************************************************************************
    ** Full physical row comparison includes actual BLOB contents, not lengths or
    ** driver object descriptions. No framework query supplies the expected data.
    *******************************************************************************/
   private List<Map<String, String>> rows(Connection connection) throws Exception
   {
      List<Map<String, String>> result = new ArrayList<>();
      try(Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT * FROM field_lab ORDER BY id"))
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
   private void freezeMetaData() throws Exception
   {
      new QInstanceValidator().revalidate(instance);
      activeJson = JsonUtils.toJson(active);
      assertMetaDataUnchanged();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertMetaDataUnchanged() throws Exception
   {
      assertSame(active, instance.getTable(TABLE));
      assertEquals(activeJson, JsonUtils.toJson(active));
      assertEquals(canonicalJson, JsonUtils.toJson(canonical));
   }


   /*******************************************************************************
    ** Reorder successful native outputs through the documented callback. Clones
    ** prevent object-reference equality from accidentally supplying the mapping.
    *******************************************************************************/
   public static class ReverseInsertedRecords implements TableCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postInsert(InsertInput input, List<QRecord> records)
      {
         assertEquals(2, records.size());
         return List.of(new QRecord(records.get(1)), new QRecord(records.get(0)));
      }
   }

}
