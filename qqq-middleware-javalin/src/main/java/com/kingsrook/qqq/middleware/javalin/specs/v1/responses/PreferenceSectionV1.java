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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceListMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceSectionMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceSectionResult;
import com.kingsrook.qqq.middleware.javalin.executors.io.UserPreferencesOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.FieldMetaData;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.Icon;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.PossibleValueOption;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;


/*******************************************************************************
 ** PreferenceSectionV1 in the user preferences API.
 *******************************************************************************/
public class PreferenceSectionV1 implements UserPreferencesOutputInterface, ToSchema
{
   private List<PreferenceActionV1> actions;
   private boolean showInUserMenu = false;
   private String name;
   private String label;
   private Icon icon;
   private String description;
   private int order;
   private String appName;
   private List<FieldMetaData> fields;
   private Map<String, Serializable> values;
   private List<PreferenceListV1> lists;
   private Map<String, List<PossibleValueOption>> possibleValues;



   /*******************************************************************************
    ** One visible section with the signed-in user's current preferences.
    *******************************************************************************/
   @Override
   public Schema toSchema()
   {
      return (new Schema().withType(Type.OBJECT).withDescription("One visible section with the signed-in user's current preferences.")
         .withProperty("showInUserMenu", new Schema().withType(Type.BOOLEAN).withDescription("Add a user-menu entry using the section label and icon to open this section; defaults to false."))
         .withProperty("name", new Schema().withType(Type.STRING).withDescription("Stable section name used in URLs."))
         .withProperty("label", new Schema().withType(Type.STRING).withDescription("Human-readable section title."))
         .withProperty("description", new Schema().withType(Type.STRING).withDescription("Explanation for clients to display."))
         .withProperty("appName", new Schema().withType(Type.STRING).withDescription("Optional owning application name."))
         .withProperty("icon", new Schema().withRefToSchema("Icon"))
         .withProperty("order", new Schema().withType(Type.INTEGER).withDescription("Display order, ascending then label; defaults to 100."))
         .withProperty("fields", new Schema().withType(Type.ARRAY).withDescription("Scalar field definitions with editability, type, defaults and validation.").withItems(new Schema().withRefToSchema("FieldMetaData")))
         .withProperty("values", new Schema().withType(Type.OBJECT).withDescription("Field name to typed value. Omitted fields retain their existing values; explicit null clears an optional field.").withAdditionalProperties(new Schema()))
         .withProperty("actions", new Schema().withType(Type.ARRAY).withItems(new Schema().withRefToSchema("PreferenceActionV1")).withDescription("Registered section, list and row actions, ordered by order then label."))
         .withProperty("lists", new Schema().withType(Type.ARRAY).withDescription("Optional fixed-row preference lists.").withItems(new Schema().withRefToSchema("PreferenceListV1")))
         .withProperty("possibleValues", new Schema().withType(Type.OBJECT).withDescription("Every ENUM possible-value source referenced by scalar, list or action input fields, keyed by source name.").withAdditionalProperties(new Schema().withType(Type.ARRAY).withItems(new Schema().withRefToSchema("PossibleValueOption"))))
         );
   }



   /*******************************************************************************
    ** Map core metadata and values, including ENUM choices for scalar and row fields.
    *******************************************************************************/
   @Override
   public void setPreferenceResults(List<UserPreferenceSectionResult> results)
   {
      UserPreferenceSectionResult result = results.get(0);
      UserPreferenceSectionMetaData section = result.getSection();
      showInUserMenu = section.getShowInUserMenu();
      name = section.getName();
      label = section.getLabel();
      icon = section.getIcon() == null ? null : new Icon(section.getIcon());
      description = section.getDescription();
      order = section.getOrder();
      appName = section.getAppName();
      fields = section.getFields().stream().map(FieldMetaData::new).toList();
      values = result.getValues().getValues();
      lists = new ArrayList<>();
      possibleValues = new LinkedHashMap<>();
      addPossibleValues(section.getFields());
      actions = section.getActions().stream().map(PreferenceActionV1::from)
         .sorted(Comparator.comparingInt(PreferenceActionV1::getOrder).thenComparing(PreferenceActionV1::getLabel)).toList();
      section.getActions().forEach(action -> addPossibleValues(action.getInputFields()));
      for(UserPreferenceListMetaData list : section.getLists())
      {
         lists.add(new PreferenceListV1().withName(list.getName()).withLabel(list.getLabel()).withDescription(list.getDescription())
            .withRowLabel(list.getRowLabel()).withFields(list.getFields().stream().map(FieldMetaData::new).toList())
            .withRows(result.getValues().getLists().getOrDefault(list.getName(), List.of()).stream().map(PreferenceRowV1::from).toList()));
         addPossibleValues(list.getFields());
      }
   }



