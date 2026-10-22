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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/*******************************************************************************
 ** One "this is a problem" signal from a producer, passed to QAlerts.raise.
 ** Raising is idempotent on (alertTypeName, dedupKey): repeats refresh one open
 ** alert, they never duplicate it.
 **
 ** Text fields have different reach.  title and summary are external-safe: a
 ** producer must write them without personal data, because they may be sent
 ** to email or chat.  details (and record ref labels) are internal: shown
 ** in-app only, to users who can see the alert.
 **
 ** Bounds: at most MAX_RECORD_REFS record refs (extras are dropped, and count
 ** keeps the true total) and MAX_ATTRIBUTES attributes (extras are ignored).
 *******************************************************************************/
public class AlertSignal implements Serializable
{
   public static final int MAX_RECORD_REFS = 100;
   public static final int MAX_ATTRIBUTES  = 20;

   private String               alertTypeName;
   private String               dedupKey;
   private AlertSeverity        severity;
   private String               title;
   private String               summary;
   private String               details;
   private List<AlertRecordRef> recordRefs = new ArrayList<>();
   private List<AlertLink>      links      = new ArrayList<>();
   private Map<String, String>  attributes = new LinkedHashMap<>();
   private Instant              occurredAt;
   private Integer              count;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public AlertSignal()
   {
   }



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public AlertSignal(String alertTypeName)
   {
      this.alertTypeName = alertTypeName;
   }



   /*******************************************************************************
    ** The dedup key to use: the one given, or the type name (one alert per type).
    *******************************************************************************/
   public String getEffectiveDedupKey()
   {
      return (dedupKey == null || dedupKey.isBlank() ? alertTypeName : dedupKey);
   }



   /*******************************************************************************
    ** The record count this signal describes: count when set, otherwise the
    ** number of record refs (null when there are none).
    *******************************************************************************/
   public Integer getEffectiveCount()
   {
      if(count != null)
      {
         return (count);
      }
      return (recordRefs.isEmpty() ? null : recordRefs.size());
   }



   /*******************************************************************************
    ** Getter for alertTypeName
    *******************************************************************************/
   public String getAlertTypeName()
   {
      return (this.alertTypeName);
   }



   /*******************************************************************************
    ** Setter for alertTypeName
    *******************************************************************************/
   public void setAlertTypeName(String alertTypeName)
   {
      this.alertTypeName = alertTypeName;
   }



