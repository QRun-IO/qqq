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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.preferences.UserPreferencesKeyValueStoreMetaDataProvider;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.audits.AuditsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.TableBasedAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceActionMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceActionRequest;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceActionResult;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceActionScope;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceChange;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceListMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceProviderInterface;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceRequestContext;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceRow;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceSectionMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceValues;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferencesMetaData;
import com.kingsrook.qqq.backend.core.model.secrets.OneTimeSecret;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.TableBasedAuthenticationModule;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Preferences security and contracts over real authenticated HTTP.
 *******************************************************************************/
class PreferencesSpecV1Test extends SpecTestBase
{
   /*******************************************************************************
    ** Register all three preference routes with real session management.
    *******************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return (new PreferencesSpecV1());
   }



   /*******************************************************************************
    ** Additional routes for HTTP tests.
    *******************************************************************************/
   @Override
   protected List<AbstractEndpointSpec<?, ?, ?>> getAdditionalSpecs()
   {
      return (List.of(new PreferenceSectionActionSpecV1(), new PreferenceSectionGetSpecV1(), new PreferenceSectionPatchSpecV1(), new ManageSessionSpecV1()));
   }



   /*******************************************************************************
    ** Version under test.
    *******************************************************************************/
   @Override
   protected String getVersion()
   {
      return ("v1");
   }



   /*******************************************************************************
    ** Memory-backed metadata with a protected store and one hidden section.
    *******************************************************************************/
   @Override
   protected QInstance defineQInstance() throws QException
   {
      QInstance instance = TestUtils.defineInstance();
      TableBasedAuthenticationMetaData auth = new TableBasedAuthenticationMetaData();
      instance.addTable(auth.defineStandardUserTable(TestUtils.BACKEND_NAME_MEMORY));
      instance.addTable(auth.defineStandardSessionTable(TestUtils.BACKEND_NAME_MEMORY));
      new AuditsMetaDataProvider().withClientChannelField(true).defineAll(instance, TestUtils.BACKEND_NAME_MEMORY, null);
      new UserPreferencesKeyValueStoreMetaDataProvider().defineAll(instance, TestUtils.BACKEND_NAME_MEMORY, null);
      instance.getTable("user_preference").setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      instance.setEnforcePermissionsInCoreActions(true);
      instance.addPossibleValueSource(new QPossibleValueSource().withName("themes").withEnumValues(List.of(new QPossibleValue<>("light", "Light"), new QPossibleValue<>("dark", "Dark"))));
      instance.addPossibleValueSource(new QPossibleValueSource().withName("actionChoices").withEnumValues(List.of(new QPossibleValue<>("short", "Short-lived"))));
      UserPreferencesMetaData.of(instance)
         .withSection(new UserPreferenceSectionMetaData().withName("display").withShowInUserMenu(true).withLabel("Display").withDescription("Display preferences").withIcon(new QIcon("settings"))
            .withField(new QFieldMetaData("theme", QFieldType.STRING).withLabel("Theme").withDefaultValue("light").withPossibleValueSourceName("themes"))
            .withField(new QFieldMetaData("count", QFieldType.INTEGER).withLabel("Count").withDefaultValue(2).withIsRequired(true))
            .withField(new QFieldMetaData("title", QFieldType.STRING).withLabel("Title").withMaxLength(4)))
         .withSection(new UserPreferenceSectionMetaData().withName("secret").withLabel("Restricted").withPermissionName("preferences.restricted"))
         .withSection(new UserPreferenceSectionMetaData().withName("rows").withLabel("Rows").withProvider(new QCodeReference(RowsProvider.class))
            .withAction(new UserPreferenceActionMetaData().withName("rotate").withLabel("Rotate").withIcon(new QIcon("refresh")).withScope(UserPreferenceActionScope.ROW).withListName("categories")
               .withConfirmationText("Rotate this token?").withIsDestructive(true).withOrder(20))
            .withAction(new UserPreferenceActionMetaData().withName("create").withLabel("Create").withScope(UserPreferenceActionScope.LIST).withListName("categories").withOrder(10)
               .withInputField(new QFieldMetaData("duration", QFieldType.STRING).withLabel("Duration").withPossibleValueSourceName("actionChoices")))
            .withList(new UserPreferenceListMetaData().withName("categories").withLabel("Categories").withDescription("Category choices").withRowLabel("Category")
               .withField(new QFieldMetaData("theme", QFieldType.STRING).withLabel("Theme").withPossibleValueSourceName("themes"))));
      return (instance);
   }



