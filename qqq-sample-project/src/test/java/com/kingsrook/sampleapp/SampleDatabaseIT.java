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


import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.postgres.model.metadata.PostgreSQLBackendMetaData;
import com.kingsrook.qqq.backend.module.postgres.model.metadata.PostgreSQLTableBackendDetails;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSTransaction;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import com.kingsrook.sampleapp.metadata.FieldLabTableMetaDataProducer;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/*******************************************************************************
 ** Run Field Lab temporal data through QQQ and real disposable SQL databases.
 ** These containers contain only synthetic sample data and stop after each test.
 *******************************************************************************/
class SampleDatabaseIT
{
   private static final String TABLE = FieldLabTableMetaDataProducer.NAME;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMySqlTemporalValuesAcrossHostTimeZones() throws Exception
   {
      try(MySQLContainer database = new MySQLContainer("mysql:8.4")
         .withDatabaseName("qqq_sample").withUsername("sample").withPassword("sample-fixture-only"))
      {
         database.start();
         RDBMSBackendMetaData backend = new RDBMSBackendMetaData().withVendor("mysql")
            .withName(SampleMetaDataProvider.RDBMS_BACKEND_NAME)
            .withHostName(database.getHost()).withPort(database.getFirstMappedPort())
            .withDatabaseName(database.getDatabaseName()).withUsername(database.getUsername()).withPassword(database.getPassword())
            .withQueriesForNewConnections(List.of("SET time_zone = '+00:00'"));
         exerciseTemporalValues(backend, "mysql");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPostgresTemporalValuesAcrossHostTimeZones() throws Exception
   {
      try(PostgreSQLContainer database = new PostgreSQLContainer("postgres:17-alpine")
         .withDatabaseName("qqq_sample").withUsername("sample").withPassword("sample-fixture-only"))
      {
         database.start();
         RDBMSBackendMetaData backend = new PostgreSQLBackendMetaData()
            .withName(SampleMetaDataProvider.RDBMS_BACKEND_NAME)
            .withHostName(database.getHost()).withPort(database.getFirstMappedPort())
            .withDatabaseName(database.getDatabaseName()).withUsername(database.getUsername()).withPassword(database.getPassword())
            .withQueriesForNewConnections(List.of("SET TIME ZONE 'UTC'"));
         exerciseTemporalValues(backend, "postgres");
      }
   }



   /*******************************************************************************
    ** Native MySQL precision, collation and concurrent UNIQUE enforcement.
    *******************************************************************************/
   @Test
   void testMySqlNativeConstraintsCoverRoundingCollationAndConcurrentWrites() throws Exception
   {
      try(MySQLContainer database = new MySQLContainer("mysql:8.4")
         .withDatabaseName("qqq_sample").withUsername("sample").withPassword("sample-fixture-only"))
      {
         database.start();
         RDBMSBackendMetaData backend = new RDBMSBackendMetaData().withVendor("mysql")
            .withName(SampleMetaDataProvider.RDBMS_BACKEND_NAME)
            .withHostName(database.getHost()).withPort(database.getFirstMappedPort())
            .withDatabaseName(database.getDatabaseName()).withUsername(database.getUsername()).withPassword(database.getPassword())
            .withQueriesForNewConnections(List.of("SET time_zone = '+00:00'"));
         SampleStorageConstraintTest.exerciseNativeConstraints(backend, "mysql");
      }
   }



   /*******************************************************************************
    ** Native PostgreSQL precision, default case-sensitive collation and UNIQUE races.
    *******************************************************************************/
   @Test
   void testPostgresNativeConstraintsCoverRoundingCollationAndConcurrentWrites() throws Exception
   {
      try(PostgreSQLContainer database = new PostgreSQLContainer("postgres:17-alpine")
         .withDatabaseName("qqq_sample").withUsername("sample").withPassword("sample-fixture-only"))
      {
         database.start();
         RDBMSBackendMetaData backend = new PostgreSQLBackendMetaData()
            .withName(SampleMetaDataProvider.RDBMS_BACKEND_NAME)
            .withHostName(database.getHost()).withPort(database.getFirstMappedPort())
            .withDatabaseName(database.getDatabaseName()).withUsername(database.getUsername()).withPassword(database.getPassword())
            .withQueriesForNewConnections(List.of("SET TIME ZONE 'UTC'"));
         SampleStorageConstraintTest.exerciseNativeConstraints(backend, "postgres");
      }
   }



   /*******************************************************************************
    ** Canonical three-level relationships and native MySQL key generation modes.
    *******************************************************************************/
   @Test
   void testMySqlCanonicalAssociationsAndInsertPrimaryKeys() throws Exception
   {
      try(MySQLContainer database = new MySQLContainer("mysql:8.4")
         .withDatabaseName("qqq_sample").withUsername("sample").withPassword("sample-fixture-only"))
      {
         database.start();
         RDBMSBackendMetaData backend = new RDBMSBackendMetaData().withVendor("mysql")
            .withName(SampleMetaDataProvider.RDBMS_BACKEND_NAME)
            .withHostName(database.getHost()).withPort(database.getFirstMappedPort())
            .withDatabaseName(database.getDatabaseName()).withUsername(database.getUsername()).withPassword(database.getPassword())
            .withQueriesForNewConnections(List.of("SET time_zone = '+00:00'"));
         exerciseCanonicalAssociations(backend, "mysql");
         exerciseNativeInsertKeys(backend, "mysql");
      }
   }



   /*******************************************************************************
    ** PostgreSQL uses its own identity and sequence semantics for the same graph.
    *******************************************************************************/
   @Test
   void testPostgresCanonicalAssociationsAndInsertPrimaryKeys() throws Exception
   {
      try(PostgreSQLContainer database = new PostgreSQLContainer("postgres:17-alpine")
         .withDatabaseName("qqq_sample").withUsername("sample").withPassword("sample-fixture-only"))
      {
         database.start();
         RDBMSBackendMetaData backend = new PostgreSQLBackendMetaData()
            .withName(SampleMetaDataProvider.RDBMS_BACKEND_NAME)
            .withHostName(database.getHost()).withPort(database.getFirstMappedPort())
            .withDatabaseName(database.getDatabaseName()).withUsername(database.getUsername()).withPassword(database.getPassword())
            .withQueriesForNewConnections(List.of("SET TIME ZONE 'UTC'"));
         exerciseCanonicalAssociations(backend, "postgres");
         exerciseNativeInsertKeys(backend, "postgres");
      }
   }



   /*******************************************************************************
    ** The vendor fixture mirrors the runnable H2 Person/pets/notes metadata.
    *******************************************************************************/
   private void exerciseCanonicalAssociations(RDBMSBackendMetaData backend, String vendor) throws Exception
   {
      try
      {
         ConnectionManager.resetConnectionProviders();
         QInstance instance = SampleMetaDataProvider.defineTestInstance();
         instance.getBackends().put(backend.getName(), backend);
         if(vendor.equals("postgres"))
         {
            for(String table : List.of("person", "pet", "petNote"))
            {
               instance.getTable(table).setBackendDetails(new PostgreSQLTableBackendDetails().withTableName(table.equals("petNote") ? "pet_note" : table));
            }
         }
         QContext.init(instance, new QSession());
         try(Connection connection = ConnectionManager.getConnection(backend);
             Statement statement = connection.createStatement();
             InputStream schema = SampleDatabaseIT.class.getResourceAsStream("/database/associations-" + vendor + ".sql"))
         {
            assertNotNull(schema);
            for(String command : new String(schema.readAllBytes(), StandardCharsets.UTF_8).split(";"))
            {
               if(!command.isBlank())
               {
                  statement.execute(command);
               }
            }
         }
         List<List<String>> originalPeople = nativeRows(backend, "SELECT * FROM person ORDER BY id");
         List<List<String>> originalPets = nativeRows(backend, "SELECT * FROM pet ORDER BY id");
         List<List<String>> originalNotes = nativeRows(backend, "SELECT * FROM pet_note ORDER BY id");
         exerciseQueryPagination(backend);
         QRecord seeded = new GetAction().execute(new GetInput("person").withPrimaryKey(1).withIncludeAssociations(true)).getRecord();
         assertEquals(4, seeded.getAssociatedRecords().get("pets").size());
         QRecord seededPet = seeded.getAssociatedRecords().get("pets").stream().filter(record -> record.getValueInteger("id").equals(1)).findFirst().orElseThrow();
         assertEquals("Target note", seededPet.getAssociatedRecords().get("notes").get(0).getValueString("note"));
         QRecord inserted = new InsertAction().execute(new InsertInput("person").withRecord(new QRecord()
            .withValue("firstName", "Native").withValue("lastName", "Graph").withValue("email", "native@example.invalid")
            .withAssociatedRecords("pets", List.of(new QRecord().withValue("name", "Native pet").withValue("speciesId", 1)
               .withAssociatedRecords("notes", List.of(new QRecord().withValue("note", "Native note"))))))).getRecords().get(0);
         assertTrue(inserted.getErrors().isEmpty(), String.valueOf(inserted.getErrors()));
         int personId = inserted.getValueInteger("id");
         QRecord pet = inserted.getAssociatedRecords().get("pets").get(0);
         QRecord note = pet.getAssociatedRecords().get("notes").get(0);
         assertTrue(pet.getErrors().isEmpty(), String.valueOf(pet.getErrors()));
         assertTrue(note.getErrors().isEmpty(), String.valueOf(note.getErrors()));
         int petId = pet.getValueInteger("id");
         int noteId = note.getValueInteger("id");
         assertTrue(personId > 5 && petId > 6 && noteId > 2);
         assertEquals(List.of(List.of(Integer.toString(noteId), Integer.toString(petId), Integer.toString(personId), "Native note")),
            nativeRows(backend, "SELECT n.id,n.pet_id,p.person_id,n.note FROM pet_note n JOIN pet p ON p.id=n.pet_id WHERE n.id=" + noteId));
         QRecord expanded = new GetAction().execute(new GetInput("person").withPrimaryKey(personId).withIncludeAssociations(true)).getRecord();
         assertEquals(noteId, expanded.getAssociatedRecords().get("pets").get(0).getAssociatedRecords().get("notes").get(0).getValueInteger("id"));
         QRecord childPatch = new QRecord().withValue("id", petId).withValue("name", "Updated native pet");
         UpdateInput update = new UpdateInput("person").withRecord(new QRecord().withValue("id", personId).withAssociatedRecords("pets", List.of(childPatch)));
         QRecord omitted = new UpdateAction().execute(update).getRecords().get(0);
         assertTrue(omitted.getErrors().isEmpty(), String.valueOf(omitted.getErrors()));
         assertEquals(List.of(List.of("Native note")), nativeRows(backend, "SELECT note FROM pet_note WHERE id=" + noteId));
         childPatch.withAssociatedRecords("notes", List.of());
         QRecord emptied = new UpdateAction().execute(update).getRecords().get(0);
         assertTrue(emptied.getErrors().isEmpty(), String.valueOf(emptied.getErrors()));
         assertEquals(originalNotes, nativeRows(backend, "SELECT * FROM pet_note ORDER BY id"));
         assertEquals(originalPeople, nativeRows(backend, "SELECT * FROM person WHERE id<>" + personId + " ORDER BY id"));
         assertEquals(originalPets, nativeRows(backend, "SELECT * FROM pet WHERE id<>" + petId + " ORDER BY id"));
      }
      finally
      {
         QContext.clear();
         ConnectionManager.resetConnectionProviders();
      }
   }



   /*******************************************************************************
    ** Pagination preserves native ordered populations on each owned SQL vendor.
    *******************************************************************************/
   private void exerciseQueryPagination(RDBMSBackendMetaData backend) throws Exception
   {
      List<List<String>> original = nativeRows(backend, "SELECT id,first_name FROM person ORDER BY id");
      assertEquals(List.of("1", "2", "3", "4", "5"), original.stream().map(row -> row.get(0)).toList());
      List<Executable> cases = new ArrayList<>();
      for(Integer limit : List.of(0, 1, 3, 10))
      {
         for(Integer skip : Arrays.asList(null, 0, 1, 3, 5, 10))
         {
            cases.add(() ->
            {
               int start = Math.min(skip == null ? 0 : skip, original.size());
               int end = limit == null ? original.size() : Math.min(start + limit, original.size());
               QueryInput input = new QueryInput("person").withFieldNamesToInclude(Set.of("id", "firstName"))
                  .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id")).withLimit(limit).withSkip(skip));
               List<List<String>> actual = new QueryAction().execute(input).getRecords().stream()
                  .map(record -> List.of(record.getValueString("id"), record.getValueString("firstName"))).toList();
               assertEquals(original.subList(start, end), actual, "limit=" + limit + ", skip=" + skip);
            });
         }
      }
      assertAll(cases);
      assertEquals(original, nativeRows(backend, "SELECT id,first_name FROM person ORDER BY id"));
   }



   /*******************************************************************************
    ** Native key fixtures vary storage, without adding generation metadata guesses.
    *******************************************************************************/
   private void exerciseNativeInsertKeys(RDBMSBackendMetaData backend, String vendor) throws Exception
   {
      try
      {
         ConnectionManager.resetConnectionProviders();
         QInstance instance = SampleMetaDataProvider.defineTestInstance();
         instance.getBackends().put(backend.getName(), backend);
         QContext.init(instance, new QSession());
         String identity = vendor.equals("mysql") ? "INTEGER AUTO_INCREMENT" : "INTEGER GENERATED BY DEFAULT AS IDENTITY";
         nativeSql(backend, "CREATE TABLE sample_manual_key(business_key VARCHAR(40) PRIMARY KEY, payload VARCHAR(80))");
         nativeSql(backend, "CREATE TABLE sample_manual_numeric_key(record_key INTEGER PRIMARY KEY, payload VARCHAR(80))");
         nativeSql(backend, "CREATE TABLE sample_generated_key(record_key " + identity + " PRIMARY KEY, id VARCHAR(40), payload VARCHAR(80))");
         nativeSql(backend, "CREATE TABLE sample_only_key(record_key " + identity + " PRIMARY KEY)");
         QTableMetaData manual = nativeKeyTable(backend, vendor, "manualKey", "sample_manual_key", "id", "business_key", QFieldType.STRING)
            .withField(new QFieldMetaData("payload", QFieldType.STRING));
         QTableMetaData generated = nativeKeyTable(backend, vendor, "generatedKey", "sample_generated_key", "recordKey", "record_key", QFieldType.INTEGER)
            .withField(new QFieldMetaData("id", QFieldType.STRING)).withField(new QFieldMetaData("payload", QFieldType.STRING));
         QTableMetaData onlyKey = nativeKeyTable(backend, vendor, "onlyKey", "sample_only_key", "key", "record_key", QFieldType.INTEGER);
         QTableMetaData manualNumeric = nativeKeyTable(backend, vendor, "manualNumericKey", "sample_manual_numeric_key", "recordKey", "record_key", QFieldType.INTEGER)
            .withField(new QFieldMetaData("payload", QFieldType.STRING));
         instance.addTable(manual);
         instance.addTable(generated);
         instance.addTable(onlyKey);
         instance.addTable(manualNumeric);
         new QInstanceValidator().revalidate(instance);
         QRecord owner = new InsertAction().execute(new InsertInput("manualKey")
            .withRecord(new QRecord().withValue("id", "NATURAL/A#1").withValue("payload", "Manual owner"))).getRecords().get(0);
         assertTrue(owner.getErrors().isEmpty(), String.valueOf(owner.getErrors()));
         assertEquals("NATURAL/A#1", owner.getValueString("id"));
         List<List<String>> manualBefore = nativeRows(backend, "SELECT * FROM sample_manual_key ORDER BY business_key");
         assertEquals(List.of(List.of("NATURAL/A#1", "Manual owner")), manualBefore);
         assertThrows(QException.class, () -> new InsertAction().execute(new InsertInput("manualKey")
            .withRecord(new QRecord().withValue("payload", "Missing manual key"))));
         assertEquals(manualBefore, nativeRows(backend, "SELECT * FROM sample_manual_key ORDER BY business_key"));
         assertThrows(QException.class, () -> new InsertAction().execute(new InsertInput("manualKey")
            .withRecord(new QRecord().withValue("id", "NATURAL/A#1").withValue("payload", "Overwrite attempt"))));
         assertEquals(manualBefore, nativeRows(backend, "SELECT * FROM sample_manual_key ORDER BY business_key"));
         QRecord emptyNaturalKey = new InsertAction().executeForRecord(new InsertInput("manualKey")
            .withRecord(new QRecord().withValue("id", "").withValue("payload", "Empty natural key")));
         assertTrue(emptyNaturalKey.getErrors().isEmpty());
         assertEquals("", emptyNaturalKey.getValueString("id"));
         assertEquals(List.of(List.of("", "Empty natural key"), List.of("NATURAL/A#1", "Manual owner")),
            nativeRows(backend, "SELECT * FROM sample_manual_key ORDER BY business_key"));
         List<QRecord> mixed = List.of(new QRecord().withValue("id", "external-1").withValue("payload", "Generated first"),
            new QRecord().withValue("recordKey", 41).withValue("id", "external-2").withValue("payload", "Manual middle"),
            new QRecord().withValue("id", "external-3").withValue("payload", "Generated last"),
            new QRecord().withValue("recordKey", 0).withValue("id", "external-4").withValue("payload", "Zero numeric key"),
            new QRecord().withValue("recordKey", "").withValue("id", "external-5").withValue("payload", "Blank generated key"),
            new QRecord().withValue("recordKey", " \t ").withValue("id", "external-6").withValue("payload", "Whitespace generated key"));
         InsertOutput output = new InsertAction().execute(new InsertInput("generatedKey").withRecords(mixed));
         assertEquals(6, output.getRecords().size());
         for(int i = 0; i < mixed.size(); i++)
         {
            QRecord result = output.getRecords().get(i);
            assertTrue(result.getErrors().isEmpty(), String.valueOf(result.getErrors()));
            assertNotNull(result.getValueInteger("recordKey"));
            assertEquals(mixed.get(i).getValueString("payload"), result.getValueString("payload"));
            assertEquals(List.of(List.of(result.getValueString("recordKey"), mixed.get(i).getValueString("id"), mixed.get(i).getValueString("payload"))),
               nativeRows(backend, "SELECT record_key,id,payload FROM sample_generated_key WHERE id='external-" + (i + 1) + "'"));
         }
         assertEquals(41, output.getRecords().get(1).getValueInteger("recordKey"));
         assertEquals(6, output.getRecords().stream().map(record -> record.getValueInteger("recordKey")).distinct().count());
         assertEquals(6, nativeRows(backend, "SELECT * FROM sample_generated_key").size());
         InsertOutput keysOnly = new InsertAction().execute(new InsertInput("onlyKey").withRecords(List.of(new QRecord(), new QRecord().withValue("key", ""))));
         assertEquals(2, keysOnly.getRecords().size());
         for(QRecord result : keysOnly.getRecords())
         {
            assertTrue(result.getErrors().isEmpty(), String.valueOf(result.getErrors()));
            assertNotNull(result.getValueInteger("key"));
            assertEquals(List.of(List.of(result.getValueString("key"))), nativeRows(backend, "SELECT record_key FROM sample_only_key WHERE record_key=" + result.getValueInteger("key")));
         }
         assertEquals(2, keysOnly.getRecords().stream().map(record -> record.getValueInteger("key")).distinct().count());
         assertEquals(2, nativeRows(backend, "SELECT * FROM sample_only_key").size());
         QRecord manualZero = new InsertAction().executeForRecord(new InsertInput("manualNumericKey")
            .withRecord(new QRecord().withValue("recordKey", 0).withValue("payload", "Manual zero")));
         assertTrue(manualZero.getErrors().isEmpty());
         assertEquals(0, manualZero.getValueInteger("recordKey"));
         assertEquals(List.of(List.of("0", "Manual zero")), nativeRows(backend, "SELECT record_key,payload FROM sample_manual_numeric_key"));
         if(vendor.equals("mysql"))
         {
            assertMySqlZeroWithNativeMode(backend);
         }
      }
      finally
      {
         QContext.clear();
         ConnectionManager.resetConnectionProviders();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertMySqlZeroWithNativeMode(RDBMSBackendMetaData backend) throws Exception
   {
      List<List<String>> before = nativeRows(backend, "SELECT record_key FROM sample_only_key ORDER BY record_key");
      try(RDBMSTransaction transaction = new RDBMSTransaction(ConnectionManager.getConnection(backend)))
      {
         Connection connection = transaction.getConnection();
         String originalMode;
         try(Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT @@SESSION.sql_mode"))
         {
            assertTrue(result.next());
            originalMode = result.getString(1);
         }
         try
         {
            try(Statement statement = connection.createStatement())
            {
               statement.execute("SET SESSION sql_mode = CONCAT(@@SESSION.sql_mode, ',NO_AUTO_VALUE_ON_ZERO')");
            }
            QRecord zero = new InsertAction().executeForRecord(new InsertInput("onlyKey").withTransaction(transaction)
               .withRecord(new QRecord().withValue("key", "0.00")));
            assertTrue(zero.getErrors().isEmpty());
            assertEquals(0, zero.getValueInteger("key"));
            assertFalse(connection.isClosed());
            assertEquals(before, nativeRows(backend, "SELECT record_key FROM sample_only_key ORDER BY record_key"));
            transaction.commit();
            assertEquals(List.of(List.of("0")), nativeRows(backend, "SELECT record_key FROM sample_only_key WHERE record_key=0"));
            assertEquals(before, nativeRows(backend, "SELECT record_key FROM sample_only_key WHERE record_key<>0 ORDER BY record_key"));
         }
         finally
         {
            try(PreparedStatement statement = connection.prepareStatement("SET SESSION sql_mode = ?"))
            {
               statement.setString(1, originalMode);
               statement.execute();
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QTableMetaData nativeKeyTable(RDBMSBackendMetaData backend, String vendor, String name, String physical, String key, String column, QFieldType type)
   {
      return new QTableMetaData().withName(name).withBackendName(backend.getName()).withPrimaryKeyField(key)
         .withBackendDetails(vendor.equals("postgres") ? new PostgreSQLTableBackendDetails().withTableName(physical) : new RDBMSTableBackendDetails().withTableName(physical))
         .withField(new QFieldMetaData(key, type).withBackendName(column));
   }



   /*******************************************************************************
    ** These statements touch only fixture tables in this method's owned container.
    *******************************************************************************/
   private void nativeSql(RDBMSBackendMetaData backend, String sql) throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(backend); Statement statement = connection.createStatement())
      {
         statement.execute(sql);
      }
   }



   /*******************************************************************************
    ** Native storage is independent from framework output identities and labels.
    *******************************************************************************/
   private List<List<String>> nativeRows(RDBMSBackendMetaData backend, String sql) throws Exception
   {
      List<List<String>> rows = new ArrayList<>();
      try(Connection connection = ConnectionManager.getConnection(backend);
          Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql))
      {
         while(result.next())
         {
            List<String> row = new ArrayList<>();
            for(int i = 1; i <= result.getMetaData().getColumnCount(); i++)
            {
               row.add(result.getString(i));
            }
            rows.add(row);
         }
      }
      return rows;
   }



   /*******************************************************************************
    ** Real blocked PostgreSQL counts must time out or cancel, then recover.
    *******************************************************************************/
   @Test
   void testPostgresCountTimeoutAndCancellation() throws Exception
   {
      try(PostgreSQLContainer database = new PostgreSQLContainer("postgres:17-alpine")
         .withDatabaseName("qqq_sample").withUsername("sample").withPassword("sample-fixture-only"))
      {
         database.start();
         RDBMSBackendMetaData backend = new PostgreSQLBackendMetaData()
            .withName(SampleMetaDataProvider.RDBMS_BACKEND_NAME)
            .withHostName(database.getHost()).withPort(database.getFirstMappedPort())
            .withDatabaseName(database.getDatabaseName()).withUsername(database.getUsername()).withPassword(database.getPassword())
            .withQueriesForNewConnections(List.of("SET TIME ZONE 'UTC'", "SET application_name = 'qqq-sample-count'"));
         try
         {
            ConnectionManager.resetConnectionProviders();
            QInstance instance = SampleMetaDataProvider.defineTestInstance();
            instance.getBackends().put(backend.getName(), backend);
            instance.getTable(TABLE).setBackendDetails(new PostgreSQLTableBackendDetails().withTableName("field_lab"));
            QContext.init(instance, new QSession());
            try(Connection connection = ConnectionManager.getConnection(backend);
                Statement statement = connection.createStatement();
                InputStream schema = SampleDatabaseIT.class.getResourceAsStream("/database/field-lab-postgres.sql"))
            {
               assertNotNull(schema);
               statement.execute(new String(schema.readAllBytes(), StandardCharsets.UTF_8));
               statement.executeUpdate("INSERT INTO field_lab(name) VALUES ('count-recovery')");
            }
            exerciseBlockedPostgresCount(instance, backend, true);
            exerciseBlockedPostgresCount(instance, backend, false);
         }
         finally
         {
            QContext.clear();
            ConnectionManager.resetConnectionProviders();
         }
      }
   }



   /*******************************************************************************
    ** Hold a table lock until the driver has ended the count; release on any failure.
    *******************************************************************************/
   private void exerciseBlockedPostgresCount(QInstance instance, RDBMSBackendMetaData backend, boolean timeout) throws Exception
   {
      ExecutorService worker = Executors.newSingleThreadExecutor();
      CountAction action = new CountAction();
      try(Connection lock = ConnectionManager.getConnection(backend);
          Connection monitor = ConnectionManager.getConnection(backend))
      {
         lock.setAutoCommit(false);
         try(Statement statement = lock.createStatement())
         {
            statement.execute("LOCK TABLE field_lab IN ACCESS EXCLUSIVE MODE");
            int lockPid;
            try(ResultSet result = statement.executeQuery("SELECT pg_backend_pid()"))
            {
               assertTrue(result.next());
               lockPid = result.getInt(1);
            }
            Future<CountOutput> count = worker.submit(() ->
            {
               QContext.init(instance, new QSession());
               try
               {
                  return action.execute(new CountInput(TABLE).withTimeoutSeconds(timeout ? 3 : null));
               }
               finally
               {
                  QContext.clear();
               }
            });
            int countPid = awaitBlockedCount(monitor, lockPid);
            if(!timeout)
            {
               action.cancel();
            }
            ExecutionException failure = assertThrows(ExecutionException.class, () -> count.get(10, TimeUnit.SECONDS));
            QUserFacingException cause = assertInstanceOf(QUserFacingException.class, failure.getCause());
            assertEquals(timeout ? "Count timed out." : "Count was cancelled.", cause.getMessage());
            try(PreparedStatement waiting = monitor.prepareStatement("SELECT 1 FROM pg_locks WHERE pid = ? AND NOT granted"))
            {
               waiting.setInt(1, countPid);
               try(ResultSet result = waiting.executeQuery())
               {
                  assertFalse(result.next(), "The completed count must leave no blocked PostgreSQL request");
               }
            }
         }
         finally
         {
            lock.rollback();
         }
      }
      finally
      {
         worker.shutdownNow();
         assertTrue(worker.awaitTermination(10, TimeUnit.SECONDS), "Count worker must terminate after releasing its owned lock");
      }
      assertEquals(1, CountAction.execute(TABLE, null), "A fresh count must recover after " + (timeout ? "timeout" : "cancellation"));
   }



   /*******************************************************************************
    ** Synchronize on the real driver query waiting for this scenario's table lock.
    *******************************************************************************/
   private int awaitBlockedCount(Connection monitor, int lockPid) throws Exception
   {
      String sql = "SELECT activity.pid FROM pg_stat_activity activity JOIN pg_locks locks USING (pid) "
         + "WHERE locks.relation = 'field_lab'::regclass AND NOT locks.granted "
         + "AND activity.wait_event_type = 'Lock' AND activity.application_name = 'qqq-sample-count' "
         + "AND activity.query ILIKE 'SELECT COUNT(%' AND ? = ANY(pg_blocking_pids(activity.pid))";
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
      try(PreparedStatement statement = monitor.prepareStatement(sql))
      {
         statement.setQueryTimeout(2);
         statement.setInt(1, lockPid);
         while(System.nanoTime() < deadline)
         {
            try(ResultSet result = statement.executeQuery())
            {
               if(result.next())
               {
                  return result.getInt(1);
               }
            }
            Thread.sleep(25);
         }
      }
      return fail("PostgreSQL never observed the Count query blocked by the owned table lock");
   }



   /*******************************************************************************
    ** Driver timeout and explicit Query cancellation fail distinctly and release native work.
    *******************************************************************************/
   @Test
   void testPostgresQueryTimeoutAndCancellation() throws Exception
   {
      try(PostgreSQLContainer database = new PostgreSQLContainer("postgres:17-alpine")
         .withDatabaseName("qqq_sample").withUsername("sample").withPassword("sample-fixture-only"))
      {
         database.start();
         RDBMSBackendMetaData backend = new PostgreSQLBackendMetaData()
            .withName(SampleMetaDataProvider.RDBMS_BACKEND_NAME)
            .withHostName(database.getHost()).withPort(database.getFirstMappedPort())
            .withDatabaseName(database.getDatabaseName()).withUsername(database.getUsername()).withPassword(database.getPassword())
            .withQueriesForNewConnections(List.of("SET TIME ZONE 'UTC'", "SET application_name = 'qqq-sample-query'"));
         try
         {
            ConnectionManager.resetConnectionProviders();
            QInstance instance = SampleMetaDataProvider.defineTestInstance();
            instance.getBackends().put(backend.getName(), backend);
            instance.getTable(TABLE).setBackendDetails(new PostgreSQLTableBackendDetails().withTableName("field_lab"));
            QContext.init(instance, new QSession());
            try(Connection connection = ConnectionManager.getConnection(backend);
                Statement statement = connection.createStatement();
                InputStream schema = SampleDatabaseIT.class.getResourceAsStream("/database/field-lab-postgres.sql"))
            {
               assertNotNull(schema);
               statement.execute(new String(schema.readAllBytes(), StandardCharsets.UTF_8));
               assertEquals(2, statement.executeUpdate("INSERT INTO field_lab(name) VALUES ('query-recovery-one'),('query-recovery-two')"));
            }
            List<List<String>> expected = nativeRows(backend, "SELECT id,name FROM field_lab ORDER BY id");
            assertEquals(List.of(List.of("1", "query-recovery-one"), List.of("2", "query-recovery-two")), expected);
            exerciseBlockedPostgresQuery(instance, backend, expected, 3, false);
            exerciseBlockedPostgresQuery(instance, backend, expected, null, true);
            for(Integer timeoutSeconds : Arrays.asList(null, 0, -1))
            {
               exerciseBlockedPostgresQuery(instance, backend, expected, timeoutSeconds, false);
            }
         }
         finally
         {
            QContext.clear();
            ConnectionManager.resetConnectionProviders();
         }
      }
   }



   /*******************************************************************************
    ** Observe the actual blocked driver before cancellation; release owned locks on every path.
    *******************************************************************************/
   private void exerciseBlockedPostgresQuery(QInstance instance, RDBMSBackendMetaData backend, List<List<String>> expected, Integer timeoutSeconds, boolean cancel) throws Exception
   {
      ExecutorService worker = Executors.newSingleThreadExecutor();
      QueryAction action = new QueryAction();
      Future<QueryOutput> query = null;
      try(Connection lock = ConnectionManager.getConnection(backend);
          Connection monitor = ConnectionManager.getConnection(backend))
      {
         lock.setAutoCommit(false);
         try(Statement statement = lock.createStatement())
         {
            statement.setQueryTimeout(5);
            statement.execute("LOCK TABLE field_lab IN ACCESS EXCLUSIVE MODE");
            int lockPid;
            try(ResultSet result = statement.executeQuery("SELECT pg_backend_pid()"))
            {
               assertTrue(result.next());
               lockPid = result.getInt(1);
            }
            query = worker.submit(() ->
            {
               try
               {
                  QContext.init(instance, new QSession());
                  return action.execute(new QueryInput(TABLE).withTimeoutSeconds(timeoutSeconds)
                     .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id"))));
               }
               finally
               {
                  QContext.clear();
               }
            });
            Future<QueryOutput> activeQuery = query;
            int queryPid = awaitBlockedQuery(monitor, lockPid);
            if(cancel)
            {
               action.cancel();
            }
            if(cancel || (timeoutSeconds != null && timeoutSeconds > 0))
            {
               ExecutionException failure = assertThrows(ExecutionException.class, () -> activeQuery.get(10, TimeUnit.SECONDS));
               QUserFacingException cause = assertInstanceOf(QUserFacingException.class, failure.getCause());
               assertEquals(cancel ? "Query was cancelled." : "Query timed out.", cause.getMessage());
            }
            else
            {
               assertThrows(TimeoutException.class, () -> activeQuery.get(150, TimeUnit.MILLISECONDS), "No-timeout query must remain blocked until its owned lock is released");
               lock.rollback();
               assertEquals(expected, queryRows(activeQuery.get(10, TimeUnit.SECONDS)));
            }
            try(PreparedStatement waiting = monitor.prepareStatement("SELECT 1 FROM pg_locks WHERE pid = ? AND NOT granted"))
            {
               waiting.setQueryTimeout(2);
               waiting.setInt(1, queryPid);
               try(ResultSet result = waiting.executeQuery())
               {
                  assertFalse(result.next(), "The completed query must leave no blocked PostgreSQL request");
               }
            }
            try(PreparedStatement active = monitor.prepareStatement("SELECT 1 FROM pg_stat_activity WHERE pid = ? AND state = 'active'"))
            {
               active.setQueryTimeout(2);
               active.setInt(1, queryPid);
               long idleDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
               while(true)
               {
                  try(ResultSet result = active.executeQuery())
                  {
                     if(!result.next())
                     {
                        break;
                     }
                  }
                  assertTrue(System.nanoTime() < idleDeadline, "The completed query must leave no active PostgreSQL statement");
                  Thread.sleep(25);
               }
            }
         }
         finally
         {
            lock.rollback();
         }
      }
      finally
      {
         if(query != null)
         {
            query.cancel(true);
         }
         worker.shutdownNow();
         assertTrue(worker.awaitTermination(10, TimeUnit.SECONDS), "Query worker must terminate after releasing its owned lock");
      }
      assertEquals(expected, queryRows(new QueryAction().execute(new QueryInput(TABLE)
         .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id"))))), "A fresh query must recover after the blocked request");
      assertEquals(expected, nativeRows(backend, "SELECT id,name FROM field_lab ORDER BY id"));
   }



   /*******************************************************************************
    ** Select only the worker blocked by this scenario's lock and application name.
    *******************************************************************************/
   private int awaitBlockedQuery(Connection monitor, int lockPid) throws Exception
   {
      String sql = "SELECT activity.pid FROM pg_stat_activity activity JOIN pg_locks locks USING (pid) "
         + "WHERE locks.relation = 'field_lab'::regclass AND NOT locks.granted "
         + "AND activity.wait_event_type = 'Lock' AND activity.application_name = 'qqq-sample-query' "
         + "AND activity.query ILIKE 'SELECT %' AND ? = ANY(pg_blocking_pids(activity.pid))";
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
      try(PreparedStatement statement = monitor.prepareStatement(sql))
      {
         statement.setQueryTimeout(2);
         statement.setInt(1, lockPid);
         while(System.nanoTime() < deadline)
         {
            try(ResultSet result = statement.executeQuery())
            {
               if(result.next())
               {
                  return result.getInt(1);
               }
            }
            Thread.sleep(25);
         }
      }
      return fail("PostgreSQL never observed the Query blocked by the owned table lock");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<List<String>> queryRows(QueryOutput output)
   {
      return output.getRecords().stream().map(record -> List.of(record.getValueString("id"), record.getValueString("name"))).toList();
   }



   /*******************************************************************************
    ** Independent SQL reads ensure matching write/read mistakes cannot cancel out.
    *******************************************************************************/
   private void exerciseTemporalValues(RDBMSBackendMetaData backend, String vendor) throws Exception
   {
      TimeZone original = TimeZone.getDefault();
      try
      {
         ConnectionManager.resetConnectionProviders();
         QInstance instance = SampleMetaDataProvider.defineTestInstance();
         instance.getBackends().put(backend.getName(), backend);
         if(vendor.equals("postgres"))
         {
            instance.getTable(TABLE).setBackendDetails(new PostgreSQLTableBackendDetails().withTableName("field_lab"));
         }
         QContext.init(instance, new QSession());
         try(Connection connection = ConnectionManager.getConnection(backend);
             Statement statement = connection.createStatement();
             InputStream schema = SampleDatabaseIT.class.getResourceAsStream("/database/field-lab-" + vendor + ".sql"))
         {
            assertTrue(connection.getMetaData().getDatabaseProductName().toLowerCase(java.util.Locale.ROOT).startsWith(vendor));
            assertNotNull(schema);
            statement.execute(new String(schema.readAllBytes(), StandardCharsets.UTF_8));
         }
         for(String zone : List.of("UTC", "America/Chicago", "Asia/Tokyo"))
         {
            TimeZone.setDefault(TimeZone.getTimeZone(zone));
            ConnectionManager.resetConnectionProviders();
            LocalDate date = LocalDate.of(2024, 2, 29);
            LocalTime time = LocalTime.of(23, 59, 58);
            Instant instant = Instant.parse("2026-03-08T07:59:59Z");
            QRecord inserted = InsertAction.executeForRecords(new InsertInput().withTableName(TABLE)
               .withRecord(new QRecord().withValue("name", vendor + "-" + zone)
                  .withValue("dateValue", date).withValue("timeValue", time).withValue("dateTimeValue", instant))).get(0);
            assertTrue(inserted.getErrors().isEmpty(), String.valueOf(inserted.getErrors()));
            QRecord stored = GetAction.execute(TABLE, inserted.getValueInteger("id"));
            assertEquals(date, stored.getValueLocalDate("dateValue"), zone);
            assertEquals(time, stored.getValueLocalTime("timeValue"), zone);
            assertEquals(instant, stored.getValueInstant("dateTimeValue"), zone);
            try(Connection connection = ConnectionManager.getConnection(backend);
                PreparedStatement statement = connection.prepareStatement("SELECT date_value, time_value, date_time_value FROM field_lab WHERE id = ?"))
            {
               statement.setInt(1, inserted.getValueInteger("id"));
               try(ResultSet values = statement.executeQuery())
               {
                  assertTrue(values.next());
                  assertEquals("2024-02-29", values.getString(1), zone);
                  assertEquals("23:59:58", values.getString(2), zone);
                  assertTrue(values.getString(3).startsWith("2026-03-08 07:59:59"), zone + ": " + values.getString(3));
                  assertFalse(values.next());
               }
            }
            QRecord updated = UpdateAction.executeForRecords(new UpdateInput().withTableName(TABLE)
               .withRecord(new QRecord().withValue("id", inserted.getValueInteger("id"))
                  .withValue("dateValue", LocalDate.of(2024, 3, 1)).withValue("timeValue", LocalTime.MIDNIGHT))).get(0);
            assertTrue(updated.getErrors().isEmpty(), String.valueOf(updated.getErrors()));
            stored = GetAction.execute(TABLE, inserted.getValueInteger("id"));
            assertEquals(LocalDate.of(2024, 3, 1), stored.getValueLocalDate("dateValue"), zone);
            assertEquals(LocalTime.MIDNIGHT, stored.getValueLocalTime("timeValue"), zone);
            assertEquals(instant, stored.getValueInstant("dateTimeValue"), zone);
            updated = UpdateAction.executeForRecords(new UpdateInput().withTableName(TABLE)
               .withRecord(new QRecord().withValue("id", inserted.getValueInteger("id"))
                  .withValue("dateValue", null).withValue("timeValue", null).withValue("dateTimeValue", null))).get(0);
            assertTrue(updated.getErrors().isEmpty(), String.valueOf(updated.getErrors()));
            stored = GetAction.execute(TABLE, inserted.getValueInteger("id"));
            assertNull(stored.getValue("dateValue"));
            assertNull(stored.getValue("timeValue"));
            assertNull(stored.getValue("dateTimeValue"));
         }
      }
      finally
      {
         QContext.clear();
         ConnectionManager.resetConnectionProviders();
         TimeZone.setDefault(original);
      }
   }
}
