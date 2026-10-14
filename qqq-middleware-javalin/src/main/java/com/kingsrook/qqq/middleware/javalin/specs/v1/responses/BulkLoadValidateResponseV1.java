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
import com.kingsrook.qqq.middleware.javalin.executors.io.BulkLoadValidateOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.BulkLoadRowError;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.BulkLoadSummaryLine;


/*******************************************************************************
 ** The result of a bulk-load dry run (nothing is stored).
 *******************************************************************************/
public class BulkLoadValidateResponseV1 implements BulkLoadValidateOutputInterface, ToSchema
{
   @OpenAPIDescription("SAMPLE (the first 100 rows) or FULL (the whole file).")
   private String mode;

   @OpenAPIDescription("The table's bulk-insert process the dry run used.")
   private String processName;

   @OpenAPIDescription("Records read from the file.")
   private Integer recordCount;

   @OpenAPIDescription("Records that would be inserted.")
   private Integer okCount;

   @OpenAPIDescription("Records with errors.")
   private Integer errorCount;

   @OpenAPIDescription("The standard process summary lines.")
   @OpenAPIListItems(value = BulkLoadSummaryLine.class, useRef = true)
   private List<BulkLoadSummaryLine> summaryLines;

   @OpenAPIDescription("Error groups with row numbers (up to 100).")
   @OpenAPIListItems(value = BulkLoadRowError.class, useRef = true)
   private List<BulkLoadRowError> errors;

   @OpenAPIDescription("For SAMPLE, the rows validated.")
   private Integer previewRecordCount;

   @OpenAPIDescription("Reserved: a FULL run over the durable threshold will run as a VALIDATE job and return its id. Always null in this version.")
   private Long jobId;



   /*******************************************************************************
    ** Getter for mode
    *******************************************************************************/
   public String getMode()
   {
      return (this.mode);
   }



   /*******************************************************************************
    ** Fluent setter for mode
    *******************************************************************************/
   public BulkLoadValidateResponseV1 withMode(String mode)
   {
      this.mode = mode;
      return (this);
   }



   /*******************************************************************************
    ** Getter for processName
    *******************************************************************************/
   public String getProcessName()
   {
      return (this.processName);
   }



   /*******************************************************************************
    ** Fluent setter for processName
    *******************************************************************************/
   public BulkLoadValidateResponseV1 withProcessName(String processName)
   {
      this.processName = processName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for recordCount
    *******************************************************************************/
   public Integer getRecordCount()
   {
      return (this.recordCount);
   }



   /*******************************************************************************
    ** Fluent setter for recordCount
    *******************************************************************************/
   public BulkLoadValidateResponseV1 withRecordCount(Integer recordCount)
   {
      this.recordCount = recordCount;
      return (this);
   }



   /*******************************************************************************
    ** Getter for okCount
    *******************************************************************************/
   public Integer getOkCount()
   {
      return (this.okCount);
   }



   /*******************************************************************************
    ** Fluent setter for okCount
    *******************************************************************************/
   public BulkLoadValidateResponseV1 withOkCount(Integer okCount)
   {
      this.okCount = okCount;
      return (this);
   }



   /*******************************************************************************
    ** Getter for errorCount
    *******************************************************************************/
   public Integer getErrorCount()
   {
      return (this.errorCount);
   }



   /*******************************************************************************
    ** Fluent setter for errorCount
    *******************************************************************************/
   public BulkLoadValidateResponseV1 withErrorCount(Integer errorCount)
   {
      this.errorCount = errorCount;
      return (this);
   }



   /*******************************************************************************
    ** Getter for summaryLines
    *******************************************************************************/
   public List<BulkLoadSummaryLine> getSummaryLines()
   {
      return (this.summaryLines);
   }



   /*******************************************************************************
    ** Fluent setter for summaryLines
    *******************************************************************************/
   public BulkLoadValidateResponseV1 withSummaryLines(List<BulkLoadSummaryLine> summaryLines)
   {
      this.summaryLines = summaryLines;
      return (this);
   }



   /*******************************************************************************
    ** Getter for errors
    *******************************************************************************/
   public List<BulkLoadRowError> getErrors()
   {
      return (this.errors);
   }



   /*******************************************************************************
    ** Fluent setter for errors
    *******************************************************************************/
   public BulkLoadValidateResponseV1 withErrors(List<BulkLoadRowError> errors)
   {
      this.errors = errors;
      return (this);
   }



   /*******************************************************************************
    ** Getter for previewRecordCount
    *******************************************************************************/
   public Integer getPreviewRecordCount()
   {
      return (this.previewRecordCount);
   }



   /*******************************************************************************
    ** Fluent setter for previewRecordCount
    *******************************************************************************/
   public BulkLoadValidateResponseV1 withPreviewRecordCount(Integer previewRecordCount)
   {
      this.previewRecordCount = previewRecordCount;
      return (this);
   }



   /*******************************************************************************
    ** Getter for jobId
    *******************************************************************************/
   public Long getJobId()
   {
      return (this.jobId);
   }



   /*******************************************************************************
    ** Fluent setter for jobId
    *******************************************************************************/
   public BulkLoadValidateResponseV1 withJobId(Long jobId)
   {
      this.jobId = jobId;
      return (this);
   }



   /*******************************************************************************
    ** Fill from core's result.
    *******************************************************************************/
   @Override
   public void setResult(BulkLoadAssistModels.Validation result)
   {
      this.mode = result.mode();
      this.processName = result.processName();
      this.recordCount = result.recordCount();
      this.okCount = result.okCount();
      this.errorCount = result.errorCount();
      this.previewRecordCount = result.previewRecordCount();
      this.jobId = result.jobId();
      this.summaryLines = new ArrayList<>();
      result.summaryLines().forEach(l -> this.summaryLines.add(new BulkLoadSummaryLine().withStatus(l.status()).withCount(l.count()).withMessage(l.message()).withBulletsOfText(new ArrayList<>(l.bulletsOfText()))));
      this.errors = new ArrayList<>();
      result.errors().forEach(e -> this.errors.add(new BulkLoadRowError().withRowNumbers(e.rowNumbers()).withFieldName(e.fieldName()).withErrorCode(e.errorCode()).withMessage(e.message())));
   }

}
