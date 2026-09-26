/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components;


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIMapValueType;


/*******************************************************************************
 **
 *******************************************************************************/
public class ProcessMetaDataAdjustment
{
   @OpenAPIDescription("""
      In case the backend has changed the list of frontend steps, it will be set in this field.""")
   @OpenAPIListItems(FrontendStep.class)
   private List<FrontendStep> updatedFrontendStepList = null;

   @OpenAPIDescription("""
      Fields whose meta-data has changed.  e.g., changing a label, or required status, or inline-possible-values.""")
   @OpenAPIMapValueType(value = FieldMetaData.class, useRef = true)
   private Map<String, FieldMetaData> updatedFields = null;



   /*******************************************************************************
    ** Getter for updatedFrontendStepList
    **
    *******************************************************************************/
   public List<FrontendStep> getUpdatedFrontendStepList()
   {
      return updatedFrontendStepList;
   }



   /*******************************************************************************
    ** Setter for updatedFrontendStepList
    **
    *******************************************************************************/
   public void setUpdatedFrontendStepList(List<FrontendStep> updatedFrontendStepList)
   {
      this.updatedFrontendStepList = updatedFrontendStepList;
   }



   /*******************************************************************************
    ** Fluent setter for updatedFrontendStepList
    **
    *******************************************************************************/
   public ProcessMetaDataAdjustment withUpdatedFrontendStepList(List<FrontendStep> updatedFrontendStepList)
   {
      this.updatedFrontendStepList = updatedFrontendStepList;
      return (this);
   }



   /*******************************************************************************
    ** Getter for updatedFields
    *******************************************************************************/
   public Map<String, FieldMetaData> getUpdatedFields()
   {
      return (this.updatedFields);
   }



   /*******************************************************************************
    ** Setter for updatedFields
    *******************************************************************************/
   public void setUpdatedFields(Map<String, FieldMetaData> updatedFields)
   {
      this.updatedFields = updatedFields;
   }



   /*******************************************************************************
    ** Fluent setter for updatedFields
    *******************************************************************************/
   public ProcessMetaDataAdjustment withUpdatedFields(Map<String, FieldMetaData> updatedFields)
   {
      this.updatedFields = updatedFields;
      return (this);
   }

}
