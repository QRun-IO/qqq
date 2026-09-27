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
import java.util.Arrays;
import java.util.UUID;
import com.amazonaws.services.s3.model.ListMultipartUploadsRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.kingsrook.qqq.backend.module.filesystem.s3.BaseS3Test;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


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



   /*******************************************************************************
    ** The length is a count, including when it is smaller than the source offset.
    ******************************************************************************/
   @Test
   void testNonzeroOffsetSlices() throws Exception
   {
      byte[] source = "0123456789".getBytes(StandardCharsets.UTF_8);
      assertSlice(source, 2, 5, new byte[0]);
      assertSlice(source, 7, 2, new byte[0]);
   }



   /*******************************************************************************
    ** Empty slices at any valid position leave the accumulated bytes unchanged.
    ******************************************************************************/
   @Test
   void testZeroLengthSlices() throws Exception
   {
      String key = "uploader-tests/" + UUID.randomUUID();
      byte[] source = "0123456789".getBytes(StandardCharsets.UTF_8);
      try(S3UploadOutputStream output = new S3UploadOutputStream(getAmazonS3(), BUCKET_NAME, key, null))
      {
         output.write(source, 0, 0);
         output.write(source, 2, 0);
         output.write(source, source.length, 0);
         output.write(new byte[0], 0, 0);
         assertFalse(getAmazonS3().doesObjectExist(BUCKET_NAME, key));
         assertTrue(getAmazonS3().listMultipartUploads(new ListMultipartUploadsRequest(BUCKET_NAME).withPrefix(key)).getMultipartUploads().isEmpty());
         output.write(source);
         output.write(source, source.length, 0);
      }
      try(S3Object object = getAmazonS3().getObject(BUCKET_NAME, key))
      {
         assertArrayEquals(source, object.getObjectContent().readAllBytes());
      }
   }



   /*******************************************************************************
    ** Reject the entire slice before copying or creating multipart uploads.
    ******************************************************************************/
   @Test
   void testInvalidSlicesHaveNoSideEffects() throws Exception
   {
      String key = "uploader-tests/" + UUID.randomUUID();
      byte[] source = new byte[5 * 1024 * 1024];
      byte[] retained = "before-after".getBytes(StandardCharsets.UTF_8);
      try(S3UploadOutputStream output = new S3UploadOutputStream(getAmazonS3(), BUCKET_NAME, key, null))
      {
         output.write("before-".getBytes(StandardCharsets.UTF_8));
         assertThrows(NullPointerException.class, () -> output.write(null, 0, 0));
         assertThrows(IndexOutOfBoundsException.class, () -> output.write(source, -1, 1));
         assertThrows(IndexOutOfBoundsException.class, () -> output.write(source, 0, -1));
         assertThrows(IndexOutOfBoundsException.class, () -> output.write(source, source.length + 1, 0));
         assertThrows(IndexOutOfBoundsException.class, () -> output.write(source, Integer.MAX_VALUE, 1));
         assertThrows(IndexOutOfBoundsException.class, () -> output.write(source, 0, source.length + 1));
         assertTrue(getAmazonS3().listMultipartUploads(new ListMultipartUploadsRequest(BUCKET_NAME).withPrefix(key)).getMultipartUploads().isEmpty(),
            "an invalid large slice must not publish even one multipart part");
         assertThrows(IndexOutOfBoundsException.class, () -> output.write(source, 1, Integer.MAX_VALUE));
         assertFalse(getAmazonS3().doesObjectExist(BUCKET_NAME, key));
         output.write("after".getBytes(StandardCharsets.UTF_8));
      }
      try(S3Object object = getAmazonS3().getObject(BUCKET_NAME, key))
      {
         assertArrayEquals(retained, object.getObjectContent().readAllBytes());
      }
   }



   /*******************************************************************************
    ** Cross multipart boundaries from empty and already populated buffers.
    ******************************************************************************/
   @Test
   void testNonzeroSlicesCrossMultipartBoundaries() throws Exception
   {
      byte[] source = new byte[6 * 1024 * 1024 + 37];
      for(int i = 0; i < source.length; i++)
      {
         source[i] = (byte) (i % 251);
      }
      assertSlice(source, 11, source.length - 19, new byte[0]);
      assertSlice(source, 17, source.length - 31, "already buffered".getBytes(StandardCharsets.UTF_8));
   }



   /*******************************************************************************
    ** Compare the public stream's stored object with an independently built slice.
    ******************************************************************************/
   private void assertSlice(byte[] source, int start, int length, byte[] prefix) throws Exception
   {
      String key = "uploader-tests/" + UUID.randomUUID();
      ByteArrayOutputStream expected = new ByteArrayOutputStream();
      expected.write(prefix);
      expected.write(Arrays.copyOfRange(source, start, start + length));
      try(S3UploadOutputStream output = new S3UploadOutputStream(getAmazonS3(), BUCKET_NAME, key, null))
      {
         output.write(prefix);
         output.write(source, start, length);
      }
      try(S3Object object = getAmazonS3().getObject(BUCKET_NAME, key))
      {
         assertArrayEquals(expected.toByteArray(), object.getObjectContent().readAllBytes());
      }
      assertTrue(getAmazonS3().listMultipartUploads(new ListMultipartUploadsRequest(BUCKET_NAME).withPrefix(key)).getMultipartUploads().isEmpty());
   }
}
