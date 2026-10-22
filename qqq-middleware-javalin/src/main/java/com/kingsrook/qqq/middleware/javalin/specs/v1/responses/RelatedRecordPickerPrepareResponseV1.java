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
 ** Response of the relatedRecordPicker prepare route: unsaved child rows.
 *******************************************************************************/
public class RelatedRecordPickerPrepareResponseV1 implements RelatedRecordPickerOutputInterface, ToSchema
{
   @OpenAPIDescription("One unsaved child row per target id, in request order, with defaults, values copied from the target, and the parent and target keys (whose display value is the target's label)")
   @OpenAPIListItems(value = OutputRecord.class, useRef = true)
   private List<OutputRecord> records;

   @OpenAPIDescription("Child fields for the user to review per row before the add route; empty means add without a review")
   @OpenAPIListItems(value = String.class)
   private List<String> reviewFieldNames;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void setPickerOutput(RelatedRecordPickerOutput pickerOutput)
   {
      this.records = RelatedRecordPickerResponseUtils.toOutputRecords(pickerOutput.getRecords());
      this.reviewFieldNames = pickerOutput.getReviewFieldNames();
   }



   /*******************************************************************************
    ** Getter for records
    *******************************************************************************/
   public List<OutputRecord> getRecords()
   {
      return (this.records);
   }



   /*******************************************************************************
    ** Getter for reviewFieldNames
    *******************************************************************************/
   public List<String> getReviewFieldNames()
   {
      return (this.reviewFieldNames);
   }
}
