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


import java.io.Serializable;
import java.util.Map;
import com.kingsrook.qqq.middleware.javalin.executors.io.DeliveryOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;


/*******************************************************************************
 ** DeliveryTargetsRequestV1.
 *******************************************************************************/
public class DeliveryTargetsRequestV1 implements ToSchema
{
   private Map<String, Serializable> chosen;



   /*******************************************************************************
    ** Chosen contingent target values.
    *******************************************************************************/
   public Map<String, Serializable> getChosen()
   {
      return (chosen);
   }



   /*******************************************************************************
    ** Set chosen values.
    *******************************************************************************/
   public void setChosen(Map<String, Serializable> chosen)
   {
      this.chosen = chosen;
   }



   /*******************************************************************************
    ** Request contract.
    *******************************************************************************/
   @Override
   public Schema toSchema()
   {
      return (new Schema().withType(Type.OBJECT).withProperty("chosen", new Schema().withType(Type.OBJECT).withAdditionalProperties(true)));
   }
}
