/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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


import java.io.Serializable;
import java.time.Instant;
import java.util.Optional;


/*******************************************************************************
 ** QQQ state provider interface.  Provides standard interface for various
 ** implementations of how to store & retrieve user/process state data, like
 ** sessions, or process data.  Not like permanent record data - that is done in
 ** Backend modules.
 **
 ** Different implementations may be:  in-memory (non-persistent!!), or on-disk
 ** (with the tradeoffs that has), in-database, in-cache-system, etc.
 **
 ** Things which probably haven't been thought about here include:
 ** - multi-layering.  e.g., always have an in-memory layer on top of a more
 **   persistent backend, but then how to avoid staleness in-memory?
 *  - cleanup.  when do we ever purge things to avoid running out of memory/storage?
 *******************************************************************************/
public interface StateProviderInterface
{

   /*******************************************************************************
    ** Put a block of data, under a key, into the state store.
    *******************************************************************************/
   <T extends Serializable> void put(AbstractStateKey key, T data);

   /*******************************************************************************
    ** Get a block of data, under a key, from the state store.
    *******************************************************************************/
   <T extends Serializable> Optional<T> get(Class<? extends T> type, AbstractStateKey key);

   /*******************************************************************************
    ** Remove a block of data, under a key, from the state store.
    *******************************************************************************/
   void remove(AbstractStateKey key);

   /*******************************************************************************
    ** Get the current status
    *******************************************************************************/
   String status();

   /*******************************************************************************
    ** Clean entries that started before the given Instant
    *******************************************************************************/
   void clean(Instant startTime);
}
