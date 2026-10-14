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


import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceActionResult;
import com.kingsrook.qqq.backend.core.model.metadata.preferences.UserPreferenceSectionResult;
import com.kingsrook.qqq.middleware.javalin.executors.io.UserPreferencesOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;


/*******************************************************************************
 ** PreferenceActionResponseV1 in the user preferences API.
 *******************************************************************************/
public class PreferenceActionResponseV1 implements UserPreferencesOutputInterface, ToSchema
{
   private PreferenceSectionV1 section;
   private String message;
   private List<PreferenceSecretV1> secrets;
   private String focusRowKey;



   /*******************************************************************************
    ** Describe the wire contract.
    *******************************************************************************/
   @Override
   public Schema toSchema()
   {
      return (new Schema().withType(Type.OBJECT)
         .withDescription("Action result and reloaded section. Secrets are shown once and must not be stored by clients. Response uses Cache-Control: no-store.")
         .withProperty("section", new Schema().withRefToSchema("PreferenceSectionV1"))
         .withProperty("message", new Schema().withType(Type.STRING).withDescription("User-facing outcome message."))
         .withProperty("secrets", new Schema().withType(Type.ARRAY).withItems(new Schema().withRefToSchema("PreferenceSecretV1")).withDescription("One-time secrets for immediate display; clients must not store them."))
         .withProperty("focusRowKey", new Schema().withType(Type.STRING).withDescription("Optional row key to focus after reloading the section.")));
   }



   /*******************************************************************************
    ** Map only the reloaded, non-secret section.
    *******************************************************************************/
   @Override
   public void setPreferenceResults(List<UserPreferenceSectionResult> results)
   {
      section = new PreferenceSectionV1();
      section.setPreferenceResults(results);
   }



   /*******************************************************************************
    ** Map transient secrets only into this action response.
    *******************************************************************************/
   @Override
   public void setActionResult(UserPreferenceActionResult result)
   {
      setPreferenceResults(List.of(result.getSection()));
      message = result.getMessage();
      focusRowKey = result.getFocusRowKey();
      secrets = result.getSecrets().stream().map(secret -> new PreferenceSecretV1().withLabel(secret.getLabel()).withValue(secret.getValue()).withMasked(secret.getMasked()).withUsageText(secret.getUsageText())).toList();
   }



   /*******************************************************************************
    ** Get section.
    *******************************************************************************/
   public PreferenceSectionV1 getSection()
   {
      return (section);
   }



   /*******************************************************************************
    ** Set section.
    *******************************************************************************/
   public void setSection(PreferenceSectionV1 section)
   {
      this.section = section;
   }



   /*******************************************************************************
    ** With section.
    *******************************************************************************/
   public PreferenceActionResponseV1 withSection(PreferenceSectionV1 section)
   {
      this.section = section;
      return (this);
   }



   /*******************************************************************************
    ** Get message.
    *******************************************************************************/
   public String getMessage()
   {
      return (message);
   }



   /*******************************************************************************
    ** Set message.
    *******************************************************************************/
   public void setMessage(String message)
   {
      this.message = message;
   }



   /*******************************************************************************
    ** With message.
    *******************************************************************************/
   public PreferenceActionResponseV1 withMessage(String message)
   {
      this.message = message;
      return (this);
   }



   /*******************************************************************************
    ** Get secrets.
    *******************************************************************************/
   public List<PreferenceSecretV1> getSecrets()
   {
      return (secrets);
   }



   /*******************************************************************************
    ** Set secrets.
    *******************************************************************************/
   public void setSecrets(List<PreferenceSecretV1> secrets)
   {
      this.secrets = secrets;
   }



   /*******************************************************************************
    ** With secrets.
    *******************************************************************************/
   public PreferenceActionResponseV1 withSecrets(List<PreferenceSecretV1> secrets)
   {
      this.secrets = secrets;
      return (this);
   }



   /*******************************************************************************
    ** Get focusRowKey.
    *******************************************************************************/
   public String getFocusRowKey()
   {
      return (focusRowKey);
   }



   /*******************************************************************************
    ** Set focusRowKey.
    *******************************************************************************/
   public void setFocusRowKey(String focusRowKey)
   {
      this.focusRowKey = focusRowKey;
   }



   /*******************************************************************************
    ** With focusRowKey.
    *******************************************************************************/
   public PreferenceActionResponseV1 withFocusRowKey(String focusRowKey)
   {
      this.focusRowKey = focusRowKey;
      return (this);
   }
}
