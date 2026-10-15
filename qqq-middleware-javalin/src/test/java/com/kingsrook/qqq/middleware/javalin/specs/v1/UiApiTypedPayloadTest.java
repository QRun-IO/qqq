/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.RawHTML;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.WidgetResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.Icon;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.MaterialDashboardInstanceSettings;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.OutputRecord;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.SupplementalInstanceMetaData;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.HtmlText;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.SemanticIcons;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.WidgetDataSchemas;
import com.kingsrook.qqq.middleware.javalin.tools.OpenApiCompatibilityChecker;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Regression coverage for typed payloads and client-neutral response fields.
 *******************************************************************************/
class UiApiTypedPayloadTest
{
   private static final Set<String> OPEN_MAPS = Set.of(
      "components.schemas.DeliveryTargetsRequestV1.properties.chosen",
      "components.schemas.FieldAdornment.properties.values.properties.supplementalCodeReference",
      "components.schemas.FormAdjusterOutputV1.properties.updatedFieldValues",
      "components.schemas.ManageSessionResponseV1.properties.values",
      "components.schemas.MetaDataResponseV1.properties.supplementalInstanceMetaData.properties.assistant",
      "components.schemas.MetaDataResponseV1.properties.supplementalInstanceMetaData.properties.materialDashboardTheme",
      "components.schemas.MetaDataResponseV1.properties.widgets.additionalProperties.properties.defaultValues",
      "components.schemas.MetaDataResponseV1.properties.widgets.additionalProperties.properties.icons",
      "components.schemas.RecordFeatureSummaryV1.properties.extensions.additionalProperties",
      "components.schemas.RecordFeaturesV1.properties.settings.additionalProperties",
      "components.schemas.SupplementalInstanceMetaData.properties.assistant",
      "components.schemas.SupplementalInstanceMetaData.properties.materialDashboardTheme",
      "components.schemas.TableApiDocumentV1",
      "components.schemas.WidgetDataGeneric",
      "paths./qqq/v1/table/{tableName}/formAdjuster/{identifier}/{event}.post.requestBody.content.application/json.schema.properties.allValues");



   /*******************************************************************************
    ** Verify the public client-neutral contract.
    *******************************************************************************/
   @Test
   void semanticIcons()
   {
      assertEquals("add-circle", SemanticIcons.semanticName("add_circle"));
      assertEquals("person-add", SemanticIcons.semanticName("person_add"));
      assertEquals("play", SemanticIcons.semanticName("play_arrow"));
      assertNull(SemanticIcons.semanticName(null));
      assertNull(SemanticIcons.semanticName("application_custom_icon"));
      JSONObject icon = new JSONObject(JsonUtils.toJson(new Icon(new QIcon().withName("library_add"))));
      assertEquals("library_add", icon.getString("name"));
      assertEquals("library-add", icon.getString("semanticName"));
   }



   /*******************************************************************************
    ** Verify the public client-neutral contract.
    *******************************************************************************/
   @Test
   void htmlText()
   {
      assertNull(HtmlText.fromHtml(null));
      assertEquals("", HtmlText.fromHtml(""));
      assertEquals("Hello & link\nNext", HtmlText.fromHtml("<p>Hello &amp; <a href='https://example.test'>link</a><br>Next</p>"));
      assertEquals("First\nSecond", HtmlText.fromHtml("<div>First</div><div>Second</div>"));
      assertEquals("Visible", HtmlText.fromHtml("<script>alert(1)</script><style>p{color:red}</style><p>Visible</p>"));
      assertEquals("unfinished", HtmlText.fromHtml("<p><b>unfinished"));
      RawHTML raw = new RawHTML();
      raw.setHtml("<p>Hello<br>world</p>");
      raw.setFooterHTML("<a href='/details'>Details</a>");
      JSONObject widget = new JSONObject(JsonUtils.toJson(new WidgetResponseV1().withWidgetData(raw).getWidgetData()));
      assertEquals("<p>Hello<br>world</p>", widget.getString("html"));
      assertEquals("Hello\nworld", widget.getString("text"));
      assertEquals("Details", widget.getString("footerText"));
      OutputRecord record = new OutputRecord(new QRecord().withValue("cleared", null).withValue("html", "<b>record value</b>"), true);
      JSONObject nested = new JSONObject(JsonUtils.toJson(new WidgetResponseV1().withWidgetData(Map.of("record", record)).getWidgetData()));
      JSONObject values = nested.getJSONObject("record").getJSONObject("values");
      assertTrue(values.has("cleared"));
      assertTrue(values.isNull("cleared"));
      assertEquals("<b>record value</b>", values.getString("html"));
      assertFalse(values.has("text"));
   }



