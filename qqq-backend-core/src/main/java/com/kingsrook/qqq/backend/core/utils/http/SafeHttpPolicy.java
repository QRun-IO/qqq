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

package com.kingsrook.qqq.backend.core.utils.http;


import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/*******************************************************************************
 ** Limits for {@link SafeHttpClient}. Defaults: HTTPS only, no internal
 ** targets, connect 5 s, read 10 s, total 30 s, responses read up to 64 KiB,
 ** redirects never followed (a 3xx is returned as the response).
 *******************************************************************************/
public final class SafeHttpPolicy
{
   private boolean      allowPlainHttp   = false;
   private List<String> internalAllowlist = new ArrayList<>();
   private Duration     connectTimeout   = Duration.ofSeconds(5);
   private Duration     readTimeout      = Duration.ofSeconds(10);
   private Duration     totalTimeout     = Duration.ofSeconds(30);
   private int          maxResponseBytes = 64 * 1024;
   private String       userAgent        = "QQQ-Webhooks/1.0";



   /*******************************************************************************
    ** The defaults.
    *******************************************************************************/
   public static SafeHttpPolicy defaults()
   {
      return (new SafeHttpPolicy());
   }



   /*******************************************************************************
    ** A key that is equal for equal policies (used to share pooled clients).
    *******************************************************************************/
   public String cacheKey()
   {
      return (allowPlainHttp + "|" + String.join(",", internalAllowlist) + "|" + connectTimeout.toMillis() + "|" + readTimeout.toMillis()
         + "|" + totalTimeout.toMillis() + "|" + maxResponseBytes + "|" + userAgent);
   }



   /*******************************************************************************
    ** Allow http:// (default false: https only).
    *******************************************************************************/
   public SafeHttpPolicy withAllowPlainHttp(boolean allowPlainHttp)
   {
      this.allowPlainHttp = allowPlainHttp;
      return (this);
   }



   /*******************************************************************************
    ** Internal targets to allow (see {@link AddressRules}).
    *******************************************************************************/
   public SafeHttpPolicy withInternalAllowlist(List<String> internalAllowlist)
   {
      this.internalAllowlist = internalAllowlist == null ? new ArrayList<>() : new ArrayList<>(internalAllowlist);
      return (this);
   }



   /*******************************************************************************
    ** Connect timeout.
    *******************************************************************************/
   public SafeHttpPolicy withConnectTimeout(Duration connectTimeout)
   {
      this.connectTimeout = Objects.requireNonNull(connectTimeout);
      return (this);
   }



   /*******************************************************************************
    ** Read (socket idle) timeout.
    *******************************************************************************/
   public SafeHttpPolicy withReadTimeout(Duration readTimeout)
   {
      this.readTimeout = Objects.requireNonNull(readTimeout);
      return (this);
   }



   /*******************************************************************************
    ** Whole-request timeout (connect, send and read together).
    *******************************************************************************/
   public SafeHttpPolicy withTotalTimeout(Duration totalTimeout)
   {
      this.totalTimeout = Objects.requireNonNull(totalTimeout);
      return (this);
   }



   /*******************************************************************************
    ** Most response body bytes read; the rest is discarded with the connection.
    *******************************************************************************/
   public SafeHttpPolicy withMaxResponseBytes(int maxResponseBytes)
   {
      this.maxResponseBytes = maxResponseBytes;
      return (this);
   }



   /*******************************************************************************
    ** User-Agent header.
    *******************************************************************************/
   public SafeHttpPolicy withUserAgent(String userAgent)
   {
      this.userAgent = userAgent;
      return (this);
   }



   /*******************************************************************************
    ** Getter for allowPlainHttp.
    *******************************************************************************/
   public boolean isAllowPlainHttp()
   {
      return (allowPlainHttp);
   }



   /*******************************************************************************
    ** Getter for internalAllowlist.
    *******************************************************************************/
   public List<String> getInternalAllowlist()
   {
      return (internalAllowlist);
   }



   /*******************************************************************************
    ** Getter for connectTimeout.
    *******************************************************************************/
   public Duration getConnectTimeout()
   {
      return (connectTimeout);
   }



   /*******************************************************************************
    ** Getter for readTimeout.
    *******************************************************************************/
   public Duration getReadTimeout()
   {
      return (readTimeout);
   }



   /*******************************************************************************
    ** Getter for totalTimeout.
    *******************************************************************************/
   public Duration getTotalTimeout()
   {
      return (totalTimeout);
   }



   /*******************************************************************************
    ** Getter for maxResponseBytes.
    *******************************************************************************/
   public int getMaxResponseBytes()
   {
      return (maxResponseBytes);
   }



   /*******************************************************************************
    ** Getter for userAgent.
    *******************************************************************************/
   public String getUserAgent()
   {
      return (userAgent);
   }
}
