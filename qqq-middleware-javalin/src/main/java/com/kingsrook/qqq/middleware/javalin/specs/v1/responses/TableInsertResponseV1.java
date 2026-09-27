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
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableInsertOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.OutputRecord;


/*******************************************************************************
 **
 *******************************************************************************/
public class TableInsertResponseV1 implements TableInsertOutputInterface, ToSchema
{
   @OpenAPIDescription("The record that was inserted")
   private OutputRecord record;

   @OpenAPIDescription("Any warnings that occurred during the insert")
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
         this.record = new OutputRecord(record, true);
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
   public TableInsertResponseV1 withRecord(QRecord record)
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
