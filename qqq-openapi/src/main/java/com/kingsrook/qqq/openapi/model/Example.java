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


import com.fasterxml.jackson.annotation.JsonGetter;


/*******************************************************************************
 **
 *******************************************************************************/
public class Example
{
   private String summary;
   private String ref;
   private Object value;



   /*******************************************************************************
    ** Getter for summary
    *******************************************************************************/
   public String getSummary()
   {
      return (this.summary);
   }



   /*******************************************************************************
    ** Setter for summary
    *******************************************************************************/
   public void setSummary(String summary)
   {
      this.summary = summary;
   }



   /*******************************************************************************
    ** Fluent setter for summary
    *******************************************************************************/
   public Example withSummary(String summary)
   {
      this.summary = summary;
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
   public Example withRef(String ref)
   {
      this.ref = ref;
      return (this);
   }


   /*******************************************************************************
    ** Getter for value
    *******************************************************************************/
   public Object getValue()
   {
      return (this.value);
   }



   /*******************************************************************************
    ** Setter for value
    *******************************************************************************/
   public void setValue(Object value)
   {
      this.value = value;
   }



   /*******************************************************************************
    ** Fluent setter for value
    *******************************************************************************/
   public Example withValue(Object value)
   {
      this.value = value;
      return (this);
   }


}
