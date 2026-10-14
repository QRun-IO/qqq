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

package com.kingsrook.qqq.backend.core.actions.messaging;


import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.AbstractQActionFunction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QModuleDispatchException;
import com.kingsrook.qqq.backend.core.model.actions.messaging.SendMessageInput;
import com.kingsrook.qqq.backend.core.model.actions.messaging.SendMessageOutput;
import com.kingsrook.qqq.backend.core.model.actions.messaging.SendMessageStatus;
import com.kingsrook.qqq.backend.core.model.metadata.messaging.QMessagingProviderMetaData;
import com.kingsrook.qqq.backend.core.modules.messaging.MessagingProviderCapability;
import com.kingsrook.qqq.backend.core.modules.messaging.MessagingProviderInterface;
import com.kingsrook.qqq.backend.core.modules.messaging.QMessagingProviderDispatcher;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class SendMessageAction extends AbstractQActionFunction<SendMessageInput, SendMessageOutput>
{

   /*******************************************************************************
    ** Dispatch through the named provider. Templates and idempotency require declared
    ** capabilities. Category, linked record, sender name, locale, and variables
    ** without a template are advisory and unsupported providers silently ignore them.
    *******************************************************************************/
   @Override
   public SendMessageOutput execute(SendMessageInput input) throws QException
   {
      if(!StringUtils.hasContent(input.getMessagingProviderName()))
      {
         throw (new QException("Messaging provider name was not given in SendMessageInput."));
      }

      QMessagingProviderMetaData messagingProvider = QContext.getQInstance().getMessagingProvider(input.getMessagingProviderName());
      if(messagingProvider == null)
      {
         throw (new QException("Messaging provider named [" + input.getMessagingProviderName() + "] was not found in this QInstance."));
      }

      MessagingProviderInterface messagingProviderInterface = new QMessagingProviderDispatcher().getMessagingProviderInterface(messagingProvider.getType());

      Set<MessagingProviderCapability> capabilities = messagingProviderInterface.getCapabilities();
      if(input.getTemplateName() != null && !capabilities.contains(MessagingProviderCapability.TEMPLATES))
      {
         throw (new QException("Messaging provider " + input.getMessagingProviderName() + " does not support templates"));
      }
      if(input.getIdempotencyKey() != null && !capabilities.contains(MessagingProviderCapability.IDEMPOTENCY))
      {
         throw (new QException("Messaging provider " + input.getMessagingProviderName() + " does not support idempotency"));
      }

      SendMessageOutput output = messagingProviderInterface.sendMessage(input);
      if(output == null)
      {
         output = new SendMessageOutput();
      }
      if(output.getStatus() == null)
      {
         output.setStatus(SendMessageStatus.SENT);
      }
      return (output);
   }



   /*******************************************************************************
    ** Capabilities of a named provider in the current QInstance. Returns an empty
    ** set if metadata is absent or its type cannot resolve. Use isAvailable to
    ** distinguish unavailable providers from providers with no optional features.
    *******************************************************************************/
   public static Set<MessagingProviderCapability> capabilities(String messagingProviderName)
   {
      MessagingProviderInterface provider = findProvider(messagingProviderName);
      return (provider == null ? Set.of() : provider.getCapabilities());
   }



   /*******************************************************************************
    ** Whether provider metadata exists in the current QInstance and its type resolves.
    ** Does not check transport connectivity, credentials, or delivery readiness.
    *******************************************************************************/
   public static boolean isAvailable(String messagingProviderName)
   {
      return (findProvider(messagingProviderName) != null);
   }



   /*******************************************************************************
    ** Resolve an optional provider without treating missing registration as an error.
    *******************************************************************************/
   private static MessagingProviderInterface findProvider(String messagingProviderName)
   {
      if(!StringUtils.hasContent(messagingProviderName))
      {
         return (null);
      }
      QMessagingProviderMetaData metadata = QContext.getQInstance().getMessagingProvider(messagingProviderName);
      if(metadata == null)
      {
         return (null);
      }
      try
      {
         return (new QMessagingProviderDispatcher().getMessagingProviderInterface(metadata));
      }
      catch(QModuleDispatchException e)
      {
         return (null);
      }
   }

}
