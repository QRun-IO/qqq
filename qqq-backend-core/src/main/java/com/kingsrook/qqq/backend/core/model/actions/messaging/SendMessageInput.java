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


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;


/*******************************************************************************
 ** Provider-neutral message input. All rich-message fields are optional.
 ** Category, linked record, sender name, locale, and variables without a template
 ** are advisory: providers that do not support them silently ignore them.
 ** Recipients and senders remain Party values; use MultiParty for multiple parties.
 *******************************************************************************/
public class SendMessageInput extends AbstractActionInput
{
   private String           messagingProviderName;
   private Party            to;
   private Party            from;
   private String           subject;
   private List<Content>     contentList;
   private List<Attachment> attachmentList;

   private String                    templateName;
   private Map<String, Serializable> variables;
   private String                    idempotencyKey;
   private String                    category;
   private String                    linkedTableName;
   private Serializable              linkedRecordId;
   private String                    senderName;
   private String                    locale;



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



   /*******************************************************************************
    ** Getter for templateName. Provider template name. Requires the TEMPLATES capability.
    *******************************************************************************/
   public String getTemplateName()
   {
      return (this.templateName);
   }



   /*******************************************************************************
    ** Setter for templateName
    *******************************************************************************/
   public void setTemplateName(String templateName)
   {
      this.templateName = templateName;
   }



   /*******************************************************************************
    ** Fluent setter for templateName
    *******************************************************************************/
   public SendMessageInput withTemplateName(String templateName)
   {
      this.templateName = templateName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for variables. Template variables; advisory when no template is selected.
    *******************************************************************************/
   public Map<String, Serializable> getVariables()
   {
      return (this.variables);
   }



   /*******************************************************************************
    ** Setter for variables
    *******************************************************************************/
   public void setVariables(Map<String, Serializable> variables)
   {
      this.variables = variables;
   }



   /*******************************************************************************
    ** Fluent setter for variables
    *******************************************************************************/
   public SendMessageInput withVariables(Map<String, Serializable> variables)
   {
      this.variables = variables;
      return (this);
   }



   /*******************************************************************************
    ** Getter for idempotencyKey. Provider deduplication key. Requires the IDEMPOTENCY capability.
    *******************************************************************************/
   public String getIdempotencyKey()
   {
      return (this.idempotencyKey);
   }



   /*******************************************************************************
    ** Setter for idempotencyKey
    *******************************************************************************/
   public void setIdempotencyKey(String idempotencyKey)
   {
      this.idempotencyKey = idempotencyKey;
   }



   /*******************************************************************************
    ** Fluent setter for idempotencyKey
    *******************************************************************************/
   public SendMessageInput withIdempotencyKey(String idempotencyKey)
   {
      this.idempotencyKey = idempotencyKey;
      return (this);
   }



   /*******************************************************************************
    ** Getter for category. Conventional values: TRANSACTIONAL, NOTIFICATION, MARKETING, SECURITY.
    ** Providers may refuse unknown categories.
    *******************************************************************************/
   public String getCategory()
   {
      return (this.category);
   }



   /*******************************************************************************
    ** Setter for category
    *******************************************************************************/
   public void setCategory(String category)
   {
      this.category = category;
   }



   /*******************************************************************************
    ** Fluent setter for category
    *******************************************************************************/
   public SendMessageInput withCategory(String category)
   {
      this.category = category;
      return (this);
   }



   /*******************************************************************************
    ** Getter for linkedTableName. Advisory name of the table associated with this message.
    *******************************************************************************/
   public String getLinkedTableName()
   {
      return (this.linkedTableName);
   }



   /*******************************************************************************
    ** Setter for linkedTableName
    *******************************************************************************/
   public void setLinkedTableName(String linkedTableName)
   {
      this.linkedTableName = linkedTableName;
   }



   /*******************************************************************************
    ** Fluent setter for linkedTableName
    *******************************************************************************/
   public SendMessageInput withLinkedTableName(String linkedTableName)
   {
      this.linkedTableName = linkedTableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for linkedRecordId. Advisory identifier of the record associated with this message.
    *******************************************************************************/
   public Serializable getLinkedRecordId()
   {
      return (this.linkedRecordId);
   }



   /*******************************************************************************
    ** Setter for linkedRecordId
    *******************************************************************************/
   public void setLinkedRecordId(Serializable linkedRecordId)
   {
      this.linkedRecordId = linkedRecordId;
   }



   /*******************************************************************************
    ** Fluent setter for linkedRecordId
    *******************************************************************************/
   public SendMessageInput withLinkedRecordId(Serializable linkedRecordId)
   {
      this.linkedRecordId = linkedRecordId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for senderName. Calling qbit or process name for audit; distinct from the from party.
    *******************************************************************************/
   public String getSenderName()
   {
      return (this.senderName);
   }



   /*******************************************************************************
    ** Setter for senderName
    *******************************************************************************/
   public void setSenderName(String senderName)
   {
      this.senderName = senderName;
   }



   /*******************************************************************************
    ** Fluent setter for senderName
    *******************************************************************************/
   public SendMessageInput withSenderName(String senderName)
   {
      this.senderName = senderName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for locale. Advisory locale for provider rendering.
    *******************************************************************************/
   public String getLocale()
   {
      return (this.locale);
   }



   /*******************************************************************************
    ** Setter for locale
    *******************************************************************************/
   public void setLocale(String locale)
   {
      this.locale = locale;
   }



   /*******************************************************************************
    ** Fluent setter for locale
    *******************************************************************************/
   public SendMessageInput withLocale(String locale)
   {
      this.locale = locale;
      return (this);
   }



   /*******************************************************************************
    ** Add or replace one variable, initializing the map when needed.
    *******************************************************************************/
   public SendMessageInput withVariable(String name, Serializable value)
   {
      if(this.variables == null)
      {
         this.variables = new HashMap<>();
      }
      this.variables.put(name, value);
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for the associated table and record identifier.
    *******************************************************************************/
   public SendMessageInput withLinkedRecord(String table, Serializable id)
   {
      this.linkedTableName = table;
      this.linkedRecordId = id;
      return (this);
   }

}
