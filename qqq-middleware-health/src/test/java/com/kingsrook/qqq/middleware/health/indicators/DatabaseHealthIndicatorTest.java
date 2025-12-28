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
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthCheckResult;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthStatus;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit test for DatabaseHealthIndicator
 **
 ** Note: These tests use H2 in-memory database for testing
 *******************************************************************************/
class DatabaseHealthIndicatorTest
{

   /*******************************************************************************
    ** Test that database indicator returns DOWN when backend not found
    *******************************************************************************/
   @Test
   void testCheck_backendNotFound_returnsDown() throws Exception
   {
      QInstance qInstance = new QInstance();
      
      DatabaseHealthIndicator indicator = new DatabaseHealthIndicator()
         .withBackendName("nonExistentBackend");

      HealthCheckResult result = indicator.check(qInstance);

      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isEqualTo(HealthStatus.DOWN);
      assertThat(result.getDetails()).containsKey("error");
   }



   /*******************************************************************************
    ** Test that database indicator returns UNKNOWN when no backend name configured
    *******************************************************************************/
   @Test
   void testCheck_noBackendNameConfigured_returnsUnknown() throws Exception
   {
      QInstance qInstance = new QInstance();
      
      DatabaseHealthIndicator indicator = new DatabaseHealthIndicator();

      HealthCheckResult result = indicator.check(qInstance);

      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isEqualTo(HealthStatus.UNKNOWN);
      assertThat(result.getDetails()).containsKey("error");
   }



   /*******************************************************************************
    ** Test that database indicator returns UP with valid H2 connection
    *******************************************************************************/
   @Test
   void testCheck_validH2Database_returnsUp() throws Exception
   {
      QInstance qInstance = new QInstance();
      
      RDBMSBackendMetaData backend = new RDBMSBackendMetaData()
         .withName("testBackend")
         .withVendor("h2")
         .withHostName("mem")
         .withDatabaseName("test")
         .withUsername("sa");
      
      qInstance.addBackend(backend);

      DatabaseHealthIndicator indicator = new DatabaseHealthIndicator()
         .withBackendName("testBackend")
         .withTimeout(5000);

      HealthCheckResult result = indicator.check(qInstance);

      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isEqualTo(HealthStatus.UP);
      assertThat(result.getDurationMs()).isNotNull();
      assertThat(result.getDetails()).containsKey("backendName");
      assertThat(result.getDetails()).containsKey("vendor");
   }



   /*******************************************************************************
    ** Test that indicator name is correct
    *******************************************************************************/
   @Test
   void testGetName_returnsDatabase()
   {
      DatabaseHealthIndicator indicator = new DatabaseHealthIndicator();
      assertThat(indicator.getName()).isEqualTo("database");
   }



   /*******************************************************************************
    ** Test fluent setters
    *******************************************************************************/
   @Test
   void testFluentSetters()
   {
      DatabaseHealthIndicator indicator = new DatabaseHealthIndicator()
         .withBackendName("myBackend")
         .withTimeout(3000);

      assertThat(indicator.getBackendName()).isEqualTo("myBackend");
      assertThat(indicator.getTimeoutMs()).isEqualTo(3000);
   }
}

