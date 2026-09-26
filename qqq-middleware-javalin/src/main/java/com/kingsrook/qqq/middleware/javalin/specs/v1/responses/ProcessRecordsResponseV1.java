/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.middleware.javalin.executors.io.ProcessRecordsOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.OutputRecord;


/*******************************************************************************
 ** V1 response object for process records.
 *******************************************************************************/
public class ProcessRecordsResponseV1 implements ProcessRecordsOutputInterface, ToSchema
{
   @OpenAPIDescription("List of records from the process state")
   @OpenAPIListItems(value = OutputRecord.class, useRef = true)
   private List<OutputRecord> records;

   @OpenAPIDescription("Total number of records in the process state (before pagination)")
   private Integer totalRecords;



   /*******************************************************************************
    ** Setter for records - converts QRecords to OutputRecords.
    *******************************************************************************/
   @Override
   public void setRecords(List<QRecord> records)
   {
      if(records == null)
      {
         this.records = null;
      }
      else
      {
         this.records = records.stream().map(qr -> new OutputRecord(qr)).collect(Collectors.toList());
      }
   }



   /*******************************************************************************
    ** Fluent setter for records
    *******************************************************************************/
   public ProcessRecordsResponseV1 withRecords(List<QRecord> records)
   {
      setRecords(records);
      return (this);
   }



   /*******************************************************************************
    ** Getter for records
    *******************************************************************************/
   public List<OutputRecord> getRecords()
   {
      return (this.records);
   }



   /*******************************************************************************
    ** Setter for totalRecords
    *******************************************************************************/
   @Override
   public void setTotalRecords(Integer totalRecords)
   {
      this.totalRecords = totalRecords;
   }



   /*******************************************************************************
    ** Fluent setter for totalRecords
    *******************************************************************************/
   public ProcessRecordsResponseV1 withTotalRecords(Integer totalRecords)
   {
      this.totalRecords = totalRecords;
      return (this);
   }



   /*******************************************************************************
    ** Getter for totalRecords
    *******************************************************************************/
   public Integer getTotalRecords()
   {
      return (this.totalRecords);
   }

}
