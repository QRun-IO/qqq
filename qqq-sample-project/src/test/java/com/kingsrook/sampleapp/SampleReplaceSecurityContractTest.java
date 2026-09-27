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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.ReplaceAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.replace.ReplaceInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.replace.ReplaceOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DynamicDefaultValueBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSTransaction;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Bounded native Replace matching visibility and discovered-identity contracts.
 ** BLOB/PASSWORD keys are logical metadata keys on existing FieldLab columns;
 ** the physical INTEGER primary key and all native constraints stay unchanged.
 *******************************************************************************/
class SampleReplaceSecurityContractTest
{
   private static final String TABLE = "fieldLab";
   private static final int TARGET = 981770101;
   private QInstance instance;
   private QTableMetaData canonical;
   private String canonicalJson;
   private QTableMetaData active;
   private String activeJson;



   /*******************************************************************************
    ** Own behavior sets before changing defaults; compare complete native rows.
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
    ** READ filtering prevents matching an excluded owner. Its existing canonical
    ** name constraint then rejects an INSERT; no hidden owner may be overwritten.
    *******************************************************************************/
   @Test
   void testReadExcludedOwnerCannotBeMatchedForUpdate() throws Exception
   {
      configureReadLock(20);
      assertEquals(List.of("Decoy"), publicRows().stream().map(r -> r.getValueString("name")).toList());
      assertRejectedUnchanged(input(record("Target", "Forbidden replacement")));
   }



   /*******************************************************************************
    ** Changing only the session READ grant makes the same real owner matchable.
    *******************************************************************************/
   @Test
   void testReadAllowedOwnerRetainsItsIdentity() throws Exception
   {
      configureReadLock(10);
      assertEquals(List.of("Target"), publicRows().stream().map(r -> r.getValueString("name")).toList());
      List<Map<String, String>> expected = rows();
      expected.get(0).put("HTML_VALUE", "Allowed replacement");
      QRecord candidate = record("Target", "Allowed replacement");
      ReplaceOutput output;
      try
      {
         output = new ReplaceAction().execute(input(candidate));
      }
      finally
      {
         assertEquals(expected, rows());
      }
      assertSuccess(output, 0, 1);
      assertAll(
         () -> assertEquals(TARGET, candidate.getValueInteger("id")),
         () -> assertEquals(expected, rows()),
         this::assertMetaDataUnchanged);
   }



