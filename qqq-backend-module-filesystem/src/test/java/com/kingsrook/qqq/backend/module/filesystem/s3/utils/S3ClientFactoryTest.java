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

package com.kingsrook.qqq.backend.module.filesystem.s3.utils;


import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.module.filesystem.TestUtils;
import com.kingsrook.qqq.backend.module.filesystem.s3.BaseS3Test;
import com.kingsrook.qqq.backend.module.filesystem.s3.actions.S3QueryAction;
import com.kingsrook.qqq.backend.module.filesystem.s3.actions.S3StorageAction;
import com.kingsrook.qqq.backend.module.filesystem.s3.model.S3ObjectSummary;
import com.kingsrook.qqq.backend.module.filesystem.s3.model.metadata.S3BackendMetaData;
import com.kingsrook.qqq.backend.module.filesystem.s3.model.metadata.S3ChecksumMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.Credentials;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for S3ClientFactory: S3-compatible connection settings (endpoint,
 ** path-style, checksum mode, TLS) and the listing cap, against MinIO.
 *******************************************************************************/
class S3ClientFactoryTest extends BaseS3Test
{
   /////////////////////////////////////////////////////////////
   // a throwaway self-signed CA, for trust-store parsing only //
   /////////////////////////////////////////////////////////////
   private static final String TEST_CA_PEM = """
      -----BEGIN CERTIFICATE-----
      MIIBiDCCAS+gAwIBAgIUNgWu7bBTUAWahKbRyBjTTrOPY+MwCgYIKoZIzj0EAwIw
      GTEXMBUGA1UEAwwOUVFRIFMzIFRlc3QgQ0EwIBcNMjYxMDEyMTczNDQ1WhgPMjEy
      NjA5MTgxNzM0NDVaMBkxFzAVBgNVBAMMDlFRUSBTMyBUZXN0IENBMFkwEwYHKoZI
      zj0CAQYIKoZIzj0DAQcDQgAEqqEtNgEJTGRK12VJgf/equHnhk8swGvTGD3p++Us
      hLePvEyq+r1g4LhmSWe62j6MEljycgfU96Q0WKr3rlNsAKNTMFEwHQYDVR0OBBYE
      FL4OMQqA+I7jLi22MAYfuJZDNBLmMB8GA1UdIwQYMBaAFL4OMQqA+I7jLi22MAYf
      uJZDNBLmMA8GA1UdEwEB/wQFMAMBAf8wCgYIKoZIzj0EAwIDRwAwRAIgRNh+s8H2
      iqTgKuJDgt8tczTVJyh2x897ePFqXMHFmm4CIC/1WkYhWHj/Q/c1pGl9L7o24PBV
      Vq4g/jhONjM4Z6uh
      -----END CERTIFICATE-----
      """;



   /*******************************************************************************
    ** A custom endpoint defaults to WHEN_REQUIRED; AWS (no endpoint) keeps the SDK
    ** default; an explicit setting always wins.
    *******************************************************************************/
   @Test
   void testEffectiveChecksumMode()
   {
      assertEquals(S3ChecksumMode.WHEN_SUPPORTED, S3ClientFactory.getEffectiveChecksumMode(new S3BackendMetaData()));
      assertEquals(S3ChecksumMode.WHEN_REQUIRED, S3ClientFactory.getEffectiveChecksumMode(new S3BackendMetaData().withEndpoint("http://minio:9000")));
      assertEquals(S3ChecksumMode.WHEN_SUPPORTED, S3ClientFactory.getEffectiveChecksumMode(new S3BackendMetaData().withEndpoint("http://minio:9000").withChecksumMode(S3ChecksumMode.WHEN_SUPPORTED)));
      assertEquals(S3ChecksumMode.WHEN_REQUIRED, S3ClientFactory.getEffectiveChecksumMode(new S3BackendMetaData().withChecksumMode(S3ChecksumMode.WHEN_REQUIRED)));
   }



   /*******************************************************************************
    ** null means the default cap; zero or negative means no cap.
    *******************************************************************************/
   @Test
   void testEffectiveMaxListedObjects()
   {
      assertEquals(S3ClientFactory.DEFAULT_MAX_LISTED_OBJECTS, S3ClientFactory.getEffectiveMaxListedObjects(new S3BackendMetaData()));
      assertEquals(7, S3ClientFactory.getEffectiveMaxListedObjects(new S3BackendMetaData().withMaxListedObjects(7)));
      assertNull(S3ClientFactory.getEffectiveMaxListedObjects(new S3BackendMetaData().withMaxListedObjects(0)));
      assertNull(S3ClientFactory.getEffectiveMaxListedObjects(new S3BackendMetaData().withMaxListedObjects(-1)));
   }