   /*******************************************************************************
    ** Create two separate users then switch to table-based authentication.
    *******************************************************************************/
   @Override
   protected void primeTestData(QInstance instance) throws Exception
   {
      super.primeTestData(instance);
      QContext.init(instance, new QSession());
      for(String name : List.of("alice", "bob"))
      {
         TestUtils.insertRecords(instance, instance.getTable("user"), List.of(new QRecord().withValue("username", name)
            .withValue("fullName", name).withValue("passwordHash", TableBasedAuthenticationModule.PasswordHasher.createHashedPassword("test-password"))));
      }
      instance.registerAuthenticationProvider(AuthScope.instanceDefault(), new TableBasedAuthenticationMetaData());
   }



   /*******************************************************************************
    ** Each request explicitly selects its session.
    *******************************************************************************/
   @BeforeEach
   void noSharedCookies()
   {
      Unirest.config().reset().enableCookieManagement(false);
   }



   /*******************************************************************************
    ** Restore client defaults for other spec tests.
    *******************************************************************************/
   @AfterAll
   static void resetClient()
   {
      Unirest.config().reset();
   }



   /*******************************************************************************
    ** Sign in and obtain a session cookie through the real server.
    *******************************************************************************/
   private String login(String name)
   {
      String auth = Base64.getEncoder().encodeToString((name + ":test-password").getBytes(StandardCharsets.UTF_8));
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/manageSession").header("Authorization", "Basic " + auth)
         .header("Content-Type", "application/json").body("{}").asString();
      assertEquals(200, response.getStatus(), response.getBody());
      return ("sessionUUID=" + response.getCookies().getNamed("sessionUUID").getValue());
   }



   /*******************************************************************************
    ** GET with an explicit session.
    *******************************************************************************/
   private HttpResponse<String> get(String cookie, String path)
   {
      return (Unirest.get(getBaseUrlAndPath() + "/preferences" + path).header("Cookie", cookie).asString());
   }



   /*******************************************************************************
    ** PATCH with an explicit session and client attribution.
    *******************************************************************************/
   private HttpResponse<String> patch(String cookie, String path, String json)
   {
      return (Unirest.patch(getBaseUrlAndPath() + "/preferences" + path).header("Cookie", cookie)
         .header("X-QQQ-Client", "cli/test").header("Content-Type", "application/json").body(json).asString());
   }



   /*******************************************************************************
    ** Parse a successful response.
    *******************************************************************************/
   private JSONObject ok(HttpResponse<String> response)
   {
      assertEquals(200, response.getStatus(), response.getBody());
      return (new JSONObject(response.getBody()));
   }



   /*******************************************************************************
    ** List and detail include field components, icons, inline enums and fixed row metadata.
    *******************************************************************************/
   @Test
   void testGetContracts()
   {
      String cookie = login("alice");
      JSONObject all = ok(get(cookie, ""));
      assertEquals(2, all.getJSONArray("sections").length());
      JSONObject section = ok(get(cookie, "/display"));
      assertTrue(section.getBoolean("showInUserMenu"));
      assertFalse(ok(get(cookie, "/rows")).getBoolean("showInUserMenu"));
      assertEquals("settings", section.getJSONObject("icon").getString("name"));
      assertEquals("STRING", section.getJSONArray("fields").getJSONObject(0).getString("type"));
      assertEquals("light", section.getJSONObject("values").getString("theme"));
      assertEquals(2, section.getJSONObject("possibleValues").getJSONArray("themes").length());
      assertTrue(section.getJSONObject("values").has("title"));
      assertTrue(section.getJSONObject("values").isNull("title"));
      JSONObject rows = ok(get(cookie, "/rows"));
      assertEquals(2, rows.getJSONObject("possibleValues").getJSONArray("themes").length());
      JSONObject row = rows.getJSONArray("lists").getJSONObject(0).getJSONArray("rows").getJSONObject(0);
      assertEquals("security", row.getString("key"));
      assertEquals("theme", row.getJSONArray("readOnlyFields").getString(0));
      assertEquals("Fixed by policy", row.getString("readOnlyReason"));
   }



