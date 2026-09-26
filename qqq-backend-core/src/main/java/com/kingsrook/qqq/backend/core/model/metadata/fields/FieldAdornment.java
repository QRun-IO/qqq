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

package com.kingsrook.qqq.backend.core.model.metadata.fields;


import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kingsrook.qqq.backend.core.utils.Pair;


/*******************************************************************************
 ** Special fancy things that fields might do in UIs.
 *******************************************************************************/
public class FieldAdornment
{
   private AdornmentType             type;
   private Map<String, Serializable> values = new HashMap<>();



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public FieldAdornment()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public FieldAdornment(AdornmentType type)
   {
      this.type = type;
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public FieldAdornment(AdornmentType type, Map<String, Serializable> values)
   {
      this.type = type;
      this.values = values;
   }



   /*******************************************************************************
    ** Getter for type
    **
    *******************************************************************************/
   public AdornmentType getType()
   {
      return type;
   }



   /*******************************************************************************
    ** Setter for type
    **
    *******************************************************************************/
   public void setType(AdornmentType type)
   {
      this.type = type;
   }



   /*******************************************************************************
    ** Fluent setter for type
    **
    *******************************************************************************/
   public FieldAdornment withType(AdornmentType type)
   {
      this.type = type;
      return (this);
   }



   /*******************************************************************************
    ** Getter for values
    **
    *******************************************************************************/
   public Map<String, Serializable> getValues()
   {
      return values;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @JsonIgnore
   public Optional<Serializable> getValue(String key)
   {
      if(key != null && values != null)
      {
         return (Optional.ofNullable(values.get(key)));
      }

      return (Optional.empty());
   }



   /*******************************************************************************
    ** Setter for values
    **
    *******************************************************************************/
   public void setValues(Map<String, Serializable> values)
   {
      this.values = values;
   }



   /*******************************************************************************
    ** Fluent setter for values
    **
    *******************************************************************************/
   public FieldAdornment withValues(Map<String, Serializable> values)
   {
      this.values = values;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for values
    **
    *******************************************************************************/
   public FieldAdornment withValue(String key, Serializable value)
   {
      if(this.values == null)
      {
         this.values = new HashMap<>();
      }
      this.values.put(key, value);
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for values
    **
    *******************************************************************************/
   public FieldAdornment withValue(Pair<String, ? extends Serializable> value)
   {
      return (withValue(value.getA(), value.getB()));
   }



   /*******************************************************************************
    ** Fluent setter for values
    **
    *******************************************************************************/
   @SafeVarargs
   public final FieldAdornment withValues(Pair<String, Serializable>... values)
   {
      for(Pair<String, Serializable> value : values)
      {
         withValue(value);
      }

      return (this);
   }

}