   /*******************************************************************************
    ** Include each referenced ENUM source exactly once.
    *******************************************************************************/
   private void addPossibleValues(List<QFieldMetaData> fields)
   {
      for(QFieldMetaData field : fields)
      {
         QPossibleValueSource source = field.getPossibleValueSourceName() == null ? null : QContext.getQInstance().getPossibleValueSource(field.getPossibleValueSourceName());
         if(source != null && source.getType() == QPossibleValueSourceType.ENUM)
         {
            possibleValues.put(source.getName(), source.getEnumValues().stream().map(PossibleValueOption::new).toList());
         }
      }
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
   public PreferenceSectionV1 withName(String name)
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
   public PreferenceSectionV1 withLabel(String label)
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
   public PreferenceSectionV1 withIcon(Icon icon)
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
   public PreferenceSectionV1 withDescription(String description)
   {
      this.description = description;
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
   public PreferenceSectionV1 withOrder(int order)
   {
      this.order = order;
      return (this);
   }



   /*******************************************************************************
    ** Get appName.
    *******************************************************************************/
   public String getAppName()
   {
      return (appName);
   }



   /*******************************************************************************
    ** Set appName.
    *******************************************************************************/
   public void setAppName(String appName)
   {
      this.appName = appName;
   }



   /*******************************************************************************
    ** With appName.
    *******************************************************************************/
   public PreferenceSectionV1 withAppName(String appName)
   {
      this.appName = appName;
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
   public PreferenceSectionV1 withFields(List<FieldMetaData> fields)
   {
      this.fields = fields;
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
   public PreferenceSectionV1 withValues(Map<String, Serializable> values)
   {
      this.values = values;
      return (this);
   }



   /*******************************************************************************
    ** Get lists.
    *******************************************************************************/
   public List<PreferenceListV1> getLists()
   {
      return (lists);
   }



   /*******************************************************************************
    ** Set lists.
    *******************************************************************************/
   public void setLists(List<PreferenceListV1> lists)
   {
      this.lists = lists;
   }



   /*******************************************************************************
    ** With lists.
    *******************************************************************************/
   public PreferenceSectionV1 withLists(List<PreferenceListV1> lists)
   {
      this.lists = lists;
      return (this);
   }



   /*******************************************************************************
    ** Get possibleValues.
    *******************************************************************************/
   public Map<String, List<PossibleValueOption>> getPossibleValues()
   {
      return (possibleValues);
   }



   /*******************************************************************************
    ** Set possibleValues.
    *******************************************************************************/
   public void setPossibleValues(Map<String, List<PossibleValueOption>> possibleValues)
   {
      this.possibleValues = possibleValues;
   }



   /*******************************************************************************
    ** With possibleValues.
    *******************************************************************************/
   public PreferenceSectionV1 withPossibleValues(Map<String, List<PossibleValueOption>> possibleValues)
   {
      this.possibleValues = possibleValues;
      return (this);
   }



   /*******************************************************************************
    ** Get actions.
    *******************************************************************************/
   public List<PreferenceActionV1> getActions()
   {
      return (actions);
   }



   /*******************************************************************************
    ** Set actions.
    *******************************************************************************/
   public void setActions(List<PreferenceActionV1> actions)
   {
      this.actions = actions;
   }



   /*******************************************************************************
    ** With actions.
    *******************************************************************************/
   public PreferenceSectionV1 withActions(List<PreferenceActionV1> actions)
   {
      this.actions = actions;
      return (this);
   }



   /*******************************************************************************
    ** Get showInUserMenu.
    *******************************************************************************/
   public boolean getShowInUserMenu()
   {
      return (showInUserMenu);
   }



   /*******************************************************************************
    ** Set showInUserMenu.
    *******************************************************************************/
   public void setShowInUserMenu(boolean showInUserMenu)
   {
      this.showInUserMenu = showInUserMenu;
   }



   /*******************************************************************************
    ** With showInUserMenu.
    *******************************************************************************/
   public PreferenceSectionV1 withShowInUserMenu(boolean showInUserMenu)
   {
      this.showInUserMenu = showInUserMenu;
      return (this);
   }
}
