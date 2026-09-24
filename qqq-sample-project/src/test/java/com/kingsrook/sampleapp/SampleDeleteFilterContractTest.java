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

package com.kingsrook.sampleapp;


import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QValueException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.statusmessages.NotFoundStatusMessage;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Destructive selection uses the canonical FieldLab table with native row
 ** oracles. Input exceptions and per-key not-found records are distinct results.
 *******************************************************************************/
class SampleDeleteFilterContractTest
{
   private static final String TABLE = "fieldLab";
   private QInstance instance;



   /*******************************************************************************
    ** Fixed native rows cannot hide a broadened filter behind an empty fixture.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      QContext.init(instance, new QSession());
      seedRows();
   }



   /*******************************************************************************
    ** This test owns only its canonical in-memory fixture and context.
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      QContext.clear();
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    ** Each malformed shape gets a fresh populated fixture. Nested AND controls
    ** prove that dropping only one broken criterion can broaden a subset too.
    *******************************************************************************/
   @Test
   void testMalformedFilterStructureCannotDeleteRows() throws Exception
   {
      Map<String, QQueryFilter> invalid = Map.ofEntries(
         Map.entry("sole missing operator", new QQueryFilter(new QFilterCriteria("id", null, 1))),
         Map.entry("nested missing operator", new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.GREATER_THAN, 1))
            .withSubFilter(new QQueryFilter(new QFilterCriteria("id", null, 2)))),
         Map.entry("sole missing field", new QQueryFilter(new QFilterCriteria().withOperator(QCriteriaOperator.EQUALS).withValues(List.of(1)))),
         Map.entry("nested missing field", new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.GREATER_THAN, 1))
            .withSubFilter(new QQueryFilter(new QFilterCriteria().withOperator(QCriteriaOperator.EQUALS).withValues(List.of(2))))),
         Map.entry("blank field", new QQueryFilter(new QFilterCriteria(" \t ", QCriteriaOperator.EQUALS, 1))),
         Map.entry("null criterion", new QQueryFilter().withCriteria((QFilterCriteria) null)),
         Map.entry("missing sort field", new QQueryFilter().withOrderBy(new QFilterOrderBy(null))),
         Map.entry("null sort", new QQueryFilter().withOrderBy((QFilterOrderBy) null)),
         Map.entry("null subfilter", new QQueryFilter().withSubFilters(Arrays.asList((QQueryFilter) null))));
      assertAll(invalid.entrySet().stream().map(entry -> (Executable) () ->
      {
         seedRows();
         assertRejectedFilter(entry.getKey(), entry.getValue());
      }));
   }



   /*******************************************************************************
    ** Valid structure still cannot carry an unknown field, malformed scalar or
    ** fixed-arity mismatch into a destructive query. No HTTP status is assumed.
    *******************************************************************************/
   @Test
   void testMalformedFilterOperandsCannotDeleteRows() throws Exception
   {
      Map<String, QQueryFilter> invalid = Map.ofEntries(
         Map.entry("unknown field", new QQueryFilter(new QFilterCriteria("missingField", QCriteriaOperator.EQUALS, 1))),
         Map.entry("unknown sort", new QQueryFilter().withOrderBy(new QFilterOrderBy("missingSort"))),
         Map.entry("invalid integer", new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, "not-an-integer"))),
         Map.entry("missing range endpoint", new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.BETWEEN, 1))),
         Map.entry("too many scalar values", new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 1, 2))));
      assertAll(invalid.entrySet().stream().map(entry -> (Executable) () ->
      {
         seedRows();
         assertRejectedFilter(entry.getKey(), entry.getValue());
      }));
   }



   /*******************************************************************************
    ** Negative paging must reject rather than silently become an unpaged delete.
    *******************************************************************************/
   @Test
   void testNegativeFilterPaginationCannotDeleteRows() throws Exception
   {
      assertAll(
         () ->
         {
            seedRows();
            assertRejectedFilter("negative skip", new QQueryFilter().withSkip(-1));
         },
         () ->
         {
            seedRows();
            assertRejectedFilter("negative limit", new QQueryFilter().withLimit(-1));
         });
   }



   /*******************************************************************************
    ** A selective nested filter deletes exactly its native matching rows.
    *******************************************************************************/
   @Test
   void testNestedFilterDeletesOnlyItsNativeSubset() throws Exception
   {
      QQueryFilter filter = new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.GREATER_THAN, 1))
         .withSubFilter(new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR)
            .withCriteria(new QFilterCriteria("name", QCriteriaOperator.STARTS_WITH, "Th"))
            .withCriteria(new QFilterCriteria("longValue", QCriteriaOperator.GREATER_THAN, 35)));
      List<List<String>> survivors = rows("SELECT * FROM field_lab WHERE NOT(id>1 AND (name LIKE 'Th%' OR long_value>35)) ORDER BY id");
      assertEquals(List.of(List.of("3"), List.of("4")), rows("SELECT id FROM field_lab WHERE id>1 AND (name LIKE 'Th%' OR long_value>35) ORDER BY id"));
      DeleteInput input = new DeleteInput(TABLE).withQueryFilter(filter);
      String beforeFilter = JsonUtils.toJson(filter);
      DeleteOutput output = new DeleteAction().execute(input);
      assertAll(
         () -> assertEquals(2, output.getDeletedRecordCount()),
         () -> assertTrue(CollectionUtils.nullSafeIsEmpty(output.getRecordsWithErrors())),
         () -> assertEquals(survivors, snapshot()),
         () -> assertSame(filter, input.getQueryFilter()),
         () -> assertEquals(beforeFilter, JsonUtils.toJson(filter)),
         () -> assertNull(input.getPrimaryKeys()));
   }



   /*******************************************************************************
    ** Sort/skip/limit determine keys before DML; changing order selects a different
    ** row in this deliberately non-key-ordered fixture.
    *******************************************************************************/
   @Test
   void testFilterSortSkipAndLimitSelectExactNativeKeys() throws Exception
   {
      QQueryFilter filter = new QQueryFilter().withOrderBy(new QFilterOrderBy("longValue", true)).withSkip(1).withLimit(1);
      assertEquals(List.of(List.of("3")), rows("SELECT id FROM field_lab ORDER BY long_value ASC OFFSET 1 ROWS FETCH NEXT 1 ROWS ONLY"));
      List<List<String>> survivors = rows("SELECT * FROM field_lab WHERE id<>3 ORDER BY id");
      DeleteInput input = new DeleteInput(TABLE).withPrimaryKeys(List.of()).withQueryFilter(filter);
      String beforeFilter = JsonUtils.toJson(filter);
      DeleteOutput output = new DeleteAction().execute(input);
      assertAll(
         () -> assertEquals(1, output.getDeletedRecordCount()),
         () -> assertTrue(CollectionUtils.nullSafeIsEmpty(output.getRecordsWithErrors())),
         () -> assertEquals(survivors, snapshot()),
         () -> assertEquals(List.of(), input.getPrimaryKeys()),
         () -> assertSame(filter, input.getQueryFilter()),
         () -> assertEquals(beforeFilter, JsonUtils.toJson(filter)));
   }



   /*******************************************************************************
    ** FieldLab's declared uppercase/trim filter behavior must also govern delete
    ** selection without rewriting the caller's saved criterion values.
    *******************************************************************************/
   @Test
   void testDeclaredFilterNormalizationSelectsStoredKey() throws Exception
   {
      QQueryFilter filter = new QQueryFilter(new QFilterCriteria("normalizedKey", QCriteriaOperator.EQUALS, " alpha "));
      String beforeFilter = JsonUtils.toJson(filter);
      List<List<String>> survivors = rows("SELECT * FROM field_lab WHERE normalized_key<>'ALPHA' ORDER BY id");
      assertEquals(List.of(List.of("1")), rows("SELECT id FROM field_lab WHERE normalized_key='ALPHA'"));
      DeleteInput input = new DeleteInput(TABLE).withQueryFilter(filter);
      DeleteOutput output = new DeleteAction().execute(input);
      assertAll(
         () -> assertEquals(1, output.getDeletedRecordCount()),
         () -> assertTrue(CollectionUtils.nullSafeIsEmpty(output.getRecordsWithErrors())),
         () -> assertEquals(survivors, snapshot()),
         () -> assertSame(filter, input.getQueryFilter()),
         () -> assertEquals(beforeFilter, JsonUtils.toJson(filter)));
   }



   /*******************************************************************************
    ** A USER-removed field cannot select a deletion. SYSTEM retains its declared
    ** selection field; the two invocations use independently populated rows.
    *******************************************************************************/
   @Test
   void testUserRemovedFilterFieldRejectsAndSystemCanSelect() throws Exception
   {
      instance.addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(PrivateFilterField.class));
      QTableMetaData canonical = instance.getTable(TABLE);
      List<String> fields = new ArrayList<>(canonical.getFields().keySet());
      assertAll(
         () ->
         {
            QQueryFilter filter = new QQueryFilter(new QFilterCriteria("textValue", QCriteriaOperator.EQUALS, "Preserve one"));
            DeleteInput input = new DeleteInput(TABLE).withInputSource(QInputSource.USER).withQueryFilter(filter);
            List<List<String>> before = snapshot();
            String beforeFilter = JsonUtils.toJson(filter);
            assertAll(
               () -> assertThrows(QException.class, () -> new DeleteAction().execute(input)),
               () -> assertEquals(before, snapshot()),
               () -> assertSame(filter, input.getQueryFilter()),
               () -> assertEquals(beforeFilter, JsonUtils.toJson(filter)),
               () -> assertNull(input.getPrimaryKeys()));
         },
         () ->
         {
            seedRows();
            List<List<String>> survivors = rows("SELECT * FROM field_lab WHERE text_value<>'Preserve one' ORDER BY id");
            QQueryFilter filter = new QQueryFilter(new QFilterCriteria("textValue", QCriteriaOperator.EQUALS, "Preserve one"));
            DeleteOutput output = new DeleteAction().execute(new DeleteInput(TABLE).withInputSource(QInputSource.SYSTEM).withQueryFilter(filter));
            assertAll(
               () -> assertEquals(1, output.getDeletedRecordCount()),
               () -> assertTrue(CollectionUtils.nullSafeIsEmpty(output.getRecordsWithErrors())),
               () -> assertEquals(survivors, snapshot()));
         },
         () -> assertSame(canonical, instance.getTable(TABLE)),
         () -> assertEquals(fields, new ArrayList<>(canonical.getFields().keySet())));
   }



   /*******************************************************************************
    ** Immutable typed key lists are accepted without changing their values.
    *******************************************************************************/
   @Test
   void testImmutableTypedKeysDeleteOnceAndRemainUnchanged() throws Exception
   {
      assertTypedKeysPreserved(List.of("1", 1, "2"));
   }



   /*******************************************************************************
    ** Conversion must not mutate a caller-held mutable list through its wrapper.
    *******************************************************************************/
   @Test
   void testMutableTypedKeysDeleteOnceAndRemainUnchanged() throws Exception
   {
      assertTypedKeysPreserved(new ArrayList<>(List.of("1", 1, "2")));
   }



   /*******************************************************************************
    ** A null key element is not an absent key list. Found keys are deleted once;
    ** missing/null keys yield not-found records rather than input-aligned success.
    *******************************************************************************/
   @Test
   void testFoundMissingAndNullKeysHaveSeparateOutcomes() throws Exception
   {
      List<Serializable> keys = Arrays.asList(1, 1, 999, null);
      List<List<String>> survivors = rows("SELECT * FROM field_lab WHERE id<>1 ORDER BY id");
      DeleteInput input = new DeleteInput(TABLE).withPrimaryKeys(keys);
      DeleteOutput output = new DeleteAction().execute(input);
      assertAll(
         () -> assertEquals(1, output.getDeletedRecordCount()),
         () -> assertEquals(2, CollectionUtils.nonNullList(output.getRecordsWithErrors()).size()),
         () -> assertTrue(CollectionUtils.nonNullList(output.getRecordsWithErrors()).stream()
            .allMatch(record -> record.getErrors().stream().anyMatch(error -> error instanceof NotFoundStatusMessage))),
         () -> assertEquals(Arrays.asList(999, null), CollectionUtils.nonNullList(output.getRecordsWithErrors()).stream().map(record -> record.getValueInteger("id")).toList()),
         () -> assertEquals(survivors, snapshot()),
         () -> assertEquals(Arrays.asList(1, 1, 999, null), input.getPrimaryKeys()),
         () -> assertEquals(Arrays.asList(1, 1, 999, null), keys),
         () -> assertNull(input.getQueryFilter()));
   }



   /*******************************************************************************
    ** Nonempty explicit keys and a filter are conflicting selectors, not union
    ** or intersection semantics. Rejection restores both caller inputs.
    *******************************************************************************/
   @Test
   void testConflictingSelectorsRejectWithoutMutation() throws Exception
   {
      QQueryFilter filter = new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 2));
      DeleteInput input = new DeleteInput(TABLE).withPrimaryKey("1").withQueryFilter(filter);
      List<List<String>> before = snapshot();
      String beforeFilter = JsonUtils.toJson(filter);
      assertAll(
         () -> assertThrows(QException.class, () -> new DeleteAction().execute(input)),
         () -> assertEquals(before, snapshot()),
         () -> assertEquals(List.of("1"), input.getPrimaryKeys()),
         () -> assertSame(filter, input.getQueryFilter()),
         () -> assertEquals(beforeFilter, JsonUtils.toJson(filter)));
   }



   /*******************************************************************************
    ** No key selection is a no-op; an explicit empty filter deliberately selects
    ** all eligible rows. A zero-limit or unmatched filter selects none.
    *******************************************************************************/
   @Test
   void testEmptySelectionDiffersFromExplicitEmptyFilter() throws Exception
   {
      List<List<String>> before = snapshot();
      for(DeleteInput input : List.of(new DeleteInput(TABLE), new DeleteInput(TABLE).withPrimaryKeys(List.of()),
         new DeleteInput(TABLE).withQueryFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 999))),
         new DeleteInput(TABLE).withQueryFilter(new QQueryFilter().withLimit(0))))
      {
         List<Serializable> keys = input.getPrimaryKeys() == null ? null : new ArrayList<>(input.getPrimaryKeys());
         QQueryFilter filter = input.getQueryFilter();
         DeleteOutput output = new DeleteAction().execute(input);
         assertEquals(0, output.getDeletedRecordCount());
         assertTrue(CollectionUtils.nullSafeIsEmpty(output.getRecordsWithErrors()));
         assertEquals(before, snapshot());
         assertEquals(keys, input.getPrimaryKeys());
         assertSame(filter, input.getQueryFilter());
      }
      QQueryFilter all = new QQueryFilter();
      DeleteInput input = new DeleteInput(TABLE).withQueryFilter(all);
      DeleteOutput output = new DeleteAction().execute(input);
      assertEquals(4, output.getDeletedRecordCount());
      assertTrue(CollectionUtils.nullSafeIsEmpty(output.getRecordsWithErrors()));
      assertEquals(List.of(), snapshot());
      assertSame(all, input.getQueryFilter());
      assertNull(input.getPrimaryKeys());
   }



   /*******************************************************************************
    ** Invalid scalar key conversion is an invocation error, not a missing-row
    ** result. Original scalar values remain available to the caller after failure.
    *******************************************************************************/
   @Test
   void testMalformedScalarKeysRejectBeforeAnyDeletion() throws Exception
   {
      List<List<String>> before = snapshot();
      assertAll(List.of("not-an-integer", "1.5").stream().map(value -> (Executable) () ->
      {
         DeleteInput input = new DeleteInput(TABLE).withPrimaryKey(value);
         assertAll(value,
            () -> assertThrows(QValueException.class, () -> new DeleteAction().execute(input)),
            () -> assertEquals(before, snapshot()),
            () -> assertEquals(List.of(value), input.getPrimaryKeys()),
            () -> assertNull(input.getQueryFilter()));
      }));
   }



   /*******************************************************************************
    ** A missing or unknown table is rejected at the common invocation boundary.
    *******************************************************************************/
   @Test
   void testMissingOrUnknownTableRejectsWithoutMutation() throws Exception
   {
      List<List<String>> before = snapshot();
      assertAll(
         () -> assertThrows(QException.class, () -> new DeleteAction().execute(new DeleteInput())),
         () -> assertThrows(QException.class, () -> new DeleteAction().execute(new DeleteInput("missingDeleteTable").withPrimaryKey(1))),
         () -> assertThrows(QException.class, () -> new DeleteAction().execute(new DeleteInput(" \t ").withPrimaryKey(1))),
         () -> assertEquals(before, snapshot()));
   }



   /*******************************************************************************
    ** Session and context errors reject before mutation. The same pending input
    ** then succeeds with a restored context and enclosing action stack.
    *******************************************************************************/
   @Test
   void testMissingContextAndSessionRejectThenRecover() throws Exception
   {
      List<List<String>> before = snapshot();
      DeleteInput input = new DeleteInput(TABLE).withPrimaryKey(1);
      assertEquals(QInputSource.SYSTEM, input.getInputSource());
      try
      {
         QContext.setQSession(null);
         assertEquals("QSession was not set in QContext.", assertThrows(QException.class, () -> new DeleteAction().execute(input)).getMessage());
         QContext.clear();
         assertEquals("QInstance was not set in QContext.", assertThrows(QException.class, () -> new DeleteAction().execute(input)).getMessage());
      }
      finally
      {
         QContext.init(instance, new QSession());
      }
      assertEquals(before, snapshot());
      assertEquals(List.of(1), input.getPrimaryKeys());
      DeleteInput enclosing = new DeleteInput(TABLE);
      QContext.pushAction(enclosing);
      try
      {
         List<AbstractActionInput> actions = new ArrayList<>(QContext.getActionStack());
         List<List<String>> survivors = rows("SELECT * FROM field_lab WHERE id<>1 ORDER BY id");
         DeleteOutput output = new DeleteAction().execute(input);
         assertAll(
            () -> assertEquals(1, output.getDeletedRecordCount()),
            () -> assertTrue(CollectionUtils.nullSafeIsEmpty(output.getRecordsWithErrors())),
            () -> assertEquals(survivors, snapshot()),
            () -> assertEquals(List.of(1), input.getPrimaryKeys()),
            () -> assertEquals(actions, QContext.getActionStack()));
      }
      finally
      {
         QContext.popAction();
      }
   }



   /*******************************************************************************
    ** Assertion grouping preserves actual SQL evidence even if no exception is
    ** thrown. Every caller of this helper starts with fresh nonempty rows.
    *******************************************************************************/
   private void assertRejectedFilter(String label, QQueryFilter filter) throws Exception
   {
      List<List<String>> before = snapshot();
      assertEquals(4, before.size());
      String beforeFilter = JsonUtils.toJson(filter);
      DeleteInput input = new DeleteInput(TABLE).withQueryFilter(filter);
      QContext.pushAction(new DeleteInput(TABLE));
      try
      {
         List<AbstractActionInput> actions = new ArrayList<>(QContext.getActionStack());
         assertAll(label,
            () -> assertThrows(QException.class, () -> new DeleteAction().execute(input)),
            () -> assertEquals(before, snapshot()),
            () -> assertSame(filter, input.getQueryFilter()),
            () -> assertEquals(beforeFilter, JsonUtils.toJson(filter)),
            () -> assertNull(input.getPrimaryKeys()),
            () -> assertEquals(actions, QContext.getActionStack()));
      }
      finally
      {
         QContext.popAction();
      }
   }



   /*******************************************************************************
    ** Distinct typed targets determine the root count, not the raw request length.
    *******************************************************************************/
   private void assertTypedKeysPreserved(List<Serializable> keys) throws Exception
   {
      List<Serializable> original = new ArrayList<>(keys);
      List<List<String>> survivors = rows("SELECT * FROM field_lab WHERE id NOT IN(1,2) ORDER BY id");
      DeleteInput input = new DeleteInput(TABLE).withPrimaryKeys(keys);
      DeleteOutput output = new DeleteAction().execute(input);
      assertAll(
         () -> assertEquals(2, output.getDeletedRecordCount()),
         () -> assertTrue(CollectionUtils.nullSafeIsEmpty(output.getRecordsWithErrors())),
         () -> assertEquals(survivors, snapshot()),
         () -> assertEquals(original, keys),
         () -> assertEquals(original, input.getPrimaryKeys()),
         () -> assertNull(input.getQueryFilter()));
   }



   /*******************************************************************************
    ** Recreate only the owned FieldLab rows between independent malformed cases.
    ** Each case checks its before/after snapshot before another case reseeds.
    *******************************************************************************/
   private void seedRows() throws SQLException
   {
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          Statement statement = connection.createStatement())
      {
         statement.executeUpdate("DELETE FROM field_lab");
         assertEquals(4, statement.executeUpdate("INSERT INTO field_lab(id,name,long_value,text_value,normalized_key) VALUES "
            + "(1,'One',10,'Preserve one','ALPHA'),(2,'Two',30,'Preserve two','BETA'),"
            + "(3,'Three',20,'Preserve three','GAMMA'),(4,'Four',40,'Preserve four','DELTA')"));
      }
   }



   /*******************************************************************************
    ** Read all native fields, including unchanged nullable values and decoys.
    *******************************************************************************/
   private List<List<String>> snapshot() throws SQLException
   {
      return rows("SELECT * FROM field_lab ORDER BY id");
   }



   /*******************************************************************************
    ** Every native read closes only its own connection and result resources.
    *******************************************************************************/
   private List<List<String>> rows(String sql) throws SQLException
   {
      List<List<String>> rows = new ArrayList<>();
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql))
      {
         while(result.next())
         {
            List<String> row = new ArrayList<>();
            for(int column = 1; column <= result.getMetaData().getColumnCount(); column++)
            {
               row.add(result.getString(column));
            }
            rows.add(row);
         }
      }
      return rows;
   }



   /*******************************************************************************
    ** Use existing supplemental metadata personalization without mutating schema.
    *******************************************************************************/
   public static class PrivateFilterField implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         if(!TABLE.equals(input.getTableName()) || !QInputSource.USER.equals(input.getInputSource()))
         {
            return input.getTable();
         }
         QTableMetaData table = input.getTable().clone();
         table.getFields().remove("textValue");
         return table;
      }
   }
}
