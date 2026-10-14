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


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
      HttpResponse<String> response = send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      return (new UiApiResponse(response.statusCode(), response.headers().map(), response.body()));
   }



   /*******************************************************************************
    ** Invoke with a streamed body. The caller must close the returned response.
    ** Invocation depth is released on return, independently of stream lifetime.
    *******************************************************************************/
   public UiApiStreamResponse invokeForStream(UiApiRequest request) throws QException
   {
      HttpResponse<InputStream> response = send(request, HttpResponse.BodyHandlers.ofInputStream());
      return (new UiApiStreamResponse(response.statusCode(), response.headers().map(), response.body()));
   }



   /*******************************************************************************
    ** Shared operation validation, credential requirement and invocation depth.
    *******************************************************************************/
   private <T> HttpResponse<T> send(UiApiRequest request, HttpResponse.BodyHandler<T> bodyHandler) throws QException
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

      if(!request.getMultipartFiles().isEmpty() && !declaresMultipartBody(operation))
      {
         throw (new QException("The operation " + request.getMethod() + " " + request.getPath() + " does not accept a multipart/form-data body"));
      }

      if(DEPTH.get() >= MAX_DEPTH)
      {
         throw (new QException("UI API invocations nested more than " + MAX_DEPTH + " deep"));
      }

      DEPTH.set(DEPTH.get() + 1);
      try
      {
         return (httpClient.send(buildHttpRequest(operation, request), bodyHandler));
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
      if(!request.getMultipartFiles().isEmpty() && request.getJsonBody() != null)
      {
         throw (new QException("JSON and multipart bodies are mutually exclusive"));
      }
      ///////////////////////////////////////////////////////////////////////////
      // form fields go as multipart when the operation declares only a        //
      // multipart body (e.g. process step): specs read params only from a     //
      // content type they declare                                             //
      ///////////////////////////////////////////////////////////////////////////
      boolean formAsMultipart = request.getFormParams() != null && request.getJsonBody() == null
         && declaresMultipartBody(operation) && !declaresBodyType(operation, "application/x-www-form-urlencoded");
      if(!request.getMultipartFiles().isEmpty() || formAsMultipart)
      {
         String boundary = "qqq-" + UUID.randomUUID();
         body = multipartBody(request, boundary);
         contentType = "multipart/form-data; boundary=" + boundary;
      }
      else if(request.getJsonBody() != null)
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

      if(contentType != null)
      {
         builder.header("Content-Type", contentType);
      }
      UiApiCredentials credentials = request.getCredentials();
      if(credentials != null && StringUtils.hasContent(credentials.inProcessSessionToken()))
      {
         builder.header(UiApiInProcessSessions.HEADER, credentials.inProcessSessionToken());
      }
      else if(credentials != null)
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
    ** Whether the operation's spec declares a multipart/form-data request body
    ** (process init and step, for file uploads). Only those may carry files.
    *******************************************************************************/
   static boolean declaresMultipartBody(UiApiOperation operation)
   {
      return (declaresBodyType(operation, "multipart/form-data"));
   }



   /*******************************************************************************
    ** Whether the operation's spec declares a request body of the given type.
    *******************************************************************************/
   static boolean declaresBodyType(UiApiOperation operation, String mimeType)
   {
      if(operation.openApi() == null || operation.openApi().getRequestBody() == null || operation.openApi().getRequestBody().getContent() == null)
      {
         return (false);
      }
      return (operation.openApi().getRequestBody().getContent().keySet().stream().anyMatch(type -> type != null && type.toLowerCase().startsWith(mimeType)));
   }



   /*******************************************************************************
    ** Stream multipart parts in order, opening file streams only when consumed.
    ** Each subscription obtains fresh streams from the supplied factories.
    *******************************************************************************/
   private static HttpRequest.BodyPublisher multipartBody(UiApiRequest request, String boundary) throws QException
   {
      List<Supplier<InputStream>> parts = new ArrayList<>();
      if(request.getFormParams() != null)
      {
         request.getFormParams().forEach((name, value) -> parts.add(() -> bytes("--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"" + multipartName(name) + "\"\r\n\r\n"
            + (value == null ? "" : value) + "\r\n")));
      }
      for(UiApiRequest.MultipartFile file : request.getMultipartFiles())
      {
         if(file.contentType().contains("\r") || file.contentType().contains("\n"))
         {
            throw (new QException("Invalid multipart content type"));
         }
         parts.add(() -> bytes("--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"" + multipartName(file.fieldName())
            + "\"; filename=\"" + multipartName(file.fileName()) + "\"\r\n"
            + "Content-Type: " + file.contentType() + "\r\n\r\n"));
         parts.add(file.content());
         parts.add(() -> bytes("\r\n"));
      }
      parts.add(() -> bytes("--" + boundary + "--\r\n"));
      return (HttpRequest.BodyPublishers.ofInputStream(() -> new SequenceInputStream(Collections.enumeration(parts.stream().map(part -> new InputStream()
      {
         private InputStream delegate;


         /*******************************************************************************
          ** Open this part only on its first read.
          *******************************************************************************/
         private InputStream stream()
         {
            if(delegate == null)
            {
               delegate = part.get();
            }
            return (delegate);
         }


         /*******************************************************************************
          ** Read one byte from this part.
          *******************************************************************************/
         @Override
         public int read() throws java.io.IOException
         {
            return (stream().read());
         }


         /*******************************************************************************
          ** Read a chunk without buffering the entire file.
          *******************************************************************************/
         @Override
         public int read(byte[] buffer, int offset, int length) throws java.io.IOException
         {
            return (stream().read(buffer, offset, length));
         }


         /*******************************************************************************
          ** Close opened parts; cancellation must not open unconsumed files.
          *******************************************************************************/
         @Override
         public void close() throws java.io.IOException
         {
            if(delegate != null)
            {
               delegate.close();
            }
         }
      }).toList()))));
   }



   /*******************************************************************************
    ** Encode multipart framing and text fields, never file contents.
    *******************************************************************************/
   private static InputStream bytes(String value)
   {
      return (new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8)));
   }



   /*******************************************************************************
    ** Strip header delimiters and escape quoted-string backslashes.
    *******************************************************************************/
   private static String multipartName(String value)
   {
      return (value.replace("\r", "").replace("\n", "").replace("\"", "").replace("\\", "\\\\"));
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
