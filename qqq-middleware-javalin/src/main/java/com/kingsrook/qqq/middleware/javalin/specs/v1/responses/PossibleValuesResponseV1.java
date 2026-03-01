/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
