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
public class Components
{
   private Map<String, Example>        examples;
   private Map<String, Schema>         schemas;
   private Map<String, Response>       responses;
   private Map<String, SecurityScheme> securitySchemes;



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
   public Components withExamples(Map<String, Example> examples)
   {
      this.examples = examples;
      return (this);
   }



   /*******************************************************************************
    ** Getter for schemas
    *******************************************************************************/
   public Map<String, Schema> getSchemas()
   {
      return (this.schemas);
   }



   /*******************************************************************************
    ** Setter for schemas
    *******************************************************************************/
   public void setSchemas(Map<String, Schema> schemas)
   {
      this.schemas = schemas;
   }



   /*******************************************************************************
    ** Fluent setter for schemas
    *******************************************************************************/
   public Components withSchemas(Map<String, Schema> schemas)
   {
      this.schemas = schemas;
      return (this);
   }



   /*******************************************************************************
    ** Getter for responses
    *******************************************************************************/
   public Map<String, Response> getResponses()
   {
      return (this.responses);
   }



   /*******************************************************************************
    ** Setter for responses
    *******************************************************************************/
   public void setResponses(Map<String, Response> responses)
   {
      this.responses = responses;
   }



   /*******************************************************************************
    ** Fluent setter for responses
    *******************************************************************************/
   public Components withResponses(Map<String, Response> responses)
   {
      this.responses = responses;
      return (this);
   }



   /*******************************************************************************
    ** Getter for securitySchemes
    *******************************************************************************/
   public Map<String, SecurityScheme> getSecuritySchemes()
   {
      return (this.securitySchemes);
   }



   /*******************************************************************************
    ** Setter for securitySchemes
    *******************************************************************************/
   public void setSecuritySchemes(Map<String, SecurityScheme> securitySchemes)
   {
      this.securitySchemes = securitySchemes;
   }



   /*******************************************************************************
    ** Fluent setter for securitySchemes
    *******************************************************************************/
   public Components withSecuritySchemes(Map<String, SecurityScheme> securitySchemes)
   {
      this.securitySchemes = securitySchemes;
      return (this);
   }

}
