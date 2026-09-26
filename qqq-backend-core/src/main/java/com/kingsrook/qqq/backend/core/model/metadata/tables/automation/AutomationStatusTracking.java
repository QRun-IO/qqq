/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.tables.automation;


import com.kingsrook.qqq.backend.core.model.metadata.QMetaDataObject;


/*******************************************************************************
 ** Table-automation meta-data to define how this table's per-record automation
 ** status is tracked.
 *******************************************************************************/
public class AutomationStatusTracking implements QMetaDataObject, Cloneable
{
   private AutomationStatusTrackingType type;

   private String fieldName; // used when type is FIELD_IN_TABLE

   // todo - fields for additional types (e.g., 1-1 table, shared-table)



   /*******************************************************************************
    ** Getter for type
    **
    *******************************************************************************/
   public AutomationStatusTrackingType getType()
   {
      return type;
   }



   /*******************************************************************************
    ** Setter for type
    **
    *******************************************************************************/
   public void setType(AutomationStatusTrackingType type)
   {
      this.type = type;
   }



   /*******************************************************************************
    ** Fluent setter for type
    **
    *******************************************************************************/
   public AutomationStatusTracking withType(AutomationStatusTrackingType type)
   {
      this.type = type;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fieldName
    **
    *******************************************************************************/
   public String getFieldName()
   {
      return fieldName;
   }



   /*******************************************************************************
    ** Setter for fieldName
    **
    *******************************************************************************/
   public void setFieldName(String fieldName)
   {
      this.fieldName = fieldName;
   }



   /*******************************************************************************
    ** Fluent setter for fieldName
    **
    *******************************************************************************/
   public AutomationStatusTracking withFieldName(String fieldName)
   {
      this.fieldName = fieldName;
      return (this);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public AutomationStatusTracking clone()
   {
      try
      {
         AutomationStatusTracking clone = (AutomationStatusTracking) super.clone();
         return clone;
      }
      catch(CloneNotSupportedException e)
      {
         throw new AssertionError();
      }
   }
}