   /*******************************************************************************
    ** Verify the public client-neutral contract.
    *******************************************************************************/
   @Test
   void neutralSettings()
   {
      SupplementalInstanceMetaData metadata = new SupplementalInstanceMetaData()
         .withMaterialDashboard(new MaterialDashboardInstanceSettings().withProcessNamesToAddToAllQueryAndViewScreens(List.of("visible")))
         .withMaterialDashboardTheme(Map.of("primaryColor", "#123456", "fontFamily", "sans-serif", "textPrimary", "#111111", "customCss", "body{}"));
      JSONObject settings = new JSONObject(JsonUtils.toJson(metadata)).getJSONObject("uiSettings");
      assertEquals("visible", settings.getJSONArray("processNamesToAddToAllQueryAndViewScreens").getString(0));
      assertEquals("#123456", settings.getJSONObject("theme").getString("primaryColor"));
      assertEquals("#111111", settings.getJSONObject("theme").getString("textPrimary"));
      assertFalse(settings.getJSONObject("theme").has("customCss"));
      assertNull(new SupplementalInstanceMetaData().getUiSettings());
   }



   /*******************************************************************************
    ** Verify the public client-neutral contract.
    *******************************************************************************/
   @Test
   void widgetScalarsAndReferences()
   {
      var schemas = WidgetDataSchemas.componentSchemas();
      assertEquals("number", schemas.get("WidgetDataStatistics").getProperties().get("count").getType());
      assertEquals(Boolean.TRUE, schemas.get("WidgetDataStatistics").getProperties().get("count").getNullable());
      assertEquals("integer", schemas.get("WidgetDataChart").getProperties().get("height").getType());
      assertEquals("boolean", schemas.get("WidgetDataChart").getProperties().get("isCurrency").getType());
      assertEquals("#/components/schemas/OutputRecord", schemas.get("WidgetDataChildRecordList").getProperties().get("queryOutput")
         .getProperties().get("records").getItems().getRef());
      assertEquals("number", schemas.get("WidgetDataChart").getProperties().get("chartData").getProperties().get("datasets")
         .getItems().getProperties().get("data").getItems().getType());
   }



   /*******************************************************************************
    ** Verify the public client-neutral contract.
    *******************************************************************************/
   @Test
   void onlyExplicitlyAllowedOpenMaps() throws Exception
   {
      walk(OpenApiCompatibilityChecker.parse(Files.readString(Path.of("src/main/resources/openapi/v1/openapi.yaml"))), "");
   }



   /*******************************************************************************
    ** Verify the public client-neutral contract.
    *******************************************************************************/
   private void walk(Object value, String path)
   {
      if(value instanceof Map<?, ?> map)
      {
         if(Boolean.TRUE.equals(map.get("additionalProperties")))
         {
            assertTrue(OPEN_MAPS.contains(path), "Unexpected open map: " + path);
            assertTrue(map.get("description") instanceof String description && !description.isBlank(), "Missing open-map description: " + path);
         }
         map.forEach((key, child) -> walk(child, path.isEmpty() ? key.toString() : path + "." + key));
      }
      else if(value instanceof List<?> list)
      {
         for(int i = 0; i < list.size(); i++)
         {
            walk(list.get(i), path + "." + i);
         }
      }
   }
}
