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


import java.util.LinkedHashMap;
import java.util.Map;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;


/*******************************************************************************
 ** PreferenceActionRequestV1 in the user preferences API.
 *******************************************************************************/
public class PreferenceActionRequestV1 implements ToSchema
{
   private Map<String, Object> body = new LinkedHashMap<>();



   /*******************************************************************************
    ** Describe the partial update; raw properties are retained for strict core validation.
    *******************************************************************************/
   @Override
   public Schema toSchema()
   {
      return (new Schema().withType(Type.OBJECT).withAdditionalProperties(false)
         .withDescription("Only rowKey and values are accepted. Identity always comes from the authenticated session.")
         .withProperty("rowKey", new Schema().withType(Type.STRING).withDescription("Required for ROW actions: a key loaded for this user with the action allowed. Forbidden for SECTION and LIST actions."))
         .withProperty("values", new Schema().withType(Type.OBJECT).withAdditionalProperties(new Schema()).withDescription("Action input fields. Validated and coerced like PATCH fields; unknown fields are rejected.")));
   }



   /*******************************************************************************
    ** Get body.
    *******************************************************************************/
   public Map<String, Object> getBody()
   {
      return (body);
   }



   /*******************************************************************************
    ** Set body.
    *******************************************************************************/
   public void setBody(Map<String, Object> body)
   {
      this.body = body;
   }



   /*******************************************************************************
    ** With body.
    *******************************************************************************/
   public PreferenceActionRequestV1 withBody(Map<String, Object> body)
   {
      this.body = body;
      return (this);
   }
}
