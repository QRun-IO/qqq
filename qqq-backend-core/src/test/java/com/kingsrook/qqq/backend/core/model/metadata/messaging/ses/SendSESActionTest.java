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

package com.kingsrook.qqq.backend.core.model.metadata.messaging.ses;


import cloud.localstack.ServiceName;
import cloud.localstack.docker.LocalstackDockerExtension;
import cloud.localstack.docker.annotation.LocalstackDockerProperties;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.VerifyEmailAddressRequest;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.messaging.Content;
import com.kingsrook.qqq.backend.core.model.actions.messaging.MultiParty;
import com.kingsrook.qqq.backend.core.model.actions.messaging.Party;
import com.kingsrook.qqq.backend.core.model.actions.messaging.SendMessageInput;
import com.kingsrook.qqq.backend.core.model.actions.messaging.email.EmailContentRole;
import com.kingsrook.qqq.backend.core.model.actions.messaging.email.EmailPartyRole;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for SendSESAction
 *******************************************************************************/
@ExtendWith(LocalstackDockerExtension.class)
@LocalstackDockerProperties(useSingleDockerContainer = true, services = { ServiceName.SES }, portEdge = "2960", portElasticSearch = "2961", imageTag = "1.4")
@DisabledIfEnvironmentVariable(named = "CIRCLECI", matches = "true")
class SendSESActionTest extends BaseTest
{
   public static final String TEST_TO_EMAIL_ADDRESS   = "tim-to@coldtrack.com";
   public static final String TEST_FROM_EMAIL_ADDRESS = "tim-from@coldtrack.com";



   /*******************************************************************************
    ** Before each unit test, get the test bucket into a known state
    *******************************************************************************/
   @BeforeEach
   public void beforeEach()
   {
      AmazonSimpleEmailService amazonSES = getAmazonSES();
      amazonSES.verifyEmailAddress(new VerifyEmailAddressRequest().withEmailAddress(TEST_TO_EMAIL_ADDRESS));
      amazonSES.verifyEmailAddress(new VerifyEmailAddressRequest().withEmailAddress(TEST_FROM_EMAIL_ADDRESS));
   }



