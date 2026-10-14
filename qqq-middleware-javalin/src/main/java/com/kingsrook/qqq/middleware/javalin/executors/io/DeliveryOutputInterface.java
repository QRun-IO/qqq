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

package com.kingsrook.qqq.middleware.javalin.executors.io;


import java.util.List;
import com.kingsrook.qqq.backend.core.delivery.DeliveryAttachment;
import com.kingsrook.qqq.backend.core.delivery.DeliveryTargetStep;
import com.kingsrook.qqq.backend.core.delivery.QDeliveryDestinationMetaData;


/*******************************************************************************
 ** Output mappings for delivery operations.
 *******************************************************************************/
public interface DeliveryOutputInterface extends AbstractMiddlewareOutputInterface
{
   /*******************************************************************************
    ** Map visible destination metadata without code references or provider settings.
    *******************************************************************************/
   default void setDestinations(List<QDeliveryDestinationMetaData> destinations)
   {
      throw new UnsupportedOperationException();
   }



   /*******************************************************************************
    ** Map the next contingent target step.
    *******************************************************************************/
   default void setStep(DeliveryTargetStep step)
   {
      throw new UnsupportedOperationException();
   }



   /*******************************************************************************
    ** Map authorized storage references for streaming.
    *******************************************************************************/
   default void setAttachments(List<DeliveryAttachment> attachments)
   {
      throw new UnsupportedOperationException();
   }


   /*******************************************************************************
    ** Stored text output when the producer did not supply files.
    *******************************************************************************/
   default void setTextOutput(String text)
   {
      throw new UnsupportedOperationException();
   }
}
