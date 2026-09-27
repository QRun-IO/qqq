/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.utils.memoization;


import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeFunction;


/*******************************************************************************
 ** Basic memoization functionality - with result timeouts (only when doing a get -
 ** there's no cleanup thread), and max-size.
 *******************************************************************************/
public class Memoization<K, V>
{
   private static final QLogger LOG = QLogger.getLogger(Memoization.class);

   private final Map<K, MemoizedResult<V>> map = Collections.synchronizedMap(new LinkedHashMap<>());

   private Duration timeout            = Duration.ofSeconds(600);
   private Integer  maxSize            = 1000;
   private boolean  mayStoreNullValues = true;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public Memoization()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public Memoization(Integer maxSize)
   {
      this.maxSize = maxSize;
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public Memoization(Duration timeout)
   {
      this.timeout = timeout;
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public Memoization(Duration timeout, Integer maxSize)
   {
      this.timeout = timeout;
      this.maxSize = maxSize;
   }



   /*******************************************************************************
    ** Get the memoized Value for a given input Key - computing it if it wasn't previously
    ** memoized (or expired).
    **
    ** If the returned Optional is empty, it means the value is null (whether that
    ** came form memoization, or from the lookupFunction, you don't care - the answer
    ** is null).
    *******************************************************************************/
   public <E extends Exception> Optional<V> getResultThrowing(K key, UnsafeFunction<K, V, E> lookupFunction) throws E
   {
      MemoizedResult<V> result = map.get(key);
      if(result != null)
      {
         if(result.getTime().isAfter(Instant.now().minus(timeout)))
         {
            //////////////////////////////////////////////////////////////////////////////
            // ok, we have a memoized value, and it's not expired, so we can return it. //
            // of course, it might be a memoized null, so we use .ofNullable.           //
            //////////////////////////////////////////////////////////////////////////////
            return (Optional.ofNullable(result.getResult()));
         }
      }

      /////////////////////////////////////////////////////////////////////////////////////////////
      // ok - either we never memoized this key, or it's expired, so, apply the lookup function, //
      // store the result, and then return the value (in an Optional.ofNullable)                 //
      // and if the lookup function throws - then we let it throw.                               //
      /////////////////////////////////////////////////////////////////////////////////////////////
      V value = lookupFunction.apply(key);
      storeResult(key, value);
      return (Optional.ofNullable(value));
   }



   /*******************************************************************************
    ** Get the memoized Value for a given input Key - computing it if it wasn't previously
    ** memoized (or expired).
    **
    ** If a null value was memoized, the resulting optional here will be empty.
    **
    ** If the lookup function throws, then a null value will be memoized and an empty
    ** Optional will be returned.
    **
    ** In here, if the optional is empty, it means the value is null (whether that
    ** came form memoization, or from the lookupFunction, you don't care - the answer
    ** is null).
    *******************************************************************************/
   public Optional<V> getResult(K key, UnsafeFunction<K, V, ?> lookupFunction)
   {
      try
      {
         return getResultThrowing(key, lookupFunction);
      }
      catch(Exception e)
      {
         LOG.warn("Uncaught Exception while executing a Memoization lookupFunction (to avoid this log, add a catch in the lookupFunction)", e);
         storeResult(key, null);
         return (Optional.empty());
      }
   }



   /*******************************************************************************
    ** Get a memoized result, optionally containing a Value, for a given input Key.
    **
    ** If the returned Optional is empty, it means that we haven't ever looked up
    ** or memoized the key (or it's expired).
    **
    ** If the returned Optional is not empty, then it means we've memoized something
    ** (and it's not expired) - so if the Value from the MemoizedResult is null,
    ** then null is the proper memoized value.
    *******************************************************************************/
   public Optional<MemoizedResult<V>> getMemoizedResult(K key)
   {
      MemoizedResult<V> result = map.get(key);
      if(result != null)
      {
         if(result.getTime().isAfter(Instant.now().minus(timeout)))
         {
            return (Optional.of(result));
         }
      }

      return (Optional.empty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void storeResult(K key, V value)
   {
      //////////////////////////////////////////////////////////////////////////////////////////
      // if the value is null, and we're not supposed to store nulls, then return w/o storing //
      //////////////////////////////////////////////////////////////////////////////////////////
      if(value == null && !mayStoreNullValues)
      {
         return;
      }

      map.put(key, new MemoizedResult<>(value));

      //////////////////////////////////////
      // make sure map didn't get too big //
      // do this thread safely, please    //
      //////////////////////////////////////
      try
      {
         if(map.size() > maxSize)
         {
            synchronized(map)
            {
               Iterator<Map.Entry<K, MemoizedResult<V>>> iterator = null;
               while(map.size() > maxSize)
               {
                  if(iterator == null)
                  {
                     iterator = map.entrySet().iterator();
                  }

                  if(iterator.hasNext())
                  {
                     iterator.next();
                     iterator.remove();
                  }
                  else
                  {
                     break;
                  }
               }
            }
         }
      }
      catch(Exception e)
      {
         LOG.error("Error managing size of a Memoization", e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void clear()
   {
      this.map.clear();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void clearKey(K key)
   {
      this.map.remove(key);
   }



   /*******************************************************************************
    ** Setter for timeoutSeconds
    **
    *******************************************************************************/
   public void setTimeout(Duration timeout)
   {
      this.timeout = timeout;
   }



   /*******************************************************************************
    ** Setter for maxSize
    **
    *******************************************************************************/
   public void setMaxSize(Integer maxSize)
   {
      this.maxSize = maxSize;
   }



   /*******************************************************************************
    ** package-private - for tests to look at the map.
    **
    *******************************************************************************/
   Map<K, MemoizedResult<V>> getMap()
   {
      return map;
   }



   /*******************************************************************************
    ** Getter for timeout
    *******************************************************************************/
   public Duration getTimeout()
   {
      return (this.timeout);
   }



   /*******************************************************************************
    ** Fluent setter for timeout
    *******************************************************************************/
   public Memoization<K, V> withTimeout(Duration timeout)
   {
      this.timeout = timeout;
      return (this);
   }



   /*******************************************************************************
    ** Getter for maxSize
    *******************************************************************************/
   public Integer getMaxSize()
   {
      return (this.maxSize);
   }



   /*******************************************************************************
    ** Fluent setter for maxSize
    *******************************************************************************/
   public Memoization<K, V> withMaxSize(Integer maxSize)
   {
      this.maxSize = maxSize;
      return (this);
   }



   /*******************************************************************************
    ** Getter for mayStoreNullValues
    *******************************************************************************/
   public boolean getMayStoreNullValues()
   {
      return (this.mayStoreNullValues);
   }



   /*******************************************************************************
    ** Setter for mayStoreNullValues
    *******************************************************************************/
   public void setMayStoreNullValues(boolean mayStoreNullValues)
   {
      this.mayStoreNullValues = mayStoreNullValues;
   }



   /*******************************************************************************
    ** Fluent setter for mayStoreNullValues
    *******************************************************************************/
   public Memoization<K, V> withMayStoreNullValues(boolean mayStoreNullValues)
   {
      this.mayStoreNullValues = mayStoreNullValues;
      return (this);
   }

}
