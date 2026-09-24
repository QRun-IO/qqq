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
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** BLOB/DECIMAL keys are logical metadata variations on existing FieldLab
 ** columns. The native INTEGER id primary-key constraint is never changed.
 ** Native filter selection and presentation prefetch are checked separately.
 *******************************************************************************/
class SampleDeleteKeyContractTest
{
   private static final String TABLE = "fieldLab";
   private QInstance instance;
   private QTableMetaData canonical;
   private String canonicalJson;
   private QTableMetaData active;
   private String activeJson;



   /*******************************************************************************
    ** Independent native values have distinct keys but identical binary lengths.
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
    ** The fresh metadata instance and database belong only to this test.
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      QContext.clear();
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    ** Equal contents in separate arrays identify one target despite JDBC copies.
    *******************************************************************************/
   @Test
   void testLogicalBinaryDuplicateKeysDeleteOnlyTarget() throws Exception
   {
      useLogicalKey("blobValue", false, false, false);
      assertDuplicateKeysDeleteTarget(List.of(new byte[] { 1, 2, 3 }, new byte[] { 1, 2, 3 }));
   }



   /*******************************************************************************
    ** Different request scales and DECIMAL(20,4) storage identify one target.
    *******************************************************************************/
   @Test
   void testLogicalDecimalDuplicateKeysDeleteOnlyTarget() throws Exception
   {
      useLogicalKey("decimalValue", false, false, false);
      assertDuplicateKeysDeleteTarget(List.of(new BigDecimal("2.0"), new BigDecimal("2.000")));
   }



   /*******************************************************************************
    ** Ordinary name filtering must preserve the selected binary key's contents.
    *******************************************************************************/
   @Test
   void testLogicalBinaryFilterDeletesOnlyTarget() throws Exception
   {
      useLogicalKey("blobValue", false, false, false);
      assertNativeFilterKey();
      assertSuccessfulDelete(true);
   }



   /*******************************************************************************
    ** Native selected decimal scale survives validation and destructive DML.
    *******************************************************************************/
   @Test
   void testLogicalDecimalFilterDeletesOnlyTarget() throws Exception
   {
      useLogicalKey("decimalValue", false, false, false);
      assertNativeFilterKey();
      assertSuccessfulDelete(true);
   }



   /*******************************************************************************
    ** A replacement customizer's copied binary key rejects the selected target
    ** for both explicit keys and an ordinary string filter.
    *******************************************************************************/
   @Test
   void testCopiedBinaryCustomizerKeyPreservesTarget() throws Exception
   {
      useLogicalKey("blobValue", false, false, true);
      assertRejectedSelections();
   }



   /*******************************************************************************
    ** A replacement customizer's equivalent decimal scale is not a different key.
    *******************************************************************************/
   @Test
   void testDifferentScaleCustomizerKeyPreservesTarget() throws Exception
   {
      useLogicalKey("decimalValue", false, false, true);
      assertRejectedSelections();
   }



   /*******************************************************************************
    ** Hidden is a presentation property. Native selection can find this key even
    ** when a later default presentation read would remove it before validation.
    *******************************************************************************/
   @Test
   void testHiddenLogicalKeySupportsNativeSelectionAndBothDeleteInputs() throws Exception
   {
      useLogicalKey("decimalValue", true, false, false);
      assertPresentationKeyPaths();
   }



   /*******************************************************************************
    ** Heavy-field lengths are not identities. Selection and prefetch must retain
    ** the actual binary key privately, not replace it with null or its length.
    *******************************************************************************/
   @Test
   void testHeavyLogicalKeySupportsNativeSelectionAndBothDeleteInputs() throws Exception
   {
      useLogicalKey("blobValue", false, true, false);
      assertPresentationKeyPaths();
   }



