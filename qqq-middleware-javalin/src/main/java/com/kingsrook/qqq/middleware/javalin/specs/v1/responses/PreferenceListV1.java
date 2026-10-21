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


import java.util.List;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.FieldMetaData;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;


/*******************************************************************************
 ** PreferenceListV1 in the user preferences API.
 *******************************************************************************/
public class PreferenceListV1 implements ToSchema
{
   private String name;
   private String label;
   private String description;
   private String rowLabel;
   private List<FieldMetaData> fields;
   private List<PreferenceRowV1> rows;



   /*******************************************************************************
    ** A list of fixed preference rows, rendered with the same typed field editors as scalar preferences.
    *******************************************************************************/
   @Override
   public Schema toSchema()
   {
      return (new Schema().withType(Type.OBJECT).withDescription("A list of fixed preference rows, rendered with the same typed field editors as scalar preferences.")
         .withProperty("name", new Schema().withType(Type.STRING).withDescription("List name used in PATCH lists."))
         .withProperty("label", new Schema().withType(Type.STRING).withDescription("Human-readable list title."))
         .withProperty("description", new Schema().withType(Type.STRING).withDescription("Explanation of this list."))
         .withProperty("rowLabel", new Schema().withType(Type.STRING).withDescription("Label for one row, such as Category."))
         .withProperty("fields", new Schema().withType(Type.ARRAY).withDescription("Editable columns and their validation rules.").withItems(new Schema().withRefToSchema("FieldMetaData")))
         .withProperty("rows", new Schema().withType(Type.ARRAY).withDescription("Fixed rows available to this user.").withItems(new Schema().withRefToSchema("PreferenceRowV1")))
         );
   }



   /*******************************************************************************
    ** Get name.
    *******************************************************************************/
   public String getName()
   {
      return (name);
   }



   /*******************************************************************************
    ** Set name.
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** With name.
    *******************************************************************************/
   public PreferenceListV1 withName(String name)
   {
      this.name = name;
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
   public PreferenceListV1 withLabel(String label)
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
   public PreferenceListV1 withDescription(String description)
   {
      this.description = description;
      return (this);
   }



   /*******************************************************************************
    ** Get rowLabel.
    *******************************************************************************/
   public String getRowLabel()
   {
      return (rowLabel);
   }



   /*******************************************************************************
    ** Set rowLabel.
    *******************************************************************************/
   public void setRowLabel(String rowLabel)
   {
      this.rowLabel = rowLabel;
   }



   /*******************************************************************************
    ** With rowLabel.
    *******************************************************************************/
   public PreferenceListV1 withRowLabel(String rowLabel)
   {
      this.rowLabel = rowLabel;
      return (this);
   }



   /*******************************************************************************
    ** Get fields.
    *******************************************************************************/
   public List<FieldMetaData> getFields()
   {
      return (fields);
   }



   /*******************************************************************************
    ** Set fields.
    *******************************************************************************/
   public void setFields(List<FieldMetaData> fields)
   {
      this.fields = fields;
   }



   /*******************************************************************************
    ** With fields.
    *******************************************************************************/
   public PreferenceListV1 withFields(List<FieldMetaData> fields)
   {
      this.fields = fields;
      return (this);
   }



   /*******************************************************************************
    ** Get rows.
    *******************************************************************************/
   public List<PreferenceRowV1> getRows()
   {
      return (rows);
   }



   /*******************************************************************************
    ** Set rows.
    *******************************************************************************/
   public void setRows(List<PreferenceRowV1> rows)
   {
      this.rows = rows;
   }



   /*******************************************************************************
    ** With rows.
    *******************************************************************************/
   public PreferenceListV1 withRows(List<PreferenceRowV1> rows)
   {
      this.rows = rows;
      return (this);
   }
}
