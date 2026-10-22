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
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceActionMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceActionScope;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.FieldMetaData;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.Icon;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;


/*******************************************************************************
 ** PreferenceActionV1 in the user preferences API.
 *******************************************************************************/
public class PreferenceActionV1 implements ToSchema
{
   private String name;
   private String label;
   private Icon icon;
   private String description;
   private UserPreferenceActionScope scope;
   private String listName;
   private String confirmationText;
   private boolean isDestructive;
   private List<FieldMetaData> inputFields;
   private int order;



   /*******************************************************************************
    ** Describe the wire contract.
    *******************************************************************************/
   @Override
   public Schema toSchema()
   {
      return (new Schema().withType(Type.OBJECT)
         .withProperty("name", new Schema().withType(Type.STRING).withDescription("Stable action name used in URLs."))
         .withProperty("label", new Schema().withType(Type.STRING).withDescription("Human-readable action label."))
         .withProperty("icon", new Schema().withRefToSchema("Icon"))
         .withProperty("description", new Schema().withType(Type.STRING).withDescription("Explanation for clients to display."))
         .withProperty("scope", new Schema().withType(Type.STRING).withEnumValues(List.of("SECTION", "LIST", "ROW")).withDescription("Where to display this action. ROW actions require a loaded row key."))
         .withProperty("listName", new Schema().withType(Type.STRING).withNullable(true).withDescription("List targeted by LIST or ROW actions; null for SECTION."))
         .withProperty("confirmationText", new Schema().withType(Type.STRING).withNullable(true).withDescription("When set, clients must confirm with this text before running the action."))
         .withProperty("isDestructive", new Schema().withType(Type.BOOLEAN).withDescription("Whether clients should present this as a destructive action."))
         .withProperty("inputFields", new Schema().withType(Type.ARRAY).withItems(new Schema().withRefToSchema("FieldMetaData")).withDescription("Action inputs, using preference field validation and coercion."))
         .withProperty("order", new Schema().withType(Type.INTEGER).withDescription("Display order, ascending then label; defaults to 100.")));
   }



   /*******************************************************************************
    ** Map registered action metadata to its public representation.
    *******************************************************************************/
   public static PreferenceActionV1 from(UserPreferenceActionMetaData action)
   {
      return (new PreferenceActionV1().withName(action.getName()).withLabel(action.getLabel())
         .withIcon(action.getIcon() == null ? null : new Icon(action.getIcon())).withDescription(action.getDescription())
         .withScope(action.getScope()).withListName(action.getListName()).withConfirmationText(action.getConfirmationText())
         .withIsDestructive(action.getIsDestructive()).withInputFields(action.getInputFields().stream().map(FieldMetaData::new).toList()).withOrder(action.getOrder()));
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
   public PreferenceActionV1 withName(String name)
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
   public PreferenceActionV1 withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Get icon.
    *******************************************************************************/
   public Icon getIcon()
   {
      return (icon);
   }



   /*******************************************************************************
    ** Set icon.
    *******************************************************************************/
   public void setIcon(Icon icon)
   {
      this.icon = icon;
   }



   /*******************************************************************************
    ** With icon.
    *******************************************************************************/
   public PreferenceActionV1 withIcon(Icon icon)
   {
      this.icon = icon;
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
   public PreferenceActionV1 withDescription(String description)
   {
      this.description = description;
      return (this);
   }



   /*******************************************************************************
    ** Get scope.
    *******************************************************************************/
   public UserPreferenceActionScope getScope()
   {
      return (scope);
   }



   /*******************************************************************************
    ** Set scope.
    *******************************************************************************/
   public void setScope(UserPreferenceActionScope scope)
   {
      this.scope = scope;
   }



   /*******************************************************************************
    ** With scope.
    *******************************************************************************/
   public PreferenceActionV1 withScope(UserPreferenceActionScope scope)
   {
      this.scope = scope;
      return (this);
   }



   /*******************************************************************************
    ** Get listName.
    *******************************************************************************/
   public String getListName()
   {
      return (listName);
   }



   /*******************************************************************************
    ** Set listName.
    *******************************************************************************/
   public void setListName(String listName)
   {
      this.listName = listName;
   }



   /*******************************************************************************
    ** With listName.
    *******************************************************************************/
   public PreferenceActionV1 withListName(String listName)
   {
      this.listName = listName;
      return (this);
   }



   /*******************************************************************************
    ** Get confirmationText.
    *******************************************************************************/
   public String getConfirmationText()
   {
      return (confirmationText);
   }



   /*******************************************************************************
    ** Set confirmationText.
    *******************************************************************************/
   public void setConfirmationText(String confirmationText)
   {
      this.confirmationText = confirmationText;
   }



   /*******************************************************************************
    ** With confirmationText.
    *******************************************************************************/
   public PreferenceActionV1 withConfirmationText(String confirmationText)
   {
      this.confirmationText = confirmationText;
      return (this);
   }



   /*******************************************************************************
    ** Get isDestructive.
    *******************************************************************************/
   public boolean getIsDestructive()
   {
      return (isDestructive);
   }



   /*******************************************************************************
    ** Set isDestructive.
    *******************************************************************************/
   public void setIsDestructive(boolean isDestructive)
   {
      this.isDestructive = isDestructive;
   }



   /*******************************************************************************
    ** With isDestructive.
    *******************************************************************************/
   public PreferenceActionV1 withIsDestructive(boolean isDestructive)
   {
      this.isDestructive = isDestructive;
      return (this);
   }



   /*******************************************************************************
    ** Get inputFields.
    *******************************************************************************/
   public List<FieldMetaData> getInputFields()
   {
      return (inputFields);
   }



   /*******************************************************************************
    ** Set inputFields.
    *******************************************************************************/
   public void setInputFields(List<FieldMetaData> inputFields)
   {
      this.inputFields = inputFields;
   }



   /*******************************************************************************
    ** With inputFields.
    *******************************************************************************/
   public PreferenceActionV1 withInputFields(List<FieldMetaData> inputFields)
   {
      this.inputFields = inputFields;
      return (this);
   }



   /*******************************************************************************
    ** Get order.
    *******************************************************************************/
   public int getOrder()
   {
      return (order);
   }



   /*******************************************************************************
    ** Set order.
    *******************************************************************************/
   public void setOrder(int order)
   {
      this.order = order;
   }



   /*******************************************************************************
    ** With order.
    *******************************************************************************/
   public PreferenceActionV1 withOrder(int order)
   {
      this.order = order;
      return (this);
   }
}
