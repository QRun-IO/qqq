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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses;


import com.kingsrook.qqq.middleware.javalin.executors.io.TableCountOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;


/*******************************************************************************
 **
 *******************************************************************************/
public class TableCountResponseV1 implements TableCountOutputInterface, ToSchema
{
   @OpenAPIDescription("Number (count) of records that satisfy the query request")
   private Long count;

   @OpenAPIDescription("Number (count) of distinct records that satisfy the query request.  Only included if requested.")
   private Long distinctCount;


   /*******************************************************************************
    ** Getter for count
    *******************************************************************************/
   public Long getCount()
   {
      return (this.count);
   }



   /*******************************************************************************
    ** Setter for count
    *******************************************************************************/
   public void setCount(Long count)
   {
      this.count = count;
   }



   /*******************************************************************************
    ** Fluent setter for count
    *******************************************************************************/
   public TableCountResponseV1 withCount(Long count)
   {
      this.count = count;
      return (this);
   }


   /*******************************************************************************
    ** Getter for distinctCount
    *******************************************************************************/
   public Long getDistinctCount()
   {
      return (this.distinctCount);
   }



   /*******************************************************************************
    ** Setter for distinctCount
    *******************************************************************************/
   public void setDistinctCount(Long distinctCount)
   {
      this.distinctCount = distinctCount;
   }



   /*******************************************************************************
    ** Fluent setter for distinctCount
    *******************************************************************************/
   public TableCountResponseV1 withDistinctCount(Long distinctCount)
   {
      this.distinctCount = distinctCount;
      return (this);
   }


}
