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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses;


import java.util.List;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.middleware.javalin.executors.io.PossibleValuesOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.PossibleValueOption;


/*******************************************************************************
 ** Response object for possible values searches.
 *******************************************************************************/
public class PossibleValuesResponseV1 implements PossibleValuesOutputInterface, ToSchema
{
   @OpenAPIDescription("List of possible value options matching the search request")
   @OpenAPIListItems(value = PossibleValueOption.class, useRef = true)
   private List<PossibleValueOption> options;



   /*******************************************************************************
    ** Setter for options - from QPossibleValue list (from the output interface)
    *******************************************************************************/
   @Override
   public void setOptions(List<QPossibleValue<?>> options)
   {
      if(options == null)
      {
         this.options = null;
      }
      else
      {
         this.options = options.stream().map(PossibleValueOption::new).collect(Collectors.toList());
      }
   }



   /*******************************************************************************
    ** Fluent setter for options - from QPossibleValue list
    *******************************************************************************/
   public PossibleValuesResponseV1 withOptions(List<QPossibleValue<?>> options)
   {
      setOptions(options);
      return (this);
   }



   /*******************************************************************************
    ** Getter for options
    *******************************************************************************/
   public List<PossibleValueOption> getOptions()
   {
      return (this.options);
   }

}
