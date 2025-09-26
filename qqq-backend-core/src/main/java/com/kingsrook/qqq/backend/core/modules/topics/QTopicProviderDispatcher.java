/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.modules.topics;


import java.util.HashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QModuleDispatchException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.topics.QTopicProviderMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.topics.TopicType;


/*******************************************************************************
 ** This class is responsible for loading a topic provider interface, by its name, and
 ** returning an instance.
 **
 *******************************************************************************/
public class QTopicProviderDispatcher
{
   private static final QLogger LOG = QLogger.getLogger(QTopicProviderDispatcher.class);

   private static Map<TopicType, String> typeToProviderClassNameMap;



   /*******************************************************************************
    **
    *******************************************************************************/
   public QTopicProviderDispatcher()
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

      Map<TopicType, String> newMap = new HashMap<>();

      typeToProviderClassNameMap = newMap;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void registerTopicProvider(TopicProviderInterface topicProviderInstance)
   {
      initBackendTypeToModuleClassNameMap();
      TopicType type = topicProviderInstance.getType();
      if(typeToProviderClassNameMap.containsKey(type))
      {
         LOG.info("Overwriting topicProvider type [" + type + "] with [" + topicProviderInstance.getClass() + "]");
      }
      typeToProviderClassNameMap.put(type, topicProviderInstance.getClass().getName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public TopicProviderInterface getTopicProviderInterface(QTopicProviderMetaData topicProviderMetaData) throws QModuleDispatchException
   {
      return (getTopicProviderInterface(topicProviderMetaData.getType()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public TopicProviderInterface getTopicProviderInterface(TopicType type) throws QModuleDispatchException
   {
      try
      {
         String className = typeToProviderClassNameMap.get(type);
         if(className == null)
         {
            throw (new QModuleDispatchException("Unrecognized topic provider type [" + type + "] in dispatcher."));
         }

         Class<?> moduleClass = Class.forName(className);
         return (TopicProviderInterface) moduleClass.getDeclaredConstructor().newInstance();
      }
      catch(QModuleDispatchException qmde)
      {
         throw (qmde);
      }
      catch(Exception e)
      {
         throw (new QModuleDispatchException("Error getting topic provider of type: " + type, e));
      }
   }

}
