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

package com.kingsrook.qqq.backend.module.filesystem.s3.model.metadata;


import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.AbstractFilesystemBackendMetaData;
import com.kingsrook.qqq.backend.module.filesystem.s3.S3BackendModule;


/*******************************************************************************
 ** S3 backend meta data.
 *******************************************************************************/
public class S3BackendMetaData extends AbstractFilesystemBackendMetaData
{
   private String bucketName;
   private String accessKey;
   private String secretKey;
   private String region;



   /*******************************************************************************
    ** Default Constructor.
    *******************************************************************************/
   public S3BackendMetaData()
   {
      super();
      setBackendType(S3BackendModule.class);
   }



   /*******************************************************************************
    ** Getter for bucketName
    **
    *******************************************************************************/
   public String getBucketName()
   {
      return bucketName;
   }



   /*******************************************************************************
    ** Setter for bucketName
    **
    *******************************************************************************/
   public void setBucketName(String bucketName)
   {
      this.bucketName = bucketName;
   }



   /*******************************************************************************
    ** Fluent setter for bucketName
    **
    *******************************************************************************/
   public S3BackendMetaData withBucketName(String bucketName)
   {
      this.bucketName = bucketName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for accessKey
    **
    *******************************************************************************/
   public String getAccessKey()
   {
      return accessKey;
   }



   /*******************************************************************************
    ** Setter for accessKey
    **
    *******************************************************************************/
   public void setAccessKey(String accessKey)
   {
      this.accessKey = accessKey;
   }



   /*******************************************************************************
    ** Fluent setter for accessKey
    **
    *******************************************************************************/
   public S3BackendMetaData withAccessKey(String accessKey)
   {
      this.accessKey = accessKey;
      return (this);
   }



   /*******************************************************************************
    ** Getter for secretKey
    **
    *******************************************************************************/
   public String getSecretKey()
   {
      return secretKey;
   }



   /*******************************************************************************
    ** Setter for secretKey
    **
    *******************************************************************************/
   public void setSecretKey(String secretKey)
   {
      this.secretKey = secretKey;
   }



   /*******************************************************************************
    ** Fluent setter for secretKey
    **
    *******************************************************************************/
   public S3BackendMetaData withSecretKey(String secretKey)
   {
      this.secretKey = secretKey;
      return (this);
   }



   /*******************************************************************************
    ** Getter for region
    **
    *******************************************************************************/
   public String getRegion()
   {
      return region;
   }



   /*******************************************************************************
    ** Setter for region
    **
    *******************************************************************************/
   public void setRegion(String region)
   {
      this.region = region;
   }



   /*******************************************************************************
    ** Fluent setter for region
    **
    *******************************************************************************/
   public S3BackendMetaData withRegion(String region)
   {
      this.region = region;
      return (this);
   }



   /*******************************************************************************
    ** Called by the QInstanceEnricher - to do backend-type-specific enrichments.
    ** Original use case is:  reading secrets into fields (e.g., passwords).
    ** TODO - migrate to use @InterpretableFields (and complete that impl on core side)
    *******************************************************************************/
   @Override
   public void enrich()
   {
      super.enrich();
      QMetaDataVariableInterpreter interpreter = new QMetaDataVariableInterpreter();
      accessKey = interpreter.interpret(accessKey);
      secretKey = interpreter.interpret(secretKey);
   }



   /*******************************************************************************
    ** Fluent setter for basePath
    **
    *******************************************************************************/
   public S3BackendMetaData withBasePath(String basePath)
   {
      setBasePath(basePath);
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for name
    **
    *******************************************************************************/
   public S3BackendMetaData withName(String name)
   {
      setName(name);
      return this;
   }

}
