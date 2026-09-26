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


import java.util.LinkedHashMap;
import java.util.Map;


/*******************************************************************************
 **
 *******************************************************************************/
public class RequestBody
{
   private Boolean              required = false;
   private String               description;
   private Map<String, Content> content;



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
   public RequestBody withRequired(Boolean required)
   {
      this.required = required;
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
   public RequestBody withDescription(String description)
   {
      this.description = description;
      return (this);
   }



   /*******************************************************************************
    ** Getter for content
    *******************************************************************************/
   public Map<String, Content> getContent()
   {
      return (this.content);
   }



   /*******************************************************************************
    ** Setter for content
    *******************************************************************************/
   public void setContent(Map<String, Content> content)
   {
      this.content = content;
   }



   /*******************************************************************************
    ** Fluent setter for content
    *******************************************************************************/
   public RequestBody withContent(Map<String, Content> content)
   {
      this.content = content;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for content
    *******************************************************************************/
   public RequestBody withContent(String key, Content content)
   {
      if(this.content == null)
      {
         this.content = new LinkedHashMap<>();
      }
      this.content.put(key, content);
      return (this);
   }

}
