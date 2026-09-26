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

package com.kingsrook.qqq.esb.api;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.middleware.javalin.QJavalinMetaData;


/*******************************************************************************
 * Registers EsbRouteProvider (the /qqq/v1/esb endpoints) with the Javalin
 * server, through the instance's QJavalinMetaData - as HealthMetaDataProducer
 * does for the health endpoints.  Only for Javalin applications: this class
 * needs qqq-middleware-javalin.
 *******************************************************************************/
public class EsbJavalinMetaDataProducer extends MetaDataProducer<QJavalinMetaData>
{

   /*******************************************************************************
    ** Add the route provider reference (once, however many times this runs).
    *******************************************************************************/
   @Override
   public QJavalinMetaData produce(QInstance qInstance) throws QException
   {
      QJavalinMetaData javalinMetaData = QJavalinMetaData.ofOrWithNew(qInstance);

      boolean registered = CollectionUtils.nonNullList(javalinMetaData.getAdditionalRouteProviderReferences()).stream()
         .anyMatch(codeReference -> codeReference != null && EsbRouteProvider.class.getName().equals(codeReference.getName()));
      if(!registered)
      {
         javalinMetaData.withAdditionalRouteProviderReference(new QCodeReference(EsbRouteProvider.class));
      }

      return (javalinMetaData);
   }

}
