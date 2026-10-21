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


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.secrets.OneTimeSecret;


/*******************************************************************************
 ** Transient action output. Core attaches the reloaded section; never log or audit secrets.
 *******************************************************************************/
public class UserPreferenceActionResult
{
   private String message;
   private List<OneTimeSecret> secrets = new ArrayList<>();
   private String focusRowKey;
   private UserPreferenceSectionResult section;



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
   public UserPreferenceActionResult withMessage(String message)
   {
      this.message = message;
      return (this);
   }



   /*******************************************************************************
    ** Get secrets.
    *******************************************************************************/
   public List<OneTimeSecret> getSecrets()
   {
      return (secrets);
   }



   /*******************************************************************************
    ** Set secrets.
    *******************************************************************************/
   public void setSecrets(List<OneTimeSecret> secrets)
   {
      this.secrets = secrets;
   }



   /*******************************************************************************
    ** With secrets.
    *******************************************************************************/
   public UserPreferenceActionResult withSecrets(List<OneTimeSecret> secrets)
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
   public UserPreferenceActionResult withFocusRowKey(String focusRowKey)
   {
      this.focusRowKey = focusRowKey;
      return (this);
   }



   /*******************************************************************************
    ** Get section.
    *******************************************************************************/
   public UserPreferenceSectionResult getSection()
   {
      return (section);
   }



   /*******************************************************************************
    ** Set section.
    *******************************************************************************/
   public void setSection(UserPreferenceSectionResult section)
   {
      this.section = section;
   }



   /*******************************************************************************
    ** With section.
    *******************************************************************************/
   public UserPreferenceActionResult withSection(UserPreferenceSectionResult section)
   {
      this.section = section;
      return (this);
   }
}
