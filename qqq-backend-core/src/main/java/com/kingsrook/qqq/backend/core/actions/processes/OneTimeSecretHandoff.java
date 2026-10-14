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

package com.kingsrook.qqq.backend.core.actions.processes;


import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.kingsrook.qqq.backend.core.model.secrets.OneTimeSecret;


/*******************************************************************************
 ** Hands one-time secrets from a process step that finished as an ASYNC job to
 ** the first status poll that sees it complete. The middleware runs a step as
 ** a job and answers "job started" when it takes longer than its step timeout
 ** (3 s by default), so without this a slow key-producing step would lose its
 ** secret.
 **
 ** Held in this JVM's memory only, keyed by process UUID (status polls are
 ** already restricted to the process's own user), never written to process
 ** state, traces or listeners, removed on the first take, and dropped after
 ** a short time-to-live. A poll that reaches another server instance finds
 ** nothing, which is the documented "shown once, else rotate" outcome.
 *******************************************************************************/
public final class OneTimeSecretHandoff
{
   /** How long an untaken secret is kept. */
   public static final Duration TIME_TO_LIVE = Duration.ofMinutes(5);

   private static final Map<String, Entry> ENTRIES = new ConcurrentHashMap<>();



   /*******************************************************************************
    ** One parked set of secrets.
    *******************************************************************************/
   private record Entry(List<OneTimeSecret> secrets, Instant expires)
   {
   }



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private OneTimeSecretHandoff()
   {
   }



   /*******************************************************************************
    ** Park a finished run's secrets for its process UUID (no-op when none).
    *******************************************************************************/
   public static void offer(String processUUID, List<OneTimeSecret> secrets)
   {
      evictExpired();
      if(processUUID == null || secrets == null || secrets.isEmpty())
      {
         return;
      }
      ENTRIES.put(processUUID, new Entry(new ArrayList<>(secrets), Instant.now().plus(TIME_TO_LIVE)));
   }



   /*******************************************************************************
    ** Take (and forget) the secrets parked for a process UUID; empty when none,
    ** already taken, or expired.
    *******************************************************************************/
   public static List<OneTimeSecret> take(String processUUID)
   {
      evictExpired();
      if(processUUID == null)
      {
         return (List.of());
      }
      Entry entry = ENTRIES.remove(processUUID);
      if(entry == null || entry.expires().isBefore(Instant.now()))
      {
         return (List.of());
      }
      return (entry.secrets());
   }



   /*******************************************************************************
    ** Drop anything past its time-to-live.
    *******************************************************************************/
   private static void evictExpired()
   {
      Instant now = Instant.now();
      ENTRIES.entrySet().removeIf(e -> e.getValue().expires().isBefore(now));
   }
}
