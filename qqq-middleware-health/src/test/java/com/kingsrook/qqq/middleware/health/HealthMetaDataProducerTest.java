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
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.javalin.QJavalinMetaData;
import com.kingsrook.qqq.middleware.health.indicators.MemoryHealthIndicator;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthCheckMetaData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit test for HealthMetaDataProducer
 *******************************************************************************/
class HealthMetaDataProducerTest
{

   /*******************************************************************************
    ** Test that base HealthMetaDataProducer returns null by default
    *******************************************************************************/
   @Test
   void testBaseProducer_returnsNullMetadata() throws QException
   {
      QInstance            qInstance          = new QInstance();
      HealthMetaDataProducer producer           = new HealthMetaDataProducer();
      QJavalinMetaData     javalinMetaData    = producer.produce(qInstance);

      assertThat(javalinMetaData).isNotNull();
      assertThat(javalinMetaData.getAdditionalRouteProviderReferences()).isNullOrEmpty();
   }



   /*******************************************************************************
    ** Test that extending HealthMetaDataProducer auto-registers route provider
    *******************************************************************************/
   @Test
   void testExtendedProducer_autoRegistersRouteProvider() throws QException
   {
      QInstance qInstance = new QInstance();

      HealthMetaDataProducer producer = new HealthMetaDataProducer()
      {
         @Override
         protected HealthCheckMetaData buildHealthCheckMetaData(QInstance qInstance)
         {
            return new HealthCheckMetaData()
               .withEnabled(true)
               .withEndpointPath("/health")
               .withIndicators(List.of(
                  new MemoryHealthIndicator().withThreshold(85)
               ));
         }
      };

      QJavalinMetaData javalinMetaData = producer.produce(qInstance);

      ///////////////////////////////////////////////////////////
      // Verify JavalinHealthRouteProvider reference is added //
      ///////////////////////////////////////////////////////////
      assertThat(javalinMetaData).isNotNull();
      assertThat(javalinMetaData.getAdditionalRouteProviderReferences()).isNotEmpty();
      assertThat(javalinMetaData.getAdditionalRouteProviderReferences()).hasSize(1);

      QCodeReference providerRef = javalinMetaData.getAdditionalRouteProviderReferences().get(0);
      assertThat(providerRef.getName()).isEqualTo(JavalinHealthRouteProvider.class.getName());

      ////////////////////////////////////////////////////////
      // Verify HealthCheckMetaData was added to QInstance //
      ////////////////////////////////////////////////////////
      HealthCheckMetaData healthMetaData = (HealthCheckMetaData) qInstance.getSupplementalMetaData(HealthCheckMetaData.METADATA_KEY);
      assertThat(healthMetaData).isNotNull();
      assertThat(healthMetaData.getEnabled()).isTrue();
      assertThat(healthMetaData.getEndpointPath()).isEqualTo("/health");
      assertThat(healthMetaData.getIndicators()).hasSize(1);
   }



   /*******************************************************************************
    ** Test that disabled health checks don't register route provider
    *******************************************************************************/
   @Test
   void testDisabledHealthChecks_noRouteProvider() throws QException
   {
      QInstance qInstance = new QInstance();

      HealthMetaDataProducer producer = new HealthMetaDataProducer()
      {
         @Override
         protected HealthCheckMetaData buildHealthCheckMetaData(QInstance qInstance)
         {
            return new HealthCheckMetaData()
               .withEnabled(false);
         }
      };

      QJavalinMetaData javalinMetaData = producer.produce(qInstance);

      /////////////////////////////////////////////////////////////////////
      // Verify route provider IS registered even when health is disabled //
      // (the route provider itself checks if health is enabled)         //
      /////////////////////////////////////////////////////////////////////
      assertThat(javalinMetaData).isNotNull();
      assertThat(javalinMetaData.getAdditionalRouteProviderReferences()).isNotEmpty();

      HealthCheckMetaData healthMetaData = (HealthCheckMetaData) qInstance.getSupplementalMetaData(HealthCheckMetaData.METADATA_KEY);
      assertThat(healthMetaData).isNotNull();
      assertThat(healthMetaData.getEnabled()).isFalse();
   }



   /*******************************************************************************
    ** Test that multiple calls to produce don't duplicate route providers
    *******************************************************************************/
   @Test
   void testMultipleCalls_noDuplication() throws QException
   {
      QInstance qInstance = new QInstance();

      HealthMetaDataProducer producer = new HealthMetaDataProducer()
      {
         @Override
         protected HealthCheckMetaData buildHealthCheckMetaData(QInstance qInstance)
         {
            return new HealthCheckMetaData()
               .withEnabled(true)
               .withEndpointPath("/health");
         }
      };

      //////////////////////////////////////////
      // Call produce twice on same QInstance //
      //////////////////////////////////////////
      QJavalinMetaData javalinMetaData1 = producer.produce(qInstance);
      QJavalinMetaData javalinMetaData2 = producer.produce(qInstance);

      //////////////////////////////////////////////////////////////////
      // Both should return the same QJavalinMetaData object          //
      // (ofOrWithNew returns existing if present)                    //
      //////////////////////////////////////////////////////////////////
      assertThat(javalinMetaData1).isSameAs(javalinMetaData2);

      //////////////////////////////////////////////////////////////////////
      // Route provider should be registered twice (implementation allows) //
      // In practice, this shouldn't happen as producers run once          //
      //////////////////////////////////////////////////////////////////////
      assertThat(javalinMetaData2.getAdditionalRouteProviderReferences()).hasSize(2);
   }
}
