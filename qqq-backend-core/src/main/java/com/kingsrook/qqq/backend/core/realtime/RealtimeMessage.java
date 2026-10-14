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

package com.kingsrook.qqq.backend.core.realtime;


import java.io.Serializable;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;


/*******************************************************************************
 ** A small invalidation hint. Record events carry identifiers and field names, never values.
 *******************************************************************************/
public class RealtimeMessage implements Serializable
{
   private String channel;
   private String event;
   private Map<String, Serializable> data = new LinkedHashMap<>();
   private String id = UUID.randomUUID().toString();
   private Instant at = Instant.now();




   /*******************************************************************************
    ** Getter for channel
    *******************************************************************************/
   public String getChannel()
   {
      return (channel);
   }




   /*******************************************************************************
    ** Setter for channel
    *******************************************************************************/
   public void setChannel(String channel)
   {
      this.channel = channel;
   }




   /*******************************************************************************
    ** Fluent setter for channel
    *******************************************************************************/
   public RealtimeMessage withChannel(String channel)
   {
      setChannel(channel);
      return (this);
   }




   /*******************************************************************************
    ** Getter for event
    *******************************************************************************/
   public String getEvent()
   {
      return (event);
   }




   /*******************************************************************************
    ** Setter for event
    *******************************************************************************/
   public void setEvent(String event)
   {
      this.event = event;
   }




   /*******************************************************************************
    ** Fluent setter for event
    *******************************************************************************/
   public RealtimeMessage withEvent(String event)
   {
      setEvent(event);
      return (this);
   }




   /*******************************************************************************
    ** Getter for data
    *******************************************************************************/
   public Map<String, Serializable> getData()
   {
      return (data);
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
   public RealtimeMessage withData(Map<String, Serializable> data)
   {
      setData(data);
      return (this);
   }




   /*******************************************************************************
    ** Getter for id
    *******************************************************************************/
   public String getId()
   {
      return (id);
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
   public RealtimeMessage withId(String id)
   {
      setId(id);
      return (this);
   }




   /*******************************************************************************
    ** Getter for at
    *******************************************************************************/
   public Instant getAt()
   {
      return (at);
   }




   /*******************************************************************************
    ** Setter for at
    *******************************************************************************/
   public void setAt(Instant at)
   {
      this.at = at;
   }




   /*******************************************************************************
    ** Fluent setter for at
    *******************************************************************************/
   public RealtimeMessage withAt(Instant at)
   {
      setAt(at);
      return (this);
   }
}

