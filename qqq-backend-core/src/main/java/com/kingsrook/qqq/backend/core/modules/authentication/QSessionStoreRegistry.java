/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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


import java.util.Optional;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Registry for session store providers.
 **
 ** QBits or application code implement QSessionStoreProviderInterface and
 ** register their implementation with this registry on startup. QQQ core
 ** uses the registered provider for session caching without needing to know
 ** about the specific implementation.
 **
 ** This follows the same pattern as SpaNotFoundHandlerRegistry - core defines
 ** the interface and registry, implementations register themselves.
 **
 ** Usage:
 ** - QBit/App: QSessionStoreRegistry.getInstance().register(myProvider);
 ** - Core: QSessionStoreRegistry.getInstance().getProvider().ifPresent(p -> p.store(...));
 *******************************************************************************/
public class QSessionStoreRegistry
{
   private static final QLogger LOG = QLogger.getLogger(QSessionStoreRegistry.class);

   private static final QSessionStoreRegistry INSTANCE = new QSessionStoreRegistry();

   private QSessionStoreProviderInterface provider = null;



   /***************************************************************************
    ** Private constructor for singleton
    ***************************************************************************/
   private QSessionStoreRegistry()
   {
   }



   /***************************************************************************
    ** Get the singleton instance of the registry.
    **
    ** @return The singleton QSessionStoreRegistry instance
    ***************************************************************************/
   public static QSessionStoreRegistry getInstance()
   {
      return INSTANCE;
   }



   /***************************************************************************
    ** Register a session store provider.
    **
    ** Called by QBits or application code on startup to register their
    ** session store implementation.
    **
    ** @param provider The provider implementation to register
    ***************************************************************************/
   public synchronized void register(QSessionStoreProviderInterface provider)
   {
      if(this.provider != null)
      {
         LOG.warn("Replacing existing session store provider",
            logPair("oldProvider", this.provider.getClass().getName()),
            logPair("newProvider", provider.getClass().getName()));
      }

      this.provider = provider;
      LOG.info("Registered session store provider", logPair("provider", provider.getClass().getName()));
   }



   /***************************************************************************
    ** Check if a session store provider is registered.
    **
    ** @return true if a provider is registered, false otherwise
    ***************************************************************************/
   public boolean isAvailable()
   {
      return provider != null;
   }



   /***************************************************************************
    ** Get the registered session store provider.
    **
    ** @return Optional containing the provider if registered
    ***************************************************************************/
   public Optional<QSessionStoreProviderInterface> getProvider()
   {
      return Optional.ofNullable(provider);
   }



   /***************************************************************************
    ** Clear the registered provider (useful for testing).
    ***************************************************************************/
   public synchronized void clear()
   {
      provider = null;
      LOG.debug("Cleared session store provider");
   }

}
