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

package com.kingsrook.qqq.backend.module.filesystem.s3.utils;


import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.module.filesystem.s3.model.metadata.S3BackendMetaData;
import com.kingsrook.qqq.backend.module.filesystem.s3.model.metadata.S3ChecksumMode;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.utils.AttributeMap;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Builds (and caches) AWS SDK v2 S3 clients from S3BackendMetaData.
 **
 ** One client is kept per distinct set of connection settings, because SDK v2
 ** clients own an HTTP connection pool and are meant to be long-lived and shared.
 ** The cache key is a SHA-256 hash of the settings, so no secret is held in a
 ** map key or a log line.
 *******************************************************************************/
public class S3ClientFactory
{
   private static final QLogger LOG = QLogger.getLogger(S3ClientFactory.class);

   public static final int      DEFAULT_MAX_LISTED_OBJECTS = 100_000;
   public static final Duration DEFAULT_CONNECT_TIMEOUT    = Duration.ofSeconds(10);
   public static final Duration DEFAULT_SOCKET_TIMEOUT     = Duration.ofSeconds(50);

   private static final Map<String, S3Client> CLIENT_CACHE = new ConcurrentHashMap<>();



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private S3ClientFactory()
   {
   }



   /*******************************************************************************
    ** Get the shared client for a backend's connection settings, building it on
    ** first use.  The client is shared: callers must not close it (use
    ** clearCache() to close all cached clients).
    *******************************************************************************/
   public static S3Client getClient(S3BackendMetaData backendMetaData)
   {
      return (CLIENT_CACHE.computeIfAbsent(buildCacheKey(backendMetaData), key -> buildClient(backendMetaData)));
   }



