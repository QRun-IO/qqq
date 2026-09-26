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

package com.kingsrook.qqq.esb.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.TopLevelMetaDataInterface;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 * Meta-data for a message broker that ESB destinations live on.
 *
 * url is the Artemis URL (e.g., tcp://host:61616), or, for RabbitMQ, the AMQP
 * URI (including host, port, and vhost).  The optional management fields
 * enable queue depth, consumer counts, and queue management actions.
 *
 * url, username, password, and the management fields may be given as
 * ${env.X} (etc.) variables - they are interpreted when the instance is
 * enriched.  A variable that isn't set leaves its field null, and logs a
 * warning that names the provider and field (never a value).
 *
 * url can itself carry credentials (e.g., an AMQP URI with user:pass@), so
 * don't serialize these objects or echo url to users; build a view of just
 * the fields that are safe to show instead.
 *
 * A top-level meta-data object: adding it to an instance puts it in that
 * instance's EsbInstanceMetaData.
 *******************************************************************************/
public class QEsbProviderMetaData implements TopLevelMetaDataInterface
{
   private static final long serialVersionUID = 1L;

   private static final QLogger LOG = QLogger.getLogger(QEsbProviderMetaData.class);

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
      url = interpretField(interpreter, "url", url);
      username = interpretField(interpreter, "username", username);
      password = interpretField(interpreter, "password", password);
      managementUrl = interpretField(interpreter, "managementUrl", managementUrl);
      managementUsername = interpretField(interpreter, "managementUsername", managementUsername);
      managementPassword = interpretField(interpreter, "managementPassword", managementPassword);
   }



   /*******************************************************************************
    ** Interpret one connection field.  The interpreter gives null for a variable
    ** that isn't set - so warn about that, naming the field, but never logging
    ** its value (not even the variable reference, which could hold a literal
    ** fallback, as in ${env.X}??fallback).
    *******************************************************************************/
   private String interpretField(QMetaDataVariableInterpreter interpreter, String fieldName, String value)
   {
      String interpreted = interpreter.interpret(value);
      if(value != null && interpreted == null)
      {
         LOG.warn("ESB provider connection field references a variable that is not set", logPair("provider", name), logPair("field", fieldName));
      }
      return (interpreted);
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
