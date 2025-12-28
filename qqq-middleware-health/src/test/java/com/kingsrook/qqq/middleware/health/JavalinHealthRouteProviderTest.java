/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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

