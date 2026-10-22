/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components;


import java.util.List;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;


/*******************************************************************************
 ** A line of the bulk-insert process summary.
 *******************************************************************************/
public class BulkLoadSummaryLine implements ToSchema
{
   @OpenAPIDescription("OK, INFO, WARNING or ERROR.")
   private String status;

   @OpenAPIDescription("Number of records the line describes.")
   private Integer count;

   @OpenAPIDescription("The standard process summary message.")
   private String message;

   @OpenAPIDescription("Example rows (and their values, unless masked for a classified field).")
   @OpenAPIListItems(value = String.class, useRef = false)
   private List<String> bulletsOfText;



   /*******************************************************************************
    ** Getter for status
    *******************************************************************************/
   public String getStatus()
   {
      return (this.status);
   }



   /*******************************************************************************
    ** Fluent setter for status
    *******************************************************************************/
   public BulkLoadSummaryLine withStatus(String status)
   {
      this.status = status;
      return (this);
   }



   /*******************************************************************************
    ** Getter for count
    *******************************************************************************/
   public Integer getCount()
   {
      return (this.count);
   }



   /*******************************************************************************
    ** Fluent setter for count
    *******************************************************************************/
   public BulkLoadSummaryLine withCount(Integer count)
   {
      this.count = count;
      return (this);
   }



   /*******************************************************************************
    ** Getter for message
    *******************************************************************************/
   public String getMessage()
   {
      return (this.message);
   }



   /*******************************************************************************
    ** Fluent setter for message
    *******************************************************************************/
   public BulkLoadSummaryLine withMessage(String message)
   {
      this.message = message;
      return (this);
   }



   /*******************************************************************************
    ** Getter for bulletsOfText
    *******************************************************************************/
   public List<String> getBulletsOfText()
   {
      return (this.bulletsOfText);
   }



   /*******************************************************************************
    ** Fluent setter for bulletsOfText
    *******************************************************************************/
   public BulkLoadSummaryLine withBulletsOfText(List<String> bulletsOfText)
   {
      this.bulletsOfText = bulletsOfText;
      return (this);
   }

}
