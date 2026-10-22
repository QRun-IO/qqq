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


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
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
import com.kingsrook.qqq.backend.module.filesystem.base.actions.AbstractPostReadFileCustomizer;
import com.kingsrook.qqq.backend.module.filesystem.base.actions.FilesystemTableCustomizers;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.Cardinality;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.RecordFormat;
import com.kingsrook.qqq.backend.module.filesystem.sftp.model.metadata.SFTPBackendMetaData;
import com.kingsrook.qqq.backend.module.filesystem.sftp.model.metadata.SFTPTableBackendDetails;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.sshd.common.SshException;
import org.apache.sshd.common.SyspropsMapWrapper;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.core.CoreModuleProperties;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.common.SftpConstants;
import org.apache.sshd.sftp.common.SftpException;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Real SFTP sample contracts with an owned server and independent native file oracle.
 *******************************************************************************/
class SampleSftpAcceptanceTest
{
   private static final String ONE = "sftpFile";
   private static final String CSV = "sftpCsv";
   private static final String JSON = "sftpJson";

   @TempDir
   Path directory;

   private Path root;
   private SshServer server;
   private SFTPBackendMetaData backend;



   /*******************************************************************************
    ** A fresh loopback server and virtual root isolate every test's transport and files.
    *******************************************************************************/
   @BeforeEach
   void initialize() throws Exception
   {
      root = Files.createDirectory(directory.resolve("server-root"));
      Files.createDirectory(root.resolve("one"));
      server = SshServer.setUpDefaultServer();
      server.setHost("127.0.0.1");
      server.setPort(0);
      server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(directory.resolve("host-key")));
      server.setPasswordAuthenticator((username, password, session) -> "sample".equals(username) && "fixture-only".equals(password));
      server.setFileSystemFactory(new VirtualFileSystemFactory(root));
      server.setSubsystemFactories(List.of(new SftpSubsystemFactory.Builder().build()));
      server.start();

