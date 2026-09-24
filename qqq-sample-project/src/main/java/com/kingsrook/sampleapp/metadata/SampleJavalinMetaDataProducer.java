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
