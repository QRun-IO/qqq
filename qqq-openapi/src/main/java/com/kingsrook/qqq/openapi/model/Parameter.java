/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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
