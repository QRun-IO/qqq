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

package com.kingsrook.qqq.middleware.javalin.specs.v1.utils;


import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.PreferenceListV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.PreferenceRowV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.PreferenceSectionPatchRequestV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.PreferenceSectionV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.PreferencesResponseV1;
import com.kingsrook.qqq.openapi.model.Content;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.RequestBody;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.ContentType;
import io.javalin.http.Context;


/*******************************************************************************
 ** Shared contract and JSON handling for core preferences specs.
 *******************************************************************************/
public final class UserPreferencesSpecUtils
{
   /*******************************************************************************
    ** Static utilities only.
    *******************************************************************************/
   private UserPreferencesSpecUtils()
   {
   }



   /*******************************************************************************
    ** Section name comes only from the URL path.
    *******************************************************************************/
   public static List<Parameter> sectionParameters()
   {
      return (List.of(new Parameter().withName("sectionName").withIn(In.PATH).withRequired(true)
         .withDescription("Registered visible preference section name, from GET /preferences.").withSchema(new Schema().withType(Type.STRING))));
   }



   /*******************************************************************************
    ** Component schemas for generated clients.
    *******************************************************************************/
   public static Map<String, Schema> schemas()
   {
      return (Map.of(
         "PreferencesResponseV1", new PreferencesResponseV1().toSchema(),
         "PreferenceSectionV1", new PreferenceSectionV1().toSchema(),
         "PreferenceListV1", new PreferenceListV1().toSchema(),
         "PreferenceRowV1", new PreferenceRowV1().toSchema(),
         "PreferenceSectionPatchRequestV1", new PreferenceSectionPatchRequestV1().toSchema()));
   }



   /*******************************************************************************
    ** JSON body referencing the published request schema.
    *******************************************************************************/
   public static RequestBody requestBody()
   {
      return (new RequestBody().withRequired(true).withContent(Map.of(ContentType.APPLICATION_JSON.getMimeType(),
         new Content().withSchema(new Schema().withRefToSchema("PreferenceSectionPatchRequestV1")))));
   }



   /*******************************************************************************
    ** Retain every property for core validation, and reject malformed or non-object JSON.
    *******************************************************************************/
   public static PreferenceSectionPatchRequestV1 readRequest(Context context) throws QBadRequestException
   {
      try
      {
         Map<String, Object> body = JsonUtils.toObject(context.body(), new TypeReference<Map<String, Object>>() {});
         if(body == null)
         {
            throw (new IllegalArgumentException("Expected an object"));
         }
         return (new PreferenceSectionPatchRequestV1().withBody(body));
      }
      catch(Exception e)
      {
         throw (new QBadRequestException("Invalid JSON object body", e));
      }
   }



   /*******************************************************************************
    ** Keep explicit nulls in the wire representation.
    *******************************************************************************/
   public static void writeResponse(Context context, Object output)
   {
      context.contentType(ContentType.APPLICATION_JSON);
      context.result(JsonUtils.toJsonCustomized(output, builder -> builder.serializationInclusion(JsonInclude.Include.ALWAYS)));
   }
}
