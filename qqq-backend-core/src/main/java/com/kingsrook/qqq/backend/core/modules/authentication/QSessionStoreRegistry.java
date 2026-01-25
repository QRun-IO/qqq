/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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
