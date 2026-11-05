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

package com.kingsrook.qqq.middleware.javalin.executors;


import java.io.Serializable;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleInterface;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.OAuth2AuthenticationModule;
import com.kingsrook.qqq.middleware.javalin.executors.io.ManageSessionInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.ManageSessionOutputInterface;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Unit test for ManageSessionExecutor: a bearer-token session never becomes a
 ** stored (cookie) session (login CSRF guard, QRun-IO/qqq#1229 review).
 *******************************************************************************/
class ManageSessionExecutorTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBearerSessionRefused()
   {
      QSession bearer = new QSession();
      bearer.setValue(OAuth2AuthenticationModule.BEARER_SESSION_VALUE_KEY, "true");
      bearer.setValue(OAuth2AuthenticationModule.BEARER_SESSION_EXPIRES_AT_VALUE_KEY, String.valueOf(Instant.now().plusSeconds(600).getEpochSecond()));
      assertThrows(QAuthenticationException.class, () -> ManageSessionExecutor.refuseBearerSession(bearer));

      assertDoesNotThrow(() -> ManageSessionExecutor.refuseBearerSession(new QSession()));
      assertDoesNotThrow(() -> ManageSessionExecutor.refuseBearerSession(null));
   }



   /*******************************************************************************
    ** execute() always reports the authenticated session user as values.user.id
    ** (native CLI/TUI/scanner logins), even with no profile and null frontend
    ** values, and it overwrites a provider-supplied id.
    *******************************************************************************/
   @Test
   void testExecuteReportsAuthenticatedIdentity() throws Exception
   {
      QInstance qInstance = new QInstance();
      qInstance.withInstanceDefaultAuthentication(new QAuthenticationMetaData().withName("fixture").withType(QAuthenticationType.CUSTOM)
         .withModuleCodeReference(new QCodeReference(FixtureAuthenticationModule.class)));
      QContext.init(qInstance, null);
      try
      {
         FixtureAuthenticationModule.frontendValues = null;
         assertEquals("real-user", executeAndGetUser().get("id"));

         FixtureAuthenticationModule.frontendValues = new LinkedHashMap<>(Map.of("user", new LinkedHashMap<>(Map.of("id", "spoofed", "fullName", "Kept"))));
         Map<?, ?> user = executeAndGetUser();
         assertEquals("real-user", user.get("id"));
         assertEquals("Kept", user.get("fullName"));
      }
      finally
      {
         FixtureAuthenticationModule.frontendValues = null;
         QContext.clear();
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static Map<?, ?> executeAndGetUser() throws Exception
   {
      Map<String, Serializable> captured = new LinkedHashMap<>();
      new ManageSessionExecutor().execute(new ManageSessionInput(), new ManageSessionOutputInterface()
      {
         @Override
         public void setUuid(String uuid)
         {
         }



         @Override
         public void setValues(Map<String, Serializable> values)
         {
            captured.putAll(values);
         }
      });
      return (Map<?, ?>) captured.get("user");
   }



   /***************************************************************************
    ** Minimal custom authentication module: returns a session for "real-user".
    ***************************************************************************/
   public static class FixtureAuthenticationModule implements QAuthenticationModuleInterface
   {
      static Map<String, Serializable> frontendValues;



      @Override
      public QSession createSession(QInstance qInstance, Map<String, String> context)
      {
         QSession session = new QSession();
         session.setUser(new QUser().withIdReference("real-user"));
         session.setValuesForFrontend(frontendValues);
         return (session);
      }



      @Override
      public boolean isSessionValid(QInstance instance, QSession session)
      {
         return (true);
      }
   }

}
