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


import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Helper class for optionally interacting with the QSessionStore QBit.
 **
 ** Uses reflection to avoid a hard dependency on the qbit-session-store module.
 ** All methods are designed to fail silently (returning empty/default values)
 ** when the QBit is not on the classpath, ensuring backwards compatibility.
 *******************************************************************************/
public class QSessionStoreHelper
{
   private static final QLogger LOG = QLogger.getLogger(QSessionStoreHelper.class);

   private static final String CONTEXT_CLASS = "com.kingsrook.qbits.sessionstore.QSessionStoreQBitContext";

   private static Boolean sessionStoreAvailable = null;



   /***************************************************************************
    ** Check if the session store QBit is available on the classpath.
    ***************************************************************************/
   public static boolean isSessionStoreAvailable()
   {
      if(sessionStoreAvailable == null)
      {
         try
         {
            Class.forName(CONTEXT_CLASS);
            sessionStoreAvailable = true;
         }
         catch(ClassNotFoundException e)
         {
            sessionStoreAvailable = false;
         }
      }
      return sessionStoreAvailable;
   }



   /***************************************************************************
    ** Store a session in the session store (if available and configured).
    ***************************************************************************/
   public static void storeSession(String sessionUuid, QSession session, Duration ttl)
   {
      if(!isSessionStoreAvailable())
      {
         return;
      }

      try
      {
         Object provider = getProvider();
         if(provider != null)
         {
            Method storeMethod = provider.getClass().getMethod("store", String.class, QSession.class, Duration.class);
            storeMethod.invoke(provider, sessionUuid, session, ttl);
            LOG.debug("Stored session in session store", logPair("sessionUuid", sessionUuid));
         }
      }
      catch(Exception e)
      {
         LOG.warn("Failed to store session in session store", e, logPair("sessionUuid", sessionUuid));
      }
   }



   /***************************************************************************
    ** Load a session from the session store (if available and configured).
    ***************************************************************************/
   @SuppressWarnings("unchecked")
   public static Optional<QSession> loadSession(String sessionUuid)
   {
      if(!isSessionStoreAvailable())
      {
         return Optional.empty();
      }

      try
      {
         Object provider = getProvider();
         if(provider != null)
         {
            Method loadMethod = provider.getClass().getMethod("load", String.class);
            Optional<QSession> result = (Optional<QSession>) loadMethod.invoke(provider, sessionUuid);
            if(result.isPresent())
            {
               LOG.debug("Loaded session from session store", logPair("sessionUuid", sessionUuid));
            }
            return result;
         }
      }
      catch(Exception e)
      {
         LOG.warn("Failed to load session from session store", e, logPair("sessionUuid", sessionUuid));
      }

      return Optional.empty();
   }



   /***************************************************************************
    ** Touch a session to reset its TTL (if available and configured).
    ***************************************************************************/
   public static void touchSession(String sessionUuid)
   {
      if(!isSessionStoreAvailable())
      {
         return;
      }

      try
      {
         Object provider = getProvider();
         if(provider != null)
         {
            Method touchMethod = provider.getClass().getMethod("touch", String.class);
            touchMethod.invoke(provider, sessionUuid);
         }
      }
      catch(Exception e)
      {
         LOG.warn("Failed to touch session in session store", e, logPair("sessionUuid", sessionUuid));
      }
   }



   /***************************************************************************
    ** Get the configured default TTL from the session store config.
    ***************************************************************************/
   public static Duration getDefaultTtl()
   {
      if(!isSessionStoreAvailable())
      {
         return Duration.ofHours(1);
      }

      try
      {
         Class<?> contextClass = Class.forName(CONTEXT_CLASS);
         Method getConfigMethod = contextClass.getMethod("getConfig");
         Object config = getConfigMethod.invoke(null);

         if(config != null)
         {
            Method getTtlMethod = config.getClass().getMethod("getDefaultTtl");
            return (Duration) getTtlMethod.invoke(config);
         }
      }
      catch(Exception e)
      {
         LOG.debug("Failed to get default TTL from session store config", e);
      }

      return Duration.ofHours(1);
   }



   /***************************************************************************
    ** Get the provider instance from the context class.
    ***************************************************************************/
   private static Object getProvider() throws Exception
   {
      Class<?> contextClass = Class.forName(CONTEXT_CLASS);
      Method getProviderMethod = contextClass.getMethod("getProvider");
      return getProviderMethod.invoke(null);
   }

}
