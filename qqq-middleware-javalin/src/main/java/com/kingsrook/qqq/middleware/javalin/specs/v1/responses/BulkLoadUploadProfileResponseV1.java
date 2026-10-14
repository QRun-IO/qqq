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


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.assist.BulkLoadAssistModels;
import com.kingsrook.qqq.middleware.javalin.executors.io.BulkLoadUploadProfileOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.BulkLoadColumnProfile;


/*******************************************************************************
 ** The header guess and column profiles of an upload.
 *******************************************************************************/
public class BulkLoadUploadProfileResponseV1 implements BulkLoadUploadProfileOutputInterface, ToSchema
{
   @OpenAPIDescription("The upload ref.")
   private String uploadRef;

   @OpenAPIDescription("The uploaded file name.")
   private String fileName;

   @OpenAPIDescription("Whether row 1 looks like a header row (the suggester maps at least one of its values).")
   private Boolean hasHeaderRow;

   @OpenAPIDescription("Data rows profiled (up to 1000).")
   private Integer rowsProfiled;

   @OpenAPIDescription("The columns.")
   @OpenAPIListItems(value = BulkLoadColumnProfile.class, useRef = true)
   private List<BulkLoadColumnProfile> columns;



   /*******************************************************************************
    ** Getter for uploadRef
    *******************************************************************************/
   public String getUploadRef()
   {
      return (this.uploadRef);
   }



   /*******************************************************************************
    ** Fluent setter for uploadRef
    *******************************************************************************/
   public BulkLoadUploadProfileResponseV1 withUploadRef(String uploadRef)
   {
      this.uploadRef = uploadRef;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fileName
    *******************************************************************************/
   public String getFileName()
   {
      return (this.fileName);
   }



   /*******************************************************************************
    ** Fluent setter for fileName
    *******************************************************************************/
   public BulkLoadUploadProfileResponseV1 withFileName(String fileName)
   {
      this.fileName = fileName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for hasHeaderRow
    *******************************************************************************/
   public Boolean getHasHeaderRow()
   {
      return (this.hasHeaderRow);
   }



   /*******************************************************************************
    ** Fluent setter for hasHeaderRow
    *******************************************************************************/
   public BulkLoadUploadProfileResponseV1 withHasHeaderRow(Boolean hasHeaderRow)
   {
      this.hasHeaderRow = hasHeaderRow;
      return (this);
   }



   /*******************************************************************************
    ** Getter for rowsProfiled
    *******************************************************************************/
   public Integer getRowsProfiled()
   {
      return (this.rowsProfiled);
   }



   /*******************************************************************************
    ** Fluent setter for rowsProfiled
    *******************************************************************************/
   public BulkLoadUploadProfileResponseV1 withRowsProfiled(Integer rowsProfiled)
   {
      this.rowsProfiled = rowsProfiled;
      return (this);
   }



   /*******************************************************************************
    ** Getter for columns
    *******************************************************************************/
   public List<BulkLoadColumnProfile> getColumns()
   {
      return (this.columns);
   }



   /*******************************************************************************
    ** Fluent setter for columns
    *******************************************************************************/
   public BulkLoadUploadProfileResponseV1 withColumns(List<BulkLoadColumnProfile> columns)
   {
      this.columns = columns;
      return (this);
   }



   /*******************************************************************************
    ** Fill from core's result.
    *******************************************************************************/
   @Override
   public void setResult(BulkLoadAssistModels.UploadProfile result)
   {
      this.uploadRef = result.uploadRef();
      this.fileName = result.fileName();
      this.hasHeaderRow = result.hasHeaderRow();
      this.rowsProfiled = result.rowsProfiled();
      this.columns = new ArrayList<>();
      for(BulkLoadAssistModels.ColumnProfile column : result.columns())
      {
         this.columns.add(new BulkLoadColumnProfile().withColumnIndex(column.columnIndex()).withColumnLetter(column.columnLetter()).withHeader(column.header())
            .withInferredType(column.inferredType()).withPercentBlank(column.percentBlank()).withDistinctCount(column.distinctCount())
            .withDistinctCountCapped(column.distinctCountCapped()).withMaxLength(column.maxLength()).withPatternClass(column.patternClass())
            .withMappedFieldName(column.mappedFieldName()).withClassification(column.classification()).withSamplesAllowed(column.samplesAllowed())
            .withSampleValues(new ArrayList<>(column.sampleValues())));
      }
   }

}
