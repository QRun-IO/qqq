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


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.PatternSyntaxException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.GroupGrantee;
import com.amazonaws.services.s3.model.ListMultipartUploadsRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ListPartsRequest;
import com.amazonaws.services.s3.model.PartSummary;
import com.amazonaws.services.s3.model.Permission;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.kingsrook.qqq.backend.core.actions.interfaces.CountInterface;
import com.kingsrook.qqq.backend.core.actions.interfaces.DeleteInterface;
import com.kingsrook.qqq.backend.core.actions.interfaces.InsertInterface;
import com.kingsrook.qqq.backend.core.actions.interfaces.QStorageInterface;
import com.kingsrook.qqq.backend.core.actions.interfaces.QueryInterface;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
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
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.module.filesystem.base.actions.AbstractBaseFilesystemAction;
import com.kingsrook.qqq.backend.module.filesystem.base.actions.AbstractPostReadFileCustomizer;
import com.kingsrook.qqq.backend.module.filesystem.base.actions.FilesystemTableCustomizers;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.Cardinality;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.RecordFormat;
import com.kingsrook.qqq.backend.module.filesystem.s3.S3BackendModule;
import com.kingsrook.qqq.backend.module.filesystem.s3.actions.AbstractS3Action;
import com.kingsrook.qqq.backend.module.filesystem.s3.actions.S3CountAction;
import com.kingsrook.qqq.backend.module.filesystem.s3.actions.S3DeleteAction;
import com.kingsrook.qqq.backend.module.filesystem.s3.actions.S3InsertAction;
import com.kingsrook.qqq.backend.module.filesystem.s3.actions.S3QueryAction;
import com.kingsrook.qqq.backend.module.filesystem.s3.actions.S3StorageAction;
import com.kingsrook.qqq.backend.module.filesystem.s3.model.metadata.S3BackendMetaData;
import com.kingsrook.qqq.backend.module.filesystem.s3.model.metadata.S3TableBackendDetails;
import com.kingsrook.qqq.backend.module.filesystem.s3.utils.S3Utils;
import com.kingsrook.sampleapp.metadata.FieldLabTableMetaDataProducer;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Public record and storage APIs against an owned S3 emulator. The independent
 ** SDK client inspects exact native keys and bytes, including negative cases.
 ******************************************************************************/
@Execution(ExecutionMode.SAME_THREAD)
public class SampleS3AcceptanceIT
{
   private static final String BACKEND = "sampleS3Acceptance";
   private static final String FILES = "sampleS3Files";
   private static final String CSV = "sampleS3Csv";
   private static final String JSON = "sampleS3Json";
   private static final String BUCKET = "qqq-sample-" + UUID.randomUUID();
   private static GenericContainer<?> container;
   private static AmazonS3 oracle;
   private static AmazonS3 actionClient;
   private static boolean bucketCreated;
   private String prefix;



   /*******************************************************************************
    ** Every endpoint and resource belongs to this fixture, with no shared ports.
    ******************************************************************************/
   @BeforeAll
   static void startS3()
   {
      container = new GenericContainer<>("localstack/localstack:1.4").withEnv("SERVICES", "s3")
         .withExposedPorts(4566).waitingFor(Wait.forLogMessage(".*Ready\\..*\\n", 1));
      try
      {
         container.start();
         String endpoint = "http://" + container.getHost() + ":" + container.getMappedPort(4566);
         oracle = client(endpoint);
         actionClient = client(endpoint);
         oracle.createBucket(BUCKET);
         bucketCreated = true;
         QBackendModuleDispatcher.registerBackendModule(new SampleS3Backend());
      }
      catch(RuntimeException failure)
      {
         stopS3();
         throw failure;
      }
   }



   /*******************************************************************************
    ** Close both independent SDK clients and only the owned container.
    ******************************************************************************/
   @AfterAll
   static void stopS3()
   {
      try
      {
         if(oracle != null)
         {
            try
            {
               if(bucketCreated)
               {
                  oracle.deleteBucket(BUCKET);
               }
            }
            finally
            {
               bucketCreated = false;
               oracle.shutdown();
            }
         }
      }
      finally
      {
         try
         {
            if(actionClient != null)
            {
               actionClient.shutdown();
            }
         }
         finally
         {
            if(container != null)
            {
               container.stop();
            }
         }
      }
   }



   /*******************************************************************************
    ** Fixture credentials and deadlines are explicit; provider action code is real.
    ******************************************************************************/
   private static AmazonS3 client(String endpoint)
   {
      return client(endpoint, 10000);
   }



