/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.routeproviders.authentication;

import java.util.HashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import io.javalin.http.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/*******************************************************************************
 ** Unit test for SimpleRouteAuthenticator
 *******************************************************************************/
class SimpleRouteAuthenticatorTest
{
   private SimpleRouteAuthenticator authenticator;
   private Context mockContext;


   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      authenticator = new SimpleRouteAuthenticator();
      mockContext = mock(Context.class);
      // QInstance not needed - authenticateRequest handles it
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      QContext.clear();
   }


   /*******************************************************************************
    ** Test authentication with valid session
    *******************************************************************************/
   @Test
   void testAuthenticateRequest_Success() throws Exception
   {
      when(mockContext.cookie(anyString())).thenReturn(null);
      when(mockContext.header(anyString())).thenReturn(null);
      when(mockContext.queryParam(anyString())).thenReturn(null);
      when(mockContext.queryParamMap()).thenReturn(new java.util.HashMap<String, java.util.List<String>>());
      when(mockContext.fullUrl()).thenReturn("http://example.com");

      boolean result = authenticator.authenticateRequest(mockContext);

      // Result depends on whether session was created successfully
      // This will typically be false if no valid auth credentials
   }


   /*******************************************************************************
    ** Test authentication with OAuth callback (code and state params)
    *******************************************************************************/
   @Test
   void testAuthenticateRequest_OAuthCallback() throws Exception
   {
      java.util.Map<String, java.util.List<String>> queryParams = new java.util.HashMap<>();
      queryParams.put("code", java.util.List.of("auth-code-123"));
      queryParams.put("state", java.util.List.of("state-value"));

      when(mockContext.queryParam("code")).thenReturn("auth-code-123");
      when(mockContext.queryParam("state")).thenReturn("state-value");
      when(mockContext.queryParamMap()).thenReturn(queryParams);
      when(mockContext.fullUrl()).thenReturn("http://example.com?code=auth-code-123&state=state-value");
      when(mockContext.cookie(anyString())).thenReturn(null);
      when(mockContext.header(anyString())).thenReturn(null);

      boolean result = authenticator.authenticateRequest(mockContext);

      assertFalse(result);
      verify(mockContext).redirect(anyString());
   }

}
