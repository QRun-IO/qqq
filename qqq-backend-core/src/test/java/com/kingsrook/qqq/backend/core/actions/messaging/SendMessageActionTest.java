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


import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.messaging.SendMessageInput;
import com.kingsrook.qqq.backend.core.model.actions.messaging.SendMessageOutput;
import com.kingsrook.qqq.backend.core.model.actions.messaging.SendMessageStatus;
import com.kingsrook.qqq.backend.core.model.metadata.messaging.QMessagingProviderMetaData;
import com.kingsrook.qqq.backend.core.modules.messaging.MessagingProviderCapability;
import com.kingsrook.qqq.backend.core.modules.messaging.MessagingProviderInterface;
import com.kingsrook.qqq.backend.core.modules.messaging.QMessagingProviderDispatcher;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit tests for provider validation, optional capabilities, and dispatch outcomes.
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



   /*******************************************************************************
    ** EMAIL refuses required features before trying to open an SMTP connection.
    *******************************************************************************/
   @Test
   void testCapabilityGating()
   {
      String name = TestUtils.EMAIL_MESSAGING_PROVIDER_NAME;
      assertThatThrownBy(() -> new SendMessageAction().execute(new SendMessageInput().withMessagingProviderName(name).withTemplateName("welcome")))
         .isInstanceOf(QException.class).hasMessage("Messaging provider " + name + " does not support templates");
      assertThatThrownBy(() -> new SendMessageAction().execute(new SendMessageInput().withMessagingProviderName(name).withIdempotencyKey("key")))
         .isInstanceOf(QException.class).hasMessage("Messaging provider " + name + " does not support idempotency");
   }



   /*******************************************************************************
    ** Rich input arrives intact and every explicit provider outcome survives dispatch.
    *******************************************************************************/
   @ParameterizedTest
   @EnumSource(SendMessageStatus.class)
   void testRichProvider(SendMessageStatus status) throws QException
   {
      registerTestProvider();
      SendMessageInput input = new SendMessageInput().withMessagingProviderName("richTest")
         .withTemplateName("welcome").withVariable("count", 3).withIdempotencyKey("welcome:42")
         .withCategory("SECURITY").withLinkedRecord("user", 42L).withSenderName("qbit-users")
         .withLocale("en-US").withSubject(status.name());
      EchoOutput output = (EchoOutput) new SendMessageAction().execute(input);
      assertSame(input, output.input);
      assertEquals("welcome", output.input.getTemplateName());
      assertEquals(3, output.input.getVariables().get("count"));
      assertEquals("welcome:42", output.input.getIdempotencyKey());
      assertEquals("SECURITY", output.input.getCategory());
      assertEquals("user", output.input.getLinkedTableName());
      assertEquals(42L, output.input.getLinkedRecordId());
      assertEquals("qbit-users", output.input.getSenderName());
      assertEquals("en-US", output.input.getLocale());
      assertEquals(status, output.getStatus());
      assertEquals(42L, output.getMessageId());
      assertEquals(List.of("provider explanation"), output.getReasons());
   }



   /*******************************************************************************
    ** Legacy provider outputs get SENT whether the output or only its status was null.
    *******************************************************************************/
   @Test
   void testLegacyStatusDefault() throws QException
   {
      registerTestProvider();
      SendMessageInput input = new SendMessageInput().withMessagingProviderName("richTest");
      SendMessageOutput output = new SendMessageAction().execute(input);
      assertEquals(SendMessageStatus.SENT, output.getStatus());
      assertEquals(42L, output.getMessageId());
      assertEquals(List.of("provider explanation"), output.getReasons());
      assertEquals(SendMessageStatus.SENT, new SendMessageAction().execute(input.withSubject("null-output")).getStatus());
   }



   /*******************************************************************************
    ** Lookup distinguishes registration from optional capability support.
    *******************************************************************************/
   @Test
   void testProviderLookup()
   {
      registerTestProvider();
      assertTrue(SendMessageAction.isAvailable(TestUtils.EMAIL_MESSAGING_PROVIDER_NAME));
      assertEquals(Set.of(MessagingProviderCapability.STREAMING_ATTACHMENTS), SendMessageAction.capabilities(TestUtils.EMAIL_MESSAGING_PROVIDER_NAME));
      assertTrue(SendMessageAction.isAvailable(TestUtils.SES_MESSAGING_PROVIDER_NAME));
      assertTrue(SendMessageAction.capabilities(TestUtils.SES_MESSAGING_PROVIDER_NAME).isEmpty());
      assertTrue(SendMessageAction.isAvailable("richTest"));
      assertEquals(Set.of(MessagingProviderCapability.TEMPLATES, MessagingProviderCapability.IDEMPOTENCY), SendMessageAction.capabilities("richTest"));
      QContext.getQInstance().addMessagingProvider(new QMessagingProviderMetaData().withName("unknownType").withType("UNREGISTERED_TEST_TYPE"));
      for(String name : new String[] { null, "", "missing", "unknownType" })
      {
         assertFalse(SendMessageAction.isAvailable(name));
         assertTrue(SendMessageAction.capabilities(name).isEmpty());
      }
   }



   /*******************************************************************************
    ** Register a distinct type without replacing any built-in provider registration.
    *******************************************************************************/
   private void registerTestProvider()
   {
      QMessagingProviderDispatcher.registerMessagingProvider(new RichTestProvider());
      QContext.getQInstance().addMessagingProvider(new QMessagingProviderMetaData().withName("richTest").withType("RICH_TEST"));
   }



   /*******************************************************************************
    ** Test-only output carries the actual dispatched input back to the caller.
    *******************************************************************************/
   private static class EchoOutput extends SendMessageOutput
   {
      private SendMessageInput input;
   }



   /*******************************************************************************
    ** Public constructor allows the dispatcher to instantiate this test provider.
    *******************************************************************************/
   public static class RichTestProvider implements MessagingProviderInterface
   {
      /*******************************************************************************
       ** Test type isolated from production registrations.
       *******************************************************************************/
      @Override
      public String getType()
      {
         return ("RICH_TEST");
      }



      /*******************************************************************************
       ** Declare both required rich-input features.
       *******************************************************************************/
      @Override
      public Set<MessagingProviderCapability> getCapabilities()
      {
         return (Set.of(MessagingProviderCapability.TEMPLATES, MessagingProviderCapability.IDEMPOTENCY));
      }



      /*******************************************************************************
       ** Echo the original input and a selected outcome without transport I/O.
       *******************************************************************************/
      @Override
      public SendMessageOutput sendMessage(SendMessageInput input)
      {
         if("null-output".equals(input.getSubject()))
         {
            return (null);
         }
         EchoOutput output = new EchoOutput();
         output.input = input;
         output.withMessageId(42L).withReasons(List.of("provider explanation"));
         if(input.getSubject() != null)
         {
            output.setStatus(SendMessageStatus.valueOf(input.getSubject()));
         }
         return (output);
      }
   }
}
