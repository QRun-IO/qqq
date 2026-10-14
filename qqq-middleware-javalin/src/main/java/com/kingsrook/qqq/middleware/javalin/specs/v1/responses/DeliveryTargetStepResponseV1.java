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


import com.kingsrook.qqq.backend.core.delivery.DeliveryTargetStep;
import com.kingsrook.qqq.middleware.javalin.executors.io.DeliveryOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;


/*******************************************************************************
 ** DeliveryTargetStepResponseV1.
 *******************************************************************************/
public class DeliveryTargetStepResponseV1 extends DeliveryTargetStep implements DeliveryOutputInterface, ToSchema
{
   /*******************************************************************************
    ** Describe the contingent field and its user-scoped options.
    *******************************************************************************/
   @Override
   public Schema toSchema()
   {
      Schema option = new Schema().withType(Type.OBJECT)
         .withProperty("value", new Schema().withType(Type.STRING))
         .withProperty("label", new Schema().withType(Type.STRING))
         .withProperty("description", new Schema().withType(Type.STRING));
      return (new Schema().withType(Type.OBJECT)
         .withProperty("name", new Schema().withType(Type.STRING))
         .withProperty("label", new Schema().withType(Type.STRING))
         .withProperty("options", new Schema().withType(Type.ARRAY).withItems(option))
         .withProperty("freeText", new Schema().withType(Type.BOOLEAN))
         .withProperty("complete", new Schema().withType(Type.BOOLEAN))
         .withProperty("summary", new Schema().withType(Type.STRING)));
   }



   /*******************************************************************************
    ** Copy the destination response into the versioned contract.
    *******************************************************************************/
   @Override
   public void setStep(DeliveryTargetStep step)
   {
      setName(step.getName());
      setLabel(step.getLabel());
      setOptions(step.getOptions());
      setFreeText(step.getFreeText());
      setComplete(step.getComplete());
      setSummary(step.getSummary());
   }
}
