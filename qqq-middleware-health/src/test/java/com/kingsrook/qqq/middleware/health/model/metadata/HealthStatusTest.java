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

