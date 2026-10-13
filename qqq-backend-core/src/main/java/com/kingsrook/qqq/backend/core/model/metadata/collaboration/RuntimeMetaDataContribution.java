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
import java.util.List;


/*******************************************************************************
 **
 *******************************************************************************/
public class RuntimeMetaDataContribution implements Serializable
{
   private long generation;
   private List<RecordFeatureOverride> featureOverrides = new ArrayList<>();



   /*******************************************************************************
    ** Getter for generation
    *******************************************************************************/
   public long getGeneration()
   {
      return (this.generation);
   }



   /*******************************************************************************
    ** Setter for generation
    *******************************************************************************/
   public void setGeneration(long generation)
   {
      this.generation = generation;
   }



   /*******************************************************************************
    ** Fluent setter for generation
    *******************************************************************************/
   public RuntimeMetaDataContribution withGeneration(long generation)
   {
      setGeneration(generation);
      return (this);
   }



   /*******************************************************************************
    ** Getter for featureOverrides
    *******************************************************************************/
   public List<RecordFeatureOverride> getFeatureOverrides()
   {
      return (this.featureOverrides);
   }



   /*******************************************************************************
    ** Setter for featureOverrides
    *******************************************************************************/
   public void setFeatureOverrides(List<RecordFeatureOverride> featureOverrides)
   {
      this.featureOverrides = featureOverrides;
   }



   /*******************************************************************************
    ** Fluent setter for featureOverrides
    *******************************************************************************/
   public RuntimeMetaDataContribution withFeatureOverrides(List<RecordFeatureOverride> featureOverrides)
   {
      setFeatureOverrides(featureOverrides);
      return (this);
   }

}
