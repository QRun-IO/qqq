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
import java.util.Set;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
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
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Action validation, session ownership, trusted execution and transient secrets.
 *******************************************************************************/
class UserPreferenceRunActionTest extends BaseTest
{
   private final UserPreferencesAction actions = new UserPreferencesAction();
   private UserPreferenceSectionMetaData section;
   private UserPreferenceActionMetaData create;



   /*******************************************************************************
    ** Register all action scopes and every kind of field validation.
    *******************************************************************************/
   @BeforeEach
   void setupActions()
   {
      new UserPreferencesKeyValueStoreMetaDataProvider().defineAll(QContext.getQInstance(), TestUtils.MEMORY_BACKEND_NAME, null);
      QContext.getQInstance().addPossibleValueSource(new QPossibleValueSource().withName("choices")
         .withEnumValues(List.of(new QPossibleValue<>("one", "One"))));
      create = new UserPreferenceActionMetaData().withName("create").withLabel("Create token").withScope(UserPreferenceActionScope.LIST).withListName("tokens")
         .withInputField(new QFieldMetaData("name", QFieldType.STRING).withLabel("Token name").withMaxLength(5).withIsRequired(true))
         .withInputField(new QFieldMetaData("count", QFieldType.INTEGER).withLabel("Use count"))
         .withInputField(new QFieldMetaData("enabled", QFieldType.BOOLEAN).withLabel("Enabled"))
         .withInputField(new QFieldMetaData("choice", QFieldType.STRING).withLabel("Choice").withPossibleValueSourceName("choices"))
         .withInputField(new QFieldMetaData("locked", QFieldType.STRING).withLabel("Locked").withIsEditable(false));
      section = new UserPreferenceSectionMetaData().withName("tokens").withLabel("Personal access tokens").withProvider(new QCodeReference(Provider.class))
         .withList(new UserPreferenceListMetaData().withName("tokens").withLabel("Tokens"))
         .withAction(create)
         .withAction(new UserPreferenceActionMetaData().withName("rotate").withLabel("Rotate").withScope(UserPreferenceActionScope.ROW).withListName("tokens"))
         .withAction(new UserPreferenceActionMetaData().withName("refresh").withLabel("Refresh").withScope(UserPreferenceActionScope.SECTION));
      UserPreferencesMetaData.of(QContext.getQInstance()).withSection(section);
      Provider.calls = 0;
      Provider.lastRequest = null;
      Provider.lastContext = null;
      Provider.actionNames = null;
      Provider.fail = false;
   }



   /*******************************************************************************
    ** Fields share PATCH validation and use their human-facing labels.
    *******************************************************************************/
   @ParameterizedTest
   @CsvSource({"unknown,x,unknown: isn't a setting here", "userId,B,userId: isn't a setting here", "locked,x,Locked: can't be changed", "name,abcdef,Token name: must be at most 5 characters", "count,bad,Use count: must be a whole number", "enabled,maybe,Enabled: must be true or false", "choice,two,Choice: isn't one of the choices"})
   void testFieldValidation(String field, String value, String message)
   {
      Map<String, Object> values = new HashMap<>();
      values.put("name", "test");
      values.put(field, value);
      assertThat(assertThrows(QBadRequestException.class, () -> actions.runAction("tokens", "create", Map.of("values", values))).getMessage()).contains(message);
      assertEquals(0, Provider.calls);
   }



   /*******************************************************************************
    ** Required nulls and structured values are rejected before the provider runs.
    *******************************************************************************/
   @Test
   void testRequiredAndStructuredValues()
   {
      Map<String, Object> values = new HashMap<>();
      values.put("name", null);
      assertThat(assertThrows(QBadRequestException.class, () -> actions.runAction("tokens", "create", Map.of("values", values))).getMessage()).contains("Token name: is required");
      for(Object invalid : List.of(List.of("test"), Map.of("nested", "test")))
      {
         assertThat(assertThrows(QBadRequestException.class, () -> actions.runAction("tokens", "create", Map.of("values", Map.of("name", invalid)))).getMessage()).contains("Token name: must be text");
      }
      assertEquals(0, Provider.calls);
   }



   /*******************************************************************************
    ** All supported temporal and numeric input types share coercion and error wording.
    *******************************************************************************/
   @ParameterizedTest
   @CsvSource({"DATE,2026-10-13,must be a date", "DATE_TIME,2026-10-13T12:00:00Z,must be a date and time", "TIME,12:00:00,must be a time", "DECIMAL,1.5,must be a number"})
   void testTypedFields(QFieldType type, String valid, String message) throws Exception
   {
      create.withInputField(new QFieldMetaData("typed", type).withLabel("Typed value"));
      actions.runAction("tokens", "create", Map.of("values", Map.of("name", "test", "typed", valid)));
      assertThat(Provider.lastRequest.getValues().get("typed")).isNotNull().isNotInstanceOf(String.class);
      assertThat(assertThrows(QBadRequestException.class, () -> actions.runAction("tokens", "create", Map.of("values", Map.of("name", "test", "typed", "invalid")))).getMessage()).contains("Typed value: " + message);
   }



