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


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import org.junit.jupiter.api.Test;
import static com.kingsrook.qqq.backend.core.instances.QInstanceValidatorTest.assertValidationFailureReasonsAllowingExtraReasons;
import static com.kingsrook.qqq.backend.core.instances.QInstanceValidatorTest.assertValidationSuccess;


/*******************************************************************************
 ** Supplemental destination metadata participates in instance startup validation.
 *******************************************************************************/
class DeliveryValidationTest extends BaseTest
{
   /*******************************************************************************
    ** The core built-ins satisfy the SPI contract.
    *******************************************************************************/
   @Test
   void validDestinations()
   {
      assertValidationSuccess(instance -> DeliveryDestinationsMetaData.ofOrWithNew(instance)
         .withDestination(DownloadDeliveryDestination.metadata()).withDestination(EmailDeliveryDestination.metadata()));
   }



   /*******************************************************************************
    ** Duplicate names are retained and rejected rather than silently overwritten.
    *******************************************************************************/
   @Test
   void duplicateNames()
   {
      assertValidationFailureReasonsAllowingExtraReasons(instance -> DeliveryDestinationsMetaData.ofOrWithNew(instance)
         .withDestination(DownloadDeliveryDestination.metadata()).withDestination(DownloadDeliveryDestination.metadata()),
         "Delivery destination [DOWNLOAD]: name must be unique");
   }



   /*******************************************************************************
    ** Arbitrary code references cannot be registered as delivery providers.
    *******************************************************************************/
   @Test
   void invalidImplementation()
   {
      assertValidationFailureReasonsAllowingExtraReasons(instance -> DeliveryDestinationsMetaData.ofOrWithNew(instance)
         .withDestination(DownloadDeliveryDestination.metadata().withCodeReference(new QCodeReference(String.class))),
         "Delivery destination [DOWNLOAD]: codeReference must implement DeliveryDestinationInterface");
   }
}
