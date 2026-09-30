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


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpRole;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.DenyBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.QJavalinMetaData;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.SupplementalInstanceMetaData;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for MetaDataSpecV1 
 *******************************************************************************/
class MetaDataSpecV1Test extends SpecTestBase
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new MetaDataSpecV1();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected String getVersion()
   {
      return "v1";
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test()
   {
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/metaData").asString();
      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertThat(jsonObject.getJSONObject("tables").length()).isGreaterThanOrEqualTo(1);
      assertThat(jsonObject.getJSONObject("processes").length()).isGreaterThanOrEqualTo(1);
      assertThat(jsonObject.getJSONObject("apps").length()).isGreaterThanOrEqualTo(1);
      assertThat(jsonObject.getJSONArray("appTree").length()).isGreaterThanOrEqualTo(1);

      ////////////////////////////////////////////////////////////////////////
      // widgets carry their full frontend meta-data (including permission) //
      // and reports are listed with what a frontend needs to run them      //
      ////////////////////////////////////////////////////////////////////////
      JSONObject timezoneWidget = jsonObject.getJSONObject("widgets").getJSONObject("timezoneWidget");
      assertTrue(timezoneWidget.getBoolean("hasPermission"));
      assertTrue(timezoneWidget.has("isCard"));
      JSONObject personsReport = jsonObject.getJSONObject("reports").getJSONObject("personsReport");
      assertEquals("personsReport", personsReport.getString("name"));
      assertTrue(personsReport.getBoolean("hasPermission"));
   }



   /*******************************************************************************
    ** Tables advertise their search fields, only to sessions that may read them.
    *******************************************************************************/
   @Test
   void testSearchFields()
   {
      serverQInstance.getTable(TestUtils.TABLE_NAME_PERSON).withSearchFields("firstName", "lastName");

      JSONObject tables = JsonUtils.toJSONObject(Unirest.get(getBaseUrlAndPath() + "/metaData").asString().getBody()).getJSONObject("tables");
      assertThat(tables.getJSONObject(TestUtils.TABLE_NAME_PERSON).getJSONArray("searchFields").toList()).containsExactly("firstName", "lastName");
      assertThat(tables.getJSONObject(TestUtils.TABLE_NAME_PET).has("searchFields")).isFalse();

      serverQInstance.getTable(TestUtils.TABLE_NAME_PERSON).setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      tables = JsonUtils.toJSONObject(Unirest.get(getBaseUrlAndPath() + "/metaData").asString().getBody()).getJSONObject("tables");
      assertThat(tables.optJSONObject(TestUtils.TABLE_NAME_PERSON) == null || !tables.getJSONObject(TestUtils.TABLE_NAME_PERSON).has("searchFields")).isTrue();
   }



   /*******************************************************************************
    ** Supplemental instance meta-data is never published as a whole: the javalin
    ** meta-data (route providers, file paths, route authenticators, the uploaded
    ** file archive table) and other modules' entries stay out of the response,
    ** and without a materialDashboard entry the property is omitted.
    *******************************************************************************/
   @Test
   void testSupplementalInstanceMetaDataIsNotPublishedWholesale()
   {
      QJavalinMetaData javalinMetaData = QJavalinMetaData.of(serverQInstance);
      assertThat(javalinMetaData.getRouteProviders()).isNotEmpty();
      javalinMetaData.withUploadedFileArchiveTableName("uploadedFileArchive");
      serverQInstance.withSupplementalMetaData(new OtherModuleMetaData());

      String body = getMetaDataBody();
      assertNoServerConfig(body);
      assertFalse(JsonUtils.toJSONObject(body).has("supplementalInstanceMetaData"));
   }



   /*******************************************************************************
    ** The material dashboard's all-screens processes are published, limited to
    ** processes the user may see, in configured order without duplicates - and
    ** nothing else from its (or any other) supplemental meta-data.
    *******************************************************************************/
   @Test
   void testMaterialDashboardProcessNamesAreFilteredToVisibleProcesses()
   {
      serverQInstance.getProcess(TestUtils.PROCESS_NAME_SIMPLE_SLEEP).setPermissionRules(new QPermissionRules()
         .withLevel(PermissionLevel.HAS_ACCESS_PERMISSION)
         .withDenyBehavior(DenyBehavior.HIDDEN));
      QJavalinMetaData.of(serverQInstance).withUploadedFileArchiveTableName("uploadedFileArchive");
      serverQInstance.withSupplementalMetaData(new OtherModuleMetaData());
      serverQInstance.withSupplementalMetaData(new TestMaterialDashboardMetaData()
         .withProcessNamesToAddToAllQueryAndViewScreens(List.of(
            TestUtils.PROCESS_NAME_GREET_PEOPLE_INTERACTIVE,
            TestUtils.PROCESS_NAME_SIMPLE_SLEEP,
            "notAProcess",
            TestUtils.PROCESS_NAME_GREET_PEOPLE_INTERACTIVE)));

      String     body       = getMetaDataBody();
      JSONObject jsonObject = JsonUtils.toJSONObject(body);
      assertThat(jsonObject.getJSONObject("processes").has(TestUtils.PROCESS_NAME_SIMPLE_SLEEP)).isFalse();

      JSONObject supplemental = jsonObject.getJSONObject("supplementalInstanceMetaData");
      assertEquals(Set.of("materialDashboard"), supplemental.keySet());
      JSONObject materialDashboard = supplemental.getJSONObject("materialDashboard");
      assertEquals(Set.of("processNamesToAddToAllQueryAndViewScreens"), materialDashboard.keySet());
      assertThat(materialDashboard.getJSONArray("processNamesToAddToAllQueryAndViewScreens").toList())
         .containsExactly(TestUtils.PROCESS_NAME_GREET_PEOPLE_INTERACTIVE);

      assertNoServerConfig(body);
      assertThat(body).doesNotContain(TestMaterialDashboardMetaData.SECRET_SETTING_VALUE);
   }



   /*******************************************************************************
    ** With a materialDashboard entry but no visible processes in it, the list is
    ** published empty (rather than omitted).
    *******************************************************************************/
   @Test
   void testMaterialDashboardWithNoVisibleProcessesPublishesEmptyList()
   {
      serverQInstance.withSupplementalMetaData(new TestMaterialDashboardMetaData()
         .withProcessNamesToAddToAllQueryAndViewScreens(List.of("notAProcess")));

      JSONObject materialDashboard = JsonUtils.toJSONObject(getMetaDataBody())
         .getJSONObject("supplementalInstanceMetaData")
         .getJSONObject("materialDashboard");
      assertThat(materialDashboard.getJSONArray("processNamesToAddToAllQueryAndViewScreens").toList()).isEmpty();

      serverQInstance.withSupplementalMetaData(new TestMaterialDashboardMetaData().withProcessNamesToAddToAllQueryAndViewScreens(null));
      materialDashboard = JsonUtils.toJSONObject(getMetaDataBody())
         .getJSONObject("supplementalInstanceMetaData")
         .getJSONObject("materialDashboard");
      assertThat(materialDashboard.getJSONArray("processNamesToAddToAllQueryAndViewScreens").toList()).isEmpty();
   }



   /*******************************************************************************
    ** The instance-wide record view actions placement is published by its enum
    ** constant's name; settings without one omit it (see the tests above).
    *******************************************************************************/
   @Test
   void testMaterialDashboardRecordViewActionsPlacementIsPublished()
   {
      serverQInstance.withSupplementalMetaData(new PlacementMaterialDashboardMetaData().withProcessNamesToAddToAllQueryAndViewScreens(List.of()));

      JSONObject materialDashboard = JsonUtils.toJSONObject(getMetaDataBody())
         .getJSONObject("supplementalInstanceMetaData")
         .getJSONObject("materialDashboard");
      assertEquals(Set.of("processNamesToAddToAllQueryAndViewScreens", "recordViewActionsPlacement"), materialDashboard.keySet());
      assertEquals("INLINE_WITH_PAGE_TITLE", materialDashboard.getString("recordViewActionsPlacement"));
   }



   /*******************************************************************************
    ** The material dashboard's weekday criteria settings are published (enabled,
    ** and the WeekdayOfDateTime arguments), and nothing else from that object.
    *******************************************************************************/
   @Test
   void testMaterialDashboardWeekdayCriteriaSettings()
   {
      serverQInstance.withSupplementalMetaData(new TestMaterialDashboardMetaData()
         .withWeekdayCriteriaSettings(new TestWeekdayCriteriaSettings().withEnabled(false)
            .withDateTimeFieldFunctionArguments(Map.of("timeZoneId", "US/Central", "useSessionZoneId", false))));

      String     body              = getMetaDataBody();
      JSONObject materialDashboard = JsonUtils.toJSONObject(body).getJSONObject("supplementalInstanceMetaData").getJSONObject("materialDashboard");
      assertEquals(Set.of("processNamesToAddToAllQueryAndViewScreens", "weekdayCriteriaSettings"), materialDashboard.keySet());
      JSONObject weekday = materialDashboard.getJSONObject("weekdayCriteriaSettings");
      assertEquals(Set.of("enabled", "dateTimeFieldFunctionArguments"), weekday.keySet());
      assertFalse(weekday.getBoolean("enabled"));
      assertEquals(Map.of("timeZoneId", "US/Central", "useSessionZoneId", false), weekday.getJSONObject("dateTimeFieldFunctionArguments").toMap());
      assertThat(body).doesNotContain(TestWeekdayCriteriaSettings.SECRET_SETTING_VALUE);

      /////////////////////////////////////////////////////////////////////////////
      // a settings object without a value for enabled means enabled (Material's //
      // default), and no arguments omits them                                   //
      /////////////////////////////////////////////////////////////////////////////
      serverQInstance.withSupplementalMetaData(new TestMaterialDashboardMetaData()
         .withWeekdayCriteriaSettings(new TestWeekdayCriteriaSettings().withEnabled(null).withDateTimeFieldFunctionArguments(Map.of())));
      weekday = JsonUtils.toJSONObject(getMetaDataBody()).getJSONObject("supplementalInstanceMetaData").getJSONObject("materialDashboard").getJSONObject("weekdayCriteriaSettings");
      assertEquals(Set.of("enabled"), weekday.keySet());
      assertTrue(weekday.getBoolean("enabled"));
   }



   /*******************************************************************************
    ** The material dashboard theme is published from its allow-listed visual
    ** properties only (QRun-IO/qqq#719): string, number and boolean values in
    ** allow-list order; properties without a value, without a getter, off the
    ** allow-list or of another type are left out.  Without the materialDashboard
    ** entry, only the theme is published.
    *******************************************************************************/
   @Test
   void testMaterialDashboardThemeIsPublishedFromItsAllowList()
   {
      serverQInstance.withSupplementalMetaData(new OtherModuleMetaData());
      serverQInstance.withSupplementalMetaData(new TestMaterialDashboardThemeMetaData());

      String     body         = getMetaDataBody();
      JSONObject supplemental = JsonUtils.toJSONObject(body).getJSONObject("supplementalInstanceMetaData");
      assertEquals(Set.of("materialDashboardTheme"), supplemental.keySet());

      JSONObject theme = supplemental.getJSONObject("materialDashboardTheme");
      assertEquals(Set.of("primaryColor", "preferInfoColorToPrimaryColor", "fontWeightBold", "typographyButtonTextTransform", "borderRadiusScale",
         "density", "customCss", "brandedHeaderEnabled", "brandedHeaderTagline", "sidebarBackgroundColor", "tableHeaderBackgroundColor"), theme.keySet());
      assertEquals("#0f766e", theme.getString("primaryColor"));
      assertTrue(theme.getBoolean("preferInfoColorToPrimaryColor"));
      assertEquals(800, theme.getInt("fontWeightBold"));
      assertEquals("none", theme.getString("typographyButtonTextTransform"));
      assertEquals("1.5", theme.getString("borderRadiusScale"));
      assertEquals("compact", theme.getString("density"));
      assertEquals(".qqq-themed h1 { letter-spacing: 1px; }", theme.getString("customCss"));
      assertTrue(theme.getBoolean("brandedHeaderEnabled"));
      assertEquals("Owned tagline", theme.getString("brandedHeaderTagline"));
      assertEquals("#1f2937", theme.getString("sidebarBackgroundColor"));
      assertEquals("#e0f2f1", theme.getString("tableHeaderBackgroundColor"));
      assertThat(new ArrayList<>(theme.keySet())).isSubsetOf(SupplementalInstanceMetaData.THEME_PROPERTY_NAMES);
      assertThat(body.indexOf("\"primaryColor\"")).isLessThan(body.indexOf("\"fontWeightBold\""));
      assertThat(body.indexOf("\"density\"")).isLessThan(body.indexOf("\"tableHeaderBackgroundColor\""));

      assertNoServerConfig(body);
      assertThat(body).doesNotContain(TestMaterialDashboardThemeMetaData.SECRET_SETTING_VALUE);
      assertThat(body).doesNotContain("iconStyle");
      assertThat(body).doesNotContain("secondaryColor");
   }



   /*******************************************************************************
    ** Instance-level help content is published by slot (e.g., for the query
    ** screen's bulk-add-filter-values dialog); without any, the key is omitted.
    *******************************************************************************/
   @Test
   void testInstanceHelpContents()
   {
      assertFalse(JsonUtils.toJSONObject(getMetaDataBody()).has("helpContents"));

      serverQInstance.withHelpContent("bulkAddFilterValues", new QHelpContent().withContentAsText("Paste one value per line.").withRole(QHelpRole.QUERY_SCREEN));
      JSONArray slot = JsonUtils.toJSONObject(getMetaDataBody()).getJSONObject("helpContents").getJSONArray("bulkAddFilterValues");
      assertEquals(1, slot.length());
      assertEquals("Paste one value per line.", slot.getJSONObject(0).getString("content"));
      assertEquals(List.of("QUERY_SCREEN"), slot.getJSONObject(0).getJSONArray("roles").toList());
   }



   /*******************************************************************************
    ** With both entries, both are published; the materialDashboard settings are
    ** unchanged by the theme.
    *******************************************************************************/
   @Test
   void testMaterialDashboardSettingsAndThemeArePublishedTogether()
   {
      serverQInstance.withSupplementalMetaData(new TestMaterialDashboardMetaData()
         .withProcessNamesToAddToAllQueryAndViewScreens(List.of(TestUtils.PROCESS_NAME_GREET_PEOPLE_INTERACTIVE)));
      serverQInstance.withSupplementalMetaData(new TestMaterialDashboardThemeMetaData());

      JSONObject supplemental = JsonUtils.toJSONObject(getMetaDataBody()).getJSONObject("supplementalInstanceMetaData");
      assertEquals(Set.of("materialDashboard", "materialDashboardTheme"), supplemental.keySet());
      assertThat(supplemental.getJSONObject("materialDashboard").getJSONArray("processNamesToAddToAllQueryAndViewScreens").toList())
         .containsExactly(TestUtils.PROCESS_NAME_GREET_PEOPLE_INTERACTIVE);
      assertEquals("#0f766e", supplemental.getJSONObject("materialDashboardTheme").getString("primaryColor"));
   }



   /*******************************************************************************
    ** The allow-list names every visual property of the material dashboard's
    ** theme meta-data (its TypeScript model), each once.
    *******************************************************************************/
   @Test
   void testThemePropertyAllowList()
   {
      List<String> names = SupplementalInstanceMetaData.THEME_PROPERTY_NAMES;
      assertEquals(111, names.size());
      assertEquals(names.size(), Set.copyOf(names).size());
      assertThat(names).contains("primaryColor", "preferInfoColorToPrimaryColor", "monoFontFamily", "typographyH1FontSize", "typographyCaptionTextTransform",
         "borderRadiusTooltip", "density", "customCss", "iconStyle", "brandedHeaderLogoAltText", "appBarTextColor", "sidebarDividerColor",
         "tableBorderColor", "cardBorderColor");
   }



   /*******************************************************************************
    ** Browser picker settings are public, but OAuth client secrets are not.
    *******************************************************************************/
   @Test
   void testGoogleDriveEnvironmentValuesAreAllowListed()
   {
      serverQInstance.getEnvironmentValues().clear();
      serverQInstance.getEnvironmentValues().put("GOOGLE_APP_CLIENT_ID", "owned-client");
      serverQInstance.getEnvironmentValues().put("GOOGLE_APP_API_KEY", "owned-browser-key");
      serverQInstance.getEnvironmentValues().put("GOOGLE_APP_CLIENT_SECRET", "never-published-client-secret");
      String body = getMetaDataBody();
      JSONObject values = JsonUtils.toJSONObject(body).getJSONObject("environmentValues");
      assertEquals(Set.of("GOOGLE_APP_CLIENT_ID", "GOOGLE_APP_API_KEY"), values.keySet());
      assertEquals("owned-client", values.getString("GOOGLE_APP_CLIENT_ID"));
      assertEquals("owned-browser-key", values.getString("GOOGLE_APP_API_KEY"));
      assertThat(body).doesNotContain("GOOGLE_APP_CLIENT_SECRET").doesNotContain("never-published-client-secret");
   }



   /*******************************************************************************
    ** Only the analytics environment values are published (QRun-IO/qqq#730):
    ** the named settings and the ANALYTICS_* namespace, never anything else from
    ** the environment.
    *******************************************************************************/
   @Test
   void testAnalyticsEnvironmentValuesAreAllowListed()
   {
      Map<String, String> environmentValues = serverQInstance.getEnvironmentValues();
      environmentValues.clear();
      environmentValues.put("ANALYTICS_PROVIDERS", "google,posthog,owned");
      environmentValues.put("ANALYTICS_PLUGIN_SCRIPTS", "https://cdn.example.com/owned.js");
      environmentValues.put("ANALYTICS_OWNED_SETTING", "owned-plugin-setting");
      environmentValues.put("GOOGLE_ANALYTICS_ENABLED", "true");
      environmentValues.put("GOOGLE_ANALYTICS_TRACKING_ID", "G-OWNED");
      environmentValues.put("POSTHOG_ENABLED", "true");
      environmentValues.put("POSTHOG_API_KEY", "phc_owned");
      environmentValues.put("POSTHOG_HOST", "https://eu.i.posthog.com");
      environmentValues.put("DATABASE_PASSWORD", "never-published-secret");
      environmentValues.put("GOOGLE_ANALYTICS_API_SECRET", "never-published-api-secret");
      environmentValues.put("POSTHOG_PERSONAL_API_KEY", "never-published-personal-key");

      String     body   = getMetaDataBody();
      JSONObject values = JsonUtils.toJSONObject(body).getJSONObject("environmentValues");
      assertEquals(Set.of("ANALYTICS_PROVIDERS", "ANALYTICS_PLUGIN_SCRIPTS", "ANALYTICS_OWNED_SETTING", "GOOGLE_ANALYTICS_ENABLED",
         "GOOGLE_ANALYTICS_TRACKING_ID", "POSTHOG_ENABLED", "POSTHOG_API_KEY", "POSTHOG_HOST"), values.keySet());
      assertEquals("google,posthog,owned", values.getString("ANALYTICS_PROVIDERS"));
      assertEquals("owned-plugin-setting", values.getString("ANALYTICS_OWNED_SETTING"));
      assertEquals("G-OWNED", values.getString("GOOGLE_ANALYTICS_TRACKING_ID"));
      assertEquals("https://eu.i.posthog.com", values.getString("POSTHOG_HOST"));
      assertThat(body).doesNotContain("DATABASE_PASSWORD").doesNotContain("never-published");
   }



   /*******************************************************************************
    ** Without any allow-listed value, environmentValues is omitted.
    *******************************************************************************/
   @Test
   void testEnvironmentValuesOmittedWhenNoneAreAllowListed()
   {
      serverQInstance.getEnvironmentValues().clear();
      assertFalse(JsonUtils.toJSONObject(getMetaDataBody()).has("environmentValues"));

      serverQInstance.getEnvironmentValues().put("DATABASE_PASSWORD", "never-published-secret");
      String body = getMetaDataBody();
      assertFalse(JsonUtils.toJSONObject(body).has("environmentValues"));
      assertThat(body).doesNotContain("never-published-secret");
   }



   /*******************************************************************************
    ** GET the v1 meta-data, asserting success.
    *******************************************************************************/
   private String getMetaDataBody()
   {
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/metaData").asString();
      assertEquals(200, response.getStatus(), response.getBody());
      return (response.getBody());
   }



   /*******************************************************************************
    ** Assert that none of the server configuration held in supplemental
    ** meta-data appears in a response body.
    *******************************************************************************/
   private static void assertNoServerConfig(String body)
   {
      assertThat(body).doesNotContain("routeProviders");
      assertThat(body).doesNotContain("fileSystemPath");
      assertThat(body).doesNotContain("hostedPath");
      assertThat(body).doesNotContain(TestUtils.STATIC_SITE_PATH);
      assertThat(body).doesNotContain("statically-served");
      assertThat(body).doesNotContain("SimpleRouteAuthenticator");
      assertThat(body.toLowerCase()).doesNotContain("authenticator");
      assertThat(body).doesNotContain("uploadedFileArchive");
      assertThat(body.toLowerCase()).doesNotContain("javalin");
      assertThat(body).doesNotContain(OtherModuleMetaData.NAME);
      assertThat(body).doesNotContain(OtherModuleMetaData.SECRET_VALUE);
   }



   /*******************************************************************************
    ** Stand-in for the material dashboard module's supplemental meta-data (which
    ** this module does not depend on), with one setting that must not be
    ** published.
    *******************************************************************************/
   public static class TestMaterialDashboardMetaData implements QSupplementalInstanceMetaData
   {
      public static final String SECRET_SETTING_VALUE = "material-dashboard-secret-setting";

      private List<String>                processNamesToAddToAllQueryAndViewScreens = new ArrayList<>();
      private TestWeekdayCriteriaSettings weekdayCriteriaSettings;



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public String getName()
      {
         return ("materialDashboard");
      }



      /*******************************************************************************
       ** Getter for processNamesToAddToAllQueryAndViewScreens
       *******************************************************************************/
      public List<String> getProcessNamesToAddToAllQueryAndViewScreens()
      {
         return (this.processNamesToAddToAllQueryAndViewScreens);
      }



      /*******************************************************************************
       ** Fluent setter for processNamesToAddToAllQueryAndViewScreens
       *******************************************************************************/
      public TestMaterialDashboardMetaData withProcessNamesToAddToAllQueryAndViewScreens(List<String> processNamesToAddToAllQueryAndViewScreens)
      {
         this.processNamesToAddToAllQueryAndViewScreens = processNamesToAddToAllQueryAndViewScreens;
         return (this);
      }



      /*******************************************************************************
       ** A setting that is not on the published allow-list.
       *******************************************************************************/
      public String getSecretSetting()
      {
         return (SECRET_SETTING_VALUE);
      }



      /*******************************************************************************
       ** Getter for weekdayCriteriaSettings
       *******************************************************************************/
      public TestWeekdayCriteriaSettings getWeekdayCriteriaSettings()
      {
         return (this.weekdayCriteriaSettings);
      }



      /*******************************************************************************
       ** Fluent setter for weekdayCriteriaSettings
       *******************************************************************************/
      public TestMaterialDashboardMetaData withWeekdayCriteriaSettings(TestWeekdayCriteriaSettings weekdayCriteriaSettings)
      {
         this.weekdayCriteriaSettings = weekdayCriteriaSettings;
         return (this);
      }
   }



   /*******************************************************************************
    ** Stand-in for the material dashboard module's WeekdayCriteriaSettings, with
    ** one setting that must not be published.
    *******************************************************************************/
   public static class TestWeekdayCriteriaSettings implements Serializable
   {
      public static final String SECRET_SETTING_VALUE = "weekday-criteria-secret-setting";

      private Boolean                   enabled                        = true;
      private Map<String, Serializable> dateTimeFieldFunctionArguments = new HashMap<>();



      /*******************************************************************************
       ** Getter for enabled
       *******************************************************************************/
      public Boolean getEnabled()
      {
         return (this.enabled);
      }



      /*******************************************************************************
       ** Fluent setter for enabled
       *******************************************************************************/
      public TestWeekdayCriteriaSettings withEnabled(Boolean enabled)
      {
         this.enabled = enabled;
         return (this);
      }



      /*******************************************************************************
       ** Getter for dateTimeFieldFunctionArguments
       *******************************************************************************/
      public Map<String, Serializable> getDateTimeFieldFunctionArguments()
      {
         return (this.dateTimeFieldFunctionArguments);
      }



      /*******************************************************************************
       ** Fluent setter for dateTimeFieldFunctionArguments
       *******************************************************************************/
      public TestWeekdayCriteriaSettings withDateTimeFieldFunctionArguments(Map<String, Serializable> dateTimeFieldFunctionArguments)
      {
         this.dateTimeFieldFunctionArguments = dateTimeFieldFunctionArguments;
         return (this);
      }



      /*******************************************************************************
       ** A setting that is not on the published allow-list.
       *******************************************************************************/
      public String getSecretSetting()
      {
         return (SECRET_SETTING_VALUE);
      }
   }



   /*******************************************************************************
    ** Stand-in for the material dashboard module's theme meta-data (which this
    ** module does not depend on): registered under that class's name, with
    ** allow-listed properties of each published type, an unset one, a boolean
    ** "is" getter, one of an unpublished type, and a setting off the allow-list.
    *******************************************************************************/
   public static class TestMaterialDashboardThemeMetaData implements QSupplementalInstanceMetaData
   {
      public static final String SECRET_SETTING_VALUE = "material-dashboard-theme-secret-setting";



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public String getName()
      {
         return ("com.kingsrook.qqq.frontend.materialdashboard.model.metadata.MaterialDashboardThemeMetaData");
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public String getPrimaryColor()
      {
         return ("#0f766e");
      }



      /*******************************************************************************
       ** Unset: omitted.
       *******************************************************************************/
      public String getSecondaryColor()
      {
         return (null);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public Boolean getPreferInfoColorToPrimaryColor()
      {
         return (true);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public Integer getFontWeightBold()
      {
         return (800);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public String getTypographyButtonTextTransform()
      {
         return ("none");
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public String getBorderRadiusScale()
      {
         return ("1.5");
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public String getDensity()
      {
         return ("compact");
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public String getCustomCss()
      {
         return (".qqq-themed h1 { letter-spacing: 1px; }");
      }



      /*******************************************************************************
       ** A primitive boolean, read by its "is" getter.
       *******************************************************************************/
      public boolean isBrandedHeaderEnabled()
      {
         return (true);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public String getBrandedHeaderTagline()
      {
         return ("Owned tagline");
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public String getSidebarBackgroundColor()
      {
         return ("#1f2937");
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public String getTableHeaderBackgroundColor()
      {
         return ("#e0f2f1");
      }



      /*******************************************************************************
       ** Allow-listed, but not a string, number or boolean: omitted.
       *******************************************************************************/
      public List<String> getIconStyle()
      {
         return (List.of("filled"));
      }



      /*******************************************************************************
       ** A setting that is not on the published allow-list.
       *******************************************************************************/
      public String getSecretSetting()
      {
         return (SECRET_SETTING_VALUE);
      }
   }



   /*******************************************************************************
    ** Material dashboard supplemental meta-data that also sets the record view
    ** actions placement (an enum, as the material dashboard module's is).
    *******************************************************************************/
   public static class PlacementMaterialDashboardMetaData extends TestMaterialDashboardMetaData
   {
      /*******************************************************************************
       ** Stand-in for the material dashboard's RecordViewActionsPlacement enum.
       *******************************************************************************/
      public enum Placement
      {
         IN_IDENTITY_SECTION,
         INLINE_WITH_PAGE_TITLE
      }



      /*******************************************************************************
       ** Getter for recordViewActionsPlacement
       *******************************************************************************/
      public Placement getRecordViewActionsPlacement()
      {
         return (Placement.INLINE_WITH_PAGE_TITLE);
      }
   }



   /*******************************************************************************
    ** Some other module's supplemental meta-data - none of it is published.
    *******************************************************************************/
   public static class OtherModuleMetaData implements QSupplementalInstanceMetaData
   {
      public static final String NAME         = "otherModuleSupplemental";
      public static final String SECRET_VALUE = "other-module-secret-value";



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public String getName()
      {
         return (NAME);
      }



      /*******************************************************************************
       ** A value that must never be published.
       *******************************************************************************/
      public String getSecretValue()
      {
         return (SECRET_VALUE);
      }
   }

}
