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

package com.kingsrook.qqq.backend.core.actions.messaging;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.messaging.SendMessageInput;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Unit tests for SendMessageAction — input validation guards.
 **
 ** Happy-path dispatch is covered by integration tests in provider-specific
 ** modules (e.g. qqq-middleware-email). Here we focus on the validation
 ** short-circuits inside execute() that throw before reaching any provider.
 *******************************************************************************/
class SendMessageActionTest extends BaseTest
{

   /*******************************************************************************
    ** A null provider name must throw QException with a descriptive message.
    *******************************************************************************/
   @Test
   void testExecute_nullProviderName_throwsQException()
   {
      SendMessageInput input = new SendMessageInput();
      // messagingProviderName is null by default

      assertThatThrownBy(() -> new SendMessageAction().execute(input))
         .isInstanceOf(QException.class)
         .hasMessageContaining("Messaging provider name was not given");
   }



   /*******************************************************************************
    ** An empty-string provider name must also throw QException.
    *******************************************************************************/
   @Test
   void testExecute_emptyProviderName_throwsQException()
   {
      SendMessageInput input = new SendMessageInput();
      input.setMessagingProviderName("");

      assertThatThrownBy(() -> new SendMessageAction().execute(input))
         .isInstanceOf(QException.class)
         .hasMessageContaining("Messaging provider name was not given");
   }



   /*******************************************************************************
    ** A non-existent provider name (not registered in QInstance) throws QException
    ** whose message includes the provider name for easy debugging.
    *******************************************************************************/
   @Test
   void testExecute_unknownProviderName_throwsQExceptionWithProviderName()
   {
      SendMessageInput input = new SendMessageInput();
      input.setMessagingProviderName("noSuchProvider");

      assertThatThrownBy(() -> new SendMessageAction().execute(input))
         .isInstanceOf(QException.class)
         .hasMessageContaining("noSuchProvider");
   }



   ///////////////////////////////////////////////////////////////////////////
   // Integration-level scenario (requires a real MessagingProviderInterface
   // implementation in the QInstance). Sketch only — wire in a test-double
   // provider registered via QInstance.addMessagingProvider() to verify the
   // happy path:
   //
   //   @Test
   //   void testExecute_validProvider_delegatesToProviderInterface() throws QException
   //   {
   //       QInstance instance = QContext.getQInstance();
   //       instance.addMessagingProvider(new SomeTestMessagingProviderMetaData()
   //           .withName("testProvider")
   //           .withType("testType"));
   //       // Register a MessagingProviderInterface that captures its call:
   //       QMessagingProviderDispatcher.registerProvider("testType", captureProvider);
   //
   //       SendMessageInput input = new SendMessageInput();
   //       input.setMessagingProviderName("testProvider");
   //       new SendMessageAction().execute(input);
   //
   //       assertThat(captureProvider.wasCalled()).isTrue();
   //   }
   ///////////////////////////////////////////////////////////////////////////

}