   /*******************************************************************************
    ** Two authenticated users write and read only their own settings, including partial updates.
    *******************************************************************************/
   @Test
   void testTwoUsers()
   {
      String alice = login("alice");
      String bob = login("bob");
      ok(patch(alice, "/display", "{\"values\":{\"theme\":\"dark\",\"count\":7}}"));
      ok(patch(bob, "/display", "{\"values\":{\"theme\":\"light\",\"count\":9}}"));
      ok(patch(alice, "/display", "{\"values\":{\"title\":\"A\"}}"));
      assertEquals(7, ok(get(alice, "/display")).getJSONObject("values").getInt("count"));
      assertEquals("dark", ok(get(alice, "/display")).getJSONObject("values").getString("theme"));
      assertEquals(9, ok(get(bob, "/display")).getJSONObject("values").getInt("count"));
      assertTrue(ok(get(bob, "/display")).getJSONObject("values").isNull("title"));
      assertEquals(7, ok(get(alice, "")).getJSONArray("sections").getJSONObject(0).getJSONObject("values").getInt("count"));
   }



   /*******************************************************************************
    ** Neither top-level nor values userId can change identity or write any valid subset.
    *******************************************************************************/
   @Test
   void testIdentityInjection()
   {
      String alice = login("alice");
      String bob = login("bob");
      ok(patch(bob, "/display", "{\"values\":{\"count\":9}}"));
      for(String body : List.of("{\"userId\":\"2\",\"values\":{\"count\":99}}", "{\"values\":{\"userId\":\"2\",\"count\":99}}"))
      {
         HttpResponse<String> response = patch(alice, "/display", body);
         assertEquals(400, response.getStatus(), response.getBody());
         assertThat(response.getBody()).contains("userId");
         assertEquals(9, ok(get(bob, "/display")).getJSONObject("values").getInt("count"));
         assertEquals(2, ok(get(alice, "/display")).getJSONObject("values").getInt("count"));
      }
   }



   /*******************************************************************************
    ** A query userId never participates in identity resolution.
    *******************************************************************************/
   @Test
   void testQueryIdentityIgnored()
   {
      String alice = login("alice");
      String bob = login("bob");
      ok(patch(bob, "/display", "{\"values\":{\"count\":9}}"));
      ok(patch(alice, "/display?userId=2", "{\"values\":{\"count\":7}}"));
      assertEquals(7, ok(get(alice, "/display?userId=2")).getJSONObject("values").getInt("count"));
      assertEquals(9, ok(get(bob, "/display")).getJSONObject("values").getInt("count"));
   }



   /*******************************************************************************
    ** Hidden and unknown sections return 404 for both reads and writes.
    *******************************************************************************/
   @Test
   void testHiddenAndUnknown()
   {
      String cookie = login("alice");
      for(String section : List.of("secret", "unknown"))
      {
         assertEquals(404, get(cookie, "/" + section).getStatus());
         assertEquals(404, patch(cookie, "/" + section, "{\"values\":{}}").getStatus());
      }
   }



   /*******************************************************************************
    ** Every operation requires authentication.
    *******************************************************************************/
   @Test
   void testUnauthenticated()
   {
      assertEquals(401, get("", "").getStatus());
      assertEquals(401, get("", "/display").getStatus());
      assertEquals(401, patch("", "/display", "{}").getStatus());
   }



