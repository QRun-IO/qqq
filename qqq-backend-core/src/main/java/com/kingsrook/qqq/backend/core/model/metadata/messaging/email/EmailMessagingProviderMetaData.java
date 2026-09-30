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

package com.kingsrook.qqq.backend.core.model.metadata.messaging.email;


import com.kingsrook.qqq.backend.core.model.metadata.messaging.QMessagingProviderMetaData;
import com.kingsrook.qqq.backend.core.modules.messaging.QMessagingProviderDispatcher;


/*******************************************************************************
 **
 *******************************************************************************/
public class EmailMessagingProviderMetaData extends QMessagingProviderMetaData
{
   private String smtpServer;
   private String smtpPort;

   public static final String TYPE = "EMAIL";

   static
   {
      QMessagingProviderDispatcher.registerMessagingProvider(new EmailMessagingProvider());
   }

   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public EmailMessagingProviderMetaData()
   {
      super();
      setType(TYPE);
   }



   /*******************************************************************************
    ** Getter for smtpServer
    *******************************************************************************/
   public String getSmtpServer()
   {
      return (this.smtpServer);
   }



   /*******************************************************************************
    ** Setter for smtpServer
    *******************************************************************************/
   public void setSmtpServer(String smtpServer)
   {
      this.smtpServer = smtpServer;
   }



   /*******************************************************************************
    ** Fluent setter for smtpServer
    *******************************************************************************/
   public EmailMessagingProviderMetaData withSmtpServer(String smtpServer)
   {
      this.smtpServer = smtpServer;
      return (this);
   }



   /*******************************************************************************
    ** Getter for smtpPort
    *******************************************************************************/
   public String getSmtpPort()
   {
      return (this.smtpPort);
   }



   /*******************************************************************************
    ** Setter for smtpPort
    *******************************************************************************/
   public void setSmtpPort(String smtpPort)
   {
      this.smtpPort = smtpPort;
   }



   /*******************************************************************************
    ** Fluent setter for smtpPort
    *******************************************************************************/
   public EmailMessagingProviderMetaData withSmtpPort(String smtpPort)
   {
      this.smtpPort = smtpPort;
      return (this);
   }

}
