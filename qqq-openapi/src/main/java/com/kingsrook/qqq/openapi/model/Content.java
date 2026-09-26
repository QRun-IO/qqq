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
public class Content
{
   private Schema               schema;
   private Map<String, Example> examples;



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
   public Content withSchema(Schema schema)
   {
      this.schema = schema;
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
   public Content withExamples(Map<String, Example> examples)
   {
      this.examples = examples;
      return (this);
   }

}
