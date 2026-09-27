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


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.messaging.MultiParty;
import com.kingsrook.qqq.backend.core.model.actions.messaging.Party;
import com.kingsrook.qqq.backend.core.model.actions.messaging.PartyRole;
import com.kingsrook.qqq.backend.core.model.actions.messaging.SendMessageInput;
import com.kingsrook.qqq.backend.core.model.actions.messaging.SendMessageOutput;
import com.kingsrook.qqq.backend.core.model.actions.messaging.email.EmailContentRole;
import com.kingsrook.qqq.backend.core.model.actions.messaging.email.EmailPartyRole;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class SendSESAction
{
   private static final QLogger LOG = QLogger.getLogger(SendSESAction.class);

   private AmazonSimpleEmailService amazonSES;



   /*******************************************************************************
    **
    *******************************************************************************/
   public SendMessageOutput sendMessage(SendMessageInput sendMessageInput) throws QException
   {
      try
      {
         AmazonSimpleEmailService client = getAmazonSES(sendMessageInput);

         ///////////////////////////////////
         // build up a send email request //
         ///////////////////////////////////
         SendEmailRequest request = new SendEmailRequest()
            .withSource(getSource(sendMessageInput))
            .withReplyToAddresses(getReplyTos(sendMessageInput))
            .withDestination(buildDestination(sendMessageInput))
            .withMessage(buildMessage(sendMessageInput));

         client.sendEmail(request);
         LOG.info("SES Message [" + request.getMessage().getSubject().getData() + "] was sent to [" + request.getDestination().toString() + "].");
      }
      catch(Exception e)
      {
         String message = "An unexpected error occurred sending an SES message.";
         throw (new QException(message, e));
      }

      return (null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   Message buildMessage(SendMessageInput input) throws QException
   {
      ///////////////////////////////////////////////////////////////////////////////////////////////
      // iterate over all contents of our input, looking for an HTML and Text version of the email //
      ///////////////////////////////////////////////////////////////////////////////////////////////
      Body body = new Body();
      for(com.kingsrook.qqq.backend.core.model.actions.messaging.Content content : CollectionUtils.nonNullList(input.getContentList()))
      {
         if(EmailContentRole.TEXT.equals(content.getContentRole()))
         {
            body.setText(new Content().withCharset("UTF-8").withData(content.getBody()));
         }
         else if(EmailContentRole.HTML.equals(content.getContentRole()))
         {
            body.setHtml(new Content().withCharset("UTF-8").withData(content.getBody()));
         }
      }

      ////////////////////////////////////////////////
      // error if no text or html body was provided //
      ////////////////////////////////////////////////
      if(body.getText() == null && body.getHtml() == null)
      {
         throw (new QException("Cannot send SES message because neither a 'Text' nor an 'HTML' body was provided."));
      }

      ////////////////////////////////////////
      // warning if no subject was provided //
      ////////////////////////////////////////
      Message message = new Message();
      message.setBody(body);

      /////////////////////////////////////
      // warn if no subject was provided //
      /////////////////////////////////////
      if(input.getSubject() == null)
      {
         LOG.warn("Sending SES message with no subject.");
      }
      else
      {
         message.setSubject(new Content().withCharset("UTF-8").withData(input.getSubject()));
      }

      return (message);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   List<String> getReplyTos(SendMessageInput input) throws QException
   {
      ////////////////////////////
      // no input, no reply tos //
      ////////////////////////////
      if(input == null)
      {
         return (Collections.emptyList());
      }

      ///////////////////////////////////////
      // build up a list of froms if multi //
      ///////////////////////////////////////
      List<Party> partyList = getPartyListFromParty(input.getFrom());
      if(partyList == null)
      {
         return (Collections.emptyList());
      }

      ///////////////////////////////
      // only get reply to parties //
      ///////////////////////////////
      List<Party> replyToParties = partyList.stream().filter(p -> EmailPartyRole.REPLY_TO.equals(p.getRole())).toList();

      //////////////////////////////////
      // get addresses from reply tos //
      //////////////////////////////////
      List<String> replyTos = replyToParties.stream().map(Party::getAddress).toList();

      /////////////////////////////
      // return the from address //
      /////////////////////////////
      return (replyTos);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   String getSource(SendMessageInput input) throws QException
   {
      ///////////////////////////////
      // error if no from provided //
      ///////////////////////////////
      if(input.getFrom() == null)
      {
         throw (new QException("Cannot send SES message because a FROM was not provided."));
      }

      ///////////////////////////////////////
      // build up a list of froms if multi //
      ///////////////////////////////////////
      List<Party> partyList = getPartyListFromParty(input.getFrom());

      ///////////////////////////////////////
      // remove any roles that aren't FROM //
      ///////////////////////////////////////
      partyList.removeIf(p -> p.getRole() != null && !EmailPartyRole.FROM.equals(p.getRole()));

      ///////////////////////////////////////////////////////////////////////////////////////////
      // if no froms found, error, if more than one found, log a warning and use the first one //
      ///////////////////////////////////////////////////////////////////////////////////////////
      if(partyList.isEmpty())
      {
         throw (new QException("Cannot send SES message because a FROM was not provided."));
      }
      else if(partyList.size() > 1)
      {
         LOG.warn("More than one FROM value was found, will send using the first one found [" + partyList.get(0).getAddress() + "].");
      }
      Party fromParty = partyList.get(0);
      if(fromParty.getAddress() == null)
      {
         throw (new QException("Cannot send SES message because a FROM address was not provided."));
      }

      /////////////////////////////
      // return the from address //
      /////////////////////////////
      return (getFullEmailAddress(fromParty));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   List<Party> getPartyListFromParty(Party party)
   {
      //////////////////////////////////////////////
      // get all parties into one list of parties //
      //////////////////////////////////////////////
      List<Party> partyList = new ArrayList<>();
      if(party != null)
      {
         if(party instanceof MultiParty toMultiParty)
         {
            partyList.addAll(toMultiParty.getPartyList());
         }
         else
         {
            partyList.add(party);
         }
      }
      return (partyList);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   Destination buildDestination(SendMessageInput input) throws QException
   {
      ////////////////////////////////////////////////////////////////////
      // iterate over the parties putting it the proper party type list //
      ////////////////////////////////////////////////////////////////////
      List<String> toList  = new ArrayList<>();
      List<String> ccList  = new ArrayList<>();
      List<String> bccList = new ArrayList<>();

      List<Party> partyList = getPartyListFromParty(input.getTo());
      for(Party party : partyList)
      {
         if(EmailPartyRole.CC.equals(party.getRole()))
         {
            ccList.add(getFullEmailAddress(party));
         }
         else if(EmailPartyRole.BCC.equals(party.getRole()))
         {
            bccList.add(getFullEmailAddress(party));
         }
         else if(party.getRole() == null || PartyRole.Default.DEFAULT.equals(party.getRole()) || EmailPartyRole.TO.equals(party.getRole()))
         {
            toList.add(getFullEmailAddress(party));
         }
         else
         {
            throw (new QException("An unrecognized recipient role of [" + party.getRole() + "] was provided."));
         }
      }

      //////////////////////////////////////////
      // if no to addresses, this is an error //
      //////////////////////////////////////////
      if(toList.isEmpty())
      {
         throw (new QException("Cannot send SES message because no TO addresses were provided."));
      }

      /////////////////////////////////////////////
      // build and return aws destination object //
      /////////////////////////////////////////////
      return (new Destination()
         .withToAddresses(toList)
         .withCcAddresses(ccList)
         .withBccAddresses(bccList));
   }



   /*******************************************************************************
    ** Set the amazonSES object.
    *******************************************************************************/
   public void setAmazonSES(AmazonSimpleEmailService amazonSES)
   {
      this.amazonSES = amazonSES;
   }



   /*******************************************************************************
    ** Internal accessor for the amazonSES object - should always use this, not the field.
    *******************************************************************************/
   protected AmazonSimpleEmailService getAmazonSES(SendMessageInput sendMessageInput)
   {
      if(amazonSES == null)
      {
         SESMessagingProviderMetaData messagingProvider = (SESMessagingProviderMetaData) QContext.getQInstance().getMessagingProvider(sendMessageInput.getMessagingProviderName());

         /////////////////////////////////////////////
         // get credentials and build an SES client //
         /////////////////////////////////////////////
         BasicAWSCredentials credentials = new BasicAWSCredentials(messagingProvider.getAccessKey(), messagingProvider.getSecretKey());
         amazonSES = AmazonSimpleEmailServiceClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(credentials))
            .withRegion(messagingProvider.getRegion()).build();
      }

      return amazonSES;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String getFullEmailAddress(Party party)
   {
      if(party.getLabel() != null)
      {
         return (party.getLabel() + " <" + party.getAddress() + ">");
      }

      /////////////////////////////
      // return the from address //
      /////////////////////////////
      return (party.getAddress());
   }
}
