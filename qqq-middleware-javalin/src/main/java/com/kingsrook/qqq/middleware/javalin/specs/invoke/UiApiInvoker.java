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

package com.kingsrook.qqq.middleware.javalin.specs.invoke;


import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.middleware.javalin.QApplicationJavalinServer;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractMiddlewareVersion;
import com.kingsrook.qqq.middleware.javalin.specs.CompleteOperation;
import com.kingsrook.qqq.middleware.javalin.specs.extensions.MiddlewareSpecExtensionsMetaData;


/*******************************************************************************
 ** Invokes UI API (`/qqq/v1`) operations from inside the server, with a given
 ** user's session, with exactly the semantics of an HTTP client: the call is
 ** a loopback HTTP request to the running server, so it passes through the
 ** same session setup, permission checks, filters, logging and error mapping
 ** as a request from the UI.  Used by the AI assistant, which must be held to
 ** the user's own security boundaries.  See docs/UI-API-EXTENSIONS.md.
 **
 ** - Only operations described by an endpoint spec (core or extension) can be
 **   invoked; anything else is refused before a request is made.
 ** - The caller's credentials are required and are forwarded exactly as the
 **   caller's request carried them (session cookies, Authorization, x-api-key),
 **   so the call authenticates as the same user with any authentication module;
 **   there is no service session and no elevation.
 ** - Nested invocation on one thread is limited to MAX_DEPTH.
 *******************************************************************************/
public class UiApiInvoker
{
   public static final int    MAX_DEPTH          = 8;
   public static final String INVOKED_BY_HEADER  = "X-QQQ-Invoked-By";
   public static final String TIMEZONE_HEADER    = "X-QQQ-UserTimezone";
   public static final String CLIENT_CHANNEL_HEADER = "X-QQQ-Client";

   /*******************************************************************************
    ** Credentials and the client channel are always set server side. Reserve all
    ** invoker-owned headers so custom headers cannot forge identity or override
    ** request metadata, even when an optional server-side value is absent.
    *******************************************************************************/
   private static final List<String> RESERVED_HEADERS = List.of("Cookie", "Authorization", "X-Api-Key", "Content-Type", "Accept",
      CLIENT_CHANNEL_HEADER, INVOKED_BY_HEADER, TIMEZONE_HEADER);

   private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

   private final Supplier<String>            baseUrlSupplier;
   private final AbstractMiddlewareVersion   middlewareVersion;
   private final HttpClient                  httpClient;
   private String                            clientChannel = "api";



   /*******************************************************************************
    ** Constructor
    **
    ** @param baseUrlSupplier   scheme, host and port of the server, e.g. http://127.0.0.1:8000
    ** @param middlewareVersion the UI API version whose operations may be invoked
    *******************************************************************************/
   public UiApiInvoker(Supplier<String> baseUrlSupplier, AbstractMiddlewareVersion middlewareVersion)
   {
      this.baseUrlSupplier = baseUrlSupplier;
      this.middlewareVersion = middlewareVersion;
      this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
   }



   /*******************************************************************************
    ** An invoker for a running server's first (current) UI API version, calling
    ** it on the loopback interface.
    *******************************************************************************/
   public static UiApiInvoker forServer(QApplicationJavalinServer server)
   {
      List<AbstractMiddlewareVersion> versions = server.getMiddlewareVersionList();
      if(versions == null || versions.isEmpty())
      {
         throw (new IllegalStateException("The server serves no UI API version"));
      }
      return (new UiApiInvoker(() -> "http://127.0.0.1:" + server.getBoundPort(), versions.get(0)));
   }



   /*******************************************************************************
    ** An invoker for a running server, labelling every call with a client
    ** channel (e.g. assistant) - fixed here, server side: a request can only add
    ** detail (assistant/<turnId>), never change or remove the channel.
    *******************************************************************************/
   public static UiApiInvoker forServer(QApplicationJavalinServer server, String clientChannel)
   {
      return (forServer(server).withClientChannel(clientChannel));
   }



