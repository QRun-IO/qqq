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