   /*******************************************************************************
    ** A callback can mutate a visible binary key's array after native deletion.
    ** Its failure warning must retain an independent original key and count.
    *******************************************************************************/
   @Test
   void testPostDeleteBinaryKeyMutationPreservesSuccessfulOutcome() throws Exception
   {
      useLogicalKey("blobValue", false, false, false);
      active.withCustomizer(TableCustomizers.POST_DELETE_RECORD, new QCodeReference(MutatePostDeleteBinaryKey.class));
      new QInstanceValidator().revalidate(instance);
      activeJson = JsonUtils.toJson(active);
      List<Map<String, String>> before = rows();
      DeleteInput input = input(false);
      List<Serializable> originalKeys = new ArrayList<>(input.getPrimaryKeys());
      DeleteOutput output;
      try
      {
         output = new DeleteAction().execute(input);
      }
      finally
      {
         assertAll(
            () -> assertEquals(List.of(before.get(1)), rows()),
            () -> assertSelectionUnchanged(input, originalKeys, null, "null"),
            () -> assertMetadataUnchanged());
      }
      assertAll(
         () -> assertEquals(1, output.getDeletedRecordCount()),
         () -> assertTrue(CollectionUtils.nullSafeIsEmpty(output.getRecordsWithErrors()), JsonUtils.toJson(output)),
         () -> assertEquals(1, CollectionUtils.nonNullList(output.getRecordsWithWarnings()).size()),
         () ->
         {
            QRecord warning = output.getRecordsWithWarnings().get(0);
            assertEquals("Target", warning.getValueString("name"));
            assertTargetKey(warning.getValue("blobValue"));
            assertTrue(warning.getErrors().isEmpty(), warning.getErrorsAsString());
            assertTrue(warning.getWarnings().stream().anyMatch(value -> value.getMessage().startsWith("An error occurred after the delete:")));
         });
   }



   /*******************************************************************************
    ** Each operation gets its own populated rows so a failing path cannot conceal
    ** another failure. Selection-only does not execute presentation customizers.
    *******************************************************************************/
   private void assertPresentationKeyPaths()
   {
      assertAll(
         () -> assertNativeFilterKey(),
         () ->
         {
            seedRows();
            assertSuccessfulDelete(false);
         },
         () ->
         {
            seedRows();
            assertSuccessfulDelete(true);
         });
   }



   /*******************************************************************************
    ** Per-operation rejection must be BadInput, not a false not-found outcome.
    *******************************************************************************/
   private void assertRejectedSelections()
   {
      assertAll(List.of(false, true).stream().map(filterSelection -> (Executable) () ->
      {
         seedRows();
         List<Map<String, String>> before = rows();
         DeleteInput input = input(filterSelection);
         QQueryFilter filter = input.getQueryFilter();
         String filterJson = JsonUtils.toJson(filter);
         List<Serializable> originalKeys = input.getPrimaryKeys() == null ? null : new ArrayList<>(input.getPrimaryKeys());
         DeleteOutput output = new DeleteAction().execute(input);
         assertAll(
            () -> assertEquals(before, rows()),
            () -> assertEquals(0, output.getDeletedRecordCount()),
            () -> assertEquals(1, CollectionUtils.nonNullList(output.getRecordsWithErrors()).size()),
            () ->
            {
               QRecord record = output.getRecordsWithErrors().get(0);
               assertEquals(1, record.getErrors().size());
               assertInstanceOf(BadInputStatusMessage.class, record.getErrors().get(0));
               assertEquals("Keep this logical target", record.getErrors().get(0).getMessage());
               assertTargetKey(record.getValue(active.getPrimaryKeyField()));
            },
            () -> assertTrue(CollectionUtils.nullSafeIsEmpty(output.getRecordsWithWarnings())),
            () -> assertSelectionUnchanged(input, originalKeys, filter, filterJson),
            () -> assertMetadataUnchanged());
      }));
   }



   /*******************************************************************************
    ** Native duplicate identity must coalesce before DML; a separate missing key
    ** then returns NotFound without changing the remaining decoy.
    *******************************************************************************/
   private void assertDuplicateKeysDeleteTarget(List<Serializable> keys) throws Exception
   {
      List<Map<String, String>> before = rows();
      DeleteInput input = new DeleteInput(TABLE).withInputSource(QInputSource.USER).withPrimaryKeys(keys);
      DeleteOutput output = new DeleteAction().execute(input);
      assertAll(
         () -> assertEquals(List.of(before.get(1)), rows()),
         () -> assertEquals(1, output.getDeletedRecordCount()),
         () -> assertTrue(CollectionUtils.nullSafeIsEmpty(output.getRecordsWithErrors()), JsonUtils.toJson(output)),
         () -> assertTrue(CollectionUtils.nullSafeIsEmpty(output.getRecordsWithWarnings())),
         () -> assertSelectionUnchanged(input, keys, null, "null"),
         () -> assertMetadataUnchanged());

      Serializable absent = "blobValue".equals(active.getPrimaryKeyField()) ? new byte[] { 7, 8, 9 } : new BigDecimal("9.0");
      DeleteOutput missing = new DeleteAction().execute(new DeleteInput(TABLE).withInputSource(QInputSource.USER).withPrimaryKey(absent));
      assertAll(
         () -> assertEquals(0, missing.getDeletedRecordCount()),
         () -> assertEquals(1, CollectionUtils.nonNullList(missing.getRecordsWithErrors()).size()),
         () -> assertTrue(missing.getRecordsWithErrors().get(0).getErrors().stream().anyMatch(NotFoundStatusMessage.class::isInstance)),
         () -> assertEquals(List.of(before.get(1)), rows()));
   }



