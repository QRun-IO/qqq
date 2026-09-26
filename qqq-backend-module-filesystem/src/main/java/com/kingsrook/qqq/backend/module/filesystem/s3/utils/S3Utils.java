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


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.AbstractFilesystemTableBackendDetails;
import com.kingsrook.qqq.backend.module.filesystem.exceptions.FilesystemException;
import com.kingsrook.qqq.backend.module.filesystem.local.actions.AbstractFilesystemAction;


/*******************************************************************************
 ** Utility methods for working with AWS S3.
 **
 ** Note:  May need a constructor (or similar) in the future that takes the
 ** S3BackendMetaData - e.g., if we need some metaData to construct the AmazonS3
 ** (api client) object, such as region, or authentication.
 *******************************************************************************/
public class S3Utils
{
   private static final QLogger LOG = QLogger.getLogger(S3Utils.class);

   private AmazonS3 amazonS3;



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

      ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request()
         .withBucketName(bucketName)
         .withPrefix(prefix);

      ListObjectsV2Result   listObjectsV2Result = null;
      List<S3ObjectSummary> rs                  = new ArrayList<>();

      do
      {
         if(listObjectsV2Result != null)
         {
            listObjectsV2Request.setContinuationToken(listObjectsV2Result.getNextContinuationToken());
         }
         LOG.info("Listing bucket=" + bucketName + ", path=" + path + ", prefix=" + prefix + ", glob=" + glob);
         listObjectsV2Result = getAmazonS3().listObjectsV2(listObjectsV2Request);

         //////////////////////////////////
         // put files in the result list //
         //////////////////////////////////
         for(S3ObjectSummary objectSummary : listObjectsV2Result.getObjectSummaries())
         {
            String key = objectSummary.getKey();

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

            rs.add(objectSummary);
         }
      }
      while(listObjectsV2Result.isTruncated());

      return rs;
   }



   /*******************************************************************************
    ** Get the contents (as an InputStream) for an object in s3
    *******************************************************************************/
   public InputStream getObjectAsInputStream(S3ObjectSummary s3ObjectSummary)
   {
      return getAmazonS3().getObject(s3ObjectSummary.getBucketName(), s3ObjectSummary.getKey()).getObjectContent();
   }



   /*******************************************************************************
    ** Write a file
    *******************************************************************************/
   public void writeFile(String bucket, String key, byte[] contents, String contentType)
   {
      ObjectMetadata objectMetadata = new ObjectMetadata();
      objectMetadata.setContentLength(contents.length);
      objectMetadata.setContentType(contentType);
      getAmazonS3().putObject(bucket, key, new ByteArrayInputStream(contents), objectMetadata);
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
         getAmazonS3().deleteObject(bucketName, key);
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
         getAmazonS3().copyObject(bucketName, source, bucketName, destination);
         getAmazonS3().deleteObject(bucketName, source);
      }
      catch(Exception e)
      {
         throw (new FilesystemException("Error moving s3 object " + source + " to " + destination + " in bucket " + bucketName, e));
      }
   }



   /*******************************************************************************
    ** Setter for AmazonS3 client object.
    *******************************************************************************/
   public void setAmazonS3(AmazonS3 amazonS3)
   {
      this.amazonS3 = amazonS3;
   }



   /*******************************************************************************
    ** Getter for AmazonS3 client object.
    *******************************************************************************/
   public AmazonS3 getAmazonS3()
   {
      if(amazonS3 == null)
      {
         // TODO - get this (and other props?) from backend meta data
         amazonS3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
      }

      return amazonS3;
   }

}
