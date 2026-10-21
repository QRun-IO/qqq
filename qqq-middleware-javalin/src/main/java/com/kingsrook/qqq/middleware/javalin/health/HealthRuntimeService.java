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

package com.kingsrook.qqq.middleware.javalin.health;


import java.time.Clock;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.health.HealthMonitoringMetaData;
import com.kingsrook.qqq.backend.core.health.HealthRunner;
import com.kingsrook.qqq.backend.core.instances.QRuntimeServiceInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;


/*******************************************************************************
 ** Own the health runner for the lifetime of its server or application launcher.
 *******************************************************************************/
public class HealthRuntimeService implements QRuntimeServiceInterface
{
   private HealthRunner runner;



   /*******************************************************************************
    ** Runtime identity.
    *******************************************************************************/
   @Override
   public String getName()
   {
      return ("healthRuntime");
   }



   /*******************************************************************************
    ** Start once; another launcher may already own this instance's runner.
    *******************************************************************************/
   @Override
   public synchronized void start(QInstance instance) throws QException
   {
      HealthMonitoringMetaData settings = QSupplementalInstanceMetaData.of(instance, HealthMonitoringMetaData.NAME);
      if(settings == null || !settings.getRunnerEnabled() || HealthRunner.getForInstance(instance) != null)
      {
         return;
      }
      String hostname = System.getenv("HOSTNAME");
      runner = new HealthRunner(instance, hostname == null || hostname.isBlank() ? UUID.randomUUID().toString() : hostname, Clock.systemUTC());
      runner.start();
   }



   /*******************************************************************************
    ** Stop only the runner this service started.
    *******************************************************************************/
   @Override
   public synchronized void stop()
   {
      if(runner != null)
      {
         runner.stop();
         runner = null;
      }
   }
}
