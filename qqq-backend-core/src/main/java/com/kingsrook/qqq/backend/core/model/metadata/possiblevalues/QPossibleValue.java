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

package com.kingsrook.qqq.backend.core.model.metadata.possiblevalues;


import java.io.Serializable;
import java.util.Objects;


/*******************************************************************************
 ** An actual possible value - an id and label.
 **
 ** Type parameter `T` is the type of the id (often Integer, maybe String)
 *******************************************************************************/
public class QPossibleValue<T extends Serializable>
{
   private final T      id;
   private final String label;



   /*******************************************************************************
    **
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   public QPossibleValue(String value)
   {
      this.id = (T) value;
      this.label = value;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QPossibleValue(T id, String label)
   {
      this.id = id;
      this.label = label;
   }



   /*******************************************************************************
    ** Getter for id
    **
    *******************************************************************************/
   public T getId()
   {
      return id;
   }



   /*******************************************************************************
    ** Getter for label
    **
    *******************************************************************************/
   public String getLabel()
   {
      return label;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean equals(Object o)
   {
      if(this == o)
      {
         return true;
      }

      if(o == null || getClass() != o.getClass())
      {
         return false;
      }

      QPossibleValue<?> that = (QPossibleValue<?>) o;
      return Objects.equals(id, that.id) && Objects.equals(label, that.label);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public int hashCode()
   {
      return Objects.hash(id, label);
   }
}
