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

package com.kingsrook.qqq.backend.core.model.metadata.messaging.email;


import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.messaging.Content;
import com.kingsrook.qqq.backend.core.model.actions.messaging.MultiParty;
import com.kingsrook.qqq.backend.core.model.actions.messaging.Party;
import com.kingsrook.qqq.backend.core.model.actions.messaging.PartyRole;
import com.kingsrook.qqq.backend.core.model.actions.messaging.SendMessageInput;
import com.kingsrook.qqq.backend.core.model.actions.messaging.SendMessageOutput;
import com.kingsrook.qqq.backend.core.model.actions.messaging.email.EmailContentRole;
import com.kingsrook.qqq.backend.core.model.actions.messaging.email.EmailPartyRole;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;


/*******************************************************************************
 **
 *******************************************************************************/
public class SendEmailAction
{

   /*******************************************************************************
    **
    *******************************************************************************/
   public SendMessageOutput sendMessage(SendMessageInput sendMessageInput) throws QException
   {
      EmailMessagingProviderMetaData messagingProvider = (EmailMessagingProviderMetaData) QContext.getQInstance().getMessagingProvider(sendMessageInput.getMessagingProviderName());

      /////////////////////////////////////////
      // set up properties to make a session //
      /////////////////////////////////////////
      Properties properties = new Properties();
      properties.setProperty("mail.smtp.host", messagingProvider.getSmtpServer());
      properties.setProperty("mail.smtp.port", messagingProvider.getSmtpPort());
      Session session = Session.getInstance(properties);

      try
      {
         ////////////////////////////////////////////
         // Construct a default MimeMessage object //
         ////////////////////////////////////////////
         MimeMessage emailMessage = new MimeMessage(session);
         emailMessage.setSubject(sendMessageInput.getSubject());

         Party to = sendMessageInput.getTo();
         if(to instanceof MultiParty toMultiParty)
         {
            for(Party party : toMultiParty.getPartyList())
            {
               addRecipient(emailMessage, party);
            }
         }
         else
         {
            addRecipient(emailMessage, to);
         }

         Party from = sendMessageInput.getFrom();
         if(from instanceof MultiParty fromMultiParty)
         {
            for(Party party : fromMultiParty.getPartyList())
            {
               addSender(emailMessage, party);
            }
         }
         else
         {
            addSender(emailMessage, from);
         }

         Multipart multipart = new MimeMultipart();
         for(Content content : sendMessageInput.getContentList())
         {
            if(EmailContentRole.HTML.equals(content.getContentRole()))
            {
               MimeBodyPart mimeBodyPart = new MimeBodyPart();
               mimeBodyPart.setContent(content.getBody(), "text/html; charset=utf-8");
               multipart.addBodyPart(mimeBodyPart);
            }
            else if(EmailContentRole.TEXT.equals(content.getContentRole()))
            {
               MimeBodyPart mimeBodyPart = new MimeBodyPart();
               mimeBodyPart.setContent(content.getBody(), "text/plain; charset=utf-8");
               multipart.addBodyPart(mimeBodyPart);
            }
         }

         emailMessage.setContent(multipart);

         /////////////
         // send it //
         /////////////
         Transport.send(emailMessage);
         System.out.println("Message dispatched successfully...");
      }
      catch(Exception e)
      {
         throw (new QException("Error sending email", e));
      }

      return null;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void addSender(MimeMessage emailMessage, Party party) throws Exception
   {
      if(EmailPartyRole.REPLY_TO.equals(party.getRole()))
      {
         InternetAddress internetAddress = getInternetAddressFromParty(party);
         Address[]       replyTo         = emailMessage.getReplyTo();
         if(replyTo == null || replyTo.length == 0)
         {
            emailMessage.setReplyTo(new Address[] { internetAddress });
         }
         else
         {
            List<Address> replyToList = Arrays.asList(replyTo);
            emailMessage.setReplyTo(replyToList.toArray(new Address[0]));
         }
      }
      else if(party.getRole() == null || PartyRole.Default.DEFAULT.equals(party.getRole()) || EmailPartyRole.FROM.equals(party.getRole()))
      {
         emailMessage.setFrom(getInternetAddressFromParty(party));
      }
      else
      {
         throw (new QException("Unrecognized sender role [" + party.getRole() + "]"));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void addRecipient(MimeMessage emailMessage, Party party) throws Exception
   {
      Message.RecipientType recipientType;
      if(EmailPartyRole.CC.equals(party.getRole()))
      {
         recipientType = Message.RecipientType.CC;
      }
      else if(EmailPartyRole.BCC.equals(party.getRole()))
      {
         recipientType = Message.RecipientType.BCC;
      }
      else if(party.getRole() == null || PartyRole.Default.DEFAULT.equals(party.getRole()) || EmailPartyRole.TO.equals(party.getRole()))
      {
         recipientType = Message.RecipientType.TO;
      }
      else
      {
         throw (new QException("Unrecognized recipient role [" + party.getRole() + "]"));
      }

      InternetAddress internetAddress = getInternetAddressFromParty(party);
      emailMessage.addRecipient(recipientType, internetAddress);
      System.out.println("add recipient: [" + recipientType + "] => [" + internetAddress + "]");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static InternetAddress getInternetAddressFromParty(Party party) throws AddressException, UnsupportedEncodingException
   {
      InternetAddress internetAddress = new InternetAddress(party.getAddress());
      if(StringUtils.hasContent(party.getLabel()))
      {
         internetAddress.setPersonal(party.getLabel());
      }
      return internetAddress;
   }

}