   /*******************************************************************************
    ** One shared client per distinct settings; the cache key never holds secrets.
    *******************************************************************************/
   @Test
   void testClientCache()
   {
      S3BackendMetaData backend = getMinioBackendMetaData();
      assertSame(S3ClientFactory.getClient(backend), S3ClientFactory.getClient(getMinioBackendMetaData()));
      assertNotSame(S3ClientFactory.getClient(backend), S3ClientFactory.getClient(getMinioBackendMetaData().withSocketTimeoutMillis(12_345)));

      String cacheKey = S3ClientFactory.buildCacheKey(backend);
      assertThat(cacheKey).matches("[0-9a-f]{64}").doesNotContain(MINIO_SECRET_KEY).doesNotContain(MINIO_ACCESS_KEY);
      assertThat(S3ClientFactory.buildCacheKey(getMinioBackendMetaData().withSecretKey("another-secret"))).isNotEqualTo(cacheKey);
   }



   /*******************************************************************************
    ** Both checksum modes work against MinIO, for a single put and a multipart
    ** upload, over a custom endpoint with path-style addressing.
    *******************************************************************************/
   @Test
   void testBothChecksumModesAgainstMinio() throws Exception
   {
      for(S3ChecksumMode checksumMode : S3ChecksumMode.values())
      {
         try(S3Client s3Client = S3ClientFactory.buildClient(getMinioBackendMetaData().withChecksumMode(checksumMode)))
         {
            String small = "checksum-tests/" + checksumMode + "/" + UUID.randomUUID() + ".txt";
            s3Client.putObject(PutObjectRequest.builder().bucket(BUCKET_NAME).key(small).build(), RequestBody.fromString("hello " + checksumMode, StandardCharsets.UTF_8));
            assertEquals("hello " + checksumMode, getObjectAsString(BUCKET_NAME, small));

            String large   = "checksum-tests/" + checksumMode + "/" + UUID.randomUUID() + ".bin";
            byte[] payload = new byte[6 * 1024 * 1024 + 3];
            Arrays.fill(payload, (byte) 7);
            try(S3UploadOutputStream outputStream = new S3UploadOutputStream(s3Client, BUCKET_NAME, large, "application/octet-stream"))
            {
               outputStream.write(payload);
            }
            assertArrayEquals(payload, getObjectBytes(BUCKET_NAME, large));
            assertTrue(listMultipartUploads(BUCKET_NAME, large).isEmpty());
         }
      }
   }



   /*******************************************************************************
    ** A backend configured only by meta data (endpoint, path-style, keys), with no
    ** injected client, works end to end through the QQQ actions.
    *******************************************************************************/
   @Test
   void testEndpointConfiguredBackendEndToEnd() throws QException
   {
      configureTestBackendForMinio(null);

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_BLOB_S3);
      QueryOutput queryOutput = new S3QueryAction().execute(queryInput);
      assertEquals(3, queryOutput.getRecords().size(), "Expected the 3 blobs, listed through the meta-data-built client");

