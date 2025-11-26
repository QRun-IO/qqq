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


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for ProcessBasedRouterPayload
 *******************************************************************************/
class ProcessBasedRouterPayloadTest
{
   /*******************************************************************************
    ** Test default constructor
    *******************************************************************************/
   @Test
   void testDefaultConstructor()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      
      assertNotNull(payload);
      assertNull(payload.getPath());
      assertNull(payload.getMethod());
      assertNull(payload.getPathParams());
      assertNull(payload.getQueryParams());
      assertNull(payload.getFormParams());
      assertNull(payload.getCookies());
      assertNull(payload.getBodyString());
      assertNull(payload.getStatusCode());
      assertNull(payload.getRedirectURL());
      assertNull(payload.getResponseHeaders());
      assertNull(payload.getResponseString());
      assertNull(payload.getResponseBytes());
   }


   /*******************************************************************************
    ** Test constructor with ProcessState
    *******************************************************************************/
   @Test
   void testConstructorWithProcessState()
   {
      ProcessState processState = new ProcessState();
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload(processState);
      
      assertNotNull(payload);
   }


   /*******************************************************************************
    ** Test path getter/setter
    *******************************************************************************/
   @Test
   void testPathGetterSetter()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      
      payload.setPath("/api/users");
      assertEquals("/api/users", payload.getPath());
   }


   /*******************************************************************************
    ** Test path fluent setter
    *******************************************************************************/
   @Test
   void testPathFluentSetter()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      
      ProcessBasedRouterPayload result = payload.withPath("/api/users");
      
      assertEquals(payload, result);
      assertEquals("/api/users", payload.getPath());
   }


   /*******************************************************************************
    ** Test method getter/setter
    *******************************************************************************/
   @Test
   void testMethodGetterSetter()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      
      payload.setMethod("POST");
      assertEquals("POST", payload.getMethod());
   }


   /*******************************************************************************
    ** Test method fluent setter
    *******************************************************************************/
   @Test
   void testMethodFluentSetter()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      
      ProcessBasedRouterPayload result = payload.withMethod("GET");
      
      assertEquals(payload, result);
      assertEquals("GET", payload.getMethod());
   }


   /*******************************************************************************
    ** Test pathParams getter/setter
    *******************************************************************************/
   @Test
   void testPathParamsGetterSetter()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      Map<String, String> pathParams = new HashMap<>();
      pathParams.put("id", "123");
      
      payload.setPathParams(pathParams);
      assertEquals(pathParams, payload.getPathParams());
      assertEquals("123", payload.getPathParams().get("id"));
   }


   /*******************************************************************************
    ** Test pathParams fluent setter
    *******************************************************************************/
   @Test
   void testPathParamsFluentSetter()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      Map<String, String> pathParams = new HashMap<>();
      pathParams.put("userId", "456");
      
      ProcessBasedRouterPayload result = payload.withPathParams(pathParams);
      
      assertEquals(payload, result);
      assertEquals(pathParams, payload.getPathParams());
   }


   /*******************************************************************************
    ** Test queryParams getter/setter
    *******************************************************************************/
   @Test
   void testQueryParamsGetterSetter()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      Map<String, List<String>> queryParams = new HashMap<>();
      queryParams.put("page", List.of("1"));
      queryParams.put("size", List.of("10"));
      
      payload.setQueryParams(queryParams);
      assertEquals(queryParams, payload.getQueryParams());
      assertEquals(List.of("1"), payload.getQueryParams().get("page"));
   }


   /*******************************************************************************
    ** Test queryParams fluent setter
    *******************************************************************************/
   @Test
   void testQueryParamsFluentSetter()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      Map<String, List<String>> queryParams = new HashMap<>();
      queryParams.put("filter", List.of("active"));
      
      ProcessBasedRouterPayload result = payload.withQueryParams(queryParams);
      
      assertEquals(payload, result);
      assertEquals(queryParams, payload.getQueryParams());
   }


   /*******************************************************************************
    ** Test getQueryParam - single value
    *******************************************************************************/
   @Test
   void testGetQueryParamSingleValue()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      Map<String, List<String>> queryParams = new HashMap<>();
      queryParams.put("name", List.of("John"));
      payload.setQueryParams(queryParams);
      
      assertEquals("John", payload.getQueryParam("name"));
   }


   /*******************************************************************************
    ** Test getQueryParam - multiple values (returns first)
    *******************************************************************************/
   @Test
   void testGetQueryParamMultipleValues()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      Map<String, List<String>> queryParams = new HashMap<>();
      queryParams.put("tag", List.of("first", "second", "third"));
      payload.setQueryParams(queryParams);
      
      assertEquals("first", payload.getQueryParam("tag"));
   }


   /*******************************************************************************
    ** Test getQueryParam - param not present
    *******************************************************************************/
   @Test
   void testGetQueryParamNotPresent()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      Map<String, List<String>> queryParams = new HashMap<>();
      queryParams.put("existing", List.of("value"));
      payload.setQueryParams(queryParams);
      
      assertNull(payload.getQueryParam("missing"));
   }


   /*******************************************************************************
    ** Test getQueryParam - null queryParams
    *******************************************************************************/
   @Test
   void testGetQueryParamNullQueryParams()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      
      assertNull(payload.getQueryParam("any"));
   }


   /*******************************************************************************
    ** Test getQueryParam - empty list
    *******************************************************************************/
   @Test
   void testGetQueryParamEmptyList()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      Map<String, List<String>> queryParams = new HashMap<>();
      queryParams.put("empty", List.of());
      payload.setQueryParams(queryParams);
      
      assertNull(payload.getQueryParam("empty"));
   }


   /*******************************************************************************
    ** Test formParams getter/setter
    *******************************************************************************/
   @Test
   void testFormParamsGetterSetter()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      Map<String, List<String>> formParams = new HashMap<>();
      formParams.put("username", List.of("testuser"));
      formParams.put("password", List.of("secret"));
      
      payload.setFormParams(formParams);
      assertEquals(formParams, payload.getFormParams());
   }


   /*******************************************************************************
    ** Test formParams fluent setter
    *******************************************************************************/
   @Test
   void testFormParamsFluentSetter()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      Map<String, List<String>> formParams = new HashMap<>();
      formParams.put("email", List.of("test@example.com"));
      
      ProcessBasedRouterPayload result = payload.withFormParams(formParams);
      
      assertEquals(payload, result);
      assertEquals(formParams, payload.getFormParams());
   }


   /*******************************************************************************
    ** Test getFormParam - single value
    *******************************************************************************/
   @Test
   void testGetFormParamSingleValue()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      Map<String, List<String>> formParams = new HashMap<>();
      formParams.put("field1", List.of("value1"));
      payload.setFormParams(formParams);
      
      assertEquals("value1", payload.getFormParam("field1"));
   }


   /*******************************************************************************
    ** Test getFormParam - multiple values (returns first)
    *******************************************************************************/
   @Test
   void testGetFormParamMultipleValues()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      Map<String, List<String>> formParams = new HashMap<>();
      formParams.put("options", List.of("opt1", "opt2", "opt3"));
      payload.setFormParams(formParams);
      
      assertEquals("opt1", payload.getFormParam("options"));
   }


   /*******************************************************************************
    ** Test getFormParam - param not present
    *******************************************************************************/
   @Test
   void testGetFormParamNotPresent()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      Map<String, List<String>> formParams = new HashMap<>();
      formParams.put("existing", List.of("value"));
      payload.setFormParams(formParams);
      
      assertNull(payload.getFormParam("missing"));
   }


   /*******************************************************************************
    ** Test getFormParam - null formParams
    *******************************************************************************/
   @Test
   void testGetFormParamNullFormParams()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      
      assertNull(payload.getFormParam("any"));
   }


   /*******************************************************************************
    ** Test getFormParam - empty list
    *******************************************************************************/
   @Test
   void testGetFormParamEmptyList()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      Map<String, List<String>> formParams = new HashMap<>();
      formParams.put("empty", List.of());
      payload.setFormParams(formParams);
      
      assertNull(payload.getFormParam("empty"));
   }


   /*******************************************************************************
    ** Test cookies getter/setter
    *******************************************************************************/
   @Test
   void testCookiesGetterSetter()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      Map<String, String> cookies = new HashMap<>();
      cookies.put("session", "abc123");
      
      payload.setCookies(cookies);
      assertEquals(cookies, payload.getCookies());
   }


   /*******************************************************************************
    ** Test cookies fluent setter
    *******************************************************************************/
   @Test
   void testCookiesFluentSetter()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      Map<String, String> cookies = new HashMap<>();
      cookies.put("token", "xyz789");
      
      ProcessBasedRouterPayload result = payload.withCookies(cookies);
      
      assertEquals(payload, result);
      assertEquals(cookies, payload.getCookies());
   }


   /*******************************************************************************
    ** Test bodyString getter/setter
    *******************************************************************************/
   @Test
   void testBodyStringGetterSetter()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      String bodyString = "{\"key\":\"value\"}";
      
      payload.setBodyString(bodyString);
      assertEquals(bodyString, payload.getBodyString());
   }


   /*******************************************************************************
    ** Test bodyString fluent setter
    *******************************************************************************/
   @Test
   void testBodyStringFluentSetter()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      String bodyString = "<xml>data</xml>";
      
      ProcessBasedRouterPayload result = payload.withBodyString(bodyString);
      
      assertEquals(payload, result);
      assertEquals(bodyString, payload.getBodyString());
   }


   /*******************************************************************************
    ** Test statusCode getter/setter
    *******************************************************************************/
   @Test
   void testStatusCodeGetterSetter()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      
      payload.setStatusCode(200);
      assertEquals(200, payload.getStatusCode());
   }


   /*******************************************************************************
    ** Test statusCode fluent setter
    *******************************************************************************/
   @Test
   void testStatusCodeFluentSetter()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      
      ProcessBasedRouterPayload result = payload.withStatusCode(404);
      
      assertEquals(payload, result);
      assertEquals(404, payload.getStatusCode());
   }


   /*******************************************************************************
    ** Test redirectURL getter/setter
    *******************************************************************************/
   @Test
   void testRedirectURLGetterSetter()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      String redirectURL = "https://example.com/redirect";
      
      payload.setRedirectURL(redirectURL);
      assertEquals(redirectURL, payload.getRedirectURL());
   }


   /*******************************************************************************
    ** Test redirectURL fluent setter
    *******************************************************************************/
   @Test
   void testRedirectURLFluentSetter()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      String redirectURL = "/login";
      
      ProcessBasedRouterPayload result = payload.withRedirectURL(redirectURL);
      
      assertEquals(payload, result);
      assertEquals(redirectURL, payload.getRedirectURL());
   }


   /*******************************************************************************
    ** Test responseHeaders getter/setter
    *******************************************************************************/
   @Test
   void testResponseHeadersGetterSetter()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");
      headers.put("X-Custom-Header", "value");
      
      payload.setResponseHeaders(headers);
      assertEquals(headers, payload.getResponseHeaders());
   }


   /*******************************************************************************
    ** Test responseHeaders fluent setter
    *******************************************************************************/
   @Test
   void testResponseHeadersFluentSetter()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      Map<String, String> headers = new HashMap<>();
      headers.put("Cache-Control", "no-cache");
      
      ProcessBasedRouterPayload result = payload.withResponseHeaders(headers);
      
      assertEquals(payload, result);
      assertEquals(headers, payload.getResponseHeaders());
   }


   /*******************************************************************************
    ** Test responseString getter/setter
    *******************************************************************************/
   @Test
   void testResponseStringGetterSetter()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      String responseString = "Success!";
      
      payload.setResponseString(responseString);
      assertEquals(responseString, payload.getResponseString());
   }


   /*******************************************************************************
    ** Test responseString fluent setter
    *******************************************************************************/
   @Test
   void testResponseStringFluentSetter()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      String responseString = "{\"status\":\"ok\"}";
      
      ProcessBasedRouterPayload result = payload.withResponseString(responseString);
      
      assertEquals(payload, result);
      assertEquals(responseString, payload.getResponseString());
   }


   /*******************************************************************************
    ** Test responseBytes getter/setter
    *******************************************************************************/
   @Test
   void testResponseBytesGetterSetter()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      byte[] responseBytes = new byte[]{1, 2, 3, 4, 5};
      
      payload.setResponseBytes(responseBytes);
      assertArrayEquals(responseBytes, payload.getResponseBytes());
   }


   /*******************************************************************************
    ** Test responseBytes fluent setter
    *******************************************************************************/
   @Test
   void testResponseBytesFluentSetter()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload();
      byte[] responseBytes = "binary data".getBytes();
      
      ProcessBasedRouterPayload result = payload.withResponseBytes(responseBytes);
      
      assertEquals(payload, result);
      assertArrayEquals(responseBytes, payload.getResponseBytes());
   }


   /*******************************************************************************
    ** Test chaining multiple fluent setters
    *******************************************************************************/
   @Test
   void testFluentSetterChaining()
   {
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload()
         .withPath("/api/users/123")
         .withMethod("GET")
         .withStatusCode(200)
         .withResponseString("{\"user\":\"data\"}");
      
      assertEquals("/api/users/123", payload.getPath());
      assertEquals("GET", payload.getMethod());
      assertEquals(200, payload.getStatusCode());
      assertEquals("{\"user\":\"data\"}", payload.getResponseString());
   }


   /*******************************************************************************
    ** Test complete request payload
    *******************************************************************************/
   @Test
   void testCompleteRequestPayload()
   {
      Map<String, String> pathParams = new HashMap<>();
      pathParams.put("id", "123");
      
      Map<String, List<String>> queryParams = new HashMap<>();
      queryParams.put("page", List.of("1"));
      queryParams.put("size", List.of("20"));
      
      Map<String, List<String>> formParams = new HashMap<>();
      formParams.put("name", List.of("Test"));
      
      Map<String, String> cookies = new HashMap<>();
      cookies.put("session", "abc");
      
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload()
         .withPath("/api/users/123")
         .withMethod("POST")
         .withPathParams(pathParams)
         .withQueryParams(queryParams)
         .withFormParams(formParams)
         .withCookies(cookies)
         .withBodyString("{\"data\":\"test\"}");
      
      assertEquals("/api/users/123", payload.getPath());
      assertEquals("POST", payload.getMethod());
      assertEquals("123", payload.getPathParams().get("id"));
      assertEquals("1", payload.getQueryParam("page"));
      assertEquals("Test", payload.getFormParam("name"));
      assertEquals("abc", payload.getCookies().get("session"));
      assertEquals("{\"data\":\"test\"}", payload.getBodyString());
   }


   /*******************************************************************************
    ** Test complete response payload
    *******************************************************************************/
   @Test
   void testCompleteResponsePayload()
   {
      Map<String, String> responseHeaders = new HashMap<>();
      responseHeaders.put("Content-Type", "application/json");
      responseHeaders.put("X-Custom", "value");
      
      ProcessBasedRouterPayload payload = new ProcessBasedRouterPayload()
         .withStatusCode(201)
         .withResponseHeaders(responseHeaders)
         .withResponseString("{\"id\":\"new-resource\"}");
      
      assertEquals(201, payload.getStatusCode());
      assertEquals("application/json", payload.getResponseHeaders().get("Content-Type"));
      assertEquals("{\"id\":\"new-resource\"}", payload.getResponseString());
   }
}

