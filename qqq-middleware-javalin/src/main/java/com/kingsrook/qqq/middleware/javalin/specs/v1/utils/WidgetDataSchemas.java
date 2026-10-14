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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.AlertData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.ChartData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.ChildRecordListData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.CompositeWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.CronUIWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.DividerWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.DynamicFormWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.FieldValueListData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.FilterAndColumnsSetupData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.LineChartData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.LocationData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.MultiStatisticsData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.MultiTableData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.ParentWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.PieChartData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.ProcessWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.QWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.QuickSightChart;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.RawHTML;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.RowBuilderData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.StatisticsData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.StepperData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.USMapWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.SchemaBuilder;
import com.kingsrook.qqq.openapi.model.Discriminator;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;


/*******************************************************************************
 ** Typed OpenAPI schemas for the data of each widget type (the `widgetData` of
 ** POST /widget/{name}), built from the backend's QWidgetData classes, and the
 ** discriminated union over them keyed by the data's `type`.  A client renders
 ** by type, never by sniffing the shape (docs/UI-API-CLIENTS.md).
 *******************************************************************************/
public class WidgetDataSchemas
{
   /***************************************************************************
    ** the data class of each widget type (types without a class of their own,
    ** e.g. customComponent or generic, are described by WidgetDataGeneric)
    ***************************************************************************/
   private static final Map<Class<? extends QWidgetData>, List<WidgetType>> DATA_CLASSES = new LinkedHashMap<>();

   static
   {
      DATA_CLASSES.put(AlertData.class, List.of(WidgetType.ALERT));
      DATA_CLASSES.put(ChartData.class, List.of(WidgetType.CHART, WidgetType.BAR_CHART, WidgetType.HORIZONTAL_BAR_CHART, WidgetType.STACKED_BAR_CHART));
      DATA_CLASSES.put(LineChartData.class, List.of(WidgetType.LINE_CHART, WidgetType.SMALL_LINE_CHART));
      DATA_CLASSES.put(PieChartData.class, List.of(WidgetType.PIE_CHART));
      DATA_CLASSES.put(ChildRecordListData.class, List.of(WidgetType.CHILD_RECORD_LIST));
      DATA_CLASSES.put(CompositeWidgetData.class, List.of(WidgetType.COMPOSITE));
      DATA_CLASSES.put(CronUIWidgetData.class, List.of(WidgetType.CRON_UI));
      DATA_CLASSES.put(DividerWidgetData.class, List.of(WidgetType.DIVIDER));
      DATA_CLASSES.put(DynamicFormWidgetData.class, List.of(WidgetType.DYNAMIC_FORM));
      DATA_CLASSES.put(FieldValueListData.class, List.of(WidgetType.FIELD_VALUE_LIST));
      DATA_CLASSES.put(FilterAndColumnsSetupData.class, List.of(WidgetType.FILTER_AND_COLUMNS_SETUP));
      DATA_CLASSES.put(LocationData.class, List.of(WidgetType.LOCATION));
      DATA_CLASSES.put(MultiStatisticsData.class, List.of(WidgetType.MULTI_STATISTICS));
      DATA_CLASSES.put(MultiTableData.class, List.of(WidgetType.MULTI_TABLE));
      DATA_CLASSES.put(ParentWidgetData.class, List.of(WidgetType.PARENT_WIDGET));
      DATA_CLASSES.put(ProcessWidgetData.class, List.of(WidgetType.PROCESS));
      DATA_CLASSES.put(QuickSightChart.class, List.of(WidgetType.QUICK_SIGHT_CHART));
      DATA_CLASSES.put(RawHTML.class, List.of(WidgetType.HTML));
      DATA_CLASSES.put(RowBuilderData.class, List.of(WidgetType.ROW_BUILDER));
      DATA_CLASSES.put(StatisticsData.class, List.of(WidgetType.STATISTICS));
      DATA_CLASSES.put(StepperData.class, List.of(WidgetType.STEPPER));
      DATA_CLASSES.put(TableData.class, List.of(WidgetType.TABLE));
      DATA_CLASSES.put(USMapWidgetData.class, List.of(WidgetType.USA_MAP));
   }

   public static final String GENERIC_SCHEMA_NAME = "WidgetDataGeneric";
   public static final String UNION_SCHEMA_NAME   = "WidgetData";



   /*******************************************************************************
    ** The component schema name for a widget data class.
    *******************************************************************************/
   public static String schemaName(Class<?> dataClass)
   {
      String simple = dataClass.getSimpleName().replaceFirst("(Widget)?Data$", "");
      return ("WidgetData" + simple);
   }



   /*******************************************************************************
    ** Every widget type value (as sent in widget metadata and data `type`).
    *******************************************************************************/
   public static List<String> widgetTypeValues()
   {
      return (Arrays.stream(WidgetType.values()).map(WidgetType::getType).toList());
   }



