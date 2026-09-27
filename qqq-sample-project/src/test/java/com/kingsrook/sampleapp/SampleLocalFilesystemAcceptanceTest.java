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

package com.kingsrook.sampleapp;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QValueException;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.filesystem.base.FilesystemRecordBackendDetailFields;
import com.kingsrook.qqq.backend.module.filesystem.base.actions.AbstractPostReadFileCustomizer;
import com.kingsrook.qqq.backend.module.filesystem.base.actions.FilesystemTableCustomizers;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.Cardinality;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.RecordFormat;
import com.kingsrook.qqq.backend.module.filesystem.local.model.metadata.FilesystemBackendMetaData;
import com.kingsrook.qqq.backend.module.filesystem.local.model.metadata.FilesystemTableBackendDetails;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** First-party filesystem contracts with independent reads of owned temporary files.
 *******************************************************************************/
class SampleLocalFilesystemAcceptanceTest
{
   private static final String ONE = "localFile";
   private static final String CSV = "localCsv";
   private static final String JSON = "localJson";

   @TempDir
   Path directory;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void initialize() throws Exception
   {
      QInstance instance = SampleMetaDataProvider.defineTestInstance();
      instance.addBackend(new FilesystemBackendMetaData().withName("localAcceptance").withBasePath(directory.toString()));
      instance.addTable(new QTableMetaData().withName(ONE).withBackendName("localAcceptance").withPrimaryKeyField("path")
         .withField(new QFieldMetaData("path", QFieldType.STRING))
         .withField(new QFieldMetaData("name", QFieldType.STRING))
         .withField(new QFieldMetaData("size", QFieldType.LONG))
         .withField(new QFieldMetaData("contents", QFieldType.BLOB).withIsHeavy(true))
         .withBackendDetails(new FilesystemTableBackendDetails().withBasePath("one").withCardinality(Cardinality.ONE)
            .withFileNameFieldName("path").withBaseNameFieldName("name").withSizeFieldName("size").withContentsFieldName("contents")));
      for(RecordFormat format : RecordFormat.values())
      {
         String name = format == RecordFormat.CSV ? CSV : JSON;
         instance.addTable(new QTableMetaData().withName(name).withBackendName("localAcceptance").withPrimaryKeyField("id")
            .withField(new QFieldMetaData("id", QFieldType.INTEGER))
            .withField(new QFieldMetaData("name", QFieldType.STRING))
            .withField(new QFieldMetaData("quantity", QFieldType.INTEGER))
            .withField(new QFieldMetaData("optional", QFieldType.STRING))
            .withBackendDetails(new FilesystemTableBackendDetails().withBasePath(name).withCardinality(Cardinality.MANY)
               .withRecordFormat(format).withGlob(format == RecordFormat.CSV ? "*.csv" : "*.json")));
         Files.createDirectories(directory.resolve(name));
      }
      Files.createDirectories(directory.resolve("one"));
      QContext.init(instance, new QSession());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void clearContext()
   {
      QContext.clear();
   }



   /*******************************************************************************
    ** ONE holds whole files; ordinary insert/delete and metadata agree with disk.
    *******************************************************************************/
   @Test
   void testOneInsertQueryCountDeleteAndPaths() throws Exception
   {
      String name = "nested/snow-file.json";
      byte[] bytes = "{\"message\":\"Snow ☃ & \\\"ice\\\"\"}\n".getBytes(StandardCharsets.UTF_8);
      List<QRecord> inserted = InsertAction.executeForRecords(new InsertInput().withTableName(ONE)
         .withRecords(List.of(new QRecord().withValue("path", name).withValue("contents", bytes),
            new QRecord().withValue("path", "second.csv").withValue("contents", "id,name\n1,Hello\n"))));
      assertEquals(2, inserted.size());
      for(QRecord record : inserted)
      {
         assertTrue(record.getErrors().isEmpty(), record.getErrors().toString());
      }
      Path file = directory.resolve("one").resolve(name);
      assertArrayEquals(bytes, Files.readAllBytes(file));
      assertEquals(file.toRealPath(), Path.of(inserted.get(0).getBackendDetailString(FilesystemRecordBackendDetailFields.FULL_PATH)).toRealPath());
      assertEquals("id,name\n1,Hello\n", Files.readString(directory.resolve("one/second.csv")));
      assertEquals(2, CountAction.execute(ONE, null));
      QQueryFilter filter = new QQueryFilter(new QFilterCriteria("path", QCriteriaOperator.EQUALS, name));
      List<QRecord> records = new QueryAction().execute(new QueryInput().withTableName(ONE).withFilter(filter).withShouldFetchHeavyFields(true)).getRecords();
      assertEquals(1, records.size());
      assertEquals(name, records.get(0).getValueString("path"));
      assertEquals("snow-file.json", records.get(0).getValueString("name"));
      assertEquals((long) bytes.length, records.get(0).getValueLong("size"));
      assertArrayEquals(bytes, records.get(0).getValueByteArray("contents"));
      assertEquals(1, CountAction.execute(ONE, filter));
      QRecord light = new QueryAction().execute(new QueryInput().withTableName(ONE).withFilter(filter)).getRecords().get(0);
      assertNull(light.getValue("contents"));
      assertEquals(1, new DeleteAction().execute(new DeleteInput().withTableName(ONE).withPrimaryKey(name)).getDeletedRecordCount());
      assertFalse(Files.exists(file));
      assertEquals(1, CountAction.execute(ONE, null));
      assertTrue(Files.exists(directory.resolve("one/second.csv")));
   }



   /*******************************************************************************
    ** Literal filenames must not be interpreted as an unescaped URI.
    *******************************************************************************/
   @Test
   void testFilenameWithSpaces() throws Exception
   {
      Files.writeString(directory.resolve("one/space name.txt"), "unchanged");
      List<QRecord> records = new QueryAction().execute(new QueryInput().withTableName(ONE)
         .withFilter(new QQueryFilter(new QFilterCriteria("path", QCriteriaOperator.EQUALS, "space name.txt")))).getRecords();
      assertEquals(1, records.size());
      assertEquals("space name.txt", records.get(0).getValueString("path"));
      assertEquals("unchanged", Files.readString(directory.resolve("one/space name.txt")));
   }



   /*******************************************************************************
    ** Unicode filenames require the same literal path treatment as ASCII names.
    *******************************************************************************/
   @Test
   void testUnicodeFilename() throws Exception
   {
      Files.writeString(directory.resolve("one/雪.txt"), "unchanged");
      List<QRecord> records = new QueryAction().execute(new QueryInput().withTableName(ONE)
         .withFilter(new QQueryFilter(new QFilterCriteria("path", QCriteriaOperator.EQUALS, "雪.txt")))).getRecords();
      assertEquals(1, records.size());
      assertEquals("雪.txt", records.get(0).getValueString("path"));
      assertEquals("unchanged", Files.readString(directory.resolve("one/雪.txt")));
   }



   /*******************************************************************************
    ** ONE returns bytes regardless of the parsed-record format or MANY-only hook.
    *******************************************************************************/
   @Test
   void testOneKeepsRawBytesForBothFormats() throws Exception
   {
      byte[] bytes = "raw 雪, \"quoted\"\n".getBytes(StandardCharsets.UTF_8);
      Files.write(directory.resolve("one/raw.dat"), bytes);
      QTableMetaData table = QContext.getQInstance().getTable(ONE);
      table.withCustomizer(FilesystemTableCustomizers.POST_READ_FILE.getRole(), new QCodeReference(RejectContents.class));
      for(RecordFormat format : RecordFormat.values())
      {
         ((FilesystemTableBackendDetails) table.getBackendDetails()).withRecordFormat(format);
         assertArrayEquals(bytes, query(ONE).get(0).getValueByteArray("contents"));
         assertArrayEquals(bytes, Files.readAllBytes(directory.resolve("one/raw.dat")));
      }
   }



   /*******************************************************************************
    ** Parsed MANY records retain literal Unicode, escapes and per-file provenance.
    *******************************************************************************/
   @Test
   void testCsvJsonManyParsingAndCounts() throws Exception
   {
      String csv = "id,name,quantity,optional\r\n1,\"雪, \"\"quoted\"\"\nline\",7,\r\n2,Second,8,tail\r\n";
      Path csvFile = directory.resolve(CSV).resolve("input.csv");
      Files.writeString(csvFile, csv);
      List<QRecord> csvRecords = query(CSV);
      assertEquals(2, csvRecords.size());
      assertEquals("雪, \"quoted\"\nline", csvRecords.get(0).getValueString("name"));
      assertEquals(7, csvRecords.get(0).getValueInteger("quantity"));
      assertEquals("", csvRecords.get(0).getValueString("optional"));
      assertEquals("tail", csvRecords.get(1).getValueString("optional"));
      assertEquals(2, CountAction.execute(CSV, null));
      assertEquals(csv, Files.readString(csvFile));

      String json = "[{\"id\":3,\"name\":\"雪, \\\"quoted\\\"\\nline\",\"quantity\":9},{\"id\":4,\"name\":\"Second\",\"quantity\":10}]";
      Path jsonFile = directory.resolve(JSON).resolve("input.json");
      Files.writeString(jsonFile, json);
      List<QRecord> records = query(JSON);
      assertEquals(2, records.size());
      assertEquals("雪, \"quoted\"\nline", records.get(0).getValueString("name"));
      assertEquals(9, records.get(0).getValueInteger("quantity"));
      assertNull(records.get(0).getValue("optional"));
      assertEquals(2, CountAction.execute(JSON, null));
      for(QRecord record : records)
      {
         assertTrue(record.getErrors().isEmpty());
         assertEquals(jsonFile.toRealPath(), Path.of(record.getBackendDetailString(FilesystemRecordBackendDetailFields.FULL_PATH)).toRealPath());
      }
      assertEquals(json, Files.readString(jsonFile));
      Files.writeString(directory.resolve(JSON).resolve("ignored.txt"), "not JSON");
      assertEquals(2, CountAction.execute(JSON, null), "The configured glob excludes unrelated files");
      Files.writeString(jsonFile, "{\"id\":5,\"name\":\"Single object\"}");
      assertEquals(1, query(JSON).size());
      assertEquals(5, query(JSON).get(0).getValueInteger("id"));
   }



   /*******************************************************************************
    ** The CSV adapter deliberately suffixes duplicate headers rather than overwriting.
    *******************************************************************************/
   @Test
   void testDuplicateCsvHeadersAndMissingCells() throws Exception
   {
      QContext.getQInstance().getTable(CSV).withField(new QFieldMetaData("name 2", QFieldType.STRING));
      String source = "id,name,name,quantity,optional\n1,First,Second,7\n";
      Path file = directory.resolve(CSV).resolve("duplicates.csv");
      Files.writeString(file, source);
      QRecord record = query(CSV).get(0);
      assertEquals("First", record.getValueString("name"));
      assertEquals("Second", record.getValueString("name 2"));
      assertNull(record.getValue("optional"));
      assertEquals(source, Files.readString(file));
   }



   /*******************************************************************************
    ** Native parsers reject broken syntax; failed reads leave the source untouched.
    *******************************************************************************/
   @Test
   void testMalformedAndTruncatedFiles() throws Exception
   {
      for(String source : List.of("[{\"id\":1", "[1]", "not-json", ""))
      {
         Path file = directory.resolve(JSON).resolve("broken.json");
         Files.writeString(file, source);
         assertThrows(QException.class, () -> query(JSON), source);
         assertThrows(QException.class, () -> CountAction.execute(JSON, null), source);
         assertEquals(source, Files.readString(file));
      }
      Path file = directory.resolve(CSV).resolve("broken.csv");
      String source = "id,name\n1,\"unfinished";
      Files.writeString(file, source);
      assertThrows(QException.class, () -> query(CSV));
      assertThrows(QException.class, () -> CountAction.execute(CSV, null));
      assertEquals(source, Files.readString(file));
   }



   /*******************************************************************************
    ** Parsing stores source values; typed access is the existing conversion boundary.
    *******************************************************************************/
   @Test
   void testInvalidNumericValueAtTypedAccess() throws Exception
   {
      Files.writeString(directory.resolve(CSV).resolve("bad.csv"), "id,name,quantity\n1,Bad,not-an-integer\n");
      Files.writeString(directory.resolve(JSON).resolve("bad.json"), "[{\"id\":1,\"name\":\"Bad\",\"quantity\":\"not-an-integer\"}]");
      for(String table : List.of(CSV, JSON))
      {
         QRecord record = query(table).get(0);
         assertEquals("not-an-integer", record.getValue("quantity"));
         assertThrows(QValueException.class, () -> record.getValueInteger("quantity"));
      }
   }



   /*******************************************************************************
    ** File customizers precede parsing and cannot silently hide their own failures.
    *******************************************************************************/
   @Test
   void testPostReadCustomizationAndFailures() throws Exception
   {
      for(String table : List.of(CSV, JSON))
      {
         String source = table.equals(CSV) ? "id,name\n1,ORIGINAL\n" : "[{\"id\":1,\"name\":\"ORIGINAL\"}]";
         Path file = directory.resolve(table).resolve(table.equals(CSV) ? "input.csv" : "input.json");
         Files.writeString(file, source);
         QTableMetaData metadata = QContext.getQInstance().getTable(table);
         metadata.withCustomizer(FilesystemTableCustomizers.POST_READ_FILE.getRole(), new QCodeReference(ReplaceText.class));
         assertEquals("Customized 雪", query(table).get(0).getValueString("name"));
         assertEquals(1, CountAction.execute(table, null));
         assertEquals(source, Files.readString(file));
         metadata.getCustomizers().put(FilesystemTableCustomizers.POST_READ_FILE.getRole(), new QCodeReference(RejectContents.class));
         QException error = assertThrows(QException.class, () -> query(table));
         assertEquals("fixture customizer failure", rootCause(error).getMessage());
         assertThrows(QException.class, () -> CountAction.execute(table, null));
         assertEquals(source, Files.readString(file));
      }
   }



   /*******************************************************************************
    ** MANY writes and all update operations are explicitly unsupported.
    *******************************************************************************/
   @Test
   void testUnsupportedMutationsPreserveFiles() throws Exception
   {
      Files.writeString(directory.resolve("one/keep.txt"), "keep");
      for(String table : List.of(ONE, CSV, JSON))
      {
         String key = table.equals(ONE) ? "path" : "id";
         QRecord record = new QRecord().withValue(key, table.equals(ONE) ? "keep.txt" : 1).withValue("name", "Changed");
         Exception error = assertThrows(Exception.class, () -> new UpdateAction().execute(new UpdateInput().withTableName(table).withRecord(record)));
         assertInstanceOf(NotImplementedException.class, rootCause(error));
      }
      for(String table : List.of(CSV, JSON))
      {
         Exception insertError = assertThrows(Exception.class, () -> new InsertAction().execute(new InsertInput().withTableName(table)
            .withRecord(new QRecord().withValue("id", 1).withValue("name", "Unexpected"))));
         assertInstanceOf(NotImplementedException.class, rootCause(insertError));
         Exception deleteError = assertThrows(Exception.class, () -> new DeleteAction().execute(new DeleteInput().withTableName(table).withPrimaryKey(1)));
         assertInstanceOf(NotImplementedException.class, rootCause(deleteError));
         try(var entries = Files.list(directory.resolve(table)))
         {
            assertEquals(0, entries.count());
         }
      }
      assertEquals("keep", Files.readString(directory.resolve("one/keep.txt")));
   }



   /*******************************************************************************
    ** Storage writes/replacement, input streams and download URLs agree with raw bytes.
    *******************************************************************************/
   @Test
   void testRawStorageAndReplacement() throws Exception
   {
      StorageAction action = new StorageAction();
      StorageInput input = new StorageInput(ONE).withReference("nested/雪 data.bin");
      byte[] bytes = new byte[] {0, 1, 2, -1, 10};
      try(OutputStream stream = action.createOutputStream(input))
      {
         stream.write(bytes);
      }
      Path file = directory.resolve("one/nested/雪 data.bin");
      assertArrayEquals(bytes, Files.readAllBytes(file));
      try(InputStream stream = action.getInputStream(input))
      {
         assertArrayEquals(bytes, stream.readAllBytes());
      }
      assertEquals(file.toRealPath(), Path.of(URI.create(action.getDownloadURL(input))).toRealPath());
      try(OutputStream stream = action.createOutputStream(input))
      {
         stream.write("short".getBytes(StandardCharsets.UTF_8));
      }
      assertEquals("short", Files.readString(file));
   }



   /*******************************************************************************
    ** Canonical storage references cannot leave the table, including through symlinks.
    *******************************************************************************/
   @Test
   void testStorageRejectsInvalidPathsAndMissingInput() throws Exception
   {
      StorageAction action = new StorageAction();
      Path outside = directory.resolve("outside.txt");
      Files.writeString(outside, "sentinel");
      Files.createSymbolicLink(directory.resolve("one/link"), directory);
      for(String reference : List.of("", ".", "../outside.txt", outside.toString(), "link/outside.txt", "bad\0name"))
      {
         StorageInput input = new StorageInput(ONE).withReference(reference);
         assertThrows(QException.class, () -> action.createOutputStream(input), reference);
         assertThrows(QException.class, () -> action.getInputStream(input), reference);
         assertThrows(QException.class, () -> action.getDownloadURL(input), reference);
         assertEquals("sentinel", Files.readString(outside));
      }
      assertThrows(QException.class, () -> action.getInputStream(new StorageInput(ONE).withReference("missing.bin")));
      assertEquals("sentinel", Files.readString(outside));
   }



   /*******************************************************************************
    ** A source copy failure leaves the streamed prefix; storage offers no atomic commit.
    *******************************************************************************/
   @Test
   void testPartialStorageCopyRetainsPrefix() throws Exception
   {
      StorageInput input = new StorageInput(ONE).withReference("partial.bin");
      Path file = directory.resolve("one/partial.bin");
      Files.writeString(file, "old complete contents");
      try(InputStream source = new FailingSource(); OutputStream destination = new StorageAction().createOutputStream(input))
      {
         assertThrows(IOException.class, () -> source.transferTo(destination));
      }
      assertArrayEquals(new byte[] {65, 66, 67}, Files.readAllBytes(file));
   }



   /*******************************************************************************
    ** A failed middle ONE insert reports its error and preserves the other writes.
    *******************************************************************************/
   @Test
   void testPartialInsertAndInvalidPath() throws Exception
   {
      Files.writeString(directory.resolve("one/blocker"), "sentinel");
      List<QRecord> output = InsertAction.executeForRecords(new InsertInput().withTableName(ONE).withRecords(List.of(
         new QRecord().withValue("path", "first.bin").withValue("contents", "first"),
         new QRecord().withValue("path", "blocker/child.bin").withValue("contents", "bad"),
         new QRecord().withValue("path", "last.bin").withValue("contents", "last"))));
      assertEquals(3, output.size());
      assertTrue(output.get(0).getErrors().isEmpty());
      assertFalse(output.get(1).getErrors().isEmpty());
      assertTrue(output.get(2).getErrors().isEmpty());
      assertEquals("first", Files.readString(directory.resolve("one/first.bin")));
      assertEquals("last", Files.readString(directory.resolve("one/last.bin")));
      assertEquals("sentinel", Files.readString(directory.resolve("one/blocker")));
      assertFalse(Files.exists(directory.resolve("one/blocker/child.bin")));
   }



   /*******************************************************************************
    ** Empty/missing directories return no records; missing storage input is an error.
    *******************************************************************************/
   @Test
   void testMissingRecordFiles() throws Exception
   {
      for(String table : List.of(ONE, CSV, JSON))
      {
         assertTrue(query(table).isEmpty());
         assertEquals(0, CountAction.execute(table, null));
         Files.delete(directory.resolve(table.equals(ONE) ? "one" : table));
         assertTrue(query(table).isEmpty());
         assertEquals(0, CountAction.execute(table, null));
      }
   }



   /*******************************************************************************
    ** The OS is the denial oracle, with permissions restored before temporary cleanup.
    *******************************************************************************/
   @Test
   void testLocalPermissionFailures() throws Exception
   {
      Path file = directory.resolve("one/denied.bin");
      Files.writeString(file, "protected");
      Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(file);
      try
      {
         Files.setPosixFilePermissions(file, Set.of());
         assertFalse(Files.isReadable(file), "Run the OS denial contract as an unprivileged user");
         assertFalse(Files.isWritable(file), "Run the OS denial contract as an unprivileged user");
         assertThrows(QException.class, () -> new StorageAction().getInputStream(new StorageInput(ONE).withReference("denied.bin")));
         assertThrows(QException.class, () -> new StorageAction().createOutputStream(new StorageInput(ONE).withReference("denied.bin")));
         QRecord read = query(ONE).get(0);
         assertFalse(read.getErrors().isEmpty());
         assertNull(read.getValue("contents"));
         QRecord write = InsertAction.executeForRecords(new InsertInput().withTableName(ONE)
            .withRecord(new QRecord().withValue("path", "denied.bin").withValue("contents", "overwrite"))).get(0);
         assertFalse(write.getErrors().isEmpty());
      }
      finally
      {
         Files.setPosixFilePermissions(file, permissions);
      }
      assertEquals("protected", Files.readString(file));
   }



   /*******************************************************************************
    ** All path-escape probes remain inside the owned outer temporary directory.
    *******************************************************************************/
   @Test
   void testRecordInsertCannotEscapeTableDirectory() throws Exception
   {
      Path outside = directory.resolve("outside.txt");
      Files.writeString(outside, "sentinel");
      QRecord output = InsertAction.executeForRecords(new InsertInput().withTableName(ONE)
         .withRecord(new QRecord().withValue("path", "../outside.txt").withValue("contents", "overwrite"))).get(0);
      assertEquals("sentinel", Files.readString(outside), "Record path must not escape the table directory");
      assertFalse(output.getErrors().isEmpty());
   }



   /*******************************************************************************
    ** Record deletion must preserve a sentinel outside its table directory.
    *******************************************************************************/
   @Test
   void testRecordDeleteCannotEscapeTableDirectory() throws Exception
   {
      Path outside = directory.resolve("outside.txt");
      Files.writeString(outside, "sentinel");
      var output = new DeleteAction().execute(new DeleteInput().withTableName(ONE).withPrimaryKey("../outside.txt"));
      assertTrue(Files.exists(outside), "Delete escaped the table directory");
      assertEquals("sentinel", Files.readString(outside));
      assertEquals(0, output.getDeletedRecordCount());
      assertEquals(1, output.getRecordsWithErrors().size());
   }



   /*******************************************************************************
    ** A record write through a symlink cannot mutate a file outside the table.
    *******************************************************************************/
   @Test
   void testRecordInsertCannotFollowOutsideSymlink() throws Exception
   {
      Path outside = directory.resolve("outside.txt");
      Files.writeString(outside, "sentinel");
      Files.createSymbolicLink(directory.resolve("one/link.txt"), outside);
      QRecord output = InsertAction.executeForRecords(new InsertInput().withTableName(ONE)
         .withRecord(new QRecord().withValue("path", "link.txt").withValue("contents", "overwrite"))).get(0);
      assertEquals("sentinel", Files.readString(outside));
      assertFalse(output.getErrors().isEmpty());
   }



   /*******************************************************************************
    ** Listing a symlink cannot let record reads disclose an outside-table file.
    *******************************************************************************/
   @Test
   void testRecordQueryCannotFollowOutsideSymlink() throws Exception
   {
      Path outside = directory.resolve("outside.txt");
      Files.writeString(outside, "sentinel");
      Files.createSymbolicLink(directory.resolve("one/link.txt"), outside);
      assertThrows(QException.class, () -> query(ONE));
      assertEquals("sentinel", Files.readString(outside));
   }



   /*******************************************************************************
    ** A symlink whose target stays inside the table remains a valid file reference.
    *******************************************************************************/
   @Test
   void testRecordSymlinkWithinTableRemainsUsable() throws Exception
   {
      Path target = directory.resolve("one/target.txt");
      Path link = directory.resolve("one/alias.txt");
      Files.writeString(target, "before");
      Files.createSymbolicLink(link, target);
      List<QRecord> records = new QueryAction().execute(new QueryInput().withTableName(ONE).withShouldFetchHeavyFields(true)
         .withFilter(new QQueryFilter(new QFilterCriteria("path", QCriteriaOperator.EQUALS, "alias.txt")))).getRecords();
      assertEquals(1, records.size());
      assertEquals("alias.txt", records.get(0).getValueString("path"));
      assertArrayEquals("before".getBytes(StandardCharsets.UTF_8), records.get(0).getValueByteArray("contents"));
      QRecord output = InsertAction.executeForRecords(new InsertInput().withTableName(ONE)
         .withRecord(new QRecord().withValue("path", "alias.txt").withValue("contents", "after"))).get(0);
      assertTrue(output.getErrors().isEmpty());
      assertEquals("after", Files.readString(target));
      assertEquals(1, new DeleteAction().execute(new DeleteInput().withTableName(ONE).withPrimaryKey("alias.txt")).getDeletedRecordCount());
      assertFalse(Files.exists(link));
      assertEquals("after", Files.readString(target));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Throwable rootCause(Throwable error)
   {
      while(error.getCause() != null)
      {
         error = error.getCause();
      }
      return error;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class ReplaceText extends AbstractPostReadFileCustomizer
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public String customizeFileContents(String source)
      {
         return source.replace("ORIGINAL", "Customized 雪");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class RejectContents extends AbstractPostReadFileCustomizer
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public String customizeFileContents(String source)
      {
         throw new IllegalArgumentException("fixture customizer failure");
      }
   }



   /*******************************************************************************
    ** Delivers a complete first chunk, then fails while the actual storage stream is open.
    *******************************************************************************/
   private static class FailingSource extends InputStream
   {
      private int offset;



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public int read() throws IOException
      {
         if(offset < 3)
         {
            return 65 + offset++;
         }
         throw new IOException("fixture source failure");
      }

   }



   /*******************************************************************************
    ** A declared JSON null must survive the record boundary as a null value.
    *******************************************************************************/
   @Test
   void testJsonNullValue() throws Exception
   {
      Path file = directory.resolve(JSON).resolve("null.json");
      String source = "[{\"id\":1,\"name\":\"Snow ☃\",\"optional\":null}]";
      Files.writeString(file, source);
      QRecord record = query(JSON).get(0);
      assertEquals(1, record.getValueInteger("id"));
      assertNull(record.getValue("optional"));
      assertEquals(source, Files.readString(file));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<QRecord> query(String table) throws QException
   {
      return new QueryAction().execute(new QueryInput().withTableName(table).withShouldFetchHeavyFields(true)).getRecords();
   }
}
