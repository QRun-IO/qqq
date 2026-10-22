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

package com.kingsrook.sampleapp.metadata;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.middleware.javalin.QJavalinMetaData;
import com.kingsrook.qqq.middleware.javalin.metadata.JavalinRouteProviderMetaData;
import com.kingsrook.qqq.middleware.javalin.routeproviders.authentication.SimpleRouteAuthenticator;


/*******************************************************************************
 ** Meta Data Producer for SampleJavalin
 *******************************************************************************/
public class SampleJavalinMetaDataProducer extends MetaDataProducer<QJavalinMetaData>
{

   /*******************************************************************************
    ** todo wip - test sub-directories of each other
    ** todo wip - allow mat-dash to be served at a different path
    ** todo wip - get mat-dash committed
    *******************************************************************************/
   @Override
   public QJavalinMetaData produce(QInstance qInstance) throws QException
   {
      return (new QJavalinMetaData()
         .withRouteProvider(new JavalinRouteProviderMetaData()
            .withHostedPath("/public")
            .withFileSystemPath("site/public"))

         .withRouteProvider(new JavalinRouteProviderMetaData()
            .withRouteAuthenticator(new QCodeReference(SimpleRouteAuthenticator.class))
            .withHostedPath("/private")
            .withFileSystemPath("site/private"))

         .withRouteProvider(new JavalinRouteProviderMetaData()
            .withRouteAuthenticator(new QCodeReference(SimpleRouteAuthenticator.class))
            .withHostedPath("/dynamic-site/<pagePath>")
            .withProcessName(DynamicSiteProcessMetaDataProducer.NAME)));
   }

}