   /*******************************************************************************
    ** All the component schemas: one per data class, the generic one, and the
    ** union (WidgetData).
    *******************************************************************************/
   public static Map<String, Schema> componentSchemas()
   {
      Map<String, Schema> rs      = new TreeMap<>();
      Map<String, String> mapping = new TreeMap<>();
      List<Schema>        oneOf   = new ArrayList<>();

      for(Map.Entry<Class<? extends QWidgetData>, List<WidgetType>> entry : DATA_CLASSES.entrySet())
      {
         String name   = schemaName(entry.getKey());
         Schema schema = new SchemaBuilder().classToSchema(entry.getKey());

         List<String> types = entry.getValue().stream().map(WidgetType::getType).toList();
         schema.withDescription("Data of " + StringUtils.joinWithCommasAndAnd(types) + " widgets.");
         Map<String, Schema> properties = new LinkedHashMap<>();

         ////////////////////////////////////////////////////////////////////////
         // the builder reads declared members only - so add those of any data //
         // superclass (e.g. PieChartData extends ChartData) first             //
         ////////////////////////////////////////////////////////////////////////
         for(Class<?> superclass = entry.getKey().getSuperclass(); superclass != null && !QWidgetData.class.equals(superclass); superclass = superclass.getSuperclass())
         {
            Schema superSchema = new SchemaBuilder().classToSchema(superclass);
            if(superSchema.getProperties() != null)
            {
               properties.putAll(superSchema.getProperties());
            }
         }
         if(schema.getProperties() != null)
         {
            properties.putAll(schema.getProperties());
         }
         properties.putAll(commonProperties());
         properties = shallow(properties);
         properties.put("type", new Schema().withType(Type.STRING).withDescription("The widget type this data is for.").withEnumValues(types));
         schema.withProperties(properties);

         rs.put(name, schema);
         oneOf.add(new Schema().withRefToSchema(name));
         types.forEach(type -> mapping.put(type, "#/components/schemas/" + name));
      }

      ////////////////////////////////////////////////////////////////////////////////
      // types whose data has no class of its own (e.g. customComponent, generic):  //
      // the common properties, plus whatever the widget's code adds                //
      ////////////////////////////////////////////////////////////////////////////////
      List<String> genericTypes = widgetTypeValues().stream().filter(t -> !mapping.containsKey(t)).toList();
      Schema generic = new Schema().withType(Type.OBJECT)
         .withDescription("Data of widget types without a fixed shape (" + StringUtils.joinWithCommasAndAnd(genericTypes) + "): the common properties plus widget-specific ones.")
         .withProperties(new LinkedHashMap<>(commonProperties()))
         .withAdditionalProperties(true);
      generic.getProperties().put("type", new Schema().withType(Type.STRING).withDescription("The widget type this data is for.").withEnumValues(genericTypes));
      rs.put(GENERIC_SCHEMA_NAME, generic);
      oneOf.add(new Schema().withRefToSchema(GENERIC_SCHEMA_NAME));
      genericTypes.forEach(type -> mapping.put(type, "#/components/schemas/" + GENERIC_SCHEMA_NAME));

      rs.put(UNION_SCHEMA_NAME, new Schema()
         .withDescription("The data of a widget: one of the per-type shapes, chosen by `type` (the widget type). Clients render by type and ignore unknown properties; an unknown type is rendered as the widget label with its footerHTML/label only.")
         .withOneOf(oneOf)
         .withDiscriminator(new Discriminator().withPropertyName("type").withMapping(mapping)));

      return (rs);
   }



   /*******************************************************************************
    ** Keep the top-level properties typed, but describe nested objects only as
    ** objects: the reflected shapes of nested backend classes (table metadata,
    ** records) are not part of the widget contract, and would otherwise pull in
    ** internals.  Sorted by name, so the document is stable.
    *******************************************************************************/
   private static Map<String, Schema> shallow(Map<String, Schema> properties)
   {
      Map<String, Schema> rs = new TreeMap<>();
      for(Map.Entry<String, Schema> entry : properties.entrySet())
      {
         Schema property = entry.getValue();
         if(property.getProperties() != null)
         {
            property = new Schema().withType(Type.OBJECT).withDescription(property.getDescription()).withAdditionalProperties(true);
         }
         else if(property.getItems() != null && property.getItems().getProperties() != null)
         {
            property = new Schema().withType(Type.ARRAY).withDescription(property.getDescription()).withItems(new Schema().withType(Type.OBJECT).withAdditionalProperties(true));
         }
         rs.put(entry.getKey(), property);
      }
      return (rs);
   }



   /*******************************************************************************
    ** Properties every widget data has (QWidgetData).
    *******************************************************************************/
   private static Map<String, Schema> commonProperties()
   {
      Map<String, Schema> rs = new LinkedHashMap<>();
      rs.put("label", new Schema().withType(Type.STRING).withDescription("Title of the widget (overrides the metadata label when present)."));
      rs.put("sublabel", new Schema().withType(Type.STRING).withDescription("Secondary title."));
      rs.put("footerHTML", new Schema().withType(Type.STRING).withDescription("Footer, as HTML. Non-web clients render it as plain text."));
      rs.put("hasPermission", new Schema().withType(Type.BOOLEAN).withDescription("False when the user may not see this widget's data; show the label and a permission message only."));
      rs.put("dropdownNameList", stringList("Names of the dropdown filters the widget offers (parallel to the other dropdown lists)."));
      rs.put("dropdownLabelList", stringList("Labels of the dropdown filters."));
      rs.put("dropdownDefaultValueList", stringList("Default value of each dropdown filter."));
      rs.put("dropdownDataList", new Schema().withType(Type.ARRAY).withDescription("Options of each dropdown filter: a list per dropdown of {id, label} objects.")
         .withItems(new Schema().withType(Type.ARRAY).withItems(new Schema().withType(Type.OBJECT).withAdditionalProperties(new Schema().withType(Type.STRING)))));
      rs.put("dropdownNeedsSelectedText", new Schema().withType(Type.STRING).withDescription("Message to show until every dropdown has a value."));
      rs.put("csvData", new Schema().withType(Type.ARRAY).withDescription("Rows (each a list of cells) for exporting the widget as CSV.").withItems(new Schema().withType(Type.ARRAY).withItems(new Schema())));
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Schema stringList(String description)
   {
      return (new Schema().withType(Type.ARRAY).withDescription(description).withItems(new Schema().withType(Type.STRING)));
   }
}
