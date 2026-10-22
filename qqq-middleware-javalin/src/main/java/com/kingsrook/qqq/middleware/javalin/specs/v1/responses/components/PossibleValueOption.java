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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components;


import java.io.Serializable;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;


/*******************************************************************************
 ** A single possible value option, with an id and a label.
 *******************************************************************************/
public class PossibleValueOption implements ToSchema
{
   @OpenAPIDescription("Unique identifier for this possible value option")
   private Serializable id;

   @OpenAPIDescription("Human-readable label for this possible value option")
   private String label;

   @OpenAPIDescription("For a picker request: the option's picker display values (thumbnail, preview, subtitle and detail fields), raw")
   private Map<String, Serializable> values;

   @OpenAPIDescription("For a picker request: formatted or translated display values, where they differ from the raw values")
   private Map<String, String> displayValues;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public PossibleValueOption()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public PossibleValueOption(QPossibleValue<?> qPossibleValue)
   {
      this.id = qPossibleValue.getId();
      this.label = qPossibleValue.getLabel();
   }



   /*******************************************************************************
    ** Getter for id
    *******************************************************************************/
   public Serializable getId()
   {
      return (this.id);
   }



   /*******************************************************************************
    ** Setter for id
    *******************************************************************************/
   public void setId(Serializable id)
   {
      this.id = id;
   }



   /*******************************************************************************
    ** Fluent setter for id
    *******************************************************************************/
   public PossibleValueOption withId(Serializable id)
   {
      this.id = id;
      return (this);
   }



   /*******************************************************************************
    ** Getter for label
    *******************************************************************************/
   public String getLabel()
   {
      return (this.label);
   }



   /*******************************************************************************
    ** Setter for label
    *******************************************************************************/
   public void setLabel(String label)
   {
      this.label = label;
   }



   /*******************************************************************************
    ** Fluent setter for label
    *******************************************************************************/
   public PossibleValueOption withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Getter for values
    *******************************************************************************/
   public Map<String, Serializable> getValues()
   {
      return (values);
   }



   /*******************************************************************************
    ** Setter for values
    *******************************************************************************/
   public void setValues(Map<String, Serializable> values)
   {
      this.values = values;
   }



   /*******************************************************************************
    ** Fluent setter for values
    *******************************************************************************/
   public PossibleValueOption withValues(Map<String, Serializable> values)
   {
      this.values = values;
      return (this);
   }



   /*******************************************************************************
    ** Getter for displayValues
    *******************************************************************************/
   public Map<String, String> getDisplayValues()
   {
      return (displayValues);
   }



   /*******************************************************************************
    ** Setter for displayValues
    *******************************************************************************/
   public void setDisplayValues(Map<String, String> displayValues)
   {
      this.displayValues = displayValues;
   }



   /*******************************************************************************
    ** Fluent setter for displayValues
    *******************************************************************************/
   public PossibleValueOption withDisplayValues(Map<String, String> displayValues)
   {
      this.displayValues = displayValues;
      return (this);
   }
}
