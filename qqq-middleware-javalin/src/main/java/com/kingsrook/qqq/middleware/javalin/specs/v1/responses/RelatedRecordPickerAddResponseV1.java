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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses;


import java.util.List;
import com.kingsrook.qqq.backend.core.model.actions.tables.picker.RelatedRecordPickerOutput;
import com.kingsrook.qqq.middleware.javalin.executors.io.RelatedRecordPickerOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.OutputRecord;


/*******************************************************************************
 ** Response of the relatedRecordPicker add route: the inserted rows.
 *******************************************************************************/
public class RelatedRecordPickerAddResponseV1 implements RelatedRecordPickerOutputInterface, ToSchema
{
   @OpenAPIDescription("The inserted child or join rows")
   @OpenAPIListItems(value = OutputRecord.class, useRef = true)
   private List<OutputRecord> records;

   @OpenAPIDescription("Warnings from the inserts, if any")
   @OpenAPIListItems(value = String.class)
   private List<String> warnings;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void setPickerOutput(RelatedRecordPickerOutput pickerOutput)
   {
      this.records = RelatedRecordPickerResponseUtils.toOutputRecords(pickerOutput.getRecords());
      this.warnings = pickerOutput.getWarnings();
   }



   /*******************************************************************************
    ** Getter for records
    *******************************************************************************/
   public List<OutputRecord> getRecords()
   {
      return (this.records);
   }



   /*******************************************************************************
    ** Getter for warnings
    *******************************************************************************/
   public List<String> getWarnings()
   {
      return (this.warnings);
   }
}
