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
import com.fasterxml.jackson.annotation.JsonGetter;


/*******************************************************************************
 **
 *******************************************************************************/
public class Response
{
   private String               description;
   private Map<String, Content> content;
   private String               ref;



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
   public Response withDescription(String description)
   {
      this.description = description;
      return (this);
   }



   /*******************************************************************************
    ** Getter for ref
    *******************************************************************************/
   @JsonGetter("$ref")
   public String getRef()
   {
      return (this.ref);
   }



   /*******************************************************************************
    ** Setter for ref
    *******************************************************************************/
   public void setRef(String ref)
   {
      this.ref = ref;
   }



   /*******************************************************************************
    ** Fluent setter for ref
    *******************************************************************************/
   public Response withRef(String ref)
   {
      this.ref = ref;
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
   public Response withContent(Map<String, Content> content)
   {
      this.content = content;
      return (this);
   }

}
