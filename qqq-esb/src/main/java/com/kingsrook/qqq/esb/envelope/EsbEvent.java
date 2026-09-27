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

package com.kingsrook.qqq.esb.envelope;


import java.io.Serializable;
import java.time.Instant;
import java.util.Map;


/*******************************************************************************
 * An ESB message: a CloudEvents 1.0 event (spec section 4).
 *
 * - id: unique per event (a UUID for events QQQ makes).
 * - source: qqq://instance/table/name or qqq://instance/process/name.
 * - type: qqq.table.name.inserted|updated|deleted,
 *   qqq.process.name.started|completed|failed, or caller-supplied.
 * - subject: the record's primary key (table events only).
 * - causationId: the id of the event whose triggered run made this one (the
 *   qqqcausationid extension attribute), or null.
 * - time: when the event was made.
 * - data: the event's JSON object data.
 *
 * EsbEventFactory makes events; EsbEventCodec reads and writes them as JSON and
 * JMS messages (adding specversion and datacontenttype).
 *******************************************************************************/
public class EsbEvent implements Serializable
{
   private static final long serialVersionUID = 1L;

   private String                    id;
   private String                    source;
   private String                    type;
   private String                    subject;
   private String                    causationId;
   private Instant                   time;
   private Map<String, Serializable> data;



   /*******************************************************************************
    ** Getter for id
    *******************************************************************************/
   public String getId()
   {
      return (this.id);
   }



   /*******************************************************************************
    ** Setter for id
    *******************************************************************************/
   public void setId(String id)
   {
      this.id = id;
   }



   /*******************************************************************************
    ** Fluent setter for id
    *******************************************************************************/
   public EsbEvent withId(String id)
   {
      this.id = id;
      return (this);
   }



   /*******************************************************************************
    ** Getter for source
    *******************************************************************************/
   public String getSource()
   {
      return (this.source);
   }



   /*******************************************************************************
    ** Setter for source
    *******************************************************************************/
   public void setSource(String source)
   {
      this.source = source;
   }



   /*******************************************************************************
    ** Fluent setter for source
    *******************************************************************************/
   public EsbEvent withSource(String source)
   {
      this.source = source;
      return (this);
   }



   /*******************************************************************************
    ** Getter for type
    *******************************************************************************/
   public String getType()
   {
      return (this.type);
   }



   /*******************************************************************************
    ** Setter for type
    *******************************************************************************/
   public void setType(String type)
   {
      this.type = type;
   }



   /*******************************************************************************
    ** Fluent setter for type
    *******************************************************************************/
   public EsbEvent withType(String type)
   {
      this.type = type;
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
   public EsbEvent withSubject(String subject)
   {
      this.subject = subject;
      return (this);
   }



   /*******************************************************************************
    ** Getter for causationId
    *******************************************************************************/
   public String getCausationId()
   {
      return (this.causationId);
   }



   /*******************************************************************************
    ** Setter for causationId
    *******************************************************************************/
   public void setCausationId(String causationId)
   {
      this.causationId = causationId;
   }



   /*******************************************************************************
    ** Fluent setter for causationId
    *******************************************************************************/
   public EsbEvent withCausationId(String causationId)
   {
      this.causationId = causationId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for time
    *******************************************************************************/
   public Instant getTime()
   {
      return (this.time);
   }



   /*******************************************************************************
    ** Setter for time
    *******************************************************************************/
   public void setTime(Instant time)
   {
      this.time = time;
   }



   /*******************************************************************************
    ** Fluent setter for time
    *******************************************************************************/
   public EsbEvent withTime(Instant time)
   {
      this.time = time;
      return (this);
   }



   /*******************************************************************************
    ** Getter for data
    *******************************************************************************/
   public Map<String, Serializable> getData()
   {
      return (this.data);
   }



   /*******************************************************************************
    ** Setter for data
    *******************************************************************************/
   public void setData(Map<String, Serializable> data)
   {
      this.data = data;
   }



   /*******************************************************************************
    ** Fluent setter for data
    *******************************************************************************/
   public EsbEvent withData(Map<String, Serializable> data)
   {
      this.data = data;
      return (this);
   }

}
