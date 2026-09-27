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

package com.kingsrook.qqq.backend.core.model.actions.messaging;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;


/*******************************************************************************
 **
 *******************************************************************************/
public class SendMessageInput extends AbstractActionInput
{
   private String           messagingProviderName;
   private Party            to;
   private Party            from;
   private String           subject;
   private List<Content>    contentList;
   private List<Attachment> attachmentList;



   /*******************************************************************************
    ** Getter for to
    *******************************************************************************/
   public Party getTo()
   {
      return (this.to);
   }



   /*******************************************************************************
    ** Setter for to
    *******************************************************************************/
   public void setTo(Party to)
   {
      this.to = to;
   }



   /*******************************************************************************
    ** Fluent setter for to
    *******************************************************************************/
   public SendMessageInput withTo(Party to)
   {
      this.to = to;
      return (this);
   }



   /*******************************************************************************
    ** Getter for from
    *******************************************************************************/
   public Party getFrom()
   {
      return (this.from);
   }



   /*******************************************************************************
    ** Setter for from
    *******************************************************************************/
   public void setFrom(Party from)
   {
      this.from = from;
   }



   /*******************************************************************************
    ** Fluent setter for from
    *******************************************************************************/
   public SendMessageInput withFrom(Party from)
   {
      this.from = from;
      return (this);
   }



   /*******************************************************************************
    ** Getter for subject
    *******************************************************************************/
   public String getSubject()
   {
      return (this.subject);
   }



   /*******************************************************************************
    ** Setter for subject
    *******************************************************************************/
   public void setSubject(String subject)
   {
      this.subject = subject;
   }



   /*******************************************************************************
    ** Fluent setter for subject
    *******************************************************************************/
   public SendMessageInput withSubject(String subject)
   {
      this.subject = subject;
      return (this);
   }



   /*******************************************************************************
    ** Getter for contentList
    *******************************************************************************/
   public List<Content> getContentList()
   {
      return (this.contentList);
   }



   /*******************************************************************************
    ** Setter for contentList
    *******************************************************************************/
   public void setContentList(List<Content> contentList)
   {
      this.contentList = contentList;
   }



   /*******************************************************************************
    ** Fluent setter for contentList
    *******************************************************************************/
   public SendMessageInput withContentList(List<Content> contentList)
   {
      this.contentList = contentList;
      return (this);
   }



   /*******************************************************************************
    ** Getter for attachmentList
    *******************************************************************************/
   public List<Attachment> getAttachmentList()
   {
      return (this.attachmentList);
   }



   /*******************************************************************************
    ** Setter for attachmentList
    *******************************************************************************/
   public void setAttachmentList(List<Attachment> attachmentList)
   {
      this.attachmentList = attachmentList;
   }



   /*******************************************************************************
    ** Fluent setter for attachmentList
    *******************************************************************************/
   public SendMessageInput withAttachmentList(List<Attachment> attachmentList)
   {
      this.attachmentList = attachmentList;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public SendMessageInput withContent(Content content)
   {
      addContent(content);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void addContent(Content content)
   {
      if(this.contentList == null)
      {
         this.contentList = new ArrayList<>();
      }
      this.contentList.add(content);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public SendMessageInput withAttachment(Attachment attachment)
   {
      addAttachment(attachment);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void addAttachment(Attachment attachment)
   {
      if(this.attachmentList == null)
      {
         this.attachmentList = new ArrayList<>();
      }
      this.attachmentList.add(attachment);
   }



   /*******************************************************************************
    ** Getter for messagingProviderName
    *******************************************************************************/
   public String getMessagingProviderName()
   {
      return (this.messagingProviderName);
   }



   /*******************************************************************************
    ** Setter for messagingProviderName
    *******************************************************************************/
   public void setMessagingProviderName(String messagingProviderName)
   {
      this.messagingProviderName = messagingProviderName;
   }



   /*******************************************************************************
    ** Fluent setter for messagingProviderName
    *******************************************************************************/
   public SendMessageInput withMessagingProviderName(String messagingProviderName)
   {
      this.messagingProviderName = messagingProviderName;
      return (this);
   }

}
