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


import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.middleware.javalin.specs.extensions.MiddlewareSpecExtension;
import com.kingsrook.qqq.middleware.javalin.specs.extensions.MiddlewareSpecExtensionsMetaData;


/*******************************************************************************
 ** Core-owned, independently versioned health UI API extension.
 *******************************************************************************/
public class HealthJavalinMetaDataProducer extends MetaDataProducer<MiddlewareSpecExtensionsMetaData>
{
   /*******************************************************************************
    ** Extension contract, also used by the committed OpenAPI test.
    *******************************************************************************/
   public static MiddlewareSpecExtension defineExtension()
   {
      return (new MiddlewareSpecExtension().withName("health").withPathPrefix("/health").withSemanticVersion("1.0.0")
         .withDescription("Central health summary, components, history and on-demand checks.")
         .withSpec(HealthSummarySpecV1.class).withSpec(HealthComponentSpecV1.class)
         .withSpec(HealthHistorySpecV1.class).withSpec(HealthRunSpecV1.class));
   }



   /*******************************************************************************
    ** Idempotent registration on opted-in instances.
    *******************************************************************************/
   @Override
   public MiddlewareSpecExtensionsMetaData produce(QInstance instance)
   {
      return (MiddlewareSpecExtensionsMetaData.ofOrWithNew(instance).withExtension(defineExtension()));
   }
}
