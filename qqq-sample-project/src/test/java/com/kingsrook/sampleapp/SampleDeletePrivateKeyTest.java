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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.NotFoundStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.QWarningMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.SystemErrorStatusMessage;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Hidden and masked keys remain private while native Delete retains identity.
 ** PASSWORD is a logical key on the existing column, not a physical key change.
 ** This fixture never inspects the internal identity carrier.
 *******************************************************************************/
class SampleDeletePrivateKeyTest
{
   private static final String TABLE = "fieldLab";
   private static final Integer REJECTED = 867530901;
   private static final Integer WARNED = 867530902;
   private static final Integer PLAIN = 867530903;
   private static final Integer UNRELATED = 867530904;
   private static final Integer MISSING = 867530999;
   private static final String MASK = "************";
   private QInstance instance;
   private QTableMetaData canonical;
   private String canonicalJson;
   private QTableMetaData active;
   private String activeJson;
   private boolean passwordKey;
   private boolean constraintAdded;



   /*******************************************************************************
    ** Distinctive keys make raw identity in output/error text observable.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      QContext.init(instance, new QSession());
      resetCaptures();
      seedRows();
   }



   /*******************************************************************************
    ** The optional native constraint and captures are owned by this fixture.
    *******************************************************************************/
   @AfterEach
   void tearDown() throws Exception
   {
      try
      {
         if(constraintAdded)
         {
            try(Connection connection = connection(); Statement statement = connection.createStatement())
            {
               statement.executeUpdate("ALTER TABLE field_lab DROP CONSTRAINT private_delete_restrict");
            }
         }
      }
      finally
      {
         resetCaptures();
         QContext.clear();
         ConnectionManager.resetConnectionProviders();
      }
   }



   /*******************************************************************************
    ** Cloned immutable PRE_DELETE records retain hidden identities without keys
    ** appearing in the callback or error/warning snapshots.
    *******************************************************************************/
   @Test
   void testHiddenKeyImmutablePreDeleteBatchKeepsPrivateIdentities() throws Exception
   {
      configure(false, TableCustomizers.PRE_DELETE_RECORD, MixedPrivateClones.class);
      assertMixedPreDelete();
   }



   /*******************************************************************************
    ** Equal public password masks must not collapse three distinct stored keys.
    *******************************************************************************/
   @Test
   void testPasswordKeyImmutablePreDeleteBatchKeepsDistinctIdentities() throws Exception
   {
      configure(true, TableCustomizers.PRE_DELETE_RECORD, MixedPrivateClones.class);
      assertMixedPreDelete();
   }



   /*******************************************************************************
    ** POST_DELETE clones keep all warning identities even with omitted keys.
    *******************************************************************************/
   @Test
   void testHiddenKeyPostDeleteCloneWarningsStayPrivate() throws Exception
   {
      configure(false, TableCustomizers.POST_DELETE_RECORD, WarnPrivateClones.class);
      assertPostDeleteWarnings();
   }



   /*******************************************************************************
    ** POST_DELETE warning maps distinguish records whose visible keys all mask.
    *******************************************************************************/
   @Test
   void testPasswordKeyPostDeleteCloneWarningsStayPrivate() throws Exception
   {
      configure(true, TableCustomizers.POST_DELETE_RECORD, WarnPrivateClones.class);
      assertPostDeleteWarnings();
   }



