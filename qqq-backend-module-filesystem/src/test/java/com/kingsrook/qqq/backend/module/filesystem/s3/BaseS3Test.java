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

package com.kingsrook.qqq.backend.module.filesystem.s3;


import java.util.List;
import cloud.localstack.ServiceName;
import cloud.localstack.awssdkv1.TestUtils;
import cloud.localstack.docker.LocalstackDockerExtension;
import cloud.localstack.docker.annotation.LocalstackDockerProperties;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.kingsrook.qqq.backend.module.filesystem.BaseTest;
import com.kingsrook.qqq.backend.module.filesystem.s3.utils.S3Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;


/*******************************************************************************
 ** Base class for tests that want to be able to work with localstack s3.
 *******************************************************************************/
@ExtendWith(LocalstackDockerExtension.class)
@LocalstackDockerProperties(useSingleDockerContainer = true, services = { ServiceName.S3 }, portEdge = "2960", portElasticSearch = "2961", imageTag = "1.4")
@DisabledIfEnvironmentVariable(named = "CIRCLECI", matches = "true")
public class BaseS3Test extends BaseTest
{
   public static final String BUCKET_NAME = "localstack-test-bucket";
   public static final String TEST_FOLDER = "test-files";
   public static final String SUB_FOLDER  = "sub-folder";

   public static final String BUCKET_NAME_FOR_SANS_PREFIX_BACKEND = "localstack-test-bucket-sans-prefix";


   /*******************************************************************************
    ** Before each unit test, get the test bucket into a known state
    *******************************************************************************/
   @BeforeEach
   public void beforeEach()
   {
      AmazonS3 amazonS3 = getAmazonS3();

      amazonS3.createBucket(BUCKET_NAME);
      amazonS3.putObject(BUCKET_NAME, "0.csv", getCSVHeader());
      amazonS3.putObject(BUCKET_NAME, TEST_FOLDER + "/1.csv", getCSVData1());
      amazonS3.putObject(BUCKET_NAME, TEST_FOLDER + "/2.csv", getCSVData2());
      amazonS3.putObject(BUCKET_NAME, TEST_FOLDER + "/text.txt", "This is a text test");
      amazonS3.putObject(BUCKET_NAME, TEST_FOLDER + "/" + SUB_FOLDER + "/3.csv", getCSVData3());
      amazonS3.putObject(BUCKET_NAME, TEST_FOLDER + "/blobs/BLOB-1.txt", "Hello, Blob");
      amazonS3.putObject(BUCKET_NAME, TEST_FOLDER + "/blobs/BLOB-2.txt", "Hi, Bob");
      amazonS3.putObject(BUCKET_NAME, TEST_FOLDER + "/blobs/BLOB 3.md", "# Hi, MD"); // this one, with a space in the name, tripped up listObjectsInBucketMatchingGlob's path matching at one time

      amazonS3.createBucket(BUCKET_NAME_FOR_SANS_PREFIX_BACKEND);
      amazonS3.putObject(BUCKET_NAME_FOR_SANS_PREFIX_BACKEND, "BLOB-1.txt", "Hello, Blob");
      amazonS3.putObject(BUCKET_NAME_FOR_SANS_PREFIX_BACKEND, "BLOB-2.txt", "Hi, Bob");
      amazonS3.putObject(BUCKET_NAME_FOR_SANS_PREFIX_BACKEND, "BLOB-3.md", "# Hi, MD");
   }



   /*******************************************************************************
    ** After each unit test, clean up the bucket
    *******************************************************************************/
   @AfterEach
   public void afterEach()
   {
      AmazonS3 amazonS3 = getAmazonS3();

      for(String bucketName : List.of(BUCKET_NAME, BUCKET_NAME_FOR_SANS_PREFIX_BACKEND))
      {
         if(amazonS3.doesBucketExistV2(bucketName))
         {
            ////////////////////////
            // todo - paginate... //
            ////////////////////////
            for(S3ObjectSummary objectSummary : amazonS3.listObjectsV2(bucketName).getObjectSummaries())
            {
               amazonS3.deleteObject(bucketName, objectSummary.getKey());
            }
            amazonS3.deleteBucket(bucketName);
         }
      }
   }



   /*******************************************************************************
    ** Access a localstack-configured s3 client.
    *******************************************************************************/
   protected AmazonS3 getAmazonS3()
   {
      return (TestUtils.getClientS3());
   }



   /*******************************************************************************
    ** Access the S3Utils object, with localstack-configured s3 client.
    *******************************************************************************/
   protected S3Utils getS3Utils()
   {
      S3Utils s3Utils = new S3Utils();
      s3Utils.setAmazonS3(getAmazonS3());
      return (s3Utils);
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
