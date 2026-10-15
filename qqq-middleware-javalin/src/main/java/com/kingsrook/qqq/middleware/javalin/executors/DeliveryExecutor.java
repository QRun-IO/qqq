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

package com.kingsrook.qqq.middleware.javalin.executors;


import java.util.List;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.delivery.DeliveryDestinationContext;
import com.kingsrook.qqq.backend.core.delivery.DeliveryDestinations;
import com.kingsrook.qqq.backend.core.delivery.DeliveryMetaDataProvider;
import com.kingsrook.qqq.backend.core.delivery.DeliveryRequest;
import com.kingsrook.qqq.backend.core.delivery.QDeliveryDestinationMetaData;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.executors.io.DeliveryMiddlewareInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.DeliveryOutputInterface;


/*******************************************************************************
 ** Delivery UI operations execute with the authenticated request session.
 *******************************************************************************/
public class DeliveryExecutor extends AbstractMiddlewareExecutor<DeliveryMiddlewareInput, DeliveryOutputInterface>
{
   /*******************************************************************************
    ** Apply the same permission checks for discovery and contingent targets.
    *******************************************************************************/
   @Override
   public void execute(DeliveryMiddlewareInput input, DeliveryOutputInterface output) throws QException
   {
      if(input.getOperation() == DeliveryMiddlewareInput.Operation.LIST)
      {
         output.setDestinations(DeliveryDestinations.visible());
      }
      else if(input.getOperation() == DeliveryMiddlewareInput.Operation.TARGETS)
      {
         QDeliveryDestinationMetaData destination = DeliveryDestinations.find(input.getName());
         if(destination == null)
         {
            throw new QNotFoundException("Delivery destination not found");
         }
         output.setStep(DeliveryDestinations.implementation(destination).describeTargets(new DeliveryDestinationContext().withDestination(destination), input.getChosen()));
      }
      else
      {
         readOutput(input, output);
      }
   }



   /*******************************************************************************
    ** Owners and delivery administrators alone may retrieve stored output.
    *******************************************************************************/
   private void readOutput(DeliveryMiddlewareInput input, DeliveryOutputInterface output) throws QException
   {
      if(QContext.getQInstance().getTable(DeliveryMetaDataProvider.DELIVERY_LOG) == null)
      {
         throw new QNotFoundException("Delivery output not found");
      }
      List<QRecord> records = new QueryAction().execute(new QueryInput(DeliveryMetaDataProvider.DELIVERY_LOG).withFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("id", QCriteriaOperator.EQUALS, input.getLogId())))).getRecords();
      if(records.isEmpty())
      {
         throw new QNotFoundException("Delivery output not found");
      }
      QRecord log = records.get(0);
      if(QContext.getQSession() == null || QContext.getQSession().getUser() == null
         || (!Objects.equals(log.getValueString("ownerUserId"), QContext.getQSession().getUser().getIdReference())
         && !QContext.getQSession().hasPermission(DeliveryMetaDataProvider.ADMIN_PERMISSION)))
      {
         throw new QNotFoundException("Delivery output not found");
      }
      try
      {
         DeliveryRequest request = JsonUtils.toObject(log.getValueString("storageReference"), DeliveryRequest.class);
         if(request.getAttachments().isEmpty())
         {
            output.setTextOutput(request.getSummary() == null ? request.getTitle() : request.getSummary());
         }
         output.setAttachments(request.getAttachments());
      }
      catch(java.io.IOException e)
      {
         throw new QException("Could not read delivery output reference.", e);
      }
   }
}