   /*******************************************************************************
    ** Set the client channel name every call is labelled with (default api).
    *******************************************************************************/
   public UiApiInvoker withClientChannel(String clientChannel)
   {
      if(!StringUtils.hasContent(clientChannel) || clientChannel.contains("/"))
      {
         throw (new IllegalArgumentException("A client channel name (without /detail) is required"));
      }
      this.clientChannel = clientChannel.strip().toLowerCase();
      return (this);
   }



   /*******************************************************************************
    ** Getter for clientChannel
    *******************************************************************************/
   public String getClientChannel()
   {
      return (this.clientChannel);
   }



   /*******************************************************************************
    ** An invoker for a server at a given base URL (scheme, host and port).
    *******************************************************************************/
   public static UiApiInvoker forBaseUrl(String baseUrl, AbstractMiddlewareVersion middlewareVersion)
   {
      return (new UiApiInvoker(() -> baseUrl, middlewareVersion));
   }



   /*******************************************************************************
    ** Every operation that can be invoked, from the same specs that generate the
    ** OpenAPI document.
    *******************************************************************************/
   public List<UiApiOperation> listOperations()
   {
      List<UiApiOperation> rs = new ArrayList<>();
      for(AbstractEndpointSpec<?, ?, ?> spec : middlewareVersion.getAllEndpointSpecs())
      {
         CompleteOperation operation = spec.defineCompleteOperation();
         rs.add(new UiApiOperation(operation.getHttpMethod(), operation.getPath(), operation.getShortSummary(),
            operation.getTag() == null ? null : operation.getTag().getText(), spec.isSecured(), operation.getMethod(), spec.getClass().getName()));
      }
      return (rs);
   }



   /*******************************************************************************
    ** Find the operation for a method and path template.
    *******************************************************************************/
   public Optional<UiApiOperation> findOperation(com.kingsrook.qqq.openapi.model.HttpMethod method, String pathTemplate)
   {
      String normalized = MiddlewareSpecExtensionsMetaData.normalizePathTemplate(pathTemplate);
      return (listOperations().stream()
         .filter(o -> o.method() == method && MiddlewareSpecExtensionsMetaData.normalizePathTemplate(o.pathTemplate()).equals(normalized))
         .findFirst());
   }



