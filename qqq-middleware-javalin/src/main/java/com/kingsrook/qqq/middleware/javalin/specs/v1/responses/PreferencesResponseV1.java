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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceSectionResult;
import com.kingsrook.qqq.middleware.javalin.executors.io.UserPreferencesOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;


/*******************************************************************************
 ** PreferencesResponseV1 in the user preferences API.
 *******************************************************************************/
public class PreferencesResponseV1 implements UserPreferencesOutputInterface, ToSchema
{
   private List<PreferenceSectionV1> sections;



   /*******************************************************************************
    ** Visible sections in display order, with only the signed-in user's values.
    *******************************************************************************/
   @Override
   public Schema toSchema()
   {
      return (new Schema().withType(Type.OBJECT).withDescription("Visible sections in display order, with only the signed-in user's values.")
         .withProperty("sections", new Schema().withType(Type.ARRAY).withDescription("Installed sections the session may access; an empty list is valid.").withItems(new Schema().withRefToSchema("PreferenceSectionV1")))
         );
   }



   /*******************************************************************************
    ** Map each visible section.
    *******************************************************************************/
   @Override
   public void setPreferenceResults(List<UserPreferenceSectionResult> results)
   {
      sections = new ArrayList<>();
      for(UserPreferenceSectionResult result : results)
      {
         PreferenceSectionV1 section = new PreferenceSectionV1();
         section.setPreferenceResults(List.of(result));
         sections.add(section);
      }
   }



   /*******************************************************************************
    ** Get sections.
    *******************************************************************************/
   public List<PreferenceSectionV1> getSections()
   {
      return (sections);
   }



   /*******************************************************************************
    ** Set sections.
    *******************************************************************************/
   public void setSections(List<PreferenceSectionV1> sections)
   {
      this.sections = sections;
   }



   /*******************************************************************************
    ** With sections.
    *******************************************************************************/
   public PreferencesResponseV1 withSections(List<PreferenceSectionV1> sections)
   {
      this.sections = sections;
      return (this);
   }
}
