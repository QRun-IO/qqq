/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses;


import com.kingsrook.qqq.middleware.javalin.executors.io.TableDeleteOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;


/*******************************************************************************
 **
 *******************************************************************************/
public class TableDeleteResponseV1 implements TableDeleteOutputInterface, ToSchema
{
   @OpenAPIDescription("Number of records that were deleted")
   private Integer deletedRecordCount;



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

}
