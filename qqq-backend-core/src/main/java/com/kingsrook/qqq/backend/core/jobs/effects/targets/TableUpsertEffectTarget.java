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


package com.kingsrook.qqq.backend.core.jobs.effects.targets;


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.SystemScope;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectRequest;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectResult;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectTarget;
import com.kingsrook.qqq.backend.core.jobs.effects.ReconcileResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** An idempotent external write keyed by the destination table's natural unique key.
 *******************************************************************************/
public class TableUpsertEffectTarget implements EffectTarget
{
   /*******************************************************************************
    ** Query and write as the system user so record visibility cannot cause a duplicate insert.
    *******************************************************************************/
   @Override
   @SuppressWarnings("unchecked")
   public EffectResult deliver(EffectRequest request)
   {
      try
      {
         return (SystemScope.call(() ->
         {
            String                    tableName = (String) request.payload().get("tableName");
            QRecord                   existing  = find(request);
            String                    keyField  = QContext.getQInstance().getTable(tableName).getPrimaryKeyField();
            Map<String, Serializable> values    = (Map<String, Serializable>) request.payload().get("values");
            QRecord                   record    = new QRecord();
            values.forEach(record::setValue);
            List<QRecord> results;
            if(existing == null)
            {
               results = new InsertAction().execute(new InsertInput(tableName).withRecord(record)).getRecords();
            }
            else
            {
               record.setValue(keyField, existing.getValue(keyField));
               results = new UpdateAction().execute(new UpdateInput(tableName).withRecord(record)).getRecords();
            }
            QRecord result = results.get(0);
            if(CollectionUtils.nullSafeHasContents(result.getErrors()))
            {
               return (EffectResult.failed(result.getErrors().toString()));
            }
            return (EffectResult.sent(result.getValueString(keyField)));
         }));
      }
      catch(Exception e)
      {
         return (EffectResult.unknown(e.getMessage()));
      }
   }



   /*******************************************************************************
    ** Reconcile against the same natural key used to deliver the effect.
    *******************************************************************************/
   @Override
   public ReconcileResult reconcile(EffectRequest request)
   {
      try
      {
         return (SystemScope.call(() ->
         {
            QRecord record = find(request);
            String  table  = (String) request.payload().get("tableName");
            return (record == null ? ReconcileResult.notFound()
               : ReconcileResult.found(record.getValueString(QContext.getQInstance().getTable(table).getPrimaryKeyField())));
         }));
      }
      catch(Exception e)
      {
         return (ReconcileResult.unknown(e.getMessage()));
      }
   }



   /*******************************************************************************
    ** Reject incomplete keys rather than querying or updating an arbitrary row.
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   private QRecord find(EffectRequest request) throws QException
   {
      String                    tableName = (String) request.payload().get("tableName");
      List<String>              keyFields = (List<String>) request.payload().get("keyFields");
      Map<String, Serializable> values    = (Map<String, Serializable>) request.payload().get("values");
      if(tableName == null || tableName.isBlank() || keyFields == null || keyFields.isEmpty() || values == null)
      {
         throw (new QException("tableName, keyFields and values are required"));
      }
      QQueryFilter filter = new QQueryFilter();
      for(String field : keyFields)
      {
         if(field == null || !values.containsKey(field) || values.get(field) == null)
         {
            throw (new QException("A non-null natural key value is required for " + field));
         }
         filter.withCriteria(new QFilterCriteria(field, QCriteriaOperator.EQUALS, values.get(field)));
      }
      List<QRecord> records = new QueryAction().execute(new QueryInput(tableName).withFilter(filter)).getRecords();
      if(records.size() > 1)
      {
         throw (new QException("Natural key matches more than one row in " + tableName));
      }
      return (records.isEmpty() ? null : records.get(0));
   }
}
