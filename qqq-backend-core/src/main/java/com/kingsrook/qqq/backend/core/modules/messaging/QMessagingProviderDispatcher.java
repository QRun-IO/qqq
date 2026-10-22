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

package com.kingsrook.qqq.backend.core.modules.messaging;


import java.util.HashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QModuleDispatchException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.messaging.QMessagingProviderMetaData;


/*******************************************************************************
 ** This class is responsible for loading a messaging provider, by its name, and
 ** returning an instance.
 **
 *******************************************************************************/
public class QMessagingProviderDispatcher
{
   private static final QLogger LOG = QLogger.getLogger(QMessagingProviderDispatcher.class);

   private static Map<String, String> typeToProviderClassNameMap;



   /*******************************************************************************
    **
    *******************************************************************************/
   public QMessagingProviderDispatcher()
   {
      initBackendTypeToModuleClassNameMap();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void initBackendTypeToModuleClassNameMap()
   {
      if(typeToProviderClassNameMap != null)
      {
         return;
      }

      Map<String, String> newMap = new HashMap<>();

      typeToProviderClassNameMap = newMap;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void registerMessagingProvider(MessagingProviderInterface messagingProviderInstance)
   {
      initBackendTypeToModuleClassNameMap();
      String type = messagingProviderInstance.getType();
      if(typeToProviderClassNameMap.containsKey(type))
      {
         LOG.info("Overwriting messagingProvider type [" + type + "] with [" + messagingProviderInstance.getClass() + "]");
      }
      typeToProviderClassNameMap.put(type, messagingProviderInstance.getClass().getName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public MessagingProviderInterface getMessagingProviderInterface(QMessagingProviderMetaData messagingProviderMetaData) throws QModuleDispatchException
   {
      return (getMessagingProviderInterface(messagingProviderMetaData.getType()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public MessagingProviderInterface getMessagingProviderInterface(String type) throws QModuleDispatchException
   {
      try
      {
         String className = typeToProviderClassNameMap.get(type);
         if(className == null)
         {
            throw (new QModuleDispatchException("Unrecognized messaging provider type [" + type + "] in dispatcher."));
         }

         Class<?> moduleClass = Class.forName(className);
         return (MessagingProviderInterface) moduleClass.getDeclaredConstructor().newInstance();
      }
      catch(QModuleDispatchException qmde)
      {
         throw (qmde);
      }
      catch(Exception e)
      {
         throw (new QModuleDispatchException("Error getting messaging provider of type: " + type, e));
      }
   }
}
