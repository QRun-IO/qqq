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
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.AbstractFilesystemTableBackendDetails;
import com.kingsrook.qqq.backend.module.filesystem.exceptions.FilesystemException;
import com.kingsrook.qqq.backend.module.filesystem.local.actions.AbstractFilesystemAction;
import com.kingsrook.qqq.backend.module.filesystem.s3.model.S3ObjectSummary;
import com.kingsrook.qqq.backend.module.filesystem.s3.model.metadata.S3BackendMetaData;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;


/*******************************************************************************
 ** Utility methods for working with AWS S3 (or any S3-compatible store), on an
 ** AWS SDK v2 S3Client.  Actions set the client built by S3ClientFactory from
 ** their backend's meta data.
 *******************************************************************************/
public class S3Utils
{
   private static final QLogger LOG = QLogger.getLogger(S3Utils.class);

   private S3Client s3Client;



   /*******************************************************************************
    ** List the objects in an S3 bucket matching a glob, per:
    ** https://docs.oracle.com/javase/7/docs/api/java/nio/file/FileSystem.html#getPathMatcher(java.lang.String)
    *******************************************************************************/
   public List<S3ObjectSummary> listObjectsInBucketMatchingGlob(String bucketName, String path, String glob) throws QException
   {
      return listObjectsInBucketMatchingGlob(bucketName, path, glob, null, null);
   }



   /*******************************************************************************
    ** List the objects in an S3 bucket matching a glob, per:
    ** https://docs.oracle.com/javase/7/docs/api/java/nio/file/FileSystem.html#getPathMatcher(java.lang.String)
    ** and also - (possibly) apply a file-name filter (based on the table's details).
    *******************************************************************************/
   public List<S3ObjectSummary> listObjectsInBucketMatchingGlob(String bucketName, String path, String glob, String requestedPath, AbstractFilesystemTableBackendDetails tableDetails) throws QException
   {
      return (listObjectsInBucketMatchingGlob(bucketName, path, glob, requestedPath, tableDetails, null));
   }



   /*******************************************************************************
    ** List the objects in an S3 bucket matching a glob, as above, failing with a
    ** FilesystemException (instead of exhausting memory) if more than
    ** maxListedObjects objects match.  A null maxListedObjects means no cap.
    *******************************************************************************/
   public List<S3ObjectSummary> listObjectsInBucketMatchingGlob(String bucketName, String path, String glob, String requestedPath, AbstractFilesystemTableBackendDetails tableDetails, Integer maxListedObjects) throws QException
   {
      //////////////////////////////////////////////////////////////////////////////////////////////////
      // s3 list requests find nothing if the path starts with a /, so strip away any leading slashes //
      // also strip away trailing /'s, for consistent known paths.                                    //
      // also normalize any duplicated /'s to a single /.                                             //
      //////////////////////////////////////////////////////////////////////////////////////////////////
      path = path.replaceFirst("^/+", "").replaceFirst("/+$", "").replaceAll("//+", "/");
      String prefix = path;

      // todo - maybe this is some error - that the user put a * in the path instead of the glob?
      if(prefix.indexOf('*') > -1)
      {
         prefix = prefix.substring(0, prefix.indexOf('*'));
      }

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////
      // optimization, to avoid listing whole bucket, for use-case where less than a whole bucket is requested //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////
      if(StringUtils.hasContent(requestedPath))
      {
         if(!prefix.isEmpty())
         {
            ///////////////////////////////////////////////////////
            // remember, a prefix starting with / finds nothing! //
            ///////////////////////////////////////////////////////
            prefix += "/";
         }

         prefix += requestedPath;
      }

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // mmm, we're assuming here we always want more than 1 file - so there must be some * in the glob.                //
      // That's a bad assumption, as it doesn't consider other wildcards like ? and [-] - but - put that aside for now. //
      // Anyway, add a trailing /* to globs with no wildcards (or just a '*' if it's a request for the root (""))       //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      if(glob == null)
      {
         glob = "";
      }

      if(!glob.contains("*"))
      {
         if(glob.equals(""))
         {
            glob += "*";
         }
         else
         {
            glob += "/*";
         }
      }

      String      pathMatcherArg = AbstractFilesystemAction.stripDuplicatedSlashes("glob:/" + path + "/" + glob);
      PathMatcher pathMatcher    = FileSystems.getDefault().getPathMatcher(pathMatcherArg);

      ListObjectsV2Response listObjectsV2Response = null;
      List<S3ObjectSummary> rs                    = new ArrayList<>();

      do
      {
         ListObjectsV2Request.Builder listObjectsV2Request = ListObjectsV2Request.builder()
            .bucket(bucketName)
            .prefix(prefix);

         if(listObjectsV2Response != null)
         {
            listObjectsV2Request.continuationToken(listObjectsV2Response.nextContinuationToken());
         }
         LOG.info("Listing bucket=" + bucketName + ", path=" + path + ", prefix=" + prefix + ", glob=" + glob);
         listObjectsV2Response = getS3Client().listObjectsV2(listObjectsV2Request.build());

         //////////////////////////////////
         // put files in the result list //
         //////////////////////////////////
         for(S3Object s3Object : listObjectsV2Response.contents())
         {
            String key = s3Object.key();

            //////////////////////////////////////////////////////////////////////////////////////////////////////
            // it looks like keys in s3 can have duplicated /'s - so normalize those, to create a "sane" result //
            //////////////////////////////////////////////////////////////////////////////////////////////////////
            key = key.replaceAll("//+", "/");

            ////////////////////////////////////////////////////////////////////////////////
            // always skip folders                                                        //
            // this seemed to fire when it was first written, but not in our unit tests - //
            // is this a difference with real s3 vs. localstack possibly?                 //
            ////////////////////////////////////////////////////////////////////////////////
            if(key.endsWith("/"))
            {
               // LOG.debug("Skipping file [{}] because it is a folder", key);
               continue;
            }

            ///////////////////////////////////////////
            // skip files that do not match the glob //
            ///////////////////////////////////////////
            if(!pathMatcher.matches(Path.of(URI.create("file:///" + URLEncoder.encode(key, StandardCharsets.UTF_8)))))
            {
               // LOG.debug("Skipping file [{}] that does not match glob [{}]", key, glob);
               continue;
            }

            //////////////////////////////////////////////////////////////////////////
            // fail clearly, rather than run out of memory, on a too-large listing //
            //////////////////////////////////////////////////////////////////////////
            if(maxListedObjects != null && rs.size() >= maxListedObjects)
            {
               throw (new FilesystemException("Listing bucket [" + bucketName + "] with prefix [" + prefix + "] found more than " + maxListedObjects
                  + " matching objects.  Narrow the table's path or glob, or raise the backend's maxListedObjects (zero or negative disables the cap)."));
            }

            rs.add(S3ObjectSummary.of(bucketName, s3Object));
         }
      }
      while(Boolean.TRUE.equals(listObjectsV2Response.isTruncated()));

      return rs;
   }



