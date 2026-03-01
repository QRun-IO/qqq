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


import java.util.List;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableUpdateOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.OutputRecord;


/*******************************************************************************
 **
 *******************************************************************************/
public class TableUpdateResponseV1 implements TableUpdateOutputInterface, ToSchema
{
   @OpenAPIDescription("The record that was updated")
   private OutputRecord record;

   @OpenAPIDescription("Any warnings that occurred during the update")
   @OpenAPIListItems(value = String.class)
   private List<String> warnings;



   /*******************************************************************************
    ** Setter for record
    *******************************************************************************/
   @Override
   public void setRecord(QRecord record)
   {
      if(record == null)
      {
         this.record = null;
      }
      else
      {
         this.record = new OutputRecord(record);
      }
   }



   /*******************************************************************************
    ** Setter for errors
    *******************************************************************************/
   @Override
   public void setErrors(List<String> errors)
   {
      //////////////////////////////////////////////////////////////////////////
      // errors are thrown as exceptions in the executor, not set on response //
      //////////////////////////////////////////////////////////////////////////
   }



   /*******************************************************************************
    ** Setter for warnings
    *******************************************************************************/
   @Override
   public void setWarnings(List<String> warnings)
   {
      this.warnings = warnings;
   }



   /*******************************************************************************
    ** Fluent setter for record
    *******************************************************************************/
   public TableUpdateResponseV1 withRecord(QRecord record)
   {
      setRecord(record);
      return (this);
   }



   /*******************************************************************************
    ** Getter for record
    *******************************************************************************/
   public OutputRecord getRecord()
   {
      return (this.record);
   }



   /*******************************************************************************
    ** Getter for warnings
    *******************************************************************************/
   public List<String> getWarnings()
   {
      return (this.warnings);
   }

}
