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

package com.kingsrook.qqq.backend.core.model.metadata.preferences;


import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;


/*******************************************************************************
 ** QQQ's standard "Keyboard" preference section. Its one setting turns off
 ** every shortcut made of a character key alone (`/`, `?`, `n`, Shift+D and so
 ** on), as WCAG 2.1.4 Character Key Shortcuts (Level A) requires. Clients read
 ** {@value #SECTION_NAME}.{@value #SINGLE_KEY_SHORTCUTS} by these names; a
 ** missing value means on. Shortcuts with Ctrl or Cmd are not affected.
 **
 ** The section has no provider, so it is stored in core's key-value table:
 ** the application must define that store
 ** (UserPreferencesKeyValueStoreMetaDataProvider) and create its table.
 *******************************************************************************/
public final class KeyboardPreferences
{
   public static final String SECTION_NAME          = "keyboard";
   public static final String SINGLE_KEY_SHORTCUTS  = "singleKeyShortcuts";
   public static final int    DEFAULT_SECTION_ORDER = 40;



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private KeyboardPreferences()
   {
   }



   /*******************************************************************************
    ** The section's metadata.
    *******************************************************************************/
   public static UserPreferenceSectionMetaData defineSection()
   {
      return (new UserPreferenceSectionMetaData()
         .withName(SECTION_NAME)
         .withLabel("Keyboard")
         .withIcon(new QIcon("keyboard"))
         .withDescription("Keyboard shortcuts. Turn off single-key shortcuts if they get in the way of speech input or assistive technology; shortcuts with Ctrl or ⌘ keep working.")
         .withOrder(DEFAULT_SECTION_ORDER)
         .withField(new QFieldMetaData(SINGLE_KEY_SHORTCUTS, QFieldType.BOOLEAN)
            .withLabel("Single-key shortcuts")
            .withDefaultValue(true)));
   }



   /*******************************************************************************
    ** Register the section (idempotent: an existing section of the same name is
    ** kept).
    *******************************************************************************/
   public static void register(QInstance qInstance)
   {
      UserPreferencesMetaData preferences = UserPreferencesMetaData.ofOrWithNew(qInstance);
      if(preferences.getSections().stream().noneMatch(section -> SECTION_NAME.equals(section.getName())))
      {
         preferences.withSection(defineSection());
      }
   }
}
