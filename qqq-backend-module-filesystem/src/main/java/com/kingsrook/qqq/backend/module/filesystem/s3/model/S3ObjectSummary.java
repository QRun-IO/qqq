/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.filesystem.s3.model;


import java.time.Instant;
import software.amazon.awssdk.services.s3.model.S3Object;


/*******************************************************************************
 ** One object found by an S3 listing: the "file" type that the S3 filesystem
 ** actions work with.
 **
 ** This is a QQQ-owned type (it replaced the AWS SDK v1 S3ObjectSummary when the
 ** module moved to AWS SDK v2), so that callers do not depend on SDK model
 ** classes.  Getter names follow the old v1 class where they can, except that
 ** getLastModified now returns an Instant instead of a Date.
 *******************************************************************************/
public class S3ObjectSummary
{
   private String  bucketName;
   private String  key;
   private Long    size;
   private Instant lastModified;
   private String  eTag;
   private String  storageClass;



   /*******************************************************************************
    ** Default Constructor.
    *******************************************************************************/
   public S3ObjectSummary()
   {
   }



   /*******************************************************************************
    ** Build from an AWS SDK v2 listing entry, plus the bucket it was listed from
    ** (the SDK v2 object does not carry its bucket name).
    *******************************************************************************/
   public static S3ObjectSummary of(String bucketName, S3Object s3Object)
   {
      return (new S3ObjectSummary()
         .withBucketName(bucketName)
         .withKey(s3Object.key())
         .withSize(s3Object.size())
         .withLastModified(s3Object.lastModified())
         .withETag(s3Object.eTag())
         .withStorageClass(s3Object.storageClassAsString()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String toString()
   {
      return ("S3ObjectSummary{bucketName='" + bucketName + "', key='" + key + "', size=" + size + "}");
   }



   /*******************************************************************************
    ** Getter for bucketName
    *******************************************************************************/
   public String getBucketName()
   {
      return (this.bucketName);
   }



   /*******************************************************************************
    ** Setter for bucketName
    *******************************************************************************/
   public void setBucketName(String bucketName)
   {
      this.bucketName = bucketName;
   }



   /*******************************************************************************
    ** Fluent setter for bucketName
    *******************************************************************************/
   public S3ObjectSummary withBucketName(String bucketName)
   {
      this.bucketName = bucketName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for key
    *******************************************************************************/
   public String getKey()
   {
      return (this.key);
   }



   /*******************************************************************************
    ** Setter for key
    *******************************************************************************/
   public void setKey(String key)
   {
      this.key = key;
   }



   /*******************************************************************************
    ** Fluent setter for key
    *******************************************************************************/
   public S3ObjectSummary withKey(String key)
   {
      this.key = key;
      return (this);
   }



   /*******************************************************************************
    ** Getter for size
    *******************************************************************************/
   public Long getSize()
   {
      return (this.size);
   }



   /*******************************************************************************
    ** Setter for size
    *******************************************************************************/
   public void setSize(Long size)
   {
      this.size = size;
   }



   /*******************************************************************************
    ** Fluent setter for size
    *******************************************************************************/
   public S3ObjectSummary withSize(Long size)
   {
      this.size = size;
      return (this);
   }



   /*******************************************************************************
    ** Getter for lastModified
    *******************************************************************************/
   public Instant getLastModified()
   {
      return (this.lastModified);
   }



   /*******************************************************************************
    ** Setter for lastModified
    *******************************************************************************/
   public void setLastModified(Instant lastModified)
   {
      this.lastModified = lastModified;
   }



   /*******************************************************************************
    ** Fluent setter for lastModified
    *******************************************************************************/
   public S3ObjectSummary withLastModified(Instant lastModified)
   {
      this.lastModified = lastModified;
      return (this);
   }



   /*******************************************************************************
    ** Getter for eTag
    *******************************************************************************/
   public String getETag()
   {
      return (this.eTag);
   }



   /*******************************************************************************
    ** Setter for eTag
    *******************************************************************************/
   public void setETag(String eTag)
   {
      this.eTag = eTag;
   }



   /*******************************************************************************
    ** Fluent setter for eTag
    *******************************************************************************/
   public S3ObjectSummary withETag(String eTag)
   {
      this.eTag = eTag;
      return (this);
   }



   /*******************************************************************************
    ** Getter for storageClass
    *******************************************************************************/
   public String getStorageClass()
   {
      return (this.storageClass);
   }



   /*******************************************************************************
    ** Setter for storageClass
    *******************************************************************************/
   public void setStorageClass(String storageClass)
   {
      this.storageClass = storageClass;
   }



   /*******************************************************************************
    ** Fluent setter for storageClass
    *******************************************************************************/
   public S3ObjectSummary withStorageClass(String storageClass)
   {
      this.storageClass = storageClass;
      return (this);
   }

}
