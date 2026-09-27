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

package com.kingsrook.qqq.middleware.health.model.metadata;


import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit test for HealthStatus enum
 *******************************************************************************/
class HealthStatusTest
{

   /*******************************************************************************
    ** Test that aggregate returns DOWN if any status is DOWN
    *******************************************************************************/
   @Test
   void testAggregate_withDown_returnsDown()
   {
      HealthStatus result = HealthStatus.aggregate(HealthStatus.UP, HealthStatus.DOWN, HealthStatus.UP);
      assertThat(result).isEqualTo(HealthStatus.DOWN);
   }



   /*******************************************************************************
    ** Test that aggregate returns DEGRADED if any status is DEGRADED (and none are DOWN)
    *******************************************************************************/
   @Test
   void testAggregate_withDegradedAndNoDown_returnsDegraded()
   {
      HealthStatus result = HealthStatus.aggregate(HealthStatus.UP, HealthStatus.DEGRADED, HealthStatus.UP);
      assertThat(result).isEqualTo(HealthStatus.DEGRADED);
   }



   /*******************************************************************************
    ** Test that aggregate returns UP if all statuses are UP
    *******************************************************************************/
   @Test
   void testAggregate_allUp_returnsUp()
   {
      HealthStatus result = HealthStatus.aggregate(HealthStatus.UP, HealthStatus.UP, HealthStatus.UP);
      assertThat(result).isEqualTo(HealthStatus.UP);
   }



   /*******************************************************************************
    ** Test that aggregate returns UNKNOWN if any status is UNKNOWN (and none are DOWN or DEGRADED)
    *******************************************************************************/
   @Test
   void testAggregate_withUnknownAndNoDownOrDegraded_returnsUnknown()
   {
      HealthStatus result = HealthStatus.aggregate(HealthStatus.UP, HealthStatus.UNKNOWN, HealthStatus.UP);
      assertThat(result).isEqualTo(HealthStatus.UNKNOWN);
   }



   /*******************************************************************************
    ** Test that aggregate returns UNKNOWN for empty array
    *******************************************************************************/
   @Test
   void testAggregate_emptyArray_returnsUnknown()
   {
      HealthStatus result = HealthStatus.aggregate();
      assertThat(result).isEqualTo(HealthStatus.UNKNOWN);
   }



   /*******************************************************************************
    ** Test that aggregate handles null values
    *******************************************************************************/
   @Test
   void testAggregate_withNullValue_treatsAsUnknown()
   {
      HealthStatus result = HealthStatus.aggregate(HealthStatus.UP, null, HealthStatus.UP);
      assertThat(result).isEqualTo(HealthStatus.UNKNOWN);
   }



   /*******************************************************************************
    ** Test that DOWN takes precedence over DEGRADED
    *******************************************************************************/
   @Test
   void testAggregate_downTakesPrecedenceOverDegraded()
   {
      HealthStatus result = HealthStatus.aggregate(HealthStatus.DEGRADED, HealthStatus.DOWN);
      assertThat(result).isEqualTo(HealthStatus.DOWN);
   }



   /*******************************************************************************
    ** Test that DEGRADED takes precedence over UNKNOWN
    *******************************************************************************/
   @Test
   void testAggregate_degradedTakesPrecedenceOverUnknown()
   {
      HealthStatus result = HealthStatus.aggregate(HealthStatus.UNKNOWN, HealthStatus.DEGRADED);
      assertThat(result).isEqualTo(HealthStatus.DEGRADED);
   }
}

