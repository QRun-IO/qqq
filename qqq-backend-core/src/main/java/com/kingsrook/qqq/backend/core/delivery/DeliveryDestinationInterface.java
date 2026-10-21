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


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.exceptions.QException;


/*******************************************************************************
 ** Owner-scoped output delivery provider.
 *******************************************************************************/
public interface DeliveryDestinationInterface
{
   /*******************************************************************************
    ** Describe the next target in the current owner session.
    *******************************************************************************/
   DeliveryTargetStep describeTargets(DeliveryDestinationContext context, Map<String, Serializable> chosen) throws QException;



   /*******************************************************************************
    ** Validate a complete target before sending.
    *******************************************************************************/
   default List<String> validate(DeliveryRequest request) throws QException
   {
      return (List.of());
   }



   /*******************************************************************************
    ** Deliver idempotently using the request key.
    *******************************************************************************/
   DeliveryResult deliver(DeliveryRequest request) throws QException;



   /*******************************************************************************
    ** Reconcile a previous delivery when supported.
    *******************************************************************************/
   default Optional<DeliveryResult> lookup(String idempotencyKey) throws QException
   {
      return (Optional.empty());
   }
}
