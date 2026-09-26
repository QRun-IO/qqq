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


import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class GetActionCacheHelper
{
   private static final QLogger LOG = QLogger.getLogger(GetActionCacheHelper.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   public void handleCaching(GetInput getInput, GetOutput getOutput) throws QException
   {
      ///////////////////////////////////////////////////////
      // copy Get input & output into Query input & output //
      ///////////////////////////////////////////////////////
      QueryInput queryInput = GetAction.convertGetInputToQueryInput(getInput);

      QueryOutput queryOutput = new QueryOutput(queryInput);
      if(getOutput.getRecord() != null)
      {
         queryOutput.addRecord(getOutput.getRecord());
      }

      ////////////////////////////////////
      // run the QueryActionCacheHelper //
      ////////////////////////////////////
      new QueryActionCacheHelper().handleCaching(queryInput, queryOutput);

      ///////////////////////////////////
      // set result back in get output //
      ///////////////////////////////////
      if(CollectionUtils.nullSafeHasContents(queryOutput.getRecords()))
      {
         getOutput.setRecord(queryOutput.getRecords().get(0));
      }
      else
      {
         getOutput.setRecord(null);
      }
   }

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
   // In July 2023, initial caching was added in QueryAction.                                                                             //
   // at this time, it felt wrong to essentially duplicate this code between Get & Query - as Get is a simplified use-case of Query.      //
   // so - we'll keep this code here, as a potential quick/easy fallback - but - see above - where we use QueryActionCacheHelper instead. //
   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   /*
   public void handleCaching(GetInput getInput, GetOutput getOutput) throws QException
   {
      if(getOutput.getRecord() == null)
      {
         ///////////////////////////////////////////////////////////////////////
         // if the record wasn't found, see if we should look in cache-source //
         ///////////////////////////////////////////////////////////////////////
         QRecord recordFromSource = tryToGetFromCacheSource(getInput);
         if(recordFromSource != null)
         {
            /////////////////////////////////////////////////////////////////////////////////////////////////
            // good, we found a record from the source, make sure we should cache it, and if so, do it now //
            // note, we always return the record from the source, even if we don't cache it.               //
            /////////////////////////////////////////////////////////////////////////////////////////////////
            QTableMetaData table         = getInput.getTable();
            QRecord        recordToCache = CacheUtils.mapSourceRecordToCacheRecord(table, recordFromSource);
            getOutput.setRecord(recordToCache);

            boolean shouldCacheRecord = CacheUtils.shouldCacheRecord(table, recordToCache);
            if(shouldCacheRecord)
            {
               InsertInput insertInput = new InsertInput();
               insertInput.setTableName(getInput.getTableName());
               insertInput.setRecords(List.of(recordToCache));
               InsertOutput insertOutput = new InsertAction().execute(insertInput);

               /////////////////////////////////////////////////////////////////////////////////////////////
               // update the result record from the insert (e.g., so we get its id, just in case we care) //
               /////////////////////////////////////////////////////////////////////////////////////////////
               getOutput.setRecord(insertOutput.getRecords().get(0));
            }
         }
      }
      else
      {
         /////////////////////////////////////////////////////////////////////////////////
         // if the record was found, but it's too old, maybe re-fetch from cache source //
         /////////////////////////////////////////////////////////////////////////////////
         refreshCacheIfExpired(getInput, getOutput);
      }
   }



   private QRecord tryToGetFromCacheSource(GetInput getInput) throws QException
   {
      QRecord        recordFromSource = null;
      QTableMetaData table            = getInput.getTable();

      for(CacheUseCase cacheUseCase : CollectionUtils.nonNullList(table.getCacheOf().getUseCases()))
      {
         if(CacheUseCase.Type.UNIQUE_KEY_TO_UNIQUE_KEY.equals(cacheUseCase.getType()) && getInput.getUniqueKey() != null)
         {
            recordFromSource = getFromCachedSourceForUniqueKeyToUniqueKey(getInput, table.getCacheOf().getSourceTable());
            break;
         }
         else
         {
            // todo!!
            throw new NotImplementedException("Not-yet-implemented cache use case type: " + cacheUseCase.getType());
         }
      }

      return (recordFromSource);
   }



   private void refreshCacheIfExpired(GetInput getInput, GetOutput getOutput) throws QException
   {
      QTableMetaData table             = getInput.getTable();
      Integer        expirationSeconds = table.getCacheOf().getExpirationSeconds();
      if(expirationSeconds != null)
      {
         QRecord cachedRecord = getOutput.getRecord();
         Instant cachedDate   = cachedRecord.getValueInstant(table.getCacheOf().getCachedDateFieldName());
         if(cachedDate == null || cachedDate.isBefore(Instant.now().minus(expirationSeconds, ChronoUnit.SECONDS)))
         {
            //////////////////////////////////////////////////////////////////////////
            // keep the serial key from the old record in case we need to delete it //
            //////////////////////////////////////////////////////////////////////////
            Serializable oldRecordPrimaryKey = cachedRecord.getValue(table.getPrimaryKeyField());
            boolean      shouldDeleteCachedRecord;

            ///////////////////////////////////////////
            // fetch record from original source now //
            ///////////////////////////////////////////
            QRecord recordFromSource = tryToGetFromCacheSource(getInput);
            if(recordFromSource != null)
            {
               ///////////////////////////////////////////////////////////////////
               // if the record was found in the source, put it into the output //
               // object so returned back to caller                             //
               ///////////////////////////////////////////////////////////////////
               QRecord recordToCache = CacheUtils.mapSourceRecordToCacheRecord(table, recordFromSource);
               recordToCache.setValue(table.getPrimaryKeyField(), cachedRecord.getValue(table.getPrimaryKeyField()));
               getOutput.setRecord(recordToCache);

               //////////////////////////////////////////////////////////////////////////////////////////////////////////////
               // if the record should be cached, update the cache record - else set the flag to delete the cached record. //
               //////////////////////////////////////////////////////////////////////////////////////////////////////////////
               if(CacheUtils.shouldCacheRecord(table, recordToCache))
               {
                  UpdateInput updateInput = new UpdateInput();
                  updateInput.setTableName(getInput.getTableName());
                  updateInput.setRecords(List.of(recordToCache));
                  UpdateOutput updateOutput = new UpdateAction().execute(updateInput);
                  getOutput.setRecord(updateOutput.getRecords().get(0));
                  shouldDeleteCachedRecord = false;
               }
               else
               {
                  shouldDeleteCachedRecord = true;
               }
            }
            else
            {
               ///////////////////////////////////////////////////////////////////////////////////////
               // if we did not get a record back from the source, empty out the getOutput's record //
               // and set the flag to delete the cached record                                      //
               ///////////////////////////////////////////////////////////////////////////////////////
               getOutput.setRecord(null);
               shouldDeleteCachedRecord = true;
            }

            if(shouldDeleteCachedRecord)
            {
               //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
               // if the record is no longer in the source (or it was in the source, but failed the should-cache check), then remove it from the cache //
               //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
               DeleteInput deleteInput = new DeleteInput();
               deleteInput.setTableName(getInput.getTableName());
               deleteInput.setPrimaryKeys(List.of(oldRecordPrimaryKey));
               new DeleteAction().execute(deleteInput);
            }
         }
      }
   }



   private QRecord getFromCachedSourceForUniqueKeyToUniqueKey(GetInput getInput, String sourceTableName) throws QException
   {
      /////////////////////////////////////////////////////
      // do a Get on the source table, by the unique key //
      /////////////////////////////////////////////////////
      GetInput sourceGetInput = new GetInput();
      sourceGetInput.setTableName(sourceTableName);
      sourceGetInput.setUniqueKey(getInput.getUniqueKey());
      GetOutput sourceGetOutput = new GetAction().execute(sourceGetInput);
      QRecord   outputRecord    = sourceGetOutput.getRecord();

      return (outputRecord);
   }
   */

}
