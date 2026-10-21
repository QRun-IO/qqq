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


/*******************************************************************************
 ** Meta-data, to assign to a table which is a "cache of" another table.
 ** e.g., a database table that's a "cache of" an api table - we'd have
 ** databaseTable.withCacheOf(sourceTable=apiTable)
 *******************************************************************************/
public class CacheOf implements Cloneable
{
   private String             sourceTable;
   private Integer            expirationSeconds;
   private String             cachedDateFieldName;
   private List<CacheUseCase> useCases;

   // private QCodeReference mapper;



   /*******************************************************************************
    ** Getter for sourceTable
    **
    *******************************************************************************/
   public String getSourceTable()
   {
      return sourceTable;
   }



   /*******************************************************************************
    ** Setter for sourceTable
    **
    *******************************************************************************/
   public void setSourceTable(String sourceTable)
   {
      this.sourceTable = sourceTable;
   }



   /*******************************************************************************
    ** Fluent setter for sourceTable
    **
    *******************************************************************************/
   public CacheOf withSourceTable(String sourceTable)
   {
      this.sourceTable = sourceTable;
      return (this);
   }



   /*******************************************************************************
    ** Getter for expirationSeconds
    **
    *******************************************************************************/
   public Integer getExpirationSeconds()
   {
      return expirationSeconds;
   }



   /*******************************************************************************
    ** Setter for expirationSeconds
    **
    *******************************************************************************/
   public void setExpirationSeconds(Integer expirationSeconds)
   {
      this.expirationSeconds = expirationSeconds;
   }



   /*******************************************************************************
    ** Fluent setter for expirationSeconds
    **
    *******************************************************************************/
   public CacheOf withExpirationSeconds(Integer expirationSeconds)
   {
      this.expirationSeconds = expirationSeconds;
      return (this);
   }



   /*******************************************************************************
    ** Getter for cachedDateFieldName
    **
    *******************************************************************************/
   public String getCachedDateFieldName()
   {
      return cachedDateFieldName;
   }



   /*******************************************************************************
    ** Setter for cachedDateFieldName
    **
    *******************************************************************************/
   public void setCachedDateFieldName(String cachedDateFieldName)
   {
      this.cachedDateFieldName = cachedDateFieldName;
   }



   /*******************************************************************************
    ** Fluent setter for cachedDateFieldName
    **
    *******************************************************************************/
   public CacheOf withCachedDateFieldName(String cachedDateFieldName)
   {
      this.cachedDateFieldName = cachedDateFieldName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for useCases
    **
    *******************************************************************************/
   public List<CacheUseCase> getUseCases()
   {
      return useCases;
   }



   /*******************************************************************************
    ** Setter for useCases
    **
    *******************************************************************************/
   public void setUseCases(List<CacheUseCase> useCases)
   {
      this.useCases = useCases;
   }



   /*******************************************************************************
    ** Fluent setter for useCases
    **
    *******************************************************************************/
   public CacheOf withUseCases(List<CacheUseCase> useCases)
   {
      this.useCases = useCases;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for useCases
    **
    *******************************************************************************/
   public CacheOf withUseCase(CacheUseCase useCase)
   {
      if(this.useCases == null)
      {
         this.useCases = new ArrayList<>();
      }
      this.useCases.add(useCase);
      return (this);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public CacheOf clone()
   {
      try
      {
         CacheOf clone = (CacheOf) super.clone();
         if(useCases != null)
         {
            clone.useCases = new ArrayList<>();
            useCases.forEach(c -> clone.useCases.add(c.clone()));
         }
         return clone;
      }
      catch(CloneNotSupportedException e)
      {
         throw new AssertionError();
      }
   }
}
