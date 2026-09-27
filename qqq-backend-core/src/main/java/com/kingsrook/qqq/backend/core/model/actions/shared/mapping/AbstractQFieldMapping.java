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

package com.kingsrook.qqq.backend.core.model.actions.shared.mapping;


/*******************************************************************************
 ** For bulk-loads, define where a QField comes from in an input data source.
 **
 *******************************************************************************/
public abstract class AbstractQFieldMapping<T>
{

   /*******************************************************************************
    ** Enum to define the types of sources a mapping may use
    **
    *******************************************************************************/
   @SuppressWarnings("rawtypes")
   public enum SourceType
   {
      KEY(String.class),
      INDEX(Integer.class);

      private Class sourceClass;



      /*******************************************************************************
       ** enum constructor
       *******************************************************************************/
      SourceType(Class sourceClass)
      {
         this.sourceClass = sourceClass;
      }



      /*******************************************************************************
       ** Getter for sourceClass
       **
       *******************************************************************************/
      public Class getSourceClass()
      {
         return sourceClass;
      }
   }



   /*******************************************************************************
    ** For a given field, return its source - a key (e.g., from a json object or csv
    ** with a header row) or an index (for a csv w/o a header)
    **
    *******************************************************************************/
   public abstract T getFieldSource(String fieldName);


   /*******************************************************************************
    ** for a mapping instance, get what its source-type is
    **
    *******************************************************************************/
   public abstract SourceType getSourceType();
}
