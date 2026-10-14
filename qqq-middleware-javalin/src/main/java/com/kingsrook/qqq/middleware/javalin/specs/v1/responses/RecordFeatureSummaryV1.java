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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeature;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeatureSummary;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableRecordFeaturesOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;


/*******************************************************************************
 **
 *******************************************************************************/
public class RecordFeatureSummaryV1 implements TableRecordFeaturesOutputInterface, ToSchema
{
   private RecordFeatureSummary summary = new RecordFeatureSummary();



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setSummary(RecordFeatureSummary summary)
   {
      this.summary = summary;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String getTableName()
   {
      return (summary.getTableName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String getPrimaryKey()
   {
      return (summary.getPrimaryKey());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public List<RecordFeature> getFeatures()
   {
      return (summary.getFeatures());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public Map<String, Boolean> getCapabilities()
   {
      return (summary.getCapabilities());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public Boolean getIsFavorite()
   {
      return (summary.getIsFavorite());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public Boolean getIsFollowing()
   {
      return (summary.getIsFollowing());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public Map<String, Integer> getCounts()
   {
      return (summary.getCounts());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String getOwner()
   {
      return (summary.getOwner());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String getAssignee()
   {
      return (summary.getAssignee());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public List<Map<String, Serializable>> getPresence()
   {
      return (summary.getPresence());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public Map<String, Serializable> getExtensions()
   {
      return (summary.getExtensions());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public Schema toSchema()
   {
      Map<String, Schema> properties = new LinkedHashMap<>();
      properties.put("tableName", new Schema().withType(Type.STRING));
      properties.put("primaryKey", new Schema().withType(Type.STRING));
      properties.put("features", new Schema().withType(Type.ARRAY).withItems(new Schema().withType(Type.STRING).withEnumValues(Arrays.stream(RecordFeature.values()).map(Enum::name).toList())));
      properties.put("capabilities", new Schema().withType(Type.OBJECT).withAdditionalProperties(new Schema().withType(Type.BOOLEAN)));
      properties.put("isFavorite", new Schema().withType(Type.BOOLEAN).withNullable(true));
      properties.put("isFollowing", new Schema().withType(Type.BOOLEAN).withNullable(true));
      properties.put("counts", new Schema().withType(Type.OBJECT).withAdditionalProperties(new Schema().withType(Type.INTEGER)));
      properties.put("owner", new Schema().withType(Type.STRING).withNullable(true));
      properties.put("assignee", new Schema().withType(Type.STRING).withNullable(true));
      properties.put("presence", new Schema().withType(Type.ARRAY).withItems(new Schema().withType(Type.OBJECT).withAdditionalProperties(true)).withNullable(true));
      properties.put("extensions", new Schema().withType(Type.OBJECT).withAdditionalProperties(new Schema().withType(Type.OBJECT).withAdditionalProperties(true)));
      return (new Schema().withType(Type.OBJECT).withProperties(properties));
   }

}
