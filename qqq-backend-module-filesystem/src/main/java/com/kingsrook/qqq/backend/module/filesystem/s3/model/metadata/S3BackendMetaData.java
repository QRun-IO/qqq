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
 **
 ** Works with AWS S3 and with any S3-compatible store (MinIO, Garage, Cloudflare
 ** R2, Backblaze B2, Wasabi, DigitalOcean Spaces, Ceph RGW, ...):
 ** - endpoint: the store's URL (empty means AWS's regional endpoint).
 ** - pathStyleAccess: true for http(s)://host/bucket/key addressing, which most
 **   self-hosted stores need; false (default) for virtual-hosted addressing.
 ** - checksumMode: see S3ChecksumMode.  Null means WHEN_REQUIRED when an
 **   endpoint is set, else the SDK default (WHEN_SUPPORTED).
 ** - tlsVerify: null/true verifies TLS certificates; false trusts any
 **   certificate (development only).
 ** - caCertificatesPem: extra PEM certificate(s) to trust, for stores behind a
 **   private CA (in addition to the JVM's default trust store).
 ** - connectTimeoutMillis / socketTimeoutMillis: null keeps the defaults
 **   (10 seconds / 50 seconds, the same as the AWS SDK v1 defaults).
 ** - maxListedObjects: listings that would return more objects than this fail
 **   with a clear error instead of exhausting memory.  Null means the default
 **   (100,000); zero or negative means no cap.
 **
 ** accessKey, secretKey, region, endpoint and caCertificatesPem are interpreted
 ** for ${env.X} / ${prop.X} references when the instance is enriched.
 *******************************************************************************/
public class S3BackendMetaData extends AbstractFilesystemBackendMetaData
{
   private String bucketName;
   private String accessKey;
   private String secretKey;
   private String region;

   private String         endpoint;
   private Boolean        pathStyleAccess;
   private S3ChecksumMode checksumMode;
   private Boolean        tlsVerify;
   private String         caCertificatesPem;
   private Integer        connectTimeoutMillis;
   private Integer        socketTimeoutMillis;
   private Integer        maxListedObjects;



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
    ** Getter for endpoint - the endpoint URL of the S3-compatible store
    *******************************************************************************/
   public String getEndpoint()
   {
      return (endpoint);
   }



   /*******************************************************************************
    ** Setter for endpoint - the endpoint URL of the S3-compatible store
    *******************************************************************************/
   public void setEndpoint(String endpoint)
   {
      this.endpoint = endpoint;
   }



   /*******************************************************************************
    ** Fluent setter for endpoint - the endpoint URL of the S3-compatible store
    *******************************************************************************/
   public S3BackendMetaData withEndpoint(String endpoint)
   {
      this.endpoint = endpoint;
      return (this);
   }



   /*******************************************************************************
    ** Getter for pathStyleAccess - whether to use path-style addressing
    *******************************************************************************/
   public Boolean getPathStyleAccess()
   {
      return (pathStyleAccess);
   }



   /*******************************************************************************
    ** Setter for pathStyleAccess - whether to use path-style addressing
    *******************************************************************************/
   public void setPathStyleAccess(Boolean pathStyleAccess)
   {
      this.pathStyleAccess = pathStyleAccess;
   }



   /*******************************************************************************
    ** Fluent setter for pathStyleAccess - whether to use path-style addressing
    *******************************************************************************/
   public S3BackendMetaData withPathStyleAccess(Boolean pathStyleAccess)
   {
      this.pathStyleAccess = pathStyleAccess;
      return (this);
   }



   /*******************************************************************************
    ** Getter for checksumMode - the request/response checksum mode
    *******************************************************************************/
   public S3ChecksumMode getChecksumMode()
   {
      return (checksumMode);
   }



   /*******************************************************************************
    ** Setter for checksumMode - the request/response checksum mode
    *******************************************************************************/
   public void setChecksumMode(S3ChecksumMode checksumMode)
   {
      this.checksumMode = checksumMode;
   }



   /*******************************************************************************
    ** Fluent setter for checksumMode - the request/response checksum mode
    *******************************************************************************/
   public S3BackendMetaData withChecksumMode(S3ChecksumMode checksumMode)
   {
      this.checksumMode = checksumMode;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tlsVerify - whether TLS certificates are verified
    *******************************************************************************/
   public Boolean getTlsVerify()
   {
      return (tlsVerify);
   }



   /*******************************************************************************
    ** Setter for tlsVerify - whether TLS certificates are verified
    *******************************************************************************/
   public void setTlsVerify(Boolean tlsVerify)
   {
      this.tlsVerify = tlsVerify;
   }



   /*******************************************************************************
    ** Fluent setter for tlsVerify - whether TLS certificates are verified
    *******************************************************************************/
   public S3BackendMetaData withTlsVerify(Boolean tlsVerify)
   {
      this.tlsVerify = tlsVerify;
      return (this);
   }



   /*******************************************************************************
    ** Getter for caCertificatesPem - extra PEM CA certificates to trust
    *******************************************************************************/
   public String getCaCertificatesPem()
   {
      return (caCertificatesPem);
   }



   /*******************************************************************************
    ** Setter for caCertificatesPem - extra PEM CA certificates to trust
    *******************************************************************************/
   public void setCaCertificatesPem(String caCertificatesPem)
   {
      this.caCertificatesPem = caCertificatesPem;
   }



   /*******************************************************************************
    ** Fluent setter for caCertificatesPem - extra PEM CA certificates to trust
    *******************************************************************************/
   public S3BackendMetaData withCaCertificatesPem(String caCertificatesPem)
   {
      this.caCertificatesPem = caCertificatesPem;
      return (this);
   }



   /*******************************************************************************
    ** Getter for connectTimeoutMillis - the HTTP connect timeout
    *******************************************************************************/
   public Integer getConnectTimeoutMillis()
   {
      return (connectTimeoutMillis);
   }



   /*******************************************************************************
    ** Setter for connectTimeoutMillis - the HTTP connect timeout
    *******************************************************************************/
   public void setConnectTimeoutMillis(Integer connectTimeoutMillis)
   {
      this.connectTimeoutMillis = connectTimeoutMillis;
   }



   /*******************************************************************************
    ** Fluent setter for connectTimeoutMillis - the HTTP connect timeout
    *******************************************************************************/
   public S3BackendMetaData withConnectTimeoutMillis(Integer connectTimeoutMillis)
   {
      this.connectTimeoutMillis = connectTimeoutMillis;
      return (this);
   }



   /*******************************************************************************
    ** Getter for socketTimeoutMillis - the HTTP socket (read) timeout
    *******************************************************************************/
   public Integer getSocketTimeoutMillis()
   {
      return (socketTimeoutMillis);
   }



   /*******************************************************************************
    ** Setter for socketTimeoutMillis - the HTTP socket (read) timeout
    *******************************************************************************/
   public void setSocketTimeoutMillis(Integer socketTimeoutMillis)
   {
      this.socketTimeoutMillis = socketTimeoutMillis;
   }



   /*******************************************************************************
    ** Fluent setter for socketTimeoutMillis - the HTTP socket (read) timeout
    *******************************************************************************/
   public S3BackendMetaData withSocketTimeoutMillis(Integer socketTimeoutMillis)
   {
      this.socketTimeoutMillis = socketTimeoutMillis;
      return (this);
   }



   /*******************************************************************************
    ** Getter for maxListedObjects - the cap on objects returned by one listing
    *******************************************************************************/
   public Integer getMaxListedObjects()
   {
      return (maxListedObjects);
   }



   /*******************************************************************************
    ** Setter for maxListedObjects - the cap on objects returned by one listing
    *******************************************************************************/
   public void setMaxListedObjects(Integer maxListedObjects)
   {
      this.maxListedObjects = maxListedObjects;
   }



   /*******************************************************************************
    ** Fluent setter for maxListedObjects - the cap on objects returned by one listing
    *******************************************************************************/
   public S3BackendMetaData withMaxListedObjects(Integer maxListedObjects)
   {
      this.maxListedObjects = maxListedObjects;
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
      region = interpreter.interpret(region);
      endpoint = interpreter.interpret(endpoint);
      caCertificatesPem = interpreter.interpret(caCertificatesPem);
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
