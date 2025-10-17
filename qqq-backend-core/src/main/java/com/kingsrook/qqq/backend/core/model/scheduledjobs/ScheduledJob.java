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

package com.kingsrook.qqq.backend.core.model.scheduledjobs;


import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.common.TimeZonePossibleValueSourceMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.data.QAssociation;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QIgnore;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DisplayFormat;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.collections.MutableMap;


/*******************************************************************************
 **
 *******************************************************************************/
public class ScheduledJob extends QRecordEntity
{
   public static final String TABLE_NAME = "scheduledJob";

   @QField(isEditable = false)
   private Integer id;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;

   @QField(isRequired = true, maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.TRUNCATE)
   private String label;

   @QField(maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.TRUNCATE_ELLIPSIS)
   private String description;

   @QField(isRequired = true, label = "Scheduler", maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR, possibleValueSourceName = SchedulersPossibleValueSource.NAME)
   private String schedulerName;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String cronExpression;

   @QField(maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.TRUNCATE_ELLIPSIS, isEditable = false)
   private String cronDescription;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR, possibleValueSourceName = TimeZonePossibleValueSourceMetaDataProvider.NAME)
   private String cronTimeZoneId;

   @QField(displayFormat = DisplayFormat.COMMAS)
   private Integer repeatSeconds;

   @QField(isRequired = true, maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR, possibleValueSourceName = ScheduledJobTypePossibleValueSource.NAME)
   private String type;

   @QField(isRequired = true)
   private Boolean isActive;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String foreignKeyType;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String foreignKeyValue;

   @QAssociation(name = ScheduledJobParameter.TABLE_NAME)
   private List<ScheduledJobParameter> jobParameters;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public ScheduledJob()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public ScheduledJob(QRecord qRecord) throws QException
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
   public ScheduledJob withId(Integer id)
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
   public ScheduledJob withCreateDate(Instant createDate)
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
   public ScheduledJob withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for label
    *******************************************************************************/
   public String getLabel()
   {
      return (this.label);
   }



   /*******************************************************************************
    ** Setter for label
    *******************************************************************************/
   public void setLabel(String label)
   {
      this.label = label;
   }



   /*******************************************************************************
    ** Fluent setter for label
    *******************************************************************************/
   public ScheduledJob withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Getter for description
    *******************************************************************************/
   public String getDescription()
   {
      return (this.description);
   }



   /*******************************************************************************
    ** Setter for description
    *******************************************************************************/
   public void setDescription(String description)
   {
      this.description = description;
   }



   /*******************************************************************************
    ** Fluent setter for description
    *******************************************************************************/
   public ScheduledJob withDescription(String description)
   {
      this.description = description;
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
   public ScheduledJob withCronExpression(String cronExpression)
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
   public ScheduledJob withCronTimeZoneId(String cronTimeZoneId)
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
   public ScheduledJob withIsActive(Boolean isActive)
   {
      this.isActive = isActive;
      return (this);
   }



   /*******************************************************************************
    ** Getter for schedulerName
    *******************************************************************************/
   public String getSchedulerName()
   {
      return (this.schedulerName);
   }



   /*******************************************************************************
    ** Setter for schedulerName
    *******************************************************************************/
   public void setSchedulerName(String schedulerName)
   {
      this.schedulerName = schedulerName;
   }



   /*******************************************************************************
    ** Fluent setter for schedulerName
    *******************************************************************************/
   public ScheduledJob withSchedulerName(String schedulerName)
   {
      this.schedulerName = schedulerName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for type
    *******************************************************************************/
   public String getType()
   {
      return (this.type);
   }



   /*******************************************************************************
    ** Setter for type
    *******************************************************************************/
   public void setType(String type)
   {
      this.type = type;
   }



   /*******************************************************************************
    ** Fluent setter for type
    *******************************************************************************/
   public ScheduledJob withType(String type)
   {
      this.type = type;
      return (this);
   }



   /*******************************************************************************
    ** Getter for jobParameters
    *******************************************************************************/
   public List<ScheduledJobParameter> getJobParameters()
   {
      return (this.jobParameters);
   }



   /*******************************************************************************
    ** Getter for jobParameters - but a map of just the key=value pairs.
    *******************************************************************************/
   @QIgnore
   public Map<String, String> getJobParametersMap()
   {
      if(CollectionUtils.nullSafeIsEmpty(this.jobParameters))
      {
         return (new HashMap<>());
      }

      ///////////////////////////////////////////////////////////////////////////////////////
      // wrap in mutable map, just to avoid any immutable or other bs from toMap's default //
      ///////////////////////////////////////////////////////////////////////////////////////
      return new MutableMap<>(jobParameters.stream().collect(Collectors.toMap(ScheduledJobParameter::getKey, ScheduledJobParameter::getValue)));
   }



   /*******************************************************************************
    ** Setter for jobParameters
    *******************************************************************************/
   public void setJobParameters(List<ScheduledJobParameter> jobParameters)
   {
      this.jobParameters = jobParameters;
   }



   /*******************************************************************************
    ** Fluent setter for jobParameters
    *******************************************************************************/
   public ScheduledJob withJobParameters(List<ScheduledJobParameter> jobParameters)
   {
      this.jobParameters = jobParameters;
      return (this);
   }



   /*******************************************************************************
    ** Getter for repeatSeconds
    *******************************************************************************/
   public Integer getRepeatSeconds()
   {
      return (this.repeatSeconds);
   }



   /*******************************************************************************
    ** Setter for repeatSeconds
    *******************************************************************************/
   public void setRepeatSeconds(Integer repeatSeconds)
   {
      this.repeatSeconds = repeatSeconds;
   }



   /*******************************************************************************
    ** Fluent setter for repeatSeconds
    *******************************************************************************/
   public ScheduledJob withRepeatSeconds(Integer repeatSeconds)
   {
      this.repeatSeconds = repeatSeconds;
      return (this);
   }



   /*******************************************************************************
    ** Getter for foreignKeyType
    *******************************************************************************/
   public String getForeignKeyType()
   {
      return (this.foreignKeyType);
   }



   /*******************************************************************************
    ** Setter for foreignKeyType
    *******************************************************************************/
   public void setForeignKeyType(String foreignKeyType)
   {
      this.foreignKeyType = foreignKeyType;
   }



   /*******************************************************************************
    ** Fluent setter for foreignKeyType
    *******************************************************************************/
   public ScheduledJob withForeignKeyType(String foreignKeyType)
   {
      this.foreignKeyType = foreignKeyType;
      return (this);
   }



   /*******************************************************************************
    ** Getter for foreignKeyValue
    *******************************************************************************/
   public String getForeignKeyValue()
   {
      return (this.foreignKeyValue);
   }



   /*******************************************************************************
    ** Setter for foreignKeyValue
    *******************************************************************************/
   public void setForeignKeyValue(String foreignKeyValue)
   {
      this.foreignKeyValue = foreignKeyValue;
   }



   /*******************************************************************************
    ** Fluent setter for foreignKeyValue
    *******************************************************************************/
   public ScheduledJob withForeignKeyValue(String foreignKeyValue)
   {
      this.foreignKeyValue = foreignKeyValue;
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
   public ScheduledJob withCronDescription(String cronDescription)
   {
      this.cronDescription = cronDescription;
      return (this);
   }


}
