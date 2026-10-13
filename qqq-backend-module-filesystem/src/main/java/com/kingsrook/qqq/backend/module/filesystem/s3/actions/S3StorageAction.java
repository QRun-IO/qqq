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

package com.kingsrook.qqq.backend.module.filesystem.s3.actions;


import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import com.kingsrook.qqq.backend.core.actions.interfaces.QStorageInterface;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.module.filesystem.s3.model.metadata.S3BackendMetaData;
import com.kingsrook.qqq.backend.module.filesystem.s3.utils.S3UploadOutputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectAclRequest;


/*******************************************************************************
 ** (mass, streamed) storage action for s3 module
 *******************************************************************************/
public class S3StorageAction extends AbstractS3Action implements QStorageInterface
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public OutputStream createOutputStream(StorageInput storageInput) throws QException
   {
      try
      {
         S3BackendMetaData backend = (S3BackendMetaData) storageInput.getBackend();
         preAction(backend);

         S3Client             s3Client             = getS3Utils().getS3Client();
         String               fullPath             = getFullPath(storageInput);
         S3UploadOutputStream s3UploadOutputStream = new S3UploadOutputStream(s3Client, backend.getBucketName(), fullPath, storageInput.getContentType());
         return (s3UploadOutputStream);
      }
      catch(Exception e)
      {
         throw (new QException("Exception creating s3 output stream for file", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String getFullPath(StorageInput storageInput)
   {
      QTableMetaData   table    = storageInput.getTable();
      QBackendMetaData backend  = storageInput.getBackend();
      String           fullPath = stripDuplicatedSlashes(getFullBasePath(table, backend) + File.separator + storageInput.getReference());

      /////////////////////////////////////////////////////////////
      // s3 seems to do better w/o leading slashes, so, strip... //
      /////////////////////////////////////////////////////////////
      if(fullPath.startsWith("/"))
      {
         fullPath = fullPath.substring(1);
      }

      return fullPath;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public InputStream getInputStream(StorageInput storageInput) throws QException
   {
      try
      {
         S3BackendMetaData backend = (S3BackendMetaData) storageInput.getBackend();
         preAction(backend);

         S3Client         s3Client         = getS3Utils().getS3Client();
         String           fullPath         = getFullPath(storageInput);
         GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(backend.getBucketName()).key(fullPath).build();

         return (s3Client.getObject(getObjectRequest));
      }
      catch(Exception e)
      {
         throw (new QException("Exception getting s3 input stream for file.", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getDownloadURL(StorageInput storageInput) throws QException
   {
      try
      {
         S3BackendMetaData backend = (S3BackendMetaData) storageInput.getBackend();
         preAction(backend);

         S3Client s3Client = getS3Utils().getS3Client();
         String   fullPath = getFullPath(storageInput);
         return (s3Client.utilities().getUrl(GetUrlRequest.builder().bucket(backend.getBucketName()).key(fullPath).build()).toString());
      }
      catch(Exception e)
      {
         throw (new QException("Exception getting the S3 download URL.", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void makePublic(StorageInput storageInput) throws QException
   {
      try
      {
         S3BackendMetaData backend = (S3BackendMetaData) storageInput.getBackend();
         preAction(backend);

         S3Client s3Client = getS3Utils().getS3Client();
         String   fullPath = getFullPath(storageInput);
         s3Client.putObjectAcl(PutObjectAclRequest.builder().bucket(backend.getBucketName()).key(fullPath).acl(ObjectCannedACL.PUBLIC_READ).build());
      }
      catch(Exception e)
      {
         ///////////////////////////////////////////////////////////////////////////
         // object ACLs are disabled by default on new AWS buckets, and are not   //
         // supported by several S3-compatible stores (e.g., R2, Garage) - so say //
         // what to do instead.                                                   //
         ///////////////////////////////////////////////////////////////////////////
         throw (new QException("Exception making s3 file publicly available (via an object ACL).  If the bucket or store does not support object ACLs, grant public read with a bucket policy instead.", e));
      }
   }

}
