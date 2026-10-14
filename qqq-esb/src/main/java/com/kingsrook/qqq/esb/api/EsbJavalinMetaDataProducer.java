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

package com.kingsrook.qqq.esb.api;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.esb.api.specs.EsbDeadLettersSpecV1;
import com.kingsrook.qqq.esb.api.specs.EsbMessagesSpecV1;
import com.kingsrook.qqq.esb.api.specs.EsbOverviewSpecV1;
import com.kingsrook.qqq.esb.api.specs.EsbProcessSpecV1;
import com.kingsrook.qqq.esb.api.specs.EsbTableSpecV1;
import com.kingsrook.qqq.middleware.javalin.specs.extensions.MiddlewareSpecExtension;
import com.kingsrook.qqq.middleware.javalin.specs.extensions.MiddlewareSpecExtensionsMetaData;


/*******************************************************************************
 * Registers the ESB endpoints (/qqq/v1/esb/...) as an extension of the UI API:
 * endpoint specs, served by the middleware version and described in its
 * OpenAPI document (QRun-IO/qqq docs/UI-API-EXTENSIONS.md).  The URLs are the
 * ones the route provider served before.  Only for Javalin applications: this
 * class needs qqq-middleware-javalin.
 *******************************************************************************/
public class EsbJavalinMetaDataProducer extends MetaDataProducer<MiddlewareSpecExtensionsMetaData>
{
   public static final String EXTENSION_NAME    = "esb";
   public static final String EXTENSION_VERSION = "1.0.0";



   /*******************************************************************************
    ** The ESB extension of the UI API.
    *******************************************************************************/
   public static MiddlewareSpecExtension defineExtension()
   {
      return (new MiddlewareSpecExtension()
         .withName(EXTENSION_NAME)
         .withPathPrefix("/esb")
         .withSemanticVersion(EXTENSION_VERSION)
         .withDescription("ESB overview, the ESB of tables and processes, and messages and dead letters on destinations.")
         .withSpec(EsbOverviewSpecV1.class)
         .withSpec(EsbTableSpecV1.class)
         .withSpec(EsbProcessSpecV1.class)
         .withSpec(EsbDeadLettersSpecV1.class)
         .withSpec(EsbMessagesSpecV1.class));
   }



   /*******************************************************************************
    ** Register the extension (replacing it, however many times this runs).
    *******************************************************************************/
   @Override
   public MiddlewareSpecExtensionsMetaData produce(QInstance qInstance) throws QException
   {
      return (MiddlewareSpecExtensionsMetaData.ofOrWithNew(qInstance).withExtension(defineExtension()));
   }

}
