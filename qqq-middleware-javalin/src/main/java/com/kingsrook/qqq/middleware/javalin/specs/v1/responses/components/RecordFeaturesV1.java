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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components;


import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.FrontendRecordFeatures;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeature;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordOwnership;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIMapValueType;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;


/*******************************************************************************
 **
 *******************************************************************************/
public class RecordFeaturesV1 implements ToSchema
{
   @com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude()
   private FrontendRecordFeatures wrapped;



   /*******************************************************************************
    **
    *******************************************************************************/
   public RecordFeaturesV1()
   {

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public RecordFeaturesV1(FrontendRecordFeatures wrapped)
   {
      this.wrapped = wrapped;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @OpenAPIListItems(RecordFeature.class)
   @OpenAPIDescription("Enabled features.")
   public List<RecordFeature> getFeatures()
   {
      return (wrapped.getFeatures());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @OpenAPIMapValueType(Map.class)
   @OpenAPIDescription("Public settings keyed by feature and setting name.")
   public Map<RecordFeature, Map<String, Serializable>> getSettings()
   {
      return (wrapped.getSettings());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @OpenAPIMapValueType(Boolean.class)
   @OpenAPIDescription("Current session capabilities keyed by permission suffix.")
   public Map<String, Boolean> getCapabilities()
   {
      return (wrapped.getCapabilities());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public Schema toSchema()
   {
      Map<String, Schema> properties = new TreeMap<>();
      properties.put("features", new Schema().withType(Type.ARRAY).withItems(new Schema().withType(Type.STRING).withEnumValues(Arrays.stream(RecordFeature.values()).map(Enum::name).toList())));
      properties.put("settings", new Schema().withType(Type.OBJECT).withAdditionalProperties(new Schema().withType(Type.OBJECT).withAdditionalProperties(true)));
      properties.put("ownership", new Schema().withType(Type.OBJECT).withProperties(new TreeMap<>(Map.of(
         "ownerFieldName", new Schema().withType(Type.STRING),
         "assigneeFieldName", new Schema().withType(Type.STRING)))));
      properties.put("capabilities", new Schema().withType(Type.OBJECT).withAdditionalProperties(new Schema().withType(Type.BOOLEAN)));
      return (new Schema().withType(Type.OBJECT).withProperties(properties));
   }



   /*******************************************************************************
    ** Declared user-id fields for ownership and assignment.
    *******************************************************************************/
   public RecordOwnership getOwnership()
   {
      return (wrapped.getOwnership());
   }
}