   /*******************************************************************************
    ** Access a localstack-configured SES client.
    *******************************************************************************/
   protected AmazonSimpleEmailService getAmazonSES()
   {
      return (cloud.localstack.awssdkv1.TestUtils.getClientSES());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSendSES() throws QException
   {
      SendMessageInput sendMessageInput = new SendMessageInput()
         .withMessagingProviderName(TestUtils.SES_MESSAGING_PROVIDER_NAME)
         .withTo(new MultiParty()
            .withParty(new Party().withAddress(TEST_TO_EMAIL_ADDRESS).withLabel("Test TO").withRole(EmailPartyRole.TO))
         )
         .withFrom(new Party().withAddress(TEST_FROM_EMAIL_ADDRESS).withLabel("Test FROM"))
         .withSubject("This is another qqq test message.")
         .withContent(new Content().withContentRole(EmailContentRole.TEXT).withBody("This is a text body"))
         .withContent(new Content().withContentRole(EmailContentRole.HTML).withBody("This <u>is</u> an <b>HTML</b> body!"));

      SendSESAction sendSESAction = new SendSESAction();
      sendSESAction.setAmazonSES(getAmazonSES());
      sendSESAction.sendMessage(sendMessageInput);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetPartyListFromParty() throws QException
   {
      SendSESAction sendSESAction = new SendSESAction();
      assertEquals(0, sendSESAction.getPartyListFromParty(null).size());
      assertEquals(1, sendSESAction.getPartyListFromParty(new Party()).size());
      assertEquals(4, sendSESAction.getPartyListFromParty(
         new MultiParty()
            .withParty(new Party())
            .withParty(new Party())
            .withParty(new Party())
            .withParty(new Party())
      ).size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetSource() throws QException
   {
      /////////////////////////////////////////////////////////////
      // assert exception if no from given or one without a FROM //
      /////////////////////////////////////////////////////////////
      SendSESAction sendSESAction = new SendSESAction();
      assertThatThrownBy(() -> sendSESAction.getSource(new SendMessageInput())).isInstanceOf(QException.class).hasMessageContaining("not provided");
      assertThatThrownBy(() -> sendSESAction.getSource(new SendMessageInput().withFrom(new Party().withRole(EmailPartyRole.REPLY_TO)))).isInstanceOf(QException.class).hasMessageContaining("not provided");

      ////////////////////////////////////////////////////////////////////////////////////////////////////////
      // should only be one source, and should be the first one in multi party since multiple not supported //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////
      SendMessageInput multiPartyInput = new SendMessageInput().withFrom(new MultiParty()
         .withParty(new Party().withAddress("test1").withRole(EmailPartyRole.REPLY_TO))
         .withParty(new Party().withAddress("test2").withRole(EmailPartyRole.FROM))
         .withParty(new Party().withAddress("test3").withRole(EmailPartyRole.REPLY_TO))
         .withParty(new Party().withAddress("test4").withRole(EmailPartyRole.FROM))
      );
      assertEquals("test2", sendSESAction.getSource(multiPartyInput));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetReplyTos() throws QException
   {
      SendMessageInput multiPartyInput = new SendMessageInput()
         .withFrom(new MultiParty()
            .withParty(new Party().withAddress("test1").withRole(EmailPartyRole.REPLY_TO))
            .withParty(new Party().withAddress("test2").withRole(EmailPartyRole.FROM))
            .withParty(new Party().withAddress("test3").withRole(EmailPartyRole.REPLY_TO))
            .withParty(new Party().withAddress("test4").withRole(EmailPartyRole.FROM))
         );
      SendMessageInput singleInput = new SendMessageInput()
         .withFrom(new Party().withAddress("test1").withRole(EmailPartyRole.FROM));

      SendSESAction sendSESAction = new SendSESAction();
      assertEquals(2, sendSESAction.getReplyTos(multiPartyInput).size());
      assertEquals(0, sendSESAction.getReplyTos(singleInput).size());
      assertEquals(0, sendSESAction.getReplyTos(null).size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBuildDestination() throws QException
   {
      /////////////////////////////////////////
      // assert exception if no tos provided //
      /////////////////////////////////////////
      SendSESAction sendSESAction = new SendSESAction();
      assertThatThrownBy(() -> sendSESAction.buildDestination(new SendMessageInput())).isInstanceOf(QException.class).hasMessageContaining("were provided");
      assertThatThrownBy(() -> sendSESAction.buildDestination(new SendMessageInput().withTo(new Party().withAddress("test1").withRole(EmailPartyRole.CC)))).isInstanceOf(QException.class).hasMessageContaining("were provided");

      ///////////////////////////////////////////
      // exception if a FROM given in to field //
      ///////////////////////////////////////////
      assertThatThrownBy(() -> sendSESAction.buildDestination(new SendMessageInput().withTo(new Party().withAddress("test1").withRole(EmailPartyRole.FROM)))).isInstanceOf(QException.class).hasMessageContaining("unrecognized");

      SendMessageInput multiPartyInput = new SendMessageInput()
         .withTo(new MultiParty()
            .withParty(new Party().withAddress("test1").withRole(EmailPartyRole.CC))
            .withParty(new Party().withAddress("test2").withRole(EmailPartyRole.TO))
            .withParty(new Party().withAddress("test3").withRole(EmailPartyRole.BCC))
            .withParty(new Party().withAddress("test4").withRole(EmailPartyRole.BCC))
            .withParty(new Party().withAddress("test5").withRole(EmailPartyRole.TO))
            .withParty(new Party().withAddress("test6").withRole(EmailPartyRole.TO))
            .withParty(new Party().withAddress("test7").withRole(EmailPartyRole.TO))
         );
      Destination destination = sendSESAction.buildDestination(multiPartyInput);
      assertEquals(2, destination.getBccAddresses().size());
      assertEquals(4, destination.getToAddresses().size());
      assertEquals(1, destination.getCcAddresses().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBuildMessage() throws QException
   {
      /////////////////////////////////////////
      // assert exception if no tos provided //
      /////////////////////////////////////////
      SendSESAction sendSESAction = new SendSESAction();
      assertThatThrownBy(() -> sendSESAction.buildMessage(new SendMessageInput())).isInstanceOf(QException.class).hasMessageContaining("'Text' nor an 'HTML'");

      ///////////////////////////////////////////
      // exception if a FROM given in to field //
      ///////////////////////////////////////////
      assertThatThrownBy(() -> sendSESAction.buildDestination(new SendMessageInput().withTo(new Party().withAddress("test1").withRole(EmailPartyRole.FROM)))).isInstanceOf(QException.class).hasMessageContaining("unrecognized");

      String htmlContent = "HTML_CONTENT";
      String textContent = "TEXT_CONTENT";
      String subject     = "SUBJECT";
      SendMessageInput input = new SendMessageInput()
         .withContent(new Content().withContentRole(EmailContentRole.HTML).withBody(htmlContent))
         .withContent(new Content().withContentRole(EmailContentRole.TEXT).withBody(textContent))
         .withSubject(subject);
      Message message = sendSESAction.buildMessage(input);
      assertEquals(htmlContent, message.getBody().getHtml().getData());
      assertEquals(textContent, message.getBody().getText().getData());
      assertEquals(subject, message.getSubject().getData());
   }

}
