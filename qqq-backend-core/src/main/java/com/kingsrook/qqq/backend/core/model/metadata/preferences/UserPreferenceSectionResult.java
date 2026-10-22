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




/*******************************************************************************
 ** UserPreferenceSectionResult for the session user's preferences.
 *******************************************************************************/
public class UserPreferenceSectionResult
{
   private UserPreferenceSectionMetaData section;
   private UserPreferenceValues values;



   /*******************************************************************************
    ** Get section.
    *******************************************************************************/
   public UserPreferenceSectionMetaData getSection()
   {
      return (section);
   }



   /*******************************************************************************
    ** Set section.
    *******************************************************************************/
   public void setSection(UserPreferenceSectionMetaData section)
   {
      this.section = section;
   }



   /*******************************************************************************
    ** With section.
    *******************************************************************************/
   public UserPreferenceSectionResult withSection(UserPreferenceSectionMetaData section)
   {
      this.section = section;
      return (this);
   }



   /*******************************************************************************
    ** Get values.
    *******************************************************************************/
   public UserPreferenceValues getValues()
   {
      return (values);
   }



   /*******************************************************************************
    ** Set values.
    *******************************************************************************/
   public void setValues(UserPreferenceValues values)
   {
      this.values = values;
   }



   /*******************************************************************************
    ** With values.
    *******************************************************************************/
   public UserPreferenceSectionResult withValues(UserPreferenceValues values)
   {
      this.values = values;
      return (this);
   }
}
