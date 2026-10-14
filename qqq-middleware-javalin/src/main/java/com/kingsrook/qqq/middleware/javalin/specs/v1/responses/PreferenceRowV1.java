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
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceRow;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;


/*******************************************************************************
 ** PreferenceRowV1 in the user preferences API.
 *******************************************************************************/
public class PreferenceRowV1 implements ToSchema
{
   private String key;
   private String label;
   private String description;
   private Map<String, Serializable> values;
   private List<String> readOnlyFields;
   private String readOnlyReason;



   /*******************************************************************************
    ** A fixed provider-owned row. Clients cannot add or remove rows.
    *******************************************************************************/
   @Override
   public Schema toSchema()
   {
      return (new Schema().withType(Type.OBJECT).withDescription("A fixed provider-owned row. Clients cannot add or remove rows.")
         .withProperty("key", new Schema().withType(Type.STRING).withDescription("Stable provider row key to send when patching."))
         .withProperty("label", new Schema().withType(Type.STRING).withDescription("Human-readable row label."))
         .withProperty("description", new Schema().withType(Type.STRING).withDescription("Explanation of this row."))
         .withProperty("values", new Schema().withType(Type.OBJECT).withDescription("Field name to typed value. Omitted fields retain their existing values; explicit null clears an optional field.").withAdditionalProperties(new Schema()))
         .withProperty("readOnlyFields", new Schema().withType(Type.ARRAY).withDescription("Field names that cannot be changed for this row.").withItems(new Schema().withType(Type.STRING)))
         .withProperty("readOnlyReason", new Schema().withType(Type.STRING).withDescription("Explain why the indicated fields are read-only."))
         );
   }



   /*******************************************************************************
    ** Map a provider row into the wire contract.
    *******************************************************************************/
   public static PreferenceRowV1 from(UserPreferenceRow row)
   {
      return (new PreferenceRowV1().withKey(row.getKey()).withLabel(row.getLabel()).withDescription(row.getDescription())
         .withValues(row.getValues()).withReadOnlyFields(row.getReadOnlyFieldNames()).withReadOnlyReason(row.getReadOnlyReason()));
   }



   /*******************************************************************************
    ** Get key.
    *******************************************************************************/
   public String getKey()
   {
      return (key);
   }



   /*******************************************************************************
    ** Set key.
    *******************************************************************************/
   public void setKey(String key)
   {
      this.key = key;
   }



   /*******************************************************************************
    ** With key.
    *******************************************************************************/
   public PreferenceRowV1 withKey(String key)
   {
      this.key = key;
      return (this);
   }



   /*******************************************************************************
    ** Get label.
    *******************************************************************************/
   public String getLabel()
   {
      return (label);
   }



   /*******************************************************************************
    ** Set label.
    *******************************************************************************/
   public void setLabel(String label)
   {
      this.label = label;
   }



   /*******************************************************************************
    ** With label.
    *******************************************************************************/
   public PreferenceRowV1 withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Get description.
    *******************************************************************************/
   public String getDescription()
   {
      return (description);
   }



   /*******************************************************************************
    ** Set description.
    *******************************************************************************/
   public void setDescription(String description)
   {
      this.description = description;
   }



   /*******************************************************************************
    ** With description.
    *******************************************************************************/
   public PreferenceRowV1 withDescription(String description)
   {
      this.description = description;
      return (this);
   }



   /*******************************************************************************
    ** Get values.
    *******************************************************************************/
   public Map<String, Serializable> getValues()
   {
      return (values);
   }



   /*******************************************************************************
    ** Set values.
    *******************************************************************************/
   public void setValues(Map<String, Serializable> values)
   {
      this.values = values;
   }



   /*******************************************************************************
    ** With values.
    *******************************************************************************/
   public PreferenceRowV1 withValues(Map<String, Serializable> values)
   {
      this.values = values;
      return (this);
   }



   /*******************************************************************************
    ** Get readOnlyFields.
    *******************************************************************************/
   public List<String> getReadOnlyFields()
   {
      return (readOnlyFields);
   }



   /*******************************************************************************
    ** Set readOnlyFields.
    *******************************************************************************/
   public void setReadOnlyFields(List<String> readOnlyFields)
   {
      this.readOnlyFields = readOnlyFields;
   }



   /*******************************************************************************
    ** With readOnlyFields.
    *******************************************************************************/
   public PreferenceRowV1 withReadOnlyFields(List<String> readOnlyFields)
   {
      this.readOnlyFields = readOnlyFields;
      return (this);
   }



   /*******************************************************************************
    ** Get readOnlyReason.
    *******************************************************************************/
   public String getReadOnlyReason()
   {
      return (readOnlyReason);
   }



   /*******************************************************************************
    ** Set readOnlyReason.
    *******************************************************************************/
   public void setReadOnlyReason(String readOnlyReason)
   {
      this.readOnlyReason = readOnlyReason;
   }



   /*******************************************************************************
    ** With readOnlyReason.
    *******************************************************************************/
   public PreferenceRowV1 withReadOnlyReason(String readOnlyReason)
   {
      this.readOnlyReason = readOnlyReason;
      return (this);
   }
}
