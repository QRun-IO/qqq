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
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/*******************************************************************************
 **
 *******************************************************************************/
public class FrontendRecordFeatures implements Serializable
{
   private List<RecordFeature> features = new ArrayList<>();
   private Map<RecordFeature, Map<String, Serializable>> settings = new EnumMap<>(RecordFeature.class);
   private Map<String, Boolean> capabilities = new LinkedHashMap<>();



   /*******************************************************************************
    ** Getter for features
    *******************************************************************************/
   public List<RecordFeature> getFeatures()
   {
      return (this.features);
   }



   /*******************************************************************************
    ** Setter for features
    *******************************************************************************/
   public void setFeatures(List<RecordFeature> features)
   {
      this.features = features;
   }



   /*******************************************************************************
    ** Fluent setter for features
    *******************************************************************************/
   public FrontendRecordFeatures withFeatures(List<RecordFeature> features)
   {
      setFeatures(features);
      return (this);
   }



   /*******************************************************************************
    ** Getter for settings
    *******************************************************************************/
   public Map<RecordFeature, Map<String, Serializable>> getSettings()
   {
      return (this.settings);
   }



   /*******************************************************************************
    ** Setter for settings
    *******************************************************************************/
   public void setSettings(Map<RecordFeature, Map<String, Serializable>> settings)
   {
      this.settings = settings;
   }



   /*******************************************************************************
    ** Fluent setter for settings
    *******************************************************************************/
   public FrontendRecordFeatures withSettings(Map<RecordFeature, Map<String, Serializable>> settings)
   {
      setSettings(settings);
      return (this);
   }



   /*******************************************************************************
    ** Getter for capabilities
    *******************************************************************************/
   public Map<String, Boolean> getCapabilities()
   {
      return (this.capabilities);
   }



   /*******************************************************************************
    ** Setter for capabilities
    *******************************************************************************/
   public void setCapabilities(Map<String, Boolean> capabilities)
   {
      this.capabilities = capabilities;
   }



   /*******************************************************************************
    ** Fluent setter for capabilities
    *******************************************************************************/
   public FrontendRecordFeatures withCapabilities(Map<String, Boolean> capabilities)
   {
      setCapabilities(capabilities);
      return (this);
   }

}
