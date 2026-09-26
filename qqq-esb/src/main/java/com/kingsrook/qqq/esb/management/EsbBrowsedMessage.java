/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.esb.management;


import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import com.kingsrook.qqq.esb.envelope.EsbEvent;


/*******************************************************************************
 * A message on a broker queue, as browsing shows it (the messages endpoints'
 * message shape):
 *
 * - messageId: the JMS message id (what deleteMessages and moveMessages take).
 * - timestamp: when it was sent (null if the sender turned timestamps off).
 * - deliveryCount: JMSXDeliveryCount (null if the broker didn't set it).
 * - event: the CloudEvent in its body, or null if it isn't one (e.g., a
 *   poison message).
 * - rawBody: the body as text, cut to EsbMessageBrowser.MAX_RAW_BODY_LENGTH
 *   characters; empty for a message with no text or bytes body.
 * - properties: the JMS properties (e.g., a dead letter's qqqError,
 *   qqqFailedTrigger, qqqAttempts, qqqFailedAt), with their JMS types.
 *******************************************************************************/
public class EsbBrowsedMessage implements Serializable
{
   private static final long serialVersionUID = 1L;

   private String                    messageId;
   private Instant                   timestamp;
   private Integer                   deliveryCount;
   private EsbEvent                  event;
   private String                    rawBody;
   private Map<String, Serializable> properties;



   /*******************************************************************************
    ** Getter for messageId
    *******************************************************************************/
   public String getMessageId()
   {
      return (this.messageId);
   }



   /*******************************************************************************
    ** Setter for messageId
    *******************************************************************************/
   public void setMessageId(String messageId)
   {
      this.messageId = messageId;
   }



   /*******************************************************************************
    ** Fluent setter for messageId
    *******************************************************************************/
   public EsbBrowsedMessage withMessageId(String messageId)
   {
      this.messageId = messageId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for timestamp
    *******************************************************************************/
   public Instant getTimestamp()
   {
      return (this.timestamp);
   }



   /*******************************************************************************
    ** Setter for timestamp
    *******************************************************************************/
   public void setTimestamp(Instant timestamp)
   {
      this.timestamp = timestamp;
   }



   /*******************************************************************************
    ** Fluent setter for timestamp
    *******************************************************************************/
   public EsbBrowsedMessage withTimestamp(Instant timestamp)
   {
      this.timestamp = timestamp;
      return (this);
   }



   /*******************************************************************************
    ** Getter for deliveryCount
    *******************************************************************************/
   public Integer getDeliveryCount()
   {
      return (this.deliveryCount);
   }



   /*******************************************************************************
    ** Setter for deliveryCount
    *******************************************************************************/
   public void setDeliveryCount(Integer deliveryCount)
   {
      this.deliveryCount = deliveryCount;
   }



   /*******************************************************************************
    ** Fluent setter for deliveryCount
    *******************************************************************************/
   public EsbBrowsedMessage withDeliveryCount(Integer deliveryCount)
   {
      this.deliveryCount = deliveryCount;
      return (this);
   }



   /*******************************************************************************
    ** Getter for event
    *******************************************************************************/
   public EsbEvent getEvent()
   {
      return (this.event);
   }



   /*******************************************************************************
    ** Setter for event
    *******************************************************************************/
   public void setEvent(EsbEvent event)
   {
      this.event = event;
   }



   /*******************************************************************************
    ** Fluent setter for event
    *******************************************************************************/
   public EsbBrowsedMessage withEvent(EsbEvent event)
   {
      this.event = event;
      return (this);
   }



   /*******************************************************************************
    ** Getter for rawBody
    *******************************************************************************/
   public String getRawBody()
   {
      return (this.rawBody);
   }



   /*******************************************************************************
    ** Setter for rawBody
    *******************************************************************************/
   public void setRawBody(String rawBody)
   {
      this.rawBody = rawBody;
   }



   /*******************************************************************************
    ** Fluent setter for rawBody
    *******************************************************************************/
   public EsbBrowsedMessage withRawBody(String rawBody)
   {
      this.rawBody = rawBody;
      return (this);
   }



   /*******************************************************************************
    ** Getter for properties
    *******************************************************************************/
   public Map<String, Serializable> getProperties()
   {
      return (this.properties);
   }



   /*******************************************************************************
    ** Setter for properties
    *******************************************************************************/
   public void setProperties(Map<String, Serializable> properties)
   {
      this.properties = properties;
   }



   /*******************************************************************************
    ** Fluent setter for properties
    *******************************************************************************/
   public EsbBrowsedMessage withProperties(Map<String, Serializable> properties)
   {
      this.properties = properties;
      return (this);
   }

}
