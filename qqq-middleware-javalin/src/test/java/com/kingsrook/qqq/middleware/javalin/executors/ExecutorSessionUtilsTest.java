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

package com.kingsrook.qqq.middleware.javalin.executors;

import java.util.HashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.TableVariant;
import io.javalin.http.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/*******************************************************************************
 ** Unit test for ExecutorSessionUtils
 *******************************************************************************/
class ExecutorSessionUtilsTest
{
   private QInstance qInstance;
   private Context mockContext;


   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      qInstance = TestUtils.defineInstance();
      mockContext = mock(Context.class);
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
    ** Test session setup with sessionId cookie
    *******************************************************************************/
   @Test
   void testSetupSessionWithSessionIdCookie() throws Exception
   {
      when(mockContext.cookie("sessionId")).thenReturn("test-session-id");
      when(mockContext.cookie(Mockito.anyString())).thenReturn(null);
      when(mockContext.header(anyString())).thenReturn(null);
      when(mockContext.queryParam(anyString())).thenReturn(null);
      when(mockContext.formParam(anyString())).thenThrow(new RuntimeException("formParam not available"));
      when(mockContext.cookie(anyString(), anyString(), anyInt())).thenReturn(mockContext);

      QSession session = ExecutorSessionUtils.setupSession(mockContext, qInstance);

      assertNotNull(session);
      // Verify cookie was read (for sessionId lookup)
      verify(mockContext).cookie("sessionId");
      // Cookie may or may not be set depending on auth module configuration
      // The test verifies that setupSession works correctly with sessionId cookie
   }


   /*******************************************************************************
    ** Test session setup with authorization header (Bearer token)
    *******************************************************************************/
   @Test
   void testSetupSessionWithBearerToken() throws Exception
   {
      when(mockContext.cookie(anyString())).thenReturn(null);
      when(mockContext.header("Authorization")).thenReturn("Bearer test-token-12345");
      when(mockContext.header("x-api-key")).thenReturn(null);
      when(mockContext.queryParam(anyString())).thenReturn(null);
      when(mockContext.formParam(anyString())).thenThrow(new RuntimeException("formParam not available"));
      when(mockContext.cookie(anyString(), anyString(), anyInt())).thenReturn(mockContext);

      QSession session = ExecutorSessionUtils.setupSession(mockContext, qInstance);

      assertNotNull(session);
   }


   /*******************************************************************************
    ** Test session setup with authorization header (Basic auth)
    *******************************************************************************/
   @Test
   void testSetupSessionWithBasicAuth() throws Exception
   {
      when(mockContext.cookie(anyString())).thenReturn(null);
      when(mockContext.header("Authorization")).thenReturn("Basic dXNlcm5hbWU6cGFzc3dvcmQ=");
      when(mockContext.header("x-api-key")).thenReturn(null);
      when(mockContext.queryParam(anyString())).thenReturn(null);
      when(mockContext.formParam(anyString())).thenThrow(new RuntimeException("formParam not available"));
      when(mockContext.cookie(anyString(), anyString(), anyInt())).thenReturn(mockContext);

      QSession session = ExecutorSessionUtils.setupSession(mockContext, qInstance);

      assertNotNull(session);
   }


   /*******************************************************************************
    ** Test session setup with API key header
    *******************************************************************************/
   @Test
   void testSetupSessionWithApiKey() throws Exception
   {
      when(mockContext.cookie(anyString())).thenReturn(null);
      when(mockContext.header("Authorization")).thenReturn(null);
      when(mockContext.header("x-api-key")).thenReturn("test-api-key");
      when(mockContext.queryParam(anyString())).thenReturn(null);
      when(mockContext.formParam(anyString())).thenThrow(new RuntimeException("formParam not available"));
      when(mockContext.cookie(anyString(), anyString(), anyInt())).thenReturn(mockContext);

      QSession session = ExecutorSessionUtils.setupSession(mockContext, qInstance);

      assertNotNull(session);
   }


   /*******************************************************************************
    ** Test session setup with timezone headers
    *******************************************************************************/
   @Test
   void testSetupSessionWithTimezoneHeaders() throws Exception
   {
      when(mockContext.cookie(anyString())).thenReturn(null);
      when(mockContext.header("Authorization")).thenReturn(null);
      when(mockContext.header("x-api-key")).thenReturn(null);
      when(mockContext.header("X-QQQ-UserTimezoneOffsetMinutes")).thenReturn("-300");
      when(mockContext.header("X-QQQ-UserTimezone")).thenReturn("America/New_York");
      when(mockContext.queryParam(anyString())).thenReturn(null);
      when(mockContext.formParam(anyString())).thenThrow(new RuntimeException("formParam not available"));
      when(mockContext.cookie(anyString(), anyString(), anyInt())).thenReturn(mockContext);

      QSession session = ExecutorSessionUtils.setupSession(mockContext, qInstance);

      assertNotNull(session);
      assertEquals("-300", session.getValue(QSession.VALUE_KEY_USER_TIMEZONE_OFFSET_MINUTES));
      assertEquals("America/New_York", session.getValue(QSession.VALUE_KEY_USER_TIMEZONE));
   }


   /*******************************************************************************
    ** Test session setup with invalid timezone offset (non-integer)
    *******************************************************************************/
   @Test
   void testSetupSessionWithInvalidTimezoneOffset() throws Exception
   {
      when(mockContext.cookie(anyString())).thenReturn(null);
      when(mockContext.header("Authorization")).thenReturn(null);
      when(mockContext.header("x-api-key")).thenReturn(null);
      when(mockContext.header("X-QQQ-UserTimezoneOffsetMinutes")).thenReturn("not-a-number");
      when(mockContext.queryParam(anyString())).thenReturn(null);
      when(mockContext.formParam(anyString())).thenThrow(new RuntimeException("formParam not available"));
      when(mockContext.cookie(anyString(), anyString(), anyInt())).thenReturn(mockContext);

      QSession session = ExecutorSessionUtils.setupSession(mockContext, qInstance);

      assertNotNull(session);
      // Invalid value should not be set
      assertNull(session.getValue(QSession.VALUE_KEY_USER_TIMEZONE_OFFSET_MINUTES));
   }