      backend = new SFTPBackendMetaData();
      backend.setName("sftpAcceptance");
      backend.setHostName("127.0.0.1");
      backend.setPort(server.getPort());
      backend.setUsername("sample");
      backend.setPassword("fixture-only");
      backend.setBasePath("/");
      QInstance instance = SampleMetaDataProvider.defineTestInstance();
      instance.addBackend(backend);
      instance.addTable(new QTableMetaData().withName(ONE).withBackendName(backend.getName()).withPrimaryKeyField("path")
         .withField(new QFieldMetaData("path", QFieldType.STRING))
         .withField(new QFieldMetaData("name", QFieldType.STRING))
         .withField(new QFieldMetaData("size", QFieldType.LONG))
         .withField(new QFieldMetaData("contents", QFieldType.BLOB).withIsHeavy(true))
         .withBackendDetails(new SFTPTableBackendDetails().withBasePath("one").withCardinality(Cardinality.ONE)
            .withFileNameFieldName("path").withBaseNameFieldName("name").withSizeFieldName("size").withContentsFieldName("contents")));
      for(RecordFormat format : RecordFormat.values())
      {
         String name = format == RecordFormat.CSV ? CSV : JSON;
         Files.createDirectory(root.resolve(name));
         instance.addTable(new QTableMetaData().withName(name).withBackendName(backend.getName()).withPrimaryKeyField("id")
            .withField(new QFieldMetaData("id", QFieldType.INTEGER))
            .withField(new QFieldMetaData("name", QFieldType.STRING))
            .withField(new QFieldMetaData("optional", QFieldType.STRING))
            .withBackendDetails(new SFTPTableBackendDetails().withBasePath(name).withCardinality(Cardinality.MANY)
               .withRecordFormat(format).withGlob(format == RecordFormat.CSV ? "*.csv" : "*.json")));
      }
      QContext.init(instance, new QSession());
   }



   /*******************************************************************************
    ** Stop only this fixture's listener; JUnit removes its temporary files.
    *******************************************************************************/
   @AfterEach
   void cleanup() throws Exception
   {
      QContext.clear();
      if(server != null)
      {
         int port = server.getPort();
         server.stop(true);
         assertTrue(server.isClosed());
         try(Socket socket = new Socket())
         {
            assertThrows(java.io.IOException.class, () -> socket.connect(new InetSocketAddress("127.0.0.1", port), 500));
         }
      }
   }



   /*******************************************************************************
    ** ONE record operations traverse SFTP and agree with native persisted bytes.
    *******************************************************************************/
   @Test
   void testOneCrudAndNativeReadback() throws Exception
   {
      QRecord inserted = InsertAction.executeForRecords(new InsertInput().withTableName(ONE)
         .withRecord(new QRecord().withValue("path", "snow.txt").withValue("contents", "Snow ☃"))).get(0);
      assertTrue(inserted.getErrors().isEmpty(), inserted.getErrors().toString());
      assertEquals("Snow ☃", Files.readString(root.resolve("one/snow.txt")));
      assertEquals(1, CountAction.execute(ONE, null));
      List<QRecord> records = new QueryAction().execute(new QueryInput().withTableName(ONE).withShouldFetchHeavyFields(true)).getRecords();
      assertEquals(1, records.size());
      assertEquals("snow.txt", records.get(0).getValueString("path"));
      assertEquals("snow.txt", records.get(0).getValueString("name"));
      assertEquals(8L, records.get(0).getValueLong("size"));
      assertEquals(1, new DeleteAction().execute(new DeleteInput().withTableName(ONE).withPrimaryKey("snow.txt")).getDeletedRecordCount());
      assertFalse(Files.exists(root.resolve("one/snow.txt")));
      assertEquals(0, CountAction.execute(ONE, null));
   }



   /*******************************************************************************
    ** CSV and JSON MANY parsing preserve Unicode, escaping, missing and null fields.
    *******************************************************************************/
   @Test
   void testManyFormatsAndCounts() throws Exception
   {
      String csv = "id,name,optional\n1,\"Snow 雪, \"\"quoted\"\"\",\n2,\"two\nlines\"\n";
      String json = "[{\"id\": 1, \"name\": \"Snow 雪, \\\"quoted\\\"\", \"optional\": null}, {\"id\": 2, \"name\": \"two\\nlines\"}]";
      Files.writeString(root.resolve(CSV + "/input.csv"), csv);
      Files.writeString(root.resolve(JSON + "/input.json"), json);
      for(String table : List.of(CSV, JSON))
      {
         List<QRecord> rows = query(table);
         assertEquals(List.of(1, 2), rows.stream().map(r -> r.getValueInteger("id")).toList());
         assertEquals("Snow 雪, \"quoted\"", rows.get(0).getValueString("name"));
         assertEquals("two\nlines", rows.get(1).getValueString("name"));
         assertNull(rows.get(1).getValue("optional"));
         assertEquals(2, CountAction.execute(table, null));
      }
      assertNull(query(JSON).get(0).getValue("optional"));
      assertEquals("", query(CSV).get(0).getValue("optional"));
      assertEquals(csv, Files.readString(root.resolve(CSV + "/input.csv")));
      assertEquals(json, Files.readString(root.resolve(JSON + "/input.json")));
   }



   /*******************************************************************************
    ** Exact filename selection reaches nested files; broad SFTP listing is not recursive.
    *******************************************************************************/
   @Test
   void testNamesPathsAndNonrecursiveListing() throws Exception
   {
      Files.createDirectory(root.resolve("one/nested"));
      String name = "nested/雪 data.txt";
      QRecord record = InsertAction.executeForRecords(new InsertInput().withTableName(ONE)
         .withRecord(new QRecord().withValue("path", name).withValue("contents", "nested"))).get(0);
      assertTrue(record.getErrors().isEmpty());
      assertEquals("nested", Files.readString(root.resolve("one").resolve(name)));
      assertEquals(0, CountAction.execute(ONE, null));
      QQueryFilter filter = new QQueryFilter(new QFilterCriteria("path", QCriteriaOperator.EQUALS, name));
      List<QRecord> rows = new QueryAction().execute(new QueryInput().withTableName(ONE).withFilter(filter).withShouldFetchHeavyFields(true)).getRecords();
      assertEquals(1, rows.size());
      assertEquals(name, rows.get(0).getValueString("path"));
      assertEquals("雪 data.txt", rows.get(0).getValueString("name"));
      assertEquals(1, CountAction.execute(ONE, filter));
      assertEquals(1, new DeleteAction().execute(new DeleteInput().withTableName(ONE).withPrimaryKey(name)).getDeletedRecordCount());
      assertFalse(Files.exists(root.resolve("one").resolve(name)));
   }



   /*******************************************************************************
    ** Raw storage streams persist actual bytes and shorter replacement truncates.
    *******************************************************************************/
   @Test
   void testStorageStreamsAndReplacement() throws Exception
   {
      StorageInput input = new StorageInput(ONE).withReference("雪 data.bin");
      byte[] bytes = new byte[] {0, 1, -1, 10, 13, 65};
      try(OutputStream output = new StorageAction().createOutputStream(input))
      {
         output.write(bytes);
      }
      assertArrayEquals(bytes, Files.readAllBytes(root.resolve("one/雪 data.bin")));
      try(InputStream inputStream = new StorageAction().getInputStream(input))
      {
         assertArrayEquals(bytes, inputStream.readAllBytes());
      }
      try(OutputStream output = new StorageAction().createOutputStream(input))
      {
         output.write("short".getBytes(StandardCharsets.UTF_8));
      }
      assertEquals("short", Files.readString(root.resolve("one/雪 data.bin")));
   }



   /*******************************************************************************
    ** Unsupported updates, MANY mutations and public URL operations refuse explicitly.
    *******************************************************************************/
   @Test
   void testUnsupportedOperationsPreserveFiles() throws Exception
   {
      Files.writeString(root.resolve("one/keep.txt"), "keep");
      for(String table : List.of(ONE, CSV, JSON))
      {
         QRecord record = new QRecord().withValue(table.equals(ONE) ? "path" : "id", table.equals(ONE) ? "keep.txt" : 1);
         Exception error = assertThrows(Exception.class, () -> new UpdateAction().execute(new UpdateInput().withTableName(table).withRecord(record)));
         assertInstanceOf(NotImplementedException.class, rootCause(error));
      }
      for(String table : List.of(CSV, JSON))
      {
         Exception insert = assertThrows(Exception.class, () -> new InsertAction().execute(new InsertInput().withTableName(table).withRecord(new QRecord().withValue("id", 1))));
         assertInstanceOf(NotImplementedException.class, rootCause(insert));
         Exception delete = assertThrows(Exception.class, () -> new DeleteAction().execute(new DeleteInput().withTableName(table).withPrimaryKey(1)));
         assertInstanceOf(NotImplementedException.class, rootCause(delete));
         try(var files = Files.list(root.resolve(table)))
         {
            assertEquals(0, files.count());
         }
      }
      StorageInput input = new StorageInput(ONE).withReference("keep.txt");
      assertEquals("Not implemented", rootCause(assertThrows(QException.class, () -> new StorageAction().getDownloadURL(input))).getMessage());
      assertEquals("Not implemented", rootCause(assertThrows(QException.class, () -> new StorageAction().makePublic(input))).getMessage());
      assertEquals("keep", Files.readString(root.resolve("one/keep.txt")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<QRecord> query(String table) throws QException
   {
      return new QueryAction().execute(new QueryInput().withTableName(table).withShouldFetchHeavyFields(true)).getRecords();
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
    ** A configured JSON glob must not parse an unrelated sidecar as JSON.
    *******************************************************************************/
   @Test
   void testConfiguredGlobExcludesNonRecords() throws Exception
   {
      Files.writeString(root.resolve(JSON + "/input.json"), "[{\"id\":1,\"name\":\"allowed\"}]");
      Files.writeString(root.resolve(JSON + "/ignored.txt"), "not a JSON record");
      assertAll(
         () -> assertEquals(1, query(JSON).size()),
         () -> assertEquals(1, CountAction.execute(JSON, null)));
      assertEquals("not a JSON record", Files.readString(root.resolve(JSON + "/ignored.txt")));
   }



   /*******************************************************************************
    ** Missing remote paths fail explicitly; malformed input is not silently discarded.
    *******************************************************************************/
   @Test
   void testMissingAndMalformedFiles() throws Exception
   {
      assertThrows(QException.class, () -> new StorageAction().getInputStream(new StorageInput(ONE).withReference("absent.bin")));
      assertFalse(Files.exists(root.resolve("one/absent.bin")));
      var deleted = new DeleteAction().execute(new DeleteInput().withTableName(ONE).withPrimaryKey("absent.bin"));
      assertEquals(0, deleted.getDeletedRecordCount());
      assertEquals(1, deleted.getRecordsWithErrors().size());
      Files.delete(root.resolve("one"));
      assertThrows(QException.class, () -> query(ONE));
      assertThrows(QException.class, () -> CountAction.execute(ONE, null));
      for(String body : List.of("{", "[{\"id\":1}", "not-json", "[42]"))
      {
         Path file = root.resolve(JSON + "/bad.json");
         Files.writeString(file, body);
         assertThrows(QException.class, () -> query(JSON));
         assertThrows(QException.class, () -> CountAction.execute(JSON, null));
         assertEquals(body, Files.readString(file));
      }
      Path csv = root.resolve(CSV + "/bad.csv");
      String body = "id,name\n1,\"unclosed";
      Files.writeString(csv, body);
      assertThrows(QException.class, () -> query(CSV));
      assertThrows(QException.class, () -> CountAction.execute(CSV, null));
      assertEquals(body, Files.readString(csv));
   }



   /*******************************************************************************
    ** Post-read customizers operate on MANY contents without mutating remote files.
    *******************************************************************************/
   @Test
   void testPostReadCustomizationAndFailures() throws Exception
   {
      for(String table : List.of(CSV, JSON))
      {
         String body = table.equals(CSV) ? "id,name\n1,ORIGINAL\n" : "[{\"id\":1,\"name\":\"ORIGINAL\"}]";
         Path file = root.resolve(table + (table.equals(CSV) ? "/input.csv" : "/input.json"));
         Files.writeString(file, body);
         QTableMetaData metadata = QContext.getQInstance().getTable(table);
         metadata.withCustomizer(FilesystemTableCustomizers.POST_READ_FILE.getRole(), new QCodeReference(ReplaceText.class));
         assertEquals("Changed 雪", query(table).get(0).getValueString("name"));
         assertEquals(1, CountAction.execute(table, null));
         assertEquals(body, Files.readString(file));
         metadata.getCustomizers().put(FilesystemTableCustomizers.POST_READ_FILE.getRole(), new QCodeReference(RejectText.class));
         assertEquals("fixture customizer rejection", rootCause(assertThrows(QException.class, () -> query(table))).getMessage());
         assertThrows(QException.class, () -> CountAction.execute(table, null));
         assertEquals(body, Files.readString(file));
      }
   }



   /*******************************************************************************
    ** Bad credentials are rejected over SSH; restoring credentials restores access.
    *******************************************************************************/
   @Test
   void testBadCredentialsAndRecovery() throws Exception
   {
      Files.writeString(root.resolve("one/keep.txt"), "keep");
      backend.setPassword("wrong-fixture-password");
      assertThrows(QException.class, () -> query(ONE));
      assertThrows(QException.class, () -> new InsertAction().execute(new InsertInput().withTableName(ONE)
         .withRecord(new QRecord().withValue("path", "keep.txt").withValue("contents", "overwrite"))));
      assertThrows(QException.class, () -> new StorageAction().getInputStream(new StorageInput(ONE).withReference("keep.txt")));
      assertEquals("keep", Files.readString(root.resolve("one/keep.txt")));
      backend.setPassword("fixture-only");
      backend.setUsername("wrong-fixture-user");
      assertThrows(QException.class, () -> CountAction.execute(ONE, null));
      backend.setUsername("sample");
      assertEquals(1, CountAction.execute(ONE, null));
      assertEquals("keep", query(ONE).get(0).getValueString("contents"));
   }



   /*******************************************************************************
    ** The server's actual OS permissions deny reads and writes, with allowed controls.
    *******************************************************************************/
   @Test
   void testRealPermissionsAndRecovery() throws Exception
   {
      Path folder = root.resolve("one");
      Path file = folder.resolve("protected.txt");
      Files.writeString(file, "protected");
      Set<PosixFilePermission> filePermissions = Files.getPosixFilePermissions(file);
      Set<PosixFilePermission> folderPermissions = Files.getPosixFilePermissions(folder);
      assertEquals("protected", query(ONE).get(0).getValueString("contents"));
      try
      {
         Files.setPosixFilePermissions(file, Set.of());
         Files.setPosixFilePermissions(folder, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE));
         assertFalse(Files.isReadable(file), "Run as an unprivileged user");
         assertFalse(Files.isWritable(folder));
         QException read = assertThrows(QException.class, () -> new StorageAction().getInputStream(new StorageInput(ONE).withReference("protected.txt")));
         assertEquals(SftpConstants.SSH_FX_PERMISSION_DENIED, ((SftpException) rootCause(read)).getStatus());
         QRecord deniedRead = query(ONE).get(0);
         assertFalse(deniedRead.getErrors().isEmpty());
         assertNull(deniedRead.getValue("contents"));
         assertEquals(1, CountAction.execute(ONE, null));
         var deniedDelete = new DeleteAction().execute(new DeleteInput().withTableName(ONE).withPrimaryKey("protected.txt"));
         assertEquals(0, deniedDelete.getDeletedRecordCount());
         assertEquals(1, deniedDelete.getRecordsWithErrors().size());
         assertTrue(Files.exists(file));
         Exception write = assertThrows(Exception.class, () ->
         {
            try(OutputStream stream = new StorageAction().createOutputStream(new StorageInput(ONE).withReference("new.txt")))
            {
               stream.write("denied".getBytes(StandardCharsets.UTF_8));
            }
         });
         assertTrue(rootCause(write).getMessage().contains("Permission denied"), write.toString());
         QRecord record = InsertAction.executeForRecords(new InsertInput().withTableName(ONE)
            .withRecord(new QRecord().withValue("path", "new.txt").withValue("contents", "denied"))).get(0);
         assertFalse(record.getErrors().isEmpty());
         assertFalse(Files.exists(folder.resolve("new.txt")));
      }
      finally
      {
         Files.setPosixFilePermissions(file, filePermissions);
         Files.setPosixFilePermissions(folder, folderPermissions);
      }
      assertEquals("protected", Files.readString(file));
      assertEquals("protected", query(ONE).get(0).getValueString("contents"));
      try(OutputStream stream = new StorageAction().createOutputStream(new StorageInput(ONE).withReference("new.txt")))
      {
         stream.write("allowed".getBytes(StandardCharsets.UTF_8));
      }
      assertEquals("allowed", Files.readString(folder.resolve("new.txt")));
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
      public String customizeFileContents(String contents)
      {
         return contents.replace("ORIGINAL", "Changed 雪");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class RejectText extends AbstractPostReadFileCustomizer
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public String customizeFileContents(String contents)
      {
         throw new IllegalStateException("fixture customizer rejection");
      }
   }



   /*******************************************************************************
    ** ONE exposes raw bytes for both format settings and bypasses MANY post-read hooks.
    *******************************************************************************/
   @Test
   void testOneRawFormatsAndPostReadBoundary() throws Exception
   {
      QTableMetaData table = QContext.getQInstance().getTable(ONE);
      table.withCustomizer(FilesystemTableCustomizers.POST_READ_FILE.getRole(), new QCodeReference(RejectText.class));
      for(RecordFormat format : RecordFormat.values())
      {
         ((SFTPTableBackendDetails) table.getBackendDetails()).setRecordFormat(format);
         String body = format == RecordFormat.CSV ? "id,name\n1,雪\n" : "{\"name\":\"雪\",\"optional\":null}";
         QRecord inserted = InsertAction.executeForRecords(new InsertInput().withTableName(ONE)
            .withRecord(new QRecord().withValue("path", "raw.data").withValue("contents", body))).get(0);
         assertTrue(inserted.getErrors().isEmpty());
         assertArrayEquals(body.getBytes(StandardCharsets.UTF_8), query(ONE).get(0).getValueByteArray("contents"));
         assertEquals(body, Files.readString(root.resolve("one/raw.data")));
         assertEquals(1, CountAction.execute(ONE, null));
      }
   }



   /*******************************************************************************
    ** SFTP rejects invalid paths without inventing directories or rolling back a batch.
    *******************************************************************************/
   @Test
   void testInvalidPathsAndPartialRecordBatch() throws Exception
   {
      Files.writeString(root.resolve("one/blocker"), "sentinel");
      List<QRecord> rows = InsertAction.executeForRecords(new InsertInput().withTableName(ONE).withRecords(List.of(
         new QRecord().withValue("path", "first.txt").withValue("contents", "first"),
         new QRecord().withValue("path", "blocker/child.txt").withValue("contents", "bad"),
         new QRecord().withValue("path", "missing/child.txt").withValue("contents", "bad"),
         new QRecord().withValue("path", "bad\0name").withValue("contents", "bad"),
         new QRecord().withValue("path", "last.txt").withValue("contents", "last"))));
      assertEquals(5, rows.size());
      assertTrue(rows.get(0).getErrors().isEmpty());
      for(int i : List.of(1, 2, 3))
      {
         assertFalse(rows.get(i).getErrors().isEmpty());
      }
      assertTrue(rows.get(4).getErrors().isEmpty());
      assertEquals("first", Files.readString(root.resolve("one/first.txt")));
      assertEquals("last", Files.readString(root.resolve("one/last.txt")));
      assertEquals("sentinel", Files.readString(root.resolve("one/blocker")));
      assertFalse(Files.exists(root.resolve("one/missing")));
      try(var files = Files.list(root.resolve("one")))
      {
         assertEquals(3, files.count());
      }
      assertThrows(Exception.class, () ->
      {
         try(OutputStream output = new StorageAction().createOutputStream(new StorageInput(ONE).withReference("blocker/child.txt")))
         {
            output.write(1);
         }
      });
      assertEquals("sentinel", Files.readString(root.resolve("one/blocker")));
   }



   /*******************************************************************************
    ** A source failure leaves the transmitted prefix; streamed writes are not atomic.
    *******************************************************************************/
   @Test
   void testPartialStreamFailurePreservesActualPrefix() throws Exception
   {
      Path file = root.resolve("one/partial.bin");
      Files.writeString(file, "old complete content");
      try(InputStream source = new FailingSource();
         OutputStream output = new StorageAction().createOutputStream(new StorageInput(ONE).withReference("partial.bin")))
      {
         assertEquals("fixture source failure", assertThrows(IOException.class, () -> source.transferTo(output)).getMessage());
      }
      assertArrayEquals(new byte[] {65, 66, 67}, Files.readAllBytes(file));
      try(InputStream input = new StorageAction().getInputStream(new StorageInput(ONE).withReference("partial.bin")))
      {
         assertArrayEquals(new byte[] {65, 66, 67}, input.readAllBytes());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static class FailingSource extends InputStream
   {
      private int position;

      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public int read() throws IOException
      {
         if(position == 3)
         {
            throw new IOException("fixture source failure");
         }
         return 65 + position++;
      }
   }



   /*******************************************************************************
    ** A TCP peer that never sends SSH identification hits the real client's auth timer.
    *******************************************************************************/
   @Test
   @ResourceLock(Resources.SYSTEM_PROPERTIES)
   void testSilentPeerConnectionTimeoutAndRecovery() throws Exception
   {
      String property = SyspropsMapWrapper.SYSPROPS_MAPPED_PREFIX + "." + CoreModuleProperties.AUTH_TIMEOUT.getName();
      String original = System.getProperty(property);
      int originalPort = backend.getPort();
      QInstance instance = QContext.getQInstance();
      Files.writeString(root.resolve("one/keep.txt"), "keep");
      try
      {
         System.setProperty(property, "500");
         try(var executor = Executors.newSingleThreadExecutor();
            ServerSocket peer = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")))
         {
            peer.setSoTimeout(2000);
            backend.setPort(peer.getLocalPort());
            long started = System.nanoTime();
            var attempt = executor.submit(() ->
            {
               QContext.init(instance, new QSession());
               try
               {
                  return assertThrows(QException.class, () -> query(ONE));
               }
               finally
               {
                  QContext.clear();
               }
            });
            try(Socket accepted = peer.accept())
            {
               accepted.setSoTimeout(2000);
               String banner = new BufferedReader(new InputStreamReader(accepted.getInputStream(), StandardCharsets.UTF_8)).readLine();
               assertTrue(banner.startsWith("SSH-2.0-"));
               QException failure = attempt.get(5, TimeUnit.SECONDS);
               assertInstanceOf(SshException.class, rootCause(failure));
               assertEquals("Session is being closed", rootCause(failure).getMessage());
               assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) >= 500);
               accepted.getInputStream().readAllBytes();
               assertFalse(accepted.isClosed(), "The fixture did not close the peer to provoke the failure");
               assertEquals("keep", Files.readString(root.resolve("one/keep.txt")));
            }
            finally
            {
               attempt.cancel(true);
            }
         }
      }
      finally
      {
         backend.setPort(originalPort);
         if(original == null)
         {
            System.clearProperty(property);
         }
         else
         {
            System.setProperty(property, original);
         }
      }
      assertEquals(1, CountAction.execute(ONE, null));
      assertEquals("keep", query(ONE).get(0).getValueString("contents"));
   }



   /*******************************************************************************
    ** A record path must not overwrite a sentinel outside its configured table directory.
    *******************************************************************************/
   @Test
   void testRecordPathCannotEscapeTableDirectory() throws Exception
   {
      Path outside = root.resolve("outside.txt");
      Files.writeString(outside, "sentinel");
      QRecord record = InsertAction.executeForRecords(new InsertInput().withTableName(ONE)
         .withRecord(new QRecord().withValue("path", "../outside.txt").withValue("contents", "overwrite"))).get(0);
      assertEquals("sentinel", Files.readString(outside));
      assertFalse(record.getErrors().isEmpty());
   }



   /*******************************************************************************
    ** Storage references must not overwrite a sentinel outside the table directory.
    *******************************************************************************/
   @Test
   void testStoragePathCannotEscapeTableDirectory() throws Exception
   {
      Path outside = root.resolve("outside.txt");
      Files.writeString(outside, "sentinel");
      Exception failure = null;
      try(OutputStream output = new StorageAction().createOutputStream(new StorageInput(ONE).withReference("../outside.txt")))
      {
         output.write("overwrite".getBytes(StandardCharsets.UTF_8));
      }
      catch(Exception e)
      {
         failure = e;
      }
      assertEquals("sentinel", Files.readString(outside));
      assertTrue(failure != null);
   }



   /*******************************************************************************
    ** Direct nested selection uses the same table-relative glob syntax as directory listing.
    *******************************************************************************/
   @Test
   void testGlobAppliesToExplicitNestedPaths() throws Exception
   {
      Files.createDirectory(root.resolve("one/nested"));
      Path file = root.resolve("one/nested/雪 data.txt");
      Files.writeString(file, "nested");
      SFTPTableBackendDetails details = (SFTPTableBackendDetails) QContext.getQInstance().getTable(ONE).getBackendDetails();
      QQueryFilter filter = new QQueryFilter(new QFilterCriteria("path", QCriteriaOperator.EQUALS, "nested/雪 data.txt"));
      details.setGlob("nested/*.txt");
      assertEquals(0, CountAction.execute(ONE, null));
      assertEquals(1, CountAction.execute(ONE, filter));
      assertEquals("nested", new QueryAction().execute(new QueryInput().withTableName(ONE).withFilter(filter)
         .withShouldFetchHeavyFields(true)).getRecords().get(0).getValueString("contents"));
      details.setGlob("*.csv");
      assertEquals(0, CountAction.execute(ONE, filter));
      assertTrue(new QueryAction().execute(new QueryInput().withTableName(ONE).withFilter(filter)).getRecords().isEmpty());
      assertEquals("nested", Files.readString(file));
   }



   /*******************************************************************************
    ** Existing targets outside the table must not be read through either public API.
    *******************************************************************************/
   @Test
   void testRecordAndStorageReadCannotEscapeTableDirectory() throws Exception
   {
      Path outside = root.resolve("outside.txt");
      Files.writeString(outside, "sentinel");
      QQueryFilter filter = new QQueryFilter(new QFilterCriteria("path", QCriteriaOperator.EQUALS, "../outside.txt"));
      assertAll(
         () -> assertThrows(QException.class, () ->
         {
            List<QRecord> records = new QueryAction().execute(new QueryInput().withTableName(ONE)
               .withFilter(filter).withShouldFetchHeavyFields(true)).getRecords();
            assertEquals(1, records.size());
            assertEquals("sentinel", records.get(0).getValueString("contents"));
         }),
         () -> assertThrows(QException.class, () ->
         {
            try(InputStream stream = new StorageAction().getInputStream(new StorageInput(ONE).withReference("../outside.txt")))
            {
               assertEquals("sentinel", new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            }
         }),
         () -> assertEquals("sentinel", Files.readString(outside)));
   }



   /*******************************************************************************
    ** Deleting a record path must preserve an outside-table native sentinel.
    *******************************************************************************/
   @Test
   void testRecordDeleteCannotEscapeTableDirectory() throws Exception
   {
      Path outside = root.resolve("outside.txt");
      Files.writeString(outside, "sentinel");
      var deleted = new DeleteAction().execute(new DeleteInput().withTableName(ONE).withPrimaryKey("../outside.txt"));
      assertTrue(Files.exists(outside), "SFTP record deletion escaped the configured table directory");
      assertEquals("sentinel", Files.readString(outside));
      assertEquals(0, deleted.getDeletedRecordCount());
      assertEquals(1, deleted.getRecordsWithErrors().size());
   }



   /*******************************************************************************
    ** Remote link targets must obey table bounds for every supported read/write/delete API.
    *******************************************************************************/
   @Test
   void testOutsideSymlinkTargetsPreserveSentinels() throws Exception
   {
      Path outsideDirectory = Files.createDirectory(root.resolve("outside-dir"));
      Path sentinel = outsideDirectory.resolve("keep.txt");
      Files.writeString(sentinel, "sentinel");
      Files.createSymbolicLink(root.resolve("one/leaf.txt"), Path.of("../outside-dir/keep.txt"));
      Files.createSymbolicLink(root.resolve("one/directory"), Path.of("../outside-dir"));
      for(String reference : List.of("leaf.txt", "directory/keep.txt"))
      {
         QQueryFilter filter = new QQueryFilter(new QFilterCriteria("path", QCriteriaOperator.EQUALS, reference));
         assertAll(
            () -> assertThrows(QException.class, () -> new QueryAction().execute(new QueryInput().withTableName(ONE).withFilter(filter).withShouldFetchHeavyFields(true))),
            () -> assertThrows(QException.class, () ->
            {
               try(InputStream stream = new StorageAction().getInputStream(new StorageInput(ONE).withReference(reference)))
               {
                  stream.readAllBytes();
               }
            }),
            () -> assertThrows(QException.class, () ->
            {
               try(OutputStream stream = new StorageAction().createOutputStream(new StorageInput(ONE).withReference(reference)))
               {
                  stream.write("overwrite".getBytes(StandardCharsets.UTF_8));
               }
            }),
            () ->
            {
               QRecord record = InsertAction.executeForRecords(new InsertInput().withTableName(ONE)
                  .withRecord(new QRecord().withValue("path", reference).withValue("contents", "overwrite"))).get(0);
               assertEquals(1, record.getErrors().size());
            },
            () ->
            {
               var deleted = new DeleteAction().execute(new DeleteInput().withTableName(ONE).withPrimaryKey(reference));
               assertEquals(0, deleted.getDeletedRecordCount());
               assertEquals(1, deleted.getRecordsWithErrors().size());
            },
            () -> assertEquals("sentinel", Files.readString(sentinel)));
      }
      assertThrows(QException.class, () -> query(ONE));
      assertTrue(Files.isSymbolicLink(root.resolve("one/leaf.txt")));
      assertTrue(Files.isSymbolicLink(root.resolve("one/directory")));
   }



   /*******************************************************************************
    ** New file validation follows the existing parent, allowing safe links and rejecting outside parents.
    *******************************************************************************/
   @Test
   void testNewFilesThroughLinkedParents() throws Exception
   {
      Files.createDirectory(root.resolve("one/nested"));
      Files.createDirectory(root.resolve("outside-dir"));
      Files.createSymbolicLink(root.resolve("one/safe"), Path.of("nested"));
      Files.createSymbolicLink(root.resolve("one/escape"), Path.of("../outside-dir"));
      try(OutputStream output = new StorageAction().createOutputStream(new StorageInput(ONE).withReference("safe/雪 space.txt")))
      {
         output.write("inside".getBytes(StandardCharsets.UTF_8));
      }
      assertEquals("inside", Files.readString(root.resolve("one/nested/雪 space.txt")));
      QRecord safe = InsertAction.executeForRecords(new InsertInput().withTableName(ONE)
         .withRecord(new QRecord().withValue("path", "safe/record.txt").withValue("contents", "record"))).get(0);
      assertTrue(safe.getErrors().isEmpty());
      assertEquals("record", Files.readString(root.resolve("one/nested/record.txt")));
      assertAll(
         () -> assertThrows(QException.class, () ->
         {
            try(OutputStream output = new StorageAction().createOutputStream(new StorageInput(ONE).withReference("escape/new.txt")))
            {
               output.write(65);
            }
         }),
         () ->
         {
            QRecord record = InsertAction.executeForRecords(new InsertInput().withTableName(ONE)
               .withRecord(new QRecord().withValue("path", "escape/record.txt").withValue("contents", "outside"))).get(0);
            assertEquals(1, record.getErrors().size());
         },
         () -> assertFalse(Files.exists(root.resolve("outside-dir/new.txt"))),
         () -> assertFalse(Files.exists(root.resolve("outside-dir/record.txt"))));
   }



   /*******************************************************************************
    ** Configured roots keep relative/leading-slash behavior; SSHD cannot delete through a parent link.
    *******************************************************************************/
   @Test
   void testConfiguredRootsAndTableRootSymlink() throws Exception
   {
      Path configured = Files.createDirectories(root.resolve("configured root/real table"));
      Files.createSymbolicLink(root.resolve("configured root/table alias"), Path.of("real table"));
      SFTPTableBackendDetails details = (SFTPTableBackendDetails) QContext.getQInstance().getTable(ONE).getBackendDetails();
      details.setBasePath("real table");
      for(String base : List.of("configured root", "/configured root"))
      {
         backend.setBasePath(base);
         QRecord record = InsertAction.executeForRecords(new InsertInput().withTableName(ONE)
            .withRecord(new QRecord().withValue("path", "雪 data.txt").withValue("contents", "inside"))).get(0);
         assertTrue(record.getErrors().isEmpty());
         assertEquals("inside", Files.readString(configured.resolve("雪 data.txt")));
         assertEquals(1, CountAction.execute(ONE, null));
         assertEquals("inside", query(ONE).get(0).getValueString("contents"));
         try(InputStream stream = new StorageAction().getInputStream(new StorageInput(ONE).withReference("雪 data.txt")))
         {
            assertEquals("inside", new String(stream.readAllBytes(), StandardCharsets.UTF_8));
         }
         var deleted = new DeleteAction().execute(new DeleteInput().withTableName(ONE).withPrimaryKey("雪 data.txt"));
         assertEquals(1, deleted.getDeletedRecordCount(), deleted.getRecordsWithErrors().stream().map(QRecord::getErrorsAsString).toList().toString());
         assertFalse(Files.exists(configured.resolve("雪 data.txt")));
      }
      details.setBasePath("table alias");
      QRecord aliased = InsertAction.executeForRecords(new InsertInput().withTableName(ONE)
         .withRecord(new QRecord().withValue("path", "alias.txt").withValue("contents", "through root alias"))).get(0);
      assertTrue(aliased.getErrors().isEmpty());
      assertEquals("through root alias", query(ONE).get(0).getValueString("contents"));
      try(InputStream stream = new StorageAction().getInputStream(new StorageInput(ONE).withReference("alias.txt")))
      {
         assertEquals("through root alias", new String(stream.readAllBytes(), StandardCharsets.UTF_8));
      }
      var aliasedDelete = new DeleteAction().execute(new DeleteInput().withTableName(ONE).withPrimaryKey("alias.txt"));
      assertEquals(0, aliasedDelete.getDeletedRecordCount());
      assertEquals(1, aliasedDelete.getRecordsWithErrors().size());
      assertTrue(aliasedDelete.getRecordsWithErrors().get(0).getErrorsAsString().contains("No such file"));
      assertEquals("through root alias", Files.readString(configured.resolve("alias.txt")));
   }



   /*******************************************************************************
    ** Valid in-table symlinks remain readable/writable; deleting an alias keeps its target.
    *******************************************************************************/
   @Test
   void testInTableSymlinkControl() throws Exception
   {
      Path target = root.resolve("one/target.txt");
      Path link = root.resolve("one/alias.txt");
      Files.writeString(target, "initial");
      Files.createSymbolicLink(link, Path.of("target.txt"));
      QRecord record = InsertAction.executeForRecords(new InsertInput().withTableName(ONE)
         .withRecord(new QRecord().withValue("path", "alias.txt").withValue("contents", "updated"))).get(0);
      assertTrue(record.getErrors().isEmpty());
      assertEquals("updated", Files.readString(target));
      assertTrue(Files.isSymbolicLink(link));
      assertEquals(2, CountAction.execute(ONE, null));
      assertTrue(query(ONE).stream().allMatch(row -> "updated".equals(row.getValueString("contents"))));
      try(InputStream input = new StorageAction().getInputStream(new StorageInput(ONE).withReference("alias.txt")))
      {
         assertEquals("updated", new String(input.readAllBytes(), StandardCharsets.UTF_8));
      }
      assertEquals(1, new DeleteAction().execute(new DeleteInput().withTableName(ONE).withPrimaryKey("alias.txt")).getDeletedRecordCount());
      assertFalse(Files.isSymbolicLink(link));
      assertEquals("updated", Files.readString(target));
   }
}
