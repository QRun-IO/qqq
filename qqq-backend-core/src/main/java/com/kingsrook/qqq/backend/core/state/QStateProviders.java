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

package com.kingsrook.qqq.backend.core.state;


import java.util.Map;
import java.util.WeakHashMap;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QRuntimeException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;


/*******************************************************************************
 ** The state provider for process and async job state: the instance's
 ** StateProviderMetaData (a provider class, or the built-in TableStateProvider
 ** over its table), else this node's memory, as before.
 *******************************************************************************/
public final class QStateProviders
{
   private static final Map<QInstance, StateProviderInterface> PROVIDERS = new WeakHashMap<>();



   /*******************************************************************************
    ** not instantiable
    *******************************************************************************/
   private QStateProviders()
   {
   }



   /*******************************************************************************
    ** The provider for the current context's instance.
    *******************************************************************************/
   public static StateProviderInterface get()
   {
      return (forInstance(QContext.getQInstance()));
   }



   /*******************************************************************************
    ** The provider for an instance (in-memory without one, or without meta data).
    *******************************************************************************/
   public static StateProviderInterface forInstance(QInstance instance)
   {
      StateProviderMetaData metaData = StateProviderMetaData.of(instance);
      if(metaData == null || !metaData.isShared())
      {
         return (InMemoryStateProvider.getInstance());
      }

      synchronized(PROVIDERS)
      {
         return (PROVIDERS.computeIfAbsent(instance, key -> build(key, metaData)));
      }
   }



   /*******************************************************************************
    ** Whether the instance's state is shared across nodes.
    *******************************************************************************/
   public static boolean isShared(QInstance instance)
   {
      StateProviderMetaData metaData = StateProviderMetaData.of(instance);
      return (metaData != null && metaData.isShared());
   }



   /*******************************************************************************
    ** Build a provider.
    *******************************************************************************/
   private static StateProviderInterface build(QInstance instance, StateProviderMetaData metaData)
   {
      try
      {
         if(metaData.getProviderCode() != null)
         {
            return (QCodeLoader.getAdHoc(StateProviderInterface.class, metaData.getProviderCode()));
         }
         TableStateProvider provider = new TableStateProvider(instance, metaData);
         provider.startSweeper();
         return (provider);
      }
      catch(Exception e)
      {
         throw (new QRuntimeException("Could not build the state provider", e));
      }
   }



   /*******************************************************************************
    ** Forget built providers (tests).
    *******************************************************************************/
   public static void clearForTests()
   {
      synchronized(PROVIDERS)
      {
         PROVIDERS.values().forEach(p ->
         {
            if(p instanceof TableStateProvider table)
            {
               table.stopSweeper();
            }
         });
         PROVIDERS.clear();
      }
   }
}
