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

package com.kingsrook.qqq.backend.core.health;


import java.time.Duration;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QSessionStoreProviderInterface;
import com.kingsrook.qqq.backend.core.modules.authentication.QSessionStoreRegistry;


/*******************************************************************************
 ** Round-trip a unique probe session without touching real user sessions.
 *******************************************************************************/
public class HealthSessionStoreCheck implements HealthCheck
{
   /*******************************************************************************
    ** Always remove the probe, including a failed load. TTL limits crash residue.
    *******************************************************************************/
   @Override
   public HealthCheckOutcome run(HealthCheckInput input)
   {
      QSessionStoreProviderInterface provider = QSessionStoreRegistry.getInstance().getProvider().orElse(null);
      if(provider == null)
      {
         return (HealthCheckOutcome.unknown("Session store not configured"));
      }
      String id = "health-probe-" + UUID.randomUUID();
      String marker = UUID.randomUUID().toString();
      try
      {
         provider.store(id, new QSession().withValue("healthProbe", marker), Duration.ofSeconds(60));
         return (provider.load(id).filter(session -> marker.equals(session.getValue("healthProbe"))).isPresent()
            ? HealthCheckOutcome.up("Session store round-trip succeeded") : HealthCheckOutcome.down("Session store round-trip failed"));
      }
      finally
      {
         provider.remove(id);
      }
   }
}
