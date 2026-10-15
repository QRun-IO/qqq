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

package com.kingsrook.qqq.backend.core.cluster;


import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.modules.authentication.QSessionStoreHelper;
import com.kingsrook.qqq.backend.core.utils.memoization.Memoization;


/*******************************************************************************
 ** The registry of in-memory caches (static Memoizations and the like). When a
 ** cache goes stale on one node, it is cleared on every node: ClusterBus event
 ** memo-invalidate, and for session-keyed caches also session-ended.
 **
 ** Every static cache registers, inline where it is declared:
 ** <pre>
 ** private static final Memoization&lt;Integer, Script&gt; SCRIPTS = ClusterCaches.shared("scripts.byId", new Memoization&lt;&gt;(), Integer::valueOf);
 ** private static final Memoization&lt;String, Constructor&lt;?&gt;&gt; CTORS = ClusterCaches.nodeLocal(new Memoization&lt;&gt;(), "classes are the same on every node");
 ** </pre>
 ** ClusterCacheAudit, used by tests, fails on a static Memoization that did not
 ** register.
 *******************************************************************************/
public final class ClusterCaches
{
   private static final Map<String, Entry> SHARED       = new ConcurrentHashMap<>();
   private static final Set<Object>        REGISTERED   = Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));
   private static final Set<String>        SESSION_KEYS = ConcurrentHashMap.newKeySet();



   /*******************************************************************************
    ** A shared cache's clear actions.
    **
    ** @param clearAll clear every entry
    ** @param clearKey clear the entry for a key (as text)
    *******************************************************************************/
   private record Entry(Runnable clearAll, Consumer<String> clearKey)
   {
   }



   static
   {
      ClusterBus.on(ClusterEvent.TYPE_MEMO_INVALIDATE, (instance, event) -> clearLocal(event.getName(), event.getKey()));
      ClusterBus.on(ClusterEvent.TYPE_SESSION_ENDED, (instance, event) ->
      {
         for(String name : SESSION_KEYS)
         {
            clearLocal(name, event.getKey());
         }
         QSessionStoreHelper.removeSession(event.getKey());
      });
   }



   /*******************************************************************************
    ** not instantiable
    *******************************************************************************/
   private ClusterCaches()
   {
   }



   /*******************************************************************************
    ** Register a cache that must be cleared on every node when it is
    ** invalidated. keyParser turns an event's key text back into the cache's key
    ** type (null: a key invalidation clears everything). Returns the memoization.
    *******************************************************************************/
   public static <K, V> Memoization<K, V> shared(String name, Memoization<K, V> memoization, Function<String, K> keyParser)
   {
      SHARED.put(name, new Entry(memoization::clear, keyText ->
      {
         if(keyParser == null || keyText == null)
         {
            memoization.clear();
         }
         else
         {
            memoization.clearKey(keyParser.apply(keyText));
         }
      }));
      REGISTERED.add(memoization);
      return (memoization);
   }



   /*******************************************************************************
    ** Register a shared cache keyed by session UUID: also cleared (for that
    ** session) when a session ends on any node.
    *******************************************************************************/
   public static <V> Memoization<String, V> sessionKeyed(String name, Memoization<String, V> memoization)
   {
      shared(name, memoization, Function.identity());
      SESSION_KEYS.add(name);
      return (memoization);
   }



   /*******************************************************************************
    ** Register any other in-memory cache by its clear actions (clearKey may be
    ** null: then a key invalidation clears everything).
    *******************************************************************************/
   public static <T> T shared(String name, T cache, Runnable clearAll, Consumer<String> clearKey)
   {
      SHARED.put(name, new Entry(clearAll, clearKey == null ? key -> clearAll.run() : clearKey));
      REGISTERED.add(cache);
      return (cache);
   }



   /*******************************************************************************
    ** Declare a cache that is correct per node: it holds what is the same on
    ** every node (classes, code, provider metadata). The reason documents why.
    *******************************************************************************/
   public static <T> T nodeLocal(T cache, String reason)
   {
      REGISTERED.add(cache);
      return (cache);
   }



   /*******************************************************************************
    ** Whether a cache object was registered (shared or node-local).
    *******************************************************************************/
   public static boolean isRegistered(Object cache)
   {
      return (REGISTERED.contains(cache));
   }



   /*******************************************************************************
    ** Clear a cache on every node.
    *******************************************************************************/
   public static void invalidate(String name)
   {
      invalidateKey(name, null);
   }



   /*******************************************************************************
    ** Clear one key of a cache on every node (null key: all of it).
    *******************************************************************************/
   public static void invalidateKey(String name, String key)
   {
      ClusterBus.publish(QContext.getQInstance(), new ClusterEvent(ClusterEvent.TYPE_MEMO_INVALIDATE, name, key));
   }



   /*******************************************************************************
    ** A session ended: clear session-keyed caches on every node.
    *******************************************************************************/
   public static void sessionEnded(String sessionUUID)
   {
      ClusterBus.publish(QContext.getQInstance(), new ClusterEvent(ClusterEvent.TYPE_SESSION_ENDED, null, sessionUUID));
   }



   /*******************************************************************************
    ** Clear on this node only.
    *******************************************************************************/
   static void clearLocal(String name, String key)
   {
      Entry entry = name == null ? null : SHARED.get(name);
      if(entry == null)
      {
         return;
      }
      if(key == null)
      {
         entry.clearAll().run();
      }
      else
      {
         entry.clearKey().accept(key);
      }
   }
}
