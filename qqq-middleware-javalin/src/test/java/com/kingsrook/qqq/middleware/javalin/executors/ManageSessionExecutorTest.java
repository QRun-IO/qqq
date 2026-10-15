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


import java.time.Instant;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.OAuth2AuthenticationModule;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
}
