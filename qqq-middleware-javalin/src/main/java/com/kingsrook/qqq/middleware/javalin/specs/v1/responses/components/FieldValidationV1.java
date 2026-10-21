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


import com.fasterxml.jackson.annotation.JsonInclude;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendFieldValidation;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;


/*******************************************************************************
 ** A field's validation rules, as the server enforces them on insert and
 ** update (UI API 1.6.0).  A client may check values before saving; the server
 ** stays the authority, so a rule not listed is never assumed.
 *******************************************************************************/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FieldValidationV1 implements ToSchema
{
   @OpenAPIExclude()
   private QFrontendFieldValidation wrapped;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public FieldValidationV1()
   {
   }



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public FieldValidationV1(QFrontendFieldValidation wrapped)
   {
      this.wrapped = wrapped;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @OpenAPIDescription("True when a value is required.")
   public Boolean getRequired()
   {
      return (wrapped == null ? null : wrapped.getRequired());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @OpenAPIDescription("For string fields, the most characters the field stores.  What happens to a longer value is tooLongBehavior.")
   public Integer getMaxLength()
   {
      return (wrapped == null ? null : wrapped.getMaxLength());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @OpenAPIDescription("With maxLength: ERROR (a longer value is refused), TRUNCATE or TRUNCATE_ELLIPSIS (it is trimmed on save), PASS_THROUGH (neither).")
   public String getTooLongBehavior()
   {
      return (wrapped == null ? null : wrapped.getTooLongBehavior());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @OpenAPIDescription("For number fields, the smallest value accepted (plain digits, exact).  See minInclusive.")
   public String getMinValue()
   {
      return (wrapped == null ? null : wrapped.getMinValue());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @OpenAPIDescription("Whether minValue itself is accepted (false: the value must be greater).")
   public Boolean getMinInclusive()
   {
      return (wrapped == null ? null : wrapped.getMinInclusive());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @OpenAPIDescription("For number fields, the largest value accepted (plain digits, exact).  See maxInclusive.")
   public String getMaxValue()
   {
      return (wrapped == null ? null : wrapped.getMaxValue());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @OpenAPIDescription("Whether maxValue itself is accepted (false: the value must be less).")
   public Boolean getMaxInclusive()
   {
      return (wrapped == null ? null : wrapped.getMaxInclusive());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @OpenAPIDescription("A regular expression a non-empty value must match as a whole (written to mean the same in Java and ECMAScript).")
   public String getPattern()
   {
      return (wrapped == null ? null : wrapped.getPattern());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @OpenAPIDescription("The message the server gives for a value that does not match pattern.")
   public String getPatternMessage()
   {
      return (wrapped == null ? null : wrapped.getPatternMessage());
   }
}
