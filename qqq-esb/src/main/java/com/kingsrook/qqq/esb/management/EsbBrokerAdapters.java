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
