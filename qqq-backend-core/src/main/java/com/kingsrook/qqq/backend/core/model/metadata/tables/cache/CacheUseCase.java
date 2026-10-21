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

package com.kingsrook.qqq.backend.core.model.metadata.tables.cache;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;


/*******************************************************************************
 **
 *******************************************************************************/
public class CacheUseCase implements Cloneable
{

   /***************************************************************************
    **
    ***************************************************************************/
   public enum Type
   {
      PRIMARY_KEY_TO_PRIMARY_KEY, // e.g., the primary key in the cache table equals the primary key in the source table.
      UNIQUE_KEY_TO_PRIMARY_KEY, // e.g., a unique key in the cache table equals the primary key in the source table.
      UNIQUE_KEY_TO_UNIQUE_KEY // e..g, a unique key in the cache table equals a unique key in the source table.
   }



   private Type    type;
   private boolean cacheSourceMisses = false; // whether or not, if a "miss" happens in the SOURCE, if that fact gets cached.

   //////////////////////////
   // for UNIQUE_KEY types //
   //////////////////////////
   private UniqueKey cacheUniqueKey;
   private UniqueKey sourceUniqueKey;
   private boolean   doCopySourcePrimaryKeyToCache = false;

   private List<QQueryFilter> excludeRecordsMatching;



   /*******************************************************************************
    ** Getter for type
    **
    *******************************************************************************/
   public Type getType()
   {
      return type;
   }



   /*******************************************************************************
    ** Setter for type
    **
    *******************************************************************************/
   public void setType(Type type)
   {
      this.type = type;
   }



   /*******************************************************************************
    ** Fluent setter for type
    **
    *******************************************************************************/
   public CacheUseCase withType(Type type)
   {
      this.type = type;
      return (this);
   }



   /*******************************************************************************
    ** Getter for cacheSourceMisses
    **
    *******************************************************************************/
   public boolean getCacheSourceMisses()
   {
      return cacheSourceMisses;
   }



   /*******************************************************************************
    ** Setter for cacheSourceMisses
    **
    *******************************************************************************/
   public void setCacheSourceMisses(boolean cacheSourceMisses)
   {
      this.cacheSourceMisses = cacheSourceMisses;
   }



   /*******************************************************************************
    ** Fluent setter for cacheSourceMisses
    **
    *******************************************************************************/
   public CacheUseCase withCacheSourceMisses(boolean cacheSourceMisses)
   {
      this.cacheSourceMisses = cacheSourceMisses;
      return (this);
   }



   /*******************************************************************************
    ** Getter for cacheUniqueKey
    **
    *******************************************************************************/
   public UniqueKey getCacheUniqueKey()
   {
      return cacheUniqueKey;
   }



   /*******************************************************************************
    ** Setter for cacheUniqueKey
    **
    *******************************************************************************/
   public void setCacheUniqueKey(UniqueKey cacheUniqueKey)
   {
      this.cacheUniqueKey = cacheUniqueKey;
   }



   /*******************************************************************************
    ** Fluent setter for cacheUniqueKey
    **
    *******************************************************************************/
   public CacheUseCase withCacheUniqueKey(UniqueKey cacheUniqueKey)
   {
      this.cacheUniqueKey = cacheUniqueKey;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sourceUniqueKey
    **
    *******************************************************************************/
   public UniqueKey getSourceUniqueKey()
   {
      return sourceUniqueKey;
   }



   /*******************************************************************************
    ** Setter for sourceUniqueKey
    **
    *******************************************************************************/
   public void setSourceUniqueKey(UniqueKey sourceUniqueKey)
   {
      this.sourceUniqueKey = sourceUniqueKey;
   }



   /*******************************************************************************
    ** Fluent setter for sourceUniqueKey
    **
    *******************************************************************************/
   public CacheUseCase withSourceUniqueKey(UniqueKey sourceUniqueKey)
   {
      this.sourceUniqueKey = sourceUniqueKey;
      return (this);
   }



   /*******************************************************************************
    ** Getter for excludeRecordsMatching
    **
    *******************************************************************************/
   public List<QQueryFilter> getExcludeRecordsMatching()
   {
      return excludeRecordsMatching;
   }



   /*******************************************************************************
    ** Setter for excludeRecordsMatching
    **
    *******************************************************************************/
   public void setExcludeRecordsMatching(List<QQueryFilter> excludeRecordsMatching)
   {
      this.excludeRecordsMatching = excludeRecordsMatching;
   }



   /*******************************************************************************
    ** Fluent setter for excludeRecordsMatching
    **
    *******************************************************************************/
   public CacheUseCase withExcludeRecordsMatching(List<QQueryFilter> excludeRecordsMatching)
   {
      this.excludeRecordsMatching = excludeRecordsMatching;
      return (this);
   }



   /*******************************************************************************
    ** Getter for doCopySourcePrimaryKeyToCache
    *******************************************************************************/
   public boolean getDoCopySourcePrimaryKeyToCache()
   {
      return (this.doCopySourcePrimaryKeyToCache);
   }



   /*******************************************************************************
    ** Setter for doCopySourcePrimaryKeyToCache
    *******************************************************************************/
   public void setDoCopySourcePrimaryKeyToCache(boolean doCopySourcePrimaryKeyToCache)
   {
      this.doCopySourcePrimaryKeyToCache = doCopySourcePrimaryKeyToCache;
   }



   /*******************************************************************************
    ** Fluent setter for doCopySourcePrimaryKeyToCache
    *******************************************************************************/
   public CacheUseCase withDoCopySourcePrimaryKeyToCache(boolean doCopySourcePrimaryKeyToCache)
   {
      this.doCopySourcePrimaryKeyToCache = doCopySourcePrimaryKeyToCache;
      return (this);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public CacheUseCase clone()
   {
      try
      {
         CacheUseCase clone = (CacheUseCase) super.clone();

         if(cacheUniqueKey != null)
         {
            clone.cacheUniqueKey = cacheUniqueKey.clone();
         }

         if(sourceUniqueKey != null)
         {
            clone.sourceUniqueKey = sourceUniqueKey.clone();
         }

         if(excludeRecordsMatching != null)
         {
            clone.excludeRecordsMatching = new ArrayList<>();
            for(QQueryFilter filter : excludeRecordsMatching)
            {
               clone.excludeRecordsMatching.add(filter.clone());
            }
         }

         return clone;
      }
      catch(CloneNotSupportedException e)
      {
         throw new AssertionError();
      }
   }

}
