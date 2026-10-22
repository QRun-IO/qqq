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


import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** OutputStream implementation that knows how to stream data into a new S3 file.
 **
 ** This will be done using a multipart-upload if the contents are > 5MB - else
 ** just a 1-time-call to PutObject
 *******************************************************************************/
public class S3UploadOutputStream extends OutputStream
{
   private static final QLogger LOG = QLogger.getLogger(S3UploadOutputStream.class);

   private final S3Client s3Client;
   private final String   bucketName;
   private final String   key;
   private final String   contentType;

   private byte[] buffer = new byte[5 * 1024 * 1024];
   private int    offset = 0;

   private String              uploadId       = null;
   private List<CompletedPart> completedParts = null;

   private boolean isClosed = false;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public S3UploadOutputStream(S3Client s3Client, String bucketName, String key, String contentType)
   {
      this.s3Client = s3Client;
      this.bucketName = bucketName;
      this.key = key;
      this.contentType = contentType;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void write(int b) throws IOException
   {
      ensureOpen();
      buffer[offset] = (byte) b;
      offset++;

      uploadIfNeeded();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void uploadIfNeeded()
   {
      try
      {
         if(offset == buffer.length)
         {
            //////////////////////////////////////////
            // start or continue a multipart upload //
            //////////////////////////////////////////
            if(uploadId == null)
            {
               LOG.info("Initiating a multipart upload", logPair("key", key));
               CreateMultipartUploadRequest.Builder createMultipartUploadRequest = CreateMultipartUploadRequest.builder().bucket(bucketName).key(key);
               if(this.contentType != null)
               {
                  createMultipartUploadRequest.contentType(this.contentType);
               }
               uploadId = s3Client.createMultipartUpload(createMultipartUploadRequest.build()).uploadId();

               completedParts = new ArrayList<>();
            }

            uploadPart(buffer.length, false);

            //////////////////
            // reset buffer //
            //////////////////
            offset = 0;
         }
      }
      catch(SdkException failure)
      {
         abortAfterFailure(failure);
         throw failure;
      }
   }



   /*******************************************************************************
    ** Upload the first length bytes of the buffer as the next part.
    *******************************************************************************/
   private void uploadPart(int length, boolean isFinalPart)
   {
      int partNumber = completedParts.size() + 1;
      LOG.info("Uploading a part", logPair("key", key), logPair("isFinalPart", isFinalPart), logPair("partNumber", partNumber));

      UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
         .bucket(bucketName)
         .key(key)
         .uploadId(uploadId)
         .partNumber(partNumber)
         .contentLength((long) length)
         .build();

      UploadPartResponse uploadPartResponse = s3Client.uploadPart(uploadPartRequest, RequestBody.fromBytes(Arrays.copyOf(buffer, length)));
      completedParts.add(CompletedPart.builder().partNumber(partNumber).eTag(uploadPartResponse.eTag()).build());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void write(byte[] b, int off, int len) throws IOException
   {
      Objects.checkFromIndexSize(off, len, b.length);
      ensureOpen();
      int bytesToWrite = len;

      while(bytesToWrite > buffer.length - offset)
      {
         int size = buffer.length - offset;
         System.arraycopy(b, off, buffer, offset, size);
         offset = buffer.length;
         uploadIfNeeded();
         off += size;
         bytesToWrite -= size;
      }

      int size = bytesToWrite;
      System.arraycopy(b, off, buffer, offset, size);
      offset += size;
      uploadIfNeeded();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void close() throws IOException
   {
      if(isClosed)
      {
         LOG.debug("Redundant call to close an already-closed S3UploadOutputStream.  Returning with noop.", logPair("key", key));
         return;
      }

      isClosed = true;
      try
      {
         if(uploadId != null)
         {
            if(offset > 0)
            {
               //////////////////////////////////////////////////
               // if there's a final part to upload, do it now //
               //////////////////////////////////////////////////
               uploadPart(offset, true);
            }

            s3Client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
               .bucket(bucketName)
               .key(key)
               .uploadId(uploadId)
               .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build())
               .build());
         }
         else
         {
            LOG.info("Putting object (non-multipart)", logPair("key", key), logPair("length", offset));
            PutObjectRequest.Builder putObjectRequest = PutObjectRequest.builder()
               .bucket(bucketName)
               .key(key)
               .contentLength((long) offset);

            if(this.contentType != null)
            {
               putObjectRequest.contentType(this.contentType);
            }

            s3Client.putObject(putObjectRequest.build(), RequestBody.fromBytes(Arrays.copyOf(buffer, offset)));
         }
      }
      catch(SdkException failure)
      {
         abortAfterFailure(failure);
         throw failure;
      }
   }



   /*******************************************************************************
    ** A closed or failed upload must not buffer or publish any more data.
    *******************************************************************************/
   private void ensureOpen() throws IOException
   {
      if(isClosed)
      {
         throw new IOException("Cannot write to a closed S3 upload stream.");
      }
   }



   /*******************************************************************************
    ** Abort only this stream's initiated upload, preserving the original failure
    ** if cleanup also fails. Closing again must not retry either operation.
    *******************************************************************************/
   private void abortAfterFailure(SdkException failure)
   {
      isClosed = true;
      if(uploadId != null)
      {
         try
         {
            s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder().bucket(bucketName).key(key).uploadId(uploadId).build());
         }
         catch(RuntimeException abortFailure)
         {
            if(abortFailure != failure)
            {
               failure.addSuppressed(abortFailure);
            }
         }
      }
   }

}
