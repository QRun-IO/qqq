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

package com.kingsrook.qqq.backend.module.rdbms.sharing.model;


import java.time.Instant;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;


/*******************************************************************************
 ** QRecord Entity for SharedAsset table
 *******************************************************************************/
public class SharedAsset extends QRecordEntity
{
   public static final String TABLE_NAME = "SharedAsset";

   @QField(isEditable = false)
   private Integer id;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;

   @QField(possibleValueSourceName = Asset.TABLE_NAME)
   private Integer assetId;

   @QField(possibleValueSourceName = User.TABLE_NAME)
   private Integer userId;

   @QField(possibleValueSourceName = Group.TABLE_NAME)
   private Integer groupId;


   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public SharedAsset()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public SharedAsset(QRecord record)
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
   public SharedAsset withId(Integer id)
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
   public SharedAsset withCreateDate(Instant createDate)
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
   public SharedAsset withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for assetId
    *******************************************************************************/
   public Integer getAssetId()
   {
      return (this.assetId);
   }



   /*******************************************************************************
    ** Setter for assetId
    *******************************************************************************/
   public void setAssetId(Integer assetId)
   {
      this.assetId = assetId;
   }



   /*******************************************************************************
    ** Fluent setter for assetId
    *******************************************************************************/
   public SharedAsset withAssetId(Integer assetId)
   {
      this.assetId = assetId;
      return (this);
   }




   /*******************************************************************************
    ** Getter for userId
    *******************************************************************************/
   public Integer getUserId()
   {
      return (this.userId);
   }



   /*******************************************************************************
    ** Setter for userId
    *******************************************************************************/
   public void setUserId(Integer userId)
   {
      this.userId = userId;
   }



   /*******************************************************************************
    ** Fluent setter for userId
    *******************************************************************************/
   public SharedAsset withUserId(Integer userId)
   {
      this.userId = userId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for groupId
    *******************************************************************************/
   public Integer getGroupId()
   {
      return (this.groupId);
   }



   /*******************************************************************************
    ** Setter for groupId
    *******************************************************************************/
   public void setGroupId(Integer groupId)
   {
      this.groupId = groupId;
   }



   /*******************************************************************************
    ** Fluent setter for groupId
    *******************************************************************************/
   public SharedAsset withGroupId(Integer groupId)
   {
      this.groupId = groupId;
      return (this);
   }


}
