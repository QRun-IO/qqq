/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.esb.management;


import java.util.Optional;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.esb.EsbTestBase;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.EsbProviderType;
import com.kingsrook.qqq.esb.model.QEsbProviderMetaData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit test for EsbBrokerAdapters.
 *******************************************************************************/
class EsbBrokerAdaptersTest extends EsbTestBase
{

   /*******************************************************************************
    ** A provider without a managementUrl has no adapter.
    *******************************************************************************/
   @Test
   void testNoManagementUrlHasNoAdapter()
   {
      assertThat(EsbBrokerAdapters.forProvider(PROVIDER_NAME)).isEmpty();

      addProvider("blankManagementUrl", EsbProviderType.ACTIVEMQ_ARTEMIS, "  ");
      assertThat(EsbBrokerAdapters.forProvider("blankManagementUrl")).isEmpty();
   }



   /*******************************************************************************
    ** Each provider type with a managementUrl gets its broker's adapter.
    *******************************************************************************/
   @Test
   void testAdapterPerProviderType()
   {
      addProvider("artemisManaged", EsbProviderType.ACTIVEMQ_ARTEMIS, "http://localhost:8161");
      addProvider("rabbitManaged", EsbProviderType.RABBITMQ, "http://localhost:15672");

      Optional<EsbBrokerAdapter> artemisAdapter = EsbBrokerAdapters.forProvider("artemisManaged");
      assertThat(artemisAdapter).containsInstanceOf(ArtemisJolokiaAdapter.class);
      assertThat(artemisAdapter.orElseThrow().capabilities()).isEqualTo(ArtemisJolokiaAdapter.CAPABILITIES);

      Optional<EsbBrokerAdapter> rabbitAdapter = EsbBrokerAdapters.forProvider("rabbitManaged");
      assertThat(rabbitAdapter).containsInstanceOf(RabbitManagementAdapter.class);
      assertThat(rabbitAdapter.orElseThrow().capabilities()).isEqualTo(RabbitManagementAdapter.CAPABILITIES);
   }



   /*******************************************************************************
    ** An unknown provider, a provider without a type, or no instance in context:
    ** no adapter.
    *******************************************************************************/
   @Test
   void testNoAdapterWithoutProvider()
   {
      assertThat(EsbBrokerAdapters.forProvider("noSuchProvider")).isEmpty();
      assertThat(EsbBrokerAdapters.forProvider(null)).isEmpty();

      addProvider("untyped", null, "http://localhost:8161");
      assertThat(EsbBrokerAdapters.forProvider("untyped")).isEmpty();

      QContext.clear();
      assertThat(EsbBrokerAdapters.forProvider("artemisManaged")).isEmpty();
   }



   /*******************************************************************************
    ** Browsing is plain JMS, so a provider without a management API can still
    ** browse - but do nothing else.
    *******************************************************************************/
   @Test
   void testCapabilitiesWithoutManagement()
   {
      EsbBrokerCapabilities capabilities = EsbBrokerCapabilities.WITHOUT_MANAGEMENT;
      assertThat(capabilities.browse()).isTrue();
      assertThat(capabilities.queueInfo()).isFalse();
      assertThat(capabilities.pauseQueue()).isFalse();
      assertThat(capabilities.purge()).isFalse();
      assertThat(capabilities.deleteSelected()).isFalse();
      assertThat(capabilities.deleteOlderThan()).isFalse();
      assertThat(capabilities.move()).isFalse();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void addProvider(String name, EsbProviderType type, String managementUrl)
   {
      EsbInstanceMetaData.of(QContext.getQInstance()).withProvider(new QEsbProviderMetaData()
         .withName(name)
         .withType(type)
         .withUrl("tcp://localhost:61616")
         .withManagementUrl(managementUrl));
   }

}
