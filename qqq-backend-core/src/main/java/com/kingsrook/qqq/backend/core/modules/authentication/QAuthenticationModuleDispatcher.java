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

package com.kingsrook.qqq.backend.core.modules.authentication;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QModuleDispatchException;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.FullyAnonymousAuthenticationModule;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.MockAuthenticationModule;


/*******************************************************************************
 ** This class is responsible for loading an authentication module, by its name, and
 ** returning an instance.
 **
 ** TODO - make this mapping runtime-bound, not pre-compiled in.
 **
 *******************************************************************************/
public class QAuthenticationModuleDispatcher
{
   private static Map<String, String> authenticationTypeToModuleClassNameMap = Collections.synchronizedMap(new HashMap<>());

   static
   {
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // ensure our 2 default modules are registered.                                                             //
      // Note that for "real" implementations, the pattern is for their MetaData class's constructor to register. //
      // the idea being, any qInstance using such a module, surely will have some MetaData defined for it.        //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////
      registerModule(QAuthenticationType.MOCK.getName(), MockAuthenticationModule.class.getName());
      registerModule(QAuthenticationType.FULLY_ANONYMOUS.getName(), FullyAnonymousAuthenticationModule.class.getName());
   }

   /*******************************************************************************
    **
    *******************************************************************************/
   public QAuthenticationModuleDispatcher()
   {

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void registerModule(String name, String className)
   {
      authenticationTypeToModuleClassNameMap.putIfAbsent(name, className);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QAuthenticationModuleInterface getQModule(QAuthenticationMetaData authenticationMetaData) throws QModuleDispatchException
   {
      if(authenticationMetaData == null)
      {
         throw (new QModuleDispatchException("No authentication meta data defined."));
      }

      return getQModule(authenticationMetaData.getType().getName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QAuthenticationModuleInterface getQModule(String authenticationType) throws QModuleDispatchException
   {
      try
      {
         String className = authenticationTypeToModuleClassNameMap.get(authenticationType);
         if(className == null)
         {
            throw (new QModuleDispatchException("Unrecognized authentication type [" + authenticationType + "] in dispatcher."));
         }

         Class<?> moduleClass = Class.forName(className);
         return (QAuthenticationModuleInterface) moduleClass.getDeclaredConstructor().newInstance();
      }
      catch(QModuleDispatchException qmde)
      {
         throw (qmde);
      }
      catch(Exception e)
      {
         throw (new QModuleDispatchException("Error getting authentication module of type: " + authenticationType, e));
      }
   }

}
