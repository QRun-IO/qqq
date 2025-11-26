/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.middleware.javalin.routeproviders;


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessState;
import com.kingsrook.qqq.backend.core.model.actions.processes.QProcessPayload;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** process payload shared the processes which are used as process-based-router
 ** processes.  e.g., the fields here are those written to and read by
 ** ProcessBasedRouter.
 *******************************************************************************/
public class ProcessBasedRouterPayload extends QProcessPayload
{
   private String                    path;
   private String                    method;
   private Map<String, String>       pathParams;
   private Map<String, List<String>> queryParams;
   private Map<String, List<String>> formParams;
   private Map<String, String>       cookies;
   private String                    bodyString;

   private Integer             statusCode;
   private String              redirectURL;
   private Map<String, String> responseHeaders;
   private String              responseString;
   private byte[]              responseBytes;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public ProcessBasedRouterPayload()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public ProcessBasedRouterPayload(ProcessState processState)
   {
      this.populateFromProcessState(processState);
   }



   /*******************************************************************************
    ** Get a single form parameter value by name.
    **
    ** Convenience method for the common use-case of retrieving a single value
    ** from form parameters, rather than dealing with the List of values that
    ** the formal HTTP interface provides.
    **
    ** @param name the form parameter name
    ** @return the first value for the parameter, or null if not present
    *******************************************************************************/
   public String getFormParam(String name)
   {
      if(formParams != null)
      {
         List<String> values = formParams.get(name);
         if(CollectionUtils.nullSafeHasContents(values))
         {
            return values.get(0);
         }
      }

      return (null);
   }



   /*******************************************************************************
    ** Get a single query parameter value by name.
    **
    ** Convenience method for the common use-case of retrieving a single value
    ** from query parameters, rather than dealing with the List of values that
    ** the formal HTTP interface provides.
    **
    ** @param name the query parameter name
    ** @return the first value for the parameter, or null if not present
    *******************************************************************************/
   public String getQueryParam(String name)
   {
      if(queryParams != null)
      {
         List<String> values = queryParams.get(name);
         if(CollectionUtils.nullSafeHasContents(values))
         {
            return values.get(0);
         }
      }

      return (null);
   }



   /*******************************************************************************
    ** Getter for path
    ** @see #withPath(String)
    *******************************************************************************/
   public String getPath()
   {
      return (this.path);
   }



   /*******************************************************************************
    ** Setter for path
    ** @see #withPath(String)
    *******************************************************************************/
   public void setPath(String path)
   {
      this.path = path;
   }



   /*******************************************************************************
    ** Fluent setter for path
    **
    ** @param path the HTTP request path
    ** @return this
    *******************************************************************************/
   public ProcessBasedRouterPayload withPath(String path)
   {
      this.path = path;
      return (this);
   }



   /*******************************************************************************
    ** Getter for method
    ** @see #withMethod(String)
    *******************************************************************************/
   public String getMethod()
   {
      return (this.method);
   }



   /*******************************************************************************
    ** Setter for method
    ** @see #withMethod(String)
    *******************************************************************************/
   public void setMethod(String method)
   {
      this.method = method;
   }



   /*******************************************************************************
    ** Fluent setter for method
    **
    ** @param method the HTTP method (GET, POST, etc.)
    ** @return this
    *******************************************************************************/
   public ProcessBasedRouterPayload withMethod(String method)
   {
      this.method = method;
      return (this);
   }



   /*******************************************************************************
    ** Getter for pathParams
    ** @see #withPathParams(Map)
    *******************************************************************************/
   public Map<String, String> getPathParams()
   {
      return (this.pathParams);
   }



   /*******************************************************************************
    ** Setter for pathParams
    ** @see #withPathParams(Map)
    *******************************************************************************/
   public void setPathParams(Map<String, String> pathParams)
   {
      this.pathParams = pathParams;
   }



   /*******************************************************************************
    ** Fluent setter for pathParams
    **
    ** @param pathParams map of path parameters extracted from the URL pattern
    ** @return this
    *******************************************************************************/
   public ProcessBasedRouterPayload withPathParams(Map<String, String> pathParams)
   {
      this.pathParams = pathParams;
      return (this);
   }



   /*******************************************************************************
    ** Getter for cookies
    ** @see #withCookies(Map)
    *******************************************************************************/
   public Map<String, String> getCookies()
   {
      return (this.cookies);
   }



   /*******************************************************************************
    ** Setter for cookies
    ** @see #withCookies(Map)
    *******************************************************************************/
   public void setCookies(Map<String, String> cookies)
   {
      this.cookies = cookies;
   }



   /*******************************************************************************
    ** Fluent setter for cookies
    **
    ** @param cookies map of HTTP cookies from the request
    ** @return this
    *******************************************************************************/
   public ProcessBasedRouterPayload withCookies(Map<String, String> cookies)
   {
      this.cookies = cookies;
      return (this);
   }



   /*******************************************************************************
    ** Getter for statusCode
    ** @see #withStatusCode(Integer)
    *******************************************************************************/
   public Integer getStatusCode()
   {
      return (this.statusCode);
   }



   /*******************************************************************************
    ** Setter for statusCode
    ** @see #withStatusCode(Integer)
    *******************************************************************************/
   public void setStatusCode(Integer statusCode)
   {
      this.statusCode = statusCode;
   }



   /*******************************************************************************
    ** Fluent setter for statusCode
    **
    ** @param statusCode HTTP status code for the response (e.g., 200, 404, 500)
    ** @return this
    *******************************************************************************/
   public ProcessBasedRouterPayload withStatusCode(Integer statusCode)
   {
      this.statusCode = statusCode;
      return (this);
   }



