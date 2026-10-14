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

package com.kingsrook.qqq.backend.core.health;


import java.io.Serializable;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;


/*******************************************************************************
 ** Small persistence boundary using normal core actions and their error results.
 *******************************************************************************/
final class HealthStore
{
   static final Object MEMORY_LOCK = new Object();



   /*******************************************************************************
    ** Only memory needs a shared monitor to emulate database uniqueness. A fresh
    ** monitor for other backends never serializes separate operations or pods.
    *******************************************************************************/
   static Object lockFor(QInstance qInstance) throws QException
   {
      String backend = qInstance.getTable(HealthMetaDataProvider.RESULT_TABLE).getBackendName();
      return (new QBackendModuleDispatcher().getQBackendModule(qInstance.getBackend(backend)) instanceof MemoryBackendModule ? MEMORY_LOCK : new Object());
   }



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private HealthStore()
   {
   }



   /*******************************************************************************
    ** Read records using the caller's session, including internal transition state.
    *******************************************************************************/
   static List<QRecord> query(String table, QQueryFilter filter) throws QException
   {
      return (new QueryAction().execute(new QueryInput().withTableName(table).withFilter(filter).withShouldOmitHiddenFields(false)).getRecords());
   }



   /*******************************************************************************
    ** Lookup by a field; uniqueness is enforced by the table metadata.
    *******************************************************************************/
   static QRecord find(String table, String field, Serializable value) throws QException
   {
      List<QRecord> records = query(table, new QQueryFilter(new QFilterCriteria(field, QCriteriaOperator.EQUALS, value)));
      return (records.isEmpty() ? null : records.getFirst());
   }



   /*******************************************************************************
    ** Inserts return row-level failures as well as throwing action failures.
    *******************************************************************************/
   static QRecord insert(String table, QRecord record) throws QException
   {
      List<QRecord> records = new InsertAction().execute(new InsertInput().withTableName(table).withRecord(record)).getRecords();
      if(records.isEmpty())
      {
         throw new QException("Health insert returned no record");
      }
      QRecord inserted = records.getFirst();
      if((inserted.getErrors() != null && !inserted.getErrors().isEmpty()) || inserted.getValue("id") == null)
      {
         throw new QException("Health insert failed");
      }
      return (inserted);
   }



   /*******************************************************************************
    ** Fail rather than silently accepting a rejected update.
    *******************************************************************************/
   static void update(String table, QRecord record) throws QException
   {
      List<QRecord> records = new UpdateAction().execute(new UpdateInput().withTableName(table).withRecord(record)).getRecords();
      if(records.isEmpty() || (records.getFirst().getErrors() != null && !records.getFirst().getErrors().isEmpty()))
      {
         throw new QException("Health update failed");
      }
   }
}
