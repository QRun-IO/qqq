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

package com.kingsrook.qqq.api.model.actions;


import com.kingsrook.qqq.backend.core.model.actions.AbstractActionOutput;
import com.kingsrook.qqq.openapi.model.OpenAPI;


/*******************************************************************************
 **
 *******************************************************************************/
public class GenerateOpenApiSpecOutput extends AbstractActionOutput
{
   private OpenAPI openAPI;
   private String  yaml;
   private String  json;



   /*******************************************************************************
    ** Getter for openAPI
    *******************************************************************************/
   public OpenAPI getOpenAPI()
   {
      return (this.openAPI);
   }



   /*******************************************************************************
    ** Setter for openAPI
    *******************************************************************************/
   public void setOpenAPI(OpenAPI openAPI)
   {
      this.openAPI = openAPI;
   }



   /*******************************************************************************
    ** Fluent setter for openAPI
    *******************************************************************************/
   public GenerateOpenApiSpecOutput withOpenAPI(OpenAPI openAPI)
   {
      this.openAPI = openAPI;
      return (this);
   }



   /*******************************************************************************
    ** Getter for yaml
    *******************************************************************************/
   public String getYaml()
   {
      return (this.yaml);
   }



   /*******************************************************************************
    ** Setter for yaml
    *******************************************************************************/
   public void setYaml(String yaml)
   {
      this.yaml = yaml;
   }



   /*******************************************************************************
    ** Fluent setter for yaml
    *******************************************************************************/
   public GenerateOpenApiSpecOutput withYaml(String yaml)
   {
      this.yaml = yaml;
      return (this);
   }



   /*******************************************************************************
    ** Getter for json
    *******************************************************************************/
   public String getJson()
   {
      return (this.json);
   }



   /*******************************************************************************
    ** Setter for json
    *******************************************************************************/
   public void setJson(String json)
   {
      this.json = json;
   }



   /*******************************************************************************
    ** Fluent setter for json
    *******************************************************************************/
   public GenerateOpenApiSpecOutput withJson(String json)
   {
      this.json = json;
      return (this);
   }

}
