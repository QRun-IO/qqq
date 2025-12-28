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

package com.kingsrook.qqq.backend.core.model.savedreports;


import java.time.Instant;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormatPossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.common.TimeZonePossibleValueSourceMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DynamicDefaultValueBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;


/*******************************************************************************
 ** Entity bean for the scheduled report table
 *******************************************************************************/
public class ScheduledReport extends QRecordEntity
{
   public static final String TABLE_NAME = "scheduledReport";

   @QField(isEditable = false)
   private Integer id;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;

   @QField(isRequired = true, possibleValueSourceName = SavedReport.TABLE_NAME)
   private Integer savedReportId;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR, isRequired = true)
   private String cronExpression;

   @QField(maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.TRUNCATE_ELLIPSIS, isEditable = false)
   private String cronDescription;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR, possibleValueSourceName = TimeZonePossibleValueSourceMetaDataProvider.NAME, isRequired = true)
   private String cronTimeZoneId;

   @QField(isRequired = true, defaultValue = "true")
   private Boolean isActive;

   @QField(isRequired = true)
   private String toAddresses;

   @QField(isRequired = true, maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String subject;

   @QField(isRequired = true, maxLength = 20, valueTooLongBehavior = ValueTooLongBehavior.ERROR, possibleValueSourceName = ReportFormatPossibleValueEnum.NAME)
   private String format;

   @QField()
   private String inputValues;

   @QField(maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.ERROR, dynamicDefaultValueBehavior = DynamicDefaultValueBehavior.USER_ID, label = "Owner")
   private String userId;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public ScheduledReport()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public ScheduledReport(QRecord qRecord) throws QException
   {
      populateFromQRecord(qRecord);
   }



   /*******************************************************************************
    ** Getter for id
    **
    *******************************************************************************/
   public Integer getId()
   {
      return id;
   }



   /*******************************************************************************
    ** Setter for id
    **
    *******************************************************************************/
   public void setId(Integer id)
   {
      this.id = id;
   }



   /*******************************************************************************
    ** Getter for createDate
    **
    *******************************************************************************/
   public Instant getCreateDate()
   {
      return createDate;
   }



   /*******************************************************************************
    ** Setter for createDate
    **
    *******************************************************************************/
   public void setCreateDate(Instant createDate)
   {
      this.createDate = createDate;
   }



   /*******************************************************************************
    ** Getter for modifyDate
    **
    *******************************************************************************/
   public Instant getModifyDate()
   {
      return modifyDate;
   }



   /*******************************************************************************
    ** Setter for modifyDate
    **
    *******************************************************************************/
   public void setModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
   }



   /*******************************************************************************
    ** Fluent setter for id
    *******************************************************************************/
   public ScheduledReport withId(Integer id)
   {
      this.id = id;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for createDate
    *******************************************************************************/
   public ScheduledReport withCreateDate(Instant createDate)
   {
      this.createDate = createDate;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for modifyDate
    *******************************************************************************/
   public ScheduledReport withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
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
   public ScheduledReport withSavedReportId(Integer savedReportId)
   {
      this.savedReportId = savedReportId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for cronExpression
    *******************************************************************************/
   public String getCronExpression()
   {
      return (this.cronExpression);
   }



   /*******************************************************************************
    ** Setter for cronExpression
    *******************************************************************************/
   public void setCronExpression(String cronExpression)
   {
      this.cronExpression = cronExpression;
   }



   /*******************************************************************************
    ** Fluent setter for cronExpression
    *******************************************************************************/
   public ScheduledReport withCronExpression(String cronExpression)
   {
      this.cronExpression = cronExpression;
      return (this);
   }



   /*******************************************************************************
    ** Getter for cronTimeZoneId
    *******************************************************************************/
   public String getCronTimeZoneId()
   {
      return (this.cronTimeZoneId);
   }



   /*******************************************************************************
    ** Setter for cronTimeZoneId
    *******************************************************************************/
   public void setCronTimeZoneId(String cronTimeZoneId)
   {
      this.cronTimeZoneId = cronTimeZoneId;
   }



   /*******************************************************************************
    ** Fluent setter for cronTimeZoneId
    *******************************************************************************/
   public ScheduledReport withCronTimeZoneId(String cronTimeZoneId)
   {
      this.cronTimeZoneId = cronTimeZoneId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isActive
    *******************************************************************************/
   public Boolean getIsActive()
   {
      return (this.isActive);
   }



   /*******************************************************************************
    ** Setter for isActive
    *******************************************************************************/
   public void setIsActive(Boolean isActive)
   {
      this.isActive = isActive;
   }



   /*******************************************************************************
    ** Fluent setter for isActive
    *******************************************************************************/
   public ScheduledReport withIsActive(Boolean isActive)
   {
      this.isActive = isActive;
      return (this);
   }



   /*******************************************************************************
    ** Getter for toAddresses
    *******************************************************************************/
   public String getToAddresses()
   {
      return (this.toAddresses);
   }



   /*******************************************************************************
    ** Setter for toAddresses
    *******************************************************************************/
   public void setToAddresses(String toAddresses)
   {
      this.toAddresses = toAddresses;
   }



   /*******************************************************************************
    ** Fluent setter for toAddresses
    *******************************************************************************/
   public ScheduledReport withToAddresses(String toAddresses)
   {
      this.toAddresses = toAddresses;
      return (this);
   }



   /*******************************************************************************
    ** Getter for subject
    *******************************************************************************/
   public String getSubject()
   {
      return (this.subject);
   }



   /*******************************************************************************
    ** Setter for subject
    *******************************************************************************/
   public void setSubject(String subject)
   {
      this.subject = subject;
   }



   /*******************************************************************************
    ** Fluent setter for subject
    *******************************************************************************/
   public ScheduledReport withSubject(String subject)
   {
      this.subject = subject;
      return (this);
   }



   /*******************************************************************************
    ** Getter for format
    *******************************************************************************/
   public String getFormat()
   {
      return (this.format);
   }



   /*******************************************************************************
    ** Setter for format
    *******************************************************************************/
   public void setFormat(String format)
   {
      this.format = format;
   }



   /*******************************************************************************
    ** Fluent setter for format
    *******************************************************************************/
   public ScheduledReport withFormat(String format)
   {
      this.format = format;
      return (this);
   }



   /*******************************************************************************
    ** Getter for inputValues
    *******************************************************************************/
   public String getInputValues()
   {
      return (this.inputValues);
   }



   /*******************************************************************************
    ** Setter for inputValues
    *******************************************************************************/
   public void setInputValues(String inputValues)
   {
      this.inputValues = inputValues;
   }



   /*******************************************************************************
    ** Fluent setter for inputValues
    *******************************************************************************/
   public ScheduledReport withInputValues(String inputValues)
   {
      this.inputValues = inputValues;
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
   public ScheduledReport withUserId(String userId)
   {
      this.userId = userId;
      return (this);
   }


   /*******************************************************************************
    * Getter for cronDescription
    * @see #withCronDescription(String)
    *******************************************************************************/
   public String getCronDescription()
   {
      return (this.cronDescription);
   }



   /*******************************************************************************
    * Setter for cronDescription
    * @see #withCronDescription(String)
    *******************************************************************************/
   public void setCronDescription(String cronDescription)
   {
      this.cronDescription = cronDescription;
   }



   /*******************************************************************************
    * Fluent setter for cronDescription
    *
    * @param cronDescription
    * TODO document this property
    *
    * @return this
    *******************************************************************************/
   public ScheduledReport withCronDescription(String cronDescription)
   {
      this.cronDescription = cronDescription;
      return (this);
   }


}
