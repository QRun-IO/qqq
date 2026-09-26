/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import com.kingsrook.qqq.backend.module.filesystem.s3.BaseS3Test;
import org.junit.jupiter.api.Test;


/*******************************************************************************
 ** Unit test for S3UploadOutputStream
 *******************************************************************************/
class S3UploadOutputStreamTest extends BaseS3Test
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws IOException
   {
      String bucketName = BaseS3Test.BUCKET_NAME;
      String key        = "uploader-tests/" + Instant.now().toString() + ".txt";

      // S3UploadOutputStream outputStream = new S3UploadOutputStream(amazonS3, bucketName, key);
      // FileOutputStream outputStream = new FileOutputStream("/tmp/file.json");
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      outputStream.write("[\n1".getBytes(StandardCharsets.UTF_8));
      for(int i = 2; i <= 1_000_000; i++)
      {
         outputStream.write((",\n" + i).getBytes(StandardCharsets.UTF_8));
      }
      outputStream.write("\n]\n".getBytes(StandardCharsets.UTF_8));
      outputStream.close();

      S3UploadOutputStream s3UploadOutputStream = new S3UploadOutputStream(getS3Utils().getAmazonS3(), bucketName, key, null);
      s3UploadOutputStream.write(outputStream.toByteArray(), 0, 5 * 1024 * 1024);
      s3UploadOutputStream.write(outputStream.toByteArray(), 0, 3 * 1024 * 1024);
      s3UploadOutputStream.write(outputStream.toByteArray(), 0, 3 * 1024 * 1024);
      s3UploadOutputStream.close();
   }

}
