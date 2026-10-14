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

package com.kingsrook.qqq.api.model.metadata;


import java.util.ArrayList;
import java.util.List;


/*******************************************************************************
 ** HTTP behaviour of an API instance served to the public internet, typically
 ** behind a CDN: shared-cache headers and validators for anonymous reads, and an
 ** explicit CORS policy.
 **
 ** <p>A <b>public read</b> is a GET or HEAD request that carries no credentials
 ** (no Authorization or x-api-key header, no session cookie) and answers 200.
 ** Public reads get {@code Cache-Control: public, max-age=N, s-maxage=N,
 ** stale-while-revalidate=S, stale-if-error=E}, a matching
 ** {@code CDN-Cache-Control} (RFC 9213), a weak ETag over the body, and
 ** {@code 304 Not Modified} for a matching {@code If-None-Match}.  A public read
 ** whose handler already set a Cache-Control keeps it (a plain
 ** {@code public, max-age=N} is expanded the same way).  Every other response
 ** (credentials, writes, errors) is {@code private, no-store}, so a shared cache
 ** never stores an answer that depends on who asked.</p>
 **
 ** <p>CORS: public reads answer {@code Access-Control-Allow-Origin: *} with no
 ** credentials and no {@code Vary: Origin}, so one cached copy serves every
 ** caller.  Other requests are allowed only from {@link #getCorsAllowedOrigins()}
 ** (echoed, with {@code Vary: Origin}); preflight OPTIONS requests answer 204.</p>
 **
 ** <p>An ApiInstanceMetaData without a policy keeps the historical behaviour (no
 ** cache headers; CORS reflects any origin).</p>
 *******************************************************************************/
public class ApiHttpPolicy
{
   public static final List<String> CORS_ALLOWED_HEADERS = List.of("Authorization", "Content-Type", "Accept", "If-None-Match", "x-api-key");
   public static final List<String> CORS_EXPOSED_HEADERS = List.of("ETag", "Location", "Link", "Retry-After");
   public static final int          CORS_MAX_AGE_SECONDS = 3600;

   private Integer      publicReadMaxAgeSeconds     = 60;
   private Integer      staleWhileRevalidateSeconds = 60;
   private Integer      staleIfErrorSeconds         = 86400;
   private boolean      etags                       = true;
   private int          maxEtagBodyBytes            = 8 * 1024 * 1024;
   private List<String> corsAllowedOrigins          = new ArrayList<>();



   /*******************************************************************************
    ** The Cache-Control value for a public read with the given max age.
    *******************************************************************************/
   public String publicCacheControl(int maxAgeSeconds)
   {
      return ("public, max-age=" + maxAgeSeconds + ", s-maxage=" + maxAgeSeconds + cacheExtensions());
   }



   /*******************************************************************************
    ** The CDN-Cache-Control value (RFC 9213) for a public read.
    *******************************************************************************/
   public String cdnCacheControl(int maxAgeSeconds)
   {
      return ("max-age=" + maxAgeSeconds + cacheExtensions());
   }



   /*******************************************************************************
    ** stale-while-revalidate / stale-if-error suffix (may be empty).
    *******************************************************************************/
   private String cacheExtensions()
   {
      StringBuilder rs = new StringBuilder();
      if(staleWhileRevalidateSeconds != null)
      {
         rs.append(", stale-while-revalidate=").append(staleWhileRevalidateSeconds);
      }
      if(staleIfErrorSeconds != null)
      {
         rs.append(", stale-if-error=").append(staleIfErrorSeconds);
      }
      return (rs.toString());
   }



   /*******************************************************************************
    ** Whether an origin may make credentialed / non-public requests.
    *******************************************************************************/
   public boolean isCorsAllowedOrigin(String origin)
   {
      return (origin != null && corsAllowedOrigins != null && corsAllowedOrigins.contains(origin));
   }



   /*******************************************************************************
    ** Getter for publicReadMaxAgeSeconds (null = public reads get no cache headers).
    *******************************************************************************/
   public Integer getPublicReadMaxAgeSeconds()
   {
      return (this.publicReadMaxAgeSeconds);
   }



   /*******************************************************************************
    ** Setter for publicReadMaxAgeSeconds
    *******************************************************************************/
   public void setPublicReadMaxAgeSeconds(Integer publicReadMaxAgeSeconds)
   {
      this.publicReadMaxAgeSeconds = publicReadMaxAgeSeconds;
   }



