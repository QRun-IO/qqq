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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components;


import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;


/*******************************************************************************
 ** A table field.
 *******************************************************************************/
public class BulkLoadFieldRef implements ToSchema
{
   @OpenAPIDescription("Field name.")
   private String fieldName;

   @OpenAPIDescription("Field label.")
   private String label;

   @OpenAPIDescription("Possible value source of the field, if any (its file values need mapping).")
   private String possibleValueSourceName;



   /*******************************************************************************
    ** Getter for fieldName
    *******************************************************************************/
   public String getFieldName()
   {
      return (this.fieldName);
   }



   /*******************************************************************************
    ** Fluent setter for fieldName
    *******************************************************************************/
   public BulkLoadFieldRef withFieldName(String fieldName)
   {
      this.fieldName = fieldName;
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
    ** Fluent setter for label
    *******************************************************************************/
   public BulkLoadFieldRef withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Getter for possibleValueSourceName
    *******************************************************************************/
   public String getPossibleValueSourceName()
   {
      return (this.possibleValueSourceName);
   }



   /*******************************************************************************
    ** Fluent setter for possibleValueSourceName
    *******************************************************************************/
   public BulkLoadFieldRef withPossibleValueSourceName(String possibleValueSourceName)
   {
      this.possibleValueSourceName = possibleValueSourceName;
      return (this);
   }

}
