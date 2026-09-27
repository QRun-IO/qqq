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

package com.kingsrook.qqq.backend.core.modules.backend.implementations.memory;


import java.io.Serializable;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.interfaces.QueryInterface;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.AssociatedRecordDiscovery;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.UniqueKeyLookup;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** In-memory version of query action.
 **
 *******************************************************************************/
public class MemoryQueryAction implements QueryInterface
{

   /*******************************************************************************
    **
    *******************************************************************************/
   public QueryOutput execute(QueryInput queryInput) throws QException
   {
      try
      {
         QueryOutput queryOutput = new QueryOutput(queryInput);

         ///////////////////////////////////////////////////////////////////////////////////////////////////////
         // add the records to the output one-by-one -- this more closely matches how "real" backends perform //
         // and works better w/ pipes                                                                         //
         ///////////////////////////////////////////////////////////////////////////////////////////////////////
         for(QRecord qRecord : MemoryRecordStore.getInstance().query(queryInput))
         {
            queryOutput.addRecord(qRecord);
         }

         return (queryOutput);
      }
      catch(Exception e)
      {
         throw new QException("Error executing query", e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<Serializable> findAssociatedPrimaryKeys(AssociatedRecordDiscovery.Input input) throws QException
   {
      return MemoryRecordStore.getInstance().findAssociatedPrimaryKeys(input);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> readAssociationValues(AssociatedRecordDiscovery.StoredValuesInput input) throws QException
   {
      return MemoryRecordStore.getInstance().readAssociationValues(input);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> lookupUniqueKey(UniqueKeyLookup.Input input) throws QException
   {
      return MemoryRecordStore.getInstance().lookupUniqueKey(input);
   }

}
