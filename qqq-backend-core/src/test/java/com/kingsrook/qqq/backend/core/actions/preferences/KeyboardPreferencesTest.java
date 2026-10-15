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


import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.KeyboardPreferences;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferencesMetaData;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** The standard Keyboard section (WCAG 2.1.4): on by default, stored in the
 ** core key-value store, registered once.
 *******************************************************************************/
class KeyboardPreferencesTest extends BaseTest
{
   private final UserPreferencesAction action = new UserPreferencesAction();



   /*******************************************************************************
    ** The key-value store and the section, as an application registers them.
    *******************************************************************************/
   @BeforeEach
   void setup() throws Exception
   {
      new UserPreferencesKeyValueStoreMetaDataProvider().defineAll(QContext.getQInstance(), TestUtils.MEMORY_BACKEND_NAME, null);
      KeyboardPreferences.register(QContext.getQInstance());
      KeyboardPreferences.register(QContext.getQInstance());
      new QInstanceEnricher(QContext.getQInstance()).enrich();
   }



   /*******************************************************************************
    ** Default on; a user can turn single-key shortcuts off and on again.
    *******************************************************************************/
   @Test
   void defaultsOnAndRoundTrips() throws Exception
   {
      assertThat(UserPreferencesMetaData.of(QContext.getQInstance()).getSections())
         .filteredOn(section -> KeyboardPreferences.SECTION_NAME.equals(section.getName())).hasSize(1);
      new QInstanceValidator().revalidate(QContext.getQInstance());

      assertEquals(true, singleKeyShortcuts());
      action.patchSection(KeyboardPreferences.SECTION_NAME, Map.of("values", Map.of(KeyboardPreferences.SINGLE_KEY_SHORTCUTS, false)));
      assertEquals(false, singleKeyShortcuts());
      action.patchSection(KeyboardPreferences.SECTION_NAME, Map.of("values", Map.of(KeyboardPreferences.SINGLE_KEY_SHORTCUTS, true)));
      assertEquals(true, singleKeyShortcuts());
   }



   /*******************************************************************************
    ** The current user's value.
    *******************************************************************************/
   private Object singleKeyShortcuts() throws Exception
   {
      return (action.getSection(KeyboardPreferences.SECTION_NAME).getValues().getValues().get(KeyboardPreferences.SINGLE_KEY_SHORTCUTS));
   }
}
