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

package com.kingsrook.qqq.backend.module.filesystem.s3;


import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import com.kingsrook.qqq.backend.module.filesystem.BaseTest;
import com.kingsrook.qqq.backend.module.filesystem.s3.model.metadata.S3BackendMetaData;
import com.kingsrook.qqq.backend.module.filesystem.s3.utils.S3ClientFactory;
import com.kingsrook.qqq.backend.module.filesystem.s3.utils.S3Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListMultipartUploadsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.MultipartUpload;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;


/*******************************************************************************
 ** Base class for tests that work with S3.  Runs one MinIO container (via
 ** Testcontainers) for the whole test JVM, and talks to it through the same
 ** AWS SDK v2 client factory the module uses: a custom endpoint, path-style
 ** addressing and static credentials.
 *******************************************************************************/
public class BaseS3Test extends BaseTest
{
   public static final String BUCKET_NAME = "localstack-test-bucket";
   public static final String TEST_FOLDER = "test-files";
   public static final String SUB_FOLDER  = "sub-folder";

   public static final String BUCKET_NAME_FOR_SANS_PREFIX_BACKEND = "localstack-test-bucket-sans-prefix";

   ///////////////////////////////////////////////////////////////////////////////
   // minio/minio is no longer published on Docker Hub (and quay.io/minio needs  //
   // auth), so tests use Chainguard's MinIO build, pinned by digest.  Override  //
   // with -Dqqq.test.minioImage=... (any image whose entrypoint is minio).      //
   ///////////////////////////////////////////////////////////////////////////////
   public static final String MINIO_IMAGE      = System.getProperty("qqq.test.minioImage", "cgr.dev/chainguard/minio@sha256:dc455cdd4db6e390b1b26514f15ff37681a777bf8dab41aac445a0259f751e56");
   public static final String MINIO_ACCESS_KEY = "qqq-test-access-key";
   public static final String MINIO_SECRET_KEY = "qqq-test-secret-key";

   private static GenericContainer<?> minioContainer;



