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

package com.kingsrook.qqq.esb.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.TopLevelMetaDataInterface;


/*******************************************************************************
 * Meta-data for a message broker that ESB destinations live on.
 *
 * url is the Artemis URL (e.g., tcp://host:61616), or, for RabbitMQ, the AMQP
 * URI (including host, port, and vhost).  The optional management fields
 * enable queue depth, consumer counts, and queue management actions.
 *
 * url, username, password, and the management fields may be given as
 * ${env.X} (etc.) variables - they are interpreted when the instance is
 * enriched.
 *
 * A top-level meta-data object: adding it to an instance puts it in that
 * instance's EsbInstanceMetaData.
 *******************************************************************************/
public class QEsbProviderMetaData implements TopLevelMetaDataInterface
{
   private String          name;
   private EsbProviderType type;

   private String url;
   private String username;
   private String password;

   private String managementUrl;
   private String managementUsername;
   private String managementPassword;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void addSelfToInstance(QInstance qInstance)
   {
      EsbInstanceMetaData.of(qInstance).withProvider(this);
   }



   /*******************************************************************************
    ** Interpret variables (e.g., ${env.BROKER_PASSWORD}) in the connection fields.
    *******************************************************************************/
   public void interpretVariables(QMetaDataVariableInterpreter interpreter)
   {
      url = interpreter.interpret(url);
      username = interpreter.interpret(username);
      password = interpreter.interpret(password);
      managementUrl = interpreter.interpret(managementUrl);
      managementUsername = interpreter.interpret(managementUsername);
      managementPassword = interpreter.interpret(managementPassword);
   }



   /*******************************************************************************
    ** Getter for name
    *******************************************************************************/
   @Override
   public String getName()
   {
      return (this.name);
   }



   /*******************************************************************************
    ** Setter for name
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Fluent setter for name
    *******************************************************************************/
   public QEsbProviderMetaData withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for type
    *******************************************************************************/
   public EsbProviderType getType()
   {
      return (this.type);
   }



   /*******************************************************************************
    ** Setter for type
    *******************************************************************************/
   public void setType(EsbProviderType type)
   {
      this.type = type;
   }



   /*******************************************************************************
    ** Fluent setter for type
    *******************************************************************************/
   public QEsbProviderMetaData withType(EsbProviderType type)
   {
      this.type = type;
      return (this);
   }



   /*******************************************************************************
    ** Getter for url
    *******************************************************************************/
   public String getUrl()
   {
      return (this.url);
   }



   /*******************************************************************************
    ** Setter for url
    *******************************************************************************/
   public void setUrl(String url)
   {
      this.url = url;
   }



   /*******************************************************************************
    ** Fluent setter for url
    *******************************************************************************/
   public QEsbProviderMetaData withUrl(String url)
   {
      this.url = url;
      return (this);
   }



   /*******************************************************************************
    ** Getter for username
    *******************************************************************************/
   public String getUsername()
   {
      return (this.username);
   }



   /*******************************************************************************
    ** Setter for username
    *******************************************************************************/
   public void setUsername(String username)
   {
      this.username = username;
   }



   /*******************************************************************************
    ** Fluent setter for username
    *******************************************************************************/
   public QEsbProviderMetaData withUsername(String username)
   {
      this.username = username;
      return (this);
   }



   /*******************************************************************************
    ** Getter for password - never serialized.
    *******************************************************************************/
   @JsonIgnore
   public String getPassword()
   {
      return (this.password);
   }



   /*******************************************************************************
    ** Setter for password
    *******************************************************************************/
   public void setPassword(String password)
   {
      this.password = password;
   }



   /*******************************************************************************
    ** Fluent setter for password
    *******************************************************************************/
   public QEsbProviderMetaData withPassword(String password)
   {
      this.password = password;
      return (this);
   }



   /*******************************************************************************
    ** Getter for managementUrl
    *******************************************************************************/
   public String getManagementUrl()
   {
      return (this.managementUrl);
   }



   /*******************************************************************************
    ** Setter for managementUrl
    *******************************************************************************/
   public void setManagementUrl(String managementUrl)
   {
      this.managementUrl = managementUrl;
   }



   /*******************************************************************************
    ** Fluent setter for managementUrl
    *******************************************************************************/
   public QEsbProviderMetaData withManagementUrl(String managementUrl)
   {
      this.managementUrl = managementUrl;
      return (this);
   }



   /*******************************************************************************
    ** Getter for managementUsername
    *******************************************************************************/
   public String getManagementUsername()
   {
      return (this.managementUsername);
   }



   /*******************************************************************************
    ** Setter for managementUsername
    *******************************************************************************/
   public void setManagementUsername(String managementUsername)
   {
      this.managementUsername = managementUsername;
   }



   /*******************************************************************************
    ** Fluent setter for managementUsername
    *******************************************************************************/
   public QEsbProviderMetaData withManagementUsername(String managementUsername)
   {
      this.managementUsername = managementUsername;
      return (this);
   }



   /*******************************************************************************
    ** Getter for managementPassword - never serialized.
    *******************************************************************************/
   @JsonIgnore
   public String getManagementPassword()
   {
      return (this.managementPassword);
   }



   /*******************************************************************************
    ** Setter for managementPassword
    *******************************************************************************/
   public void setManagementPassword(String managementPassword)
   {
      this.managementPassword = managementPassword;
   }



   /*******************************************************************************
    ** Fluent setter for managementPassword
    *******************************************************************************/
   public QEsbProviderMetaData withManagementPassword(String managementPassword)
   {
      this.managementPassword = managementPassword;
      return (this);
   }

}
