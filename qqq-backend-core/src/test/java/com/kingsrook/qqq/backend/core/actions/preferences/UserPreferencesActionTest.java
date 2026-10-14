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

package com.kingsrook.qqq.backend.core.actions.preferences;


import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.audits.AuditsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.ClientChannelsMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceChange;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceListMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceProviderInterface;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceRequestContext;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceRow;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceRowChange;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceSectionMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceValues;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferencesMetaData;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Core preferences validation, isolation, storage and audits.
 *******************************************************************************/
class UserPreferencesActionTest extends BaseTest
{
   private UserPreferenceSectionMetaData section;
   private final UserPreferencesAction action = new UserPreferencesAction();



   /*******************************************************************************
    ** Build a memory store and typed section.
    *******************************************************************************/
   @BeforeEach
   void setupPreferences() throws Exception
   {
      new UserPreferencesKeyValueStoreMetaDataProvider().defineAll(QContext.getQInstance(), TestUtils.MEMORY_BACKEND_NAME, null);
      new AuditsMetaDataProvider().withClientChannelField(true).defineAll(QContext.getQInstance(), TestUtils.MEMORY_BACKEND_NAME, null);
      QContext.getQInstance().addPossibleValueSource(new QPossibleValueSource().withName("themeChoices")
         .withEnumValues(List.of(new QPossibleValue<>("light", "Light"), new QPossibleValue<>("dark", "Dark"))));
      section = new UserPreferenceSectionMetaData().withName("display").withLabel("Display").withIcon(new QIcon("settings"))
         .withField(new QFieldMetaData("theme", QFieldType.STRING).withDefaultValue("light").withPossibleValueSourceName("themeChoices"))
         .withField(new QFieldMetaData("title", QFieldType.STRING).withMaxLength(5))
         .withField(new QFieldMetaData("count", QFieldType.INTEGER).withDefaultValue(2).withIsRequired(true))
         .withField(new QFieldMetaData("enabled", QFieldType.BOOLEAN))
         .withField(new QFieldMetaData("locked", QFieldType.STRING).withIsEditable(false));
      UserPreferencesMetaData.ofOrWithNew(QContext.getQInstance()).withSection(section);
      new QInstanceEnricher(QContext.getQInstance()).enrich();
      FixedProvider.saved.clear();
      FixedProvider.stores = 0;
   }



   /*******************************************************************************
    ** Defaults, insert, update, explicit null and user isolation survive reload.
    *******************************************************************************/
   @Test
   void testRoundTripAndIsolation() throws Exception
   {
      assertEquals("light", action.getSection("display").getValues().getValues().get("theme"));
      assertEquals(2, action.getSection("display").getValues().getValues().get("count"));
      action.patchSection("display", Map.of("values", Map.of("theme", "dark", "count", "3")));
      assertEquals(3, action.getSection("display").getValues().getValues().get("count"));
      action.patchSection("display", Map.of("values", Map.of("count", 4)));
      assertEquals("dark", action.getSection("display").getValues().getValues().get("theme"));
      QContext.setQSession(newSession("B"));
      assertEquals("light", action.getSection("display").getValues().getValues().get("theme"));
      action.patchSection("display", Map.of("values", Map.of("count", 8)));
      assertEquals(8, action.getSection("display").getValues().getValues().get("count"));
      QContext.setQSession(newSession());
      assertEquals(4, action.getSection("display").getValues().getValues().get("count"));
      Map<String, Object> nullValue = new HashMap<>();
      nullValue.put("theme", null);
      assertNull(action.patchSection("display", Map.of("values", nullValue)).getValues().getValues().get("theme"));
      assertNull(action.getSection("display").getValues().getValues().get("theme"));
   }



   /*******************************************************************************
    ** The user can change preferences without any permission on their backing table.
    *******************************************************************************/
   @Test
   void testTrustedAndAudited() throws Exception
   {
      QContext.getQInstance().setEnforcePermissionsInCoreActions(true);
      QContext.getQInstance().getTable("user_preference").setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      QContext.getQSession().withClientChannel("cli/test");
      assertThrows(QPermissionDeniedException.class, () -> new QueryAction().execute(new QueryInput("user_preference")));
      action.patchSection("display", Map.of("values", Map.of("count", 5)));
      action.patchSection("display", Map.of("values", Map.of("count", 6)));
      assertEquals(6, action.getSection("display").getValues().getValues().get("count"));
      List<QRecord> audits = new QueryAction().execute((QueryInput) new QueryInput("audit").withPermissionBypass("test audit inspection")).getRecords();
      assertThat(audits).hasSize(2).allMatch(r -> "cli/test".equals(r.getValueString("clientChannel")));
      List<QRecord> users = new QueryAction().execute((QueryInput) new QueryInput("auditUser").withPermissionBypass("test audit inspection")).getRecords();
      assertThat(users).hasSize(1);
      assertEquals(QContext.getQSession().getUser().getFullName(), users.get(0).getValueString("name"));
      assertThat(audits).allMatch(r -> r.getValue("auditUserId").equals(users.get(0).getValue("id")));
      assertFalse(new QueryAction().execute((QueryInput) new QueryInput("auditDetail").withPermissionBypass("test audit inspection")).getRecords().isEmpty());
      assertEquals(0, QContext.getAuthorizedActionScopeDepth());
   }