   /*******************************************************************************
    ** The fault server uses a short timeout without changing emulator deadlines.
    ******************************************************************************/
   private static AmazonS3 client(String endpoint, int socketTimeout)
   {
      return AmazonS3ClientBuilder.standard().withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, "us-east-1"))
         .withPathStyleAccessEnabled(true).withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("test", "test")))
         .withClientConfiguration(new ClientConfiguration().withConnectionTimeout(5000).withSocketTimeout(socketTimeout).withMaxErrorRetry(0)).build();
   }



   /*******************************************************************************
    ** Reuse canonical Field Lab for MANY files and declare a sample blob table.
    ******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      prefix = "case-" + UUID.randomUUID();
      QInstance instance = SampleMetaDataProvider.defineTestInstance();
      QContext.init(instance, new QSession());
      S3BackendMetaData backend = new S3BackendMetaData().withName(BACKEND).withBucketName(BUCKET).withBasePath(prefix)
         .withAccessKey("test").withSecretKey("test").withRegion("us-east-1");
      backend.setBackendType(SampleS3Backend.class);
      instance.addBackend(backend);
      instance.addTable(new QTableMetaData().withName(FILES).withBackendName(BACKEND).withPrimaryKeyField("fileName")
         .withField(new QFieldMetaData("fileName", QFieldType.STRING))
         .withField(new QFieldMetaData("contents", QFieldType.BLOB).withIsHeavy(true))
         .withField(new QFieldMetaData("baseName", QFieldType.STRING))
         .withField(new QFieldMetaData("size", QFieldType.LONG))
         .withBackendDetails(new S3TableBackendDetails().withBasePath("files").withGlob("**").withCardinality(Cardinality.ONE)
            .withFileNameFieldName("fileName").withContentsFieldName("contents").withBaseNameFieldName("baseName").withSizeFieldName("size")));
      for(String tableName : List.of(CSV, JSON))
      {
         QTableMetaData table = instance.getTable(FieldLabTableMetaDataProducer.NAME).clone();
         table.setName(tableName);
         table.setBackendName(BACKEND);
         table.setBackendDetails(new S3TableBackendDetails().withBasePath(tableName).withGlob("*").withCardinality(Cardinality.MANY)
            .withRecordFormat(tableName.equals(CSV) ? RecordFormat.CSV : RecordFormat.JSON));
         instance.addTable(table);
      }
      instance.setHasBeenValidated(null);
      new QInstanceValidator().validate(instance);
      oracle.putObject(BUCKET, prefix + "/unrelated/sentinel", "unchanged");
   }



   /*******************************************************************************
    ** Inspect the unrelated namespace, abort owned multipart uploads and delete
    ** every object for this case even after an assertion or provider failure.
    ******************************************************************************/
   @AfterEach
   void tearDown() throws Exception
   {
      try
      {
         assertArrayEquals(bytes("unchanged"), nativeBytes(prefix + "/unrelated/sentinel"));
      }
      finally
      {
         try
         {
            for(var upload : oracle.listMultipartUploads(new ListMultipartUploadsRequest(BUCKET).withPrefix(prefix + "/")).getMultipartUploads())
            {
               oracle.abortMultipartUpload(new com.amazonaws.services.s3.model.AbortMultipartUploadRequest(BUCKET, upload.getKey(), upload.getUploadId()));
            }
            for(String key : keys())
            {
               oracle.deleteObject(BUCKET, key);
            }
            assertTrue(keys().isEmpty());
            assertTrue(oracle.listMultipartUploads(new ListMultipartUploadsRequest(BUCKET).withPrefix(prefix + "/")).getMultipartUploads().isEmpty());
         }
         finally
         {
            QContext.clear();
         }
      }
   }



   /*******************************************************************************
    ** ONE insert/query/count/delete preserve exact path, content and other keys.
    ******************************************************************************/
   @Test
   void oneRecordRoundTripAndUnsupportedUpdate() throws Exception
   {
      QRecord record = new InsertAction().executeForRecord(new InsertInput(FILES).withRecord(new QRecord()
         .withValue("fileName", "nested/report.csv").withValue("contents", bytes("id,name\n1,Alpha\n"))));
      assertTrue(record.getErrors().isEmpty(), record.getErrorsAsString());
      String key = prefix + "/files/nested/report.csv";
      assertArrayEquals(bytes("id,name\n1,Alpha\n"), nativeBytes(key));
      List<QRecord> rows = new QueryAction().execute(new QueryInput(FILES).withShouldFetchHeavyFields(true)).getRecords();
      assertEquals(1, rows.size());
      assertEquals("nested/report.csv", rows.get(0).getValueString("fileName"));
      assertEquals("report.csv", rows.get(0).getValueString("baseName"));
      assertArrayEquals(bytes("id,name\n1,Alpha\n"), rows.get(0).getValueByteArray("contents"));
      assertEquals(1, new CountAction().execute(new CountInput(FILES)).getCount());
      Map<String, String> before = snapshot();
      assertThrows(NotImplementedException.class, () -> new UpdateAction().execute(new UpdateInput(FILES)
         .withRecord(new QRecord().withValue("fileName", "nested/report.csv").withValue("contents", bytes("changed")))));
      assertEquals(before, snapshot());
      assertEquals(1, new DeleteAction().execute(new DeleteInput(FILES).withPrimaryKey("nested/report.csv")).getDeletedRecordCount());
      assertFalse(oracle.doesObjectExist(BUCKET, key));
      assertTrue(new QueryAction().execute(new QueryInput(FILES)).getRecords().isEmpty());
   }



   /*******************************************************************************
    ** Native CSV/JSON become sample Field Lab rows; post-read transformation
    ** affects parsing only and leaves the native source bytes unchanged.
    ******************************************************************************/
   @Test
   void manyCsvJsonAndPostReadHaveNativeOracles() throws Exception
   {
      oracle.putObject(BUCKET, prefix + "/" + CSV + "/rows.csv", "id,name\n11,Alpha\n12,Beta\n");
      oracle.putObject(BUCKET, prefix + "/" + JSON + "/rows.json", "[{\"id\":21,\"name\":\"Alpha\"},{\"id\":22,\"name\":\"Beta\"}]");
      for(String table : List.of(CSV, JSON))
      {
         Map<String, String> before = snapshot();
         List<QRecord> rows = new QueryAction().execute(new QueryInput(table)).getRecords();
         assertEquals(List.of("Alpha", "Beta"), rows.stream().map(row -> row.getValueString("name")).toList());
         assertEquals(2, new CountAction().execute(new CountInput(table)).getCount());
         QContext.getQInstance().getTable(table).withCustomizer(FilesystemTableCustomizers.POST_READ_FILE.getRole(), new QCodeReference(RenameAlpha.class));
         assertEquals(List.of("After read", "Beta"), new QueryAction().execute(new QueryInput(table)).getRecords().stream().map(row -> row.getValueString("name")).toList());
         assertEquals(before, snapshot());
         assertInstanceOf(NotImplementedException.class, rootCause(assertThrows(QException.class, () -> new InsertAction().execute(new InsertInput(table).withRecord(new QRecord().withValue("id", 31).withValue("name", "New"))))));
         assertInstanceOf(NotImplementedException.class, rootCause(assertThrows(QException.class, () -> new DeleteAction().execute(new DeleteInput(table).withPrimaryKey(11)))));
         assertEquals(before, snapshot());
      }
   }



   /*******************************************************************************
    ** ONE keeps JSON bytes opaque and preserves spaces, Unicode and nested names.
    ******************************************************************************/
   @Test
   void oneJsonNamesHeavySelectionAndReplacement() throws Exception
   {
      String name = "nested/雪 report.json";
      byte[] content = bytes("{\"message\":\"雪 & ice\"}\n");
      QRecord inserted = new InsertAction().executeForRecord(new InsertInput(FILES).withRecord(new QRecord()
         .withValue("fileName", name).withValue("contents", content)));
      assertTrue(inserted.getErrors().isEmpty(), inserted.getErrorsAsString());
      assertArrayEquals(content, nativeBytes(prefix + "/files/" + name));
      var filter = new QQueryFilter(new QFilterCriteria("fileName", QCriteriaOperator.EQUALS, name));
      List<QRecord> records = new QueryAction().execute(new QueryInput(FILES).withFilter(filter).withShouldFetchHeavyFields(true)).getRecords();
      assertEquals(1, records.size());
      assertEquals(name, records.get(0).getValueString("fileName"));
      assertEquals("雪 report.json", records.get(0).getValueString("baseName"));
      assertEquals((long) content.length, records.get(0).getValueLong("size"));
      assertArrayEquals(content, records.get(0).getValueByteArray("contents"));
      assertEquals(1, new CountAction().execute(new CountInput(FILES).withFilter(filter)).getCount());
      assertNull(new QueryAction().execute(new QueryInput(FILES).withFilter(filter)).getRecords().get(0).getValue("contents"));
      QContext.getQInstance().getTable(FILES).withCustomizer(FilesystemTableCustomizers.POST_READ_FILE.getRole(), new QCodeReference(RejectContents.class));
      assertArrayEquals(content, new QueryAction().execute(new QueryInput(FILES).withShouldFetchHeavyFields(true)).getRecords().get(0).getValueByteArray("contents"));
      try(OutputStream output = new StorageAction().createOutputStream(new StorageInput(FILES).withReference(name)))
      {
         output.write(bytes("{}"));
      }
      assertArrayEquals(bytes("{}"), nativeBytes(prefix + "/files/" + name));
   }



   /*******************************************************************************
    ** Both parsers retain structured scalar values and reject failed customizers.
    ******************************************************************************/
   @Test
   void manyScalarValuesNullsAndFailedCustomizer() throws Exception
   {
      oracle.putObject(BUCKET, prefix + "/" + CSV + "/values.csv", "id,name,longValue,textValue\n31,\"雪, ice\",9000000000,\"line one\nline two\"\n");
      oracle.putObject(BUCKET, prefix + "/" + JSON + "/values.json", "{\"id\":32,\"name\":\"雪, ice\",\"longValue\":9000000000,\"textValue\":null}");
      Map<String, String> before = snapshot();
      for(String table : List.of(CSV, JSON))
      {
         List<QRecord> rows = new QueryAction().execute(new QueryInput(table)).getRecords();
         assertEquals(1, rows.size());
         assertEquals(table.equals(CSV) ? 31 : 32, rows.get(0).getValueInteger("id"));
         assertEquals("雪, ice", rows.get(0).getValueString("name"));
         assertEquals(9000000000L, rows.get(0).getValueLong("longValue"));
         assertEquals(table.equals(CSV) ? "line one\nline two" : null, rows.get(0).getValueString("textValue"));
         QContext.getQInstance().getTable(table).withCustomizer(FilesystemTableCustomizers.POST_READ_FILE.getRole(), new QCodeReference(RejectContents.class));
         Throwable queryFailure = rootCause(assertThrows(QException.class, () -> new QueryAction().execute(new QueryInput(table))));
         assertInstanceOf(IllegalStateException.class, queryFailure);
         assertEquals("fixture post-read failure", queryFailure.getMessage());
         assertInstanceOf(IllegalStateException.class, rootCause(assertThrows(QException.class, () -> new CountAction().execute(new CountInput(table)))));
         assertEquals(before, snapshot());
      }
   }



   /*******************************************************************************
    ** Raw storage creates exact binary bytes, and missing reads cannot fabricate
    ** a stream or alter unrelated objects.
    ******************************************************************************/
   @Test
   void rawStorageRoundTripAndMissingRead() throws Exception
   {
      StorageAction storage = new StorageAction();
      StorageInput input = new StorageInput(FILES).withReference("raw/data.bin").withContentType("application/octet-stream");
      byte[] payload = new byte[] { 0, 1, -1, 10, 0, 42 };
      try(OutputStream output = storage.createOutputStream(input))
      {
         output.write(payload);
      }
      assertArrayEquals(payload, nativeBytes(prefix + "/files/raw/data.bin"));
      assertEquals("application/octet-stream", oracle.getObjectMetadata(BUCKET, prefix + "/files/raw/data.bin").getContentType());
      try(InputStream read = storage.getInputStream(input))
      {
         assertArrayEquals(payload, read.readAllBytes());
      }
      Map<String, String> before = snapshot();
      AmazonS3Exception missing = assertInstanceOf(AmazonS3Exception.class, rootCause(assertThrows(QException.class, () ->
      {
         try(InputStream ignored = storage.getInputStream(new StorageInput(FILES).withReference("absent.bin")))
         {
            ignored.read();
         }
      })));
      assertEquals("NoSuchKey", missing.getErrorCode());
      assertEquals(before, snapshot());
   }



   /*******************************************************************************
    ** URL and ACL metadata are native emulator evidence, not public AWS access.
    ******************************************************************************/
   @Test
   void storageUrlAndAclHaveNativeMetadataOracles() throws Exception
   {
      String key = prefix + "/files/download.bin";
      oracle.putObject(BUCKET, key, "download content");
      StorageInput input = new StorageInput(FILES).withReference("download.bin");
      StorageAction storage = new StorageAction();
      URI url = URI.create(storage.getDownloadURL(input));
      assertEquals(container.getHost(), url.getHost());
      assertEquals(container.getMappedPort(4566), url.getPort());
      assertEquals("/" + BUCKET + "/" + key, url.getPath());
      assertNull(url.getQuery());
      assertFalse(oracle.getObjectAcl(BUCKET, key).getGrantsAsList().stream().anyMatch(grant -> GroupGrantee.AllUsers.equals(grant.getGrantee())));
      storage.makePublic(input);
      assertTrue(oracle.getObjectAcl(BUCKET, key).getGrantsAsList().stream()
         .anyMatch(grant -> GroupGrantee.AllUsers.equals(grant.getGrantee()) && Permission.Read.equals(grant.getPermission())));
      assertArrayEquals(bytes("download content"), nativeBytes(key));
   }



   /*******************************************************************************
    ** Native invalid input must fail parsing without modifying stored objects.
    ******************************************************************************/
   @Test
   void malformedCsvAndJsonRefuseWithoutMutation() throws Exception
   {
      oracle.putObject(BUCKET, prefix + "/" + CSV + "/bad.csv", "id,name\n1,\"unterminated");
      oracle.putObject(BUCKET, prefix + "/" + JSON + "/bad.json", "{not-json");
      Map<String, String> before = snapshot();
      for(String table : List.of(CSV, JSON))
      {
         assertThrows(QException.class, () -> new QueryAction().execute(new QueryInput(table)));
         assertEquals(before, snapshot());
      }
   }



   /*******************************************************************************
    ** Missing prefixes are empty; missing buckets and invalid globs are errors.
    ******************************************************************************/
   @Test
   void missingBucketAndInvalidGlobDoNotMutate() throws Exception
   {
      Map<String, String> before = snapshot();
      assertTrue(new QueryAction().execute(new QueryInput(FILES)).getRecords().isEmpty());
      assertEquals(0, new CountAction().execute(new CountInput(FILES)).getCount());
      S3BackendMetaData backend = (S3BackendMetaData) QContext.getQInstance().getBackend(BACKEND);
      backend.setBucketName("absent-" + UUID.randomUUID());
      try
      {
         AmazonS3Exception failure = assertInstanceOf(AmazonS3Exception.class, rootCause(assertThrows(QException.class,
            () -> new QueryAction().execute(new QueryInput(FILES)))));
         assertEquals("NoSuchBucket", failure.getErrorCode());
      }
      finally
      {
         backend.setBucketName(BUCKET);
      }
      ((S3TableBackendDetails) QContext.getQInstance().getTable(FILES).getBackendDetails()).setGlob("[");
      assertInstanceOf(PatternSyntaxException.class, rootCause(assertThrows(QException.class,
         () -> new QueryAction().execute(new QueryInput(FILES)))));
      assertEquals(before, snapshot());
   }



   /*******************************************************************************
    ** Multipart writes are invisible until close, then have exact native bytes.
    ******************************************************************************/
   @Test
   void multipartPublishesOnCloseAndReleasesUpload() throws Exception
   {
      byte[] payload = new byte[6 * 1024 * 1024 + 17];
      for(int i = 0; i < payload.length; i++)
      {
         payload[i] = (byte) (i % 251);
      }
      String key = prefix + "/files/multipart.bin";
      try(OutputStream output = new StorageAction().createOutputStream(new StorageInput(FILES).withReference("multipart.bin")))
      {
         output.write(payload);
         assertFalse(oracle.doesObjectExist(BUCKET, key));
         var uploads = oracle.listMultipartUploads(new ListMultipartUploadsRequest(BUCKET).withPrefix(key)).getMultipartUploads();
         assertEquals(1, uploads.size());
         assertEquals(key, uploads.get(0).getKey());
      }
      assertArrayEquals(payload, nativeBytes(key));
      assertTrue(oracle.listMultipartUploads(new ListMultipartUploadsRequest(BUCKET).withPrefix(key)).getMultipartUploads().isEmpty());
   }



   /*******************************************************************************
    ** A failed second part during write must not leave the first native part.
    ******************************************************************************/
   @Test
   void failedMultipartWriteAbortsUploadAndPreservesExistingObject() throws Exception
   {
      assertFailedMultipartCleanup(MultipartFailure.WRITE_PART, true, false);
   }



   /*******************************************************************************
    ** A failed final-part upload on close must release the pending native upload.
    ******************************************************************************/
   @Test
   void failedMultipartFinalPartAbortsUploadWithoutPublishing() throws Exception
   {
      assertFailedMultipartCleanup(MultipartFailure.FINAL_PART, false, false);
   }



   /*******************************************************************************
    ** Completion fails after native parts exist; the prior object stays intact.
    ******************************************************************************/
   @Test
   void failedMultipartCompletionAbortsUploadAndPreservesExistingObject() throws Exception
   {
      assertFailedMultipartCleanup(MultipartFailure.COMPLETION, true, false);
   }



   /*******************************************************************************
    ** If abort itself fails, report both failures and prove the residual state.
    ** Independent fixture teardown still owns its final native cleanup.
    ******************************************************************************/
   @Test
   void failedMultipartAbortPreservesOriginalAndResidualParts() throws Exception
   {
      assertFailedMultipartCleanup(MultipartFailure.COMPLETION, true, true);
   }



   /*******************************************************************************
    ** Only the selected SDK operation fails. Initiation, earlier part uploads and
    ** native readback use real clients. This is not an IAM or network fault test.
    ******************************************************************************/
   private void assertFailedMultipartCleanup(MultipartFailure point, boolean existingObject, boolean failAbort) throws Exception
   {
      String reference = "failed-multipart.bin";
      String key = prefix + "/files/" + reference;
      if(existingObject)
      {
         oracle.putObject(BUCKET, key, "previous complete object");
      }
      Map<String, String> before = snapshot();
      int partSize = 5 * 1024 * 1024;
      byte[] payload = new byte[point == MultipartFailure.WRITE_PART ? 2 * partSize : 6 * 1024 * 1024 + 17];
      for(int i = 0; i < payload.length; i++)
      {
         payload[i] = (byte) (i % 251);
      }
      List<String> uploadsBeforeFailure = new ArrayList<>();
      List<List<Long>> partsBeforeFailure = new ArrayList<>();
      List<SdkClientException> injectedFailures = new ArrayList<>();
      List<String> sdkCalls = new ArrayList<>();
      AtomicInteger abortAttempts = new AtomicInteger();
      SdkClientException abortFailure = new SdkClientException("synthetic abort failure");
      AmazonS3 normalClient = actionClient;
      AmazonS3 faultClient = (AmazonS3) Proxy.newProxyInstance(AmazonS3.class.getClassLoader(), new Class<?>[] { AmazonS3.class }, (proxy, method, arguments) ->
      {
         sdkCalls.add(method.getName());
         if(method.getName().equals("abortMultipartUpload"))
         {
            AbortMultipartUploadRequest request = (AbortMultipartUploadRequest) arguments[0];
            assertEquals(BUCKET, request.getBucketName());
            assertEquals(key, request.getKey());
            assertTrue(uploadsBeforeFailure.contains(request.getUploadId()));
            abortAttempts.incrementAndGet();
            if(failAbort)
            {
               throw abortFailure;
            }
         }
         String uploadId = null;
         if(point == MultipartFailure.COMPLETION && method.getName().equals("completeMultipartUpload"))
         {
            CompleteMultipartUploadRequest request = (CompleteMultipartUploadRequest) arguments[0];
            assertEquals(BUCKET, request.getBucketName());
            assertEquals(key, request.getKey());
            uploadId = request.getUploadId();
         }
         else if(point != MultipartFailure.COMPLETION && method.getName().equals("uploadPart"))
         {
            UploadPartRequest request = (UploadPartRequest) arguments[0];
            if(request.getPartNumber() == 2)
            {
               assertEquals(BUCKET, request.getBucketName());
               assertEquals(key, request.getKey());
               uploadId = request.getUploadId();
            }
         }
         if(uploadId != null)
         {
            var uploads = oracle.listMultipartUploads(new ListMultipartUploadsRequest(BUCKET).withPrefix(key)).getMultipartUploads();
            assertEquals(1, uploads.size(), "a real upload must exist before the injected failure");
            assertEquals(key, uploads.get(0).getKey());
            assertEquals(uploadId, uploads.get(0).getUploadId());
            List<Long> sizes = oracle.listParts(new ListPartsRequest(BUCKET, key, uploadId)).getParts().stream().map(PartSummary::getSize).toList();
            assertEquals(point == MultipartFailure.COMPLETION ? List.of((long) partSize, (long) payload.length - partSize) : List.of((long) partSize), sizes);
            assertEquals(before, snapshot(), "pending parts must not replace an existing object or publish a new one");
            uploadsBeforeFailure.add(uploadId);
            partsBeforeFailure.add(sizes);
            SdkClientException failure = new SdkClientException("synthetic multipart " + point + " failure");
            injectedFailures.add(failure);
            throw failure;
         }
         try
         {
            return method.invoke(normalClient, arguments);
         }
         catch(InvocationTargetException failure)
         {
            throw failure.getCause();
         }
      });
      OutputStream output;
      SdkClientException originalFailure;
      try
      {
         actionClient = faultClient;
         output = new StorageAction().createOutputStream(new StorageInput(FILES).withReference(reference));
         originalFailure = assertThrows(SdkClientException.class, () ->
         {
            try(output)
            {
               output.write(payload);
            }
         });
         assertEquals("synthetic multipart " + point + " failure", originalFailure.getMessage());
         assertSame(injectedFailures.get(0), originalFailure);
      }
      finally
      {
         actionClient = normalClient;
      }
      assertFalse(uploadsBeforeFailure.isEmpty(), "the failure must occur after native upload and part creation");
      assertEquals(before, snapshot(), "an explicit SDK failure must preserve the prior native object state");
      var remaining = oracle.listMultipartUploads(new ListMultipartUploadsRequest(BUCKET).withPrefix(key)).getMultipartUploads();
      List<List<Long>> remainingPartSizes = new ArrayList<>();
      for(var upload : remaining)
      {
         assertEquals(key, upload.getKey());
         assertTrue(uploadsBeforeFailure.contains(upload.getUploadId()));
         remainingPartSizes.add(oracle.listParts(new ListPartsRequest(BUCKET, key, upload.getUploadId())).getParts().stream().map(PartSummary::getSize).toList());
      }
      assertEquals(failAbort ? 1 : 0, remaining.size(), point + ": native part sizes before failure=" + partsBeforeFailure
         + "; remaining native part sizes=" + remainingPartSizes + "; native objects unchanged; caller already attempted close");
      if(failAbort)
      {
         assertEquals(List.of(partsBeforeFailure.get(0)), remainingPartSizes, "an unsuccessful abort must not be reported as cleanup");
      }
      assertEquals(1, abortAttempts.get(), "attempt cleanup exactly once for the owned upload");
      assertEquals(failAbort ? 1 : 0, originalFailure.getSuppressed().length);
      if(failAbort)
      {
         assertSame(abortFailure, originalFailure.getSuppressed()[0]);
      }
      List<String> atFailure = List.copyOf(sdkCalls);
      assertDoesNotThrow(output::close);
      assertDoesNotThrow(output::close);
      assertThrows(IOException.class, () -> output.write(1));
      assertThrows(IOException.class, () -> output.write(new byte[] { 2 }));
      assertThrows(IOException.class, () -> output.write(new byte[0], 0, 0));
      assertEquals(atFailure, sdkCalls, "close/reuse cannot retry, abort again or republish failed work");
      assertEquals(before, snapshot());
   }



   /*******************************************************************************
    ** Distinct stream boundaries retain the same real multipart setup and oracle.
    ******************************************************************************/
   private enum MultipartFailure
   {
      WRITE_PART,
      FINAL_PART,
      COMPLETION
   }



   /*******************************************************************************
    ** A caller copy failure can publish a partial replacement when close runs.
    ** This documents non-atomic storage, not a claim of rollback or cancellation.
    ******************************************************************************/
   @Test
   void failedCopyPublishesPrefixAndPreservesOtherKeys() throws Exception
   {
      String key = prefix + "/files/partial.bin";
      oracle.putObject(BUCKET, key, "previous complete data");
      Map<String, String> before = snapshot();
      IOException expected = new IOException("synthetic source failure");
      InputStream source = new InputStream()
      {
         private boolean delivered;

         @Override
         public int read() throws IOException
         {
            throw expected;
         }

         @Override
         public int read(byte[] data, int offset, int length) throws IOException
         {
            if(delivered)
            {
               throw expected;
            }
            delivered = true;
            System.arraycopy(bytes("prefix"), 0, data, offset, 6);
            return 6;
         }
      };
      assertEquals(expected, assertThrows(IOException.class, () ->
      {
         try(source; OutputStream output = new StorageAction().createOutputStream(new StorageInput(FILES).withReference("partial.bin")))
         {
            source.transferTo(output);
         }
      }));
      assertArrayEquals(bytes("prefix"), nativeBytes(key));
      before.put(key, Base64.getEncoder().encodeToString(bytes("prefix")));
      assertEquals(before, snapshot());
      assertTrue(oracle.listMultipartUploads(new ListMultipartUploadsRequest(BUCKET).withPrefix(prefix + "/")).getMultipartUploads().isEmpty());
   }



   /*******************************************************************************
    ** Scripted 403 responses prove SDK/provider propagation, never AWS IAM rules.
    ******************************************************************************/
   @Test
   void permissionAndCredentialResponsesPropagateWithoutNativeWrites() throws Exception
   {
      Map<String, String> before = snapshot();
      for(String code : List.of("AccessDenied", "InvalidAccessKeyId", "SignatureDoesNotMatch"))
      {
         byte[] response = bytes("<Error><Code>" + code + "</Code><Message>fixture rejection</Message></Error>");
         AtomicInteger requests = new AtomicInteger();
         HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
         server.createContext("/", exchange ->
         {
            try(exchange)
            {
               exchange.getRequestBody().readAllBytes();
               requests.incrementAndGet();
               exchange.getResponseHeaders().add("Content-Type", "application/xml");
               exchange.sendResponseHeaders(403, response.length);
               exchange.getResponseBody().write(response);
            }
         });
         server.start();
         AmazonS3 normalClient = actionClient;
         AmazonS3 faultClient = client("http://127.0.0.1:" + server.getAddress().getPort());
         try
         {
            actionClient = faultClient;
            AmazonS3Exception read = assertInstanceOf(AmazonS3Exception.class, rootCause(assertThrows(QException.class,
               () -> new QueryAction().execute(new QueryInput(FILES)))));
            assertEquals(403, read.getStatusCode());
            assertEquals(code, read.getErrorCode());
            AmazonS3Exception write = assertThrows(AmazonS3Exception.class, () ->
            {
               try(OutputStream output = new StorageAction().createOutputStream(new StorageInput(FILES).withReference("denied.bin")))
               {
                  output.write(bytes("must fail"));
               }
            });
            assertEquals(403, write.getStatusCode());
            assertEquals(code, write.getErrorCode());
            assertEquals(2, requests.get());
         }
         finally
         {
            actionClient = normalClient;
            faultClient.shutdown();
            server.stop(0);
         }
         assertEquals(before, snapshot());
      }
   }



   /*******************************************************************************
    ** A consumed request with no response must surface the configured timeout.
    ******************************************************************************/
   @Test
   void readTimeoutPropagatesWithoutNativeMutation() throws Exception
   {
      Map<String, String> before = snapshot();
      CountDownLatch arrived = new CountDownLatch(1);
      CountDownLatch release = new CountDownLatch(1);
      var executor = Executors.newSingleThreadExecutor();
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.setExecutor(executor);
      server.createContext("/", exchange ->
      {
         try(exchange)
         {
            exchange.getRequestBody().readAllBytes();
            arrived.countDown();
            try
            {
               release.await(10, TimeUnit.SECONDS);
            }
            catch(InterruptedException e)
            {
               Thread.currentThread().interrupt();
            }
         }
      });
      server.start();
      AmazonS3 normalClient = actionClient;
      AmazonS3 faultClient = client("http://127.0.0.1:" + server.getAddress().getPort(), 500);
      try
      {
         actionClient = faultClient;
         long start = System.nanoTime();
         assertInstanceOf(SocketTimeoutException.class, rootCause(assertThrows(QException.class,
            () -> new QueryAction().execute(new QueryInput(FILES)))));
         assertEquals(0, arrived.getCount(), "the owned server must actually receive the request");
         assertTrue(Duration.ofNanos(System.nanoTime() - start).compareTo(Duration.ofSeconds(5)) < 0);
      }
      finally
      {
         actionClient = normalClient;
         release.countDown();
         faultClient.shutdown();
         server.stop(0);
         executor.shutdownNow();
         assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
      }
      assertEquals(before, snapshot());
   }



   /*******************************************************************************
    ** Inspect the underlying diagnostic rather than accepting any wrapper error.
    ******************************************************************************/
   private static Throwable rootCause(Throwable failure)
   {
      while(failure.getCause() != null)
      {
         failure = failure.getCause();
      }
      return failure;
   }



   /*******************************************************************************
    ** OutputStream offsets describe a slice length, independently of its start.
    ******************************************************************************/
   @Test
   void rawStorageOffsetWritesExactSlice() throws Exception
   {
      try(OutputStream output = new StorageAction().createOutputStream(new StorageInput(FILES).withReference("slice.bin")))
      {
         output.write(bytes("0123456789"), 2, 5);
      }
      assertEquals("23456", new String(nativeBytes(prefix + "/files/slice.bin"), StandardCharsets.UTF_8));
   }



   /*******************************************************************************
    ** Hand-authored UTF-8 bytes are independent of provider conversion logic.
    ******************************************************************************/
   private static byte[] bytes(String value)
   {
      return value.getBytes(StandardCharsets.UTF_8);
   }



   /*******************************************************************************
    ** Read native data with the independent SDK client and close every stream.
    ******************************************************************************/
   private static byte[] nativeBytes(String key) throws Exception
   {
      try(S3Object object = oracle.getObject(BUCKET, key))
      {
         return object.getObjectContent().readAllBytes();
      }
   }



   /*******************************************************************************
    ** Paginate all native keys within this test's namespace.
    ******************************************************************************/
   private List<String> keys()
   {
      List<String> keys = new ArrayList<>();
      ListObjectsV2Request request = new ListObjectsV2Request().withBucketName(BUCKET).withPrefix(prefix + "/");
      ListObjectsV2Result result;
      do
      {
         result = oracle.listObjectsV2(request);
         result.getObjectSummaries().forEach(object -> keys.add(object.getKey()));
         request.setContinuationToken(result.getNextContinuationToken());
      }
      while(result.isTruncated());
      return keys;
   }



   /*******************************************************************************
    ** Compare every owned native key and byte, not a QQQ query of its own writes.
    ******************************************************************************/
   private Map<String, String> snapshot() throws Exception
   {
      Map<String, String> result = new TreeMap<>();
      for(String key : keys())
      {
         result.put(key, Base64.getEncoder().encodeToString(nativeBytes(key)));
      }
      return result;
   }



   /*******************************************************************************
    ** The sample backend changes only the SDK endpoint, with a distinct type.
    ******************************************************************************/
   public static class SampleS3Backend extends S3BackendModule
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public String getBackendType()
      {
         return BACKEND;
      }



      /***************************************************************************
       **
       ***************************************************************************/
      private <T extends AbstractS3Action> T configure(T action)
      {
         S3Utils utils = new S3Utils();
         utils.setAmazonS3(actionClient);
         action.setS3Utils(utils);
         return action;
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public QueryInterface getQueryInterface()
      {
         return configure(new S3QueryAction());
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public CountInterface getCountInterface()
      {
         return configure(new S3CountAction());
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public InsertInterface getInsertInterface()
      {
         return configure(new S3InsertAction());
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public DeleteInterface getDeleteInterface()
      {
         return configure(new S3DeleteAction());
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public QStorageInterface getStorageInterface()
      {
         return configure(new S3StorageAction());
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public AbstractBaseFilesystemAction<S3ObjectSummary> getActionBase()
      {
         return configure(new AbstractS3Action());
      }
   }



   /*******************************************************************************
    ** A real configured post-read customizer, exercised for both parser formats.
    ******************************************************************************/
   public static class RenameAlpha extends AbstractPostReadFileCustomizer
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public String customizeFileContents(String contents)
      {
         return contents.replace("Alpha", "After read");
      }
   }


   /*******************************************************************************
    ** A configured failure must abort MANY parsing; ONE never invokes this hook.
    ******************************************************************************/
   public static class RejectContents extends AbstractPostReadFileCustomizer
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public String customizeFileContents(String contents)
      {
         throw new IllegalStateException("fixture post-read failure");
      }
   }

}
