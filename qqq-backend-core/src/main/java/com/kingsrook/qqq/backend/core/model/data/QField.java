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

package com.kingsrook.qqq.backend.core.model.data;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DynamicDefaultValueBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;


/*******************************************************************************
 ** Annotation to place onto fields in a QRecordEntity, to add additional attributes
 ** for propagating down into the corresponding QFieldMetaData
 **
 *******************************************************************************/
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface QField
{
   /*******************************************************************************
    **
    *******************************************************************************/
   String label() default "";

   /*******************************************************************************
    **
    *******************************************************************************/
   String backendName() default "";

   /*******************************************************************************
    **
    *******************************************************************************/
   boolean isPrimaryKey() default false;

   /*******************************************************************************
    **
    *******************************************************************************/
   boolean isRequired() default false;

   /*******************************************************************************
    **
    *******************************************************************************/
   boolean isEditable() default true;

   /*******************************************************************************
    **
    *******************************************************************************/
   boolean isHidden() default false;

   /*******************************************************************************
    **
    *******************************************************************************/
   String defaultValue() default "";

   /*******************************************************************************
    **
    *******************************************************************************/
   String displayFormat() default "";

   /*******************************************************************************
    **
    *******************************************************************************/
   String possibleValueSourceName() default "";

   /*******************************************************************************
    **
    *******************************************************************************/
   int maxLength() default Integer.MAX_VALUE;

   /*******************************************************************************
    **
    *******************************************************************************/
   int gridColumns() default -1;

   /*******************************************************************************
    **
    *******************************************************************************/
   ValueTooLongBehavior valueTooLongBehavior() default ValueTooLongBehavior.PASS_THROUGH;

   /*******************************************************************************
    **
    *******************************************************************************/
   DynamicDefaultValueBehavior dynamicDefaultValueBehavior() default DynamicDefaultValueBehavior.NONE;

   //////////////////////////////////////////////////////////////////////////////////////////
   // new attributes here likely need implementation in QFieldMetaData.constructFromGetter //
   //////////////////////////////////////////////////////////////////////////////////////////
}
