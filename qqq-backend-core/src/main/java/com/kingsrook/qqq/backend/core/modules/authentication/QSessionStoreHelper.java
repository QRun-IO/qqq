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


import java.time.Duration;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Helper class for interacting with the session store.
 **
 ** Delegates to the provider registered with QSessionStoreRegistry. All methods
 ** are designed to fail silently (returning empty/default values) when no
 ** provider is registered, ensuring backwards compatibility.
 *******************************************************************************/
public class QSessionStoreHelper
{
   private static final QLogger LOG = QLogger.getLogger(QSessionStoreHelper.class);

   private static final Duration DEFAULT_TTL = Duration.ofHours(1);



   /***************************************************************************
    ** Check if a session store provider is available.
    ***************************************************************************/
   public static boolean isSessionStoreAvailable()
   {
      return QSessionStoreRegistry.getInstance().isAvailable();
   }



   /***************************************************************************
    ** Store a session in the session store (if available).
    ***************************************************************************/
   public static void storeSession(String sessionUuid, QSession session, Duration ttl)
   {
      Optional<QSessionStoreProviderInterface> provider = QSessionStoreRegistry.getInstance().getProvider();
      if(provider.isEmpty())
      {
         return;
      }

      try
      {
         provider.get().store(sessionUuid, session, ttl);
         LOG.debug("Stored session in session store", logPair("sessionUuid", sessionUuid));
      }
      catch(Exception e)
      {
         LOG.warn("Failed to store session in session store", e, logPair("sessionUuid", sessionUuid));
      }
   }



   /***************************************************************************
    ** Load a session from the session store (if available).
    ***************************************************************************/
   public static Optional<QSession> loadSession(String sessionUuid)
   {
      Optional<QSessionStoreProviderInterface> provider = QSessionStoreRegistry.getInstance().getProvider();
      if(provider.isEmpty())
      {
         return Optional.empty();
      }

      try
      {
         Optional<QSession> result = provider.get().load(sessionUuid);
         if(result.isPresent())
         {
            LOG.debug("Loaded session from session store", logPair("sessionUuid", sessionUuid));
         }
         return result;
      }
      catch(Exception e)
      {
         LOG.warn("Failed to load session from session store", e, logPair("sessionUuid", sessionUuid));
      }

      return Optional.empty();
   }



   /***************************************************************************
    ** Load a session and touch it to reset its TTL in a single operation.
    **
    ** This is more efficient than calling loadSession + touchSession separately,
    ** as providers may implement optimized single-call versions (e.g., Redis
    ** GETEX, combined SQL query).
    ***************************************************************************/
   public static Optional<QSession> loadAndTouchSession(String sessionUuid)
   {
      Optional<QSessionStoreProviderInterface> provider = QSessionStoreRegistry.getInstance().getProvider();
      if(provider.isEmpty())
      {
         return Optional.empty();
      }

      try
      {
         Optional<QSession> result = provider.get().loadAndTouch(sessionUuid);
         if(result.isPresent())
         {
            LOG.debug("Loaded and touched session from session store", logPair("sessionUuid", sessionUuid));
         }
         return result;
      }
      catch(Exception e)
      {
         LOG.warn("Failed to load and touch session in session store", e, logPair("sessionUuid", sessionUuid));
      }

      return Optional.empty();
   }



   /***************************************************************************
    ** Touch a session to reset its TTL (if available).
    ***************************************************************************/
   public static void touchSession(String sessionUuid)
   {
      Optional<QSessionStoreProviderInterface> provider = QSessionStoreRegistry.getInstance().getProvider();
      if(provider.isEmpty())
      {
         return;
      }

      try
      {
         provider.get().touch(sessionUuid);
      }
      catch(Exception e)
      {
         LOG.warn("Failed to touch session in session store", e, logPair("sessionUuid", sessionUuid));
      }
   }



   /***************************************************************************
    ** Get the configured default TTL from the session store provider.
    ***************************************************************************/
   public static Duration getDefaultTtl()
   {
      Optional<QSessionStoreProviderInterface> provider = QSessionStoreRegistry.getInstance().getProvider();
      if(provider.isEmpty())
      {
         return DEFAULT_TTL;
      }

      try
      {
         return provider.get().getDefaultTtl();
      }
      catch(Exception e)
      {
         LOG.debug("Failed to get default TTL from session store provider", e);
      }

      return DEFAULT_TTL;
   }

}