   /*******************************************************************************
    ** HTTP returns all validation messages and leaves the section unchanged.
    *******************************************************************************/
   @Test
   void testBadValues()
   {
      String cookie = login("alice");
      HttpResponse<String> response = patch(cookie, "/display", "{\"unexpected\":1,\"values\":{\"theme\":\"bad\",\"count\":null,\"title\":\"too long\"}}");
      assertEquals(400, response.getStatus(), response.getBody());
      assertThat(response.getBody()).contains("Unexpected property: unexpected", "Theme: isn't one of the choices", "Count: is required", "Title: must be at most");
      assertEquals(2, ok(get(cookie, "/display")).getJSONObject("values").getInt("count"));
   }



   /*******************************************************************************
    ** Reject malformed JSON and shapes; preserve explicit null after reload.
    *******************************************************************************/
   @Test
   void testBodyShapesAndNull()
   {
      String cookie = login("alice");
      for(String body : List.of("not json", "[]", "null", "{\"values\":[]}", "{\"lists\":[]}"))
      {
         assertEquals(400, patch(cookie, "/display", body).getStatus(), body);
      }
      ok(patch(cookie, "/display", "{\"values\":{\"theme\":null}}"));
      assertTrue(ok(get(cookie, "/display")).getJSONObject("values").isNull("theme"));
   }



   /*******************************************************************************
    ** POST uses only the chosen session and preserves raw body validation.
    *******************************************************************************/
   private HttpResponse<String> postAction(String cookie, String path, String body)
   {
      return (Unirest.post(getBaseUrlAndPath() + "/preferences" + path).header("Cookie", cookie)
         .header("Content-Type", "application/json").body(body).asString());
   }



   /*******************************************************************************
    ** HTTP actions expose metadata and one-time secrets, but never cross user boundaries.
    *******************************************************************************/
   @Test
   void testActions()
   {
      String alice = login("alice");
      String bob = login("bob");
      JSONObject section = ok(get(alice, "/rows"));
      assertEquals(1, section.getJSONObject("possibleValues").getJSONArray("actionChoices").length());
      JSONObject create = section.getJSONArray("actions").getJSONObject(0);
      assertEquals("create", create.getString("name"));
      assertEquals("LIST", create.getString("scope"));
      assertEquals("duration", create.getJSONArray("inputFields").getJSONObject(0).getString("name"));
      JSONObject rotate = section.getJSONArray("actions").getJSONObject(1);
      assertEquals("ROW", rotate.getString("scope"));
      assertEquals("categories", rotate.getString("listName"));
      assertEquals("refresh", rotate.getJSONObject("icon").getString("name"));
      assertTrue(rotate.getBoolean("isDestructive"));
      assertEquals("Rotate this token?", rotate.getString("confirmationText"));
      var rows = section.getJSONArray("lists").getJSONObject(0).getJSONArray("rows");
      assertEquals(0, rows.getJSONObject(0).getJSONArray("actionNames").length());
      assertEquals("rotate", rows.getJSONObject(1).getJSONArray("actionNames").getString(0));
      assertTrue(rows.getJSONObject(2).isNull("actionNames"));
      String rowKey = rows.getJSONObject(1).getString("key");
      String body = new JSONObject().put("rowKey", rowKey).toString();
      HttpResponse<String> response = postAction(alice, "/rows/actions/rotate", body);
      JSONObject result = ok(response);
      assertEquals("no-store", response.getHeaders().getFirst("Cache-Control"));
      assertEquals("Done", result.getString("message"));
      assertEquals("rows", result.getJSONObject("section").getString("name"));
      assertEquals(rowKey, result.getString("focusRowKey"));
      assertEquals("fixture-only", result.getJSONArray("secrets").getJSONObject(0).getString("value"));
      assertEquals("Token", result.getJSONArray("secrets").getJSONObject(0).getString("label"));
      assertEquals("qqq_…7f3a", result.getJSONArray("secrets").getJSONObject(0).getString("masked"));
      assertEquals("curl example\nCopy it now", result.getJSONArray("secrets").getJSONObject(0).getString("usageText"));
      assertFalse(result.getJSONArray("secrets").getJSONObject(0).has("help"));
      assertThat(get(alice, "/rows").getBody()).doesNotContain("fixture-only");
      assertThat(get(alice, "").getBody()).doesNotContain("fixture-only");
      assertEquals(400, postAction(bob, "/rows/actions/rotate", body).getStatus());
      assertEquals(400, postAction(alice, "/rows/actions/rotate", "{\"rowKey\":\"security\"}").getStatus());
      ok(postAction(alice, "/rows/actions/create", "{\"values\":{\"duration\":\"short\"}}"));
   }



