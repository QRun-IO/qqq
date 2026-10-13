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
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.QueryFilter;


/*******************************************************************************
 ** Response of the relatedRecordPicker query route.
 *******************************************************************************/
public class RelatedRecordPickerQueryResponseV1 implements RelatedRecordPickerOutputInterface, ToSchema
{
   @OpenAPIDescription("One page of target records (values and display values), in order")
   @OpenAPIListItems(value = OutputRecord.class, useRef = true)
   private List<OutputRecord> records;

   @OpenAPIDescription("Whether more records follow (request the next page with skip); there is no count")
   private Boolean hasMore;

   @OpenAPIDescription("Ids (as strings) of the returned records the parent already has a child or join row for (MULTI_CHILD, LINK with primaryKey)")
   @OpenAPIListItems(value = String.class)
   private List<String> linkedIds;

   @OpenAPIDescription("The picker's defaultFilter with its variables resolved, to start the dialog's filter from (first page only)")
   private QueryFilter resolvedDefaultFilter;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void setPickerOutput(RelatedRecordPickerOutput pickerOutput)
   {
      this.records = RelatedRecordPickerResponseUtils.toOutputRecords(pickerOutput.getRecords());
      this.hasMore = pickerOutput.getHasMore();
      this.linkedIds = pickerOutput.getLinkedIds() == null ? null : pickerOutput.getLinkedIds().stream().map(String::valueOf).toList();
      this.resolvedDefaultFilter = pickerOutput.getResolvedDefaultFilter() == null ? null : new QueryFilter(pickerOutput.getResolvedDefaultFilter());
   }



   /*******************************************************************************
    ** Getter for records
    *******************************************************************************/
   public List<OutputRecord> getRecords()
   {
      return (this.records);
   }



   /*******************************************************************************
    ** Getter for hasMore
    *******************************************************************************/
   public Boolean getHasMore()
   {
      return (this.hasMore);
   }



   /*******************************************************************************
    ** Getter for linkedIds
    *******************************************************************************/
   public List<String> getLinkedIds()
   {
      return (this.linkedIds);
   }



   /*******************************************************************************
    ** Getter for resolvedDefaultFilter
    *******************************************************************************/
   public QueryFilter getResolvedDefaultFilter()
   {
      return (this.resolvedDefaultFilter);
   }
}
