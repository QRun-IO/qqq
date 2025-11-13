/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.routeproviders.contexthandlers;


import java.util.HashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import io.javalin.http.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/*******************************************************************************
 ** Unit test for DefaultRouteProviderContextHandler
 *******************************************************************************/
class DefaultRouteProviderContextHandlerTest
{
   private DefaultRouteProviderContextHandler handler;
   private Context mockContext;


   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp()
   {
      handler = new DefaultRouteProviderContextHandler();
      mockContext = mock(Context.class);
   }


   /*******************************************************************************
    ** Test handleRequest captures context information
    *******************************************************************************/
   @Test
   void testHandleRequest()
   {
      RunProcessInput input = new RunProcessInput();

      when(mockContext.path()).thenReturn("/test/path");
      when(mockContext.method()).thenReturn(io.javalin.http.HandlerType.GET);
      when(mockContext.pathParamMap()).thenReturn(new HashMap<>());
      when(mockContext.queryParamMap()).thenReturn(new HashMap<>());
      when(mockContext.cookieMap()).thenReturn(new HashMap<>());
      when(mockContext.headerMap()).thenReturn(new HashMap<>());
      when(mockContext.formParamMap()).thenReturn(new HashMap<>());
      when(mockContext.body()).thenReturn("request body");

      handler.handleRequest(mockContext, input);

      assertEquals("/test/path", input.getValue("path"));
      // method() returns HandlerType enum, stored directly
      assertEquals(io.javalin.http.HandlerType.GET, input.getValue("method"));
      assertEquals("request body", input.getValue("bodyString"));
   }


   /*******************************************************************************
    ** Test handleResponse with redirect
    *******************************************************************************/
   @Test
   void testHandleResponse_Redirect() throws QException
   {
      RunProcessOutput output = new RunProcessOutput();
      output.addValue("redirectURL", "http://example.com");
      output.addValue("statusCode", 301);

      boolean handled = handler.handleResponse(mockContext, output);

      assertTrue(handled);
      verify(mockContext).redirect("http://example.com", io.javalin.http.HttpStatus.forStatus(301));
   }


   /*******************************************************************************
    ** Test handleResponse with status code only
    *******************************************************************************/
   @Test
   void testHandleResponse_StatusCode() throws QException
   {
      RunProcessOutput output = new RunProcessOutput();
      output.addValue("statusCode", 201);

      boolean handled = handler.handleResponse(mockContext, output);

      verify(mockContext).status(201);
   }


   /*******************************************************************************
    ** Test handleResponse with response string
    *******************************************************************************/
   @Test
   void testHandleResponse_ResponseString() throws QException
   {
      RunProcessOutput output = new RunProcessOutput();
      output.addValue("responseString", "response text");

      boolean handled = handler.handleResponse(mockContext, output);

      assertTrue(handled);
      verify(mockContext).result("response text");
   }


   /*******************************************************************************
    ** Test handleResponse with response headers
    *******************************************************************************/
   @Test
   void testHandleResponse_ResponseHeaders() throws QException
   {
      RunProcessOutput output = new RunProcessOutput();
      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");
      headers.put("X-Custom", "value");
      output.addValue("responseHeaders", (java.io.Serializable) headers);

      when(mockContext.header(anyString(), anyString())).thenReturn(mockContext);

      handler.handleResponse(mockContext, output);

      verify(mockContext).header("Content-Type", "application/json");
      verify(mockContext).header("X-Custom", "value");
   }


   /*******************************************************************************
    ** Test handleResponse with response bytes
    *******************************************************************************/
   @Test
   void testHandleResponse_ResponseBytes() throws QException
   {
      RunProcessOutput output = new RunProcessOutput();
      output.addValue("responseBytes", new byte[]{1, 2, 3, 4});

      boolean handled = handler.handleResponse(mockContext, output);

      assertTrue(handled);
      verify(mockContext).result(new byte[]{1, 2, 3, 4});
   }

}
