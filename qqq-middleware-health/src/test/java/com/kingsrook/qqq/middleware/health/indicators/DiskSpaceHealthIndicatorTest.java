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

package com.kingsrook.qqq.middleware.health.indicators;


import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthCheckResult;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthStatus;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit test for DiskSpaceHealthIndicator
 *******************************************************************************/
class DiskSpaceHealthIndicatorTest
{

   /*******************************************************************************
    ** Test that disk space indicator returns UP when sufficient space available
    *******************************************************************************/
   @Test
   void testCheck_sufficientSpace_returnsUp() throws Exception
   {
      QInstance qInstance = new QInstance();
      
      DiskSpaceHealthIndicator indicator = new DiskSpaceHealthIndicator()
         .withPath(System.getProperty("java.io.tmpdir"))
         .withMinimumFreeBytes(1L); // Very small minimum

      HealthCheckResult result = indicator.check(qInstance);

      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isIn(HealthStatus.UP, HealthStatus.DEGRADED);
      assertThat(result.getDurationMs()).isNotNull();
      assertThat(result.getDetails()).containsKey("path");
      assertThat(result.getDetails()).containsKey("freeBytes");
      assertThat(result.getDetails()).containsKey("totalBytes");
   }



   /*******************************************************************************
    ** Test that disk space indicator returns DOWN for non-existent path
    *******************************************************************************/
   @Test
   void testCheck_nonExistentPath_returnsDown() throws Exception
   {
      QInstance qInstance = new QInstance();
      
      DiskSpaceHealthIndicator indicator = new DiskSpaceHealthIndicator()
         .withPath("/this/path/definitely/does/not/exist/anywhere");

      HealthCheckResult result = indicator.check(qInstance);

      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isEqualTo(HealthStatus.DOWN);
      assertThat(result.getDetails()).containsKey("error");
   }



   /*******************************************************************************
    ** Test that indicator name is correct
    *******************************************************************************/
   @Test
   void testGetName_returnsDiskSpace()
   {
      DiskSpaceHealthIndicator indicator = new DiskSpaceHealthIndicator();
      assertThat(indicator.getName()).isEqualTo("diskSpace");
   }



   /*******************************************************************************
    ** Test fluent setters
    *******************************************************************************/
   @Test
   void testFluentSetters()
   {
      DiskSpaceHealthIndicator indicator = new DiskSpaceHealthIndicator()
         .withPath("/var/qqq")
         .withMinimumFreeBytes(5_000_000_000L);

      assertThat(indicator.getPath()).isEqualTo("/var/qqq");
      assertThat(indicator.getMinimumFreeBytes()).isEqualTo(5_000_000_000L);
   }
}

