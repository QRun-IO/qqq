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


import com.fasterxml.jackson.annotation.JsonIgnore;


/*******************************************************************************
 **
 *******************************************************************************/
public class SecurityScheme
{
   private SecuritySchemeType type;

   private String name;
   private String in;
   private String scheme;
   private String bearerFormat;



   /*******************************************************************************
    ** Getter for scheme
    *******************************************************************************/
   public String getScheme()
   {
      return (this.scheme);
   }



   /*******************************************************************************
    ** Setter for scheme
    *******************************************************************************/
   public void setScheme(String scheme)
   {
      this.scheme = scheme;
   }



   /*******************************************************************************
    ** Fluent setter for scheme
    *******************************************************************************/
   public SecurityScheme withScheme(String scheme)
   {
      this.scheme = scheme;
      return (this);
   }



   /*******************************************************************************
    ** Getter for bearerFormat
    *******************************************************************************/
   public String getBearerFormat()
   {
      return (this.bearerFormat);
   }



   /*******************************************************************************
    ** Setter for bearerFormat
    *******************************************************************************/
   public void setBearerFormat(String bearerFormat)
   {
      this.bearerFormat = bearerFormat;
   }



   /*******************************************************************************
    ** Fluent setter for bearerFormat
    *******************************************************************************/
   public SecurityScheme withBearerFormat(String bearerFormat)
   {
      this.bearerFormat = bearerFormat;
      return (this);
   }



   /*******************************************************************************
    ** Getter for type
    *******************************************************************************/
   public String getType()
   {
      return (this.type.getType());
   }



   /*******************************************************************************
    ** Getter for type
    *******************************************************************************/
   @JsonIgnore
   public SecuritySchemeType getTypeEnum()
   {
      return (this.type);
   }



   /*******************************************************************************
    ** Setter for type
    *******************************************************************************/
   public void setType(SecuritySchemeType type)
   {
      this.type = type;
   }



   /*******************************************************************************
    ** Fluent setter for type
    *******************************************************************************/
   public SecurityScheme withType(SecuritySchemeType type)
   {
      this.type = type;
      return (this);
   }



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
   public SecurityScheme withName(String name)
   {
      this.name = name;
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
   public void setIn(String in)
   {
      this.in = in;
   }



   /*******************************************************************************
    ** Fluent setter for in
    *******************************************************************************/
   public SecurityScheme withIn(String in)
   {
      this.in = in;
      return (this);
   }

}