   /*******************************************************************************
    ** Get the contents (as an InputStream) for an object in s3
    *******************************************************************************/
   public InputStream getObjectAsInputStream(S3ObjectSummary s3ObjectSummary)
   {
      return (getS3Client().getObject(GetObjectRequest.builder().bucket(s3ObjectSummary.getBucketName()).key(s3ObjectSummary.getKey()).build()));
   }



   /*******************************************************************************
    ** Write a file
    *******************************************************************************/
   public void writeFile(String bucket, String key, byte[] contents, String contentType)
   {
      PutObjectRequest.Builder putObjectRequest = PutObjectRequest.builder()
         .bucket(bucket)
         .key(key)
         .contentLength((long) contents.length);

      if(contentType != null)
      {
         putObjectRequest.contentType(contentType);
      }

      getS3Client().putObject(putObjectRequest.build(), RequestBody.fromBytes(contents));
   }



   /*******************************************************************************
    ** Delete an object (file) from a bucket
    *******************************************************************************/
   public void deleteObject(String bucketName, String key) throws FilesystemException
   {
      //////////////////////////////////////////////////////////////////////////////////////////////
      // note, aws s3 api does not appear to have any way to check the success or failure here... //
      //////////////////////////////////////////////////////////////////////////////////////////////
      try
      {
         getS3Client().deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build());
      }
      catch(Exception e)
      {
         throw (new FilesystemException("Error deleting s3 object " + key + " in bucket " + bucketName, e));
      }
   }



   /*******************************************************************************
    ** Move an object (file) within a bucket
    *******************************************************************************/
   public void moveObject(String bucketName, String source, String destination) throws FilesystemException
   {
      //////////////////////////////////////////////////////////////////////////////////////////////
      // note, aws s3 api does not appear to have any way to check the success or failure here... //
      //////////////////////////////////////////////////////////////////////////////////////////////
      try
      {
         getS3Client().copyObject(CopyObjectRequest.builder()
            .sourceBucket(bucketName)
            .sourceKey(source)
            .destinationBucket(bucketName)
            .destinationKey(destination)
            .build());
         getS3Client().deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(source).build());
      }
      catch(Exception e)
      {
         throw (new FilesystemException("Error moving s3 object " + source + " to " + destination + " in bucket " + bucketName, e));
      }
   }



   /*******************************************************************************
    ** Setter for the S3 client.
    *******************************************************************************/
   public void setS3Client(S3Client s3Client)
   {
      this.s3Client = s3Client;
   }



   /*******************************************************************************
    ** Getter for the S3 client.  If none was set, uses the shared client for a
    ** default (us-east-1, default credential chain) AWS connection.
    *******************************************************************************/
   public S3Client getS3Client()
   {
      if(s3Client == null)
      {
         s3Client = S3ClientFactory.getClient(new S3BackendMetaData().withRegion("us-east-1"));
      }

      return (s3Client);
   }

}
