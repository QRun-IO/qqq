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

package com.kingsrook.sampleapp.healthfixture;


import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.middleware.health.HealthMetaDataProducer;
import com.kingsrook.qqq.middleware.health.indicators.BasicAliveHealthIndicator;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthCheckMetaData;


/*******************************************************************************
 ** Scanned only by health acceptance; never changes the normal sample routes.
 *******************************************************************************/
public class SampleHealthMetaDataProducer extends HealthMetaDataProducer
{
   /*******************************************************************************
    ** Exercise the documented producer extension and default endpoint path.
    *******************************************************************************/
   @Override
   protected HealthCheckMetaData buildHealthCheckMetaData(QInstance instance)
   {
      return new HealthCheckMetaData().withIndicators(List.of(new BasicAliveHealthIndicator()));
   }
}