   /*******************************************************************************
    ** Fluent setter for publicReadMaxAgeSeconds
    *******************************************************************************/
   public ApiHttpPolicy withPublicReadMaxAgeSeconds(Integer publicReadMaxAgeSeconds)
   {
      this.publicReadMaxAgeSeconds = publicReadMaxAgeSeconds;
      return (this);
   }



   /*******************************************************************************
    ** Getter for staleWhileRevalidateSeconds
    *******************************************************************************/
   public Integer getStaleWhileRevalidateSeconds()
   {
      return (this.staleWhileRevalidateSeconds);
   }



   /*******************************************************************************
    ** Setter for staleWhileRevalidateSeconds
    *******************************************************************************/
   public void setStaleWhileRevalidateSeconds(Integer staleWhileRevalidateSeconds)
   {
      this.staleWhileRevalidateSeconds = staleWhileRevalidateSeconds;
   }



   /*******************************************************************************
    ** Fluent setter for staleWhileRevalidateSeconds
    *******************************************************************************/
   public ApiHttpPolicy withStaleWhileRevalidateSeconds(Integer staleWhileRevalidateSeconds)
   {
      this.staleWhileRevalidateSeconds = staleWhileRevalidateSeconds;
      return (this);
   }



   /*******************************************************************************
    ** Getter for staleIfErrorSeconds
    *******************************************************************************/
   public Integer getStaleIfErrorSeconds()
   {
      return (this.staleIfErrorSeconds);
   }



   /*******************************************************************************
    ** Setter for staleIfErrorSeconds
    *******************************************************************************/
   public void setStaleIfErrorSeconds(Integer staleIfErrorSeconds)
   {
      this.staleIfErrorSeconds = staleIfErrorSeconds;
   }



   /*******************************************************************************
    ** Fluent setter for staleIfErrorSeconds
    *******************************************************************************/
   public ApiHttpPolicy withStaleIfErrorSeconds(Integer staleIfErrorSeconds)
   {
      this.staleIfErrorSeconds = staleIfErrorSeconds;
      return (this);
   }



   /*******************************************************************************
    ** Getter for etags
    *******************************************************************************/
   public boolean getEtags()
   {
      return (this.etags);
   }



   /*******************************************************************************
    ** Setter for etags
    *******************************************************************************/
   public void setEtags(boolean etags)
   {
      this.etags = etags;
   }



   /*******************************************************************************
    ** Fluent setter for etags
    *******************************************************************************/
   public ApiHttpPolicy withEtags(boolean etags)
   {
      this.etags = etags;
      return (this);
   }



   /*******************************************************************************
    ** Getter for maxEtagBodyBytes
    *******************************************************************************/
   public int getMaxEtagBodyBytes()
   {
      return (this.maxEtagBodyBytes);
   }



   /*******************************************************************************
    ** Setter for maxEtagBodyBytes
    *******************************************************************************/
   public void setMaxEtagBodyBytes(int maxEtagBodyBytes)
   {
      this.maxEtagBodyBytes = maxEtagBodyBytes;
   }



   /*******************************************************************************
    ** Fluent setter for maxEtagBodyBytes
    *******************************************************************************/
   public ApiHttpPolicy withMaxEtagBodyBytes(int maxEtagBodyBytes)
   {
      this.maxEtagBodyBytes = maxEtagBodyBytes;
      return (this);
   }



   /*******************************************************************************
    ** Getter for corsAllowedOrigins
    *******************************************************************************/
   public List<String> getCorsAllowedOrigins()
   {
      return (this.corsAllowedOrigins);
   }



   /*******************************************************************************
    ** Setter for corsAllowedOrigins
    *******************************************************************************/
   public void setCorsAllowedOrigins(List<String> corsAllowedOrigins)
   {
      this.corsAllowedOrigins = corsAllowedOrigins;
   }



   /*******************************************************************************
    ** Fluent setter for corsAllowedOrigins
    *******************************************************************************/
   public ApiHttpPolicy withCorsAllowedOrigins(List<String> corsAllowedOrigins)
   {
      this.corsAllowedOrigins = corsAllowedOrigins;
      return (this);
   }
}
