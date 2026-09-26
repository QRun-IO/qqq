/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.health;


import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.middleware.health.indicators.MemoryHealthIndicator;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthCheckMetaData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit test for JavalinHealthRouteProvider
 *******************************************************************************/
class JavalinHealthRouteProviderTest
{

   /*******************************************************************************
    ** Test that route provider can be instantiated and configured
    *******************************************************************************/
   @Test
   void testRouteProvider_instantiation()
   {
      QInstance qInstance = new QInstance()
         .withSupplementalMetaData(new HealthCheckMetaData()
            .withEnabled(true)
            .withEndpointPath("/health")
            .withIndicators(List.of(
               new MemoryHealthIndicator().withThreshold(99)
            )));

      JavalinHealthRouteProvider provider = new JavalinHealthRouteProvider();
      provider.setQInstance(qInstance);

      assertThat(provider).isNotNull();
   }



   /*******************************************************************************
    ** Test that route provider handles disabled health check
    *******************************************************************************/
   @Test
   void testRouteProvider_disabled()
   {
      QInstance qInstance = new QInstance()
         .withSupplementalMetaData(new HealthCheckMetaData()
            .withEnabled(false));

      JavalinHealthRouteProvider provider = new JavalinHealthRouteProvider();
      provider.setQInstance(qInstance);

      assertThat(provider).isNotNull();
   }



   /*******************************************************************************
    ** Test that route provider handles null metadata
    *******************************************************************************/
   @Test
   void testRouteProvider_noMetadata()
   {
      QInstance qInstance = new QInstance();

      JavalinHealthRouteProvider provider = new JavalinHealthRouteProvider();
      provider.setQInstance(qInstance);

      assertThat(provider).isNotNull();
   }
}

