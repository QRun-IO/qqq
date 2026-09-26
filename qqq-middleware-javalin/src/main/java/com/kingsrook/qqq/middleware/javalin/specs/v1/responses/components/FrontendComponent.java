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
import com.kingsrook.qqq.backend.core.model.metadata.processes.QComponentType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendComponentMetaData;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIEnumSubSet;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIMapKnownEntries;


/*******************************************************************************
 **
 *******************************************************************************/
public class FrontendComponent implements ToSchema
{
   @OpenAPIExclude()
   private QFrontendComponentMetaData wrapped;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public FrontendComponent(QFrontendComponentMetaData wrapped)
   {
      this.wrapped = wrapped;
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public FrontendComponent()
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static class QComponentTypeSubSet implements OpenAPIEnumSubSet.EnumSubSet<QComponentType>
   {
      private static EnumSet<QComponentType> subSet = null;

      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public EnumSet<QComponentType> getSubSet()
      {
         if(subSet == null)
         {
            EnumSet<QComponentType> subSet = EnumSet.allOf(QComponentType.class);
            subSet.remove(QComponentType.BULK_LOAD_FILE_MAPPING_FORM);
            subSet.remove(QComponentType.BULK_LOAD_VALUE_MAPPING_FORM);
            subSet.remove(QComponentType.BULK_LOAD_PROFILE_FORM);
            QComponentTypeSubSet.subSet = subSet;
         }

         return(subSet);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("The type of this component.  e.g., what kind of UI element(s) should be presented to the user.")
   @OpenAPIEnumSubSet(QComponentTypeSubSet.class)
   public QComponentType getType()
   {
      return (this.wrapped.getType());
   }



   /*******************************************************************************
    ** Getter for values
    **
    *******************************************************************************/
   @OpenAPIDescription("Name-value pairs specific to the type of component.")
   @OpenAPIMapKnownEntries(value = FrontendComponentValues.class, useRef = true)
   public Map<String, Serializable> getValues()
   {
      return (this.wrapped.getValues() == null ? null : new FrontendComponentValues(this.wrapped.getValues()).toMap());
   }

}
