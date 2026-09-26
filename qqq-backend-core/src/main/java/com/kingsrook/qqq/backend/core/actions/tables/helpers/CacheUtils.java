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

package com.kingsrook.qqq.backend.core.actions.tables.helpers;


import java.time.Instant;
import com.kingsrook.qqq.backend.core.logging.LogPair;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.cache.CacheUseCase;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.utils.BackendQueryFilterUtils;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class CacheUtils
{
   private static final QLogger LOG = QLogger.getLogger(CacheUtils.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   static QRecord mapSourceRecordToCacheRecord(QTableMetaData table, QRecord recordFromSource, CacheUseCase cacheUseCase)
   {
      QRecord cacheRecord = new QRecord(recordFromSource);

      //////////////////////////////////////////////////////////////////////////////////////////////
      // make sure every value in the qRecord is set, because we will possibly be doing an update //
      // on this record and want to null out any fields not set, not leave them populated         //
      //////////////////////////////////////////////////////////////////////////////////////////////
      for(String fieldName : table.getFields().keySet())
      {
         if(fieldName.equals(table.getPrimaryKeyField()))
         {
            if(!cacheUseCase.getDoCopySourcePrimaryKeyToCache())
            {
               cacheRecord.removeValue(fieldName);
            }
         }
         else if(!cacheRecord.getValues().containsKey(fieldName))
         {
            cacheRecord.setValue(fieldName, null);
         }
      }

      if(StringUtils.hasContent(table.getCacheOf().getCachedDateFieldName()))
      {
         cacheRecord.setValue(table.getCacheOf().getCachedDateFieldName(), Instant.now());
      }
      return (cacheRecord);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static boolean shouldCacheRecord(QTableMetaData table, QRecord recordToCache)
   {
      boolean shouldCacheRecord = true;
      recordMatchExclusionLoop:
      for(CacheUseCase useCase : CollectionUtils.nonNullList(table.getCacheOf().getUseCases()))
      {
         for(QQueryFilter filter : CollectionUtils.nonNullList(useCase.getExcludeRecordsMatching()))
         {
            if(BackendQueryFilterUtils.doesRecordMatch(filter, recordToCache))
            {
               LOG.info("Not caching record because it matches a use case's filter exclusion", new LogPair("record", recordToCache), new LogPair("filter", filter));
               shouldCacheRecord = false;
               break recordMatchExclusionLoop;
            }
         }
      }

      return (shouldCacheRecord);
   }

}