   /*******************************************************************************
    ** Unknown keys, malformed values, and row keys on non-row actions are refused.
    *******************************************************************************/
   @Test
   void testBodyValidation()
   {
      for(String key : List.of("userId", "email", "listName", "actionName", "other"))
      {
         assertThat(assertThrows(QBadRequestException.class, () -> actions.runAction("tokens", "refresh", Map.of(key, "B"))).getMessage()).contains("Unexpected property: " + key);
      }
      for(Object values : List.of("text", List.of(), 123))
      {
         assertThrows(QBadRequestException.class, () -> actions.runAction("tokens", "refresh", Map.of("values", values)));
      }
      Map<String, Object> nullValues = new HashMap<>();
      nullValues.put("values", null);
      assertThrows(QBadRequestException.class, () -> actions.runAction("tokens", "refresh", nullValues));
      assertThrows(QBadRequestException.class, () -> actions.runAction("tokens", "refresh", null));
      for(String action : List.of("create", "refresh"))
      {
         for(Object key : List.of("row", 42))
         {
            assertThrows(QBadRequestException.class, () -> actions.runAction("tokens", action, Map.of("rowKey", key)));
         }
         Map<String, Object> nullRow = new HashMap<>();
         nullRow.put("rowKey", null);
         assertThrows(QBadRequestException.class, () -> actions.runAction("tokens", action, nullRow));
      }
      assertEquals(0, Provider.calls);
   }



   /*******************************************************************************
    ** SECTION actions accept omitted values; validation failures remain atomic.
    *******************************************************************************/
   @Test
   void testSectionActionAndCombinedErrors() throws Exception
   {
      actions.runAction("tokens", "refresh", Map.of());
      assertNull(Provider.lastRequest.getListName());
      assertNull(Provider.lastRequest.getRowKey());
      assertThat(Provider.lastRequest.getValues()).isEmpty();
      String message = assertThrows(QBadRequestException.class, () -> actions.runAction("tokens", "create",
         Map.of("userId", "B", "rowKey", "row", "values", Map.of("name", "too long", "count", "bad")))).getMessage();
      assertThat(message).contains("Unexpected property: userId", "does not accept a row key", "Token name", "Use count");
      assertEquals(1, Provider.calls);
   }



   /*******************************************************************************
    ** ROW actions require a nonempty string key from this user's loaded list.
    *******************************************************************************/
   @Test
   void testRowOwnership() throws Exception
   {
      String aliceKey = QContext.getQSession().getUser().getIdReference();
      actions.runAction("tokens", "rotate", Map.of("rowKey", aliceKey));
      assertEquals("tokens", Provider.lastRequest.getListName());
      assertEquals(aliceKey, Provider.lastRequest.getRowKey());
      QContext.setQSession(newSession("B"));
      assertThrows(QBadRequestException.class, () -> actions.runAction("tokens", "rotate", Map.of("rowKey", aliceKey)));
      assertThrows(QBadRequestException.class, () -> actions.runAction("tokens", "rotate", Map.of()));
      for(Object key : List.of("", " ", 42, "missing"))
      {
         assertThrows(QBadRequestException.class, () -> actions.runAction("tokens", "rotate", Map.of("rowKey", key)));
      }
      Map<String, Object> nullRow = new HashMap<>();
      nullRow.put("rowKey", null);
      assertThrows(QBadRequestException.class, () -> actions.runAction("tokens", "rotate", nullRow));
      assertEquals(1, Provider.calls);
   }



   /*******************************************************************************
    ** Null allows all row actions, an empty set none, and a named set only its members.
    *******************************************************************************/
   @Test
   void testActionNames() throws Exception
   {
      Map<String, Object> body = Map.of("rowKey", QContext.getQSession().getUser().getIdReference());
      actions.runAction("tokens", "rotate", body);
      Provider.actionNames = Set.of("rotate");
      actions.runAction("tokens", "rotate", body);
      Provider.actionNames = Set.of();
      assertThrows(QBadRequestException.class, () -> actions.runAction("tokens", "rotate", body));
      Provider.actionNames = Set.of("other");
      assertThrows(QBadRequestException.class, () -> actions.runAction("tokens", "rotate", body));
      assertEquals(2, Provider.calls);
   }



   /*******************************************************************************
    ** Not-found checks run before body validation or provider execution.
    *******************************************************************************/
   @Test
   void testNotFound()
   {
      assertThat(assertThrows(QNotFoundException.class, () -> actions.runAction("tokens", "absent", Map.of("invalid", true))).getMessage()).isEqualTo("Action not found");
      section.withPermissionName("hidden");
      assertThrows(QNotFoundException.class, () -> actions.runAction("tokens", "rotate", Map.of()));
      assertThrows(QNotFoundException.class, () -> actions.runAction("absent", "rotate", Map.of()));
      assertEquals(0, Provider.calls);
   }



   /*******************************************************************************
    ** Providers opt into actions; old implementations retain the explicit bad-request default.
    *******************************************************************************/
   @Test
   void testDefaultProvider()
   {
      section.withProvider(null).getLists().clear();
      assertThat(assertThrows(QBadRequestException.class, () -> actions.runAction("tokens", "refresh", Map.of())).getMessage()).isEqualTo("This action isn't available.");
   }



