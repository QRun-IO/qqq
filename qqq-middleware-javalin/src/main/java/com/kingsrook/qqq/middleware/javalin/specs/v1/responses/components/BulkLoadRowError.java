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
 ** An error group of a bulk-load dry run.
 *******************************************************************************/
public class BulkLoadRowError implements ToSchema
{
   @OpenAPIDescription("File rows with the error, e.g. Row 3; Rows 7-9 (no values).")
   private String rowNumbers;

   @OpenAPIDescription("The field the error is about, when known.")
   private String fieldName;

   @OpenAPIDescription("VALUE_TYPE, POSSIBLE_VALUE, DUPLICATE, REQUIRED or OTHER.")
   private String errorCode;

   @OpenAPIDescription("The error message (value-free).")
   private String message;



   /*******************************************************************************
    ** Getter for rowNumbers
    *******************************************************************************/
   public String getRowNumbers()
   {
      return (this.rowNumbers);
   }



   /*******************************************************************************
    ** Fluent setter for rowNumbers
    *******************************************************************************/
   public BulkLoadRowError withRowNumbers(String rowNumbers)
   {
      this.rowNumbers = rowNumbers;
      return (this);
   }



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
   public BulkLoadRowError withFieldName(String fieldName)
   {
      this.fieldName = fieldName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for errorCode
    *******************************************************************************/
   public String getErrorCode()
   {
      return (this.errorCode);
   }



   /*******************************************************************************
    ** Fluent setter for errorCode
    *******************************************************************************/
   public BulkLoadRowError withErrorCode(String errorCode)
   {
      this.errorCode = errorCode;
      return (this);
   }



   /*******************************************************************************
    ** Getter for message
    *******************************************************************************/
   public String getMessage()
   {
      return (this.message);
   }



   /*******************************************************************************
    ** Fluent setter for message
    *******************************************************************************/
   public BulkLoadRowError withMessage(String message)
   {
      this.message = message;
      return (this);
   }

}
