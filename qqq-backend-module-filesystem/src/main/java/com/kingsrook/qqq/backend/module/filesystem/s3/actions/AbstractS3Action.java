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

package com.kingsrook.qqq.backend.module.filesystem.s3.actions;


import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.module.filesystem.base.actions.AbstractBaseFilesystemAction;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.AbstractFilesystemTableBackendDetails;
import com.kingsrook.qqq.backend.module.filesystem.exceptions.FilesystemException;
import com.kingsrook.qqq.backend.module.filesystem.s3.model.metadata.S3BackendMetaData;
import com.kingsrook.qqq.backend.module.filesystem.s3.model.metadata.S3TableBackendDetails;
import com.kingsrook.qqq.backend.module.filesystem.s3.utils.S3Utils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Base class for all S3 filesystem actions
 *******************************************************************************/
public class AbstractS3Action extends AbstractBaseFilesystemAction<S3ObjectSummary>
{
   private static final QLogger LOG = QLogger.getLogger(AbstractS3Action.class);

   private S3Utils s3Utils;



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Long getFileSize(S3ObjectSummary s3ObjectSummary)
   {
      return (s3ObjectSummary.getSize());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Instant getFileCreateDate(S3ObjectSummary s3ObjectSummary)
   {
      return null;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Instant getFileModifyDate(S3ObjectSummary s3ObjectSummary)
   {
      return s3ObjectSummary.getLastModified().toInstant();
   }



   /*******************************************************************************
    ** Setup the s3 utils object to be used for this action.
    *******************************************************************************/
   @Override
   public void preAction(QBackendMetaData backendMetaData) throws QException
   {
      super.preAction(backendMetaData);

      if(s3Utils != null)
      {
         LOG.debug("s3Utils object is already set - not re-setting it.");
         return;
      }

      s3Utils = new S3Utils();
      s3Utils.setAmazonS3(buildAmazonS3ClientFromBackendMetaData(backendMetaData));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected AmazonS3 buildAmazonS3ClientFromBackendMetaData(QBackendMetaData backendMetaData)
   {
      S3BackendMetaData     s3BackendMetaData = getBackendMetaData(S3BackendMetaData.class, backendMetaData);
      AmazonS3ClientBuilder clientBuilder     = AmazonS3ClientBuilder.standard();

      if(StringUtils.hasContent(s3BackendMetaData.getAccessKey()) && StringUtils.hasContent(s3BackendMetaData.getSecretKey()))
      {
         BasicAWSCredentials credentials = new BasicAWSCredentials(s3BackendMetaData.getAccessKey(), s3BackendMetaData.getSecretKey());
         clientBuilder.setCredentials(new AWSStaticCredentialsProvider(credentials));
      }

      if(StringUtils.hasContent(s3BackendMetaData.getRegion()))
      {
         clientBuilder.setRegion(s3BackendMetaData.getRegion());
      }

      return (clientBuilder.build());
   }



   /*******************************************************************************
    ** Set the S3Utils object.
    *******************************************************************************/
   public void setS3Utils(S3Utils s3Utils)
   {
      this.s3Utils = s3Utils;
   }



   /*******************************************************************************
    ** Internal accessor for the s3Utils object - should always use this, not the field.
    *******************************************************************************/
   protected S3Utils getS3Utils()
   {
      if(s3Utils == null)
      {
         s3Utils = new S3Utils();
      }

      return s3Utils;
   }



   /*******************************************************************************
    ** List the files for a table.
    *******************************************************************************/
   @Override
   public List<S3ObjectSummary> listFiles(QTableMetaData table, QBackendMetaData backendBase, String requestedPath) throws QException
   {
      S3BackendMetaData                     s3BackendMetaData = getBackendMetaData(S3BackendMetaData.class, backendBase);
      AbstractFilesystemTableBackendDetails tableDetails      = getTableBackendDetails(AbstractFilesystemTableBackendDetails.class, table);

      String fullPath   = getFullBasePath(table, backendBase);
      String bucketName = s3BackendMetaData.getBucketName();
      String glob       = tableDetails.getGlob();

      ////////////////////////////////////////////////////////////////////
      // todo - look at metadata to configure the s3 client here?       //
      ////////////////////////////////////////////////////////////////////
      return getS3Utils().listObjectsInBucketMatchingGlob(bucketName, fullPath, glob, requestedPath, tableDetails);
   }



   /*******************************************************************************
    ** Read the contents of a file.
    *******************************************************************************/
   @Override
   public InputStream readFile(S3ObjectSummary s3ObjectSummary) throws IOException
   {
      return (getS3Utils().getObjectAsInputStream(s3ObjectSummary));
   }



   /*******************************************************************************
    ** Write a file - to be implemented in module-specific subclasses.
    *******************************************************************************/
   @Override
   public void writeFile(QBackendMetaData backendMetaData, QTableMetaData table, QRecord record, String path, byte[] contents) throws IOException
   {
      String bucketName = ((S3BackendMetaData) backendMetaData).getBucketName();

      try
      {
         path = stripLeadingSlash(stripDuplicatedSlashes(path));
         String contentType = null;

         if(table.getBackendDetails() instanceof S3TableBackendDetails s3TableBackendDetails)
         {
            contentType = switch(Objects.requireNonNullElse(s3TableBackendDetails.getContentTypeStrategy(), S3TableBackendDetails.ContentTypeStrategy.NONE))
            {
               case BASED_ON_FILE_NAME -> URLConnection.guessContentTypeFromName(path);
               case FROM_FIELD -> record == null ? null : record.getValueString(s3TableBackendDetails.getContentTypeFieldName());
               case HARDCODED -> s3TableBackendDetails.getHardcodedContentType();
               case NONE -> null;
            };
         }

         getS3Utils().writeFile(bucketName, path, contents, contentType);
      }
      catch(Exception e)
      {
         LOG.warn("Error writing file", e, logPair("path", path), logPair("bucketName", bucketName));
         throw (new IOException("Error writing file", e));
      }
   }



   /*******************************************************************************
    ** Get a string that represents the full path to a file.
    *******************************************************************************/
   @Override
   public String getFullPathForFile(S3ObjectSummary s3ObjectSummary)
   {
      return (s3ObjectSummary.getKey());
   }



   /*******************************************************************************
    ** e.g., with a base path of /foo/
    ** and a table path of /bar/
    ** and a file at /foo/bar/baz.txt
    ** give us just the baz.txt part.
    *******************************************************************************/
   @Override
   public String stripBackendAndTableBasePathsFromFileName(String filePath, QBackendMetaData backend, QTableMetaData table)
   {
      String tablePath    = getFullBasePath(table, backend);
      String strippedPath = filePath.replaceFirst("^/*" + stripLeadingSlash(tablePath), "");
      return (strippedPath);
   }



   /*******************************************************************************
    ** In contrast with the DeleteAction, which deletes RECORDS - this is a
    ** filesystem-(or s3, sftp, etc)-specific extension to delete an entire FILE
    ** e.g., for post-ETL.
    **
    ** @throws FilesystemException if the delete is known to have failed, and the file is thought to still exit
    *******************************************************************************/
   @Override
   public void deleteFile(QTableMetaData table, String fileReference) throws FilesystemException
   {
      QBackendMetaData backend     = QContext.getQInstance().getBackend(table.getBackendName());
      String           bucketName  = ((S3BackendMetaData) backend).getBucketName();
      String           cleanedPath = stripLeadingSlash(stripDuplicatedSlashes(fileReference));

      getS3Utils().deleteObject(bucketName, cleanedPath);
   }



   /*******************************************************************************
    ** In contrast with the DeleteAction, which deletes RECORDS - this is a
    ** filesystem-(or s3, sftp, etc)-specific extension to delete an entire FILE
    ** e.g., for post-ETL.
    **
    ** @param destination assumed to be a file path - not a directory
    ** @throws FilesystemException if the move is known to have failed
    *******************************************************************************/
   @Override
   public void moveFile(QInstance instance, QTableMetaData table, String source, String destination) throws FilesystemException
   {
      QBackendMetaData backend    = instance.getBackend(table.getBackendName());
      String           bucketName = ((S3BackendMetaData) backend).getBucketName();
      destination = stripLeadingSlash(stripDuplicatedSlashes(destination));

      getS3Utils().moveObject(bucketName, source, destination);
   }

}