   /*******************************************************************************
    ** Native and not-found errors retain their categories without repeating a
    ** hidden key in values, display values, record labels or native error text.
    *******************************************************************************/
   @Test
   void testHiddenKeyNativeAndMissingErrorsNeverPublishRawIdentity() throws Exception
   {
      configure(false, null, null);
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         assertEquals(1, statement.executeUpdate("UPDATE field_lab SET long_value=" + REJECTED + " WHERE id=" + UNRELATED));
         statement.executeUpdate("ALTER TABLE field_lab ADD CONSTRAINT private_delete_restrict FOREIGN KEY(long_value) REFERENCES field_lab(id) ON DELETE RESTRICT");
         constraintAdded = true;
      }
      List<Map<String, String>> before = rows();
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         SQLException failure = assertThrows(SQLException.class, () -> statement.executeUpdate("DELETE FROM field_lab WHERE id=" + REJECTED));
         assertEquals("23503", failure.getSQLState());
      }
      assertEquals(before, rows());
      assertAll(
         () ->
         {
            DeleteInput blockedInput = new DeleteInput(TABLE).withInputSource(QInputSource.USER).withPrimaryKey(REJECTED);
            DeleteOutput blocked = new DeleteAction().execute(blockedInput);
            assertAll(
               () -> assertEquals(before, rows()),
               () -> assertEquals(0, blocked.getDeletedRecordCount()),
               () -> assertEquals(1, CollectionUtils.nonNullList(blocked.getRecordsWithErrors()).size()),
               () -> assertTrue(blocked.getRecordsWithErrors().get(0).getErrors().stream().anyMatch(SystemErrorStatusMessage.class::isInstance)),
               () -> assertPrivateOutput(blocked),
               () -> assertEquals(List.of(REJECTED), blockedInput.getPrimaryKeys()));
         },
         () ->
         {
            DeleteInput missingInput = new DeleteInput(TABLE).withInputSource(QInputSource.USER).withPrimaryKey(MISSING);
            DeleteOutput missing = new DeleteAction().execute(missingInput);
            assertAll(
               () -> assertEquals(before, rows()),
               () -> assertEquals(0, missing.getDeletedRecordCount()),
               () -> assertEquals(1, CollectionUtils.nonNullList(missing.getRecordsWithErrors()).size()),
               () -> assertTrue(missing.getRecordsWithErrors().get(0).getErrors().stream().anyMatch(NotFoundStatusMessage.class::isInstance)),
               () -> assertPrivateOutput(missing),
               () -> assertEquals(List.of(MISSING), missingInput.getPrimaryKeys()));
         },
         () -> assertMetadataUnchanged());
   }



   /*******************************************************************************
    ** Internal hidden-key reads must not change ordinary public Query/Get flags.
    *******************************************************************************/
   @Test
   void testHiddenKeyDeleteLeavesOrdinaryQueryAndGetPrivate() throws Exception
   {
      configure(false, null, null);
      assertReadPrivacyAroundDelete();
   }



   /*******************************************************************************
    ** Internal password identity never changes public masking or display values.
    *******************************************************************************/
   @Test
   void testPasswordKeyDeleteLeavesOrdinaryQueryAndGetMasked() throws Exception
   {
      configure(true, null, null);
      assertReadPrivacyAroundDelete();
   }



   /*******************************************************************************
    ** Both setter and direct-map public-key replacement invalidate a captured
    ** hidden identity. A clone may not redirect DML to an unrelated stored row.
    *******************************************************************************/
   @Test
   void testChangedPublicKeyCloneCannotRedirectDeletion() throws Exception
   {
      configure(false, TableCustomizers.PRE_DELETE_RECORD, ChangePrivateKey.class);
      assertAll(List.of(false, true).stream().map(directMap -> (Executable) () ->
      {
         seedRows();
         ChangePrivateKey.directMap = directMap;
         List<Map<String, String>> before = rows();
         DeleteInput input = new DeleteInput(TABLE).withInputSource(QInputSource.USER).withPrimaryKey(REJECTED);
         assertAll(
            () -> assertThrows(QException.class, () -> new DeleteAction().execute(input)),
            () -> assertEquals(before, rows()),
            () -> assertEquals(List.of(REJECTED), input.getPrimaryKeys()),
            () -> assertNull(input.getQueryFilter()),
            () -> assertMetadataUnchanged());
      }));
   }



   /*******************************************************************************
    ** Fresh records without captured identity must identify an original target.
    ** An unkeyed error cannot silently fail to reject it, and a different key
    ** cannot stand in for it. Both cases fail before native mutation.
    *******************************************************************************/
   @Test
   void testFreshUnkeyedOrUnrelatedCustomizerRecordsRejectBeforeDeletion() throws Exception
   {
      configure(false, TableCustomizers.PRE_DELETE_RECORD, ReconstructPrivateKey.class);
      assertAll(List.of(false, true).stream().map(unrelatedKey -> (Executable) () ->
      {
         seedRows();
         ReconstructPrivateKey.unrelatedKey = unrelatedKey;
         List<Map<String, String>> before = rows();
         DeleteInput input = new DeleteInput(TABLE).withInputSource(QInputSource.USER).withPrimaryKey(REJECTED);
         assertAll(
            () -> assertThrows(QException.class, () -> new DeleteAction().execute(input)),
            () -> assertEquals(before, rows()),
            () -> assertEquals(List.of(REJECTED), input.getPrimaryKeys()),
            () -> assertNull(input.getQueryFilter()),
            () -> assertMetadataUnchanged());
      }));
   }



   /*******************************************************************************
    ** Malformed POST_DELETE identities cannot erase successful DML outcomes or
    ** inject unrelated errors. Catch warnings use the original private snapshot,
    ** including when the callback modifies the object it was originally passed.
    *******************************************************************************/
   @Test
   void testMalformedPostDeleteReplacementKeepsSuccessfulPrivateWarning() throws Exception
   {
      configure(false, TableCustomizers.POST_DELETE_RECORD, MalformedPostIdentity.class);
      assertAll(List.of("missing", "unrelated", "in-place").stream().map(mode -> (Executable) () ->
      {
         seedRows();
         MalformedPostIdentity.mode = mode;
         List<Map<String, String>> before = rows();
         DeleteInput input = new DeleteInput(TABLE).withInputSource(QInputSource.USER).withPrimaryKey(REJECTED);
         DeleteOutput output = new DeleteAction().execute(input);
         assertAll(mode,
            () -> assertEquals(List.of(before.get(1), before.get(2), before.get(3)), rows()),
            () -> assertEquals(1, output.getDeletedRecordCount()),
            () -> assertTrue(CollectionUtils.nullSafeIsEmpty(output.getRecordsWithErrors()), JsonUtils.toJson(output)),
            () -> assertEquals(List.of("Rejected target"), names(output.getRecordsWithWarnings())),
            () ->
            {
               QRecord warning = output.getRecordsWithWarnings().get(0);
               assertTrue(warning.getErrors().isEmpty(), warning.getErrorsAsString());
               assertTrue(warning.getWarnings().stream().anyMatch(value -> value.getMessage().startsWith("An error occurred after the delete:")));
            },
            () -> assertPrivateOutput(output),
            () -> assertEquals(List.of(REJECTED), input.getPrimaryKeys()),
            () -> assertNull(input.getQueryFilter()),
            () -> assertMetadataUnchanged());
      }));
   }



   /*******************************************************************************
    ** Names are public; neither this customizer nor the oracle needs private keys
    ** to partition errors and warnings. SQL independently proves the identities.
    *******************************************************************************/
   private void assertMixedPreDelete() throws Exception
   {
      List<Map<String, String>> before = rows();
      DeleteInput input = threeTargets();
      QQueryFilter filter = input.getQueryFilter();
      String filterJson = JsonUtils.toJson(filter);
      DeleteOutput output = new DeleteAction().execute(input);
      assertAll(
         () -> assertEquals(List.of(before.get(0), before.get(3)), rows()),
         () -> assertEquals(2, output.getDeletedRecordCount()),
         () -> assertEquals(List.of("Rejected target"), names(output.getRecordsWithErrors())),
         () -> assertTrue(output.getRecordsWithErrors().get(0).getErrors().stream().anyMatch(BadInputStatusMessage.class::isInstance)),
         () -> assertEquals(List.of("Warned target"), names(output.getRecordsWithWarnings())),
         () -> assertEquals(List.of("Reviewed private-key deletion"), output.getRecordsWithWarnings().get(0)
            .getWarnings().stream().map(QWarningMessage::getMessage).toList()),
         () -> assertEquals(List.of("Plain target", "Rejected target", "Warned target"), names(MixedPrivateClones.received)),
         () -> assertPrivateRecords(MixedPrivateClones.received),
         () -> assertPrivateOutput(output),
         () -> assertSame(filter, input.getQueryFilter()),
         () -> assertEquals(filterJson, JsonUtils.toJson(filter)),
         () -> assertNull(input.getPrimaryKeys()),
         () -> assertMetadataUnchanged());
   }



   /*******************************************************************************
    ** Every distinct stored row must retain its own post-delete warning entry.
    *******************************************************************************/
   private void assertPostDeleteWarnings() throws Exception
   {
      List<Map<String, String>> before = rows();
      DeleteOutput output = new DeleteAction().execute(threeTargets());
      List<String> expectedNames = List.of("Plain target", "Rejected target", "Warned target");
      assertAll(
         () -> assertEquals(List.of(before.get(3)), rows()),
         () -> assertEquals(3, output.getDeletedRecordCount()),
         () -> assertTrue(CollectionUtils.nullSafeIsEmpty(output.getRecordsWithErrors())),
         () -> assertEquals(expectedNames, names(output.getRecordsWithWarnings())),
         () -> assertEquals(expectedNames, names(WarnPrivateClones.received)),
         () -> assertTrue(output.getRecordsWithWarnings().stream().allMatch(record -> record.getWarnings().stream()
            .anyMatch(warning -> "Reviewed after private-key deletion".equals(warning.getMessage())))),
         () -> assertPrivateRecords(WarnPrivateClones.received),
         () -> assertPrivateOutput(output),
         () -> assertMetadataUnchanged());
   }



   /*******************************************************************************
    ** A real mutation separates the before/after public reads. Both reads select
    ** a surviving decoy and independently confirm the same physical database.
    *******************************************************************************/
   private void assertReadPrivacyAroundDelete() throws Exception
   {
      List<Map<String, String>> before = rows();
      assertOrdinaryReadPrivacy();
      assertEquals(before, rows());
      DeleteOutput deleted = new DeleteAction().execute(new DeleteInput(TABLE).withInputSource(QInputSource.USER).withPrimaryKey(key(PLAIN)));
      assertAll(
         () -> assertEquals(1, deleted.getDeletedRecordCount()),
         () -> assertTrue(CollectionUtils.nullSafeIsEmpty(deleted.getRecordsWithErrors())),
         () -> assertEquals(List.of(before.get(0), before.get(1), before.get(3)), rows()));
      assertOrdinaryReadPrivacy();
      assertEquals(List.of(before.get(0), before.get(1), before.get(3)), rows());
      assertMetadataUnchanged();
   }



   /*******************************************************************************
    ** These are ordinary public reads with normal default privacy settings.
    *******************************************************************************/
   private void assertOrdinaryReadPrivacy() throws Exception
   {
      List<QRecord> queried = new QueryAction().execute(new QueryInput(TABLE).withInputSource(QInputSource.USER)
         .withFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Warned target")))
         .withShouldGenerateDisplayValues(true)).getRecords();
      QRecord gotten = new GetAction().execute(new GetInput(TABLE).withInputSource(QInputSource.USER)
         .withPrimaryKey(key(WARNED)).withShouldGenerateDisplayValues(true)).getRecord();
      assertAll(
         () -> assertEquals(List.of("Warned target"), names(queried)),
         () -> assertNotNull(gotten),
         () -> assertEquals("Warned target", gotten.getValueString("name")),
         () -> assertPrivateRecords(queried),
         () ->
         {
            assertNotNull(gotten);
            assertPrivateRecords(List.of(gotten));
         });
      if(passwordKey)
      {
         assertEquals(MASK, queried.get(0).getDisplayValues().get("passwordValue"));
         assertEquals(MASK, gotten.getDisplayValues().get("passwordValue"));
      }
   }



   /*******************************************************************************
    ** Three selected rows have distinct native keys and an unrelated survivor.
    *******************************************************************************/
   private DeleteInput threeTargets()
   {
      return new DeleteInput(TABLE).withInputSource(QInputSource.USER)
         .withQueryFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.NOT_EQUALS, "Unrelated")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Serializable key(Integer id)
   {
      return passwordKey ? "private-delete-key-" + id : id;
   }



   /*******************************************************************************
    ** A cloned table changes only presentation/key metadata and the owned hook.
    ** Native INTEGER id remains the physical primary key in both variants.
    *******************************************************************************/
   private void configure(boolean usePasswordKey, TableCustomizers customizer, Class<?> implementation) throws Exception
   {
      passwordKey = usePasswordKey;
      canonical = instance.getTable(TABLE);
      canonicalJson = JsonUtils.toJson(canonical);
      active = canonical.clone();
      active.setPrimaryKeyField(passwordKey ? "passwordValue" : "id");
      if(!passwordKey)
      {
         active.getField("id").setIsHidden(true);
         for(QFieldSection section : active.getSections())
         {
            section.setFieldNames(section.getFieldNames().stream().filter(name -> !"id".equals(name)).toList());
         }
      }
      if(customizer != null)
      {
         active.withCustomizer(customizer, new QCodeReference(implementation));
      }
      Map<String, QTableMetaData> tables = new LinkedHashMap<>(instance.getTables());
      tables.put(TABLE, active);
      instance.setTables(tables);
      new QInstanceValidator().revalidate(instance);
      activeJson = JsonUtils.toJson(active);
      assertMetadataUnchanged();
   }



   /*******************************************************************************
    ** Records and messages must not contain private raw keys at any public path.
    *******************************************************************************/
   private void assertPrivateOutput(DeleteOutput output)
   {
      assertPrivateRecords(CollectionUtils.nonNullList(output.getRecordsWithErrors()));
      assertPrivateRecords(CollectionUtils.nonNullList(output.getRecordsWithWarnings()));
      assertNoPrivateKeyText(JsonUtils.toJson(output));
   }



   /*******************************************************************************
    ** Callback copies and public output have the same metadata privacy contract.
    *******************************************************************************/
   private void assertPrivateRecords(List<QRecord> records)
   {
      for(QRecord record : records)
      {
         if(passwordKey)
         {
            assertEquals(MASK, record.getValueString("passwordValue"));
            String display = CollectionUtils.nonNullMap(record.getDisplayValues()).get("passwordValue");
            assertTrue(display == null || MASK.equals(display));
         }
         else
         {
            assertFalse(record.getValues().containsKey("id"));
            assertFalse(CollectionUtils.nonNullMap(record.getDisplayValues()).containsKey("id"));
         }
      }
      assertNoPrivateKeyText(JsonUtils.toJson(records));
   }



   /*******************************************************************************
    ** Password variants still expose their unrelated ordinary INTEGER id field.
    ** Hidden-id variants prohibit even known request keys in response messages.
    *******************************************************************************/
   private void assertNoPrivateKeyText(String text)
   {
      assertFalse(text.contains("private-delete-key-"), text);
      if(!passwordKey)
      {
         for(Integer id : List.of(REJECTED, WARNED, PLAIN, UNRELATED, MISSING))
         {
            assertFalse(text.contains(id.toString()), text);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertMetadataUnchanged() throws Exception
   {
      assertSame(active, instance.getTable(TABLE));
      assertEquals(activeJson, JsonUtils.toJson(active));
      assertEquals("id", canonical.getPrimaryKeyField());
      assertEquals(canonicalJson, JsonUtils.toJson(canonical));
      try(Connection connection = connection())
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
    **
    *******************************************************************************/
   private static List<String> names(List<QRecord> records)
   {
      return CollectionUtils.nonNullList(records).stream().map(record -> record.getValueString("name"))
         .sorted(Comparator.nullsFirst(Comparator.naturalOrder())).toList();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void seedRows() throws Exception
   {
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         statement.executeUpdate("DELETE FROM field_lab");
         assertEquals(4, statement.executeUpdate("INSERT INTO field_lab(id,name,password_value,text_value) VALUES "
            + "(" + REJECTED + ",'Rejected target','private-delete-key-" + REJECTED + "','Original rejected'),"
            + "(" + WARNED + ",'Warned target','private-delete-key-" + WARNED + "','Original warned'),"
            + "(" + PLAIN + ",'Plain target','private-delete-key-" + PLAIN + "','Original plain'),"
            + "(" + UNRELATED + ",'Unrelated','private-delete-key-" + UNRELATED + "','Original unrelated')"));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Connection connection() throws Exception
   {
      return ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
   }



   /*******************************************************************************
    ** Native snapshots contain every field and unrelated row; BLOB is SQL NULL.
    *******************************************************************************/
   private List<Map<String, String>> rows() throws Exception
   {
      List<Map<String, String>> rows = new ArrayList<>();
      try(Connection connection = connection(); Statement statement = connection.createStatement();
          ResultSet result = statement.executeQuery("SELECT * FROM field_lab ORDER BY id"))
      {
         while(result.next())
         {
            Map<String, String> row = new LinkedHashMap<>();
            for(int column = 1; column <= result.getMetaData().getColumnCount(); column++)
            {
               row.put(result.getMetaData().getColumnLabel(column), result.getString(column));
            }
            rows.add(row);
         }
      }
      return rows;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void resetCaptures()
   {
      MixedPrivateClones.received = List.of();
      WarnPrivateClones.received = List.of();
      ChangePrivateKey.directMap = false;
      ReconstructPrivateKey.unrelatedKey = false;
      MalformedPostIdentity.mode = null;
   }



   /*******************************************************************************
    ** Clone records into an immutable list; choose outcomes using visible names.
    *******************************************************************************/
   public static class MixedPrivateClones implements TableCustomizerInterface
   {
      private static List<QRecord> received = List.of();



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> preDelete(DeleteInput input, List<QRecord> records, boolean isPreview)
      {
         received = records.stream().map(QRecord::new).toList();
         return records.stream().map(record ->
         {
            QRecord copy = new QRecord(record);
            if("Rejected target".equals(copy.getValueString("name")))
            {
               copy.addError(new BadInputStatusMessage("Rejected private-key deletion"));
            }
            else if("Warned target".equals(copy.getValueString("name")))
            {
               copy.addWarning(new QWarningMessage("Reviewed private-key deletion"));
            }
            return copy;
         }).toList();
      }
   }



   /*******************************************************************************
    ** Every copied post-delete record gets its own visible-name warning result.
    *******************************************************************************/
   public static class WarnPrivateClones implements TableCustomizerInterface
   {
      private static List<QRecord> received = List.of();



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postDelete(DeleteInput input, List<QRecord> records)
      {
         received = records.stream().map(QRecord::new).toList();
         return records.stream().map(record -> new QRecord(record).withWarning(new QWarningMessage("Reviewed after private-key deletion"))).toList();
      }
   }



   /*******************************************************************************
    ** The public map must not silently override an internal captured identity.
    *******************************************************************************/
   public static class ChangePrivateKey implements TableCustomizerInterface
   {
      private static boolean directMap;



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> preDelete(DeleteInput input, List<QRecord> records, boolean isPreview)
      {
         return records.stream().map(record ->
         {
            QRecord copy = new QRecord(record);
            if(directMap)
            {
               copy.getValues().put("id", UNRELATED);
            }
            else
            {
               copy.withValue("id", UNRELATED);
            }
            return copy;
         }).toList();
      }
   }



   /*******************************************************************************
    ** New QRecords deliberately do not preserve the original identity.
    *******************************************************************************/
   public static class ReconstructPrivateKey implements TableCustomizerInterface
   {
      private static boolean unrelatedKey;



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> preDelete(DeleteInput input, List<QRecord> records, boolean isPreview)
      {
         return records.stream().map(record ->
         {
            QRecord fresh = new QRecord().withValue("name", record.getValueString("name"))
               .withError(new BadInputStatusMessage("Reject this original target"));
            if(unrelatedKey)
            {
               fresh.withValue("id", UNRELATED);
            }
            return fresh;
         }).toList();
      }
   }



   /*******************************************************************************
    ** Malformed callback records deliberately contain an error that must never
    ** be merged into the successful DeleteOutput or its preserved warning copy.
    *******************************************************************************/
   public static class MalformedPostIdentity implements TableCustomizerInterface
   {
      private static String mode;



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postDelete(DeleteInput input, List<QRecord> records)
      {
         if("in-place".equals(mode))
         {
            records.get(0).getValues().put("id", UNRELATED);
            records.get(0).addError(new BadInputStatusMessage("Spurious post-delete error"));
            return records;
         }
         QRecord fresh = new QRecord().withValue("name", "Unrelated")
            .withError(new BadInputStatusMessage("Spurious post-delete error"));
         if("unrelated".equals(mode))
         {
            fresh.withValue("id", UNRELATED);
         }
         return List.of(fresh);
      }
   }
}
