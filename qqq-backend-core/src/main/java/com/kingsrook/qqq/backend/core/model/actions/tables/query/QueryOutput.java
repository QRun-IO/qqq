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

package com.kingsrook.qqq.backend.core.model.actions.tables.query;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;


/*******************************************************************************
 ** Output for a query action
 **
 *******************************************************************************/
public class QueryOutput extends AbstractActionOutput implements Serializable
{
   private QueryOutputStorageInterface storage;



   /*******************************************************************************
    ** Construct a new query output, based on a query input (which will drive some
    ** of how our output is structured... e.g., if we pipe the output)
    *******************************************************************************/
   public QueryOutput(QueryInput queryInput)
   {
      if(queryInput.getRecordPipe() != null)
      {
         storage = new QueryOutputRecordPipe(queryInput.getRecordPipe());
      }
      else
      {
         storage = new QueryOutputList(queryInput);
      }
   }



   /*******************************************************************************
    ** Add a record to this output.  Note - we often don't care, in such a method,
    ** whether the record is "completed" or not (e.g., all of its values have been
    ** populated) - but - note in here - that this records MAY be going into a pipe
    ** that could be read asynchronously, at any time, by another thread - SO - only
    ** completely populated records should be passed into this method.
    *******************************************************************************/
   public void addRecord(QRecord record) throws QException
   {
      storage.addRecord(record);
   }



   /*******************************************************************************
    ** add a list of records to this output
    *******************************************************************************/
   public void addRecords(List<QRecord> records) throws QException
   {
      storage.addRecords(records);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public List<QRecord> getRecords()
   {
      return storage.getRecords();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public <T extends QRecordEntity> List<T> getRecordEntities(Class<T> entityClass) throws QException
   {
      List<T> rs = new ArrayList<>();
      for(QRecord record : storage.getRecords())
      {
         rs.add(QRecordEntity.fromQRecord(entityClass, record));
      }
      return (rs);
   }
}
