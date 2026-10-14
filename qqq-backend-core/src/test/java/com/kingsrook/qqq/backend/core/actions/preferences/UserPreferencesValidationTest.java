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


import java.util.List;
import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.preferences.UserPreferencesKeyValueStoreMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceActionMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceActionScope;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceChange;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceListMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceProviderInterface;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceRequestContext;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceSectionMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceValues;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferencesMetaData;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;

import static com.kingsrook.qqq.backend.core.instances.QInstanceValidatorTest.assertValidationFailureReasonsAllowingExtraReasons;
import static com.kingsrook.qqq.backend.core.instances.QInstanceValidatorTest.assertValidationSuccess;


/*******************************************************************************
 ** Startup validation of every user preference metadata rule.
 *******************************************************************************/
class UserPreferencesValidationTest extends BaseTest
{
   /*******************************************************************************
    ** Valid metadata with a scalar default provider.
    *******************************************************************************/
   private static void setup(QInstance instance)
   {
      new UserPreferencesKeyValueStoreMetaDataProvider().defineAll(instance, TestUtils.MEMORY_BACKEND_NAME, null);
      UserPreferencesMetaData.of(instance).withSection(new UserPreferenceSectionMetaData().withName("display").withLabel("Display")
         .withField(new QFieldMetaData("enabled", QFieldType.BOOLEAN).withLabel("Enabled")));
   }



   /*******************************************************************************
    ** Get the fixture section.
    *******************************************************************************/
   private static UserPreferenceSectionMetaData section(QInstance instance)
   {
      return (UserPreferencesMetaData.of(instance).getSections().get(0));
   }



   /*******************************************************************************
    ** Exercise the full instance validator, including its supplemental hook.
    *******************************************************************************/
   private void fails(Consumer<QInstance> change, String message)
   {
      assertValidationFailureReasonsAllowingExtraReasons(instance ->
      {
         setup(instance);
         change.accept(instance);
      }, message);
   }



   /*******************************************************************************
    ** Valid scalar and custom-provider list definitions pass.
    *******************************************************************************/
   @Test
   void testValid()
   {
      assertValidationSuccess(UserPreferencesValidationTest::setup);
      assertValidationSuccess(instance ->
      {
         setup(instance);
         section(instance).withProvider(new QCodeReference(Provider.class)).withList(list());
      });
   }



   /*******************************************************************************
    ** Valid list metadata.
    *******************************************************************************/
   private static UserPreferenceListMetaData list()
   {
      return (new UserPreferenceListMetaData().withName("rows").withLabel("Rows")
         .withField(new QFieldMetaData("enabled", QFieldType.BOOLEAN).withLabel("Enabled")));
   }



   /*******************************************************************************
    ** Validate SectionNameMissing.
    *******************************************************************************/
   @Test
   void testSectionNameMissing()
   {
      fails(i -> section(i).setName(null), "name must match");
   }



   /*******************************************************************************
    ** Validate SectionNameMalformed.
    *******************************************************************************/
   @Test
   void testSectionNameMalformed()
   {
      fails(i -> section(i).setName("9bad"), "name must match");
   }



   /*******************************************************************************
    ** Validate SectionNameDuplicate.
    *******************************************************************************/
   @Test
   void testSectionNameDuplicate()
   {
      fails(i -> UserPreferencesMetaData.of(i).withSection(section(i)), "name must be unique");
   }



   /*******************************************************************************
    ** Validate SectionLabelMissing.
    *******************************************************************************/
   @Test
   void testSectionLabelMissing()
   {
      fails(i -> section(i).setLabel(" "), "label is required");
   }



   /*******************************************************************************
    ** Validate ListNameMissing.
    *******************************************************************************/
   @Test
   void testListNameMissing()
   {
      fails(i -> section(i).withProvider(new QCodeReference(Provider.class)).withList(list().withName(null)), "name must match");
   }



   /*******************************************************************************
    ** Validate ListNameMalformed.
    *******************************************************************************/
   @Test
   void testListNameMalformed()
   {
      fails(i -> section(i).withProvider(new QCodeReference(Provider.class)).withList(list().withName("bad.name")), "name must match");
   }



