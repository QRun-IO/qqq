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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;


/*******************************************************************************
 ** Delete old history in bounded batches, preserving each check's last transition.
 *******************************************************************************/
public class HealthRetention
{
   /*******************************************************************************
    ** Sweep using the current automated session. Return deleted row count.
    *******************************************************************************/
   public static long sweep(QInstance qInstance, Instant now) throws QException
   {
      Instant cutoff = now.minus(HealthMonitoringMetaData.ofOrWithNew(qInstance).getResultRetentionDays(), ChronoUnit.DAYS);
      Set<Serializable> keep = new HashSet<>();
      for(QRecord check : HealthStore.query(HealthMetaDataProvider.CHECK_TABLE, new QQueryFilter()))
      {
         List<QRecord> latest = HealthStore.query(HealthMetaDataProvider.RESULT_TABLE, new QQueryFilter()
            .withCriteria(new QFilterCriteria("healthCheckId", QCriteriaOperator.EQUALS, check.getValue("id")))
            .withCriteria(new QFilterCriteria("isTransition", QCriteriaOperator.EQUALS, true))
            .withOrderBy(new QFilterOrderBy("startedAt", false)).withOrderBy(new QFilterOrderBy("id", false)).withLimit(1));
         if(!latest.isEmpty())
         {
            keep.add(latest.getFirst().getValue("id"));
         }
      }
      long count = 0;
      while(true)
      {
         QQueryFilter filter = new QQueryFilter(new QFilterCriteria("startedAt", QCriteriaOperator.LESS_THAN, cutoff))
            .withCriteria(new QFilterCriteria("status", QCriteriaOperator.NOT_EQUALS, HealthResultStatus.RUNNING.name())).withLimit(1000);
         if(!keep.isEmpty())
         {
            filter.withCriteria(new QFilterCriteria("id", QCriteriaOperator.NOT_IN, new ArrayList<>(keep)));
         }
         List<Serializable> ids = HealthStore.query(HealthMetaDataProvider.RESULT_TABLE, filter).stream().map(r -> r.getValue("id")).toList();
         if(ids.isEmpty())
         {
            return (count);
         }
         DeleteOutput output = new DeleteAction().execute(new DeleteInput().withTableName(HealthMetaDataProvider.RESULT_TABLE).withPrimaryKeys(ids));
         if(output.getDeletedRecordCount() != ids.size())
         {
            throw new QException("Health retention could not delete its entire batch");
         }
         count += output.getDeletedRecordCount();
      }
   }
}