   /*******************************************************************************
    ** Reject every category of bad scalar input.
    *******************************************************************************/
   @ParameterizedTest
   @CsvSource({"unknown,x,isn't a setting here", "locked,x,can't be changed", "count,no,must be a whole number", "enabled,maybe,must be true or false", "title,abcdef,must be at most 5 characters", "theme,blue,isn't one of the choices", "userId,B,isn't a setting here"})
   void testScalarValidation(String name, String value, String error)
   {
      assertThat(assertThrows(QBadRequestException.class, () -> action.patchSection("display", Map.of("values", Map.of(name, value)))).getMessage()).contains(error);
   }



   /*******************************************************************************
    ** Reject unknown top-level properties, including user identity.
    *******************************************************************************/
   @Test
   void testTopLevelProperties()
   {
      for(String key : List.of("userId", "other"))
      {
         assertThat(assertThrows(QBadRequestException.class, () -> action.patchSection("display", Map.of(key, "B"))).getMessage()).contains("Unexpected property: " + key);
      }
   }



   /*******************************************************************************
    ** Required values cannot be cleared.
    *******************************************************************************/
   @Test
   void testRequiredNull()
   {
      Map<String, Serializable> values = new HashMap<>();
      values.put("count", null);
      assertThat(assertThrows(QBadRequestException.class, () -> action.patchSection("display", new UserPreferenceChange().withValues(values))).getMessage()).contains("Count: is required");
   }



   /*******************************************************************************
    ** All messages are returned together and no valid subset is written.
    *******************************************************************************/
   @Test
   void testAllErrorsTogether() throws Exception
   {
      String message = assertThrows(QBadRequestException.class, () -> action.patchSection("display", Map.of("userId", "B", "values", Map.of("theme", "invalid", "title", "toolong", "count", "bad", "locked", "x", "absent", 1, "enabled", true), "lists", Map.of("absent", List.of())))).getMessage();
      assertThat(message).contains("userId", "Theme", "Title", "Count", "Locked", "absent: isn't a setting here");
      assertNull(action.getSection("display").getValues().getValues().get("enabled"));
   }



