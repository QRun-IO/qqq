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

package com.kingsrook.qqq.backend.core.delivery;


import java.time.Instant;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.QScheduleMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;


/*******************************************************************************
 ** Optional scheduled retries; each attempt rebuilds the owner's authority.
 *******************************************************************************/
public class DeliveryRetryProcess implements BackendStep, MetaDataProducerInterface<QProcessMetaData>
{
   public static final String NAME = "deliveryRetry";



   /*******************************************************************************
    ** Register with the standard core process scheduler.
    *******************************************************************************/
   @Override
   public QProcessMetaData produce(QInstance instance)
   {
      return (new QProcessMetaData().withName(NAME).withLabel("Retry Output Deliveries")
         .withStepList(List.of(new QBackendStepMetaData().withName("execute").withCode(new QCodeReference(DeliveryRetryProcess.class)))));
   }



   /*******************************************************************************
    ** No tables means no retry work. Ordinary users cannot run the system dispatcher.
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      if(QContext.getQInstance().getTable(DeliveryMetaDataProvider.DELIVERY_LOG) == null)
      {
         return;
      }
      if(!(QContext.getQSession() instanceof QSystemUserSession))
      {
         throw new QPermissionDeniedException("Delivery retries require the system context.");
      }
      boolean previous = DeliveryTableCustomizer.LOG_WRITE.get();
      DeliveryTableCustomizer.LOG_WRITE.set(true);
      try
      {
         int retentionDays = DeliveryDestinationsMetaData.of(QContext.getQInstance()).getLogRetentionDays();
         new DeleteAction().execute(new DeleteInput(DeliveryMetaDataProvider.DELIVERY_LOG).withQueryFilter(new QQueryFilter()
            .withCriteria(new QFilterCriteria("createdAt", QCriteriaOperator.LESS_THAN, Instant.now().minusSeconds(86400L * retentionDays)))));
      }
      finally
      {
         DeliveryTableCustomizer.LOG_WRITE.set(previous);
      }
      List<QRecord> rows = new QueryAction().execute(new QueryInput(DeliveryMetaDataProvider.DELIVERY_LOG).withFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("status", QCriteriaOperator.EQUALS, "FAILED_RETRYABLE"))
         .withCriteria(new QFilterCriteria("nextAttemptAt", QCriteriaOperator.LESS_THAN_OR_EQUALS, Instant.now())))).getRecords();
      for(QRecord row : rows)
      {
         try
         {
            DeliveryRequest request = JsonUtils.toObject(row.getValueString("storageReference"), DeliveryRequest.class);
            new DeliverOutputAction().execute(request);
         }
         catch(java.io.IOException e)
         {
            throw new QException("Invalid stored delivery request.", e);
         }
      }
   }
}
