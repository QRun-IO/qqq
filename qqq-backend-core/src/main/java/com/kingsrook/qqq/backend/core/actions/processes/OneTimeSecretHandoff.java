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


import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.secrets.OneTimeSecret;
import com.kingsrook.qqq.backend.core.state.QStateProviders;
import com.kingsrook.qqq.backend.core.state.SimpleStateKey;
import com.kingsrook.qqq.backend.core.state.StateProviderInterface;
import com.kingsrook.qqq.backend.core.state.TableStateProvider;


/*******************************************************************************
 ** Hands one-time secrets from a process step that finished as an ASYNC job to
 ** the first status poll that sees it complete. The middleware runs a step as
 ** a job and answers "job started" when it takes longer than its step timeout
 ** (3 s by default), so without this a slow key-producing step would lose its
 ** secret.
 **
 ** Keyed by process UUID (status polls are already restricted to the process's
 ** own user), never written to process state, traces or listeners, removed on
 ** the first take, and dropped after a short time-to-live.
 **
 ** Where it is held: in this JVM's memory - and, when the instance shares state
 ** across nodes through an ENCRYPTED table store (StateProviderMetaData with
 ** QQQ_STATE_ENCRYPTION_KEY), also there, so the poll may reach any node. An
 ** unencrypted shared store is never used for secrets. A poll that finds
 ** nothing gets the documented "shown once, else rotate" outcome.
 *******************************************************************************/
public final class OneTimeSecretHandoff
{
   private static final QLogger LOG = QLogger.getLogger(OneTimeSecretHandoff.class);

   /** How long an untaken secret is kept. */
   public static final Duration TIME_TO_LIVE = Duration.ofMinutes(5);

   private static final String KEY_PREFIX = "oneTimeSecrets:";

   private static final Map<String, Entry> ENTRIES = new ConcurrentHashMap<>();



   /*******************************************************************************
    ** The held secrets and when they expire.
    *******************************************************************************/
   private record Entry(ArrayList<OneTimeSecret> secrets, Instant expires) implements Serializable
   {
   }



   /*******************************************************************************
    ** not instantiable
    *******************************************************************************/
   private OneTimeSecretHandoff()
   {
   }



   /*******************************************************************************
    ** Hold secrets for the process's next status poll.
    *******************************************************************************/
   public static void offer(String processUUID, List<OneTimeSecret> secrets)
   {
      evictExpired();
      if(processUUID == null || secrets == null || secrets.isEmpty())
      {
         return;
      }

      Entry entry = new Entry(new ArrayList<>(secrets), Instant.now().plus(TIME_TO_LIVE));
      ENTRIES.put(processUUID, entry);

      encryptedSharedStore().ifPresent(store ->
      {
         try
         {
            store.put(key(processUUID), entry);
         }
         catch(Exception e)
         {
            LOG.warn("Could not share one-time secrets across nodes; they stay on this node", e);
         }
      });
   }



   /*******************************************************************************
    ** Take (and remove) the process's held secrets - empty when none or expired.
    *******************************************************************************/
   public static List<OneTimeSecret> take(String processUUID)
   {
      evictExpired();
      if(processUUID == null)
      {
         return (List.of());
      }

      Entry entry = ENTRIES.remove(processUUID);

      Optional<StateProviderInterface> store = encryptedSharedStore();
      if(store.isPresent())
      {
         try
         {
            if(entry == null)
            {
               entry = store.get().get(Entry.class, key(processUUID)).orElse(null);
            }
            store.get().remove(key(processUUID));
         }
         catch(Exception e)
         {
            LOG.warn("Could not read one-time secrets from the shared store", e);
         }
      }

      if(entry == null || entry.expires().isBefore(Instant.now()))
      {
         return (List.of());
      }
      return (entry.secrets());
   }



   /*******************************************************************************
    ** The shared store, when the instance has one and it encrypts.
    *******************************************************************************/
   private static Optional<StateProviderInterface> encryptedSharedStore()
   {
      QInstance instance = QContext.getQInstance();
      if(instance == null || !QStateProviders.isShared(instance))
      {
         return (Optional.empty());
      }
      StateProviderInterface provider = QStateProviders.forInstance(instance);
      if(provider instanceof TableStateProvider table && table.isEncrypted())
      {
         return (Optional.of(provider));
      }
      return (Optional.empty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static SimpleStateKey<String> key(String processUUID)
   {
      return (new SimpleStateKey<>(KEY_PREFIX + processUUID));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void evictExpired()
   {
      Instant now = Instant.now();
      ENTRIES.entrySet().removeIf(e -> e.getValue().expires().isBefore(now));
   }
}
