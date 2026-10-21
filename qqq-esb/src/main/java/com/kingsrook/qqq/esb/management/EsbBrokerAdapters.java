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

package com.kingsrook.qqq.esb.management;


import java.util.Optional;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.QEsbProviderMetaData;


/*******************************************************************************
 * Picks the broker adapter for an ESB provider.
 *******************************************************************************/
public final class EsbBrokerAdapters
{

   /*******************************************************************************
    ** Utility class - static methods only.
    *******************************************************************************/
   private EsbBrokerAdapters()
   {
   }



   /*******************************************************************************
    ** The broker adapter for a provider in the QContext's instance - Artemis
    ** (Jolokia) or RabbitMQ (management API), per its type, using its
    ** managementUrl and management credentials.
    **
    ** Empty if the provider has no managementUrl (it then can only browse - see
    ** EsbBrokerCapabilities.WITHOUT_MANAGEMENT), or if there's no such provider
    ** (or no type on it, or no instance in context).
    **
    ** Adapters are cheap (they share one HTTP client), and read the provider's
    ** meta-data when made - so get one when needed, rather than keep it.
    *******************************************************************************/
   public static Optional<EsbBrokerAdapter> forProvider(String providerName)
   {
      QInstance qInstance = QContext.getQInstance();
      if(qInstance == null || providerName == null)
      {
         return (Optional.empty());
      }

      EsbInstanceMetaData  esbInstanceMetaData = QSupplementalInstanceMetaData.of(qInstance, EsbInstanceMetaData.NAME);
      QEsbProviderMetaData provider            = esbInstanceMetaData == null ? null : esbInstanceMetaData.getProvider(providerName);
      if(provider == null || provider.getType() == null || !StringUtils.hasContent(provider.getManagementUrl()))
      {
         return (Optional.empty());
      }

      return (Optional.of(switch(provider.getType())
      {
         case ACTIVEMQ_ARTEMIS -> new ArtemisJolokiaAdapter(provider);
         case RABBITMQ -> new RabbitManagementAdapter(provider);
      }));
   }

}
