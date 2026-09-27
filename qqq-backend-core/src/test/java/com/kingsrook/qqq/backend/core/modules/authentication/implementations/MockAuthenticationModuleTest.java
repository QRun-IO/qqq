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

package com.kingsrook.qqq.backend.core.modules.authentication.implementations;


import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Tests for MockAuthenticationModule, specifically the customizeSession fix
 ** for issue #331.
 *******************************************************************************/
class MockAuthenticationModuleTest extends BaseTest
{

   /*******************************************************************************
    ** Customizer that writes a sentinel value to verify it was called.
    *******************************************************************************/
   public static class SentinelCustomizer implements QAuthenticationModuleCustomizerInterface
   {
      @Override
      public void customizeSession(QInstance qInstance, QSession qSession, Map<String, Object> context)
      {
         qSession.withSecurityKeyValue("sentinelKey", "sentinelValue");
      }
   }



   /*******************************************************************************
    ** createSession() MUST invoke customizeSession() when a customizer is configured
    ** (regression test for issue #331).
    *******************************************************************************/
   @Test
   void testCreateSession_customizerIsCalled() throws Exception
   {
      QInstance qInstance = QContext.getQInstance();
      QAuthenticationMetaData authMetaData = new QAuthenticationMetaData()
         .withName("mock")
         .withCustomizer(new QCodeReference(SentinelCustomizer.class));
      qInstance.registerAuthenticationProvider(AuthScope.instanceDefault(), authMetaData);

      QSession session = new MockAuthenticationModule().createSession(qInstance, Map.of());

      assertNotNull(session, "Session must not be null");
      assertEquals("sentinelValue", session.getSecurityKeyValues("sentinelKey").stream().findFirst().orElse(null),
         "customizeSession must have been called — sentinel security key must be present on session");
   }



   /*******************************************************************************
    ** createSession() MUST NOT throw when no customizer is configured.
    *******************************************************************************/
   @Test
   void testCreateSession_noCustomizer_noException() throws Exception
   {
      QInstance qInstance = QContext.getQInstance();
      QAuthenticationMetaData authMetaData = new QAuthenticationMetaData().withName("mock");
      qInstance.registerAuthenticationProvider(AuthScope.instanceDefault(), authMetaData);

      QSession session = new MockAuthenticationModule().createSession(qInstance, Map.of());

      assertNotNull(session, "Session must not be null when no customizer is configured");
      assertNotNull(session.getUser(), "Session user must not be null");
   }



   /*******************************************************************************
    ** Browser requests retain the session used for server-generated downloads.
    *******************************************************************************/
   @Test
   void testSessionCookieRestoresIdentity() throws Exception
   {
      MockAuthenticationModule module = new MockAuthenticationModule();
      QInstance instance = QContext.getQInstance();
      QSession first = module.createSession(instance, Map.of());
      QSession restored = module.createSession(instance, Map.of("sessionId", first.getUuid()));
      assertTrue(module.usesSessionIdCookie());
      assertEquals(first.getUuid(), restored.getUuid());
      assertEquals(first.getIdReference(), restored.getIdReference());
      assertNotEquals(first.getIdReference(), module.createSession(instance, Map.of()).getIdReference());
   }
}