   /*******************************************************************************
    ** Storage is the success oracle; a zero count or NotFound cannot fake success.
    *******************************************************************************/
   private void assertSuccessfulDelete(boolean filterSelection) throws Exception
   {
      List<Map<String, String>> before = rows();
      DeleteInput input = input(filterSelection);
      QQueryFilter filter = input.getQueryFilter();
      String filterJson = JsonUtils.toJson(filter);
      List<Serializable> originalKeys = input.getPrimaryKeys() == null ? null : new ArrayList<>(input.getPrimaryKeys());
      DeleteOutput output = new DeleteAction().execute(input);
      assertAll(
         () -> assertEquals(List.of(before.get(1)), rows()),
         () -> assertEquals(1, output.getDeletedRecordCount()),
         () -> assertTrue(CollectionUtils.nullSafeIsEmpty(output.getRecordsWithErrors()), JsonUtils.toJson(output)),
         () -> assertTrue(CollectionUtils.nullSafeIsEmpty(output.getRecordsWithWarnings())),
         () -> assertSelectionUnchanged(input, originalKeys, filter, filterJson),
         () -> assertMetadataUnchanged());
   }



   /*******************************************************************************
    ** This public internal selection step is checked before any Delete prefetch.
    ** The criterion uses name so coercion of a key predicate cannot hide failure.
    *******************************************************************************/
   private void assertNativeFilterKey() throws Exception
   {
      List<Map<String, String>> before = rows();
      DeleteInput input = input(true);
      QQueryFilter filter = input.getQueryFilter();
      String filterJson = JsonUtils.toJson(filter);
      List<Serializable> selected = DeleteAction.getPrimaryKeysFromQueryFilter(input);
      assertAll(
         () -> assertEquals(1, selected.size()),
         () -> assertTargetKey(selected.get(0)),
         () -> assertEquals(before, rows()),
         () -> assertSelectionUnchanged(input, null, filter, filterJson),
         () -> assertMetadataUnchanged());
   }



