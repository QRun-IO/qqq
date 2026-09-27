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
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableQueryOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.OutputRecord;


/*******************************************************************************
 **
 *******************************************************************************/
public class TableQueryResponseV1 implements TableQueryOutputInterface, ToSchema
{
   @OpenAPIDescription("List of records that satisfy the query request")
   @OpenAPIListItems(value = OutputRecord.class, useRef = true)
   private List<OutputRecord> records;



   /*******************************************************************************
    ** Setter for records
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
    ** Setter for records
    *******************************************************************************/
   public TableQueryResponseV1 withRecords(List<QRecord> records)
   {
      setRecords(records);
      return this;
   }



   /*******************************************************************************
    ** Getter for records
    *******************************************************************************/
   public List<OutputRecord> getRecords()
   {
      return (this.records);
   }

}
