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

package com.kingsrook.qqq.middleware.javalin.specs.invoke;


import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import com.kingsrook.qqq.backend.core.model.session.QSession;


/*******************************************************************************
 ** JVM-local, in-memory session tokens accepted only from loopback requests.
 ** For components that authenticate users by other means, then mint sessions via
 ** QAuthenticationModuleInterface.createAutomatedSessionForUser. Registration and
 ** resolution copy the session so per-request mutations cannot affect other calls.
 *******************************************************************************/
public final class UiApiInProcessSessions
{
   public static final String HEADER = "X-QQQ-In-Process-Session";

   private static final SecureRandom RANDOM = new SecureRandom();
   private static final ConcurrentHashMap<String, Entry> SESSIONS = new ConcurrentHashMap<>();



   /*******************************************************************************
    ** Static API only.
    *******************************************************************************/
   private UiApiInProcessSessions()
   {
   }



   /*******************************************************************************
    ** A stored session snapshot and its expiration.
    *******************************************************************************/
   private record Entry(QSession session, Instant expiresAt)
   {
   }



   /*******************************************************************************
    ** Register a snapshot for a positive lifetime of at most 24 hours.
    *******************************************************************************/
   public static String register(QSession session, Duration ttl)
   {
      if(ttl == null || ttl.isNegative() || ttl.isZero() || ttl.compareTo(Duration.ofHours(24)) > 0)
      {
         throw (new IllegalArgumentException("Session TTL must be positive and at most 24 hours"));
      }
      QSession snapshot = copy(Objects.requireNonNull(session));
      Instant now = Instant.now();
      SESSIONS.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
      Entry entry = new Entry(snapshot, now.plus(ttl));
      String token;
      do
      {
         byte[] bytes = new byte[32];
         RANDOM.nextBytes(bytes);
         token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
      }
      while(SESSIONS.putIfAbsent(token, entry) != null);
      return (token);
   }



   /*******************************************************************************
    ** Resolve an unexpired token to a fresh request-local session copy.
    *******************************************************************************/
   public static Optional<QSession> resolve(String token)
   {
      Entry entry = token == null ? null : SESSIONS.get(token);
      if(entry == null)
      {
         return (Optional.empty());
      }
      if(!entry.expiresAt().isAfter(Instant.now()))
      {
         SESSIONS.remove(token, entry);
         return (Optional.empty());
      }
      return (Optional.of(copy(entry.session())));
   }



   /*******************************************************************************
    ** Revoke a token; unknown tokens are harmless.
    *******************************************************************************/
   public static void revoke(String token)
   {
      if(token != null)
      {
         SESSIONS.remove(token);
      }
   }



   /*******************************************************************************
    ** Number of stored tokens, including entries not yet purged after expiry.
    *******************************************************************************/
   public static int size()
   {
      return (SESSIONS.size());
   }



   /*******************************************************************************
    ** Clear the registry for test isolation.
    *******************************************************************************/
   static void clearForTests()
   {
      SESSIONS.clear();
   }



   /*******************************************************************************
    ** QSession.clone copies the user and every mutable session collection.
    *******************************************************************************/
   private static QSession copy(QSession session)
   {
      try
      {
         return (session.clone());
      }
      catch(CloneNotSupportedException e)
      {
         throw (new IllegalStateException("Unable to copy in-process session", e));
      }
   }
}
