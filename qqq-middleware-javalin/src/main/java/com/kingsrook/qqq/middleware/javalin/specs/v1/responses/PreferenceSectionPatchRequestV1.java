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
 ** PreferenceSectionPatchRequestV1 in the user preferences API.
 *******************************************************************************/
public class PreferenceSectionPatchRequestV1 implements ToSchema
{
   private Map<String, Object> body = new LinkedHashMap<>();



   /*******************************************************************************
    ** Describe the partial update; raw properties are retained for strict core validation.
    *******************************************************************************/
   @Override
   public Schema toSchema()
   {
      Schema row = new Schema().withType(Type.OBJECT).withAdditionalProperties(false)
         .withProperty("key", new Schema().withType(Type.STRING).withDescription("Required fixed row key from the section response. No new rows can be created."))
         .withProperty("values", new Schema().withType(Type.OBJECT).withAdditionalProperties(new Schema()).withDescription("Changed row fields only. Read-only fields are rejected."));
      return (new Schema().withType(Type.OBJECT).withAdditionalProperties(false)
         .withDescription("Partial update for the signed-in user only. Unknown properties, fields, lists and rows, including userId, are rejected with 400. All validation errors are returned together; omitted fields and rows are unchanged.")
         .withProperty("values", new Schema().withType(Type.OBJECT).withAdditionalProperties(new Schema()).withDescription("Changed scalar fields only. Explicit null clears an optional field. Types, required, maxLength, ENUM membership and editability are validated."))
         .withProperty("lists", new Schema().withType(Type.OBJECT).withDescription("List name to changed fixed rows. No add or remove operation is supported.")
            .withAdditionalProperties(new Schema().withType(Type.ARRAY).withItems(row))));
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
   public PreferenceSectionPatchRequestV1 withBody(Map<String, Object> body)
   {
      this.body = body;
      return (this);
   }
}
