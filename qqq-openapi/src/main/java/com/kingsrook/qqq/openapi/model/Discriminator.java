/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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
public class Discriminator
{
   private String              propertyName;
   private Map<String, String> mapping;



   /*******************************************************************************
    ** Getter for propertyName
    *******************************************************************************/
   public String getPropertyName()
   {
      return (this.propertyName);
   }



   /*******************************************************************************
    ** Setter for propertyName
    *******************************************************************************/
   public void setPropertyName(String propertyName)
   {
      this.propertyName = propertyName;
   }



   /*******************************************************************************
    ** Fluent setter for propertyName
    *******************************************************************************/
   public Discriminator withPropertyName(String propertyName)
   {
      this.propertyName = propertyName;
      return (this);
   }


   /*******************************************************************************
    ** Getter for mapping
    *******************************************************************************/
   public Map<String, String> getMapping()
   {
      return (this.mapping);
   }



   /*******************************************************************************
    ** Setter for mapping
    *******************************************************************************/
   public void setMapping(Map<String, String> mapping)
   {
      this.mapping = mapping;
   }



   /*******************************************************************************
    ** Fluent setter for mapping
    *******************************************************************************/
   public Discriminator withMapping(Map<String, String> mapping)
   {
      this.mapping = mapping;
      return (this);
   }


}