   /*******************************************************************************
    ** Getter for responseHeaders
    ** @see #withResponseHeaders(Map)
    *******************************************************************************/
   public Map<String, String> getResponseHeaders()
   {
      return (this.responseHeaders);
   }



   /*******************************************************************************
    ** Setter for responseHeaders
    ** @see #withResponseHeaders(Map)
    *******************************************************************************/
   public void setResponseHeaders(Map<String, String> responseHeaders)
   {
      this.responseHeaders = responseHeaders;
   }



   /*******************************************************************************
    ** Fluent setter for responseHeaders
    **
    ** @param responseHeaders map of HTTP headers to include in the response
    ** @return this
    *******************************************************************************/
   public ProcessBasedRouterPayload withResponseHeaders(Map<String, String> responseHeaders)
   {
      this.responseHeaders = responseHeaders;
      return (this);
   }



   /*******************************************************************************
    ** Getter for responseString
    ** @see #withResponseString(String)
    *******************************************************************************/
   public String getResponseString()
   {
      return (this.responseString);
   }



   /*******************************************************************************
    ** Setter for responseString
    ** @see #withResponseString(String)
    *******************************************************************************/
   public void setResponseString(String responseString)
   {
      this.responseString = responseString;
   }



   /*******************************************************************************
    ** Fluent setter for responseString
    **
    ** @param responseString string content to send in the HTTP response body
    ** @return this
    *******************************************************************************/
   public ProcessBasedRouterPayload withResponseString(String responseString)
   {
      this.responseString = responseString;
      return (this);
   }



   /*******************************************************************************
    ** Getter for responseBytes
    ** @see #withResponseBytes(byte[])
    *******************************************************************************/
   public byte[] getResponseBytes()
   {
      return (this.responseBytes);
   }



   /*******************************************************************************
    ** Setter for responseBytes
    ** @see #withResponseBytes(byte[])
    *******************************************************************************/
   public void setResponseBytes(byte[] responseBytes)
   {
      this.responseBytes = responseBytes;
   }



   /*******************************************************************************
    ** Fluent setter for responseBytes
    **
    ** @param responseBytes binary content to send in the HTTP response body
    ** @return this
    *******************************************************************************/
   public ProcessBasedRouterPayload withResponseBytes(byte[] responseBytes)
   {
      this.responseBytes = responseBytes;
      return (this);
   }



   /*******************************************************************************
    ** Getter for redirectURL
    ** @see #withRedirectURL(String)
    *******************************************************************************/
   public String getRedirectURL()
   {
      return (this.redirectURL);
   }



   /*******************************************************************************
    ** Setter for redirectURL
    ** @see #withRedirectURL(String)
    *******************************************************************************/
   public void setRedirectURL(String redirectURL)
   {
      this.redirectURL = redirectURL;
   }



   /*******************************************************************************
    ** Fluent setter for redirectURL
    **
    ** @param redirectURL URL to redirect the client to instead of sending a body
    ** @return this
    *******************************************************************************/
   public ProcessBasedRouterPayload withRedirectURL(String redirectURL)
   {
      this.redirectURL = redirectURL;
      return (this);
   }



   /*******************************************************************************
    ** Getter for queryParams
    ** @see #withQueryParams(Map)
    *******************************************************************************/
   public Map<String, List<String>> getQueryParams()
   {
      return (this.queryParams);
   }



   /*******************************************************************************
    ** Setter for queryParams
    ** @see #withQueryParams(Map)
    *******************************************************************************/
   public void setQueryParams(Map<String, List<String>> queryParams)
   {
      this.queryParams = queryParams;
   }



   /*******************************************************************************
    ** Fluent setter for queryParams
    **
    ** @param queryParams map of query parameters from the URL
    ** @return this
    *******************************************************************************/
   public ProcessBasedRouterPayload withQueryParams(Map<String, List<String>> queryParams)
   {
      this.queryParams = queryParams;
      return (this);
   }



   /*******************************************************************************
    ** Getter for formParams
    ** @see #withFormParams(Map)
    *******************************************************************************/
   public Map<String, List<String>> getFormParams()
   {
      return (this.formParams);
   }



   /*******************************************************************************
    ** Setter for formParams
    ** @see #withFormParams(Map)
    *******************************************************************************/
   public void setFormParams(Map<String, List<String>> formParams)
   {
      this.formParams = formParams;
   }



   /*******************************************************************************
    ** Fluent setter for formParams
    **
    ** @param formParams map of form parameters from POST request body
    ** @return this
    *******************************************************************************/
   public ProcessBasedRouterPayload withFormParams(Map<String, List<String>> formParams)
   {
      this.formParams = formParams;
      return (this);
   }


   /*******************************************************************************
    ** Getter for bodyString
    ** @see #withBodyString(String)
    *******************************************************************************/
   public String getBodyString()
   {
      return (this.bodyString);
   }



   /*******************************************************************************
    ** Setter for bodyString
    ** @see #withBodyString(String)
    *******************************************************************************/
   public void setBodyString(String bodyString)
   {
      this.bodyString = bodyString;
   }



   /*******************************************************************************
    ** Fluent setter for bodyString
    **
    ** @param bodyString raw request body content as a string
    ** @return this
    *******************************************************************************/
   public ProcessBasedRouterPayload withBodyString(String bodyString)
   {
      this.bodyString = bodyString;
      return (this);
   }
}