   /*******************************************************************************
    ** Invoke an operation with the request's session.
    *******************************************************************************/
   public UiApiResponse invoke(UiApiRequest request) throws QException
   {
      if(request.getMethod() == null || !StringUtils.hasContent(request.getPath()))
      {
         throw (new QException("A UI API request needs a method and a path template"));
      }

      UiApiOperation operation = findOperation(request.getMethod(), request.getPath())
         .orElseThrow(() -> new QException("Not a UI API operation: " + request.getMethod() + " " + request.getPath() + " (only operations in the OpenAPI document can be invoked)"));

      if(operation.secured() && (request.getCredentials() == null || !request.getCredentials().isPresent()))
      {
         throw (new QException("The caller's credentials (session) are required to invoke " + request.getMethod() + " " + request.getPath()));
      }

      if(DEPTH.get() >= MAX_DEPTH)
      {
         throw (new QException("UI API invocations nested more than " + MAX_DEPTH + " deep"));
      }

      DEPTH.set(DEPTH.get() + 1);
      try
      {
         HttpResponse<String> response = httpClient.send(buildHttpRequest(operation, request), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
         return (new UiApiResponse(response.statusCode(), response.headers().map(), response.body()));
      }
      catch(InterruptedException e)
      {
         Thread.currentThread().interrupt();
         throw (new QException("Interrupted invoking " + request.getMethod() + " " + request.getPath(), e));
      }
      catch(Exception e)
      {
         throw (new QException("Error invoking " + request.getMethod() + " " + request.getPath() + ": " + e.getMessage(), e));
      }
      finally
      {
         DEPTH.set(DEPTH.get() - 1);
      }
   }



   /*******************************************************************************
    ** Build the loopback HTTP request.
    *******************************************************************************/
   HttpRequest buildHttpRequest(UiApiOperation operation, UiApiRequest request) throws QException
   {
      String path = operation.pathTemplate();
      for(Map.Entry<String, String> entry : request.getPathParams().entrySet())
      {
         path = path.replace("{" + entry.getKey() + "}", encode(entry.getValue()).replace("+", "%20"));
      }
      if(path.matches(".*\\{[^}]+}.*"))
      {
         throw (new QException("Missing path parameter(s) for " + operation.pathTemplate() + ": " + path));
      }

      String url = baseUrlSupplier.get().replaceFirst("/+$", "") + ("/qqq/" + middlewareVersion.getVersionBasePath() + path).replaceAll("/+", "/");
      if(!request.getQueryParams().isEmpty())
      {
         url += "?" + formEncode(request.getQueryParams());
      }

      HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.noBody();
      String                    contentType = null;
      if(request.getJsonBody() != null)
      {
         body = HttpRequest.BodyPublishers.ofString(JsonUtils.toJson(request.getJsonBody()), StandardCharsets.UTF_8);
         contentType = "application/json";
      }
      else if(request.getFormParams() != null)
      {
         body = HttpRequest.BodyPublishers.ofString(formEncode(request.getFormParams()), StandardCharsets.UTF_8);
         contentType = "application/x-www-form-urlencoded";
      }

      HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
         .timeout(request.getTimeout())
         .method(operation.method().name(), body);

      for(Map.Entry<String, String> entry : request.getHeaders().entrySet())
      {
         if(entry.getValue() != null && RESERVED_HEADERS.stream().noneMatch(header -> header.equalsIgnoreCase(entry.getKey())))
         {
            builder.header(entry.getKey(), entry.getValue());
         }
      }

      if(contentType != null)
      {
         builder.header("Content-Type", contentType);
      }
      UiApiCredentials credentials = request.getCredentials();
      if(credentials != null)
      {
         List<String> cookies = new ArrayList<>();
         if(StringUtils.hasContent(credentials.sessionId()))
         {
            cookies.add("sessionId=" + credentials.sessionId());
         }
         if(StringUtils.hasContent(credentials.sessionUUID()))
         {
            cookies.add("sessionUUID=" + credentials.sessionUUID());
         }
         if(!cookies.isEmpty())
         {
            builder.header("Cookie", String.join("; ", cookies));
         }
         if(StringUtils.hasContent(credentials.authorization()))
         {
            builder.header("Authorization", credentials.authorization());
         }
         if(StringUtils.hasContent(credentials.apiKey()))
         {
            builder.header("x-api-key", credentials.apiKey());
         }
      }
      if(StringUtils.hasContent(request.getUserTimezone()))
      {
         builder.header(TIMEZONE_HEADER, request.getUserTimezone());
      }
      builder.header(INVOKED_BY_HEADER, StringUtils.hasContent(request.getInvokedBy()) ? request.getInvokedBy() : "in-process");

      ////////////////////////////////////////////////////////////////////////////////
      // the client channel is always this invoker's: the request may only add the //
      // detail after the slash (e.g. a turn id) - it cannot change or remove it    //
      ////////////////////////////////////////////////////////////////////////////////
      String detail = request.getClientDetail() == null ? "" : request.getClientDetail().replaceAll("[^A-Za-z0-9._:-]", "");
      builder.header(CLIENT_CHANNEL_HEADER, clientChannel + (detail.isEmpty() ? "" : "/" + detail));
      return (builder.build());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String formEncode(Map<String, String> params)
   {
      return (params.entrySet().stream()
         .map(e -> encode(e.getKey()) + "=" + encode(e.getValue() == null ? "" : e.getValue()))
         .collect(Collectors.joining("&")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String encode(String value)
   {
      return (URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8));
   }
}
