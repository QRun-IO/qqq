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


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthCheckMetaData;
import com.kingsrook.qqq.middleware.javalin.QJavalinMetaData;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** MetaData producer for health check configuration.
 **
 ** This producer demonstrates the recommended pattern for auto-registering
 ** health endpoints via metadata. Applications can extend this class and
 ** override the produce() method to customize health indicator configuration.
 **
 ** Example usage:
 ** <pre>
 ** public class MyHealthMetaDataProducer extends HealthMetaDataProducer
 ** {
 **    &#64;Override
 **    protected HealthCheckMetaData buildHealthCheckMetaData(QInstance qInstance)
 **    {
 **       return new HealthCheckMetaData()
 **          .withEnabled(true)
 **          .withEndpointPath("/health")
 **          .withIndicators(List.of(
 **             new DatabaseHealthIndicator().withBackendName("rdbms"),
 **             new MemoryHealthIndicator().withThreshold(85)
 **          ));
 **    }
 ** }
 ** </pre>
 *******************************************************************************/
public class HealthMetaDataProducer extends MetaDataProducer<QJavalinMetaData>
{
   private static final QLogger LOG = QLogger.getLogger(HealthMetaDataProducer.class);



   /*******************************************************************************
    ** Produce QJavalinMetaData with health check configuration and auto-register
    ** the JavalinHealthRouteProvider.
    **
    ** This method performs two key operations:
    ** 1. Adds HealthCheckMetaData to QInstance supplemental metadata
    ** 2. Registers JavalinHealthRouteProvider reference for auto-loading
    **
    ** Applications should override buildHealthCheckMetaData() to customize
    ** health indicator configuration.
    *******************************************************************************/
   @Override
   public QJavalinMetaData produce(QInstance qInstance) throws QException
   {
      QJavalinMetaData javalinMetaData = QJavalinMetaData.ofOrWithNew(qInstance);

      ////////////////////////////////////////////////////////////
      // Build health check configuration (override to customize) //
      ////////////////////////////////////////////////////////////
      HealthCheckMetaData healthCheckMetaData = buildHealthCheckMetaData(qInstance);

      if(healthCheckMetaData != null)
      {
         //////////////////////////////////////////////////////////
         // Add health check metadata to QInstance supplemental data //
         //////////////////////////////////////////////////////////
         qInstance.withSupplementalMetaData(healthCheckMetaData);

         LOG.info("Health check metadata configured",
            logPair("enabled", healthCheckMetaData.getEnabled()),
            logPair("endpointPath", healthCheckMetaData.getEndpointPath()),
            logPair("indicatorCount", healthCheckMetaData.getIndicators() != null ? healthCheckMetaData.getIndicators().size() : 0));

         ///////////////////////////////////////////////////////////////////////////
         // Auto-register JavalinHealthRouteProvider to consume health metadata //
         ///////////////////////////////////////////////////////////////////////////
         javalinMetaData.withAdditionalRouteProviderReference(
            new QCodeReference(JavalinHealthRouteProvider.class)
         );

         LOG.info("Health route provider auto-registered via metadata");
      }
      else
      {
         LOG.debug("Health check metadata is null - health endpoints will not be registered");
      }

      return javalinMetaData;
   }



   /*******************************************************************************
    ** Build the HealthCheckMetaData configuration.
    **
    ** Override this method in application-specific subclasses to customize
    ** health indicators, endpoint path, timeouts, and other configuration.
    **
    ** Default implementation returns null (no health endpoints). Applications
    ** must override to enable health checks.
    **
    ** @param qInstance the QInstance being configured
    ** @return HealthCheckMetaData configuration, or null to disable health checks
    *******************************************************************************/
   protected HealthCheckMetaData buildHealthCheckMetaData(QInstance qInstance) throws QException
   {
      /////////////////////////////////////////////////////////////////////
      // Default: no health checks. Applications should override this. //
      /////////////////////////////////////////////////////////////////////
      return null;
   }
}