   /*******************************************************************************
    ** A USER-removed matching field is an invalid selector, not an empty match.
    *******************************************************************************/
   @Test
   void testUserRemovedMatchingFieldRejectsBeforeMutation() throws Exception
   {
      instance.addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(PrivateSelector.class));
      ReplaceInput input = input(record("Candidate", "Forbidden replacement").withValue("unchangedValue", "selector"))
         .withKey(new UniqueKey("unchangedValue"));
      input.setInputSource(QInputSource.USER);
      assertRejectedUnchanged(input);
   }



   /*******************************************************************************
    ** The same supplemental customizer leaves SYSTEM matching available.
    *******************************************************************************/
   @Test
   void testSystemCanMatchTheFieldRemovedOnlyForUser() throws Exception
   {
      instance.addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(PrivateSelector.class));
      List<Map<String, String>> expected = rows();
      expected.get(0).put("NAME", "Candidate");
      expected.get(0).put("HTML_VALUE", "Allowed replacement");
      QRecord candidate = record("Candidate", "Allowed replacement").withValue("unchangedValue", "selector");
      ReplaceInput input = input(candidate).withKey(new UniqueKey("unchangedValue"));
      input.setInputSource(QInputSource.SYSTEM);
      ReplaceOutput output;
      try
      {
         output = new ReplaceAction().execute(input);
      }
      finally
      {
         assertEquals(expected, rows());
      }
      assertSuccess(output, 0, 1);
      assertAll(
         () -> assertEquals(TARGET, candidate.getValueInteger("id")),
         () -> assertEquals(expected, rows()),
         this::assertMetaDataUnchanged);
   }



   /*******************************************************************************
    ** Native NULL and the literal empty string are distinct. Test both directions
    ** with single and composite keys, each on freshly seeded rows.
    *******************************************************************************/
   @Test
   void testNullAndEmptyStringMatchingRemainDistinct()
   {
      assertAll(
         () -> assertNullOrEmpty(false, false),
         () -> assertNullOrEmpty(true, false),
         () -> assertNullOrEmpty(false, true),
         () -> assertNullOrEmpty(true, true));
   }



   /*******************************************************************************
    ** A discovered hidden id is private; an explicitly submitted id is caller
    ** data. Also exercise generated-id publication without granting hidden READ.
    *******************************************************************************/
   @Test
   void testHiddenPrimaryKeyDiscoveryAndPublicationStayPrivate() throws Exception
   {
      configurePrivateKey("id");
      assertAll(
         () -> assertPrivateMatch("id", false, false),
         () -> assertPrivateMatch("id", true, false),
         () -> assertPrivateMatch("id", true, true),
         this::assertHiddenInsertedKeyPublication);
   }



   /*******************************************************************************
    ** Heavy BLOB keys remain absent when discovered, while caller-supplied bytes
    ** retain the existing Update behavior. Ordinary public Query stays light.
    *******************************************************************************/
   @Test
   void testHeavyPrimaryKeyDiscoveryStaysPrivate() throws Exception
   {
      configurePrivateKey("blobValue");
      assertAll(
         () -> assertPrivateMatch("blobValue", false, false),
         () -> assertPrivateMatch("blobValue", true, false),
         () -> assertPrivateMatch("blobValue", true, true));
   }



   /*******************************************************************************
    ** A discovered PASSWORD key is masked in caller/output records; raw keys are
    ** retained only for the distinct explicitly supplied caller-value control.
    *******************************************************************************/
   @Test
   void testPasswordPrimaryKeyDiscoveryStaysMasked() throws Exception
   {
      configurePrivateKey("passwordValue");
      assertAll(
         () -> assertPrivateMatch("passwordValue", false, false),
         () -> assertPrivateMatch("passwordValue", true, false),
         () -> assertPrivateMatch("passwordValue", true, true));
   }



   /*******************************************************************************
    ** Reusing an already masked public record must not turn its private identity
    ** into a newly supplied raw caller key. Exercise both actual public objects.
    *******************************************************************************/
   @Test
   void testReusingMaskedPasswordCallerAndOutputCannotRevealPrivateKey() throws Exception
   {
      configurePrivateKey("passwordValue");
      assertAll(
         () -> assertMaskedPasswordReuse(false),
         () -> assertMaskedPasswordReuse(true));
   }



   /*******************************************************************************
    ** Matching and private key retrieval both see an uncommitted renamed owner
    ** and changed BLOB key. The caller remains responsible for rollback/commit.
    *******************************************************************************/
   @Test
   void testPrivateKeyRetrievalUsesCallerTransactionAndLeavesItOwned() throws Exception
   {
      configurePrivateKey("blobValue");
      List<Map<String, String>> before = rows();
      try(RDBMSTransaction transaction = new RDBMSTransaction(connection()))
      {
         Connection connection = transaction.getConnection();
         try(Statement statement = connection.createStatement())
         {
            assertEquals(1, statement.executeUpdate("UPDATE field_lab SET name='Pending match',blob_value=X'0D0E0F' WHERE id=" + TARGET));
         }
         List<Map<String, String>> expected = rows(connection);
         expected.get(0).put("HTML_VALUE", "Pending replacement");
         QRecord candidate = record("Pending match", "Pending replacement");
         ReplaceInput input = input(candidate).withTransaction(transaction);
         ReplaceOutput output;
         try
         {
            output = new ReplaceAction().execute(input);
         }
         finally
         {
            assertAll(
               () -> assertEquals(expected, rows(connection)),
               () -> assertEquals(before, rows()));
         }
         assertSuccess(output, 0, 1);
         assertAll(
            () -> assertEquals(expected, rows(connection)),
            () -> assertEquals(before, rows()),
            () -> assertPrivateRecord(candidate, "blobValue"),
            () -> assertPrivateRecord(output.getUpdateOutput().getRecords().get(0), "blobValue"),
            () -> assertCallerOwns(input, transaction, connection));
         transaction.rollback();
         assertAll(
            () -> assertEquals(before, rows(connection)),
            () -> assertEquals(before, rows()),
            () -> assertCallerOwns(input, transaction, connection));
      }
      assertPublicPrivacy("blobValue");
      assertMetaDataUnchanged();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void configureReadLock(int owner) throws Exception
   {
      instance.addSecurityKeyType(new QSecurityKeyType().withName("replaceReadOwner"));
      active.withRecordSecurityLock(new RecordSecurityLock().withFieldName("longValue").withSecurityKeyType("replaceReadOwner")
         .withLockScope(RecordSecurityLock.LockScope.READ));
      QContext.setQSession(new QSession().withSecurityKeyValue("replaceReadOwner", owner));
      freezeMetaData();
   }



   /*******************************************************************************
    ** unchangedValue already opts out of whitespace/case normalization, making a
    ** literal empty string a meaningful native control beside the null owner.
    *******************************************************************************/
   private void assertNullOrEmpty(boolean empty, boolean composite) throws Exception
   {
      seed();
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         assertEquals(2, statement.executeUpdate("UPDATE field_lab SET unchanged_value=CASE WHEN id=" + TARGET + " THEN NULL ELSE '' END,long_value=10 WHERE id IN (" + TARGET + "," + (TARGET + 1) + ")"));
         try(ResultSet nativeValues = statement.executeQuery("SELECT unchanged_value FROM field_lab WHERE id IN (" + TARGET + "," + (TARGET + 1) + ") ORDER BY id"))
         {
            assertTrue(nativeValues.next());
            assertNull(nativeValues.getString(1));
            assertTrue(nativeValues.next());
            assertEquals("", nativeValues.getString(1));
            assertFalse(nativeValues.next());
         }
      }
      List<Map<String, String>> expected = rows();
      int selected = empty ? 1 : 0;
      expected.get(selected).put("NAME", "Selected replacement");
      expected.get(selected).put("HTML_VALUE", "Null or empty replacement");
      QRecord candidate = record("Selected replacement", "Null or empty replacement")
         .withValue("unchangedValue", empty ? "" : null).withValue("longValue", 10L);
      ReplaceInput input = input(candidate).withKey(composite ? new UniqueKey("unchangedValue", "longValue") : new UniqueKey("unchangedValue"))
         .withAllowNullKeyValuesToEqual(true);
      ReplaceOutput output;
      try
      {
         output = new ReplaceAction().execute(input);
      }
      finally
      {
         assertEquals(expected, rows());
      }
      assertSuccess(output, 0, 1);
      assertAll(
         () -> assertEquals(TARGET + selected, candidate.getValueInteger("id")),
         () -> assertEquals(expected, rows()),
         this::assertMetaDataUnchanged);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void configurePrivateKey(String field) throws Exception
   {
      active.setPrimaryKeyField(field);
      if("id".equals(field))
      {
         active.getField(field).setIsHidden(true);
         for(QFieldSection section : active.getSections())
         {
            section.setFieldNames(section.getFieldNames().stream().filter(name -> !field.equals(name)).toList());
         }
      }
      else if("blobValue".equals(field))
      {
         active.getField(field).setIsHeavy(true);
      }
      freezeMetaData();
   }



   /*******************************************************************************
    ** Matching supplies only visible name unless the explicit caller-key control
    ** is requested. No reflection or access to internal identity carriers is used.
    *******************************************************************************/
   private void assertPrivateMatch(String field, boolean publish, boolean callerSupplied) throws Exception
   {
      seed();
      assertPublicPrivacy(field);
      List<Map<String, String>> expected = rows();
      expected.get(0).put("HTML_VALUE", "Private-key replacement");
      QRecord candidate = record("Target", "Private-key replacement");
      Serializable callerKey = key(field);
      if(callerSupplied)
      {
         candidate.setValue(field, callerKey);
      }
      ReplaceInput input = input(candidate).withSetPrimaryKeyInInsertedRecords(publish);
      ReplaceOutput output;
      try
      {
         output = new ReplaceAction().execute(input);
      }
      finally
      {
         assertEquals(expected, rows());
      }
      assertSuccess(output, 0, 1);
      QRecord returned = output.getUpdateOutput().getRecords().get(0);
      assertAll(
         () -> assertEquals(expected, rows()),
         () ->
         {
            if(callerSupplied)
            {
               if(callerKey instanceof byte[] bytes)
               {
                  assertArrayEquals(bytes, (byte[]) candidate.getValue(field));
                  assertArrayEquals(bytes, (byte[]) returned.getValue(field));
               }
               else
               {
                  assertEquals(callerKey, candidate.getValue(field));
                  assertEquals(callerKey, returned.getValue(field));
               }
            }
            else
            {
               assertPrivateRecord(candidate, field);
               assertPrivateRecord(returned, field);
               assertPrivateJson(JsonUtils.toJson(output), field);
            }
         },
         () -> assertPublicPrivacy(field),
         this::assertMetaDataUnchanged);
      assertEquals(expected, rows());
   }



   /*******************************************************************************
    ** Each branch starts from a record with no supplied password and proves the
    ** first call's native success/privacy before resubmitting the same object.
    *******************************************************************************/
   private void assertMaskedPasswordReuse(boolean useReturnedRecord) throws Exception
   {
      seed();
      assertPublicPrivacy("passwordValue");
      List<Map<String, String>> expected = rows();
      expected.get(0).put("HTML_VALUE", "First replacement");
      QRecord original = record("Target", "First replacement");
      assertFalse(original.getValues().containsKey("passwordValue"));
      ReplaceOutput first = new ReplaceAction().execute(input(original).withSetPrimaryKeyInInsertedRecords(true));
      assertSuccess(first, 0, 1);
      QRecord returned = first.getUpdateOutput().getRecords().get(0);
      assertAll(
         () -> assertEquals(expected, rows()),
         () -> assertPrivateRecord(original, "passwordValue"),
         () -> assertPrivateRecord(returned, "passwordValue"),
         () -> assertPrivateJson(JsonUtils.toJson(first), "passwordValue"));

      QRecord reused = useReturnedRecord ? returned : original;
      reused.setValue("htmlValue", "Second replacement");
      assertEquals("************", reused.getValueString("passwordValue"));
      expected.get(0).put("HTML_VALUE", "Second replacement");
      ReplaceInput secondInput = input(reused).withSetPrimaryKeyInInsertedRecords(true);
      ReplaceOutput second;
      try
      {
         second = new ReplaceAction().execute(secondInput);
      }
      finally
      {
         assertEquals(expected, rows());
      }
      assertSuccess(second, 0, 1);
      assertAll(
         () -> assertSame(reused, secondInput.getRecords().get(0)),
         () -> assertPrivateRecord(original, "passwordValue"),
         () -> assertPrivateRecord(reused, "passwordValue"),
         () -> assertPrivateRecord(second.getUpdateOutput().getRecords().get(0), "passwordValue"),
         () -> assertPrivateJson(JsonUtils.toJson(secondInput.getRecords()), "passwordValue"),
         () -> assertPrivateJson(JsonUtils.toJson(second), "passwordValue"),
         () -> assertPublicPrivacy("passwordValue"),
         () -> assertEquals(expected, rows()),
         this::assertMetaDataUnchanged);
   }



   /*******************************************************************************
    ** Hidden generated ids can be used internally without being published by the
    ** convenience flag. Native name readback proves both actual inserted identity
    ** and the absence of any accidental duplicate/retargeted row.
    *******************************************************************************/
   private void assertHiddenInsertedKeyPublication() throws Exception
   {
      seed();
      List<Map<String, String>> before = rows();
      QRecord candidate = record("Added", "Added payload");
      ReplaceInput input = input(candidate).withSetPrimaryKeyInInsertedRecords(true);
      ReplaceOutput output = new ReplaceAction().execute(input);
      assertSuccess(output, 1, 0);
      List<Map<String, String>> actual = rows();
      assertEquals(4, actual.size());
      Map<String, String> added = actual.get(3);
      assertTrue(Integer.parseInt(added.get("ID")) > TARGET + 2);
      List<Map<String, String>> expected = new ArrayList<>(before);
      Map<String, String> newRow = new LinkedHashMap<>();
      before.get(0).keySet().forEach(name -> newRow.put(name, null));
      newRow.put("ID", added.get("ID"));
      newRow.put("NAME", "Added");
      newRow.put("HTML_VALUE", "Added payload");
      newRow.put("BOOLEAN_VALUE", "TRUE");
      expected.add(newRow);
      assertAll(
         () -> assertEquals(expected, actual),
         () -> assertPrivateRecord(candidate, "id"),
         () -> assertPrivateRecord(output.getInsertOutput().getRecords().get(0), "id"),
         () -> assertFalse(JsonUtils.toJson(output).contains(added.get("ID"))),
         () -> assertPublicPrivacy("id"),
         this::assertMetaDataUnchanged);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Serializable key(String field)
   {
      return switch(field)
      {
         case "id" -> TARGET;
         case "blobValue" -> new byte[] { 1, 2, 3 };
         case "passwordValue" -> "replace-private-key-first";
         default -> throw new IllegalArgumentException(field);
      };
   }



   /*******************************************************************************
    ** Normal Query still obeys its unchanged masking/hidden/heavy defaults.
    *******************************************************************************/
   private void assertPublicPrivacy(String field) throws Exception
   {
      List<QRecord> records = publicRows();
      assertEquals(rows().size(), records.size());
      for(QRecord record : records)
      {
         assertPrivateRecord(record, field);
      }
      assertPrivateJson(JsonUtils.toJson(records), field);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<QRecord> publicRows() throws QException
   {
      return new QueryAction().execute(new QueryInput(TABLE)).getRecords();
   }



   /*******************************************************************************
    ** Explicit caller values are tested separately; these records have no grant
    ** to receive a discovered raw PK merely because a write used it internally.
    *******************************************************************************/
   private void assertPrivateRecord(QRecord record, String field)
   {
      if("passwordValue".equals(field))
      {
         assertEquals("************", record.getValueString(field));
         String display = CollectionUtils.nonNullMap(record.getDisplayValues()).get(field);
         assertTrue(display == null || "************".equals(display));
      }
      else
      {
         assertFalse(record.getValues().containsKey(field), JsonUtils.toJson(record));
         assertFalse(CollectionUtils.nonNullMap(record.getDisplayValues()).containsKey(field));
      }
      assertPrivateJson(JsonUtils.toJson(record), field);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertPrivateJson(String json, String field)
   {
      if("id".equals(field))
      {
         for(int index = 0; index < 3; index++)
         {
            assertFalse(json.contains(Integer.toString(TARGET + index)), json);
         }
      }
      else if("blobValue".equals(field))
      {
         assertFalse(json.contains("AQID"), json);
         assertFalse(json.contains("BAUG"), json);
         assertFalse(json.contains("BwgJ"), json);
         assertFalse(json.contains("DQ4P"), json);
      }
      else
      {
         assertFalse(json.contains("replace-private-key-"), json);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private ReplaceInput input(QRecord... records)
   {
      ReplaceInput input = new ReplaceInput().withKey(new UniqueKey("name")).withRecords(List.of(records))
         .withPerformDeletes(false);
      input.setTableName(TABLE);
      return input;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord record(String name, String html)
   {
      return new QRecord().withValue("name", name).withValue("htmlValue", html);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertRejectedUnchanged(ReplaceInput input) throws Exception
   {
      List<Map<String, String>> before = rows();
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
         () -> assertTrue(rejected, "Replace did not reject an inaccessible or invalid match"),
         () -> assertEquals(before, rows()),
         this::assertMetaDataUnchanged);
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
   private void assertSuccess(ReplaceOutput output, int inserts, int updates)
   {
      assertNotNull(output);
      assertAll(
         () -> assertEquals(inserts, output.getInsertOutput().getRecords().size()),
         () -> assertEquals(updates, output.getUpdateOutput().getRecords().size()),
         () -> assertFalse(hasErrors(output), JsonUtils.toJson(output)),
         () -> assertNull(output.getDeleteOutput()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertCallerOwns(ReplaceInput input, RDBMSTransaction transaction, Connection connection) throws Exception
   {
      assertSame(transaction, input.getTransaction());
      assertSame(connection, transaction.getConnection());
      assertFalse(connection.isClosed());
      assertFalse(connection.getAutoCommit());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void seed() throws Exception
   {
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         statement.executeUpdate("DELETE FROM field_lab");
         assertEquals(3, statement.executeUpdate("INSERT INTO field_lab(id,name,long_value,unchanged_value,password_value,blob_value,html_value) VALUES "
            + "(981770101,'Target',10,'selector','replace-private-key-first',X'010203','Original first'),"
            + "(981770102,'Decoy',20,'decoy','replace-private-key-second',X'040506','Original second'),"
            + "(981770103,'Outside',30,'outside','replace-private-key-third',X'070809','Original third')"));
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
    **
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
    **
    *******************************************************************************/
   public static class PrivateSelector implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         if(!TABLE.equals(input.getTableName()) || input.getInputSource() != QInputSource.USER)
         {
            return input.getTable();
         }
         QTableMetaData result = input.getTable().clone();
         result.getFields().remove("unchangedValue");
         return result;
      }
   }
}
