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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.delivery.QDeliveryDestinationMetaData;
import com.kingsrook.qqq.middleware.javalin.executors.io.DeliveryOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;


/*******************************************************************************
 ** DeliveryDestinationsResponseV1.
 *******************************************************************************/
public class DeliveryDestinationsResponseV1 implements DeliveryOutputInterface, ToSchema
{
   private List<Map<String, Object>> destinations;



   /*******************************************************************************
    ** Publish the visible destination contract.
    *******************************************************************************/
   @Override
   public Schema toSchema()
   {
      Schema destination = new Schema().withType(Type.OBJECT);
      for(String name : List.of("name", "label", "icon", "maxClassification", "aboveMaxClassification"))
      {
         destination.withProperty(name, new Schema().withType(Type.STRING));
      }
      destination.withProperty("order", new Schema().withType(Type.INTEGER));
      destination.withProperty("maxAttachmentBytes", new Schema().withType(Type.INTEGER).withFormat("int64"));
      destination.withProperty("allowBearerLinks", new Schema().withType(Type.BOOLEAN));
      return (new Schema().withType(Type.OBJECT).withProperty("destinations", new Schema().withType(Type.ARRAY).withItems(destination)));
   }



   /*******************************************************************************
    ** Strip implementation and provider configuration from public metadata.
    *******************************************************************************/
   @Override
   public void setDestinations(List<QDeliveryDestinationMetaData> values)
   {
      destinations = new ArrayList<>();
      for(QDeliveryDestinationMetaData value : values)
      {
         Map<String, Object> destination = new LinkedHashMap<>();
         destination.put("name", value.getName());
         destination.put("label", value.getLabel());
         destination.put("icon", value.getIcon());
         destination.put("order", value.getOrder());
         destination.put("maxClassification", value.getMaxClassification());
         destination.put("maxAttachmentBytes", value.getMaxAttachmentBytes());
         destination.put("allowBearerLinks", value.getAllowBearerLinks());
         destination.put("aboveMaxClassification", value.getAboveMaxClassification());
         destinations.add(destination);
      }
   }



   /*******************************************************************************
    ** Get public destination descriptions.
    *******************************************************************************/
   public List<Map<String, Object>> getDestinations()
   {
      return (destinations);
   }
}