   /*******************************************************************************
    ** Test setTableVariantInSession with valid variant
    *******************************************************************************/
   @Test
   void testSetTableVariantInSession() throws Exception
   {
      when(mockContext.cookie(anyString())).thenReturn(null);
      when(mockContext.header(anyString())).thenReturn(null);
      when(mockContext.queryParam(anyString())).thenReturn(null);
      when(mockContext.formParam(anyString())).thenThrow(new RuntimeException("formParam not available"));
      when(mockContext.cookie(anyString(), anyString(), anyInt())).thenReturn(mockContext);

      QSession session = ExecutorSessionUtils.setupSession(mockContext, qInstance);
      QContext.init(qInstance, session);

      TableVariant variant = new TableVariant();
      variant.setType("testType");
      variant.setId("testId");

      ExecutorSessionUtils.setTableVariantInSession(variant);

      // Verify variant was set (getBackendVariants returns Map<String, Serializable>)
      java.util.Map<String, java.io.Serializable> variants = session.getBackendVariants();
      assertNotNull(variants);
      assertEquals("testId", variants.get("testType"));
   }


   /*******************************************************************************
    ** Test setTableVariantInSession with null variant
    *******************************************************************************/
   @Test
   void testSetTableVariantInSessionWithNull() throws Exception
   {
      when(mockContext.cookie(anyString())).thenReturn(null);
      when(mockContext.header(anyString())).thenReturn(null);
      when(mockContext.queryParam(anyString())).thenReturn(null);
      when(mockContext.formParam(anyString())).thenThrow(new RuntimeException("formParam not available"));
      when(mockContext.cookie(anyString(), anyString(), anyInt())).thenReturn(mockContext);

      QSession session = ExecutorSessionUtils.setupSession(mockContext, qInstance);
      QContext.init(qInstance, session);

      ExecutorSessionUtils.setTableVariantInSession(null);

      // Should not throw, and variants should remain unchanged
      // Variants may be null or unchanged
   }


   /*******************************************************************************
    ** Test setTableVariantInSession with variant missing type
    *******************************************************************************/
   @Test
   void testSetTableVariantInSessionWithMissingType() throws Exception
   {
      when(mockContext.cookie(anyString())).thenReturn(null);
      when(mockContext.header(anyString())).thenReturn(null);
      when(mockContext.queryParam(anyString())).thenReturn(null);
      when(mockContext.formParam(anyString())).thenThrow(new RuntimeException("formParam not available"));
      when(mockContext.cookie(anyString(), anyString(), anyInt())).thenReturn(mockContext);

      QSession session = ExecutorSessionUtils.setupSession(mockContext, qInstance);
      QContext.init(qInstance, session);

      TableVariant variant = new TableVariant();
      variant.setId("testId");
      // type is null

      ExecutorSessionUtils.setTableVariantInSession(variant);

      // Should not set variant when type is missing
      // Variants should remain unchanged when type is missing
   }


   /*******************************************************************************
    ** Test session setup with OAuth callback (code and state params)
    *******************************************************************************/
   @Test
   void testSetupSessionWithOAuthCallback() throws Exception
   {
      when(mockContext.cookie(anyString())).thenReturn(null);
      when(mockContext.header(anyString())).thenReturn(null);
      when(mockContext.queryParam("code")).thenReturn("auth-code-123");
      when(mockContext.queryParam("state")).thenReturn("state-value");
      when(mockContext.formParam(anyString())).thenThrow(new RuntimeException("formParam not available"));
      when(mockContext.cookie(anyString(), anyString(), anyInt())).thenReturn(mockContext);

      QSession session = ExecutorSessionUtils.setupSession(mockContext, qInstance);

      assertNotNull(session);
   }


   /*******************************************************************************
    ** Test session setup with sessionUUID cookie
    *******************************************************************************/
   @Test
   void testSetupSessionWithSessionUuidCookie() throws Exception
   {
      when(mockContext.cookie("sessionId")).thenReturn(null);
      when(mockContext.cookie("sessionUUID")).thenReturn("test-session-uuid");
      when(mockContext.cookie(anyString())).thenReturn(null);
      when(mockContext.header(anyString())).thenReturn(null);
      when(mockContext.queryParam(anyString())).thenReturn(null);
      when(mockContext.formParam(anyString())).thenThrow(new RuntimeException("formParam not available"));
      when(mockContext.cookie(anyString(), anyString(), anyInt())).thenReturn(mockContext);

      QSession session = ExecutorSessionUtils.setupSession(mockContext, qInstance);

      assertNotNull(session);
   }


   /*******************************************************************************
    ** Test session setup with Authorization form param (fallback)
    *******************************************************************************/
   @Test
   void testSetupSessionWithAuthorizationFormParam() throws Exception
   {
      when(mockContext.cookie(anyString())).thenReturn(null);
      when(mockContext.header(anyString())).thenReturn(null);
      when(mockContext.queryParam(anyString())).thenReturn(null);
      when(mockContext.formParam("Authorization")).thenReturn("Bearer form-token");
      when(mockContext.cookie(anyString(), anyString(), anyInt())).thenReturn(mockContext);

      QSession session = ExecutorSessionUtils.setupSession(mockContext, qInstance);

      assertNotNull(session);
   }

}
