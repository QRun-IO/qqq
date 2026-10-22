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
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.assist.BulkLoadAssistModels;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.executors.io.BulkLoadSuggestMappingOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.BulkLoadColumnRef;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.BulkLoadFieldRef;
import org.json.JSONObject;


/*******************************************************************************
 ** A suggested mapping for an upload's header row.
 *******************************************************************************/
public class BulkLoadSuggestMappingResponseV1 implements BulkLoadSuggestMappingOutputInterface, ToSchema
{
   @OpenAPIDescription("The suggested bulk load profile (v1: version, layout, hasHeaderRow, isBulkEdit, and fieldList entries of fieldName, columnIndex, headerName, defaultValue, doValueMapping, valueMappings, clearIfEmpty).")
   private Map<String, Object> profile;

   @OpenAPIDescription("File columns the suggestion does not map.")
   @OpenAPIListItems(value = BulkLoadColumnRef.class, useRef = true)
   private List<BulkLoadColumnRef> unmappedColumns;

   @OpenAPIDescription("Required table fields the suggestion does not fill.")
   @OpenAPIListItems(value = BulkLoadFieldRef.class, useRef = true)
   private List<BulkLoadFieldRef> unmappedRequiredFields;

   @OpenAPIDescription("Mapped fields with a possible value source, whose file values may need mapping.")
   @OpenAPIListItems(value = BulkLoadFieldRef.class, useRef = true)
   private List<BulkLoadFieldRef> valueMappingFields;



   /*******************************************************************************
    ** Getter for profile
    *******************************************************************************/
   public Map<String, Object> getProfile()
   {
      return (this.profile);
   }



   /*******************************************************************************
    ** Fluent setter for profile
    *******************************************************************************/
   public BulkLoadSuggestMappingResponseV1 withProfile(Map<String, Object> profile)
   {
      this.profile = profile;
      return (this);
   }



   /*******************************************************************************
    ** Getter for unmappedColumns
    *******************************************************************************/
   public List<BulkLoadColumnRef> getUnmappedColumns()
   {
      return (this.unmappedColumns);
   }



   /*******************************************************************************
    ** Fluent setter for unmappedColumns
    *******************************************************************************/
   public BulkLoadSuggestMappingResponseV1 withUnmappedColumns(List<BulkLoadColumnRef> unmappedColumns)
   {
      this.unmappedColumns = unmappedColumns;
      return (this);
   }



   /*******************************************************************************
    ** Getter for unmappedRequiredFields
    *******************************************************************************/
   public List<BulkLoadFieldRef> getUnmappedRequiredFields()
   {
      return (this.unmappedRequiredFields);
   }



   /*******************************************************************************
    ** Fluent setter for unmappedRequiredFields
    *******************************************************************************/
   public BulkLoadSuggestMappingResponseV1 withUnmappedRequiredFields(List<BulkLoadFieldRef> unmappedRequiredFields)
   {
      this.unmappedRequiredFields = unmappedRequiredFields;
      return (this);
   }



   /*******************************************************************************
    ** Getter for valueMappingFields
    *******************************************************************************/
   public List<BulkLoadFieldRef> getValueMappingFields()
   {
      return (this.valueMappingFields);
   }



   /*******************************************************************************
    ** Fluent setter for valueMappingFields
    *******************************************************************************/
   public BulkLoadSuggestMappingResponseV1 withValueMappingFields(List<BulkLoadFieldRef> valueMappingFields)
   {
      this.valueMappingFields = valueMappingFields;
      return (this);
   }



   /*******************************************************************************
    ** Fill from core's result.
    *******************************************************************************/
   @Override
   public void setResult(BulkLoadAssistModels.Suggestion result) throws QException
   {
      this.profile = new JSONObject(JsonUtils.toJson(result.profile())).toMap();
      this.unmappedColumns = new ArrayList<>();
      result.unmappedColumns().forEach(c -> this.unmappedColumns.add(new BulkLoadColumnRef().withColumnIndex(c.columnIndex()).withHeader(c.header())));
      this.unmappedRequiredFields = new ArrayList<>();
      result.unmappedRequiredFields().forEach(f -> this.unmappedRequiredFields.add(new BulkLoadFieldRef().withFieldName(f.fieldName()).withLabel(f.label()).withPossibleValueSourceName(f.possibleValueSourceName())));
      this.valueMappingFields = new ArrayList<>();
      result.valueMappingFields().forEach(f -> this.valueMappingFields.add(new BulkLoadFieldRef().withFieldName(f.fieldName()).withLabel(f.label()).withPossibleValueSourceName(f.possibleValueSourceName())));
   }

}
