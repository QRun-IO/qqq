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

package com.kingsrook.qqq.backend.module.filesystem.s3.utils;


import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.module.filesystem.s3.BaseS3Test;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 **
 *******************************************************************************/
public class S3UtilsTest extends BaseS3Test
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testListObjectsInBucketAtPath() throws QException
   {
      S3Utils s3Utils = getS3Utils();
      assertEquals(3, s3Utils.listObjectsInBucketMatchingGlob(BUCKET_NAME, TEST_FOLDER, "/").size(), "Expected # of s3 objects without subfolders");
      assertEquals(2, s3Utils.listObjectsInBucketMatchingGlob(BUCKET_NAME, TEST_FOLDER, "/*.csv").size(), "Expected # of csv s3 objects without subfolders");
      assertEquals(1, s3Utils.listObjectsInBucketMatchingGlob(BUCKET_NAME, TEST_FOLDER, "/*.txt").size(), "Expected # of txt s3 objects without subfolders");
      assertEquals(0, s3Utils.listObjectsInBucketMatchingGlob(BUCKET_NAME, TEST_FOLDER, "/*.pdf").size(), "Expected # of pdf s3 objects without subfolders");
      assertEquals(7, s3Utils.listObjectsInBucketMatchingGlob(BUCKET_NAME, TEST_FOLDER, "/**").size(), "Expected # of s3 objects with subfolders");
      assertEquals(3, s3Utils.listObjectsInBucketMatchingGlob(BUCKET_NAME, "/" + TEST_FOLDER, "/").size(), "With leading slash");
      assertEquals(3, s3Utils.listObjectsInBucketMatchingGlob(BUCKET_NAME, "/" + TEST_FOLDER, "").size(), "Without trailing slash");
      assertEquals(3, s3Utils.listObjectsInBucketMatchingGlob(BUCKET_NAME, "//" + TEST_FOLDER, "//").size(), "With multiple leading and trailing slashes");
      assertEquals(1, s3Utils.listObjectsInBucketMatchingGlob(BUCKET_NAME, TEST_FOLDER + "/" + SUB_FOLDER, "").size(), "Just in the subfolder non-recursive");
      assertEquals(1, s3Utils.listObjectsInBucketMatchingGlob(BUCKET_NAME, TEST_FOLDER + "/" + SUB_FOLDER, "/**").size(), "Just in the subfolder recursive");
      assertEquals(1, s3Utils.listObjectsInBucketMatchingGlob(BUCKET_NAME, TEST_FOLDER + "//" + SUB_FOLDER, "/**").size(), "Just in the subfolder recursive, multi /");
      assertEquals(0, s3Utils.listObjectsInBucketMatchingGlob(BUCKET_NAME, "not-a-real-path/", "").size(), "In a non-existing folder");
      assertEquals(1, s3Utils.listObjectsInBucketMatchingGlob(BUCKET_NAME, "/", "").size(), "In the root folder, specified as /");
      assertEquals(1, s3Utils.listObjectsInBucketMatchingGlob(BUCKET_NAME, "//", "").size(), "In the root folder, specified as multiple /s");
      assertEquals(1, s3Utils.listObjectsInBucketMatchingGlob(BUCKET_NAME, "", "").size(), "In the root folder, specified as empty-string");
      assertEquals(8, s3Utils.listObjectsInBucketMatchingGlob(BUCKET_NAME, "/", "**").size(), "In the root folder, specified as /, and recursively");
      assertEquals(8, s3Utils.listObjectsInBucketMatchingGlob(BUCKET_NAME, "", "**").size(), "In the root folder, specified as empty-string, and recursively");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testGetObjectAsInputStream() throws Exception
   {
      S3Utils               s3Utils           = getS3Utils();
      List<S3ObjectSummary> s3ObjectSummaries = s3Utils.listObjectsInBucketMatchingGlob(BUCKET_NAME, "test-files", "");
      S3ObjectSummary       s3ObjectSummary   = s3ObjectSummaries.stream().filter(o -> o.getKey().contains("1.csv")).findAny().get();
      InputStream           inputStream       = s3Utils.getObjectAsInputStream(s3ObjectSummary);
      String                csvFromS3         = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

      assertEquals(getCSVData1(), csvFromS3, "File from S3 should match expected content");
   }

}