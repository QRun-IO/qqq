/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.modules.authentication.implementations.model;


import java.time.Instant;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;


/*******************************************************************************
 ** QRecord Entity for UserSession table
 *******************************************************************************/
public class UserSession extends QRecordEntity
{
   public static final String TABLE_NAME = "userSession";

   @QField(isEditable = false)
   private Integer id;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;

   @QField(isEditable = false, isHidden = true, maxLength = 40, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String uuid;

   @QField(isEditable = false, isHidden = true)
   private String accessToken;

   @QField(isEditable = false, maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.TRUNCATE_ELLIPSIS)
   private String userId;



   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public UserSession()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public UserSession(QRecord record)
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
   public UserSession withId(Integer id)
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
   public UserSession withCreateDate(Instant createDate)
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
   public UserSession withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for uuid
    *******************************************************************************/
   public String getUuid()
   {
      return (this.uuid);
   }



   /*******************************************************************************
    ** Setter for uuid
    *******************************************************************************/
   public void setUuid(String uuid)
   {
      this.uuid = uuid;
   }



   /*******************************************************************************
    ** Fluent setter for uuid
    *******************************************************************************/
   public UserSession withUuid(String uuid)
   {
      this.uuid = uuid;
      return (this);
   }



   /*******************************************************************************
    ** Getter for accessToken
    *******************************************************************************/
   public String getAccessToken()
   {
      return (this.accessToken);
   }



   /*******************************************************************************
    ** Setter for accessToken
    *******************************************************************************/
   public void setAccessToken(String accessToken)
   {
      this.accessToken = accessToken;
   }



   /*******************************************************************************
    ** Fluent setter for accessToken
    *******************************************************************************/
   public UserSession withAccessToken(String accessToken)
   {
      this.accessToken = accessToken;
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
   public UserSession withUserId(String userId)
   {
      this.userId = userId;
      return (this);
   }

}
