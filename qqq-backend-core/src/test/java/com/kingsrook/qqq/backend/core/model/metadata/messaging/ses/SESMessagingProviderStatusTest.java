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

package com.kingsrook.qqq.backend.core.model.metadata.messaging.ses;


import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.messaging.Content;
import com.kingsrook.qqq.backend.core.model.actions.messaging.Party;
import com.kingsrook.qqq.backend.core.model.actions.messaging.SendMessageInput;
import com.kingsrook.qqq.backend.core.model.actions.messaging.SendMessageStatus;
import com.kingsrook.qqq.backend.core.model.actions.messaging.email.EmailContentRole;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


/*******************************************************************************
 ** Verify synchronous SES output through the existing injectable client seam.
 *******************************************************************************/
class SESMessagingProviderStatusTest extends BaseTest
{
   /*******************************************************************************
    ** Successful transport submission returns SENT without contacting AWS.
    *******************************************************************************/
   @Test
   void testStatus() throws QException
   {
      AmazonSimpleEmailService client = mock(AmazonSimpleEmailService.class);
      SendSESAction action = new SendSESAction();
      action.setAmazonSES(client);
      SendMessageInput input = new SendMessageInput()
         .withTo(new Party().withAddress("to@example.com"))
         .withFrom(new Party().withAddress("from@example.com"))
         .withSubject("Test message")
         .withContent(new Content().withContentRole(EmailContentRole.TEXT).withBody("Test body"));
      assertEquals(SendMessageStatus.SENT, action.sendMessage(input).getStatus());
      verify(client).sendEmail(any(SendEmailRequest.class));
   }
}
