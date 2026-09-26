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


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.kingsrook.qqq.backend.core.logging.QLogger;
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

   private final AmazonS3 amazonS3;
   private final String   bucketName;
   private final String   key;
   private final String   contentType;

   private byte[] buffer = new byte[5 * 1024 * 1024];
   private int    offset = 0;

   private InitiateMultipartUploadResult initiateMultipartUploadResult = null;
   private List<UploadPartResult>        uploadPartResultList          = null;

   private boolean isClosed = false;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public S3UploadOutputStream(AmazonS3 amazonS3, String bucketName, String key, String contentType)
   {
      this.amazonS3 = amazonS3;
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
      buffer[offset] = (byte) b;
      offset++;

      uploadIfNeeded();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void uploadIfNeeded()
   {
      ObjectMetadata objectMetadata = null;
      if(this.contentType != null)
      {
         objectMetadata = new ObjectMetadata();
         objectMetadata.setContentType(this.contentType);
      }

      if(offset == buffer.length)
      {
         //////////////////////////////////////////
         // start or continue a multipart upload //
         //////////////////////////////////////////
         if(initiateMultipartUploadResult == null)
         {
            LOG.info("Initiating a multipart upload", logPair("key", key));
            initiateMultipartUploadResult = amazonS3.initiateMultipartUpload(new InitiateMultipartUploadRequest(bucketName, key, objectMetadata));

            uploadPartResultList = new ArrayList<>();
         }

         LOG.info("Uploading a part", logPair("key", key), logPair("partNumber", uploadPartResultList.size() + 1));
         UploadPartRequest uploadPartRequest = new UploadPartRequest()
            .withUploadId(initiateMultipartUploadResult.getUploadId())
            .withPartNumber(uploadPartResultList.size() + 1)
            .withInputStream(new ByteArrayInputStream(buffer))
            .withBucketName(bucketName)
            .withKey(key)
            .withPartSize(buffer.length)
            .withObjectMetadata(objectMetadata);

         uploadPartResultList.add(amazonS3.uploadPart(uploadPartRequest));

         //////////////////
         // reset buffer //
         //////////////////
         offset = 0;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void write(byte[] b, int off, int len) throws IOException
   {
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

      int size = len - off;
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

      ObjectMetadata objectMetadata = null;
      if(this.contentType != null)
      {
         objectMetadata = new ObjectMetadata();
         objectMetadata.setContentType(this.contentType);
      }

      if(initiateMultipartUploadResult != null)
      {
         if(offset > 0)
         {
            //////////////////////////////////////////////////
            // if there's a final part to upload, do it now //
            //////////////////////////////////////////////////
            LOG.info("Uploading a part", logPair("key", key), logPair("isFinalPart", true), logPair("partNumber", uploadPartResultList.size() + 1));
            UploadPartRequest uploadPartRequest = new UploadPartRequest()
               .withUploadId(initiateMultipartUploadResult.getUploadId())
               .withPartNumber(uploadPartResultList.size() + 1)
               .withInputStream(new ByteArrayInputStream(buffer, 0, offset))
               .withBucketName(bucketName)
               .withKey(key)
               .withPartSize(offset)
               .withObjectMetadata(objectMetadata);
            uploadPartResultList.add(amazonS3.uploadPart(uploadPartRequest));
         }

         CompleteMultipartUploadRequest completeMultipartUploadRequest = new CompleteMultipartUploadRequest()
            .withUploadId(initiateMultipartUploadResult.getUploadId())
            .withPartETags(uploadPartResultList)
            .withBucketName(bucketName)
            .withKey(key);
         CompleteMultipartUploadResult completeMultipartUploadResult = amazonS3.completeMultipartUpload(completeMultipartUploadRequest);
      }
      else
      {
         if(objectMetadata == null)
         {
            objectMetadata = new ObjectMetadata();
         }

         LOG.info("Putting object (non-multipart)", logPair("key", key), logPair("length", offset));
         objectMetadata.setContentLength(offset);
         PutObjectResult putObjectResult = amazonS3.putObject(bucketName, key, new ByteArrayInputStream(buffer, 0, offset), objectMetadata);
      }

      isClosed = true;
   }

}