   /*******************************************************************************
    ** Core returns one-time secrets and reloads the section, preserving session and channel.
    *******************************************************************************/
   @Test
   void testTrustedResultAndReload() throws Exception
   {
      QContext.getQInstance().setEnforcePermissionsInCoreActions(true);
      QContext.getQInstance().getTable("user_preference").setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      QContext.getQSession().withClientChannel("cli/test");
      assertThrows(QPermissionDeniedException.class, () -> new QueryAction().execute(new QueryInput("user_preference")));
      UserPreferenceActionResult result = actions.runAction("tokens", "create", Map.of("values", Map.of("name", "test", "count", "3", "enabled", "yes")));
      assertEquals(3, Provider.lastRequest.getValues().get("count"));
      assertEquals(true, Provider.lastRequest.getValues().get("enabled"));
      assertSame(QContext.getQSession(), Provider.lastContext.getSession());
      assertEquals("cli/test", Provider.lastContext.getSession().getClientChannel());
      assertEquals("Created", result.getMessage());
      assertEquals("fixture-only", result.getSecrets().get(0).getValue());
      assertEquals("qqq_…7f3a", result.getSecrets().get(0).getMasked());
      assertEquals("curl example\nCopy it now", result.getSecrets().get(0).getUsageText());
      assertEquals(Provider.lastContext.getUserId(), result.getFocusRowKey());
      assertEquals(1, result.getSection().getValues().getValues().get("calls"));
      assertThat(result.getSection().getValues().getValues()).doesNotContainValue("fixture-only");
      assertEquals(0, QContext.getAuthorizedActionScopeDepth());
      assertThrows(QPermissionDeniedException.class, () -> new QueryAction().execute(new QueryInput("user_preference")));
      Provider.fail = true;
      assertThrows(QException.class, () -> actions.runAction("tokens", "refresh", Map.of()));
      assertEquals(0, QContext.getAuthorizedActionScopeDepth());
   }



   /*******************************************************************************
    ** Email comes from the frontend user map, or a syntactically valid session id.
    *******************************************************************************/
   @Test
   void testEmailAddress()
   {
      assertNull(new UserPreferenceRequestContext(section, newSession("123")).getUserEmailAddress());
      assertEquals("person@example.com", new UserPreferenceRequestContext(section, newSession("person@example.com")).getUserEmailAddress());
      var session = newSession("fallback@example.com");
      session.withValueForFrontend("user", new LinkedHashMap<>(Map.of("name", "Person", "email", "oauth@example.com")));
      assertEquals("oauth@example.com", new UserPreferenceRequestContext(section, session).getUserEmailAddress());
      for(Serializable malformed : List.of("not a map", new LinkedHashMap<>(Map.of("name", "Person")), new LinkedHashMap<>(Map.of("email", 12)), new LinkedHashMap<>(Map.of("email", ""))))
      {
         session.withValueForFrontend("user", malformed);
         assertEquals("fallback@example.com", new UserPreferenceRequestContext(section, session).getUserEmailAddress());
      }
      for(String invalid : List.of("not-email", "missing@", "@example.com", "a b@example.com", "a@example.com,b@example.com"))
      {
         assertNull(new UserPreferenceRequestContext(section, newSession(invalid)).getUserEmailAddress());
      }
   }



   /*******************************************************************************
    ** Fixture provider exposes only the current user's row and no stored secret.
    *******************************************************************************/
   public static class Provider implements UserPreferenceProviderInterface
   {
      private static int calls;
      private static Set<String> actionNames;
      private static UserPreferenceActionRequest lastRequest;
      private static UserPreferenceRequestContext lastContext;
      private static boolean fail;



      /*******************************************************************************
       ** Only session-owned rows are returned.
       *******************************************************************************/
      @Override
      public UserPreferenceValues load(UserPreferenceRequestContext context)
      {
         return (new UserPreferenceValues().withValues(Map.of("calls", calls)).withLists(Map.of("tokens", List.of(new UserPreferenceRow()
            .withKey(context.getUserId()).withLabel("Token").withActionNames(actionNames)))));
      }



      /*******************************************************************************
       ** The fixture has no PATCH persistence.
       *******************************************************************************/
      @Override
      public void store(UserPreferenceRequestContext context, UserPreferenceChange change)
      {
      }



      /*******************************************************************************
       ** A protected query proves the trusted boundary is active inside this method.
       *******************************************************************************/
      @Override
      public UserPreferenceActionResult runAction(UserPreferenceRequestContext context, UserPreferenceActionRequest request) throws QException
      {
         new QueryAction().execute(new QueryInput("user_preference"));
         if(fail)
         {
            throw (new QException("Fixture failure"));
         }
         calls++;
         lastRequest = request;
         lastContext = context;
         return (new UserPreferenceActionResult().withMessage("Created").withFocusRowKey(context.getUserId())
            .withSecrets(List.of(new OneTimeSecret().withLabel("Token").withValue("fixture-only").withMasked("qqq_…7f3a").withUsageText("curl example\nCopy it now"))));
      }
   }
}