   /*******************************************************************************
    ** Authentication, not-found and strict request failures apply to POST too.
    *******************************************************************************/
   @Test
   void testActionErrors()
   {
      String alice = login("alice");
      HttpResponse<String> unauthenticated = postAction("", "/rows/actions/create", "{}");
      assertEquals(401, unauthenticated.getStatus());
      assertEquals("no-store", unauthenticated.getHeaders().getFirst("Cache-Control"));
      assertEquals(404, postAction(alice, "/secret/actions/create", "{}").getStatus());
      assertEquals(404, postAction(alice, "/absent/actions/create", "{}").getStatus());
      HttpResponse<String> missing = postAction(alice, "/rows/actions/absent", "{}");
      assertEquals(404, missing.getStatus());
      assertEquals("no-store", missing.getHeaders().getFirst("Cache-Control"));
      assertThat(missing.getBody()).contains("Action not found");
      for(String body : List.of("not json", "[]", "null", "{\"values\":[]}", "{\"rowKey\":\"x\"}", "{\"userId\":\"B\"}", "{\"values\":{\"duration\":\"invalid\"}}", "{\"values\":{\"userId\":\"B\"}}"))
      {
         assertEquals(400, postAction(alice, "/rows/actions/create", body).getStatus(), body);
      }
      assertEquals(400, postAction(alice, "/rows/actions/rotate", "{}").getStatus());
   }



   /*******************************************************************************
    ** Read-only list fixture for the HTTP response contract.
    *******************************************************************************/
   public static class RowsProvider implements UserPreferenceProviderInterface
   {
      /*******************************************************************************
       ** Load a fixed row.
       *******************************************************************************/
      @Override
      public UserPreferenceValues load(UserPreferenceRequestContext context)
      {
         return (new UserPreferenceValues().withLists(Map.of("categories", List.of(new UserPreferenceRow()
            .withKey("security").withLabel("Security").withDescription("Required notices")
            .withValues(Map.of("theme", "light")).withReadOnlyFieldNames(List.of("theme")).withReadOnlyReason("Fixed by policy").withActionNames(Set.of()),
            new UserPreferenceRow().withKey(context.getUserId()).withLabel("My token").withActionNames(Set.of("rotate")),
            new UserPreferenceRow().withKey("all-" + context.getUserId()).withLabel("All actions")))));
      }



      /*******************************************************************************
       ** Transient fixture secret: never placed in load output or backing records.
       *******************************************************************************/
      @Override
      public UserPreferenceActionResult runAction(UserPreferenceRequestContext context, UserPreferenceActionRequest request)
      {
         return (new UserPreferenceActionResult().withMessage("Done").withFocusRowKey(request.getRowKey())
            .withSecrets(List.of(new OneTimeSecret().withLabel("Token").withValue("fixture-only").withMasked("qqq_…7f3a").withUsageText("curl example\nCopy it now"))));
      }



      /*******************************************************************************
       ** No mutable fields in this fixture.
       *******************************************************************************/
      @Override
      public void store(UserPreferenceRequestContext context, UserPreferenceChange change)
      {
      }
   }
}
