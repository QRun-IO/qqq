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
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.kingsrook.qqq.backend.core.actions.interfaces.QStorageInterface;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.module.filesystem.s3.model.metadata.S3BackendMetaData;
import com.kingsrook.qqq.backend.module.filesystem.s3.utils.S3UploadOutputStream;


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

         AmazonS3             amazonS3             = getS3Utils().getAmazonS3();
         String               fullPath             = getFullPath(storageInput);
         S3UploadOutputStream s3UploadOutputStream = new S3UploadOutputStream(amazonS3, backend.getBucketName(), fullPath, storageInput.getContentType());
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

         AmazonS3            amazonS3         = getS3Utils().getAmazonS3();
         String              fullPath         = getFullPath(storageInput);
         GetObjectRequest    getObjectRequest = new GetObjectRequest(backend.getBucketName(), fullPath);
         S3Object            s3Object         = amazonS3.getObject(getObjectRequest);
         S3ObjectInputStream objectContent    = s3Object.getObjectContent();

         return (objectContent);
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

         AmazonS3 amazonS3 = getS3Utils().getAmazonS3();
         String   fullPath = getFullPath(storageInput);
         return (amazonS3.getUrl(backend.getBucketName(), fullPath).toString());
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

         AmazonS3 amazonS3 = getS3Utils().getAmazonS3();
         String   fullPath = getFullPath(storageInput);
         amazonS3.setObjectAcl(backend.getBucketName(), fullPath, CannedAccessControlList.PublicRead);
      }
      catch(Exception e)
      {
         throw (new QException("Exception making s3 file publicly available.", e));
      }
   }

}