   /*******************************************************************************
    ** Byte contents and numeric value are intentional typed identity contracts.
    *******************************************************************************/
   private void assertTargetKey(Serializable key)
   {
      if("blobValue".equals(active.getPrimaryKeyField()))
      {
         assertArrayEquals(new byte[] { 1, 2, 3 }, assertInstanceOf(byte[].class, key));
      }
      else
      {
         assertEquals(0, new BigDecimal("2.0").compareTo(assertInstanceOf(BigDecimal.class, key)));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private DeleteInput input(boolean filterSelection)
   {
      DeleteInput input = new DeleteInput(TABLE).withInputSource(QInputSource.USER);
      if(filterSelection)
      {
         return input.withQueryFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Target")));
      }
      return input.withPrimaryKey("blobValue".equals(active.getPrimaryKeyField()) ? new byte[] { 1, 2, 3 } : new BigDecimal("2.0"));
   }



   /*******************************************************************************
    ** Original array references and decimal scales remain caller-owned even when
    ** identity comparison coalesces equivalent keys internally.
    *******************************************************************************/
   private void assertSelectionUnchanged(DeleteInput input, List<Serializable> originalKeys, QQueryFilter filter, String filterJson) throws Exception
   {
      assertSame(filter, input.getQueryFilter());
      assertEquals(filterJson, JsonUtils.toJson(input.getQueryFilter()));
      if(originalKeys == null)
      {
         assertNull(input.getPrimaryKeys());
      }
      else
      {
         assertEquals(originalKeys.size(), input.getPrimaryKeys().size());
         for(int i = 0; i < originalKeys.size(); i++)
         {
            assertSame(originalKeys.get(i), input.getPrimaryKeys().get(i));
            assertTargetKey(input.getPrimaryKeys().get(i));
         }
      }
   }



   /*******************************************************************************
    ** Replace a registered table through the supported copied-map setter. The
    ** hidden key leaves field sections so the metadata remains validator-valid.
    *******************************************************************************/
   private void useLogicalKey(String fieldName, boolean hidden, boolean heavy, boolean rejecting) throws Exception
   {
      canonical = instance.getTable(TABLE);
      canonicalJson = JsonUtils.toJson(canonical);
      active = canonical.clone();
      active.setPrimaryKeyField(fieldName);
      active.getField(fieldName).setIsHidden(hidden);
      active.getField(fieldName).setIsHeavy(heavy);
      if(hidden)
      {
         for(var section : active.getSections())
         {
            section.setFieldNames(section.getFieldNames().stream().filter(name -> !fieldName.equals(name)).toList());
         }
      }
      if(rejecting)
      {
         active.withCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(RejectEquivalentKey.class));
      }
      Map<String, QTableMetaData> tables = new LinkedHashMap<>(instance.getTables());
      tables.put(TABLE, active);
      instance.setTables(tables);
      new QInstanceValidator().revalidate(instance);
      activeJson = JsonUtils.toJson(active);
      assertMetadataUnchanged();
   }



   /*******************************************************************************
    ** Action flags cannot mutate either registered active metadata or its source.
    ** JDBC metadata independently confirms the physical key is still INTEGER id.
    *******************************************************************************/
   private void assertMetadataUnchanged() throws Exception
   {
      assertSame(active, instance.getTable(TABLE));
      assertEquals(activeJson, JsonUtils.toJson(active));
      assertEquals("id", canonical.getPrimaryKeyField());
      assertEquals(canonicalJson, JsonUtils.toJson(canonical));
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend()))
      {
         try(ResultSet keys = connection.getMetaData().getPrimaryKeys(null, null, "FIELD_LAB"))
         {
            assertTrue(keys.next());
            assertEquals("ID", keys.getString("COLUMN_NAME"));
            assertFalse(keys.next());
         }
         try(ResultSet columns = connection.getMetaData().getColumns(null, null, "FIELD_LAB", "ID"))
         {
            assertTrue(columns.next());
            assertEquals(Types.INTEGER, columns.getInt("DATA_TYPE"));
            assertFalse(columns.next());
         }
      }
   }



   /*******************************************************************************
    ** Restore only this fixture's rows between independent selection paths.
    *******************************************************************************/
   private void seedRows() throws SQLException
   {
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          Statement statement = connection.createStatement())
      {
         statement.executeUpdate("DELETE FROM field_lab");
         assertEquals(2, statement.executeUpdate("INSERT INTO field_lab(id,name,long_value,decimal_value,boolean_value,text_value,blob_value) VALUES "
            + "(1,'Target',7,2.0000,TRUE,'Original target',X'010203'),"
            + "(2,'Decoy',8,3.0000,TRUE,'Original decoy',X'040506')"));
      }
   }



   /*******************************************************************************
    ** Full native rows include actual binary bytes, not JDBC object descriptions.
    *******************************************************************************/
   private List<Map<String, String>> rows() throws SQLException
   {
      List<Map<String, String>> rows = new ArrayList<>();
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT * FROM field_lab ORDER BY id"))
      {
         while(result.next())
         {
            Map<String, String> row = new LinkedHashMap<>();
            for(int column = 1; column <= result.getMetaData().getColumnCount(); column++)
            {
               String value;
               if(result.getMetaData().getColumnType(column) == Types.BLOB)
               {
                  byte[] bytes = result.getBytes(column);
                  value = bytes == null ? null : Base64.getEncoder().encodeToString(bytes);
               }
               else
               {
                  value = result.getString(column);
               }
               row.put(result.getMetaData().getColumnLabel(column), value);
            }
            rows.add(row);
         }
      }
      return rows;
   }



   /*******************************************************************************
    ** A customizer may return new records with independently equivalent keys.
    *******************************************************************************/
   public static class RejectEquivalentKey implements TableCustomizerInterface
   {
      /*******************************************************************************
       ** Keep the validation record distinct from its original prefetch object.
       *******************************************************************************/
      @Override
      public List<QRecord> preDelete(DeleteInput input, List<QRecord> records, boolean isPreview)
      {
         String field = input.getTable().getPrimaryKeyField();
         List<QRecord> result = new ArrayList<>();
         for(QRecord record : records)
         {
            Serializable key = record.getValue(field);
            Serializable equivalent = key instanceof byte[] bytes ? bytes.clone() : ((BigDecimal) key).stripTrailingZeros();
            result.add(new QRecord().withValue(field, equivalent).withError(new BadInputStatusMessage("Keep this logical target")));
         }
         return result;
      }
   }



   /*******************************************************************************
    ** In-place array mutation also affects an ordinary QRecord shallow value copy.
    *******************************************************************************/
   public static class MutatePostDeleteBinaryKey implements TableCustomizerInterface
   {
      /*******************************************************************************
       ** Preserve the record and array references to exercise fallback isolation.
       *******************************************************************************/
      @Override
      public List<QRecord> postDelete(DeleteInput input, List<QRecord> records)
      {
         byte[] key = (byte[]) records.get(0).getValue("blobValue");
         key[0] = 9;
         return List.copyOf(records);
      }
   }
}