   /*******************************************************************************
    ** Validate ListNameDuplicate.
    *******************************************************************************/
   @Test
   void testListNameDuplicate()
   {
      fails(i -> section(i).withProvider(new QCodeReference(Provider.class)).withList(list()).withList(list()), "name must be unique");
   }



   /*******************************************************************************
    ** Validate ListLabelMissing.
    *******************************************************************************/
   @Test
   void testListLabelMissing()
   {
      fails(i -> section(i).withProvider(new QCodeReference(Provider.class)).withList(list().withLabel(null)), "label is required");
   }



   /*******************************************************************************
    ** Validate FieldNameMissing.
    *******************************************************************************/
   @Test
   void testFieldNameMissing()
   {
      fails(i -> section(i).getFields().get(0).setName(null), "name must match");
   }



   /*******************************************************************************
    ** Validate FieldNameMalformed.
    *******************************************************************************/
   @Test
   void testFieldNameMalformed()
   {
      fails(i -> section(i).getFields().get(0).setName("bad.name"), "name must match");
   }



   /*******************************************************************************
    ** Validate FieldNameDuplicate.
    *******************************************************************************/
   @Test
   void testFieldNameDuplicate()
   {
      fails(i -> section(i).withField(section(i).getFields().get(0)), "name must be unique");
   }



   /*******************************************************************************
    ** Validate ListFieldDuplicate.
    *******************************************************************************/
   @Test
   void testListFieldDuplicate()
   {
      fails(i -> section(i).withProvider(new QCodeReference(Provider.class)).withList(list().withField(new QFieldMetaData("enabled", QFieldType.BOOLEAN).withLabel("Enabled"))), "name must be unique");
   }



   /*******************************************************************************
    ** Validate FieldLabelMissing.
    *******************************************************************************/
   @Test
   void testFieldLabelMissing()
   {
      fails(i -> section(i).getFields().get(0).setLabel(null), "label is required");
   }



   /*******************************************************************************
    ** Validate TypeMissing.
    *******************************************************************************/
   @Test
   void testTypeMissing()
   {
      fails(i -> section(i).getFields().get(0).setType(null), "unsupported field type");
   }



   /*******************************************************************************
    ** Validate PossibleValuesMissing.
    *******************************************************************************/
   @Test
   void testPossibleValuesMissing()
   {
      fails(i -> section(i).getFields().get(0).setPossibleValueSourceName("absent"), "possible value source must exist");
   }



   /*******************************************************************************
    ** Validate AppMissing.
    *******************************************************************************/
   @Test
   void testAppMissing()
   {
      fails(i -> section(i).setAppName("absent"), "appName must exist");
   }



   /*******************************************************************************
    ** Validate ProviderMissingClass.
    *******************************************************************************/
   @Test
   void testProviderMissingClass()
   {
      fails(i -> section(i).setProvider(new QCodeReference().withName("missing.Provider")), "provider must load");
   }



   /*******************************************************************************
    ** Validate ProviderWrongInterface.
    *******************************************************************************/
   @Test
   void testProviderWrongInterface()
   {
      fails(i -> section(i).setProvider(new QCodeReference(String.class)), "provider must load");
   }



   /*******************************************************************************
    ** Validate DefaultProviderLists.
    *******************************************************************************/
   @Test
   void testDefaultProviderLists()
   {
      fails(i -> section(i).withList(list()), "does not support lists");
   }



   /*******************************************************************************
    ** Validate StoreMissing.
    *******************************************************************************/
   @Test
   void testStoreMissing()
   {
      fails(i -> UserPreferencesMetaData.of(i).setKeyValueTableName("absent"), "key-value table");
   }



   /*******************************************************************************
    ** Validate IdentityFieldReserved.
    *******************************************************************************/
   @Test
   void testIdentityFieldReserved()
   {
      fails(i -> section(i).withField(new QFieldMetaData("userId", QFieldType.STRING).withLabel("User")), "userId is reserved");
   }



   /*******************************************************************************
    ** Only the eight specified field types are allowed, also on list fields.
    *******************************************************************************/
   @Test
   void testUnsupportedTypes()
   {
      for(QFieldType type : List.of(QFieldType.LONG, QFieldType.BLOB, QFieldType.HTML, QFieldType.PASSWORD))
      {
         fails(i -> section(i).getFields().get(0).setType(type), "unsupported field type");
         fails(i -> section(i).withProvider(new QCodeReference(Provider.class)).withList(list().withField(new QFieldMetaData("bad", type).withLabel("Bad"))), "unsupported field type");
      }
   }



