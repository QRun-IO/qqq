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
import com.kingsrook.qqq.backend.core.actions.ai.DataClassification;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;


/*******************************************************************************
 ** Authenticated retrieval of output retained in application storage.
 *******************************************************************************/
public class DownloadDeliveryDestination implements DeliveryDestinationInterface
{
   /*******************************************************************************
    ** Default download policy.
    *******************************************************************************/
   public static QDeliveryDestinationMetaData metadata()
   {
      return (new QDeliveryDestinationMetaData().withName("DOWNLOAD").withLabel("Download").withIcon("download")
         .withMaxClassification(DataClassification.PHI).withMaxAttachmentBytes(Long.MAX_VALUE).withCodeReference(new QCodeReference(DownloadDeliveryDestination.class)));
   }



   /*******************************************************************************
    ** No destination-specific target is needed.
    *******************************************************************************/
   @Override
   public DeliveryTargetStep describeTargets(DeliveryDestinationContext context, Map<String, Serializable> chosen)
   {
      return (new DeliveryTargetStep().withComplete(true).withSummary("Download"));
   }



   /*******************************************************************************
    ** A missing stored output cannot be represented as a download.
    *******************************************************************************/
   @Override
   public List<String> validate(DeliveryRequest request)
   {
      return (request.getLink() == null ? List.of("No stored output is available.") : List.of());
   }



   /*******************************************************************************
    ** Return the authenticated application link.
    *******************************************************************************/
   @Override
   public DeliveryResult deliver(DeliveryRequest request)
   {
      return (new DeliveryResult().withStatus(DeliveryResult.Status.DELIVERED).withExternalReference(request.getLink()));
   }
}
