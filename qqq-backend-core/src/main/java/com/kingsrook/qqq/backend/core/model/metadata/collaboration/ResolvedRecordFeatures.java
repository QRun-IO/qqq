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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;


/*******************************************************************************
 **
 *******************************************************************************/
public class ResolvedRecordFeatures implements Serializable
{
   private Map<RecordFeature, ResolvedRecordFeature> features = new EnumMap<>(RecordFeature.class);
   private RecordOwnership ownership;



   /*******************************************************************************
    **
    *******************************************************************************/
   public boolean isEnabled(RecordFeature feature)
   {
      return (features.containsKey(feature) && features.get(feature).getEnabled());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public List<RecordFeature> enabledFeatures()
   {
      return (features.keySet().stream().filter(this::isEnabled).toList());
   }



   /*******************************************************************************
    ** Getter for features
    *******************************************************************************/
   public Map<RecordFeature, ResolvedRecordFeature> getFeatures()
   {
      return (this.features);
   }



   /*******************************************************************************
    ** Setter for features
    *******************************************************************************/
   public void setFeatures(Map<RecordFeature, ResolvedRecordFeature> features)
   {
      this.features = features;
   }



   /*******************************************************************************
    ** Fluent setter for features
    *******************************************************************************/
   public ResolvedRecordFeatures withFeatures(Map<RecordFeature, ResolvedRecordFeature> features)
   {
      setFeatures(features);
      return (this);
   }



   /*******************************************************************************
    ** Getter for ownership
    *******************************************************************************/
   public RecordOwnership getOwnership()
   {
      return (this.ownership);
   }



   /*******************************************************************************
    ** Setter for ownership
    *******************************************************************************/
   public void setOwnership(RecordOwnership ownership)
   {
      this.ownership = ownership;
   }



   /*******************************************************************************
    ** Fluent setter for ownership
    *******************************************************************************/
   public ResolvedRecordFeatures withOwnership(RecordOwnership ownership)
   {
      setOwnership(ownership);
      return (this);
   }

}
