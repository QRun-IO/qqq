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
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import com.kingsrook.qqq.backend.module.filesystem.s3.BaseS3Test;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
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

      // FileOutputStream outputStream = new FileOutputStream("/tmp/file.json");
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      outputStream.write("[\n1".getBytes(StandardCharsets.UTF_8));
      for(int i = 2; i <= 1_000_000; i++)
      {
         outputStream.write((",\n" + i).getBytes(StandardCharsets.UTF_8));
      }
      outputStream.write("\n]\n".getBytes(StandardCharsets.UTF_8));
      outputStream.close();

      S3UploadOutputStream s3UploadOutputStream = new S3UploadOutputStream(getS3Utils().getS3Client(), bucketName, key, null);
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
      try(S3UploadOutputStream output = new S3UploadOutputStream(getS3Client(), BUCKET_NAME, key, null))
      {
         output.write(source, 0, 0);
         output.write(source, 2, 0);
         output.write(source, source.length, 0);
         output.write(new byte[0], 0, 0);
         assertFalse(doesObjectExist(BUCKET_NAME, key));
         assertTrue(listMultipartUploads(BUCKET_NAME, key).isEmpty());
         output.write(source);
         output.write(source, source.length, 0);
      }
      assertArrayEquals(source, getObjectBytes(BUCKET_NAME, key));
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
      try(S3UploadOutputStream output = new S3UploadOutputStream(getS3Client(), BUCKET_NAME, key, null))
      {
         output.write("before-".getBytes(StandardCharsets.UTF_8));
         assertThrows(NullPointerException.class, () -> output.write(null, 0, 0));
         assertThrows(IndexOutOfBoundsException.class, () -> output.write(source, -1, 1));
         assertThrows(IndexOutOfBoundsException.class, () -> output.write(source, 0, -1));
         assertThrows(IndexOutOfBoundsException.class, () -> output.write(source, source.length + 1, 0));
         assertThrows(IndexOutOfBoundsException.class, () -> output.write(source, Integer.MAX_VALUE, 1));
         assertThrows(IndexOutOfBoundsException.class, () -> output.write(source, 0, source.length + 1));
         assertTrue(listMultipartUploads(BUCKET_NAME, key).isEmpty(),
            "an invalid large slice must not publish even one multipart part");
         assertThrows(IndexOutOfBoundsException.class, () -> output.write(source, 1, Integer.MAX_VALUE));
         assertFalse(doesObjectExist(BUCKET_NAME, key));
         output.write("after".getBytes(StandardCharsets.UTF_8));
      }
      assertArrayEquals(retained, getObjectBytes(BUCKET_NAME, key));
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
    ** Failed part writes abort once and cannot be retried through close or write.
    ******************************************************************************/
   @Test
   void testPartFailureIsTerminal() throws Exception
   {
      assertFailedStreamIsTerminal(false, false);
   }



   /*******************************************************************************
    ** Completion errors retain their identity and stop further SDK operations.
    ******************************************************************************/
   @Test
   void testCompletionFailureIsTerminal() throws Exception
   {
      assertFailedStreamIsTerminal(true, false);
   }



   /*******************************************************************************
    ** Abort failure supplements, rather than replaces, the original SDK failure.
    ******************************************************************************/
   @Test
   void testAbortFailureIsSuppressed() throws Exception
   {
      assertFailedStreamIsTerminal(false, true);
      assertFailedStreamIsTerminal(true, true);
   }



   /*******************************************************************************
    ** Successful multipart completion preserves metadata and redundant close.
    ******************************************************************************/
   @Test
   void testSuccessfulCloseIsTerminalAndIdempotent() throws Exception
   {
      String key = "uploader-tests/" + UUID.randomUUID();
      byte[] payload = new byte[5 * 1024 * 1024 + 19];
      Arrays.fill(payload, (byte) 23);
      S3UploadOutputStream output = new S3UploadOutputStream(getS3Client(), BUCKET_NAME, key, "application/octet-stream");
      try(output)
      {
         output.write(payload);
      }
      assertDoesNotThrow(output::close);
      assertThrows(IOException.class, () -> output.write(1));
      assertThrows(IOException.class, () -> output.write(new byte[] { 1 }, 0, 1));
      assertDoesNotThrow(output::close);
      assertArrayEquals(payload, getObjectBytes(BUCKET_NAME, key));
      assertEquals("application/octet-stream", headObject(BUCKET_NAME, key).contentType());
      assertTrue(listMultipartUploads(BUCKET_NAME, key).isEmpty());
   }



   /*******************************************************************************
    ** Deterministic SDK faults isolate exception identity and terminal behavior;
    ** sample tests separately prove native upload existence and cleanup.
    ******************************************************************************/
   private void assertFailedStreamIsTerminal(boolean failCompletion, boolean failAbort) throws Exception
   {
      String key = "failure-key";
      SdkClientException original = SdkClientException.create("original SDK failure");
      SdkClientException abortFailure = SdkClientException.create("abort SDK failure");
      List<String> calls = new ArrayList<>();
      S3Client client = (S3Client) Proxy.newProxyInstance(S3Client.class.getClassLoader(), new Class<?>[] { S3Client.class }, (proxy, method, arguments) ->
      {
         calls.add(method.getName());
         switch(method.getName())
         {
            case "createMultipartUpload":
               return CreateMultipartUploadResponse.builder().uploadId("owned-upload").build();
            case "uploadPart":
               if(!failCompletion)
               {
                  throw original;
               }
               return UploadPartResponse.builder().eTag("fixture-etag").build();
            case "completeMultipartUpload":
               throw original;
            case "abortMultipartUpload":
               AbortMultipartUploadRequest request = (AbortMultipartUploadRequest) arguments[0];
               assertEquals(BUCKET_NAME, request.bucket());
               assertEquals(key, request.key());
               assertEquals("owned-upload", request.uploadId());
               if(failAbort)
               {
                  throw abortFailure;
               }
               return null;
            default:
               throw new AssertionError("Unexpected SDK operation: " + method.getName());
         }
      });
      S3UploadOutputStream output = new S3UploadOutputStream(client, BUCKET_NAME, key, null);
      byte[] part = new byte[5 * 1024 * 1024];
      if(failCompletion)
      {
         output.write(part);
         assertSame(original, assertThrows(SdkClientException.class, output::close));
      }
      else
      {
         assertSame(original, assertThrows(SdkClientException.class, () -> output.write(part)));
      }
      assertEquals(failCompletion ? List.of("createMultipartUpload", "uploadPart", "completeMultipartUpload", "abortMultipartUpload")
         : List.of("createMultipartUpload", "uploadPart", "abortMultipartUpload"), calls);
      assertEquals(failAbort ? 1 : 0, original.getSuppressed().length);
      if(failAbort)
      {
         assertSame(abortFailure, original.getSuppressed()[0]);
      }
      List<String> atFailure = List.copyOf(calls);
      assertDoesNotThrow(output::close);
      assertDoesNotThrow(output::close);
      assertThrows(IOException.class, () -> output.write(1));
      assertThrows(IOException.class, () -> output.write(new byte[] { 2 }));
      assertThrows(IOException.class, () -> output.write(new byte[0], 0, 0));
      assertEquals(atFailure, calls, "terminal operations cannot retry, abort again or republish");
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
      try(S3UploadOutputStream output = new S3UploadOutputStream(getS3Client(), BUCKET_NAME, key, null))
      {
         output.write(prefix);
         output.write(source, start, length);
      }
      assertArrayEquals(expected.toByteArray(), getObjectBytes(BUCKET_NAME, key));
      assertTrue(listMultipartUploads(BUCKET_NAME, key).isEmpty());
   }
}
