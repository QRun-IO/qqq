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

package com.kingsrook.qqq.backend.core.modules.backend;


import java.util.HashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QModuleDispatchException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;


/*******************************************************************************
 ** This class is responsible for loading a backend module, by its name, and 
 ** returning an instance.
 **
 *******************************************************************************/
public class QBackendModuleDispatcher
{
   private static final QLogger LOG = QLogger.getLogger(QBackendModuleDispatcher.class);

   private static Map<String, String> backendTypeToModuleClassNameMap = new HashMap<>();



   /*******************************************************************************
    **
    *******************************************************************************/
   public QBackendModuleDispatcher()
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void registerBackendModule(QBackendModuleInterface moduleInstance)
   {
      String backendType = moduleInstance.getBackendType();
      if(backendTypeToModuleClassNameMap.containsKey(backendType))
      {
         LOG.info("Overwriting backend type [" + backendType + "] with [" + moduleInstance.getClass() + "]");
      }
      backendTypeToModuleClassNameMap.put(backendType, moduleInstance.getClass().getName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QBackendModuleInterface getQBackendModule(QBackendMetaData backend) throws QModuleDispatchException
   {
      return (getQBackendModule(backend.getBackendType()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QBackendModuleInterface getQBackendModule(String backendType) throws QModuleDispatchException
   {
      try
      {
         String className = backendTypeToModuleClassNameMap.get(backendType);
         if(className == null)
         {
            throw (new QModuleDispatchException("Unrecognized backend type [" + backendType + "] in dispatcher."));
         }

         Class<?> moduleClass = Class.forName(className);
         return (QBackendModuleInterface) moduleClass.getDeclaredConstructor().newInstance();
      }
      catch(QModuleDispatchException qmde)
      {
         throw (qmde);
      }
      catch(Exception e)
      {
         throw (new QModuleDispatchException("Error getting backend module of type: " + backendType, e));
      }
   }
}
