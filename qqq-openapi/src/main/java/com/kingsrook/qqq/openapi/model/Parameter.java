/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.openapi.model;


import java.util.Map;


/*******************************************************************************
 **
 *******************************************************************************/
public class Parameter
{
   private String               name;
   private String               description;
   private Boolean              required;
   private String               in;
   private Schema               schema;
   private Boolean              explode;
   private Map<String, Example> examples;
   private Object               example;



   /*******************************************************************************
    ** Getter for name
    *******************************************************************************/
   public String getName()
   {
      return (this.name);
   }



   /*******************************************************************************
    ** Setter for name
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Fluent setter for name
    *******************************************************************************/
   public Parameter withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for description
    *******************************************************************************/
   public String getDescription()
   {
      return (this.description);
   }



   /*******************************************************************************
    ** Setter for description
    *******************************************************************************/
   public void setDescription(String description)
   {
      this.description = description;
   }



   /*******************************************************************************
    ** Fluent setter for description
    *******************************************************************************/
   public Parameter withDescription(String description)
   {
      this.description = description;
      return (this);
   }



   /*******************************************************************************
    ** Getter for in
    *******************************************************************************/
   public String getIn()
   {
      return (this.in);
   }



   /*******************************************************************************
    ** Setter for in
    *******************************************************************************/
   public void setIn(In in)
   {
      this.in = in.toString().toLowerCase();
   }



   /*******************************************************************************
    ** Fluent setter for in
    *******************************************************************************/
   public Parameter withIn(In in)
   {
      setIn(in);
      return (this);
   }



   /*******************************************************************************
    ** Getter for schema
    *******************************************************************************/
   public Schema getSchema()
   {
      return (this.schema);
   }



   /*******************************************************************************
    ** Setter for schema
    *******************************************************************************/
   public void setSchema(Schema schema)
   {
      this.schema = schema;
   }



   /*******************************************************************************
    ** Fluent setter for schema
    *******************************************************************************/
   public Parameter withSchema(Schema schema)
   {
      this.schema = schema;
      return (this);
   }



   /*******************************************************************************
    ** Getter for explode
    *******************************************************************************/
   public Boolean getExplode()
   {
      return (this.explode);
   }



   /*******************************************************************************
    ** Setter for explode
    *******************************************************************************/
   public void setExplode(Boolean explode)
   {
      this.explode = explode;
   }



   /*******************************************************************************
    ** Fluent setter for explode
    *******************************************************************************/
   public Parameter withExplode(Boolean explode)
   {
      this.explode = explode;
      return (this);
   }



   /*******************************************************************************
    ** Getter for examples
    *******************************************************************************/
   public Map<String, Example> getExamples()
   {
      return (this.examples);
   }



   /*******************************************************************************
    ** Setter for examples
    *******************************************************************************/
   public void setExamples(Map<String, Example> examples)
   {
      this.examples = examples;
   }



   /*******************************************************************************
    ** Fluent setter for examples
    *******************************************************************************/
   public Parameter withExamples(Map<String, Example> examples)
   {
      this.examples = examples;
      return (this);
   }



   /*******************************************************************************
    ** Getter for required
    *******************************************************************************/
   public Boolean getRequired()
   {
      return (this.required);
   }



   /*******************************************************************************
    ** Setter for required
    *******************************************************************************/
   public void setRequired(Boolean required)
   {
      this.required = required;
   }



   /*******************************************************************************
    ** Fluent setter for required
    *******************************************************************************/
   public Parameter withRequired(Boolean required)
   {
      this.required = required;
      return (this);
   }



   /*******************************************************************************
    ** Getter for example
    *******************************************************************************/
   public Object getExample()
   {
      return (this.example);
   }



   /*******************************************************************************
    ** Setter for example
    *******************************************************************************/
   public void setExample(Example example)
   {
      this.example = example;
   }



   /*******************************************************************************
    ** Fluent setter for example
    *******************************************************************************/
   public Parameter withExample(Example example)
   {
      this.example = example;
      return (this);
   }


   /*******************************************************************************
    ** Setter for example
    *******************************************************************************/
   public void setExample(String example)
   {
      this.example = example;
   }



   /*******************************************************************************
    ** Fluent setter for example
    *******************************************************************************/
   public Parameter withExample(String example)
   {
      this.example = example;
      return (this);
   }

}