   /*******************************************************************************
    ** Known, hidden and uninstalled sections are indistinguishable to callers.
    *******************************************************************************/
   @Test
   void testVisibility() throws Exception
   {
      assertThrows(QNotFoundException.class, () -> action.getSection("absent"));
      section.withPermissionName("prefs.use");
      assertTrue(action.listSections().isEmpty());
      assertThrows(QNotFoundException.class, () -> action.getSection("display"));
      assertThrows(QNotFoundException.class, () -> action.patchSection("display", Map.of()));
      QContext.getQSession().withPermissions("prefs.use");
      assertEquals(1, action.listSections().size());
      section.withAppName("app");
      assertThrows(QNotFoundException.class, () -> action.getSection("display"));
      QContext.getQInstance().addApp(new QAppMetaData().withName("app").withLabel("App")
         .withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION)));
      assertThrows(QNotFoundException.class, () -> action.getSection("display"));
      QContext.getQSession().withPermissions("prefs.use", "app.hasAccess");
      assertNotNull(action.getSection("display"));
      ClientChannelsMetaData.ofOrWithNew(QContext.getQInstance()).withDeniedObject("assistant", "app");
      QContext.getQSession().withClientChannel("assistant");
      assertThrows(QNotFoundException.class, () -> action.getSection("display"));
      assertThrows(QNotFoundException.class, () -> action.patchSection("display", Map.of()));
   }



   /*******************************************************************************
    ** Display ordering is numeric and then alphabetical by label.
    *******************************************************************************/
   @Test
   void testOrdering() throws Exception
   {
      UserPreferencesMetaData.of(QContext.getQInstance())
         .withSection(new UserPreferenceSectionMetaData().withName("z").withLabel("Z").withOrder(1))
         .withSection(new UserPreferenceSectionMetaData().withName("a").withLabel("A").withOrder(1));
      assertEquals(List.of("a", "z", "display"), action.listSections().stream().map(r -> r.getSection().getName()).toList());
   }



   /*******************************************************************************
    ** No user or user id can be supplied outside the session.
    *******************************************************************************/
   @Test
   void testContextRefusesMissingUser()
   {
      assertThrows(IllegalArgumentException.class, () -> new UserPreferenceRequestContext(section, null));
      assertThrows(IllegalArgumentException.class, () -> new UserPreferenceRequestContext(section, new com.kingsrook.qqq.backend.core.model.session.QSession()));
      UserPreferenceRequestContext context = new UserPreferenceRequestContext(section, QContext.getQSession());
      assertSame(QContext.getQSession(), context.getSession());
      assertSame(section, context.getSection());
      assertEquals(DEFAULT_USER_ID, context.getUserId());
   }



   /*******************************************************************************
    ** Install a fixed-row provider.
    *******************************************************************************/
   private void useLists()
   {
      section.withProvider(new QCodeReference(FixedProvider.class))
         .withList(new UserPreferenceListMetaData().withName("categories").withLabel("Categories").withRowLabel("Category")
            .withField(new QFieldMetaData("enabled", QFieldType.BOOLEAN).withDefaultValue(true))
            .withField(new QFieldMetaData("severity", QFieldType.INTEGER)));
   }



   /*******************************************************************************
    ** Patch body for one fixed row.
    *******************************************************************************/
   private Map<String, Object> row(String key, Map<String, Object> values)
   {
      return (Map.of("lists", Map.of("categories", List.of(Map.of("key", key, "values", values)))));
   }



   /*******************************************************************************
    ** Unknown rows cannot be created.
    *******************************************************************************/
   @Test
   void testUnknownRow()
   {
      useLists();
      assertThat(assertThrows(QBadRequestException.class, () -> action.patchSection("display", row("absent", Map.of("enabled", false)))).getMessage()).contains("Categories, absent: isn't a setting here");
   }



   /*******************************************************************************
    ** Read-only fields can vary by provider row.
    *******************************************************************************/
   @Test
   void testReadOnlyRow()
   {
      useLists();
      assertThat(assertThrows(QBadRequestException.class, () -> action.patchSection("display", row("security", Map.of("enabled", false)))).getMessage()).contains("Categories, Security: Enabled: can't be changed: Security notices cannot be muted");
      assertEquals(0, FixedProvider.stores);
   }



   /*******************************************************************************
    ** Unknown list names are refused.
    *******************************************************************************/
   @Test
   void testUnknownList()
   {
      assertThat(assertThrows(QBadRequestException.class, () -> action.patchSection("display", Map.of("lists", Map.of("absent", List.of())))).getMessage()).contains("absent: isn't a setting here");
   }



   /*******************************************************************************
    ** Custom providers receive coerced partial values; domain validation runs before store.
    *******************************************************************************/
   @Test
   void testListsAndDomainValidation() throws Exception
   {
      useLists();
      assertEquals(true, action.getSection("display").getValues().getLists().get("categories").get(1).getValues().get("enabled"));
      action.patchSection("display", row("news", Map.of("enabled", false, "severity", "3")));
      assertEquals(3, FixedProvider.saved.get("severity"));
      assertEquals(false, action.getSection("display").getValues().getLists().get("categories").get(1).getValues().get("enabled"));
      assertThat(assertThrows(QBadRequestException.class, () -> action.patchSection("display", Map.of("values", Map.of("title", "bad")))).getMessage()).contains("domain one", "domain two");
      assertEquals(1, FixedProvider.stores);
      action.patchSection("display", new UserPreferenceChange().withLists(Map.of("categories", List.of(new UserPreferenceRowChange().withKey("news").withValues(Map.of("severity", 4))))));
      assertEquals(4, FixedProvider.saved.get("severity"));
      assertEquals(false, FixedProvider.saved.get("enabled"));
   }



   /*******************************************************************************
    ** Malformed structures, nested identity and duplicate rows are rejected together.
    *******************************************************************************/
   @Test
   void testMalformedPatchShapes()
   {
      useLists();
      for(Map<String, Object> body : List.of(Map.<String, Object>of("values", "bad"), Map.<String, Object>of("lists", "bad"), Map.<String, Object>of("lists", Map.of("categories", "bad")), Map.<String, Object>of("lists", Map.of("categories", List.of("bad"))), Map.<String, Object>of("values", Map.of("title", List.of("nested"))), Map.<String, Object>of("lists", Map.of("categories", List.of(Map.of("key", "news", "userId", "B", "values", Map.of())))), Map.<String, Object>of("lists", Map.of("categories", List.of(Map.of("key", "news", "values", Map.of()), Map.of("key", "news", "values", Map.of()))))))
      {
         assertThrows(QBadRequestException.class, () -> action.patchSection("display", body));
      }
   }



   /*******************************************************************************
    ** Labels, rather than implementation names, identify every invalid value.
    *******************************************************************************/
   @ParameterizedTest
   @CsvSource({"INTEGER,must be a whole number", "DECIMAL,must be a number", "BOOLEAN,must be true or false", "DATE,must be a date", "DATE_TIME,must be a date and time", "TIME,must be a time"})
   void testLabeledTypeErrors(QFieldType type, String problem)
   {
      section.withField(new QFieldMetaData("typed", type).withLabel("Friendly label"));
      assertEquals("Friendly label: " + problem, assertThrows(QBadRequestException.class,
         () -> action.patchSection("display", Map.of("values", Map.of("typed", "invalid")))).getMessage());
   }



   /*******************************************************************************
    ** Fixed rows identify their list, row and field without exposing wire paths.
    *******************************************************************************/
   @Test
   void testLabeledRowErrors()
   {
      useLists();
      assertEquals("Categories, News: Severity: must be a whole number", assertThrows(QBadRequestException.class,
         () -> action.patchSection("display", row("news", Map.of("severity", "bad")))).getMessage());
      assertEquals("Categories, News: absent: isn't a setting here", assertThrows(QBadRequestException.class,
         () -> action.patchSection("display", row("news", Map.of("absent", true)))).getMessage());
      assertEquals("Categories, News: row appears twice", assertThrows(QBadRequestException.class,
         () -> action.patchSection("display", Map.of("lists", Map.of("categories", List.of(
            Map.of("key", "news", "values", Map.of()), Map.of("key", "news", "values", Map.of())))))).getMessage());
      assertEquals(0, FixedProvider.stores);
   }



   /*******************************************************************************
    ** Every supported field type survives a JSON storage round trip.
    *******************************************************************************/
   @ParameterizedTest
   @CsvSource({"TEXT,hello", "DECIMAL,1.25", "DATE,2026-10-13", "TIME,13:14:15", "DATE_TIME,2026-10-13T13:14:15Z", "BOOLEAN,true"})
   void testTypeRoundTrip(QFieldType type, String raw) throws Exception
   {
      section.withField(new QFieldMetaData("typed", type));
      Serializable expected = com.kingsrook.qqq.backend.core.utils.ValueUtils.getValueAsFieldType(type, raw);
      action.patchSection("display", Map.of("values", Map.of("typed", raw)));
      assertEquals(expected, action.getSection("display").getValues().getValues().get("typed"));
   }



   /*******************************************************************************
    ** A custom provider with two fixed rows and a read-only security switch.
    *******************************************************************************/
   public static class FixedProvider implements UserPreferenceProviderInterface
   {
      static Map<String, Serializable> saved = new LinkedHashMap<>();
      static int stores;



      /*******************************************************************************
       ** Return independent rows to prevent validation mutating persistent values.
       *******************************************************************************/
      @Override
      public UserPreferenceValues load(UserPreferenceRequestContext context)
      {
         return (new UserPreferenceValues().withValues(Map.of()).withLists(Map.of("categories", List.of(
            new UserPreferenceRow().withKey("security").withLabel("Security").withDescription("Required notices").withReadOnlyFieldNames(List.of("enabled")).withReadOnlyReason("Security notices cannot be muted"),
            new UserPreferenceRow().withKey("news").withLabel("News").withValues(Map.copyOf(saved))))));
      }



      /*******************************************************************************
       ** Domain errors are returned together.
       *******************************************************************************/
      @Override
      public List<String> validate(UserPreferenceRequestContext context, UserPreferenceChange change)
      {
         return ("bad".equals(change.getValues().get("title")) ? List.of("domain one", "domain two") : List.of());
      }



      /*******************************************************************************
       ** Assert trusted scope without a system-user substitution.
       *******************************************************************************/
      @Override
      public void store(UserPreferenceRequestContext context, UserPreferenceChange change)
      {
         assertTrue(QContext.getAuthorizedActionScopeDepth() > 0);
         assertEquals(DEFAULT_USER_ID, context.getUserId());
         stores++;
         for(UserPreferenceRowChange row : change.getLists().getOrDefault("categories", List.of()))
         {
            saved.putAll(row.getValues());
         }
      }
   }
}