   /*******************************************************************************
    ** Build a new (uncached) client.  Callers own it, and should close it.
    *******************************************************************************/
   public static S3Client buildClient(S3BackendMetaData backendMetaData)
   {
      Objects.requireNonNull(backendMetaData, "backendMetaData is required");

      S3ClientBuilder builder = S3Client.builder()
         .credentialsProvider(buildCredentialsProvider(backendMetaData))
         .httpClient(buildHttpClient(backendMetaData));

      String endpoint = backendMetaData.getEndpoint();
      if(StringUtils.hasContent(endpoint))
      {
         builder.endpointOverride(URI.create(endpoint));
      }

      //////////////////////////////////////////////////////////////////////////////
      // an explicit region always wins.  a custom endpoint with no region gets    //
      // us-east-1, which every S3-compatible store accepts for signing.  with     //
      // neither, the SDK's default region provider chain applies (as it did for  //
      // the v1 client).                                                          //
      //////////////////////////////////////////////////////////////////////////////
      if(StringUtils.hasContent(backendMetaData.getRegion()))
      {
         builder.region(Region.of(backendMetaData.getRegion()));
      }
      else if(StringUtils.hasContent(endpoint))
      {
         builder.region(Region.US_EAST_1);
      }

      if(backendMetaData.getPathStyleAccess() != null)
      {
         builder.forcePathStyle(backendMetaData.getPathStyleAccess());
      }

      if(getEffectiveChecksumMode(backendMetaData) == S3ChecksumMode.WHEN_REQUIRED)
      {
         builder.requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED);
         builder.responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED);
      }
      else
      {
         builder.requestChecksumCalculation(RequestChecksumCalculation.WHEN_SUPPORTED);
         builder.responseChecksumValidation(ResponseChecksumValidation.WHEN_SUPPORTED);
      }

      LOG.debug("Building S3 client", logPair("endpoint", endpoint), logPair("region", backendMetaData.getRegion()),
         logPair("pathStyleAccess", backendMetaData.getPathStyleAccess()), logPair("checksumMode", getEffectiveChecksumMode(backendMetaData)));

      return (builder.build());
   }



   /*******************************************************************************
    ** The checksum mode a backend actually uses: its explicit setting, else
    ** WHEN_REQUIRED for a custom endpoint (S3-compatible stores), else the SDK
    ** default, WHEN_SUPPORTED (AWS).
    *******************************************************************************/
   public static S3ChecksumMode getEffectiveChecksumMode(S3BackendMetaData backendMetaData)
   {
      if(backendMetaData.getChecksumMode() != null)
      {
         return (backendMetaData.getChecksumMode());
      }

      return (StringUtils.hasContent(backendMetaData.getEndpoint()) ? S3ChecksumMode.WHEN_REQUIRED : S3ChecksumMode.WHEN_SUPPORTED);
   }



   /*******************************************************************************
    ** The listing cap a backend actually uses: null means the default; zero or
    ** negative means no cap (returned as null).
    *******************************************************************************/
   public static Integer getEffectiveMaxListedObjects(S3BackendMetaData backendMetaData)
   {
      Integer maxListedObjects = backendMetaData.getMaxListedObjects();
      if(maxListedObjects == null)
      {
         return (DEFAULT_MAX_LISTED_OBJECTS);
      }

      return (maxListedObjects > 0 ? maxListedObjects : null);
   }



   /*******************************************************************************
    ** Close and forget every cached client (e.g., after credentials rotate, or
    ** between tests).
    *******************************************************************************/
   public static void clearCache()
   {
      List<S3Client> clients = new ArrayList<>(CLIENT_CACHE.values());
      CLIENT_CACHE.clear();
      for(S3Client client : clients)
      {
         try
         {
            client.close();
         }
         catch(RuntimeException e)
         {
            LOG.debug("Error closing a cached S3 client", e);
         }
      }
   }



   /*******************************************************************************
    ** Static keys when both are given, else the SDK's default chain (environment,
    ** profile, web identity, container and instance roles).
    *******************************************************************************/
   private static AwsCredentialsProvider buildCredentialsProvider(S3BackendMetaData backendMetaData)
   {
      if(StringUtils.hasContent(backendMetaData.getAccessKey()) && StringUtils.hasContent(backendMetaData.getSecretKey()))
      {
         return (StaticCredentialsProvider.create(AwsBasicCredentials.create(backendMetaData.getAccessKey(), backendMetaData.getSecretKey())));
      }

      return (DefaultCredentialsProvider.builder().build());
   }



   /*******************************************************************************
    ** Apache HTTP client with the backend's timeouts and TLS settings.
    *******************************************************************************/
   private static SdkHttpClient buildHttpClient(S3BackendMetaData backendMetaData)
   {
      ApacheHttpClient.Builder builder = ApacheHttpClient.builder()
         .connectionTimeout(backendMetaData.getConnectTimeoutMillis() == null ? DEFAULT_CONNECT_TIMEOUT : Duration.ofMillis(backendMetaData.getConnectTimeoutMillis()))
         .socketTimeout(backendMetaData.getSocketTimeoutMillis() == null ? DEFAULT_SOCKET_TIMEOUT : Duration.ofMillis(backendMetaData.getSocketTimeoutMillis()));

      if(Boolean.FALSE.equals(backendMetaData.getTlsVerify()))
      {
         LOG.warn("TLS certificate verification is disabled for an S3 backend; use only for development", logPair("endpoint", backendMetaData.getEndpoint()));
         return (builder.buildWithDefaults(AttributeMap.builder().put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES, Boolean.TRUE).build()));
      }

      if(StringUtils.hasContent(backendMetaData.getCaCertificatesPem()))
      {
         TrustManager[] trustManagers = buildTrustManagers(backendMetaData.getCaCertificatesPem());
         builder.tlsTrustManagersProvider(() -> trustManagers);
      }

      return (builder.build());
   }



   /*******************************************************************************
    ** Trust the given PEM certificate(s) in addition to the JVM's default trust
    ** store.
    *******************************************************************************/
   static TrustManager[] buildTrustManagers(String caCertificatesPem)
   {
      try
      {
         CertificateFactory                certificateFactory = CertificateFactory.getInstance("X.509");
         Collection<? extends Certificate> certificates       = certificateFactory.generateCertificates(new ByteArrayInputStream(caCertificatesPem.getBytes(StandardCharsets.UTF_8)));
         if(certificates.isEmpty())
         {
            throw (new IllegalArgumentException("caCertificatesPem contains no certificates"));
         }

         KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
         keyStore.load(null, null);
         int index = 0;
         for(Certificate certificate : certificates)
         {
            keyStore.setCertificateEntry("qqq-s3-ca-" + index++, certificate);
         }

         X509TrustManager customTrustManager  = getX509TrustManager(keyStore);
         X509TrustManager defaultTrustManager = getX509TrustManager(null);
         return (new TrustManager[] { new CompositeX509TrustManager(customTrustManager, defaultTrustManager) });
      }
      catch(IllegalArgumentException e)
      {
         throw (e);
      }
      catch(Exception e)
      {
         throw (new IllegalArgumentException("Could not load caCertificatesPem for the S3 backend", e));
      }
   }



   /*******************************************************************************
    ** The X509 trust manager for a key store (null: the JVM default trust store).
    *******************************************************************************/
   private static X509TrustManager getX509TrustManager(KeyStore keyStore) throws Exception
   {
      TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init(keyStore);
      for(TrustManager trustManager : trustManagerFactory.getTrustManagers())
      {
         if(trustManager instanceof X509TrustManager x509TrustManager)
         {
            return (x509TrustManager);
         }
      }

      throw (new IllegalStateException("No X509TrustManager available"));
   }



   /*******************************************************************************
    ** Hash of every setting that affects the client.  Includes the secret, so the
    ** key is a digest, never the raw values.
    *******************************************************************************/
   static String buildCacheKey(S3BackendMetaData backendMetaData)
   {
      String settings = String.join("\u0000",
         String.valueOf(backendMetaData.getEndpoint()),
         String.valueOf(backendMetaData.getRegion()),
         String.valueOf(backendMetaData.getAccessKey()),
         String.valueOf(backendMetaData.getSecretKey()),
         String.valueOf(backendMetaData.getPathStyleAccess()),
         String.valueOf(getEffectiveChecksumMode(backendMetaData)),
         String.valueOf(backendMetaData.getTlsVerify()),
         String.valueOf(backendMetaData.getCaCertificatesPem()),
         String.valueOf(backendMetaData.getConnectTimeoutMillis()),
         String.valueOf(backendMetaData.getSocketTimeoutMillis()));

      try
      {
         return (HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(settings.getBytes(StandardCharsets.UTF_8))));
      }
      catch(Exception e)
      {
         throw (new IllegalStateException("SHA-256 is not available", e));
      }
   }



   /*******************************************************************************
    ** Trusts a chain if either the custom (private CA) or the default trust
    ** manager trusts it.
    *******************************************************************************/
   private record CompositeX509TrustManager(X509TrustManager custom, X509TrustManager fallback) implements X509TrustManager
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
      {
         try
         {
            custom.checkClientTrusted(chain, authType);
         }
         catch(CertificateException e)
         {
            fallback.checkClientTrusted(chain, authType);
         }
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
      {
         try
         {
            custom.checkServerTrusted(chain, authType);
         }
         catch(CertificateException e)
         {
            fallback.checkServerTrusted(chain, authType);
         }
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public X509Certificate[] getAcceptedIssuers()
      {
         X509Certificate[] customIssuers   = custom.getAcceptedIssuers();
         X509Certificate[] fallbackIssuers = fallback.getAcceptedIssuers();
         X509Certificate[] all             = Arrays.copyOf(customIssuers, customIssuers.length + fallbackIssuers.length);
         System.arraycopy(fallbackIssuers, 0, all, customIssuers.length, fallbackIssuers.length);
         return (all);
      }
   }

}
