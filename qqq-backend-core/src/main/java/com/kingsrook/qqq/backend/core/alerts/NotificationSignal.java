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

package com.kingsrook.qqq.backend.core.alerts;


import java.io.Serializable;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;


/*******************************************************************************
 ** One user notification from a producer, passed to QAlerts.notify: "tell
 ** these people this".  Unlike an AlertSignal it is not a problem to fix; it
 ** is a message for a person's bell (and, by their preference, email): a job
 ** that failed or finished, a share, a security notice, a release note.
 **
 ** title and audience are required.  Bounds: at most MAX_ATTRIBUTES attributes
 ** (extras are ignored).
 *******************************************************************************/
public class NotificationSignal implements Serializable
{
   public static final int MAX_ATTRIBUTES = 20;

   private String               title;
   private String               body;
   private NotificationSeverity severity;
   private NotificationCategory category;
   private String               sourceName;
   private String               sourceKey;
   private String               sourceTableName;
   private String               sourceRecordId;
   private AlertLink            link;
   private NotificationAudience audience;
   private Instant              publishAt;
   private Instant              expireAt;
   private Boolean              sticky;
   private Map<String, String>  attributes = new LinkedHashMap<>();
   private Instant              occurredAt;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public NotificationSignal()
   {
   }



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public NotificationSignal(String title)
   {
      this.title = title;
   }



   /*******************************************************************************
    ** Getter for title
    *******************************************************************************/
   public String getTitle()
   {
      return (this.title);
   }



   /*******************************************************************************
    ** Setter for title
    *******************************************************************************/
   public void setTitle(String title)
   {
      this.title = title;
   }



   /*******************************************************************************
    ** Fluent setter for title (the headline, plain text; required)
    *******************************************************************************/
   public NotificationSignal withTitle(String title)
   {
      this.title = title;
      return (this);
   }



   /*******************************************************************************
    ** Getter for body
    *******************************************************************************/
   public String getBody()
   {
      return (this.body);
   }



   /*******************************************************************************
    ** Setter for body
    *******************************************************************************/
   public void setBody(String body)
   {
      this.body = body;
   }



   /*******************************************************************************
    ** Fluent setter for body (the message, markdown (rendered safely, no raw HTML))
    *******************************************************************************/
   public NotificationSignal withBody(String body)
   {
      this.body = body;
      return (this);
   }



   /*******************************************************************************
    ** Getter for severity
    *******************************************************************************/
   public NotificationSeverity getSeverity()
   {
      return (this.severity);
   }



   /*******************************************************************************
    ** Setter for severity
    *******************************************************************************/
   public void setSeverity(NotificationSeverity severity)
   {
      this.severity = severity;
   }



   /*******************************************************************************
    ** Fluent setter for severity (default INFO)
    *******************************************************************************/
   public NotificationSignal withSeverity(NotificationSeverity severity)
   {
      this.severity = severity;
      return (this);
   }



   /*******************************************************************************
    ** Getter for category
    *******************************************************************************/
   public NotificationCategory getCategory()
   {
      return (this.category);
   }



   /*******************************************************************************
    ** Setter for category
    *******************************************************************************/
   public void setCategory(NotificationCategory category)
   {
      this.category = category;
   }



