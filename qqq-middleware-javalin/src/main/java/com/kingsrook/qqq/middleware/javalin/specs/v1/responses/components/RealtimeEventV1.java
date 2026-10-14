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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components;


import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;


/*******************************************************************************
 ** Typed realtime UI API payload. Qbits publish their own event names through the same bus.
 *******************************************************************************/
public class RealtimeEventV1 implements ToSchema
{
   private String event;
   private String channel;
   private Map<String, Serializable> data;



   /*******************************************************************************
    ** Explicit schema for clients in any language.
    *******************************************************************************/
   @Override
   public Schema toSchema()
   {
      Map<String, Schema> properties = new TreeMap<>();
      properties.put("event", new Schema().withType(Type.STRING));
      properties.put("channel", new Schema().withType(Type.STRING));
      properties.put("data", new Schema().withType(Type.OBJECT).withAdditionalProperties(true));
      return (new Schema().withType(Type.OBJECT).withProperties(properties));
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
   public RealtimeEventV1 withEvent(String event)
   {
      setEvent(event);
      return (this);
   }




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
   public RealtimeEventV1 withChannel(String channel)
   {
      setChannel(channel);
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
   public RealtimeEventV1 withData(Map<String, Serializable> data)
   {
      setData(data);
      return (this);
   }
}

