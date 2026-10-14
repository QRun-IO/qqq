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


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;


/*******************************************************************************
 ** Merge code-owned identity without overwriting operator scheduling overrides.
 *******************************************************************************/
public class HealthCheckSync
{
   /*******************************************************************************
    ** Sync a newly resolved snapshot. Missing definitions are retained as history.
    *******************************************************************************/
   public List<HealthCheckMetaData> sync(QInstance qInstance) throws QException
   {
      List<HealthCheckMetaData> definitions = HealthChecksMetaData.ofOrWithNew(qInstance).resolveAll(qInstance);
      return (sync(qInstance, definitions));
   }



   /*******************************************************************************
    ** Persist an already resolved snapshot, without invoking suppliers twice.
    *******************************************************************************/
   List<HealthCheckMetaData> sync(QInstance qInstance, List<HealthCheckMetaData> definitions) throws QException
   {
      synchronized(HealthStore.lockFor(qInstance))
      {
         Set<String> names = new HashSet<>();
         for(HealthCheckMetaData definition : definitions)
         {
            names.add(definition.getName());
            QRecord existing = HealthStore.find(HealthMetaDataProvider.CHECK_TABLE, "name", definition.getName());
            QRecord identity = new QRecord().withValue("name", definition.getName())
               .withValue("label", HealthRedactor.redact(definition.getLabel() == null ? definition.getName() : definition.getLabel()))
               .withValue("component", definition.getComponent()).withValue("category", definition.getCategory().name())
               .withValue("kind", definition.getKind().name()).withValue("severity", definition.getSeverity().name()).withValue("registered", true);
            if(existing == null)
            {
               try
               {
                  HealthStore.insert(HealthMetaDataProvider.CHECK_TABLE, new QRecord(identity).withValue("currentStatus", HealthStatus.UNKNOWN.name())
                     .withValue("consecutiveFailures", 0).withValue("consecutiveSuccesses", 0).withValue("flapping", false));
               }
               catch(QException e)
               {
                  // Another pod may have inserted the definition while this pod resolved it.
                  existing = HealthStore.find(HealthMetaDataProvider.CHECK_TABLE, "name", definition.getName());
                  if(existing == null)
                  {
                     throw e;
                  }
               }
            }
            if(existing != null)
            {
               HealthStore.update(HealthMetaDataProvider.CHECK_TABLE, identity.withValue("id", existing.getValue("id")));
            }
         }
         for(QRecord row : HealthStore.query(HealthMetaDataProvider.CHECK_TABLE, new QQueryFilter()))
         {
            if(!names.contains(row.getValueString("name")))
            {
               HealthStore.update(HealthMetaDataProvider.CHECK_TABLE, new QRecord().withValue("id", row.getValue("id")).withValue("registered", false));
            }
         }
      }
      return (definitions);
   }
}
