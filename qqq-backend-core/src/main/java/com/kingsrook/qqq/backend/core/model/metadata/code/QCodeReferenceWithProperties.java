/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.code;


import java.io.Serializable;
import java.util.Map;
import java.util.Objects;


/*******************************************************************************
 ** a code reference that also has a map of properties.  This object (with the
 ** properties) will be passed in to the referenced object, if it implements
 ** InitializableViaCodeReference.
 *******************************************************************************/
public class QCodeReferenceWithProperties extends QCodeReference
{
   private final Map<String, Serializable> properties;



   /***************************************************************************
    **
    ***************************************************************************/
   public QCodeReferenceWithProperties(Class<?> javaClass, Map<String, Serializable> properties)
   {
      super(javaClass);
      this.properties = properties;
   }



   /*******************************************************************************
    ** Getter for properties
    **
    *******************************************************************************/
   public Map<String, Serializable> getProperties()
   {
      return properties;
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public boolean equals(Object o)
   {
      if(o == null || getClass() != o.getClass())
      {
         return false;
      }
      if(!super.equals(o))
      {
         return false;
      }
      QCodeReferenceWithProperties that = (QCodeReferenceWithProperties) o;
      return Objects.equals(properties, that.properties);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public int hashCode()
   {
      return Objects.hash(super.hashCode(), properties);
   }
}
