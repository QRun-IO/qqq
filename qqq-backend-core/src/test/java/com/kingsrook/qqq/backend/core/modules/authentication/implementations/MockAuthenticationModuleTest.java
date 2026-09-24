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
