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

package com.kingsrook.qqq.esb.management;


import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.esb.connection.EsbConnectionFactoryBuilder;
import com.kingsrook.qqq.esb.model.QEsbProviderMetaData;


/*******************************************************************************
 * HTTP calls to one provider's broker management API, for the broker
 * adapters: plain java.net.http, with the provider's management credentials
 * (HTTP Basic, when a managementUsername is set).
 *
 * Connecting waits at most EsbConnectionFactoryBuilder.CONNECT_TIMEOUT_MS
 * (5 s), and a whole request at most REQUEST_TIMEOUT (a purge of a big queue
 * can take a while).  Redirects are not followed, so credentials only ever go
 * to the managementUrl's host.
 *
 * Error messages name the provider, never the managementUrl (which could carry
 * credentials).
 *******************************************************************************/
final class EsbManagementHttp
{
   private static final Duration REQUEST_TIMEOUT       = Duration.ofSeconds(60);
   private static final Integer  MAX_ERROR_TEXT_LENGTH = 500;

   ///////////////////////////////////////////////////////////////////////
   // one client for every adapter - an HttpClient is thread-safe, and  //
   // keeps its own threads and connection pool, so it's made just once //
   ///////////////////////////////////////////////////////////////////////
   private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
      .version(HttpClient.Version.HTTP_1_1)
      .connectTimeout(Duration.ofMillis(EsbConnectionFactoryBuilder.CONNECT_TIMEOUT_MS))
      .followRedirects(HttpClient.Redirect.NEVER)
      .build();

   private final String providerName;
   private final String baseUrl;
   private final String authorization;



   /*******************************************************************************
    ** Constructor - for a provider with a managementUrl.
    *******************************************************************************/
   EsbManagementHttp(QEsbProviderMetaData provider)
   {
      this.providerName = provider.getName();
      this.baseUrl = Objects.requireNonNullElse(provider.getManagementUrl(), "").trim().replaceFirst("/+$", "");

      if(StringUtils.hasContent(provider.getManagementUsername()))
      {
         String credentials = provider.getManagementUsername() + ":" + Objects.requireNonNullElse(provider.getManagementPassword(), "");
         this.authorization = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
      }
      else
      {
         this.authorization = null;
      }
   }



   /*******************************************************************************
    ** Start a request to a path (which starts with a slash) under the
    ** managementUrl - with the timeout and credentials set.
    *******************************************************************************/
   HttpRequest.Builder newRequest(String path) throws QException
   {
      HttpRequest.Builder builder;
      try
      {
         builder = HttpRequest.newBuilder(URI.create(baseUrl + path));
      }
      catch(IllegalArgumentException e)
      {
         throw (new QException("The managementUrl of ESB provider " + providerName + " is not a valid http(s) URL"));
      }

      builder.timeout(REQUEST_TIMEOUT);
      if(authorization != null)
      {
         builder.header("Authorization", authorization);
      }
      return (builder);
   }



   /*******************************************************************************
    ** Send a request; action says what it was for (e.g., "purge queue orders"),
    ** for the error message if it can't be sent, or gets no answer.  Any answer,
    ** whatever its status, is returned.
    *******************************************************************************/
   HttpResponse<String> send(HttpRequest request, String action) throws QException
   {
      try
      {
         return (HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)));
      }
      catch(IOException e)
      {
         throw (new QException("Could not reach the management API of ESB provider " + providerName + " (to " + action + ")", e));
      }
      catch(InterruptedException e)
      {
         Thread.currentThread().interrupt();
         throw (new QException("Interrupted calling the management API of ESB provider " + providerName + " (to " + action + ")", e));
      }
   }



   /*******************************************************************************
    ** The managementUrl's origin (scheme://host[:port]) - what a browser on the
    ** management console sends as its Origin header.
    *******************************************************************************/
   String getOrigin() throws QException
   {
      try
      {
         URI uri = URI.create(baseUrl);
         return (uri.getScheme() + "://" + uri.getHost() + (uri.getPort() == -1 ? "" : ":" + uri.getPort()));
      }
      catch(IllegalArgumentException e)
      {
         throw (new QException("The managementUrl of ESB provider " + providerName + " is not a valid http(s) URL"));
      }
   }



   /*******************************************************************************
    ** Getter for providerName
    *******************************************************************************/
   String getProviderName()
   {
      return (providerName);
   }



   /*******************************************************************************
    ** Percent-encode a value as one URL path segment (so, e.g., a slash in it is
    ** %2F, and a space is %20).
    *******************************************************************************/
   static String encodePathSegment(String value)
   {
      return (URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20"));
   }



   /*******************************************************************************
    ** Text from a broker (e.g., an error message), cut short enough to put in an
    ** exception message.
    *******************************************************************************/
   static String abbreviate(String text)
   {
      if(text == null)
      {
         return ("");
      }

      String trimmed = text.trim();
      return (trimmed.length() <= MAX_ERROR_TEXT_LENGTH ? trimmed : trimmed.substring(0, MAX_ERROR_TEXT_LENGTH) + "...");
   }

}