   /*******************************************************************************
    ** Fluent setter for alertTypeName
    *******************************************************************************/
   public AlertSignal withAlertTypeName(String alertTypeName)
   {
      this.alertTypeName = alertTypeName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for dedupKey
    *******************************************************************************/
   public String getDedupKey()
   {
      return (this.dedupKey);
   }



   /*******************************************************************************
    ** Setter for dedupKey
    *******************************************************************************/
   public void setDedupKey(String dedupKey)
   {
      this.dedupKey = dedupKey;
   }



   /*******************************************************************************
    ** Fluent setter for dedupKey - a stable key, so repeats update one open alert.
    *******************************************************************************/
   public AlertSignal withDedupKey(String dedupKey)
   {
      this.dedupKey = dedupKey;
      return (this);
   }



   /*******************************************************************************
    ** Getter for severity
    *******************************************************************************/
   public AlertSeverity getSeverity()
   {
      return (this.severity);
   }



   /*******************************************************************************
    ** Setter for severity
    *******************************************************************************/
   public void setSeverity(AlertSeverity severity)
   {
      this.severity = severity;
   }



   /*******************************************************************************
    ** Fluent setter for severity (optional; defaults to the type's severity)
    *******************************************************************************/
   public AlertSignal withSeverity(AlertSeverity severity)
   {
      this.severity = severity;
      return (this);
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
    ** Fluent setter for title (external-safe: no personal data)
    *******************************************************************************/
   public AlertSignal withTitle(String title)
   {
      this.title = title;
      return (this);
   }



   /*******************************************************************************
    ** Getter for summary
    *******************************************************************************/
   public String getSummary()
   {
      return (this.summary);
   }



   /*******************************************************************************
    ** Setter for summary
    *******************************************************************************/
   public void setSummary(String summary)
   {
      this.summary = summary;
   }



   /*******************************************************************************
    ** Fluent setter for summary (external-safe: no personal data)
    *******************************************************************************/
   public AlertSignal withSummary(String summary)
   {
      this.summary = summary;
      return (this);
   }



   /*******************************************************************************
    ** Getter for details
    *******************************************************************************/
   public String getDetails()
   {
      return (this.details);
   }



   /*******************************************************************************
    ** Setter for details
    *******************************************************************************/
   public void setDetails(String details)
   {
      this.details = details;
   }



   /*******************************************************************************
    ** Fluent setter for details (internal: in-app only, never sent externally)
    *******************************************************************************/
   public AlertSignal withDetails(String details)
   {
      this.details = details;
      return (this);
   }



   /*******************************************************************************
    ** Getter for recordRefs
    *******************************************************************************/
   public List<AlertRecordRef> getRecordRefs()
   {
      return (this.recordRefs);
   }



   /*******************************************************************************
    ** Setter for recordRefs
    *******************************************************************************/
   public void setRecordRefs(List<AlertRecordRef> recordRefs)
   {
      this.recordRefs = new ArrayList<>();
      withRecordRefs(recordRefs);
   }



   /*******************************************************************************
    ** Add record refs.  Beyond MAX_RECORD_REFS they are dropped, and count is
    ** set to the true total so the alert still says how many there were.
    *******************************************************************************/
   public AlertSignal withRecordRefs(Collection<AlertRecordRef> recordRefs)
   {
      if(recordRefs == null)
      {
         return (this);
      }

      int total = this.recordRefs.size();
      for(AlertRecordRef recordRef : recordRefs)
      {
         if(recordRef == null)
         {
            continue;
         }
         total++;
         if(this.recordRefs.size() < MAX_RECORD_REFS)
         {
            this.recordRefs.add(recordRef);
         }
      }

      if(total > MAX_RECORD_REFS && (count == null || count < total))
      {
         count = total;
      }
      return (this);
   }



   /*******************************************************************************
    ** Add one record ref.
    *******************************************************************************/
   public AlertSignal withRecordRef(AlertRecordRef recordRef)
   {
      return (withRecordRefs(recordRef == null ? null : List.of(recordRef)));
   }



   /*******************************************************************************
    ** Getter for links
    *******************************************************************************/
   public List<AlertLink> getLinks()
   {
      return (this.links);
   }



   /*******************************************************************************
    ** Setter for links
    *******************************************************************************/
   public void setLinks(List<AlertLink> links)
   {
      this.links = links == null ? new ArrayList<>() : new ArrayList<>(links);
   }



   /*******************************************************************************
    ** Add a link to where the problem is fixed.
    *******************************************************************************/
   public AlertSignal withLink(AlertLink link)
   {
      if(link != null)
      {
         this.links.add(link);
      }
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
   public AlertSignal withAttribute(String key, String value)
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
    ** Fluent setter for occurredAt (default: when QAlerts.raise is called)
    *******************************************************************************/
   public AlertSignal withOccurredAt(Instant occurredAt)
   {
      this.occurredAt = occurredAt;
      return (this);
   }



   /*******************************************************************************
    ** Getter for count
    *******************************************************************************/
   public Integer getCount()
   {
      return (this.count);
   }



   /*******************************************************************************
    ** Setter for count
    *******************************************************************************/
   public void setCount(Integer count)
   {
      this.count = count;
   }



   /*******************************************************************************
    ** Fluent setter for count - for aggregate alerts, e.g. "37 records".
    *******************************************************************************/
   public AlertSignal withCount(Integer count)
   {
      this.count = count;
      return (this);
   }
}
