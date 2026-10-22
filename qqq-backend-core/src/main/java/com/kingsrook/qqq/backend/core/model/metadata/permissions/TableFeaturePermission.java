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

package com.kingsrook.qqq.backend.core.model.metadata.permissions;


import java.io.Serializable;


/*******************************************************************************
 ** A grantable table feature, optionally implied by a standard table permission.
 *******************************************************************************/
public class TableFeaturePermission implements Serializable
{
   private String suffix;
   private String label;
   private TableFeaturePredicate appliesTo;
   private TableFeatureImplication impliedBy;



   /*******************************************************************************
    ** Getter for suffix
    *******************************************************************************/
   public String getSuffix()
   {
      return (this.suffix);
   }



   /*******************************************************************************
    ** Setter for suffix
    *******************************************************************************/
   public void setSuffix(String suffix)
   {
      this.suffix = suffix;
   }



   /*******************************************************************************
    ** Fluent setter for suffix
    *******************************************************************************/
   public TableFeaturePermission withSuffix(String suffix)
   {
      setSuffix(suffix);
      return (this);
   }



   /*******************************************************************************
    ** Getter for label
    *******************************************************************************/
   public String getLabel()
   {
      return (this.label);
   }



   /*******************************************************************************
    ** Setter for label
    *******************************************************************************/
   public void setLabel(String label)
   {
      this.label = label;
   }



   /*******************************************************************************
    ** Fluent setter for label
    *******************************************************************************/
   public TableFeaturePermission withLabel(String label)
   {
      setLabel(label);
      return (this);
   }



   /*******************************************************************************
    ** Getter for appliesTo
    *******************************************************************************/
   public TableFeaturePredicate getAppliesTo()
   {
      return (this.appliesTo);
   }



   /*******************************************************************************
    ** Setter for appliesTo
    *******************************************************************************/
   public void setAppliesTo(TableFeaturePredicate appliesTo)
   {
      this.appliesTo = appliesTo;
   }



   /*******************************************************************************
    ** Fluent setter for appliesTo
    *******************************************************************************/
   public TableFeaturePermission withAppliesTo(TableFeaturePredicate appliesTo)
   {
      setAppliesTo(appliesTo);
      return (this);
   }



   /*******************************************************************************
    ** Getter for impliedBy
    *******************************************************************************/
   public TableFeatureImplication getImpliedBy()
   {
      return (this.impliedBy);
   }



   /*******************************************************************************
    ** Setter for impliedBy
    *******************************************************************************/
   public void setImpliedBy(TableFeatureImplication impliedBy)
   {
      this.impliedBy = impliedBy;
   }



   /*******************************************************************************
    ** Fluent setter for impliedBy
    *******************************************************************************/
   public TableFeaturePermission withImpliedBy(TableFeatureImplication impliedBy)
   {
      setImpliedBy(impliedBy);
      return (this);
   }

}
