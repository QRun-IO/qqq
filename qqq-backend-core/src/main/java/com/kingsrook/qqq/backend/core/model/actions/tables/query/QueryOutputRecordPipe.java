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


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.reporting.RecordPipe;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Query output that uses a RecordPipe
 *******************************************************************************/
class QueryOutputRecordPipe implements QueryOutputStorageInterface
{
   private static final QLogger LOG = QLogger.getLogger(QueryOutputRecordPipe.class);

   private RecordPipe recordPipe;



   /*******************************************************************************
    **
    *******************************************************************************/
   public QueryOutputRecordPipe(RecordPipe recordPipe)
   {
      this.recordPipe = recordPipe;
   }



   /*******************************************************************************
    ** add a record to this output
    *******************************************************************************/
   @Override
   public void addRecord(QRecord record) throws QException
   {
      recordPipe.addRecord(record);
   }



   /*******************************************************************************
    ** add a list of records to this output
    *******************************************************************************/
   @Override
   public void addRecords(List<QRecord> records) throws QException
   {
      recordPipe.addRecords(records);
   }



   /*******************************************************************************
    ** Get all stored records
    *******************************************************************************/
   @Override
   public List<QRecord> getRecords()
   {
      throw (new IllegalStateException("getRecords may not be called on a piped query output"));
   }

}
