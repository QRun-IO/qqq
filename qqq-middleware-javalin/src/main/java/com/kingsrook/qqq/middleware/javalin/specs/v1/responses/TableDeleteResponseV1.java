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


import java.util.List;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableDeleteOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;


/*******************************************************************************
 **
 *******************************************************************************/
public class TableDeleteResponseV1 implements TableDeleteOutputInterface, ToSchema
{
   @OpenAPIDescription("Number of records that were deleted")
   private Integer deletedRecordCount;

   @OpenAPIDescription("Why records were not deleted (for example, a record that was not found).  Omitted when every record was deleted.")
   @OpenAPIListItems(value = String.class)
   private List<String> errors;



   /*******************************************************************************
    ** Getter for deletedRecordCount
    *******************************************************************************/
   public Integer getDeletedRecordCount()
   {
      return (this.deletedRecordCount);
   }



   /*******************************************************************************
    ** Setter for deletedRecordCount
    *******************************************************************************/
   @Override
   public void setDeletedRecordCount(Integer deletedRecordCount)
   {
      this.deletedRecordCount = deletedRecordCount;
   }



   /*******************************************************************************
    ** Fluent setter for deletedRecordCount
    *******************************************************************************/
   public TableDeleteResponseV1 withDeletedRecordCount(Integer deletedRecordCount)
   {
      this.deletedRecordCount = deletedRecordCount;
      return (this);
   }



   /*******************************************************************************
    ** Getter for errors
    *******************************************************************************/
   public List<String> getErrors()
   {
      return (errors);
   }



   /*******************************************************************************
    ** Setter for errors
    *******************************************************************************/
   @Override
   public void setErrors(List<String> errors)
   {
      this.errors = errors;
   }



   /*******************************************************************************
    ** Fluent setter for errors
    *******************************************************************************/
   public TableDeleteResponseV1 withErrors(List<String> errors)
   {
      this.errors = errors;
      return (this);
   }

}
