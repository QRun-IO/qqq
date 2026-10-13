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

package com.kingsrook.qqq.backend.core.model.metadata.collaboration;


import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;


/*******************************************************************************
 **
 *******************************************************************************/
public class ResolvedRecordFeature implements Serializable
{
   private boolean enabled;
   private Map<String, Serializable> settings = new LinkedHashMap<>();
   private FeaturePermissionMode permissionMode = FeaturePermissionMode.EDIT_OR_GRANTED;



   /*******************************************************************************
    ** Getter for enabled
    *******************************************************************************/
   public boolean getEnabled()
   {
      return (this.enabled);
   }



   /*******************************************************************************
    ** Setter for enabled
    *******************************************************************************/
   public void setEnabled(boolean enabled)
   {
      this.enabled = enabled;
   }



   /*******************************************************************************
    ** Fluent setter for enabled
    *******************************************************************************/
   public ResolvedRecordFeature withEnabled(boolean enabled)
   {
      setEnabled(enabled);
      return (this);
   }



   /*******************************************************************************
    ** Getter for settings
    *******************************************************************************/
   public Map<String, Serializable> getSettings()
   {
      return (this.settings);
   }



   /*******************************************************************************
    ** Setter for settings
    *******************************************************************************/
   public void setSettings(Map<String, Serializable> settings)
   {
      this.settings = settings;
   }



   /*******************************************************************************
    ** Fluent setter for settings
    *******************************************************************************/
   public ResolvedRecordFeature withSettings(Map<String, Serializable> settings)
   {
      setSettings(settings);
      return (this);
   }



   /*******************************************************************************
    ** Getter for permissionMode
    *******************************************************************************/
   public FeaturePermissionMode getPermissionMode()
   {
      return (this.permissionMode);
   }



   /*******************************************************************************
    ** Setter for permissionMode
    *******************************************************************************/
   public void setPermissionMode(FeaturePermissionMode permissionMode)
   {
      this.permissionMode = permissionMode;
   }



   /*******************************************************************************
    ** Fluent setter for permissionMode
    *******************************************************************************/
   public ResolvedRecordFeature withPermissionMode(FeaturePermissionMode permissionMode)
   {
      setPermissionMode(permissionMode);
      return (this);
   }

}