      S3StorageAction storageAction = new S3StorageAction();
      StorageInput    storageInput  = new StorageInput(TestUtils.TABLE_NAME_BLOB_S3).withReference("BLOB-1.txt");
      assertEquals("Hello, Blob", new String(readAllBytes(storageAction.getInputStream(storageInput)), StandardCharsets.UTF_8));
      assertEquals(getMinioEndpoint() + "/" + BUCKET_NAME + "/" + TEST_FOLDER + "/blobs/BLOB-1.txt", new S3StorageAction().getDownloadURL(storageInput),
         "path-style download URL on the custom endpoint");
   }



   /*******************************************************************************
    ** Listings above maxListedObjects fail clearly; zero disables the cap.
    *******************************************************************************/
   @Test
   void testListingCap() throws QException
   {
      configureTestBackendForMinio(2);

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_BLOB_S3);
      assertThatThrownBy(() -> new S3QueryAction().execute(queryInput))
         .rootCause()
         .hasMessageContaining("more than 2 matching objects")
         .hasMessageContaining("maxListedObjects");

      configureTestBackendForMinio(0);
      assertEquals(3, new S3QueryAction().execute(queryInput).getRecords().size());

      S3Utils s3Utils = getS3Utils();
      assertThatThrownBy(() -> s3Utils.listObjectsInBucketMatchingGlob(BUCKET_NAME, TEST_FOLDER, "**", null, null, 4))
         .hasMessageContaining("more than 4 matching objects");
      List<S3ObjectSummary> all = s3Utils.listObjectsInBucketMatchingGlob(BUCKET_NAME, TEST_FOLDER, "**", null, null, null);
      assertEquals(7, all.size());
      assertTrue(all.stream().allMatch(summary -> BUCKET_NAME.equals(summary.getBucketName()) && summary.getLastModified() != null && summary.getETag() != null));
   }



   /*******************************************************************************
    ** A private CA's PEM is trusted in addition to the JVM defaults; bad PEM is
    ** rejected clearly; clients build with custom CA and with TLS verify off.
    *******************************************************************************/
   @Test
   void testTlsSettings()
   {
      TrustManager[] trustManagers = S3ClientFactory.buildTrustManagers(TEST_CA_PEM);
      assertEquals(1, trustManagers.length);
      X509TrustManager trustManager = (X509TrustManager) trustManagers[0];
      assertTrue(Arrays.stream(trustManager.getAcceptedIssuers()).anyMatch(issuer -> issuer.getSubjectX500Principal().getName().contains("QQQ S3 Test CA")));
      assertTrue(trustManager.getAcceptedIssuers().length > 1, "the JVM default trust anchors are kept too");

      assertThatThrownBy(() -> S3ClientFactory.buildTrustManagers("not a certificate"))
         .isInstanceOf(IllegalArgumentException.class);

      try(S3Client withCa = S3ClientFactory.buildClient(getMinioBackendMetaData().withCaCertificatesPem(TEST_CA_PEM));
          S3Client noVerify = S3ClientFactory.buildClient(getMinioBackendMetaData().withTlsVerify(false)))
      {
         ////////////////////////////////////////////////////////////////////
         // the test endpoint is plain http, so both must still talk to it //
         ////////////////////////////////////////////////////////////////////
         assertTrue(withCa.listBuckets().buckets().stream().anyMatch(bucket -> BUCKET_NAME.equals(bucket.name())));
         assertTrue(noVerify.listBuckets().buckets().stream().anyMatch(bucket -> BUCKET_NAME.equals(bucket.name())));
      }
   }



   /*******************************************************************************
    ** Credentials precedence: provider code, then keys with a session token
    ** (temporary credentials), then plain keys, then the SDK default chain.
    *******************************************************************************/
   @Test
   void testCredentialsProvider()
   {
      AwsCredentials basic = S3ClientFactory.buildCredentialsProvider(new S3BackendMetaData().withAccessKey("ak").withSecretKey("sk")).resolveCredentials();
      assertInstanceOf(AwsBasicCredentials.class, basic);
      assertEquals("ak", basic.accessKeyId());

      AwsCredentials        session            = S3ClientFactory.buildCredentialsProvider(new S3BackendMetaData().withAccessKey("ak").withSecretKey("sk").withSessionToken("tok")).resolveCredentials();
      AwsSessionCredentials sessionCredentials = assertInstanceOf(AwsSessionCredentials.class, session);
      assertEquals("ak", sessionCredentials.accessKeyId());
      assertEquals("sk", sessionCredentials.secretAccessKey());
      assertEquals("tok", sessionCredentials.sessionToken());

      assertThatThrownBy(() -> S3ClientFactory.buildCredentialsProvider(new S3BackendMetaData().withAccessKey("ak").withSessionToken("secret-token-value")))
         .isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("sessionToken")
         .hasMessageNotContaining("secret-token-value");

      assertInstanceOf(DefaultCredentialsProvider.class, S3ClientFactory.buildCredentialsProvider(new S3BackendMetaData()));

      S3BackendMetaData withProvider = new S3BackendMetaData().withAccessKey("ignored").withSecretKey("ignored")
         .withCredentialsProviderCode(new QCodeReference(TestCredentialsProvider.class));
      assertInstanceOf(TestCredentialsProvider.class, S3ClientFactory.buildCredentialsProvider(withProvider));
      assertEquals("provider-ak", S3ClientFactory.buildCredentialsProvider(withProvider).resolveCredentials().accessKeyId());

      assertThatThrownBy(() -> S3ClientFactory.buildCredentialsProvider(new S3BackendMetaData().withCredentialsProviderCode(new QCodeReference(String.class))))
         .isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("AwsCredentialsProvider");
   }



   /*******************************************************************************
    ** The session token and the provider code are part of the client cache key.
    *******************************************************************************/
   @Test
   void testCacheKeyIncludesSessionCredentials()
   {
      String cacheKey = S3ClientFactory.buildCacheKey(getMinioBackendMetaData());
      assertThat(S3ClientFactory.buildCacheKey(getMinioBackendMetaData().withSessionToken("token-1"))).isNotEqualTo(cacheKey);
      assertThat(S3ClientFactory.buildCacheKey(getMinioBackendMetaData().withSessionToken("token-1")))
         .isNotEqualTo(S3ClientFactory.buildCacheKey(getMinioBackendMetaData().withSessionToken("token-2")))
         .doesNotContain("token-1");
      assertThat(S3ClientFactory.buildCacheKey(getMinioBackendMetaData().withCredentialsProviderCode(new QCodeReference(TestCredentialsProvider.class)))).isNotEqualTo(cacheKey);
   }



   /*******************************************************************************
    ** enrich() resolves the session token like the keys, including from a mounted
    ** secret file.
    *******************************************************************************/
   @Test
   void testEnrichInterpretsSessionToken(@TempDir Path secretsRoot) throws Exception
   {
      Path tokenFile = Files.writeString(secretsRoot.resolve("session-token"), "file-token\n", StandardCharsets.UTF_8);
      System.setProperty(QMetaDataVariableInterpreter.FILE_REFERENCE_ROOTS_PROPERTY, secretsRoot.toString());
      System.setProperty("qqq.test.s3.accessKey", "prop-ak");
      try
      {
         S3BackendMetaData backend = new S3BackendMetaData()
            .withAccessKey("${prop.qqq.test.s3.accessKey}")
            .withSecretKey("plain-sk")
            .withSessionToken("${file." + tokenFile.toAbsolutePath() + "}");
         backend.enrich();
         assertEquals("prop-ak", backend.getAccessKey());
         assertEquals("plain-sk", backend.getSecretKey());
         assertEquals("file-token", backend.getSessionToken());
      }
      finally
      {
         System.clearProperty(QMetaDataVariableInterpreter.FILE_REFERENCE_ROOTS_PROPERTY);
         System.clearProperty("qqq.test.s3.accessKey");
      }
   }



   /*******************************************************************************
    ** Temporary credentials from MinIO's STS (AssumeRole): a client built with
    ** the session token reads and writes; the same keys without the token, or
    ** with a wrong one, are refused, so the token is really sent.
    *******************************************************************************/
   @Test
   void testSessionCredentialsAgainstMinio()
   {
      Credentials credentials;
      try(StsClient stsClient = StsClient.builder()
         .endpointOverride(URI.create(getMinioEndpoint()))
         .region(Region.US_EAST_1)
         .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(MINIO_ACCESS_KEY, MINIO_SECRET_KEY)))
         .build())
      {
         credentials = stsClient.assumeRole(AssumeRoleRequest.builder().roleArn("arn:xxx:xxx:xxx:xxxx").roleSessionName("qqq-test").durationSeconds(900).build()).credentials();
      }

      S3BackendMetaData sessionBackend = getMinioBackendMetaData()
         .withAccessKey(credentials.accessKeyId())
         .withSecretKey(credentials.secretAccessKey())
         .withSessionToken(credentials.sessionToken());

      String   key           = "session-tests/" + UUID.randomUUID() + ".txt";
      S3Client sessionClient = S3ClientFactory.getClient(sessionBackend);
      sessionClient.putObject(PutObjectRequest.builder().bucket(BUCKET_NAME).key(key).build(), RequestBody.fromString("via session credentials", StandardCharsets.UTF_8));
      assertEquals("via session credentials", getObjectAsString(BUCKET_NAME, key));

      try(S3Client noToken = S3ClientFactory.buildClient(getMinioBackendMetaData().withAccessKey(credentials.accessKeyId()).withSecretKey(credentials.secretAccessKey()));
          S3Client badToken = S3ClientFactory.buildClient(getMinioBackendMetaData().withAccessKey(credentials.accessKeyId()).withSecretKey(credentials.secretAccessKey()).withSessionToken("not-the-token")))
      {
         assertThatThrownBy(noToken::listBuckets).isInstanceOf(S3Exception.class);
         assertThatThrownBy(badToken::listBuckets).isInstanceOf(S3Exception.class);
      }
   }



   /*******************************************************************************
    ** An application credentials provider, as named by credentialsProviderCode.
    *******************************************************************************/
   public static class TestCredentialsProvider implements AwsCredentialsProvider
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public AwsCredentials resolveCredentials()
      {
         return (AwsBasicCredentials.create("provider-ak", "provider-sk"));
      }
   }



   /*******************************************************************************
    ** Point the test instance's s3 backend at MinIO by meta data alone.
    *******************************************************************************/
   private void configureTestBackendForMinio(Integer maxListedObjects)
   {
      S3BackendMetaData backend = (S3BackendMetaData) QContext.getQInstance().getBackend(TestUtils.BACKEND_NAME_S3);
      backend.withEndpoint(getMinioEndpoint())
         .withPathStyleAccess(true)
         .withAccessKey(MINIO_ACCESS_KEY)
         .withSecretKey(MINIO_SECRET_KEY)
         .withRegion("us-east-1")
         .withMaxListedObjects(maxListedObjects);
   }

}
