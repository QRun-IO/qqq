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


import java.time.Duration;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.model.session.QSession;


/*******************************************************************************
 ** Interface for session storage providers.
 **
 ** QBits or application code can implement this interface to provide session
 ** persistence. Implementations register themselves with QSessionStoreRegistry
 ** on startup, and QQQ core uses the registered provider for session caching.
 **
 ** Example providers:
 ** - InMemory: ConcurrentHashMap for dev/testing
 ** - TableBased: QQQ table storage for multi-instance persistence
 ** - Redis: Distributed caching for HA deployments
 *******************************************************************************/
public interface QSessionStoreProviderInterface
{

   /***************************************************************************
    ** Store a session with the given TTL.
    **
    ** @param sessionUuid Unique identifier for the session
    ** @param session The session to store
    ** @param ttl Time-to-live for the session
    ***************************************************************************/
   void store(String sessionUuid, QSession session, Duration ttl);


   /***************************************************************************
    ** Load a session by UUID.
    **
    ** @param sessionUuid Unique identifier for the session
    ** @return Optional containing the session if found and not expired
    ***************************************************************************/
   Optional<QSession> load(String sessionUuid);


   /***************************************************************************
    ** Remove a session by UUID.
    **
    ** @param sessionUuid Unique identifier for the session to remove
    ***************************************************************************/
   void remove(String sessionUuid);


   /***************************************************************************
    ** Touch a session to reset its TTL (sliding expiration).
    **
    ** @param sessionUuid Unique identifier for the session to touch
    ***************************************************************************/
   void touch(String sessionUuid);


   /***************************************************************************
    ** Get the default TTL for sessions.
    **
    ** @return The default time-to-live duration
    ***************************************************************************/
   Duration getDefaultTtl();


   /***************************************************************************
    ** Load a session and touch it to reset its TTL in a single operation.
    **
    ** Default implementation calls load() then touch(). Providers may override
    ** with optimized implementations (e.g., Redis GETEX, combined SQL query).
    **
    ** @param sessionUuid Unique identifier for the session
    ** @return Optional containing the session if found and not expired
    ***************************************************************************/
   default Optional<QSession> loadAndTouch(String sessionUuid)
   {
      Optional<QSession> session = load(sessionUuid);
      session.ifPresent(s -> touch(sessionUuid));
      return session;
   }

}
