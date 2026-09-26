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


import java.io.Serializable;
import java.util.EnumSet;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIEnumSubSet;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;


/*******************************************************************************
 **
 *******************************************************************************/
public class FieldAdornment implements ToSchema
{
   @OpenAPIExclude()
   private com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAdornment wrapped;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public FieldAdornment(com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAdornment wrapped)
   {
      this.wrapped = wrapped;
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public FieldAdornment()
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static class FieldAdornmentSubSet implements OpenAPIEnumSubSet.EnumSubSet<AdornmentType>
   {
      private static EnumSet<AdornmentType> subSet = null;



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public EnumSet<AdornmentType> getSubSet()
      {
         if(subSet == null)
         {
            EnumSet<AdornmentType> subSet = EnumSet.allOf(AdornmentType.class);
            subSet.remove(AdornmentType.FILE_UPLOAD); // todo - remove for next version!
            subSet.remove(AdornmentType.TOOLTIP); // todo - remove for next version!
            FieldAdornmentSubSet.subSet = subSet;
         }

         return (subSet);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Type of this adornment")
   @OpenAPIEnumSubSet(FieldAdornmentSubSet.class)
   public AdornmentType getType()
   {
      return (this.wrapped == null || this.wrapped.getType() == null ? null : this.wrapped.getType());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Values associated with this adornment.  Keys and the meanings of their values will differ by type.")
   public Map<String, Serializable> getValues()
   {
      return (this.wrapped.getValues());
   }

}
