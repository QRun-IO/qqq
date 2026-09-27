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

package com.kingsrook.qqq.middleware.javalin.specs.v1.utils;


import com.kingsrook.qqq.middleware.javalin.specs.TagsInterface;


/*******************************************************************************
 **
 *******************************************************************************/
public enum TagsV1 implements TagsInterface
{
   AUTHENTICATION("Authentication"),
   GENERAL("General"),
   TABLES("Tables"),
   PROCESSES("Processes"),
   REPORTS("Reports"),
   WIDGETS("Widgets");


   private final String text;



   /***************************************************************************
    **
    ***************************************************************************/
   TagsV1(String text)
   {
      this.text = text;
   }



   /*******************************************************************************
    ** Getter for text
    **
    *******************************************************************************/
   public String getText()
   {
      return text;
   }
}