   /*******************************************************************************
    ** Fluent setter for category (default SYSTEM)
    *******************************************************************************/
   public NotificationSignal withCategory(NotificationCategory category)
   {
      this.category = category;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sourceName
    *******************************************************************************/
   public String getSourceName()
   {
      return (this.sourceName);
   }



   /*******************************************************************************
    ** Setter for sourceName
    *******************************************************************************/
   public void setSourceName(String sourceName)
   {
      this.sourceName = sourceName;
   }



   /*******************************************************************************
    ** Fluent setter for sourceName (who emitted it: a qbit or app name)
    *******************************************************************************/
   public NotificationSignal withSourceName(String sourceName)
   {
      this.sourceName = sourceName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sourceKey
    *******************************************************************************/
   public String getSourceKey()
   {
      return (this.sourceKey);
   }



   /*******************************************************************************
    ** Setter for sourceKey
    *******************************************************************************/
   public void setSourceKey(String sourceKey)
   {
      this.sourceKey = sourceKey;
   }



   /*******************************************************************************
    ** Fluent setter for sourceKey (dedup key: a second notify with the same key and audience updates the first)
    *******************************************************************************/
   public NotificationSignal withSourceKey(String sourceKey)
   {
      this.sourceKey = sourceKey;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sourceTableName
    *******************************************************************************/
   public String getSourceTableName()
   {
      return (this.sourceTableName);
   }



   /*******************************************************************************
    ** Setter for sourceTableName
    *******************************************************************************/
   public void setSourceTableName(String sourceTableName)
   {
      this.sourceTableName = sourceTableName;
   }



   /*******************************************************************************
    ** Fluent setter for sourceTableName (the table of the record it is about)
    *******************************************************************************/
   public NotificationSignal withSourceTableName(String sourceTableName)
   {
      this.sourceTableName = sourceTableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sourceRecordId
    *******************************************************************************/
   public String getSourceRecordId()
   {
      return (this.sourceRecordId);
   }



   /*******************************************************************************
    ** Setter for sourceRecordId
    *******************************************************************************/
   public void setSourceRecordId(String sourceRecordId)
   {
      this.sourceRecordId = sourceRecordId;
   }



   /*******************************************************************************
    ** Fluent setter for sourceRecordId (the id of the record it is about)
    *******************************************************************************/
   public NotificationSignal withSourceRecordId(String sourceRecordId)
   {
      this.sourceRecordId = sourceRecordId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for link
    *******************************************************************************/
   public AlertLink getLink()
   {
      return (this.link);
   }



   /*******************************************************************************
    ** Setter for link
    *******************************************************************************/
   public void setLink(AlertLink link)
   {
      this.link = link;
   }



   /*******************************************************************************
    ** Fluent setter for link (a deep link to where the user acts on it)
    *******************************************************************************/
   public NotificationSignal withLink(AlertLink link)
   {
      this.link = link;
      return (this);
   }



   /*******************************************************************************
    ** Getter for audience
    *******************************************************************************/
   public NotificationAudience getAudience()
   {
      return (this.audience);
   }



   /*******************************************************************************
    ** Setter for audience
    *******************************************************************************/
   public void setAudience(NotificationAudience audience)
   {
      this.audience = audience;
   }



   /*******************************************************************************
    ** Fluent setter for audience (who it is for; required)
    *******************************************************************************/
   public NotificationSignal withAudience(NotificationAudience audience)
   {
      this.audience = audience;
      return (this);
   }



   /*******************************************************************************
    ** Getter for publishAt
    *******************************************************************************/
   public Instant getPublishAt()
   {
      return (this.publishAt);
   }



   /*******************************************************************************
    ** Setter for publishAt
    *******************************************************************************/
   public void setPublishAt(Instant publishAt)
   {
      this.publishAt = publishAt;
   }



   /*******************************************************************************
    ** Fluent setter for publishAt (not shown before this time)
    *******************************************************************************/
   public NotificationSignal withPublishAt(Instant publishAt)
   {
      this.publishAt = publishAt;
      return (this);
   }



   /*******************************************************************************
    ** Getter for expireAt
    *******************************************************************************/
   public Instant getExpireAt()
   {
      return (this.expireAt);
   }



   /*******************************************************************************
    ** Setter for expireAt
    *******************************************************************************/
   public void setExpireAt(Instant expireAt)
   {
      this.expireAt = expireAt;
   }



   /*******************************************************************************
    ** Fluent setter for expireAt (hidden after this time)
    *******************************************************************************/
   public NotificationSignal withExpireAt(Instant expireAt)
   {
      this.expireAt = expireAt;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sticky
    *******************************************************************************/
   public Boolean getSticky()
   {
      return (this.sticky);
   }



   /*******************************************************************************
    ** Setter for sticky
    *******************************************************************************/
   public void setSticky(Boolean sticky)
   {
      this.sticky = sticky;
   }



   /*******************************************************************************
    ** Fluent setter for sticky (a banner until dismissed (meant for CRITICAL))
    *******************************************************************************/
   public NotificationSignal withSticky(Boolean sticky)
   {
      this.sticky = sticky;
      return (this);
   }



   /*******************************************************************************
    ** Getter for attributes
    *******************************************************************************/
   public Map<String, String> getAttributes()
   {
      return (this.attributes);
   }



   /*******************************************************************************
    ** Setter for attributes
    *******************************************************************************/
   public void setAttributes(Map<String, String> attributes)
   {
      this.attributes = new LinkedHashMap<>();
      if(attributes != null)
      {
         attributes.forEach(this::withAttribute);
      }
   }



   /*******************************************************************************
    ** Add a routing label (not displayed by default).  At most MAX_ATTRIBUTES
    ** keys; a new key beyond that is ignored (replacing an existing key is fine).
    *******************************************************************************/
   public NotificationSignal withAttribute(String key, String value)
   {
      if(key != null && (attributes.containsKey(key) || attributes.size() < MAX_ATTRIBUTES))
      {
         attributes.put(key, value);
      }
      return (this);
   }



   /*******************************************************************************
    ** Getter for occurredAt
    *******************************************************************************/
   public Instant getOccurredAt()
   {
      return (this.occurredAt);
   }



   /*******************************************************************************
    ** Setter for occurredAt
    *******************************************************************************/
   public void setOccurredAt(Instant occurredAt)
   {
      this.occurredAt = occurredAt;
   }



   /*******************************************************************************
    ** Fluent setter for occurredAt (default: when QAlerts.notify is called)
    *******************************************************************************/
   public NotificationSignal withOccurredAt(Instant occurredAt)
   {
      this.occurredAt = occurredAt;
      return (this);
   }
}
