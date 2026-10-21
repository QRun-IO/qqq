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

package com.kingsrook.qqq.backend.core.processes.locks;


import java.time.Instant;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;


/*******************************************************************************
 ** QRecord Entity for ProcessLock table
 *******************************************************************************/
public class ProcessLock extends QRecordEntity
{
   public static final String TABLE_NAME = "processLock";

   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;

   @QField(isRequired = true, maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String key;

   @QField(possibleValueSourceName = ProcessLockType.TABLE_NAME)
   private Integer processLockTypeId;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String userId;

   @QField(label = "Session UUID", maxLength = 36, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String sessionUUID;

   @QField(maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String details;

   @QField()
   private Instant checkInTimestamp;

   @QField()
   private Instant expiresAtTimestamp;



   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public ProcessLock()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public ProcessLock(QRecord record)
   {
      populateFromQRecord(record);
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
   public ProcessLock withId(Integer id)
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
   public ProcessLock withCreateDate(Instant createDate)
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
   public ProcessLock withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for key
    *******************************************************************************/
   public String getKey()
   {
      return (this.key);
   }



   /*******************************************************************************
    ** Setter for key
    *******************************************************************************/
   public void setKey(String key)
   {
      this.key = key;
   }



   /*******************************************************************************
    ** Fluent setter for key
    *******************************************************************************/
   public ProcessLock withKey(String key)
   {
      this.key = key;
      return (this);
   }



   /*******************************************************************************
    ** Getter for checkInTimestamp
    *******************************************************************************/
   public Instant getCheckInTimestamp()
   {
      return (this.checkInTimestamp);
   }



   /*******************************************************************************
    ** Setter for checkInTimestamp
    *******************************************************************************/
   public void setCheckInTimestamp(Instant checkInTimestamp)
   {
      this.checkInTimestamp = checkInTimestamp;
   }



   /*******************************************************************************
    ** Fluent setter for checkInTimestamp
    *******************************************************************************/
   public ProcessLock withCheckInTimestamp(Instant checkInTimestamp)
   {
      this.checkInTimestamp = checkInTimestamp;
      return (this);
   }



   /*******************************************************************************
    ** Getter for expiresAtTimestamp
    *******************************************************************************/
   public Instant getExpiresAtTimestamp()
   {
      return (this.expiresAtTimestamp);
   }



   /*******************************************************************************
    ** Setter for expiresAtTimestamp
    *******************************************************************************/
   public void setExpiresAtTimestamp(Instant expiresAtTimestamp)
   {
      this.expiresAtTimestamp = expiresAtTimestamp;
   }



   /*******************************************************************************
    ** Fluent setter for expiresAtTimestamp
    *******************************************************************************/
   public ProcessLock withExpiresAtTimestamp(Instant expiresAtTimestamp)
   {
      this.expiresAtTimestamp = expiresAtTimestamp;
      return (this);
   }



   /*******************************************************************************
    ** Getter for processLockTypeId
    *******************************************************************************/
   public Integer getProcessLockTypeId()
   {
      return (this.processLockTypeId);
   }



   /*******************************************************************************
    ** Setter for processLockTypeId
    *******************************************************************************/
   public void setProcessLockTypeId(Integer processLockTypeId)
   {
      this.processLockTypeId = processLockTypeId;
   }



   /*******************************************************************************
    ** Fluent setter for processLockTypeId
    *******************************************************************************/
   public ProcessLock withProcessLockTypeId(Integer processLockTypeId)
   {
      this.processLockTypeId = processLockTypeId;
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
   public ProcessLock withUserId(String userId)
   {
      this.userId = userId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sessionUUID
    *******************************************************************************/
   public String getSessionUUID()
   {
      return (this.sessionUUID);
   }



   /*******************************************************************************
    ** Setter for sessionUUID
    *******************************************************************************/
   public void setSessionUUID(String sessionUUID)
   {
      this.sessionUUID = sessionUUID;
   }



   /*******************************************************************************
    ** Fluent setter for sessionUUID
    *******************************************************************************/
   public ProcessLock withSessionUUID(String sessionUUID)
   {
      this.sessionUUID = sessionUUID;
      return (this);
   }



   /*******************************************************************************
    ** Getter for details
    *******************************************************************************/
   public String getDetails()
   {
      return (this.details);
   }



   /*******************************************************************************
    ** Setter for details
    *******************************************************************************/
   public void setDetails(String details)
   {
      this.details = details;
   }



   /*******************************************************************************
    ** Fluent setter for details
    *******************************************************************************/
   public ProcessLock withDetails(String details)
   {
      this.details = details;
      return (this);
   }

}