   /*******************************************************************************
    ** Start (once per JVM) and return the MinIO container.
    *******************************************************************************/
   public static synchronized GenericContainer<?> getMinioContainer()
   {
      if(minioContainer == null)
      {
         GenericContainer<?> container = new GenericContainer<>(MINIO_IMAGE)
            .withEnv("MINIO_ROOT_USER", MINIO_ACCESS_KEY)
            .withEnv("MINIO_ROOT_PASSWORD", MINIO_SECRET_KEY)
            .withCommand("server", "/tmp/minio-data")
            .withExposedPorts(9000)
            .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000).withStartupTimeout(Duration.ofMinutes(2)));
         container.start();
         minioContainer = container;
      }

      return (minioContainer);
   }



   /*******************************************************************************
    ** The MinIO endpoint URL, e.g., http://localhost:49153
    *******************************************************************************/
   public static String getMinioEndpoint()
   {
      GenericContainer<?> container = getMinioContainer();
      return ("http://" + container.getHost() + ":" + container.getMappedPort(9000));
   }



   /*******************************************************************************
    ** Backend meta data for the test MinIO (no bucket set).
    *******************************************************************************/
   public static S3BackendMetaData getMinioBackendMetaData()
   {
      return (new S3BackendMetaData()
         .withEndpoint(getMinioEndpoint())
         .withPathStyleAccess(true)
         .withAccessKey(MINIO_ACCESS_KEY)
         .withSecretKey(MINIO_SECRET_KEY)
         .withRegion("us-east-1"));
   }



   /*******************************************************************************
    ** The shared S3 client for the test MinIO.
    *******************************************************************************/
   public static S3Client getTestS3Client()
   {
      return (S3ClientFactory.getClient(getMinioBackendMetaData()));
   }



   /*******************************************************************************
    ** Before each unit test, get the test bucket into a known state
    *******************************************************************************/
   @BeforeEach
   public void beforeEach()
   {
      createBucket(BUCKET_NAME);

      putObject(BUCKET_NAME, "0.csv", getCSVHeader());
      putObject(BUCKET_NAME, TEST_FOLDER + "/1.csv", getCSVData1());
      putObject(BUCKET_NAME, TEST_FOLDER + "/2.csv", getCSVData2());
      putObject(BUCKET_NAME, TEST_FOLDER + "/text.txt", "This is a text test");
      putObject(BUCKET_NAME, TEST_FOLDER + "/" + SUB_FOLDER + "/3.csv", getCSVData3());
      putObject(BUCKET_NAME, TEST_FOLDER + "/blobs/BLOB-1.txt", "Hello, Blob");
      putObject(BUCKET_NAME, TEST_FOLDER + "/blobs/BLOB-2.txt", "Hi, Bob");
      putObject(BUCKET_NAME, TEST_FOLDER + "/blobs/BLOB 3.md", "# Hi, MD"); // this one, with a space in the name, tripped up listObjectsInBucketMatchingGlob's path matching at one time

      createBucket(BUCKET_NAME_FOR_SANS_PREFIX_BACKEND);
      putObject(BUCKET_NAME_FOR_SANS_PREFIX_BACKEND, "BLOB-1.txt", "Hello, Blob");
      putObject(BUCKET_NAME_FOR_SANS_PREFIX_BACKEND, "BLOB-2.txt", "Hi, Bob");
      putObject(BUCKET_NAME_FOR_SANS_PREFIX_BACKEND, "BLOB-3.md", "# Hi, MD");
   }



   /*******************************************************************************
    ** After each unit test, clean up the bucket
    *******************************************************************************/
   @AfterEach
   public void afterEach()
   {
      for(String bucketName : List.of(BUCKET_NAME, BUCKET_NAME_FOR_SANS_PREFIX_BACKEND))
      {
         deleteBucketAndContents(bucketName);
      }
   }



   /*******************************************************************************
    ** Access the test S3 client.
    *******************************************************************************/
   protected S3Client getS3Client()
   {
      return (getTestS3Client());
   }



   /*******************************************************************************
    ** Access the S3Utils object, with the test s3 client.
    *******************************************************************************/
   protected S3Utils getS3Utils()
   {
      S3Utils s3Utils = new S3Utils();
      s3Utils.setS3Client(getS3Client());
      return (s3Utils);
   }



   /*******************************************************************************
    ** Create a bucket if it does not exist.
    *******************************************************************************/
   public static void createBucket(String bucketName)
   {
      if(!doesBucketExist(bucketName))
      {
         getTestS3Client().createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
      }
   }



   /*******************************************************************************
    ** Does a bucket exist?
    *******************************************************************************/
   public static boolean doesBucketExist(String bucketName)
   {
      try
      {
         getTestS3Client().headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
         return (true);
      }
      catch(NoSuchBucketException e)
      {
         return (false);
      }
      catch(S3Exception e)
      {
         if(e.statusCode() == 404)
         {
            return (false);
         }
         throw (e);
      }
   }



   /*******************************************************************************
    ** Delete every object in a bucket (all pages), then the bucket.
    *******************************************************************************/
   public static void deleteBucketAndContents(String bucketName)
   {
      if(!doesBucketExist(bucketName))
      {
         return;
      }

      S3Client s3Client = getTestS3Client();
      for(S3Object s3Object : s3Client.listObjectsV2Paginator(ListObjectsV2Request.builder().bucket(bucketName).build()).contents())
      {
         s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(s3Object.key()).build());
      }

      for(MultipartUpload upload : s3Client.listMultipartUploads(ListMultipartUploadsRequest.builder().bucket(bucketName).build()).uploads())
      {
         s3Client.abortMultipartUpload(builder -> builder.bucket(bucketName).key(upload.key()).uploadId(upload.uploadId()));
      }

      s3Client.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build());
   }



   /*******************************************************************************
    ** Put a string as an object.
    *******************************************************************************/
   public static void putObject(String bucketName, String key, String contents)
   {
      getTestS3Client().putObject(PutObjectRequest.builder().bucket(bucketName).key(key).build(), RequestBody.fromString(contents, StandardCharsets.UTF_8));
   }



   /*******************************************************************************
    ** Does an object exist?
    *******************************************************************************/
   public static boolean doesObjectExist(String bucketName, String key)
   {
      try
      {
         getTestS3Client().headObject(HeadObjectRequest.builder().bucket(bucketName).key(key).build());
         return (true);
      }
      catch(NoSuchKeyException e)
      {
         return (false);
      }
      catch(S3Exception e)
      {
         if(e.statusCode() == 404)
         {
            return (false);
         }
         throw (e);
      }
   }



   /*******************************************************************************
    ** Get an object's metadata.
    *******************************************************************************/
   public static HeadObjectResponse headObject(String bucketName, String key)
   {
      return (getTestS3Client().headObject(HeadObjectRequest.builder().bucket(bucketName).key(key).build()));
   }



   /*******************************************************************************
    ** Get an object's bytes.
    *******************************************************************************/
   public static byte[] getObjectBytes(String bucketName, String key)
   {
      ResponseBytes<GetObjectResponse> responseBytes = getTestS3Client().getObjectAsBytes(GetObjectRequest.builder().bucket(bucketName).key(key).build());
      return (responseBytes.asByteArray());
   }



   /*******************************************************************************
    ** Get an object's contents as a string.
    *******************************************************************************/
   public static String getObjectAsString(String bucketName, String key)
   {
      return (new String(getObjectBytes(bucketName, key), StandardCharsets.UTF_8));
   }



   /*******************************************************************************
    ** The in-progress multipart uploads under a key prefix.
    *******************************************************************************/
   public static List<MultipartUpload> listMultipartUploads(String bucketName, String prefix)
   {
      return (getTestS3Client().listMultipartUploads(ListMultipartUploadsRequest.builder().bucket(bucketName).prefix(prefix).build()).uploads());
   }



   /*******************************************************************************
    ** Read all bytes of a stream, unchecked.
    *******************************************************************************/
   protected static byte[] readAllBytes(InputStream inputStream)
   {
      try(inputStream)
      {
         return (inputStream.readAllBytes());
      }
      catch(IOException e)
      {
         throw (new UncheckedIOException(e));
      }
   }



   /*******************************************************************************
    ** Access a string of CSV test data.
    *******************************************************************************/
   protected String getCSVHeader()
   {
      return ("""
         "id","createDate","modifyDate","firstName","lastName","birthDate","email"
         """);
   }



   /*******************************************************************************
    ** Access a string of CSV test data.
    *******************************************************************************/
   protected String getCSVData1()
   {
      return (getCSVHeader() + """
         "1","2021-10-26 14:39:37","2021-10-26 14:39:37","John","Doe","1981-01-01","john@kingsrook.com"
         "2","2022-06-17 14:52:59","2022-06-17 14:52:59","Jane","Smith","1982-02-02","jane@kingsrook.com"
         """);
   }



   /*******************************************************************************
    ** Access a string of CSV test data.
    *******************************************************************************/
   protected String getCSVData2()
   {
      return (getCSVHeader() + """
         "3","2021-11-27 15:40:38","2021-11-27 15:40:38","Homer","S","1983-03-03","homer.s@kingsrook.com"
         "4","2022-07-18 15:53:00","2022-07-18 15:53:00","Marge","S","1984-04-04","marge.s@kingsrook.com"
         "5","2022-11-11 12:00:00","2022-11-12 13:00:00","Bart","S","1985-05-05","bart.s@kingsrook.com"
         """);
   }



   /*******************************************************************************
    ** Access a string of CSV test data.
    *******************************************************************************/
   protected String getCSVData3()
   {
      return (getCSVHeader() + """
         "6","2022-06-20 15:31:02","2022-06-20 15:31:02","Lisa","S","1986-06-06","lisa.s@kingsrook.com"
         """);
   }

}
