/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.slack;


import java.lang.reflect.Proxy;
import java.util.List;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.middleware.javalin.QJavalinImplementation;
import io.javalin.http.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Slack must propagate credential-boundary denials before any protected action.
 *******************************************************************************/
class QSlackImplementationTest
{
   /*******************************************************************************
    ** Clear the shared legacy instance and request state.
    *******************************************************************************/
   @AfterEach
   void cleanup()
   {
      QJavalinImplementation.setQInstance(null);
      QContext.clear();
   }



   /*******************************************************************************
    ** Neither browser-cookie variant may override the supplied bearer credential.
    *******************************************************************************/
   @Test
   void mixedCredentialsFailBeforeAuthenticationOrAction()
   {
      QJavalinImplementation.setQInstance(new QInstance());
      for(String cookie : List.of("sessionId", "sessionUUID"))
      {
         Context context = (Context) Proxy.newProxyInstance(Context.class.getClassLoader(), new Class<?>[] { Context.class }, (proxy, method, args) ->
         {
            if(method.getName().equals("header") && args.length == 1 && "Authorization".equals(args[0]))
            {
               return ("Bearer opaque-test-token");
            }
            if(method.getName().equals("cookie") && args.length == 1)
            {
               return (cookie.equals(args[0]) ? "browser-session" : null);
            }
            throw (new AssertionError("Credential denial must precede request/session mutation: " + method.getName()));
         });
         assertThrows(QPermissionDeniedException.class, () -> QSlackImplementation.setupSession(context, new AbstractActionInput()));
      }
   }
}
