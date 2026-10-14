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


import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import com.kingsrook.qqq.openapi.model.HttpMethod;


/*******************************************************************************
 ** One in-process call to a UI API operation (see UiApiInvoker).  The path is
 ** the operation's template exactly as in the OpenAPI document, relative to the
 ** version base path, e.g. `/table/{tableName}/query`.
 *******************************************************************************/
public class UiApiRequest
{
   private HttpMethod          method;
   private String              path;
   private Map<String, String> pathParams  = new LinkedHashMap<>();
   private Map<String, String> queryParams = new LinkedHashMap<>();
   private Map<String, String> formParams;
   private final List<MultipartFile> multipartFiles = new ArrayList<>();
   private Object              jsonBody;
   private UiApiCredentials    credentials;
   private String              clientDetail;
   private String              userTimezone;
   private String              invokedBy;
   private Duration            timeout     = Duration.ofSeconds(60);



   /*******************************************************************************
    ** Fluent setter for method
    *******************************************************************************/
   public UiApiRequest withMethod(HttpMethod method)
   {
      this.method = method;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for path (the operation's path template)
    *******************************************************************************/
   public UiApiRequest withPath(String path)
   {
      this.path = path;
      return (this);
   }



   /*******************************************************************************
    ** Add a value for a `{name}` in the path template.
    *******************************************************************************/
   public UiApiRequest withPathParam(String name, Object value)
   {
      this.pathParams.put(name, value == null ? null : String.valueOf(value));
      return (this);
   }



   /*******************************************************************************
    ** Add a query-string parameter.
    *******************************************************************************/
   public UiApiRequest withQueryParam(String name, Object value)
   {
      this.queryParams.put(name, value == null ? null : String.valueOf(value));
      return (this);
   }



   /*******************************************************************************
    ** Send a form-encoded body (for operations whose request body is form data).
    *******************************************************************************/
   public UiApiRequest withFormParam(String name, Object value)
   {
      if(this.formParams == null)
      {
         this.formParams = new LinkedHashMap<>();
      }
      this.formParams.put(name, value == null ? null : String.valueOf(value));
      return (this);
   }



   /*******************************************************************************
    ** One file part. The supplier must return a fresh stream for each HTTP attempt.
    *******************************************************************************/
   public record MultipartFile(String fieldName, String fileName, String contentType, Supplier<InputStream> content)
   {
   }



   /*******************************************************************************
    ** Append a streamed file part. Form parameters become multipart text parts.
    *******************************************************************************/
   public UiApiRequest withMultipartFile(String fieldName, String fileName, String contentType, Supplier<InputStream> content)
   {
      multipartFiles.add(new MultipartFile(Objects.requireNonNull(fieldName), Objects.requireNonNull(fileName),
         Objects.requireNonNull(contentType), Objects.requireNonNull(content)));
      return (this);
   }



   /*******************************************************************************
    ** The file parts in insertion order.
    *******************************************************************************/
   public List<MultipartFile> getMultipartFiles()
   {
      return (List.copyOf(multipartFiles));
   }



   /*******************************************************************************
    ** Send a JSON body (serialized with QQQ's JsonUtils).
    *******************************************************************************/
   public UiApiRequest withJsonBody(Object jsonBody)
   {
      this.jsonBody = jsonBody;
      return (this);
   }



   /*******************************************************************************
    ** The signed-in user's session UUID, sent as the sessionUUID cookie.  Required.
    *******************************************************************************/
   public UiApiRequest withSessionUUID(String sessionUUID)
   {
      this.credentials = UiApiCredentials.ofSessionUUID(sessionUUID);
      return (this);
   }



   /*******************************************************************************
    ** The calling user's credentials, as their request carried them (see
    ** UiApiCredentials.fromContext) - forwarded as they were, so the call
    ** authenticates exactly as the caller did, with any authentication module.
    *******************************************************************************/
   public UiApiRequest withCredentials(UiApiCredentials credentials)
   {
      this.credentials = credentials;
      return (this);
   }



   /*******************************************************************************
    ** The user's time zone (X-QQQ-UserTimezone), for relative dates.
    *******************************************************************************/
   public UiApiRequest withUserTimezone(String userTimezone)
   {
      this.userTimezone = userTimezone;
      return (this);
   }



   /*******************************************************************************
    ** Who is calling (X-QQQ-Invoked-By, written to the access log), e.g. assistant.
    *******************************************************************************/
   public UiApiRequest withInvokedBy(String invokedBy)
   {
      this.invokedBy = invokedBy;
      return (this);
   }



   /*******************************************************************************
    ** Per-call timeout (default 60 seconds).
    *******************************************************************************/
   public UiApiRequest withTimeout(Duration timeout)
   {
      this.timeout = timeout;
      return (this);
   }



   /*******************************************************************************
    ** Getter for method
    *******************************************************************************/
   public HttpMethod getMethod()
   {
      return (this.method);
   }



   /*******************************************************************************
    ** Getter for path
    *******************************************************************************/
   public String getPath()
   {
      return (this.path);
   }



   /*******************************************************************************
    ** Getter for pathParams
    *******************************************************************************/
   public Map<String, String> getPathParams()
   {
      return (this.pathParams);
   }



   /*******************************************************************************
    ** Getter for queryParams
    *******************************************************************************/
   public Map<String, String> getQueryParams()
   {
      return (this.queryParams);
   }



   /*******************************************************************************
    ** Getter for formParams
    *******************************************************************************/
   public Map<String, String> getFormParams()
   {
      return (this.formParams);
   }



   /*******************************************************************************
    ** Getter for jsonBody
    *******************************************************************************/
   public Object getJsonBody()
   {
      return (this.jsonBody);
   }



   /*******************************************************************************
    ** Detail appended to the invoker's client channel (channel/detail), e.g.
    ** the assistant turn id.  Letters, digits and . _ : - only.
    *******************************************************************************/
   public UiApiRequest withClientDetail(String clientDetail)
   {
      this.clientDetail = clientDetail;
      return (this);
   }



   /*******************************************************************************
    ** Getter for clientDetail
    *******************************************************************************/
   public String getClientDetail()
   {
      return (this.clientDetail);
   }



   /*******************************************************************************
    ** Getter for credentials
    *******************************************************************************/
   public UiApiCredentials getCredentials()
   {
      return (this.credentials);
   }



   /*******************************************************************************
    ** Getter for userTimezone
    *******************************************************************************/
   public String getUserTimezone()
   {
      return (this.userTimezone);
   }



   /*******************************************************************************
    ** Getter for invokedBy
    *******************************************************************************/
   public String getInvokedBy()
   {
      return (this.invokedBy);
   }



   /*******************************************************************************
    ** Getter for timeout
    *******************************************************************************/
   public Duration getTimeout()
   {
      return (this.timeout);
   }
}