   /*******************************************************************************
    ** Valid action fixture; scope and target are supplied by each rule's test.
    *******************************************************************************/
   private static UserPreferenceActionMetaData action()
   {
      return (new UserPreferenceActionMetaData().withName("create").withLabel("Create").withScope(UserPreferenceActionScope.SECTION));
   }



   /*******************************************************************************
    ** Action names and scope/list relationships must be consistent.
    *******************************************************************************/
   @Test
   void testActionMetadata()
   {
      fails(i -> section(i).withAction(action().withName(null)), "name must match");
      fails(i -> section(i).withAction(action().withName("bad.name")), "name must match");
      fails(i -> section(i).withAction(action()).withAction(action()), "name must be unique");
      fails(i -> section(i).withAction(action().withLabel(" ")), "label is required");
      fails(i -> section(i).withAction(action().withScope(null)), "scope is required");
      fails(i -> section(i).withAction(action().withListName("rows")), "SECTION scope must not have listName");
      for(UserPreferenceActionScope scope : List.of(UserPreferenceActionScope.LIST, UserPreferenceActionScope.ROW))
      {
         fails(i -> section(i).withAction(action().withScope(scope)), "listName must name an existing list");
         fails(i -> section(i).withAction(action().withScope(scope).withListName("absent")), "listName must name an existing list");
         assertValidationSuccess(i ->
         {
            setup(i);
            section(i).withProvider(new QCodeReference(Provider.class)).withList(list()).withAction(action().withScope(scope).withListName("rows"));
         });
      }
      assertValidationSuccess(i ->
      {
         setup(i);
         section(i).withAction(action().withInputField(new QFieldMetaData("name", QFieldType.STRING).withLabel("Name")));
      });
   }



   /*******************************************************************************
    ** Every preference field rule also applies to action input fields.
    *******************************************************************************/
   @Test
   void testActionInputFields()
   {
      fails(i -> section(i).withAction(action().withInputField(new QFieldMetaData().withLabel("Name").withType(QFieldType.STRING))), "name must match");
      fails(i -> section(i).withAction(action().withInputField(new QFieldMetaData("bad.name", QFieldType.STRING).withLabel("Name"))), "name must match");
      fails(i -> section(i).withAction(action().withInputField(new QFieldMetaData("name", QFieldType.STRING).withLabel(" "))), "label is required");
      fails(i -> section(i).withAction(action().withInputField(new QFieldMetaData().withName("name").withLabel("Name"))), "unsupported field type");
      fails(i -> section(i).withAction(action().withInputField(new QFieldMetaData("userId", QFieldType.STRING).withLabel("User"))), "userId is reserved");
      fails(i -> section(i).withAction(action().withInputField(new QFieldMetaData("name", QFieldType.STRING).withLabel("Name").withPossibleValueSourceName("absent"))), "possible value source must exist");
      fails(i -> section(i).withAction(action().withInputFields(List.of(new QFieldMetaData("name", QFieldType.STRING).withLabel("Name"), new QFieldMetaData("name", QFieldType.STRING).withLabel("Name")))), "name must be unique");
      for(QFieldType type : List.of(QFieldType.LONG, QFieldType.BLOB, QFieldType.HTML, QFieldType.PASSWORD))
      {
         fails(i -> section(i).withAction(action().withInputField(new QFieldMetaData("name", type).withLabel("Name"))), "unsupported field type");
      }
   }



   /*******************************************************************************
    ** Minimal custom SPI for metadata validation.
    *******************************************************************************/
   public static class Provider implements UserPreferenceProviderInterface
   {
      /*******************************************************************************
       ** Load no values.
       *******************************************************************************/
      @Override
      public UserPreferenceValues load(UserPreferenceRequestContext context)
      {
         return (new UserPreferenceValues());
      }



      /*******************************************************************************
       ** No persistence is required by this metadata fixture.
       *******************************************************************************/
      @Override
      public void store(UserPreferenceRequestContext context, UserPreferenceChange change)
      {
      }
   }
}
