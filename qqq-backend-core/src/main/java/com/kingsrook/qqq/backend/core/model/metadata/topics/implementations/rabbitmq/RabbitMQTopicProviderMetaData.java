/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.topics.implementations.rabbitmq;


import com.kingsrook.qqq.backend.core.model.metadata.topics.QTopicProviderMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.topics.TopicType;
import com.kingsrook.qqq.backend.core.modules.topics.QTopicProviderDispatcher;
import com.kingsrook.qqq.backend.core.modules.topics.implementations.rabbitmq.RabbitMQTopicProvider;


/*******************************************************************************
 * Implementation of a rabbitMQ topic-provider.
 *
 *******************************************************************************/
public class RabbitMQTopicProviderMetaData extends QTopicProviderMetaData
{
   private String host;
   private int    port;
   private String username;
   private String password;

   static
   {
      QTopicProviderDispatcher.registerTopicProvider(new RabbitMQTopicProvider());
   }

   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public RabbitMQTopicProviderMetaData()
   {
      super();
      setType(TopicType.RABBIT_MQ);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public RabbitMQTopicProviderMetaData withName(String name)
   {
      super.withName(name);
      return (this);
   }



   /*******************************************************************************
    * Getter for host
    * @see #withHost(String)
    *******************************************************************************/
   public String getHost()
   {
      return (this.host);
   }



   /*******************************************************************************
    * Setter for host
    * @see #withHost(String)
    *******************************************************************************/
   public void setHost(String host)
   {
      this.host = host;
   }



   /*******************************************************************************
    * Fluent setter for host
    *
    * @param host
    * TODO document this property
    *
    * @return this
    *******************************************************************************/
   public RabbitMQTopicProviderMetaData withHost(String host)
   {
      this.host = host;
      return (this);
   }



   /*******************************************************************************
    * Getter for port
    * @see #withPort(int)
    *******************************************************************************/
   public int getPort()
   {
      return (this.port);
   }



   /*******************************************************************************
    * Setter for port
    * @see #withPort(int)
    *******************************************************************************/
   public void setPort(int port)
   {
      this.port = port;
   }



   /*******************************************************************************
    * Fluent setter for port
    *
    * @param port
    * TODO document this property
    *
    * @return this
    *******************************************************************************/
   public RabbitMQTopicProviderMetaData withPort(int port)
   {
      this.port = port;
      return (this);
   }



   /*******************************************************************************
    * Getter for username
    * @see #withUsername(String)
    *******************************************************************************/
   public String getUsername()
   {
      return (this.username);
   }



   /*******************************************************************************
    * Setter for username
    * @see #withUsername(String)
    *******************************************************************************/
   public void setUsername(String username)
   {
      this.username = username;
   }



   /*******************************************************************************
    * Fluent setter for username
    *
    * @param username
    * TODO document this property
    *
    * @return this
    *******************************************************************************/
   public RabbitMQTopicProviderMetaData withUsername(String username)
   {
      this.username = username;
      return (this);
   }



   /*******************************************************************************
    * Getter for password
    * @see #withPassword(String)
    *******************************************************************************/
   public String getPassword()
   {
      return (this.password);
   }



   /*******************************************************************************
    * Setter for password
    * @see #withPassword(String)
    *******************************************************************************/
   public void setPassword(String password)
   {
      this.password = password;
   }



   /*******************************************************************************
    * Fluent setter for password
    *
    * @param password
    * TODO document this property
    *
    * @return this
    *******************************************************************************/
   public RabbitMQTopicProviderMetaData withPassword(String password)
   {
      this.password = password;
      return (this);
   }

}
