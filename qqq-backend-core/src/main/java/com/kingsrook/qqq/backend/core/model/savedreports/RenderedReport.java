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

package com.kingsrook.qqq.backend.core.model.savedreports;


import java.time.Instant;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormatPossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DisplayFormat;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DynamicDefaultValueBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;


/*******************************************************************************
 ** Entity bean for the rendered report table
 *******************************************************************************/
public class RenderedReport extends QRecordEntity
{
   public static final String TABLE_NAME = "renderedReport";

   @QField(isEditable = false)
   private Integer id;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;

   @QField(maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.ERROR, dynamicDefaultValueBehavior = DynamicDefaultValueBehavior.USER_ID)
   private String userId;

   @QField(possibleValueSourceName = SavedReport.TABLE_NAME)
   private Integer savedReportId;

   @QField(possibleValueSourceName = RenderedReportStatus.NAME, label = "Status")
   private Integer renderedReportStatusId;

   @QField(maxLength = 40, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String jobUuid;

   @QField(maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String resultPath;

   @QField(maxLength = 10, valueTooLongBehavior = ValueTooLongBehavior.ERROR, possibleValueSourceName = ReportFormatPossibleValueEnum.NAME)
   private String reportFormat;

   @QField()
   private Instant startTime;

   @QField()
   private Instant endTime;

   @QField(displayFormat = DisplayFormat.COMMAS)
   private Integer rowCount;

   @QField(maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.TRUNCATE_ELLIPSIS)
   private String errorMessage;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public RenderedReport()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public RenderedReport(QRecord qRecord) throws QException
   {
      populateFromQRecord(qRecord);
   }


   /*******************************************************************************
    ** Getter for id
    *******************************************************************************/
   public Integer getId()
   {
      return (this.id);
   }



   /*******************************************************************************
    ** Setter for id
    *******************************************************************************/
   public void setId(Integer id)
   {
      this.id = id;
   }



   /*******************************************************************************
    ** Fluent setter for id
    *******************************************************************************/
   public RenderedReport withId(Integer id)
   {
      this.id = id;
      return (this);
   }



   /*******************************************************************************
    ** Getter for createDate
    *******************************************************************************/
   public Instant getCreateDate()
   {
      return (this.createDate);
   }



   /*******************************************************************************
    ** Setter for createDate
    *******************************************************************************/
   public void setCreateDate(Instant createDate)
   {
      this.createDate = createDate;
   }



   /*******************************************************************************
    ** Fluent setter for createDate
    *******************************************************************************/
   public RenderedReport withCreateDate(Instant createDate)
   {
      this.createDate = createDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for modifyDate
    *******************************************************************************/
   public Instant getModifyDate()
   {
      return (this.modifyDate);
   }



   /*******************************************************************************
    ** Setter for modifyDate
    *******************************************************************************/
   public void setModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
   }



   /*******************************************************************************
    ** Fluent setter for modifyDate
    *******************************************************************************/
   public RenderedReport withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for userId
    *******************************************************************************/
   public String getUserId()
   {
      return (this.userId);
   }



   /*******************************************************************************
    ** Setter for userId
    *******************************************************************************/
   public void setUserId(String userId)
   {
      this.userId = userId;
   }



   /*******************************************************************************
    ** Fluent setter for userId
    *******************************************************************************/
   public RenderedReport withUserId(String userId)
   {
      this.userId = userId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for savedReportId
    *******************************************************************************/
   public Integer getSavedReportId()
   {
      return (this.savedReportId);
   }



   /*******************************************************************************
    ** Setter for savedReportId
    *******************************************************************************/
   public void setSavedReportId(Integer savedReportId)
   {
      this.savedReportId = savedReportId;
   }



   /*******************************************************************************
    ** Fluent setter for savedReportId
    *******************************************************************************/
   public RenderedReport withSavedReportId(Integer savedReportId)
   {
      this.savedReportId = savedReportId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for renderedReportStatusId
    *******************************************************************************/
   public Integer getRenderedReportStatusId()
   {
      return (this.renderedReportStatusId);
   }



   /*******************************************************************************
    ** Setter for renderedReportStatusId
    *******************************************************************************/
   public void setRenderedReportStatusId(Integer renderedReportStatusId)
   {
      this.renderedReportStatusId = renderedReportStatusId;
   }



   /*******************************************************************************
    ** Fluent setter for renderedReportStatusId
    *******************************************************************************/
   public RenderedReport withRenderedReportStatusId(Integer renderedReportStatusId)
   {
      this.renderedReportStatusId = renderedReportStatusId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for jobUuid
    *******************************************************************************/
   public String getJobUuid()
   {
      return (this.jobUuid);
   }



   /*******************************************************************************
    ** Setter for jobUuid
    *******************************************************************************/
   public void setJobUuid(String jobUuid)
   {
      this.jobUuid = jobUuid;
   }



   /*******************************************************************************
    ** Fluent setter for jobUuid
    *******************************************************************************/
   public RenderedReport withJobUuid(String jobUuid)
   {
      this.jobUuid = jobUuid;
      return (this);
   }



   /*******************************************************************************
    ** Getter for resultPath
    *******************************************************************************/
   public String getResultPath()
   {
      return (this.resultPath);
   }



   /*******************************************************************************
    ** Setter for resultPath
    *******************************************************************************/
   public void setResultPath(String resultPath)
   {
      this.resultPath = resultPath;
   }



   /*******************************************************************************
    ** Fluent setter for resultPath
    *******************************************************************************/
   public RenderedReport withResultPath(String resultPath)
   {
      this.resultPath = resultPath;
      return (this);
   }



   /*******************************************************************************
    ** Getter for reportFormat
    *******************************************************************************/
   public String getReportFormat()
   {
      return (this.reportFormat);
   }



   /*******************************************************************************
    ** Setter for reportFormat
    *******************************************************************************/
   public void setReportFormat(String reportFormat)
   {
      this.reportFormat = reportFormat;
   }



   /*******************************************************************************
    ** Fluent setter for reportFormat
    *******************************************************************************/
   public RenderedReport withReportFormat(String reportFormat)
   {
      this.reportFormat = reportFormat;
      return (this);
   }



   /*******************************************************************************
    ** Getter for startTime
    *******************************************************************************/
   public Instant getStartTime()
   {
      return (this.startTime);
   }



   /*******************************************************************************
    ** Setter for startTime
    *******************************************************************************/
   public void setStartTime(Instant startTime)
   {
      this.startTime = startTime;
   }



   /*******************************************************************************
    ** Fluent setter for startTime
    *******************************************************************************/
   public RenderedReport withStartTime(Instant startTime)
   {
      this.startTime = startTime;
      return (this);
   }



   /*******************************************************************************
    ** Getter for endTime
    *******************************************************************************/
   public Instant getEndTime()
   {
      return (this.endTime);
   }



   /*******************************************************************************
    ** Setter for endTime
    *******************************************************************************/
   public void setEndTime(Instant endTime)
   {
      this.endTime = endTime;
   }



   /*******************************************************************************
    ** Fluent setter for endTime
    *******************************************************************************/
   public RenderedReport withEndTime(Instant endTime)
   {
      this.endTime = endTime;
      return (this);
   }



   /*******************************************************************************
    ** Getter for rowCount
    *******************************************************************************/
   public Integer getRowCount()
   {
      return (this.rowCount);
   }



   /*******************************************************************************
    ** Setter for rowCount
    *******************************************************************************/
   public void setRowCount(Integer rowCount)
   {
      this.rowCount = rowCount;
   }



   /*******************************************************************************
    ** Fluent setter for rowCount
    *******************************************************************************/
   public RenderedReport withRowCount(Integer rowCount)
   {
      this.rowCount = rowCount;
      return (this);
   }



   /*******************************************************************************
    ** Getter for errorMessage
    *******************************************************************************/
   public String getErrorMessage()
   {
      return (this.errorMessage);
   }



   /*******************************************************************************
    ** Setter for errorMessage
    *******************************************************************************/
   public void setErrorMessage(String errorMessage)
   {
      this.errorMessage = errorMessage;
   }



   /*******************************************************************************
    ** Fluent setter for errorMessage
    *******************************************************************************/
   public RenderedReport withErrorMessage(String errorMessage)
   {
      this.errorMessage = errorMessage;
      return (this);
   }


}
